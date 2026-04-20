package com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;

import java.util.ArrayList;
import java.util.List;


class BheToBsdxWazSkillConverter {

    private final BheToBsdxWazSlotMap slotMap = new BheToBsdxWazSlotMap();
    private final BheToBsdxWazObjectConverter objectConverter = new BheToBsdxWazObjectConverter();

    Waz.Skill convert(com.giga.nexas.dto.bhe.waz.Waz.Skill source) {
        Waz.Skill target = new Waz.Skill();
        if (source == null) {
            return target;
        }

        target.setSkillNameJapanese(source.getSkillNameJapanese());
        target.setSkillNameEnglish(source.getSkillNameEnglish());
        target.setSkillSuffixList(convertSuffixList(source.getSkillSuffixList()));

        List<Waz.Skill.SkillPhase> phases = new ArrayList<>();
        if (source.getPhasesInfo() != null) {
            for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase sourcePhase : source.getPhasesInfo()) {
                phases.add(convertPhase(sourcePhase));
            }
        }
        target.setPhaseQuantity(phases.size());
        target.setPhasesInfo(phases);
        return target;
    }

    private List<Waz.Skill.SkillSuffix> convertSuffixList(
            List<com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillSuffix> sourceList
    ) {
        List<Waz.Skill.SkillSuffix> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillSuffix source : sourceList) {
            Waz.Skill.SkillSuffix target = new Waz.Skill.SkillSuffix();
            if (source != null) {
                BeanUtil.copyProperties(source, target);
            }
            targetList.add(target);
        }
        return targetList;
    }

    private Waz.Skill.SkillPhase convertPhase(com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase source) {
        Waz.Skill.SkillPhase target = new Waz.Skill.SkillPhase();
        if (source == null || source.getSkillUnitCollection() == null) {
            return target;
        }

        List<SkillUnit> units = new ArrayList<>();
        for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit sourceUnit
                : source.getSkillUnitCollection()) {
            SkillUnit convertedUnit = convertUnit(sourceUnit);
            if (convertedUnit != null) {
                units.add(convertedUnit);
            }
        }
        target.setSkillUnitCollection(units);
        return target;
    }

    private SkillUnit convertUnit(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit source) {
        if (source == null) {
            return null;
        }

        Integer sourceSlot = source.getUnitQuantity();
        Integer targetSlot = slotMap.resolveTargetSlot(sourceSlot);
        if (targetSlot == null) {
            throw new IllegalStateException("BHE WAZ 事件槽位没有声明映射: " + sourceSlot);
        }
        if (targetSlot < 0) {
            return null;
        }

        SkillUnit target = new SkillUnit();
        target.setUnitQuantity(targetSlot);
        target.setSkillInfoObjectList(convertObjects(source, sourceSlot, targetSlot));
        target.setSkillInfoUnknownList(convertUnknownObjects(source, targetSlot));

        if (target.getSkillInfoObjectList().isEmpty() && target.getSkillInfoUnknownList().isEmpty()) {
            return null;
        }
        return target;
    }

    private List<SkillInfoObject> convertObjects(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit sourceUnit,
            int sourceSlot,
            int targetSlot
    ) {
        List<SkillInfoObject> targetList = new ArrayList<>();
        if (sourceUnit.getSkillInfoObjectList() == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject sourceObject
                : sourceUnit.getSkillInfoObjectList()) {
            SkillInfoObject targetObject = objectConverter.convert(sourceObject, sourceSlot, targetSlot);
            if (targetObject != null) {
                targetList.add(targetObject);
            }
        }
        return targetList;
    }

    private List<com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown> convertUnknownObjects(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit sourceUnit,
            int targetSlot
    ) {
        List<com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown> targetList =
                new ArrayList<>();
        if (sourceUnit.getSkillInfoUnknownList() == null) {
            return targetList;
        }
        for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown sourceUnknown
                : sourceUnit.getSkillInfoUnknownList()) {
            com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown targetUnknown =
                    new com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown(0xFF);
            BeanUtil.copyProperties(sourceUnknown, targetUnknown);
            targetUnknown.setSlotNum(targetSlot);
            targetList.add(targetUnknown);
        }
        return targetList;
    }
}
