package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 补齐所有基线机体 .mek 的 CMaterial 组数，使其与追加后的 grp 容量对齐。
 *
 * <p>grp 追加后（如 SpriteGroup 138→139, BatVoice 30→31），每个机体的 .mek
 * 里 CMaterial 的 spriteGroups / voiceGroups 组数必须同步增长，否则引擎加载时
 * 组数对不上会导致内存分配异常（实测给了 -1 直接崩）。</p>
 *
 * <p>本 step 对每条 PluginEntry 的三段组列表做 tail-padding：末尾追加空组 {@code new int[0]}，
 * 不改动任何已有组内的 ID 数据。引擎读到空组直接跳过，无副作用。</p>
 */
public class PadBaselineMekMaterialStep {

    /**
     * 遍历基线中所有 .mek，补齐 CMaterial 的 spriteGroups / seGroups / voiceGroups 容器长度。
     *
     * @param bsdxBaseline 已完成 grp 追加的基线容器（grp 内 list 尺寸即为追加后的值）
     */
    public void padMaterialBlock(BsdxBaselineBundle bsdxBaseline) {
        if (bsdxBaseline == null) {
            return;
        }

        int targetSpriteGroups = bsdxBaseline.getSpriteGroupGrp().getSpriteList().size();
        int targetSeGroups = bsdxBaseline.getSeGroupGrp().getSeList().size();
        int targetVoiceGroups = bsdxBaseline.getBatVoiceGrp().getVoiceList().size();

        for (Map.Entry<String, Mek> entry : bsdxBaseline.getMekByFileName().entrySet()) {
            Mek mek = entry.getValue();
            Mek.MekMaterialBlock materialBlock = mek.getMekMaterialBlock();
            if (materialBlock == null) {
                continue;
            }

            padEntries(materialBlock.getEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
            padEntries(materialBlock.getRegularEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
            padEntries(materialBlock.getTrailingEntries(), targetSpriteGroups, targetSeGroups, targetVoiceGroups);
        }
    }

    /**
     * 对单条 PluginEntry 列表做 tail-padding。
     */
    private void padEntries(
            List<Mek.MekMaterialBlock.PluginEntry> entries,
            int targetSpriteGroups,
            int targetSeGroups,
            int targetVoiceGroups
    ) {
        if (entries == null) {
            return;
        }

        for (Mek.MekMaterialBlock.PluginEntry pe : entries) {
            if (pe == null) {
                continue;
            }

            padIntArrayList(pe.getSpriteGroups(), targetSpriteGroups);
            padIntArrayList(pe.getSeGroups(), targetSeGroups);
            padIntArrayList(pe.getVoiceGroups(), targetVoiceGroups);
        }
    }

    /**
     * 如果 list 长度不足 target，末尾补空组直到对齐。
     * 已有组的内容不做任何修改。
     */
    private void padIntArrayList(List<int[]> list, int target) {
        if (list == null) {
            return;
        }
        int current = list.size();
        if (current < target) {
            int diff = target - current;
            for (int i = 0; i < diff; i++) {
                list.add(new int[0]);
            }
        }
    }
}
