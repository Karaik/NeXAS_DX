package com.giga.nexas.transfer.bhe2bsdx.meka.naoto.graft;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.naoto.graft.resolve.BheResourceIndexResolver;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;

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


public class BuildResourceClosureStep {

    public TsukuyomiImportPlan buildResourceClosure(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            BheCommonProjectileAppendPlan commonProjectileAppendPlan
    ) {
        TsukuyomiImportPlan importPlan = new TsukuyomiImportPlan();
        BheResourceIndexResolver resolver =
                new BheResourceIndexResolver(commonProjectileAppendPlan, null);
        collectSourceSpriteIndices(tsukuyomiPackage, importPlan);
        collectSourceWazIndices(tsukuyomiPackage, importPlan);
        collectTargetSpriteIndices(bsdxBaseline, importPlan);
        collectTargetWazIndices(bsdxBaseline, importPlan);

        if (tsukuyomiPackage.getTsukuyomiMek() != null) {
            importPlan.getRequiredMekFiles().add(request.getMekFileName());
        }
        List<String> spmFiles = new ArrayList<>(tsukuyomiPackage.getSpmByFileName().keySet());
        spmFiles.sort(String.CASE_INSENSITIVE_ORDER);
        importPlan.getRequiredSpmFiles().addAll(spmFiles);

        collectReferencedChainFromTsukuyomiWaz(request, tsukuyomiPackage, importPlan, resolver);
        appendAuditTargets(request, importPlan);
        appendRebindTargets(importPlan);
        appendNotes(importPlan);
        return importPlan;
    }

    private void appendAuditTargets(TsukuyomiGraftRequest request, TsukuyomiImportPlan importPlan) {
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
        for (Map.Entry<Integer, Integer> entry : importPlan.getCommonWazReferenceTargetIndexBySourceIndex().entrySet()) {
            importPlan.getGrpAppendTargets().add("CommonWazaRef:" + entry.getKey() + "->" + entry.getValue());
        }
        for (Map.Entry<Integer, Integer> entry : importPlan.getCommonSpriteReferenceTargetIndexBySourceIndex().entrySet()) {
            importPlan.getGrpAppendTargets().add("CommonSpriteRef:" + entry.getKey() + "->" + entry.getValue());
        }
        for (Map.Entry<String, Integer> entry : importPlan.getCommonSeReferenceTargetItemIndexBySourcePair().entrySet()) {
            importPlan.getGrpAppendTargets().add("CommonSeRef:" + entry.getKey() + "->" + entry.getValue());
        }

        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array1");
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array2");
        importPlan.getProgramMaterialSyncTargets().add("ProgramMaterial.array3");
    }

    private void appendRebindTargets(TsukuyomiImportPlan importPlan) {
        importPlan.getMekRebindTargets().add("Tsukuyomi.mek.mekBasicInfo.wazFileSequence");
        importPlan.getMekRebindTargets().add("Tsukuyomi.mek.mekBasicInfo.spmFileSequence");
        importPlan.getMekRebindTargets().add("MekWeaponInfo.wazSequence 保持解释为目标 waz 内部 skill 索引");

        importPlan.getWazRebindTargets().add("CEventWazaSelect.wazFileNo");
        importPlan.getWazRebindTargets().add("CEventSprite.spmFileSequence");
        importPlan.getWazRebindTargets().add("CEventSe.seGroupIndex");
        importPlan.getWazRebindTargets().add("CEventSe.seItemIndex");
        importPlan.getWazRebindTargets().add("CEventVoice.byteDataList 前4字节（语音组索引）统一指向 Tsukuyomi 目标语音组");
    }

    private void appendNotes(TsukuyomiImportPlan importPlan) {
        importPlan.getNotes().add("step3 当前生成的是当前机体资源链计划，不再做文件级 diff。");
        importPlan.getNotes().add("step4 会针对链上的每个资源做复用或尾插决策。");
        importPlan.getNotes().add("step6/7 只消费 step4 产出的源到目标映射表。");
    }

    private void collectSourceSpriteIndices(TsukuyomiPackageBundle tsukuyomiPackage, TsukuyomiImportPlan importPlan) {
        SpriteGroupGrp spriteGroupGrp = tsukuyomiPackage.getSpriteGroupGrp();
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

    private void collectSourceWazIndices(TsukuyomiPackageBundle tsukuyomiPackage, TsukuyomiImportPlan importPlan) {
        WazaGroupGrp wazaGroupGrp = tsukuyomiPackage.getWazaGroupGrp();
        if (wazaGroupGrp == null) {
            return;
        }
        for (int i = 0; i < wazaGroupGrp.getWazaList().size(); i++) {
            WazaGroupGrp.WazaGroupEntry entry = wazaGroupGrp.getWazaList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            String fileName = buildWazFileName(entry);
            if (fileName != null) {
                importPlan.getSourceWazIndexByFileName().put(normalizeFileName(fileName), i);
            }
        }
    }

    private void collectTargetSpriteIndices(TsukuyomiBsdxBaselineBundle bsdxBaseline, TsukuyomiImportPlan importPlan) {
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

    private void collectTargetWazIndices(TsukuyomiBsdxBaselineBundle bsdxBaseline, TsukuyomiImportPlan importPlan) {
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
            if (fileName != null) {
                importPlan.getTargetWazIndexByFileName().put(normalizeFileName(fileName), i);
            }
        }
    }

    private void collectReferencedChainFromTsukuyomiWaz(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiImportPlan importPlan,
            BheResourceIndexResolver resolver
    ) {
        Waz sourceWaz = findRequiredSourceWaz(tsukuyomiPackage, request.getWazFileName());
        if (sourceWaz == null) {
            importPlan.getUnresolvedResources().add("缺少主 waz: " + request.getWazFileName());
            return;
        }

        Set<String> requiredWazFiles = new LinkedHashSet<>();
        requiredWazFiles.add(request.getWazFileName());
        Set<String> visitedWazFiles = new LinkedHashSet<>();

        Map<Integer, String> sourceWazFileNameByIndex = invertIndexMap(importPlan.getSourceWazIndexByFileName());
        Map<Integer, String> sourceSpriteFileNameByIndex = invertIndexMap(importPlan.getSourceSpriteIndexByFileName());

        boolean changed;
        do {
            changed = false;
            List<String> snapshot = new ArrayList<>(requiredWazFiles);
            for (String fileName : snapshot) {
                String normalizedFileName = normalizeFileName(fileName);
                if (!visitedWazFiles.add(normalizedFileName)) {
                    continue;
                }
                Waz currentWaz = findRequiredSourceWaz(tsukuyomiPackage, fileName);
                if (currentWaz == null) {
                    importPlan.getUnresolvedResources().add("缺少辅助 waz: " + fileName);
                    continue;
                }
                int before = requiredWazFiles.size();
                traverseWazReferences(
                        currentWaz,
                        importPlan,
                        tsukuyomiPackage,
                        sourceWazFileNameByIndex,
                        sourceSpriteFileNameByIndex,
                        requiredWazFiles,
                        resolver
                );
                changed |= requiredWazFiles.size() > before;
            }
        } while (changed);

        importPlan.getRequiredWazFiles().clear();
        importPlan.getRequiredWazFiles().addAll(requiredWazFiles);
    }

    private void traverseWazReferences(
            Waz sourceWaz,
            TsukuyomiImportPlan importPlan,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Map<Integer, String> sourceWazFileNameByIndex,
            Map<Integer, String> sourceSpriteFileNameByIndex,
            Set<String> requiredWazFiles,
            BheResourceIndexResolver resolver
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
                        tsukuyomiPackage,
                        sourceWazFileNameByIndex,
                        sourceSpriteFileNameByIndex,
                        requiredWazFiles,
                        resolver
                );
                    }
                }
            }
        }
    }

    private void collectReferencedIndicesFromObject(
            SkillInfoObject object,
            TsukuyomiImportPlan importPlan,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Map<Integer, String> sourceWazFileNameByIndex,
            Map<Integer, String> sourceSpriteFileNameByIndex,
            Set<String> requiredWazFiles,
            BheResourceIndexResolver resolver
    ) {
        if (object == null) {
            return;
        }

        if (object instanceof CEventWazaSelect select) {
            Integer sourceIndex = select.getWazFileNo();
            if (sourceIndex != null && sourceIndex >= 0) {
                if (resolver.isCommonWazIndex(sourceIndex)) {
                    importPlan.getCommonWazReferenceTargetIndexBySourceIndex()
                            .put(sourceIndex, resolver.resolveWazGroupIndex(sourceIndex));
                    return;
                }
                String fileName = sourceWazFileNameByIndex.get(sourceIndex);
                if (fileName != null) {
                    importPlan.getReferencedSourceWazFileNameByGroupIndex().put(sourceIndex, fileName);
                    if (containsFile(tsukuyomiPackage.getWazByFileName(), fileName)) {
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
                if (resolver.isCommonSpriteIndex(sourceIndex)) {
                    importPlan.getCommonSpriteReferenceTargetIndexBySourceIndex()
                            .put(sourceIndex, resolver.resolveSpriteGroupIndex(sourceIndex));
                    return;
                }
                String fileName = sourceSpriteFileNameByIndex.get(sourceIndex);
                if (fileName != null) {
                    importPlan.getReferencedSourceSpriteFileNameByGroupIndex().put(sourceIndex, fileName);
                } else {
                    importPlan.getUnresolvedResources().add("引用了无法解析文件名的源 SpriteGroup 索引: " + sourceIndex);
                }
            }
        }

        if (object instanceof CEventSe se) {
            collectReferencedSeIndices(se, importPlan, resolver);
        }

        if (hasNestedUnitList(object.getClass())) {
            collectReferencedIndicesFromNestedUnitLists(
                    object,
                    importPlan,
                    tsukuyomiPackage,
                    sourceWazFileNameByIndex,
                    sourceSpriteFileNameByIndex,
                    requiredWazFiles,
                    resolver
            );
        }
    }

    private void collectReferencedSeIndices(
            CEventSe se,
            TsukuyomiImportPlan importPlan,
            BheResourceIndexResolver resolver
    ) {
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
            if (resolver.isCommonSePair(groupIndex, itemIndex)) {
                String key = BheCommonProjectileAppendPlan.sePairKey(groupIndex, itemIndex);
                importPlan.getCommonSeReferenceTargetItemIndexBySourcePair()
                        .put(key, resolver.resolveSe(groupIndex, itemIndex).targetItemIndex());
                continue;
            }
            importPlan.getReferencedSourceSeItemIndicesByGroupIndex().computeIfAbsent(groupIndex, key -> new ArrayList<>());
            List<Integer> itemIndices = importPlan.getReferencedSourceSeItemIndicesByGroupIndex().get(groupIndex);
            if (!itemIndices.contains(itemIndex)) {
                itemIndices.add(itemIndex);
            }
        }
    }

    private void collectReferencedIndicesFromNestedUnitLists(
            SkillInfoObject source,
            TsukuyomiImportPlan importPlan,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Map<Integer, String> sourceWazFileNameByIndex,
            Map<Integer, String> sourceSpriteFileNameByIndex,
            Set<String> requiredWazFiles,
            BheResourceIndexResolver resolver
    ) {
        for (Field field : getAllFields(source.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType())
                    || !field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
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
                                tsukuyomiPackage,
                                sourceWazFileNameByIndex,
                                sourceSpriteFileNameByIndex,
                                requiredWazFiles,
                                resolver
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
            return value instanceof SkillInfoObject object ? object : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private boolean hasNestedUnitList(Class<?> type) {
        for (Field field : getAllFields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (List.class.isAssignableFrom(field.getType())
                    && field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
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

    private Waz findRequiredSourceWaz(TsukuyomiPackageBundle tsukuyomiPackage, String fileName) {
        if (tsukuyomiPackage.getWazByFileName() == null) {
            return null;
        }
        for (Map.Entry<String, Waz> entry : tsukuyomiPackage.getWazByFileName().entrySet()) {
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
        return existFlag != null && existFlag != 0;
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
