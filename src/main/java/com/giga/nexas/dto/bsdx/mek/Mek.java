package com.giga.nexas.dto.bsdx.mek;

import com.giga.nexas.dto.bsdx.Bsdx;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import lombok.Data;

import java.util.*;

/**
 * @Author 这位同学(Karaik)
 *
 * 另外感谢（排名不分先后）
 * @；）
 * @蓝色幻想
 * @柚木式子
 * 对逆向做出的贡献
 *
 * @note mek中的数据大致可分为7个部分，目前的发现为
 * 1 偏移量块
 * 2 机体基本信息块
 * 3 spm信息块
 * 4 武装信息块
 * 5 ai块
 * 6 疑似播放音频相关
 * 7 武装栏位块
 *
 * 经深度逆向发现并由本人完善结构
 */
@Data
public class Mek extends Bsdx {

    private String fileName;

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

    // ai信息块
    private List<MekAiInfo> mekAiInfoList;

    // 声音信息块
    private MekVoiceInfo mekVoiceInfo;

    // 未知信息块2
    private MekPluginBlock mekPluginBlock;

    public Mek() {
        this.mekHead = new MekHead();
        this.mekBlocks = new MekBlocks();
        this.mekBasicInfo = new MekBasicInfo();
        this.mekUnknownBlock1 = new MekUnknownBlock1();
        this.mekWeaponInfoMap = new LinkedHashMap<>();
        this.mekAiInfoList = new ArrayList<>();
        this.mekVoiceInfo = new MekVoiceInfo();
        this.mekPluginBlock = new MekPluginBlock();
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

        // 序列5：区块大小块 + 机体信息 + 未知信息1 + 武装信息 + AI信息总字节
        private Integer sequence5;

        // 序列6：区块大小块 + 机体信息 + 未知信息1 + 武装信息 + AI信息 + 声音信息总字节
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

        // AI信息块的大小
        private Integer aiInfoBlockSize;

        // 声音信息块的大小
        private Integer voiceInfoBlockSize;

        // 用于计算每个区块的字节数
        public void calculateBlockSizes(MekHead mekHead) {

            // 计算每个块的字节数
            this.bodyInfoBlockSize = mekHead.getSequence2() - mekHead.getSequence1();
            this.unknownInfo1BlockSize = mekHead.getSequence3() - mekHead.getSequence2();
            this.weaponInfoBlockSize = mekHead.getSequence4() - mekHead.getSequence3();
            this.aiInfoBlockSize = mekHead.getSequence5() - mekHead.getSequence4();
            this.voiceInfoBlockSize = mekHead.getSequence6() - mekHead.getSequence5();
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
     * 逆向完善by：我（Karaik）
     *
     */
    @Data
    public static class MekBasicInfo {

        // jpメカ名
        private String mekNameKana;

        // jpメカのルビ
        private String mekNameEnglish;

        // jpパイロット名
        private String pilotNameKanji;

        // jpパイロットのルビ
        private String pilotNameRoma;

        // jpメカ解説
        private String mekDescription;

        /**
         * jpデフォルト技グルー
         * 1.对应哪个.waz
         */
        private Integer wazFileSequence;

        /**
         * jpデフォルトスプライトグループ
         * 2.对应哪个.spm
         */
        private Integer spmFileSequence;

        /**
         * jpフラグ
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
         * jp強さＬｖ
         * 4.作为敌机，在自机不使用FC/初始器下被击破时，自机回复的血量/3
         */
        private Integer healthRecovery;

        /**
         * jpフォース数
         * 5.0级击破所得Force数
         */
        private Integer forceOnKill;

        /**
         * jp耐久力
         * 6.基础血量/10
         *
         * 基础血量等于机体图鉴中的4级血量
         */
        private Integer baseHealth;

        /**
         * jpＦＣゲージLv1
         * 7.≥几级时，能量槽+1
         */
        private Integer energyIncreaseLevel1;

        /**
         * jpＦＣゲージLv2
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
         * jpブースターが１になるLv
         * 9.从几级开始有一个助推器
         */
        private Integer boosterLevel;

        /**
         * jpブースター増分
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
         * jp常時装甲
         * 11.常驻甲/100 设置为3就是300常驻甲
         */
        private Integer permanentArmor;

        /**
         * jpコンボ感想反映率
         * 12.疑似技能影响因子
         * 修改后表现为：在部分角色使用特定技能时，目标被击中的浮空高度变高。
         * 可能影响浮空类技能的combo连击稳定性、浮空持续时间。
         * 数值越高，浮空效果越明显
         *
         * 当前观察值范围：0～1000，部分机体（如村正）为 1000
         * 通过逆向出的字符串，推测该系数大概率为技能物理动作效果的调节因子
         */
        private Integer comboImpactFactor;

        /**
         * jp性能：格闘
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
         * jp性能：射撃
         * 14.机体图鉴四维图的射击
         */
        private Integer shootingAbility;

        /**
         * jp性能：耐久力
         * 15.机体图鉴四维图的耐久力
         */
        private Integer durability;

        /**
         * jp性能：機動
         * 16.机体图鉴四维图的机动力
         */
        private Integer mobility;

        /**
         * jp重量
         * 17.重量
         *
         * 修改后无明显变化
         * 经过逆向后根据字符串判断为物理上的重量
         * 在bhe中有一关会增加角色受到的重力，虽然bsdx里好像没用
         */
        private Integer physicsWeight;

        /**
         * jp速度：歩行
         * 18.“走路”速度
         *
         * “走路”是镇静剂的主要移动方式
         * 诺伊也会“走路”，使用“走路”时会有ND没有的脚步声和步行动画
         * 速度单位未知
         */
        private Integer walkingSpeed;

        /**
         * jp速度：ダッシュ
         * 19.ND速度（normal dash）
         */
        private Integer normalDashSpeed;

        /**
         * jp速度：サーチダッシュ
         * 20.SD速度（search dash）
         */
        private Integer searchDashSpeed;

        /**
         * jp速度：ブーストダッシュ
         * 21.BD速度（boost dash）
         */
        private Integer boostDashSpeed;

        /**
         * jp浮遊高度
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
     * 该大块数据信息感谢以下
     * @柚木式子
     * （b站主页：https://space.bilibili.com/1420258295）
     * @；）
     * （b站主页：https://space.bilibili.com/3546627702786420）
     * 群友的部分总结与发现
     *
     */
    @Data
    public static class MekWeaponInfo {

        // 仅用作记录
        public int offset;

        /**
         * jpヘッダ名
         * header名，武器名字
         */
        private String weaponName;
        /**
         * jpメカグループ番号
         * .mek的序号
         */
        private String weaponSequence;
        /**
         * jp解説
         * 为什么逆向后，这个会在数组的最后一项呢？
         */
        private String weaponDescription;

        /**
         * 1.使用该武装时，会切换为对应编号的mek
         */
        private Integer switchToMekNo;

        /**
         * jp技番号
         * 2.对应.waz中的哪一个武装
         */
        private Integer wazSequence;

        /**
         * 3.使用时获得的FC
         * 这个倒是没有逆向出字符串，为什么呢？
         */
        private Integer forceCrashAmount;

        /**
         * jp蓄積熱量：初期値
         * 4.消耗的最大热量（一管热量是80）
         */
        private Integer heatMaxConsumption;

        /**
         * jp蓄積熱量：最小値
         * 5.武装master后的热量
         *
         * 关于4和5，除主角机外这俩值都一样
         */
        private Integer heatMinConsumption;

        /**
         * jp必要経験値
         * 6.升级所需经验
         */
        private Integer upgradeExp;

        /**
         * jpデモ時の距離
         * 7.武装演示时机体的起始坐标
         *
         * 负数则在右边
         */
        private Integer startPointWhenDemonstrate;

        /**
         * jpカテゴリ
         * 8.武装种类
         *
         * 其中
         * 00   - 常规武装
         * 01   - FC（红）
         * 02   - 初始器
         *
         * 若普通武装设置为02，会直接附带初始器效果
         * 其中，初始器自身效果仅为进场演出特效不同
         * ps：为什么逆向后，字符串不是按照实际排列顺序来的呢？
         */
        private Integer weaponCategory;

        /**
         * jp攻撃タイプ
         * 9.武装类型
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
         * jpタイプ：格闘技
         * 10.格斗技
         */
        private Integer meleeSkillFlag;

        /**
         * jpタイプ：武器
         * 11.兵器
         */
        private Integer coldWeaponSkillFlag;

        /**
         * jpタイプ：ミサイル
         * 12.导弹
         */
        private Integer missileSkillFlag;

        /**
         * jpタイプ：実弾
         * 13.实弹
         */
        private Integer bulletCategorySkillFlag;

        /**
         * jpタイプ：光学兵器
         * 14.光学兵器
         */
        private Integer opticalWeaponSkillFlag;

        /**
         * jpタイプ：ビット
         * 15.无人机
         */
        private Integer droneSkillFlag;

        /**
         * jpタイプ：爆発物
         * 16.爆炸物
         */
        private Integer explosiveSkillFlag;

        /**
         * jpタイプ：防御兵器
         * 17.防御兵器
         */
        private Integer defensiveWeaponSkillFlag;

        /**
         * jpフラグ
         * 18.武装标识符
         *
         * 其中
         * 00   - 常规武装
         * 01   - hc武装（heat charge）
         * 02   - 不显示该武装
         *
         * 设置为02时，该武装会被隐藏，不显示在武装列表中
         */
        private Integer weaponIdentifier;

        /**
         * 19.
         * ps:攻撃Ｌｖ是你吗？
         */
        private Integer weaponUnknownProperty19;

        /**
         * 该武装对应的插槽信息
         */
        private byte[] weaponPluginInfo;

    }

    @Data
    public static class MekAiInfo {

        private String aiTypeJapanese;
        private String aiTypeEnglish;

        private List<CCpuEvent> cpuEventList = new ArrayList<>();

    }

    @Data
    public static class MekVoiceInfo {

        private byte[] info;

    }

    /**
     * 极大可能为武装选择列表（插槽块）
     * 其中，开头的几个为常规块，如走路、站立、ND、BD等，因机体的不同有包括数量在内的差异
     * 另，内容物作用尚未查明，故直接以byte数组存储
     */
    @Data
    public static class MekPluginBlock {

        private byte[] info;

        private List<byte[]> regularPluginInfoList;

        private List<byte[]> weaponPluginInfoList;

        public MekPluginBlock() {
            this.regularPluginInfoList = new ArrayList<>();
            this.weaponPluginInfoList = new ArrayList<>();
        }

    }

}