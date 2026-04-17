package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.common;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Tsukuyomi 首次接入时显式指定的公共资源清单。
 */
public final class TsukuyomiSpecifiedCommonResources {

    public static final List<String> COMMON_PROJECTILE_WAZ_FILES = List.of(
            "effect.waz",
            "tama01.waz",
            "tama02.waz",
            "tama03.waz",
            "tama04.waz",
            "tama05.waz",
            "laser.waz",
            "bomb.waz"
    );

    private static final Set<String> COMMON_PROJECTILE_WAZ_FILE_SET = COMMON_PROJECTILE_WAZ_FILES.stream()
            .map(TsukuyomiSpecifiedCommonResources::normalizeFileName)
            .collect(Collectors.toUnmodifiableSet());

    private TsukuyomiSpecifiedCommonResources() {
    }

    public static boolean isSpecifiedCommonWaz(String fileName) {
        return COMMON_PROJECTILE_WAZ_FILE_SET.contains(normalizeFileName(fileName));
    }

    public static String[] commonProjectileWazFileArray() {
        return COMMON_PROJECTILE_WAZ_FILES.toArray(String[]::new);
    }

    public static List<String> commonProjectileWazFiles() {
        return Arrays.asList(commonProjectileWazFileArray());
    }

    private static String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }
}
