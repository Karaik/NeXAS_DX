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

    private List<WazInfoObject> wazInfoObjectList;
    private List<WazInfoUnknown> wazInfoUnknownList;

    public WazUnit() {
        wazInfoObjectList = new ArrayList<>();
        wazInfoUnknownList = new ArrayList<>();
    }

}
