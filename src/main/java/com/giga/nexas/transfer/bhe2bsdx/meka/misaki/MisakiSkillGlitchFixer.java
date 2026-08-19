package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;

import java.util.Locale;
import java.util.Map;

/**
 * Misaki 专属技能修复器
 *
 * <p>直接修正 Tama05.waz 中 Skill 488/336（床召喚）与 Skill 491/339（あたためる）中
 * CEventEffect Slot 4（同時発射数）异常的 int5=999 计数，将其规整为 1。</p>
 */
public final class MisakiSkillGlitchFixer {

    public static final MisakiSkillGlitchFixer INSTANCE = new MisakiSkillGlitchFixer();

    private MisakiSkillGlitchFixer() {
    }

    public void fix(FollowupMekaContext context) {
        if (context == null) {
            return;
        }

        // 1. 修复 selectedPackage 中的 tama05.waz（源辅助 WAZ，索引 336 与 339）
        if (context.getSelectedPackage() != null && context.getSelectedPackage().getWazByFileName() != null) {
            Waz packageTama05 = findWaz(context.getSelectedPackage().getWazByFileName(), "tama05.waz");
            if (packageTama05 != null) {
                fixSkillSlot4(packageTama05, 336);
                fixSkillSlot4(packageTama05, 339);
            }
        }

        // 2. 修复 baseline 中的 tama05.waz（重绑定后的基线 WAZ，索引 488 与 491）
        if (context.getBaseline() != null && context.getBaseline().getWazByFileName() != null) {
            Waz baselineTama05 = findWaz(context.getBaseline().getWazByFileName(), "tama05.waz");
            if (baselineTama05 != null) {
                fixSkillSlot4(baselineTama05, 488);
                fixSkillSlot4(baselineTama05, 491);
                fixSkillSlot4(baselineTama05, 336);
                fixSkillSlot4(baselineTama05, 339);
            }
        }
    }

    private void fixSkillSlot4(Waz waz, int skillIdx) {
        if (waz.getSkillList() == null || skillIdx < 0 || skillIdx >= waz.getSkillList().size()) {
            return;
        }
        Waz.Skill skill = waz.getSkillList().get(skillIdx);
        if (skill == null || skill.getPhasesInfo() == null) {
            return;
        }
        for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
            if (phase.getSkillUnitCollection() == null) {
                continue;
            }
            for (SkillUnit unit : phase.getSkillUnitCollection()) {
                if (unit.getSkillInfoObjectList() == null) {
                    continue;
                }
                for (SkillInfoObject obj : unit.getSkillInfoObjectList()) {
                    if (obj instanceof CEventEffect effect && effect.getCeventEffectUnitList() != null) {
                        for (CEventEffect.CEventEffectUnit effectUnit : effect.getCeventEffectUnitList()) {
                            if (effectUnit.getUnitSlotNum() == 4 && effectUnit.getData() instanceof CEventValRandom randomVal) {
                                if (randomVal.getInt5() != null && randomVal.getInt5() == 999) {
                                    randomVal.setInt5(1);
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Waz findWaz(Map<String, Waz> wazMap, String targetFileName) {
        if (wazMap == null || targetFileName == null) {
            return null;
        }
        for (Map.Entry<String, Waz> entry : wazMap.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().toLowerCase(Locale.ROOT).equals(targetFileName.toLowerCase(Locale.ROOT))) {
                return entry.getValue();
            }
        }
        return null;
    }
}
