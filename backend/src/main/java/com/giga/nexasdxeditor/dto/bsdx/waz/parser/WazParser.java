package com.giga.nexasdxeditor.dto.bsdx.waz.parser;

import com.giga.nexasdxeditor.dto.bsdx.waz.Waz;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.SkillInfoFactory;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.SkillUnit;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoObject;
import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj.SkillInfoUnknown;
import lombok.extern.slf4j.Slf4j;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.*;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/1/19
 * @Description WazParser
 * 逆向所得
 */
@Slf4j
public class WazParser {

    public static Waz parseWaz(byte[] bytes, String fileName, String charset) {

        Waz waz = new Waz(fileName);
        List<Waz.Skill> wazBlockList = waz.getSkillList();
        try {

            int offset = 0;
            while (offset < bytes.length) {

                Waz.Skill skill = new Waz.Skill();

                // 每个块开始前，都会有个flag代表“是否存在”
                int flag = readInt32(bytes, offset); offset += 4;
                if (flag == 0) {
                    wazBlockList.add(skill);
                    continue;
                }

                // 1.技能信息1
                skill.setSkillNameJapanese(new String(bytes, offset, findNullTerminator(bytes, offset), Charset.forName(charset)));
                offset += findNullTerminator(bytes, offset) + 1;

                // 2.技能信息2
                skill.setSkillNameEnglish(new String(bytes, offset, findNullTerminator(bytes, offset), StandardCharsets.UTF_8));
                offset += findNullTerminator(bytes, offset) + 1;

                // 3.阶段数
                int phaseQuantity = readInt32(bytes, offset); offset += 4;
                skill.setPhaseQuantity(phaseQuantity);

                // 4.技能数据
                List<Waz.Skill.SkillPhase> phaseInfoList = skill.getPhasesInfo();
                for (int i = 0; i < phaseQuantity; i++) {
                    Waz.Skill.SkillPhase skillPhase = new Waz.Skill.SkillPhase();
                    offset = parseSkillPhaseInfo(skillPhase, bytes, offset);
                    phaseInfoList.add(skillPhase);
                }

                int countSuffix = readInt32(bytes, offset); offset += 4;
                List<Waz.Skill.SkillSuffix> skillSuffixList = skill.getSkillSuffixList();
                for (int i = 0; i < countSuffix; i++) {
                    Waz.Skill.SkillSuffix skillSuffix = new Waz.Skill.SkillSuffix();
                    skillSuffix.setInt1(readInt32(bytes, offset)); offset += 4;
                    skillSuffix.setInt2(readInt32(bytes, offset)); offset += 4;
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

    private static int parseSkillPhaseInfo(Waz.Skill.SkillPhase skillPhase, byte[] bytes, int offset) {
        // 存放单个阶段的数据信息
        List<SkillUnit> skillUnitCollection = skillPhase.getSkillUnitCollection();

        for (int i = 0; i < 72; i++) { // 逆向得知循环72次
            SkillUnit skillUnit = new SkillUnit(i, SkillInfoFactory.SKILL_INFO_TYPE_ENTRIES[i].getDescription());

            List<SkillInfoObject> skillInfoObjectList = skillUnit.getSkillInfoObjectList();
            int count1 = readInt32(bytes, offset); offset += 4;
            for (int j = 0; j < count1; j++) {
                try {
                    SkillInfoObject eventObject = SkillInfoFactory.createEventObject(i);
                    offset = eventObject.readInfo(bytes, offset);
                    skillInfoObjectList.add(eventObject);
                } catch (Exception e) {
                    log.info("error === i={}", i);
                    throw e;
                }
            }

            List<SkillInfoUnknown> wazInfoUnknownList = skillUnit.getSkillInfoUnknownList();
            int count2 = readInt32(bytes, offset); offset += 4;
            for (int j = 0; j < count2; j++) {
                try {
                    SkillInfoUnknown wazInfoUnknown = (SkillInfoUnknown) SkillInfoFactory.createEventObject(0xFF);
                    offset = wazInfoUnknown.readInfo(bytes, offset);
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

        return offset;
    }


}
