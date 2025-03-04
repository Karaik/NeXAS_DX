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
 * CEventScreenYure__Read
 */
@Data
public class CEventScreenYure extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventScreenYureType {
        private String description;
    }

    public static final CEventScreenYureType[] CEVENT_SCREEN_YURE_TYPES = {
            new CEventScreenYureType("角度指定"),
            new CEventScreenYureType("左回転"),
            new CEventScreenYureType("右回転"),
            new CEventScreenYureType("下"),
            new CEventScreenYureType("上"),
            new CEventScreenYureType("左"),
            new CEventScreenYureType("右"),
            new CEventScreenYureType("ランダム")
    };


    private List<WazInfoCollection> wazInfoCollectionList = new ArrayList<>();

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.wazInfoCollectionList.clear();

        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        offset = wazInfoCollection.readCollection(bytes, offset);
        this.wazInfoCollectionList.add(wazInfoCollection);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;
        this.int4 = readInt32(bytes, offset); offset += 4;

        return offset;
    }
}

