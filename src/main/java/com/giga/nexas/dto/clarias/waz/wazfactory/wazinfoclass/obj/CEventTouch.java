package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventTouch extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventTouchType {
        private Integer type;
        private String description;
    }

    public static final CEventTouchType[] CEVENT_TOUCH_TYPES = {
            new CEventTouchType(0xFFFFFFFF, "水平：メカ（数）"),
            new CEventTouchType(0xFFFFFFFF, "水平：メカ（向方）"),
            new CEventTouchType(0xFFFFFFFF, "水平：弾（数）"),
            new CEventTouchType(0xFFFFFFFF, "水平：弾（向方）"),
            new CEventTouchType(0xFFFFFFFF, "水平：トップOBJ"),
            new CEventTouchType(0xFFFFFFFF, "遮蔽（メカ）"),
            new CEventTouchType(0xFFFFFFFF, "遮蔽（弾）"),
            new CEventTouchType(0xFFFFFFFF, "遮蔽（壁）"),
            new CEventTouchType(0xFFFFFFFF, "画面端"),
            new CEventTouchType(0xFFFFFFFF, "垂直：メカ（数）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：メカ（向方）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：弾（数）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：弾（向方）"),
            new CEventTouchType(0xFFFFFFFF, "垂直：障害物")
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
    private Integer int12;
    private Integer int13;
    private Integer int14;
    private Integer int15; //diff
    private Integer int16; //diff
    private Integer int17; //diff
    private Integer int18; //diff
    private Short short1; //diff

    public CEventTouch(Integer typeId) {
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
        this.int10 = reader.readInt();
        this.int11 = reader.readInt();
        this.int12 = reader.readInt();
        this.int13 = reader.readInt();
        this.int14 = reader.readInt();
        this.int15 = reader.readInt(); //diff
        this.int16 = reader.readInt(); //diff
        this.int17 = reader.readInt(); //diff
        this.int18 = reader.readInt(); //diff
        this.short1 = reader.readShort(); //diff
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
        writer.writeInt(this.int12);
        writer.writeInt(this.int13);
        writer.writeInt(this.int14);
        writer.writeInt(this.int15); //diff
        writer.writeInt(this.int16); //diff
        writer.writeInt(this.int17); //diff
        writer.writeInt(this.int18); //diff
        writer.writeShort(this.short1); //diff
    }
}
