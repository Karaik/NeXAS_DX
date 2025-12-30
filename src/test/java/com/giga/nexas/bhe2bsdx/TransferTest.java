package com.giga.nexas.bhe2bsdx;

import com.giga.nexas.bhe2bsdx.steps.TransMeka;
import com.giga.nexas.bhe2bsdx.steps.TransMekaOutputWriter;
import com.giga.nexas.bhe2bsdx.steps.TransMekaResult;
import com.giga.nexas.dto.ResponseDTO;

import com.giga.nexas.service.BheBinService;
import com.giga.nexas.service.BsdxBinService;
import com.giga.nexas.dto.bsdx.dat.Dat;
import com.giga.nexas.util.PacUtil;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

public class TransferTest {

    private static final Path OUTPUT_DIR = Paths.get("src/main/resources/testBhe");

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

                mekaPilotSpm,
                selectMekaMenuMekaSpm,
                selectMekaMenuDat);

        // 3.回写到 BSDX Map（保持内存一致），同时只输出本次变更，避免写出全部 spm
        if (result != null) {
            if (result.getBsdxMeka() != null) {
                bsdxMek.put(tsukuyomiKey, result.getBsdxMeka());
            }
            if (result.getBsdxWaz() != null) {
                bsdxWaz.put(tsukuyomiKey, result.getBsdxWaz());
            }
            if (result.getBsdxSpm() != null) {
                bsdxSpm.put(tsukuyomiKey, result.getBsdxSpm());
            }
            if (result.getBsdxCSpm() != null) {
                bsdxSpm.put(cTsukuyomiKey, result.getBsdxCSpm());
            }
            if (result.getBsdxSSpm() != null) {
                bsdxSpm.put(sTsukuyomiKey, result.getBsdxSSpm());
            }
            if (result.getBsdxGSpm() != null) {
                bsdxSpm.put(gTsukuyomiKey, result.getBsdxGSpm());
            }
            if (result.getBsdxMSpm() != null) {
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
            outputMek.put(tsukuyomiKey, result.getBsdxMeka());
        }

        Map<String, com.giga.nexas.dto.bsdx.waz.Waz> outputWaz = new HashMap<>();
        if (result != null && result.getBsdxWaz() != null) {
            outputWaz.put(tsukuyomiKey, result.getBsdxWaz());
        }

        Map<String, com.giga.nexas.dto.bsdx.spm.Spm> outputSpm = new HashMap<>();
        if (result != null && result.getBsdxSpm() != null) {
            outputSpm.put(tsukuyomiKey, result.getBsdxSpm());
        }
        if (result != null && result.getBsdxCSpm() != null) {
            outputSpm.put(cTsukuyomiKey, result.getBsdxCSpm());
        }
        if (result != null && result.getBsdxSSpm() != null) {
            outputSpm.put(sTsukuyomiKey, result.getBsdxSSpm());
        }
        if (result != null && result.getBsdxGSpm() != null) {
            outputSpm.put(gTsukuyomiKey, result.getBsdxGSpm());
        }
        if (result != null && result.getBsdxMSpm() != null) {
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

        // 打包
        log.info("outputPath === {}", PacUtil.pack(outputPath, "7"));
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
