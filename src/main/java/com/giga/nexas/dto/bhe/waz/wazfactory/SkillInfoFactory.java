package com.giga.nexas.dto.bhe.waz.wazfactory;


import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillInfoTypeEntry;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.exception.BusinessException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description SkillInfoFactory
 */
public class SkillInfoFactory {

    public static final SkillInfoTypeEntry[] SKILL_INFO_TYPE_ENTRIES_BHE = {
            new SkillInfoTypeEntry(0x10, 0x00000000, "ｽﾌﾟﾗｲﾄ"), // 0
            new SkillInfoTypeEntry(0x02, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小"), // 1
            new SkillInfoTypeEntry(0x02, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小X"), // 2
            new SkillInfoTypeEntry(0x02, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小Y"), // 3
            new SkillInfoTypeEntry(0x02, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小Y(ｽﾛｰ影響)"), // 4
            new SkillInfoTypeEntry(0x02, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小(ﾍﾞｰｽ)"), // 5
            new SkillInfoTypeEntry(0x11, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：振動"), // 6
            new SkillInfoTypeEntry(0x12, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：属性"), // 7
            new SkillInfoTypeEntry(0x0B, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：方向"), // 8
            new SkillInfoTypeEntry(0x03, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：方向補正"), // 9
            new SkillInfoTypeEntry(0x03, 0x001E82E6, "ｽﾌﾟﾗｲﾄ：方向増分"), // 10
            new SkillInfoTypeEntry(0x07, 0x00E6821E, "位置"), // 11
            new SkillInfoTypeEntry(0x08, 0x00E6821E, "位置：無効条件"), // 12
            new SkillInfoTypeEntry(0x02, 0x00E6821E, "位置：分散率"), // 13
            new SkillInfoTypeEntry(0x09, 0x00E6821E, "座標移動"), // 14
            new SkillInfoTypeEntry(0x0C, 0x00D0B010, "ﾍﾞｸﾄﾙ：方向"), // 15
            new SkillInfoTypeEntry(0x03, 0x00D0B010, "ﾍﾞｸﾄﾙ：方向補正"), // 16
            new SkillInfoTypeEntry(0x03, 0x00D0B010, "ﾍﾞｸﾄﾙ：方向増分"), // 17
            new SkillInfoTypeEntry(0x05, 0x00D0B010, "ﾍﾞｸﾄﾙ：速度"), // 18
            new SkillInfoTypeEntry(0x05, 0x00D0B010, "ﾍﾞｸﾄﾙ：ジャンプ力"), // 19
            new SkillInfoTypeEntry(0x0A, 0x00D0B010, "ﾍﾞｸﾄﾙ：追尾ジャンプ力"), // 20
            new SkillInfoTypeEntry(0x04, 0x00D0B010, "ﾍﾞｸﾄﾙ：重力"), // 21
            new SkillInfoTypeEntry(0x02, 0x00D0B010, "ﾍﾞｸﾄﾙ：最低高度"), // 22
            new SkillInfoTypeEntry(0x05, 0x00D0B010, "ﾍﾞｸﾄﾙ：速度XYZ"), // 23
            new SkillInfoTypeEntry(0x02, 0x00D0B010, "ﾍﾞｸﾄﾙ：慣性"), // 24
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：移動"), // 25
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：技"), // 26
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：のけぞり"), // 27
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：重力"), // 28
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：ジャンプ力"), // 29
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：浮遊"), // 30
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：ｻｰﾁﾀﾞｯｼｭ"), // 31
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：慣性"), // 32
            new SkillInfoTypeEntry(0x00, 0x00AA6400, "ﾍﾞｸﾄﾙ補正率：押し戻し"), // 33
            new SkillInfoTypeEntry(0x0E, 0x00AA6400, "技後の慣性補正"), // 34
            new SkillInfoTypeEntry(0x07, 0x00AA6400, "標的"), // 35
            new SkillInfoTypeEntry(0x00, 0x00DCDCFF, "耐久力"), // 36
            new SkillInfoTypeEntry(0x0F, 0x00C86432, "汎用変数"), // 37
            new SkillInfoTypeEntry(0x2A, 0x00C86432, "技ツールパラメータ"), // 38
            new SkillInfoTypeEntry(0x00, 0x00E0E0E0, "装甲"), // 39
            new SkillInfoTypeEntry(0x00, 0x00E0E0E0, "ハイパーアーマー"), // 40
            new SkillInfoTypeEntry(0x00, 0x00E0E0E0, "装甲ﾋｯﾄｽﾄｯﾌﾟ補正率"), // 41
            new SkillInfoTypeEntry(0x00, 0x00E0E0E0, "無敵"), // 42
            new SkillInfoTypeEntry(0x00, 0x00E0E0E0, "死亡(自爆)"), // 43
            new SkillInfoTypeEntry(0x00, 0x00C80000, "弱点"), // 44
            new SkillInfoTypeEntry(0x00, 0x00C84000, "熱量"), // 45
            new SkillInfoTypeEntry(0x16, 0x00E61E1E, "攻撃：メカ"), // 46
            new SkillInfoTypeEntry(0x16, 0x00E61E1E, "攻撃：弾"), // 47
            new SkillInfoTypeEntry(0x00, 0x00E61E1E, "攻撃判定拡大縮小反映率"), // 48
            new SkillInfoTypeEntry(0x29, 0x00821E1E, "パラメータ変動"), // 49
            new SkillInfoTypeEntry(0x17, 0x001E1EE0, "ＣＰＵ回避"), // 50
            new SkillInfoTypeEntry(0x18, 0x001E1EE0, "ＣＰＵボタン入力"), // 51
            new SkillInfoTypeEntry(0x00, 0x001E1EE0, "ＣＰＵ特殊行動"), // 52
            new SkillInfoTypeEntry(0x00, 0x00000001, "ｷｬﾝｾﾙﾌﾗｸﾞ"), // 53
            new SkillInfoTypeEntry(0x19, 0x001EC8E6, "エフェクト"), // 54
            new SkillInfoTypeEntry(0x1A, 0x001EC8E6, "残像"), // 55
            new SkillInfoTypeEntry(0x1B, 0x00C80000, "溜め"), // 56
            new SkillInfoTypeEntry(0x1C, 0x00E6821E, "接触"), // 57
            new SkillInfoTypeEntry(0x1D, 0x00D0B010, "ＳＥ"), // 58
            new SkillInfoTypeEntry(0x1E, 0x00D0B010, "ボイス"), // 59
            new SkillInfoTypeEntry(0x00, 0x0060605E, "影の濃さ"), // 60
            new SkillInfoTypeEntry(0x00, 0x00C86432, "優先順位補正"), // 61
            new SkillInfoTypeEntry(0x2B, 0x00C8C832, "属性"), // 62
            new SkillInfoTypeEntry(0x00, 0x0082E61E, "ﾌｫｰｽﾌｨｰﾄﾞﾊﾞｯｸ"), // 63
            new SkillInfoTypeEntry(0x00, 0x0082E61E, "ﾚｰｻﾞｰ：振幅"), // 64
            new SkillInfoTypeEntry(0x00, 0x0082E61E, "ﾚｰｻﾞｰ：周期"), // 65
            new SkillInfoTypeEntry(0x13, 0x0080807C, "ﾏﾙﾁﾛｯｸ:ロック開始・終了"), // 66
            new SkillInfoTypeEntry(0x14, 0x0080807C, "ﾏﾙﾁﾛｯｸ:ロック"), // 67
            new SkillInfoTypeEntry(0x02, 0x0080807C, "ﾏﾙﾁﾛｯｸ:ターゲットカウンタ"), // 68
            new SkillInfoTypeEntry(0x15, 0x0080807C, "ﾏﾙﾁﾛｯｸ:描画フラグ"), // 69
            new SkillInfoTypeEntry(0x02, 0x0082E61E, "画面演出：拡大縮小"), // 70
            new SkillInfoTypeEntry(0x1F, 0x0082E61E, "画面演出：振動"), // 71
            new SkillInfoTypeEntry(0x20, 0x0082E61E, "画面演出：属性"), // 72
            new SkillInfoTypeEntry(0x21, 0x0082E61E, "画面演出：エフェクト"), // 73
            new SkillInfoTypeEntry(0x22, 0x0082E61E, "画面演出：効果線"), // 74
            new SkillInfoTypeEntry(0x23, 0x0082E61E, "画面演出：集中線"), // 75
            new SkillInfoTypeEntry(0x24, 0x0082E61E, "画面演出：ブラー"), // 76
            new SkillInfoTypeEntry(0x25, 0x0082E61E, "画面演出：カメラ"), // 77
            new SkillInfoTypeEntry(0x00, 0x0082E61E, "攻撃方向"), // 78
            new SkillInfoTypeEntry(0x00, 0x0082E61E, "画面停止"), // 79
            new SkillInfoTypeEntry(0x00, 0x0082E61E, "スロー効果"), // 80
            new SkillInfoTypeEntry(0x00, 0x0082E61E, "スロー反映率"), // 81
            new SkillInfoTypeEntry(0x28, 0x00F8F8F8, "変更処理") // 82
    };

    public static SkillInfoObject createCEventObjectByTypeBhe(int typeId) {
        return switch (typeId) {
            case 0x00 -> new CEventVal(typeId); // 0
            case 0x01 -> new CEventVal(typeId); // 1
            case 0x02 -> new CEventValRandom(typeId); // 2
            case 0x03 -> new CEventValRandom(typeId); // 3
            case 0x04 -> new CEventValRandom(typeId); // 4
            case 0x05 -> new CEventValRandom(typeId); // 5
            case 0x06 -> new CEventWazaSelect(typeId); // 6
            case 0x07 -> new CEventTerm(typeId); // 7
            case 0x08 -> new CEventTerm(typeId); // 8
            case 0x09 -> new CEventMove(typeId); // 9
            case 0x0A -> new CEventHeight(typeId); // 10
            case 0x0B -> new CEventTerm(typeId); // 11
            case 0x0C -> new CEventTerm(typeId); // 12
            case 0x0D -> new CEventTerm(typeId); // 13
            case 0x0E -> new CEventSlipHosei(typeId); // 14
            case 0x0F -> new CEventFreeParam(typeId); // 15 add
            case 0x10 -> new CEventSprite(typeId); // 16
            case 0x11 -> new CEventSpriteYure(typeId); // 17
            case 0x12 -> new CEventSpriteAttr(typeId); // 18
            case 0x13 -> new CEventMultiLockStartEnd(typeId); // 19 add
            case 0x14 -> new CEventMultiLock(typeId); // 20 add
            case 0x15 -> new CEventMultiLockDraw(typeId); // 21 add
            case 0x16 -> new CEventHit(typeId); // 22
            case 0x17 -> new CEventEscape(typeId); // 23
            case 0x18 -> new CEventCpuButton(typeId); // 24
            case 0x19 -> new CEventEffect(typeId); // 25
            case 0x1A -> new CEventBlink(typeId); // 26
            case 0x1B -> new CEventCharge(typeId); // 27
            case 0x1C -> new CEventTouch(typeId); // 28
            case 0x1D -> new CEventSe(typeId); // 29
            case 0x1E -> new CEventVoice(typeId); // 30
            case 0x1F -> new CEventScreenYure(typeId); // 31
            case 0x20 -> new CEventScreenAttr(typeId); // 32
            case 0x21 -> new CEventScreenEffect(typeId); // 33
            case 0x22 -> new CEventScreenLine(typeId); // 34
            case 0x23 -> new CEventRadialLine(typeId); // 35
            case 0x24 -> new CEventBlur(typeId); // 36
            case 0x25 -> new CEventCamera(typeId); // 37
            case 0x26 -> new CEventTerm(typeId); // 38
            case 0x27 -> new CEventNokezori(typeId); // 39
            case 0x28 -> new CEventChange(typeId); // 40
            case 0x29 -> new CEventStatus(typeId); // 41
            case 0x2A -> new CEventWazaToolParam(typeId); // 42 add
            case 0x2B -> new CEventAttr(typeId); // 43 add
            case 0xFFFFFFFF -> new CEventTerm(typeId); // ???
            default -> throw new BusinessException(500, "未曾设想的类型："+typeId);
        };
    }

    public static SkillInfoObject createEventObjectBhe(int sequence) {

        if (sequence == 0xFF) {
            return new SkillInfoUnknown(sequence);
        }

        if (sequence < 0 || sequence >= SKILL_INFO_TYPE_ENTRIES_BHE.length) {
            throw new BusinessException(500, "该位置waz信息读取错误（请检查waz文件来源）： " + sequence);
        }

        int typeId = SKILL_INFO_TYPE_ENTRIES_BHE[sequence].getType();
        SkillInfoObject obj = createCEventObjectByTypeBhe(typeId);

        obj.setSlotNum(sequence);
        return obj;
    }

}
