package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;

@Data
@NoArgsConstructor
public class CEventValRandom extends SkillInfoObject {
    public String className = "CEventValRandom";
    public Integer fieldTableAddress = 0x00C36420;
    public Integer intCount = 6;
    private Byte enabled;
    private java.util.List<Integer> intFields = new java.util.ArrayList<>();

    public CEventValRandom(Integer typeId) {
        super(typeId);
    }

    public CEventValRandom(Integer typeId, int intCount) {
        super(typeId);
        this.intCount = intCount;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.intFields = new java.util.ArrayList<>();
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
