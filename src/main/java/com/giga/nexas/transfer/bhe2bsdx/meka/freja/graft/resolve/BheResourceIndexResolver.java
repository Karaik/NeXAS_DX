package com.giga.nexas.transfer.bhe2bsdx.meka.freja.graft.resolve;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

import java.util.Map;


public class BheResourceIndexResolver {

    private final BheCommonProjectileAppendPlan commonPlan;
    private final TsukuyomiGrpAppendPlan privatePlan;

    public BheResourceIndexResolver(
            BheCommonProjectileAppendPlan commonPlan,
            TsukuyomiGrpAppendPlan privatePlan
    ) {
        this.commonPlan = commonPlan;
        this.privatePlan = privatePlan;
    }

    public boolean isCommonWazIndex(Integer sourceIndex) {
        return sourceIndex != null
                && commonPlan != null
                && commonPlan.getSourceWazIndexToTargetIndex().containsKey(sourceIndex);
    }

    public boolean isCommonSpriteIndex(Integer sourceIndex) {
        return sourceIndex != null
                && commonPlan != null
                && commonPlan.getSourceSpriteIndexToTargetIndex().containsKey(sourceIndex);
    }

    public boolean isCommonSePair(Integer sourceGroupIndex, Integer sourceItemIndex) {
        if (sourceGroupIndex == null || sourceItemIndex == null || commonPlan == null) {
            return false;
        }
        return commonPlan.getSourceSePairToTargetItemIndex()
                .containsKey(BheCommonProjectileAppendPlan.sePairKey(sourceGroupIndex, sourceItemIndex));
    }

    public Integer resolveWazGroupIndex(Integer sourceIndex) {
        if (sourceIndex == null || sourceIndex < 0) {
            return sourceIndex;
        }
        if (isCommonWazIndex(sourceIndex)) {
            return commonPlan.getSourceWazIndexToTargetIndex().get(sourceIndex);
        }
        Integer targetIndex = privatePlan == null ? null : privatePlan.getSourceWazGroupIndexToTargetIndex().get(sourceIndex);
        if (targetIndex == null) {
            throw new IllegalStateException("找不到源 WazaGroup 目标索引: " + sourceIndex);
        }
        return targetIndex;
    }

    public Integer resolveWazSkillIndex(Integer sourceWazIndex, Integer sourceSkillIndex) {
        if (sourceWazIndex == null || sourceWazIndex < 0 || sourceSkillIndex == null || sourceSkillIndex < 0) {
            return sourceSkillIndex;
        }
        if (isCommonWazIndex(sourceWazIndex)) {
            Integer skillBase = commonPlan.getSourceWazIndexToTargetSkillBase().get(sourceWazIndex);
            return skillBase == null ? sourceSkillIndex : skillBase + sourceSkillIndex;
        }
        Map<Integer, Integer> skillMap = privatePlan == null
                ? null
                : privatePlan.getSourceWazSkillIndexToTargetIndexByGroup().get(sourceWazIndex);
        if (skillMap == null) {
            return sourceSkillIndex;
        }
        return skillMap.getOrDefault(sourceSkillIndex, sourceSkillIndex);
    }

    public Integer resolveSpriteGroupIndex(Integer sourceIndex) {
        if (sourceIndex == null || sourceIndex < 0) {
            return sourceIndex;
        }
        if (isCommonSpriteIndex(sourceIndex)) {
            return commonPlan.getSourceSpriteIndexToTargetIndex().get(sourceIndex);
        }
        Integer targetIndex = privatePlan == null ? null : privatePlan.getSourceSpriteGroupIndexToTargetIndex().get(sourceIndex);
        if (targetIndex == null) {
            throw new IllegalStateException("找不到源 SpriteGroup 目标索引: " + sourceIndex);
        }
        return targetIndex;
    }

    public Integer resolveSpriteActionGroupIndex(Integer sourceSpriteIndex, Integer sourceActionGroupIndex) {
        if (sourceSpriteIndex == null || sourceSpriteIndex < 0
                || sourceActionGroupIndex == null || sourceActionGroupIndex < 0) {
            return sourceActionGroupIndex;
        }
        if (isCommonSpriteIndex(sourceSpriteIndex)) {
            Integer actionBase = commonPlan.getSourceSpriteIndexToTargetActionGroupBase().get(sourceSpriteIndex);
            return actionBase == null ? sourceActionGroupIndex : actionBase + sourceActionGroupIndex;
        }
        return sourceActionGroupIndex;
    }

    public BheResolvedSeRef resolveSe(Integer sourceGroupIndex, Integer sourceItemIndex) {
        if (sourceGroupIndex == null || sourceItemIndex == null || sourceGroupIndex < 0 || sourceItemIndex < 0) {
            return null;
        }
        if (isCommonSePair(sourceGroupIndex, sourceItemIndex)) {
            String key = BheCommonProjectileAppendPlan.sePairKey(sourceGroupIndex, sourceItemIndex);
            Integer targetItemIndex = commonPlan.getSourceSePairToTargetItemIndex().get(key);
            return new BheResolvedSeRef(commonPlan.getCommonProjectileSeGroupIndex(), targetItemIndex, true);
        }

        Integer targetGroupIndex = privatePlan == null
                ? null
                : privatePlan.getSourceSeGroupIndexToTargetIndex().get(sourceGroupIndex);
        Map<Integer, Integer> itemMap = privatePlan == null
                ? null
                : privatePlan.getSourceSeItemIndexToTargetIndexByGroup().get(sourceGroupIndex);
        Integer targetItemIndex = itemMap == null ? null : itemMap.get(sourceItemIndex);
        if (targetGroupIndex == null || targetItemIndex == null) {
            throw new IllegalStateException(
                    "找不到源 SE 目标索引: group=" + sourceGroupIndex + ", item=" + sourceItemIndex
            );
        }
        return new BheResolvedSeRef(targetGroupIndex, targetItemIndex, false);
    }

    public Integer resolveBatVoiceGroupIndex(Integer sourceIndex) {
        if (sourceIndex == null || sourceIndex < 0) {
            return sourceIndex;
        }
        Integer targetIndex = privatePlan == null ? null : privatePlan.getSourceBatVoiceGroupIndexToTargetIndex().get(sourceIndex);
        if (targetIndex == null) {
            throw new IllegalStateException("找不到源 BatVoiceGroup 目标索引: " + sourceIndex);
        }
        return targetIndex;
    }
}
