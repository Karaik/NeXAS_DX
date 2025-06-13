package com.giga.nexas.bhe;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.mek.Mek;
import com.giga.nexas.service.BheBinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestMek {

    private static final Logger log = LoggerFactory.getLogger(TestMek.class);

    private final BheBinService bheBinService = new BheBinService();

    private static final Path MEK_DIR = Paths.get("src/main/resources/game/bhe/mek");
    private static final Path JSON_OUTPUT = Paths.get("src/main/resources/mekBheJson");

    @Test
    void testParseMek() throws IOException {
        List<Mek> allMek = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                try {
                    ResponseDTO dto = bheBinService.parse(path.toString(), "windows-31j");
                    allMek.add((Mek) dto.getData());
                } catch (Exception ignored) {
                    continue;
                }
            }
        }

        log.info("✅ Parsed mek count: {}", allMek.size());
    }

    @Test
    void testGenerateMekJsonFiles() throws IOException {
        Files.createDirectories(JSON_OUTPUT);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                try {
                    ResponseDTO dto = bheBinService.parse(path.toString(), "windows-31j");
                    Mek mek = (Mek) dto.getData();

                    String baseName = path.getFileName().toString().replace(".mek", "");
                    Path jsonPath = JSON_OUTPUT.resolve(baseName + ".mek.json");
                    FileUtil.writeUtf8String(JSONUtil.toJsonStr(mek), jsonPath.toFile());

                    log.info("✅ Exported: {}", jsonPath);
                } catch (Exception e) {
                    log.info("❌ Failed to parse: {}", path.getFileName());
                }
            }
        }
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
            log.error("❌ 未找到任何 .mek 文件");
            return;
        }

        Mek mek = (Mek) bheBinService.parse(target.toString(), "windows-31j").getData();
        bheBinService.generate(target.toString(), mek, "windows-31j");

        log.info("✅ 再生成成功: {}", target);
    }
}
