package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

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
 * CEventScreenYure__Read
 */
@Data
@NoArgsConstructor
public class CEventScreenYure extends SkillInfoObject {

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


    private List<BsdxInfoCollection> bsdxInfoCollectionList = new ArrayList<>();

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    public CEventScreenYure(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.bsdxInfoCollectionList.clear();

        BsdxInfoCollection bsdxInfoCollection = new BsdxInfoCollection();
        bsdxInfoCollection.readCollection(reader);
        this.bsdxInfoCollectionList.add(bsdxInfoCollection);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (BsdxInfoCollection collection : bsdxInfoCollectionList) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
    }


}

