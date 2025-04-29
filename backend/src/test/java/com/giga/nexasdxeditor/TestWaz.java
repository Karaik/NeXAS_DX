package com.giga.nexasdxeditor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.waz.Waz;
import com.giga.nexasdxeditor.service.impl.BinServiceImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class TestWaz {

    Logger log = LoggerFactory.getLogger(TestWaz.class);

    @Autowired
    BinServiceImpl binServiceImpl;

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
                ResponseDTO parse = binServiceImpl.parse(path, "x-SJIS");
                Waz waz = (Waz) parse.getData();
                allWazList.add(waz);
            } catch (Exception e) {
                continue;
            }
        }

        log.info("allWazList.size ===  {} ", allWazList.size());

        // 输出JSON文件到指定目录
        String outputDir = "src/main/resources/wazJson";
        FileUtil.mkdir(outputDir); // 创建目录（如果不存在）

        for (int i = 0; i < allWazList.size(); i++) {
            Waz waz = allWazList.get(i);
            String jsonStr = JSONUtil.toJsonStr(waz);
            String filePath = FileUtil.file(outputDir, baseNames.get(i) + ".json").getAbsolutePath();
            FileUtil.writeUtf8String(jsonStr, filePath);
            log.info("transfer === {} ", baseNames.get(i));
        }
    }

    @Test
    void testGenerateWazFilesByJson() throws IOException {
        // 读取之前生成的 JSON 文件
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/wazJson/*.json");

        String outputDir = "src/main/resources/wazGenerated";  // 输出 .waz 文件路径
        FileUtil.mkdir(outputDir);  // 创建输出目录

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        for (Resource resource : resources) {
            String jsonStr = FileUtil.readUtf8String(resource.getFile());
            JSONObject jsonObj = JSONUtil.parseObj(jsonStr);

            // 反序列化成 Waz 对象
            Waz waz = objectMapper.readValue(jsonStr, Waz.class);
            String baseName = resource.getFilename().replace(".json", "");
            String outputPath = FileUtil.file(outputDir, baseName + ".waz").getAbsolutePath();

            // 调用生成器
            binServiceImpl.generate(outputPath, waz, "x-SJIS");
            log.info("generated === {}", baseName);
        }
    }

    @Test
    void testWazParseGenerateBinaryConsistency() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/waz/*.waz");

        for (Resource resource : resources) {
            String originalPath = resource.getFile().getPath();
            String fileName = resource.getFilename();
            log.info("checking === {}", fileName);

            // 解析.waz
            ResponseDTO<?> dto = binServiceImpl.parse(originalPath, "x-SJIS");
            Waz waz = (Waz) dto.getData();

            // 生成.waz
            String tempPath = "src/main/resources/temp/" + fileName;
            FileUtil.mkdir("src/main/resources/temp/");
            binServiceImpl.generate(tempPath, waz, "x-SJIS");

            // 读取为byte[]
            byte[] originalBytes = FileUtil.readBytes(originalPath);
            byte[] generatedBytes = FileUtil.readBytes(tempPath);

            // 校验二进制是否一致
            boolean isEqual = ArrayUtil.equals(originalBytes, generatedBytes);
            if (!isEqual) {
                log.error("二进制不一致: {}", fileName);
                log.error("原始文件大小: {}, 生成文件大小: {}", originalBytes.length, generatedBytes.length);
            }

        }
    }

}
