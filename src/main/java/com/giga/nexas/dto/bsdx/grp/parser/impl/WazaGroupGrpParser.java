package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description WazaGroupGrpParser
 */
public class WazaGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "WazaGroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        WazaGroupGrp wazaGroupGrp = new WazaGroupGrp();

        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            int flag = reader.readInt();
            WazaGroupGrp.WazaGroupEntry entry = new WazaGroupGrp.WazaGroupEntry();
            if (flag != 0) {
                entry.setWazaName(reader.readNullTerminatedString());
                entry.setWazaCodeName(reader.readNullTerminatedString());
                entry.setWazaDisplayName(reader.readNullTerminatedString());
                entry.setParam(reader.readInt());
            }
            wazaGroupGrp.getWazaList().add(entry);
        }

        return wazaGroupGrp;
    }
}
