package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ExePatchPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 负责汇总本次迁移后的目标容量，并对当前已经确认的 exe 位点执行 patch。
 *
 * <p>当前已确认并真正执行 patch 的只有机体侧 `103 -> N` 这条链。
 * 其余 `waza/sprite/batvoice/se` 目前只汇总需求，不虚构 patch 位点。</p>
 */
public class PatchExeCapacitiesStep {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    /**
     * 机体侧 103 容量在当前固定 exe 里的两个绝对偏移。
     * 原始指令都是 {@code 6A 67}，也就是 {@code push 103}。
     */
    private static final int[] MEKA_CAPACITY_PATCH_OFFSETS = {
            0x56CE3,
            0x56F9A
    };

    public ExePatchPlan patchExeCapacities(
            AkaoGraftRequest request,
            BsdxBaselineBundle bsdxBaseline,
            GrpAppendPlan grpAppendPlan
    ) {
        ExePatchPlan plan = new ExePatchPlan();

        if (request == null || !request.isPlanExeCapacityPatch()) {
            return plan;
        }

        // Step 10-1: 先汇总这次迁移后的目标容量。
        populateRequiredCapacities(plan, bsdxBaseline, grpAppendPlan);

        plan.setSourceExePath(request.getTargetExePath());
        plan.getNotes().add("step10 先汇总本次迁移后的目标容量。");
        plan.getNotes().add("当前只对机体侧 103 容量链执行固定偏移 patch。");
        plan.getNotes().add("waza/sprite/batvoice/se 当前仅记录需求，尚未定位稳定 patch 位点。");

        Path sourceExe = request.getTargetExePath();
        if (sourceExe == null || !Files.exists(sourceExe)) {
            throw new IllegalStateException("目标 exe 不存在: " + sourceExe);
        }

        // Step 10-2: 当前已确认的机体容量 patch 仍然是 imm8 形式。
        if (plan.getRequiredMekaCapacity() < 0 || plan.getRequiredMekaCapacity() > 127) {
            throw new IllegalStateException(
                    "当前 fixed-offset patch 只支持 0..127 的机体容量，实际需求=" + plan.getRequiredMekaCapacity()
            );
        }

        try {
            byte[] exeBytes = Files.readAllBytes(sourceExe);

            // Step 10-3: 按固定绝对偏移覆写机体容量位点。
            for (int offset : MEKA_CAPACITY_PATCH_OFFSETS) {
                applyImm8Patch(exeBytes, offset, 0x67, plan.getRequiredMekaCapacity(), plan);
            }

            // Step 10-4: 最后写出带时间戳的测试 exe。
            Path outputDir = request.getExeOutputDir();
            Files.createDirectories(outputDir);
            Path outputExe = outputDir.resolve(buildTimestampedExeName(sourceExe));
            Files.write(outputExe, exeBytes);

            plan.setPatched(true);
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

    private void applyImm8Patch(
            byte[] exeBytes,
            int offset,
            int expectedImm8,
            int targetImm8,
            ExePatchPlan plan
    ) {
        if (offset < 0 || offset + 1 >= exeBytes.length) {
            throw new IllegalStateException(String.format("patch 偏移越界: 0x%06X", offset));
        }
        if ((exeBytes[offset] & 0xFF) != 0x6A) {
            throw new IllegalStateException(String.format("patch 偏移不是 push imm8: 0x%06X", offset));
        }

        int current = exeBytes[offset + 1] & 0xFF;
        if (current != expectedImm8 && current != targetImm8) {
            throw new IllegalStateException(String.format(
                    "patch 偏移原值不符合预期: 0x%06X current=0x%02X expected=0x%02X target=0x%02X",
                    offset,
                    current,
                    expectedImm8,
                    targetImm8
            ));
        }

        exeBytes[offset + 1] = (byte) targetImm8;
        plan.getTargetOffsets().add(String.format(
                "0x%06X: 6A %02X -> 6A %02X",
                offset,
                current,
                targetImm8
        ));
    }

    private String buildTimestampedExeName(Path sourceExe) {
        String fileName = sourceExe.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String base = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot >= 0 ? fileName.substring(dot) : ".exe";
        return base + "_" + LocalDateTime.now().format(TS) + ext;
    }
}
