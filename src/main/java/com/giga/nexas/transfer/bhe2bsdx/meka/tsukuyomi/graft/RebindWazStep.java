package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import cn.hutool.core.bean.BeanUtil;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSe;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term.BheInfoCollectionObjectGraphRewriter;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.resolve.BheResolvedSeRef;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.resolve.BheResourceIndexResolver;
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

/**
 * 负责重建并回写 Tsukuyomi.waz。
 *
 * <p>这一层和 step6 的 mek 重建一样，不是在源对象上直接改字段，
 * 而是先构建上下文，再按 Waz 的层级一层层重建目标对象。</p>
 *
 * <p>当前 step7 已经明确处理的外部索引有：</p>
 * <ul>
 *     <li>{@link CEventWazaSelect#wazFileNo}：改成迁移到 BSDX 之后的目标 waz 顶层索引</li>
 *     <li>{@link CEventSprite#spmFileSequence}：改成迁移到 BSDX 之后的目标 sprite 顶层索引</li>
 *     <li>{@link CEventSe#seGroupIndex}：改成迁移到 BSDX 之后的目标 SeGroup 顶层索引</li>
 *     <li>{@link CEventSe#seItemIndex}：改成迁移到 BSDX 之后的目标 SeItem 索引（group 内偏移）</li>
 *     <li>{@link CEventVoice} 的 {@code byteDataList}：每 12-byte 段的前 4 字节（语音组索引）改为 Tsukuyomi 目标语音组索引</li>
 * </ul>
 *
 * <p>同时，像 {@code CEventEffect} 这类带嵌套 unit 列表的对象，
 * 会递归重建内部 {@code data}，而不是只做一层浅复制。</p>
 *
 * <p>BHE 公共资源映射通过 {@link BheResourceIndexResolver} 查询。它让公共资源映射优先，
 * 私有 graft 映射兜底，避免把公共资源混入 {@link TsukuyomiGrpAppendPlan}。</p>
 */
public class RebindWazStep {

    private final BheInfoCollectionObjectGraphRewriter termRewriter = new BheInfoCollectionObjectGraphRewriter();

    public Waz rebindTsukuyomiWaz(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiImportPlan importPlan,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            BheCommonProjectileAppendPlan commonProjectileAppendPlan
    ) {
        if (request == null || tsukuyomiPackage == null || importPlan == null || grpAppendPlan == null) {
            return null;
        }

        // Step 7-1: 先取出本次要迁移的源 waz。
        Waz sourceWaz = findRequiredSourceWaz(tsukuyomiPackage, request.getWazFileName());
        if (sourceWaz == null) {
            return null;
        }

        // Step 7-2: 把源侧索引和目标侧索引整理成 step7 专用上下文。
        TsukuyomiWazRebindContext context = buildContext(request, sourceWaz, importPlan, grpAppendPlan);
        BheResourceIndexResolver resolver = new BheResourceIndexResolver(commonProjectileAppendPlan, grpAppendPlan);

        // Step 7-3: 先创建目标 Waz 外壳，只保留最顶层公共信息。
        Waz targetWaz = createTargetWazShell(context);

        // Step 7-4: 再按 Waz -> Skill -> Phase -> Unit -> Object 的顺序逐层重建。
        targetWaz.setSkillList(rebuildSkillList(context, resolver));

        // Step 7-5: WAZ 结构重建完成后统一重编译 term，避免嵌套 CEvent 残留 BHE term 索引空间。
        termRewriter.rewrite(targetWaz, "tsukuyomi main WAZ " + request.getWazFileName());

        // Step 7-6: 对这一步已经明确会改的外部引用做结果校验。
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

        // BHE 私有辅助 WAZ 按整文件重绑，不和 baseline 同名 WAZ 做 skill 合并。
        // 因此 wazSequenceNo 保持源文件内部 skill index，AppendGrpEntriesStep 只提供 identity skill 映射。
        targetWaz.setSkillList(rebuildSkillList(context, resolver));
        termRewriter.rewrite(targetWaz, "tsukuyomi auxiliary WAZ " + targetWaz.getFileName());
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

        // 先装入源侧 Waz 顶层索引与文件名的对应关系。
        for (Map.Entry<String, Integer> entry : importPlan.getSourceWazIndexByFileName().entrySet()) {
            String normalizedFileName = normalizeFileName(entry.getKey());
            context.getSourceWazFileNameByGroupIndex().put(entry.getValue(), normalizedFileName);
        }

        // 再装入源侧 Sprite 顶层索引与文件名的对应关系。
        for (Map.Entry<String, Integer> entry : importPlan.getSourceSpriteIndexByFileName().entrySet()) {
            String normalizedFileName = normalizeFileName(entry.getKey());
            context.getSourceSpriteFileNameByGroupIndex().put(entry.getValue(), normalizedFileName);
        }

        // 这里先放入 BSDX 基线里已有的共享 waz 顶层索引。
        for (Map.Entry<String, Integer> entry : importPlan.getTargetWazIndexByFileName().entrySet()) {
            context.getTargetWazGroupIndexByFileName().put(normalizeFileName(entry.getKey()), entry.getValue());
        }

        // 再用 step4 的结果覆盖主 Tsukuyomi.waz 的目标索引。
        context.getTargetWazGroupIndexByFileName().put(
                normalizeFileName(request.getWazFileName()),
                grpAppendPlan.getWazaGroupIndex()
        );

        // 这里先放入 BSDX 基线里已有的共享 sprite 顶层索引。
        for (Map.Entry<String, Integer> entry : importPlan.getTargetSpriteIndexByFileName().entrySet()) {
            context.getTargetSpriteGroupIndexByFileName().put(normalizeFileName(entry.getKey()), entry.getValue());
        }

        // 再用 step4 的结果覆盖主 tsukuyomi.spm（对应 sakurabi 机体） 的目标索引。
        context.getTargetSpriteGroupIndexByFileName().put(
                normalizeFileName(request.getSpriteFileName()),
                grpAppendPlan.getSpriteGroupIndex()
        );

        // 把“源侧顶层序号 -> 目标顶层序号”的 waz 映射预先算好。
        buildWazIndexMap(context);

        // 把“源侧顶层序号 -> 目标顶层序号”的 sprite 映射预先算好。
        buildSpriteIndexMap(context);
        context.getSourceToTargetSeGroupIndex().putAll(grpAppendPlan.getSourceSeGroupIndexToTargetIndex());
        for (Map.Entry<Integer, Map<Integer, Integer>> entry : grpAppendPlan.getSourceSeItemIndexToTargetIndexByGroup().entrySet()) {
            context.getSourceToTargetSeItemIndexByGroup().put(entry.getKey(), new LinkedHashMap<>(entry.getValue()));
        }
        // BatVoiceGroup 源→目标映射：用于 CEventVoice 的 group 重绑
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

        // 这里按 skill 粒度重建整份 Tsukuyomi.waz。
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

        // 先复制 skill 级别的基础描述字段。
        target.setPhaseQuantity(source.getPhaseQuantity());
        target.setSkillNameJapanese(source.getSkillNameJapanese());
        target.setSkillNameEnglish(source.getSkillNameEnglish());

        // 再重建 phase 列表。
        target.setPhasesInfo(rebuildSkillPhaseList(source.getPhasesInfo(), context, resolver));

        // 最后重建 skill suffix 列表。
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

        // 一个 phase 里真正有语义的是 unit 列表，所以继续往下逐 unit 重建。
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

        // 先复制 unit 自身的标识信息。
        target.setUnitQuantity(source.getUnitQuantity());
        target.setUnitDescription(source.getUnitDescription());

        // 再逐个重建该 unit 里的对象列表。
        target.setSkillInfoObjectList(rebuildSkillInfoObjectList(source.getSkillInfoObjectList(), context, resolver));

        // unknown 列表本身不带外部文件号，但也要做结构级复制。
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

        // 这里先把已经确认带“外部文件索引”语义的对象单独拉出来处理。
        if (source instanceof CEventWazaSelect wazaSelect) {
            return rebuildCEventWazaSelect(wazaSelect, context, resolver);
        }
        if (source instanceof CEventSprite sprite) {
            return rebuildCEventSprite(sprite, context, resolver);
        }

        // 这两类虽然不改索引，但内部是 byte[] 列表，不能直接共享源数组引用。
        if (source instanceof CEventSe se) {
            return rebuildCEventSe(se, resolver);
        }
        if (source instanceof CEventVoice voice) {
            return rebuildCEventVoice(voice, context);
        }

        // WazInfoUnknown 自身不带子单元列表，直接复制即可。
        if (source instanceof SkillInfoUnknown unknown) {
            return rebuildSkillInfoUnknown(unknown);
        }

        // 像 CEventEffect / CEventEscape / CEventCamera 这类对象，
        // 内部会继续挂一层 unit -> data，需要递归重建 data。
        if (hasNestedUnitList(source.getClass())) {
            return rebuildNestedUnitObject(source, context, resolver);
        }

        // 剩下的是普通叶子对象，只需要复制它自身的字段。
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

        // 这里是真正的外部 waz 顶层索引重绑点。
        // 原字段指向 TSUKUYOMI 的 WazaGroup 顶层序号，迁移后必须改成 BSDX 目标序号。
        target.setWazFileNo(resolver.resolveWazGroupIndex(source.getWazFileNo()));

        // wazSequenceNo 是“目标 WAZ 内部 skill index”；BHE 整文件重绑时保持源文件内部 skill index。
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

        // CEventSprite 的 spmFileSequence 在存在非负值时，解释的是外部 sprite 顶层序号。
        // 所以这里也要按“源侧 sprite 索引 -> 目标 BSDX sprite 索引”去回写。
        target.setSpmFileSequence(resolver.resolveSpriteGroupIndex(source.getSpmFileSequence()));
        return target;
    }

    private CEventSe rebuildCEventSe(CEventSe source, BheResourceIndexResolver resolver) {
        CEventSe target = new CEventSe();
        BeanUtil.copyProperties(source, target);

        // 这里的 byteDataList 不能直接复用源数组引用，否则后续改写会串源对象。
        target.setByteDataList(deepCopyByteArrayList(source.getByteDataList()));
        rewriteSeTargets(target.getByteDataList(), resolver);
        return target;
    }

    private CEventVoice rebuildCEventVoice(CEventVoice source, TsukuyomiWazRebindContext context) {
        CEventVoice target = new CEventVoice();
        BeanUtil.copyProperties(source, target);

        // Voice 的内部 12-byte 段同样要做数组级复制。
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

        // 先把对象自身的普通字段复制过去，
        // 再把所有 *UnitList 里的 data 递归重建，避免内部仍然指着旧索引。
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

        // unit 自身大多只是 buffer / slot / description。
        // 真正带资源引用语义的是里面的 data，所以这里只递归替换 data。
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

    /**
     * 重写 CEventVoice 每个 12-byte 段的前 4 字节（BatVoiceGroup 全局索引）。
     * <p>
     * 逻辑与 {@link #rewriteSeTargets} 一致：先读源侧 group index，
     * 再从 sourceToTargetBatVoiceGroupIndex 映射表中查出目标 index 后覆写。
     * </p>
     */
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
                // -1 / 0xFFFFFFFF 表示无效/默认，跳过
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
            // 先尝试无参构造，失败后再尝试 int typeId 构造。
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
