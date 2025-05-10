//package com.giga.nexas;
//
//import com.giga.nexas.dto.ResponseDTO;
//import com.giga.nexas.service.PacService;
//import org.junit.jupiter.api.Test;
//
//import java.nio.file.*;
//
//public class TestPac {
//
//    private final PacService pacService = new PacService();
//
//    private static final Path RESOURCE_DIR = Paths.get("src/main/resources");
//
//    @Test
//    void testUnpac() throws Exception {
//        try (DirectoryStream<Path> stream = Files.newDirectoryStream(RESOURCE_DIR, "*.pac")) {
//            for (Path pacPath : stream) {
//                String fileName = pacPath.getFileName().toString();
//                String pacFileName = fileName.replace(".pac", "");
//                Path outputDir = RESOURCE_DIR.resolve(pacFileName);
//
//                Files.createDirectories(outputDir);
//                System.out.println("📦 解压: " + fileName);
//
//                ResponseDTO result = pacService.unPac(pacPath.toString());
//                System.out.println("✅ 解压完成: " + result);
//            }
//        }
//    }
//
//    @Test
//    void testPac() throws Exception {
//        Path inputDir = RESOURCE_DIR.resolve("Update3");
//
//        if (!Files.isDirectory(inputDir)) {
//            System.err.println("❌ 输入目录不存在: " + inputDir);
//            return;
//        }
//
//        ResponseDTO result = pacService.pac(inputDir.toString());
//        System.out.println("📦 打包结果: " + result);
//    }
//}
