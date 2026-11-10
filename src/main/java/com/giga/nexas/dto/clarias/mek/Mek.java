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
    private MekHead mekHead;
    private MekBasicInfo mekBasicInfo;
    private MekPairBlock mekPairBlock;
    private Map<Integer, MekWeaponInfo> mekWeaponInfoMap;
    private List<MekAiInfo> mekAiInfoList;
    private Integer aiTrailingZeroByteCount;
    private MekVoiceInfo mekVoiceInfo;
    private MekMaterialBlock mekMaterialBlock;

    public Mek() {
        this.mekHead = new MekHead();
        this.mekBlocks = new MekBlocks();
        this.mekBasicInfo = new MekBasicInfo();
        this.mekPairBlock = new MekPairBlock();
        this.mekWeaponInfoMap = new LinkedHashMap<>();
        this.mekAiInfoList = new ArrayList<>();
        this.aiTrailingZeroByteCount = 0;
        this.mekVoiceInfo = new MekVoiceInfo();
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
        private Integer voiceInfoBlockSize;
        private Integer materialBlockSize;

        public void calculateBlockSizes(MekHead mekHead) {
            this.bodyInfoBlockSize = mekHead.getSequence2() - mekHead.getSequence1();
            this.pairInfoBlockSize = mekHead.getSequence3() - mekHead.getSequence2();
            this.weaponInfoBlockSize = mekHead.getSequence4() - mekHead.getSequence3();
            this.aiInfoBlockSize = mekHead.getSequence5() - mekHead.getSequence4();
            this.voiceInfoBlockSize = mekHead.getSequence6() - mekHead.getSequence5();
        }
    }

    @Data
    public static class MekBasicInfo {
        private String stringField1;
        private String stringField2;
        private String stringField3;
        private String stringField4;
        private String stringField5;
        private Integer intField1;
        private Integer intField2;
        private Integer intField3;
        private Integer intField4;
        private Integer intField5;
        private Integer intField6;
        private Integer intField7;
        private Integer intField8;
        private Integer intField9;
        private Integer intField10;
        private Integer intField11;
        private Integer intField12;
        private Byte byteField1;
        private Integer intField13;
        private Integer intField14;
        private Integer intField15;
        private Integer intField16;
        private Integer intField17;
        private Integer intField18;
    }

    @Data
    public static class MekPairBlock {
        private List<Pair> unkPair = new ArrayList<>();

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
    public static class MekVoiceInfo {
        private Integer version;
        private List<Emotion> emotions = new ArrayList<>();
        private List<VoiceSlot> voiceSlots = new ArrayList<>();
        private List<List<List<Entry>>> table = new ArrayList<>();
        public Integer builtinEmotionCount = 0;

        @Data
        public static class Emotion {
            private String name;
            private String token;
        }

        @Data
        public static class VoiceSlot {
            private String name;
            private String token;
        }

        @Data
        public static class Entry {
            private Integer voiceType;
            private Integer groupId;
            private Integer weight;
        }
    }

    @Data
    public static class MekMaterialBlock {
        private Integer extraRegularCount;
        public Integer regularCount;
        private List<PluginEntry> entries = new ArrayList<>();
        public List<PluginEntry> regularEntries = new ArrayList<>();
        public List<PluginEntry> trailingEntries = new ArrayList<>();

        @Data
        public static class PluginEntry {
            public Integer offset;
            public Integer length;
            private List<int[]> spriteGroups = new ArrayList<>();
            private List<int[]> seGroups = new ArrayList<>();
            private List<int[]> voiceGroups = new ArrayList<>();
        }
    }
}
