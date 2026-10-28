package com.giga.nexas.transfer.bhe2bsdx.meka.shared;

import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MekaPreloadBudgetTest {
    private static final String FILE_NAME = "BQMekaMemory.dat";

    @TempDir
    Path tempDir;

    @Test
    void inheritedNonIntegerSchemaDoesNotFallBackOrRewriteFiles() throws Exception {
        byte[] inherited = budgetRows(103);
        ByteBuffer.wrap(inherited).order(ByteOrder.LITTLE_ENDIAN).putInt(4, 1);
        assertRejectedInheritedTableIsUntouched(inherited);
    }

    @Test
    void inherited115RowsAreNotTruncatedTo114OrReplacedByFallback() throws Exception {
        assertRejectedInheritedTableIsUntouched(budgetRows(115));
    }

    @Test
    void inheritedNegativeLastRowDoesNotRewriteEarlierValidRows() throws Exception {
        byte[] inherited = budgetRows(114);
        ByteBuffer.wrap(inherited).order(ByteOrder.LITTLE_ENDIAN).putInt(inherited.length - Integer.BYTES, -1);
        assertRejectedInheritedTableIsUntouched(inherited);
    }

    @Test
    void inheritedTruncatedRowDoesNotFallBackOrRewriteFiles() throws Exception {
        byte[] inherited = budgetRows(103);
        assertRejectedInheritedTableIsUntouched(Arrays.copyOf(inherited, inherited.length - 1));
    }

    @Test
    void expands103RowsTo104WithoutChangingTheSourceOrExistingZero() throws Exception {
        byte[] original = budgetRows(103);
        Path sourceDir = Files.createDirectories(tempDir.resolve("original"));
        Path source = sourceDir.resolve(FILE_NAME);
        Files.write(source, original);
        Path outputRoot = Files.createDirectories(tempDir.resolve("out"));
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setBsdxDatDir(sourceDir);
        TsukuyomiImportedAssetSet assets = new TsukuyomiImportedAssetSet();
        assets.setOutputRootDir(outputRoot);

        MekaPreloadBudget.writeIfExpanded(request, roster(104), outputRoot, assets);

        Path output = outputRoot.resolve(FILE_NAME);
        byte[] expanded = Files.readAllBytes(output);
        assertAll(
                () -> assertEquals(424, expanded.length),
                () -> assertArrayEquals(original, Arrays.copyOf(expanded, original.length)),
                () -> assertEquals(0, ByteBuffer.wrap(expanded).order(ByteOrder.LITTLE_ENDIAN).getInt(8 + 20 * 4)),
                () -> assertEquals(100_000_000, ByteBuffer.wrap(expanded).order(ByteOrder.LITTLE_ENDIAN).getInt(420)),
                () -> assertArrayEquals(original, Files.readAllBytes(source)),
                () -> assertEquals(List.of(output), assets.getGeneratedDatFiles()));
    }

    @Test
    void unexpanded103RowsDoNotRequireASourceOrCreateOutput() throws Exception {
        Path missingSource = tempDir.resolve("missing-original");
        Path outputRoot = tempDir.resolve("not-created-output");
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setBsdxDatDir(missingSource);
        TsukuyomiImportedAssetSet assets = new TsukuyomiImportedAssetSet();
        assets.setOutputRootDir(outputRoot);

        MekaPreloadBudget.writeIfExpanded(request, roster(103), outputRoot, assets);

        assertAll(
                () -> assertFalse(Files.exists(missingSource)),
                () -> assertFalse(Files.exists(outputRoot)),
                () -> assertTrue(assets.getGeneratedDatFiles().isEmpty()));
    }

    private void assertRejectedInheritedTableIsUntouched(byte[] malformed) throws Exception {
        Path sourceDir = Files.createDirectories(tempDir.resolve("valid-fallback"));
        Path source = sourceDir.resolve(FILE_NAME);
        byte[] fallback = budgetRows(103);
        Files.write(source, fallback);
        Path outputRoot = Files.createDirectories(tempDir.resolve("out"));
        Path output = outputRoot.resolve(FILE_NAME);
        Files.write(output, malformed);
        TsukuyomiGraftRequest request = new TsukuyomiGraftRequest();
        request.setBsdxDatDir(sourceDir);
        TsukuyomiImportedAssetSet assets = new TsukuyomiImportedAssetSet();
        assets.setOutputRootDir(outputRoot);
        assets.getGeneratedDatFiles().add(output); // 模拟Importer已经复制并登记的继承表。
        try {
            assertThrows(IllegalStateException.class,
                    () -> MekaPreloadBudget.writeIfExpanded(request, roster(114), outputRoot, assets));
        } finally {
            assertAll("坏继承表优先被拒绝，不能回退或先写后拒绝",
                    () -> assertArrayEquals(malformed, Files.readAllBytes(output)),
                    () -> assertArrayEquals(fallback, Files.readAllBytes(source)),
                    () -> assertEquals(List.of(output), assets.getGeneratedDatFiles()));
        }
    }

    private static byte[] budgetRows(int rows) {
        ByteBuffer bytes = ByteBuffer.allocate(8 + rows * Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN);
        bytes.putInt(1).putInt(2);
        for (int id = 0; id < rows; id++) {
            bytes.putInt(id == 20 ? 0 : id == 18 ? 100_000_000 : 1_000 + id);
        }
        return bytes.array();
    }

    private static TsukuyomiBsdxBaselineBundle roster(int rows) {
        MekaGroupGrp group = new MekaGroupGrp();
        for (int id = 0; id < rows; id++) {
            var entry = new MekaGroupGrp.MekaGroup();
            entry.setExistFlag(0);
            group.getMekaList().add(entry);
        }
        TsukuyomiBsdxBaselineBundle baseline = new TsukuyomiBsdxBaselineBundle();
        baseline.setMekaGroupGrp(group);
        return baseline;
    }
}
