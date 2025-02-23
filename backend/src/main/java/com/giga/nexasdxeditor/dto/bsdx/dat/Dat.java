package com.giga.nexasdxeditor.dto.bsdx.dat;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/1
 * @Description csv
 */
@Data
public class Dat {

    private String fileName;

    // 列数
    private int columnCount;

    // 每列的类型
    private List<String> columnTypes = new ArrayList<>();

    // 内容
    private List<List<Object>> data = new ArrayList<>();

    public void addColumnType(String type) {
        this.columnTypes.add(type);
    }

    public void addRow(List<Object> row) {
        this.data.add(row);
    }
}
