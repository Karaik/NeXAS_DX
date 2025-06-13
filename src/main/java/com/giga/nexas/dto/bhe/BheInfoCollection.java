package com.giga.nexas.dto.bhe;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description BheInfoCollection
 * 后续逆向发现不单独是针对waz文件的集合，ai部分也有用到
 */
@Data
public class BheInfoCollection {

    private Integer int1;
    private List<Integer> intList1;
    private List<Integer> intList2;
    private List<Integer> intList3;
    private List<Integer> intList4;
    private Integer int2;

    public BheInfoCollection() {
        intList1 = new ArrayList<>();
        intList2 = new ArrayList<>();
        intList3 = new ArrayList<>();
        intList4 = new ArrayList<>();
    }

    public void readCollection(BinaryReader reader) {

        setInt1(reader.readInt());

        int count1 = reader.readInt();
        for (int i = 0; i < count1; i++) {
            intList1.add(reader.readInt());
        }

        int count2 = reader.readInt();
        for (int i = 0; i < count2; i++) {
            intList2.add(reader.readInt());
        }

        int count3 = reader.readInt();
        for (int i = 0; i < count3; i++) {
            intList3.add(reader.readInt());
        }

        int count4 = reader.readInt();
        for (int i = 0; i < count4; i++) {
            intList4.add(reader.readInt());
        }

        setInt2(reader.readInt());
    }

    public void writeCollection(BinaryWriter writer) throws  IOException {
        writer.writeInt(this.int1);

        writer.writeInt(intList1.size());
        for (Integer val : intList1) {
            writer.writeInt(val);
        }

        writer.writeInt(intList2.size());
        for (Integer val : intList2) {
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

        writer.writeInt(this.int2);
    }

}
