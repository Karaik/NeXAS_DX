package com.giga.nexas.bsdx;

import com.giga.nexas.controller.support.HellModPackService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Hell mod 打包服务的回归测试。
 *
 * <p>这组测试验证当前编辑器复用的 pac 打包逻辑：
 * `mod` 目录通过 `PacUtil.pack(...)` 生成 `.pacNew`，
 * 再移动成 `mod/Update4.pac`。
 */
class HellModPackServiceTest {

    /**
     * 验证打包结果最终会落到 `mod/pac/Update4.pac`，
     * 并且 pac 内容 staging 只收 `mod` 根目录下的平铺非 `.pac` 文件。
     */
    @Test
    void packModDirectoryBuildsFlatStagingAndMovesPacIntoModPacFolder() throws Exception {
        Path root = Files.createTempDirectory("hell-mod-pack");
        try {
            Path mod = Files.createDirectories(root.resolve("mod"));
            Files.writeString(mod.resolve("Hell100.bin"), "data");
            Files.writeString(mod.resolve("HellConfig.dat"), "config");
            Files.writeString(mod.resolve("Update4.pac"), "old");
            Files.createDirectories(mod.resolve("pac"));
            Files.writeString(mod.resolve("pac").resolve("stale.bin"), "stale");
            Files.createDirectories(mod.resolve("nested"));
            Files.writeString(mod.resolve("nested").resolve("nested.dat"), "nested");

            HellModPackService service = new HellModPackService();
            service.setPackRunnerForTest((folderPath, compressMode) -> {
                Path sourceFolder = Path.of(folderPath);
                Assertions.assertTrue(Files.exists(sourceFolder.resolve("Hell100.bin")));
                Assertions.assertTrue(Files.exists(sourceFolder.resolve("HellConfig.dat")));
                Assertions.assertFalse(Files.exists(sourceFolder.resolve("Update4.pac")));
                Assertions.assertFalse(Files.exists(sourceFolder.resolve("nested.dat")));
                Assertions.assertFalse(Files.exists(sourceFolder.resolve("stale.bin")));
                Path pacNew = sourceFolder.resolveSibling(sourceFolder.getFileName().toString() + ".pacNew");
                Files.writeString(pacNew, "packed-" + compressMode);
                return "ok";
            });

            Path outputPac = service.packModDirectory(mod, "Update4.pac", "4");

            Assertions.assertEquals(mod.resolve("pac").resolve("Update4.pac").toAbsolutePath().normalize(), outputPac.toAbsolutePath().normalize());
            Assertions.assertTrue(Files.exists(outputPac));
            Assertions.assertEquals("packed-4", Files.readString(outputPac));
            Assertions.assertEquals("old", Files.readString(mod.resolve("Update4.pac")));
        } finally {
            deleteRecursively(root);
        }
    }

    /**
     * 清理测试目录。
     */
    private void deleteRecursively(Path root) throws Exception {
        Files.walk(root)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> path.toFile().delete());
    }
}
