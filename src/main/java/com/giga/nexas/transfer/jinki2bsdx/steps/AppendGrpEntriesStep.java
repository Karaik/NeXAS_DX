package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 负责把当前机体资源链真正挂进 BSDX 基线，并产出最终的源到目标映射表。
 */
public class AppendGrpEntriesStep {

    public GrpAppendPlan appendAkaoBranch(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan
    ) {
        validateInputs(request, jinkiPackage, bsdxBaseline, importPlan);

        GrpAppendPlan plan = new GrpAppendPlan();

        // 1. 先挂主机体入口。
        MekaGroupGrp.MekaGroup sourceMeka = findRequiredMekaGroup(
                jinkiPackage.getMekaGroupGrp(),
                request.getMekaCodeName()
        );
        plan.setMekaGroupIndex(upsertMekaGroup(request, bsdxBaseline.getMekaGroupGrp(), sourceMeka));

        // 2. 再挂主 waz 入口。
        WazaGroupGrp.WazaGroupEntry sourceMainWaz = findRequiredWazaGroup(
                jinkiPackage.getWazaGroupGrp(),
                request.getWazCodeName()
        );
        plan.setWazaGroupIndex(upsertWazaGroup(bsdxBaseline.getWazaGroupGrp(), sourceMainWaz));
        Integer sourceMainWazIndex = importPlan.getSourceWazIndexByFileName().get(normalizeFileName(request.getWazFileName()));
        if (sourceMainWazIndex != null) {
            plan.getSourceWazGroupIndexToTargetIndex().put(sourceMainWazIndex, plan.getWazaGroupIndex());
            importPlan.getTargetWazIndexByFileName().put(normalizeFileName(request.getWazFileName()), plan.getWazaGroupIndex());
        }

        // 3. 再挂主 sprite 入口。
        SpriteGroupGrp.SpriteGroupEntry sourceMainSprite = findRequiredSpriteGroup(
                jinkiPackage.getSpriteGroupGrp(),
                request.getSpriteCodeName(),
                request.getSpriteFileName()
        );
        plan.setSpriteGroupIndex(upsertSpriteGroup(bsdxBaseline.getSpriteGroupGrp(), sourceMainSprite));
        Integer sourceMainSpriteIndex = importPlan.getSourceSpriteIndexByFileName().get(normalizeFileName(request.getSpriteFileName()));
        if (sourceMainSpriteIndex != null) {
            plan.getSourceSpriteGroupIndexToTargetIndex().put(sourceMainSpriteIndex, plan.getSpriteGroupIndex());
            importPlan.getTargetSpriteIndexByFileName().put(normalizeFileName(request.getSpriteFileName()), plan.getSpriteGroupIndex());
        }

        // 4. 再挂 AKAO 的语音组入口。
        IndexedBatVoiceGroup sourceBatVoice = findRequiredBatVoiceGroup(
                jinkiPackage.getBatVoiceGrp(),
                request.getMekaCodeName()
        );
        plan.setBatVoiceGroupIndex(upsertBatVoiceGroup(bsdxBaseline.getBatVoiceGrp(), sourceBatVoice.group()));
        plan.getSourceBatVoiceGroupIndexToTargetIndex().put(sourceBatVoice.index(), plan.getBatVoiceGroupIndex());

        // 5. 挂当前机体通过 CEventWazaSelect 用到的辅助 waz 链。
        // WazaGroup 当前只追加主 AKAO 条目；Akao.waz 里引用到的 0..6 共享辅助 waz 继续复用 BSDX 现有索引。
        mapReferencedWazGroups(importPlan, plan);

        // 5-1. 主 AKAO 和外部实际引用到的共通 waz，都要把 grp.param 重算到真实 skill 数量。
        //      当前策略是：主 AKAO 用 JINKI 的 Akao.waz；共享辅助 waz 继续复用 BSDX 现有文件。
        recalculateReferencedWazaParams(request, jinkiPackage, bsdxBaseline, importPlan, plan);

        // 6. 挂当前机体通过 CEventSprite 用到的辅助 sprite 链。
        appendReferencedSpriteGroups(jinkiPackage, bsdxBaseline, importPlan, plan);

        // 7. 挂当前机体通过 CEventSe 用到的 se 组和组内条目链。
        appendReferencedSeChain(jinkiPackage, bsdxBaseline, importPlan, plan);

        return plan;
    }

    private void mapReferencedWazGroups(JinkiImportPlan importPlan, GrpAppendPlan plan) {
        for (Map.Entry<Integer, String> entry : importPlan.getReferencedSourceWazFileNameByGroupIndex().entrySet()) {
            Integer sourceIndex = entry.getKey();
            if (plan.getSourceWazGroupIndexToTargetIndex().containsKey(sourceIndex)) {
                continue;
            }

            Integer targetIndex = importPlan.getTargetWazIndexByFileName().get(normalizeFileName(entry.getValue()));
            if (targetIndex != null && targetIndex >= 0) {
                plan.getSourceWazGroupIndexToTargetIndex().put(sourceIndex, targetIndex);
            }
        }
    }

    private void recalculateReferencedWazaParams(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            GrpAppendPlan plan
    ) {
        WazaGroupGrp targetWazaGroup = bsdxBaseline.getWazaGroupGrp();
        if (targetWazaGroup == null || targetWazaGroup.getWazaList() == null) {
            return;
        }

        // 主 AKAO 条目始终按 JINKI 的 Akao.waz 实际 skill 数量回写。
        Waz mainWaz = findWazByFileName(jinkiPackage.getWazByFileName(), request.getWazFileName());
        if (mainWaz != null) {
            updateWazaParam(targetWazaGroup, plan.getWazaGroupIndex(), countSkills(mainWaz));
        }

        // 外部引用到的共享辅助 waz，当前继续复用 BSDX 现有文件，所以 param 也按 BSDX 实际文件重算。
        for (Map.Entry<Integer, String> entry : importPlan.getReferencedSourceWazFileNameByGroupIndex().entrySet()) {
            Integer sourceIndex = entry.getKey();
            Integer targetIndex = plan.getSourceWazGroupIndexToTargetIndex().get(sourceIndex);
            if (targetIndex == null || targetIndex < 0) {
                continue;
            }

            // 主 AKAO 已经单独处理过，避免重复覆盖。
            if (targetIndex == plan.getWazaGroupIndex()) {
                continue;
            }

            Waz resolvedWaz = findWazByFileName(bsdxBaseline.getWazByFileName(), entry.getValue());
            if (resolvedWaz == null) {
                resolvedWaz = findWazByFileName(jinkiPackage.getWazByFileName(), entry.getValue());
            }
            if (resolvedWaz == null) {
                throw new IllegalStateException("无法找到用于重算 WazaGroup.param 的 waz 文件: " + entry.getValue());
            }

            updateWazaParam(targetWazaGroup, targetIndex, countSkills(resolvedWaz));
        }
    }

    private void updateWazaParam(WazaGroupGrp targetGroup, int targetIndex, int skillCount) {
        if (targetIndex < 0 || targetIndex >= targetGroup.getWazaList().size()) {
            throw new IllegalStateException("目标 WazaGroup 索引越界: " + targetIndex);
        }
        WazaGroupGrp.WazaGroupEntry targetEntry = targetGroup.getWazaList().get(targetIndex);
        if (targetEntry == null || !isExisting(targetEntry.getExistFlag())) {
            throw new IllegalStateException("目标 WazaGroup 条目不存在: " + targetIndex);
        }
        targetEntry.setParam(skillCount);
    }

    private int countSkills(Waz waz) {
        if (waz == null || waz.getSkillList() == null) {
            return 0;
        }
        return waz.getSkillList().size();
    }

    private Waz findWazByFileName(Map<String, Waz> wazByFileName, String fileName) {
        if (wazByFileName == null || wazByFileName.isEmpty()) {
            return null;
        }
        String normalizedTarget = normalizeFileName(fileName);
        for (Map.Entry<String, Waz> entry : wazByFileName.entrySet()) {
            if (normalizeFileName(entry.getKey()).equals(normalizedTarget)) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void appendReferencedSpriteGroups(
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            GrpAppendPlan plan
    ) {
        for (Map.Entry<Integer, String> entry : importPlan.getReferencedSourceSpriteFileNameByGroupIndex().entrySet()) {
            Integer sourceIndex = entry.getKey();
            if (plan.getSourceSpriteGroupIndexToTargetIndex().containsKey(sourceIndex)) {
                continue;
            }

            SpriteGroupGrp.SpriteGroupEntry sourceEntry = requireSourceSpriteGroupByIndex(
                    jinkiPackage.getSpriteGroupGrp(),
                    sourceIndex
            );
            int targetIndex = upsertSpriteGroup(bsdxBaseline.getSpriteGroupGrp(), sourceEntry);
            plan.getSourceSpriteGroupIndexToTargetIndex().put(sourceIndex, targetIndex);
            importPlan.getTargetSpriteIndexByFileName().put(normalizeFileName(entry.getValue()), targetIndex);
        }
    }

    private void appendReferencedSeChain(
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            GrpAppendPlan plan
    ) {
        for (Map.Entry<Integer, List<Integer>> entry : importPlan.getReferencedSourceSeItemIndicesByGroupIndex().entrySet()) {
            Integer sourceGroupIndex = entry.getKey();

            SeGroupGrp.SeGroupGroup sourceGroup = requireSourceSeGroupByIndex(
                    jinkiPackage.getSeGroupGrp(),
                    sourceGroupIndex
            );

            int targetGroupIndex = upsertSeGroup(bsdxBaseline.getSeGroupGrp(), sourceGroup);
            plan.getSourceSeGroupIndexToTargetIndex().put(sourceGroupIndex, targetGroupIndex);

            SeGroupGrp.SeGroupGroup targetGroup = bsdxBaseline.getSeGroupGrp().getSeList().get(targetGroupIndex);
            for (Integer sourceItemIndex : entry.getValue()) {
                SeGroupGrp.SeGroupItem sourceItem = requireSourceSeItemByIndex(sourceGroup, sourceItemIndex);
                int targetItemIndex = upsertSeItem(targetGroup, sourceItem);
                plan.getSourceSeItemIndexToTargetIndexByGroup()
                        .computeIfAbsent(sourceGroupIndex, key -> new LinkedHashMap<>())
                        .put(sourceItemIndex, targetItemIndex);
            }
        }
    }

    private int upsertMekaGroup(
            AkaoGraftRequest request,
            MekaGroupGrp targetGroup,
            MekaGroupGrp.MekaGroup sourceEntry
    ) {
        Integer fixedIndex = request == null ? null : request.getFixedMekaGroupIndex();
        if (fixedIndex != null && fixedIndex >= 0 && fixedIndex < targetGroup.getMekaList().size()) {
            targetGroup.getMekaList().set(fixedIndex, copyMekaGroup(sourceEntry));
            return fixedIndex;
        }

        int existingIndex = findMekaGroupIndex(targetGroup, sourceEntry.getMekaCodeName());
        if (existingIndex >= 0) {
            return existingIndex;
        }
        targetGroup.getMekaList().add(copyMekaGroup(sourceEntry));
        return targetGroup.getMekaList().size() - 1;
    }

    private int upsertWazaGroup(WazaGroupGrp targetGroup, WazaGroupGrp.WazaGroupEntry sourceEntry) {
        int existingIndex = findWazaGroupIndex(targetGroup, sourceEntry);
        if (existingIndex >= 0) {
            return existingIndex;
        }
        targetGroup.getWazaList().add(copyWazaGroup(sourceEntry));
        return targetGroup.getWazaList().size() - 1;
    }

    private int upsertSpriteGroup(SpriteGroupGrp targetGroup, SpriteGroupGrp.SpriteGroupEntry sourceEntry) {
        int existingIndex = findSpriteGroupIndex(targetGroup, sourceEntry);
        if (existingIndex >= 0) {
            return existingIndex;
        }
        targetGroup.getSpriteList().add(copySpriteGroup(sourceEntry));
        return targetGroup.getSpriteList().size() - 1;
    }

    private int upsertBatVoiceGroup(BatVoiceGrp targetGroup, BatVoiceGrp.BatVoiceGroup sourceEntry) {
        int existingIndex = findBatVoiceGroupIndex(targetGroup, sourceEntry.getCharacterCodeName());
        if (existingIndex >= 0) {
            return existingIndex;
        }
        targetGroup.getVoiceList().add(copyBatVoiceGroup(sourceEntry));
        return targetGroup.getVoiceList().size() - 1;
    }

    private int upsertSeGroup(SeGroupGrp targetGroup, SeGroupGrp.SeGroupGroup sourceEntry) {
        int existingIndex = findSeGroupIndex(targetGroup, sourceEntry);
        if (existingIndex >= 0) {
            return existingIndex;
        }
        targetGroup.getSeList().add(copySeGroupShell(sourceEntry));
        return targetGroup.getSeList().size() - 1;
    }

    private int upsertSeItem(SeGroupGrp.SeGroupGroup targetGroup, SeGroupGrp.SeGroupItem sourceItem) {
        if (targetGroup.getSeItems() == null) {
            targetGroup.setSeItems(new ArrayList<>());
        }

        int existingIndex = findSeItemIndex(targetGroup, sourceItem);
        if (existingIndex >= 0) {
            return existingIndex;
        }

        targetGroup.getSeItems().add(copySeItem(sourceItem));
        return targetGroup.getSeItems().size() - 1;
    }

    private MekaGroupGrp.MekaGroup findRequiredMekaGroup(MekaGroupGrp group, String codeName) {
        if (group == null || group.getMekaList() == null) {
            throw new IllegalStateException("JINKI MekaGroup 不存在");
        }
        for (MekaGroupGrp.MekaGroup entry : group.getMekaList()) {
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(codeName, entry.getMekaCodeName())) {
                return entry;
            }
        }
        throw new IllegalStateException("JINKI MekaGroup 中找不到目标条目: " + codeName);
    }

    private WazaGroupGrp.WazaGroupEntry findRequiredWazaGroup(WazaGroupGrp group, String codeName) {
        if (group == null || group.getWazaList() == null) {
            throw new IllegalStateException("JINKI WazaGroup 不存在");
        }
        for (WazaGroupGrp.WazaGroupEntry entry : group.getWazaList()) {
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(codeName, entry.getWazaCodeName())) {
                return entry;
            }
        }
        throw new IllegalStateException("JINKI WazaGroup 中找不到目标条目: " + codeName);
    }

    private SpriteGroupGrp.SpriteGroupEntry findRequiredSpriteGroup(
            SpriteGroupGrp group,
            String codeName,
            String fileName
    ) {
        if (group == null || group.getSpriteList() == null) {
            throw new IllegalStateException("JINKI SpriteGroup 不存在");
        }
        for (SpriteGroupGrp.SpriteGroupEntry entry : group.getSpriteList()) {
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(codeName, entry.getSpriteCodeName())
                    && equalsIgnoreCase(fileName, entry.getSpriteFileName())) {
                return entry;
            }
        }
        throw new IllegalStateException("JINKI SpriteGroup 中找不到目标条目: " + codeName + " -> " + fileName);
    }

    private IndexedBatVoiceGroup findRequiredBatVoiceGroup(BatVoiceGrp group, String codeName) {
        if (group == null || group.getVoiceList() == null) {
            throw new IllegalStateException("JINKI BatVoice 不存在");
        }
        for (int i = 0; i < group.getVoiceList().size(); i++) {
            BatVoiceGrp.BatVoiceGroup entry = group.getVoiceList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(codeName, entry.getCharacterCodeName())) {
                return new IndexedBatVoiceGroup(i, entry);
            }
        }
        throw new IllegalStateException("JINKI BatVoice 中找不到目标条目: " + codeName);
    }

    private WazaGroupGrp.WazaGroupEntry requireSourceWazaGroupByIndex(WazaGroupGrp group, int index) {
        if (group == null || group.getWazaList() == null || index < 0 || index >= group.getWazaList().size()) {
            throw new IllegalStateException("源 WazaGroup 索引越界: " + index);
        }
        WazaGroupGrp.WazaGroupEntry entry = group.getWazaList().get(index);
        if (!isExisting(entry == null ? null : entry.getExistFlag())) {
            throw new IllegalStateException("源 WazaGroup 条目不存在: " + index);
        }
        return entry;
    }

    private SpriteGroupGrp.SpriteGroupEntry requireSourceSpriteGroupByIndex(SpriteGroupGrp group, int index) {
        if (group == null || group.getSpriteList() == null || index < 0 || index >= group.getSpriteList().size()) {
            throw new IllegalStateException("源 SpriteGroup 索引越界: " + index);
        }
        SpriteGroupGrp.SpriteGroupEntry entry = group.getSpriteList().get(index);
        if (!isExisting(entry == null ? null : entry.getExistFlag())) {
            throw new IllegalStateException("源 SpriteGroup 条目不存在: " + index);
        }
        return entry;
    }

    private SeGroupGrp.SeGroupGroup requireSourceSeGroupByIndex(SeGroupGrp group, int index) {
        if (group == null || group.getSeList() == null || index < 0 || index >= group.getSeList().size()) {
            throw new IllegalStateException("源 SeGroup 索引越界: " + index);
        }
        SeGroupGrp.SeGroupGroup entry = group.getSeList().get(index);
        if (!isExisting(entry == null ? null : entry.getExistFlag())) {
            throw new IllegalStateException("源 SeGroup 条目不存在: " + index);
        }
        return entry;
    }

    private SeGroupGrp.SeGroupItem requireSourceSeItemByIndex(SeGroupGrp.SeGroupGroup group, int index) {
        if (group.getSeItems() == null || index < 0 || index >= group.getSeItems().size()) {
            throw new IllegalStateException("源 SeItem 索引越界: " + index);
        }
        SeGroupGrp.SeGroupItem entry = group.getSeItems().get(index);
        if (!isExisting(entry == null ? null : entry.getExistFlag())) {
            throw new IllegalStateException("源 SeItem 条目不存在: " + index);
        }
        return entry;
    }

    private int findMekaGroupIndex(MekaGroupGrp group, String codeName) {
        if (group == null || group.getMekaList() == null) {
            return -1;
        }
        for (int i = 0; i < group.getMekaList().size(); i++) {
            MekaGroupGrp.MekaGroup entry = group.getMekaList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(codeName, entry.getMekaCodeName())) {
                return i;
            }
        }
        return -1;
    }

    private int findWazaGroupIndex(WazaGroupGrp group, WazaGroupGrp.WazaGroupEntry sourceEntry) {
        if (group == null || group.getWazaList() == null) {
            return -1;
        }
        for (int i = 0; i < group.getWazaList().size(); i++) {
            WazaGroupGrp.WazaGroupEntry entry = group.getWazaList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(sourceEntry.getWazaCodeName(), entry.getWazaCodeName())
                    || equalsIgnoreCase(sourceEntry.getWazaDisplayName(), entry.getWazaDisplayName())) {
                return i;
            }
        }
        return -1;
    }

    private int findSpriteGroupIndex(SpriteGroupGrp group, SpriteGroupGrp.SpriteGroupEntry sourceEntry) {
        if (group == null || group.getSpriteList() == null) {
            return -1;
        }
        for (int i = 0; i < group.getSpriteList().size(); i++) {
            SpriteGroupGrp.SpriteGroupEntry entry = group.getSpriteList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(sourceEntry.getSpriteCodeName(), entry.getSpriteCodeName())
                    || equalsIgnoreCase(sourceEntry.getSpriteFileName(), entry.getSpriteFileName())) {
                return i;
            }
        }
        return -1;
    }

    private int findBatVoiceGroupIndex(BatVoiceGrp group, String codeName) {
        if (group == null || group.getVoiceList() == null) {
            return -1;
        }
        for (int i = 0; i < group.getVoiceList().size(); i++) {
            BatVoiceGrp.BatVoiceGroup entry = group.getVoiceList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(codeName, entry.getCharacterCodeName())) {
                return i;
            }
        }
        return -1;
    }

    private int findSeGroupIndex(SeGroupGrp group, SeGroupGrp.SeGroupGroup sourceEntry) {
        if (group == null || group.getSeList() == null) {
            return -1;
        }
        for (int i = 0; i < group.getSeList().size(); i++) {
            SeGroupGrp.SeGroupGroup entry = group.getSeList().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(sourceEntry.getSeTypeCodeName(), entry.getSeTypeCodeName())
                    || equalsIgnoreCase(sourceEntry.getSeType(), entry.getSeType())) {
                return i;
            }
        }
        return -1;
    }

    private int findSeItemIndex(SeGroupGrp.SeGroupGroup targetGroup, SeGroupGrp.SeGroupItem sourceItem) {
        if (targetGroup.getSeItems() == null) {
            return -1;
        }
        for (int i = 0; i < targetGroup.getSeItems().size(); i++) {
            SeGroupGrp.SeGroupItem entry = targetGroup.getSeItems().get(i);
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(sourceItem.getSeItemCodeName(), entry.getSeItemCodeName())
                    || equalsIgnoreCase(sourceItem.getSeFileName(), entry.getSeFileName())) {
                return i;
            }
        }
        return -1;
    }

    private MekaGroupGrp.MekaGroup copyMekaGroup(MekaGroupGrp.MekaGroup sourceEntry) {
        MekaGroupGrp.MekaGroup copied = new MekaGroupGrp.MekaGroup();
        copied.setExistFlag(1);
        copied.setMekaName(sourceEntry.getMekaName());
        copied.setMekaCodeName(sourceEntry.getMekaCodeName());
        return copied;
    }

    private WazaGroupGrp.WazaGroupEntry copyWazaGroup(WazaGroupGrp.WazaGroupEntry sourceEntry) {
        WazaGroupGrp.WazaGroupEntry copied = new WazaGroupGrp.WazaGroupEntry();
        copied.setExistFlag(1);
        copied.setWazaName(sourceEntry.getWazaName());
        copied.setWazaCodeName(sourceEntry.getWazaCodeName());
        copied.setWazaDisplayName(sourceEntry.getWazaDisplayName());
        copied.setParam(sourceEntry.getParam());
        return copied;
    }

    private SpriteGroupGrp.SpriteGroupEntry copySpriteGroup(SpriteGroupGrp.SpriteGroupEntry sourceEntry) {
        SpriteGroupGrp.SpriteGroupEntry copied = new SpriteGroupGrp.SpriteGroupEntry();
        copied.setExistFlag(1);
        copied.setSpriteFileName(sourceEntry.getSpriteFileName());
        copied.setSpriteCodeName(sourceEntry.getSpriteCodeName());
        copied.setParam(sourceEntry.getParam());
        return copied;
    }

    private BatVoiceGrp.BatVoiceGroup copyBatVoiceGroup(BatVoiceGrp.BatVoiceGroup sourceEntry) {
        BatVoiceGrp.BatVoiceGroup copied = new BatVoiceGrp.BatVoiceGroup();
        copied.setExistFlag(1);
        copied.setCharacterName(sourceEntry.getCharacterName());
        copied.setCharacterCodeName(sourceEntry.getCharacterCodeName());

        List<BatVoiceGrp.BatVoice> copiedVoices = new ArrayList<>();
        if (sourceEntry.getVoices() != null) {
            for (BatVoiceGrp.BatVoice voice : sourceEntry.getVoices()) {
                if (voice == null) {
                    copiedVoices.add(null);
                    continue;
                }
                BatVoiceGrp.BatVoice copiedVoice = new BatVoiceGrp.BatVoice();
                copiedVoice.setExistFlag(voice.getExistFlag());
                copiedVoice.setVoice(voice.getVoice());
                copiedVoice.setVoiceCodeName(voice.getVoiceCodeName());
                copiedVoice.setVoiceFileName(voice.getVoiceFileName());
                copiedVoices.add(copiedVoice);
            }
        }
        copied.setVoices(copiedVoices);
        return copied;
    }

    private SeGroupGrp.SeGroupGroup copySeGroupShell(SeGroupGrp.SeGroupGroup sourceEntry) {
        SeGroupGrp.SeGroupGroup copied = new SeGroupGrp.SeGroupGroup();
        copied.setExistFlag(1);
        copied.setSeType(sourceEntry.getSeType());
        copied.setSeTypeCodeName(sourceEntry.getSeTypeCodeName());
        copied.setSeItems(new ArrayList<>());
        return copied;
    }

    private SeGroupGrp.SeGroupItem copySeItem(SeGroupGrp.SeGroupItem sourceItem) {
        SeGroupGrp.SeGroupItem copied = new SeGroupGrp.SeGroupItem();
        copied.setExistFlag(1);
        copied.setSeItemName(sourceItem.getSeItemName());
        copied.setSeItemCodeName(sourceItem.getSeItemCodeName());
        copied.setSeFileName(sourceItem.getSeFileName());
        return copied;
    }

    private void validateInputs(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan
    ) {
        if (request == null) {
            throw new IllegalArgumentException("AkaoGraftRequest 不能为空");
        }
        if (jinkiPackage == null) {
            throw new IllegalArgumentException("JinkiPackageBundle 不能为空");
        }
        if (bsdxBaseline == null) {
            throw new IllegalArgumentException("BsdxBaselineBundle 不能为空");
        }
        if (importPlan == null) {
            throw new IllegalArgumentException("JinkiImportPlan 不能为空");
        }
    }

    private boolean isExisting(Integer existFlag) {
        return existFlag == null || existFlag != 0;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        return fileName.trim().toLowerCase(Locale.ROOT);
    }

    private record IndexedBatVoiceGroup(int index, BatVoiceGrp.BatVoiceGroup group) {
    }
}
