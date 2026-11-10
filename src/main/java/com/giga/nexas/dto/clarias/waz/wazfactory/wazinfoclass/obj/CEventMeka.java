package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

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
public class CEventMeka extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventMekaType {
        private Integer type;
        private String description;
    }

    public static final CEventMekaType[] CEVENT_MEKA_ENTRIES = {
            new CEventMekaType(0xFFFFFFFF, "所属タイプ"),
            new CEventMekaType(0xFFFFFFFF, "フラグ"),
            new CEventMekaType(0xFFFFFFFF, "生成メカ番号"),
            new CEventMekaType(0xFFFFFFFF, "開始技"),
            new CEventMekaType(0x8, "(削除予定)メカ技指定"),
            new CEventMekaType(0xFFFFFFFF, "CPU番号"),
            new CEventMekaType(0x3, "同時生成数"),
            new CEventMekaType(0x3, "最大生成数"),
            new CEventMekaType(0x3, "生成間隔"),
            new CEventMekaType(0x9, "生成位置"),
            new CEventMekaType(0xE, "生成位置補正：方向"),
            new CEventMekaType(0x4, "生成位置補正：方向補正"),
            new CEventMekaType(0x4, "生成位置補正：距離"),
            new CEventMekaType(0x4, "生成位置補正：高さ"),
            new CEventMekaType(0xFFFFFFFF, "生成方向：１発ごとの補正"),
            new CEventMekaType(0x1, "生成方向：同時発射補正"),
            new CEventMekaType(0x3, "生成方向：全体補正"),
            new CEventMekaType(0x22, "生成ＳＥ"),
            new CEventMekaType(0x3, "ｽﾌﾟﾗｲﾄ：拡大縮小"),
            new CEventMekaType(0xE, "ｽﾌﾟﾗｲﾄ：方向"),
            new CEventMekaType(0x4, "ｽﾌﾟﾗｲﾄ：方向補正"),
            new CEventMekaType(0x9, "(未実装)標的"),
            new CEventMekaType(0xE, "ﾍﾞｸﾄﾙ：方向"),
            new CEventMekaType(0x4, "ﾍﾞｸﾄﾙ：方向補正"),
            new CEventMekaType(0x4, "ﾍﾞｸﾄﾙ：速度"),
            new CEventMekaType(0x4, "ﾍﾞｸﾄﾙ：速度減衰"),
            new CEventMekaType(0x4, "ﾍﾞｸﾄﾙ：高さ"),
            new CEventMekaType(0x3, "ﾍﾞｸﾄﾙ：重力"),
            new CEventMekaType(0x3, "耐久力(％)"),
            new CEventMekaType(0xFFFFFFFF, "記憶ＯＢＪの最大数"),
            new CEventMekaType(0xFFFFFFFF, "１イベント生成制限数"),
            new CEventMekaType(0xFFFFFFFF, "消滅フレーム(寿命)"),
            new CEventMekaType(0xFFFFFFFF, "消滅タイプ")
    };

    public static final String[] CEVENT_MEKA_EXTRA_ENTRIES = {
            "敵",
            "ログイン登場",
            "ＯＢＪを記憶",
            "フォーメーション",
            "生成時にゲージを表示しない",
            "その場消滅",
            "ログアウト"
    };

    public Integer fieldTableAddress = 0x00C369A0;
    public Integer wrapperTableAddress = 0x00B60570;

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
    private Integer flatInt11;
    private Integer flatInt12;

    @Data
    public static class CEventMekaUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventMekaUnit> unitList = new ArrayList<>();

    public CEventMeka(Integer typeId) { super(typeId); }

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
        this.flatInt11 = reader.readInt();
        this.flatInt12 = reader.readInt();
        this.unitList.clear();
        for (int i = 0; i < 33; i++) {
            int buffer = reader.readInt();
            CEventMekaUnit unit = new CEventMekaUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_MEKA_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_MEKA_ENTRIES[i].getType();
            if (buffer != 0) {
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
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
        writer.writeInt(this.flatInt11);
        writer.writeInt(this.flatInt12);
        for (int i = 0; i < 33; i++) {
            CEventMekaUnit target = null;
            for (CEventMekaUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) { target = unit; break; }
            }
            writer.writeInt(target.getBuffer());
            if (target.getBuffer() != 0) {
                target.getData().writeInfo(writer);
            }
        }
    }
}
