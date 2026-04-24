package com.giga.nexas.dto.clarias.waz.generator;

import cn.hutool.core.io.FileUtil;
import com.giga.nexas.dto.clarias.ClariasGenerator;
import com.giga.nexas.dto.clarias.waz.Waz;
import com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.io.BinaryWriter;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class WazGenerator implements ClariasGenerator<Waz> {
    @Override
    public String supportExtension() {
        return "waz";
    }

    @Override
    public void generate(String path, Waz waz, String charset) throws IOException {
        FileUtil.mkdir(FileUtil.getParent(path, 1));
        try (OutputStream outputStream = new BufferedOutputStream(new FileOutputStream(path));
             BinaryWriter writer = new BinaryWriter(outputStream, charset)) {
for (Waz.Skill skill : waz.getSkillList()) {
                if (skill.isEmpty() || skill.getPhaseQuantity() == null) {
                    writer.writeInt(0);
                    continue;
                }
                writer.writeInt(1);
                writer.writeNullTerminatedString(skill.getSkillNameJapanese());
                writer.writeNullTerminatedString(skill.getSkillNameEnglish());
                writer.writeInt(skill.getPhaseQuantity());
                for (Waz.Skill.SkillPhase phase : skill.getPhasesInfo()) {
                    writeSkillPhaseInfo(phase, writer);
                }
                writer.writeInt(skill.getSkillSuffixList().size());
                for (Waz.Skill.SkillSuffix suffix : skill.getSkillSuffixList()) {
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

    private void writeSkillPhaseInfo(Waz.Skill.SkillPhase phase, BinaryWriter writer) throws IOException {
        List<SkillUnit> units = phase.getSkillUnitCollection();
        for (int i = 0; i < SkillInfoFactory.SLOT_COUNT; i++) {
SkillUnit matched = null;
            for (SkillUnit unit : units) {
                if (unit.getUnitQuantity() != null && unit.getUnitQuantity() == i) {
                    matched = unit;
                    break;
                }
            }
            List<SkillInfoObject> objects = matched == null ? new ArrayList<>() : matched.getSkillInfoObjectList();
            writer.writeInt(objects.size());
            for (SkillInfoObject object : objects) {
                object.writeInfo(writer);
            }
            List<SkillInfoUnknown> unknowns = matched == null ? new ArrayList<>() : matched.getSkillInfoUnknownList();
            writer.writeInt(unknowns.size());
            for (SkillInfoUnknown unknown : unknowns) {
                unknown.writeInfo(writer);
            }
        }
        writer.writeNullTerminatedString(phase.getPhaseTail());
    }
}
