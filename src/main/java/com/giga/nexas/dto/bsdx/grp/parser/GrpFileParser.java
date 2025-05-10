package com.giga.nexas.dto.bsdx.grp.parser;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description GrpFileParser
 */
public interface GrpFileParser<T extends Grp> {
    String getParserKey();
    T parse(BinaryReader reader);
}
