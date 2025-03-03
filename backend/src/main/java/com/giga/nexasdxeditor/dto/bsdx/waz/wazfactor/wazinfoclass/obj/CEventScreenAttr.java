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
 * CEventScreenAttr__Read
 */
@Data
public class CEventScreenAttr extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventScreenAttrType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenAttrType[] CEVENT_SCREEN_ATTR_TYPES = {
            new CEventScreenAttrType(0xFFFFFFFF, "持続カウンタ"),
            new CEventScreenAttrType(0xFFFFFFFF, "タイプ"),
            new CEventScreenAttrType(0xFFFFFFFF, "速度1"),
            new CEventScreenAttrType(0xFFFFFFFF, "速度2"),
            new CEventScreenAttrType(0x9, "方向"),
            new CEventScreenAttrType(0xFFFFFFFF, "スピード"),
            new CEventScreenAttrType(0xFFFFFFFF, "炎"),
            new CEventScreenAttrType(0xFFFFFFFF, "爆発")
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

