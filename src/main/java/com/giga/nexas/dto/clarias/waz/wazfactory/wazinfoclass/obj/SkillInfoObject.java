package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "typeId", visible = true, defaultImpl = SkillInfoGeneric.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = SkillInfoGeneric.class, name = "0"),
        @JsonSubTypes.Type(value = CEventVal.class, name = "1"),
        @JsonSubTypes.Type(value = CEventVal.class, name = "3"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "4"),
        @JsonSubTypes.Type(value = CEventValRandom.class, name = "5"),
        @JsonSubTypes.Type(value = CEventSprite.class, name = "20"),
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
