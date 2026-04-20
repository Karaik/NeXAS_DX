package com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.mek;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;


public class BheToBsdxMekConverter {

    private final BheToBsdxMekAiConverter aiConverter = new BheToBsdxMekAiConverter();
    private final BheToBsdxMekMaterialConverter materialConverter = new BheToBsdxMekMaterialConverter();

    public Mek convert(com.giga.nexas.dto.bhe.mek.Mek source) {
        Mek target = new Mek();
        if (source == null) {
            return target;
        }

        target.setFileName(source.getFileName());
        target.setExtensionName(source.getExtensionName());
        target.setMekHead(copyBean(source.getMekHead(), new Mek.MekHead()));
        target.setMekBlocks(copyBean(source.getMekBlocks(), new Mek.MekBlocks()));
        target.setMekBasicInfo(copyBean(source.getMekBasicInfo(), new Mek.MekBasicInfo()));
        target.setMekPairBlock(convertPairBlock(source.getMekPairBlock()));
        target.setMekWeaponInfoMap(convertWeaponInfoMap(source.getMekWeaponInfoMap()));
        target.setMekAiInfoList(aiConverter.convert(source.getMekAiInfoList()));
        target.setMekVoiceInfo(convertVoiceInfo(source.getMekVoiceInfo()));
        target.setMekMaterialBlock(materialConverter.convert(source.getMekMaterialBlock()));
        return target;
    }

    private Mek.MekPairBlock convertPairBlock(com.giga.nexas.dto.bhe.mek.Mek.MekPairBlock source) {
        Mek.MekPairBlock target = new Mek.MekPairBlock();
        if (source == null || source.getUnkPair() == null) {
            return target;
        }

        List<Mek.MekPairBlock.Pair> pairs = new ArrayList<>();
        for (com.giga.nexas.dto.bhe.mek.Mek.MekPairBlock.Pair sourcePair : source.getUnkPair()) {
            Mek.MekPairBlock.Pair targetPair = new Mek.MekPairBlock.Pair();
            if (sourcePair != null) {
                BeanUtil.copyProperties(sourcePair, targetPair);
            }
            pairs.add(targetPair);
        }
        target.setUnkPair(pairs);
        return target;
    }

    private Map<Integer, Mek.MekWeaponInfo> convertWeaponInfoMap(
            Map<Integer, com.giga.nexas.dto.bhe.mek.Mek.MekWeaponInfo> sourceMap
    ) {
        Map<Integer, Mek.MekWeaponInfo> targetMap = new LinkedHashMap<>();
        if (sourceMap == null) {
            return targetMap;
        }

        for (Map.Entry<Integer, com.giga.nexas.dto.bhe.mek.Mek.MekWeaponInfo> entry : sourceMap.entrySet()) {
            Mek.MekWeaponInfo target = new Mek.MekWeaponInfo();
            if (entry.getValue() != null) {
                BeanUtil.copyProperties(entry.getValue(), target);
            }
            targetMap.put(entry.getKey(), target);
        }
        return targetMap;
    }

    private Mek.MekVoiceInfo convertVoiceInfo(com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo source) {
        Mek.MekVoiceInfo target = new Mek.MekVoiceInfo();
        if (source == null) {
            return target;
        }

        target.setVersion(source.getVersion());
        target.builtinEmotionCount = source.builtinEmotionCount;
        target.setEmotions(convertVoiceEmotions(source.getEmotions()));
        target.setVoiceSlots(convertVoiceSlots(source.getVoiceSlots()));
        target.setTable(convertVoiceTable(source.getTable()));
        return target;
    }

    private List<Mek.MekVoiceInfo.Emotion> convertVoiceEmotions(
            List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Emotion> sourceList
    ) {
        List<Mek.MekVoiceInfo.Emotion> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }

        for (com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Emotion source : sourceList) {
            targetList.add(copyBean(source, new Mek.MekVoiceInfo.Emotion()));
        }
        return targetList;
    }

    private List<Mek.MekVoiceInfo.VoiceSlot> convertVoiceSlots(
            List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.VoiceSlot> sourceList
    ) {
        List<Mek.MekVoiceInfo.VoiceSlot> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }

        for (com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.VoiceSlot source : sourceList) {
            targetList.add(copyBean(source, new Mek.MekVoiceInfo.VoiceSlot()));
        }
        return targetList;
    }

    private List<List<List<Mek.MekVoiceInfo.Entry>>> convertVoiceTable(
            List<List<List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry>>> sourceTable
    ) {
        List<List<List<Mek.MekVoiceInfo.Entry>>> targetTable = new ArrayList<>();
        if (sourceTable == null) {
            return targetTable;
        }

        for (List<List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry>> sourceRow : sourceTable) {
            List<List<Mek.MekVoiceInfo.Entry>> targetRow = new ArrayList<>();
            if (sourceRow != null) {
                for (List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry> sourceCell : sourceRow) {
                    targetRow.add(convertVoiceCell(sourceCell));
                }
            }
            targetTable.add(targetRow);
        }
        return targetTable;
    }

    private List<Mek.MekVoiceInfo.Entry> convertVoiceCell(
            List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry> sourceCell
    ) {
        List<Mek.MekVoiceInfo.Entry> targetCell = new ArrayList<>();
        if (sourceCell == null) {
            return targetCell;
        }

        for (com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry source : sourceCell) {
            targetCell.add(copyBean(source, new Mek.MekVoiceInfo.Entry()));
        }
        return targetCell;
    }

    private <T> T copyBean(Object source, T target) {
        if (source != null) {
            BeanUtil.copyProperties(source, target);
        }
        return target;
    }
}
