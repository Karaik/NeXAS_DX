package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenAttr__Read
 */
@Data
public class CEventScreenAttr extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventScreenAttrType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenAttrType[] CEVENT_SCREEN_ATTR_TYPES = {
            new CEventScreenAttrType(0xFFFFFFFF, "フェード描画"),
            new CEventScreenAttrType(0xFFFFFFFF, "フラッシュ描画"),
            new CEventScreenAttrType(0xFFFFFFFF, "フェード色指定描画"),
            new CEventScreenAttrType(0xFFFFFFFF, "フラッシュ色指定描画"),
            new CEventScreenAttrType(0xFFFFFFFF, "ネガティブ描画"),
            new CEventScreenAttrType(0xFFFFFFFF, "アルファ塗りつぶし描画"),
            new CEventScreenAttrType(0xFFFFFFFF, "加算塗りつぶし描画"),
            new CEventScreenAttrType(0xFFFFFFFF, "減算塗りつぶし描画")
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

        return offset;
    }
}

