package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

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

