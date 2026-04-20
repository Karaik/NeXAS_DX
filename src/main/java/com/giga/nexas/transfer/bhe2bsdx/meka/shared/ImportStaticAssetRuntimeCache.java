package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;

import java.io.IOException;
import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

/**
 * 单次静态资源导出过程中的轻量缓存。
 *
 * <p>只缓存重复读取、重复反射、重复目录扫描的中间结果，不改任何业务选择逻辑。
 * 每次 importAssets 入口都应 reset，一次导出内复用，同步保持字节级输出不变。</p>
 */
public final class ImportStaticAssetRuntimeCache {

    private final BsdxBinService bsdxBinService;
    private final Map<String, Path> outputFileIndex = new HashMap<>();
    private final Map<String, Path> externalFileIndex = new HashMap<>();
    private final Map<String, Waz> parsedOutputWazByName = new HashMap<>();
    private final Map<String, Spm> parsedOutputSpmByName = new HashMap<>();
    private final Map<Class<?>, List<Field>> fieldsByType = new HashMap<>();

    private Path indexedOutputRoot;
    private Path indexedExternalRoot;

    public ImportStaticAssetRuntimeCache(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService;
    }

    public void reset() {
        outputFileIndex.clear();
        externalFileIndex.clear();
        parsedOutputWazByName.clear();
        parsedOutputSpmByName.clear();
        indexedOutputRoot = null;
        indexedExternalRoot = null;
    }

    public Waz parseOutputWaz(Path outputRoot, String fileName, String charset) throws IOException {
        if (outputRoot == null || fileName == null || fileName.isBlank()) {
            return null;
        }
        String key = normalize(fileName);
        Waz cached = parsedOutputWazByName.get(key);
        if (cached != null) {
            return cached;
        }

        Path output = resolveOutputFileCaseInsensitive(outputRoot, fileName);
        if (output == null || !Files.exists(output)) {
            return null;
        }

        Waz parsed = (Waz) bsdxBinService.parse(output.toString(), charset).getData();
        parsedOutputWazByName.put(key, parsed);
        return parsed;
    }

    public Spm parseOutputSpm(Path outputRoot, String fileName, String charset) throws IOException {
        if (outputRoot == null || fileName == null || fileName.isBlank()) {
            return null;
        }
        String key = normalize(fileName);
        Spm cached = parsedOutputSpmByName.get(key);
        if (cached != null) {
            return cached;
        }

        Path output = resolveOutputFileCaseInsensitive(outputRoot, fileName);
        if (output == null || !Files.exists(output)) {
            return null;
        }

        Spm parsed = (Spm) bsdxBinService.parse(output.toString(), charset).getData();
        parsedOutputSpmByName.put(key, parsed);
        return parsed;
    }

    public Path resolveExternalFileCaseInsensitive(Path root, String fileName) throws IOException {
        if (root == null || fileName == null || fileName.isBlank() || !Files.exists(root)) {
            return null;
        }
        ensureExternalFileIndex(root);
        return externalFileIndex.get(normalize(fileName));
    }

    public List<Field> getAllFields(Class<?> type) {
        if (type == null) {
            return List.of();
        }
        List<Field> cached = fieldsByType.get(type);
        if (cached != null) {
            return cached;
        }

        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        fieldsByType.put(type, fields);
        return fields;
    }

    private Path resolveOutputFileCaseInsensitive(Path dir, String fileName) throws IOException {
        if (dir == null || fileName == null || fileName.isBlank() || !Files.exists(dir)) {
            return null;
        }
        ensureOutputFileIndex(dir);
        return outputFileIndex.get(normalize(fileName));
    }

    private void ensureOutputFileIndex(Path dir) throws IOException {
        Path normalizedDir = dir.toAbsolutePath().normalize();
        if (normalizedDir.equals(indexedOutputRoot)) {
            return;
        }

        outputFileIndex.clear();
        indexedOutputRoot = normalizedDir;
        try (Stream<Path> stream = Files.list(normalizedDir)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> outputFileIndex.putIfAbsent(normalize(path.getFileName().toString()), path));
        }
    }

    private void ensureExternalFileIndex(Path root) throws IOException {
        Path normalizedRoot = root.toAbsolutePath().normalize();
        if (normalizedRoot.equals(indexedExternalRoot)) {
            return;
        }

        externalFileIndex.clear();
        indexedExternalRoot = normalizedRoot;
        try (Stream<Path> stream = Files.walk(normalizedRoot)) {
            stream.filter(Files::isRegularFile)
                    .forEach(path -> externalFileIndex.putIfAbsent(normalize(path.getFileName().toString()), path));
        }
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
