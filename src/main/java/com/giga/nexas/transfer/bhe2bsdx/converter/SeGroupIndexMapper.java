package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * SE Group 索引映射器。
 * 基于 seFileName 做 BHE -> BSDX 的 group/seq 映射。
 */
@Slf4j
public class SeGroupIndexMapper {

    public static final int DEFAULT_APPEND_GROUP_INDEX = 11;

    public SeGroupMap build(
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp bheSeGroup,
            SeGroupGrp bsdxSeGroup
    ) {
        return build(bheSeGroup, bsdxSeGroup, DEFAULT_APPEND_GROUP_INDEX);
    }

    public SeGroupMap build(
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp bheSeGroup,
            SeGroupGrp bsdxSeGroup,
            int appendGroupIndex
    ) {
        SeGroupMap result = new SeGroupMap();
        if (bheSeGroup == null || bsdxSeGroup == null) {
            return result;
        }
        if (bheSeGroup.getSeList() == null || bheSeGroup.getSeList().isEmpty()) {
            return result;
        }

        Map<String, GroupSeq> bsdxNameIndex = buildBsdxNameIndex(bsdxSeGroup);
        SeGroupGrp.SeGroupGroup appendGroup = ensureAppendGroup(bsdxSeGroup, appendGroupIndex);

        List<com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupGroup> bheGroups = bheSeGroup.getSeList();
        for (int bheGroupIndex = 0; bheGroupIndex < bheGroups.size(); bheGroupIndex++) {
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupGroup bheGroup = bheGroups.get(bheGroupIndex);
            if (bheGroup == null || bheGroup.getSeItems() == null) {
                continue;
            }
            List<com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem> bheItems = bheGroup.getSeItems();
            for (int bheSeqIndex = 0; bheSeqIndex < bheItems.size(); bheSeqIndex++) {
                com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem bheItem = bheItems.get(bheSeqIndex);
                if (bheItem == null) {
                    continue;
                }
                String fileName = bheItem.getSeFileName();
                String normalizedName = normalizeSeName(fileName);
                if (normalizedName.isEmpty()) {
                    continue;
                }

                GroupSeq mapped = bsdxNameIndex.get(normalizedName);
                if (mapped == null) {
                    mapped = appendToBsdxGroup(appendGroup, appendGroupIndex, bheItem);
                    if (mapped != null) {
                        bsdxNameIndex.put(normalizedName, mapped);
                        result.appendedItems++;
                    }
                }

                if (mapped != null) {
                    result.put(bheGroupIndex, bheSeqIndex, mapped.groupIndex, mapped.seqIndex);
                }
            }
        }

        if (result.appendedItems > 0) {
            log.info("segroup mapping appended {} missing items into BSDX group {}", result.appendedItems, appendGroupIndex);
        }

        return result;
    }

    private Map<String, GroupSeq> buildBsdxNameIndex(SeGroupGrp bsdxSeGroup) {
        Map<String, GroupSeq> out = new HashMap<>();
        if (bsdxSeGroup.getSeList() == null) {
            bsdxSeGroup.setSeList(new ArrayList<>());
            return out;
        }
        for (int groupIndex = 0; groupIndex < bsdxSeGroup.getSeList().size(); groupIndex++) {
            SeGroupGrp.SeGroupGroup group = bsdxSeGroup.getSeList().get(groupIndex);
            if (group == null || group.getSeItems() == null) {
                continue;
            }
            for (int seqIndex = 0; seqIndex < group.getSeItems().size(); seqIndex++) {
                SeGroupGrp.SeGroupItem item = group.getSeItems().get(seqIndex);
                if (item == null) {
                    continue;
                }
                String name = normalizeSeName(item.getSeFileName());
                if (!name.isEmpty() && !out.containsKey(name)) {
                    out.put(name, new GroupSeq(groupIndex, seqIndex));
                }
            }
        }
        return out;
    }

    private SeGroupGrp.SeGroupGroup ensureAppendGroup(SeGroupGrp bsdxSeGroup, int appendGroupIndex) {
        if (bsdxSeGroup.getSeList() == null) {
            bsdxSeGroup.setSeList(new ArrayList<>());
        }
        while (bsdxSeGroup.getSeList().size() <= appendGroupIndex) {
            SeGroupGrp.SeGroupGroup group = new SeGroupGrp.SeGroupGroup();
            group.setExistFlag(1);
            group.setSeType("AUTO_MIGRATED");
            group.setSeTypeCodeName("AUTO_MIGRATED");
            group.setSeItems(new ArrayList<>());
            bsdxSeGroup.getSeList().add(group);
        }
        SeGroupGrp.SeGroupGroup group = bsdxSeGroup.getSeList().get(appendGroupIndex);
        if (group == null) {
            group = new SeGroupGrp.SeGroupGroup();
            group.setExistFlag(1);
            group.setSeType("AUTO_MIGRATED");
            group.setSeTypeCodeName("AUTO_MIGRATED");
            group.setSeItems(new ArrayList<>());
            bsdxSeGroup.getSeList().set(appendGroupIndex, group);
        }
        if (group.getSeItems() == null) {
            group.setSeItems(new ArrayList<>());
        }
        if (group.getExistFlag() == null) {
            group.setExistFlag(1);
        }
        return group;
    }

    private GroupSeq appendToBsdxGroup(
            SeGroupGrp.SeGroupGroup appendGroup,
            int appendGroupIndex,
            com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.SeGroupItem source
    ) {
        if (appendGroup == null || source == null) {
            return null;
        }
        SeGroupGrp.SeGroupItem target = new SeGroupGrp.SeGroupItem();
        target.setExistFlag(1);
        target.setSeFileName(source.getSeFileName());
        target.setSeItemName(orDefault(source.getSeItemName(), source.getSeFileName(), "AUTO_MIGRATED"));
        target.setSeItemCodeName(orDefault(source.getSeItemCodeName(), source.getSeFileName(), "AUTO_MIGRATED"));
        int seqIndex = appendGroup.getSeItems().size();
        appendGroup.getSeItems().add(target);
        return new GroupSeq(appendGroupIndex, seqIndex);
    }

    private String orDefault(String primary, String secondary, String fallback) {
        if (primary != null && !primary.isBlank()) {
            return primary;
        }
        if (secondary != null && !secondary.isBlank()) {
            return secondary;
        }
        return fallback;
    }

    private String normalizeSeName(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private static long pairKey(int groupIndex, int seqIndex) {
        return ((long) groupIndex << 32) ^ (seqIndex & 0xFFFF_FFFFL);
    }

    public static final class SeGroupMap {
        private final Map<Long, GroupSeq> pairMap = new HashMap<>();
        private int appendedItems;

        private void put(int bheGroup, int bheSeq, int bsdxGroup, int bsdxSeq) {
            pairMap.put(pairKey(bheGroup, bheSeq), new GroupSeq(bsdxGroup, bsdxSeq));
        }

        public boolean isEmpty() {
            return pairMap.isEmpty();
        }

        public int getMappedPairCount() {
            return pairMap.size();
        }

        public int getAppendedItems() {
            return appendedItems;
        }

        public byte[] remapBlock(byte[] source) {
            if (source == null) {
                return null;
            }
            byte[] out = Arrays.copyOf(source, source.length);
            remapBlockInPlace(out);
            return out;
        }

        public void remapBlockInPlace(byte[] buffer) {
            if (buffer == null || buffer.length < 8) {
                return;
            }
            int groupIndex = readIntLE(buffer, 0);
            int seqIndex = readIntLE(buffer, 4);
            GroupSeq mapped = pairMap.get(pairKey(groupIndex, seqIndex));
            if (mapped == null) {
                return;
            }
            writeIntLE(buffer, 0, mapped.groupIndex);
            writeIntLE(buffer, 4, mapped.seqIndex);
        }

        private int readIntLE(byte[] buffer, int offset) {
            return (buffer[offset] & 0xFF)
                    | ((buffer[offset + 1] & 0xFF) << 8)
                    | ((buffer[offset + 2] & 0xFF) << 16)
                    | ((buffer[offset + 3] & 0xFF) << 24);
        }

        private void writeIntLE(byte[] buffer, int offset, int value) {
            buffer[offset] = (byte) (value & 0xFF);
            buffer[offset + 1] = (byte) ((value >>> 8) & 0xFF);
            buffer[offset + 2] = (byte) ((value >>> 16) & 0xFF);
            buffer[offset + 3] = (byte) ((value >>> 24) & 0xFF);
        }
    }

    private static final class GroupSeq {
        private final int groupIndex;
        private final int seqIndex;

        private GroupSeq(int groupIndex, int seqIndex) {
            this.groupIndex = groupIndex;
            this.seqIndex = seqIndex;
        }
    }
}
