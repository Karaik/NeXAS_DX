package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek;

import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * BHE MEK MaterialBlock 源侧形状转换器。
 *
 * <p>本阶段只把 BHE material 转成 BSDX DTO 能承载的结构，不做目标 BSDX group 重绑，
 * 不裁剪到目标 group 数量，也不在这里决定是否置空。</p>
 *
 * <p>BHE 的 spriteGroups 组内 payload 是 actionGroup/actionNo 成对出现；
 * BSDX 的 spriteGroups 组内 payload 只保留 actionGroup。因此这里只做 pair 压缩。</p>
 */
class BheToBsdxMekMaterialConverter {

    Mek.MekMaterialBlock convert(com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock source) {
        Mek.MekMaterialBlock target = new Mek.MekMaterialBlock();
        if (source == null) {
            return target;
        }

        target.setExtraRegularCount(source.getExtraRegularCount());
        target.setRegularCount(source.getRegularCount());
        target.setEntries(convertEntries(source.getEntries()));
        target.setRegularEntries(convertEntries(source.getRegularEntries()));
        target.setTrailingEntries(convertEntries(source.getTrailingEntries()));
        return target;
    }

    private List<Mek.MekMaterialBlock.PluginEntry> convertEntries(
            List<com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry> sourceList
    ) {
        List<Mek.MekMaterialBlock.PluginEntry> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }

        for (com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry source : sourceList) {
            targetList.add(convertEntry(source));
        }
        return targetList;
    }

    private Mek.MekMaterialBlock.PluginEntry convertEntry(
            com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry source
    ) {
        Mek.MekMaterialBlock.PluginEntry target = new Mek.MekMaterialBlock.PluginEntry();
        if (source == null) {
            return target;
        }

        target.setOffset(source.getOffset());
        target.setLength(source.getLength());
        target.setSpriteGroups(convertSpriteGroups(source.getSpriteGroups()));
        target.setSeGroups(copyGroupList(source.getSeGroups()));
        target.setVoiceGroups(copyGroupList(source.getVoiceGroups()));
        return target;
    }

    private List<int[]> convertSpriteGroups(List<int[]> sourceGroups) {
        List<int[]> targetGroups = new ArrayList<>();
        if (sourceGroups == null) {
            return targetGroups;
        }

        for (int[] sourceGroup : sourceGroups) {
            targetGroups.add(convertSpriteGroupPayload(sourceGroup));
        }
        return targetGroups;
    }

    private int[] convertSpriteGroupPayload(int[] sourcePayload) {
        if (sourcePayload == null || sourcePayload.length == 0) {
            return new int[0];
        }

        int pairCount = sourcePayload.length / 2;
        int[] targetPayload = new int[pairCount];
        for (int i = 0; i < pairCount; i++) {
            // BHE pair 第二个值 actionNo 在 BSDX material 结构中没有承载位，源侧转换只保留 actionGroup。
            targetPayload[i] = sourcePayload[i * 2];
        }
        return targetPayload;
    }

    private List<int[]> copyGroupList(List<int[]> sourceGroups) {
        List<int[]> targetGroups = new ArrayList<>();
        if (sourceGroups == null) {
            return targetGroups;
        }

        for (int[] sourceGroup : sourceGroups) {
            targetGroups.add(sourceGroup == null ? new int[0] : Arrays.copyOf(sourceGroup, sourceGroup.length));
        }
        return targetGroups;
    }
}
