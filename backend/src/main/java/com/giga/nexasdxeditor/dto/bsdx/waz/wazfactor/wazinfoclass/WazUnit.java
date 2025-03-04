package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.WazInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.WazInfoUnknown;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * @Description WazUnit
 */
@Data
public class WazUnit {

    public Integer unitQuantity; // 记录用
    public String unitDescription; // 记录用

    private List<WazInfoObject> wazInfoObjectList;
    private List<WazInfoUnknown> wazInfoUnknownList;

    public WazUnit() {
        this.wazInfoObjectList = new ArrayList<>();
        this.wazInfoUnknownList = new ArrayList<>();
    }

    public WazUnit(Integer unitQuantity, String unitDescription) {
        this.unitQuantity = unitQuantity;
        this.unitDescription = unitDescription;
        this.wazInfoObjectList = new ArrayList<>();
        this.wazInfoUnknownList = new ArrayList<>();
    }

}
