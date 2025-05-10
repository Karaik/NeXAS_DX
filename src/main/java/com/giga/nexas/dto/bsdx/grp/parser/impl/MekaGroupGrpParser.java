package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description MekaGroupGrpParser
 */
public class MekaGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "MekaGroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        MekaGroupGrp mekaGroupGrp = new MekaGroupGrp();

        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            int flag = reader.readInt();
            MekaGroupGrp.MekaGroup group = new MekaGroupGrp.MekaGroup();
            if (flag != 0) {
                group.setMekaName(reader.readNullTerminatedString());
                group.setMekaCodeName(reader.readNullTerminatedString());
            }
            mekaGroupGrp.getMekaList().add(group);
        }

        return mekaGroupGrp;
    }
}
