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
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapSpriteMapListCharacterizationTest {

    @Test
    void completeStaticResourceRootCharacterizesMultipleSpriteMapListCandidates() throws Exception {
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

        RootFileIndex rootIndex = RootFileIndex.from(completeResourceRoot);
        SpriteMapListProfile profile = profileSpriteMapLists(plan, request.getCharset(), rootIndex);

        assertEquals(142, profile.totalMapCount);
        assertEquals(Map.of(0, 8, 1, 25, 2, 13, 3, 4, 4, 10, 5, 24, 6, 22, 7, 18, 8, 11, 10, 7), profile.sizeDistribution);
        assertEquals(109, profile.multipleMapCount);
        assertEquals(614, profile.totalMultipleReferences);
        assertEquals(5, profile.missingReferences.size());
        assertEquals(0, profile.nonMissingReferenceAbsentInRootCount);
        assertEquals(109, profile.firstItemFoundMapCount);
        assertEquals(104, profile.allItemsFoundMapCount);
        assertEquals(46, profile.mapNameMatchedMapCount);
        assertEquals(2, profile.foregroundRelatedMapCount);
        assertEquals(0, profile.resourceSlotRelatedMapCount);

        printProfile(profile);
    }

    private SpriteMapListProfile profileSpriteMapLists(
            BheMapAppendPlan plan,
            String charset,
            RootFileIndex rootIndex
    ) throws Exception {
        SpriteMapListProfile profile = new SpriteMapListProfile();
        BheBinService bheBinService = new BheBinService();
        profile.totalMapCount = plan.getEntries().size();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            com.giga.nexas.dto.bhe.map.MapData mapData =
                    (com.giga.nexas.dto.bhe.map.MapData) bheBinService.parse(entry.getSourceMapPath().toString(), charset).getData();
            List<String> spriteMapList = normalizedSpriteMapList(mapData);
            profile.sizeDistribution.merge(spriteMapList.size(), 1, Integer::sum);
            if (spriteMapList.size() <= 1) {
                continue;
            }

            MultipleSpriteMapRecord record = buildMultipleRecord(entry, mapData, spriteMapList, rootIndex);
            profile.multipleRecords.add(record);
            profile.multipleMapCount++;
            profile.totalMultipleReferences += record.details.size();
            profile.nonMissingReferenceAbsentInRootCount += record.nonMissingReferenceAbsentInRootCount();
            profile.mapNameMatchedMapCount += record.hasUniqueMapNameMatch() ? 1 : 0;
            profile.foregroundRelatedMapCount += record.hasUniqueForegroundMatch() ? 1 : 0;
            profile.resourceSlotRelatedMapCount += record.hasUniqueResourceSlotMatch() ? 1 : 0;
            profile.firstItemFoundMapCount += record.firstItemFound() ? 1 : 0;
            profile.allItemsFoundMapCount += record.allItemsFound() ? 1 : 0;
            profile.canonicalFamilyCounts.merge(record.canonicalFamilyName, 1, Integer::sum);
            for (SpriteMapReferenceDetail detail : record.details) {
                profile.sourceFileNameCounts.merge(detail.sourceFileName, 1, Integer::sum);
                if (detail.status == BheMapReferenceStatus.MISSING_NON_BLOCKING) {
                    profile.missingReferences.add(detail);
                }
            }
        }
        profile.strategySummaries.add(strategySummary(
                "KEEP_UNSUPPORTED",
                109,
                "LOW",
                "不产生有损转换，保持当前阻断；需要额外证据后再进入正式策略"
        ));
        profile.strategySummaries.add(strategySummary(
                "USE_FIRST_ITEM",
                profile.firstItemFoundMapCount,
                "HIGH",
                "第一项存在不等于主图；缺少运行时读取顺序和层级语义证据"
        ));
        profile.strategySummaries.add(strategySummary(
                "USE_MAP_NAME_MATCH",
                profile.mapNameMatchedMapCount,
                "HIGH",
                "按 map 名唯一命中的覆盖率不足，不能解释绝大多数多项列表"
        ));
        profile.strategySummaries.add(strategySummary(
                "USE_FOREGROUND_OR_SLOT_RELATION",
                Math.max(profile.foregroundRelatedMapCount, profile.resourceSlotRelatedMapCount),
                "HIGH",
                "spriteMapList 文件名与 foreground/resource slot 没有稳定一对一关系"
        ));
        profile.strategySummaries.add(strategySummary(
                "SPLIT_TO_MULTIPLE_BSDX_MAPS",
                109,
                "VERY_HIGH",
                "会改变 MapGroup 与关卡入口数量，不属于单张 MapData 内存转换规则"
        ));
        profile.strategySummaries.add(strategySummary(
                "RESOURCE_COMPOSITION_REQUIRED",
                109,
                "MEDIUM",
                "多项列表更像多层对象图，若要压成 BSDX 单 spriteMap 需要资源合成证据和工具链"
        ));
        return profile;
    }

    private MultipleSpriteMapRecord buildMultipleRecord(
            BheMapEntryPlan entry,
            com.giga.nexas.dto.bhe.map.MapData mapData,
            List<String> spriteMapList,
            RootFileIndex rootIndex
    ) {
        MultipleSpriteMapRecord record = new MultipleSpriteMapRecord();
        record.sourceMapFileName = entry.getSourceMapFileName();
        record.spriteMapListSize = spriteMapList.size();
        record.foregroundImage = mapData.getForegroundImage();
        record.resourceSlotTexts = resourceSlotTexts(mapData);
        record.canonicalFamilyName = canonicalFamilyName(entry.getSourceMapFileName());
        Map<String, BheMapResourceReference> references = spriteMapListReferences(entry);
        for (String sourceText : spriteMapList) {
            BheMapResourceReference reference = references.get(sourceText);
            SpriteMapReferenceDetail detail = buildDetail(
                    entry.getSourceMapFileName(),
                    sourceText,
                    reference,
                    rootIndex
            );
            record.details.add(detail);
        }
        return record;
    }

    private SpriteMapReferenceDetail buildDetail(
            String sourceMapFileName,
            String sourceText,
            BheMapResourceReference reference,
            RootFileIndex rootIndex
    ) {
        SpriteMapReferenceDetail detail = new SpriteMapReferenceDetail();
        detail.sourceMapFileName = sourceMapFileName;
        detail.sourceText = sourceText;
        detail.sourceFileName = reference == null ? sourceFileName(sourceText) : reference.getSourceFileName();
        detail.targetText = reference == null ? null : reference.getTargetText();
        detail.sourcePath = reference == null || reference.getSourcePath() == null ? null : reference.getSourcePath().toString();
        detail.status = reference == null ? null : reference.getStatus();
        detail.existsInCompleteRoot = rootIndex.find(detail.sourceFileName) != null;
        if (detail.status == BheMapReferenceStatus.MISSING_NON_BLOCKING && !detail.existsInCompleteRoot) {
            detail.missingReason = "完整静态资源根按 sourceFileName 查找仍不存在";
        } else if (reference == null) {
            detail.missingReason = "import plan 未收集该 spriteMapList 引用";
        } else {
            detail.missingReason = "";
        }
        return detail;
    }

    private List<String> normalizedSpriteMapList(com.giga.nexas.dto.bhe.map.MapData mapData) {
        if (mapData.getSpriteMapList() == null) {
            return List.of();
        }
        return mapData.getSpriteMapList().stream()
                .filter(value -> value != null && !value.isBlank())
                .map(String::trim)
                .toList();
    }

    private List<String> resourceSlotTexts(com.giga.nexas.dto.bhe.map.MapData mapData) {
        if (mapData.getNamedResourceSlotBlocks() == null) {
            return List.of();
        }
        return mapData.getNamedResourceSlotBlocks().stream()
                .filter(slot -> slot != null && slot.getSlotText() != null && !slot.getSlotText().isBlank())
                .map(slot -> slot.getSlotText().trim())
                .toList();
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

    private StrategySummary strategySummary(String name, int coveredMapCount, String riskLevel, String evidence) {
        StrategySummary summary = new StrategySummary();
        summary.name = name;
        summary.coveredMapCount = coveredMapCount;
        summary.riskLevel = riskLevel;
        summary.evidence = evidence;
        return summary;
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

    private void printProfile(SpriteMapListProfile profile) {
        System.out.println("spriteMapList size distribution: " + profile.sizeDistribution);
        System.out.println("multiple spriteMapList map count: " + profile.multipleMapCount);
        System.out.println("multiple spriteMapList reference count: " + profile.totalMultipleReferences);
        System.out.println("multiple first item found map count: " + profile.firstItemFoundMapCount);
        System.out.println("multiple all items found map count: " + profile.allItemsFoundMapCount);
        System.out.println("multiple map-name unique match count: " + profile.mapNameMatchedMapCount);
        System.out.println("multiple foreground unique relation count: " + profile.foregroundRelatedMapCount);
        System.out.println("multiple resource-slot unique relation count: " + profile.resourceSlotRelatedMapCount);
        System.out.println("non-missing spriteMapList reference absent in complete root count: " + profile.nonMissingReferenceAbsentInRootCount);
        System.out.println("canonical family counts: " + profile.canonicalFamilyCounts);
        System.out.println("top spriteMapList sourceFileName counts: " + topCounts(profile.sourceFileNameCounts, 30));
        System.out.println("multiple spriteMapList map details:");
        for (MultipleSpriteMapRecord record : profile.multipleRecords) {
            System.out.println(record);
        }
        System.out.println("missing spriteMapList references:");
        for (SpriteMapReferenceDetail detail : profile.missingReferences) {
            System.out.println(detail.missingLine());
        }
        System.out.println("candidate strategy summaries:");
        for (StrategySummary summary : profile.strategySummaries) {
            System.out.println(summary);
        }
    }

    private Map<String, Integer> topCounts(Map<String, Integer> counts, int limit) {
        Map<String, Integer> top = new LinkedHashMap<>();
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(limit)
                .forEach(entry -> top.put(entry.getKey(), entry.getValue()));
        return top;
    }

    private String sourceFileName(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String normalized = reference.trim().replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        return slash < 0 ? normalized : normalized.substring(slash + 1);
    }

    private String stem(String fileName) {
        if (fileName == null) {
            return "";
        }
        int dot = fileName.lastIndexOf('.');
        return dot < 0 ? fileName : fileName.substring(0, dot);
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "");
    }

    private String canonicalFamilyName(String sourceMapFileName) {
        String stem = stem(sourceMapFileName);
        return stem.replaceAll("([A-Za-z]+)(E|N)(\\d+)$", "$1$3");
    }

    private int commonPrefixLength(String left, String right) {
        int max = Math.min(left.length(), right.length());
        int index = 0;
        while (index < max && left.charAt(index) == right.charAt(index)) {
            index++;
        }
        return index;
    }

    private static class RootFileIndex {

        private final Map<String, Path> filesByName = new LinkedHashMap<>();

        private static RootFileIndex from(Path root) throws Exception {
            RootFileIndex index = new RootFileIndex();
            try (Stream<Path> stream = Files.walk(root)) {
                stream.filter(Files::isRegularFile).forEach(path -> {
                    String fileName = path.getFileName().toString().toLowerCase(Locale.ROOT);
                    index.filesByName.putIfAbsent(fileName, path);
                });
            }
            return index;
        }

        private Path find(String sourceFileName) {
            if (sourceFileName == null || sourceFileName.isBlank()) {
                return null;
            }
            return filesByName.get(sourceFileName.toLowerCase(Locale.ROOT));
        }
    }

    private class MultipleSpriteMapRecord {

        private String sourceMapFileName;
        private String foregroundImage;
        private String canonicalFamilyName;
        private int spriteMapListSize;
        private List<String> resourceSlotTexts = new ArrayList<>();
        private List<SpriteMapReferenceDetail> details = new ArrayList<>();

        private boolean firstItemFound() {
            return !details.isEmpty() && details.get(0).existsInCompleteRoot;
        }

        private boolean allItemsFound() {
            return details.stream().allMatch(detail -> detail.existsInCompleteRoot);
        }

        private int nonMissingReferenceAbsentInRootCount() {
            return (int) details.stream()
                    .filter(detail -> detail.status == BheMapReferenceStatus.FOUND_DEFERRED)
                    .filter(detail -> !detail.existsInCompleteRoot)
                    .count();
        }

        private boolean hasUniqueMapNameMatch() {
            String mapName = normalize(stem(sourceMapFileName));
            return details.stream()
                    .filter(detail -> {
                        String sourceName = normalize(stem(detail.sourceFileName));
                        return !sourceName.isBlank() && (mapName.contains(sourceName) || sourceName.contains(mapName));
                    })
                    .count() == 1;
        }

        private boolean hasUniqueForegroundMatch() {
            String foregroundStem = normalize(stem(sourceFileName(foregroundImage)));
            if (foregroundStem.isBlank()) {
                return false;
            }
            return details.stream()
                    .filter(detail -> commonPrefixLength(foregroundStem, normalize(stem(detail.sourceFileName))) >= 6)
                    .count() == 1;
        }

        private boolean hasUniqueResourceSlotMatch() {
            List<String> slotStems = resourceSlotTexts.stream()
                    .map(value -> normalize(stem(sourceFileName(value))))
                    .filter(value -> !value.isBlank())
                    .toList();
            if (slotStems.isEmpty()) {
                return false;
            }
            return details.stream()
                    .filter(detail -> slotStems.stream()
                            .anyMatch(slotStem -> commonPrefixLength(slotStem, normalize(stem(detail.sourceFileName))) >= 6))
                    .count() == 1;
        }

        @Override
        public String toString() {
            return "sourceMapFileName=" + sourceMapFileName
                    + ", spriteMapListSize=" + spriteMapListSize
                    + ", foregroundImage=" + foregroundImage
                    + ", resourceSlotTexts=" + resourceSlotTexts
                    + ", details=" + details;
        }
    }

    private class SpriteMapReferenceDetail {

        private String sourceMapFileName;
        private String sourceText;
        private String sourceFileName;
        private String targetText;
        private String sourcePath;
        private BheMapReferenceStatus status;
        private boolean existsInCompleteRoot;
        private String missingReason;

        private String missingLine() {
            return "sourceMapFileName=" + sourceMapFileName
                    + ", sourceText=" + sourceText
                    + ", sourceFileName=" + sourceFileName
                    + ", targetText=" + targetText
                    + ", sourcePath=" + sourcePath
                    + ", status=" + status
                    + ", existsInCompleteRoot=" + existsInCompleteRoot
                    + ", missingReason=" + missingReason;
        }

        @Override
        public String toString() {
            return "{sourceText=" + sourceText
                    + ", targetText=" + targetText
                    + ", sourceFileName=" + sourceFileName
                    + ", sourcePath=" + sourcePath
                    + ", status=" + status
                    + ", existsInCompleteRoot=" + existsInCompleteRoot + "}";
        }
    }

    private static class StrategySummary {

        private String name;
        private int coveredMapCount;
        private String riskLevel;
        private String evidence;

        @Override
        public String toString() {
            return "strategyName=" + name
                    + ", coveredMapCount=" + coveredMapCount
                    + ", riskLevel=" + riskLevel
                    + ", evidence=" + evidence;
        }
    }

    private static class SpriteMapListProfile {

        private int totalMapCount;
        private int multipleMapCount;
        private int totalMultipleReferences;
        private int firstItemFoundMapCount;
        private int allItemsFoundMapCount;
        private int mapNameMatchedMapCount;
        private int foregroundRelatedMapCount;
        private int resourceSlotRelatedMapCount;
        private int nonMissingReferenceAbsentInRootCount;
        private Map<Integer, Integer> sizeDistribution = new TreeMap<>();
        private Map<String, Integer> sourceFileNameCounts = new LinkedHashMap<>();
        private Map<String, Integer> canonicalFamilyCounts = new LinkedHashMap<>();
        private List<MultipleSpriteMapRecord> multipleRecords = new ArrayList<>();
        private List<SpriteMapReferenceDetail> missingReferences = new ArrayList<>();
        private List<StrategySummary> strategySummaries = new ArrayList<>();
    }
}
