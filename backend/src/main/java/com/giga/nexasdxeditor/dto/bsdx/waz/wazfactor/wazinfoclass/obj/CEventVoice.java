package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventVoice__Read
 */
@Data
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
}

