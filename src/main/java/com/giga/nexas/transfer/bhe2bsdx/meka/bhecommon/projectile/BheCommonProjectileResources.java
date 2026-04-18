package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * BHE 公共弹幕资源簇的固定清单。
 *
 * <p>这批资源由首个 BHE 机体接入时统一转换并接入基线；其他单机体移植不重复转换。
 * 清单放在 bhecommon 包下，避免 Tsukuyomi 私有转换步骤继续混入公共资源职责。</p>
 */
public final class BheCommonProjectileResources {

    /**
     * BHE 公共 WAZ 主入口。
     *
     * <p>这 8 个文件对应 BHE `WazaGroup[0..7]`，公共 WAZ 内部的
     * `CEventWazaSelect.wazFileNo` 经审计确认只在这 8 个入口之间相互引用。</p>
     */
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

    /**
     * BHE 公共 WAZ 直接引用到的公共 SPM。
     *
     * <p>清单来自 2026-04-18 数据审计：递归扫描 8 个公共 WAZ 中所有
     * `spmFileSequence >= 0`，并反查 BHE `SpriteGroup.grp` 得到这些文件。
     * `spmFileSequence = -2` 是特殊值，不表示外部 SPM 文件，因此不列入清单。</p>
     */
    public static final List<String> COMMON_PROJECTILE_SPM_FILES = List.of(
            "Tama.spm",
            "bomb.spm",
            "Elec.spm",
            "Smoke.spm",
            "Mark.spm",
            "Pic.spm",
            "Ice.spm",
            "Link.spm",
            "Fire.spm",
            "Wind.spm",
            "MekaEffect.spm",
            "設置物：掲示板.spm"
    );

    // Set 只用于边界判断；顺序以 List 为准，接入和审计需要稳定输出顺序。
    private static final Set<String> COMMON_PROJECTILE_WAZ_FILE_SET = COMMON_PROJECTILE_WAZ_FILES.stream()
            .map(BheCommonProjectileResources::normalizeFileName)
            .collect(Collectors.toUnmodifiableSet());

    private static final Set<String> COMMON_PROJECTILE_SPM_FILE_SET = COMMON_PROJECTILE_SPM_FILES.stream()
            .map(BheCommonProjectileResources::normalizeFileName)
            .collect(Collectors.toUnmodifiableSet());

    private BheCommonProjectileResources() {
    }

    public static boolean isCommonProjectileWaz(String fileName) {
        return COMMON_PROJECTILE_WAZ_FILE_SET.contains(normalizeFileName(fileName));
    }

    public static boolean isCommonProjectileSpm(String fileName) {
        return COMMON_PROJECTILE_SPM_FILE_SET.contains(normalizeFileName(fileName));
    }

    public static String[] commonProjectileWazFileArray() {
        return COMMON_PROJECTILE_WAZ_FILES.toArray(String[]::new);
    }

    public static String[] commonProjectileSpmFileArray() {
        return COMMON_PROJECTILE_SPM_FILES.toArray(String[]::new);
    }

    public static List<String> commonProjectileWazFiles() {
        return Arrays.asList(commonProjectileWazFileArray());
    }

    public static List<String> commonProjectileSpmFiles() {
        return Arrays.asList(commonProjectileSpmFileArray());
    }

    private static String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }
}
