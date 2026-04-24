package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeClarias;

@Data
@NoArgsConstructor
public class CEventStatus extends SkillInfoObject {
    private byte[] byteData1;
    private byte[] byteData2;
    private Byte byte1;
    private List<Integer> intFields = new ArrayList<>();
    private List<CEventStatusUnit> statusUnitList = new ArrayList<>();

    public CEventStatus(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventStatusUnit {
        public Integer unitSlotNum;
        public Integer typeId;
        private Integer buffer;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.byteData1 = reader.readBytes(15);
        this.byteData2 = reader.readBytes(60);
        this.byte1 = reader.readByte();
        this.intFields = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            this.intFields.add(reader.readInt());
        }
        this.statusUnitList = new ArrayList<>();
        for (int i = 0; i < 25; i++) {
            int buffer = reader.readInt();
            CEventStatusUnit unit = new CEventStatusUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            int subTypeId = statusSubType(i);
            unit.setTypeId(subTypeId);
            if (buffer != 0 && subTypeId >= 0) {
                SkillInfoObject obj = createCEventObjectByTypeClarias(subTypeId);
                obj.readInfo(reader);
                unit.setData(obj);
                this.statusUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeBytes(this.byteData1);
        writer.writeBytes(this.byteData2);
        writer.writeByte(this.byte1);
        for (Integer value : this.intFields) {
            writer.writeInt(value);
        }
        for (int i = 0; i < 25; i++) {
            CEventStatusUnit target = null;
            for (CEventStatusUnit unit : this.statusUnitList) {
                if (unit.getUnitSlotNum() != null && unit.getUnitSlotNum() == i) {
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

    private static int statusSubType(int slot) {
        return slot == 24 ? 0x6 : -1;
    }
}