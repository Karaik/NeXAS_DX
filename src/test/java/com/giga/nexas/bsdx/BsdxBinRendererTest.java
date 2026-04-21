package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.generator.BinGenerator;
import com.giga.nexas.dto.bsdx.bin.pseudo.recognizer.BsdxBinRecognizer;
import com.giga.nexas.dto.bsdx.bin.pseudo.renderer.BsdxBinRenderer;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BsdxBinRendererTest {

    private static final String CHARSET = "windows-31j";
    private static final Path GAME_BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");
    private static final Path PSEUDO_OUTPUT_DIR = Paths.get("src/main/resources/binBsdxPseudo");

    private final BsdxBinRenderer renderer = new BsdxBinRenderer();
    private final BsdxBinRecognizer recognizer = new BsdxBinRecognizer();
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BinGenerator binGenerator = new BinGenerator();

    @Test
    void renderPseudoRendersCallLabelsAndConstants() {
        Bin bin = new Bin();
        bin.setProperties(List.of("flag"));
        bin.setStringTable(List.of("stage_start"));
        bin.setConstants(Map.of(0, new Integer[]{0}));

        List<Bin.Instruction> instructions = new ArrayList<>();
        instructions.add(inst(0x1B, 1));
        instructions.add(inst(0, 0));
        instructions.add(inst(5, 1));
        instructions.add(inst(0, 13));
        instructions.add(inst(5, 0));
        instructions.add(inst(7, (2 << 16) | 375));
        instructions.add(inst(0, 0));
        instructions.add(inst(8, 0));
        instructions.add(inst(0x41, 9));
        instructions.add(inst(1, 0x20));
        instructions.add(inst(0, 1));
        instructions.add(inst(0x2F, 0));
        bin.setInstructions(instructions);

        String pseudo = renderer.renderPseudo(bin);

        Assertions.assertTrue(pseudo.contains("entry 1"));
        Assertions.assertTrue(pseudo.contains("syscall SEPlay(\"stage_start\", 13)"));
        Assertions.assertTrue(pseudo.contains("if(flag) jmp_direct label_0"));
        Assertions.assertTrue(pseudo.contains("label label_0"));
        Assertions.assertTrue(pseudo.contains("flag += 1"));
        Assertions.assertTrue(pseudo.contains("global[0] = {flag}"));
    }

    @Test
    void renderPseudoCompilesBackToIdenticalBinary() throws IOException {
        Bin original = new Bin();
        original.setExtensionName("bin");
        original.setCharset(CHARSET);
        original.setPreCount(0);
        original.setPreInstructions(new byte[0]);
        original.setProperties(List.of("flag"));
        original.setStringTable(List.of("stage_start"));
        original.setConstants(Map.of(0, new Integer[]{0}));

        List<Bin.Instruction> instructions = new ArrayList<>();
        instructions.add(inst(0x1B, 1));
        instructions.add(inst(0, 0));
        instructions.add(inst(5, 1));
        instructions.add(inst(0, 13));
        instructions.add(inst(5, 0));
        instructions.add(inst(7, (2 << 16) | 375));
        instructions.add(inst(0, 0));
        instructions.add(inst(8, 0));
        instructions.add(inst(0x41, 9));
        instructions.add(inst(1, 0x20));
        instructions.add(inst(0, 1));
        instructions.add(inst(0x2F, 0));
        original.setInstructions(instructions);

        String pseudo = renderer.renderPseudo(original);
        Bin compiled = recognizer.compile(original, pseudo);
        compiled.setExtensionName("bin");
        compiled.setCharset(CHARSET);

        Path tempDir = Files.createTempDirectory("bin-pseudo-roundtrip");
        try {
            Path originalPath = tempDir.resolve("original.bin");
            Path compiledPath = tempDir.resolve("compiled.bin");
            binGenerator.generate(originalPath.toString(), original, CHARSET);
            binGenerator.generate(compiledPath.toString(), compiled, CHARSET);

            byte[] originalBytes = Files.readAllBytes(originalPath);
            byte[] compiledBytes = Files.readAllBytes(compiledPath);
            Assertions.assertArrayEquals(originalBytes, compiledBytes);
        } finally {
            FileUtil.del(tempDir.toFile());
        }
    }

    /**
     * 验证 pseudo 文本只改一行时，未修改行的 IR 回放结果仍然会保留在最终输出里。
     *
     * <p>这个回归测试覆盖脚本编辑器最常见的工作流：
     * 先从 renderer 生成带 IR 注释的 pseudo，
     * 再把其中一行改成新的语义文本，
     * 最后回编后仍然保留其它未修改语句。
     */
    @Test
    void renderPseudoKeepsUnchangedLinesWhenOnlyOneLineIsEdited() {
        Bin original = new Bin();
        original.setExtensionName("bin");
        original.setCharset(CHARSET);
        original.setPreCount(0);
        original.setPreInstructions(new byte[0]);
        original.setProperties(List.of("flag"));
        original.setStringTable(List.of("stage_start"));
        original.setConstants(Map.of(0, new Integer[]{0}));

        List<Bin.Instruction> instructions = new ArrayList<>();
        instructions.add(inst(0x1B, 1));
        instructions.add(inst(0, 0));
        instructions.add(inst(5, 1));
        instructions.add(inst(0, 13));
        instructions.add(inst(5, 0));
        instructions.add(inst(7, (2 << 16) | 375));
        instructions.add(inst(0, 0));
        instructions.add(inst(8, 0));
        instructions.add(inst(0x41, 9));
        instructions.add(inst(1, 0x20));
        instructions.add(inst(0, 1));
        instructions.add(inst(0x2F, 0));
        original.setInstructions(instructions);

        String pseudo = renderer.renderPseudo(original);
        String editedPseudo = pseudo.replace("flag += 1", "flag += 2");

        Bin compiled = recognizer.compile(original, editedPseudo);
        String reRendered = renderer.renderPseudo(compiled);

        Assertions.assertEquals(original.getInstructions().size(), compiled.getInstructions().size());
        for (int i = 0; i < original.getInstructions().size(); i++) {
            Bin.Instruction originalInst = original.getInstructions().get(i);
            Bin.Instruction compiledInst = compiled.getInstructions().get(i);
            if (i == 10) {
                Assertions.assertEquals(originalInst.getOpcodeNum(), compiledInst.getOpcodeNum());
                Assertions.assertEquals(2, compiledInst.getOperandNum());
                continue;
            }
            Assertions.assertEquals(originalInst.getOpcodeNum(), compiledInst.getOpcodeNum(), "opcode mismatch at " + i);
            Assertions.assertEquals(originalInst.getOperandNum(), compiledInst.getOperandNum(), "operand mismatch at " + i);
        }
        Assertions.assertTrue(reRendered.contains("entry 1"));
        Assertions.assertTrue(reRendered.contains("syscall SEPlay(\"stage_start\", 13)"));
        Assertions.assertTrue(reRendered.contains("if(flag) jmp_direct label_0"));
        Assertions.assertTrue(reRendered.contains("label label_0"));
        Assertions.assertTrue(reRendered.contains("flag += 2"));
    }

    @Test
    void compileStandaloneSyscallWithoutMetadataKeepsScriptCsWrapperInstructions() {
        Bin template = new Bin();
        template.setExtensionName("bin");
        template.setCharset(CHARSET);
        template.setPreCount(0);
        template.setPreInstructions(new byte[0]);
        template.setInstructions(new ArrayList<>());
        template.setProperties(new ArrayList<>());
        template.setStringTable(new ArrayList<>());

        String pseudo = ""
                + "entry 1\r\n"
                + "syscall 0x0177(\"stage_start\", 13)\r\n";

        Bin compiled = recognizer.compile(template, pseudo);

        Assertions.assertEquals(9, compiled.getInstructions().size());
        Assertions.assertEquals(0x1B, compiled.getInstructions().get(0).getOpcodeNum());
        Assertions.assertEquals(1, compiled.getInstructions().get(0).getOperandNum());
        Assertions.assertEquals(0x1D, compiled.getInstructions().get(1).getOpcodeNum());
        Assertions.assertEquals(6, compiled.getInstructions().get(1).getOperandNum());
        Assertions.assertEquals(0x2C, compiled.getInstructions().get(2).getOpcodeNum());
        Assertions.assertEquals(0x7D, compiled.getInstructions().get(2).getOperandNum());
        Assertions.assertEquals(0, compiled.getInstructions().get(3).getOpcodeNum());
        Assertions.assertEquals(0, compiled.getInstructions().get(3).getOperandNum());
        Assertions.assertEquals(5, compiled.getInstructions().get(4).getOpcodeNum());
        Assertions.assertEquals(1, compiled.getInstructions().get(4).getOperandNum());
        Assertions.assertEquals(0, compiled.getInstructions().get(5).getOpcodeNum());
        Assertions.assertEquals(13, compiled.getInstructions().get(5).getOperandNum());
        Assertions.assertEquals(5, compiled.getInstructions().get(6).getOpcodeNum());
        Assertions.assertEquals(0, compiled.getInstructions().get(6).getOperandNum());
        Assertions.assertEquals(7, compiled.getInstructions().get(7).getOpcodeNum());
        Assertions.assertEquals((2 << 16) | 0x0177, compiled.getInstructions().get(7).getOperandNum());
        Assertions.assertEquals(0x1D, compiled.getInstructions().get(8).getOpcodeNum());
        Assertions.assertEquals(7, compiled.getInstructions().get(8).getOperandNum());
    }

    /**
     * 验证 BSDX `bin` 解析时已经自动注入同目录 `__GLOBAL.bin` 的符号表。
     *
     * <p>这一步是符号化伪代码显示的前提：如果 service 没先加载全局环境，
     * renderer 就只能退回到裸索引，无法稳定显示全局符号别名。
     */
    @Test
    void bsdxServiceLoadsGlobalSymbolsBeforeNormalBinParsing() throws Exception {
        Path sample = GAME_BIN_DIR.resolve("Hell.bin");
        if (!Files.exists(sample)) {
            return;
        }

        ResponseDTO<?> dto = bsdxBinService.parse(sample.toString(), CHARSET);
        Bin bin = (Bin) dto.getData();

        Assertions.assertNotNull(bin.getGlobalSymbols(), "Global symbols should be injected before normal bin parsing.");
        Assertions.assertEquals(442, bin.getGlobalSymbols().size(), "Unexpected __GLOBAL symbol count.");
        Assertions.assertEquals("LUCK_TABLE", bin.getGlobalSymbols().get(0));
    }

    /**
     * 验证当伪代码里显示的是全局符号别名时，依然能够无损回编。
     *
     * <p>这里不依赖真实样本，而是用最小可控样本验证：
     * `global[g0_xxx] = {g76_xxx}` 这样的文本能被 recognizer 精确还原回原始索引。
     */
    @Test
    void symbolicGlobalNamesStillCompileBackToIdenticalBinary() throws IOException {
        Bin original = new Bin();
        original.setExtensionName("bin");
        original.setCharset(CHARSET);
        original.setPreCount(0);
        original.setPreInstructions(new byte[0]);
        original.setGlobalSymbols(buildSyntheticGlobalSymbols(77));
        original.setConstants(Map.of(0, new Integer[]{76}));
        original.setInstructions(new ArrayList<>());

        String pseudo = renderer.renderPseudo(original);
        Assertions.assertTrue(pseudo.contains("g0_LUCK_TABLE"));
        Assertions.assertTrue(pseudo.contains("g76_ATK_NANOHA_12"));

        Bin compiled = recognizer.compile(original, pseudo);
        compiled.setExtensionName("bin");
        compiled.setCharset(CHARSET);

        Path tempDir = Files.createTempDirectory("bin-symbolic-global-roundtrip");
        try {
            Path originalPath = tempDir.resolve("original.bin");
            Path compiledPath = tempDir.resolve("compiled.bin");
            binGenerator.generate(originalPath.toString(), original, CHARSET);
            binGenerator.generate(compiledPath.toString(), compiled, CHARSET);

            byte[] originalBytes = Files.readAllBytes(originalPath);
            byte[] compiledBytes = Files.readAllBytes(compiledPath);
            Assertions.assertArrayEquals(originalBytes, compiledBytes);
        } finally {
            FileUtil.del(tempDir.toFile());
        }
    }

    @Test
    void hellStageBinsStrictRoundTripToIdenticalBinary() throws IOException {
        if (!Files.exists(GAME_BIN_DIR)) {
            return;
        }

        List<String> failures = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("hell-bin-roundtrip");
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_BIN_DIR, "Hell*.bin")) {
                for (Path path : stream) {
                    String fileName = path.getFileName().toString();
                    try {
                        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                        Bin original = (Bin) dto.getData();
                        if (original == null) {
                            failures.add(fileName + " parse returned null");
                            continue;
                        }

                        original.setExtensionName("bin");
                        original.setCharset(CHARSET);
                        String pseudo = renderer.renderStrictPseudo(original);
                        Bin compiled = recognizer.compileStrict(original, pseudo);
                        compiled.setExtensionName("bin");
                        compiled.setCharset(CHARSET);

                        Path compiledPath = tempDir.resolve(fileName);
                        binGenerator.generate(compiledPath.toString(), compiled, CHARSET);

                        byte[] originalBytes = Files.readAllBytes(path);
                        byte[] compiledBytes = Files.readAllBytes(compiledPath);
                        if (!java.util.Arrays.equals(originalBytes, compiledBytes)) {
                            failures.add(fileName + " binary mismatch");
                        }
                    } catch (Exception ex) {
                        failures.add(fileName + " " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                }
            }
        } finally {
            FileUtil.del(tempDir.toFile());
        }

        Assertions.assertTrue(
                failures.isEmpty(),
                "Hell stage round-trip failures:\n" + failures.stream().collect(Collectors.joining("\n"))
        );
    }

    @Test
    void hellStageBinsPseudoRoundTripToIdenticalBinary() throws IOException {
        if (!Files.exists(GAME_BIN_DIR)) {
            return;
        }

        List<String> failures = new ArrayList<>();
        Path tempDir = Files.createTempDirectory("hell-bin-pseudo-roundtrip");
        try {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_BIN_DIR, "Hell*.bin")) {
                for (Path path : stream) {
                    String fileName = path.getFileName().toString();
                    try {
                        ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                        Bin original = (Bin) dto.getData();
                        if (original == null) {
                            failures.add(fileName + " parse returned null");
                            continue;
                        }

                        original.setExtensionName("bin");
                        original.setCharset(CHARSET);
                        String pseudo = renderer.renderPseudo(original);
                        Bin compiled = recognizer.compile(original, pseudo);
                        compiled.setExtensionName("bin");
                        compiled.setCharset(CHARSET);

                        Path compiledPath = tempDir.resolve(fileName);
                        binGenerator.generate(compiledPath.toString(), compiled, CHARSET);

                        byte[] originalBytes = Files.readAllBytes(path);
                        byte[] compiledBytes = Files.readAllBytes(compiledPath);
                        if (!java.util.Arrays.equals(originalBytes, compiledBytes)) {
                            failures.add(fileName + " binary mismatch");
                        }
                    } catch (Exception ex) {
                        failures.add(fileName + " " + ex.getClass().getSimpleName() + ": " + ex.getMessage());
                    }
                }
            }
        } finally {
            FileUtil.del(tempDir.toFile());
        }

        Assertions.assertTrue(
                failures.isEmpty(),
                "Hell stage pseudo round-trip failures:\n" + failures.stream().collect(Collectors.joining("\n"))
        );
    }

    @Test
    @Disabled
    void exportAllBinsAsPseudoCode() throws IOException {
        if (!Files.exists(GAME_BIN_DIR)) {
            return;
        }

        Files.createDirectories(PSEUDO_OUTPUT_DIR);
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(GAME_BIN_DIR, "*.bin")) {
            for (Path path : stream) {
                String fileName = URLDecoder.decode(path.getFileName().toString(), StandardCharsets.UTF_8);
                String baseName = fileName.substring(0, fileName.lastIndexOf('.'));
                if ("__GLOBAL".equalsIgnoreCase(baseName)) {
                    continue;
                }

                ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), CHARSET);
                Bin bin = (Bin) dto.getData();
                String pseudo = renderer.renderPseudo(bin);
                FileUtil.writeUtf8String(pseudo, PSEUDO_OUTPUT_DIR.resolve(baseName + ".txt").toFile());
            }
        }
    }

    private Bin.Instruction inst(int opcodeNum, int operandNum) {
        Bin.Instruction inst = new Bin.Instruction();
        inst.setOpcodeNum(opcodeNum);
        inst.setOperandNum(operandNum);
        return inst;
    }

    /**
     * 构造一个最小的全局符号表样本，专门用于验证符号别名显示与回编。
     */
    private List<String> buildSyntheticGlobalSymbols(int size) {
        List<String> symbols = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            symbols.add("GLOBAL_" + i);
        }
        symbols.set(0, "LUCK_TABLE");
        symbols.set(76, "ATK_NANOHA[12]");
        return symbols;
    }
}
