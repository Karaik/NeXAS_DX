package com.giga.nexas.dto.bhe.waz.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.bhe.BheGenerator;
import com.giga.nexas.dto.bhe.waz.Waz;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.io.BinaryWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WazGenerator implements BheGenerator<Waz> {

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
                // parser 中 flag==0 时不会读取任何字段, phaseQuantity 保持 null; 以此作为空 Skill 的存在性标记
                if (skill.getPhaseQuantity() == null) {
                    writer.writeInt(0);
                    continue;
                }

                writer.writeInt(1);
                writer.writeNullTerminatedString(skill.getSkillNameJapanese());
                writer.writeNullTerminatedString(skill.getSkillNameEnglish());

                writer.writeInt(skill.getPhaseQuantity());

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

    // 写入技能阶段信息 (BHE 固定 83 个 slot)
    private void writeSkillPhaseInfo(Waz.Skill.SkillPhase skillPhase, BinaryWriter writer) throws IOException {

        List<SkillUnit> skillUnitCollection = skillPhase.getSkillUnitCollection();

        for (int i = 0; i < 83; i++) {
            SkillUnit matchedUnit = null;
            for (SkillUnit unit : skillUnitCollection) {
                if (unit.getUnitQuantity() != null && unit.getUnitQuantity() == i) {
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
