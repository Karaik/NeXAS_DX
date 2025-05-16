package com.giga.nexas.dto.bsdx.grp.groupmap;

import com.giga.nexas.dto.bsdx.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description BatVoice
 */
@Data
public class BatVoiceGrp extends Grp {

    private List<BatVoiceTypeGroup> voiceTypeList = new ArrayList<>();
    private List<BatVoiceGroup> voiceList = new ArrayList<>();

    @Data
    public static class BatVoiceTypeGroup {
        private String voiceType;
        private String voiceTypeCodeName;
    }

    @Data
    public static class BatVoiceGroup {
        public Integer existFlag; // 仅记录用
        private String characterName;
        private String characterCodeName;
        private List<BatVoice> voices = new ArrayList<>();
    }

    @Data
    public static class BatVoice {
        public Integer existFlag; // 仅记录用
        private String voice;
        private String voiceCodeName;
        private String voiceFileName;
    }

}
