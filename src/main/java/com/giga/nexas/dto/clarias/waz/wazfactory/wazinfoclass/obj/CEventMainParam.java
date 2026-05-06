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
public class CEventMainParam extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventMainParamType {
        private Integer type;
        private String description;
    }

    public static final CEventMainParamType[] CEVENT_MAIN_PARAM_ENTRIES = {
            new CEventMainParamType(0xFFFFFFFF, "パラメータタイプ"),
            new CEventMainParamType(0x9, "対象OBJ"),
            new CEventMainParamType(0x9, "吸収OBJ"),
            new CEventMainParamType(0xFFFFFFFF, "回復(吸収)量"),
            new CEventMainParamType(0xFFFFFFFF, "フラグ")
    };

    public static final String[] CEVENT_MAIN_PARAM_EXTRA_ENTRIES = {
            "HP(魔痕×)",
            "魔痕がない場合のみ、HP回復可能",
            "HPゼロにはならない(必ず1は残る)"
    };

    public Integer fieldTableAddress = 0x00C37DB8;
    public Integer wrapperTableAddress = 0x00B64FC0;

    // Flat fields from field table (3 active entries), read before wrapper loop
    private Byte flatByte0;     // 1 byte
    private Byte flatByte1;     // 1 byte
    private Integer flatInt2;   // 4 bytes

    @Data
    public static class CEventMainParamUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventMainParamUnit> unitList = new ArrayList<>();

    public CEventMainParam(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        // Read flat fields from field table
        this.flatByte0 = reader.readByte();
        this.flatByte1 = reader.readByte();
        this.flatInt2 = reader.readInt();
        // Read wrapper entries
        this.unitList.clear();
        for (int i = 0; i < 5; i++) {
            int buffer = reader.readInt();
            CEventMainParamUnit unit = new CEventMainParamUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_MAIN_PARAM_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_MAIN_PARAM_ENTRIES[i].getType();
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
        // Write flat fields
        writer.writeByte(this.flatByte0);
        writer.writeByte(this.flatByte1);
        writer.writeInt(this.flatInt2);
        // Write wrapper entries
        for (int i = 0; i < 5; i++) {
            CEventMainParamUnit target = null;
            for (CEventMainParamUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) { target = unit; break; }
            }
            if (target != null) {
                writer.writeInt(target.getBuffer());
                if (target.getBuffer() != 0 && target.getData() != null) {
                    target.getData().writeInfo(writer);
                }
            } else {
                writer.writeInt(0);
            }
        }
    }
}
