package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.map.MapData;
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
import java.util.List;

@TestMethodOrder(OrderAnnotation.class)
@Execution(ExecutionMode.SAME_THREAD)
public class TestMapData {

    private static final Logger log = LoggerFactory.getLogger(TestMapData.class);

    private static final Path GAME_MAP_DIR = Paths.get("src/main/resources/game/bsdx/map");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/mapBsdxJson");
    private static final Path MAP_OUTPUT_DIR = Paths.get("src/main/resources/mapBsdxGenerated");

    private static final String MAP_EXT = ".map";
    private static final String GENERATED_SUFFIX = ".generated";
    private static final String CHARSET = "windows-31j";

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    @Order(1)
    void testGenerateMapJsonFiles() throws IOException {
        assertMapAssets();
        Files.createDirectories(JSON_OUTPUT_DIR);

        List<MapData> allMapList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_MAP_DIR, "*" + MAP_EXT)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                MapData mapData = (MapData) dto.getData();
                allMapList.add(mapData);
            }
        }

        for (int i = 0; i < allMapList.size(); i++) {
            String jsonStr = JSONUtil.toJsonStr(allMapList.get(i));
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".map.json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("✅ Exported: {}", jsonPath);
        }
    }

    @Test
    @Order(2)
    void testGenerateMapFilesByJson() throws IOException {
        assertMapAssets();
        Assertions.assertTrue(Files.isDirectory(JSON_OUTPUT_DIR), "missing generated bsdx map json directory");

        Files.createDirectories(MAP_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.endsWith(".map.json")
                        ? fileName.substring(0, fileName.length() - ".map.json".length())
                        : fileName.substring(0, fileName.lastIndexOf('.'));

                String jsonStr = FileUtil.readUtf8String(path.toFile());
                MapData mapData = mapper.readValue(jsonStr, MapData.class);

                Path output = MAP_OUTPUT_DIR.resolve(baseName + MAP_EXT + GENERATED_SUFFIX);
                bsdxBinService.generate(output.toString(), mapData, CHARSET);
                log.info("✅ Generated: {}", output);
            }
        }
    }

    @Test
    @Order(3)
    void testMapDataParseGenerateBinaryConsistency() throws IOException {
        assertMapAssets();
        Assertions.assertTrue(Files.isDirectory(MAP_OUTPUT_DIR), "missing generated bsdx map directory");

        boolean anyIssue = false;
        int compared = 0;

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(GAME_MAP_DIR, "*" + MAP_EXT)) {
            for (Path ori : oriStream) {
                String name = ori.getFileName().toString();
                Path gen = MAP_OUTPUT_DIR.resolve(name + GENERATED_SUFFIX);

                if (!Files.exists(gen)) {
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
                    log.info("✅ Match: {}", name);
                }
            }
        }

        if (!anyIssue && compared > 0) {
            if (Files.exists(JSON_OUTPUT_DIR)) {
                FileUtil.del(JSON_OUTPUT_DIR.toFile());
                log.info("Removed: {}", JSON_OUTPUT_DIR.toAbsolutePath());
            }
            if (Files.exists(MAP_OUTPUT_DIR)) {
                FileUtil.del(MAP_OUTPUT_DIR.toFile());
                log.info("Removed: {}", MAP_OUTPUT_DIR.toAbsolutePath());
            }
        } else {
            if (anyIssue) {
                Assertions.fail("Mismatch or missing generated map files detected. Outputs retained for inspection.");
            } else {
                Assertions.fail("No map files compared. Check inputs/outputs before cleanup.");
            }
        }
    }

    private static void assertMapAssets() throws IOException {
        Assertions.assertTrue(Files.isDirectory(GAME_MAP_DIR), "missing bsdx map asset directory");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_MAP_DIR, "*" + MAP_EXT)) {
            Assertions.assertTrue(stream.iterator().hasNext(), "no bsdx map assets found");
        }
    }
}
