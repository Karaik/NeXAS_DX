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
public class CEventGamePadShake extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventGamePadShakeType {
        private Integer type;
        private String description;
    }

    public static final CEventGamePadShakeType[] CEVENT_GAME_PAD_SHAKE_ENTRIES = {
            new CEventGamePadShakeType(0xFFFFFFFF, "フラグ"),
            new CEventGamePadShakeType(0x1, "振動Lv( 0 - 10 )"),
            new CEventGamePadShakeType(0xFFFFFFFF, "持続時間(frame:10～)")
    };

    public static final String[] CEVENT_GAME_PAD_SHAKE_EXTRA_ENTRIES = {
            "自機のみ有効"
    };

    public static final String[] CEVENT_GAME_PAD_SHAKE_FORMATS = {
            "振動:",
            "フレーム:"
    };

    public Integer fieldTableAddress = 0x00C37910;
    public Integer wrapperTableAddress = 0x00B643B0;
    public Integer linkedDisplayTableAddress = 0x00B64418;

    @Data
    public static class CEventGamePadShakeUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Integer int1;
    private Integer int2;
    private List<CEventGamePadShakeUnit> unitList = new ArrayList<>();

    public CEventGamePadShake(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();

        this.unitList.clear();
        for (int i = 0; i < 3; i++) {
            int buffer = reader.readInt();

            CEventGamePadShakeUnit unit = new CEventGamePadShakeUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_GAME_PAD_SHAKE_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_GAME_PAD_SHAKE_ENTRIES[i].getType();
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

        for (int i = 0; i < 3; i++) {
            CEventGamePadShakeUnit target = null;
            for (CEventGamePadShakeUnit unit : this.unitList) {
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
