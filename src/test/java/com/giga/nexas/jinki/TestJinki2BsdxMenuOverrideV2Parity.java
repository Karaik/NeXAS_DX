package com.giga.nexas.jinki;

import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.steps.LoadBsdxBaselineStep;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuLayoutPolicy;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuOverrideContext;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuOverridePipelineV2;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuOverrideSpec;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.MenuSlotMapping;
import com.giga.nexas.transfer.jinki2bsdx.v2.menu.ResolveMenuSlotStep;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class TestJinki2BsdxMenuOverrideV2Parity {

    @TempDir
    Path tempDir;

    @Test
    public void spec_shouldContainOnlyMinimalAkaoMenuOverrideFields() {
        MenuOverrideSpec spec = MenuOverrideSpec.defaultAkaoMenuOverride();

        Assertions.assertEquals(24, spec.getVisibleSlotIndex());
        Assertions.assertEquals(23, spec.getStateDonorRowIndex());
        Assertions.assertEquals(
                List.of("MOD_001_HELL_AKAO_001.png", "MOD_001_HELL_MEKA_AKAO_001.png"),
                spec.getMekaPilotImageNames()
        );
        Assertions.assertEquals(
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Moribito_2_001.png",
                        "MOD_001_SelectMekaMenuMeka_Moribito_2_002.png"
                ),
                spec.getSelectMenuMekaImageNames()
        );
        Assertions.assertEquals(MenuLayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR, spec.getMekaPilotLayoutPolicy());
        Assertions.assertEquals(MenuLayoutPolicy.ORIGIN_CENTER, spec.getSelectMenuMekaLayoutPolicy());
    }

    @Test
    public void resolveMenuSlot_shouldDeriveCurrentAkaoMenuSlot() {
        AkaoGraftRequest request = new AkaoGraftRequest();
        MenuOverrideContext context = new MenuOverrideContext(
                request,
                null,
                new LoadBsdxBaselineStep().loadBaseline(request),
                null,
                null,
                tempDir.resolve("resolve-only"),
                MenuOverrideSpec.defaultAkaoMenuOverride()
        );

        MenuSlotMapping mapping = new ResolveMenuSlotStep().resolve(context);

        Assertions.assertEquals(24, mapping.getSelectMenuRowIndex());
        Assertions.assertEquals(32, mapping.getSourceMekaIndex());
        Assertions.assertEquals(18, mapping.getSelectMenuAnimIndex());
        Assertions.assertEquals(29, mapping.getPilotRowIndex());
        Assertions.assertEquals(29, mapping.getPilotAnimIndex());
        Assertions.assertSame(mapping, context.getSlotMapping());
        Assertions.assertSame(mapping, context.getAudit().getSlotMapping());
        Assertions.assertFalse(context.getAudit().getSlotMappingNotes().isEmpty());
    }

    @Test
    public void menuOverrideV2_shouldMatchOldMenuOverrideOutputs() throws IOException {
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPatchMenuData(true);
        AkaoGraftResult oldResult = new Jinki2BsdxSingleRunner().run(request);

        Path v2OutputRoot = tempDir.resolve("menu-v2");
        MenuOverrideContext context = new MenuOverridePipelineV2().execute(
                request,
                oldResult.getJinkiPackage(),
                oldResult.getBsdxBaseline(),
                oldResult.getGrpAppendPlan(),
                oldResult.getReboundAkaoMek(),
                v2OutputRoot,
                MenuOverrideSpec.defaultAkaoMenuOverride()
        );

        Path oldOutputRoot = oldResult.getImportedAssetSet().getOutputRootDir();
        assertByteIdentical(oldOutputRoot, v2OutputRoot, "Meka.dat");
        assertByteIdentical(oldOutputRoot, v2OutputRoot, "MekaPilot.dat");
        assertByteIdentical(oldOutputRoot, v2OutputRoot, "SelectMekaMenu.dat");
        assertByteIdentical(oldOutputRoot, v2OutputRoot, "MekaPilot.spm");
        assertByteIdentical(oldOutputRoot, v2OutputRoot, "SelectMekaMenuMeka.spm");

        MenuOverrideSpec spec = context.getSpec();
        assertCopiedImageMatchesOldOutput(oldOutputRoot, v2OutputRoot, spec.getMekaPilotImageNames());
        assertCopiedImageMatchesOldOutput(oldOutputRoot, v2OutputRoot, spec.getSelectMenuMekaImageNames());

        Assertions.assertNotNull(context.getAudit().getSlotMapping());
        Assertions.assertFalse(context.getAudit().getWrittenFiles().isEmpty());
        Assertions.assertFalse(context.getAudit().getCopiedImages().isEmpty());
    }

    private void assertCopiedImageMatchesOldOutput(Path oldOutputRoot, Path v2OutputRoot, List<String> imageNames) throws IOException {
        for (String imageName : imageNames) {
            String fileName = Path.of(imageName).getFileName().toString();
            assertByteIdentical(oldOutputRoot, v2OutputRoot, fileName);
        }
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
