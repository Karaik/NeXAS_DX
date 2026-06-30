package com.giga.nexas.dto.clarias.mek.mekcpu;

import com.giga.nexas.dto.clarias.ClariasInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
public class CCpuEventChange extends CCpuEvent {

    private List<ClariasInfoCollection> termList = new ArrayList<>();
    private ClariasInfoCollection term = new ClariasInfoCollection();
    private Integer termChangeValue;
    private Integer changeValue;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.termList.clear();
        int termCount = reader.readInt();
        for (int i = 0; i < termCount; i++) {
            ClariasInfoCollection item = new ClariasInfoCollection();
            item.readCollection(reader);
            this.termList.add(item);
        }

        this.term = new ClariasInfoCollection();
        this.term.readCollection(reader);
        this.termChangeValue = reader.readInt();
        this.changeValue = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);

        writer.writeInt(this.termList.size());
        for (ClariasInfoCollection item : this.termList) {
            item.writeCollection(writer);
        }

        this.term.writeCollection(writer);
        writer.writeInt(this.termChangeValue);
        writer.writeInt(this.changeValue);
    }
}
