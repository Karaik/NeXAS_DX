package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 公共弹幕 WAZ 落盘前的语音组重定向器。
 *
 * <p>公共弹幕 WAZ 对象挂在 {@code preparedBaseline} 里，会被后续机体继续继承。
 * 如果直接原地改这些共享对象，BHE 源侧 BatVoice group 编号就会丢失，后续机体再落盘公共 WAZ 时
 * 无法知道哪些 group 还需要重定向。因此这里固定先复制一份输出对象，只在复制品上改 {@link CEventVoice}。</p>
 *
 * <p>{@link CEventVoice} 的每条候选语音是 12 字节：
 * {@code [BatVoiceGroup:int][VoiceItem:int][Probability:int]}。只有前 4 字节是全局语音组索引；
 * item index 和 probability 已经是组内语义，不能改，否则会换成另一条语音或改变随机权重。</p>
 */
public class BheCommonProjectileVoiceRebinder {

    public Waz copyForOutput(
            Waz source,
            Map<Integer, Integer> cumulativeSourceBatVoiceGroupIndexToTargetIndex
    ) {
        if (source == null) {
            return null;
        }

        Waz target = new Waz();
        target.setFileName(source.getFileName());
        target.setExtensionName(source.getExtensionName());
        target.setSkillList(copySkillList(source.getSkillList(), cumulativeSourceBatVoiceGroupIndexToTargetIndex));
        return target;
    }

    private List<Waz.Skill> copySkillList(
            List<Waz.Skill> source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        List<Waz.Skill> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (Waz.Skill skill : source) {
            target.add(copySkill(skill, voiceGroupMap));
        }
        return target;
    }

    private Waz.Skill copySkill(
            Waz.Skill source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        if (source == null) {
            return null;
        }

        Waz.Skill target = new Waz.Skill();
        target.setPhaseQuantity(source.getPhaseQuantity());
        target.setSkillNameJapanese(source.getSkillNameJapanese());
        target.setSkillNameEnglish(source.getSkillNameEnglish());
        target.setPhasesInfo(copySkillPhaseList(source.getPhasesInfo(), voiceGroupMap));
        target.setSkillSuffixList(copySkillSuffixList(source.getSkillSuffixList()));
        return target;
    }

    private List<Waz.Skill.SkillPhase> copySkillPhaseList(
            List<Waz.Skill.SkillPhase> source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        List<Waz.Skill.SkillPhase> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (Waz.Skill.SkillPhase phase : source) {
            target.add(copySkillPhase(phase, voiceGroupMap));
        }
        return target;
    }

    private Waz.Skill.SkillPhase copySkillPhase(
            Waz.Skill.SkillPhase source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        if (source == null) {
            return null;
        }

        Waz.Skill.SkillPhase target = new Waz.Skill.SkillPhase();
        target.setSkillUnitCollection(copySkillUnitList(source.getSkillUnitCollection(), voiceGroupMap));
        return target;
    }

    private List<SkillUnit> copySkillUnitList(
            List<SkillUnit> source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        List<SkillUnit> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (SkillUnit unit : source) {
            target.add(copySkillUnit(unit, voiceGroupMap));
        }
        return target;
    }

    private SkillUnit copySkillUnit(
            SkillUnit source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        if (source == null) {
            return null;
        }

        SkillUnit target = new SkillUnit();
        target.setUnitQuantity(source.getUnitQuantity());
        target.setUnitDescription(source.getUnitDescription());
        target.setSkillInfoObjectList(copySkillInfoObjectList(source.getSkillInfoObjectList(), voiceGroupMap));
        target.setSkillInfoUnknownList(copySkillInfoUnknownList(source.getSkillInfoUnknownList()));
        return target;
    }

    private List<SkillInfoObject> copySkillInfoObjectList(
            List<SkillInfoObject> source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        List<SkillInfoObject> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (SkillInfoObject object : source) {
            target.add(copySkillInfoObject(object, voiceGroupMap));
        }
        return target;
    }

    private List<SkillInfoUnknown> copySkillInfoUnknownList(List<SkillInfoUnknown> source) {
        List<SkillInfoUnknown> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (SkillInfoUnknown unknown : source) {
            target.add(copySkillInfoUnknown(unknown));
        }
        return target;
    }

    private SkillInfoUnknown copySkillInfoUnknown(SkillInfoUnknown source) {
        if (source == null) {
            return null;
        }
        SkillInfoUnknown target = new SkillInfoUnknown();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    private SkillInfoObject copySkillInfoObject(
            SkillInfoObject source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        if (source == null) {
            return null;
        }

        SkillInfoObject target = instantiateLike(source);
        BeanUtil.copyProperties(source, target);

        if (source instanceof CEventVoice sourceVoice && target instanceof CEventVoice targetVoice) {
            targetVoice.setByteDataList(copyVoiceByteData(sourceVoice.getByteDataList(), voiceGroupMap));
        } else if (source instanceof CEventSe sourceSe && target instanceof CEventSe targetSe) {
            targetSe.setByteDataList(deepCopyByteArrayList(sourceSe.getByteDataList()));
        }

        copyNestedUnitLists(source, target, voiceGroupMap);
        return target;
    }

    private void copyNestedUnitLists(
            SkillInfoObject source,
            SkillInfoObject target,
            Map<Integer, Integer> voiceGroupMap
    ) {
        for (Field field : getAllFields(source.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType())
                    || !field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
                continue;
            }

            field.setAccessible(true);
            try {
                List<?> sourceUnits = (List<?>) field.get(source);
                List<Object> targetUnits = new ArrayList<>();
                if (sourceUnits != null) {
                    for (Object sourceUnit : sourceUnits) {
                        targetUnits.add(copyNestedUnit(sourceUnit, voiceGroupMap));
                    }
                }
                field.set(target, targetUnits);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("复制嵌套 WAZ unit 列表失败: " + field.getName(), e);
            }
        }
    }

    private Object copyNestedUnit(
            Object sourceUnit,
            Map<Integer, Integer> voiceGroupMap
    ) {
        if (sourceUnit == null) {
            return null;
        }

        Object targetUnit = instantiateUnitLike(sourceUnit);
        BeanUtil.copyProperties(sourceUnit, targetUnit);

        SkillInfoObject sourceData = tryGetUnitData(sourceUnit);
        if (sourceData != null) {
            trySetUnitData(targetUnit, copySkillInfoObject(sourceData, voiceGroupMap));
        }
        return targetUnit;
    }

    private List<byte[]> copyVoiceByteData(
            List<byte[]> source,
            Map<Integer, Integer> voiceGroupMap
    ) {
        List<byte[]> target = deepCopyByteArrayList(source);
        if (voiceGroupMap == null || voiceGroupMap.isEmpty()) {
            return target;
        }

        for (byte[] bytes : target) {
            if (bytes == null || bytes.length < 4) {
                continue;
            }

            int sourceGroupIndex = readLittleEndianInt(bytes, 0);
            if (sourceGroupIndex < 0) {
                continue;
            }

            /*
             * 公共 WAZ 可能同时包含多个 BHE 机体的技能；当前轮还没 graft 的机体没有目标 BatVoice group。
             * 这些尚未迁移的源 group 必须保持原值，等后续机体进入链条后，累计映射表变大，再重新复制并落盘公共 WAZ。
             */
            Integer targetGroupIndex = voiceGroupMap.get(sourceGroupIndex);
            if (targetGroupIndex != null) {
                writeLittleEndianInt(bytes, 0, targetGroupIndex);
            }
        }
        return target;
    }

    private List<byte[]> deepCopyByteArrayList(List<byte[]> source) {
        List<byte[]> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (byte[] bytes : source) {
            target.add(bytes == null ? null : bytes.clone());
        }
        return target;
    }

    private List<Waz.Skill.SkillSuffix> copySkillSuffixList(List<Waz.Skill.SkillSuffix> source) {
        List<Waz.Skill.SkillSuffix> target = new ArrayList<>();
        if (source == null) {
            return target;
        }
        for (Waz.Skill.SkillSuffix suffix : source) {
            if (suffix == null) {
                target.add(null);
                continue;
            }
            Waz.Skill.SkillSuffix copied = new Waz.Skill.SkillSuffix();
            copied.setInt1(suffix.getInt1());
            copied.setInt2(suffix.getInt2());
            target.add(copied);
        }
        return target;
    }

    private SkillInfoObject instantiateLike(SkillInfoObject source) {
        try {
            Constructor<?> constructor = source.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            return (SkillInfoObject) constructor.newInstance();
        } catch (ReflectiveOperationException ignored) {
            // 少数 DTO 只有 Integer typeId 构造器，先试无参，失败后再按 typeId 创建。
        }

        try {
            Constructor<?> constructor = source.getClass().getDeclaredConstructor(Integer.class);
            constructor.setAccessible(true);
            return (SkillInfoObject) constructor.newInstance(source.getTypeId());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建 WAZ 对象: " + source.getClass().getName(), e);
        }
    }

    private Object instantiateUnitLike(Object sourceUnit) {
        try {
            Constructor<?> constructor = sourceUnit.getClass().getDeclaredConstructor();
            constructor.setAccessible(true);
            return constructor.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建嵌套 WAZ unit: " + sourceUnit.getClass().getName(), e);
        }
    }

    private SkillInfoObject tryGetUnitData(Object unit) {
        try {
            Method getter = unit.getClass().getMethod("getData");
            Object value = getter.invoke(unit);
            return value instanceof SkillInfoObject object ? object : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private void trySetUnitData(Object unit, SkillInfoObject data) {
        try {
            Method setter = unit.getClass().getMethod("setData", SkillInfoObject.class);
            setter.invoke(unit, data);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写回嵌套 WAZ unit.data 失败: " + unit.getClass().getName(), e);
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            for (Field field : current.getDeclaredFields()) {
                fields.add(field);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private void writeLittleEndianInt(byte[] bytes, int offset, int value) {
        bytes[offset] = (byte) (value & 0xFF);
        bytes[offset + 1] = (byte) ((value >>> 8) & 0xFF);
        bytes[offset + 2] = (byte) ((value >>> 16) & 0xFF);
        bytes[offset + 3] = (byte) ((value >>> 24) & 0xFF);
    }
}
