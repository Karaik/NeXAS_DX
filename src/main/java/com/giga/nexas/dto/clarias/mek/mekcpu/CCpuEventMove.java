package com.giga.nexas.dto.clarias.mek.mekcpu;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;

@Data
public class CCpuEventMove extends CCpuEvent {

    @Data
    @AllArgsConstructor
    public static class CCpuEventMoveType {
        private Integer type;
        private String label;
    }

    public static final CCpuEventMoveType[] CCPU_EVENT_MOVE_TYPES = {
            new CCpuEventMoveType(0xFFFFFFFF, "slot01"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot02"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot03"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot04"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot05"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot06"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot07"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot08"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot09"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot10"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot11"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot12"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot13"),
            new CCpuEventMoveType(0xFFFFFFFF, "slot14")
    };

    private Integer moveField1;
    private Integer moveField2;
    private Integer moveField3;
    private Integer moveField4;
    private Integer moveField5;
    private Integer moveField6;
    private Integer moveField7;
    private Integer moveField8;
    private Integer moveField9;
    private Integer moveField10;
    private Integer moveField11;
    private Integer moveField12;
    private Integer moveField13;
    private Integer moveField14;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.moveField1 = reader.readInt();
        this.moveField2 = reader.readInt();
        this.moveField3 = reader.readInt();
        this.moveField4 = reader.readInt();
        this.moveField5 = reader.readInt();
        this.moveField6 = reader.readInt();
        this.moveField7 = reader.readInt();
        this.moveField8 = reader.readInt();
        this.moveField9 = reader.readInt();
        this.moveField10 = reader.readInt();
        this.moveField11 = reader.readInt();
        this.moveField12 = reader.readInt();
        this.moveField13 = reader.readInt();
        this.moveField14 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.moveField1);
        writer.writeInt(this.moveField2);
        writer.writeInt(this.moveField3);
        writer.writeInt(this.moveField4);
        writer.writeInt(this.moveField5);
        writer.writeInt(this.moveField6);
        writer.writeInt(this.moveField7);
        writer.writeInt(this.moveField8);
        writer.writeInt(this.moveField9);
        writer.writeInt(this.moveField10);
        writer.writeInt(this.moveField11);
        writer.writeInt(this.moveField12);
        writer.writeInt(this.moveField13);
        writer.writeInt(this.moveField14);
    }
}
