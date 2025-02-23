package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;
import static com.giga.nexasdxeditor.util.ParserUtil.readInt64;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventSlipHosei__Read
 * ERROR:CMeka,CPartsじゃないのに自動指定を使った aErrorCmekaCpar
 */
@Data
public class CEventSlipHosei extends WazInfoObject {

    private Long long1;
    private Long long2;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset += super.readInfo(bytes, offset);

        setLong1(readInt64(bytes, offset));
        offset += 8;
        setLong2(readInt64(bytes, offset));
        offset += 8;

        setInt1(readInt32(bytes, offset));
        offset += 4;
        setInt2(readInt32(bytes, offset));
        offset += 4;
        setInt3(readInt32(bytes, offset));
        offset += 4;
        setInt4(readInt32(bytes, offset));
        offset += 4;

        return offset;
    }
}
