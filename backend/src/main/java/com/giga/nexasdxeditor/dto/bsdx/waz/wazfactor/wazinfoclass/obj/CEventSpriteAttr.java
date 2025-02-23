package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.WazInfoObject;
import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * @Description 逆向时该虚函数调用下的字符串信息，一并记录
 * CEventSpriteAttr__Read
 * 角度指定
 * 左回転
 * 右回転
 * 下
 * 上
 * 左
 * 右
 * ランダム
 * %s : %4d (周期 : %4d)
 * %s : %4d
 * ⇒ %4d (周期 : %4d)
 */
@Data
public class CEventSpriteAttr extends WazInfoObject {

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

    @Override
    public int readInfo(byte[] bytes, int offset) {
        offset += super.readInfo(bytes, offset);

        setInt1(readInt32(bytes, offset));
        offset += 4;
        setInt2(readInt32(bytes, offset));
        offset += 4;
        setInt3(readInt32(bytes, offset));
        offset += 4;
        setInt4(readInt32(bytes, offset));
        offset += 4;
        setInt5(readInt32(bytes, offset));
        offset += 4;
        setInt6(readInt32(bytes, offset));
        offset += 4;
        setInt7(readInt32(bytes, offset));
        offset += 4;
        setInt8(readInt32(bytes, offset));
        offset += 4;
        setInt9(readInt32(bytes, offset));
        offset += 4;
        setInt10(readInt32(bytes, offset));
        offset += 4;
        setInt11(readInt32(bytes, offset));
        offset += 4;

        return offset;
    }
}
