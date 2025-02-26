package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor;


import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.WazInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description WazInfoFactory
 * 逆向所得
 */
public class WazInfoFactory {

    public static WazInfoObject createEventObject(int type) {
        return switch (type) {
            case 0x0 -> new CEventSprite();
            case 0x1 -> new CEventValRandom();
            case 0x2 -> new CEventValRandom();
            case 0x3 -> new CEventValRandom();
            case 0x4 -> new CEventValRandom();
            case 0x5 -> new CEventValRandom();
            case 0x6 -> new CEventSpriteYure();
            case 0x7 -> new CEventSpriteAttr();
            case 0x8 -> new CEventTerm();
            case 0x9 -> new CEventValRandom();
            case 0xA -> new CEventValRandom();
            case 0xB -> new CEventTerm();
            case 0xC -> new CEventTerm();
            case 0xD -> new CEventValRandom();
            case 0xE -> new CEventMove();
            case 0xF -> new CEventTerm();
            case 0x10 -> new CEventValRandom();
            case 0x11 -> new CEventValRandom();
            case 0x12 -> new CEventValRandom();
            case 0x13 -> new CEventValRandom();
            case 0x14 -> new CEventHeight();
            case 0x15 -> new CEventValRandom();
            case 0x16 -> new CEventValRandom();
            case 0x17 -> new CEventValRandom();
            case 0x18 -> new CEventVal();
            case 0x19 -> new CEventVal();
            case 0x1A -> new CEventVal();
            case 0x1B -> new CEventVal();
            case 0x1C -> new CEventVal();
            case 0x1D -> new CEventVal();
            case 0x1E -> new CEventVal();
            case 0x1F -> new CEventVal();
            case 0x20 -> new CEventVal();
            case 0x21 -> new CEventSlipHosei();
            case 0x22 -> new CEventVal();
            case 0x23 -> new CEventVal();
            case 0x24 -> new CEventVal();
            case 0x25 -> new CEventVal();
            case 0x26 -> new CEventVal();
            case 0x27 -> new CEventVal();
            case 0x28 -> new CEventVal();
            case 0x29 -> new CEventHit();
            case 0x2A -> new CEventHit();
            case 0x2B -> new CEventVal();
            case 0x2C -> new CEventStatus();
            case 0x2D -> new CEventEscape();
            case 0x2E -> new CEventCpuButton();
            case 0x2F -> new CEventVal();
            case 0x30 -> new CEventEffect();
            case 0x31 -> new CEventBlink();
            case 0x32 -> new CEventCharge();
            case 0x33 -> new CEventTouch();
            case 0x34 -> new CEventSe();
            case 0x35 -> new CEventVoice();
            case 0x36 -> new CEventVal();
            case 0x37 -> new CEventVal();
            case 0x38 -> new CEventVal();
            case 0x39 -> new CEventVal();
            case 0x3A -> new CEventVal();
            case 0x3B -> new CEventValRandom();
            case 0x3C -> new CEventScreenYure();
            case 0x3D -> new CEventScreenAttr();
            case 0x3E -> new CEventScreenEffect();
            case 0x3F -> new CEventScreenLine();
            case 0x40 -> new CEventRadialLine();
            case 0x41 -> new CEventBlur();
            case 0x42 -> new CEventCamera();
            case 0x43 -> new CEventVal();
            case 0x44 -> new CEventVal();
            case 0x45 -> new CEventVal();
            case 0x46 -> new CEventVal();
            case 0x47 -> new CEventChange();
            case 0xFF -> new WazInfoUnknown();
            default -> throw new IllegalArgumentException("Invalid event type");
        };
    }
}
