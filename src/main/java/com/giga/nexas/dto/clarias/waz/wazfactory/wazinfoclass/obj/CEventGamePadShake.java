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
        int buffer0 = reader.readInt();
        CEventGamePadShakeUnit unit0 = new CEventGamePadShakeUnit();
        unit0.setUnitSlotNum(0);
        unit0.setBuffer(buffer0);
        unit0.setDescription(CEVENT_GAME_PAD_SHAKE_ENTRIES[0].getDescription());
        this.unitList.add(unit0);

        int buffer1 = reader.readInt();
        CEventGamePadShakeUnit unit1 = new CEventGamePadShakeUnit();
        unit1.setUnitSlotNum(1);
        unit1.setBuffer(buffer1);
        unit1.setDescription(CEVENT_GAME_PAD_SHAKE_ENTRIES[1].getDescription());
        if (buffer1 != 0) {
            SkillInfoObject obj = createCEventObjectByTypeClarias(CEVENT_GAME_PAD_SHAKE_ENTRIES[1].getType());
            obj.readInfo(reader);
            unit1.setData(obj);
        }
        this.unitList.add(unit1);

        int buffer2 = reader.readInt();
        CEventGamePadShakeUnit unit2 = new CEventGamePadShakeUnit();
        unit2.setUnitSlotNum(2);
        unit2.setBuffer(buffer2);
        unit2.setDescription(CEVENT_GAME_PAD_SHAKE_ENTRIES[2].getDescription());
        this.unitList.add(unit2);
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);

        CEventGamePadShakeUnit unit0 = this.unitList.get(0);
        writer.writeInt(unit0.getBuffer());

        CEventGamePadShakeUnit unit1 = this.unitList.get(1);
        writer.writeInt(unit1.getBuffer());
        if (unit1.getBuffer() != 0) {
            unit1.getData().writeInfo(writer);
        }

        CEventGamePadShakeUnit unit2 = this.unitList.get(2);
        writer.writeInt(unit2.getBuffer());
    }
}
