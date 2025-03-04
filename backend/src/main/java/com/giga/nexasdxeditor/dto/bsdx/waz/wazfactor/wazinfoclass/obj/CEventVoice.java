package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEventVoice__Read
 */
@Data
public class CEventVoice extends WazInfoObject {

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
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;

        this.result = readInt32(bytes, offset); offset += 4;

        if (result > 0) {
            this.byteDataList.clear();
            int counter = result;
            do {
                // 读取0xCu字节
                byte[] buffer = Arrays.copyOfRange(bytes, offset, offset + 12); offset += 12;
                this.byteDataList.add(buffer);
                --counter;
            } while (counter > 0);
        }

        return offset;
    }
}

