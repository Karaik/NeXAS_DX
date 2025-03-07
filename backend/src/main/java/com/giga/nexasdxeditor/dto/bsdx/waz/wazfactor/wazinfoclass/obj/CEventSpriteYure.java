package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

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
