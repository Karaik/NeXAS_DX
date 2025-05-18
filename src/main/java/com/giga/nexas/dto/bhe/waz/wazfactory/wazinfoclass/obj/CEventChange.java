package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bhe.BheInfoCollection;
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
 * @Date 2025/5/18
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

    private List<BheInfoCollection> bheInfoCollectionList1 = new ArrayList<>();
    private List<BheInfoCollection> bheInfoCollectionList2 = new ArrayList<>();

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

        this.bheInfoCollectionList1.clear();
        this.bheInfoCollectionList2.clear();

        if (flag > 0) {

            int counter = 0;
            do {
                BheInfoCollection bheInfoCollection = new BheInfoCollection();
                bheInfoCollection.readCollection(reader);  // 使用 BinaryReader 读取数据
                this.bheInfoCollectionList1.add(bheInfoCollection);

                counter++;
            } while (counter < flag);
        }

        BheInfoCollection bheInfoCollection2 = new BheInfoCollection();
        bheInfoCollection2.readCollection(reader);
        this.bheInfoCollectionList2.add(bheInfoCollection2);

        this.int1 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.flag);
        for (BheInfoCollection collection : bheInfoCollectionList1) {
            collection.writeCollection(writer);
        }
        for (BheInfoCollection collection : bheInfoCollectionList2) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
    }

}
