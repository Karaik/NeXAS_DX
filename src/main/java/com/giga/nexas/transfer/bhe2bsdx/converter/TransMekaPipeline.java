package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaResult;

import java.util.Map;
import java.util.Set;

/**
 * 迁移流程编排器：按步骤执行并产出结果。
 *
 * 流程（文字流程图）：
 * 输入(BHE: mek/waz/spm/grp/batvoice)
 * -> Step1: BatVoice 深拷贝并写入目标槽位（默认 Nanoha）
 * -> Step2: grp 对齐（目标槽位替换或 upsert）并生成索引
 * -> Step3: spritegroup 索引映射(BHE index -> BSDX index)
 * -> Step4: 资源转换(mek/waz/spm，含 hitbox)
 * -> Step5: 回写 MekBasicInfo 的 waz/spm 索引
 * -> Step6: UI SPM 挂接（替换 Nanoha 槽位）
 */
public class TransMekaPipeline {

    private final BatVoiceConverter batVoiceConverter = new BatVoiceConverter();
    private final GrpRegistryUpdater grpRegistryUpdater = new GrpRegistryUpdater();
    private final SpriteGroupIndexMapper spriteGroupIndexMapper = new SpriteGroupIndexMapper();
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

        // Step1: batvoice 深拷贝并写入目标槽位（默认 Nanoha）
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

        // Step2: grp 对齐，返回最终序号（索引用于 mek/waz/spm 对齐）
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

        // Step3: spritegroup 映射（BHE 索引 -> BSDX 索引）
        // 先从 BHE mek 的 materialBlock 抽取需要的 spritegroup 索引，再按 BHE grp 重建到 BSDX
        Map<Integer, Integer> spriteIndexMap = new java.util.HashMap<>();
        Set<Integer> requiredSpriteIndices =
                spriteGroupIndexMapper.collectRequiredIndicesFromMek(request.getBheMek());
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
        } else {
            spriteIndexMap = spriteGroupIndexMapper.buildMap(
                    request.getBheSpriteGroup(),
                    request.getBsdxSpriteGroup(),
                    requiredSpriteIndices
            );
        }
        result.setSpriteIndexMap(spriteIndexMap);

        // Step4: 转换核心资源（mek/waz/spm）
        int voiceGroupCount = request.getBsdxBatVoice() != null
                ? request.getBsdxBatVoice().getVoiceList().size()
                : 0;
        result.setBsdxMeka(mekConverter.convert(request.getBheMek(), spriteIndexMap, voiceGroupCount));
        WazSequenceSanitizer sanitizer = WazSequenceSanitizer.fromBsdxWaz(request.getBsdxWazRegistry());
        result.setBsdxWaz(wazConverter.convert(request.getBheWaz(), sanitizer, spriteIndexMap));
        result.setBsdxSpm(spmConverter.convert(request.getBheSpm()));
        result.setBsdxCSpm(spmConverter.convert(request.getBheCSpm()));
        result.setBsdxSSpm(spmConverter.convert(request.getBheSSpm()));
        result.setBsdxGSpm(spmConverter.convert(request.getBheGSpm()));
        result.setBsdxMSpm(spmConverter.convert(request.getBheMSpm()));

        // Step5: 回写 mek 内部的 waz/spm 索引
        alignMekIndex(result.getBsdxMeka(), result.getWazaGroupIndex(), result.getSpriteGroupIndex());

        // Step6: UI 资源表挂接（用 tsukuyomi 替换 nanoha 槽位，便于测试）
        UiSpmReplacer.UiSpmReplaceResult uiReplaceResult = uiSpmReplacer.replaceNanohaWithTsukuyomi(
                request.getMekaPilotSpm(),
                request.getSelectMekaMenuMekaSpm(),
                result.getBsdxMSpm(),
                result.getBsdxSSpm(),
                request.getSelectMekaMenuDat(),
                request.getBsdxMekaGroup(),
                result.getBsdxMeka(),
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
