package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventTermChange extends SkillInfoObject {
    public String className = "CEventTermChange";
    public Integer ctorAddress = 0x008E8AA0;
    private Byte enabled;
    private CTermChange termChange = new CTermChange();

    public CEventTermChange(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.termChange.readInfo(reader);
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        this.termChange.writeInfo(writer);
    }
}