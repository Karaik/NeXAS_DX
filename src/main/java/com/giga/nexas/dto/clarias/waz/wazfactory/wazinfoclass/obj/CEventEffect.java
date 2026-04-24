package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeClarias;

@Data
@NoArgsConstructor
public class CEventEffect extends SkillInfoObject {
    public static final int SUB_SLOT_COUNT = 46;

    private Byte enabled;
    private List<Integer> intFields = new ArrayList<>();
    private List<Short> shortFields = new ArrayList<>();
    private List<CEventEffectUnit> effectUnitList = new ArrayList<>();
    private byte[] tailBytes;
    private Byte optionalTailByte;

    public CEventEffect(Integer typeId) {
        super(typeId);
    }

    @Data
    @NoArgsConstructor
    public static class CEventEffectUnit {
        public Integer unitSlotNum;
        public Integer typeId;
        private Integer buffer;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.intFields = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            this.intFields.add(reader.readInt());
        }
        this.shortFields = new ArrayList<>();
        for (int i = 0; i < 2; i++) {
            this.shortFields.add(reader.readShort());
        }
        this.effectUnitList = new ArrayList<>();
        for (int i = 0; i < SUB_SLOT_COUNT; i++) {
            int bufferOffset = reader.getPosition();
            int buffer = reader.readInt();
            CEventEffectUnit unit = new CEventEffectUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            int subTypeId = subTypeId(i);
            unit.setTypeId(subTypeId);
            if (buffer != 0 && subTypeId >= 0) {
                SkillInfoObject obj = createEffectSubObject(subTypeId);
                obj.setSlotNum(i);
                int dataOffset = reader.getPosition();
                obj.readInfo(reader);
                unit.setData(obj);
            }
            if (buffer != 0 || unit.getData() != null) {
                this.effectUnitList.add(unit);
            }
        }
        this.tailBytes = reader.readBytes(4);
        if (hasOptionalTailByte()) {
            this.optionalTailByte = reader.readByte();
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        for (Integer value : this.intFields) {
            writer.writeInt(value);
        }
        for (Short value : this.shortFields) {
            writer.writeShort(value);
        }
        for (int i = 0; i < SUB_SLOT_COUNT; i++) {
            CEventEffectUnit target = findUnit(i);
            if (target == null) {
                writer.writeInt(0);
                continue;
            }
            writer.writeInt(target.getBuffer());
            if (target.getBuffer() != 0 && target.getData() != null) {
                target.getData().writeInfo(writer);
            }
        }
        writer.writeBytes(this.tailBytes);
        if (this.optionalTailByte != null) {
            writer.writeByte(this.optionalTailByte);
        }
    }

    private boolean hasOptionalTailByte() {
        return this.shortFields.size() >= 2 && this.shortFields.get(0) == -1 && this.shortFields.get(1) == -1;
    }

    private CEventEffectUnit findUnit(int slot) {
        for (CEventEffectUnit unit : this.effectUnitList) {
            if (unit.getUnitSlotNum() != null && unit.getUnitSlotNum() == slot) {
                return unit;
            }
        }
        return null;
    }

    private static SkillInfoObject createEffectSubObject(int subTypeId) {
        if (subTypeId == 0x7) {
            return new CEventWazaSelectSimple(subTypeId);
        }
        if (subTypeId == 0x9 || subTypeId == 0xE) {
            return new CEventTermSimple(subTypeId, 0x00C35008);
        }
        if (subTypeId == 0x15) {
            return new CEventTermAndIntsSimple(subTypeId, "CEventSpriteYure", 0x00C36460, 4);
        }
        if (subTypeId == 0x16) {
            return new CEventTermAndIntsSimple(subTypeId, "CEventSpriteAttr", 0x00C36508, 8);
        }
        return createCEventObjectByTypeClarias(subTypeId);
    }

    private static int subTypeId(int slot) {
        return switch (slot) {
            case 3 -> 0x7;
            case 4, 5, 6, 14, 16, 17, 18, 19, 26, 34, 35, 36, 41 -> 0x3;
            case 7, 25, 28 -> 0x9;
            case 8, 22, 29 -> 0xE;
            case 9, 10, 11, 23, 24, 30, 31, 32, 33 -> 0x4;
            case 13, 38, 39, 40 -> 0x1;
            case 15 -> 0x22;
            case 20 -> 0x15;
            case 21 -> 0x16;
            case 27 -> 0xC;
            case 37 -> 0x13;
            default -> -1;
        };
    }
}
