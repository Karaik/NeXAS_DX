package com.giga.nexasdxeditor.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexasdxeditor.io.BinaryReader;
import lombok.Data;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventVal__Read(read_wazSpmFrameAnd4Unk)
 * タイプ
 * 優先順位
 */
@Data
public class CEventVal extends SkillInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
    }
}
