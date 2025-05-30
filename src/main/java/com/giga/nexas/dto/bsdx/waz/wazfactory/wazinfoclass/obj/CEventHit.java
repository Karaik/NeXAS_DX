package com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory.createCEventObjectByTypeBsdx;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventHit__Read
 */
@Data
@NoArgsConstructor
public class CEventHit extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventHitType {
        private Integer type;
        private String description;
    }

    public static final CEventHitType[] CEVENT_HIT_TYPES = {
            new CEventHitType(0xFFFFFFFF, "攻撃グループ"),
            new CEventHitType(0xFFFFFFFF, "攻撃グループ2"),
            new CEventHitType(0xFFFFFFFF, "ヒットカウンタ初期化"),
            new CEventHitType(0xFFFFFFFF, "フラグ"),
            new CEventHitType(0xFFFFFFFF, "ヒット数"),
            new CEventHitType(0xFFFFFFFF, "ヒット間隔"),
            new CEventHitType(0x20, "攻撃力"),
            new CEventHitType(0xFFFFFFFF, "攻撃力最低値"),
            new CEventHitType(0xFFFFFFFF, "攻撃力溜め反映率"),
            new CEventHitType(0xFFFFFFFF, "装甲攻撃力"),
            new CEventHitType(0xFFFFFFFF, "基底コンボ補正値"),
            new CEventHitType(0xFFFFFFFF, "攻撃力補正：技のみ"),
            new CEventHitType(0xFFFFFFFF, "攻撃力補正"),
            new CEventHitType(0xFFFFFFFF, "攻撃力補正：技終了時"),
            new CEventHitType(0xFFFFFFFF, "自分停止時間"),
            new CEventHitType(0xFFFFFFFF, "消滅時間"),
            new CEventHitType(0x4, "ヒットエフェクト"),
            new CEventHitType(0x21, "のけぞり（地上→地上）"),
            new CEventHitType(0x21, "のけぞり（空中）"),
            new CEventHitType(0x21, "のけぞり（空中→地上）"),
            new CEventHitType(0x21, "のけぞり（ダウン）"),
            new CEventHitType(0xFFFFFFFF, "ヒットストップ"),
            new CEventHitType(0xFFFFFFFF, "ヒットストップ（自機）"),
            new CEventHitType(0xFFFFFFFF, "スロー反映率"),
            new CEventHitType(0xFFFFFFFF, "画面：拡大縮小時間"),
            new CEventHitType(0x2, "画面：拡大縮小"),
            new CEventHitType(0xFFFFFFFF, "画面：振動時間"),
            new CEventHitType(0x19, "画面：振動"),
            new CEventHitType(0x17, "ヒットＳＥ"),
            new CEventHitType(0xFFFFFFFF, "鉄ヒットＳＥ"),
            new CEventHitType(0xFFFFFFFF, "ダメージ色時間"),
            new CEventHitType(0xFFFFFFFF, "キャンセルフラグ"),
            new CEventHitType(0x23, "メカステータス増減値")
    };

    /**
     * 01为正常对敌造成伤害，01更改为06时会只对自机造成伤害，更改为03时会对敌机和友军造成伤害
     */
    private Short attackTargetType;

    /**
     *
     */
    private Short short2;

    /**
     *
     */
    private Integer int1;

    /**
     *
     */
    private Integer int2;

    /**
     *
     */
    private Integer int3;

    /**
     * 当前行为的hit数
     *
     * ヒット数
     */
    private Integer hitCount;

    /**
     * 每hit的间隔
     *
     * ヒット間隔
     */
    private Integer hitInterval;

    /**
     * 内补正，简单来说就是多段伤害的技能在每次造成伤害后
     * 会在技能未结束时反复算上的补正，技能结束后会还原
     *
     * 攻撃力補正：技のみ
     */
    private Integer internalCorrection;

    /**
     * 连段中的补正，中途补正，加上终了补正为最终补正
     *
     * 攻撃力補正
     */
    private Integer midComboCorrection;

    /**
     * 起手补正一般是起手+终了
     *
     * 攻撃力補正：技終了時
     */
    private Integer endCorrection;

    /**
     * 底伤，保底伤害
     *
     * 攻撃力最低値
     */
    private Integer minDamage;

    /**
     * 起手补正，一般是本补正+终了补正
     *
     * 基底コンボ補正値
     */
    private Integer startComboCorrection;

    /**
     *
     */
    private Integer int11;

    /**
     * 伤害倍率，蓄力时间对最终伤害的影响程度，
     * 村正的威为-1，空的蜂刺、jk的矛为0，此数值可以不影响蓄力时间
     * 蓄力时间“不会影响”并不代表不能蓄，而是不会提升伤害
     *
     * 攻撃力溜め反映率
     */
    private Integer chargeDamageRate;

    /**
     *
     */
    private Integer int13;

    /**
     *
     */
    private Integer int14;

    /**
     *
     */
    private Integer int15;

    /**
     *
     */
    private Integer int16;

    /**
     *
     */
    private Integer int17;

    /**
     *
     */
    private Integer int18;

    /**
     * 单位为帧
     *
     * 画面：振動時間
     */
    private Integer screenShakeFrame;

    /**
     *
     */
    private Integer int20;

    /**
     * 鉄ヒットＳＥ？
     */
    private Integer int21;

    /**
     * ダメージ色時間？
     */
    private Integer int22;

    /**
     * キャンセルフラグ？
     */
    private Integer int23;

    private List<CEventHitUnit> ceventHitUnitList = new ArrayList<>();

    public CEventHit(Integer typeId) {
        super(typeId);
    }

    @Data
    public static class CEventHitUnit {
        private Integer ceventHitUnitQuantity;
        private Integer buffer;
        private Integer unitSlotNum;
        private String description;
        private SkillInfoObject data;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.attackTargetType = reader.readShort();
        this.short2 = reader.readShort();

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.hitCount = reader.readInt();
        this.hitInterval = reader.readInt();
        this.internalCorrection = reader.readInt();
        this.midComboCorrection = reader.readInt();
        this.endCorrection = reader.readInt();
        this.minDamage = reader.readInt();
        this.startComboCorrection = reader.readInt();
        this.int11 = reader.readInt();
        this.chargeDamageRate = reader.readInt();
        this.int13 = reader.readInt();
        this.int14 = reader.readInt();
        this.int15 = reader.readInt();
        this.int16 = reader.readInt();
        this.int17 = reader.readInt();
        this.int18 = reader.readInt();
        this.screenShakeFrame = reader.readInt();
        this.int20 = reader.readInt();
        this.int21 = reader.readInt();
        this.int22 = reader.readInt();
        this.int23 = reader.readInt();

        this.ceventHitUnitList.clear();

        for (int i = 0; i < 33; i++) {
            CEventHitUnit unit = new CEventHitUnit();
            int buffer = reader.readInt();
            unit.setDescription(CEVENT_HIT_TYPES[i].getDescription());
            unit.setCeventHitUnitQuantity(i);
            unit.setBuffer(buffer);
            unit.setUnitSlotNum(i);

            if (buffer != 0) {
                int typeId = CEVENT_HIT_TYPES[i].getType();
                SkillInfoObject obj = createCEventObjectByTypeBsdx(typeId);

                obj.readInfo(reader);
                unit.setData(obj);
                this.ceventHitUnitList.add(unit);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeShort(this.attackTargetType);
        writer.writeShort(this.short2);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.hitCount);
        writer.writeInt(this.hitInterval);
        writer.writeInt(this.internalCorrection);
        writer.writeInt(this.midComboCorrection);
        writer.writeInt(this.endCorrection);
        writer.writeInt(this.minDamage);
        writer.writeInt(this.startComboCorrection);
        writer.writeInt(this.int11);
        writer.writeInt(this.chargeDamageRate);
        writer.writeInt(this.int13);
        writer.writeInt(this.int14);
        writer.writeInt(this.int15);
        writer.writeInt(this.int16);
        writer.writeInt(this.int17);
        writer.writeInt(this.int18);
        writer.writeInt(this.screenShakeFrame);
        writer.writeInt(this.int20);
        writer.writeInt(this.int21);
        writer.writeInt(this.int22);
        writer.writeInt(this.int23);
        for (int i = 0; i < 33; i++) {
            CEventHitUnit target = null;
            for (CEventHitUnit unit : this.ceventHitUnitList) {
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
