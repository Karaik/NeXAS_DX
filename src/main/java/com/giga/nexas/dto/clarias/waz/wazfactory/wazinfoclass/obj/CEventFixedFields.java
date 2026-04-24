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
public class CEventFixedFields extends SkillInfoObject {
    public String className;
    public Integer fieldTableAddress;
    public String fieldLayout;
    private Byte enabled;
    private List<Integer> intFields = new ArrayList<>();
    private List<Short> shortFields = new ArrayList<>();
    private List<Byte> byteFields = new ArrayList<>();

    public CEventFixedFields(Integer typeId, String className, Integer fieldTableAddress, String fieldLayout) {
        super(typeId);
        this.className = className;
        this.fieldTableAddress = fieldTableAddress;
        this.fieldLayout = fieldLayout;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.intFields = new ArrayList<>();
        this.shortFields = new ArrayList<>();
        this.byteFields = new ArrayList<>();
        for (int i = 0; i < this.fieldLayout.length(); i++) {
            char kind = this.fieldLayout.charAt(i);
            if (kind == 'i') {
                this.intFields.add(reader.readInt());
            } else if (kind == 's') {
                this.shortFields.add(reader.readShort());
            } else if (kind == 'b') {
                this.byteFields.add(reader.readByte());
            } else {
                throw new IllegalStateException("unknown CLARIAS fixed field kind: " + kind);
            }
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        int intIndex = 0;
        int shortIndex = 0;
        int byteIndex = 0;
        for (int i = 0; i < this.fieldLayout.length(); i++) {
            char kind = this.fieldLayout.charAt(i);
            if (kind == 'i') {
                writer.writeInt(this.intFields.get(intIndex++));
            } else if (kind == 's') {
                writer.writeShort(this.shortFields.get(shortIndex++));
            } else if (kind == 'b') {
                writer.writeByte(this.byteFields.get(byteIndex++));
            } else {
                throw new IllegalStateException("unknown CLARIAS fixed field kind: " + kind);
            }
        }
    }
}