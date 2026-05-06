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
public class CEventHitSousai extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventHitSousaiType {
        private Integer type;
        private String description;
    }

    public static final CEventHitSousaiType[] CEVENT_HIT_SOUSAI_ENTRIES = {
            new CEventHitSousaiType(0xFFFFFFFF, "対象OBJ"),
            new CEventHitSousaiType(0xFFFFFFFF, "チェック対象"),
            new CEventHitSousaiType(0xFFFFFFFF, "攻撃レベル"),
            new CEventHitSousaiType(0x7, "(削除予定)エフェクト（自機・味方）"),
            new CEventHitSousaiType(0x7, "張り付けエフェクト(通常)"),
            new CEventHitSousaiType(0x7, "張り付けエフェクト(強)"),
            new CEventHitSousaiType(0xFFFFFFFF, "有効角度(360=全方向)"),
            new CEventHitSousaiType(0xFFFFFFFF, "相手を弾く速度(弾かれ速度優先)(100=1)"),
            new CEventHitSousaiType(0xFFFFFFFF, "相殺ダメージ値")
    };

    @Data
    public static class CEventHitSousaiUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private Byte byte1;
    private Byte byte2;
    private Byte byte3;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private List<CEventHitSousaiUnit> unitList = new ArrayList<>();

    public CEventHitSousai(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.byte1 = reader.readByte();
        this.byte2 = reader.readByte();
        this.byte3 = reader.readByte();
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();

        this.unitList.clear();
        for (int i = 0; i < 9; i++) {
            int buffer = reader.readInt();

            CEventHitSousaiUnit unit = new CEventHitSousaiUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_HIT_SOUSAI_ENTRIES[i].getDescription());

            int innerTypeId = CEVENT_HIT_SOUSAI_ENTRIES[i].getType();
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
        writer.writeByte(this.byte3);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);

        for (int i = 0; i < 9; i++) {
            CEventHitSousaiUnit target = null;
            for (CEventHitSousaiUnit unit : this.unitList) {
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
