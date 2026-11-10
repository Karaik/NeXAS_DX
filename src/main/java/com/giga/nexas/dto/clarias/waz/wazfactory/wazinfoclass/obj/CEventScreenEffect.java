package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.clarias.ClariasInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventScreenEffect extends SkillInfoObject {

    public Integer fieldTableAddress = 0x00C36CA0;

    // field table: type=2 (InfoCollection) + 4 × type=0 param=4 (int)
    private ClariasInfoCollection infoCollection = new ClariasInfoCollection();
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    public CEventScreenEffect(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.infoCollection = new ClariasInfoCollection();
        this.infoCollection.readCollection(reader);
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        this.infoCollection.writeCollection(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
    }
}
