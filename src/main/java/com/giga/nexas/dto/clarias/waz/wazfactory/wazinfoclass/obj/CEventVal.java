package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventVal extends SkillInfoObject {
    public String className = "CEventVal";
    public Integer fieldTableAddress = 0x00C363E0;
    private Byte enabled;
    private Integer intField1;
    private Integer intField2;
    private Integer intField3;
    private Integer intField4;

    public CEventVal(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.intField1 = reader.readInt();
        this.intField2 = reader.readInt();
        this.intField3 = reader.readInt();
        this.intField4 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        writer.writeInt(this.intField1);
        writer.writeInt(this.intField2);
        writer.writeInt(this.intField3);
        writer.writeInt(this.intField4);
    }
}
