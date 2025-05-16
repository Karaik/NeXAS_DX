package com.giga.nexas.dto.bsdx.grp.generator;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 */
public interface GrpFileGenerator<T extends Grp> {
    String getGeneratorKey();
    void generate(BinaryWriter writer, Grp grp) throws IOException;
}
