package com.giga.nexas.dto.clarias.mek;

import com.giga.nexas.dto.clarias.Clarias;
import com.giga.nexas.dto.clarias.mek.mekcpu.CCpuEvent;
import lombok.Data;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class Mek extends Clarias {

    private String fileName;
    private MekBlocks mekBlocks;
    private byte[] rawBlock1 = new byte[0];
    private byte[] rawBlock2 = new byte[0];
    private byte[] rawBlock3 = new byte[0];
    private byte[] rawBlock4 = new byte[0];
    private byte[] rawBlock5 = new byte[0];
    private MekHead mekHead;
    private MekBasicInfo mekBasicInfo;
    private MekPairBlock mekPairBlock;
    private Map<Integer, MekWeaponInfo> mekWeaponInfoMap;
    private List<MekAiInfo> mekAiInfoList;
    private List<MekSimpleBlockEntry> mekSimpleBlockEntries;
    private MekMaterialBlock mekMaterialBlock;

    public Mek() {
        this.mekHead = new MekHead();
        this.mekBlocks = new MekBlocks();
        this.mekBasicInfo = new MekBasicInfo();
        this.mekPairBlock = new MekPairBlock();
        this.mekWeaponInfoMap = new LinkedHashMap<>();
        this.mekAiInfoList = new ArrayList<>();
        this.mekSimpleBlockEntries = new ArrayList<>();
        this.mekMaterialBlock = new MekMaterialBlock();
    }

    @Data
    public static class MekHead {
        private Integer sequence1;
        private Integer sequence2;
        private Integer sequence3;
        private Integer sequence4;
        private Integer sequence5;
        private Integer sequence6;
    }

    @Data
    public static class MekBlocks {
        private Integer bodyInfoBlockSize;
        private Integer pairInfoBlockSize;
        private Integer weaponInfoBlockSize;
        private Integer aiInfoBlockSize;
        private Integer simpleInfoBlockSize;
        private Integer materialBlockSize;

        public void calculateBlockSizes(MekHead mekHead) {
            this.bodyInfoBlockSize = mekHead.getSequence2() - mekHead.getSequence1();
            this.pairInfoBlockSize = mekHead.getSequence3() - mekHead.getSequence2();
            this.weaponInfoBlockSize = mekHead.getSequence4() - mekHead.getSequence3();
            this.aiInfoBlockSize = mekHead.getSequence5() - mekHead.getSequence4();
            this.simpleInfoBlockSize = mekHead.getSequence6() - mekHead.getSequence5();
        }
    }

    @Data
    public static class MekBasicInfo {
        private String stringField1;
        private String stringField2;
        private String stringField3;
        private String stringField4;
        private String stringField5;
        private List<Integer> leadingInts = new ArrayList<>();
        private Byte flagByte;
        private List<Integer> trailingInts = new ArrayList<>();
    }

    @Data
    public static class MekPairBlock {
        private List<Pair> unkPair = new ArrayList<>();
        private byte[] rawBytes = new byte[0];

        @Data
        public static class Pair {
            private Integer int1;
            private Integer int2;
        }
    }

    @Data
    public static class MekWeaponInfo {
        public int offset;
        private Integer enabled = 1;
        private String stringField1;
        private String stringField2;
        private String stringField3;
        private List<Integer> leadingInts = new ArrayList<>();
        private Byte flagByte;
        private List<Integer> trailingInts = new ArrayList<>();
        private Integer variantCount = 0;
        private List<Variant> variants = new ArrayList<>();

        @Data
        public static class Variant {
            private Integer int1;
            private Integer int2;
            private Integer int3;
            private Integer int4;
            private Integer int5;
            private Integer int6;
            private Integer int7;
        }
    }

    @Data
    public static class MekAiInfo {
        private String stringField1;
        private String stringField2;
        private List<CCpuEvent> cpuEventList = new ArrayList<>();
    }

    @Data
    public static class MekSimpleBlockEntry {
        private Integer int1;
        private Integer int2;
        private Integer int3;
    }

    @Data
    public static class MekMaterialBlock {
        private List<MaterialSnapshot> snapshots = new ArrayList<>();
        private byte[] rawBytes = new byte[0];

        @Data
        public static class MaterialSnapshot {
            private List<int[]> groupSegment1 = new ArrayList<>();
            private List<int[]> groupSegment2 = new ArrayList<>();
            private List<int[]> groupSegment3 = new ArrayList<>();
        }
    }
}
