package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * spritegroup 索引映射：
 * - 以 BHE grp 为基准，确保 BSDX 中存在对应条目（必要时追加/占空槽）
 * - 只对“需要的索引”建立映射，避免无关条目污染
 */
public class SpriteGroupIndexMapper {

    private final GrpRegistryUpdater grpRegistryUpdater = new GrpRegistryUpdater();

    public Map<Integer, Integer> buildMap(
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheGroup,
            SpriteGroupGrp bsdxGroup,
            java.util.Set<Integer> requiredIndices
    ) {
        Map<Integer, Integer> map = new HashMap<>();
        if (bheGroup == null || bsdxGroup == null) {
            return map;
        }

        // requiredIndices 为空时默认映射全部 BHE 条目
        List<com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry> bheList = bheGroup.getSpriteList();
        for (int i = 0; i < bheList.size(); i++) {
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry bheEntry = bheList.get(i);
            if (requiredIndices != null && !requiredIndices.contains(i)) {
                continue;
            }
            if (bheEntry == null || isEmptyEntry(bheEntry.getExistFlag())) {
                continue;
            }
            // 确保 BSDX 中存在对应条目（不存在则追加/占空槽）
            int mapped = grpRegistryUpdater.upsertSpriteGroup(bsdxGroup, bheEntry);
            if (mapped >= 0) {
                map.put(i, mapped);
            }
        }

        return map;
    }

    /**
     * 仅按名称建立映射（不修改 BSDX grp）。
     * - 优先按 spriteFileName 匹配
     * - 再按 spriteCodeName 匹配
     */
    public Map<Integer, Integer> buildMapByName(
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheGroup,
            SpriteGroupGrp bsdxGroup,
            java.util.Set<Integer> requiredIndices
    ) {
        Map<Integer, Integer> map = new HashMap<>();
        if (bheGroup == null || bsdxGroup == null) {
            return map;
        }

        Map<String, Integer> bsdxFileIndex = new HashMap<>();
        Map<String, Integer> bsdxBaseIndex = new HashMap<>();
        Map<String, Integer> bsdxCodeIndex = new HashMap<>();

        List<SpriteGroupGrp.SpriteGroupEntry> bsdxList = bsdxGroup.getSpriteList();
        if (bsdxList != null) {
            for (int i = 0; i < bsdxList.size(); i++) {
                SpriteGroupGrp.SpriteGroupEntry entry = bsdxList.get(i);
                if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                    continue;
                }
                String fileName = normalizeFileName(entry.getSpriteFileName());
                String baseName = normalizeBaseName(fileName);
                String codeName = normalizeKey(entry.getSpriteCodeName());
                if (fileName != null) {
                    bsdxFileIndex.putIfAbsent(fileName, i);
                }
                if (baseName != null) {
                    bsdxBaseIndex.putIfAbsent(baseName, i);
                }
                if (codeName != null) {
                    bsdxCodeIndex.putIfAbsent(codeName, i);
                }
            }
        }

        List<com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry> bheList = bheGroup.getSpriteList();
        if (bheList == null) {
            return map;
        }
        for (int i = 0; i < bheList.size(); i++) {
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry bheEntry = bheList.get(i);
            if (requiredIndices != null && !requiredIndices.contains(i)) {
                continue;
            }
            if (bheEntry == null || isEmptyEntry(bheEntry.getExistFlag())) {
                continue;
            }
            String fileName = normalizeFileName(bheEntry.getSpriteFileName());
            String baseName = normalizeBaseName(fileName);
            String codeName = normalizeKey(bheEntry.getSpriteCodeName());

            Integer mapped = null;
            if (fileName != null) {
                mapped = bsdxFileIndex.get(fileName);
            }
            if (mapped == null && baseName != null) {
                mapped = bsdxBaseIndex.get(baseName);
            }
            if (mapped == null && codeName != null) {
                mapped = bsdxCodeIndex.get(codeName);
            }
            if (mapped != null) {
                map.put(i, mapped);
            }
        }
        return map;
    }

    /**
     * 在 BHE spritegroup 中定位条目索引（按 code/fileName）。
     */
    public int findBheSpriteIndex(
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry bheEntry
    ) {
        if (bheGroup == null || bheEntry == null || bheGroup.getSpriteList() == null) {
            return -1;
        }
        String codeKey = normalizeKey(bheEntry.getSpriteCodeName());
        String fileKey = normalizeFileName(bheEntry.getSpriteFileName());
        for (int i = 0; i < bheGroup.getSpriteList().size(); i++) {
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry =
                    bheGroup.getSpriteList().get(i);
            if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                continue;
            }
            String entryCode = normalizeKey(entry.getSpriteCodeName());
            String entryFile = normalizeFileName(entry.getSpriteFileName());
            if (codeKey != null && codeKey.equals(entryCode)) {
                return i;
            }
            if (fileKey != null && fileKey.equals(entryFile)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 从 BHE mek 的 materialBlock 中抽取实际引用的 spritegroup 索引。
     */
    public java.util.Set<Integer> collectRequiredIndicesFromMek(com.giga.nexas.dto.bhe.mek.Mek bheMek) {
        java.util.Set<Integer> indices = new java.util.HashSet<>();
        if (bheMek == null || bheMek.getMekMaterialBlock() == null) {
            return indices;
        }

        List<com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry> entries =
                bheMek.getMekMaterialBlock().getEntries();
        if (entries == null || entries.isEmpty()) {
            // fallback: regular + trailing
            entries = new java.util.ArrayList<>();
            if (bheMek.getMekMaterialBlock().getRegularEntries() != null) {
                entries.addAll(bheMek.getMekMaterialBlock().getRegularEntries());
            }
            if (bheMek.getMekMaterialBlock().getTrailingEntries() != null) {
                entries.addAll(bheMek.getMekMaterialBlock().getTrailingEntries());
            }
        }

        for (com.giga.nexas.dto.bhe.mek.Mek.MekMaterialBlock.PluginEntry entry : entries) {
            if (entry == null || entry.getSpriteGroups() == null) {
                continue;
            }
            for (int[] group : entry.getSpriteGroups()) {
                if (group == null) {
                    continue;
                }
                int pairCount = group.length / 2;
                for (int i = 0; i < pairCount; i++) {
                    indices.add(group[i * 2]);
                }
            }
        }
        return indices;
    }

    private boolean isEmptyEntry(Integer existFlag) {
        return existFlag == null || existFlag == 0;
    }

    private String normalizeKey(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }

    private String normalizeFileName(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim().replace("\\", "/");
        if (trimmed.isEmpty()) {
            return null;
        }
        int slash = trimmed.lastIndexOf('/');
        String name = slash >= 0 && slash + 1 < trimmed.length() ? trimmed.substring(slash + 1) : trimmed;
        return name.isEmpty() ? null : name.toLowerCase();
    }

    private String normalizeBaseName(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dot = fileName.lastIndexOf('.');
        String base = dot > 0 ? fileName.substring(0, dot) : fileName;
        return base.isEmpty() ? null : base.toLowerCase();
    }
}
