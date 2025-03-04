package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description
 * CEventSpriteAttr__Read
 */
@Data
public class CEventSpriteAttr extends WazInfoObject {

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
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;
        this.int4 = readInt32(bytes, offset); offset += 4;
        this.int5 = readInt32(bytes, offset); offset += 4;
        this.int6 = readInt32(bytes, offset); offset += 4;
        this.int7 = readInt32(bytes, offset); offset += 4;
        this.int8 = readInt32(bytes, offset); offset += 4;
        this.int9 = readInt32(bytes, offset); offset += 4;
        this.int10 = readInt32(bytes, offset); offset += 4;
        this.int11 = readInt32(bytes, offset); offset += 4;

        return offset;
    }
}
