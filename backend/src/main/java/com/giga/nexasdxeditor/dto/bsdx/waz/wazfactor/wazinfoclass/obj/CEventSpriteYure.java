package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description
 * CEventSpriteYure__Read
 */
@Data
public class CEventSpriteYure extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventSpriteYureType {
        private Integer type;
        private String description;
    }

    public static final CEventSpriteYureType[] CEVENT_SPRITE_YURE_TYPES = {
            new CEventSpriteYureType(0xFFFFFFFF, "角度指定"),
            new CEventSpriteYureType(0xFFFFFFFF, "左回転"),
            new CEventSpriteYureType(0xFFFFFFFF, "右回転"),
            new CEventSpriteYureType(0xFFFFFFFF, "下"),
            new CEventSpriteYureType(0xFFFFFFFF, "上"),
            new CEventSpriteYureType(0xFFFFFFFF, "左"),
            new CEventSpriteYureType(0xFFFFFFFF, "右"),
            new CEventSpriteYureType(0xFFFFFFFF, "ランダム")
    };

    public static final String[] CEVENT_SPRITE_YURE_FORMATS = {
            "%s : %4d (周期 : %4d)",
            "%s : %4d ",
            "⇒ %4d (周期 : %4d)"
    };


    private List<WazInfoCollection> wazInfoCollectionList = new ArrayList<>();

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.wazInfoCollectionList.clear();
        WazInfoCollection wazInfoCollection = new WazInfoCollection();
        wazInfoCollection.readCollection(reader);
        this.wazInfoCollectionList.add(wazInfoCollection);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (WazInfoCollection collection : wazInfoCollectionList) {
            collection.writeCollection(writer);
        }
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
    }

}
