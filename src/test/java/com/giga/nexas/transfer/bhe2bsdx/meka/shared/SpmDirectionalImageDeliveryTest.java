package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output.OutputBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Regression for the native 7655A0 direction-page selection, through its actual delivery consumers. */
class SpmDirectionalImageDeliveryTest {
    @TempDir Path temp;
    private final BsdxBinService service = new BsdxBinService();

    @Test void allNativeDirectionAndMirrorModesCollectOnlyTheirReachableImages() throws Exception {
        // Independent literal unions from 7655A0 and the EXE table at 867B58.
        // rotate=4 uses integer 3600/16/10 = 22 degree bins, so no mirror can reach page +16.
        int[][] counts = {{1,1,-1}, {2,1,-1}, {4,3,2}, {8,5,3},
                {17,9,5}, {24,13,7}, {40,21,11}, {1,1,1}};
        for (int rotate = 0; rotate < counts.length; rotate++) {
            for (int mirror = 0; mirror < counts[rotate].length; mirror++) {
                int count = counts[rotate][mirror];
                if (count < 0) continue;
                Spm data = spm(rotate, mirror, 2, 45);
                Set<String> expected = IntStream.range(2, 2 + count).mapToObj(i -> "image-" + i + ".png")
                        .collect(Collectors.toCollection(LinkedHashSet::new));
                for (String branch : List.of("common", "tsukuyomi", "followup")) {
                    assertEquals(expected, collect(branch, data), branch + " rotate=" + rotate + " mirror=" + mirror);
                }
            }
        }
    }

    @Test void heatProctorDirectionsIncludeTheSecondImageWithoutCopyingAnUnreachablePage() throws Exception {
        Spm data = heatFixture();
        for (String branch : List.of("common", "tsukuyomi", "followup")) {
            assertEquals(Set.of("bhe_base.png", "bhe_direction.png"), collect(branch, data), branch);
        }
    }

    @Test void rotateSevenDoesNotInterpretMirrorAndEmptyPagesStayUnreferenced() throws Exception {
        Spm data = spm(7, 99, 2, 8);
        data.setPatPageNum(2);
        data.getAnimData().get(0).getPatData().get(0).setPageNo(List.of(2, -1));
        for (String branch : List.of("common", "tsukuyomi", "followup")) {
            assertEquals(Set.of("image-2.png"), collect(branch, data), branch);
        }
    }

    @Test void negativeBaseIsFilteredAfterNativeDirectionSelection() throws Exception {
        // 76563D loads raw -1 without a guard. rotate=1/mirror=0 at 180 degrees adds 1 and draws page 0.
        // Mirrored 24-way animation reaches raw -1 plus offsets 0..12: only resulting pages 0..11 draw.
        for (int[] example : List.of(new int[]{1, 0, 1}, new int[]{5, 1, 12},
                new int[]{7, 0, 0}, new int[]{1, 1, 0})) {
            Spm data = spm(example[0], example[1], -1, 16);
            Set<String> expected = IntStream.range(0, example[2]).mapToObj(i -> "image-" + i + ".png")
                    .collect(Collectors.toSet());
            for (String branch : List.of("common", "tsukuyomi", "followup")) {
                assertEquals(expected, collect(branch, data),
                        branch + " raw base=-1 rotate=" + example[0] + " mirror=" + example[1]);
            }
        }
    }

    @Test void validatorAuditsReachableImageFromNegativeBaseWithoutMakingMissingMediaFatal() throws Exception {
        Path output = Files.createDirectories(temp.resolve("negative-base-validate"));
        var baseline = baseline(spm(1, 0, -1, 2));
        var assets = new TsukuyomiImportedAssetSet(); assets.setOutputRootDir(output);
        Path spmFile = output.resolve("Tama.spm"), wazFile = output.resolve("fixture.waz");
        service.generate(spmFile.toString(), baseline.getSpmByFileName().get("Tama.spm"), "windows-31j");
        service.generate(wazFile.toString(), baseline.getWazByFileName().get("fixture.waz"), "windows-31j");
        service.generate(output.resolve("SpriteGroup.grp").toString(), baseline.getSpriteGroupGrp(), "windows-31j");
        assets.getCopiedSpmFiles().add(spmFile); assets.getGeneratedWazFiles().add(wazFile);
        var request = new TsukuyomiGraftRequest(); request.setBsdxStaticAssetRoot(temp.resolve("missing-original-negative"));
        request.setJinkiGeneratedAssetDir(temp.resolve("missing-inherited-negative"));
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(request, assets));
        assertEquals(Set.of("image-0.png"), assets.getUnresolvedImageReferences().keySet());
        assertTrue(assets.getMissingAssets().isEmpty());
    }

    @Test void nativeNonRenderingMirrorModesDoNotInventImageDependencies() throws Exception {
        for (int mirror : List.of(-1, 3, 99)) {
            for (String branch : List.of("common", "tsukuyomi", "followup")) {
                assertTrue(collect(branch, spm(5, mirror, 2, 45)).isEmpty(), branch + " mirror=" + mirror);
            }
        }
    }

    @Test void undefinedDirectionModesAndOutOfRangeDerivedPagesAreRejected() throws Exception {
        for (Spm data : List.of(spm(-1, 0, 0, 45), spm(8, 0, 0, 45),
                spm(0, 2, 0, 45), spm(1, 2, 0, 45), spm(5, 1, 0, 12))) {
            for (String branch : List.of("common", "tsukuyomi", "followup")) {
                InvocationTargetException failure = assertThrows(InvocationTargetException.class, () -> collect(branch, data));
                assertInstanceOf(IllegalStateException.class, failure.getCause());
            }
        }
    }

    @Test void commonOutputCopiesBothExactDonorsAndDoesNotClaimAnUnusedImage() throws Exception {
        Path donor = Files.createDirectories(temp.resolve("donor"));
        Path output = Files.createDirectories(temp.resolve("copied"));
        byte[] base = {1,2,3}, direction = {4,5,6,7}, unused = {8,9};
        Files.write(donor.resolve("base.png"), base);
        Files.write(donor.resolve("direction.png"), direction);
        Files.write(donor.resolve("unused.png"), unused);
        var request = new TsukuyomiGraftRequest(); request.setExternalStaticAssetRoot(donor);
        request.setBsdxStaticAssetRoot(temp.resolve("no-original"));
        var baseline = baseline(heatFixture());
        var assets = new TsukuyomiImportedAssetSet(); assets.setOutputRootDir(output);
        copyCommon(request, baseline, output, assets);
        assertArrayEquals(base, Files.readAllBytes(output.resolve("bhe_base.png")));
        assertTrue(Files.isRegularFile(output.resolve("bhe_direction.png")), "direction-only donor must be delivered");
        assertArrayEquals(direction, Files.readAllBytes(output.resolve("bhe_direction.png")));
        assertFalse(Files.exists(output.resolve("bhe_unused.png")));
        assertEquals(2, assets.getCopiedImageFiles().size());
        assertTrue(assets.getMissingAssets().isEmpty());
        assertArrayEquals(direction, Files.readAllBytes(donor.resolve("direction.png")));
    }

    @Test void validatorAuditsMissingDirectionalImagesWithoutMakingThemFatal() throws Exception {
        Path output = Files.createDirectories(temp.resolve("validate"));
        var baseline = baseline(heatFixture());
        var assets = new TsukuyomiImportedAssetSet(); assets.setOutputRootDir(output);
        Path spmFile = output.resolve("Tama.spm"), wazFile = output.resolve("fixture.waz");
        service.generate(spmFile.toString(), baseline.getSpmByFileName().get("Tama.spm"), "windows-31j");
        service.generate(wazFile.toString(), baseline.getWazByFileName().get("fixture.waz"), "windows-31j");
        service.generate(output.resolve("SpriteGroup.grp").toString(), baseline.getSpriteGroupGrp(), "windows-31j");
        assets.getCopiedSpmFiles().add(spmFile); assets.getGeneratedWazFiles().add(wazFile);
        var request = new TsukuyomiGraftRequest(); request.setBsdxStaticAssetRoot(temp.resolve("missing-original"));
        request.setJinkiGeneratedAssetDir(temp.resolve("missing-inherited"));
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(request, assets));
        assertEquals(Set.of("bhe_base.png", "bhe_direction.png"), assets.getUnresolvedImageReferences().keySet());
        assertTrue(assets.getMissingAssets().isEmpty());
    }

    @Test void realBheOrangeProctorHasOneBaseImageAndAnotherImageOnlyOnDirectionPages() throws Exception {
        var raw = (com.giga.nexas.dto.bhe.spm.Spm) new BheBinService()
                .parse("src/main/resources/game/bhe/spm/tama.spm", "windows-31j").getData();
        var source = raw.getAnimData().get(275);
        assertEquals("プロクター橙", source.getAnimName());
        assertEquals(5, source.getAnimRotateDirection()); assertEquals(1, source.getAnimReverseDirection());
        assertEquals(List.of(1196), source.getPatData().get(0).getPageNo());
        // Use the real page/image graph with the final prefix; do not copy a source binary into test resources.
        Spm data = new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.spm.BheToBsdxSpmConverter().convert(raw);
        data.getImageData().forEach(image -> image.setImageName("bhe_" + image.getImageName()));
        data.setAnimData(List.of(data.getAnimData().get(275))); data.setNumAnimData(1);
        assertEquals(Set.of("bhe_tama_bit041_0002.png", "bhe_tama_bit041_0001.png"), collect("common", data));
        var request = new TsukuyomiGraftRequest();
        Path sourceImage = request.getExternalStaticAssetRoot().resolve("tama_bit041_0001.png");
        byte[] expected = Files.readAllBytes(sourceImage);
        assertEquals("fd9bd82d98a5030a277870336dcd56d92ec40f4bb9b34b39b380faa558ca1b14",
                java.util.HexFormat.of().formatHex(java.security.MessageDigest.getInstance("SHA-256").digest(expected)));
        Path output = Files.createDirectories(temp.resolve("real-donor-delivery"));
        var assets = new TsukuyomiImportedAssetSet(); assets.setOutputRootDir(output);
        copyCommon(request, baseline(data), output, assets);
        assertArrayEquals(expected, Files.readAllBytes(output.resolve("bhe_tama_bit041_0001.png")));
        assertArrayEquals(Files.readAllBytes(request.getExternalStaticAssetRoot().resolve("tama_bit041_0002.png")),
                Files.readAllBytes(output.resolve("bhe_tama_bit041_0002.png")));
        assertEquals(2, assets.getCopiedImageFiles().size());
        assertTrue(assets.getUnresolvedImageReferences().isEmpty());
        assertArrayEquals(expected, Files.readAllBytes(sourceImage));
    }

    private Set<String> collect(String branch, Spm data) throws Exception {
        var baseline = baseline(data); Set<String> images = new LinkedHashSet<>();
        if (branch.equals("common")) {
            Method method = OutputBheCommonProjectileResourcesStep.class.getDeclaredMethod("collectImageNamesFromSprite",
                    TsukuyomiBsdxBaselineBundle.class, CEventSprite.class, Set.class);
            method.setAccessible(true);
            method.invoke(new OutputBheCommonProjectileResourcesStep(), baseline, sprite(), images);
        } else {
            Class<?> type = Class.forName("com.giga.nexas.transfer.bhe2bsdx.meka." + branch + ".graft.ImportStaticAssetsStep");
            Method method = type.getDeclaredMethod("collectImageNamesFromSpriteAction", CEventSprite.class,
                    TsukuyomiBsdxBaselineBundle.class, TsukuyomiPackageBundle.class, Path.class, Set.class);
            method.setAccessible(true);
            method.invoke(type.getConstructor().newInstance(), sprite(), baseline, new TsukuyomiPackageBundle(),
                    temp.resolve("empty-" + branch), images);
        }
        return images;
    }

    private void copyCommon(TsukuyomiGraftRequest request, TsukuyomiBsdxBaselineBundle baseline,
                            Path output, TsukuyomiImportedAssetSet assets) throws Exception {
        var plan = new com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan();
        plan.getSourceWazIndexToTargetFileName().put(0, "fixture.waz");
        plan.getSourceWazIndexToTargetSkillBase().put(0, 0); plan.getSourceWazIndexToTargetSkillCount().put(0, 1);
        Method method = OutputBheCommonProjectileResourcesStep.class.getDeclaredMethod("copyCommonSpmImages",
                TsukuyomiGraftRequest.class, TsukuyomiBsdxBaselineBundle.class, plan.getClass(), Path.class, TsukuyomiImportedAssetSet.class);
        method.setAccessible(true); method.invoke(new OutputBheCommonProjectileResourcesStep(), request, baseline, plan, output, assets);
    }

    private static TsukuyomiBsdxBaselineBundle baseline(Spm data) {
        var baseline = new TsukuyomiBsdxBaselineBundle(); baseline.getSpmByFileName().put("Tama.spm", data);
        var group = new SpriteGroupGrp(); group.setFileName("SpriteGroup"); group.setExtensionName("grp");
        var entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(1); entry.setSpriteFileName("Tama.spm"); entry.setSpriteCodeName("TAMA"); entry.setParam(data.getAnimData().size());
        group.getSpriteList().add(entry); baseline.setSpriteGroupGrp(group);
        var waz = new Waz("fixture"); waz.setExtensionName("waz");
        var skill = new Waz.Skill(); skill.setSkillNameJapanese("fixture"); skill.setSkillNameEnglish("FIXTURE"); skill.setPhaseQuantity(1);
        var unit = new SkillUnit(0, "sprite"); unit.getSkillInfoObjectList().add(sprite());
        var phase = new Waz.Skill.SkillPhase(); phase.getSkillUnitCollection().add(unit); skill.getPhasesInfo().add(phase);
        var suffix = new Waz.Skill.SkillSuffix(); suffix.setInt1(0); suffix.setInt2(0); skill.getSkillSuffixList().add(suffix);
        waz.getSkillList().add(skill); baseline.getWazByFileName().put("fixture.waz", waz);
        return baseline;
    }

    private static CEventSprite sprite() {
        var sprite = new CEventSprite(13); sprite.setStartFrame(0); sprite.setEndFrame(0);
        sprite.setSpmFileSequence(0); sprite.setActionGroupNumber(0); sprite.setActionNumber(0); return sprite;
    }

    private static Spm heatFixture() {
        Spm data = spm(5, 1, 0, 14); List<Spm.SPMImageData> images = new ArrayList<>();
        for (String name : List.of("bhe_base.png", "bhe_direction.png", "bhe_unused.png")) {
            var image = new Spm.SPMImageData(); image.setImageName(name); images.add(image);
        }
        data.setImageData(images); data.setNumImageData(3);
        for (int i = 0; i < 14; i++) data.getPageData().get(i).getChipData().get(0).setImageNo(i == 0 ? 0 : i < 13 ? 1 : 2);
        return data;
    }

    private static Spm spm(int rotate, int mirror, int base, int pages) {
        var data = new Spm(); data.setExtensionName("spm"); data.setSpmVersion("SPM VER-2.00");
        List<Spm.SPMPageData> pageData = new ArrayList<>(); List<Spm.SPMImageData> images = new ArrayList<>();
        for (int i = 0; i < pages; i++) {
            var image = new Spm.SPMImageData(); image.setImageName("image-" + i + ".png"); images.add(image);
            var page = new Spm.SPMPageData(); page.setNumChipData(1); page.setPageWidth(1); page.setPageHeight(1);
            page.setPageRect(rect()); page.setPageOption(0L); page.setRotateCenterX(0); page.setRotateCenterY(0); page.setHitFlag(0L);
            page.setHitRects(new ArrayList<>());
            var chip = new Spm.SPMChipData(); chip.setImageNo(i); chip.setDstRect(rect()); chip.setSrcRect(rect());
            chip.setChipWidth(1); chip.setChipHeight(1); chip.setDrawOption(0L); chip.setDrawOptionValue(256L); chip.setOption(0);
            page.setChipData(List.of(chip)); pageData.add(page);
        }
        data.setPageData(pageData); data.setNumPageData(pages); data.setImageData(images); data.setNumImageData(images.size()); data.setPatPageNum(1);
        var anim = new Spm.SPMAnimData(); anim.setAnimName("directions"); anim.setNumPat(1);
        anim.setAnimRotateDirection(rotate); anim.setAnimReverseDirection(mirror);
        var pat = new Spm.SPMPatData(); pat.setWaitFrame(0); pat.setPageNo(List.of(base)); anim.setPatData(List.of(pat));
        data.setAnimData(List.of(anim)); data.setNumAnimData(1); return data;
    }

    private static Spm.SPMRect rect() {
        var rect = new Spm.SPMRect(); rect.setLeft(0); rect.setTop(0); rect.setRight(1); rect.setBottom(1); return rect;
    }
}
