package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CEventMultiLock extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventMultiLockType {
        private Integer type;
        private String description;
    }

    public static final CEventMultiLockType[] CEVENT_MULTI_LOCK_ENTRIES = {
            new CEventMultiLockType(0xFFFFFFFF, "ロックタイプ"),
            new CEventMultiLockType(0x07, "OBJ"),
            new CEventMultiLockType(0xFFFFFFFF, "フラグ"),
            new CEventMultiLockType(0xFFFFFFFF, "1フレームロック数")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;

    @Data
    public static class CEventMultiLockUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventMultiLockUnit> unitList = new ArrayList<>();

    public CEventMultiLock(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();

        this.unitList.clear();

        for (int i = 0; i < 4; i++) {
                int buffer = reader.readInt();

            CEventMultiLockUnit unit = new CEventMultiLockUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_MULTI_LOCK_ENTRIES[i].getDescription());

            if (buffer != 0) {
                int typeId = CEVENT_MULTI_LOCK_ENTRIES[i].getType();
                SkillInfoObject obj = SkillInfoFactory.createCEventObjectByTypeBhe(typeId);

                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
            }

            this.unitList.add(unit);
        }
    }
}