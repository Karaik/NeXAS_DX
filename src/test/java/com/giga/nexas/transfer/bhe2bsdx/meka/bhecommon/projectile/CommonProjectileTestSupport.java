package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile;

import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class CommonProjectileTestSupport {

    private static final String CHARSET = "windows-31j";
    private static final Path BSDX_WAZ_DIR = Paths.get("src/main/resources/game/bsdx/waz");
    private static final Path BSDX_SPM_DIR = Paths.get("src/main/resources/game/bsdx/spm");

    private static final BsdxBinService BSDX_BIN_SERVICE = new BsdxBinService();

    private CommonProjectileTestSupport() {
    }

    public static void loadBsdxHostCommonResources(TsukuyomiBsdxBaselineBundle bundle) throws Exception {
        /*
         * 新公共弹幕策略依赖 BSDX 固定公共宿主资源。
         * 测试基线不能只构造 GRP，否则 selfRedirect 会误以为宿主 WAZ/SPM 丢失。
         */
        for (String fileName : BheCommonProjectileResources.commonProjectileHostWazFiles()) {
            Path path = resolveCaseInsensitive(BSDX_WAZ_DIR, fileName);
            bundle.getWazByFileName().put(path.getFileName().toString(),
                    (Waz) BSDX_BIN_SERVICE.parse(path.toString(), CHARSET).getData());
        }
        for (String fileName : BheCommonProjectileResources.commonProjectileSpmFiles()) {
            Path path = resolveCaseInsensitiveOrNull(BSDX_SPM_DIR, fileName);
            if (path != null) {
                bundle.getSpmByFileName().put(path.getFileName().toString(),
                        (Spm) BSDX_BIN_SERVICE.parse(path.toString(), CHARSET).getData());
            }
        }
    }

    private static Path resolveCaseInsensitive(Path dir, String fileName) throws Exception {
        Path path = resolveCaseInsensitiveOrNull(dir, fileName);
        if (path == null) {
            throw new IllegalStateException("测试基线缺少 BSDX 宿主资源: " + dir.resolve(fileName));
        }
        return path;
    }

    private static Path resolveCaseInsensitiveOrNull(Path dir, String fileName) throws Exception {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        try (var stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().equalsIgnoreCase(fileName))
                    .findFirst()
                    .orElse(null);
        }
    }
}
