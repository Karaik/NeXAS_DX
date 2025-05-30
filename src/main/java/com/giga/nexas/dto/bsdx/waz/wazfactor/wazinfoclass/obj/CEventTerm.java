package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description 逆向时该虚函数调用下的字符串信息，一并记录
 * CEventTerm__Read
 */
@Data
@NoArgsConstructor
public class CEventTerm extends SkillInfoObject {

    private List<WazInfoCollection> wazInfoCollectionList = new ArrayList<>();

    public CEventTerm(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.wazInfoCollectionList.clear();
        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        wazInfoCollection.readCollection(reader);
        this.wazInfoCollectionList.add(wazInfoCollection);
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (WazInfoCollection collection : wazInfoCollectionList) {
            collection.writeCollection(writer);
        }
    }

}
