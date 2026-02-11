package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaResult;

import java.util.Map;
import java.util.Set;

/**
 * Migration pipeline orchestrator: executes each step and returns converted results.
 *
 * Flow:
 * input(BHE: mek/waz/spm/grp/batvoice)
 * -> Step1: copy BatVoice into target slot (default Nanoha slot)
 * -> Step2: align grp registries (replace target slot or upsert) and build indices
 * -> Step3: build spritegroup index map (BHE index -> BSDX index)
 * -> Step4: convert core resources (mek/waz/spm, including hitbox data)
 * -> Step5: write back waz/spm indices into MekBasicInfo
 * -> Step6: attach UI SPM replacement for target slot
 */
public class TransMekaPipeline {

    private final BatVoiceConverter batVoiceConverter = new BatVoiceConverter();
    private final GrpRegistryUpdater grpRegistryUpdater = new GrpRegistryUpdater();
    private final SpriteGroupIndexMapper spriteGroupIndexMapper = new SpriteGroupIndexMapper();
    private final SeGroupIndexMapper seGroupIndexMapper = new SeGroupIndexMapper();
    private final MekConverter mekConverter = new MekConverter();
    private final WazConverter wazConverter = new WazConverter();
    private final SpmConverter spmConverter = new SpmConverter();
    private final UiSpmReplacer uiSpmReplacer = new UiSpmReplacer();

    public TransMekaResult execute(TransMekaRequest request) {
        TransMekaResult result = new TransMekaResult();
        if (request == null) {
            return result;
        }

        String targetCode = normalizeCode(request.getTargetCodeName(), "NANOHA");
        boolean keepTargetKey = request.isKeepTargetKey();
        boolean useTargetSlot = request.getTargetBsdxMek() != null && targetCode != null;

        // Step1: copy batvoice into target slot (default Nanoha)
        if (request.getBheBatVoiceGroup() != null && request.getBsdxBatVoice() != null) {
            if (useTargetSlot) {
                int batVoiceIndex = findBatVoiceGroupIndex(request.getBsdxBatVoice(), targetCode);
                if (batVoiceIndex >= 0) {
                    BatVoiceGrp.BatVoiceGroup converted = batVoiceConverter.convert(request.getBheBatVoiceGroup());
                    BatVoiceGrp.BatVoiceGroup replaced = mergeBatVoiceGroup(
                            request.getBsdxBatVoice().getVoiceList().get(batVoiceIndex),
                            converted,
                            keepTargetKey
                    );
                    request.getBsdxBatVoice().getVoiceList().set(batVoiceIndex, replaced);
                    result.setBsdxBatVoiceGroup(replaced);
                    result.setBatVoiceIndex(batVoiceIndex);
                }
            } else {
                BatVoiceGrp.BatVoiceGroup batVoice = batVoiceConverter.convert(request.getBheBatVoiceGroup());
                request.getBsdxBatVoice().getVoiceList().add(batVoice);
                result.setBsdxBatVoiceGroup(batVoice);
                result.setBatVoiceIndex(request.getBsdxBatVoice().getVoiceList().size() - 1);
            }
        }

        // Step2: align grp and return final sequence indices for mek/waz/spm
        if (useTargetSlot) {
            int mekaIndex = grpRegistryUpdater.findMekaGroupIndexByCode(request.getBsdxMekaGroup(), targetCode);
            if (mekaIndex >= 0) {
                grpRegistryUpdater.replaceMekaGroupAtIndex(
                        request.getBsdxMekaGroup(),
                        mekaIndex,
                        request.getBheMekaGroup(),
                        keepTargetKey
                );
            }
            int wazaIndex = resolveWazaIndex(request, targetCode);
            if (wazaIndex >= 0) {
                grpRegistryUpdater.replaceWazaGroupAtIndex(
                        request.getBsdxWazaGroup(),
                        wazaIndex,
                        request.getBheWazaGroup(),
                        keepTargetKey
                );
            }
            int spriteIndex = resolveSpriteIndex(request, targetCode);
            if (spriteIndex >= 0) {
                grpRegistryUpdater.replaceSpriteGroupAtIndex(
                        request.getBsdxSpriteGroup(),
                        spriteIndex,
                        request.getBheSpriteGroupEntry(),
                        keepTargetKey
                );
            }
            result.setMekaGroupIndex(mekaIndex);
            result.setWazaGroupIndex(wazaIndex);
            result.setSpriteGroupIndex(spriteIndex);
        } else {
            result.setMekaGroupIndex(
                    grpRegistryUpdater.upsertMekaGroup(request.getBsdxMekaGroup(), request.getBheMekaGroup())
            );
            result.setWazaGroupIndex(
                    grpRegistryUpdater.upsertWazaGroup(request.getBsdxWazaGroup(), request.getBheWazaGroup())
            );
            result.setSpriteGroupIndex(
                    grpRegistryUpdater.upsertSpriteGroup(request.getBsdxSpriteGroup(), request.getBheSpriteGroupEntry())
            );
        }

        // Step3: spritegroup mapping (BHE index -> BSDX index)
        // Collect required sprite indices from BHE mek/waz, then map into BSDX grp.
        Map<Integer, Integer> spriteIndexMap = new java.util.HashMap<>();
        Set<Integer> requiredSpriteIndices = new java.util.HashSet<>();
        requiredSpriteIndices.addAll(spriteGroupIndexMapper.collectRequiredIndicesFromMek(request.getBheMek()));
        requiredSpriteIndices.addAll(spriteGroupIndexMapper.collectRequiredIndicesFromWaz(request.getBheWaz()));
        if (useTargetSlot) {
            spriteIndexMap = spriteGroupIndexMapper.buildMapByName(
                    request.getBheSpriteGroup(),
                    request.getBsdxSpriteGroup(),
                    requiredSpriteIndices
            );
            int bheSpriteIndex = spriteGroupIndexMapper.findBheSpriteIndex(
                    request.getBheSpriteGroup(),
                    request.getBheSpriteGroupEntry()
            );
            if (bheSpriteIndex >= 0 && result.getSpriteGroupIndex() >= 0) {
                spriteIndexMap.put(bheSpriteIndex, result.getSpriteGroupIndex());
            }
            fillMissingSpriteMappings(
                    spriteIndexMap,
                    requiredSpriteIndices,
                    result.getSpriteGroupIndex(),
                    request.getBsdxSpriteGroup()
            );
        } else {
            spriteIndexMap = spriteGroupIndexMapper.buildMap(
                    request.getBheSpriteGroup(),
                    request.getBsdxSpriteGroup(),
                    requiredSpriteIndices
            );
        }
        result.setSpriteIndexMap(spriteIndexMap);
        SeGroupIndexMapper.SeGroupMap seGroupMap = seGroupIndexMapper.build(
                request.getBheSeGroup(),
                request.getBsdxSeGroup(),
                request.getSeGroupAppendIndex()
        );

        // Step4: convert core resources (mek/waz/spm)
        int voiceGroupCount = request.getBsdxBatVoice() != null
                ? request.getBsdxBatVoice().getVoiceList().size()
                : 0;
        result.setBsdxMeka(mekConverter.convert(request.getBheMek(), spriteIndexMap, voiceGroupCount));
        WazSequenceSanitizer sanitizer = WazSequenceSanitizer.fromBsdxWaz(request.getBsdxWazRegistry());
        result.setBsdxWaz(wazConverter.convert(request.getBheWaz(), sanitizer, spriteIndexMap, seGroupMap));
        result.setBsdxSpm(spmConverter.convert(request.getBheSpm()));
        result.setBsdxCSpm(spmConverter.convert(request.getBheCSpm()));
        result.setBsdxSSpm(spmConverter.convert(request.getBheSSpm()));
        result.setBsdxGSpm(spmConverter.convert(request.getBheGSpm()));
        result.setBsdxMSpm(spmConverter.convert(request.getBheMSpm()));

        // Step5: write back waz/spm indices into mek basic info
        alignMekIndex(result.getBsdxMeka(), result.getWazaGroupIndex(), result.getSpriteGroupIndex());

        // Step6: attach UI resources by replacing the Nanoha slot with transfer output
        UiSpmReplacer.UiSpmReplaceResult uiReplaceResult = uiSpmReplacer.replaceTargetSlotUiSpm(
                request.getMekaPilotSpm(),
                request.getSelectMekaMenuMekaSpm(),
                result.getBsdxMSpm(),
                result.getBsdxSSpm(),
                request.getSelectMekaMenuDat(),
                request.getBsdxMekaGroup(),
                result.getBsdxMeka(),
                request.getTargetBsdxMek(),
                targetCode,
                keepTargetKey
        );
        if (uiReplaceResult.isMekaPilotReplaced()) {
            result.setBsdxMekaPilotSpm(request.getMekaPilotSpm());
        }
        if (uiReplaceResult.isSelectMekaMenuReplaced()) {
            result.setBsdxSelectMekaMenuMekaSpm(request.getSelectMekaMenuMekaSpm());
        }

        return result;
    }

    private void alignMekIndex(Mek mek, int wazaIndex, int spriteIndex) {
        if (mek == null || mek.getMekBasicInfo() == null) {
            return;
        }
        if (wazaIndex >= 0) {
            mek.getMekBasicInfo().setWazFileSequence(wazaIndex);
        }
        if (spriteIndex >= 0) {
            mek.getMekBasicInfo().setSpmFileSequence(spriteIndex);
        }
    }

    private String normalizeCode(String code, String fallback) {
        if (code == null || code.isBlank()) {
            return fallback;
        }
        return code.trim().toUpperCase();
    }

    private int resolveWazaIndex(TransMekaRequest request, String targetCode) {
        if (request.getTargetBsdxMek() != null && request.getTargetBsdxMek().getMekBasicInfo() != null) {
            Integer index = request.getTargetBsdxMek().getMekBasicInfo().getWazFileSequence();
            if (index != null) {
                return index;
            }
        }
        return grpRegistryUpdater.findWazaGroupIndexByCode(request.getBsdxWazaGroup(), targetCode);
    }

    private int resolveSpriteIndex(TransMekaRequest request, String targetCode) {
        if (request.getTargetBsdxMek() != null && request.getTargetBsdxMek().getMekBasicInfo() != null) {
            Integer index = request.getTargetBsdxMek().getMekBasicInfo().getSpmFileSequence();
            if (index != null) {
                return index;
            }
        }
        return grpRegistryUpdater.findSpriteGroupIndexByCode(request.getBsdxSpriteGroup(), targetCode);
    }

    private int findBatVoiceGroupIndex(BatVoiceGrp grp, String targetCode) {
        if (grp == null || grp.getVoiceList() == null || targetCode == null) {
            return -1;
        }
        for (int i = 0; i < grp.getVoiceList().size(); i++) {
            BatVoiceGrp.BatVoiceGroup group = grp.getVoiceList().get(i);
            if (group == null || group.getExistFlag() == null || group.getExistFlag() == 0) {
                continue;
            }
            String code = group.getCharacterCodeName();
            if (code != null && targetCode.equalsIgnoreCase(code.trim())) {
                return i;
            }
        }
        return -1;
    }

    private void fillMissingSpriteMappings(
            Map<Integer, Integer> spriteIndexMap,
            Set<Integer> requiredSpriteIndices,
            int targetSpriteIndex,
            SpriteGroupGrp bsdxSpriteGroup
    ) {
        if (requiredSpriteIndices == null || requiredSpriteIndices.isEmpty()) {
            return;
        }
        if (spriteIndexMap == null) {
            return;
        }

        for (Integer bheIndex : requiredSpriteIndices) {
            if (bheIndex == null || bheIndex < 0 || spriteIndexMap.containsKey(bheIndex)) {
                continue;
            }
            Integer fallback = resolveFallbackSpriteIndex(bheIndex, targetSpriteIndex, bsdxSpriteGroup);
            if (fallback != null && fallback >= 0) {
                spriteIndexMap.put(bheIndex, fallback);
            }
        }
    }

    private Integer resolveFallbackSpriteIndex(int bheIndex, int targetSpriteIndex, SpriteGroupGrp bsdxSpriteGroup) {
        if (hasValidSpriteEntry(bsdxSpriteGroup, targetSpriteIndex)) {
            return targetSpriteIndex;
        }
        if (hasValidSpriteEntry(bsdxSpriteGroup, bheIndex)) {
            return bheIndex;
        }
        return firstValidSpriteIndex(bsdxSpriteGroup);
    }

    private boolean hasValidSpriteEntry(SpriteGroupGrp bsdxSpriteGroup, int index) {
        if (bsdxSpriteGroup == null || bsdxSpriteGroup.getSpriteList() == null || index < 0) {
            return false;
        }
        if (index >= bsdxSpriteGroup.getSpriteList().size()) {
            return false;
        }
        SpriteGroupGrp.SpriteGroupEntry entry = bsdxSpriteGroup.getSpriteList().get(index);
        return entry != null && entry.getExistFlag() != null && entry.getExistFlag() != 0;
    }

    private Integer firstValidSpriteIndex(SpriteGroupGrp bsdxSpriteGroup) {
        if (bsdxSpriteGroup == null || bsdxSpriteGroup.getSpriteList() == null) {
            return null;
        }
        for (int i = 0; i < bsdxSpriteGroup.getSpriteList().size(); i++) {
            if (hasValidSpriteEntry(bsdxSpriteGroup, i)) {
                return i;
            }
        }
        return null;
    }

    private BatVoiceGrp.BatVoiceGroup mergeBatVoiceGroup(
            BatVoiceGrp.BatVoiceGroup target,
            BatVoiceGrp.BatVoiceGroup source,
            boolean keepTargetKey
    ) {
        if (source == null) {
            source = new BatVoiceGrp.BatVoiceGroup();
        }
        if (keepTargetKey && target != null) {
            source.setExistFlag(target.getExistFlag() != null ? target.getExistFlag() : 1);
            source.setCharacterName(target.getCharacterName());
            source.setCharacterCodeName(target.getCharacterCodeName());
        }
        if (source.getExistFlag() == null) {
            source.setExistFlag(1);
        }
        return source;
    }
}
