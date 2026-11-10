package com.giga.nexas.bsdx;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.ResourceLayer;
import com.giga.nexas.controller.support.HellScriptDataLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Hell 脚本列表装载器的真实样本测试。
 */
class HellScriptDataLoaderTest {

    private static final Path GAME_BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");
    private static final Path GAME_DAT_DIR = Paths.get("src/main/resources/game/bsdx/dat");
    private static final String CHARSET = "windows-31j";

    private final HellScriptDataLoader loader = new HellScriptDataLoader();

    /**
     * 验证 `HellConfig.dat -> Hell.bin -> HellXXX.bin` 的索引链已经能稳定装成列表。
     */
    @Test
    void loadHellStageDescriptorsFromRealResources() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR)) {
            return;
        }

        Path flattenedRoot = Files.createTempDirectory("bsdx-hell-flat-root");
        try {
            copyMinimalFlatResource(flattenedRoot, GAME_BIN_DIR.resolve("Hell.bin"));
            copyMinimalFlatResource(flattenedRoot, GAME_BIN_DIR.resolve("__GLOBAL.bin"));
            copyMinimalFlatResource(flattenedRoot, GAME_DAT_DIR.resolve("HellConfig.dat"));

            for (String name : List.of(
                    "Hell100：ノイ.bin",
                    "Hell101：クリス.bin",
                    "Hell102：しずか.bin",
                    "Hell103：由紀江.bin",
                    "Hell352：悪夢.bin"
            )) {
                copyMinimalFlatResource(flattenedRoot, GAME_BIN_DIR.resolve(name));
            }

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(flattenedRoot);
            List<HellStageDescriptor> stages = loader.loadStages(session, CHARSET);

            Assertions.assertEquals(100, stages.size());

            HellStageDescriptor first = stages.get(0);
            Assertions.assertEquals(0, first.getIndex());
            Assertions.assertNotNull(first.getTitle());
            Assertions.assertTrue(first.getScriptFileName().startsWith("Hell"));
            Assertions.assertEquals(ResourceLayer.ROOT, first.getConfigLayer());
            Assertions.assertEquals(ResourceLayer.ROOT, first.getScriptLayer());

            HellStageDescriptor noi = stages.get(14);
            Assertions.assertEquals("世界最強のヤブ医者・ノイ", noi.getTitle());
            Assertions.assertEquals(45, noi.getMapId());
            Assertions.assertEquals("Hell100：ノイ.bin", noi.getScriptFileName());
        } finally {
            Files.walk(flattenedRoot)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> path.toFile().delete());
        }
    }

    /**
     * 把实际资源复制到临时平铺目录。
     */
    private void copyMinimalFlatResource(Path targetRoot, Path source) throws Exception {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Missing source resource: " + source);
        }
        Files.copy(source, targetRoot.resolve(source.getFileName().toString()));
    }
}
