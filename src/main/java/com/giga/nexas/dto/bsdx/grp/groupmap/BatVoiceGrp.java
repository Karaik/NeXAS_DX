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

    public List<BatVoiceTypeGroup> voiceTypeList = new ArrayList<>();
    public List<BatVoiceGroup> voiceList = new ArrayList<>();

    @Data
    public static class BatVoiceTypeGroup {
        public String voiceType;
        public String voiceTypeCodeName;
    }

    @Data
    public static class BatVoiceGroup {
        public String characterName;
        public String characterCodeName;
        public List<BatVoice> voices = new ArrayList<>();
    }

    @Data
    public static class BatVoice {
        public String voice;
        public String voiceCodeName;
        public String voiceFileName;
    }

}
