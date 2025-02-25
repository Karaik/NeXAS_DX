package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import lombok.Data;

import static com.giga.nexasdxeditor.util.ParserUtil.readInt32;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/22
 * @Description SpmCallingInfo
 * CEventSprite__Read
 * 通常描画
 *  该阶段中的各动作使用哪个spm图层，以及帧数关系
 */
@Data
public class CEventSprite extends WazInfoObject {

    /**
     * 机体对应spm，但多为FF FF FF FF(-1)
     */
    private Integer spmFileSequence;

    /**
     * 第几号动作组
     */
    private Integer actionGroupNumber;

    /**
     * 该动作组的第几个动作
     */
    private Integer actionNumber;

    @Override
    public int readInfo(byte[] bytes, int offset) {

        offset = super.readInfo(bytes, offset);

        setSpmFileSequence(readInt32(bytes, offset)); offset += 4;
        setActionGroupNumber(readInt32(bytes, offset)); offset += 4;
        setActionNumber(readInt32(bytes, offset)); offset += 4;

        return offset;
    }
}
