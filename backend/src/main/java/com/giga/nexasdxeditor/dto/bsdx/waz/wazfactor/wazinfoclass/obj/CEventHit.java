package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventHit__Read
 */
@Data
public class CEventHit extends WazInfoObject {

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

    @Data
    public static class CEventHitUnit {
        private Integer buffer;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.short1 = readInt16(bytes, offset); offset += 2;
        this.short2 = readInt16(bytes, offset); offset += 2;

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
        this.int12 = readInt32(bytes, offset); offset += 4;
        this.int13 = readInt32(bytes, offset); offset += 4;
        this.int14 = readInt32(bytes, offset); offset += 4;
        this.int15 = readInt32(bytes, offset); offset += 4;
        this.int16 = readInt32(bytes, offset); offset += 4;
        this.int17 = readInt32(bytes, offset); offset += 4;
        this.int18 = readInt32(bytes, offset); offset += 4;
        this.int19 = readInt32(bytes, offset); offset += 4;
        this.int20 = readInt32(bytes, offset); offset += 4;
        this.int21 = readInt32(bytes, offset); offset += 4;
        this.int22 = readInt32(bytes, offset); offset += 4;
        this.int23 = readInt32(bytes, offset); offset += 4;

        this.ceventHitUnitList.clear();

        // TODO 原逻辑部分位置意义不明，做了逻辑整合和优化，所以极有可能出错
        for (int i = 0; i < 33; i++) {
            CEventHitUnit unit = new CEventHitUnit();
            int buffer = readInt32(bytes, offset); offset += 4;
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_HIT_TYPES[i].getType();
                WazInfoObject obj = createCEventObjectByType(typeId);

                offset = obj.readInfo(bytes, offset);
                unit.setData(obj);

            }

            this.ceventHitUnitList.add(unit);

        }


        return offset;
    }

}
