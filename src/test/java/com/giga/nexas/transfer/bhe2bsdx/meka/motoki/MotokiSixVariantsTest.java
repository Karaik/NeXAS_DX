package com.giga.nexas.transfer.bhe2bsdx.meka.motoki;

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


/** Real BHE fixture, with static graph/serialization assertions; no claim of runtime child-hit or lifetime validation. */
class MotokiSixVariantsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CS = "windows-31j", ROLE = "motoki";
    // M1 has one equipable router and one PRIVATE execution; M2..M6 each add one skill.
    private static final int ADDED_SKILLS = 7, OLD_SKILLS = 2;
    private static final List<Variant> VARIANTS = List.of(
            new Variant("集中爆撃・帰投", "DX_FOCUSED_BOMB_RETURN", "0020", 40, 13, List.of(13, 9, 10)),
            new Variant("ワンツー・離脱", "DX_MOTOKI_ONE_TWO_EXIT", "0001", 18, 0, List.of(0)),
            new Variant("ドリル・確定突き", "DX_MOTOKI_DRILL_CONFIRM", "0003", 12, 1, List.of(1)),
            new Variant("排熱弾・即発", "DX_MOTOKI_QUICK_EXHAUST", "0005", 18, 2, List.of(2)),
            new Variant("ガンポッド・三連点射", "DX_MOTOKI_THREE_SHOT_GUNPOD", "0006", 12, 4, List.of(4)),
            new Variant("ビーム・短照射", "DX_MOTOKI_SHORT_BEAM", "0010", 24, 6, List.of(6)));
    @TempDir Path output;

    private static void customize(Fixture f) { MotokiCustomizer.INSTANCE.customizeBeforeWrite(f.context); }

    private static void assertBehavior(Fixture f, Mek mek, Waz waz) {
        Waz.Skill execution = skill(waz, "DX_FOCUSED_BOMB_EXECUTION");
        assertEquals(4, execution.getPhaseQuantity());
        assertTrue(mek.getMekWeaponInfoMap().values().stream().noneMatch(w -> w.getWazSequence() == waz.getSkillList().indexOf(execution)),
                "the existing private four-block bomb execution is not a seventh new equipment row");

        Waz.Skill punches = skill(waz, "DX_MOTOKI_ONE_TWO_EXIT");
        power(event(punches, 1, 41, CEventHit.class, 18, 22), 40); power(event(punches, 1, 41, CEventHit.class, 42, 46), 40);
        next(event(punches, 0, 71, CEventChange.class, 19, 19));
        for (int[] window : List.of(new int[]{18, 22}, new int[]{42, 46}))
            sameWithWindow(event(source(f, 0), 1, 41, CEventHit.class, window[0], window[1]),
                    event(punches, 1, 41, CEventHit.class, window[0], window[1]), window[0], window[1]);
        assertEquals(0, event(punches, 1, 41, CEventHit.class, 18, 22).getSelfStunFrame());
        assertEquals(1, event(punches, 1, 41, CEventHit.class, 42, 46).getSelfStunFrame(),
                "second hit retains int23 cancel without reinterpreting int22");
        assertEquals(1, events(punches, 1, 71, CEventChange.class).size(), "held input cannot repeat the55f loop");
        gate(punches, 1, 47, 1); sd(punches, 1, 48, 63); end(event(punches, 1, 71, CEventChange.class, 64, 64));
        armor(punches, 0, 0, 0); zeroCounters(punches); noCharge(punches); physical(punches, 0, 0);

        Waz.Skill drill = skill(waz, "DX_MOTOKI_DRILL_CONFIRM");
        power(event(drill, 1, 41, CEventHit.class, 0, 8), 20); power(event(drill, 3, 41, CEventHit.class, 0, 2), 20);
        input(event(drill, 0, 71, CEventChange.class, 12, 12), 1);
        sameWithWindow(event(source(f, 1), 1, 41, CEventHit.class, 0, 138),
                event(drill, 1, 41, CEventHit.class, 0, 8), 0, 8);
        assertEquals(1, event(drill, 1, 41, CEventHit.class, 0, 8).getHitCount());
        var sourceContactEffects = events(source(f, 1), 1, 48, CEventEffect.class);
        assertEquals(3, sourceContactEffects.size());
        assertTrue(events(drill, 1, 48, CEventEffect.class).stream().noneMatch(e -> sameReference(e, sourceContactEffects.get(0))),
                "zero-damage suction child must not confirm the actual20-power body hit");
        var drillConfirm = event(drill, 1, 71, CEventChange.class, 0, 8); hitCondition(drillConfirm); input(drillConfirm, 3);
        input(event(drill, 1, 71, CEventChange.class, 9, 9), 4);
        sameWithWindow(event(source(f, 1), 3, 41, CEventHit.class, 0, 2),
                event(drill, 3, 41, CEventHit.class, 0, 2), 0, 2);
        input(event(drill, 3, 71, CEventChange.class, 4, 4), 4);
        input(event(drill, 2, 71, CEventChange.class, 1, 1), 4);
        assertTrue(events(drill, 4, 41, CEventHit.class).isEmpty()); assertTrue(events(drill, 4, 48, CEventEffect.class).isEmpty());
        armor(drill, 4, 0, 0); physical(drill, 4, 0); gate(drill, 4, 0, 1);
        end(event(drill, 4, 71, CEventChange.class, 20, 20)); noCharge(drill);
        assertTrue(allEvents(drill, 4).stream().allMatch(e -> e.getEndFrame() <= 20), "pure short recovery has no old60/132f attack or wait");
        var visualSource = sourceContactEffects.subList(1, 3);
        var visualCopy = events(drill, 1, 48, CEventEffect.class);
        assertEquals(2, visualCopy.size());
        for (int i = 0; i < 2; i++) resizedEmitter(visualSource.get(i), visualCopy.get(i), 0, 8, 8);

        Waz.Skill exhaust = skill(waz, "DX_MOTOKI_QUICK_EXHAUST");
        power(event(exhaust, 3, 41, CEventHit.class, 1, 1), 20);
        input(event(exhaust, 0, 71, CEventChange.class, 10, 10), 3);
        sameWithWindow(event(source(f, 2), 3, 41, CEventHit.class, 1, 1),
                event(exhaust, 3, 41, CEventHit.class, 1, 1), 1, 1);
        assertEquals(tree(events(source(f, 2), 3, 48, CEventEffect.class)), tree(events(exhaust, 3, 48, CEventEffect.class)),
                "the native low20 body hit and both7:371 blast/visual pairs stay intact");
        input(event(exhaust, 3, 71, CEventChange.class, 10, 10), 7);
        for (int p : List.of(1, 2, 4, 5, 6)) {
            assertTrue(events(exhaust, p, 48, CEventEffect.class).isEmpty(), "unused entry may not retain suction/high-tier emissions");
            assertTrue(events(exhaust, p, 41, CEventHit.class).isEmpty(), "unused entry may not retain high-tier body Hits");
            input(event(exhaust, p, 71, CEventChange.class, 1, 1), 7);
        }
        physical(exhaust, 7, 0); armor(exhaust, 7, 0, 0); end(event(exhaust, 7, 71, CEventChange.class, 18, 18));
        noCharge(exhaust); zeroCounters(exhaust);

        Waz.Skill pod = skill(waz, "DX_MOTOKI_THREE_SHOT_GUNPOD");
        next(event(pod, 0, 71, CEventChange.class, 10, 10)); armor(pod, 0, 0, 0);
        var nativeBatches = events(source(f, 4), 1, 48, CEventEffect.class);
        var burst = events(pod, 1, 48, CEventEffect.class);
        assertEquals(12, burst.size(), "three complete native four-event muzzle batches");
        assertEquals(List.of(0, 2, 4), burst.stream().filter(e -> sameReference(e, nativeBatches.get(0))).map(CEventEffect::getStartFrame).sorted().toList());
        for (int frame : List.of(0, 2, 4)) {
            var batch = burst.stream().filter(e -> e.getStartFrame() == frame).toList(); assertEquals(4, batch.size());
            assertTrue(List.of(0, 2).stream().anyMatch(sourceFrame -> {
                var donor = nativeBatches.stream().filter(e -> e.getStartFrame() == sourceFrame).toList();
                if (donor.size() != batch.size()) return false;
                for (int i = 0; i < donor.size(); i++) {
                    ObjectNode expected = (ObjectNode) tree(donor.get(i)); expected.put("startFrame", frame); expected.put("endFrame", frame);
                    if (!expected.equals(tree(batch.get(i)))) return false;
                }
                return true;
            }), "each point-shot is a complete original muzzle batch with native bullet payload");
        }
        assertTrue(events(pod, 1, 41, CEventHit.class).isEmpty(), "delete the old zero-damage body Hit");
        assertEquals(1, events(pod, 1, 71, CEventChange.class).size()); next(event(pod, 1, 71, CEventChange.class, 5, 5));
        gate(pod, 2, 0, 1); end(event(pod, 2, 71, CEventChange.class, 16, 16)); noCharge(pod); zeroCounters(pod);

        assertBeam(f, mek, waz);
    }

    private static void assertBeam(Fixture f, Mek mek, Waz waz) {
        Waz.Skill source = source(f, 6), beam = skill(waz, "DX_MOTOKI_SHORT_BEAM");
        assertEquals("ビーム", source.getSkillNameJapanese()); assertEquals("0021", source.getSkillNameEnglish());
        assertEquals(1, beam.getPhaseQuantity());
        var nativeEffects = events(source, 0, 48, CEventEffect.class);
        assertEquals(2, nativeEffects.size());
        assertEquals(List.of(List.of(36, 65), List.of(108, 137)), nativeEffects.stream().map(e -> List.of(e.getStartFrame(), e.getEndFrame())).toList());
        var effects = events(beam, 0, 48, CEventEffect.class); assertEquals(1, effects.size());
        sameWithWindow(nativeEffects.get(0), effects.get(0), 18, 47);
        assertTrue(sameReference(effects.get(0), nativeEffects.get(0)), "retain the real donor's rebound beam reference rather than moving its payload into the main WAZ");
        for (var u : effects.get(0).getCeventEffectUnitList()) if (u.getBuffer() != 0) {
            assertEquals(0, u.getData().getStartFrame()); assertEquals(29, u.getData().getEndFrame());
        }
        assertEquals(List.of(0, 1, 0, 0, 1, 0), values(assertInstanceOf(CEventValRandom.class, effectData(effects.get(0), 4))));
        assertEquals(List.of(0, 5, 0, 0, 5, 0), values(assertInstanceOf(CEventValRandom.class, effectData(effects.get(0), 6))));
        assertEquals(24, assertInstanceOf(CEventValRandom.class, effectData(effects.get(0), 31)).getInt5());
        assertEquals(List.of(0, 3), assertInstanceOf(CEventTerm.class, effectData(effects.get(0), 7)).getBsdxInfoCollectionList().get(0).getIntList3());

        var nativeSprites = events(source, 0, 0, CEventSprite.class);
        var sprites = events(beam, 0, 0, CEventSprite.class);
        assertEquals(5, sprites.size());
        int[][] windows = {{0, 5}, {6, 17}, {18, 47}, {48, 55}, {56, 77}};
        int[] poses = {0, 1, 2, 3, 7};
        for (int i = 0; i < 5; i++) {
            sameWithWindow(nativeSprites.get(poses[i]), sprites.get(i), windows[i][0], windows[i][1]);
            assertEquals(-1, sprites.get(i).getSpmFileSequence(), "keep the native Motoki auto sprite context");
        }
        assertEquals(1, events(beam, 0, 6, CEventSpriteYure.class).size());
        sameWithWindow(events(source, 0, 6, CEventSpriteYure.class).get(0), event(beam, 0, 6, CEventSpriteYure.class, 6, 17), 6, 17);
        sameWithWindow(event(source, 0, 10, CEventValRandom.class, 0, 35), event(beam, 0, 10, CEventValRandom.class, 0, 17), 0, 17);
        assertEquals(List.of(0, 0, 0, 0, 0, 0), values(event(beam, 0, 10, CEventValRandom.class, 48, 48)));
        assertEquals(List.of(0, 0, 0, 0), values(event(beam, 0, 24, CEventVal.class, 0, 0)));
        assertEquals(List.of(0, 100, 0, 100), values(event(beam, 0, 24, CEventVal.class, 48, 48)));
        assertTrue(events(beam, 0, 18, CEventValRandom.class).isEmpty(), "source has no active speed impulse: do not invent a dash");
        assertEquals(1, events(beam, 0, 45, CEventEscape.class).size());
        sameWithWindow(event(source, 0, 45, CEventEscape.class, 0, 36), event(beam, 0, 45, CEventEscape.class, 0, 18), 0, 18);
        gate(beam, 0, 48, 1); sd(beam, 0, 49, 76);
        assertEquals(2, events(beam, 0, 52, CEventSe.class).size());
        sameWithWindow(event(source, 0, 52, CEventSe.class, 0, 0), event(beam, 0, 52, CEventSe.class, 0, 0), 0, 0);
        sameWithWindow(event(source, 0, 52, CEventSe.class, 26, 26), event(beam, 0, 52, CEventSe.class, 8, 8), 8, 8);
        assertEquals(1, events(beam, 0, 53, CEventVoice.class).size());
        sameWithWindow(event(source, 0, 53, CEventVoice.class, 30, 30), event(beam, 0, 53, CEventVoice.class, 12, 12), 12, 12);
        assertEquals(List.of(0, 0, 0, 0), values(event(beam, 0, 70, CEventVal.class, 0, 0)));
        assertEquals(100, event(beam, 0, 70, CEventVal.class, 18, 18).getInt4());
        assertEquals(1, events(beam, 0, 71, CEventChange.class).size()); end(event(beam, 0, 71, CEventChange.class, 77, 77));
        armor(beam, 0, 0, 0); armor(beam, 0, 48, 0); physical(beam, 0, 0); physical(beam, 0, 48);
        for (JsonNode frame : tree(beam).findValues("endFrame")) assertTrue(frame.asInt() <= 77, "no second-wave or obsolete known/unknown event survives after77");
        assertTrue(waz.getSkillList().stream().noneMatch(s -> s.getSkillNameEnglish() != null && s.getSkillNameEnglish().startsWith("DX_MOTOKI_PULL")),
                "the withdrawn464-copy and private pull helper are not part of this design");
    }

    private static void resizedEmitter(CEventEffect donor, CEventEffect actual, int start, int end, int innerEnd) {
        ObjectNode expected = (ObjectNode) tree(donor); expected.put("startFrame", start); expected.put("endFrame", end);
        for (JsonNode unit : expected.get("ceventEffectUnitList")) if (unit.get("buffer").asInt() != 0) {
            ObjectNode data = (ObjectNode) unit.get("data"); data.put("startFrame", 0); data.put("endFrame", innerEnd);
        }
        assertEquals(expected, tree(actual));
    }

    @Test
    void sixOrdinaryEquipmentRowsHaveUniqueFinalWazIdentitiesAndPreserveEveryOriginal() throws Exception {
        Fixture f = fixture();
        customize(f);
        assertEquals(f.weaponCount + 6, f.mek.getMekWeaponInfoMap().size(),
                "six independent new ordinary MEK rows; phases and private execution helpers do not count");
        assertEquals(f.skillCount + ADDED_SKILLS, f.waz.getSkillList().size());
        assertSix(f.mek, f.waz);
        assertOriginals(f);
        assertMaterials(f);
        assertSources(f);
    }

    @Test
    void materialIsIndependentIdempotentAndGeneratedCp932BinaryRegeneratesExactly() throws Exception {
        Fixture f = fixture(); customize(f); assertSix(f.mek, f.waz);
        JsonNode onceMek = tree(f.mek), onceWaz = tree(f.waz);
        customize(f);
        assertEquals(onceMek, tree(f.mek)); assertEquals(onceWaz, tree(f.waz));
        assertMaterials(f);
        var service = new BsdxBinService();
        Path m1 = output.resolve("first.mek"), w1 = output.resolve("first.waz");
        Path m2 = output.resolve("second.mek"), w2 = output.resolve("second.waz");
        service.generate(m1.toString(), f.mek, CS); service.generate(w1.toString(), f.waz, CS);
        Mek mek = (Mek) service.parse(m1.toString(), CS).getData();
        Waz waz = (Waz) service.parse(w1.toString(), CS).getData();
        assertSix(mek, waz); assertBehavior(f, mek, waz);
        service.generate(m2.toString(), mek, CS); service.generate(w2.toString(), waz, CS);
        assertArrayEquals(Files.readAllBytes(m1), Files.readAllBytes(m2));
        assertArrayEquals(Files.readAllBytes(w1), Files.readAllBytes(w2));
        for (Variant v : VARIANTS) {
            int target = row(f.mek, v.name);
            var copy = material(f.mek, target);
            for (int donor : v.donors) {
                var original = material(f.mek, donor);
                independent(copy.getSpriteGroups(), original.getSpriteGroups());
                independent(copy.getSeGroups(), original.getSeGroups());
                independent(copy.getVoiceGroups(), original.getVoiceGroups());
            }
        }
        var mutableGroups = material(f.mek, row(f.mek, VARIANTS.get(5).name)).getSpriteGroups().stream().filter(g -> g.length > 0).toList();
        assertFalse(mutableGroups.isEmpty()); mutableGroups.get(0)[0] = 999999;
        assertEquals(f.materials, tree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, f.weaponCount)));
        assertSources(f);
    }

    @Test
    void newVariantsHaveLiteralTimingConditionalFollowupsAndFiniteRecovery() throws Exception {
        Fixture f = fixture(); customize(f); assertSix(f.mek, f.waz);
        assertBehavior(f, f.mek, f.waz);
        for (Variant v : VARIANTS.subList(1, 6)) {
            Waz.Skill s = skill(f.waz, v.english);
            assertEquals(tree(source(f, v.sourceRow).getSkillSuffixList()), tree(s.getSkillSuffixList()),
                    "preserve all real suffix entry points");
            finiteLocalRoutes(s);
        }
        assertOriginals(f); assertSources(f);
    }

    @Test
    void eachNewDonorDriftRejectsWithoutLeakingAnyPartOfTheNewBatch() throws Exception {
        for (Variant v : VARIANTS.subList(1, 6)) {
            Fixture f = fixture(); primeExistingOnly(f);
            Waz.Skill donor = source(f, v.sourceRow);
            donor.setSkillNameEnglish(donor.getSkillNameEnglish() + "_DRIFT");
            JsonNode mek = tree(f.mek), waz = tree(f.waz);
            assertThrows(IllegalStateException.class, () -> customize(f), "new donor must be validated: " + v.english);
            assertEquals(mek, tree(f.mek), "the already completed old customizer baseline remains intact");
            assertEquals(waz, tree(f.waz), "no earlier new variant may leak before a later donor rejects");
        }
    }

    @Test
    void existingNewVariantPayloadDriftIsRejectedWithoutWrites() throws Exception {
        Fixture f = fixture(); customize(f); assertSix(f.mek, f.waz);
        Waz.Skill s = skill(f.waz, VARIANTS.get(5).english);
        SkillInfoObject event = allEvents(s, 0).get(0); event.setEndFrame(event.getEndFrame() + 1);
        JsonNode mek = tree(f.mek), waz = tree(f.waz);
        assertThrows(IllegalStateException.class, () -> customize(f));
        assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz));
    }

    @Test
    void changedNewBeamNativeSpeedRejectsBeforeAnyNewAppend() throws Exception {
        Fixture f = fixture(); primeExistingOnly(f);
        CEventEffect emitter = events(source(f, 6), 0, 48, CEventEffect.class).get(0);
        assertInstanceOf(CEventValRandom.class, effectData(emitter, 31)).setInt5(25);
        JsonNode mek = tree(f.mek), waz = tree(f.waz);
        assertThrows(IllegalStateException.class, () -> customize(f));
        assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz));
    }

    private static Fixture fixture() throws Exception {
        Path mekPath = Path.of("src/main/resources/game/bhe/mek/" + ROLE + "/" + ROLE + ".mek");
        Path wazPath = Path.of("src/main/resources/game/bhe/waz/" + ROLE + "/" + ROLE + ".waz");
        BheBinService service = new BheBinService();
        var rawMek = (com.giga.nexas.dto.bhe.mek.Mek) service.parse(mekPath.toString(), CS).getData();
        var rawWaz = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(wazPath.toString(), CS).getData();
        List<Object> raw = List.of(rawMek, rawWaz);
        List<JsonNode> rawTrees = raw.stream().map(v -> (JsonNode) JSON.valueToTree(v)).toList();
        Map<Path, byte[]> files = new LinkedHashMap<>();
        for (Path p : List.of(mekPath, wazPath)) files.put(p, Files.readAllBytes(p));
        for (String name : List.of("bomb.waz", "effect.waz", "laser.waz", "tama01.waz", "tama02.waz", "tama03.waz", "tama04.waz", "tama05.waz")) {
            Path p = Path.of("src/main/resources/game/bhe/waz", name); files.put(p, Files.readAllBytes(p));
        }
        Mek mek = new BheToBsdxMekConverter().convert(rawMek);
        Waz waz = new BheToBsdxWazConverter().convert(rawWaz);
        new BheInfoCollectionObjectGraphRewriter().rewrite(waz, ROLE + "SixVariantsTest");
        int sourceGroup = mek.getMekBasicInfo().getWazFileSequence();
        mek.getMekBasicInfo().setWazFileSequence(233);
        waz.getSkillList().add(0, new Waz.Skill());
        for (var weapon : mek.getMekWeaponInfoMap().values()) weapon.setWazSequence(weapon.getWazSequence() + 1);
        for (var s : waz.getSkillList()) for (int p = 0; p < s.getPhasesInfo().size(); p++)
            for (var c : events(s, p, 71, CEventChange.class)) for (var d : c.getBsdxInfoCollectionList2())
                if (Integer.valueOf(16).equals(d.getInt1()) && List.of(1).equals(d.getTypeList())
                        && d.getParamList().size() == 2 && Integer.valueOf(sourceGroup).equals(d.getParamList().get(0)))
                    d.setParamList(new ArrayList<>(List.of(233, d.getParamList().get(1) + 1)));
        FollowupMekaContext context = new FollowupMekaContext(); context.setReboundMek(mek); context.setReboundWaz(waz);
        return new Fixture(mek, waz, context, mek.getMekWeaponInfoMap().size(), waz.getSkillList().size(),
                tree(mek.getMekWeaponInfoMap()), tree(waz.getSkillList()), tree(mek.getMekMaterialBlock().getTrailingEntries()),
                tree(mek.getMekAiInfoList()), raw, rawTrees, files);
    }

    private static void primeExistingOnly(Fixture f) {
        customize(f);
        Map<Integer, Mek.MekWeaponInfo> existingWeapons = new LinkedHashMap<>();
        for (Map.Entry<Integer, Mek.MekWeaponInfo> entry : f.mek.getMekWeaponInfoMap().entrySet())
            if (entry.getKey() < f.weaponCount + 1) existingWeapons.put(entry.getKey(), entry.getValue());
        f.mek.setMekWeaponInfoMap(existingWeapons);
        f.waz.setSkillList(new ArrayList<>(f.waz.getSkillList().subList(0, f.skillCount + OLD_SKILLS)));
        var b = f.mek.getMekMaterialBlock();
        b.setTrailingEntries(new ArrayList<>(b.getTrailingEntries().subList(0, f.weaponCount + 1)));
        var entries = new ArrayList<>(b.getRegularEntries()); entries.addAll(b.getTrailingEntries()); b.setEntries(entries);
    }

    private static void assertSix(Mek mek, Waz waz) throws Exception {
        assertEquals(6, mek.getMekWeaponInfoMap().values().stream().filter(w -> Integer.valueOf(0).equals(w.getWeaponCategory())
                && w.getWazSequence() >= 0 && w.getWazSequence() < waz.getSkillList().size()
                && waz.getSkillList().get(w.getWazSequence()).getSkillNameEnglish() != null
                && waz.getSkillList().get(w.getWazSequence()).getSkillNameEnglish().startsWith("DX_")).count());
        Set<Integer> targets = new HashSet<>();
        for (Variant v : VARIANTS) {
            int r = row(mek, v.name); assertTrue(r <= 152);
            var w = mek.getMekWeaponInfoMap().get(r); var s = skill(waz, v.english);
            assertEquals(v.name, s.getSkillNameJapanese()); assertEquals(v.sequence, w.getWeaponSequence());
            assertEquals(0, w.getWeaponCategory()); assertEquals(waz.getSkillList().indexOf(s), w.getWazSequence());
            assertTrue(w.getWazSequence() <= 152); assertTrue(targets.add(w.getWazSequence()));
            assertEquals(v.heat, w.getHeatMinConsumption()); assertEquals(v.heat, w.getHeatMaxConsumption());
            assertEquals(0, w.getUpgradeExp()); assertFalse(w.getWeaponDescription().isBlank());
            cp932(v.name); cp932(w.getWeaponDescription());
        }
        assertEquals(mek.getMekWeaponInfoMap().size(), mek.getMekMaterialBlock().getTrailingEntries().size());
    }
    private static void assertOriginals(Fixture f) {
        for (int i = 0; i < f.weaponCount; i++) assertEquals(f.weapons.get("" + i), tree(f.mek.getMekWeaponInfoMap().get(i)));
        assertEquals(f.skills, tree(f.waz.getSkillList().subList(0, f.skillCount)));
        assertEquals(f.materials, tree(f.mek.getMekMaterialBlock().getTrailingEntries().subList(0, f.weaponCount)));
        assertEquals(f.ai, tree(f.mek.getMekAiInfoList()));
    }
    private static void assertSources(Fixture f) throws Exception {
        for (int i = 0; i < f.raw.size(); i++) assertEquals(f.rawTrees.get(i), JSON.valueToTree(f.raw.get(i)));
        for (var e : f.files.entrySet()) assertArrayEquals(e.getValue(), Files.readAllBytes(e.getKey()), e.getKey().toString());
    }
    private static void assertMaterials(Fixture f) {
        var b = f.mek.getMekMaterialBlock(); var combined = new ArrayList<>(b.getRegularEntries()); combined.addAll(b.getTrailingEntries());
        assertEquals(tree(combined), tree(b.getEntries()));
        for (Variant v : VARIANTS) {
            var actual = material(f.mek, row(f.mek, v.name));
            ObjectNode expected = JSON.createObjectNode();
            for (String field : List.of("spriteGroups", "seGroups", "voiceGroups")) {
                List<List<Integer>> groups = new ArrayList<>();
                for (int donorIndex = 0; donorIndex < v.donors.size(); donorIndex++) {
                    int donor = v.donors.get(donorIndex);
                    JsonNode source = tree(material(f.mek, donor)).get(field);
                    for (int i = 0; i < source.size(); i++) {
                        while (groups.size() <= i) groups.add(new ArrayList<>());
                        for (JsonNode value : source.get(i))
                            if (donorIndex == 0 || !groups.get(i).contains(value.asInt())) groups.get(i).add(value.asInt());
                    }
                }
                // The previously accepted bomb helper explicitly applies distinctGroups after the ordinary merge.
                if ("DX_FOCUSED_BOMB_RETURN".equals(v.english))
                    groups.replaceAll(g -> new ArrayList<>(new LinkedHashSet<>(g)));
                expected.set(field, JSON.valueToTree(groups));
            }
            assertEquals(expected, tree(actual), v.english + " exact per-weapon material union");
        }
    }
    private static void independent(List<int[]> copy, List<int[]> donor) {
        assertNotSame(copy, donor);
        for (int[] a : copy) for (int[] b : donor) assertNotSame(a, b);
    }
    private static void finiteLocalRoutes(Waz.Skill s) {
        for (int p = 0; p < s.getPhasesInfo().size(); p++) {
            var changes = events(s, p, 71, CEventChange.class);
            assertTrue(changes.stream().anyMatch(c -> c.getBsdxInfoCollectionList1().size() == 1
                    && List.of(0).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())), s.getSkillNameEnglish() + " P" + p + " finite unconditional fallback");
            for (var c : changes) for (var d : c.getBsdxInfoCollectionList2()) if (List.of(0, 1).equals(d.getTypeList())) {
                int entry = d.getParamList().get(0);
                assertTrue(entry >= 0 && entry < s.getSkillSuffixList().size());
                assertTrue(s.getSkillSuffixList().get(entry).getInt1() > p, "no backwards INPUT/re-entry loop in new finite variant");
            }
        }
    }
    private static Waz.Skill source(Fixture f, int row) { return f.waz.getSkillList().get(f.mek.getMekWeaponInfoMap().get(row).getWazSequence()); }
    private static Mek.MekMaterialBlock.PluginEntry material(Mek mek, int row) { return mek.getMekMaterialBlock().getTrailingEntries().get(row); }
    private static int row(Mek mek, String name) { var a = mek.getMekWeaponInfoMap().entrySet().stream().filter(e -> name.equals(e.getValue().getWeaponName())).toList(); assertEquals(1, a.size(), "one equipment identity " + name); return a.get(0).getKey(); }
    private static Waz.Skill skill(Waz waz, String english) { var a = waz.getSkillList().stream().filter(Objects::nonNull).filter(s -> english.equals(s.getSkillNameEnglish())).toList(); assertEquals(1, a.size(), "one WAZ identity " + english); return a.get(0); }
    private static List<SkillInfoObject> allEvents(Waz.Skill s, int p) { return s.getPhasesInfo().get(p).getSkillUnitCollection().stream().flatMap(u -> u.getSkillInfoObjectList().stream()).toList(); }
    private static <T extends SkillInfoObject> List<T> events(Waz.Skill s, int p, int slot, Class<T> type) { return s.getPhasesInfo().get(p).getSkillUnitCollection().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitQuantity())).flatMap(u -> u.getSkillInfoObjectList().stream()).filter(type::isInstance).map(type::cast).toList(); }
    private static <T extends SkillInfoObject> T event(Waz.Skill s, int p, int slot, Class<T> type, int start, int end) { var a = events(s, p, slot, type).stream().filter(e -> Integer.valueOf(start).equals(e.getStartFrame()) && Integer.valueOf(end).equals(e.getEndFrame())).toList(); assertEquals(1, a.size(), s.getSkillNameEnglish() + " P" + p + " slot" + slot + " " + start + ".." + end); return a.get(0); }
    private static SkillInfoObject effectData(CEventEffect e, int slot) { var a = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(slot).equals(u.getUnitSlotNum()) && u.getBuffer() != 0).toList(); assertEquals(1, a.size()); return a.get(0).getData(); }
    private static boolean refers(CEventEffect e, int group, int sequence) { var a = e.getCeventEffectUnitList().stream().filter(u -> Integer.valueOf(3).equals(u.getUnitSlotNum()) && u.getBuffer() != 0).toList(); return a.size() == 1 && a.get(0).getData() instanceof CEventWazaSelect v && Integer.valueOf(group).equals(v.getWazFileNo()) && Integer.valueOf(sequence).equals(v.getWazSequenceNo()); }
    private static boolean sameReference(CEventEffect actual, CEventEffect donor) {
        var reference = assertInstanceOf(CEventWazaSelect.class, effectData(donor, 3));
        return refers(actual, reference.getWazFileNo(), reference.getWazSequenceNo());
    }
    private static void sameWithWindow(SkillInfoObject source, SkillInfoObject actual, int start, int end) { ObjectNode expected = (ObjectNode) tree(source); expected.put("startFrame", start); expected.put("endFrame", end); assertEquals(expected, tree(actual), "only outer time changes; the native nested payload remains intact"); }
    private static void next(CEventChange c) { destination(c, List.of(0, 0), List.of()); }
    private static void end(CEventChange c) { assertTrue(List.of(List.of(0, 0), List.of(0, 2)).contains(c.getBsdxInfoCollectionList2().get(0).getTypeList())); assertEquals(List.of(0), c.getBsdxInfoCollectionList1().get(0).getTypeList()); }
    private static void input(CEventChange c, int entry) { destination(c, List.of(0, 1), List.of(entry)); }
    private static void jump(CEventChange c, Mek mek, Waz waz, Waz.Skill target) { destination(c, List.of(1), List.of(mek.getMekBasicInfo().getWazFileSequence(), waz.getSkillList().indexOf(target))); }
    private static void destination(CEventChange c, List<Integer> types, List<Integer> params) { assertEquals(16, c.getBsdxInfoCollectionList2().get(0).getInt1()); assertEquals(types, c.getBsdxInfoCollectionList2().get(0).getTypeList()); assertEquals(params, c.getBsdxInfoCollectionList2().get(0).getParamList()); }
    private static void unconditional(CEventChange c) { assertEquals(1, c.getFlag()); assertEquals(List.of(0), c.getBsdxInfoCollectionList1().get(0).getTypeList()); }
    private static void hitCondition(CEventChange c) { assertEquals(1, c.getFlag()); assertEquals(0, c.getBsdxInfoCollectionList1().get(0).getInt1()); assertEquals(List.of(2, 6, 0, 0), c.getBsdxInfoCollectionList1().get(0).getTypeList()); assertEquals(List.of(0), c.getBsdxInfoCollectionList1().get(0).getParamList()); assertEquals(List.of(0, 0), c.getBsdxInfoCollectionList1().get(0).getIntList3()); }
    private static List<Integer> values(CEventVal v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4()); }
    private static List<Integer> values(CEventValRandom v) { return List.of(v.getInt1(), v.getInt2(), v.getInt3(), v.getInt4(), v.getInt5(), v.getInt6()); }
    private static void gate(Waz.Skill s, int p, int frame, int value) { var v = event(s, p, 47, CEventVal.class, frame, frame); assertEquals(0, v.getInt1()); assertEquals(0, v.getInt3()); assertEquals(value, v.getInt4()); }
    private static void sd(Waz.Skill s, int p, int from, int to) { assertEquals(List.of(0, 3, 0, 3), values(event(s, p, 47, CEventVal.class, from, to))); }
    private static void armor(Waz.Skill s, int p, int frame, int value) { assertEquals(List.of(0, 0, 0, value), values(event(s, p, 36, CEventVal.class, frame, frame))); }
    private static void physical(Waz.Skill s, int p, int frame) {
        var g = events(s, p, 21, CEventValRandom.class).stream().filter(v -> v.getStartFrame() <= frame && frame <= v.getEndFrame()).toList();
        assertFalse(g.isEmpty(), "explicit G50 before the new attack/cancel");
        for (var v : g) assertEquals(List.of(0, 50, 0, 0, 50, 0), values(v), "constant G50 rather than a 0-to-50 ramp");
        for (int slot : List.of(27, 28)) {
            var a = events(s, p, slot, CEventVal.class).stream().filter(v -> v.getStartFrame() <= frame && frame <= v.getEndFrame()).toList();
            assertFalse(a.isEmpty(), "explicit gravity/jump multiplier100"); for (var v : a) assertEquals(List.of(0, 100, 0, 100), values(v));
        }
    }
    private static void noCharge(Waz.Skill s) { for (int p = 0; p < s.getPhasesInfo().size(); p++) assertTrue(allEvents(s, p).stream().noneMatch(CEventCharge.class::isInstance), "new finite variant must not spend per-loop HC or retain Charge"); }
    private static void zeroCounters(Waz.Skill s) { for (int p = 0; p < s.getPhasesInfo().size(); p++) for (var v : events(s, p, 35, CEventVal.class)) assertEquals(List.of(0, 0, 0, 0), values(v)); }
    private static void power(CEventHit hit, int expected) {
        var units = hit.getCeventHitUnitList().stream().filter(u -> Integer.valueOf(6).equals(u.getUnitSlotNum()) && u.getBuffer() != 0).toList();
        assertEquals(1, units.size()); var term = assertInstanceOf(CEventTerm.class, units.get(0).getData());
        assertEquals(1, term.getBsdxInfoCollectionList().size()); var value = term.getBsdxInfoCollectionList().get(0);
        assertEquals(23, value.getInt1()); assertEquals(List.of(0), value.getTypeList()); assertEquals(List.of(expected), value.getParamList());
    }
    private static void cp932(String value) throws Exception { Charset cs = Charset.forName(CS); assertEquals(value, cs.decode(cs.newEncoder().encode(CharBuffer.wrap(value))).toString()); }
    private static JsonNode tree(Object value) { JsonNode n = JSON.valueToTree(value); clean(n); return n; }
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(MotokiSixVariantsTest::clean); }
    private record Variant(String name, String english, String sequence, int heat, int sourceRow, List<Integer> donors) { }
    private record Fixture(Mek mek, Waz waz, FollowupMekaContext context, int weaponCount, int skillCount,
                           JsonNode weapons, JsonNode skills, JsonNode materials, JsonNode ai,
                           List<Object> raw, List<JsonNode> rawTrees, Map<Path, byte[]> files) { }

}
