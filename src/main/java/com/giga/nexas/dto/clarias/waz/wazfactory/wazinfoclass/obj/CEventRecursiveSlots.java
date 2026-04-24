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
public class CEventRecursiveSlots extends SkillInfoObject {
    public String className;
    public Integer fieldTableAddress;
    private Byte enabled;
    private byte[] dataBytes;
    private List<RecursiveUnit> recursiveUnitList = new ArrayList<>();
    private int[] subTypeIds;

    public CEventRecursiveSlots(Integer typeId, String className, Integer fieldTableAddress, int dataLength, int[] subTypeIds) {
        super(typeId);
        this.className = className;
        this.fieldTableAddress = fieldTableAddress;
        this.dataBytes = new byte[dataLength];
        this.subTypeIds = subTypeIds;
    }

    @Data
    @NoArgsConstructor
    public static class RecursiveUnit {
        public Integer unitSlotNum;
        public Integer typeId;
        private Integer buffer;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.dataBytes = reader.readBytes(this.dataBytes.length);
        this.recursiveUnitList = new ArrayList<>();
        for (int i = 0; i < this.subTypeIds.length; i++) {
            int buffer = reader.readInt();
            RecursiveUnit unit = new RecursiveUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            int subTypeId = this.subTypeIds[i];
            unit.setTypeId(subTypeId);
            if (buffer != 0 && subTypeId >= 0) {
                SkillInfoObject obj = createCEventObjectByTypeClarias(subTypeId);
                obj.setSlotNum(i);
                obj.readInfo(reader);
                unit.setData(obj);
            }
            if (buffer != 0 || unit.getData() != null) {
                this.recursiveUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        writer.writeBytes(this.dataBytes);
        for (int i = 0; i < this.subTypeIds.length; i++) {
            RecursiveUnit unit = findUnit(i);
            if (unit == null) {
                writer.writeInt(0);
                continue;
            }
            writer.writeInt(unit.getBuffer());
            if (unit.getBuffer() != 0 && unit.getData() != null) {
                unit.getData().writeInfo(writer);
            }
        }
    }

    private RecursiveUnit findUnit(int slot) {
        for (RecursiveUnit unit : this.recursiveUnitList) {
            if (unit.getUnitSlotNum() != null && unit.getUnitSlotNum() == slot) {
                return unit;
            }
        }
        return null;
    }
}
