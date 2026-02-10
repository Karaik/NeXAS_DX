package com.giga.nexas.transfer.bhe2bsdx;

import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.transfer.bhe2bsdx.converter.StaticAssetCopier;
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
import java.util.Map;

/**
 * 单机体 BHE->BSDX 转换执行器。
 * <p>
 * 从 {@link com.giga.nexas.bhe2bsdx.TransferTest#runSingleSourcePipeline} 提取而来，
 * 负责对单个 {@link MekaSource} 执行完整的转换流水线：
 * 注册资源 -> 抽取源条目 -> 转换 -> 写盘 -> 复制静态资源 -> 打包 -> 重命名。
 */
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

    /**
     * 对单个源机体执行完整的 BHE->BSDX 转换流水线。
     *
     * @param source 源机体描述（baseKey + codeName）
     */
    public void run(MekaSource source) throws Exception {
        if (source == null) {
            return;
        }

        // 0. 规范化 baseKey / codeName
        String baseKey = normalizeKey(source.getBaseKey());
        String codeName = normalizeCode(source.getCodeName());
        if (baseKey.isEmpty() || codeName.isEmpty()) {
            log.warn("源机体配置无效: baseKey={}, codeName={}", source.getBaseKey(), source.getCodeName());
            return;
        }

        // 构建变体 key（c_/s_/g_/m_ 前缀）
        String cKey = variantKey("c_", baseKey);
        String sKey = variantKey("s_", baseKey);
        String gKey = variantKey("g_", baseKey);
        String mKey = variantKey("m_", baseKey);

        // 创建输出目录
        Path outputDir = config.getOutputBaseDir().resolve(baseKey);
        String outputPath = outputDir.toAbsolutePath().toString();

        log.info("========== 开始转换: baseKey={}, codeName={}, outputDir={} ==========",
                baseKey, codeName, outputDir);

        // 1. 注册全部所需文件资源（每个源机体都重新注册，保证基线干净）
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> bsdxGrp = loader.registerBsdxGrp();
        Map<String, com.giga.nexas.dto.bhe.grp.Grp> bheGrp = loader.registerBheGrp();
        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> bsdxMek = loader.registerBsdxMek();
        Map<String, com.giga.nexas.dto.bhe.mek.Mek> bheMek = loader.registerBheMek();
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> bsdxWaz = loader.registerBsdxWaz();
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> bheWaz = loader.registerBheWaz();
        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> bsdxSpm = loader.registerBsdxSpm();
        Map<String, com.giga.nexas.dto.bhe.spm.Spm> bheSpm = loader.registerBheSpm();
        Dat selectMekaMenuDat = loader.loadBsdxDat("SelectMekaMenu.dat");

        // 2. 抽出 BHE 源条目
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
        com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp bsdxMekaGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp) bsdxGrp.get("mekagroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp bsdxWazaGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp) bsdxGrp.get("wazagroup");
        com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp bsdxSpriteGroup =
                (com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp) bsdxGrp.get("spritegroup");

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
            log.warn("源资源不完整(baseKey={}): mek={}, waz={}, spm={}",
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

        // 3. 调用 TransMeka.process() 执行核心转换
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
                bsdxMekaGroup,
                bsdxWazaGroup,
                bsdxSpriteGroup,
                bsdxWaz,

                mekaPilotSpm,
                selectMekaMenuMekaSpm,
                selectMekaMenuDat,
                targetBsdxMek,
                targetCodeName,
                keepTargetKey);

        // 回写到 BSDX Map（保持内存一致），同时只输出本次变更
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
            if (!useTargetSlot && result.getBsdxCSpm() != null) {
                bsdxSpm.put(cKey, result.getBsdxCSpm());
            }
            if (!useTargetSlot && result.getBsdxSSpm() != null) {
                bsdxSpm.put(sKey, result.getBsdxSSpm());
            }
            if (!useTargetSlot && result.getBsdxGSpm() != null) {
                bsdxSpm.put(gKey, result.getBsdxGSpm());
            }
            if (!useTargetSlot && result.getBsdxMSpm() != null) {
                bsdxSpm.put(mKey, result.getBsdxMSpm());
            }
            if (result.getBsdxMekaPilotSpm() != null) {
                bsdxSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
            }
            if (result.getBsdxSelectMekaMenuMekaSpm() != null) {
                bsdxSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
            }
        }

        // 4. 输出变更文件（grp/mek/waz/spm）
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> outputGrp = new HashMap<>();
        outputGrp.put("batvoice", bsdxBatVoice);
        outputGrp.put("mekagroup", bsdxMekaGroup);
        outputGrp.put("wazagroup", bsdxWazaGroup);
        outputGrp.put("spritegroup", bsdxSpriteGroup);

        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> outputMek = new HashMap<>();
        if (result != null && result.getBsdxMeka() != null) {
            outputMek.put(useTargetSlot ? targetKey : baseKey, result.getBsdxMeka());
        }

        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> outputWaz = new HashMap<>();
        if (result != null && result.getBsdxWaz() != null) {
            outputWaz.put(useTargetSlot ? targetKey : baseKey, result.getBsdxWaz());
        }

        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> outputSpm = new HashMap<>();
        if (result != null && result.getBsdxSpm() != null) {
            outputSpm.put(useTargetSlot ? targetSpriteKey : baseKey, result.getBsdxSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxCSpm() != null) {
            outputSpm.put(cKey, result.getBsdxCSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxSSpm() != null) {
            outputSpm.put(sKey, result.getBsdxSSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxGSpm() != null) {
            outputSpm.put(gKey, result.getBsdxGSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxMSpm() != null) {
            outputSpm.put(mKey, result.getBsdxMSpm());
        }
        if (result != null && result.getBsdxMekaPilotSpm() != null) {
            outputSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
        }
        if (result != null && result.getBsdxSelectMekaMenuMekaSpm() != null) {
            outputSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
        }

        TransMekaOutputWriter outputWriter = new TransMekaOutputWriter();
        outputWriter.writeOutputs(outputDir, outputGrp, outputMek, outputWaz, outputSpm);

        // 5. 复制静态资源
        if (config.isCopyStaticAssets()) {
            StaticAssetCopier assetCopier = new StaticAssetCopier();
            Map<String, com.giga.nexas.dto.bsdx.grp.Grp> assetGrp = new HashMap<>();
            if (result != null && result.getBsdxBatVoiceGroup() != null) {
                BatVoiceGrp batVoiceGrp = new BatVoiceGrp();
                batVoiceGrp.getVoiceList().add(result.getBsdxBatVoiceGroup());
                assetGrp.put("batvoice", batVoiceGrp);
            }
            // 仅复制 BHE 来源的 spm 静态资源，避免搜索 BSDX 自带图片
            Map<String, com.giga.nexas.dto.bsdx.spm.Spm> assetSpm = new HashMap<>();
            if (result != null) {
                putIfPresent(assetSpm, baseKey, result.getBsdxSpm());
                putIfPresent(assetSpm, cKey, result.getBsdxCSpm());
                putIfPresent(assetSpm, sKey, result.getBsdxSSpm());
                putIfPresent(assetSpm, gKey, result.getBsdxGSpm());
                putIfPresent(assetSpm, mKey, result.getBsdxMSpm());
            }
            assetCopier.copyAssets(outputDir, config.getStaticAssetRoot(), assetSpm, assetGrp);
        }

        // 6. 打包
        String packLog = PacUtil.pack(outputPath, config.getPacCompressMode());
        log.info("outputPath === {}", packLog);

        // 7. 统一输出包名：<outputDir>.pacNew -> Update3_<baseKey>.pac
        Path pacNew = outputDir.resolveSibling(outputDir.getFileName().toString() + ".pacNew");
        Path updatePac = outputDir.resolveSibling("Update3_" + baseKey + ".pac");
        if (Files.exists(pacNew)) {
            Files.move(pacNew, updatePac, StandardCopyOption.REPLACE_EXISTING);
            log.info("pac renamed: {} -> {}", pacNew.getFileName(), updatePac.getFileName());
        } else {
            log.warn("pac not found: {}", pacNew);
        }
    }

    // ===== 辅助方法 =====

    /**
     * 根据目标 mek 的 spmFileSequence 索引，从 spriteGroup 中解析出 sprite 基础文件名。
     * 用于确定目标槽位对应的 spm 文件名。
     */
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

    /**
     * 仅当 value 非 null 时放入 map，避免写出空条目。
     */
    private void putIfPresent(Map<String, com.giga.nexas.dto.bsdx.spm.Spm> map,
                              String key,
                              com.giga.nexas.dto.bsdx.spm.Spm value) {
        if (map == null || key == null || value == null) {
            return;
        }
        map.put(key, value);
    }

    private String normalizeKey(String key) {
        return key == null ? "" : key.trim().toLowerCase();
    }

    private String normalizeCode(String codeName) {
        return codeName == null ? "" : codeName.trim().toUpperCase();
    }

    private String variantKey(String prefix, String baseKey) {
        if (prefix == null || baseKey == null || baseKey.isEmpty()) {
            return baseKey;
        }
        return prefix + baseKey;
    }
}
