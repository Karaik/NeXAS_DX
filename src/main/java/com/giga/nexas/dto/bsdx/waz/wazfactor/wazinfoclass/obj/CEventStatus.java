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
 * @Date 2025/2/23
 * @Description CEventStatus
 * CEventStatus__Read
 */
@Data
@NoArgsConstructor
public class CEventStatus extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventStatusType {
        private Integer type;
        private String description;
    }

    public static final CEventStatusType[] CEVENT_STATUS_TYPES = {
            new CEventStatusType(0xFFFFFFFF, "追加タイプ"),
            new CEventStatusType(0xFFFFFFFF, "対象タイプ"),
            new CEventStatusType(0xFFFFFFFF, "最大HP"),
            new CEventStatusType(0xFFFFFFFF, "ブースター数"),
            new CEventStatusType(0xFFFFFFFF, "FCゲージ数"),
            new CEventStatusType(0xFFFFFFFF, "打撃攻撃力"),
            new CEventStatusType(0xFFFFFFFF, "射撃攻撃力"),
            new CEventStatusType(0xFFFFFFFF, "打撃防御力"),
            new CEventStatusType(0xFFFFFFFF, "射撃防御力"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：歩行"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：ダッシュ"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：ブーストダッシュ"),
            new CEventStatusType(0xFFFFFFFF, "移動速度：サーチダッシュ"),
            new CEventStatusType(0xFFFFFFFF, "攻撃速度：打撃"),
            new CEventStatusType(0xFFFFFFFF, "攻撃速度：射撃"),
            new CEventStatusType(0xFFFFFFFF, "熱冷却速度"),
            new CEventStatusType(0xFFFFFFFF, "根性値"),
            new CEventStatusType(0xFFFFFFFF, "データタイプ"),
            new CEventStatusType(0xFFFFFFFF, "終了タイプ"),
            new CEventStatusType(0xFFFFFFFF, "終了カウント"),
            new CEventStatusType(0xFFFFFFFF, "開始時間"),
            new CEventStatusType(0xFFFFFFFF, "実行間隔"),
            new CEventStatusType(0xFFFFFFFF, "優先順位"),
            new CEventStatusType(0x4, "重複タイプ"),
            new CEventStatusType(0xFFFFFFFF, "エフェクト")
    };

    public static final String[] CEVENT_STATUS_FORMATS = {
            "%s : %4d (周期 : %4d)",
            "%s : %4d ",
            "⇒ %4d (周期 : %4d)"
    };

    private byte[] byteData1;
    private byte[] byteData2;
    private byte byte1;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;

    private List<CEventStatusUnit> ceventStatusUnitList = new ArrayList<>();

    public CEventStatus(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventStatusUnit {
        private Integer ceventStatusUnitQuantity;
        private Integer buffer;
        private Integer unitSlotNum; // 记录槽位便于write
        private String description;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        // 读取 0xF 字节
        this.byteData1 = reader.readBytes(15);
        // 读取 0x3C 字节
        this.byteData2 = reader.readBytes(60);

        this.byte1 = reader.readByte();

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
        this.int7 = reader.readInt();
        this.int8 = reader.readInt();
        this.int9 = reader.readInt();

        this.ceventStatusUnitList.clear();

        for (int i = 0; i < 25; i++) {
            int buffer = reader.readInt();
            CEventStatusUnit unit = new CEventStatusUnit();
            unit.setCeventStatusUnitQuantity(i);
            unit.setDescription(CEVENT_STATUS_TYPES[i].getDescription());
            unit.setBuffer(buffer);
            unit.setUnitSlotNum(i);

            if (buffer != 0) {
                int typeId = CEVENT_STATUS_TYPES[i].getType();
                SkillInfoObject obj = createCEventObjectByType(typeId);
                if (obj != null) {
                    obj.readInfo(reader);
                    unit.setData(obj);
                }
                this.ceventStatusUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeBytes(this.byteData1);
        writer.writeBytes(this.byteData2);
        writer.writeByte(this.byte1);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
        writer.writeInt(this.int7);
        writer.writeInt(this.int8);
        writer.writeInt(this.int9);

        for (int i = 0; i < 25; i++) {
            CEventStatusUnit target = null;
            for (CEventStatusUnit unit : this.ceventStatusUnitList) {
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
