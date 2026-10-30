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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

/** Real source integration contracts only; emitted timelines are not gameplay validation. */
class MisakiSixVariantsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CS = "windows-31j";
    private static final List<Variant> VARIANTS = List.of(
            new Variant("転移手・挟撃", "DX_PINCER_HANDS", "0037", 36, 5, List.of(6)),
            new Variant("鉱石・二拍射", "DX_ORE_DOUBLE_TAP", "0017", 18, 1, List.of()),
            new Variant("転移手・二度招き", "DX_HAND_TWO_REACH", "0025", 24, 4, List.of()),
            new Variant("血晶槍・短束", "DX_CRYSTAL_SHORT_SPEAR", "0024", 18, 14, List.of()),
            new Variant("血晶剣・帰着", "DX_CRYSTAL_SWORD_RETURN", "0022", 32, 11, List.of()),
            new Variant("床召還・重ね置き", "DX_FLOOR_DOUBLE_MARK", "0034", 34, 9, List.of()));
    @TempDir Path output;

    @Test
    void sixEquipableEntriesKeepEveryOriginalAndIndependentMaterial() throws Exception {
        Fixture f = fixture();
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        assertEquals(f.weaponCount + 6, f.mek.getMekWeaponInfoMap().size(), "initializer/internal WAZ cannot fill six equipment slots");
        assertEquals(f.skillCount + 6, f.waz.getSkillList().size());
        assertSix(f.mek, f.waz);
        assertOriginals(f);
        for (Variant v : VARIANTS) {
            int row = row(f.mek, v.name);
            var source = f.mek.getMekMaterialBlock().getTrailingEntries().get(v.sourceRow);
            var copied = f.mek.getMekMaterialBlock().getTrailingEntries().get(row);
            assertNotSame(source, copied);
            assertMaterial(copied, source);
            for (int donor : v.donors) assertMaterial(copied, f.mek.getMekMaterialBlock().getTrailingEntries().get(donor));
            assertNotSame(source.getSpriteGroups(), copied.getSpriteGroups());
            for (int i = 0; i < source.getSpriteGroups().size(); i++) assertNotSame(source.getSpriteGroups().get(i), copied.getSpriteGroups().get(i));
        }
        var source = events(skill(f.waz, "WARP_P1_08"), 1, 48, CEventEffect.class).get(0);
        var copy = events(skill(f.waz, "DX_ORE_DOUBLE_TAP"), 1, 48, CEventEffect.class).get(1);
        assertNotSame(source.getCeventEffectUnitList(), copy.getCeventEffectUnitList());
        JsonNode original = tree(source);
        select(copy).setWazSequenceNo(999);
        assertEquals(original, tree(source));
        assertSources(f);
    }

    @Test
    void generateParseAndReapplyKeepAllSixAndThePublicRepair() throws Exception {
        Fixture f = fixture();
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        JsonNode mek = tree(f.mek), waz = tree(f.waz), common = tree(f.host);
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context);
        assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz)); assertEquals(common, tree(f.host));
        BsdxBinService service = new BsdxBinService();
        Path m = output.resolve("misaki.mek"), w = output.resolve("misaki.waz");
        service.generate(m.toString(), f.mek, CS); service.generate(w.toString(), f.waz, CS);
        Mek parsedMek = (Mek) service.parse(m.toString(), CS).getData();
        Waz parsedWaz = (Waz) service.parse(w.toString(), CS).getData();
        assertSix(parsedMek, parsedWaz);
        assertOre(parsedWaz); assertHands(parsedWaz); assertSpear(parsedWaz); assertSword(parsedWaz); assertFloor(parsedWaz);
        Path m2 = output.resolve("misaki-again.mek"), w2 = output.resolve("misaki-again.waz");
        service.generate(m2.toString(), parsedMek, CS); service.generate(w2.toString(), parsedWaz, CS);
        assertArrayEquals(Files.readAllBytes(m), Files.readAllBytes(m2));
        assertArrayEquals(Files.readAllBytes(w), Files.readAllBytes(w2));
        assertSources(f);
    }

    @Test
    void oreHasTwoCopiedLaunchesAndAnOrdinaryGateAfterTheSecond() throws Exception {
        Fixture f = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertOre(f.waz);
        var original = events(skill(f.waz, "WARP_P1_08"), 1, 48, CEventEffect.class).get(0);
        var copies = events(skill(f.waz, "DX_ORE_DOUBLE_TAP"), 1, 48, CEventEffect.class);
        assertCopiedEffect(original, copies.get(0), 0); assertCopiedEffect(original, copies.get(1), 10);
    }

    @Test
    void handsHaveAtMostTwoAttemptsAndNoCounterOrHeldButtonBackEdge() throws Exception {
        Fixture f = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertHands(f.waz);
        Waz.Skill copy = skill(f.waz, "DX_HAND_TWO_REACH"), source = skill(f.waz, "WARP_P1_06");
        for (int p : List.of(1, 2)) assertEquals(tree(events(source, p, 48, CEventEffect.class)), tree(events(copy, p, 48, CEventEffect.class)));
    }

    @Test
    void shortSpearClipsEveryInnerLaunchWindowAndRestoresBeforeOrdinaryCancel() throws Exception {
        Fixture f = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertSpear(f.waz);
    }

    @Test
    void swordHasLandingAndTimeoutRecoveryAndSafeOrdinaryAndSdDoors() throws Exception {
        Fixture f = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertSword(f.waz);
        Waz.Skill source = skill(f.waz, "S_HAMMER06"), copy = skill(f.waz, "DX_CRYSTAL_SWORD_RETURN");
        assertEquals(tree(events(source, 3, 48, CEventEffect.class)), tree(events(copy, 3, 48, CEventEffect.class)), "10f explosion and visual payloads survive recovery edits");
    }

    @Test
    void floorPreservesTwoIndependentTargetSnapshotsAndFiniteSourceLifetime() throws Exception {
        Fixture f = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context); assertFloor(f.waz);
        var original = events(skill(f.waz, "G_BIT07"), 0, 48, CEventEffect.class).get(0);
        var copies = events(skill(f.waz, "DX_FLOOR_DOUBLE_MARK"), 0, 48, CEventEffect.class);
        assertCopiedEffect(original, copies.get(0), 18); assertCopiedEffect(original, copies.get(1), 34);
        assertNotSame(copies.get(0).getCeventEffectUnitList(), copies.get(1).getCeventEffectUnitList());
        assertEquals(List.of(1, 0), ((CEventTerm) effectData(copies.get(1), 7)).getBsdxInfoCollectionList().get(0).getIntList3());
        assertEquals(-9999, ((CEventValRandom) effectData(copies.get(1), 11)).getInt5());
        // Resolve this fixture's real append plan, never a C-drive build or a guessed final 488.
        Waz.Skill publicFloor = f.host.getSkillList().get(select(copies.get(0)).getWazSequenceNo());
        assertTrue(publicFloor.getPhasesInfo().stream().flatMap(p -> p.getSkillUnitCollection().stream())
                .flatMap(u -> u.getSkillInfoObjectList().stream()).filter(CEventChange.class::isInstance)
                .map(CEventChange.class::cast).anyMatch(c -> c.getBsdxInfoCollectionList1().stream()
                        .anyMatch(t -> List.of(2, 2, 1).equals(t.getTypeList()) && t.getIntList3().isEmpty())), "INDEX_OVER remains an object-free condition");
        assertTrue(publicFloor.getPhasesInfo().stream().flatMap(p -> p.getSkillUnitCollection().stream())
                .flatMap(u -> u.getSkillInfoObjectList().stream()).filter(CEventChange.class::isInstance)
                .anyMatch(e -> Integer.valueOf(184).equals(e.getStartFrame())), "source timeout remains finite");
    }

    @Test
    void eachNewDonorEventDriftRejectsWithoutAppendingAnyNewVariant() throws Exception {
        for (String en : List.of("WARP_P1_08", "WARP_P1_06", "BEAM04", "S_HAMMER06", "G_BIT07")) {
            Fixture f = fixture();
            Waz.Skill source = skill(f.waz, en);
            var changes = events(source, "G_BIT07".equals(en) ? 0 : 1, 71, CEventChange.class);
            assertFalse(changes.isEmpty()); changes.get(0).setEndFrame(999);
            assertRejectedWithoutNewAppend(f);
        }
    }

    @Test
    void donorSuffixDriftRejectsAndAnExistingNewVariantCannotHideTampering() throws Exception {
        Fixture f = fixture(); skill(f.waz, "WARP_P1_08").getSkillSuffixList().get(1).setInt1(99);
        assertRejectedWithoutNewAppend(f);
        Fixture applied = fixture(); MisakiCustomizer.INSTANCE.customizeBeforeWrite(applied.context);
        event(skill(applied.waz, "DX_ORE_DOUBLE_TAP"), 1, 48, CEventEffect.class, 10, 10).setStartFrame(11);
        JsonNode mek = tree(applied.mek), waz = tree(applied.waz);
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(applied.context));
        assertEquals(mek, tree(applied.mek)); assertEquals(waz, tree(applied.waz));
    }

    private static void assertOre(Waz waz) {
        Waz.Skill s = skill(waz, "DX_ORE_DOUBLE_TAP");
        assertEquals(List.of(0, 10), events(s, 1, 48, CEventEffect.class).stream().map(CEventEffect::getStartFrame).toList());
        for (var e : events(s, 1, 48, CEventEffect.class)) { assertEquals(e.getStartFrame(), e.getEndFrame()); assertEquals(1, ((CEventValRandom) effectData(e, 4)).getInt5()); }
        assertEquals(0, gate(s, 1, 10)); assertEquals(1, gate(s, 1, 11)); assertEquals(1, gate(s, 2, 0));
        next(event(s, 0, 71, CEventChange.class, 9, 9)); next(event(s, 1, 71, CEventChange.class, 18, 18));
        next(event(s, 2, 71, CEventChange.class, 18, 18)); bounded(s, 1, 18); bounded(s, 2, 18);
    }

    private static void assertHands(Waz waz) {
        Waz.Skill s = skill(waz, "DX_HAND_TWO_REACH");
        for (int slot : List.of(27, 28)) {
            CEventVal preparation = event(s, 0, slot, CEventVal.class, 0, 17);
            assertEquals(List.of(0, 100, 0, 100), List.of(preparation.getInt1(), preparation.getInt2(), preparation.getInt3(), preparation.getInt4()), "new finite grab cannot inherit the native preparation multiplier10");
        }
        for (int p : List.of(1, 2, 3)) physical(s, p, 0);
        var enter = event(s, 0, 71, CEventChange.class, 40, 40); always(enter); phase(enter, 1);
        List<CEventChange> first = events(s, 1, 71, CEventChange.class);
        assertFalse(first.isEmpty()); assertEquals(List.of(2, 0, 4, 5), first.get(0).getBsdxInfoCollectionList1().get(0).getTypeList());
        assertEquals(List.of(0), first.get(0).getBsdxInfoCollectionList1().get(0).getParamList()); phase(first.get(0), 3);
        assertTrue(first.stream().skip(1).allMatch(c -> List.of(0).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())), "no inherited PARAMCOUNT repetition");
        for (var c : first.subList(1, first.size())) phase(c, 2);
        assertTrue(first.size() >= 2, "failed first grab must reach second attempt");
        var second = events(s, 2, 71, CEventChange.class); assertFalse(second.isEmpty()); for (var c : second) phase(c, 3);
        for (int p : List.of(1, 2)) assertEquals(2, events(s, p, 48, CEventEffect.class).size(), "one hand plus one trail, not two damage hands");
        assertEquals(1, gate(s, 2, 2)); assertNotEquals(1, gate(s, 3, 0), "successful grab cleanup cannot be blindly cancelled");
        next(event(s, 3, 71, CEventChange.class, 61, 61));
    }

    private static void assertSpear(Waz waz) {
        Waz.Skill s = skill(waz, "DX_CRYSTAL_SHORT_SPEAR"); next(event(s, 0, 71, CEventChange.class, 14, 14));
        var effects = events(s, 1, 48, CEventEffect.class); assertEquals(1, effects.size());
        var e = effects.get(0); assertEquals(2, e.getStartFrame()); assertEquals(18, e.getEndFrame());
        for (var u : e.getCeventEffectUnitList()) if (u.getData() != null && u.getBuffer() != 0) {
            assertEquals(0, u.getData().getStartFrame()); assertEquals(16, u.getData().getEndFrame(), "all emitter-relative windows must be clipped");
        }
        assertNotEquals(1, gate(s, 1, 2)); assertNotEquals(1, gate(s, 1, 18));
        next(event(s, 1, 71, CEventChange.class, 18, 18));
        physical(s, 2, 0); assertEquals(1, gate(s, 2, 0)); next(event(s, 2, 71, CEventChange.class, 17, 17));
        event(s, 2, 24, CEventVal.class, 8, 17); bounded(s, 2, 17);
    }

    private static void assertSword(Waz waz) {
        Waz.Skill s = skill(waz, "DX_CRYSTAL_SWORD_RETURN");
        var changes = events(s, 2, 71, CEventChange.class);
        assertTrue(changes.stream().anyMatch(c -> List.of(2, 7, 16).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())), "landing route remains present");
        for (var c : changes) localPhase(c, 2, 3);
        for (var c : changes) if (List.of(2, 7, 16).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())) phase(c, 3);
        CEventChange timeout = event(s, 2, 71, CEventChange.class, 72, 72); always(timeout); localPhase(timeout, 2, 3);
        physical(s, 2, 0); physical(s, 3, 0);
        for (int p : List.of(2, 3)) for (CEventValRandom g : events(s, p, 21, CEventValRandom.class))
            assertTrue(g.getInt2() <= 50 && g.getInt5() <= 50, "SD cancellation must not carry G200");
        assertEquals(3, gate(s, 2, 0)); assertEquals(3, gate(s, 3, 17)); assertEquals(1, gate(s, 3, 18)); assertEquals(1, gate(s, 3, 78));
        assertTrue(events(s, 2, 47, CEventVal.class).stream().allMatch(v -> v.getEndFrame() <= 71));
        next(event(s, 3, 71, CEventChange.class, 78, 78));
    }

    private static void assertFloor(Waz waz) {
        Waz.Skill s = skill(waz, "DX_FLOOR_DOUBLE_MARK");
        assertEquals(List.of(18, 34), events(s, 0, 48, CEventEffect.class).stream().map(CEventEffect::getStartFrame).toList());
        assertNotEquals(1, gate(s, 0, 34)); assertEquals(1, gate(s, 0, 35));
        event(s, 0, 24, CEventVal.class, 42, 61); next(event(s, 0, 71, CEventChange.class, 61, 61)); bounded(s, 0, 61);
    }

    private static Fixture fixture() throws Exception {
        Path mp = Path.of("src/main/resources/game/bhe/mek/misaki/misaki.mek"), wp = Path.of("src/main/resources/game/bhe/waz/misaki/misaki.waz"), cp = Path.of("src/main/resources/game/bhe/waz/tama05.waz");
        Map<Path, byte[]> files = new LinkedHashMap<>(); for (Path p : List.of(mp, wp, cp)) files.put(p, Files.readAllBytes(p));
        BheBinService service = new BheBinService();
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mp.toString(), CS).getData();
        var rawWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wp.toString(), CS).getData();
        var rawCommon = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(cp.toString(), CS).getData();
        List<Object> raw = List.of(rawMek, rawWaz, rawCommon); List<JsonNode> rawTrees = raw.stream().map(MisakiSixVariantsTest::tree).toList();
        Mek mek = new BheToBsdxMekConverter().convert(rawMek); Waz waz = new BheToBsdxWazConverter().convert(rawWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "MisakiSixVariantsTest"); mek.getMekBasicInfo().setWazFileSequence(233);
        for (var s : waz.getSkillList()) if (s != null) for (int p = 0; p < s.getPhasesInfo().size(); p++) for (var e : events(s, p, 48, CEventEffect.class)) {
            CEventWazaSelect target = select(e); if (Integer.valueOf(5).equals(target.getWazFileNo())) { target.setWazFileNo(9); target.setWazSequenceNo(32 + target.getWazSequenceNo()); }
        }
        Waz common = new BheToBsdxWazConverter().convert(rawCommon);
        TsukuyomiRawSourceBundle rawBundle = new TsukuyomiRawSourceBundle(); rawBundle.getCommonProjectileWazByFileName().put("tama05.waz", rawCommon);
        TsukuyomiPackageBundle bundle = new TsukuyomiPackageBundle(); bundle.getWazByFileName().put("tama05.waz", common);
        BheCommonProjectileAppendPlan plan = new BheCommonProjectileAppendPlan();
        plan.getSourceWazIndexToTargetIndex().put(5, 9); plan.getSourceWazIndexToTargetSkillBase().put(5, 32);
        plan.getSourceWazIndexToTargetSkillCount().put(5, common.getSkillList().size()); plan.getSourceWazIndexToTargetFileName().put(5, "tama05.waz");
        TsukuyomiConvertedBundle converted = new TsukuyomiConvertedBundle(); converted.setRawSourceBundle(rawBundle); converted.setCommonProjectileResourceBundle(bundle); converted.setCommonProjectileAppendPlan(plan);
        Waz host = new Waz(); for (int i = 0; i < 32; i++) host.getSkillList().add(new Waz.Skill()); host.getSkillList().addAll(common.getSkillList());
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle(); baseline.getWazByFileName().put("tama05.waz", host);
        FollowupMekaContext context = new FollowupMekaContext(); context.setConvertedBundle(converted); context.setBaseline(baseline);
        // Existing both-null public-only callers must still repair and return normally.
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(context);
        context.setReboundMek(mek); context.setReboundWaz(waz);
        return new Fixture(mek, waz, host, context, mek.getMekWeaponInfoMap().size(), waz.getSkillList().size(), tree(mek.getMekWeaponInfoMap()), tree(waz.getSkillList()), tree(mek.getMekMaterialBlock().getTrailingEntries()), raw, rawTrees, files);
    }

    private static void assertSix(Mek mek, Waz waz) throws Exception {
        assertEquals(6, mek.getMekWeaponInfoMap().values().stream().filter(w -> Integer.valueOf(0).equals(w.getWeaponCategory()) && w.getWazSequence() >= 0 && w.getWazSequence() < waz.getSkillList().size() && waz.getSkillList().get(w.getWazSequence()).getSkillNameEnglish().startsWith("DX_")).count());
        Set<Integer> identities = new HashSet<>();
        for (Variant v : VARIANTS) {
            int index = row(mek, v.name); assertTrue(index <= 152); var weapon = mek.getMekWeaponInfoMap().get(index); var s = skill(waz, v.english);
            assertEquals(v.name, s.getSkillNameJapanese()); assertEquals(v.sequence, weapon.getWeaponSequence()); assertEquals(0, weapon.getWeaponCategory());
            assertEquals(waz.getSkillList().indexOf(s), weapon.getWazSequence()); assertTrue(identities.add(weapon.getWazSequence()));
            assertEquals(v.heat, weapon.getHeatMinConsumption()); assertEquals(v.heat, weapon.getHeatMaxConsumption()); assertEquals(0, weapon.getUpgradeExp());
            assertFalse(weapon.getWeaponDescription().isBlank()); cp932(v.name); cp932(weapon.getWeaponDescription());
            var source = waz.getSkillList().get(mek.getMekWeaponInfoMap().get(v.sourceRow).getWazSequence());
            assertEquals(source.getPhaseQuantity(), s.getPhaseQuantity()); assertEquals(tree(source.getSkillSuffixList()), tree(s.getSkillSuffixList()));
        }
        assertEquals(mek.getMekWeaponInfoMap().size(), mek.getMekMaterialBlock().getTrailingEntries().size());
    }
    private static void assertRejectedWithoutNewAppend(Fixture f) {
        JsonNode weapons = tree(f.mek.getMekWeaponInfoMap()), skills = tree(f.waz.getSkillList()), materials = tree(f.mek.getMekMaterialBlock().getTrailingEntries());
        assertAll(() -> assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context)),
                () -> { assertTrue(f.mek.getMekWeaponInfoMap().size() <= f.weaponCount + 1, "new batch must leave no partial equipment"); assertTrue(f.waz.getSkillList().size() <= f.skillCount + 1, "new batch must leave no partial WAZ");
                    for (int i = 0; i < f.weaponCount; i++) assertEquals(weapons.get("" + i), tree(f.mek.getMekWeaponInfoMap().get(i)));
                    assertEquals(skills, tree(f.waz.getSkillList().subList(0, f.skillCount))); assertEquals(materials, tree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, f.weaponCount)));
                    for (Variant v : VARIANTS.subList(1, 6)) assertTrue(f.waz.getSkillList().stream().noneMatch(s -> v.english.equals(s.getSkillNameEnglish()))); });
    }
    private static void assertOriginals(Fixture f) {
        for (int i = 0; i < f.weaponCount; i++) assertEquals(f.weapons.get("" + i), tree(f.mek.getMekWeaponInfoMap().get(i)));
        assertEquals(f.skills, tree(f.waz.getSkillList().subList(0, f.skillCount)));
        assertEquals(f.materials, tree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, f.weaponCount)));
    }
    private static void assertSources(Fixture f) throws Exception { for (int i = 0; i < f.raw.size(); i++) assertEquals(f.rawTrees.get(i), tree(f.raw.get(i))); for (var e : f.files.entrySet()) assertArrayEquals(e.getValue(), Files.readAllBytes(e.getKey())); }
    private static void assertMaterial(Mek.MekMaterialBlock.PluginEntry copy, Mek.MekMaterialBlock.PluginEntry donor) { groups(copy.getSpriteGroups(), donor.getSpriteGroups()); groups(copy.getSeGroups(), donor.getSeGroups()); groups(copy.getVoiceGroups(), donor.getVoiceGroups()); }
    private static void groups(List<int[]> actual, List<int[]> donor) { assertNotSame(actual, donor); assertTrue(actual.size() >= donor.size()); for (int i = 0; i < donor.size(); i++) { assertNotSame(actual.get(i), donor.get(i)); for (int v : donor.get(i)) { final int group = i; assertTrue(Arrays.stream(actual.get(group)).anyMatch(x -> x == v)); } } }
    private static void cp932(String value) throws Exception { Charset cs = Charset.forName(CS); assertEquals(value, cs.decode(cs.newEncoder().encode(CharBuffer.wrap(value))).toString()); }
    private static int row(Mek mek, String name) { var rows = mek.getMekWeaponInfoMap().entrySet().stream().filter(e -> name.equals(e.getValue().getWeaponName())).toList(); assertEquals(1, rows.size(), "one equipment identity: " + name); return rows.get(0).getKey(); }
    private static Waz.Skill skill(Waz waz, String en) { var found = waz.getSkillList().stream().filter(Objects::nonNull).filter(s -> en.equals(s.getSkillNameEnglish())).toList(); assertEquals(1, found.size(), "one WAZ identity: " + en); return found.get(0); }
    private static <T extends SkillInfoObject> List<T> events(Waz.Skill s, int p, int slot, Class<T> type) { return s.getPhasesInfo().get(p).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).flatMap(u -> u.getSkillInfoObjectList().stream()).filter(type::isInstance).map(type::cast).toList(); }
    private static <T extends SkillInfoObject> T event(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) { var found = events(s, p, slot, type).stream().filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame())).toList(); assertEquals(1, found.size(), s.getSkillNameEnglish() + " P" + p + " slot" + slot + " " + start + ".." + end); return found.get(0); }
    private static SkillInfoObject effectData(CEventEffect effect, int slot) { var found = effect.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && u.getBuffer() != 0).toList(); assertEquals(1, found.size()); return found.get(0).getData(); }
    private static CEventWazaSelect select(CEventEffect e) { return assertInstanceOf(CEventWazaSelect.class, effectData(e, 3)); }
    private static void assertCopiedEffect(CEventEffect source, CEventEffect actual, int frame) { ObjectNode expected = tree(source).deepCopy(); expected.put("startFrame", frame); expected.put("endFrame", frame); assertEquals(expected, tree(actual), "copy preserves amount, target expression and all relative child clocks"); }
    private static void always(CEventChange c) { assertEquals(1, c.getFlag()); assertEquals(List.of(0), c.getBsdxInfoCollectionList1().get(0).getTypeList()); }
    private static void phase(CEventChange c, int p) { assertEquals(List.of(0, 1), c.getBsdxInfoCollectionList2().get(0).getTypeList()); assertEquals(List.of(p), c.getBsdxInfoCollectionList2().get(0).getParamList()); }
    private static void localPhase(CEventChange c, int from, int to) { var target = c.getBsdxInfoCollectionList2().get(0); if (List.of(0, 0).equals(target.getTypeList())) assertEquals(from + 1, to); else phase(c, to); }
    private static void next(CEventChange c) { always(c); assertEquals(List.of(0, 0), c.getBsdxInfoCollectionList2().get(0).getTypeList()); }
    private static void physical(Waz.Skill s, int p, int frame) { var gravity = events(s, p, 21, CEventValRandom.class).stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && Integer.valueOf(frame).equals(e.getEndFrame())).toList(); assertFalse(gravity.isEmpty(), "explicit gravity restore before cancellation"); for (var g : gravity) { assertEquals(0, g.getInt4()); assertEquals(50, g.getInt5()); } for (int slot : List.of(27, 28)) { var modifiers = events(s, p, slot, CEventVal.class).stream().filter(e -> Integer.valueOf(frame).equals(e.getStartFrame()) && Integer.valueOf(frame).equals(e.getEndFrame())).toList(); assertFalse(modifiers.isEmpty(), "explicit multiplier restore before cancellation"); for (var v : modifiers) { assertEquals(0, v.getInt3()); assertEquals(100, v.getInt4()); } } }
    /** Slot47 type0 absolute interpolation only; 3 is an SD door, never a normal door. */
    private static int gate(Waz.Skill s, int p, int frame) { int result = 0; for (int t = 0; t <= frame; t++) for (var v : events(s, p, 47, CEventVal.class)) if (v.getStartFrame() <= t && t <= v.getEndFrame()) { assertEquals(0, v.getInt1()); assertEquals(0, v.getInt3()); int d = v.getEndFrame() - v.getStartFrame(); result = d == 0 ? v.getInt4() : v.getInt2() + (t - v.getStartFrame()) * (v.getInt4() - v.getInt2()) / d; } return result; }
    private static void bounded(Waz.Skill s, int p, int exit) { for (var u : s.getPhasesInfo().get(p).getSkillUnitCollection()) for (var e : u.getSkillInfoObjectList()) assertTrue(e.getEndFrame() <= exit, "no necessary sprite/motion tail may remain after exit: slot" + u.getUnitQuantity()); }
    private static JsonNode tree(Object value) { JsonNode n = JSON.valueToTree(value); clean(n); return n; }
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(MisakiSixVariantsTest::clean); }
    private record Variant(String name, String english, String sequence, int heat, int sourceRow, List<Integer> donors) { }
    private record Fixture(Mek mek, Waz waz, Waz host, FollowupMekaContext context, int weaponCount, int skillCount, JsonNode weapons, JsonNode skills, JsonNode materials, List<Object> raw, List<JsonNode> rawTrees, Map<Path, byte[]> files) { }
}
