package com.giga.nexas.dto.bsdx.mek.parser;

import com.giga.nexas.dto.bsdx.BsdxParser;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.checker.MekChecker;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove;
import com.giga.nexas.exception.BusinessException;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.util.ParserUtil;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

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
public class MekParser implements BsdxParser<Mek> {

    @Override
    public String supportExtension() {
        return "mek";
    }

    @Override
    public Mek parse(byte[] bytes, String filename, String charset) {
        Mek mek = new Mek();
        try {
            // 1.解析头
            parseMekHead(mek, bytes);
            if (MekChecker.checkMek(mek, bytes)) {
                throw new BusinessException(500, "文件内部参数不正确，无法读取");
            }
            mek.setFileName(filename);

            // 按 MekHead 分割数组
            Mek.MekHead mekHead = mek.getMekHead();
            byte[] bodyInfoBlock     = Arrays.copyOfRange(bytes, mekHead.getSequence1(), mekHead.getSequence2());
            byte[] unknownInfoBlock1 = Arrays.copyOfRange(bytes, mekHead.getSequence2(), mekHead.getSequence3());
            byte[] weaponInfoBlock   = Arrays.copyOfRange(bytes, mekHead.getSequence3(), mekHead.getSequence4());
            byte[] aiInfoBlock1      = Arrays.copyOfRange(bytes, mekHead.getSequence4(), mekHead.getSequence5());
            byte[] aiInfoBlock2      = Arrays.copyOfRange(bytes, mekHead.getSequence5(), mekHead.getSequence6());
            byte[] mekPluginBlock    = Arrays.copyOfRange(bytes, mekHead.getSequence6(), bytes.length);

            // 2.解析机体
            parseMekInfo(mek, bodyInfoBlock, charset);
            // 3.解析未知块1
            parseMekUnknownBlock1(mek, unknownInfoBlock1);
            // 4.解析武装块
            parseMekWeaponInfo(mek, weaponInfoBlock, charset);
            // 5.解析 ai 块1
            parseMekAiInfoBlock(mek, aiInfoBlock1, charset);
            // 6.解析 ai 块2
            parseMekAiInfoBlock2(mek, aiInfoBlock2, charset);
            // 7.解析武装插槽块
            parseMekPluginBlock(mek, mekPluginBlock);

        } catch (Exception e) {
            log.info("error === {}", e.getMessage());
            throw e;
        }
        return mek;
    }

    /**
     * 解析 Mek 头
     */
    private static void parseMekHead(Mek mek, byte[] bytes) {
        Mek.MekHead mekHead = mek.getMekHead();
        BinaryReader reader = new BinaryReader(bytes);
        mekHead.setSequence1(reader.readInt());
        mekHead.setSequence2(reader.readInt());
        mekHead.setSequence3(reader.readInt());
        mekHead.setSequence4(reader.readInt());
        mekHead.setSequence5(reader.readInt());
        mekHead.setSequence6(reader.readInt());

        // 计算各块大小
        mek.getMekBlocks().calculateBlockSizes(mekHead);
    }

    /**
     * 解析机体信息块
     */
    private static void parseMekInfo(Mek mek, byte[] bytes, String charset) {
        Mek.MekBasicInfo mekInfo = mek.getMekBasicInfo();
        BinaryReader reader = new BinaryReader(bytes, charset);

        mekInfo.setMekNameKana(reader.readNullTerminatedString());

        reader.setCharset("UTF-8");
        mekInfo.setMekNameEnglish(reader.readNullTerminatedString());

        reader.setCharset(charset);
        mekInfo.setPilotNameKanji(reader.readNullTerminatedString());

        reader.setCharset("UTF-8");
        mekInfo.setPilotNameRoma(reader.readNullTerminatedString());

        // 机体描述
        reader.setCharset(charset);
        mekInfo.setMekDescription(reader.readNullTerminatedString());

        mekInfo.setWazFileSequence(reader.readInt());
        mekInfo.setSpmFileSequence(reader.readInt());
        mekInfo.setMekType(reader.readInt());
        mekInfo.setHealthRecovery(reader.readInt());
        mekInfo.setForceOnKill(reader.readInt());
        mekInfo.setBaseHealth(reader.readInt());
        mekInfo.setEnergyIncreaseLevel1(reader.readInt());
        mekInfo.setEnergyIncreaseLevel2(reader.readInt());
        mekInfo.setBoosterLevel(reader.readInt());
        mekInfo.setBoosterIncreaseLevel(reader.readInt());
        mekInfo.setPermanentArmor(reader.readInt());
        mekInfo.setComboImpactFactor(reader.readInt());
        mekInfo.setFightingAbility(reader.readInt());
        mekInfo.setShootingAbility(reader.readInt());
        mekInfo.setDurability(reader.readInt());
        mekInfo.setMobility(reader.readInt());
        mekInfo.setPhysicsWeight(reader.readInt());
        mekInfo.setWalkingSpeed(reader.readInt());
        mekInfo.setNormalDashSpeed(reader.readInt());
        mekInfo.setSearchDashSpeed(reader.readInt());
        mekInfo.setBoostDashSpeed(reader.readInt());
        mekInfo.setAutoHoverHeight(reader.readInt());
    }

    /**
     * 解析未知块 1（暂存原始字节）
     */
    private static void parseMekUnknownBlock1(Mek mek, byte[] bytes) {
        // 前面的蛆，以后再探索吧
        mek.getMekUnknownBlock1().setInfo(bytes);
    }

    /**
     * 解析武装信息块
     */
    private static void parseMekWeaponInfo(Mek mek, byte[] bytes, String charset) {
        Map<Integer, Mek.MekWeaponInfo> mekWeaponInfoMap = mek.getMekWeaponInfoMap();
        BinaryReader reader = new BinaryReader(bytes, charset);

        int weaponCount = reader.readInt();
        for (int i = 0; i < weaponCount; i++) {

            // 起始符
            int flag = reader.readInt();
            if (flag != 1) {
                return;
            }

            Mek.MekWeaponInfo mekWeaponInfo = new Mek.MekWeaponInfo();
            // 记录绝对偏移
            mekWeaponInfo.setOffset(mek.getMekHead().getSequence3() + reader.getPosition());

            reader.setCharset(charset);
            mekWeaponInfo.setWeaponName(reader.readNullTerminatedString());
            mekWeaponInfo.setWeaponSequence(reader.readNullTerminatedString());
            mekWeaponInfo.setWeaponDescription(reader.readNullTerminatedString());

            mekWeaponInfo.setSwitchToMekNo(reader.readInt());
            mekWeaponInfo.setWazSequence(reader.readInt());
            mekWeaponInfo.setForceCrashAmount(reader.readInt());
            mekWeaponInfo.setHeatMaxConsumption(reader.readInt());
            mekWeaponInfo.setHeatMinConsumption(reader.readInt());
            mekWeaponInfo.setUpgradeExp(reader.readInt());
            mekWeaponInfo.setStartPointWhenDemonstrate(reader.readInt());
            mekWeaponInfo.setWeaponCategory(reader.readInt());
            mekWeaponInfo.setWeaponType(reader.readInt());
            mekWeaponInfo.setMeleeSkillFlag(reader.readInt());
            mekWeaponInfo.setColdWeaponSkillFlag(reader.readInt());
            mekWeaponInfo.setMissileSkillFlag(reader.readInt());
            mekWeaponInfo.setBulletCategorySkillFlag(reader.readInt());
            mekWeaponInfo.setOpticalWeaponSkillFlag(reader.readInt());
            mekWeaponInfo.setDroneSkillFlag(reader.readInt());
            mekWeaponInfo.setExplosiveSkillFlag(reader.readInt());
            mekWeaponInfo.setDefensiveWeaponSkillFlag(reader.readInt());
            mekWeaponInfo.setWeaponIdentifier(reader.readInt());
            mekWeaponInfo.setWeaponUnknownProperty19(reader.readInt());

            mekWeaponInfoMap.put(i, mekWeaponInfo);
        }
    }

    /**
     * ai信息块
     */
    private static void parseMekAiInfoBlock(Mek mek, byte[] bytes, String charset) {
        List<Mek.MekAiInfo> aiInfoList = mek.getMekAiInfoList();
        BinaryReader reader = new BinaryReader(bytes, charset);

        int aiCount = reader.readInt();
        for (int i = 0; i < aiCount; i++) {
            Mek.MekAiInfo mekAiInfo = new Mek.MekAiInfo();
            mekAiInfo.setAiTypeJapanese(reader.readNullTerminatedString());
            mekAiInfo.setAiTypeEnglish(reader.readNullTerminatedString());
            int actionCount = reader.readInt();
            for (int j = 0; j < actionCount; j++) {
                short type = reader.readShort();

                CCpuEvent event;
                if (type == 1) {
                    event = new CCpuEventMove();
                    event.readInfo(reader);
                } else if (type == 2) {
                    event = new CCpuEventAttack();
                    event.readInfo(reader);
                } else {
                    log.warn("未知AI子对象类型: {}, 偏移: {}", type, reader.getPosition());
                    event = new CCpuEvent();
                }

                event.setType(type);
                mekAiInfo.getCpuEventList().add(event);
            }
            aiInfoList.add(mekAiInfo);
        }
    }

    private static void parseMekAiInfoBlock2(Mek mek, byte[] bytes, String charset) {
        // 前面的蛆，以后再探索吧
        mek.getMekVoiceInfo().setInfo(bytes);
    }

    /**
     * 解析武装插槽块（保持原有逻辑）
     */
    private static void parseMekPluginBlock(Mek mek, byte[] bytes) {
        Mek.MekPluginBlock mekPluginBlock = mek.getMekPluginBlock();
        mekPluginBlock.setInfo(bytes); // 先原样保存

        int offset = 0;
        int start;
        List<byte[]> infoList = new ArrayList<>();

        byte[] startFlag;
        if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.WEAPON_PLUGINS_FLAG_DATA)) {
            startFlag = ParserUtil.WEAPON_PLUGINS_FLAG_DATA;
        } else if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), ParserUtil.WEAPON_PLUGINS_FLAG_DATA_2)) {
            startFlag = ParserUtil.WEAPON_PLUGINS_FLAG_DATA_2;
        } else {
            throw new BusinessException(500, "未曾设想的起始符类型！");
        }

        while (offset < bytes.length) {
            // 起始符
            if (Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), startFlag)) {
                offset += 4;
                start = offset;
            } else {
                throw new BusinessException(500, "武装插槽块解析错误！");
            }

            // 内容物
            while (offset <= bytes.length) {
                if (offset != bytes.length &&
                        !Arrays.equals(Arrays.copyOfRange(bytes, offset, offset + 4), startFlag)) {
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
