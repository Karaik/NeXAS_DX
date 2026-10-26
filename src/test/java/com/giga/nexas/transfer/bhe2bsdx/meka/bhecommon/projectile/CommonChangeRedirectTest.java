package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross.CrossRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommonChangeRedirectTest {

    @Test
    void explicitWazJumpsUseTheMappedHostAndAppendBaseAfterBinaryReadback() throws Exception {
        CEventChange effect = change(16, List.of(1), List.of(0, 100));
        CEventChange laser = change(16, List.of(1), List.of(6, 130));
        redirect(plan(146, 402), effect, laser);

        assertEquals(List.of(0, 246), readBack(effect).getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(9, 532), readBack(laser).getBsdxInfoCollectionList2().get(0).getParamList());
    }

    @Test
    void inheritedHostSizesComeFromTheCurrentPlan() {
        CEventChange change = change(16, List.of(1), List.of(0, 100));
        redirect(plan(701, 902), change);
        assertEquals(List.of(0, 801), change.getBsdxInfoCollectionList2().get(0).getParamList());
    }

    @Test
    void phaseAlgorithmAndNegativeSentinelParametersArePreserved() {
        CEventChange phase = change(16, List.of(0, 1), List.of(4));
        CEventChange algorithm = change(16, List.of(2), List.of(0, 100));
        CEventChange currentGroup = change(16, List.of(1), List.of(-1, 100));
        CEventChange sentinelSkill = change(16, List.of(1), List.of(6, -1));
        redirect(plan(146, 402), phase, algorithm, currentGroup, sentinelSkill);

        assertEquals(List.of(4), phase.getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(0, 100), algorithm.getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(-1, 100), currentGroup.getBsdxInfoCollectionList2().get(0).getParamList());
        assertEquals(List.of(6, -1), sentinelSkill.getBsdxInfoCollectionList2().get(0).getParamList());
    }

    @Test
    void unmappedExplicitPublicJumpFailsInsteadOfKeepingASourceIdentity() {
        CEventChange change = change(16, List.of(1), List.of(5, 100));
        assertThrows(IllegalStateException.class, () -> redirect(plan(146, 402), change));
    }

    private static BheCommonProjectileAppendPlan plan(int effectBase, int laserBase) {
        BheCommonProjectileAppendPlan plan = new BheCommonProjectileAppendPlan();
        plan.getSourceWazIndexToTargetIndex().put(0, 0);
        plan.getSourceWazIndexToTargetIndex().put(6, 9);
        plan.getSourceWazIndexToTargetSkillBase().put(0, effectBase);
        plan.getSourceWazIndexToTargetSkillBase().put(6, laserBase);
        plan.getSourceSpriteIndexToTargetIndex().put(0, 0);
        plan.getSourceSePairToTargetItemIndex().put("0:0", 0);
        plan.setCommonProjectileSeGroupIndex(1);
        return plan;
    }

    private static void redirect(BheCommonProjectileAppendPlan plan, CEventChange... changes) {
        Waz waz = new Waz("effect.waz");
        Waz.Skill skill = new Waz.Skill();
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit(71, "変更処理");
        unit.getSkillInfoObjectList().addAll(List.of(changes));
        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        skill.setPhaseQuantity(1);
        waz.getSkillList().add(skill);
        TsukuyomiPackageBundle common = new TsukuyomiPackageBundle();
        common.getWazByFileName().put("effect.waz", waz);
        TsukuyomiConvertedBundle converted = new TsukuyomiConvertedBundle();
        converted.setCommonProjectileResourceBundle(common);
        new CrossRedirectBheCommonProjectileResourcesStep().redirect(
                new TsukuyomiGraftRequest(), new TsukuyomiRawSourceBundle(), null, converted, plan);
    }

    private static CEventChange change(int group, List<Integer> types, List<Integer> parameters) {
        CEventChange change = new CEventChange(0x22);
        change.setSlotNum(71);
        change.setStartFrame(0);
        change.setEndFrame(0);
        change.setFlag(0);
        change.setInt1(0);
        BsdxInfoCollection destination = new BsdxInfoCollection();
        destination.setInt1(group);
        destination.setInt2(0);
        destination.getTypeList().addAll(types);
        destination.getParamList().addAll(parameters);
        change.getBsdxInfoCollectionList2().add(destination);
        return change;
    }

    private static CEventChange readBack(CEventChange change) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (BinaryWriter writer = new BinaryWriter(bytes, "windows-31j")) {
            change.writeInfo(writer);
        }
        CEventChange parsed = new CEventChange(0x22);
        parsed.readInfo(new BinaryReader(bytes.toByteArray(), "windows-31j"));
        return parsed;
    }
}
