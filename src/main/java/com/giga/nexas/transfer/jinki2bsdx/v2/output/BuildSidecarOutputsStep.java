package com.giga.nexas.transfer.jinki2bsdx.v2.output;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class BuildSidecarOutputsStep {

    /**
     * sidecar DAT 生成时使用的字符集。
     *
     * <p>`WeaponEquip.dat` 中可能包含日文/全角文本，必须和 BSDX DAT 生成流程保持一致。</p>
     */
    private static final String CHARSET = "windows-31j";

    /**
     * DAT 生成服务。
     *
     * <p>当前只用于写出 patch 后的 `WeaponEquip.dat` sidecar；
     * 其他文件不要在这里临时复制，必须先进入 SidecarResourceSpec 建模。</p>
     */
    private final BsdxBinService bsdxBinService;

    public BuildSidecarOutputsStep() {
        this(new BsdxBinService());
    }

    public BuildSidecarOutputsStep(BsdxBinService bsdxBinService) {
        this.bsdxBinService = bsdxBinService == null ? new BsdxBinService() : bsdxBinService;
    }

    public OutputManifest build(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            SidecarResourceSpec spec,
            ResourceOverwriteAudit overwriteAudit
    ) {
        if (outputRoot == null) {
            throw new IllegalArgumentException("sidecar 输出目录不能为空");
        }
        SidecarResourceSpec sidecarSpec = spec == null ? SidecarResourceSpec.defaultJinkiSidecars() : spec;
        OutputManifest manifest = new OutputManifest();
        manifest.setOutputRoot(outputRoot.toAbsolutePath().normalize().toString());

        try {
            Files.createDirectories(outputRoot);
            if (sidecarSpec.isIncludeWeaponEquipDat()) {
                writeWeaponEquipDat(jinkiPackage, bsdxBaseline, outputRoot, sidecarSpec.getWeaponEquipFileName(), manifest, overwriteAudit);
            }
            recordExternalProgramMaterial(request, sidecarSpec, manifest);
            return manifest;
        } catch (IOException e) {
            throw new IllegalStateException("构建 sidecar 输出失败", e);
        }
    }

    public Dat buildPatchedWeaponEquipDat(JinkiPackageBundle jinkiPackage, BsdxBaselineBundle bsdxBaseline) {
        if (jinkiPackage == null || bsdxBaseline == null) {
            return null;
        }
        Dat source = jinkiPackage.getWeaponEquipDat();
        Dat baseline = bsdxBaseline.getWeaponEquipDat();
        if (source == null || baseline == null || source.getData() == null || baseline.getData() == null) {
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
        for (int i = baseline.getData().size(); i < source.getData().size(); i++) {
            patched.addRow(copyDatRow(source.getData().get(i)));
        }
        return patched;
    }

    private void writeWeaponEquipDat(
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            String fileName,
            OutputManifest manifest,
            ResourceOverwriteAudit overwriteAudit
    ) throws IOException {
        Dat patchedWeaponEquip = buildPatchedWeaponEquipDat(jinkiPackage, bsdxBaseline);
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
            AkaoGraftRequest request,
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
