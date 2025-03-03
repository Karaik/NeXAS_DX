package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.collection.WazInfoCollection;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.WazInfoFactory.createCEventObjectByType;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenLine__Read
 */
@Data
public class CEventScreenLine extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventScreenLineType {
        private Integer type;
        private String name;
    }

    public static final CEventScreenLineType[] CEVENT_SCREEN_LINE_TYPES = {
            new CEventScreenLineType(0xFFFFFFFF, "角度指定"),
            new CEventScreenLineType(0xFFFFFFFF, "左回転"),
            new CEventScreenLineType(0xFFFFFFFF, "右回転"),
            new CEventScreenLineType(0x9, "下"),
            new CEventScreenLineType(0xFFFFFFFF, "上"),
            new CEventScreenLineType(0xFFFFFFFF, "左"),
            new CEventScreenLineType(0xFFFFFFFF, "右"),
            new CEventScreenLineType(0xFFFFFFFF, "ランダム")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;

    private List<CEventScreenLineUnit> ceventScreenLineUnitList = new ArrayList<>();

    @Data
    public static class CEventScreenLineUnit {
        private Integer buffer;
        private WazInfoObject data;
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
        this.int9 = readInt32(bytes, offset); offset += 4;
        this.int10 = readInt32(bytes, offset); offset += 4;

        this.ceventScreenLineUnitList.clear();

        for (int i = 0; i < 11; i++) {
            if (i == 3) { // 仅在索引3时处理
                int buffer = readInt32(bytes, offset); offset += 4;

                CEventScreenLineUnit unit = new CEventScreenLineUnit();
                unit.setBuffer(buffer);

                if (buffer != 0) {
                    // 根据类型ID创建对象（假设i=3对应静态数组的第3项）
                    int typeId = CEVENT_SCREEN_LINE_TYPES[3].getType();
                    WazInfoObject obj = createCEventObjectByType(typeId);

                    if (obj != null) {
                        offset = obj.readInfo(bytes, offset);
                        unit.setData(obj);
                    }
                }
                this.ceventScreenLineUnitList.add(unit);
            }
        }

        return offset;
    }
}
