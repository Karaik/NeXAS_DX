package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CEventHit extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventHitType {
        private Integer type;
        private String description;
    }

    public static final CEventHitType[] CEVENT_HIT_ENTRIES = {
            new CEventHitType(0xFFFFFFFF, "攻撃グループ"), // 0  0x00
            new CEventHitType(0xFFFFFFFF, "攻撃グループ2"), // 1  0x01
            new CEventHitType(0xFFFFFFFF, "ヒットカウンタ初期化"), // 2  0x02
            new CEventHitType(0xFFFFFFFF, "フラグ"), // 3  0x03
            new CEventHitType(0xFFFFFFFF, "ヒット数"), // 4  0x04
            new CEventHitType(0xFFFFFFFF, "ヒット間隔"), // 5  0x05
            new CEventHitType(0x26, "攻撃力"), // 6  0x06
            new CEventHitType(0xFFFFFFFF, "攻撃力最低値"), // 7  0x07
            new CEventHitType(0xFFFFFFFF, "攻撃力溜め反映率"), // 8  0x08
            new CEventHitType(0xFFFFFFFF, "装甲攻撃力"), // 9  0x09
            new CEventHitType(0xFFFFFFFF, "ダウン時攻撃力（％）"), // 10  0x0A
            new CEventHitType(0xFFFFFFFF, "ヒートゲージ上昇値"), // 11  0x0B
            new CEventHitType(0xFFFFFFFF, "ヒートゲージ上昇値（Win120）"), // 12  0x0C
            new CEventHitType(0xFFFFFFFF, "ヒートゲージ上昇値（自機）"), // 13  0x0D
            new CEventHitType(0xFFFFFFFF, "ヒートチャージゲージ上昇値"), // 14  0x0E
            new CEventHitType(0xFFFFFFFF, "ヒートチャージゲージ上昇値（自機）"), // 15  0x0F
            new CEventHitType(0xFFFFFFFF, "ヒートチャージゲージ上昇値（自機）(Win120)"), // 16  0x10
            new CEventHitType(0xFFFFFFFF, "基底コンボ補正値"), // 17  0x11
            new CEventHitType(0xFFFFFFFF, "攻撃力補正：技のみ"), // 18  0x12
            new CEventHitType(0xFFFFFFFF, "攻撃力補正"), // 19  0x13
            new CEventHitType(0xFFFFFFFF, "攻撃力補正：技終了時"), // 20  0x14
            new CEventHitType(0xFFFFFFFF, "自分停止時間"), // 21  0x15
            new CEventHitType(0xFFFFFFFF, "消滅時間"), // 22  0x16
            new CEventHitType(0x6, "ヒットエフェクト"), // 23  0x17
            new CEventHitType(0x27, "のけぞり（地上→地上）"), // 24  0x18
            new CEventHitType(0x27, "のけぞり（空中）"), // 25  0x19
            new CEventHitType(0x27, "のけぞり（空中→地上）"), // 26  0x1A
            new CEventHitType(0x27, "のけぞり（ダウン）"), // 27  0x1B
            new CEventHitType(0xFFFFFFFF, "のけぞり優先順位"), // 28  0x1C
            new CEventHitType(0xFFFFFFFF, "ヒットストップ"), // 29  0x1D
            new CEventHitType(0xFFFFFFFF, "ヒットストップ（自機）"), // 30  0x1E
            new CEventHitType(0xFFFFFFFF, "スロー反映率"), // 31  0x1F
            new CEventHitType(0xFFFFFFFF, "画面：拡大縮小時間"), // 32  0x20
            new CEventHitType(0x2, "画面：拡大縮小"), // 33  0x21
            new CEventHitType(0xFFFFFFFF, "画面：振動時間"), // 34  0x22
            new CEventHitType(0x1F, "画面：振動"), // 35  0x23
            new CEventHitType(0x1D, "ヒットＳＥ"), // 36  0x24
            new CEventHitType(0xFFFFFFFF, "鉄ヒット"), // 37  0x25
            new CEventHitType(0xFFFFFFFF, "ダメージ色時間"), // 38  0x26
            new CEventHitType(0xFFFFFFFF, "キャンセルフラグ"), // 39  0x27
            new CEventHitType(0x29, "ステータス増減値") // 40  0x8
    };

    @Data
    public static class CEventHitUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

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
    private Integer int24;
    private Integer int25;
    private Integer int26;
    private Integer int27;
    private Integer int28;
    private Integer int29;
    private Integer int30;
    private Integer int31;

    private List<CEventHitUnit> unitList = new ArrayList<>();

    public CEventHit(Integer typeId) {
        super(typeId);
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
        this.int24 = reader.readInt();
        this.int25 = reader.readInt();
        this.int26 = reader.readInt();
        this.int27 = reader.readInt();
        this.int28 = reader.readInt();
        this.int29 = reader.readInt();
        this.int30 = reader.readInt();
        this.int31 = reader.readInt();

        this.unitList.clear();
        for (int i = 0; i < 41; i++) {
            try {
                int buffer = reader.readInt();
                if (buffer != 0 && buffer != 1) {
                    throw new RuntimeException("buffer == "+buffer);
                }

                CEventHitUnit unit = new CEventHitUnit();
                unit.setUnitSlotNum(i);
                unit.setBuffer(buffer);
                unit.setDescription(CEVENT_HIT_ENTRIES[i].getDescription());

                if (buffer != 0) {
                    int typeId = CEVENT_HIT_ENTRIES[i].getType();
                    SkillInfoObject obj = SkillInfoFactory.createCEventObjectByTypeBhe(typeId);
                    if (obj != null) {
                        obj.readInfo(reader);
                        unit.setData(obj);
                        this.unitList.add(unit);
                    }
                }
            } catch (Exception e) {
                // todo bug
                throw new RuntimeException(e);
            }
        }
    }
}
