package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventBlink extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventBlinkType {
        private Integer type;
        private String description;
    }

    public static final CEventBlinkType[] CEVENT_BLINK_ENTRIES = {
            new CEventBlinkType(0xFFFFFFFF, "残像タイプ"),
            new CEventBlinkType(0xFFFFFFFF, "発射間隔"),
            new CEventBlinkType(0xFFFFFFFF, "アニメ時間"),
            new CEventBlinkType(0xFFFFFFFF, "描画優先順位(相対)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:描画タイプ"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値(R)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値(G)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始描画値(B)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値(R)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値(G)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了描画値(B)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:開始拡大率(0未満無視)"),
            new CEventBlinkType(0xFFFFFFFF, "詳細:終了拡大率(0未満無視)"),
            new CEventBlinkType(0xFFFFFFFF, "フラグ")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4; //diff
    private Short short1; //diff
    private Integer int5; //diff
    private Integer int6; //diff
    private Byte byte1; //diff
    private Byte byte2; //diff
    private Byte byte3; //diff
    private Integer int7; //diff
    private Byte byte4; //diff
    private Byte byte5; //diff
    private Byte byte6; //diff
    private Integer int8; //diff
    private Integer int9; //diff

    public CEventBlink(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt(); //diff
        this.short1 = reader.readShort(); //diff
        this.int5 = reader.readInt(); //diff
        this.int6 = reader.readInt(); //diff
        this.byte1 = reader.readByte(); //diff
        this.byte2 = reader.readByte(); //diff
        this.byte3 = reader.readByte(); //diff
        this.int7 = reader.readInt(); //diff
        this.byte4 = reader.readByte(); //diff
        this.byte5 = reader.readByte(); //diff
        this.byte6 = reader.readByte(); //diff
        this.int8 = reader.readInt(); //diff
        this.int9 = reader.readInt(); //diff
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4); //diff
        writer.writeShort(this.short1); //diff
        writer.writeInt(this.int5); //diff
        writer.writeInt(this.int6); //diff
        writer.writeByte(this.byte1); //diff
        writer.writeByte(this.byte2); //diff
        writer.writeByte(this.byte3); //diff
        writer.writeInt(this.int7); //diff
        writer.writeByte(this.byte4); //diff
        writer.writeByte(this.byte5); //diff
        writer.writeByte(this.byte6); //diff
        writer.writeInt(this.int8); //diff
        writer.writeInt(this.int9); //diff
    }
}
