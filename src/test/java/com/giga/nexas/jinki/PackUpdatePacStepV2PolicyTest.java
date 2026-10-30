package com.giga.nexas.jinki;

import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.v2.pack.PackUpdatePacStepV2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class PackUpdatePacStepV2PolicyTest {
    @TempDir Path temp;

    @Test
    void standaloneRequestsStillEnablePackingByDefault() {
        assertTrue(new AkaoGraftRequest().isPackUpdatePac());
    }

    @Test
    void disabledIntermediatePackingDoesNotInvokeThePackerOrCreateFiles() throws Exception {
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPackUpdatePac(false);
        ImportedAssetSet assets = new ImportedAssetSet();
        Path missing = temp.resolve("not-created-assets");
        assets.setOutputRootDir(missing);

        var plan = new PackUpdatePacStepV2().packUpdatePac(request, assets);

        assertFalse(plan.isPacked());
        assertNull(plan.getOutputPacPath());
        assertEquals(missing.toAbsolutePath().normalize(), plan.getSourceFolder());
        assertFalse(Files.exists(missing));
        try (var children = Files.list(temp)) {
            assertEquals(0, children.count());
        }
    }

    @Test
    void disabledPackingKeepsAnExistingFinalPacUntouched() throws Exception {
        Path existing = temp.resolve("Update3.pac");
        byte[] original = "existing-test-sentinel".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        Files.write(existing, original);
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPackUpdatePac(false);
        ImportedAssetSet assets = new ImportedAssetSet();
        assets.setOutputRootDir(temp.resolve("assets"));

        var plan = new PackUpdatePacStepV2().packUpdatePac(request, assets);

        assertFalse(plan.isPacked());
        assertArrayEquals(original, Files.readAllBytes(existing));
        assertFalse(Files.exists(temp.resolve("assets.pacNew")));
    }
}
