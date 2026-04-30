package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.exception.OperationException;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeClarias;

@Data
@NoArgsConstructor
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
            new CEventEffectType(0x7, "エフェクト番号"),
            new CEventEffectType(0x3, "同時発射数"),
            new CEventEffectType(0x3, "最大発射数"),
            new CEventEffectType(0x3, "発射間隔"),
            new CEventEffectType(0x9, "発射位置"),
            new CEventEffectType(0xE, "発射位置補正：方向"),
            new CEventEffectType(0x4, "発射位置補正：方向補正"),
            new CEventEffectType(0x4, "発射位置補正：距離"),
            new CEventEffectType(0x4, "発射位置補正：高さ"),
            new CEventEffectType(0xFFFFFFFF, "発射方向：１発ごとの補正"),
            new CEventEffectType(0x1, "発射方向：同時発射補正"),
            new CEventEffectType(0x3, "発射方向：全体補正"),
            new CEventEffectType(0x22, "発射ＳＥ"),
            new CEventEffectType(0x3, "ｽﾌﾟﾗｲﾄ：拡大縮小"),
            new CEventEffectType(0x3, "ｽﾌﾟﾗｲﾄ：拡大縮小X"),
            new CEventEffectType(0x3, "ｽﾌﾟﾗｲﾄ：拡大縮小Y"),
            new CEventEffectType(0x3, "ｽﾌﾟﾗｲﾄ：拡大縮小Y(ｽﾛｰ影響)"),
            new CEventEffectType(0x15, "ｽﾌﾟﾗｲﾄ：振動"),
            new CEventEffectType(0x16, "ｽﾌﾟﾗｲﾄ：属性"),
            new CEventEffectType(0xE, "ｽﾌﾟﾗｲﾄ：方向"),
            new CEventEffectType(0x4, "ｽﾌﾟﾗｲﾄ：方向補正"),
            new CEventEffectType(0x4, "ｽﾌﾟﾗｲﾄ：方向増分"),
            new CEventEffectType(0x9, "位置"),
            new CEventEffectType(0x3, "位置：分散率"),
            new CEventEffectType(0xC, "座標移動"),
            new CEventEffectType(0x9, "標的"),
            new CEventEffectType(0xE, "ﾍﾞｸﾄﾙ：方向"),
            new CEventEffectType(0x4, "ﾍﾞｸﾄﾙ：方向補正"),
            new CEventEffectType(0x4, "ﾍﾞｸﾄﾙ：方向増分"),
            new CEventEffectType(0x4, "ﾍﾞｸﾄﾙ：速度"),
            new CEventEffectType(0x4, "ﾍﾞｸﾄﾙ：高さ"),
            new CEventEffectType(0x3, "ﾍﾞｸﾄﾙ：重力"),
            new CEventEffectType(0x3, "ﾍﾞｸﾄﾙ：最低高度"),
            new CEventEffectType(0x3, "ﾍﾞｸﾄﾙ：慣性"),
            new CEventEffectType(0x13, "汎用変数"),
            new CEventEffectType(0x1, "溜め攻撃力"),
            new CEventEffectType(0x1, "影の濃さ"),
            new CEventEffectType(0x1, "優先順位補正"),
            new CEventEffectType(0x3, "耐久力"),
            new CEventEffectType(0xFFFFFFFF, "記憶ＯＢＪの最大数"),
            new CEventEffectType(0xFFFFFFFF, "指定OBJ数以下なら生成"),
            new CEventEffectType(0xFFFFFFFF, "指定画面範囲内なら生成"),
            new CEventEffectType(0xFFFFFFFF, "指定画面範囲外なら削除")
    };

    public static final String[] CEVENT_EFFECT_EXTRA_ENTRIES = {
            "パーツ（破片）",
            "保持パーツ(エフェクト)",
            "サンダーライン枝",
            "サンダーライン枝（煙用）",
            "記憶",
            "後方描画",
            "接触○",
            "弾相殺を行わない",
            "画面停止無効",
            "バトルスキル有効時のみ発動可能"
    };

    public Integer fieldTableAddress = 0x00C368B0;
    public Integer wrapperTableAddress = 0x00B5FA10;

    private Integer flatInt0;
    private Integer flatInt1;
    private Integer flatInt2;
    private Integer flatInt3;
    private Integer flatInt4;
    private Integer flatInt5;
    private Integer flatInt6;
    private Integer flatInt7;
    private Integer flatInt8;
    private Integer flatInt9;
    private Integer flatInt10;

    @Data
    public static class CEventEffectUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventEffectUnit> unitList = new ArrayList<>();

    public CEventEffect(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.flatInt0 = reader.readInt();
        this.flatInt1 = reader.readInt();
        this.flatInt2 = reader.readInt();
        this.flatInt3 = reader.readInt();
        this.flatInt4 = reader.readInt();
        this.flatInt5 = reader.readInt();
        this.flatInt6 = reader.readInt();
        this.flatInt7 = reader.readInt();
        this.flatInt8 = reader.readInt();
        this.flatInt9 = reader.readInt();
        this.flatInt10 = reader.readInt();
        this.unitList.clear();
        for (int i = 0; i < 46; i++) {
            int buffer = reader.readInt();
            CEventEffectUnit unit = new CEventEffectUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_EFFECT_TYPES[i].getDescription());
            int innerTypeId = CEVENT_EFFECT_TYPES[i].getType();
            if (buffer != 0) {
                if (innerTypeId == 0xFFFFFFFF) {
                    throw new OperationException(500, "unexpected non-zero effect wrapper buffer at slot " + i);
                }
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                if (obj == null) {
                    throw new OperationException(500, "missing effect wrapper type at slot " + i + ": " + innerTypeId);
                }
                obj.readInfo(reader);
                unit.setData(obj);
            }
            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.flatInt0);
        writer.writeInt(this.flatInt1);
        writer.writeInt(this.flatInt2);
        writer.writeInt(this.flatInt3);
        writer.writeInt(this.flatInt4);
        writer.writeInt(this.flatInt5);
        writer.writeInt(this.flatInt6);
        writer.writeInt(this.flatInt7);
        writer.writeInt(this.flatInt8);
        writer.writeInt(this.flatInt9);
        writer.writeInt(this.flatInt10);
        for (int i = 0; i < 46; i++) {
            CEventEffectUnit target = null;
            for (CEventEffectUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) { target = unit; break; }
            }
            if (target != null) {
                writer.writeInt(target.getBuffer());
                if (target.getBuffer() != 0 && target.getData() != null) {
                    target.getData().writeInfo(writer);
                }
            } else {
                writer.writeInt(0);
            }
        }
    }
}
