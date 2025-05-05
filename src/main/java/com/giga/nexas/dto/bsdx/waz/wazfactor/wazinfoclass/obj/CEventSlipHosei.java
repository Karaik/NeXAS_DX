package com.giga.nexas.dto.bsdx.waz.wazfactor.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/2/23
 * CEventSlipHosei__Read
 */
@Data
@NoArgsConstructor
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

    public CEventSlipHosei(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.long1 = reader.readDouble();
        this.long2 = reader.readDouble();

        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeDouble(this.long1);
        writer.writeDouble(this.long2);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
    }

}
