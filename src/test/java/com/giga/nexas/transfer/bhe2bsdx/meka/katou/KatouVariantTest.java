package com.giga.nexas.transfer.bhe2bsdx.meka.katou;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class KatouVariantTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final String NAME = "トンファー・二連打";
    private static final String ENGLISH = "DX_TONFA_TWO_HIT";
    @TempDir Path output;

    @Test
    void realTonfaAddsTwoFixedHitsAndFinishesAtTheReboundStorageSkill() throws Exception {
        Fixture f = fixture();
        Waz.Skill original = skill(f.waz(), "T_AXE04");
        event(original, 3, 71, CEventChange.class, 3, 3);
        event(original, 5, 71, CEventChange.class, 3, 3);
        JsonNode sourceMek = JSON.valueToTree(f.sourceMek()), sourceWaz = JSON.valueToTree(f.sourceWaz());
        JsonNode originalSkills = JSON.valueToTree(f.waz().getSkillList());
        JsonNode originalWeapons = JSON.valueToTree(f.mek().getMekWeaponInfoMap());
        int weapons = f.mek().getMekWeaponInfoMap().size(), skills = f.waz().getSkillList().size();

        KatouCustomizer.INSTANCE.customizeBeforeWrite(context(f));

        assertEquals(weapons + 1, f.mek().getMekWeaponInfoMap().size(), "必须保留原连打并追加固定二连变体");
        assertVariant(f.mek(), f.waz());
        assertEquals(weapons + 1, f.mek().getMekWeaponInfoMap().size());
        assertEquals(skills + 1, f.waz().getSkillList().size());
        assertEquals(originalSkills, JSON.valueToTree(f.waz().getSkillList().subList(0, skills)));
        for (int i = 0; i < weapons; i++) {
            assertEquals(originalWeapons.get(Integer.toString(i)), JSON.valueToTree(f.mek().getMekWeaponInfoMap().get(i)));
        }
        Waz.Skill copy = skill(f.waz(), ENGLISH);
        assertEquals(JSON.valueToTree(event(original, 3, 41, CEventHit.class, 1, 5)),
                JSON.valueToTree(event(copy, 3, 41, CEventHit.class, 1, 5)));
        assertEquals(JSON.valueToTree(event(original, 5, 41, CEventHit.class, 1, 5)),
                JSON.valueToTree(event(copy, 5, 41, CEventHit.class, 1, 5)));
        CEventChange originalExit = event(original, 6, 71, CEventChange.class, 15, 15);
        CEventChange copiedExit = event(copy, 6, 71, CEventChange.class, 15, 15);
        assertNotSame(originalExit.getBsdxInfoCollectionList2().get(0).getParamList(),
                copiedExit.getBsdxInfoCollectionList2().get(0).getParamList());
        copiedExit.getBsdxInfoCollectionList2().get(0).getParamList().set(1, 999);
        assertEquals(List.of(233, 14), originalExit.getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(sourceMek, JSON.valueToTree(f.sourceMek()));
        assertEquals(sourceWaz, JSON.valueToTree(f.sourceWaz()));
        assertArrayEquals(f.mekBytes(), Files.readAllBytes(mekPath()));
        assertArrayEquals(f.wazBytes(), Files.readAllBytes(wazPath()));
    }

    @Test
    void generatedVariantIncludesTheStorageMaterialAndIsIdempotent() throws Exception {
        Fixture f = fixture();
        KatouCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        JsonNode onceMek = JSON.valueToTree(f.mek()), onceWaz = JSON.valueToTree(f.waz());
        KatouCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        assertEquals(onceMek, JSON.valueToTree(f.mek()));
        assertEquals(onceWaz, JSON.valueToTree(f.waz()));
        var block = f.mek().getMekMaterialBlock();
        int sourceIndex = weaponIndex(f.mek(), "トンファー連打");
        int donorIndex = weaponIndex(f.mek(), "トンファー収納");
        var copy = block.getTrailingEntries().get(block.getTrailingEntries().size() - 1);
        assertNotSame(block.getTrailingEntries().get(sourceIndex), copy);
        for (int i = 0; i < copy.getSpriteGroups().size(); i++) {
            int[] values = copy.getSpriteGroups().get(i);
            for (int expected : block.getTrailingEntries().get(sourceIndex).getSpriteGroups().get(i)) {
                assertTrue(java.util.Arrays.stream(values).anyMatch(v -> v == expected));
            }
            for (int expected : block.getTrailingEntries().get(donorIndex).getSpriteGroups().get(i)) {
                assertTrue(java.util.Arrays.stream(values).anyMatch(v -> v == expected));
            }
        }
        BsdxBinService service = new BsdxBinService();
        Path mek = output.resolve("katou.mek"), waz = output.resolve("katou.waz");
        service.generate(mek.toString(), f.mek(), CHARSET);
        service.generate(waz.toString(), f.waz(), CHARSET);
        Mek parsedMek = (Mek) service.parse(mek.toString(), CHARSET).getData();
        Waz parsedWaz = (Waz) service.parse(waz.toString(), CHARSET).getData();
        assertVariant(parsedMek, parsedWaz);
        assertEquals(parsedMek.getMekWeaponInfoMap().size(), parsedMek.getMekMaterialBlock().getTrailingEntries().size());
        String text = weapon(parsedMek, NAME).getWeaponDescription();
        assertEquals(text, Charset.forName(CHARSET).decode(Charset.forName(CHARSET).newEncoder()
                .encode(CharBuffer.wrap(text))).toString());
    }

    @Test
    void changedSecondLoopConditionRejectsBeforeAnyAppend() throws Exception {
        Fixture f = fixture();
        event(skill(f.waz(), "T_AXE04"), 5, 71, CEventChange.class, 3, 3)
                .getBsdxInfoCollectionList1().get(0).getParamList().set(0, 999);
        assertRejectedWithoutWrites(f);
    }

    @Test
    void anExistingVariantWithTheOldReturnRouteIsRejected() throws Exception {
        Fixture f = fixture();
        KatouCustomizer.INSTANCE.customizeBeforeWrite(context(f));
        event(skill(f.waz(), ENGLISH), 6, 71, CEventChange.class, 15, 15)
                .getBsdxInfoCollectionList2().get(0).getParamList().set(1, 14);
        assertRejectedWithoutWrites(f);
    }

    private static void assertRejectedWithoutWrites(Fixture f) {
        JsonNode mek = JSON.valueToTree(f.mek()), waz = JSON.valueToTree(f.waz());
        assertThrows(IllegalStateException.class, () -> KatouCustomizer.INSTANCE.customizeBeforeWrite(context(f)));
        assertEquals(mek, JSON.valueToTree(f.mek()));
        assertEquals(waz, JSON.valueToTree(f.waz()));
    }
    private static void assertVariant(Mek mek, Waz waz) {
        Waz.Skill copy = skill(waz, ENGLISH);
        var weapon = weapon(mek, NAME);
        assertEquals(NAME, copy.getSkillNameJapanese());
        assertEquals("PUNCH2", weapon.getWeaponSequence());
        assertEquals(waz.getSkillList().indexOf(copy), weapon.getWazSequence());
        assertEquals(18, weapon.getHeatMinConsumption());
        assertEquals(18, weapon.getHeatMaxConsumption());
        assertEquals(0, weapon.getUpgradeExp());
        assertEquals(1, changes(copy, 3).size());
        assertEquals(1, changes(copy, 5).size());
        assertEquals(List.of(3), event(copy, 3, 71, CEventChange.class, 12, 12)
                .getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(0, 0), event(copy, 5, 71, CEventChange.class, 12, 12)
                .getBsdxInfoCollectionList2().get(0).getTypeList());
        assertEquals(List.of(233, waz.getSkillList().indexOf(skill(waz, "0009"))),
                event(copy, 6, 71, CEventChange.class, 15, 15).getBsdxInfoCollectionList2().get(0).getParamList());
    }
    private static Fixture fixture() throws Exception {
        byte[] mekBytes = Files.readAllBytes(mekPath()), wazBytes = Files.readAllBytes(wazPath());
        BheBinService service = new BheBinService();
        var sourceMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mekPath().toString(), CHARSET).getData();
        var sourceWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wazPath().toString(), CHARSET).getData();
        Mek mek = new BheToBsdxMekConverter().convert(sourceMek);
        Waz waz = new BheToBsdxWazConverter().convert(sourceWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "KatouVariantTest");
        // 本测试只迁移本角色显式Change目标，模拟最终group不等于BHE源19的调用边界。
        for (Waz.Skill s : waz.getSkillList()) {
            if (s == null) continue;
            for (var p : s.getPhasesInfo()) for (var u : p.getSkillUnitCollection()) for (var o : u.getSkillInfoObjectList()) {
                if (o instanceof CEventChange c) for (var collection : c.getBsdxInfoCollectionList2()) {
                    if (collection.getParamList().size() == 2 && Integer.valueOf(19).equals(collection.getParamList().get(0))) {
                        collection.setParamList(new java.util.ArrayList<>(List.of(233, collection.getParamList().get(1))));
                    }
                }
            }
        }
        mek.getMekBasicInfo().setWazFileSequence(233);
        return new Fixture(sourceMek, sourceWaz, mek, waz, mekBytes, wazBytes);
    }
    private static Path mekPath() { return Path.of("src/main/resources/game/bhe/mek/katou/katou.mek"); }
    private static Path wazPath() { return Path.of("src/main/resources/game/bhe/waz/katou/katou.waz"); }
    private static FollowupMekaContext context(Fixture f) {
        FollowupMekaContext c = new FollowupMekaContext(); c.setReboundMek(f.mek()); c.setReboundWaz(f.waz()); return c;
    }
    private static Waz.Skill skill(Waz waz, String english) {
        Waz.Skill result = waz.getSkillList().stream().filter(s -> s != null && english.equals(s.getSkillNameEnglish()))
                .reduce((a, b) -> { throw new AssertionError("技能身份重复"); }).orElse(null);
        assertNotNull(result, "应存在技能 " + english);
        return result;
    }
    private static Mek.MekWeaponInfo weapon(Mek mek, String name) { return mek.getMekWeaponInfoMap().get(weaponIndex(mek, name)); }
    private static int weaponIndex(Mek mek, String name) {
        return mek.getMekWeaponInfoMap().entrySet().stream().filter(e -> name.equals(e.getValue().getWeaponName()))
                .reduce((a, b) -> { throw new AssertionError("武装身份重复"); }).orElseThrow().getKey();
    }
    private static List<CEventChange> changes(Waz.Skill skill, int phase) {
        return skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(71).equals(u.getUnitQuantity())).flatMap(u -> u.getSkillInfoObjectList().stream())
                .filter(CEventChange.class::isInstance).map(CEventChange.class::cast).toList();
    }
    private static <T extends SkillInfoObject> T event(Waz.Skill skill, int phase, int slot, Class<T> type, int start, int end) {
        return skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).flatMap(u -> u.getSkillInfoObjectList().stream())
                .filter(type::isInstance).map(type::cast)
                .filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame()))
                .reduce((a, b) -> { throw new AssertionError("事件身份重复"); }).orElseThrow();
    }
    private record Fixture(com.giga.nexas.dto.bhe.mek.Mek sourceMek, com.giga.nexas.dto.bhe.waz.Waz sourceWaz,
                           Mek mek, Waz waz, byte[] mekBytes, byte[] wazBytes) { }
}
