package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;

public class TestWaz {

    private static final Logger log = LoggerFactory.getLogger(TestWaz.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    private static final Path GAME_WAZ_DIR = Paths.get("src/main/resources/game/bsdx/waz");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/wazJson");
    private static final Path WAZ_OUTPUT_DIR = Paths.get("src/main/resources/wazGenerated");

    @Test
    void testGenerateWazJsonFiles() throws IOException {
        List<Waz> allWazList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_WAZ_DIR, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    Waz waz = (Waz) dto.getData();
                    allWazList.add(waz);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse: {}", fileName, e);
                }
            }
        }

        for (int i = 0; i < allWazList.size(); i++) {
            Waz waz = allWazList.get(i);
            String jsonStr = JSONUtil.toJsonStr(waz);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".waz.json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("✅ Exported: {}", jsonPath);
        }
    }

    @Test
    void testGenerateWazFilesByJson() throws IOException {
        Files.createDirectories(WAZ_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String jsonStr = FileUtil.readUtf8String(path.toFile());
                Waz waz = mapper.readValue(jsonStr, Waz.class);
                String baseName = path.getFileName().toString().replace(".json", "");
                Path output = WAZ_OUTPUT_DIR.resolve(baseName + ".waz");

                bsdxBinService.generate(output.toString(), waz, "windows-31j");
                log.info("✅ Generated: {}", output);
            }
        }
    }

    @Test
    void testWazParseGenerateBinaryConsistency() throws IOException {
        Map<String, Path> generatedMap = new HashMap<>();
        try (DirectoryStream<Path> genStream = Files.newDirectoryStream(WAZ_OUTPUT_DIR, "*.waz")) {
            for (Path gen : genStream) {
                generatedMap.put(gen.getFileName().toString(), gen);
            }
        }

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(GAME_WAZ_DIR, "*.waz")) {
            for (Path ori : oriStream) {
                String name = ori.getFileName().toString();
                Path gen = generatedMap.get(name);
                if (gen == null) {
                    log.warn("⚠️ Not Found: {}", name);
                    continue;
                }

                byte[] originalBytes = FileUtil.readBytes(ori.toFile());
                byte[] generatedBytes = FileUtil.readBytes(gen.toFile());

                if (!ArrayUtil.equals(originalBytes, generatedBytes)) {
                    log.error("❌ Mismatch: {}", name);
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
                } else {
                    log.info("✅ Match: {}", name);
                }
            }
        }
    }
}
