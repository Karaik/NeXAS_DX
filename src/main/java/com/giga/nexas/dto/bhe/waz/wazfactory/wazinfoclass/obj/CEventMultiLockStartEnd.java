package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CEventMultiLockStartEnd extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventMultiLockStartEndType {
        private Integer type;
        private String description;
    }

    public static final CEventMultiLockStartEndType[] CEVENT_MULTI_LOCK_START_END_ENTRIES = {
            new CEventMultiLockStartEndType(0xFFFFFFFF, "開始・終了タイプ"),
            new CEventMultiLockStartEndType(0x7, "開始OBJ"),
            new CEventMultiLockStartEndType(0xFFFFFFFF, "拡大速度(%)"),
            new CEventMultiLockStartEndType(0xFFFFFFFF, "直径開始値(100 = 200 dot)"),
            new CEventMultiLockStartEndType(0xFFFFFFFF, "直径最大値(100 = 200 dot)"),
            new CEventMultiLockStartEndType(0xFFFFFFFF, "フラグ")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Short short1;

    @Data
    public static class CEventMultiLockStartEndUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventMultiLockStartEndUnit> unitList = new ArrayList<>();

    public CEventMultiLockStartEnd(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.short1 = reader.readShort();

        this.unitList.clear();

        for (int i = 0; i < 6; i++) {
                int buffer = reader.readInt();

            CEventMultiLockStartEndUnit unit = new CEventMultiLockStartEndUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_MULTI_LOCK_START_END_ENTRIES[i].getDescription());

            if (buffer != 0) {
                int typeId = CEVENT_MULTI_LOCK_START_END_ENTRIES[i].getType();
                SkillInfoObject obj = SkillInfoFactory.createCEventObjectByTypeBhe(typeId);

                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
            }

            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeShort(this.short1);
        for (int i = 0; i < 6; i++) {
            CEventMultiLockStartEndUnit target = null;
            for (CEventMultiLockStartEndUnit unit : this.unitList) {
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
