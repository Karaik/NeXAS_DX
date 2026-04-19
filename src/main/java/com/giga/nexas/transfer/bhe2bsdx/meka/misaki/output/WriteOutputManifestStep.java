package com.giga.nexas.transfer.bhe2bsdx.meka.misaki.output;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public class WriteOutputManifestStep {

    
    private final ObjectMapper objectMapper = new ObjectMapper()
            .enable(SerializationFeature.INDENT_OUTPUT);

    public OutputManifest buildManifest(Path outputRoot) {
        if (outputRoot == null || !Files.exists(outputRoot)) {
            throw new IllegalArgumentException("输出目录不存在: " + outputRoot);
        }

        OutputManifest manifest = new OutputManifest();
        manifest.setOutputRoot(outputRoot.toAbsolutePath().normalize().toString());
        ResourceOverwriteAudit audit = new ResourceOverwriteAudit();
        try (Stream<Path> stream = Files.walk(outputRoot)) {
            List<Path> files = stream
                    .filter(Files::isRegularFile)
                    .sorted(Comparator.comparing(path -> OutputManifestSupport.relativePath(outputRoot, path)))
                    .toList();
            for (Path file : files) {
                manifest.addEntry(OutputManifestSupport.entryForFile(
                        outputRoot,
                        file,
                        OutputManifestSupport.categoryFromExtension(file),
                        "existing-output",
                        audit
                ));
            }
            manifest.getNotes().addAll(audit.getOverwriteNotes());
            return manifest;
        } catch (IOException e) {
            throw new IllegalStateException("构建输出 manifest 失败: " + outputRoot, e);
        }
    }

    public Path write(Path manifestPath, OutputManifest manifest) {
        if (manifestPath == null) {
            throw new IllegalArgumentException("manifestPath 不能为空");
        }
        try {
            Path parent = manifestPath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            objectMapper.writeValue(manifestPath.toFile(), manifest);
            return manifestPath;
        } catch (IOException e) {
            throw new IllegalStateException("写出 output manifest 失败: " + manifestPath, e);
        }
    }
}
