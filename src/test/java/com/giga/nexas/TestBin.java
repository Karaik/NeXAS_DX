package com.giga.nexas;

import cn.hutool.core.io.FileUtil;
import cn.hutool.json.JSONUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.service.BinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class TestBin {

    private static final Logger log = LoggerFactory.getLogger(TestBin.class);
    private static final Path BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");
    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/binJson");

    private final BinService binService = new BinService();

    @Test
    public void testGenerateBinJsonFiles() throws IOException {
        if (!Files.exists(BIN_DIR)) {
            log.error("❌ Bin 目录不存在: {}", BIN_DIR);
            return;
        }

        Files.createDirectories(OUTPUT_DIR);
        List<Bin> allBinList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BIN_DIR, "*.bin")) {
            for (Path path : stream) {
                String fileName = URLDecoder.decode(path.getFileName().toString(), StandardCharsets.UTF_8);
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO parse = binService.parse(path.toString(), "windows-31j");
                    Bin bin = (Bin) parse.getData();
                    if (bin != null) {
                        allBinList.add(bin);
                    } else {
                        log.info("⚠️ 解析为空: " + fileName);
                    }
                } catch (Exception e) {
                    log.error("❌ 解析失败: {} - {}", fileName, e.getMessage());
                    throw e;
                }
            }
        }

        log.info("✅ bin 文件总数: " + allBinList.size());

        for (int i = 0; i < allBinList.size(); i++) {
            Bin bin = allBinList.get(i);
            String jsonStr = JSONUtil.toJsonStr(bin);
            if (jsonStr == null) continue;

            Path outputPath = OUTPUT_DIR.resolve(baseNames.get(i) + ".json");
            FileUtil.writeUtf8String(jsonStr, outputPath.toFile());
            log.info("✅ 导出 JSON: {}", outputPath);
        }
    }

    @Test
    public void testOutputIR() throws IOException {
        if (!Files.exists(BIN_DIR)) {
            log.error("❌ Bin 目录不存在: {}", BIN_DIR);
            return;
        }

        Files.createDirectories(OUTPUT_DIR);
        List<Bin> allBinList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BIN_DIR, "*.bin")) {
            for (Path path : stream) {
                String fileName = URLDecoder.decode(path.getFileName().toString(), StandardCharsets.UTF_8);
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO parse = binService.parse(path.toString(), "windows-31j");
                    Bin bin = (Bin) parse.getData();
                    if (bin != null) {
                        allBinList.add(bin);
                    } else {
                        log.info("⚠️ 解析为空: " + fileName);
                    }
                } catch (Exception e) {
                    log.error("❌ 解析失败: {} - {}", fileName, e.getMessage());
                }
            }
        }

        log.info("✅ bin 文件总数: " + allBinList.size());

        for (int i = 0; i < allBinList.size(); i++) {
            Bin bin = allBinList.get(i);

            String jsonStr = JSONUtil.toJsonStr(bin);
            if (jsonStr == null) continue;

            Path outputPath = OUTPUT_DIR.resolve(baseNames.get(i) + ".json");
            FileUtil.writeUtf8String(jsonStr, outputPath.toFile());
            log.info("✅ 导出 bin 内的IR: {}", outputPath);
        }
    }
}
