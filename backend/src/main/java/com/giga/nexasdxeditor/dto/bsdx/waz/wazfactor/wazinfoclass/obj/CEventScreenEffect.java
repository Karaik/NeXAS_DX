package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventScreenEffect__Read
 */
@Data
@NoArgsConstructor
public class CEventScreenEffect extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventScreenEffectType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenEffectType[] CEVENT_SCREEN_EFFECT_TYPES = {
            new CEventScreenEffectType(0xFFFFFFFF, "持続カウンタ"),
            new CEventScreenEffectType(0xFFFFFFFF, "タイプ"),
            new CEventScreenEffectType(0xFFFFFFFF, "速度1"),
            new CEventScreenEffectType(0xFFFFFFFF, "速度2"),
            new CEventScreenEffectType(0x9, "方向")
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    private List<CEventScreenEffectUnit> ceventScreenEffectUnitList = new ArrayList<>();

    public CEventScreenEffect(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventScreenEffectUnit {
        private Integer ceventScreenEffectUnitQuantity;
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

        this.ceventScreenEffectUnitList.clear();

        for (int i = 0; i < 5; i++) {
            if (i == 4) {
                int buffer = reader.readInt();
                CEventScreenEffectUnit unit = new CEventScreenEffectUnit();
                unit.setCeventScreenEffectUnitQuantity(i);
                unit.setDescription(CEVENT_SCREEN_EFFECT_TYPES[i].getDescription());
                unit.setBuffer(buffer);
                unit.setUnitSlotNum(i);

                if (buffer != 0) {
                    int typeId = CEVENT_SCREEN_EFFECT_TYPES[i].getType();
                    SkillInfoObject obj = createCEventObjectByType(typeId);
                    if (obj != null) {
                        obj.readInfo(reader);
                        unit.setData(obj);
                    }
                    this.ceventScreenEffectUnitList.add(unit);
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

        for (int i = 0; i < 5; i++) {
            if (i == 4) {
                CEventScreenEffectUnit target = null;
                for (CEventScreenEffectUnit unit : this.ceventScreenEffectUnitList) {
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
