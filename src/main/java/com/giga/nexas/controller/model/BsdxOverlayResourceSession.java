package com.giga.nexas.controller.model;

import lombok.Getter;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * BSDX 平铺资源目录的会话模型。
 *
 * <p>这个模型处理两层目录：
 * 根资源目录，以及其下的 `mod` 覆盖目录。
 * 读取顺序是 `mod` 后 `root`，写入目标是 `mod`。
 */
@Getter
public class BsdxOverlayResourceSession {

    /**
     * 原始资源根目录。
     */
    private final Path rootDirectory;

    /**
     * 覆盖层目录。
     */
    private final Path modDirectory;

    /**
     * 根目录平铺文件索引。
     */
    private final Map<String, Path> rootFiles;

    /**
     * mod 目录平铺文件索引。
     */
    private final Map<String, Path> modFiles;

    private BsdxOverlayResourceSession(Path rootDirectory,
                                       Path modDirectory,
                                       Map<String, Path> rootFiles,
                                       Map<String, Path> modFiles) {
        this.rootDirectory = rootDirectory;
        this.modDirectory = modDirectory;
        this.rootFiles = rootFiles;
        this.modFiles = modFiles;
    }

    /**
     * 基于平铺资源目录创建会话。
     */
    public static BsdxOverlayResourceSession open(Path rootDirectory) throws IOException {
        Path normalizedRoot = rootDirectory.toAbsolutePath().normalize();
        Path modDirectory = normalizedRoot.resolve("mod");
        Files.createDirectories(modDirectory);

        return new BsdxOverlayResourceSession(
                normalizedRoot,
                modDirectory,
                indexFlatFiles(normalizedRoot, false),
                indexFlatFiles(modDirectory, true)
        );
    }

    /**
     * 刷新根目录与 mod 目录的平铺索引。
     */
    public BsdxOverlayResourceSession refresh() throws IOException {
        return open(rootDirectory);
    }

    /**
     * 解析读取路径。
     *
     * <p>返回顺序是 `mod` 后 `root`。
     */
    public Optional<Path> resolveReadPath(String fileName) {
        String key = normalizeFileName(fileName);
        Path mod = modFiles.get(key);
        if (mod != null) {
            return Optional.of(mod);
        }
        return Optional.ofNullable(rootFiles.get(key));
    }

    /**
     * 判断某个文件当前是否由 `mod` 覆盖。
     */
    public boolean isOverridden(String fileName) {
        return modFiles.containsKey(normalizeFileName(fileName));
    }

    /**
     * 判断某个文件名是否已经存在于当前覆盖会话。
     *
     * <p>这里同时检查 `mod` 与 `root` 两层，
     * 用于蓝本复制时为新脚本分配一个不会撞名的目标文件名。
     */
    public boolean exists(String fileName) {
        String normalized = normalizeFileName(fileName);
        return modFiles.containsKey(normalized) || rootFiles.containsKey(normalized);
    }

    /**
     * 为编辑准备可写路径。
     *
     * <p>目标文件位于 `mod`。
     * `mod` 中缺少目标文件时，这里会复制当前命中的源文件。
     */
    public Path prepareWritablePath(String fileName) throws IOException {
        String normalized = normalizeFileName(fileName);
        Path existingMod = modFiles.get(normalized);
        if (existingMod != null) {
            return existingMod;
        }

        Path source = resolveReadPath(fileName)
                .orElseThrow(() -> new IllegalArgumentException("Resource file not found: " + fileName));

        Path target = modDirectory.resolve(source.getFileName().toString());
        Files.createDirectories(target.getParent());
        if (!Files.exists(target)) {
            Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        }
        modFiles.put(normalized, target);
        return target;
    }

    /**
     * 把当前会话中可读到的源文件复制成 `mod` 层里的一个新文件名。
     *
     * <p>源文件来自当前命中的读取层。
     * 目标文件位于 `mod`，并登记进覆盖索引。
     */
    public Path copyIntoMod(String sourceFileName, String targetFileName) throws IOException {
        Path source = resolveReadPath(sourceFileName)
                .orElseThrow(() -> new IllegalArgumentException("Resource file not found: " + sourceFileName));

        Path target = modDirectory.resolve(targetFileName).toAbsolutePath().normalize();
        Files.createDirectories(target.getParent());
        Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
        modFiles.put(normalizeFileName(targetFileName), target);
        return target;
    }

    /**
     * 统一文件名归一规则。
     */
    private static String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }

    /**
     * 对单层目录建立文件名索引。
     */
    private static Map<String, Path> indexFlatFiles(Path directory, boolean skipNestedDirectories) throws IOException {
        Map<String, Path> result = new HashMap<>();
        if (!Files.isDirectory(directory)) {
            return result;
        }

        try (var stream = Files.list(directory)) {
            stream.forEach(path -> {
                if (Files.isDirectory(path)) {
                    if (skipNestedDirectories) {
                        return;
                    }
                    if ("mod".equalsIgnoreCase(path.getFileName().toString())) {
                        return;
                    }
                    return;
                }
                result.put(normalizeFileName(path.getFileName().toString()), path.toAbsolutePath().normalize());
            });
        }
        return result;
    }
}
