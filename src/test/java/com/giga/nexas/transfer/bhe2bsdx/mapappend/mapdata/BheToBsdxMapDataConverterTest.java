package com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata;

import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.resource.BheMapResourceRewriteTable;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheToBsdxMapDataConverterTest {

    @Test
    void syntheticMapConvertsInMemoryWithResourceRewriteAndScriptRebucket() {
        BheMapEntryPlan entryPlan = entryPlan("synthetic.map");
        com.giga.nexas.dto.bhe.map.MapData source = syntheticMapData();
        BheMapResourceRewriteTable rewriteTable = rewriteTable(
                found(BheMapReferenceType.FOREGROUND, "fg/source.png", "fg/bhe_source.png"),
                found(BheMapReferenceType.SPRITE_MAP, "spm/source.spm", "spm/bhe_source.spm"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot0.png", "slot/bhe_slot0.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot1.png", "slot/bhe_slot1.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot2.png", "slot/bhe_slot2.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot3.png", "slot/bhe_slot3.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot4.png", "slot/bhe_slot4.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot5.png", "slot/bhe_slot5.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot6.png", "slot/bhe_slot6.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot7.png", "slot/bhe_slot7.png")
        );

        BheMapDataConversionResult result = new BheToBsdxMapDataConverter().convert(entryPlan, source, rewriteTable);

        assertTrue(result.isConverted());
        assertTrue(result.getBlockingIssues().isEmpty());
        com.giga.nexas.dto.bsdx.map.MapData target = result.getConvertedMapData();
        assertNotNull(target);
        assertEquals(BheToBsdxMapDataConverter.BSDX_MAP_MAGIC, target.getMagic());
        assertEquals(source.getWidth(), target.getWidth());
        assertEquals(source.getHeight(), target.getHeight());
        assertEquals(source.getTileRecords().size(), target.getTileRecords().size());
        assertEquals("fg/bhe_source.png", target.getForegroundImage());
        assertEquals("spm/bhe_source.spm", target.getSpriteMap());

        assertEquals(8, target.getNamedResourceSlotBlocks().size());
        assertEquals("slot/bhe_slot0.png", target.getNamedResourceSlotBlocks().get(0).getSlotText());
        assertEquals(100, target.getNamedResourceSlotBlocks().get(0).getParam0());
        assertEquals(104, target.getNamedResourceSlotBlocks().get(0).getParam4());
        assertEquals("slot/bhe_slot7.png", target.getNamedResourceSlotBlocks().get(7).getSlotText());
        assertEquals(7, target.getNamedResourceSlotBlocks().get(7).getSlotNum());

        assertEquals(9, target.getScriptEntryGroupBlocks().size());
        assertEquals(1, target.getScriptEntryGroupBlocks().get(0).getEntries().size());
        assertEquals(11, target.getScriptEntryGroupBlocks().get(0).getEntries().get(0).getTypeId());
        assertEquals(111, target.getScriptEntryGroupBlocks().get(0).getEntries().get(0).getX());
        assertEquals(222, target.getScriptEntryGroupBlocks().get(0).getEntries().get(0).getY());
        assertEquals(1, target.getScriptEntryGroupBlocks().get(3).getEntries().size());
        assertEquals(33, target.getScriptEntryGroupBlocks().get(3).getEntries().get(0).getTypeId());
        assertEquals(2, result.getAudit().getRebucketedScriptEntryCount());
        assertEquals(Map.of(0, 1, 3, 1), result.getAudit().getTargetScriptGroupEntryCounts());
    }

    @Test
    void unsupportedGroupIndexBlocksConversionWithoutOutputMapData() {
        BheMapEntryPlan entryPlan = entryPlan("unsupported-group.map");
        com.giga.nexas.dto.bhe.map.MapData source = syntheticMapData();
        source.getScriptEntryGroupBlocks().get(0).getEntries().add(scriptEntry(9, 99, 900, 901));
        source.getScriptEntryGroupBlocks().get(0).setCount(3);

        BheMapDataConversionResult result = new BheToBsdxMapDataConverter()
                .convert(entryPlan, source, completeRewriteTable());

        assertNull(result.getConvertedMapData());
        BheMapDataConversionIssue issue = onlyIssue(result, BheMapDataConversionIssueType.UNSUPPORTED_SCRIPT_GROUP_INDEX);
        assertEquals("unsupported-group.map", issue.getSourceMapFileName());
        assertEquals(9, issue.getGroupIndex());
        assertEquals(99, issue.getTypeId());
        assertEquals(900, issue.getX());
        assertEquals(901, issue.getY());
    }

    @Test
    void missingFinalReferenceBlocksConversionWithoutCheckingTargetFileMaterialization() {
        BheMapEntryPlan entryPlan = entryPlan("missing-reference.map");
        com.giga.nexas.dto.bhe.map.MapData source = syntheticMapData();
        BheMapResourceRewriteTable rewriteTable = completeRewriteTable();
        rewriteTable.put(missing(BheMapReferenceType.FOREGROUND, "fg/source.png", "fg/bhe_source.png"));

        BheMapDataConversionResult result = new BheToBsdxMapDataConverter().convert(entryPlan, source, rewriteTable);

        assertNull(result.getConvertedMapData());
        BheMapDataConversionIssue issue = onlyIssue(result, BheMapDataConversionIssueType.MISSING_FINAL_REFERENCE);
        assertEquals("missing-reference.map", issue.getSourceMapFileName());
        assertEquals(BheMapReferenceType.FOREGROUND, issue.getReferenceType());
        assertEquals("fg/source.png", issue.getReferenceText());
    }

    @Test
    void multipleSpriteMapListBlocksConversionWithoutChoosingFirstItem() {
        BheMapEntryPlan entryPlan = entryPlan("multiple-sprite.map");
        com.giga.nexas.dto.bhe.map.MapData source = syntheticMapData();
        source.setSpriteMap(null);
        source.getSpriteMapList().add("spm/a.spm");
        source.getSpriteMapList().add("spm/b.spm");

        BheMapDataConversionResult result = new BheToBsdxMapDataConverter()
                .convert(entryPlan, source, completeRewriteTable());

        assertNull(result.getConvertedMapData());
        BheMapDataConversionIssue issue = onlyIssue(result, BheMapDataConversionIssueType.UNSUPPORTED_MULTIPLE_SPRITE_MAPS);
        assertEquals("multiple-sprite.map", issue.getSourceMapFileName());
        assertEquals(BheMapReferenceType.SPRITE_MAP_LIST, issue.getReferenceType());
        assertEquals("spm/a.spm, spm/b.spm", issue.getReferenceText());
    }

    @Test
    void incompleteLocalResourceRootCharacterizesMissingFinalReferenceInputs() throws Exception {
        BheMapAppendRequest request = new BheMapAppendRequest();
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        Path localResourceRoot = Paths.get("src/main/resources/game/bhe");
        request.setBheStaticResourceRoot(localResourceRoot);
        BheMapAppendPlan plan = new BheMapAppendPlanBuilder().build(
                sourceCatalog,
                request.resolveBheMapDir(),
                request.resolveBheStaticResourceRoot(),
                new BsdxMapBaseline(),
                request.getCharset(),
                new BheMapAppendAudit()
        );

        LocalResourceRootAudit rootAudit = auditLocalResourceRoot(request.resolveBheStaticResourceRoot());
        PlanReferenceAudit planAudit = auditPlanResourceReferences(plan);
        RealCandidateStats stats = convertRealCandidates(plan, request.getCharset());

        assertEquals(142, stats.totalPlanEntries);
        assertEquals(0, stats.conversionSuccessCount);
        assertEquals(stats.totalPlanEntries, stats.blockedByMissingFinalReferenceCount);
        assertTrue(stats.blockedByMultipleSpriteMapListCount > 0);
        assertEquals(0, stats.issueTypeCount(BheMapDataConversionIssueType.MISSING_RESOURCE_REWRITE));
        assertEquals(142, stats.missingFinalReferenceTypeCount(BheMapReferenceType.FOREGROUND));
        assertEquals(74, stats.missingFinalReferenceTypeCount(BheMapReferenceType.RESOURCE_SLOT));
        assertEquals(0, stats.missingFinalReferenceTypeCount(BheMapReferenceType.SPRITE_MAP));
        assertEquals(0, stats.missingFinalReferenceTypeCount(BheMapReferenceType.SPRITE_MAP_LIST));
        assertEquals(stats.totalPlanEntries, stats.conversionSuccessCount + stats.blockedMapCount());
        assertTrue(rootAudit.spmFileCount > 0);
        assertEquals(0, rootAudit.imageFileCount);
        assertEquals(855, planAudit.totalResourceReferences);
        assertEquals(634, planAudit.statusCount(BheMapReferenceStatus.FOUND_DEFERRED));
        assertEquals(221, planAudit.statusCount(BheMapReferenceStatus.MISSING_NON_BLOCKING));
        assertTrue(planAudit.missingCount(BheMapReferenceType.FOREGROUND) > 0);
        assertTrue(planAudit.missingCount(BheMapReferenceType.RESOURCE_SLOT) > 0);
        assertTrue(planAudit.foundCount(BheMapReferenceType.SPRITE_MAP)
                + planAudit.foundCount(BheMapReferenceType.SPRITE_MAP_LIST) > 0);
        assertEquals(142, planAudit.missingCount(BheMapReferenceType.FOREGROUND));
        assertEquals(74, planAudit.missingCount(BheMapReferenceType.RESOURCE_SLOT));
        assertEquals(634, planAudit.foundCount(BheMapReferenceType.SPRITE_MAP_LIST));
        assertEquals(5, planAudit.missingCount(BheMapReferenceType.SPRITE_MAP_LIST));
        assertEquals(0, planAudit.blankSourceTextCount);
        assertEquals(0, planAudit.blankTargetTextCount);
        assertEquals(stats.lookupAudit.totalLookups, stats.lookupAudit.matchedLookups);
        assertEquals(0, stats.lookupAudit.planNotCollectedCount);
        assertEquals(0, stats.lookupAudit.referenceTypeMismatchCount);
        assertEquals(0, stats.lookupAudit.sourceTextMismatchCount);

        printLocalResourceRootCharacterization(rootAudit, planAudit, stats);
    }

    @Test
    void completeStaticResourceRootCharacterizesMapDataConversionCandidates() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "需要通过 -Dbhe.staticResourceRoot 或 BHE_STATIC_RESOURCE_ROOT 显式提供完整 BHE 静态资源根"
        );

        BheMapAppendRequest request = new BheMapAppendRequest();
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        request.setBheStaticResourceRoot(completeResourceRoot);
        BheMapAppendPlan plan = new BheMapAppendPlanBuilder().build(
                sourceCatalog,
                request.resolveBheMapDir(),
                request.resolveBheStaticResourceRoot(),
                new BsdxMapBaseline(),
                request.getCharset(),
                new BheMapAppendAudit()
        );

        LocalResourceRootAudit rootAudit = auditLocalResourceRoot(request.resolveBheStaticResourceRoot());
        PlanReferenceAudit planAudit = auditPlanResourceReferences(plan);
        RealCandidateStats stats = convertRealCandidates(plan, request.getCharset());

        assertEquals(142, stats.totalPlanEntries);
        assertTrue(rootAudit.imageFileCount > 0);
        assertEquals(855, planAudit.totalResourceReferences);
        assertEquals(850, planAudit.statusCount(BheMapReferenceStatus.FOUND_DEFERRED));
        assertEquals(5, planAudit.statusCount(BheMapReferenceStatus.MISSING_NON_BLOCKING));
        assertEquals(142, planAudit.foundCount(BheMapReferenceType.FOREGROUND));
        assertEquals(74, planAudit.foundCount(BheMapReferenceType.RESOURCE_SLOT));
        assertEquals(0, planAudit.missingCount(BheMapReferenceType.FOREGROUND));
        assertEquals(0, planAudit.missingCount(BheMapReferenceType.RESOURCE_SLOT));
        assertEquals(33, stats.conversionSuccessCount);
        assertEquals(7, stats.blockedByGroupIndex9MapCount);
        assertEquals(109, stats.blockedByMultipleSpriteMapListCount);
        assertEquals(0, stats.blockedByMissingFinalReferenceCount);
        assertEquals(38, stats.issueTypeCount(BheMapDataConversionIssueType.UNSUPPORTED_SCRIPT_GROUP_INDEX));
        assertEquals(109, stats.issueTypeCount(BheMapDataConversionIssueType.UNSUPPORTED_MULTIPLE_SPRITE_MAPS));
        assertEquals(0, stats.issueTypeCount(BheMapDataConversionIssueType.MISSING_FINAL_REFERENCE));
        assertEquals(0, stats.issueTypeCount(BheMapDataConversionIssueType.MISSING_RESOURCE_REWRITE));
        assertEquals(855, stats.lookupAudit.totalLookups);
        assertEquals(855, stats.lookupAudit.matchedLookups);
        assertEquals(0, stats.lookupAudit.planNotCollectedCount);
        assertEquals(0, stats.lookupAudit.referenceTypeMismatchCount);
        assertEquals(0, stats.lookupAudit.sourceTextMismatchCount);

        printCompleteResourceRootCharacterization(rootAudit, planAudit, stats);
    }

    private RealCandidateStats convertRealCandidates(BheMapAppendPlan plan, String charset) throws Exception {
        RealCandidateStats stats = new RealCandidateStats();
        BheBinService bheBinService = new BheBinService();
        BheToBsdxMapDataConverter converter = new BheToBsdxMapDataConverter();
        stats.totalPlanEntries = plan.getEntries().size();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            com.giga.nexas.dto.bhe.map.MapData source =
                    (com.giga.nexas.dto.bhe.map.MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            BheMapResourceRewriteTable rewriteTable = BheMapResourceRewriteTable.fromEntryPlan(entry);
            auditRewriteLookups(stats.lookupAudit, entry, source, rewriteTable);
            BheMapDataConversionResult result = converter.convert(
                    entry,
                    source,
                    rewriteTable
            );
            if (result.isConverted()) {
                stats.conversionSuccessCount++;
                continue;
            }
            collectStats(stats, entry, result);
        }
        return stats;
    }

    private Path resolveCompleteStaticResourceRoot() {
        String propertyValue = System.getProperty("bhe.staticResourceRoot");
        if (!isBlank(propertyValue)) {
            return Paths.get(propertyValue).toAbsolutePath().normalize();
        }
        String environmentValue = System.getenv("BHE_STATIC_RESOURCE_ROOT");
        if (!isBlank(environmentValue)) {
            return Paths.get(environmentValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private void collectStats(
            RealCandidateStats stats,
            BheMapEntryPlan entry,
            BheMapDataConversionResult result
    ) {
        boolean groupIndex9 = false;
        boolean multipleSpriteMaps = false;
        boolean missingFinalReference = false;
        for (BheMapDataConversionIssue issue : result.getBlockingIssues()) {
            stats.issueTypeCounts.merge(issue.getType(), 1, Integer::sum);
            if (issue.getType() == BheMapDataConversionIssueType.UNSUPPORTED_SCRIPT_GROUP_INDEX) {
                groupIndex9 = true;
            }
            if (issue.getType() == BheMapDataConversionIssueType.UNSUPPORTED_MULTIPLE_SPRITE_MAPS) {
                multipleSpriteMaps = true;
            }
            if (issue.getType() == BheMapDataConversionIssueType.MISSING_FINAL_REFERENCE) {
                missingFinalReference = true;
                stats.missingFinalReferenceTypeCounts.merge(issue.getReferenceType(), 1, Integer::sum);
                stats.missingFinalReferenceExamples.add(MissingReferenceExample.from(entry, issue));
            }
        }
        if (groupIndex9) {
            stats.blockedByGroupIndex9MapCount++;
        }
        if (multipleSpriteMaps) {
            stats.blockedByMultipleSpriteMapListCount++;
        }
        if (missingFinalReference) {
            stats.blockedByMissingFinalReferenceCount++;
        }
    }

    private LocalResourceRootAudit auditLocalResourceRoot(Path root) throws Exception {
        LocalResourceRootAudit audit = new LocalResourceRootAudit(root);
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String extension = extension(path.getFileName().toString());
                audit.totalFileCount++;
                audit.extensionCounts.merge(extension, 1, Integer::sum);
                if (".spm".equals(extension)) {
                    audit.spmFileCount++;
                }
                if (".map".equals(extension)) {
                    audit.mapFileCount++;
                }
                if (isImageExtension(extension)) {
                    audit.imageFileCount++;
                }
            });
        }
        return audit;
    }

    private PlanReferenceAudit auditPlanResourceReferences(BheMapAppendPlan plan) {
        PlanReferenceAudit audit = new PlanReferenceAudit();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
                audit.totalResourceReferences++;
                if (isBlank(reference.getSourceText())) {
                    audit.blankSourceTextCount++;
                }
                if (isBlank(reference.getTargetText())) {
                    audit.blankTargetTextCount++;
                }
                audit.statusCounts.merge(reference.getStatus(), 1, Integer::sum);
                audit.typeStatusCounts
                        .computeIfAbsent(reference.getType(), ignored -> new EnumMap<>(BheMapReferenceStatus.class))
                        .merge(reference.getStatus(), 1, Integer::sum);
            }
        }
        return audit;
    }

    private void auditRewriteLookups(
            RewriteLookupAudit audit,
            BheMapEntryPlan entry,
            com.giga.nexas.dto.bhe.map.MapData source,
            BheMapResourceRewriteTable table
    ) {
        auditRewriteLookup(audit, entry, table, BheMapReferenceType.FOREGROUND, source.getForegroundImage());
        if (source.getNamedResourceSlotBlocks() != null) {
            for (com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock slot : source.getNamedResourceSlotBlocks()) {
                auditRewriteLookup(audit, entry, table, BheMapReferenceType.RESOURCE_SLOT, slot == null ? null : slot.getSlotText());
            }
        }
        if (source.getSpriteMapList() != null && !source.getSpriteMapList().isEmpty()) {
            for (String spriteMap : source.getSpriteMapList()) {
                auditRewriteLookup(audit, entry, table, BheMapReferenceType.SPRITE_MAP_LIST, spriteMap);
            }
            return;
        }
        auditRewriteLookup(audit, entry, table, BheMapReferenceType.SPRITE_MAP, source.getSpriteMap());
    }

    private void auditRewriteLookup(
            RewriteLookupAudit audit,
            BheMapEntryPlan entry,
            BheMapResourceRewriteTable table,
            BheMapReferenceType type,
            String sourceText
    ) {
        if (isBlank(sourceText)) {
            audit.blankLookupSourceTextCount++;
            return;
        }
        audit.totalLookups++;
        audit.lookupCountsByType.merge(type, 1, Integer::sum);
        BheMapResourceReference reference = table.find(type, sourceText);
        if (reference == null) {
            classifyMissingRewriteLookup(audit, entry, type, sourceText);
            return;
        }

        audit.matchedLookups++;
        if (reference.isMissing()) {
            audit.statusMissingCount++;
            audit.lookupStatusByType
                    .computeIfAbsent(type, ignored -> new LinkedHashMap<>())
                    .merge("STATUS_MISSING", 1, Integer::sum);
            return;
        }
        if (isBlank(reference.getTargetText())) {
            audit.blankTargetTextCount++;
            audit.lookupStatusByType
                    .computeIfAbsent(type, ignored -> new LinkedHashMap<>())
                    .merge("BLANK_TARGET_TEXT", 1, Integer::sum);
            return;
        }
        audit.foundReadyCount++;
        audit.lookupStatusByType
                .computeIfAbsent(type, ignored -> new LinkedHashMap<>())
                .merge("FOUND_READY", 1, Integer::sum);
    }

    private void classifyMissingRewriteLookup(
            RewriteLookupAudit audit,
            BheMapEntryPlan entry,
            BheMapReferenceType type,
            String sourceText
    ) {
        List<BheMapResourceReference> references = entry.getResourceReferences().getReferences();
        boolean sameSourceDifferentType = references.stream()
                .anyMatch(reference -> sourceText.equals(reference.getSourceText()) && reference.getType() != type);
        boolean sameTypeDifferentTextSameFile = references.stream()
                .anyMatch(reference -> reference.getType() == type
                        && sourceFileName(sourceText).equals(reference.getSourceFileName())
                        && !sourceText.equals(reference.getSourceText()));

        if (sameSourceDifferentType) {
            audit.referenceTypeMismatchCount++;
            audit.lookupStatusByType
                    .computeIfAbsent(type, ignored -> new LinkedHashMap<>())
                    .merge("REFERENCE_TYPE_MISMATCH", 1, Integer::sum);
            return;
        }
        if (sameTypeDifferentTextSameFile) {
            audit.sourceTextMismatchCount++;
            audit.lookupStatusByType
                    .computeIfAbsent(type, ignored -> new LinkedHashMap<>())
                    .merge("SOURCE_TEXT_MISMATCH", 1, Integer::sum);
            return;
        }
        audit.planNotCollectedCount++;
        audit.lookupStatusByType
                .computeIfAbsent(type, ignored -> new LinkedHashMap<>())
                .merge("PLAN_NOT_COLLECTED", 1, Integer::sum);
    }

    private void printLocalResourceRootCharacterization(
            LocalResourceRootAudit rootAudit,
            PlanReferenceAudit planAudit,
            RealCandidateStats stats
    ) {
        System.out.println("local resource root path: " + rootAudit.root);
        System.out.println("local resource root total files: " + rootAudit.totalFileCount);
        System.out.println("local resource root extension counts: " + rootAudit.extensionCounts);
        System.out.println("local resource root spm files: " + rootAudit.spmFileCount);
        System.out.println("local resource root map files: " + rootAudit.mapFileCount);
        System.out.println("local resource root image files: " + rootAudit.imageFileCount);
        System.out.println("plan total resource references: " + planAudit.totalResourceReferences);
        System.out.println("plan reference status counts: " + planAudit.statusCounts);
        System.out.println("plan reference type/status counts: " + planAudit.typeStatusCounts);
        System.out.println("plan blank sourceText count: " + planAudit.blankSourceTextCount);
        System.out.println("plan blank targetText count: " + planAudit.blankTargetTextCount);
        System.out.println("local resource root candidate total plan entries: " + stats.totalPlanEntries);
        System.out.println("local resource root candidate conversion success count: " + stats.conversionSuccessCount);
        System.out.println("local resource root candidate blocked by groupIndex=9 map count: " + stats.blockedByGroupIndex9MapCount);
        System.out.println("local resource root candidate blocked by multiple spriteMapList count: " + stats.blockedByMultipleSpriteMapListCount);
        System.out.println("local resource root candidate blocked by missing final reference count: " + stats.blockedByMissingFinalReferenceCount);
        System.out.println("local resource root candidate blocking issue type counts: " + stats.issueTypeCounts);
        System.out.println("missing final reference type counts: " + stats.missingFinalReferenceTypeCounts);
        System.out.println("rewrite lookup total count: " + stats.lookupAudit.totalLookups);
        System.out.println("rewrite lookup matched count: " + stats.lookupAudit.matchedLookups);
        System.out.println("rewrite lookup type counts: " + stats.lookupAudit.lookupCountsByType);
        System.out.println("rewrite lookup status/type counts: " + stats.lookupAudit.lookupStatusByType);
        System.out.println("rewrite lookup plan-not-collected count: " + stats.lookupAudit.planNotCollectedCount);
        System.out.println("rewrite lookup reference-type-mismatch count: " + stats.lookupAudit.referenceTypeMismatchCount);
        System.out.println("rewrite lookup sourceText-mismatch count: " + stats.lookupAudit.sourceTextMismatchCount);
        printMissingReferenceExamples(stats);
    }

    private void printCompleteResourceRootCharacterization(
            LocalResourceRootAudit rootAudit,
            PlanReferenceAudit planAudit,
            RealCandidateStats stats
    ) {
        System.out.println("complete resource root path: " + rootAudit.root);
        System.out.println("complete resource root total files: " + rootAudit.totalFileCount);
        System.out.println("complete resource root extension counts: " + rootAudit.extensionCounts);
        System.out.println("complete resource root spm files: " + rootAudit.spmFileCount);
        System.out.println("complete resource root map files: " + rootAudit.mapFileCount);
        System.out.println("complete resource root image files: " + rootAudit.imageFileCount);
        System.out.println("complete root plan total resource references: " + planAudit.totalResourceReferences);
        System.out.println("complete root plan reference status counts: " + planAudit.statusCounts);
        System.out.println("complete root plan reference type/status counts: " + planAudit.typeStatusCounts);
        System.out.println("complete root plan blank sourceText count: " + planAudit.blankSourceTextCount);
        System.out.println("complete root plan blank targetText count: " + planAudit.blankTargetTextCount);
        System.out.println("complete root candidate total plan entries: " + stats.totalPlanEntries);
        System.out.println("complete root candidate conversion success count: " + stats.conversionSuccessCount);
        System.out.println("complete root candidate blocked by groupIndex=9 map count: " + stats.blockedByGroupIndex9MapCount);
        System.out.println("complete root candidate blocked by multiple spriteMapList count: " + stats.blockedByMultipleSpriteMapListCount);
        System.out.println("complete root candidate blocked by missing final reference count: " + stats.blockedByMissingFinalReferenceCount);
        System.out.println("complete root candidate blocking issue type counts: " + stats.issueTypeCounts);
        System.out.println("complete root missing final reference type counts: " + stats.missingFinalReferenceTypeCounts);
        System.out.println("complete root rewrite lookup total count: " + stats.lookupAudit.totalLookups);
        System.out.println("complete root rewrite lookup matched count: " + stats.lookupAudit.matchedLookups);
        System.out.println("complete root rewrite lookup type counts: " + stats.lookupAudit.lookupCountsByType);
        System.out.println("complete root rewrite lookup status/type counts: " + stats.lookupAudit.lookupStatusByType);
        System.out.println("complete root rewrite lookup plan-not-collected count: " + stats.lookupAudit.planNotCollectedCount);
        System.out.println("complete root rewrite lookup reference-type-mismatch count: " + stats.lookupAudit.referenceTypeMismatchCount);
        System.out.println("complete root rewrite lookup sourceText-mismatch count: " + stats.lookupAudit.sourceTextMismatchCount);
        printMissingReferenceExamples(stats);
    }

    private void printMissingReferenceExamples(RealCandidateStats stats) {
        for (BheMapReferenceType type : BheMapReferenceType.values()) {
            List<MissingReferenceExample> examples = stats.missingFinalReferenceExamples.stream()
                    .filter(example -> example.referenceType == type)
                    .limit(20)
                    .toList();
            System.out.println("missing final reference first examples for " + type + ":");
            for (MissingReferenceExample example : examples) {
                System.out.println("  " + example);
            }
        }
    }

    private BheMapDataConversionIssue onlyIssue(
            BheMapDataConversionResult result,
            BheMapDataConversionIssueType type
    ) {
        List<BheMapDataConversionIssue> matches = result.getBlockingIssues().stream()
                .filter(issue -> issue.getType() == type)
                .toList();
        assertEquals(1, matches.size());
        return matches.get(0);
    }

    private BheMapEntryPlan entryPlan(String sourceMapFileName) {
        BheMapEntryPlan entryPlan = new BheMapEntryPlan();
        entryPlan.setSourceMapFileName(sourceMapFileName);
        entryPlan.setTargetMapFileName("bhe_" + sourceMapFileName);
        return entryPlan;
    }

    private com.giga.nexas.dto.bhe.map.MapData syntheticMapData() {
        com.giga.nexas.dto.bhe.map.MapData mapData = new com.giga.nexas.dto.bhe.map.MapData("synthetic.map");
        mapData.setMagic("MAPDATA VER-1.00");
        mapData.setWidth(1);
        mapData.setHeight(1);
        mapData.getTileRecords().add(tileRecord());
        mapData.setCameraRectBlock(cameraRectBlock());
        mapData.setRawPointGroup0(rawPointGroupBlock(1, 2));
        mapData.setRawPointGroup1(rawPointGroupBlock(3, 4));
        mapData.setRawRectGroup0(rawRectGroupBlock(1, 2, 3, 4));
        mapData.setRawRectGroup1(rawRectGroupBlock(5, 6, 7, 8));
        mapData.setRawRectGroup2(rawRectGroupBlock(9, 10, 11, 12));
        mapData.setNamedResourceSlotBlocks(resourceSlots());
        mapData.setScriptEntryGroupBlocks(emptyBheScriptGroups());
        mapData.getScriptEntryGroupBlocks().get(0).getEntries().add(scriptEntry(0, 11, 111, 222));
        mapData.getScriptEntryGroupBlocks().get(0).getEntries().add(scriptEntry(3, 33, 333, 444));
        mapData.getScriptEntryGroupBlocks().get(0).setCount(2);
        mapData.setForegroundImage("fg/source.png");
        mapData.setSpriteMap("spm/source.spm");
        return mapData;
    }

    private com.giga.nexas.dto.bhe.map.MapData.TileRecord tileRecord() {
        com.giga.nexas.dto.bhe.map.MapData.TileRecord record = new com.giga.nexas.dto.bhe.map.MapData.TileRecord();
        record.setPackedValue0(10);
        record.setPackedValue1(20);
        record.setByte0((byte) 1);
        record.setByte1((byte) 2);
        return record;
    }

    private com.giga.nexas.dto.bhe.map.MapData.CameraRectBlock cameraRectBlock() {
        com.giga.nexas.dto.bhe.map.MapData.CameraRectBlock block = new com.giga.nexas.dto.bhe.map.MapData.CameraRectBlock();
        block.setCount(1);
        com.giga.nexas.dto.bhe.map.MapData.CameraRectEntry entry = new com.giga.nexas.dto.bhe.map.MapData.CameraRectEntry();
        entry.setLeft(1);
        entry.setTop(2);
        entry.setRight(3);
        entry.setBottom(4);
        block.getEntries().add(entry);
        return block;
    }

    private com.giga.nexas.dto.bhe.map.MapData.RawPointGroupBlock rawPointGroupBlock(int value0, int value1) {
        com.giga.nexas.dto.bhe.map.MapData.RawPointGroupBlock block = new com.giga.nexas.dto.bhe.map.MapData.RawPointGroupBlock();
        block.setCount(1);
        com.giga.nexas.dto.bhe.map.MapData.RawPointGroupEntry entry = new com.giga.nexas.dto.bhe.map.MapData.RawPointGroupEntry();
        entry.setRawValue0(value0);
        entry.setRawValue1(value1);
        block.getEntries().add(entry);
        return block;
    }

    private com.giga.nexas.dto.bhe.map.MapData.RawRectGroupBlock rawRectGroupBlock(
            int value0,
            int value1,
            int value2,
            int value3
    ) {
        com.giga.nexas.dto.bhe.map.MapData.RawRectGroupBlock block = new com.giga.nexas.dto.bhe.map.MapData.RawRectGroupBlock();
        block.setCount(1);
        com.giga.nexas.dto.bhe.map.MapData.RawRectGroupEntry entry = new com.giga.nexas.dto.bhe.map.MapData.RawRectGroupEntry();
        entry.setRawValue0(value0);
        entry.setRawValue1(value1);
        entry.setRawValue2(value2);
        entry.setRawValue3(value3);
        block.getEntries().add(entry);
        return block;
    }

    private List<com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock> resourceSlots() {
        java.util.ArrayList<com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock> slots = new java.util.ArrayList<>();
        for (int index = 0; index < com.giga.nexas.dto.bhe.map.MapData.RESOURCE_SLOT_COUNT; index++) {
            com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock slot =
                    new com.giga.nexas.dto.bhe.map.MapData.NamedResourceSlotBlock();
            slot.setSlotNum(index);
            slot.setSlotText("slot/slot" + index + ".png");
            slot.setParam0(100 + index);
            slot.setParam1(101 + index);
            slot.setParam2(102 + index);
            slot.setParam3(103 + index);
            slot.setParam4(104 + index);
            slots.add(slot);
        }
        return slots;
    }

    private List<com.giga.nexas.dto.bhe.map.MapData.ScriptEntryGroupBlock> emptyBheScriptGroups() {
        java.util.ArrayList<com.giga.nexas.dto.bhe.map.MapData.ScriptEntryGroupBlock> groups = new java.util.ArrayList<>();
        for (int groupNum = 0; groupNum < com.giga.nexas.dto.bhe.map.MapData.SCRIPT_GROUP_COUNT; groupNum++) {
            com.giga.nexas.dto.bhe.map.MapData.ScriptEntryGroupBlock group =
                    new com.giga.nexas.dto.bhe.map.MapData.ScriptEntryGroupBlock();
            group.setGroupNum(groupNum);
            group.setCount(0);
            groups.add(group);
        }
        return groups;
    }

    private com.giga.nexas.dto.bhe.map.MapData.ScriptEntry scriptEntry(int groupIndex, int typeId, int x, int y) {
        com.giga.nexas.dto.bhe.map.MapData.ScriptEntry entry = new com.giga.nexas.dto.bhe.map.MapData.ScriptEntry();
        entry.setGroupIndex(groupIndex);
        entry.setTypeId(typeId);
        entry.setX(x);
        entry.setY(y);
        return entry;
    }

    private BheMapResourceRewriteTable completeRewriteTable() {
        return rewriteTable(
                found(BheMapReferenceType.FOREGROUND, "fg/source.png", "fg/bhe_source.png"),
                found(BheMapReferenceType.SPRITE_MAP, "spm/source.spm", "spm/bhe_source.spm"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot0.png", "slot/bhe_slot0.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot1.png", "slot/bhe_slot1.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot2.png", "slot/bhe_slot2.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot3.png", "slot/bhe_slot3.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot4.png", "slot/bhe_slot4.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot5.png", "slot/bhe_slot5.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot6.png", "slot/bhe_slot6.png"),
                found(BheMapReferenceType.RESOURCE_SLOT, "slot/slot7.png", "slot/bhe_slot7.png")
        );
    }

    private BheMapResourceRewriteTable rewriteTable(BheMapResourceReference... references) {
        BheMapResourceRewriteTable table = new BheMapResourceRewriteTable();
        for (BheMapResourceReference reference : references) {
            table.put(reference);
        }
        return table;
    }

    private BheMapResourceReference found(BheMapReferenceType type, String sourceText, String targetText) {
        BheMapResourceReference reference = reference(type, sourceText, targetText);
        reference.setStatus(BheMapReferenceStatus.FOUND_DEFERRED);
        return reference;
    }

    private BheMapResourceReference missing(BheMapReferenceType type, String sourceText, String targetText) {
        BheMapResourceReference reference = reference(type, sourceText, targetText);
        reference.setStatus(BheMapReferenceStatus.MISSING_NON_BLOCKING);
        return reference;
    }

    private BheMapResourceReference reference(BheMapReferenceType type, String sourceText, String targetText) {
        BheMapResourceReference reference = new BheMapResourceReference();
        reference.setType(type);
        reference.setSourceText(sourceText);
        reference.setTargetText(targetText);
        return reference;
    }

    private String sourceFileName(String reference) {
        return Path.of(reference.trim().replace('\\', '/')).getFileName().toString();
    }

    private String extension(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) {
            return "";
        }
        return fileName.substring(dot).toLowerCase(java.util.Locale.ROOT);
    }

    private boolean isImageExtension(String extension) {
        return ".png".equals(extension)
                || ".bmp".equals(extension)
                || ".jpg".equals(extension)
                || ".jpeg".equals(extension)
                || ".tga".equals(extension)
                || ".dds".equals(extension);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static class RealCandidateStats {

        private int totalPlanEntries;
        private int conversionSuccessCount;
        private int blockedByGroupIndex9MapCount;
        private int blockedByMultipleSpriteMapListCount;
        private int blockedByMissingFinalReferenceCount;
        private Map<BheMapDataConversionIssueType, Integer> issueTypeCounts =
                new EnumMap<>(BheMapDataConversionIssueType.class);
        private Map<BheMapReferenceType, Integer> missingFinalReferenceTypeCounts =
                new EnumMap<>(BheMapReferenceType.class);
        private List<MissingReferenceExample> missingFinalReferenceExamples = new ArrayList<>();
        private RewriteLookupAudit lookupAudit = new RewriteLookupAudit();

        private int blockedMapCount() {
            return totalPlanEntries - conversionSuccessCount;
        }

        private int issueTypeCount(BheMapDataConversionIssueType type) {
            return issueTypeCounts.getOrDefault(type, 0);
        }

        private int missingFinalReferenceTypeCount(BheMapReferenceType type) {
            return missingFinalReferenceTypeCounts.getOrDefault(type, 0);
        }
    }

    private static class LocalResourceRootAudit {

        private final Path root;
        private int totalFileCount;
        private int spmFileCount;
        private int mapFileCount;
        private int imageFileCount;
        private Map<String, Integer> extensionCounts = new LinkedHashMap<>();

        private LocalResourceRootAudit(Path root) {
            this.root = root;
        }
    }

    private static class PlanReferenceAudit {

        private int totalResourceReferences;
        private int blankSourceTextCount;
        private int blankTargetTextCount;
        private Map<BheMapReferenceStatus, Integer> statusCounts =
                new EnumMap<>(BheMapReferenceStatus.class);
        private Map<BheMapReferenceType, Map<BheMapReferenceStatus, Integer>> typeStatusCounts =
                new EnumMap<>(BheMapReferenceType.class);

        private int foundCount(BheMapReferenceType type) {
            return statusCount(type, BheMapReferenceStatus.FOUND_DEFERRED);
        }

        private int missingCount(BheMapReferenceType type) {
            return statusCount(type, BheMapReferenceStatus.MISSING_NON_BLOCKING);
        }

        private int statusCount(BheMapReferenceType type, BheMapReferenceStatus status) {
            return typeStatusCounts.getOrDefault(type, Map.of()).getOrDefault(status, 0);
        }

        private int statusCount(BheMapReferenceStatus status) {
            return statusCounts.getOrDefault(status, 0);
        }
    }

    private static class RewriteLookupAudit {

        private int totalLookups;
        private int matchedLookups;
        private int blankLookupSourceTextCount;
        private int planNotCollectedCount;
        private int referenceTypeMismatchCount;
        private int sourceTextMismatchCount;
        private int statusMissingCount;
        private int blankTargetTextCount;
        private int foundReadyCount;
        private Map<BheMapReferenceType, Integer> lookupCountsByType =
                new EnumMap<>(BheMapReferenceType.class);
        private Map<BheMapReferenceType, Map<String, Integer>> lookupStatusByType =
                new EnumMap<>(BheMapReferenceType.class);
    }

    private static class MissingReferenceExample {

        private final String sourceMapFileName;
        private final BheMapReferenceType referenceType;
        private final String sourceText;
        private final String sourceFileName;
        private final String targetText;
        private final String sourcePath;
        private final String status;

        private MissingReferenceExample(
                String sourceMapFileName,
                BheMapReferenceType referenceType,
                String sourceText,
                String sourceFileName,
                String targetText,
                String sourcePath,
                String status
        ) {
            this.sourceMapFileName = sourceMapFileName;
            this.referenceType = referenceType;
            this.sourceText = sourceText;
            this.sourceFileName = sourceFileName;
            this.targetText = targetText;
            this.sourcePath = sourcePath;
            this.status = status;
        }

        private static MissingReferenceExample from(BheMapEntryPlan entry, BheMapDataConversionIssue issue) {
            BheMapResourceReference reference = findReference(entry, issue.getReferenceType(), issue.getReferenceText());
            return new MissingReferenceExample(
                    issue.getSourceMapFileName(),
                    issue.getReferenceType(),
                    issue.getReferenceText(),
                    reference == null ? null : reference.getSourceFileName(),
                    reference == null ? null : reference.getTargetText(),
                    reference == null || reference.getSourcePath() == null ? null : reference.getSourcePath().toString(),
                    reference == null || reference.getStatus() == null ? "PLAN_REFERENCE_NOT_FOUND" : reference.getStatus().name()
            );
        }

        private static BheMapResourceReference findReference(
                BheMapEntryPlan entry,
                BheMapReferenceType referenceType,
                String sourceText
        ) {
            return entry.getResourceReferences().getReferences().stream()
                    .filter(reference -> reference.getType() == referenceType)
                    .filter(reference -> sourceText.equals(reference.getSourceText()))
                    .findFirst()
                    .orElse(null);
        }

        @Override
        public String toString() {
            return "sourceMapFileName=" + sourceMapFileName
                    + ", referenceType=" + referenceType
                    + ", sourceText=" + sourceText
                    + ", sourceFileName=" + sourceFileName
                    + ", targetText=" + targetText
                    + ", sourcePath=" + sourcePath
                    + ", status=" + status;
        }
    }
}
