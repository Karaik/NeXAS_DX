package com.giga.nexas.dto.clarias.waz;

import com.giga.nexas.dto.clarias.Clarias;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.SkillUnit;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Waz extends Clarias {

    public String fileName; // 仅记录用

    private List<Skill> skillList = new ArrayList<>();

    public Waz() {
    }

    public Waz(String fileName) {
        this.fileName = fileName;
    }

    /**
     * waz 的最大单元，存储了一个技能的信息
     */
    @Data
    public static class Skill {
        /**
         * 该技能的阶段数
         */
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

        /**
         * 技能的阶段
         */
        @Data
        public static class SkillPhase {
            private List<SkillUnit> skillUnitCollection = new ArrayList<>();
            private String phaseTail; //diff
        }

        /**
         * 单个技能尾部附加结构
         */
        @Data
        public static class SkillSuffix {
            private Integer int1;
            private Integer int2;
        }

    }

}
