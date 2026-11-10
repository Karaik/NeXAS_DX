package com.giga.nexas.dto.bsdx.bin.strictir;

/**
 * Instruction IR 层中的单条原始 VM 指令。
 *
 * <p>核心语义仍然由原始 {@code opcode/operand} 承载，不会在这一层做语义改写。
 * 额外的 CALL 字段只是从 operand 位域拆出来的辅助信息，目的是让更高层不必反复手工拆包。
 *
 * @param index 指令在当前指令流中的序号
 * @param opcodeNum BIN 中原始的 opcode 数值
 * @param operandNum BIN 中原始的 operand 数值
 * @param opcodeMnemonic 从 {@link com.giga.nexas.dto.bsdx.bin.consts.Opcode} 映射出的可选助记符
 * @param callArity 当 opcode 是 CALL 时拆出的参数个数，否则为 {@code null}
 * @param callNativeId 当 opcode 是 CALL 时拆出的目标函数 id，否则为 {@code null}
 * @param callNativeMnemonic 对应函数 id 的可选名字
 */
public record BsdxBinStrictIrInstruction(
        int index,
        int opcodeNum,
        int operandNum,
        String opcodeMnemonic,
        Integer callArity,
        Integer callNativeId,
        String callNativeMnemonic
) {
}
