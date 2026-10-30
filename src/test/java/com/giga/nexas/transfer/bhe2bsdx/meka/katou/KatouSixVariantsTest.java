package com.giga.nexas.transfer.bhe2bsdx.meka.katou;

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

/** Real source integration contracts only; a legal event graph does not prove an in-game combo. */
class KatouSixVariantsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CS = "windows-31j";
    private static final List<Variant> VARIANTS = List.of(
            new Variant("トンファー・二連打", "DX_TONFA_TWO_HIT", "PUNCH2", 18, 7, List.of(4)),
            new Variant("トンファー・踏込肘", "DX_TONFA_ENTRY_ELBOW", "0009", 18, 5, List.of(6, 3)),
            new Variant("トンファー・引き肘", "DX_TONFA_RECOIL_ELBOW", "0001", 12, 6, List.of(4)),
            new Variant("蹴崩し・追い肘", "DX_KICK_CONFIRM_ELBOW", "PUNCH", 18, 0, List.of(6)),
            new Variant("トンファー・叩き納め", "DX_TONFA_SLAM_STOW", "0011", 28, 8, List.of(4)),
            new Variant("ミサイル・二段牽制", "DX_MISSILE_TWO_BURST", "MISSILE", 16, 14, List.of()));
    @TempDir Path output;

    @Test
    void sixDistinctOrdinaryEntriesPreserveOriginalAttacksAndMaterialPrefixes() throws Exception {
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        assertEquals(f.weaponCount + 6, f.mek.getMekWeaponInfoMap().size(), "original edits, FC and initializer cannot fill six new equipment slots");
        assertEquals(f.skillCount + 6, f.waz.getSkillList().size()); assertSix(f.mek, f.waz); assertOriginals(f);
        for (Variant v : VARIANTS) {
            var source = f.mek.getMekMaterialBlock().getTrailingEntries().get(v.sourceRow);
            var copied = f.mek.getMekMaterialBlock().getTrailingEntries().get(row(f.mek, v.name));
            assertNotSame(source, copied); assertMaterial(copied, source);
            for (int donor : v.donors) assertMaterial(copied, f.mek.getMekMaterialBlock().getTrailingEntries().get(donor));
            assertNotSame(source.getSpriteGroups(), copied.getSpriteGroups());
            for (int i = 0; i < source.getSpriteGroups().size(); i++) assertNotSame(source.getSpriteGroups().get(i), copied.getSpriteGroups().get(i));
        }
        CEventChange sourceExit = event(skill(f.waz, "STRAIGHT04"), 0, 71, CEventChange.class, 38, 38);
        CEventChange copiedExit = event(skill(f.waz, "DX_TONFA_RECOIL_ELBOW"), 0, 71, CEventChange.class, 38, 38);
        JsonNode before = tree(sourceExit); copiedExit.getBsdxInfoCollectionList2().get(0).getParamList().set(1, 999);
        assertEquals(before, tree(sourceExit)); assertSources(f);
    }

    @Test
    void sixEntriesAreIdempotentCp932SafeAndSurviveRealGenerateParse() throws Exception {
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        JsonNode mek = tree(f.mek), waz = tree(f.waz); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz));
        BsdxBinService service = new BsdxBinService(); Path m = output.resolve("katou.mek"), w = output.resolve("katou.waz");
        service.generate(m.toString(), f.mek, CS); service.generate(w.toString(), f.waz, CS);
        Mek parsedMek = (Mek) service.parse(m.toString(), CS).getData(); Waz parsedWaz = (Waz) service.parse(w.toString(), CS).getData();
        assertSix(parsedMek, parsedWaz); assertEntry(parsedMek, parsedWaz); assertRecoil(parsedMek, parsedWaz); assertKick(parsedMek, parsedWaz); assertSlam(parsedMek, parsedWaz); assertMissiles(parsedWaz);
        Path m2 = output.resolve("katou-again.mek"), w2 = output.resolve("katou-again.waz");
        service.generate(m2.toString(), parsedMek, CS); service.generate(w2.toString(), parsedWaz, CS);
        assertArrayEquals(Files.readAllBytes(m), Files.readAllBytes(m2)); assertArrayEquals(Files.readAllBytes(w), Files.readAllBytes(w2)); assertSources(f);
    }

    @Test
    void entryHasBoundedDriveAndConditionallyBrakesBeforeTheActualMovedElbow() throws Exception {
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertEntry(f.mek, f.waz);
        Waz.Skill source = skill(f.waz, "S_HAMMER07"), copy = skill(f.waz, "DX_TONFA_ENTRY_ELBOW");
        var nativeView = events(source, 0, 71, CEventChange.class).stream().filter(c -> List.of(2, 3, 8, 3).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())).toList();
        var copiedView = events(copy, 0, 71, CEventChange.class).stream().filter(c -> List.of(2, 3, 8, 3).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())).toList();
        assertEquals(1, nativeView.size()); assertEquals(1, copiedView.size()); assertEquals(tree(nativeView.get(0).getBsdxInfoCollectionList1()), tree(copiedView.get(0).getBsdxInfoCollectionList1()));
    }

    @Test
    void recoilPreservesNativeHitThenHasConstantBackstepAndStorageExit() throws Exception {
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertRecoil(f.mek, f.waz);
        assertEquals(tree(event(skill(f.waz, "STRAIGHT04"), 0, 41, CEventHit.class, 7, 20)), tree(event(skill(f.waz, "DX_TONFA_RECOIL_ELBOW"), 0, 41, CEventHit.class, 7, 20)));
        assertEquals(tree(event(skill(f.waz, "STRAIGHT04"), 0, 27, CEventVal.class, 31, 38)), tree(event(skill(f.waz, "DX_TONFA_RECOIL_ELBOW"), 0, 27, CEventVal.class, 31, 38)));
    }

    @Test
    void kickConfirmsOnlyARealHitAndRetainsItsWhiffEnd() throws Exception {
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertKick(f.mek, f.waz);
        assertEquals(tree(event(skill(f.waz, "LOWKICK04"), 0, 41, CEventHit.class, 37, 43)), tree(event(skill(f.waz, "DX_KICK_CONFIRM_ELBOW"), 0, 41, CEventHit.class, 37, 43)), "80-power kick and all control payloads remain native");
    }

    @Test
    void bothSlamBranchesRecoverBeforeNormalAndSdCancelAndEndInStorage() throws Exception {
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertSlam(f.mek, f.waz);
        Waz.Skill source = skill(f.waz, "S_HAMMER08"), copy = skill(f.waz, "DX_TONFA_SLAM_STOW");
        onlyCancelChanges(event(source, 0, 41, CEventHit.class, 44, 52), event(copy, 0, 41, CEventHit.class, 44, 52), 0);
        onlyCancelChanges(event(source, 1, 41, CEventHit.class, 28, 36), event(copy, 1, 41, CEventHit.class, 28, 36), 0);
        for (int p : List.of(0, 1)) assertEquals(tree(events(source, p, 48, CEventEffect.class)), tree(events(copy, p, 48, CEventEffect.class)), "attack visuals remain on their original pages and frames");
    }

    @Test
    void missilesHaveTwoWholeLaunchesAndNoReleaseOrHeldButtonLoop() throws Exception {
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertMissiles(f.waz);
        var source = events(skill(f.waz, "0002"), 1, 48, CEventEffect.class); var actual = events(skill(f.waz, "DX_MISSILE_TWO_BURST"), 1, 48, CEventEffect.class);
        assertEquals(2, source.size());
        for (int i = 0; i < source.size(); i++) { CEventEffect template = source.get(i); for (int frame : List.of(9, 21)) {
            var found = actual.stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && tree(e.getCeventEffectUnitList()).equals(tree(template.getCeventEffectUnitList()))).toList();
            assertEquals(1, found.size()); assertCopiedEffect(template, found.get(0), frame);
        } }
        assertNotSame(actual.get(0).getCeventEffectUnitList(), actual.get(2).getCeventEffectUnitList());
    }

    @Test
    void everyNewDonorKeyEventDriftRejectsWithoutAHalfNewBatch() throws Exception {
        for (String en : List.of("S_HAMMER07", "STRAIGHT04", "LOWKICK04", "S_HAMMER08", "0002")) {
            Fixture f = fixture(); Waz.Skill s = skill(f.waz, en); int p = "0002".equals(en) ? 1 : 0;
            var changes = events(s, p, 71, CEventChange.class); assertFalse(changes.isEmpty()); changes.get(0).setEndFrame(999);
            assertRejectedWithoutNewAppend(f);
        }
    }

    @Test
    void entrySuffixDriftAndExistingMissileTamperingCannotPassIdempotence() throws Exception {
        Fixture drift = fixture(); skill(drift.waz, "S_HAMMER07").getSkillSuffixList().get(1).setInt1(99); assertRejectedWithoutNewAppend(drift);
        Fixture f = fixture(); KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        event(skill(f.waz, "DX_MISSILE_TWO_BURST"), 1, 71, CEventChange.class, 25, 25).getBsdxInfoCollectionList2().get(0).getTypeList().set(1, 1);
        JsonNode mek = tree(f.mek), waz = tree(f.waz);
        assertThrows(IllegalStateException.class, () -> KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context)); assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz));
    }

    private static void assertEntry(Mek mek, Waz waz) {
        Waz.Skill s = skill(waz, "DX_TONFA_ENTRY_ELBOW");
        assertEquals(List.of(0, 50, 0, 0, 50, 0), values(event(s, 0, 21, CEventValRandom.class, 0, 0)), "new approach cannot carry native G0 into an airborne cancel");
        physical(s, 1, 0);
        assertEquals(List.of(0, 64, 0, 0, 16, 0), values(event(s, 0, 18, CEventValRandom.class, 34, 38)));
        var changes = events(s, 0, 71, CEventChange.class);
        var view = changes.stream().filter(c -> List.of(2, 3, 8, 3).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())).toList();
        assertEquals(1, view.size()); assertEquals(List.of(180), view.get(0).getBsdxInfoCollectionList1().get(0).getParamList());
        var distance = changes.stream().filter(c -> List.of(2, 3, 6, 2).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())).toList();
        assertEquals(1, distance.size()); assertEquals(List.of(110), distance.get(0).getBsdxInfoCollectionList1().get(0).getParamList());
        assertEquals(List.of(0, 0), distance.get(0).getBsdxInfoCollectionList1().get(0).getIntList3()); assertEquals(List.of(1, 0), distance.get(0).getBsdxInfoCollectionList1().get(0).getIntList4());
        for (var c : changes) { assertTrue(c.getEndFrame() <= 44); localPhase(c, 0, 1); }
        var timeout = event(s, 0, 71, CEventChange.class, 44, 44); always(timeout); localPhase(timeout, 0, 1); bounded(s, 0, 44);
        assertEquals(0, event(s, 1, 18, CEventValRandom.class, 0, 0).getInt5());
        assertNotEquals(1, gate(s, 1, 4)); assertEquals(1, gate(s, 1, 5));
        jump(event(s, 1, 71, CEventChange.class, 8, 8), mek, waz, skill(waz, "STRAIGHT04"));
    }
    private static void assertRecoil(Mek mek, Waz waz) {
        Waz.Skill s = skill(waz, "DX_TONFA_RECOIL_ELBOW");
        assertEquals(List.of(0, -16, 0, 0, -16, 0), values(event(s, 0, 18, CEventValRandom.class, 24, 31)));
        assertEquals(0, event(s, 0, 18, CEventValRandom.class, 32, 32).getInt5());
        assertEquals(1, event(s, 0, 41, CEventHit.class, 7, 20).getSelfStunFrame());
        event(s, 0, 18, CEventValRandom.class, 7, 15); jump(event(s, 0, 71, CEventChange.class, 38, 38), mek, waz, skill(waz, "0009"));
    }
    private static void assertKick(Mek mek, Waz waz) {
        Waz.Skill s = skill(waz, "DX_KICK_CONFIRM_ELBOW"); var changes = events(s, 0, 71, CEventChange.class);
        assertEquals(2, changes.size()); CEventChange hit = event(s, 0, 71, CEventChange.class, 37, 43); assertSame(hit, changes.get(0), "native hit route has priority over other exits");
        assertEquals(1, hit.getFlag()); assertEquals(0, hit.getInt1()); var term = hit.getBsdxInfoCollectionList1().get(0);
        assertEquals(0, term.getInt1()); assertEquals(List.of(2, 6, 0, 0), term.getTypeList()); assertEquals(List.of(0), term.getParamList()); assertEquals(List.of(0, 0), term.getIntList3()); assertTrue(term.getIntList4().isEmpty());
        jump(hit, mek, waz, skill(waz, "STRAIGHT04"));
        CEventChange whiff = event(s, 0, 71, CEventChange.class, 70, 70); always(whiff); assertEquals(List.of(0, 2), whiff.getBsdxInfoCollectionList2().get(0).getTypeList());
    }
    private static void assertSlam(Mek mek, Waz waz) {
        Waz.Skill s = skill(waz, "DX_TONFA_SLAM_STOW");
        for (int p : List.of(0, 1)) {
            int lastHit = p == 0 ? 52 : 36, door = lastHit + 1, exit = p == 0 ? 65 : 49;
            physical(s, p, 0); physical(s, p, door);
            assertEquals(0, event(s, p, 41, CEventHit.class, p == 0 ? 44 : 28, lastHit).getSelfStunFrame());
            assertEquals(3, gate(s, p, lastHit)); assertEquals(1, gate(s, p, door)); assertEquals(1, gate(s, p, exit));
            for (var g : events(s, p, 21, CEventValRandom.class)) { assertTrue(g.getInt2() <= 50 && g.getInt5() <= 50, "early SD cannot inherit G100/G300"); }
            for (var v : events(s, p, 47, CEventVal.class)) if (v.getInt4() == 3) assertTrue(v.getEndFrame() <= lastHit, "later SD must not overwrite the normal door");
            jump(event(s, p, 71, CEventChange.class, exit, exit), mek, waz, skill(waz, "0009"));
        }
        assertTrue(events(s, 0, 71, CEventChange.class).stream().anyMatch(c -> List.of(2, 7, 17).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())), "ground/air entry branch survives");
    }
    private static void assertMissiles(Waz waz) {
        Waz.Skill s = skill(waz, "DX_MISSILE_TWO_BURST");
        assertEquals(List.of(9, 9, 21, 21), events(s, 1, 48, CEventEffect.class).stream().map(CEventEffect::getStartFrame).sorted().toList());
        assertEquals(1, events(s, 1, 71, CEventChange.class).size()); CEventChange exit = event(s, 1, 71, CEventChange.class, 25, 25); always(exit); localPhase(exit, 1, 2);
        assertNotEquals(1, gate(s, 1, 21)); assertEquals(1, gate(s, 1, 22));
        for (var e : events(s, 1, 48, CEventEffect.class)) { assertEquals(e.getStartFrame(), e.getEndFrame()); var amount = assertInstanceOf(CEventValRandom.class, effectData(e, 4)); assertEquals(1, amount.getInt5(), "interval6 is not six missiles"); }
        always(event(s, 0, 71, CEventChange.class, 21, 21)); event(s, 2, 71, CEventChange.class, 39, 39);
    }

    private static Fixture fixture() throws Exception {
        Path mp = Path.of("src/main/resources/game/bhe/mek/katou/katou.mek"), wp = Path.of("src/main/resources/game/bhe/waz/katou/katou.waz");
        Map<Path, byte[]> files = new LinkedHashMap<>(); for (Path p : List.of(mp, wp)) files.put(p, Files.readAllBytes(p));
        BheBinService service = new BheBinService();
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mp.toString(), CS).getData(); var rawWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wp.toString(), CS).getData();
        List<Object> raw = List.of(rawMek, rawWaz); List<JsonNode> rawTrees = raw.stream().map(KatouSixVariantsTest::tree).toList();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek); Waz waz = new BheToBsdxWazConverter().convert(rawWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "KatouSixVariantsTest");
        Collections.swap(waz.getSkillList(), 15, 17);
        for (var weapon : mek.getMekWeaponInfoMap().values()) weapon.setWazSequence(moved(weapon.getWazSequence()));
        for (var s : waz.getSkillList()) if (s != null) for (int p = 0; p < s.getPhasesInfo().size(); p++) for (var c : events(s, p, 71, CEventChange.class)) for (var target : c.getBsdxInfoCollectionList2())
            if (List.of(1).equals(target.getTypeList()) && target.getParamList().size() == 2 && Integer.valueOf(19).equals(target.getParamList().get(0))) target.setParamList(new ArrayList<>(List.of(233, moved(target.getParamList().get(1)))));
        mek.getMekBasicInfo().setWazFileSequence(233);
        FollowupMekaContext context = new FollowupMekaContext(); context.setReboundMek(mek); context.setReboundWaz(waz);
        return new Fixture(mek, waz, context, mek.getMekWeaponInfoMap().size(), waz.getSkillList().size(), tree(mek.getMekWeaponInfoMap()), tree(waz.getSkillList()), tree(mek.getMekMaterialBlock().getTrailingEntries()), raw, rawTrees, files);
    }
    private static int moved(int index) { return index == 15 ? 17 : index == 17 ? 15 : index; }
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
    }
    private static void assertRejectedWithoutNewAppend(Fixture f) {
        JsonNode weapons = tree(f.mek.getMekWeaponInfoMap()), skills = tree(f.waz.getSkillList()), material = tree(f.mek.getMekMaterialBlock().getTrailingEntries());
        assertAll(() -> assertThrows(IllegalStateException.class, () -> KatouCustomizer.INSTANCE.customizeBeforeWrite(f.context)),
                () -> { assertTrue(f.mek.getMekWeaponInfoMap().size() <= f.weaponCount + 1, "new batch leaves no partial equipment"); assertTrue(f.waz.getSkillList().size() <= f.skillCount + 1, "new batch leaves no partial WAZ");
                    for (int i = 0; i < f.weaponCount; i++) assertEquals(weapons.get("" + i), tree(f.mek.getMekWeaponInfoMap().get(i)));
                    assertEquals(skills, tree(f.waz.getSkillList().subList(0, f.skillCount))); assertEquals(material, tree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, f.weaponCount)));
                    for (var v : VARIANTS.subList(1, 6)) assertTrue(f.waz.getSkillList().stream().noneMatch(s -> v.english.equals(s.getSkillNameEnglish()))); });
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
    private static void onlyCancelChanges(CEventHit source, CEventHit actual, int flag) { ObjectNode expected = tree(source).deepCopy(); expected.put("selfStunFrame", flag); assertEquals(expected, tree(actual), "int23 changes while int22 and all power/control payloads stay native"); }
    private static List<Integer> values(CEventValRandom v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()); }
    private static void always(CEventChange c) { assertEquals(1, c.getFlag()); assertEquals(List.of(0), c.getBsdxInfoCollectionList1().get(0).getTypeList()); }
    private static void localPhase(CEventChange c, int sourcePhase, int targetPhase) { var target = c.getBsdxInfoCollectionList2().get(0); if (List.of(0, 0).equals(target.getTypeList())) assertEquals(sourcePhase + 1, targetPhase); else { assertEquals(List.of(0, 1), target.getTypeList()); assertEquals(List.of(targetPhase), target.getParamList()); } }
    private static void jump(CEventChange c, Mek mek, Waz waz, Waz.Skill target) { assertEquals(List.of(1), c.getBsdxInfoCollectionList2().get(0).getTypeList()); assertEquals(List.of(mek.getMekBasicInfo().getWazFileSequence(), waz.getSkillList().indexOf(target)), c.getBsdxInfoCollectionList2().get(0).getParamList()); }
    private static void physical(Waz.Skill s, int p, int frame) { var gravity = events(s, p, 21, CEventValRandom.class).stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && Integer.valueOf(frame).equals(e.getEndFrame())).toList(); assertFalse(gravity.isEmpty(), "explicit gravity restore before cancellation"); for (var g : gravity) { assertEquals(0, g.getInt4()); assertEquals(50, g.getInt5()); } for (int slot : List.of(27, 28)) { var modifiers = events(s, p, slot, CEventVal.class).stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && Integer.valueOf(frame).equals(e.getEndFrame())).toList(); assertFalse(modifiers.isEmpty(), "explicit multiplier restore before cancellation"); for (var v : modifiers) { assertEquals(0, v.getInt3()); assertEquals(100, v.getInt4()); } } }
    /** Absolute type0 cancellation Work only; a persisted value 3 grants SD, not an ordinary follow-up. */
    private static int gate(Waz.Skill s, int p, int frame) { int result = 0; for (int t = 0; t <= frame; t++) for (var v : events(s, p, 47, CEventVal.class)) if (v.getStartFrame() <= t && t <= v.getEndFrame()) { assertEquals(0, v.getInt1()); assertEquals(0, v.getInt3()); int d = v.getEndFrame() - v.getStartFrame(); result = d == 0 ? v.getInt4() : v.getInt2() + (t - v.getStartFrame()) * (v.getInt4() - v.getInt2()) / d; } return result; }
    private static void bounded(Waz.Skill s, int p, int exit) { for (var u : s.getPhasesInfo().get(p).getSkillUnitCollection()) for (var e : u.getSkillInfoObjectList()) { assertTrue(e.getEndFrame() <= exit, "tail beyond the finite approach exit: slot" + u.getUnitQuantity()); if (e instanceof CEventEffect effect) for (var child : effect.getCeventEffectUnitList()) if (child.getData() != null && child.getBuffer() != 0) assertTrue(child.getData().getEndFrame() <= e.getEndFrame() - e.getStartFrame(), "clipped approach trails also need clipped relative child clocks"); } }
    private static JsonNode tree(Object value) { JsonNode n = JSON.valueToTree(value); clean(n); return n; }
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(KatouSixVariantsTest::clean); }
    private record Variant(String name, String english, String sequence, int heat, int sourceRow, List<Integer> donors) { }
    private record Fixture(Mek mek, Waz waz, FollowupMekaContext context, int weaponCount, int skillCount, JsonNode weapons, JsonNode skills, JsonNode materials, List<Object> raw, List<JsonNode> rawTrees, Map<Path, byte[]> files) { }
}
