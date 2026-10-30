package com.giga.nexas.transfer.bhe2bsdx.meka.yuri;

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
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

/** Real binary structural/event regression; this does not establish runtime playability. */
class YuriSixVariantsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final String ROLE = "yuri";
    private static final int TARGET_GROUP = 211;
    private static final List<Variant> VARIANTS = List.of(
            new Variant("ダガー・刻み", "DX_DAGGER_LINK", "ダガー", "0007", "PUNCH", 10, 1, false),
            new Variant("アサシンダガー・速刺", "DX_ASSASSIN_QUICK", "アサシンダガー", "STRAIGHT04", "0004", 24, 4, false),
            new Variant("サイドステップ・地走", "DX_SIDESTEP_GROUND", "サイドステップキック", "SIDESTEPKICK04", "NAGE", 14, 2, false),
            new Variant("パルス・三点射", "DX_PULSE_THREE", "パルスライフル", "RIFLE4", "0001", 12, 3, false),
            new Variant("狙撃・一射離脱", "DX_SNIPER_SINGLE", "スナイパーライフル", "RIFLE04", "0002", 42, 4, false),
            new Variant("マイン・置き逃げ", "DX_MINE_RETREAT", "マインスイーパ：避", "MINE_S05", "0009", 16, 2, false));

    private static void customize(Fixture f) {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setReboundMek(f.mek());
        context.setReboundWaz(f.waz());
        YuriCustomizer.INSTANCE.customizeBeforeWrite(context);
    }

    private static void applyKnownLegacyBaseline(Fixture f) {
        var dagger = skill(f.waz(), "アサシンダガー", "STRAIGHT04");
        CEventHit hit = event(dagger, 3, 41, CEventHit.class, 46, 48);
        hit.setStartFrame(44);
        hit.setEndFrame(50);
        power(hit).getBsdxInfoCollectionList().get(0).setParamList(new ArrayList<>(List.of(140)));
        CEventChange exit = event(dagger, 3, 71, CEventChange.class, 46, 48);
        exit.setStartFrame(44);
        exit.setEndFrame(50);
        event(skill(f.waz(), "サイドステップキック", "SIDESTEPKICK04"),
                1, 18, CEventValRandom.class, 14, 28).setInt2(40);
        var daggerWeapon = weapon(f.mek(), "アサシンダガー", "0004");
        daggerWeapon.setHeatMinConsumption(28);
        daggerWeapon.setHeatMaxConsumption(28);
        daggerWeapon.setUpgradeExp(0);
        daggerWeapon.setWeaponDescription("踏み込みから鋭く刺し込む一撃。攻撃の有効時間と単発威力を強化し、近距離での反撃に向く。");
        var sideWeapon = weapon(f.mek(), "サイドステップキック", "NAGE");
        sideWeapon.setHeatMinConsumption(12);
        sideWeapon.setHeatMaxConsumption(12);
        sideWeapon.setUpgradeExp(0);
        sideWeapon.setWeaponDescription("側方へ踏み込みながら蹴り上げる攻撃。位置と高さを変え、相手との間合いを見て次の攻めを選ぶ。");
        f.mek().getMekBasicInfo().setMekDescription(
                "ダガーとエネルギーガンを使い分ける高速戦闘型。サイドステップキックで位置を変え、アサシンダガーで鋭く刺し込む。近距離の切り返しと射撃による牽制を組み合わせ、同じ場所に留まらず戦いたい。");
    }

    private static List<Consumer<Fixture>> newDonorCorruptions() {
        return List.of(
                f -> event(skill(f.waz(), "マインスイーパ：避", "MINE_S05"), 1, 18, CEventValRandom.class, 10, 25).setInt2(999),
                f -> events(skill(f.waz(), "マインスイーパ：避", "MINE_S05"), 1, 48).get(0).setStartFrame(9),
                f -> skill(f.waz(), "マインスイーパ：避", "MINE_S05").setSkillNameJapanese("マイン・漂移"),
                f -> event(skill(f.waz(), "パルスライフル", "RIFLE4"), 1, 35, CEventVal.class, 0, 0).setInt4(16));
    }

    private static void assertSemantics(Waz waz, Mek mek) {
        Waz.Skill originalDagger = skill(waz, "ダガー", "0007");
        Waz.Skill dagger = variant(waz, 1);
        assertEquals(List.of(0, 16, 0, 0, 4, 0), six(event(dagger, 1, 18, CEventValRandom.class, 11, 12)));
        assertGravity(dagger, 1, 0);
        assertGate(dagger, 1, 14, event(originalDagger, 1, 47, CEventVal.class, 20, 20));
        assertEquals(2, event(dagger, 1, 47, CEventVal.class, 14, 14).getInt4(),
                "the dagger's conditional unlock is not an ordinary gate value 1");
        assertEquals(payload(events(originalDagger, 1, 48)), payload(events(dagger, 1, 48)));
        assertEquals(1, effectsLike(dagger, 1, originalDagger, 1, 13).size());
        assertNormalEnd(dagger, 1, 43);

        Waz.Skill originalAssassin = skill(waz, "アサシンダガー", "STRAIGHT04");
        Waz.Skill quick = variant(waz, 2);
        CEventHit fastHit = event(quick, 3, 41, CEventHit.class, 29, 33);
        assertPower(fastHit, 100);
        CEventChange fastHitExit = event(quick, 3, 71, CEventChange.class, 29, 33);
        assertEquals(body(event(originalAssassin, 3, 71, CEventChange.class, 44, 50)), body(fastHitExit),
                "hit-driven self-stop remains conditional, it is not replaced by an ordinary cancel");
        assertEquals(body(event(originalAssassin, 3, 71, CEventChange.class, 92, 92)),
                body(event(quick, 3, 71, CEventChange.class, 61, 61)));
        event(quick, 3, 18, CEventValRandom.class, 24, 28);
        event(quick, 3, 18, CEventValRandom.class, 29, 30);
        assertGravity(quick, 3, 0);
        assertCompressedClocks(originalAssassin, quick, 3, 2, 3);
        for (int phase : List.of(1, 2, 4))
            assertEquals(payload(originalAssassin.getPhasesInfo().get(phase - 1)), payload(quick.getPhasesInfo().get(phase - 1)));

        Waz.Skill originalSide = skill(waz, "サイドステップキック", "SIDESTEPKICK04");
        Waz.Skill ground = variant(waz, 3);
        assertTrue(events(ground, 1, 20).isEmpty(), "remove the whole tracking-height jump, including hidden max-height payload");
        assertEquals(List.of(0, 0, 0, 0, 0, 0), six(event(ground, 1, 19, CEventValRandom.class, 29, 46)));
        assertEquals(List.of(0, 0, 0, 0, 0, 0), six(event(ground, 2, 19, CEventValRandom.class, 0, 9)));
        assertEquals(0, event(ground, 1, 19, CEventValRandom.class, 0, 0).getInt5());
        assertGravity(ground, 1, 0);
        assertGravity(ground, 2, 0);
        assertEquals(List.of(0, 24, 0, 0, 4, 0), six(event(ground, 1, 18, CEventValRandom.class, 14, 28)));
        assertPower(event(ground, 1, 41, CEventHit.class, 18, 42), 80);
        Waz.Skill pulseDonor = skill(waz, "パルスライフル", "RIFLE4");
        CEventVal normalGate = event(pulseDonor, 2, 47, CEventVal.class, 3, 3);
        assertEquals(1, normalGate.getInt4());
        assertGate(ground, 1, 43, normalGate);
        assertEquals(payload(events(originalSide, 1, 71)), payload(events(ground, 1, 71)));
        assertEquals(payload(events(originalSide, 2, 71)), payload(events(ground, 2, 71)));

        Waz.Skill pulse = variant(waz, 4);
        CEventVal budget = event(pulse, 1, 35, CEventVal.class, 0, 0);
        assertEquals(List.of(0, 0, 0, 3), List.of(budget.getInt1(), budget.getInt2(), budget.getInt3(), budget.getInt4()));
        CEventVal consume = event(pulse, 2, 35, CEventVal.class, 0, 0);
        assertEquals(List.of(0, 0, 1, -1), List.of(consume.getInt1(), consume.getInt2(), consume.getInt3(), consume.getInt4()));
        assertEquals(payload(events(pulseDonor, 2, 71)), payload(events(pulse, 2, 71)),
                "finite budget, heat and button-release checks must remain on the private loop");
        assertEquals(payload(events(pulseDonor, 2, 48)), payload(events(pulse, 2, 48)));
        assertEquals(1, effectsLike(pulse, 2, pulseDonor, 2, 3).size(), "one real shot per finite iteration");
        assertGate(pulse, 2, 3, normalGate);
        assertNormalEnd(pulse, 3, 23);

        Waz.Skill sniperDonor = skill(waz, "スナイパーライフル", "RIFLE04");
        Waz.Skill sniper = variant(waz, 5);
        assertCompressedClocks(sniperDonor, sniper, 1, 3, 5);
        assertEquals(payload(sniperDonor.getPhasesInfo().get(1)), payload(sniper.getPhasesInfo().get(1)),
                "aim child, release conditions and 200-frame timeout are preserved");
        assertEquals(payload(sniperDonor.getPhasesInfo().get(3)), payload(sniper.getPhasesInfo().get(3)));
        List<CEventEffect> sniperShots = effectsLike(sniper, 3, sniperDonor, 3, 3);
        assertEquals(1, sniperShots.size());
        assertEquals(3, sniperShots.get(0).getStartFrame());
        assertEquals(body(effectsLike(sniperDonor, 3, sniperDonor, 3, 3).get(0)), body(sniperShots.get(0)));
        assertTrue(events(sniper, 3, 48).stream().allMatch(e -> e.getEndFrame() <= 24),
                "post-exit cosmetic consumers must be removed");
        assertNormalEnd(sniper, 3, 24);
        assertEquals(1, events(sniper, 3, 71).size(), "no old 57-frame resource re-entry survives the single-shot exit");
        assertEquals(0, event(sniper, 3, 18, CEventValRandom.class, 3, 7).getInt2());
        assertEquals(0, event(sniper, 3, 18, CEventValRandom.class, 3, 7).getInt5());

        Waz.Skill mineDonor = skill(waz, "マインスイーパ：避", "MINE_S05");
        Waz.Skill mine = variant(waz, 6);
        assertGravity(mine, 1, 0);
        assertGravity(mine, 1, 10);
        assertEquals(List.of(0, 0, 0, 0, 0, 0), six(event(mine, 1, 19, CEventValRandom.class, 10, 25)));
        assertEquals(0, event(mine, 1, 19, CEventValRandom.class, 0, 0).getInt5());
        assertEquals(List.of(0, 16, 0, 0, 0, 0), six(event(mine, 1, 18, CEventValRandom.class, 10, 25)));
        CEventValRandom stop = event(mine, 1, 18, CEventValRandom.class, 26, 26);
        assertEquals(0, stop.getInt2());
        assertEquals(0, stop.getInt5());
        assertEquals(1, effectsLike(mine, 1, mineDonor, 1, 10).size(), "one native mine, without multiplying request count");
        assertEquals(payload(events(mineDonor, 1, 48)), payload(events(mine, 1, 48)));
        for (int phase : List.of(1, 2)) {
            assertEquals(payload(events(mineDonor, phase, 71)), payload(events(mine, phase, 71)));
            assertEquals(payload(events(mineDonor, phase, 47)), payload(events(mine, phase, 47)));
        }
    }

    private static void assertCompressedClocks(Waz.Skill donor, Waz.Skill target, int phase, int numerator, int denominator) {
        for (var unit : donor.getPhasesInfo().get(phase - 1).getSkillUnitCollection()) {
            int slot = unit.getUnitQuantity();
            if (slot == 21) continue; // the quick stab adds an explicit entry gravity
            List<SkillInfoObject> original = unit.getSkillInfoObjectList();
            List<SkillInfoObject> copy = events(target, phase, slot);
            assertEquals(original.size(), copy.size(), "top-level event count at slot " + slot);
            for (int i = 0; i < original.size(); i++) {
                assertEquals(original.get(i).getStartFrame() * numerator / denominator, copy.get(i).getStartFrame());
                assertEquals(original.get(i).getEndFrame() * numerator / denominator, copy.get(i).getEndFrame());
                assertEquals(original.get(i).getTypeId(), copy.get(i).getTypeId());
                if (original.get(i) instanceof CEventHit oldHit && copy.get(i) instanceof CEventHit newHit) {
                    assertEquals(combatExceptDirectPower(oldHit), combatExceptDirectPower(newHit),
                            "nested attack parameters and hit physics must not be recursively retimed");
                }
            }
        }
    }

    private static JsonNode combatExceptDirectPower(CEventHit hit) {
        ObjectNode node = (ObjectNode) body(hit);
        for (JsonNode unit : node.path("ceventHitUnitList"))
            if (unit.path("unitSlotNum").asInt(-1) == 6) ((ObjectNode) unit).remove("data");
        return node;
    }

    @TempDir
    Path output;

    @Test
    void lateMineSuffixDriftRejectsBeforeAnyAdditionalRowIsPublished() throws Exception {
        Fixture f = fixture();
        skill(f.waz(), "マインスイーパ：避", "MINE_S05").getSkillSuffixList().get(0).setInt1(1);
        assertRejectedWithoutNewRows(f);
    }

    @Test
    void groundedSidestepReadsTheNormalGateAt43InsteadOfTheEarlierLongSdEvent() throws Exception {
        Fixture f = fixture();
        var donor = skill(f.waz(), "サイドステップキック", "SIDESTEPKICK04");
        JsonNode sourceCancel = JSON.valueToTree(events(donor, 1, 47));
        customize(f);
        var ground = variant(f.waz(), 3);
        assertEquals(1, firstActiveGate(ground, 1, 43).getInt4(),
                "native 63A7D0 returns the first active event; an overlapping SD value 3 hides the normal gate");
        CEventVal sd = event(ground, 1, 47, CEventVal.class, 0, 42);
        assertEquals(List.of(0, 3, 0, 3), List.of(sd.getInt1(), sd.getInt2(), sd.getInt3(), sd.getInt4()));
        assertEquals(sourceCancel, JSON.valueToTree(events(donor, 1, 47)), "only the private SD window is shortened");
        assertSourceUnchanged(f);
    }

    private static CEventVal firstActiveGate(Waz.Skill skill, int phase, int clock) {
        var active = events(skill, phase, 47).stream().filter(CEventVal.class::isInstance).map(CEventVal.class::cast)
                .filter(e -> e.getStartFrame() <= clock && clock <= e.getEndFrame()).toList();
        assertFalse(active.isEmpty(), "a gate event must be active at the intended ordinary cancel clock");
        return active.get(0);
    }

    private static void assertRejectedWithoutNewRows(Fixture f) throws Exception {
        JsonNode beforeMek = JSON.valueToTree(f.mek());
        JsonNode beforeWaz = JSON.valueToTree(f.waz());
        assertThrows(IllegalStateException.class, () -> customize(f),
                "a donor's exact reviewed suffix or cancel-loop signature is required on a fresh old baseline");
        assertEquals(beforeMek, JSON.valueToTree(f.mek()), "reject without any new MEK or Material row");
        assertEquals(beforeWaz, JSON.valueToTree(f.waz()), "reject without any new private WAZ skill");
        assertSourceUnchanged(f);
    }

    private static List<List<Integer>> suffixPairs(Waz.Skill skill) {
        return skill.getSkillSuffixList().stream().map(e -> List.of(e.getInt1(), e.getInt2())).toList();
    }

    private static List<List<Integer>> reviewedSuffix(Variant variant) {
        return switch (variant.english()) {
            case "DX_DAGGER_LINK" -> List.of(List.of(0, 0));
            case "DX_SIDESTEP_GROUND", "DX_MINE_RETREAT" -> List.of(List.of(0, 0), List.of(1, 0));
            case "DX_PULSE_THREE" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0));
            case "DX_ASSASSIN_QUICK", "DX_SNIPER_SINGLE" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0));
            default -> throw new AssertionError("unreviewed variant suffix");
        };
    }

    @Test
    void appendsSixEquipableOrdinaryWeaponsWithoutChangingTheKnownOriginalRework() throws Exception {
        Fixture f = fixture();
        Snapshot before = snapshot(f);

        customize(f);

        assertSix(f.mek(), f.waz(), f.originalWeapons(), f.originalSkills());
        assertOriginalPrefix(before, f);
        assertSourceUnchanged(f);
    }

    @Test
    void eachIndependentWeaponHasItsPromisedAttackMovementAndExitSemantics() throws Exception {
        Fixture f = fixture();
        customize(f);
        assertSix(f.mek(), f.waz(), f.originalWeapons(), f.originalSkills());

        assertSemantics(f.waz(), f.mek());

        assertSourceUnchanged(f);
    }

    @Test
    void generatedBinaryRetainsEveryVariantAndSecondApplicationIsIdentical() throws Exception {
        Fixture f = fixture();
        customize(f);
        assertSix(f.mek(), f.waz(), f.originalWeapons(), f.originalSkills());
        JsonNode onceMek = JSON.valueToTree(f.mek());
        JsonNode onceWaz = JSON.valueToTree(f.waz());

        customize(f);

        assertEquals(onceMek, JSON.valueToTree(f.mek()), "second application must not append or rewrite");
        assertEquals(onceWaz, JSON.valueToTree(f.waz()));
        BsdxBinService service = new BsdxBinService();
        Path mekPath = output.resolve(ROLE + ".mek");
        Path wazPath = output.resolve(ROLE + ".waz");
        service.generate(mekPath.toString(), f.mek(), CHARSET);
        service.generate(wazPath.toString(), f.waz(), CHARSET);
        Mek parsedMek = (Mek) service.parse(mekPath.toString(), CHARSET).getData();
        Waz parsedWaz = (Waz) service.parse(wazPath.toString(), CHARSET).getData();
        assertSix(parsedMek, parsedWaz, f.originalWeapons(), f.originalSkills());
        assertSemantics(parsedWaz, parsedMek);
        for (Variant variant : VARIANTS) {
            assertEquals(payload(skill(f.waz(), variant.name(), variant.english())),
                    payload(skill(parsedWaz, variant.name(), variant.english())),
                    variant.english() + " complete private execution graph must survive WAZ serialization");
            assertEquals(payload(material(f.mek(), weapon(f.mek(), variant.name(), variant.sequence()))),
                    payload(material(parsedMek, weapon(parsedMek, variant.name(), variant.sequence()))));
            assertEquals(weapon(f.mek(), variant.name(), variant.sequence()).getWeaponDescription(),
                    weapon(parsedMek, variant.name(), variant.sequence()).getWeaponDescription());
        }
        assertSourceUnchanged(f);
    }

    @Test
    void lateNewDonorDriftIsRejectedWithoutLeavingAnEarlierPartialAppend() throws Exception {
        for (Consumer<Fixture> corrupt : newDonorCorruptions()) {
            Fixture f = fixture(); // already contains only the old, known Customizer changes
            corrupt.accept(f);
            JsonNode beforeMek = JSON.valueToTree(f.mek());
            JsonNode beforeWaz = JSON.valueToTree(f.waz());

            assertThrows(IllegalStateException.class, () -> customize(f),
                    "a new donor signature must be checked even after the original Customizer work is complete");

            assertEquals(beforeMek, JSON.valueToTree(f.mek()), "no partial new MEK or Material rows");
            assertEquals(beforeWaz, JSON.valueToTree(f.waz()), "no partial new private WAZ skills");
            assertSourceUnchanged(f);
        }
    }

    private static Fixture fixture() throws Exception {
        Path mekPath = Path.of("src/main/resources/game/bhe/mek", ROLE, ROLE + ".mek");
        Path wazPath = Path.of("src/main/resources/game/bhe/waz", ROLE, ROLE + ".waz");
        byte[] mekBytes = Files.readAllBytes(mekPath);
        byte[] wazBytes = Files.readAllBytes(wazPath);
        BheBinService service = new BheBinService();
        var sourceMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mekPath.toString(), CHARSET).getData();
        var sourceWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wazPath.toString(), CHARSET).getData();
        JsonNode sourceMekTree = JSON.valueToTree(sourceMek);
        JsonNode sourceWazTree = JSON.valueToTree(sourceWaz);
        Mek mek = new BheToBsdxMekConverter().convert(sourceMek);
        Waz waz = new BheToBsdxWazConverter().convert(sourceWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, ROLE + "SixVariantsTest");
        int originalWeapons = mek.getMekWeaponInfoMap().size();
        int sourceGroup = mek.getMekBasicInfo().getWazFileSequence();
        mek.getMekBasicInfo().setWazFileSequence(TARGET_GROUP);
        // Shift the real converted graph, including its incoming local jumps: bare BHE indices must fail.
        waz.getSkillList().add(0, new Waz.Skill());
        for (var weapon : mek.getMekWeaponInfoMap().values()) weapon.setWazSequence(weapon.getWazSequence() + 1);
        for (var skill : waz.getSkillList()) for (var phase : skill.getPhasesInfo()) {
            for (var unit : phase.getSkillUnitCollection()) for (var event : unit.getSkillInfoObjectList()) {
                if (event instanceof CEventChange change) for (var jump : change.getBsdxInfoCollectionList2()) {
                    if (Integer.valueOf(16).equals(jump.getInt1()) && List.of(1).equals(jump.getTypeList())
                            && jump.getParamList().size() == 2 && Objects.equals(sourceGroup, jump.getParamList().get(0))) {
                        jump.setParamList(new ArrayList<>(List.of(TARGET_GROUP, jump.getParamList().get(1) + 1)));
                    }
                }
            }
        }
        int originalSkills = waz.getSkillList().size();
        Fixture f = new Fixture(mek, waz, originalWeapons, originalSkills, mekPath, wazPath, mekBytes, wazBytes,
                sourceMek, sourceWaz, sourceMekTree, sourceWazTree);
        applyKnownLegacyBaseline(f);
        return f;
    }

    private static void assertSix(Mek mek, Waz waz, int originalWeapons, int originalSkills) throws Exception {
        List<Mek.MekWeaponInfo> added = mek.getMekWeaponInfoMap().values().stream()
                .filter(w -> w.getWazSequence() != null && w.getWazSequence() >= 0 && w.getWazSequence() < waz.getSkillList().size())
                .filter(w -> Optional.ofNullable(waz.getSkillList().get(w.getWazSequence()).getSkillNameEnglish())
                        .orElse("").startsWith("DX_")).toList();
        assertEquals(6, added.size(), "six separate equipable MEK rows are required; phases and legacy parameter edits do not count");
        assertEquals(originalWeapons + 6, mek.getMekWeaponInfoMap().size());
        assertEquals(originalSkills + 6, waz.getSkillList().size(), "one private main WAZ skill per new weapon");
        assertEquals(new HashSet<>(VARIANTS.stream().map(Variant::english).toList()),
                new HashSet<>(added.stream().map(w -> waz.getSkillList().get(w.getWazSequence()).getSkillNameEnglish()).toList()));
        int ordinal = 0;
        for (int key : mek.getMekWeaponInfoMap().keySet()) assertEquals(ordinal++, key, "player indices stay contiguous");
        var block = mek.getMekMaterialBlock();
        assertEquals(mek.getMekWeaponInfoMap().size(), block.getTrailingEntries().size());
        List<Mek.MekMaterialBlock.PluginEntry> views = new ArrayList<>(block.getRegularEntries());
        views.addAll(block.getTrailingEntries());
        assertEquals(payload(views), payload(block.getEntries()), "Material views must stay aligned");
        Set<Integer> privateWaz = new HashSet<>();
        for (Variant variant : VARIANTS) {
            var weapon = weapon(mek, variant.name(), variant.sequence());
            var copy = skill(waz, variant.name(), variant.english());
            var donor = skill(waz, variant.donorName(), variant.donorEnglish());
            var donorWeapon = weapon(mek, variant.donorName(), variant.sequence());
            assertTrue(privateWaz.add(weapon.getWazSequence()), "each equipable weapon needs its own WAZ reference");
            assertSame(copy, waz.getSkillList().get(weapon.getWazSequence()));
            int playerIndex = new ArrayList<>(mek.getMekWeaponInfoMap().values()).indexOf(weapon);
            assertTrue(playerIndex >= originalWeapons && playerIndex <= 152);
            assertSame(weapon, mek.getMekWeaponInfoMap().get(playerIndex), "present player row must survive map serialization");
            assertEquals(0, weapon.getWeaponCategory(), "FC and initializer do not count");
            assertEquals(variant.heat(), weapon.getHeatMinConsumption());
            assertEquals(variant.heat(), weapon.getHeatMaxConsumption());
            assertEquals(0, weapon.getUpgradeExp());
            assertEquals(donorWeapon.getForceCrashAmount(), weapon.getForceCrashAmount());
            assertEquals(donorWeapon.getWeaponIdentifier(), weapon.getWeaponIdentifier());
            assertEquals(variant.phases(), copy.getPhaseQuantity());
            assertEquals(variant.phases(), copy.getPhasesInfo().size());
            assertEquals(payload(donor.getSkillSuffixList()), payload(copy.getSkillSuffixList()), "suffix ordinal is not a phase index");
            assertEquals(reviewedSuffix(variant), suffixPairs(donor), "source suffix must match the reviewed entry table");
            assertEquals(reviewedSuffix(variant), suffixPairs(copy), "the copied entry cannot merely mirror a drifted donor");
            assertNotSame(donor, copy);
            assertNotSame(donor.getPhasesInfo(), copy.getPhasesInfo());
            assertNotSame(donor.getSkillSuffixList(), copy.getSkillSuffixList());
            for (int p = 0; p < copy.getPhasesInfo().size(); p++) {
                assertNotSame(donor.getPhasesInfo().get(p), copy.getPhasesInfo().get(p));
                assertNotSame(donor.getPhasesInfo().get(p).getSkillUnitCollection(), copy.getPhasesInfo().get(p).getSkillUnitCollection());
            }
            var sourceMaterial = material(mek, donorWeapon);
            var copiedMaterial = material(mek, weapon);
            assertNotSame(sourceMaterial, copiedMaterial);
            Mek.MekMaterialBlock.PluginEntry extra = variant.sheathMaterial()
                    ? material(mek, weapon(mek, "納刀", "0003")) : null;
            assertGroups(sourceMaterial.getSpriteGroups(), copiedMaterial.getSpriteGroups(), extra == null ? null : extra.getSpriteGroups());
            assertGroups(sourceMaterial.getSeGroups(), copiedMaterial.getSeGroups(), extra == null ? null : extra.getSeGroups());
            assertGroups(sourceMaterial.getVoiceGroups(), copiedMaterial.getVoiceGroups(), extra == null ? null : extra.getVoiceGroups());
            assertJapanese(variant.name());
            assertJapanese(weapon.getWeaponDescription());
            assertEquals(variant.name(), copy.getSkillNameJapanese());
        }
    }

    private static void assertGroups(List<int[]> source, List<int[]> actual, List<int[]> extra) {
        assertNotSame(source, actual);
        int expectedSize = Math.max(source.size(), extra == null ? 0 : extra.size());
        assertEquals(expectedSize, actual.size());
        for (int i = 0; i < expectedSize; i++) {
            Set<Integer> expected = new HashSet<>();
            if (i < source.size()) {
                assertNotSame(source.get(i), actual.get(i), "Material group arrays must be deep copies");
                for (int n : source.get(i)) expected.add(n);
            }
            if (extra != null && i < extra.size()) for (int n : extra.get(i)) expected.add(n);
            Set<Integer> values = new HashSet<>();
            for (int n : actual.get(i)) values.add(n);
            assertEquals(expected, values, "the actual consumers determine the Material union");
        }
    }

    private static Snapshot snapshot(Fixture f) {
        return new Snapshot(f.mek().getMekWeaponInfoMap().size(), f.waz().getSkillList().size(),
                JSON.valueToTree(f.mek().getMekWeaponInfoMap()), JSON.valueToTree(f.waz().getSkillList()),
                JSON.valueToTree(f.mek().getMekMaterialBlock().getTrailingEntries()), JSON.valueToTree(f.mek().getMekAiInfoList()));
    }

    private static void assertOriginalPrefix(Snapshot before, Fixture f) {
        for (int i = 0; i < before.weapons(); i++)
            assertEquals(before.weaponTree().get(Integer.toString(i)), JSON.valueToTree(f.mek().getMekWeaponInfoMap().get(i)));
        assertEquals(before.skillTree(), JSON.valueToTree(f.waz().getSkillList().subList(0, before.skills())),
                "the known original rework and its existing private skill remain intact");
        assertEquals(before.materialTree(), JSON.valueToTree(f.mek().getMekMaterialBlock().getTrailingEntries().subList(0, before.weapons())));
        assertEquals(before.aiTree(), JSON.valueToTree(f.mek().getMekAiInfoList()), "new weapon indices must not drift old AI");
    }

    private static void assertSourceUnchanged(Fixture f) throws Exception {
        assertEquals(f.sourceMekTree(), JSON.valueToTree(f.sourceMek()));
        assertEquals(f.sourceWazTree(), JSON.valueToTree(f.sourceWaz()));
        assertArrayEquals(f.mekBytes(), Files.readAllBytes(f.mekPath()), "original BHE MEK is read-only");
        assertArrayEquals(f.wazBytes(), Files.readAllBytes(f.wazPath()), "original BHE WAZ is read-only");
    }

    private static Mek.MekWeaponInfo weapon(Mek mek, String name, String sequence) {
        var matches = mek.getMekWeaponInfoMap().values().stream()
                .filter(w -> name.equals(w.getWeaponName()) && sequence.equals(w.getWeaponSequence())).toList();
        assertEquals(1, matches.size(), "unique MEK identity: " + name + "/" + sequence);
        return matches.get(0);
    }

    private static Waz.Skill skill(Waz waz, String name, String english) {
        var matches = waz.getSkillList().stream()
                .filter(s -> name.equals(s.getSkillNameJapanese()) && english.equals(s.getSkillNameEnglish())).toList();
        assertEquals(1, matches.size(), "unique WAZ identity: " + name + "/" + english);
        return matches.get(0);
    }

    private static Waz.Skill variant(Waz waz, int index) {
        Variant v = VARIANTS.get(index - 1);
        return skill(waz, v.name(), v.english());
    }

    private static List<SkillInfoObject> events(Waz.Skill skill, int phase, int slot) {
        var matches = skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).toList();
        assertTrue(matches.size() <= 1, "duplicate slot");
        return matches.isEmpty() ? List.of() : matches.get(0).getSkillInfoObjectList();
    }

    private static <T extends SkillInfoObject> T event(Waz.Skill skill, int phase, int slot, Class<T> type, int start, int end) {
        var matches = events(skill, phase, slot).stream().filter(type::isInstance).map(type::cast)
                .filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame())).toList();
        assertEquals(1, matches.size(), skill.getSkillNameEnglish() + " P" + phase + " slot" + slot + " " + start + ".." + end);
        return matches.get(0);
    }

    private static List<Integer> six(CEventValRandom e) {
        return List.of(e.getInt1(), e.getInt2(), e.getInt3(), e.getInt4(), e.getInt5(), e.getInt6());
    }

    private static void assertGravity(Waz.Skill skill, int phase, int frame) {
        assertEquals(List.of(0, 50, 0, 0, 50, 0),
                six(event(skill, phase, 21, CEventValRandom.class, frame, frame)), "both endpoints must restore constant gravity");
    }

    private static CEventTerm power(CEventHit hit) {
        var matches = hit.getCeventHitUnitList().stream().filter(u -> Integer.valueOf(6).equals(u.getUnitSlotNum())).toList();
        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).getBuffer());
        assertInstanceOf(CEventTerm.class, matches.get(0).getData());
        return (CEventTerm) matches.get(0).getData();
    }

    private static void assertPower(CEventHit hit, int expected) {
        CEventTerm term = power(hit);
        assertEquals(0, term.getStartFrame(), "nested parameter clocks are not attack clocks");
        assertEquals(0, term.getEndFrame());
        assertEquals(1, term.getBsdxInfoCollectionList().size());
        var value = term.getBsdxInfoCollectionList().get(0);
        assertEquals(23, value.getInt1());
        assertEquals(List.of(0), value.getTypeList());
        assertEquals(List.of(expected), value.getParamList());
    }

    private static JsonNode body(SkillInfoObject value) {
        ObjectNode node = (ObjectNode) payload(value);
        node.remove(List.of("startFrame", "endFrame"));
        return node;
    }

    private static void assertGate(Waz.Skill target, int phase, int frame, CEventVal donor) {
        CEventVal actual = event(target, phase, 47, CEventVal.class, frame, frame);
        assertEquals(body(donor), body(actual), "copy complete cancellation semantics, including priority and gate value");
    }

    private static void assertNormalEnd(Waz.Skill target, int phase, int frame) {
        CEventChange exit = event(target, phase, 71, CEventChange.class, frame, frame);
        assertEquals(1, exit.getFlag());
        assertEquals(0, exit.getInt1());
        assertEquals(1, exit.getBsdxInfoCollectionList1().size());
        assertEquals(1, exit.getBsdxInfoCollectionList2().size());
        var condition = exit.getBsdxInfoCollectionList1().get(0);
        var jump = exit.getBsdxInfoCollectionList2().get(0);
        assertEquals(0, condition.getInt1());
        assertEquals(List.of(0), condition.getTypeList());
        assertEquals(List.of(), condition.getParamList());
        assertEquals(16, jump.getInt1());
        assertEquals(List.of(0, 0), jump.getTypeList());
        assertEquals(List.of(), jump.getParamList());
        assertEquals(List.of(), condition.getIntList3());
        assertEquals(List.of(), jump.getIntList3());
        assertEquals(List.of(), condition.getIntList4());
        assertEquals(List.of(), jump.getIntList4());
        assertEquals(0, condition.getInt2());
        assertEquals(0, jump.getInt2());
    }

    private static List<CEventEffect> effects(Waz.Skill skill, int phase, int group, int index) {
        return events(skill, phase, 48).stream().filter(CEventEffect.class::isInstance).map(CEventEffect.class::cast)
                .filter(e -> e.getCeventEffectUnitList().stream().anyMatch(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())
                        && u.getData() instanceof CEventWazaSelect ref && Integer.valueOf(group).equals(ref.getWazFileNo())
                        && Integer.valueOf(index).equals(ref.getWazSequenceNo()))).toList();
    }

    private static List<CEventEffect> effectsLike(Waz.Skill target, int targetPhase, Waz.Skill donor, int donorPhase, int donorFrame) {
        var launch = events(donor, donorPhase, 48).stream().filter(CEventEffect.class::isInstance).map(CEventEffect.class::cast)
                .filter(e -> Integer.valueOf(donorFrame).equals(e.getStartFrame()))
                .filter(e -> e.getCeventEffectUnitList().stream().anyMatch(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())
                        && u.getData() instanceof CEventWazaSelect ref && ref.getWazFileNo() != 0)).toList();
        assertFalse(launch.isEmpty(), "real projectile donor exists");
        var ref = (CEventWazaSelect) launch.get(0).getCeventEffectUnitList().stream()
                .filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum())).findFirst().orElseThrow().getData();
        return effects(target, targetPhase, ref.getWazFileNo(), ref.getWazSequenceNo());
    }

    private static Mek.MekMaterialBlock.PluginEntry material(Mek mek, Mek.MekWeaponInfo weapon) {
        return mek.getMekMaterialBlock().getTrailingEntries().get(new ArrayList<>(mek.getMekWeaponInfoMap().values()).indexOf(weapon));
    }

    private static void assertJapanese(String text) throws Exception {
        assertNotNull(text);
        assertFalse(text.isBlank());
        var charset = Charset.forName(CHARSET);
        assertEquals(text, charset.decode(charset.newEncoder().encode(CharBuffer.wrap(text))).toString());
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        stripMetadata(tree);
        return tree;
    }

    private static void stripMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description"));
        node.forEach(YuriSixVariantsTest::stripMetadata);
    }

    private record Variant(String name, String english, String donorName, String donorEnglish,
                           String sequence, int heat, int phases, boolean sheathMaterial) { }
    private record Snapshot(int weapons, int skills, JsonNode weaponTree, JsonNode skillTree,
                            JsonNode materialTree, JsonNode aiTree) { }
    private record Fixture(Mek mek, Waz waz, int originalWeapons, int originalSkills, Path mekPath, Path wazPath,
                           byte[] mekBytes, byte[] wazBytes, com.giga.nexas.dto.bhe.mek.Mek sourceMek,
                           com.giga.nexas.dto.bhe.waz.Waz sourceWaz, JsonNode sourceMekTree, JsonNode sourceWazTree) { }
}
