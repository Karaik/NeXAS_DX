package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventWazaSelect extends SkillInfoObject {

    private Integer wazFileNo;
    private Integer wazSequenceNo;

    public CEventWazaSelect(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.wazFileNo = reader.readInt();
        this.wazSequenceNo = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.wazFileNo);
        writer.writeInt(this.wazSequenceNo);
    }
}
