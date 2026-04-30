package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.exception.OperationException;
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
            new CEventScreenLineType(0x0B, "角度"),
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

    private List<CEventScreenLineUnit> unitList = new ArrayList<>();

    public CEventScreenLine(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.unitList.clear();
        for (int i = 0; i < 11; i++) {
            int buffer = reader.readInt();
            CEventScreenLineUnit unit = new CEventScreenLineUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_SCREEN_LINE_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_SCREEN_LINE_ENTRIES[i].getType();
            if (buffer != 0) {
                if (innerTypeId == 0xFFFFFFFF) {
                    throw new OperationException(500, "unexpected non-zero screenLine wrapper buffer at slot " + i);
                }
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                if (obj == null) {
                    throw new OperationException(500, "missing screenLine wrapper type at slot " + i + ": " + innerTypeId);
                }
                obj.readInfo(reader);
                unit.setData(obj);
            }
            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (int i = 0; i < 11; i++) {
            CEventScreenLineUnit target = null;
            for (CEventScreenLineUnit unit : this.unitList) {
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
