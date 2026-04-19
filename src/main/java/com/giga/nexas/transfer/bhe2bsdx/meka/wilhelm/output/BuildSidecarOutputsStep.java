package com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm.output;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BuildSidecarOutputsStep {

    
    private static final String CHARSET = "windows-31j";

    
    private final BsdxBinService bsdxBinService;

    public BuildSidecarOutputsStep() {
        this(new BsdxBinService());
    }

    public BuildSidecarOutputsStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public OutputManifest build(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            SidecarResourceSpec spec,
            ResourceOverwriteAudit overwriteAudit
    ) {
        if (outputRoot == null) {
            throw new IllegalArgumentException("sidecar 输出目录不能为空");
        }
        SidecarResourceSpec sidecarSpec = spec == null ? SidecarResourceSpec.defaultWilhelmSidecars() : spec;
        OutputManifest manifest = new OutputManifest();
        manifest.setOutputRoot(outputRoot.toAbsolutePath().normalize().toString());

        try {
            Files.createDirectories(outputRoot);
            if (sidecarSpec.isIncludeWeaponEquipDat()) {
                writeWeaponEquipDat(tsukuyomiPackage, bsdxBaseline, outputRoot, sidecarSpec.getWeaponEquipFileName(), manifest, overwriteAudit);
            }
            recordExternalProgramMaterial(request, sidecarSpec, manifest);
            return manifest;
        } catch (IOException e) {
            throw new IllegalStateException("构建 sidecar 输出失败", e);
        }
    }

    public Dat buildPatchedWeaponEquipDat(TsukuyomiPackageBundle tsukuyomiPackage, TsukuyomiBsdxBaselineBundle bsdxBaseline) {
        if (bsdxBaseline == null) {
            return null;
        }
        Dat baseline = bsdxBaseline.getWeaponEquipDat();
        if (baseline == null || baseline.getData() == null) {
            return null;
        }

        Dat patched = new Dat();
        patched.setFileName(baseline.getFileName());
        patched.setExtensionName(baseline.getExtensionName());
        patched.setColumnCount(baseline.getColumnCount());
        patched.setColumnTypes(baseline.getColumnTypes() == null ? new ArrayList<>() : new ArrayList<>(baseline.getColumnTypes()));

        for (List<Object> row : baseline.getData()) {
            patched.addRow(copyDatRow(row));
        }
        patched.addRow(buildTsukuyomiWeaponEquipRow(baseline));
        return patched;
    }

    private List<Object> buildTsukuyomiWeaponEquipRow(Dat baseline) {
        int width = 0;
        if (baseline != null && baseline.getColumnTypes() != null && !baseline.getColumnTypes().isEmpty()) {
            width = baseline.getColumnTypes().size();
        }
        if (width <= 0 && baseline != null) {
            width = baseline.getColumnCount();
        }
        if (width <= 0) {
            throw new IllegalStateException("无法确定 WeaponEquip.dat 的列数");
        }

        List<Object> row = new ArrayList<>(width);
        for (int i = 0; i < width; i++) {
            row.add(i == 0 ? 0 : -1);
        }
        return row;
    }

    private void writeWeaponEquipDat(
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            String fileName,
            OutputManifest manifest,
            ResourceOverwriteAudit overwriteAudit
    ) throws IOException {
        Dat patchedWeaponEquip = buildPatchedWeaponEquipDat(tsukuyomiPackage, bsdxBaseline);
        if (patchedWeaponEquip == null) {
            manifest.addNote("WeaponEquip.dat sidecar skipped: source or baseline missing");
            return;
        }

        String outputFileName = fileName == null || fileName.isBlank() ? "WeaponEquip.dat" : fileName;
        Path output = outputRoot.resolve(outputFileName);
        bsdxBinService.generate(output.toString(), patchedWeaponEquip, CHARSET);
        manifest.addEntry(OutputManifestSupport.entryForFile(
                outputRoot,
                output,
                "dat",
                "sidecar: patched WeaponEquip.dat",
                overwriteAudit
        ));
    }

    private void recordExternalProgramMaterial(
            TsukuyomiGraftRequest request,
            SidecarResourceSpec sidecarSpec,
            OutputManifest manifest
    ) {
        String fileName = sidecarSpec.getExternalProgramMaterialFileName();
        if (fileName == null || fileName.isBlank()) {
            return;
        }
        Path root = request == null ? null : request.getExternalStaticAssetRoot();
        Path source = root == null ? null : root.resolve(fileName);
        if (source != null && Files.exists(source)) {
            manifest.addNote("external sidecar input available: " + source.toAbsolutePath().normalize());
        } else {
            manifest.addNote("external sidecar input missing: " + fileName);
        }
    }

    private List<Object> copyDatRow(List<Object> row) {
        return row == null ? new ArrayList<>() : new ArrayList<>(row);
    }
}
