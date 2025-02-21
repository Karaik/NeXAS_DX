package com.giga.nexasdxeditor.dto.bsdx.waz;

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

    private Integer spmSequence;

    private String skillNameKanji;

    private String skillNameEnglish;

    private List<WazPhase> phasesInfo;

    /**
     * 该技能的阶段数
     */
    private Integer phaseQuantity;

    private byte[] data;

    public Waz(Integer spmSequence) {
        this.spmSequence = spmSequence;
        this.phasesInfo = new ArrayList<>();
    }

    /**
     * 技能的阶段
     * 逆向得知，每个阶段信息内，有72个最小单元，每个单元大小为3*4+5*4字节，
     * 其中单元数由读到的第一个整数决定
     */
    @Data
    public static class WazPhase {

        /**
         * 该阶段的动作数
         */
        private Integer actionQuantity;

        /**
         * 该阶段中的各动作使用哪个spm图层，以及帧数关系
         */
        private List<spmCallingInfo> spmCallingInfoList;

        /**
         * 逆向所得，size=72
         */
        private List<wazPhaseDataUnit> wazPhaseDataUnitList;

        private byte[] wazPhaseData;

        /**
         * 如果有，则记录下逆向过程中填入的padding位置
         */
        private Integer paddingOffset;

        public WazPhase () {
            this.spmCallingInfoList = new ArrayList<>();
            this.wazPhaseDataUnitList = new ArrayList<>();
        }

        @Data
        public static class spmCallingInfo {

            /**
             * 第几帧开始调用spm
             */
            private Integer startFrame;

            /**
             * 第几帧结束调用
             */
            private Integer endFrame;

            /**
             * 机体对应spm，但多为FF FF FF FF
             */
            private Integer spmFileSequence;

            /**
             * 第几号动作组
             */
            private Integer actionGroupNumber;

            /**
             * 该动作组的第几个动作
             */
            private Integer actionNumber;

        }

        @Data
        public static class wazPhaseDataUnit {

            private List<Unit1> unit1List;
            private List<Unit2> unit2List;

            public wazPhaseDataUnit () {
                unit1List = new ArrayList<>();
                unit2List = new ArrayList<>();
            }

            @Data
            public static class Unit1 {
                private List<Integer> data;

                public Unit1 () {
                    data = new ArrayList<>();
                }
            }

            @Data
            public static class Unit2 {
                private Integer int1;
                private Integer int2;
                private Integer int3;
                private Integer int4;
                private Integer int5;
            }
        }


    }




}
