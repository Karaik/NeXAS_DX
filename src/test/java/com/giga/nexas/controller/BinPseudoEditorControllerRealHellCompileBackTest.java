package com.giga.nexas.controller;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.generator.BinGenerator;
import com.giga.nexas.dto.bsdx.bin.pseudo.recognizer.BsdxBinRecognizer;
import com.giga.nexas.dto.bsdx.bin.pseudo.renderer.BsdxBinRenderer;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

class BinPseudoEditorControllerRealHellCompileBackTest {

    private static final String CHARSET = "windows-31j";
    private static final Path GAME_BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");

    private final BsdxBinService service = new BsdxBinService();
    private final BsdxBinRenderer renderer = new BsdxBinRenderer();
    private final BsdxBinRecognizer recognizer = new BsdxBinRecognizer();
    private final BinGenerator generator = new BinGenerator();

    @Test
    void hell801ThreeMekIndexEditsOnlyChangeThreeBytes() throws Exception {
        if (!Files.exists(GAME_BIN_DIR)) {
            return;
        }

        Path source;
        try (var stream = Files.list(GAME_BIN_DIR)) {
            source = stream
                    .filter(path -> path.getFileName().toString().startsWith("Hell801"))
                    .findFirst()
                    .orElse(null);
        }
        if (source == null) {
            return;
        }

        ResponseDTO<?> dto = service.parse(source.toString(), CHARSET);
        Bin original = (Bin) dto.getData();
        original.setExtensionName("bin");
        original.setCharset(CHARSET);

        String lossless = renderer.renderPseudo(original);
        String visible = BinPseudoEditorController.stripPseudoMetadataStatic(lossless);
        String edited = visible;
        edited = edited.replaceFirst("LoadMek\\(0, 127", "LoadMek(25, 127");
        edited = edited.replaceFirst("LoadMekAlt\\(0, 127", "LoadMekAlt(25, 127");
        edited = edited.replaceFirst("InitDeployMek\\(1, 2, 0, 10", "InitDeployMek(1, 2, 25, 10");

        String compileSource = BinPseudoEditorController.buildPseudoSourceForCompile(edited, lossless);
        Bin compiled = recognizer.compile(original, compileSource);
        compiled.setExtensionName("bin");
        compiled.setCharset(CHARSET);

        Path tempDir = Files.createTempDirectory("hell801-compile-back");
        try {
            Path output = tempDir.resolve("HellMOD001.bin");
            generator.generate(output.toString(), compiled, CHARSET);

            byte[] originalBytes = Files.readAllBytes(source);
            byte[] compiledBytes = Files.readAllBytes(output);
            Assertions.assertEquals(originalBytes.length, compiledBytes.length);

            List<Integer> diffOffsets = new ArrayList<>();
            for (int i = 0; i < originalBytes.length; i++) {
                if (originalBytes[i] != compiledBytes[i]) {
                    diffOffsets.add(i);
                }
            }

            Assertions.assertEquals(List.of(28, 540, 1428), diffOffsets);
            for (Integer offset : diffOffsets) {
                Assertions.assertEquals(0, originalBytes[offset]);
                Assertions.assertEquals(25, compiledBytes[offset] & 0xFF);
            }
        } finally {
            Files.walk(tempDir)
                    .sorted((left, right) -> right.compareTo(left))
                    .forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (Exception ignored) {
                        }
                    });
        }
    }
}
