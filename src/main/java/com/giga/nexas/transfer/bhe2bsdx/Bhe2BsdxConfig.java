package com.giga.nexas.transfer.bhe2bsdx;

import lombok.Builder;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * BHE→BSDX 批量移植配置。
 * <p>
 * 集中管理所有路径、目标槽位、开关等配置项。
 * 使用 Builder 模式构建，所有字段均有默认值。
 */
@Data
@Builder
public class Bhe2BsdxConfig {

    // ===== 输出目录 =====
    /** 转换结果输出根目录，每个源机体会在此下创建子目录 */
    @Builder.Default
    private Path outputBaseDir = Paths.get("src/main/resources/testBhe");

    // ===== 静态资源 =====
    /** BHE 静态资源来源目录（图片、音频等） */
    @Builder.Default
    private Path staticAssetRoot = Paths.get("D:\\BaiduNetdiskDownload\\bsdx_bhe\\bheAll");

    /** 是否复制静态资源到输出目录 */
    @Builder.Default
    private boolean copyStaticAssets = true;

    // ===== 源机体发现 =====
    /** BHE MEK JSON 目录，用于自动发现源机体 */
    @Builder.Default
    private Path bheMekJsonDir = Paths.get("src/main/resources/mekBheJson");

    // ===== BSDX 基线资源路径 =====
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

    // ===== 目标槽位 =====
    /** 替换目标的文件名 key（如 "nanoha"） */
    @Builder.Default
    private String targetKey = "nanoha";

    /** 替换目标的 GRP codeName（如 "NANOHA"） */
    @Builder.Default
    private String targetCodeName = "NANOHA";

    /** 是否保留目标槽位的原始 key/codeName（true=游戏内仍显示为 Nanoha） */
    @Builder.Default
    private boolean keepTargetKey = true;

    // ===== PAC 打包 =====
    /** PAC 压缩模式（"4" = 默认压缩） */
    @Builder.Default
    private String pacCompressMode = "4";

    /** 二进制文件编码 */
    @Builder.Default
    private String charset = "windows-31j";

    /**
     * 创建默认配置实例。
     */
    public static Bhe2BsdxConfig defaults() {
        return Bhe2BsdxConfig.builder().build();
    }
}
