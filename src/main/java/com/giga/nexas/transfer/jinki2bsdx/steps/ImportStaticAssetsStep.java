package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.ProgramMaterialGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.spm.Spm;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.BsdxBaselineBundle;
import com.giga.nexas.transfer.jinki2bsdx.model.ImportedAssetSet;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiImportPlan;
import com.giga.nexas.transfer.jinki2bsdx.model.JinkiPackageBundle;

import java.io.IOException;
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

/**
 * 负责把 step6/7 产物和当前机体链上的静态资源落盘到输出根目录。
 *
 * <p>当前统一平铺写入：
 * `src/main/resources/out/jinki2bsdx_assets_<timestamp>`</p>
 */
public class ImportStaticAssetsStep {

    private static final String CHARSET = "windows-31j";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    public ImportedAssetSet importAssets(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            BsdxBaselineBundle bsdxBaseline,
            JinkiImportPlan importPlan,
            ProgramMaterialGrp syncedProgramMaterial,
            Mek reboundAkaoMek,
            Waz reboundAkaoWaz
    ) {
        ImportedAssetSet importedAssetSet = new ImportedAssetSet();
        if (request == null || jinkiPackage == null || bsdxBaseline == null || importPlan == null) {
            return importedAssetSet;
        }

        importedAssetSet.getWazFiles().addAll(importPlan.getRequiredWazFiles());
        importedAssetSet.getSpmFiles().addAll(importPlan.getRequiredSpmFiles());
        importedAssetSet.getAuxiliaryFiles().addAll(importPlan.getRequiredMekFiles());

        try {
            // Step 8-1: 创建本次迁移产物的输出根目录。
            Path outputRoot = request.getExeOutputDir().resolve("jinki2bsdx_assets_" + LocalDateTime.now().format(TS));
            Files.createDirectories(outputRoot);
            importedAssetSet.setOutputRootDir(outputRoot);

            // Step 8-2: 先把所有修改过的 grp 和 ProgramMaterial 真正写进产物。
            writePatchedGrpOutputs(bsdxBaseline, syncedProgramMaterial, outputRoot, importedAssetSet);

            // Step 8-3: 再把主 mek / waz 产物直接平铺写到根目录。
            writeReboundMek(request, reboundAkaoMek, outputRoot, importedAssetSet);
            writeReboundWaz(request, reboundAkaoWaz, outputRoot, importedAssetSet);

            // Step 8-4: 再把辅助 waz 平铺复制到根目录。
            copyRequiredWazFiles(request, importPlan, outputRoot, importedAssetSet);

            // Step 8-5: 把链上需要的 spm 平铺复制到根目录。
            copyRequiredSpmFiles(request, importPlan, outputRoot, importedAssetSet);

            // Step 8-6: 再按这些 spm 的 imageData，补齐真正用到的图像文件。
            copyRequiredImageFiles(request, jinkiPackage, importPlan, outputRoot, importedAssetSet);

            // Step 8-7: 最后只补当前机体链真实关联到的音频，不再整组打包。
            copyRequiredAudioAssets(request, jinkiPackage, importPlan, reboundAkaoWaz, outputRoot, importedAssetSet);
            return importedAssetSet;
        } catch (IOException e) {
            throw new IllegalStateException("step8 静态资源落盘失败", e);
        }
    }

    private void writePatchedGrpOutputs(
            BsdxBaselineBundle bsdxBaseline,
            ProgramMaterialGrp syncedProgramMaterial,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        writeGrp(outputRoot, "MekaGroup.grp", bsdxBaseline.getMekaGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "WazaGroup.grp", bsdxBaseline.getWazaGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "SpriteGroup.grp", bsdxBaseline.getSpriteGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "BatVoice.grp", bsdxBaseline.getBatVoiceGrp(), importedAssetSet);
        writeGrp(outputRoot, "SeGroup.grp", bsdxBaseline.getSeGroupGrp(), importedAssetSet);
        writeGrp(outputRoot, "ProgramMaterial.grp", syncedProgramMaterial != null ? syncedProgramMaterial : bsdxBaseline.getProgramMaterialGrp(), importedAssetSet);
    }

    private void writeGrp(Path outputRoot, String fileName, Object grp, ImportedAssetSet importedAssetSet) throws IOException {
        if (grp == null) {
            importedAssetSet.getMissingAssets().add("缺少 grp 产物: " + fileName);
            return;
        }
        Path output = outputRoot.resolve(fileName);
        bsdxBinService.generate(output.toString(), (com.giga.nexas.dto.bsdx.Bsdx) grp, CHARSET);
        importedAssetSet.getGeneratedGrpFiles().add(output);
    }

    private void writeReboundMek(
            AkaoGraftRequest request,
            Mek reboundAkaoMek,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundAkaoMek == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 mek");
            return;
        }

        Path output = outputRoot.resolve(request.getMekFileName());
        bsdxBinService.generate(output.toString(), reboundAkaoMek, CHARSET);
        importedAssetSet.getGeneratedMekFiles().add(output);
    }

    private void writeReboundWaz(
            AkaoGraftRequest request,
            Waz reboundAkaoWaz,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundAkaoWaz == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 waz");
            return;
        }

        Path output = outputRoot.resolve(request.getWazFileName());
        bsdxBinService.generate(output.toString(), reboundAkaoWaz, CHARSET);
        importedAssetSet.getGeneratedWazFiles().add(output);
    }

    private void copyRequiredWazFiles(
            AkaoGraftRequest request,
            JinkiImportPlan importPlan,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : importPlan.getRequiredWazFiles()) {
            if (normalize(fileName).equals(normalize(request.getWazFileName()))) {
                continue;
            }

            Path source = resolveFileCaseInsensitive(request.getJinkiWazDir(), fileName);
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少辅助 waz: " + fileName);
                continue;
            }

            Path output = outputRoot.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getGeneratedWazFiles().add(output);
        }
    }

    private void copyRequiredSpmFiles(
            AkaoGraftRequest request,
            JinkiImportPlan importPlan,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : importPlan.getRequiredSpmFiles()) {
            Path source = resolveFileCaseInsensitive(request.getJinkiSpmDir(), fileName);
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少 spm: " + fileName);
                continue;
            }

            Path output = outputRoot.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedSpmFiles().add(output);
        }
    }

    private void copyRequiredImageFiles(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiImportPlan importPlan,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在: " + request.getExternalStaticAssetRoot());
            return;
        }

        Set<String> imageNames = collectRequiredImageNames(jinkiPackage, importPlan);
        if (imageNames.isEmpty()) {
            return;
        }

        for (String imageName : imageNames) {
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

    private Set<String> collectRequiredImageNames(JinkiPackageBundle jinkiPackage, JinkiImportPlan importPlan) {
        Set<String> imageNames = new LinkedHashSet<>();
        Map<String, Spm> spmByFileName = jinkiPackage.getSpmByFileName();
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

    private void copyRequiredAudioAssets(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiImportPlan importPlan,
            Waz reboundAkaoWaz,
            Path outputRoot,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在: " + request.getExternalStaticAssetRoot());
            return;
        }

        Set<String> requiredBaseNames = new LinkedHashSet<>();
        requiredBaseNames.addAll(collectRequiredSeBaseNames(jinkiPackage, importPlan));
        requiredBaseNames.addAll(collectRequiredVoiceBaseNames(jinkiPackage, reboundAkaoWaz, request.getMekaCodeName()));

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

    private Set<String> collectRequiredSeBaseNames(JinkiPackageBundle jinkiPackage, JinkiImportPlan importPlan) {
        Set<String> baseNames = new LinkedHashSet<>();
        SeGroupGrp seGroupGrp = jinkiPackage.getSeGroupGrp();
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
            JinkiPackageBundle jinkiPackage,
            Waz reboundAkaoWaz,
            String codeName
    ) {
        Set<String> baseNames = new LinkedHashSet<>();
        if (reboundAkaoWaz == null) {
            return baseNames;
        }

        BatVoiceGrp.BatVoiceGroup akaoVoiceGroup = findAkaoVoiceGroup(jinkiPackage.getBatVoiceGrp(), codeName);
        if (akaoVoiceGroup == null || akaoVoiceGroup.getVoices() == null) {
            return baseNames;
        }

        List<Integer> usedVoiceIndices = collectUsedVoiceIndices(reboundAkaoWaz);
        for (Integer index : usedVoiceIndices) {
            if (index == null || index < 0 || index >= akaoVoiceGroup.getVoices().size()) {
                continue;
            }
            BatVoiceGrp.BatVoice voice = akaoVoiceGroup.getVoices().get(index);
            if (voice == null || !isExisting(voice.getExistFlag()) || voice.getVoiceFileName() == null || voice.getVoiceFileName().isBlank()) {
                continue;
            }
            baseNames.add(voice.getVoiceFileName());
        }

        return baseNames;
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

    private BatVoiceGrp.BatVoiceGroup findAkaoVoiceGroup(BatVoiceGrp batVoiceGrp, String codeName) {
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
