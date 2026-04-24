package com.giga.nexas.dto.clarias.waz.wazfactory.wazinfoclass.obj;

import com.giga.nexas.io.BinaryReader;
import com.giga.nexas.io.BinaryWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class CEventFieldTableGeneric extends SkillInfoObject {
    public String className;
    public Integer fieldTableAddress;
    private Byte enabled;
    private List<FieldValue> fields = new ArrayList<>();

    public CEventFieldTableGeneric(Integer typeId, String className, Integer fieldTableAddress) {
        super(typeId);
        this.className = className;
        this.fieldTableAddress = fieldTableAddress;
    }

    @Override
    public void readInfo(BinaryReader reader) {
        super.readInfo(reader);
        this.enabled = reader.readByte();
        this.fields = new ArrayList<>();
        for (FieldDef def : FieldDef.forTable(this.fieldTableAddress)) {
            if ((def.flags & 2) != 0) {
                continue;
            }
            FieldValue value = new FieldValue(def.kind, def.size, def.flags);
            if (def.kind == 0) {
                value.setBytes(reader.readBytes(def.size));
            } else if (def.kind == 1) {
                value.setInts(readInts(reader, 2));
            } else if (def.kind == 2) {
                CTerm term = new CTerm();
                term.readInfo(reader);
                value.setTerm(term);
            } else if (def.kind == 3) {
                CTermChange termChange = new CTermChange();
                termChange.readInfo(reader);
                value.setTermChange(termChange);
            } else if (def.kind == 4) {
                value.setInts(readInts(reader, 4));
            } else if (def.kind == 5) {
                value.setInts(readInts(reader, 6));
            } else if (def.kind == 6) {
                value.setInts(readInts(reader, 8));
            } else {
                throw new UnsupportedOperationException("CLARIAS field kind " + def.kind + " is not restored for type " + getTypeId());
            }
            this.fields.add(value);
        }
    }

    @Override
    public void writeInfo(BinaryWriter writer) throws IOException {
        super.writeInfo(writer);
        writer.writeByte(this.enabled);
        int valueIndex = 0;
        for (FieldDef def : FieldDef.forTable(this.fieldTableAddress)) {
            if ((def.flags & 1) != 0) {
                continue;
            }
            FieldValue value = this.fields.get(valueIndex++);
            if (def.kind == 0) {
                writer.writeBytes(value.getBytes());
            } else if (def.kind == 1 || def.kind == 4 || def.kind == 5) {
                for (Integer intValue : value.getInts()) {
                    writer.writeInt(intValue);
                }
            } else if (def.kind == 2) {
                value.getTerm().writeInfo(writer);
            } else if (def.kind == 3) {
                value.getTermChange().writeInfo(writer);
            } else if (def.kind == 6) {
                for (Integer intValue : value.getInts()) {
                    writer.writeInt(intValue);
                }
            } else {
                throw new UnsupportedOperationException("CLARIAS field kind " + def.kind + " is not restored for type " + getTypeId());
            }
        }
    }

    private static List<Integer> readInts(BinaryReader reader, int count) {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            values.add(reader.readInt());
        }
        return values;
    }

    @Data
    @NoArgsConstructor
    public static class FieldValue {
        private Integer kind;
        private Integer size;
        private Integer flags;
        private byte[] bytes;
        private List<Integer> ints = new ArrayList<>();
        private CTerm term;
        private CTermChange termChange;

        public FieldValue(Integer kind, Integer size, Integer flags) {
            this.kind = kind;
            this.size = size;
            this.flags = flags;
        }
    }

    @AllArgsConstructor
    private static class FieldDef {
        private final int kind;
        private final int size;
        private final int flags;

        private static List<FieldDef> forTable(int address) {
            return switch (address) {
                case 0x00C36E08 -> List.of(f(1, 0, 19), f(0, 1, 19), f(2, 0, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0));
                case 0x00C36EA8 -> List.of(f(1, 0, 19), f(0, 1, 19), f(0, 4, 0), f(0, 4, 0));
                case 0x00C36C60 -> List.of(f(3, 0, 0));
                case 0x00C36AF0 -> List.of(f(0, 56, 0), f(0, 12, 0), f(0, 4, 0), f(0, 2, 0));
                case 0x00C37450, 0x00C377C0 -> List.of(f(0, 1, 0), f(0, 1, 0));
                case 0x00C374B0 -> List.of(f(0, 2, 0));
                case 0x00C37820 -> List.of(f(0, 4, 0), f(0, 2, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0), f(0, 4, 0));
                case 0x00C37910 -> List.of(f(0, 4, 0), f(0, 4, 0));
                case 0x00C37970 -> List.of(f(0, 1, 0), f(0, 4, 0));
                case 0x00C37B68 -> List.of(f(0, 1, 0), f(0, 1, 0), f(0, 4, 0), f(0, 1, 0));
                case 0x00C37CE0 -> List.of(f(0, 4, 0), f(0, 4, 0), f(0, 4, 0));
                default -> throw new UnsupportedOperationException("CLARIAS field table " + Integer.toHexString(address) + " is not registered");
            };
        }

        private static FieldDef f(int kind, int size, int flags) {
            return new FieldDef(kind, size, flags);
        }
    }
}
