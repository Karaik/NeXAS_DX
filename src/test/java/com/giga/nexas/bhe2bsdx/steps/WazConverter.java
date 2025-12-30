package com.giga.nexas.bhe2bsdx.steps;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.*;
import com.giga.nexas.exception.OperationException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * waz 转换：
 * 按槽位映射重建 SkillUnit，保留事件结构。
 * BHE 共有 83 槽位，BSDX 只有 72；映射表会丢弃 BHE 独有槽位。
 */
public class WazConverter {

    public Waz convert(com.giga.nexas.dto.bhe.waz.Waz bheWaz) {
        Waz bsdxWaz = new Waz();
        if (bheWaz == null) {
            return bsdxWaz;
        }
        processWazaSkillUnitCollection(bsdxWaz, bheWaz);
        return bsdxWaz;
    }

    // unitQuantity 与实际情况不符，需额外处理
    private void processWazaSkillUnitCollection(
            com.giga.nexas.dto.bsdx.waz.Waz bsdxWaz,
            com.giga.nexas.dto.bhe.waz.Waz bheWaz
    ) {
        bsdxWaz.setFileName(bheWaz.getFileName());
        bsdxWaz.setExtensionName(bheWaz.getExtensionName());

        // 类对应表（83 -> 72）
        Map<Integer, Integer> bheToBsdxSlotMap = bheToBsdxSlotMap();
        // 目标技能列表
        List<com.giga.nexas.dto.bsdx.waz.Waz.Skill> dstSkills = new ArrayList<>();

        // for each Skill
        for (com.giga.nexas.dto.bhe.waz.Waz.Skill srcSkill : bheWaz.getSkillList()) {
            com.giga.nexas.dto.bsdx.waz.Waz.Skill dstSkill = new com.giga.nexas.dto.bsdx.waz.Waz.Skill();

            dstSkill.setSkillNameJapanese(srcSkill.getSkillNameJapanese());
            dstSkill.setSkillNameEnglish(srcSkill.getSkillNameEnglish());

            // suffix 结构一一拷贝
            if (srcSkill.getSkillSuffixList() != null) {
                List<com.giga.nexas.dto.bsdx.waz.Waz.Skill.SkillSuffix> dstSuffix = new ArrayList<>();
                for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillSuffix s : srcSkill.getSkillSuffixList()) {
                    com.giga.nexas.dto.bsdx.waz.Waz.Skill.SkillSuffix t =
                            new com.giga.nexas.dto.bsdx.waz.Waz.Skill.SkillSuffix();
                    BeanUtil.copyProperties(s, t);
                    dstSuffix.add(t);
                }
                dstSkill.setSkillSuffixList(dstSuffix);
            }

            // phasesInfo 重建
            List<Waz.Skill.SkillPhase> dstPhases = new ArrayList<>();
            for (com.giga.nexas.dto.bhe.waz.Waz.Skill.SkillPhase srcSkillPhase : srcSkill.getPhasesInfo()) {

                Waz.Skill.SkillPhase dstPhase = new Waz.Skill.SkillPhase();
                for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit srcSkillUnit
                        : srcSkillPhase.getSkillUnitCollection()) {
                    // 读取 bhe 槽位
                    Integer bheSlot = srcSkillUnit.getUnitQuantity();

                    // 槽位映射 bhe -> bsdx
                    Integer bsdxSlot = bheToBsdxSlotMap.get(bheSlot);

                    // 无映射或越界则丢弃该单元
                    if (bsdxSlot == null || bsdxSlot < 0 || bsdxSlot >= 72) {
                        continue;
                    }

                    // 新建 bsdx SkillUnit
                    SkillUnit dstUnit = new SkillUnit();
                    dstUnit.setUnitQuantity(bsdxSlot);

                    // 目标事件列表
                    List<SkillInfoObject> dstInfos = new ArrayList<>();

                    // 源事件列表（bhe）
                    List<com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject> srcInfoList =
                            srcSkillUnit.getSkillInfoObjectList();

                    // 为空跳过
                    if (srcInfoList == null || srcInfoList.isEmpty()) {
                        continue;
                    }

                    // 将每个 bhe 事件创建为对应 bsdx 子类
                    for (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject srcInfo : srcInfoList) {

                        SkillInfoObject dstInfo;
                        // 37: 汎用変数  BHE:CEventFreeParam -> BSDX:CEventVal
                        if (bheSlot == 37 &&
                                srcInfo instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventFreeParam srcBhe) {
                            dstInfo = SkillInfoFactory.createEventObjectBsdx(35);
                            if (dstInfo instanceof CEventVal ev) {
                                for (var unit : srcBhe.getUnitList()) {
                                    if (unit.getBuffer() != 0) {
                                        continue;
                                    }
                                    if (unit.getData() instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventVal v) {
                                        ev.setInt1(v.getInt1());
                                        ev.setInt2(v.getInt2());
                                        ev.setInt3(v.getInt3());
                                        ev.setInt4(v.getInt4());
                                    }
                                }
                                dstInfos.add(ev);
                            } else {
                                throw new OperationException(500, "汎用変数error");
                            }
                            continue;
                        }

                        dstInfo = SkillInfoFactory.createEventObjectBsdx(bsdxSlot);

                        if (dstInfo instanceof CEventSpriteAttr ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventCpuButton ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventCpuButtonToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventVoice ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventRadialLine ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventRadialLineToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventValRandom ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventMove ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventSe ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventTouch ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventEffect ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventEffectToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventScreenLine ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventScreenLineToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventSlipHosei ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventVal ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventBlur ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventCharge ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventChargeToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventScreenAttr ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventTerm ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventEscape ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventEscapeToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventScreenEffect ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventHit ev) {
                            // CEventHit 需要显式拷贝基础帧信息，再做字段映射
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventHitToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventStatus ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventStatusToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventChange ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventSprite ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventHeight ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventHeightToBsdx(srcInfo, ev);
                        } else if (dstInfo instanceof CEventCamera ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventScreenYure ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventSpriteYure ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                        } else if (dstInfo instanceof CEventBlink ev) {
                            BeanUtil.copyProperties(srcInfo, ev);
                            ev.transBheCEventBlinkToBsdx(srcInfo, ev);
                        } else {
                            throw new OperationException(500, "error");
                        }

                        // 槽位号同步
                        dstInfo.setSlotNum(bsdxSlot);
                        normalizeFrames(dstInfo, srcInfo);
                        dstInfos.add(dstInfo);
                    }

                    // 写回该单元的事件集合
                    dstUnit.setSkillInfoObjectList(dstInfos);
                    dstPhase.getSkillUnitCollection().add(dstUnit);
                }
                dstPhases.add(dstPhase);
            }

            dstSkill.setPhaseQuantity(dstPhases.size());
            dstSkill.setPhasesInfo(dstPhases);
            dstSkills.add(dstSkill);
        }

        bsdxWaz.setSkillList(dstSkills);
    }

    // bhe -> bsdx
    private Map<Integer, Integer> bheToBsdxSlotMap() {
        Map<Integer, Integer> m = new HashMap<>();

        // -1 表示 BHE 独有槽位，BSDX 无对应，直接丢弃
        m.put(0, 0);
        m.put(1, 1);
        m.put(2, 2);
        m.put(3, 3);
        m.put(4, 4);
        m.put(5, 5);
        m.put(6, 6);
        m.put(7, 7);
        m.put(8, 8);
        m.put(9, 9);
        m.put(10, 10);
        m.put(11, 11);
        m.put(12, 12);
        m.put(13, 13);
        m.put(14, 14);
        m.put(15, 15);
        m.put(16, 16);
        m.put(17, 17);
        m.put(18, 18);
        m.put(19, 19);
        m.put(20, 20);
        m.put(21, 21);
        m.put(22, 22);

        // 23 速度XYZ
        m.put(23, -1);

        m.put(24, 23);
        m.put(25, 24);
        m.put(26, 25);
        m.put(27, 26);
        m.put(28, 27);
        m.put(29, 28);
        m.put(30, 29);
        m.put(31, 30);
        m.put(32, 31);
        m.put(33, 32);
        m.put(34, 33);

        // 35 標的
        m.put(35, -1);

        m.put(36, 34);
        m.put(37, 35);

        // 38 技ツールパラメータ
        m.put(38, -1);
        m.put(39, 36);

        // 40 ハイパーアーマー
        m.put(40, -1);
        m.put(41, 37);

        // 42 無敵
        m.put(42, 38);

        // 43 死亡(自爆)
        m.put(43, -1);

        m.put(44, 39);
        m.put(45, 40);
        m.put(46, 41);
        m.put(47, 42);
        m.put(48, 43);
        m.put(49, 44);
        m.put(50, 45);
        m.put(51, 46);

        // 52 CPU 特殊行動
        m.put(52, -1);

        m.put(53, 47);
        m.put(54, 48);
        m.put(55, 49);
        m.put(56, 50);
        m.put(57, 51);
        m.put(58, 52);
        m.put(59, 53);
        m.put(60, 54);
        m.put(61, 55);

        // 62 属性
        m.put(62, -1);

        m.put(63, 56);
        m.put(64, 57);
        m.put(65, 58);

        // 66-69 マルチロック
        m.put(66, -1);
        m.put(67, -1);
        m.put(68, -1);
        m.put(69, -1);

        m.put(70, 59);
        m.put(71, 60);
        m.put(72, 61);
        m.put(73, 62);
        m.put(74, 63);
        m.put(75, 64);
        m.put(76, 65);
        m.put(77, 66);
        m.put(78, 67);
        m.put(79, 68);
        m.put(80, 69);
        m.put(81, 70);
        m.put(82, 71);

        return m;
    }

    /**
     * 生成前兜底：避免 startFrame/endFrame 为空导致写盘 NPE。
     */
    private void normalizeFrames(
            SkillInfoObject dstInfo,
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject srcInfo
    ) {
        if (dstInfo.getStartFrame() == null) {
            dstInfo.setStartFrame(srcInfo != null && srcInfo.getStartFrame() != null ? srcInfo.getStartFrame() : 0);
        }
        if (dstInfo.getEndFrame() == null) {
            dstInfo.setEndFrame(srcInfo != null && srcInfo.getEndFrame() != null ? srcInfo.getEndFrame() : 0);
        }
    }
}
