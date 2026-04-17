package com.giga.nexas.jinki;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.v2.Jinki2BsdxTransferV2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
@Slf4j
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
        assertAuxiliaryWazGraftAndRebind(result);
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
        assertMenuSpmImageChains(result, request);
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

    @Test
    public void testRunAkaoGraftPipelineV2() {
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPatchMenuData(true);
        AkaoGraftResult akaoGraftResultV2 = Jinki2BsdxTransferV2.process(request);
        log.info("jinki v2 result = {}", 1);
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

    private void assertMenuSpmImageChains(AkaoGraftResult result, AkaoGraftRequest request) {
        int visibleSlotIndex = 25 - 1;
        List<Object> selectRow = result.getPatchedSelectMekaMenuDat().getData().get(visibleSlotIndex);
        int targetMekaIndex = asInt(selectRow.get(0));
        int selectMenuAnimIndex = asInt(selectRow.get(1));
        int pilotAnimIndex = findRowIndexByFirstColumn(result.getPatchedMekaPilotDat(), targetMekaIndex);

        Assertions.assertEquals(29, pilotAnimIndex, "menu slot 25 should derive MekaPilot.spm anim index through MekaPilot.dat");
        Assertions.assertEquals(18, selectMenuAnimIndex, "menu slot 25 should derive SelectMekaMenuMeka.spm anim index from SelectMekaMenu.dat column 2");

        List<Integer> pilotPages = collectAnimPageIndices(result.getPatchedMekaPilotSpm(), pilotAnimIndex);
        List<Integer> selectMenuPages = collectAnimPageIndices(result.getPatchedSelectMekaMenuMekaSpm(), selectMenuAnimIndex);

        Assertions.assertEquals(List.of(59, 60), pilotPages, "derived MekaPilot anim should keep its existing page chain");
        Assertions.assertEquals(List.of(36, 37), selectMenuPages, "derived SelectMekaMenuMeka anim should keep its existing page chain");

        assertPageImageChain(
                result.getPatchedMekaPilotSpm(),
                pilotAnimIndex,
                0,
                pilotPages.get(0),
                "MOD_001_HELL_AKAO_001.png",
                request.getExternalStaticAssetRoot(),
                result.getImportedAssetSet().getOutputRootDir(),
                LayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR
        );
        assertPageImageChain(
                result.getPatchedMekaPilotSpm(),
                pilotAnimIndex,
                1,
                pilotPages.get(1),
                "MOD_001_HELL_MEKA_AKAO_001.png",
                request.getExternalStaticAssetRoot(),
                result.getImportedAssetSet().getOutputRootDir(),
                LayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR
        );
        assertPageImageChain(
                result.getPatchedSelectMekaMenuMekaSpm(),
                selectMenuAnimIndex,
                0,
                selectMenuPages.get(0),
                "MOD_001_SelectMekaMenuMeka_Moribito_2_001.png",
                request.getExternalStaticAssetRoot(),
                result.getImportedAssetSet().getOutputRootDir(),
                LayoutPolicy.ORIGIN_CENTER
        );
        assertPageImageChain(
                result.getPatchedSelectMekaMenuMekaSpm(),
                selectMenuAnimIndex,
                1,
                selectMenuPages.get(1),
                "MOD_001_SelectMekaMenuMeka_Moribito_2_002.png",
                request.getExternalStaticAssetRoot(),
                result.getImportedAssetSet().getOutputRootDir(),
                LayoutPolicy.ORIGIN_CENTER
        );
    }

    private int findRowIndexByFirstColumn(Dat dat, int firstColumnValue) {
        Assertions.assertNotNull(dat);
        Assertions.assertNotNull(dat.getData());
        for (int i = 0; i < dat.getData().size(); i++) {
            List<Object> row = dat.getData().get(i);
            if (row != null && !row.isEmpty() && asInt(row.get(0)) == firstColumnValue) {
                return i;
            }
        }
        return -1;
    }

    private List<Integer> collectAnimPageIndices(Spm spm, int animIndex) {
        Assertions.assertNotNull(spm);
        Assertions.assertNotNull(spm.getAnimData());
        Assertions.assertTrue(animIndex >= 0 && animIndex < spm.getAnimData().size(), "derived anim index out of range");

        Spm.SPMAnimData anim = spm.getAnimData().get(animIndex);
        Assertions.assertNotNull(anim);
        Assertions.assertNotNull(anim.getPatData());

        List<Integer> pages = new ArrayList<>();
        for (Spm.SPMPatData patData : anim.getPatData()) {
            Assertions.assertNotNull(patData);
            Assertions.assertNotNull(patData.getPageNo());
            pages.addAll(patData.getPageNo());
        }
        return pages;
    }

    private void assertPageImageChain(
            Spm spm,
            int animIndex,
            int patIndex,
            int pageIndex,
            String expectedImageName,
            Path externalStaticAssetRoot,
            Path outputRoot,
            LayoutPolicy layoutPolicy
    ) {
        ImageSize expectedSize = readImageSize(externalStaticAssetRoot.resolve(expectedImageName));
        ExpectedRect expectedRect = calculateExpectedRect(spm, animIndex, patIndex, expectedSize, layoutPolicy);

        Assertions.assertTrue(pageIndex >= 0 && pageIndex < spm.getPageData().size(), "page index out of range");
        Spm.SPMPageData page = spm.getPageData().get(pageIndex);
        Assertions.assertNotNull(page);
        Assertions.assertEquals(1, page.getNumChipData());
        Assertions.assertNotNull(page.getChipData());
        Assertions.assertEquals(1, page.getChipData().size());

        Spm.SPMChipData chip = page.getChipData().get(0);
        Assertions.assertNotNull(chip);
        Assertions.assertNotNull(chip.getImageNo());
        Assertions.assertTrue(chip.getImageNo() >= 0 && chip.getImageNo() < spm.getImageData().size(), "chip imageNo out of range");
        Assertions.assertEquals(expectedImageName, spm.getImageData().get(chip.getImageNo()).getImageName());

        Assertions.assertEquals(expectedSize.width(), page.getPageWidth());
        Assertions.assertEquals(expectedSize.height(), page.getPageHeight());
        Assertions.assertEquals(expectedSize.width(), chip.getChipWidth());
        Assertions.assertEquals(expectedSize.height(), chip.getChipHeight());
        assertRectSize(page.getPageRect(), expectedSize);
        assertRectSize(chip.getDstRect(), expectedSize);
        assertSameRect(page.getPageRect(), chip.getDstRect());
        assertRectEquals(expectedRect, page.getPageRect());
        Assertions.assertEquals(0, chip.getSrcRect().getLeft());
        Assertions.assertEquals(0, chip.getSrcRect().getTop());
        Assertions.assertEquals(expectedSize.width(), chip.getSrcRect().getRight());
        Assertions.assertEquals(expectedSize.height(), chip.getSrcRect().getBottom());

        Assertions.assertTrue(Files.exists(outputRoot.resolve(expectedImageName)), expectedImageName + " should be copied into output dir");
    }

    private ExpectedRect calculateExpectedRect(Spm spm, int animIndex, int patIndex, ImageSize imageSize, LayoutPolicy layoutPolicy) {
        if (layoutPolicy == LayoutPolicy.ORIGIN_CENTER) {
            int left = -Math.floorDiv(imageSize.width(), 2);
            int top = -Math.floorDiv(imageSize.height(), 2);
            return new ExpectedRect(left, top, left + imageSize.width(), top + imageSize.height());
        }

        List<AnchorSample> samples = collectMekaPilotAnchorSamples(spm, animIndex, patIndex);
        Assertions.assertFalse(samples.isEmpty(), "MekaPilot anchor sample pool should not be empty");
        double centerX = median(samples.stream().map(AnchorSample::centerX).toList());
        double bottom = median(samples.stream().map(AnchorSample::bottom).toList());
        int left = (int) Math.round(centerX - imageSize.width() / 2.0);
        int rectBottom = (int) Math.round(bottom);
        return new ExpectedRect(left, rectBottom - imageSize.height(), left + imageSize.width(), rectBottom);
    }

    private List<AnchorSample> collectMekaPilotAnchorSamples(Spm spm, int animIndex, int patIndex) {
        List<AnchorSample> samples = new ArrayList<>();
        for (int i = 0; i < spm.getAnimData().size() && i < animIndex; i++) {
            Spm.SPMAnimData anim = spm.getAnimData().get(i);
            if (anim == null || anim.getPatData() == null || patIndex < 0 || patIndex >= anim.getPatData().size()) {
                continue;
            }
            Spm.SPMPatData patData = anim.getPatData().get(patIndex);
            if (patData == null || patData.getPageNo() == null || patData.getPageNo().isEmpty()) {
                continue;
            }
            int pageNo = patData.getPageNo().get(0);
            if (pageNo < 0 || pageNo >= spm.getPageData().size()) {
                continue;
            }
            Spm.SPMPageData page = spm.getPageData().get(pageNo);
            if (page == null || page.getChipData() == null || page.getChipData().size() != 1) {
                continue;
            }
            Spm.SPMRect rect = page.getPageRect();
            samples.add(new AnchorSample((rect.getLeft() + rect.getRight()) / 2.0, rect.getBottom()));
        }
        return samples;
    }

    private double median(List<Double> values) {
        List<Double> sorted = values.stream().sorted().toList();
        int mid = sorted.size() / 2;
        if (sorted.size() % 2 == 1) {
            return sorted.get(mid);
        }
        return (sorted.get(mid - 1) + sorted.get(mid)) / 2.0;
    }

    private void assertRectSize(Spm.SPMRect rect, ImageSize expectedSize) {
        Assertions.assertNotNull(rect);
        Assertions.assertEquals(expectedSize.width(), rect.getRight() - rect.getLeft());
        Assertions.assertEquals(expectedSize.height(), rect.getBottom() - rect.getTop());
    }

    private void assertSameRect(Spm.SPMRect left, Spm.SPMRect right) {
        Assertions.assertEquals(left.getLeft(), right.getLeft());
        Assertions.assertEquals(left.getTop(), right.getTop());
        Assertions.assertEquals(left.getRight(), right.getRight());
        Assertions.assertEquals(left.getBottom(), right.getBottom());
    }

    private void assertRectEquals(ExpectedRect expected, Spm.SPMRect actual) {
        Assertions.assertEquals(expected.left(), actual.getLeft());
        Assertions.assertEquals(expected.top(), actual.getTop());
        Assertions.assertEquals(expected.right(), actual.getRight());
        Assertions.assertEquals(expected.bottom(), actual.getBottom());
    }

    private ImageSize readImageSize(Path path) {
        Assertions.assertTrue(Files.exists(path), "missing expected PNG: " + path);
        try {
            BufferedImage image = ImageIO.read(path.toFile());
            Assertions.assertNotNull(image, "failed to read PNG: " + path);
            return new ImageSize(image.getWidth(), image.getHeight());
        } catch (IOException e) {
            throw new AssertionError("failed to read PNG: " + path, e);
        }
    }

    private record ImageSize(int width, int height) {
    }

    private record ExpectedRect(int left, int top, int right, int bottom) {
    }

    private record AnchorSample(double centerX, double bottom) {
    }

    private enum LayoutPolicy {
        MEKA_PILOT_MEDIAN_ANCHOR,
        ORIGIN_CENTER
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

            Waz actualWaz = parseOutputWazIfExists(result, sourceFileName);
            if (actualWaz == null) {
                actualWaz = findWazByFileName(result.getBsdxBaseline().getWazByFileName(), sourceFileName);
            }
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

    private Waz parseOutputWazIfExists(AkaoGraftResult result, String fileName) {
        if (result == null || result.getImportedAssetSet() == null || result.getImportedAssetSet().getOutputRootDir() == null) {
            return null;
        }
        Path output = result.getImportedAssetSet().getOutputRootDir().resolve(fileName);
        if (!Files.exists(output)) {
            return null;
        }
        return parseWaz(output);
    }

    private void assertAuxiliaryWazGraftAndRebind(AkaoGraftResult result) {
        Assertions.assertTrue(
                result.getImportPlan().getRequiredWazFiles().stream().anyMatch(file -> "bomb.waz".equalsIgnoreCase(file)),
                "Bomb.waz should be pulled in through recursive Tama references"
        );

        Path outputRoot = result.getImportedAssetSet().getOutputRootDir();
        Path bombPath = outputRoot.resolve("bomb.waz");
        Assertions.assertTrue(Files.exists(bombPath), "merged Bomb.waz should be written to output");
        Waz bombWaz = parseWaz(bombPath);
        Assertions.assertEquals(136, bombWaz.getSkillList().size(), "Bomb.waz should keep BSDX skills and append JINKI tail skills 133..135");

        Integer targetBombGroup = result.getGrpAppendPlan().getSourceWazGroupIndexToTargetIndex().get(7);
        Assertions.assertNotNull(targetBombGroup, "source WazaGroup[7]=BOMB should have target mapping");

        for (String fileName : List.of("Tama01.waz", "Tama02.waz", "Tama03.waz", "Tama04.waz", "Tama05.waz")) {
            Path path = outputRoot.resolve(fileName);
            Assertions.assertTrue(Files.exists(path), fileName + " should be written to output");
            Waz waz = parseWaz(path);
            List<WazRef> refs = collectWazRefs(waz);
            Assertions.assertTrue(refs.stream().anyMatch(ref -> ref.groupIndex() == targetBombGroup) || fileName.equals("Tama01.waz"));
        }

        assertHasWazRef(parseWaz(outputRoot.resolve("Tama02.waz")), targetBombGroup, 134);
        assertHasWazRef(parseWaz(outputRoot.resolve("Tama04.waz")), targetBombGroup, 133);
        assertHasWazRef(parseWaz(outputRoot.resolve("Tama05.waz")), targetBombGroup, 135);
        Assertions.assertTrue(
                Files.exists(outputRoot.resolve("bomb_004_0002.png")),
                "Bomb.waz[134] should pull bomb.spm image bomb_004_0002.png into output"
        );
    }

    private void assertHasWazRef(Waz waz, int expectedGroup, int expectedSequence) {
        Assertions.assertTrue(
                collectWazRefs(waz).stream().anyMatch(ref -> ref.groupIndex() == expectedGroup && ref.sequenceNo() == expectedSequence),
                "expected CEventWazaSelect ref group=" + expectedGroup + ", sequence=" + expectedSequence
        );
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

    private Waz parseWaz(Path path) {
        try {
            ResponseDTO<?> dto = new BsdxBinService().parse(path.toString(), "windows-31j");
            return (Waz) dto.getData();
        } catch (Exception e) {
            throw new AssertionError("failed to parse waz: " + path, e);
        }
    }

    private List<WazRef> collectWazRefs(Waz waz) {
        List<WazRef> refs = new ArrayList<>();
        if (waz == null || waz.getSkillList() == null) {
            return refs;
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
                        collectWazRefsFromObject(object, refs);
                    }
                }
            }
        }
        return refs;
    }

    private void collectWazRefsFromObject(SkillInfoObject object, List<WazRef> refs) {
        if (object == null) {
            return;
        }
        if (object instanceof CEventWazaSelect select) {
            refs.add(new WazRef(select.getWazFileNo(), select.getWazSequenceNo()));
        }

        for (Field field : getAllFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType()) || !field.getName().endsWith("UnitList")) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object units = field.get(object);
                if (!(units instanceof List<?> unitList)) {
                    continue;
                }
                for (Object unit : unitList) {
                    SkillInfoObject data = getNestedUnitData(unit);
                    if (data != null) {
                        collectWazRefsFromObject(data, refs);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new AssertionError("failed to collect nested waz refs", e);
            }
        }
    }

    private SkillInfoObject getNestedUnitData(Object unit) {
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

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private record WazRef(int groupIndex, int sequenceNo) {
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
