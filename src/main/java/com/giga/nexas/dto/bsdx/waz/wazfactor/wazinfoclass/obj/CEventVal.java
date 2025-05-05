package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventVal__Read(read_wazSpmFrameAnd4Unk)
 * タイプ
 * 優先順位
 */
@Data
@NoArgsConstructor
public class CEventVal extends SkillInfoObject {

    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    public CEventVal(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
    }

}
