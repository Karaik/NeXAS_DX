package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责生成 AKAO graft 的导入计划。
 *
 * <p>这里不再做“JINKI 与 BSDX 的 loose file diff”。
 * 当前规则就是把 JINKI 闭包整体视为导入输入，
 * 然后明确后续有哪些 grp 追加点、哪些索引重绑点。</p>
 */
public class BuildImportPlanStep {

    public JinkiImportPlan buildImportPlan(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline
    ) {
        JinkiImportPlan importPlan = new JinkiImportPlan();

        // 1. 固定主 mek 导入闭包。
        if (jinkiPackage.getAkaoMek() != null) {
            importPlan.getRequiredMekFiles().add(request.getMekFileName());
        }

        // 2. 固定主 waz 与依赖 waz 导入闭包。
        // 当前策略是不再尝试复用同名 BSDX loose file，
        // 而是默认把 JINKI 真源下的这一整组 waz 全部导入。
        List<String> wazFiles = new ArrayList<>(jinkiPackage.getWazByFileName().keySet());
        wazFiles.sort(String.CASE_INSENSITIVE_ORDER);
        importPlan.getRequiredWazFiles().addAll(wazFiles);

        // 3. 固定主 spm 与依赖 spm 导入闭包。
        // 同样地，这里默认按 JINKI 真源整体导入，不再期待文件级复用。
        List<String> spmFiles = new ArrayList<>(jinkiPackage.getSpmByFileName().keySet());
        spmFiles.sort(String.CASE_INSENSITIVE_ORDER);
        importPlan.getRequiredSpmFiles().addAll(spmFiles);

        // 4. 明确后面 step4 需要追加的 grp 顶层目标。
        importPlan.getGrpAppendTargets().add("MekaGroup:" + request.getMekaCodeName());
        importPlan.getGrpAppendTargets().add("WazaGroup:" + request.getWazCodeName());
        importPlan.getGrpAppendTargets().add(
                "SpriteGroup:" + request.getSpriteCodeName() + "->" + request.getSpriteFileName()
        );
        importPlan.getGrpAppendTargets().add("BatVoice:" + request.getMekaCodeName());

        // 5. 明确 ProgramMaterial 需要同步的外层数组。
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array1");
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array3");

        // 6. 明确 Akao.mek 的索引重绑目标。
        importPlan.getMekRebindTargets().add("Akao.mek.mekBasicInfo.wazFileSequence");
        importPlan.getMekRebindTargets().add("Akao.mek.mekBasicInfo.spmFileSequence");
        importPlan.getMekRebindTargets().add("MekWeaponInfo.wazSequence 保持解释为 Akao.waz 内部 skill 索引");

        // 7. 明确 Akao.waz 的索引重绑目标。
        importPlan.getWazRebindTargets().add("Akao.waz 外部 spmFileSequence");
        importPlan.getWazRebindTargets().add("Akao.waz 外部 wazFileNo");

        // 8. 记录 JINKI 侧当前 sprite/waz 的源索引，方便后面真正重绑时直接查。
        collectSourceSpriteIndices(jinkiPackage, importPlan);
        collectSourceWazIndices(jinkiPackage, importPlan);

        // 9. 写几条固定说明，明确这一步的语义。
        importPlan.getNotes().add("当前 step3 生成的是导入计划，不再生成 reuse/import diff。");
        importPlan.getNotes().add("同名 loose file 默认不信任 BSDX 现存版本，按 JINKI 真源整体导入。");
        importPlan.getNotes().add("step4 起再根据 grp 追加结果分配目标索引，并执行 mek/waz 重绑。");

        return importPlan;
    }

    private void collectSourceSpriteIndices(JinkiPackageBundle jinkiPackage, JinkiImportPlan importPlan) {
        SpriteGroupGrp spriteGroupGrp = jinkiPackage.getSpriteGroupGrp();
        if (spriteGroupGrp == null) {
            return;
        }

        for (int i = 0; i < spriteGroupGrp.getSpriteList().size(); i++) {
            SpriteGroupGrp.SpriteGroupEntry entry = spriteGroupGrp.getSpriteList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (entry.getSpriteFileName() == null || entry.getSpriteFileName().isBlank()) {
                continue;
            }
            importPlan.getSourceSpriteIndexByFileName().put(entry.getSpriteFileName(), i);
        }
    }

    private void collectSourceWazIndices(JinkiPackageBundle jinkiPackage, JinkiImportPlan importPlan) {
        WazaGroupGrp wazaGroupGrp = jinkiPackage.getWazaGroupGrp();
        if (wazaGroupGrp == null) {
            return;
        }

        for (int i = 0; i < wazaGroupGrp.getWazaList().size(); i++) {
            WazaGroupGrp.WazaGroupEntry entry = wazaGroupGrp.getWazaList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            String fileName = buildWazFileName(entry.getWazaName());
            if (fileName == null) {
                continue;
            }
            importPlan.getSourceWazIndexByFileName().put(fileName, i);
        }
    }

    private boolean isExisting(Integer existFlag) {
        return existFlag == null || existFlag != 0;
    }

    private String buildWazFileName(String wazaName) {
        if (wazaName == null || wazaName.isBlank()) {
            return null;
        }
        return wazaName + ".waz";
    }
}
