package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;

import java.util.List;

/**
 * grp 对齐工具：
 * - 优先按 codeName 匹配，若缺失则使用 fileName
 * - 未找到则优先占用 existFlag=0 的空槽，否则追加到末尾
 */
public class GrpRegistryUpdater {

    public int upsertMekaGroup(
            MekaGroupGrp bsdxGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup bheEntry
    ) {
        if (bsdxGroup == null || bheEntry == null) {
            return -1;
        }
        List<MekaGroupGrp.MekaGroup> list = bsdxGroup.getMekaList();
        String codeKey = normalizeKey(bheEntry.getMekaCodeName());

        for (int i = 0; i < list.size(); i++) {
            MekaGroupGrp.MekaGroup entry = list.get(i);
            if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                continue;
            }
            String dstKey = normalizeKey(entry.getMekaCodeName());
            if (codeKey != null && codeKey.equals(dstKey)) {
                return i;
            }
        }

        MekaGroupGrp.MekaGroup newEntry = new MekaGroupGrp.MekaGroup();
        newEntry.setExistFlag(1);
        newEntry.setMekaName(bheEntry.getMekaName());
        newEntry.setMekaCodeName(bheEntry.getMekaCodeName());

        for (int i = 0; i < list.size(); i++) {
            MekaGroupGrp.MekaGroup entry = list.get(i);
            if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                list.set(i, newEntry);
                return i;
            }
        }

        list.add(newEntry);
        return list.size() - 1;
    }

    public int upsertWazaGroup(
            WazaGroupGrp bsdxGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry bheEntry
    ) {
        if (bsdxGroup == null || bheEntry == null) {
            return -1;
        }
        List<WazaGroupGrp.WazaGroupEntry> list = bsdxGroup.getWazaList();
        String codeKey = normalizeKey(bheEntry.getWazaCodeName());
        String displayKey = normalizeKey(bheEntry.getWazaDisplayName());

        for (int i = 0; i < list.size(); i++) {
            WazaGroupGrp.WazaGroupEntry entry = list.get(i);
            if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                continue;
            }
            String dstCode = normalizeKey(entry.getWazaCodeName());
            String dstDisplay = normalizeKey(entry.getWazaDisplayName());
            if ((codeKey != null && codeKey.equals(dstCode)) ||
                    (displayKey != null && displayKey.equals(dstDisplay))) {
                return i;
            }
        }

        WazaGroupGrp.WazaGroupEntry newEntry = new WazaGroupGrp.WazaGroupEntry();
        newEntry.setExistFlag(1);
        newEntry.setWazaName(bheEntry.getWazaName());
        newEntry.setWazaCodeName(bheEntry.getWazaCodeName());
        newEntry.setWazaDisplayName(bheEntry.getWazaDisplayName());
        newEntry.setParam(bheEntry.getParam());

        for (int i = 0; i < list.size(); i++) {
            WazaGroupGrp.WazaGroupEntry entry = list.get(i);
            if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                list.set(i, newEntry);
                return i;
            }
        }

        list.add(newEntry);
        return list.size() - 1;
    }

    public int upsertSpriteGroup(
            SpriteGroupGrp bsdxGroup,
            com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry bheEntry
    ) {
        if (bsdxGroup == null || bheEntry == null) {
            return -1;
        }
        List<SpriteGroupGrp.SpriteGroupEntry> list = bsdxGroup.getSpriteList();
        String codeKey = normalizeKey(bheEntry.getSpriteCodeName());
        String fileKey = normalizeKey(bheEntry.getSpriteFileName());

        for (int i = 0; i < list.size(); i++) {
            SpriteGroupGrp.SpriteGroupEntry entry = list.get(i);
            if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                continue;
            }
            String dstCode = normalizeKey(entry.getSpriteCodeName());
            String dstFile = normalizeKey(entry.getSpriteFileName());
            if ((codeKey != null && codeKey.equals(dstCode)) ||
                    (fileKey != null && fileKey.equals(dstFile))) {
                return i;
            }
        }

        SpriteGroupGrp.SpriteGroupEntry newEntry = new SpriteGroupGrp.SpriteGroupEntry();
        newEntry.setExistFlag(1);
        newEntry.setSpriteFileName(bheEntry.getSpriteFileName());
        newEntry.setSpriteCodeName(bheEntry.getSpriteCodeName());
        newEntry.setParam(bheEntry.getParam());

        for (int i = 0; i < list.size(); i++) {
            SpriteGroupGrp.SpriteGroupEntry entry = list.get(i);
            if (entry == null || isEmptyEntry(entry.getExistFlag())) {
                list.set(i, newEntry);
                return i;
            }
        }

        list.add(newEntry);
        return list.size() - 1;
    }

    private boolean isEmptyEntry(Integer existFlag) {
        return existFlag == null || existFlag == 0;
    }

    private String normalizeKey(String value) {
        return value == null ? null : value.trim().toLowerCase();
    }
}
