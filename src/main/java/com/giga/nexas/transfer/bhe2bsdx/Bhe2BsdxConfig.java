package com.giga.nexas.transfer.bhe2bsdx;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Centralized config for BHE -> BSDX transfer.
 */
@Data
@Builder
public class Bhe2BsdxConfig {

    @Builder.Default
    private Path outputBaseDir = Paths.get("src/main/resources/testBhe");

    @Builder.Default
    private Path staticAssetRoot = Paths.get("D:\\BDY\\bsdx_bhe\\bhe_resources");

    @Builder.Default
    private boolean copyStaticAssets = true;

    @Builder.Default
    private Path bheMekJsonDir = Paths.get("src/main/resources/mekBheJson");

    @Builder.Default
    private Path bsdxGrpDir = Paths.get("src/main/resources/game/bsdx/grp");

    @Builder.Default
    private Path bheGrpDir = Paths.get("src/main/resources/game/bhe/grp");

    @Builder.Default
    private Path bsdxMekDir = Paths.get("src/main/resources/game/bsdx/mek");

    @Builder.Default
    private Path bheMekDir = Paths.get("src/main/resources/game/bhe/mek");

    @Builder.Default
    private Path bsdxWazDir = Paths.get("src/main/resources/game/bsdx/waz");

    @Builder.Default
    private Path bheWazDir = Paths.get("src/main/resources/game/bhe/waz");

    @Builder.Default
    private Path bsdxSpmDir = Paths.get("src/main/resources/game/bsdx/spm");

    @Builder.Default
    private Path bheSpmDir = Paths.get("src/main/resources/game/bhe/spm");

    @Builder.Default
    private Path bsdxDatDir = Paths.get("src/main/resources/game/bsdx/dat");

    @Builder.Default
    private String targetKey = "nanoha";

    @Builder.Default
    private String targetCodeName = "NANOHA";

    @Builder.Default
    private boolean keepTargetKey = true;

    @Builder.Default
    private String pacCompressMode = "4";

    @Builder.Default
    private String charset = "windows-31j";

    public static Bhe2BsdxConfig defaults() {
        Bhe2BsdxConfig config = Bhe2BsdxConfig.builder().build();
        config.setOutputBaseDir(resolvePathProp("transfer.outputBaseDir", config.getOutputBaseDir()));
        config.setStaticAssetRoot(resolvePathProp("transfer.staticAssetRoot", config.getStaticAssetRoot()));
        config.setCopyStaticAssets(resolveBooleanProp("transfer.copyStaticAssets", config.isCopyStaticAssets()));
        config.setTargetKey(resolveStringProp("transfer.targetKey", config.getTargetKey()));
        config.setTargetCodeName(resolveStringProp("transfer.targetCodeName", config.getTargetCodeName()));
        config.setKeepTargetKey(resolveBooleanProp("transfer.keepTargetKey", config.isKeepTargetKey()));
        config.setPacCompressMode(resolveStringProp("transfer.pacCompressMode", config.getPacCompressMode()));
        config.setCharset(resolveStringProp("transfer.charset", config.getCharset()));
        if (config.isCopyStaticAssets() && !Files.isDirectory(config.getStaticAssetRoot())) {
            Path internalFallback = Paths.get("src/main/resources/game/bhe");
            if (Files.isDirectory(internalFallback)) {
                config.setStaticAssetRoot(internalFallback);
            }
        }
        return config;
    }

    private static Path resolvePathProp(String key, Path defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Paths.get(raw.trim());
    }

    private static boolean resolveBooleanProp(String key, boolean defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return Boolean.parseBoolean(raw.trim());
    }

    private static String resolveStringProp(String key, String defaultValue) {
        String raw = System.getProperty(key);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        return raw.trim();
    }
}

