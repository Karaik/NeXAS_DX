package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventWazaSelectSimple extends SkillInfoObject {
    public String className = "CEventWazaSelect";
    public Integer fieldTableAddress = 0x00C36E08;
    private Byte enabled;
    private Byte byteField1;
    private Byte byteField2;

    public CEventWazaSelectSimple(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.byteField1 = reader.readByte();
        this.byteField2 = reader.readByte();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        writer.writeByte(this.byteField1);
        writer.writeByte(this.byteField2);
    }
}
