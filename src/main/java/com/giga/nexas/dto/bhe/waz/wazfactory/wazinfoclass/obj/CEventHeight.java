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
 * @Description CEventHeight
 * CEventHeight__Read
 */
@Data
@NoArgsConstructor
public class CEventHeight extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventHeightType {
        private Integer type;
        private String description;
    }

    public static final CEventHeightType[] CEVENT_HEIGHT_TYPES = {
            new CEventHeightType(0xFFFFFFFF, "タイプ"),
            new CEventHeightType(0x5, "最大増分(1=0.01)"),
            new CEventHeightType(0x4, "標的高度補正"),

            // todo ???
            new CEventHeightType(0xFFFFFFFF, "増分:%s"),
            new CEventHeightType(0xFFFFFFFF, "%s 補正:%s")
    };

    private Integer int1;

    private List<CEventHeightUnit> ceventHeightUnitList = new ArrayList<>();

    public CEventHeight(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventHeightUnit {
        private Integer ceventHeightUnitQuantity;
        private String description;
        private Integer buffer;
        private Integer unitSlotNum; // 记录槽位便于write
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();

        this.ceventHeightUnitList.clear();

        for (int i = 0; i < 3; i++) {
                int buffer = reader.readInt();
            CEventHeightUnit unit = new CEventHeightUnit();
            unit.setCeventHeightUnitQuantity(i);
            unit.setDescription(CEVENT_HEIGHT_TYPES[i].getDescription());
            unit.setBuffer(buffer);
            unit.setUnitSlotNum(i);

            if (buffer != 0) {
                int typeId = CEVENT_HEIGHT_TYPES[i].getType();
                SkillInfoObject obj = createCEventObjectByTypeBhe(typeId);
                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
                this.ceventHeightUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        for (int i = 0; i < 3; i++) {
            CEventHeightUnit target = null;
            for (CEventHeightUnit unit : this.ceventHeightUnitList) {
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
