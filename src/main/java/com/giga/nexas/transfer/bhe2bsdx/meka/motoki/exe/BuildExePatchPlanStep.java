package com.giga.nexas.transfer.bhe2bsdx.meka.motoki.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

import java.nio.file.Path;
import java.util.Map;

public class BuildExePatchPlanStep {
    private static final int BASELINE_VISIBLE_SELECT_MEKA_MENU_ROWS = 70;
    private static final int MAX_AUDITED_SELECT_MEKA_MENU_ROWS = 76;

    public TsukuyomiExePatchPlan build(
            Path sourceExe,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            TsukuyomiGraftResult result
    ) {
        TsukuyomiExePatchPlan plan = new TsukuyomiExePatchPlan();
        plan.setSourceExePath(sourceExe);
        populateRequiredCapacities(plan, bsdxBaseline, grpAppendPlan);
        populateRequiredMenuCapacities(plan, bsdxBaseline, result);
        plan.getNotes().add("EXE meka capacity target = " + plan.getRequiredMekaCapacity()
                + "，容量来自追加后的 MekaGroup.grp 实际条目数。");
        addMekaCapacityPatchRationaleNotes(plan);
        return plan;
    }

    private void populateRequiredCapacities(
            TsukuyomiExePatchPlan plan,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan
    ) {
        plan.setRequiredMekaCapacity(resolveGroupSize(
                bsdxBaseline == null || bsdxBaseline.getMekaGroupGrp() == null ? null : bsdxBaseline.getMekaGroupGrp().getMekaList(),
                grpAppendPlan == null ? null : grpAppendPlan.getMekaGroupIndex()
        ));
        plan.setRequiredWazaCapacity(resolveGroupSize(
                bsdxBaseline == null || bsdxBaseline.getWazaGroupGrp() == null ? null : bsdxBaseline.getWazaGroupGrp().getWazaList(),
                grpAppendPlan == null || grpAppendPlan.getSourceWazGroupIndexToTargetIndex().isEmpty()
                        ? null
                        : maxValue(grpAppendPlan.getSourceWazGroupIndexToTargetIndex())
        ));
        plan.setRequiredSpriteCapacity(resolveGroupSize(
                bsdxBaseline == null || bsdxBaseline.getSpriteGroupGrp() == null ? null : bsdxBaseline.getSpriteGroupGrp().getSpriteList(),
                grpAppendPlan == null || grpAppendPlan.getSourceSpriteGroupIndexToTargetIndex().isEmpty()
                        ? null
                        : maxValue(grpAppendPlan.getSourceSpriteGroupIndexToTargetIndex())
        ));
        plan.setRequiredBatVoiceCapacity(resolveGroupSize(
                bsdxBaseline == null || bsdxBaseline.getBatVoiceGrp() == null ? null : bsdxBaseline.getBatVoiceGrp().getVoiceList(),
                grpAppendPlan == null ? null : grpAppendPlan.getBatVoiceGroupIndex()
        ));
        plan.setRequiredSeCapacity(resolveGroupSize(
                bsdxBaseline == null || bsdxBaseline.getSeGroupGrp() == null ? null : bsdxBaseline.getSeGroupGrp().getSeList(),
                grpAppendPlan == null || grpAppendPlan.getSourceSeGroupIndexToTargetIndex().isEmpty()
                        ? null
                        : maxValue(grpAppendPlan.getSourceSeGroupIndexToTargetIndex())
        ));
        int baselineWeaponEquipRows = bsdxBaseline == null
                || bsdxBaseline.getWeaponEquipDat() == null
                || bsdxBaseline.getWeaponEquipDat().getData() == null
                ? -1
                : bsdxBaseline.getWeaponEquipDat().getData().size();
        if (baselineWeaponEquipRows >= 0) {
            plan.setRequiredWeaponEquipRows(baselineWeaponEquipRows + 1);
        }
    }

    private void populateRequiredMenuCapacities(
            TsukuyomiExePatchPlan plan,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGraftResult result
    ) {
        int baselineRows = bsdxBaseline == null || bsdxBaseline.getSelectMekaMenuDat() == null || bsdxBaseline.getSelectMekaMenuDat().getData() == null
                ? -1
                : bsdxBaseline.getSelectMekaMenuDat().getData().size();
        int patchedRows = result == null || result.getPatchedSelectMekaMenuDat() == null || result.getPatchedSelectMekaMenuDat().getData() == null
                ? -1
                : result.getPatchedSelectMekaMenuDat().getData().size();

        int appendedRows = baselineRows >= 0 && patchedRows >= baselineRows ? patchedRows - baselineRows : 0;
        plan.setRequiredSelectMekaMenuRows(BASELINE_VISIBLE_SELECT_MEKA_MENU_ROWS + appendedRows);

        if (plan.getRequiredSelectMekaMenuRows() > MAX_AUDITED_SELECT_MEKA_MENU_ROWS) {
            throw new IllegalStateException(
                    "当前只审计到 SelectMekaMenu 可见 " + MAX_AUDITED_SELECT_MEKA_MENU_ROWS
                            + " 行，实际需求 " + plan.getRequiredSelectMekaMenuRows()
                            + "，需要先继续做 switch/object-id 审计"
            );
        }
    }

    private int resolveGroupSize(java.util.List<?> list, Integer maxIndex) {
        int bySize = list == null ? -1 : list.size();
        int byIndex = maxIndex == null ? -1 : maxIndex + 1;
        return Math.max(bySize, byIndex);
    }

    private int maxValue(Map<Integer, Integer> map) {
        int max = -1;
        for (Integer value : map.values()) {
            if (value != null && value > max) {
                max = value;
            }
        }
        return max;
    }

    private void addMekaCapacityPatchRationaleNotes(TsukuyomiExePatchPlan plan) {
        plan.getNotes().add("site 1 @0x1E3F40: 初始化分配器容量，避免新增机体槽没有 runtime 结构。");
        plan.getNotes().add("site 2 @0x1E3DA2: scene cleanup 循环边界，避免新增机体引用计数不被清理。");
        plan.getNotes().add("site 3 @0x276427: 存档读取第一段循环，避免新增机体基础状态不被读取。");
        plan.getNotes().add("site 4 @0x2762EB: 存档读取第二段循环，避免新增机体详细状态不被读取。");
        plan.getNotes().add("site 5 @0x275336: 存档写入分配器容量，避免新增机体没有写出空间。");
        plan.getNotes().add("site 6 @0x063A85: 资源尺寸统计循环边界，避免新增机体被资源统计跳过。");
        plan.getNotes().add("site 7 @0x2749ED: 存档读取第三段循环，避免并行读取路径仍停在旧上界。");
        plan.getNotes().add("site 8 @0x056CE4: CMekaGroup runtime 表预分配容量，避免新增机体 runtime 记录为空。");
        plan.getNotes().add("site 8b @0x056F45: CMekaGroup runtime 表初始化字节边界，避免新增槽位未初始化。");
        plan.getNotes().add("site 9 @0x056F9B: 初始化备用路径预分配容量。");
        plan.getNotes().add("site 10 @0x275158: 存档写入备用路径容量。");
        plan.getNotes().add("site 11 @0x30390A: 独立预分配路径容量。");
        plan.getNotes().add("site 12 @0x05498B: WeaponEquip 填充硬上限，目标行数 = " + plan.getRequiredWeaponEquipRows()
                + "，该值独立于 MekaGroup 容量。");
        plan.getNotes().add("site 13 @0x20C1FD: sub_60CDF0 战斗语音 gate 绕过，允许追加机体的 AT/FC 语音请求进入队列。");
        plan.getNotes().add("site 14 @0x20C2CD: sub_60CEC0 表驱动战斗语音 gate 绕过，允许追加机体的表驱动语音请求进入队列。");
    }
}
