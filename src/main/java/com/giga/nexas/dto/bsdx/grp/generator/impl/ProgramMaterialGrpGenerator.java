package com.giga.nexas.dto.bsdx.grp.generator.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.generator.GrpFileGenerator;
import com.giga.nexas.io.BinaryWriter;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 * @Description
 */
public class ProgramMaterialGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "ProgramMaterial";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) {

    }

}
