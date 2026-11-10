package com.giga.nexas.controller.model;

import com.giga.nexas.controller.support.HellConfigLayout;
import lombok.Builder;
import lombok.Getter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Hell 关卡元数据编辑草稿。
 *
 * <p>它是右侧元数据面板和写回服务之间的中间对象，
 * 避免 controller 直接操作 `Dat` 行数组。
 */
@Getter
@Builder
public class HellStageMetadataDraft {

    private final int index;
    private final String title;
    private final String description;
    private final Integer hellLevel;
    private final Integer mapId;
    private final Integer shopId;
    private final Integer portraitId;
    private final Integer balloonStyleId;
    private final List<Integer> enemyTypes;
    private final List<Integer> enemyCounts;

    /**
     * 从当前选中的关卡描述生成一个可编辑草稿。
     *
     * <p>这个方法属于 Hell Script Editor 的表单装载层，
     * 负责把树节点上的只读描述对象稳定转换成右侧面板可写草稿。
     * 它不会改动任何底层数据，也不会丢失槽位数量信息。
     */
    public static HellStageMetadataDraft fromStage(HellStageDescriptor stage) {
        if (stage == null) {
            return null;
        }
        return HellStageMetadataDraft.builder()
                .index(stage.getIndex())
                .title(stage.getTitle())
                .description(stage.getDescription())
                .hellLevel(stage.getHellLevel())
                .mapId(stage.getMapId())
                .shopId(stage.getShopId())
                .portraitId(stage.getPortraitId())
                .balloonStyleId(stage.getBalloonStyleId())
                .enemyTypes(normalizeSlots(stage.getEnemyTypes()))
                .enemyCounts(normalizeSlots(stage.getEnemyCounts()))
                .build();
    }

    /**
     * 规范化敌机槽列表长度。
     *
     * <p>`HellConfig.dat` 的敌机类型/数量都固定为 8 个槽位。
     * 这里无论上游列表长度如何，都补齐到固定槽位数，避免 UI 编辑时槽位错位。
     */
    private static List<Integer> normalizeSlots(List<Integer> source) {
        List<Integer> values = new ArrayList<>(Collections.nCopies(HellConfigLayout.ENEMY_SLOT_COUNT, null));
        if (source != null) {
            for (int i = 0; i < Math.min(source.size(), values.size()); i++) {
                values.set(i, source.get(i));
            }
        }
        return List.copyOf(values);
    }
}
