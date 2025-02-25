package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import java.util.Arrays;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventCounter__Read
 */
@Data
public class CEventCounter extends WazInfoObject {

    private byte[] byteData1;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        // 读取0x1Cu字节
        byteData1 = Arrays.copyOfRange(bytes, offset, offset + 28); offset += 28;

        return offset;
    }
}
