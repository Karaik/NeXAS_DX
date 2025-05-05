package com.giga.nexas.dto.bsdx.waz.parser;

import com.giga.nexas.dto.bsdx.BsdxParser;
import com.giga.nexas.dto.bsdx.waz.Waz;
import com.giga.nexas.dto.bsdx.waz.wazfactor.SkillInfoFactory;
import com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.SkillUnit;
import com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoUnknown;
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
public class WazParser implements BsdxParser<Waz> {

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

                // 每个块开始前，都会有个flag代表“是否存在”
                int flag = reader.readInt();
                if (flag == 0) {
                    wazBlockList.add(skill);
                    continue;
                }

                // 1.技能信息1
                skill.setSkillNameJapanese(reader.readNullTerminatedString());

                // 2.技能信息2
                skill.setSkillNameEnglish(reader.readNullTerminatedString());

                // 3.阶段数
                int phaseQuantity = reader.readInt();
                skill.setPhaseQuantity(phaseQuantity);

                // 4.技能数据
                List<Waz.Skill.SkillPhase> phaseInfoList = skill.getPhasesInfo();
                for (int i = 0; i < phaseQuantity; i++) {
                    Waz.Skill.SkillPhase skillPhase = new Waz.Skill.SkillPhase();
                    parseSkillPhaseInfo(skillPhase, reader);
                    phaseInfoList.add(skillPhase);
                }

                // 解析技能后缀
                int countSuffix = reader.readInt();
                List<Waz.Skill.SkillSuffix> skillSuffixList = skill.getSkillSuffixList();
                for (int i = 0; i < countSuffix; i++) {
                    Waz.Skill.SkillSuffix skillSuffix = new Waz.Skill.SkillSuffix();
                    skillSuffix.setInt1(reader.readInt());
                    skillSuffix.setInt2(reader.readInt());
                    skillSuffixList.add(skillSuffix);
                }

                // 添加技能到列表
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

        for (int i = 0; i < 72; i++) { // 逆向得知循环72次
            SkillUnit skillUnit = new SkillUnit(i, SkillInfoFactory.SKILL_INFO_TYPE_ENTRIES[i].getDescription());

            List<SkillInfoObject> skillInfoObjectList = skillUnit.getSkillInfoObjectList();
            int count1 = reader.readInt();
            for (int j = 0; j < count1; j++) {
                try {
                    SkillInfoObject eventObject = SkillInfoFactory.createEventObject(i);
                    eventObject.setSlotNum(i);
                    eventObject.readInfo(reader);  // 使用 BinaryReader 读取信息
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
                    SkillInfoUnknown wazInfoUnknown = (SkillInfoUnknown) SkillInfoFactory.createEventObject(0xFF);
                    wazInfoUnknown.readInfo(reader);  // 使用 BinaryReader 读取信息
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
