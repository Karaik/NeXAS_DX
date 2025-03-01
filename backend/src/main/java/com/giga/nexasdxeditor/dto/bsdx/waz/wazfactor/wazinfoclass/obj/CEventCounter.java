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
            new CEventCounterType(0x9, "チェック方向"),
            new CEventCounterType(0xFFFFFFFF, "チェック方向補正"),
            new CEventCounterType(0xFFFFFFFF, "チェック範囲"),
            new CEventCounterType(0xFFFFFFFF, "チェック範囲（高さ）"),
            new CEventCounterType(0xFFFFFFFF, "チェック高度補正"),
            new CEventCounterType(0xFFFFFFFF, "チェック回数"),
            new CEventCounterType(0xFFFFFFFF, "ボタン入力（範囲内）"),
            new CEventCounterType(0xFFFFFFFF, "ボタン入力（範囲外）")
    };

    private byte[] byteData1;

    private Integer checkDirection;       // チェック方向 (4 字节)
    private Integer directionCorrection;  // チェック方向補正 (4 字节)
    private Integer checkRange;           // チェック範囲 (4 字节)
    private Integer heightRange;          // チェック範囲（高さ） (4 字节)
    private Integer heightCorrection;     // チェック高度補正 (4 字节)
    private Integer checkCount;           // チェック回数 (4 字节)
    private Integer buttonInsideRange;    // ボタン入力（範囲内） (4 字节)

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset = super.readInfo(bytes, offset);

        // 读取0x1Cu字节
        byteData1 = Arrays.copyOfRange(bytes, offset, offset + 28);

        checkDirection = readInt32(bytes, offset); offset += 4;
        directionCorrection = readInt32(bytes, offset); offset += 4;
        checkRange = readInt32(bytes, offset); offset += 4;
        heightRange = readInt32(bytes, offset); offset += 4;
        heightCorrection = readInt32(bytes, offset); offset += 4;
        checkCount = readInt32(bytes, offset); offset += 4;
        buttonInsideRange = readInt32(bytes, offset); offset += 4;

        return offset;
    }
}
