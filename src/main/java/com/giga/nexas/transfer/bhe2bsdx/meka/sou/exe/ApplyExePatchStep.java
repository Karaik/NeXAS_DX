package com.giga.nexas.transfer.bhe2bsdx.meka.sou.exe;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiExePatchPlan;

import java.io.IOException;
import com.giga.nexas.transfer.util.ExeOutputNameSupport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;


public class ApplyExePatchStep {

    
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    
    private static final int SELECT_MEKA_MENU_ROW_LIMIT_IMM32_OFFSET = 0x14F21F;

    
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

        ExePatchProfile patchProfile = profile == null
                ? ExePatchProfile.forCapacities(plan.getRequiredMekaCapacity(), plan.getRequiredWeaponEquipRows())
                : profile;
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
        return ExeOutputNameSupport.buildTimestampedExeName(sourceExe, TS);
    }
}
