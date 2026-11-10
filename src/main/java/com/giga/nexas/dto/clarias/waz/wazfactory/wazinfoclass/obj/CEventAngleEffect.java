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
public class CEventAngleEffect extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventAngleEffectType {
        private Integer type;
        private String description;
    }

    public static final CEventAngleEffectType[] CEVENT_ANGLE_EFFECT_ENTRIES = {
            new CEventAngleEffectType(0xFFFFFFFF, "タイプ"),
            new CEventAngleEffectType(0xFFFFFFFF, "回転方向"),
            new CEventAngleEffectType(0x1, "角度[0-360](1=1度)"),
            new CEventAngleEffectType(0x1, "回転量[0-3600](10=1度)")
    };

    @Data
    public static class CEventAngleEffectUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Byte angleType;
    private Byte rotationDirection;
    private List<CEventAngleEffectUnit> unitList = new ArrayList<>();

    public CEventAngleEffect(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.angleType = reader.readByte();
        this.rotationDirection = reader.readByte();

        this.unitList.clear();
        for (int i = 0; i < 4; i++) {
            int buffer = reader.readInt();

            CEventAngleEffectUnit unit = new CEventAngleEffectUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_ANGLE_EFFECT_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_ANGLE_EFFECT_ENTRIES[i].getType();
            if (buffer != 0) {
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                obj.readInfo(reader);
                unit.setData(obj);
            }

            this.unitList.add(unit);
        }

        // exe 内会设置为固定值，不复刻，此处保留信息
        // if (Byte.toUnsignedInt(this.angleType) == 2) {
        //     this.rotationDirection = 2;
        //     for (CEventAngleEffectUnit unit : this.unitList) {
        //         if (unit.getUnitSlotNum() == 2 && unit.getData() instanceof CEventVal val) {
        //             val.setInt1(1);
        //             val.setInt2(0);
        //             val.setInt3(0);
        //             val.setInt4(360);
        //             break;
        //         }
        //     }
        // }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.angleType);
        writer.writeByte(this.rotationDirection);

        for (int i = 0; i < 4; i++) {
            CEventAngleEffectUnit target = null;
            for (CEventAngleEffectUnit unit : this.unitList) {
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
