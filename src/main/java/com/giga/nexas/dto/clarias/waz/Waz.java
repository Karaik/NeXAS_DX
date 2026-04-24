package com.giga.nexas.dto.clarias.waz;

import com.giga.nexas.dto.clarias.Clarias;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.SkillUnit;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Waz extends Clarias {

    public String fileName;
    public byte[] sourceBytes;
    private List<Skill> skillList = new ArrayList<>();

    public Waz() {
    }

    public Waz(String fileName) {
        this.fileName = fileName;
    }

    @Data
    public static class Skill {
        public Integer phaseQuantity;
        private String skillNameJapanese;
        private String skillNameEnglish;
        private List<SkillPhase> phasesInfo = new ArrayList<>();
        private List<SkillSuffix> skillSuffixList = new ArrayList<>();

        public boolean isEmpty() {
            return phaseQuantity == null
                    && skillNameJapanese == null
                    && skillNameEnglish == null
                    && phasesInfo.isEmpty()
                    && skillSuffixList.isEmpty();
        }

        @Data
        public static class SkillPhase {
            private List<SkillUnit> skillUnitCollection = new ArrayList<>();
            private String phaseTail;
        }

        @Data
        public static class SkillSuffix {
            private Integer int1;
            private Integer int2;
        }
    }
}
