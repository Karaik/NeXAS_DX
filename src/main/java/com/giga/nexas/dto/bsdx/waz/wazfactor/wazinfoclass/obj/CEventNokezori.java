package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.bsdx.waz.wazfactor.SkillInfoFactory.createCEventObjectByType;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/28
 * CEventNokezori__Read
 */
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
            new CEventNokezoriType(0x9, "のけぞり方向"),
            new CEventNokezoriType(0x0B, "のけぞり速度タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "のけぞり速度"),
            new CEventNokezoriType(0xFFFFFFFF, "速度減速率"),
            new CEventNokezoriType(0x0B, "吹き飛び規模タイプ"),
            new CEventNokezoriType(0xFFFFFFFF, "吹き飛び規模"),
            new CEventNokezoriType(0xFFFFFFFF, "吹き飛び重力"),
            new CEventNokezoriType(0xFFFFFFFF, "バウンドLv"),
            new CEventNokezoriType(0xFFFFFFFF, "バウンド速度"),

            // todo???
            new CEventNokezoriType(0xFFFFFFFF, "指定："),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：地上→地上"),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：空中"),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：空中→地上"),
            new CEventNokezoriType(0xFFFFFFFF, "同じ：ダウン"),
            new CEventNokezoriType(0xFFFFFFFF, "なし")
    };

    private Integer int1;
    private Short short1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private Integer int11;
    private Integer int12;
    private Integer int13;

    private List<CEventNokezoriUnit> ceventNokezoriUnitList = new ArrayList<>();

    public CEventNokezori(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventNokezoriUnit {
        private Integer ceventHitUnitQuantity;
        private Integer buffer;
        private Integer unitSlotNum; // 记录槽位便于write
        private String description;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.short1 = reader.readShort();

        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.int7 = reader.readInt();
        this.int8 = reader.readInt();
        this.int9 = reader.readInt();
        this.int10 = reader.readInt();
        this.int11 = reader.readInt();
        this.int12 = reader.readInt();
        this.int13 = reader.readInt();

        this.ceventNokezoriUnitList.clear();

        for (int i = 0; i < 17; i++) {
            int buffer = reader.readInt();

            CEventNokezoriUnit unit = new CEventNokezoriUnit();
            unit.setCeventHitUnitQuantity(i);
            unit.setDescription(CEVENT_NOKEZORI_ENTRIES[i].getDescription());
            unit.setBuffer(buffer);
            unit.setUnitSlotNum(i);

            if (buffer != 0) {
                int typeId = CEVENT_NOKEZORI_ENTRIES[i].getType();
                SkillInfoObject obj = createCEventObjectByType(typeId);

                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
                this.ceventNokezoriUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeShort(this.short1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
        writer.writeInt(this.int7);
        writer.writeInt(this.int8);
        writer.writeInt(this.int9);
        writer.writeInt(this.int10);
        writer.writeInt(this.int11);
        writer.writeInt(this.int12);
        writer.writeInt(this.int13);
        for (int i = 0; i < 17; i++) {
            CEventNokezoriUnit target = null;
            for (CEventNokezoriUnit unit : this.ceventNokezoriUnitList) {
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
