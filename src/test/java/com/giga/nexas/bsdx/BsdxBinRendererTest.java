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
}
