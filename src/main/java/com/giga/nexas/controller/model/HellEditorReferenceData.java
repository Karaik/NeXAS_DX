package com.giga.nexas.controller.model;

import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.Map;

/**
 * Hell 编辑器引用数据快照。
 *
 * <p>它属于编辑器引用数据层，用来把当前资源目录里和 Hell 编辑相关的
 * 机体索引、地图索引统一装成一份会话快照，避免 controller 每次刷新 UI
 * 都重复解析 `MekaGroup.grp / MapGroup.grp`。
 *
 * <p>输入来自引用数据服务；
 * 输出是 `mekaIndex`、`mapId` 到展示模型的稳定映射。
 */
@Getter
@Builder
public class HellEditorReferenceData {

    /**
     * 供机体速查表直接展示的条目列表。
     */
    private final List<MekaLookupEntry> mekaEntries;

    /**
     * 供敌机槽位即时反查使用的 `mekaIndex -> 机体` 映射。
     */
    private final Map<Integer, MekaLookupEntry> mekaByIndex;

    /**
     * 供地图信息显示和地图速查使用的地图条目列表。
     */
    private final List<MapLookupEntry> mapEntries;

    /**
     * 供 `mapId` 直接反查地图说明与预览资源名的映射。
     */
    private final Map<Integer, MapLookupEntry> mapById;
}
