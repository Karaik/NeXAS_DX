package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt16;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventCamera__Read
 */
@Data
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

    @Data
    public static class CEventCameraUnit {
        private Integer ceventCameraUnitQuantity;
        private String description;
        private Integer buffer;
        private SkillInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        setShort1(readInt16(bytes, offset)); offset += 2;

        this.ceventCameraUnitList.clear();

        for (int i = 0; i < 5; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;

            CEventCameraUnit unit = new CEventCameraUnit();
            unit.setCeventCameraUnitQuantity(i);
            unit.setDescription(CEVENT_CAMERA_ENTRIES[i].getDescription());
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_CAMERA_ENTRIES[i].getType();
                SkillInfoObject obj = SkillInfoFactory.createCEventObjectByType(typeId);
                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
                ceventCameraUnitList.add(unit);
            }
        }


        return offset;
    }
}

