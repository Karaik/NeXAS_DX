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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Real source/public-role regression for Sou135; finite packet budgets are static, not runtime validation. */
@Execution(ExecutionMode.SAME_THREAD)
class SouSnowMoonSingleTargetTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String CHARSET = "windows-31j";
    private static final Path BHE = Path.of("src/main/resources/game/bhe");
    private static final Path DX = Path.of("src/main/resources/game/bsdx");
    private static final List<String> ENGLISH = List.of("SETUGEKKA01", "SETUGEKKA02", "SETUGEKKA03");
    private static final List<String> JAPANESE = List.of("雪月花Lv1", "雪月花Lv2", "雪月花Lv3");
    private static final List<Integer> FIELD_FRAME = List.of(34, 28, 22);
    private static final List<Integer> ORIGINAL_START = List.of(35, 29, 23);
    private static final List<Integer> INNER_END = List.of(120, 126, 132);
    private static final String PUBLIC_JP = "★蒼：雪月花凍結2";
    private static final String PUBLIC_EN = "0340";

    @TempDir Path output;
    private int sequence;

    @Test
    void eachLevelHasOnePreparationShotOutsideEveryEffectLoopAndOnePrivateContinuationShot() throws Exception {
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill child = privateController(f);
        for (int level = 0; level < 3; level++) {
            Waz.Skill main = family(f.main(), level);
            CEventEffect prep = effect(main, 0, 2), continuation = effect(main, 1, 0);
            assertEquals(FIELD_FRAME.get(level), prep.getStartFrame());
            assertEquals(FIELD_FRAME.get(level), prep.getEndFrame());
            assertEquals(effect(main, 0, 1).getStartFrame(), prep.getStartFrame(), "the shot shares the original field-creation frame");
            assertEquals(1, singleShotBudget(prep, unit(main, 0, 48).getSkillInfoUnknownList()));
            assertEquals(4, prep.getInt3(), "clear only MULTILOCK bit 0x20 and retain bit 0x4");
            assertEquals(1, singleShotBudget(continuation, unit(main, 1, 48).getSkillInfoUnknownList()));
            assertEquals(0, continuation.getInt3());
            assertEquals(List.of(f.hostGroup(), f.publicAttack()), reference(prep));
            assertEquals(List.of(f.hostGroup(), privateIndex(f)), reference(continuation));
            assertEquals(List.of(0, 0, 0, 0), four(inner(continuation, 36, CEventVal.class)),
                    "the original restored channel0 zero payload is retained");
            assertEquals(List.of(0, INNER_END.get(level)), frames(inner(prep, 4, CEventValRandom.class)),
                    "outer single-point adaptation does not rewrite the original inner frame window");
        }
        CEventEffect childShot = effect(child, 1, 2);
        assertEquals(1, singleShotBudget(childShot, unit(child, 1, 48).getSkillInfoUnknownList()));
        assertEquals(4, childShot.getInt3());
        assertEquals(List.of(f.hostGroup(), f.publicAttack()), reference(childShot));
        assertEquals(List.of(0, 10), frames(inner(childShot, 4, CEventValRandom.class)));
        assertSourceFilesUnchanged(f);
    }

    @Test
    void completeMainGraphChangesOnlyTheSixExactDangerousPackets() throws Exception {
        Fixture f = fixture();
        Waz expected = expectedMain(f, f.host().getSkillList().size());
        Map<String, String> preserved = preservedGraphs(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertPayloadEquals(expected, f.main(), "sou.waz complete graph minus only the six explicit packet deltas");
        assertEquals(preserved, preservedGraphs(f), "no source, selected-view, MEK/Material/AI, append-plan or unrelated host graph changes");
        assertSourceFilesUnchanged(f);
    }

    @Test
    void oneIndependentPrivateClonePreservesTheEntireExistingHostPrefixAndPublicPrototype() throws Exception {
        Fixture f = fixture();
        int beforeCount = f.host().getSkillList().size();
        JsonNode beforePrefix = payload(f.host().getSkillList());
        JsonNode beforeGroups = payload(f.context().getBaseline().getWazaGroupGrp());
        byte[] beforeBytes = writeWaz(f.host(), "host-prefix-before");
        Waz.Skill prototype = copy(publicController(f), Waz.Skill.class);
        Map<String, String> preserved = preservedGraphs(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill child = privateController(f);
        assertEquals(beforeCount + 1, f.host().getSkillList().size());
        assertEquals(beforeCount, privateIndex(f), "the child is appended at the actual final host tail");
        assertPayloadEquals(beforePrefix, f.host().getSkillList().subList(0, beforeCount), "complete native/BHE public and original private prefix");
        assertArrayEquals(beforeBytes, Arrays.copyOf(writeWaz(f.host(), "host-prefix-after"), beforeBytes.length));
        assertPayloadEquals(prototype, publicController(f), "public 515 prototype remains complete and unchanged");
        assertPayloadEquals(expectedChild(prototype, child), child, "private 515 copy differs only in identity and its third P1 packet");
        assertNotSame(publicController(f), child);
        assertNotSame(publicController(f).getPhasesInfo(), child.getPhasesInfo());
        for (int phase = 0; phase < 4; phase++) {
            assertNotSame(publicController(f).getPhasesInfo().get(phase), child.getPhasesInfo().get(phase));
            assertNotSame(publicController(f).getPhasesInfo().get(phase).getSkillUnitCollection(), child.getPhasesInfo().get(phase).getSkillUnitCollection());
        }
        assertEquals(4, child.getPhaseQuantity());
        assertEquals(List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0)), suffix(child));
        assertFalse(child.getSkillNameJapanese().isBlank());
        assertFalse(child.getSkillNameEnglish().isBlank());
        assertNotEquals(PUBLIC_JP, child.getSkillNameJapanese());
        assertNotEquals(PUBLIC_EN, child.getSkillNameEnglish());
        assertEquals(beforeGroups.path("wazaList").size(), f.context().getBaseline().getWazaGroupGrp().getWazaList().size(), "no new WazaGroup");
        for (int i = 0; i < beforeGroups.path("wazaList").size(); i++) {
            JsonNode actual = payload(f.context().getBaseline().getWazaGroupGrp().getWazaList().get(i));
            JsonNode expected = beforeGroups.path("wazaList").get(i).deepCopy();
            if (i == f.hostGroup()) ((ObjectNode) expected).put("param", beforeCount + 1);
            assertPayloadEquals(expected, actual, "WazaGroup entry " + i);
        }
        assertEquals(preserved, preservedGraphs(f));
        assertEquals(211, f.context().getReboundMek().getMekWeaponInfoMap().size());
        assertEquals(211, f.context().getReboundMek().getMekMaterialBlock().getTrailingEntries().size());
        assertSourceFilesUnchanged(f);
    }

    @Test
    void privateCloneUsesTheActualTailEvenAfterAnotherUnrelatedPrivateSkillWasAppended() throws Exception {
        Fixture f = fixture();
        Waz.Skill unrelated = copy(publicController(f), Waz.Skill.class);
        unrelated.setSkillNameJapanese("試験：無関係な私有弾");
        unrelated.setSkillNameEnglish("TEST_UNRELATED_PRIVATE_TAIL");
        f.host().getSkillList().add(unrelated);
        syncHostCount(f);
        int tail = f.host().getSkillList().size();
        JsonNode prefix = payload(f.host().getSkillList());
        Waz expected = expectedMain(f, tail);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(tail, privateIndex(f), "never assume current package index 540");
        assertEquals(tail + 1, f.host().getSkillList().size());
        assertSame(unrelated, f.host().getSkillList().get(tail - 1));
        assertPayloadEquals(prefix, f.host().getSkillList().subList(0, tail), "dynamic existing prefix");
        assertPayloadEquals(expected, f.main(), "all three callers use the dynamically resolved private index");
        assertSourceFilesUnchanged(f);
    }

    @Test
    void zeroScalarRetainsTheOriginalNatural0To1To3ExitAndTheCompleteUnusedPhase2() throws Exception {
        Fixture f = fixture();
        assertOriginalExitBytes(f);
        JsonNode originalPhase2 = payload(publicController(f).getPhasesInfo().get(2));
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill child = privateController(f);
        CEventChange enter = change(child, 0, 0), leave = change(child, 1, 1), finish = change(child, 3, 0);
        assertScalarExit(enter, 0, 0, 150, 1);
        assertScalarExit(leave, 20, 139, 0, 3);
        assertEquals(List.of(0, 0), finish.getBsdxInfoCollectionList2().get(0).getTypeList());
        assertEquals(List.of(), finish.getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(5, 5), frames(finish));
        assertEquals(List.of(0), finish.getBsdxInfoCollectionList1().get(0).getTypeList());
        assertTrue(0 <= enter.getBsdxInfoCollectionList1().get(0).getParamList().get(0));
        assertTrue(0 <= leave.getBsdxInfoCollectionList1().get(0).getParamList().get(0));
        assertEquals(List.of(1), enter.getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(3), leave.getBsdxInfoCollectionList2().get(0).getParamList());
        CEventVal zero = assertInstanceOf(CEventVal.class, unit(child, 1, 35).getSkillInfoObjectList().get(0));
        assertEquals(List.of(20, 20), frames(zero));
        assertEquals(List.of(0, 0, 0, 0), four(zero), "the existing channel0 maintenance remains zero instead of becoming a new timer");
        assertPayloadEquals(originalPhase2, child.getPhasesInfo().get(2), "unused P2 remains the original graph including every event and FF");
        assertSourceFilesUnchanged(f);
    }

    @Test
    void repeatPreservesTheSameMainHostPrivateSkillAndNestedObjectsWithoutAnotherAppend() throws Exception {
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill child = privateController(f);
        int installedIndex = privateIndex(f);
        Waz.Skill later = copy(publicController(f), Waz.Skill.class);
        later.setSkillNameJapanese("試験：後続の無関係な私有弾");
        later.setSkillNameEnglish("TEST_LATER_UNRELATED_PRIVATE_TAIL");
        f.host().getSkillList().add(later);
        syncHostCount(f);
        List<Waz.Skill> mainList = f.main().getSkillList(), hostList = f.host().getSkillList();
        List<Waz.Skill> mainSkills = new ArrayList<>(mainList), hostSkills = new ArrayList<>(hostList);
        Set<Object> identities = Collections.newSetFromMap(new IdentityHashMap<>());
        collectIdentities(child, identities);
        Snapshot before = snapshot(f);
        byte[] mainBytes = writeWaz(f.main(), "repeat-main-before"), hostBytes = writeWaz(f.host(), "repeat-host-before");
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertSame(mainList, f.main().getSkillList()); assertSame(hostList, f.host().getSkillList());
        for (int i = 0; i < mainSkills.size(); i++) assertSame(mainSkills.get(i), mainList.get(i));
        for (int i = 0; i < hostSkills.size(); i++) assertSame(hostSkills.get(i), hostList.get(i));
        assertSame(child, privateController(f));
        assertEquals(installedIndex, privateIndex(f), "an installed private child remains valid after another private tail is appended");
        assertSame(later, f.host().getSkillList().get(f.host().getSkillList().size() - 1));
        Set<Object> after = Collections.newSetFromMap(new IdentityHashMap<>());
        collectIdentities(child, after);
        for (Object identity : identities) assertTrue(after.contains(identity), "verified repeat retains every private object identity");
        assertEquals(before, snapshot(f));
        assertArrayEquals(mainBytes, writeWaz(f.main(), "repeat-main-after"));
        assertArrayEquals(hostBytes, writeWaz(f.host(), "repeat-host-after"));
        assertSourceFilesUnchanged(f);
    }

    @Test
    void originalSourceAndFinalTargetDriftRejectTheWholeBatchBeforeAnyGraphOrCountWrites() throws Exception {
        for (Consumer<Fixture> corrupt : List.<Consumer<Fixture>>of(
                f -> rawCount(rawEffect(rawFamily(f, 2), 0, 2)).setInt5(998),
                f -> rawEffect(rawFamily(f, 2), 0, 2).setInt3(4),
                f -> rawFamily(f, 2).getPhasesInfo().get(0).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(54).equals(u.getUnitQuantity())).findFirst().orElseThrow().getSkillInfoUnknownList().get(0).setStartFrame(115),
                f -> rawCount(rawEffect(rawController(f), 1, 2)).setInt2(998),
                f -> rawChange(rawController(f), 0, 0).getBheInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(149))),
                f -> rawFamily(f, 2).setSkillNameJapanese("別名：雪月花Lv3"),
                f -> inner(effect(family(f.main(), 2), 0, 2), 4, CEventValRandom.class).setInt5(998),
                f -> effect(family(f.main(), 2), 0, 2).setStartFrame(24),
                f -> effect(family(f.main(), 2), 1, 0).setInt3(0),
                f -> unit(family(f.main(), 2), 0, 48).getSkillInfoUnknownList().get(0).setStartFrame(115),
                f -> referenceObject(effect(family(f.main(), 2), 1, 0)).setWazSequenceNo(f.publicField()),
                f -> inner(effect(family(f.main(), 2), 1, 0), 36, CEventVal.class).setInt4(1),
                f -> inner(effect(publicController(f), 1, 2), 4, CEventValRandom.class).setInt2(998),
                f -> assertInstanceOf(CEventVal.class, unit(publicController(f), 1, 35).getSkillInfoObjectList().get(0)).setInt4(1),
                f -> change(publicController(f), 1, 1).getBsdxInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(1))),
                f -> {
                    BsdxInfoCollection condition = change(f.host().getSkillList().get(f.publicAttack()), 5, 0).getBsdxInfoCollectionList1().get(2);
                    condition.setTypeList(new ArrayList<>(List.of(1, 0))); condition.setParamList(new ArrayList<>(List.of(-1)));
                },
                f -> change(f.host().getSkillList().get(f.publicField()), 0, 0).getBsdxInfoCollectionList1().get(0).setIntList3(new ArrayList<>(List.of(1, 0))),
                f -> unit(family(f.main(), 2), 0, 48).setSkillInfoUnknownList(null),
                f -> family(f.main(), 2).setSkillNameEnglish("SETUGEKKA03_NEAR_MISS"))) {
            Fixture f = fixture();
            corrupt.accept(f);
            assertRejectedWithoutExtraWrites(f);
        }
    }

    @Test
    void halfInstalledPacketsDuplicateFamilyOrChangedInstalledChildCannotBeSilentlyRepaired() throws Exception {
        for (boolean duplicate : List.of(false, true)) {
            Fixture f = fixture();
            if (duplicate) {
                f.main().getSkillList().add(copy(family(f.main(), 2), Waz.Skill.class));
                syncMainCount(f);
            } else {
                CEventEffect packet = effect(family(f.main(), 2), 0, 2);
                packet.setStartFrame(22); packet.setEndFrame(22); packet.setInt3(4); setCountOne(packet);
            }
            assertRejectedWithoutExtraWrites(f);
        }
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill child = privateController(f);
        inner(effect(child, 1, 2), 4, CEventValRandom.class).setInt5(2);
        assertRejectedWithoutExtraWrites(f);
        assertSame(child, privateController(f));
    }

    @Test
    void completeMainAndHostSurviveTwoRealBinaryPassesAndParsedIdempotence() throws Exception {
        Fixture f = fixture();
        Waz expected = expectedMain(f, f.host().getSkillList().size());
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill child = privateController(f);
        assertPayloadEquals(expected, f.main(), "missing main repair fails before binary work");
        assertPayloadEquals(expectedChild(publicController(f), child), child, "complete independent child before binary work");
        BsdxBinService service = new BsdxBinService();
        Waz[] originals = {f.main(), f.host()};
        Waz[] parsed = new Waz[2];
        for (int index = 0; index < originals.length; index++) {
            Waz expectedReadback = expectedHeaderReadback(originals[index]);
            Path first = output.resolve("snow-" + index + "-pass1.waz"), second = output.resolve("snow-" + index + "-pass2.waz");
            service.generate(first.toString(), originals[index], CHARSET);
            Waz parsed1 = (Waz) service.parse(first.toString(), CHARSET).getData();
            assertPayloadEquals(expectedReadback, parsed1, "first full binary readback graph " + index);
            service.generate(second.toString(), parsed1, CHARSET);
            parsed[index] = (Waz) service.parse(second.toString(), CHARSET).getData();
            assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
            assertPayloadEquals(expectedReadback, parsed[index], "second full binary readback graph " + index);
        }
        f.context().setReboundWaz(parsed[0]);
        f.context().getBaseline().getWazByFileName().put("sou.waz", parsed[0]);
        String hostKey = f.context().getBaseline().getWazByFileName().keySet().stream().filter(f.hostName()::equalsIgnoreCase).findFirst().orElseThrow();
        f.context().getBaseline().getWazByFileName().put(hostKey, parsed[1]);
        List<Waz.Skill> mainList = f.main().getSkillList(), hostList = f.host().getSkillList();
        Waz.Skill parsedChild = privateController(f);
        Snapshot before = snapshot(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertSame(mainList, f.main().getSkillList()); assertSame(hostList, f.host().getSkillList());
        assertSame(parsedChild, privateController(f));
        assertEquals(before, snapshot(f));
        assertOriginalExitBytes(f);
        assertSourceFilesUnchanged(f);
    }

    private Fixture fixture() throws Exception {
        BheBinService bhe = new BheBinService();
        Path mekPath = BHE.resolve("mek/sou/sou.mek"), mainPath = BHE.resolve("waz/sou/sou.waz");
        Map<Path, byte[]> sourceFiles = new LinkedHashMap<>();
        for (Path p : List.of(mekPath, mainPath, resolve(BHE.resolve("waz"), "tama05.waz"), resolve(DX.resolve("waz"), "Tama05.waz")))
            sourceFiles.put(p, Files.readAllBytes(p));
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) bhe.parse(mekPath.toString(), CHARSET).getData();
        var rawMain = (com.giga.nexas.dto.bhe.waz.Waz) bhe.parse(mainPath.toString(), CHARSET).getData();
        TsukuyomiRawSourceBundle raw = loadCommonSource(bhe);
        raw.setTsukuyomiMek(rawMek); raw.getWazByFileName().put("sou.waz", rawMain);
        TsukuyomiBsdxBaselineBundle baseline = loadBaseline();
        TsukuyomiConvertedBundle prepared = new TsukuyomiConvertedBundle();
        new BheCommonProjectilePrepareStep().prepare(new TsukuyomiGraftRequest(), raw, baseline, prepared);
        var plan = prepared.getCommonProjectileAppendPlan();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek);
        Waz main = new BheToBsdxWazConverter().convert(rawMain);
        TsukuyomiPackageBundle selected = new TsukuyomiPackageBundle();
        selected.setTsukuyomiMek(mek); selected.getWazByFileName().put("sou.waz", copy(main, Waz.class));
        TsukuyomiGrpAppendPlan privatePlan = new TsukuyomiGrpAppendPlan();
        int mainGroup = baseline.getWazaGroupGrp().getWazaList().size();
        privatePlan.setWazaGroupIndex(mainGroup);
        privatePlan.getSourceWazGroupIndexToTargetIndex().put(rawMek.getMekBasicInfo().getWazFileSequence(), mainGroup);
        // The real Flare caller's role-private SE pair uses this nonidentity mapping; audio delivery is outside this protocol fixture.
        privatePlan.getSourceSeGroupIndexToTargetIndex().put(16, 46);
        privatePlan.getSourceSeItemIndexToTargetIndexByGroup().put(16, new LinkedHashMap<>(Map.of(4, 11)));
        rebindWazaSelects(main, new BheResourceIndexResolver(plan, privatePlan), Collections.newSetFromMap(new IdentityHashMap<>()));
        new BheInfoCollectionObjectGraphRewriter().rewrite(main, "SouSnowMoonSingleTargetTest main");
        mek.getMekBasicInfo().setWazFileSequence(mainGroup);
        WazaGroupGrp.WazaGroupEntry entry = new WazaGroupGrp.WazaGroupEntry();
        entry.setExistFlag(1); entry.setWazaName("sou"); entry.setWazaCodeName("SOU"); entry.setWazaDisplayName("蒼");
        entry.setParam(main.getSkillList().size()); baseline.getWazaGroupGrp().getWazaList().add(entry);
        baseline.getWazByFileName().put("sou.waz", main);
        privatePlan.getTargetWazSkillCountByGroupIndex().put(mainGroup, main.getSkillList().size());
        FollowupGraftRequest request = new FollowupGraftRequest();
        request.setMekaCodeName("SOU"); request.setWazCodeName("SOU"); request.setWazFileName("sou.waz");
        request.setMekFileName("Sou.mek"); request.setSpriteFileName("sou.spm");
        TsukuyomiConvertedBundle currentSou = new TsukuyomiConvertedBundle();
        currentSou.setRawSourceBundle(raw); currentSou.setCommonProjectileAppendPlan(plan);
        currentSou.setPreparedBaselineBundle(baseline); currentSou.setSelectedResourceBundle(selected);
        FollowupMekaContext context = new FollowupMekaContext();
        context.setRequest(request); context.setBaseline(baseline); context.setConvertedBundle(currentSou);
        context.setSelectedPackage(selected); context.setGrpAppendPlan(privatePlan); context.setReboundMek(mek); context.setReboundWaz(main);
        // Prime the other role repairs; Snow Moon must first run through the public entry in each test.
        SouSkillGlitchFixer.INSTANCE.fix(context);
        SouHumanCannonCounterFixer.fix(context);
        SouCursedBladeSingleTargetFixer.fix(context);
        SouFlareSeekerSingleTargetFixer.fix(context);
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        assertTrue(sourceGroup >= 0);
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        int hostGroup = plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        int base = plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
        var rawTama = raw.getCommonProjectileWazByFileName().get("tama05.waz");
        int attack = sourceIndex(rawTama, "★蒼：雪月花凍結", "0338");
        int controller = sourceIndex(rawTama, PUBLIC_JP, PUBLIC_EN);
        int field = sourceIndex(rawTama, "★蒼：雪月花フィールド", "0339");
        Fixture f = new Fixture(context, raw, mainGroup, hostName, hostGroup, base + attack, base + controller, base + field, controller, sourceFiles);
        assertEquals(PUBLIC_EN, publicController(f).getSkillNameEnglish());
        assertEquals(4, publicController(f).getPhaseQuantity());
        for (int level = 0; level < 3; level++) {
            Waz.Skill skill = family(main, level);
            assertEquals(3, skill.getPhaseQuantity());
            assertEquals(List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0)), suffix(skill));
            assertEquals(10, unit(skill, 0, 48).getSkillInfoObjectList().size());
            assertEquals(1, unit(skill, 1, 48).getSkillInfoObjectList().size());
            assertEquals(level == 0 ? List.of(List.of(116, 155, -1, 2, 2), List.of(35, 155, -1, 1, -2))
                            : List.of(List.of(116, 155, -1, 1, -2)),
                    unit(skill, 0, 48).getSkillInfoUnknownList().stream()
                            .map(loop -> List.of(loop.getStartFrame(), loop.getEndFrame(), loop.getInt1(), loop.getInt2(), loop.getInt3())).toList(),
                    "complete original Effect FF signatures");
            CEventEffect prep = effect(skill, 0, 2), continuation = effect(skill, 1, 0);
            assertEquals(List.of(ORIGINAL_START.get(level), 155), frames(prep));
            assertEquals(List.of(1, 0, 36, 100, -1, -1, -1, 0, 1, 0, 0), eleven(prep));
            assertEquals(List.of(0, 999, 0, 0, 999, 0), six(inner(prep, 4, CEventValRandom.class)));
            assertEquals(List.of(0, INNER_END.get(level)), frames(inner(prep, 4, CEventValRandom.class)));
            assertEquals(List.of(3, 3), frames(continuation));
            assertEquals(List.of(1, 0, 32, 0, -1, -1, -1, 0, 1, 0, 0), eleven(continuation));
            assertEquals(List.of(0, 1, 0, 0, 999, 0), six(inner(continuation, 4, CEventValRandom.class)));
            assertEquals(List.of(hostGroup, f.publicAttack()), reference(prep));
            assertEquals(List.of(hostGroup, f.publicController()), reference(continuation));
            assertEquals(FIELD_FRAME.get(level), effect(skill, 0, 1).getStartFrame());
        }
        assertEquals(List.of(0, 10), frames(effect(publicController(f), 1, 2)));
        assertEquals(List.of(0, 999, 0, 0, 999, 0), six(inner(effect(publicController(f), 1, 2), 4, CEventValRandom.class)));
        assertOriginalExitBytes(f);
        assertSourceFilesUnchanged(f);
        return f;
    }

    private static Waz expectedMain(Fixture f, int privateIndex) {
        Waz expected = copy(f.main(), Waz.class);
        for (int level = 0; level < 3; level++) {
            CEventEffect prep = effect(family(expected, level), 0, 2);
            prep.setStartFrame(FIELD_FRAME.get(level)); prep.setEndFrame(FIELD_FRAME.get(level)); prep.setInt3(4); setCountOne(prep);
            CEventEffect continuation = effect(family(expected, level), 1, 0);
            continuation.setInt3(0); setCountOne(continuation);
            referenceObject(continuation).setWazSequenceNo(privateIndex);
        }
        return expected;
    }

    private static Waz.Skill expectedChild(Waz.Skill prototype, Waz.Skill child) {
        Waz.Skill expected = copy(prototype, Waz.Skill.class);
        expected.setSkillNameJapanese(child.getSkillNameJapanese()); expected.setSkillNameEnglish(child.getSkillNameEnglish());
        CEventEffect packet = effect(expected, 1, 2);
        packet.setStartFrame(0); packet.setEndFrame(0); packet.setInt3(4); setCountOne(packet);
        return expected;
    }

    private static Waz expectedHeaderReadback(Waz original) {
        Waz expected = copy(original, Waz.class);
        for (Waz.Skill skill : expected.getSkillList()) {
            // The already-audited flag1 zero-phase shell headers write null names as one NUL; no active field is normalized.
            if (Integer.valueOf(0).equals(skill.getPhaseQuantity()) && skill.getPhasesInfo().isEmpty() && skill.getSkillSuffixList().isEmpty()
                    && !skill.isEmpty() && (skill.getSkillNameJapanese() == null || skill.getSkillNameJapanese().isEmpty())
                    && (skill.getSkillNameEnglish() == null || skill.getSkillNameEnglish().isEmpty())) {
                if (skill.getSkillNameJapanese() == null) skill.setSkillNameJapanese("");
                if (skill.getSkillNameEnglish() == null) skill.setSkillNameEnglish("");
            }
        }
        return expected;
    }

    private static int singleShotBudget(CEventEffect packet, List<SkillInfoUnknown> loops) {
        assertEquals(packet.getStartFrame(), packet.getEndFrame(), "a one-point packet cannot multiply over its original 120/126/132-frame span");
        assertEquals(0, packet.getInt3() & 0x20);
        assertEquals(List.of(0, 1, 0, 0, 1, 0), six(inner(packet, 4, CEventValRandom.class)));
        for (SkillInfoUnknown loop : loops)
            assertTrue(packet.getStartFrame() < loop.getStartFrame() || packet.getStartFrame() > loop.getEndFrame(),
                    "a preserved FF window must not cover the new point: " + packet.getStartFrame() + " in " + loop.getStartFrame() + ".." + loop.getEndFrame());
        return inner(packet, 4, CEventValRandom.class).getInt2();
    }

    private static void setCountOne(CEventEffect packet) {
        CEventValRandom count = inner(packet, 4, CEventValRandom.class);
        count.setInt2(1); count.setInt5(1);
    }

    private static int privateIndex(Fixture f) {
        Set<Integer> indices = new HashSet<>();
        for (int level = 0; level < 3; level++) {
            CEventWazaSelect ref = referenceObject(effect(family(f.main(), level), 1, 0));
            assertEquals(f.hostGroup(), ref.getWazFileNo());
            indices.add(ref.getWazSequenceNo());
        }
        assertEquals(1, indices.size(), "all three levels share exactly one independent private controller");
        int index = indices.iterator().next();
        assertTrue(index > f.publicController(), "main P1 must leave the public 515 prototype and select an appended private child");
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        var plan = f.context().getConvertedBundle().getCommonProjectileAppendPlan();
        assertTrue(index >= plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup) + plan.getSourceWazIndexToTargetSkillCount().get(sourceGroup),
                "private append must remain outside the unchanged inherited BHE source slice");
        assertTrue(index >= 0 && index < f.host().getSkillList().size());
        return index;
    }

    private static Waz.Skill privateController(Fixture f) { return f.host().getSkillList().get(privateIndex(f)); }
    private static Waz.Skill publicController(Fixture f) { return f.host().getSkillList().get(f.publicController()); }

    private static Waz.Skill family(Waz main, int level) {
        var matches = main.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> JAPANESE.get(level).equals(s.getSkillNameJapanese()) && ENGLISH.get(level).equals(s.getSkillNameEnglish())).toList();
        assertEquals(1, matches.size(), "unique paired Snow Moon identity " + ENGLISH.get(level));
        return matches.get(0);
    }

    private static SkillUnit unit(Waz.Skill skill, int phase, int number) {
        var matches = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(number).equals(u.getUnitQuantity())).toList();
        assertEquals(1, matches.size()); return matches.get(0);
    }

    private static CEventEffect effect(Waz.Skill skill, int phase, int ordinal) {
        return assertInstanceOf(CEventEffect.class, unit(skill, phase, 48).getSkillInfoObjectList().get(ordinal));
    }

    private static CEventChange change(Waz.Skill skill, int phase, int ordinal) {
        return assertInstanceOf(CEventChange.class, unit(skill, phase, 71).getSkillInfoObjectList().get(ordinal));
    }

    private static <T> T inner(CEventEffect packet, int slot, Class<T> type) {
        var matches = packet.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum())).toList();
        assertEquals(1, matches.size()); assertEquals(1, matches.get(0).getBuffer());
        return assertInstanceOf(type, matches.get(0).getData());
    }

    private static CEventWazaSelect referenceObject(CEventEffect packet) { return inner(packet, 3, CEventWazaSelect.class); }
    private static List<Integer> reference(CEventEffect packet) { var ref = referenceObject(packet); return List.of(ref.getWazFileNo(), ref.getWazSequenceNo()); }
    private static List<Integer> frames(SkillInfoObject event) { return List.of(event.getStartFrame(), event.getEndFrame()); }
    private static List<Integer> four(CEventVal value) { return List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4()); }
    private static List<Integer> six(CEventValRandom value) { return List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6()); }
    private static List<Integer> eleven(CEventEffect v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6(), v.getInt7(), v.getInt8(), v.getInt9(), v.getInt10(), v.getInt11()); }
    private static List<List<Integer>> suffix(Waz.Skill skill) { return skill.getSkillSuffixList().stream().map(s -> List.of(s.getInt1(), s.getInt2())).toList(); }

    private static int sourceIndex(com.giga.nexas.dto.bhe.waz.Waz main, String japanese, String english) {
        var matches = IntStream.range(0, main.getSkillList().size()).filter(i -> {
            var skill = main.getSkillList().get(i); return skill != null && japanese.equals(skill.getSkillNameJapanese()) && english.equals(skill.getSkillNameEnglish());
        }).boxed().toList();
        assertEquals(1, matches.size()); return matches.get(0);
    }

    private static com.giga.nexas.dto.bhe.waz.Waz.Skill rawFamily(Fixture f, int level) {
        var raw = f.raw().getWazByFileName().get("sou.waz");
        return raw.getSkillList().get(sourceIndex(raw, JAPANESE.get(level), ENGLISH.get(level)));
    }

    private static com.giga.nexas.dto.bhe.waz.Waz.Skill rawController(Fixture f) {
        return f.raw().getCommonProjectileWazByFileName().get("tama05.waz").getSkillList().get(f.sourceController());
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect rawEffect(com.giga.nexas.dto.bhe.waz.Waz.Skill skill, int phase, int ordinal) {
        var slot = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(54).equals(u.getUnitQuantity())).findFirst().orElseThrow();
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect) slot.getSkillInfoObjectList().get(ordinal);
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventValRandom rawCount(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect packet) {
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventValRandom) packet.getCeventEffectUnitList().stream()
                .filter(u -> Integer.valueOf(4).equals(u.getUnitSlotNum())).findFirst().orElseThrow().getData();
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange rawChange(com.giga.nexas.dto.bhe.waz.Waz.Skill skill, int phase, int ordinal) {
        var slot = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(82).equals(u.getUnitQuantity())).findFirst().orElseThrow();
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange) slot.getSkillInfoObjectList().get(ordinal);
    }

    private static void assertScalarExit(CEventChange event, int start, int end, int threshold, int destination) {
        assertEquals(List.of(start, end), frames(event)); assertEquals(1, event.getFlag()); assertEquals(0, event.getInt1());
        BsdxInfoCollection condition = event.getBsdxInfoCollectionList1().get(0), target = event.getBsdxInfoCollectionList2().get(0);
        assertEquals(0, condition.getInt1()); assertEquals(List.of(2, 0, 0, 2), condition.getTypeList());
        assertEquals(List.of(threshold), condition.getParamList()); assertEquals(List.of(0, 0), condition.getIntList3());
        assertEquals(List.of(), condition.getIntList4()); assertEquals(0, condition.getInt2());
        assertEquals(16, target.getInt1()); assertEquals(List.of(0, 1), target.getTypeList());
        assertEquals(List.of(destination), target.getParamList()); assertEquals(List.of(), target.getIntList3());
        assertEquals(List.of(), target.getIntList4()); assertEquals(0, target.getInt2());
    }

    private static void assertOriginalExitBytes(Fixture f) {
        byte[] source = f.sourceFiles().entrySet().stream().filter(e -> e.getKey().startsWith(BHE) && e.getKey().getFileName().toString().equalsIgnoreCase("tama05.waz"))
                .findFirst().orElseThrow().getValue();
        var controller = rawController(f);
        assertSourcePacket(source, rawChange(controller, 0, 0).getOffset(), encodedExit(0, 0, List.of(2, 0, 1, 2), List.of(150), List.of(0, 0), List.of(0, 1), List.of(1)));
        assertSourcePacket(source, rawChange(controller, 1, 1).getOffset(), encodedExit(20, 139, List.of(2, 0, 1, 2), List.of(0), List.of(0, 0), List.of(0, 1), List.of(3)));
        assertSourcePacket(source, rawChange(controller, 3, 0).getOffset(), encodedExit(5, 5, List.of(0), List.of(), List.of(), List.of(0, 0), List.of()));
    }

    private static void assertSourcePacket(byte[] source, int offset, byte[] literal) {
        assertArrayEquals(literal, Arrays.copyOfRange(source, offset, offset + literal.length), "complete original zero-route Change packet at byte " + offset);
    }

    private static byte[] encodedExit(int start, int end, List<Integer> types, List<Integer> params, List<Integer> objects, List<Integer> targetTypes, List<Integer> targetParams) {
        List<Integer> words = new ArrayList<>(List.of(start, end, 1));
        appendCollection(words, 0, types, params, objects);
        appendCollection(words, 16, targetTypes, targetParams, List.of());
        words.add(0);
        ByteBuffer bytes = ByteBuffer.allocate(words.size() * 4).order(ByteOrder.LITTLE_ENDIAN);
        words.forEach(bytes::putInt);
        return bytes.array();
    }

    private static void appendCollection(List<Integer> words, int root, List<Integer> types, List<Integer> params, List<Integer> objects) {
        words.add(root); words.add(types.size()); words.addAll(types); words.add(params.size()); words.addAll(params);
        words.add(objects.size()); words.addAll(objects); words.add(0); words.add(0);
    }

    private static void syncHostCount(Fixture f) {
        f.context().getBaseline().getWazaGroupGrp().getWazaList().get(f.hostGroup()).setParam(f.host().getSkillList().size());
    }

    private static void syncMainCount(Fixture f) {
        f.context().getBaseline().getWazaGroupGrp().getWazaList().get(f.mainGroup()).setParam(f.main().getSkillList().size());
        f.context().getGrpAppendPlan().getTargetWazSkillCountByGroupIndex().put(f.mainGroup(), f.main().getSkillList().size());
    }

    private static Map<String, String> preservedGraphs(Fixture f) throws Exception {
        Map<String, String> result = new LinkedHashMap<>();
        result.put("source", digest(f.raw())); result.put("mek", digest(f.context().getReboundMek()));
        result.put("selected", digest(f.context().getSelectedPackage()));
        result.put("common-plan", digest(f.context().getConvertedBundle().getCommonProjectileAppendPlan()));
        result.put("private-plan", digest(f.context().getGrpAppendPlan()));
        for (var entry : f.context().getBaseline().getWazByFileName().entrySet())
            if (!"sou.waz".equalsIgnoreCase(entry.getKey()) && !f.hostName().equalsIgnoreCase(entry.getKey())) result.put("public:" + entry.getKey(), digest(entry.getValue()));
        return result;
    }

    private static Snapshot snapshot(Fixture f) throws Exception {
        return new Snapshot(digest(f.main()), digest(f.host()), digest(f.context().getBaseline().getWazaGroupGrp()), preservedGraphs(f));
    }

    private static void assertRejectedWithoutExtraWrites(Fixture f) throws Exception {
        Snapshot before = snapshot(f);
        List<Waz.Skill> main = f.main().getSkillList(), host = f.host().getSkillList();
        assertThrows(IllegalStateException.class, () -> SouCustomizer.INSTANCE.customizeBeforeWrite(f.context()), "drift is rejected without appending or publishing any partial family");
        assertEquals(before, snapshot(f)); assertSame(main, f.main().getSkillList()); assertSame(host, f.host().getSkillList());
        assertSourceFilesUnchanged(f);
    }

    private static void assertSourceFilesUnchanged(Fixture f) throws Exception {
        for (var entry : f.sourceFiles().entrySet()) assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), "read-only original " + entry.getKey());
    }

    private byte[] writeWaz(Waz waz, String label) throws Exception {
        Path path = output.resolve(label + "-" + sequence++ + ".waz");
        new BsdxBinService().generate(path.toString(), waz, CHARSET);
        return Files.readAllBytes(path);
    }

    private static TsukuyomiRawSourceBundle loadCommonSource(BheBinService service) throws Exception {
        TsukuyomiRawSourceBundle raw = new TsukuyomiRawSourceBundle();
        raw.setWazaGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp) service.parse(resolve(BHE.resolve("grp"), "WazaGroup.grp").toString(), CHARSET).getData());
        raw.setSpriteGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp) service.parse(resolve(BHE.resolve("grp"), "SpriteGroup.grp").toString(), CHARSET).getData());
        raw.setSeGroupGrp((com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp) service.parse(resolve(BHE.resolve("grp"), "SeGroup.grp").toString(), CHARSET).getData());
        for (String name : BheCommonProjectileResources.commonProjectileWazFiles())
            raw.getCommonProjectileWazByFileName().put(name, (com.giga.nexas.dto.bhe.waz.Waz) service.parse(resolve(BHE.resolve("waz"), name).toString(), CHARSET).getData());
        for (String name : BheCommonProjectileResources.commonProjectileSpmFiles())
            raw.getCommonProjectileSpmByFileName().put(name, (com.giga.nexas.dto.bhe.spm.Spm) service.parse(resolve(BHE.resolve("spm"), name).toString(), CHARSET).getData());
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
            int group = ref.getWazFileNo();
            ref.setWazSequenceNo(resolver.resolveWazSkillIndex(group, ref.getWazSequenceNo()));
            ref.setWazFileNo(resolver.resolveWazGroupIndex(group));
            return;
        }
        if (node instanceof Collection<?> values) { for (Object value : values) rebindWazaSelects(value, resolver, seen); return; }
        if (node instanceof Map<?, ?> values) { for (Object value : values.values()) rebindWazaSelects(value, resolver, seen); return; }
        if (!node.getClass().getName().startsWith("com.giga.nexas.dto.")) return;
        for (Class<?> type = node.getClass(); type != null; type = type.getSuperclass()) for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            field.setAccessible(true); rebindWazaSelects(field.get(node), resolver, seen);
        }
    }

    private static void collectIdentities(Object node, Set<Object> seen) throws Exception {
        if (node == null || node instanceof Number || node instanceof String || node instanceof Boolean || !seen.add(node)) return;
        if (node instanceof Collection<?> values) { for (Object value : values) collectIdentities(value, seen); return; }
        if (!node.getClass().getName().startsWith("com.giga.nexas.dto.")) return;
        for (Class<?> type = node.getClass(); type != null; type = type.getSuperclass()) for (Field field : type.getDeclaredFields()) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
            field.setAccessible(true); collectIdentities(field.get(node), seen);
        }
    }

    private static Path resolve(Path directory, String name) throws Exception {
        try (var files = Files.list(directory)) {
            return files.filter(Files::isRegularFile).filter(p -> name.equalsIgnoreCase(p.getFileName().toString())).findFirst().orElseThrow();
        }
    }

    private static <T> T copy(T value, Class<T> type) {
        try { return JSON.readValue(JSON.writeValueAsBytes(value), type); }
        catch (java.io.IOException error) { throw new IllegalStateException("independent test graph copy failed", error); }
    }

    private static String digest(Object value) throws Exception {
        // Preserved views must retain even source parser metadata; only expected-vs-readback WAZ comparisons normalize annotations.
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(value)));
    }

    private static void assertPayloadEquals(Object expected, Object actual, String label) throws Exception {
        JsonNode expectedGraph = payload(expected), actualGraph = payload(actual);
        // Keep exact complete-graph equality, but never attach the full WAZ as JUnit expected/actual diagnostic values.
        if (!expectedGraph.equals(actualGraph)) {
            List<String> differences = new ArrayList<>();
            collectDifferences(expectedGraph, actualGraph, "$", differences);
            Path directory = Path.of("target/sou135-object-explosion-20261030/graph-diff");
            Files.createDirectories(directory);
            String stem = label.replaceAll("[^A-Za-z0-9]+", "-") + "-" + System.currentTimeMillis();
            Path expectedFile = directory.resolve(stem + ".expected.json");
            Path actualFile = directory.resolve(stem + ".actual.json");
            Path differenceFile = directory.resolve(stem + ".diff.txt");
            String summary = label + "; expected SHA-256=" + digest(expectedGraph) + "; actual SHA-256=" + digest(actualGraph)
                    + "\nfirst differences (at most 10):\n" + String.join("\n", differences);
            // The deliberate missing-feature RED needs only bounded diagnostics. Save full graphs
            // for an unexpected binary readback mismatch, where independent serialization audit needs them.
            if (label.contains("binary readback")) {
                writeDiagnostic(expectedFile, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(expectedGraph));
                writeDiagnostic(actualFile, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(actualGraph));
                summary += "\nexpected graph: " + expectedFile + "\nactual graph: " + actualFile;
            }
            writeDiagnostic(differenceFile, summary);
            fail(summary + "\ndifference report: " + differenceFile);
        }
    }

    private static void collectDifferences(JsonNode expected, JsonNode actual, String path, List<String> differences) {
        if (differences.size() >= 10 || Objects.equals(expected, actual)) return;
        if (expected == null || actual == null || expected.getNodeType() != actual.getNodeType()) {
            differences.add(path + ": expected " + shortNode(expected) + ", actual " + shortNode(actual));
            return;
        }
        if (expected.isObject()) {
            Set<String> fields = new TreeSet<>();
            expected.fieldNames().forEachRemaining(fields::add);
            actual.fieldNames().forEachRemaining(fields::add);
            for (String field : fields) {
                collectDifferences(expected.get(field), actual.get(field), path + "." + field, differences);
                if (differences.size() >= 10) return;
            }
        } else if (expected.isArray()) {
            if (expected.size() != actual.size())
                differences.add(path + ".length: expected " + expected.size() + ", actual " + actual.size());
            for (int i = 0; i < Math.min(expected.size(), actual.size()) && differences.size() < 10; i++)
                collectDifferences(expected.get(i), actual.get(i), path + "[" + i + "]", differences);
        } else {
            differences.add(path + ": expected " + shortNode(expected) + ", actual " + shortNode(actual));
        }
    }

    private static String shortNode(JsonNode node) {
        if (node == null) return "<missing>";
        if (node.isObject()) return "<object fields=" + node.size() + ">";
        if (node.isArray()) return "<array size=" + node.size() + ">";
        String value = node.toString();
        return value.length() <= 120 ? value : value.substring(0, 117) + "...";
    }

    private static void writeDiagnostic(Path path, String text) throws Exception {
        Files.writeString(path, text.replace("\r\n", "\n").replace("\r", "\n").stripTrailing().replace("\n", "\r\n") + "\r\n");
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        stripMetadata(tree);
        return tree;
    }

    private static void stripMetadata(JsonNode node) {
        // File labels and parser annotations are not serialized WAZ behavior.
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description", "fileName", "extensionName"));
        node.forEach(SouSnowMoonSingleTargetTest::stripMetadata);
    }

    private record Snapshot(String main, String host, String groups, Map<String, String> preserved) { }

    private record Fixture(FollowupMekaContext context, TsukuyomiRawSourceBundle raw, int mainGroup,
                           String hostName, int hostGroup, int publicAttack, int publicController, int publicField,
                           int sourceController, Map<Path, byte[]> sourceFiles) {
        Waz main() { return context.getReboundWaz(); }
        Waz host() { return context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> hostName.equalsIgnoreCase(e.getKey())).findFirst().orElseThrow().getValue(); }
    }
}
