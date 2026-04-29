package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.clarias.ClariasInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CEventPlayerChange extends SkillInfoObject {

    private Integer flag;
    private List<ClariasInfoCollection> clariasInfoCollectionList1 = new ArrayList<>();
    private List<ClariasInfoCollection> clariasInfoCollectionList2 = new ArrayList<>();
    private Integer int1;

    public CEventPlayerChange(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.flag = reader.readInt();
        this.clariasInfoCollectionList1.clear();
        this.clariasInfoCollectionList2.clear();

        if (this.flag > 0) {
            int counter = 0;
            do {
                ClariasInfoCollection collection = new ClariasInfoCollection();
                collection.readCollection(reader);
                this.clariasInfoCollectionList1.add(collection);
                counter++;
            } while (counter < this.flag);
        }

        ClariasInfoCollection collection2 = new ClariasInfoCollection();
        collection2.readCollection(reader);
        this.clariasInfoCollectionList2.add(collection2);

        this.int1 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.flag);
        for (ClariasInfoCollection collection : this.clariasInfoCollectionList1) {
            collection.writeCollection(writer);
        }
        for (ClariasInfoCollection collection : this.clariasInfoCollectionList2) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
    }
}
