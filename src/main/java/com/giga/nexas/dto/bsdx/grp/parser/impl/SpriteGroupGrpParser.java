package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description SpriteGroupGrpParser
 */
public class SpriteGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "SpriteGroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        SpriteGroupGrp spriteGroupGrp = new SpriteGroupGrp();

        int groupCount = reader.readInt();
        for (int i = 0; i < groupCount; i++) {
            SpriteGroupGrp.SpriteGroupEntry entry = new SpriteGroupGrp.SpriteGroupEntry();
            int flag = reader.readInt();
            entry.setExistFlag(flag);
            if (flag != 0) {
                entry.setSpriteFileName(reader.readNullTerminatedString());
                entry.setSpriteCodeName(reader.readNullTerminatedString());
                entry.setParam(reader.readInt());
            }
            spriteGroupGrp.getSpriteList().add(entry);
        }

        return spriteGroupGrp;
    }
}
