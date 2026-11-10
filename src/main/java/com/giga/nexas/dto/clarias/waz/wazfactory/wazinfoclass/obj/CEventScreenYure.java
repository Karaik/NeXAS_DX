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
public class CEventScreenYure extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventScreenYureType {
        private Integer type;
        private String description;
    }

    public static final CEventScreenYureType[] CEVENT_SCREEN_YURE_TYPES = {
            new CEventScreenYureType(0xFFFFFFFF, "角度指定"),
            new CEventScreenYureType(0xFFFFFFFF, "左回転"),
            new CEventScreenYureType(0xFFFFFFFF, "右回転"),
            new CEventScreenYureType(0xFFFFFFFF, "下"),
            new CEventScreenYureType(0xFFFFFFFF, "上"),
            new CEventScreenYureType(0xFFFFFFFF, "左"),
            new CEventScreenYureType(0xFFFFFFFF, "右"),
            new CEventScreenYureType(0xFFFFFFFF, "ランダム")
    };

    private ClariasInfoCollection term = new ClariasInfoCollection();
    private Integer int1;
    private Integer int2;
    private Integer int3;
    private Integer int4;

    public CEventScreenYure(Integer typeId) {
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
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        this.term.writeCollection(writer);
        writer.writeInt(this.int1);
        writer.writeInt(this.int2);
        writer.writeInt(this.int3);
        writer.writeInt(this.int4);
    }
}
