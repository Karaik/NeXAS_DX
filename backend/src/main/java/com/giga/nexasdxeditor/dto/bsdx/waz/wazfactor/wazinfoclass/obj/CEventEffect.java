package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventEffect__Read
 */
@Data
public class CEventEffect extends SkillInfoObject {

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
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.int7 = reader.readInt();
        this.int8 = reader.readInt();
        this.int9 = reader.readInt();
        this.int10 = reader.readInt();
        this.int11 = reader.readInt();

        this.ceventEffectUnitList.clear();

        for (int i = 0; i < 45; i++) {
            int buffer = reader.readInt();
            CEventEffectUnit unit = new CEventEffectUnit();
            unit.setDescription(CEVENT_EFFECT_TYPES[i].getDescription());
            unit.setCeventEffectUnitQuantity(i);
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_EFFECT_TYPES[i].getType();
                SkillInfoObject obj = createCEventObjectByType(typeId);
                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
                ceventEffectUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
        writer.writeInt(this.int7);
        writer.writeInt(this.int8);
        writer.writeInt(this.int9);
        writer.writeInt(this.int10);
        writer.writeInt(this.int11);
        for (CEventEffectUnit unit : ceventEffectUnitList) {
            writer.writeInt(unit.getBuffer());
            if (unit.getBuffer() != 0 && unit.getData() != null) {
                unit.getData().writeInfo(writer);
            }
        }
    }

}

