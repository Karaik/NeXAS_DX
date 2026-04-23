package com.giga.nexas.dto.clarias.grp.parser.impl;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.clarias.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description MekaGroupGrpParser
 */
public class MekaGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "mekagroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        MekaGroupGrp mekaGroupGrp = new MekaGroupGrp();

        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            MekaGroupGrp.MekaGroup group = new MekaGroupGrp.MekaGroup();
            int existFLag = reader.readInt();
            group.setExistFlag(existFLag);
            if (existFLag != 0) {
                group.setMekaName(reader.readNullTerminatedString());
                group.setMekaCodeName(reader.readNullTerminatedString());
            }
            mekaGroupGrp.getMekaList().add(group);
        }

        return mekaGroupGrp;
    }
}

