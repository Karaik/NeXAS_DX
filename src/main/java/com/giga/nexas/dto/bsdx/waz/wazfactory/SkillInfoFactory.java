package com.giga.nexas.dto.bsdx.waz.wazfactory;


import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.*;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.exception.BusinessException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description WazInfoFactory
 * 逆向所得
 */
public class SkillInfoFactory {

    public static final SkillInfoTypeEntry[] SKILL_INFO_TYPE_ENTRIES_BSDX = {
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

    public static SkillInfoObject createCEventObjectByTypeBsdx(int typeId) {
        return switch (typeId) {
            case 0x0 -> new CEventVal(typeId); // 0
            case 0x1 -> new CEventVal(typeId); // 1
            case 0x2 -> new CEventValRandom(typeId); // 2
            case 0x3 -> new CEventValRandom(typeId); // 3
            case 0x4 -> new CEventWazaSelect(typeId); // 4
            case 0x5 -> new CEventTerm(typeId); // 5
            case 0x6 -> new CEventTerm(typeId); // 6
            case 0x7 -> new CEventMove(typeId); // 7
            case 0x8 -> new CEventHeight(typeId); // 8
            case 0x9 -> new CEventTerm(typeId); // 9
            case 0xA -> new CEventTerm(typeId); // 10
            case 0xB -> new CEventTerm(typeId); // 11
            case 0xC -> new CEventSlipHosei(typeId); // 12
            case 0xD -> new CEventSprite(typeId); // 13
            case 0xE -> new CEventSpriteYure(typeId); // 14
            case 0xF -> new CEventSpriteAttr(typeId); // 15
            case 0x10 -> new CEventHit(typeId); // 16
            case 0x11 -> new CEventEscape(typeId); // 17
            case 0x12 -> new CEventCpuButton(typeId); // 18
            case 0x13 -> new CEventEffect(typeId); // 19
            case 0x14 -> new CEventBlink(typeId); // 20
            case 0x15 -> new CEventCharge(typeId); // 21
            case 0x16 -> new CEventTouch(typeId); // 22
            case 0x17 -> new CEventSe(typeId); // 23
            case 0x18 -> new CEventVoice(typeId); // 24
            case 0x19 -> new CEventScreenYure(typeId); // 25
            case 0x1A -> new CEventScreenAttr(typeId); // 26
            case 0x1B -> new CEventScreenEffect(typeId); // 27
            case 0x1C -> new CEventScreenLine(typeId); // 28
            case 0x1D -> new CEventRadialLine(typeId); // 29
            case 0x1E -> new CEventBlur(typeId); // 30
            case 0x1F -> new CEventCamera(typeId); // 31
            case 0x20 -> new CEventTerm(typeId); // 32
            case 0x21 -> new CEventNokezori(typeId); // 33
            case 0x22 -> new CEventChange(typeId); // 34
            case 0x23 -> new CEventStatus(typeId); // 35
            case 0xFFFFFFFF -> new CEventTerm(typeId); // ????
            default -> throw new BusinessException(500, "未曾设想的类型："+typeId);
        };
    }

    public static SkillInfoObject createEventObjectBsdx(int sequence) {

        if (sequence == 0xFF) {
            return new SkillInfoUnknown(sequence);
        }

        if (sequence < 0 || sequence >= SKILL_INFO_TYPE_ENTRIES_BSDX.length) {
            throw new BusinessException(500, "该位置waz信息读取错误（请检查waz文件来源）： " + sequence);
        }

        int typeId = SKILL_INFO_TYPE_ENTRIES_BSDX[sequence].getType();
        SkillInfoObject obj = createCEventObjectByTypeBsdx(typeId);

        obj.setSlotNum(sequence);
        return obj;
    }



}
