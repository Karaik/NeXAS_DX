package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description
 */
public class MapGroupGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "MapGroup";
    }

    @Override
    public Grp parse(BinaryReader reader) {

        return null;
    }
}
