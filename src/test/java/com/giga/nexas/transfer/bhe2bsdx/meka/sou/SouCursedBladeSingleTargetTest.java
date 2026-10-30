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
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.*;

/** Real source/conversion/public-role regression; control-graph checks make no runtime claim. */
@Execution(ExecutionMode.SAME_THREAD)
class SouCursedBladeSingleTargetTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String CHARSET = "windows-31j";
    private static final Path BHE = Path.of("src/main/resources/game/bhe");
    private static final Path DX = Path.of("src/main/resources/game/bsdx");
    private static final List<String> ENGLISH = List.of("JUSOUTOU01", "JUSOUTOU02", "JUSOUTOU03");
    private static final List<String> JAPANESE = List.of("呪装刀Lv1", "呪装刀Lv2", "呪装刀Lv3");
    private static final List<Integer> REENTRY = List.of(11, 9, 7);
    private static final List<Integer> ENTRY = List.of(41, 44, 50);
    private static final List<Integer> RELEASE = List.of(78, 86, 90);
    private static final List<Integer> PREP_END = List.of(74, 77, 83);
    private static final List<List<Integer>> SUFFIX = List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0));

    @TempDir Path output;
    private int sequence;

    @Test
    void publicCustomizerRemovesOnlyTheThreeExactReentriesFromTheCompleteMainGraph() throws Exception {
        Fixture f = fixture();
        Waz expected = expectedSinglePass(f.main());
        Map<String, String> preserved = preservedGraphs(f);

        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());

        assertPayloadEquals(expected, f.main(),
                "all levels, every phase, event, unknown loop, suffix and unrelated skill must equal the original graph minus exactly one P1 event per level");
        assertEquals(preserved, preservedGraphs(f), "the Sou63 repair has no MEK, source, selected-view, public-host, GRP or plan side effects");
        assertSourceFilesUnchanged(f);
    }

    @Test
    void eachLevelKeepsTheOriginalEntryAndThe46Then49RecoveryPath() throws Exception {
        Fixture f = fixture();
        JsonNode[] entries = IntStream.range(0, 3).mapToObj(i -> payload(family(f.main(), i).getPhasesInfo().get(0)))
                .toArray(JsonNode[]::new);

        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());

        for (int i = 0; i < 3; i++) {
            Waz.Skill skill = family(f.main(), i);
            assertPayloadEquals(entries[i], skill.getPhasesInfo().get(0), "P0 keeps its complete original timing, release condition and ordinary whiff behavior");
            assertEquals(SUFFIX, suffix(skill));
            assertEquals(3, skill.getPhaseQuantity());
            assertEquals(3, changes(skill, 0).size());
            assertChange(changes(skill, 0).get(0), RELEASE.get(i), List.of(2, 4, 3), List.of(), List.of(), List.of(0, 0), List.of());
            assertChange(changes(skill, 0).get(1), ENTRY.get(i), List.of(0), List.of(), List.of(0, 0), List.of(0, 0), List.of());
            assertChange(changes(skill, 0).get(2), PREP_END.get(i), List.of(0), List.of(), List.of(), List.of(0, 2), List.of());
            assertEquals(1, changes(skill, 1).size(), "P1 must not re-enter the same suffix while a button remains held");
            assertChange(changes(skill, 1).get(0), 46, List.of(0), List.of(), List.of(), List.of(0, 0), List.of());
            assertChange(changes(skill, 2).get(0), 49, List.of(0), List.of(), List.of(), List.of(0, 0), List.of());
            assertEquals(List.of(List.of(1, 46, 2), List.of(2, 49, -1)), recoveryEdges(skill),
                    "the existing NEXT resolves P1 to P2, then leaves the final suffix; no new END, budget, target gate or timeout is introduced");
        }
        assertSourceFilesUnchanged(f);
    }

    @Test
    void completeBinaryGraphSurvivesTwoGenerateParsePassesAndAParsedRepeat() throws Exception {
        Fixture f = fixture();
        Waz expected = expectedSinglePass(f.main());
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertPayloadEquals(expected, f.main(), "missing adaptation must fail before binary work");
        Waz expectedReadback = copy(expected, Waz.class);
        int shellHeaders = 0;
        for (Waz.Skill skill : expectedReadback.getSkillList()) {
            // These are flag1 zero-phase shell headers, not flag0 disabled entries. The writer emits
            // one NUL for a null name, and the parser reads that exact header name back as "".
            if (Integer.valueOf(0).equals(skill.getPhaseQuantity()) && skill.getPhasesInfo().isEmpty()
                    && skill.getSkillSuffixList().isEmpty() && !skill.isEmpty()
                    && (skill.getSkillNameJapanese() == null || skill.getSkillNameJapanese().isEmpty())
                    && (skill.getSkillNameEnglish() == null || skill.getSkillNameEnglish().isEmpty())) {
                if (skill.getSkillNameJapanese() == null) skill.setSkillNameJapanese("");
                if (skill.getSkillNameEnglish() == null) skill.setSkillNameEnglish("");
                shellHeaders++;
            }
        }
        assertEquals(209, shellHeaders, "the real source fixture has exactly the independently audited zero-phase shell headers");
        BsdxBinService service = new BsdxBinService();
        Path first = output.resolve("sou-single-target-pass1.waz");
        Path second = output.resolve("sou-single-target-pass2.waz");
        service.generate(first.toString(), f.main(), CHARSET);
        Waz parsed1 = (Waz) service.parse(first.toString(), CHARSET).getData();
        assertPayloadEquals(expectedReadback, parsed1, "first real binary readback preserves the complete expected graph");
        service.generate(second.toString(), parsed1, CHARSET);
        Waz parsed2 = (Waz) service.parse(second.toString(), CHARSET).getData();
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second), "second complete generation is byte-identical");
        assertPayloadEquals(expectedReadback, parsed2, "second real binary readback preserves every retained event and suffix");
        f.context().setReboundWaz(parsed2);
        f.context().getBaseline().getWazByFileName().put("sou.waz", parsed2);
        List<Waz.Skill> sameList = parsed2.getSkillList();
        List<Waz.Skill> sameSkills = new ArrayList<>(sameList);
        Map<String, String> preserved = preservedGraphs(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertSame(sameList, parsed2.getSkillList(), "parsed installed graphs are recognized without a DTO marker");
        for (int i = 0; i < sameSkills.size(); i++) assertSame(sameSkills.get(i), parsed2.getSkillList().get(i));
        assertPayloadEquals(expectedReadback, parsed2, "parsed-repeat sou.waz complete graph");
        assertEquals(preserved, preservedGraphs(f));
        assertArrayEquals(Files.readAllBytes(second), writeWaz(parsed2, "parsed-repeat"));
        assertSourceFilesUnchanged(f);
    }

    @Test
    void verifiedSecondApplicationKeepsTheSameListsSkillsPhasesAndEvents() throws Exception {
        Fixture f = fixture();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        for (int i = 0; i < 3; i++) assertEquals(1, changes(family(f.main(), i), 1).size(), "first application must be real");
        List<Waz.Skill> skills = f.main().getSkillList();
        List<Waz.Skill> identities = new ArrayList<>(skills);
        List<Object> nested = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Waz.Skill skill = family(f.main(), i);
            nested.add(skill.getPhasesInfo()); nested.add(skill.getSkillSuffixList());
            for (var phase : skill.getPhasesInfo()) {
                nested.add(phase); nested.add(phase.getSkillUnitCollection());
                for (var unit : phase.getSkillUnitCollection()) {
                    nested.add(unit); nested.add(unit.getSkillInfoObjectList()); nested.addAll(unit.getSkillInfoObjectList());
                    nested.add(unit.getSkillInfoUnknownList()); nested.addAll(unit.getSkillInfoUnknownList());
                }
            }
        }
        Snapshot before = snapshot(f);
        byte[] binary = writeWaz(f.main(), "installed");

        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());

        assertSame(skills, f.main().getSkillList());
        for (int i = 0; i < identities.size(); i++) assertSame(identities.get(i), skills.get(i));
        Set<Object> stillPresent = Collections.newSetFromMap(new IdentityHashMap<>());
        collectIdentities(f.main(), stillPresent);
        for (Object value : nested) assertTrue(stillPresent.contains(value), "verified repeat must retain each nested object identity");
        assertEquals(before, snapshot(f));
        assertArrayEquals(binary, writeWaz(f.main(), "installed-repeat"));
        assertSourceFilesUnchanged(f);
    }

    @Test
    void originalMultilockSignatureDriftRejectsTheWholeBatchWithoutAnyAdditionalWrites() throws Exception {
        for (Consumer<Fixture> corrupt : List.<Consumer<Fixture>>of(
                f -> rawChange(f, 2, 1, 0).getBheInfoCollectionList1().get(0).setTypeList(new ArrayList<>(List.of(2, 9, 8, 4))),
                f -> rawChange(f, 2, 1, 0).getBheInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(1))),
                f -> rawChange(f, 2, 1, 0).getBheInfoCollectionList1().get(0).setIntList3(new ArrayList<>(List.of(1, 0))),
                f -> rawChange(f, 2, 1, 0).getBheInfoCollectionList1().get(0).setInt2(1),
                f -> rawChange(f, 2, 1, 0).getBheInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(2))),
                f -> rawChange(f, 2, 0, 1).getBheInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(1))),
                f -> rawChange(f, 2, 2, 0).setStartFrame(48),
                f -> rawFamily(f, 2).getSkillSuffixList().get(2).setInt1(1),
                f -> rawFamily(f, 2).setSkillNameJapanese("別名：呪装刀Lv3"),
                f -> f.raw().getWazByFileName().clear())) {
            Fixture f = fixture();
            corrupt.accept(f);
            assertRejectedWithoutExtraWrites(f);
        }
    }

    @Test
    void convertedNearMissOrMalformedFinalLevelRejectsBeforeEitherEarlierLevelIsPublished() throws Exception {
        for (Consumer<Fixture> corrupt : List.<Consumer<Fixture>>of(
                f -> changes(family(f.main(), 2), 1).get(0).setStartFrame(8),
                f -> changes(family(f.main(), 2), 1).get(0).getBsdxInfoCollectionList1().get(0).setIntList3(new ArrayList<>()),
                f -> changes(family(f.main(), 2), 1).get(0).getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(2))),
                f -> family(f.main(), 2).getSkillSuffixList().get(1).setInt2(1),
                f -> family(f.main(), 2).setSkillNameJapanese("別名：呪装刀Lv3"),
                f -> family(f.main(), 2).setSkillNameEnglish("JUSOUTOU03_NEAR_MISS"),
                f -> f.main().getSkillList().add(copy(family(f.main(), 2), Waz.Skill.class)),
                f -> family(f.main(), 2).getPhasesInfo().get(1).getSkillUnitCollection().add(copy(unit(family(f.main(), 2), 1, 71), SkillUnit.class)),
                f -> unit(family(f.main(), 2), 1, 71).getSkillInfoObjectList().add(copy(changes(family(f.main(), 2), 1).get(0), CEventChange.class)),
                f -> unit(family(f.main(), 2), 1, 71).setSkillInfoUnknownList(null),
                f -> unit(family(f.main(), 2), 1, 71).setSkillInfoObjectList(null),
                f -> unit(family(f.main(), 2), 1, 71).getSkillInfoObjectList().remove(0))) {
            Fixture f = fixture();
            corrupt.accept(f);
            syncMainCount(f);
            assertRejectedWithoutExtraWrites(f);
        }
    }

    @Test
    void sameNamedButUnpairedSkillsRemainCompletelyUnchanged() throws Exception {
        Fixture f = fixture();
        Waz.Skill japaneseNearMiss = copy(family(f.main(), 2), Waz.Skill.class);
        japaneseNearMiss.setSkillNameJapanese("別機：呪装刀Lv3");
        Waz.Skill englishNearMiss = copy(family(f.main(), 2), Waz.Skill.class);
        englishNearMiss.setSkillNameEnglish("TEST_UNPAIRED_JUSOUTOU03");
        f.main().getSkillList().add(japaneseNearMiss);
        f.main().getSkillList().add(englishNearMiss);
        syncMainCount(f);
        Waz expected = expectedSinglePass(f.main());
        JsonNode japanese = payload(japaneseNearMiss), english = payload(englishNearMiss);

        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());

        assertPayloadEquals(expected, f.main(), "sou.waz complete graph with unpaired near misses");
        assertPayloadEquals(japanese, japaneseNearMiss, "unpaired Japanese near miss complete graph");
        assertPayloadEquals(english, englishNearMiss, "unpaired English near miss complete graph");
        assertSame(japaneseNearMiss, f.main().getSkillList().get(f.main().getSkillList().size() - 2));
        assertSame(englishNearMiss, f.main().getSkillList().get(f.main().getSkillList().size() - 1));
        assertEquals(2, changes(japaneseNearMiss, 1).size(), "matching English alone is insufficient to delete a branch");
        assertEquals(2, changes(englishNearMiss, 1).size(), "matching Japanese alone is insufficient to delete a branch");
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
        // FollowupConvertOverviewStep/LoadRawBheSourceStep retain this genuine pre-conversion view in production.
        raw.setTsukuyomiMek(rawMek);
        raw.getWazByFileName().put("sou.waz", rawMain);
        TsukuyomiBsdxBaselineBundle baseline = loadBaseline();
        TsukuyomiConvertedBundle prepared = new TsukuyomiConvertedBundle();
        new BheCommonProjectilePrepareStep().prepare(new TsukuyomiGraftRequest(), raw, baseline, prepared);
        var plan = prepared.getCommonProjectileAppendPlan();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek);
        Waz main = new BheToBsdxWazConverter().convert(rawMain);
        TsukuyomiPackageBundle selected = new TsukuyomiPackageBundle();
        selected.setTsukuyomiMek(mek);
        selected.getWazByFileName().put("sou.waz", copy(main, Waz.class));
        TsukuyomiGrpAppendPlan privatePlan = new TsukuyomiGrpAppendPlan();
        int mainGroup = baseline.getWazaGroupGrp().getWazaList().size();
        privatePlan.setWazaGroupIndex(mainGroup);
        privatePlan.getSourceWazGroupIndexToTargetIndex().put(rawMek.getMekBasicInfo().getWazFileSequence(), mainGroup);
        rebindWazaSelects(main, new BheResourceIndexResolver(plan, privatePlan), Collections.newSetFromMap(new IdentityHashMap<>()));
        new BheInfoCollectionObjectGraphRewriter().rewrite(main, "SouCursedBladeSingleTargetTest main");
        mek.getMekBasicInfo().setWazFileSequence(mainGroup);
        WazaGroupGrp.WazaGroupEntry entry = new WazaGroupGrp.WazaGroupEntry();
        entry.setExistFlag(1); entry.setWazaName("sou"); entry.setWazaCodeName("SOU"); entry.setWazaDisplayName("蒼");
        entry.setParam(main.getSkillList().size());
        baseline.getWazaGroupGrp().getWazaList().add(entry);
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
        context.setSelectedPackage(selected); context.setGrpAppendPlan(privatePlan);
        context.setReboundMek(mek); context.setReboundWaz(main);
        // Prime the other role repairs; Blade must first run through the public boundary in each test.
        SouSkillGlitchFixer.INSTANCE.fix(context);
        SouHumanCannonCounterFixer.fix(context);
        SouSnowMoonSingleTargetFixer.fix(context);
        Fixture f = new Fixture(context, raw, mainGroup, sourceFiles);
        for (int i = 0; i < 3; i++) {
            assertEquals(SUFFIX, suffix(family(main, i)));
            assertEquals(SUFFIX, rawFamily(f, i).getSkillSuffixList().stream().map(s -> List.of(s.getInt1(), s.getInt2())).toList());
            assertOriginalMultilock(rawChange(f, i, 0, 1), ENTRY.get(i), List.of(0, 0), List.of());
            assertOriginalMultilock(rawChange(f, i, 1, 0), REENTRY.get(i), List.of(0, 1), List.of(1));
            assertEquals(2, changes(family(main, i), 1).size());
            assertChange(changes(family(main, i), 1).get(0), REENTRY.get(i), List.of(0), List.of(), List.of(0, 0), List.of(0, 1), List.of(1));
        }
        assertEquals(211, context.getReboundMek().getMekWeaponInfoMap().size());
        assertEquals(211, context.getReboundMek().getMekMaterialBlock().getTrailingEntries().size());
        assertEquals("JUSOUTOU", context.getReboundMek().getMekWeaponInfoMap().get(63).getWeaponSequence());
        assertEquals(231, context.getReboundMek().getMekWeaponInfoMap().get(63).getWazSequence());
        assertSourceFilesUnchanged(f);
        return f;
    }

    private static Waz expectedSinglePass(Waz original) {
        Waz expected = copy(original, Waz.class);
        for (int i = 0; i < 3; i++) {
            SkillUnit changeSlot = unit(family(expected, i), 1, 71);
            assertEquals(2, changeSlot.getSkillInfoObjectList().size());
            assertChange((CEventChange) changeSlot.getSkillInfoObjectList().get(0), REENTRY.get(i),
                    List.of(0), List.of(), List.of(0, 0), List.of(0, 1), List.of(1));
            changeSlot.getSkillInfoObjectList().remove(0);
        }
        return expected;
    }

    private static List<List<Integer>> recoveryEdges(Waz.Skill skill) {
        List<List<Integer>> result = new ArrayList<>();
        Set<Integer> seen = new HashSet<>();
        int phase = skill.getSkillSuffixList().get(1).getInt1();
        while (phase < skill.getPhasesInfo().size()) {
            assertTrue(seen.add(phase), "recovery graph cannot restart a visited phase");
            List<CEventChange> events = changes(skill, phase);
            assertEquals(1, events.size());
            CEventChange event = events.get(0);
            assertEquals(List.of(0), event.getBsdxInfoCollectionList1().get(0).getTypeList());
            BsdxInfoCollection destination = event.getBsdxInfoCollectionList2().get(0);
            assertEquals(List.of(0, 0), destination.getTypeList(), "only the original NEXT recovery edges remain");
            assertTrue(destination.getParamList().isEmpty());
            int next = phase + 1;
            result.add(List.of(phase, event.getStartFrame(), next < skill.getPhasesInfo().size() ? next : -1));
            phase = next;
        }
        return result;
    }

    private static void assertChange(CEventChange event, int frame, List<Integer> types, List<Integer> params,
                                     List<Integer> objects, List<Integer> destinationTypes, List<Integer> destinationParams) {
        assertEquals(34, event.getTypeId()); assertEquals(frame, event.getStartFrame()); assertEquals(frame, event.getEndFrame());
        assertEquals(1, event.getFlag()); assertEquals(0, event.getInt1());
        assertEquals(1, event.getBsdxInfoCollectionList1().size()); assertEquals(1, event.getBsdxInfoCollectionList2().size());
        assertCollection(event.getBsdxInfoCollectionList1().get(0), 0, types, params, objects);
        assertCollection(event.getBsdxInfoCollectionList2().get(0), 16, destinationTypes, destinationParams, List.of());
    }

    private static void assertCollection(BsdxInfoCollection value, int root, List<Integer> types, List<Integer> params, List<Integer> objects) {
        assertEquals(root, value.getInt1()); assertEquals(types, value.getTypeList()); assertEquals(params, value.getParamList());
        assertEquals(objects, value.getIntList3()); assertEquals(List.of(), value.getIntList4()); assertEquals(0, value.getInt2());
    }

    private static void assertOriginalMultilock(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange event,
                                               int frame, List<Integer> destinationTypes, List<Integer> destinationParams) {
        assertEquals(40, event.getTypeId()); assertEquals(frame, event.getStartFrame()); assertEquals(frame, event.getEndFrame());
        assertEquals(1, event.getFlag()); assertEquals(0, event.getInt1());
        assertEquals(1, event.getBheInfoCollectionList1().size()); assertEquals(1, event.getBheInfoCollectionList2().size());
        var condition = event.getBheInfoCollectionList1().get(0);
        assertEquals(0, condition.getInt1()); assertEquals(List.of(2, 9, 8, 5), condition.getTypeList());
        assertEquals(List.of(0), condition.getParamList()); assertEquals(List.of(0, 0), condition.getIntList3());
        assertEquals(List.of(), condition.getIntList4()); assertEquals(0, condition.getInt2());
        var destination = event.getBheInfoCollectionList2().get(0);
        assertEquals(16, destination.getInt1()); assertEquals(destinationTypes, destination.getTypeList());
        assertEquals(destinationParams, destination.getParamList()); assertEquals(List.of(), destination.getIntList3());
        assertEquals(List.of(), destination.getIntList4()); assertEquals(0, destination.getInt2());
    }

    private static Waz.Skill family(Waz main, int level) {
        List<Waz.Skill> matches = main.getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> ENGLISH.get(level).equals(s.getSkillNameEnglish()) && JAPANESE.get(level).equals(s.getSkillNameJapanese())).toList();
        assertEquals(1, matches.size(), "unique exact Sou cursed-blade identity: " + ENGLISH.get(level));
        return matches.get(0);
    }

    private static com.giga.nexas.dto.bhe.waz.Waz.Skill rawFamily(Fixture f, int level) {
        var matches = f.raw().getWazByFileName().get("sou.waz").getSkillList().stream().filter(Objects::nonNull)
                .filter(s -> ENGLISH.get(level).equals(s.getSkillNameEnglish()) && JAPANESE.get(level).equals(s.getSkillNameJapanese())).toList();
        assertEquals(1, matches.size());
        return matches.get(0);
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange rawChange(Fixture f, int level, int phase, int ordinal) {
        var slots = rawFamily(f, level).getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(82).equals(u.getUnitQuantity())).toList();
        assertEquals(1, slots.size());
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventChange) slots.get(0).getSkillInfoObjectList().get(ordinal);
    }

    private static SkillUnit unit(Waz.Skill skill, int phase, int number) {
        var slots = skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(number).equals(u.getUnitQuantity())).toList();
        assertEquals(1, slots.size());
        return slots.get(0);
    }

    private static List<CEventChange> changes(Waz.Skill skill, int phase) {
        SkillUnit slot = unit(skill, phase, 71);
        assertTrue(slot.getSkillInfoUnknownList().isEmpty(), "there are no FF records in the Sou63 Change slot");
        return slot.getSkillInfoObjectList().stream().map(e -> assertInstanceOf(CEventChange.class, e)).toList();
    }

    private static List<List<Integer>> suffix(Waz.Skill skill) {
        return skill.getSkillSuffixList().stream().map(s -> List.of(s.getInt1(), s.getInt2())).toList();
    }

    private static void syncMainCount(Fixture f) {
        f.context().getBaseline().getWazaGroupGrp().getWazaList().get(f.mainGroup()).setParam(f.main().getSkillList().size());
        f.context().getGrpAppendPlan().getTargetWazSkillCountByGroupIndex().put(f.mainGroup(), f.main().getSkillList().size());
    }

    private static Snapshot snapshot(Fixture f) throws Exception {
        return new Snapshot(digest(f.main()), preservedGraphs(f));
    }

    private static Map<String, String> preservedGraphs(Fixture f) throws Exception {
        Map<String, String> graphs = new LinkedHashMap<>();
        graphs.put("source", digest(f.raw())); graphs.put("mek", digest(f.context().getReboundMek()));
        graphs.put("selected", digest(f.context().getSelectedPackage()));
        graphs.put("common-plan", digest(f.context().getConvertedBundle().getCommonProjectileAppendPlan()));
        graphs.put("private-plan", digest(f.context().getGrpAppendPlan()));
        graphs.put("groups", digest(f.context().getBaseline().getWazaGroupGrp()));
        for (var entry : f.context().getBaseline().getWazByFileName().entrySet())
            if (!"sou.waz".equalsIgnoreCase(entry.getKey())) graphs.put("public:" + entry.getKey(), digest(entry.getValue()));
        return graphs;
    }

    private static void assertRejectedWithoutExtraWrites(Fixture f) throws Exception {
        Snapshot before = snapshot(f);
        List<Waz.Skill> mainList = f.main().getSkillList();
        List<Waz.Skill> skills = new ArrayList<>(mainList);
        assertThrows(IllegalStateException.class, () -> SouCustomizer.INSTANCE.customizeBeforeWrite(f.context()),
                "source/target drift or a partial installation is rejected before any level is published");
        assertEquals(before, snapshot(f), "rejection must preserve every complete current graph, including the intentionally drifted source");
        assertSame(mainList, f.main().getSkillList());
        for (int i = 0; i < skills.size(); i++) assertSame(skills.get(i), mainList.get(i));
        assertSourceFilesUnchanged(f);
    }

    private static void assertSourceFilesUnchanged(Fixture f) throws Exception {
        for (var entry : f.sourceFiles().entrySet()) assertArrayEquals(entry.getValue(), Files.readAllBytes(entry.getKey()), "read-only original: " + entry.getKey());
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
            Path directory = Path.of("target/sou063-debug-20261030/graph-diff");
            Files.createDirectories(directory);
            String stem = label.replaceAll("[^A-Za-z0-9]+", "-") + "-" + System.currentTimeMillis();
            Path expectedFile = directory.resolve(stem + ".expected.json");
            Path actualFile = directory.resolve(stem + ".actual.json");
            Path differenceFile = directory.resolve(stem + ".diff.txt");
            writeDiagnostic(expectedFile, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(expectedGraph));
            writeDiagnostic(actualFile, JSON.writerWithDefaultPrettyPrinter().writeValueAsString(actualGraph));
            String summary = label + "; expected SHA-256=" + digest(expectedGraph) + "; actual SHA-256=" + digest(actualGraph)
                    + "\nexpected graph: " + expectedFile + "\nactual graph: " + actualFile
                    + "\nfirst differences (at most 10):\n" + String.join("\n", differences);
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
        node.forEach(SouCursedBladeSingleTargetTest::stripMetadata);
    }

    private record Snapshot(String mainSha256, Map<String, String> preserved) { }

    private record Fixture(FollowupMekaContext context, TsukuyomiRawSourceBundle raw, int mainGroup, Map<Path, byte[]> sourceFiles) {
        Waz main() { return context.getReboundWaz(); }
    }
}
