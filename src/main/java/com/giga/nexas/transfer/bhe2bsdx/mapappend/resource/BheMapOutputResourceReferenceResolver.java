package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import java.nio.file.Path;

public class BheMapOutputResourceReferenceResolver {

    public String toOutputResourceFileName(String mapInternalResourceText) {
        if (mapInternalResourceText == null || mapInternalResourceText.isBlank()) {
            throw new IllegalArgumentException("mapInternalResourceText must not be blank");
        }
        Path fileName = Path.of(mapInternalResourceText.trim().replace('\\', '/')).getFileName();
        if (fileName == null || fileName.toString().isBlank()) {
            throw new IllegalArgumentException("mapInternalResourceText has no file name: " + mapInternalResourceText);
        }
        return fileName.toString();
    }

    public Path resolveOutputResourcePath(Path outputRoot, String mapInternalResourceText) {
        if (outputRoot == null) {
            throw new IllegalArgumentException("outputRoot must not be null");
        }
        Path normalizedOutputRoot = outputRoot.toAbsolutePath().normalize();
        Path outputPath = normalizedOutputRoot.resolve(toOutputResourceFileName(mapInternalResourceText)).normalize();
        if (!outputPath.startsWith(normalizedOutputRoot)) {
            throw new IllegalArgumentException("resolved output resource path escapes outputRoot: " + mapInternalResourceText);
        }
        return outputPath;
    }
}
