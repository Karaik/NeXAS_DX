package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm.graft;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventChange;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm.graft.resolve.BheResolvedSeRef;
import com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm.graft.resolve.BheResourceIndexResolver;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiWazRebindContext;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;


public class RebindWazStep {

    private final BheInfoCollectionObjectGraphRewriter termRewriter = new BheInfoCollectionObjectGraphRewriter();

    public Waz rebindWilhelmWaz(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiImportPlan importPlan,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            BheCommonProjectileAppendPlan commonProjectileAppendPlan
    ) {
        if (request == null || tsukuyomiPackage == null || importPlan == null || grpAppendPlan == null) {
            return null;
        }
        Waz sourceWaz = findRequiredSourceWaz(tsukuyomiPackage, request.getWazFileName());
        if (sourceWaz == null) {
            return null;
        }
        TsukuyomiWazRebindContext context = buildContext(request, sourceWaz, importPlan, grpAppendPlan);
        BheResourceIndexResolver resolver = new BheResourceIndexResolver(commonProjectileAppendPlan, grpAppendPlan);
        Waz targetWaz = createTargetWazShell(context);
        targetWaz.setSkillList(rebuildSkillList(context, resolver));
        termRewriter.rewrite(targetWaz, "wilhelm main WAZ " + request.getWazFileName());
        rewriteCEventChangeWazaSelectParams(targetWaz, resolver);
        validateRebindResult(targetWaz, context);
        return targetWaz;
    }

    public Waz rebindAuxiliaryWaz(
            TsukuyomiGraftRequest request,
            Waz sourceWaz,
            TsukuyomiImportPlan importPlan,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            Integer sourceWazGroupIndex,
            BheCommonProjectileAppendPlan commonProjectileAppendPlan
    ) {
        if (request == null || sourceWaz == null || importPlan == null || grpAppendPlan == null) {
            return null;
        }

        TsukuyomiWazRebindContext context = buildContext(request, sourceWaz, importPlan, grpAppendPlan);
        BheResourceIndexResolver resolver = new BheResourceIndexResolver(commonProjectileAppendPlan, grpAppendPlan);
        Waz targetWaz = new Waz();
        targetWaz.setFileName(sourceWaz.getFileName());
        targetWaz.setExtensionName(sourceWaz.getExtensionName());
        targetWaz.setSkillList(rebuildSkillList(context, resolver));
        termRewriter.rewrite(targetWaz, "wilhelm auxiliary WAZ " + targetWaz.getFileName());
        rewriteCEventChangeWazaSelectParams(targetWaz, resolver);
        validateRebindResult(targetWaz, context);
        return targetWaz;
    }

    private Waz findRequiredSourceWaz(TsukuyomiPackageBundle tsukuyomiPackage, String fileName) {
        if (tsukuyomiPackage.getWazByFileName() == null || tsukuyomiPackage.getWazByFileName().isEmpty()) {
            return null;
        }

        for (Map.Entry<String, Waz> entry : tsukuyomiPackage.getWazByFileName().entrySet()) {
            if (normalizeFileName(entry.getKey()).equals(normalizeFileName(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private TsukuyomiWazRebindContext buildContext(
            TsukuyomiGraftRequest request,
            Waz sourceWaz,
            TsukuyomiImportPlan importPlan,
            TsukuyomiGrpAppendPlan grpAppendPlan
    ) {
        TsukuyomiWazRebindContext context = new TsukuyomiWazRebindContext();
        context.setSourceWaz(sourceWaz);
        context.setSourceFileName(sourceWaz.getFileName());
        context.setTargetMainWazGroupIndex(grpAppendPlan.getWazaGroupIndex());
        context.setTargetMainSpriteGroupIndex(grpAppendPlan.getSpriteGroupIndex());
        context.setTargetMainBatVoiceGroupIndex(grpAppendPlan.getBatVoiceGroupIndex());
        for (Map.Entry<String, Integer> entry : importPlan.getSourceWazIndexByFileName().entrySet()) {
            String normalizedFileName = normalizeFileName(entry.getKey());
            context.getSourceWazFileNameByGroupIndex().put(entry.getValue(), normalizedFileName);
        }
        for (Map.Entry<String, Integer> entry : importPlan.getSourceSpriteIndexByFileName().entrySet()) {
            String normalizedFileName = normalizeFileName(entry.getKey());
            context.getSourceSpriteFileNameByGroupIndex().put(entry.getValue(), normalizedFileName);
        }
        for (Map.Entry<String, Integer> entry : importPlan.getTargetWazIndexByFileName().entrySet()) {
            context.getTargetWazGroupIndexByFileName().put(normalizeFileName(entry.getKey()), entry.getValue());
        }
        context.getTargetWazGroupIndexByFileName().put(
                normalizeFileName(request.getWazFileName()),
                grpAppendPlan.getWazaGroupIndex()
        );
        for (Map.Entry<String, Integer> entry : importPlan.getTargetSpriteIndexByFileName().entrySet()) {
            context.getTargetSpriteGroupIndexByFileName().put(normalizeFileName(entry.getKey()), entry.getValue());
        }
        context.getTargetSpriteGroupIndexByFileName().put(
                normalizeFileName(request.getSpriteFileName()),
                grpAppendPlan.getSpriteGroupIndex()
        );
        buildWazIndexMap(context);
        buildSpriteIndexMap(context);
        context.getSourceToTargetSeGroupIndex().putAll(grpAppendPlan.getSourceSeGroupIndexToTargetIndex());
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : grpAppendPlan.getSourceSeItemIndexToTargetIndexByGroup().entrySet()) {
            context.getSourceToTargetSeItemIndexByGroup().put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        context.getSourceToTargetBatVoiceGroupIndex().putAll(grpAppendPlan.getSourceBatVoiceGroupIndexToTargetIndex());
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : grpAppendPlan.getSourceWazSkillIndexToTargetIndexByGroup().entrySet()) {
            context.getSourceToTargetWazSkillIndexByGroup().put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        return context;
    }

    private void buildWazIndexMap(TsukuyomiWazRebindContext context) {
        for (Map.Entry<Integer, String> entry : context.getSourceWazFileNameByGroupIndex().entrySet()) {
            Integer targetIndex = context.getTargetWazGroupIndexByFileName().get(entry.getValue());
            if (targetIndex != null) {
                context.getSourceToTargetWazGroupIndex().put(entry.getKey(), targetIndex);
            }
        }
    }

    private void buildSpriteIndexMap(TsukuyomiWazRebindContext context) {
        for (Map.Entry<Integer, String> entry : context.getSourceSpriteFileNameByGroupIndex().entrySet()) {
            Integer targetIndex = context.getTargetSpriteGroupIndexByFileName().get(entry.getValue());
            if (targetIndex != null) {
                context.getSourceToTargetSpriteGroupIndex().put(entry.getKey(), targetIndex);
            }
        }
    }

    private Waz createTargetWazShell(TsukuyomiWazRebindContext context) {
        Waz targetWaz = new Waz();
        if (context.getSourceWaz() != null) {
            targetWaz.setFileName(context.getSourceWaz().getFileName());
            targetWaz.setExtensionName(context.getSourceWaz().getExtensionName());
        }
        return targetWaz;
    }

    private List<Waz.Skill> rebuildSkillList(
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        List<Waz.Skill> target = new ArrayList<>();
        List<Waz.Skill> source = context.getSourceWaz().getSkillList();
        if (source == null) {
            return target;
        }
        for (Waz.Skill skill : source) {
            target.add(rebuildSkill(skill, context, resolver));
        }
        return target;
    }

    private Waz.Skill rebuildSkill(
            Waz.Skill source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        if (source == null) {
            return null;
        }

        Waz.Skill target = new Waz.Skill();
        target.setPhaseQuantity(source.getPhaseQuantity());
        target.setSkillNameJapanese(source.getSkillNameJapanese());
        target.setSkillNameEnglish(source.getSkillNameEnglish());
        target.setPhasesInfo(rebuildSkillPhaseList(source.getPhasesInfo(), context, resolver));
        target.setSkillSuffixList(rebuildSkillSuffixList(source.getSkillSuffixList()));
        return target;
    }

    private List<Waz.Skill.SkillPhase> rebuildSkillPhaseList(
            List<Waz.Skill.SkillPhase> source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        List<Waz.Skill.SkillPhase> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (Waz.Skill.SkillPhase phase : source) {
            target.add(rebuildSkillPhase(phase, context, resolver));
        }
        return target;
    }

    private Waz.Skill.SkillPhase rebuildSkillPhase(
            Waz.Skill.SkillPhase source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        if (source == null) {
            return null;
        }

        Waz.Skill.SkillPhase target = new Waz.Skill.SkillPhase();
        target.setSkillUnitCollection(rebuildSkillUnitList(source.getSkillUnitCollection(), context, resolver));
        return target;
    }

    private List<SkillUnit> rebuildSkillUnitList(
            List<SkillUnit> source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        List<SkillUnit> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (SkillUnit unit : source) {
            target.add(rebuildSkillUnit(unit, context, resolver));
        }
        return target;
    }

    private SkillUnit rebuildSkillUnit(
            SkillUnit source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        if (source == null) {
            return null;
        }

        SkillUnit target = new SkillUnit();
        target.setUnitQuantity(source.getUnitQuantity());
        target.setUnitDescription(source.getUnitDescription());
        target.setSkillInfoObjectList(rebuildSkillInfoObjectList(source.getSkillInfoObjectList(), context, resolver));
        target.setSkillInfoUnknownList(rebuildSkillInfoUnknownList(source.getSkillInfoUnknownList()));
        return target;
    }

    private List<SkillInfoObject> rebuildSkillInfoObjectList(
            List<SkillInfoObject> source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        List<SkillInfoObject> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (SkillInfoObject infoObject : source) {
            target.add(rebuildSkillInfoObject(infoObject, context, resolver));
        }
        return target;
    }

    private List<SkillInfoUnknown> rebuildSkillInfoUnknownList(List<SkillInfoUnknown> source) {
        List<SkillInfoUnknown> target = new ArrayList<>();
        if (source == null) {
            return target;
        }

        for (SkillInfoUnknown infoUnknown : source) {
            target.add(rebuildSkillInfoUnknown(infoUnknown));
        }
        return target;
    }

    private SkillInfoObject rebuildSkillInfoObject(
            SkillInfoObject source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        if (source == null) {
            return null;
        }
        if (source instanceof CEventWazaSelect wazaSelect) {
            return rebuildCEventWazaSelect(wazaSelect, context, resolver);
        }
        if (source instanceof CEventSprite sprite) {
            return rebuildCEventSprite(sprite, context, resolver);
        }
        if (source instanceof CEventSe se) {
            return rebuildCEventSe(se, resolver);
        }
        if (source instanceof CEventVoice voice) {
            return rebuildCEventVoice(voice, context);
        }
        if (source instanceof SkillInfoUnknown unknown) {
            return rebuildSkillInfoUnknown(unknown);
        }
        if (hasNestedUnitList(source.getClass())) {
            return rebuildNestedUnitObject(source, context, resolver);
        }
        return rebuildSimpleLeafObject(source);
    }

    private SkillInfoUnknown rebuildSkillInfoUnknown(SkillInfoUnknown source) {
        if (source == null) {
            return null;
        }

        SkillInfoUnknown target = new SkillInfoUnknown();
        BeanUtil.copyProperties(source, target);
        return target;
    }

    private CEventWazaSelect rebuildCEventWazaSelect(
            CEventWazaSelect source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        CEventWazaSelect target = new CEventWazaSelect();
        BeanUtil.copyProperties(source, target);
        target.setWazFileNo(resolver.resolveWazGroupIndex(source.getWazFileNo()));
        target.setWazSequenceNo(resolver.resolveWazSkillIndex(source.getWazFileNo(), source.getWazSequenceNo()));
        return target;
    }

    private CEventSprite rebuildCEventSprite(
            CEventSprite source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        CEventSprite target = new CEventSprite();
        BeanUtil.copyProperties(source, target);
        target.setSpmFileSequence(resolver.resolveSpriteGroupIndex(source.getSpmFileSequence()));
        target.setActionGroupNumber(resolver.resolveSpriteActionGroupIndex(
                source.getSpmFileSequence(),
                source.getActionGroupNumber()
        ));
        return target;
    }

    private CEventSe rebuildCEventSe(CEventSe source, BheResourceIndexResolver resolver) {
        CEventSe target = new CEventSe();
        BeanUtil.copyProperties(source, target);
        target.setByteDataList(deepCopyByteArrayList(source.getByteDataList()));
        rewriteSeTargets(target.getByteDataList(), resolver);
        return target;
    }

    private CEventVoice rebuildCEventVoice(CEventVoice source, TsukuyomiWazRebindContext context) {
        CEventVoice target = new CEventVoice();
        BeanUtil.copyProperties(source, target);
        target.setByteDataList(deepCopyByteArrayList(source.getByteDataList()));
        rewriteVoiceTargetGroup(target.getByteDataList(), context);
        return target;
    }

    private SkillInfoObject rebuildNestedUnitObject(
            SkillInfoObject source,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        SkillInfoObject target = instantiateLike(source);
        BeanUtil.copyProperties(source, target);
        rebuildNestedUnitLists(source, target, context, resolver);
        return target;
    }

    private SkillInfoObject rebuildSimpleLeafObject(SkillInfoObject source) {
        SkillInfoObject target = instantiateLike(source);
        BeanUtil.copyProperties(source, target);
        return target;
    }

    private void rebuildNestedUnitLists(
            SkillInfoObject source,
            SkillInfoObject target,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        for (Field field : getAllFields(source.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType())) {
                continue;
            }
            if (!field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
                continue;
            }

            field.setAccessible(true);
            try {
                List<?> sourceUnits = (List<?>) field.get(source);
                List<Object> targetUnits = new ArrayList<>();

                if (sourceUnits != null) {
                    for (Object sourceUnit : sourceUnits) {
                        targetUnits.add(rebuildNestedUnit(sourceUnit, context, resolver));
                    }
                }

                field.set(target, targetUnits);
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("重建嵌套 unit 列表失败: " + field.getName(), e);
            }
        }
    }

    private Object rebuildNestedUnit(
            Object sourceUnit,
            TsukuyomiWazRebindContext context,
            BheResourceIndexResolver resolver
    ) {
        if (sourceUnit == null) {
            return null;
        }

        Object targetUnit = instantiateUnitLike(sourceUnit);
        BeanUtil.copyProperties(sourceUnit, targetUnit);
        SkillInfoObject sourceData = tryGetUnitData(sourceUnit);
        if (sourceData != null) {
            trySetUnitData(targetUnit, rebuildSkillInfoObject(sourceData, context, resolver));
        }
        return targetUnit;
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

    private void rewriteSeTargets(List<byte[]> byteDataList, BheResourceIndexResolver resolver) {
        if (byteDataList == null || resolver == null) {
            return;
        }

        for (byte[] bytes : byteDataList) {
            if (bytes == null || bytes.length < 8) {
                continue;
            }

            int sourceGroupIndex = readLittleEndianInt(bytes, 0);
            int sourceItemIndex = readLittleEndianInt(bytes, 4);
            if (sourceGroupIndex < 0 || sourceItemIndex < 0) {
                continue;
            }

            BheResolvedSeRef targetRef = resolver.resolveSe(sourceGroupIndex, sourceItemIndex);
            writeLittleEndianInt(bytes, 0, targetRef.targetGroupIndex());
            writeLittleEndianInt(bytes, 4, targetRef.targetItemIndex());
        }
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

    
    private void rewriteVoiceTargetGroup(List<byte[]> byteDataList, TsukuyomiWazRebindContext context) {
        if (byteDataList == null || context == null) {
            return;
        }

        Map<Integer, Integer> batVoiceGroupMap = context.getSourceToTargetBatVoiceGroupIndex();
        for (byte[] bytes : byteDataList) {
            if (bytes == null || bytes.length < 4) {
                continue;
            }

            int sourceGroupIndex = readLittleEndianInt(bytes, 0);
            if (sourceGroupIndex < 0) {
                continue;
            }

            Integer targetGroupIndex = context.getSourceToTargetBatVoiceGroupIndex().get(sourceGroupIndex);
            if (targetGroupIndex == null) {
                throw new IllegalStateException(
                        "找不到 CEventVoice 的 BatVoiceGroup 目标映射: sourceGroup=" + sourceGroupIndex
                );
            }

            writeLittleEndianInt(bytes, 0, targetGroupIndex);
        }
    }

    private void rewriteCEventChangeWazaSelectParams(Waz targetWaz, BheResourceIndexResolver resolver) {
        if (targetWaz == null || targetWaz.getSkillList() == null || resolver == null) {
            return;
        }
        for (Waz.Skill skill : targetWaz.getSkillList()) {
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
                        rewriteCEventChangeWazaSelectParams(object, resolver);
                    }
                }
            }
        }
    }

    private void rewriteCEventChangeWazaSelectParams(
            SkillInfoObject object,
            BheResourceIndexResolver resolver
    ) {
        if (object == null) {
            return;
        }
        if (object instanceof CEventChange change) {
            rewriteCEventChangeList2(change, resolver);
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
                        rewriteCEventChangeWazaSelectParams(nested, resolver);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("重写 CEventChange WAZ 参数失败: " + field.getName(), e);
            }
        }
    }

    private void rewriteCEventChangeList2(CEventChange change, BheResourceIndexResolver resolver) {
        if (change.getBsdxInfoCollectionList2() == null || change.getBsdxInfoCollectionList2().isEmpty()) {
            return;
        }
        for (BsdxInfoCollection collection : change.getBsdxInfoCollectionList2()) {
            if (collection == null || collection.getParamList() == null || collection.getParamList().size() < 2) {
                continue;
            }
            Integer sourceWazIndex = collection.getParamList().get(0);
            Integer sourceActionGroupIndex = collection.getParamList().get(1);
            if (sourceWazIndex == null || sourceWazIndex < 0 || sourceActionGroupIndex == null || sourceActionGroupIndex < 0) {
                continue;
            }
            Integer targetWazIndex = resolver.resolveWazGroupIndex(sourceWazIndex);
            Integer targetActionGroupIndex = resolver.resolveWazSkillIndex(sourceWazIndex, sourceActionGroupIndex);
            collection.getParamList().set(0, targetWazIndex);
            collection.getParamList().set(1, targetActionGroupIndex);
        }
    }

    private List<Waz.Skill.SkillSuffix> rebuildSkillSuffixList(List<Waz.Skill.SkillSuffix> source) {
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

    private boolean hasNestedUnitList(Class<?> type) {
        for (Field field : getAllFields(type)) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (List.class.isAssignableFrom(field.getType())
                    && field.getName().toLowerCase(Locale.ROOT).endsWith("unitlist")) {
                return true;
            }
        }
        return false;
    }

    private SkillInfoObject instantiateLike(SkillInfoObject source) {
        try {
            Constructor<?> noArg = source.getClass().getDeclaredConstructor();
            noArg.setAccessible(true);
            return (SkillInfoObject) noArg.newInstance();
        } catch (ReflectiveOperationException ignored) {
        }

        try {
            Constructor<?> typeCtor = source.getClass().getDeclaredConstructor(Integer.class);
            typeCtor.setAccessible(true);
            return (SkillInfoObject) typeCtor.newInstance(source.getTypeId());
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建 SkillInfoObject 实例: " + source.getClass().getName(), e);
        }
    }

    private Object instantiateUnitLike(Object sourceUnit) {
        try {
            Constructor<?> noArg = sourceUnit.getClass().getDeclaredConstructor();
            noArg.setAccessible(true);
            return noArg.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("无法创建 unit 实例: " + sourceUnit.getClass().getName(), e);
        }
    }

    private SkillInfoObject tryGetUnitData(Object unit) {
        try {
            Method getter = unit.getClass().getMethod("getData");
            Object value = getter.invoke(unit);
            return value instanceof SkillInfoObject ? (SkillInfoObject) value : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private void trySetUnitData(Object unit, SkillInfoObject data) {
        try {
            Method setter = unit.getClass().getMethod("setData", SkillInfoObject.class);
            setter.invoke(unit, data);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("写回 unit.data 失败: " + unit.getClass().getName(), e);
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;

        while (current != null && current != Object.class) {
            Field[] declaredFields = current.getDeclaredFields();
            for (Field declaredField : declaredFields) {
                fields.add(declaredField);
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null) {
            return "";
        }
        return fileName.trim().toLowerCase(Locale.ROOT);
    }

    private void validateRebindResult(Waz targetWaz, TsukuyomiWazRebindContext context) {
        if (targetWaz == null) {
            throw new IllegalStateException("step7 输出的目标 Waz 为空");
        }
        if (targetWaz.getSkillList() == null || targetWaz.getSkillList().isEmpty()) {
            throw new IllegalStateException("step7 输出的目标 Waz 没有 skillList");
        }
        if (context.getSourceToTargetWazGroupIndex().isEmpty()) {
            throw new IllegalStateException("step7 没有得到任何 waz 顶层索引映射");
        }
    }
}
