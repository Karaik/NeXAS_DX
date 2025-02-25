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
//            case 0x2D -> new WazInfoObject45();
//            case 0x2E -> new WazInfoObject46();
//            case 0x2F -> new WazInfoObject47();
//            case 0x30 -> new WazInfoObject48();
//            case 0x31 -> new WazInfoObject49();
//            case 0x32 -> new WazInfoObject50();
//            case 0x33 -> new WazInfoObject51();
//            case 0x34 -> new WazInfoObject52();
//            case 0x35 -> new WazInfoObject53();
//            case 0x36 -> new WazInfoObject54();
//            case 0x37 -> new WazInfoObject55();
//            case 0x38 -> new WazInfoObject56();
//            case 0x39 -> new WazInfoObject57();
//            case 0x3A -> new WazInfoObject58();
//            case 0x3B -> new WazInfoObject59();
//            case 0x3C -> new WazInfoObject60();
//            case 0x3D -> new WazInfoObject61();
//            case 0x3E -> new WazInfoObject62();
//            case 0x3F -> new WazInfoObject63();
//            case 0x40 -> new WazInfoObject64();
//            case 0x41 -> new WazInfoObject65();
//            case 0x42 -> new WazInfoObject66();
//            case 0x43 -> new WazInfoObject67();
//            case 0x44 -> new WazInfoObject68();
//            case 0x45 -> new WazInfoObject69();
//            case 0x46 -> new WazInfoObject70();
//            case 0x47 -> new WazInfoObject71();
            default -> throw new IllegalArgumentException("Invalid event type");
        };
    }
}
