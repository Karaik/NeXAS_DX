package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.resolve;

import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

import java.util.Map;

/**
 * BHE 单机体 graft 的资源索引解析器。
 *
 * <p>这里统一封装“公共资源优先、单机体私有资源兜底”的协议。公共资源来自
 * {@link BheCommonProjectileAppendPlan}，属于 preparedBaseline 既成事实；私有资源来自
 * {@link TsukuyomiGrpAppendPlan}，属于本次 selected graft 产物。不要把两张映射表合并，
 * 否则调用方会误以为公共资源也是本次单机体 closure 生成的资源。</p>
 */
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
            return sourceSkillIndex;
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
