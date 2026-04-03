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
 * 负责对固定 exe 做容量补丁。
 *
 * <p>当前实现严格按固定绝对偏移直接覆盖字节，不做模式搜索。</p>
 */
public class PatchExeCapacitiesStep {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    /**
     * 机体侧 103 容量在当前固定 exe 里的两个绝对偏移。
     *
     * <p>两处原始指令都是 {@code 6A 67}，也就是 {@code push 103}。</p>
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

        int requiredMekaCapacity = resolveRequiredMekaCapacity(bsdxBaseline, grpAppendPlan);
        plan.setRequiredMekaCapacity(requiredMekaCapacity);
        plan.getNotes().add("当前按固定绝对偏移直接 patch exe。");
        plan.getNotes().add("当前只处理机体侧 103 容量链。");

        // 这里 patch 的是 push imm8，所以当前只能安全覆盖到 0..127。
        if (requiredMekaCapacity < 0 || requiredMekaCapacity > 127) {
            throw new IllegalStateException("当前 fixed-offset patch 只支持 0..127 的机体容量，实际需求=" + requiredMekaCapacity);
        }

        Path sourceExe = request.getTargetExePath();
        if (sourceExe == null || !Files.exists(sourceExe)) {
            throw new IllegalStateException("目标 exe 不存在: " + sourceExe);
        }

        try {
            byte[] exeBytes = Files.readAllBytes(sourceExe);

            // 逐个固定偏移做原地覆盖。
            for (int offset : MEKA_CAPACITY_PATCH_OFFSETS) {
                applyImm8Patch(exeBytes, offset, 0x67, requiredMekaCapacity, plan);
            }

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

    private int resolveRequiredMekaCapacity(BsdxBaselineBundle bsdxBaseline, GrpAppendPlan grpAppendPlan) {
        int byGrpSize = -1;
        if (bsdxBaseline != null
                && bsdxBaseline.getMekaGroupGrp() != null
                && bsdxBaseline.getMekaGroupGrp().getMekaList() != null) {
            byGrpSize = bsdxBaseline.getMekaGroupGrp().getMekaList().size();
        }

        int byAppendPlan = grpAppendPlan != null ? grpAppendPlan.getMekaGroupIndex() + 1 : -1;
        return Math.max(byGrpSize, byAppendPlan);
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
