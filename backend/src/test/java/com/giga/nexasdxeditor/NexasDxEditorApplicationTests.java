package com.giga.nexasdxeditor;

import cn.hutool.core.util.StrUtil;
import com.giga.nexasdxeditor.demos.web.service.impl.BinServiceImpl;
import com.giga.nexasdxeditor.demos.web.service.impl.PacServiceImpl;
import com.giga.nexasdxeditor.demos.web.util.PacUtil;
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
        String result = PacUtil.unpack(
                "D:\\A\\NeXAS_DX\\backend\\src\\main\\resources\\"+filename+".pac",
                "D:\\A\\NeXAS_DX\\backend\\src\\main\\resources\\"+filename);
        log.info("\nunpack output: \n{}", result);

    }
    @Test
    void testPac() throws Exception {
//        String filename = this.getPacName();
        String result = PacUtil.pack(
                "D:\\A\\NeXAS_DX\\backend\\src\\main\\resources\\Update3");
        log.info("\npack output: \n{}", result);
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
