package com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Tsukuyomi graft 请求对象。
 */
@Data
public class TsukuyomiGraftRequest {

    /**
     * JINKI V2 输出的已补丁改写 exe，相对项目根目录记录。
     */
    private Path jinkiGeneratedExePath;

    /**
     * JINKI V2 输出的平铺 asset 目录，相对项目根目录记录。
     */
    private Path jinkiGeneratedAssetDir;

    /**
     * TODO 20260417：
     * 通过现有 request 入口携带上一层 JINKI 结果，避免新增重载入口。
     */
    private AkaoGraftResult inheritedJinkiResult;

    /**
     * BHE 游戏资源根目录。这个目录不在项目内，内部按 grp/dat/mek/spm/waz 分类。
     */
    private Path bheGameResourceRoot = Paths.get("D:/BDY/NeXAS_Resources/bhe_game");

    /**
     * Tsukuyomi 个人资源在 BHE 游戏资源根目录下的相对路径。
     */
    private Path tsukuyomiResourceDir = Paths.get("tsukuyomi");

    private Path bheGrpDir = Paths.get("src/main/resources/game/bhe/grp");
    private Path bheDatDir = Paths.get("src/main/resources/game/bhe/dat");
    private Path bheMekDir = Paths.get("src/main/resources/game/bhe/mek/tsukuyomi");
    private Path bheSpmDir = Paths.get("src/main/resources/game/bhe/spm/tsukuyomi");
    private Path bheWazDir = Paths.get("src/main/resources/game/bhe/waz/tsukuyomi");

    private Path bsdxGrpDir = Paths.get("src/main/resources/game/bsdx/grp");
    private Path bsdxDatDir = Paths.get("src/main/resources/game/bsdx/dat");
    private Path bsdxMekDir = Paths.get("src/main/resources/game/bsdx/mek");
    private Path bsdxSpmDir = Paths.get("src/main/resources/game/bsdx/spm");
    private Path bsdxWazDir = Paths.get("src/main/resources/game/bsdx/waz");

    private Path externalStaticAssetRoot = Paths.get("D:/BDY/NeXAS_Resources/bhe_resources");
    private Path targetExePath = Paths.get("src/main/resources/ida-reverse/BaldrSky.exe");
    private Path exeOutputDir = Paths.get("src/main/resources/out");
    private String pacCompressMode = "4";

    private String mekaCodeName = "TSUKUYOMI";
    private String wazCodeName = "TSUKUYOMI";
    private String spriteCodeName = "TSUKUYOMI";
    private String spriteFileName = "tsukuyomi.spm";
    private String mekFileName = "tsukuyomi.mek";
    private String wazFileName = "tsukuyomi.waz";
    private Integer fixedMekaGroupIndex = null;

    private boolean patchMenuData = true;
    private boolean planExeCapacityPatch = true;

    public Path resolveTargetExePath() {
        return resolveAgainstProjectRoot(jinkiGeneratedExePath != null ? jinkiGeneratedExePath : targetExePath);
    }

    public Path resolveBsdxGrpDir() {
        return resolveBaselineDir(bsdxGrpDir);
    }

    public Path resolveBsdxDatDir() {
        return resolveBaselineDir(bsdxDatDir);
    }

    public Path resolveBsdxMekDir() {
        return resolveBaselineDir(bsdxMekDir);
    }

    public Path resolveBsdxSpmDir() {
        return resolveBaselineDir(bsdxSpmDir);
    }

    public Path resolveBsdxWazDir() {
        return resolveBaselineDir(bsdxWazDir);
    }

    public Path resolveBheGrpDir() {
        if (bheGameResourceRoot != null) {
            return resolveAgainstProjectRoot(bheGameResourceRoot).resolve("grp");
        }
        return resolveAgainstProjectRoot(bheGrpDir);
    }

    public Path resolveBheDatDir() {
        if (bheGameResourceRoot != null) {
            return resolveAgainstProjectRoot(bheGameResourceRoot).resolve("dat");
        }
        return resolveAgainstProjectRoot(bheDatDir);
    }

    public Path resolveBheMekDir() {
        if (bheGameResourceRoot != null && tsukuyomiResourceDir != null) {
            return resolveAgainstProjectRoot(bheGameResourceRoot).resolve("mek").resolve(tsukuyomiResourceDir);
        }
        return resolveAgainstProjectRoot(bheMekDir);
    }

    public Path resolveBheSpmDir() {
        if (bheGameResourceRoot != null && tsukuyomiResourceDir != null) {
            return resolveAgainstProjectRoot(bheGameResourceRoot).resolve("spm").resolve(tsukuyomiResourceDir);
        }
        return resolveAgainstProjectRoot(bheSpmDir);
    }

    public Path resolveBheWazDir() {
        if (bheGameResourceRoot != null && tsukuyomiResourceDir != null) {
            return resolveAgainstProjectRoot(bheGameResourceRoot).resolve("waz").resolve(tsukuyomiResourceDir);
        }
        return resolveAgainstProjectRoot(bheWazDir);
    }

    private Path resolveBaselineDir(Path fallback) {
        if (jinkiGeneratedAssetDir != null) {
            return resolveAgainstProjectRoot(jinkiGeneratedAssetDir);
        }
        return resolveAgainstProjectRoot(fallback);
    }

    private Path resolveAgainstProjectRoot(Path path) {
        if (path == null) {
            return null;
        }
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return Paths.get("").toAbsolutePath().normalize().resolve(path).normalize();
    }
}
