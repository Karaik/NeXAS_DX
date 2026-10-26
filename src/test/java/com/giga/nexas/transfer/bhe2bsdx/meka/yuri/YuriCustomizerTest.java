package com.giga.nexas.transfer.bhe2bsdx.meka.yuri;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
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

class YuriCustomizerTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";

    @TempDir
    Path output;

    @Test
    void realDaggerAndSidestepKeepTheirDistinctCombatRoles() throws Exception {
        Fixture fixture = fixture();
        Waz.Skill dagger = skill(fixture.waz(), "アサシンダガー", "STRAIGHT04");
        Waz.Skill sidestep = skill(fixture.waz(), "サイドステップキック", "SIDESTEPKICK04");
        CEventHit daggerHit = event(dagger, 3, 41, CEventHit.class, 46, 48);
        CEventChange hitExit = event(dagger, 3, 71, CEventChange.class, 46, 48);
        JsonNode daggerCombat = combatExceptPowerAndFrames(daggerHit);
        JsonNode conditions = JSON.valueToTree(hitExit.getBsdxInfoCollectionList1());
        JsonNode transition = JSON.valueToTree(hitExit.getBsdxInfoCollectionList2());
        JsonNode timeout = JSON.valueToTree(event(dagger, 3, 71, CEventChange.class, 92, 92));
        JsonNode kickHit = JSON.valueToTree(event(sidestep, 1, 41, CEventHit.class, 18, 42));
        JsonNode recovery = JSON.valueToTree(event(sidestep, 1, 18, CEventValRandom.class, 29, 46));
        List<JsonNode> otherSkills = fixture.waz().getSkillList().stream()
                .filter(value -> value != dagger && value != sidestep).map(value -> JSON.<JsonNode>valueToTree(value)).toList();
        JsonNode sourceMek = JSON.valueToTree(fixture.sourceMek());
        JsonNode sourceWaz = JSON.valueToTree(fixture.sourceWaz());

        YuriCustomizer.INSTANCE.customizeBeforeWrite(context(fixture));

        assertRework(fixture.mek(), fixture.waz());
        assertEquals(daggerCombat, combatExceptPowerAndFrames(daggerHit));
        assertEquals(conditions, JSON.valueToTree(hitExit.getBsdxInfoCollectionList1()));
        assertEquals(transition, JSON.valueToTree(hitExit.getBsdxInfoCollectionList2()));
        assertEquals(timeout, JSON.valueToTree(event(dagger, 3, 71, CEventChange.class, 92, 92)));
        assertEquals(kickHit, JSON.valueToTree(event(sidestep, 1, 41, CEventHit.class, 18, 42)));
        assertEquals(recovery, JSON.valueToTree(event(sidestep, 1, 18, CEventValRandom.class, 29, 46)));
        assertEquals(otherSkills, fixture.waz().getSkillList().stream()
                .filter(value -> value != dagger && value != sidestep).map(value -> JSON.<JsonNode>valueToTree(value)).toList());
        assertEquals(sourceMek, JSON.valueToTree(fixture.sourceMek()));
        assertEquals(sourceWaz, JSON.valueToTree(fixture.sourceWaz()));
    }

    @Test
    void generatedMekAndWazPreserveReworkAndIdempotence() throws Exception {
        Fixture fixture = fixture();
        FollowupMekaContext context = context(fixture);
        YuriCustomizer.INSTANCE.customizeBeforeWrite(context);
        assertRework(fixture.mek(), fixture.waz());
        JsonNode onceMek = JSON.valueToTree(fixture.mek());
        JsonNode onceWaz = JSON.valueToTree(fixture.waz());

        YuriCustomizer.INSTANCE.customizeBeforeWrite(context);

        assertEquals(onceMek, JSON.valueToTree(fixture.mek()));
        assertEquals(onceWaz, JSON.valueToTree(fixture.waz()));
        BsdxBinService service = new BsdxBinService();
        Path mekFile = output.resolve("yuri.mek");
        Path wazFile = output.resolve("yuri.waz");
        service.generate(mekFile.toString(), fixture.mek(), CHARSET);
        service.generate(wazFile.toString(), fixture.waz(), CHARSET);
        Mek parsedMek = (Mek) service.parse(mekFile.toString(), CHARSET).getData();
        Waz parsedWaz = (Waz) service.parse(wazFile.toString(), CHARSET).getData();
        assertRework(parsedMek, parsedWaz);
        assertEquals(fixture.mek().getMekBasicInfo().getMekDescription(), parsedMek.getMekBasicInfo().getMekDescription());
        assertLosslessJapanese(parsedMek.getMekBasicInfo().getMekDescription());
        assertLosslessJapanese(weapon(parsedMek, "アサシンダガー", "0004").getWeaponDescription());
        assertLosslessJapanese(weapon(parsedMek, "サイドステップキック", "NAGE").getWeaponDescription());
    }

    @Test
    void changedSidestepRefusesDaggerAndMekWritesAsOneBatch() throws Exception {
        Fixture fixture = fixture();
        event(skill(fixture.waz(), "サイドステップキック", "SIDESTEPKICK04"),
                1, 18, CEventValRandom.class, 14, 28).setInt2(999);
        JsonNode beforeMek = JSON.valueToTree(fixture.mek());
        JsonNode beforeWaz = JSON.valueToTree(fixture.waz());

        assertThrows(IllegalStateException.class,
                () -> YuriCustomizer.INSTANCE.customizeBeforeWrite(context(fixture)));

        assertEquals(beforeMek, JSON.valueToTree(fixture.mek()));
        assertEquals(beforeWaz, JSON.valueToTree(fixture.waz()));
    }

    @Test
    void missingHitExitRefusesTheWholeBatchBeforeAnyWrite() throws Exception {
        Fixture fixture = fixture();
        CEventChange exit = event(skill(fixture.waz(), "アサシンダガー", "STRAIGHT04"),
                3, 71, CEventChange.class, 46, 48);
        exit.setStartFrame(45);
        JsonNode beforeMek = JSON.valueToTree(fixture.mek());
        JsonNode beforeWaz = JSON.valueToTree(fixture.waz());

        assertThrows(IllegalStateException.class,
                () -> YuriCustomizer.INSTANCE.customizeBeforeWrite(context(fixture)));

        assertEquals(beforeMek, JSON.valueToTree(fixture.mek()));
        assertEquals(beforeWaz, JSON.valueToTree(fixture.waz()));
    }

    @Test
    void duplicateSkillIdentityRefusesInsteadOfEditingAnArbitraryMatch() throws Exception {
        Fixture fixture = fixture();
        Waz.Skill duplicate = new Waz.Skill();
        duplicate.setSkillNameJapanese("アサシンダガー");
        duplicate.setSkillNameEnglish("STRAIGHT04");
        fixture.waz().getSkillList().add(duplicate);
        JsonNode beforeMek = JSON.valueToTree(fixture.mek());
        JsonNode beforeWaz = JSON.valueToTree(fixture.waz());

        assertThrows(IllegalStateException.class,
                () -> YuriCustomizer.INSTANCE.customizeBeforeWrite(context(fixture)));

        assertEquals(beforeMek, JSON.valueToTree(fixture.mek()));
        assertEquals(beforeWaz, JSON.valueToTree(fixture.waz()));
    }

    private static Fixture fixture() throws Exception {
        BheBinService service = new BheBinService();
        var sourceMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(
                "src/main/resources/game/bhe/mek/yuri/yuri.mek", CHARSET).getData();
        var sourceWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(
                "src/main/resources/game/bhe/waz/yuri/yuri.waz", CHARSET).getData();
        Mek mek = new BheToBsdxMekConverter().convert(sourceMek);
        Waz waz = new BheToBsdxWazConverter().convert(sourceWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "YuriCustomizerTest");
        return new Fixture(sourceMek, sourceWaz, mek, waz);
    }

    private static FollowupMekaContext context(Fixture fixture) {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setReboundMek(fixture.mek());
        context.setReboundWaz(fixture.waz());
        return context;
    }

    private static void assertRework(Mek mek, Waz waz) {
        Waz.Skill dagger = skill(waz, "アサシンダガー", "STRAIGHT04");
        CEventHit daggerHit = unit(dagger, 3, 41).getSkillInfoObjectList().stream()
                .filter(CEventHit.class::isInstance).map(CEventHit.class::cast)
                .reduce((left, right) -> { throw new AssertionError("主攻击事件不唯一"); }).orElseThrow();
        assertEquals(44, daggerHit.getStartFrame());
        assertEquals(50, daggerHit.getEndFrame());
        assertEquals(List.of(140), power(daggerHit).getBsdxInfoCollectionList().get(0).getParamList());
        event(dagger, 3, 71, CEventChange.class, 44, 50);
        event(dagger, 3, 71, CEventChange.class, 92, 92);
        Waz.Skill sidestep = skill(waz, "サイドステップキック", "SIDESTEPKICK04");
        CEventValRandom speed = event(sidestep, 1, 18, CEventValRandom.class, 14, 28);
        assertEquals(List.of(0, 40, 0, 0, 8, 0), List.of(speed.getInt1(), speed.getInt2(), speed.getInt3(), speed.getInt4(), speed.getInt5(), speed.getInt6()));
        assertEquals(List.of(80), power(event(sidestep, 1, 41, CEventHit.class, 18, 42))
                .getBsdxInfoCollectionList().get(0).getParamList());
        Mek.MekWeaponInfo daggerWeapon = weapon(mek, "アサシンダガー", "0004");
        assertEquals(28, daggerWeapon.getHeatMinConsumption());
        assertEquals(28, daggerWeapon.getHeatMaxConsumption());
        assertEquals(0, daggerWeapon.getUpgradeExp());
        Mek.MekWeaponInfo sidestepWeapon = weapon(mek, "サイドステップキック", "NAGE");
        assertEquals(12, sidestepWeapon.getHeatMinConsumption());
        assertEquals(12, sidestepWeapon.getHeatMaxConsumption());
        assertEquals(0, sidestepWeapon.getUpgradeExp());
    }

    private static Mek.MekWeaponInfo weapon(Mek mek, String japanese, String sequence) {
        return mek.getMekWeaponInfoMap().values().stream()
                .filter(value -> japanese.equals(value.getWeaponName()) && sequence.equals(value.getWeaponSequence()))
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

    private static JsonNode combatExceptPowerAndFrames(CEventHit hit) {
        var tree = (com.fasterxml.jackson.databind.node.ObjectNode) JSON.valueToTree(hit);
        tree.remove(List.of("startFrame", "endFrame"));
        for (JsonNode node : tree.get("ceventHitUnitList")) {
            if (node.path("unitSlotNum").asInt(-1) == 6) {
                ((com.fasterxml.jackson.databind.node.ObjectNode) node).remove("data");
            }
        }
        return tree;
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
