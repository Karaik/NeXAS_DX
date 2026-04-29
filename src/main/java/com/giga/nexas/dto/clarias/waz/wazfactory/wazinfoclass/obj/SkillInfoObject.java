package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "typeId", visible = true)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CEventVoid.class, name = "0"),
        @JsonSubTypes.Type(value = CEventVal.class, name = "1"),
        @JsonSubTypes.Type(value = CEventVal.class, name = "2"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "3"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "4"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "5"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "6"),
        @JsonSubTypes.Type(value = CEventWazaSelect.class, name = "7"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "8"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "9"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "10"),
        @JsonSubTypes.Type(value = CEventPosChange.class, name = "11"),
        @JsonSubTypes.Type(value = CEventMove.class, name = "12"),
        @JsonSubTypes.Type(value = CEventHeight.class, name = "13"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "14"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "15"),
        @JsonSubTypes.Type(value = CEventAngleEffect.class, name = "16"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "17"),
        @JsonSubTypes.Type(value = CEventSlipHosei.class, name = "18"),
        @JsonSubTypes.Type(value = CEventFreeParam.class, name = "19"),
        @JsonSubTypes.Type(value = CEventSprite.class, name = "20"),
        @JsonSubTypes.Type(value = CEventSpriteYure.class, name = "21"),
        @JsonSubTypes.Type(value = CEventSpriteAttr.class, name = "22"),
        @JsonSubTypes.Type(value = CEventMultiLockStartEnd.class, name = "23"),
        @JsonSubTypes.Type(value = CEventMultiLock.class, name = "24"),
        @JsonSubTypes.Type(value = CEventMultiLockDraw.class, name = "25"),
        @JsonSubTypes.Type(value = CEventHit.class, name = "26"),
        @JsonSubTypes.Type(value = CEventEscape.class, name = "27"),
        @JsonSubTypes.Type(value = CEventCpuButton.class, name = "28"),
        @JsonSubTypes.Type(value = CEventEffect.class, name = "29"),
        @JsonSubTypes.Type(value = CEventMeka.class, name = "30"),
        @JsonSubTypes.Type(value = CEventBlink.class, name = "31"),
        @JsonSubTypes.Type(value = CEventCharge.class, name = "32"),
        @JsonSubTypes.Type(value = CEventTouch.class, name = "33"),
        @JsonSubTypes.Type(value = CEventSe.class, name = "34"),
        @JsonSubTypes.Type(value = CEventVoice.class, name = "35"),
        @JsonSubTypes.Type(value = CEventScreenYure.class, name = "36"),
        @JsonSubTypes.Type(value = CEventScreenAttr.class, name = "37"),
        @JsonSubTypes.Type(value = CEventScreenEffect.class, name = "38"),
        @JsonSubTypes.Type(value = CEventScreenLine.class, name = "39"),
        @JsonSubTypes.Type(value = CEventRadialLine.class, name = "40"),
        @JsonSubTypes.Type(value = CEventBlur.class, name = "41"),
        @JsonSubTypes.Type(value = CEventCamera.class, name = "42"),
        @JsonSubTypes.Type(value = CEventTerm.class, name = "43"),
        @JsonSubTypes.Type(value = CEventNokezori.class, name = "44"),
        @JsonSubTypes.Type(value = CEventChange.class, name = "45"),
        @JsonSubTypes.Type(value = CEventStatus.class, name = "46"),
        @JsonSubTypes.Type(value = CEventParamRevise.class, name = "47"),
        @JsonSubTypes.Type(value = CEventWazaToolParam.class, name = "48"),
        @JsonSubTypes.Type(value = CEventAttr.class, name = "49"),
        @JsonSubTypes.Type(value = CEventStealth.class, name = "50"),
        @JsonSubTypes.Type(value = CEventGamePadShake.class, name = "51"),
        @JsonSubTypes.Type(value = CEventKeyInputControl.class, name = "52"),
        @JsonSubTypes.Type(value = CEventHitSousai.class, name = "53"),
        @JsonSubTypes.Type(value = CEventSystemSlow.class, name = "54"),
        @JsonSubTypes.Type(value = CEventHitStopEffect.class, name = "55"),
        @JsonSubTypes.Type(value = CEventDefense.class, name = "56"),
        @JsonSubTypes.Type(value = CEventPartnerAttackCancel.class, name = "57"),
        @JsonSubTypes.Type(value = CEventWeakPoint.class, name = "58"),
        @JsonSubTypes.Type(value = CEventPlayerChange.class, name = "59"),
        @JsonSubTypes.Type(value = CEventMainParam.class, name = "60"),
        @JsonSubTypes.Type(value = SkillInfoUnknown.class, name = "255")
})
@Data
@NoArgsConstructor
public class SkillInfoObject {

    public Integer offset;
    public Integer slotNum;
    public Integer typeId;
    private Integer startFrame;
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
