package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

import java.nio.file.Path;
import java.util.Map;

public class BuildExePatchPlanStep {

    // TODO 客制化入口：这两个值是 EXE 菜单容量审计边界，不是 DAT 行数的通用真理。
    // 如果以后手动扩展更多可见 SelectMekaMenu 行，先继续逆向 switch/object-id/绘制表，
    // 再同步调整这里和 ExePatchProfile；不要只改 DAT 行数，否则菜单可能能打包但运行时读不到。
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
        plan.getNotes().add("step10: patch exe meka capacity 103 -> 104 (10 active sites, 1 excluded site).");
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
                    "当前只审到 SelectMekaMenu 可见 " + MAX_AUDITED_SELECT_MEKA_MENU_ROWS
                            + " 项，实际需求 " + plan.getRequiredSelectMekaMenuRows()
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
        plan.getNotes().add("site 1 @0x1E3F40: init allocator, otherwise meka slot 103 is never preallocated.");
        plan.getNotes().add("site 2 @0x1E3DA2: scene cleanup loop bound, otherwise slot 103 reference counts are never cleared.");
        plan.getNotes().add("site 3 @0x276427: save-read loop #1, otherwise slot 103 base/type state is never loaded.");
        plan.getNotes().add("site 4 @0x2762EB: save-read loop #2, otherwise slot 103 detailed state is never loaded.");
        plan.getNotes().add("site 5 @0x275336: save-write allocator, otherwise slot 103 is missing from save output buffers.");
        plan.getNotes().add("site 6 @0x063A85: resource-size loop bound, otherwise slot 103 is skipped by aggregate resource accounting.");
        plan.getNotes().add("site 7 @0x2749ED: save-read loop #3, otherwise a third parallel save-read pass still stops at 102.");
        plan.getNotes().add("site 8 @0x056CE4: CMekaGroup runtime table prealloc size, otherwise dword_875F54 is allocated for only 103 meka records.");
        plan.getNotes().add("site 8b @0x056F45: CMekaGroup runtime table init loop bound, otherwise only 103 * 4336 bytes are initialized and slot 103 never becomes a valid runtime record.");
        plan.getNotes().add("site 9 @0x056F9B: init prealloc alt path, otherwise an alternate init path still allocates only 103 entries.");
        plan.getNotes().add("site 10 @0x275158: save-write alt path, otherwise an alternate write path still stops at 103.");
        plan.getNotes().add("site 11 @0x30390A: standalone prealloc path, otherwise another meka-side allocator still uses 103.");
        plan.getNotes().add("site 12 @0x05498B: WeaponEquip fill hard cap, otherwise sub_454E60 still stops at 56 * 103 and slot 103 keeps garbage equip-menu label bytes.");
        plan.getNotes().add("site 13 @0x20C1FD: sub_60CDF0 gate bypass, otherwise AT/FC battle-voice requests for appended mekaId 103 are rejected by sub_60CC20 before they enter the request queue.");
        plan.getNotes().add("site 14 @0x20C2CD: sub_60CEC0 gate bypass, otherwise the sibling table-driven combat voice request path is still rejected by the same sub_60CC20 gate.");
    }
}
