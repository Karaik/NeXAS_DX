package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass;

import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SkillUnit {
    public Integer unitQuantity;
    public String unitDescription;

    private List<SkillInfoObject> skillInfoObjectList = new ArrayList<>();
    private List<SkillInfoUnknown> skillInfoUnknownList = new ArrayList<>();
    private byte[] secondaryTailBytes = new byte[0];

    public SkillUnit() {
    }

    public SkillUnit(Integer unitQuantity, String unitDescription) {
        this.unitQuantity = unitQuantity;
        this.unitDescription = unitDescription;
    }
}
