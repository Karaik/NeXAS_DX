package com.giga.nexasdxeditor;

import cn.hutool.core.util.StrUtil;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.Mek;
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

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

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
    void testParseWaz() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的文件
        Resource[] resources = resolver.getResources("classpath*:/*.waz");

        String path = null;
        for (Resource resource : resources) {
            path = resource.getFile().getPath();
        }

        binServiceImpl.parse(path, "Shift-JIS");

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
    void testUnpac() throws Exception {
        String filename = this.getPacName();
        ResponseDTO responseDTO = pacServiceImpl.unPac(
                "D:\\Code\\java\\NeXAS_DX\\backend\\src\\main\\resources\\" + filename + ".pac");
        log.info("\nunpack output: \n{}", responseDTO);

    }
    @Test
    void testPac() throws Exception {
        ResponseDTO responseDTO = pacServiceImpl.pac(
                "D:\\Code\\java\\NeXAS_DX\\backend\\src\\main\\resources\\"+"test");
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

    @Test
    void test1() {
        String text = "機動力上昇し、長時間にわたって移動速度を上げる。攻撃距離に影響なし、効果時間は長いし、長時間作戦に対してはとても有利な技です。";
        try {
            // 使用Shift-JIS编码将字符串转换为字节数组
            byte[] bytes = text.getBytes(Charset.forName("Shift-JIS"));
            // 转换为十六进制字符串
            StringBuilder hexBuilder = new StringBuilder();
            for (byte b : bytes) {
                hexBuilder.append(String.format("%02X ", b)); // 大写十六进制，带空格分隔
            }
            log.info(hexBuilder.toString().trim());
        } catch (Exception e) {
            System.err.println("编码转换失败: " + e.getMessage());
        }
    }

    private String getPacName() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        Resource[] resourcesArray = resolver.getResources("classpath*:*.pac");
        Resource resource = resourcesArray[0];
        String filename = StrUtil.removeSuffix(resource.getFilename(), ".pac");
        return filename;
    }
    // 将Resource转换为MultipartFile
    private MultipartFile convertToMultipartFile(Resource resource) throws IOException {
        // 读取文件内容
        byte[] content = Files.readAllBytes(resource.getFile().toPath());

        // 使用MockMultipartFile包装文件
        return new MockMultipartFile(
                resource.getFilename(),      // 文件名
                resource.getFilename(),      // 原始文件名
                "application/octet-stream",  // 文件类型
                content                      // 文件内容
        );
    }

}
