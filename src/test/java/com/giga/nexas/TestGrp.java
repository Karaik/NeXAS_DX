package com.giga.nexas;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.giga.nexas.dto.bsdx.grp.GroupMap;
import com.giga.nexas.dto.bsdx.grp.parser.GroupMapParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TestGrp {

    private static final Path GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");
    private static final Path OUTPUT_EXCEL_PATH = Paths.get("src/main/resources/output_grp.xlsx");

    @Test
    void testGrp2Excel() throws IOException {
        if (!Files.exists(GRP_DIR)) {
            System.err.println("❌ GRP 目录不存在: " + GRP_DIR);
            return;
        }

        Files.createDirectories(OUTPUT_EXCEL_PATH.getParent());
        if (Files.exists(OUTPUT_EXCEL_PATH)) {
            Files.delete(OUTPUT_EXCEL_PATH);
        }

        ExcelWriter writer = ExcelUtil.getWriter(OUTPUT_EXCEL_PATH.toFile());
        GroupMapParser parser = new GroupMapParser();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String sheetName = path.getFileName().toString().replace(".grp", "");
                byte[] fileBytes = Files.readAllBytes(path);
                List<GroupMap> groupMapList = parser.parseGrp(fileBytes, "windows-31j");

                List<Map<String, Object>> excelData = new ArrayList<>();
                for (GroupMap groupMap : groupMapList) {
                    Map<String, Object> rowMap = new LinkedHashMap<>();
                    rowMap.put("fileName", groupMap.getFileName());
                    rowMap.put("codeName", groupMap.getCodeName());
                    rowMap.put("itemQuantity", groupMap.getItemQuantity() != null ? groupMap.getItemQuantity() : "");
                    excelData.add(rowMap);
                }

                writer.setSheet(sheetName);
                writer.write(excelData, true);

                for (int i = 0; i < 3; i++) {
                    writer.getSheet().autoSizeColumn(i);
                }

                System.out.println("✅ 导出 Sheet: " + sheetName);
            }
        }

        writer.close();
        System.out.println("✅ Excel 文件生成完成: " + OUTPUT_EXCEL_PATH.toAbsolutePath());
    }
}
