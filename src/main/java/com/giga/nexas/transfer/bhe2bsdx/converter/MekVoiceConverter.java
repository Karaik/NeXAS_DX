package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.ArrayList;
import java.util.List;

/**
 * mek 语音表迁移：
 * 深拷贝 emotion/slot/table，保留版本号，避免打乱语音表结构。
 */
public class MekVoiceConverter {

    // 是否清空语音表（设为 false 以保留语音映射）
    private static final boolean CLEAR_VOICE_TABLES = false;

    public Mek.MekVoiceInfo convert(com.giga.nexas.dto.bhe.mek.Mek.MekVoiceInfo src) {
        Mek.MekVoiceInfo dst = new Mek.MekVoiceInfo();
        if (src == null) {
            dst.setVersion(1);
            dst.builtinEmotionCount = 0;
            return dst;
        }

        // 版本号保持一致，不强行覆盖
        Integer version = src.getVersion();
        dst.setVersion(version != null ? version : 1);
        dst.builtinEmotionCount = CLEAR_VOICE_TABLES ? 0 : src.builtinEmotionCount;

        if (CLEAR_VOICE_TABLES) {
            dst.setEmotions(new ArrayList<>());
            dst.setVoiceSlots(new ArrayList<>());
            dst.setTable(new ArrayList<>());
            return dst;
        }

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
