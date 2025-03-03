package com.giga.nexasdxeditor.dto.bsdx.waz;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazUnit;
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

    private List<WazBlock> wazBlockList = new ArrayList<>();

    public Waz() {
    }
    public Waz(String fileName) {
        this.fileName = fileName;
        this.wazBlockList = new ArrayList<>();
    }

    @Data
    public static class WazBlock {
        /**
         * 该技能的阶段数
         */
        public Integer phaseQuantity; // 仅记录用

        private String skillNameJapanese;

        private String skillNameEnglish;

        private List<WazPhase> phasesInfo = new ArrayList<>();

        private WazSuffix wazSuffix;

        /**
         * 技能的阶段
         * 逆向得知，每个阶段信息内，有72个最小单元，每个单元大小为3*4+5*4字节，
         * 其中单元数由读到的第一个整数决定
         */
        @Data
        public static class WazPhase {

            /**
             * 逆向所得，size=72
             */
            private List<WazUnit> wazUnitCollection = new ArrayList<>();

        }

        @Data
        public static class WazSuffix {
            private Integer count;
            private Integer int1;
            private Integer int2;
        }

    }

}
