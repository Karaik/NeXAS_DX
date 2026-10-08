package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.*;
import com.giga.nexas.util.PacUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import static org.junit.jupiter.api.Assertions.*;

class LayeredAssetDeliveryTest {
    @TempDir Path temp;
    private final BsdxBinService service = new BsdxBinService();

    @Test void outOfRangeMekVoiceNumberPreservesNativeSilentReference() throws Exception {
        var fixture = fixtureWithRealMekAndMaterial(); var baseline = fixture.assets().getValidationContext().baseline();
        var group = new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceGroup();
        group.setExistFlag(1); group.setCharacterName("fixture"); group.setCharacterCodeName("PRIVATE");
        baseline.getBatVoiceGrp().getVoiceList().add(group);
        baseline.getProgramMaterialGrp().getArray3().add(new com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp.IntArray());
        var info = fixture.mek().getMekVoiceInfo(); info.setVersion(0); info.setEmotions(List.of());
        var slot = new com.giga.nexas.dto.bsdx.mek.Mek.MekVoiceInfo.VoiceSlot();
        slot.setName("test"); slot.setToken("TEST"); info.setVoiceSlots(List.of(slot));
        var reference = new com.giga.nexas.dto.bsdx.mek.Mek.MekVoiceInfo.Entry();
        reference.setVoiceType(-1); reference.setGroupId(101); reference.setWeight(50);
        info.setTable(List.of(List.of(List.of(reference)))); info.setBuiltinEmotionCount(1);
        Path output = fixture.assets().getOutputRootDir(); generate(output.resolve("fixture.mek"), fixture.mek());
        generate(output.resolve("BatVoice.grp"), baseline.getBatVoiceGrp()); generate(output.resolve("ProgramMaterial.grp"), baseline.getProgramMaterialGrp());
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        var parsed = (com.giga.nexas.dto.bsdx.mek.Mek) service.parse(output.resolve("fixture.mek").toString(), "windows-31j").getData();
        assertEquals(101, parsed.getMekVoiceInfo().getTable().get(0).get(0).get(0).getGroupId());
        assertTrue(fixture.assets().getCopiedAudioFiles().isEmpty());
        assertEquals("BatVoice[0][101]", fixture.assets().getUnresolvedMekVoiceReferences().get("fixture.mek -> VoiceInfo.table[0][0][0]"));
    }

    @Test void disabledVoicePlaceholderDoesNotRequireAnAudioFile() throws Exception {
        var fixture = fixtureWithRealMekAndMaterial(); var baseline = fixture.assets().getValidationContext().baseline();
        var group = new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceGroup();
        group.setExistFlag(1); group.setCharacterName("fixture"); group.setCharacterCodeName("PRIVATE");
        var voice = new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoice(); voice.setExistFlag(0);
        group.getVoices().add(voice); baseline.getBatVoiceGrp().getVoiceList().add(group);
        baseline.getProgramMaterialGrp().getArray3().add(new com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp.IntArray());
        for (var e : fixture.mek().getMekMaterialBlock().getEntries()) e.getVoiceGroups().add(new int[0]);
        var info = fixture.mek().getMekVoiceInfo(); info.setVersion(0); info.setEmotions(List.of());
        var slot = new com.giga.nexas.dto.bsdx.mek.Mek.MekVoiceInfo.VoiceSlot(); slot.setName("test"); slot.setToken("TEST"); info.setVoiceSlots(List.of(slot));
        var reference = new com.giga.nexas.dto.bsdx.mek.Mek.MekVoiceInfo.Entry();
        reference.setVoiceType(-1); reference.setGroupId(0); reference.setWeight(100);
        info.setTable(List.of(List.of(List.of(reference)))); info.setBuiltinEmotionCount(1);
        Path output = fixture.assets().getOutputRootDir(); generate(output.resolve("fixture.mek"), fixture.mek());
        generate(output.resolve("BatVoice.grp"), baseline.getBatVoiceGrp()); generate(output.resolve("ProgramMaterial.grp"), baseline.getProgramMaterialGrp());
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
    }

    @Test void emptyReservedMaterialGroupsAreNotOutOfRangeReferences() throws Exception {
        var fixture = fixtureWithRealMekAndMaterial(); Path output = fixture.assets().getOutputRootDir();
        for (var entry : fixture.mek().getMekMaterialBlock().getEntries()) {
            entry.getSpriteGroups().add(new int[0]); entry.getSeGroups().add(new int[0]); entry.getVoiceGroups().add(new int[0]);
        }
        generate(output.resolve("fixture.mek"), fixture.mek());
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        fixture.mek().getMekMaterialBlock().getEntries().get(0).getSpriteGroups().set(1, new int[]{0});
        generate(output.resolve("fixture.mek"), fixture.mek());
        assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
    }

    @Test void unrelatedNestedResourcesDoNotBlockFlatMediaLookup() throws Exception {
        Path vanilla = Files.createDirectories(temp.resolve("vanilla-media"));
        Path nested = Files.createDirectories(vanilla.resolve("mod"));
        Files.write(vanilla.resolve("Hell.bin"), new byte[]{1}); Files.write(nested.resolve("Hell.bin"), new byte[]{2});
        Files.write(vanilla.resolve("actual.png"), new byte[]{3}); Files.write(nested.resolve("actual.png"), new byte[]{4});
        var req = request(false); req.setBsdxStaticAssetRoot(vanilla);
        var assets = assets(); var resolver = new LayeredAssetResolver(req, assets.getOutputRootDir(), assets);
        assertEquals(vanilla.resolve("actual.png"), resolver.resolveMedia("actual.png", false).path());
        assertDoesNotThrow(() -> resolver.auditImageReference("absent.png", "unused image"));
        assertTrue(assets.getUnresolvedImageReferences().containsKey("absent.png"));
    }

    @Test void missingSpmImageDoesNotBlockEitherImporter() throws Exception {
        for (String branch : List.of("tsukuyomi", "followup")) {
            Path output = Files.createDirectories(temp.resolve(branch));
            var request = request(false); request.setExternalStaticAssetRoot(temp.resolve("missing-bhe"));
            request.setBsdxStaticAssetRoot(temp.resolve("missing-original"));
            var selected = new TsukuyomiPackageBundle();
            selected.getSpmByFileName().put("private.spm", spm("missing-image.png"));
            var plan = new TsukuyomiImportPlan(); plan.getRequiredSpmFiles().add("private.spm");
            var assets = new TsukuyomiImportedAssetSet(); assets.setOutputRootDir(output);
            Class<?> type = Class.forName("com.giga.nexas.transfer.bhe2bsdx.meka." + branch + ".graft.ImportStaticAssetsStep");
            Method method = type.getDeclaredMethod("copyRequiredImageFiles", TsukuyomiGraftRequest.class,
                    TsukuyomiPackageBundle.class, TsukuyomiBsdxBaselineBundle.class, TsukuyomiImportPlan.class,
                    TsukuyomiGrpAppendPlan.class, Path.class, TsukuyomiImportedAssetSet.class);
            method.setAccessible(true);
            assertDoesNotThrow(() -> method.invoke(type.getConstructor().newInstance(), request, selected,
                    new TsukuyomiBsdxBaselineBundle(), plan, new TsukuyomiGrpAppendPlan(), output, assets));
            assertTrue(assets.getMissingAssets().isEmpty());
            assertTrue(assets.getCopiedImageFiles().isEmpty());
            assertTrue(assets.getUnresolvedImageReferences().containsKey("missing-image.png"));
            assertFalse(Files.exists(output.resolve("missing-image.png")));
            assertEquals("missing-image.png", selected.getSpmByFileName().get("private.spm").getImageData().get(0).getImageName());
        }
    }

    @Test void absentImageInReferencedAnimationDoesNotBlockRealPacking() throws Exception {
        var fixture = fixtureWithRealMekAndMaterial();
        var req = fixture.request(); req.setBsdxStaticAssetRoot(temp.resolve("no-original-media"));
        req.setExternalStaticAssetRoot(temp.resolve("no-donor"));
        Path output = fixture.assets().getOutputRootDir();
        Waz waz = (Waz) service.parse(output.resolve("private.waz").toString(), "windows-31j").getData();
        var sprite = new CEventSprite(13); sprite.setSpmFileSequence(0); sprite.setActionGroupNumber(0); sprite.setActionNumber(0);
        sprite.setStartFrame(0); sprite.setEndFrame(0);
        var unit = new SkillUnit(0, "sprite"); unit.getSkillInfoObjectList().add(sprite);
        var phase = new Waz.Skill.SkillPhase(); phase.getSkillUnitCollection().add(unit);
        waz.getSkillList().get(0).setPhaseQuantity(1); waz.getSkillList().get(0).getPhasesInfo().add(phase);
        generate(output.resolve("private.waz"), waz);
        Files.deleteIfExists(output.resolve("image.png"));
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(req, fixture.assets()));
        assertTrue(fixture.assets().getUnresolvedImageReferences().containsKey("image.png"));
        req.setPackUpdatePac(true);
        var packed = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.pack.PackUpdatePacStep().packUpdatePac(req, fixture.assets());
        assertTrue(packed.isPacked());
        assertEquals(manifest(output), unpackedManifest(packed.getOutputPacPath()));
        assertFalse(Files.exists(output.resolve("image.png")));
    }

    @Test void unusedMissingImageDataEntryDoesNotBecomeAMandatoryAsset() throws Exception {
        var fixture = fixtureWithRealMekAndMaterial(); Path output = fixture.assets().getOutputRootDir();
        Spm data = (Spm) service.parse(output.resolve("private.spm").toString(), "windows-31j").getData();
        var absent = new Spm.SPMImageData(); absent.setImageName("unused-and-absent.png");
        data.setImageData(new java.util.ArrayList<>(data.getImageData())); data.getImageData().add(absent);
        data.setNumImageData(data.getImageData().size()); generate(output.resolve("private.spm"), data);
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertFalse(Files.exists(output.resolve("unused-and-absent.png")));
    }

    @Test void commonProjectileOutputPreservesMissingImageReferenceWithoutAborting() throws Exception {
        var assets = assets(); var req = request(false);
        req.setBsdxStaticAssetRoot(temp.resolve("no-vanilla")); req.setExternalStaticAssetRoot(temp.resolve("no-donor"));
        var baseline = new TsukuyomiBsdxBaselineBundle();
        var grp = new SpriteGroupGrp(); var entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(1); entry.setSpriteFileName("Smoke.spm"); entry.setSpriteCodeName("SMOKE"); entry.setParam(1);
        grp.getSpriteList().add(entry); baseline.setSpriteGroupGrp(grp);
        baseline.getSpmByFileName().put("Smoke.spm", spm("bhe_smoke_007_0001.png"));
        var waz = new Waz("Effect"); var skill = new Waz.Skill();
        skill.setSkillNameJapanese("test"); skill.setSkillNameEnglish("TEST"); skill.setPhaseQuantity(1);
        var sprite = new CEventSprite(13); sprite.setSpmFileSequence(0); sprite.setActionGroupNumber(0);
        sprite.setActionNumber(0); sprite.setStartFrame(0); sprite.setEndFrame(0);
        var unit = new SkillUnit(0, "sprite"); unit.getSkillInfoObjectList().add(sprite);
        var phase = new Waz.Skill.SkillPhase(); phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase); waz.getSkillList().add(skill); baseline.getWazByFileName().put("Effect.waz", waz);
        var plan = new com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan();
        plan.getCommonProjectileWazFiles().add("Effect.waz"); plan.getCommonProjectileSpmFiles().add("Smoke.spm");
        plan.getSourceWazIndexToTargetFileName().put(0, "Effect.waz");
        plan.getSourceWazIndexToTargetSkillBase().put(0, 0); plan.getSourceWazIndexToTargetSkillCount().put(0, 1);
        new com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output.OutputBheCommonProjectileResourcesStep()
                .output(req, baseline, plan, Map.of(), assets.getOutputRootDir(), assets);
        assertTrue(assets.getUnresolvedImageReferences().containsKey("bhe_smoke_007_0001.png"));
        assertTrue(assets.getMissingAssets().isEmpty()); assertTrue(assets.getCopiedImageFiles().isEmpty());
        assertFalse(Files.exists(assets.getOutputRootDir().resolve("bhe_smoke_007_0001.png")));
        var parsed = (Spm) service.parse(assets.getOutputRootDir().resolve("Smoke.spm").toString(), "windows-31j").getData();
        assertEquals("bhe_smoke_007_0001.png", parsed.getImageData().get(0).getImageName());
    }

    @Test void inheritedImageDoesNotRequireBheDonorDirectoryInEitherImporter() throws Exception {
        for (String branch : List.of("tsukuyomi", "followup")) {
            Path output = Files.createDirectories(temp.resolve(branch));
            Files.write(output.resolve("Vanilla.png"), new byte[] {1, 2, 3});
            TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
            request.setExternalStaticAssetRoot(temp.resolve("absent-donor"));
            TsukuyomiPackageBundle selected = new TsukuyomiPackageBundle();
            selected.getSpmByFileName().put("private.spm", spm("vanilla.png"));
            TsukuyomiImportPlan plan = new TsukuyomiImportPlan();
            plan.getRequiredSpmFiles().add("private.spm");
            TsukuyomiImportedAssetSet assets = new TsukuyomiImportedAssetSet();
            Class<?> type = Class.forName("com.giga.nexas.transfer.bhe2bsdx.meka." + branch + ".graft.ImportStaticAssetsStep");
            Method method = type.getDeclaredMethod("copyRequiredImageFiles", TsukuyomiGraftRequest.class,
                    TsukuyomiPackageBundle.class, TsukuyomiBsdxBaselineBundle.class, TsukuyomiImportPlan.class,
                    TsukuyomiGrpAppendPlan.class, Path.class, TsukuyomiImportedAssetSet.class);
            method.setAccessible(true);
            method.invoke(type.getConstructor().newInstance(), request, selected, new TsukuyomiBsdxBaselineBundle(),
                    plan, new TsukuyomiGrpAppendPlan(), output, assets);
            assertTrue(assets.getMissingAssets().isEmpty(), branch + ": inherited file is already effective");
            assertTrue(assets.getCopiedImageFiles().isEmpty(), "already effective image must not be copied again");
        }
    }

    @Test void missingDependencyRejectsDisabledPackingInBothBranches() throws Exception {
        TsukuyomiGraftRequest request = request(false);
        TsukuyomiImportedAssetSet assets = assets();
        assets.getMissingAssets().add("缺少辅助 waz: private.waz");
        assertThrows(IllegalStateException.class, () -> new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.pack.PackUpdatePacStep().packUpdatePac(request, assets));
        assertThrows(IllegalStateException.class, () -> new com.giga.nexas.transfer.bhe2bsdx.meka.followup.pack.PackUpdatePacStep().packUpdatePac(request, assets));
        assertFalse(Files.exists(temp.resolve("Update3.pac")));
    }

    @Test void corruptGeneratedSpmCannotPassDisabledPack() throws Exception {
        TsukuyomiImportedAssetSet assets = assets();
        Path file = assets.getOutputRootDir().resolve("broken.spm");
        Files.write(file, new byte[] {1, 2, 3});
        assets.getCopiedSpmFiles().add(file);
        assertThrows(IllegalStateException.class, () -> new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.pack.PackUpdatePacStep().packUpdatePac(request(false), assets));
    }

    @Test void finalSpriteActionIndexIsCheckedFromGeneratedBytes() throws Exception {
        TsukuyomiImportedAssetSet assets = assets();
        Path output = assets.getOutputRootDir();
        SpriteGroupGrp grp = new SpriteGroupGrp();
        SpriteGroupGrp.SpriteGroupEntry entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(1); entry.setSpriteFileName("private.spm"); entry.setSpriteCodeName("PRIVATE"); entry.setParam(0);
        grp.getSpriteList().add(entry);
        generate(output.resolve("SpriteGroup.grp"), grp);
        generate(output.resolve("private.spm"), spm("image.png"));
        Files.write(output.resolve("image.png"), new byte[] {7});
        Waz waz = new Waz("private"); waz.setExtensionName("waz");
        Waz.Skill skill = new Waz.Skill(); skill.setSkillNameJapanese("test"); skill.setSkillNameEnglish("TEST"); skill.setPhaseQuantity(1);
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase(); SkillUnit unit = new SkillUnit(0, "sprite");
        CEventSprite sprite = new CEventSprite(13); sprite.setStartFrame(0); sprite.setEndFrame(1);
        sprite.setSpmFileSequence(0); sprite.setActionGroupNumber(9); sprite.setActionNumber(0);
        unit.getSkillInfoObjectList().add(sprite); phase.getSkillUnitCollection().add(unit); skill.getPhasesInfo().add(phase);
        waz.getSkillList().add(skill); generate(output.resolve("private.waz"), waz);
        assets.getGeneratedWazFiles().add(output.resolve("private.waz"));
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> new com.giga.nexas.transfer.bhe2bsdx.meka.followup.pack.PackUpdatePacStep().packUpdatePac(request(false), assets));
        assertTrue(failure.getMessage().contains("private.waz"));
        assertTrue(failure.getMessage().contains("9"));
    }

    @Test void realPacRoundTripMatchesRelativePathsAndBytes() throws Exception {
        TsukuyomiImportedAssetSet assets = assets();
        Files.write(assets.getOutputRootDir().resolve("borrowed.png"), new byte[] {1, 7, 3, 5});
        Files.write(assets.getOutputRootDir().resolve("new.ogg"), new byte[] {9, 2, 4});
        TsukuyomiPacPackPlan plan = new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.pack.PackUpdatePacStep().packUpdatePac(request(true), assets);
        assertTrue(plan.isPacked()); assertTrue(Files.size(plan.getOutputPacPath()) > 0);
        Path copy = temp.resolve("verify.pac"); Files.copy(plan.getOutputPacPath(), copy);
        PacUtil.unpack(copy.toString());
        assertEquals(manifest(assets.getOutputRootDir()), manifest(temp.resolve("verify")));
    }

    @Test void vanillaImageIsInheritedWhileNewBheImageIsMaterializedAndPacked() throws Exception {
        Path vanilla = Files.createDirectories(temp.resolve("vanilla"));
        Path donor = Files.createDirectories(temp.resolve("bhe"));
        Files.write(vanilla.resolve("Original.PNG"), new byte[] {11});
        Files.write(vanilla.resolve("shared.png"), new byte[] {13});
        Files.write(donor.resolve("shared.png"), new byte[] {17});
        Files.write(vanilla.resolve("SpriteGroup.grp"), new byte[] {99}); // 媒体根内不同版本的表绝不参与索引。
        TsukuyomiGraftRequest request = request(true); request.setBsdxStaticAssetRoot(vanilla); request.setExternalStaticAssetRoot(donor);
        TsukuyomiImportedAssetSet assets = assets();
        LayeredAssetResolver resolver = new LayeredAssetResolver(request, assets.getOutputRootDir(), assets);
        resolver.materialize("original.png", "original.png", false, false, "private.waz[0]");
        resolver.materialize("shared.png", "shared.png", false, true, "private.spm.image[1]");
        assertFalse(Files.exists(assets.getOutputRootDir().resolve("original.png")));
        assertArrayEquals(new byte[] {17}, Files.readAllBytes(assets.getOutputRootDir().resolve("shared.png")));
        assertTrue(assets.getResolvedMediaSources().get("original.png").startsWith("vanilla:"));
        var plan = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.pack.PackUpdatePacStep().packUpdatePac(request, assets);
        assertTrue(plan.isPacked());
        Path copy = temp.resolve("increment.pac"); Files.copy(plan.getOutputPacPath(), copy); PacUtil.unpack(copy.toString());
        assertEquals(Map.of("shared.png", "11"), manifest(temp.resolve("increment")));
    }

    @Test void newBheMediaCannotSilentlyReuseDifferentVanillaFile() throws Exception {
        Path vanilla = Files.createDirectories(temp.resolve("vanilla")); Files.write(vanilla.resolve("new.png"), new byte[] {8});
        TsukuyomiGraftRequest request = request(false); request.setBsdxStaticAssetRoot(vanilla); request.setExternalStaticAssetRoot(temp.resolve("absent"));
        TsukuyomiImportedAssetSet assets = assets();
        assertDoesNotThrow(() -> new LayeredAssetResolver(request, assets.getOutputRootDir(), assets)
                .materialize("new.png", "new.png", false, true, "private.waz[4].sprite"));
        assertFalse(Files.exists(assets.getOutputRootDir().resolve("new.png")));
        assertFalse(assets.getResolvedMediaSources().containsKey("new.png"));
        assertTrue(assets.getUnresolvedImageReferences().containsKey("new.png"));
    }

    @Test void missingImageIsAuditedWhileDisabledSpriteGroupIsRejected() throws Exception {
        TsukuyomiImportedAssetSet assets = assets(); Path output = assets.getOutputRootDir();
        SpriteGroupGrp grp = new SpriteGroupGrp(); var entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(1); entry.setSpriteFileName("private.spm"); entry.setSpriteCodeName("PRIVATE"); entry.setParam(0); grp.getSpriteList().add(entry);
        generate(output.resolve("SpriteGroup.grp"), grp); generate(output.resolve("private.spm"), spm("missing.png"));
        Waz waz = new Waz("private"); var skill = new Waz.Skill(); skill.setSkillNameJapanese("test"); skill.setSkillNameEnglish("TEST"); skill.setPhaseQuantity(1);
        var phase = new Waz.Skill.SkillPhase(); var unit = new SkillUnit(0, "sprite"); var sprite = new CEventSprite(13);
        sprite.setStartFrame(0); sprite.setEndFrame(1); sprite.setSpmFileSequence(0); sprite.setActionGroupNumber(0); sprite.setActionNumber(0);
        unit.getSkillInfoObjectList().add(sprite); phase.getSkillUnitCollection().add(unit); skill.getPhasesInfo().add(phase); waz.getSkillList().add(skill);
        generate(output.resolve("private.waz"), waz); assets.getGeneratedWazFiles().add(output.resolve("private.waz"));
        TsukuyomiGraftRequest request = request(false); request.setBsdxStaticAssetRoot(temp.resolve("absent-vanilla"));
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(request, assets));
        Files.write(output.resolve("missing.png"), new byte[] {1}); entry.setExistFlag(0); generate(output.resolve("SpriteGroup.grp"), grp);
        var disabled = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(request, assets));
        assertTrue(disabled.getMessage().contains("SpriteGroup[0]"));
        assertTrue(disabled.getMessage().contains("禁用"));
    }

    private record DeliveryFixture(TsukuyomiGraftRequest request, TsukuyomiImportedAssetSet assets,
                                   com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp group,
                                   com.giga.nexas.dto.bsdx.mek.Mek mek) { }

    private DeliveryFixture fixtureWithRealMekAndMaterial() throws Exception {
        TsukuyomiImportedAssetSet assets = assets(); Path output = assets.getOutputRootDir();
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        SpriteGroupGrp spriteGrp = new SpriteGroupGrp(); var spriteEntry = new SpriteGroupGrp.SpriteGroupEntry();
        spriteEntry.setExistFlag(1); spriteEntry.setSpriteFileName("private.spm"); spriteEntry.setSpriteCodeName("PRIVATE"); spriteEntry.setParam(1);
        spriteGrp.getSpriteList().add(spriteEntry); baseline.setSpriteGroupGrp(spriteGrp);
        var wazaGrp = new com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp();
        var wazaEntry = new com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp.WazaGroupEntry();
        wazaEntry.setExistFlag(1); wazaEntry.setWazaName("test"); wazaEntry.setWazaCodeName("PRIVATE"); wazaEntry.setWazaDisplayName("private"); wazaEntry.setParam(1);
        wazaGrp.getWazaList().add(wazaEntry); baseline.setWazaGroupGrp(wazaGrp);
        var mekaGrp = new com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp();
        var mekaEntry = new com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp.MekaGroup();
        mekaEntry.setExistFlag(1); mekaEntry.setMekaName("fixture"); mekaEntry.setMekaCodeName("PRIVATE"); mekaGrp.getMekaList().add(mekaEntry); baseline.setMekaGroupGrp(mekaGrp);
        baseline.setSeGroupGrp(new com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp());
        baseline.setBatVoiceGrp(new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp());
        var program = new com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp();
        program.getArray1().add(new com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp.IntArray()); baseline.setProgramMaterialGrp(program);
        var mek = (com.giga.nexas.dto.bsdx.mek.Mek) service.parse("src/main/resources/game/bsdx/mek/Zako102a.mek", "windows-31j").getData();
        mek.getMekBasicInfo().setWazFileSequence(0); mek.getMekBasicInfo().setSpmFileSequence(0);
        var weapon = mek.getMekWeaponInfoMap().values().iterator().next(); weapon.setWazSequence(0); weapon.setWeaponCategory(0);
        mek.setMekWeaponInfoMap(new java.util.LinkedHashMap<>(Map.of(0, weapon))); mek.getMekAiInfoList().clear();
        mek.getMekVoiceInfo().setEmotions(List.of()); mek.getMekVoiceInfo().setVoiceSlots(List.of()); mek.getMekVoiceInfo().setTable(List.of());
        var material = new com.giga.nexas.dto.bsdx.mek.Mek.MekMaterialBlock(); material.setExtraRegularCount(0);
        for (int i = 0; i < 9; i++) {
            var entry = new com.giga.nexas.dto.bsdx.mek.Mek.MekMaterialBlock.PluginEntry();
            entry.getSpriteGroups().add(i == 8 ? new int[] {99} : new int[0]); material.getEntries().add(entry);
        }
        mek.setMekMaterialBlock(material);
        Waz waz = new Waz("private"); var skill = new Waz.Skill(); skill.setSkillNameJapanese("test"); skill.setSkillNameEnglish("TEST"); skill.setPhaseQuantity(0); waz.getSkillList().add(skill);
        generate(output.resolve("fixture.mek"), mek); generate(output.resolve("private.waz"), waz); generate(output.resolve("private.spm"), spm("image.png"));
        generate(output.resolve("MekaGroup.grp"), mekaGrp); generate(output.resolve("WazaGroup.grp"), wazaGrp); generate(output.resolve("SpriteGroup.grp"), spriteGrp);
        generate(output.resolve("SeGroup.grp"), baseline.getSeGroupGrp()); generate(output.resolve("BatVoice.grp"), baseline.getBatVoiceGrp()); generate(output.resolve("ProgramMaterial.grp"), program);
        TsukuyomiGraftRequest request = request(false); request.setWazFileName("private.waz"); request.setBsdxStaticAssetRoot(temp.resolve("absent"));
        request.setBsdxDatDir(temp.resolve("absent-original-dat"));
        assets.setValidationContext(new AssetDeliveryValidationContext(request, baseline, "fixture.mek"));
        return new DeliveryFixture(request, assets, mekaGrp, mek);
    }

    @Test void kouConsumesThreeMaterialRowsForCategoryZeroWhileUnusedTailIsNotConsumed() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial();
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()), "non-KOU consumes exactly one existing weapon row");
        fixture.group().getMekaList().get(0).setMekaCodeName("KOU"); generate(fixture.assets().getOutputRootDir().resolve("MekaGroup.grp"), fixture.group());
        IllegalStateException failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("Material"));
    }

    @Test void firstPlayerEquipmentSlotIsCheckedWithoutRestrictingUnusedWeaponPoolRows() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial();
        var equip = new com.giga.nexas.dto.bsdx.dat.Dat(); equip.setFileName("WeaponEquip");
        equip.setColumnCount(14); equip.setColumnTypes(java.util.Collections.nCopies(14, "Integer"));
        var row = new java.util.ArrayList<Object>(java.util.Collections.nCopies(14, -1)); row.set(0, 153); equip.addRow(row);
        Path file = fixture.assets().getOutputRootDir().resolve("WeaponEquip.dat"); generate(file, equip);
        var failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("WeaponEquip[0][0]=153"));
        row.set(0, 2); generate(file, equip);
        failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("weapon[2]"));
        row.set(0, 0); generate(file, equip);
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
    }

    @Test void actualAiAttackConsumerCannotPointToMissingWeapon() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial();
        var source = (com.giga.nexas.dto.bsdx.mek.Mek) service.parse("src/main/resources/game/bsdx/mek/Zako102a.mek", "windows-31j").getData();
        var ai = source.getMekAiInfoList().stream().filter(info -> info.getCpuEventList().stream()
                .anyMatch(event -> event instanceof com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack)).findFirst().orElseThrow();
        var attack = (com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack) ai.getCpuEventList().stream()
                .filter(event -> event instanceof com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack).findFirst().orElseThrow();
        attack.setMekWeaponInfoMapNo(2); ai.setCpuEventList(List.of(attack)); fixture.mek().setMekAiInfoList(List.of(ai));
        Path output = fixture.assets().getOutputRootDir().resolve("fixture.mek"); generate(output, fixture.mek());
        var failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("AI attack weapon[2]"));
        attack.setMekWeaponInfoMapNo(-1); generate(output, fixture.mek());
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
    }

    @Test void declaredGeneratedGrpCannotFallBackToMutableBaselineWhenDeleted() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial();
        Path group = fixture.assets().getOutputRootDir().resolve("SpriteGroup.grp");
        fixture.assets().getGeneratedGrpFiles().add(group); Files.delete(group);
        var failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("声明产物未落盘"));
        assertTrue(failure.getMessage().contains("SpriteGroup.grp"));
    }

    @Test void inheritedJinkiCharacterIsValidatedAndCarriedIntoFollowupScope() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial();
        var jinki = new com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult();
        var plan = new com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan(); plan.setMekaGroupIndex(0); plan.setWazaGroupIndex(0); jinki.setGrpAppendPlan(plan);
        var baseline = fixture.assets().getValidationContext().baseline();
        var inheritedEntry = new com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp.MekaGroup();
        inheritedEntry.setExistFlag(1); inheritedEntry.setMekaName("inherited"); inheritedEntry.setMekaCodeName("AKAO"); baseline.getMekaGroupGrp().getMekaList().set(0, inheritedEntry);
        fixture.request().setInheritedJinkiResult(jinki);
        var context = new AssetDeliveryValidationContext(fixture.request(), baseline, "fixture.mek");
        assertTrue(context.mekFiles().contains("inherited.mek"), "AKAO migrated root must not disappear from validation");
        assertTrue(context.fullWazFiles().contains("private.waz"));
        var previous = new TsukuyomiGraftResult(); fixture.assets().setValidationContext(context); previous.setImportedAssetSet(fixture.assets());
        var followup = new com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest();
        followup.setPreviousCharacterResult(previous); followup.setWazFileName("next.waz");
        var cumulative = new AssetDeliveryValidationContext(followup, baseline, "next.mek");
        assertEquals(java.util.Set.of("fixture.mek", "inherited.mek", "next.mek"), cumulative.mekFiles());
        assertEquals(java.util.Set.of("private.waz", "next.waz"), cumulative.fullWazFiles());
    }

    @Test void menuWeaponCandidateCannotExceedPlayerBankWhenEquippedSlotsAreSafe() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial();
        var equip = new com.giga.nexas.dto.bsdx.dat.Dat(); equip.setColumnCount(14); equip.setColumnTypes(java.util.Collections.nCopies(14, "Integer"));
        var row = new java.util.ArrayList<Object>(java.util.Collections.nCopies(14, -1)); row.set(0, 0); equip.addRow(row);
        generate(fixture.assets().getOutputRootDir().resolve("WeaponEquip.dat"), equip);
        var candidates = new com.giga.nexas.dto.bsdx.dat.Dat(); candidates.setColumnCount(1); candidates.setColumnTypes(List.of("Integer")); candidates.addRow(List.of(153));
        generate(fixture.assets().getOutputRootDir().resolve("Weapon.dat"), candidates);
        var failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("Weapon.dat[0]"));
        assertTrue(failure.getMessage().contains("153"));
    }

    @Test void mekVoiceTableIsValidatedWithoutWazOrMaterialVoiceReferences() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial(); var baseline = fixture.assets().getValidationContext().baseline();
        var group = new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoiceGroup();
        group.setExistFlag(1); group.setCharacterName("fixture"); group.setCharacterCodeName("PRIVATE");
        var voice = new com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp.BatVoice();
        voice.setExistFlag(1); voice.setVoice("test"); voice.setVoiceCodeName("TEST"); voice.setVoiceFileName("required_voice"); group.getVoices().add(voice);
        baseline.getBatVoiceGrp().getVoiceList().add(group);
        baseline.getProgramMaterialGrp().getArray3().add(new com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp.IntArray());
        for (var material : fixture.mek().getMekMaterialBlock().getEntries()) material.getVoiceGroups().add(new int[0]);
        var info = fixture.mek().getMekVoiceInfo(); info.setVersion(0); info.setEmotions(List.of());
        var slot = new com.giga.nexas.dto.bsdx.mek.Mek.MekVoiceInfo.VoiceSlot(); slot.setName("test"); slot.setToken("TEST"); info.setVoiceSlots(List.of(slot));
        var reference = new com.giga.nexas.dto.bsdx.mek.Mek.MekVoiceInfo.Entry(); reference.setVoiceType(-1); reference.setGroupId(2); reference.setWeight(100);
        info.setTable(List.of(List.of(List.of(reference)))); info.setBuiltinEmotionCount(1);
        Path output = fixture.assets().getOutputRootDir(); generate(output.resolve("fixture.mek"), fixture.mek());
        generate(output.resolve("BatVoice.grp"), baseline.getBatVoiceGrp()); generate(output.resolve("ProgramMaterial.grp"), baseline.getProgramMaterialGrp());
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertEquals("BatVoice[0][2]", fixture.assets().getUnresolvedMekVoiceReferences().get("fixture.mek -> VoiceInfo.table[0][0][0]"));
        reference.setGroupId(0); generate(output.resolve("fixture.mek"), fixture.mek());
        var failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("required_voice"));
        Files.write(output.resolve("required_voice.wav"), new byte[] {1, 2});
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        // 原生first int是显式group覆盖；即使默认组越界，覆盖组仍可独立有效。
        info.setVersion(999); reference.setVoiceType(0); generate(output.resolve("fixture.mek"), fixture.mek());
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        reference.setVoiceType(999); generate(output.resolve("fixture.mek"), fixture.mek());
        assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        // negative覆盖回退实际header，不按角色名猜group。
        reference.setVoiceType(-1); generate(output.resolve("fixture.mek"), fixture.mek());
        assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
    }

    @Test void negativeWazGroupUsesOwningMekAndNegativeSkillIsNotConsumed() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial(); Path file = fixture.assets().getOutputRootDir().resolve("private.waz");
        Waz waz = (Waz) service.parse(file.toString(), "windows-31j").getData();
        var select = new com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect(4);
        select.setStartFrame(0); select.setEndFrame(0); select.setWazFileNo(-1); select.setWazSequenceNo(0);
        var effect = new com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect(19);
        effect.setStartFrame(0); effect.setEndFrame(0);
        for (var field : effect.getClass().getDeclaredFields()) if (field.getName().matches("int[0-9]+")) { field.setAccessible(true); field.set(effect, 0); }
        var nested = new com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect.CEventEffectUnit();
        nested.setCeventEffectUnitQuantity(3); nested.setUnitSlotNum(3); nested.setBuffer(1); nested.setData(select); effect.getCeventEffectUnitList().add(nested);
        var unit = new SkillUnit(48, "effect"); unit.getSkillInfoObjectList().add(effect);
        var phase = new Waz.Skill.SkillPhase(); phase.getSkillUnitCollection().add(unit); waz.getSkillList().get(0).setPhaseQuantity(1); waz.getSkillList().get(0).getPhasesInfo().add(phase);
        generate(file, waz);
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()), "negative group selects the MEK's own WAZ");
        select.setWazFileNo(999); select.setWazSequenceNo(-1); generate(file, waz);
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()), "negative skill consumes no WAZ payload even with unavailable group");
    }

    @Test void changeListTwoWazModesValidateIndicesWithoutTreatingPhaseModesAsWaz() throws Exception {
        DeliveryFixture fixture = fixtureWithRealMekAndMaterial(); Path file = fixture.assets().getOutputRootDir().resolve("private.waz");
        Waz waz = (Waz) service.parse(file.toString(), "windows-31j").getData();
        var change = new com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange(34);
        change.setStartFrame(0); change.setEndFrame(0); change.setFlag(1); change.setInt1(0);
        var condition = new com.giga.nexas.dto.bsdx.BsdxInfoCollection(); condition.setInt1(0); condition.setInt2(0); change.getBsdxInfoCollectionList1().add(condition);
        var target = new com.giga.nexas.dto.bsdx.BsdxInfoCollection(); target.setInt1(16); target.setInt2(0); target.setTypeList(List.of(1)); target.setParamList(List.of(0, 9));
        change.getBsdxInfoCollectionList2().add(target);
        var unit = new SkillUnit(71, "change"); unit.getSkillInfoObjectList().add(change); var phase = new Waz.Skill.SkillPhase(); phase.getSkillUnitCollection().add(unit);
        waz.getSkillList().get(0).setPhaseQuantity(1); waz.getSkillList().get(0).getPhasesInfo().add(phase); generate(file, waz);
        var failure = assertThrows(IllegalStateException.class, () -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        assertTrue(failure.getMessage().contains("List2")); assertTrue(failure.getMessage().contains("9"));
        target.setTypeList(List.of(0)); target.setParamList(List.of(999, 999)); generate(file, waz);
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
        target.setTypeList(List.of(2)); target.setParamList(List.of(-1, 0)); generate(file, waz);
        assertDoesNotThrow(() -> AssetDeliveryValidator.validate(fixture.request(), fixture.assets()));
    }

    @Test void unchangedVanillaResourcesStayOutsideIncrementalOutputButChangesAndInheritedFilesRemain() throws Exception {
        for (String branch : List.of("tsukuyomi", "followup")) {
            Path vanilla = Files.createDirectories(temp.resolve(branch + "-original"));
            Path output = Files.createDirectories(temp.resolve(branch + "-increment"));
            Spm originalSpm = spm("original.png"); generate(vanilla.resolve("vanilla.spm"), originalSpm);
            Waz originalWaz = new Waz("vanilla"); var skill = new Waz.Skill();
            skill.setSkillNameJapanese("original"); skill.setSkillNameEnglish("ORIGINAL"); skill.setPhaseQuantity(0); originalWaz.getSkillList().add(skill);
            generate(vanilla.resolve("vanilla.waz"), originalWaz);
            byte[] originalBytes = Files.readAllBytes(vanilla.resolve("vanilla.spm"));
            TsukuyomiGraftRequest request = request(false); request.setBsdxSpmDir(vanilla); request.setBsdxWazDir(vanilla);
            TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
            baseline.getSpmByFileName().put("vanilla.spm", originalSpm); baseline.getWazByFileName().put("vanilla.waz", originalWaz);
            TsukuyomiImportedAssetSet assets = new TsukuyomiImportedAssetSet(); assets.setOutputRootDir(output);
            Class<?> type = Class.forName("com.giga.nexas.transfer.bhe2bsdx.meka." + branch + ".graft.ImportStaticAssetsStep");
            Object step = type.getConstructor().newInstance();
            for (String methodName : List.of("writeBaselineSpmFiles", "writeBaselineWazFiles")) {
                Method method = type.getDeclaredMethod(methodName, TsukuyomiGraftRequest.class, TsukuyomiBsdxBaselineBundle.class, Path.class, TsukuyomiImportedAssetSet.class);
                method.setAccessible(true); method.invoke(step, request, baseline, output, assets);
            }
            assertFalse(Files.exists(output.resolve("vanilla.spm")), branch + ": unchanged SPM stays inherited");
            assertFalse(Files.exists(output.resolve("vanilla.waz")), branch + ": unchanged WAZ stays inherited");
            originalSpm.getAnimData().get(0).setAnimRotateDirection(1);
            Method method = type.getDeclaredMethod("writeBaselineSpmFiles", TsukuyomiGraftRequest.class, TsukuyomiBsdxBaselineBundle.class, Path.class, TsukuyomiImportedAssetSet.class);
            method.setAccessible(true); method.invoke(step, request, baseline, output, assets);
            Spm changed = (Spm) service.parse(output.resolve("vanilla.spm").toString(), "windows-31j").getData();
            assertEquals(1, changed.getAnimData().get(0).getAnimRotateDirection());
            byte[] inherited = Files.readAllBytes(output.resolve("vanilla.spm"));
            originalSpm.getAnimData().get(0).setAnimRotateDirection(2); method.invoke(step, request, baseline, output, assets);
            assertArrayEquals(inherited, Files.readAllBytes(output.resolve("vanilla.spm")), "already materialized inherited layer must retain precedence");
            assertArrayEquals(originalBytes, Files.readAllBytes(vanilla.resolve("vanilla.spm")));
        }
    }

    private TsukuyomiGraftRequest request(boolean pack) {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest(); request.setPackUpdatePac(pack); return request;
    }
    private TsukuyomiImportedAssetSet assets() throws Exception {
        TsukuyomiImportedAssetSet assets = new TsukuyomiImportedAssetSet();
        assets.setOutputRootDir(Files.createDirectories(temp.resolve("assets"))); return assets;
    }
    private void generate(Path file, com.giga.nexas.dto.bsdx.Bsdx data) throws Exception {
        if (data instanceof com.giga.nexas.dto.bsdx.grp.Grp grp) grp.setFileName(file.getFileName().toString().substring(0, file.getFileName().toString().lastIndexOf('.')).toLowerCase(java.util.Locale.ROOT));
        data.setExtensionName(file.getFileName().toString().substring(file.getFileName().toString().lastIndexOf('.') + 1));
        service.generate(file.toString(), data, "windows-31j");
    }
    private Map<String, String> manifest(Path root) throws Exception {
        Map<String, String> result = new TreeMap<>();
        try (var files = Files.walk(root)) {
            for (Path file : files.filter(Files::isRegularFile).toList()) result.put(root.relativize(file).toString(), java.util.HexFormat.of().formatHex(Files.readAllBytes(file)));
        }
        return result;
    }

    private Map<String, String> unpackedManifest(Path pac) throws Exception {
        Path copy = temp.resolve("missing-image-verified.pac"); Files.copy(pac, copy);
        PacUtil.unpack(copy.toString());
        return manifest(temp.resolve("missing-image-verified"));
    }
    private Spm spm(String image) {
        Spm spm = new Spm(); spm.setExtensionName("spm"); spm.setSpmVersion("SPM VER-2.00");
        Spm.SPMRect rect = new Spm.SPMRect(); rect.setLeft(0); rect.setTop(0); rect.setRight(1); rect.setBottom(1);
        Spm.SPMChipData chip = new Spm.SPMChipData(); chip.setImageNo(0); chip.setDstRect(rect); chip.setSrcRect(rect);
        chip.setChipWidth(1); chip.setChipHeight(1); chip.setDrawOption(0L); chip.setDrawOptionValue(0L); chip.setOption(0);
        Spm.SPMPageData page = new Spm.SPMPageData(); page.setNumChipData(1); page.setPageWidth(1); page.setPageHeight(1);
        page.setPageRect(rect); page.setPageOption(0L); page.setRotateCenterX(0); page.setRotateCenterY(0); page.setHitFlag(0L);
        page.setHitRects(List.of()); page.setChipData(List.of(chip)); spm.setNumPageData(1); spm.setPageData(List.of(page));
        Spm.SPMImageData img = new Spm.SPMImageData(); img.setImageName(image);
        spm.setNumImageData(1); spm.setImageData(List.of(img)); spm.setPatPageNum(1);
        Spm.SPMAnimData anim = new Spm.SPMAnimData(); anim.setAnimName("stand"); anim.setNumPat(1);
        anim.setAnimRotateDirection(0); anim.setAnimReverseDirection(0);
        Spm.SPMPatData pat = new Spm.SPMPatData(); pat.setWaitFrame(1); pat.setPageNo(List.of(0)); anim.setPatData(List.of(pat));
        spm.setNumAnimData(1); spm.setAnimData(List.of(anim)); return spm;
    }
}
