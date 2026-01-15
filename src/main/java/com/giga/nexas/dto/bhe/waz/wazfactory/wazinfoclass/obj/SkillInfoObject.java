package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "typeId", visible = true)
@JsonSubTypes({
        // typeId 0x00-0x2B 映射（基于 SkillInfoFactory.createCEventObjectByTypeBhe）
        @JsonSubTypes.Type(value = CEventVal.class, name = "0"),           // 0x00
        @JsonSubTypes.Type(value = CEventVal.class, name = "1"),           // 0x01
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "2"),     // 0x02
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "3"),     // 0x03
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "4"),     // 0x04
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "5"),     // 0x05
        @JsonSubTypes.Type(value = CEventWazaSelect.class, name = "6"),    // 0x06
        @JsonSubTypes.Type(value = CEventTerm.class, name = "7"),          // 0x07
        @JsonSubTypes.Type(value = CEventTerm.class, name = "8"),          // 0x08
        @JsonSubTypes.Type(value = CEventMove.class, name = "9"),          // 0x09
        @JsonSubTypes.Type(value = CEventHeight.class, name = "10"),       // 0x0A
        @JsonSubTypes.Type(value = CEventTerm.class, name = "11"),         // 0x0B
        @JsonSubTypes.Type(value = CEventTerm.class, name = "12"),         // 0x0C
        @JsonSubTypes.Type(value = CEventTerm.class, name = "13"),         // 0x0D
        @JsonSubTypes.Type(value = CEventSlipHosei.class, name = "14"),    // 0x0E
        @JsonSubTypes.Type(value = CEventFreeParam.class, name = "15"),    // 0x0F
        @JsonSubTypes.Type(value = CEventSprite.class, name = "16"),       // 0x10
        @JsonSubTypes.Type(value = CEventSpriteYure.class, name = "17"),   // 0x11
        @JsonSubTypes.Type(value = CEventSpriteAttr.class, name = "18"),   // 0x12
        @JsonSubTypes.Type(value = CEventMultiLockStartEnd.class, name = "19"), // 0x13
        @JsonSubTypes.Type(value = CEventMultiLock.class, name = "20"),    // 0x14
        @JsonSubTypes.Type(value = CEventMultiLockDraw.class, name = "21"),// 0x15
        @JsonSubTypes.Type(value = CEventHit.class, name = "22"),          // 0x16
        @JsonSubTypes.Type(value = CEventEscape.class, name = "23"),       // 0x17
        @JsonSubTypes.Type(value = CEventCpuButton.class, name = "24"),    // 0x18
        @JsonSubTypes.Type(value = CEventEffect.class, name = "25"),       // 0x19
        @JsonSubTypes.Type(value = CEventBlink.class, name = "26"),        // 0x1A
        @JsonSubTypes.Type(value = CEventCharge.class, name = "27"),       // 0x1B
        @JsonSubTypes.Type(value = CEventTouch.class, name = "28"),        // 0x1C
        @JsonSubTypes.Type(value = CEventSe.class, name = "29"),           // 0x1D
        @JsonSubTypes.Type(value = CEventVoice.class, name = "30"),        // 0x1E
        @JsonSubTypes.Type(value = CEventScreenYure.class, name = "31"),   // 0x1F
        @JsonSubTypes.Type(value = CEventScreenAttr.class, name = "32"),   // 0x20
        @JsonSubTypes.Type(value = CEventScreenEffect.class, name = "33"), // 0x21
        @JsonSubTypes.Type(value = CEventScreenLine.class, name = "34"),   // 0x22
        @JsonSubTypes.Type(value = CEventRadialLine.class, name = "35"),   // 0x23
        @JsonSubTypes.Type(value = CEventBlur.class, name = "36"),         // 0x24
        @JsonSubTypes.Type(value = CEventCamera.class, name = "37"),       // 0x25
        @JsonSubTypes.Type(value = CEventTerm.class, name = "38"),         // 0x26
        @JsonSubTypes.Type(value = CEventNokezori.class, name = "39"),     // 0x27
        @JsonSubTypes.Type(value = CEventChange.class, name = "40"),       // 0x28
        @JsonSubTypes.Type(value = CEventStatus.class, name = "41"),       // 0x29
        @JsonSubTypes.Type(value = CEventWazaToolParam.class, name = "42"),// 0x2A
        @JsonSubTypes.Type(value = CEventAttr.class, name = "43"),         // 0x2B
        @JsonSubTypes.Type(value = SkillInfoUnknown.class, name = "255")   // 0xFF
})
@Data
@NoArgsConstructor
public class SkillInfoObject {

    public Integer offset;
    public Integer slotNum;
    public Integer typeId;
    /**
     * 第几帧开始
     */
    private Integer startFrame;

    /**
     * 第几帧结束
     */
    private Integer endFrame;

    public SkillInfoObject(Integer typeId) {
        this.typeId = typeId;
    }

    public void readInfo(BinaryReader reader) {

        this.offset = reader.getPosition();

        this.startFrame = reader.readInt();
        this.endFrame = reader.readInt();
    }

    public void writeInfo(BinaryWriter writer) throws IOException {
        writer.writeInt(this.startFrame);
        writer.writeInt(this.endFrame);
    }

}
