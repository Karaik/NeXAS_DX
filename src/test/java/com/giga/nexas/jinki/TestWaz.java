package com.giga.nexas.jinki;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestMethodOrder(OrderAnnotation.class)
@Execution(ExecutionMode.SAME_THREAD)
public class TestWaz {

    private static final Logger log = LoggerFactory.getLogger(TestWaz.class);
    private static final Path GAME_WAZ_DIR = Paths.get("src/main/resources/game/jinki/waz");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/wazJinkiJson");
    private static final Path WAZ_OUTPUT_DIR = Paths.get("src/main/resources/wazJinkiGenerated");
    private static final String CHARSET = "windows-31j";
    private static final String WAZ_EXT = ".waz";
    private static final String GENERATED_SUFFIX = ".generated";

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    private static void ensureSourceDirExists() {
        if (!Files.exists(GAME_WAZ_DIR) || !Files.isDirectory(GAME_WAZ_DIR)) {
            throw new IllegalStateException("Jinki resource directory not found: " + GAME_WAZ_DIR);
        }
    }

    @Test
    @Order(1)
    void testGenerateWazJsonFiles() throws IOException {
        ensureSourceDirExists();

        List<Waz> allWazList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_WAZ_DIR, "*" + WAZ_EXT)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                    Waz waz = (Waz) dto.getData();
                    allWazList.add(waz);
                } catch (Exception e) {
                    log.warn("Failed to parse waz: {}", fileName, e);
                    throw e;
                }
            }
        }

        for (int i = 0; i < allWazList.size(); i++) {
            Waz waz = allWazList.get(i);
            String jsonStr = JSONUtil.toJsonStr(waz);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".waz.json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("Exported waz json: {}", jsonPath);
        }
    }

    @Test
    @Order(2)
    void testGenerateWazFilesByJson() throws IOException {
        Files.createDirectories(WAZ_OUTPUT_DIR);

        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.endsWith(".waz.json")
                        ? fileName.substring(0, fileName.length() - ".waz.json".length())
                        : fileName.substring(0, fileName.lastIndexOf('.'));

                String jsonStr = FileUtil.readUtf8String(path.toFile());
                Waz waz = mapper.readValue(jsonStr, Waz.class);

                Path output = WAZ_OUTPUT_DIR.resolve(baseName + WAZ_EXT + GENERATED_SUFFIX);
                bsdxBinService.generate(output.toString(), waz, CHARSET);
                log.info("Generated waz: {}", output);
            }
        }
    }

    @Test
    @Order(3)
    void testWazParseGenerateBinaryConsistency() throws IOException {
        ensureSourceDirExists();

        Map<String, Path> generatedMap = new HashMap<>();
        try (DirectoryStream<Path> genStream = Files.newDirectoryStream(WAZ_OUTPUT_DIR, "*" + WAZ_EXT + GENERATED_SUFFIX)) {
            for (Path gen : genStream) {
                String genName = gen.getFileName().toString();
                String originalName = genName.replace(GENERATED_SUFFIX, "");
                generatedMap.put(originalName, gen);
            }
        }

        boolean anyIssue = false;
        int compared = 0;

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(GAME_WAZ_DIR, "*" + WAZ_EXT)) {
            for (Path ori : oriStream) {
                String name = ori.getFileName().toString();
                Path gen = generatedMap.get(name);
                if (gen == null) {
                    anyIssue = true;
                    log.error("Generated waz not found for original: {}", name);
                    continue;
                }

                byte[] originalBytes = FileUtil.readBytes(ori.toFile());
                byte[] generatedBytes = FileUtil.readBytes(gen.toFile());
                compared++;

                if (!ArrayUtil.equals(originalBytes, generatedBytes)) {
                    anyIssue = true;
                    log.error("Mismatch waz: {}", name);
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
                    log.info("Match waz: {}", name);
                }
            }
        }

        if (!anyIssue && compared > 0) {
            if (Files.exists(JSON_OUTPUT_DIR)) {
                FileUtil.del(JSON_OUTPUT_DIR.toFile());
            }
            if (Files.exists(WAZ_OUTPUT_DIR)) {
                FileUtil.del(WAZ_OUTPUT_DIR.toFile());
            }
        } else if (anyIssue) {
            Assertions.fail("Mismatch or missing generated waz files detected. Outputs retained for inspection.");
        } else {
            Assertions.fail("No waz files compared. Check inputs/outputs before cleanup.");
        }
    }
}
