package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bhe.BheInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description 逆向时该虚函数调用下的字符串信息，一并记录
 * CEventTerm__Read
 */
@Data
@NoArgsConstructor
public class CEventTerm extends SkillInfoObject {

    private List<BheInfoCollection> bheInfoCollectionList = new ArrayList<>();

    public CEventTerm(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.bheInfoCollectionList.clear();
        BheInfoCollection bheInfoCollection = new BheInfoCollection();
        bheInfoCollection.readCollection(reader);
        this.bheInfoCollectionList.add(bheInfoCollection);
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (BheInfoCollection collection : bheInfoCollectionList) {
            collection.writeCollection(writer);
        }
    }

}
