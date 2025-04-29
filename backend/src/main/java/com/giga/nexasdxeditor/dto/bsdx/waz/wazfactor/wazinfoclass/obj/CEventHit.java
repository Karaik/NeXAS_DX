package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventHit__Read
 */
@Data
@NoArgsConstructor
public class CEventHit extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventHitType {
        private Integer type;
        private String description;
    }

    public static final CEventHitType[] CEVENT_HIT_TYPES = {
            new CEventHitType(0xFFFFFFFF, "攻撃グループ"),
            new CEventHitType(0xFFFFFFFF, "攻撃グループ2"),
            new CEventHitType(0xFFFFFFFF, "ヒットカウンタ初期化"),
            new CEventHitType(0xFFFFFFFF, "フラグ"),
            new CEventHitType(0xFFFFFFFF, "ヒット数"),
            new CEventHitType(0xFFFFFFFF, "ヒット間隔"),
            new CEventHitType(0x20, "攻撃力"),
            new CEventHitType(0xFFFFFFFF, "攻撃力最低値"),
            new CEventHitType(0xFFFFFFFF, "攻撃力溜め反映率"),
            new CEventHitType(0xFFFFFFFF, "装甲攻撃力"),
            new CEventHitType(0xFFFFFFFF, "基底コンボ補正値"),
            new CEventHitType(0xFFFFFFFF, "攻撃力補正：技のみ"),
            new CEventHitType(0xFFFFFFFF, "攻撃力補正"),
            new CEventHitType(0xFFFFFFFF, "攻撃力補正：技終了時"),
            new CEventHitType(0xFFFFFFFF, "自分停止時間"),
            new CEventHitType(0xFFFFFFFF, "消滅時間"),
            new CEventHitType(0x4, "ヒットエフェクト"),
            new CEventHitType(0x21, "のけぞり（地上→地上）"),
            new CEventHitType(0x21, "のけぞり（空中）"),
            new CEventHitType(0x21, "のけぞり（空中→地上）"),
            new CEventHitType(0x21, "のけぞり（ダウン）"),
            new CEventHitType(0xFFFFFFFF, "ヒットストップ"),
            new CEventHitType(0xFFFFFFFF, "ヒットストップ（自機）"),
            new CEventHitType(0xFFFFFFFF, "スロー反映率"),
            new CEventHitType(0xFFFFFFFF, "画面：拡大縮小時間"),
            new CEventHitType(0x2, "画面：拡大縮小"),
            new CEventHitType(0xFFFFFFFF, "画面：振動時間"),
            new CEventHitType(0x19, "画面：振動"),
            new CEventHitType(0x17, "ヒットＳＥ"),
            new CEventHitType(0xFFFFFFFF, "鉄ヒットＳＥ"),
            new CEventHitType(0xFFFFFFFF, "ダメージ色時間"),
            new CEventHitType(0xFFFFFFFF, "キャンセルフラグ"),
            new CEventHitType(0x23, "メカステータス増減値")
    };

    private Short short1;
    private Short short2;

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
    private Integer int12;
    private Integer int13;
    private Integer int14;
    private Integer int15;
    private Integer int16;
    private Integer int17;
    private Integer int18;
    private Integer int19;
    private Integer int20;
    private Integer int21;
    private Integer int22;
    private Integer int23;

    private List<CEventHitUnit> ceventHitUnitList = new ArrayList<>();

    public CEventHit(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventHitUnit {
        private Integer ceventHitUnitQuantity;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.short1 = reader.readShort();
        this.short2 = reader.readShort();

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
        this.int12 = reader.readInt();
        this.int13 = reader.readInt();
        this.int14 = reader.readInt();
        this.int15 = reader.readInt();
        this.int16 = reader.readInt();
        this.int17 = reader.readInt();
        this.int18 = reader.readInt();
        this.int19 = reader.readInt();
        this.int20 = reader.readInt();
        this.int21 = reader.readInt();
        this.int22 = reader.readInt();
        this.int23 = reader.readInt();

        this.ceventHitUnitList.clear();

        for (int i = 0; i < 33; i++) {
            CEventHitUnit unit = new CEventHitUnit();
            int buffer = reader.readInt();
            unit.setDescription(CEVENT_HIT_TYPES[i].getDescription());
            unit.setCeventHitUnitQuantity(i);
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_HIT_TYPES[i].getType();
                SkillInfoObject obj = createCEventObjectByType(typeId);

                obj.readInfo(reader);
                unit.setData(obj);
                this.ceventHitUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeShort(this.short1);
        writer.writeShort(this.short2);
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
        writer.writeInt(this.int12);
        writer.writeInt(this.int13);
        writer.writeInt(this.int14);
        writer.writeInt(this.int15);
        writer.writeInt(this.int16);
        writer.writeInt(this.int17);
        writer.writeInt(this.int18);
        writer.writeInt(this.int19);
        writer.writeInt(this.int20);
        writer.writeInt(this.int21);
        writer.writeInt(this.int22);
        writer.writeInt(this.int23);
        for (CEventHitUnit unit : ceventHitUnitList) {
            writer.writeInt(unit.getBuffer());
            if (unit.getBuffer() != 0 && unit.getData() != null) {
                unit.getData().writeInfo(writer);
            }
        }
    }

}
