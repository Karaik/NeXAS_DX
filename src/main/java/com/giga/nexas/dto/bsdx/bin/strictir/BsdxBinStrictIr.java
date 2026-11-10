package com.giga.nexas.dto.bsdx.bin.strictir;

import java.util.List;

/**
 * BIN 新主链中的 Instruction IR 层。
 *
 * <p>这一层位于 {@link com.giga.nexas.dto.bsdx.bin.Bin} 之上，但还没有开始解释控制流或表达式，
 * 只负责把解析后的二进制内容整理成稳定、可比较、可继续提升的中间表示。
 *
 * <p>输入通常来自旧的 {@code BinParser} / {@code Bin} 对象，输出会继续送入
 * Structured IR 的 lifting 阶段，或者直接 lowering 回 {@code Bin} 做字节级 round-trip。
 *
 * @param extensionName 原始文件扩展名，回写二进制时仍然需要
 * @param charset 原始文件字符集，回生成 BIN 时必须保留
 * @param preCount BIN 头里的预指令数量
 * @param preInstructions 预指令原始字节，这里以无符号整数列表形式保存
 * @param instructions 原始指令流，CALL 额外带有拆包后的辅助字段
 * @param dataSection 字符串表、属性表、constants、68 字节表等非代码区的无损镜像
 * @param entryPointIndices 从 START 指令派生出的入口索引，兼容旧 BIN 工作流
 * @param tailRaw 解析器未解释但必须原样保留的尾部字节
 */
public record BsdxBinStrictIr(
        String extensionName,
        String charset,
        int preCount,
        List<Integer> preInstructions,
        List<BsdxBinStrictIrInstruction> instructions,
        BsdxBinStrictIrDataSection dataSection,
        List<Integer> entryPointIndices,
        List<Integer> tailRaw
) {
}
