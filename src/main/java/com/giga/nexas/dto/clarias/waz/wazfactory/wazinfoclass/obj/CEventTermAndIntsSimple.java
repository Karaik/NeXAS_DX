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
public class CEventTermAndIntsSimple extends SkillInfoObject {
    public String className;
    public Integer fieldTableAddress;
    public Integer intCount;
    private Byte enabled;
    private List<Integer> intFields = new ArrayList<>();

    public CEventTermAndIntsSimple(Integer typeId, String className, Integer fieldTableAddress, int intCount) {
        super(typeId);
        this.className = className;
        this.fieldTableAddress = fieldTableAddress;
        this.intCount = intCount;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.intFields = new ArrayList<>();
        for (int i = 0; i < this.intCount; i++) {
            this.intFields.add(reader.readInt());
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        for (Integer value : this.intFields) {
            writer.writeInt(value);
        }
    }
}
