package com.giga.nexas.controller.support;

import com.giga.nexas.util.PacUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Hell 编辑器的 mod 打包服务。
 *
 * <p>这一层属于编辑器输出层。
 * 它直接复用现有 `PacUtil.pack(...)` 的打包流程，
 * 只负责把 `mod` 目录打成 `Update4.pac`，
 * 不自行发明新的 pac 格式或参数协议。
 *
 * <p>输入是 `mod` 目录、输出 pac 文件名和压缩模式；
 * 输出是最终落到 `mod` 根目录里的 pac 路径。
 */
public class HellModPackService {

    private static final String PAC_OUTPUT_DIRECTORY_NAME = "pac";

    /**
     * 等待 `PacUtil.pack(...)` 生成 `.pacNew` 的最长时间。
     *
     * <p>当前只处理文件系统落盘延迟，不承担业务超时语义。
     */
    private static final long PAC_NEW_WAIT_TIMEOUT_MS = 5000;

    /**
     * 真实打包执行器。
     *
     * <p>默认直接调用 `PacUtil.pack(...)`，
     * 测试里可以替换成伪实现来稳定验证 GUI 行为。
     */
    private PacPackRunner packRunner = PacUtil::pack;

    /**
     * 把指定 `mod` 目录打成目标 pac。
     *
     * <p>当前打包输入不是整个 `mod` 目录本身，而是一个临时 staging 目录。
     * staging 目录只收：
     * 1. `mod` 根目录下的平铺文件
     * 2. 不是目录
     * 3. 不是 `.pac` 文件
     *
     * <p>最终输出固定落到 `mod/pac/<outputPacFileName>`。
     */
    public Path packModDirectory(Path modDirectory, String outputPacFileName, String compressMode) {
        if (modDirectory == null) {
            throw new IllegalArgumentException("Mod directory must not be null.");
        }
        if (!Files.isDirectory(modDirectory)) {
            throw new IllegalArgumentException("Mod directory does not exist: " + modDirectory);
        }
        if (outputPacFileName == null || outputPacFileName.isBlank()) {
            throw new IllegalArgumentException("Output pac file name must not be blank.");
        }

        Path normalizedModDirectory = modDirectory.toAbsolutePath().normalize();
        Path outputDirectory = normalizedModDirectory.resolve(PAC_OUTPUT_DIRECTORY_NAME);

        try {
            Files.createDirectories(outputDirectory);
            Path outputPac = outputDirectory.resolve(outputPacFileName);
            Files.deleteIfExists(outputPac);

            Path stagingDirectory = Files.createTempDirectory("hell-mod-pack-staging");
            try {
                copyPackableFlatFiles(normalizedModDirectory, stagingDirectory);

                Path pacNew = stagingDirectory.resolveSibling(stagingDirectory.getFileName().toString() + ".pacNew");
                Files.deleteIfExists(pacNew);

                packRunner.pack(stagingDirectory.toString(), compressMode);
                if (!waitForFile(pacNew, PAC_NEW_WAIT_TIMEOUT_MS)) {
                    throw new IllegalStateException("PacUtil.pack executed, but pacNew was not found: " + pacNew);
                }

                Files.move(pacNew, outputPac, StandardCopyOption.REPLACE_EXISTING);
            } finally {
                deleteRecursively(stagingDirectory);
            }
            return outputPac;
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to pack mod directory into " + outputPacFileName, ex);
        }
    }

    /**
     * 测试用覆盖执行器。
     */
    public void setPackRunnerForTest(PacPackRunner packRunner) {
        this.packRunner = packRunner == null ? PacUtil::pack : packRunner;
    }

    /**
     * 把 `mod` 根目录下允许进入 pac 的平铺文件复制到 staging。
     *
     * <p>这里只处理一层，不向子目录递归。
     * `mod/pac` 这类目录不会进入 pac 内容。
     */
    private void copyPackableFlatFiles(Path modDirectory, Path stagingDirectory) throws IOException {
        try (var stream = Files.list(modDirectory)) {
            List<Path> flatFiles = stream
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.getFileName().toString().toLowerCase().endsWith(".pac"))
                    .toList();

            for (Path file : flatFiles) {
                Files.copy(
                        file,
                        stagingDirectory.resolve(file.getFileName().toString()),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        }
    }

    private boolean waitForFile(Path path, long timeoutMs) {
        long deadline = System.currentTimeMillis() + timeoutMs;
        while (System.currentTimeMillis() < deadline) {
            if (Files.exists(path)) {
                return true;
            }
            try {
                Thread.sleep(100);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                return Files.exists(path);
            }
        }
        return Files.exists(path);
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        Files.walk(root)
                .sorted((a, b) -> b.getNameCount() - a.getNameCount())
                .forEach(path -> path.toFile().delete());
    }

    @FunctionalInterface
    public interface PacPackRunner {
        String pack(String folderPath, String compressMode) throws IOException;
    }
}
