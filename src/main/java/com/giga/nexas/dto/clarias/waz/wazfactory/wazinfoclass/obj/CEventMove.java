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
public class CEventMove extends SkillInfoObject {
    @Data
    @AllArgsConstructor
    public static class CEventMoveType {
        private Integer type;
        private String description;
    }

    public static final CEventMoveType[] CEVENT_MOVE_TYPES = {
            new CEventMoveType(0xFFFFFFFF, "開始位置"),
            new CEventMoveType(0xFFFFFFFF, "終了位置"),
            new CEventMoveType(0xFFFFFFFF, "移動タイプ")
    };

    public Integer fieldTableAddress = 0x00C37E28;
    private ClariasInfoCollection startTerm = new ClariasInfoCollection();
    private ClariasInfoCollection endTerm = new ClariasInfoCollection();
    private Integer int1;

    public CEventMove(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.startTerm = new ClariasInfoCollection();
        this.startTerm.readCollection(reader);
        this.endTerm = new ClariasInfoCollection();
        this.endTerm.readCollection(reader);
        this.int1 = reader.readInt();
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        this.startTerm.writeCollection(writer);
        this.endTerm.writeCollection(writer);
        writer.writeInt(this.int1);
    }
}
