package com.giga.nexas.transfer.bhe2bsdx.meka.nagi;

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


/** Static real-source contract only; these assertions do not claim runtime combo or object-lifetime validation. */
class NagiSixVariantsTest {
    private static final ObjectMapper JSON = new ObjectMapper();
    private static final String CS = "windows-31j", ROLE = "nagi";
    private static final int ADDED_SKILLS = 6, OLD_SKILLS = 1;
    private static final List<Variant> VARIANTS = List.of(
            new Variant("瞬間ディフレクト", "DX_QUICK_DEFLECT", "0018", 12, 12, List.of(12)),
            new Variant("斬上げ・接続", "DX_NAGI_CLOSE_LAUNCH", "0028", 14, 0, List.of(0)),
            new Variant("乱れ突き・三連", "DX_NAGI_THREE_THRUST", "0020", 12, 1, List.of(1, 7)),
            new Variant("転移突き・追撃", "DX_NAGI_TRANSFER_LINK", "0035", 18, 2, List.of(2)),
            new Variant("盾打ち・反攻", "DX_NAGI_BASH_CONFIRM", "0004", 28, 8, List.of(8, 0, 7)),
            new Variant("単発リフレクト・解放", "DX_NAGI_SINGLE_REFLECT", "0036", 16, 13, List.of(13, 7)));
    @TempDir Path output;

    private static void customize(Fixture f) { NagiCustomizer.INSTANCE.customizeBeforeWrite(f.context); }

    private static void assertBehavior(Fixture f, Mek mek, Waz waz) {
        Waz.Skill quick = skill(waz, "DX_QUICK_DEFLECT");
        power(event(quick, 1, 41, CEventHit.class, 19, 35), 150);
        armor(quick, 1, 0, 0);
        assertEquals(List.of(2, 6, 6), event(quick, 0, 71, CEventChange.class, 5, 19).getBsdxInfoCollectionList1().get(0).getTypeList());
        armor(quick, 0, 20, 0); end(event(quick, 0, 71, CEventChange.class, 39, 39));
        sameWithWindow(event(source(f, 12), 1, 41, CEventHit.class, 19, 35),
                event(quick, 1, 41, CEventHit.class, 19, 35), 19, 35);

        Waz.Skill launch = skill(waz, "DX_NAGI_CLOSE_LAUNCH");
        power(event(launch, 1, 41, CEventHit.class, 7, 21), 60);
        CEventChange close = event(launch, 0, 71, CEventChange.class, 0, 23);
        assertEquals(List.of(2, 3, 6, 2), close.getBsdxInfoCollectionList1().get(0).getTypeList());
        assertEquals(List.of(135), close.getBsdxInfoCollectionList1().get(0).getParamList()); next(close);
        assertEquals(0, event(launch, 0, 18, CEventValRandom.class, 23, 23).getInt5());
        end(event(launch, 0, 71, CEventChange.class, 24, 24));
        assertEquals(2, events(launch, 0, 71, CEventChange.class).size(), "no old unconditional far-range NEXT at23");
        sameWithWindow(event(source(f, 0), 1, 41, CEventHit.class, 7, 21),
                event(launch, 1, 41, CEventHit.class, 7, 21), 7, 21);
        assertEquals(1, event(launch, 1, 41, CEventHit.class, 7, 21).getSelfStunFrame(), "int23 is the native hit-cancel flag");
        physical(launch, 0, 0); physical(launch, 1, 0); armor(launch, 0, 0, 0);
        gate(launch, 1, 22, 1); sd(launch, 1, 23, 31); end(event(launch, 1, 71, CEventChange.class, 32, 32));

        Waz.Skill thrust = skill(waz, "DX_NAGI_THREE_THRUST");
        for (int p : List.of(0, 1)) { var c = event(thrust, p, 71, CEventChange.class, 5, 5); unconditional(c); input(c, 2); }
        var originalThrust = events(source(f, 1), 2, 48, CEventEffect.class);
        var three = events(thrust, 2, 48, CEventEffect.class);
        assertEquals(3, three.size()); assertEquals(List.of(0, 3, 6), three.stream().map(CEventEffect::getStartFrame).toList());
        for (int i = 0; i < 3; i++) sameWithWindow(originalThrust.get(i), three.get(i), i * 3, i * 3);
        assertTrue(events(thrust, 2, 41, CEventHit.class).isEmpty(), "three child emitters are not invented body Hits");
        assertEquals(1, events(thrust, 2, 71, CEventChange.class).size()); next(event(thrust, 2, 71, CEventChange.class, 8, 8));
        gate(thrust, 3, 0, 1); armor(thrust, 3, 0, 0); physical(thrust, 3, 0);
        jump(event(thrust, 3, 71, CEventChange.class, 18, 18), mek, waz, waz.getSkillList().get(mek.getMekWeaponInfoMap().get(7).getWazSequence()));
        noCharge(thrust); zeroCounters(thrust);

        Waz.Skill transfer = skill(waz, "DX_NAGI_TRANSFER_LINK");
        var originalTransfer = events(source(f, 2), 0, 48, CEventEffect.class);
        var copiedTransfer = events(transfer, 0, 48, CEventEffect.class);
        assertEquals(2, copiedTransfer.size());
        for (int i = 0; i < 2; i++) sameWithWindow(originalTransfer.get(i), copiedTransfer.get(i), 14, 14);
        assertTrue(events(transfer, 0, 41, CEventHit.class).isEmpty(), "probe and child carry the actual hit semantics");
        gate(transfer, 0, 32, 1); sd(transfer, 0, 33, 38);
        next(event(transfer, 0, 71, CEventChange.class, 39, 39)); end(event(transfer, 1, 71, CEventChange.class, 16, 16));

        Waz.Skill bash = skill(waz, "DX_NAGI_BASH_CONFIRM");
        power(event(bash, 3, 41, CEventHit.class, 4, 7), 200);
        var ground = events(bash, 0, 71, CEventChange.class);
        assertEquals(2, ground.size());
        var grounded = ground.stream().filter(c -> List.of(2, 7, 16).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())).findFirst();
        assertTrue(grounded.isPresent()); next(grounded.orElseThrow());
        input(ground.stream().filter(c -> List.of(0).equals(c.getBsdxInfoCollectionList1().get(0).getTypeList())).findFirst().orElseThrow(), 2);
        event(bash, 1, 18, CEventValRandom.class, 5, 12);
        assertEquals(0, event(bash, 1, 18, CEventValRandom.class, 13, 13).getInt5());
        input(event(bash, 1, 71, CEventChange.class, 14, 14), 3);
        var land = event(bash, 2, 71, CEventChange.class, 8, 90);
        assertEquals(List.of(2, 7, 17), land.getBsdxInfoCollectionList1().get(0).getTypeList()); next(land);
        jump(event(bash, 2, 71, CEventChange.class, 91, 91), mek, waz, waz.getSkillList().get(mek.getMekWeaponInfoMap().get(7).getWazSequence()));
        sameWithWindow(event(source(f, 8), 3, 41, CEventHit.class, 4, 7), event(bash, 3, 41, CEventHit.class, 4, 7), 4, 7);
        var confirm = event(bash, 3, 71, CEventChange.class, 8, 9); hitCondition(confirm); jump(confirm, mek, waz, launch);
        gate(bash, 3, 10, 1); jump(event(bash, 3, 71, CEventChange.class, 24, 24), mek, waz, waz.getSkillList().get(mek.getMekWeaponInfoMap().get(7).getWazSequence()));
        physical(bash, 2, 0); armor(bash, 0, 0, 0); noCharge(bash);

        Waz.Skill reflect = skill(waz, "DX_NAGI_SINGLE_REFLECT");
        assertEquals(List.of(2, 6, 6), event(reflect, 0, 71, CEventChange.class, 5, 17).getBsdxInfoCollectionList1().get(0).getTypeList());
        armor(reflect, 0, 5, 6); armor(reflect, 0, 18, 0); gate(reflect, 0, 19, 1);
        Waz.Skill shieldEnd = waz.getSkillList().get(mek.getMekWeaponInfoMap().get(7).getWazSequence());
        jump(event(reflect, 0, 71, CEventChange.class, 32, 32), mek, waz, shieldEnd);
        assertEquals(tree(events(source(f, 13), 1, 48, CEventEffect.class)), tree(events(reflect, 1, 48, CEventEffect.class)),
                "the captured-object reflection emitter and link stay native");
        assertEquals(1, events(reflect, 2, 71, CEventChange.class).size(), "remove the old armor-conditioned re-entry");
        armor(reflect, 2, 14, 0); gate(reflect, 2, 15, 1);
        jump(event(reflect, 2, 71, CEventChange.class, 30, 30), mek, waz, shieldEnd);
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
    void changedNewTransferEmitterRejectsBeforeAnyNewAppend() throws Exception {
        Fixture f = fixture(); primeExistingOnly(f);
        CEventEffect emitter = events(source(f, 2), 0, 48, CEventEffect.class).get(0);
        emitter.setInt11(emitter.getInt11() + 1);
        JsonNode mek = tree(f.mek), waz = tree(f.waz);
        assertThrows(IllegalStateException.class, () -> customize(f));
        assertEquals(mek, tree(f.mek)); assertEquals(waz, tree(f.waz));
    }

    @Test
    void closeLaunchMustRejectAConditionWhoseNextTargetWasChangedToEnd() throws Exception {
        assertCloseRouteRejected(c -> c.getBsdxInfoCollectionList2().get(0).setTypeList(new ArrayList<>(List.of(0, 2))));
    }

    @Test
    void closeLaunchMustRejectChangedObjectOperandsAndChangeMode() {
        assertAll(
                () -> assertCloseRouteRejected(c -> c.getBsdxInfoCollectionList1().get(0).setIntList3(new ArrayList<>(List.of(1, 0)))),
                () -> assertCloseRouteRejected(c -> c.getBsdxInfoCollectionList1().get(0).setIntList4(new ArrayList<>(List.of(0, 0)))),
                () -> assertCloseRouteRejected(c -> c.getBsdxInfoCollectionList1().get(0).setInt2(1)),
                () -> assertCloseRouteRejected(c -> c.setInt1(1)),
                () -> assertCloseRouteRejected(c -> c.getBsdxInfoCollectionList2().get(0).setIntList3(new ArrayList<>(List.of(0, 0)))),
                () -> assertCloseRouteRejected(c -> c.getBsdxInfoCollectionList2().get(0).setInt2(1)));
    }

    @Test
    void oldQuickDeflectDonorCannotSkipItsGuardViaALaterFirstSuffixBlock() throws Exception {
        Fixture f = fixture();
        var suffix = source(f, 12).getSkillSuffixList();
        assertEquals(List.of(List.of(0, 0), List.of(1, 0)), suffix.stream().map(v -> List.of(v.getInt1(), v.getInt2())).toList());
        suffix.get(0).setInt1(1);
        assertRejectedWithoutWrites(f);
    }

    private static void assertCloseRouteRejected(Consumer<CEventChange> mutation) throws Exception {
        Fixture f = fixture();
        var change = event(source(f, 0), 0, 71, CEventChange.class, 12, 23);
        var condition = change.getBsdxInfoCollectionList1().get(0);
        var destination = change.getBsdxInfoCollectionList2().get(0);
        assertEquals(0, change.getInt1());
        assertEquals(List.of(0, 0), condition.getIntList3()); assertEquals(List.of(1, 0), condition.getIntList4()); assertEquals(0, condition.getInt2());
        assertEquals(16, destination.getInt1()); assertEquals(List.of(0, 0), destination.getTypeList());
        assertTrue(destination.getParamList().isEmpty()); assertTrue(destination.getIntList3().isEmpty());
        assertTrue(destination.getIntList4().isEmpty()); assertEquals(0, destination.getInt2());
        mutation.accept(change);
        assertRejectedWithoutWrites(f);
    }

    private static void assertRejectedWithoutWrites(Fixture f) {
        JsonNode beforeMek = tree(f.mek), beforeWaz = tree(f.waz);
        assertAll(() -> assertThrows(IllegalStateException.class, () -> customize(f)),
                () -> assertTrue(beforeMek.equals(tree(f.mek)), "rejected raw fixture must preserve the complete MEK graph"),
                () -> assertTrue(beforeWaz.equals(tree(f.waz)), "rejected raw fixture must preserve the complete WAZ graph"));
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
    private static void clean(JsonNode n) { if (n instanceof ObjectNode o) o.remove(List.of("offset", "length", "slotNum", "unitDescription", "description")); n.forEach(NagiSixVariantsTest::clean); }
    private record Variant(String name, String english, String sequence, int heat, int sourceRow, List<Integer> donors) { }
    private record Fixture(Mek mek, Waz waz, FollowupMekaContext context, int weaponCount, int skillCount,
                           JsonNode weapons, JsonNode skills, JsonNode materials, JsonNode ai,
                           List<Object> raw, List<JsonNode> rawTrees, Map<Path, byte[]> files) { }

}
