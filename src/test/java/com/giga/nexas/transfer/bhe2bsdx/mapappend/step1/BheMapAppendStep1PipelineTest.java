package com.giga.nexas.transfer.bhe2bsdx.mapappend.step1;

import com.giga.nexas.dto.ResponseDTO;
import com.giga.nexas.dto.bhe.map.MapData;
import com.giga.nexas.service.BheBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.BheMapAppendPipeline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblem;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendResult;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendStatistics;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemSeverity;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewSourceKind;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapSourceEntry;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.naming.BheMapAppendNamingPolicy;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BheMapAppendStep1PipelineTest {

    @TempDir
    Path tempDir;

    @Test
    void requestRequiresExplicitStaticResourceRootInsteadOfMachineLocalDefault() {
        BheMapAppendRequest request = new BheMapAppendRequest();

        assertNull(request.getBheStaticResourceRoot());
        IllegalArgumentException error = assertThrows(
                IllegalArgumentException.class,
                request::resolveBheStaticResourceRoot
        );
        assertEquals("BHE 静态资源目录未设置", error.getMessage());
    }

    @Test
    void sameRunTargetGroupResourceNameDuplicateIsBlockingCollision() throws Exception {
        Path mapDir = tempDir.resolve("duplicate-map");
        Files.createDirectories(mapDir);
        Files.write(mapDir.resolve("mapDup.map"), new byte[]{1});

        BheMapCatalog catalog = new BheMapCatalog();
        catalog.setTotalGroupCount(2);
        catalog.getEntries().add(sourceEntry(1, "mapDup"));
        catalog.getEntries().add(sourceEntry(2, "mapDup"));

        BheMapAppendAudit audit = new BheMapAppendAudit();
        BheMapAppendPlan plan = new BheMapAppendPlanBuilder(
                new BheMapAppendNamingPolicy(),
                new ParsedMapStubBheBinService()
        ).build(
                catalog,
                mapDir,
                tempDir.resolve("empty-static"),
                new BsdxMapBaseline(),
                "windows-31j",
                audit
        );

        // 本轮 targetGroupResourceName 重复会让后续追加无法唯一定位目标条目，必须进入阻塞审计。
        assertEquals(2, plan.size());
        assertEquals(1, audit.blockingProblemCount());
        assertEquals(1, audit.getTargetNameCollisions().size());
        assertEquals(1, plan.getEntries().get(1).getProblems().size());
        assertEquals(BheMapAppendProblemType.TARGET_NAME_COLLISION, plan.getEntries().get(1).getProblems().get(0).getType());
        assertEquals(BheMapAppendProblemSeverity.BLOCKING, plan.getEntries().get(1).getProblems().get(0).getSeverity());
    }

    @Test
    void blockingSummaryDoesNotIncludeNonBlockingInternalResourceMissing() {
        BheMapAppendAudit audit = new BheMapAppendAudit();
        BheMapAppendProblem missingInternalResource = new BheMapAppendProblem();
        missingInternalResource.setType(BheMapAppendProblemType.MISSING_INTERNAL_RESOURCE);
        missingInternalResource.setSeverity(BheMapAppendProblemSeverity.NON_BLOCKING);
        missingInternalResource.setMessage("missing internal resource marker");

        audit.addMissingResourceFile(missingInternalResource);

        assertEquals(0, audit.blockingProblemCount());
        assertFalse(audit.blockingSummary().contains("missing internal resource marker"));
    }

    @Test
    void realStep1BuildsConsumablePlanAndMaterializesPreviewOnlyFromPlan() throws Exception {
        BheMapAppendRequest request = new BheMapAppendRequest();
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        Path staticResourceRoot = tempDir.resolve("bhe-static");
        materializeSyntheticStaticResources(request, sourceCatalog, staticResourceRoot);

        Path fallbackRoot = tempDir.resolve("fallback");
        Files.createDirectories(fallbackRoot);
        Files.write(fallbackRoot.resolve("T_mapBlack_S01.bmp"), new byte[]{1, 2, 3});

        request.setBheStaticResourceRoot(staticResourceRoot);
        request.setOutputRoot(tempDir.resolve("out"));
        request.setBsdxPreviewFallbackRoots(List.of(fallbackRoot));

        BheMapAppendResult result = new BheMapAppendPipeline().executeStep1(request);
        BheMapAppendStatistics statistics = result.getStatistics();

        // 这里直接读取 BHE MapGroup，证明 Step1 入口不是从 preview 文件夹反推地图清单。
        assertEquals(sourceCatalog.getTotalGroupCount(), statistics.getBheMapGroupTotalCount());
        assertEquals(sourceCatalog.getEntries().size(), statistics.getBheMapGroupMigratableCount());

        assertFalse(result.getAppendPlan().getEntries().isEmpty());
        assertEquals(statistics.getBheMapGroupMigratableCount(), statistics.getPlanEntryCount());
        assertEquals(statistics.getPlanEntryCount(), statistics.getMatchedMapFileCount());
        assertTrue(statistics.getParsedMapFileCount() > 100);

        BheMapEntryPlan sample = result.getAppendPlan().getEntries().stream()
                .filter(entry -> entry.getSourceGroupResourceName().equalsIgnoreCase("mapb06_kouya01"))
                .findFirst()
                .orElseThrow();

        assertTrue(sample.isSourceMapFound());
        assertTrue(sample.isSourceMapParsed());
        assertTrue(Files.exists(sample.getSourceMapPath()));
        assertNotNull(sample.getSourceGroupName());
        assertNotNull(sample.getSourceGroupCodeName());
        assertEquals("mapB06_Kouya01", sample.getSourceGroupResourceName());
        assertTrue(sample.getSourceMapGroupPairArray1Count() >= 0);
        assertTrue(sample.getSourceMapGroupArray2Count() >= 0);
        assertTrue(sample.getSourceMapGroupArray3Count() >= 0);
        BheMapEntryPlan catalogSample = sourceCatalog.getEntries().stream()
                .filter(entry -> entry.getGroupResourceName().equals(sample.getSourceGroupResourceName()))
                .findFirst()
                .map(entry -> {
                    BheMapEntryPlan plan = new BheMapEntryPlan();
                    plan.setSourceMapGroupInt1(entry.getMapGroupInt1());
                    plan.setSourceMapGroupItemCount(entry.getItemCount());
                    plan.setSourceMapGroupPairArray1Count(entry.getPairArray1Count());
                    plan.setSourceMapGroupArray2Count(entry.getArray2Count());
                    plan.setSourceMapGroupArray3Count(entry.getArray3Count());
                    return plan;
                })
                .orElseThrow();
        assertEquals(catalogSample.getSourceMapGroupInt1(), sample.getSourceMapGroupInt1());
        assertEquals(catalogSample.getSourceMapGroupItemCount(), sample.getSourceMapGroupItemCount());
        assertEquals(catalogSample.getSourceMapGroupPairArray1Count(), sample.getSourceMapGroupPairArray1Count());
        assertEquals(catalogSample.getSourceMapGroupArray2Count(), sample.getSourceMapGroupArray2Count());
        assertEquals(catalogSample.getSourceMapGroupArray3Count(), sample.getSourceMapGroupArray3Count());
        assertEquals(sample.getSourceGroupResourceName(), sample.getSourceMapGroupSnapshot().getGroupResourceName());
        assertEquals(sample.getSourceMapGroupItemCount(), sample.getSourceMapGroupSnapshot().getItems().size());
        assertEquals(sample.getSourceMapGroupPairArray1Count(), sample.getSourceMapGroupSnapshot().getPairArray1().size());
        assertEquals(sample.getSourceMapGroupArray2Count(), sample.getSourceMapGroupSnapshot().getArray2().size());
        assertEquals(sample.getSourceMapGroupArray3Count(), sample.getSourceMapGroupSnapshot().getArray3().size());
        assertEquals("bhe_mapB06_Kouya01", sample.getTargetGroupResourceName());
        assertEquals("mapB06_Kouya01.map", sample.getSourceMapFileName());
        assertEquals("bhe_mapB06_Kouya01.map", sample.getTargetMapFileName());
        assertEquals("T_mapB06_Kouya01.bmp", sample.getSourcePreviewFileName());
        assertEquals("T_bhe_mapB06_Kouya01.bmp", sample.getTargetPreviewFileName());
        assertTrue(sample.getSourceMapDataSummary().getMagic().startsWith("MAPDATA"));
        assertTrue(sample.getSourceMapDataSummary().getWidth() > 0);
        assertTrue(sample.getSourceMapDataSummary().getHeight() > 0);
        assertEquals(
                sample.getSourceMapDataSummary().getWidth() * sample.getSourceMapDataSummary().getHeight(),
                sample.getSourceMapDataSummary().getTileRecordCount()
        );
        assertEquals(8, sample.getSourceMapDataSummary().getResourceSlotBlockCount());
        assertEquals(9, sample.getSourceMapDataSummary().getScriptEntryGroupCount());
        assertEquals(sample.getResourceReferences().getSpriteMapFiles().size(), sample.getSourceMapDataSummary().getSpriteMapCount());
        assertNotNull(sample.getResourceReferences().getForegroundImage());
        assertFalse(sample.getResourceReferences().getSpriteMapFiles().isEmpty());
        assertFalse(sample.getResourceReferences().getReferences().isEmpty());
        assertTrue(sample.getResourceReferences().getReferences().stream()
                .anyMatch(reference -> reference.getType() == BheMapReferenceType.FOREGROUND
                        && reference.isFound()
                        && reference.getTargetFileName().startsWith("bhe_")
                        && reference.getTargetText().endsWith(reference.getTargetFileName())));
        assertTrue(sample.getResourceReferences().getReferences().stream()
                .anyMatch(reference -> reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST
                        && reference.isFound()
                        && reference.getTargetFileName().startsWith("bhe_")
                        && reference.getTargetText().endsWith(reference.getTargetFileName())));
        BheMapResourceReference resourceSlotReference = result.getAppendPlan().getEntries().stream()
                .flatMap(entry -> entry.getResourceReferences().getReferences().stream())
                .filter(reference -> reference.getType() == BheMapReferenceType.RESOURCE_SLOT)
                .findFirst()
                .orElseThrow();
        assertNotNull(resourceSlotReference.getResourceSlotNum());
        assertNotNull(resourceSlotReference.getResourceSlotParam0());
        assertNotNull(resourceSlotReference.getResourceSlotParam1());
        assertNotNull(resourceSlotReference.getResourceSlotParam2());
        assertNotNull(resourceSlotReference.getResourceSlotParam3());
        assertNotNull(resourceSlotReference.getResourceSlotParam4());
        assertEquals("bhe_" + resourceSlotReference.getSourceFileName(), resourceSlotReference.getTargetFileName());
        assertTrue(resourceSlotReference.getTargetText().endsWith(resourceSlotReference.getTargetFileName()));

        Path copiedPreview = request.resolveOutputRoot().resolve(sample.getTargetPreviewFileName());
        assertTrue(Files.exists(copiedPreview));
        assertFalse(Files.exists(request.resolveOutputRoot().resolve(sample.getSourcePreviewFileName())));
        assertEquals(sample.getSourcePreviewFileName(), sample.getPreviewPlan().getSourcePreviewFileName());
        assertEquals(sample.getTargetPreviewFileName(), sample.getPreviewPlan().getTargetPreviewFileName());
        assertEquals(BheMapPreviewStatus.COPIED, sample.getPreviewPlan().getStatus());
        assertEquals(BheMapPreviewSourceKind.BHE, sample.getPreviewPlan().getSourceKind());
        assertTrue(Files.exists(sample.getPreviewPlan().getSourcePath()));
        assertEquals(copiedPreview, sample.getPreviewPlan().getOutputPath());

        BheMapEntryPlan mapBlack = result.getAppendPlan().getEntries().stream()
                .filter(entry -> entry.getSourceGroupResourceName().equalsIgnoreCase("mapBlack_S01"))
                .findFirst()
                .orElseThrow();
        assertEquals(BheMapPreviewStatus.COPIED, mapBlack.getPreviewPlan().getStatus());
        assertEquals(BheMapPreviewSourceKind.BSDX_FALLBACK, mapBlack.getPreviewPlan().getSourceKind());
        assertEquals("T_mapBlack_S01.bmp", mapBlack.getPreviewPlan().getBsdxPreviewFallbackFileName());

        assertEquals(statistics.getPlanEntryCount(), statistics.getPreviewCopiedCount());
        assertEquals(0, statistics.getMissingPreviewCount());
        assertEquals(0, statistics.getBlockingProblemCount());
        assertEquals(0, result.getAudit().getProblems().stream()
                .filter(problem -> problem.getSeverity() == BheMapAppendProblemSeverity.BLOCKING)
                .count());
        assertTrue(result.getAudit().getProblems().stream()
                .allMatch(problem -> problem.getMessage() != null && !problem.getMessage().isBlank()));
        assertEquals(5, result.getAudit().getProblems().stream()
                .filter(problem -> problem.getType() == BheMapAppendProblemType.MISSING_INTERNAL_RESOURCE)
                .filter(problem -> problem.getSeverity() == BheMapAppendProblemSeverity.NON_BLOCKING)
                .count());
        assertTrue(result.getAudit().getProblems().stream()
                .anyMatch(problem -> problem.getType() == BheMapAppendProblemType.MISSING_INTERNAL_RESOURCE
                        && problem.getSeverity() == BheMapAppendProblemSeverity.NON_BLOCKING
                        && problem.getSourceMapFileName().equals("mapB06_Harbor03.map")
                        && problem.getReferenceText().contains("設置物：建築物.spm")));
        BheMapEntryPlan harbor03 = result.getAppendPlan().getEntries().stream()
                .filter(entry -> entry.getSourceMapFileName().equals("mapB06_Harbor03.map"))
                .findFirst()
                .orElseThrow();
        assertTrue(harbor03.getProblems().stream()
                .anyMatch(problem -> problem.getType() == BheMapAppendProblemType.MISSING_INTERNAL_RESOURCE
                        && problem.getSeverity() == BheMapAppendProblemSeverity.NON_BLOCKING
                        && problem.getReferenceText().contains("設置物：建築物.spm")));
        assertTrue(harbor03.getProblems().stream()
                .filter(problem -> problem.getType() == BheMapAppendProblemType.MISSING_INTERNAL_RESOURCE)
                .allMatch(problem -> problem.getSourceMapFileName() != null
                        && problem.getReferenceText() != null
                        && problem.getSeverity() == BheMapAppendProblemSeverity.NON_BLOCKING));
        assertEquals(0, harbor03.getProblems().stream()
                .filter(problem -> problem.getSeverity() == BheMapAppendProblemSeverity.BLOCKING)
                .count());
        assertEquals(5, result.getAudit().getMissingResourceFiles().size());
        assertTrue(result.getAudit().getMissingResourceFiles().stream()
                .anyMatch(problem -> problem.contains("設置物：薄壁：夕方.spm")));
        assertTrue(result.getAudit().getMissingResourceFiles().stream()
                .anyMatch(problem -> problem.contains("設置物：建築物.spm")));
        List<BheMapResourceReference> missingReferences = result.getAppendPlan().getEntries().stream()
                .flatMap(entry -> entry.getResourceReferences().getReferences().stream())
                .filter(BheMapResourceReference::isMissing)
                .toList();
        assertEquals(5, missingReferences.size());
        assertTrue(missingReferences.stream()
                .allMatch(reference -> reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST));
        assertTrue(missingReferences.stream()
                .allMatch(reference -> reference.getSourcePath() == null));

        System.out.println("BHE MapGroup entries: " + statistics.getBheMapGroupTotalCount());
        System.out.println("Matched .map files: " + statistics.getMatchedMapFileCount());
        System.out.println("Parsed .map files: " + statistics.getParsedMapFileCount());
        System.out.println("Plan entries: " + statistics.getPlanEntryCount());
        System.out.println("Preview copied: " + statistics.getPreviewCopiedCount());
        System.out.println("Missing preview: " + statistics.getMissingPreviewCount());
        System.out.println("Blocking problems: " + statistics.getBlockingProblemCount());
    }

    private void materializeSyntheticStaticResources(
            BheMapAppendRequest request,
            BheMapCatalog sourceCatalog,
            Path staticResourceRoot
    ) throws Exception {
        Files.createDirectories(staticResourceRoot);
        BheMapAppendNamingPolicy namingPolicy = new BheMapAppendNamingPolicy();
        BheBinService bheBinService = new BheBinService();
        Set<String> intentionallyMissingInternalResources = Set.of(
                "設置物：薄壁：夕方.spm",
                "設置物：建築物.spm"
        );

        for (BheMapSourceEntry entry : sourceCatalog.getEntries()) {
            if (!"mapBlack_S01".equalsIgnoreCase(entry.getGroupResourceName())) {
                writeSyntheticResource(staticResourceRoot.resolve(namingPolicy.toPreviewFileName(entry.getGroupResourceName())));
            }

            Path sourceMapPath = resolveCaseInsensitive(
                    request.resolveBheMapDir(),
                    namingPolicy.toMapFileName(entry.getGroupResourceName())
            );
            MapData mapData = (MapData) bheBinService.parse(sourceMapPath.toString(), request.getCharset()).getData();
            for (String reference : collectInternalReferences(mapData)) {
                String fileName = sourceFileName(reference);
                if (!intentionallyMissingInternalResources.contains(fileName)) {
                    writeSyntheticResource(staticResourceRoot.resolve(fileName));
                }
            }
        }
    }

    private Set<String> collectInternalReferences(MapData mapData) {
        Set<String> references = new LinkedHashSet<>();
        addReference(references, mapData.getForegroundImage());
        if (mapData.getNamedResourceSlotBlocks() != null) {
            for (MapData.NamedResourceSlotBlock slot : mapData.getNamedResourceSlotBlocks()) {
                addReference(references, slot == null ? null : slot.getSlotText());
            }
        }
        if (mapData.getSpriteMapList() != null && !mapData.getSpriteMapList().isEmpty()) {
            for (String spriteMap : mapData.getSpriteMapList()) {
                addReference(references, spriteMap);
            }
        } else {
            addReference(references, mapData.getSpriteMap());
        }
        return references;
    }

    private void addReference(Set<String> references, String reference) {
        if (reference != null && !reference.isBlank()) {
            references.add(reference.trim());
        }
    }

    private Path resolveCaseInsensitive(Path directory, String fileName) throws Exception {
        String normalizedFileName = normalize(fileName);
        try (Stream<Path> stream = Files.list(directory)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> normalize(path.getFileName().toString()).equals(normalizedFileName))
                    .findFirst()
                    .orElseThrow();
        }
    }

    private String sourceFileName(String reference) {
        return Path.of(reference.trim().replace('\\', '/')).getFileName().toString();
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private void writeSyntheticResource(Path path) throws Exception {
        if (!Files.exists(path)) {
            Files.write(path, new byte[]{1});
        }
    }

    private BheMapSourceEntry sourceEntry(int sourceMapIndex, String groupResourceName) {
        BheMapSourceEntry entry = new BheMapSourceEntry();
        entry.setSourceMapIndex(sourceMapIndex);
        entry.setGroupName(groupResourceName);
        entry.setGroupCodeName(groupResourceName);
        entry.setGroupResourceName(groupResourceName);
        return entry;
    }

    private static class ParsedMapStubBheBinService extends BheBinService {

        @Override
        public ResponseDTO<?> parse(String path, String charset) throws IOException {
            MapData mapData = new MapData();
            mapData.setMagic("MAPDATA VER-1.00");
            mapData.setWidth(1);
            mapData.setHeight(1);
            return new ResponseDTO<>(mapData, "ok");
        }
    }
}
