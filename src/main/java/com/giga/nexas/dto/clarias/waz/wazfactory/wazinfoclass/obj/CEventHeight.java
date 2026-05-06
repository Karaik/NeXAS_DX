package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

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
public class CEventHeight extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventHeightType {
        private Integer type;
        private String description;
    }

    public static final CEventHeightType[] CEVENT_HEIGHT_ENTRIES = {
            new CEventHeightType(0xFFFFFFFF, "タイプ"),
            new CEventHeightType(0x6, "最大増分(1=0.01)"),
            new CEventHeightType(0x5, "標的高度補正"),
            new CEventHeightType(0xFFFFFFFF, "増分:%s"),
            new CEventHeightType(0xFFFFFFFF, "%s 補正:%s")
    };

    public static final String[] CEVENT_HEIGHT_SEARCH_TYPES = {
            "標的サーチ",
            "親サーチ"
    };

    public Integer fieldTableAddress = 0x00C37028;
    public Integer wrapperTableAddress = 0x00B61D8C;
    public Integer linkedDisplayTableAddress = 0x00B61DBC;
    public Integer linkedSearchTableAddress1 = 0x00B61DF4;
    public Integer linkedSearchTableAddress2 = 0x00B61E00;
    public Integer linkedFormatTableAddress1 = 0x00B61E18;
    public Integer linkedFormatTableAddress2 = 0x00B61E20;

    @Data
    public static class CEventHeightUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventHeightUnit> unitList = new ArrayList<>();

    public CEventHeight(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.unitList.clear();
        for (int i = 0; i < 5; i++) {
            int buffer = reader.readInt();

            CEventHeightUnit unit = new CEventHeightUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_HEIGHT_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_HEIGHT_ENTRIES[i].getType();
            if (buffer != 0) {
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                obj.readInfo(reader);
                unit.setData(obj);
            }

            this.unitList.add(unit);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);

        for (int i = 0; i < 5; i++) {
            CEventHeightUnit target = null;
            for (CEventHeightUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) {
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
