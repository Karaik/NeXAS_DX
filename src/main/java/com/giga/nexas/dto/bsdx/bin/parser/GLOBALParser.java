package com.giga.nexas.dto.bsdx.bin.parser;

import com.giga.nexas.dto.bsdx.BsdxParser;
import com.giga.nexas.dto.bsdx.bin.GLOBAL;
import com.giga.nexas.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * `__GLOBAL.bin` 的真实解析器。
 *
 * <p>当前 BSDX 样本中的磁盘结构已经确认是：
 * 三张字符串表 + 一段固定 8 字节对齐的初始化指令流。
 * 这里按真实结构逐段解析，不再沿用旧的“四张字符串表”假模型。
 */
public class GLOBALParser implements BsdxParser<GLOBAL> {

    @Override
    public String supportExtension() {
        return "bin";
    }

    /**
     * 按 `read__GLOBAL` 的真实顺序解析 `__GLOBAL.bin`。
     *
     * <p>这一步是无损结构化解析，不尝试解释 opcode 语义，只负责把磁盘数据
     * 提升成稳定的 DTO，供后续符号显示和全局环境初始化使用。
     */
    @Override
    public GLOBAL parse(byte[] data, String filename, String charset) {
        BinaryReader reader = new BinaryReader(data, charset);
        GLOBAL global = new GLOBAL();

        global.setSymbolTable(readStringTable(reader));
        global.setReservedTable2(readStringTable(reader));
        global.setReservedTable3(readStringTable(reader));
        global.setInitInstructions(readInitInstructions(reader));

        return global;
    }

    /**
     * 读取一张 null-terminated 字符串表。
     */
    private List<String> readStringTable(BinaryReader reader) {
        int count = reader.readInt();
        List<String> table = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            table.add(reader.readNullTerminatedString());
        }
        return table;
    }

    /**
     * 读取尾部初始化指令流。
     *
     * <p>当前每条指令固定为 8 字节：`opcode(u32) + operand(s32)`。
     */
    private List<GLOBAL.InitInstruction> readInitInstructions(BinaryReader reader) {
        int count = reader.readInt();
        List<GLOBAL.InitInstruction> instructions = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            GLOBAL.InitInstruction instruction = new GLOBAL.InitInstruction();
            instruction.setIndex(i);
            instruction.setOpcode(reader.readInt());
            instruction.setOperand(reader.readInt());
            instructions.add(instruction);
        }
        return instructions;
    }
}
