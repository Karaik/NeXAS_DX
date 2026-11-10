package com.giga.nexas.controller.model;

import lombok.Builder;
import lombok.Getter;

/**
 * Hell 编辑器里的地图索引条目。
 *
 * <p>它属于编辑器引用数据层，负责表达
 * `mapId -> MapGroup.grp -> 资源名前缀` 这条映射。
 *
 * <p>输入来自 `MapGroup.grp`；
 * 输出是供地图信息显示、预览资源推导和地图速查使用的只读模型。
 */
@Getter
@Builder
public class MapLookupEntry {

    /**
     * 运行时使用的地图编号，也是 `HellConfig[2]` / `LoadMap(...)` 里的值。
     */
    private final int mapId;

    /**
     * `MapGroup.grp` 里的地图名称。
     */
    private final String groupName;

    /**
     * `MapGroup.grp` 里的地图代号。
     */
    private final String groupCodeName;

    /**
     * `MapGroup.grp` 里的资源名前缀。
     */
    private final String groupResourceName;
}
