package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.dto.bhe.spm.Spm;
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
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapSpmCompositionCompatibilityAuditTest {

    @Test
    void completeStaticResourceRootAuditsSpriteMapListCompositionCompatibility() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "Provide the complete BHE static resource root with -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        BheMapAppendPlan plan = new BheMapAppendPlanBuilder().build(
                sourceCatalog,
                request.resolveBheMapDir(),
                request.resolveBheStaticResourceRoot(),
                new BsdxMapBaseline(),
                request.getCharset(),
                new BheMapAppendAudit()
        );

        CompositionCompatibilityProfile profile = auditCompositionCompatibility(
                plan,
                request.getCharset(),
                CaseInsensitiveFileIndex.fromTree(completeResourceRoot)
        );
        printProfile(profile);

        assertEquals(142, profile.totalMapCount);
        assertEquals(109, profile.multipleMapCount);
        assertEquals(104, profile.completeSourceMapCount);
        assertEquals(5, profile.missingSourceMapCount);
        assertEquals(109, profile.typeIdCollisionMapCount);
        assertEquals(0, profile.plainAppendSafeMapCount);
        assertEquals(109, profile.mapsNeedingTypeIdRewriteCount);
        assertEquals(0, profile.completeMapsWithComputableUniqueRewriteCount);
        assertEquals(104, profile.completeMapsWithoutComputableUniqueRewriteCount);
        assertFalse(profile.records.isEmpty());
    }

    private CompositionCompatibilityProfile auditCompositionCompatibility(
            BheMapAppendPlan plan,
            String charset,
            CaseInsensitiveFileIndex completeResourceIndex
    ) throws Exception {
        CompositionCompatibilityProfile profile = new CompositionCompatibilityProfile();
        BheBinService bheBinService = new BheBinService();
        profile.totalMapCount = plan.getEntries().size();

        for (BheMapEntryPlan entry : plan.getEntries()) {
            MapData mapData = (MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            List<String> spriteMapList = normalizedSpriteMapList(mapData);
            if (spriteMapList.size() <= 1) {
                continue;
            }

            CompositionMapRecord record = buildRecord(
                    entry,
                    mapData,
                    spriteMapList,
                    bheBinService,
                    charset,
                    completeResourceIndex
            );
            profile.records.add(record);
            profile.multipleMapCount++;
            if (record.allSourceSpmFound) {
                profile.completeSourceMapCount++;
            } else {
                profile.missingSourceMapCount++;
            }
            if (record.hasAnyTypeIdCollision()) {
                profile.typeIdCollisionMapCount++;
            }
            if (record.isPlainAppendSafe()) {
                profile.plainAppendSafeMapCount++;
            }
            if (record.needsTypeIdRewrite()) {
                profile.mapsNeedingTypeIdRewriteCount++;
            }
            if (record.allSourceSpmFound && record.hasComputableUniqueRewrite()) {
                profile.completeMapsWithComputableUniqueRewriteCount++;
            }
            if (record.allSourceSpmFound && !record.hasComputableUniqueRewrite()) {
                profile.completeMapsWithoutComputableUniqueRewriteCount++;
            }
            profile.sizeDistribution.merge(record.spriteMapListSize, 1, Integer::sum);
        }
        return profile;
    }

    private CompositionMapRecord buildRecord(
            BheMapEntryPlan entry,
            MapData mapData,
            List<String> spriteMapList,
            BheBinService bheBinService,
            String charset,
            CaseInsensitiveFileIndex completeResourceIndex
    ) throws Exception {
        CompositionMapRecord record = new CompositionMapRecord();
        record.sourceMapFileName = entry.getSourceMapFileName();
        record.spriteMapListSize = spriteMapList.size();
        record.scriptTypeIds = collectScriptTypeIds(mapData);
        record.representativeSample = representativeSample(record.sourceMapFileName, record.spriteMapListSize);
        Map<String, BheMapResourceReference> referencesByText = spriteMapListReferences(entry);
        int appendOffset = 0;
        for (String sourceText : spriteMapList) {
            BheMapResourceReference reference = referencesByText.get(sourceText);
            SourceSpmRecord sourceRecord = new SourceSpmRecord();
            sourceRecord.sourceText = sourceText;
            sourceRecord.sourceFileName = reference == null ? sourceFileName(sourceText) : reference.getSourceFileName();
            sourceRecord.sourcePath = reference == null || reference.getSourcePath() == null
                    ? null
                    : reference.getSourcePath();
            Path completeRootPath = completeResourceIndex.resolveByName(sourceRecord.sourceFileName);
            sourceRecord.status = reference == null ? null : reference.getStatus();
            sourceRecord.appendAnimOffset = appendOffset;
            if (completeRootPath != null) {
                sourceRecord.sourcePath = completeRootPath;
                sourceRecord.status = BheMapReferenceStatus.FOUND_DEFERRED;
                Spm spm = (Spm) bheBinService.parse(completeRootPath.toString(), charset).getData();
                sourceRecord.animCount = safeSize(spm.getAnimData());
                sourceRecord.chipCount = countChips(spm);
                sourceRecord.hitCount = countHits(spm);
                sourceRecord.animIndexStart = 0;
                sourceRecord.animIndexEnd = Math.max(0, sourceRecord.animCount - 1);
                sourceRecord.matchedTypeIds.addAll(record.scriptTypeIds.stream()
                        .filter(typeId -> typeId >= 0 && typeId < sourceRecord.animCount)
                        .toList());
                appendOffset += sourceRecord.animCount;
            } else {
                record.allSourceSpmFound = false;
            }
            record.sourceSpmRecords.add(sourceRecord);
        }
        record.totalComposedAnimCount = appendOffset;
        record.computeTypeIdBindings();
        return record;
    }

    private List<String> normalizedSpriteMapList(MapData mapData) {
        if (mapData.getSpriteMapList() == null) {
            return List.of();
        }
        return mapData.getSpriteMapList().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private Set<Integer> collectScriptTypeIds(MapData mapData) {
        Set<Integer> typeIds = new LinkedHashSet<>();
        if (mapData.getScriptEntryGroupBlocks() == null) {
            return typeIds;
        }
        for (MapData.ScriptEntryGroupBlock groupBlock : mapData.getScriptEntryGroupBlocks()) {
            if (groupBlock == null || groupBlock.getEntries() == null) {
                continue;
            }
            for (MapData.ScriptEntry entry : groupBlock.getEntries()) {
                if (entry != null && entry.getTypeId() != null) {
                    typeIds.add(entry.getTypeId());
                }
            }
        }
        return typeIds;
    }

    private Map<String, BheMapResourceReference> spriteMapListReferences(BheMapEntryPlan entry) {
        Map<String, BheMapResourceReference> references = new LinkedHashMap<>();
        for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
            if (reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST) {
                references.put(reference.getSourceText(), reference);
            }
        }
        return references;
    }

    private int safeSize(List<?> values) {
        return values == null ? 0 : values.size();
    }

    private int countChips(Spm spm) {
        if (spm.getPageData() == null) {
            return 0;
        }
        int count = 0;
        for (Spm.SPMPageData page : spm.getPageData()) {
            count += page == null || page.getChipData() == null ? 0 : page.getChipData().size();
        }
        return count;
    }

    private int countHits(Spm spm) {
        if (spm.getPageData() == null) {
            return 0;
        }
        int count = 0;
        for (Spm.SPMPageData page : spm.getPageData()) {
            count += page == null || page.getHitRects() == null ? 0 : page.getHitRects().size();
        }
        return count;
    }

    private String sourceFileName(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String normalized = reference.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash < 0 ? normalized : normalized.substring(slash + 1);
    }

    private boolean representativeSample(String fileName, int spriteMapListSize) {
        Set<String> names = Set.of(
                "mapB06_Battleship09.map",
                "mapB06_Battleship01.map",
                "mapB06_Harbor03.map",
                "mapB06_Outsider07.map",
                "mapB06_OutsiderE01.map"
        );
        return names.contains(fileName) || spriteMapListSize == 2 || spriteMapListSize == 5
                || spriteMapListSize == 8 || spriteMapListSize == 10;
    }

    private Path resolveCompleteStaticResourceRoot() {
        String propertyValue = System.getProperty("bhe.staticResourceRoot");
        if (propertyValue != null && !propertyValue.isBlank()) {
            return Paths.get(propertyValue).toAbsolutePath().normalize();
        }
        String environmentValue = System.getenv("BHE_STATIC_RESOURCE_ROOT");
        if (environmentValue != null && !environmentValue.isBlank()) {
            return Paths.get(environmentValue).toAbsolutePath().normalize();
        }
        return null;
    }

    private void printProfile(CompositionCompatibilityProfile profile) {
        System.out.println("SPM composition compatibility audit");
        System.out.println("total map count: " + profile.totalMapCount);
        System.out.println("multiple spriteMapList map count: " + profile.multipleMapCount);
        System.out.println("spriteMapList size distribution: " + profile.sizeDistribution);
        System.out.println("complete source SPM map count: " + profile.completeSourceMapCount);
        System.out.println("missing source SPM map count: " + profile.missingSourceMapCount);
        System.out.println("typeId collision map count: " + profile.typeIdCollisionMapCount);
        System.out.println("plain append safe map count: " + profile.plainAppendSafeMapCount);
        System.out.println("maps needing typeId rewrite count: " + profile.mapsNeedingTypeIdRewriteCount);
        System.out.println("complete maps with computable unique rewrite count: " + profile.completeMapsWithComputableUniqueRewriteCount);
        System.out.println("complete maps without computable unique rewrite count: " + profile.completeMapsWithoutComputableUniqueRewriteCount);
        System.out.println("all composition records:");
        profile.records.stream()
                .sorted(Comparator.comparing(record -> record.sourceMapFileName))
                .forEach(record -> System.out.println(record.compactLine()));
        System.out.println("representative records:");
        profile.records.stream()
                .filter(record -> record.representativeSample)
                .sorted(Comparator.comparing(record -> record.sourceMapFileName))
                .forEach(System.out::println);
        System.out.println("first collision records:");
        profile.records.stream()
                .filter(CompositionMapRecord::hasAnyTypeIdCollision)
                .limit(20)
                .forEach(record -> System.out.println(record.collisionLine()));
        System.out.println("missing source records:");
        profile.records.stream()
                .filter(record -> !record.allSourceSpmFound)
                .forEach(record -> System.out.println(record.missingLine()));
    }

    private static class CompositionCompatibilityProfile {

        private int totalMapCount;
        private int multipleMapCount;
        private int completeSourceMapCount;
        private int missingSourceMapCount;
        private int typeIdCollisionMapCount;
        private int plainAppendSafeMapCount;
        private int mapsNeedingTypeIdRewriteCount;
        private int completeMapsWithComputableUniqueRewriteCount;
        private int completeMapsWithoutComputableUniqueRewriteCount;
        private Map<Integer, Integer> sizeDistribution = new TreeMap<>();
        private List<CompositionMapRecord> records = new ArrayList<>();
    }

    private static class CompositionMapRecord {

        private String sourceMapFileName;
        private int spriteMapListSize;
        private boolean allSourceSpmFound = true;
        private boolean representativeSample;
        private int totalComposedAnimCount;
        private Set<Integer> scriptTypeIds = new LinkedHashSet<>();
        private Map<Integer, List<SourceSpmRecord>> localTypeIdHits = new LinkedHashMap<>();
        private Map<Integer, Integer> uniqueRewriteTargets = new LinkedHashMap<>();
        private List<Integer> ambiguousTypeIds = new ArrayList<>();
        private List<SourceSpmRecord> sourceSpmRecords = new ArrayList<>();

        private void computeTypeIdBindings() {
            for (Integer typeId : scriptTypeIds) {
                List<SourceSpmRecord> hits = sourceSpmRecords.stream()
                        .filter(record -> record.status == BheMapReferenceStatus.FOUND_DEFERRED)
                        .filter(record -> typeId >= 0 && typeId < record.animCount)
                        .toList();
                localTypeIdHits.put(typeId, hits);
                if (hits.size() == 1) {
                    SourceSpmRecord hit = hits.get(0);
                    uniqueRewriteTargets.put(typeId, hit.appendAnimOffset + typeId);
                }
                if (hits.size() > 1) {
                    ambiguousTypeIds.add(typeId);
                }
            }
        }

        private boolean hasAnyTypeIdCollision() {
            return !ambiguousTypeIds.isEmpty();
        }

        private boolean isPlainAppendSafe() {
            if (!allSourceSpmFound) {
                return false;
            }
            for (Integer typeId : scriptTypeIds) {
                List<SourceSpmRecord> hits = localTypeIdHits.getOrDefault(typeId, List.of());
                if (hits.size() != 1) {
                    return false;
                }
                SourceSpmRecord hit = hits.get(0);
                if (hit.appendAnimOffset + typeId != typeId) {
                    return false;
                }
            }
            return true;
        }

        private boolean needsTypeIdRewrite() {
            if (!allSourceSpmFound) {
                return true;
            }
            for (Map.Entry<Integer, Integer> entry : uniqueRewriteTargets.entrySet()) {
                if (!entry.getKey().equals(entry.getValue())) {
                    return true;
                }
            }
            return hasAnyTypeIdCollision();
        }

        private boolean hasComputableUniqueRewrite() {
            if (!allSourceSpmFound) {
                return false;
            }
            for (Integer typeId : scriptTypeIds) {
                if (!uniqueRewriteTargets.containsKey(typeId)) {
                    return false;
                }
            }
            return true;
        }

        private String collisionLine() {
            return "sourceMapFileName=" + sourceMapFileName
                    + ", ambiguousTypeIds=" + ambiguousTypeIds
                    + ", sourceSpmRanges=" + sourceSpmRecords.stream()
                    .map(SourceSpmRecord::rangeLine)
                    .collect(Collectors.joining("; "));
        }

        private String missingLine() {
            return "sourceMapFileName=" + sourceMapFileName
                    + ", missingSourceSpm=" + sourceSpmRecords.stream()
                    .filter(record -> record.status != BheMapReferenceStatus.FOUND_DEFERRED)
                    .map(SourceSpmRecord::missingLine)
                    .collect(Collectors.joining("; "));
        }

        private String compactLine() {
            return "sourceMapFileName=" + sourceMapFileName
                    + ", spriteMapListSize=" + spriteMapListSize
                    + ", scriptTypeIdCount=" + scriptTypeIds.size()
                    + ", scriptTypeIds=" + scriptTypeIds
                    + ", allSourceSpmFound=" + allSourceSpmFound
                    + ", ambiguousTypeIdCount=" + ambiguousTypeIds.size()
                    + ", plainAppendSafe=" + isPlainAppendSafe()
                    + ", needsTypeIdRewrite=" + needsTypeIdRewrite()
                    + ", hasComputableUniqueRewrite=" + hasComputableUniqueRewrite()
                    + ", sourceSpmRanges=" + sourceSpmRecords.stream()
                    .map(SourceSpmRecord::rangeLine)
                    .collect(Collectors.joining("; "));
        }

        @Override
        public String toString() {
            return "sourceMapFileName=" + sourceMapFileName
                    + ", spriteMapListSize=" + spriteMapListSize
                    + ", scriptTypeIds=" + scriptTypeIds
                    + ", plainAppendSafe=" + isPlainAppendSafe()
                    + ", needsTypeIdRewrite=" + needsTypeIdRewrite()
                    + ", hasComputableUniqueRewrite=" + hasComputableUniqueRewrite()
                    + ", ambiguousTypeIds=" + ambiguousTypeIds
                    + ", sourceSpmRecords=" + sourceSpmRecords;
        }
    }

    private static class SourceSpmRecord {

        private String sourceText;
        private String sourceFileName;
        private Path sourcePath;
        private BheMapReferenceStatus status;
        private int animCount;
        private int chipCount;
        private int hitCount;
        private int animIndexStart;
        private int animIndexEnd;
        private int appendAnimOffset;
        private List<Integer> matchedTypeIds = new ArrayList<>();

        private String rangeLine() {
            return sourceFileName + "[anim=" + animIndexStart + ".." + animIndexEnd
                    + ", appendOffset=" + appendAnimOffset
                    + ", matchedTypeIds=" + matchedTypeIds + "]";
        }

        private String missingLine() {
            return sourceFileName + "[status=" + status
                    + ", sourceText=" + sourceText
                    + ", sourcePath=" + sourcePath + "]";
        }

        @Override
        public String toString() {
            return "{sourceFileName=" + sourceFileName
                    + ", status=" + status
                    + ", animCount=" + animCount
                    + ", chipCount=" + chipCount
                    + ", hitCount=" + hitCount
                    + ", localAnimRange=" + animIndexStart + ".." + animIndexEnd
                    + ", appendAnimOffset=" + appendAnimOffset
                    + ", matchedTypeIds=" + matchedTypeIds + "}";
        }
    }
}
