package com.giga.nexas.controller.model;

import lombok.Builder;
import lombok.Getter;

import java.nio.file.Path;
import java.util.List;

/**
 * Hell 关卡树中的单个节点描述。
 *
 * <p>这是第一版脚本编辑模式的核心视图模型：
 * 一条 `HellConfig.dat` 记录，加上它映射到的 `HellXXX.bin` 及来源层信息。
 */
@Getter
@Builder
public class HellStageDescriptor {

    /**
     * `HellConfig.dat` 中的记录下标。
     */
    private final int index;

    /**
     * 选关标题。
     */
    private final String title;

    /**
     * 详情说明文本。
     */
    private final String description;

    /**
     * 难度或地狱度显示值。
     */
    private final Integer hellLevel;

    /**
     * 地图 id。
     */
    private final Integer mapId;

    /**
     * 商店编号。
     */
    private final Integer shopId;

    /**
     * 选关文本小人编号。
     */
    private final Integer portraitId;

    /**
     * 选关对话框样式编号。
     */
    private final Integer balloonStyleId;

    /**
     * 敌机种类槽位。
     */
    private final List<Integer> enemyTypes;

    /**
     * 与敌机槽位一一对应的数量或附加参数。
     */
    private final List<Integer> enemyCounts;

    /**
     * 从 `Hell.bin` 字符串表分派到的脚本文件名。
     */
    private final String scriptFileName;

    /**
     * 当前实际加载到的脚本路径。
     */
    private final Path activeScriptPath;

    /**
     * 脚本当前命中的来源层。
     */
    private final ResourceLayer scriptLayer;

    /**
     * `HellConfig.dat` 当前命中的来源层。
     */
    private final ResourceLayer configLayer;

    @Override
    public String toString() {
        return String.format("%03d %s", index, title == null ? "(untitled)" : title);
    }
}
