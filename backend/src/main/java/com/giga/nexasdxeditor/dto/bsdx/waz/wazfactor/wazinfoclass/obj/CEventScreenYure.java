package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenYure__Read
 */
@Data
public class CEventScreenYure extends WazInfoObject {

    private List<WazInfoCollection> wazInfoCollectionList;

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    public CEventScreenYure() {
        wazInfoCollectionList = new ArrayList<>();
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        offset = wazInfoCollection.readCollection(bytes, offset);
        wazInfoCollectionList.add(wazInfoCollection);

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;
        setInt4(readInt32(bytes, offset)); offset += 4;

        return offset;
    }
}

