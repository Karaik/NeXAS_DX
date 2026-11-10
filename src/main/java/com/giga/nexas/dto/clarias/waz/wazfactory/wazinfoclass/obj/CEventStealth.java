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
public class CEventStealth extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventStealthType {
        private Integer type;
        private String description;
    }

    public static final CEventStealthType[] CEVENT_STEALTH_ENTRIES = {
            new CEventStealthType(0xFFFFFFFF, "対象タイプ"),
            new CEventStealthType(0xFFFFFFFF, "フラグ"),
            new CEventStealthType(0xFFFFFFFF, "効果範囲"),
            new CEventStealthType(0xFFFFFFFF, "角度補正値"),
            new CEventStealthType(0xFFFFFFFF, "距離最大値"),
            new CEventStealthType(0xFFFFFFFF, "距離最小時の補正値"),
            new CEventStealthType(0xFFFFFFFF, "距離最大時の補正値"),
            new CEventStealthType(0xFFFFFFFF, "攻撃時補正値"),
            new CEventStealthType(0xFFFFFFFF, "ジャマーとの距離最大値"),
            new CEventStealthType(0xFFFFFFFF, "ジャマーとの距離最小値の補正値"),
            new CEventStealthType(0xFFFFFFFF, "ジャマーとの距離最大値の補正値")
    };

    @Data
    public static class CEventStealthUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Integer int1;
    private Short short1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private List<CEventStealthUnit> unitList = new ArrayList<>();

    public CEventStealth(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.short1 = reader.readShort();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.int7 = reader.readInt();
        this.int8 = reader.readInt();
        this.int9 = reader.readInt();
        this.int10 = reader.readInt();

        this.unitList.clear();
        for (int i = 0; i < 11; i++) {
            int buffer = reader.readInt();

            CEventStealthUnit unit = new CEventStealthUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_STEALTH_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_STEALTH_ENTRIES[i].getType();
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
        writer.writeShort(this.short1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
        writer.writeInt(this.int7);
        writer.writeInt(this.int8);
        writer.writeInt(this.int9);
        writer.writeInt(this.int10);

        for (int i = 0; i < 11; i++) {
            CEventStealthUnit target = null;
            for (CEventStealthUnit unit : this.unitList) {
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
