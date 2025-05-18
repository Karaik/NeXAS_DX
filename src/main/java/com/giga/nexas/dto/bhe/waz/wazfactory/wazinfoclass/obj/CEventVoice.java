package com.giga.nexas.dto.bhe.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/5/18
 * CEventVoice__Read
 */
@Data
@NoArgsConstructor
public class CEventVoice extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventVoiceType {
        private Integer type;
        private String description;
    }

    public static final CEventVoiceType[] CEVENT_VOICE_TYPES = {
            new CEventVoiceType(0xFFFFFFFF, "タイプ"),
            new CEventVoiceType(0xFFFFFFFF, "優先順位")
    };

    public static final String[] CEVENT_VOICE_FORMATS = {
            "ERROR",
            "[%3d%%] %s",
            "[%3d%%] %s",
            "複：[%3d%%] %s",
            "bad allocation"
    };

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer result;

    private List<byte[]> byteDataList = new ArrayList<>();

    public CEventVoice(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();

        this.result = reader.readInt();

        if (result > 0) {
            this.byteDataList.clear();
            int counter = result;
            do {
                // 读取0xCu字节
                byte[] buffer = reader.readBytes(12);
                this.byteDataList.add(buffer);
                --counter;
            } while (counter > 0);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.result);
        for (byte[] buffer : byteDataList) {
            writer.writeBytes(buffer);
        }
    }

}

