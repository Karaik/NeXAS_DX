package com.giga.nexas.bhe2bsdx;

import com.giga.nexas.bhe2bsdx.steps.TransMeka;
import com.giga.nexas.bhe2bsdx.steps.TransMekaOutputWriter;
import com.giga.nexas.bhe2bsdx.steps.TransMekaResult;
import com.giga.nexas.bhe2bsdx.steps.StaticAssetCopier;
import com.giga.nexas.bhe2bsdx.steps.WazConverter;
import com.giga.nexas.dto.ResponseDTO;

import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.util.PacUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

public class TransferTest {

    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/testBhe");
    // 静态资源来源目录（按需修改）
    private static final Path STATIC_ASSET_ROOT = Paths.get("D:\\BaiduNetdiskDownload\\bsdx_bhe\\bheAll");
    private static final boolean COPY_STATIC_ASSETS = true;

    private static final Logger log = LoggerFactory.getLogger(TransferTest.class);
    private final BsdxBinService bsdxBinService = new BsdxBinService();
    private final BheBinService bheBinService = new BheBinService();

    /**
     * 移植用，pipeline模拟
     */
    @Test
    public void testPipeline() throws Exception {

        Path outputDir = OUTPUT_DIR;
        String outputPath = outputDir.toAbsolutePath().toString();

        final String tsukuyomiKey = "tsukuyomi";
        final String cTsukuyomiKey = "c_tsukuyomi";
        final String sTsukuyomiKey = "s_tsukuyomi";
        final String gTsukuyomiKey = "g_tsukuyomi";
        final String mTsukuyomiKey = "m_tsukuyomi";
        final String targetKey = "nanoha";
        final String targetCodeName = "NANOHA";

        // 1.注册全部所需文件资源
        // grp
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> bsdxGrp = registerBsdxGrp();
        Map<String, com.giga.nexas.dto.bhe.grp.Grp> bheGrp = registerBheGrp();
        // mek
        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> bsdxMek = registerBsdxMek();
        Map<String, com.giga.nexas.dto.bhe.mek.Mek> bheMek = registerBheMek();
        // waz
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> bsdxWaz = registerBsdxWaz();
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> bheWaz = registerBheWaz();
        // spm
        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> bsdxSpm = registerBsdxSpm();
        Map<String, com.giga.nexas.dto.bhe.spm.Spm> bheSpm = registerBheSpm();
        // dat（UI 选择菜单映射表）
        Dat selectMekaMenuDat = loadBsdxDat("SelectMekaMenu.dat");

        // 2.抽出移植目标
        // 月读
        // batVoice
        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp bheBatVoice =
                (com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp) bheGrp.get("batvoice");
        com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp bsdxBatVoice =
                (com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp) bsdxGrp.get("batvoice");
        com.giga.nexas.dto.bhe.grp.groupmap.BatVoiceGrp.BatVoiceGroup tsukuyomiBatvoice = bheBatVoice.getVoiceList().get(1);
        // grp: 机体/技能/spm 注册表
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
        final String tsukuyomiCode = "TSUKUYOMI";
        com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup tsukuyomiMekaGroup = null;
        for (com.giga.nexas.dto.bhe.grp.groupmap.MekaGroupGrp.MekaGroup group : bheMekaGroup.getMekaList()) {
            if (tsukuyomiCode.equalsIgnoreCase(group.getMekaCodeName())) {
                tsukuyomiMekaGroup = group;
                break;
            }
        }
        com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry tsukuyomiWazaGroup = null;
        for (com.giga.nexas.dto.bhe.grp.groupmap.WazaGroupGrp.WazaGroupEntry entry : bheWazaGroup.getWazaList()) {
            if (tsukuyomiCode.equalsIgnoreCase(entry.getWazaCodeName())) {
                tsukuyomiWazaGroup = entry;
                break;
            }
        }
        com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry tsukuyomiSpriteGroup = null;
        for (com.giga.nexas.dto.bhe.grp.groupmap.SpriteGroupGrp.SpriteGroupEntry entry : bheSpriteGroup.getSpriteList()) {
            if (tsukuyomiCode.equalsIgnoreCase(entry.getSpriteCodeName())) {
                tsukuyomiSpriteGroup = entry;
                break;
            }
        }
        // mek
        com.giga.nexas.dto.bhe.mek.Mek tsukuyomiMek = bheMek.get(tsukuyomiKey);
        // waz
        com.giga.nexas.dto.bhe.waz.Waz tsukuyomiWaz = bheWaz.get(tsukuyomiKey);
        // spm（5 种静态资源）
        com.giga.nexas.dto.bhe.spm.Spm tsukuyomiSpm = bheSpm.get(tsukuyomiKey);
        com.giga.nexas.dto.bhe.spm.Spm tsukuyomiCSpm = bheSpm.get(cTsukuyomiKey);
        com.giga.nexas.dto.bhe.spm.Spm tsukuyomiSSpm = bheSpm.get(sTsukuyomiKey);
        com.giga.nexas.dto.bhe.spm.Spm tsukuyomiGSpm = bheSpm.get(gTsukuyomiKey);
        com.giga.nexas.dto.bhe.spm.Spm tsukuyomiMSpm = bheSpm.get(mTsukuyomiKey);

        com.giga.nexas.dto.bsdx.spm.Spm mekaPilotSpm = bsdxSpm.get("mekapilot");
        com.giga.nexas.dto.bsdx.spm.Spm selectMekaMenuMekaSpm = bsdxSpm.get("selectmekamenumeka");
        com.giga.nexas.dto.bsdx.mek.Mek targetBsdxMek = bsdxMek.get(targetKey);
        boolean useTargetSlot = targetBsdxMek != null;
        boolean keepTargetKey = true;
        String targetSpriteKey = useTargetSlot
                ? resolveSpriteBaseName(bsdxSpriteGroup, targetBsdxMek, targetKey)
                : tsukuyomiKey;

        //
        TransMekaResult result = TransMeka.process(
                tsukuyomiMek,
                tsukuyomiWaz,
                tsukuyomiSpm,
                tsukuyomiCSpm,
                tsukuyomiSSpm,
                tsukuyomiGSpm,
                tsukuyomiMSpm,

                tsukuyomiBatvoice,
                bsdxBatVoice,

                tsukuyomiMekaGroup,
                tsukuyomiWazaGroup,
                tsukuyomiSpriteGroup,
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

        // 3.回写到 BSDX Map（保持内存一致），同时只输出本次变更，避免写出全部 spm
        if (result != null) {
            if (result.getBsdxMeka() != null) {
                if (useTargetSlot) {
                    result.getBsdxMeka().setFileName(targetKey);
                }
                bsdxMek.put(useTargetSlot ? targetKey : tsukuyomiKey, result.getBsdxMeka());
            }
            if (result.getBsdxWaz() != null) {
                if (useTargetSlot) {
                    result.getBsdxWaz().setFileName(targetKey);
                }
                bsdxWaz.put(useTargetSlot ? targetKey : tsukuyomiKey, result.getBsdxWaz());
            }
            if (result.getBsdxSpm() != null) {
                bsdxSpm.put(useTargetSlot ? targetSpriteKey : tsukuyomiKey, result.getBsdxSpm());
            }
            if (!useTargetSlot && result.getBsdxCSpm() != null) {
                bsdxSpm.put(cTsukuyomiKey, result.getBsdxCSpm());
            }
            if (!useTargetSlot && result.getBsdxSSpm() != null) {
                bsdxSpm.put(sTsukuyomiKey, result.getBsdxSSpm());
            }
            if (!useTargetSlot && result.getBsdxGSpm() != null) {
                bsdxSpm.put(gTsukuyomiKey, result.getBsdxGSpm());
            }
            if (!useTargetSlot && result.getBsdxMSpm() != null) {
                bsdxSpm.put(mTsukuyomiKey, result.getBsdxMSpm());
            }
            if (result.getBsdxMekaPilotSpm() != null) {
                bsdxSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
            }
            if (result.getBsdxSelectMekaMenuMekaSpm() != null) {
                bsdxSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
            }
        }

        // 4.输出变更文件（grp/mek/waz/spm）
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> outputGrp = new HashMap<>();
        outputGrp.put("batvoice", bsdxBatVoice);
        outputGrp.put("mekagroup", bsdxMekaGroup);
        outputGrp.put("wazagroup", bsdxWazaGroup);
        outputGrp.put("spritegroup", bsdxSpriteGroup);

        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> outputMek = new HashMap<>();
        if (result != null && result.getBsdxMeka() != null) {
            outputMek.put(useTargetSlot ? targetKey : tsukuyomiKey, result.getBsdxMeka());
        }

        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> outputWaz = new HashMap<>();
        if (result != null && result.getBsdxWaz() != null) {
            outputWaz.put(useTargetSlot ? targetKey : tsukuyomiKey, result.getBsdxWaz());
        }

        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> outputSpm = new HashMap<>();
        if (result != null && result.getBsdxSpm() != null) {
            outputSpm.put(useTargetSlot ? targetSpriteKey : tsukuyomiKey, result.getBsdxSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxCSpm() != null) {
            outputSpm.put(cTsukuyomiKey, result.getBsdxCSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxSSpm() != null) {
            outputSpm.put(sTsukuyomiKey, result.getBsdxSSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxGSpm() != null) {
            outputSpm.put(gTsukuyomiKey, result.getBsdxGSpm());
        }
        if (!useTargetSlot && result != null && result.getBsdxMSpm() != null) {
            outputSpm.put(mTsukuyomiKey, result.getBsdxMSpm());
        }
        if (result != null && result.getBsdxMekaPilotSpm() != null) {
            outputSpm.put("mekapilot", result.getBsdxMekaPilotSpm());
        }
        if (result != null && result.getBsdxSelectMekaMenuMekaSpm() != null) {
            outputSpm.put("selectmekamenumeka", result.getBsdxSelectMekaMenuMekaSpm());
        }

        TransMekaOutputWriter outputWriter = new TransMekaOutputWriter();
        outputWriter.writeOutputs(outputDir, outputGrp, outputMek, outputWaz, outputSpm);

        if (COPY_STATIC_ASSETS) {
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
                putIfPresent(assetSpm, tsukuyomiKey, result.getBsdxSpm());
                putIfPresent(assetSpm, cTsukuyomiKey, result.getBsdxCSpm());
                putIfPresent(assetSpm, sTsukuyomiKey, result.getBsdxSSpm());
                putIfPresent(assetSpm, gTsukuyomiKey, result.getBsdxGSpm());
                putIfPresent(assetSpm, mTsukuyomiKey, result.getBsdxMSpm());
            }
            assetCopier.copyAssets(outputDir, STATIC_ASSET_ROOT, assetSpm, assetGrp);
        }

        // 打包
        String packLog = PacUtil.pack(outputPath, "4");
        log.info("outputPath === {}", packLog);

        // 统一输出包名：testBhe.pacNew -> Update3.pac
        Path pacNew = outputDir.resolveSibling(outputDir.getFileName().toString() + ".pacNew");
        Path updatePac = outputDir.resolveSibling("Update3.pac");
        if (Files.exists(pacNew)) {
            Files.move(pacNew, updatePac, StandardCopyOption.REPLACE_EXISTING);
            log.info("✅ pac renamed: {} -> {}", pacNew.getFileName(), updatePac.getFileName());
        } else {
            log.warn("⚠️ pac not found: {}", pacNew);
        }
    }

    @Test
    public void testTransSingle() throws Exception {
        Path resourceDir = Paths.get("src/main/resources");
        String bheWazJsonName = "tkytama.waz.json";
        Path bheWazJsonPath = resourceDir.resolve(bheWazJsonName);

        // 1. 读取 BHE WAZ JSON
        com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper()
                .configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        String jsonStr = Files.readString(bheWazJsonPath);
        com.giga.nexas.dto.bhe.waz.Waz bheWaz = mapper.readValue(jsonStr, com.giga.nexas.dto.bhe.waz.Waz.class);
        log.info("✅ BHE WAZ loaded: {}, skills={}", bheWaz.getFileName(), bheWaz.getSkillList().size());

        // 2. 转换为 BSDX WAZ
        WazConverter wazConverter = new WazConverter();
        com.giga.nexas.dto.bsdx.waz.Waz bsdxWaz = wazConverter.convert(bheWaz);
        bsdxWaz.setFileName(bheWaz.getFileName());
        bsdxWaz.setExtensionName("waz");
        log.info("✅ BSDX WAZ converted: {}, skills={}", bsdxWaz.getFileName(), bsdxWaz.getSkillList().size());

        // 3. 输出到同一目录
        String outputName = bheWazJsonName.replace(".waz.json", ".bsdx.waz.json");
        Path outputPath = resourceDir.resolve(outputName);
        String outputJson = cn.hutool.json.JSONUtil.toJsonPrettyStr(bsdxWaz);
        Files.writeString(outputPath, outputJson);
        log.info("✅ BSDX WAZ JSON written: {}", outputPath);

        // 4. 验证：用 BSDX 解析器读取生成的 JSON
        String bsdxJsonStr = Files.readString(outputPath);
        com.giga.nexas.dto.bsdx.waz.Waz verifyWaz = mapper.readValue(bsdxJsonStr, com.giga.nexas.dto.bsdx.waz.Waz.class);
        log.info("✅ BSDX WAZ verified: {}, skills={}", verifyWaz.getFileName(), verifyWaz.getSkillList().size());
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

    private void putIfPresent(Map<String, com.giga.nexas.dto.bsdx.spm.Spm> map, String key,
                              com.giga.nexas.dto.bsdx.spm.Spm value) {
        if (map == null || key == null || value == null) {
            return;
        }
        map.put(key, value);
    }

    // grp
    private static final Path BSDX_GRP_DIR = Paths.get("src/main/resources/game/bsdx/grp");
    private static final Path BHE_GRP_DIR = Paths.get("src/main/resources/game/bhe/grp");
    private Map<String, com.giga.nexas.dto.bsdx.grp.Grp> registerBsdxGrp() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.grp.Grp> grpMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.grp.Grp grp = (com.giga.nexas.dto.bsdx.grp.Grp) dto.getData();
                    grpMap.put(baseName, grp);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bsdxGrp: {}", fileName);
                }
            }
        }

        return grpMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.grp.Grp> registerBheGrp() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.grp.Grp> grpMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_GRP_DIR, "*.grp")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.grp.Grp grp = (com.giga.nexas.dto.bhe.grp.Grp) dto.getData();
                    grpMap.put(baseName, grp);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bheGrp: {}", fileName);
                }
            }
        }

        return grpMap;
    }

    // mek
    private static final Path BSDX_MEK_DIR = Paths.get("src/main/resources/game/bsdx/mek");
    private static final Path BHE_MEK_DIR = Paths.get("src/main/resources/game/bhe/mek");
    private Map<String, com.giga.nexas.dto.bsdx.mek.Mek> registerBsdxMek() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.mek.Mek> mekMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.mek.Mek mek = (com.giga.nexas.dto.bsdx.mek.Mek) dto.getData();
                    mekMap.put(baseName, mek);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bsdxMek: {}", fileName);
                }
            }
        }

        return mekMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.mek.Mek> registerBheMek() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.mek.Mek> mekMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_MEK_DIR, "*.mek")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.mek.Mek mek = (com.giga.nexas.dto.bhe.mek.Mek) dto.getData();
                    mekMap.put(baseName, mek);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bheMek: {}", fileName);
                }
            }
        }

        return mekMap;
    }

    // waz
    private static final Path BSDX_WAZ_DIR = Paths.get("src/main/resources/game/bsdx/waz");
    private static final Path BHE_WAZ_DIR = Paths.get("src/main/resources/game/bhe/waz");
    private Map<String, com.giga.nexas.dto.bsdx.waz.Waz> registerBsdxWaz() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> wazMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_WAZ_DIR, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.waz.Waz waz = (com.giga.nexas.dto.bsdx.waz.Waz) dto.getData();
                    wazMap.put(baseName, waz);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bsdxWaz: {}", fileName);
                }
            }
        }

        return wazMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.waz.Waz> registerBheWaz() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.waz.Waz> wazMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_WAZ_DIR, "*.waz")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.waz.Waz waz = (com.giga.nexas.dto.bhe.waz.Waz) dto.getData();
                    wazMap.put(baseName, waz);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bheWaz: {}", fileName);
                }
            }
        }

        return wazMap;
    }

    // spm
    // spm在多个版本中无差异，但已经确定2.0.0在bhe中多了关于hitbox的信息，变得更复杂了
    private static final Path BSDX_SPM_DIR = Paths.get("src/main/resources/game/bsdx/spm");
    private static final Path BHE_SPM_DIR = Paths.get("src/main/resources/game/bhe/spm");
    private Map<String, com.giga.nexas.dto.bsdx.spm.Spm> registerBsdxSpm() throws IOException {
        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> spmMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BSDX_SPM_DIR, "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bsdx.spm.Spm spm = (com.giga.nexas.dto.bsdx.spm.Spm) dto.getData();
                    spmMap.put(baseName, spm);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bsdxSpm: {}", fileName);
                }
            }
        }

        return spmMap;
    }
    private Map<String, com.giga.nexas.dto.bhe.spm.Spm> registerBheSpm() throws IOException {
        Map<String, com.giga.nexas.dto.bhe.spm.Spm> spmMap = new HashMap<>();

        try (DirectoryStream<Path> stream = Files.newDirectoryStream(BHE_SPM_DIR, "*.spm")) {
            for (Path path : stream) {
                String fileName = path.getFileName().toString();
                String baseName = fileName.substring(0, fileName.lastIndexOf('.')).toLowerCase();

                try {
                    ResponseDTO<?> dto = bheBinService.parse(path.toString(), "windows-31j");
                    com.giga.nexas.dto.bhe.spm.Spm spm = (com.giga.nexas.dto.bhe.spm.Spm) dto.getData();
                    spmMap.put(baseName, spm);
                } catch (Exception e) {
                    log.warn("❌ Failed to parse bheSpm: {}", fileName);
                }
            }
        }

        return spmMap;
    }

    // dat
    // dat无差别，全为csv
    private static final Path BSDX_DAT_DIR = Paths.get("src/main/resources/game/bsdx/dat");
    private Dat loadBsdxDat(String fileName) throws IOException {
        Path path = BSDX_DAT_DIR.resolve(fileName);
        try {
            ResponseDTO<?> dto = bsdxBinService.parse(path.toString(), "windows-31j");
            return (Dat) dto.getData();
        } catch (Exception e) {
            log.warn("❌ Failed to parse bsdxDat: {}", fileName);
            return null;
        }
    }

    @Test
    void outputClassName() {
        HashMap<String, Object> classMapBsdx = new HashMap<>();
        for (int i = 0; i < 72; i++) {
            com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject obj =
                    com.giga.nexas.dto.bsdx.waz.wazfactory.SkillInfoFactory.createEventObjectBsdx(i);
            log.info("{}", obj.getClass().getSimpleName());
            classMapBsdx.put(obj.getClass().getSimpleName(),null);
        }

        log.info("==========");

        for (int i = 0; i < 83; i++) {
            com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject obj =
                    com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory.createEventObjectBhe(i);
            log.info("{}", obj.getClass().getSimpleName());

        }

        log.info("==========");
        log.info("==========");
        log.info("==========");

        classMapBsdx.forEach((k, v) -> {log.info("{}", k);});

    }

}
