package com.giga.nexas.jinki;

import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.BuildSidecarOutputsStep;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.OutputManifest;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.OutputManifestComparator;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.OutputResourceEntry;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.ResourceOverwriteAudit;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.SidecarResourceSpec;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.WriteOutputManifestStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;

public class TestJinki2BsdxSidecarOutputV2Parity {

    @TempDir
    Path tempDir;

    @Test
    public void sidecarAndManifestV2_shouldMatchOldPipelineOutputBoundary() throws IOException {
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPatchMenuData(true);
        AkaoGraftResult oldResult = new Jinki2BsdxSingleRunner().run(request);
        Path oldOutputRoot = oldResult.getImportedAssetSet().getOutputRootDir();

        Path sidecarOutputRoot = tempDir.resolve("sidecar-v2");
        ResourceOverwriteAudit overwriteAudit = new ResourceOverwriteAudit();
        OutputManifest sidecarManifest = new BuildSidecarOutputsStep().build(
                request,
                oldResult.getJinkiPackage(),
                oldResult.getBsdxBaseline(),
                sidecarOutputRoot,
                SidecarResourceSpec.defaultJinkiSidecars(),
                overwriteAudit
        );

        assertByteIdentical(oldOutputRoot, sidecarOutputRoot, "WeaponEquip.dat");
        Assertions.assertEquals(Set.of("WeaponEquip.dat"), new OutputManifestComparator().manifestFileSet(sidecarManifest));
        Assertions.assertTrue(
                sidecarManifest.getNotes().stream().anyMatch(note -> note.contains("ProgramMaterial.grp")),
                "ProgramMaterial sidecar declaration should be present in manifest notes"
        );

        WriteOutputManifestStep manifestStep = new WriteOutputManifestStep();
        OutputManifest oldOutputManifest = manifestStep.buildManifest(oldOutputRoot);
        OutputManifestComparator comparator = new OutputManifestComparator();
        Assertions.assertEquals(
                comparator.collectRelativeFiles(oldOutputRoot),
                comparator.manifestFileSet(oldOutputManifest),
                "manifest should cover every file in old pipeline output"
        );

        Path manifestPath = tempDir.resolve("audit/output-manifest.json");
        manifestStep.write(manifestPath, oldOutputManifest);
        Assertions.assertTrue(Files.exists(manifestPath));

        OutputManifestComparator.ComparisonResult selfCompare = comparator.compareDirectories(oldOutputRoot, oldOutputRoot, true);
        Assertions.assertTrue(selfCompare.isIdentical(), selfCompare.allDifferences().toString());

        ResourceOverwriteAudit duplicateAudit = new ResourceOverwriteAudit();
        Assertions.assertFalse(duplicateAudit.recordOutput("WeaponEquip.dat", "baseline"));
        Assertions.assertTrue(duplicateAudit.recordOutput("WeaponEquip.dat", "sidecar"));
        Assertions.assertFalse(duplicateAudit.getOverwriteNotes().isEmpty());
        Assertions.assertTrue(duplicateAudit.getOverwriteNotes().get(0).contains("WeaponEquip.dat"));

        OutputResourceEntry entry = sidecarManifest.getEntries().get(0);
        Assertions.assertEquals("dat", entry.getCategory());
        Assertions.assertFalse(entry.getSha256().isBlank());
        Assertions.assertTrue(entry.getSize() > 0);
    }

    private void assertByteIdentical(Path oldOutputRoot, Path v2OutputRoot, String fileName) throws IOException {
        Path oldFile = oldOutputRoot.resolve(fileName);
        Path v2File = v2OutputRoot.resolve(fileName);
        Assertions.assertTrue(Files.exists(oldFile), "old output missing " + fileName);
        Assertions.assertTrue(Files.exists(v2File), "v2 output missing " + fileName);
        Assertions.assertArrayEquals(
                Files.readAllBytes(oldFile),
                Files.readAllBytes(v2File),
                fileName + " should be byte-identical"
        );
    }
}
