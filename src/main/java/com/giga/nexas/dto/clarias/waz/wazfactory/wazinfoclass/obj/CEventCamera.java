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
public class CEventCamera extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventCameraType {
        private Integer type;
        private String description;
    }

    public static final CEventCameraType[] CEVENT_CAMERA_ENTRIES = {
            new CEventCameraType(0x9, "位置"),
            new CEventCameraType(0xE, "方向"),
            new CEventCameraType(0x1, "距離"),
            new CEventCameraType(0x1, "高さ"),
            new CEventCameraType(0xFFFFFFFF, "フラグ")
    };

    public static final String[] CEVENT_CAMERA_EXTRA_FLAGS = {
            "標的スクロール×",
            "CPU時○"
    };

    public static final String[] CEVENT_CAMERA_FORMATS = {
            ":%s",
            ":(距離)%s",
            ":(高度)%s"
    };

    public Integer fieldTableAddress = 0x00C36C10;
    public Integer wrapperTableAddress = 0x00B610B8;
    public Integer linkedDisplayTableAddress = 0x00B6110C;

    @Data
    public static class CEventCameraUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Short extraFlags; // diff: Clarias field table 0x00C36C10 reads 2 bytes at this+40 before wrapper slots
    private List<CEventCameraUnit> unitList = new ArrayList<>();

    public CEventCamera(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.extraFlags = reader.readShort();
        this.unitList.clear();
        for (int i = 0; i < 5; i++) {
            int buffer = reader.readInt();
            CEventCameraUnit unit = new CEventCameraUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_CAMERA_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_CAMERA_ENTRIES[i].getType();
            if (buffer != 0) {
                if (innerTypeId == 0xFFFFFFFF) {
                    throw new OperationException(500, "unexpected non-zero camera wrapper buffer at slot " + i);
                }
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                if (obj == null) {
                    throw new OperationException(500, "missing camera wrapper type at slot " + i + ": " + innerTypeId);
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
        writer.writeShort(this.extraFlags != null ? this.extraFlags : 0);
        for (int i = 0; i < 5; i++) {
            CEventCameraUnit target = null;
            for (CEventCameraUnit unit : this.unitList) {
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
