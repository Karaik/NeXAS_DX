package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TestSpm {

    private static final Logger log = LoggerFactory.getLogger(TestSpm.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    private static final Path GAME_SPM_DIR = Paths.get("src/main/resources/game/bsdx/spm");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/spmJson");
    private static final Path SPM_OUTPUT_DIR = Paths.get("src/main/resources/spmGenerated");

    @Test
    void testGenerateSpmJsonFiles() throws IOException {
        List<Spm> allSpmList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_SPM_DIR, "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    Spm spm = (Spm) dto.getData();
                    allSpmList.add(spm);
                } catch (Exception e) {
                    log.warn("Failed to parse: {}", fileName, e);
                }
            }
        }

        for (int i = 0; i < allSpmList.size(); i++) {
            Spm spm = allSpmList.get(i);
            String jsonStr = JSONUtil.toJsonStr(spm);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".spm.json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("Exported: {}", jsonPath);
        }
    }

    @Test
    void testGenerateSpmFilesByJson() throws IOException {
        Files.createDirectories(SPM_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String jsonStr = FileUtil.readUtf8String(path.toFile());
                Spm spm = mapper.readValue(jsonStr, Spm.class);
                String baseName = path.getFileName().toString().replace(".json", "");
                Path output = SPM_OUTPUT_DIR.resolve(baseName + ".generated.spm");

                bsdxBinService.generate(output.toString(), spm, "windows-31j");
                log.info("Generated: {}", output);
            }
        }
    }

    @Test
    void testSpmParseGenerateBinaryConsistency() throws IOException {
        Map<String, Path> generatedMap = new HashMap<>();
        try (DirectoryStream<Path> genStream = Files.newDirectoryStream(SPM_OUTPUT_DIR, "*.generated.spm")) {
            for (Path gen : genStream) {
                generatedMap.put(gen.getFileName().toString(), gen);
            }
        }

        Path mismatchDir = SPM_OUTPUT_DIR.resolve("mismatch");
        Files.createDirectories(mismatchDir); // 确保 mismatch 文件夹存在

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(GAME_SPM_DIR, "*.spm")) {
            for (Path ori : oriStream) {
                String name = ori.getFileName().toString().replace(".spm", ".generated.spm");
                Path gen = generatedMap.get(name);
                if (gen == null) {
                    log.warn("Not Found: {}", name);
                    continue;
                }

                byte[] originalBytes = FileUtil.readBytes(ori.toFile());
                byte[] generatedBytes = FileUtil.readBytes(gen.toFile());

                if (!ArrayUtil.equals(originalBytes, generatedBytes)) {
                    log.error("Mismatch: {}", name);
                    int minLen = Math.min(originalBytes.length, generatedBytes.length);
                    for (int i = 0; i < minLen; i++) {
                        if (originalBytes[i] != generatedBytes[i]) {
                            log.error("Diff at 0x{}: orig=0x{} gen=0x{}",
                                    Integer.toHexString(i),
                                    Integer.toHexString(originalBytes[i] & 0xFF),
                                    Integer.toHexString(generatedBytes[i] & 0xFF));
                            break;
                        }
                    }

                    // 移动 mismatch 文件
                    String newName = gen.getFileName().toString().replace(".generated", "");
                    Path target = mismatchDir.resolve(newName);
                    Files.move(gen, target, StandardCopyOption.REPLACE_EXISTING);
                    log.warn("Moved mismatch file to: {}", target);

                } else {
//                log.info("Match: {}", name);
                }
            }
        }
    }

}
