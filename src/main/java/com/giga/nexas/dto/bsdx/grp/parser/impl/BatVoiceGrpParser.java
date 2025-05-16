package com.giga.nexas.dto.bsdx.grp.parser.impl;

import com.giga.nexas.dto.bsdx.grp.Grp;
import com.giga.nexas.dto.bsdx.grp.groupmap.BatVoiceGrp;
import com.giga.nexas.dto.bsdx.grp.parser.GrpFileParser;
import com.giga.nexas.io.BinaryReader;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/10
 * @Description
 */
public class BatVoiceGrpParser implements GrpFileParser<Grp> {

    @Override
    public String getParserKey() {
        return "BatVoice";
    }

    @Override
    public Grp parse(BinaryReader reader) {
        BatVoiceGrp batVoiceGrp = new BatVoiceGrp();
        
        int groupNameCount = reader.readInt();
        for (int i = 0; i < groupNameCount; i++) {
            BatVoiceGrp.BatVoiceTypeGroup type = new BatVoiceGrp.BatVoiceTypeGroup();
            type.setVoiceType(reader.readNullTerminatedString());
            type.setVoiceTypeCodeName(reader.readNullTerminatedString());
            batVoiceGrp.getVoiceTypeList().add(type);
        }

        int groupObjectCount = reader.readInt();
        for (int i = 0; i < groupObjectCount; i++) {
            BatVoiceGrp.BatVoiceGroup group = new BatVoiceGrp.BatVoiceGroup();
            int existFlag = reader.readInt();
            group.setExistFlag(existFlag);
            if (existFlag != 0) {
                group.setCharacterName(reader.readNullTerminatedString());
                group.setCharacterCodeName(reader.readNullTerminatedString());

                int voiceCount = reader.readInt();
                for (int j = 0; j < voiceCount; j++) {
                    BatVoiceGrp.BatVoice voice = new BatVoiceGrp.BatVoice();
                    int voiceExist = reader.readInt();
                    voice.setExistFlag(voiceExist);
                    if (voiceExist != 0) {
                        voice.setVoice(reader.readNullTerminatedString());
                        voice.setVoiceCodeName(reader.readNullTerminatedString());
                        voice.setVoiceFileName(reader.readNullTerminatedString());
                    }
                    group.getVoices().add(voice);
                }
            }
            batVoiceGrp.getVoiceList().add(group);
        }

        return batVoiceGrp;
    }
}
