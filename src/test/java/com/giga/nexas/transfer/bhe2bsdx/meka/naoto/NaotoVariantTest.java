package com.giga.nexas.transfer.bhe2bsdx.meka.naoto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NaotoVariantTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CS = "windows-31j", NAME = "ワンツー・コンファーム", EN = "DX_HIT_CONFIRM_ONE_TWO";
    @TempDir Path output;

    @Test
    void realJabAddsOnlyAnOnHitRouteToTheExistingMovedStraight() throws Exception {
        Fixture f = fixture();
        int weapons = f.mek().getMekWeaponInfoMap().size(), skills = f.waz().getSkillList().size();
        JsonNode beforeSkills = JSON.valueToTree(f.waz().getSkillList());
        JsonNode beforeWeapons = JSON.valueToTree(f.mek().getMekWeaponInfoMap());
        JsonNode rawMek = JSON.valueToTree(f.rawMek()), rawWaz = JSON.valueToTree(f.rawWaz());
        NaotoCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        assertEquals(weapons + 1, f.mek().getMekWeaponInfoMap().size(), "追加命中确认装备，保留原jab");
        assertEquals(skills + 1, f.waz().getSkillList().size(), "Straight不改，不复制隐藏终结技");
        assertVariant(f.mek(), f.waz());
        assertEquals(beforeSkills, JSON.valueToTree(f.waz().getSkillList().subList(0, skills)));
        for (int i = 0; i < weapons; i++) assertEquals(beforeWeapons.get(Integer.toString(i)), JSON.valueToTree(f.mek().getMekWeaponInfoMap().get(i)));
        CEventChange onHit = event(skill(f.waz(), EN), 1, 71, CEventChange.class, 11, 16);
        assertEquals(f.nativeOnHitCondition(), JSON.valueToTree(onHit.getBsdxInfoCollectionList1()),
                "命中语义来自实际Yuri原版转换后的命中派生，不能凭数字猜条件");
        assertEquals(JSON.valueToTree(event(skill(f.waz(), "STRAIGHT07"), 1, 41, CEventHit.class, 11, 16)),
                JSON.valueToTree(event(skill(f.waz(), EN), 1, 41, CEventHit.class, 11, 16)));
        assertEquals(rawMek, JSON.valueToTree(f.rawMek()));
        assertEquals(rawWaz, JSON.valueToTree(f.rawWaz()));
    }

    @Test
    void generatedVariantKeepsItsWhiffExitAndReapplyDoesNotDuplicateIt() throws Exception {
        Fixture f = fixture();
        NaotoCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        JsonNode mek = JSON.valueToTree(f.mek()), waz = JSON.valueToTree(f.waz());
        NaotoCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        assertEquals(mek, JSON.valueToTree(f.mek())); assertEquals(waz, JSON.valueToTree(f.waz()));
        BsdxBinService service = new BsdxBinService();
        Path m = output.resolve("naoto.mek"), w = output.resolve("naoto.waz");
        service.generate(m.toString(), f.mek(), CS); service.generate(w.toString(), f.waz(), CS);
        Mek parsed = (Mek) service.parse(m.toString(), CS).getData();
        assertVariant(parsed, (Waz) service.parse(w.toString(), CS).getData());
        assertEquals(parsed.getMekWeaponInfoMap().size(), parsed.getMekMaterialBlock().getTrailingEntries().size());
        String description = variantWeapon(parsed).getWeaponDescription();
        assertEquals(description, Charset.forName(CS).decode(Charset.forName(CS).newEncoder().encode(CharBuffer.wrap(description))).toString());
    }

    @Test
    void changedFinisherPowerRejectsTheWholeAppend() throws Exception {
        Fixture f = fixture();
        power(event(skill(f.waz(), "STRAIGHT04"), 1, 41, CEventHit.class, 17, 30)).getBsdxInfoCollectionList().get(0).getParamList().set(0, 999);
        assertRejected(f);
    }

    @Test
    void anExistingVariantWithADifferentHitConditionIsRejected() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        event(skill(f.waz(), EN), 1, 71, CEventChange.class, 11, 16).getBsdxInfoCollectionList1().get(0).setInt1(999);
        assertRejected(f);
    }

    private static void assertRejected(Fixture f) {
        JsonNode mek = JSON.valueToTree(f.mek()), waz = JSON.valueToTree(f.waz());
        assertThrows(IllegalStateException.class, () -> NaotoCustomizer.INSTANCE.customizeBeforeWrite(context(f)));
        assertEquals(mek, JSON.valueToTree(f.mek())); assertEquals(waz, JSON.valueToTree(f.waz()));
    }
    private static void assertVariant(Mek mek, Waz waz) {
        Waz.Skill copy = skill(waz, EN); Mek.MekWeaponInfo weapon = variantWeapon(mek);
        assertEquals("0001", weapon.getWeaponSequence()); assertEquals(20, weapon.getHeatMinConsumption());
        assertEquals(20, weapon.getHeatMaxConsumption()); assertEquals(0, weapon.getUpgradeExp());
        assertEquals(waz.getSkillList().indexOf(copy), weapon.getWazSequence());
        CEventChange hit = event(copy, 1, 71, CEventChange.class, 11, 16);
        assertEquals(1, hit.getFlag()); assertEquals(0, hit.getInt1());
        var c = hit.getBsdxInfoCollectionList1().get(0);
        assertEquals(0, c.getInt1()); assertEquals(List.of(2, 6, 0, 0), c.getTypeList());
        assertEquals(List.of(0), c.getParamList()); assertEquals(List.of(0, 0), c.getIntList3()); assertTrue(c.getIntList4().isEmpty());
        var destination = hit.getBsdxInfoCollectionList2().get(0);
        assertEquals(16, destination.getInt1()); assertEquals(List.of(1), destination.getTypeList());
        assertEquals(List.of(233, 13), destination.getParamList(), "定位移动后的现有Straight，不能使用源12");
        assertEquals(List.of(0, 0), event(copy, 1, 71, CEventChange.class, 37, 37).getBsdxInfoCollectionList2().get(0).getTypeList());
        assertEquals(List.of(20), power(event(copy, 1, 41, CEventHit.class, 11, 16)).getBsdxInfoCollectionList().get(0).getParamList());
        assertEquals(List.of(30), power(event(skill(waz, "STRAIGHT04"), 1, 41, CEventHit.class, 17, 30)).getBsdxInfoCollectionList().get(0).getParamList());
        event(skill(waz, "STRAIGHT04"), 1, 71, CEventChange.class, 57, 57);
    }
    private static Fixture fixture() throws Exception {
        BheBinService service = new BheBinService();
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse("src/main/resources/game/bhe/mek/naoto/naoto.mek", CS).getData();
        var rawWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse("src/main/resources/game/bhe/waz/naoto/naoto.waz", CS).getData();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek); Waz waz = new BheToBsdxWazConverter().convert(rawWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "NaotoVariantTest");
        Collections.swap(waz.getSkillList(), 12, 13);
        for (var weapon : mek.getMekWeaponInfoMap().values()) {
            if (Integer.valueOf(12).equals(weapon.getWazSequence())) weapon.setWazSequence(13);
            else if (Integer.valueOf(13).equals(weapon.getWazSequence())) weapon.setWazSequence(12);
        }
        mek.getMekBasicInfo().setWazFileSequence(233);
        var rawYuri = (com.giga.nexas.dto.bhe.waz.Waz) service.parse("src/main/resources/game/bhe/waz/yuri/yuri.waz", CS).getData();
        Waz yuri = new BheToBsdxWazConverter().convert(rawYuri);
        new BheInfoCollectionObjectGraphRewriter().rewrite(yuri, "NaotoVariantTest native hit condition");
        JsonNode condition = JSON.valueToTree(event(skill(yuri, "STRAIGHT04"), 3, 71, CEventChange.class, 46, 48).getBsdxInfoCollectionList1());
        return new Fixture(rawMek, rawWaz, mek, waz, condition);
    }
    private static FollowupMekaContext context(Fixture f) { FollowupMekaContext c = new FollowupMekaContext(); c.setReboundMek(f.mek()); c.setReboundWaz(f.waz()); return c; }
    private static Mek.MekWeaponInfo variantWeapon(Mek mek) {
        var result = mek.getMekWeaponInfoMap().values().stream().filter(w -> NAME.equals(w.getWeaponName())).findFirst().orElse(null);
        assertNotNull(result, "必须有命中确认装备"); return result;
    }
    private static Waz.Skill skill(Waz waz, String english) {
        Waz.Skill result = waz.getSkillList().stream().filter(s -> s != null && english.equals(s.getSkillNameEnglish()))
                .reduce((a, b) -> { throw new AssertionError("技能身份重复"); }).orElse(null);
        assertNotNull(result, "应存在技能 " + english); return result;
    }
    private static <T extends SkillInfoObject> T event(Waz.Skill s, int phase, int slot, Class<T> type, int start, int end) {
        return s.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity()))
                .flatMap(u -> u.getSkillInfoObjectList().stream()).filter(type::isInstance).map(type::cast)
                .filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame()))
                .reduce((a, b) -> { throw new AssertionError("事件重复"); }).orElseThrow();
    }
    private static CEventTerm power(CEventHit hit) { return (CEventTerm) hit.getCeventHitUnitList().stream().filter(u -> Integer.valueOf(6).equals(u.getUnitSlotNum())).findFirst().orElseThrow().getData(); }
    private record Fixture(com.giga.nexas.dto.bhe.mek.Mek rawMek, com.giga.nexas.dto.bhe.waz.Waz rawWaz,
                           Mek mek, Waz waz, JsonNode nativeOnHitCondition) { }
}
