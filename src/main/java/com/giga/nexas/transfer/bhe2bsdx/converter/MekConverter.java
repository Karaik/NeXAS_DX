package com.giga.nexas.transfer.bhe2bsdx.converter;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.mek.Mek;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * mek 转換：
 * 按区块结构逐一迁移，避免直接 BeanCopy 造成类型污染。
 * BHE 独有字段将被忽略，BSDX 需要的字段会补零或保留默认值。
 */
public class MekConverter {

    private final MekAiConverter aiConverter = new MekAiConverter();
    private final MekVoiceConverter voiceConverter = new MekVoiceConverter();
    private final MekMaterialConverter materialConverter = new MekMaterialConverter();

    public Mek convert(
            com.giga.nexas.dto.bhe.mek.Mek bheMek,
            Map<Integer, Integer> spriteIndexMap,
            int voiceGroupCount
    ) {
        Mek dst = new Mek();
        if (bheMek == null) {
            return dst;
        }

        // 顶层字段
        dst.setFileName(bheMek.getFileName());
        dst.setExtensionName(bheMek.getExtensionName());

        // 1) 头信息（偏移表）
        dst.setMekHead(copyHead(bheMek.getMekHead()));
        // 2) 块大小（用于回写）
        dst.setMekBlocks(copyBlocks(bheMek.getMekBlocks()));
        // 3) 基础属性（字段一致，直接拷贝）
        dst.setMekBasicInfo(copyBasicInfo(bheMek.getMekBasicInfo()));
        // 4) PairBlock（14*8 的未知块）
        dst.setMekPairBlock(copyPairBlock(bheMek.getMekPairBlock()));
        // 5) 武装表（丢弃 BHE 独有字段）
        dst.setMekWeaponInfoMap(copyWeaponInfoMap(bheMek.getMekWeaponInfoMap()));
        // 6) AI（含 BsdxInfoCollection 迁移）
        dst.setMekAiInfoList(aiConverter.convert(bheMek.getMekAiInfoList()));
        // 7) Voice（保持版本号）
        dst.setMekVoiceInfo(voiceConverter.convert(bheMek.getMekVoiceInfo()));
        // 8) Material（SpriteGroup 索引映射）
        dst.setMekMaterialBlock(materialConverter.convert(
                bheMek.getMekMaterialBlock(),
                spriteIndexMap,
                voiceGroupCount
        ));

        return dst;
    }

    private Mek.MekHead copyHead(com.giga.nexas.dto.bhe.mek.Mek.MekHead src) {
        Mek.MekHead dst = new Mek.MekHead();
        if (src == null) {
            return dst;
        }
        dst.setSequence1(src.getSequence1());
        dst.setSequence2(src.getSequence2());
        dst.setSequence3(src.getSequence3());
        dst.setSequence4(src.getSequence4());
        dst.setSequence5(src.getSequence5());
        dst.setSequence6(src.getSequence6());
        return dst;
    }

    private Mek.MekBlocks copyBlocks(com.giga.nexas.dto.bhe.mek.Mek.MekBlocks src) {
        Mek.MekBlocks dst = new Mek.MekBlocks();
        if (src == null) {
            return dst;
        }
        dst.setBodyInfoBlockSize(src.getBodyInfoBlockSize());
        dst.setUnknownInfo1BlockSize(src.getUnknownInfo1BlockSize());
        dst.setWeaponInfoBlockSize(src.getWeaponInfoBlockSize());
        dst.setAiInfoBlockSize(src.getAiInfoBlockSize());
        dst.setVoiceInfoBlockSize(src.getVoiceInfoBlockSize());
        return dst;
    }

    private Mek.MekBasicInfo copyBasicInfo(com.giga.nexas.dto.bhe.mek.Mek.MekBasicInfo src) {
        Mek.MekBasicInfo dst = new Mek.MekBasicInfo();
        if (src == null) {
            return dst;
        }
        // 基础字段一致，使用 BeanCopy 简化
        BeanUtil.copyProperties(src, dst);
        return dst;
    }

    private Mek.MekPairBlock copyPairBlock(com.giga.nexas.dto.bhe.mek.Mek.MekPairBlock src) {
        Mek.MekPairBlock dst = new Mek.MekPairBlock();
        if (src == null || src.getUnkPair() == null) {
            return dst;
        }
        List<Mek.MekPairBlock.Pair> pairs = new ArrayList<>();
        for (com.giga.nexas.dto.bhe.mek.Mek.MekPairBlock.Pair pair : src.getUnkPair()) {
            Mek.MekPairBlock.Pair dstPair = new Mek.MekPairBlock.Pair();
            dstPair.setInt1(pair.getInt1());
            dstPair.setInt2(pair.getInt2());
            pairs.add(dstPair);
        }
        dst.setUnkPair(pairs);
        return dst;
    }

    private Map<Integer, Mek.MekWeaponInfo> copyWeaponInfoMap(
            Map<Integer, com.giga.nexas.dto.bhe.mek.Mek.MekWeaponInfo> srcMap
    ) {
        Map<Integer, Mek.MekWeaponInfo> out = new LinkedHashMap<>();
        if (srcMap == null) {
            return out;
        }
        for (Map.Entry<Integer, com.giga.nexas.dto.bhe.mek.Mek.MekWeaponInfo> entry : srcMap.entrySet()) {
            com.giga.nexas.dto.bhe.mek.Mek.MekWeaponInfo src = entry.getValue();
            Mek.MekWeaponInfo dst = new Mek.MekWeaponInfo();
            if (src != null) {
                // BHE 中多出的字段（如 bheInt/feiFlag）在 BSDX 不存在，直接抛弃
                dst.setOffset(src.getOffset());
                dst.setWeaponName(src.getWeaponName());
                dst.setWeaponSequence(src.getWeaponSequence());
                dst.setWeaponDescription(src.getWeaponDescription());
                dst.setSwitchToMekNo(src.getSwitchToMekNo());
                dst.setWazSequence(src.getWazSequence());
                dst.setForceCrashAmount(src.getForceCrashAmount());
                dst.setHeatMaxConsumption(src.getHeatMaxConsumption());
                dst.setHeatMinConsumption(src.getHeatMinConsumption());
                dst.setUpgradeExp(src.getUpgradeExp());
                dst.setStartPointWhenDemonstrate(src.getStartPointWhenDemonstrate());
                dst.setWeaponCategory(src.getWeaponCategory());
                dst.setWeaponType(src.getWeaponType());
                dst.setMeleeSkillFlag(src.getMeleeSkillFlag());
                dst.setColdWeaponSkillFlag(src.getColdWeaponSkillFlag());
                dst.setMissileSkillFlag(src.getMissileSkillFlag());
                dst.setBulletCategorySkillFlag(src.getBulletCategorySkillFlag());
                dst.setOpticalWeaponSkillFlag(src.getOpticalWeaponSkillFlag());
                dst.setDroneSkillFlag(src.getDroneSkillFlag());
                dst.setExplosiveSkillFlag(src.getExplosiveSkillFlag());
                dst.setDefensiveWeaponSkillFlag(src.getDefensiveWeaponSkillFlag());
                dst.setWeaponIdentifier(src.getWeaponIdentifier());
                dst.setWeaponUnknownProperty19(src.getWeaponUnknownProperty19());
            }
            out.put(entry.getKey(), dst);
        }
        return out;
    }
}
