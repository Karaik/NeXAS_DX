package com.giga.nexasdxeditor.dto.bsdx.waz;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.SkillUnit;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description Waz
 */
@Data
public class Waz {

    public String fileName; // 仅记录用

    private List<Skill> skillList = new ArrayList<>();

    public Waz() {
    }
    public Waz(String fileName) {
        this.fileName = fileName;
        this.skillList = new ArrayList<>();
    }

    /**
     * waz的最大单元，存储了一个技能的信息
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

        private SkillSuffix skillSuffix;

        public boolean isEmpty() {
            return phaseQuantity == null &&
                    skillNameJapanese == null &&
                    skillNameEnglish == null &&
                    phasesInfo.isEmpty() &&
                    skillSuffix == null;
        }

        /**
         * 技能的阶段
         * 逆向得知，每个阶段信息内，有72个最小单元，
         * 每个单元由两个部分组成，每个单元结构大体如下
         * count1 & WAZA_BLOCK_TYPE_ENTRIES[i]*count1
         * &
         * count2 & WazInfoUnknown*count2
         */
        @Data
        public static class SkillPhase {

            /**
             * 逆向所得，size=72
             */
            private List<SkillUnit> skillUnitCollection = new ArrayList<>();

        }

        /**
         * 单个技能，也就是一个wazSkill最后会有一个这样的结构
         * 大体如下
         * count & (int1&int2)*count
         */
        @Data
        public static class SkillSuffix {
            private Integer count;
            private Integer int1;
            private Integer int2;
        }

    }

}
