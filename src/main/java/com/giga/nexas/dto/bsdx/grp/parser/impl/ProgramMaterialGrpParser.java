package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description
 */
public class ProgramMaterialGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "ProgramMaterial";
    }

    @Override
    public Grp parse(BinaryReader reader) {

        return null;
    }
}
