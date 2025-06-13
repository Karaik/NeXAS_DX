package com.giga.nexas.dto.bhe.mek.mekcpu;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.dto.bhe.BheInfoCollection;
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
            new CCpuEventType(0xFFFFFFFF, "特殊行動"), // diff
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

    /**
     * 発動確率
     */
    private Integer activationProbability;

    /**
     * 発動確率（反撃時）
     */
    private Integer activationProbabilityWhenCounter;
    private Integer int5;

    /**
     * 発動範囲Min
     */
    private Integer activationRangeMin;

    /**
     * 発動範囲Max
     */
    private Integer activationRangeMax;

    /**
     * 角度範囲Min
     */
    private Integer activationAngleRangeMin;

    /**
     * 角度範囲Max
     */
    private Integer activationAngleRangeMax;

    /**
     * 発動高度Min
     */
    private Integer activationAltitudeMin;

    /**
     * 発動高度Max
     */
    private Integer activationAltitudeMax;

    /**
     * 発動耐久力Min(%)
     */
    private Integer activationDurabilityMinPercentage;

    /**
     * 発動耐久力Max(%)
     */
    private Integer activationDurabilityMaxPercentage;
    private Integer int14;
    private Integer int15;

    /**
     * 発動熱量Min
     */
    private Integer activationHeatMin;

    /**
     * 発動熱量Max
     */
    private Integer activationHeatMax;
    private Integer int18;
    private Integer int19;
    private Integer int20;
    private Integer int21;
    private Integer int22;
    private Integer int23;
    private Integer int24;
    private Integer int25;
//    private Integer int26; // diff

    private Short short2;

    private Integer int27;
    private Integer int28;

    private Short short3;
    private Integer int29; // diff
    private Integer int30; // diff
    private Short short4;
    private Short short5; // diff
    private Byte byte1; // diff
    private List<BheInfoCollection> bheInfoCollectionList = new ArrayList<>();

    public void readInfo(BinaryReader reader) {
        this.bheInfoCollectionList.clear();
        BheInfoCollection bheInfoCollection = new BheInfoCollection();
        bheInfoCollection.readCollection(reader);
        this.bheInfoCollectionList.add(bheInfoCollection);

        this.short1 = reader.readShort();
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.activationProbability = reader.readInt();
        this.activationProbabilityWhenCounter = reader.readInt();
        this.int5 = reader.readInt();
        this.activationRangeMin = reader.readInt();
        this.activationRangeMax = reader.readInt();
        this.activationAngleRangeMin = reader.readInt();
        this.activationAngleRangeMax = reader.readInt();
        this.activationAltitudeMin = reader.readInt();
        this.activationAltitudeMax = reader.readInt();
        this.activationDurabilityMinPercentage = reader.readInt();
        this.activationDurabilityMaxPercentage = reader.readInt();
        this.int14 = reader.readInt();
        this.int15 = reader.readInt();
        this.activationHeatMin = reader.readInt();
        this.activationHeatMax = reader.readInt();
        this.int18 = reader.readInt();
        this.int19 = reader.readInt();
        this.int20 = reader.readInt();
        this.int21 = reader.readInt();
        this.int22 = reader.readInt();
        this.int23 = reader.readInt();
        this.int24 = reader.readInt();
        this.int25 = reader.readInt();
//        this.int26 = reader.readInt(); // diff
        this.short2 = reader.readShort();
        this.int27 = reader.readInt();
        this.int28 = reader.readInt();
        this.short3 = reader.readShort();
        this.int29 = reader.readInt();
        this.int30 = reader.readInt();
        this.short4 = reader.readShort();
        this.short5 = reader.readShort();
        this.byte1 = reader.readByte();
    }

    public void writeInfo(BinaryWriter writer) throws IOException {
        for (BheInfoCollection collection : bheInfoCollectionList) {
            collection.writeCollection(writer);
        }

        writer.writeShort(this.short1);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.activationProbability);
        writer.writeInt(this.activationProbabilityWhenCounter);
        writer.writeInt(this.int5);
        writer.writeInt(this.activationRangeMin);
        writer.writeInt(this.activationRangeMax);
        writer.writeInt(this.activationAngleRangeMin);
        writer.writeInt(this.activationAngleRangeMax);
        writer.writeInt(this.activationAltitudeMin);
        writer.writeInt(this.activationAltitudeMax);
        writer.writeInt(this.activationDurabilityMinPercentage);
        writer.writeInt(this.activationDurabilityMaxPercentage);
        writer.writeInt(this.int14);
        writer.writeInt(this.int15);
        writer.writeInt(this.activationHeatMin);
        writer.writeInt(this.activationHeatMax);
        writer.writeInt(this.int18);
        writer.writeInt(this.int19);
        writer.writeInt(this.int20);
        writer.writeInt(this.int21);
        writer.writeInt(this.int22);
        writer.writeInt(this.int23);
        writer.writeInt(this.int24);
        writer.writeInt(this.int25);
//        writer.writeInt(this.int26); // diff
        writer.writeShort(this.short2);
        writer.writeInt(this.int27);
        writer.writeInt(this.int28);
        writer.writeShort(this.short3);
        writer.writeInt(this.int29); // diff
        writer.writeInt(this.int30); // diff
        writer.writeShort(this.short4);
        writer.writeShort(this.short5); // diff
        writer.writeByte(this.byte1); // diff
    }


}
