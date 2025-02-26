package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventChange__Read
 */
@Data
public class CEventChange extends WazInfoObject {

    private Integer flag;

    private List<WazInfoCollection> wazInfoCollectionList1;
    private List<WazInfoCollection> wazInfoCollectionList2;

    private Integer int1;

    public CEventChange() {
        wazInfoCollectionList1 = new ArrayList<>();
        wazInfoCollectionList2 = new ArrayList<>();
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        offset = innerRead(bytes, offset);

        return offset;
    }

    private int innerRead(byte[] bytes, int offset) {

        int flag = readInt32(bytes, offset); offset += 4;
        setFlag(flag);

        if (flag > 0) {

            int counter = 0;
            do {
                WazInfoCollection wazInfoCollection1 = new WazInfoCollection();
                offset = wazInfoCollection1.readCollection(bytes, offset);
                wazInfoCollectionList1.add(wazInfoCollection1);

                counter++;
            } while (counter < flag);

        }

        WazInfoCollection wazInfoCollection2 = new WazInfoCollection();
        offset = wazInfoCollection2.readCollection(bytes, offset);
        wazInfoCollectionList2.add(wazInfoCollection2);

        setInt1(readInt32(bytes, offset)); offset += 4;

        return offset;
    }
}
