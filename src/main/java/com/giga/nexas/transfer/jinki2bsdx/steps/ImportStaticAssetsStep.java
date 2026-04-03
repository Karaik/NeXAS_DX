package com.giga.nexas.transfer.jinki2bsdx.steps;

import com.giga.nexas.dto.bsdx.mek.Mek;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 负责把 step6/7 产物和当前机体链上的静态资源真正落盘到输出目录。
 */
public class ImportStaticAssetsStep {

    private static final String CHARSET = "windows-31j";
    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS");

    private final BsdxBinService bsdxBinService = new BsdxBinService();

    public ImportedAssetSet importAssets(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiImportPlan importPlan,
            Mek reboundAkaoMek,
            Waz reboundAkaoWaz
    ) {
        ImportedAssetSet importedAssetSet = new ImportedAssetSet();
        if (request == null || jinkiPackage == null || importPlan == null) {
            return importedAssetSet;
        }

        importedAssetSet.getWazFiles().addAll(importPlan.getRequiredWazFiles());
        importedAssetSet.getSpmFiles().addAll(importPlan.getRequiredSpmFiles());
        importedAssetSet.getAuxiliaryFiles().addAll(importPlan.getRequiredMekFiles());

        try {
            // Step 8-1: 先创建这次迁移产物的独立输出根目录。
            Path outputRoot = request.getExeOutputDir().resolve("jinki2bsdx_assets_" + LocalDateTime.now().format(TS));
            Files.createDirectories(outputRoot);
            importedAssetSet.setOutputRootDir(outputRoot);

            Path mekDir = outputRoot.resolve("mek");
            Path wazDir = outputRoot.resolve("waz");
            Path spmDir = outputRoot.resolve("spm");
            Path audioDir = outputRoot.resolve("audio");
            Files.createDirectories(mekDir);
            Files.createDirectories(wazDir);
            Files.createDirectories(spmDir);
            Files.createDirectories(audioDir);

            // Step 8-2: 先写入 step6 生成的主 mek 产物。
            writeReboundMek(request, reboundAkaoMek, mekDir, importedAssetSet);

            // Step 8-3: 再写入 step7 生成的主 waz 产物。
            writeReboundWaz(request, reboundAkaoWaz, wazDir, importedAssetSet);

            // Step 8-4: 把链上其余辅助 waz 从包内 JINKI 目录复制进输出。
            copyRequiredWazFiles(request, importPlan, wazDir, importedAssetSet);

            // Step 8-5: 把链上需要的 spm 从包内 JINKI 目录复制进输出。
            copyRequiredSpmFiles(request, importPlan, spmDir, importedAssetSet);

            // Step 8-6: 再从项目外静态资源目录补齐当前链实际需要的语音和音效文件。
            copyRequiredAudioAssets(request, jinkiPackage, importPlan, audioDir, importedAssetSet);
            return importedAssetSet;
        } catch (IOException e) {
            throw new IllegalStateException("step8 静态资源落盘失败", e);
        }
    }

    private void writeReboundMek(
            AkaoGraftRequest request,
            Mek reboundAkaoMek,
            Path mekDir,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundAkaoMek == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 mek");
            return;
        }

        Path output = mekDir.resolve(request.getMekFileName());
        bsdxBinService.generate(output.toString(), reboundAkaoMek, CHARSET);
        importedAssetSet.getGeneratedMekFiles().add(output);
    }

    private void writeReboundWaz(
            AkaoGraftRequest request,
            Waz reboundAkaoWaz,
            Path wazDir,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (reboundAkaoWaz == null) {
            importedAssetSet.getMissingAssets().add("缺少重绑后的主 waz");
            return;
        }

        Path output = wazDir.resolve(request.getWazFileName());
        bsdxBinService.generate(output.toString(), reboundAkaoWaz, CHARSET);
        importedAssetSet.getGeneratedWazFiles().add(output);
    }

    private void copyRequiredWazFiles(
            AkaoGraftRequest request,
            JinkiImportPlan importPlan,
            Path wazDir,
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

            Path output = wazDir.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getGeneratedWazFiles().add(output);
        }
    }

    private void copyRequiredSpmFiles(
            AkaoGraftRequest request,
            JinkiImportPlan importPlan,
            Path spmDir,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        for (String fileName : importPlan.getRequiredSpmFiles()) {
            Path source = resolveFileCaseInsensitive(request.getJinkiSpmDir(), fileName);
            if (source == null) {
                importedAssetSet.getMissingAssets().add("缺少 spm: " + fileName);
                continue;
            }

            Path output = spmDir.resolve(source.getFileName().toString());
            Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
            importedAssetSet.getCopiedSpmFiles().add(output);
        }
    }

    private void copyRequiredAudioAssets(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiImportPlan importPlan,
            Path audioDir,
            ImportedAssetSet importedAssetSet
    ) throws IOException {
        if (request.getExternalStaticAssetRoot() == null || !Files.exists(request.getExternalStaticAssetRoot())) {
            importedAssetSet.getMissingAssets().add("外部静态资源目录不存在: " + request.getExternalStaticAssetRoot());
            return;
        }

        Set<String> requiredBaseNames = collectRequiredAudioBaseNames(request, jinkiPackage, importPlan);
        if (requiredBaseNames.isEmpty()) {
            return;
        }

        Map<String, List<Path>> externalIndex = buildExternalAssetIndex(request.getExternalStaticAssetRoot());
        Set<String> copiedNames = new LinkedHashSet<>();

        for (String baseName : requiredBaseNames) {
            List<Path> matched = externalIndex.get(normalizeBaseName(baseName));
            if (matched == null || matched.isEmpty()) {
                importedAssetSet.getMissingAssets().add("缺少外部静态资源: " + baseName);
                continue;
            }

            for (Path source : matched) {
                String targetName = source.getFileName().toString();
                if (!copiedNames.add(normalize(targetName))) {
                    continue;
                }
                Path output = audioDir.resolve(targetName);
                Files.copy(source, output, StandardCopyOption.REPLACE_EXISTING);
                importedAssetSet.getCopiedAudioFiles().add(output);
            }
        }
    }

    private Set<String> collectRequiredAudioBaseNames(
            AkaoGraftRequest request,
            JinkiPackageBundle jinkiPackage,
            JinkiImportPlan importPlan
    ) {
        Set<String> baseNames = new LinkedHashSet<>();

        // 先收集当前机体自己的语音文件名。
        BatVoiceGrp.BatVoiceGroup akaoVoiceGroup = findAkaoVoiceGroup(jinkiPackage.getBatVoiceGrp(), request.getMekaCodeName());
        if (akaoVoiceGroup != null && akaoVoiceGroup.getVoices() != null) {
            for (BatVoiceGrp.BatVoice voice : akaoVoiceGroup.getVoices()) {
                if (voice == null || !isExisting(voice.getExistFlag()) || voice.getVoiceFileName() == null || voice.getVoiceFileName().isBlank()) {
                    continue;
                }
                baseNames.add(voice.getVoiceFileName());
            }
        }

        // 再收集当前机体在 Akao.waz 中真实使用到的 SE 文件名。
        SeGroupGrp seGroupGrp = jinkiPackage.getSeGroupGrp();
        if (seGroupGrp != null) {
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
        }

        return baseNames;
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

    private Map<String, List<Path>> buildExternalAssetIndex(Path root) throws IOException {
        Map<String, List<Path>> index = new HashMap<>();

        try (Stream<Path> stream = Files.walk(root)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                String baseName = normalizeBaseName(path.getFileName().toString());
                index.computeIfAbsent(baseName, key -> new ArrayList<>()).add(path);
            });
        }

        return index;
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

    private String normalizeBaseName(String fileName) {
        String normalized = normalize(fileName);
        int dot = normalized.lastIndexOf('.');
        return dot >= 0 ? normalized.substring(0, dot) : normalized;
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
