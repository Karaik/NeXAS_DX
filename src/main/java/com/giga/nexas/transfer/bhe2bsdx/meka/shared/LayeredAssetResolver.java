package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/** 单次输出的有效媒体视图；原版媒体目录绝不参与 GRP/MEK/WAZ/SPM 索引解析。 */
public final class LayeredAssetResolver {
    public record ResolvedAsset(String layer, Path path) { }

    private final TsukuyomiGraftRequest request;
    private final Path output;
    private final TsukuyomiImportedAssetSet assets;
    private final Map<Path, Map<String, Path>> indices = new HashMap<>();
    private final Map<Path, Map<String, Path>> mediaIndices = new HashMap<>();

    public LayeredAssetResolver(TsukuyomiGraftRequest request, Path output, TsukuyomiImportedAssetSet assets) {
        this.request = request;
        this.output = output;
        this.assets = assets;
    }

    public ResolvedAsset resolveMedia(String name, boolean audio) throws IOException {
        ResolvedAsset found = find("output", output, name, audio);
        if (found == null) found = find("inherited", request.getJinkiGeneratedAssetDir(), name, audio);
        if (found == null) found = find("vanilla", request.getBsdxStaticAssetRoot(), name, audio);
        if (found != null) record(name, found);
        return found;
    }

    /** 有供体的新 BHE 媒体复制到输出；缺图保留引用并审计，原版媒体记录继承来源。 */
    public void materialize(String target, String donor, boolean audio, boolean newBheAsset, String chain) throws IOException {
        ResolvedAsset effective = find("output", output, target, audio);
        if (effective == null) effective = find("inherited", request.getJinkiGeneratedAssetDir(), target, audio);
        if (effective == null && !newBheAsset) effective = find("vanilla", request.getBsdxStaticAssetRoot(), target, audio);
        if (effective != null) {
            record(target, effective);
            return;
        }
        ResolvedAsset source = find("BHE donor", request.getExternalStaticAssetRoot(), donor, audio);
        if (source == null) {
            if (!audio) {
                recordUnresolvedImage(target, chain + " (donor=" + donor + ")");
                return;
            }
            throw new IllegalStateException("必需音频缺失: " + chain + " -> " + target
                    + " (donor=" + donor + "; output/inherited/vanilla/BHE donor 均无可用载荷)");
        }
        String outputName = audio ? stripAudioExtension(target) + extension(source.path().getFileName().toString()) : target;
        Path destination = output.resolve(outputName).normalize();
        if (!destination.getParent().equals(output.toAbsolutePath().normalize())
                && !destination.getParent().equals(output.normalize())) throw new IllegalStateException("媒体输出必须为平铺文件名: " + target);
        Files.copy(source.path(), destination, StandardCopyOption.REPLACE_EXISTING);
        index(output).put(normalize(outputName), destination);
        mediaIndex(output).put(normalize(outputName), destination);
        if (assets != null) {
            if (audio) assets.getCopiedAudioFiles().add(destination);
            else assets.getCopiedImageFiles().add(destination);
        }
        record(target, new ResolvedAsset("output <- BHE donor", destination));
    }

    public Path resolveFile(Path root, String name) throws IOException {
        return root == null ? null : index(root).get(normalize(name));
    }

    /** 原生SPM先保存图名、绘制时才尝试载入；静态引用不能证明物理图片必须存在。 */
    public void auditImageReference(String name, String chain) throws IOException {
        if (resolveMedia(name, false) == null) recordUnresolvedImage(name, chain);
    }

    private void recordUnresolvedImage(String name, String chain) {
        if (assets != null) assets.getUnresolvedImageReferences().putIfAbsent(normalize(name), chain);
    }

    private ResolvedAsset find(String layer, Path root, String name, boolean audio) throws IOException {
        if (root == null || name == null || name.isBlank()) return null;
        Map<String, Path> files = mediaIndex(root);
        Path path = files.get(normalize(name));
        if (path == null && audio) {
            String base = stripAudioExtension(name);
            path = files.get(normalize(base + ".ogg"));
            if (path == null) path = files.get(normalize(base + ".wav"));
        }
        if (path == null || !Files.isRegularFile(path)) return null;
        String ext = extension(path.getFileName().toString());
        if (audio ? !ext.equals(".ogg") && !ext.equals(".wav") : !ext.equals(".png") && !ext.equals(".bmp")) return null;
        return new ResolvedAsset(layer, path);
    }

    private Map<String, Path> index(Path root) throws IOException {
        Path normalized = root.toAbsolutePath().normalize();
        Map<String, Path> cached = indices.get(normalized);
        if (cached != null) return cached;
        Map<String, Path> result = new LinkedHashMap<>();
        if (Files.isDirectory(normalized)) {
            try (var paths = Files.walk(normalized)) {
                for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                    String key = normalize(path.getFileName().toString());
                    Path prior = result.putIfAbsent(key, path);
                    if (prior != null) throw new IllegalStateException("同层同名资源冲突: " + prior + " / " + path);
                }
            }
        }
        indices.put(normalized, result);
        return result;
    }

    /** 媒体根是平铺载荷目录；其中的mod/解包备份不是原版媒体加载层。 */
    private Map<String, Path> mediaIndex(Path root) throws IOException {
        Path normalized = root.toAbsolutePath().normalize();
        Map<String, Path> cached = mediaIndices.get(normalized);
        if (cached != null) return cached;
        Map<String, Path> files = new LinkedHashMap<>();
        if (Files.isDirectory(normalized)) try (var paths = Files.list(normalized)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String name = path.getFileName().toString();
                String ext = extension(name);
                if (!ext.equals(".png") && !ext.equals(".bmp") && !ext.equals(".ogg") && !ext.equals(".wav")) continue;
                Path prior = files.putIfAbsent(normalize(name), path);
                if (prior != null) throw new IllegalStateException("同层媒体名称冲突: " + prior + " / " + path);
            }
        }
        mediaIndices.put(normalized, files);
        return files;
    }

    private void record(String name, ResolvedAsset source) {
        if (assets != null) assets.getResolvedMediaSources().put(normalize(name), source.layer() + ": " + source.path());
    }
    public static String normalize(String value) { return value == null ? "" : value.trim().toLowerCase(Locale.ROOT); }
    private static String extension(String name) {
        int dot = name.lastIndexOf('.'); return dot < 0 ? "" : normalize(name.substring(dot));
    }
    private static String stripAudioExtension(String name) {
        String ext = extension(name); return ext.equals(".ogg") || ext.equals(".wav") ? name.substring(0, name.lastIndexOf('.')) : name;
    }
}
