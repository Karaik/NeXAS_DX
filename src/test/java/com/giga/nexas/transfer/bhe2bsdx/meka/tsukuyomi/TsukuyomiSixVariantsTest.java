package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi;

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
class TsukuyomiSixVariantsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final String ROLE = "tsukuyomi";
    private static final int TARGET_GROUP = 210;
    private static final List<Variant> VARIANTS = List.of(
            new Variant("袈裟・一閃", "DX_KESA_ISSEN", "袈裟", "PUNCH", "PUNCH", 12, 1, true),
            new Variant("打突・留め", "DX_DATOTSU_HOLD", "打突", "0001", "NAGE", 14, 1, true),
            new Variant("一威・止刃", "DX_ICHII_BRAKE", "一威", "0005", "SWORD_KAITEN", 18, 2, false),
            new Variant("逆風・返し", "DX_GYAKUFU_RETURN", "逆風", "0003", "SWORD_TATE", 18, 3, false),
            new Variant("銃撃・離脱", "DX_GUNSHOT_RETREAT", "銃撃", "G_LAUNCHER04", "0008", 24, 3, false),
            new Variant("切返・二連", "DX_KIRIKAESHI_DOUBLE", "切返", "0004", "KIRIKAESHI", 26, 1, false));

    private static void customize(Fixture f) {
        TsukuyomiCustomizer.INSTANCE.customizeBeforeWrite(f.mek(), f.waz());
    }

    // The existing one-weapon baseline is prepared independently of the changing public Customizer.
    private static void applyKnownLegacyBaseline(Fixture f) {
        var source = skill(f.waz(), "袈裟", "PUNCH");
        var sheath = skill(f.waz(), "納刀", "0008");
        var sheathWeapon = weapon(f.mek(), "納刀", "0003");
        com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants.append(
                f.mek(), f.waz(), weapon(f.mek(), "袈裟", "PUNCH"), source, "袈裟・一閃", "DX_KESA_ISSEN", 12,
                "踏み込んで一刀を放ち、納刀して仕切り直す。抜刀中の連撃とは使い分け、短い接近戦から次の間合いを作る。",
                copy -> {
                    event(copy, 1, 18, CEventValRandom.class, 8, 11).setInt2(32);
                    event(copy, 1, 71, CEventChange.class, 38, 38).getBsdxInfoCollectionList2().get(0)
                            .setParamList(new ArrayList<>(List.of(TARGET_GROUP, f.waz().getSkillList().indexOf(sheath))));
                }, sheathWeapon);
    }

    private static List<Consumer<Fixture>> newDonorCorruptions() {
        return List.of(
                f -> event(skill(f.waz(), "切返", "0004"), 1, 18, CEventValRandom.class, 54, 59).setInt5(999),
                f -> event(skill(f.waz(), "切返", "0004"), 1, 41, CEventHit.class, 54, 65).setStartFrame(53),
                f -> skill(f.waz(), "切返", "0004").setSkillNameJapanese("切返・漂移"),
                f -> event(skill(f.waz(), "疾風", "0011"), 1, 47, CEventVal.class, 43, 43).setInt4(2));
    }

    private static void assertSemantics(Waz waz, Mek mek) {
        var gate = event(skill(waz, "疾風", "0011"), 1, 47, CEventVal.class, 43, 43);
        assertEquals(1, gate.getInt4());

        Waz.Skill issen = variant(waz, 1);
        assertEquals(List.of(0, 32, 0, 0, 0, 0), six(event(issen, 1, 18, CEventValRandom.class, 8, 11)));
        assertPower(event(issen, 1, 41, CEventHit.class, 10, 11), 30);
        assertSheathJump(waz, mek, issen, 38);

        Waz.Skill hold = variant(waz, 2);
        assertEquals(List.of(0, 12, 0, 0, 4, 0), six(event(hold, 1, 18, CEventValRandom.class, 9, 31)));
        assertEquals(List.of(0, 6, 0, 0, 8, 0), six(event(hold, 1, 18, CEventValRandom.class, 40, 48)));
        assertEquals(List.of(0, 0, 0, 0, 0, 0), six(event(hold, 1, 19, CEventValRandom.class, 9, 31)));
        assertEquals(0, event(hold, 1, 19, CEventValRandom.class, 42, 42).getInt5());
        assertGravity(hold, 1, 0);
        assertPower(event(hold, 1, 41, CEventHit.class, 43, 50), 30);
        assertGate(hold, 1, 51, gate);
        assertSheathJump(waz, mek, hold, 76);

        Waz.Skill brake = variant(waz, 3);
        assertEquals(List.of(0, 16, 0, 0, 4, 0), six(event(brake, 2, 18, CEventValRandom.class, 8, 10)));
        assertEquals(0, event(brake, 2, 18, CEventValRandom.class, 11, 24).getInt5());
        assertPower(event(brake, 2, 41, CEventHit.class, 7, 10), 60);
        assertGravity(brake, 2, 11);
        assertGravity(brake, 2, 20);
        assertEquals(payload(events(skill(waz, "一威", "0005"), 1, 71)), payload(events(brake, 1, 71)),
                "near-distance and fallback selection must keep the original suffix semantics");
        assertEquals(payload(events(skill(waz, "一威", "0005"), 2, 47)), payload(events(brake, 2, 47)),
                "existing conditional cancellation cannot become a global always-cancel gate");

        Waz.Skill reverse = variant(waz, 4);
        assertPower(event(reverse, 1, 41, CEventHit.class, 24, 27), 60);
        assertGate(reverse, 1, 28, gate);
        assertGravity(reverse, 1, 0);
        assertEquals(List.of(0, 12, 0, 0, 4, 0), six(event(reverse, 2, 19, CEventValRandom.class, 14, 16)));
        assertPower(event(reverse, 2, 41, CEventHit.class, 10, 11), 50);
        assertEquals(payload(events(skill(waz, "逆風", "0003"), 3, 71)), payload(events(reverse, 3, 71)));

        Waz.Skill retreat = variant(waz, 5);
        Waz.Skill gun = skill(waz, "銃撃", "G_LAUNCHER04");
        List<CEventEffect> shots = effectsLike(retreat, 3, gun, 3, 17);
        assertEquals(1, shots.size(), "one real projectile, not the decorative effect count");
        assertEquals(17, shots.get(0).getStartFrame());
        assertEquals(payload(events(gun, 3, 48)), payload(events(retreat, 3, 48)), "preserve exact bullet and launch position");
        assertEquals(List.of(0, -6, 0, 0, 0, 0), six(event(retreat, 3, 18, CEventValRandom.class, 18, 25)));
        assertGravity(retreat, 3, 18);
        assertGate(retreat, 3, 17, event(gun, 3, 47, CEventVal.class, 17, 17));
        assertNormalEnd(retreat, 3, 50);

        Waz.Skill doubleCut = variant(waz, 6);
        CEventHit hit = event(doubleCut, 1, 41, CEventHit.class, 54, 65);
        assertEquals(2, hit.getHitCount());
        assertEquals(7, hit.getHitInterval());
        assertPower(hit, 65);
        assertEquals(8, event(doubleCut, 1, 18, CEventValRandom.class, 54, 59).getInt5());
        assertEquals(8, event(doubleCut, 1, 18, CEventValRandom.class, 60, 63).getInt5());
        assertGate(doubleCut, 1, 66, gate);
        assertEquals(payload(events(skill(waz, "切返", "0004"), 1, 71)), payload(events(doubleCut, 1, 71)));
    }

    private static void assertSheathJump(Waz waz, Mek mek, Waz.Skill target, int frame) {
        CEventChange exit = event(target, 1, 71, CEventChange.class, frame, frame);
        assertEquals(1, exit.getFlag());
        assertEquals(List.of(0), exit.getBsdxInfoCollectionList1().get(0).getTypeList());
        var jump = exit.getBsdxInfoCollectionList2().get(0);
        assertEquals(16, jump.getInt1());
        assertEquals(List.of(1), jump.getTypeList());
        assertEquals(List.of(mek.getMekBasicInfo().getWazFileSequence(), waz.getSkillList().indexOf(skill(waz, "納刀", "0008"))),
                jump.getParamList(), "exit targets the rebound sheath identity rather than a BHE numeric index");
    }

    @TempDir
    Path output;

    @Test
    void lateDoubleCutSuffixDriftRejectsBeforeAnyAdditionalRowIsPublished() throws Exception {
        Fixture f = fixture();
        skill(f.waz(), "切返", "0004").getSkillSuffixList().get(0).setInt2(1);
        assertRejectedWithoutNewRows(f);
    }

    @Test
    void gunDonorCannotSilentlyRedirectItsOnlyEntryFromThirdPhaseToFirst() throws Exception {
        Fixture f = fixture();
        var gun = skill(f.waz(), "銃撃", "G_LAUNCHER04");
        assertEquals(List.of(List.of(2, 0)), suffixPairs(gun));
        gun.getSkillSuffixList().get(0).setInt1(0);
        assertRejectedWithoutNewRows(f);
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
            case "DX_KESA_ISSEN", "DX_DATOTSU_HOLD", "DX_KIRIKAESHI_DOUBLE" -> List.of(List.of(0, 0));
            case "DX_ICHII_BRAKE" -> List.of(List.of(0, 0), List.of(1, 0));
            case "DX_GYAKUFU_RETURN" -> List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0));
            case "DX_GUNSHOT_RETREAT" -> List.of(List.of(2, 0));
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
        node.forEach(TsukuyomiSixVariantsTest::stripMetadata);
    }

    private record Variant(String name, String english, String donorName, String donorEnglish,
                           String sequence, int heat, int phases, boolean sheathMaterial) { }
    private record Snapshot(int weapons, int skills, JsonNode weaponTree, JsonNode skillTree,
                            JsonNode materialTree, JsonNode aiTree) { }
    private record Fixture(Mek mek, Waz waz, int originalWeapons, int originalSkills, Path mekPath, Path wazPath,
                           byte[] mekBytes, byte[] wazBytes, com.giga.nexas.dto.bhe.mek.Mek sourceMek,
                           com.giga.nexas.dto.bhe.waz.Waz sourceWaz, JsonNode sourceMekTree, JsonNode sourceWazTree) { }
}
