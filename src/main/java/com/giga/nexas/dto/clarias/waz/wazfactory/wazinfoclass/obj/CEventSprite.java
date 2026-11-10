package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventSprite extends SkillInfoObject {

    private Integer spmFileSequence;
    private Integer actionGroupNumber;
    private Integer actionNumber;

    public CEventSprite(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.spmFileSequence = reader.readInt();
        this.actionGroupNumber = reader.readInt();
        this.actionNumber = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.spmFileSequence);
        writer.writeInt(this.actionGroupNumber);
        writer.writeInt(this.actionNumber);
    }
}
