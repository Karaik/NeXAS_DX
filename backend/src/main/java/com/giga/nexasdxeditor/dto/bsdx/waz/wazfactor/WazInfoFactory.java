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
            case 0x0 -> new CEventSprite(); // 0
            case 0x1 -> new CEventValRandom(); // 1
            case 0x2 -> new CEventValRandom(); // 2
            case 0x3 -> new CEventValRandom(); // 3
            case 0x4 -> new CEventValRandom(); // 4
            case 0x5 -> new CEventValRandom(); // 5
            case 0x6 -> new CEventSpriteYure(); // 6
            case 0x7 -> new CEventSpriteAttr(); // 7
            case 0x8 -> new CEventTerm(); // 8
            case 0x9 -> new CEventValRandom(); // 9
            case 0xA -> new CEventValRandom(); // 10
            case 0xB -> new CEventTerm(); // 11
            case 0xC -> new CEventTerm(); // 12
            case 0xD -> new CEventValRandom(); // 13
            case 0xE -> new CEventMove(); // 14
            case 0xF -> new CEventTerm(); // 15
            case 0x10 -> new CEventValRandom(); // 16
            case 0x11 -> new CEventValRandom(); // 17
            case 0x12 -> new CEventValRandom(); // 18
            case 0x13 -> new CEventValRandom(); // 19
            case 0x14 -> new CEventHeight(); // 20
            case 0x15 -> new CEventValRandom(); // 21
            case 0x16 -> new CEventValRandom(); // 22
            case 0x17 -> new CEventValRandom(); // 23
            case 0x18 -> new CEventVal(); // 24
            case 0x19 -> new CEventVal(); // 25
            case 0x1A -> new CEventVal(); // 26
            case 0x1B -> new CEventVal(); // 27
            case 0x1C -> new CEventVal(); // 28
            case 0x1D -> new CEventVal(); // 29
            case 0x1E -> new CEventVal(); // 30
            case 0x1F -> new CEventVal(); // 31
            case 0x20 -> new CEventVal(); // 32
            case 0x21 -> new CEventSlipHosei(); // 33
            case 0x22 -> new CEventVal(); // 34
            case 0x23 -> new CEventVal(); // 35
            case 0x24 -> new CEventVal(); // 36
            case 0x25 -> new CEventVal(); // 37
            case 0x26 -> new CEventVal(); // 38
            case 0x27 -> new CEventVal(); // 39
            case 0x28 -> new CEventVal(); // 40
            case 0x29 -> new CEventHit(); // 41
            case 0x2A -> new CEventHit(); // 42
            case 0x2B -> new CEventVal(); // 43
            case 0x2C -> new CEventStatus(); // 44
            case 0x2D -> new CEventEscape(); // 45
            case 0x2E -> new CEventCpuButton(); // 46
            case 0x2F -> new CEventVal(); // 47
            case 0x30 -> new CEventEffect(); // 48
            case 0x31 -> new CEventBlink(); // 49
            case 0x32 -> new CEventCharge(); // 50
            case 0x33 -> new CEventTouch(); // 51
            case 0x34 -> new CEventSe(); // 52
            case 0x35 -> new CEventVoice(); // 53
            case 0x36 -> new CEventVal(); // 54
            case 0x37 -> new CEventVal(); // 55
            case 0x38 -> new CEventVal(); // 56
            case 0x39 -> new CEventVal(); // 57
            case 0x3A -> new CEventVal(); // 58
            case 0x3B -> new CEventValRandom(); // 59
            case 0x3C -> new CEventScreenYure(); // 60
            case 0x3D -> new CEventScreenAttr(); // 61
            case 0x3E -> new CEventScreenEffect(); // 62
            case 0x3F -> new CEventScreenLine(); // 63
            case 0x40 -> new CEventRadialLine(); // 64
            case 0x41 -> new CEventBlur(); // 65
            case 0x42 -> new CEventCamera(); // 66
            case 0x43 -> new CEventVal(); // 67
            case 0x44 -> new CEventVal(); // 68
            case 0x45 -> new CEventVal(); // 69
            case 0x46 -> new CEventVal(); // 70
            case 0x47 -> new CEventChange(); // 71
            case 0xFF -> new WazInfoUnknown(); //
            default -> throw new IllegalArgumentException("Invalid event type");
        };
    }
}
