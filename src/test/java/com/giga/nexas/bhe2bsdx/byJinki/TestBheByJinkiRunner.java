package com.giga.nexas.bhe2bsdx.byJinki;

import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.TsukuyomiTransfer;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuLayoutPolicy;
import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.menu.MenuOverrideSpec;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.v2.Jinki2BsdxTransferV2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

@Slf4j
public class TestBheByJinkiRunner {

    @Test
    public void testRunBheByJinki() {
        Path projectRoot = Paths.get("").toAbsolutePath().normalize();

        // 1. akao
        AkaoGraftRequest jinkiRequest = new AkaoGraftRequest();
        jinkiRequest.setPatchMenuData(true);
        AkaoGraftResult jinkiResult = Jinki2BsdxTransferV2.process(jinkiRequest);

        Path jinkiGeneratedExeRelative = projectRoot.relativize(
                jinkiResult.getExePatchPlan().getOutputExePath().toAbsolutePath().normalize()
        );
        Path jinkiGeneratedAssetDirRelative = projectRoot.relativize(
                jinkiResult.getImportedAssetSet().getOutputRootDir().toAbsolutePath().normalize()
        );

        //-----------------------------------------------------------------------
        // BHE
        // -----------------------------------------------------------------------

        // 2. tsukuyomi
        TsukuyomiGraftRequest tsukuyomiRequest = new TsukuyomiGraftRequest();
        tsukuyomiRequest.setJinkiGeneratedExePath(jinkiGeneratedExeRelative);
        tsukuyomiRequest.setJinkiGeneratedAssetDir(jinkiGeneratedAssetDirRelative);
        tsukuyomiRequest.setInheritedJinkiResult(jinkiResult);
        tsukuyomiRequest.setBheGameResourceRoot(null);
        tsukuyomiRequest.setTsukuyomiResourceDir(Paths.get("tsukuyomi"));
        tsukuyomiRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        tsukuyomiRequest.setPatchMenuData(true);
        tsukuyomiRequest.setMenuOverrideSpec(tsukuyomiMenuOverrideSpec());

        TsukuyomiGraftResult tsukuyomiResult = TsukuyomiTransfer.process(tsukuyomiRequest);
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
                MenuLayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR,
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
                MenuLayoutPolicy.MEKA_PILOT_MEDIAN_ANCHOR,
                MenuLayoutPolicy.ORIGIN_CENTER
        );
    }
}
