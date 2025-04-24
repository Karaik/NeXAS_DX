package com.giga.nexasdxeditor.dto.bsdx.bin.parser;

import com.giga.nexasdxeditor.dto.bsdx.BsdxParser;
import com.giga.nexasdxeditor.dto.bsdx.bin.Bin;
import com.giga.nexasdxeditor.dto.bsdx.bin.consts.BinConst;
import com.giga.nexasdxeditor.exception.BusinessException;
import com.giga.nexasdxeditor.io.BinaryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/4/24
 */
public class BinParser implements BsdxParser<Bin> {

    Logger log = LoggerFactory.getLogger(BinParser.class);

    private final static String SKIP_BIN = "__GLOBAL";

    @Override
    public String supportExtension() {
        return "bin";
    }

    @Override
    public Bin parse(byte[] data, String filename, String charset) {

        if (SKIP_BIN.equals(filename)) {
            log.info("skip === {} ", filename);
            return null;
        }

        BinaryReader reader = new BinaryReader(data, charset);

        Bin bin = new Bin();

        // 预指令块
        int preCount = reader.readInt();
        bin.setPreInstructions(preCount == 0 ? new byte[0] : reader.readBytes(preCount * 8));

        // 解析指令块、Commands 与入口点
        parseInstructions(reader, bin);

        // 解析字符串表
        parseStrings(reader, bin);

        // 解析属性表
        parseProperties(reader, bin);

        // 属性表2
        parseProperties2(reader, bin);

        // 解析常量表 & 68 字节表
        parseTablesAndConstants(reader, bin);

        // 返回最终结果
        return bin;
    }

    // 解析指令 + Commands + EntryPoints
    private void parseInstructions(BinaryReader reader, Bin bin) {
        // 指令数量
        int count = reader.readInt();

        if (count > 200000) {
            log.info("parseInstructions count ===  {} ", count);
            throw new BusinessException(500, "数值巨大");
        }

        // 指令列表
        List<Bin.Instruction> instructions = new ArrayList<>(count);

        // 遍历读取
        for (int i = 0; i < count; i++) {
            // 读取 opcodeNum
            int opcodeNum = reader.readInt();
            String opcode = BinConst.OPCODE_MNEMONIC_MAP.get(opcodeNum) == null ?
                    "UNKNOWN" + opcodeNum :
                    BinConst.OPCODE_MNEMONIC_MAP.get(opcodeNum);
            // 读取 operandNum
            int operandNum = reader.readInt();
            String operand = BinConst.OPERAND_MNEMONIC_MAP.get(operandNum) == null ?
                    "UNKNOWN" + opcodeNum :
                    BinConst.OPERAND_MNEMONIC_MAP.get(operandNum);

            // 构造 Instruction
            Bin.Instruction inst = new Bin.Instruction();
            inst.setOpcode(opcode);
            inst.setOperand(operand);
            instructions.add(inst);
        }

        // 填充对象
        bin.setInstructions(instructions);

        // 入口点映射
        List<Bin.Instruction> entryPoints = new ArrayList<>();
        for (Bin.Instruction instruction : instructions) {
            // 27:START
            if (BinConst.OPCODE_MNEMONIC_MAP.get(0x1B).equals(instruction.getOpcode())) {
                entryPoints.add(instruction);
            }
        }
        bin.setEntryPoints(entryPoints);
    }

    // 解析字符串表
    private void parseStrings(BinaryReader reader, Bin bin) {
        // 字符串数量
        int count = reader.readInt();

        if (count > 10000) {
            log.info("parseStrings count ===  {} ", count);
            throw new BusinessException(500, "数值巨大");
        }

        // 结果列表
        List<String> table = new ArrayList<>(count);

        // 循环读取
        for (int i = 0; i < count; i++) {
            // 记录字符串起始
            int start = reader.getPosition();
            // 查找 0 终止
            while (reader.readByte() != 0) {
            }
            // 记录结束位置
            int end = reader.getPosition();
            // 回到起始
            reader.seek(start);
            // 计算长度
            int len = end - start;
            // 读取字符串
            String txt = new String(reader.readBytes(len), reader.getCharset())
                    .replace("\0", "");
            // 按 C# 实现包双引号
            table.add("\"" + txt + "\"");
        }

        // 写入结果
        bin.setStringTable(table);
    }

    // 解析属性表
    private void parseProperties(BinaryReader reader, Bin bin) {
        // 属性数量
        int count = reader.readInt();

        if (count > 10000) {
            log.info("parseProperties count ===  {} ", count);
            throw new BusinessException(500, "数值巨大");
        }

        // 结果列表
        List<String> properties = new ArrayList<>(count);

        // 循环读取
        for (int i = 0; i < count; i++) {
            // 记录起始
            int start = reader.getPosition();
            // 查找 0 终止
            while (reader.readByte() != 0) {
            }
            // 记录结束
            int end = reader.getPosition();
            // 回到起始
            reader.seek(start);
            // 计算长度
            int len = end - start;
            String prop = new String(reader.readBytes(len), reader.getCharset())
                    .replace('(', '[')
                    .replace(')', ']')
                    .replace("\0", "");
            properties.add(prop);
        }

        // 写入结果
        bin.setProperties(properties);
    }

    // 解析属性表2
    private void parseProperties2(BinaryReader reader, Bin bin) {
        // 读取属性2数量
        int count = reader.readInt();

        if (count > 10000) {
            log.info("parseProperties2 count ===  {} ", count);
            throw new BusinessException(500, "数值巨大");
        }

        // 构造结果列表
        List<String> props2 = new ArrayList<>(count);

        // 循环读取 count 条 Shift-JIS 字符串
        for (int i = 0; i < count; i++) {
            // 记录起始位置
            int start = reader.getPosition();
            // 查找 0x00 终止符
            while (reader.readByte() != 0) {
            }
            // 记录结束位置
            int end = reader.getPosition();
            // 回到字符串开头
            reader.seek(start);
            // 计算长度（包含末尾 0）
            int len = end - start;

            // 替换 () → []
            String p2 = new String(reader.readBytes(len), reader.getCharset())
                    .replace('(', '[')
                    .replace(')', ']')
                    .replace("\0", "");

            props2.add(p2);
        }

        // 写回对象
        bin.setProperties2(props2);
    }

    // 解析 68 字节表 & 常量
    private void parseTablesAndConstants(BinaryReader reader, Bin bin) {
        // 表数量
        int tableCount = reader.readInt();

        if (tableCount > 10000) {
            log.info("parseTablesAndConstants count ===  {} ", tableCount);
            throw new BusinessException(500, "数值巨大");
        }

        // 68 字节表集合
        List<byte[]> tables = new ArrayList<>(tableCount);

        // 读取所有表
        for (int i = 0; i < tableCount; i++) {
            tables.add(reader.readBytes(68));
        }
        bin.setTable(tables);

        // 解析常量
        Map<Integer, Integer[]> constants = new HashMap<>();

        for (byte[] tbl : tables) {
            // 针对单表重新建 Reader
            BinaryReader tr = new BinaryReader(tbl);
            // 索引号
            int index = tr.readInt();
            // 常量列表
            List<Integer> nums = new ArrayList<>();
            // 读取到 -1 结束
            while (true) {
                int val = tr.readInt();
                if (val == 0xFFFFFFFF) {
                    break;
                }
                nums.add(val);
            }
            constants.put(index, nums.toArray(new Integer[0]));
        }

        // 写入结果
        bin.setConstants(constants);
    }

}
