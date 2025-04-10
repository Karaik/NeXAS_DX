package com.giga.nexasdxeditor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@SpringBootTest
public class TestWaz {

    Logger log = LoggerFactory.getLogger(TestWaz.class);

    @Autowired
    BinServiceImpl binServiceImpl;

    @Test
    void parseJson2Waz() throws IOException {
        String inputFilePath = "src/main/resources/wazJson/Kou.json";
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

}
