package com.giga.nexas.dto.clarias.dat.parser;

import com.giga.nexas.dto.clarias.ClariasParser;
import com.giga.nexas.dto.clarias.dat.Dat;
import com.giga.nexas.io.BinaryReader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.giga.nexas.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/11/12
 * @Description DatParser
 */
@Slf4j
public class DatParser implements ClariasParser<Dat> {

    @Override
    public String supportExtension() {
        return "dat";
    }

    private static final Set<String> FILTER_FILES = Set.of(
            "mekaformationlist",
            "mekapartnercpulist",
            "wazafreeparamdefine"
    );

    @Override
    public Dat parse(byte[] data, String fileName, String charset) {
        Dat dat = new Dat();
        dat.setFileName(fileName);

        if (FILTER_FILES.contains(fileName)) {
            return dat;
        }

        BinaryReader reader = new BinaryReader(data);
        try {
            // 读取列数
            int columnCount = reader.readInt();
            dat.setColumnCount(columnCount);

            log.info("Parsing Clarias Dat file: {}", fileName);
            log.info("Column count          : {}", columnCount);

            // 1. 读列类型
            List<String> columnTypes = readColumnTypes(reader, columnCount);
            dat.setColumnTypes(columnTypes);

            // 2. 按列类型顺序读取每一行数据
            readRows(reader, data.length, dat);

        } catch (Exception e) {
            log.error("Failed to parse Clarias .dat file: {}", fileName, e);
            throw e;
        }

        return dat;
    }

    /**
     * 读取列类型列表
     */
    private List<String> readColumnTypes(BinaryReader reader, int columnCount) {
        List<String> types = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            int typeFlag = reader.readInt();
            String type = resolveColumnType(typeFlag);
            types.add(type);
        }

        return types;
    }

    /**
     * 将类型标志位映射为内部使用的类型字符串
     */
    private String resolveColumnType(int typeFlag) {
        switch (typeFlag) {
            case DAT_COLUMN_TYPE_STRING:
                return TYPE_STRING;
            case DAT_COLUMN_TYPE_DATA:
                return TYPE_INT;
            case DAT_COLUMN_TYPE_INT_NEW:
                return TYPE_INT_NEW;
            case DAT_COLUMN_TYPE_BYTE:
                return TYPE_BYTE;
            default:
                return String.valueOf(typeFlag);
        }
    }

    /**
     * 按列类型循环读取所有行
     */
    private void readRows(BinaryReader reader, int dataLength, Dat dat) {
        List<String> columnTypes = dat.getColumnTypes();

        while (reader.getPosition() < dataLength) {
            List<Object> row = new ArrayList<>(columnTypes.size());

            for (String columnType : columnTypes) {
                row.add(readValueByType(reader, columnType));
            }

            dat.addRow(row);
        }
    }

    /**
     * 根据列类型读取单个字段
     */
    private Object readValueByType(BinaryReader reader, String columnType) {
        switch (columnType) {
            case TYPE_STRING:
                return reader.readNullTerminatedString();
            case TYPE_INT:
            case TYPE_INT_NEW:
                return reader.readInt();
            case TYPE_BYTE:
                return reader.readByte();
            default:
                log.warn("Unexpected column type: {}", columnType);
                return null;
        }
    }
}
