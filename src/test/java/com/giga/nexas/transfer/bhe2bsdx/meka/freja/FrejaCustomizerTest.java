package com.giga.nexas.transfer.bhe2bsdx.meka.freja;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class FrejaCustomizerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";

    @TempDir
    Path output;

    @Test
    void realSpearImprovesBothBranchesAndPreservesOtherCombatData() throws Exception {
        Fixture fixture = fixture();
        int originalSkillCount = fixture.waz().getSkillList().size();
        Waz.Skill spear = skill(fixture.waz(), "スピア", "0002");
        CEventHit first = event(spear, 3, 41, CEventHit.class, 3, 10);
        CEventHit second = event(spear, 5, 41, CEventHit.class, 16, 24);
        JsonNode firstCombat = combatExceptPower(first);
        JsonNode secondCombat = combatExceptPower(second);
        JsonNode recovery = JSON.valueToTree(event(spear, 3, 18, CEventValRandom.class, 9, 19));
        List<JsonNode> otherSkills = fixture.waz().getSkillList().stream()
                .filter(value -> value != spear).map(value -> JSON.<JsonNode>valueToTree(value)).toList();
        JsonNode sourceMek = JSON.valueToTree(fixture.sourceMek());
        JsonNode sourceWaz = JSON.valueToTree(fixture.sourceWaz());
        CEventValRandom numericDecoy = new CEventValRandom(3);
        numericDecoy.setStartFrame(1);
        numericDecoy.setEndFrame(8);
        numericDecoy.setInt1(0);
        numericDecoy.setInt2(32);
        numericDecoy.setInt3(0);
        numericDecoy.setInt4(0);
        numericDecoy.setInt5(16);
        numericDecoy.setInt6(0);
        SkillUnit decoyUnit = new SkillUnit(19, "非速度槽的数值负对照");
        decoyUnit.getSkillInfoObjectList().add(numericDecoy);
        spear.getPhasesInfo().get(2).getSkillUnitCollection().add(decoyUnit);
        JsonNode decoy = JSON.valueToTree(numericDecoy);

        FrejaCustomizer.INSTANCE.customizeBeforeWrite(context(fixture));

        assertSpear(fixture.mek(), fixture.waz());
        assertEquals(firstCombat, combatExceptPower(first), "击退、补正和命中计数必须保留");
        assertEquals(secondCombat, combatExceptPower(second));
        assertEquals(recovery, JSON.valueToTree(event(spear, 3, 18, CEventValRandom.class, 9, 19)));
        assertEquals(decoy, JSON.valueToTree(numericDecoy), "同数值的非速度槽事件不能被改写");
        assertEquals(originalSkillCount + 6, fixture.waz().getSkillList().size());
        assertEquals(otherSkills, fixture.waz().getSkillList().stream().limit(originalSkillCount)
                .filter(value -> value != spear).map(value -> JSON.<JsonNode>valueToTree(value)).toList());
        assertEquals(sourceMek, JSON.valueToTree(fixture.sourceMek()));
        assertEquals(sourceWaz, JSON.valueToTree(fixture.sourceWaz()), "不能通过共享列表改坏BHE源对象");
    }

    @Test
    void realGeneratedMekAndWazRetainTheReworkAndCanBeAppliedTwice() throws Exception {
        Fixture fixture = fixture();
        FollowupMekaContext context = context(fixture);
        FrejaCustomizer.INSTANCE.customizeBeforeWrite(context);
        assertSpear(fixture.mek(), fixture.waz());
        JsonNode onceMek = JSON.valueToTree(fixture.mek());
        JsonNode onceWaz = JSON.valueToTree(fixture.waz());

        FrejaCustomizer.INSTANCE.customizeBeforeWrite(context);

        assertEquals(onceMek, JSON.valueToTree(fixture.mek()));
        assertEquals(onceWaz, JSON.valueToTree(fixture.waz()));
        BsdxBinService service = new BsdxBinService();
        Path mekFile = output.resolve("freja.mek");
        Path wazFile = output.resolve("freja.waz");
        service.generate(mekFile.toString(), fixture.mek(), CHARSET);
        service.generate(wazFile.toString(), fixture.waz(), CHARSET);
        Mek parsedMek = (Mek) service.parse(mekFile.toString(), CHARSET).getData();
        Waz parsedWaz = (Waz) service.parse(wazFile.toString(), CHARSET).getData();
        assertSpear(parsedMek, parsedWaz);
        assertEquals(fixture.mek().getMekBasicInfo().getMekDescription(), parsedMek.getMekBasicInfo().getMekDescription());
        assertLosslessJapanese(parsedMek.getMekBasicInfo().getMekDescription());
        assertLosslessJapanese(weapon(parsedMek).getWeaponDescription());
    }

    @Test
    void changedSecondSpeedRefusesTheWholeBatchBeforeAnyWrite() throws Exception {
        Fixture fixture = fixture();
        event(skill(fixture.waz(), "スピア", "0002"), 5, 18, CEventValRandom.class, 12, 15).setInt5(99);
        JsonNode beforeMek = JSON.valueToTree(fixture.mek());
        JsonNode beforeWaz = JSON.valueToTree(fixture.waz());

        assertThrows(IllegalStateException.class,
                () -> FrejaCustomizer.INSTANCE.customizeBeforeWrite(context(fixture)));

        assertEquals(beforeMek, JSON.valueToTree(fixture.mek()));
        assertEquals(beforeWaz, JSON.valueToTree(fixture.waz()));
    }

    @Test
    void nonLiteralSecondAttackRefusesTheWholeBatchBeforeAnyWrite() throws Exception {
        Fixture fixture = fixture();
        power(event(skill(fixture.waz(), "スピア", "0002"), 5, 41, CEventHit.class, 16, 24))
                .getBsdxInfoCollectionList().get(0).getTypeList().add(1);
        JsonNode beforeMek = JSON.valueToTree(fixture.mek());
        JsonNode beforeWaz = JSON.valueToTree(fixture.waz());

        assertThrows(IllegalStateException.class,
                () -> FrejaCustomizer.INSTANCE.customizeBeforeWrite(context(fixture)));

        assertEquals(beforeMek, JSON.valueToTree(fixture.mek()));
        assertEquals(beforeWaz, JSON.valueToTree(fixture.waz()));
    }

    @Test
    void changedSecondAttackRootRefusesTheWholeBatchBeforeAnyWrite() throws Exception {
        assertSecondAttackRootRejected(false);
    }

    @Test
    void missingSecondAttackRootRefusesTheWholeBatchBeforeAnyWrite() throws Exception {
        assertSecondAttackRootRejected(true);
    }

    private static void assertSecondAttackRootRejected(boolean missing) throws Exception {
        Fixture fixture = fixture();
        var collection = power(event(skill(fixture.waz(), "スピア", "0002"),
                5, 41, CEventHit.class, 16, 24)).getBsdxInfoCollectionList().get(0);
        Integer actualRoot = collection.getInt1();
        assertNotNull(actualRoot, "真实转换后的攻击表达式必须带root");
        collection.setInt1(missing ? null : actualRoot + 1);
        JsonNode beforeMek = JSON.valueToTree(fixture.mek());
        JsonNode beforeWaz = JSON.valueToTree(fixture.waz());

        assertThrows(IllegalStateException.class,
                () -> FrejaCustomizer.INSTANCE.customizeBeforeWrite(context(fixture)),
                "只改变P5第二攻击的root，真实目标root=" + actualRoot + ", changed=" + collection.getInt1());

        assertEquals(beforeMek, JSON.valueToTree(fixture.mek()));
        assertEquals(beforeWaz, JSON.valueToTree(fixture.waz()));
    }

    private static Fixture fixture() throws Exception {
        BheBinService service = new BheBinService();
        var sourceMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(
                "src/main/resources/game/bhe/mek/freja/freja.mek", CHARSET).getData();
        var sourceWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(
                "src/main/resources/game/bhe/waz/freja/freja.waz", CHARSET).getData();
        Mek mek = new BheToBsdxMekConverter().convert(sourceMek);
        Waz waz = new BheToBsdxWazConverter().convert(sourceWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "FrejaCustomizerTest");
        return new Fixture(sourceMek, sourceWaz, mek, waz);
    }

    private static FollowupMekaContext context(Fixture fixture) {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setReboundMek(fixture.mek());
        context.setReboundWaz(fixture.waz());
        return context;
    }

    private static void assertSpear(Mek mek, Waz waz) {
        Waz.Skill spear = skill(waz, "スピア", "0002");
        assertEquals(List.of(0, 36, 0, 0, 20, 0), values(event(spear, 3, 18, CEventValRandom.class, 1, 8)));
        assertEquals(List.of(0, 36, 0, 0, 20, 0), values(event(spear, 5, 18, CEventValRandom.class, 12, 15)));
        assertEquals(List.of(180), power(event(spear, 3, 41, CEventHit.class, 3, 10)).getBsdxInfoCollectionList().get(0).getParamList());
        assertEquals(List.of(180), power(event(spear, 5, 41, CEventHit.class, 16, 24)).getBsdxInfoCollectionList().get(0).getParamList());
        assertEquals(24, weapon(mek).getHeatMinConsumption());
        assertEquals(24, weapon(mek).getHeatMaxConsumption());
        assertEquals(0, weapon(mek).getUpgradeExp());
    }

    private static Mek.MekWeaponInfo weapon(Mek mek) {
        return mek.getMekWeaponInfoMap().values().stream()
                .filter(value -> "スピア".equals(value.getWeaponName()) && "0003".equals(value.getWeaponSequence()))
                .reduce((left, right) -> { throw new AssertionError("武装身份不唯一"); }).orElseThrow();
    }

    private static Waz.Skill skill(Waz waz, String japanese, String english) {
        return waz.getSkillList().stream()
                .filter(value -> japanese.equals(value.getSkillNameJapanese()) && english.equals(value.getSkillNameEnglish()))
                .reduce((left, right) -> { throw new AssertionError("技能身份不唯一"); }).orElseThrow();
    }

    private static SkillUnit unit(Waz.Skill skill, int phase, int slot) {
        return skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(value -> Integer.valueOf(slot).equals(value.getUnitQuantity())).findFirst().orElseThrow();
    }

    private static <T extends SkillInfoObject> T event(Waz.Skill skill, int phase, int slot, Class<T> type, int start, int end) {
        return unit(skill, phase, slot).getSkillInfoObjectList().stream()
                .filter(type::isInstance).map(type::cast)
                .filter(value -> Integer.valueOf(start).equals(value.getStartFrame()) && Integer.valueOf(end).equals(value.getEndFrame()))
                .reduce((left, right) -> { throw new AssertionError("事件签名不唯一"); }).orElseThrow();
    }

    private static CEventTerm power(CEventHit hit) {
        return (CEventTerm) hit.getCeventHitUnitList().stream()
                .filter(value -> Integer.valueOf(6).equals(value.getUnitSlotNum()) && Integer.valueOf(1).equals(value.getBuffer()))
                .findFirst().orElseThrow().getData();
    }

    private static JsonNode combatExceptPower(CEventHit hit) {
        JsonNode tree = JSON.valueToTree(hit);
        for (JsonNode node : tree.get("ceventHitUnitList")) {
            if (node.path("unitSlotNum").asInt(-1) == 6) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("data");
            }
        }
        return tree;
    }

    private static List<Integer> values(CEventValRandom value) {
        return List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6());
    }

    private static void assertLosslessJapanese(String text) throws Exception {
        assertNotNull(text);
        assertFalse(text.isBlank());
        var encoder = Charset.forName(CHARSET).newEncoder();
        assertEquals(text, Charset.forName(CHARSET).decode(encoder.encode(CharBuffer.wrap(text))).toString());
    }

    private record Fixture(com.giga.nexas.dto.bhe.mek.Mek sourceMek,
                           com.giga.nexas.dto.bhe.waz.Waz sourceWaz, Mek mek, Waz waz) { }
}
