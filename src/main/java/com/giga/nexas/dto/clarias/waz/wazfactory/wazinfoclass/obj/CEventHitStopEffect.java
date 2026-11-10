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
public class CEventHitStopEffect extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventHitStopEffectType {
        private Integer type;
        private String description;
    }

    public static final CEventHitStopEffectType[] CEVENT_HIT_STOP_EFFECT_ENTRIES = {
            new CEventHitStopEffectType(0xFFFFFFFF, "発動タイプ"),
            new CEventHitStopEffectType(0x7, "ヒットストップ演出"),
            new CEventHitStopEffectType(0xFFFFFFFF, "フラグ")
    };

    @Data
    public static class CEventHitStopEffectUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Byte byte1;
    private Byte byte2;
    private List<CEventHitStopEffectUnit> unitList = new ArrayList<>();

    public CEventHitStopEffect(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.byte1 = reader.readByte();
        this.byte2 = reader.readByte();

        this.unitList.clear();
        for (int i = 0; i < 3; i++) {
            int buffer = reader.readInt();

            CEventHitStopEffectUnit unit = new CEventHitStopEffectUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_HIT_STOP_EFFECT_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_HIT_STOP_EFFECT_ENTRIES[i].getType();
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

        for (int i = 0; i < 3; i++) {
            CEventHitStopEffectUnit target = null;
            for (CEventHitStopEffectUnit unit : this.unitList) {
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
