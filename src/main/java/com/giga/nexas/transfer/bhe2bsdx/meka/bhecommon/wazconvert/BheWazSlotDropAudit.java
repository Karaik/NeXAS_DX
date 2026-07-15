package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert;

import com.giga.nexas.dto.bhe.waz.Waz;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** 集中扫描 BHE WAZ 中已确认无法由 BSDX 承载的目标选择结构。 */
public final class BheWazSlotDropAudit {

    public static final int TARGET_SOURCE_SLOT = 35;
    public static final int CEVENT_EFFECT_TARGET_SOURCE_SLOT = 28;
    public static final Set<Integer> MULTILOCK_SOURCE_SLOTS = Set.of(66, 67, 68, 69);

    private final List<BheWazSlotDropRecord> drops = new ArrayList<>();

    public static BheWazSlotDropAudit scan(Waz source) {
        BheWazSlotDropAudit audit = new BheWazSlotDropAudit();
        if (source == null) {
            return audit;
        }
        for (int skillIndex = 0; skillIndex < source.getSkillList().size(); skillIndex++) {
            Waz.Skill skill = source.getSkillList().get(skillIndex);
            for (int phaseIndex = 0; phaseIndex < skill.getPhasesInfo().size(); phaseIndex++) {
                Waz.Skill.SkillPhase phase = skill.getPhasesInfo().get(phaseIndex);
                for (int unitIndex = 0; unitIndex < phase.getSkillUnitCollection().size(); unitIndex++) {
                    audit.scanUnit(
                            source.getFileName(),
                            skillIndex,
                            phaseIndex,
                            unitIndex,
                            phase.getSkillUnitCollection().get(unitIndex)
                    );
                }
            }
        }
        return audit;
    }

    private void scanUnit(
            String fileName,
            int skillIndex,
            int phaseIndex,
            int unitIndex,
            SkillUnit unit
    ) {
        int sourceSlot = unit.getUnitQuantity();
        if (sourceSlot == TARGET_SOURCE_SLOT || MULTILOCK_SOURCE_SLOTS.contains(sourceSlot)) {
            drops.add(new BheWazSlotDropRecord(fileName, skillIndex, phaseIndex, unitIndex, sourceSlot));
        }
        for (int objectIndex = 0; objectIndex < unit.getSkillInfoObjectList().size(); objectIndex++) {
            if (unit.getSkillInfoObjectList().get(objectIndex) instanceof CEventEffect effect) {
                scanEffect(fileName, skillIndex, phaseIndex, unitIndex, sourceSlot, objectIndex, effect);
            }
        }
    }

    private void scanEffect(
            String fileName,
            int skillIndex,
            int phaseIndex,
            int unitIndex,
            int sourceSlot,
            int objectIndex,
            CEventEffect effect
    ) {
        for (CEventEffect.CEventEffectUnit nested : effect.getCeventEffectUnitList()) {
            if (nested.getUnitSlotNum() == CEVENT_EFFECT_TARGET_SOURCE_SLOT
                    && nested.getBuffer() != 0) {
                drops.add(new BheWazSlotDropRecord(
                        fileName,
                        skillIndex,
                        phaseIndex,
                        unitIndex,
                        sourceSlot,
                        objectIndex,
                        CEVENT_EFFECT_TARGET_SOURCE_SLOT
                ));
            }
        }
    }

    public List<BheWazSlotDropRecord> getDrops() {
        return List.copyOf(drops);
    }

    public List<BheWazSlotDropRecord> multiLockDrops() {
        return drops.stream()
                .filter(drop -> MULTILOCK_SOURCE_SLOTS.contains(drop.sourceSlot()))
                .toList();
    }

    public List<BheWazSlotDropRecord> nestedCEventEffectTargetDrops() {
        return drops.stream()
                .filter(drop -> Integer.valueOf(CEVENT_EFFECT_TARGET_SOURCE_SLOT).equals(drop.nestedSourceSlot()))
                .toList();
    }
}
