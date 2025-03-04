package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * @Description CEventCpuButton
 * CEventCpuButton__Read
 */
@Data
public class CEventCpuButton extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventCpuButtonType {
        private Integer type;
        private String description;
    }

    public static final CEventCpuButtonType[] CEVENT_CPU_BUTTON_ENTRIES = {
            new CEventCpuButtonType(0x9, "チェック方向"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック方向補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック範囲"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック範囲（高さ）"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック高度補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック回数"),
            new CEventCpuButtonType(0xFFFFFFFF, "ボタン入力（範囲内）"),
            new CEventCpuButtonType(0xFFFFFFFF, "ボタン入力（範囲外）")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Short short1;
    private Short short2;

    private List<CEventCpuButtonUnit> ceventCpuButtonUnitList = new ArrayList<>();

    @Data
    public static class CEventCpuButtonUnit {
        private Integer ceventCpuButtonUnitQuantity;
        private String description;
        private Integer buffer;
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

        this.short1 = readInt16(bytes, offset); offset += 2;
        this.short2 = readInt16(bytes, offset); offset += 2;

        this.ceventCpuButtonUnitList.clear();

        for (int i = 0; i < 8; i++) {

            if (i == 0) {
                CEventCpuButtonUnit unit = new CEventCpuButtonUnit();
                int buffer = readInt32(bytes, offset); offset += 4;
                unit.setCeventCpuButtonUnitQuantity(i);
                unit.setDescription(CEVENT_CPU_BUTTON_ENTRIES[i].getDescription());
                unit.setBuffer(buffer);

                if (buffer != 0) {
                    WazInfoObject obj = createCEventObjectByType(CEVENT_CPU_BUTTON_ENTRIES[i].getType());
                    if (obj != null) {
                        offset = obj.readInfo(bytes, offset);
                        unit.setData(obj);
                    }
                }
                this.ceventCpuButtonUnitList.add(unit);
            }

        }

        return offset;
    }
}
