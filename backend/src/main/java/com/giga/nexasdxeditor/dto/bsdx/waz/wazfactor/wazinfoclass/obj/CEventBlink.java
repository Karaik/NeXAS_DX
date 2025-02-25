package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventBlink__Read
 */
@Data
public class CEventBlink extends WazInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Short short1;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        setInt1(readInt32(bytes, offset)); offset += 4;
        setInt2(readInt32(bytes, offset)); offset += 4;
        setInt3(readInt32(bytes, offset)); offset += 4;

        setShort1(readInt16(bytes, offset)); offset += 2;

        return offset;
    }
}
