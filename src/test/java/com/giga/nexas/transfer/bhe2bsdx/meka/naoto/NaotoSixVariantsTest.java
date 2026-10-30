package com.giga.nexas.transfer.bhe2bsdx.meka.naoto;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Real source integration contracts only; three equipment STARTs and actual hits need runtime evidence. */
class NaotoSixVariantsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CS = "windows-31j", FOLLOWUP = "DX_CLAW_DEPLOY_RISE_FOLLOWUP";
    private static final List<Variant> VARIANTS = List.of(
            new Variant("ワンツー・コンファーム", "DX_HIT_CONFIRM_ONE_TWO", "0001", 20, 0, List.of(1)),
            new Variant("フック・リターン", "DX_HOOK_RETURN", "0002", 22, 2, List.of()),
            new Variant("クロー・起動突き", "DX_CLAW_DEPLOY_RISE", "0015", 26, 5, List.of(8, 7)),
            new Variant("ダウンネイル・収納", "DX_DOWN_NAIL_RETRACT", "0014", 24, 10, List.of(7)),
            new Variant("マシンガン・三点射", "DX_MACHINEGUN_THREE_BURST", "0005", 12, 14, List.of()),
            new Variant("炸裂弾・交差連射", "DX_EXPLOSIVE_STAGGER", "0013", 32, 15, List.of()));
    @TempDir Path output;

    @Test
    void sixEquipmentEntriesAndOnePrivateFollowupPreserveAllOriginals() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        assertEquals(f.weaponCount + 6, f.mek.getMekWeaponInfoMap().size(), "private Rise is not a seventh equipment entry");
        assertEquals(f.skillCount + 7, f.waz.getSkillList().size(), "six main WAZ plus one private Rise");
        assertSix(f.mek, f.waz); assertOriginals(f);
        Waz.Skill helper = skill(f.waz, FOLLOWUP);
        assertTrue(f.mek.getMekWeaponInfoMap().values().stream().noneMatch(w -> Integer.valueOf(f.waz.getSkillList().indexOf(helper)).equals(w.getWazSequence())), "helper cannot consume a second slot or heat charge");
        for (Variant v : VARIANTS) {
            var source = f.mek.getMekMaterialBlock().getTrailingEntries().get(v.sourceRow);
            var copied = f.mek.getMekMaterialBlock().getTrailingEntries().get(row(f.mek, v.name));
            assertNotSame(source, copied); assertMaterial(copied, source);
            for (int donor : v.donors) assertMaterial(copied, f.mek.getMekMaterialBlock().getTrailingEntries().get(donor));
            assertNotSame(source.getSpriteGroups(), copied.getSpriteGroups());
            for (int i = 0; i < source.getSpriteGroups().size(); i++) assertNotSame(source.getSpriteGroups().get(i), copied.getSpriteGroups().get(i));
        }
        CEventChange sourceExit = event(skill(f.waz, "STRAIGHT08"), 0, 71, CEventChange.class, 71, 71);
        CEventChange copiedExit = event(helper, 0, 71, CEventChange.class, 71, 71);
        JsonNode original = tree(sourceExit); copiedExit.getBsdxInfoCollectionList2().get(0).getParamList().set(1, 999);
        assertEquals(original, tree(sourceExit)); assertSources(f);
    }

    @Test
    void allSixAndPrivateRiseSurviveReapplyCp932AndRealGenerateParse() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        JsonNode mek = tree(f.mek), waz = tree(f.waz);
        NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz));
        BsdxBinService service = new BsdxBinService(); Path m = output.resolve("naoto.mek"), w = output.resolve("naoto.waz");
        service.generate(m.toString(), f.mek, CS); service.generate(w.toString(), f.waz, CS);
        Mek parsedMek = (Mek) service.parse(m.toString(), CS).getData(); Waz parsedWaz = (Waz) service.parse(w.toString(), CS).getData();
        assertSix(parsedMek, parsedWaz); assertHook(parsedWaz); assertDeploy(parsedMek, parsedWaz); assertDown(parsedMek, parsedWaz); assertMachinegun(parsedWaz); assertExplosive(parsedWaz);
        Path m2 = output.resolve("naoto-again.mek"), w2 = output.resolve("naoto-again.waz");
        service.generate(m2.toString(), parsedMek, CS); service.generate(w2.toString(), parsedWaz, CS);
        assertArrayEquals(Files.readAllBytes(m), Files.readAllBytes(m2)); assertArrayEquals(Files.readAllBytes(w), Files.readAllBytes(w2)); assertSources(f);
    }

    @Test
    void hookKeepsItsRealHitAndHasConstantRetreatAndAWhiffDoor() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertHook(f.waz);
        assertEquals(tree(event(skill(f.waz, "STRAIGHT05"), 0, 41, CEventHit.class, 34, 36)), tree(event(skill(f.waz, "DX_HOOK_RETURN"), 0, 41, CEventHit.class, 34, 36)), "100-power attack and native normal-hit cancel remain intact");
    }

    @Test
    void deployRoutesBothEntrypathsThroughOneIndependentRiseAndThenStorage() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertDeploy(f.mek, f.waz);
        Waz.Skill source = skill(f.waz, "STRAIGHT10"), copy = skill(f.waz, "DX_CLAW_DEPLOY_RISE");
        for (int p : List.of(0, 1)) assertEquals(tree(events(source, p, 0, CEventSprite.class)), tree(events(copy, p, 0, CEventSprite.class)), "deployment form sprites are preserved on both paths");
        Waz.Skill nativeRise = skill(f.waz, "STRAIGHT08"), helper = skill(f.waz, FOLLOWUP);
        assertEquals(tree(event(nativeRise, 0, 41, CEventHit.class, 18, 22)), tree(event(helper, 0, 41, CEventHit.class, 18, 22)));
        onlyCancelChanges(event(nativeRise, 0, 41, CEventHit.class, 23, 33), event(helper, 0, 41, CEventHit.class, 23, 33), 1);
    }

    @Test
    void downNailRestoresAtTheHeavyHitAndUsesTheActualMovedStorageIndex() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertDown(f.mek, f.waz);
        Waz.Skill original = skill(f.waz, "STRAIGHT09"), copy = skill(f.waz, "DX_DOWN_NAIL_RETRACT");
        assertEquals(tree(event(original, 0, 41, CEventHit.class, 31, 35)), tree(event(copy, 0, 41, CEventHit.class, 31, 35)));
        onlyCancelChanges(event(original, 0, 41, CEventHit.class, 36, 37), event(copy, 0, 41, CEventHit.class, 36, 37), 1);
        assertEquals(tree(events(original, 0, 48, CEventEffect.class)), tree(events(copy, 0, 48, CEventEffect.class)), "31f attack display is not skipped");
    }

    @Test
    void machinegunCopiesThreeWholeBatchesAndRemovesEveryHeldFireLoop() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertMachinegun(f.waz);
        var source = events(skill(f.waz, "GATRING4"), 1, 48, CEventEffect.class);
        var actual = events(skill(f.waz, "DX_MACHINEGUN_THREE_BURST"), 1, 48, CEventEffect.class);
        assertEquals(3, source.size());
        for (int i = 0; i < 3; i++) for (int batch = 0; batch < 3; batch++) {
            int frame = batch * 4;
            CEventEffect template = source.get(i);
            var matches = actual.stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && tree(e.getCeventEffectUnitList()).equals(tree(template.getCeventEffectUnitList()))).toList();
            assertEquals(1, matches.size(), "bullet and both muzzle effects repeat as complete independent batches"); assertCopiedEffect(template, matches.get(0), frame);
        }
        assertNotSame(actual.get(0).getCeventEffectUnitList(), actual.get(3).getCeventEffectUnitList());
    }

    @Test
    void explosiveMovesTheRightBulletAndItsOwnMuzzleWithoutChangingPositionsOrSpeed() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertExplosive(f.waz);
        var source = events(skill(f.waz, "0004"), 1, 48, CEventEffect.class);
        var actual = events(skill(f.waz, "DX_EXPLOSIVE_STAGGER"), 1, 48, CEventEffect.class);
        assertEquals(4, source.size());
        for (int i = 0; i < 4; i++) assertCopiedEffect(source.get(i), actual.get(i), i < 2 ? 0 : 8);
        assertEquals(tree(events(skill(f.waz, "0004"), 1, 18, CEventValRandom.class)), tree(events(skill(f.waz, "DX_EXPLOSIVE_STAGGER"), 1, 18, CEventValRandom.class)));
    }

    @Test
    void newDonorKeyEventDriftRejectsWithNoNewBatchOrPrivateHalfAppend() throws Exception {
        for (String en : List.of("STRAIGHT05", "STRAIGHT10", "STRAIGHT09", "GATRING4", "0004")) {
            Fixture f = fixture(); Waz.Skill s = skill(f.waz, en);
            int p = List.of("STRAIGHT05", "STRAIGHT09").contains(en) ? 0 : 1;
            var changes = events(s, p, 71, CEventChange.class); assertFalse(changes.isEmpty()); changes.get(0).setEndFrame(999);
            assertRejectedWithoutNewAppend(f);
        }
    }

    @Test
    void privateRiseDonorSuffixAndAttackDriftAreRejectedBeforeItCanLeak() throws Exception {
        Fixture suffix = fixture(); skill(suffix.waz, "STRAIGHT08").getSkillSuffixList().get(0).setInt1(99); assertRejectedWithoutNewAppend(suffix);
        Fixture hit = fixture(); event(skill(hit.waz, "STRAIGHT08"), 0, 41, CEventHit.class, 23, 33).setSelfStunFrame(99); assertRejectedWithoutNewAppend(hit);
    }

    @Test
    void tamperedExistingPrivateRiseIsRejectedWithoutAnyRepairOrDuplicate() throws Exception {
        Fixture f = fixture(); NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        Waz.Skill helper = skill(f.waz, FOLLOWUP); event(helper, 0, 71, CEventChange.class, 71, 71).getBsdxInfoCollectionList2().get(0).getParamList().set(1, 999);
        JsonNode mek = tree(f.mek), waz = tree(f.waz);
        assertThrows(IllegalStateException.class, () -> NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context)); assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz));
    }

    private static void assertHook(Waz waz) {
        Waz.Skill s = skill(waz, "DX_HOOK_RETURN");
        assertEquals(List.of(0, -12, 0, 0, -12, 0), values(event(s, 0, 18, CEventValRandom.class, 40, 47)), "both endpoints must be negative, not a 0-to-negative ramp");
        assertEquals(0, event(s, 0, 18, CEventValRandom.class, 48, 48).getInt5());
        assertEquals(1, event(s, 0, 41, CEventHit.class, 34, 36).getSelfStunFrame(), "int23 is the normal hit cancel flag");
        assertEquals(List.of(0, 50, 0, 0, 50, 0), values(event(s, 0, 21, CEventValRandom.class, 34, 34)), "restore constant G50 before the first hit can cancel");
        for (int slot : List.of(27, 28)) {
            CEventVal restore = event(s, 0, slot, CEventVal.class, 34, 34);
            assertEquals(0, restore.getTypeId()); assertEquals(List.of(0, 100, 0, 100), modifierValues(restore));
        }
        assertEquals(2, event(s, 0, 21, CEventValRandom.class, 34, 34).getTypeId());
        assertEquals(List.of(0, 0, 0, 20), modifierValues(event(s, 0, 27, CEventVal.class, 0, 0)), "source preparation's persisted 20 multiplier stays before the attack");
        assertEquals(List.of(0, 100, 0, 100), modifierValues(event(s, 0, 27, CEventVal.class, 63, 70)), "late recovery cannot be a relative 0-to-100 ramp after cancellation is open");
        assertTrue(events(s, 0, 47, CEventVal.class).stream().allMatch(v -> v.getStartFrame() >= 48 && v.getInt4() != 3), "source has no early SD door and this variant must not invent one");
        assertEquals(1, gate(s, 0, 48)); next(event(s, 0, 71, CEventChange.class, 70, 70));
    }
    private static void assertDeploy(Mek mek, Waz waz) {
        Waz.Skill s = skill(waz, "DX_CLAW_DEPLOY_RISE"), helper = skill(waz, FOLLOWUP);
        assertEquals("クロー・起動突き：追撃", helper.getSkillNameJapanese()); assertEquals(1, helper.getPhaseQuantity());
        assertEquals(List.of(List.of(0, 0)), helper.getSkillSuffixList().stream().map(v -> List.of(v.getInt1(), v.getInt2())).toList());
        jump(event(s, 0, 71, CEventChange.class, 67, 67), mek, waz, helper); jump(event(s, 1, 71, CEventChange.class, 53, 53), mek, waz, helper);
        assertEquals(1, gate(s, 0, 44)); assertEquals(1, gate(s, 1, 30));
        assertEquals(0, event(helper, 0, 41, CEventHit.class, 18, 22).getSelfStunFrame());
        assertEquals(1, event(helper, 0, 41, CEventHit.class, 23, 33).getSelfStunFrame()); physical(helper, 0, 23);
        assertEquals(50, event(helper, 0, 21, CEventValRandom.class, 45, 45).getInt5());
        jump(event(helper, 0, 71, CEventChange.class, 71, 71), mek, waz, skill(waz, "STRAIGHT11"));
        for (var g : events(helper, 0, 21, CEventValRandom.class)) if (g.getStartFrame() >= 23) assertTrue(g.getInt5() <= 50);
    }
    private static void assertDown(Mek mek, Waz waz) {
        Waz.Skill s = skill(waz, "DX_DOWN_NAIL_RETRACT");
        assertEquals(0, event(s, 0, 41, CEventHit.class, 31, 35).getSelfStunFrame());
        assertEquals(1, event(s, 0, 41, CEventHit.class, 36, 37).getSelfStunFrame()); physical(s, 0, 36);
        assertTrue(events(s, 0, 27, CEventVal.class).stream().allMatch(v -> v.getEndFrame() <= 55), "71..87 restore cannot occur after the 55f exit");
        jump(event(s, 0, 71, CEventChange.class, 55, 55), mek, waz, skill(waz, "STRAIGHT11"));
        assertEquals(1, events(s, 0, 71, CEventChange.class).size());
    }
    private static void assertMachinegun(Waz waz) {
        Waz.Skill s = skill(waz, "DX_MACHINEGUN_THREE_BURST");
        assertEquals(List.of(0, 100, 0, 100), modifierValues(event(s, 0, 27, CEventVal.class, 0, 13)), "three-burst preparation does not preserve the native multiplier5");
        physical(s, 1, 0); physical(s, 2, 0);
        assertEquals(List.of(0, 100, 0, 100), modifierValues(event(s, 2, 27, CEventVal.class, 21, 33)), "late recovery cannot change safe cancellation physics");
        assertEquals(List.of(0, 0, 0, 4, 4, 4, 8, 8, 8), events(s, 1, 48, CEventEffect.class).stream().map(CEventEffect::getStartFrame).sorted().toList());
        assertEquals(1, events(s, 1, 71, CEventChange.class).size()); next(event(s, 1, 71, CEventChange.class, 12, 12));
        assertNotEquals(1, gate(s, 1, 8)); assertEquals(1, gate(s, 1, 9));
        next(event(s, 0, 71, CEventChange.class, 35, 35)); next(event(s, 2, 71, CEventChange.class, 33, 33));
        for (var e : events(s, 1, 48, CEventEffect.class)) { assertEquals(e.getStartFrame(), e.getEndFrame()); assertEquals(1, ((CEventValRandom) effectData(e, 4)).getInt5()); }
    }
    private static void assertExplosive(Waz waz) {
        Waz.Skill s = skill(waz, "DX_EXPLOSIVE_STAGGER");
        assertEquals(List.of(0, 0, 8, 8), events(s, 1, 48, CEventEffect.class).stream().map(CEventEffect::getStartFrame).toList());
        assertNotEquals(1, gate(s, 1, 8)); assertEquals(1, gate(s, 1, 9)); next(event(s, 1, 71, CEventChange.class, 24, 24)); next(event(s, 2, 71, CEventChange.class, 34, 34));
        assertEquals(1, events(s, 1, 71, CEventChange.class).size());
    }

    private static Fixture fixture() throws Exception {
        Path mp = Path.of("src/main/resources/game/bhe/mek/naoto/naoto.mek"), wp = Path.of("src/main/resources/game/bhe/waz/naoto/naoto.waz");
        Map<Path, byte[]> files = new LinkedHashMap<>(); for (Path p : List.of(mp, wp)) files.put(p, Files.readAllBytes(p));
        BheBinService service = new BheBinService();
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mp.toString(), CS).getData(); var rawWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wp.toString(), CS).getData();
        List<Object> raw = List.of(rawMek, rawWaz); List<JsonNode> rawTrees = raw.stream().map(NaotoSixVariantsTest::tree).toList();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek); Waz waz = new BheToBsdxWazConverter().convert(rawWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "NaotoSixVariantsTest");
        // Move the existing Straight, Rise and storage; every new route must use real identities.
        Collections.swap(waz.getSkillList(), 12, 13); Collections.swap(waz.getSkillList(), 18, 19);
        for (var weapon : mek.getMekWeaponInfoMap().values()) weapon.setWazSequence(moved(weapon.getWazSequence()));
        for (var s : waz.getSkillList()) if (s != null) for (int p = 0; p < s.getPhasesInfo().size(); p++) for (var c : events(s, p, 71, CEventChange.class)) for (var target : c.getBsdxInfoCollectionList2())
            if (List.of(1).equals(target.getTypeList()) && target.getParamList().size() == 2 && Integer.valueOf(15).equals(target.getParamList().get(0))) target.setParamList(new ArrayList<>(List.of(233, moved(target.getParamList().get(1)))));
        mek.getMekBasicInfo().setWazFileSequence(233);
        FollowupMekaContext context = new FollowupMekaContext(); context.setReboundMek(mek); context.setReboundWaz(waz);
        return new Fixture(mek, waz, context, mek.getMekWeaponInfoMap().size(), waz.getSkillList().size(), tree(mek.getMekWeaponInfoMap()), tree(waz.getSkillList()), tree(mek.getMekMaterialBlock().getTrailingEntries()), raw, rawTrees, files);
    }
    private static int moved(int index) { return switch (index) { case 12 -> 13; case 13 -> 12; case 18 -> 19; case 19 -> 18; default -> index; }; }
    private static void assertSix(Mek mek, Waz waz) throws Exception {
        assertEquals(6, mek.getMekWeaponInfoMap().values().stream().filter(w -> Integer.valueOf(0).equals(w.getWeaponCategory()) && w.getWazSequence() >= 0 && w.getWazSequence() < waz.getSkillList().size() && waz.getSkillList().get(w.getWazSequence()).getSkillNameEnglish().startsWith("DX_")).count());
        Set<Integer> targets = new HashSet<>();
        for (Variant v : VARIANTS) {
            int r = row(mek, v.name); assertTrue(r <= 152); var weapon = mek.getMekWeaponInfoMap().get(r); Waz.Skill s = skill(waz, v.english);
            assertEquals(v.name, s.getSkillNameJapanese()); assertEquals(v.sequence, weapon.getWeaponSequence()); assertEquals(0, weapon.getWeaponCategory()); assertEquals(waz.getSkillList().indexOf(s), weapon.getWazSequence()); assertTrue(targets.add(weapon.getWazSequence()));
            assertEquals(v.heat, weapon.getHeatMinConsumption()); assertEquals(v.heat, weapon.getHeatMaxConsumption()); assertEquals(0, weapon.getUpgradeExp()); assertFalse(weapon.getWeaponDescription().isBlank()); cp932(v.name); cp932(weapon.getWeaponDescription());
            var donor = waz.getSkillList().get(mek.getMekWeaponInfoMap().get(v.sourceRow).getWazSequence()); assertEquals(donor.getPhaseQuantity(), s.getPhaseQuantity()); assertEquals(tree(donor.getSkillSuffixList()), tree(s.getSkillSuffixList()));
        }
        assertEquals(mek.getMekWeaponInfoMap().size(), mek.getMekMaterialBlock().getTrailingEntries().size());
        cp932(skill(waz, FOLLOWUP).getSkillNameJapanese());
    }
    private static void assertRejectedWithoutNewAppend(Fixture f) {
        JsonNode weapons = tree(f.mek.getMekWeaponInfoMap()), skills = tree(f.waz.getSkillList()), material = tree(f.mek.getMekMaterialBlock().getTrailingEntries());
        assertAll(() -> assertThrows(IllegalStateException.class, () -> NaotoCustomizer.INSTANCE.customizeBeforeWrite(f.context)),
                () -> { assertTrue(f.mek.getMekWeaponInfoMap().size() <= f.weaponCount + 1); assertTrue(f.waz.getSkillList().size() <= f.skillCount + 1, "private Rise must not leak before the new batch validates");
                    for (int i = 0; i < f.weaponCount; i++) assertEquals(weapons.get("" + i), tree(f.mek.getMekWeaponInfoMap().get(i)));
                    assertEquals(skills, tree(f.waz.getSkillList().subList(0, f.skillCount))); assertEquals(material, tree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, f.weaponCount)));
                    assertTrue(f.waz.getSkillList().stream().noneMatch(s -> FOLLOWUP.equals(s.getSkillNameEnglish()))); for (var v : VARIANTS.subList(1, 6)) assertTrue(f.waz.getSkillList().stream().noneMatch(s -> v.english.equals(s.getSkillNameEnglish()))); });
    }
    private static void assertOriginals(Fixture f) { for (int i = 0; i < f.weaponCount; i++) assertEquals(f.weapons.get("" + i), tree(f.mek.getMekWeaponInfoMap().get(i))); assertEquals(f.skills, tree(f.waz.getSkillList().subList(0, f.skillCount))); assertEquals(f.materials, tree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, f.weaponCount))); }
    private static void assertSources(Fixture f) throws Exception { for (int i = 0; i < f.raw.size(); i++) assertEquals(f.rawTrees.get(i), tree(f.raw.get(i))); for (var e : f.files.entrySet()) assertArrayEquals(e.getValue(), Files.readAllBytes(e.getKey())); }
    private static void assertMaterial(Mek.MekMaterialBlock.PluginEntry copy, Mek.MekMaterialBlock.PluginEntry donor) { groups(copy.getSpriteGroups(), donor.getSpriteGroups()); groups(copy.getSeGroups(), donor.getSeGroups()); groups(copy.getVoiceGroups(), donor.getVoiceGroups()); }
    private static void groups(List<int[]> actual, List<int[]> donor) { assertNotSame(actual, donor); assertTrue(actual.size() >= donor.size()); for (int i = 0; i < donor.size(); i++) { assertNotSame(actual.get(i), donor.get(i)); for (int v : donor.get(i)) { final int group = i; assertTrue(Arrays.stream(actual.get(group)).anyMatch(x -> x == v)); } } }
    private static void cp932(String value) throws Exception { Charset cs = Charset.forName(CS); assertEquals(value, cs.decode(cs.newEncoder().encode(CharBuffer.wrap(value))).toString()); }
    private static int row(Mek mek, String name) { var found = mek.getMekWeaponInfoMap().entrySet().stream().filter(e -> name.equals(e.getValue().getWeaponName())).toList(); assertEquals(1, found.size(), "one equipment identity: " + name); return found.get(0).getKey(); }
    private static Waz.Skill skill(Waz waz, String en) { var found = waz.getSkillList().stream().filter(Objects::nonNull).filter(s -> en.equals(s.getSkillNameEnglish())).toList(); assertEquals(1, found.size(), "one WAZ identity: " + en); return found.get(0); }
    private static <T extends SkillInfoObject> List<T> events(Waz.Skill s, int p, int slot, Class<T> type) { return s.getPhasesInfo().get(p).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).flatMap(u -> u.getSkillInfoObjectList().stream()).filter(type::isInstance).map(type::cast).toList(); }
    private static <T extends SkillInfoObject> T event(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) { var found = events(s, p, slot, type).stream().filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame())).toList(); assertEquals(1, found.size(), s.getSkillNameEnglish() + " P" + p + " slot" + slot + " " + start + ".." + end); return found.get(0); }
    private static SkillInfoObject effectData(CEventEffect e, int slot) { var found = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && u.getBuffer() != 0).toList(); assertEquals(1, found.size()); return found.get(0).getData(); }
    private static void assertCopiedEffect(CEventEffect source, CEventEffect actual, int frame) { ObjectNode expected = tree(source).deepCopy(); expected.put("startFrame", frame); expected.put("endFrame", frame); assertEquals(expected, tree(actual)); }
    private static void onlyCancelChanges(CEventHit source, CEventHit actual, int flag) { ObjectNode expected = tree(source).deepCopy(); expected.put("selfStunFrame", flag); assertEquals(expected, tree(actual), "only int23 changes; int22 and all power/control payloads remain native"); }
    private static List<Integer> values(CEventValRandom v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()); }
    private static List<Integer> modifierValues(CEventVal v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4()); }
    private static void next(CEventChange c) { assertEquals(1, c.getFlag()); assertEquals(List.of(0), c.getBsdxInfoCollectionList1().get(0).getTypeList()); assertEquals(List.of(0, 0), c.getBsdxInfoCollectionList2().get(0).getTypeList()); }
    private static void jump(CEventChange c, Mek mek, Waz waz, Waz.Skill target) { assertEquals(List.of(1), c.getBsdxInfoCollectionList2().get(0).getTypeList()); assertEquals(List.of(mek.getMekBasicInfo().getWazFileSequence(), waz.getSkillList().indexOf(target)), c.getBsdxInfoCollectionList2().get(0).getParamList()); }
    private static void physical(Waz.Skill s, int p, int frame) { var gravity = events(s, p, 21, CEventValRandom.class).stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && Integer.valueOf(frame).equals(e.getEndFrame())).toList(); assertFalse(gravity.isEmpty(), "explicit gravity restore before cancellation"); for (var g : gravity) { assertEquals(0, g.getInt4()); assertEquals(50, g.getInt5()); } for (int slot : List.of(27, 28)) { var modifiers = events(s, p, slot, CEventVal.class).stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && Integer.valueOf(frame).equals(e.getEndFrame())).toList(); assertFalse(modifiers.isEmpty(), "explicit multiplier restore before cancellation"); for (var v : modifiers) { assertEquals(0, v.getInt3()); assertEquals(100, v.getInt4()); } } }
    private static int gate(Waz.Skill s, int p, int frame) { int result = 0; for (int t = 0; t <= frame; t++) for (var v : events(s, p, 47, CEventVal.class)) if (v.getStartFrame() <= t && t <= v.getEndFrame()) { assertEquals(0, v.getInt1()); assertEquals(0, v.getInt3()); int d = v.getEndFrame() - v.getStartFrame(); result = d == 0 ? v.getInt4() : v.getInt2() + (t - v.getStartFrame()) * (v.getInt4() - v.getInt2()) / d; } return result; }
    private static JsonNode tree(Object value) { JsonNode n = JSON.valueToTree(value); clean(n); return n; }
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(NaotoSixVariantsTest::clean); }
    private record Variant(String name, String english, String sequence, int heat, int sourceRow, List<Integer> donors) { }
    private record Fixture(Mek mek, Waz waz, FollowupMekaContext context, int weaponCount, int skillCount, JsonNode weapons, JsonNode skills, JsonNode materials, List<Object> raw, List<JsonNode> rawTrees, Map<Path, byte[]> files) { }
}
