package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventEffect__Read
 */
@Data
public class CEventEffect extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventEffectType {
        private Integer type;
        private String description;
    }

    public static final CEventEffectType[] CEVENT_EFFECT_TYPES = {
            new CEventEffectType(0xFFFFFFFF, "エフェクトタイプ1"),
            new CEventEffectType(0xFFFFFFFF, "エフェクトタイプ2"),
            new CEventEffectType(0xFFFFFFFF, "フラグ"),
            new CEventEffectType(0x4, "エフェクト番号"),
            new CEventEffectType(0x2, "同時発射数"),
            new CEventEffectType(0x2, "最大発射数"),
            new CEventEffectType(0x2, "発射間隔"),
            new CEventEffectType(0x5, "発射位置"),
            new CEventEffectType(0x9, "発射位置補正：方向"),
            new CEventEffectType(0x3, "発射位置補正：方向補正"),
            new CEventEffectType(0x3, "発射位置補正：距離"),
            new CEventEffectType(0x3, "発射位置補正：高さ"),
            new CEventEffectType(0xFFFFFFFF, "発射方向：１発ごとの補正"),
            new CEventEffectType(0x0, "発射方向：同時発射補正"),
            new CEventEffectType(0x2, "発射方向：全体補正"),
            new CEventEffectType(0x17, "発射ＳＥ"),
            new CEventEffectType(0x2, "ｽﾌﾟﾗｲﾄ：拡大縮小"),
            new CEventEffectType(0x2, "ｽﾌﾟﾗｲﾄ：拡大縮小X"),
            new CEventEffectType(0x2, "ｽﾌﾟﾗｲﾄ：拡大縮小Y"),
            new CEventEffectType(0x2, "ｽﾌﾟﾗｲﾄ：拡大縮小Y(ｽﾛｰ影響)"),
            new CEventEffectType(0xE, "ｽﾌﾟﾗｲﾄ：振動"),
            new CEventEffectType(0xF, "ｽﾌﾟﾗｲﾄ：属性"),
            new CEventEffectType(0x9, "ｽﾌﾟﾗｲﾄ：方向"),
            new CEventEffectType(0x3, "ｽﾌﾟﾗｲﾄ：方向補正"),
            new CEventEffectType(0x3, "ｽﾌﾟﾗｲﾄ：方向増分"),
            new CEventEffectType(0x5, "位置"),
            new CEventEffectType(0x2, "位置：分散率"),
            new CEventEffectType(0x7, "座標移動"),
            new CEventEffectType(0x9, "ﾍﾞｸﾄﾙ：方向"),
            new CEventEffectType(0x3, "ﾍﾞｸﾄﾙ：方向補正"),
            new CEventEffectType(0x3, "ﾍﾞｸﾄﾙ：方向増分"),
            new CEventEffectType(0x3, "ﾍﾞｸﾄﾙ：速度"),
            new CEventEffectType(0x3, "ﾍﾞｸﾄﾙ：高さ"),
            new CEventEffectType(0x2, "ﾍﾞｸﾄﾙ：重力"),
            new CEventEffectType(0x2, "ﾍﾞｸﾄﾙ：最低高度"),
            new CEventEffectType(0x2, "ﾍﾞｸﾄﾙ：慣性"),
            new CEventEffectType(0x0, "汎用変数"),
            new CEventEffectType(0x0, "溜め攻撃力"),
            new CEventEffectType(0x0, "影の濃さ"),
            new CEventEffectType(0x0, "優先順位補正"),
            new CEventEffectType(0x2, "耐久力"),
            new CEventEffectType(0xFFFFFFFF, "記憶ＯＢＪの最大数"),
            new CEventEffectType(0xFFFFFFFF, "指定OBJ数以下なら生成"),
            new CEventEffectType(0xFFFFFFFF, "指定画面範囲内なら生成"),
            new CEventEffectType(0xFFFFFFFF, "指定画面範囲外なら削除")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private Integer int11;

    private List<CEventEffectUnit> ceventEffectUnitList = new ArrayList<>();

    @Data
    public static class CEventEffectUnit {
        private Integer ceventEffectUnitQuantity;
        private Integer buffer;
        private String description;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;
        this.int4 = readInt32(bytes, offset); offset += 4;
        this.int5 = readInt32(bytes, offset); offset += 4;
        this.int6 = readInt32(bytes, offset); offset += 4;
        this.int7 = readInt32(bytes, offset); offset += 4;
        this.int8 = readInt32(bytes, offset); offset += 4;
        this.int9 = readInt32(bytes, offset); offset += 4;
        this.int10 = readInt32(bytes, offset); offset += 4;
        this.int11 = readInt32(bytes, offset); offset += 4;

        this.ceventEffectUnitList.clear();

        for (int i = 0; i < 45; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;
            CEventEffectUnit unit = new CEventEffectUnit();
            unit.setDescription(CEVENT_EFFECT_TYPES[i].getDescription());
            unit.setCeventEffectUnitQuantity(i);
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_EFFECT_TYPES[i].getType();
                WazInfoObject obj = createCEventObjectByType(typeId);
                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
                ceventEffectUnitList.add(unit);
            }

        }

        return offset;
    }
}

