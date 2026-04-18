package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * BHE 公共弹幕资源簇的自重定向步骤。
 *
 * <p>自重定向只处理“公共资源自身如何成为目标侧资源”的问题，不处理 WAZ 指向其他资源的交叉引用。
 * 典型内容包括：新增 8 个 bhe_* WazaGroup entry、12 个 bhe_* SpriteGroup entry、
 * 1 个 bhe_common_projectile_se SeGroup entry、同步 GRP/ProgramMaterial/MEK material 容量。</p>
 */
public class SelfRedirectBheCommonProjectileResourcesStep {

    private static final String COMMON_SE_GROUP_NAME = "BHE_SE_PUBLIC";

    public void redirect(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        validateInputs(rawSourceBundle, inheritedBaseline, convertedBundle, appendPlan);

        appendCommonWazaGroupEntries(rawSourceBundle, inheritedBaseline, convertedBundle, appendPlan);
        appendCommonSpriteGroupEntries(rawSourceBundle, inheritedBaseline, convertedBundle, appendPlan);
        appendCommonSeGroupEntry(rawSourceBundle, inheritedBaseline, convertedBundle, appendPlan);
        syncGlobalCapacity(inheritedBaseline);
    }

    private void validateInputs(
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        if (rawSourceBundle == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少 rawSourceBundle");
        }
        if (inheritedBaseline == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少 inheritedBaseline");
        }
        if (convertedBundle == null || convertedBundle.getCommonProjectileResourceBundle() == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少 commonProjectileResourceBundle");
        }
        if (appendPlan == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少 appendPlan");
        }
        if (rawSourceBundle.getWazaGroupGrp() == null || rawSourceBundle.getWazaGroupGrp().getWazaList() == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少源 WazaGroup");
        }
        if (rawSourceBundle.getSpriteGroupGrp() == null || rawSourceBundle.getSpriteGroupGrp().getSpriteList() == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少源 SpriteGroup");
        }
        if (rawSourceBundle.getSeGroupGrp() == null || rawSourceBundle.getSeGroupGrp().getSeList() == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少源 SeGroup");
        }
        if (inheritedBaseline.getWazaGroupGrp() == null || inheritedBaseline.getWazaGroupGrp().getWazaList() == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少目标 WazaGroup");
        }
        if (inheritedBaseline.getSpriteGroupGrp() == null || inheritedBaseline.getSpriteGroupGrp().getSpriteList() == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少目标 SpriteGroup");
        }
        if (inheritedBaseline.getSeGroupGrp() == null || inheritedBaseline.getSeGroupGrp().getSeList() == null) {
            throw new IllegalStateException("BHE 公共资源接入缺少目标 SeGroup");
        }
    }

    private void appendCommonWazaGroupEntries(
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        WazaGroupGrp targetGroup = inheritedBaseline.getWazaGroupGrp();
        int baseSize = targetGroup.getWazaList().size();
        appendPlan.setBaseWazaGroupSize(baseSize);

        List<String> sourceFiles = BheCommonProjectileResources.commonProjectileWazFiles();
        for (int sourceIndex = 0; sourceIndex < sourceFiles.size(); sourceIndex++) {
            String sourceFileName = sourceFiles.get(sourceIndex);
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry sourceEntry =
                    requireSourceWazaGroup(rawSourceBundle, sourceIndex, sourceFileName);
            Waz convertedWaz = requireConvertedWaz(convertedBundle.getCommonProjectileResourceBundle(), sourceFileName);

            String targetFileName = prefixBheFileName(sourceEntry.getWazaDisplayName() + ".waz");
            convertedWaz.setFileName(stripExtension(targetFileName));

            WazaGroupGrp.WazaGroupEntry targetEntry = new WazaGroupGrp.WazaGroupEntry();
            targetEntry.setExistFlag(existFlagOrDefault(sourceEntry.getExistFlag()));
            targetEntry.setWazaName(sourceEntry.getWazaName());
            targetEntry.setWazaCodeName(prefixCodeName(sourceEntry.getWazaCodeName(), "WAZ_" + sourceIndex));
            targetEntry.setWazaDisplayName(stripExtension(targetFileName));
            targetEntry.setParam(countSkills(convertedWaz));

            int targetIndex = baseSize + sourceIndex;
            targetGroup.getWazaList().add(targetEntry);
            inheritedBaseline.getWazByFileName().put(targetFileName, convertedWaz);

            appendPlan.getCommonProjectileWazFiles().add(targetFileName);
            appendPlan.getSourceWazIndexToTargetIndex().put(sourceIndex, targetIndex);
            appendPlan.getSourceWazIndexToTargetFileName().put(sourceIndex, targetFileName);
            appendPlan.getNotes().add("WAZ " + sourceIndex + " -> " + targetIndex + " " + targetFileName
                    + " skills=" + targetEntry.getParam());
        }
    }

    private void appendCommonSpriteGroupEntries(
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        SpriteGroupGrp targetGroup = inheritedBaseline.getSpriteGroupGrp();
        int baseSize = targetGroup.getSpriteList().size();
        appendPlan.setBaseSpriteGroupSize(baseSize);

        List<String> sourceFiles = BheCommonProjectileResources.commonProjectileSpmFiles();
        for (int compactOrdinal = 0; compactOrdinal < sourceFiles.size(); compactOrdinal++) {
            String sourceFileName = sourceFiles.get(compactOrdinal);
            IndexedSpriteSource source = requireSourceSpriteGroup(rawSourceBundle, sourceFileName);
            Spm convertedSpm = requireConvertedSpm(convertedBundle.getCommonProjectileResourceBundle(), sourceFileName);

            String targetFileName = prefixBheFileName(source.entry().getSpriteFileName());
            prefixSpmImageNames(convertedSpm);

            SpriteGroupGrp.SpriteGroupEntry targetEntry = new SpriteGroupGrp.SpriteGroupEntry();
            targetEntry.setExistFlag(existFlagOrDefault(source.entry().getExistFlag()));
            targetEntry.setSpriteFileName(targetFileName);
            targetEntry.setSpriteCodeName(prefixCodeName(source.entry().getSpriteCodeName(), "SPM_" + compactOrdinal));
            targetEntry.setParam(source.entry().getParam());

            int targetIndex = baseSize + compactOrdinal;
            targetGroup.getSpriteList().add(targetEntry);
            inheritedBaseline.getSpmByFileName().put(targetFileName, convertedSpm);

            appendPlan.getCommonProjectileSpmFiles().add(targetFileName);
            appendPlan.getSourceSpriteIndexToTargetIndex().put(source.index(), targetIndex);
            appendPlan.getSourceSpriteIndexToTargetFileName().put(source.index(), targetFileName);
            appendPlan.getNotes().add("SPM " + source.index() + " -> " + targetIndex + " " + targetFileName);
        }
    }

    private void appendCommonSeGroupEntry(
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        SeGroupGrp targetGroup = inheritedBaseline.getSeGroupGrp();
        int baseSize = targetGroup.getSeList().size();
        appendPlan.setBaseSeGroupSize(baseSize);
        appendPlan.setCommonProjectileSeGroupIndex(baseSize);

        // SE 聚合组必须以 BHE 源公共 WAZ 的实际引用为准，不能用转换后 WAZ 反推。
        // 原因是格式转换阶段可能丢弃未映射事件；公共资源输入清单必须保留源侧完整引用事实。
        List<SePair> sourcePairs = collectCommonSePairs(rawSourceBundle);
        SeGroupGrp.SeGroupGroup aggregateGroup = new SeGroupGrp.SeGroupGroup();
        aggregateGroup.setExistFlag(1);
        // 公共弹幕 SE 聚合组使用固定业务名，便于人工审计和后续单机体阶段稳定引用。
        aggregateGroup.setSeType(COMMON_SE_GROUP_NAME);
        aggregateGroup.setSeTypeCodeName(COMMON_SE_GROUP_NAME);

        for (int targetItemIndex = 0; targetItemIndex < sourcePairs.size(); targetItemIndex++) {
            SePair sourcePair = sourcePairs.get(targetItemIndex);
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem sourceItem =
                    requireSourceSeItem(rawSourceBundle, sourcePair);

            SeGroupGrp.SeGroupItem targetItem = new SeGroupGrp.SeGroupItem();
            targetItem.setExistFlag(existFlagOrDefault(sourceItem.getExistFlag()));
            targetItem.setSeItemName(sourceItem.getSeItemName());
            targetItem.setSeItemCodeName(buildSeItemCodeName(sourcePair, sourceItem));
            targetItem.setSeFileName(prefixBheFileName(sourceItem.getSeFileName()));
            aggregateGroup.getSeItems().add(targetItem);

            String sourcePairKey = BheCommonProjectileAppendPlan.sePairKey(
                    sourcePair.groupIndex(),
                    sourcePair.itemIndex()
            );
            appendPlan.getSourceSePairToTargetItemIndex().put(sourcePairKey, targetItemIndex);
            appendPlan.getSourceSePairToTargetFileName().put(sourcePairKey, targetItem.getSeFileName());
        }

        targetGroup.getSeList().add(aggregateGroup);
        appendPlan.getGlobalSeReferences().add("BHE common projectile SE group index=" + baseSize
                + ", items=" + aggregateGroup.getSeItems().size());
        appendPlan.getNotes().add("SE common aggregate -> " + baseSize
                + " items=" + aggregateGroup.getSeItems().size());
    }

    private void syncGlobalCapacity(TsukuyomiBsdxBaselineBundle inheritedBaseline) {
        int spriteGroupSize = inheritedBaseline.getSpriteGroupGrp().getSpriteList().size();
        int seGroupSize = inheritedBaseline.getSeGroupGrp().getSeList().size();
        int batVoiceGroupSize = inheritedBaseline.getBatVoiceGrp() == null
                || inheritedBaseline.getBatVoiceGrp().getVoiceList() == null
                ? 0
                : inheritedBaseline.getBatVoiceGrp().getVoiceList().size();

        syncProgramMaterial(inheritedBaseline, spriteGroupSize, seGroupSize, batVoiceGroupSize);
        syncMapGroup(inheritedBaseline, spriteGroupSize, seGroupSize, batVoiceGroupSize);
        syncBaselineMekMaterial(inheritedBaseline, spriteGroupSize, seGroupSize, batVoiceGroupSize);
    }

    private void syncProgramMaterial(
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            int spriteGroupSize,
            int seGroupSize,
            int batVoiceGroupSize
    ) {
        ProgramMaterialGrp programMaterialGrp = inheritedBaseline.getProgramMaterialGrp();
        if (programMaterialGrp == null) {
            throw new IllegalStateException("公共资源接入需要 ProgramMaterial.grp");
        }
        ensureProgramArraySize(programMaterialGrp.getArray1(), spriteGroupSize, "ProgramMaterial.array1");
        ensureProgramArraySize(programMaterialGrp.getArray2(), seGroupSize, "ProgramMaterial.array2");
        ensureProgramArraySize(programMaterialGrp.getArray3(), batVoiceGroupSize, "ProgramMaterial.array3");
    }

    private void syncMapGroup(
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            int spriteGroupSize,
            int seGroupSize,
            int batVoiceGroupSize
    ) {
        if (inheritedBaseline.getMapGroupGrp() == null || inheritedBaseline.getMapGroupGrp().getGroupList() == null) {
            throw new IllegalStateException("公共资源接入需要 MapGroup.grp");
        }
        for (MapGroupGrp.MapGroup entry : inheritedBaseline.getMapGroupGrp().getGroupList()) {
            if (entry == null) {
                continue;
            }
            ensureMapArraySize(entry.getArray1(), spriteGroupSize);
            ensureMapArraySize(entry.getArray2(), seGroupSize);
            ensureMapArraySize(entry.getArray3(), batVoiceGroupSize);
        }
    }

    private void syncBaselineMekMaterial(
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            int spriteGroupSize,
            int seGroupSize,
            int batVoiceGroupSize
    ) {
        for (Map.Entry<String, Mek> entry : inheritedBaseline.getMekByFileName().entrySet()) {
            Mek mek = entry.getValue();
            if (mek == null || mek.getMekMaterialBlock() == null) {
                continue;
            }
            padMaterialEntries(mek.getMekMaterialBlock().getEntries(), spriteGroupSize, seGroupSize, batVoiceGroupSize);
            padMaterialEntries(mek.getMekMaterialBlock().getRegularEntries(), spriteGroupSize, seGroupSize, batVoiceGroupSize);
            padMaterialEntries(mek.getMekMaterialBlock().getTrailingEntries(), spriteGroupSize, seGroupSize, batVoiceGroupSize);
        }
    }

    private void padMaterialEntries(
            List<Mek.MekMaterialBlock.PluginEntry> entries,
            int spriteGroupSize,
            int seGroupSize,
            int batVoiceGroupSize
    ) {
        if (entries == null) {
            return;
        }
        for (Mek.MekMaterialBlock.PluginEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            padIntArrayList(entry.getSpriteGroups(), spriteGroupSize);
            padIntArrayList(entry.getSeGroups(), seGroupSize);
            padIntArrayList(entry.getVoiceGroups(), batVoiceGroupSize);
        }
    }

    private void ensureProgramArraySize(
            List<ProgramMaterialGrp.IntArray> values,
            int requiredSize,
            String label
    ) {
        if (values == null) {
            throw new IllegalStateException(label + " 不能为空");
        }
        while (values.size() < requiredSize) {
            values.add(new ProgramMaterialGrp.IntArray());
        }
    }

    private void ensureMapArraySize(List<MapGroupGrp.IntArray> values, int requiredSize) {
        if (values == null) {
            return;
        }
        while (values.size() < requiredSize) {
            values.add(new MapGroupGrp.IntArray());
        }
    }

    private void padIntArrayList(List<int[]> values, int requiredSize) {
        if (values == null) {
            return;
        }
        while (values.size() < requiredSize) {
            values.add(new int[0]);
        }
    }

    private com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry requireSourceWazaGroup(
            TsukuyomiRawSourceBundle rawSourceBundle,
            int sourceIndex,
            String sourceFileName
    ) {
        List<com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry> entries =
                rawSourceBundle.getWazaGroupGrp().getWazaList();
        if (sourceIndex < 0 || sourceIndex >= entries.size()) {
            throw new IllegalStateException("公共 WAZ 源索引越界: " + sourceIndex);
        }
        com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry entry = entries.get(sourceIndex);
        if (entry == null || !isExisting(entry.getExistFlag())) {
            throw new IllegalStateException("公共 WAZ 源条目不存在: " + sourceIndex);
        }
        String entryFileName = entry.getWazaDisplayName() + ".waz";
        if (!normalizeFileName(entryFileName).equals(normalizeFileName(sourceFileName))) {
            throw new IllegalStateException("公共 WAZ 清单与 BHE WazaGroup 不一致: index="
                    + sourceIndex + ", expected=" + sourceFileName + ", actual=" + entryFileName);
        }
        return entry;
    }

    private IndexedSpriteSource requireSourceSpriteGroup(
            TsukuyomiRawSourceBundle rawSourceBundle,
            String sourceFileName
    ) {
        List<com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry> entries =
                rawSourceBundle.getSpriteGroupGrp().getSpriteList();
        for (int i = 0; i < entries.size(); i++) {
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry = entries.get(i);
            if (entry == null || !isExisting(entry.getExistFlag())) {
                continue;
            }
            if (normalizeFileName(entry.getSpriteFileName()).equals(normalizeFileName(sourceFileName))) {
                return new IndexedSpriteSource(i, entry);
            }
        }
        throw new IllegalStateException("公共 SPM 清单在 BHE SpriteGroup 中找不到: " + sourceFileName);
    }

    private com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem requireSourceSeItem(
            TsukuyomiRawSourceBundle rawSourceBundle,
            SePair sourcePair
    ) {
        List<com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupGroup> groups =
                rawSourceBundle.getSeGroupGrp().getSeList();
        if (sourcePair.groupIndex() < 0 || sourcePair.groupIndex() >= groups.size()) {
            throw new IllegalStateException("公共 SE 源 group 越界: " + sourcePair);
        }
        com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupGroup group = groups.get(sourcePair.groupIndex());
        if (group == null || !isExisting(group.getExistFlag()) || group.getSeItems() == null) {
            throw new IllegalStateException("公共 SE 源 group 不存在: " + sourcePair);
        }
        if (sourcePair.itemIndex() < 0 || sourcePair.itemIndex() >= group.getSeItems().size()) {
            throw new IllegalStateException("公共 SE 源 item 越界: " + sourcePair);
        }
        com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem item =
                group.getSeItems().get(sourcePair.itemIndex());
        if (item == null || !isExisting(item.getExistFlag())) {
            throw new IllegalStateException("公共 SE 源 item 不存在: " + sourcePair);
        }
        return item;
    }

    private Waz requireConvertedWaz(TsukuyomiPackageBundle bundle, String sourceFileName) {
        Waz waz = findByFileName(bundle.getWazByFileName(), sourceFileName);
        if (waz == null) {
            throw new IllegalStateException("公共 WAZ 未完成格式转换: " + sourceFileName);
        }
        return waz;
    }

    private Spm requireConvertedSpm(TsukuyomiPackageBundle bundle, String sourceFileName) {
        Spm spm = findByFileName(bundle.getSpmByFileName(), sourceFileName);
        if (spm == null) {
            throw new IllegalStateException("公共 SPM 未完成格式转换: " + sourceFileName);
        }
        return spm;
    }

    private <T> T findByFileName(Map<String, T> map, String sourceFileName) {
        if (map == null) {
            return null;
        }
        String normalized = normalizeFileName(sourceFileName);
        for (Map.Entry<String, T> entry : map.entrySet()) {
            if (normalizeFileName(entry.getKey()).equals(normalized)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private List<SePair> collectCommonSePairs(TsukuyomiRawSourceBundle rawSourceBundle) {
        Set<SePair> pairs = new LinkedHashSet<>();
        for (com.giga.nexas.dto.bhe.waz.Waz waz : rawSourceBundle.getCommonProjectileWazByFileName().values()) {
            collectSePairsFromWaz(waz, pairs);
        }
        // 聚合后的 item index 需要稳定可审计，因此不按遍历遇到顺序，而按源 group/item 排序。
        return pairs.stream()
                .sorted(Comparator.comparingInt(SePair::groupIndex).thenComparingInt(SePair::itemIndex))
                .toList();
    }

    private void collectSePairsFromWaz(com.giga.nexas.dto.bhe.waz.Waz waz, Set<SePair> pairs) {
        if (waz == null || waz.getSkillList() == null) {
            return;
        }
        for (com.giga.nexas.dto.bhe.waz.Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject object
                            : unit.getSkillInfoObjectList()) {
                        collectSePairsFromObject(object, pairs);
                    }
                }
            }
        }
    }

    private void collectSePairsFromObject(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject object,
            Set<SePair> pairs
    ) {
        if (object == null) {
            return;
        }
        if (object instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventSe se) {
            collectSePairsFromCEventSe(se, pairs);
        }

        for (Field field : getAllFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType())
                    || !field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
                continue;
            }
            field.setAccessible(true);
            try {
                List<?> units = (List<?>) field.get(object);
                if (units == null) {
                    continue;
                }
                for (Object unit : units) {
                    com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject nested =
                            tryGetUnitData(unit);
                    if (nested != null) {
                        collectSePairsFromObject(nested, pairs);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("解析公共 WAZ 嵌套 SE 引用失败: " + field.getName(), e);
            }
        }
    }

    private void collectSePairsFromCEventSe(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventSe se,
            Set<SePair> pairs
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
            if (groupIndex >= 0 && itemIndex >= 0) {
                pairs.add(new SePair(groupIndex, itemIndex));
            }
        }
    }

    private com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject tryGetUnitData(Object unit) {
        if (unit == null) {
            return null;
        }
        try {
            Method getter = unit.getClass().getMethod("getData");
            Object value = getter.invoke(unit);
            return value instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject object
                    ? object
                    : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private void prefixSpmImageNames(Spm spm) {
        if (spm == null || spm.getImageData() == null) {
            return;
        }
        for (Spm.SPMImageData imageData : spm.getImageData()) {
            if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                continue;
            }
            imageData.setImageName(prefixBheFileName(imageData.getImageName()));
        }
    }

    private int countSkills(Waz waz) {
        return waz == null || waz.getSkillList() == null ? 0 : waz.getSkillList().size();
    }

    private String buildSeItemCodeName(
            SePair sourcePair,
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem sourceItem
    ) {
        String sourceCode = sourceItem == null ? "" : safeCodeName(sourceItem.getSeItemCodeName());
        return "BHE_" + sourcePair.groupIndex() + "_" + sourcePair.itemIndex()
                + (sourceCode.isBlank() ? "" : "_" + sourceCode);
    }

    private String prefixBheFileName(String sourceFileName) {
        if (sourceFileName == null || sourceFileName.isBlank()) {
            return "bhe_";
        }
        String trimmed = sourceFileName.trim();
        return trimmed.regionMatches(true, 0, "bhe_", 0, 4) ? trimmed : "bhe_" + trimmed;
    }

    private String prefixCodeName(String sourceCodeName, String fallback) {
        String value = safeCodeName(sourceCodeName);
        if (value.isBlank()) {
            value = fallback;
        }
        return value.startsWith("BHE_") ? value : "BHE_" + value;
    }

    private String safeCodeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim()
                .toUpperCase(Locale.ROOT)
                .replaceAll("[^A-Z0-9_]", "_")
                .replaceAll("_+", "_")
                .replaceAll("^_|_$", "");
    }

    private int existFlagOrDefault(Integer value) {
        return value == null ? 1 : value;
    }

    private boolean isExisting(Integer existFlag) {
        return existFlag != null && existFlag != 0;
    }

    private String stripExtension(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot <= 0 ? fileName : fileName.substring(0, dot);
    }

    private String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }

    private record IndexedSpriteSource(
            int index,
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry
    ) {
    }

    private record SePair(int groupIndex, int itemIndex) {
    }
}
