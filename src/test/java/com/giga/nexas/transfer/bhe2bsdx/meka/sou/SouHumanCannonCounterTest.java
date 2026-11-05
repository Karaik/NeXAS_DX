package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.grp.groupmap.*;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectilePrepareStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.CommonProjectileTestSupport;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output.OutputBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.resolve.BheResourceIndexResolver;
import com.giga.nexas.transfer.bhe2bsdx.model.followup.FollowupGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Real Sou/main/common binaries exercise the public role boundary; no game/runtime claim. */
@Execution(ExecutionMode.SAME_THREAD)
class SouHumanCannonCounterTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String CHARSET = "windows-31j";
    private static final Path BHE = Path.of("src/main/resources/game/bhe");
    private static final Path DX = Path.of("src/main/resources/game/bsdx");
    private static final List<String> ENGLISH = List.of("N_HOUDAN01", "N_HOUDAN02", "N_HOUDAN03");
    private static final List<String> JAPANESE = List.of("人間砲弾Lv1", "人間砲弾Lv2", "人間砲弾Lv3");
    private static final List<Integer> FLIGHT_ENTRIES = List.of(6, 5, 4, 3, 2);
    private static final List<List<Integer>> WAIT_WINDOWS = List.of(List.of(42, 134), List.of(34, 126), List.of(22, 114));

    @TempDir Path output;
    private int sequence;

    @Test
    void allThreeLevelsWaitForReleaseAndKeepAllFiveOriginalFlightChoices() throws Exception {
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill controller = privateController(f);
        assertProtocol(f, controller);
        assertSourceUnchanged(f);
    }

    @Test
    void privateControllerStaysInTheExistingPublicHostAndPreservesEveryUnpairedCaller() throws Exception {
        Fixture f = fixture();
        Snapshot before = snapshot(f);
        byte[] publicBytes = writeWaz(f.host(), "before-public");
        byte[] nativePrefix = f.sourceFiles().get(resolve(DX.resolve("waz"), "Tama05.waz"));
        assertArrayEquals(nativePrefix, Arrays.copyOf(publicBytes, nativePrefix.length),
                "the real native Tama05 bytes already form the prefix of the prepared host fixture");
        JsonNode originalController = payload(publicController(f));
        JsonNode selectedMain = payload(f.context().getSelectedPackage().getWazByFileName());

        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());

        Waz.Skill controller = privateController(f);
        assertEquals(before.mainCount(), f.main().getSkillList().size(), "a Tama controller must not be appended into Sou's main WAZ");
        assertEquals(before.hostCount() + 1, f.host().getSkillList().size());
        assertTrue(controller.getSkillNameEnglish().startsWith("DX_"), "private controller has its own identity");
        assertJapanese(controller.getSkillNameJapanese());
        assertJapanese(controller.getSkillNameEnglish());
        assertEquals(8, controller.getPhaseQuantity());
        assertEquals(IntStream.range(0, 8).mapToObj(i -> List.of(i, 0)).toList(), suffix(controller));
        assertNotSame(publicController(f), controller);
        assertNotSame(publicController(f).getPhasesInfo(), controller.getPhasesInfo());
        for (int phase = 0; phase < 8; phase++) {
            assertNotSame(publicController(f).getPhasesInfo().get(phase), controller.getPhasesInfo().get(phase));
            assertNotSame(publicController(f).getPhasesInfo().get(phase).getSkillUnitCollection(),
                    controller.getPhasesInfo().get(phase).getSkillUnitCollection());
        }
        assertNotSame(event(publicController(f), 7, 35, CEventVal.class, 0, 0),
                event(controller, 7, 35, CEventVal.class, 0, 0));
        assertEquals(originalController, payload(publicController(f)), "public HEATBOMB06 remains unchanged");
        assertTrue(before.publicSkills().equals(payload(f.host().getSkillList().subList(0, before.hostCount()))),
                "complete prepared public skill prefix must stay byte-for-byte equivalent");
        byte[] after = writeWaz(f.host(), "after-public");
        assertArrayEquals(publicBytes, Arrays.copyOf(after, publicBytes.length), "all original public WAZ bytes are a preserved prefix");
        assertOriginalMainExceptPairs(f, before);
        assertTrue(before.otherPublic().equals(otherPublic(f)), "other inherited public hosts cannot be changed");
        assertTrue(selectedMain.equals(payload(f.context().getSelectedPackage().getWazByFileName())),
                "the converted selected source view cannot be edited through an alias");
        assertEquals(before.plan(), payload(f.plan()), "private append must not change the inherited BHE source append range");
        assertMekAndMaterialUnchanged(f, before);
        assertEquals(f.host().getSkillList().size(), hostGroup(f).getParam(), "public WazaGroup must include the new private controller");
        assertEquals(before.groups().size(), f.baseline().getWazaGroupGrp().getWazaList().size(), "no new top-level WazaGroup");
        for (int i = 0; i < before.groups().size(); i++) {
            if (i != f.hostGroup())
                assertEquals(before.groups().get(i), payload(f.baseline().getWazaGroupGrp().getWazaList().get(i)));
        }
        assertSameHostReferences(f, controller);
        assertSourceUnchanged(f);
    }

    @Test
    void completeBinaryGenerationAndReadbackKeepThePairedProtocolAndCounts() throws Exception {
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        privateController(f); // missing implementation must fail an assertion, never a parse/class error
        BsdxBinService service = new BsdxBinService();
        Path mekPath = output.resolve("roundtrip-sou.mek");
        Path mainPath = output.resolve("roundtrip-sou.waz");
        Path hostPath = output.resolve("roundtrip-public.waz");
        Path groupPath = output.resolve("WazaGroup.grp");
        service.generate(mekPath.toString(), f.mek(), CHARSET);
        service.generate(mainPath.toString(), f.main(), CHARSET);
        service.generate(hostPath.toString(), f.host(), CHARSET);
        service.generate(groupPath.toString(), f.baseline().getWazaGroupGrp(), CHARSET);
        Mek mek = (Mek) service.parse(mekPath.toString(), CHARSET).getData();
        Waz main = (Waz) service.parse(mainPath.toString(), CHARSET).getData();
        Waz host = (Waz) service.parse(hostPath.toString(), CHARSET).getData();
        WazaGroupGrp groups = (WazaGroupGrp) service.parse(groupPath.toString(), CHARSET).getData();
        assertEquals(payload(f.mek().getMekWeaponInfoMap()), payload(mek.getMekWeaponInfoMap()));
        assertEquals(payload(f.mek().getMekMaterialBlock()), payload(mek.getMekMaterialBlock()));
        assertEquals(f.host().getSkillList().size(), groups.getWazaList().get(f.hostGroup()).getParam());
        assertArrayEquals(Files.readAllBytes(mainPath), writeWaz(main, "parsed-complete-main"),
                "the complete generated Sou WAZ survives parse/generate byte for byte, including reserved empty entries");
        assertArrayEquals(Files.readAllBytes(hostPath), writeWaz(host, "parsed-complete-host"),
                "the complete public prefix and private tail survive parse/generate byte for byte");
        // Exercise the production whole-host writer as well. This separate filename-only plan
        // deliberately excludes media copying; the fixture's real inherited append plan stays intact.
        BheCommonProjectileAppendPlan fileOutputPlan = new BheCommonProjectileAppendPlan();
        fileOutputPlan.getCommonProjectileWazFiles().add(f.hostName());
        Path imported = output.resolve("common-writer");
        Files.createDirectories(imported);
        Files.write(imported.resolve(f.hostName()), new byte[]{0, 0, 0, 0});
        TsukuyomiImportedAssetSet assets = new TsukuyomiImportedAssetSet();
        new OutputBheCommonProjectileResourcesStep().output(f.context().getRequest(), f.baseline(), fileOutputPlan,
                Map.of(), imported, assets);
        assertEquals(List.of(imported.resolve(f.hostName())), assets.getGeneratedWazFiles());
        assertTrue(assets.getMissingAssets().isEmpty());
        assertArrayEquals(Files.readAllBytes(hostPath), Files.readAllBytes(imported.resolve(f.hostName())),
                "production common output overwrites inherited files with the full host including the private tail");
        f.context().setReboundMek(mek);
        f.context().setReboundWaz(main);
        String inheritedKey = f.baseline().getWazByFileName().keySet().stream()
                .filter(name -> name.equalsIgnoreCase(f.hostName())).findFirst().orElseThrow();
        f.baseline().getWazByFileName().put(inheritedKey, host);
        Fixture parsed = f.withResources(mek, main, host);
        assertProtocol(parsed, privateController(parsed));
        assertSourceUnchanged(f);
    }

    @Test
    void secondApplicationKeepsTheSameControllerCallerAndHostObjects() throws Exception {
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill privateChild = privateController(f);
        List<Waz.Skill> paired = paired(f);
        List<Waz.Skill> hostList = f.host().getSkillList();
        List<Waz.Skill> mainList = f.main().getSkillList();
        WazaGroupGrp.WazaGroupEntry group = hostGroup(f);
        JsonNode main = payload(f.main()), host = payload(f.host()), plan = payload(f.plan());
        byte[] firstMain = writeWaz(f.main(), "first-main");
        byte[] firstHost = writeWaz(f.host(), "first-host");

        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());

        assertSame(hostList, f.host().getSkillList(), "a verified repeat does not replace the public skill list");
        assertSame(mainList, f.main().getSkillList());
        assertSame(privateChild, privateController(f));
        assertSame(group, hostGroup(f));
        for (int i = 0; i < paired.size(); i++) assertSame(paired.get(i), paired(f).get(i));
        assertTrue(main.equals(payload(f.main())));
        assertTrue(host.equals(payload(f.host())));
        assertEquals(plan, payload(f.plan()));
        assertArrayEquals(firstMain, writeWaz(f.main(), "second-main"));
        assertArrayEquals(firstHost, writeWaz(f.host(), "second-host"));
        assertSourceUnchanged(f);
    }

    @Test
    void lateFamilyBranchOrSuffixDriftRejectsWithoutAnyNewChildOrCallerWrites() throws Exception {
        for (Consumer<Fixture> corrupt : List.<Consumer<Fixture>>of(
                f -> change(family(f.main(), 2), 1, 3).getBsdxInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(4))),
                f -> family(f.main(), 2).getSkillSuffixList().get(13).setInt2(1),
                f -> family(f.main(), 1).setSkillNameJapanese("別名：人間砲弾Lv2"),
                f -> {
                    Waz.Skill duplicate = copy(family(f.main(), 2), Waz.Skill.class);
                    f.main().getSkillList().add(duplicate);
                    f.baseline().getWazaGroupGrp().getWazaList().get(f.mainGroup()).setParam(f.main().getSkillList().size());
                })) {
            Fixture f = fixture(); // only SouSkillGlitchFixer has primed the old MEK baseline
            corrupt.accept(f);
            assertRejectedWithoutExtraWrites(f);
        }
    }

    @Test
    void changedPublicReleaseInputLoopOrHostCountRejectsTheCompleteBatch() throws Exception {
        for (Consumer<Fixture> corrupt : List.<Consumer<Fixture>>of(
                f -> event(publicController(f), 7, 35, CEventVal.class, 0, 0).setInt4(1),
                f -> publicController(f).getSkillSuffixList().get(7).setInt1(6),
                f -> change(publicController(f), 0, 1).getBsdxInfoCollectionList1().get(0).setTypeList(new ArrayList<>(List.of(2, 4, 1))),
                f -> {
                    SkillInfoUnknown loop = new SkillInfoUnknown(255);
                    loop.setStartFrame(0); loop.setEndFrame(0);
                    loop.setInt1(-1); loop.setInt2(1); loop.setInt3(-2);
                    unit(publicController(f), 7, 35).getSkillInfoUnknownList().add(loop);
                },
                f -> unit(family(f.main(), 2), 0, 71).getSkillInfoUnknownList().get(0).setEndFrame(113),
                f -> hostGroup(f).setParam(hostGroup(f).getParam() - 1))) {
            Fixture f = fixture();
            corrupt.accept(f);
            assertRejectedWithoutExtraWrites(f);
        }
    }

    @Test
    void halfConvertedWaiterCannotBeRepairedBySilentlyAppendingAnotherController() throws Exception {
        Fixture f = fixture();
        change(family(f.main(), 0), 0, 0).getBsdxInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(15)));
        assertRejectedWithoutExtraWrites(f);
    }

    @Test
    void changedAlreadyInstalledPrivateControllerIsRejectedWithoutReplacingThePair() throws Exception {
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill child = privateController(f);
        event(child, 7, 35, CEventVal.class, 0, 0).setInt4(17);
        assertRejectedWithoutExtraWrites(f);
        assertSame(child, privateController(f));
    }

    @Test
    void holdPhasesCannotIntroduceAnEarlyReleaseCounterWriteOrCounterClockLoop() throws Exception {
        for (int phase : List.of(5, 6)) for (boolean counterLoop : List.of(false, true)) {
            Fixture f = fixture(); // fresh old baseline; the new paired codec has never run
            Waz.Skill prototype = publicController(f);
            assertTrue(prototype.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                    .noneMatch(value -> Integer.valueOf(35).equals(value.getUnitQuantity())),
                    "the real controller hold phase originally has no counter writer or FF slot");
            SkillUnit injected = new SkillUnit(35, "持続phaseの不正なrelease入力");
            if (counterLoop) {
                SkillInfoUnknown loop = new SkillInfoUnknown(255);
                loop.setStartFrame(0); loop.setEndFrame(0);
                loop.setInt1(-1); loop.setInt2(1); loop.setInt3(-2);
                injected.getSkillInfoUnknownList().add(loop);
            } else {
                CEventVal earlyRelease = new CEventVal(0);
                earlyRelease.setSlotNum(35);
                earlyRelease.setStartFrame(0); earlyRelease.setEndFrame(0);
                earlyRelease.setInt1(0); earlyRelease.setInt2(16);
                earlyRelease.setInt3(0); earlyRelease.setInt4(16);
                injected.getSkillInfoObjectList().add(earlyRelease);
            }
            prototype.getPhasesInfo().get(phase).getSkillUnitCollection().add(injected);
            assertRejectedWithoutExtraWrites(f);
        }
    }

    private Fixture fixture() throws Exception {
        Map<Path, byte[]> sourceFiles = new LinkedHashMap<>();
        Path mekPath = BHE.resolve("mek/sou/sou.mek");
        Path mainPath = BHE.resolve("waz/sou/sou.waz");
        Path sourceTamaPath = resolve(BHE.resolve("waz"), "tama05.waz");
        Path nativeTamaPath = resolve(DX.resolve("waz"), "Tama05.waz");
        for (Path p : List.of(mekPath, mainPath, sourceTamaPath, nativeTamaPath))
            sourceFiles.put(p, Files.readAllBytes(p));
        BheBinService bhe = new BheBinService();
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) bhe.parse(mekPath.toString(), CHARSET).getData();
        var rawMain = (com.giga.nexas.dto.bhe.waz.Waz) bhe.parse(mainPath.toString(), CHARSET).getData();
        TsukuyomiRawSourceBundle raw = loadCommonSource(bhe);
        raw.setTsukuyomiMek(rawMek);
        raw.getWazByFileName().put("sou.waz", rawMain);
        var rawTama = raw.getCommonProjectileWazByFileName().get("tama05.waz");
        JsonNode beforeRawMek = JSON.valueToTree(rawMek);
        JsonNode beforeRawMain = JSON.valueToTree(rawMain);
        JsonNode beforeRawTama = JSON.valueToTree(rawTama);
        TsukuyomiBsdxBaselineBundle baseline = loadBaseline();
        TsukuyomiConvertedBundle prepared = new TsukuyomiConvertedBundle();
        new BheCommonProjectilePrepareStep().prepare(new TsukuyomiGraftRequest(), raw, baseline, prepared);
        BheCommonProjectileAppendPlan plan = prepared.getCommonProjectileAppendPlan();
        int sourceTamaGroup = sourceTamaGroup(raw);
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceTamaGroup);
        int hostGroup = plan.getSourceWazIndexToTargetIndex().get(sourceTamaGroup);
        int publicBase = plan.getSourceWazIndexToTargetSkillBase().get(sourceTamaGroup);
        Waz host = findHost(baseline, hostName);
        int sourceControllerIndex = sourceControllerIndex(rawTama);
        int publicControllerIndex = publicBase + sourceControllerIndex;
        assertEquals("HEATBOMB06", host.getSkillList().get(publicControllerIndex).getSkillNameEnglish());
        assertEquals("★蒼：人間砲弾", host.getSkillList().get(publicControllerIndex).getSkillNameJapanese());

        Mek mek = new BheToBsdxMekConverter().convert(rawMek);
        Waz main = new BheToBsdxWazConverter().convert(rawMain);
        TsukuyomiPackageBundle selected = new TsukuyomiPackageBundle();
        selected.setTsukuyomiMek(mek);
        selected.getWazByFileName().put("sou.waz", copy(main, Waz.class));
        TsukuyomiGrpAppendPlan privatePlan = new TsukuyomiGrpAppendPlan();
        int mainGroup = baseline.getWazaGroupGrp().getWazaList().size();
        privatePlan.setWazaGroupIndex(mainGroup);
        privatePlan.getSourceWazGroupIndexToTargetIndex().put(rawMek.getMekBasicInfo().getWazFileSequence(), mainGroup);
        // The real Flare caller's role-private SE pair uses this nonidentity mapping; audio delivery is outside this protocol fixture.
        privatePlan.getSourceSeGroupIndexToTargetIndex().put(16, 46);
        privatePlan.getSourceSeItemIndexToTargetIndexByGroup().put(16, new LinkedHashMap<>(Map.of(4, 11)));
        BheResourceIndexResolver resolver = new BheResourceIndexResolver(plan, privatePlan);
        rebindWazaSelects(main, resolver, Collections.newSetFromMap(new IdentityHashMap<>()));
        new BheInfoCollectionObjectGraphRewriter().rewrite(main, "SouHumanCannonCounterTest main");
        mek.getMekBasicInfo().setWazFileSequence(mainGroup);
        // This is a Sou protocol boundary fixture, not full private SPM/SE/voice delivery validation.
        // Common hosts and every WAZ-to-WAZ select use the real production append plan/resolver.
        Waz.Skill lookalikeJapanese = copy(family(main, 2), Waz.Skill.class);
        lookalikeJapanese.setSkillNameJapanese("別機：人間砲弾Lv3");
        main.getSkillList().add(lookalikeJapanese);
        Waz.Skill lookalikeEnglish = copy(family(main, 2), Waz.Skill.class);
        lookalikeEnglish.setSkillNameEnglish("TEST_UNPAIRED_HUMAN_CANNON");
        main.getSkillList().add(lookalikeEnglish);
        WazaGroupGrp.WazaGroupEntry mainEntry = new WazaGroupGrp.WazaGroupEntry();
        mainEntry.setExistFlag(1); mainEntry.setWazaName("sou");
        mainEntry.setWazaCodeName("SOU"); mainEntry.setWazaDisplayName("蒼");
        mainEntry.setParam(main.getSkillList().size());
        baseline.getWazaGroupGrp().getWazaList().add(mainEntry);
        baseline.getWazByFileName().put("sou.waz", main);
        privatePlan.getTargetWazSkillCountByGroupIndex().put(mainGroup, main.getSkillList().size());

        FollowupGraftRequest request = new FollowupGraftRequest();
        request.setMekaCodeName("SOU"); request.setWazCodeName("SOU");
        request.setWazFileName("sou.waz"); request.setMekFileName("Sou.mek"); request.setSpriteFileName("sou.spm");
        TsukuyomiConvertedBundle currentSou = new TsukuyomiConvertedBundle();
        currentSou.setCommonProjectileAppendPlan(plan);
        currentSou.setRawSourceBundle(raw);
        currentSou.setPreparedBaselineBundle(baseline);
        currentSou.setSelectedResourceBundle(selected);
        FollowupMekaContext context = new FollowupMekaContext();
        context.setRequest(request); context.setBaseline(baseline); context.setConvertedBundle(currentSou);
        context.setSelectedPackage(selected); context.setGrpAppendPlan(privatePlan);
        context.setReboundMek(mek); context.setReboundWaz(main);
        SouSkillGlitchFixer.INSTANCE.fix(context); // baseline prime must not install the counter codec
        // Prime the other role repairs so complete non-counter graphs stay stable; the first public call still installs the counter.
        SouCursedBladeSingleTargetFixer.fix(context);
        SouSnowMoonSingleTargetFixer.fix(context);
        SouFlareSeekerSingleTargetFixer.fix(context);
        SouCoolGainReturnProtocolFixer.fix(context);
        SouBoostProcterDashContinuationFixer.fix(context);
        assertEquals(211, mek.getMekWeaponInfoMap().size());
        assertTrue(currentSou.getCommonProjectileResourceBundle().getWazByFileName().isEmpty(),
                "Sou inherits prepared hosts; a fresh Sou converted common bundle is empty in the real pipeline");
        Fixture f = new Fixture(context, baseline, plan, mek, main, host, hostName, hostGroup, mainGroup,
                publicControllerIndex, host.getSkillList().size(), rawMek, rawMain, rawTama,
                beforeRawMek, beforeRawMain, beforeRawTama, sourceFiles);
        assertSourceUnchanged(f);
        return f;
    }

    private static TsukuyomiRawSourceBundle loadCommonSource(BheBinService service) throws Exception {
        TsukuyomiRawSourceBundle raw = new TsukuyomiRawSourceBundle();
        raw.setWazaGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp)
                service.parse(resolve(BHE.resolve("grp"), "WazaGroup.grp").toString(), CHARSET).getData());
        raw.setSpriteGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp)
                service.parse(resolve(BHE.resolve("grp"), "SpriteGroup.grp").toString(), CHARSET).getData());
        raw.setSeGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp)
                service.parse(resolve(BHE.resolve("grp"), "SeGroup.grp").toString(), CHARSET).getData());
        for (String name : BheCommonProjectileResources.commonProjectileWazFiles())
            raw.getCommonProjectileWazByFileName().put(name, (com.giga.nexas.dto.bhe.waz.Waz)
                    service.parse(resolve(BHE.resolve("waz"), name).toString(), CHARSET).getData());
        for (String name : BheCommonProjectileResources.commonProjectileSpmFiles())
            raw.getCommonProjectileSpmByFileName().put(name, (com.giga.nexas.dto.bhe.spm.Spm)
                    service.parse(resolve(BHE.resolve("spm"), name).toString(), CHARSET).getData());
        return raw;
    }

    private static TsukuyomiBsdxBaselineBundle loadBaseline() throws Exception {
        BsdxBinService service = new BsdxBinService();
        TsukuyomiBsdxBaselineBundle b = new TsukuyomiBsdxBaselineBundle();
        b.setWazaGroupGrp((WazaGroupGrp) service.parse(resolve(DX.resolve("grp"), "WazaGroup.grp").toString(), CHARSET).getData());
        b.setSpriteGroupGrp((SpriteGroupGrp) service.parse(resolve(DX.resolve("grp"), "SpriteGroup.grp").toString(), CHARSET).getData());
        b.setSeGroupGrp((SeGroupGrp) service.parse(resolve(DX.resolve("grp"), "SeGroup.grp").toString(), CHARSET).getData());
        b.setBatVoiceGrp((BatVoiceGrp) service.parse(resolve(DX.resolve("grp"), "BatVoice.grp").toString(), CHARSET).getData());
        b.setProgramMaterialGrp((ProgramMaterialGrp) service.parse(resolve(DX.resolve("grp"), "ProgramMaterial.grp").toString(), CHARSET).getData());
        b.setMapGroupGrp((MapGroupGrp) service.parse(resolve(DX.resolve("grp"), "MapGroup.grp").toString(), CHARSET).getData());
        CommonProjectileTestSupport.loadBsdxHostCommonResources(b);
        return b;
    }

    private static int sourceTamaGroup(TsukuyomiRawSourceBundle raw) {
        var entries = raw.getWazaGroupGrp().getWazaList();
        var matches = IntStream.range(0, entries.size()).filter(i -> {
            return "TAMA05".equalsIgnoreCase(entries.get(i).getWazaCodeName());
        }).boxed().toList();
        assertEquals(1, matches.size(), "real BHE Tama05 WazaGroup identity");
        return matches.get(0);
    }

    private static int sourceControllerIndex(com.giga.nexas.dto.bhe.waz.Waz source) {
        var matches = IntStream.range(0, source.getSkillList().size()).filter(i -> {
            var s = source.getSkillList().get(i);
            return s != null && "HEATBOMB06".equals(s.getSkillNameEnglish()) && "★蒼：人間砲弾".equals(s.getSkillNameJapanese());
        }).boxed().toList();
        assertEquals(1, matches.size());
        return matches.get(0);
    }

    private static Waz findHost(TsukuyomiBsdxBaselineBundle baseline, String name) {
        var matches = baseline.getWazByFileName().entrySet().stream().filter(e -> name.equalsIgnoreCase(e.getKey())).toList();
        assertEquals(1, matches.size());
        return matches.get(0).getValue();
    }

    private static void rebindWazaSelects(Object node, BheResourceIndexResolver resolver, Set<Object> seen) throws Exception {
        if (node == null || node instanceof Number || node instanceof String || node instanceof Boolean || !seen.add(node)) return;
        if (node instanceof CEventSe se && se.getByteDataList() != null) {
            List<byte[]> rebound = null;
            for (int i = 0; i < se.getByteDataList().size(); i++) {
                byte[] source = se.getByteDataList().get(i);
                if (source == null || source.length < 8) continue;
                var pair = java.nio.ByteBuffer.wrap(source).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                if (pair.getInt(0) != 16 || pair.getInt(4) != 4) continue;
                var target = resolver.resolveSe(16, 4);
                byte[] independent = source.clone();
                var bytes = java.nio.ByteBuffer.wrap(independent).order(java.nio.ByteOrder.LITTLE_ENDIAN);
                bytes.putInt(0, target.targetGroupIndex()); bytes.putInt(4, target.targetItemIndex());
                if (rebound == null) rebound = new ArrayList<>(se.getByteDataList());
                rebound.set(i, independent);
            }
            if (rebound != null) se.setByteDataList(rebound);
            return;
        }
        if (node instanceof CEventWazaSelect ref && ref.getWazFileNo() != null && ref.getWazFileNo() >= 0) {
            int sourceGroup = ref.getWazFileNo();
            ref.setWazSequenceNo(resolver.resolveWazSkillIndex(sourceGroup, ref.getWazSequenceNo()));
            ref.setWazFileNo(resolver.resolveWazGroupIndex(sourceGroup));
            return;
        }
        if (node instanceof Collection<?> collection) {
            for (Object item : collection) rebindWazaSelects(item, resolver, seen);
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Object item : map.values()) rebindWazaSelects(item, resolver, seen);
            return;
        }
        if (!node.getClass().getName().startsWith("com.giga.nexas.dto.")) return;
        for (Class<?> type = node.getClass(); type != null; type = type.getSuperclass())
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                field.setAccessible(true);
                rebindWazaSelects(field.get(node), resolver, seen);
            }
    }

    private static Waz.Skill family(Waz main, int levelIndex) {
        var matches = main.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> ENGLISH.get(levelIndex).equals(s.getSkillNameEnglish()) && JAPANESE.get(levelIndex).equals(s.getSkillNameJapanese())).toList();
        assertEquals(1, matches.size(), "unique paired Sou family identity: " + ENGLISH.get(levelIndex));
        return matches.get(0);
    }

    private static List<Waz.Skill> paired(Fixture f) {
        return IntStream.range(0, 3).mapToObj(i -> family(f.main(), i)).toList();
    }

    private static Waz.Skill publicController(Fixture f) {
        return f.host().getSkillList().get(f.publicControllerIndex());
    }

    private static Waz.Skill privateController(Fixture f) {
        assertEquals(f.publicCount() + 1, f.host().getSkillList().size(),
                "Sou must append one private controller to the inherited Tama05 host through the public Customizer entry");
        Set<Integer> destinations = new HashSet<>();
        for (Waz.Skill main : paired(f)) {
            CEventWazaSelect ref = controllerReference(main);
            assertEquals(f.hostGroup(), ref.getWazFileNo(), "private control remains under the existing public host group");
            assertTrue(ref.getWazSequenceNo() >= f.publicCount(), "paired caller cannot keep pointing at public HEATBOMB06");
            assertTrue(ref.getWazSequenceNo() < f.host().getSkillList().size());
            destinations.add(ref.getWazSequenceNo());
        }
        assertEquals(1, destinations.size(), "all three levels share the one paired private controller");
        return f.host().getSkillList().get(destinations.iterator().next());
    }

    private static CEventWazaSelect controllerReference(Waz.Skill main) {
        var effects = events(main, 0, 48).stream().filter(CEventEffect.class::isInstance).map(CEventEffect.class::cast).toList();
        var refs = effects.stream().flatMap(e -> e.getCeventEffectUnitList().stream())
                .filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum()) && Integer.valueOf(1).equals(u.getBuffer()))
                .map(CEventEffect.CEventEffectUnit::getData).filter(CEventWazaSelect.class::isInstance).map(CEventWazaSelect.class::cast)
                .filter(r -> r.getWazFileNo() >= 0 && r.getWazFileNo() != 0).toList();
        assertEquals(1, refs.size(), "one real controller request; group0 decoration does not count");
        return refs.get(0);
    }

    private static void assertProtocol(Fixture f, Waz.Skill controller) {
        CEventVal initial = event(controller, 0, 35, CEventVal.class, 0, 0);
        assertVal(initial, List.of(0, 0, 0, 0));
        assertEquals(1, events(controller, 0, 35).size());
        assertEquals(0, applyPointVal(initial, 1234), "private entry explicitly resets the level instead of inheriting another object's value");
        CEventVal release = event(controller, 7, 35, CEventVal.class, 0, 0);
        assertVal(release, List.of(0, 16, 1, 16));
        assertEquals(1, events(controller, 7, 35).size(),
                "replace the original absolute-zero event; keeping it before an appended +16 hides the release under native first-active selection");
        assertTrue(unit(controller, 0, 35).getSkillInfoUnknownList().isEmpty());
        assertTrue(unit(controller, 7, 35).getSkillInfoUnknownList().isEmpty(), "release offset is applied once, never by a repeating FF");
        assertEquals(1, events(controller, 7, 71).size());
        assertTrue(unit(controller, 7, 71).getSkillInfoUnknownList().isEmpty(), "release phase has no local clock rewind or back edge");
        assertTrue(controllerExceptProtocol(publicController(f)).equals(controllerExceptProtocol(controller)),
                "only identity, entry counter initialization and release counter replacement may differ from the public controller");
        for (int p = 1; p <= 4; p++)
            assertEquals(payload(events(publicController(f), p, 35)), payload(events(controller, p, 35)),
                    "original charging level increment remains intact");
        assertEquals(payload(events(publicController(f), 7, 71)), payload(events(controller, 7, 71)), "65f child end is not replaced with a forced parent END");
        for (int p : List.of(0, 5, 6))
            assertEquals(payload(events(publicController(f), p, 71)), payload(events(controller, p, 71)),
                    "ordinary key-release, direction and WAZACOUNT transitions retain their complete original payloads");
        for (int tier = 0; tier < 3; tier++) {
            Waz.Skill main = family(f.main(), tier);
            assertEquals(14, main.getPhaseQuantity());
            assertEquals(IntStream.range(0, 14).mapToObj(i -> List.of(i, 0)).toList(), suffix(main));
            CEventChange wait = change(main, 0, 0);
            assertEquals(WAIT_WINDOWS.get(tier).get(0), wait.getStartFrame());
            assertEquals(WAIT_WINDOWS.get(tier).get(1), wait.getEndFrame());
            assertEquals(List.of(WAIT_WINDOWS.get(tier).get(0), WAIT_WINDOWS.get(tier).get(1), -1, 1, -2),
                    loopSignature(unit(main, 0, 71).getSkillInfoUnknownList().get(0)), "each original level retains its own wait window and FF");
            for (int level = 0; level <= 4; level++) {
                assertFalse(evaluateComparison(wait.getBsdxInfoCollectionList1().get(0), level),
                        main.getSkillNameEnglish() + " level" + level + " must keep waiting while attack is held");
                int released = applyPointVal(release, level);
                assertTrue(evaluateComparison(wait.getBsdxInfoCollectionList1().get(0), released),
                        "release must open the original NEXT path even at level0");
                int selectedEntry = selectFlightEntry(main, released);
                assertEquals(FLIGHT_ENTRIES.get(level), selectedEntry, main.getSkillNameEnglish() + " must preserve original level" + level + " flight choice");
                assertEquals(FLIGHT_ENTRIES.get(level), main.getSkillSuffixList().get(selectedEntry).getInt1());
            }
        }
    }

    /** Only the two reviewed Sou scalar predicates are executed; this is not a channel VM. */
    private static boolean evaluateComparison(BsdxInfoCollection condition, int counter) {
        assertEquals(0, condition.getInt1()); assertEquals(0, condition.getInt2());
        assertEquals(List.of(14, 0), condition.getIntList3(), "LASTTAMA uses the BSDX object dictionary");
        assertEquals(List.of(), condition.getIntList4());
        assertEquals(1, condition.getParamList().size());
        if (List.of(2, 0, 0, 5).equals(condition.getTypeList())) return counter > condition.getParamList().get(0);
        assertEquals(List.of(2, 0, 0, 0), condition.getTypeList());
        return counter == condition.getParamList().get(0);
    }

    private static int applyPointVal(CEventVal value, int previous) {
        assertEquals(0, value.getTypeId()); assertEquals(0, value.getStartFrame()); assertEquals(0, value.getEndFrame());
        assertEquals(0, value.getInt1()); assertEquals(value.getInt2(), value.getInt4(), "point delta is constant at both endpoints");
        return switch (value.getInt3()) {
            case 0 -> value.getInt4();
            case 1 -> previous + value.getInt4();
            default -> throw new AssertionError("unreviewed scalar operation");
        };
    }

    private static int selectFlightEntry(Waz.Skill main, int counter) {
        var branches = events(main, 1, 71);
        assertEquals(5, branches.size());
        for (SkillInfoObject value : branches) {
            assertInstanceOf(CEventChange.class, value);
            CEventChange branch = (CEventChange) value;
            assertEquals(1, branch.getFlag()); assertEquals(0, branch.getInt1());
            assertEquals(0, branch.getStartFrame()); assertEquals(0, branch.getEndFrame());
            var condition = branch.getBsdxInfoCollectionList1().get(0);
            boolean chosen = List.of(0).equals(condition.getTypeList()) || evaluateComparison(condition, counter);
            if (chosen) {
                var target = branch.getBsdxInfoCollectionList2().get(0);
                assertEquals(16, target.getInt1()); assertEquals(List.of(0, 1), target.getTypeList());
                assertEquals(1, target.getParamList().size());
                return target.getParamList().get(0);
            }
        }
        throw new AssertionError("Sou flight selection has no fallback");
    }

    private static void assertSameHostReferences(Fixture f, Waz.Skill privateChild) {
        Waz.Skill prototype = publicController(f);
        for (int p = 0; p < prototype.getPhasesInfo().size(); p++)
            assertEquals(payload(events(prototype, p, 48)), payload(events(privateChild, p, 48)),
                    "private control must retain rebound public effects under their real hosts");
        assertEquals(payload(prototype.getSkillSuffixList()), payload(privateChild.getSkillSuffixList()));
    }

    private static void assertVal(CEventVal value, List<Integer> four) {
        assertEquals(0, value.getTypeId());
        assertEquals(four, List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4()));
    }

    private static WazaGroupGrp.WazaGroupEntry hostGroup(Fixture f) {
        return f.baseline().getWazaGroupGrp().getWazaList().get(f.hostGroup());
    }

    private static SkillUnit unit(Waz.Skill skill, int phase, int slot) {
        var matches = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        assertEquals(1, matches.size(), skill.getSkillNameEnglish() + " phase" + phase + " slot" + slot);
        return matches.get(0);
    }

    private static List<SkillInfoObject> events(Waz.Skill skill, int phase, int slot) {
        var matches = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        assertTrue(matches.size() <= 1);
        return matches.isEmpty() ? List.of() : matches.get(0).getSkillInfoObjectList();
    }

    private static <T extends SkillInfoObject> T event(Waz.Skill skill, int phase, int slot, Class<T> type, int start, int end) {
        var matches = events(skill, phase, slot).stream().filter(type::isInstance).map(type::cast)
                .filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame())).toList();
        assertEquals(1, matches.size(), skill.getSkillNameEnglish() + " phase" + phase + " slot" + slot + " " + start + ".." + end);
        return matches.get(0);
    }

    private static CEventChange change(Waz.Skill skill, int phase, int ordinal) {
        var values = events(skill, phase, 71);
        assertTrue(ordinal < values.size());
        assertInstanceOf(CEventChange.class, values.get(ordinal));
        return (CEventChange) values.get(ordinal);
    }

    private static List<Integer> loopSignature(SkillInfoUnknown loop) {
        return List.of(loop.getStartFrame(), loop.getEndFrame(), loop.getInt1(), loop.getInt2(), loop.getInt3());
    }

    private static List<List<Integer>> suffix(Waz.Skill skill) {
        return skill.getSkillSuffixList().stream().map(s -> List.of(s.getInt1(), s.getInt2())).toList();
    }

    private static Snapshot snapshot(Fixture f) {
        return new Snapshot(f.main().getSkillList().size(), f.host().getSkillList().size(),
                payload(f.mek()), payload(f.main().getSkillList()), payload(f.host().getSkillList()),
                payload(f.plan()), otherPublic(f), payload(f.baseline().getWazaGroupGrp().getWazaList()));
    }

    private static JsonNode otherPublic(Fixture f) {
        Map<String, Waz> others = new LinkedHashMap<>();
        f.baseline().getWazByFileName().forEach((name, waz) -> {
            if (!name.equalsIgnoreCase(f.hostName()) && !name.equalsIgnoreCase("sou.waz")) others.put(name, waz);
        });
        return payload(others);
    }

    private static void assertOriginalMainExceptPairs(Fixture f, Snapshot before) {
        Set<Waz.Skill> paired = Collections.newSetFromMap(new IdentityHashMap<>());
        paired.addAll(paired(f));
        for (int i = 0; i < before.mainCount(); i++) {
            Waz.Skill skill = f.main().getSkillList().get(i);
            if (!paired.contains(skill)) assertTrue(before.mainSkills().get(i).equals(payload(skill)),
                    "unpaired main skill " + i + " must be retained, including both-name lookalikes");
            else assertTrue(callerExceptProtocol(before.mainSkills().get(i)).equals(callerExceptProtocol(payload(skill))),
                    "paired caller " + i + " may change only its controller target and the five scalar comparison constants; gravity and exits remain intact");
        }
    }

    private static JsonNode controllerExceptProtocol(Waz.Skill source) {
        ObjectNode tree = (ObjectNode) payload(source);
        tree.remove(List.of("skillNameJapanese", "skillNameEnglish"));
        for (int phase : List.of(0, 7)) {
            var units = tree.path("phasesInfo").get(phase).path("skillUnitCollection");
            for (var iterator = units.elements(); iterator.hasNext();) {
                JsonNode unit = iterator.next();
                if (unit.path("unitQuantity").asInt(-1) == 35) iterator.remove();
            }
        }
        return tree;
    }

    private static JsonNode callerExceptProtocol(JsonNode source) {
        JsonNode tree = source.deepCopy();
        for (JsonNode unit : tree.path("phasesInfo").get(0).path("skillUnitCollection")) {
            if (unit.path("unitQuantity").asInt(-1) == 48) {
                for (JsonNode event : unit.path("skillInfoObjectList")) for (JsonNode inner : event.path("ceventEffectUnitList")) {
                    JsonNode data = inner.path("data");
                    if (inner.path("unitSlotNum").asInt(-1) == 3 && data.path("wazFileNo").asInt(-1) > 0)
                        ((ObjectNode) data).put("wazSequenceNo", -1);
                }
            }
            if (unit.path("unitQuantity").asInt(-1) == 71)
                ((ObjectNode) unit.path("skillInfoObjectList").get(0).path("bsdxInfoCollectionList1").get(0)).putArray("paramList");
        }
        for (JsonNode unit : tree.path("phasesInfo").get(1).path("skillUnitCollection")) {
            if (unit.path("unitQuantity").asInt(-1) == 71) {
                JsonNode branches = unit.path("skillInfoObjectList");
                for (int i = 0; i < 4; i++)
                    ((ObjectNode) branches.get(i).path("bsdxInfoCollectionList1").get(0)).putArray("paramList");
            }
        }
        return tree;
    }

    private static void assertMekAndMaterialUnchanged(Fixture f, Snapshot before) {
        assertTrue(before.mek().equals(payload(f.mek())), "counter repair must add no MEK rows, hide no extra menu row, and preserve Material/AI");
        assertEquals(211, f.mek().getMekWeaponInfoMap().size());
        assertEquals(211, f.mek().getMekMaterialBlock().getTrailingEntries().size());
        assertEquals(IntStream.range(0, 153).boxed().toList(),
                f.mek().getMekWeaponInfoMap().entrySet().stream()
                        .filter(e -> (e.getValue().getWeaponIdentifier() & 2) == 0).map(Map.Entry::getKey).toList());
        assertEquals("N_HOUDAN", f.mek().getMekWeaponInfoMap().get(30).getWeaponSequence());
        assertEquals(0, f.mek().getMekWeaponInfoMap().get(30).getWeaponIdentifier());
    }

    private static void assertRejectedWithoutExtraWrites(Fixture f) throws Exception {
        Snapshot before = snapshot(f);
        JsonNode groups = payload(f.baseline().getWazaGroupGrp());
        assertThrows(IllegalStateException.class, () -> SouCustomizer.INSTANCE.customizeBeforeWrite(f.context()),
                "drift or a half-installed protocol is rejected on the old MEK baseline without creating another child");
        assertTrue(before.mek().equals(payload(f.mek())));
        assertTrue(before.mainSkills().equals(payload(f.main().getSkillList())), "no partial caller rewrite");
        assertTrue(before.publicSkills().equals(payload(f.host().getSkillList())), "no partial controller append");
        assertEquals(before.plan(), payload(f.plan()));
        assertEquals(groups, payload(f.baseline().getWazaGroupGrp()), "no partial public count update");
        assertTrue(before.otherPublic().equals(otherPublic(f)));
        assertSourceUnchanged(f);
    }

    private static void assertSourceUnchanged(Fixture f) throws Exception {
        assertTrue(f.sourceMekTree().equals(JSON.valueToTree(f.sourceMek())));
        assertTrue(f.sourceMainTree().equals(JSON.valueToTree(f.sourceMain())));
        assertTrue(f.sourceTamaTree().equals(JSON.valueToTree(f.sourceTama())));
        for (var entry : f.sourceFiles().entrySet())
            assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), "read-only binary: " + entry.getKey());
    }

    private byte[] writeWaz(Waz waz, String label) throws Exception {
        Path p = output.resolve(label + "-" + sequence++ + ".waz");
        new BsdxBinService().generate(p.toString(), waz, CHARSET);
        return Files.readAllBytes(p);
    }

    private static Path resolve(Path directory, String name) throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().equalsIgnoreCase(name))
                    .findFirst().orElseThrow(() -> new IllegalStateException("Missing binary fixture: " + directory.resolve(name)));
        }
    }

    private static void assertJapanese(String text) throws Exception {
        assertNotNull(text); assertFalse(text.isBlank());
        Charset charset = Charset.forName(CHARSET);
        assertEquals(text, charset.decode(charset.newEncoder().encode(CharBuffer.wrap(text))).toString());
    }

    private static <T> T copy(T value, Class<T> type) {
        try {
            return JSON.readValue(JSON.writeValueAsBytes(value), type);
        } catch (java.io.IOException error) {
            throw new IllegalStateException("test fixture deep copy failed", error);
        }
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        stripMetadata(tree);
        return tree;
    }

    private static void stripMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description"));
        node.forEach(SouHumanCannonCounterTest::stripMetadata);
    }

    private record Snapshot(int mainCount, int hostCount, JsonNode mek, JsonNode mainSkills,
                            JsonNode publicSkills, JsonNode plan, JsonNode otherPublic, JsonNode groups) { }

    private record Fixture(FollowupMekaContext context, TsukuyomiBsdxBaselineBundle baseline,
                           BheCommonProjectileAppendPlan plan, Mek mek, Waz main, Waz host,
                           String hostName, int hostGroup, int mainGroup, int publicControllerIndex, int publicCount,
                           com.giga.nexas.dto.bhe.mek.Mek sourceMek,
                           com.giga.nexas.dto.bhe.waz.Waz sourceMain,
                           com.giga.nexas.dto.bhe.waz.Waz sourceTama,
                           JsonNode sourceMekTree, JsonNode sourceMainTree, JsonNode sourceTamaTree,
                           Map<Path, byte[]> sourceFiles) {
        Fixture withResources(Mek newMek, Waz newMain, Waz newHost) {
            return new Fixture(context, baseline, plan, newMek, newMain, newHost, hostName, hostGroup, mainGroup,
                    publicControllerIndex, publicCount, sourceMek, sourceMain, sourceTama,
                    sourceMekTree, sourceMainTree, sourceTamaTree, sourceFiles);
        }
    }
}
