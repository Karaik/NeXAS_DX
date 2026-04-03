package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.GrpAppendPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.util.ArrayList;
import java.util.List;

/**
 * 负责把 AKAO 相关顶层条目追加进 BSDX grp 容器。
 *
 * <p>当前策略：</p>
 * <ul>
 *     <li>如果 BSDX 已经存在同 codeName 的目标条目，直接复用现有索引</li>
 *     <li>如果 BSDX 不存在该条目，则直接尾插到列表末尾</li>
 *     <li>不再占用 {@code existFlag=0} 的空槽</li>
 * </ul>
 */
public class AppendGrpEntriesStep {

    public GrpAppendPlan appendAkaoBranch(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan
    ) {
        validateInputs(request, jinkiPackage, bsdxBaseline);

        GrpAppendPlan plan = new GrpAppendPlan();

        // 1. 从 JINKI 的 MekaGroup 中取出 AKAO 源条目。
        MekaGroupGrp.MekaGroup sourceMeka = findRequiredMekaGroup(
                jinkiPackage.getMekaGroupGrp(),
                request.getMekaCodeName()
        );

        // 2. 把 AKAO meka 注册项写入 BSDX 的 MekaGroup，拿到最终目标索引。
        plan.setMekaGroupIndex(upsertMekaGroup(bsdxBaseline.getMekaGroupGrp(), sourceMeka));

        // 3. 从 JINKI 的 WazaGroup 中取出 AKAO 源条目。
        WazaGroupGrp.WazaGroupEntry sourceWaza = findRequiredWazaGroup(
                jinkiPackage.getWazaGroupGrp(),
                request.getWazCodeName()
        );

        // 4. 把 AKAO waza 注册项写入 BSDX 的 WazaGroup，拿到最终目标索引。
        plan.setWazaGroupIndex(upsertWazaGroup(bsdxBaseline.getWazaGroupGrp(), sourceWaza));

        // 5. 从 JINKI 的 SpriteGroup 中取出 0001 -> moribito_2.spm 这条主 sprite 源条目。
        SpriteGroupGrp.SpriteGroupEntry sourceSprite = findRequiredSpriteGroup(
                jinkiPackage.getSpriteGroupGrp(),
                request.getSpriteCodeName(),
                request.getSpriteFileName()
        );

        // 6. 把主 sprite 条目写入 BSDX 的 SpriteGroup，拿到最终目标索引。
        plan.setSpriteGroupIndex(upsertSpriteGroup(bsdxBaseline.getSpriteGroupGrp(), sourceSprite));

        // 7. 从 JINKI 的 BatVoice 中取出 AKAO 语音组。
        BatVoiceGrp.BatVoiceGroup sourceBatVoice = findRequiredBatVoiceGroup(
                jinkiPackage.getBatVoiceGrp(),
                request.getMekaCodeName()
        );

        // 8. 把 AKAO 语音组写入 BSDX 的 BatVoice，拿到最终目标索引。
        plan.setBatVoiceGroupIndex(upsertBatVoiceGroup(bsdxBaseline.getBatVoiceGrp(), sourceBatVoice));

        return plan;
    }

    private void validateInputs(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline
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

    private BatVoiceGrp.BatVoiceGroup findRequiredBatVoiceGroup(BatVoiceGrp group, String codeName) {
        if (group == null || group.getVoiceList() == null) {
            throw new IllegalStateException("JINKI BatVoice 不存在");
        }
        for (BatVoiceGrp.BatVoiceGroup entry : group.getVoiceList()) {
            if (!isExisting(entry == null ? null : entry.getExistFlag())) {
                continue;
            }
            if (equalsIgnoreCase(codeName, entry.getCharacterCodeName())) {
                return entry;
            }
        }
        throw new IllegalStateException("JINKI BatVoice 中找不到目标条目: " + codeName);
    }

    private int upsertMekaGroup(MekaGroupGrp targetGroup, MekaGroupGrp.MekaGroup sourceEntry) {
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
                    || equalsIgnoreCase(sourceEntry.getWazaName(), entry.getWazaName())
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

    private boolean isExisting(Integer existFlag) {
        return existFlag == null || existFlag != 0;
    }

    private boolean equalsIgnoreCase(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return a.trim().equalsIgnoreCase(b.trim());
    }
}
