package com.giga.nexas.dto.bhe.waz.parser;

import com.giga.nexas.dto.bhe.BheParser;
import com.giga.nexas.dto.bhe.waz.Waz;
import com.giga.nexas.dto.bhe.waz.wazfactory.SkillInfoFactory;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj.SkillInfoUnknown;
import com.giga.nexas.io.BinaryReader;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description WazParser
 * 逆向所得
 */
@Slf4j
public class WazParser implements BheParser<Waz> {

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

    // 解析技能阶段信息
    private void parseSkillPhaseInfo(Waz.Skill.SkillPhase skillPhase, BinaryReader reader) {
        List<SkillUnit> skillUnitCollection = skillPhase.getSkillUnitCollection();

        // 83次
        for (int i = 0; i < 83; i++) {
            SkillUnit skillUnit = new SkillUnit(i, SkillInfoFactory.SKILL_INFO_TYPE_ENTRIES_BHE[i].getDescription());

            List<SkillInfoObject> skillInfoObjectList = skillUnit.getSkillInfoObjectList();
            int count1 = reader.readInt();
            if (count1 > 100) {
                throw new RuntimeException("count1 == "+count1);
            }
            for (int j = 0; j < count1; j++) {
                try {
                    SkillInfoObject eventObject = SkillInfoFactory.createEventObjectBhe(i);
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
            if (count2 > 20) {
                throw new RuntimeException("count2 == "+count2);
            }
            for (int j = 0; j < count2; j++) {
                try {
                    SkillInfoUnknown wazInfoUnknown = (SkillInfoUnknown) SkillInfoFactory.createEventObjectBhe(0xFF);
                    wazInfoUnknown.readInfo(reader);
                    wazInfoUnknownList.add(wazInfoUnknown);
                } catch (Exception e) {
                    log.info("error === i={}", i);
                    throw e;
                }
            }

            if (!skillInfoObjectList.isEmpty() || !wazInfoUnknownList.isEmpty()) {
                skillUnitCollection.add(skillUnit);
            }
        }
    }
}
