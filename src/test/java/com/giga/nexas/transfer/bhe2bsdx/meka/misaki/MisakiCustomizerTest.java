package com.giga.nexas.transfer.bhe2bsdx.meka.misaki;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventEffect;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventValRandom;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.convert.waz.BheToBsdxWazConverter;
import com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize.FollowupMekaContext;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MisakiCustomizerTest {
    private static final String BED_JP = "◆みさき：床召還";
    private static final String BED_EN = "0304";
    private static final String WARM_JP = "◆みさき：あたためる";
    private static final String WARM_EN = "0297";
    @TempDir Path tempDir;

    @Test
    void fixesBothAppendBasesWithoutTouchingOldIndexDecoys() {
        for (int base : List.of(152, 300)) {
            Fixture f = fixture(base);
            MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
            assertEquals(1, count(f.host().getSkillList().get(base + 336)).getInt5());
            assertEquals(1, count(f.host().getSkillList().get(base + 339)).getInt5());
            for (int old : List.of(336, 339, 488, 491)) {
                if (old != base + 336 && old != base + 339) {
                    assertEquals(999, count(f.host().getSkillList().get(old)).getInt5(), "无关旧编号: " + old);
                }
            }
            assertEquals(999, count(f.source().getSkillList().get(336)).getInt5(), "公共源保持不变");
        }
    }

    @Test
    void sourceIdentityCanMoveWithinItsCommonWaz() {
        Fixture f = fixture(300);
        f.source().getSkillList().set(7, f.source().getSkillList().get(336));
        f.source().getSkillList().set(336, new Waz.Skill());
        f.host().getSkillList().set(307, emission(BED_JP, BED_EN, 999));
        f.host().getSkillList().set(636, emission("unrelated", BED_EN, 999));
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(1, count(f.host().getSkillList().get(307)).getInt5());
        assertEquals(999, count(f.host().getSkillList().get(636)).getInt5());
    }

    @Test
    void repeatedFixDoesNotChangeOtherCounts() {
        Fixture f = fixture(300);
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(1, count(f.host().getSkillList().get(636)).getInt5());
        assertEquals(1, count(f.host().getSkillList().get(639)).getInt5());
        assertEquals(999, count(f.host().getSkillList().get(488)).getInt5());
    }

    @Test
    void sharedConvertedSourceAndBaselineRemainIdempotent() {
        Fixture f = fixture(300);
        f.host().getSkillList().set(636, f.source().getSkillList().get(336));
        f.host().getSkillList().set(639, f.source().getSkillList().get(339));
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(1, count(f.host().getSkillList().get(636)).getInt5());
        assertEquals(1, count(f.host().getSkillList().get(639)).getInt5());
    }

    @Test
    void alreadyNormalSourceCannotGuessAnUnfixedTarget() {
        Fixture f = fixture(300);
        count(f.source().getSkillList().get(336)).setInt5(1);
        count(f.source().getSkillList().get(339)).setInt5(1);
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
        assertEquals(999, count(f.host().getSkillList().get(636)).getInt5());
        assertEquals(999, count(f.host().getSkillList().get(639)).getInt5());
    }

    @Test
    void previousResultSuppliesCommonSourceWithoutSelectedPackageGuessing() {
        Fixture f = fixture(300);
        var previous = new com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult();
        previous.setConvertedBundle(f.context().getConvertedBundle());
        var current = new TsukuyomiConvertedBundle();
        current.setCommonProjectileAppendPlan(previous.getConvertedBundle().getCommonProjectileAppendPlan());
        f.context().setConvertedBundle(current);
        var request = new com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftRequest();
        request.setPreviousCharacterResult(previous);
        f.context().setRequest(request);
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertEquals(1, count(f.host().getSkillList().get(636)).getInt5());
        assertEquals(1, count(f.host().getSkillList().get(639)).getInt5());
    }

    @Test
    void realMisakiRequestCannotSkipMissingCommonSource() {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setConvertedBundle(new TsukuyomiConvertedBundle());
        context.setRequest(new com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftRequest());
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(context));
    }

    @Test
    void unpreparedOptionalContextRecordsItsSkip() {
        FollowupMekaContext context = new FollowupMekaContext();
        context.setConvertedBundle(new TsukuyomiConvertedBundle());
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(context);
        assertEquals(1, context.getConvertedBundle().getNotes().size());
    }

    @Test
    void ambiguousSourceIdentityFailsBeforeAnyMutation() {
        Fixture f = fixture(300);
        f.source().getSkillList().set(8, emission(BED_JP, BED_EN, 999));
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
        assertEquals(999, count(f.host().getSkillList().get(636)).getInt5());
    }

    @Test
    void targetIdentityMismatchFailsBeforeAnyMutation() {
        Fixture f = fixture(300);
        f.host().getSkillList().get(639).setSkillNameJapanese("別の技能");
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
        assertEquals(999, count(f.host().getSkillList().get(636)).getInt5());
    }

    @Test
    void unexpectedTargetCountIsDiagnosedInsteadOfOverwritten() {
        Fixture f = fixture(300);
        count(f.host().getSkillList().get(639)).setInt5(7);
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
        assertEquals(999, count(f.host().getSkillList().get(636)).getInt5());
    }

    @Test
    void duplicateAnomalousSourceNodesAreRejected() {
        Fixture f = fixture(300);
        f.source().getSkillList().get(336).getPhasesInfo()
                .add(emission(BED_JP, BED_EN, 999).getPhasesInfo().get(0));
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
    }

    @Test
    void missingAppendPlanCannotFallBackToTargetNumbers() {
        Fixture f = fixture(300);
        f.context().getConvertedBundle().setCommonProjectileAppendPlan(null);
        assertThrows(IllegalStateException.class, () -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
        assertEquals(999, count(f.host().getSkillList().get(488)).getInt5());
    }

    @Test
    void realFollowupRawCommonSourceFixSurvivesBinaryRoundTrip() throws Exception {
        roundTripRealCommonSource(false);
    }

    @Test
    void realRawSignatureSurvivesSharedConvertedBaselineMutation() throws Exception {
        roundTripRealCommonSource(true);
    }

    @Test
    void realConvertedOnlySharedCommonWithNormalCountsRemainsIdempotent() throws Exception {
        var raw = (com.giga.nexas.dto.bhe.waz.Waz) new BheBinService().parse(
                "src/main/resources/game/bhe/waz/tama05.waz", "windows-31j").getData();
        Waz converted = new BheToBsdxWazConverter().convert(raw);
        Fixture f = fixture(300);
        f.context().getConvertedBundle().getCommonProjectileResourceBundle().getWazByFileName()
                .put("tama05.waz", converted);
        f.host().getSkillList().subList(300, f.host().getSkillList().size()).clear();
        f.host().getSkillList().addAll(converted.getSkillList());
        assertEquals(List.of(999, 1, 1), counts(converted.getSkillList().get(336)).stream()
                .map(CEventValRandom::getInt5).toList());
        assertEquals(List.of(1, 1, 999, 1), counts(converted.getSkillList().get(339)).stream()
                .map(CEventValRandom::getInt5).toList());
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        assertDoesNotThrow(() -> MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context()));
        assertEquals(List.of(1, 1, 1), counts(f.host().getSkillList().get(636)).stream()
                .map(CEventValRandom::getInt5).toList());
        assertEquals(List.of(1, 1, 1, 1), counts(f.host().getSkillList().get(639)).stream()
                .map(CEventValRandom::getInt5).toList());
    }

    private void roundTripRealCommonSource(boolean sharedConverted) throws Exception {
        var raw = (com.giga.nexas.dto.bhe.waz.Waz) new BheBinService().parse(
                "src/main/resources/game/bhe/waz/tama05.waz", "windows-31j").getData();
        assertEquals(BED_JP, raw.getSkillList().get(336).getSkillNameJapanese());
        assertEquals(BED_EN, raw.getSkillList().get(336).getSkillNameEnglish());
        assertEquals(WARM_JP, raw.getSkillList().get(339).getSkillNameJapanese());
        assertEquals(WARM_EN, raw.getSkillList().get(339).getSkillNameEnglish());
        Waz converted = new BheToBsdxWazConverter().convert(raw);
        Fixture f = fixture(300);
        f.context().getConvertedBundle().getCommonProjectileResourceBundle().getWazByFileName().clear();
        if (sharedConverted) {
            f.context().getConvertedBundle().getCommonProjectileResourceBundle().getWazByFileName()
                    .put("tama05.waz", converted);
        }
        f.context().getConvertedBundle().getRawSourceBundle()
                .getCommonProjectileWazByFileName().put("tama05.waz", raw);
        f.host().getSkillList().subList(300, f.host().getSkillList().size()).clear();
        f.host().getSkillList().addAll(converted.getSkillList());
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        MisakiCustomizer.INSTANCE.customizeBeforeWrite(f.context());
        Path output = tempDir.resolve("Host.waz");
        BsdxBinService bsdx = new BsdxBinService();
        f.host().setExtensionName("waz");
        bsdx.generate(output.toString(), f.host(), "windows-31j");
        Waz reparsed = (Waz) bsdx.parse(output.toString(), "windows-31j").getData();
        for (int index : List.of(636, 639)) {
            assertEquals(index == 636 ? 3 : 4, counts(reparsed.getSkillList().get(index)).size());
            assertTrue(counts(reparsed.getSkillList().get(index)).stream().allMatch(v -> v.getInt5() == 1));
        }
        assertEquals(1, raw.getSkillList().get(336).getPhasesInfo().stream()
                .flatMap(p -> p.getSkillUnitCollection().stream()).flatMap(u -> u.getSkillInfoObjectList().stream())
                .filter(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect.class::isInstance)
                .map(com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventEffect.class::cast)
                .flatMap(e -> e.getCeventEffectUnitList().stream())
                .filter(u -> Integer.valueOf(4).equals(u.getUnitSlotNum()))
                .map(u -> (com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.CEventValRandom) u.getData())
                .filter(v -> Integer.valueOf(999).equals(v.getInt5())).count(), "源保持999");
    }

    private Fixture fixture(int base) {
        Waz source = emptyWaz("tama05.waz", 387);
        source.getSkillList().set(336, emission(BED_JP, BED_EN, 999));
        source.getSkillList().set(339, emission(WARM_JP, WARM_EN, 999));
        Waz host = emptyWaz("Host.waz", base + 387);
        for (int old : List.of(336, 339, 488, 491)) {
            host.getSkillList().set(old, emission("無関係" + old, "DECOY" + old, 999));
        }
        host.getSkillList().set(base + 336, emission(BED_JP, BED_EN, 999));
        host.getSkillList().set(base + 339, emission(WARM_JP, WARM_EN, 999));
        FollowupMekaContext context = new FollowupMekaContext();
        context.setBaseline(new TsukuyomiBsdxBaselineBundle());
        context.getBaseline().getWazByFileName().put("Host.waz", host);
        context.setConvertedBundle(new TsukuyomiConvertedBundle());
        context.getConvertedBundle().getCommonProjectileResourceBundle().getWazByFileName().put("tama05.waz", source);
        var plan = context.getConvertedBundle().getCommonProjectileAppendPlan();
        plan.getSourceWazIndexToTargetIndex().put(5, 5);
        plan.getSourceWazIndexToTargetFileName().put(5, "Host.waz");
        plan.getSourceWazIndexToTargetSkillBase().put(5, base);
        plan.getSourceWazIndexToTargetSkillCount().put(5, 387);
        return new Fixture(context, source, host);
    }

    private Waz emptyWaz(String name, int size) {
        Waz waz = new Waz(name);
        for (int i = 0; i < size; i++) waz.getSkillList().add(new Waz.Skill());
        return waz;
    }

    private Waz.Skill emission(String japanese, String english, int value) {
        Waz.Skill skill = new Waz.Skill();
        skill.setSkillNameJapanese(japanese);
        skill.setSkillNameEnglish(english);
        Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
        SkillUnit unit = new SkillUnit(48, "特效生成");
        CEventEffect effect = new CEventEffect();
        CEventEffect.CEventEffectUnit countUnit = new CEventEffect.CEventEffectUnit();
        countUnit.setUnitSlotNum(4);
        countUnit.setBuffer(1);
        CEventValRandom random = new CEventValRandom();
        random.setStartFrame(0); random.setEndFrame(0);
        random.setInt1(0); random.setInt2(1); random.setInt3(0); random.setInt4(0);
        random.setInt5(value); random.setInt6(0);
        countUnit.setData(random);
        effect.getCeventEffectUnitList().add(countUnit);
        unit.getSkillInfoObjectList().add(effect);
        phase.getSkillUnitCollection().add(unit);
        skill.getPhasesInfo().add(phase);
        return skill;
    }

    private CEventValRandom count(Waz.Skill skill) {
        return counts(skill).get(0);
    }

    private List<CEventValRandom> counts(Waz.Skill skill) {
        return skill.getPhasesInfo().stream().flatMap(p -> p.getSkillUnitCollection().stream())
                .flatMap(u -> u.getSkillInfoObjectList().stream()).filter(CEventEffect.class::isInstance)
                .map(CEventEffect.class::cast).flatMap(e -> e.getCeventEffectUnitList().stream())
                .filter(u -> Integer.valueOf(4).equals(u.getUnitSlotNum()))
                .map(u -> (CEventValRandom) u.getData()).toList();
    }

    private record Fixture(FollowupMekaContext context, Waz source, Waz host) {}
}
