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
    public Waz parse(byte[] data, String filename, String charset) {
        Waz waz = new Waz(filename);
        waz.setSourceBytes(data);
        BinaryReader reader = new BinaryReader(data, charset);
        try {
            while (reader.hasRemaining()) {
                int skillStartOffset = reader.getPosition();
                Waz.Skill skill = new Waz.Skill();
                int flag = reader.readInt();
                if (flag == 0) {
                    waz.getSkillList().add(skill);
                    continue;
                }
                skill.setSkillNameJapanese(reader.readNullTerminatedString());
                skill.setSkillNameEnglish(reader.readNullTerminatedString());
                int phaseQuantity = reader.readInt();
                if (phaseQuantity < 0 || phaseQuantity > 20) {
                    throw new IllegalStateException("CLARIAS WAZ phase count invalid: skill=" + waz.getSkillList().size()
                            + ", skillStartOffset=" + skillStartOffset
                            + ", japanese=" + skill.getSkillNameJapanese()
                            + ", english=" + skill.getSkillNameEnglish()
                            + ", phaseQuantity=" + phaseQuantity
                            + ", readerOffset=" + reader.getPosition());
                }
                skill.setPhaseQuantity(phaseQuantity);
                for (int i = 0; i < phaseQuantity; i++) {
                    Waz.Skill.SkillPhase phase = new Waz.Skill.SkillPhase();
                    parseSkillPhaseInfo(phase, reader, waz.getSkillList().size(), i);
                    skill.getPhasesInfo().add(phase);
                }
                int suffixCountOffset = reader.getPosition();
                int suffixCount = reader.readInt();
                if (suffixCount < 0 || suffixCount > 100) {
                    throw new IllegalStateException("CLARIAS WAZ suffix count invalid: skill=" + waz.getSkillList().size()
                            + ", skillStartOffset=" + skillStartOffset
                            + ", japanese=" + skill.getSkillNameJapanese()
                            + ", english=" + skill.getSkillNameEnglish()
                            + ", suffixCount=" + suffixCount
                            + ", suffixCountOffset=" + suffixCountOffset);
                }
                for (int i = 0; i < suffixCount; i++) {
                    Waz.Skill.SkillSuffix suffix = new Waz.Skill.SkillSuffix();
                    try {
                        suffix.setInt1(reader.readInt());
                        suffix.setInt2(reader.readInt());
                    } catch (RuntimeException e) {
                        throw new IllegalStateException("CLARIAS WAZ suffix read failed: skill=" + waz.getSkillList().size()
                                + ", skillStartOffset=" + skillStartOffset
                                + ", japanese=" + skill.getSkillNameJapanese()
                                + ", english=" + skill.getSkillNameEnglish()
                                + ", suffixCount=" + suffixCount
                                + ", suffixCountOffset=" + suffixCountOffset
                                + ", suffixIndex=" + i
                                + ", readerOffset=" + reader.getPosition(), e);
                    }
                    skill.getSkillSuffixList().add(suffix);
                }
                waz.getSkillList().add(skill);
            }
        } catch (Exception e) {
            throw e;
        }
        return waz;
    }

    private void parseSkillPhaseInfo(Waz.Skill.SkillPhase phase, BinaryReader reader, int skillIndex, int phaseIndex) {
        List<SkillUnit> units = phase.getSkillUnitCollection();
        for (int i = 0; i < SkillInfoFactory.SLOT_COUNT; i++) {
            SkillUnit unit = new SkillUnit(i, SkillInfoFactory.SKILL_INFO_TYPE_ENTRIES_CLARIAS[i].getDescription());
            int count1Offset = reader.getPosition();
            int count1 = reader.readInt();
            if (count1 < 0 || count1 > 100) {
                throw new IllegalStateException("CLARIAS WAZ count1 invalid: skill=" + skillIndex
                        + ", phase=" + phaseIndex
                        + ", slot=" + i
                        + ", count1=" + count1
                        + ", count1Offset=" + count1Offset);
            }
            for (int j = 0; j < count1; j++) {
                SkillInfoObject object = SkillInfoFactory.createEventObjectClarias(i);
                object.setSlotNum(i);
                int objectOffset = reader.getPosition();
                try {
                    object.readInfo(reader);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("CLARIAS WAZ object read failed: skill=" + skillIndex
                            + ", phase=" + phaseIndex
                            + ", slot=" + i
                            + ", typeId=" + object.getTypeId()
                            + ", count1=" + count1
                            + ", count1Offset=" + count1Offset
                            + ", objectIndex=" + j
                            + ", objectOffset=" + objectOffset
                            + ", readerOffset=" + reader.getPosition(), e);
                }
                unit.getSkillInfoObjectList().add(object);
            }
            int count2Offset = reader.getPosition();
            int count2 = reader.readInt();
            if (count2 < 0 || count2 > 100) {
                throw new IllegalStateException("CLARIAS WAZ count2 invalid: skill=" + skillIndex
                        + ", phase=" + phaseIndex
                        + ", slot=" + i
                        + ", count2=" + count2
                        + ", count2Offset=" + count2Offset);
            }
            for (int j = 0; j < count2; j++) {
                SkillInfoUnknown unknown = createSecondaryObject(i, j, count2);
                int unknownOffset = reader.getPosition();
                try {
                    unknown.readInfo(reader);
                } catch (RuntimeException e) {
                    throw new IllegalStateException("CLARIAS WAZ unknown read failed: skill=" + skillIndex
                            + ", phase=" + phaseIndex
                            + ", slot=" + i
                            + ", count2=" + count2
                            + ", count2Offset=" + count2Offset
                            + ", unknownIndex=" + j
                            + ", unknownOffset=" + unknownOffset
                            + ", readerOffset=" + reader.getPosition(), e);
                }
                unit.getSkillInfoUnknownList().add(unknown);
            }
            if (i == 71 && count2 > 0) {
                unit.setSecondaryTailBytes(reader.readBytes(1));
            }
            if (!unit.getSkillInfoObjectList().isEmpty() || !unit.getSkillInfoUnknownList().isEmpty()) {
                units.add(unit);
            }
        }
        phase.setPhaseTail(reader.readNullTerminatedString());
    }


    private SkillInfoUnknown createSecondaryObject(int slot, int index, int count) {
        if (slot == 69 && index == 0 && count == 2) {
            return new SkillInfoUnknown(0xFF, 2, true);
        }
        return new SkillInfoUnknown(0xFF);
    }
}
