package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.util.PacUtil;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.TreeMap;

/** 真正解包后验收路径集合和内容；验证工作目录永远位于本次输出的父目录。 */
public final class PacDeliveryVerifier {
    private PacDeliveryVerifier() { }

    public static void verify(Path source, Path pac) throws IOException {
        Path workspace = Files.createTempDirectory(source.toAbsolutePath().normalize().getParent(), ".pac-verification-");
        try {
            Path copy = workspace.resolve("delivery.pac");
            Files.copy(pac, copy);
            PacUtil.unpack(copy.toString());
            Path unpacked = workspace.resolve("delivery");
            if (!Files.isDirectory(unpacked)) throw new IllegalStateException("PAC 解包未生成目录: " + unpacked);
            Map<String, String> expected = manifest(source), actual = manifest(unpacked);
            if (!expected.equals(actual)) {
                throw new IllegalStateException("PAC 路径/内容与最终输出不一致; expected=" + expected.keySet() + "; actual=" + actual.keySet());
            }
        } finally {
            // 仅清理本函数创建、位于本次输出父目录内的唯一临时目录。
            try (var paths = Files.walk(workspace)) {
                for (Path path : paths.sorted(java.util.Comparator.reverseOrder()).toList()) Files.deleteIfExists(path);
            }
        }
    }

    private static Map<String, String> manifest(Path root) throws IOException {
        Map<String, String> result = new TreeMap<>();
        try (var paths = Files.walk(root)) {
            for (Path path : paths.filter(Files::isRegularFile).sorted().toList()) {
                String relative = root.relativize(path).toString().replace('\\', '/');
                try {
                    MessageDigest digest = MessageDigest.getInstance("SHA-256");
                    try (var input = Files.newInputStream(path)) {
                        byte[] buffer = new byte[64 * 1024];
                        for (int count; (count = input.read(buffer)) >= 0;) if (count > 0) digest.update(buffer, 0, count);
                    }
                    result.put(relative, HexFormat.of().formatHex(digest.digest()));
                } catch (NoSuchAlgorithmException e) { throw new IllegalStateException("SHA-256 不可用", e); }
            }
        }
        return result;
    }
}
