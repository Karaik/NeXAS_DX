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
public class CEventEscape extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventEscapeType {
        private Integer type;
        private String description;
    }

    public static final CEventEscapeType[] CEVENT_ESCAPE_ENTRIES = {
            new CEventEscapeType(0xFFFFFFFF, "タイプ"),
            new CEventEscapeType(0xFFFFFFFF, "優先順位"),
            new CEventEscapeType(0x0E, "チェック方向"),
            new CEventEscapeType(0xFFFFFFFF, "チェック方向補正"),
            new CEventEscapeType(0xFFFFFFFF, "チェック範囲"),
            new CEventEscapeType(0xFFFFFFFF, "チェック範囲（高さ）"),
            new CEventEscapeType(0xFFFFFFFF, "チェック高度補正"),
            new CEventEscapeType(0xFFFFFFFF, "チェック回数"),
            new CEventEscapeType(0x0E, "回避方向"),
            new CEventEscapeType(0xFFFFFFFF, "回避方向補正"),
            new CEventEscapeType(0xFFFFFFFF, "短打撃"),
            new CEventEscapeType(0xFFFFFFFF, "突進"),
            new CEventEscapeType(0xFFFFFFFF, "単射撃"),
            new CEventEscapeType(0xFFFFFFFF, "連射撃")
    };

    public Integer fieldTableAddress = 0x00C36730;
    public Integer wrapperTableAddress = 0x00B5F560;

    @Data
    public static class CEventEscapeUnit {
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
    private List<CEventEscapeUnit> unitList = new ArrayList<>();

    public CEventEscape(Integer typeId) {
        super(typeId);
    }

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

        this.unitList.clear();
        for (int i = 0; i < 10; i++) {
            if (i == 2 || i == 8) {
                int buffer = reader.readInt();
                CEventEscapeUnit unit = new CEventEscapeUnit();
                unit.setUnitSlotNum(i);
                unit.setBuffer(buffer);
                unit.setDescription(CEVENT_ESCAPE_ENTRIES[i].getDescription());
                if (buffer != 0) {
                    SkillInfoObject obj = createCEventObjectByTypeClarias(CEVENT_ESCAPE_ENTRIES[i].getType());
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
                this.unitList.add(unit);
            }
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

        CEventEscapeUnit slot2 = null;
        CEventEscapeUnit slot8 = null;
        for (CEventEscapeUnit unit : this.unitList) {
            if (unit.getUnitSlotNum() == 2) {
                slot2 = unit;
            } else if (unit.getUnitSlotNum() == 8) {
                slot8 = unit;
            }
        }

        writer.writeInt(slot2.getBuffer());
        if (slot2.getBuffer() != 0) {
            slot2.getData().writeInfo(writer);
        }

        writer.writeInt(slot8.getBuffer());
        if (slot8.getBuffer() != 0) {
            slot8.getData().writeInfo(writer);
        }
    }
}
