package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class AuxConsumerRebindTest {
    private final BheInfoCollectionTermConverter converter = new BheInfoCollectionTermConverter();

    @Test void anglePositionBranchRetainsHitNineAndMapsFirePosition() {
        assertEquals(List.of(9, 0), converter.convert(source(18, List.of(1), 9, 0)).getIntList3());
        assertEquals(List.of(13, 0), converter.convert(source(18, List.of(2), 14, 0)).getIntList3());
    }
    @Test void angleSelectorBranchMapsHitWazaUsingObjectDictionaryOnly() {
        assertEquals(List.of(8, 19), converter.convert(source(18, List.of(3, 0), 9, 19)).getIntList3());
    }
    @Test void conditionUsesObjectDictionaryAndDoesNotInterpretUnusedAnchor() {
        assertEquals(List.of(8, 19), converter.convert(source(0, List.of(0), 9, 19)).getIntList3());
    }
    @Test void mekAiTargetUsesObjectEvenWithPosRoot() {
        var ai = new CCpuEventAttack();
        ai.getBsdxInfoCollectionList().add(copied(20, List.of(1), 9, 19));
        new BheInfoCollectionObjectGraphRewriter().rewrite(ai, "AI");
        assertEquals(List.of(8, 19), ai.getBsdxInfoCollectionList().get(0).getIntList3());
    }
    @Test void inactiveAngleAuxIsPreservedWithoutPretendingItIsAReference() {
        var term = new CEventTerm(9);
        term.getBsdxInfoCollectionList().add(copied(18, List.of(0), 999, 999));
        new BheInfoCollectionObjectGraphRewriter().rewrite(term, "angle-before");
        assertEquals(List.of(999, 999), term.getBsdxInfoCollectionList().get(0).getIntList3());
    }
    @Test void invalidConsumedIndexHasContextInsteadOfRawArrayException() {
        var error = assertThrows(IllegalStateException.class,
                () -> converter.convert(source(18, List.of(3, 0), 999, 0), "bad-angle"));
        assertTrue(error.getMessage().contains("bad-angle"));
    }
    @Test void unknownTermConsumerDoesNotSilentlyPickPositionDictionary() {
        var term = new CEventTerm(99);
        term.getBsdxInfoCollectionList().add(copied(20, List.of(1), 9, 0));
        assertThrows(IllegalStateException.class,
                () -> new BheInfoCollectionObjectGraphRewriter().rewrite(term, "unknown"));
    }

    @Test void recordsHeightLossOnlyForPositionAnchors() {
        assertEquals(List.of(9, 0), converter.convert(source(20, List.of(1), 9, 19)).getIntList3());
        assertTrue(converter.getAuxAudit().get(0).heightLoss());
        converter.convert(source(18, List.of(3, 0), 9, 19));
        assertFalse(converter.getAuxAudit().get(1).heightLoss());
        assertTrue(converter.getAuxAudit().get(1).anchorNotRead());
    }

    @Test void nativeTermTypesSelectPositionConditionAndAngleConsumers() {
        for (int type : List.of(5, 6, 9, 10)) {
            var term = new CEventTerm(type);
            term.getBsdxInfoCollectionList().add(type == 5 ? copied(20, List.of(1), 10, 0)
                    : type == 6 ? copied(0, List.of(0), 9, 19) : copied(18, List.of(1), 14, 0));
            new BheInfoCollectionObjectGraphRewriter().rewrite(term, "native-type" + type);
            assertEquals(type == 5 ? List.of(9, 0) : type == 6 ? List.of(8, 19) : List.of(13, 0),
                    term.getBsdxInfoCollectionList().get(0).getIntList3());
        }
    }

    @Test void realSouAndMisakiObjectGraphsPreservePositionBranches() throws Exception {
        var service = new com.giga.nexas.service.BheBinService();
        var conversion = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter();
        for (String role : List.of("sou", "misaki")) {
            var source = (com.giga.nexas.dto.bhe.waz.Waz) service.parse(
                    "src/main/resources/game/bhe/waz/" + role + "/" + role + ".waz", "windows-31j").getData();
            var target = conversion.convert(source);
            var rewriter = new BheInfoCollectionObjectGraphRewriter();
            rewriter.rewrite(target, role);
            if (role.equals("sou")) {
                var body = target.getSkillList().stream().filter(s -> "BODYSLUM01".equals(s.getSkillNameEnglish())).findFirst().orElseThrow();
                var tree = new com.fasterxml.jackson.databind.ObjectMapper().valueToTree(body);
                assertTrue(hasPositionHit(tree), "真实BODYSLUM01角度位置分支应保留HIT9");
            } else {
                var upper = target.getSkillList().stream().filter(s -> "M_UPPER04".equals(s.getSkillNameEnglish())).findFirst().orElseThrow();
                var effects = upper.getPhasesInfo().get(0).getSkillUnitCollection().stream()
                        .filter(u -> u.getUnitQuantity() == 48).findFirst().orElseThrow();
                var effect = (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect) effects.getSkillInfoObjectList().get(2);
                var direction = (CEventTerm) effect.getCeventEffectUnitList().stream()
                        .filter(u -> u.getUnitSlotNum() == 28).findFirst().orElseThrow().getData();
                assertEquals(List.of(13, 0), direction.getBsdxInfoCollectionList().get(0).getIntList4(), "Misaki FIREPOS14应转为位置字典13");
            }
        }
    }

    private static boolean hasPositionHit(com.fasterxml.jackson.databind.JsonNode node) {
        if (node.has("typeList") && node.has("int1") && node.get("int1").asInt() == 18
                && node.get("typeList").size() > 0 && node.get("typeList").get(0).asInt() == 1
                && node.has("intList3") && node.get("intList3").size() > 0
                && node.get("intList3").get(0).asInt() == 9) return true;
        for (var child : node) if (hasPositionHit(child)) return true;
        return false;
    }
    private static BheInfoCollection source(int root, List<Integer> chain, int object, int anchor) {
        var source = new BheInfoCollection();source.setInt1(root);source.setTypeList(chain);
        source.setIntList3(List.of(object, anchor));source.setInt2(0);return source;
    }
    private static BsdxInfoCollection copied(int root, List<Integer> chain, int object, int anchor) {
        var c = new BsdxInfoCollection();c.setInt1(root);c.getTypeList().addAll(chain);
        c.getIntList3().addAll(List.of(object, anchor));return c;
    }
}
