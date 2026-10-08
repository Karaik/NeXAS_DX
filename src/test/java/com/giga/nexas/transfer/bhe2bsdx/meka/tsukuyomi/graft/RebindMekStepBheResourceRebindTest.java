package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.giga.nexas.dto.bsdx.BsdxInfoCollection;
import com.giga.nexas.dto.bsdx.BsdxInfoCollectionAnalyzer;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bhe.spm.Spm;
import com.giga.nexas.dto.bhe.waz.Waz;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.BheCommonProjectileResources;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.CommonProjectileTestSupport;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.convert.ConvertBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.redirect.self.SelfRedirectBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.convert.mek.BheToBsdxMekConverter;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiConvertedBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiRawSourceBundle;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RebindMekStepBheResourceRebindTest {

    private static final int TARGET_WAZ_GROUP = 666;
    private static final int TARGET_SPRITE_GROUP = 777;
    private static final int TARGET_BAT_VOICE_GROUP = 888;
    private static final int PRIVATE_SE_GROUP_BASE = 900;
    private static final int PRIVATE_SE_ITEM_BASE = 3000;

    private final ObjectMapper mapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    private final BheToBsdxMekConverter mekConverter = new BheToBsdxMekConverter();
    private final RebindMekStep rebindMekStep = new RebindMekStep();
    private final ConvertBheCommonProjectileResourcesStep convertCommonStep =
            new ConvertBheCommonProjectileResourcesStep();
    private final SelfRedirectBheCommonProjectileResourcesStep selfRedirectCommonStep =
            new SelfRedirectBheCommonProjectileResourcesStep();

    @Test
    void explicitVoiceOverrideUsesBatVoiceMapInBothProductionPaths() {
        for (boolean followup : List.of(false, true)) {
            var fixture = animationFixture();
            var info = fixture.sourcePackage().getTsukuyomiMek().getMekVoiceInfo();
            info.setVersion(8);
            var explicit = new Mek.MekVoiceInfo.Entry(); explicit.setVoiceType(8); explicit.setGroupId(2); explicit.setWeight(100);
            var defaults = new Mek.MekVoiceInfo.Entry(); defaults.setVoiceType(-1); defaults.setGroupId(3); defaults.setWeight(100);
            info.setTable(List.of(List.of(List.of(explicit, defaults))));
            fixture.privatePlan().getSourceBatVoiceGroupIndexToTargetIndex().put(8, 42);
            Mek rebound = followup
                    ? new com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindMekStep().rebindFollowupMek(new TsukuyomiGraftRequest(), fixture.sourcePackage(), fixture.privatePlan(), fixture.commonPlan())
                    : rebindMekStep.rebindTsukuyomiMek(new TsukuyomiGraftRequest(), fixture.sourcePackage(), fixture.privatePlan(), fixture.commonPlan());
            assertEquals(42, rebound.getMekVoiceInfo().getVersion());
            var entries = rebound.getMekVoiceInfo().getTable().get(0).get(0);
            assertEquals(42, entries.get(0).getVoiceType()); assertEquals(2, entries.get(0).getGroupId());
            assertEquals(-1, entries.get(1).getVoiceType()); assertEquals(3, entries.get(1).getGroupId());
            assertEquals(8, explicit.getVoiceType());
        }
    }

    @Test
    void actualNaotoVoiceOverrideDoesNotPointAtVanillaGilberto() throws Exception {
        var raw = (com.giga.nexas.dto.bhe.mek.Mek) new com.giga.nexas.service.BheBinService().parse(
                "src/main/resources/game/bhe/mek/naoto/naoto.mek", "windows-31j").getData();
        Mek source = mekConverter.convert(raw);
        var common = buildActualCommonProjectilePlan();
        var plan = buildPrivatePlanFromActualMekMaterial(source, common);
        for (var material : source.getMekMaterialBlock().getEntries()) {
            for (int group = 0; group < material.getSpriteGroups().size(); group++) {
                if (!common.getSourceSpriteIndexToTargetIndex().containsKey(group)) {
                    plan.getSourceSpriteGroupIndexToTargetIndex().putIfAbsent(group, 2000 + group);
                }
            }
        }
        plan.getSourceBatVoiceGroupIndexToTargetIndex().put(8, TARGET_BAT_VOICE_GROUP);
        var pkg = new TsukuyomiPackageBundle(); pkg.setTsukuyomiMek(source);
        var rebound = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindMekStep()
                .rebindFollowupMek(new TsukuyomiGraftRequest(), pkg, plan, common);
        int count = 0;
        for (var row : rebound.getMekVoiceInfo().getTable()) for (var cell : row) for (var entry : cell) {
            assertEquals(TARGET_BAT_VOICE_GROUP, entry.getVoiceType()); count++;
        }
        assertEquals(23, count);
    }

    @Test
    void tsukuyomiEntryPointRebindsMaterialAnimationsWithoutMutatingSource() {
        AnimationFixture fixture = animationFixture();
        Mek rebound = rebindMekStep.rebindTsukuyomiMek(
                new TsukuyomiGraftRequest(), fixture.sourcePackage(), fixture.privatePlan(), fixture.commonPlan()
        );

        assertAnimationRebind(fixture, rebound);
    }

    @Test
    void followupEntryPointRebindsMaterialAnimationsWithoutMutatingSource() {
        AnimationFixture fixture = animationFixture();
        Mek rebound = new com.giga.nexas.transfer.bhe2bsdx.meka.followup.graft.RebindMekStep()
                .rebindFollowupMek(
                        new TsukuyomiGraftRequest(), fixture.sourcePackage(), fixture.privatePlan(), fixture.commonPlan()
                );

        assertAnimationRebind(fixture, rebound);
    }

    private AnimationFixture animationFixture() {
        Mek source = new Mek();
        source.setFileName("material-animation");
        source.setExtensionName("mek");
        source.getMekBasicInfo().setWazFileSequence(11);
        source.getMekBasicInfo().setSpmFileSequence(13);
        Mek.MekMaterialBlock.PluginEntry entry = new Mek.MekMaterialBlock.PluginEntry();
        entry.setSpriteGroups(new ArrayList<>());
        for (int i = 0; i <= 13; i++) {
            entry.getSpriteGroups().add(new int[0]);
        }
        entry.getSpriteGroups().set(2, null);
        entry.getSpriteGroups().set(3, new int[]{0, 10, 41});
        entry.getSpriteGroups().set(6, new int[]{0, 10, 41, -1});
        entry.getSpriteGroups().set(7, new int[]{0, 20, -2});
        entry.getSpriteGroups().set(13, new int[]{0, 10, 41, -1, -2});
        Mek.MekMaterialBlock.PluginEntry emptyEntry = new Mek.MekMaterialBlock.PluginEntry();
        source.getMekMaterialBlock().setEntries(new ArrayList<>(Arrays.asList(entry, null, emptyEntry)));
        source.getMekMaterialBlock().setRegularEntries(new ArrayList<>(List.of(entry)));
        source.getMekMaterialBlock().setTrailingEntries(new ArrayList<>(List.of(entry)));

        TsukuyomiPackageBundle sourcePackage = new TsukuyomiPackageBundle();
        sourcePackage.setTsukuyomiMek(source);
        TsukuyomiGrpAppendPlan privatePlan = new TsukuyomiGrpAppendPlan();
        privatePlan.setWazaGroupIndex(TARGET_WAZ_GROUP);
        privatePlan.setSpriteGroupIndex(22);
        privatePlan.getSourceSpriteGroupIndexToTargetIndex().put(13, 22);
        BheCommonProjectileAppendPlan commonPlan = new BheCommonProjectileAppendPlan();
        commonPlan.getSourceSpriteIndexToTargetIndex().put(3, 20);
        commonPlan.getSourceSpriteIndexToTargetActionGroupBase().put(3, 40);
        commonPlan.getSourceSpriteIndexToTargetIndex().put(6, 21);
        commonPlan.getSourceSpriteIndexToTargetActionGroupBase().put(6, 60);
        commonPlan.getSourceSpriteIndexToTargetIndex().put(7, 21);
        commonPlan.getSourceSpriteIndexToTargetActionGroupBase().put(7, 40);
        return new AnimationFixture(sourcePackage, privatePlan, commonPlan);
    }

    private void assertAnimationRebind(AnimationFixture fixture, Mek rebound) {
        Mek source = fixture.sourcePackage().getTsukuyomiMek();
        for (Mek.MekMaterialBlock.PluginEntry target : List.of(
                rebound.getMekMaterialBlock().getEntries().get(0),
                rebound.getMekMaterialBlock().getRegularEntries().get(0),
                rebound.getMekMaterialBlock().getTrailingEntries().get(0)
        )) {
            assertArrayEquals(new int[]{40, 50, 81}, groupAt(target.getSpriteGroups(), 20),
                    "公共 Smoke 动画必须加宿主原有的 40 个动画基址");
            assertArrayEquals(new int[]{60, 70, 101, -1, 40, -2}, groupAt(target.getSpriteGroups(), 21),
                    "多个源 group 合并时必须先映射动画，保留负哨兵并合并重复目标动画");
            assertArrayEquals(new int[]{0, 10, 41, -1, -2}, groupAt(target.getSpriteGroups(), 22),
                    "私有动画内部索引保持不变");
            assertTrue(isEmpty(groupAt(target.getSpriteGroups(), 2)), "null 源 group 不生成动画引用");
            assertNotSame(source.getMekMaterialBlock().getEntries().get(0), target);
            assertNotSame(source.getMekMaterialBlock().getEntries().get(0).getSpriteGroups().get(13),
                    target.getSpriteGroups().get(22));
        }
        assertEquals(null, rebound.getMekMaterialBlock().getEntries().get(1));
        assertTrue(rebound.getMekMaterialBlock().getEntries().get(2).getSpriteGroups().isEmpty());
        Mek.MekMaterialBlock.PluginEntry sourceEntry = source.getMekMaterialBlock().getEntries().get(0);
        assertArrayEquals(new int[]{0, 10, 41}, sourceEntry.getSpriteGroups().get(3));
        assertArrayEquals(new int[]{0, 10, 41, -1}, sourceEntry.getSpriteGroups().get(6));
        assertArrayEquals(new int[]{0, 20, -2}, sourceEntry.getSpriteGroups().get(7));
        assertArrayEquals(new int[]{0, 10, 41, -1, -2}, sourceEntry.getSpriteGroups().get(13));
    }

    @Test
    void rebindActualTsukuyomiMekMaterialThroughCommonAndPrivateResourcePlans() throws Exception {
        BheCommonProjectileAppendPlan commonPlan = buildActualCommonProjectilePlan();
        Mek sourceMek = readConvertedTsukuyomiMek();
        TsukuyomiGrpAppendPlan privatePlan = buildPrivatePlanFromActualMekMaterial(sourceMek, commonPlan);

        TsukuyomiPackageBundle sourcePackage = new TsukuyomiPackageBundle();
        sourcePackage.setTsukuyomiMek(sourceMek);
        Mek rebound = rebindMekStep.rebindTsukuyomiMek(
                new TsukuyomiGraftRequest(),
                sourcePackage,
                privatePlan,
                commonPlan
        );

        assertBasicResourceIndexes(rebound);
        assertWeaponSkillIndexesStayInsideWholeBheWaz(sourceMek, rebound);
        assertVoiceIndexes(sourceMek, rebound);
        assertMaterialSpriteGroups(sourceMek, rebound, commonPlan);
        assertMaterialSeGroups(sourceMek, rebound, commonPlan, privatePlan);
        assertMekAiTermsAreRecompiled(rebound);
    }

    private TsukuyomiGrpAppendPlan buildPrivatePlanFromActualMekMaterial(
            Mek sourceMek,
            BheCommonProjectileAppendPlan commonPlan
    ) {
        TsukuyomiGrpAppendPlan plan = new TsukuyomiGrpAppendPlan();
        plan.setWazaGroupIndex(TARGET_WAZ_GROUP);
        plan.setSpriteGroupIndex(TARGET_SPRITE_GROUP);
        plan.setBatVoiceGroupIndex(TARGET_BAT_VOICE_GROUP);
        plan.getSourceSpriteGroupIndexToTargetIndex().put(13, TARGET_SPRITE_GROUP);
        plan.getSourceBatVoiceGroupIndexToTargetIndex().put(1, TARGET_BAT_VOICE_GROUP);

        for (Pair pair : collectMaterialSePairs(sourceMek)) {
            String key = BheCommonProjectileAppendPlan.sePairKey(pair.groupIndex(), pair.itemIndex());
            if (commonPlan.getSourceSePairToTargetItemIndex().containsKey(key)) {
                continue;
            }
            plan.getSourceSeGroupIndexToTargetIndex().put(pair.groupIndex(), PRIVATE_SE_GROUP_BASE + pair.groupIndex());
            plan.getSourceSeItemIndexToTargetIndexByGroup()
                    .computeIfAbsent(pair.groupIndex(), ignored -> new LinkedHashMap<>())
                    .put(pair.itemIndex(), PRIVATE_SE_ITEM_BASE + pair.itemIndex());
        }
        return plan;
    }

    private void assertBasicResourceIndexes(Mek rebound) {
        assertEquals(TARGET_WAZ_GROUP, rebound.getMekBasicInfo().getWazFileSequence());
        assertEquals(TARGET_SPRITE_GROUP, rebound.getMekBasicInfo().getSpmFileSequence());
    }

    private void assertWeaponSkillIndexesStayInsideWholeBheWaz(Mek sourceMek, Mek rebound) throws Exception {
        Waz sourceWaz = read("src/main/resources/wazBheJson/tsukuyomi.waz.json", Waz.class);
        int skillCount = sourceWaz.getSkillList().size();
        for (Map.Entry<Integer, Mek.MekWeaponInfo> entry : sourceMek.getMekWeaponInfoMap().entrySet()) {
            Integer weaponIndex = entry.getKey();
            Integer sourceSequence = entry.getValue().getWazSequence();
            Integer reboundSequence = rebound.getMekWeaponInfoMap().get(weaponIndex).getWazSequence();
            assertEquals(sourceSequence, reboundSequence, "MEK weapon skill index 必须保持源 WAZ 内部同位语义");
            assertTrue(reboundSequence >= 0 && reboundSequence < skillCount,
                    "MEK weapon skill index 越界: weapon=" + weaponIndex + ", skill=" + reboundSequence);
        }
    }

    private void assertVoiceIndexes(Mek sourceMek, Mek rebound) {
        assertEquals(TARGET_BAT_VOICE_GROUP, rebound.getMekVoiceInfo().getVersion());
        assertEquals(
                flattenVoiceTableGroupIds(sourceMek.getMekVoiceInfo()),
                flattenVoiceTableGroupIds(rebound.getMekVoiceInfo()),
                "MekVoiceInfo.table.Entry.groupId 是 BatVoice 组内条目语义，不能改成顶层 BatVoiceGroup"
        );

        for (Mek.MekMaterialBlock.PluginEntry entry : allMaterialEntries(rebound)) {
            assertTrue(isEmpty(groupAt(entry.getVoiceGroups(), 1)), "源 BatVoiceGroup[1] 应被搬走");
            assertArrayEquals(
                    groupAt(findSourceEntry(sourceMek, entry.offset).getVoiceGroups(), 1),
                    groupAt(entry.getVoiceGroups(), TARGET_BAT_VOICE_GROUP),
                    "Material voice group 应搬到 Tsukuyomi 目标 BatVoiceGroup"
            );
        }
    }

    private void assertMaterialSpriteGroups(
            Mek sourceMek,
            Mek rebound,
            BheCommonProjectileAppendPlan commonPlan
    ) {
        Set<Integer> sourceSpriteGroups = collectNonEmptySpriteGroupIndexes(sourceMek);
        assertEquals(Set.of(0, 1, 3, 4, 5, 7, 9, 13), sourceSpriteGroups,
                "实际 Tsukuyomi MEK material 的 sprite 引用集合发生变化，必须先重新审计");
        assertEquals(40, commonPlan.getSourceSpriteIndexToTargetActionGroupBase().get(3),
                "实际 BSDX Smoke 宿主已有 40 个动画");

        for (Mek.MekMaterialBlock.PluginEntry targetEntry : allMaterialEntries(rebound)) {
            Mek.MekMaterialBlock.PluginEntry sourceEntry = findSourceEntry(sourceMek, targetEntry.offset);
            for (Integer sourceGroupIndex : sourceSpriteGroups) {
                int targetGroupIndex = sourceGroupIndex == 13
                        ? TARGET_SPRITE_GROUP
                        : commonPlan.getSourceSpriteIndexToTargetIndex().get(sourceGroupIndex);
                if (targetGroupIndex != sourceGroupIndex) {
                    assertTrue(isEmpty(groupAt(targetEntry.getSpriteGroups(), sourceGroupIndex)),
                            "源 SpriteGroup[" + sourceGroupIndex + "] 不能残留在目标 MEK material 原槽位");
                }
                int actionBase = sourceGroupIndex == 13
                        ? 0
                        : commonPlan.getSourceSpriteIndexToTargetActionGroupBase().get(sourceGroupIndex);
                int[] expectedActions = Arrays.stream(groupAt(sourceEntry.getSpriteGroups(), sourceGroupIndex))
                        .map(action -> action < 0 ? action : actionBase + action)
                        .toArray();
                assertArrayEquals(
                        expectedActions,
                        groupAt(targetEntry.getSpriteGroups(), targetGroupIndex),
                        "SpriteGroup[" + sourceGroupIndex + "] 应搬到目标 group " + targetGroupIndex
                                + " 且内部动画指向同一份追加后的动画身份"
                );
            }
        }
    }

    private void assertMaterialSeGroups(
            Mek sourceMek,
            Mek rebound,
            BheCommonProjectileAppendPlan commonPlan,
            TsukuyomiGrpAppendPlan privatePlan
    ) {
        Set<Pair> sourcePairs = collectMaterialSePairs(sourceMek);
        int commonPairCount = 0;
        int privatePairCount = 0;
        for (Pair sourcePair : sourcePairs) {
            String key = BheCommonProjectileAppendPlan.sePairKey(sourcePair.groupIndex(), sourcePair.itemIndex());
            if (commonPlan.getSourceSePairToTargetItemIndex().containsKey(key)) {
                commonPairCount++;
            } else {
                privatePairCount++;
            }
        }
        assertEquals(46, commonPairCount, "实际 MEK material 公共 SE pair 数发生变化，必须先重新审计");
        assertEquals(31, privatePairCount, "实际 MEK material 私有 SE pair 数发生变化，必须先重新审计");

        for (Mek.MekMaterialBlock.PluginEntry targetEntry : allMaterialEntries(rebound)) {
            Mek.MekMaterialBlock.PluginEntry sourceEntry = findSourceEntry(sourceMek, targetEntry.offset);
            for (Pair sourcePair : collectSePairs(sourceEntry.getSeGroups())) {
                String key = BheCommonProjectileAppendPlan.sePairKey(sourcePair.groupIndex(), sourcePair.itemIndex());
                int targetGroupIndex;
                int targetItemIndex;
                if (commonPlan.getSourceSePairToTargetItemIndex().containsKey(key)) {
                    targetGroupIndex = commonPlan.getCommonProjectileSeGroupIndex();
                    targetItemIndex = commonPlan.getSourceSePairToTargetItemIndex().get(key);
                } else {
                    targetGroupIndex = privatePlan.getSourceSeGroupIndexToTargetIndex().get(sourcePair.groupIndex());
                    targetItemIndex = privatePlan.getSourceSeItemIndexToTargetIndexByGroup()
                            .get(sourcePair.groupIndex())
                            .get(sourcePair.itemIndex());
                }
                assertContains(groupAt(targetEntry.getSeGroups(), targetGroupIndex), targetItemIndex,
                        "SE pair " + sourcePair + " 没有被搬到目标 group/item");
            }
        }
    }

    private void assertMekAiTermsAreRecompiled(Mek rebound) {
        List<BsdxInfoCollection> collections = new ArrayList<>();
        collectInfoCollections(rebound, new IdentityHashMap<>(), collections);
        assertEquals(62, collections.size(), "实际 Tsukuyomi MEK AI term 数发生变化，必须先重新审计");
        for (BsdxInfoCollection collection : collections) {
            assertTrue(BsdxInfoCollectionAnalyzer.analyze(collection).getWarnings().isEmpty(),
                    "MEK AI term 重编译后不应残留 analyzer warning");
        }
    }

    private BheCommonProjectileAppendPlan buildActualCommonProjectilePlan() throws Exception {
        TsukuyomiRawSourceBundle rawSourceBundle = loadRawSourceBundle();
        TsukuyomiBsdxBaselineBundle baseline = loadBsdxBaseline();
        TsukuyomiConvertedBundle convertedBundle = new TsukuyomiConvertedBundle();
        BheCommonProjectileAppendPlan appendPlan = new BheCommonProjectileAppendPlan();
        convertCommonStep.convert(rawSourceBundle, convertedBundle);
        selfRedirectCommonStep.redirect(null, rawSourceBundle, baseline, convertedBundle, appendPlan);
        return appendPlan;
    }

    private Mek readConvertedTsukuyomiMek() throws Exception {
        com.giga.nexas.dto.bhe.mek.Mek source =
                read("src/main/resources/mekBheJson/tsukuyomi.mek.json", com.giga.nexas.dto.bhe.mek.Mek.class);
        return mekConverter.convert(source);
    }

    private TsukuyomiRawSourceBundle loadRawSourceBundle() throws Exception {
        TsukuyomiRawSourceBundle bundle = new TsukuyomiRawSourceBundle();
        bundle.setBatVoiceGrp(read("src/main/resources/grpBheJson/batvoice.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.class));
        bundle.setWazaGroupGrp(read("src/main/resources/grpBheJson/wazagroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.class));
        bundle.setSpriteGroupGrp(read("src/main/resources/grpBheJson/spritegroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.class));
        bundle.setSeGroupGrp(read("src/main/resources/grpBheJson/segroup.grp.json",
                com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp.class));
        for (String fileName : BheCommonProjectileResources.COMMON_PROJECTILE_WAZ_FILES) {
            bundle.getCommonProjectileWazByFileName().put(fileName, readWaz(fileName));
        }
        for (String fileName : BheCommonProjectileResources.COMMON_PROJECTILE_SPM_FILES) {
            bundle.getCommonProjectileSpmByFileName().put(fileName, readSpm(fileName));
        }
        return bundle;
    }

    private TsukuyomiBsdxBaselineBundle loadBsdxBaseline() throws Exception {
        TsukuyomiBsdxBaselineBundle bundle = new TsukuyomiBsdxBaselineBundle();
        bundle.setWazaGroupGrp(read("src/main/resources/grpBsdxJson/WazaGroup.grp.json", WazaGroupGrp.class));
        bundle.setSpriteGroupGrp(read("src/main/resources/grpBsdxJson/SpriteGroup.grp.json", SpriteGroupGrp.class));
        bundle.setSeGroupGrp(read("src/main/resources/grpBsdxJson/SeGroup.grp.json", SeGroupGrp.class));
        bundle.setBatVoiceGrp(read("src/main/resources/grpBsdxJson/BatVoice.grp.json", BatVoiceGrp.class));
        bundle.setProgramMaterialGrp(read("src/main/resources/grpBsdxJson/ProgramMaterial.grp.json", ProgramMaterialGrp.class));
        bundle.setMapGroupGrp(read("src/main/resources/grpBsdxJson/MapGroup.grp.json", MapGroupGrp.class));
        CommonProjectileTestSupport.loadBsdxHostCommonResources(bundle);
        return bundle;
    }

    private Waz readWaz(String fileName) throws Exception {
        String baseName = fileName.substring(0, fileName.length() - ".waz".length());
        return read("src/main/resources/wazBheJson/" + baseName + ".waz.json", Waz.class);
    }

    private Spm readSpm(String fileName) throws Exception {
        return read("src/main/resources/spmBheJson/" + fileName + ".json", Spm.class);
    }

    private <T> T read(String path, Class<T> type) throws Exception {
        return mapper.readValue(Paths.get(path).toFile(), type);
    }

    private List<Mek.MekMaterialBlock.PluginEntry> allMaterialEntries(Mek mek) {
        List<Mek.MekMaterialBlock.PluginEntry> entries = new ArrayList<>();
        if (mek.getMekMaterialBlock() == null) {
            return entries;
        }
        addAll(entries, mek.getMekMaterialBlock().getEntries());
        addAll(entries, mek.getMekMaterialBlock().getRegularEntries());
        addAll(entries, mek.getMekMaterialBlock().getTrailingEntries());
        return entries;
    }

    private void addAll(
            List<Mek.MekMaterialBlock.PluginEntry> target,
            List<Mek.MekMaterialBlock.PluginEntry> source
    ) {
        if (source != null) {
            target.addAll(source);
        }
    }

    private Mek.MekMaterialBlock.PluginEntry findSourceEntry(Mek sourceMek, int offset) {
        for (Mek.MekMaterialBlock.PluginEntry entry : allMaterialEntries(sourceMek)) {
            if (entry != null && entry.offset == offset) {
                return entry;
            }
        }
        throw new IllegalStateException("找不到源 material entry: offset=" + offset);
    }

    private Set<Integer> collectNonEmptySpriteGroupIndexes(Mek mek) {
        Set<Integer> result = new LinkedHashSet<>();
        for (Mek.MekMaterialBlock.PluginEntry entry : allMaterialEntries(mek)) {
            if (entry == null || entry.getSpriteGroups() == null) {
                continue;
            }
            for (int i = 0; i < entry.getSpriteGroups().size(); i++) {
                if (!isEmpty(groupAt(entry.getSpriteGroups(), i))) {
                    result.add(i);
                }
            }
        }
        return result;
    }

    private Set<Pair> collectMaterialSePairs(Mek mek) {
        Set<Pair> result = new LinkedHashSet<>();
        for (Mek.MekMaterialBlock.PluginEntry entry : allMaterialEntries(mek)) {
            result.addAll(collectSePairs(entry.getSeGroups()));
        }
        return result;
    }

    private Set<Pair> collectSePairs(List<int[]> groups) {
        Set<Pair> result = new LinkedHashSet<>();
        if (groups == null) {
            return result;
        }
        for (int groupIndex = 0; groupIndex < groups.size(); groupIndex++) {
            for (int itemIndex : groupAt(groups, groupIndex)) {
                result.add(new Pair(groupIndex, itemIndex));
            }
        }
        return result;
    }

    private int[] groupAt(List<int[]> groups, int index) {
        if (groups == null || index < 0 || index >= groups.size() || groups.get(index) == null) {
            return new int[0];
        }
        return groups.get(index);
    }

    private boolean isEmpty(int[] values) {
        return values == null || values.length == 0;
    }

    private void assertContains(int[] values, int expected, String message) {
        assertTrue(Arrays.stream(values).anyMatch(value -> value == expected), message);
    }

    private List<Integer> flattenVoiceTableGroupIds(Mek.MekVoiceInfo voiceInfo) {
        List<Integer> result = new ArrayList<>();
        if (voiceInfo == null || voiceInfo.getTable() == null) {
            return result;
        }
        for (List<List<Mek.MekVoiceInfo.Entry>> row : voiceInfo.getTable()) {
            if (row == null) {
                continue;
            }
            for (List<Mek.MekVoiceInfo.Entry> cell : row) {
                if (cell == null) {
                    continue;
                }
                for (Mek.MekVoiceInfo.Entry entry : cell) {
                    if (entry != null) {
                        result.add(entry.getGroupId());
                    }
                }
            }
        }
        return result;
    }

    private void collectInfoCollections(
            Object node,
            IdentityHashMap<Object, Boolean> visited,
            List<BsdxInfoCollection> result
    ) {
        if (node == null || isLeaf(node)) {
            return;
        }
        if (visited.put(node, Boolean.TRUE) != null) {
            return;
        }
        if (node instanceof BsdxInfoCollection collection) {
            result.add(collection);
            return;
        }
        if (node instanceof List<?> list) {
            for (Object item : list) {
                collectInfoCollections(item, visited, result);
            }
            return;
        }
        if (node instanceof Map<?, ?> map) {
            for (Object value : map.values()) {
                collectInfoCollections(value, visited, result);
            }
            return;
        }
        if (node.getClass().isArray()) {
            return;
        }
        for (Field field : getAllFields(node.getClass())) {
            if (Modifier.isStatic(field.getModifiers()) || field.isSynthetic()) {
                continue;
            }
            try {
                field.setAccessible(true);
                collectInfoCollections(field.get(node), visited, result);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("读取字段失败: " + field.getName(), e);
            }
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(Arrays.asList(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private boolean isLeaf(Object value) {
        return value instanceof String
                || value instanceof Number
                || value instanceof Boolean
                || value instanceof Character
                || value.getClass().isEnum();
    }

    private record Pair(int groupIndex, int itemIndex) {
    }

    private record AnimationFixture(
            TsukuyomiPackageBundle sourcePackage,
            TsukuyomiGrpAppendPlan privatePlan,
            BheCommonProjectileAppendPlan commonPlan
    ) {
    }
}
