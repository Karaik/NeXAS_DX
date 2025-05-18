package com.giga.nexas.dto.bhe.waz;

import com.giga.nexas.dto.bhe.Bhe;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * @Description Waz
 */
@Data
public class Waz extends Bhe {

    public String fileName; // 仅记录用

    private List<Skill> skillList = new ArrayList<>();

    public Waz() {
    }
    public Waz(String fileName) {
        this.fileName = fileName;
    }

    @Data
    public static class Skill {

        public Integer phaseQuantity; // 仅记录用

        private String skillNameJapanese;

        private String skillNameEnglish;

        private List<SkillPhase> phasesInfo = new ArrayList<>();

        private List<SkillSuffix> skillSuffixList = new ArrayList<>();

        public boolean isEmpty() {
            return phaseQuantity == null &&
                    skillNameJapanese == null &&
                    skillNameEnglish == null &&
                    phasesInfo.isEmpty() &&
                    skillSuffixList == null;
        }

        @Data
        public static class SkillPhase {
            private List<SkillUnit> skillUnitCollection = new ArrayList<>();
        }

        @Data
        public static class SkillSuffix {
            private Integer int1;
            private Integer int2;
        }

    }

}
