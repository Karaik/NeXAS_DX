package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventEscape__Read
 */
@Data
public class CEventEscape extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventEscapeType {
        private Integer type;
        private String description;
    }

    public static final CEventEscapeType[] CEVENT_ESCAPE_TYPES = {
            new CEventEscapeType(0xFFFFFFFF, "タイプ"),
            new CEventEscapeType(0xFFFFFFFF, "優先順位"),
            new CEventEscapeType(0x9, "チェック方向"),
            new CEventEscapeType(0xFFFFFFFF, "チェック方向補正"),
            new CEventEscapeType(0xFFFFFFFF, "チェック範囲"),
            new CEventEscapeType(0xFFFFFFFF, "チェック範囲（高さ）"),
            new CEventEscapeType(0xFFFFFFFF, "チェック高度補正"),
            new CEventEscapeType(0xFFFFFFFF, "チェック回数"),
            new CEventEscapeType(0x9, "回避方向"),
            new CEventEscapeType(0xFFFFFFFF, "回避方向補正"),
            new CEventEscapeType(0xFFFFFFFF, "短打撃"),
            new CEventEscapeType(0xFFFFFFFF, "突進"),
            new CEventEscapeType(0xFFFFFFFF, "単射撃"),
            new CEventEscapeType(0xFFFFFFFF, "連射撃")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;

    private List<CEventEscapeUnit> ceventEscapeUnitList = new ArrayList<>();

    @Data
    public static class CEventEscapeUnit {
        private Integer ceventEscapeUnitQuantity;
        private String description;
        private Integer buffer;
        private SkillInfoObject data;
    }

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;
        this.int4 = readInt32(bytes, offset); offset += 4;
        this.int5 = readInt32(bytes, offset); offset += 4;
        this.int6 = readInt32(bytes, offset); offset += 4;
        this.int7 = readInt32(bytes, offset); offset += 4;
        this.int8 = readInt32(bytes, offset); offset += 4;

        this.ceventEscapeUnitList.clear();

        for (int i = 0; i < 10; i++) {
            CEventEscapeUnit unit = new CEventEscapeUnit();

            if (i == 2 || i == 8) {
                int buffer = readInt32(bytes, offset); offset += 4;
                unit.setCeventEscapeUnitQuantity(i);
                unit.setDescription(CEVENT_ESCAPE_TYPES[i].getDescription());
                unit.setBuffer(buffer);

                if (buffer != 0) {
//                    int typeId = 9;
                    int typeId = CEVENT_ESCAPE_TYPES[i].getType();
                    SkillInfoObject obj = createCEventObjectByType(typeId);
                    if (obj != null) {
                        offset = obj.readInfo(bytes, offset);
                        unit.setData(obj);
                    }
                }
                this.ceventEscapeUnitList.add(unit);
            }
        }

        return offset;
    }
}