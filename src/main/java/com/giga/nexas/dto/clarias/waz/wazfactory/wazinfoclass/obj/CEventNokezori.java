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
public class CEventNokezori extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventNokezoriType {
        private Integer type;
        private String description;
    }

    public static final CEventNokezoriType[] CEVENT_NOKEZORI_ENTRIES = {
            new CEventNokezoriType(0xFFFFFFFF, "処理タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "フラグ"),
            new CEventNokezoriType(0xFFFFFFFF, "停止時間"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ(のけぞり時)"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ:自機補正"),
            new CEventNokezoriType(0xFFFFFFFF, "ﾋｯﾄﾊﾞｯｸ:自機補正(のけぞり時)"),
            new CEventNokezoriType(0xFFFFFFFF, "ダウン時間"),
            new CEventNokezoriType(0xE, "のけぞり方向"),
            new CEventNokezoriType(0x11, "のけぞり速度タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "のけぞり速度"),
            new CEventNokezoriType(0xFFFFFFFF, "速度減速率"),
            new CEventNokezoriType(0x11, "吹き飛び規模タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "吹き飛び規模"),
            new CEventNokezoriType(0xFFFFFFFF, "吹き飛び重力"),
            new CEventNokezoriType(0xFFFFFFFF, "バウンドLv"),
            new CEventNokezoriType(0xFFFFFFFF, "バウンド速度")
    };

    public static final String[] CEVENT_NOKEZORI_EXTRA_ENTRIES = {
            "攻撃者の速度反映",
            "攻撃者の上昇量反映",
            "のけぞり速度クリップ（攻撃者の速度反映時のみ）",
            "吹き飛び量クリップ（攻撃者の上昇量反映時のみ）",
            "回転吹き飛び"
    };

    public Integer fieldTableAddress = 0x00C382B0;
    public Integer wrapperTableAddress = 0x00B66080;

    // Flat fields from field table (14 active entries), read before wrapper loop
    // Field table order: 4, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4
    private Integer flatInt0;    // 4 bytes
    private Short flatShort1;    // 2 bytes
    private Integer flatInt2;    // 4 bytes
    private Integer flatInt3;    // 4 bytes
    private Integer flatInt4;    // 4 bytes
    private Integer flatInt5;    // 4 bytes
    private Integer flatInt6;    // 4 bytes
    private Integer flatInt7;    // 4 bytes
    private Integer flatInt8;    // 4 bytes
    private Integer flatInt9;    // 4 bytes
    private Integer flatInt10;   // 4 bytes
    private Integer flatInt11;   // 4 bytes
    private Integer flatInt12;   // 4 bytes
    private Integer flatInt13;   // 4 bytes

    @Data
    public static class CEventNokezoriUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventNokezoriUnit> unitList = new ArrayList<>();

    public CEventNokezori(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        // Read flat fields from field table
        this.flatInt0 = reader.readInt();
        this.flatShort1 = reader.readShort();
        this.flatInt2 = reader.readInt();
        this.flatInt3 = reader.readInt();
        this.flatInt4 = reader.readInt();
        this.flatInt5 = reader.readInt();
        this.flatInt6 = reader.readInt();
        this.flatInt7 = reader.readInt();
        this.flatInt8 = reader.readInt();
        this.flatInt9 = reader.readInt();
        this.flatInt10 = reader.readInt();
        this.flatInt11 = reader.readInt();
        this.flatInt12 = reader.readInt();
        this.flatInt13 = reader.readInt();
        // Read wrapper entries
        this.unitList.clear();
        for (int i = 0; i < 17; i++) {
            int buffer = reader.readInt();
            CEventNokezoriUnit unit = new CEventNokezoriUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_NOKEZORI_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_NOKEZORI_ENTRIES[i].getType();
            if (buffer != 0) {
                if (innerTypeId == 0xFFFFFFFF) {
                    throw new OperationException(500, "unexpected non-zero nokezori wrapper buffer at slot " + i);
                }
                SkillInfoObject obj = createCEventObjectByTypeClarias(innerTypeId);
                if (obj == null) {
                    throw new OperationException(500, "missing nokezori wrapper type at slot " + i + ": " + innerTypeId);
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
        // Write flat fields
        writer.writeInt(this.flatInt0);
        writer.writeShort(this.flatShort1);
        writer.writeInt(this.flatInt2);
        writer.writeInt(this.flatInt3);
        writer.writeInt(this.flatInt4);
        writer.writeInt(this.flatInt5);
        writer.writeInt(this.flatInt6);
        writer.writeInt(this.flatInt7);
        writer.writeInt(this.flatInt8);
        writer.writeInt(this.flatInt9);
        writer.writeInt(this.flatInt10);
        writer.writeInt(this.flatInt11);
        writer.writeInt(this.flatInt12);
        writer.writeInt(this.flatInt13);
        // Write wrapper entries
        for (int i = 0; i < 17; i++) {
            CEventNokezoriUnit target = null;
            for (CEventNokezoriUnit unit : this.unitList) {
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
