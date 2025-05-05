package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass;

import com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoUnknown;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * @Description WazUnit
 */
@Data
public class SkillUnit {

    public Integer unitQuantity; // 记录用
    public String unitDescription; // 记录用

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
