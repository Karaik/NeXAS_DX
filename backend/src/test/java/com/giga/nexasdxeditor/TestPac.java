package com.giga.nexasdxeditor;

import com.giga.nexasdxeditor.dto.ResponseDTO;
import com.giga.nexasdxeditor.service.impl.PacServiceImpl;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import java.io.File;

@SpringBootTest
public class TestPac {
    Logger log = LoggerFactory.getLogger(TestPac.class);

    
    PacServiceImpl pacServiceImpl;

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
            String outputFolderPath = "D:\\Code\\java\\NeXAS_DX\\backend\\src\\main\\resources\\" + pacFileName;

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

}
