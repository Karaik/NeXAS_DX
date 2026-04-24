package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class SkillInfoUnknown extends SkillInfoObject {
    public Integer offset;
    private Integer startFrame;
    private Integer endFrame;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Byte byteField;
    public Integer intFieldCount = 3;
    public boolean hasByteField = false;

    public SkillInfoUnknown(Integer typeId) {
        super(typeId);
    }

    public SkillInfoUnknown(Integer typeId, int intFieldCount, boolean hasByteField) {
        super(typeId);
        this.intFieldCount = intFieldCount;
        this.hasByteField = hasByteField;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        this.offset = reader.getPosition();
        this.startFrame = reader.readInt();
        this.endFrame = reader.readInt();
        if (this.intFieldCount >= 1) {
            this.int1 = reader.readInt();
        }
        if (this.intFieldCount >= 2) {
            this.int2 = reader.readInt();
        }
        if (this.intFieldCount >= 3) {
            this.int3 = reader.readInt();
        }
        if (this.hasByteField) {
            this.byteField = reader.readByte();
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        writer.writeInt(this.startFrame);
        writer.writeInt(this.endFrame);
        if (this.intFieldCount >= 1) {
            writer.writeInt(this.int1);
        }
        if (this.intFieldCount >= 2) {
            writer.writeInt(this.int2);
        }
        if (this.intFieldCount >= 3) {
            writer.writeInt(this.int3);
        }
        if (this.hasByteField) {
            writer.writeByte(this.byteField);
        }
    }
}
