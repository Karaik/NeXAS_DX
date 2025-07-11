package com.giga.nexas.jinki;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TestGrp {

    private static final Logger log = LoggerFactory.getLogger(TestGrp.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    private static final Path GRP_DIR = Paths.get("src/main/resources/game/jinki/grp");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/grpJinkiJson");
    private static final Path GRP_OUTPUT_DIR = Paths.get("src/main/resources/grpJinkiGenerated");

    @Test
    void testGenerateGrpJsonFiles() throws IOException {
        List<Grp> allGrpList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    Grp grp = (Grp) dto.getData();
                    allGrpList.add(grp);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse: {}", fileName);
                }
            }
        }

        for (int i = 0; i < allGrpList.size(); i++) {
            Grp grp = allGrpList.get(i);
            String jsonStr = JSONUtil.toJsonStr(grp);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(grp.getFileName() + ".grp.json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("✅ Exported: {}", jsonPath);
        }
    }

    @Test
    void testGenerateGrpFilesByJson() throws IOException {
        Files.createDirectories(GRP_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String jsonStr = FileUtil.readUtf8String(path.toFile());
                Grp grp = mapper.readValue(jsonStr, Grp.class);
                String baseName = path.getFileName().toString().replace(".json", "");
                Path output = GRP_OUTPUT_DIR.resolve(baseName + ".generated.grp");

                bsdxBinService.generate(output.toString(), grp, "windows-31j");
                log.info("Generated: {}", output);
            }
        }
    }

    @Test
    void testGrpParseGenerateBinaryConsistency() throws IOException {
        Map<String, Path> generatedMap = new HashMap<>();
        try (DirectoryStream<Path> genStream = Files.newDirectoryStream(GRP_OUTPUT_DIR, "*.generated.grp")) {
            for (Path gen : genStream) {
                generatedMap.put(gen.getFileName().toString(), gen);
            }
        }

        Path mismatchDir = GRP_OUTPUT_DIR.resolve("mismatch");
        Files.createDirectories(mismatchDir);

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(GRP_DIR, "*.grp")) {
            for (Path ori : oriStream) {
                String name = ori.getFileName().toString().replace(".grp", ".generated.grp");
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
                    String newName = gen.getFileName().toString().replace(".generated", "");
                    Path target = mismatchDir.resolve(newName);
                    Files.move(gen, target, StandardCopyOption.REPLACE_EXISTING);
                    log.warn("Moved mismatch file to: {}", target);
                }
            }
        }
    }
}
