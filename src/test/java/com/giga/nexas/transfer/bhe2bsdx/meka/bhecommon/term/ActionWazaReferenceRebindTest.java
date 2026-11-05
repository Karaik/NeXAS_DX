package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output.OutputBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class ActionWazaReferenceRebindTest {
    private static final ObjectMapper JSON = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private static final String CHARSET = "windows-31j";
    @TempDir Path output;

    @Test
    void realSouProcterBitConditionUsesTheAssignedPrivateWazIdentityWithoutMutatingItsSource() throws Exception {
        Waz source = realBit();
        JsonNode before = JSON.valueToTree(source);
        var rawSou = (com.giga.nexas.dto.bhe.waz.Waz) new BheBinService().parse(
                "src/main/resources/game/bhe/waz/sou/sou.waz", CHARSET).getData();
        assertEquals("A_PROCTER03", rawSou.getSkillList().get(413).getSkillNameEnglish());
        for (boolean followup : List.of(true, false)) {
            Waz target = rebind(source, privatePlan(120, 413), followup);
            var conditions = actionConditions(target);
            assertEquals(1, conditions.size(), "real BIT47 has one negated owner ACTION_WAZA condition");
            assertEquals(List.of(120, 413), conditions.get(0).getParamList());
            assertEquals(1, conditions.get(0).getInt2(), "native negation is independent of resource rebinding");
            assertEquals(List.of(3, 0), conditions.get(0).getIntList3(), "root-owner selection is preserved");
            assertEquals(before, JSON.valueToTree(source));
        }
    }

    @Test
    void actionWazaRebindingUsesNonidentitySkillMapsAndPreservesOtherConsumersAndUnmappedPairs() throws Exception {
        for (int selector : List.of(0, 1, 3)) {
            Waz source = conditionWaz(0, List.of(2, 1, 16), List.of(9, 413), selector);
            assertEquals(List.of(233, 501), actionConditions(rebind(source, privatePlan(233, 501), true)).get(0).getParamList());
        }
        for (Waz source : List.of(
                conditionWaz(0, List.of(2, 1, 14), List.of(9, 413), 3),
                conditionWaz(0, List.of(2, 1, 16), List.of(99, 413), 3),
                conditionWaz(0, List.of(2, 1, 16), List.of(9, 9999), 3),
                conditionWaz(0, List.of(2, 1, 16), List.of(-1, 413), 3),
                conditionWaz(0, List.of(2, 1, 16), List.of(120, 413), 3))) {
            Waz target = rebind(source, privatePlan(120, 413), true);
            assertEquals(firstCondition(source).getParamList(), firstCondition(target).getParamList());
        }
    }

    @Test
    void commonOutputRebindsOnlyItsBheSliceAndEachOutputStartsFromTheUnchangedBaseline() throws Exception {
        Waz bit = realBit();
        new BheInfoCollectionObjectGraphRewriter().rewrite(bit, "real BIT47 output fixture");
        Waz nativePrefix = conditionWaz(0, List.of(2, 1, 13), List.of(9, 413), 3);
        Waz targetTail = copy(nativePrefix);
        Waz shared = copy(nativePrefix);
        shared.setFileName("Tama03");
        shared.getSkillList().addAll(bit.getSkillList());
        shared.getSkillList().addAll(targetTail.getSkillList());
        JsonNode before = JSON.valueToTree(shared);
        BheCommonProjectileAppendPlan common = publicPlan();
        common.getCommonProjectileWazFiles().add("Tama03.waz");
        common.getSourceWazIndexToTargetFileName().put(3, "Tama03.waz");
        common.getSourceWazIndexToTargetSkillBase().put(3, 1);
        common.getSourceWazIndexToTargetSkillCount().put(3, 1);
        rememberPrivateTargets(common, privatePlan(120, 413));
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.getWazByFileName().put("Tama03.waz", shared);
        RecordingService service = new RecordingService();
        var step = new OutputBheCommonProjectileResourcesStep(service);
        for (int pass = 0; pass < 2; pass++) {
            step.output(new TsukuyomiGraftRequest(), baseline, common, Map.of(), output,
                    new TsukuyomiImportedAssetSet());
            Waz emitted = service.waz;
            assertEquals(List.of(9, 413), firstCondition(skillWaz(emitted.getSkillList().get(0))).getParamList(),
                    "native DX prefix group9 means KOU and is not a BHE source reference");
            assertEquals(List.of(120, 413), actionConditions(skillWaz(emitted.getSkillList().get(1))).get(0).getParamList());
            assertEquals(List.of(9, 413), firstCondition(skillWaz(emitted.getSkillList().get(2))).getParamList(),
                    "unregistered target-domain tail is outside the recorded BHE source slice");
            assertEquals(before, JSON.valueToTree(shared), "common output must not mutate the prepared baseline");
            actionConditions(skillWaz(emitted.getSkillList().get(1))).get(0).getParamList().set(1, 999);
            assertEquals(before, JSON.valueToTree(shared), "output condition lists must be deeply isolated");
        }
    }

    @Test
    void publicOutputCanWaitForPrivateAllocationThenRebindOnANewCopy() throws Exception {
        Waz shared = realBit();
        new BheInfoCollectionObjectGraphRewriter().rewrite(shared, "late owner fixture");
        JsonNode before = JSON.valueToTree(shared);
        BheCommonProjectileAppendPlan common = publicPlan();
        common.getCommonProjectileWazFiles().add("Tama03.waz");
        common.getSourceWazIndexToTargetFileName().put(3, "Tama03.waz");
        common.getSourceWazIndexToTargetSkillBase().put(3, 0);
        common.getSourceWazIndexToTargetSkillCount().put(3, 1);
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.getWazByFileName().put("Tama03.waz", shared);
        RecordingService service = new RecordingService();
        var step = new OutputBheCommonProjectileResourcesStep(service);
        step.output(new TsukuyomiGraftRequest(), baseline, common, Map.of(), output, new TsukuyomiImportedAssetSet());
        assertEquals(List.of(9, 413), actionConditions(service.waz).get(0).getParamList());
        rememberPrivateTargets(common, privatePlan(233, 501));
        step.output(new TsukuyomiGraftRequest(), baseline, common, Map.of(), output, new TsukuyomiImportedAssetSet());
        assertEquals(List.of(233, 501), actionConditions(service.waz).get(0).getParamList());
        assertEquals(before, JSON.valueToTree(shared));
    }

    @Test
    void realNagiOwnerConditionUsesItsOwnGroupAndHeatProcterHasNoSuchCondition() throws Exception {
        Waz nagi = realSkill(88, "BIT77");
        var allocated = privatePlan(120, 413);
        allocated.getSourceWazGroupIndexToTargetIndex().put(14, 234);
        allocated.getSourceWazSkillIndexToTargetIndexByGroup().put(14, Map.of(15, 115));
        assertEquals(List.of(234, 115), actionConditions(rebind(nagi, allocated, true)).get(0).getParamList());
        for (int index : List.of(64, 65, 66, 67, 68)) {
            Waz heat = realSkill(index, switch (index) {
                case 64 -> "BIT63"; case 65 -> "BIT64"; case 66 -> "BIT49";
                case 67 -> "BIT50"; default -> "BIT57";
            });
            new BheInfoCollectionObjectGraphRewriter().rewrite(heat, "HeatProcter " + index);
            assertEquals(0, actionConditions(heat).size());
            JsonNode before = JSON.valueToTree(heat);
            BheActionWazaReferenceRebinder.rebindSourceCopy(heat, publicPlan(), allocated);
            assertEquals(before, JSON.valueToTree(heat), "absent consumer must not invent lifetime or exits");
        }
    }

    @Test
    void onlyConditionConsumersRebindAndUnconsumedParameterTailIsPreserved() throws Exception {
        var source = conditionWaz(0, List.of(2, 1, 16), List.of(9, 413, 9), 3);
        Waz target = rebind(source, privatePlan(233, 501), true);
        assertEquals(List.of(233, 501, 9), firstCondition(target).getParamList(),
                "native ACTION_WAZA consumes its first pair, never unrelated trailing integers");
        for (int type : List.of(6, 5, 9, 11, 32)) {
            Waz compiled = conditionWaz(0, List.of(2, 1, 16), List.of(9, 413), 3);
            new BheInfoCollectionObjectGraphRewriter().rewrite(compiled, "condition consumer fixture");
            var term = new CEventTerm(type); term.getBsdxInfoCollectionList().add(firstCondition(compiled));
            BheActionWazaReferenceRebinder.rebindSourceCopy(term, publicPlan(), privatePlan(233, 501));
            assertEquals(type == 6 ? List.of(233, 501) : List.of(9, 413),
                    term.getBsdxInfoCollectionList().get(0).getParamList());
        }
    }

    @Test
    void commonSourceSkillBoundsAndBaseAreAppliedWithoutRebindingInheritedGroups() throws Exception {
        Waz target = conditionWaz(0, List.of(2, 1, 16), List.of(3, 60), 3);
        new BheInfoCollectionObjectGraphRewriter().rewrite(target, "common ACTION_WAZA");
        BheCommonProjectileAppendPlan common = publicPlan();
        common.getSourceWazIndexToTargetIndex().put(3, 93);
        common.getSourceWazIndexToTargetSkillBase().put(3, 47);
        common.getSourceWazIndexToTargetSkillCount().put(3, 61);
        BheActionWazaReferenceRebinder.rebindSourceCopy(target, common, null);
        assertEquals(List.of(93, 107), firstCondition(target).getParamList());
        BheActionWazaReferenceRebinder.rebindSourceCopy(target, common, null);
        assertEquals(List.of(93, 107), firstCondition(target).getParamList());
        Waz outside = conditionWaz(0, List.of(2, 1, 16), List.of(3, 61), 3);
        new BheInfoCollectionObjectGraphRewriter().rewrite(outside, "out-of-source-range fixture");
        BheActionWazaReferenceRebinder.rebindSourceCopy(outside, common, null);
        assertEquals(List.of(3, 61), firstCondition(outside).getParamList());
    }

    @Test
    void assignedBitConditionSurvivesTwoRealBinaryPasses() throws Exception {
        Waz target = rebind(realBit(), privatePlan(233, 501), true);
        BsdxBinService service = new BsdxBinService();
        Path first = output.resolve("bit47-pass1.waz"), second = output.resolve("bit47-pass2.waz");
        service.generate(first.toString(), target, CHARSET);
        Waz parsed = (Waz) service.parse(first.toString(), CHARSET).getData();
        assertEquals(List.of(233, 501), actionConditions(parsed).get(0).getParamList());
        service.generate(second.toString(), parsed, CHARSET);
        assertArrayEquals(Files.readAllBytes(first), Files.readAllBytes(second));
    }

    @Test
    void privateCopiesCanResolveAlreadyAllocatedOtherCharactersWithoutSharingTheAllocationMaps() throws Exception {
        BheCommonProjectileAppendPlan common = publicPlan();
        TsukuyomiGrpAppendPlan previous = new TsukuyomiGrpAppendPlan();
        previous.getSourceWazGroupIndexToTargetIndex().put(14, 234);
        previous.getSourceWazSkillIndexToTargetIndexByGroup().put(14, new LinkedHashMap<>(Map.of(15, 115)));
        rememberPrivateTargets(common, previous);
        previous.getSourceWazSkillIndexToTargetIndexByGroup().get(14).put(15, 999);
        Waz target = conditionWaz(0, List.of(2, 1, 16), List.of(14, 15), 3);
        new BheInfoCollectionObjectGraphRewriter().rewrite(target, "previously allocated owner");
        BheActionWazaReferenceRebinder.rebindSourceCopy(target, common, privatePlan(120, 413));
        assertEquals(List.of(234, 115), firstCondition(target).getParamList());
    }

    private Waz rebind(Waz source, TsukuyomiGrpAppendPlan privatePlan, boolean followup) {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setWazFileName("sou.waz"); request.setSpriteFileName("sou.spm");
        TsukuyomiImportPlan imports = new TsukuyomiImportPlan();
        imports.getSourceWazIndexByFileName().put("sou.waz", 9);
        BheCommonProjectileAppendPlan common = publicPlan();
        for (Object node : nodes(source)) {
            if (node instanceof CEventSprite sprite && sprite.getSpmFileSequence() >= 0) {
                common.getSourceSpriteIndexToTargetIndex().put(sprite.getSpmFileSequence(), sprite.getSpmFileSequence());
            } else if (node instanceof CEventSe se) {
                for (byte[] bytes : se.getByteDataList()) {
                    ByteBuffer values = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                    int group = values.getInt(), item = values.getInt();
                    if (group >= 0 && item >= 0) {
                        privatePlan.getSourceSeGroupIndexToTargetIndex().put(group, group);
                        privatePlan.getSourceSeItemIndexToTargetIndexByGroup().computeIfAbsent(group, key -> new LinkedHashMap<>()).put(item, item);
                    }
                }
            }
        }
        return followup
                ? new com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindWazStep()
                    .rebindAuxiliaryWaz(request, source, imports, privatePlan, 3, common)
                : new com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.RebindWazStep()
                    .rebindAuxiliaryWaz(request, source, imports, privatePlan, 3, common);
    }

    private static TsukuyomiGrpAppendPlan privatePlan(int group, int skill) {
        var plan = new TsukuyomiGrpAppendPlan();
        plan.setWazaGroupIndex(group); plan.setSpriteGroupIndex(121); plan.setBatVoiceGroupIndex(58);
        plan.getSourceWazGroupIndexToTargetIndex().put(9, group);
        plan.getSourceWazSkillIndexToTargetIndexByGroup().put(9, new LinkedHashMap<>(Map.of(413, skill)));
        return plan;
    }

    private static BheCommonProjectileAppendPlan publicPlan() {
        var plan = new BheCommonProjectileAppendPlan();
        for (int group = 0; group < 8; group++) {
            plan.getSourceWazIndexToTargetIndex().put(group, group);
            plan.getSourceWazIndexToTargetSkillBase().put(group, 0);
            plan.getSourceWazIndexToTargetSkillCount().put(group, 1000);
        }
        return plan;
    }

    private static Waz realBit() throws Exception {
        return realSkill(60, "BIT47");
    }

    private static Waz realSkill(int index, String english) throws Exception {
        var raw = (com.giga.nexas.dto.bhe.waz.Waz) new BheBinService().parse(
                "src/main/resources/game/bhe/waz/tama03.waz", CHARSET).getData();
        assertEquals(english, raw.getSkillList().get(index).getSkillNameEnglish());
        Waz converted = new BheToBsdxWazConverter().convert(raw);
        return skillWaz(converted.getSkillList().get(index));
    }

    private static Waz conditionWaz(int root, List<Integer> types, List<Integer> params, int object) {
        var c = new BsdxInfoCollection(); c.setInt1(root); c.setInt2(1);
        c.setTypeList(new ArrayList<>(types)); c.setParamList(new ArrayList<>(params));
        c.setIntList3(new ArrayList<>(List.of(object, 0)));
        var end = new BsdxInfoCollection(); end.setInt1(16); end.setInt2(0);
        end.setTypeList(new ArrayList<>(List.of(0, 2)));
        var change = new CEventChange(34); change.setFlag(1); change.setInt1(0);
        change.setStartFrame(0); change.setEndFrame(0);
        change.getBsdxInfoCollectionList1().add(c); change.getBsdxInfoCollectionList2().add(end);
        var unit = new SkillUnit(71, "condition fixture"); unit.getSkillInfoObjectList().add(change);
        var phase = new Waz.Skill.SkillPhase(); phase.getSkillUnitCollection().add(unit);
        var skill = new Waz.Skill(); skill.setPhaseQuantity(1); skill.setSkillNameJapanese("fixture");
        skill.setSkillNameEnglish("fixture"); skill.getPhasesInfo().add(phase);
        return skillWaz(skill);
    }

    private static Waz skillWaz(Waz.Skill skill) {
        var waz = new Waz(); waz.setFileName("Tama03"); waz.setExtensionName("waz");
        waz.setSkillList(new ArrayList<>(List.of(skill))); return waz;
    }

    private static BsdxInfoCollection firstCondition(Waz waz) {
        return ((CEventChange) waz.getSkillList().get(0).getPhasesInfo().get(0).getSkillUnitCollection().get(0)
                .getSkillInfoObjectList().get(0)).getBsdxInfoCollectionList1().get(0);
    }

    private static List<BsdxInfoCollection> actionConditions(Waz waz) {
        return nodes(waz).stream().filter(BsdxInfoCollection.class::isInstance).map(BsdxInfoCollection.class::cast)
                .filter(c -> BsdxInfoCollectionAnalyzer.analyze(c).getSteps().stream().anyMatch(s ->
                        "OBJECT3".equals(s.getTermGroupCodeName()) && "ACTION_WAZA".equals(s.getTermItemDescription())))
                .toList();
    }

    private static Waz copy(Waz source) throws Exception { return JSON.readValue(JSON.writeValueAsBytes(source), Waz.class); }

    private static void rememberPrivateTargets(BheCommonProjectileAppendPlan common, TsukuyomiGrpAppendPlan allocated) throws Exception {
        BheActionWazaReferenceRebinder.rememberPrivateTargets(common, allocated);
    }

    private static List<Object> nodes(Object root) {
        List<Object> result = new ArrayList<>(); visit(root, new IdentityHashMap<>(), result); return result;
    }

    private static void visit(Object value, IdentityHashMap<Object, Boolean> seen, List<Object> out) {
        if (value == null || seen.put(value, true) != null) return;
        if (value instanceof List<?> list) { list.forEach(v -> visit(v, seen, out)); return; }
        if (value instanceof Map<?, ?> map) { map.values().forEach(v -> visit(v, seen, out)); return; }
        if (!value.getClass().getName().startsWith("com.giga.nexas.")) return;
        out.add(value);
        for (Class<?> type = value.getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) if (!Modifier.isStatic(field.getModifiers()) && !field.isSynthetic()) {
                try { field.setAccessible(true); visit(field.get(value), seen, out); }
                catch (ReflectiveOperationException error) { throw new IllegalStateException(error); }
            }
        }
    }

    private static final class RecordingService extends BsdxBinService {
        Waz waz;
        @Override public <T extends Bsdx> ResponseDTO<?> generate(String path, T object, String charset) {
            assertInstanceOf(Waz.class, object); waz = (Waz) object; return new ResponseDTO<>(null, "captured");
        }
    }
}
