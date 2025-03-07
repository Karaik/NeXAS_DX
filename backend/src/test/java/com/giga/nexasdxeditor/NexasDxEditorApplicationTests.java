package com.giga.nexasdxeditor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.poi.excel.ExcelUtil;
import cn.hutool.poi.excel.ExcelWriter;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.dat.Dat;
import com.giga.nexasdxeditor.dto.bsdx.dat.paser.DatParser;
import com.giga.nexasdxeditor.dto.bsdx.grp.GroupMap;
import com.giga.nexasdxeditor.dto.bsdx.grp.parser.GroupMapParser;
import com.giga.nexasdxeditor.dto.bsdx.mek.Mek;
import com.giga.nexasdxeditor.dto.bsdx.waz.Waz;
import com.giga.nexasdxeditor.service.impl.BinServiceImpl;
import com.giga.nexasdxeditor.service.impl.PacServiceImpl;
import com.giga.nexasdxeditor.util.TransferUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

@SpringBootTest
class NexasDxEditorApplicationTests {

    private static final Logger log = LoggerFactory.getLogger(NexasDxEditorApplicationTests.class);
    @Autowired
    BinServiceImpl binServiceImpl;
    @Autowired
    PacServiceImpl pacServiceImpl;

    @Test
    void testParseMek() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/*.mek");

        String path = null;
        for (Resource resource : resources) {
            path = resource.getFile().getPath();
        }

        binServiceImpl.parse(path, "Shift-jis");

    }

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
            List<GroupMap> groupMapList = parser.parseGrp(fileBytes, "Shift-JIS");

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

    @Test
    void testDat2Excel() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取 resources 目录下的 .dat 文件
        Resource[] resources = resolver.getResources("classpath*:/game/bh/dat/*.dat");

        String outputExcelPath = "src/main/resources/output_dat.xlsx";

        // 确保输出文件的目录存在
        File outputFile = new File(outputExcelPath);
        outputFile.getParentFile().mkdirs(); // 创建父目录
        if (outputFile.exists()) {
            outputFile.delete(); // 删除旧的空文件
        }

        // 创建 Excel 写入器
        ExcelWriter writer = ExcelUtil.getWriter(outputFile);

        // 遍历所有的 .dat 文件
        for (Resource resource : resources) {
            String filePath = resource.getFile().getPath();
            String sheetName = resource.getFilename().replace(".dat", "");  // 去掉扩展名作为 Sheet 名

            // 读取并解析 .dat 文件
            byte[] fileBytes = Files.readAllBytes(resource.getFile().toPath());
            Dat dat = DatParser.parseDat(fileBytes, resource.getFilename());

            // 准备 Excel 数据
            List<Map<String, Object>> excelData = new ArrayList<>();
            for (List<Object> rowData : dat.getData()) {
                Map<String, Object> rowMap = new LinkedHashMap<>();
                for (int i = 0; i < dat.getColumnTypes().size(); i++) {
                    String columnName = dat.getColumnTypes().get(i) + "_" + (i + 1);

                    // 防止数据越界
                    if (i < rowData.size()) {
                        rowMap.put(columnName, rowData.get(i));
                    } else {
                        rowMap.put(columnName, "");
                    }
                }
                excelData.add(rowMap);
            }

            // 切换到当前 Sheet 并写入数据
            writer.setSheet(sheetName);
            writer.write(excelData, true);  // 写入标题行

            // 自动调整列宽
            for (int columnIndex = 0; columnIndex < dat.getColumnTypes().size(); columnIndex++) {
                writer.getSheet().autoSizeColumn(columnIndex);
            }
        }

        // 关闭写入器
        writer.close();
        log.info("Excel 文件生成成功，路径：" + outputFile.getAbsolutePath());
    }


    @Test
    void testMekInfo2CSV() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/mek/*.mek");

        String path = null;
        String outputJsonPath = "src/main/resources/game/bsdx/mek/output.json";
        List<String> jsonList = new ArrayList<>();
        for (Resource resource : resources) {
            path = resource.getFile().getPath();
            ResponseDTO parse = binServiceImpl.parse(path, "Shift-jis");
            Mek mek = (Mek) parse.getData();
            String filename = resource.getFilename();
            mek.setFileName(filename);
            String jsonPrettyStr = JSONUtil.toJsonPrettyStr(mek.getMekBasicInfo())+",";
            jsonList.add(jsonPrettyStr);
        }

        // 将 jsonList 写入到 output.json
        Path outputPath = Paths.get(outputJsonPath);
        Files.createDirectories(outputPath.getParent()); // 确保目标目录存在
        Files.write(outputPath, jsonList, Charset.forName("UTF-8"), StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);

    }

    @Test
    void testMekInfo2Excel() throws IOException, IllegalAccessException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取 resources 目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/mek/*.mek");

        String path = null;
        String outputExcelPath = "src/main/resources/output.xlsx";
        List<Map<String, Object>> excelData = new ArrayList<>();

        for (Resource resource : resources) {
            path = resource.getFile().getPath();
            ResponseDTO parse = binServiceImpl.parse(path, "Shift-jis");
            Mek mek = (Mek) parse.getData();
            String filename = resource.getFilename();
            mek.setFileName(filename);
            Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();

            Map<String, Object> row = new LinkedHashMap<>();
            row.put("fileName", filename);
            Field[] fields = Mek.MekBasicInfo.class.getDeclaredFields();
            for (Field field : fields) {
                field.setAccessible(true);
                row.put(field.getName(), field.get(mekBasicInfo));
            }
            excelData.add(row);
        }

        // 确保输出文件的目录存在
        File outputFile = new File(outputExcelPath);
        outputFile.getParentFile().mkdirs(); // 创建父目录
        if (outputFile.exists()) {
            outputFile.delete(); // 删除旧的空文件
        }

        // 写入 Excel 数据
        ExcelWriter writer = ExcelUtil.getWriter(outputFile);
        writer.write(excelData, true); // 写入标题行
        writer.close();

        log.info("Excel 文件生成成功，路径：" + outputFile.getAbsolutePath());
    }

    @Test
    void testParseWaz() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/waz/*.waz");

        String path = null;
        List<Waz> allWaz = new ArrayList<>();
        for (Resource resource : resources) {
            path = resource.getFile().getPath();
            ResponseDTO parse = null;
            try {
                parse = binServiceImpl.parse(path, "Shift-jis");
            } catch (Exception e) {
                continue;
            }
            Waz waz = (Waz) parse.getData();
            allWaz.add(waz);
        }
        log.info("allWaz.size ===  {} ", allWaz.size());


    }

    @Test
    void testGenerateWazJsonFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/waz/*.waz");

        List<Waz> allWazList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>(); // 存储文件名（不含扩展名）

        for (Resource resource : resources) {
            String path = resource.getFile().getPath();
            String fileName = resource.getFilename(); // 获取文件名，如 "example.waz"
            String baseName = fileName.substring(0, fileName.lastIndexOf('.')); // 去除扩展名
            baseNames.add(baseName);

            try {
                ResponseDTO parse = binServiceImpl.parse(path, "Shift-jis");
                Waz waz = (Waz) parse.getData();
                allWazList.add(waz);
            } catch (Exception e) {
                continue;
            }
        }

        log.info("allWazList.size ===  {} ", allWazList.size());

        // 输出JSON文件到指定目录
        String outputDir = "D:\\A\\json";
        FileUtil.mkdir(outputDir); // 创建目录（如果不存在）

        for (int i = 0; i < allWazList.size(); i++) {
            Waz waz = allWazList.get(i);
            String jsonStr = JSONUtil.toJsonStr(waz);
            String filePath = FileUtil.file(outputDir, baseNames.get(i) + ".txt").getAbsolutePath();
            FileUtil.writeUtf8String(jsonStr, filePath);
            log.info("transfer === {} ", baseNames.get(i));
        }
    }

    @Test
    void parseJson2Waz() throws IOException {
        String inputFilePath = "D:\\A\\json\\Kou.json";
        String jsonStr1 = FileUtil.readUtf8String(new File(inputFilePath));
        Waz bean = JSONUtil.toBean(jsonStr1, Waz.class);
        log.info("bean");

        String JsonStr2 = JSONUtil.toJsonStr(bean);
        JSONObject jsonObject1 = JSONUtil.parseObj(bean);
        JSONObject jsonObject2 = JSONUtil.parseObj(JsonStr2);
        boolean isEqual = jsonObject1.equals(jsonObject2);
        log.info("result: {}", isEqual ? "✅" : "❌");
        log.info("over");

    }


    @Test
    void testGenerate() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/*.mek");

        String path = null;
        for (Resource resource : resources) {
            path = resource.getFile().getPath();
            break;
        }

        Mek mek = (Mek) binServiceImpl.parse(path, "Shift-JIS").getData();
        binServiceImpl.generate(path, mek, "Shift-JIS");

    }

    @Test
    void testCreateTestWazDat() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/Update3/*.waz");

        if (resources.length > 0) {
            byte[] firstFileContent = FileUtil.readBytes(resources[0].getFile());

            for (int i = 1; i < resources.length; i++) {
                File file = resources[i].getFile();
                FileUtil.writeBytes(firstFileContent, file);
            }
        }

    }



    @Test
    void testUnpac() throws Exception {
        // 获取 resources 目录下的 .pac 文件
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/*.pac");

        // 遍历所有的 .pac 文件
        for (Resource resource : resources) {
            String filePath = resource.getFile().getPath();
            String pacFileName = resource.getFilename().replace(".pac", "");  // 去掉扩展名作为文件夹名

            // 生成文件夹路径，文件夹名即为 pac 文件的名称
            String outputFolderPath = "D:\\Code\\java\\NeXAS_DX\\backend\\src\\main\\resources\\bsdx\\" + pacFileName;

            // 确保输出文件夹存在
            File outputFolder = new File(outputFolderPath);
            if (!outputFolder.exists()) {
                outputFolder.mkdirs(); // 创建文件夹
            }

            // 解压该 .pac 文件到对应的文件夹
            log.info("开始解压文件: " + pacFileName);
            ResponseDTO responseDTO = pacServiceImpl.unPac(filePath);
            log.info("解压输出: \n{}", responseDTO);
        }
    }

    @Test
    void testPac() throws Exception {
        ResponseDTO responseDTO = pacServiceImpl.pac(
                "D:\\Code\\java\\NeXAS_DX\\backend\\src\\main\\resources\\"+"Update3");
        log.info("\npack output: \n{}", responseDTO);
    }

    @Test
    void transJA2ZH() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources1 = resolver.getResources("classpath*:/*.zh.mek");
        Resource[] resources2 = resolver.getResources("classpath*:/*.ja.mek");
        String path1 = null;
        for (Resource resource : resources1) {
            path1 = resource.getFile().getPath();
            break;
        }
        String path2 = null;
        for (Resource resource : resources2) {
            path2 = resource.getFile().getPath();
            break;
        }

        Mek zhMek = (Mek) binServiceImpl.parse(path1, "GBK").getData();
        Mek jaMek = (Mek) binServiceImpl.parse(path2, "Shift-JIS").getData();

        TransferUtil.transJA2ZH(jaMek, zhMek);

        binServiceImpl.generate(path1, jaMek, "GBK");
    }


}

