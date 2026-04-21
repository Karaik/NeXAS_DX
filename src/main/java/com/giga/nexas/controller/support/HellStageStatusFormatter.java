package com.giga.nexas.controller.support;

import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.ResourceLayer;

/**
 * Hell Script Editor 的来源状态文本格式化器。
 *
 * <p>这一层属于 UI 文本支撑层，负责把 `ROOT / MOD` 覆盖来源、
 * 关卡索引和脚本文件名整理成统一显示文本。
 *
 * <p>输入是关卡描述对象和关卡总数，输出是顶部状态、详情 Source 标签、
 * 底部状态栏可直接显示的英文文本。
 */
public final class HellStageStatusFormatter {

    private HellStageStatusFormatter() {
    }

    /**
     * 格式化详情面板里的来源标签。
     */
    public static String formatLayerSummary(HellStageDescriptor stage) {
        if (stage == null) {
            return "-";
        }
        return "config=" + formatLayer(stage.getConfigLayer()) + ", script=" + formatLayer(stage.getScriptLayer());
    }

    /**
     * 格式化 Hell Script Editor 顶部和底部共用的状态摘要。
     *
     * <p>这里显式保留选中关卡索引与 `ROOT / MOD` 命中结果，
     * 用于在保存后、重新打开后快速确认当前实际加载的是哪一层。
     */
    public static String formatEditorSummary(int stageCount, HellStageDescriptor selectedStage) {
        if (selectedStage == null || selectedStage.getIndex() < 0) {
            return "Hell Script Editor | " + stageCount + " stages loaded.";
        }
        String scriptName = selectedStage.getScriptFileName() == null ? "-" : selectedStage.getScriptFileName();
        return String.format(
                "Hell Script Editor | %d stages | selected %03d | config=%s | script=%s | %s",
                stageCount,
                selectedStage.getIndex(),
                formatLayer(selectedStage.getConfigLayer()),
                formatLayer(selectedStage.getScriptLayer()),
                scriptName
        );
    }

    /**
     * 统一 `ResourceLayer` 的空值和枚举展示。
     */
    private static String formatLayer(ResourceLayer layer) {
        return layer == null ? "-" : layer.name();
    }
}
