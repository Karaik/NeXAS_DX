package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.mek.Mek;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * mek MaterialBlock 迁移：
 * - BHE spriteGroups 为 <u32,u32> 对，BSDX 只保留第一项
 * - 通过 spriteIndexMap 做索引修正
 * - 未匹配到的索引会保留原值并记录警告
 */
@Slf4j
public class MekMaterialConverter {

    // 默认按协议置空：保留组数量，但清空每组内容，避免错误映射
    private static final boolean CLEAR_MATERIAL_GROUPS = true;

    public Mek.MekMaterialBlock convert(
            com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock src,
            Map<Integer, Integer> spriteIndexMap
    ) {
        Mek.MekMaterialBlock dst = new Mek.MekMaterialBlock();
        if (src == null) {
            return dst;
        }

        dst.setExtraRegularCount(src.getExtraRegularCount());
        dst.setRegularCount(src.getRegularCount());
        dst.setEntries(convertEntries(src.getEntries(), spriteIndexMap));
        dst.setRegularEntries(convertEntries(src.getRegularEntries(), spriteIndexMap));
        dst.setTrailingEntries(convertEntries(src.getTrailingEntries(), spriteIndexMap));

        return dst;
    }

    private List<Mek.MekMaterialBlock.PluginEntry> convertEntries(
            List<com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry> src,
            Map<Integer, Integer> spriteIndexMap
    ) {
        List<Mek.MekMaterialBlock.PluginEntry> out = new ArrayList<>();
        if (src == null) {
            return out;
        }
        for (com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry entry : src) {
            out.add(convertEntry(entry, spriteIndexMap));
        }
        return out;
    }

    private Mek.MekMaterialBlock.PluginEntry convertEntry(
            com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry src,
            Map<Integer, Integer> spriteIndexMap
    ) {
        Mek.MekMaterialBlock.PluginEntry dst = new Mek.MekMaterialBlock.PluginEntry();
        if (src == null) {
            return dst;
        }
        dst.setOffset(src.getOffset());
        dst.setLength(src.getLength());
        if (CLEAR_MATERIAL_GROUPS) {
            dst.setSpriteGroups(emptyGroupsLike(src.getSpriteGroups()));
            dst.setSeGroups(emptyGroupsLike(src.getSeGroups()));
            dst.setVoiceGroups(emptyGroupsLike(src.getVoiceGroups()));
            return dst;
        }
        // spriteGroups: BHE 的每个组是成对数据，BSDX 只保留第一项
        dst.setSpriteGroups(convertSpriteGroups(src.getSpriteGroups(), spriteIndexMap));
        dst.setSeGroups(copyGroupList(src.getSeGroups()));
        dst.setVoiceGroups(copyGroupList(src.getVoiceGroups()));
        return dst;
    }

    private List<int[]> convertSpriteGroups(List<int[]> srcGroups, Map<Integer, Integer> spriteIndexMap) {
        List<int[]> out = new ArrayList<>();
        if (srcGroups == null) {
            return out;
        }
        for (int[] group : srcGroups) {
            if (group == null) {
                out.add(new int[0]);
                continue;
            }
            if (group.length % 2 != 0) {
                log.warn("spriteGroups 长度异常(非偶数): {}", group.length);
            }
            int pairCount = group.length / 2;
            int[] dst = new int[pairCount];
            for (int i = 0; i < pairCount; i++) {
                int bheIndex = group[i * 2];
                dst[i] = remapSpriteGroupIndex(bheIndex, spriteIndexMap);
            }
            out.add(dst);
        }
        return out;
    }

    private List<int[]> emptyGroupsLike(List<int[]> srcGroups) {
        List<int[]> out = new ArrayList<>();
        if (srcGroups == null) {
            return out;
        }
        for (int i = 0; i < srcGroups.size(); i++) {
            out.add(new int[0]);
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

    private int remapSpriteGroupIndex(int bheIndex, Map<Integer, Integer> spriteIndexMap) {
        if (spriteIndexMap == null || spriteIndexMap.isEmpty()) {
            return bheIndex;
        }
        Integer mapped = spriteIndexMap.get(bheIndex);
        if (mapped == null) {
            log.warn("spriteGroup 索引 {} 无法映射，保留原值", bheIndex);
            return bheIndex;
        }
        return mapped;
    }
}
