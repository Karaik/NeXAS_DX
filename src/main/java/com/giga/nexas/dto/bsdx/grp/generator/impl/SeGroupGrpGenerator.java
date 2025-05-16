package com.giga.nexas.dto.bsdx.grp.generator.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.generator.GrpFileGenerator;
import com.giga.nexas.dto.bsdx.grp.groupmap.SeGroupGrp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 * @Description SeGroupGrpGenerator
 */
public class SeGroupGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "SeGroup";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) throws IOException {
        SeGroupGrp seGroupGrp = (SeGroupGrp) grp;
        writer.writeInt(seGroupGrp.getSeList().size());

        for (SeGroupGrp.SeGroupGroup group : seGroupGrp.getSeList()) {
            writer.writeInt(group.getExistFlag());

            if (group.getExistFlag() != 0) {
                writer.writeNullTerminatedString(group.getSeType());
                writer.writeNullTerminatedString(group.getSeTypeCodeName());

                writer.writeInt(group.getSeItems().size());

                for (SeGroupGrp.SeGroupItem item : group.getSeItems()) {
                    writer.writeInt(item.getExistFlag());
                    if (item.getExistFlag() != 0) {
                        writer.writeNullTerminatedString(item.getSeItemName());
                        writer.writeNullTerminatedString(item.getSeItemCodeName());
                        writer.writeNullTerminatedString(item.getSeFileName());
                    }
                }
            }
        }
    }
}
