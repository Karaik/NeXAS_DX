package com.giga.nexasdxeditor.dto.bsdx.mecha.mek;

import lombok.Data;

@Data
public class Mek {

    // 存储各个数据块的字节大小
    private MekBlocks mekBlocks;

    // 区块大小块
    private MekHead mekHead;

    // 机甲信息块
    private MekBasicInfo mekBasicInfo;

    // 武装信息块
    private MekWeaponInfo mekWeaponInfo;

    // ai信息块1
    private MekAi1Info mekAi1Info;

    // ai信息块2
    private MekAi2Info mekAi2Info;

    public Mek() {
        this.mekHead = new MekHead();
        this.mekBlocks = new MekBlocks();
        this.mekBasicInfo = new MekBasicInfo();
        this.mekWeaponInfo = new MekWeaponInfo();
        this.mekAi1Info = new MekAi1Info();
        this.mekAi2Info = new MekAi2Info();
    }

    @Data
    public static class MekHead {

        // 序列1：区块大小块总字节
        private Integer sequence1;

        // 序列2：区块大小块 + 机体信息总字节
        private Integer sequence2;

        // 序列3：区块大小块 + 机体信息 + 未知信息1总字节
        private Integer sequence3;

        // 序列4：区块大小块 + 机体信息 + 未知信息1 + 武装信息总字节
        private Integer sequence4;

        // 序列5：区块大小块 + 机体信息 + 未知信息1 + 武装信息 + AI信息1总字节
        private Integer sequence5;

        // 序列6：区块大小块 + 机体信息 + 未知信息1 + 武装信息 + AI信息1 + AI信息2总字节
        private Integer sequence6;

    }

    @Data
    public static class MekBlocks {

        // 机体信息块大小
        private Integer bodyInfoBlockSize;

        // 未知信息块1的大小
        private Integer unknownInfo1BlockSize;

        // 武装信息块大小
        private Integer weaponInfoBlockSize;

        // AI信息块1的大小
        private Integer aiInfo1BlockSize;

        // AI信息块2的大小
        private Integer aiInfo2BlockSize;

        // 用于计算每个区块的字节数
        public void calculateBlockSizes(MekHead mekHead) {

            // 计算每个块的字节数
            this.bodyInfoBlockSize = mekHead.getSequence2() - mekHead.getSequence1();
            this.unknownInfo1BlockSize = mekHead.getSequence3() - mekHead.getSequence2();
            this.weaponInfoBlockSize = mekHead.getSequence4() - mekHead.getSequence3();
            this.aiInfo1BlockSize = mekHead.getSequence5() - mekHead.getSequence4();
            this.aiInfo2BlockSize = mekHead.getSequence6() - mekHead.getSequence5();
        }

    }

    @Data
    public static class MekBasicInfo {

        private String mekNameKana;
        private String mekNameEnglish;
        private String pilotNameKanji;
        private String pilotNameRoma;
        private String mekDescription;

        //
        private Integer sequence1;

        //
        private Integer sequence2;

        //
        private Integer sequence3;

        //
        private Integer sequence4;

        //
        private Integer sequence5;

        //
        private Integer sequence6;

        //
        private Integer sequence7;

        //
        private Integer sequence8;

        //
        private Integer sequence9;

        //
        private Integer sequence10;

        //
        private Integer sequence11;

        //
        private Integer sequence12;

        //
        private Integer sequence13;

        //
        private Integer sequence14;

        //
        private Integer sequence15;

        //
        private Integer sequence16;

        //
        private Integer sequence17;

        //
        private Integer sequence18;

        //
        private Integer sequence19;

        //
        private Integer sequence20;

        //
        private Integer sequence21;

        //
        private Integer sequence22;

    }

    @Data
    public static class MekWeaponInfo {

        private String weaponName;
        private String weaponSequence;
        private String weaponDescription;

        //
        private Integer sequence1;

        //
        private Integer sequence2;

        //
        private Integer sequence3;

        //
        private Integer sequence4;

        //
        private Integer sequence5;

        //
        private Integer sequence6;

        //
        private Integer sequence7;

        //
        private Integer sequence8;

        //
        private Integer sequence9;

        //
        private Integer sequence10;

        //
        private Integer sequence11;

        //
        private Integer sequence12;

        //
        private Integer sequence13;

        //
        private Integer sequence14;

        //
        private Integer sequence15;

        //
        private Integer sequence16;

        //
        private Integer sequence17;

    }

    @Data
    public static class MekAi1Info {

        private byte[] info;

    }

    @Data
    public static class MekAi2Info {

        private byte[] info;

    }

}