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

    @Data
    public static class CEventNokezoriUnit {
        private Integer buffer;
        private WazInfoObject data;
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
            new CEventNokezoriType(0xFFFFFFFF, "バウンド速度")
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

    private List<CEventNokezoriUnit> ceventNokezoriUnitList;

    public CEventNokezori() {
        ceventNokezoriUnitList = new ArrayList<>();
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        int1 = readInt32(bytes, offset); offset += 4;
        short1 = readInt16(bytes, offset); offset += 2;
        int2 = readInt32(bytes, offset); offset += 4;
        int3 = readInt32(bytes, offset); offset += 4;
        int4 = readInt32(bytes, offset); offset += 4;
        int5 = readInt32(bytes, offset); offset += 4;
        int6 = readInt32(bytes, offset); offset += 4;
        int7 = readInt32(bytes, offset); offset += 4;
        int8 = readInt32(bytes, offset); offset += 4;
        int9 = readInt32(bytes, offset); offset += 4;
        int10 = readInt32(bytes, offset); offset += 4;
        int11 = readInt32(bytes, offset); offset += 4;
        int12 = readInt32(bytes, offset); offset += 4;
        int13 = readInt32(bytes, offset); offset += 4;

        ceventNokezoriUnitList.clear();

        for (int i = 0; i < 17; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;

            CEventNokezoriUnit unit = new CEventNokezoriUnit();
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_NOKEZORI_ENTRIES[i].getType();
                WazInfoObject obj = createCEventObjectByType(typeId);

                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
            }

            ceventNokezoriUnitList.add(unit);
        }

        return offset;
    }

}
