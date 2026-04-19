package com.giga.nexas.transfer.bhe2bsdx.meka.naoto.output;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;

final class OutputManifestSupport {

    private OutputManifestSupport() {
    }

    static OutputResourceEntry entryForFile(
            Path outputRoot,
            Path file,
            String category,
            String sourceDescription,
            ResourceOverwriteAudit overwriteAudit
    ) throws IOException {
        String relativePath = relativePath(outputRoot, file);
        boolean overwritten = overwriteAudit != null && overwriteAudit.recordOutput(relativePath, sourceDescription);
        return OutputResourceEntry.builder()
                .relativePath(relativePath)
                .fileName(file.getFileName().toString())
                .category(category)
                .sourceDescription(sourceDescription)
                .size(Files.size(file))
                .sha256(sha256(file))
                .overwritten(overwritten)
                .build();
    }

    static String relativePath(Path outputRoot, Path file) {
        Path root = outputRoot.toAbsolutePath().normalize();
        Path normalizedFile = file.toAbsolutePath().normalize();
        return root.relativize(normalizedFile).toString().replace('\\', '/');
    }

    static String categoryFromExtension(Path file) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return "unknown";
        }
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    static String sha256(Path file) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream input = Files.newInputStream(file);
                 DigestInputStream digestInput = new DigestInputStream(input, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInput.read(buffer) != -1) {
                }
            }
            return toHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 digest is unavailable", e);
        }
    }

    private static String toHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte value : bytes) {
            builder.append(String.format("%02x", value));
        }
        return builder.toString();
    }
}
