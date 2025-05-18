package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventChange__Read
 */
@Data
@NoArgsConstructor
public class CEventChange extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventChangeType {
        private Integer type;
        private String description;
    }

    private Integer flag;

    private List<BsdxInfoCollection> bsdxInfoCollectionList1 = new ArrayList<>();
    private List<BsdxInfoCollection> bsdxInfoCollectionList2 = new ArrayList<>();

    private Integer int1;

    public CEventChange(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        innerRead(reader);
    }

    private void innerRead(BinaryReader reader) {

        this.flag = reader.readInt();

        this.bsdxInfoCollectionList1.clear();
        this.bsdxInfoCollectionList2.clear();

        if (flag > 0) {

            int counter = 0;
            do {
                BsdxInfoCollection bsdxInfoCollection = new BsdxInfoCollection();
                bsdxInfoCollection.readCollection(reader);  // 使用 BinaryReader 读取数据
                this.bsdxInfoCollectionList1.add(bsdxInfoCollection);

                counter++;
            } while (counter < flag);
        }

        BsdxInfoCollection bsdxInfoCollection2 = new BsdxInfoCollection();
        bsdxInfoCollection2.readCollection(reader);
        this.bsdxInfoCollectionList2.add(bsdxInfoCollection2);

        this.int1 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.flag);
        for (BsdxInfoCollection collection : bsdxInfoCollectionList1) {
            collection.writeCollection(writer);
        }
        for (BsdxInfoCollection collection : bsdxInfoCollectionList2) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
    }

}
