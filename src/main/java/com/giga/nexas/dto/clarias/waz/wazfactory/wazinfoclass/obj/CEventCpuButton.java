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
public class CEventCpuButton extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventCpuButtonType {
        private Integer type;
        private String description;
    }

    public static final CEventCpuButtonType[] CEVENT_CPU_BUTTON_ENTRIES = {
            new CEventCpuButtonType(0x0E, "チェック方向"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック方向補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック範囲"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック範囲（高さ）"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック高度補正"),
            new CEventCpuButtonType(0xFFFFFFFF, "チェック回数"),
            new CEventCpuButtonType(0xFFFFFFFF, "ボタン入力（範囲内）"),
            new CEventCpuButtonType(0xFFFFFFFF, "ボタン入力（範囲外）")
    };

    public Integer fieldTableAddress = 0x00C36800;
    public Integer wrapperTableAddress = 0x00B5F690;
    public Integer linkedDisplayTableAddress = 0x00B5F674;

    @Data
    public static class CEventCpuButtonUnit {
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
    private Short short1;
    private Short short2;
    private List<CEventCpuButtonUnit> unitList = new ArrayList<>();

    public CEventCpuButton(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.short1 = reader.readShort();
        this.short2 = reader.readShort();

        this.unitList.clear();

        int buffer = reader.readInt();
        CEventCpuButtonUnit unit = new CEventCpuButtonUnit();
        unit.setUnitSlotNum(0);
        unit.setBuffer(buffer);
        unit.setDescription(CEVENT_CPU_BUTTON_ENTRIES[0].getDescription());
        if (buffer != 0) {
            SkillInfoObject obj = createCEventObjectByTypeClarias(CEVENT_CPU_BUTTON_ENTRIES[0].getType());
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
        writer.writeShort(this.short1);
        writer.writeShort(this.short2);

        CEventCpuButtonUnit unit = this.unitList.get(0);
        writer.writeInt(unit.getBuffer());
        if (unit.getBuffer() != 0) {
            unit.getData().writeInfo(writer);
        }
    }
}
