package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.mek.Mek;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * mek MaterialBlock 迁移：
 * - BHE spriteGroups[spriteIndex] 为 (actionGroupNum, actionNum) 对的列表
 * - BSDX spriteGroups[spriteIndex] 只保留 actionGroupNum 列表
 * - 通过 spriteIndexMap 重映射数组索引（spriteIndex）
 * - actionGroupNum 不需要重映射，直接保留
 */
@Slf4j
public class MekMaterialConverter {

    // 是否清空演出资源组（设为 false 以保留 sprite/SE/voice 引用）
    private static final boolean CLEAR_MATERIAL_GROUPS = false;

    public Mek.MekMaterialBlock convert(
            com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock src,
            Map<Integer, Integer> spriteIndexMap,
            int voiceGroupCount
    ) {
        Mek.MekMaterialBlock dst = new Mek.MekMaterialBlock();
        if (src == null) {
            return dst;
        }

        dst.setExtraRegularCount(src.getExtraRegularCount());
        dst.setRegularCount(src.getRegularCount());
        dst.setEntries(convertEntries(src.getEntries(), spriteIndexMap, voiceGroupCount));
        dst.setRegularEntries(convertEntries(src.getRegularEntries(), spriteIndexMap, voiceGroupCount));
        dst.setTrailingEntries(convertEntries(src.getTrailingEntries(), spriteIndexMap, voiceGroupCount));

        return dst;
    }

    private List<Mek.MekMaterialBlock.PluginEntry> convertEntries(
            List<com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry> src,
            Map<Integer, Integer> spriteIndexMap,
            int voiceGroupCount
    ) {
        List<Mek.MekMaterialBlock.PluginEntry> out = new ArrayList<>();
        if (src == null) {
            return out;
        }
        for (com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry entry : src) {
            out.add(convertEntry(entry, spriteIndexMap, voiceGroupCount));
        }
        return out;
    }

    private Mek.MekMaterialBlock.PluginEntry convertEntry(
            com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry src,
            Map<Integer, Integer> spriteIndexMap,
            int voiceGroupCount
    ) {
        Mek.MekMaterialBlock.PluginEntry dst = new Mek.MekMaterialBlock.PluginEntry();
        if (src == null) {
            return dst;
        }
        dst.setOffset(src.getOffset());
        dst.setLength(src.getLength());
        if (CLEAR_MATERIAL_GROUPS) {
            // 完全置空：groupCount=0，避免触发演示资源读取
            dst.setSpriteGroups(new ArrayList<>());
            dst.setSeGroups(new ArrayList<>());
            dst.setVoiceGroups(new ArrayList<>());
            return dst;
        }
        // spriteGroups: 重映射数组索引，转换 (actionGroupNum, actionNum) 对为 actionGroupNum 列表
        dst.setSpriteGroups(convertSpriteGroups(src.getSpriteGroups(), spriteIndexMap));
        dst.setSeGroups(copyGroupList(src.getSeGroups()));
        dst.setVoiceGroups(copyGroupList(src.getVoiceGroups()));
        return dst;
    }

    /**
     * 转换 spriteGroups：
     * - BHE: spriteGroups[bheIndex] = [actionGroupNum0, actionNum0, actionGroupNum1, actionNum1, ...]
     * - BSDX: spriteGroups[bsdxIndex] = [actionGroupNum0, actionGroupNum1, ...]
     *
     * 关键：用 spriteIndexMap 重映射数组索引（bheIndex -> bsdxIndex），
     * 而不是重映射 actionGroupNum。
     */
    private List<int[]> convertSpriteGroups(List<int[]> srcGroups, Map<Integer, Integer> spriteIndexMap) {
        if (srcGroups == null) {
            return new ArrayList<>();
        }

        // 确定输出数组大小：取源大小和映射后最大索引的较大值
        int outSize = srcGroups.size();
        if (spriteIndexMap != null && !spriteIndexMap.isEmpty()) {
            int maxMappedIndex = spriteIndexMap.values().stream()
                    .mapToInt(Integer::intValue)
                    .max()
                    .orElse(0);
            outSize = Math.max(outSize, maxMappedIndex + 1);
        }

        // 初始化输出数组（全部为空数组）
        List<int[]> out = new ArrayList<>(outSize);
        for (int i = 0; i < outSize; i++) {
            out.add(new int[0]);
        }

        // 遍历源数组，转换并重映射索引
        for (int bheIndex = 0; bheIndex < srcGroups.size(); bheIndex++) {
            int[] group = srcGroups.get(bheIndex);
            if (group == null || group.length == 0) {
                continue;
            }

            // 用 spriteIndexMap 重映射数组索引
            int bsdxIndex = bheIndex;
            if (spriteIndexMap != null && spriteIndexMap.containsKey(bheIndex)) {
                bsdxIndex = spriteIndexMap.get(bheIndex);
            }

            if (bsdxIndex < 0 || bsdxIndex >= outSize) {
                log.warn("spriteGroup 索引 {} 映射到 {} 超出范围，跳过", bheIndex, bsdxIndex);
                continue;
            }

            // 转换：BHE 的 (actionGroupNum, actionNum) 对 -> BSDX 的 actionGroupNum 列表
            if (group.length % 2 != 0) {
                log.warn("spriteGroups[{}] 长度异常(非偶数): {}", bheIndex, group.length);
            }
            int pairCount = group.length / 2;
            int[] dst = new int[pairCount];
            for (int i = 0; i < pairCount; i++) {
                // 取每对的第一个值（actionGroupNum），不需要重映射
                dst[i] = group[i * 2];
            }
            out.set(bsdxIndex, dst);
        }

        return out;
    }

    private List<int[]> copyGroupList(List<int[]> srcGroups) {
        List<int[]> out = new ArrayList<>();
        if (srcGroups == null) {
            return out;
        }
        for (int[] arr : srcGroups) {
            out.add(arr == null ? new int[0] : Arrays.copyOf(arr, arr.length));
        }
        return out;
    }
}
