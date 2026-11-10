package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeClarias;

@Data
@NoArgsConstructor
public class CEventFreeParam extends SkillInfoObject {

    public static final Integer FREE_PARAM_VALUE_TYPE_ID = 0x1;

    @Data
    public static class CEventFreeParamUnit {
        private Integer key;
        private SkillInfoObject data;
    }

    private Integer count;
    private List<CEventFreeParamUnit> unitList = new ArrayList<>();

    public CEventFreeParam(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.count = reader.readInt();
        this.unitList.clear();

        if (this.count > 0) {
            for (int i = 0; i < this.count; i++) {
                CEventFreeParamUnit unit = new CEventFreeParamUnit();
                unit.setKey(reader.readInt());

                SkillInfoObject obj = createCEventObjectByTypeClarias(FREE_PARAM_VALUE_TYPE_ID);
                obj.readInfo(reader);
                unit.setData(obj);

                this.unitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.count);
        for (CEventFreeParamUnit unit : this.unitList) {
            writer.writeInt(unit.getKey());
            unit.getData().writeInfo(writer);
        }
    }
}
