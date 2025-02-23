package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description WazInfoCollection
 */
@Data
public class WazInfoCollection {

    private Integer int1;
    private List<Integer> intList1;
    private List<Integer> intList2;
    private List<Integer> intList3;
    private List<Integer> intList4;
    private Integer int2;

    public WazInfoCollection() {
        intList1 = new ArrayList<>();
        intList2 = new ArrayList<>();
        intList3 = new ArrayList<>();
        intList4 = new ArrayList<>();
    }

    public int readCollection(byte[] bytes, int offset) {

        setInt1(readInt32(bytes, offset));
        offset += 4;

        int count1 = readInt32(bytes, offset);
        offset += 4;
        for (int i = 0; i < count1; i++) {
            intList1.add(readInt32(bytes, offset));
            offset += 4;
        }

        int count2 = readInt32(bytes, offset);
        offset += 4;
        for (int i = 0; i < count2; i++) {
            intList2.add(readInt32(bytes, offset));
            offset += 4;
        }

        int count3 = readInt32(bytes, offset);
        offset += 4;
        for (int i = 0; i < count3; i++) {
            intList3.add(readInt32(bytes, offset));
            offset += 4;
        }

        int count4 = readInt32(bytes, offset);
        offset += 4;
        for (int i = 0; i < count4; i++) {
            intList4.add(readInt32(bytes, offset));
            offset += 4;
        }

        setInt2(readInt32(bytes, offset));
        offset += 4;
        return offset;
    }
}
