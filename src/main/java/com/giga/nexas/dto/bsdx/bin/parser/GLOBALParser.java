package com.giga.nexas.dto.bsdx.bin.parser;

import com.giga.nexas.dto.bsdx.BsdxParser;
import com.giga.nexas.dto.bsdx.bin.GLOBAL;
import com.giga.nexas.io.BinaryReader;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/11
 * @Description GLOBALparser
 */
public class GLOBALParser implements BsdxParser<GLOBAL> {
    @Override
    public String supportExtension() {
        return "bin";
    }

    @Override
    public GLOBAL parse(byte[] data, String filename, String charset) {
        BinaryReader reader = new BinaryReader(data, charset);
        GLOBAL global = new GLOBAL();

        // 1. table1
        int table1Count = reader.readInt();
        List<String> table1 = new ArrayList<>();
        for (int i = 0; i < table1Count; i++) {
            table1.add(reader.readNullTerminatedString());
        }
        global.setTable1(table1);

        // 2. table2
        int table2Count = reader.readInt();
        List<String> table2 = new ArrayList<>();
        for (int i = 0; i < table2Count; i++) {
            table2.add(reader.readNullTerminatedString());
        }
        global.setTable2(table2);

        // 3. table3
        int table3Count = reader.readInt();
        List<String> table3 = new ArrayList<>();
        for (int i = 0; i < table3Count; i++) {
            table3.add(reader.readNullTerminatedString());
        }
        global.setTable3(table3);

        // 4. table4
        int table4Count = reader.readInt();
        List<String> table4 = new ArrayList<>();
        for (int i = 0; i < table4Count; i++) {
            table4.add(reader.readNullTerminatedString());
        }
        global.setTable4(table4);

        return global;
    }
}
