package com.giga.nexas.bhe2bsdx.steps;

import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.ArrayList;
import java.util.List;

/**
 * mek 语音表迁移：
 * 深拷贝 emotion/slot/table，保留版本号，避免打乱语音表结构。
 */
public class MekVoiceConverter {

    public Mek.MekVoiceInfo convert(com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo src) {
        Mek.MekVoiceInfo dst = new Mek.MekVoiceInfo();
        if (src == null) {
            return dst;
        }

        // 版本号保持一致，不强行覆盖
        dst.setVersion(src.getVersion());
        dst.builtinEmotionCount = src.builtinEmotionCount;

        List<Mek.MekVoiceInfo.Emotion> emotions = new ArrayList<>();
        if (src.getEmotions() != null) {
            for (com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Emotion emotion : src.getEmotions()) {
                Mek.MekVoiceInfo.Emotion dstEmotion = new Mek.MekVoiceInfo.Emotion();
                dstEmotion.setName(emotion.getName());
                dstEmotion.setToken(emotion.getToken());
                emotions.add(dstEmotion);
            }
        }
        dst.setEmotions(emotions);

        List<Mek.MekVoiceInfo.VoiceSlot> slots = new ArrayList<>();
        if (src.getVoiceSlots() != null) {
            for (com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.VoiceSlot slot : src.getVoiceSlots()) {
                Mek.MekVoiceInfo.VoiceSlot dstSlot = new Mek.MekVoiceInfo.VoiceSlot();
                dstSlot.setName(slot.getName());
                dstSlot.setToken(slot.getToken());
                slots.add(dstSlot);
            }
        }
        dst.setVoiceSlots(slots);

        List<List<List<Mek.MekVoiceInfo.Entry>>> table = new ArrayList<>();
        if (src.getTable() != null) {
            for (List<List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry>> row : src.getTable()) {
                List<List<Mek.MekVoiceInfo.Entry>> dstRow = new ArrayList<>();
                for (List<com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry> cell : row) {
                    List<Mek.MekVoiceInfo.Entry> dstCell = new ArrayList<>();
                    for (com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo.Entry entry : cell) {
                        Mek.MekVoiceInfo.Entry dstEntry = new Mek.MekVoiceInfo.Entry();
                        dstEntry.setVoiceType(entry.getVoiceType());
                        dstEntry.setGroupId(entry.getGroupId());
                        dstEntry.setWeight(entry.getWeight());
                        dstCell.add(dstEntry);
                    }
                    dstRow.add(dstCell);
                }
                table.add(dstRow);
            }
        }
        dst.setTable(table);

        return dst;
    }
}
