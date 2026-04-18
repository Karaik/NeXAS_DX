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
     * BSDX 侧固定公共 WAZ fallback 宿主。
     *
     * <p>运行时会按 BSDX 既有公共 WAZ 入口预加载 Effect/Tama/Laser/Bomb。
     * BHE 公共 WAZ 只能追加到这些宿主文件里，再通过 skillBase 重定向到追加段。
     * 接入时会先找继承基线中的同名宿主；找不到同名宿主时才使用本 fallback 表。</p>
     *
     * <p>纯 BSDX 基线与 BHE 的公共 WAZ 顶层分类不是一一同构：
     * BHE `Tama03` 没有 BSDX 顶层 entry，归入 BSDX `Tama05`；
     * BHE `Tama04` 没有 BSDX 顶层 entry，归入 BSDX `Laser`。
     * JINKI 继承基线已经补出 `Tama03/Tama04` 宿主时，会优先使用同名宿主，不走这两个 fallback。
     * 这里的顺序严格对应 {@link #COMMON_PROJECTILE_WAZ_FILES}。</p>
     *
     * <pre>
     * BHE source WazaGroup[0] Effect  -> BSDX host WazaGroup Effect  / Effect.waz
     * BHE source WazaGroup[1] Tama01  -> BSDX host WazaGroup Tama01  / Tama01.waz
     * BHE source WazaGroup[2] Tama02  -> BSDX host WazaGroup Tama02  / Tama02.waz
     * BHE source WazaGroup[3] Tama03  -> fallback BSDX host WazaGroup Tama05 / Tama05.waz
     * BHE source WazaGroup[4] Tama04  -> fallback BSDX host WazaGroup Laser  / Laser.waz
     * BHE source WazaGroup[5] Tama05  -> BSDX host WazaGroup Tama05  / Tama05.waz
     * BHE source WazaGroup[6] Laser   -> BSDX host WazaGroup Laser   / Laser.waz
     * BHE source WazaGroup[7] Bomb    -> BSDX host WazaGroup Bomb    / Bomb.waz
     * </pre>
     */
    public static final List<String> COMMON_PROJECTILE_HOST_WAZ_FILES = List.of(
            "Effect.waz",
            "Tama01.waz",
            "Tama02.waz",
            "Tama05.waz",
            "Laser.waz",
            "Tama05.waz",
            "Laser.waz",
            "Bomb.waz"
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

    public static List<String> commonProjectileHostWazFiles() {
        return COMMON_PROJECTILE_HOST_WAZ_FILES;
    }

    public static List<String> commonProjectileSpmFiles() {
        return Arrays.asList(commonProjectileSpmFileArray());
    }

    private static String normalizeFileName(String fileName) {
        return fileName == null ? "" : fileName.trim().toLowerCase(Locale.ROOT);
    }
}
