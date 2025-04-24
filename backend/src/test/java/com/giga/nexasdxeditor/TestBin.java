package com.giga.nexasdxeditor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.bin.Bin;
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
public class TestBin {
    Logger log = LoggerFactory.getLogger(TestBin.class);

    @Autowired
    BinServiceImpl binServiceImpl;
    
    @Test
    void testGenerateBinJsonFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/bin/*.bin");

        List<Bin> allBinList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>(); // 存储文件名（不含扩展名）

        for (Resource resource : resources) {
            String path = resource.getFile().getPath();
            String fileName = java.net.URLDecoder.decode(
                    resource.getFilename(), java.nio.charset.StandardCharsets.UTF_8); // 获取文件名
            String baseName = fileName.substring(0, fileName.lastIndexOf('.')); // 去除扩展名
            baseNames.add(baseName);

            try {
                ResponseDTO parse = binServiceImpl.parse(path, "Shift-jis");
                Bin bin = (Bin) parse.getData();
                allBinList.add(bin);
            } catch (Exception e) {
                continue;
            }
        }

        log.info("allBinList.size ===  {} ", allBinList.size());

        // 输出JSON文件到指定目录
        String outputDir = "src/main/resources/binJson";
        FileUtil.mkdir(outputDir); // 创建目录（如果不存在）

        for (int i = 0; i < allBinList.size(); i++) {
            Bin bin = allBinList.get(i);
            String jsonStr = JSONUtil.toJsonStr(bin);
            if (jsonStr==null) {
                continue;
            }
            String filePath = FileUtil.file(outputDir, baseNames.get(i) + ".json").getAbsolutePath();
            FileUtil.writeUtf8String(jsonStr, filePath);
            log.info("transfer === {} ", baseNames.get(i));
        }
    }
}
