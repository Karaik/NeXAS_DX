package com.giga.nexasdxeditor.dto.bsdx.dat.paser;

import com.giga.nexasdxeditor.dto.bsdx.dat.Dat;
import com.giga.nexasdxeditor.util.ParserUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.findNullTerminator;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/1
 * @Description DatParser
 */
@Slf4j
public class DatParser {

    public static Dat parseDat(byte[] bytes, String fileName) {
        Dat dat = new Dat();
        dat.setFileName(fileName);

        try {
            int offset = 0;

            // Step 1: 读取列数
            int columnCount = readInt32(bytes, offset);
            offset += 4;
            dat.setColumnCount(columnCount);
            log.info("Column count == {}", columnCount);
            log.info("fileName == {}", fileName);

            // Step 2: 读取每列的数据类型
            for (int i = 0; i < columnCount; i++) {
                int typeFlag = readInt32(bytes, offset);
                offset += 4;
                String type;
                if (typeFlag == ParserUtil.DAT_COLUMN_TYPE_STRING) {
                    type = "String";
                } else if (typeFlag == ParserUtil.DAT_COLUM_TYPE_DATA) {
                    type = "Integer";
                } else if (typeFlag == ParserUtil.DAT_COLUMN_TYPE_UNKNOWN) {
                    type = "Unknown";
                } else {
//                    throw new IllegalArgumentException("未知的类型！" + typeFlag);
                    return dat;
                }
                dat.addColumnType(type);
            }

            // Step 3: 逐行读取数据
            while (offset < bytes.length) {
                List<Object> row = new ArrayList<>();
                for (String columnType : dat.getColumnTypes()) {
                    if ("String".equals(columnType)) {
                        int stringLength = findNullTerminator(bytes, offset);
                        String value = new String(bytes, offset, stringLength, Charset.forName("Shift-JIS")).trim();
                        offset += stringLength + 1;
                        row.add(value);
                    } else if ("Integer".equals(columnType)) {
                        int value = readInt32(bytes, offset);
                        offset += 4;
                        row.add(value);
                    } else if ("Unknown".equals(columnType)) {
                        int unknownValue = readInt32(bytes, offset);
                        offset += 4;
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
