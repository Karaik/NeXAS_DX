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
import com.giga.nexas.io.BinaryWriter;
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

import java.io.ByteArrayOutputStream;
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

/** Real-source protocol regression. The small native-condition model is not game/runtime validation. */
@Execution(ExecutionMode.SAME_THREAD)
class SouCoolGainReturnProtocolTest {
    private static final ObjectMapper JSON = new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String BODY = "DX_SOU_COOL_GAIN_BODY", ICE = "DX_SOU_COOL_GAIN_ICE", COOLING = "DX_SOU_COOL_GAIN_COOLING";
    private static final String BODY_JP = "★蒼：クールゲイン：本体・回収", ICE_JP = "★蒼：クールゲイン：氷・回収", COOLING_JP = "★蒼：クールゲイン：熱冷却・専用";
    private static final Path BHE = Path.of("src/main/resources/game/bhe"), DX = Path.of("src/main/resources/game/bsdx");
    private static final String CHARSET = "windows-31j";
    @TempDir Path output;

    @Test void threeCallersOnlyChangeTheirReferenceAndKeepTheirOwnReleaseFrames() throws Exception {
        Fixture f = fixture(); Waz expected = copy(f.main(), Waz.class); int tail = f.host().getSkillList().size();
        Map<String,String> preserved = preserved(f);
        for (int level = 1; level <= 3; level++) reference(effect(skill(expected, "C_GAIN0" + level), 0, 0)).setWazSequenceNo(tail);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(tail, index(f.host(), BODY)); assertEquals(tail + 1, index(f.host(), ICE)); assertEquals(tail + 2, index(f.host(), COOLING));
        assertEquals(tail + 3, f.host().getSkillList().size());
        assertEquals(payload(expected), payload(f.main()), "only three main carrier sequence numbers change");
        for (int level = 1; level <= 3; level++) {
            CEventEffect caller = effect(skill(f.main(), "C_GAIN0" + level), 0, 0);
            assertEquals(List.of(33 - 6 * level, 33 - 6 * level), frames(caller));
            assertEquals(f.hostGroup(), reference(caller).getWazFileNo());
        }
        assertEquals(preserved, preserved(f)); assertSourceFiles(f);
    }

    @Test void privateBodyKeepsAccumulatedChargeAndOriginalDecrementAndTwentyFrameFinish() throws Exception {
        Fixture f = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(f.context()); Waz.Skill body = skill(f.host(), BODY);
        assertEquals(List.of(0,0,1,7), four(val(body, 0, 0)));
        assertEquals(1, unit(body, 1, 35).getSkillInfoObjectList().size(), "channel3-only zero downgrade must not reset charge");
        CEventVal decrement = val(body, 1, 0);
        assertEquals(List.of(1,1), frames(decrement)); assertEquals(List.of(0,0,1,-1), four(decrement));
        assertEquals(List.of(List.of(1,1,-1,1,-2)), loops(unit(body, 1, 35)));
        for (int chargedFrames : List.of(1, 2, 25, 85, 1000)) {
            int budget = 0;
            for (int tick = 0; tick < chargedFrames; tick++) budget = scalarPoint(val(body, 0, 0), budget);
            assertEquals(chargedFrames * 7, budget);
            // No phase1 event is active at 0f; phase entry therefore preserves this budget.
            assertTrue(unit(body, 1, 35).getSkillInfoObjectList().stream().noneMatch(v -> v.getStartFrame() <= 0 && v.getEndFrame() >= 0));
            for (int tick = 0; tick < chargedFrames * 7; tick++) budget = scalarPoint(decrement, budget);
            assertEquals(0, budget, "original charge, not a replacement fixed timer");
        }
        assertEquals(List.of(0,0,0,0), four(val(body, 2, 0)));
        assertEquals(List.of(20,20), frames(change(body, 2, 0)));
        assertEquals(List.of(0,0), change(body, 2, 0).getBsdxInfoCollectionList2().get(0).getTypeList());
        assertSourceFiles(f);
    }

    @Test void iceReturnsOnParentTurnThenBudgetExhaustionAndNullParentHasFirstPriority() throws Exception {
        Fixture f = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(f.context()); Waz.Skill ice = skill(f.host(), ICE);
        assertEquals(5, nextIcePhase(ice, 5, true, 0, 0, 70));
        assertEquals(6, nextIcePhase(ice, 5, true, 1, 0, 70));
        assertEquals(6, nextIcePhase(ice, 6, true, 1, 1, 1));
        assertEquals(7, nextIcePhase(ice, 6, true, 1, 1, 0));
        assertEquals(7, nextIcePhase(ice, 6, true, 2, 1, 0), "HP/interruption exit reaches phase2's existing zero write");
        // 60E130 refreshes savedParentEntry on every ice phase change, including entry into phase5.
        assertEquals(5, nextIcePhase(ice, 5, true, 1, 1, 70), "late phase1 observer waits for the next parent change");
        assertEquals(5, nextIcePhase(ice, 5, true, 2, 2, 0), "late phase2 observer does not invent a parent change");
        assertEquals(6, nextIcePhase(ice, 5, true, 2, 1, 0), "late phase1 observer sees the subsequent parent phase2");
        assertEquals(7, nextIcePhase(ice, 6, true, 2, 2, 0), "entering phase6 refreshes the snapshot and zero budget ends return");
        assertEquals(7, nextIcePhase(ice, 5, false, -1, 2, 0), "late phase2 observer enters fade when its parent disappears");
        for (int phase : List.of(5,6)) {
            for (int savedParentEntry : List.of(0,1,2))
                assertEquals(7, nextIcePhase(ice, phase, false, -1, savedParentEntry, 0), "missing-parent fallback has priority over PARENT_TURN");
            BsdxInfoCollection first = change(ice, phase, 0).getBsdxInfoCollectionList1().get(0);
            assertEquals(List.of(2,1,0), first.getTypeList()); assertEquals(List.of(2,0), first.getIntList3());
        }
        assertEquals(List.of(15,15), frames(change(ice, 7, 0)));
        assertEquals(List.of(0,2), change(ice, 7, 0).getBsdxInfoCollectionList2().get(0).getTypeList());
    }

    @Test void completePrivateGraphsHaveOnlyApprovedDeltasAndEntirePublicPrefixIsPreserved() throws Exception {
        Fixture f = fixture(); int count = f.host().getSkillList().size(); JsonNode prefix = payload(f.host().getSkillList());
        Waz.Skill body = copy(skill(f.host(), "0344"), Waz.Skill.class), ice = copy(skill(f.host(), "0345"), Waz.Skill.class), cooling = copy(skill(f.host(), "0346"), Waz.Skill.class);
        JsonNode groups = payload(f.context().getBaseline().getWazaGroupGrp()); Map<String,String> before = preserved(f);
        byte[] prefixBytes = binary(f.host(), output.resolve("prefix-before.waz"));
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(count + 3, f.host().getSkillList().size());
        assertEquals(prefix, payload(f.host().getSkillList().subList(0, count)));
        assertArrayEquals(prefixBytes, Arrays.copyOf(binary(f.host(), output.resolve("prefix-after.waz")), prefixBytes.length));
        assertEquals(payload(expectedBody(body, index(f.host(), ICE), index(f.host(), COOLING))), payload(skill(f.host(), BODY)));
        assertEquals(payload(expectedIce(ice)), payload(skill(f.host(), ICE)));
        assertEquals(payload(expectedCooling(cooling, f.heatGrammarDonor())), payload(skill(f.host(), COOLING)));
        assertIndependentMutableGraphs(List.of(skill(f.host(), "0344"), skill(f.host(), "0345"), skill(f.host(), "0346"), f.heatGrammarDonor()),
                List.of(skill(f.host(), BODY), skill(f.host(), ICE), skill(f.host(), COOLING)));
        ((ObjectNode) groups.path("wazaList").get(f.hostGroup())).put("param", count + 3);
        assertEquals(groups, payload(f.context().getBaseline().getWazaGroupGrp()));
        assertEquals(before, preserved(f)); assertSourceFiles(f);
    }

    @Test void dynamicTailAndVerifiedRepeatPreserveInstalledObjectsAndLaterPrivateAppends() throws Exception {
        Fixture f = fixture(); appendUnrelated(f, "BEFORE_COOL_GAIN"); int tail = f.host().getSkillList().size();
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(tail, index(f.host(), BODY)); assertEquals(tail + 1, index(f.host(), ICE)); assertEquals(tail + 2, index(f.host(), COOLING));
        assertEquals(tail + 3, f.host().getSkillList().size());
        appendUnrelated(f, "AFTER_COOL_GAIN");
        List<Waz.Skill> main = f.main().getSkillList(), host = f.host().getSkillList();
        Waz.Skill body = skill(f.host(), BODY), ice = skill(f.host(), ICE), cooling = skill(f.host(), COOLING); String before = snapshot(f);
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertSame(main, f.main().getSkillList()); assertSame(host, f.host().getSkillList());
        assertSame(body, skill(f.host(), BODY)); assertSame(ice, skill(f.host(), ICE)); assertSame(cooling, skill(f.host(), COOLING)); assertEquals(before, snapshot(f));
    }

    @Test void rawSourceAndPublicTargetDriftRejectBeforeAnyCoolGainPublication() throws Exception {
        for (Consumer<Fixture> corruption : List.<Consumer<Fixture>>of(
                f -> f.raw().getWazByFileName().get("sou.waz").getSkillList().get(449).setSkillNameEnglish("C_GAIN03_DRIFT"),
                f -> f.raw().getCommonProjectileWazByFileName().get("tama05.waz").getSkillList().get(375).getSkillSuffixList().get(2).setInt1(1),
                f -> val(skill(f.host(), "0344"), 0, 0).setInt4(8),
                f -> val(skill(f.host(), "0344"), 1, 0).setInt4(1),
                f -> change(skill(f.host(), "0345"), 5, 0).getBsdxInfoCollectionList1().get(0).setParamList(new ArrayList<>(List.of(2))),
                f -> change(skill(f.host(), "0345"), 7, 0).setStartFrame(14),
                f -> change(skill(f.host(), "0346"), 0, 0).setStartFrame(1),
                f -> sourceCoolingHit(f).setInt10(-99))) {
            Fixture f = fixture(); corruption.accept(f); assertRejected(f);
        }
    }

    @Test void partialInstallDuplicateIdentityAndTamperedPrivateBudgetCannotBeSilentlyRepaired() throws Exception {
        Fixture partial = fixture(); reference(effect(skill(partial.main(), "C_GAIN03"), 0, 0)).setWazSequenceNo(partial.host().getSkillList().size()); assertRejected(partial);
        Fixture duplicate = fixture(); duplicate.main().getSkillList().add(copy(skill(duplicate.main(), "C_GAIN03"), Waz.Skill.class));
        duplicate.context().getBaseline().getWazaGroupGrp().getWazaList().get(duplicate.context().getGrpAppendPlan().getWazaGroupIndex()).setParam(duplicate.main().getSkillList().size());
        duplicate.context().getGrpAppendPlan().getTargetWazSkillCountByGroupIndex().put(duplicate.context().getGrpAppendPlan().getWazaGroupIndex(), duplicate.main().getSkillList().size()); assertRejected(duplicate);
        Fixture installed = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(installed.context());
        val(skill(installed.host(), BODY), 1, 0).setInt4(-2); assertRejected(installed);
        Fixture missing = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(missing.context());
        skill(missing.host(), ICE).setSkillNameEnglish("INCOMPLETE_ICE"); assertRejected(missing);
    }

    @Test void mainHostAndGroupSurviveTwoBinaryPassesAndParsedIdempotence() throws Exception {
        Fixture f = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(BODY_JP, skill(f.host(), BODY).getSkillNameJapanese()); assertEquals(ICE_JP, skill(f.host(), ICE).getSkillNameJapanese());
        assertEquals(COOLING_JP, skill(f.host(), COOLING).getSkillNameJapanese());
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

    @Test void privateCoolingUsesOriginalBsdxHeatGrammarAndTheOriginalHundredRawAmountAndCadence() throws Exception {
        Fixture f = fixture(); Waz.Skill donor = f.heatGrammarDonor();
        assertEquals("発光爆風（小）", donor.getSkillNameJapanese()); assertEquals("0004", donor.getSkillNameEnglish());
        CEventVal vanilla = heat(donor);
        assertEquals(0, vanilla.getTypeId()); assertEquals(List.of(0,0), frames(vanilla)); assertEquals(List.of(0,0,0,-30), four(vanilla));
        assertEquals(1, unit(donor, 0, 40).getSkillInfoObjectList().size()); assertTrue(unit(donor, 0, 40).getSkillInfoUnknownList().isEmpty());
        byte[] bombBytes = f.sourceFiles().get(resolve(DX.resolve("waz"), "Bomb.waz"));
        byte[] originalHeatRecord = Arrays.copyOfRange(bombBytes, 76993, 77017);
        assertArrayEquals(ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN).putInt(0).putInt(0).putInt(0).putInt(0).putInt(0).putInt(-30).array(), originalHeatRecord);
        assertArrayEquals(originalHeatRecord, eventBytes(vanilla), "real vanilla Bomb23 slot40 record proves the 0f absolute-value grammar");
        assertEquals(-100, sourceCoolingHit(f).getInt10(), "original BHE hit transfers minus 100 raw heat to its victim");
        assertEquals(6, sourceCoolingHit(f).getShort1().intValue());
        Waz.Skill publicCooling = skill(f.host(), "0346");
        assertFalse(hasHeat(publicCooling));
        SouCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Waz.Skill body = skill(f.host(), BODY), cooling = skill(f.host(), COOLING);
        CEventVal actual = heat(cooling), expected = copy(vanilla, CEventVal.class); expected.setInt4(sourceCoolingHit(f).getInt10());
        assertEquals(payload(expected), payload(actual)); assertEquals(1, unit(cooling, 0, 40).getSkillInfoObjectList().size());
        assertTrue(unit(cooling, 0, 40).getSkillInfoUnknownList().isEmpty(), "one pulse per cooling entity, no repeating heat FF");
        assertEquals(payload(unit(publicCooling, 0, 41)), payload(unit(cooling, 0, 41)), "the existing self zero-damage Hit remains intact");
        assertEquals(payload(unit(publicCooling, 0, 71)), payload(unit(cooling, 0, 71)), "the original 0f exit remains intact");
        CEventEffect pulse = effect(body, 1, 0), publicPulse = effect(skill(f.host(), "0344"), 1, 0);
        CEventEffect onlyRebound = copy(publicPulse, CEventEffect.class); reference(onlyRebound).setWazSequenceNo(index(f.host(), COOLING));
        assertEquals(payload(onlyRebound), payload(pulse), "only the P1 cooling reference changes; original spawn packet remains");
        assertEquals(f.hostGroup(), reference(pulse).getWazFileNo()); assertEquals(index(f.host(), COOLING), reference(pulse).getWazSequenceNo());
        assertEquals(List.of(0,107), frames(pulse)); assertEquals(List.of(List.of(0,107,-1,1,-2)), loops(unit(body, 1, 48)));
        CEventValRandom amount = inner(pulse, 4), interval = inner(pulse, 6);
        assertEquals(List.of(0,1,0,0,1,0), six(amount)); assertEquals(List.of(0,3,0,0,3,0), six(interval));
        assertFalse(hasHeat(body)); assertFalse(hasHeat(skill(f.host(), ICE))); assertFalse(hasHeat(publicCooling));
        for (int rawHeat : List.of(0,1,50,99,100,101,900,1000,1080,8000)) {
            int once = applyNativeHeatDelta(rawHeat, actual.getInt4(), 0);
            assertEquals(Math.max(0, rawHeat - 100), once);
            assertEquals(once, applyNativeHeatDelta(once, actual.getInt4(), actual.getInt4()), "unchanged cumulative slot40 value must not deduct a second time");
            int accumulated = rawHeat;
            for (int entity = 0; entity < 10; entity++) accumulated = applyNativeHeatDelta(accumulated, actual.getInt4(), 0);
            assertEquals(Math.max(0, rawHeat - 1000), accumulated, "each new entity has an independent zero previous contribution");
        }
        // 610300/621780 apply current minus previous raw contribution with floor0; 611820 handles heat before the 0f exit.
        // This adaptation deducts on cooling-entity activation, whereas BHE deducts after its self Hit is confirmed.
        // The amount/cadence checks and arithmetic model do not claim per-hit equivalence or game/runtime validation.
        assertSourceFiles(f);
    }

    @Test void oldTwoMemberFamilyAndMissingOrTamperedCoolingRejectWithoutMigration() throws Exception {
        Fixture oldPair = fixture(); int first = oldPair.host().getSkillList().size();
        oldPair.host().getSkillList().add(expectedBody(skill(oldPair.host(), "0344"), first + 1, index(oldPair.host(), "0346")));
        oldPair.host().getSkillList().add(expectedIce(skill(oldPair.host(), "0345")));
        for (int level = 1; level <= 3; level++) reference(effect(skill(oldPair.main(), "C_GAIN0" + level), 0, 0)).setWazSequenceNo(first);
        oldPair.context().getBaseline().getWazaGroupGrp().getWazaList().get(oldPair.hostGroup()).setParam(first + 2);
        assertRejected(oldPair);
        Fixture missing = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(missing.context());
        skill(missing.host(), COOLING).setSkillNameEnglish("INCOMPLETE_COOLING"); assertRejected(missing);
        Fixture alteredAmount = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(alteredAmount.context());
        heat(skill(alteredAmount.host(), COOLING)).setInt4(-1000); assertRejected(alteredAmount);
        Fixture shiftedEvent = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(shiftedEvent.context());
        heat(skill(shiftedEvent.host(), COOLING)).setStartFrame(1); assertRejected(shiftedEvent);
        Fixture oldReference = fixture(); SouCustomizer.INSTANCE.customizeBeforeWrite(oldReference.context());
        reference(effect(skill(oldReference.host(), BODY), 1, 0)).setWazSequenceNo(index(oldReference.host(), "0346")); assertRejected(oldReference);
    }

    private static Waz.Skill expectedBody(Waz.Skill prototype, int iceIndex, int coolingIndex) {
        Waz.Skill expected = copy(prototype, Waz.Skill.class); expected.setSkillNameJapanese(BODY_JP); expected.setSkillNameEnglish(BODY);
        unit(expected, 1, 35).getSkillInfoObjectList().remove(0);
        assertEquals(4, unit(expected, 0, 48).getSkillInfoObjectList().size());
        for (int i = 0; i < 4; i++) reference(effect(expected, 0, i)).setWazSequenceNo(iceIndex);
        reference(effect(expected, 1, 0)).setWazSequenceNo(coolingIndex);
        return expected;
    }

    private static Waz.Skill expectedCooling(Waz.Skill prototype, Waz.Skill vanillaDonor) {
        Waz.Skill expected = copy(prototype, Waz.Skill.class); expected.setSkillNameJapanese(COOLING_JP); expected.setSkillNameEnglish(COOLING);
        assertTrue(expected.getPhasesInfo().get(0).getSkillUnitCollection().stream().noneMatch(u -> Integer.valueOf(40).equals(u.getUnitQuantity())));
        SkillUnit heat = copy(unit(vanillaDonor, 0, 40), SkillUnit.class);
        assertInstanceOf(CEventVal.class, heat.getSkillInfoObjectList().get(0)).setInt4(-100);
        expected.getPhasesInfo().get(0).getSkillUnitCollection().add(heat);
        expected.getPhasesInfo().get(0).getSkillUnitCollection().sort(Comparator.comparingInt(SkillUnit::getUnitQuantity));
        return expected;
    }

    private static CEventVal heat(Waz.Skill skill) { return assertInstanceOf(CEventVal.class, unit(skill, 0, 40).getSkillInfoObjectList().get(0)); }
    private static boolean hasHeat(Waz.Skill skill) { return skill.getPhasesInfo().stream().flatMap(p -> p.getSkillUnitCollection().stream()).anyMatch(u -> Integer.valueOf(40).equals(u.getUnitQuantity()) && (!u.getSkillInfoObjectList().isEmpty() || !u.getSkillInfoUnknownList().isEmpty())); }
    private static CEventValRandom inner(CEventEffect event, int slot) { return event.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum())).map(u -> assertInstanceOf(CEventValRandom.class, u.getData())).findFirst().orElseThrow(); }
    private static List<Integer> six(CEventValRandom value) { return List.of(value.getInt1(),value.getInt2(),value.getInt3(),value.getInt4(),value.getInt5(),value.getInt6()); }
    private static byte[] eventBytes(CEventVal event) throws Exception { ByteArrayOutputStream out = new ByteArrayOutputStream(); try (BinaryWriter writer = new BinaryWriter(out, CHARSET)) { event.writeInfo(writer); } return out.toByteArray(); }
    private static int applyNativeHeatDelta(int rootHeat, int currentContribution, int previousContribution) { return Math.max(0, Math.addExact(rootHeat, Math.subtractExact(currentContribution, previousContribution))); }
    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventHit sourceCoolingHit(Fixture f) {
        var unit = f.raw().getCommonProjectileWazByFileName().get("tama05.waz").getSkillList().get(377).getPhasesInfo().get(0).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(46).equals(u.getUnitQuantity())).findFirst().orElseThrow();
        assertEquals(1, unit.getSkillInfoObjectList().size());
        return assertInstanceOf(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventHit.class, unit.getSkillInfoObjectList().get(0));
    }

    private static Waz.Skill expectedIce(Waz.Skill prototype) {
        Waz.Skill expected = copy(prototype, Waz.Skill.class); expected.setSkillNameJapanese(ICE_JP); expected.setSkillNameEnglish(ICE);
        for (int phase : List.of(5,6)) {
            CEventChange signal = change(expected, phase, 0), absent = change(expected, phase, 1);
            BsdxInfoCollection condition = signal.getBsdxInfoCollectionList1().get(0);
            condition.setTypeList(new ArrayList<>(phase == 5 ? List.of(2,8,7) : List.of(2,0,0,2)));
            condition.setParamList(new ArrayList<>(phase == 5 ? List.of() : List.of(0)));
            condition.setIntList3(new ArrayList<>(phase == 5 ? List.of(0,0) : List.of(2,0)));
            absent.getBsdxInfoCollectionList1().get(0).setIntList3(new ArrayList<>(List.of(2,0)));
            unit(expected, phase, 71).setSkillInfoObjectList(new ArrayList<>(List.of(absent, signal)));
        }
        return expected;
    }

    // Independent evaluation of only the approved predicates in 66B6E0/66B230/66EB40; no native game calls.
    private static int nextIcePhase(Waz.Skill ice, int phase, boolean parentPresent, int currentParentEntry, int savedParentEntry, int budget) {
        for (SkillInfoObject object : unit(ice, phase, 71).getSkillInfoObjectList()) {
            CEventChange event = assertInstanceOf(CEventChange.class, object); assertEquals(1, event.getFlag());
            BsdxInfoCollection c = event.getBsdxInfoCollectionList1().get(0); assertEquals(0, c.getInt2()); boolean matches;
            if (c.getTypeList().equals(List.of(2,1,0))) matches = c.getIntList3().equals(List.of(2,0)) && !parentPresent;
            else if (c.getTypeList().equals(List.of(2,8,7))) {
                assertEquals(List.of(0,0), c.getIntList3()); matches = !parentPresent || currentParentEntry != savedParentEntry;
            } else if (c.getTypeList().equals(List.of(2,0,0,2))) {
                assertEquals(List.of(2,0), c.getIntList3()); assertEquals(List.of(0), c.getParamList()); matches = parentPresent && budget <= 0;
            } else throw new AssertionError("Unexpected private ice predicate: " + c.getTypeList());
            if (matches) { var target = event.getBsdxInfoCollectionList2().get(0); return target.getTypeList().equals(List.of(0,0)) ? phase + 1 : target.getParamList().get(0); }
        }
        return phase;
    }

    private static int scalarPoint(CEventVal value, int before) {
        return switch (value.getInt3()) { case 0 -> value.getInt4(); case 1 -> Math.addExact(before, value.getInt4()); default -> throw new AssertionError("Unreviewed scalar operation"); };
    }

    private static void appendUnrelated(Fixture f, String name) { Waz.Skill extra = copy(skill(f.host(), "0346"), Waz.Skill.class); extra.setSkillNameJapanese(name); extra.setSkillNameEnglish(name); f.host().getSkillList().add(extra); f.context().getBaseline().getWazaGroupGrp().getWazaList().get(f.hostGroup()).setParam(f.host().getSkillList().size()); }
    private static int index(Waz waz, String name) { List<Integer> matches = new ArrayList<>(); for (int i = 0; i < waz.getSkillList().size(); i++) if (waz.getSkillList().get(i) != null && name.equals(waz.getSkillList().get(i).getSkillNameEnglish())) matches.add(i); assertEquals(1, matches.size(), "unique skill " + name); return matches.get(0); }
    private static Waz.Skill skill(Waz waz, String name) { return waz.getSkillList().get(index(waz, name)); }
    private static SkillUnit unit(Waz.Skill s, int phase, int slot) { return s.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> u.getUnitQuantity() == slot).findFirst().orElseThrow(); }
    private static CEventEffect effect(Waz.Skill s, int phase, int ordinal) { return assertInstanceOf(CEventEffect.class, unit(s, phase, 48).getSkillInfoObjectList().get(ordinal)); }
    private static CEventChange change(Waz.Skill s, int phase, int ordinal) { return assertInstanceOf(CEventChange.class, unit(s, phase, 71).getSkillInfoObjectList().get(ordinal)); }
    private static CEventVal val(Waz.Skill s, int phase, int ordinal) { return assertInstanceOf(CEventVal.class, unit(s, phase, 35).getSkillInfoObjectList().get(ordinal)); }
    private static CEventWazaSelect reference(CEventEffect e) { return e.getCeventEffectUnitList().stream().filter(u -> u.getUnitSlotNum() == 3).map(u -> assertInstanceOf(CEventWazaSelect.class, u.getData())).findFirst().orElseThrow(); }
    private static List<Integer> four(CEventVal v) { return List.of(v.getInt1(),v.getInt2(),v.getInt3(),v.getInt4()); }
    private static List<Integer> frames(SkillInfoObject e) { return List.of(e.getStartFrame(),e.getEndFrame()); }
    private static List<List<Integer>> loops(SkillUnit u) { return u.getSkillInfoUnknownList().stream().map(v -> List.of(v.getStartFrame(),v.getEndFrame(),v.getInt1(),v.getInt2(),v.getInt3())).toList(); }
    private static byte[] binary(Waz waz, Path path) throws Exception { new BsdxBinService().generate(path.toString(), waz, CHARSET); return Files.readAllBytes(path); }
    private static String digest(Object value) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(JSON.writeValueAsBytes(payload(value)))); }
    private static JsonNode payload(Object value) { JsonNode n = JSON.valueToTree(value); strip(n); return n; }
    private static void strip(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset","length","slotNum","unitDescription","description","fileName","extensionName")); n.forEach(SouCoolGainReturnProtocolTest::strip); }
    private static <T> T copy(T value, Class<T> type) { try { return JSON.readValue(JSON.writeValueAsBytes(value), type); } catch (Exception e) { throw new IllegalStateException(e); } }
    private static Waz headerReadback(Waz value) { Waz expected = copy(value,Waz.class); for (Waz.Skill s : expected.getSkillList()) if (s.getPhaseQuantity() == 0 && s.getPhasesInfo().isEmpty() && s.getSkillSuffixList().isEmpty() && !s.isEmpty() && (s.getSkillNameJapanese() == null || s.getSkillNameJapanese().isEmpty()) && (s.getSkillNameEnglish() == null || s.getSkillNameEnglish().isEmpty())) { if (s.getSkillNameJapanese() == null) s.setSkillNameJapanese(""); if (s.getSkillNameEnglish() == null) s.setSkillNameEnglish(""); } return expected; }
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

    private record Fixture(FollowupMekaContext context, TsukuyomiRawSourceBundle raw, String hostName, int hostGroup, Waz.Skill heatGrammarDonor, Map<Path,byte[]> sourceFiles) {
        Waz main() { return context.getReboundWaz(); }
        Waz host() { return context.getBaseline().getWazByFileName().entrySet().stream().filter(e -> hostName.equalsIgnoreCase(e.getKey())).findFirst().orElseThrow().getValue(); }
    }

    private Fixture fixture() throws Exception {
        BheBinService bhe = new BheBinService();
        Path mekPath = BHE.resolve("mek/sou/sou.mek"), mainPath = BHE.resolve("waz/sou/sou.waz");
        Map<Path, byte[]> sourceFiles = new LinkedHashMap<>();
        for (Path p : List.of(mekPath, mainPath, resolve(BHE.resolve("waz"), "tama05.waz"), resolve(DX.resolve("waz"), "Tama05.waz"), resolve(DX.resolve("waz"), "Bomb.waz")))
            sourceFiles.put(p, Files.readAllBytes(p));
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) bhe.parse(mekPath.toString(), CHARSET).getData();
        var rawMain = (com.giga.nexas.dto.bhe.waz.Waz) bhe.parse(mainPath.toString(), CHARSET).getData();
        TsukuyomiRawSourceBundle raw = loadCommonSource(bhe);
        raw.setTsukuyomiMek(rawMek); raw.getWazByFileName().put("sou.waz", rawMain);
        TsukuyomiBsdxBaselineBundle baseline = loadBaseline();
        Waz vanillaBomb = baseline.getWazByFileName().entrySet().stream().filter(e -> e.getKey().equalsIgnoreCase("Bomb.waz")).findFirst().orElseThrow().getValue();
        Waz.Skill heatGrammarDonor = copy(vanillaBomb.getSkillList().get(23), Waz.Skill.class);
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
        new BheInfoCollectionObjectGraphRewriter().rewrite(main, "SouCoolGainReturnProtocolTest main");
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
        // Existing repairs are isolated; Cool Gain first runs through the public Customizer in each test.
        SouSkillGlitchFixer.INSTANCE.fix(context);
        SouHumanCannonCounterFixer.fix(context);
        SouCursedBladeSingleTargetFixer.fix(context);
        SouSnowMoonSingleTargetFixer.fix(context);
        SouFlareSeekerSingleTargetFixer.fix(context);
        SouBoostProcterDashContinuationFixer.fix(context);
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf("tama05.waz");
        String hostName = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        int hostGroup = plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        Fixture f = new Fixture(context, raw, hostName, hostGroup, heatGrammarDonor, sourceFiles);
        assertEquals("C_GAIN01", rawMain.getSkillList().get(447).getSkillNameEnglish());
        assertEquals("C_GAIN02", rawMain.getSkillList().get(448).getSkillNameEnglish());
        assertEquals("C_GAIN03", rawMain.getSkillList().get(449).getSkillNameEnglish());
        assertEquals(3, skill(f.host(), "0344").getPhaseQuantity());
        assertEquals(8, skill(f.host(), "0345").getPhaseQuantity());
        assertEquals(1, skill(f.host(), "0346").getPhaseQuantity());
        assertEquals(List.of(0,0,0,0), four(val(skill(f.host(), "0344"), 1, 0)));
        assertEquals(List.of(1), change(skill(f.host(), "0345"), 5, 0).getBsdxInfoCollectionList1().get(0).getParamList());
        assertEquals(List.of(2), change(skill(f.host(), "0345"), 6, 0).getBsdxInfoCollectionList1().get(0).getParamList());
        assertRecord(sourceFiles.get(mainPath), 3509989, 2749, "ffcbfed245bdcfceb3ea465a5ca6cec567259fa29a39a9f0f69169706e677c90");
        assertRecord(sourceFiles.get(mainPath), 3512738, 2749, "818ec68875675e22327f272b8af7b6e4b127bcc803737d34b3960ecc505352de");
        assertRecord(sourceFiles.get(mainPath), 3515487, 2725, "7f82a99970ab1fe6aaf24fad60b556691af3852cf71aa8c4413155c2f13c09d1");
        byte[] tamaBytes = sourceFiles.get(resolve(BHE.resolve("waz"), "tama05.waz"));
        assertRecord(tamaBytes, 1885685, 11484, "98ac7ed2e7fd20abdf9ac198890a27df0903054c2346da578b5279e20e1735a7");
        assertRecord(tamaBytes, 1897169, 12498, "5f6e3837b825abff9fcdb719087d5b221e02dcf9695b67e309cd0198128234e0");
        assertRecord(tamaBytes, 1909667, 1350, "f5849d84b11d3eaba2d0143d2440b8922d2ce818be67b3c6f3cec6fcf57fb26c");
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
