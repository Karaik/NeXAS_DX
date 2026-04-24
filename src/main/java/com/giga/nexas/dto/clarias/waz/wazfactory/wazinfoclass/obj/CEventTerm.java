package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventTerm extends SkillInfoObject {
    public String className = "CEventTerm";
    public Integer fieldTableAddress = 0x00C35008;
    private Byte enabled;
    private CTerm term = new CTerm();

    public CEventTerm(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.term = new CTerm();
        this.term.readInfo(reader);
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        this.term.writeInfo(writer);
    }
}
