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
public class CEventScreenEffect extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventScreenEffectType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenEffectType[] CEVENT_SCREEN_EFFECT_ENTRIES = {
            new CEventScreenEffectType(0xFFFFFFFF, "持続カウンタ"),
            new CEventScreenEffectType(0xFFFFFFFF, "タイプ"),
            new CEventScreenEffectType(0xFFFFFFFF, "速度1"),
            new CEventScreenEffectType(0xFFFFFFFF, "速度2"),
            new CEventScreenEffectType(0x0B, "方向")
    };

    public static final String[] CEVENT_SCREEN_EFFECT_EXTRA_ENTRIES = {
            "炎",
            "爆発"
    };

    public static final String[] CEVENT_SCREEN_EFFECT_FORMATS = {
            "  角度:%s  速度:%d",
            "  角度:%s  速度1:%d  速度2:%d",
            "  速度:%d"
    };

    public Integer fieldTableAddress = 0x00C38150;
    public Integer wrapperTableAddress = 0x00B65AC0;
    public Integer linkedDisplayTableAddress = 0x00B65AF4;
    public Integer linkedTypeNameTableAddress = 0x00B65B3C;
    public Integer linkedFormatTableAddress1 = 0x00B65B54;
    public Integer linkedFormatTableAddress2 = 0x00B65B68;
    public Integer linkedFormatTableAddress3 = 0x00B65B88;

    @Data
    public static class CEventScreenEffectUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventScreenEffectUnit> unitList = new ArrayList<>();

    public CEventScreenEffect(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.unitList.clear();
        for (int i = 0; i < 5; i++) {
            int buffer = reader.readInt();
            CEventScreenEffectUnit unit = new CEventScreenEffectUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_SCREEN_EFFECT_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_SCREEN_EFFECT_ENTRIES[i].getType();
            if (buffer != 0) {
                if (innerTypeId == 0xFFFFFFFF) {
                    throw new OperationException(500, "unexpected non-zero screenEffect wrapper buffer at slot " + i);
                }
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                if (obj == null) {
                    throw new OperationException(500, "missing screenEffect wrapper type at slot " + i + ": " + innerTypeId);
                }
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
            CEventScreenEffectUnit target = null;
            for (CEventScreenEffectUnit unit : this.unitList) {
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
