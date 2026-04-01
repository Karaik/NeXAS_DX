package com.giga.nexas.dto.bhe.mek.parser;

import com.giga.nexas.dto.bhe.mek.Mek;
import com.giga.nexas.dto.bhe.mek.checker.MekChecker;
import com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bhe.mek.mekcpu.CCpuEventMove;
import com.giga.nexas.dto.bhe.BheParser;
import com.giga.nexas.exception.OperationException;
import com.giga.nexas.io.BinaryReader;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Slf4j
public class MekParser implements BheParser<Mek> {

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
                throw new OperationException(500, "invalid file header!");
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
            parseMekPairBlock(mek, unknownInfoBlock1, charset);
            // 4.解析武装块
            parseMekWeaponInfo(mek, weaponInfoBlock, charset);
            // 5.解析 ai 块1
            parseMekAiInfoBlock(mek, aiInfoBlock1, charset);
            // 6.解析 ai 块2
            parseVoiceInfo(mek, aiInfoBlock2, charset);
            // 7.解析武装插槽块
            parseMekMaterialBlock(mek, mekPluginBlock);

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

        mekInfo.setMekName(reader.readNullTerminatedString());

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
    private static void parseMekPairBlock(Mek mek, byte[] bytes, String charset) {
        Mek.MekPairBlock mekPairBlock = mek.getMekPairBlock();
        BinaryReader reader = new BinaryReader(bytes, charset);

        List<Mek.MekPairBlock.Pair> unkPair = mekPairBlock.getUnkPair();
        for (int i = 0; i < 14; i++) {
            Mek.MekPairBlock.Pair pair = new Mek.MekPairBlock.Pair();
            pair.setInt1(reader.readInt());
            pair.setInt2(reader.readInt());
            unkPair.add(pair);
        }
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
            // diff
            mekWeaponInfo.setBheInt1(reader.readInt());
            mekWeaponInfo.setBheInt2(reader.readInt());

            mekWeaponInfo.setUpgradeExp(reader.readInt());
            mekWeaponInfo.setStartPointWhenDemonstrate(reader.readInt());
            // diff
            mekWeaponInfo.setBheInt3(reader.readInt());
            mekWeaponInfo.setBheInt4(reader.readInt());

            mekWeaponInfo.setWeaponCategory(reader.readInt());
            mekWeaponInfo.setWeaponType(reader.readInt());
            // diff
            mekWeaponInfo.setBheInt5(reader.readInt());
            mekWeaponInfo.setBheInt6(reader.readInt());

            mekWeaponInfo.setMeleeSkillFlag(reader.readInt());
            mekWeaponInfo.setColdWeaponSkillFlag(reader.readInt());
            mekWeaponInfo.setMissileSkillFlag(reader.readInt());
            mekWeaponInfo.setBulletCategorySkillFlag(reader.readInt());
            mekWeaponInfo.setOpticalWeaponSkillFlag(reader.readInt());
            mekWeaponInfo.setDroneSkillFlag(reader.readInt());
            mekWeaponInfo.setExplosiveSkillFlag(reader.readInt());
            mekWeaponInfo.setDefensiveWeaponSkillFlag(reader.readInt());
            mekWeaponInfo.setWeaponIdentifier(reader.readInt());
            // diff
            mekWeaponInfo.setBheInt7(reader.readInt());

            mekWeaponInfo.setWeaponUnknownProperty19(reader.readInt());
            // diff
            mekWeaponInfo.setFeiFlag(reader.readInt());
            for (int integer = 0; integer < mekWeaponInfo.getFeiFlag(); integer++) {
                parseMekWeaponFeiInfo(mekWeaponInfo, reader);
            }

            mekWeaponInfoMap.put(i, mekWeaponInfo);
        }
    }

    private static void parseMekWeaponFeiInfo(Mek.MekWeaponInfo mekWeaponInfo, BinaryReader reader) {
        List<Mek.MekWeaponInfo.Fei> feiList = mekWeaponInfo.getFeiList();

        Mek.MekWeaponInfo.Fei feiInfo = new Mek.MekWeaponInfo.Fei();
        feiInfo.setFeiInt1(reader.readInt());
        feiInfo.setFeiInt2(reader.readInt());
        feiInfo.setFeiInt3(reader.readInt());
        feiInfo.setFeiInt4(reader.readInt());
        feiInfo.setFeiInt5(reader.readInt());
        feiInfo.setFeiInt6(reader.readInt());
        feiInfo.setFeiInt7(reader.readInt());

        feiList.add(feiInfo);
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

    private static void parseVoiceInfo(Mek mek, byte[] bytes, String charset) {
        Mek.MekVoiceInfo mekVoiceInfo = mek.getMekVoiceInfo();
        BinaryReader reader = new BinaryReader(bytes, charset);

        mekVoiceInfo.setVersion(reader.readInt());

        int emotionCount = reader.readInt();
        for (int i = 0; i < emotionCount; i++) {
            Mek.MekVoiceInfo.Emotion emotion = new Mek.MekVoiceInfo.Emotion();
            emotion.setName(reader.readNullTerminatedString());
            emotion.setToken(reader.readNullTerminatedString());
            mekVoiceInfo.getEmotions().add(emotion);
        }

        int voiceSlotCount = reader.readInt();
        for (int i = 0; i < voiceSlotCount; i++) {
            Mek.MekVoiceInfo.VoiceSlot voiceSlot = new Mek.MekVoiceInfo.VoiceSlot();
            voiceSlot.setName(reader.readNullTerminatedString());
            voiceSlot.setToken(reader.readNullTerminatedString());
            mekVoiceInfo.getVoiceSlots().add(voiceSlot);
        }

        final List<List<List<Mek.MekVoiceInfo.Entry>>> table = mekVoiceInfo.getTable();
        int rows = 0;
        while (reader.getPosition() < bytes.length) {
            List<List<Mek.MekVoiceInfo.Entry>> row = new ArrayList<>(voiceSlotCount);
            for (int c = 0; c < voiceSlotCount; c++) {
                int n = reader.readInt();
                List<Mek.MekVoiceInfo.Entry> cell = new ArrayList<>(n);
                for (int k = 0; k < n; k++) {
                    Mek.MekVoiceInfo.Entry entry = new Mek.MekVoiceInfo.Entry();
                    entry.setVoiceType(reader.readInt());
                    entry.setGroupId(reader.readInt());
                    entry.setWeight(reader.readInt());
                    cell.add(entry);
                }
                row.add(cell);
            }
            table.add(row);
            rows++;
        }

        mekVoiceInfo.builtinEmotionCount = rows - emotionCount;
    }

    // todo
    // 20260331 2 bhe -> bsdx，常规块（regular）部分直接采用所要移植到目标对象的常规块，后面挂接的部分置空（flag=0）
    private static void parseMekMaterialBlock(Mek mek, byte[] blockBytes) {
        Mek.MekMaterialBlock out = new Mek.MekMaterialBlock();
        BinaryReader reader = new BinaryReader(blockBytes);

        List<Mek.MekMaterialBlock.PluginEntry> all = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            all.add(readPluginEntry(reader));
        }

        int extra = reader.readInt();
        if (extra < 0) {
            throw new OperationException(500, "invalid extraRegularCount: " + extra);
        }
        out.setExtraRegularCount(extra);

        for (int i = 0; i < extra; i++) {
            all.add(readPluginEntry(reader));
        }

        while (reader.getPosition() < blockBytes.length) {
            int before = reader.getPosition();
            all.add(readPluginEntry(reader));
            if (reader.getPosition() <= before) {
                break;
            }
        }

        int regular = 7 + extra;
        out.setRegularCount(regular);

        int split = Math.min(regular, all.size());
        List<Mek.MekMaterialBlock.PluginEntry> regularCopy  = new ArrayList<>(all.subList(0, split));
        List<Mek.MekMaterialBlock.PluginEntry> trailingCopy = new ArrayList<>(all.subList(split, all.size()));

        out.setEntries(new ArrayList<>(all));
        out.setRegularEntries(regularCopy);
        out.setTrailingEntries(trailingCopy);

        mek.setMekMaterialBlock(out);
    }

    private static Mek.MekMaterialBlock.PluginEntry readPluginEntry(BinaryReader reader) {
        int start = reader.getPosition();

        Mek.MekMaterialBlock.PluginEntry pluginEntry = new Mek.MekMaterialBlock.PluginEntry();

        int countSpriteGroups = reader.readInt();
        if (countSpriteGroups < 0) {
            throw new OperationException(500, "negative group count A(spriteGroups) at " + start);
        }
        List<int[]> spriteGroups = new ArrayList<>(countSpriteGroups);
        for (int i = 0; i < countSpriteGroups; i++) {
            // diff 存储的是“对数” 每组由若干“<u32,u32>”构成
            int pairCount = reader.readInt();
            if (pairCount < 0) {
                throw new OperationException(500, "negative group pairCount A(spriteGroups) at " + reader.getPosition());
            }
            int[] arr = new int[pairCount * 2];
            for (int k = 0; k < pairCount; k++) {
                arr[k * 2]     = reader.readInt();
                arr[k * 2 + 1] = reader.readInt();
            }
            spriteGroups.add(arr);
        }

        int countSeGroups = reader.readInt();
        if (countSeGroups < 0) {
            throw new OperationException(500, "negative group count B(seGroups) at " + reader.getPosition());
        }
        List<int[]> seGroups = new ArrayList<>(countSeGroups);
        for (int i = 0; i < countSeGroups; i++) {
            int len = reader.readInt();
            if (len < 0) {
                throw new OperationException(500, "negative group len B(seGroups) at " + reader.getPosition());
            }
            int[] arr = new int[len];
            for (int k = 0; k < len; k++) {
                arr[k] = reader.readInt();
            }
            seGroups.add(arr);
        }

        int countVoiceGroups = reader.readInt();
        if (countVoiceGroups < 0) {
            throw new OperationException(500, "negative group count C(voiceGroups) at " + reader.getPosition());
        }
        List<int[]> voiceGroups = new ArrayList<>(countVoiceGroups);
        for (int i = 0; i < countVoiceGroups; i++) {
            int len = reader.readInt();
            if (len < 0) {
                throw new OperationException(500, "negative group len C(voiceGroups) at " + reader.getPosition());
            }
            int[] arr = new int[len];
            for (int k = 0; k < len; k++) {
                arr[k] = reader.readInt();
            }
            voiceGroups.add(arr);
        }

        pluginEntry.setSpriteGroups(spriteGroups);
        pluginEntry.setSeGroups(seGroups);
        pluginEntry.setVoiceGroups(voiceGroups);

        pluginEntry.setOffset(start);
        pluginEntry.setLength(reader.getPosition() - start);
        return pluginEntry;
    }
}
