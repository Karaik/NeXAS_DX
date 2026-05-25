package com.giga.nexas.transfer.bhe2bsdx.mapappend.output;

import com.giga.nexas.dto.bsdx.grp.groupmap.MapGroupGrp;
import com.giga.nexas.dto.bsdx.map.parser.MapDataParser;
import com.giga.nexas.dto.bsdx.spm.parser.SpmParser;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.composition.MapCompatibilityBlockingReason;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BsdxMapBaselineLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.mapdata.BheMapDataConversionIssueType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMapImportOutputWriterTest {

    @TempDir
    Path outputRoot;

    @Test
    void completeStaticResourceRootWritesConvertibleMapsAndAppendsMapGroupEntries() throws Exception {
        Path completeResourceRoot = resolveCompleteStaticResourceRoot();
        assumeTrue(
                completeResourceRoot != null && Files.isDirectory(completeResourceRoot),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );

        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
        BheMapAppendPlan plan = buildPlan(request);

        BheMapImportOutputResult result = new BheMapImportOutputWriter().write(
                plan,
                request.resolveBsdxMapGroupPath(),
                outputRoot,
                request.getCharset()
        );

        assertEquals(142, result.getTotalPlanEntries());
        assertEquals(137, result.getConvertedMapCount());
        assertEquals(137, result.getWrittenMapCount());
        assertEquals(33, result.getDirectWrittenMapCount());
        assertEquals(104, result.getCompositionWrittenMapCount());
        assertEquals(104, result.getComposedSpmWrittenCount());
        assertEquals(137, result.getAppendedMapGroupCount());
        assertEquals(0, result.getBlockedByMultipleSpriteMapListCount());
        assertEquals(0, result.getBlockedByUnsupportedScriptGroupCount());
        assertEquals(5, result.getBlockedByMissingSourceSpmCount());
        assertEquals(0, result.getBlockedByMissingFinalReferenceCount());
        assertEquals(5, result.compositionBlockingReasonCount(MapCompatibilityBlockingReason.MISSING_SOURCE_SPM));
        assertEquals(0, result.issueTypeCount(BheMapDataConversionIssueType.UNSUPPORTED_MULTIPLE_SPRITE_MAPS));
        assertEquals(0, result.issueTypeCount(BheMapDataConversionIssueType.UNSUPPORTED_SCRIPT_GROUP_INDEX));
        assertEquals(0, result.issueTypeCount(BheMapDataConversionIssueType.MISSING_FINAL_REFERENCE));
        assertEquals(0, result.issueTypeCount(BheMapDataConversionIssueType.MISSING_RESOURCE_REWRITE));
        assertTrue(result.isSpmWritten());
        assertTrue(result.isComposedSpmWritten());
        assertFalse(result.isPreviewWritten());
        assertFalse(result.isPacked());

        assertEquals(result.getBaselineMapGroupCount() + 137, result.getOutputMapGroupCount());
        assertTrue(Files.isRegularFile(result.getOutputMapGroupPath()));
        MapGroupGrp outputMapGroup = parseMapGroup(result.getOutputMapGroupPath(), request.getCharset());
        assertEquals(result.getOutputMapGroupCount(), outputMapGroup.getGroupList().size());

        List<String> writtenTargetGroupResourceNames = result.getWrittenMaps().stream()
                .map(BheMapImportWrittenMap::getTargetGroupResourceName)
                .toList();
        assertEquals(writtenTargetGroupResourceNames, result.getAppendedGroupResourceNames());
        List<String> appendedTailGroupResourceNames = outputMapGroup.getGroupList().stream()
                .skip(result.getBaselineMapGroupCount())
                .map(MapGroupGrp.MapGroup::getGroupResourceName)
                .toList();
        assertEquals(writtenTargetGroupResourceNames, appendedTailGroupResourceNames);

        MapDataParser mapDataParser = new MapDataParser();
        SpmParser spmParser = new SpmParser();
        for (BheMapImportWrittenMap writtenMap : result.getWrittenMaps()) {
            Path writtenMapPath = writtenMap.getMapPath();
            assertTrue(Files.isRegularFile(writtenMapPath));
            mapDataParser.parse(Files.readAllBytes(writtenMapPath), writtenMap.getTargetMapFileName(), request.getCharset());
            if (writtenMap.isComposition()) {
                assertTrue(Files.isRegularFile(writtenMap.getComposedSpmPath()));
                spmParser.parse(
                        Files.readAllBytes(writtenMap.getComposedSpmPath()),
                        writtenMap.getComposedSpmFileName(),
                        request.getCharset()
                );
            }
        }

        for (BheMapImportBlockedMap blockedMap : result.getBlockedMaps()) {
            assertFalse(Files.exists(outputRoot.resolve(blockedMap.getTargetMapFileName())));
        }

        assertEquals(137, countFilesByExtension(outputRoot, ".map"));
        assertEquals(104, result.getWrittenComposedSpmFileList().size());
        assertEquals(104, countFilesByExtension(outputRoot, ".spm"));
        assertEquals(0, countFilesByExtension(outputRoot, ".bmp"));
        assertEquals(0, countFilesByExtension(outputRoot, ".pac"));
        assertEquals(Set.of("MapGroup.grp"), topLevelGrpFiles(outputRoot));
    }

    private BheMapAppendPlan buildPlan(BheMapAppendRequest request) {
        BheMapCatalog sourceCatalog = new BheMapCatalogLoader().load(
                request.resolveBheMapGroupPath(),
                request.getCharset()
        );
        BsdxMapBaseline baseline = new BsdxMapBaselineLoader().load(
                request.resolveBsdxMapGroupPath(),
                request.getCharset()
        );
        return new BheMapAppendPlanBuilder().build(
                sourceCatalog,
                request.resolveBheMapDir(),
                request.resolveBheStaticResourceRoot(),
                baseline,
                request.getCharset(),
                new BheMapAppendAudit()
        );
    }

    private MapGroupGrp parseMapGroup(Path mapGroupPath, String charset) throws Exception {
        return (MapGroupGrp) new BsdxBinService().parse(mapGroupPath.toString(), charset).getData();
    }

    private long countFilesByExtension(Path root, String extension) throws Exception {
        try (var stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(extension))
                    .count();
        }
    }

    private Set<String> topLevelGrpFiles(Path root) throws Exception {
        try (var stream = Files.list(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .map(path -> path.getFileName().toString())
                    .filter(name -> name.toLowerCase(Locale.ROOT).endsWith(".grp"))
                    .collect(Collectors.toSet());
        }
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
