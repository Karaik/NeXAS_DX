package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 负责生成 AKAO graft 的导入计划。
 *
 * <p>当前 step3 的职责是：
 * 先从 JINKI 真源里把当前机体实际用到的索引链抽出来，
 * 再把这些源索引整理成 step4 可执行的输入。</p>
 */
public class BuildImportPlanStep {

    public JinkiImportPlan buildImportPlan(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline
    ) {
        JinkiImportPlan importPlan = new JinkiImportPlan();

        // 1. 先建立 JINKI 源侧的完整索引表。
        collectSourceSpriteIndices(jinkiPackage, importPlan);
        collectSourceWazIndices(jinkiPackage, importPlan);

        // 2. 再建立 BSDX 基线的索引表。
        // 这里仅用于 step4 做“复用还是尾插”的决策，不作为 step7 的最终目标表。
        collectTargetSpriteIndices(bsdxBaseline, importPlan);
        collectTargetWazIndices(bsdxBaseline, importPlan);

        // 3. 固定主 mek 文件。
        if (jinkiPackage.getAkaoMek() != null) {
            importPlan.getRequiredMekFiles().add(request.getMekFileName());
        }

        // 4. 先把当前包内 spm 全部纳入闭包。
        // 这里暂时不对 spm 再做二次裁剪，因为当前包内只有 5 个文件，而且主 spm 依赖链还没有单独展开器。
        List<String> spmFiles = new ArrayList<>(jinkiPackage.getSpmByFileName().keySet());
        spmFiles.sort(String.CASE_INSENSITIVE_ORDER);
        importPlan.getRequiredSpmFiles().addAll(spmFiles);

        // 5. 从 Akao.waz 出发递归抽出真正会用到的外部 waz / sprite / se 索引链。
        //    弹幕/特效类辅助 waz 内部也可能继续引用其他 WAZ，所以这里按闭包遍历。
        collectReferencedChainFromAkaoWaz(request, jinkiPackage, importPlan);

        // 6. 以“当前机体自己的资源链”为中心，形成这次迁移要处理的对象清单。
        importPlan.getGrpAppendTargets().add("MekaGroup:" + request.getMekaCodeName());
        importPlan.getGrpAppendTargets().add("WazaGroup:" + request.getWazCodeName());
        importPlan.getGrpAppendTargets().add("SpriteGroup:" + request.getSpriteCodeName() + "->" + request.getSpriteFileName());
        importPlan.getGrpAppendTargets().add("BatVoice:" + request.getMekaCodeName());

        for (Map.Entry<Integer, String> entry : importPlan.getReferencedSourceWazFileNameByGroupIndex().entrySet()) {
            importPlan.getGrpAppendTargets().add("WazaGroupRef:" + entry.getKey() + "->" + entry.getValue());
        }

        for (Map.Entry<Integer, String> entry : importPlan.getReferencedSourceSpriteFileNameByGroupIndex().entrySet()) {
            importPlan.getGrpAppendTargets().add("SpriteGroupRef:" + entry.getKey() + "->" + entry.getValue());
        }

        for (Map.Entry<Integer, List<Integer>> entry : importPlan.getReferencedSourceSeItemIndicesByGroupIndex().entrySet()) {
            importPlan.getGrpAppendTargets().add("SeGroupRef:" + entry.getKey() + " items=" + entry.getValue());
        }

        // 7. ProgramMaterial 当前需要同步的外层数组。
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array1");
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array2");
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array3");

        // 8. 记录 mek / waz 这两步当前要负责重绑的字段。
        importPlan.getMekRebindTargets().add("Akao.mek.mekBasicInfo.wazFileSequence");
        importPlan.getMekRebindTargets().add("Akao.mek.mekBasicInfo.spmFileSequence");
        importPlan.getMekRebindTargets().add("MekWeaponInfo.wazSequence 保持解释为目标 waz 内部 skill 索引");

        importPlan.getWazRebindTargets().add("CEventWazaSelect.wazFileNo");
        importPlan.getWazRebindTargets().add("CEventSprite.spmFileSequence");
        importPlan.getWazRebindTargets().add("CEventSe.seGroupIndex");
        importPlan.getWazRebindTargets().add("CEventSe.seItemIndex");
        importPlan.getWazRebindTargets().add("CEventVoice.byteDataList 前4字节（语音组索引）统一指向 AKAO 目标语音组");

        // 9. 补几条说明，避免后续再回到文件级 diff 叙事。
        importPlan.getNotes().add("step3 当前生成的是当前机体资源链计划，不再做文件级 diff。");
        importPlan.getNotes().add("step4 会针对链上的每个资源做复用或尾插决策。");
        importPlan.getNotes().add("step6/7 只消费 step4 产出的源到目标映射表。");
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
            importPlan.getSourceSpriteIndexByFileName().put(normalizeFileName(entry.getSpriteFileName()), i);
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
            importPlan.getSourceWazIndexByFileName().put(normalizeFileName(fileName), i);
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
            importPlan.getTargetSpriteIndexByFileName().put(normalizeFileName(entry.getSpriteFileName()), i);
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
            importPlan.getTargetWazIndexByFileName().put(normalizeFileName(fileName), i);
        }
    }

    private void collectReferencedChainFromAkaoWaz(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiImportPlan importPlan
    ) {
        Waz sourceWaz = findRequiredSourceWaz(jinkiPackage, request.getWazFileName());
        if (sourceWaz == null) {
            importPlan.getUnresolvedResources().add("缺少主 waz: " + request.getWazFileName());
            return;
        }

        Set<String> requiredWazFiles = new LinkedHashSet<>();
        requiredWazFiles.add(request.getWazFileName());

        // requiredWazFiles 会在遍历时继续增长；visited 用来保证每个 WAZ 只扫描一次，
        // 防止 Effect/Bomb 这类特效链互相引用时出现无限递归。
        Set<String> visitedWazFiles = new LinkedHashSet<>();

        Map<Integer, String> sourceWazFileNameByIndex = invertIndexMap(importPlan.getSourceWazIndexByFileName());
        Map<Integer, String> sourceSpriteFileNameByIndex = invertIndexMap(importPlan.getSourceSpriteIndexByFileName());

        boolean changed;
        do {
            changed = false;

            // 这里要对快照遍历，不能直接遍历 requiredWazFiles 本体；
            // collectReferencedIndicesFromObject 可能会在循环中追加新的二级 WAZ。
            List<String> snapshot = new ArrayList<>(requiredWazFiles);
            for (String fileName : snapshot) {
                String normalizedFileName = normalizeFileName(fileName);
                if (!visitedWazFiles.add(normalizedFileName)) {
                    continue;
                }
                Waz currentWaz = findRequiredSourceWaz(jinkiPackage, fileName);
                if (currentWaz == null) {
                    importPlan.getUnresolvedResources().add("缺少辅助 waz: " + fileName);
                    continue;
                }
                int before = requiredWazFiles.size();
                traverseWazReferences(
                        currentWaz,
                        importPlan,
                        jinkiPackage,
                        sourceWazFileNameByIndex,
                        sourceSpriteFileNameByIndex,
                        requiredWazFiles
                );
                changed |= requiredWazFiles.size() > before;
            }
        } while (changed);

        importPlan.getRequiredWazFiles().clear();
        importPlan.getRequiredWazFiles().addAll(requiredWazFiles);
    }

    private void traverseWazReferences(
            Waz sourceWaz,
            JinkiImportPlan importPlan,
            JinkiPackageBundle jinkiPackage,
            Map<Integer, String> sourceWazFileNameByIndex,
            Map<Integer, String> sourceSpriteFileNameByIndex,
            Set<String> requiredWazFiles
    ) {
        if (sourceWaz.getSkillList() == null) {
            return;
        }

        for (Waz.Skill skill : sourceWaz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                        collectReferencedIndicesFromObject(
                                object,
                                importPlan,
                                jinkiPackage,
                                sourceWazFileNameByIndex,
                                sourceSpriteFileNameByIndex,
                                requiredWazFiles
                        );
                    }
                }
            }
        }
    }

    private void collectReferencedIndicesFromObject(
            SkillInfoObject object,
            JinkiImportPlan importPlan,
            JinkiPackageBundle jinkiPackage,
            Map<Integer, String> sourceWazFileNameByIndex,
            Map<Integer, String> sourceSpriteFileNameByIndex,
            Set<String> requiredWazFiles
    ) {
        if (object == null) {
            return;
        }

        if (object instanceof CEventWazaSelect select) {
            Integer sourceIndex = select.getWazFileNo();
            if (sourceIndex != null && sourceIndex >= 0) {
                String fileName = sourceWazFileNameByIndex.get(sourceIndex);
                if (fileName != null) {
                    // CEventWazaSelect 的 wazFileNo 是源侧 WazaGroup index；
                    // 先记录 group->file，后续 Step 4 再决定复用目标同名 WAZ 还是 append。
                    importPlan.getReferencedSourceWazFileNameByGroupIndex().put(sourceIndex, fileName);
                    if (containsFile(jinkiPackage.getWazByFileName(), fileName)) {
                        requiredWazFiles.add(fileName);
                    } else {
                        importPlan.getUnresolvedResources().add("引用了源 WazGroup[" + sourceIndex + "] 但包内缺少文件: " + fileName);
                    }
                } else {
                    importPlan.getUnresolvedResources().add("引用了无法解析文件名的源 WazGroup 索引: " + sourceIndex);
                }
            }
        }

        if (object instanceof CEventSprite sprite) {
            Integer sourceIndex = sprite.getSpmFileSequence();
            if (sourceIndex != null && sourceIndex >= 0) {
                String fileName = sourceSpriteFileNameByIndex.get(sourceIndex);
                if (fileName != null) {
                    // spmFileSequence 同样是源侧 SpriteGroup index；这里只收集引用，不在 plan 阶段改值。
                    importPlan.getReferencedSourceSpriteFileNameByGroupIndex().put(sourceIndex, fileName);
                } else {
                    importPlan.getUnresolvedResources().add("引用了无法解析文件名的源 SpriteGroup 索引: " + sourceIndex);
                }
            }
        }

        if (object instanceof CEventSe se) {
            collectReferencedSeIndices(se, importPlan);
        }

        if (hasNestedUnitList(object.getClass())) {
            // CEventEffect 等事件会把真正的资源引用塞在 *UnitList.data 里；
            // 如果不递归下去，Tama -> Bomb 这类二级弹幕链会直接漏掉。
            collectReferencedIndicesFromNestedUnitLists(
                    object,
                    importPlan,
                    jinkiPackage,
                    sourceWazFileNameByIndex,
                    sourceSpriteFileNameByIndex,
                    requiredWazFiles
            );
        }
    }

    private void collectReferencedSeIndices(CEventSe se, JinkiImportPlan importPlan) {
        if (se.getByteDataList() == null) {
            return;
        }

        for (byte[] bytes : se.getByteDataList()) {
            if (bytes == null || bytes.length < 8) {
                continue;
            }

            int groupIndex = readLittleEndianInt(bytes, 0);
            int itemIndex = readLittleEndianInt(bytes, 4);
            if (groupIndex < 0 || itemIndex < 0) {
                continue;
            }

            importPlan.getReferencedSourceSeItemIndicesByGroupIndex()
                    .computeIfAbsent(groupIndex, key -> new ArrayList<>());

            List<Integer> itemIndices = importPlan.getReferencedSourceSeItemIndicesByGroupIndex().get(groupIndex);
            if (!itemIndices.contains(itemIndex)) {
                itemIndices.add(itemIndex);
            }
        }
    }

    private void collectReferencedIndicesFromNestedUnitLists(
            SkillInfoObject source,
            JinkiImportPlan importPlan,
            JinkiPackageBundle jinkiPackage,
            Map<Integer, String> sourceWazFileNameByIndex,
            Map<Integer, String> sourceSpriteFileNameByIndex,
            Set<String> requiredWazFiles
    ) {
        for (Field field : getAllFields(source.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            if (!field.getName().endsWith("UnitList")) {
                continue;
            }

            field.setAccessible(true);
            try {
                List<?> units = (List<?>) field.get(source);
                if (units == null) {
                    continue;
                }
                for (Object unit : units) {
                    SkillInfoObject data = tryGetUnitData(unit);
                    if (data != null) {
                        collectReferencedIndicesFromObject(
                                data,
                                importPlan,
                                jinkiPackage,
                                sourceWazFileNameByIndex,
                                sourceSpriteFileNameByIndex,
                                requiredWazFiles
                        );
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("解析嵌套 unit 引用失败: " + field.getName(), e);
            }
        }
    }

    private Map<Integer, String> invertIndexMap(Map<String, Integer> source) {
        Map<Integer, String> target = new LinkedHashMap<>();
        for (Map.Entry<String, Integer> entry : source.entrySet()) {
            target.put(entry.getValue(), entry.getKey());
        }
        return target;
    }

    private SkillInfoObject tryGetUnitData(Object unit) {
        if (unit == null) {
            return null;
        }

        try {
            Method getter = unit.getClass().getMethod("getData");
            Object value = getter.invoke(unit);
            return value instanceof SkillInfoObject ? (SkillInfoObject) value : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private boolean hasNestedUnitList(Class<?> type) {
        for (Field field : getAllFields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (List.class.isAssignableFrom(field.getType()) && field.getName().endsWith("UnitList")) {
                return true;
            }
        }
        return false;
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            Field[] declaredFields = current.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                fields.add(declaredField);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private Waz findRequiredSourceWaz(JinkiPackageBundle jinkiPackage, String fileName) {
        if (jinkiPackage.getWazByFileName() == null) {
            return null;
        }

        for (Map.Entry<String, Waz> entry : jinkiPackage.getWazByFileName().entrySet()) {
            if (normalizeFileName(entry.getKey()).equals(normalizeFileName(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private boolean containsFile(Map<String, ?> registry, String fileName) {
        if (registry == null || fileName == null) {
            return false;
        }

        for (String key : registry.keySet()) {
            if (normalizeFileName(key).equals(normalizeFileName(fileName))) {
                return true;
            }
        }
        return false;
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private boolean isExisting(Integer existFlag) {
        return existFlag == null || existFlag != 0;
    }

    private String buildWazFileName(WazaGroupGrp.WazaGroupEntry entry) {
        if (entry == null) {
            return null;
        }
        String displayName = entry.getWazaDisplayName();
        if (displayName == null || displayName.isBlank()) {
            return null;
        }
        return displayName + ".waz";
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        return fileName.trim().toLowerCase(Locale.ROOT);
    }
}
