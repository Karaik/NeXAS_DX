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
            new CEventRadialLineType(0x9, "座標"),
            new CEventRadialLineType(0xFFFFFFFF, "距離"),
            new CEventRadialLineType(0xFFFFFFFF, "長さ"),
            new CEventRadialLineType(0xFFFFFFFF, "太さ"),
            new CEventRadialLineType(0xFFFFFFFF, "輝度"),
            new CEventRadialLineType(0xFFFFFFFF, "フェード時間"),
            new CEventRadialLineType(0xFFFFFFFF, "色R"),
            new CEventRadialLineType(0xFFFFFFFF, "色G"),
            new CEventRadialLineType(0xFFFFFFFF, "色B")
    };

    public static final String[] CEVENT_RADIAL_LINE_EXTRA_ENTRIES = {
            "設定なし",
            "SPEED_UP",
            "SPEED_DOWN"
    };

    public Integer linkedDisplayTableAddress = 0x00B61F38;
    public Integer linkedExtraTableAddress = 0x00B61FAC;
    public Integer fieldTableAddress = 0x00C37078;
    public Integer wrapperTableAddress = 0x00B61E78;

    // Flat fields from field table (12 active entries), read before wrapper loop
    private Integer flatInt0;   // 4 bytes
    private Integer flatInt1;   // 4 bytes
    private Integer flatInt2;   // 4 bytes
    private Integer flatInt3;   // 4 bytes
    private Integer flatInt4;   // 4 bytes
    private Integer flatInt5;   // 4 bytes
    private Integer flatInt6;   // 4 bytes
    private Integer flatInt7;   // 4 bytes
    private Integer flatInt8;   // 4 bytes
    private Integer flatInt9;   // 4 bytes
    private Integer flatInt10;  // 4 bytes
    private Integer flatInt11;  // 4 bytes

    @Data
    public static class CEventRadialLineUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventRadialLineUnit> unitList = new ArrayList<>();

    public CEventRadialLine(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        // Read flat fields from field table
        this.flatInt0 = reader.readInt();
        this.flatInt1 = reader.readInt();
        this.flatInt2 = reader.readInt();
        this.flatInt3 = reader.readInt();
        this.flatInt4 = reader.readInt();
        this.flatInt5 = reader.readInt();
        this.flatInt6 = reader.readInt();
        this.flatInt7 = reader.readInt();
        this.flatInt8 = reader.readInt();
        this.flatInt9 = reader.readInt();
        this.flatInt10 = reader.readInt();
        this.flatInt11 = reader.readInt();
        // Read wrapper entries — only slot 4 has binary data
        this.unitList.clear();
        for (int i = 0; i < 13; i++) {
            CEventRadialLineUnit unit = new CEventRadialLineUnit();
            unit.setUnitSlotNum(i);
            unit.setDescription(CEVENT_RADIAL_LINE_TYPES[i].getDescription());
            if (i == 4) {
                int buffer = reader.readInt();
                unit.setBuffer(buffer);
                int innerTypeId = CEVENT_RADIAL_LINE_TYPES[i].getType();
                if (buffer != 0) {
                    if (innerTypeId == 0xFFFFFFFF) {
                        throw new OperationException(500, "unexpected non-zero radialLine wrapper buffer at slot " + i);
                    }
                    SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                    if (obj == null) {
                        throw new OperationException(500, "missing radialLine wrapper type at slot " + i + ": " + innerTypeId);
                    }
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
            } else {
                unit.setBuffer(0);
            }
            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        // Write flat fields
        writer.writeInt(this.flatInt0);
        writer.writeInt(this.flatInt1);
        writer.writeInt(this.flatInt2);
        writer.writeInt(this.flatInt3);
        writer.writeInt(this.flatInt4);
        writer.writeInt(this.flatInt5);
        writer.writeInt(this.flatInt6);
        writer.writeInt(this.flatInt7);
        writer.writeInt(this.flatInt8);
        writer.writeInt(this.flatInt9);
        writer.writeInt(this.flatInt10);
        writer.writeInt(this.flatInt11);
        // Write wrapper entries — only slot 4 has binary data
        for (int i = 0; i < 13; i++) {
            if (i == 4) {
                CEventRadialLineUnit target = null;
                for (CEventRadialLineUnit unit : this.unitList) {
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
}
