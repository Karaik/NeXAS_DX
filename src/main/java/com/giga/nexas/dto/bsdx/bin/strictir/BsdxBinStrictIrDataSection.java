package com.giga.nexas.dto.bsdx.bin.strictir;

import java.util.List;
import java.util.Map;

/**
 * BIN 非代码区的无损镜像。
 *
 * <p>它和指令流一起挂在 {@link BsdxBinStrictIr} 上。当前 Structured IR 还不会深度解释这部分数据，
 * 但 round-trip 时必须始终带着它，否则字符串、属性、constants 和 68 字节表就会丢失。
 *
 * @param stringTable 供脚本参数使用的字符串表
 * @param properties 变量读写使用的主属性表
 * @param properties2 次级属性字符串表
 * @param tables 原始 68 字节常量表块
 * @param constants 对 {@code tables} 的解码视图
 * @param constants2 次级常量表族的预留镜像
 */
public record BsdxBinStrictIrDataSection(
        List<String> stringTable,
        List<String> properties,
        List<String> properties2,
        List<BsdxBinStrictIrTableEntry> tables,
        Map<Integer, List<Integer>> constants,
        Map<Integer, List<Integer>> constants2
) {
}
