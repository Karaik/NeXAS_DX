package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor;


import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.SkillInfoTypeEntry;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description WazInfoFactory
 * 逆向所得
 */
public class SkillInfoFactory {

    public static final SkillInfoTypeEntry[] SKILL_INFO_TYPE_ENTRIES = {
            new SkillInfoTypeEntry(0x0D, 0x00000000, "ｽﾌﾟﾗｲﾄ"), //0
            new SkillInfoTypeEntry(0x02, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小"), //1
            new SkillInfoTypeEntry(0x02, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小X"), //2
            new SkillInfoTypeEntry(0x02, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小Y"), //3
            new SkillInfoTypeEntry(0x02, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小Y(ｽﾛｰ影響)"), //4
            new SkillInfoTypeEntry(0x02, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小(ﾍﾞｰｽ)"), //5
            new SkillInfoTypeEntry(0x0E, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：振動"), //6
            new SkillInfoTypeEntry(0x0F, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：属性"), //7
            new SkillInfoTypeEntry(0x09, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：方向"), //8
            new SkillInfoTypeEntry(0x03, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：方向補正"), //9
            new SkillInfoTypeEntry(0x03, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：方向増分"), //10
            new SkillInfoTypeEntry(0x05, 0xE6821E, "位置"), //11
            new SkillInfoTypeEntry(0x06, 0xE6821E, "位置：無効条件"), //12
            new SkillInfoTypeEntry(0x02, 0xE6821E, "位置：分散率"), //13
            new SkillInfoTypeEntry(0x07, 0xE6821E, "座標移動"), //14
            new SkillInfoTypeEntry(0x0A, 0xD0B010, "ﾍﾞｸﾄﾙ：方向"), //15
            new SkillInfoTypeEntry(0x03, 0xD0B010, "ﾍﾞｸﾄﾙ：方向補正"), //16
            new SkillInfoTypeEntry(0x03, 0xD0B010, "ﾍﾞｸﾄﾙ：方向増分"), //17
            new SkillInfoTypeEntry(0x03, 0xD0B010, "ﾍﾞｸﾄﾙ：速度"), //18
            new SkillInfoTypeEntry(0x03, 0xD0B010, "ﾍﾞｸﾄﾙ：ジャンプ力"), //19
            new SkillInfoTypeEntry(0x08, 0xD0B010, "ﾍﾞｸﾄﾙ：追尾ジャンプ力"), //20
            new SkillInfoTypeEntry(0x02, 0xD0B010, "ﾍﾞｸﾄﾙ：重力"), //21
            new SkillInfoTypeEntry(0x02, 0xD0B010, "ﾍﾞｸﾄﾙ：最低高度"), //22
            new SkillInfoTypeEntry(0x02, 0xD0B010, "ﾍﾞｸﾄﾙ：慣性"), //23
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：移動"), //24
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：技"), //25
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：のけぞり"), //26
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：重力"), //27
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：ジャンプ力"), //28
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：浮遊"), //29
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：ｻｰﾁﾀﾞｯｼｭ"), //30
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：慣性"), //31
            new SkillInfoTypeEntry(0x00, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：押し戻し"), //32
            new SkillInfoTypeEntry(0x0C, 0xAA6400, "技後の慣性補正"), //33
            new SkillInfoTypeEntry(0x00, 0xDCDCFF, "耐久力"), //34
            new SkillInfoTypeEntry(0x00, 0xC86432, "汎用変数"), //35
            new SkillInfoTypeEntry(0x00, 0xE0E0E0, "装甲"), //36
            new SkillInfoTypeEntry(0x00, 0xE0E0E0, "装甲ﾋｯﾄｽﾄｯﾌﾟ補正率"), //37
            new SkillInfoTypeEntry(0x00, 0xE0E0E0, "無敵"), //38
            new SkillInfoTypeEntry(0x00, 0xC80000, "弱点"), //39
            new SkillInfoTypeEntry(0x00, 0xC84000, "熱量"), //40
            new SkillInfoTypeEntry(0x10, 0xE61E1E, "攻撃：メカ"), //41
            new SkillInfoTypeEntry(0x10, 0xE61E1E, "攻撃：弾"), //42
            new SkillInfoTypeEntry(0x00, 0xE61E1E, "攻撃判定拡大縮小反映率"), //43
            new SkillInfoTypeEntry(0x23, 0x821E1E, "パラメータ変動"), //44
            new SkillInfoTypeEntry(0x11, 0x1E1EE0, "ＣＰＵ回避"), //45
            new SkillInfoTypeEntry(0x12, 0x1E1EE0, "ＣＰＵボタン入力"), //46
            new SkillInfoTypeEntry(0x00, 0xE61E1E, "ｷｬﾝｾﾙﾌﾗｸﾞ"), //47
            new SkillInfoTypeEntry(0x13, 0x1EC8E6, "エフェクト"), //48
            new SkillInfoTypeEntry(0x14, 0x1EC8E6, "残像"), //49
            new SkillInfoTypeEntry(0x15, 0xC80000, "溜め"), //50
            new SkillInfoTypeEntry(0x16, 0xE6821E, "接触"), //51
            new SkillInfoTypeEntry(0x17, 0xD0B010, "ＳＥ"), //52
            new SkillInfoTypeEntry(0x18, 0xD0B010, "ボイス"), //53
            new SkillInfoTypeEntry(0x00, 0x606060, "影の濃さ"), //54
            new SkillInfoTypeEntry(0x00, 0xC86432, "優先順位補正"), //55
            new SkillInfoTypeEntry(0x00, 0x82E61E, "ﾌｫｰｽﾌｨｰﾄﾞﾊﾞｯｸ"), //56
            new SkillInfoTypeEntry(0x00, 0x82E61E, "ﾚｰｻﾞｰ：振幅"), //57
            new SkillInfoTypeEntry(0x00, 0x82E61E, "ﾚｰｻﾞｰ：周期"), //58
            new SkillInfoTypeEntry(0x02, 0x82E61E, "画面演出：拡大縮小"), //59
            new SkillInfoTypeEntry(0x19, 0x82E61E, "画面演出：振動"), //60
            new SkillInfoTypeEntry(0x1A, 0x82E61E, "画面演出：属性"), //61
            new SkillInfoTypeEntry(0x1B, 0x82E61E, "画面演出：エフェクト"), //62
            new SkillInfoTypeEntry(0x1C, 0x82E61E, "画面演出：効果線"), //63
            new SkillInfoTypeEntry(0x1D, 0x82E61E, "画面演出：集中線"), //64
            new SkillInfoTypeEntry(0x1E, 0x82E61E, "画面演出：ブラー"), //65
            new SkillInfoTypeEntry(0x1F, 0x82E61E, "画面演出：カメラ"), //66
            new SkillInfoTypeEntry(0x00, 0x00000000, "攻撃方向"), // 67
            new SkillInfoTypeEntry(0x00, 0x82E61E, "画面停止"), // 68
            new SkillInfoTypeEntry(0x00, 0x82E61E, "スロー効果"), // 69
            new SkillInfoTypeEntry(0x00, 0x82E61E, "スロー反映率"), // 70
            new SkillInfoTypeEntry(0x22, 0x82E61E, "変更処理") // 71
    };

    public static SkillInfoObject createCEventObjectByType(int type) {
        return switch (type) {
            case 0x0 -> new CEventVal(); // 0
            case 0x1 -> new CEventVal(); // 1
            case 0x2 -> new CEventValRandom(); // 2
            case 0x3 -> new CEventValRandom(); // 3
            case 0x4 -> new CEventWazaSelect(); // 4
            case 0x5 -> new CEventTerm(); // 5
            case 0x6 -> new CEventTerm(); // 6
            case 0x7 -> new CEventMove(); // 7
            case 0x8 -> new CEventHeight(); // 8
            case 0x9 -> new CEventTerm(); // 9
            case 0xA -> new CEventTerm(); // 10
            case 0xB -> new CEventTerm(); // 11
            case 0xC -> new CEventSlipHosei(); // 12
            case 0xD -> new CEventSprite(); // 13
            case 0xE -> new CEventSpriteYure(); // 14
            case 0xF -> new CEventSpriteAttr(); // 15
            case 0x10 -> new CEventHit(); // 16
            case 0x11 -> new CEventEscape(); // 17
            case 0x12 -> new CEventCpuButton(); // 18
            case 0x13 -> new CEventEffect(); // 19
            case 0x14 -> new CEventBlink(); // 20
            case 0x15 -> new CEventCharge(); // 21
            case 0x16 -> new CEventTouch(); // 22
            case 0x17 -> new CEventSe(); // 23
            case 0x18 -> new CEventVoice(); // 24
            case 0x19 -> new CEventScreenYure(); // 25
            case 0x1A -> new CEventScreenAttr(); // 26
            case 0x1B -> new CEventScreenEffect(); // 27
            case 0x1C -> new CEventScreenLine(); // 28
            case 0x1D -> new CEventRadialLine(); // 29
            case 0x1E -> new CEventBlur(); // 30
            case 0x1F -> new CEventCamera(); // 31
            case 0x20 -> new CEventTerm(); // 32
            case 0x21 -> new CEventNokezori(); // 33
            case 0x22 -> new CEventChange(); // 34
            case 0x23 -> new CEventStatus(); // 35
            case 0xFFFFFFFF -> new CEventTerm(); // ????
            default -> throw new IllegalArgumentException("Invalid event type");
        };
    }

    public static SkillInfoObject createEventObject(int sequence) {
        return switch (sequence) {
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
            case 0xFF -> new SkillInfoUnknown();
            default -> throw new IllegalArgumentException("Invalid event type");
        };
    }

}
