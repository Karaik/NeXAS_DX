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
public class CEventHit extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventHitType {
        private Integer type;
        private String description;
    }

    public static final CEventHitType[] CEVENT_HIT_ENTRIES = {
            new CEventHitType(0xFFFFFFFF, "攻撃グループ"),
            new CEventHitType(0xFFFFFFFF, "攻撃グループ2"),
            new CEventHitType(0xFFFFFFFF, "ヒットカウンタ初期化"),
            new CEventHitType(0xFFFFFFFF, "フラグ"),
            new CEventHitType(0xFFFFFFFF, "フラグ２"),
            new CEventHitType(0xFFFFFFFF, "攻撃タイプ"),
            new CEventHitType(0xFFFFFFFF, "ヒット数"),
            new CEventHitType(0xFFFFFFFF, "ヒット間隔"),
            new CEventHitType(0x2B, "攻撃力"),
            new CEventHitType(0xFFFFFFFF, "攻撃力最低値"),
            new CEventHitType(0xFFFFFFFF, "攻撃力溜め反映率"),
            new CEventHitType(0xFFFFFFFF, "装甲攻撃力"),
            new CEventHitType(0xFFFFFFFF, "ダウン時攻撃力（％）"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "魔痕タイプ"),
            new CEventHitType(0xFFFFFFFF, "魔痕ダメージ"),
            new CEventHitType(0xFFFFFFFF, "自分停止時間"),
            new CEventHitType(0x3, "消滅時間"),
            new CEventHitType(0x7, "ヒットエフェクト"),
            new CEventHitType(0x7, "ヒットエフェクト(相手付与)"),
            new CEventHitType(0x2C, "のけぞり（地上→地上）"),
            new CEventHitType(0x2C, "のけぞり（空中）"),
            new CEventHitType(0x2C, "のけぞり（空中→地上）"),
            new CEventHitType(0x2C, "のけぞり（ダウン）"),
            new CEventHitType(0xFFFFFFFF, "のけぞり優先順位"),
            new CEventHitType(0xFFFFFFFF, "ヒットストップ"),
            new CEventHitType(0xFFFFFFFF, "ヒットストップ（自機）"),
            new CEventHitType(0xFFFFFFFF, "スロー反映率"),
            new CEventHitType(0xFFFFFFFF, "画面：拡大縮小時間"),
            new CEventHitType(0x3, "画面：拡大縮小"),
            new CEventHitType(0xFFFFFFFF, "画面：振動時間"),
            new CEventHitType(0x24, "画面：振動"),
            new CEventHitType(0x33, "ゲームパッド振動"),
            new CEventHitType(0x22, "ヒットＳＥ"),
            new CEventHitType(0xFFFFFFFF, "----"),
            new CEventHitType(0xFFFFFFFF, "ダメージ色時間"),
            new CEventHitType(0xFFFFFFFF, "キャンセルフラグ"),
            new CEventHitType(0x2E, "メカステータス増減値")
    };

    public static final String[] CEVENT_HIT_EXTRA_ENTRIES = {
            "敵",
            "味方",
            "自分",
            "プレイヤー(自機)",
            "鉄ＳＥ",
            "破片",
            "火花",
            "メカ",
            "弾",
            "マップＯＢＪ",
            "位置ＯＢＪ",
            "位置ＯＢＪ以外",
            "標的ＯＢＪ",
            "標的ＯＢＪ以外",
            "キャンセル不可",
            "ヒット",
            "ヒット(Point)"
    };

    public Integer fieldTableAddress = 0x00C35320;
    public Integer wrapperTableAddress = 0x00B5BAC0;

    // Flat fields from field table (entries 2-37), read before wrapper loop
    private Short hitGroup;              // 2 bytes
    private Short hitGroup2;             // 2 bytes
    private Integer hitCounterInit;      // 4 bytes
    private Integer flags;               // 4 bytes
    private Integer flags2;              // 4 bytes
    private Integer attackType;          // 4 bytes
    private Byte hitCount;               // 1 byte
    private Integer hitInterval;         // 4 bytes
    private Integer attackPower;         // 4 bytes
    private Integer attackPowerMin;      // 4 bytes
    private Integer attackPowerChargeRate; // 4 bytes
    private Integer armorAttackPower;    // 4 bytes
    private Integer downAttackPercent;   // 4 bytes
    private Integer flatInt13;           // 4 bytes (separator)
    private Integer flatInt14;           // 4 bytes (separator)
    private Integer flatInt15;           // 4 bytes (separator)
    private Integer flatInt16;           // 4 bytes (separator)
    private Integer flatInt17;           // 4 bytes (separator)
    private Integer flatInt18;           // 4 bytes (separator)
    private Integer flatInt19;           // 4 bytes (separator)
    private Integer makMarkType;         // 4 bytes
    private Integer makMarkDamage;       // 4 bytes
    private Integer selfStopTime;        // 4 bytes
    private Byte vanishTime;             // 1 byte
    private Integer hitEffect;           // 4 bytes
    private Integer hitEffectOpponent;   // 4 bytes
    private Integer nokezoriGround;      // 4 bytes
    private Integer nokezoriAir;         // 4 bytes
    private Integer nokezoriAirToGround; // 4 bytes
    private Integer nokezoriDown;        // 4 bytes
    private Integer nokezoriPriority;    // 4 bytes
    private Integer hitStop;             // 4 bytes
    private Integer hitStopSelf;         // 4 bytes
    private Integer slowRate;            // 4 bytes
    private Integer screenZoomTime;      // 4 bytes
    private Integer screenZoom;          // 4 bytes

    @Data
    public static class CEventHitUnit {
        private Integer unitSlotNum;
        private Integer buffer;
        private String description;
        private SkillInfoObject data;
    }

    private List<CEventHitUnit> unitList = new ArrayList<>();

    public CEventHit(Integer typeId) { super(typeId); }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        // Read flat fields from field table (entries 2-37)
        this.hitGroup = reader.readShort();
        this.hitGroup2 = reader.readShort();
        this.hitCounterInit = reader.readInt();
        this.flags = reader.readInt();
        this.flags2 = reader.readInt();
        this.attackType = reader.readInt();
        this.hitCount = reader.readByte();
        this.hitInterval = reader.readInt();
        this.attackPower = reader.readInt();
        this.attackPowerMin = reader.readInt();
        this.attackPowerChargeRate = reader.readInt();
        this.armorAttackPower = reader.readInt();
        this.downAttackPercent = reader.readInt();
        this.flatInt13 = reader.readInt();
        this.flatInt14 = reader.readInt();
        this.flatInt15 = reader.readInt();
        this.flatInt16 = reader.readInt();
        this.flatInt17 = reader.readInt();
        this.flatInt18 = reader.readInt();
        this.flatInt19 = reader.readInt();
        this.makMarkType = reader.readInt();
        this.makMarkDamage = reader.readInt();
        this.selfStopTime = reader.readInt();
        this.vanishTime = reader.readByte();
        this.hitEffect = reader.readInt();
        this.hitEffectOpponent = reader.readInt();
        this.nokezoriGround = reader.readInt();
        this.nokezoriAir = reader.readInt();
        this.nokezoriAirToGround = reader.readInt();
        this.nokezoriDown = reader.readInt();
        this.nokezoriPriority = reader.readInt();
        this.hitStop = reader.readInt();
        this.hitStopSelf = reader.readInt();
        this.slowRate = reader.readInt();
        this.screenZoomTime = reader.readInt();
        this.screenZoom = reader.readInt();
        // Read wrapper entries
        this.unitList.clear();
        for (int i = 0; i < 47; i++) {
            int buffer = reader.readInt();
            CEventHitUnit unit = new CEventHitUnit();
            unit.setUnitSlotNum(i);
            unit.setBuffer(buffer);
            unit.setDescription(CEVENT_HIT_ENTRIES[i].getDescription());
            int innerTypeId = CEVENT_HIT_ENTRIES[i].getType();
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
        // Write flat fields
        writer.writeShort(this.hitGroup);
        writer.writeShort(this.hitGroup2);
        writer.writeInt(this.hitCounterInit);
        writer.writeInt(this.flags);
        writer.writeInt(this.flags2);
        writer.writeInt(this.attackType);
        writer.writeByte(this.hitCount);
        writer.writeInt(this.hitInterval);
        writer.writeInt(this.attackPower);
        writer.writeInt(this.attackPowerMin);
        writer.writeInt(this.attackPowerChargeRate);
        writer.writeInt(this.armorAttackPower);
        writer.writeInt(this.downAttackPercent);
        writer.writeInt(this.flatInt13);
        writer.writeInt(this.flatInt14);
        writer.writeInt(this.flatInt15);
        writer.writeInt(this.flatInt16);
        writer.writeInt(this.flatInt17);
        writer.writeInt(this.flatInt18);
        writer.writeInt(this.flatInt19);
        writer.writeInt(this.makMarkType);
        writer.writeInt(this.makMarkDamage);
        writer.writeInt(this.selfStopTime);
        writer.writeByte(this.vanishTime);
        writer.writeInt(this.hitEffect);
        writer.writeInt(this.hitEffectOpponent);
        writer.writeInt(this.nokezoriGround);
        writer.writeInt(this.nokezoriAir);
        writer.writeInt(this.nokezoriAirToGround);
        writer.writeInt(this.nokezoriDown);
        writer.writeInt(this.nokezoriPriority);
        writer.writeInt(this.hitStop);
        writer.writeInt(this.hitStopSelf);
        writer.writeInt(this.slowRate);
        writer.writeInt(this.screenZoomTime);
        writer.writeInt(this.screenZoom);
        // Write wrapper entries
        for (int i = 0; i < 47; i++) {
            CEventHitUnit target = null;
            for (CEventHitUnit unit : this.unitList) {
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
