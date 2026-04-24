package com.giga.nexas.clarias;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.clarias.waz.Waz;
import com.giga.nexas.service.ClariasBinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
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
    private final ClariasBinService clariasBinService = new ClariasBinService();

    private static final Path GAME_WAZ_DIR = Paths.get("src/main/resources/game/clarias/waz");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/wazClariasJson");
    private static final Path WAZ_OUTPUT_DIR = Paths.get("src/main/resources/wazClariasGenerated");

    private static final String WAZ_EXT = ".waz";
    private static final String GENERATED_SUFFIX = ".generated";

    @Test
    @Order(1)
    void testGenerateWazJsonFiles() throws IOException {
        List<Waz> allWazList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        assumeWazAssets();
        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_WAZ_DIR, "*" + WAZ_EXT)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO<?> dto = clariasBinService.parse(path.toString(), "windows-31j");
                    Waz waz = (Waz) dto.getData();
                    allWazList.add(waz);
                } catch (Exception e) {
                    log.warn("Failed to parse: {}", fileName, e);
                    throw e;
                }
            }
        }

        for (int i = 0; i < allWazList.size(); i++) {
            Waz waz = allWazList.get(i);
            String jsonStr = JSONUtil.toJsonStr(waz);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".waz.json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("Exported: {}", jsonPath);
        }
    }

    @Test
    @Order(2)
    void testGenerateWazFilesByJson() throws IOException {
        assumeWazAssets();
        Assumptions.assumeTrue(Files.isDirectory(JSON_OUTPUT_DIR), "missing generated clarias waz json directory");

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
                clariasBinService.generate(output.toString(), waz, "windows-31j");
                log.info("Generated: {}", output);
            }
        }
    }

    @Test
    @Order(3)
    void testWazParseGenerateBinaryConsistency() throws IOException {
        assumeWazAssets();
        Assumptions.assumeTrue(Files.isDirectory(WAZ_OUTPUT_DIR), "missing generated clarias waz directory");

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
                    log.error("Generated file not found for original: {}", name);
                    continue;
                }

                byte[] originalBytes = FileUtil.readBytes(ori.toFile());
                byte[] generatedBytes = FileUtil.readBytes(gen.toFile());
                compared++;

                if (!ArrayUtil.equals(originalBytes, generatedBytes)) {
                    anyIssue = true;
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
                } else {
                    log.info("Match: {}", name);
                }
            }
        }

        if (!anyIssue && compared > 0) {
            if (Files.exists(JSON_OUTPUT_DIR)) {
                FileUtil.del(JSON_OUTPUT_DIR.toFile());
                log.info("Removed: {}", JSON_OUTPUT_DIR.toAbsolutePath());
            }
            if (Files.exists(WAZ_OUTPUT_DIR)) {
                FileUtil.del(WAZ_OUTPUT_DIR.toFile());
                log.info("Removed: {}", WAZ_OUTPUT_DIR.toAbsolutePath());
            }
        } else {
            if (anyIssue) {
                Assertions.fail("Mismatch or missing generated files detected. Outputs retained for inspection.");
            } else {
                Assertions.fail("No files compared. Check inputs/outputs before cleanup.");
            }
        }
    }

    private static void assumeWazAssets() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(GAME_WAZ_DIR), "missing clarias waz asset directory");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_WAZ_DIR, "*" + WAZ_EXT)) {
            Assumptions.assumeTrue(stream.iterator().hasNext(), "no clarias waz assets found");
        }
    }
}
