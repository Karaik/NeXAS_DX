package com.giga.nexas.dto.clarias.grp;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.giga.nexas.dto.clarias.Clarias;
import lombok.Data;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/2/2
 * @Description GroupMap
 */
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "fileName",
        visible = true
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.BatVoiceGrp.class, name = "batvoice"),
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.MapGroupGrp.class, name = "mapgroup"),
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.MekaGroupGrp.class, name = "mekagroup"),
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.ProgramMaterialGrp.class, name = "programmaterial"),
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.SeGroupGrp.class, name = "segroup"),
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.SpriteGroupGrp.class, name = "spritegroup"),
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.TermGrp.class, name = "term"),
        @JsonSubTypes.Type(value = com.giga.nexas.dto.clarias.grp.groupmap.WazaGroupGrp.class, name = "wazagroup")
})
@Data
public class Grp extends Clarias {

    // 鏂囦欢鍚
    private String fileName;

}

