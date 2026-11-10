package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.cross;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * BHE 公共弹幕资源簇的交叉重定向步骤。
 *
 * <p>交叉重定向只处理“公共 WAZ 指向其他资源”的引用重写，不处理公共资源目标 entry 的创建。
 * 典型内容包括：WAZ -> WAZ、WAZ -> SPM、WAZ -> SeGroup/SeItem。</p>
 *
 * <p>CEventVoice 只记录 SOU/MISAKI 外部角色语音依赖，不在公共资源阶段重建 BatVoice。
 * term / InfoCollection 由 term 包统一重编译，避免和 WAZ/SPM/SE 的普通 index 重写混成一团。</p>
 */
public class CrossRedirectBheCommonProjectileResourcesStep {

    private final BheInfoCollectionObjectGraphRewriter termRewriter = new BheInfoCollectionObjectGraphRewriter();

    public void redirect(
            TsukuyomiGraftRequest request,
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiBsdxBaselineBundle inheritedBaseline,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        validateInputs(rawSourceBundle, convertedBundle, appendPlan);
        for (Map.Entry<String, Waz> entry : convertedBundle.getCommonProjectileResourceBundle().getWazByFileName().entrySet()) {
            rewriteCommonWaz(entry.getKey(), entry.getValue(), rawSourceBundle, appendPlan);
            // 格式转换阶段只把字段搬到 BSDX DTO；公共资源接入阶段负责把 BHE term 索引空间重编译成 BSDX term 索引空间。
            termRewriter.rewrite(entry.getValue(), "common projectile WAZ " + entry.getKey());
        }
    }

    private void validateInputs(
            TsukuyomiRawSourceBundle rawSourceBundle,
            TsukuyomiConvertedBundle convertedBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        if (rawSourceBundle == null) {
            throw new IllegalStateException("公共 WAZ 交叉重定向缺少 rawSourceBundle");
        }
        if (convertedBundle == null || convertedBundle.getCommonProjectileResourceBundle() == null) {
            throw new IllegalStateException("公共 WAZ 交叉重定向缺少 commonProjectileResourceBundle");
        }
        if (appendPlan == null) {
            throw new IllegalStateException("公共 WAZ 交叉重定向缺少 appendPlan");
        }
        if (appendPlan.getSourceWazIndexToTargetIndex().isEmpty()
                || appendPlan.getSourceSpriteIndexToTargetIndex().isEmpty()
                || appendPlan.getSourceSePairToTargetItemIndex().isEmpty()
                || appendPlan.getCommonProjectileSeGroupIndex() < 0) {
            throw new IllegalStateException("公共 WAZ 交叉重定向需要先完成 selfRedirect 映射");
        }
    }

    private void rewriteCommonWaz(
            String fileName,
            Waz waz,
            TsukuyomiRawSourceBundle rawSourceBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        if (waz == null || waz.getSkillList() == null) {
            return;
        }
        for (Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                        rewriteObject(fileName, object, rawSourceBundle, appendPlan);
                    }
                }
            }
        }
    }

    private void rewriteObject(
            String fileName,
            SkillInfoObject object,
            TsukuyomiRawSourceBundle rawSourceBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        if (object == null) {
            return;
        }
        if (object instanceof CEventWazaSelect select) {
            rewriteWazaSelect(fileName, select, appendPlan);
        } else if (object instanceof CEventSprite sprite) {
            rewriteSprite(fileName, sprite, appendPlan);
        } else if (object instanceof CEventSe se) {
            rewriteSe(fileName, se, appendPlan);
        } else if (object instanceof CEventVoice voice) {
            recordVoiceDependency(fileName, voice, rawSourceBundle, appendPlan);
        }

        for (Field field : getAllFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType())
                    || !field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
                continue;
            }
            field.setAccessible(true);
            try {
                List<?> units = (List<?>) field.get(object);
                if (units == null) {
                    continue;
                }
                for (Object unit : units) {
                    SkillInfoObject nested = tryGetUnitData(unit);
                    if (nested != null) {
                        rewriteObject(fileName, nested, rawSourceBundle, appendPlan);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("公共 WAZ 交叉重定向解析嵌套 unit 失败: " + field.getName(), e);
            }
        }
    }

    private void rewriteWazaSelect(
            String fileName,
            CEventWazaSelect select,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        Integer sourceIndex = select.getWazFileNo();
        if (sourceIndex == null || sourceIndex < 0) {
            return;
        }
        Integer targetIndex = appendPlan.getSourceWazIndexToTargetIndex().get(sourceIndex);
        if (targetIndex == null) {
            throw new IllegalStateException("公共 WAZ 找不到 CEventWazaSelect 目标映射: file="
                    + fileName + ", sourceWaz=" + sourceIndex);
        }
        select.setWazFileNo(targetIndex);
        Integer skillBase = appendPlan.getSourceWazIndexToTargetSkillBase().get(sourceIndex);
        if (skillBase != null && select.getWazSequenceNo() != null && select.getWazSequenceNo() >= 0) {
            select.setWazSequenceNo(skillBase + select.getWazSequenceNo());
        }
    }

    private void rewriteSprite(
            String fileName,
            CEventSprite sprite,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        Integer sourceIndex = sprite.getSpmFileSequence();
        if (sourceIndex == null || sourceIndex < 0) {
            return;
        }
        Integer targetIndex = appendPlan.getSourceSpriteIndexToTargetIndex().get(sourceIndex);
        if (targetIndex == null) {
            throw new IllegalStateException("公共 WAZ 找不到 CEventSprite 目标映射: file="
                    + fileName + ", sourceSprite=" + sourceIndex);
        }
        sprite.setSpmFileSequence(targetIndex);
        Integer actionBase = appendPlan.getSourceSpriteIndexToTargetActionGroupBase().get(sourceIndex);
        if (actionBase != null && sprite.getActionGroupNumber() != null && sprite.getActionGroupNumber() >= 0) {
            /*
             * 同名公共 SPM 会追加到 BSDX 宿主 SPM，CEventSprite 的 actionGroupNumber
             * 必须从“源 SPM 内部 anim index”切换成“宿主 SPM 内部 anim index”。
             */
            sprite.setActionGroupNumber(actionBase + sprite.getActionGroupNumber());
        }
    }

    private void rewriteSe(
            String fileName,
            CEventSe se,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        if (se.getByteDataList() == null) {
            return;
        }
        for (byte[] bytes : se.getByteDataList()) {
            if (bytes == null || bytes.length < 8) {
                continue;
            }
            int sourceGroupIndex = readLittleEndianInt(bytes, 0);
            int sourceItemIndex = readLittleEndianInt(bytes, 4);
            if (sourceGroupIndex < 0 || sourceItemIndex < 0) {
                continue;
            }
            String pairKey = BheCommonProjectileAppendPlan.sePairKey(sourceGroupIndex, sourceItemIndex);
            Integer targetItemIndex = appendPlan.getSourceSePairToTargetItemIndex().get(pairKey);
            if (targetItemIndex == null) {
                throw new IllegalStateException("公共 WAZ 找不到 CEventSe 目标映射: file="
                        + fileName + ", sourceSe=" + pairKey);
            }
            writeLittleEndianInt(bytes, 0, appendPlan.getCommonProjectileSeGroupIndex());
            writeLittleEndianInt(bytes, 4, targetItemIndex);
        }
    }

    private void recordVoiceDependency(
            String fileName,
            CEventVoice voice,
            TsukuyomiRawSourceBundle rawSourceBundle,
            BheCommonProjectileAppendPlan appendPlan
    ) {
        if (voice.getByteDataList() == null) {
            return;
        }
        Set<String> references = new LinkedHashSet<>(appendPlan.getGlobalVoiceReferences());
        for (byte[] bytes : voice.getByteDataList()) {
            if (bytes == null || bytes.length < 8) {
                continue;
            }
            int sourceGroupIndex = readLittleEndianInt(bytes, 0);
            int sourceVoiceIndex = readLittleEndianInt(bytes, 4);
            if (sourceGroupIndex < 0 || sourceVoiceIndex < 0) {
                continue;
            }
            references.add(buildVoiceReference(fileName, sourceGroupIndex, sourceVoiceIndex, rawSourceBundle));
        }
        appendPlan.getGlobalVoiceReferences().clear();
        appendPlan.getGlobalVoiceReferences().addAll(references);
    }

    private String buildVoiceReference(
            String fileName,
            int sourceGroupIndex,
            int sourceVoiceIndex,
            TsukuyomiRawSourceBundle rawSourceBundle
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append(fileName)
                .append(" BatVoice[")
                .append(sourceGroupIndex)
                .append("][")
                .append(sourceVoiceIndex)
                .append("]");
        if (rawSourceBundle.getBatVoiceGrp() != null
                && rawSourceBundle.getBatVoiceGrp().getVoiceList() != null
                && sourceGroupIndex < rawSourceBundle.getBatVoiceGrp().getVoiceList().size()) {
            var group = rawSourceBundle.getBatVoiceGrp().getVoiceList().get(sourceGroupIndex);
            if (group != null) {
                builder.append(" ")
                        .append(group.getCharacterCodeName())
                        .append("/");
                if (group.getVoices() != null && sourceVoiceIndex < group.getVoices().size()) {
                    var voice = group.getVoices().get(sourceVoiceIndex);
                    if (voice != null) {
                        builder.append(voice.getVoiceFileName());
                    }
                }
            }
        }
        return builder.toString();
    }

    private SkillInfoObject tryGetUnitData(Object unit) {
        if (unit == null) {
            return null;
        }
        try {
            Method getter = unit.getClass().getMethod("getData");
            Object value = getter.invoke(unit);
            return value instanceof SkillInfoObject object ? object : null;
        } catch (ReflectiveOperationException e) {
            return null;
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
