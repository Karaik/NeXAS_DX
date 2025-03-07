package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventSlipHosei__Read
 */
@Data
public class CEventSlipHosei extends SkillInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventSlipHoseiType {
        private Integer type;
        private String description;
    }

    public static final CEventSlipHoseiType[] CEVENT_SLIP_HOSEI_TYPES = {
            new CEventSlipHoseiType(0xFFFFFFFF, "最低速度"),
            new CEventSlipHoseiType(0xFFFFFFFF, "最高速度"),
            new CEventSlipHoseiType(0xFFFFFFFF, "最低上昇量"),
            new CEventSlipHoseiType(0xFFFFFFFF, "最高上昇量"),
            new CEventSlipHoseiType(0xFFFFFFFF, "最低重力増分"),
            new CEventSlipHoseiType(0xFFFFFFFF, "最高重力増分")
    };


    private Double long1;
    private Double long2;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        this.long1 = readDouble(bytes, offset); offset += 8;
        this.long2 = readDouble(bytes, offset); offset += 8;

        this.int1 = readInt32(bytes, offset); offset += 4;
        this.int2 = readInt32(bytes, offset); offset += 4;
        this.int3 = readInt32(bytes, offset); offset += 4;
        this.int4 = readInt32(bytes, offset); offset += 4;

        return offset;
    }
}
