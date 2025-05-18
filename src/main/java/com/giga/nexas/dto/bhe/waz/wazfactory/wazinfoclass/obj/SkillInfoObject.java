package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description WazInfoObject
 */

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "typeId", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CEventVal.class, name = "0"),
        @JsonSubTypes.Type(value = CEventVal.class, name = "1"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "2"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "3"),
        @JsonSubTypes.Type(value = CEventWazaSelect.class, name = "4"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "5"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "6"),
        @JsonSubTypes.Type(value = CEventMove.class, name = "7"),
        @JsonSubTypes.Type(value = CEventHeight.class, name = "8"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "9"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "10"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "11"),
        @JsonSubTypes.Type(value = CEventSlipHosei.class, name = "12"),
        @JsonSubTypes.Type(value = CEventSprite.class, name = "13"),
        @JsonSubTypes.Type(value = CEventSpriteYure.class, name = "14"),
        @JsonSubTypes.Type(value = CEventSpriteAttr.class, name = "15"),
        @JsonSubTypes.Type(value = CEventHit.class, name = "16"),
        @JsonSubTypes.Type(value = CEventEscape.class, name = "17"),
        @JsonSubTypes.Type(value = CEventCpuButton.class, name = "18"),
        @JsonSubTypes.Type(value = CEventEffect.class, name = "19"),
        @JsonSubTypes.Type(value = CEventBlink.class, name = "20"),
        @JsonSubTypes.Type(value = CEventCharge.class, name = "21"),
        @JsonSubTypes.Type(value = CEventTouch.class, name = "22"),
        @JsonSubTypes.Type(value = CEventSe.class, name = "23"),
        @JsonSubTypes.Type(value = CEventVoice.class, name = "24"),
        @JsonSubTypes.Type(value = CEventScreenYure.class, name = "25"),
        @JsonSubTypes.Type(value = CEventScreenAttr.class, name = "26"),
        @JsonSubTypes.Type(value = CEventScreenEffect.class, name = "27"),
        @JsonSubTypes.Type(value = CEventScreenLine.class, name = "28"),
        @JsonSubTypes.Type(value = CEventRadialLine.class, name = "29"),
        @JsonSubTypes.Type(value = CEventBlur.class, name = "30"),
        @JsonSubTypes.Type(value = CEventCamera.class, name = "31"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "32"),
        @JsonSubTypes.Type(value = CEventNokezori.class, name = "33"),
        @JsonSubTypes.Type(value = CEventChange.class, name = "34"),
        @JsonSubTypes.Type(value = CEventStatus.class, name = "35"),
        @JsonSubTypes.Type(value = SkillInfoUnknown.class, name = "255") // 0xFF
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
