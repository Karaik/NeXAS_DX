package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 按文件名做大小写不敏感索引。
 *
 * <p>BHE 资源引用里大小写并不稳定，import plan construction 需要用同一套规则定位 .map、preview 和内部引用资源。
 * 索引只负责“文件名 -> 实际路径”，不参与业务命名，也不推导任何 MapGroup 信息。</p>
 */
public class CaseInsensitiveFileIndex {

    private final Map<String, Path> filesByName = new LinkedHashMap<>();

    public static CaseInsensitiveFileIndex empty() {
        return new CaseInsensitiveFileIndex();
    }

    public static CaseInsensitiveFileIndex fromFlatDirectory(Path directory) {
        CaseInsensitiveFileIndex index = new CaseInsensitiveFileIndex();
        if (directory == null || !Files.isDirectory(directory)) {
            return index;
        }

        try (Stream<Path> stream = Files.list(directory)) {
            stream.filter(Files::isRegularFile).forEach(index::add);
        } catch (IOException e) {
            throw new IllegalStateException("扫描文件目录失败: " + directory, e);
        }
        return index;
    }

    public static CaseInsensitiveFileIndex fromTree(Path root) {
        CaseInsensitiveFileIndex index = new CaseInsensitiveFileIndex();
        if (root == null || !Files.exists(root)) {
            return index;
        }

        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(index::add);
        } catch (IOException e) {
            throw new IllegalStateException("扫描资源目录失败: " + root, e);
        }
        return index;
    }

    public Path resolveByName(String fileNameOrReference) {
        String fileName = extractFileName(fileNameOrReference);
        if (fileName == null) {
            return null;
        }
        return filesByName.get(normalize(fileName));
    }

    private void add(Path path) {
        String fileName = path.getFileName().toString();
        filesByName.putIfAbsent(normalize(fileName), path);
    }

    private String extractFileName(String fileNameOrReference) {
        if (fileNameOrReference == null || fileNameOrReference.isBlank()) {
            return null;
        }
        return Path.of(fileNameOrReference.trim().replace('\\', '/')).getFileName().toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }
}
