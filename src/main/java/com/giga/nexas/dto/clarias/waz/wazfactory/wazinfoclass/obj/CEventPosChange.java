package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


@Data
@NoArgsConstructor
public class CEventPosChange extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventPosChangeType {
        private Integer type;
        private String description;
    }

    public static final CEventPosChangeType[] CEVENT_POS_CHANGE_ENTRIES = {
            new CEventPosChangeType(0xFFFFFFFF, "開始位置X"),
            new CEventPosChangeType(0xFFFFFFFF, "開始位置Y"),
            new CEventPosChangeType(0xFFFFFFFF, "開始位置"),
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;

    @Data
    public static class CEventPosChangeUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventPosChangeUnit> unitList = new ArrayList<>();

    public CEventPosChange(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();

        this.unitList.clear();
        for (int i = 0; i < 3; i++) {
            int buffer = reader.readInt();

            CEventPosChangeUnit unit = new CEventPosChangeUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_POS_CHANGE_ENTRIES[i].getDescription());

            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);

        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);

        for (int i = 0; i < 3; i++) {
            CEventPosChangeUnit target = null;
            for (CEventPosChangeUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
            }

            writer.writeInt(target.getBuffer());
        }
    }
}
