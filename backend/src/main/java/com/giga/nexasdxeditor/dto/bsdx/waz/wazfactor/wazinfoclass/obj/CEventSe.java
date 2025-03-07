package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/26
 * CEvent__Read
 */
@Data
public class CEventSe extends SkillInfoObject {

    private Integer count;

    private List<byte[]> byteDataList = new ArrayList<>();

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        int count = readInt32(bytes, offset);
        setCount(count); offset += 4;
        if (count > 0) {
            this.byteDataList.clear();
            do {
                // 读取16字节
                byte[] byteData = Arrays.copyOfRange(bytes, offset, offset + 16); offset += 16;
                this.byteDataList.add(byteData);
                --count;
            } while (count > 0);
        }

        return offset;
    }
}
