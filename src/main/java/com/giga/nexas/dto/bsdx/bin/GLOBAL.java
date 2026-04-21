package com.giga.nexas.dto.bsdx.bin;

import com.giga.nexas.dto.bsdx.Bsdx;
import lombok.Data;

import java.util.List;

/**
 * BSDX `__GLOBAL.bin` 的解析结果。
 *
 * <p>这一层不把 `__GLOBAL.bin` 当成普通关卡脚本，而是明确表达成：
 * 一张全局符号表、两张当前样本里为空的保留字符串表，以及一段初始化指令流。
 * 这些数据会在普通 BSDX `bin` 加载前先建立全局脚本运行环境。
 */
@Data
public class GLOBAL extends Bsdx {

    /**
     * 第一张字符串表。
     *
     * <p>当前真实 BSDX 样本中，这张表是全局符号名表，例如：
     * `LUCK_TABLE`、`HP_NANOHA`、`g_nqHealCnt`。
     */
    private List<String> symbolTable;

    /**
     * 第二张字符串表。
     *
     * <p>当前 BSDX 样本里该表为空，但仍然保留字段以维持真实文件结构。
     */
    private List<String> reservedTable2;

    /**
     * 第三张字符串表。
     *
     * <p>当前 BSDX 样本里该表为空，但仍然保留字段以维持真实文件结构。
     */
    private List<String> reservedTable3;

    /**
     * `__GLOBAL.bin` 尾部的初始化指令流。
     *
     * <p>这部分不是普通剧情脚本，而是 VM 的全局默认值初始化程序。
     */
    private List<InitInstruction> initInstructions;

    /**
     * `__GLOBAL.bin` 初始化流中的单条 8 字节指令。
     */
    @Data
    public static class InitInstruction {

        /**
         * 初始化指令 opcode。
         */
        private int opcode;

        /**
         * 初始化指令 operand。
         */
        private int operand;

        /**
         * 指令在 `__GLOBAL.bin` 初始化段中的顺序号。
         */
        private int index;
    }
}
