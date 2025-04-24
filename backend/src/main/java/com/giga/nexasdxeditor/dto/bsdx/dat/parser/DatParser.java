package com.giga.nexasdxeditor.dto.bsdx.dat.parser;

import com.giga.nexasdxeditor.dto.bsdx.BsdxParser;
import com.giga.nexasdxeditor.dto.bsdx.dat.Dat;
import com.giga.nexasdxeditor.io.BinaryReader;
import com.giga.nexasdxeditor.util.ParserUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/1
 * @Description DatParser
 */
@Slf4j
public class DatParser implements BsdxParser<Dat> {

    @Override
    public String supportExtension() {
        return "dat";
    }

    @Override
    public Dat parse(byte[] data, String fileName, String charset) {
        Dat dat = new Dat();
        dat.setFileName(fileName);

        BinaryReader reader = new BinaryReader(data);
        try {

            // 读取列数
            int columnCount = reader.readInt();
            dat.setColumnCount(columnCount);
            log.info("Column count == {}", columnCount);
            log.info("fileName == {}", fileName);

            // 读取每列的数据类型
            for (int i = 0; i < columnCount; i++) {
                int typeFlag = reader.readInt();
                String type;
                if (typeFlag == ParserUtil.DAT_COLUMN_TYPE_STRING) {
                    type = "String";
                } else if (typeFlag == ParserUtil.DAT_COLUMN_TYPE_DATA) {
                    type = "Integer";
                } else if (typeFlag == ParserUtil.DAT_COLUMN_TYPE_UNKNOWN) {
                    type = "Unknown";
                } else {
//                    throw new IllegalArgumentException("未知的类型！" + typeFlag);
                    return dat;
                }
                dat.addColumnType(type);
            }

            while (reader.getPosition() < data.length) {
                List<Object> row = new ArrayList<>();
                for (String columnType : dat.getColumnTypes()) {
                    if ("String".equals(columnType)) {
                        String value = reader.readNullTerminatedString();
                        row.add(value);
                    } else if ("Integer".equals(columnType)) {
                        int value = reader.readInt();
                        row.add(value);
                    } else if ("Unknown".equals(columnType)) {
                        int unknownValue = reader.readInt();
                        row.add(unknownValue);
                    }
                }
                dat.addRow(row);
            }

        } catch (Exception e) {
            log.error("Failed to parse .dat file: {}", e.getMessage(), e);
            throw e;
        }

        return dat;
    }
}
