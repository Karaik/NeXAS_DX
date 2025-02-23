package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.*;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventHit__Read
 * 攻撃力：
 * 攻撃力：無し
 * 開始位置
 * 終了位置
 */
@Data
public class CEventHit extends WazInfoObject {

    private Short short1;
    private Short short2;
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;
    private Integer int7;
    private Integer int8;
    private Integer int9;
    private Integer int10;
    private Integer int11;
    private Integer int12;
    private Integer int13;
    private Integer int14;
    private Integer int15;
    private Integer int16;
    private Integer int17;
    private Integer int18;
    private Integer int19;
    private Integer int20;
    private Integer int21;
    private Integer int22;
    private Integer int23;

    private Integer flag1;
    private Integer flag2;
    private Integer flag3;
    private Integer maybeInt1;
    private Integer maybeInt2;
    private Integer maybeInt3;

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset += super.readInfo(bytes, offset);

        short1 = readInt16(bytes, offset); offset += 2;
        short2 = readInt16(bytes, offset); offset += 2;
        int1 = readInt32(bytes, offset); offset += 4;
        int2 = readInt32(bytes, offset); offset += 4;
        int3 = readInt32(bytes, offset); offset += 4;
        int4 = readInt32(bytes, offset); offset += 4;
        int5 = readInt32(bytes, offset); offset += 4;
        int6 = readInt32(bytes, offset); offset += 4;
        int7 = readInt32(bytes, offset); offset += 4;
        int8 = readInt32(bytes, offset); offset += 4;
        int9 = readInt32(bytes, offset); offset += 4;
        int10 = readInt32(bytes, offset); offset += 4;
        int11 = readInt32(bytes, offset); offset += 4;
        int12 = readInt32(bytes, offset); offset += 4;
        int13 = readInt32(bytes, offset); offset += 4;
        int14 = readInt32(bytes, offset); offset += 4;
        int15 = readInt32(bytes, offset); offset += 4;
        int16 = readInt32(bytes, offset); offset += 4;
        int17 = readInt32(bytes, offset); offset += 4;
        int18 = readInt32(bytes, offset); offset += 4;
        int19 = readInt32(bytes, offset); offset += 4;
        int20 = readInt32(bytes, offset); offset += 4;
        int21 = readInt32(bytes, offset); offset += 4;
        int22 = readInt32(bytes, offset); offset += 4;
        int23 = readInt32(bytes, offset); offset += 4;

        // TODO 这里的逻辑也调用了数个虚函数，极有可能出错
        for (int i = 0; i < 33; i++) {
            if (i < 17 || i > 20) {
                flag1 = readInt32(bytes, offset); offset += 4;
                if (flag1 == 0) {
                    continue;
                }
                maybeInt1 = readInt32(bytes, offset); offset += 4;
            } else {
                flag2 = readInt32(bytes, offset); offset += 4;
                if (flag2 != 0) {
                    maybeInt2 = readInt32(bytes, offset); offset += 4;
                } else {
                    flag3 = readInt32(bytes, offset); offset += 4;
                    if (flag3 != 0) {
                        maybeInt3 = readInt32(bytes, offset); offset += 4;
                    }
                }
            }
        }

        return offset;
    }

}
