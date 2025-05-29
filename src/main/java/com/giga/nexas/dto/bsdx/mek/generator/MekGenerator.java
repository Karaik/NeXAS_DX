package com.giga.nexas.dto.bsdx.mek.generator;

import com.giga.nexas.dto.bsdx.BsdxGenerator;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import com.giga.nexas.exception.BusinessException;
import com.giga.nexas.io.BinaryWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description MekGenerator
 */
public class MekGenerator implements BsdxGenerator<Mek> {

    @Override
    public String supportExtension() {
        return "mek";
    }

    @Override
    public void generate(String path, Mek mek, String charset) throws IOException {

        File originalFile   = new File(path);
        String parentDir    = originalFile.getParent();
        String fileNameBase = originalFile.getName().replaceFirst("\\.mek$", "");
        File newFile        = new File(parentDir, fileNameBase + ".mek");

        // 区块序列化为byte[]
        byte[] bodyInfoBlock     = serializeMekBasicInfo(mek, charset);
        byte[] unknownInfo1Block = mek.getMekUnknownBlock1().getInfo();
        byte[] weaponInfoBlock   = serializeMekWeaponInfoMap(mek, charset);
        byte[] aiInfo1Block      = serializeMekAiInfoMap(mek, charset);
        byte[] aiInfo2Block      = mek.getMekVoiceInfo().getInfo();
        byte[] mekPluginBlock    = mek.getMekPluginBlock().getInfo();

        // 计算6个序列偏移
        int sequence1 = 24;
        int sequence2 = sequence1 + bodyInfoBlock.length;
        int sequence3 = sequence2 + unknownInfo1Block.length;
        int sequence4 = sequence3 + weaponInfoBlock.length;
        int sequence5 = sequence4 + aiInfo1Block.length;
        int sequence6 = sequence5 + aiInfo2Block.length;

        try (FileOutputStream fos = new FileOutputStream(newFile);
             BinaryWriter writer  = new BinaryWriter(fos, charset)) {

            // head
            writer.writeInt(sequence1);
            writer.writeInt(sequence2);
            writer.writeInt(sequence3);
            writer.writeInt(sequence4);
            writer.writeInt(sequence5);
            writer.writeInt(sequence6);

            writer.writeBytes(bodyInfoBlock);
            writer.writeBytes(unknownInfo1Block);
            writer.writeBytes(weaponInfoBlock);
            writer.writeBytes(aiInfo1Block);
            writer.writeBytes(aiInfo2Block);
            writer.writeBytes(mekPluginBlock);
        }
    }

    // 2 meka基本信息
    private static byte[] serializeMekBasicInfo(Mek mek, String charset) throws IOException {
        Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, charset)) {

            writer.writeNullTerminatedString(mekBasicInfo.getMekName());
            writer.writeNullTerminatedString(mekBasicInfo.getMekNameEnglish());
            writer.writeNullTerminatedString(mekBasicInfo.getPilotNameKanji());
            writer.writeNullTerminatedString(mekBasicInfo.getPilotNameRoma());
            writer.writeNullTerminatedString(mekBasicInfo.getMekDescription());

            writer.writeInt(mekBasicInfo.getWazFileSequence());
            writer.writeInt(mekBasicInfo.getSpmFileSequence());
            writer.writeInt(mekBasicInfo.getMekType());
            writer.writeInt(mekBasicInfo.getHealthRecovery());
            writer.writeInt(mekBasicInfo.getForceOnKill());
            writer.writeInt(mekBasicInfo.getBaseHealth());
            writer.writeInt(mekBasicInfo.getEnergyIncreaseLevel1());
            writer.writeInt(mekBasicInfo.getEnergyIncreaseLevel2());
            writer.writeInt(mekBasicInfo.getBoosterLevel());
            writer.writeInt(mekBasicInfo.getBoosterIncreaseLevel());
            writer.writeInt(mekBasicInfo.getPermanentArmor());
            writer.writeInt(mekBasicInfo.getComboImpactFactor());
            writer.writeInt(mekBasicInfo.getFightingAbility());
            writer.writeInt(mekBasicInfo.getShootingAbility());
            writer.writeInt(mekBasicInfo.getDurability());
            writer.writeInt(mekBasicInfo.getMobility());
            writer.writeInt(mekBasicInfo.getPhysicsWeight());
            writer.writeInt(mekBasicInfo.getWalkingSpeed());
            writer.writeInt(mekBasicInfo.getNormalDashSpeed());
            writer.writeInt(mekBasicInfo.getSearchDashSpeed());
            writer.writeInt(mekBasicInfo.getBoostDashSpeed());
            writer.writeInt(mekBasicInfo.getAutoHoverHeight());

            writer.close();
            return baos.toByteArray();
        }
    }

    // 3 spm相关信息？

    // 4 武装基本信息
    private static byte[] serializeMekWeaponInfoMap(Mek mek, String charset) throws IOException {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, charset)) {

            writer.writeInt(weaponInfoMap.size());
            // todo edit
            for (Mek.MekWeaponInfo weaponInfo : weaponInfoMap.values()) {
                writer.writeInt(0x01);
                writer.writeNullTerminatedString(weaponInfo.getWeaponName());
                writer.writeNullTerminatedString(weaponInfo.getWeaponSequence());
                writer.writeNullTerminatedString(weaponInfo.getWeaponDescription());

                writer.writeInt(weaponInfo.getSwitchToMekNo());
                writer.writeInt(weaponInfo.getWazSequence());
                writer.writeInt(weaponInfo.getForceCrashAmount());
                writer.writeInt(weaponInfo.getHeatMaxConsumption());
                writer.writeInt(weaponInfo.getHeatMinConsumption());
                writer.writeInt(weaponInfo.getUpgradeExp());
                writer.writeInt(weaponInfo.getStartPointWhenDemonstrate());
                writer.writeInt(weaponInfo.getWeaponCategory());
                writer.writeInt(weaponInfo.getWeaponType());
                writer.writeInt(weaponInfo.getMeleeSkillFlag());
                writer.writeInt(weaponInfo.getColdWeaponSkillFlag());
                writer.writeInt(weaponInfo.getMissileSkillFlag());
                writer.writeInt(weaponInfo.getBulletCategorySkillFlag());
                writer.writeInt(weaponInfo.getOpticalWeaponSkillFlag());
                writer.writeInt(weaponInfo.getDroneSkillFlag());
                writer.writeInt(weaponInfo.getExplosiveSkillFlag());
                writer.writeInt(weaponInfo.getDefensiveWeaponSkillFlag());
                writer.writeInt(weaponInfo.getWeaponIdentifier());
                writer.writeInt(weaponInfo.getWeaponUnknownProperty19());
            }

            writer.close();
            return baos.toByteArray();
        }
    }

    // 5 ai相关基本信息
    private static byte[] serializeMekAiInfoMap(Mek mek, String charset) throws IOException {
        List<Mek.MekAiInfo> aiInfos = mek.getMekAiInfoList();
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             BinaryWriter writer = new BinaryWriter(baos, charset)) {

            writer.writeInt(aiInfos.size());
            for (Mek.MekAiInfo ai : aiInfos) {
                writer.writeNullTerminatedString(ai.getAiTypeJapanese());
                writer.setCharset("UTF-8");
                writer.writeNullTerminatedString(ai.getAiTypeEnglish());
                writer.setCharset(charset);

                List<CCpuEvent> events = ai.getCpuEventList();

                writer.writeInt(events.size());
                for (CCpuEvent cpuEvent : events) {
                    short type = cpuEvent.getType();
                    writer.writeShort(type);
                    if (type == 1 || type == 2) {
                        cpuEvent.writeInfo(writer);
                    } else {
                        throw new BusinessException(500, "未曾设想的ai类型：" + type);
                    }
                }
            }
            writer.close();
            return baos.toByteArray();
        }
    }

    // 6 跟声音绑定的各种信息

    // 7 武装选择界面，该角色的武装插槽信息，剩余未知，推测跟spm相关，会播放动画
}