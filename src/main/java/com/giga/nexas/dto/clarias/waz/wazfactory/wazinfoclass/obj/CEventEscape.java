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
            new CEventEscapeType(0x0B, "チェック方向"),
            new CEventEscapeType(0xFFFFFFFF, "チェック方向補正"),
            new CEventEscapeType(0xFFFFFFFF, "チェック範囲"),
            new CEventEscapeType(0xFFFFFFFF, "チェック範囲（高さ）"),
            new CEventEscapeType(0xFFFFFFFF, "チェック高度補正"),
            new CEventEscapeType(0xFFFFFFFF, "チェック回数"),
            new CEventEscapeType(0x0B, "回避方向"),
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

    private List<CEventEscapeUnit> unitList = new ArrayList<>();

    public CEventEscape(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.unitList.clear();
        for (int i = 0; i < 14; i++) {
            int buffer = reader.readInt();
            CEventEscapeUnit unit = new CEventEscapeUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_ESCAPE_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_ESCAPE_ENTRIES[i].getType();
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
        for (int i = 0; i < 14; i++) {
            CEventEscapeUnit target = null;
            for (CEventEscapeUnit unit : this.unitList) {
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
