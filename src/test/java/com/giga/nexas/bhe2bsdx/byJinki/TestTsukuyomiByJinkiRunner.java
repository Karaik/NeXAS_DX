package com.giga.nexas.bhe2bsdx.byJinki;

import com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.TsukuyomiTransfer;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftRequest;
import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.transfer.jinki2bsdx.v2.Jinki2BsdxTransferV2;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
public class TestTsukuyomiByJinkiRunner {

    @Test
    public void testRunTsukuyomiByJinki() {
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
        tsukuyomiRequest.setBheGameResourceRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_game"));
        tsukuyomiRequest.setTsukuyomiResourceDir(Paths.get("tsukuyomi"));
        tsukuyomiRequest.setExternalStaticAssetRoot(Paths.get("D:/BDY/NeXAS_Resources/bhe_resources"));
        tsukuyomiRequest.setPatchMenuData(true);

        TsukuyomiGraftResult tsukuyomiResult = TsukuyomiTransfer.process(tsukuyomiRequest);

        log.info("jinki generated exe (relative) = {}", jinkiGeneratedExeRelative);
        log.info("jinki generated asset dir (relative) = {}", jinkiGeneratedAssetDirRelative);
        log.info("bhe game resource root = {}", tsukuyomiRequest.getBheGameResourceRoot());
        log.info("tsukuyomi resource dir (relative) = {}", tsukuyomiRequest.getTsukuyomiResourceDir());
        log.info("tsukuyomi output root = {}", tsukuyomiResult == null || tsukuyomiResult.getImportedAssetSet() == null
                ? null
                : tsukuyomiResult.getImportedAssetSet().getOutputRootDir());
    }
}
