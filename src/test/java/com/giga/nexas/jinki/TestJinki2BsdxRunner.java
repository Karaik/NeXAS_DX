package com.giga.nexas.jinki;

import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;

/**
 * 用于从测试侧直接启动 JINKI -> BSDX 的单机体 runner。
 */
public class TestJinki2BsdxRunner {

    @Test
    public void testRunAkaoGraftPipeline() {
        AkaoGraftResult result = new Jinki2BsdxSingleRunner().run();

        Assertions.assertNotNull(result);
        Assertions.assertNotNull(result.getJinkiPackage());
        Assertions.assertNotNull(result.getBsdxBaseline());
        Assertions.assertNotNull(result.getImportPlan());

        Assertions.assertFalse(result.getJinkiPackage().getSpmByFileName().isEmpty());
        Assertions.assertFalse(result.getJinkiPackage().getWazByFileName().isEmpty());
        Assertions.assertFalse(result.getBsdxBaseline().getSpmByFileName().isEmpty());
        Assertions.assertFalse(result.getBsdxBaseline().getWazByFileName().isEmpty());

        Assertions.assertFalse(result.getImportPlan().getRequiredMekFiles().isEmpty());
        Assertions.assertFalse(result.getImportPlan().getRequiredWazFiles().isEmpty());
        Assertions.assertFalse(result.getImportPlan().getRequiredSpmFiles().isEmpty());
        Assertions.assertFalse(result.getImportPlan().getGrpAppendTargets().isEmpty());

        Assertions.assertNotNull(result.getGrpAppendPlan());
        Assertions.assertTrue(result.getGrpAppendPlan().getMekaGroupIndex() >= 0);
        Assertions.assertTrue(result.getGrpAppendPlan().getWazaGroupIndex() >= 0);
        Assertions.assertTrue(result.getGrpAppendPlan().getSpriteGroupIndex() >= 0);
        Assertions.assertTrue(result.getGrpAppendPlan().getBatVoiceGroupIndex() >= 0);

        Assertions.assertNotNull(result.getExePatchPlan());
        Assertions.assertTrue(result.getExePatchPlan().isPatched());
        Assertions.assertEquals(104, result.getExePatchPlan().getRequiredMekaCapacity());
        Assertions.assertNotNull(result.getExePatchPlan().getOutputExePath());
        Assertions.assertTrue(Files.exists(result.getExePatchPlan().getOutputExePath()));
        Assertions.assertFalse(result.getExePatchPlan().getTargetOffsets().isEmpty());
    }
}
