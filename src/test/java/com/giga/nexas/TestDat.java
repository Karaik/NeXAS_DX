package com.giga.nexas;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BinService;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TestDat {

    private static final Path INPUT_DIR = Paths.get("src/main/resources/game/bh/dat");
    private static final Path OUTPUT_EXCEL = Paths.get("src/main/resources/output_dat.xlsx");

    private final BinService binService = new BinService();

    @Test
    void testDat2Excel() throws IOException {
        if (!Files.exists(INPUT_DIR)) {
            System.err.println("❌ 输入路径不存在: " + INPUT_DIR);
            return;
        }

        Files.createDirectories(OUTPUT_EXCEL.getParent());
        if (Files.exists(OUTPUT_EXCEL)) {
            Files.delete(OUTPUT_EXCEL);
        }

        ExcelWriter writer = ExcelUtil.getWriter(OUTPUT_EXCEL.toFile());

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(INPUT_DIR, "*.dat")) {
            for (Path datFile : stream) {
                String sheetName = datFile.getFileName().toString().replace(".dat", "");
                ResponseDTO dto = binService.parse(datFile.toString(), "windows-31j");
                Dat dat = (Dat) dto.getData();

                List<Map<String, Object>> rows = new ArrayList<>();
                for (List<Object> row : dat.getData()) {
                    Map<String, Object> rowMap = new LinkedHashMap<>();
                    for (int i = 0; i < dat.getColumnTypes().size(); i++) {
                        String col = dat.getColumnTypes().get(i) + "_" + (i + 1);
                        rowMap.put(col, i < row.size() ? row.get(i) : "");
                    }
                    rows.add(rowMap);
                }

                writer.setSheet(sheetName);
                writer.write(rows, true);

                for (int i = 0; i < dat.getColumnTypes().size(); i++) {
                    writer.getSheet().autoSizeColumn(i);
                }

                System.out.println("✅ 写入 sheet: " + sheetName);
            }
        }

        writer.close();
        System.out.println("✅ Excel 输出成功: " + OUTPUT_EXCEL.toAbsolutePath());
    }
}
