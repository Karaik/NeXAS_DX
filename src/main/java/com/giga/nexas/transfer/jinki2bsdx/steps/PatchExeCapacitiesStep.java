package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 负责汇总本轮迁移后的 exe 容量需求，并视情况写入已确认的固定偏移 patch。
 *
 * <p>当前验证策略复用旧 mekaIndex=32，不走 103 -> 104 扩容路线，
 * 所以 {@link AkaoGraftRequest#isPlanExeCapacityPatch()} 默认为 true 进入本 step，
 * 但机体容量的数学条件不满足时不会触发实际 patch 写入。</p>
 *
 * <p>目前已实现写入的 patch 位点仅限 SelectMekaMenu 行数上界（IMM32 0x14F21F）；
 * waza/sprite/batvoice/se 的容量需求仅记录在 {@link ExePatchPlan} 中，尚未写固定偏移。</p>
 */
public class PatchExeCapacitiesStep {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final int BASELINE_VISIBLE_SELECT_MEKA_MENU_ROWS = 70;
    private static final int MAX_AUDITED_SELECT_MEKA_MENU_ROWS = 76;
    private static final int SELECT_MEKA_MENU_ROW_LIMIT_IMM32_OFFSET = 0x14F21F;

    public ExePatchPlan patchExeCapacities(
            AkaoGraftRequest request,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan,
            AkaoGraftResult result
    ) {
        ExePatchPlan plan = new ExePatchPlan();
        if (request == null || !request.isPlanExeCapacityPatch()) {
            return plan;
        }

        populateRequiredCapacities(plan, bsdxBaseline, grpAppendPlan);
        populateRequiredMenuCapacities(plan, bsdxBaseline, result);

        plan.setSourceExePath(request.getTargetExePath());
        plan.getNotes().add("step10 先汇总本次迁移后的目标容量。");
        plan.getNotes().add("当前验证策略复用旧 mekaIndex=32，机体容量数学条件不满足，不会触发实际 patch 写入。");
        plan.getNotes().add("waza/sprite/batvoice/se 当前仅记录容量需求，尚未写固定偏移 patch。");
        plan.getNotes().add("SelectMekaMenu 当前仍保守审到 76 个可见槽；超过 76 需要继续审 switch/object-id。");

        Path sourceExe = request.getTargetExePath();
        if (sourceExe == null || !Files.exists(sourceExe)) {
            throw new IllegalStateException("目标 exe 不存在: " + sourceExe);
        }

        try {
            byte[] exeBytes = Files.readAllBytes(sourceExe);

            if (plan.getRequiredSelectMekaMenuRows() > 0) {
                int targetMaxOffset = Math.max(0, (plan.getRequiredSelectMekaMenuRows() - 1) * 12);
                if (targetMaxOffset != 0x33C) {
                    applyImm32Patch(
                            exeBytes,
                            SELECT_MEKA_MENU_ROW_LIMIT_IMM32_OFFSET,
                            0x33C,
                            targetMaxOffset,
                            "SelectMekaMenu row upper-bound",
                            plan
                    );
                } else {
                    plan.getNotes().add("SelectMekaMenu 当前仍保持 70 个可见槽，本轮不需要改 0x14F21F。");
                }
            }

            Path outputDir = request.getExeOutputDir();
            Files.createDirectories(outputDir);
            Path outputExe = outputDir.resolve(buildTimestampedExeName(sourceExe));
            Files.write(outputExe, exeBytes);

            plan.setPatched(!plan.getTargetOffsets().isEmpty());
            plan.setOutputExePath(outputExe);
            plan.getNotes().add("patched exe 已写出到 resources/out。");
            return plan;
        } catch (IOException e) {
            throw new IllegalStateException("写出 patched exe 失败", e);
        }
    }

    private void populateRequiredCapacities(
            ExePatchPlan plan,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        plan.setRequiredMekaCapacity(resolveGroupSize(
                bsdxBaseline == null ? null : bsdxBaseline.getMekaGroupGrp() == null ? null : bsdxBaseline.getMekaGroupGrp().getMekaList(),
                grpAppendPlan == null ? null : grpAppendPlan.getMekaGroupIndex()
        ));
        plan.setRequiredWazaCapacity(resolveGroupSize(
                bsdxBaseline == null ? null : bsdxBaseline.getWazaGroupGrp() == null ? null : bsdxBaseline.getWazaGroupGrp().getWazaList(),
                grpAppendPlan == null || grpAppendPlan.getSourceWazGroupIndexToTargetIndex().isEmpty()
                        ? null
                        : maxValue(grpAppendPlan.getSourceWazGroupIndexToTargetIndex())
        ));
        plan.setRequiredSpriteCapacity(resolveGroupSize(
                bsdxBaseline == null ? null : bsdxBaseline.getSpriteGroupGrp() == null ? null : bsdxBaseline.getSpriteGroupGrp().getSpriteList(),
                grpAppendPlan == null || grpAppendPlan.getSourceSpriteGroupIndexToTargetIndex().isEmpty()
                        ? null
                        : maxValue(grpAppendPlan.getSourceSpriteGroupIndexToTargetIndex())
        ));
        plan.setRequiredBatVoiceCapacity(resolveGroupSize(
                bsdxBaseline == null ? null : bsdxBaseline.getBatVoiceGrp() == null ? null : bsdxBaseline.getBatVoiceGrp().getVoiceList(),
                grpAppendPlan == null ? null : grpAppendPlan.getBatVoiceGroupIndex()
        ));
        plan.setRequiredSeCapacity(resolveGroupSize(
                bsdxBaseline == null ? null : bsdxBaseline.getSeGroupGrp() == null ? null : bsdxBaseline.getSeGroupGrp().getSeList(),
                grpAppendPlan == null || grpAppendPlan.getSourceSeGroupIndexToTargetIndex().isEmpty()
                        ? null
                        : maxValue(grpAppendPlan.getSourceSeGroupIndexToTargetIndex())
        ));
    }

    private void populateRequiredMenuCapacities(
            ExePatchPlan plan,
            BsdxBaselineBundle bsdxBaseline,
            AkaoGraftResult result
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

    private int maxValue(java.util.Map<Integer, Integer> map) {
        int max = -1;
        for (Integer value : map.values()) {
            if (value != null && value > max) {
                max = value;
            }
        }
        return max;
    }

    private void applyImm32Patch(
            byte[] exeBytes,
            int offset,
            int expectedImm32,
            int targetImm32,
            String label,
            ExePatchPlan plan
    ) {
        if (offset < 0 || offset + 3 >= exeBytes.length) {
            throw new IllegalStateException(String.format("patch 偏移越界: 0x%06X", offset));
        }

        int current = readLittleEndianInt(exeBytes, offset);
        if (current != expectedImm32 && current != targetImm32) {
            throw new IllegalStateException(String.format(
                    "patch 偏移原值不符合预期: 0x%06X current=0x%08X expected=0x%08X target=0x%08X",
                    offset,
                    current,
                    expectedImm32,
                    targetImm32
            ));
        }

        writeLittleEndianInt(exeBytes, offset, targetImm32);
        plan.getTargetOffsets().add(String.format(
                "0x%06X: %s 0x%08X -> 0x%08X",
                offset,
                label,
                current,
                targetImm32
        ));
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private void writeLittleEndianInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }

    private String buildTimestampedExeName(Path sourceExe) {
        String fileName = sourceExe.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot >= 0 ? fileName.substring(dot) : ".exe";
        return baseName + "_" + LocalDateTime.now().format(TS) + ext;
    }
}
