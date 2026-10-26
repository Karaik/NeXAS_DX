package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

class RebindWazSourceIsolationTest {

    @Test
    void tsukuyomiCanRebindTheSameSourceTwiceWithoutChangingItsCollections() {
        RebindWazStep step = new RebindWazStep();
        assertSourceIsolation(source -> step.rebindAuxiliaryWaz(
                new TsukuyomiGraftRequest(), source, importPlan(), privatePlan(), 11,
                new BheCommonProjectileAppendPlan()));
    }

    @Test
    void followupCanRebindTheSameSourceTwiceWithoutChangingItsCollections() {
        var step = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindWazStep();
        assertSourceIsolation(source -> step.rebindAuxiliaryWaz(
                new TsukuyomiGraftRequest(), source, importPlan(), privatePlan(), 11,
                new BheCommonProjectileAppendPlan()));
    }

    private void assertSourceIsolation(Function<Waz, Waz> rebind) {
        Waz source = source();
        CEventChange originalChange = change(source);
        CEventTerm originalTerm = term(source);
        Waz first = rebind.apply(source);

        assertEquals(List.of(11, 12), originalChange.getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(12, originalTerm.getBsdxInfoCollectionList().get(0).getInt1());
        assertEquals(List.of(15, 0), originalTerm.getBsdxInfoCollectionList().get(0).getTypeList());
        assertNotSame(originalChange.getBsdxInfoCollectionList2().get(0),
                change(first).getBsdxInfoCollectionList2().get(0));
        assertNotSame(originalTerm.getBsdxInfoCollectionList().get(0).getParamList(),
                term(first).getBsdxInfoCollectionList().get(0).getParamList());

        Waz second = rebind.apply(source);
        assertEquals(List.of(200, 12), change(first).getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(200, 12), change(second).getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(term(first).getBsdxInfoCollectionList(), term(second).getBsdxInfoCollectionList());
        term(first).getBsdxInfoCollectionList().get(0).getParamList().set(0, 999);
        assertEquals(List.of(7), originalTerm.getBsdxInfoCollectionList().get(0).getParamList());
        assertEquals(List.of(7), term(second).getBsdxInfoCollectionList().get(0).getParamList());
    }

    private static Waz source() {
        Waz source = new Waz("aux.waz");
        source.setExtensionName("waz");
        Waz.Skill skill = new Waz.Skill();
        skill.setPhaseQuantity(1);
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit(71, "test");
        CEventChange change = new CEventChange(34);
        change.setFlag(0);
        change.setInt1(0);
        change.getBsdxInfoCollectionList2().add(collection(16, List.of(1), List.of(11, 12)));
        CEventTerm term = new CEventTerm(5);
        // BHE PARAM/VAL在重编译后会改变root；源对象必须保留BHE字典身份。
        term.getBsdxInfoCollectionList().add(collection(12, List.of(15, 0), List.of(7)));
        unit.getSkillInfoObjectList().add(change);
        phase.getSkillUnitCollection().add(unit);
        SkillUnit position = new SkillUnit(11, "位置");
        position.getSkillInfoObjectList().add(term);
        phase.getSkillUnitCollection().add(position);
        skill.getPhasesInfo().add(phase);
        source.getSkillList().add(skill);
        return source;
    }

    private static BsdxInfoCollection collection(int root, List<Integer> types, List<Integer> parameters) {
        BsdxInfoCollection collection = new BsdxInfoCollection();
        collection.setInt1(root);
        collection.setInt2(0);
        collection.getTypeList().addAll(types);
        collection.getParamList().addAll(parameters);
        return collection;
    }

    private static TsukuyomiImportPlan importPlan() {
        TsukuyomiImportPlan plan = new TsukuyomiImportPlan();
        plan.getSourceWazIndexByFileName().put("aux.waz", 11);
        plan.getTargetWazIndexByFileName().put("aux.waz", 200);
        return plan;
    }

    private static TsukuyomiGrpAppendPlan privatePlan() {
        TsukuyomiGrpAppendPlan plan = new TsukuyomiGrpAppendPlan();
        plan.setWazaGroupIndex(200);
        plan.getSourceWazGroupIndexToTargetIndex().put(11, 200);
        return plan;
    }

    private static CEventChange change(Waz waz) {
        return (CEventChange) waz.getSkillList().get(0).getPhasesInfo().get(0)
                .getSkillUnitCollection().get(0).getSkillInfoObjectList().get(0);
    }

    private static CEventTerm term(Waz waz) {
        return (CEventTerm) waz.getSkillList().get(0).getPhasesInfo().get(0)
                .getSkillUnitCollection().get(1).getSkillInfoObjectList().get(0);
    }
}
