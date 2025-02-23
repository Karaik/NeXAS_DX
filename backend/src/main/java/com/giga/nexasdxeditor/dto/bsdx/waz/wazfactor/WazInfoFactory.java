package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor;


import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description WazInfoFactory
 * 逆向所得
 */
public class WazInfoFactory {

    public static WazInfoObject createEventObject(int type) {
        switch (type) {
            case 0x0:
                return new CEventSprite();
            case 0x1:
                return new CEventValRandom();
            case 0x2:
                return new CEventValRandom();
            case 0x3:
                return new CEventValRandom();
            case 0x4:
                return new CEventValRandom();
            case 0x5:
                return new CEventValRandom();
            case 0x6:
                return new CEventSpriteYure();
            case 0x7:
                return new CEventSpriteAttr();
            case 0x8:
                return new CEventTerm();
            case 0x9:
                return new CEventValRandom();
            case 0xA:
                return new CEventValRandom();
            case 0xB:
                return new CEventTerm();
            case 0xC:
                return new CEventTerm();
            case 0xD:
                return new CEventValRandom();
            case 0xE:
                return new CEventMove();
            case 0xF:
                return new CEventTerm();
            case 0x10:
                return new CEventValRandom();
            case 0x11:
                return new CEventValRandom();
            case 0x12:
                return new CEventValRandom();
            case 0x13:
                return new CEventValRandom();
            case 0x14:
                return new CEventHeight();
            case 0x15:
                return new CEventValRandom();
            case 0x16:
                return new CEventValRandom();
            case 0x17:
                return new CEventValRandom();
            case 0x18:
                return new CEventVal();
            case 0x19:
                return new CEventVal();
            case 0x1A:
                return new CEventVal();
            case 0x1B:
                return new CEventVal();
            case 0x1C:
                return new CEventVal();
            case 0x1D:
                return new CEventVal();
            case 0x1E:
                return new CEventVal();
            case 0x1F:
                return new CEventVal();
            case 0x20:
                return new CEventVal();
            case 0x21:
                return new CEventSlipHosei();
            case 0x22:
                return new CEventVal();
            case 0x23:
                return new CEventVal();
            case 0x24:
                return new CEventVal();
            case 0x25:
                return new CEventVal();
            case 0x26:
                return new CEventVal();
            case 0x27:
                return new CEventVal();
            case 0x28:
                return new CEventVal();
            case 0x29:
                return new CEventHit();
            case 0x2A:
                return new CEventHit();
            case 0x2B:
                return new CEventVal();
            case 0x2C:
                return new CEventStatus();
//            case 0x2D:
//                return new WazInfoObject45();
//            case 0x2E:
//                return new WazInfoObject46();
//            case 0x2F:
//                return new WazInfoObject47();
//            case 0x30:
//                return new WazInfoObject48();
//            case 0x31:
//                return new WazInfoObject49();
//            case 0x32:
//                return new WazInfoObject50();
//            case 0x33:
//                return new WazInfoObject51();
//            case 0x34:
//                return new WazInfoObject52();
//            case 0x35:
//                return new WazInfoObject53();
//            case 0x36:
//                return new WazInfoObject54();
//            case 0x37:
//                return new WazInfoObject55();
//            case 0x38:
//                return new WazInfoObject56();
//            case 0x39:
//                return new WazInfoObject57();
//            case 0x3A:
//                return new WazInfoObject58();
//            case 0x3B:
//                return new WazInfoObject59();
//            case 0x3C:
//                return new WazInfoObject60();
//            case 0x3D:
//                return new WazInfoObject61();
//            case 0x3E:
//                return new WazInfoObject62();
//            case 0x3F:
//                return new WazInfoObject63();
//            case 0x40:
//                return new WazInfoObject64();
//            case 0x41:
//                return new WazInfoObject65();
//            case 0x42:
//                return new WazInfoObject66();
//            case 0x43:
//                return new WazInfoObject67();
//            case 0x44:
//                return new WazInfoObject68();
//            case 0x45:
//                return new WazInfoObject69();
//            case 0x46:
//                return new WazInfoObject70();
//            case 0x47:
//                return new WazInfoObject71();
            default:
                throw new IllegalArgumentException("Invalid event type");
        }
    }
}
