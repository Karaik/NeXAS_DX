package com.giga.nexas.dto.clarias.dat.generator;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.giga.nexas.dto.clarias.ClariasGenerator;
import com.giga.nexas.dto.clarias.dat.Dat;
import com.giga.nexas.io.BinaryWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import static com.giga.nexas.util.ParserUtil.DAT_COLUMN_TYPE_DATA;
import static com.giga.nexas.util.ParserUtil.DAT_COLUMN_TYPE_INT_NEW;
import static com.giga.nexas.util.ParserUtil.DAT_COLUMN_TYPE_STRING;
import static com.giga.nexas.util.ParserUtil.TYPE_INT;
import static com.giga.nexas.util.ParserUtil.TYPE_INT_NEW;
import static com.giga.nexas.util.ParserUtil.TYPE_STRING;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/11/12
 * @Description DatGenerator
 */
@Slf4j
public class DatGenerator implements ClariasGenerator<Dat> {

    private static final String DEFAULT_CHARSET = "windows-31j";

    @Override
    public String supportExtension() {
        return "dat";
    }

    @Override
    public void generate(String path, Dat dat, String charsetName) throws IOException {
        List<String> columnTypes = dat.getColumnTypes();

        FileUtil.mkdir(FileUtil.getParent(path, 1));

        String effectiveCharset = charsetName;
        try (OutputStream os = new BufferedOutputStream(new FileOutputStream(path));
             BinaryWriter writer = new BinaryWriter(os, effectiveCharset)) {

            writeColumnDefinitions(writer, dat.getColumnCount(), columnTypes);
            writeRows(writer, columnTypes, dat.getData());
        }
    }

    private void writeColumnDefinitions(BinaryWriter writer, int columnCount, List<String> columnTypes) throws IOException {
        int effectiveColumnCount = columnCount;
        if (effectiveColumnCount != columnTypes.size()) {
            log.warn("columnCount({}) != columnTypes.size({}), fallback to columnTypes size.", columnCount, columnTypes.size());
            effectiveColumnCount = columnTypes.size();
        }

        writer.writeInt(effectiveColumnCount);
        for (int i = 0; i < effectiveColumnCount; i++) {
            writer.writeInt(resolveColumnFlag(columnTypes.get(i)));
        }
    }

    private void writeRows(BinaryWriter writer, List<String> columnTypes, List<List<Object>> rows) throws IOException {
        for (List<Object> row : rows) {
            for (int i = 0; i < columnTypes.size(); i++) {
                String columnType = columnTypes.get(i);
                Object value = row.get(i);
                writeValue(writer, columnType, value);
            }
        }
    }

    private void writeValue(BinaryWriter writer, String columnType, Object value) throws IOException {
        switch (columnType) {
            case TYPE_STRING:
                writer.writeNullTerminatedString(String.valueOf(value));
                break;
            case TYPE_INT:
            case TYPE_INT_NEW:
                writer.writeInt(convertToInt(value));
                break;
            default:
                log.warn("Unsupported column type '{}', fallback to string serialization.", columnType);
                writer.writeNullTerminatedString(String.valueOf(value));
        }
    }

    private int resolveColumnFlag(String columnType) {
        if (TYPE_STRING.equalsIgnoreCase(columnType)) {
            return DAT_COLUMN_TYPE_STRING;
        }
        if (TYPE_INT.equalsIgnoreCase(columnType)) {
            return DAT_COLUMN_TYPE_DATA;
        }
        if (TYPE_INT_NEW.equalsIgnoreCase(columnType)) {
            return DAT_COLUMN_TYPE_INT_NEW;
        }

        if (StrUtil.isNotBlank(columnType)) {
            try {
                return Integer.parseInt(columnType.trim());
            } catch (NumberFormatException ignored) {
                // fall-through to warn below
            }
        }

        log.warn("Unknown column type '{}', fallback to DAT_COLUMN_TYPE_STRING.", columnType);
        return DAT_COLUMN_TYPE_STRING;
    }

    private int convertToInt(Object value) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String && StrUtil.isNotBlank((String) value)) {
            try {
                return Integer.parseInt(((String) value).trim());
            } catch (NumberFormatException ignored) {
                log.warn("Failed to parse int from '{}', fallback to 0.", value);
            }
        }
        return 0;
    }
}
