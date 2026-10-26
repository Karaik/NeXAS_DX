package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek.BheToBsdxMekConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MisakiVariantTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CS = "windows-31j", NAME = "転移手・挟撃", EN = "DX_PINCER_HANDS";
    @TempDir Path output;

    @Test
    void realFrontAndRearHandsBecomeTwoIndependentBeatsWithTheirDynamicPositions() throws Exception {
        Fixture f = fixture();
        Waz.Skill front = skill(f.waz(), "WARP_P1_11"), rear = skill(f.waz(), "WARP_P1_09");
        JsonNode originals = JSON.valueToTree(f.waz().getSkillList());
        JsonNode sourceMek = JSON.valueToTree(f.rawMek()), sourceWaz = JSON.valueToTree(f.rawWaz());
        int weapons = f.mek().getMekWeaponInfoMap().size(), skills = f.waz().getSkillList().size();
        List<JsonNode> first = effects(front).stream().map(e -> JSON.<JsonNode>valueToTree(e)).toList();
        List<JsonNode> second = effects(rear).stream().map(e -> {
            ObjectNode n = JSON.valueToTree(e); n.put("startFrame", 12); n.put("endFrame", 12); return (JsonNode) n;
        }).toList();
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(weapons + 1, f.mek().getMekWeaponInfoMap().size(), "前後错拍必须是独立可装备变体");
        assertEquals(skills + 1, f.waz().getSkillList().size()); assertVariant(f.mek(), f.waz());
        assertEquals(originals, JSON.valueToTree(f.waz().getSkillList().subList(0, skills)));
        List<CEventEffect> actual = effects(skill(f.waz(), EN));
        assertEquals(first, actual.subList(0, 4).stream().map(e -> JSON.<JsonNode>valueToTree(e)).toList());
        assertEquals(second, actual.subList(4, 8).stream().map(e -> JSON.<JsonNode>valueToTree(e)).toList());
        assertNotSame(effects(rear).get(0).getCeventEffectUnitList(), actual.get(4).getCeventEffectUnitList());
        select(actual.get(4)).setWazSequenceNo(999);
        assertEquals(364, select(effects(rear).get(0)).getWazSequenceNo(), "追加后方Effect不能共享原始子对象");
        assertEquals(sourceMek, JSON.valueToTree(f.rawMek())); assertEquals(sourceWaz, JSON.valueToTree(f.rawWaz()));
    }

    @Test
    void generatedPincerKeepsEightEffectsAndDoesNotAppendAgain() throws Exception {
        Fixture f = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        JsonNode mek = JSON.valueToTree(f.mek()), waz = JSON.valueToTree(f.waz());
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(mek, JSON.valueToTree(f.mek())); assertEquals(waz, JSON.valueToTree(f.waz()));
        BsdxBinService service = new BsdxBinService(); Path m = output.resolve("misaki.mek"), w = output.resolve("misaki.waz");
        service.generate(m.toString(), f.mek(), CS); service.generate(w.toString(), f.waz(), CS);
        Mek parsed = (Mek) service.parse(m.toString(), CS).getData();
        assertVariant(parsed, (Waz) service.parse(w.toString(), CS).getData());
        assertEquals(parsed.getMekWeaponInfoMap().size(), parsed.getMekMaterialBlock().getTrailingEntries().size());
        String text = variantWeapon(parsed).getWeaponDescription();
        assertEquals(text, Charset.forName(CS).decode(Charset.forName(CS).newEncoder().encode(CharBuffer.wrap(text))).toString());
    }

    @Test
    void aChangedRearEmitterRejectsBeforeAppendingAMixedVariant() throws Exception {
        Fixture f = fixture(); select(effects(skill(f.waz(), "WARP_P1_09")).get(3)).setWazSequenceNo(999);
        assertRejected(f);
    }

    @Test
    void aTamperedSecondBeatCannotPassAsAnAlreadyAppliedVariant() throws Exception {
        Fixture f = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        effects(skill(f.waz(), EN)).get(7).setStartFrame(13); assertRejected(f);
    }

    private static void assertRejected(Fixture f) {
        JsonNode mek = JSON.valueToTree(f.mek()), waz = JSON.valueToTree(f.waz());
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
        assertEquals(mek, JSON.valueToTree(f.mek())); assertEquals(waz, JSON.valueToTree(f.waz()));
    }
    private static void assertVariant(Mek mek, Waz waz) {
        Waz.Skill copy = skill(waz, EN); var weapon = variantWeapon(mek);
        assertEquals("0037", weapon.getWeaponSequence()); assertEquals(36, weapon.getHeatMinConsumption());
        assertEquals(36, weapon.getHeatMaxConsumption()); assertEquals(0, weapon.getUpgradeExp());
        assertEquals(waz.getSkillList().indexOf(copy), weapon.getWazSequence());
        List<CEventEffect> effects = effects(copy); assertEquals(8, effects.size());
        assertEquals(List.of(0, 0, 0, 0, 12, 12, 12, 12), effects.stream().map(CEventEffect::getStartFrame).toList());
        assertEquals(List.of(0, 0, 0, 0, 12, 12, 12, 12), effects.stream().map(CEventEffect::getEndFrame).toList());
        assertEquals(List.of(364, 364, 363, 363, 364, 364, 363, 363), effects.stream().map(e -> select(e).getWazSequenceNo()).toList());
        assertTrue(effects.stream().allMatch(e -> Integer.valueOf(9).equals(select(e).getWazFileNo())));
        event(copy, 2, 0, CEventSprite.class, 3, 32); event(copy, 2, 71, CEventChange.class, 32, 32);
        event(copy, 3, 71, CEventChange.class, 27, 27);
    }
    private static Fixture fixture() throws Exception {
        BheBinService service = new BheBinService();
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse("src/main/resources/game/bhe/mek/misaki/misaki.mek", CS).getData();
        var rawWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse("src/main/resources/game/bhe/waz/misaki/misaki.waz", CS).getData();
        var rawCommon = (com.giga.nexas.dto.bhe.waz.Waz) service.parse("src/main/resources/game/bhe/waz/tama05.waz", CS).getData();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek); Waz waz = new BheToBsdxWazConverter().convert(rawWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "MisakiVariantTest"); mek.getMekBasicInfo().setWazFileSequence(233);
        for (String identity : List.of("WARP_P1_11", "WARP_P1_09")) for (CEventEffect e : effects(skill(waz, identity))) {
            var target = select(e); assertEquals(5, target.getWazFileNo());
            target.setWazFileNo(9); target.setWazSequenceNo(32 + target.getWazSequenceNo());
        }
        Waz common = new BheToBsdxWazConverter().convert(rawCommon);
        TsukuyomiRawSourceBundle rawBundle = new TsukuyomiRawSourceBundle(); rawBundle.getCommonProjectileWazByFileName().put("tama05.waz", rawCommon);
        TsukuyomiPackageBundle bundle = new TsukuyomiPackageBundle(); bundle.getWazByFileName().put("tama05.waz", common);
        BheCommonProjectileAppendPlan plan = new BheCommonProjectileAppendPlan();
        plan.getSourceWazIndexToTargetIndex().put(5, 9); plan.getSourceWazIndexToTargetSkillBase().put(5, 32);
        plan.getSourceWazIndexToTargetSkillCount().put(5, common.getSkillList().size()); plan.getSourceWazIndexToTargetFileName().put(5, "tama05.waz");
        TsukuyomiConvertedBundle converted = new TsukuyomiConvertedBundle(); converted.setRawSourceBundle(rawBundle);
        converted.setCommonProjectileResourceBundle(bundle); converted.setCommonProjectileAppendPlan(plan);
        Waz host = new Waz(); for (int i = 0; i < 32; i++) host.getSkillList().add(new Waz.Skill());
        host.getSkillList().addAll(common.getSkillList());
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle(); baseline.getWazByFileName().put("tama05.waz", host);
        FollowupMekaContext context = new FollowupMekaContext(); context.setReboundMek(mek); context.setReboundWaz(waz);
        context.setConvertedBundle(converted); context.setBaseline(baseline);
        return new Fixture(rawMek, rawWaz, mek, waz, context);
    }
    private static Mek.MekWeaponInfo variantWeapon(Mek mek) {
        var result = mek.getMekWeaponInfoMap().values().stream().filter(w -> NAME.equals(w.getWeaponName())).findFirst().orElse(null);
        assertNotNull(result, "必须有前后挟撃装备"); return result;
    }
    private static Waz.Skill skill(Waz waz, String english) {
        Waz.Skill s = waz.getSkillList().stream().filter(v -> v != null && english.equals(v.getSkillNameEnglish()))
                .reduce((a, b) -> { throw new AssertionError("技能重复"); }).orElse(null);
        assertNotNull(s, "应存在技能 " + english); return s;
    }
    private static List<CEventEffect> effects(Waz.Skill s) {
        return s.getPhasesInfo().get(1).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(48).equals(u.getUnitQuantity()))
                .flatMap(u -> u.getSkillInfoObjectList().stream()).filter(CEventEffect.class::isInstance).map(CEventEffect.class::cast).toList();
    }
    private static CEventWazaSelect select(CEventEffect e) {
        return (CEventWazaSelect) e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())).findFirst().orElseThrow().getData();
    }
    private static <T extends SkillInfoObject> T event(Waz.Skill s, int phase, int slot, Class<T> type, int start, int end) {
        return s.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity()))
                .flatMap(u -> u.getSkillInfoObjectList().stream()).filter(type::isInstance).map(type::cast)
                .filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame()))
                .reduce((a, b) -> { throw new AssertionError("事件重复"); }).orElseThrow();
    }
    private record Fixture(com.giga.nexas.dto.bhe.mek.Mek rawMek, com.giga.nexas.dto.bhe.waz.Waz rawWaz,
                           Mek mek, Waz waz, FollowupMekaContext context) { }
}
