package com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize;

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
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.MotokiCustomizer;
import com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm.WilhelmCustomizer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.InvocationTargetException;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.*;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class CharacterWeaponVariantsTest {

    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final String TOOL = "com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.CharacterWeaponVariants";
    private static final String TSUKUYOMI = "com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.TsukuyomiCustomizer";

    @TempDir
    Path output;

    @Test
    void copiesReboundSkillCollectionsAndThreeMaterialGroupsWithoutSharing() throws Exception {
        Fixture f = fixture("tsukuyomi", 210);
        var sourceWeapon = weapon(f.mek(), "袈裟", "PUNCH");
        var sourceSkill = skill(f.waz(), "袈裟", "PUNCH");
        var donor = weapon(f.mek(), "納刀", "0003");
        materialMarkers(f.mek(), sourceWeapon, 111, 311, 511);
        materialMarkers(f.mek(), donor, 222, 322, 522);
        int oldWeapons = f.mek().getMekWeaponInfoMap().size();
        int oldSkills = f.waz().getSkillList().size();
        JsonNode beforeSkill = JSON.valueToTree(sourceSkill);
        JsonNode beforeWeapon = JSON.valueToTree(sourceWeapon);
        JsonNode beforeMaterial = JSON.valueToTree(material(f.mek(), sourceWeapon));
        JsonNode beforeAi = JSON.valueToTree(f.mek().getMekAiInfoList());

        appendTestVariant(f);

        assertEquals(oldWeapons + 1, f.mek().getMekWeaponInfoMap().size());
        assertEquals(oldSkills + 1, f.waz().getSkillList().size());
        var variantWeapon = weapon(f.mek(), "試験一閃", "PUNCH");
        var variantSkill = skill(f.waz(), "試験一閃", "DX_TEST_VARIANT");
        assertEquals(oldSkills, variantWeapon.getWazSequence());
        assertEquals(0, variantWeapon.getWeaponCategory());
        assertEquals(sourceWeapon.getWeaponIdentifier(), variantWeapon.getWeaponIdentifier());
        assertEquals(List.of(0, 32, 0, 0, 0, 0), speedValues(event(variantSkill, 1, 18, CEventValRandom.class, 8, 11)));
        var added = material(f.mek(), variantWeapon);
        assertArrayEquals(new int[]{111, 222}, added.getSpriteGroups().get(0));
        assertArrayEquals(new int[]{311, 322}, added.getSeGroups().get(0));
        assertArrayEquals(new int[]{511, 522}, added.getVoiceGroups().get(0));
        assertMaterialViews(f.mek());
        assertNotSame(sourceSkill, variantSkill);
        var originalPower = power(event(sourceSkill, 1, 41, CEventHit.class, 10, 11));
        var copiedPower = power(event(variantSkill, 1, 41, CEventHit.class, 10, 11));
        assertNotSame(originalPower, copiedPower);
        assertNotSame(originalPower.getTypeList(), copiedPower.getTypeList());
        assertNotSame(originalPower.getParamList(), copiedPower.getParamList());
        assertNotSame(originalPower.getIntList3(), copiedPower.getIntList3());
        assertNotSame(originalPower.getIntList4(), copiedPower.getIntList4());
        copiedPower.getParamList().set(0, 999);
        added.getSpriteGroups().get(0)[0] = 999;
        added.getSeGroups().get(0)[0] = 999;
        added.getVoiceGroups().get(0)[0] = 999;
        assertEquals(beforeSkill, JSON.valueToTree(sourceSkill));
        assertEquals(beforeWeapon, JSON.valueToTree(sourceWeapon));
        assertEquals(beforeMaterial, JSON.valueToTree(material(f.mek(), sourceWeapon)));
        assertEquals(beforeAi, JSON.valueToTree(f.mek().getMekAiInfoList()));
    }

    @Test
    void repeatsIdenticallyAndRejectsChangedExistingVariantBeforeWriting() throws Exception {
        Fixture f = fixture("tsukuyomi", 210);
        appendTestVariant(f);
        JsonNode onceMek = JSON.valueToTree(f.mek());
        JsonNode onceWaz = JSON.valueToTree(f.waz());
        appendTestVariant(f);
        assertEquals(onceMek, JSON.valueToTree(f.mek()));
        assertEquals(onceWaz, JSON.valueToTree(f.waz()));
        event(skill(f.waz(), "試験一閃", "DX_TEST_VARIANT"), 1, 18, CEventValRandom.class, 8, 11).setInt2(999);
        assertNoWritesOnRejection(f, () -> appendTestVariant(f));
    }

    @Test
    void rejectsMaterialMismatchAndDuplicateNamesBeforeAnyAppend() throws Exception {
        Fixture badMaterial = fixture("tsukuyomi", 210);
        badMaterial.mek().getMekMaterialBlock().getTrailingEntries().remove(0);
        assertNoWritesOnRejection(badMaterial, () -> appendTestVariant(badMaterial));
        Fixture duplicate = fixture("tsukuyomi", 210);
        weapon(duplicate.mek(), "抜刀", "0001").setWeaponName("試験一閃");
        assertNoWritesOnRejection(duplicate, () -> appendTestVariant(duplicate));
    }

    @Test
    void rejectsKouSparseMapAndWeaponBankOverflowWithoutChangingOriginals() throws Exception {
        Fixture kou = fixture("tsukuyomi", 210);
        kou.mek().getMekBasicInfo().setMekNameEnglish("KOU");
        assertNoWritesOnRejection(kou, () -> appendTestVariant(kou));
        Fixture sparse = fixture("tsukuyomi", 210);
        var first = sparse.mek().getMekWeaponInfoMap().remove(0);
        sparse.mek().getMekWeaponInfoMap().put(99, first);
        assertNoWritesOnRejection(sparse, () -> appendTestVariant(sparse));
        Fixture full = fixture("tsukuyomi", 210);
        var block = full.mek().getMekMaterialBlock();
        while (full.mek().getMekWeaponInfoMap().size() < 153) {
            int index = full.mek().getMekWeaponInfoMap().size();
            var filler = JSON.convertValue(weapon(full.mek(), "袈裟", "PUNCH"), Mek.MekWeaponInfo.class);
            filler.setWeaponName("予備" + index);
            full.mek().getMekWeaponInfoMap().put(index, filler);
            block.getTrailingEntries().add(block.getTrailingEntries().get(2));
            block.getEntries().add(block.getTrailingEntries().get(2));
        }
        assertNoWritesOnRejection(full, () -> appendTestVariant(full));
    }

    @Test
    void failedCopyEditCannotAppendHalfAWeaponOrMaterial() throws Exception {
        Fixture f = fixture("tsukuyomi", 210);
        assertNoWritesOnRejection(f, () -> append(f,
                weapon(f.mek(), "袈裟", "PUNCH"), skill(f.waz(), "袈裟", "PUNCH"),
                "試験一閃", "DX_TEST_VARIANT", 12, "試験用の独立武装。", copy -> {
                    event(copy, 1, 18, CEventValRandom.class, 8, 11).setInt2(32);
                    throw new IllegalStateException("later signature mismatch");
                }, weapon(f.mek(), "納刀", "0003")));
    }

    @Test
    void tsukuyomiVariantDashesOnceAndExitsThroughIdentityResolvedSheathing() throws Exception {
        Fixture f = fixture("tsukuyomi", 210);
        var original = skill(f.waz(), "袈裟", "PUNCH");
        assertEquals(List.of(0, 24, 0, 0, 0, 0), speedValues(event(original, 1, 18, CEventValRandom.class, 8, 11)));
        assertEquals(List.of(30), power(event(original, 1, 41, CEventHit.class, 10, 11)).getParamList());
        assertJump(event(original, 1, 71, CEventChange.class, 38, 38), 210,
                f.waz().getSkillList().indexOf(skill(f.waz(), "抜刀中", "0009")));
        Snapshot before = snapshot(f);

        customizeTsukuyomi(f);

        assertAdded(f, before, "袈裟・一閃", "PUNCH", "DX_KESA_ISSEN", 12, 6);
        var variant = skill(f.waz(), "袈裟・一閃", "DX_KESA_ISSEN");
        assertEquals(List.of(0, 32, 0, 0, 0, 0), speedValues(event(variant, 1, 18, CEventValRandom.class, 8, 11)));
        assertEquals(List.of(30), power(event(variant, 1, 41, CEventHit.class, 10, 11)).getParamList());
        assertEquals(JSON.valueToTree(event(original, 1, 48, CEventEffect.class, 8, 11)),
                JSON.valueToTree(event(variant, 1, 48, CEventEffect.class, 8, 11)));
        assertJump(event(variant, 1, 71, CEventChange.class, 38, 38), 210,
                f.waz().getSkillList().indexOf(skill(f.waz(), "納刀", "0008")));
        event(skill(f.waz(), "納刀", "0008"), 1, 71, CEventChange.class, 30, 30);
        roundTrip(f, "袈裟・一閃", "PUNCH", "DX_KESA_ISSEN", 12);
        assertIdempotent(f, () -> customizeTsukuyomi(f));
        assertSourceFiles(f);
    }

    @Test
    void wilhelmVariantDelaysOnlyRightCannonsThenReturnsToFourLeggedForm() throws Exception {
        Fixture f = fixture("wilhelm", 211);
        var original = skill(f.waz(), "キャタピラ：主砲斉射", "0017");
        List<CEventEffect> originalEffects = events(original, 2, 48, CEventEffect.class);
        assertEquals(10, originalEffects.size());
        assertEquals(4, originalEffects.stream().filter(CharacterWeaponVariantsTest::rightCannon).count());
        for (CEventEffect effect : originalEffects) assertEquals(0, effect.getStartFrame());
        event(original, 1, 71, CEventChange.class, 43, 43);
        Snapshot before = snapshot(f);

        WilhelmCustomizer.INSTANCE.customizeBeforeWrite(context(f));

        assertAdded(f, before, "交互砲撃・離脱", "0007", "DX_ALTERNATE_CANNON_EXIT", 40, 6);
        var variant = skill(f.waz(), "交互砲撃・離脱", "DX_ALTERNATE_CANNON_EXIT");
        List<CEventEffect> effects = events(variant, 2, 48, CEventEffect.class);
        assertEquals(10, effects.size());
        for (int i = 0; i < effects.size(); i++) {
            ObjectNode expected = JSON.valueToTree(originalEffects.get(i));
            expected.put("startFrame", i >= 4 && i < 8 ? 8 : 0);
            expected.put("endFrame", i >= 4 && i < 8 ? 8 : 0);
            assertEquals(expected, JSON.valueToTree(effects.get(i)), "只允许改右侧外层事件帧");
        }
        assertJump(event(variant, 2, 71, CEventChange.class, 50, 50), 211,
                f.waz().getSkillList().indexOf(skill(f.waz(), "キャタピラ：終了", "TACKLE05")));
        roundTrip(f, "交互砲撃・離脱", "0007", "DX_ALTERNATE_CANNON_EXIT", 40);
        assertIdempotent(f, () -> WilhelmCustomizer.INSTANCE.customizeBeforeWrite(context(f)));
        assertSourceFiles(f);
    }

    @Test
    void motokiVariantPreservesSixBombPayloadsAndUsesFlightSlices() throws Exception {
        Fixture f = fixture("motoki", 212);
        var original = skill(f.waz(), "飛行形態：集中爆撃", "0023");
        List<CEventEffect> effects = events(original, 1, 48, CEventEffect.class);
        assertEquals(1, effects.size());
        assertEquals(10, effects.get(0).getStartFrame());
        var amount = (CEventValRandom) effects.get(0).getCeventEffectUnitList().stream()
                .filter(value -> Integer.valueOf(4).equals(value.getUnitSlotNum())).findFirst().orElseThrow().getData();
        assertEquals(6, amount.getInt5());
        Snapshot before = snapshot(f);

        MotokiCustomizer.INSTANCE.customizeBeforeWrite(context(f));

        assertEquals(before.weapons() + 6, f.mek().getMekWeaponInfoMap().size());
        assertEquals(before.skills() + 7, f.waz().getSkillList().size());
        var added = weapon(f.mek(), "集中爆撃・帰投", "0020");
        assertSame(added, f.mek().getMekWeaponInfoMap().get(before.weapons()));
        assertTrue(before.weapons() <= 152);
        assertEquals(before.skills(), added.getWazSequence());
        assertEquals(0, added.getWeaponCategory());
        assertEquals(40, added.getHeatMinConsumption());
        assertEquals(40, added.getHeatMaxConsumption());
        assertEquals(0, added.getUpgradeExp());
        for (int i = 0; i < before.weapons(); i++) assertEquals(before.weaponTree().get(Integer.toString(i)), JSON.valueToTree(f.mek().getMekWeaponInfoMap().get(i)));
        assertEquals(before.skillTree(), JSON.valueToTree(f.waz().getSkillList().subList(0, before.skills())));
        assertEquals(before.materialTree(), JSON.valueToTree(f.mek().getMekMaterialBlock().getTrailingEntries().subList(0, before.weapons())));
        assertEquals(before.aiTree(), JSON.valueToTree(f.mek().getMekAiInfoList()));
        assertMaterialViews(f.mek());
        var router = skill(f.waz(), "集中爆撃・帰投", "DX_FOCUSED_BOMB_RETURN");
        assertEquals(1, router.getPhaseQuantity());
        assertJump(event(router, 1, 71, CEventChange.class, 0, 0), 212, before.skills() + 1);
        var execution = skill(f.waz(), "集中爆撃・帰投：制御", "DX_FOCUSED_BOMB_EXECUTION");
        assertEquals(4, execution.getPhaseQuantity());
        assertTrue(events(execution, 2, 48, CEventEffect.class).isEmpty());
        var release = event(execution, 3, 48, CEventEffect.class, 0, 0);
        Set<Integer> bombControls = Set.of(7, 9, 10, 11, 31, 32, 33);
        assertEquals(JSON.valueToTree(effects.get(0).getCeventEffectUnitList().stream()
                        .filter(unit -> !bombControls.contains(unit.getUnitSlotNum())).toList()),
                JSON.valueToTree(release.getCeventEffectUnitList().stream()
                        .filter(unit -> !bombControls.contains(unit.getUnitSlotNum())).toList()));
        var position = (CEventTerm) release.getCeventEffectUnitList().stream()
                .filter(unit -> Integer.valueOf(7).equals(unit.getUnitSlotNum())).findFirst().orElseThrow().getData();
        assertEquals(List.of(1, 0), position.getBsdxInfoCollectionList().get(0).getIntList3());
        assertEquals(List.of(1, 0, 0, 0, 12, 0), speedValues(event(execution, 3, 18, CEventValRandom.class, 0, 1)));
        assertEquals(List.of(1, 0, 0, 0, 8, 0), speedValues(event(execution, 3, 18, CEventValRandom.class, 2, 49)));
        var exit = event(execution, 3, 71, CEventChange.class, 79, 79).getBsdxInfoCollectionList2().get(0);
        assertEquals(16, exit.getInt1());
        assertEquals(List.of(0, 1), exit.getTypeList());
        assertEquals(List.of(3), exit.getParamList());
        event(skill(f.waz(), "飛行形態：終了", "BOOST_DASH6"), 1, 71, CEventChange.class, 30, 30);
        roundTrip(f, "集中爆撃・帰投", "0020", "DX_FOCUSED_BOMB_RETURN", 40);
        assertIdempotent(f, () -> MotokiCustomizer.INSTANCE.customizeBeforeWrite(context(f)));
        assertSourceFiles(f);
    }

    @Test
    void tsukuyomiRejectsSheathingDonorThatReturnsToDrawnStanceWithoutWriting() throws Exception {
        Fixture f = fixture("tsukuyomi", 210);
        var ending = event(skill(f.waz(), "納刀", "0008"), 1, 71, CEventChange.class, 30, 30);
        assertNormalEnd(ending);
        ending.getBsdxInfoCollectionList2().get(0).setTypeList(new ArrayList<>(List.of(1)));
        ending.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(210,
                f.waz().getSkillList().indexOf(skill(f.waz(), "抜刀中", "0009")))));

        assertNoWritesOnRejection(f, () -> customizeTsukuyomi(f));
    }

    @Test
    void wilhelmRejectsEndingDonorThatReturnsToCaterpillarStanceWithoutWriting() throws Exception {
        Fixture f = fixture("wilhelm", 211);
        var ending = event(skill(f.waz(), "キャタピラ：終了", "TACKLE05"), 1, 71, CEventChange.class, 25, 25);
        assertNormalEnd(ending);
        ending.getBsdxInfoCollectionList2().get(0).setTypeList(new ArrayList<>(List.of(1)));
        ending.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(211,
                f.waz().getSkillList().indexOf(skill(f.waz(), "キャタピラ：持続", "TACKLE07")))));

        assertNoWritesOnRejection(f, () -> WilhelmCustomizer.INSTANCE.customizeBeforeWrite(context(f)));
    }

    @Test
    void motokiRejectsEndingDonorThatReturnsToFlightWithoutWriting() throws Exception {
        Fixture f = fixture("motoki", 212);
        var ending = event(skill(f.waz(), "飛行形態：終了", "BOOST_DASH6"), 1, 71, CEventChange.class, 30, 30);
        assertNormalEnd(ending);
        ending.getBsdxInfoCollectionList2().get(0).setTypeList(new ArrayList<>(List.of(1)));
        ending.getBsdxInfoCollectionList2().get(0).setParamList(new ArrayList<>(List.of(212,
                f.waz().getSkillList().indexOf(skill(f.waz(), "飛行形態：飛行中", "BOOST_DASH5")))));

        assertNoWritesOnRejection(f, () -> MotokiCustomizer.INSTANCE.customizeBeforeWrite(context(f)));
    }

    private static void appendTestVariant(Fixture f) {
        append(f, weapon(f.mek(), "袈裟", "PUNCH"), skill(f.waz(), "袈裟", "PUNCH"),
                "試験一閃", "DX_TEST_VARIANT", 12, "試験用の独立武装。",
                copy -> event(copy, 1, 18, CEventValRandom.class, 8, 11).setInt2(32), weapon(f.mek(), "納刀", "0003"));
    }

    private static void append(Fixture f, Mek.MekWeaponInfo sourceWeapon, Waz.Skill sourceSkill,
                               String name, String english, int heat, String description,
                               Consumer<Waz.Skill> edit, Mek.MekWeaponInfo... donors) {
        try {
            Class<?> tool = Class.forName(TOOL);
            tool.getMethod("append", Mek.class, Waz.class, Mek.MekWeaponInfo.class, Waz.Skill.class,
                    String.class, String.class, int.class, String.class, Consumer.class, Mek.MekWeaponInfo[].class)
                    .invoke(null, f.mek(), f.waz(), sourceWeapon, sourceSkill, name, english, heat, description, edit, donors);
        } catch (InvocationTargetException error) {
            rethrow(error.getCause());
        } catch (ReflectiveOperationException error) {
            fail("有限武装复制能力尚未提供声明接口", error);
        }
    }

    private static void customizeTsukuyomi(Fixture f) {
        try {
            Class<?> type = Class.forName(TSUKUYOMI);
            type.getMethod("customizeBeforeWrite", Mek.class, Waz.class)
                    .invoke(type.getField("INSTANCE").get(null), f.mek(), f.waz());
        } catch (InvocationTargetException error) {
            rethrow(error.getCause());
        } catch (ReflectiveOperationException error) {
            fail("Tsukuyomi独立变体定制入口尚未实现", error);
        }
    }

    private static void rethrow(Throwable cause) {
        if (cause instanceof RuntimeException error) throw error;
        if (cause instanceof Error error) throw error;
        throw new AssertionError(cause);
    }

    private static Fixture fixture(String role, int targetGroup) throws Exception {
        Path mekPath = Path.of("src/main/resources/game/bhe/mek/" + role + "/" + role + ".mek");
        Path wazPath = Path.of("src/main/resources/game/bhe/waz/" + role + "/" + role + ".waz");
        BheBinService service = new BheBinService();
        var sourceMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mekPath.toString(), CHARSET).getData();
        var sourceWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wazPath.toString(), CHARSET).getData();
        Mek mek = new BheToBsdxMekConverter().convert(sourceMek);
        Waz waz = new BheToBsdxWazConverter().convert(sourceWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, role + "VariantsTest");
        int sourceGroup = mek.getMekBasicInfo().getWazFileSequence();
        mek.getMekBasicInfo().setWazFileSequence(targetGroup);
        // 给主WAZ加一空前缀，逼迫实现按最终身份定位退出目标，不能沿用源裸skill序号。
        waz.getSkillList().add(0, new Waz.Skill());
        for (var weapon : mek.getMekWeaponInfoMap().values()) weapon.setWazSequence(weapon.getWazSequence() + 1);
        for (var skill : waz.getSkillList()) for (var phase : skill.getPhasesInfo()) {
            for (var unit : phase.getSkillUnitCollection()) for (var event : unit.getSkillInfoObjectList()) {
                if (event instanceof CEventChange change) for (var collection : change.getBsdxInfoCollectionList2()) {
                    if (Integer.valueOf(16).equals(collection.getInt1()) && List.of(1).equals(collection.getTypeList())
                            && collection.getParamList().size() == 2 && collection.getParamList().get(0) == sourceGroup) {
                        collection.setParamList(new ArrayList<>(List.of(targetGroup, collection.getParamList().get(1) + 1)));
                    }
                }
            }
        }
        return new Fixture(mek, waz, mekPath, wazPath, sha256(mekPath), sha256(wazPath));
    }

    private static Snapshot snapshot(Fixture f) {
        return new Snapshot(f.mek().getMekWeaponInfoMap().size(), f.waz().getSkillList().size(),
                JSON.valueToTree(f.mek().getMekWeaponInfoMap()), JSON.valueToTree(f.waz().getSkillList()),
                JSON.valueToTree(f.mek().getMekMaterialBlock().getTrailingEntries()), JSON.valueToTree(f.mek().getMekAiInfoList()));
    }

    private static void assertAdded(Fixture f, Snapshot before, String name, String sequence, String english, int heat) {
        assertAdded(f, before, name, sequence, english, heat, 1);
    }

    private static void assertAdded(Fixture f, Snapshot before, String name, String sequence, String english, int heat, int addedCount) {
        assertEquals(before.weapons() + addedCount, f.mek().getMekWeaponInfoMap().size());
        assertEquals(before.skills() + addedCount, f.waz().getSkillList().size());
        var added = weapon(f.mek(), name, sequence);
        assertSame(added, f.mek().getMekWeaponInfoMap().get(before.weapons()));
        assertTrue(before.weapons() <= 152);
        assertEquals(before.skills(), added.getWazSequence());
        assertEquals(0, added.getWeaponCategory());
        assertEquals(heat, added.getHeatMinConsumption());
        assertEquals(heat, added.getHeatMaxConsumption());
        assertEquals(0, added.getUpgradeExp());
        assertEquals(english, f.waz().getSkillList().get(added.getWazSequence()).getSkillNameEnglish());
        for (int i = 0; i < before.weapons(); i++) assertEquals(before.weaponTree().get(Integer.toString(i)), JSON.valueToTree(f.mek().getMekWeaponInfoMap().get(i)));
        assertEquals(before.skillTree(), JSON.valueToTree(f.waz().getSkillList().subList(0, before.skills())));
        assertEquals(before.materialTree(), JSON.valueToTree(f.mek().getMekMaterialBlock().getTrailingEntries().subList(0, before.weapons())));
        assertEquals(before.aiTree(), JSON.valueToTree(f.mek().getMekAiInfoList()));
        assertMaterialViews(f.mek());
    }

    private static void assertMaterialViews(Mek mek) {
        var block = mek.getMekMaterialBlock();
        assertEquals(mek.getMekWeaponInfoMap().size(), block.getTrailingEntries().size());
        List<Mek.MekMaterialBlock.PluginEntry> expected = new ArrayList<>(block.getRegularEntries());
        expected.addAll(block.getTrailingEntries());
        assertEquals(JSON.valueToTree(expected), JSON.valueToTree(block.getEntries()));
    }

    private static void assertNoWritesOnRejection(Fixture f, Runnable action) {
        JsonNode mek = JSON.valueToTree(f.mek());
        JsonNode waz = JSON.valueToTree(f.waz());
        assertThrows(IllegalStateException.class, action::run);
        assertEquals(mek, JSON.valueToTree(f.mek()));
        assertEquals(waz, JSON.valueToTree(f.waz()));
    }

    private static void assertIdempotent(Fixture f, Runnable action) {
        JsonNode mek = JSON.valueToTree(f.mek());
        JsonNode waz = JSON.valueToTree(f.waz());
        action.run();
        assertEquals(mek, JSON.valueToTree(f.mek()));
        assertEquals(waz, JSON.valueToTree(f.waz()));
    }

    private void roundTrip(Fixture f, String name, String sequence, String english, int heat) throws Exception {
        BsdxBinService service = new BsdxBinService();
        Path mekFile = output.resolve(english + ".mek");
        Path wazFile = output.resolve(english + ".waz");
        service.generate(mekFile.toString(), f.mek(), CHARSET);
        service.generate(wazFile.toString(), f.waz(), CHARSET);
        Mek mek = (Mek) service.parse(mekFile.toString(), CHARSET).getData();
        Waz waz = (Waz) service.parse(wazFile.toString(), CHARSET).getData();
        var added = weapon(mek, name, sequence);
        assertEquals(heat, added.getHeatMinConsumption());
        assertEquals(english, waz.getSkillList().get(added.getWazSequence()).getSkillNameEnglish());
        assertEquals(payload(skill(f.waz(), name, english)), payload(waz.getSkillList().get(added.getWazSequence())),
                "变体完整事件与退出目标必须保存在实际WAZ字节中");
        assertEquals(payload(material(f.mek(), weapon(f.mek(), name, sequence))), payload(material(mek, added)));
        assertEquals(f.mek().getMekWeaponInfoMap().size(), mek.getMekMaterialBlock().getTrailingEntries().size());
        String text = added.getWeaponDescription();
        assertEquals(text, Charset.forName(CHARSET).decode(Charset.forName(CHARSET).newEncoder().encode(CharBuffer.wrap(text))).toString());
        assertEquals(weapon(f.mek(), name, sequence).getWeaponDescription(), text);
    }

    private static FollowupMekaContext context(Fixture f) {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setReboundMek(f.mek()); context.setReboundWaz(f.waz()); return context;
    }

    private static Mek.MekWeaponInfo weapon(Mek mek, String japanese, String sequence) {
        return mek.getMekWeaponInfoMap().values().stream().filter(value -> japanese.equals(value.getWeaponName())
                && sequence.equals(value.getWeaponSequence())).reduce((a, b) -> { throw new AssertionError("重复武装身份"); }).orElseThrow();
    }

    private static Waz.Skill skill(Waz waz, String japanese, String english) {
        return waz.getSkillList().stream().filter(value -> japanese.equals(value.getSkillNameJapanese())
                && english.equals(value.getSkillNameEnglish())).reduce((a, b) -> { throw new AssertionError("重复技能身份"); }).orElseThrow();
    }

    private static <T extends SkillInfoObject> List<T> events(Waz.Skill skill, int phase, int slot, Class<T> type) {
        return skill.getPhasesInfo().get(phase - 1).getSkillUnitCollection().stream()
                .filter(value -> Integer.valueOf(slot).equals(value.getUnitQuantity())).flatMap(value -> value.getSkillInfoObjectList().stream())
                .filter(type::isInstance).map(type::cast).toList();
    }

    private static <T extends SkillInfoObject> T event(Waz.Skill skill, int phase, int slot, Class<T> type, int start, int end) {
        return events(skill, phase, slot, type).stream().filter(value -> Integer.valueOf(start).equals(value.getStartFrame())
                && Integer.valueOf(end).equals(value.getEndFrame())).reduce((a, b) -> { throw new AssertionError("重复事件"); }).orElseThrow();
    }

    private static com.giga.nexas.dto.bsdx.BsdxInfoCollection power(CEventHit hit) {
        return ((CEventTerm) hit.getCeventHitUnitList().stream().filter(value -> Integer.valueOf(6).equals(value.getUnitSlotNum()))
                .findFirst().orElseThrow().getData()).getBsdxInfoCollectionList().get(0);
    }

    private static boolean rightCannon(CEventEffect effect) {
        var term = (CEventTerm) effect.getCeventEffectUnitList().stream().filter(value -> Integer.valueOf(7).equals(value.getUnitSlotNum()))
                .findFirst().orElseThrow().getData();
        var collection = term.getBsdxInfoCollectionList().get(0);
        assertEquals(20, collection.getInt1());
        assertEquals(List.of(1), collection.getTypeList());
        return List.of(0, 2).equals(collection.getIntList3());
    }

    private static List<Integer> speedValues(CEventValRandom value) {
        return List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6());
    }

    private static void assertJump(CEventChange change, int group, int skill) {
        assertEquals(1, change.getFlag());
        assertEquals(0, change.getBsdxInfoCollectionList1().get(0).getInt1());
        assertEquals(List.of(0), change.getBsdxInfoCollectionList1().get(0).getTypeList());
        var target = change.getBsdxInfoCollectionList2().get(0);
        assertEquals(16, target.getInt1());
        assertEquals(List.of(1), target.getTypeList());
        assertEquals(List.of(group, skill), target.getParamList());
    }

    private static void assertNormalEnd(CEventChange change) {
        assertEquals(1, change.getFlag());
        assertEquals(0, change.getInt1());
        assertEquals(1, change.getBsdxInfoCollectionList1().size());
        assertEquals(1, change.getBsdxInfoCollectionList2().size());
        var condition = change.getBsdxInfoCollectionList1().get(0);
        assertEquals(0, condition.getInt1());
        assertEquals(List.of(0), condition.getTypeList());
        assertEquals(List.of(), condition.getParamList());
        assertEquals(List.of(), condition.getIntList3());
        assertEquals(List.of(), condition.getIntList4());
        assertEquals(0, condition.getInt2());
        var destination = change.getBsdxInfoCollectionList2().get(0);
        assertEquals(16, destination.getInt1());
        assertEquals(List.of(0, 0), destination.getTypeList());
        assertEquals(List.of(), destination.getParamList());
        assertEquals(List.of(), destination.getIntList3());
        assertEquals(List.of(), destination.getIntList4());
        assertEquals(0, destination.getInt2());
    }

    private static Mek.MekMaterialBlock.PluginEntry material(Mek mek, Mek.MekWeaponInfo weapon) {
        return mek.getMekMaterialBlock().getTrailingEntries().get(new ArrayList<>(mek.getMekWeaponInfoMap().values()).indexOf(weapon));
    }

    private static void materialMarkers(Mek mek, Mek.MekWeaponInfo weapon, int sprite, int se, int voice) {
        var entry = material(mek, weapon);
        entry.getSpriteGroups().set(0, new int[]{sprite}); entry.getSeGroups().set(0, new int[]{se}); entry.getVoiceGroups().set(0, new int[]{voice});
        int index = new ArrayList<>(mek.getMekWeaponInfoMap().values()).indexOf(weapon);
        mek.getMekMaterialBlock().getEntries().set(mek.getMekMaterialBlock().getRegularEntries().size() + index, entry);
    }

    private static void assertSourceFiles(Fixture f) throws Exception {
        assertEquals(f.mekHash(), sha256(f.mekPath())); assertEquals(f.wazHash(), sha256(f.wazPath()));
    }

    private static JsonNode payload(Object value) {
        JsonNode tree = JSON.valueToTree(value);
        removeMetadata(tree);
        return tree;
    }

    private static void removeMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description"));
        node.forEach(CharacterWeaponVariantsTest::removeMetadata);
    }

    private static String sha256(Path file) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file)));
    }

    private record Fixture(Mek mek, Waz waz, Path mekPath, Path wazPath, String mekHash, String wazHash) { }
    private record Snapshot(int weapons, int skills, JsonNode weaponTree, JsonNode skillTree,
                            JsonNode materialTree, JsonNode aiTree) { }
}
