package com.giga.nexas.transfer.bhe2bsdx.meka.sou.convert.waz;

import com.giga.nexas.dto.bsdx.waz.Waz;

import java.util.ArrayList;
import java.util.List;


public class BheToBsdxWazConverter {

    private final BheToBsdxWazSkillConverter skillConverter = new BheToBsdxWazSkillConverter();

    public Waz convert(com.giga.nexas.dto.bhe.waz.Waz source) {
        Waz target = new Waz();
        if (source == null) {
            return target;
        }

        target.setFileName(source.getFileName());
        target.setExtensionName(source.getExtensionName());
        target.setSkillList(convertSkills(source.getSkillList()));
        return target;
    }

    private List<Waz.Skill> convertSkills(List<com.giga.nexas.dto.bhe.waz.Waz.Skill> sourceList) {
        List<Waz.Skill> targetList = new ArrayList<>();
        if (sourceList == null) {
            return targetList;
        }

        for (com.giga.nexas.dto.bhe.waz.Waz.Skill source : sourceList) {
            targetList.add(skillConverter.convert(source));
        }
        return targetList;
    }
}
