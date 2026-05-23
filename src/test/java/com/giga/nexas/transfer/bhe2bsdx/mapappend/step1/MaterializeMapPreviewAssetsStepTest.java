package com.giga.nexas.transfer.bhe2bsdx.mapappend.step1;

import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendAudit;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemSeverity;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapAppendProblemType;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapEntryPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewPlan;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.model.BheMapPreviewStatus;
import com.giga.nexas.transfer.bhe2bsdx.mapappend.preview.MaterializeMapPreviewAssetsStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaterializeMapPreviewAssetsStepTest {

    @TempDir
    Path tempDir;

    @Test
    void materializePreviewConsumesPlanNamesWithoutScanningMapGroup() throws Exception {
        Path bheRoot = tempDir.resolve("bhe");
        Path outputRoot = tempDir.resolve("out");
        Files.createDirectories(bheRoot);
        byte[] previewBytes = new byte[]{9, 8, 7};
        Files.write(bheRoot.resolve("source_by_plan.bmp"), previewBytes);
        Files.write(bheRoot.resolve("unused_mapgroup_style.bmp"), new byte[]{1});

        BheMapEntryPlan entryPlan = new BheMapEntryPlan();
        entryPlan.setSourcePreviewFileName("source_by_plan.bmp");
        entryPlan.setTargetPreviewFileName("target_by_plan.bmp");
        BheMapPreviewPlan previewPlan = new BheMapPreviewPlan();
        previewPlan.setSourcePreviewFileName("source_by_plan.bmp");
        previewPlan.setTargetPreviewFileName("target_by_plan.bmp");
        entryPlan.setPreviewPlan(previewPlan);
        BheMapAppendPlan plan = new BheMapAppendPlan();
        plan.getEntries().add(entryPlan);

        BheMapAppendAudit audit = new BheMapAppendAudit();
        new MaterializeMapPreviewAssetsStep().materialize(plan, bheRoot, List.of(), outputRoot, audit);

        // 预览图物料化只能消费 plan 中的文件名，不能自己扫描 MapGroup 或拼目标名。
        assertArrayEquals(previewBytes, Files.readAllBytes(outputRoot.resolve("target_by_plan.bmp")));
        assertFalse(Files.exists(outputRoot.resolve("source_by_plan.bmp")));
        assertFalse(Files.exists(outputRoot.resolve("unused_mapgroup_style.bmp")));
        assertEquals(BheMapPreviewStatus.COPIED, entryPlan.getPreviewPlan().getStatus());
        assertEquals(outputRoot.resolve("target_by_plan.bmp"), entryPlan.getPreviewPlan().getOutputPath());
        assertEquals(1, audit.getCopiedPreviewFiles().size());
        assertTrue(audit.getMissingPreviewFiles().isEmpty());
    }

    @Test
    void missingPreviewIsNonBlockingStructuredProblemOnPlanEntry() throws Exception {
        Path bheRoot = tempDir.resolve("empty-bhe");
        Path outputRoot = tempDir.resolve("missing-out");
        Files.createDirectories(bheRoot);

        BheMapEntryPlan entryPlan = new BheMapEntryPlan();
        entryPlan.setSourceGroupResourceName("map_missing");
        entryPlan.setSourceMapFileName("map_missing.map");
        BheMapPreviewPlan previewPlan = new BheMapPreviewPlan();
        previewPlan.setSourcePreviewFileName("T_map_missing.bmp");
        previewPlan.setTargetPreviewFileName("T_bhe_map_missing.bmp");
        entryPlan.setPreviewPlan(previewPlan);
        BheMapAppendPlan plan = new BheMapAppendPlan();
        plan.getEntries().add(entryPlan);

        BheMapAppendAudit audit = new BheMapAppendAudit();
        new MaterializeMapPreviewAssetsStep().materialize(plan, bheRoot, List.of(), outputRoot, audit);

        assertEquals(BheMapPreviewStatus.MISSING_NON_BLOCKING, entryPlan.getPreviewPlan().getStatus());
        assertEquals(1, audit.getMissingPreviewFiles().size());
        assertEquals(1, audit.getProblems().size());
        assertEquals(1, entryPlan.getProblems().size());
        assertEquals(BheMapAppendProblemType.MISSING_PREVIEW, audit.getProblems().get(0).getType());
        assertEquals(BheMapAppendProblemSeverity.NON_BLOCKING, audit.getProblems().get(0).getSeverity());
        assertEquals(BheMapAppendProblemType.MISSING_PREVIEW, entryPlan.getProblems().get(0).getType());
        assertEquals(BheMapAppendProblemSeverity.NON_BLOCKING, entryPlan.getProblems().get(0).getSeverity());
        assertEquals(0, audit.blockingProblemCount());
    }
}
