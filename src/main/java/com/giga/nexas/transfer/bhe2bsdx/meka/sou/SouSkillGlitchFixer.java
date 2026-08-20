package com.giga.nexas.transfer.bhe2bsdx.meka.sou;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;

/**
 * Sou (sou / Schwertiger) 技能重映射与数值修复器
 *
 * <p>1. 将 sou.mek 中 140 组常规武器从 Lv1 初始索引批量重映射为 Lv3 (完全体) 索引 (wazSeq = baseLv1 + 2)；</p>
 * <p>2. 同步将满级武器的热量最大消耗 (heatMaxConsumption) 规整为最小消耗 (heatMinConsumption)；</p>
 * <p>3. 若存在 FC 槽位 (weaponCategory == 1)，配置为 sou.waz 内部专属的 EX 技能 (606~641 区间)。</p>
 */
@Slf4j
public final class SouSkillGlitchFixer {

    public static final SouSkillGlitchFixer INSTANCE = new SouSkillGlitchFixer();

    private SouSkillGlitchFixer() {
    }

    public void fix(FollowupMekaContext context) {
        if (context == null) {
            return;
        }

        // 1. 修复重绑后的目标 MEK (reboundMek)
        if (context.getReboundMek() != null) {
            upgradeMekToMaxLevel(context.getReboundMek());
        }

        // 2. 修复 selectedPackage 中的源机体 MEK（保持内存视图一致）
        if (context.getSelectedPackage() != null && context.getSelectedPackage().getTsukuyomiMek() != null) {
            upgradeMekToMaxLevel(context.getSelectedPackage().getTsukuyomiMek());
        }
    }

    private void upgradeMekToMaxLevel(Mek mek) {
        Map<Integer, Mek.MekWeaponInfo> weaponMap = mek.getMekWeaponInfoMap();
        if (weaponMap == null || weaponMap.isEmpty()) {
            return;
        }

        for (Map.Entry<Integer, Mek.MekWeaponInfo> entry : weaponMap.entrySet()) {
            Mek.MekWeaponInfo weapon = entry.getValue();
            if (weapon == null || weapon.getWazSequence() == null) {
                continue;
            }

            int wazSeq = weapon.getWazSequence();

            // 规则 A：常规武器 140 组（4 个 35 门武器的大组，每门武器连续 3 个等级）
            // Group 1 (0~34): 39..143 (39 -> 41, 42 -> 44, ..., 141 -> 143)
            // Group 2 (35~69): 145..249 (145 -> 147, 148 -> 150, ..., 247 -> 249)
            // Group 3 (70~104): 251..355 (251 -> 253, 254 -> 256, ..., 353 -> 355)
            // Group 4 (105~139): 357..461 (357 -> 359, 360 -> 362, ..., 459 -> 461)
            int baseLv1 = -1;
            if (wazSeq >= 39 && wazSeq <= 143) {
                baseLv1 = 39 + ((wazSeq - 39) / 3) * 3;
            } else if (wazSeq >= 145 && wazSeq <= 249) {
                baseLv1 = 145 + ((wazSeq - 145) / 3) * 3;
            } else if (wazSeq >= 251 && wazSeq <= 355) {
                baseLv1 = 251 + ((wazSeq - 251) / 3) * 3;
            } else if (wazSeq >= 357 && wazSeq <= 461) {
                baseLv1 = 357 + ((wazSeq - 357) / 3) * 3;
            }

            if (baseLv1 != -1) {
                weapon.setWazSequence(baseLv1 + 2); // 升级为 Lv3 完全体
                if (weapon.getHeatMinConsumption() != null) {
                    weapon.setHeatMaxConsumption(weapon.getHeatMinConsumption());
                }
                weapon.setUpgradeExp(0);
            }

            // 规则 B：如果存在 FC 槽位（weaponCategory == 1），配置为 EX 技能
            // 如 615: 武幻翔嵐舞 (EX_SWORD) 或 611: 破軍煌帝脚 (EX_KAISERKICK)
            if (weapon.getWeaponCategory() != null && weapon.getWeaponCategory() == 1) {
                if (weapon.getWazSequence() < 606 || weapon.getWazSequence() > 641) {
                    weapon.setWazSequence(615); // 默认挂配 EX 技能：武幻翔嵐舞
                }
            }
        }
    }
}
