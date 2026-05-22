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
public class CEventMultiLockDraw extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventMultiLockDrawType {
        private Integer type;
        private String description;
    }

    public static final CEventMultiLockDrawType[] CEVENT_MULTI_LOCK_DRAW_ENTRIES = {
            new CEventMultiLockDrawType(0xFFFFFFFF, "エリア描画フラグ"),
            new CEventMultiLockDrawType(0xFFFFFFFF, "カーソル描画フラグ"),
            new CEventMultiLockDrawType(0x22, "ロックＳＥ")
    };

    public Integer fieldTableAddress = 0x00C37450;
    public Integer wrapperTableAddress = 0x00B62C60;

    @Data
    public static class CEventMultiLockDrawUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Byte byte1;
    private Byte byte2;
    private List<CEventMultiLockDrawUnit> unitList = new ArrayList<>();

    public CEventMultiLockDraw(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.byte1 = reader.readByte();
        this.byte2 = reader.readByte();
        this.unitList.clear();
        for (int i = 0; i < 3; i++) {
            int buffer = reader.readInt();
            CEventMultiLockDrawUnit unit = new CEventMultiLockDrawUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_MULTI_LOCK_DRAW_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_MULTI_LOCK_DRAW_ENTRIES[i].getType();
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
        writer.writeByte(this.byte1);
        writer.writeByte(this.byte2);
        for (int i = 0; i < 3; i++) {
            CEventMultiLockDrawUnit target = null;
            for (CEventMultiLockDrawUnit unit : this.unitList) {
                if (unit.getUnitSlotNum() == i) {
                    target = unit;
                    break;
                }
            }
            writer.writeInt(target.getBuffer());
            if (target.getBuffer() != 0) {
                target.getData().writeInfo(writer);
            }
        }
    }
}
