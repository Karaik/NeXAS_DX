package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

/**
 * 执行 exe 兼容 patch。
 *
 * <p>这一步只负责“按 profile 改字节并写出新 exe”，不负责计算容量需求；
 * 容量需求由 {@link BuildExePatchPlanStep} 写入 {@link TsukuyomiExePatchPlan}。</p>
 *
 * <p>每个 patch site 都接受两种合法输入：expected bytes 或 target bytes。
 * 这是链式成果物模型的关键约束：当 BSDX+TSUKUYOMI 成为下一层 baseline 时，
 * 同一个 patch site 可能已经是目标字节，重复执行不应该失败。</p>
 */
public class ApplyExePatchStep {

    /**
     * patched exe 输出文件名里的时间戳格式。
     *
     * <p>旧 pipeline 会把 patched exe 写到 out 目录并带时间戳；
     * 当前 保持同类输出习惯，最终 parity 会比较实际生成的 exe bytes。</p>
     */
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    /**
     * EXE 中 SelectMekaMenu 可见行最大 offset 的 imm32 文件偏移。
     *
     * <p>当前 Tsukuyomi 复用现有可见槽位，不需要改这里；
     * 只有未来真的追加可见菜单行，并完成 switch/object-id 审计后，才会通过这个 offset 写新上界。</p>
     */
    private static final int SELECT_MEKA_MENU_ROW_LIMIT_IMM32_OFFSET = 0x14F21F;

    /**
     * patch site 字节校验器。
     *
     * <p>执行前必须确认目标位置匹配 expectedBytes 或 targetBytes，
     * 防止在未知 exe 版本上盲写魔法字节。</p>
     */
    private final PatchBytesVerifier verifier;

    public ApplyExePatchStep() {
        this(new PatchBytesVerifier());
    }

    public ApplyExePatchStep(PatchBytesVerifier verifier) {
        this.verifier = verifier == null ? new PatchBytesVerifier() : verifier;
    }

    public ExePatchAudit apply(
            TsukuyomiGraftRequest request,
            TsukuyomiExePatchPlan plan,
            ExePatchProfile profile
    ) {
        ExePatchAudit audit = new ExePatchAudit();
        if (request == null || !request.isPlanExeCapacityPatch()) {
            return audit;
        }
        if (plan == null || plan.getSourceExePath() == null || !Files.exists(plan.getSourceExePath())) {
            throw new IllegalStateException("目标 exe 不存在: " + (plan == null ? null : plan.getSourceExePath()));
        }

        ExePatchProfile patchProfile = profile == null ? ExePatchProfile.defaultTsukuyomiCompatibilityProfile() : profile;
        try {
            byte[] exeBytes = Files.readAllBytes(plan.getSourceExePath());
            for (ExePatchSite site : patchProfile.getSites()) {
                applySite(exeBytes, site, plan, audit);
            }
            applySelectMenuRowPatchIfNeeded(exeBytes, plan, audit);

            Path outputDir = request.getExeOutputDir();
            Files.createDirectories(outputDir);
            Path outputExe = outputDir.resolve(buildTimestampedExeName(plan.getSourceExePath()));
            Files.write(outputExe, exeBytes);

            plan.setPatched(!plan.getTargetOffsets().isEmpty());
            plan.setOutputExePath(outputExe);
            plan.getNotes().add("patched exe 已写出到 resources/out。");
            return audit;
        } catch (IOException e) {
            throw new IllegalStateException("写出 patched exe 失败", e);
        }
    }

    private void applySite(byte[] exeBytes, ExePatchSite site, TsukuyomiExePatchPlan plan, ExePatchAudit audit) {
        verifier.verifySiteInRange(exeBytes, site);
        boolean matchesExpected = verifier.matches(exeBytes, site.getOffset(), site.getExpectedBytes());
        boolean matchesTarget = verifier.matches(exeBytes, site.getOffset(), site.getTargetBytes());
        if (!matchesExpected && !matchesTarget) {
            throw new IllegalStateException(String.format(
                    "exe patch 偏移原值不符合预期: 0x%06X (%s)",
                    site.getOffset(),
                    site.getLabel()
            ));
        }

        // 即使当前已经是 target bytes，也统一写回 target bytes。
        // 这样输出路径和审计列表保持稳定，下一层成果物继续累加 patch 时也能判断为 already patched。
        System.arraycopy(site.getTargetBytes(), 0, exeBytes, site.getOffset(), site.getTargetBytes().length);
        if (matchesExpected) {
            audit.addAppliedSite(site.getLabel());
        } else {
            audit.addAlreadyPatchedSite(site.getLabel());
        }
        plan.getTargetOffsets().add(formatTargetOffset(site, matchesExpected));
    }

    private void applySelectMenuRowPatchIfNeeded(byte[] exeBytes, TsukuyomiExePatchPlan plan, ExePatchAudit audit) {
        if (plan.getRequiredSelectMekaMenuRows() <= 0) {
            return;
        }

        int targetMaxOffset = Math.max(0, (plan.getRequiredSelectMekaMenuRows() - 1) * 12);
        if (targetMaxOffset == 0x33C) {
            // 当前 Tsukuyomi 菜单策略是复用第 26 个可见槽位，不新增 SelectMekaMenu 行。
            // 因此旧 exe 里 70 行上界保持原值，记录 note 而不 patch。
            plan.getNotes().add("SelectMekaMenu 当前仍保持 70 个可见槽，本轮不需要改 0x14F21F。");
            return;
        }

        ExePatchSite site = ExePatchSite.builder()
                .offset(SELECT_MEKA_MENU_ROW_LIMIT_IMM32_OFFSET)
                .expectedBytes(littleEndian(0x33C))
                .targetBytes(littleEndian(targetMaxOffset))
                .label("SelectMekaMenu row upper-bound")
                .build();
        applySite(exeBytes, site, plan, audit);
    }

    private String formatTargetOffset(ExePatchSite site, boolean matchesExpected) {
        String before = formatBytes(matchesExpected ? site.getExpectedBytes() : site.getTargetBytes());
        String after = formatBytes(site.getTargetBytes());
        if (site.getTargetBytes().length == 1) {
            return String.format(
                    "0x%06X: %s 0x%s -> 0x%s",
                    site.getOffset(),
                    site.getLabel(),
                    before,
                    after
            );
        }
        if (site.getTargetBytes().length == 4 && looksLikeImm32(site)) {
            // 单纯按长度无法区分 imm32 patch 和 4-byte 指令序列 patch。
            // 当前 profile 里 battle voice bypass 也是 4 bytes，但它要按字节序列打印；
            // 其他 4-byte site 才按 little-endian imm32 打印，保持旧 targetOffsets 文本完全一致。
            return String.format(
                    "0x%06X: %s 0x%08X -> 0x%08X",
                    site.getOffset(),
                    site.getLabel(),
                    readLittleEndianInt(matchesExpected ? site.getExpectedBytes() : site.getTargetBytes(), 0),
                    readLittleEndianInt(site.getTargetBytes(), 0)
            );
        }
        return String.format(
                "0x%06X: %s %s -> %s",
                site.getOffset(),
                site.getLabel(),
                before,
                after
        );
    }

    private boolean looksLikeImm32(ExePatchSite site) {
        return site.getLabel() != null
                && !site.getLabel().contains("battle voice gate bypass")
                && site.getExpectedBytes().length == 4
                && site.getTargetBytes().length == 4
                && !Arrays.equals(site.getExpectedBytes(), site.getTargetBytes());
    }

    private String formatBytes(byte[] bytes) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < bytes.length; i++) {
            if (i > 0) {
                builder.append(' ');
            }
            builder.append(String.format("%02X", bytes[i] & 0xFF));
        }
        return builder.toString();
    }

    private byte[] littleEndian(int value) {
        return new byte[]{
                (byte) (value & 0xFF),
                (byte) ((value >>> 8) & 0xFF),
                (byte) ((value >>> 16) & 0xFF),
                (byte) ((value >>> 24) & 0xFF)
        };
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private String buildTimestampedExeName(Path sourceExe) {
        String fileName = sourceExe.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot >= 0 ? fileName.substring(0, dot) : fileName;
        String ext = dot >= 0 ? fileName.substring(dot) : ".exe";
        return baseName + "_" + LocalDateTime.now().format(TS) + ext;
    }
}
