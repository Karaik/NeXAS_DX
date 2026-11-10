package com.giga.nexas.transfer.bhe2bsdx;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.transfer.bhe2bsdx.converter.StaticAssetCopier;
import com.giga.nexas.transfer.bhe2bsdx.converter.TransferDependencyCollector;
import com.giga.nexas.transfer.bhe2bsdx.converter.TransMekaOutputWriter;
import com.giga.nexas.transfer.bhe2bsdx.model.MekaSource;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMeka;
import com.giga.nexas.transfer.bhe2bsdx.model.TransMekaResult;
import com.giga.nexas.util.PacUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Stream;

@Slf4j
public class Bhe2BsdxSingleRunner {

    private final Bhe2BsdxConfig config;
    private final Bhe2BsdxResourceLoader loader;
    private final Bhe2BsdxSourceDiscovery discovery;

    public Bhe2BsdxSingleRunner(Bhe2BsdxConfig config,
                                Bhe2BsdxResourceLoader loader,
                                Bhe2BsdxSourceDiscovery discovery) {
        this.config = config;
        this.loader = loader;
        this.discovery = discovery;
    }

    public void run(MekaSource source) throws Exception {
        if (source == null) {
            return;
        }

        String baseKey = normalizeKey(source.getBaseKey());
        String codeName = normalizeCode(source.getCodeName());
        if (baseKey.isEmpty() || codeName.isEmpty()) {
            log.warn("invalid source config: baseKey={}, codeName={}", source.getBaseKey(), source.getCodeName());
            return;
        }

        String cKey = variantKey("c_", baseKey);
        String sKey = variantKey("s_", baseKey);
        String gKey = variantKey("g_", baseKey);
        String mKey = variantKey("m_", baseKey);

        Path outputDir = config.getOutputBaseDir().resolve(baseKey);
        String outputPath = outputDir.toAbsolutePath().toString();
        prepareOutputDir(outputDir);

        log.info("========== start transfer: baseKey={}, codeName={}, outputDir={} ==========",
                baseKey, codeName, outputDir);

        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> bsdxGrp = loader.registerBsdxGrp();
        Map<String, com.giga.nexas.dto.bhe.grp.Grp> bheGrp = loader.registerBheGrp();
        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> bsdxMek = loader.registerBsdxMek();
        Map<String, com.giga.nexas.dto.bhe.mek.Mek> bheMek = loader.registerBheMek();
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> bsdxWaz = loader.registerBsdxWaz();
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> bheWaz = loader.registerBheWaz();
        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> bsdxSpm = loader.registerBsdxSpm();
        Map<String, com.giga.nexas.dto.bhe.spm.Spm> bheSpm = loader.registerBheSpm();
        Dat selectMekaMenuDat = loader.loadBsdxDat("SelectMekaMenu.dat");

        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp bheBatVoice =
                (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp) bheGrp.get("batvoice");
        com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp bsdxBatVoice =
                (com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp) bsdxGrp.get("batvoice");
        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup sourceBatvoice =
                discovery.findBatVoiceGroupByCode(bheBatVoice, codeName);

        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp bheMekaGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp) bheGrp.get("mekagroup");
        com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp bheWazaGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp) bheGrp.get("wazagroup");
        com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp bheSpriteGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp) bheGrp.get("spritegroup");
        com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp bheSeGroup =
                (com.giga.nexas.dto.bhe.grp.groupmap.SeGroupGrp) bheGrp.get("segroup");

        com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp bsdxMekaGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp) bsdxGrp.get("mekagroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp bsdxWazaGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp) bsdxGrp.get("wazagroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp bsdxSpriteGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp) bsdxGrp.get("spritegroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp bsdxSeGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp) bsdxGrp.get("segroup");

        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup sourceMekaGroup =
                discovery.findMekaGroupByCode(bheMekaGroup, codeName);
        com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry sourceWazaGroup =
                discovery.findWazaGroupByCode(bheWazaGroup, codeName);
        com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry sourceSpriteGroup =
                discovery.findSpriteGroupByCode(bheSpriteGroup, codeName);

        com.giga.nexas.dto.bhe.mek.Mek sourceMek = bheMek.get(baseKey);
        com.giga.nexas.dto.bhe.waz.Waz sourceWaz = bheWaz.get(baseKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceSpm = bheSpm.get(baseKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceCSpm = bheSpm.get(cKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceSSpm = bheSpm.get(sKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceGSpm = bheSpm.get(gKey);
        com.giga.nexas.dto.bhe.spm.Spm sourceMSpm = bheSpm.get(mKey);

        if (sourceMek == null || sourceWaz == null || sourceSpm == null) {
            log.warn("incomplete source resources(baseKey={}): mek={}, waz={}, spm={}",
                    baseKey, sourceMek != null, sourceWaz != null, sourceSpm != null);
            return;
        }

        String targetKey = config.getTargetKey();
        String targetCodeName = config.getTargetCodeName();
        boolean keepTargetKey = config.isKeepTargetKey();

        com.giga.nexas.dto.bsdx.spm.Spm mekaPilotSpm = bsdxSpm.get("mekapilot");
        com.giga.nexas.dto.bsdx.spm.Spm selectMekaMenuMekaSpm = bsdxSpm.get("selectmekamenumeka");
        com.giga.nexas.dto.bsdx.mek.Mek targetBsdxMek = bsdxMek.get(targetKey);
        boolean useTargetSlot = targetBsdxMek != null;
        String targetSpriteKey = useTargetSlot
                ? resolveSpriteBaseName(bsdxSpriteGroup, targetBsdxMek, targetKey)
                : baseKey;

        TransMekaResult result = TransMeka.process(
                sourceMek,
                sourceWaz,
                sourceSpm,
                sourceCSpm,
                sourceSSpm,
                sourceGSpm,
                sourceMSpm,
                sourceBatvoice,
                bsdxBatVoice,
                sourceMekaGroup,
                sourceWazaGroup,
                sourceSpriteGroup,
                bheSpriteGroup,
                bheSeGroup,
                bsdxMekaGroup,
                bsdxWazaGroup,
                bsdxSpriteGroup,
                bsdxSeGroup,
                bsdxWaz,
                mekaPilotSpm,
                selectMekaMenuMekaSpm,
                selectMekaMenuDat,
                targetBsdxMek,
                targetCodeName,
                keepTargetKey
        );

        if (result != null) {
            if (result.getBsdxMeka() != null) {
                if (useTargetSlot) {
                    result.getBsdxMeka().setFileName(targetKey);
                }
                bsdxMek.put(useTargetSlot ? targetKey : baseKey, result.getBsdxMeka());
            }
            if (result.getBsdxWaz() != null) {
                if (useTargetSlot) {
                    result.getBsdxWaz().setFileName(targetKey);
                }
                bsdxWaz.put(useTargetSlot ? targetKey : baseKey, result.getBsdxWaz());
            }
            if (result.getBsdxSpm() != null) {
                bsdxSpm.put(useTargetSlot ? targetSpriteKey : baseKey, result.getBsdxSpm());
            }
            if (result.getBsdxCSpm() != null) {
                bsdxSpm.put(cKey, result.getBsdxCSpm());
            }
            if (result.getBsdxSSpm() != null) {
                bsdxSpm.put(sKey, result.getBsdxSSpm());
            }
            if (result.getBsdxGSpm() != null) {
                bsdxSpm.put(gKey, result.getBsdxGSpm());
            }
            if (result.getBsdxMSpm() != null) {
                bsdxSpm.put(mKey, result.getBsdxMSpm());
            }
            if (result.getBsdxMekaPilotSpm() != null) {
                bsdxSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
            }
            if (result.getBsdxSelectMekaMenuMekaSpm() != null) {
                bsdxSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
            }
        }

        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> outputGrp = new HashMap<>();
        outputGrp.put("batvoice", bsdxBatVoice);
        outputGrp.put("mekagroup", bsdxMekaGroup);
        outputGrp.put("wazagroup", bsdxWazaGroup);
        outputGrp.put("spritegroup", bsdxSpriteGroup);
        if (bsdxSeGroup != null) {
            outputGrp.put("segroup", bsdxSeGroup);
        }

        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> outputMek = new HashMap<>();
        if (result != null && result.getBsdxMeka() != null) {
            outputMek.put(useTargetSlot ? targetKey : baseKey, result.getBsdxMeka());
        }

        TransferDependencyCollector dependencyCollector = new TransferDependencyCollector();
        String outputWazMainKey = useTargetSlot ? targetKey : baseKey;
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> outputWaz = dependencyCollector.collectWazOutputMap(
                outputWazMainKey,
                result != null ? result.getBsdxWaz() : null,
                bsdxWaz
        );

        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> outputSpm = new LinkedHashMap<>(
                dependencyCollector.collectSpmOutputMap(
                        outputWazMainKey,
                        result != null ? result.getBsdxWaz() : null,
                        bsdxWaz,
                        bsdxSpriteGroup,
                        bsdxSpm
                )
        );
        if (result != null && result.getBsdxSpm() != null) {
            outputSpm.put(useTargetSlot ? targetSpriteKey : baseKey, result.getBsdxSpm());
        }
        if (result != null && result.getBsdxCSpm() != null) {
            outputSpm.put(cKey, result.getBsdxCSpm());
        }
        if (result != null && result.getBsdxSSpm() != null) {
            outputSpm.put(sKey, result.getBsdxSSpm());
        }
        if (result != null && result.getBsdxGSpm() != null) {
            outputSpm.put(gKey, result.getBsdxGSpm());
        }
        if (result != null && result.getBsdxMSpm() != null) {
            outputSpm.put(mKey, result.getBsdxMSpm());
        }
        if (result != null && result.getBsdxMekaPilotSpm() != null) {
            outputSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
        }
        if (result != null && result.getBsdxSelectMekaMenuMekaSpm() != null) {
            outputSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
        }
        includeBombWazWhenBombSpritePresent(outputWaz, outputSpm, bsdxWaz);

        TransMekaOutputWriter outputWriter = new TransMekaOutputWriter();
        outputWriter.writeOutputs(outputDir, outputGrp, outputMek, outputWaz, outputSpm);

        if (config.isCopyStaticAssets()) {
            Path staticRoot = config.getStaticAssetRoot();
            if (staticRoot == null || !Files.isDirectory(staticRoot)) {
                throw new IllegalStateException("copyStaticAssets=true but staticAssetRoot not found: " + staticRoot);
            }
            StaticAssetCopier assetCopier = new StaticAssetCopier();
            Map<String, com.giga.nexas.dto.bsdx.grp.Grp> assetGrp = new HashMap<>();
            if (result != null && result.getBsdxBatVoiceGroup() != null) {
                BatVoiceGrp batVoiceGrp = new BatVoiceGrp();
                batVoiceGrp.getVoiceList().add(result.getBsdxBatVoiceGroup());
                assetGrp.put("batvoice", batVoiceGrp);
            }
            Map<String, com.giga.nexas.dto.bsdx.spm.Spm> assetSpm = new LinkedHashMap<>(outputSpm);
            assetCopier.copyAssets(outputDir, staticRoot, assetSpm, assetGrp);
        }

        String packLog = PacUtil.pack(outputPath, config.getPacCompressMode());
        log.info("outputPath === {}", packLog);

        Path pacNew = outputDir.resolveSibling(outputDir.getFileName().toString() + ".pacNew");
        Path updatePac = outputDir.resolveSibling("Update3_" + baseKey + ".pac");
        if (Files.exists(pacNew)) {
            Files.move(pacNew, updatePac, StandardCopyOption.REPLACE_EXISTING);
            log.info("pac renamed: {} -> {}", pacNew.getFileName(), updatePac.getFileName());
        } else {
            log.warn("pac not found: {}", pacNew);
        }
    }

    private String resolveSpriteBaseName(
            com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp spriteGroup,
            com.giga.nexas.dto.bsdx.mek.Mek targetMek,
            String fallback
    ) {
        if (spriteGroup == null || targetMek == null || targetMek.getMekBasicInfo() == null) {
            return fallback;
        }
        Integer index = targetMek.getMekBasicInfo().getSpmFileSequence();
        if (index == null || spriteGroup.getSpriteList() == null
                || index < 0 || index >= spriteGroup.getSpriteList().size()) {
            return fallback;
        }
        com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry =
                spriteGroup.getSpriteList().get(index);
        if (entry == null || entry.getSpriteFileName() == null) {
            return fallback;
        }
        String fileName = entry.getSpriteFileName().trim();
        if (fileName.isEmpty()) {
            return fallback;
        }
        int dot = fileName.lastIndexOf('.');
        String baseName = dot > 0 ? fileName.substring(0, dot) : fileName;
        return baseName.isEmpty() ? fallback : baseName;
    }

    private void prepareOutputDir(Path outputDir) throws Exception {
        if (outputDir == null) {
            return;
        }
        if (!Files.exists(outputDir)) {
            Files.createDirectories(outputDir);
            return;
        }
        try (Stream<Path> stream = Files.list(outputDir)) {
            for (Path child : stream.toList()) {
                if (Files.isRegularFile(child)) {
                    Files.deleteIfExists(child);
                }
            }
        }
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private String normalizeCode(String codeName) {
        return codeName == null ? "" : codeName.trim().toUpperCase();
    }

    private void putIfPresent(
            Map<String, com.giga.nexas.dto.bsdx.spm.Spm> map,
            String key,
            com.giga.nexas.dto.bsdx.spm.Spm value
    ) {
        if (map == null || key == null || key.isBlank() || value == null) {
            return;
        }
        map.put(key, value);
    }

    private String variantKey(String prefix, String baseKey) {
        if (prefix == null || baseKey == null || baseKey.isEmpty()) {
            return baseKey;
        }
        return prefix + baseKey;
    }

    private void includeBombWazWhenBombSpritePresent(
            Map<String, com.giga.nexas.dto.bsdx.waz.Waz> outputWaz,
            Map<String, com.giga.nexas.dto.bsdx.spm.Spm> outputSpm,
            Map<String, com.giga.nexas.dto.bsdx.waz.Waz> registryWaz
    ) {
        if (outputWaz == null || outputSpm == null || registryWaz == null) {
            return;
        }
        if (!outputSpm.containsKey("bomb") || outputWaz.containsKey("bomb")) {
            return;
        }
        com.giga.nexas.dto.bsdx.waz.Waz bomb = registryWaz.get("bomb");
        if (bomb != null) {
            outputWaz.put("bomb", bomb);
        }
    }
}
