package com.giga.nexasdxeditor;

import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.giga.nexasdxeditor.dto.bsdx.grp.GroupMap;
import com.giga.nexasdxeditor.dto.bsdx.grp.parser.GroupMapParser;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@SpringBootTest
public class TestGrp {

    Logger log = LoggerFactory.getLogger(TestGrp.class);

    @Test
    void testGrp2Excel() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取 resources 目录下的 .grp 文件
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/grp/SpriteGroup.grp");

        String outputExcelPath = "src/main/resources/output_grp.xlsx";

        // 确保输出文件的目录存在
        File outputFile = new File(outputExcelPath);
        outputFile.getParentFile().mkdirs(); // 创建父目录
        if (outputFile.exists()) {
            outputFile.delete(); // 删除旧的空文件
        }

        // 创建 Excel 写入器
        ExcelWriter writer = ExcelUtil.getWriter(outputFile);
        GroupMapParser parser = new GroupMapParser();

        // 遍历所有的 .grp 文件
        for (Resource resource : resources) {
            String filePath = resource.getFile().getPath();
            String sheetName = resource.getFilename().replace(".grp", "");  // 去掉扩展名作为 Sheet 名

            // 读取并解析 .grp 文件
            byte[] fileBytes = Files.readAllBytes(resource.getFile().toPath());
            List<GroupMap> groupMapList = parser.parseGrp(fileBytes, "windows-31j");

            // 准备 Excel 数据
            List<Map<String, Object>> excelData = new ArrayList<>();
            for (GroupMap groupMap : groupMapList) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                rowMap.put("fileName", groupMap.getFileName());
                rowMap.put("codeName", groupMap.getCodeName());
                rowMap.put("itemQuantity", groupMap.getItemQuantity() != null ? groupMap.getItemQuantity() : "");

                excelData.add(rowMap);
            }

            // 切换到当前 Sheet 并写入数据
            writer.setSheet(sheetName);
            writer.write(excelData, true);  // 写入标题行

            // 自动调整列宽
            for (int columnIndex = 0; columnIndex < 3; columnIndex++) {
                writer.getSheet().autoSizeColumn(columnIndex);
            }
        }

        // 关闭写入器
        writer.close();
        log.info("Excel 文件生成成功，路径：" + outputFile.getAbsolutePath());
    }

}
