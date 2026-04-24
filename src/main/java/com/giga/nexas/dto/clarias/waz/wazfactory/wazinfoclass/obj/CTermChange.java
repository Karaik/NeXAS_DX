package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CTermChange {
    private List<CTerm> termList = new ArrayList<>();
    private CTerm defaultTerm = new CTerm();
    private Integer intField1;

    public void readInfo(BinaryReader reader) {
        int count = reader.readInt();
        this.termList = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            CTerm term = new CTerm();
            term.readInfo(reader);
            this.termList.add(term);
        }
        this.defaultTerm = new CTerm();
        this.defaultTerm.readInfo(reader);
        this.intField1 = reader.readInt();
    }

    public void writeInfo(BinaryWriter writer) throws IOException {
        writer.writeInt(this.termList.size());
        for (CTerm term : this.termList) {
            term.writeInfo(writer);
        }
        this.defaultTerm.writeInfo(writer);
        writer.writeInt(this.intField1);
    }
}