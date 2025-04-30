package com.giga.nexasdxeditor;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.mek.Mek;
import com.giga.nexasdxeditor.service.impl.BinServiceImpl;
import com.giga.nexasdxeditor.util.TransferUtil;
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
public class TestMek {
    Logger log = LoggerFactory.getLogger(TestMek.class);

    
    BinServiceImpl binServiceImpl = new BinServiceImpl();

    @Test
    void testParseMek() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/mek/*.mek");

        String path = null;
        List<Mek> allMek = new ArrayList<>();
        for (Resource resource : resources) {
            path = resource.getFile().getPath();
            ResponseDTO parse = null;
            try {
                parse = binServiceImpl.parse(path, "windows-31j");
            } catch (Exception e) {
                continue;
            }
            Mek mek = (Mek) parse.getData();
            allMek.add(mek);
        }
        log.info("allMek.size ===  {} ", allMek.size());

    }

    @Test
    void testGenerateMekJsonFiles() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resources = resolver.getResources("classpath*:/game/bsdx/mek/*.mek");

        List<Mek> allMekList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>(); // 存储文件名（不含扩展名）

        for (Resource resource : resources) {
            String path = resource.getFile().getPath();
            String fileName = resource.getFilename(); // 获取文件名，如 "example.mek"
            String baseName = fileName.substring(0, fileName.lastIndexOf('.')); // 去除扩展名
            baseNames.add(baseName);

            try {
                ResponseDTO parse = binServiceImpl.parse(path, "windows-31j");
                Mek mek = (Mek) parse.getData();
                allMekList.add(mek);
            } catch (Exception e) {
                continue;
            }
        }

        log.info("allMekList.size ===  {} ", allMekList.size());

        // 输出JSON文件到指定目录
        String outputDir = "src/main/resources/mekJson";
        FileUtil.mkdir(outputDir); // 创建目录（如果不存在）

        for (int i = 0; i < allMekList.size(); i++) {
            Mek mek = allMekList.get(i);
            String jsonStr = JSONUtil.toJsonStr(mek);
            String filePath = FileUtil.file(outputDir, baseNames.get(i) + ".json").getAbsolutePath();
            FileUtil.writeUtf8String(jsonStr, filePath);
            log.info("transfer === {} ", baseNames.get(i));
        }
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
        Mek jaMek = (Mek) binServiceImpl.parse(path2, "windows-31j").getData();

        TransferUtil.transJA2ZH(jaMek, zhMek);

        binServiceImpl.generate(path1, jaMek, "GBK");
    }

    @Test
    void testGenerateMek() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/*.mek");

        String path = null;
        for (Resource resource : resources) {
            path = resource.getFile().getPath();
            break;
        }

        Mek mek = (Mek) binServiceImpl.parse(path, "windows-31j").getData();
        binServiceImpl.generate(path, mek, "windows-31j");

    }

}
