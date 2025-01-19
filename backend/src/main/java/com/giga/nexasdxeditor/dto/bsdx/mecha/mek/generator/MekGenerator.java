package com.giga.nexasdxeditor.dto.bsdx.mecha.mek.generator;

import cn.hutool.core.date.DateUtil;
import com.giga.nexasdxeditor.dto.bsdx.mecha.mek.Mek;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description MekGenerator
 */
public class MekGenerator {

    public static void generate(String path, Mek mek) throws IOException {
        File originalFile = new File(path);
        String parentDir = originalFile.getParent();
        String fileNameWithoutExt = originalFile.getName().replaceFirst("\\.mek$", "");
        String newFileName = fileNameWithoutExt + DateUtil.current() + ".mek";
        File newFile = new File(parentDir, newFileName);

        List<byte[]> blocks = new ArrayList<>();

        byte[] bodyInfoBlock = serializeMekBasicInfo(mek);
        byte[] unknownInfo1Block = mek.getMekUnknownBlock1().getInfo();
        byte[] weaponInfoBlock = serializeMekWeaponInfoMap(mek);
        byte[] aiInfo1Block = mek.getMekAi1Info().getInfo();
        byte[] aiInfo2Block = mek.getMekAi2Info().getInfo();
        byte[] unknownInfo2Block = mek.getMekUnknownBlock2().getInfo();
        blocks.add(bodyInfoBlock);
        blocks.add(unknownInfo1Block);
        blocks.add(weaponInfoBlock);
        blocks.add(aiInfo1Block);
        blocks.add(aiInfo2Block);
        blocks.add(unknownInfo2Block);

        // 计算块序列值
        int sequence1 = 24;
        int sequence2 = sequence1 + bodyInfoBlock.length;
        int sequence3 = sequence2 + unknownInfo1Block.length;
        int sequence4 = sequence3 + weaponInfoBlock.length;
        int sequence5 = sequence4 + aiInfo1Block.length;
        int sequence6 = sequence5 + aiInfo2Block.length;

        try (FileOutputStream fos = new FileOutputStream(newFile)) {
            ByteBuffer buffer = ByteBuffer.allocate(sequence6+mek.getMekUnknownBlock2().getInfo().length);
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

    private static byte[] serializeMekBasicInfo(Mek mek) {
        Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();
        ByteBuffer buffer = ByteBuffer.allocate(calculateBodyInfoBlockSize(mek));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        putString(buffer, mekBasicInfo.getMekNameKana(), "Shift-JIS");
        putString(buffer, mekBasicInfo.getMekNameEnglish(), "Shift-JIS");
        putString(buffer, mekBasicInfo.getPilotNameKanji(), "Shift-JIS");
        putString(buffer, mekBasicInfo.getPilotNameRoma(), "Shift-JIS");
        putString(buffer, mekBasicInfo.getMekDescription(), "Shift-JIS");
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

    private static byte[] serializeMekWeaponInfoMap(Mek mek) {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        ByteBuffer buffer = ByteBuffer.allocate(calculateWeaponInfoBlockSize(mek));
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        buffer.putInt(weaponInfoMap.size());
        weaponInfoMap.values().forEach(weaponInfo -> {
            buffer.putInt(0x01);
            putString(buffer, weaponInfo.getWeaponName(), "Shift-JIS");
            putString(buffer, weaponInfo.getWeaponSequence(), "Shift-JIS");
            putString(buffer, weaponInfo.getWeaponDescription(), "Shift-JIS");
            buffer.putInt(0xFF_FF_FF_FF);
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
            buffer.putInt(0x01);
        });
        return buffer.array();
    }

    private static int calculateBodyInfoBlockSize(Mek mek) {
        Mek.MekBasicInfo mekBasicInfo = mek.getMekBasicInfo();
        int size = 0;
        size += calculateStringSize(mekBasicInfo.getMekNameKana(), "Shift-JIS");
        size += calculateStringSize(mekBasicInfo.getMekNameEnglish(), "Shift-JIS");
        size += calculateStringSize(mekBasicInfo.getPilotNameKanji(), "Shift-JIS");
        size += calculateStringSize(mekBasicInfo.getPilotNameRoma(), "Shift-JIS");
        size += calculateStringSize(mekBasicInfo.getMekDescription(), "Shift-JIS");
        size += 4 * 22;
        return size;
    }

    private static int calculateWeaponInfoBlockSize(Mek mek) {
        Map<Integer, Mek.MekWeaponInfo> weaponInfoMap = mek.getMekWeaponInfoMap();
        int size = 4;
        for (Mek.MekWeaponInfo weaponInfo : weaponInfoMap.values()) {
            // 01 00 00 00
            size += 4;
            size += calculateStringSize(weaponInfo.getWeaponName(), "Shift-JIS");
            size += calculateStringSize(weaponInfo.getWeaponSequence(), "Shift-JIS");
            size += calculateStringSize(weaponInfo.getWeaponDescription(), "Shift-JIS");
            // FF FF FF FF
            size += 4;
            size += 4 * 17;
            // 01 00 00 00
            size += 4;
        }
        return size;
    }

    private static void putString(ByteBuffer buffer, String value, String charset) {
        if (value != null) {
            buffer.put(value.getBytes(Charset.forName(charset)));
        }
        buffer.put((byte) 0x00);
    }

    private static int calculateStringSize(String value, String charset) {
        if (value == null) {
            return 1; // 仅终止符
        }
        return value.getBytes(Charset.forName(charset)).length + 1; // 字符串长度 + 终止符
    }
}