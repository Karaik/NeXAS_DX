package com.giga.nexas.jinki;

import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

/**
 * 用于从测试侧直接启动 JINKI -> BSDX 的单机体 runner。
 */
public class TestJinki2BsdxRunner {

    @Test
    public void testRunAkaoGraftPipeline() {
        AkaoGraftRequest request = new AkaoGraftRequest();
        request.setPatchMenuData(true);

        AkaoGraftResult result = new Jinki2BsdxSingleRunner().run(request);

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

        Assertions.assertNotNull(result.getSyncedProgramMaterial());
        Assertions.assertEquals(139, result.getSyncedProgramMaterial().getArray1().size());
        Assertions.assertEquals(38, result.getSyncedProgramMaterial().getArray2().size());
        Assertions.assertEquals(31, result.getSyncedProgramMaterial().getArray3().size());

        Assertions.assertNotNull(result.getReboundAkaoMek());
        Assertions.assertNotNull(result.getReboundAkaoMek().getMekBasicInfo());
        Assertions.assertEquals(110, result.getReboundAkaoMek().getMekBasicInfo().getWazFileSequence());
        Assertions.assertEquals(138, result.getReboundAkaoMek().getMekBasicInfo().getSpmFileSequence());

        Assertions.assertNotNull(result.getReboundAkaoWaz());
        Assertions.assertFalse(result.getReboundAkaoWaz().getSkillList().isEmpty());
        Assertions.assertFalse(collectVoiceGroupIndices(result.getReboundAkaoWaz()).isEmpty());
        Assertions.assertTrue(
                collectVoiceGroupIndices(result.getReboundAkaoWaz()).stream().allMatch(index -> index == 30)
        );

        Assertions.assertNotNull(result.getImportedAssetSet());
        Assertions.assertNotNull(result.getImportedAssetSet().getOutputRootDir());
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir()));
        Assertions.assertFalse(result.getImportedAssetSet().getGeneratedDatFiles().isEmpty());
        Assertions.assertFalse(result.getImportedAssetSet().getGeneratedMekFiles().isEmpty());
        Assertions.assertFalse(result.getImportedAssetSet().getGeneratedWazFiles().isEmpty());
        Assertions.assertFalse(result.getImportedAssetSet().getCopiedSpmFiles().isEmpty());

        Assertions.assertNotNull(result.getPatchedMekaDat());
        Assertions.assertNotNull(result.getPatchedMekaPilotDat());

        Assertions.assertNotNull(result.getExePatchPlan());
        Assertions.assertTrue(result.getExePatchPlan().isPatched());
        Assertions.assertEquals(104, result.getExePatchPlan().getRequiredMekaCapacity());
        Assertions.assertNotNull(result.getExePatchPlan().getOutputExePath());
        Assertions.assertTrue(Files.exists(result.getExePatchPlan().getOutputExePath()));
        Assertions.assertFalse(result.getExePatchPlan().getTargetOffsets().isEmpty());
    }

    private List<Integer> collectVoiceGroupIndices(Waz waz) {
        List<Integer> indices = new ArrayList<>();
        if (waz == null || waz.getSkillList() == null) {
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
                for (SkillUnit unit : phase.getSkillUnitCollection()) {
                    if (unit == null || unit.getSkillInfoObjectList() == null) {
                        continue;
                    }
                    for (SkillInfoObject object : unit.getSkillInfoObjectList()) {
                        collectVoiceGroupIndicesFromObject(object, indices);
                    }
                }
            }
        }
        return indices;
    }

    private void collectVoiceGroupIndicesFromObject(SkillInfoObject object, List<Integer> indices) {
        if (object == null) {
            return;
        }

        if (object instanceof CEventVoice voice) {
            if (voice.getByteDataList() == null) {
                return;
            }
            for (byte[] bytes : voice.getByteDataList()) {
                if (bytes == null || bytes.length < 4) {
                    continue;
                }
                indices.add(readLittleEndianInt(bytes, 0));
            }
        }
    }

    private int readLittleEndianInt(byte[] bytes, int offset) {
        return (bytes[offset] & 0xFF)
                | ((bytes[offset + 1] & 0xFF) << 8)
                | ((bytes[offset + 2] & 0xFF) << 16)
                | ((bytes[offset + 3] & 0xFF) << 24);
    }
}
