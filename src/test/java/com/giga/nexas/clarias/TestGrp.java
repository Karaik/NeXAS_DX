package com.giga.nexas.clarias;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.clarias.grp.Grp;
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
public class TestGrp {

    private static final Logger log = LoggerFactory.getLogger(TestGrp.class);
    private final ClariasBinService clariasBinService = new ClariasBinService();

    private static final Path GAME_GRP_DIR = Paths.get("src/main/resources/game/clarias/grp");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/grpClariasJson");
    private static final Path GRP_OUTPUT_DIR = Paths.get("src/main/resources/grpClariasGenerated");

    private static final String GRP_EXT = ".grp";
    private static final String GENERATED_SUFFIX = ".generated";

    @Test
    @Order(1)
    void testGenerateGrpJsonFiles() throws IOException {
        List<Grp> allGrpList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        assumeGrpAssets();
        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_GRP_DIR, "*" + GRP_EXT)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO<?> dto = clariasBinService.parse(path.toString(), "windows-31j");
                    Grp grp = (Grp) dto.getData();
                    allGrpList.add(grp);
                } catch (Exception e) {
                    log.warn("Failed to parse: {}", fileName, e);
                    throw e;
                }
            }
        }

        for (int i = 0; i < allGrpList.size(); i++) {
            Grp grp = allGrpList.get(i);
            String jsonStr = JSONUtil.toJsonStr(grp);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".grp.json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("Exported: {}", jsonPath);
        }
    }

    @Test
    @Order(2)
    void testGenerateGrpFilesByJson() throws IOException {
        assumeGrpAssets();
        Assumptions.assumeTrue(Files.isDirectory(JSON_OUTPUT_DIR), "missing generated clarias grp json directory");

        Files.createDirectories(GRP_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.endsWith(".grp.json")
                        ? fileName.substring(0, fileName.length() - ".grp.json".length())
                        : fileName.substring(0, fileName.lastIndexOf('.'));

                String jsonStr = FileUtil.readUtf8String(path.toFile());
                Grp grp = mapper.readValue(jsonStr, Grp.class);

                Path output = GRP_OUTPUT_DIR.resolve(baseName + GRP_EXT + GENERATED_SUFFIX);
                clariasBinService.generate(output.toString(), grp, "windows-31j");
                log.info("Generated: {}", output);
            }
        }
    }

    @Test
    @Order(3)
    void testGrpParseGenerateBinaryConsistency() throws IOException {
        assumeGrpAssets();
        Assumptions.assumeTrue(Files.isDirectory(GRP_OUTPUT_DIR), "missing generated clarias grp directory");

        Map<String, Path> generatedMap = new HashMap<>();
        try (DirectoryStream<Path> genStream = Files.newDirectoryStream(GRP_OUTPUT_DIR, "*" + GRP_EXT + GENERATED_SUFFIX)) {
            for (Path gen : genStream) {
                String genName = gen.getFileName().toString();
                String originalName = genName.replace(GENERATED_SUFFIX, "");
                generatedMap.put(originalName, gen);
            }
        }

        boolean anyIssue = false;
        int compared = 0;

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(GAME_GRP_DIR, "*" + GRP_EXT)) {
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
            if (Files.exists(GRP_OUTPUT_DIR)) {
                FileUtil.del(GRP_OUTPUT_DIR.toFile());
                log.info("Removed: {}", GRP_OUTPUT_DIR.toAbsolutePath());
            }
        } else {
            if (anyIssue) {
                Assertions.fail("Mismatch or missing generated files detected. Outputs retained for inspection.");
            } else {
                Assertions.fail("No files compared. Check inputs/outputs before cleanup.");
            }
        }
    }

    private static void assumeGrpAssets() throws IOException {
        Assumptions.assumeTrue(Files.isDirectory(GAME_GRP_DIR), "missing clarias grp asset directory");
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_GRP_DIR, "*" + GRP_EXT)) {
            Assumptions.assumeTrue(stream.iterator().hasNext(), "no clarias grp assets found");
        }
    }
}
