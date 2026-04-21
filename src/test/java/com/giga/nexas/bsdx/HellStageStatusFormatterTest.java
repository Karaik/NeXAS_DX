package com.giga.nexas.bsdx;

import com.giga.nexas.controller.model.HellStageDescriptor;
import com.giga.nexas.controller.model.ResourceLayer;
import com.giga.nexas.controller.support.HellStageStatusFormatter;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Hell Script Editor 来源状态文本的回归测试。
 *
 * <p>这组测试覆盖 Stage 3 的显示约定：
 * 同一份 `ROOT / MOD` 命中结果必须能稳定同步到详情 Source 和底部状态栏。
 */
class HellStageStatusFormatterTest {

    /**
     * 验证详情面板与状态栏使用的来源文本都包含明确的 `ROOT / MOD` 结果。
     */
    @Test
    void formatterProducesStableRootModLabels() {
        HellStageDescriptor stage = HellStageDescriptor.builder()
                .index(14)
                .title("Sample")
                .scriptFileName("Hell100.bin")
                .configLayer(ResourceLayer.MOD)
                .scriptLayer(ResourceLayer.ROOT)
                .build();

        Assertions.assertEquals("config=MOD, script=ROOT", HellStageStatusFormatter.formatLayerSummary(stage));

        String summary = HellStageStatusFormatter.formatEditorSummary(100, stage);
        Assertions.assertTrue(summary.contains("selected 014"));
        Assertions.assertTrue(summary.contains("config=MOD"));
        Assertions.assertTrue(summary.contains("script=ROOT"));
        Assertions.assertTrue(summary.contains("Hell100.bin"));
    }
}
