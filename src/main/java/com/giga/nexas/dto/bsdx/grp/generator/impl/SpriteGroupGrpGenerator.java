package com.giga.nexas.dto.bsdx.grp.generator.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.generator.GrpFileGenerator;
import com.giga.nexas.dto.bsdx.grp.groupmap.SpriteGroupGrp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 * @Description SpriteGroupGrpGenerator
 */
public class SpriteGroupGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "SpriteGroup";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) throws IOException {
        SpriteGroupGrp spriteGroupGrp = (SpriteGroupGrp) grp;
        writer.writeInt(spriteGroupGrp.getSpriteList().size());

        for (SpriteGroupGrp.SpriteGroupEntry entry : spriteGroupGrp.getSpriteList()) {
            writer.writeInt(entry.getExistFlag());
            if (entry.getExistFlag() != 0) {
                writer.writeNullTerminatedString(entry.getSpriteFileName());
                writer.writeNullTerminatedString(entry.getSpriteCodeName());
                writer.writeInt(entry.getParam());
            }
        }
    }
}
