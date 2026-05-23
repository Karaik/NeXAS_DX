package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

import lombok.Data;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * BHE 地图追加请求。
 *
 * <p>Step1 需要同时读取 BHE MapGroup.grp、BHE .map 目录和 BHE 静态资源目录。
 * outputRoot 只用于按 plan 物料化 preview，不参与 plan 构建。</p>
 */
@Data
public class BheMapAppendRequest {

    private static final Path PROJECT_ROOT = Paths.get("").toAbsolutePath().normalize();

    private Path bheMapGroupPath = Paths.get("src/main/resources/game/bhe/grp/mapgroup.grp");
    private Path bsdxMapGroupPath = Paths.get("src/main/resources/game/bsdx/grp/MapGroup.grp");
    private Path bheMapDir = Paths.get("src/main/resources/game/bhe/map");
    private Path bheStaticResourceRoot;
    private List<Path> bsdxPreviewFallbackRoots = new ArrayList<>();
    private Path outputRoot;
    private String charset = "windows-31j";

    public Path resolveBheMapGroupPath() {
        return resolveAgainstProjectRoot(bheMapGroupPath);
    }

    public Path resolveBsdxMapGroupPath() {
        return resolveAgainstProjectRoot(bsdxMapGroupPath);
    }

    public Path resolveBheMapDir() {
        return resolveAgainstProjectRoot(bheMapDir);
    }

    public Path resolveBheStaticResourceRoot() {
        // 静态资源根必须由调用方显式指定，避免默认绑定某台机器的本机目录。
        if (bheStaticResourceRoot == null) {
            throw new IllegalArgumentException("BHE 静态资源目录未设置");
        }
        return resolveAgainstProjectRoot(bheStaticResourceRoot);
    }

    public List<Path> resolveBsdxPreviewFallbackRoots() {
        List<Path> resolved = new ArrayList<>();
        for (Path root : bsdxPreviewFallbackRoots) {
            resolved.add(resolveAgainstProjectRoot(root));
        }
        return resolved;
    }

    public Path resolveOutputRoot() {
        if (outputRoot == null) {
            throw new IllegalArgumentException("BHE 地图追加输出目录不能为空");
        }
        return resolveAgainstProjectRoot(outputRoot);
    }

    private Path resolveAgainstProjectRoot(Path path) {
        if (path == null) {
            return null;
        }
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return PROJECT_ROOT.resolve(path).normalize();
    }
}
