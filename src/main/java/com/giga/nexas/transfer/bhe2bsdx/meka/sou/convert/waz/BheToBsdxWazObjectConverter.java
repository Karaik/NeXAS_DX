package com.giga.nexas.transfer.bhe2bsdx.meka.sou.convert.waz;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventBlink;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventBlur;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventCamera;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventCharge;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventCpuButton;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEscape;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHeight;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventHit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventMove;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventRadialLine;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventScreenAttr;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventScreenEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventScreenLine;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventScreenYure;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSlipHosei;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSpriteAttr;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSpriteYure;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventStatus;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTouch;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVal;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.util.InfoCollectionMapper;


class BheToBsdxWazObjectConverter {

    SkillInfoObject convert(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject source,
            int sourceSlot,
            int targetSlot
    ) {
        if (source == null) {
            return null;
        }
        if (sourceSlot == 37
                && source instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventFreeParam freeParam) {
            return convertFreeParam(freeParam, source, targetSlot);
        }

        SkillInfoObject target = SkillInfoFactory.createEventObjectBsdx(targetSlot);
        Integer correctBsdxTypeId = target.getTypeId();
        copyAndTrans(source, target);
        InfoCollectionMapper.copyBheToBsdx(source, target);

        target.setSlotNum(targetSlot);
        target.typeId = correctBsdxTypeId;
        normalizeFrames(target, source);
        return target;
    }

    private SkillInfoObject convertFreeParam(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventFreeParam sourceFreeParam,
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject source,
            int targetSlot
    ) {
        SkillInfoObject target = SkillInfoFactory.createEventObjectBsdx(targetSlot);
        if (!(target instanceof CEventVal eventVal)) {
            throw new OperationException(500, "CEventFreeParam 目标槽位不是 CEventVal: " + targetSlot);
        }

        boolean foundBufferZero = false;
        eventVal.setInt1(0);
        eventVal.setInt2(0);
        eventVal.setInt3(0);
        eventVal.setInt4(0);
        for (var unit : sourceFreeParam.getUnitList()) {
            if (unit.getBuffer() == 0) {
                foundBufferZero = true;
                if (unit.getData() instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal value) {
                    eventVal.setInt1(value.getInt1() == null ? 0 : value.getInt1());
                    eventVal.setInt2(value.getInt2() == null ? 0 : value.getInt2());
                    eventVal.setInt3(value.getInt3() == null ? 0 : value.getInt3());
                    eventVal.setInt4(value.getInt4() == null ? 0 : value.getInt4());
                }
                break;
            }
        }
        if (!foundBufferZero) {
            throw new IllegalStateException("CEventFreeParam 缺少 buffer==0 的 CEventVal 数据");
        }

        eventVal.setSlotNum(targetSlot);
        eventVal.setStartFrame(source.getStartFrame());
        eventVal.setEndFrame(source.getEndFrame());
        normalizeFrames(eventVal, source);
        return eventVal;
    }

    private void copyAndTrans(
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject source,
            SkillInfoObject target
    ) {
        if (target instanceof CEventSpriteAttr event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventCpuButton event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventCpuButtonToBsdx(source, event);
        } else if (target instanceof CEventVoice event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventRadialLine event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventRadialLineToBsdx(source, event);
        } else if (target instanceof CEventValRandom event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventMove event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventSe event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventTouch event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventEffect event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventEffectToBsdx(source, event);
        } else if (target instanceof CEventScreenLine event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventScreenLineToBsdx(source, event);
        } else if (target instanceof CEventSlipHosei event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventVal event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventBlur event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventCharge event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventChargeToBsdx(source, event);
        } else if (target instanceof CEventScreenAttr event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventTerm event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventEscape event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventEscapeToBsdx(source, event);
        } else if (target instanceof CEventScreenEffect event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventScreenEffectToBsdx(source, event);
        } else if (target instanceof CEventHit event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventHitToBsdx(source, event);
        } else if (target instanceof CEventStatus event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventStatusToBsdx(source, event);
        } else if (target instanceof CEventChange event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventSprite event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventHeight event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventHeightToBsdx(source, event);
        } else if (target instanceof CEventCamera event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventCameraToBsdx(source, event);
        } else if (target instanceof CEventScreenYure event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventSpriteYure event) {
            BeanUtil.copyProperties(source, event);
        } else if (target instanceof CEventBlink event) {
            BeanUtil.copyProperties(source, event);
            event.transBheCEventBlinkToBsdx(source, event);
        } else {
            throw new OperationException(500, "未支持的 BSDX WAZ 事件对象: " + target.getClass().getName());
        }
    }

    private void normalizeFrames(
            SkillInfoObject target,
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject source
    ) {
        if (target.getStartFrame() == null) {
            target.setStartFrame(source != null && source.getStartFrame() != null ? source.getStartFrame() : 0);
        }
        if (target.getEndFrame() == null) {
            target.setEndFrame(source != null && source.getEndFrame() != null ? source.getEndFrame() : 0);
        }
    }
}
