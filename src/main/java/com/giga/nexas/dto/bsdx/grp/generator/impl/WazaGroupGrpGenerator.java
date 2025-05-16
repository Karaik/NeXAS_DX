package com.giga.nexas.dto.bsdx.grp.generator.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.generator.GrpFileGenerator;
import com.giga.nexas.dto.bsdx.grp.groupmap.WazaGroupGrp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 * @Description WazaGroupGrpGenerator
 */
public class WazaGroupGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "WazaGroup";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) throws IOException {
        WazaGroupGrp wazaGroupGrp = (WazaGroupGrp) grp;
        writer.writeInt(wazaGroupGrp.getWazaList().size());

        for (WazaGroupGrp.WazaGroupEntry entry : wazaGroupGrp.getWazaList()) {
            writer.writeInt(entry.getExistFlag());
            if (entry.getExistFlag() != 0) {
                writer.writeNullTerminatedString(entry.getWazaName());
                writer.writeNullTerminatedString(entry.getWazaCodeName());
                writer.writeNullTerminatedString(entry.getWazaDisplayName());
                writer.writeInt(entry.getParam());
            }
        }
    }
}
