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
                if (skill.isEmpty()) {
                    writer.writeInt(0);
                    continue;
                }

                writer.writeInt(1);
                writer.writeNullTerminatedString(skill.getSkillNameJapanese());
                writer.writeNullTerminatedString(skill.getSkillNameEnglish());

                int phaseQuantity = skill.getPhaseQuantity();
                writer.writeInt(phaseQuantity);

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
    private void writeSkillPhaseInfo(Waz.Skill.SkillPhase skillPhase,
                                     BinaryWriter writer) throws IOException {

        // 固定72个SkillUnit
        for (int i = 0; i < 72; i++) {

            // todo bugs
            // 手动查找unitQuantity==i的SkillUnit
            SkillUnit skillUnit = null;
            for (SkillUnit su : skillPhase.getSkillUnitCollection()) {
                if (su.getUnitQuantity() == i) {
                    skillUnit = su;
                    break;
                }
            }

            // SkillInfoObject
            List<SkillInfoObject> infoObjects;
            if (skillUnit != null) {
                infoObjects = skillUnit.getSkillInfoObjectList();
            } else {
                infoObjects = new ArrayList<>();
            }

            writer.writeInt(infoObjects.size());
            for (SkillInfoObject info : infoObjects) {
                info.writeInfo(writer);
            }

            // SkillInfoUnknown
            List<SkillInfoUnknown> unknownObjects;
            if (skillUnit != null) {
                unknownObjects = skillUnit.getSkillInfoUnknownList();
            } else {
                unknownObjects = new java.util.ArrayList<>();
            }

            writer.writeInt(unknownObjects.size());
            for (SkillInfoUnknown un : unknownObjects) {
                un.writeInfo(writer);
            }
        }

    }

}
