package com.giga.nexas.jinki;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 直接跑一遍当前 jinki2bsdx runner，
 * 确认流程保持在“复用第 25 槽 + 复用 mekaIndex=32 + 禁用 103 扩容 patch”这条线上。
 */
public class TestJinki2BsdxRunner {

    @Test
    public void testRunAkaoGraftPipeline() {
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPatchMenuData(true);

        AkaoGraftResult result = new Jinki2BsdxSingleRunner().run(request);

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getJinkiPackage());
        Assertions.assertNotNull(result.getBsdxBaseline());
        Assertions.assertNotNull(result.getImportPlan());
        Assertions.assertNotNull(result.getGrpAppendPlan());
        Assertions.assertNotNull(result.getReboundAkaoMek());
        Assertions.assertNotNull(result.getReboundAkaoWaz());
        Assertions.assertNotNull(result.getImportedAssetSet());
        Assertions.assertNotNull(result.getExePatchPlan());
        Assertions.assertNotNull(result.getPacPackPlan());

        Assertions.assertFalse(result.getJinkiPackage().getSpmByFileName().isEmpty());
        Assertions.assertFalse(result.getJinkiPackage().getWazByFileName().isEmpty());
        Assertions.assertFalse(result.getImportPlan().getRequiredWazFiles().isEmpty());
        Assertions.assertFalse(result.getImportPlan().getRequiredSpmFiles().isEmpty());

        Assertions.assertEquals(103, result.getGrpAppendPlan().getMekaGroupIndex());
        Assertions.assertTrue(result.getGrpAppendPlan().getWazaGroupIndex() >= 0);
        Assertions.assertTrue(result.getGrpAppendPlan().getSpriteGroupIndex() >= 0);
        Assertions.assertTrue(result.getGrpAppendPlan().getBatVoiceGroupIndex() >= 0);

        Assertions.assertNotNull(result.getSyncedProgramMaterial());
        Assertions.assertEquals(139, result.getSyncedProgramMaterial().getArray1().size());
        Assertions.assertEquals(38, result.getSyncedProgramMaterial().getArray2().size());
        Assertions.assertEquals(31, result.getSyncedProgramMaterial().getArray3().size());

        Assertions.assertEquals(110, result.getReboundAkaoMek().getMekBasicInfo().getWazFileSequence());
        Assertions.assertEquals(138, result.getReboundAkaoMek().getMekBasicInfo().getSpmFileSequence());
        Assertions.assertFalse(result.getReboundAkaoWaz().getSkillList().isEmpty());
        Assertions.assertFalse(collectVoiceGroupIndices(result.getReboundAkaoWaz()).isEmpty());
        Assertions.assertTrue(collectVoiceGroupIndices(result.getReboundAkaoWaz()).stream().allMatch(index -> index == 30));
        assertWazaGroupParamsMatchActualWaz(result);
        assertMaterialSpriteGroupsRemapped(result);
        assertMaterialSeGroupsRemapped(result);
        assertMaterialVoiceGroupsRemapped(result);
        assertMekVoiceInfoDefaultGroupRemapped(result);
        assertWeaponEquipDatExpanded(result);

        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir()));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("ProgramMaterial.grp")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("Meka.dat")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("MekaPilot.dat")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("SelectMekaMenu.dat")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("MekaPilot.spm")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("SelectMekaMenuMeka.spm")));

        int mekaIdx = result.getGrpAppendPlan().getMekaGroupIndex();
        Assertions.assertEquals(200, findSecondColumnValue(result.getPatchedMekaDat(), mekaIdx));
        Assertions.assertTrue(containsSingleColumnRow(result.getPatchedMekaPilotDat(), mekaIdx));
        Assertions.assertEquals(mekaIdx, asInt(result.getPatchedSelectMekaMenuDat().getData().get(24).get(0)));
        Assertions.assertEquals(
                asInt(result.getBsdxBaseline().getSelectMekaMenuDat().getData().get(23).get(2)),
                asInt(result.getPatchedSelectMekaMenuDat().getData().get(24).get(2))
        );
        Assertions.assertEquals(
                result.getBsdxBaseline().getSelectMekaMenuDat().getData().size(),
                result.getPatchedSelectMekaMenuDat().getData().size()
        );
        Assertions.assertEquals(
                result.getBsdxBaseline().getMekaPilotSpm().getAnimData().size(),
                result.getPatchedMekaPilotSpm().getAnimData().size()
        );
        Assertions.assertEquals(
                result.getBsdxBaseline().getSelectMekaMenuMekaSpm().getAnimData().size(),
                result.getPatchedSelectMekaMenuMekaSpm().getAnimData().size()
        );
        Assertions.assertTrue(
                result.getPatchedSelectMekaMenuMekaSpm().getImageData().stream()
                        .map(image -> image == null || image.getImageName() == null ? "" : image.getImageName().toLowerCase(Locale.ROOT))
                        .anyMatch("selectmekamenumeka_0011_0001.png"::equals)
        );
        Assertions.assertTrue(
                result.getPatchedSelectMekaMenuMekaSpm().getImageData().stream()
                        .map(image -> image == null || image.getImageName() == null ? "" : image.getImageName().toLowerCase(Locale.ROOT))
                        .anyMatch("selectmekamenumeka_0012_0001.png"::equals)
        );
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("selectmekamenumeka_0011_0001.png")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("selectmekamenumeka_0012_0001.png")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("Akao_0901.ogg")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("Akao_0902.ogg")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("Akao_0903.ogg")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("Akao_0904.ogg")));

        Assertions.assertEquals(104, result.getExePatchPlan().getRequiredMekaCapacity());
        Assertions.assertTrue(result.getExePatchPlan().getRequiredWazaCapacity() >= 111);
        Assertions.assertTrue(result.getExePatchPlan().getRequiredSpriteCapacity() >= 139);
        Assertions.assertTrue(result.getExePatchPlan().getRequiredBatVoiceCapacity() >= 31);
        Assertions.assertTrue(result.getExePatchPlan().getRequiredSeCapacity() >= 38);
        Assertions.assertEquals(70, result.getExePatchPlan().getRequiredSelectMekaMenuRows());
        // patchMenuData=true 时 exe 有菜单 patch offsets，不为空是预期行为
        Assertions.assertFalse(result.getExePatchPlan().getTargetOffsets().isEmpty());
        Assertions.assertTrue(Files.exists(result.getExePatchPlan().getOutputExePath()));
        assertExeContainsPatchedMekaRuntimeTableBounds(result);

        Assertions.assertTrue(result.getPacPackPlan().isPacked());
        Assertions.assertTrue(Files.exists(result.getPacPackPlan().getOutputPacPath()));
    }

    private List<Integer> collectVoiceGroupIndices(Waz waz) {
        List<Integer> indices = new ArrayList<>();
        if (waz == null || waz.getSkillList() == null) {
            return indices;
        }

        for (Waz.Skill skill : waz.getSkillList()) {
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
                        collectVoiceGroupIndicesFromObject(object, indices);
                    }
                }
            }
        }
        return indices;
    }

    private void collectVoiceGroupIndicesFromObject(SkillInfoObject object, List<Integer> indices) {
        if (object == null) {
            return;
        }

        if (object instanceof CEventVoice voice) {
            if (voice.getByteDataList() == null) {
                return;
            }
            for (byte[] bytes : voice.getByteDataList()) {
                if (bytes == null || bytes.length < 4) {
                    continue;
                }
                indices.add(readLittleEndianInt(bytes, 0));
            }
        }
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private boolean containsSingleColumnRow(com.giga.nexas.dto.bsdx.dat.Dat dat, int expectedFirst) {
        if (dat == null || dat.getData() == null) {
            return false;
        }
        return dat.getData().stream()
                .filter(Objects::nonNull)
                .anyMatch(row -> !row.isEmpty() && expectedFirst == asInt(row.get(0)));
    }

    private int findSecondColumnValue(com.giga.nexas.dto.bsdx.dat.Dat dat, int expectedFirst) {
        if (dat == null || dat.getData() == null) {
            return -1;
        }
        for (List<Object> row : dat.getData()) {
            if (row != null && row.size() > 1 && expectedFirst == asInt(row.get(0))) {
                return asInt(row.get(1));
            }
        }
        return -1;
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private void assertMaterialVoiceGroupsRemapped(AkaoGraftResult result) {
        int targetVoiceGroupIndex = result.getGrpAppendPlan().getBatVoiceGroupIndex();
        int sourceVoiceGroupIndex = findSourceBatVoiceGroupIndex(result, targetVoiceGroupIndex);
        BatVoiceGrp.BatVoiceGroup targetVoiceGroup = result.getBsdxBaseline().getBatVoiceGrp().getVoiceList().get(targetVoiceGroupIndex);
        int legalVoiceCount = targetVoiceGroup.getVoices() == null ? 0 : targetVoiceGroup.getVoices().size();

        Mek sourceMek = result.getJinkiPackage().getAkaoMek();
        Mek targetMek = result.getReboundAkaoMek();
        Assertions.assertNotNull(sourceMek);
        Assertions.assertNotNull(targetMek);
        Assertions.assertNotNull(sourceMek.getMekMaterialBlock());
        Assertions.assertNotNull(targetMek.getMekMaterialBlock());

        assertPluginEntryVoiceGroupsRemapped(
                sourceMek.getMekMaterialBlock().getEntries(),
                targetMek.getMekMaterialBlock().getEntries(),
                sourceVoiceGroupIndex,
                targetVoiceGroupIndex,
                legalVoiceCount,
                "entries"
        );
        assertPluginEntryVoiceGroupsRemapped(
                sourceMek.getMekMaterialBlock().getRegularEntries(),
                targetMek.getMekMaterialBlock().getRegularEntries(),
                sourceVoiceGroupIndex,
                targetVoiceGroupIndex,
                legalVoiceCount,
                "regularEntries"
        );
        assertPluginEntryVoiceGroupsRemapped(
                sourceMek.getMekMaterialBlock().getTrailingEntries(),
                targetMek.getMekMaterialBlock().getTrailingEntries(),
                sourceVoiceGroupIndex,
                targetVoiceGroupIndex,
                legalVoiceCount,
                "trailingEntries"
        );
    }

    private void assertMekVoiceInfoDefaultGroupRemapped(AkaoGraftResult result) {
        int targetVoiceGroupIndex = result.getGrpAppendPlan().getBatVoiceGroupIndex();
        int sourceVoiceGroupIndex = findSourceBatVoiceGroupIndex(result, targetVoiceGroupIndex);

        Mek sourceMek = result.getJinkiPackage().getAkaoMek();
        Mek targetMek = result.getReboundAkaoMek();
        Assertions.assertNotNull(sourceMek);
        Assertions.assertNotNull(targetMek);
        Assertions.assertNotNull(sourceMek.getMekVoiceInfo());
        Assertions.assertNotNull(targetMek.getMekVoiceInfo());

        Assertions.assertEquals(sourceVoiceGroupIndex, sourceMek.getMekVoiceInfo().getVersion());
        Assertions.assertEquals(targetVoiceGroupIndex, targetMek.getMekVoiceInfo().getVersion());
        Assertions.assertNotEquals(sourceVoiceGroupIndex, targetMek.getMekVoiceInfo().getVersion());
    }

    private int findSourceBatVoiceGroupIndex(AkaoGraftResult result, int targetVoiceGroupIndex) {
        return result.getGrpAppendPlan().getSourceBatVoiceGroupIndexToTargetIndex().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() == targetVoiceGroupIndex)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缺少 BatVoice 源 -> 目标映射，target=" + targetVoiceGroupIndex));
    }

    private void assertPluginEntryVoiceGroupsRemapped(
            List<Mek.MekMaterialBlock.PluginEntry> sourceEntries,
            List<Mek.MekMaterialBlock.PluginEntry> targetEntries,
            int sourceVoiceGroupIndex,
            int targetVoiceGroupIndex,
            int legalVoiceCount,
            String label
    ) {
        if (sourceEntries == null || targetEntries == null) {
            return;
        }

        Assertions.assertEquals(sourceEntries.size(), targetEntries.size(), label + " size mismatch");
        boolean sawSourceVoiceGroup = false;
        for (int i = 0; i < sourceEntries.size(); i++) {
            Mek.MekMaterialBlock.PluginEntry sourceEntry = sourceEntries.get(i);
            Mek.MekMaterialBlock.PluginEntry targetEntry = targetEntries.get(i);
            if (sourceEntry == null || targetEntry == null) {
                Assertions.assertEquals(sourceEntry, targetEntry, label + "[" + i + "] null mismatch");
                continue;
            }

            int[] sourceItems = getVoiceGroupItems(sourceEntry, sourceVoiceGroupIndex);
            int[] targetItems = getVoiceGroupItems(targetEntry, targetVoiceGroupIndex);
            if (!isNullOrEmpty(sourceItems)) {
                sawSourceVoiceGroup = true;
                Assertions.assertArrayEquals(
                        sourceItems,
                        targetItems,
                        label + "[" + i + "] source group " + sourceVoiceGroupIndex + " should remap to target group " + targetVoiceGroupIndex
                );
            }

            Assertions.assertTrue(
                    isNullOrEmpty(getVoiceGroupItems(targetEntry, sourceVoiceGroupIndex)),
                    label + "[" + i + "] should not retain source voice group " + sourceVoiceGroupIndex
            );

            if (!isNullOrEmpty(targetItems)) {
                for (int itemIndex : targetItems) {
                    Assertions.assertTrue(
                            itemIndex >= 0 && itemIndex < legalVoiceCount,
                            label + "[" + i + "] target group " + targetVoiceGroupIndex + " contains illegal item index " + itemIndex
                    );
                }
            }
        }
        Assertions.assertTrue(sawSourceVoiceGroup, label + " should contain at least one source voice group " + sourceVoiceGroupIndex + " before remap");
    }

    private void assertMaterialSpriteGroupsRemapped(AkaoGraftResult result) {
        int targetSpriteGroupIndex = result.getGrpAppendPlan().getSpriteGroupIndex();
        int sourceSpriteGroupIndex = findSourceSpriteGroupIndex(result, targetSpriteGroupIndex);

        Mek sourceMek = result.getJinkiPackage().getAkaoMek();
        Mek targetMek = result.getReboundAkaoMek();
        Assertions.assertNotNull(sourceMek);
        Assertions.assertNotNull(targetMek);
        Assertions.assertNotNull(sourceMek.getMekMaterialBlock());
        Assertions.assertNotNull(targetMek.getMekMaterialBlock());

        assertIndexedGroupsRemappedIfPresent(
                sourceMek.getMekMaterialBlock().getEntries(),
                targetMek.getMekMaterialBlock().getEntries(),
                true,
                sourceSpriteGroupIndex,
                targetSpriteGroupIndex,
                null,
                "entries.sprite"
        );
        assertIndexedGroupsRemappedIfPresent(
                sourceMek.getMekMaterialBlock().getRegularEntries(),
                targetMek.getMekMaterialBlock().getRegularEntries(),
                true,
                sourceSpriteGroupIndex,
                targetSpriteGroupIndex,
                null,
                "regularEntries.sprite"
        );
        assertIndexedGroupsRemappedIfPresent(
                sourceMek.getMekMaterialBlock().getTrailingEntries(),
                targetMek.getMekMaterialBlock().getTrailingEntries(),
                true,
                sourceSpriteGroupIndex,
                targetSpriteGroupIndex,
                null,
                "trailingEntries.sprite"
        );
    }

    private void assertMaterialSeGroupsRemapped(AkaoGraftResult result) {
        Mek sourceMek = result.getJinkiPackage().getAkaoMek();
        Mek targetMek = result.getReboundAkaoMek();
        Assertions.assertNotNull(sourceMek);
        Assertions.assertNotNull(targetMek);
        Assertions.assertNotNull(sourceMek.getMekMaterialBlock());
        Assertions.assertNotNull(targetMek.getMekMaterialBlock());

        for (Map.Entry<Integer, Integer> mapping : result.getGrpAppendPlan().getSourceSeGroupIndexToTargetIndex().entrySet()) {
            Integer sourceSeGroupIndex = mapping.getKey();
            Integer targetSeGroupIndex = mapping.getValue();
            if (sourceSeGroupIndex == null || targetSeGroupIndex == null) {
                continue;
            }
            if (!containsAnyIndexedGroup(
                    sourceMek.getMekMaterialBlock().getEntries(),
                    sourceMek.getMekMaterialBlock().getRegularEntries(),
                    sourceMek.getMekMaterialBlock().getTrailingEntries(),
                    false,
                    sourceSeGroupIndex
            )) {
                continue;
            }

            Map<Integer, Integer> itemMapping = result.getGrpAppendPlan()
                    .getSourceSeItemIndexToTargetIndexByGroup()
                    .get(sourceSeGroupIndex);

            assertIndexedGroupsRemappedIfPresent(
                    sourceMek.getMekMaterialBlock().getEntries(),
                    targetMek.getMekMaterialBlock().getEntries(),
                    false,
                    sourceSeGroupIndex,
                    targetSeGroupIndex,
                    itemMapping,
                    "entries.se[" + sourceSeGroupIndex + "->" + targetSeGroupIndex + "]"
            );
            assertIndexedGroupsRemappedIfPresent(
                    sourceMek.getMekMaterialBlock().getRegularEntries(),
                    targetMek.getMekMaterialBlock().getRegularEntries(),
                    false,
                    sourceSeGroupIndex,
                    targetSeGroupIndex,
                    itemMapping,
                    "regularEntries.se[" + sourceSeGroupIndex + "->" + targetSeGroupIndex + "]"
            );
            assertIndexedGroupsRemappedIfPresent(
                    sourceMek.getMekMaterialBlock().getTrailingEntries(),
                    targetMek.getMekMaterialBlock().getTrailingEntries(),
                    false,
                    sourceSeGroupIndex,
                    targetSeGroupIndex,
                    itemMapping,
                    "trailingEntries.se[" + sourceSeGroupIndex + "->" + targetSeGroupIndex + "]"
            );
        }
    }

    private void assertIndexedGroupsRemappedIfPresent(
            List<Mek.MekMaterialBlock.PluginEntry> sourceEntries,
            List<Mek.MekMaterialBlock.PluginEntry> targetEntries,
            boolean useSpriteGroups,
            int sourceGroupIndex,
            int targetGroupIndex,
            Map<Integer, Integer> itemMapping,
            String label
    ) {
        if (!containsAnyIndexedGroup(sourceEntries, useSpriteGroups, sourceGroupIndex)) {
            return;
        }
        assertPluginEntryIndexedGroupsRemapped(
                sourceEntries,
                targetEntries,
                useSpriteGroups,
                sourceGroupIndex,
                targetGroupIndex,
                itemMapping,
                label
        );
    }

    private void assertPluginEntryIndexedGroupsRemapped(
            List<Mek.MekMaterialBlock.PluginEntry> sourceEntries,
            List<Mek.MekMaterialBlock.PluginEntry> targetEntries,
            boolean useSpriteGroups,
            int sourceGroupIndex,
            int targetGroupIndex,
            Map<Integer, Integer> itemMapping,
            String label
    ) {
        if (sourceEntries == null || targetEntries == null) {
            return;
        }

        Assertions.assertEquals(sourceEntries.size(), targetEntries.size(), label + " size mismatch");
        boolean sawSourceGroup = false;
        for (int i = 0; i < sourceEntries.size(); i++) {
            Mek.MekMaterialBlock.PluginEntry sourceEntry = sourceEntries.get(i);
            Mek.MekMaterialBlock.PluginEntry targetEntry = targetEntries.get(i);
            if (sourceEntry == null || targetEntry == null) {
                Assertions.assertEquals(sourceEntry, targetEntry, label + "[" + i + "] null mismatch");
                continue;
            }

            List<int[]> sourceGroups = useSpriteGroups ? sourceEntry.getSpriteGroups() : sourceEntry.getSeGroups();
            List<int[]> targetGroups = useSpriteGroups ? targetEntry.getSpriteGroups() : targetEntry.getSeGroups();
            int[] sourceItems = getIndexedGroupItems(sourceGroups, sourceGroupIndex);
            int[] targetItems = getIndexedGroupItems(targetGroups, targetGroupIndex);

            if (!isNullOrEmpty(sourceItems)) {
                sawSourceGroup = true;
                Assertions.assertArrayEquals(
                        remapItems(sourceItems, itemMapping),
                        targetItems,
                        label + "[" + i + "] source group " + sourceGroupIndex + " should remap to target group " + targetGroupIndex
                );
            }

            if (sourceGroupIndex != targetGroupIndex) {
                Assertions.assertTrue(
                        isNullOrEmpty(getIndexedGroupItems(targetGroups, sourceGroupIndex)),
                        label + "[" + i + "] should not retain source group " + sourceGroupIndex
                );
            }
        }
        Assertions.assertTrue(sawSourceGroup, label + " should contain at least one source group " + sourceGroupIndex + " before remap");
    }

    private int[] getVoiceGroupItems(Mek.MekMaterialBlock.PluginEntry entry, int groupIndex) {
        if (entry == null || entry.getVoiceGroups() == null || groupIndex < 0 || groupIndex >= entry.getVoiceGroups().size()) {
            return null;
        }
        return entry.getVoiceGroups().get(groupIndex);
    }

    private int[] getIndexedGroupItems(List<int[]> groups, int groupIndex) {
        if (groups == null || groupIndex < 0 || groupIndex >= groups.size()) {
            return null;
        }
        return groups.get(groupIndex);
    }

    private boolean containsAnyIndexedGroup(
            List<Mek.MekMaterialBlock.PluginEntry> entries,
            List<Mek.MekMaterialBlock.PluginEntry> regularEntries,
            List<Mek.MekMaterialBlock.PluginEntry> trailingEntries,
            boolean useSpriteGroups,
            int groupIndex
    ) {
        return containsAnyIndexedGroup(entries, useSpriteGroups, groupIndex)
                || containsAnyIndexedGroup(regularEntries, useSpriteGroups, groupIndex)
                || containsAnyIndexedGroup(trailingEntries, useSpriteGroups, groupIndex);
    }

    private boolean containsAnyIndexedGroup(
            List<Mek.MekMaterialBlock.PluginEntry> entries,
            boolean useSpriteGroups,
            int groupIndex
    ) {
        if (entries == null) {
            return false;
        }
        for (Mek.MekMaterialBlock.PluginEntry entry : entries) {
            if (entry == null) {
                continue;
            }
            List<int[]> groups = useSpriteGroups ? entry.getSpriteGroups() : entry.getSeGroups();
            if (!isNullOrEmpty(getIndexedGroupItems(groups, groupIndex))) {
                return true;
            }
        }
        return false;
    }

    private int[] remapItems(int[] sourceItems, Map<Integer, Integer> itemMapping) {
        if (sourceItems == null) {
            return null;
        }
        if (itemMapping == null || itemMapping.isEmpty()) {
            return sourceItems.clone();
        }

        int[] remapped = new int[sourceItems.length];
        for (int i = 0; i < sourceItems.length; i++) {
            remapped[i] = itemMapping.getOrDefault(sourceItems[i], sourceItems[i]);
        }
        return remapped;
    }

    private boolean isNullOrEmpty(int[] items) {
        return items == null || items.length == 0;
    }

    private int findSourceSpriteGroupIndex(AkaoGraftResult result, int targetSpriteGroupIndex) {
        return result.getGrpAppendPlan().getSourceSpriteGroupIndexToTargetIndex().entrySet().stream()
                .filter(entry -> entry.getValue() != null && entry.getValue() == targetSpriteGroupIndex)
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("缂哄皯 SpriteGroup 婧?-> 鐩爣鏄犲皠锛宼arget=" + targetSpriteGroupIndex));
    }

    private void assertWazaGroupParamsMatchActualWaz(AkaoGraftResult result) {
        Map<Integer, Integer> resolvedTargets = new LinkedHashMap<>(result.getGrpAppendPlan().getSourceWazGroupIndexToTargetIndex());

        // 主 AKAO 自己也纳入统一校验。
        Integer sourceMainWazIndex = result.getImportPlan().getSourceWazIndexByFileName().get("akao.waz");
        if (sourceMainWazIndex != null) {
            resolvedTargets.put(sourceMainWazIndex, result.getGrpAppendPlan().getWazaGroupIndex());
        }

        for (Map.Entry<Integer, Integer> mapping : resolvedTargets.entrySet()) {
            Integer sourceIndex = mapping.getKey();
            Integer targetIndex = mapping.getValue();
            String sourceFileName = findSourceWazFileName(result, sourceIndex);
            Assertions.assertNotNull(sourceFileName, "缺少源 Waz 文件名映射: " + sourceIndex);

            Waz actualWaz = findWazByFileName(result.getBsdxBaseline().getWazByFileName(), sourceFileName);
            if (actualWaz == null) {
                actualWaz = findWazByFileName(result.getJinkiPackage().getWazByFileName(), sourceFileName);
            }
            Assertions.assertNotNull(actualWaz, "缺少用于校验 param 的 Waz 文件: " + sourceFileName);

            int expectedSkillCount = actualWaz.getSkillList() == null ? 0 : actualWaz.getSkillList().size();
            int actualParam = result.getBsdxBaseline().getWazaGroupGrp().getWazaList().get(targetIndex).getParam();
            Assertions.assertEquals(
                    expectedSkillCount,
                    actualParam,
                    "WazaGroup.param 与实际 waz 技能数不一致: sourceIndex=" + sourceIndex + ", file=" + sourceFileName + ", targetIndex=" + targetIndex
            );
        }
    }

    private String findSourceWazFileName(AkaoGraftResult result, Integer sourceIndex) {
        for (Map.Entry<String, Integer> entry : result.getImportPlan().getSourceWazIndexByFileName().entrySet()) {
            if (Objects.equals(entry.getValue(), sourceIndex)) {
                return entry.getKey();
            }
        }
        return null;
    }

    private Waz findWazByFileName(Map<String, Waz> wazByFileName, String fileName) {
        if (wazByFileName == null) {
            return null;
        }
        String normalizedTarget = fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
        for (Map.Entry<String, Waz> entry : wazByFileName.entrySet()) {
            String candidate = entry.getKey() == null ? "" : entry.getKey().trim().toLowerCase(Locale.ROOT);
            if (candidate.equals(normalizedTarget)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void assertWeaponEquipDatExpanded(AkaoGraftResult result) {
        Assertions.assertNotNull(result.getJinkiPackage().getWeaponEquipDat());
        Assertions.assertNotNull(result.getBsdxBaseline().getWeaponEquipDat());

        int baselineRows = result.getBsdxBaseline().getWeaponEquipDat().getData().size();
        int sourceRows = result.getJinkiPackage().getWeaponEquipDat().getData().size();
        Assertions.assertEquals(103, baselineRows);
        Assertions.assertEquals(104, sourceRows);

        Path rootOutput = result.getImportedAssetSet().getOutputRootDir().resolve("WeaponEquip.dat");
        Assertions.assertTrue(Files.exists(rootOutput));

        Dat parsed = parseDat(rootOutput);
        Assertions.assertEquals(sourceRows, parsed.getData().size());
        Assertions.assertEquals(
                result.getJinkiPackage().getWeaponEquipDat().getData().get(sourceRows - 1),
                parsed.getData().get(sourceRows - 1)
        );
    }

    private Dat parseDat(Path path) {
        try {
            ResponseDTO<?> dto = new BsdxBinService().parse(path.toString(), "windows-31j");
            return (Dat) dto.getData();
        } catch (Exception e) {
            throw new AssertionError("failed to parse dat: " + path, e);
        }
    }

    private void assertExeContainsPatchedMekaRuntimeTableBounds(AkaoGraftResult result) {
        Assertions.assertNotNull(result.getExePatchPlan());
        Assertions.assertNotNull(result.getExePatchPlan().getOutputExePath());
        try {
            byte[] exe = Files.readAllBytes(result.getExePatchPlan().getOutputExePath());
            Assertions.assertEquals(0x68, exe[0x056CE4] & 0xFF, "0x056CE4 should patch runtime meka table prealloc from 103 to 104");
            int imm32 = readLittleEndianInt(exe, 0x056F45);
            Assertions.assertEquals(0x0006E180, imm32, "0x056F45 should patch runtime meka table init loop bound to 104 * 4336");
            int weaponEquipBound = readLittleEndianInt(exe, 0x05498B);
            Assertions.assertEquals(0x000016C0, weaponEquipBound, "0x05498B should patch WeaponEquip fill hard cap from 56 * 103 to 56 * 104");
            Assertions.assertArrayEquals(
                    new byte[]{(byte) 0x90, (byte) 0x90, (byte) 0xEB, (byte) 0x06},
                    java.util.Arrays.copyOfRange(exe, 0x20C1FD, 0x20C201),
                    "0x20C1FD should bypass sub_60CC20 failure return in sub_60CDF0 for direct AT/FC requests"
            );
            Assertions.assertArrayEquals(
                    new byte[]{(byte) 0x90, (byte) 0x90, (byte) 0xEB, (byte) 0x06},
                    java.util.Arrays.copyOfRange(exe, 0x20C2CD, 0x20C2D1),
                    "0x20C2CD should bypass sub_60CC20 failure return in sub_60CEC0 for table-driven combat voice requests"
            );
        } catch (Exception e) {
            throw new AssertionError("failed to read patched exe for runtime meka table assertions", e);
        }
    }
}
