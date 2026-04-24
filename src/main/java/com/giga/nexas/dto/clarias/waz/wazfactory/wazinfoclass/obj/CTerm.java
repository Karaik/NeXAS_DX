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
public class CTerm {
    private Integer intField1;
    private List<Integer> intList1 = new ArrayList<>();
    private List<Integer> intList2 = new ArrayList<>();
    private List<Integer> intList3 = new ArrayList<>();
    private List<Integer> intList4 = new ArrayList<>();
    private List<IntPair> pairList = new ArrayList<>();
    private Integer intField2;

    @Data
    @NoArgsConstructor
    public static class IntPair {
        private Integer intField1;
        private Integer intField2;
    }

    public void readInfo(BinaryReader reader) {
        this.intField1 = reader.readInt();
        this.intList1 = readIntList(reader);
        this.intList2 = readIntList(reader);
        this.intList3 = readIntList(reader);
        this.intList4 = readIntList(reader);
        int pairCount = reader.readInt();
        this.pairList = new ArrayList<>();
        for (int i = 0; i < pairCount; i++) {
            IntPair pair = new IntPair();
            pair.setIntField1(reader.readInt());
            pair.setIntField2(reader.readInt());
            this.pairList.add(pair);
        }
        this.intField2 = reader.readInt();
    }

    public void writeInfo(BinaryWriter writer) throws IOException {
        writer.writeInt(this.intField1);
        writeIntList(writer, this.intList1);
        writeIntList(writer, this.intList2);
        writeIntList(writer, this.intList3);
        writeIntList(writer, this.intList4);
        writer.writeInt(this.pairList.size());
        for (IntPair pair : this.pairList) {
            writer.writeInt(pair.getIntField1());
            writer.writeInt(pair.getIntField2());
        }
        writer.writeInt(this.intField2);
    }

    private static List<Integer> readIntList(BinaryReader reader) {
        int count = reader.readInt();
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(reader.readInt());
        }
        return values;
    }

    private static void writeIntList(BinaryWriter writer, List<Integer> values) throws IOException {
        writer.writeInt(values.size());
        for (Integer value : values) {
            writer.writeInt(value);
        }
    }
}