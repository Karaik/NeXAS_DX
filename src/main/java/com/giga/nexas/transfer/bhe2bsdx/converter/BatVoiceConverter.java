package com.giga.nexas.transfer.bhe2bsdx.converter;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;

import java.util.ArrayList;
import java.util.List;

/**
 * BatVoice 深拷贝：BHE -> BSDX。
 * 处理 BHE 独有字段（unk0）并保证类型一致。
 */
public class BatVoiceConverter {

    public BatVoiceGrp.BatVoiceGroup convert(
            com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup bheBatVoiceGrp
    ) {
        if (bheBatVoiceGrp == null) {
            return new BatVoiceGrp.BatVoiceGroup();
        }

        BatVoiceGrp.BatVoiceGroup dst = new BatVoiceGrp.BatVoiceGroup();
        dst.setExistFlag(existFlagOrDefault(bheBatVoiceGrp.getExistFlag()));
        dst.setCharacterName(bheBatVoiceGrp.getCharacterName());
        dst.setCharacterCodeName(bheBatVoiceGrp.getCharacterCodeName());

        List<BatVoiceGrp.BatVoice> voices = new ArrayList<>();
        if (bheBatVoiceGrp.getVoices() != null) {
            for (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoice voice : bheBatVoiceGrp.getVoices()) {
                BatVoiceGrp.BatVoice dstVoice = new BatVoiceGrp.BatVoice();
                dstVoice.setExistFlag(existFlagOrDefault(voice.getExistFlag()));
                dstVoice.setVoice(voice.getVoice());
                dstVoice.setVoiceCodeName(voice.getVoiceCodeName());
                dstVoice.setVoiceFileName(voice.getVoiceFileName());
                voices.add(dstVoice);
            }
        }
        dst.setVoices(voices);

        return dst;
    }

    private int existFlagOrDefault(Integer existFlag) {
        return existFlag == null ? 1 : existFlag;
    }
}
