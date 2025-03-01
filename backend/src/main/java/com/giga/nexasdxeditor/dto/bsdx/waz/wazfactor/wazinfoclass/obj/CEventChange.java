package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
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

    @Data
    @AllArgsConstructor
    public static class CEventChangeType {
        private Integer type;
        private String description;
    }

    public static final CEventChangeType[] CEVENT_CHANGE_ENTRIES = {
            // .rdata:007F2048
            new CEventChangeType(0x0, "消費する熱チャージ量"),
            new CEventChangeType(0x0, "蓄積する溜め")
    };

    private Integer flag;

    private List<WazInfoCollection> wazInfoCollectionList1;
    private List<WazInfoCollection> wazInfoCollectionList2;

    private Integer int1;

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
                List<WazInfoCollection> wazInfoCollectionList1 = new ArrayList<>();
                WazInfoCollection wazInfoCollection = new WazInfoCollection();
                offset = wazInfoCollection.readCollection(bytes, offset);
                wazInfoCollectionList1.add(wazInfoCollection);
                setWazInfoCollectionList1(wazInfoCollectionList1);

                counter++;
            } while (counter < flag);

        }

        List<WazInfoCollection> wazInfoCollectionList2 = new ArrayList<>();
        WazInfoCollection wazInfoCollection2 = new WazInfoCollection();
        offset = wazInfoCollection2.readCollection(bytes, offset);
        wazInfoCollectionList2.add(wazInfoCollection2);
        setWazInfoCollectionList2(wazInfoCollectionList2);

        setInt1(readInt32(bytes, offset)); offset += 4;

        return offset;
    }
}
