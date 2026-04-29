package com.giga.nexas.dto.clarias;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
public class ClariasInfoCollection {

    private Integer int1;
    private List<Integer> typeList;
    private List<Integer> paramList;
    private List<Integer> intList3;
    private List<Integer> intList4;
    private List<IntPair> pairList;
    private Integer int2;

    @Data
    public static class IntPair {
        private Integer intField1;
        private Integer intField2;
    }

    public ClariasInfoCollection() {
        this.typeList = new ArrayList<>();
        this.paramList = new ArrayList<>();
        this.intList3 = new ArrayList<>();
        this.intList4 = new ArrayList<>();
        this.pairList = new ArrayList<>();
    }

    public void readCollection(BinaryReader reader) {
        setInt1(reader.readInt());

        int count1 = reader.readInt();
        for (int i = 0; i < count1; i++) {
            typeList.add(reader.readInt());
        }

        int count2 = reader.readInt();
        for (int i = 0; i < count2; i++) {
            paramList.add(reader.readInt());
        }

        int count3 = reader.readInt();
        for (int i = 0; i < count3; i++) {
            intList3.add(reader.readInt());
        }

        int count4 = reader.readInt();
        for (int i = 0; i < count4; i++) {
            intList4.add(reader.readInt());
        }

        int pairCount = reader.readInt();
        this.pairList = new ArrayList<>();
        for (int i = 0; i < pairCount; i++) {
            IntPair pair = new IntPair();
            pair.setIntField1(reader.readInt());
            pair.setIntField2(reader.readInt());
            this.pairList.add(pair);
        }

        setInt2(reader.readInt());
    }

    public void writeCollection(BinaryWriter writer) throws IOException {
        writer.writeInt(this.int1);

        writer.writeInt(typeList.size());
        for (Integer val : typeList) {
            writer.writeInt(val);
        }

        writer.writeInt(paramList.size());
        for (Integer val : paramList) {
            writer.writeInt(val);
        }

        writer.writeInt(intList3.size());
        for (Integer val : intList3) {
            writer.writeInt(val);
        }

        writer.writeInt(intList4.size());
        for (Integer val : intList4) {
            writer.writeInt(val);
        }

        writer.writeInt(pairList.size());
        for (IntPair pair : pairList) {
            writer.writeInt(pair.getIntField1());
            writer.writeInt(pair.getIntField2());
        }

        writer.writeInt(this.int2);
    }
}
