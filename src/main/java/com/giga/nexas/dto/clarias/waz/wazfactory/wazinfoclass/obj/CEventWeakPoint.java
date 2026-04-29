package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.exception.OperationException;
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
public class CEventWeakPoint extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventWeakPointType {
        private Integer type;
        private String description;
    }

    public static final CEventWeakPointType[] CEVENT_WEAK_POINT_ENTRIES = {
            new CEventWeakPointType(0xFFFFFFFF, "弱点タイプ"),
            new CEventWeakPointType(0x1, "必要ポイント"),
            new CEventWeakPointType(0x1, "加算ポイント")
    };

    @Data
    public static class CEventWeakPointUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Byte byte1;
    private Integer int1;
    private Integer int2;
    private List<CEventWeakPointUnit> unitList = new ArrayList<>();

    public CEventWeakPoint(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.byte1 = reader.readByte();
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();

        this.unitList.clear();
        for (int i = 0; i < 3; i++) {
            int buffer = reader.readInt();

            CEventWeakPointUnit unit = new CEventWeakPointUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_WEAK_POINT_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_WEAK_POINT_ENTRIES[i].getType();
            if (buffer != 0) {
                if (innerTypeId == 0xFFFFFFFF) {
                    throw new OperationException(500, "unexpected non-zero weakPoint wrapper buffer at slot " + i);
                }
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                if (obj == null) {
                    throw new OperationException(500, "missing weakPoint wrapper type at slot " + i + ": " + innerTypeId);
                }
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
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);

        for (int i = 0; i < 3; i++) {
            CEventWeakPointUnit target = null;
            for (CEventWeakPointUnit unit : this.unitList) {
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
