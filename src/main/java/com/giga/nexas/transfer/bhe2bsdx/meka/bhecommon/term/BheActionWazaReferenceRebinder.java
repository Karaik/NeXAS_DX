package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventTerm;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/** ACTION_WAZA的两个参数是所选对象的绝对WAZ组/技能，不是普通比较整数。 */
public final class BheActionWazaReferenceRebinder {
    private BheActionWazaReferenceRebinder() { }

    /** 只接受刚从BHE转换并隔离复制、已经重编译Term语法的对象图。 */
    public static void rebindSourceCopy(Object root, BheCommonProjectileAppendPlan common,
                                        TsukuyomiGrpAppendPlan allocated) {
        visit(root, false, common, allocated, new IdentityHashMap<>());
    }

    /** 私有角色稍后才分配组号；公共源参数保持原样，由每轮输出副本消费累计映射。 */
    public static void rememberPrivateTargets(BheCommonProjectileAppendPlan common, TsukuyomiGrpAppendPlan allocated) {
        if (common == null || allocated == null) return;
        Map<Integer, Integer> groups = new LinkedHashMap<>(common.getSourcePrivateWazIndexToTargetIndex());
        Map<Integer, Map<Integer, Integer>> skills = new LinkedHashMap<>();
        common.getSourcePrivateWazSkillIndexToTargetIndexByGroup().forEach((group, mapping) ->
                skills.put(group, new LinkedHashMap<>(mapping)));
        for (var row : allocated.getSourceWazGroupIndexToTargetIndex().entrySet()) {
            Integer group = row.getKey(), target = row.getValue();
            Map<Integer, Integer> mapping = allocated.getSourceWazSkillIndexToTargetIndexByGroup().get(group);
            if (group == null || group < 0 || target == null || target < 0 || mapping == null) continue;
            Integer previous = groups.putIfAbsent(group, target);
            if (previous != null && !previous.equals(target))
                throw new IllegalStateException("ACTION_WAZA私有组映射冲突: " + group);
            Map<Integer, Integer> staged = skills.computeIfAbsent(group, key -> new LinkedHashMap<>());
            for (var skill : mapping.entrySet()) {
                if (skill.getKey() == null || skill.getKey() < 0 || skill.getValue() == null || skill.getValue() < 0) continue;
                Integer old = staged.putIfAbsent(skill.getKey(), skill.getValue());
                if (old != null && !old.equals(skill.getValue()))
                    throw new IllegalStateException("ACTION_WAZA私有技能映射冲突: " + group + ":" + skill.getKey());
            }
        }
        common.setSourcePrivateWazIndexToTargetIndex(groups);
        common.setSourcePrivateWazSkillIndexToTargetIndexByGroup(skills);
    }

    /** 只处理原BHE公共追加区间；继承DX前缀和未登记尾段仍在其原索引域。 */
    public static void rebindAppendedOutputCopy(Waz output, String fileName, BheCommonProjectileAppendPlan common) {
        if (output == null || common == null || output.getSkillList() == null) return;
        Set<Integer> visitedSkills = new HashSet<>();
        for (var row : common.getSourceWazIndexToTargetFileName().entrySet()) {
            if (row.getValue() == null || !row.getValue().equalsIgnoreCase(fileName)) continue;
            Integer base = common.getSourceWazIndexToTargetSkillBase().get(row.getKey());
            Integer count = common.getSourceWazIndexToTargetSkillCount().get(row.getKey());
            if (base == null || count == null || base < 0 || count < 0
                    || (long) base + count > output.getSkillList().size())
                throw new IllegalStateException("ACTION_WAZA公共源区间无效: " + fileName + ":" + row.getKey());
            for (int skill = base; skill < base + count; skill++) {
                if (!visitedSkills.add(skill)) throw new IllegalStateException("ACTION_WAZA公共源区间重叠: " + fileName);
                rebindSourceCopy(output.getSkillList().get(skill), common, null);
            }
        }
    }

    private static void visit(Object value, boolean conditionConsumer, BheCommonProjectileAppendPlan common,
                              TsukuyomiGrpAppendPlan allocated, IdentityHashMap<Object, Boolean> seen) {
        if (value == null) return;
        if (value instanceof BsdxInfoCollection collection) {
            if (conditionConsumer && seen.put(value, true) == null) rebind(collection, common, allocated);
            return;
        }
        if (seen.put(value, true) != null) return;
        if (value instanceof List<?> list) {
            list.forEach(item -> visit(item, conditionConsumer, common, allocated, seen)); return;
        }
        if (value instanceof Map<?, ?> map) {
            map.values().forEach(item -> visit(item, conditionConsumer, common, allocated, seen)); return;
        }
        if (!value.getClass().getName().startsWith("com.giga.nexas.")) return;
        for (Class<?> type = value.getClass(); type != Object.class; type = type.getSuperclass()) {
            for (Field field : type.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) continue;
                boolean consumer = conditionConsumer;
                if (value instanceof CEventChange) consumer = field.getName().equals("bsdxInfoCollectionList1");
                else if (value instanceof CEventTerm term) consumer = Integer.valueOf(6).equals(term.getTypeId());
                try {
                    field.setAccessible(true);
                    visit(field.get(value), consumer, common, allocated, seen);
                } catch (IllegalAccessException error) {
                    throw new IllegalStateException("ACTION_WAZA条件读取失败: " + field.getName(), error);
                }
            }
        }
    }

    private static void rebind(BsdxInfoCollection collection, BheCommonProjectileAppendPlan common,
                               TsukuyomiGrpAppendPlan allocated) {
        var semantic = BsdxInfoCollectionAnalyzer.analyze(collection);
        if (!semantic.getWarnings().isEmpty() || semantic.getSteps().isEmpty()) return;
        var terminal = semantic.getSteps().get(semantic.getSteps().size() - 1);
        if (!"OBJECT3".equals(terminal.getTermGroupCodeName())
                || !"ACTION_WAZA".equals(terminal.getTermItemDescription())) return;
        List<Integer> params = collection.getParamList();
        if (params == null || params.size() < 2 || params.get(0) == null || params.get(0) < 0
                || params.get(1) == null || params.get(1) < 0) return;
        Reference target = resolve(params.get(0), params.get(1), common, allocated);
        if (target != null) {
            List<Integer> rebound = new ArrayList<>(params);
            rebound.set(0, target.group); rebound.set(1, target.skill);
            collection.setParamList(rebound);
        }
    }

    private static Reference resolve(int sourceGroup, int sourceSkill, BheCommonProjectileAppendPlan common,
                                      TsukuyomiGrpAppendPlan allocated) {
        if (common != null && common.getSourceWazIndexToTargetIndex().containsKey(sourceGroup)) {
            Integer group = common.getSourceWazIndexToTargetIndex().get(sourceGroup);
            Integer base = common.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
            Integer count = common.getSourceWazIndexToTargetSkillCount().get(sourceGroup);
            return group != null && group >= 0 && base != null && base >= 0 && count != null && sourceSkill < count
                    ? new Reference(group, Math.addExact(base, sourceSkill)) : null;
        }
        boolean allocatedHere = allocated != null && allocated.getSourceWazGroupIndexToTargetIndex().containsKey(sourceGroup);
        Map<Integer, Integer> groups = allocatedHere ? allocated.getSourceWazGroupIndexToTargetIndex()
                : common == null ? Map.of() : common.getSourcePrivateWazIndexToTargetIndex();
        Map<Integer, Map<Integer, Integer>> skills = allocatedHere ? allocated.getSourceWazSkillIndexToTargetIndexByGroup()
                : common == null ? Map.of() : common.getSourcePrivateWazSkillIndexToTargetIndexByGroup();
        Integer group = groups.get(sourceGroup);
        Integer skill = skills.getOrDefault(sourceGroup, Map.of()).get(sourceSkill);
        return group != null && group >= 0 && skill != null && skill >= 0 ? new Reference(group, skill) : null;
    }

    private record Reference(int group, int skill) { }
}
