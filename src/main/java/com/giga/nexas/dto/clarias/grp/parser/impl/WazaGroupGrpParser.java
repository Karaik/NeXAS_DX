package com.giga.nexas.dto.clarias.grp.parser.impl;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.dto.clarias.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description WazaGroupGrpParser
 */
public class WazaGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "wazagroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        WazaGroupGrp wazaGroupGrp = new WazaGroupGrp();

        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            WazaGroupGrp.WazaGroupEntry entry = new WazaGroupGrp.WazaGroupEntry();
            int flag = reader.readInt();
            entry.setExistFlag(flag);
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

