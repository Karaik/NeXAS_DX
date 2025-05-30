package com.giga.nexas.dto.bsdx.mek.generator;

import com.giga.nexas.dto.bsdx.BsdxGenerator;
import com.giga.nexas.dto.bsdx.mek.Mek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
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
        File originalFile = new File(path);
        String parentDir = originalFile.getParent();
        String fileNameWithoutExt = originalFile.getName().replaceFirst("\\.mek$", "");
        String newFileName = fileNameWithoutExt + ".mek";
        File newFile = new File(parentDir, newFileName);

        List<byte[]> blocks = new ArrayList<>();

        byte[] bodyInfoBlock = serializeMekBasicInfo(mek, charset);
        byte[] unknownInfo1Block = mek.getMekUnknownBlock1().getInfo();
        byte[] weaponInfoBlock = serializeMekWeaponInfoMap(mek, charset);
        byte[] aiInfo1Block = mek.getMekAi1Info().getInfo();
        byte[] aiInfo2Block = mek.getMekAi2Info().getInfo();
        byte[] mekPluginBlock = mek.getMekPluginBlock().getInfo();
        blocks.add(bodyInfoBlock);
        blocks.add(unknownInfo1Block);
        blocks.add(weaponInfoBlock);
        blocks.add(aiInfo1Block);
        blocks.add(aiInfo2Block);
        blocks.add(mekPluginBlock);

        // 计算块序列值
        int sequence1 = 24;
        int sequence2 = sequence1 + bodyInfoBlock.length;
        int sequence3 = sequence2 + unknownInfo1Block.length;
        int sequence4 = sequence3 + weaponInfoBlock.length;
        int sequence5 = sequence4 + aiInfo1Block.length;
        int sequence6 = sequence5 + aiInfo2Block.length;

        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            ByteBuffer buffer = ByteBuffer.allocate(sequence6+mek.getMekPluginBlock().getInfo().length);
            buffer.order(ByteOrder.LITTLE_ENDIAN);

            // 写入头部
            buffer.putInt(sequence1);
            buffer.putInt(sequence2);
            buffer.putInt(sequence3);
            buffer.putInt(sequence4);
            buffer.putInt(sequence5);
            buffer.putInt(sequence6);

            // 写入各块数据
            for (byte[] block : blocks) {
                buffer.put(block);
            }

            fos.write(buffer.array());
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
        buffer.putInt(mekBasicInfo.getUnknownProperty12());
        buffer.putInt(mekBasicInfo.getFightingAbility());
        buffer.putInt(mekBasicInfo.getShootingAbility());
        buffer.putInt(mekBasicInfo.getDurability());
        buffer.putInt(mekBasicInfo.getMobility());
        buffer.putInt(mekBasicInfo.getUnknownProperty17());
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
            buffer.putInt(weaponInfo.getWeaponUnknownProperty1());
            buffer.putInt(weaponInfo.getWazSequence());
            buffer.putInt(weaponInfo.getForceCrashAmount());
            buffer.putInt(weaponInfo.getHeatMaxConsumption());
            buffer.putInt(weaponInfo.getHeatMinConsumption());
            buffer.putInt(weaponInfo.getNecessaryUpgradeExp());
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
            // 01 00 00 00
            size += 4;
            size += calculateStringSize(weaponInfo.getWeaponName(), charset);
            size += calculateStringSize(weaponInfo.getWeaponSequence(), charset);
            size += calculateStringSize(weaponInfo.getWeaponDescription(), charset);
            // FF FF FF FF
//            size += 4;
            size += 4 * 19;
            // 01 00 00 00
//            size += 4;
        }
        return size;
    }

}