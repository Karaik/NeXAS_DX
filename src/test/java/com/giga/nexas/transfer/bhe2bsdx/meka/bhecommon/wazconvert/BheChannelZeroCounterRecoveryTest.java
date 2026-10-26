package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventFreeParam;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVal;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class BheChannelZeroCounterRecoveryTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final Function<com.giga.nexas.dto.bhe.waz.Waz, Waz> FOLLOWUP =
            new com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter()::convert;
    private static final Function<com.giga.nexas.dto.bhe.waz.Waz, Waz> TSUKUYOMI =
            new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.waz.BheToBsdxWazConverter()::convert;

    @TempDir
    Path output;

    @Test
    void realFrejaFollowupKeepsThreeIncrementsAndResetInGeneratedWaz() throws Exception {
        assertRealFreja(FOLLOWUP, "freja-followup");
    }

    @Test
    void realFrejaTsukuyomiKeepsThreeIncrementsAndResetInGeneratedWaz() throws Exception {
        assertRealFreja(TSUKUYOMI, "freja-tsukuyomi");
    }

    @Test
    void realMisakiEffectKeepsActiveZeroChannelAndOtherEffectFields() throws Exception {
        Path input = Path.of("src/main/resources/game/bhe/waz/misaki/misaki.waz");
        String hash = sha256(input);
        var source = read(input);
        JsonNode before = JSON.valueToTree(source);
        var original = sourceEffect(source, 12, 2, 1);
        assertEquals("掴む", source.getSkillList().get(12).getSkillNameJapanese());
        assertEquals("EGGE04", source.getSkillList().get(12).getSkillNameEnglish());
        var free = (CEventFreeParam) original.getCeventEffectUnitList().stream()
                .filter(value -> Integer.valueOf(37).equals(value.getUnitSlotNum())).findFirst().orElseThrow().getData();
        assertEquals(List.of(0, 0, 0, 1), sourceValues(activeZero(free)));
        Waz target = FOLLOWUP.apply(source);
        CEventEffect effect = targetEffect(target, 12, 2, 1);

        assertValue(nestedCounter(effect), List.of(0, 0, 0, 1), 0, 0, 0);
        assertEquals(List.of(1, 1, 16, 0, -1, -1, -1, 0, 1, 0, 0), effectValues(effect));
        assertEquals(0, effect.getStartFrame());
        assertEquals(0, effect.getEndFrame());
        assertEquals(48, effect.getSlotNum());
        assertEquals(19, effect.getTypeId());
        Waz parsed = roundTrip(target, "misaki-counter");
        assertValue(nestedCounter(targetEffect(parsed, 12, 2, 1)), List.of(0, 0, 0, 1), 0, 0, null);
        nestedCounter(effect).setInt4(9876);
        assertEquals(before, JSON.valueToTree(source));
        assertEquals(hash, sha256(input));
    }

    @Test
    void nonOneBufferMeansPresenceInBothTopLevelPathsAndNestedEffect() {
        CEventFreeParam free = free();
        free.getUnitList().add(unit(1, 0, null));
        free.getUnitList().add(unit(0, 2, value(3, 4, 5, 6)));
        assertAllPaths(free, List.of(3, 4, 5, 6));
    }

    @Test
    void activeZeroPayloadNeedsNoInactiveSentinel() {
        CEventFreeParam free = free();
        free.getUnitList().add(unit(0, 1, value(0, 0, 0, 0)));
        assertAllPaths(free, List.of(0, 0, 0, 0));
    }

    @Test
    void noActiveZeroKeepsZeroDegradationInsteadOfBorrowingAnotherChannel() {
        CEventFreeParam free = free();
        free.getUnitList().add(unit(3, 2, value(90, 91, 92, 93)));
        assertAllPaths(free, List.of(0, 0, 0, 0));
    }

    @Test
    void allFourChannelsActiveSelectsChannelIdentityRatherThanFirstListItem() {
        CEventFreeParam free = free();
        free.getUnitList().add(unit(3, 1, value(90, 91, 92, 93)));
        free.getUnitList().add(unit(2, 1, value(80, 81, 82, 83)));
        free.getUnitList().add(unit(1, 1, value(70, 71, 72, 73)));
        free.getUnitList().add(unit(0, 1, value(0, 0, 1, 1)));
        assertAllPaths(free, List.of(0, 0, 1, 1));
    }

    @Test
    void activeZeroMissingDataIsRejectedInAllThreePaths() {
        CEventFreeParam free = free();
        free.getUnitList().add(unit(0, 1, null));
        free.getUnitList().add(unit(1, 0, null));
        assertAllReject(free);
    }

    @Test
    void activeZeroWrongPayloadTypeIsRejectedInAllThreePaths() {
        CEventFreeParam free = free();
        free.getUnitList().add(unit(0, 2,
                new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventValRandom(3)));
        free.getUnitList().add(unit(1, 0, null));
        assertAllReject(free);
    }

    private void assertRealFreja(Function<com.giga.nexas.dto.bhe.waz.Waz, Waz> convert, String name) throws Exception {
        Path input = Path.of("src/main/resources/game/bhe/waz/freja/freja.waz");
        String hash = sha256(input);
        var source = read(input);
        JsonNode before = JSON.valueToTree(source);
        var skill = source.getSkillList().get(26);
        assertEquals("武幻剛乱舞", skill.getSkillNameJapanese());
        assertEquals("0001", skill.getSkillNameEnglish());
        for (int phase : List.of(2, 3, 5)) {
            assertEquals(List.of(0, 0, 1, 1), sourceValues(activeZero(sourceCounter(source, 26, phase))));
        }
        assertEquals(List.of(0, 0, 0, 0), sourceValues(activeZero(sourceCounter(source, 26, 4))));
        Waz target = convert.apply(source);

        assertFrejaCounters(target, 35);
        Waz parsed = roundTrip(target, name);
        assertFrejaCounters(parsed, null);
        topCounter(target, 26, 2).setInt4(9876);
        assertEquals(before, JSON.valueToTree(source), "转换和目标编辑不能改变原始payload");
        assertEquals(hash, sha256(input));
    }

    private static void assertFrejaCounters(Waz target, Integer recordedSlot) {
        assertAll(
                () -> assertValue(topCounter(target, 26, 2), List.of(0, 0, 1, 1), 0, 0, recordedSlot),
                () -> assertValue(topCounter(target, 26, 3), List.of(0, 0, 1, 1), 0, 0, recordedSlot),
                () -> assertValue(topCounter(target, 26, 4), List.of(0, 0, 0, 0), 0, 0, recordedSlot),
                () -> assertValue(topCounter(target, 26, 5), List.of(0, 0, 1, 1), 0, 0, recordedSlot));
    }

    private static void assertAllPaths(CEventFreeParam free, List<Integer> expected) {
        JsonNode before = JSON.valueToTree(free);
        assertAll(
                () -> assertValue(topCounter(FOLLOWUP.apply(wrap(free, false)), 0, 1), expected, 7, 13, 35),
                () -> assertValue(topCounter(TSUKUYOMI.apply(wrap(free, false)), 0, 1), expected, 7, 13, 35),
                () -> {
                    CEventEffect effect = targetEffect(FOLLOWUP.apply(wrap(free, true)), 0, 1, 0);
                    assertValue(nestedCounter(effect), expected, 7, 13, 0);
                    assertEquals(17, effect.getStartFrame());
                    assertEquals(23, effect.getEndFrame());
                    assertEquals(List.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11), effectValues(effect));
                });
        assertEquals(before, JSON.valueToTree(free));
    }

    private static void assertAllReject(CEventFreeParam free) {
        JsonNode before = JSON.valueToTree(free);
        assertAll(
                () -> assertThrows(IllegalStateException.class, () -> FOLLOWUP.apply(wrap(free, false))),
                () -> assertThrows(IllegalStateException.class, () -> TSUKUYOMI.apply(wrap(free, false))),
                () -> assertThrows(IllegalStateException.class, () -> FOLLOWUP.apply(wrap(free, true))));
        assertEquals(before, JSON.valueToTree(free));
    }

    private static com.giga.nexas.dto.bhe.waz.Waz wrap(CEventFreeParam free, boolean nested) {
        var waz = new com.giga.nexas.dto.bhe.waz.Waz("counter-test");
        waz.setExtensionName("waz");
        var skill = new com.giga.nexas.dto.bhe.waz.Waz.Skill();
        skill.setPhaseQuantity(1);
        skill.setSkillNameJapanese("カウンタ");
        skill.setSkillNameEnglish("COUNTER_TEST");
        var phase = new com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase();
        SkillUnit slot = new SkillUnit(nested ? 54 : 37, "counter fixture");
        if (nested) {
            var effect = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect(25);
            effect.setStartFrame(17);
            effect.setEndFrame(23);
            effect.setInt1(1); effect.setInt2(2); effect.setInt3(3); effect.setInt4(4);
            effect.setInt5(5); effect.setInt6(6); effect.setInt7(7); effect.setInt8(8);
            effect.setInt9(9); effect.setInt10(10); effect.setInt11(11);
            var unit = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect.CEventEffectUnit();
            unit.setCeventEffectUnitQuantity(37);
            unit.setUnitSlotNum(37);
            unit.setBuffer(1);
            unit.setData(free);
            effect.getCeventEffectUnitList().add(unit);
            slot.getSkillInfoObjectList().add(effect);
        } else {
            slot.getSkillInfoObjectList().add(free);
        }
        phase.getSkillUnitCollection().add(slot);
        skill.getPhasesInfo().add(phase);
        waz.getSkillList().add(skill);
        return waz;
    }

    private static CEventFreeParam free() {
        CEventFreeParam free = new CEventFreeParam(15);
        free.setStartFrame(7);
        free.setEndFrame(13);
        return free;
    }

    private static CEventFreeParam.CEventFreeParamUnit unit(int channel, int buffer,
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject value) {
        var unit = new CEventFreeParam.CEventFreeParamUnit();
        unit.setUnitSlotNum(channel);
        unit.setBuffer(buffer);
        unit.setData(value);
        return unit;
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal value(int a, int b, int c, int d) {
        var value = new com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal(0);
        value.setStartFrame(91);
        value.setEndFrame(96);
        value.setInt1(a); value.setInt2(b); value.setInt3(c); value.setInt4(d);
        return value;
    }

    private Waz roundTrip(Waz target, String name) throws Exception {
        Path file = output.resolve(name + ".waz");
        BsdxBinService service = new BsdxBinService();
        service.generate(file.toString(), target, CHARSET);
        return (Waz) service.parse(file.toString(), CHARSET).getData();
    }

    private static com.giga.nexas.dto.bhe.waz.Waz read(Path file) throws Exception {
        return (com.giga.nexas.dto.bhe.waz.Waz) new BheBinService().parse(file.toString(), CHARSET).getData();
    }

    private static CEventFreeParam sourceCounter(com.giga.nexas.dto.bhe.waz.Waz waz, int skill, int phase) {
        return (CEventFreeParam) waz.getSkillList().get(skill).getPhasesInfo().get(phase - 1)
                .getSkillUnitCollection().stream().filter(value -> Integer.valueOf(37).equals(value.getUnitQuantity()))
                .findFirst().orElseThrow().getSkillInfoObjectList().get(0);
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal activeZero(CEventFreeParam free) {
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal) free.getUnitList().stream()
                .filter(value -> Integer.valueOf(0).equals(value.getUnitSlotNum()) && value.getBuffer() != 0)
                .findFirst().orElseThrow().getData();
    }

    private static CEventVal topCounter(Waz waz, int skill, int phase) {
        return (CEventVal) waz.getSkillList().get(skill).getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(value -> Integer.valueOf(35).equals(value.getUnitQuantity()))
                .findFirst().orElseThrow().getSkillInfoObjectList().get(0);
    }

    private static com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect sourceEffect(
            com.giga.nexas.dto.bhe.waz.Waz waz, int skill, int phase, int ordinal) {
        return (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect) waz.getSkillList().get(skill)
                .getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(value -> Integer.valueOf(54).equals(value.getUnitQuantity()))
                .findFirst().orElseThrow().getSkillInfoObjectList().get(ordinal);
    }

    private static CEventEffect targetEffect(Waz waz, int skill, int phase, int ordinal) {
        return (CEventEffect) waz.getSkillList().get(skill).getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(value -> Integer.valueOf(48).equals(value.getUnitQuantity()))
                .findFirst().orElseThrow().getSkillInfoObjectList().get(ordinal);
    }

    private static CEventVal nestedCounter(CEventEffect effect) {
        var unit = effect.getCeventEffectUnitList().stream().filter(value -> Integer.valueOf(36).equals(value.getUnitSlotNum()))
                .findFirst().orElseThrow();
        assertEquals(1, unit.getBuffer());
        return (CEventVal) unit.getData();
    }

    private static void assertValue(CEventVal actual, List<Integer> expected, int start, int end, Integer recordedSlot) {
        assertEquals(expected, Arrays.asList(actual.getInt1(), actual.getInt2(), actual.getInt3(), actual.getInt4()));
        assertEquals(start, actual.getStartFrame());
        assertEquals(end, actual.getEndFrame());
        assertEquals(0, actual.getTypeId());
        if (recordedSlot != null) assertEquals(recordedSlot, actual.getSlotNum());
    }

    private static List<Integer> sourceValues(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal value) {
        return List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4());
    }

    private static List<Integer> effectValues(CEventEffect effect) {
        return List.of(effect.getInt1(), effect.getInt2(), effect.getInt3(), effect.getInt4(), effect.getInt5(),
                effect.getInt6(), effect.getInt7(), effect.getInt8(), effect.getInt9(), effect.getInt10(), effect.getInt11());
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }
}
