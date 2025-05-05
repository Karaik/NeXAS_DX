package com.giga.nexas;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.service.BinService;
import com.giga.nexas.util.TransferUtil;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

public class TestMek {

    private final BinService binService = new BinService();

    private static final Path MEK_DIR = Paths.get("src/main/resources/game/bsdx/mek");
    private static final Path JSON_OUTPUT = Paths.get("src/main/resources/mekJson");

    @Test
    void testParseMek() throws IOException {
        List<Mek> allMek = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                try {
                    ResponseDTO dto = binService.parse(path.toString(), "windows-31j");
                    allMek.add((Mek) dto.getData());
                } catch (Exception ignored) {
                }
            }
        }

        System.out.println("✅ Parsed mek count: " + allMek.size());
    }

    @Test
    void testGenerateMekJsonFiles() throws IOException {
        Files.createDirectories(JSON_OUTPUT);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                try {
                    ResponseDTO dto = binService.parse(path.toString(), "windows-31j");
                    Mek mek = (Mek) dto.getData();

                    String baseName = path.getFileName().toString().replace(".mek", "");
                    Path jsonPath = JSON_OUTPUT.resolve(baseName + ".json");
                    FileUtil.writeUtf8String(JSONUtil.toJsonStr(mek), jsonPath.toFile());

                    System.out.println("✅ Exported: " + jsonPath);
                } catch (Exception e) {
                    System.err.println("❌ Failed to parse: " + path.getFileName());
                }
            }
        }
    }

    @Test
    void transJA2ZH() throws IOException {
        Path zhFile = null, jaFile = null;

        try (DirectoryStream<Path> zh = Files.newDirectoryStream(MEK_DIR, "*.zh.mek")) {
            for (Path path : zh) {
                zhFile = path;
                break;
            }
        }

        try (DirectoryStream<Path> ja = Files.newDirectoryStream(MEK_DIR, "*.ja.mek")) {
            for (Path path : ja) {
                jaFile = path;
                break;
            }
        }

        if (zhFile == null || jaFile == null) {
            System.err.println("❌ 找不到 .zh.mek 或 .ja.mek 文件");
            return;
        }

        Mek zhMek = (Mek) binService.parse(zhFile.toString(), "GBK").getData();
        Mek jaMek = (Mek) binService.parse(jaFile.toString(), "windows-31j").getData();

        TransferUtil.transJA2ZH(jaMek, zhMek);
        binService.generate(zhFile.toString(), jaMek, "GBK");

        System.out.println("✅ 已用日语内容替换中文文件：" + zhFile);
    }

    @Test
    void testGenerateMek() throws IOException {
        Path target = null;

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                target = path;
                break;
            }
        }

        if (target == null) {
            System.err.println("❌ 未找到任何 .mek 文件");
            return;
        }

        Mek mek = (Mek) binService.parse(target.toString(), "windows-31j").getData();
        binService.generate(target.toString(), mek, "windows-31j");

        System.out.println("✅ 再生成成功: " + target);
    }
}
