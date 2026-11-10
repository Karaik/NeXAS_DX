package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.generator.BinGenerator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessPipeline;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIr;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIrInstruction;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * 新 strict IR / structured IR / lossless DSL 主链的回归测试。
 *
 * <p>这里故意只测新主链，而且直接使用真实游戏 BIN 文件。
 * 这样一旦失败，说明偏离的是实际 VM 行为，而不是玩具样本。
 */
class BsdxBinLosslessDslRoundTripTest {

    private static final String CHARSET = "windows-31j";
    private static final Path GAME_BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BsdxBinLosslessPipeline pipeline = new BsdxBinLosslessPipeline();
    private final BinGenerator generator = new BinGenerator();

    /**
     * 验证最底层的可逆段：{@code bin -> strict IR -> bin}。
     *
     * <p>这里选 {@code Hell.bin}，因为它是真实关卡脚本，包含分支、marker、call，
     * 但仍落在当前第一批支持的 opcode 子集里。
     */
    @Test
    void strictIrRoundTripPreservesHellBinBytes() throws Exception {
        Path sample = resolveSample("Hell.bin");
        Bin original = parseBin(sample);
        BsdxBinStrictIr strictIr = pipeline.toStrictIr(original);
        Bin rebuilt = pipeline.fromStrictIr(strictIr);
        assertBinaryEquals(sample, rebuilt);
    }

    /**
     * 验证第一版完整链路：
     * {@code bin -> strict IR -> structured IR -> DSL -> structured IR -> strict IR -> bin}。
     *
     * <p>这是当前最核心的证明：说明新 DSL 在已支持子集上确实是无损的。
     */
    @Test
    void losslessDslRoundTripPreservesHellBinStructureAndBytes() throws Exception {
        Path sample = resolveSample("Hell.bin");
        Bin original = parseBin(sample);
        BsdxBinStrictIr strictIr = pipeline.toStrictIr(original);

        BsdxBinLosslessProgram program = pipeline.lift(strictIr);
        String dsl = pipeline.render(program);
        BsdxBinLosslessProgram reparsedProgram = pipeline.parse(program, dsl);
        BsdxBinStrictIr reparsedStrictIr = pipeline.lower(reparsedProgram);

        Assertions.assertEquals(program, reparsedProgram, "Structured IR should survive DSL round-trip without drift.");
        Assertions.assertEquals(strictIr, reparsedStrictIr, "Strict IR should survive DSL round-trip without drift.");
        assertBinaryEquals(sample, pipeline.fromStrictIr(reparsedStrictIr));
    }

    /**
     * 用第二个真实样本验证这条链不是只对一个文件成立。
     *
     * <p>{@code Tutorial010...bin} 比 {@code Hell.bin} 小很多，主要覆盖偏简单的 call-heavy 子集，
     * 可以防止实现偷偷依赖关卡文件特有形状。
     */
    @Test
    void secondRealSampleAlsoRoundTripsLosslessly() throws Exception {
        Path sample = resolveSampleByPrefix("Tutorial010");
        Bin original = parseBin(sample);
        BsdxBinStrictIr strictIr = pipeline.toStrictIr(original);

        BsdxBinLosslessProgram program = pipeline.lift(strictIr);
        String dsl = pipeline.render(program);
        BsdxBinStrictIr reparsedStrictIr = pipeline.lower(pipeline.parse(program, dsl));

        Assertions.assertEquals(strictIr, reparsedStrictIr, "Second real sample strict IR drifted after DSL round-trip.");
        assertBinaryEquals(sample, pipeline.fromStrictIr(reparsedStrictIr));
    }

    /**
     * 验证未支持 opcode 会明确失败，而不是被悄悄忽略或靠猜测继续。
     *
     * <p>这是 lossless 保证的一部分：一旦 lifter 无法建模，就必须立刻停下来。
     */
    @Test
    void unsupportedOpcodeIsRejectedExplicitly() {
        BsdxBinStrictIr unsupported = new BsdxBinStrictIr(
                "bin",
                CHARSET,
                0,
                List.of(),
                List.of(new BsdxBinStrictIrInstruction(0, 62, 0, "RETURN", null, null, null)),
                null,
                List.of(),
                List.of()
        );

        IllegalArgumentException error = Assertions.assertThrows(
                IllegalArgumentException.class,
                () -> pipeline.lift(unsupported)
        );
        Assertions.assertTrue(error.getMessage().contains("Opcode is outside the current lossless subset"));
    }

    /**
     * 验证全量 BSDX BIN 在“DSL 未改动”的情况下都能走通新主链并回到完全一致的 strict IR。
     *
     * <p>这条测试就是当前反编译器阶段的“全量通过”标准：
     * {@code bin -> strict IR -> structured IR -> DSL -> structured IR -> strict IR}。
     * 只要 DSL 文本没变，就必须精准回到原始 strict IR。
     */
    @Test
    void allBsdxBinsRoundTripLosslesslyWhenDslIsUnchanged() throws Exception {
        if (!Files.exists(GAME_BIN_DIR)) {
            return;
        }

        List<String> failures = new ArrayList<>();
        try (var stream = Files.newDirectoryStream(GAME_BIN_DIR, "*.bin")) {
            for (Path path : stream) {
                if (shouldSkipDecompilerAudit(path)) {
                    continue;
                }

                try {
                    Bin original = parseBin(path);
                    if (shouldSkipDecompilerAudit(path, original)) {
                        continue;
                    }
                    BsdxBinStrictIr strictIr = pipeline.toStrictIr(original);
                    BsdxBinLosslessProgram program = pipeline.lift(strictIr);
                    String dsl = pipeline.render(program);
                    BsdxBinStrictIr reparsedStrictIr = pipeline.lower(pipeline.parse(program, dsl));

                    if (!strictIr.equals(reparsedStrictIr)) {
                        failures.add(path.getFileName() + " strict IR drift");
                    }
                } catch (Exception ex) {
                    failures.add(path.getFileName() + " " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }
        }

        Assertions.assertTrue(
                failures.isEmpty(),
                "Lossless DSL full-bin audit failures:\n" + String.join("\n", failures)
        );
    }

    /**
     * 按文件名判断是否应跳过反编译器全量审计。
     *
     * <p>这类文件通常是全局容器或特殊资源壳，不作为“普通脚本文本可逆”目标处理。
     */
    private boolean shouldSkipDecompilerAudit(Path path) {
        String upper = path.getFileName().toString().toUpperCase();
        return upper.startsWith("__") || upper.contains("GLOBAL");
    }

    /**
     * 在解析完成后再次判断是否应跳过审计。
     *
     * <p>如果某个 BIN 虽然扩展名相同，但解析后没有正常脚本指令流，也按特殊 BIN 处理。
     */
    private boolean shouldSkipDecompilerAudit(Path path, Bin bin) {
        return shouldSkipDecompilerAudit(path)
                || bin == null
                || bin.getInstructions() == null
                || bin.getInstructions().isEmpty();
    }

    /**
     * 通过现有 service 层解析真实 BIN，并补齐回写时需要的元数据。
     */
    private Bin parseBin(Path sample) throws IOException {
        ResponseDTO<?> dto = bsdxBinService.parse(sample.toString(), CHARSET);
        Bin bin = (Bin) dto.getData();
        bin.setExtensionName("bin");
        bin.setCharset(CHARSET);
        return bin;
    }

    /**
     * 把重建后的 BIN 落盘，再与原样本做字节对比。
     *
     * <p>这是本测试类所有 round-trip 验证的最终字节级判定器。
     */
    private void assertBinaryEquals(Path originalPath, Bin rebuilt) throws Exception {
        rebuilt.setExtensionName("bin");
        rebuilt.setCharset(CHARSET);

        Path tempDir = Files.createTempDirectory("bin-lossless-roundtrip");
        try {
            Path rebuiltPath = tempDir.resolve(originalPath.getFileName().toString());
            generator.generate(rebuiltPath.toString(), rebuilt, CHARSET);

            byte[] expected = Files.readAllBytes(originalPath);
            byte[] actual = Files.readAllBytes(rebuiltPath);
            Assertions.assertArrayEquals(expected, actual, "Binary bytes changed after round-trip: " + originalPath.getFileName());
        } finally {
            FileUtil.del(tempDir.toFile());
        }
    }

    /**
     * 在 BSDX 游戏 BIN 目录中按精确文件名解析真实样本。
     */
    private Path resolveSample(String fileName) {
        Path sample = GAME_BIN_DIR.resolve(fileName);
        Assertions.assertTrue(Files.exists(sample), "Missing sample file: " + sample);
        return sample;
    }

    /**
     * 当文件名含有不方便硬编码的非 ASCII 文本时，按前缀解析真实样本。
     */
    private Path resolveSampleByPrefix(String prefix) throws IOException {
        try (var stream = Files.list(GAME_BIN_DIR)) {
            return stream
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Missing sample with prefix: " + prefix));
        }
    }
}
