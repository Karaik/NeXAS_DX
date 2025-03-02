package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventEscape__Read
 */
@Data
public class CEventEscape extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventEscapeType {
        private Integer type;
        private String description;
    }

    public static final CEventEscapeType[] CEVENT_ESCAPE_TYPES = {
            new CEventEscapeType(0xFFFFFFFF, "タイプ"),
            new CEventEscapeType(0x3, "最大増分(1=0.01)"),
            new CEventEscapeType(0x2, "標的高度補正")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;

    private List<CEventEscapeUnit> ceventEscapeUnitList;

    @Data
    public static class CEventEscapeUnit {
        private Integer buffer;
        private WazInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;
        setInt4(readInt32(bytes, offset)); offset += 4;
        setInt5(readInt32(bytes, offset)); offset += 4;
        setInt6(readInt32(bytes, offset)); offset += 4;
        setInt7(readInt32(bytes, offset)); offset += 4;
        setInt8(readInt32(bytes, offset)); offset += 4;

        List<CEventEscapeUnit> ceventEscapeUnitList = new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            CEventEscapeUnit unit = new CEventEscapeUnit();

            if (i == 2 || i == 8) {
                int buffer = readInt32(bytes, offset); offset += 4;
                unit.setBuffer(buffer);

                if (buffer != 0) {
                    int typeId = 9;
                    WazInfoObject obj = createCEventObjectByType(typeId);
                    if (obj != null) {
                        offset = obj.readInfo(bytes, offset);
                        unit.setData(obj);
                    }
                }

            }  else {
                unit.setBuffer(null);
                unit.setData(null);
            }

            ceventEscapeUnitList.add(unit);
        }

        setCeventEscapeUnitList(ceventEscapeUnitList);

        return offset;
    }
}