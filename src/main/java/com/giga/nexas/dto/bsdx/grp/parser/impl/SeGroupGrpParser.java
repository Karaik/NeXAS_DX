package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description
 */
public class SeGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "SeGroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        SeGroupGrp seGroupGrp = new SeGroupGrp();

        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            int existFlag = reader.readInt();
            if (existFlag != 0) {
                SeGroupGrp.SeGroupGroup group = new SeGroupGrp.SeGroupGroup();
                group.setSeType(reader.readNullTerminatedString());
                group.setSeTypeCodeName(reader.readNullTerminatedString());
                int itemCount = reader.readInt();
                for (int j = 0; j < itemCount; j++) {
                    int itemFlag = reader.readInt();
                    if (itemFlag != 0) {
                        SeGroupGrp.SeGroupItem item = new SeGroupGrp.SeGroupItem();
                        item.setSeItemName(reader.readNullTerminatedString());
                        item.setSeItemCodeName(reader.readNullTerminatedString());
                        item.setSeFileName(reader.readNullTerminatedString());
                        group.getSeItems().add(item);
                    }
                }
                seGroupGrp.getSeList().add(group);
            }
        }

        return seGroupGrp;
    }
}
