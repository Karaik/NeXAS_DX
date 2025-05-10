package com.giga.nexas.dto.bsdx.mek.mekcpu;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
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
        @JsonSubTypes.Type(value = CCpuEventAttack.class, name = "2")
})
/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description CCpuEvent
 */
@Data
public class CCpuEvent {

    @Data
    @AllArgsConstructor
    public static class CCpuEventType {
        private Integer type;
        private String description;
    }

    public static final CCpuEventType[] CCPU_EVENT_TYPES = {
            new CCpuEventType(0xFFFFFFFF, "データタイプ"), // 1
            new CCpuEventType(0xFFFFFFFF, "状態：自分"), // 2
            new CCpuEventType(0xFFFFFFFF, "状態：標的"), // 3
            new CCpuEventType(0xFFFFFFFF, "状態：進禁"), // 4
            new CCpuEventType(0xFFFFFFFF, "優先順位"), // 5
            new CCpuEventType(0xFFFFFFFF, "必要ＣＰＵレベル"), // 6
            new CCpuEventType(0xFFFFFFFF, "発動確率"), // 7
            new CCpuEventType(0xFFFFFFFF, "発動確率（反撃時）"), // 8
            new CCpuEventType(0xFFFFFFFF, "回避・反撃発動技タイプ"), // 9
            new CCpuEventType(0xFFFFFFFF, "標的外時の発動確率補正"), // 10
            new CCpuEventType(0xFFFFFFFF, "対象ＯＢＪ"), // 11
            new CCpuEventType(0xFFFFFFFF, "発動範囲Min"), // 12
            new CCpuEventType(0xFFFFFFFF, "発動範囲Max"), // 13
            new CCpuEventType(0xFFFFFFFF, "発動高度Min"), // 14
            new CCpuEventType(0xFFFFFFFF, "発動高度Max"), // 15
            new CCpuEventType(0xFFFFFFFF, "角度範囲Min"), // 16
            new CCpuEventType(0xFFFFFFFF, "角度範囲Max"), // 17
            new CCpuEventType(0xFFFFFFFF, "発動耐久力Min(%)"), // 18
            new CCpuEventType(0xFFFFFFFF, "発動耐久力Max(%)"), // 19
            new CCpuEventType(0xFFFFFFFF, "発動熱量Min"), // 20
            new CCpuEventType(0xFFFFFFFF, "発動熱量Max"), // 21
            new CCpuEventType(0xFFFFFFFF, "有効フレーム"), // 22
            new CCpuEventType(0xFFFFFFFF, "終了フラグ"), // 23
            new CCpuEventType(0xFFFFFFFF, "終了範囲Min"), // 24
            new CCpuEventType(0xFFFFFFFF, "終了範囲Max") // 25
    };

    // 仅记录用
    public Short type;

    private Short short1;

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
    private Integer int15;
    private Integer int16;
    private Integer int17;
    private Integer int18;
    private Integer int19;
    private Integer int20;
    private Integer int21;
    private Integer int22;
    private Integer int23;
    private Integer int24;
    private Integer int25;
    private Integer int26;

    private Short short2;

    private Integer int27;
    private Integer int28;

    private Short short3;
    private Short short4;
    private List<BsdxInfoCollection> bsdxInfoCollectionList = new ArrayList<>();

    public void readInfo(BinaryReader reader) {
        this.bsdxInfoCollectionList.clear();
        BsdxInfoCollection bsdxInfoCollection = new BsdxInfoCollection();
        bsdxInfoCollection.readCollection(reader);
        this.bsdxInfoCollectionList.add(bsdxInfoCollection);

        this.short1 = reader.readShort();
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
        this.int15 = reader.readInt();
        this.int16 = reader.readInt();
        this.int17 = reader.readInt();
        this.int18 = reader.readInt();
        this.int19 = reader.readInt();
        this.int20 = reader.readInt();
        this.int21 = reader.readInt();
        this.int22 = reader.readInt();
        this.int23 = reader.readInt();
        this.int24 = reader.readInt();
        this.int25 = reader.readInt();
        this.int26 = reader.readInt();
        this.short2 = reader.readShort();
        this.int27 = reader.readInt();
        this.int28 = reader.readInt();
        this.short3 = reader.readShort();
        this.short4 = reader.readShort();
    }

    public void writeInfo(BinaryWriter writer) throws IOException {
        for (BsdxInfoCollection collection : bsdxInfoCollectionList) {
            collection.writeCollection(writer);
        }

        writer.writeShort(this.short1);
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
        writer.writeInt(this.int15);
        writer.writeInt(this.int16);
        writer.writeInt(this.int17);
        writer.writeInt(this.int18);
        writer.writeInt(this.int19);
        writer.writeInt(this.int20);
        writer.writeInt(this.int21);
        writer.writeInt(this.int22);
        writer.writeInt(this.int23);
        writer.writeInt(this.int24);
        writer.writeInt(this.int25);
        writer.writeInt(this.int26);
        writer.writeShort(this.short2);
        writer.writeInt(this.int27);
        writer.writeInt(this.int28);
        writer.writeShort(this.short3);
        writer.writeShort(this.short4);
    }


}
