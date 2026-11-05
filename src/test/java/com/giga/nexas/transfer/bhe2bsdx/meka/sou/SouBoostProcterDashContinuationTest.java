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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Real-source dash continuation regression. Its small native-condition model is not game/runtime validation. */
@Execution(ExecutionMode.SAME_THREAD)
class SouBoostProcterDashContinuationTest {
    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final List<String> MAIN_NAMES = List.of("B_PROCTER01", "B_PROCTER02", "B_PROCTER03");
    private static final List<String> PUBLIC_NAMES = List.of("BIT61", "BIT62", "BIT48");
    private static final List<String> PUBLIC_JP = List.of("◆蒼：ブースト・プロクターLv1", "◆蒼：ブースト・プロクターLv2", "◆蒼：ブースト・プロクターLv3");
    private static final List<String> PRIVATE_NAMES = List.of("DX_SOU_BOOST_PROCTER_LV1", "DX_SOU_BOOST_PROCTER_LV2", "DX_SOU_BOOST_PROCTER_LV3");
    private static final List<String> PRIVATE_JP = List.of("◆蒼：ブースト・プロクターLv1・継続", "◆蒼：ブースト・プロクターLv2・継続", "◆蒼：ブースト・プロクターLv3・継続");
    private static final Path BHE = Path.of("src/main/resources/game/bhe"), DX = Path.of("src/main/resources/game/bsdx");
    private static final String CHARSET = "windows-31j";
    @TempDir Path output;

    @Test void threeCallersOnlyChangeTheirOwnReferenceAndKeepTwentyFourEighteenTwelveFrames() throws Exception {
        Fixture f = fixture(); Waz expected = copy(f.main(), Waz.class); int tail = f.host().getSkillList().size();
        Map<String,String> before = preserved(f);
        for (int level = 0; level < 3; level++) reference(effect(skill(expected, MAIN_NAMES.get(level)), 0, 0)).setWazSequenceNo(tail + level);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(tail + 3, f.host().getSkillList().size(), "three internal skills, not new player weapons");
        assertEquals(payload(expected), payload(f.main()), "only the three main body sequence references change");
        for (int level = 0; level < 3; level++) {
            assertEquals(tail + level, index(f.host(), PRIVATE_NAMES.get(level)));
            CEventEffect caller = effect(skill(f.main(), MAIN_NAMES.get(level)), 0, 0);
            assertEquals(List.of(24 - level * 6,24 - level * 6), frames(caller));
            assertEquals(f.hostGroup(), reference(caller).getWazFileNo());
        }
        assertEquals(before, preserved(f)); assertSourceFiles(f);
    }

    @Test void eachPrivateBodyOnlyLosesItsIncorrectEndOrBranchAndKeepsItsOwnLevelPayload() throws Exception {
        Fixture f = fixture(); List<Waz.Skill> prototypes = java.util.stream.IntStream.range(0, 3).mapToObj(level -> copy(publicBody(f, level), Waz.Skill.class)).toList();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        for (int level = 0; level < 3; level++) {
            Waz.Skill body = skill(f.host(), PRIVATE_NAMES.get(level));
            assertEquals(payload(expectedBody(prototypes.get(level), level)), payload(body), "preserve this level's complete graph");
            assertEquals(2, unit(body, 1, 71).getSkillInfoObjectList().size());
            CEventChange stopped = change(body, 1, 0), cap = change(body, 1, 1), end = change(body, 2, 0);
            assertEquals(List.of(0,149), frames(stopped)); assertEquals(2, stopped.getFlag()); assertEquals(0, stopped.getInt1());
            assertEquals(List.of(List.of(2,1,4),List.of(2,1,5)), stopped.getBsdxInfoCollectionList1().stream().map(BsdxInfoCollection::getTypeList).toList());
            for (var condition : stopped.getBsdxInfoCollectionList1()) {
                assertEquals(1, condition.getInt2()); assertEquals(List.of(3,0), condition.getIntList3());
            }
            assertEquals(List.of(149,149), frames(cap)); assertEquals(List.of(0), cap.getBsdxInfoCollectionList1().get(0).getTypeList());
            assertEquals(List.of(40,40), frames(end)); assertEquals(List.of(0,2), end.getBsdxInfoCollectionList2().get(0).getTypeList());
            assertEquals(payload(prototypes.get(level).getPhasesInfo().get(0)), payload(body.getPhasesInfo().get(0)), "P0 memory/durability/contact guards stay in P0");
            assertEquals(payload(prototypes.get(level).getPhasesInfo().get(2)), payload(body.getPhasesInfo().get(2)), "original P2 fade stays intact");
            assertEquals(List.of(5,5), frames(effect(body, 0, 1)));
            assertEquals(List.of(10,10), frames(effect(body, 1, 1)));
            assertEquals(f.childGroup(), reference(effect(body, 0, 1)).getWazFileNo()); assertEquals(f.childBase() + 145, reference(effect(body, 0, 1)).getWazSequenceNo());
            assertEquals(f.childGroup(), reference(effect(body, 1, 1)).getWazFileNo()); assertEquals(f.childBase() + 146, reference(effect(body, 1, 1)).getWazSequenceNo());
        }
        // The three real prototypes differ beyond names. A shared Lv3 copy must not replace the lower-level payloads.
        assertNotEquals(levelPayload(prototypes.get(0)), levelPayload(prototypes.get(2)));
        assertNotEquals(levelPayload(prototypes.get(1)), levelPayload(prototypes.get(2)));
        assertNotEquals(levelPayload(skill(f.host(), PRIVATE_NAMES.get(0))), levelPayload(skill(f.host(), PRIVATE_NAMES.get(2))));
        assertNotEquals(levelPayload(skill(f.host(), PRIVATE_NAMES.get(1))), levelPayload(skill(f.host(), PRIVATE_NAMES.get(2))));
    }

    @Test void nativeDashStatesReachTheTenFrameEmissionAndStopOnlyAfterBothCeaseOrOriginalCap() throws Exception {
        Fixture f = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        for (int level = 0; level < 3; level++) {
            Waz.Skill original = publicBody(f, level), body = skill(f.host(), PRIVATE_NAMES.get(level));
            for (int nativeState : List.of(3,4)) {
                assertEquals(2, nextBodyPhase(original, 1, 0, true, nativeState), "the converted END alias reproduces the premature exit");
                for (int clock = 0; clock < 149; clock++)
                    assertEquals(1, nextBodyPhase(body, 1, clock, true, nativeState), "hold either native dash state through the original P1 window");
                assertEquals(1, nextBodyPhase(body, 1, effect(body, 1, 1).getStartFrame(), true, nativeState), "original rebound horizontal-child emission at 10f is reachable");
                assertEquals(2, nextBodyPhase(body, 1, 149, true, nativeState), "retain the original cap, not an invented timer");
            }
            assertEquals(1, nextBodyPhase(body, 1, 8, true, 3));
            assertEquals(1, nextBodyPhase(body, 1, 9, true, 4), "switching directly between dash states does not satisfy NOT-AND");
            for (int stoppedOrInterrupted : List.of(0,1,2,5,6,7,8,9,10,11,12))
                assertEquals(2, nextBodyPhase(body, 1, 12, true, stoppedOrInterrupted));
            assertEquals(1, nextBodyPhase(body, 1, 0, false, -1), "NULL returns native unknown, not an immediate NOT-AND exit");
            assertEquals(1, nextBodyPhase(body, 1, 148, false, -1));
            assertEquals(2, nextBodyPhase(body, 1, 149, false, -1), "NULL still reaches the original unconditional cap");
            assertEquals(2, nextBodyPhase(body, 2, 39, false, -1));
            assertEquals(-1, nextBodyPhase(body, 2, 40, false, -1), "existing 40f END finishes the original exit phase");
        }
    }

    @Test void publicPrefixChildrenOtherRepairsAndMutableNodeIsolationArePreserved() throws Exception {
        Fixture f = fixture(); int count = f.host().getSkillList().size(); JsonNode prefix = payload(f.host().getSkillList());
        JsonNode groups = payload(f.context().getBaseline().getWazaGroupGrp()); Map<String,String> before = preserved(f);
        byte[] prefixBytes = binary(f.host(), output.resolve("prefix-before.waz"));
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(count + 3, f.host().getSkillList().size());
        assertEquals(prefix, payload(f.host().getSkillList().subList(0, count)));
        assertArrayEquals(prefixBytes, Arrays.copyOf(binary(f.host(), output.resolve("prefix-after.waz")), prefixBytes.length));
        assertIndependentMutableGraphs(java.util.stream.IntStream.range(0, 3).mapToObj(level -> publicBody(f, level)).toList(), PRIVATE_NAMES.stream().map(n -> skill(f.host(), n)).toList());
        ((ObjectNode) groups.path("wazaList").get(f.hostGroup())).put("param", count + 3);
        assertEquals(groups, payload(f.context().getBaseline().getWazaGroupGrp()));
        assertEquals(before, preserved(f), "includes the full rebound child host, other hosts, MEK and plans; the body prefix separately protects cohosted Cool Gain");
        assertSourceFiles(f);
    }

    @Test void dynamicTailAndVerifiedRepeatPreserveAllThreeInstalledObjectsAndLaterAppends() throws Exception {
        Fixture f = fixture(); appendUnrelated(f, "BEFORE_BOOST_PROCTER"); int tail = f.host().getSkillList().size();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        for (int level = 0; level < 3; level++) assertEquals(tail + level, index(f.host(), PRIVATE_NAMES.get(level)));
        appendUnrelated(f, "AFTER_BOOST_PROCTER");
        List<Waz.Skill> main = f.main().getSkillList(), host = f.host().getSkillList();
        List<Waz.Skill> installed = PRIVATE_NAMES.stream().map(n -> skill(f.host(), n)).toList(); String before = snapshot(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertSame(main, f.main().getSkillList()); assertSame(host, f.host().getSkillList()); assertEquals(before, snapshot(f));
        for (int level = 0; level < 3; level++) assertSame(installed.get(level), skill(f.host(), PRIVATE_NAMES.get(level)));
    }

    @Test void rawSourceAndPublicBodyOrChildDriftRejectBeforeAnyPublication() throws Exception {
        for (Consumer<Fixture> corruption : List.<Consumer<Fixture>>of(
                f -> f.raw().getWazByFileName().get("sou.waz").getSkillList().get(416).setSkillNameEnglish("B_PROCTER03_DRIFT"),
                f -> f.raw().getCommonProjectileWazByFileName().get("tama03.waz").getSkillList().get(61).getSkillSuffixList().get(2).setInt1(1),
                f -> f.raw().getCommonProjectileWazByFileName().get("tama04.waz").getSkillList().get(146).setSkillNameEnglish("0183_DRIFT"),
                f -> change(publicBody(f, 0), 0, 0).setInt1(0),
                f -> change(publicBody(f, 1), 1, 0).getBsdxInfoCollectionList1().get(0).setInt2(0),
                f -> change(publicBody(f, 2), 1, 1).setInt1(0),
                f -> effect(publicBody(f, 2), 1, 1).setStartFrame(9),
                f -> change(publicBody(f, 2), 2, 0).setStartFrame(39),
                f -> change(childSkill(f, 146), 0, 0).setStartFrame(1))) {
            Fixture f = fixture(); corruption.accept(f); assertRejected(f);
        }
    }

    @Test void partialInstallDuplicateCallerAndTamperedPrivateGraphsCannotBeSilentlyRepaired() throws Exception {
        Fixture partial = fixture(); reference(effect(skill(partial.main(), "B_PROCTER03"), 0, 0)).setWazSequenceNo(partial.host().getSkillList().size() + 2); assertRejected(partial);
        Fixture duplicate = fixture(); duplicate.main().getSkillList().add(copy(skill(duplicate.main(), "B_PROCTER03"), Waz.Skill.class));
        duplicate.context().getBaseline().getWazaGroupGrp().getWazaList().get(duplicate.context().getGrpAppendPlan().getWazaGroupIndex()).setParam(duplicate.main().getSkillList().size());
        duplicate.context().getGrpAppendPlan().getTargetWazSkillCountByGroupIndex().put(duplicate.context().getGrpAppendPlan().getWazaGroupIndex(), duplicate.main().getSkillList().size()); assertRejected(duplicate);
        Fixture installed = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(installed.context());
        change(skill(installed.host(), PRIVATE_NAMES.get(1)), 1, 0).setInt1(1); assertRejected(installed);
        Fixture missing = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(missing.context());
        skill(missing.host(), PRIVATE_NAMES.get(2)).setSkillNameEnglish("INCOMPLETE_BOOST_PROCTER"); assertRejected(missing);
        Fixture duplicatePrivate = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(duplicatePrivate.context());
        duplicatePrivate.host().getSkillList().add(copy(skill(duplicatePrivate.host(), PRIVATE_NAMES.get(0)), Waz.Skill.class));
        duplicatePrivate.context().getBaseline().getWazaGroupGrp().getWazaList().get(duplicatePrivate.hostGroup()).setParam(duplicatePrivate.host().getSkillList().size()); assertRejected(duplicatePrivate);
    }

    @Test void mainHostAndGroupSurviveTwoBinaryPassesAndParsedIdempotence() throws Exception {
        Fixture f = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        for (int level = 0; level < 3; level++) assertEquals(PRIVATE_JP.get(level), skill(f.host(), PRIVATE_NAMES.get(level)).getSkillNameJapanese());
        BsdxBinService service = new BsdxBinService(); Waz[] parsed = new Waz[2];
        Waz[] originals = {f.main(),f.host()};
        for (int i = 0; i < 2; i++) {
            Path first = output.resolve("waz-" + i + "-first.waz"), second = output.resolve("waz-" + i + "-second.waz");
            byte[] bytes = binary(originals[i], first); parsed[i] = (Waz) service.parse(first.toString(), CHARSET).getData();
            assertEquals(payload(headerReadback(originals[i])), payload(parsed[i])); assertArrayEquals(bytes, binary(parsed[i], second));
        }
        Path first = Files.createDirectories(output.resolve("group-first")).resolve("WazaGroup.grp");
        Path second = Files.createDirectories(output.resolve("group-second")).resolve("WazaGroup.grp");
        service.generate(first.toString(), f.context().getBaseline().getWazaGroupGrp(), CHARSET);
        var group = (WazaGroupGrp) service.parse(first.toString(), CHARSET).getData(); service.generate(second.toString(), group, CHARSET);
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
        f.context().setReboundWaz(parsed[0]); f.context().getBaseline().getWazByFileName().put("sou.waz", parsed[0]);
        String hostKey = f.context().getBaseline().getWazByFileName().keySet().stream().filter(f.hostName()::equalsIgnoreCase).findFirst().orElseThrow();
        f.context().getBaseline().getWazByFileName().put(hostKey, parsed[1]); f.context().getBaseline().setWazaGroupGrp(group);
        String before = snapshot(f); SouCustomizer.INSTANCE.customizeBeforeWrite(f.context()); assertEquals(before, snapshot(f)); assertSourceFiles(f);
    }

    private static Waz.Skill expectedBody(Waz.Skill prototype, int level) {
        Waz.Skill expected = copy(prototype, Waz.Skill.class);
        expected.setSkillNameJapanese(PRIVATE_JP.get(level)); expected.setSkillNameEnglish(PRIVATE_NAMES.get(level));
        unit(expected, 1, 71).getSkillInfoObjectList().remove(1);
        return expected;
    }

    private static JsonNode levelPayload(Waz.Skill body) {
        ObjectNode tree = (ObjectNode) payload(body); tree.remove(List.of("skillNameJapanese", "skillNameEnglish")); return tree;
    }

    // Only the native condition-result rules in 6685D0/66A730/66B6E0 are evaluated; this is not a game simulator.
    private static int nextBodyPhase(Waz.Skill body, int phase, int clock, boolean rootPresent, int rootState) {
        for (SkillInfoObject object : unit(body, phase, 71).getSkillInfoObjectList()) {
            CEventChange event = assertInstanceOf(CEventChange.class, object);
            if (clock < event.getStartFrame() || clock > event.getEndFrame()) continue;
            assertEquals(event.getFlag(), event.getBsdxInfoCollectionList1().size());
            List<Integer> results = new ArrayList<>();
            for (var condition : event.getBsdxInfoCollectionList1()) {
                int result;
                if (condition.getTypeList().equals(List.of(0))) result = 2;
                else {
                    assertEquals(List.of(3,0), condition.getIntList3());
                    int expectedState;
                    if (condition.getTypeList().equals(List.of(2,1,4))) expectedState = 3;
                    else if (condition.getTypeList().equals(List.of(2,1,5))) expectedState = 4;
                    else throw new AssertionError("Unreviewed predicate " + condition.getTypeList());
                    result = !rootPresent ? 3 : rootState == expectedState ? 2 : 0;
                }
                if (condition.getInt2() == 1) result = result == 0 ? 2 : result == 1 || result == 2 ? 0 : result;
                else assertEquals(0, condition.getInt2());
                results.add(result);
            }
            boolean matches = switch (event.getInt1()) {
                case 0 -> results.stream().allMatch(v -> v == 2);
                case 1 -> results.stream().anyMatch(v -> v == 2);
                default -> throw new AssertionError("Unreviewed AND/OR mode");
            };
            if (matches) {
                var target = event.getBsdxInfoCollectionList2().get(0);
                if (target.getTypeList().equals(List.of(0,0))) return phase + 1;
                assertEquals(List.of(0,2), target.getTypeList()); return -1;
            }
        }
        return phase;
    }

    private static void appendUnrelated(Fixture f, String name) { Waz.Skill extra = copy(publicBody(f, 0), Waz.Skill.class); extra.setSkillNameJapanese(name); extra.setSkillNameEnglish(name); f.host().getSkillList().add(extra); f.context().getBaseline().getWazaGroupGrp().getWazaList().get(f.hostGroup()).setParam(f.host().getSkillList().size()); }
    private static Waz.Skill publicBody(Fixture f, int level) {
        return sourceSliceSkill(f, "tama03.waz", 61 + level, PUBLIC_JP.get(level), PUBLIC_NAMES.get(level));
    }

    private static Waz.Skill childSkill(Fixture f, int sourceIndex) {
        assertTrue(sourceIndex == 145 || sourceIndex == 146);
        return sourceSliceSkill(f, "tama04.waz", sourceIndex,
                sourceIndex == 145 ? "◆蒼：ブースト・プロクター：縦" : "◆蒼：ブースト・プロクター：横",
                sourceIndex == 145 ? "0119" : "0183");
    }

    private static Waz.Skill sourceSliceSkill(Fixture f, String sourceName, int sourceIndex, String japanese, String english) {
        var plan = f.context().getConvertedBundle().getCommonProjectileAppendPlan();
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf(sourceName);
        assertTrue(sourceGroup >= 0);
        var source = f.raw().getCommonProjectileWazByFileName().get(sourceName);
        assertNotNull(source); assertTrue(sourceIndex >= 0 && sourceIndex < source.getSkillList().size());
        assertEquals(japanese, source.getSkillList().get(sourceIndex).getSkillNameJapanese());
        assertEquals(english, source.getSkillList().get(sourceIndex).getSkillNameEnglish());
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        var hosts = f.context().getBaseline().getWazByFileName().entrySet().stream().filter(e -> hostName.equalsIgnoreCase(e.getKey())).toList();
        assertEquals(1, hosts.size()); Waz host = hosts.get(0).getValue();
        int base = plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
        int count = plan.getSourceWazIndexToTargetSkillCount().get(sourceGroup);
        assertEquals(source.getSkillList().size(), count);
        int end = Math.addExact(base, count);
        assertTrue(base >= 0 && end <= host.getSkillList().size());
        List<Integer> matches = new ArrayList<>();
        for (int i = base; i < end; i++) {
            Waz.Skill candidate = host.getSkillList().get(i);
            if (candidate != null && japanese.equals(candidate.getSkillNameJapanese()) && english.equals(candidate.getSkillNameEnglish())) matches.add(i);
        }
        assertEquals(List.of(Math.addExact(base, sourceIndex)), matches, "one exact dual-name skill at its original source offset within " + sourceName);
        return host.getSkillList().get(matches.get(0));
    }

    private static int index(Waz waz, String name) { List<Integer> matches = new ArrayList<>(); for (int i = 0; i < waz.getSkillList().size(); i++) if (waz.getSkillList().get(i) != null && name.equals(waz.getSkillList().get(i).getSkillNameEnglish())) matches.add(i); assertEquals(1, matches.size(), "unique skill " + name); return matches.get(0); }
    private static Waz.Skill skill(Waz waz, String name) { return waz.getSkillList().get(index(waz, name)); }
    private static SkillUnit unit(Waz.Skill s, int phase, int slot) { return s.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> u.getUnitQuantity() == slot).findFirst().orElseThrow(); }
    private static CEventEffect effect(Waz.Skill s, int phase, int ordinal) { return assertInstanceOf(CEventEffect.class, unit(s, phase, 48).getSkillInfoObjectList().get(ordinal)); }
    private static CEventChange change(Waz.Skill s, int phase, int ordinal) { return assertInstanceOf(CEventChange.class, unit(s, phase, 71).getSkillInfoObjectList().get(ordinal)); }
    private static CEventWazaSelect reference(CEventEffect e) { return e.getCeventEffectUnitList().stream().filter(u -> u.getUnitSlotNum() == 3).map(u -> assertInstanceOf(CEventWazaSelect.class, u.getData())).findFirst().orElseThrow(); }
    private static List<Integer> frames(SkillInfoObject e) { return List.of(e.getStartFrame(),e.getEndFrame()); }
    private static byte[] binary(Waz waz, Path path) throws Exception { new BsdxBinService().generate(path.toString(), waz, CHARSET); return Files.readAllBytes(path); }
    private static String digest(Object value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(payload(value)))); }
    private static JsonNode payload(Object value) { JsonNode n = JSON.valueToTree(value); strip(n); return n; }
    private static void strip(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset","length","slotNum","unitDescription","description","fileName","extensionName")); n.forEach(SouBoostProcterDashContinuationTest::strip); }
    private static <T> T copy(T value, Class<T> type) { try { return JSON.readValue(JSON.writeValueAsBytes(value), type); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static Waz headerReadback(Waz value) { Waz expected = copy(value,Waz.class); for (Waz.Skill s : expected.getSkillList()) if (Integer.valueOf(0).equals(s.getPhaseQuantity()) && s.getPhasesInfo().isEmpty() && s.getSkillSuffixList().isEmpty() && !s.isEmpty() && (s.getSkillNameJapanese() == null || s.getSkillNameJapanese().isEmpty()) && (s.getSkillNameEnglish() == null || s.getSkillNameEnglish().isEmpty())) { if (s.getSkillNameJapanese() == null) s.setSkillNameJapanese(""); if (s.getSkillNameEnglish() == null) s.setSkillNameEnglish(""); } return expected; }
    private static void assertSourceFiles(Fixture f) throws Exception { for (var e : f.sourceFiles().entrySet()) assertArrayEquals(e.getValue(), Files.readAllBytes(e.getKey())); }
    private static Map<String,String> preserved(Fixture f) throws Exception { Map<String,String> m = new TreeMap<>(); m.put("raw",digest(f.raw())); m.put("mek",digest(f.context().getReboundMek())); m.put("selected",digest(f.context().getSelectedPackage())); m.put("private-plan",digest(f.context().getGrpAppendPlan())); m.put("common-plan",digest(f.context().getConvertedBundle().getCommonProjectileAppendPlan())); for (var e : f.context().getBaseline().getWazByFileName().entrySet()) if (!e.getKey().equalsIgnoreCase("sou.waz") && !e.getKey().equalsIgnoreCase(f.hostName())) m.put(e.getKey(),digest(e.getValue())); return m; }
    private static String snapshot(Fixture f) throws Exception { return digest(List.of(f.main(), f.host(), f.context().getBaseline().getWazaGroupGrp(), preserved(f))); }
    private static void assertRejected(Fixture f) throws Exception { String before = snapshot(f); var main = f.main().getSkillList(); var host = f.host().getSkillList(); assertThrows(IllegalStateException.class, () -> SouCustomizer.INSTANCE.customizeBeforeWrite(f.context())); assertEquals(before,snapshot(f)); assertSame(main,f.main().getSkillList()); assertSame(host,f.host().getSkillList()); assertSourceFiles(f); }

    private static void assertIndependentMutableGraphs(Object publicGraph, Object privateGraph) throws Exception {
        Set<Object> publicNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        Set<Object> privateNodes = Collections.newSetFromMap(new IdentityHashMap<>());
        collectMutableIdentities(publicGraph, publicNodes);
        collectMutableIdentities(privateGraph, privateNodes);
        for (Object node : privateNodes)
            assertFalse(publicNodes.contains(node), "private graph shares mutable " + node.getClass().getName() + " with a public prototype");
    }

    private static void collectMutableIdentities(Object node, Set<Object> seen) throws Exception {
        if (node == null) return;
        Class<?> type = node.getClass();
        if (!type.isArray() && !(node instanceof Collection<?>) && !(node instanceof Map<?, ?>)
                && !type.getName().startsWith("com.giga.nexas.dto.")) return;
        if (!seen.add(node)) return;
        if (type.isArray()) {
            if (!type.getComponentType().isPrimitive())
                for (int i = 0; i < Array.getLength(node); i++) collectMutableIdentities(Array.get(node, i), seen);
            return;
        }
        if (node instanceof Collection<?> values) {
            for (Object value : values) collectMutableIdentities(value, seen);
            return;
        }
        if (node instanceof Map<?, ?> values) {
            for (var entry : values.entrySet()) {
                collectMutableIdentities(entry.getKey(), seen);
                collectMutableIdentities(entry.getValue(), seen);
            }
            return;
        }
        for (Class<?> current = type; current != null; current = current.getSuperclass())
            for (Field field : current.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                field.setAccessible(true);
                collectMutableIdentities(field.get(node), seen);
            }
    }

    private record Fixture(FollowupMekaContext context, TsukuyomiRawSourceBundle raw, String hostName, int hostGroup, String childName, int childGroup, int childBase, Map<Path,byte[]> sourceFiles) {
        Waz main() { return context.getReboundWaz(); }
        Waz child() { return context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> childName.equalsIgnoreCase(e.getKey())).findFirst().orElseThrow().getValue(); }
        Waz host() { return context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> hostName.equalsIgnoreCase(e.getKey())).findFirst().orElseThrow().getValue(); }
    }

    private Fixture fixture() throws Exception {
        BheBinService bhe = new BheBinService();
        Path mekPath = BHE.resolve("mek/sou/sou.mek"), mainPath = BHE.resolve("waz/sou/sou.waz");
        Map<Path, byte[]> sourceFiles = new LinkedHashMap<>();
        for (Path p : List.of(mekPath, mainPath, resolve(BHE.resolve("waz"), "tama03.waz"), resolve(BHE.resolve("waz"), "tama04.waz"), resolve(DX.resolve("waz"), "Tama05.waz"), resolve(DX.resolve("waz"), "Laser.waz")))
            sourceFiles.put(p, Files.readAllBytes(p));
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) bhe.parse(mekPath.toString(), CHARSET).getData();
        var rawMain = (com.giga.nexas.dto.bhe.waz.Waz) bhe.parse(mainPath.toString(), CHARSET).getData();
        TsukuyomiRawSourceBundle raw = loadCommonSource(bhe);
        raw.setTsukuyomiMek(rawMek); raw.getWazByFileName().put("sou.waz", rawMain);
        TsukuyomiBsdxBaselineBundle baseline = loadBaseline();
        // This standalone real-DX fixture has no JINKI-inherited Tama03/Tama04 hosts.
        // Pin the actual fallback identities and original host lengths before preparation.
        int bodyPrefix = baseline.getWazByFileName().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Tama05.waz")).findFirst().orElseThrow().getValue().getSkillList().size();
        int childPrefix = baseline.getWazByFileName().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Laser.waz")).findFirst().orElseThrow().getValue().getSkillList().size();
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
        // Declare the actual nonidentity private-SE reference boundary, without pretending this fixture delivers audio assets.
        privatePlan.getSourceSeGroupIndexToTargetIndex().put(16, 46);
        privatePlan.getSourceSeItemIndexToTargetIndexByGroup().put(16, new LinkedHashMap<>(Map.of(4, 11)));
        rebindWazaSelects(main, new BheResourceIndexResolver(plan, privatePlan), Collections.newSetFromMap(new IdentityHashMap<>()));
        new BheInfoCollectionObjectGraphRewriter().rewrite(main, "SouBoostProcterDashContinuationTest main");
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
        // Existing repairs, including Cool Gain from HEAD, are isolated; Boost Procter first runs through the public Customizer.
        SouSkillGlitchFixer.INSTANCE.fix(context);
        SouHumanCannonCounterFixer.fix(context);
        SouCursedBladeSingleTargetFixer.fix(context);
        SouSnowMoonSingleTargetFixer.fix(context);
        SouFlareSeekerSingleTargetFixer.fix(context);
        SouCoolGainReturnProtocolFixer.fix(context);
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama03.waz");
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        int hostGroup = plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        int childSourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama04.waz");
        String childName = plan.getSourceWazIndexToTargetFileName().get(childSourceGroup);
        int childGroup = plan.getSourceWazIndexToTargetIndex().get(childSourceGroup), childBase = plan.getSourceWazIndexToTargetSkillBase().get(childSourceGroup);
        Fixture f = new Fixture(context, raw, hostName, hostGroup, childName, childGroup, childBase, sourceFiles);
        assertEquals(3, sourceGroup); assertEquals(4, childSourceGroup);
        assertEquals("tama05.waz", hostName.toLowerCase(Locale.ROOT)); assertEquals(5, hostGroup);
        assertEquals("laser.waz", childName.toLowerCase(Locale.ROOT)); assertEquals(6, childGroup);
        assertEquals(bodyPrefix, plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup));
        assertEquals(childPrefix, childBase);
        for (int level = 0; level < 3; level++) {
            assertEquals(MAIN_NAMES.get(level), rawMain.getSkillList().get(414 + level).getSkillNameEnglish());
            Waz.Skill body = publicBody(f, level);
            assertSame(f.host().getSkillList().get(bodyPrefix + 61 + level), body); assertEquals(3, body.getPhaseQuantity());
            assertEquals(3, unit(body, 1, 71).getSkillInfoObjectList().size());
            assertEquals(0, change(body, 1, 0).getInt1()); assertEquals(1, change(body, 1, 1).getInt1());
            assertEquals(List.of(List.of(2,1,4),List.of(2,1,5)), change(body, 1, 1).getBsdxInfoCollectionList1().stream().map(BsdxInfoCollection::getTypeList).toList());
            assertEquals(List.of(149,149), frames(change(body, 1, 2)));
            assertEquals(List.of(40,40), frames(change(body, 2, 0)));
        }
        assertSame(f.child().getSkillList().get(childPrefix + 145), childSkill(f, 145));
        assertSame(f.child().getSkillList().get(childPrefix + 146), childSkill(f, 146));
        assertRecord(sourceFiles.get(mainPath), 3396188, 1828, "ecd5aa0935092d61bf3420ac146be4061c60d72b8fded7dbfd535b689541a724");
        assertRecord(sourceFiles.get(mainPath), 3398016, 1860, "83b7aca42db8125265b43b8e4832a003855f84b1c6025d27e3e1c4e796ec17e9");
        assertRecord(sourceFiles.get(mainPath), 3399876, 1860, "0ac86cccf50c20e430ca1ef503841e6f7b0d30f5f3d15b9d1c1bee6609316547");
        byte[] bodyBytes = sourceFiles.get(resolve(BHE.resolve("waz"), "tama03.waz"));
        assertRecord(bodyBytes, 244596, 9544, "5dd22b26588e462c94f611d898ae86050cdfeefc1b675ecce8be76c23a5c6d6d");
        assertRecord(bodyBytes, 254140, 9544, "9025d0525e4da418f23be0f7ac2cc93bc87b820b428e41dff6506826d4c1435d");
        assertRecord(bodyBytes, 263684, 9496, "5e39cc6df40a444d7ba2eb1eedfb91e2db8f675dd419228d8138f54011b4bb61");
        byte[] childBytes = sourceFiles.get(resolve(BHE.resolve("waz"), "tama04.waz"));
        assertRecord(childBytes, 576699, 4498, "a04c632cc0f2ecaef9b59a26b8819c2036c29ca52b2142f1bdf740d130d536dc");
        assertRecord(childBytes, 581197, 3762, "8db500d3a492fd3af1db099ce48d5e3f81dfc3d9a1d20ffbb4ff343149600a76");
        return f;
    }

    private static void assertRecord(byte[] bytes, int offset, int length, String sha) throws Exception {
        assertEquals(sha, HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(Arrays.copyOfRange(bytes, offset, offset + length))), "complete original BHE record bytes");
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

    private static Path resolve(Path directory, String name) throws Exception {
        try (var paths = Files.list(directory)) {
            var matches = paths.filter(p -> p.getFileName().toString().equalsIgnoreCase(name)).toList();
            assertEquals(1, matches.size(), "one actual source file " + name);
            return matches.get(0);
        }
    }

}
