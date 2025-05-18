package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeBhe;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * CEventRadialLine__Read
 */
@Data
@NoArgsConstructor
public class CEventRadialLine extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventRadialLineType {
        private Integer type;
        private String description;
    }

    public static final CEventRadialLineType[] CEVENT_RADIAL_LINE_TYPES = {
            new CEventRadialLineType(0xFFFFFFFF, "持続カウンタ"),
            new CEventRadialLineType(0xFFFFFFFF, "フレーム数"),
            new CEventRadialLineType(0xFFFFFFFF, "フレーム速度"),
            new CEventRadialLineType(0xFFFFFFFF, "数"),
            new CEventRadialLineType(0x7, "座標"),
            new CEventRadialLineType(0xFFFFFFFF, "距離"),
            new CEventRadialLineType(0xFFFFFFFF, "長さ"),
            new CEventRadialLineType(0xFFFFFFFF, "太さ"),
            new CEventRadialLineType(0xFFFFFFFF, "輝度"),
            new CEventRadialLineType(0xFFFFFFFF, "フェード時間"),
            new CEventRadialLineType(0xFFFFFFFF, "色R"),
            new CEventRadialLineType(0xFFFFFFFF, "色G"),
            new CEventRadialLineType(0xFFFFFFFF, "色B")
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
    private Integer int11;
    private Integer int12;

    private List<CEventRadialLineUnit> ceventRadialLineUnitList = new ArrayList<>();

    public CEventRadialLine(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventRadialLineUnit {
        private Integer ceventRadialLineUnitQuantity;
        private Integer buffer;
        private Integer unitSlotNum; // 记录槽位便于write
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
        this.int11 = reader.readInt();
        this.int12 = reader.readInt();

        this.ceventRadialLineUnitList.clear();

        for (int i = 0; i < 13; i++) {
            if (i == 4) {
                    int buffer = reader.readInt();
                CEventRadialLineUnit unit = new CEventRadialLineUnit();
                unit.setCeventRadialLineUnitQuantity(i);
                unit.setDescription(CEVENT_RADIAL_LINE_TYPES[i].getDescription());
                unit.setBuffer(buffer);
                unit.setUnitSlotNum(i);

                if (buffer != 0) {
                    int typeId = CEVENT_RADIAL_LINE_TYPES[i].getType();
//                    SkillInfoObject obj = createCEventObjectByType(5);
                    SkillInfoObject obj = createCEventObjectByTypeBhe(typeId);

                    if (obj != null) {
                        obj.readInfo(reader);
                        unit.setData(obj);
                    }
                }
                this.ceventRadialLineUnitList.add(unit);
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
        writer.writeInt(this.int11);
        writer.writeInt(this.int12);

        for (int i = 0; i < 13; i++) {
            if (i == 4) {
                CEventRadialLineUnit target = null;
                for (CEventRadialLineUnit unit : this.ceventRadialLineUnitList) {
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

}


