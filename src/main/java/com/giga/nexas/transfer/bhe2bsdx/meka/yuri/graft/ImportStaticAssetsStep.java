package com.giga.nexas.transfer.bhe2bsdx.meka.yuri.graft;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventSprite;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.model.BheCommonProjectileAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.projectile.output.OutputBheCommonProjectileResourcesStep;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiBsdxBaselineBundle;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGrpAppendPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportedAssetSet;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiImportPlan;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiPackageBundle;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;


public class ImportStaticAssetsStep {

    
    private static final String CHARSET = "windows-31j";

    
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    
    private final BsdxBinService bsdxBinService = new BsdxBinService();

    
    private final RebindWazStep rebindWazStep = new RebindWazStep();
    private final PadBaselineMekMaterialStep padBaselineMekMaterialStep = new PadBaselineMekMaterialStep();
    private final OutputBheCommonProjectileResourcesStep outputCommonProjectileResourcesStep =
            new OutputBheCommonProjectileResourcesStep();

    public TsukuyomiImportedAssetSet importAssets(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiImportPlan importPlan,
            ProgramMaterialGrp syncedProgramMaterial,
            Mek reboundTsukuyomiMek,
            Waz reboundTsukuyomiWaz,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            BheCommonProjectileAppendPlan commonProjectileAppendPlan
    ) {
        TsukuyomiImportedAssetSet importedAssetSet = new TsukuyomiImportedAssetSet();
        if (request == null || tsukuyomiPackage == null || bsdxBaseline == null || importPlan == null) {
            return importedAssetSet;
        }

        importedAssetSet.getWazFiles().addAll(importPlan.getRequiredWazFiles());
        importedAssetSet.getSpmFiles().addAll(importPlan.getRequiredSpmFiles());
        importedAssetSet.getAuxiliaryFiles().addAll(importPlan.getRequiredMekFiles());

        try {
            Path outputRoot = request.getExeOutputDir().resolve("yuri_assets_" + LocalDateTime.now().format(TS));
            Files.createDirectories(outputRoot);
            importedAssetSet.setOutputRootDir(outputRoot);
            copyInheritedJinkiAssets(request, outputRoot, importedAssetSet);
            writePatchedGrpOutputs(bsdxBaseline, syncedProgramMaterial, outputRoot, importedAssetSet);
            writePatchedConfigDatOutputs(tsukuyomiPackage, bsdxBaseline, outputRoot, importedAssetSet);
            outputCommonProjectileResourcesStep.output(
                    request,
                    bsdxBaseline,
                    commonProjectileAppendPlan,
                    outputRoot,
                    importedAssetSet
            );
            writeBaselineWazFiles(request, bsdxBaseline, outputRoot, importedAssetSet);
            writeBaselineSpmFiles(request, bsdxBaseline, outputRoot, importedAssetSet);

            String targetMekFileName = resolveTargetMekFileName(bsdxBaseline, grpAppendPlan, request.getMekFileName());
            writeReboundMek(targetMekFileName, reboundTsukuyomiMek, outputRoot, importedAssetSet);
            writePatchedBaselineMeks(bsdxBaseline, targetMekFileName, request.getMekFileName(), outputRoot, importedAssetSet);
            padExistingOutputMeks(outputRoot, bsdxBaseline, importedAssetSet);
            writeReboundWaz(request, reboundTsukuyomiWaz, outputRoot, importedAssetSet);
            writeRequiredAuxiliaryWazFiles(
                    request,
                    tsukuyomiPackage,
                    bsdxBaseline,
                    importPlan,
                    grpAppendPlan,
                    commonProjectileAppendPlan,
                    outputRoot,
                    importedAssetSet
            );
            writeRequiredSpmFiles(tsukuyomiPackage, importPlan, outputRoot, importedAssetSet);
            copyRequiredImageFiles(request, tsukuyomiPackage, bsdxBaseline, importPlan, grpAppendPlan, outputRoot, importedAssetSet);
            copyRequiredAudioAssets(request, tsukuyomiPackage, importPlan, reboundTsukuyomiMek, reboundTsukuyomiWaz, outputRoot, importedAssetSet);
            return importedAssetSet;
        } catch (IOException e) {
            throw new IllegalStateException("step8 静态资源落盘失败", e);
        }
    }

    private void writePatchedGrpOutputs(
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            ProgramMaterialGrp syncedProgramMaterial,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        writeGrp(outputRoot, "MekaGroup.grp", bsdxBaseline.getMekaGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "WazaGroup.grp", bsdxBaseline.getWazaGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "SpriteGroup.grp", bsdxBaseline.getSpriteGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "BatVoice.grp", bsdxBaseline.getBatVoiceGrp(), importedAssetSet);
        writeGrp(outputRoot, "MapGroup.grp", bsdxBaseline.getMapGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "SeGroup.grp", bsdxBaseline.getSeGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "ProgramMaterial.grp", syncedProgramMaterial != null ? syncedProgramMaterial : bsdxBaseline.getProgramMaterialGrp(), importedAssetSet);
    }

    private void copyInheritedJinkiAssets(
            TsukuyomiGraftRequest request,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request == null || request.getJinkiGeneratedAssetDir() == null) {
            return;
        }
        Path inheritedAssetDir = Path.of("").toAbsolutePath().normalize()
                .resolve(request.getJinkiGeneratedAssetDir())
                .normalize();
        if (!Files.exists(inheritedAssetDir)) {
            return;
        }

        try (Stream<Path> stream = Files.walk(inheritedAssetDir)) {
            for (Path source : stream.filter(Files::isRegularFile).toList()) {
                String fileName = source.getFileName().toString();
                if (fileName.endsWith(".pac") || fileName.endsWith(".exe")) {
                    continue;
                }
                Path output = outputRoot.resolve(fileName);
                Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
                registerInheritedCopiedAsset(importedAssetSet, output);
            }
        }
    }

    private void registerInheritedCopiedAsset(TsukuyomiImportedAssetSet importedAssetSet, Path output) {
        String lower = output.getFileName().toString().toLowerCase(Locale.ROOT);
        if (lower.endsWith(".png")) {
            importedAssetSet.getCopiedImageFiles().add(output);
        } else if (lower.endsWith(".ogg") || lower.endsWith(".wav")) {
            importedAssetSet.getCopiedAudioFiles().add(output);
        } else if (lower.endsWith(".spm")) {
            importedAssetSet.getCopiedSpmFiles().add(output);
        } else if (lower.endsWith(".mek")) {
            importedAssetSet.getGeneratedMekFiles().add(output);
        } else if (lower.endsWith(".waz")) {
            importedAssetSet.getGeneratedWazFiles().add(output);
        } else if (lower.endsWith(".dat")) {
            importedAssetSet.getGeneratedDatFiles().add(output);
        } else if (lower.endsWith(".grp")) {
            importedAssetSet.getGeneratedGrpFiles().add(output);
        }
    }

    private void writeGrp(Path outputRoot, String fileName, Object grp, TsukuyomiImportedAssetSet importedAssetSet) throws IOException {
        if (grp == null) {
            importedAssetSet.getMissingAssets().add("缺少 grp 产物: " + fileName);
            return;
        }
        Path output = outputRoot.resolve(fileName);
        bsdxBinService.generate(output.toString(), (com.giga.nexas.dto.bsdx.Bsdx) grp, CHARSET);
        importedAssetSet.getGeneratedGrpFiles().add(output);
    }

    private void writePatchedConfigDatOutputs(
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        Dat patchedWeaponEquip = buildPatchedWeaponEquipDat(tsukuyomiPackage, bsdxBaseline);
        if (patchedWeaponEquip == null) {
            return;
        }

        Path rootOutput = outputRoot.resolve("WeaponEquip.dat");
        bsdxBinService.generate(rootOutput.toString(), patchedWeaponEquip, CHARSET);
        importedAssetSet.getGeneratedDatFiles().add(rootOutput);
    }

    private Dat buildPatchedWeaponEquipDat(TsukuyomiPackageBundle tsukuyomiPackage, TsukuyomiBsdxBaselineBundle bsdxBaseline) {
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
        patched.setColumnTypes(new ArrayList<>(baseline.getColumnTypes()));

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

    private List<Object> copyDatRow(List<Object> row) {
        return row == null ? new ArrayList<>() : new ArrayList<>(row);
    }

    private String resolveTargetMekFileName(
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            String fallbackFileName
    ) {
        if (bsdxBaseline != null
                && bsdxBaseline.getMekaGroupGrp() != null
                && bsdxBaseline.getMekaGroupGrp().getMekaList() != null
                && grpAppendPlan != null
                && grpAppendPlan.getMekaGroupIndex() >= 0
                && grpAppendPlan.getMekaGroupIndex() < bsdxBaseline.getMekaGroupGrp().getMekaList().size()) {
            MekaGroupGrp.MekaGroup targetGroup =
                    bsdxBaseline.getMekaGroupGrp().getMekaList().get(grpAppendPlan.getMekaGroupIndex());
            if (targetGroup != null && targetGroup.getMekaName() != null && !targetGroup.getMekaName().isBlank()) {
                return targetGroup.getMekaName() + ".mek";
            }
        }
        return fallbackFileName;
    }

    private void writeReboundMek(
            String targetMekFileName,
            Mek reboundTsukuyomiMek,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundTsukuyomiMek == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 mek");
            return;
        }

        Path output = outputRoot.resolve(targetMekFileName);
        bsdxBinService.generate(output.toString(), reboundTsukuyomiMek, CHARSET);
        importedAssetSet.getGeneratedMekFiles().add(output);
    }

    
    private void writePatchedBaselineMeks(
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            String targetMekFileName,
            String sourceMekFileName,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (bsdxBaseline == null || bsdxBaseline.getMekByFileName() == null) {
            return;
        }

        for (Map.Entry<String, Mek> entry : bsdxBaseline.getMekByFileName().entrySet()) {
            String fileName = entry.getKey();
            Mek mek = entry.getValue();

            if (mek == null) {
                continue;
            }
            if (normalize(fileName).equals(normalize(targetMekFileName))
                    || normalize(fileName).equals(normalize(sourceMekFileName))) {
                continue;
            }

            Path output = outputRoot.resolve(fileName);
            bsdxBinService.generate(output.toString(), mek, CHARSET);
            importedAssetSet.getGeneratedMekFiles().add(output);
        }
    }

    private void padExistingOutputMeks(
            Path outputRoot,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (outputRoot == null || bsdxBaseline == null || !Files.exists(outputRoot)) {
            return;
        }

        try (Stream<Path> stream = Files.list(outputRoot)) {
            for (Path mekPath : stream
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(".mek"))
                    .toList()) {
                Mek mek = (Mek) bsdxBinService.parse(mekPath.toString(), CHARSET).getData();
                padBaselineMekMaterialStep.padMaterialBlock(mek, bsdxBaseline);
                bsdxBinService.generate(mekPath.toString(), mek, CHARSET);
                if (!importedAssetSet.getGeneratedMekFiles().contains(mekPath)) {
                    importedAssetSet.getGeneratedMekFiles().add(mekPath);
                }
            }
        }
    }

    private void writeReboundWaz(
            TsukuyomiGraftRequest request,
            Waz reboundTsukuyomiWaz,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundTsukuyomiWaz == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 waz");
            return;
        }

        Path output = outputRoot.resolve(request.getWazFileName());
        bsdxBinService.generate(output.toString(), reboundTsukuyomiWaz, CHARSET);
        importedAssetSet.getGeneratedWazFiles().add(output);
    }

    private void writeBaselineWazFiles(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (bsdxBaseline == null || bsdxBaseline.getWazByFileName() == null) {
            return;
        }

        for (Map.Entry<String, Waz> entry : bsdxBaseline.getWazByFileName().entrySet()) {
            String fileName = entry.getKey();
            Waz waz = entry.getValue();
            if (fileName == null || fileName.isBlank() || waz == null) {
                continue;
            }
            if (normalize(fileName).equals(normalize(request == null ? null : request.getWazFileName()))) {
                continue;
            }
            Path output = outputRoot.resolve(fileName);
            if (Files.exists(output)) {
                continue;
            }

            bsdxBinService.generate(output.toString(), waz, CHARSET);
            importedAssetSet.getGeneratedWazFiles().add(output);
        }
    }

    private void writeBaselineSpmFiles(
            TsukuyomiGraftRequest request,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (bsdxBaseline == null || bsdxBaseline.getSpmByFileName() == null) {
            return;
        }

        for (Map.Entry<String, Spm> entry : bsdxBaseline.getSpmByFileName().entrySet()) {
            String fileName = entry.getKey();
            Spm spm = entry.getValue();
            if (fileName == null || fileName.isBlank() || spm == null) {
                continue;
            }
            Path output = outputRoot.resolve(fileName);
            if (Files.exists(output)) {
                continue;
            }

            spm.setExtensionName("spm");
            bsdxBinService.generate(output.toString(), spm, CHARSET);
            importedAssetSet.getCopiedSpmFiles().add(output);
        }
    }

    private void writeRequiredAuxiliaryWazFiles(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiImportPlan importPlan,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            BheCommonProjectileAppendPlan commonProjectileAppendPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : importPlan.getRequiredWazFiles()) {
            if (normalize(fileName).equals(normalize(request.getWazFileName()))) {
                continue;
            }

            Waz sourceWaz = findWazByFileName(tsukuyomiPackage.getWazByFileName(), fileName);
            if (sourceWaz == null) {
                importedAssetSet.getMissingAssets().add("缺少辅助 waz: " + fileName);
                continue;
            }

            Integer sourceWazGroupIndex = findSourceWazGroupIndex(importPlan, fileName);
            Waz outputWaz = rebindWazStep.rebindAuxiliaryWaz(
                    request,
                    sourceWaz,
                    importPlan,
                    grpAppendPlan,
                    sourceWazGroupIndex,
                    commonProjectileAppendPlan
            );

            Path output = outputRoot.resolve(fileName);
            bsdxBinService.generate(output.toString(), outputWaz, CHARSET);
            importedAssetSet.getGeneratedWazFiles().add(output);
        }
    }

    private Integer findSourceWazGroupIndex(TsukuyomiImportPlan importPlan, String fileName) {
        if (importPlan == null || importPlan.getSourceWazIndexByFileName() == null) {
            return null;
        }
        for (Map.Entry<String, Integer> entry : importPlan.getSourceWazIndexByFileName().entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private void writeRequiredSpmFiles(
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiImportPlan importPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : importPlan.getRequiredSpmFiles()) {
            Spm spm = findSpm(tsukuyomiPackage.getSpmByFileName(), fileName);
            if (spm == null) {
                importedAssetSet.getMissingAssets().add("缺少转换后的 spm 产物: " + fileName);
                continue;
            }
            spm.setExtensionName("spm");
            Path output = outputRoot.resolve(fileName);
            bsdxBinService.generate(output.toString(), spm, CHARSET);
            importedAssetSet.getCopiedSpmFiles().add(output);
        }
    }

    private void copyRequiredImageFiles(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiImportPlan importPlan,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在: " + request.getExternalStaticAssetRoot());
            return;
        }

        Set<String> imageNames = collectRequiredImageNames(tsukuyomiPackage, importPlan);
        imageNames.addAll(collectGraftedSkillImageNames(tsukuyomiPackage, bsdxBaseline, importPlan, grpAppendPlan, outputRoot));
        if (imageNames.isEmpty()) {
            return;
        }

        for (String imageName : imageNames) {
            if (Files.exists(outputRoot.resolve(imageName))) {
                continue;
            }
            Path source = resolveExternalFileCaseInsensitive(request.getExternalStaticAssetRoot(), imageName);
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少图像资源: " + imageName);
                continue;
            }

            Path output = outputRoot.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedImageFiles().add(output);
        }
    }

    private Set<String> collectRequiredImageNames(TsukuyomiPackageBundle tsukuyomiPackage, TsukuyomiImportPlan importPlan) {
        Set<String> imageNames = new LinkedHashSet<>();
        Map<String, Spm> spmByFileName = tsukuyomiPackage.getSpmByFileName();
        if (spmByFileName == null) {
            return imageNames;
        }

        for (String fileName : importPlan.getRequiredSpmFiles()) {
            Spm spm = findSpm(spmByFileName, fileName);
            if (spm == null || spm.getImageData() == null) {
                continue;
            }

            for (Spm.SPMImageData imageData : spm.getImageData()) {
                if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                    continue;
                }
                imageNames.add(imageData.getImageName());
            }
        }

        return imageNames;
    }

    private Set<String> collectGraftedSkillImageNames(
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiImportPlan importPlan,
            TsukuyomiGrpAppendPlan grpAppendPlan,
            Path outputRoot
    ) throws IOException {
        Set<String> imageNames = new LinkedHashSet<>();
        if (tsukuyomiPackage == null || bsdxBaseline == null || importPlan == null || grpAppendPlan == null || outputRoot == null) {
            return imageNames;
        }

        for (String fileName : importPlan.getRequiredWazFiles()) {
            Integer sourceWazGroupIndex = findSourceWazGroupIndex(importPlan, fileName);
            if (sourceWazGroupIndex == null) {
                continue;
            }

            Map<Integer, Integer> skillIndexMap = grpAppendPlan.getSourceWazSkillIndexToTargetIndexByGroup().get(sourceWazGroupIndex);
            if (skillIndexMap == null || skillIndexMap.isEmpty()) {
                continue;
            }

            Waz sourceWaz = findWazByFileName(tsukuyomiPackage.getWazByFileName(), fileName);
            Waz outputWaz = parseOutputWaz(outputRoot, fileName);
            if (sourceWaz == null || outputWaz == null || sourceWaz.getSkillList() == null || outputWaz.getSkillList() == null) {
                continue;
            }
            for (int sourceSkillIndex = 0; sourceSkillIndex < sourceWaz.getSkillList().size(); sourceSkillIndex++) {
                Integer targetSkillIndex = skillIndexMap.get(sourceSkillIndex);
                if (targetSkillIndex == null || targetSkillIndex < 0 || targetSkillIndex >= outputWaz.getSkillList().size()) {
                    continue;
                }

                collectImageNamesFromReachableSkill(
                        outputWaz,
                        targetSkillIndex,
                        bsdxBaseline,
                        tsukuyomiPackage,
                        outputRoot,
                        imageNames,
                        new LinkedHashSet<>()
                );
            }
        }

        return imageNames;
    }

    private Waz parseOutputWaz(Path outputRoot, String fileName) throws IOException {
        Path output = outputRoot.resolve(fileName);
        if (!Files.exists(output)) {
            output = resolveFileCaseInsensitive(outputRoot, fileName);
        }
        if (output == null || !Files.exists(output)) {
            return null;
        }
        return (Waz) bsdxBinService.parse(output.toString(), CHARSET).getData();
    }

    private void collectImageNamesFromReachableSkill(
            Waz waz,
            int skillIndex,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (waz == null || waz.getSkillList() == null || skillIndex < 0 || skillIndex >= waz.getSkillList().size()) {
            return;
        }
        String visitKey = normalize(waz.getFileName()) + "#" + skillIndex;
        if (!visited.add(visitKey)) {
            return;
        }
        collectImageNamesFromSkill(waz.getSkillList().get(skillIndex), bsdxBaseline, tsukuyomiPackage, outputRoot, imageNames, visited);
    }

    private void collectImageNamesFromSkill(
            Waz.Skill skill,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (skill == null || skill.getPhasesInfo() == null) {
            return;
        }

        for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
            if (phase == null || phase.getSkillUnitCollection() == null) {
                continue;
            }
            for (SkillUnit unit : phase.getSkillUnitCollection()) {
                if (unit == null || unit.getSkillInfoObjectList() == null) {
                    continue;
                }
                for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                    collectImageNamesFromObject(object, bsdxBaseline, tsukuyomiPackage, outputRoot, imageNames, visited);
                }
            }
        }
    }

    private void collectImageNamesFromObject(
            SkillInfoObject object,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (object == null) {
            return;
        }

        if (object instanceof CEventSprite sprite) {
            collectImageNamesFromSpriteAction(sprite, bsdxBaseline, tsukuyomiPackage, outputRoot, imageNames);
        }

        if (object instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect select) {
            collectImageNamesFromWazRef(select, bsdxBaseline, tsukuyomiPackage, outputRoot, imageNames, visited);
        }

        for (Field field : getAllFields(object.getClass())) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            if (!List.class.isAssignableFrom(field.getType()) || !field.getName().endsWith("UnitList")) {
                continue;
            }
            field.setAccessible(true);
            try {
                Object units = field.get(object);
                if (!(units instanceof List<?> unitList)) {
                    continue;
                }
                for (Object unit : unitList) {
                    SkillInfoObject data = tryGetUnitData(unit);
                    if (data != null) {
                        collectImageNamesFromObject(data, bsdxBaseline, tsukuyomiPackage, outputRoot, imageNames, visited);
                    }
                }
            } catch (ReflectiveOperationException e) {
                throw new IllegalStateException("鏀堕泦 WAZ 鍐呭祵 sprite 鍥惧儚澶辫触: " + field.getName(), e);
            }
        }
    }

    private void collectImageNamesFromWazRef(
            com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventWazaSelect select,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Path outputRoot,
            Set<String> imageNames,
            Set<String> visited
    ) throws IOException {
        if (select.getWazFileNo() == null || select.getWazFileNo() < 0 || select.getWazSequenceNo() == null || select.getWazSequenceNo() < 0
                || bsdxBaseline.getWazaGroupGrp() == null || bsdxBaseline.getWazaGroupGrp().getWazaList() == null
                || select.getWazFileNo() >= bsdxBaseline.getWazaGroupGrp().getWazaList().size()) {
            return;
        }
        var entry = bsdxBaseline.getWazaGroupGrp().getWazaList().get(select.getWazFileNo());
        if (entry == null || entry.getWazaDisplayName() == null || entry.getWazaDisplayName().isBlank()) {
            return;
        }
        Waz targetWaz = parseOutputWaz(outputRoot, entry.getWazaDisplayName() + ".waz");
        if (targetWaz == null) {
            targetWaz = findWazByFileName(bsdxBaseline.getWazByFileName(), entry.getWazaDisplayName() + ".waz");
        }
        collectImageNamesFromReachableSkill(targetWaz, select.getWazSequenceNo(), bsdxBaseline, tsukuyomiPackage, outputRoot, imageNames, visited);
    }

    private void collectImageNamesFromSpriteAction(
            CEventSprite sprite,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Path outputRoot,
            Set<String> imageNames
    ) throws IOException {
        Integer spriteGroupIndex = sprite.getSpmFileSequence();
        if (spriteGroupIndex == null || spriteGroupIndex < 0 || bsdxBaseline.getSpriteGroupGrp() == null
                || bsdxBaseline.getSpriteGroupGrp().getSpriteList() == null
                || spriteGroupIndex >= bsdxBaseline.getSpriteGroupGrp().getSpriteList().size()) {
            return;
        }

        SpriteGroupGrp.SpriteGroupEntry spriteEntry = bsdxBaseline.getSpriteGroupGrp().getSpriteList().get(spriteGroupIndex);
        if (spriteEntry == null || spriteEntry.getSpriteFileName() == null || spriteEntry.getSpriteFileName().isBlank()) {
            return;
        }

        Spm spm = resolveSpmForImageCollection(spriteEntry.getSpriteFileName(), bsdxBaseline, tsukuyomiPackage, outputRoot);
        if (spm == null || spm.getImageData() == null || spm.getAnimData() == null
                || sprite.getActionGroupNumber() == null || sprite.getActionGroupNumber() < 0
                || sprite.getActionGroupNumber() >= spm.getAnimData().size()) {
            return;
        }

        Spm.SPMAnimData animData = spm.getAnimData().get(sprite.getActionGroupNumber());
        if (animData == null || animData.getPatData() == null) {
            return;
        }
        for (Spm.SPMPatData patData : animData.getPatData()) {
            if (patData == null || patData.getPageNo() == null) {
                continue;
            }
            for (Integer pageNo : patData.getPageNo()) {
                collectImageNamesFromPage(spm, pageNo, imageNames);
            }
        }
    }

    private void collectImageNamesFromPage(Spm spm, Integer pageNo, Set<String> imageNames) {
        if (spm == null || spm.getPageData() == null || spm.getImageData() == null
                || pageNo == null || pageNo < 0 || pageNo >= spm.getPageData().size()) {
            return;
        }
        Spm.SPMPageData pageData = spm.getPageData().get(pageNo);
        if (pageData == null || pageData.getChipData() == null) {
            return;
        }
        for (Spm.SPMChipData chipData : pageData.getChipData()) {
            if (chipData == null || chipData.getImageNo() == null
                    || chipData.getImageNo() < 0 || chipData.getImageNo() >= spm.getImageData().size()) {
                continue;
            }
            Spm.SPMImageData imageData = spm.getImageData().get(chipData.getImageNo());
            if (imageData == null || imageData.getImageName() == null || imageData.getImageName().isBlank()) {
                continue;
            }
            imageNames.add(imageData.getImageName());
        }
    }

    private Spm resolveSpmForImageCollection(
            String fileName,
            TsukuyomiBsdxBaselineBundle bsdxBaseline,
            TsukuyomiPackageBundle tsukuyomiPackage,
            Path outputRoot
    ) throws IOException {
        Path outputSpm = resolveFileCaseInsensitive(outputRoot, fileName);
        if (outputSpm != null && Files.exists(outputSpm)) {
            return (Spm) bsdxBinService.parse(outputSpm.toString(), CHARSET).getData();
        }

        Spm spm = findSpm(tsukuyomiPackage.getSpmByFileName(), fileName);
        if (spm != null) {
            return spm;
        }
        return findSpm(bsdxBaseline.getSpmByFileName(), fileName);
    }

    private SkillInfoObject tryGetUnitData(Object unit) {
        if (unit == null) {
            return null;
        }
        try {
            Method getter = unit.getClass().getMethod("getData");
            Object value = getter.invoke(unit);
            return value instanceof SkillInfoObject object ? object : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private List<Field> getAllFields(Class<?> type) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = type;
        while (current != null && current != Object.class) {
            fields.addAll(List.of(current.getDeclaredFields()));
            current = current.getSuperclass();
        }
        return fields;
    }

    private void copyRequiredAudioAssets(
            TsukuyomiGraftRequest request,
            TsukuyomiPackageBundle tsukuyomiPackage,
            TsukuyomiImportPlan importPlan,
            Mek reboundTsukuyomiMek,
            Waz reboundTsukuyomiWaz,
            Path outputRoot,
            TsukuyomiImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在: " + request.getExternalStaticAssetRoot());
            return;
        }

        Set<String> requiredBaseNames = new LinkedHashSet<>();
        requiredBaseNames.addAll(collectRequiredSeBaseNames(tsukuyomiPackage, importPlan));
        requiredBaseNames.addAll(collectRequiredVoiceBaseNames(tsukuyomiPackage, reboundTsukuyomiMek, reboundTsukuyomiWaz, request.getMekaCodeName()));

        if (requiredBaseNames.isEmpty()) {
            return;
        }

        Map<String, Path> externalIndex = buildPreferredExternalAudioIndex(request.getExternalStaticAssetRoot());
        for (String baseName : requiredBaseNames) {
            Path source = externalIndex.get(normalizeBaseName(baseName));
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少音频资源: " + baseName);
                continue;
            }

            Path output = outputRoot.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedAudioFiles().add(output);
        }
    }

    private Set<String> collectRequiredSeBaseNames(TsukuyomiPackageBundle tsukuyomiPackage, TsukuyomiImportPlan importPlan) {
        Set<String> baseNames = new LinkedHashSet<>();
        SeGroupGrp seGroupGrp = tsukuyomiPackage.getSeGroupGrp();
        if (seGroupGrp == null) {
            return baseNames;
        }

        for (Map.Entry<Integer, List<Integer>> entry : importPlan.getReferencedSourceSeItemIndicesByGroupIndex().entrySet()) {
            Integer sourceGroupIndex = entry.getKey();
            if (sourceGroupIndex == null || sourceGroupIndex < 0 || sourceGroupIndex >= seGroupGrp.getSeList().size()) {
                continue;
            }

            SeGroupGrp.SeGroupGroup sourceGroup = seGroupGrp.getSeList().get(sourceGroupIndex);
            if (sourceGroup == null || sourceGroup.getSeItems() == null) {
                continue;
            }

            for (Integer sourceItemIndex : entry.getValue()) {
                if (sourceItemIndex == null || sourceItemIndex < 0 || sourceItemIndex >= sourceGroup.getSeItems().size()) {
                    continue;
                }
                SeGroupGrp.SeGroupItem item = sourceGroup.getSeItems().get(sourceItemIndex);
                if (item == null || !isExisting(item.getExistFlag()) || item.getSeFileName() == null || item.getSeFileName().isBlank()) {
                    continue;
                }
                baseNames.add(item.getSeFileName());
            }
        }
        return baseNames;
    }

    private Set<String> collectRequiredVoiceBaseNames(
            TsukuyomiPackageBundle tsukuyomiPackage,
            Mek reboundTsukuyomiMek,
            Waz reboundTsukuyomiWaz,
            String codeName
    ) {
        Set<String> baseNames = new LinkedHashSet<>();
        if (reboundTsukuyomiMek == null && reboundTsukuyomiWaz == null) {
            return baseNames;
        }

        BatVoiceGrp.BatVoiceGroup tsukuyomiVoiceGroup = findTsukuyomiVoiceGroup(tsukuyomiPackage.getBatVoiceGrp(), codeName);
        if (tsukuyomiVoiceGroup == null || tsukuyomiVoiceGroup.getVoices() == null) {
            return baseNames;
        }
        Set<Integer> usedVoiceIndices = new LinkedHashSet<>(collectUsedVoiceIndices(reboundTsukuyomiWaz));
        usedVoiceIndices.addAll(collectUsedMekVoiceIndices(reboundTsukuyomiMek));
        for (Integer index : usedVoiceIndices) {
            if (index == null || index < 0 || index >= tsukuyomiVoiceGroup.getVoices().size()) {
                continue;
            }
            BatVoiceGrp.BatVoice voice = tsukuyomiVoiceGroup.getVoices().get(index);
            if (voice == null || !isExisting(voice.getExistFlag()) || voice.getVoiceFileName() == null || voice.getVoiceFileName().isBlank()) {
                continue;
            }
            baseNames.add(voice.getVoiceFileName());
        }

        return baseNames;
    }

    private Set<Integer> collectUsedMekVoiceIndices(Mek mek) {
        Set<Integer> indices = new LinkedHashSet<>();
        if (mek == null || mek.getMekVoiceInfo() == null || mek.getMekVoiceInfo().getTable() == null) {
            return indices;
        }

        for (List<List<Mek.MekVoiceInfo.Entry>> row : mek.getMekVoiceInfo().getTable()) {
            if (row == null) {
                continue;
            }
            for (List<Mek.MekVoiceInfo.Entry> cell : row) {
                if (cell == null) {
                    continue;
                }
                for (Mek.MekVoiceInfo.Entry entry : cell) {
                    if (entry == null || entry.getGroupId() == null || entry.getGroupId() < 0) {
                        continue;
                    }
                    indices.add(entry.getGroupId());
                }
            }
        }

        return indices;
    }

    private List<Integer> collectUsedVoiceIndices(Waz waz) {
        List<Integer> indices = new ArrayList<>();
        if (waz.getSkillList() == null) {
            return indices;
        }

        for (Waz.Skill skill : waz.getSkillList()) {
            if (skill == null || skill.getPhasesInfo() == null) {
                continue;
            }
            for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                if (phase == null || phase.getSkillUnitCollection() == null) {
                    continue;
                }
                for (com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (Object object : unit.getSkillInfoObjectList()) {
                        collectUsedVoiceIndicesFromObject(object, indices);
                    }
                }
            }
        }

        return indices;
    }

    private void collectUsedVoiceIndicesFromObject(Object object, List<Integer> indices) {
        if (object == null) {
            return;
        }

        if (object instanceof com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice voice) {
            if (voice.getByteDataList() == null) {
                return;
            }
            for (byte[] bytes : voice.getByteDataList()) {
                if (bytes == null || bytes.length < 8) {
                    continue;
                }
                indices.add(readLittleEndianInt(bytes, 4));
            }
        }
    }

    private BatVoiceGrp.BatVoiceGroup findTsukuyomiVoiceGroup(BatVoiceGrp batVoiceGrp, String codeName) {
        if (batVoiceGrp == null || batVoiceGrp.getVoiceList() == null) {
            return null;
        }

        for (BatVoiceGrp.BatVoiceGroup voiceGroup : batVoiceGrp.getVoiceList()) {
            if (voiceGroup == null || !isExisting(voiceGroup.getExistFlag())) {
                continue;
            }
            if (voiceGroup.getCharacterCodeName() != null
                    && voiceGroup.getCharacterCodeName().trim().equalsIgnoreCase(codeName)) {
                return voiceGroup;
            }
        }
        return null;
    }

    private Spm findSpm(Map<String, Spm> spmByFileName, String fileName) {
        for (Map.Entry<String, Spm> entry : spmByFileName.entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Waz findWazByFileName(Map<String, Waz> wazByFileName, String fileName) {
        if (wazByFileName == null || fileName == null) {
            return null;
        }
        for (Map.Entry<String, Waz> entry : wazByFileName.entrySet()) {
            if (normalize(entry.getKey()).equals(normalize(fileName))) {
                return entry.getValue();
            }
        }
        return null;
    }

    private Map<String, Path> buildPreferredExternalAudioIndex(Path root) throws IOException {
        Map<String, Path> index = new HashMap<>();
        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String ext = extension(path.getFileName().toString());
                if (!ext.equals(".ogg") && !ext.equals(".wav")) {
                    return;
                }

                String baseName = normalizeBaseName(path.getFileName().toString());
                Path existing = index.get(baseName);
                if (existing == null || preferAudio(path, existing)) {
                    index.put(baseName, path);
                }
            });
        }
        return index;
    }

    private boolean preferAudio(Path candidate, Path existing) {
        String candidateExt = extension(candidate.getFileName().toString());
        String existingExt = extension(existing.getFileName().toString());
        if (candidateExt.equals(existingExt)) {
            return false;
        }
        return candidateExt.equals(".ogg");
    }

    private Path resolveExternalFileCaseInsensitive(Path root, String fileName) throws IOException {
        try (Stream<Path> stream = Files.walk(root)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> normalize(path.getFileName().toString()).equals(normalize(fileName)))
                    .findFirst()
                    .orElse(null);
        }
    }

    private Path resolveFileCaseInsensitive(Path dir, String fileName) throws IOException {
        if (dir == null || fileName == null || !Files.exists(dir)) {
            return null;
        }

        try (Stream<Path> stream = Files.list(dir)) {
            return stream
                    .filter(Files::isRegularFile)
                    .filter(path -> normalize(path.getFileName().toString()).equals(normalize(fileName)))
                    .findFirst()
                    .orElse(null);
        }
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }

    private String normalizeBaseName(String fileName) {
        String normalized = normalize(fileName);
        int dot = normalized.lastIndexOf('.');
        return dot >= 0 ? normalized.substring(0, dot) : normalized;
    }

    private String extension(String fileName) {
        String normalized = normalize(fileName);
        int dot = normalized.lastIndexOf('.');
        return dot >= 0 ? normalized.substring(dot) : "";
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isExisting(Integer existFlag) {
        return existFlag == null || existFlag != 0;
    }
}
