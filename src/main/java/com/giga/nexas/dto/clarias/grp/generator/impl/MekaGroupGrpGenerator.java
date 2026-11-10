package com.giga.nexas.dto.clarias.grp.generator.impl;

import com.giga.nexas.dto.clarias.grp.Grp;
import com.giga.nexas.dto.clarias.grp.generator.GrpFileGenerator;
import com.giga.nexas.dto.clarias.grp.groupmap.MekaGroupGrp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 杩欎綅鍚屽(Karaik)
 * @Date 2025/5/16
 * @Description MekaGroupGrpGenerator
 */
public class MekaGroupGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "mekagroup";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) throws IOException {
        MekaGroupGrp mekaGroupGrp = (MekaGroupGrp) grp;
        writer.writeInt(mekaGroupGrp.getMekaList().size());

        for (MekaGroupGrp.MekaGroup group : mekaGroupGrp.getMekaList()) {
            writer.writeInt(group.getExistFlag());
            if (group.getExistFlag() != 0) {
                writer.writeNullTerminatedString(group.getMekaName());
                writer.writeNullTerminatedString(group.getMekaCodeName());
            }
        }
    }
}

