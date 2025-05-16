package com.giga.nexas.dto.bsdx.grp.generator.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.generator.GrpFileGenerator;
import com.giga.nexas.dto.bsdx.grp.groupmap.TermGrp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 * @Description TermGrpGenerator
 */
public class TermGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "Term";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) throws IOException {
        TermGrp termGrp = (TermGrp) grp;
        writer.writeInt(termGrp.getTermList().size());

        for (TermGrp.TermGroup group : termGrp.getTermList()) {
            writer.writeNullTerminatedString(group.getTermGroupName());
            writer.writeNullTerminatedString(group.getTermGroupCodeName());

            writer.writeInt(group.getTermItemList().size());
            for (TermGrp.TermItem item : group.getTermItemList()) {
                writer.writeNullTerminatedString(item.getTermItemName());
                writer.writeNullTerminatedString(item.getTermItemCodeName());
                writer.writeNullTerminatedString(item.getTermItemDescription());
                writer.writeInt(item.getParam1());
                writer.writeInt(item.getParam2());
            }
        }
    }
}
