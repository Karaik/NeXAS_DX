package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenAttr__Read
 */
@Data
@NoArgsConstructor
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

    public CEventScreenAttr(Integer typeId) {
        super(typeId);
    }

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
    }

}

