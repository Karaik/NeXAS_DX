package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BuildResourceClosureStepCommonReferenceTest {

    private final BuildResourceClosureStep step = new BuildResourceClosureStep();

    @Test
    void keepCommonReferencesOutOfSelectedClosure() {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        TsukuyomiPackageBundle sourcePackage = new TsukuyomiPackageBundle();
        sourcePackage.setWazaGroupGrp(sourceWazaGroup());
        sourcePackage.setSpriteGroupGrp(sourceSpriteGroup());
        sourcePackage.getWazByFileName().put("tsukuyomi.waz", sourceWazWithCommonReferences());
        sourcePackage.getSpmByFileName().put("tsukuyomi.spm", new com.giga.nexas.dto.bsdx.spm.Spm());
        sourcePackage.setTsukuyomiMek(new com.giga.nexas.dto.bsdx.mek.Mek());

        TsukuyomiImportPlan plan = step.buildResourceClosure(
                request,
                sourcePackage,
                emptyBaseline(),
                commonPlan()
        );

        assertEquals(1, plan.getRequiredWazFiles().size());
        assertTrue(plan.getRequiredWazFiles().contains("tsukuyomi.waz"));
        assertTrue(plan.getReferencedSourceWazFileNameByGroupIndex().isEmpty());
        assertTrue(plan.getReferencedSourceSpriteFileNameByGroupIndex().isEmpty());
        assertTrue(plan.getReferencedSourceSeItemIndicesByGroupIndex().isEmpty());
        assertEquals(108, plan.getCommonWazReferenceTargetIndexBySourceIndex().get(0));
        assertEquals(149, plan.getCommonSpriteReferenceTargetIndexBySourceIndex().get(173));
        assertEquals(656, plan.getCommonSeReferenceTargetItemIndexBySourcePair().get("47:55"));
        assertTrue(plan.getUnresolvedResources().isEmpty());
    }

    private BheCommonProjectileAppendPlan commonPlan() {
        BheCommonProjectileAppendPlan plan = new BheCommonProjectileAppendPlan();
        plan.getSourceWazIndexToTargetIndex().put(0, 108);
        plan.getSourceSpriteIndexToTargetIndex().put(173, 149);
        plan.setCommonProjectileSeGroupIndex(38);
        plan.getSourceSePairToTargetItemIndex().put(BheCommonProjectileAppendPlan.sePairKey(47, 55), 656);
        return plan;
    }

    private TsukuyomiBsdxBaselineBundle emptyBaseline() {
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.setWazaGroupGrp(new WazaGroupGrp());
        baseline.setSpriteGroupGrp(new SpriteGroupGrp());
        return baseline;
    }

    private WazaGroupGrp sourceWazaGroup() {
        WazaGroupGrp group = new WazaGroupGrp();
        group.getWazaList().add(wazaEntry("Effect"));
        for (int i = 1; i < 11; i++) {
            group.getWazaList().add(new WazaGroupGrp.WazaGroupEntry());
        }
        group.getWazaList().add(wazaEntry("Tsukuyomi"));
        return group;
    }

    private WazaGroupGrp.WazaGroupEntry wazaEntry(String displayName) {
        WazaGroupGrp.WazaGroupEntry entry = new WazaGroupGrp.WazaGroupEntry();
        entry.setExistFlag(1);
        entry.setWazaDisplayName(displayName);
        entry.setWazaCodeName(displayName.toUpperCase());
        entry.setWazaName(displayName);
        entry.setParam(1);
        return entry;
    }

    private SpriteGroupGrp sourceSpriteGroup() {
        SpriteGroupGrp group = new SpriteGroupGrp();
        for (int i = 0; i < 174; i++) {
            group.getSpriteList().add(new SpriteGroupGrp.SpriteGroupEntry());
        }
        SpriteGroupGrp.SpriteGroupEntry publicSprite = new SpriteGroupGrp.SpriteGroupEntry();
        publicSprite.setExistFlag(1);
        publicSprite.setSpriteFileName("設置物：掲示板.spm");
        publicSprite.setSpriteCodeName("MAPOBJ_KEIZIBAN");
        group.getSpriteList().set(173, publicSprite);
        return group;
    }

    private Waz sourceWazWithCommonReferences() {
        Waz waz = new Waz();
        waz.setFileName("tsukuyomi");
        Waz.Skill skill = new Waz.Skill();
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit(0, "test");
        CEventWazaSelect select = new CEventWazaSelect(4);
        select.setWazFileNo(0);
        select.setWazSequenceNo(12);
        CEventSprite sprite = new CEventSprite(13);
        sprite.setSpmFileSequence(173);
        sprite.setActionGroupNumber(4);
        CEventSe se = new CEventSe(23);
        se.setCount(1);
        se.getByteDataList().add(seBytes(47, 55));
        unit.getSkillInfoObjectList().add(select);
        unit.getSkillInfoObjectList().add(sprite);
        unit.getSkillInfoObjectList().add(se);
        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        waz.getSkillList().add(skill);
        return waz;
    }

    private byte[] seBytes(int group, int item) {
        byte[] bytes = new byte[16];
        writeLittleEndian(bytes, 0, group);
        writeLittleEndian(bytes, 4, item);
        return bytes;
    }

    private void writeLittleEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }
}
