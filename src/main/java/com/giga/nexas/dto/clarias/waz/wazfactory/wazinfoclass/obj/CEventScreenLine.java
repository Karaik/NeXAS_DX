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
public class CEventScreenLine extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventScreenLineType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenLineType[] CEVENT_SCREEN_LINE_ENTRIES = {
            new CEventScreenLineType(0xFFFFFFFF, "持続カウンタ"),
            new CEventScreenLineType(0xFFFFFFFF, "変化フレーム数"),
            new CEventScreenLineType(0xFFFFFFFF, "数"),
            new CEventScreenLineType(0x0E, "角度"),
            new CEventScreenLineType(0xFFFFFFFF, "速度"),
            new CEventScreenLineType(0xFFFFFFFF, "長さ"),
            new CEventScreenLineType(0xFFFFFFFF, "太さ"),
            new CEventScreenLineType(0xFFFFFFFF, "輝度"),
            new CEventScreenLineType(0xFFFFFFFF, "色R"),
            new CEventScreenLineType(0xFFFFFFFF, "色G"),
            new CEventScreenLineType(0xFFFFFFFF, "色B")
    };

    public static final String[] CEVENT_SCREEN_LINE_FORMATS = {
            "  角度:%s  速度:%d"
    };

    public Integer fieldTableAddress = 0x00C381D0;
    public Integer wrapperTableAddress = 0x00B65C10;
    public Integer linkedDisplayTableAddress = 0x00B65C8C;
    public Integer linkedFormatTableAddress = 0x00B65CE4;

    @Data
    public static class CEventScreenLineUnit {
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
    private Integer int9;
    private Integer int10;
    private List<CEventScreenLineUnit> unitList = new ArrayList<>();

    public CEventScreenLine(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.int1 = reader.readInt();
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
        int buffer = reader.readInt();
        CEventScreenLineUnit unit = new CEventScreenLineUnit();
        unit.setUnitSlotNum(3);
        unit.setBuffer(buffer);
        unit.setDescription(CEVENT_SCREEN_LINE_ENTRIES[3].getDescription());
        if (buffer != 0) {
            SkillInfoObject obj = createCEventObjectByTypeClarias(CEVENT_SCREEN_LINE_ENTRIES[3].getType());
            obj.readInfo(reader);
            unit.setData(obj);
        }
        this.unitList.add(unit);
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
        writer.writeInt(this.int9);
        writer.writeInt(this.int10);
        CEventScreenLineUnit target = this.unitList.get(0);
        writer.writeInt(target.getBuffer());
        if (target.getBuffer() != 0) {
            target.getData().writeInfo(writer);
        }
    }
}
