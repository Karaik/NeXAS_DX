package com.giga.nexas.controller.model;

import java.util.Set;

/**
 * 引擎类型及其支持的扩展名定义。
 */
public enum EngineType {

    BSDX("BALDR SKY DIVEX",
            "windows-31j",
            Set.of("waz", "mek", "spm", "grp", "bin", "dat"),
            Set.of("waz", "mek", "spm", "grp", "bin", "dat")),

    BHE("BALDR HEART EXE",
            "windows-31j",
            Set.of("waz", "mek", "spm", "grp"),
            Set.of("spm", "grp")),

    CLARIAS("CLARIAS",
            "windows-31j",
            Set.of("dat", "mek", "grp"),
            Set.of("dat", "mek", "grp")),

    KINGDOM("KINGDOM",
                    "windows-31j",
            Set.of("tfn"),
            null);

    private final String displayName;
    private final String defaultCharset;
    private final Set<String> parseExtensions;
    private final Set<String> generateExtensions;

    EngineType(String displayName,
               String defaultCharset,
               Set<String> parseExtensions,
               Set<String> generateExtensions) {
        this.displayName = displayName;
        this.defaultCharset = defaultCharset;
        this.parseExtensions = parseExtensions;
        this.generateExtensions = generateExtensions;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDefaultCharset() {
        return defaultCharset;
    }

    public Set<String> getParseExtensions() {
        return parseExtensions;
    }

    public Set<String> getGenerateExtensions() {
        return generateExtensions;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

