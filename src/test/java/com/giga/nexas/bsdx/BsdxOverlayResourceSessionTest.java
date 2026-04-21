package com.giga.nexas.bsdx;

import com.giga.nexas.controller.model.BsdxOverlayResourceSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * `root + mod` 覆盖会话的回归测试。
 */
class BsdxOverlayResourceSessionTest {

    /**
     * 验证读取优先级与可写路径策略：
     * 读取优先命中 `mod`，写入始终落到 `mod`。
     */
    @Test
    void modLayerOverridesRootAndBecomesWritableTarget() throws Exception {
        Path root = Files.createTempDirectory("bsdx-overlay-root");
        try {
            Files.writeString(root.resolve("Hell.bin"), "root", StandardCharsets.UTF_8);

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(root);
            Assertions.assertEquals(root.resolve("Hell.bin").toAbsolutePath().normalize(), session.resolveReadPath("Hell.bin").orElseThrow());

            Path writable = session.prepareWritablePath("Hell.bin");
            Assertions.assertTrue(writable.startsWith(root.resolve("mod")));
            Assertions.assertTrue(Files.exists(writable));

            Files.writeString(writable, "mod", StandardCharsets.UTF_8);
            BsdxOverlayResourceSession refreshed = session.refresh();
            Assertions.assertTrue(refreshed.isOverridden("Hell.bin"));
            Assertions.assertEquals(writable.toAbsolutePath().normalize(), refreshed.resolveReadPath("Hell.bin").orElseThrow());
        } finally {
            Files.walk(root)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> path.toFile().delete());
        }
    }

    /**
     * 验证 `mod` 覆盖规则对所有平铺文件生效，不只针对 `bin`。
     */
    @Test
    void modLayerOverridesNonBinFilesToo() throws Exception {
        Path root = Files.createTempDirectory("bsdx-overlay-root-all-files");
        try {
            Files.writeString(root.resolve("HellConfig.dat"), "root-dat", StandardCharsets.UTF_8);
            Path modDir = Files.createDirectories(root.resolve("mod"));
            Files.writeString(modDir.resolve("HellConfig.dat"), "mod-dat", StandardCharsets.UTF_8);

            BsdxOverlayResourceSession session = BsdxOverlayResourceSession.open(root);

            Assertions.assertTrue(session.isOverridden("HellConfig.dat"));
            Assertions.assertEquals(
                    modDir.resolve("HellConfig.dat").toAbsolutePath().normalize(),
                    session.resolveReadPath("HellConfig.dat").orElseThrow()
            );
        } finally {
            Files.walk(root)
                    .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                    .forEach(path -> path.toFile().delete());
        }
    }
}
