package com.giga.nexas.jinki;

import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.CEventVoice;
import com.giga.nexas.dto.bsdx.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftRequest;
import com.giga.nexas.transfer.jinki2bsdx.model.AkaoGraftResult;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 直接跑一遍当前 jinki2bsdx runner，
 * 确认流程保持在“复用第 25 槽 + 复用 mekaIndex=32 + 禁用 103 扩容 patch”这条线上。
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
        Assertions.assertNotNull(result.getGrpAppendPlan());
        Assertions.assertNotNull(result.getReboundAkaoMek());
        Assertions.assertNotNull(result.getReboundAkaoWaz());
        Assertions.assertNotNull(result.getImportedAssetSet());
        Assertions.assertNotNull(result.getExePatchPlan());
        Assertions.assertNotNull(result.getPacPackPlan());

        Assertions.assertFalse(result.getJinkiPackage().getSpmByFileName().isEmpty());
        Assertions.assertFalse(result.getJinkiPackage().getWazByFileName().isEmpty());
        Assertions.assertFalse(result.getImportPlan().getRequiredWazFiles().isEmpty());
        Assertions.assertFalse(result.getImportPlan().getRequiredSpmFiles().isEmpty());

        Assertions.assertEquals(103, result.getGrpAppendPlan().getMekaGroupIndex());
        Assertions.assertTrue(result.getGrpAppendPlan().getWazaGroupIndex() >= 0);
        Assertions.assertTrue(result.getGrpAppendPlan().getSpriteGroupIndex() >= 0);
        Assertions.assertTrue(result.getGrpAppendPlan().getBatVoiceGroupIndex() >= 0);

        Assertions.assertNotNull(result.getSyncedProgramMaterial());
        Assertions.assertEquals(139, result.getSyncedProgramMaterial().getArray1().size());
        Assertions.assertEquals(38, result.getSyncedProgramMaterial().getArray2().size());
        Assertions.assertEquals(31, result.getSyncedProgramMaterial().getArray3().size());

        Assertions.assertEquals(110, result.getReboundAkaoMek().getMekBasicInfo().getWazFileSequence());
        Assertions.assertEquals(138, result.getReboundAkaoMek().getMekBasicInfo().getSpmFileSequence());
        Assertions.assertFalse(result.getReboundAkaoWaz().getSkillList().isEmpty());
        Assertions.assertFalse(collectVoiceGroupIndices(result.getReboundAkaoWaz()).isEmpty());
        Assertions.assertTrue(collectVoiceGroupIndices(result.getReboundAkaoWaz()).stream().allMatch(index -> index == 30));

        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir()));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("ProgramMaterial.grp")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("Meka.dat")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("MekaPilot.dat")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("SelectMekaMenu.dat")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("MekaPilot.spm")));
        Assertions.assertTrue(Files.exists(result.getImportedAssetSet().getOutputRootDir().resolve("SelectMekaMenuMeka.spm")));

        int mekaIdx = result.getGrpAppendPlan().getMekaGroupIndex();
        Assertions.assertTrue(containsSingleColumnRow(result.getPatchedMekaPilotDat(), mekaIdx));
        Assertions.assertEquals(mekaIdx, asInt(result.getPatchedSelectMekaMenuDat().getData().get(24).get(0)));
        Assertions.assertEquals(
                asInt(result.getBsdxBaseline().getSelectMekaMenuDat().getData().get(24).get(2)),
                asInt(result.getPatchedSelectMekaMenuDat().getData().get(24).get(2))
        );
        Assertions.assertEquals(
                result.getBsdxBaseline().getSelectMekaMenuDat().getData().size(),
                result.getPatchedSelectMekaMenuDat().getData().size()
        );
        Assertions.assertEquals(
                result.getBsdxBaseline().getMekaPilotSpm().getAnimData().size(),
                result.getPatchedMekaPilotSpm().getAnimData().size()
        );
        Assertions.assertEquals(
                result.getBsdxBaseline().getSelectMekaMenuMekaSpm().getAnimData().size(),
                result.getPatchedSelectMekaMenuMekaSpm().getAnimData().size()
        );

        Assertions.assertFalse(result.getExePatchPlan().isPatched());
        Assertions.assertEquals(104, result.getExePatchPlan().getRequiredMekaCapacity());
        Assertions.assertTrue(result.getExePatchPlan().getRequiredWazaCapacity() >= 111);
        Assertions.assertTrue(result.getExePatchPlan().getRequiredSpriteCapacity() >= 139);
        Assertions.assertTrue(result.getExePatchPlan().getRequiredBatVoiceCapacity() >= 31);
        Assertions.assertTrue(result.getExePatchPlan().getRequiredSeCapacity() >= 38);
        Assertions.assertEquals(70, result.getExePatchPlan().getRequiredSelectMekaMenuRows());
        Assertions.assertTrue(result.getExePatchPlan().getTargetOffsets().isEmpty());
        Assertions.assertTrue(Files.exists(result.getExePatchPlan().getOutputExePath()));

        Assertions.assertTrue(result.getPacPackPlan().isPacked());
        Assertions.assertTrue(Files.exists(result.getPacPackPlan().getOutputPacPath()));
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

    private boolean containsSingleColumnRow(com.giga.nexas.dto.bsdx.dat.Dat dat, int expectedFirst) {
        if (dat == null || dat.getData() == null) {
            return false;
        }
        return dat.getData().stream()
                .filter(Objects::nonNull)
                .anyMatch(row -> !row.isEmpty() && expectedFirst == asInt(row.get(0)));
    }

    private int asInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }
}
