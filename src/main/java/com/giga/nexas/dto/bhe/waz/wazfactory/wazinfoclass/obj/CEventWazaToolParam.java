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
 * @Description CEventWazaToolParam
 */
@Data
@NoArgsConstructor
public class CEventWazaToolParam extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventWazaToolParamType {
        private Integer type;
        private String description;
    }

    public static final CEventWazaToolParamType[] CEVENT_WAZA_TOOL_PARAM_ENTRIES = {
            new CEventWazaToolParamType(0xFFFFFFFF, "ロックタイプ"),
            new CEventWazaToolParamType(0xFFFFFFFF, "C4タイプ")
    };

    private Integer int1;
    private Integer int2;

    @Data
    public static class CEventWazaToolParamUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventWazaToolParamUnit> unitList = new ArrayList<>();

    public CEventWazaToolParam(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();

        this.unitList.clear();
        for (int i = 0; i < 2; i++) {
                int buffer = reader.readInt();

            CEventWazaToolParamUnit unit = new CEventWazaToolParamUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_WAZA_TOOL_PARAM_ENTRIES[i].getDescription());

            if (buffer != 0) {
                int typeId = CEVENT_WAZA_TOOL_PARAM_ENTRIES[i].getType();
                SkillInfoObject obj = SkillInfoFactory.createCEventObjectByTypeBhe(typeId);
                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
            }

            this.unitList.add(unit);
        }
    }
}
