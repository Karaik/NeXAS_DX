package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SkillInfoTypeEntry {

    private Integer type;
    private Integer address;
    private String description;

    public Integer getFlags() {
        return 0;
    }

}
