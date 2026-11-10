package com.giga.nexas.dto.bsdx.bin.strictir;

import java.util.List;

/**
 * BIN 数据区中的一条固定 68 字节表项。
 *
 * @param slotIndex 该表项在表数组中的顺序位置
 * @param raw68 原始 68 字节内容的无符号整数镜像
 */
public record BsdxBinStrictIrTableEntry(
        int slotIndex,
        List<Integer> raw68
) {
}
