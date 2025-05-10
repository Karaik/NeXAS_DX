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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.List;
import java.util.Map;

import static com.giga.nexas.util.GenerateUtil.calculateStringSize;
import static com.giga.nexas.util.GenerateUtil.putString;

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
        byte[] aiInfo2Block      = mek.getMekAi2Info().getInfo();
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

    private static byte[] serializeMekBasicInfo(Mek mek, String charset) {
        Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();
        ByteBuffer buffer = ByteBuffer.allocate(calculateBodyInfoBlockSize(mek, charset));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        putString(buffer, mekBasicInfo.getMekNameKana(), charset);
        putString(buffer, mekBasicInfo.getMekNameEnglish(), charset);
        putString(buffer, mekBasicInfo.getPilotNameKanji(), charset);
        putString(buffer, mekBasicInfo.getPilotNameRoma(), charset);
        putString(buffer, mekBasicInfo.getMekDescription(), charset);
        buffer.putInt(mekBasicInfo.getWazFileSequence());
        buffer.putInt(mekBasicInfo.getSpmFileSequence());
        buffer.putInt(mekBasicInfo.getMekType());
        buffer.putInt(mekBasicInfo.getHealthRecovery());
        buffer.putInt(mekBasicInfo.getForceOnKill());
        buffer.putInt(mekBasicInfo.getBaseHealth());
        buffer.putInt(mekBasicInfo.getEnergyIncreaseLevel1());
        buffer.putInt(mekBasicInfo.getEnergyIncreaseLevel2());
        buffer.putInt(mekBasicInfo.getBoosterLevel());
        buffer.putInt(mekBasicInfo.getBoosterIncreaseLevel());
        buffer.putInt(mekBasicInfo.getPermanentArmor());
        buffer.putInt(mekBasicInfo.getComboImpactFactor());
        buffer.putInt(mekBasicInfo.getFightingAbility());
        buffer.putInt(mekBasicInfo.getShootingAbility());
        buffer.putInt(mekBasicInfo.getDurability());
        buffer.putInt(mekBasicInfo.getMobility());
        buffer.putInt(mekBasicInfo.getPhysicsWeight());
        buffer.putInt(mekBasicInfo.getWalkingSpeed());
        buffer.putInt(mekBasicInfo.getNormalDashSpeed());
        buffer.putInt(mekBasicInfo.getSearchDashSpeed());
        buffer.putInt(mekBasicInfo.getBoostDashSpeed());
        buffer.putInt(mekBasicInfo.getAutoHoverHeight());
        return buffer.array();
    }

    private static byte[] serializeMekWeaponInfoMap(Mek mek, String charset) {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        ByteBuffer buffer = ByteBuffer.allocate(calculateWeaponInfoBlockSize(mek, charset));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(weaponInfoMap.size());
        weaponInfoMap.values().forEach(weaponInfo -> {
            buffer.putInt(0x01);
            putString(buffer, weaponInfo.getWeaponName(), charset);
            putString(buffer, weaponInfo.getWeaponSequence(), charset);
            putString(buffer, weaponInfo.getWeaponDescription(), charset);
            buffer.putInt(weaponInfo.getSwitchToMekNo());
            buffer.putInt(weaponInfo.getWazSequence());
            buffer.putInt(weaponInfo.getForceCrashAmount());
            buffer.putInt(weaponInfo.getHeatMaxConsumption());
            buffer.putInt(weaponInfo.getHeatMinConsumption());
            buffer.putInt(weaponInfo.getUpgradeExp());
            buffer.putInt(weaponInfo.getStartPointWhenDemonstrate());
            buffer.putInt(weaponInfo.getWeaponCategory());
            buffer.putInt(weaponInfo.getWeaponType());
            buffer.putInt(weaponInfo.getMeleeSkillFlag());
            buffer.putInt(weaponInfo.getColdWeaponSkillFlag());
            buffer.putInt(weaponInfo.getMissileSkillFlag());
            buffer.putInt(weaponInfo.getBulletCategorySkillFlag());
            buffer.putInt(weaponInfo.getOpticalWeaponSkillFlag());
            buffer.putInt(weaponInfo.getDroneSkillFlag());
            buffer.putInt(weaponInfo.getExplosiveSkillFlag());
            buffer.putInt(weaponInfo.getDefensiveWeaponSkillFlag());
            buffer.putInt(weaponInfo.getWeaponIdentifier());
            buffer.putInt(weaponInfo.getWeaponUnknownProperty19());
        });
        return buffer.array();
    }

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

    private static int calculateBodyInfoBlockSize(Mek mek, String charset) {
        Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();
        int size = 0;
        size += calculateStringSize(mekBasicInfo.getMekNameKana(), charset);
        size += calculateStringSize(mekBasicInfo.getMekNameEnglish(), charset);
        size += calculateStringSize(mekBasicInfo.getPilotNameKanji(), charset);
        size += calculateStringSize(mekBasicInfo.getPilotNameRoma(), charset);
        size += calculateStringSize(mekBasicInfo.getMekDescription(), charset);
        size += 4 * 22;
        return size;
    }

    private static int calculateWeaponInfoBlockSize(Mek mek, String charset) {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        int size = 4;
        for (Mek.MekWeaponInfo weaponInfo : weaponInfoMap.values()) {
            // 01 00 00 00 flag，表示下一个块是否存在，目前没有发现不存在的
            size += 4;
            size += calculateStringSize(weaponInfo.getWeaponName(), charset);
            size += calculateStringSize(weaponInfo.getWeaponSequence(), charset);
            size += calculateStringSize(weaponInfo.getWeaponDescription(), charset);
            size += 4 * 19;
        }
        return size;
    }

}