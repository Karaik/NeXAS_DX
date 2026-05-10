package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeClarias;

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
            new CEventWazaToolParamType(0xFFFFFFFF, "設定対象OBJ"),
            new CEventWazaToolParamType(0xFFFFFFFF, "ロックタイプ"),
            new CEventWazaToolParamType(0xFFFFFFFF, "C4タイプ"),
            new CEventWazaToolParamType(0xFFFFFFFF, "エリスの端末通知"),
            new CEventWazaToolParamType(0xFFFFFFFF, "アイテム破棄通知"),
            new CEventWazaToolParamType(0xFFFFFFFF, "ジャスト回避通知"),
            new CEventWazaToolParamType(0xFFFFFFFF, "予備2"),
            new CEventWazaToolParamType(0xFFFFFFFF, "予備3"),
            new CEventWazaToolParamType(0xFFFFFFFF, "予備4")
    };

    @Data
    public static class CEventWazaToolParamUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Byte byte1;
    private List<CEventWazaToolParamUnit> unitList = new ArrayList<>();

    public CEventWazaToolParam(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        // Field table order is type=6 block first, then the byte field at 0x38.
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.int7 = reader.readInt();
        this.int8 = reader.readInt();
        // exe 内会设置为固定值 -999，不复刻，此处保留信息
        // this.int6 = -999;
        // this.int7 = -999;
        // this.int8 = -999;
        this.byte1 = reader.readByte();

        this.unitList.clear();
        for (int i = 0; i < 9; i++) {
            int buffer = reader.readInt();

            CEventWazaToolParamUnit unit = new CEventWazaToolParamUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_WAZA_TOOL_PARAM_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_WAZA_TOOL_PARAM_ENTRIES[i].getType();
            if (buffer != 0) {
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                obj.readInfo(reader);
                unit.setData(obj);
            }

            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
        writer.writeInt(this.int7);
        writer.writeInt(this.int8);
        writer.writeByte(this.byte1);

        for (int i = 0; i < 9; i++) {
            CEventWazaToolParamUnit target = null;
            for (CEventWazaToolParamUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
            }

            writer.writeInt(target.getBuffer());
            if (target.getBuffer() != 0) {
                target.getData().writeInfo(writer);
            }
        }
    }
}
