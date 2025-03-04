package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Arrays;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/25
 * CEventCounter__Read
 */
@Data
public class CEventCounter extends WazInfoObject {

    @Data
    @AllArgsConstructor
    public static class CEventCounterType {
        private Integer type;
        private String description;
    }

    public static final CEventCounterType[] CEVENT_COUNTER_ENTRIES = {
            // 根据 .rdata:007F20B8 数组逆向数据定义
            new CEventCounterType(0xFFFFFFFF, "タイプ"),
            new CEventCounterType(0xFFFFFFFF, "回数"),
            new CEventCounterType(0xFFFFFFFF, "開始値"),
            new CEventCounterType(0xFFFFFFFF, "終了値"),
            new CEventCounterType(0xFFFFFFFF, "通常"),
            new CEventCounterType(0xFFFFFFFF, "加速"),
            new CEventCounterType(0xFFFFFFFF, "減速"),
            new CEventCounterType(0xFFFFFFFF, "ループ"),
            new CEventCounterType(0xFFFFFFFF, "往復")
    };

    private byte[] byteData1;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        // 读取0x1Cu字节
        this.byteData1 = Arrays.copyOfRange(bytes, offset, offset + 28); offset += 28;

        return offset;
    }
}
