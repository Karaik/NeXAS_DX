package com.giga.nexas.bsdx;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.HellStageMetadataDraft;
import com.giga.nexas.controller.model.ResourceLayer;
import com.giga.nexas.controller.support.HellScriptDataLoader;
import com.giga.nexas.controller.support.HellScriptEditorService;
import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.generator.BinGenerator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessPipeline;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIr;
import com.giga.nexas.service.BsdxBinService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * Hell Script Editor 保存链路的真实资源回归测试。
 *
 * <p>当前重点验证 Stage 2：
 * 右侧元数据保存时只写 `mod/HellConfig.dat`，
 * 并且刷新覆盖会话后，关卡列表已经能读到保存后的新值。
 */
class HellScriptEditorServiceTest {

    private static final Path GAME_BIN_DIR = Paths.get("src/main/resources/game/bsdx/bin");
    private static final Path GAME_DAT_DIR = Paths.get("src/main/resources/game/bsdx/dat");
    private static final String CHARSET = "windows-31j";

    private final HellScriptEditorService service = new HellScriptEditorService();
    private final HellScriptDataLoader loader = new HellScriptDataLoader();
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BsdxBinLosslessPipeline losslessPipeline = new BsdxBinLosslessPipeline();
    private final BinGenerator binGenerator = new BinGenerator();

    /**
     * 验证元数据保存时：
     * 1. 根目录 `HellConfig.dat` 字节不变
     * 2. 编辑结果只落到 `mod/HellConfig.dat`
     * 3. 刷新覆盖会话后，loader 命中的配置来源已经变成 `MOD`
     */
    @Test
    void saveMetadataWritesOnlyModAndReloadsFromOverlay() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR)) {
            return;
        }

        Path flattenedRoot = createFlatRoot("Hell100");
        try {
            Path rootConfig = flattenedRoot.resolve("HellConfig.dat");
            byte[] originalRootBytes = Files.readAllBytes(rootConfig);

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(flattenedRoot);
            List<HellStageDescriptor> before = loader.loadStages(session, CHARSET);
            HellStageDescriptor blueprint = before.get(14);

            HellStageMetadataDraft draft = HellStageMetadataDraft.builder()
                    .index(blueprint.getIndex())
                    .title(blueprint.getTitle() + " Copy")
                    .description("Edited in Stage 2 test.")
                    .hellLevel(77)
                    .mapId(88)
                    .shopId(19)
                    .portraitId(20)
                    .balloonStyleId(21)
                    .enemyTypes(List.of(11, 12, 13, 14, 15, 16, 17, 18))
                    .enemyCounts(List.of(1, 2, 3, 4, 5, 6, 7, 8))
                    .build();

            BsdxOverlayResourceSession refreshed = service.saveMetadata(session, CHARSET, draft);
            List<HellStageDescriptor> after = loader.loadStages(refreshed, CHARSET);
            HellStageDescriptor edited = after.get(14);

            Assertions.assertArrayEquals(originalRootBytes, Files.readAllBytes(rootConfig));
            Assertions.assertTrue(Files.exists(flattenedRoot.resolve("mod").resolve("HellConfig.dat")));
            Assertions.assertEquals(draft.getTitle(), edited.getTitle());
            Assertions.assertEquals(draft.getDescription(), edited.getDescription());
            Assertions.assertEquals(draft.getMapId(), edited.getMapId());
            Assertions.assertEquals(draft.getHellLevel(), edited.getHellLevel());
            Assertions.assertEquals(draft.getEnemyTypes(), edited.getEnemyTypes());
            Assertions.assertEquals(draft.getEnemyCounts(), edited.getEnemyCounts());
            Assertions.assertEquals(ResourceLayer.MOD, edited.getConfigLayer());
            Assertions.assertEquals(ResourceLayer.ROOT, edited.getScriptLayer());
        } finally {
            deleteRecursively(flattenedRoot);
        }
    }

    /**
     * 验证保存后即使重新打开覆盖会话，读取到的仍然是 `mod` 覆盖结果。
     *
     * <p>这条回归测试覆盖 Stage 3 的核心前提：
     * 保存并关闭后，下一次加载不会回退到 root 原件。
     */
    @Test
    void savedMetadataStillOverridesRootAfterSessionReopen() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR)) {
            return;
        }

        Path flattenedRoot = createFlatRoot("Hell100");
        try {
            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(flattenedRoot);
            List<HellStageDescriptor> before = loader.loadStages(session, CHARSET);
            HellStageDescriptor blueprint = before.get(14);

            HellStageMetadataDraft draft = HellStageMetadataDraft.builder()
                    .index(blueprint.getIndex())
                    .title("Reopen Check")
                    .description(blueprint.getDescription())
                    .hellLevel(blueprint.getHellLevel())
                    .mapId(blueprint.getMapId())
                    .shopId(blueprint.getShopId())
                    .portraitId(blueprint.getPortraitId())
                    .balloonStyleId(blueprint.getBalloonStyleId())
                    .enemyTypes(blueprint.getEnemyTypes())
                    .enemyCounts(blueprint.getEnemyCounts())
                    .build();

            service.saveMetadata(session, CHARSET, draft);

            BsdxOverlayResourceSession reopened = BsdxOverlayResourceSession.open(flattenedRoot);
            HellStageDescriptor reopenedStage = loader.loadStages(reopened, CHARSET).get(14);

            Assertions.assertEquals("Reopen Check", reopenedStage.getTitle());
            Assertions.assertEquals(ResourceLayer.MOD, reopenedStage.getConfigLayer());
        } finally {
            deleteRecursively(flattenedRoot);
        }
    }

    /**
     * 验证 `saveMetadata` 写入的日文字段会以 `windows-31j` 落盘到 `mod/HellConfig.dat`。
     *
     * <p>这条测试直接覆盖服务层保存链，不经过 GUI。
     * 断言同时检查：
     * 1. 重新解析后的字符串值正确
     * 2. `mod/HellConfig.dat` 原始 bytes 里出现的是 `windows-31j` 编码序列
     * 3. 根目录原始文件 bytes 保持不变
     */
    @Test
    void saveMetadataWritesJapaneseStringsUsingWindows31j() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR)) {
            return;
        }

        Path flattenedRoot = createFlatRoot("Hell100");
        try {
            byte[] rootConfigBytes = Files.readAllBytes(flattenedRoot.resolve("HellConfig.dat"));
            String japaneseTitle = "テスト関門名";
            String japaneseDescription = "これは日本語の説明です。";

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(flattenedRoot);
            HellStageDescriptor blueprint = loader.loadStages(session, CHARSET).get(14);

            HellStageMetadataDraft draft = HellStageMetadataDraft.builder()
                    .index(blueprint.getIndex())
                    .title(japaneseTitle)
                    .description(japaneseDescription)
                    .hellLevel(blueprint.getHellLevel())
                    .mapId(blueprint.getMapId())
                    .shopId(blueprint.getShopId())
                    .portraitId(blueprint.getPortraitId())
                    .balloonStyleId(blueprint.getBalloonStyleId())
                    .enemyTypes(blueprint.getEnemyTypes())
                    .enemyCounts(blueprint.getEnemyCounts())
                    .build();

            BsdxOverlayResourceSession refreshed = service.saveMetadata(session, CHARSET, draft);
            HellStageDescriptor edited = loader.loadStages(refreshed, CHARSET).get(14);
            Path modConfig = flattenedRoot.resolve("mod").resolve("HellConfig.dat");
            byte[] modBytes = Files.readAllBytes(modConfig);

            Assertions.assertEquals(japaneseTitle, edited.getTitle());
            Assertions.assertEquals(japaneseDescription, edited.getDescription());
            Assertions.assertArrayEquals(rootConfigBytes, Files.readAllBytes(flattenedRoot.resolve("HellConfig.dat")));
            Assertions.assertTrue(containsBytes(modBytes, japaneseTitle.getBytes(Charset.forName(CHARSET))));
            Assertions.assertTrue(containsBytes(modBytes, japaneseDescription.getBytes(Charset.forName(CHARSET))));
            Assertions.assertFalse(containsBytes(modBytes, japaneseTitle.getBytes(StandardCharsets.UTF_8)));
            Assertions.assertFalse(containsBytes(modBytes, japaneseDescription.getBytes(StandardCharsets.UTF_8)));
        } finally {
            deleteRecursively(flattenedRoot);
        }
    }

    /**
     * 验证蓝本复制会同时补齐：
     * 1. `HellConfig.dat` 新行
     * 2. 新的 `mod/HellMODxxx.bin`
     * 3. `Hell.bin` 的字符串表与显式 dispatch
     *
     * <p>同时再对新生成的 `mod/Hell.bin` 和新脚本各做一次
     * `bin -> pseudo -> bin` 字节闭环，防止复制逻辑把脚本主链破坏掉。
     */
    @Test
    void duplicateStageCreatesLoadableModEntryAndKeepsScriptRoundTripStable() throws Exception {
        if (!Files.isDirectory(GAME_BIN_DIR) || !Files.isDirectory(GAME_DAT_DIR)) {
            return;
        }

        Path flattenedRoot = createFlatRoot("Hell100");
        try {
            byte[] rootConfigBytes = Files.readAllBytes(flattenedRoot.resolve("HellConfig.dat"));
            byte[] rootHellBinBytes = Files.readAllBytes(flattenedRoot.resolve("Hell.bin"));

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(flattenedRoot);
            List<HellStageDescriptor> before = loader.loadStages(session, CHARSET);
            HellStageDescriptor blueprint = before.get(14);

            HellScriptEditorService.DuplicateStageResult result = service.duplicateStage(session, CHARSET, blueprint);
            List<HellStageDescriptor> after = loader.loadStages(result.session(), CHARSET);
            HellStageDescriptor duplicated = after.get(result.stageIndex());

            Assertions.assertEquals(before.size() + 1, after.size());
            Assertions.assertEquals(after.size() - 1, result.stageIndex());
            Assertions.assertEquals(result.scriptFileName(), duplicated.getScriptFileName());
            Assertions.assertTrue(duplicated.getTitle().endsWith(" Copy"));
            Assertions.assertEquals(ResourceLayer.MOD, duplicated.getConfigLayer());
            Assertions.assertEquals(ResourceLayer.MOD, duplicated.getScriptLayer());
            Assertions.assertTrue(Files.exists(flattenedRoot.resolve("mod").resolve(result.scriptFileName())));
            Assertions.assertTrue(Files.exists(flattenedRoot.resolve("mod").resolve("Hell.bin")));
            Assertions.assertArrayEquals(rootConfigBytes, Files.readAllBytes(flattenedRoot.resolve("HellConfig.dat")));
            Assertions.assertArrayEquals(rootHellBinBytes, Files.readAllBytes(flattenedRoot.resolve("Hell.bin")));
            Assertions.assertArrayEquals(
                    Files.readAllBytes(blueprint.getActiveScriptPath()),
                    Files.readAllBytes(flattenedRoot.resolve("mod").resolve(result.scriptFileName()))
            );

            Bin rootHellBin = parseBin(flattenedRoot.resolve("Hell.bin"));
            Bin modHellBin = parseBin(flattenedRoot.resolve("mod").resolve("Hell.bin"));
            Assertions.assertEquals(maxMarker(rootHellBin), maxMarker(modHellBin));
            BsdxBinLosslessProgram hellProgram = losslessPipeline.lift(losslessPipeline.toStrictIr(modHellBin));
            String hellDsl = losslessPipeline.render(hellProgram);
            Assertions.assertTrue(hellDsl.contains("if (r1 == 101) goto"));
            Assertions.assertTrue(hellDsl.contains("call CallScript(str[103], var[0]"));

            assertBinaryRoundTrip(flattenedRoot.resolve("mod").resolve("Hell.bin"));
            assertBinaryRoundTrip(flattenedRoot.resolve("mod").resolve(result.scriptFileName()));
        } finally {
            deleteRecursively(flattenedRoot);
        }
    }

    /**
     * 创建符合当前编辑器假设的 BSDX 平铺资源目录。
     *
     * <p>这里只复制本轮测试需要的最小资源集合：
     * `Hell.bin`、`__GLOBAL.bin`、`HellConfig.dat` 和指定蓝本脚本。
     */
    private Path createFlatRoot(String... requiredScriptPrefixes) throws Exception {
        Path flattenedRoot = Files.createTempDirectory("bsdx-hell-editor-root");
        copyRequired(flattenedRoot, GAME_BIN_DIR.resolve("Hell.bin"));
        copyRequired(flattenedRoot, GAME_BIN_DIR.resolve("__GLOBAL.bin"));
        copyRequired(flattenedRoot, GAME_DAT_DIR.resolve("HellConfig.dat"));

        for (String prefix : requiredScriptPrefixes) {
            copyBinByPrefix(flattenedRoot, prefix);
        }
        return flattenedRoot;
    }

    /**
     * 通过稳定前缀复制真实样本脚本，避免在测试源码里硬编码整段非 ASCII 文件名。
     */
    private void copyBinByPrefix(Path flattenedRoot, String prefix) throws Exception {
        try (var stream = Files.list(GAME_BIN_DIR)) {
            Path source = stream
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Missing test bin with prefix: " + prefix));
            copyRequired(flattenedRoot, source);
        }
    }

    /**
     * 把一个真实资源复制到测试平铺目录。
     */
    private void copyRequired(Path flattenedRoot, Path source) throws Exception {
        if (!Files.exists(source)) {
            throw new IllegalArgumentException("Missing source resource: " + source);
        }
        Files.copy(source, flattenedRoot.resolve(source.getFileName().toString()));
    }

    /**
     * 清理测试生成的平铺资源目录。
     */
    private void deleteRecursively(Path root) throws Exception {
        Files.walk(root)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> path.toFile().delete());
    }

    /**
     * 通过现有 service 层解析真实或测试生成的 BIN。
     */
    private Bin parseBin(Path path) throws Exception {
        ResponseDTO<?> response = bsdxBinService.parse(path.toString(), CHARSET);
        Bin bin = (Bin) response.getData();
        bin.setExtensionName("bin");
        bin.setCharset(CHARSET);
        return bin;
    }

    /**
     * 对单个脚本执行完整的 `bin -> pseudo -> bin` 字节闭环断言。
     */
    private void assertBinaryRoundTrip(Path originalPath) throws Exception {
        Bin original = parseBin(originalPath);
        BsdxBinStrictIr strictIr = losslessPipeline.toStrictIr(original);
        BsdxBinLosslessProgram program = losslessPipeline.lift(strictIr);
        BsdxBinStrictIr reparsedStrictIr = losslessPipeline.lower(losslessPipeline.parse(program, losslessPipeline.render(program)));
        Bin rebuilt = losslessPipeline.fromStrictIr(reparsedStrictIr);
        rebuilt.setExtensionName("bin");
        rebuilt.setCharset(CHARSET);

        Path tempDir = Files.createTempDirectory("hell-editor-roundtrip");
        try {
            Path rebuiltPath = tempDir.resolve(originalPath.getFileName().toString());
            binGenerator.generate(rebuiltPath.toString(), rebuilt, CHARSET);
            Assertions.assertArrayEquals(Files.readAllBytes(originalPath), Files.readAllBytes(rebuiltPath));
        } finally {
            FileUtil.del(tempDir.toFile());
        }
    }

    /**
     * 判断原始字节数组里是否出现了指定子序列。
     */
    private boolean containsBytes(byte[] source, byte[] target) {
        if (source == null || target == null || target.length == 0 || target.length > source.length) {
            return false;
        }
        for (int start = 0; start <= source.length - target.length; start++) {
            boolean matched = true;
            for (int offset = 0; offset < target.length; offset++) {
                if (source[start + offset] != target[offset]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return true;
            }
        }
        return false;
    }

    private int maxMarker(Bin bin) {
        BsdxBinLosslessProgram program = losslessPipeline.lift(losslessPipeline.toStrictIr(bin));
        return program.statements().stream()
                .filter(BsdxBinLosslessProgram.MarkerStmt.class::isInstance)
                .map(BsdxBinLosslessProgram.MarkerStmt.class::cast)
                .map(BsdxBinLosslessProgram.MarkerStmt::marker)
                .max(Integer::compareTo)
                .orElse(-1);
    }
}
