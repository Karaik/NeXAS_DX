package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class SkillInfoGeneric extends SkillInfoObject {
    public String className;
    public Integer fieldTableAddress;
    private List<Integer> intFields = new ArrayList<>();

    public SkillInfoGeneric(Integer typeId) {
        super(typeId);
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        throw new UnsupportedOperationException("CLARIAS CEvent type " + getTypeId() + " is not structurally restored yet");
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        for (Integer value : intFields) {
            writer.writeInt(value);
        }
    }
}
