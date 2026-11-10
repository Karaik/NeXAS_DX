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
public class CEventCharge extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventChargeType {
        private Integer type;
        private String description;
    }

    public static final CEventChargeType[] CEVENT_CHARGE_ENTRIES = {
            new CEventChargeType(0x1, "消費する熱チャージ量"), //diff
            new CEventChargeType(0x1, "蓄積する溜め") //diff
    };

    @Data
    public static class CEventChargeUnit {
        private Integer ceventChargeUnitQuantity;
        private String description;
        private Integer buffer;
        private Integer unitSlotNum;
        private SkillInfoObject data;
    }

    private List<CEventChargeUnit> ceventChargeUnitList = new ArrayList<>();

    public CEventCharge(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.ceventChargeUnitList.clear();

        for (int i = 0; i < 2; i++) {
            int buffer = reader.readInt();

            CEventChargeUnit unit = new CEventChargeUnit();
            unit.setCeventChargeUnitQuantity(i);
            unit.setDescription(CEVENT_CHARGE_ENTRIES[i].getDescription());
            unit.setBuffer(buffer);
            unit.setUnitSlotNum(i);

            if (buffer != 0) {
                int innerTypeId = CEVENT_CHARGE_ENTRIES[i].getType();
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                obj.readInfo(reader);
                unit.setData(obj);
            }
            this.ceventChargeUnitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);

        for (int i = 0; i < 2; i++) {
            CEventChargeUnit target = null;

            for (CEventChargeUnit unit : this.ceventChargeUnitList) {
                if (unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
            }

            writer.writeInt(target.getBuffer());
            if (target.getBuffer() != 0) {
                target.getData().writeInfo(writer);
            }
        }
    }
}
