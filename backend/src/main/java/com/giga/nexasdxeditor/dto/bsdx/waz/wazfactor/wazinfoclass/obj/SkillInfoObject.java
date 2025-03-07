package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description WazInfoObject
 */
@Data
public class SkillInfoObject {

    public Integer offset;
    /**
     * 第几帧开始
     */
    private Integer startFrame;

    /**
     * 第几帧结束
     */
    private Integer endFrame;

    public int readInfo(byte[] bytes, int offset) {

        this.offset = offset;

        this.startFrame = readInt32(bytes, offset); offset += 4;
        this.endFrame = readInt32(bytes, offset); offset += 4;

        return offset; // +8
    }

    public void writeInfo(byte[] bytes) {
        // TODO
    }

}
