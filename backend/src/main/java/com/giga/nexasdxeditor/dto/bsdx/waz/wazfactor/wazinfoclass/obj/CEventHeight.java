package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description CEventHeight
 * CEventHeight__Read
 */
@Data
public class CEventHeight extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventHeightType {
        private Integer type;
        private String description;
    }

    public static final CEventHeightType[] CEVENT_HEIGHT_TYPES = {
            // .rdata:007F28A8
            new CEventHeightType(0xFFFFFFFF, "タイプ"),
            new CEventHeightType(0x3, "最大増分(1=0.01)"),
            new CEventHeightType(0x2, "標的高度補正")
    };

    private Integer int1;

    private List<CEventHeightUnit> ceventHeightUnitList = new ArrayList<>();

    @Data
    public static class CEventHeightUnit {
        private Integer buffer;
        private WazInfoObject data;
    }


    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;

        this.ceventHeightUnitList.clear();

        for (int i = 0; i < 3; i++) {
            int buffer = readInt32(bytes, offset); offset += 4;
            CEventHeightUnit unit = new CEventHeightUnit();
            unit.setBuffer(buffer);

            if (buffer != 0) {
                int typeId = CEVENT_HEIGHT_TYPES[i].getType();
                WazInfoObject obj = createCEventObjectByType(typeId);
                if (obj != null) {
                    offset = obj.readInfo(bytes, offset);
                    unit.setData(obj);
                }
            }
            this.ceventHeightUnitList.add(unit);
        }

        return offset;
    }
}
