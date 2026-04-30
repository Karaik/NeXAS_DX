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
public class CEventCpuButton extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventCpuButtonType {
        private Integer type;
        private String description;
    }

    public static final CEventCpuButtonType[] CEVENT_CPU_BUTTON_ENTRIES = {
            new CEventCpuButtonType(0x0B, "チェック方向"),
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

    private List<CEventCpuButtonUnit> unitList = new ArrayList<>();

    public CEventCpuButton(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.unitList.clear();
        for (int i = 0; i < 8; i++) {
            int buffer = reader.readInt();
            CEventCpuButtonUnit unit = new CEventCpuButtonUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_CPU_BUTTON_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_CPU_BUTTON_ENTRIES[i].getType();
            if (buffer != 0) {
                if (innerTypeId == 0xFFFFFFFF) {
                    throw new OperationException(500, "unexpected non-zero cpuButton wrapper buffer at slot " + i);
                }
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                if (obj == null) {
                    throw new OperationException(500, "missing cpuButton wrapper type at slot " + i + ": " + innerTypeId);
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
        for (int i = 0; i < 8; i++) {
            CEventCpuButtonUnit target = null;
            for (CEventCpuButtonUnit unit : this.unitList) {
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
