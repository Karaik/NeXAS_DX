package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeBsdx;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventEscape__Read
 */
@Data
@NoArgsConstructor
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

    public CEventEscape(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventEscapeUnit {
        private Integer ceventEscapeUnitQuantity;
        private String description;
        private Integer buffer;
        private Integer unitSlotNum; // 记录槽位便于write
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

        this.ceventEscapeUnitList.clear();

        for (int i = 0; i < 10; i++) {
            CEventEscapeUnit unit = new CEventEscapeUnit();

            if (i == 2 || i == 8) {
                int buffer = reader.readInt();
                unit.setCeventEscapeUnitQuantity(i);
                unit.setDescription(CEVENT_ESCAPE_TYPES[i].getDescription());
                unit.setBuffer(buffer);
                unit.setUnitSlotNum(i);

                if (buffer != 0) {
                    int typeId = CEVENT_ESCAPE_TYPES[i].getType();
                    SkillInfoObject obj = createCEventObjectByTypeBsdx(typeId);
                    if (obj != null) {
                        obj.readInfo(reader);
                        unit.setData(obj);
                    }
                }
                this.ceventEscapeUnitList.add(unit);
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

        for (int i = 0; i < 10; i++) {
            if (i == 2 || i == 8) {
                CEventEscapeUnit target = null;
                for (CEventEscapeUnit unit : this.ceventEscapeUnitList) {
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