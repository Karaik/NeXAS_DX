package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.TermGrp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description TermGrpParser
 */
public class TermGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "Term";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        TermGrp termGrp = new TermGrp();

        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            TermGrp.TermGroup group = new TermGrp.TermGroup();
            group.setTermGroupName(reader.readNullTerminatedString());
            group.setTermGroupCodeName(reader.readNullTerminatedString());

            int itemCount = reader.readInt();
            for (int j = 0; j < itemCount; j++) {
                TermGrp.TermItem item = new TermGrp.TermItem();
                item.setTermItemName(reader.readNullTerminatedString());
                item.setTermItemCodeName(reader.readNullTerminatedString());
                item.setTermItemDescription(reader.readNullTerminatedString());
                item.setParam1(reader.readInt());
                item.setParam2(reader.readInt());
                group.getTermItemList().add(item);
            }

            termGrp.getTermList().add(group);
        }

        return termGrp;
    }
}
