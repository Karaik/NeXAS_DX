package com.giga.nexas.dto.bsdx.grp.generator.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.generator.GrpFileGenerator;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.io.BinaryWriter;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/16
 * @Description BatVoiceGrpGenerator
 */
public class BatVoiceGrpGenerator implements GrpFileGenerator<Grp> {

    @Override
    public String getGeneratorKey() {
        return "BatVoice";
    }

    @Override
    public void generate(BinaryWriter writer, Grp grp) throws IOException {
        BatVoiceGrp batVoiceGrp = (BatVoiceGrp) grp;
        writer.writeInt(batVoiceGrp.getVoiceTypeList().size());
        for (BatVoiceGrp.BatVoiceTypeGroup type : batVoiceGrp.getVoiceTypeList()) {
            writer.writeNullTerminatedString(type.getVoiceType());
            writer.writeNullTerminatedString(type.getVoiceTypeCodeName());
        }

        writer.writeInt(batVoiceGrp.getVoiceList().size());
        for (BatVoiceGrp.BatVoiceGroup group : batVoiceGrp.getVoiceList()) {
            writer.writeInt(group.getExistFlag());
            if (group.getExistFlag() != 0) {
                writer.writeNullTerminatedString(group.getCharacterName());
                writer.writeNullTerminatedString(group.getCharacterCodeName());

                writer.writeInt(group.getVoices().size());
                for (BatVoiceGrp.BatVoice voice : group.getVoices()) {
                    writer.writeInt(voice.getExistFlag());
                    if (voice.getExistFlag() != 0) {
                        writer.writeNullTerminatedString(voice.getVoice());
                        writer.writeNullTerminatedString(voice.getVoiceCodeName());
                        writer.writeNullTerminatedString(voice.getVoiceFileName());
                    }
                }
            }
        }
    }

}
