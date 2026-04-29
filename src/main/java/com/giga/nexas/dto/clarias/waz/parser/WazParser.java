package com.giga.nexas.dto.clarias.waz.parser;

import com.giga.nexas.dto.clarias.ClariasParser;
import com.giga.nexas.dto.clarias.waz.Waz;
import com.giga.nexas.dto.clarias.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.io.BinaryReader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class WazParser implements ClariasParser<Waz> {

    @Override
    public String supportExtension() {
        return "waz";
    }

    @Override
    public Waz parse(byte[] bytes, String fileName, String charset) {
        Waz waz = new Waz(fileName);
        List<Waz.Skill> wazBlockList = waz.getSkillList();
        BinaryReader reader = new BinaryReader(bytes, charset);

        try {
            while (reader.hasRemaining()) {
                Waz.Skill skill = new Waz.Skill();

                int flag = reader.readInt();
                if (flag == 0) {
                    wazBlockList.add(skill);
                    continue;
                }

                skill.setSkillNameJapanese(reader.readNullTerminatedString());
                skill.setSkillNameEnglish(reader.readNullTerminatedString());

                int phaseQuantity = reader.readInt();
                skill.setPhaseQuantity(phaseQuantity);

                List<Waz.Skill.SkillPhase> phaseInfoList = skill.getPhasesInfo();
                for (int i = 0; i < phaseQuantity; i++) {
                    Waz.Skill.SkillPhase skillPhase = new Waz.Skill.SkillPhase();
                    parseSkillPhaseInfo(skillPhase, reader);
                    phaseInfoList.add(skillPhase);
                }

                int countSuffix = reader.readInt();
                List<Waz.Skill.SkillSuffix> skillSuffixList = skill.getSkillSuffixList();
                for (int i = 0; i < countSuffix; i++) {
                    Waz.Skill.SkillSuffix skillSuffix = new Waz.Skill.SkillSuffix();
                    skillSuffix.setInt1(reader.readInt());
                    skillSuffix.setInt2(reader.readInt());
                    skillSuffixList.add(skillSuffix);
                }

                wazBlockList.add(skill);
            }
        } catch (Exception e) {
            log.info("filename === {}", fileName);
            log.info("error === {}", e.getMessage());
            throw e;
        }
        return waz;
    }

    private void parseSkillPhaseInfo(Waz.Skill.SkillPhase skillPhase, BinaryReader reader) {
        List<SkillUnit> skillUnitCollection = skillPhase.getSkillUnitCollection();

        for (int i = 0; i < 112; i++) { //diff
            if ((SkillInfoFactory.SKILL_INFO_TYPE_ENTRIES_CLARIAS[i].getFlags() & 2) != 0) { //diff
                continue; //diff
            } //diff
            SkillUnit skillUnit = new SkillUnit(i, SkillInfoFactory.SKILL_INFO_TYPE_ENTRIES_CLARIAS[i].getDescription());

            List<SkillInfoObject> skillInfoObjectList = skillUnit.getSkillInfoObjectList();
            int count1 = reader.readInt();
            for (int j = 0; j < count1; j++) {
                try {
                    SkillInfoObject eventObject = SkillInfoFactory.createEventObjectClarias(i);
                    eventObject.setSlotNum(i);
                    eventObject.readInfo(reader);
                    skillInfoObjectList.add(eventObject);
                } catch (Exception e) {
                    log.info("error === i={}", i);
                    throw e;
                }
            }

            List<SkillInfoUnknown> wazInfoUnknownList = skillUnit.getSkillInfoUnknownList();
            int count2 = reader.readInt();
            for (int j = 0; j < count2; j++) {
                try {
                    SkillInfoUnknown wazInfoUnknown = createSecondaryObject(i, j, count2); //diff
                    wazInfoUnknown.readInfo(reader);
                    wazInfoUnknownList.add(wazInfoUnknown);
                } catch (Exception e) {
                    log.info("error === i={}", i);
                    throw e;
                }
            }

            if (i == 71 && count2 > 0) { //diff
                skillUnit.setSecondaryTailBytes(reader.readBytes(1));
            }

            if (!skillInfoObjectList.isEmpty() || !wazInfoUnknownList.isEmpty()) {
                skillUnitCollection.add(skillUnit);
            }
        }

        skillPhase.setPhaseTail(reader.readNullTerminatedString()); //diff
    }

    private SkillInfoUnknown createSecondaryObject(int slot, int index, int count) { //diff
        if (slot == 69 && index == 0 && count == 2) {
            return new SkillInfoUnknown(0xFF, 2, true);
        }
        return new SkillInfoUnknown(0xFF);
    }
}
