package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AppendGrpEntriesStepBheRebindTest {

    private final AppendGrpEntriesStep step = new AppendGrpEntriesStep();

    @Test
    void appendPrivateAuxiliaryWazAsWholeFileWithoutKeyBasedMerge() {
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setWazFileName("tsukuyomi.waz");
        request.setWazCodeName("TSUKUYOMI");
        request.setSpriteFileName("tsukuyomi.spm");
        request.setSpriteCodeName("TSUKUYOMI");
        request.setMekaCodeName("TSUKUYOMI");

        TsukuyomiPackageBundle sourcePackage = sourcePackage();
        TsukuyomiBsdxBaselineBundle baseline = baselineWithExistingAuxWaz();
        TsukuyomiImportPlan importPlan = new TsukuyomiImportPlan();
        importPlan.getSourceWazIndexByFileName().put("tsukuyomi.waz", 0);
        importPlan.getSourceWazIndexByFileName().put("aux.waz", 1);
        importPlan.getSourceSpriteIndexByFileName().put("tsukuyomi.spm", 13);
        importPlan.getReferencedSourceWazFileNameByGroupIndex().put(1, "aux.waz");

        TsukuyomiGrpAppendPlan plan = step.appendTsukuyomiBranch(request, sourcePackage, baseline, importPlan);

        assertEquals(1, plan.getWazaGroupIndex(), "主 WAZ 应追加到 baseline 原有 AUX 之后");
        assertEquals(2, plan.getSourceWazGroupIndexToTargetIndex().get(1), "私有辅助 WAZ 必须追加新 entry，不能复用 baseline 同名 AUX");
        assertEquals("AUX", baseline.getWazaGroupGrp().getWazaList().get(0).getWazaCodeName());
        assertEquals(99, baseline.getWazaGroupGrp().getWazaList().get(0).getParam());
        assertEquals(3, baseline.getWazaGroupGrp().getWazaList().get(2).getParam(), "辅助 WAZ param 应等于 BHE 源文件自身 skill 数");
        assertEquals(Map.of(0, 0, 1, 1, 2, 2), plan.getSourceWazSkillIndexToTargetIndexByGroup().get(1));
        assertEquals(3, plan.getTargetWazSkillCountByGroupIndex().get(2));
    }

    private TsukuyomiPackageBundle sourcePackage() {
        TsukuyomiPackageBundle bundle = new TsukuyomiPackageBundle();
        bundle.setMekaGroupGrp(mekaGroup("TSUKUYOMI"));
        bundle.setWazaGroupGrp(wazaGroup(
                waza("TSUKUYOMI", "Tsukuyomi", 2),
                waza("AUX", "Aux", 3)
        ));
        bundle.setSpriteGroupGrp(spriteGroup("TSUKUYOMI", "tsukuyomi.spm"));
        bundle.setBatVoiceGrp(batVoiceGroup("TSUKUYOMI"));
        bundle.setSeGroupGrp(new SeGroupGrp());
        bundle.getWazByFileName().put("tsukuyomi.waz", waz("main0", "main1"));
        bundle.getWazByFileName().put("aux.waz", waz("aux0", "aux1", "aux2"));
        return bundle;
    }

    private TsukuyomiBsdxBaselineBundle baselineWithExistingAuxWaz() {
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.setMekaGroupGrp(new MekaGroupGrp());
        baseline.setWazaGroupGrp(wazaGroup(waza("AUX", "Aux", 99)));
        baseline.setSpriteGroupGrp(new SpriteGroupGrp());
        baseline.setBatVoiceGrp(new BatVoiceGrp());
        baseline.setSeGroupGrp(new SeGroupGrp());
        baseline.getWazByFileName().put("aux.waz", waz("baseline0", "baseline1", "baseline2", "baseline3"));
        return baseline;
    }

    private MekaGroupGrp mekaGroup(String codeName) {
        MekaGroupGrp group = new MekaGroupGrp();
        MekaGroupGrp.MekaGroup entry = new MekaGroupGrp.MekaGroup();
        entry.setExistFlag(1);
        entry.setMekaCodeName(codeName);
        entry.setMekaName(codeName);
        group.getMekaList().add(entry);
        return group;
    }

    private WazaGroupGrp wazaGroup(WazaGroupGrp.WazaGroupEntry... entries) {
        WazaGroupGrp group = new WazaGroupGrp();
        for (WazaGroupGrp.WazaGroupEntry entry : entries) {
            group.getWazaList().add(entry);
        }
        return group;
    }

    private WazaGroupGrp.WazaGroupEntry waza(String codeName, String displayName, int param) {
        WazaGroupGrp.WazaGroupEntry entry = new WazaGroupGrp.WazaGroupEntry();
        entry.setExistFlag(1);
        entry.setWazaName(displayName);
        entry.setWazaCodeName(codeName);
        entry.setWazaDisplayName(displayName);
        entry.setParam(param);
        return entry;
    }

    private SpriteGroupGrp spriteGroup(String codeName, String fileName) {
        SpriteGroupGrp group = new SpriteGroupGrp();
        SpriteGroupGrp.SpriteGroupEntry entry = new SpriteGroupGrp.SpriteGroupEntry();
        entry.setExistFlag(1);
        entry.setSpriteCodeName(codeName);
        entry.setSpriteFileName(fileName);
        entry.setParam(0);
        group.getSpriteList().add(entry);
        return group;
    }

    private BatVoiceGrp batVoiceGroup(String codeName) {
        BatVoiceGrp group = new BatVoiceGrp();
        BatVoiceGrp.BatVoiceGroup entry = new BatVoiceGrp.BatVoiceGroup();
        entry.setExistFlag(1);
        entry.setCharacterCodeName(codeName);
        entry.setCharacterName(codeName);
        group.getVoiceList().add(entry);
        return group;
    }

    private Waz waz(String... skillNames) {
        Waz waz = new Waz();
        for (String skillName : skillNames) {
            Waz.Skill skill = new Waz.Skill();
            skill.setSkillNameEnglish(skillName);
            skill.setSkillNameJapanese(skillName);
            waz.getSkillList().add(skill);
        }
        return waz;
    }
}
