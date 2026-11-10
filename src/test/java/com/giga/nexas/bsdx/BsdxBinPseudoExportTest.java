package com.giga.nexas.bsdx;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessPipeline;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram;
import com.giga.nexas.dto.bsdx.bin.pseudo.renderer.BsdxBinRenderer;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIr;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * 手动审视真实 BSDX BIN 伪代码的导出测试。
 *
 * <p>这个测试不验证 round-trip，而是把真实游戏资源里的 {@code Hell*.bin}
 * 直接走新主链导出成 lossless pseudo DSL，落盘到仓库内的 {@code tmp} 目录，
 * 方便后续对照真实脚本、IDA 逆向记录和关卡配置关系做人工分析。
 */
class BsdxBinPseudoExportTest {

    /**
     * BSDX BIN 的默认文本编码。
     */
    private static final String CHARSET = "windows-31j";

    /**
     * 仓库内的真实 BSDX BIN 样本目录。
     */
    private static final Path GAME_BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");

    /**
     * 伪代码导出目录。
     *
     * <p>这里故意放在 {@code tmp} 下，便于人工查看，同时避免被正式资源目录误引用。
     */
    private static final Path EXPORT_DIR = Paths.get("src/main/resources/tmp/bsdx-lossless-pseudo-export");

    /**
     * 导入 `__GLOBAL.bin` 符号后的旧 pseudo 导出目录。
     */
    private static final Path SYMBOLIC_EXPORT_DIR = Paths.get("src/main/resources/tmp/bsdx-symbolic-pseudo-export");

    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BsdxBinLosslessPipeline pipeline = new BsdxBinLosslessPipeline();
    private final BsdxBinRenderer renderer = new BsdxBinRenderer();

    /**
     * 导出全部 {@code Hell*.bin} 的 lossless pseudo DSL。
     *
     * <p>这一步的目的不是自动验收功能完整性，而是尽快把真实关卡脚本文本导出来，
     * 让后续的“Hell stage 到脚本/配置的映射关系”分析可以直接建立在实际伪代码之上。
     */
    @Test
    void exportHellBinsAsLosslessDslForManualInspection() throws Exception {
        if (!Files.exists(GAME_BIN_DIR)) {
            return;
        }

        Files.createDirectories(EXPORT_DIR);

        List<String> exported = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        try (Stream<Path> stream = Files.list(GAME_BIN_DIR)) {
            List<Path> hellBins = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("Hell"))
                    .filter(path -> path.getFileName().toString().endsWith(".bin"))
                    .sorted()
                    .toList();

            for (Path path : hellBins) {
                String fileName = path.getFileName().toString();
                try {
                    Bin original = parseBin(path);
                    BsdxBinStrictIr strictIr = pipeline.toStrictIr(original);
                    BsdxBinLosslessProgram program = pipeline.lift(strictIr);
                    String dsl = pipeline.render(program);

                    Path output = EXPORT_DIR.resolve(fileName + ".lossless.pseudo.txt");
                    Files.writeString(output, dsl, StandardCharsets.UTF_8);
                    exported.add(output.getFileName().toString());
                } catch (Exception ex) {
                    failures.add(fileName + " " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }
        }

        writeSummary(exported, failures);

        Assertions.assertFalse(exported.isEmpty(), "No Hell BIN pseudo files were exported.");
        Assertions.assertTrue(
                failures.isEmpty(),
                "Some Hell BIN pseudo exports failed. See summary under tmp.\n" + String.join("\n", failures)
        );
    }

    /**
     * 导出注入 `__GLOBAL.bin` 符号后的旧 pseudo 文本。
     *
     * <p>这份导出直接面向编辑器与人工阅读：
     * 它保留现有 pseudo compile 链可回编的文本格式，同时把能识别的全局符号别名显示出来。
     */
    @Test
    void exportHellBinsAsSymbolicPseudoForManualInspection() throws Exception {
        if (!Files.exists(GAME_BIN_DIR)) {
            return;
        }

        Files.createDirectories(SYMBOLIC_EXPORT_DIR);

        List<String> exported = new ArrayList<>();
        List<String> failures = new ArrayList<>();

        try (Stream<Path> stream = Files.list(GAME_BIN_DIR)) {
            List<Path> hellBins = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith("Hell"))
                    .filter(path -> path.getFileName().toString().endsWith(".bin"))
                    .sorted()
                    .toList();

            for (Path path : hellBins) {
                String fileName = path.getFileName().toString();
                try {
                    Bin original = parseBin(path);
                    String pseudo = renderer.renderPseudo(original);

                    Path output = SYMBOLIC_EXPORT_DIR.resolve(fileName + ".symbolic.pseudo.txt");
                    Files.writeString(output, pseudo, StandardCharsets.UTF_8);
                    exported.add(output.getFileName().toString());
                } catch (Exception ex) {
                    failures.add(fileName + " " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                }
            }
        }

        writeSummary(SYMBOLIC_EXPORT_DIR, "BSDX Hell BIN symbolic pseudo export summary", exported, failures);

        Assertions.assertFalse(exported.isEmpty(), "No symbolic pseudo files were exported.");
        Assertions.assertTrue(
                failures.isEmpty(),
                "Some symbolic pseudo exports failed. See summary under tmp.\n" + String.join("\n", failures)
        );
    }

    /**
     * 单独导出 {@code Hell.bin} 的字符串表。
     *
     * <p>当前 Hell 模式总控脚本会按字符串表索引分发到不同子脚本，
     * 所以这份导出能直接验证 {@code str[n]} 与真实 {@code HellXXX.bin} 文件名之间的关系。
     */
    @Test
    void exportHellDispatcherStringTable() throws Exception {
        Path hellBin = GAME_BIN_DIR.resolve("Hell.bin");
        if (!Files.exists(hellBin)) {
            return;
        }

        Files.createDirectories(EXPORT_DIR);

        Bin bin = parseBin(hellBin);
        List<String> lines = new ArrayList<>();
        lines.add("Hell.bin string table");
        lines.add("source=" + hellBin.toAbsolutePath());
        lines.add("");

        List<String> stringTable = bin.getStringTable();
        for (int i = 0; i < stringTable.size(); i++) {
            lines.add("[" + i + "] " + stringTable.get(i));
        }

        Files.write(
                EXPORT_DIR.resolve("Hell.bin.string-table.txt"),
                lines,
                StandardCharsets.UTF_8
        );

        Assertions.assertFalse(stringTable.isEmpty(), "Hell.bin string table should not be empty.");
    }

    /**
     * 通过现有 service 层解析真实 BIN，并补齐新主链需要的基本元数据。
     */
    private Bin parseBin(Path sample) throws Exception {
        ResponseDTO<?> dto = bsdxBinService.parse(sample.toString(), CHARSET);
        Bin bin = (Bin) dto.getData();
        bin.setExtensionName("bin");
        bin.setCharset(CHARSET);
        return bin;
    }

    /**
     * 把本次导出的结果与失败清单写到摘要文件。
     *
     * <p>这样即使终端输出被截断，也可以直接打开文件看到成功和失败情况。
     */
    private void writeSummary(List<String> exported, List<String> failures) throws Exception {
        writeSummary(EXPORT_DIR, "BSDX Hell BIN pseudo export summary", exported, failures);
    }

    /**
     * 把指定目录的导出结果和失败清单写成摘要文件。
     */
    private void writeSummary(Path outputDir, String title, List<String> exported, List<String> failures) throws Exception {
        List<String> lines = new ArrayList<>();
        lines.add(title);
        lines.add("export_dir=" + outputDir.toAbsolutePath());
        lines.add("exported_count=" + exported.size());
        lines.add("failure_count=" + failures.size());
        lines.add("");
        lines.add("[exported]");
        lines.addAll(exported);
        lines.add("");
        lines.add("[failures]");
        lines.addAll(failures);
        lines.add("");
        lines.add("hint=open Hell.bin.lossless.pseudo.txt first");

        Files.write(
                outputDir.resolve("__export-summary.txt"),
                lines,
                StandardCharsets.UTF_8
        );
    }
}
