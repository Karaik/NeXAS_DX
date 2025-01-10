package com.giga.nexasdxeditor.dto;

import com.giga.nexasdxeditor.io.BinaryReader;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public class Script {
    private List<Instruction> instructions = new ArrayList<>();
    byte[] preInstructions;
    private List<String> stringTable = new ArrayList<>();
    private List<Long> commands = new ArrayList<>();
    private List<String> properties = new ArrayList<>();
    private List<String> properties2 = new ArrayList<>();
    private List<byte[]> table = new ArrayList<>();

    public byte[] getPreInstructions() {
        return preInstructions;
    }

    public void setPreInstructions(byte[] preInstructions) {
        this.preInstructions = preInstructions;
    }

    public List<Instruction> getInstructions() {
        return instructions;
    }

    public List<String> getStringTable() {
        return stringTable;
    }

    public List<Long> getCommands() {
        return commands;
    }

    public List<String> getProperties() {
        return properties;
    }

    public List<String> getProperties2() {
        return properties2;
    }

    public List<byte[]> getTable() {
        return table;
    }

    public void setTable(List<byte[]> table) {
        this.table = table;
    }

    public void findEntries() {
        // Implementation can be added if necessary
    }

    public void setConstants(List<byte[]> tables) {
        // Implementation can be added if necessary
    }


    public static String parseScript(Script script, BinaryReader br, boolean forceEncoding) throws IOException {
        StringBuilder sb = new StringBuilder();
        int count = 0;

        // 判断是否为新版本
        count = br.readInt32();
        script.setPreInstructions(br.readBytes(count * 8)); // 读取预指令

        // 读取指令
        count = br.readInt32();
        for (int i = 0; i < count; i++) {
            Instruction inst = new Instruction();
            inst.setOffset(i);
            inst.setOpcode(br.readInt32());
            inst.setOperand(br.readInt32());
            script.getInstructions().add(inst);
            br.rewind(8); // 回退 8 字节
            script.getCommands().add(br.readInt64());
        }

        // 查找条目
        script.findEntries();

        // 读取字符串表
        count = br.readInt32();
        for (int i = 0; i < count; i++) {
            long curPos = br.getPosition();
            while (br.readByte() != 0) {} // 读取直到遇到 0 字节
            int len = (int) (br.getPosition() - curPos);
            br.setPosition(curPos);
            String txt = new String(br.readBytes(len), Charset.forName("UTF-8")).replace("\0", "");
            if (forceEncoding && txt.length() > 3 && txt.substring(txt.length() - 3).equals("bin")) {
                br.setPosition(curPos);
                txt = new String(br.readBytes(len), Charset.forName("Shift-JIS")).replace("\0", "");
            }

            script.getStringTable().add(String.format("\"%s\"", txt));

        }

        // 读取属性

        count = br.readInt32();
        for (int i = 0; i < count; i++) {
            long curPos = br.getPosition();
            while (br.readByte() != 0) {} // 读取直到遇到 0 字节
            int len = (int) (br.getPosition() - curPos);
            br.setPosition(curPos);
            String prop = new String(br.readBytes(len), Charset.forName("Shift-JIS")).replace('(', '[').replace(')', ']');
            script.getProperties().add(prop.replace("\0", ""));
        }

        // 如果是新版本，读取额外的属性

        count = br.readInt32();
        for (int i = 0; i < count; i++) {
            long curPos = br.getPosition();
            while (br.readByte() != 0) {} // 读取直到遇到 0 字节
            int len = (int) (br.getPosition() - curPos);
            br.setPosition(curPos);
            String prop = new String(br.readBytes(len), Charset.forName("Shift-JIS")).replace('(', '[').replace(')', ']');
            script.getProperties2().add(prop.replace("\0", ""));
        }


        // 读取表格数据
        count = br.readInt32();
        List<byte[]> tables = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            tables.add(br.readBytes(68));
        }
        script.setTable(tables);
        script.setConstants(tables);

        return sb.toString();
    }


    private static byte[] readBytes(RandomAccessFile raf, int length) throws IOException {
        byte[] bytes = new byte[length];
        raf.readFully(bytes);  // 直接从文件中读取指定字节数
        return bytes;
    }



    public static class Instruction {
        private int opcode;
        private int operand;
        private int offset;

        public int getOpcode() {
            return opcode;
        }

        public void setOpcode(int opcode) {
            this.opcode = opcode;
        }

        public int getOperand() {
            return operand;
        }

        public void setOperand(int operand) {
            this.operand = operand;
        }

        public int getOffset() {
            return offset;
        }

        public void setOffset(int offset) {
            this.offset = offset;
        }
    }
}


