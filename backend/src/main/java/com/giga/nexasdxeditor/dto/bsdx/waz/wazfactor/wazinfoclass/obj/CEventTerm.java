package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description 逆向时该虚函数调用下的字符串信息，一并记录
 * CEventTerm__Read
 */
@Data
public class CEventTerm extends WazInfoObject {

    private List<WazInfoCollection> wazInfoCollectionList = new ArrayList<>();

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.wazInfoCollectionList.clear();
        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        offset = wazInfoCollection.readCollection(bytes, offset);
        this.wazInfoCollectionList.add(wazInfoCollection);

        return offset;
    }
}
