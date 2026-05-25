package com.giga.nexas.transfer.bhe2bsdx.mapappend.resource;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BheMapCatalogLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.catalog.BsdxMapBaselineLoader;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendRequest;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapCatalog;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapReferenceType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapResourceReference;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BsdxMapBaseline;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.plan.BheMapAppendPlanBuilder;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

class BheMissingSourceSpmResolutionTest {

    @Test
    void missingSourceSpmReferencesHaveNoBasenameCandidateInCompleteStaticResourceRoot() throws Exception {
        Path completeResourceRoot = requireCompleteStaticResourceRoot();
        BheMapAppendPlan plan = buildPlan(completeResourceRoot);

        List<MissingSpmReference> missingReferences = new ArrayList<>();
        for (BheMapEntryPlan entry : plan.getEntries()) {
            if (entry.getResourceReferences() == null) {
                continue;
            }
            for (BheMapResourceReference reference : entry.getResourceReferences().getReferences()) {
                if (reference.getType() == BheMapReferenceType.SPRITE_MAP_LIST && reference.isMissing()) {
                    missingReferences.add(new MissingSpmReference(
                            entry,
                            reference,
                            candidates(completeResourceRoot, reference.getSourceFileName())
                    ));
                }
            }
        }

        for (MissingSpmReference missingReference : missingReferences) {
            System.out.println("missingSourceSpm "
                    + "map=" + missingReference.entry().getSourceMapFileName()
                    + " sourceText=" + missingReference.reference().getSourceText()
                    + " sourceFileName=" + missingReference.reference().getSourceFileName()
                    + " targetText=" + missingReference.reference().getTargetText()
                    + " sourcePath=" + missingReference.reference().getSourcePath()
                    + " basenameCandidates=" + missingReference.candidates());
        }

        assertEquals(5, missingReferences.size());
        for (MissingSpmReference missingReference : missingReferences) {
            assertTrue(missingReference.candidates().isEmpty(),
                    "basename candidate exists and resource resolution should be fixed: "
                            + missingReference.entry().getSourceMapFileName()
                            + " / "
                            + missingReference.reference().getSourceFileName());
        }
    }

    private BheMapAppendPlan buildPlan(Path completeResourceRoot) {
        BheMapAppendRequest request = new BheMapAppendRequest();
        request.setBheStaticResourceRoot(completeResourceRoot);
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

    private List<Path> candidates(Path root, String sourceFileName) throws Exception {
        if (root == null || sourceFileName == null || sourceFileName.isBlank()) {
            return List.of();
        }
        String normalized = sourceFileName.trim().toLowerCase(Locale.ROOT);
        List<Path> matches = new ArrayList<>();
        try (var stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().trim().toLowerCase(Locale.ROOT).equals(normalized))
                    .forEach(matches::add);
        }
        return matches;
    }

    private Path requireCompleteStaticResourceRoot() {
        Path root = resolveCompleteStaticResourceRoot();
        assumeTrue(
                root != null && Files.isDirectory(root),
                "complete BHE static resource root must be provided by -Dbhe.staticResourceRoot or BHE_STATIC_RESOURCE_ROOT"
        );
        return root;
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

    private record MissingSpmReference(
            BheMapEntryPlan entry,
            BheMapResourceReference reference,
            List<Path> candidates
    ) {
    }
}
