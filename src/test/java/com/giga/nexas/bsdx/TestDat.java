package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;


public class TestDat {

    private static final Logger log = LoggerFactory.getLogger(TestDat.class);
    private static final Path INPUT_DIR = Paths.get("src/main/resources/game/bsdx/dat");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/datJson");
    private static final Path DAT_OUTPUT_DIR = Paths.get("src/main/resources/datGenerated");

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    @Test
    void testGenerateDatJsonFiles() throws IOException {
        List<Dat> allDatList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(INPUT_DIR, "*.dat")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    Dat dat = (Dat) dto.getData();
                    allDatList.add(dat);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse: {}", fileName, e);
                }
            }
        }

        for (int i = 0; i < allDatList.size(); i++) {
            Dat dat = allDatList.get(i);
            String jsonStr = JSONUtil.toJsonStr(dat);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("✅ Exported: {}", jsonPath);
        }
    }

    @Test
    void testGenerateDatFilesByJson() throws IOException {
        Files.createDirectories(DAT_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String jsonStr = FileUtil.readUtf8String(path.toFile());
                Dat dat = mapper.readValue(jsonStr, Dat.class);
                String baseName = path.getFileName().toString().replace(".json", "");
                Path output = DAT_OUTPUT_DIR.resolve(baseName + ".dat");

                bsdxBinService.generate(output.toString(), dat, "windows-31j");
                log.info("✅ Generated: {}", output);
            }
        }
    }

    @Test
    void testDatParseGenerateBinaryConsistency() throws IOException {
        Map<String, Path> generatedMap = new HashMap<>();
        try (DirectoryStream<Path> genStream = Files.newDirectoryStream(DAT_OUTPUT_DIR, "*.dat")) {
            for (Path gen : genStream) {
                generatedMap.put(gen.getFileName().toString(), gen);
            }
        }

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(INPUT_DIR, "*.dat")) {
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
