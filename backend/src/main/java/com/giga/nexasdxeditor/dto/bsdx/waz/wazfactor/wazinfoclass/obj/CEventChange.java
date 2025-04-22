package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import com.giga.nexasdxeditor.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

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
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        innerRead(reader);
    }

    private void innerRead(BinaryReader reader) {

        this.flag = reader.readInt();

        this.wazInfoCollectionList1.clear();
        this.wazInfoCollectionList2.clear();

        if (flag > 0) {

            int counter = 0;
            do {
                WazInfoCollection wazInfoCollection = new WazInfoCollection();
                wazInfoCollection.readCollection(reader);  // 使用 BinaryReader 读取数据
                this.wazInfoCollectionList1.add(wazInfoCollection);

                counter++;
            } while (counter < flag);
        }

        WazInfoCollection wazInfoCollection2 = new WazInfoCollection();
        wazInfoCollection2.readCollection(reader);
        this.wazInfoCollectionList2.add(wazInfoCollection2);

        this.int1 = reader.readInt();
    }
}
