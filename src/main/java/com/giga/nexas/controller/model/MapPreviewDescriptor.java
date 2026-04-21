package com.giga.nexas.controller.model;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

/**
 * 地图预览解析结果。
 *
 * <p>这个类属于地图预览层。
 * 它承载某个 `mapId` 在当前覆盖会话里解析出的真实预览图信息，
 * 当前语义是：
 * `mapId -> MapGroup.grp.groupResourceName -> T_map*.bmp -> 实际命中路径`。
 *
 * <p>输入来自 `MapGroup.grp` 和当前 `root + mod` 覆盖会话。
 * 输出是主界面地图预览区直接使用的资源名、路径和状态文本。
 */
@Getter
@Builder
public class MapPreviewDescriptor {

    /**
     * 当前地图的索引信息。
     */
    private final MapLookupEntry mapEntry;

    /**
     * 真实命中的预览图文件名。
     *
     * <p>当前格式是 `T_ + groupResourceName + .bmp`。
     */
    private final String previewImageName;

    /**
     * 当前覆盖会话里最终命中的预览图路径。
     *
     * <p>有值时说明 GUI 可以直接显示图片；
     * 为空时说明当前资源目录里没有这张图。
     */
    private final Path previewImagePath;

    /**
     * 当前预览链涉及到的图片文件名列表。
     *
     * <p>这一步当前只保留一张真实候选图，
     * 方便 UI 或后续提示层直接显示命中的资源名。
     */
    private final List<String> previewImageNames;

    /**
     * 当前预览状态的说明文本。
     */
    private final String statusText;
}
