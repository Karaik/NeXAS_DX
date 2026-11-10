package com.giga.nexas.jinki;

import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.v2.Jinki2BsdxTransferV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.output.OutputManifestComparator;
import com.giga.nexas.transfer.jinki2bsdx.v2.exe.PatchedExeComparator;
import com.giga.nexas.util.PacUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class TestJinki2BsdxPipelineV2FinalParity {

    @TempDir
    Path tempDir;

    @Test
    public void jinkiPipelineV2_shouldMatchOldPipelineFinalOutputsAndPackedContents() throws IOException {
        AkaoGraftRequest oldRequest = new AkaoGraftRequest();
        oldRequest.setPatchMenuData(true);
        AkaoGraftResult oldResult = new Jinki2BsdxSingleRunner().run(oldRequest);
        Path oldPacCopy = copyPac(oldResult.getPacPackPlan().getOutputPacPath(), tempDir.resolve("old-update3.pac"));

        AkaoGraftRequest v2Request = new AkaoGraftRequest();
        v2Request.setPatchMenuData(true);
        AkaoGraftResult v2Result = Jinki2BsdxTransferV2.process(v2Request);
        Path v2PacCopy = copyPac(v2Result.getPacPackPlan().getOutputPacPath(), tempDir.resolve("v2-update3.pac"));

        Assertions.assertEquals(
                oldResult.getJinkiPackage().getSpmByFileName().keySet(),
                v2Result.getJinkiPackage().getSpmByFileName().keySet(),
                "V2 source loader should preserve JINKI SPM registry"
        );
        Assertions.assertEquals(
                oldResult.getJinkiPackage().getWazByFileName().keySet(),
                v2Result.getJinkiPackage().getWazByFileName().keySet(),
                "V2 source loader should preserve JINKI WAZ registry"
        );
        Assertions.assertEquals(
                oldResult.getBsdxBaseline().getSpmByFileName().keySet(),
                v2Result.getBsdxBaseline().getSpmByFileName().keySet(),
                "V2 baseline loader should preserve BSDX SPM registry"
        );
        Assertions.assertEquals(
                oldResult.getBsdxBaseline().getWazByFileName().keySet(),
                v2Result.getBsdxBaseline().getWazByFileName().keySet(),
                "V2 baseline loader should preserve BSDX WAZ registry"
        );
        Assertions.assertEquals(
                oldResult.getImportPlan(),
                v2Result.getImportPlan(),
                "V2 import/resource-closure plan should match old pipeline import plan"
        );
        Assertions.assertEquals(
                oldResult.getGrpAppendPlan(),
                v2Result.getGrpAppendPlan(),
                "V2 GRP append/mapping plan should match old pipeline GRP append plan"
        );
        Assertions.assertEquals(
                oldResult.getReboundAkaoMek().getMekBasicInfo(),
                v2Result.getReboundAkaoMek().getMekBasicInfo(),
                "V2 rebound MEK basic info should match old pipeline"
        );
        Assertions.assertEquals(
                oldResult.getReboundAkaoMek().getMekWeaponInfoMap().keySet(),
                v2Result.getReboundAkaoMek().getMekWeaponInfoMap().keySet(),
                "V2 rebound MEK weapon keys should match old pipeline"
        );
        Assertions.assertEquals(
                oldResult.getReboundAkaoWaz().getSkillList().size(),
                v2Result.getReboundAkaoWaz().getSkillList().size(),
                "V2 rebound WAZ skill count should match old pipeline"
        );
        Assertions.assertEquals(
                oldResult.getReboundAkaoWaz().getSkillList().stream().map(skill -> skill.getSkillNameEnglish()).toList(),
                v2Result.getReboundAkaoWaz().getSkillList().stream().map(skill -> skill.getSkillNameEnglish()).toList(),
                "V2 rebound WAZ skill keys should match old pipeline"
        );

        Assertions.assertTrue(
                new PatchedExeComparator().byteIdentical(
                        oldResult.getExePatchPlan().getOutputExePath(),
                        v2Result.getExePatchPlan().getOutputExePath()
                ),
                "V2 patched exe should be byte-identical to old pipeline patched exe"
        );
        Assertions.assertEquals(
                oldResult.getExePatchPlan().getTargetOffsets(),
                v2Result.getExePatchPlan().getTargetOffsets(),
                "V2 exe patch target offsets should match old pipeline"
        );

        OutputManifestComparator comparator = new OutputManifestComparator();
        OutputManifestComparator.ComparisonResult outputDirCompare = comparator.compareDirectories(
                oldResult.getImportedAssetSet().getOutputRootDir(),
                v2Result.getImportedAssetSet().getOutputRootDir(),
                true
        );
        Assertions.assertTrue(
                outputDirCompare.isIdentical(),
                "V2 output directory differs from old pipeline output: " + outputDirCompare.allDifferences()
        );

        Path oldUnpacked = unpackPac(oldPacCopy);
        Path v2Unpacked = unpackPac(v2PacCopy);
        OutputManifestComparator.ComparisonResult unpackedCompare = comparator.compareDirectories(oldUnpacked, v2Unpacked, true);
        Assertions.assertTrue(
                unpackedCompare.isIdentical(),
                "V2 packed object contents differ from old pipeline packed contents: " + unpackedCompare.allDifferences()
        );
    }

    private Path copyPac(Path sourcePac, Path targetPac) throws IOException {
        Assertions.assertNotNull(sourcePac, "pac path should not be null");
        Assertions.assertTrue(Files.exists(sourcePac), "pac should exist: " + sourcePac);
        Files.copy(sourcePac, targetPac);
        return targetPac;
    }

    private Path unpackPac(Path pacPath) throws IOException {
        Path unpackDir = siblingWithoutExtension(pacPath);
        deleteRecursively(unpackDir);
        PacUtil.unpack(pacPath.toString());
        Assertions.assertTrue(Files.exists(unpackDir), "unpacked folder should exist: " + unpackDir);
        return unpackDir;
    }

    private Path siblingWithoutExtension(Path file) {
        String fileName = file.getFileName().toString();
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
        return file.resolveSibling(baseName);
    }

    private void deleteRecursively(Path root) throws IOException {
        if (root == null || !Files.exists(root)) {
            return;
        }
        try (var stream = Files.walk(root)) {
            for (Path path : stream.sorted((left, right) -> right.compareTo(left)).toList()) {
                Files.deleteIfExists(path);
            }
        }
    }
}
