package com.giga.nexas.transfer.bhe2bsdx.meka.freja;

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
class FrejaSixVariantsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final String ROLE = "freja";
    private static final int TARGET_GROUP = 212;
    private static final List<Variant> VARIANTS = List.of(
            new Variant("スピア・留め突き", "DX_SPEAR_ANCHOR", "スピア", "0002", "0003", 22, 5, false),
            new Variant("斬り上げ・低空", "DX_UPPERCUT_LOW", "斬り上げ", "SCYTHE04", "0005", 20, 2, false),
            new Variant("バックキック・退避", "DX_BACKKICK_RETREAT", "バックキック", "0004", "BLOW2", 18, 2, false),
            new Variant("スイング・双薙ぎ", "DX_SWING_DOUBLE", "スイング", "0003", "0004", 28, 1, false),
            new Variant("ジャベリン・二段投擲", "DX_JAVELIN_TWO", "ジャベリン", "G_BIT03", "RIFLE", 16, 1, false),
            new Variant("火炎弾・扇射", "DX_FLAME_FAN", "火炎弾", "0005", "SINMISSILE", 22, 1, false));

    private static void customize(Fixture f) {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setReboundMek(f.mek());
        context.setReboundWaz(f.waz());
        FrejaCustomizer.INSTANCE.customizeBeforeWrite(context);
    }

    private static void applyKnownLegacyBaseline(Fixture f) {
        Waz.Skill spear = skill(f.waz(), "スピア", "0002");
        for (int phase : List.of(3, 5)) {
            int start = phase == 3 ? 1 : 12;
            int end = phase == 3 ? 8 : 15;
            CEventValRandom speed = event(spear, phase, 18, CEventValRandom.class, start, end);
            speed.setInt2(36);
            speed.setInt5(20);
            CEventHit hit = event(spear, phase, 41, CEventHit.class, phase == 3 ? 3 : 16, phase == 3 ? 10 : 24);
            power(hit).getBsdxInfoCollectionList().get(0).setParamList(new ArrayList<>(List.of(180)));
        }
        var weapon = weapon(f.mek(), "スピア", "0003");
        weapon.setHeatMinConsumption(24);
        weapon.setHeatMaxConsumption(24);
        weapon.setUpgradeExp(0);
        weapon.setWeaponDescription("踏み込みと単発威力を強化したスピア。長い間合いから接近し、斬り上げやバックキックへつなげたい。");
        weapon(f.mek(), "武幻剛乱舞", "0007").setWeaponDescription(
                "ヒートチャージの余力を使う連撃。使用可能な残量に応じて、連撃の継続や締めへの移行が変わる。");
        f.mek().getMekBasicInfo().setMekDescription(
                "ハルバードの間合いを生かして戦う四足型。踏み込みを強化したスピアを軸に、斬り上げとバックキックを使い分ける。遠距離ではジャベリンで牽制し、相手の動きを見て接近したい。");
    }

    private static List<Consumer<Fixture>> newDonorCorruptions() {
        return List.of(
                f -> {
                    var flame = skill(f.waz(), "火炎弾", "0005");
                    var shots = effectsLike(flame, 1, flame, 1, 14);
                    ((CEventValRandom) effectUnit(shots.get(1), 29)).setInt5(-11);
                },
                f -> events(skill(f.waz(), "火炎弾", "0005"), 1, 48).get(0).setStartFrame(13),
                f -> skill(f.waz(), "火炎弾", "0005").setSkillNameEnglish("DRIFTED_FLAME"),
                f -> event(skill(f.waz(), "ジャベリン", "G_BIT03"), 1, 47, CEventVal.class, 13, 13).setInt4(2));
    }

    private static void assertSemantics(Waz waz, Mek mek) {
        Waz.Skill javelinDonor = skill(waz, "ジャベリン", "G_BIT03");
        CEventVal gate = event(javelinDonor, 1, 47, CEventVal.class, 13, 13);
        assertEquals(1, gate.getInt4());

        Waz.Skill anchor = variant(waz, 1);
        assertEquals(List.of(0, 24, 0, 0, 8, 0), six(event(anchor, 3, 18, CEventValRandom.class, 1, 8)));
        assertEquals(List.of(0, 24, 0, 0, 8, 0), six(event(anchor, 5, 18, CEventValRandom.class, 12, 15)));
        assertPower(event(anchor, 3, 41, CEventHit.class, 3, 10), 180);
        assertPower(event(anchor, 5, 41, CEventHit.class, 16, 24), 180);
        assertGate(anchor, 3, 11, gate);
        assertGate(anchor, 5, 25, gate);
        assertNormalEnd(anchor, 3, 21);
        assertNormalEnd(anchor, 5, 64);
        Waz.Skill spear = skill(waz, "スピア", "0002");
        for (int phase : List.of(1, 2, 4))
            assertEquals(payload(spear.getPhasesInfo().get(phase - 1)), payload(anchor.getPhasesInfo().get(phase - 1)),
                    "ground/air suffix routing is retained, not flattened into one branch");

        Waz.Skill low = variant(waz, 2);
        assertEquals(List.of(0, 8, 0, 0, 0, 0), six(event(low, 1, 19, CEventValRandom.class, 33, 39)));
        assertGravity(low, 1, 0);
        assertGravity(low, 2, 0);
        assertEquals(0, event(low, 2, 19, CEventValRandom.class, 0, 0).getInt5());
        assertEquals(8, event(low, 1, 18, CEventValRandom.class, 29, 32).getInt5());
        assertEquals(0, event(low, 1, 18, CEventValRandom.class, 33, 51).getInt5());
        assertPower(event(low, 1, 41, CEventHit.class, 29, 44), 170);
        assertGate(low, 1, 45, gate);
        Waz.Skill uppercut = skill(waz, "斬り上げ", "SCYTHE04");
        for (int phase : List.of(1, 2))
            assertEquals(payload(events(uppercut, phase, 71)), payload(events(low, phase, 71)), "landing branch and fallback stay native");
        assertEquals(hitPhysics(event(uppercut, 1, 41, CEventHit.class, 29, 44)),
                hitPhysics(event(low, 1, 41, CEventHit.class, 29, 44)), "lower self-lift does not rewrite target knockback");

        Waz.Skill retreat = variant(waz, 3);
        assertEquals(List.of(0, 0, 0, 0, 0, 0), six(event(retreat, 1, 19, CEventValRandom.class, 19, 26)));
        assertGravity(retreat, 1, 3);
        assertGravity(retreat, 1, 46);
        assertEquals(List.of(0, 16, 0, 0, 4, 0), six(event(retreat, 1, 18, CEventValRandom.class, 19, 36)));
        assertEquals(0, event(retreat, 1, 18, CEventValRandom.class, 37, 55).getInt5());
        assertPower(event(retreat, 1, 41, CEventHit.class, 23, 32), 180);
        assertEquals(1, events(retreat, 1, 41).size(), "no residual 23..46 hit window");
        assertGate(retreat, 1, 33, gate);
        Waz.Skill backkick = skill(waz, "バックキック", "0004");
        assertEquals(payload(events(backkick, 1, 15)), payload(events(retreat, 1, 15)), "reverse direction stays native");
        for (int phase : List.of(1, 2))
            assertEquals(payload(events(backkick, phase, 71)), payload(events(retreat, phase, 71)));

        Waz.Skill swing = variant(waz, 4);
        CEventHit doubleHit = event(swing, 1, 41, CEventHit.class, 42, 51);
        assertEquals(2, doubleHit.getHitCount());
        assertEquals(6, doubleHit.getHitInterval());
        assertPower(doubleHit, 90);
        assertEquals(12, event(swing, 1, 18, CEventValRandom.class, 37, 41).getInt5());
        assertGate(swing, 1, 52, gate);
        assertNormalEnd(swing, 1, 113);

        Waz.Skill javelin = variant(waz, 5);
        List<CEventEffect> throwsList = effectsLike(javelin, 1, javelinDonor, 1, 13);
        assertEquals(2, throwsList.size(), "two real projectile requests, not two decorative effects");
        assertEquals(List.of(13, 25), throwsList.stream().map(SkillInfoObject::getStartFrame).sorted().toList());
        CEventEffect originalThrow = effectsLike(javelinDonor, 1, javelinDonor, 1, 13).get(0);
        for (var launch : throwsList) {
            assertEquals(launch.getStartFrame(), launch.getEndFrame());
            assertEquals(body(originalThrow), body(launch), "preserve projectile, position, angle, speed and launch count");
        }
        assertGate(javelin, 1, 26, gate);
        assertTrue(events(javelin, 1, 47).stream().noneMatch(e -> Integer.valueOf(13).equals(e.getStartFrame())),
                "ordinary cancel cannot precede the promised second throw");
        assertTrue(events(javelin, 1, 35).isEmpty(), "two explicit requests need no new loop variable");
        assertNormalEnd(javelin, 1, 52);

        Waz.Skill flameDonor = skill(waz, "火炎弾", "0005");
        Waz.Skill fan = variant(waz, 6);
        List<CEventEffect> fanShots = effectsLike(fan, 1, flameDonor, 1, 14);
        assertEquals(3, fanShots.size(), "three real fireballs; the two zero-group decorations are not shots");
        Set<List<Integer>> positionAndAngle = new HashSet<>();
        for (CEventEffect launch : fanShots) {
            assertEquals(14, launch.getStartFrame());
            assertEquals(14, launch.getEndFrame());
            CEventTerm position = (CEventTerm) effectUnit(launch, 7);
            var expression = position.getBsdxInfoCollectionList().get(0);
            assertEquals(20, expression.getInt1());
            assertEquals(List.of(1), expression.getTypeList());
            List<Integer> point = expression.getIntList3();
            CEventValRandom direction = (CEventValRandom) effectUnit(launch, 29);
            positionAndAngle.add(List.of(point.get(0), point.get(1), direction.getInt5()));
        }
        assertEquals(Set.of(List.of(0, 1, 30), List.of(0, 2, -30), List.of(0, 0, 0)), positionAndAngle,
                "left, right and center launches have distinct native position/angle pairs");
        long decorationsBefore = events(flameDonor, 1, 48).stream().filter(CEventEffect.class::isInstance)
                .map(CEventEffect.class::cast).filter(FrejaSixVariantsTest::isDecoration).count();
        long decorationsAfter = events(fan, 1, 48).stream().filter(CEventEffect.class::isInstance)
                .map(CEventEffect.class::cast).filter(FrejaSixVariantsTest::isDecoration).count();
        assertEquals(2, decorationsBefore);
        assertEquals(decorationsBefore, decorationsAfter);
        assertGate(fan, 1, 14, event(flameDonor, 1, 47, CEventVal.class, 14, 14));
        assertNormalEnd(fan, 1, 56);

        // Existing FC is a separate high-resource path, never one of the six new ordinary weapons.
        assertEquals(1, weapon(mek, "武幻剛乱舞", "0007").getWeaponCategory());
        assertEquals(80, weapon(mek, "武幻剛乱舞", "0007").getHeatMaxConsumption());
        assertEquals(8, skill(waz, "武幻剛乱舞", "0001").getPhaseQuantity());
    }

    private static SkillInfoObject effectUnit(CEventEffect effect, int slot) {
        var matches = effect.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum())).toList();
        assertEquals(1, matches.size());
        assertEquals(1, matches.get(0).getBuffer());
        return matches.get(0).getData();
    }

    private static boolean isDecoration(CEventEffect effect) {
        return effectUnit(effect, 3) instanceof CEventWazaSelect ref && Integer.valueOf(0).equals(ref.getWazFileNo());
    }

    private static JsonNode hitPhysics(CEventHit hit) {
        ObjectNode node = (ObjectNode) body(hit);
        for (JsonNode unit : node.path("ceventHitUnitList"))
            if (unit.path("unitSlotNum").asInt(-1) == 6) ((ObjectNode) unit).remove("data");
        return node;
    }

    @TempDir
    Path output;

    @Test
    void lateFlameSuffixDriftRejectsBeforeAnyAdditionalRowIsPublished() throws Exception {
        Fixture f = fixture();
        skill(f.waz(), "火炎弾", "0005").getSkillSuffixList().get(0).setInt2(1);
        assertRejectedWithoutNewRows(f);
    }

    @Test
    void lowUppercutStartsItsMainPhaseWithAbsoluteJumpModifier100() throws Exception {
        Fixture f = fixture();
        var donor = skill(f.waz(), "斬り上げ", "SCYTHE04");
        JsonNode sourceModifiers = JSON.valueToTree(events(donor, 1, 27));
        customize(f);
        assertAbsoluteModifier(variant(f.waz(), 2), 1, 27, 100);
        CEventVal later = event(variant(f.waz(), 2), 1, 27, CEventVal.class, 48, 48);
        assertEquals(List.of(0, 100, 0, 100), List.of(later.getInt1(), later.getInt2(), later.getInt3(), later.getInt4()),
                "the later recovery point must not temporarily restart the modifier from zero");
        assertEquals(sourceModifiers, JSON.valueToTree(events(donor, 1, 27)), "original uppercut's 0-to-100 modifier timing remains intact");
        assertSourceUnchanged(f);
    }

    @Test
    void earlyLandingRecoveryRestoresGravityJumpFallAndOrdinaryChoiceAtEntry() throws Exception {
        Fixture f = fixture();
        var donor = skill(f.waz(), "斬り上げ", "SCYTHE04");
        JsonNode originalLanding = JSON.valueToTree(events(donor, 1, 71));
        JsonNode originalRecovery = JSON.valueToTree(donor.getPhasesInfo().get(1));
        customize(f);
        var low = variant(f.waz(), 2);
        assertAll("P1 can take the native landing transition at37 before the main ordinary gate45; P2 must restore its own entry state",
                () -> assertGravity(low, 2, 0),
                () -> assertAbsoluteModifier(low, 2, 27, 100),
                () -> assertAbsoluteModifier(low, 2, 28, 100),
                () -> assertEquals(List.of(0, 0, 0, 0, 0, 0), six(event(low, 2, 19, CEventValRandom.class, 0, 0))),
                () -> assertEquals(1, firstActiveGate(low, 2, 0).getInt4()));
        assertEquals(originalLanding, JSON.valueToTree(events(low, 1, 71)), "early landing and original suffix transition stay native");
        assertEquals(originalRecovery, JSON.valueToTree(donor.getPhasesInfo().get(1)), "the original recovery graph is preserved");
        assertSourceUnchanged(f);
    }

    private static void assertAbsoluteModifier(Waz.Skill skill, int phase, int slot, int expected) {
        CEventVal modifier = event(skill, phase, slot, CEventVal.class, 0, 0);
        assertEquals(0, modifier.getTypeId(), "jump/fall modifiers use the four-integer type0 format");
        assertEquals(List.of(0, expected, 0, expected), List.of(modifier.getInt1(), modifier.getInt2(), modifier.getInt3(), modifier.getInt4()),
                "a constant modifier has the desired value at both endpoints, including entry clock0");
    }

    @Test
    void lowUppercutNormalGateIsTheFirstActiveEventAt45() throws Exception {
        Fixture f = fixture();
        JsonNode sourceCancel = cancelSlot(skill(f.waz(), "斬り上げ", "SCYTHE04"), 1);
        customize(f);
        var low = variant(f.waz(), 2);
        assertEquals(1, firstActiveGate(low, 1, 45).getInt4(), "SD 3..159 must not shadow the new ordinary gate");
        assertEquals(3, event(low, 1, 47, CEventVal.class, 3, 44).getInt4());
        assertEquals(sourceCancel, cancelSlot(skill(f.waz(), "斬り上げ", "SCYTHE04"), 1));
        assertSourceUnchanged(f);
    }

    @Test
    void retreatKickNormalGateIsTheFirstActiveEventAt33() throws Exception {
        Fixture f = fixture();
        JsonNode sourceCancel = cancelSlot(skill(f.waz(), "バックキック", "0004"), 1);
        customize(f);
        var retreat = variant(f.waz(), 3);
        assertEquals(1, firstActiveGate(retreat, 1, 33).getInt4(), "SD 3..132 must not shadow the new ordinary gate");
        assertEquals(3, event(retreat, 1, 47, CEventVal.class, 3, 32).getInt4());
        assertEquals(sourceCancel, cancelSlot(skill(f.waz(), "バックキック", "0004"), 1));
        assertSourceUnchanged(f);
    }

    @Test
    void privateCancelSlotClocksCannotRewindBeforeReachingTheNewNormalGates() throws Exception {
        Fixture f = fixture();
        var uppercut = skill(f.waz(), "斬り上げ", "SCYTHE04");
        var backkick = skill(f.waz(), "バックキック", "0004");
        assertEquals(List.of(List.of(3, 159, -1, 1, -2)), ffSignature(uppercut, 1));
        assertEquals(List.of(List.of(3, 132, -1, 1, -2)), ffSignature(backkick, 1));
        customize(f);
        assertTrue(unknowns(variant(f.waz(), 2), 1, 47).isEmpty(),
                "63ACF0 resets the whole slot clock at FF.end: trimming this infinite FF to 44 would still never reach gate45");
        assertTrue(unknowns(variant(f.waz(), 3), 1, 47).isEmpty(),
                "trimming an infinite FF to 32 would still never reach gate33; remove only the verified private slot47 FF");
        assertEquals(List.of(List.of(3, 159, -1, 1, -2)), ffSignature(uppercut, 1));
        assertEquals(List.of(List.of(3, 132, -1, 1, -2)), ffSignature(backkick, 1));
        assertSourceUnchanged(f);
    }

    @Test
    void changedSourceCancelLoopSignatureRejectsWithoutAFirstPartialAppend() throws Exception {
        for (String[] identity : List.of(new String[]{"斬り上げ", "SCYTHE04"}, new String[]{"バックキック", "0004"})) {
            Fixture f = fixture();
            var source = skill(f.waz(), identity[0], identity[1]);
            var loop = unknowns(source, 1, 47);
            assertEquals(1, loop.size());
            loop.get(0).setInt3(-3);
            assertRejectedWithoutNewRows(f);
        }
    }

    private static CEventVal firstActiveGate(Waz.Skill skill, int phase, int clock) {
        var active = events(skill, phase, 47).stream().filter(CEventVal.class::isInstance).map(CEventVal.class::cast)
                .filter(e -> e.getStartFrame() <= clock && clock <= e.getEndFrame()).toList();
        assertFalse(active.isEmpty(), "a gate event must be active at the intended ordinary cancel clock");
        return active.get(0);
    }

    private static List<SkillInfoUnknown> unknowns(Waz.Skill skill, int phase, int slot) {
        return skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).flatMap(u -> u.getSkillInfoUnknownList().stream()).toList();
    }

    private static List<List<Integer>> ffSignature(Waz.Skill skill, int phase) {
        return unknowns(skill, phase, 47).stream()
                .map(e -> List.of(e.getStartFrame(), e.getEndFrame(), e.getInt1(), e.getInt2(), e.getInt3())).toList();
    }

    private static JsonNode cancelSlot(Waz.Skill skill, int phase) {
        return JSON.valueToTree(skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(47).equals(u.getUnitQuantity())).toList());
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
            case "DX_SPEAR_ANCHOR" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0), List.of(4, 0));
            case "DX_UPPERCUT_LOW", "DX_BACKKICK_RETREAT" -> List.of(List.of(0, 0), List.of(1, 0));
            case "DX_SWING_DOUBLE", "DX_JAVELIN_TWO", "DX_FLAME_FAN" -> List.of(List.of(0, 0));
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
        node.forEach(FrejaSixVariantsTest::stripMetadata);
    }

    private record Variant(String name, String english, String donorName, String donorEnglish,
                           String sequence, int heat, int phases, boolean sheathMaterial) { }
    private record Snapshot(int weapons, int skills, JsonNode weaponTree, JsonNode skillTree,
                            JsonNode materialTree, JsonNode aiTree) { }
    private record Fixture(Mek mek, Waz waz, int originalWeapons, int originalSkills, Path mekPath, Path wazPath,
                           byte[] mekBytes, byte[] wazBytes, com.giga.nexas.dto.bhe.mek.Mek sourceMek,
                           com.giga.nexas.dto.bhe.waz.Waz sourceWaz, JsonNode sourceMekTree, JsonNode sourceWazTree) { }
}
