package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventCharge__Read
 */
@Data
public class CEventCharge extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventChargeType {
        private Integer type;
        private String description;
    }

    public static final CEventChargeType[] CEVENT_CHARGE_ENTRIES = {
            new CEventChargeType(0x0, "消費する熱チャージ量"),
            new CEventChargeType(0x0, "蓄積する溜め")
    };

    private List<CEventChargeUnit> ceventChargeUnitList = new ArrayList<>();

    @Data
    public static class CEventChargeUnit {
        private Integer ceventChargeUnitQuantity;
        private String description;
        private Integer buffer;
        private SkillInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.ceventChargeUnitList.clear();

        for (int i = 0; i < 2; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;

            CEventChargeUnit unit = new CEventChargeUnit();
            unit.setCeventChargeUnitQuantity(i);
            unit.setDescription(CEVENT_CHARGE_ENTRIES[i].getDescription());
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_CHARGE_ENTRIES[i].getType();
                SkillInfoObject obj = createCEventObjectByType(typeId);

                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
                this.ceventChargeUnitList.add(unit);
            }
        }

        return offset;
    }
}

