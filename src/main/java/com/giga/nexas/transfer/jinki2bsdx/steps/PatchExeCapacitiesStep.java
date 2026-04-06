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
 * 负责 patch exe 中的 meka 容量硬编码上限。
 *
 * <p>当 AKAO 被追加为 MekaGroup[103]（第 104 个条目，count 从 103→104）时，
 * 原版 exe 有 8 处硬编码的 {@code 103}（0x67）需要改为 {@code 104}（0x68），
 * 否则存档读/写、场景切换清理、初始化预分配、资源大小计算等功能在遇到
 * index 103 的机体时会跳过或越界。</p>
 *
 * <h3>8 处 meka capacity patch 位点</h3>
 * <p>所有偏移均为 BaldrSky.exe（4,662,784 字节，未加壳版）的<strong>文件绝对偏移</strong>。
 * 每处 patch 的字节位置（存储 0x67 的那个字节）如下：</p>
 * <table>
 *   <tr><th>#</th><th>偏移</th><th>指令</th><th>函数</th><th>用途</th></tr>
 *   <tr>
 *     <td>1</td><td>{@code 0x1E3F40}</td>
 *     <td>{@code push 103} (6A 67)</td>
 *     <td>sub_5E4B20 (init)</td>
 *     <td>ensureStructArrayCapacity(&dword_876080, 103, 0) —
 *         系统初始化时预分配 meka 槽数组，103→104 确保第 104 个槽位被分配</td>
 *   </tr>
 *   <tr>
 *     <td>2</td><td>{@code 0x1E3DA2}</td>
 *     <td>{@code cmp ebp, 103} (83 FD 67)</td>
 *     <td>sub_5E46A0 (scene cleanup)</td>
 *     <td>do{}while(v14&lt;103) — 场景切换时清理 dword_87608C 引用计数数组，
 *         103→104 确保遍历到 AKAO 的计数器</td>
 *   </tr>
 *   <tr>
 *     <td>3</td><td>{@code 0x276427}</td>
 *     <td>{@code cmp esi, 103} (83 FE 67)</td>
 *     <td>sub_676E00 (save read) loop#1</td>
 *     <td>while(v4&lt;103) — 存档读取第一个循环，遍历每个 meka 条目的类型标记，
 *         103→104 确保 AKAO 的类型被正确读出</td>
 *   </tr>
 *   <tr>
 *     <td>4</td><td>{@code 0x2762EB}</td>
 *     <td>{@code cmp esi, 103} (83 FE 67)</td>
 *     <td>sub_676E00 (save read) loop#2</td>
 *     <td>while(v10&lt;103) — 存档读取第二个循环，处理 meka 机体的详细状态初始化，
 *         103→104 确保 AKAO 的机体状态被正确恢复</td>
 *   </tr>
 *   <tr>
 *     <td>5</td><td>{@code 0x275336}</td>
 *     <td>{@code push 103} (6A 67)</td>
 *     <td>sub_675520 (save write)</td>
 *     <td>ensureStructArrayCapacity 预分配 meka 写入缓冲区，
 *         103→104 确保存档写入时 AKAO 的数据有足够空间</td>
 *   </tr>
 *   <tr>
 *     <td>6</td><td>{@code 0x063A85}</td>
 *     <td>{@code cmp ebx, 103} (83 FB 67)</td>
 *     <td>sub_464630 (resource calc)</td>
 *     <td>do{}while(v3&lt;103) — 资源大小累加循环，判断是否超过 100MB 阈值，
 *         103→104 确保 AKAO 的资源大小也被计入（虽然不计入反而更安全，
 *         但为完整性仍做 patch）</td>
 *   </tr>
 *   <tr>
 *     <td>7</td><td>{@code 0x2749EB}</td>
 *     <td>{@code cmp esi, 103} (83 FE 67)</td>
 *     <td>存档读区 loop#3（独立函数）</td>
 *     <td>do{}while(++i&lt;103) — 存档读取第三个循环，
 *         与 #3/#4 指令模式完全相同（inc esi; cmp esi,103; jl），
 *         103→104 确保 AKAO 的数据在第三个读区也被正确处理</td>
 *   </tr>
 *   <tr>
 *     <td>8</td><td>{@code 0x056CE3}</td>
 *     <td>{@code push 103} (6A 67)</td>
 *     <td>初始化预分配</td>
 *     <td>push 103 后紧跟 call，与 #1 模式类似，
 *         可能是另一处 ensureStructArrayCapacity 或类似预分配调用，
 *         103→104 确保 AKAO 的槽数组被预分配</td>
 *   </tr>
 * </table>
 *
 * <h3>定位方法</h3>
 * <p>PE section header 被加壳器混淆，无法通过 VA→file offset 映射定位。
 * 改用函数签名特征（全局变量地址如 dword_876080、dword_86CD5C、dword_86CD00 等）
 * 在 packed exe 二进制中搜索，再在确认的函数范围内搜索 {@code 0x67} 立即数。</p>
 *
 * <h3>patch 类型</h3>
 * <ul>
 *   <li>{@code push imm8}（6A 67）：修改 +1 偏移处的一个字节，103→104</li>
 *   <li>{@code cmp reg, imm8}（83 Fx 67）：修改 +2 偏移处的一个字节，103→104</li>
 * </ul>
 *
 * <h3>保留的 SelectMekaMenu patch</h3>
 * <p>IMM32 偏移 {@code 0x14F21F} 处的 SelectMekaMenu 行数上界 patch 仍保留，
 * 但当前策略是原地替换而非追加行，所以该值保持不变。</p>
 */
public class PatchExeCapacitiesStep {

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");
    private static final int BASELINE_VISIBLE_SELECT_MEKA_MENU_ROWS = 70;
    private static final int MAX_AUDITED_SELECT_MEKA_MENU_ROWS = 76;
    private static final int SELECT_MEKA_MENU_ROW_LIMIT_IMM32_OFFSET = 0x14F21F;

    // ── meka capacity 103→104 patch 位点 ──────────────────────────────────
    // 每项: (文件绝对偏移, 期望原值, 目标值, 描述)
    // "文件绝对偏移"指的是 exe 文件中存储 0x67（103）的那个字节的偏移。

    /** #1 sub_5E4B20 (init): push 103 → push 104 */
    private static final int MEKA_CAP_INIT_PUSH_OFFSET = 0x1E3F40;
    private static final int MEKA_CAP_INIT_EXPECTED = 0x67; // 103
    private static final int MEKA_CAP_INIT_TARGET = 0x68;   // 104

    /** #2 sub_5E46A0 (scene cleanup): cmp ebp, 103 → cmp ebp, 104 */
    private static final int MEKA_CAP_SCENE_CLEANUP_OFFSET = 0x1E3DA2;
    private static final int MEKA_CAP_SCENE_CLEANUP_EXPECTED = 0x67;
    private static final int MEKA_CAP_SCENE_CLEANUP_TARGET = 0x68;

    /** #3 sub_676E00 (save read) loop#1: cmp esi, 103 → cmp esi, 104 */
    private static final int MEKA_CAP_SAVE_READ_LOOP1_OFFSET = 0x276427;
    private static final int MEKA_CAP_SAVE_READ_LOOP1_EXPECTED = 0x67;
    private static final int MEKA_CAP_SAVE_READ_LOOP1_TARGET = 0x68;

    /** #4 sub_676E00 (save read) loop#2: cmp esi, 103 → cmp esi, 104 */
    private static final int MEKA_CAP_SAVE_READ_LOOP2_OFFSET = 0x2762EB;
    private static final int MEKA_CAP_SAVE_READ_LOOP2_EXPECTED = 0x67;
    private static final int MEKA_CAP_SAVE_READ_LOOP2_TARGET = 0x68;

    /** #5 sub_675520 (save write): push 103 → push 104 */
    private static final int MEKA_CAP_SAVE_WRITE_OFFSET = 0x275336;
    private static final int MEKA_CAP_SAVE_WRITE_EXPECTED = 0x67;
    private static final int MEKA_CAP_SAVE_WRITE_TARGET = 0x68;

    /** #6 sub_464630 (resource calc): cmp ebx, 103 → cmp ebx, 104 */
    private static final int MEKA_CAP_RESOURCE_CALC_OFFSET = 0x063A85;
    private static final int MEKA_CAP_RESOURCE_CALC_EXPECTED = 0x67;
    private static final int MEKA_CAP_RESOURCE_CALC_TARGET = 0x68;

    /** #7 save read loop#3 (独立函数): cmp esi, 103 → cmp esi, 104 */
    private static final int MEKA_CAP_SAVE_READ_LOOP3_OFFSET = 0x2749EB;
    private static final int MEKA_CAP_SAVE_READ_LOOP3_EXPECTED = 0x67;
    private static final int MEKA_CAP_SAVE_READ_LOOP3_TARGET = 0x68;

    /** #8 init prealloc: push 103 → push 104 */
    private static final int MEKA_CAP_INIT_PREALLOC_OFFSET = 0x056CE3;
    private static final int MEKA_CAP_INIT_PREALLOC_EXPECTED = 0x67;
    private static final int MEKA_CAP_INIT_PREALLOC_TARGET = 0x68;

    // ── 公开方法 ─────────────────────────────────────────────────────────

    /**
     * 执行 exe patch：将 meka capacity 从 103 改为 104。
     *
     * <p>当 MekaGroup count 从 103 增加到 104 时，exe 中所有硬编码的上界 103
     * 必须同步更新为 104，否则：</p>
     * <ul>
     *   <li>存档读取时 AKAO 的类型标记和状态不会被读出（#3, #4, #7）</li>
     *   <li>存档写入时 AKAO 不会被保存（#5）</li>
     *   <li>场景切换时 AKAO 的引用计数不会被清理（#2）</li>
     *   <li>初始化时 AKAO 的槽数组不会被预分配（#1, #8）</li>
     *   <li>资源大小计算跳过 AKAO（#6，影响较轻但为完整性仍 patch）</li>
     * </ul>
     */
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
        plan.getNotes().add("step10: patch exe meka capacity 103 -> 104 (8 sites).");

        Path sourceExe = request.getTargetExePath();
        if (sourceExe == null || !Files.exists(sourceExe)) {
            throw new IllegalStateException("目标 exe 不存在: " + sourceExe);
        }

        try {
            byte[] exeBytes = Files.readAllBytes(sourceExe);

            // ── meka capacity 103 → 104 (8 处) ──
            applyImm8Patch(exeBytes,
                    MEKA_CAP_INIT_PUSH_OFFSET,
                    MEKA_CAP_INIT_EXPECTED,
                    MEKA_CAP_INIT_TARGET,
                    "meka cap init: push 103 -> push 104 (sub_5E4B20 ensureStructArrayCapacity)",
                    plan);
            applyImm8Patch(exeBytes,
                    MEKA_CAP_SCENE_CLEANUP_OFFSET,
                    MEKA_CAP_SCENE_CLEANUP_EXPECTED,
                    MEKA_CAP_SCENE_CLEANUP_TARGET,
                    "meka cap scene cleanup: cmp ebp,103 -> cmp ebp,104 (sub_5E46A0 do-while)",
                    plan);
            applyImm8Patch(exeBytes,
                    MEKA_CAP_SAVE_READ_LOOP1_OFFSET,
                    MEKA_CAP_SAVE_READ_LOOP1_EXPECTED,
                    MEKA_CAP_SAVE_READ_LOOP1_TARGET,
                    "meka cap save read loop#1: cmp esi,103 -> cmp esi,104 (sub_676E00 type read)",
                    plan);
            applyImm8Patch(exeBytes,
                    MEKA_CAP_SAVE_READ_LOOP2_OFFSET,
                    MEKA_CAP_SAVE_READ_LOOP2_EXPECTED,
                    MEKA_CAP_SAVE_READ_LOOP2_TARGET,
                    "meka cap save read loop#2: cmp esi,103 -> cmp esi,104 (sub_676E00 state read)",
                    plan);
            applyImm8Patch(exeBytes,
                    MEKA_CAP_SAVE_WRITE_OFFSET,
                    MEKA_CAP_SAVE_WRITE_EXPECTED,
                    MEKA_CAP_SAVE_WRITE_TARGET,
                    "meka cap save write: push 103 -> push 104 (sub_675520 ensureStructArrayCapacity)",
                    plan);
            applyImm8Patch(exeBytes,
                    MEKA_CAP_RESOURCE_CALC_OFFSET,
                    MEKA_CAP_RESOURCE_CALC_EXPECTED,
                    MEKA_CAP_RESOURCE_CALC_TARGET,
                    "meka cap resource calc: cmp ebx,103 -> cmp ebx,104 (sub_464630 do-while)",
                    plan);
            applyImm8Patch(exeBytes,
                    MEKA_CAP_SAVE_READ_LOOP3_OFFSET,
                    MEKA_CAP_SAVE_READ_LOOP3_EXPECTED,
                    MEKA_CAP_SAVE_READ_LOOP3_TARGET,
                    "meka cap save read loop#3: cmp esi,103 -> cmp esi,104 (独立函数 do-while)",
                    plan);
            applyImm8Patch(exeBytes,
                    MEKA_CAP_INIT_PREALLOC_OFFSET,
                    MEKA_CAP_INIT_PREALLOC_EXPECTED,
                    MEKA_CAP_INIT_PREALLOC_TARGET,
                    "meka cap init prealloc: push 103 -> push 104 (初始化预分配)",
                    plan);

            // ── SelectMekaMenu 行数上界 (IMM32, 仅在行数增加时才 patch) ──
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

            // ── 写出 patched exe ──
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

    // ── 私有方法 ─────────────────────────────────────────────────────────

    /**
     * 应用 IMM8 patch（修改单个字节）。
     * 用于 {@code push imm8}（6A xx）和 {@code cmp reg, imm8}（83 Fx xx）指令。
     */
    private void applyImm8Patch(
            byte[] exeBytes,
            int offset,
            int expectedValue,
            int targetValue,
            String label,
            ExePatchPlan plan
    ) {
        if (offset < 0 || offset >= exeBytes.length) {
            throw new IllegalStateException(String.format("imm8 patch 偏移越界: 0x%06X", offset));
        }
        int current = exeBytes[offset] & 0xFF;
        if (current != expectedValue && current != targetValue) {
            throw new IllegalStateException(String.format(
                    "imm8 patch 偏移原值不符合预期: 0x%06X current=0x%02X expected=0x%02X target=0x%02X",
                    offset, current, expectedValue, targetValue
            ));
        }
        exeBytes[offset] = (byte) (targetValue & 0xFF);
        plan.getTargetOffsets().add(String.format(
                "0x%06X: %s 0x%02X -> 0x%02X",
                offset, label, current, targetValue
        ));
    }

    /**
     * 应用 IMM32 patch（修改 4 字节，小端序）。
     * 用于 {@code cmp eax, imm32}（3D xx xx xx xx）和 {@code mov [addr], imm32}（C7 xx xx xx xx）指令。
     */
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
