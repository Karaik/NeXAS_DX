package com.giga.nexas.controller.support;

/**
 * BSDX `HellConfig.dat` 的统一列布局定义。
 *
 * <p>这一层属于 Hell Script Editor 的基础格式层，负责把调查确认过的
 * `HellConfig.dat` 列号语义集中到一个地方，避免 loader、保存服务、
 * 蓝本复制逻辑各自散落裸列号。
 *
 * <p>输入是 `HellConfig.dat` 的行数组，输出是对各列的命名访问约定。
 * 这里不承担读写行为，只负责表达“哪一列代表什么”。
 */
public final class HellConfigLayout {

    /**
     * `HellConfig.dat` 当前确认的固定列数。
     */
    public static final int COLUMN_COUNT = 23;

    /**
     * 选关标题文本。
     */
    public static final int TITLE = 0;

    /**
     * 地狱度 / 难度显示值。
     */
    public static final int HELL_LEVEL = 1;

    /**
     * 地图编号。
     */
    public static final int MAP_ID = 2;

    /**
     * 敌机类型槽起始列。
     */
    public static final int ENEMY_TYPE_START = 3;

    /**
     * 敌机类型槽结束列。
     */
    public static final int ENEMY_TYPE_END = 10;

    /**
     * 敌机数量槽起始列。
     */
    public static final int ENEMY_COUNT_START = 11;

    /**
     * 敌机数量槽结束列。
     */
    public static final int ENEMY_COUNT_END = 18;

    /**
     * 亚季商店编号。
     */
    public static final int SHOP_ID = 19;

    /**
     * 选关文本小人编号。
     */
    public static final int PORTRAIT_ID = 20;

    /**
     * 对话框样式编号。
     */
    public static final int BALLOON_STYLE_ID = 21;

    /**
     * 关卡说明文本。
     */
    public static final int DESCRIPTION = 22;

    /**
     * 敌机槽总数。
     */
    public static final int ENEMY_SLOT_COUNT = ENEMY_TYPE_END - ENEMY_TYPE_START + 1;

    private HellConfigLayout() {
    }
}
