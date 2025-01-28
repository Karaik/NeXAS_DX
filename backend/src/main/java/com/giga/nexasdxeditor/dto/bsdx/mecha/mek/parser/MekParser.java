package com.giga.nexasdxeditor.dto.bsdx.mecha.mek.parser;

import cn.hutool.core.util.ByteUtil;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.Mek;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.checker.MekChecker;
import com.giga.nexasdxeditor.exception.BusinessException;
import com.giga.nexasdxeditor.util.ParserUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.giga.nexasdxeditor.util.ParserUtil.findNullTerminator;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 *
 * 另外感谢（排名不分先后）
 * @；）
 * @蓝色幻想
 * @柚木式子
 * 对逆向做出的贡献
 */
@Slf4j
public class MekParser {

    // 每个机体的数据区块大小固定 共有22个数据
    public static final Integer BSDX_MEK_INFO_DATA_BLOCK_QUANTITY = 88 / 4;

    // 每个武装的数据区块大小固定 共有17个数据
    public static final Integer BSDX_MEK_WEAPON_DATA_BLOCK_QUANTITY = 68 / 4;

    public static Mek parseMek(byte[] bytes, String charset) {

        Mek mek = new Mek();
        try {

            // 1.解析头
            parseMekHead(mek, bytes);
            if (MekChecker.checkMek(mek, bytes)) {
                throw new BusinessException(500, "文件内部参数不正确，无法读取");
            }

            // 按MekHead分割数组
            Mek.MekHead mekHead = mek.getMekHead();
            // 机体信息块
            byte[] bodyInfoBlock = Arrays.copyOfRange(bytes, mekHead.getSequence1(), mekHead.getSequence2());
            // 未知信息块1
            byte[] unknownInfoBlock1 = Arrays.copyOfRange(bytes, mekHead.getSequence2(), mekHead.getSequence3());
            // 武装信息块
            byte[] weaponInfoBlock = Arrays.copyOfRange(bytes, mekHead.getSequence3(), mekHead.getSequence4());
            // AI信息块1
            byte[] aiInfoBlock1 = Arrays.copyOfRange(bytes, mekHead.getSequence4(), mekHead.getSequence5());
            // AI信息块2
            byte[] aiInfoBlock2 = Arrays.copyOfRange(bytes, mekHead.getSequence5(), mekHead.getSequence6());
            // 武装插槽块
            byte[] mekPluginBlock = Arrays.copyOfRange(bytes, mekHead.getSequence6(), bytes.length);

            // 2.解析机体
            parseMekInfo(mek, bodyInfoBlock, charset);

            // 3.解析未知块1
            parseMekUnknownBlock1(mek, unknownInfoBlock1);

            // 4.解析武装块
            parseMekWeaponInfo(mek, weaponInfoBlock, charset);

            // 5.解析ai块1
            parseMekAiInfoBlock1(mek, aiInfoBlock1);

            // 6.解析ai块2
            parseMekAiInfoBlock2(mek, aiInfoBlock2);

            // 7.解析武装插槽块
            parseMekPluginBlock(mek, mekPluginBlock);


        } catch (Exception e) {
            log.info("error === {}", e.getMessage());
        }

        return mek;
    }

    public static void parseMekHead(Mek mek, byte[] bytes) {
        Mek.MekHead mekHead = mek.getMekHead();

        mekHead.setSequence1(readInt32(bytes, 0));
        mekHead.setSequence2(readInt32(bytes, 4));
        mekHead.setSequence3(readInt32(bytes, 8));
        mekHead.setSequence4(readInt32(bytes, 12));
        mekHead.setSequence5(readInt32(bytes, 16));
        mekHead.setSequence6(readInt32(bytes, 20));

        // 计算各块大小
        mek.getMekBlocks().calculateBlockSizes(mekHead);

    }

    private static void parseMekInfo(Mek mek, byte[] bytes, String charset) {
        Mek.MekBasicInfo mekInfo = mek.getMekBasicInfo();
        int offset = 0;

        mekInfo.setMekNameKana(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
        offset += findNullTerminator(bytes, offset) + 1;

        mekInfo.setMekNameEnglish(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName("UTF-8")));
        offset += findNullTerminator(bytes, offset) + 1;

        mekInfo.setPilotNameKanji(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
        offset += findNullTerminator(bytes, offset) + 1;

        mekInfo.setPilotNameRoma(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName("UTF-8")));
        offset += findNullTerminator(bytes, offset) + 1;

        mekInfo.setMekDescription(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
        offset += findNullTerminator(bytes, offset) + 1;

        mekInfo.setWazFileSequence(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setSpmFileSequence(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setMekType(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setHealthRecovery(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setForceOnKill(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setBaseHealth(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setEnergyIncreaseLevel1(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setEnergyIncreaseLevel2(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setBoosterLevel(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setBoosterIncreaseLevel(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setPermanentArmor(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setUnknownProperty12(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setFightingAbility(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setShootingAbility(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setDurability(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setMobility(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setUnknownProperty17(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setWalkingSpeed(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setNormalDashSpeed(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setSearchDashSpeed(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setBoostDashSpeed(readInt32(bytes, offset));
        offset += 4;

        mekInfo.setAutoHoverHeight(readInt32(bytes, offset));
    }

    private static void parseMekUnknownBlock1(Mek mek, byte[] bytes) {
        // 前面的蛆，以后再探索吧
        Mek.MekUnknownBlock1 mekUnknownBlock1 = mek.getMekUnknownBlock1();
        mekUnknownBlock1.setInfo(bytes);
    }

    private static void parseMekWeaponInfo(Mek mek, byte[] bytes, String charset) {
        Map<Integer, Mek.MekWeaponInfo> mekWeaponInfoMap = mek.getMekWeaponInfoMap();
        int offset = 0;

        int weaponCount = readInt32(bytes, offset);
        offset += 4;

        for (int i = 0; i < weaponCount; i++) {

            // 起始符
            if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.FLAG_DATA)) {
                offset += 4;
            }

            Mek.MekWeaponInfo mekWeaponInfo = new Mek.MekWeaponInfo();

            mekWeaponInfo.setWeaponName(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
            offset += findNullTerminator(bytes, offset) + 1;

            mekWeaponInfo.setWeaponSequence(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
            offset += findNullTerminator(bytes, offset) + 1;

            mekWeaponInfo.setWeaponDescription(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
            offset += findNullTerminator(bytes, offset) + 1;

            if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.SPLIT_DATA)) {
                offset += 4;
            }

            mekWeaponInfo.setWazSequence(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setForceCrashAmount(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setHeatMaxConsumption(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setHeatMinConsumption(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setNecessaryUpgradeExp(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setStartPointWhenDemonstrate(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setWeaponCategory(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setWeaponType(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setMeleeSkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setColdWeaponSkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setMissileSkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setBulletCategorySkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setOpticalWeaponSkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setDroneSkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setExplosiveSkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setDefensiveWeaponSkillFlag(readInt32(bytes, offset));
            offset += 4;

            mekWeaponInfo.setWeaponIdentifier(readInt32(bytes, offset));
            offset += 4;

            // 结尾符
            if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.FLAG_DATA)) {
                offset += 4;
            }

            mekWeaponInfoMap.put(i, mekWeaponInfo);
        }
    }

    private static void parseMekAiInfoBlock1(Mek mek, byte[] bytes) {
        // 前面的蛆，以后再探索吧
        Mek.MekAi1Info MekAiInfoBlock1 = mek.getMekAi1Info();
        MekAiInfoBlock1.setInfo(bytes);
    }

    private static void parseMekAiInfoBlock2(Mek mek, byte[] bytes) {
        // 前面的蛆，以后再探索吧
        Mek.MekAi2Info MekAiInfoBlock2 = mek.getMekAi2Info();
        MekAiInfoBlock2.setInfo(bytes);
    }

    private static void parseMekPluginBlock(Mek mek, byte[] bytes) {

        Mek.MekPluginBlock mekPluginBlock = mek.getMekPluginBlock();

        // 原路放回
        mekPluginBlock.setInfo(bytes);

        // TODO
        // 尝试解析
        int offset = 0;
        int start = 0;
        List<byte[]> infoList = new ArrayList<>();
        while (offset < bytes.length) {
            // 起始符
            if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.WEAPON_PLUGINS_FLAG_DATA)) {
                offset += 4;
                start = offset;
            } else {
                throw new BusinessException(500, "武装插槽块解析错误！");
            }

            // 内容物
            while (offset <= bytes.length) {
                if (offset != bytes.length &&
                        !Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.WEAPON_PLUGINS_FLAG_DATA)) {
                    offset += 4;
                } else {
                    byte[] chunk = Arrays.copyOfRange(bytes, start, offset);
                    infoList.add(chunk);
                    break;
                }
            }

        }

        // 存储常规状态（武装）插槽
        int regularPluginInfoSize = infoList.size() - mek.getMekWeaponInfoMap().size();
        List<byte[]> regularPluginInfoList = mekPluginBlock.getRegularPluginInfoList();
        for (int i = 0; i < regularPluginInfoSize; i++) {
            regularPluginInfoList.add(infoList.get(i));
        }

        // 存储武装插槽
        List<byte[]> weaponPluginInfoList = mekPluginBlock.getWeaponPluginInfoList();
        for (int i = 0; i < mek.getMekWeaponInfoMap().size(); i++) {
            int index = regularPluginInfoSize + i;
            byte[] weaponPluginInfo = infoList.get(index);
            weaponPluginInfoList.add(weaponPluginInfo);
            mek.getMekWeaponInfoMap().get(i).setWeaponPluginInfo(weaponPluginInfo);
        }


    }

}
