package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description
 * CEventSpriteAttr__Read
 */
@Data
public class CEventSpriteAttr extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventSpriteAttrType {
        private Integer type;
        private String description;
    }

    public static final CEventSpriteAttrType[] CEVENT_SPRITE_ATTR_TYPES = {
            new CEventSpriteAttrType(0xFFFFFFFF, "通常描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "塗りつぶし描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "モノトーン描画（未実装）"),
            new CEventSpriteAttrType(0xFFFFFFFF, "フィルム描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "メッシュ描画（未実装）"),
            new CEventSpriteAttrType(0xFFFFFFFF, "フェード描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "フラッシュ描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "半透明描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "アルファブレンド描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "加算描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "減算描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "フェード色指定描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "フラッシュ色指定描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "モノトーン色指定描画（未実装）"),
            new CEventSpriteAttrType(0xFFFFFFFF, "加算色指定描画"),
            new CEventSpriteAttrType(0xFFFFFFFF, "減算色指定描画（未実装）"),
            new CEventSpriteAttrType(0xFFFFFFFF, "ネガティブ描画（未実装）"),
            new CEventSpriteAttrType(0xFFFFFFFF, "ぼかし描画（未実装）"),
            new CEventSpriteAttrType(0xFFFFFFFF, "マスク描画")
    };

    public static final String[] CEVENT_SPRITE_ATTR_FORMATS = {
            "%s",
            "%s : %4d",
            "%s : %4d ",
            "⇒ %4d",
            "%s : (%3d:%3d:%3d)",
            "%s : (%3d:%3d:%3d) ",
            "⇒ (%3d:%3d:%3d)"
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private Integer int11;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.int7 = reader.readInt();
        this.int8 = reader.readInt();
        this.int9 = reader.readInt();
        this.int10 = reader.readInt();
        this.int11 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
        writer.writeInt(this.int7);
        writer.writeInt(this.int8);
        writer.writeInt(this.int9);
        writer.writeInt(this.int10);
        writer.writeInt(this.int11);
    }

}
