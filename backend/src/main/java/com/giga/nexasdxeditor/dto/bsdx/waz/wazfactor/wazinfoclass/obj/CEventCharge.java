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
 * CEventCharge__Read
 */
@Data
public class CEventCharge extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventChargeType {
        private Integer type;
        private String description;
    }

    public static final CEventChargeType[] CEVENT_CHARGE_ENTRIES = {
            // 根据 .rdata:007F20B8 数组逆向数据定义
            new CEventChargeType(0xFFFFFFFF, "タイプ"),
            new CEventChargeType(0xFFFFFFFF, "回数"),
            new CEventChargeType(0xFFFFFFFF, "開始値"),
            new CEventChargeType(0xFFFFFFFF, "終了値"),
            new CEventChargeType(0xFFFFFFFF, "通常"),
            new CEventChargeType(0xFFFFFFFF, "加速"),
            new CEventChargeType(0xFFFFFFFF, "減速"),
            new CEventChargeType(0xFFFFFFFF, "ループ"),
            new CEventChargeType(0xFFFFFFFF, "往復")
    };

    private List<CEventChargeUnit> ceventChargeUnitList = new ArrayList<>();

    @Data
    public static class CEventChargeUnit {
        private Integer buffer;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.ceventChargeUnitList.clear();

        for (int i = 0; i < 2; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;

            CEventChargeUnit unit = new CEventChargeUnit();
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEventChange.CEVENT_CHANGE_ENTRIES[i].getType();
                WazInfoObject obj = createCEventObjectByType(typeId);

                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
            }
            ceventChargeUnitList.add(unit);
        }

        return offset;
    }
}

