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
public class CEventChange extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventChangeType {
        private Integer type;
        private String description;
    }

    private Integer flag;

    private List<WazInfoCollection> wazInfoCollectionList1 = new ArrayList<>();
    private List<WazInfoCollection> wazInfoCollectionList2 = new ArrayList<>();

    private Integer int1;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        offset = innerRead(bytes, offset);

        return offset;
    }

    private int innerRead(byte[] bytes, int offset) {

        this.flag = readInt32(bytes, offset); offset += 4;

        this.wazInfoCollectionList1.clear();
        this.wazInfoCollectionList2.clear();

        if (flag > 0) {

            int counter = 0;
            do {
                WazInfoCollection wazInfoCollection = new WazInfoCollection();
                offset = wazInfoCollection.readCollection(bytes, offset);
                this.wazInfoCollectionList1.add(wazInfoCollection);

                counter++;
            } while (counter < flag);

        }

        WazInfoCollection wazInfoCollection2 = new WazInfoCollection();
        offset = wazInfoCollection2.readCollection(bytes, offset);
        this.wazInfoCollectionList2.add(wazInfoCollection2);

        this.int1 = readInt32(bytes, offset); offset += 4;

        return offset;
    }
}
