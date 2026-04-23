package com.giga.nexas.dto.clarias.grp.parser;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/10
 * @Description GrpFileParser
 */
public interface GrpFileParser<T extends Grp> {
    String getParserKey();
    T parse(BinaryReader reader);
}

