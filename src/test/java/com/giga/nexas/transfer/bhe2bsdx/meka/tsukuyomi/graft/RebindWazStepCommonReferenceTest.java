package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class RebindWazStepCommonReferenceTest {

    private final RebindWazStep step = new RebindWazStep();

    @Test
    void rebindPublicReferencesThroughCommonPlan() {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        TsukuyomiPackageBundle sourcePackage = new TsukuyomiPackageBundle();
        sourcePackage.getWazByFileName().put("tsukuyomi.waz", sourceWazWithCommonReferences());

        TsukuyomiImportPlan importPlan = new TsukuyomiImportPlan();
        importPlan.getSourceWazIndexByFileName().put("tsukuyomi.waz", 11);

        TsukuyomiGrpAppendPlan privatePlan = new TsukuyomiGrpAppendPlan();
        privatePlan.setWazaGroupIndex(200);
        privatePlan.setSpriteGroupIndex(201);
        privatePlan.setBatVoiceGroupIndex(202);
        privatePlan.getSourceWazGroupIndexToTargetIndex().put(11, 200);

        Waz rebound = step.rebindTsukuyomiWaz(request, sourcePackage, importPlan, privatePlan, commonPlan());

        SkillUnit unit = rebound.getSkillList().get(0).getPhasesInfo().get(0).getSkillUnitCollection().get(0);
        CEventWazaSelect select = (CEventWazaSelect) unit.getSkillInfoObjectList().get(0);
        CEventSprite sprite = (CEventSprite) unit.getSkillInfoObjectList().get(1);
        CEventSe se = (CEventSe) unit.getSkillInfoObjectList().get(2);

        assertEquals(108, select.getWazFileNo());
        assertEquals(12, select.getWazSequenceNo());
        assertEquals(149, sprite.getSpmFileSequence());
        assertEquals(4, sprite.getActionGroupNumber());
        assertEquals(38, readLittleEndian(se.getByteDataList().get(0), 0));
        assertEquals(656, readLittleEndian(se.getByteDataList().get(0), 4));
    }

    @Test
    void rebindAuxiliaryWazKeepsWholeBheFileSkillLayout() {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        Waz sourceWaz = wazWithNamedSkills("aux0", "aux1", "aux2");
        TsukuyomiImportPlan importPlan = new TsukuyomiImportPlan();
        importPlan.getSourceWazIndexByFileName().put("aux.waz", 20);
        importPlan.getTargetWazIndexByFileName().put("aux.waz", 220);
        TsukuyomiGrpAppendPlan privatePlan = new TsukuyomiGrpAppendPlan();
        privatePlan.getSourceWazGroupIndexToTargetIndex().put(20, 220);
        privatePlan.getSourceWazSkillIndexToTargetIndexByGroup().put(20, Map.of(0, 0, 1, 1, 2, 2));

        Waz rebound = step.rebindAuxiliaryWaz(
                request,
                sourceWaz,
                importPlan,
                privatePlan,
                20,
                new BheCommonProjectileAppendPlan()
        );

        assertEquals(3, rebound.getSkillList().size());
        assertEquals("aux0", rebound.getSkillList().get(0).getSkillNameEnglish());
        assertEquals("aux1", rebound.getSkillList().get(1).getSkillNameEnglish());
        assertEquals("aux2", rebound.getSkillList().get(2).getSkillNameEnglish());
    }

    private BheCommonProjectileAppendPlan commonPlan() {
        BheCommonProjectileAppendPlan plan = new BheCommonProjectileAppendPlan();
        plan.getSourceWazIndexToTargetIndex().put(0, 108);
        plan.getSourceSpriteIndexToTargetIndex().put(173, 149);
        plan.setCommonProjectileSeGroupIndex(38);
        plan.getSourceSePairToTargetItemIndex().put(BheCommonProjectileAppendPlan.sePairKey(47, 55), 656);
        return plan;
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

    private Waz wazWithNamedSkills(String... names) {
        Waz waz = new Waz();
        waz.setFileName("aux");
        waz.setExtensionName("waz");
        for (String name : names) {
            Waz.Skill skill = new Waz.Skill();
            skill.setSkillNameJapanese(name);
            skill.setSkillNameEnglish(name);
            waz.getSkillList().add(skill);
        }
        return waz;
    }

    private byte[] seBytes(int group, int item) {
        byte[] bytes = new byte[16];
        writeLittleEndian(bytes, 0, group);
        writeLittleEndian(bytes, 4, item);
        return bytes;
    }

    private int readLittleEndian(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private void writeLittleEndian(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }
}
