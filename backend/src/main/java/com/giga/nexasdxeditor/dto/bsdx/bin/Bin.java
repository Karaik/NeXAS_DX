package com.giga.nexasdxeditor.dto.bsdx.bin;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/4/24
 */
@Data
public class Bin {
    
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

    // 常量表 key: 索引号, value: 常量数组
    private Map<Integer, Integer[]> constants;
     
    // 常量表2
    private Map<Integer, Integer[]> constants2;
    
    // 固定长度68字节的表
    private List<byte[]> table;
    
    // 指令内部类
    @Data
    public static class Instruction {
        // 操作码
        private String opcode;
        // 操作数
//        private String operand;
        // 形参个数
        private Integer paramCount;
        // 自定义函数
        private String nativeFunction;
    }
}
