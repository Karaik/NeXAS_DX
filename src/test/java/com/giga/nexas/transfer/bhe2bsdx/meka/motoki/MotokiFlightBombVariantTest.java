package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
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
import java.security.MessageDigest;
import java.util.*;
import java.util.function.IntUnaryOperator;

import static org.junit.jupiter.api.Assertions.*;

/** Real BHE fixture; the local motion interpreter covers only this task's vertical motion and exits. */
class MotokiFlightBombVariantTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CHARSET = "windows-31j";
    private static final String NAME = "集中爆撃・帰投";
    private static final String ROUTER = "DX_FOCUSED_BOMB_RETURN";
    private static final String EXECUTION = "DX_FOCUSED_BOMB_EXECUTION";
    private static final String EXECUTION_NAME = "集中爆撃・帰投：制御";
    private static final Set<Integer> BOMB_CONTROL_UNITS = Set.of(7, 9, 10, 11, 31, 32, 33);
    @TempDir Path output;

    @Test
    void appendsOneWeaponAndTwoSkillsWithRealReboundFourPhaseGraph() throws Exception {
        Fixture f = fixture();
        markMaterial(f.mek, 13, 111, 311, 511);
        markMaterial(f.mek, 9, 222, 322, 522);
        markMaterial(f.mek, 10, 333, 333, 533);
        int weapons = f.mek.getMekWeaponInfoMap().size();
        int skills = f.waz.getSkillList().size();
        JsonNode oldWeapons = JSON.valueToTree(f.mek.getMekWeaponInfoMap());
        JsonNode oldSkills = JSON.valueToTree(f.waz.getSkillList());
        JsonNode oldMaterial = JSON.valueToTree(f.mek.getMekMaterialBlock().getTrailingEntries());
        JsonNode oldAi = JSON.valueToTree(f.mek.getMekAiInfoList());
        byte[] originalWaz = wazBytes(f.waz, "original");
        var sourceBomb = skill(f.waz, "飛行形態：集中爆撃", "0023");
        for (var donor : List.of(sourceBomb, skill(f.waz, "飛行形態：開始", "BOOST_DASH4"),
                skill(f.waz, "飛行形態：終了", "BOOST_DASH6"))) assertSingleEntrySuffix(donor);
        JsonNode bombEffect = effectPayloadOutsideBombControls(events(sourceBomb, 0, 48, CEventEffect.class).get(0));
        var expectedMaterial = materialUnion(f.mek, 13, 9, 10);

        customize(f);

        assertEquals(weapons + 1, f.mek.getMekWeaponInfoMap().size());
        assertEquals(skills + 2, f.waz.getSkillList().size(), "one router plus one private execution skill");
        var added = f.mek.getMekWeaponInfoMap().get(weapons);
        assertEquals(NAME, added.getWeaponName());
        assertEquals("0020", added.getWeaponSequence());
        assertEquals(0, added.getWeaponCategory());
        assertEquals(40, added.getHeatMinConsumption());
        assertEquals(40, added.getHeatMaxConsumption());
        assertEquals(0, added.getUpgradeExp());
        assertTrue(weapons <= 152);
        assertEquals(skills, added.getWazSequence());
        assertGraph(f.waz, skills);
        var newBombEffect = events(f.waz.getSkillList().get(skills + 1), 2, 48, CEventEffect.class).get(0);
        assertEquals(bombEffect, effectPayloadOutsideBombControls(newBombEffect),
                "outer fields, resources, six-bomb amount and every non-whitelisted unit survive");
        for (int i = 0; i < weapons; i++) assertEquals(oldWeapons.get(Integer.toString(i)), JSON.valueToTree(f.mek.getMekWeaponInfoMap().get(i)));
        assertEquals(oldSkills, JSON.valueToTree(f.waz.getSkillList().subList(0, skills)));
        assertEquals(oldMaterial, JSON.valueToTree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, weapons)));
        assertEquals(oldAi, JSON.valueToTree(f.mek.getMekAiInfoList()));
        assertEquals(expectedMaterial, payload(material(f.mek, weapons)));
        assertMaterialViews(f.mek);
        Waz originalPrefix = new Waz();
        originalPrefix.setExtensionName("waz");
        originalPrefix.setSkillList(new ArrayList<>(f.waz.getSkillList().subList(0, skills)));
        assertArrayEquals(originalWaz, wazBytes(originalPrefix, "preserved-prefix"), "all original skill bytes including old 9/13 remain intact");

        Fixture read = roundTrip(f, "graph");
        assertGraph(read.waz, skills);
        assertEquals(expectedMaterial, payload(material(read.mek, weapons)));
        assertEquals(payload(f.waz.getSkillList()), payload(read.waz.getSkillList()));
        String text = read.mek.getMekWeaponInfoMap().get(weapons).getWeaponDescription();
        assertEquals(text, Charset.forName(CHARSET).decode(Charset.forName(CHARSET).newEncoder().encode(CharBuffer.wrap(text))).toString());
        for (String keyword : List.of("離陸", "高度", "六", "着地")) assertTrue(text.contains(keyword), keyword);
        material(f.mek, weapons).getSpriteGroups().get(0)[0] = 999;
        events(requireExecution(f.waz), 0, 19, CEventValRandom.class).get(0).setInt2(999);
        assertInstanceOf(CEventTerm.class, effectUnit(newBombEffect, 7).getData())
                .getBsdxInfoCollectionList().get(0).getIntList3().set(0, 99);
        effectValue(newBombEffect, 9).setInt2(999);
        assertEquals(oldMaterial, JSON.valueToTree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, weapons)), "new material arrays must not alias donors");
        assertEquals(oldSkills, JSON.valueToTree(f.waz.getSkillList().subList(0, skills)), "execution phases must not alias original skills");
        assertSources(f);
    }

    @Test
    void parsedArtifactActuallyClimbsBombsAndLandsInsteadOfPassingByTimeout() throws Exception {
        Fixture f = fixture(); customize(f);
        Fixture read = roundTrip(f, "motion");
        var execution = requireExecution(read.waz);
        Motion ground = simulate(execution, 0, 100, 100, frame -> 0);
        assertEquals(6, ground.bombs);
        assertTrue(ground.playerCenterHeightAtRelease >= 200);
        assertEquals(80, ground.spawnZAtRelease);
        assertEquals(0, ground.targetZAtRelease);
        assertTrue(ground.maxZ > 0, "ground start must really gain height");
        assertTrue(ground.takeoffFrames >= 18 && ground.takeoffFrames < 240);
        assertTrue(ground.landed && ground.normalEnd);
        assertFalse(ground.cancelled);
        assertEquals(0, ground.finalZ);
        assertEquals(50, ground.finalGravity, "landing must leave the native baseline for the next weapon");

        Motion airborne = simulate(execution, 300, 100, 100, frame -> 0);
        assertEquals(0, airborne.takeoffFrames, "sufficient center height takes clock0 shortcut");
        assertEquals(300, airborne.maxZ, "clock0 transition must not perform another climb");
        assertEquals(6, airborne.bombs);
        assertEquals(80, airborne.spawnZAtRelease, "bomb spawn uses target Z+80, not the player's Z300");
        assertTrue(airborne.landed && airborne.normalEnd);

        Motion risingTarget = simulate(execution, 0, 100, 100, frame -> frame < 20 ? 0 : 160);
        assertEquals(6, risingTarget.bombs);
        assertTrue(risingTarget.takeoffFrames > ground.takeoffFrames);
        assertTrue(risingTarget.playerCenterHeightAtRelease >= 200);
        assertEquals(240, risingTarget.spawnZAtRelease);
        assertTrue(risingTarget.landed && risingTarget.normalEnd);
        Motion differentCenters = simulate(execution, 0, 80, 180, frame -> 0);
        assertEquals(6, differentCenters.bombs);
        assertTrue(differentCenters.takeoffFrames > ground.takeoffFrames, "HEIGHT2 uses both center offsets");

        Motion unreachable = simulate(execution, 0, 100, 100, frame -> 10000);
        assertEquals(0, unreachable.bombs);
        assertEquals(240, unreachable.takeoffFrames);
        assertTrue(unreachable.cancelled);
        assertTrue(unreachable.landed, "cancel path still descends under positive gravity");
    }

    @Test
    void rechecksHeightWhenTargetRisesAfterTakeoffAdmission() throws Exception {
        Fixture f = fixture(); customize(f);
        Fixture read = roundTrip(f, "late-target-rise");
        // P0 admits the airborne actor at height 200. The target then rises while
        // the bomb animation prepares, after the admission check has finished.
        Motion lateRise = simulate(requireExecution(read.waz), 200, 100, 100,
                frame -> frame == 0 ? 0 : 80);
        assertEquals(6, lateRise.bombs, "a reachable target must still receive a bomb release");
        assertTrue(lateRise.playerCenterHeightAtRelease >= 200, "player center-height admission still handles the late target rise");
        assertEquals(160, lateRise.spawnZAtRelease);
        assertTrue(lateRise.landed && lateRise.normalEnd);
        assertFalse(lateRise.cancelled);
        assertEquals(1, lateRise.preparationRetries, "the late rise must return through takeoff before release");
    }

    @Test
    void nativeTurnInputResolvesSuffixEntriesBeforeSelectingActualBlocks() throws Exception {
        Fixture f = fixture(); customize(f);
        Waz.Skill execution = requireExecution(roundTrip(f, "native-suffix-routing").waz);
        assertExecutionSuffix(execution);
        // 60E9C0 reads the requested suffix index, then passes entry.int1 to
        // 60E130 as the actual block. Redirecting entry1 to landing must skip bombs.
        execution.getSkillSuffixList().get(1).setInt1(3);
        Motion redirected = simulate(execution, 0, 100, 100, actorPass -> 0);
        assertEquals(0, redirected.bombs, "motion interpretation must consume suffix routing, not the requested index as a block");
        assertTrue(redirected.landed && redirected.normalEnd);
        assertThrows(AssertionError.class, () -> resolveSuffixBlock(execution, -1));
        assertThrows(AssertionError.class, () -> resolveSuffixBlock(execution, 4));
        execution.getSkillSuffixList().get(1).setInt1(4);
        assertThrows(AssertionError.class, () -> resolveSuffixBlock(execution, 1), "out-of-range actual block must not be simulated");
    }

    @Test
    void nativeAbsoluteModifierWorkStaysAtHundredFromFirstThroughLastFrame() throws Exception {
        Fixture f = fixture(); customize(f);
        Waz.Skill execution = requireExecution(roundTrip(f, "native-modifier-values").waz);
        for (int block = 0; block < execution.getPhasesInfo().size(); block++) {
            for (int slot : List.of(27, 28)) {
                var modifiers = events(execution, block, slot, CEventVal.class);
                assertEquals(1, modifiers.size(), "one explicit absolute modifier per block and slot");
                var value = modifiers.get(0);
                int first = value.getStartFrame(), last = value.getEndFrame();
                for (int clock : List.of(first, first + (last - first) / 2, last)) {
                    assertEquals(100, absoluteValAt(value, clock), "modifier must already be 100 at block " + block + ", slot " + slot + ", clock " + clock);
                }
                value.setInt1(1);
                assertThrows(AssertionError.class, () -> absoluteValAt(value, first), "relative-start mode needs a prior Work value; local subset must reject it");
                value.setInt1(0); value.setInt3(1);
                assertThrows(AssertionError.class, () -> absoluteValAt(value, first), "relative-end mode must not be treated as an absolute value");
            }
        }
    }

    @Test
    void reparsedBombParentUsesTargetPositionAndCrossesItsCenterWithinFewVerticalSteps() throws Exception {
        Fixture f = fixture(); customize(f);
        Waz.Skill execution = requireExecution(roundTrip(f, "target-bomb-parent").waz);
        var effect = events(execution, 2, 48, CEventEffect.class).get(0);
        assertStableBombControls(effect);
        for (int targetZ : List.of(0, 98, 250)) {
            BombLaunch launch = decodeBombLaunch(effect, targetZ);
            assertEquals(6, launch.count);
            assertEquals(targetZ + 80, launch.spawnZ);
            assertEquals(0, launch.horizontalSpeed);
            assertEquals(-24, launch.verticalSpeed);
            assertEquals(50, launch.gravity);
            int targetCenterZ = targetZ + 50;
            int zRaw = launch.spawnZ * 1024, vzRaw = launch.verticalSpeed * 1024;
            assertTrue(zRaw > targetCenterZ * 1024);
            int crossedAt = -1;
            // 5F8C10 subset: 100% modifier, fixed initial Vz and gravityRaw/200.
            // This checks vertical passage only; it does not simulate hit shapes or prove a combo.
            for (int step = 1; step <= 8; step++) {
                zRaw = Math.max(0, zRaw + vzRaw);
                if (zRaw <= targetCenterZ * 1024) { crossedAt = step; break; }
                vzRaw -= launch.gravity * 1024 / 200;
            }
            assertTrue(crossedAt > 0 && crossedAt <= 4, "candidate must pass the stationary target center in a few steps");
        }
    }

    @Test
    void retriesRepeatedPreparationRisesAndCancelsAnUnreachableLateRiseWithoutBombing() throws Exception {
        Fixture f = fixture(); customize(f);
        Waz.Skill execution = requireExecution(roundTrip(f, "repeated-late-target-rise").waz);
        Motion repeatedRise = simulate(execution, 200, 100, 100,
                actorPass -> actorPass == 0 ? 0 : actorPass < 36 ? 80 : 240);
        assertTrue(repeatedRise.preparationRetries >= 2, "both preparation-period rises need another height admission");
        assertEquals(6, repeatedRise.bombs);
        assertTrue(repeatedRise.playerCenterHeightAtRelease >= 200);
        assertEquals(320, repeatedRise.spawnZAtRelease);
        assertTrue(repeatedRise.landed && repeatedRise.normalEnd);
        assertFalse(repeatedRise.cancelled);

        Motion unreachableLateRise = simulate(execution, 200, 100, 100,
                actorPass -> actorPass == 0 ? 0 : 10000);
        assertEquals(1, unreachableLateRise.preparationRetries);
        assertEquals(0, unreachableLateRise.bombs);
        assertTrue(unreachableLateRise.cancelled && unreachableLateRise.landed);
        assertFalse(unreachableLateRise.normalEnd);
        assertEquals(50, unreachableLateRise.finalGravity);
    }

    @Test
    void repeatsWithIdenticalBytesAndRejectsExistingPayloadDrift() throws Exception {
        Fixture f = fixture(); customize(f);
        byte[] mekOnce = mekBytes(f.mek, "once");
        byte[] wazOnce = wazBytes(f.waz, "once");
        customize(f);
        assertArrayEquals(mekOnce, mekBytes(f.mek, "twice"));
        assertArrayEquals(wazOnce, wazBytes(f.waz, "twice"));
        var execution = requireExecution(f.waz);
        events(execution, 0, 19, CEventValRandom.class).get(0).setInt2(999);
        assertNoWrites(f);
    }

    @Test
    void rejectsBadTakeoffBombLandingAndMaterialDonorsBeforeAnyCommit() throws Exception {
        Fixture takeoff = fixture();
        events(skill(takeoff.waz, "飛行形態：開始", "BOOST_DASH4"), 0, 19, CEventValRandom.class).get(0).setInt2(999);
        assertNoWrites(takeoff);
        Fixture bomb = fixture();
        amount(events(skill(bomb.waz, "飛行形態：集中爆撃", "0023"), 0, 48, CEventEffect.class).get(0)).setInt5(5);
        assertNoWrites(bomb);
        Fixture unknownBomb = fixture();
        var bombUnit = skill(unknownBomb.waz, "飛行形態：集中爆撃", "0023").getPhasesInfo().get(0).getSkillUnitCollection().stream()
                .filter(u -> Integer.valueOf(10).equals(u.getUnitQuantity())).findFirst().orElseThrow();
        var unknown = new SkillInfoUnknown(255); unknown.setStartFrame(0); unknown.setEndFrame(20);
        unknown.setInt1(-1); unknown.setInt2(1); unknown.setInt3(-2);
        bombUnit.getSkillInfoUnknownList().add(unknown);
        assertNoWrites(unknownBomb);
        Fixture landing = fixture();
        var end = events(skill(landing.waz, "飛行形態：終了", "BOOST_DASH6"), 0, 71, CEventChange.class).get(0);
        end.setStartFrame(31); end.setEndFrame(31);
        assertNoWrites(landing);
        Fixture material = fixture();
        material.mek.getMekMaterialBlock().getTrailingEntries().remove(0);
        assertNoWrites(material);
    }

    @Test
    void rejectsChangedSourceBombControlSignaturesWithoutWrites() throws Exception {
        for (int slot : List.of(7, 9, 10, 31, 33)) {
            Fixture f = fixture();
            var effect = sourceBombEffect(f.waz);
            assertOriginalBombControls(effect);
            if (slot == 7) {
                assertInstanceOf(CEventTerm.class, effectUnit(effect, slot).getData())
                        .getBsdxInfoCollectionList().get(0).getIntList3().set(0, 1);
            } else {
                var value = effectValue(effect, slot); value.setInt5(value.getInt5() + 1);
            }
            assertNoWrites(f);
            assertSources(f);
        }
    }

    @Test
    void rejectsUnexpectedSourceHeightOrVerticalSpeedOverrideWithoutWrites() throws Exception {
        for (int slot : List.of(11, 32)) {
            Fixture f = fixture();
            var effect = sourceBombEffect(f.waz);
            assertOriginalBombControls(effect);
            var unexpected = new CEventEffect.CEventEffectUnit();
            unexpected.setUnitSlotNum(slot); unexpected.setCeventEffectUnitQuantity(slot); unexpected.setBuffer(1);
            var value = new CEventValRandom(3); value.setStartFrame(0); value.setEndFrame(0);
            value.setInt1(0); value.setInt2(0); value.setInt3(0);
            value.setInt4(0); value.setInt5(0); value.setInt6(0);
            unexpected.setData(value);
            effect.getCeventEffectUnitList().removeIf(unit -> Integer.valueOf(slot).equals(unit.getUnitSlotNum()));
            effect.getCeventEffectUnitList().add(unexpected);
            assertNoWrites(f);
            assertSources(f);
        }
    }

    @Test
    void rejectsBombDonorSuffixThatSelectsMissingBlockWithoutWrites() throws Exception {
        Fixture f = fixture();
        var donor = skill(f.waz, "飛行形態：集中爆撃", "0023");
        assertSingleEntrySuffix(donor);
        donor.getSkillSuffixList().get(0).setInt1(99);
        assertNoWrites(f);
        assertSources(f);
    }

    @Test
    void rejectsBombDonorSuffixThatSkipsItsOnlyBlockWithoutWrites() throws Exception {
        Fixture f = fixture();
        var donor = skill(f.waz, "飛行形態：集中爆撃", "0023");
        assertSingleEntrySuffix(donor);
        donor.getSkillSuffixList().get(0).setInt2(1);
        assertNoWrites(f);
        assertSources(f);
    }

    @Test
    void rejectsBombDonorWithoutSuffixEntriesWithoutWrites() throws Exception {
        Fixture f = fixture();
        var donor = skill(f.waz, "飛行形態：集中爆撃", "0023");
        assertSingleEntrySuffix(donor);
        donor.getSkillSuffixList().clear();
        assertNoWrites(f);
        assertSources(f);
    }

    @Test
    void rejectsTakeoffAndLandingDonorSuffixDriftWithoutWrites() throws Exception {
        for (String[] identity : List.of(new String[]{"飛行形態：開始", "BOOST_DASH4"},
                new String[]{"飛行形態：終了", "BOOST_DASH6"})) {
            Fixture f = fixture();
            var donor = skill(f.waz, identity[0], identity[1]);
            assertSingleEntrySuffix(donor);
            donor.getSkillSuffixList().get(0).setInt1(99);
            assertNoWrites(f);
            assertSources(f);
        }
    }

    @Test
    void rejectsHalfBuiltAndConflictingPrivateIdentitiesWithoutWrites() throws Exception {
        Fixture missingExecution = fixture(); customize(missingExecution);
        var execution = requireExecution(missingExecution.waz);
        missingExecution.waz.getSkillList().remove(execution);
        assertNoWrites(missingExecution);
        Fixture missingRouter = fixture(); customize(missingRouter);
        requireExecution(missingRouter.waz);
        missingRouter.waz.getSkillList().removeIf(s -> ROUTER.equals(s.getSkillNameEnglish()));
        assertNoWrites(missingRouter);
        Fixture occupied = fixture();
        skill(occupied.waz, "飛行形態：飛行中", "BOOST_DASH5").setSkillNameEnglish(EXECUTION);
        assertNoWrites(occupied);
        Fixture routerDrift = fixture(); customize(routerDrift);
        requireExecution(routerDrift.waz);
        events(skill(routerDrift.waz, NAME, ROUTER), 0, 71, CEventChange.class).get(0)
                .getBsdxInfoCollectionList2().get(0).getParamList().set(0, 999);
        assertNoWrites(routerDrift);
    }

    private static void assertGraph(Waz waz, int routerIndex) {
        Waz.Skill router = waz.getSkillList().get(routerIndex);
        assertEquals(NAME, router.getSkillNameJapanese());
        assertEquals(ROUTER, router.getSkillNameEnglish());
        assertEquals(1, router.getPhaseQuantity());
        assertEquals(1, router.getPhasesInfo().size());
        assertSingleEntrySuffix(router);
        assertEquals(0, resolveSuffixBlock(router, 0), "router entry must reach its one actual block after binary roundtrip");
        assertTrue(router.getPhasesInfo().get(0).getSkillUnitCollection().stream().allMatch(u -> u.getSkillInfoUnknownList().isEmpty()));
        var routeEvents = allEvents(router, 0);
        assertEquals(1, routeEvents.size(), "router only contains zero-frame JUMP/WAZA");
        CEventChange route = assertInstanceOf(CEventChange.class, routeEvents.get(0));
        assertFrames(route, 0, 0);
        assertAlways(route);
        assertCollection(route.getBsdxInfoCollectionList2().get(0), 16, List.of(1), List.of(212, routerIndex + 1), List.of(), List.of());
        Waz.Skill execution = waz.getSkillList().get(routerIndex + 1);
        assertEquals(EXECUTION_NAME, execution.getSkillNameJapanese());
        assertEquals(EXECUTION, execution.getSkillNameEnglish());
        assertEquals(4, execution.getPhaseQuantity());
        assertEquals(4, execution.getPhasesInfo().size());
        assertExecutionSuffix(execution);
        assertValues(events(execution, 0, 18, CEventValRandom.class), List.of(0, 0, 0, 0, 0, 0));
        assertValues(events(execution, 0, 19, CEventValRandom.class), List.of(0, 8, 0, 0, 8, 0));
        assertValues(events(execution, 0, 21, CEventValRandom.class), List.of(0, 0, 0, 0, 0, 0));
        assertTrue(events(execution, 0, 40, CEventVal.class).isEmpty(), "takeoff must not retain -1000 heat loop");
        var p0 = events(execution, 0, 71, CEventChange.class);
        assertEquals(3, p0.size());
        assertHeightJump(change(p0, 0, 0), 1);
        assertHeightJump(change(p0, 18, 239), 1);
        assertPhaseJump(change(p0, 240, 240), 3); assertAlways(change(p0, 240, 240));

        assertValues(events(execution, 1, 19, CEventValRandom.class), List.of(0, 0, 0, 0, 0, 0));
        assertValues(events(execution, 1, 21, CEventValRandom.class), List.of(0, 0, 0, 0, 0, 0));
        assertTrue(events(execution, 1, 48, CEventEffect.class).isEmpty(), "preparation cannot release a bomb");
        var preparationSprites = events(execution, 1, 0, CEventSprite.class);
        assertEquals(2, preparationSprites.size());
        assertFrames(preparationSprites.get(0), 0, 5); assertFrames(preparationSprites.get(1), 6, 9);
        var p1 = events(execution, 1, 71, CEventChange.class);
        assertEquals(2, p1.size());
        assertFrames(p1.get(0), 10, 10); assertHeightJump(p1.get(0), 2);
        assertFrames(p1.get(1), 10, 10); assertAlways(p1.get(1)); assertPhaseJump(p1.get(1), 0);

        assertValues(events(execution, 2, 19, CEventValRandom.class), List.of(0, 0, 0, 0, 0, 0));
        assertValues(events(execution, 2, 21, CEventValRandom.class), List.of(0, 0, 0, 0, 0, 0));
        var effects = events(execution, 2, 48, CEventEffect.class);
        assertEquals(1, effects.size()); assertFrames(effects.get(0), 0, 0);
        assertEquals(List.of(0, 1, 0, 0, 6, 0), values(amount(effects.get(0))));
        assertStableBombControls(effects.get(0));
        var releaseSprites = events(execution, 2, 0, CEventSprite.class);
        assertEquals(2, releaseSprites.size());
        assertFrames(releaseSprites.get(0), 0, 49); assertFrames(releaseSprites.get(1), 50, 79);
        var speeds = events(execution, 2, 18, CEventValRandom.class);
        assertEquals(2, speeds.size()); assertFrames(speeds.get(0), 0, 1); assertFrames(speeds.get(1), 2, 49);
        assertEquals(List.of(1, 0, 0, 0, 12, 0), values(speeds.get(0)));
        assertEquals(List.of(1, 0, 0, 0, 8, 0), values(speeds.get(1)));
        var p2 = events(execution, 2, 71, CEventChange.class);
        assertEquals(1, p2.size()); assertFrames(p2.get(0), 79, 79);
        assertAlways(p2.get(0)); assertPhaseJump(p2.get(0), 3);

        assertValues(events(execution, 3, 18, CEventValRandom.class), List.of(0, 0, 0, 0, 0, 0));
        var clear = events(execution, 3, 19, CEventValRandom.class);
        assertEquals(1, clear.size()); assertFrames(clear.get(0), 0, 0);
        assertEquals(List.of(0, 0, 0, 0, 0, 0), values(clear.get(0)));
        assertValues(events(execution, 3, 21, CEventValRandom.class), List.of(0, 50, 0, 0, 50, 0));
        var p3 = events(execution, 3, 71, CEventChange.class);
        assertEquals(2, p3.size());
        var landed = change(p3, 30, 359);
        assertEquals(1, landed.getFlag());
        assertCollection(landed.getBsdxInfoCollectionList1().get(0), 0, List.of(2, 7, 16), List.of(), List.of(0, 0), List.of());
        assertEnd(landed);
        var cancel = change(p3, 360, 360); assertAlways(cancel); assertEnd(cancel);
        for (int phase = 0; phase < 4; phase++) {
            for (var value : events(execution, phase, 19, CEventValRandom.class))
                assertEquals(3, value.getTypeId(), "JumpPower must retain BSDX slot19 factory type");
            for (var value : events(execution, phase, 21, CEventValRandom.class))
                assertEquals(2, value.getTypeId(), "Gravity must retain BSDX slot21 factory type");
            for (int slot : List.of(27, 28)) {
                var modifier = events(execution, phase, slot, CEventVal.class);
                assertFalse(modifier.isEmpty(), "explicit gravity/jump modifier100");
                for (var value : modifier) {
                    assertEquals(0, value.getTypeId(), "modifier must retain native integer payload type");
                    assertEquals(List.of(0, 100, 0, 100), List.of(value.getInt1(), value.getInt2(), value.getInt3(), value.getInt4()));
                }
            }
        }
    }

    private static void assertStableBombControls(CEventEffect effect) {
        var position = assertInstanceOf(CEventTerm.class, effectUnit(effect, 7).getData());
        assertEquals(5, position.getTypeId()); assertFrames(position, 0, 0);
        assertEquals(1, position.getBsdxInfoCollectionList().size());
        assertCollection(position.getBsdxInfoCollectionList().get(0), 20, List.of(1), List.of(), List.of(1, 0), List.of());
        for (int slot : List.of(9, 10, 11, 31, 32, 33)) {
            var value = effectValue(effect, slot);
            int expected = switch (slot) { case 11 -> 80; case 32 -> -24; case 33 -> 50; default -> 0; };
            assertEquals(slot == 33 ? 2 : 3, value.getTypeId());
            assertFrames(value, 0, 0);
            assertEquals(List.of(0, expected, 0, 0, expected, 0), values(value), "deterministic parent unit " + slot);
            assertEquals(expected, singleFrameFixedValue(value), "single-frame Work consumes its ending value");
        }
    }

    private static void assertOriginalBombControls(CEventEffect effect) {
        var position = assertInstanceOf(CEventTerm.class, effectUnit(effect, 7).getData());
        assertCollection(position.getBsdxInfoCollectionList().get(0), 20, List.of(1), List.of(), List.of(0, 0), List.of());
        assertEquals(List.of(0, 0, 0, 0, 0, 60), values(effectValue(effect, 9)));
        assertEquals(List.of(0, 0, 0, 0, 0, 60), values(effectValue(effect, 10)));
        assertEquals(List.of(0, 2, 6, 0, 0, 6), values(effectValue(effect, 31)));
        assertEquals(List.of(0, 50, 100, 0, 5, 10), values(effectValue(effect, 33)));
        for (int slot : List.of(11, 32)) assertTrue(effect.getCeventEffectUnitList().stream()
                .noneMatch(unit -> Integer.valueOf(slot).equals(unit.getUnitSlotNum()) && unit.getBuffer() != 0));
    }

    /** Only the parent's legal deterministic spawn subset, not collision or an entire projectile WAZ. */
    private static BombLaunch decodeBombLaunch(CEventEffect effect, int targetZ) {
        var position = assertInstanceOf(CEventTerm.class, effectUnit(effect, 7).getData());
        assertCollection(position.getBsdxInfoCollectionList().get(0), 20, List.of(1), List.of(), List.of(1, 0), List.of());
        assertEquals(0, singleFrameFixedValue(effectValue(effect, 9)), "local subset requires no offset angle");
        assertEquals(0, singleFrameFixedValue(effectValue(effect, 10)), "local subset requires no radial scatter");
        return new BombLaunch(singleFrameFixedValue(amount(effect)), targetZ + singleFrameFixedValue(effectValue(effect, 11)),
                singleFrameFixedValue(effectValue(effect, 31)), singleFrameFixedValue(effectValue(effect, 32)),
                singleFrameFixedValue(effectValue(effect, 33)));
    }

    /** 5E6AD0 / 5E68F0: a 0..0 Work selects the ending value and ending random width. */
    private static int singleFrameFixedValue(CEventValRandom value) {
        assertTrue(value.getTypeId() == 2 || value.getTypeId() == 3, "supported integer/double ValRandom only");
        assertFrames(value, 0, 0);
        assertEquals(0, value.getInt1()); assertEquals(0, value.getInt4(), "local subset rejects relative modes");
        assertEquals(0, value.getInt3()); assertEquals(0, value.getInt6(), "local subset rejects random widths");
        return value.getInt5();
    }

    private static void assertHeightJump(CEventChange change, int phase) {
        assertEquals(1, change.getFlag());
        assertEquals(1, change.getBsdxInfoCollectionList1().size());
        assertCollection(change.getBsdxInfoCollectionList1().get(0), 0, List.of(2, 3, 4, 3), List.of(200), List.of(0, 0), List.of(1, 0));
        assertPhaseJump(change, phase);
    }
    private static void assertExecutionSuffix(Waz.Skill execution) {
        assertEquals(List.of(List.of(0, 0), List.of(1, 0), List.of(2, 0), List.of(3, 0)),
                execution.getSkillSuffixList().stream().map(value -> List.of(value.getInt1(), value.getInt2())).toList(),
                "TURN/INPUT selects a suffix entry; all four actual blocks need non-skipping suffix entries");
    }
    private static void assertSingleEntrySuffix(Waz.Skill skill) {
        assertEquals(List.of(List.of(0, 0)), skill.getSkillSuffixList().stream()
                        .map(value -> List.of(value.getInt1(), value.getInt2())).toList(),
                "one-block donors and the new router must have exactly one executable suffix entry to block0");
    }
    private static void assertPhaseJump(CEventChange change, int phase) {
        assertEquals(1, change.getBsdxInfoCollectionList2().size());
        assertCollection(change.getBsdxInfoCollectionList2().get(0), 16, List.of(0, 1), List.of(phase), List.of(), List.of());
    }
    private static void assertEnd(CEventChange change) {
        assertCollection(change.getBsdxInfoCollectionList2().get(0), 16, List.of(0, 2), List.of(), List.of(), List.of());
    }
    private static void assertAlways(CEventChange change) {
        assertEquals(1, change.getFlag());
        assertEquals(1, change.getBsdxInfoCollectionList1().size());
        assertCollection(change.getBsdxInfoCollectionList1().get(0), 0, List.of(0), List.of(), List.of(), List.of());
    }
    private static void assertCollection(BsdxInfoCollection c, int root, List<Integer> types, List<Integer> params, List<Integer> aux3, List<Integer> aux4) {
        assertEquals(root, c.getInt1()); assertEquals(types, c.getTypeList());
        assertEquals(params, c.getParamList()); assertEquals(aux3, c.getIntList3());
        assertEquals(aux4, c.getIntList4()); assertEquals(0, c.getInt2());
    }

    /**
     * Independent, task-local interpreter, not a complete engine simulator. Native scalar vectors use
     * 1024 units (60FCE0); 5F8C10 subtracts gravityRaw/200, and HEIGHT2 compares Z+bodyHeight/2.
     * Read only reparsed artifact events; no production builder or expected graph drives this loop.
     * TURN/INPUT addresses the suffix table (60E9C0), whose int1 is the actual block (60E130).
     * A queued phase entry creates another actor pass, not a vertical-physics step. Target samples may
     * change between passes because 717F80 can update the target before re-entering this actor.
     */
    private static Motion simulate(Waz.Skill skill, int initialZ, int selfBodyHeight, int targetBodyHeight, IntUnaryOperator targetZ) {
        int phase = resolveSuffixBlock(skill, 0), clock = 0, vz = 0, gravity = 0, jumpModifier = 100, gravityModifier = 100;
        int zRaw = initialZ * 1024, maxZ = initialZ, bombs = 0, playerCenterHeightAtRelease = Integer.MIN_VALUE, takeoffFrames = -1;
        int spawnZAtRelease = Integer.MIN_VALUE, targetZAtRelease = Integer.MIN_VALUE;
        int preparationRetries = 0;
        boolean cancelled = false, landed = false;
        for (int actorPass = 0; actorPass < 1000; actorPass++) {
            for (var v : events(skill, phase, 19, CEventValRandom.class)) if (active(v, clock)) {
                assertEquals(0, v.getInt1(), "local subset expects absolute deterministic Vz");
                assertEquals(v.getInt2(), v.getInt5()); vz = v.getInt2() * 1024;
            }
            for (var v : events(skill, phase, 21, CEventValRandom.class)) if (active(v, clock)) {
                assertEquals(0, v.getInt1()); assertEquals(v.getInt2(), v.getInt5()); gravity = v.getInt2() * 1024;
            }
            for (var v : events(skill, phase, 28, CEventVal.class)) if (active(v, clock)) jumpModifier = absoluteValAt(v, clock);
            for (var v : events(skill, phase, 27, CEventVal.class)) if (active(v, clock)) gravityModifier = absoluteValAt(v, clock);
            int z = (zRaw + 512) >> 10;
            int height2 = z + selfBodyHeight / 2 - targetZ.applyAsInt(actorPass) - targetBodyHeight / 2;
            for (var effect : events(skill, phase, 48, CEventEffect.class)) if (active(effect, clock)) {
                targetZAtRelease = targetZ.applyAsInt(actorPass);
                var launch = decodeBombLaunch(effect, targetZAtRelease);
                bombs += launch.count; spawnZAtRelease = launch.spawnZ; playerCenterHeightAtRelease = height2;
            }
            boolean transitioned = false;
            for (var change : events(skill, phase, 71, CEventChange.class)) {
                if (!active(change, clock)) continue;
                var c = change.getBsdxInfoCollectionList1().get(0);
                boolean condition;
                if (List.of(0).equals(c.getTypeList())) condition = true;
                else if (List.of(2, 3, 4, 3).equals(c.getTypeList())) condition = height2 >= c.getParamList().get(0);
                else if (List.of(2, 7, 16).equals(c.getTypeList())) condition = zRaw == 0 && vz <= 0;
                else throw new AssertionError("unsupported local condition " + c.getTypeList());
                if (!condition) continue;
                var destination = change.getBsdxInfoCollectionList2().get(0);
                if (List.of(0, 2).equals(destination.getTypeList())) {
                    boolean normal = List.of(2, 7, 16).equals(c.getTypeList());
                    return new Motion(bombs, playerCenterHeightAtRelease, spawnZAtRelease, targetZAtRelease,
                            maxZ, z, takeoffFrames, cancelled || !normal, landed, normal && !cancelled,
                            preparationRetries, gravity / 1024);
                }
                assertEquals(List.of(0, 1), destination.getTypeList());
                if (phase == 0) { takeoffFrames = clock; cancelled = clock == 240; }
                int destinationBlock = resolveSuffixBlock(skill, destination.getParamList().get(0));
                if (phase == 1 && destinationBlock == 0) preparationRetries++;
                phase = destinationBlock; clock = 0; transitioned = true; break;
            }
            if (transitioned) continue;
            zRaw = Math.max(0, zRaw + vz * Math.max(0, jumpModifier) / 100);
            if (phase == skill.getPhasesInfo().size() - 1 && zRaw == 0) landed = true;
            if (zRaw > 0) vz -= gravity / 200 * Math.max(0, gravityModifier) / 100;
            else if (vz < 0) vz = 0;
            maxZ = Math.max(maxZ, (zRaw + 512) >> 10); clock++;
        }
        throw new AssertionError("local motion failed to reach END");
    }

    /** Local subset of 60E9C0: positive int2 entries are skipped; missing routing is rejected. */
    private static int resolveSuffixBlock(Waz.Skill skill, int requestedEntry) {
        assertTrue(requestedEntry >= 0 && requestedEntry < skill.getSkillSuffixList().size(),
                "TURN/INPUT suffix index outside the emitted suffix table: " + requestedEntry);
        for (int entry = requestedEntry; entry < skill.getSkillSuffixList().size(); entry++) {
            var suffix = skill.getSkillSuffixList().get(entry);
            if (suffix.getInt2() > 0) continue;
            int block = suffix.getInt1();
            assertTrue(block >= 0 && block < skill.getPhasesInfo().size(), "suffix actual block outside emitted blocks: " + block);
            return block;
        }
        throw new AssertionError("TURN/INPUT has no executable suffix entry at or after " + requestedEntry);
    }

    /** 5E6BA0 initializes absolute endpoints; type0 Work uses 5E68F0 integer interpolation. */
    private static int absoluteValAt(CEventVal value, int clock) {
        assertEquals(0, value.getTypeId(), "modifier must retain the native type0 Work");
        assertEquals(0, value.getInt1(), "local subset only supports absolute starting values");
        assertEquals(0, value.getInt3(), "local subset only supports absolute ending values");
        assertTrue(active(value, clock), "modifier sampled outside its active event window");
        int duration = value.getEndFrame() - value.getStartFrame();
        if (duration == 0 || clock == value.getEndFrame()) return value.getInt4();
        return value.getInt2() + (int) ((long) (clock - value.getStartFrame())
                * (value.getInt4() - value.getInt2()) / duration);
    }

    private static Fixture fixture() throws Exception {
        Path mekPath = Path.of("src/main/resources/game/bhe/mek/motoki/motoki.mek");
        Path wazPath = Path.of("src/main/resources/game/bhe/waz/motoki/motoki.waz");
        var service = new BheBinService();
        var sourceMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mekPath.toString(), CHARSET).getData();
        var sourceWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wazPath.toString(), CHARSET).getData();
        JsonNode sourceMekTree = JSON.valueToTree(sourceMek), sourceWazTree = JSON.valueToTree(sourceWaz);
        Mek mek = new BheToBsdxMekConverter().convert(sourceMek);
        Waz waz = new BheToBsdxWazConverter().convert(sourceWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, "MotokiFlightBombVariantTest");
        int sourceGroup = mek.getMekBasicInfo().getWazFileSequence();
        mek.getMekBasicInfo().setWazFileSequence(212);
        waz.getSkillList().add(0, new Waz.Skill());
        for (var weapon : mek.getMekWeaponInfoMap().values()) weapon.setWazSequence(weapon.getWazSequence() + 1);
        for (var skill : waz.getSkillList()) for (var phase : skill.getPhasesInfo()) {
            for (var unit : phase.getSkillUnitCollection()) for (var event : unit.getSkillInfoObjectList()) {
                if (event instanceof CEventChange change) for (var c : change.getBsdxInfoCollectionList2()) {
                    if (Integer.valueOf(16).equals(c.getInt1()) && List.of(1).equals(c.getTypeList())
                            && c.getParamList().size() == 2 && c.getParamList().get(0) == sourceGroup)
                        c.setParamList(new ArrayList<>(List.of(212, c.getParamList().get(1) + 1)));
                }
            }
        }
        var hashes = new LinkedHashMap<Path, String>();
        for (Path path : List.of(mekPath, wazPath, Path.of("src/main/resources/game/bhe/waz/bomb.waz"), Path.of("src/main/resources/game/bsdx/waz/Bomb.waz"))) hashes.put(path, sha256(path));
        return new Fixture(mek, waz, hashes, sourceMek, sourceWaz, sourceMekTree, sourceWazTree);
    }
    private static void customize(Fixture f) {
        var context = new FollowupMekaContext(); context.setReboundMek(f.mek); context.setReboundWaz(f.waz);
        MotokiCustomizer.INSTANCE.customizeBeforeWrite(context);
    }
    private Fixture roundTrip(Fixture f, String name) throws Exception {
        var service = new BsdxBinService();
        Path mekFile = output.resolve(name + ".mek"), wazFile = output.resolve(name + ".waz");
        service.generate(mekFile.toString(), f.mek, CHARSET); service.generate(wazFile.toString(), f.waz, CHARSET);
        return new Fixture((Mek) service.parse(mekFile.toString(), CHARSET).getData(), (Waz) service.parse(wazFile.toString(), CHARSET).getData(), f.hashes, f.sourceMek, f.sourceWaz, f.sourceMekTree, f.sourceWazTree);
    }
    private byte[] wazBytes(Waz waz, String name) throws Exception {
        Path file = output.resolve(name + ".waz"); new BsdxBinService().generate(file.toString(), waz, CHARSET); return Files.readAllBytes(file);
    }
    private byte[] mekBytes(Mek mek, String name) throws Exception {
        Path file = output.resolve(name + ".mek"); new BsdxBinService().generate(file.toString(), mek, CHARSET); return Files.readAllBytes(file);
    }
    private static void assertNoWrites(Fixture f) {
        JsonNode mek = JSON.valueToTree(f.mek), waz = JSON.valueToTree(f.waz);
        assertThrows(IllegalStateException.class, () -> customize(f));
        assertEquals(mek, JSON.valueToTree(f.mek)); assertEquals(waz, JSON.valueToTree(f.waz));
    }
    private static void assertSources(Fixture f) throws Exception {
        for (var entry : f.hashes.entrySet()) assertEquals(entry.getValue(), sha256(entry.getKey()), entry.getKey().toString());
        assertEquals(f.sourceMekTree, JSON.valueToTree(f.sourceMek)); assertEquals(f.sourceWazTree, JSON.valueToTree(f.sourceWaz));
    }
    private static Waz.Skill requireExecution(Waz waz) {
        var matches = waz.getSkillList().stream().filter(s -> EXECUTION.equals(s.getSkillNameEnglish())).toList();
        assertEquals(1, matches.size(), "private four-phase execution must exist exactly once"); return matches.get(0);
    }
    private static Waz.Skill skill(Waz waz, String japanese, String english) {
        return waz.getSkillList().stream().filter(s -> japanese.equals(s.getSkillNameJapanese()) && english.equals(s.getSkillNameEnglish())).reduce((a, b) -> { throw new AssertionError("duplicate identity"); }).orElseThrow();
    }
    private static List<SkillInfoObject> allEvents(Waz.Skill skill, int phase) {
        return skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream().flatMap(u -> u.getSkillInfoObjectList().stream()).toList();
    }
    private static <T extends SkillInfoObject> List<T> events(Waz.Skill skill, int phase, int slot, Class<T> type) {
        return skill.getPhasesInfo().get(phase).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity()))
                .flatMap(u -> u.getSkillInfoObjectList().stream()).filter(type::isInstance).map(type::cast).toList();
    }
    private static CEventChange change(List<CEventChange> changes, int start, int end) {
        var matches = changes.stream().filter(c -> c.getStartFrame() == start && c.getEndFrame() == end).toList();
        assertEquals(1, matches.size(), "expected transition frames " + start + ".." + end); return matches.get(0);
    }
    private static CEventValRandom amount(CEventEffect effect) {
        return effectValue(effect, 4);
    }
    private static CEventEffect sourceBombEffect(Waz waz) {
        return events(skill(waz, "飛行形態：集中爆撃", "0023"), 0, 48, CEventEffect.class).get(0);
    }
    private static CEventEffect.CEventEffectUnit effectUnit(CEventEffect effect, int slot) {
        var matches = effect.getCeventEffectUnitList().stream().filter(unit -> Integer.valueOf(slot).equals(unit.getUnitSlotNum())).toList();
        assertEquals(1, matches.size(), "one parent Effect unit " + slot);
        assertEquals(1, matches.get(0).getBuffer(), "enabled parent Effect unit " + slot);
        return matches.get(0);
    }
    private static CEventValRandom effectValue(CEventEffect effect, int slot) {
        return assertInstanceOf(CEventValRandom.class, effectUnit(effect, slot).getData());
    }
    private static List<Integer> values(CEventValRandom v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()); }
    private static void assertValues(List<CEventValRandom> events, List<Integer> expected) {
        assertFalse(events.isEmpty()); for (var event : events) assertEquals(expected, values(event));
    }
    private static void assertFrames(SkillInfoObject event, int start, int end) { assertEquals(start, event.getStartFrame()); assertEquals(end, event.getEndFrame()); }
    private static boolean active(SkillInfoObject event, int frame) { return event.getStartFrame() <= frame && frame <= event.getEndFrame(); }
    private static Mek.MekMaterialBlock.PluginEntry material(Mek mek, int row) { return mek.getMekMaterialBlock().getTrailingEntries().get(row); }
    private static void markMaterial(Mek mek, int row, int sprite, int se, int voice) {
        var entry = material(mek, row);
        entry.getSpriteGroups().set(0, new int[]{sprite});
        entry.getSeGroups().set(0, new int[]{se});
        entry.getVoiceGroups().set(0, new int[]{voice});
        mek.getMekMaterialBlock().getEntries().set(mek.getMekMaterialBlock().getRegularEntries().size() + row, entry);
    }
    private static JsonNode materialUnion(Mek mek, int... rows) {
        ObjectNode union = JSON.createObjectNode();
        for (String field : List.of("spriteGroups", "seGroups", "voiceGroups")) {
            List<LinkedHashSet<Integer>> groups = new ArrayList<>();
            for (int row : rows) {
                JsonNode source = JSON.valueToTree(material(mek, row));
                for (int i = 0; i < source.get(field).size(); i++) {
                    while (groups.size() <= i) groups.add(new LinkedHashSet<>());
                    for (JsonNode value : source.get(field).get(i)) groups.get(i).add(value.asInt());
                }
            }
            union.set(field, JSON.valueToTree(groups));
        }
        return union;
    }
    private static void assertMaterialViews(Mek mek) {
        var block = mek.getMekMaterialBlock();
        assertEquals(mek.getMekWeaponInfoMap().size(), block.getTrailingEntries().size());
        List<Mek.MekMaterialBlock.PluginEntry> expected = new ArrayList<>(block.getRegularEntries()); expected.addAll(block.getTrailingEntries());
        assertEquals(JSON.valueToTree(expected), JSON.valueToTree(block.getEntries()));
    }
    private static JsonNode payload(Object value) { JsonNode tree = JSON.valueToTree(value); removeMetadata(tree); return tree; }
    private static JsonNode eventPayloadWithoutWindow(SkillInfoObject value) {
        ObjectNode tree = (ObjectNode) payload(value); tree.remove(List.of("startFrame", "endFrame")); return tree;
    }
    private static JsonNode effectPayloadOutsideBombControls(CEventEffect effect) {
        ObjectNode tree = (ObjectNode) eventPayloadWithoutWindow(effect);
        var retained = JSON.createArrayNode();
        for (JsonNode unit : tree.get("ceventEffectUnitList"))
            if (!BOMB_CONTROL_UNITS.contains(unit.get("unitSlotNum").asInt())) retained.add(unit);
        tree.set("ceventEffectUnitList", retained);
        return tree;
    }
    private static void removeMetadata(JsonNode node) {
        if (node instanceof ObjectNode object) object.remove(List.of("offset", "length", "slotNum", "unitDescription", "description"));
        node.forEach(MotokiFlightBombVariantTest::removeMetadata);
    }
    private static String sha256(Path file) throws Exception { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(file))); }
    private record Fixture(Mek mek, Waz waz, Map<Path, String> hashes, Object sourceMek, Object sourceWaz, JsonNode sourceMekTree, JsonNode sourceWazTree) { }
    private record BombLaunch(int count, int spawnZ, int horizontalSpeed, int verticalSpeed, int gravity) { }
    private record Motion(int bombs, int playerCenterHeightAtRelease, int spawnZAtRelease, int targetZAtRelease,
                          int maxZ, int finalZ, int takeoffFrames, boolean cancelled,
                          boolean landed, boolean normalEnd, int preparationRetries, int finalGravity) { }
}
