package com.giga.nexasdxeditor.dto.bsdx.waz.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexasdxeditor.dto.bsdx.BsdxGenerator;
import com.giga.nexasdxeditor.dto.bsdx.waz.Waz;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.SkillUnit;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexasdxeditor.io.BinaryWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WazGenerator implements BsdxGenerator<Waz> {

    @Override
    public String supportExtension() {
        return "waz";
    }

    @Override
    public void generate(String path, Waz waz, String charsetName) throws IOException {

        FileUtil.mkdir(FileUtil.getParent(path, 1));

        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path));
             BinaryWriter writer = new BinaryWriter(outputStream, charsetName)) {

            List<Waz.Skill> skills = waz.getSkillList();
            for (Waz.Skill skill : skills) {
                if (skill.isEmpty() || skill.getPhaseQuantity() == null) {
                    writer.writeInt(0);
                    continue;
                }

                writer.writeInt(1);
                writer.writeNullTerminatedString(skill.getSkillNameJapanese());
                writer.writeNullTerminatedString(skill.getSkillNameEnglish());

                try {
                    int phaseQuantity = skill.getPhaseQuantity();
                    writer.writeInt(phaseQuantity);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }

                List<Waz.Skill.SkillPhase> phaseInfoList = skill.getPhasesInfo();
                for (Waz.Skill.SkillPhase skillPhase : phaseInfoList) {
                    writeSkillPhaseInfo(skillPhase, writer);
                }

                List<Waz.Skill.SkillSuffix> suffixList = skill.getSkillSuffixList();
                writer.writeInt(suffixList.size());
                for (Waz.Skill.SkillSuffix suffix : suffixList) {
                    writer.writeInt(suffix.getInt1());
                    writer.writeInt(suffix.getInt2());
                }
            }

        } catch (Exception e) {
            log.info("path === {}", path);
            log.info("error === {}", e.getMessage());
            throw e;
        }
    }

    // 写入技能阶段信息
    private void writeSkillPhaseInfo(Waz.Skill.SkillPhase skillPhase, BinaryWriter writer) throws IOException {

        List<SkillUnit> skillUnitCollection = skillPhase.getSkillUnitCollection();

        for (int i = 0; i < 72; i++) {
            SkillUnit matchedUnit = null;
            for (SkillUnit unit : skillUnitCollection) {
                if (unit.getUnitQuantity() == i) {
                    matchedUnit = unit;
                    break;
                }
            }

            List<SkillInfoObject> skillInfoObjectList;
            if (matchedUnit != null) {
                skillInfoObjectList = matchedUnit.getSkillInfoObjectList();
            } else {
                skillInfoObjectList = new ArrayList<>();
            }

            writer.writeInt(skillInfoObjectList.size());
            for (SkillInfoObject obj : skillInfoObjectList) {
                obj.writeInfo(writer);
            }

            List<SkillInfoUnknown> skillInfoUnknownList;
            if (matchedUnit != null) {
                skillInfoUnknownList = matchedUnit.getSkillInfoUnknownList();
            } else {
                skillInfoUnknownList = new ArrayList<>();
            }

            writer.writeInt(skillInfoUnknownList.size());
            for (SkillInfoUnknown unknown : skillInfoUnknownList) {
                unknown.writeInfo(writer);
            }
        }
    }

}
