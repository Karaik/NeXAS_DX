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
public class CEventDefense extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventDefenseType {
        private Integer type;
        private String description;
    }

    public static final CEventDefenseType[] CEVENT_DEFENSE_ENTRIES = {
            new CEventDefenseType(0xFFFFFFFF, "攻撃レベル"),
            new CEventDefenseType(0xFFFFFFFF, "くらい状態"),
            new CEventDefenseType(0xFFFFFFFF, "ダメージ補正率(100=100%)"),
            new CEventDefenseType(0x7, "ヒットストップ演出(通常)"),
            new CEventDefenseType(0x7, "ヒットストップ演出(耐久０)"),
            new CEventDefenseType(0xFFFFFFFF, "フラグ")
    };

    @Data
    public static class CEventDefenseUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Byte byte1;
    private Byte byte2;
    private Integer int1;
    private Byte byte3;
    private List<CEventDefenseUnit> unitList = new ArrayList<>();

    public CEventDefense(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.byte1 = reader.readByte();
        this.byte2 = reader.readByte();
        this.int1 = reader.readInt();
        this.byte3 = reader.readByte();

        this.unitList.clear();
        for (int i = 0; i < 6; i++) {
            int buffer = reader.readInt();

            CEventDefenseUnit unit = new CEventDefenseUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_DEFENSE_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_DEFENSE_ENTRIES[i].getType();
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
        writer.writeByte(this.byte1);
        writer.writeByte(this.byte2);
        writer.writeInt(this.int1);
        writer.writeByte(this.byte3);

        for (int i = 0; i < 6; i++) {
            CEventDefenseUnit target = null;
            for (CEventDefenseUnit unit : this.unitList) {
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
