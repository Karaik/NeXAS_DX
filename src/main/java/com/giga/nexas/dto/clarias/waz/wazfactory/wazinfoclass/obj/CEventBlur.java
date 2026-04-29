package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.dto.clarias.ClariasInfoCollection;
import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventBlur extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventBlurType {
        private Integer type;
        private String description;
    }

    public static final CEventBlurType[] CEVENT_BLUR_ENTRIES = {
            new CEventBlurType(0xFFFFFFFF, "%3d(%3d,%3d)"),
            new CEventBlurType(0xFFFFFFFF, "%3d(%3d,%3d)"),
            new CEventBlurType(0xFFFFFFFF, "⇒ %3d(%3d,%3d)")
    };

    private ClariasInfoCollection term = new ClariasInfoCollection();
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;
    private Integer int5;
    private Integer int6;

    public CEventBlur(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);

        this.term = new ClariasInfoCollection();
        this.term.readCollection(reader);
        this.int1 = reader.readInt();
        this.int2 = reader.readInt();
        this.int3 = reader.readInt();
        this.int4 = reader.readInt();
        this.int5 = reader.readInt();
        this.int6 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        this.term.writeCollection(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
        writer.writeInt(this.int5);
        writer.writeInt(this.int6);
    }
}
