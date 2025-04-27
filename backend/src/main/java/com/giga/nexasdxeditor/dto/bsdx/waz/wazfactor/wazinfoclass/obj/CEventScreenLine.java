package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenLine__Read
 */
@Data
public class CEventScreenLine extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventScreenLineType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenLineType[] CEVENT_SCREEN_LINE_TYPES = {
            new CEventScreenLineType(0xFFFFFFFF, "持続カウンタ"),
            new CEventScreenLineType(0xFFFFFFFF, "変化フレーム数"),
            new CEventScreenLineType(0xFFFFFFFF, "数"),
            new CEventScreenLineType(0x9, "角度"),
            new CEventScreenLineType(0xFFFFFFFF, "速度"),
            new CEventScreenLineType(0xFFFFFFFF, "長さ"),
            new CEventScreenLineType(0xFFFFFFFF, "太さ"),
            new CEventScreenLineType(0xFFFFFFFF, "輝度"),
            new CEventScreenLineType(0xFFFFFFFF, "色R"),
            new CEventScreenLineType(0xFFFFFFFF, "色G"),
            new CEventScreenLineType(0xFFFFFFFF, "色B")
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
        private Integer ceventScreenLineUnitQuantity;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.int7 = reader.readInt();
        this.int8 = reader.readInt();
        this.int9 = reader.readInt();
        this.int10 = reader.readInt();

        this.ceventScreenLineUnitList.clear();

        for (int i = 0; i < 11; i++) {
            if (i == 3) {
                int buffer = reader.readInt();

                CEventScreenLineUnit unit = new CEventScreenLineUnit();
                unit.setCeventScreenLineUnitQuantity(i);
                unit.setDescription(CEVENT_SCREEN_LINE_TYPES[i].getDescription());
                unit.setBuffer(buffer);

                if (buffer != 0) {
//                    int typeId = CEVENT_SCREEN_LINE_TYPES[3].getType();
                    int typeId = CEVENT_SCREEN_LINE_TYPES[i].getType();
                    SkillInfoObject obj = createCEventObjectByType(typeId);

                    if (obj != null) {
                        obj.readInfo(reader);
                        unit.setData(obj);
                    }
                    this.ceventScreenLineUnitList.add(unit);
                }
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
        writer.writeInt(this.int7);
        writer.writeInt(this.int8);
        writer.writeInt(this.int9);
        writer.writeInt(this.int10);
        for (CEventScreenLineUnit unit : ceventScreenLineUnitList) {
            writer.writeInt(unit.getBuffer());
            if (unit.getBuffer() != 0 && unit.getData() != null) {
                unit.getData().writeInfo(writer);
            }
        }
    }

}
