package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass;

import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description WazInfoObject
 */
@Data
public class WazInfoObject {

    /**
     * 第几帧开始
     */
    private Integer startFrame;

    /**
     * 第几帧结束
     */
    private Integer endFrame;

    public int readInfo(byte[] bytes, int offset) {

        setStartFrame(readInt32(bytes, offset));
        offset += 4;
        setEndFrame(readInt32(bytes, offset));
        offset += 4;

        return 8; // +8
    }

}
