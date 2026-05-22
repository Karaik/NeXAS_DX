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

    private List<SkillInfoObject> skillInfoObjectList;
    private List<SkillInfoUnknown> skillInfoUnknownList;

    public SkillUnit() {
        this.skillInfoObjectList = new ArrayList<>();
        this.skillInfoUnknownList = new ArrayList<>();
    }

    public SkillUnit(Integer unitQuantity, String unitDescription) {
        this.unitQuantity = unitQuantity;
        this.unitDescription = unitDescription;
        this.skillInfoObjectList = new ArrayList<>();
        this.skillInfoUnknownList = new ArrayList<>();
    }
}
