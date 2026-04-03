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
 * <p>这里不再做文件级 diff，而是直接把 JINKI 真源闭包整理成后续步骤可执行的施工单。</p>
 */
public class BuildImportPlanStep {

    public JinkiImportPlan buildImportPlan(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline
    ) {
        JinkiImportPlan importPlan = new JinkiImportPlan();

        // 1. 固定本次 graft 需要导入的主 mek 文件。
        if (jinkiPackage.getAkaoMek() != null) {
            importPlan.getRequiredMekFiles().add(request.getMekFileName());
        }

        // 2. 固定本次 graft 需要导入的 waz 闭包。
        // 这里不再尝试做“同名 loose file 是否复用”的 diff，
        // 而是默认把 JINKI 真源里这批 waz 全部纳入迁移输入。
        List<String> wazFiles = new ArrayList<>(jinkiPackage.getWazByFileName().keySet());
        wazFiles.sort(String.CASE_INSENSITIVE_ORDER);
        importPlan.getRequiredWazFiles().addAll(wazFiles);

        // 3. 固定本次 graft 需要导入的 spm 闭包。
        List<String> spmFiles = new ArrayList<>(jinkiPackage.getSpmByFileName().keySet());
        spmFiles.sort(String.CASE_INSENSITIVE_ORDER);
        importPlan.getRequiredSpmFiles().addAll(spmFiles);

        // 4. 明确 step4 之后要挂进 BSDX 的顶层 grp 条目。
        importPlan.getGrpAppendTargets().add("MekaGroup:" + request.getMekaCodeName());
        importPlan.getGrpAppendTargets().add("WazaGroup:" + request.getWazCodeName());
        importPlan.getGrpAppendTargets().add(
                "SpriteGroup:" + request.getSpriteCodeName() + "->" + request.getSpriteFileName()
        );
        importPlan.getGrpAppendTargets().add("BatVoice:" + request.getMekaCodeName());

        // 5. 明确 ProgramMaterial 需要同步的外层数组。
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array1");
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array3");

        // 6. 明确 Akao.mek 里当前已经确认要重绑的字段。
        importPlan.getMekRebindTargets().add("Akao.mek.mekBasicInfo.wazFileSequence");
        importPlan.getMekRebindTargets().add("Akao.mek.mekBasicInfo.spmFileSequence");
        importPlan.getMekRebindTargets().add("MekWeaponInfo.wazSequence 保持解释为 Akao.waz 内部 skill 索引");

        // 7. 明确 Akao.waz 里这一步要关心的外部引用字段。
        importPlan.getWazRebindTargets().add("Akao.waz 外部 spmFileSequence");
        importPlan.getWazRebindTargets().add("Akao.waz 外部 wazFileNo");
        importPlan.getWazRebindTargets().add("CEventWazaSelect.wazSequenceNo 继续解释为目标 waz 内部 skill 索引");

        // 8. 记录 JINKI 源侧的 sprite / waz 顶层索引。
        // step7 做内部重绑时，需要先知道“源字段原本指的是哪一个文件”。
        collectSourceSpriteIndices(jinkiPackage, importPlan);
        collectSourceWazIndices(jinkiPackage, importPlan);

        // 9. 记录 BSDX 基线里已经存在的 sprite / waz 顶层索引。
        // AKAO 本体和 moribito_2 的最终目标索引以 step4 结果为准，
        // 这里主要给 step7 提供共享辅助文件的目标索引参考。
        collectTargetSpriteIndices(bsdxBaseline, importPlan);
        collectTargetWazIndices(bsdxBaseline, importPlan);

        // 10. 给后续步骤留下少量固定说明，避免把这一步误读成 diff。
        importPlan.getNotes().add("当前 step3 生成的是导入计划，不再生成 reuse/import diff。");
        importPlan.getNotes().add("同名 loose file 默认不信任 BSDX 现存版本，按 JINKI 真源整体导入。");
        importPlan.getNotes().add("step4 起再根据 grp 追加结果分配 AKAO / moribito_2 的新目标索引。");

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
            String fileName = buildWazFileName(entry);
            if (fileName == null) {
                continue;
            }
            importPlan.getSourceWazIndexByFileName().put(fileName, i);
        }
    }

    private void collectTargetSpriteIndices(BsdxBaselineBundle bsdxBaseline, JinkiImportPlan importPlan) {
        SpriteGroupGrp spriteGroupGrp = bsdxBaseline.getSpriteGroupGrp();
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
            importPlan.getTargetSpriteIndexByFileName().put(entry.getSpriteFileName(), i);
        }
    }

    private void collectTargetWazIndices(BsdxBaselineBundle bsdxBaseline, JinkiImportPlan importPlan) {
        WazaGroupGrp wazaGroupGrp = bsdxBaseline.getWazaGroupGrp();
        if (wazaGroupGrp == null) {
            return;
        }

        for (int i = 0; i < wazaGroupGrp.getWazaList().size(); i++) {
            WazaGroupGrp.WazaGroupEntry entry = wazaGroupGrp.getWazaList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            String fileName = buildWazFileName(entry);
            if (fileName == null) {
                continue;
            }
            importPlan.getTargetWazIndexByFileName().put(fileName, i);
        }
    }

    private boolean isExisting(Integer existFlag) {
        return existFlag == null || existFlag != 0;
    }

    private String buildWazFileName(WazaGroupGrp.WazaGroupEntry entry) {
        if (entry == null) {
            return null;
        }

        // WazaGroup 里真正稳定可回到 loose file 名的是 displayName。
        // 例如 EFFECT -> Effect.waz，AKAO -> Akao.waz。
        String displayName = entry.getWazaDisplayName();
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        return displayName + ".waz";
    }
}
