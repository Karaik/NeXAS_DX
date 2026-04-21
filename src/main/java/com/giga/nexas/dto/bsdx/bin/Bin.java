package com.giga.nexas.dto.bsdx.bin;

import com.giga.nexas.dto.bsdx.Bsdx;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/4/24
 */
@Data
public class Bin extends Bsdx {
    
    // 入口点
    private List<Instruction> entryPoints;
     
    // 预指令块
    private byte[] preInstructions;
            
    // 指令集
    private List<Instruction> instructions;
     
    // 字符串表
    private List<String> stringTable;
     
    // 属性表
    private List<String> properties;
     
    // 属性表2
    private List<String> properties2;

    // 仅 BSDX 使用的全局符号表，来源于同目录 __GLOBAL.bin 的第一张符号名表
    private List<String> globalSymbols;

    // 常量表 key: 索引号, value: 常量数组
    private Map<Integer, Integer[]> constants;
     
    // 常量表2
    private Map<Integer, Integer[]> constants2;
    
    // 固定长度68字节的表
    private List<byte[]> table;

    // 文件字符集（元信息，参与写回）
    private String charset;

    // 预指令数量（元信息，参与写回）
    private Integer preCount;

    // 入口点索引列表（派生信息，用于还原，非元信息）
    public List<Integer> entryPointIndices;

    // 未解析的尾随原始字节（无损写回兜底）
    public byte[] tailRaw;

    @Data
    public static class Instruction {

        // 助记符视图
        private String opcode;
        // 操作数
//        private String operand;
        // 形参个数
        private Integer paramCount;

        // opcode/operand（元信息）
        private int opcodeNum;
        private int operandNum;

        // 指令在块中的序号（PC/Index）
        public int index;

        // CALL的nativeId（派生于 operandNum 的低16位）
        public Integer nativeId;

        // 自定义函数名（助记符视图或数字字符串，派生视图）
        public String nativeFunction;
    }
}
