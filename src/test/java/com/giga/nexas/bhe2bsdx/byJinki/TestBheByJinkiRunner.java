package com.giga.nexas.bhe2bsdx.byJinki;

import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.TsukuyomiTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.freja.FrejaTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.katou.KatouTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.misaki.MisakiTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.motoki.MotokiTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.naoto.NaotoTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.nagi.NagiTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.sou.SouTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.wilhelm.WilhelmTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.yuri.YuriTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuLayoutPolicy;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverrideSpec;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.freja.FrejaGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.freja.FrejaGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.katou.KatouGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.katou.KatouGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.misaki.MisakiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.motoki.MotokiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.naoto.NaotoGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.naoto.NaotoGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.nagi.NagiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.nagi.NagiGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.sou.SouGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.sou.SouGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.wilhelm.WilhelmGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.wilhelm.WilhelmGraftResult;
import com.giga.nexas.transfer.bhe2bsdx.model.yuri.YuriGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.yuri.YuriGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.v2.Jinki2BsdxTransferV2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
public class TestBheByJinkiRunner {

    @Test
    public void testRunBheByJinki() throws Exception {
        runBheByJinki(true);
    }

    @Test
    public void testRunNineCharacterRework() throws Exception {
        // 九角色参与本轮重做；原有Sou仍保留，避免旧配置中的机体编号失效。
        runBheByJinki(true);
    }

    private void runBheByJinki(boolean includeSou) throws Exception {
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();
        Path buildsRoot = projectRoot.resolve("target/bhe-playable-builds");
        Files.createDirectories(buildsRoot);
        Path buildRoot = buildsRoot.resolve("run_" + LocalDateTime.now(ZoneId.of("Asia/Tokyo"))
                .format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS")));
        // 每轮独立目录；禁止覆盖旧Update3.pac或继承已经吞掉CRT异常的旧EXE。
        Files.createDirectory(buildRoot);

        // 1. akao
        AkaoGraftRequest jinkiRequest = new AkaoGraftRequest();
        jinkiRequest.setExeOutputDir(buildRoot.resolve("akao"));
        // bsdx_game中的EXE带旧宽屏补丁，会让原版机体也偏离装备预览视野。
        jinkiRequest.setTargetExePath(projectRoot.resolve("src/main/resources/ida-reverse/BaldrSky.exe"));
        Path sourceExe = jinkiRequest.getTargetExePath().toAbsolutePath().normalize();
        String sourceExeSha256 = sha256(sourceExe);
        assertEquals("713cf605360a55a863f87bfbdea5453aaeeb2a33983ad4a005c9297e3a86e757", sourceExeSha256,
                "九角色构建必须使用已实测的800×600参考EXE，不能继承旧宽屏基线");
        jinkiRequest.setPatchMenuData(true);
        AkaoGraftResult jinkiResult = Jinki2BsdxTransferV2.process(jinkiRequest);

        Path jinkiGeneratedExeRelative = projectRoot.relativize(
                jinkiResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path jinkiGeneratedAssetDirRelative = projectRoot.relativize(
                jinkiResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );
        log.info("01 akao patch append end!");

        //-----------------------------------------------------------------------
        // BHE
        // -----------------------------------------------------------------------

        // 2. tsukuyomi
        TsukuyomiGraftRequest tsukuyomiRequest = new TsukuyomiGraftRequest();
        tsukuyomiRequest.setExeOutputDir(buildRoot.resolve("tsukuyomi"));
        tsukuyomiRequest.setJinkiGeneratedExePath(jinkiGeneratedExeRelative);
        tsukuyomiRequest.setJinkiGeneratedAssetDir(jinkiGeneratedAssetDirRelative);
        tsukuyomiRequest.setInheritedJinkiResult(jinkiResult);
        tsukuyomiRequest.setBheGameResourceRoot(null);
        tsukuyomiRequest.setTsukuyomiResourceDir(Paths.get("tsukuyomi"));
        tsukuyomiRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        tsukuyomiRequest.setPatchMenuData(true);
        tsukuyomiRequest.setPackUpdatePac(false);
        tsukuyomiRequest.setMenuOverrideSpec(tsukuyomiMenuOverrideSpec());

        TsukuyomiGraftResult tsukuyomiResult = TsukuyomiTransfer.process(tsukuyomiRequest);
        log.info("02 tsukuyomi patch append end!");

        Path yuriGeneratedExeRelative = projectRoot.relativize(
                tsukuyomiResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path yuriGeneratedAssetDirRelative = projectRoot.relativize(
                tsukuyomiResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 3. yuri
        YuriGraftRequest yuriRequest = new YuriGraftRequest();
        yuriRequest.setExeOutputDir(buildRoot.resolve("yuri"));
        yuriRequest.setJinkiGeneratedExePath(yuriGeneratedExeRelative);
        yuriRequest.setJinkiGeneratedAssetDir(yuriGeneratedAssetDirRelative);
        yuriRequest.setInheritedJinkiResult(jinkiResult);
        yuriRequest.setPreviousCharacterResult(tsukuyomiResult);
        yuriRequest.setBheGameResourceRoot(null);
        yuriRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        yuriRequest.setPatchMenuData(true);
        yuriRequest.setPackUpdatePac(false);
        yuriRequest.setMenuOverrideSpec(yuriMenuOverrideSpec());

        YuriGraftResult yuriResult = YuriTransfer.process(yuriRequest);
        log.info("03 yuri patch append end!");

        Path frejaGeneratedExeRelative = projectRoot.relativize(
                yuriResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path frejaGeneratedAssetDirRelative = projectRoot.relativize(
                yuriResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 4. freja
        FrejaGraftRequest frejaRequest = new FrejaGraftRequest();
        frejaRequest.setExeOutputDir(buildRoot.resolve("freja"));
        frejaRequest.setJinkiGeneratedExePath(frejaGeneratedExeRelative);
        frejaRequest.setJinkiGeneratedAssetDir(frejaGeneratedAssetDirRelative);
        frejaRequest.setInheritedJinkiResult(jinkiResult);
        frejaRequest.setPreviousCharacterResult(yuriResult);
        frejaRequest.setBheGameResourceRoot(null);
        frejaRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        frejaRequest.setPatchMenuData(true);
        frejaRequest.setPackUpdatePac(false);
        frejaRequest.setMenuOverrideSpec(frejaMenuOverrideSpec());

        FrejaGraftResult frejaResult = FrejaTransfer.process(frejaRequest);
        log.info("04 freja patch append end!");

        Path nagiGeneratedExeRelative = projectRoot.relativize(
                frejaResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path nagiGeneratedAssetDirRelative = projectRoot.relativize(
                frejaResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 5. nagi
        NagiGraftRequest nagiRequest = new NagiGraftRequest();
        nagiRequest.setExeOutputDir(buildRoot.resolve("nagi"));
        nagiRequest.setJinkiGeneratedExePath(nagiGeneratedExeRelative);
        nagiRequest.setJinkiGeneratedAssetDir(nagiGeneratedAssetDirRelative);
        nagiRequest.setInheritedJinkiResult(jinkiResult);
        nagiRequest.setPreviousCharacterResult(frejaResult);
        nagiRequest.setBheGameResourceRoot(null);
        nagiRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        nagiRequest.setPatchMenuData(true);
        nagiRequest.setPackUpdatePac(false);
        nagiRequest.setMenuOverrideSpec(nagiMenuOverrideSpec());

        NagiGraftResult nagiResult = NagiTransfer.process(nagiRequest);
        log.info("05 nagi patch append end!");

        Path misakiGeneratedExeRelative = projectRoot.relativize(
                nagiResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path misakiGeneratedAssetDirRelative = projectRoot.relativize(
                nagiResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 6. misaki
        MisakiGraftRequest misakiRequest = new MisakiGraftRequest();
        misakiRequest.setExeOutputDir(buildRoot.resolve("misaki"));
        misakiRequest.setJinkiGeneratedExePath(misakiGeneratedExeRelative);
        misakiRequest.setJinkiGeneratedAssetDir(misakiGeneratedAssetDirRelative);
        misakiRequest.setInheritedJinkiResult(jinkiResult);
        misakiRequest.setPreviousCharacterResult(nagiResult);
        misakiRequest.setBheGameResourceRoot(null);
        misakiRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        misakiRequest.setPatchMenuData(true);
        misakiRequest.setPackUpdatePac(false);
        misakiRequest.setMenuOverrideSpec(misakiMenuOverrideSpec());

        MisakiGraftResult misakiResult = MisakiTransfer.process(misakiRequest);
        log.info("06 misaki patch append end!");

        Path naotoGeneratedExeRelative = projectRoot.relativize(
                misakiResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path naotoGeneratedAssetDirRelative = projectRoot.relativize(
                misakiResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 7. naoto
        NaotoGraftRequest naotoRequest = new NaotoGraftRequest();
        naotoRequest.setExeOutputDir(buildRoot.resolve("naoto"));
        naotoRequest.setJinkiGeneratedExePath(naotoGeneratedExeRelative);
        naotoRequest.setJinkiGeneratedAssetDir(naotoGeneratedAssetDirRelative);
        naotoRequest.setInheritedJinkiResult(jinkiResult);
        naotoRequest.setPreviousCharacterResult(misakiResult);
        naotoRequest.setBheGameResourceRoot(null);
        naotoRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        naotoRequest.setPatchMenuData(true);
        naotoRequest.setPackUpdatePac(false);
        naotoRequest.setMenuOverrideSpec(naotoMenuOverrideSpec());

        NaotoGraftResult naotoResult = NaotoTransfer.process(naotoRequest);
        log.info("07 naoto patch append end!");

        Path katouGeneratedExeRelative = projectRoot.relativize(
                naotoResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path katouGeneratedAssetDirRelative = projectRoot.relativize(
                naotoResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 8. katou
        KatouGraftRequest katouRequest = new KatouGraftRequest();
        katouRequest.setExeOutputDir(buildRoot.resolve("katou"));
        katouRequest.setJinkiGeneratedExePath(katouGeneratedExeRelative);
        katouRequest.setJinkiGeneratedAssetDir(katouGeneratedAssetDirRelative);
        katouRequest.setInheritedJinkiResult(jinkiResult);
        katouRequest.setPreviousCharacterResult(naotoResult);
        katouRequest.setBheGameResourceRoot(null);
        katouRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        katouRequest.setPatchMenuData(true);
        katouRequest.setPackUpdatePac(false);
        katouRequest.setMenuOverrideSpec(katouMenuOverrideSpec());

        KatouGraftResult katouResult = KatouTransfer.process(katouRequest);
        log.info("08 katou patch append end!");

        Path wilhelmGeneratedExeRelative = projectRoot.relativize(
                katouResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path wilhelmGeneratedAssetDirRelative = projectRoot.relativize(
                katouResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 9. wilhelm
        WilhelmGraftRequest wilhelmRequest = new WilhelmGraftRequest();
        wilhelmRequest.setExeOutputDir(buildRoot.resolve("wilhelm"));
        wilhelmRequest.setJinkiGeneratedExePath(wilhelmGeneratedExeRelative);
        wilhelmRequest.setJinkiGeneratedAssetDir(wilhelmGeneratedAssetDirRelative);
        wilhelmRequest.setInheritedJinkiResult(jinkiResult);
        wilhelmRequest.setPreviousCharacterResult(katouResult);
        wilhelmRequest.setBheGameResourceRoot(null);
        wilhelmRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        wilhelmRequest.setPatchMenuData(true);
        wilhelmRequest.setPackUpdatePac(false);
        wilhelmRequest.setMenuOverrideSpec(wilhelmMenuOverrideSpec());

        WilhelmGraftResult wilhelmResult = WilhelmTransfer.process(wilhelmRequest);
        log.info("09 wilhelm patch append end!");

        Path motokiGeneratedExeRelative = projectRoot.relativize(
                wilhelmResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path motokiGeneratedAssetDirRelative = projectRoot.relativize(
                wilhelmResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 10. motoki
        MotokiGraftRequest motokiRequest = new MotokiGraftRequest();
        motokiRequest.setExeOutputDir(buildRoot.resolve("motoki"));
        motokiRequest.setJinkiGeneratedExePath(motokiGeneratedExeRelative);
        motokiRequest.setJinkiGeneratedAssetDir(motokiGeneratedAssetDirRelative);
        motokiRequest.setInheritedJinkiResult(jinkiResult);
        motokiRequest.setPreviousCharacterResult(wilhelmResult);
        motokiRequest.setBheGameResourceRoot(null);
        motokiRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        motokiRequest.setPatchMenuData(true);
        motokiRequest.setPackUpdatePac(!includeSou);
        motokiRequest.setMenuOverrideSpec(motokiMenuOverrideSpec());

        MotokiGraftResult motokiResult = MotokiTransfer.process(motokiRequest);
        log.info("10 motoki patch append end!");
        if (!includeSou) {
            writeBuildReceipt(projectRoot, buildRoot, motokiResult, false, sourceExe, sourceExeSha256);
            return;
        }

        Path souGeneratedExeRelative = projectRoot.relativize(
                motokiResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path souGeneratedAssetDirRelative = projectRoot.relativize(
                motokiResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        // 11. sou
        SouGraftRequest souRequest = new SouGraftRequest();
        souRequest.setExeOutputDir(buildRoot.resolve("sou"));
        souRequest.setJinkiGeneratedExePath(souGeneratedExeRelative);
        souRequest.setJinkiGeneratedAssetDir(souGeneratedAssetDirRelative);
        souRequest.setInheritedJinkiResult(jinkiResult);
        souRequest.setPreviousCharacterResult(motokiResult);
        souRequest.setBheGameResourceRoot(null);
        souRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        souRequest.setMapAppendEnabled(true);
        souRequest.setBheMapGroupPath(Paths.get("src/main/resources/game/bhe/grp/mapgroup.grp"));
        souRequest.setBheMapDir(Paths.get("src/main/resources/game/bhe/map"));
        souRequest.setBheStaticResourceRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        souRequest.setPatchMenuData(true);
        souRequest.setPackUpdatePac(true);
        souRequest.setMenuOverrideSpec(souMenuOverrideSpec());

        SouGraftResult souResult = SouTransfer.process(souRequest);
        log.info("11 sou patch append end!");
        writeBuildReceipt(projectRoot, buildRoot, souResult, true, sourceExe, sourceExeSha256);
    }

    private void writeBuildReceipt(Path projectRoot, Path buildRoot,
                                   TsukuyomiGraftResult result, boolean includeSou,
                                   Path sourceExe, String sourceExeSha256) throws Exception {
        assertEquals(sourceExeSha256, sha256(sourceExe), "构建期间源EXE不得变化");
        assertNotNull(result.getImportedAssetSet());
        assertNotNull(result.getPacPackPlan());
        assertTrue(result.getPacPackPlan().isPacked(), "最终阶段必须封包并完成现有解包字节验收");
        Path exe = result.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize();
        Path pac = result.getPacPackPlan().getOutputPacPath().toAbsolutePath().normalize();
        Map<String, Object> receipt = new LinkedHashMap<>();
        receipt.put("buildRoot", buildRoot.toString());
        receipt.put("sourceExe", sourceExe.toAbsolutePath().normalize().toString());
        receipt.put("sourceExeSha256", sourceExeSha256);
        receipt.put("assetRoot", result.getImportedAssetSet().getOutputRootDir().toAbsolutePath().toString());
        receipt.put("exe", exe.toString());
        receipt.put("pac", pac.toString());
        receipt.put("exeSha256", sha256(exe));
        receipt.put("pacSha256", sha256(pac));
        receipt.put("includeSou", includeSou);
        receipt.put("reworkedCharacters", List.of("tsukuyomi", "yuri", "freja", "nagi", "misaki",
                "naoto", "katou", "wilhelm", "motoki"));
        receipt.put("preservedCharacters", includeSou ? List.of("sou") : List.of());
        receipt.put("runtimeValidated", false);
        receipt.put("characters", includeSou
                ? List.of("tsukuyomi", "yuri", "freja", "nagi", "misaki", "naoto", "katou", "wilhelm", "motoki", "sou")
                : List.of("tsukuyomi", "yuri", "freja", "nagi", "misaki", "naoto", "katou", "wilhelm", "motoki"));
        String json = new ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(receipt)
                .replace("\r\n", "\n").replace("\n", "\r\n") + "\r\n";
        Files.writeString(buildRoot.resolve("build.json"), json, StandardCharsets.UTF_8);
        Files.writeString(projectRoot.resolve("target/bhe-playable-build-latest.json"), json, StandardCharsets.UTF_8);
        log.info("构建记录已写出：{}；游戏效果仍需单独验收", buildRoot.resolve("build.json"));
    }

    private static String sha256(Path path) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var input = Files.newInputStream(path)) {
            byte[] buffer = new byte[1024 * 1024];
            int count;
            while ((count = input.read(buffer)) != -1) digest.update(buffer, 0, count);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    // 2. tsukuyomi
    private MenuOverrideSpec tsukuyomiMenuOverrideSpec() {
        return new MenuOverrideSpec(
                25,
                24,
                List.of(
                        "MOD_001_HELL_TSUKUYOMI_001.png",
                        "MOD_001_HELL_MEKA_TSUKUYOMI_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Sakurabi_001.png",
                        "MOD_001_SelectMekaMenuMeka_Sakurabi_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 3. yuri
    private MenuOverrideSpec yuriMenuOverrideSpec() {
        return new MenuOverrideSpec(
                26,
                25,
                List.of(
                        "MOD_001_HELL_YURI_001.png",
                        "MOD_001_HELL_MEKA_YURI_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_GrimmTail_001.png",
                        "MOD_001_SelectMekaMenuMeka_GrimmTail_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 4. freja
    private MenuOverrideSpec frejaMenuOverrideSpec() {
        return new MenuOverrideSpec(
                27,
                26,
                List.of(
                        "MOD_001_HELL_FREJA_001.png",
                        "MOD_001_HELL_MEKA_FREJA_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Strjeljets_001.png",
                        "MOD_001_SelectMekaMenuMeka_Strjeljets_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 5. nagi
    private MenuOverrideSpec nagiMenuOverrideSpec() {
        return new MenuOverrideSpec(
                28,
                27,
                List.of(
                        "MOD_001_HELL_NAGI_001.png",
                        "MOD_001_HELL_MEKA_NAGI_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_EdelWeiss_001.png",
                        "MOD_001_SelectMekaMenuMeka_EdelWeiss_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 6. misaki
    private MenuOverrideSpec misakiMenuOverrideSpec() {
        return new MenuOverrideSpec(
                29,
                28,
                List.of(
                        "MOD_001_HELL_MISAKI_001.png",
                        "MOD_001_HELL_MEKA_MISAKI_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Misaki_001.png",
                        "MOD_001_SelectMekaMenuMeka_Misaki_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 7. naoto
    private MenuOverrideSpec naotoMenuOverrideSpec() {
        return new MenuOverrideSpec(
                30,
                29,
                List.of(
                        "MOD_001_HELL_NAOTO_001.png",
                        "MOD_001_HELL_MEKA_NAOTO_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Shiden_001.png",
                        "MOD_001_SelectMekaMenuMeka_Shiden_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 8. katou
    private MenuOverrideSpec katouMenuOverrideSpec() {
        return new MenuOverrideSpec(
                31,
                30,
                List.of(
                        "MOD_001_HELL_KATOU_001.png",
                        "MOD_001_HELL_MEKA_KATOU_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_MorgueMan_001.png",
                        "MOD_001_SelectMekaMenuMeka_MorgueMan_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 9. wilhelm
    private MenuOverrideSpec wilhelmMenuOverrideSpec() {
        return new MenuOverrideSpec(
                32,
                31,
                List.of(
                        "MOD_001_HELL_WILHELM_001.png",
                        "MOD_001_HELL_MEKA_WILHELM_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Gestoeber_001.png",
                        "MOD_001_SelectMekaMenuMeka_Gestoeber_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 10. motoki
    private MenuOverrideSpec motokiMenuOverrideSpec() {
        return new MenuOverrideSpec(
                33,
                32,
                List.of(
                        "MOD_001_HELL_MOTOKI_001.png",
                        "MOD_001_HELL_MEKA_MOTOKI_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Shinatsuhiko_001.png",
                        "MOD_001_SelectMekaMenuMeka_Shinatsuhiko_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

    // 11. sou
    private MenuOverrideSpec souMenuOverrideSpec() {
        return new MenuOverrideSpec(
                34,
                33,
                List.of(
                        "MOD_001_HELL_SOU_001.png",
                        "MOD_001_HELL_MEKA_SOU_001.png"
                ),
                List.of(
                        "MOD_001_SelectMekaMenuMeka_Schwertiger_001.png",
                        "MOD_001_SelectMekaMenuMeka_Schwertiger_002.png"
                ),
                MenuLayoutPolicy.HELL_PILOT_TOP_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }

}
