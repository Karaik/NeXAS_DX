package com.giga.nexas.dto.clarias.mek.mekcpu;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.dto.clarias.ClariasInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CCpuEventMove.class, name = "1"),
        @JsonSubTypes.Type(value = CCpuEventAttack.class, name = "2"),
        @JsonSubTypes.Type(value = CCpuEventChange.class, name = "8")
})
@Data
public class CCpuEvent {

    @Data
    @AllArgsConstructor
    public static class CCpuEventType {
        private Integer type;
        private String label;
    }

    public static final CCpuEventType[] CCPU_EVENT_TYPES = {
            new CCpuEventType(0xFFFFFFFF, "slot01"),
            new CCpuEventType(0xFFFFFFFF, "slot02"),
            new CCpuEventType(0xFFFFFFFF, "slot03"),
            new CCpuEventType(0xFFFFFFFF, "slot04"),
            new CCpuEventType(0xFFFFFFFF, "slot05"),
            new CCpuEventType(0xFFFFFFFF, "slot06"),
            new CCpuEventType(0xFFFFFFFF, "slot07"),
            new CCpuEventType(0xFFFFFFFF, "slot08"),
            new CCpuEventType(0xFFFFFFFF, "slot09"),
            new CCpuEventType(0xFFFFFFFF, "slot10"),
            new CCpuEventType(0xFFFFFFFF, "slot11"),
            new CCpuEventType(0xFFFFFFFF, "slot12"),
            new CCpuEventType(0xFFFFFFFF, "slot13"),
            new CCpuEventType(0xFFFFFFFF, "slot14"),
            new CCpuEventType(0xFFFFFFFF, "slot15"),
            new CCpuEventType(0xFFFFFFFF, "slot16"),
            new CCpuEventType(0xFFFFFFFF, "slot17"),
            new CCpuEventType(0xFFFFFFFF, "slot18"),
            new CCpuEventType(0xFFFFFFFF, "slot19"),
            new CCpuEventType(0xFFFFFFFF, "slot20"),
            new CCpuEventType(0xFFFFFFFF, "slot21"),
            new CCpuEventType(0xFFFFFFFF, "slot22"),
            new CCpuEventType(0xFFFFFFFF, "slot23"),
            new CCpuEventType(0xFFFFFFFF, "slot24"),
            new CCpuEventType(0xFFFFFFFF, "slot25"),
            new CCpuEventType(0xFFFFFFFF, "slot26")
    };

    private Short type;
    private Short shortField1;
    private Integer intField1;
    private Integer intField2;
    private Integer intField3;
    private Integer intField4;
    private Integer intField5;
    private Integer intField6;
    private Integer intField7;
    private Integer intField8;
    private Integer intField9;
    private Integer intField10;
    private Integer intField11;
    private Integer intField12;
    private Integer intField13;
    private Integer intField14;
    private Integer intField15;
    private Integer intField16;
    private Integer intField17;
    private Integer intField18;
    private Integer intField19;
    private Integer intField20;
    private Integer intField21;
    private Integer intField22;
    private Integer intField23;
    private Integer intField24;
    private Integer intField25;
    private Integer intField26;
    private Integer intField27;
    private Integer intField28;
    private Integer intField29;
    private Short shortField2;
    private Integer intField30;
    private Integer intField31;
    private Short shortField3;
    private Integer intField32;
    private Integer intField33;
    private Short shortField4;
    private Short shortField5;
    private Byte byteField1;
    private List<ClariasInfoCollection> clariasInfoCollectionList = new ArrayList<>();

    public void readInfo(BinaryReader reader) {
        this.clariasInfoCollectionList.clear();
        ClariasInfoCollection collection = new ClariasInfoCollection();
        collection.readCollection(reader);
        this.clariasInfoCollectionList.add(collection);

        this.shortField1 = reader.readShort();
        this.intField1 = reader.readInt();
        this.intField2 = reader.readInt();
        this.intField3 = reader.readInt();
        this.intField4 = reader.readInt();
        this.intField5 = reader.readInt();
        this.intField6 = reader.readInt();
        this.intField7 = reader.readInt();
        this.intField8 = reader.readInt();
        this.intField9 = reader.readInt();
        this.intField10 = reader.readInt();
        this.intField11 = reader.readInt();
        this.intField12 = reader.readInt();
        this.intField13 = reader.readInt();
        this.intField14 = reader.readInt();
        this.intField15 = reader.readInt();
        this.intField16 = reader.readInt();
        this.intField17 = reader.readInt();
        this.intField18 = reader.readInt();
        this.intField19 = reader.readInt();
        this.intField20 = reader.readInt();
        this.intField21 = reader.readInt();
        this.intField22 = reader.readInt();
        this.intField23 = reader.readInt();
        this.intField24 = reader.readInt();
        this.intField25 = reader.readInt();
        this.intField26 = reader.readInt();
        this.intField27 = reader.readInt();
        this.intField28 = reader.readInt();
        this.intField29 = reader.readInt();
        this.shortField2 = reader.readShort();
        this.intField30 = reader.readInt();
        this.intField31 = reader.readInt();
        this.shortField3 = reader.readShort();
        this.intField32 = reader.readInt();
        this.intField33 = reader.readInt();
        this.shortField4 = reader.readShort();
        this.shortField5 = reader.readShort();
        this.byteField1 = reader.readByte();
    }

    public void writeInfo(BinaryWriter writer) throws IOException {
        for (ClariasInfoCollection collection : clariasInfoCollectionList) {
            collection.writeCollection(writer);
        }

        writer.writeShort(this.shortField1);
        writer.writeInt(this.intField1);
        writer.writeInt(this.intField2);
        writer.writeInt(this.intField3);
        writer.writeInt(this.intField4);
        writer.writeInt(this.intField5);
        writer.writeInt(this.intField6);
        writer.writeInt(this.intField7);
        writer.writeInt(this.intField8);
        writer.writeInt(this.intField9);
        writer.writeInt(this.intField10);
        writer.writeInt(this.intField11);
        writer.writeInt(this.intField12);
        writer.writeInt(this.intField13);
        writer.writeInt(this.intField14);
        writer.writeInt(this.intField15);
        writer.writeInt(this.intField16);
        writer.writeInt(this.intField17);
        writer.writeInt(this.intField18);
        writer.writeInt(this.intField19);
        writer.writeInt(this.intField20);
        writer.writeInt(this.intField21);
        writer.writeInt(this.intField22);
        writer.writeInt(this.intField23);
        writer.writeInt(this.intField24);
        writer.writeInt(this.intField25);
        writer.writeInt(this.intField26);
        writer.writeInt(this.intField27);
        writer.writeInt(this.intField28);
        writer.writeInt(this.intField29);
        writer.writeShort(this.shortField2);
        writer.writeInt(this.intField30);
        writer.writeInt(this.intField31);
        writer.writeShort(this.shortField3);
        writer.writeInt(this.intField32);
        writer.writeInt(this.intField33);
        writer.writeShort(this.shortField4);
        writer.writeShort(this.shortField5);
        writer.writeByte(this.byteField1);
    }
}
