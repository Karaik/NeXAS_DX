package com.giga.nexas.transfer.bhe2bsdx.meka.nagi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class NagiVariantTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final String NAME = "瞬間ディフレクト";
    private static final String ENGLISH = "DX_QUICK_DEFLECT";
    @TempDir Path output;

    @Test
    void realDeflectAddsAShortGuardWindowWithoutEditingTheLongGuardOrCounterAttack() throws Exception {
        Fixture f = fixture();
        Waz.Skill original = skill(f.waz(), "0016");
        assertNotNull(event(original, 1, 71, CEventChange.class, 5, 93));
        assertEquals(List.of(0, 0, 0, 0), values(event(original, 1, 36, CEventVal.class, 94, 94)));
        JsonNode originalSkills = JSON.valueToTree(f.waz().getSkillList());
        JsonNode originalWeapons = JSON.valueToTree(f.mek().getMekWeaponInfoMap());
        JsonNode sourceMek = JSON.valueToTree(f.sourceMek());
        JsonNode sourceWaz = JSON.valueToTree(f.sourceWaz());
        int weaponCount = f.mek().getMekWeaponInfoMap().size();
        int skillCount = f.waz().getSkillList().size();
        int tailCount = f.mek().getMekMaterialBlock().getTrailingEntries().size();

        NagiCustomizer.INSTANCE.customizeBeforeWrite(context(f));

        assertEquals(weaponCount + 6, f.mek().getMekWeaponInfoMap().size(), "必须保留原武装并追加六个独立武装");
        assertVariant(f.mek(), f.waz());
        assertEquals(originalSkills, JSON.valueToTree(f.waz().getSkillList().subList(0, skillCount)));
        for (int i = 0; i < weaponCount; i++) {
            assertEquals(originalWeapons.get(Integer.toString(i)), JSON.valueToTree(f.mek().getMekWeaponInfoMap().get(i)));
        }
        assertEquals(weaponCount + 6, f.mek().getMekWeaponInfoMap().size());
        assertEquals(skillCount + 6, f.waz().getSkillList().size());
        assertEquals(tailCount + 6, f.mek().getMekMaterialBlock().getTrailingEntries().size());
        Waz.Skill copy = skill(f.waz(), ENGLISH);
        ObjectNode counterWithoutArmorRelease = JSON.valueToTree(copy.getPhasesInfo().get(1));
        ArrayNode counterUnits = (ArrayNode) counterWithoutArmorRelease.get("skillUnitCollection");
        for (int i = counterUnits.size() - 1; i >= 0; i--) if (counterUnits.get(i).get("unitQuantity").asInt() == 36) counterUnits.remove(i);
        assertEquals(JSON.valueToTree(original.getPhasesInfo().get(1)), counterWithoutArmorRelease,
                "only the approved successful-counter armor release may differ");
        assertEquals(List.of(0, 0, 0, 0), values(event(copy, 2, 36, CEventVal.class, 0, 0)));
        var originalChange = event(original, 1, 71, CEventChange.class, 5, 93);
        var copiedChange = event(copy, 1, 71, CEventChange.class, 5, 19);
        assertNotSame(originalChange.getBsdxInfoCollectionList1().get(0), copiedChange.getBsdxInfoCollectionList1().get(0));
        copiedChange.getBsdxInfoCollectionList1().get(0).getTypeList().set(0, 999);
        assertEquals(List.of(2, 6, 6), originalChange.getBsdxInfoCollectionList1().get(0).getTypeList());
        assertEquals(sourceMek, JSON.valueToTree(f.sourceMek()));
        assertEquals(sourceWaz, JSON.valueToTree(f.sourceWaz()));
        assertArrayEquals(f.mekBytes(), Files.readAllBytes(mekPath()));
        assertArrayEquals(f.wazBytes(), Files.readAllBytes(wazPath()));
    }

    @Test
    void generatedVariantHasIndependentMaterialAndDoesNotDuplicateOnReapply() throws Exception {
        Fixture f = fixture();
        NagiCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        JsonNode onceMek = JSON.valueToTree(f.mek());
        JsonNode onceWaz = JSON.valueToTree(f.waz());
        NagiCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        assertEquals(onceMek, JSON.valueToTree(f.mek()));
        assertEquals(onceWaz, JSON.valueToTree(f.waz()));
        var block = f.mek().getMekMaterialBlock();
        int sourceIndex = f.mek().getMekWeaponInfoMap().entrySet().stream()
                .filter(e -> "シールド：ディフレクト".equals(e.getValue().getWeaponName())).findFirst().orElseThrow().getKey();
        var source = block.getTrailingEntries().get(sourceIndex);
        int copyIndex = f.mek().getMekWeaponInfoMap().entrySet().stream()
                .filter(e -> NAME.equals(e.getValue().getWeaponName())).findFirst().orElseThrow().getKey();
        var copy = block.getTrailingEntries().get(copyIndex);
        assertNotSame(source, copy);
        assertEquals(JSON.valueToTree(source.getSpriteGroups()), JSON.valueToTree(copy.getSpriteGroups()));
        assertNotSame(source.getSpriteGroups(), copy.getSpriteGroups());
        for (int i = 0; i < source.getSpriteGroups().size(); i++) {
            assertNotSame(source.getSpriteGroups().get(i), copy.getSpriteGroups().get(i));
        }
        BsdxBinService service = new BsdxBinService();
        Path mek = output.resolve("nagi.mek"), waz = output.resolve("nagi.waz");
        service.generate(mek.toString(), f.mek(), CHARSET);
        service.generate(waz.toString(), f.waz(), CHARSET);
        Mek parsedMek = (Mek) service.parse(mek.toString(), CHARSET).getData();
        Waz parsedWaz = (Waz) service.parse(waz.toString(), CHARSET).getData();
        assertVariant(parsedMek, parsedWaz);
        assertEquals(parsedMek.getMekWeaponInfoMap().size(), parsedMek.getMekMaterialBlock().getTrailingEntries().size());
        String description = weapon(parsedMek, NAME).getWeaponDescription();
        assertEquals(description, Charset.forName(CHARSET).decode(Charset.forName(CHARSET).newEncoder()
                .encode(CharBuffer.wrap(description))).toString());
    }

    @Test
    void changedArmorReleaseRejectsBeforeAppendingAnything() throws Exception {
        Fixture f = fixture();
        event(skill(f.waz(), "0016"), 1, 36, CEventVal.class, 94, 94).setInt4(99);
        assertRejectedWithoutWrites(f);
    }

    @Test
    void anExistingVariantWithADifferentGuardWindowIsRejected() throws Exception {
        Fixture f = fixture();
        NagiCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        event(skill(f.waz(), ENGLISH), 1, 71, CEventChange.class, 5, 19).setEndFrame(20);
        assertRejectedWithoutWrites(f);
    }

    private static void assertRejectedWithoutWrites(Fixture f) {
        JsonNode mek = JSON.valueToTree(f.mek()), waz = JSON.valueToTree(f.waz());
        assertThrows(IllegalStateException.class, () -> NagiCustomizer.INSTANCE.customizeBeforeWrite(context(f)));
        assertEquals(mek, JSON.valueToTree(f.mek()));
        assertEquals(waz, JSON.valueToTree(f.waz()));
    }

    private static void assertVariant(Mek mek, Waz waz) {
        Waz.Skill copy = skill(waz, ENGLISH);
        assertEquals(NAME, copy.getSkillNameJapanese());
        var weapon = weapon(mek, NAME);
        assertEquals("0018", weapon.getWeaponSequence());
        assertEquals(waz.getSkillList().indexOf(copy), weapon.getWazSequence());
        assertEquals(12, weapon.getHeatMinConsumption());
        assertEquals(12, weapon.getHeatMaxConsumption());
        assertEquals(0, weapon.getUpgradeExp());
        event(copy, 1, 0, CEventSprite.class, 4, 19);
        event(copy, 1, 0, CEventSprite.class, 20, 39);
        assertEquals(List.of(0, 0, 0, 4), values(event(copy, 1, 36, CEventVal.class, 5, 5)));
        assertEquals(List.of(0, 0, 0, 0), values(event(copy, 1, 36, CEventVal.class, 20, 20)));
        assertEquals(List.of(0, 3, 0, 3), values(event(copy, 1, 47, CEventVal.class, 20, 39)));
        CEventChange counter = event(copy, 1, 71, CEventChange.class, 5, 19);
        assertEquals(List.of(2, 6, 6), counter.getBsdxInfoCollectionList1().get(0).getTypeList());
        assertEquals(List.of(0, 0), counter.getBsdxInfoCollectionList2().get(0).getTypeList());
        assertEquals(List.of(0, 2), event(copy, 1, 71, CEventChange.class, 39, 39)
                .getBsdxInfoCollectionList2().get(0).getTypeList());
        event(copy, 2, 41, CEventHit.class, 19, 35);
        event(copy, 2, 71, CEventChange.class, 79, 79);
    }

    private static Fixture fixture() throws Exception {
        byte[] mekBytes = Files.readAllBytes(mekPath()), wazBytes = Files.readAllBytes(wazPath());
        BheBinService service = new BheBinService();
        var sourceMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mekPath().toString(), CHARSET).getData();
        var sourceWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wazPath().toString(), CHARSET).getData();
        Mek mek = new BheToBsdxMekConverter().convert(sourceMek);
        Waz waz = new BheToBsdxWazConverter().convert(sourceWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "NagiVariantTest");
        int sourceGroup = mek.getMekBasicInfo().getWazFileSequence();
        mek.getMekBasicInfo().setWazFileSequence(233);
        for (var s : waz.getSkillList()) for (var phase : s.getPhasesInfo())
            for (var unit : phase.getSkillUnitCollection()) for (var value : unit.getSkillInfoObjectList())
                if (value instanceof CEventChange change) for (var destination : change.getBsdxInfoCollectionList2())
                    if (Integer.valueOf(16).equals(destination.getInt1()) && List.of(1).equals(destination.getTypeList())
                            && destination.getParamList().size() == 2 && Integer.valueOf(sourceGroup).equals(destination.getParamList().get(0)))
                        destination.setParamList(new java.util.ArrayList<>(List.of(233, destination.getParamList().get(1))));
        return new Fixture(sourceMek, sourceWaz, mek, waz, mekBytes, wazBytes);
    }

    private static Path mekPath() { return Path.of("src/main/resources/game/bhe/mek/nagi/nagi.mek"); }
    private static Path wazPath() { return Path.of("src/main/resources/game/bhe/waz/nagi/nagi.waz"); }
    private static FollowupMekaContext context(Fixture f) {
        FollowupMekaContext c = new FollowupMekaContext(); c.setReboundMek(f.mek()); c.setReboundWaz(f.waz()); return c;
    }
    private static Waz.Skill skill(Waz waz, String english) {
        Waz.Skill result = waz.getSkillList().stream().filter(s -> s != null && english.equals(s.getSkillNameEnglish()))
                .reduce((a, b) -> { throw new AssertionError("技能身份重复"); }).orElse(null);
        assertNotNull(result, "应存在技能 " + english);
        return result;
    }
    private static Mek.MekWeaponInfo weapon(Mek mek, String name) {
        return mek.getMekWeaponInfoMap().values().stream().filter(w -> name.equals(w.getWeaponName()))
                .reduce((a, b) -> { throw new AssertionError("武装身份重复"); }).orElseThrow();
    }
    private static <T extends SkillInfoObject> T event(Waz.Skill skill, int phase, int slot, Class<T> type, int start, int end) {
        return skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).flatMap(u -> u.getSkillInfoObjectList().stream())
                .filter(type::isInstance).map(type::cast)
                .filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame()))
                .reduce((a, b) -> { throw new AssertionError("事件身份重复"); }).orElseThrow();
    }
    private static List<Integer> values(CEventVal e) { return List.of(e.getInt1(), e.getInt2(), e.getInt3(), e.getInt4()); }
    private record Fixture(com.giga.nexas.dto.bhe.mek.Mek sourceMek, com.giga.nexas.dto.bhe.waz.Waz sourceWaz,
                           Mek mek, Waz waz, byte[] mekBytes, byte[] wazBytes) { }
}
