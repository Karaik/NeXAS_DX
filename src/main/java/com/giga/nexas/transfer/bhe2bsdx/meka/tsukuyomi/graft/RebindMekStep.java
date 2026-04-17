package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiMekRebindContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

/**
 * 负责重建并回写 Tsukuyomi.mek。
 *
 * <p>这一层不是“在源对象上打补丁”，而是：</p>
 * <ul>
 *     <li>先构建 step6 专用上下文</li>
 *     <li>再创建一个新的目标 Mek 外壳</li>
 *     <li>最后按 Mek.java 的分片顺序，逐片重建目标对象</li>
 * </ul>
 *
 * <p>当前 step6 已明确会做语义修改的只有 {@code MekBasicInfo}：</p>
 * <ul>
 *     <li>{@code wazFileSequence -> 目标 WazaGroup 索引}</li>
 *     <li>{@code spmFileSequence -> 目标 SpriteGroup 索引}</li>
 * </ul>
 *
 * <p>其他分片当前先做“结构级深拷贝”，不强行改它们的内部语义。</p>
 */
public class RebindMekStep {

    public Mek rebindTsukuyomiMek(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiGrpAppendPlan grpAppendPlan
    ) {
        if (request == null || tsukuyomiPackage == null || grpAppendPlan == null) {
            return null;
        }

        Mek sourceMek = tsukuyomiPackage.getTsukuyomiMek();
        if (sourceMek == null) {
            return null;
        }

        // Step 6-1: 先根据源 Mek 和 step4 的 grp 结果，构建本步专用上下文。
        TsukuyomiMekRebindContext context = buildContext(sourceMek, grpAppendPlan);

        // Step 6-2: 创建目标 Mek 外壳，只保留最顶层公共信息。
        Mek targetMek = createTargetMekShell(context);

        // Step 6-3: 重建头部偏移信息。
        targetMek.setMekHead(rebuildMekHead(context));

        // Step 6-4: 重建区块大小信息。
        targetMek.setMekBlocks(rebuildMekBlocks(context));

        // Step 6-5: 重建并真正回写 MekBasicInfo 的顶层外部索引。
        targetMek.setMekBasicInfo(rebuildMekBasicInfo(context));

        // Step 6-6: 重建 pair block。
        targetMek.setMekPairBlock(rebuildMekPairBlock(context));

        // Step 6-7: 重建武装表。
        // 当前只做结构级深拷贝，不修改 weapon 内部的 wazSequence。
        targetMek.setMekWeaponInfoMap(rebuildWeaponInfoMap(context));

        // Step 6-8: 重建 AI 分片。
        // 当前显式深拷贝 CPU 事件，但不改变 AI 逻辑语义。
        targetMek.setMekAiInfoList(rebuildAiInfoList(context));

        // Step 6-9: 重建 Voice 分片。
        // 当前显式深拷贝，但不改 table 里的 groupId 语义。
        targetMek.setMekVoiceInfo(rebuildMekVoiceInfo(context));

        // Step 6-10: 重建 Material 分片。
        // 当前显式深拷贝条目和数组，但不改 sprite/se/voice 组内容的语义。
        targetMek.setMekMaterialBlock(rebuildMekMaterialBlock(context));

        // Step 6-11: 对当前已经确定会改的字段做结果校验。
        validateRebindResult(targetMek, context);
        return targetMek;
    }

    private TsukuyomiMekRebindContext buildContext(Mek sourceMek, TsukuyomiGrpAppendPlan grpAppendPlan) {
        TsukuyomiMekRebindContext context = new TsukuyomiMekRebindContext();
        context.setSourceMek(sourceMek);
        context.setSourceFileName(sourceMek.getFileName());
        context.setSourceWazFileSequence(
                sourceMek.getMekBasicInfo() != null ? sourceMek.getMekBasicInfo().getWazFileSequence() : null
        );
        context.setSourceSpmFileSequence(
                sourceMek.getMekBasicInfo() != null ? sourceMek.getMekBasicInfo().getSpmFileSequence() : null
        );
        context.setTargetWazaGroupIndex(grpAppendPlan.getWazaGroupIndex());
        context.setTargetSpriteGroupIndex(grpAppendPlan.getSpriteGroupIndex());
        context.setTargetBatVoiceGroupIndex(grpAppendPlan.getBatVoiceGroupIndex());

        // MEK 内部也持有 sprite / voice / se 的顶层 group index；
        // 这些字段必须消费 Step 4 的最终映射，不能在 MEK 重建阶段重新猜。
        context.getSourceSpriteGroupIndexToTargetIndex().putAll(grpAppendPlan.getSourceSpriteGroupIndexToTargetIndex());
        context.getSourceBatVoiceGroupIndexToTargetIndex().putAll(grpAppendPlan.getSourceBatVoiceGroupIndexToTargetIndex());
        context.getSourceSeGroupIndexToTargetIndex().putAll(grpAppendPlan.getSourceSeGroupIndexToTargetIndex());
        context.getSourceSeItemIndexToTargetIndexByGroup().putAll(grpAppendPlan.getSourceSeItemIndexToTargetIndexByGroup());
        return context;
    }

    private Mek createTargetMekShell(TsukuyomiMekRebindContext context) {
        Mek targetMek = new Mek();

        if (context.getSourceMek() != null) {
            targetMek.setFileName(context.getSourceMek().getFileName());
            targetMek.setExtensionName(context.getSourceMek().getExtensionName());
        }
        return targetMek;
    }

    private Mek.MekHead rebuildMekHead(TsukuyomiMekRebindContext context) {
        Mek.MekHead source = context.getSourceMek().getMekHead();
        if (source == null) {
            return null;
        }

        Mek.MekHead target = new Mek.MekHead();

        // 这里是纯结构信息，不带索引语义，按字段原样复制。
        target.setSequence1(source.getSequence1());
        target.setSequence2(source.getSequence2());
        target.setSequence3(source.getSequence3());
        target.setSequence4(source.getSequence4());
        target.setSequence5(source.getSequence5());
        target.setSequence6(source.getSequence6());
        return target;
    }

    private Mek.MekBlocks rebuildMekBlocks(TsukuyomiMekRebindContext context) {
        Mek.MekBlocks source = context.getSourceMek().getMekBlocks();
        if (source == null) {
            return null;
        }

        Mek.MekBlocks target = new Mek.MekBlocks();

        // 这里也是纯结构尺寸信息，当前不改语义。
        target.setBodyInfoBlockSize(source.getBodyInfoBlockSize());
        target.setUnknownInfo1BlockSize(source.getUnknownInfo1BlockSize());
        target.setWeaponInfoBlockSize(source.getWeaponInfoBlockSize());
        target.setAiInfoBlockSize(source.getAiInfoBlockSize());
        target.setVoiceInfoBlockSize(source.getVoiceInfoBlockSize());
        return target;
    }

    private Mek.MekBasicInfo rebuildMekBasicInfo(TsukuyomiMekRebindContext context) {
        Mek.MekBasicInfo source = context.getSourceMek().getMekBasicInfo();
        if (source == null) {
            return null;
        }

        Mek.MekBasicInfo target = new Mek.MekBasicInfo();

        // 先复制所有普通属性字段。
        target.setMekName(source.getMekName());
        target.setMekNameEnglish(source.getMekNameEnglish());
        target.setPilotNameKanji(source.getPilotNameKanji());
        target.setPilotNameRoma(source.getPilotNameRoma());
        target.setMekDescription(source.getMekDescription());
        target.setMekType(source.getMekType());
        target.setHealthRecovery(source.getHealthRecovery());
        target.setForceOnKill(source.getForceOnKill());
        target.setBaseHealth(source.getBaseHealth());
        target.setEnergyIncreaseLevel1(source.getEnergyIncreaseLevel1());
        target.setEnergyIncreaseLevel2(source.getEnergyIncreaseLevel2());
        target.setBoosterLevel(source.getBoosterLevel());
        target.setBoosterIncreaseLevel(source.getBoosterIncreaseLevel());
        target.setPermanentArmor(source.getPermanentArmor());
        target.setComboImpactFactor(source.getComboImpactFactor());
        target.setFightingAbility(source.getFightingAbility());
        target.setShootingAbility(source.getShootingAbility());
        target.setDurability(source.getDurability());
        target.setMobility(source.getMobility());
        target.setPhysicsWeight(source.getPhysicsWeight());
        target.setWalkingSpeed(source.getWalkingSpeed());
        target.setNormalDashSpeed(source.getNormalDashSpeed());
        target.setSearchDashSpeed(source.getSearchDashSpeed());
        target.setBoostDashSpeed(source.getBoostDashSpeed());
        target.setAutoHoverHeight(source.getAutoHoverHeight());

        // 这里是 step6 当前第一轮真正有语义改动的两个字段。
        target.setWazFileSequence(context.getTargetWazaGroupIndex());
        target.setSpmFileSequence(context.getTargetSpriteGroupIndex());
        return target;
    }

    private Mek.MekPairBlock rebuildMekPairBlock(TsukuyomiMekRebindContext context) {
        Mek.MekPairBlock source = context.getSourceMek().getMekPairBlock();
        if (source == null) {
            return null;
        }

        Mek.MekPairBlock target = new Mek.MekPairBlock();
        List<Mek.MekPairBlock.Pair> pairs = new ArrayList<>();

        if (source.getUnkPair() != null) {
            for (Mek.MekPairBlock.Pair pair : source.getUnkPair()) {
                if (pair == null) {
                    pairs.add(null);
                    continue;
                }

                Mek.MekPairBlock.Pair copied = new Mek.MekPairBlock.Pair();
                copied.setInt1(pair.getInt1());
                copied.setInt2(pair.getInt2());
                pairs.add(copied);
            }
        }

        target.setUnkPair(pairs);
        return target;
    }

    private Map<Integer, Mek.MekWeaponInfo> rebuildWeaponInfoMap(TsukuyomiMekRebindContext context) {
        Map<Integer, Mek.MekWeaponInfo> source = context.getSourceMek().getMekWeaponInfoMap();
        Map<Integer, Mek.MekWeaponInfo> target = new LinkedHashMap<>();
        if (source == null) {
            return target;
        }

        // 当前只做武装对象的结构级深拷贝。
        // MekWeaponInfo.wazSequence 仍然解释为 Tsukuyomi.waz 内部 skill 索引，不在 step6 修改。
        for (Map.Entry<Integer, Mek.MekWeaponInfo> entry : source.entrySet()) {
            Mek.MekWeaponInfo sourceWeapon = entry.getValue();
            if (sourceWeapon == null) {
                target.put(entry.getKey(), null);
                continue;
            }

            Mek.MekWeaponInfo copied = new Mek.MekWeaponInfo();
            copied.offset = sourceWeapon.offset;
            copied.setWeaponName(sourceWeapon.getWeaponName());
            copied.setWeaponSequence(sourceWeapon.getWeaponSequence());
            copied.setWeaponDescription(sourceWeapon.getWeaponDescription());
            copied.setSwitchToMekNo(sourceWeapon.getSwitchToMekNo());
            copied.setWazSequence(sourceWeapon.getWazSequence());
            copied.setForceCrashAmount(sourceWeapon.getForceCrashAmount());
            copied.setHeatMaxConsumption(sourceWeapon.getHeatMaxConsumption());
            copied.setHeatMinConsumption(sourceWeapon.getHeatMinConsumption());
            copied.setUpgradeExp(sourceWeapon.getUpgradeExp());
            copied.setStartPointWhenDemonstrate(sourceWeapon.getStartPointWhenDemonstrate());
            copied.setWeaponCategory(sourceWeapon.getWeaponCategory());
            copied.setWeaponType(sourceWeapon.getWeaponType());
            copied.setMeleeSkillFlag(sourceWeapon.getMeleeSkillFlag());
            copied.setColdWeaponSkillFlag(sourceWeapon.getColdWeaponSkillFlag());
            copied.setMissileSkillFlag(sourceWeapon.getMissileSkillFlag());
            copied.setBulletCategorySkillFlag(sourceWeapon.getBulletCategorySkillFlag());
            copied.setOpticalWeaponSkillFlag(sourceWeapon.getOpticalWeaponSkillFlag());
            copied.setDroneSkillFlag(sourceWeapon.getDroneSkillFlag());
            copied.setExplosiveSkillFlag(sourceWeapon.getExplosiveSkillFlag());
            copied.setDefensiveWeaponSkillFlag(sourceWeapon.getDefensiveWeaponSkillFlag());
            copied.setWeaponIdentifier(sourceWeapon.getWeaponIdentifier());
            copied.setWeaponUnknownProperty19(sourceWeapon.getWeaponUnknownProperty19());
            target.put(entry.getKey(), copied);
        }
        return target;
    }

    private List<Mek.MekAiInfo> rebuildAiInfoList(TsukuyomiMekRebindContext context) {
        List<Mek.MekAiInfo> source = context.getSourceMek().getMekAiInfoList();
        List<Mek.MekAiInfo> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        // 当前 AI 分片也改成显式深拷贝。
        // 但这里只做“保留原语义”，不改变 CPU 事件内部逻辑。
        for (Mek.MekAiInfo sourceAiInfo : source) {
            if (sourceAiInfo == null) {
                target.add(null);
                continue;
            }

            Mek.MekAiInfo copiedAiInfo = new Mek.MekAiInfo();
            copiedAiInfo.setAiTypeJapanese(sourceAiInfo.getAiTypeJapanese());
            copiedAiInfo.setAiTypeEnglish(sourceAiInfo.getAiTypeEnglish());
            copiedAiInfo.setCpuEventList(copyCpuEventList(sourceAiInfo.getCpuEventList()));
            target.add(copiedAiInfo);
        }
        return target;
    }

    private List<CCpuEvent> copyCpuEventList(List<CCpuEvent> source) {
        List<CCpuEvent> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (CCpuEvent event : source) {
            target.add(copyCpuEvent(event));
        }
        return target;
    }

    private CCpuEvent copyCpuEvent(CCpuEvent source) {
        if (source == null) {
            return null;
        }

        // 先根据运行时真实类型创建目标事件对象。
        CCpuEvent target = createCpuEventByType(source);

        // 先复制 CCpuEvent 共有字段。
        copyCpuEventBaseFields(source, target);

        // 再复制子类特有字段。
        if (source instanceof CCpuEventMove sourceMove && target instanceof CCpuEventMove targetMove) {
            copyCpuEventMoveFields(sourceMove, targetMove);
        } else if (source instanceof CCpuEventAttack sourceAttack && target instanceof CCpuEventAttack targetAttack) {
            copyCpuEventAttackFields(sourceAttack, targetAttack);
        }

        return target;
    }

    private CCpuEvent createCpuEventByType(CCpuEvent source) {
        if (source instanceof CCpuEventMove) {
            return new CCpuEventMove();
        }
        if (source instanceof CCpuEventAttack) {
            return new CCpuEventAttack();
        }
        return new CCpuEvent();
    }

    private void copyCpuEventBaseFields(CCpuEvent source, CCpuEvent target) {
        target.setType(source.getType());
        target.setShort1(source.getShort1());
        target.setInt1(source.getInt1());
        target.setInt2(source.getInt2());
        target.setActivationProbability(source.getActivationProbability());
        target.setActivationProbabilityWhenCounter(source.getActivationProbabilityWhenCounter());
        target.setInt5(source.getInt5());
        target.setActivationRangeMin(source.getActivationRangeMin());
        target.setActivationRangeMax(source.getActivationRangeMax());
        target.setActivationAngleRangeMin(source.getActivationAngleRangeMin());
        target.setActivationAngleRangeMax(source.getActivationAngleRangeMax());
        target.setActivationAltitudeMin(source.getActivationAltitudeMin());
        target.setActivationAltitudeMax(source.getActivationAltitudeMax());
        target.setActivationDurabilityMinPercentage(source.getActivationDurabilityMinPercentage());
        target.setActivationDurabilityMaxPercentage(source.getActivationDurabilityMaxPercentage());
        target.setInt14(source.getInt14());
        target.setInt15(source.getInt15());
        target.setActivationHeatMin(source.getActivationHeatMin());
        target.setActivationHeatMax(source.getActivationHeatMax());
        target.setInt18(source.getInt18());
        target.setInt19(source.getInt19());
        target.setInt20(source.getInt20());
        target.setInt21(source.getInt21());
        target.setInt22(source.getInt22());
        target.setInt23(source.getInt23());
        target.setInt24(source.getInt24());
        target.setInt25(source.getInt25());
        target.setInt26(source.getInt26());
        target.setShort2(source.getShort2());
        target.setInt27(source.getInt27());
        target.setInt28(source.getInt28());
        target.setShort3(source.getShort3());
        target.setShort4(source.getShort4());
        target.setBsdxInfoCollectionList(copyBsdxInfoCollections(source.getBsdxInfoCollectionList()));
    }

    private void copyCpuEventMoveFields(CCpuEventMove source, CCpuEventMove target) {
        target.setMoveType(source.getMoveType());
        target.setMoveSpeed(source.getMoveSpeed());
        target.setMoveInertia(source.getMoveInertia());
        target.setMoveTargetType(source.getMoveTargetType());
        target.setMoveTargetAngleCorrection(source.getMoveTargetAngleCorrection());
        target.setViewpointType(source.getViewpointType());
        target.setViewpointAngleCorrection(source.getViewpointAngleCorrection());
        target.setJumpType(source.getJumpType());
        target.setAscentVar1(source.getAscentVar1());
        target.setAscentVar2(source.getAscentVar2());
        target.setGenericFlag(source.getGenericFlag());
        target.setAttackProbabilityCorrection(source.getAttackProbabilityCorrection());
    }

    private void copyCpuEventAttackFields(CCpuEventAttack source, CCpuEventAttack target) {
        target.wazaName = source.wazaName;
        target.setMekWeaponInfoMapNo(source.getMekWeaponInfoMapNo());
    }

    private List<BsdxInfoCollection> copyBsdxInfoCollections(List<BsdxInfoCollection> source) {
        List<BsdxInfoCollection> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (BsdxInfoCollection collection : source) {
            if (collection == null) {
                target.add(null);
                continue;
            }
            BsdxInfoCollection copied = new BsdxInfoCollection();
            copied.setInt1(collection.getInt1());
            copied.setTypeList(collection.getTypeList() == null ? new ArrayList<>() : new ArrayList<>(collection.getTypeList()));
            copied.setParamList(collection.getParamList() == null ? new ArrayList<>() : new ArrayList<>(collection.getParamList()));
            copied.setIntList3(collection.getIntList3() == null ? new ArrayList<>() : new ArrayList<>(collection.getIntList3()));
            copied.setIntList4(collection.getIntList4() == null ? new ArrayList<>() : new ArrayList<>(collection.getIntList4()));
            copied.setInt2(collection.getInt2());
            target.add(copied);
        }
        return target;
    }

    private Mek.MekVoiceInfo rebuildMekVoiceInfo(TsukuyomiMekRebindContext context) {
        Mek.MekVoiceInfo source = context.getSourceMek().getMekVoiceInfo();
        if (source == null) {
            return null;
        }

        Mek.MekVoiceInfo target = new Mek.MekVoiceInfo();
        target.setVersion(remapBatVoiceGroupIndex(source.getVersion(), context));
        target.builtinEmotionCount = source.builtinEmotionCount;

        // emotions 当前做结构级深拷贝。
        List<Mek.MekVoiceInfo.Emotion> emotions = new ArrayList<>();
        if (source.getEmotions() != null) {
            for (Mek.MekVoiceInfo.Emotion emotion : source.getEmotions()) {
                if (emotion == null) {
                    emotions.add(null);
                    continue;
                }
                Mek.MekVoiceInfo.Emotion copied = new Mek.MekVoiceInfo.Emotion();
                copied.setName(emotion.getName());
                copied.setToken(emotion.getToken());
                emotions.add(copied);
            }
        }
        target.setEmotions(emotions);

        // voiceSlots 当前做结构级深拷贝。
        List<Mek.MekVoiceInfo.VoiceSlot> voiceSlots = new ArrayList<>();
        if (source.getVoiceSlots() != null) {
            for (Mek.MekVoiceInfo.VoiceSlot voiceSlot : source.getVoiceSlots()) {
                if (voiceSlot == null) {
                    voiceSlots.add(null);
                    continue;
                }
                Mek.MekVoiceInfo.VoiceSlot copied = new Mek.MekVoiceInfo.VoiceSlot();
                copied.setName(voiceSlot.getName());
                copied.setToken(voiceSlot.getToken());
                voiceSlots.add(copied);
            }
        }
        target.setVoiceSlots(voiceSlots);

        // table 当前也显式重建容器，但不修改 Entry.groupId。
        // 这里的 groupId 在运行时表现为默认 BatVoice group 内的 item index，不是顶层 BatVoiceGroup index。
        List<List<List<Mek.MekVoiceInfo.Entry>>> table = new ArrayList<>();
        if (source.getTable() != null) {
            for (List<List<Mek.MekVoiceInfo.Entry>> row : source.getTable()) {
                if (row == null) {
                    table.add(null);
                    continue;
                }
                List<List<Mek.MekVoiceInfo.Entry>> copiedRow = new ArrayList<>();
                for (List<Mek.MekVoiceInfo.Entry> cell : row) {
                    if (cell == null) {
                        copiedRow.add(null);
                        continue;
                    }
                    List<Mek.MekVoiceInfo.Entry> copiedCell = new ArrayList<>();
                    for (Mek.MekVoiceInfo.Entry entry : cell) {
                        if (entry == null) {
                            copiedCell.add(null);
                            continue;
                        }
                        Mek.MekVoiceInfo.Entry copiedEntry = new Mek.MekVoiceInfo.Entry();
                        copiedEntry.setVoiceType(entry.getVoiceType());
                        copiedEntry.setGroupId(entry.getGroupId());
                        copiedEntry.setWeight(entry.getWeight());
                        copiedCell.add(copiedEntry);
                    }
                    copiedRow.add(copiedCell);
                }
                table.add(copiedRow);
            }
        }
        target.setTable(table);
        return target;
    }

    private Integer remapBatVoiceGroupIndex(Integer sourceGroupIndex, TsukuyomiMekRebindContext context) {
        if (sourceGroupIndex == null
                || context == null
                || context.getSourceBatVoiceGroupIndexToTargetIndex() == null
                || context.getSourceBatVoiceGroupIndexToTargetIndex().isEmpty()) {
            return sourceGroupIndex;
        }
        return context.getSourceBatVoiceGroupIndexToTargetIndex().getOrDefault(sourceGroupIndex, sourceGroupIndex);
    }

    private Mek.MekMaterialBlock rebuildMekMaterialBlock(TsukuyomiMekRebindContext context) {
        Mek.MekMaterialBlock source = context.getSourceMek().getMekMaterialBlock();
        if (source == null) {
            return null;
        }

        Mek.MekMaterialBlock target = new Mek.MekMaterialBlock();
        target.setExtraRegularCount(source.getExtraRegularCount());
        target.regularCount = source.regularCount;

        // CMaterial 的 entries / regularEntries / trailingEntries 都可能含有外层 group index。
        // 统一走 copyPluginEntries，避免只修其中一段导致 confirm 或战斗初始化再错位。
        target.setEntries(copyPluginEntries(source.getEntries(), context));

        // regularEntries 当前显式重建 PluginEntry 容器。
        target.setRegularEntries(copyPluginEntries(source.getRegularEntries(), context));

        // trailingEntries 当前显式重建 PluginEntry 容器。
        target.setTrailingEntries(copyPluginEntries(source.getTrailingEntries(), context));
        return target;
    }

    private List<Mek.MekMaterialBlock.PluginEntry> copyPluginEntries(
            List<Mek.MekMaterialBlock.PluginEntry> source,
            TsukuyomiMekRebindContext context
    ) {
        List<Mek.MekMaterialBlock.PluginEntry> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (Mek.MekMaterialBlock.PluginEntry entry : source) {
            if (entry == null) {
                target.add(null);
                continue;
            }

            Mek.MekMaterialBlock.PluginEntry copied = new Mek.MekMaterialBlock.PluginEntry();
            copied.offset = entry.offset;
            copied.length = entry.length;

            // 只改外层 group index 和确认可映射的 SE item；
            // sprite/voice 组内 payload 的语义由引擎解释，不能在这里按文件名盲改。
            copied.setSpriteGroups(remapSpriteGroups(entry.getSpriteGroups(), context));
            copied.setSeGroups(remapSeGroups(entry.getSeGroups(), context));
            copied.setVoiceGroups(remapVoiceGroups(entry.getVoiceGroups(), context));
            target.add(copied);
        }
        return target;
    }

    private List<int[]> copyIntArrayGroups(List<int[]> source) {
        List<int[]> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (int[] arr : source) {
            target.add(arr == null ? null : arr.clone());
        }
        return target;
    }

    private List<int[]> remapVoiceGroups(List<int[]> source, TsukuyomiMekRebindContext context) {
        return remapIndexedGroups(source, context, context == null ? null : context.getSourceBatVoiceGroupIndexToTargetIndex(), null);
    }

    private List<int[]> remapSpriteGroups(List<int[]> source, TsukuyomiMekRebindContext context) {
        return remapIndexedGroups(source, context, context == null ? null : context.getSourceSpriteGroupIndexToTargetIndex(), null);
    }

    private List<int[]> remapSeGroups(List<int[]> source, TsukuyomiMekRebindContext context) {
        return remapIndexedGroups(
                source,
                context,
                context == null ? null : context.getSourceSeGroupIndexToTargetIndex(),
                context == null ? null : context.getSourceSeItemIndexToTargetIndexByGroup()
        );
    }

    private List<int[]> remapIndexedGroups(
            List<int[]> source,
            TsukuyomiMekRebindContext context,
            Map<Integer, Integer> sourceGroupIndexToTargetIndex,
            Map<Integer, Map<Integer, Integer>> sourceItemIndexToTargetIndexByGroup
    ) {
        List<int[]> copied = copyIntArrayGroups(source);
        if (copied.isEmpty()
                || context == null
                || sourceGroupIndexToTargetIndex == null
                || sourceGroupIndexToTargetIndex.isEmpty()) {
            return copied;
        }

        ensureGroupCapacity(copied, requiredGroupCapacity(copied, sourceGroupIndexToTargetIndex));

        for (Map.Entry<Integer, Integer> mapping : sourceGroupIndexToTargetIndex.entrySet()) {
            Integer sourceGroupIndex = mapping.getKey();
            Integer targetGroupIndex = mapping.getValue();
            if (sourceGroupIndex == null || targetGroupIndex == null || sourceGroupIndex < 0 || targetGroupIndex < 0) {
                continue;
            }
            if (sourceGroupIndex >= copied.size()) {
                continue;
            }

            int[] sourceItems = copied.get(sourceGroupIndex);
            if (sourceItems == null || sourceItems.length == 0) {
                continue;
            }

            // SE 有明确的 source item -> target item 映射；sprite/voice 目前只搬外层 group，items 原样保留。
            int[] remappedItems = remapGroupItems(
                    sourceItems,
                    sourceItemIndexToTargetIndexByGroup == null ? null : sourceItemIndexToTargetIndexByGroup.get(sourceGroupIndex)
            );
            if (sourceGroupIndex.equals(targetGroupIndex)) {
                copied.set(targetGroupIndex, remappedItems);
                continue;
            }

            int[] targetItems = targetGroupIndex < copied.size() ? copied.get(targetGroupIndex) : null;
            copied.set(targetGroupIndex, mergeUniqueItems(targetItems, remappedItems));
            copied.set(sourceGroupIndex, null);
        }
        return copied;
    }

    private int requiredGroupCapacity(List<int[]> groups, Map<Integer, Integer> sourceToTargetGroupIndex) {
        int required = groups == null ? 0 : groups.size();
        if (sourceToTargetGroupIndex == null) {
            return required;
        }
        for (Map.Entry<Integer, Integer> entry : sourceToTargetGroupIndex.entrySet()) {
            if (entry.getKey() != null) {
                required = Math.max(required, entry.getKey() + 1);
            }
            if (entry.getValue() != null) {
                required = Math.max(required, entry.getValue() + 1);
            }
        }
        return required;
    }

    private void ensureGroupCapacity(List<int[]> groups, int requiredSize) {
        while (groups.size() < requiredSize) {
            groups.add(null);
        }
    }

    private int[] mergeUniqueItems(int[] existing, int[] incoming) {
        if (existing == null || existing.length == 0) {
            return incoming == null ? null : incoming.clone();
        }
        if (incoming == null || incoming.length == 0) {
            return existing.clone();
        }

        LinkedHashSet<Integer> merged = new LinkedHashSet<>();
        for (int value : existing) {
            merged.add(value);
        }
        for (int value : incoming) {
            merged.add(value);
        }

        int[] result = new int[merged.size()];
        int cursor = 0;
        for (Integer value : merged) {
            result[cursor++] = value;
        }
        return result;
    }

    private int[] remapGroupItems(int[] sourceItems, Map<Integer, Integer> sourceItemIndexToTargetIndex) {
        if (sourceItems == null) {
            return null;
        }
        if (sourceItemIndexToTargetIndex == null || sourceItemIndexToTargetIndex.isEmpty()) {
            return sourceItems.clone();
        }

        int[] remapped = new int[sourceItems.length];
        for (int i = 0; i < sourceItems.length; i++) {
            remapped[i] = sourceItemIndexToTargetIndex.getOrDefault(sourceItems[i], sourceItems[i]);
        }
        return remapped;
    }

    private void validateRebindResult(Mek targetMek, TsukuyomiMekRebindContext context) {
        if (targetMek == null || targetMek.getMekBasicInfo() == null) {
            throw new IllegalStateException("step6 重建后的 MekBasicInfo 不能为空");
        }
        if (!Integer.valueOf(context.getTargetWazaGroupIndex()).equals(targetMek.getMekBasicInfo().getWazFileSequence())) {
            throw new IllegalStateException("step6 wazFileSequence 回写失败");
        }
        if (!Integer.valueOf(context.getTargetSpriteGroupIndex()).equals(targetMek.getMekBasicInfo().getSpmFileSequence())) {
            throw new IllegalStateException("step6 spmFileSequence 回写失败");
        }
    }
}
