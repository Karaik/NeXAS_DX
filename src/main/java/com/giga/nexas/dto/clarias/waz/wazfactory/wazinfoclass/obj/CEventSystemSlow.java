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
public class CEventSystemSlow extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventSystemSlowType {
        private Integer type;
        private String description;
    }

    public static final CEventSystemSlowType[] CEVENT_SYSTEM_SLOW_ENTRIES = {
            new CEventSystemSlowType(0x1, "持続フレーム"),
            new CEventSystemSlowType(0x1, "強さ"),
            new CEventSystemSlowType(0x1, "復帰フレーム"),
            new CEventSystemSlowType(0xFFFFFFFF, "フラグ")
    };

    @Data
    public static class CEventSystemSlowUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Byte byte1;
    private List<CEventSystemSlowUnit> unitList = new ArrayList<>();

    public CEventSystemSlow(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.byte1 = reader.readByte();

        this.unitList.clear();
        for (int i = 0; i < 4; i++) {
            int buffer = reader.readInt();

            CEventSystemSlowUnit unit = new CEventSystemSlowUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_SYSTEM_SLOW_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_SYSTEM_SLOW_ENTRIES[i].getType();
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
        writer.writeByte(this.byte1);

        for (int i = 0; i < 4; i++) {
            CEventSystemSlowUnit target = null;
            for (CEventSystemSlowUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
            }

            if (target != null) {
                writer.writeInt(target.getBuffer());
                if (target.getBuffer() != 0 && target.getData() != null) {
                    target.getData().writeInfo(writer);
                }
            } else {
                writer.writeInt(0);
            }
        }
    }
}
