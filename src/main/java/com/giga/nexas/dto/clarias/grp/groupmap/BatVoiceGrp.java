package com.giga.nexas.dto.clarias.grp.groupmap;

import com.giga.nexas.dto.clarias.grp.Grp;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 杩欎綅鍚屽(Karaik)
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
        public Integer existFlag; // 浠呰褰曠敤
        private String characterName;
        private String characterCodeName;
        private List<BatVoice> voices = new ArrayList<>();
    }

    @Data
    public static class BatVoice {
        public Integer existFlag; // 浠呰褰曠敤
        private String voice;
        private String voiceCodeName;
        private String voiceFileName;
    }

}

