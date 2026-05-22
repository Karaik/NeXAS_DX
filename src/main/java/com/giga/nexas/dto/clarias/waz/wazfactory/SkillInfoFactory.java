package com.giga.nexas.dto.clarias.waz.wazfactory;

import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.SkillInfoTypeEntry;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.exception.OperationException;

public class SkillInfoFactory {

    public static final SkillInfoTypeEntry[] SKILL_INFO_TYPE_ENTRIES_CLARIAS = {
            new SkillInfoTypeEntry(0x14, 0x0, "ｽﾌﾟﾗｲﾄ", -1, 0, 1), //0
            new SkillInfoTypeEntry(0x3, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小", -1, 0, 1), //1
            new SkillInfoTypeEntry(0x3, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小X", -1, 0, 1), //2
            new SkillInfoTypeEntry(0x3, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小Y", -1, 0, 1), //3
            new SkillInfoTypeEntry(0x3, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小Y(ｽﾛｰ影響)", -1, 0, 1), //4
            new SkillInfoTypeEntry(0x3, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：拡大縮小(ﾍﾞｰｽ)", -1, 0, 1), //5
            new SkillInfoTypeEntry(0x15, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：振動", -1, 0, 1), //6
            new SkillInfoTypeEntry(0x16, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：属性", -1, 0, 1), //7
            new SkillInfoTypeEntry(0xE, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：方向", -1, 0, 1), //8
            new SkillInfoTypeEntry(0x4, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：方向補正", 0, 0, 1), //9
            new SkillInfoTypeEntry(0x4, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：方向増分", -1, 0, 1), //10
            new SkillInfoTypeEntry(0x10, 0x1E82E6, "ｽﾌﾟﾗｲﾄ：方向(演出)", -1, 0, 1), //11
            new SkillInfoTypeEntry(0x9, 0xE6821E, "位置", -1, 1, 1), //12
            new SkillInfoTypeEntry(0xA, 0xE6821E, "位置：無効条件", -1, 1, 1), //13
            new SkillInfoTypeEntry(0x3, 0xE6821E, "位置：分散率", -1, 1, 1), //14
            new SkillInfoTypeEntry(0xB, 0xE6821E, "交代位置", -1, 1, 1), //15
            new SkillInfoTypeEntry(0xC, 0xE6821E, "座標移動", -1, 1, 1), //16
            new SkillInfoTypeEntry(0xF, 0xD0B010, "ﾍﾞｸﾄﾙ：方向", -1, 2, 1), //17
            new SkillInfoTypeEntry(0x4, 0xD0B010, "ﾍﾞｸﾄﾙ：方向補正", -1, 2, 1), //18
            new SkillInfoTypeEntry(0x4, 0xD0B010, "ﾍﾞｸﾄﾙ：方向増分", -1, 2, 1), //19
            new SkillInfoTypeEntry(0x6, 0xD0B010, "ﾍﾞｸﾄﾙ：速度", -1, 2, 1), //20
            new SkillInfoTypeEntry(0x6, 0xD0B010, "ﾍﾞｸﾄﾙ：ジャンプ力", -1, 2, 1), //21
            new SkillInfoTypeEntry(0xD, 0xD0B010, "ﾍﾞｸﾄﾙ：追尾ジャンプ力", -1, 2, 1), //22
            new SkillInfoTypeEntry(0x5, 0xD0B010, "ﾍﾞｸﾄﾙ：重力", -1, 2, 1), //23
            new SkillInfoTypeEntry(0x3, 0xD0B010, "ﾍﾞｸﾄﾙ：最低高度", -1, 2, 1), //24
            new SkillInfoTypeEntry(0x6, 0xD0B010, "ﾍﾞｸﾄﾙ：速度XYZ", -1, 2, 1), //25
            new SkillInfoTypeEntry(0x3, 0xD0B010, "ﾍﾞｸﾄﾙ：慣性", -1, 2, 1), //26
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：移動", -1, 2, 1), //27
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：技", -1, 2, 1), //28
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：のけぞり", -1, 2, 1), //29
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：重力", -1, 2, 1), //30
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：ジャンプ力", -1, 2, 1), //31
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：浮遊", -1, 2, 1), //32
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：ｼｮｰﾄﾀﾞｯｼｭ", -1, 2, 1), //33
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：慣性", -1, 2, 1), //34
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：押し戻し", -1, 2, 1), //35
            new SkillInfoTypeEntry(0x1, 0xAA6400, "ﾍﾞｸﾄﾙ補正率：相殺", -1, 2, 1), //36
            new SkillInfoTypeEntry(0x1, 0xAA6400, "相殺ベクトル更新", -1, 2, 1), //37
            new SkillInfoTypeEntry(0x12, 0xAA6400, "技後の慣性補正", -1, 2, 1), //38
            new SkillInfoTypeEntry(0x0, 0xAA6400, "技後の慣性補正：無効化", -1, 2, 1), //39
            new SkillInfoTypeEntry(0x9, 0xAA6400, "標的", -1, 2, 1), //40
            new SkillInfoTypeEntry(0x3, 0xAA6400, "弾かれ速度(100=1)", -1, 2, 1), //41
            new SkillInfoTypeEntry(0x1, 0xDCDCFF, "耐久力", -1, 3, 1), //42
            new SkillInfoTypeEntry(0x13, 0xC86432, "汎用変数", -1, 3, 1), //43
            new SkillInfoTypeEntry(0x3C, 0xC86432, "メインパラメータ変動", -1, 3, 1), //44
            new SkillInfoTypeEntry(0x30, 0xC86432, "技ツールパラメータ", -1, 3, 1), //45
            new SkillInfoTypeEntry(0x1, 0xE0E0E0, "装甲", -1, 3, 1), //46
            new SkillInfoTypeEntry(0x0, 0xE0E0E0, "ハイパーアーマー", -1, 3, 1), //47
            new SkillInfoTypeEntry(0x1, 0xE0E0E0, "装甲ﾋｯﾄｽﾄｯﾌﾟ補正率", -1, 3, 1), //48
            new SkillInfoTypeEntry(0x0, 0xE0E0E0, "無敵", -1, 3, 1), //49
            new SkillInfoTypeEntry(0x1, 0xE0E0E0, "死亡(自爆)", -1, 3, 1), //50
            new SkillInfoTypeEntry(0x3A, 0xC80000, "弱点(魔痕)", -1, 3, 1), //51
            new SkillInfoTypeEntry(0x38, 0xC86464, "くらい状態", -1, 3, 1), //52
            new SkillInfoTypeEntry(0x1, 0xC86464, "くらい：HP", -1, 3, 1), //53
            new SkillInfoTypeEntry(0x1, 0xC84000, "パワーゲージ増減：現在値", -1, 3, 1), //54
            new SkillInfoTypeEntry(0x1, 0xC84000, "熱量", -1, 3, 1), //55
            new SkillInfoTypeEntry(0x1A, 0xE61E1E, "攻撃：メカ", -1, 4, 1), //56
            new SkillInfoTypeEntry(0x1A, 0xE61E1E, "攻撃：弾", -1, 4, 1), //57
            new SkillInfoTypeEntry(0x1, 0xE61E1E, "攻撃判定拡大縮小反映率", -1, 4, 1), //58
            new SkillInfoTypeEntry(0x37, 0xE61E1E, "ヒットストップ演出", -1, 4, 1), //59
            new SkillInfoTypeEntry(0x35, 0xB41E1E, "相殺(メイン)", -1, 4, 1), //60
            new SkillInfoTypeEntry(0x35, 0xB41E1E, "相殺(弾同士)", -1, 4, 1), //61
            new SkillInfoTypeEntry(0x0, 0xB41E1E, "ジャスト回避", -1, 4, 1), //62
            new SkillInfoTypeEntry(0x1, 0xA00000, "攻撃アイコン", 5, 4, 1), //63
            new SkillInfoTypeEntry(0x39, 0x821E1E, "パートナー援護：キャンセル", -1, 4, 1), //64
            new SkillInfoTypeEntry(0x2E, 0x821E1E, "能力(ステータス)変動", -1, 4, 1), //65
            new SkillInfoTypeEntry(0x2F, 0x821E1E, "バトルパラメータ補正", -1, 4, 1), //66
            new SkillInfoTypeEntry(0x1B, 0x1E1EE0, "ＣＰＵ回避", -1, 5, 1), //67
            new SkillInfoTypeEntry(0x1C, 0x1E1EE0, "ＣＰＵボタン入力", -1, 5, 1), //68
            new SkillInfoTypeEntry(0x1, 0x1E1EE0, "ＣＰＵ特殊行動", 6, 5, 1), //69
            new SkillInfoTypeEntry(0x1, 0xE61E1E, "ｷｬﾝｾﾙﾌﾗｸﾞ", 1, 3, 1), //70
            new SkillInfoTypeEntry(0x1D, 0x1EC8E6, "エフェクト", -1, 6, 1), //71
            new SkillInfoTypeEntry(0x1E, 0x1EC8E6, "メカ生成", -1, 6, 1), //72
            new SkillInfoTypeEntry(0x1F, 0x1EC8E6, "残像", -1, 6, 1), //73
            new SkillInfoTypeEntry(0x20, 0xC80000, "溜め", -1, 3, 1), //74
            new SkillInfoTypeEntry(0x21, 0xE6821E, "接触", -1, 4, 1), //75
            new SkillInfoTypeEntry(0x0, 0xE66432, "のけぞり状態", -1, 4, 1), //76
            new SkillInfoTypeEntry(0x22, 0xD0B010, "ＳＥ", -1, 7, 1), //77
            new SkillInfoTypeEntry(0x23, 0xD0B010, "ボイス", -1, 7, 1), //78
            new SkillInfoTypeEntry(0x1, 0x606060, "影の濃さ", -1, 3, 1), //79
            new SkillInfoTypeEntry(0x1, 0xC86432, "優先順位補正", -1, 3, 1), //80
            new SkillInfoTypeEntry(0x31, 0xC8C832, "属性", -1, 3, 1), //81
            new SkillInfoTypeEntry(0x32, 0xE6821E, "ステルス", -1, 3, 1), //82
            new SkillInfoTypeEntry(0x1, 0xE6821E, "記憶OBJ最大数", -1, 3, 1), //83
            new SkillInfoTypeEntry(0x34, 0x82E61E, "キー入力制御", -1, 8, 1), //84
            new SkillInfoTypeEntry(0x33, 0x82E61E, "ゲームパッド振動", -1, 8, 1), //85
            new SkillInfoTypeEntry(0x1, 0x82E61E, "ﾚｰｻﾞｰ：振幅", -1, 9, 1), //86
            new SkillInfoTypeEntry(0x1, 0x82E61E, "ﾚｰｻﾞｰ：周期", -1, 9, 1), //87
            new SkillInfoTypeEntry(0x17, 0x808080, "ﾏﾙﾁﾛｯｸ:ロック開始・終了", -1, 10, 1), //88
            new SkillInfoTypeEntry(0x18, 0x808080, "ﾏﾙﾁﾛｯｸ:ロック", -1, 10, 1), //89
            new SkillInfoTypeEntry(0x3, 0x808080, "ﾏﾙﾁﾛｯｸ:ターゲットカウンタ", -1, 10, 1), //90
            new SkillInfoTypeEntry(0x19, 0x808080, "ﾏﾙﾁﾛｯｸ:描画フラグ", -1, 10, 1), //91
            new SkillInfoTypeEntry(0x3, 0x82E61E, "画面演出：拡大縮小", -1, 11, 1), //92
            new SkillInfoTypeEntry(0x24, 0x82E61E, "画面演出：振動", -1, 11, 1), //93
            new SkillInfoTypeEntry(0x25, 0x82E61E, "画面演出：属性", -1, 11, 1), //94
            new SkillInfoTypeEntry(0x26, 0x82E61E, "画面演出：エフェクト", -1, 11, 1), //95
            new SkillInfoTypeEntry(0x27, 0x82E61E, "画面演出：効果線", -1, 11, 1), //96
            new SkillInfoTypeEntry(0x28, 0x82E61E, "画面演出：集中線", -1, 11, 1), //97
            new SkillInfoTypeEntry(0x29, 0x82E61E, "画面演出：ブラー", -1, 11, 1), //98
            new SkillInfoTypeEntry(0x2A, 0x82E61E, "画面演出：カメラ", -1, 11, 1), //99
            new SkillInfoTypeEntry(0x1, 0x82E61E, "攻撃方向", -1, 11, 1), //100
            new SkillInfoTypeEntry(0x0, 0x82E61E, "画面停止", -1, 11, 1), //101
            new SkillInfoTypeEntry(0x1, 0x82E61E, "画面停止：エフェクト更新間隔", -1, 11, 1), //102
            new SkillInfoTypeEntry(0x1, 0x82E61E, "技スロー効果", -1, 11, 1), //103
            new SkillInfoTypeEntry(0x36, 0x82E61E, "システムスロー効果", -1, 11, 1), //104
            new SkillInfoTypeEntry(0x1, 0x82E61E, "スロー反映率", -1, 11, 1), //105
            new SkillInfoTypeEntry(0x3, 0x82E61E, "画面演出(スロー影響なし)：拡大縮小", -1, 11, 0), //106
            new SkillInfoTypeEntry(0x24, 0x82E61E, "画面演出(スロー影響なし)：振動", -1, 11, 0), //107
            new SkillInfoTypeEntry(0x25, 0x82E61E, "画面演出(スロー影響なし)：属性", -1, 11, 0), //108
            new SkillInfoTypeEntry(0x29, 0x82E61E, "画面演出(スロー影響なし)：ブラー", -1, 11, 0), //109
            new SkillInfoTypeEntry(0x3B, 0xC8C8C8, "プレイヤー変更", -1, 12, 1), //110
            new SkillInfoTypeEntry(0x2D, 0xF8F8F8, "変更処理", -1, 12, 1), //111
    };

    public static SkillInfoObject createCEventObjectByTypeClarias(int typeId) {
        return switch (typeId) {
            case 0x0 -> new CEventVoid(typeId);
            case 0x1 -> new CEventVal(typeId);
            case 0x2 -> new CEventVal(typeId);
            case 0x3 -> new CEventValRandom(typeId);
            case 0x4 -> new CEventValRandom(typeId);
            case 0x5 -> new CEventValRandom(typeId);
            case 0x6 -> new CEventValRandom(typeId);
            case 0x7 -> new CEventWazaSelect(typeId);
            case 0x8 -> new CEventTerm(typeId);
            case 0x9 -> new CEventTerm(typeId);
            case 0xA -> new CEventTerm(typeId);
            case 0xB -> new CEventPosChange(typeId);
            case 0xC -> new CEventMove(typeId);
            case 0xD -> new CEventHeight(typeId);
            case 0xE -> new CEventTerm(typeId);
            case 0xF -> new CEventTerm(typeId);
            case 0x10 -> new CEventAngleEffect(typeId);
            case 0x11 -> new CEventTerm(typeId);
            case 0x12 -> new CEventSlipHosei(typeId);
            case 0x13 -> new CEventFreeParam(typeId);
            case 0x14 -> new CEventSprite(typeId);
            case 0x15 -> new CEventSpriteYure(typeId);
            case 0x16 -> new CEventSpriteAttr(typeId);
            case 0x17 -> new CEventMultiLockStartEnd(typeId);
            case 0x18 -> new CEventMultiLock(typeId);
            case 0x19 -> new CEventMultiLockDraw(typeId);
            case 0x1A -> new CEventHit(typeId);
            case 0x1B -> new CEventEscape(typeId);
            case 0x1C -> new CEventCpuButton(typeId);
            case 0x1D -> new CEventEffect(typeId);
            case 0x1E -> new CEventMeka(typeId);
            case 0x1F -> new CEventBlink(typeId);
            case 0x20 -> new CEventCharge(typeId);
            case 0x21 -> new CEventTouch(typeId);
            case 0x22 -> new CEventSe(typeId);
            case 0x23 -> new CEventVoice(typeId);
            case 0x24 -> new CEventScreenYure(typeId);
            case 0x25 -> new CEventScreenAttr(typeId);
            case 0x26 -> new CEventScreenEffect(typeId);
            case 0x27 -> new CEventScreenLine(typeId);
            case 0x28 -> new CEventRadialLine(typeId);
            case 0x29 -> new CEventBlur(typeId);
            case 0x2A -> new CEventCamera(typeId);
            case 0x2B -> new CEventTerm(typeId);
            case 0x2C -> new CEventNokezori(typeId);
            case 0x2D -> new CEventChange(typeId);
            case 0x2E -> new CEventStatus(typeId);
            case 0x2F -> new CEventParamRevise(typeId);
            case 0x30 -> new CEventWazaToolParam(typeId);
            case 0x31 -> new CEventAttr(typeId);
            case 0x32 -> new CEventStealth(typeId);
            case 0x33 -> new CEventGamePadShake(typeId);
            case 0x34 -> new CEventKeyInputControl(typeId);
            case 0x35 -> new CEventHitSousai(typeId);
            case 0x36 -> new CEventSystemSlow(typeId);
            case 0x37 -> new CEventHitStopEffect(typeId);
            case 0x38 -> new CEventDefense(typeId);
            case 0x39 -> new CEventPartnerAttackCancel(typeId);
            case 0x3A -> new CEventWeakPoint(typeId);
            case 0x3B -> new CEventPlayerChange(typeId);
            case 0x3C -> new CEventMainParam(typeId);
            default -> throw new OperationException(500, "unexpected CEvent type: " + typeId);
        };
    }

    public static SkillInfoObject createEventObjectClarias(int sequence) {
        if (sequence < 0 || sequence >= SKILL_INFO_TYPE_ENTRIES_CLARIAS.length) {
            throw new OperationException(500, "wrong waz type (please insure your waz file is original file or generated file): " + sequence);
        }

        int typeId = SKILL_INFO_TYPE_ENTRIES_CLARIAS[sequence].getType();
        SkillInfoObject obj = createCEventObjectByTypeClarias(typeId);

        obj.setSlotNum(sequence);
        return obj;
    }

}
