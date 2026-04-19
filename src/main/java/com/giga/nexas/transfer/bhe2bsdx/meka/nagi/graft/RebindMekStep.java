package com.giga.nexas.transfer.bhe2bsdx.meka.nagi.graft;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEvent;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventAttack;
import com.giga.nexas.dto.bsdx.mek.mekcpu.CCpuEventMove;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.graft.resolve.BheResolvedSeRef;
import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.graft.resolve.BheResourceIndexResolver;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiMekRebindContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;


public class RebindMekStep {

    private final BheInfoCollectionObjectGraphRewriter termRewriter = new BheInfoCollectionObjectGraphRewriter();

    public Mek rebindNagiMek(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            BheCommonProjectileAppendPlan commonProjectileAppendPlan
    ) {
        if (request == null || tsukuyomiPackage == null || grpAppendPlan == null) {
            return null;
        }

        Mek sourceMek = tsukuyomiPackage.getTsukuyomiMek();
        if (sourceMek == null) {
            return null;
        }
        TsukuyomiMekRebindContext context = buildContext(sourceMek, grpAppendPlan);
        BheResourceIndexResolver resolver = new BheResourceIndexResolver(commonProjectileAppendPlan, grpAppendPlan);
        Mek targetMek = createTargetMekShell(context);
        targetMek.setMekHead(rebuildMekHead(context));
        targetMek.setMekBlocks(rebuildMekBlocks(context));
        targetMek.setMekBasicInfo(rebuildMekBasicInfo(context));
        targetMek.setMekPairBlock(rebuildMekPairBlock(context));
        targetMek.setMekWeaponInfoMap(rebuildWeaponInfoMap(context));
        targetMek.setMekAiInfoList(rebuildAiInfoList(context));
        targetMek.setMekVoiceInfo(rebuildMekVoiceInfo(context));
        targetMek.setMekMaterialBlock(rebuildMekMaterialBlock(context, resolver));
        termRewriter.rewrite(targetMek, "nagi MEK " + context.getSourceFileName());
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
        CCpuEvent target = createCpuEventByType(source);
        copyCpuEventBaseFields(source, target);
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

    private Mek.MekMaterialBlock rebuildMekMaterialBlock(
            TsukuyomiMekRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        Mek.MekMaterialBlock source = context.getSourceMek().getMekMaterialBlock();
        if (source == null) {
            return null;
        }

        Mek.MekMaterialBlock target = new Mek.MekMaterialBlock();
        target.setExtraRegularCount(source.getExtraRegularCount());
        target.regularCount = source.regularCount;
        target.setEntries(copyPluginEntries(source.getEntries(), resolver));

        target.setRegularEntries(copyPluginEntries(source.getRegularEntries(), resolver));

        target.setTrailingEntries(copyPluginEntries(source.getTrailingEntries(), resolver));
        return target;
    }

    private List<Mek.MekMaterialBlock.PluginEntry> copyPluginEntries(
            List<Mek.MekMaterialBlock.PluginEntry> source,
            BheResourceIndexResolver resolver
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
            copied.setSpriteGroups(remapSpriteGroups(entry.getSpriteGroups(), resolver));
            copied.setSeGroups(remapSeGroups(entry.getSeGroups(), resolver));
            copied.setVoiceGroups(remapVoiceGroups(entry.getVoiceGroups(), resolver));
            target.add(copied);
        }
        return target;
    }

    private List<int[]> emptyGroupShell(List<int[]> source) {
        List<int[]> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (int i = 0; i < source.size(); i++) {
            target.add(new int[0]);
        }
        return target;
    }

    private List<int[]> remapVoiceGroups(List<int[]> source, BheResourceIndexResolver resolver) {
        List<int[]> target = emptyGroupShell(source);
        if (source == null || resolver == null) {
            return target;
        }
        for (int sourceGroupIndex = 0; sourceGroupIndex < source.size(); sourceGroupIndex++) {
            int[] sourceItems = source.get(sourceGroupIndex);
            if (isEmpty(sourceItems)) {
                continue;
            }
            int targetGroupIndex = resolver.resolveBatVoiceGroupIndex(sourceGroupIndex);
            putItems(target, targetGroupIndex, sourceItems);
        }
        return target;
    }

    private List<int[]> remapSpriteGroups(List<int[]> source, BheResourceIndexResolver resolver) {
        List<int[]> target = emptyGroupShell(source);
        if (source == null || resolver == null) {
            return target;
        }
        for (int sourceGroupIndex = 0; sourceGroupIndex < source.size(); sourceGroupIndex++) {
            int[] sourceItems = source.get(sourceGroupIndex);
            if (isEmpty(sourceItems)) {
                continue;
            }
            int targetGroupIndex = resolver.resolveSpriteGroupIndex(sourceGroupIndex);
            putItems(target, targetGroupIndex, sourceItems);
        }
        return target;
    }

    private List<int[]> remapSeGroups(List<int[]> source, BheResourceIndexResolver resolver) {
        List<int[]> target = emptyGroupShell(source);
        if (source == null || resolver == null) {
            return target;
        }
        for (int sourceGroupIndex = 0; sourceGroupIndex < source.size(); sourceGroupIndex++) {
            int[] sourceItems = source.get(sourceGroupIndex);
            if (isEmpty(sourceItems)) {
                continue;
            }
            for (int sourceItemIndex : sourceItems) {
                BheResolvedSeRef targetRef = resolver.resolveSe(sourceGroupIndex, sourceItemIndex);
                if (targetRef != null) {
                    addItem(target, targetRef.targetGroupIndex(), targetRef.targetItemIndex());
                }
            }
        }
        return target;
    }

    private boolean isEmpty(int[] items) {
        return items == null || items.length == 0;
    }

    private void putItems(List<int[]> groups, int groupIndex, int[] items) {
        ensureGroupCapacity(groups, groupIndex + 1);
        int[] existing = groups.get(groupIndex);
        groups.set(groupIndex, mergeUniqueItems(existing, items));
    }

    private void addItem(List<int[]> groups, int groupIndex, int itemIndex) {
        ensureGroupCapacity(groups, groupIndex + 1);
        int[] existing = groups.get(groupIndex);
        if (existing == null || existing.length == 0) {
            groups.set(groupIndex, new int[]{itemIndex});
            return;
        }
        for (int existingItem : existing) {
            if (existingItem == itemIndex) {
                return;
            }
        }
        int[] expanded = new int[existing.length + 1];
        System.arraycopy(existing, 0, expanded, 0, existing.length);
        expanded[existing.length] = itemIndex;
        groups.set(groupIndex, expanded);
    }

    private void ensureGroupCapacity(List<int[]> groups, int requiredSize) {
        while (groups.size() < requiredSize) {
            groups.add(new int[0]);
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
