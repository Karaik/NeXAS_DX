package com.giga.nexasdxeditor.dto.bsdx.mecha.mek;

import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 *
 * 另外感谢（排名不分先后）
 * @；）
 * @蓝色幻想
 * @柚木式子
 * 对逆向做出的贡献
 */
@Data
public class Mek {

    // 存储各个数据块的字节大小
    private MekBlocks mekBlocks;

    // 区块大小块
    private MekHead mekHead;

    // 机甲信息块
    private MekBasicInfo mekBasicInfo;

    // 未知信息块1
    private MekUnknownBlock1 mekUnknownBlock1;

    // 武装信息块
    private Map<Integer, MekWeaponInfo> mekWeaponInfoMap;

    // ai信息块1
    private MekAi1Info mekAi1Info;

    // ai信息块2
    private MekAi2Info mekAi2Info;

    // 未知信息块2
    private MekUnknownBlock2 mekUnknownBlock2;

    public Mek() {
        this.mekHead = new MekHead();
        this.mekBlocks = new MekBlocks();
        this.mekBasicInfo = new MekBasicInfo();
        this.mekUnknownBlock1 = new MekUnknownBlock1();
        this.mekWeaponInfoMap = new HashMap<>();
        this.mekAi1Info = new MekAi1Info();
        this.mekAi2Info = new MekAi2Info();
        this.mekUnknownBlock2 = new MekUnknownBlock2();
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

    /**
     *
     * 该大块数据信息由
     * @；）
     * （b站主页：https://space.bilibili.com/3546627702786420）
     *
     * 总结与发现
     *
     */
    @Data
    public static class MekBasicInfo {

        private String mekNameKana;
        private String mekNameEnglish;
        private String pilotNameKanji;
        private String pilotNameRoma;
        private String mekDescription;

        /**
         * 1.对应哪个.waz
         */
        private Integer wazFileSequence;

        /**
         * 2.对应哪个.spm
         */
        private Integer spmFileSequence;

        /**
         * 3.机体图鉴中的机体类型（分类和擅长）
         *
         * -虽然理论上只有六种组合，但值有很多种，进一步研究具体规律可能得统一提取出，再和机体信息对比
         *
         * 不过对于主要的机体：
         * 0D - 战斗用电子体+近距离型
         * 0E - 战斗用电子体+远距离型
         * 0F - 战斗用电子体+全能型
         *
         */
        private Integer mekType;

        /**
         * 4.作为敌机，在自机不使用FC/初始器下被击破时，自机回复的血量/3
         */
        private Integer healthRecovery;

        /**
         * 5.0级击破所得Force数
         */
        private Integer forceOnKill;

        /**
         * 6.基础血量/10
         *
         * 基础血量等于机体图鉴中的4级血量
         */
        private Integer baseHealth;

        /**
         * 7.≥几级时，能量槽+1
         */
        private Integer energyIncreaseLevel1;

        /**
         * 8.≥几级时，能量槽+1
         *
         * 7和8
         *
         * 如果第7设置为2，第8设置为5，就是：
         * 0-1级零个能量槽，2-4级一个能量槽，5级以上两个能量槽
         *
         * 如果第7设置为8，第8设置为1，就是：
         * 0级零个能量槽，1-7级一个能量槽，8级以上两个能量槽
         */
        private Integer energyIncreaseLevel2;

        /**
         * 9.从几级开始有一个助推器
         */
        private Integer boosterLevel;

        /**
         * 10.每增长几级多一个助推器
         *
         * 9和10
         * 如果第9设置为2，第10设置为5，就是：
         * 0-1级零个助推器，2-6级一个助推器，7-10级两个助推器
         *
         * 如果第9设置为0，第10设置为3，就是：
         * 0-2级一个助推器，3-5级两个助推器，6-8级三个助推器，9-10级四个助推器
         * 【注意：虽然目前游戏中使用的等级为0-10，但存在尚未仔细研究的11-15自机的等级为16，等级为16的敌机数值不稳定】
         */
        private Integer boosterIncreaseLevel;

        /**
         * 11.常驻甲/100设置为3就是300常驻甲
         */
        private Integer permanentArmor;

        /**
         * 12.未知，目前最高的好像是村正，1000修改后无明显变化
         */
        private Integer unknownProperty12;

        /**
         * 13.机体图鉴四维图的格斗
         *
         * 对应关系如下：
         * 0/1  - E
         * 2    - D
         * 3    - C
         * 4    - B
         * 5    - A
         */
        private Integer fightingAbility;

        /**
         * 14.机体图鉴四维图的射击
         */
        private Integer shootingAbility;

        /**
         * 15.机体图鉴四维图的耐久力
         */
        private Integer durability;

        /**
         * 16.机体图鉴四维图的机动力
         */
        private Integer mobility;

        /**
         * 17.未知，目前查看的机体都是0
         *
         * 修改后无明显变化
         */
        private Integer unknownProperty17;

        /**
         * 18.“走路”速度
         *
         * “走路”是镇静剂的主要移动方式
         * 诺伊也会“走路”，使用“走路”时会有ND没有的脚步声和步行动画
         * 速度单位未知
         */
        private Integer walkingSpeed;

        /**
         * 19.ND速度（normal dash）
         */
        private Integer normalDashSpeed;

        /**
         * 20.SD速度（search dash）
         */
        private Integer searchDashSpeed;

        /**
         * 21.BD速度（boost dash）
         */
        private Integer boostDashSpeed;

        /**
         * 22.无热量、不使用武装时，自动升空停止的高度
         * 
         * 大部分敌机都是0，眼球射线96
         */
        private Integer autoHoverHeight;

    }

    @Data
    public static class MekUnknownBlock1 {

        private byte[] info;

    }

    /**
     *
     * 该大块数据信息由
     * @柚木式子
     * （b站主页：https://space.bilibili.com/1420258295）
     * @；）
     * （b站主页：https://space.bilibili.com/3546627702786420）
     * 总结与发现
     *
     */
    @Data
    public static class MekWeaponInfo {

        private String weaponName;
        private String weaponSequence;
        private String weaponDescription;

        /**
         * 1.对应.waz中的哪一个武装
         */
        private Integer wazSequence;

        /**
         * 2.使用时获得的FC（是叫force crash来着？）
         */
        private Integer forceCrashAmount;

        /**
         * 3.消耗的最大热量（一管热量是80）
         */
        private Integer heatMaxConsumption;

        /**
         * 4.武装master后的热量
         *
         * 关于3和4，除主角机外这俩值都一样
         */
        private Integer heatMinConsumption;

        /**
         * 5.疑似和升级经验有关
         */
        private Integer necessaryUpgradeExp;

        /**
         * 6.武装演示时机体的起始坐标
         *
         * 负数则在右边
         */
        private Integer startPointWhenDemonstrate;

        /**
         * 7.武装种类
         *
         * 其中
         * 00   - 常规武装
         * 01   - FC（红）
         * 02   - 初始器
         *
         * 若普通武装设置为02，会直接附带初始器效果
         * 其中，初始器自身效果仅为进场演出特效不同
         */
        private Integer weaponCategory;

        /**
         * 8.武装类型
         *
         * 其中
         * 00   - 格斗
         * 01   - 射击
         * 02   - 未知1
         * 03   - 未知2
         */
        private Integer weaponType;

        // 【以下武装标签能在究极生存的兵器情报里查看，值大于0就有标签，大于0的数值具体影响未知，武装标签对武装的影响未知】
        /**
         * 9.格斗技
         */
        private Integer meleeSkillFlag;

        /**
         * 10.兵器
         */
        private Integer coldWeaponSkillFlag;

        /**
         * 11.导弹
         */
        private Integer missileSkillFlag;

        /**
         * 12.实弹
         */
        private Integer bulletCategorySkillFlag;

        /**
         * 13.光学兵器
         */
        private Integer opticalWeaponSkillFlag;

        /**
         * 14.无人机
         */
        private Integer droneSkillFlag;

        /**
         * 15.爆炸物
         */
        private Integer explosiveSkillFlag;

        /**
         * 16.防御兵器
         */
        private Integer defensiveWeaponSkillFlag;

        /**
         * 17.武装标识符
         *
         * 其中
         * 00   - 常规武装
         * 01   - hc武装（heat charge）
         * 02   - 不显示该武装
         *
         * 设置为02时，该武装会被隐藏，不显示在武装列表中
         */
        private Integer weaponIdentifier;

    }

    @Data
    public static class MekAi1Info {

        private byte[] info;

    }

    @Data
    public static class MekAi2Info {

        private byte[] info;

    }

    @Data
    public static class MekUnknownBlock2 {

        private byte[] info;

    }

}