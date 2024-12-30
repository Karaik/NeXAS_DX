package com.giga.nexasdxeditor;

import com.giga.nexasdxeditor.demos.web.service.impl.BinServiceImpl;
import org.junit.jupiter.api.Test;
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

    @Autowired
    BinServiceImpl binServiceImpl;

    @Test
    void contextLoads() throws IOException {
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
