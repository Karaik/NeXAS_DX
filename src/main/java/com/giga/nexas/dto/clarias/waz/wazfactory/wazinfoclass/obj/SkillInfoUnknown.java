package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class SkillInfoUnknown extends SkillInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Byte byteField; //diff
    public Integer intFieldCount = 3; //diff
    public boolean hasByteField = false; //diff

    public SkillInfoUnknown(Integer typeId) {
        super(typeId);
    }

    public SkillInfoUnknown(Integer typeId, int intFieldCount, boolean hasByteField) { //diff
        super(typeId);
        this.intFieldCount = intFieldCount;
        this.hasByteField = hasByteField;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        this.offset = reader.getPosition(); //diff
        this.setStartFrame(reader.readInt()); //diff
        this.setEndFrame(reader.readInt()); //diff

        if (this.intFieldCount >= 1) { //diff
            this.int1 = reader.readInt();
        }
        if (this.intFieldCount >= 2) { //diff
            this.int2 = reader.readInt();
        }
        if (this.intFieldCount >= 3) { //diff
            this.int3 = reader.readInt();
        }
        if (this.hasByteField) { //diff
            this.byteField = reader.readByte();
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        writer.writeInt(this.getStartFrame()); //diff
        writer.writeInt(this.getEndFrame()); //diff
        if (this.intFieldCount >= 1) { //diff
            writer.writeInt(this.int1);
        }
        if (this.intFieldCount >= 2) { //diff
            writer.writeInt(this.int2);
        }
        if (this.intFieldCount >= 3) { //diff
            writer.writeInt(this.int3);
        }
        if (this.hasByteField) { //diff
            writer.writeByte(this.byteField);
        }
    }

}
