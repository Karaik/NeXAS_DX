package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 以公共源双名称与异常节点签名定位 Misaki 发射数，再通过追加计划修正最终公共宿主。
 * 原始公共 WAZ 提供只读签名，避免 converted 与 baseline 共享对象时重复调用失去定位依据。
 */
public final class MisakiSkillGlitchFixer {
    public static final MisakiSkillGlitchFixer INSTANCE = new MisakiSkillGlitchFixer();
    private static final String SOURCE_FILE = "tama05.waz";
    private static final List<Identity> IDENTITIES = List.of(
            new Identity("◆みさき：床召還", "0304"),
            new Identity("◆みさき：あたためる", "0297"));

    private MisakiSkillGlitchFixer() {
    }

    public void fix(FollowupMekaContext context) {
        if (context == null || context.getConvertedBundle() == null) {
            throw failure("缺少公共转换上下文");
        }
        TsukuyomiConvertedBundle bundle = context.getConvertedBundle();
        Source source = sourceFrom(bundle);
        if (context.getRequest() != null && context.getRequest().getPreviousCharacterResult() != null) {
            var previous = context.getRequest().getPreviousCharacterResult();
            Source inherited = sourceFrom(previous.getConvertedBundle());
            if (source.converted() == null && source.raw() == null) {
                source = inherited;
            }
            if (source.raw() == null && previous.getRawSourceBundle() != null) {
                source = new Source(source.converted(), uniqueFile(
                        previous.getRawSourceBundle().getCommonProjectileWazByFileName(), SOURCE_FILE));
            }
            if (source.raw() == null && inherited.raw() != null) {
                source = new Source(source.converted(), inherited.raw());
            }
        }

        BheCommonProjectileAppendPlan plan = bundle.getCommonProjectileAppendPlan();
        int sourceGroup = BheCommonProjectileResources.commonProjectileWazFiles().indexOf(SOURCE_FILE);
        if (source.converted() == null && source.raw() == null) {
            boolean required = context.getRequest() != null
                    && "MISAKI".equalsIgnoreCase(context.getRequest().getMekaCodeName());
            boolean planned = plan != null && plan.getSourceWazIndexToTargetSkillBase().containsKey(sourceGroup);
            if (required || planned) {
                throw failure("缺少 " + SOURCE_FILE + " 公共源，不能按目标编号猜测");
            }
            bundle.getNotes().add("misaki emission: 未准备对应公共源及追加段，跳过修复");
            return;
        }
        if (plan == null || context.getBaseline() == null) {
            throw failure("缺少公共追加计划或目标基线");
        }
        String targetFile = plan.getSourceWazIndexToTargetFileName().get(sourceGroup);
        Integer base = plan.getSourceWazIndexToTargetSkillBase().get(sourceGroup);
        Integer count = plan.getSourceWazIndexToTargetSkillCount().get(sourceGroup);
        Integer targetGroup = plan.getSourceWazIndexToTargetIndex().get(sourceGroup);
        if (targetFile == null || base == null || base < 0 || count == null || count <= 0
                || targetGroup == null || targetGroup < 0) {
            throw failure("公共源 " + SOURCE_FILE + " 的宿主/base/count/group 映射不完整");
        }
        Waz target = uniqueFile(context.getBaseline().getWazByFileName(), targetFile);
        if (target == null || target.getSkillList() == null
                || (long) base + count > target.getSkillList().size()) {
            throw failure("目标公共宿主 " + targetFile + " 追加段越界或缺失");
        }

        // 先验证两个目标，全部成功后才改值，不能留下半修复状态。
        List<CEventValRandom> targetValues = new ArrayList<>();
        for (Identity identity : IDENTITIES) {
            Located located = locate(source, identity);
            int sourceIndex = located.index();
            if (sourceIndex >= count) {
                throw failure(identity + " 源技能不在计划追加段内: " + sourceIndex);
            }
            int targetIndex = base + sourceIndex;
            Waz.Skill targetSkill = target.getSkillList().get(targetIndex);
            if (targetSkill == null || !identity.matches(
                    targetSkill.getSkillNameJapanese(), targetSkill.getSkillNameEnglish())) {
                throw failure(targetFile + "[" + targetIndex + "] 双名称不匹配 " + identity);
            }
            if (located.site() == null) {
                verifyAlreadyNormal(source.converted().getSkillList().get(sourceIndex), targetSkill, identity);
                continue;
            }
            CEventValRandom value = targetNode(targetSkill, located.site());
            if (!located.site().signature().sameExceptCount(signature(value))
                    || (!Objects.equals(value.getInt5(), 999) && !Objects.equals(value.getInt5(), 1))) {
                throw failure(targetFile + "[" + targetIndex + "] Unit4 签名或原值不匹配: " + value.getInt5());
            }
            targetValues.add(value);
        }
        targetValues.forEach(value -> value.setInt5(1));
    }

    private Source sourceFrom(TsukuyomiConvertedBundle bundle) {
        if (bundle == null) return new Source(null, null);
        Waz converted = bundle.getCommonProjectileResourceBundle() == null ? null
                : uniqueFile(bundle.getCommonProjectileResourceBundle().getWazByFileName(), SOURCE_FILE);
        var raw = bundle.getRawSourceBundle() == null ? null
                : uniqueFile(bundle.getRawSourceBundle().getCommonProjectileWazByFileName(), SOURCE_FILE);
        return new Source(converted, raw);
    }

    private Located locate(Source source, Identity identity) {
        int convertedIndex = source.converted() == null ? -1 : uniqueConvertedSkill(source.converted(), identity);
        if (source.raw() != null) {
            int rawIndex = -1;
            var skills = source.raw().getSkillList();
            for (int i = 0; i < skills.size(); i++) {
                var skill = skills.get(i);
                if (skill != null && identity.matches(skill.getSkillNameJapanese(), skill.getSkillNameEnglish())) {
                    if (rawIndex >= 0) throw failure("公共原始源双名称重复: " + identity);
                    rawIndex = i;
                }
            }
            if (rawIndex < 0 || (convertedIndex >= 0 && convertedIndex != rawIndex)) {
                throw failure("公共源身份缺失或原始/转换索引不一致: " + identity);
            }
            List<Site> sites = new ArrayList<>();
            var skill = skills.get(rawIndex);
            for (int p = 0; p < skill.getPhasesInfo().size(); p++) {
                var phase = skill.getPhasesInfo().get(p);
                for (var unit : phase.getSkillUnitCollection()) {
                    if (!Objects.equals(unit.getUnitQuantity(), 54)) continue;
                    int ordinal = 0;
                    for (var object : unit.getSkillInfoObjectList()) {
                        if (!(object instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect effect)) continue;
                        for (var child : effect.getCeventEffectUnitList()) {
                            if (Objects.equals(child.getUnitSlotNum(), 4)
                                    && child.getBuffer() != null && child.getBuffer() != 0
                                    && child.getData() instanceof com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventValRandom value) {
                                Signature s = new Signature(value.getStartFrame(), value.getEndFrame(), value.getInt1(),
                                        value.getInt2(), value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6());
                                if (s.isEmission(999)) sites.add(new Site(p, ordinal, s));
                            }
                        }
                        ordinal++;
                    }
                }
            }
            return new Located(rawIndex, uniqueSite(sites, identity));
        }
        var skill = source.converted().getSkillList().get(convertedIndex);
        List<Site> anomalous = convertedSites(skill, 999);
        // 已修复的共享源无法从多个正常1节点中还原原异常位置。
        // 只验证源/目标全部对应发射节点都正常，不猜节点、不再改值。
        return new Located(convertedIndex, anomalous.isEmpty() ? null : uniqueSite(anomalous, identity));
    }

    private void verifyAlreadyNormal(Waz.Skill source, Waz.Skill target, Identity identity) {
        List<Site> sourceNodes = allCountSites(source);
        List<Site> targetNodes = allCountSites(target);
        if (sourceNodes.isEmpty() || sourceNodes.size() != targetNodes.size()) {
            throw failure(identity + " 已修复源/目标的Unit4结构不一致");
        }
        for (int i = 0; i < sourceNodes.size(); i++) {
            Site a = sourceNodes.get(i);
            Site b = targetNodes.get(i);
            if (a.phase() != b.phase() || a.effectOrdinal() != b.effectOrdinal()
                    || !Objects.equals(a.signature().count(), 1)
                    || !Objects.equals(b.signature().count(), 1)
                    || !a.signature().sameExceptCount(b.signature())) {
                throw failure(identity + " 已修复源/目标的Unit4值或签名不一致");
            }
        }
    }

    private List<Site> allCountSites(Waz.Skill skill) {
        List<Site> sites = new ArrayList<>();
        for (int p = 0; p < skill.getPhasesInfo().size(); p++) {
            for (SkillUnit unit : skill.getPhasesInfo().get(p).getSkillUnitCollection()) {
                if (!Objects.equals(unit.getUnitQuantity(), 48)) continue;
                int ordinal = 0;
                for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                    if (!(object instanceof CEventEffect effect)) continue;
                    int matches = 0;
                    for (var child : effect.getCeventEffectUnitList()) {
                        if (!Objects.equals(child.getUnitSlotNum(), 4)
                                || child.getBuffer() == null || child.getBuffer() == 0) continue;
                        if (++matches > 1 || !(child.getData() instanceof CEventValRandom value)) {
                            throw failure("已修复公共源/目标的Effect Unit4类型错误或重复");
                        }
                        sites.add(new Site(p, ordinal, signature(value)));
                    }
                    ordinal++;
                }
            }
        }
        return sites;
    }

    private int uniqueConvertedSkill(Waz waz, Identity identity) {
        int index = -1;
        for (int i = 0; i < waz.getSkillList().size(); i++) {
            Waz.Skill skill = waz.getSkillList().get(i);
            if (skill != null && identity.matches(skill.getSkillNameJapanese(), skill.getSkillNameEnglish())) {
                if (index >= 0) throw failure("公共转换源双名称重复: " + identity);
                index = i;
            }
        }
        if (index < 0) throw failure("公共转换源缺少双名称: " + identity);
        return index;
    }

    private List<Site> convertedSites(Waz.Skill skill, int count) {
        List<Site> sites = new ArrayList<>();
        for (int p = 0; p < skill.getPhasesInfo().size(); p++) {
            for (SkillUnit unit : skill.getPhasesInfo().get(p).getSkillUnitCollection()) {
                if (!Objects.equals(unit.getUnitQuantity(), 48)) continue;
                int ordinal = 0;
                for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                    if (!(object instanceof CEventEffect effect)) continue;
                    for (var child : effect.getCeventEffectUnitList()) {
                        if (Objects.equals(child.getUnitSlotNum(), 4)
                                && child.getBuffer() != null && child.getBuffer() != 0
                                && child.getData() instanceof CEventValRandom value
                                && signature(value).isEmission(count)) {
                            sites.add(new Site(p, ordinal, signature(value)));
                        }
                    }
                    ordinal++;
                }
            }
        }
        return sites;
    }

    private Site uniqueSite(List<Site> sites, Identity identity) {
        if (sites.size() != 1) throw failure(identity + " Unit4 原异常签名命中数必须为1，实际 " + sites.size());
        return sites.get(0);
    }

    private CEventValRandom targetNode(Waz.Skill skill, Site site) {
        if (site.phase() >= skill.getPhasesInfo().size()) throw failure("目标 phase 缺失");
        List<CEventEffect> effects = new ArrayList<>();
        for (SkillUnit unit : skill.getPhasesInfo().get(site.phase()).getSkillUnitCollection()) {
            if (!Objects.equals(unit.getUnitQuantity(), 48)) continue;
            for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                if (object instanceof CEventEffect effect) effects.add(effect);
            }
        }
        if (site.effectOrdinal() >= effects.size()) throw failure("目标 Effect 节点缺失");
        List<CEventValRandom> values = new ArrayList<>();
        for (var child : effects.get(site.effectOrdinal()).getCeventEffectUnitList()) {
            if (Objects.equals(child.getUnitSlotNum(), 4) && child.getBuffer() != null && child.getBuffer() != 0
                    && child.getData() instanceof CEventValRandom value) values.add(value);
        }
        if (values.size() != 1) throw failure("目标 Effect Unit4 缺失或重复");
        return values.get(0);
    }

    private Signature signature(CEventValRandom value) {
        return new Signature(value.getStartFrame(), value.getEndFrame(), value.getInt1(), value.getInt2(),
                value.getInt3(), value.getInt4(), value.getInt5(), value.getInt6());
    }

    private <T> T uniqueFile(Map<String, T> files, String fileName) {
        if (files == null) return null;
        T found = null;
        boolean matched = false;
        for (var entry : files.entrySet()) {
            if (entry.getKey() != null && entry.getKey().trim().equalsIgnoreCase(fileName)) {
                if (matched) throw failure("公共文件名匹配不唯一: " + fileName);
                matched = true;
                found = entry.getValue();
            }
        }
        return found;
    }

    private IllegalStateException failure(String detail) {
        return new IllegalStateException("Misaki 发射数修复失败: " + detail);
    }

    private record Source(Waz converted, com.giga.nexas.dto.bhe.waz.Waz raw) {}
    private record Located(int index, Site site) {}
    private record Site(int phase, int effectOrdinal, Signature signature) {}
    private record Identity(String japanese, String english) {
        boolean matches(String jp, String en) {
            return japanese.equals(jp) && english.equals(en);
        }
    }
    private record Signature(Integer start, Integer end, Integer a, Integer b, Integer c,
                             Integer d, Integer count, Integer f) {
        boolean isEmission(int value) {
            return Objects.equals(start, 0) && Objects.equals(end, 0)
                    && Objects.equals(a, 0) && Objects.equals(b, 1) && Objects.equals(c, 0)
                    && Objects.equals(d, 0) && Objects.equals(count, value) && Objects.equals(f, 0);
        }
        boolean sameExceptCount(Signature other) {
            return Objects.equals(start, other.start) && Objects.equals(end, other.end)
                    && Objects.equals(a, other.a) && Objects.equals(b, other.b)
                    && Objects.equals(c, other.c) && Objects.equals(d, other.d) && Objects.equals(f, other.f);
        }
    }
}
