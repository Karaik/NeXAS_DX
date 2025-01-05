package com.giga.nexasdxeditor;

import cn.hutool.core.util.StrUtil;
import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.service.impl.BinServiceImpl;
import com.giga.nexasdxeditor.service.impl.PacServiceImpl;
import com.giga.nexasdxeditor.util.PacUtil;
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
    void testWazAndMek() throws IOException {
        PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
        // 获取resources目录下的以.waz和.mek结尾的文件
        Resource[] resourcesWaz = resolver.getResources("classpath*:/*.waz");
        Resource[] resourcesMek = resolver.getResources("classpath*:/*.mek");

        // 打印文件路径
        MultipartFile wazMultipartFile = null;
        for (Resource resource : resourcesWaz) {
            wazMultipartFile = convertToMultipartFile(resource);
            System.out.println("Found .waz file: " + resource.getURI());
        }
        MultipartFile mekMultipartFile = null;
        for (Resource resource : resourcesMek) {
            mekMultipartFile = convertToMultipartFile(resource);
            System.out.println("Found .mek file: " + resource.getURI());
        }

        binServiceImpl.convertBin2Hex(wazMultipartFile, mekMultipartFile);

    }

    @Test
    void testUnpac() throws Exception {
        String filename = this.getPacName();
        ResponseDTO responseDTO = pacServiceImpl.unPac(
                "D:\\A\\NeXAS_DX\\backend\\src\\main\\resources\\" + filename + ".pac");
        log.info("\nunpack output: \n{}", responseDTO);

    }
    @Test
    void testPac() throws Exception {
//        String filename = this.getPacName();
        ResponseDTO responseDTO = pacServiceImpl.pac(
                "D:\\A\\NeXAS_DX\\backend\\src\\main\\resources\\Update3");
        log.info("\npack output: \n{}", responseDTO);
    }

    @Test
    void test1() {
        String text = "前方に宙返りを行い、連続押すと最大3回行うことができます。ダメージはありませんが、ブースターがあれば空中移動に回れます。";
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
