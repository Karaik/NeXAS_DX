package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ArrayUtil;
import cn.hutool.json.JSONUtil;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@TestMethodOrder(OrderAnnotation.class)
@Execution(ExecutionMode.SAME_THREAD)
/**
 * BSDX BIN 旧 parse/generate 路径的全量回归测试。
 *
 * <p>这个测试类不走新的 lossless DSL 主链，而是直接验证现有
 * {@link com.giga.nexas.service.BsdxBinService} 的解析与回写结果是否保持字节一致。
 * 它和新主链测试共同构成当前 BIN 反编译器/回编译器的双保险。
 */
public class TestBin {

    private static final Logger log = LoggerFactory.getLogger(TestBin.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    private static final String CHARSET = "windows-31j";

    private static final Path GAME_BIN_DIR   = Paths.get("src/main/resources/game/bsdx/bin");
    private static final Path JSON_OUTPUT_DIR = Paths.get("src/main/resources/binBsdxJson");
    private static final Path BIN_OUTPUT_DIR  = Paths.get("src/main/resources/binBsdxGenerated");

    private static final String BIN_EXT = ".bin";
    private static final String GENERATED_SUFFIX = ".generated"; // 所有生成的二进制都带这个后缀（放在扩展名后面）

    /**
     * 判断某个 BIN 是否属于应跳过的特殊文件。
     *
     * <p>这类文件通常是全局容器或资源壳，不属于普通脚本入口，
     * 因此不纳入“全量脚本 parse/generate 一致性”统计。
     */
    private boolean shouldSkipSpecialBin(String fileName) {
        String upper = fileName == null ? "" : fileName.toUpperCase();
        return upper.startsWith("__") || upper.contains("GLOBAL");
    }

    @Test
    @Order(1)
    void testGenerateBinJsonFiles() throws IOException {
        List<Bin> allBinList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        Files.createDirectories(JSON_OUTPUT_DIR);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_BIN_DIR, "*" + BIN_EXT)) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                if (shouldSkipSpecialBin(fileName)) {
                    log.info("skip special bin: {}", fileName);
                    continue;
                }
                baseNames.add(baseName);

                try {
                    ResponseDTO dto = bsdxBinService.parse(path.toString(), CHARSET);
                    Bin bin = (Bin) dto.getData();
                    allBinList.add(bin);
                    log.info("✅ parsed: {}", fileName);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse: {}", fileName, e);
                    throw e;
                }
            }
        }

        for (int i = 0; i < allBinList.size(); i++) {
            Bin bin = allBinList.get(i);
            String jsonStr = JSONUtil.toJsonStr(bin);
            Path jsonPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + BIN_EXT + ".json");
            FileUtil.writeUtf8String(jsonStr, jsonPath.toFile());
            log.info("✅ Exported: {}", jsonPath);
        }
    }

    @Test
    @Order(2)
    void testGenerateBinFilesByJson() throws IOException {
        Files.createDirectories(BIN_OUTPUT_DIR);
        ObjectMapper mapper = new ObjectMapper();
        mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(JSON_OUTPUT_DIR, "*.json")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                // 期望 JSON 文件名：<basename>.bin.json
                String baseName = fileName.endsWith(BIN_EXT + ".json")
                        ? fileName.substring(0, fileName.length() - (BIN_EXT + ".json").length())
                        : fileName.substring(0, fileName.lastIndexOf('.'));

                String jsonStr = FileUtil.readUtf8String(path.toFile());
                Bin bin = mapper.readValue(jsonStr, Bin.class);

                // 生成的二进制统一命名：<basename>.bin.generated
                Path output = BIN_OUTPUT_DIR.resolve(baseName + BIN_EXT + GENERATED_SUFFIX);
                bsdxBinService.generate(output.toString(), bin, CHARSET);
                log.info("✅ Generated: {}", output);
            }
        }
    }

    @Test
    @Order(3)
    void testBinParseGenerateBinaryConsistency() throws IOException {
        Map<String, Path> generatedMap = new HashMap<>();
        try (DirectoryStream<Path> genStream = Files.newDirectoryStream(BIN_OUTPUT_DIR, "*" + BIN_EXT + GENERATED_SUFFIX)) {
            for (Path gen : genStream) {
                String genName = gen.getFileName().toString();
                String originalName = genName.replace(GENERATED_SUFFIX, "");
                generatedMap.put(originalName, gen);
            }
        }

        boolean anyIssue = false;
        int compared = 0;
        int matched = 0;

        try (DirectoryStream<Path> oriStream = Files.newDirectoryStream(GAME_BIN_DIR, "*" + BIN_EXT)) {
            for (Path ori : oriStream) {
                String name = ori.getFileName().toString();
                if (shouldSkipSpecialBin(name)) {
                    log.info("skip special bin during binary consistency check: {}", name);
                    continue;
                }
                compared++;
                Path gen = generatedMap.get(name);
                if (gen == null) {
                    anyIssue = true;
                    log.error("❌ Generated file not found for original: {}", name);
                    continue;
                }

                byte[] originalBytes = FileUtil.readBytes(ori.toFile());
                byte[] generatedBytes = FileUtil.readBytes(gen.toFile());

                if (!ArrayUtil.equals(originalBytes, generatedBytes)) {
                    anyIssue = true;
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
                    matched++;
                    log.info("✅ Match: {}", name);
                }
            }
        }

        double rate = compared > 0 ? (matched * 1.0 / compared) : 0.0;
        log.info("Matched {}/{} ({})", matched, compared, String.format("%.2f%%", rate * 100.0));

        if (compared > 0 && rate >= 0.90) {
            if (Files.exists(JSON_OUTPUT_DIR)) {
                FileUtil.del(JSON_OUTPUT_DIR.toFile());
                log.info("🧹 Removed: {}", JSON_OUTPUT_DIR.toAbsolutePath());
            }
            if (Files.exists(BIN_OUTPUT_DIR)) {
                FileUtil.del(BIN_OUTPUT_DIR.toFile());
                log.info("🧹 Removed: {}", BIN_OUTPUT_DIR.toAbsolutePath());
            }
        } else {
            if (anyIssue) {
                Assertions.fail("Success rate below 90%. Outputs retained for inspection.");
            } else {
                Assertions.fail("No files compared. Check inputs/outputs before cleanup.");
            }
        }
    }

    @Test
    @Disabled
    public void testOutputIR() throws IOException {
        if (!Files.exists(GAME_BIN_DIR)) {
            log.error("❌ Bin 目录不存在: {}", GAME_BIN_DIR);
            return;
        }

        Files.createDirectories(JSON_OUTPUT_DIR);
        List<Bin> allBinList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_BIN_DIR, "*.bin")) {
            for (Path path : stream) {
                String fileName = URLDecoder.decode(path.getFileName().toString(), StandardCharsets.UTF_8);
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO parse = bsdxBinService.parse(path.toString(), "windows-31j");
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

            Path outputPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".json");
            FileUtil.writeUtf8String(jsonStr, outputPath.toFile());
            log.info("✅ 导出 bin 内的IR: {}", outputPath);
        }
    }

    @Test
    @Disabled
    void testOutputInstructions() throws IOException {
        if (!Files.exists(GAME_BIN_DIR)) {
            log.error("❌ Bin 目录不存在: {}", GAME_BIN_DIR);
            return;
        }

        Files.createDirectories(JSON_OUTPUT_DIR);
        List<List<Bin.Instruction>> allInstructionList = new ArrayList<>();
        List<String> baseNames = new ArrayList<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_BIN_DIR, "*.bin")) {
            for (Path path : stream) {
                String fileName = URLDecoder.decode(path.getFileName().toString(), StandardCharsets.UTF_8);
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                baseNames.add(baseName);

                try {
                    ResponseDTO parse = bsdxBinService.parse(path.toString(), "windows-31j");
                    Bin bin = (Bin) parse.getData();
                    if (bin != null) {
                        allInstructionList.add(bin.getInstructions());
                    } else {
                        log.info("⚠️ 解析为空: " + fileName);
                    }
                } catch (Exception e) {
                    log.error("❌ 解析失败: {} - {}", fileName, e.getMessage());
                    throw e;
                }
            }
        }

        for (int i = 0; i < allInstructionList.size(); i++) {
            List<Bin.Instruction> instructions = allInstructionList.get(i);
            StringBuilder instructionStr = new StringBuilder();
            for (Bin.Instruction instruction : instructions) {
                String line = String.format("%-8s\t%-2d\t%s",
                        instruction.getOpcode(),
                        instruction.getParamCount(),
                        instruction.getNativeFunction() != null ? instruction.getNativeFunction() : "");
                instructionStr.append(line).append("\n");
            }
            if (instructionStr.isEmpty()) continue;

            Path outputPath = JSON_OUTPUT_DIR.resolve(baseNames.get(i) + ".txt");
            FileUtil.writeUtf8String(instructionStr.toString(), outputPath.toFile());
            log.info("✅ 导出 JSON: {}", outputPath);
        }
    }

}
