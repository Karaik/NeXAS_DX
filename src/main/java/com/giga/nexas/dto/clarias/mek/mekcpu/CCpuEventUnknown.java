package com.giga.nexas.dto.clarias.mek.mekcpu;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.Data;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
public class CCpuEventUnknown extends CCpuEvent {

    private Boolean parsedCommonFields = false;
    private List<Integer> trailingIntFields = new ArrayList<>();
    private Short trailingShortField;
    private Byte trailingByteField;

    @Override
    public void readInfo(BinaryReader reader) {
        this.trailingIntFields.clear();
        this.trailingShortField = null;
        this.trailingByteField = null;
        this.parsedCommonFields = false;

        int start = reader.getPosition();
        try {
            super.readInfo(reader);
            this.parsedCommonFields = true;
        } catch (Exception ignored) {
            reader.seek(start);
        }

        while (reader.remaining() >= 4) {
            this.trailingIntFields.add(reader.readInt());
        }
        if (reader.remaining() >= 2) {
            this.trailingShortField = reader.readShort();
        }
        if (reader.remaining() >= 1) {
            this.trailingByteField = reader.readByte();
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        if (Boolean.TRUE.equals(this.parsedCommonFields)) {
            super.writeInfo(writer);
        }
        for (Integer value : trailingIntFields) {
            writer.writeInt(value == null ? 0 : value);
        }
        if (this.trailingShortField != null) {
            writer.writeShort(this.trailingShortField);
        }
        if (this.trailingByteField != null) {
            writer.writeByte(this.trailingByteField);
        }
    }
}
