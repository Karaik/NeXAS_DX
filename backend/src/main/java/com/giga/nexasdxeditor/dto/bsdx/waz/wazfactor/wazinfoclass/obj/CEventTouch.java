package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import java.util.Arrays;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventTouch__Read
 */
@Data
public class CEventTouch extends WazInfoObject {

    private byte[] byteData1;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        // 读取0x38u字节
        byteData1 = Arrays.copyOfRange(bytes, offset, offset + 56); offset += 56;

        return offset;
    }
}
