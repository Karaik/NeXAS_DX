package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt16;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/28
 * CEventNokezori__Read
 */
@Data
public class CEventNokezori extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventNokezoriType {
        private Integer type;
        private String description;
    }

    public static final CEventNokezoriType[] CEVENT_NOKEZORI_ENTRIES = {
            new CEventNokezoriType(0xFFFFFFFF, "処理タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "フラグ"),
            new CEventNokezoriType(0xFFFFFFFF, "停止時間"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ(のけぞり時)"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ:自機補正"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ:自機補正(のけぞり時)"),
            new CEventNokezoriType(0xFFFFFFFF, "ダウン時間"),
            new CEventNokezoriType(0x9, "のけぞり方向"),
            new CEventNokezoriType(0x0B, "のけぞり速度タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "のけぞり速度"),
            new CEventNokezoriType(0xFFFFFFFF, "速度減速率"),
            new CEventNokezoriType(0x0B, "吹き飛び規模タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "吹き飛び規模"),
            new CEventNokezoriType(0xFFFFFFFF, "吹き飛び重力"),
            new CEventNokezoriType(0xFFFFFFFF, "バウンドLv"),
            new CEventNokezoriType(0xFFFFFFFF, "バウンド速度"),

            // todo???
            new CEventNokezoriType(0xFFFFFFFF, "指定："),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：地上→地上"),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：空中"),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：空中→地上"),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：ダウン"),
            new CEventNokezoriType(0xFFFFFFFF, "なし")
    };

    private Integer int1;
    private Short short1;
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

    private List<CEventNokezoriUnit> ceventNokezoriUnitList = new ArrayList<>();

    @Data
    public static class CEventNokezoriUnit {
        private Integer ceventHitUnitQuantity;
        private Integer buffer;
        private String description;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;

        this.short1 = readInt16(bytes, offset); offset += 2;

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

        this.ceventNokezoriUnitList.clear();

        for (int i = 0; i < 17; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;

            CEventNokezoriUnit unit = new CEventNokezoriUnit();
            unit.setCeventHitUnitQuantity(i);
            unit.setDescription(CEVENT_NOKEZORI_ENTRIES[i].getDescription());
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_NOKEZORI_ENTRIES[i].getType();
                WazInfoObject obj = createCEventObjectByType(typeId);

                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
                this.ceventNokezoriUnitList.add(unit);
            }
        }

        return offset;
    }

}
