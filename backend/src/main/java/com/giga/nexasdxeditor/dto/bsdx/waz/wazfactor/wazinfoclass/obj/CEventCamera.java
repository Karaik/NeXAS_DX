package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory;
import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventCamera__Read
 */
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
            new CEventCameraType(0x5, "位置"),
            new CEventCameraType(0x9, "方向"),
            new CEventCameraType(0x0, "距離"),
            new CEventCameraType(0x0, "高さ"),
            new CEventCameraType(0xFFFFFFFF, "フラグ")
    };

    private Short short1;
    private List<CEventCameraUnit> ceventCameraUnitList = new ArrayList<>();

    public CEventCamera(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventCameraUnit {
        private Integer ceventCameraUnitQuantity;
        private String description;
        private Integer buffer;
        private Integer unitSlotNum; // 记录槽位便于write
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        setShort1(reader.readShort());

        this.ceventCameraUnitList.clear();

        for (int i = 0; i < 5; i++) {
            int buffer = reader.readInt();

            CEventCameraUnit unit = new CEventCameraUnit();
            unit.setCeventCameraUnitQuantity(i);
            unit.setDescription(CEVENT_CAMERA_ENTRIES[i].getDescription());
            unit.setBuffer(buffer);
            unit.setUnitSlotNum(i);

            if (buffer != 0) {
                int typeId = CEVENT_CAMERA_ENTRIES[i].getType();
                SkillInfoObject obj = SkillInfoFactory.createCEventObjectByType(typeId);
                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
                ceventCameraUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeShort(this.short1);

        for (int i = 0; i < 5; i++) {
            CEventCameraUnit target = null;

            for (CEventCameraUnit unit : this.ceventCameraUnitList) {
                if (unit.getUnitSlotNum() != null && unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
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

