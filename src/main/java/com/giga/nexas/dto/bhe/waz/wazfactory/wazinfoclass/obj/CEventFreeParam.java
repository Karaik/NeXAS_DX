package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description CEventFreeParam
 */
@Data
@NoArgsConstructor
public class CEventFreeParam extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventFreeParamType {
        private Integer type;
        private String description;
    }

    public static final CEventFreeParamType[] CEVENT_FREE_PARAM_ENTRIES = {
            new CEventFreeParamType(0x0,  "汎用変数1"),
            new CEventFreeParamType(0x0,  "汎用変数2"),
            new CEventFreeParamType(0x0,  "汎用変数3"),
            new CEventFreeParamType(0x0,  "汎用変数4")
    };

    @Data
    public static class CEventFreeParamUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventFreeParamUnit> unitList = new ArrayList<>();

    public CEventFreeParam(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.unitList.clear();

        for (int i = 0; i < 4; i++) {
                int buffer = reader.readInt();

            CEventFreeParamUnit unit = new CEventFreeParamUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_FREE_PARAM_ENTRIES[i].getDescription());

            if (buffer != 0) {
                int type = CEVENT_FREE_PARAM_ENTRIES[i].getType();
                SkillInfoObject obj = SkillInfoFactory.createCEventObjectByTypeBhe(type);
                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
            }

            this.unitList.add(unit);
        }
    }
}
