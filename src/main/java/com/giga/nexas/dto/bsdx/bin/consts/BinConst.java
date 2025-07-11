package com.giga.nexas.dto.bsdx.bin.consts;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * @Author 这位同学(Karaik)
 * @Date 2025/6/5
 * @Description opcode & operand mapping by : @koukdw
 */
public class BinConst {

    // opcode
    public final static Map<Integer, String> OPCODE_MNEMONIC_MAP;
    public final static Map<String, Integer> MNEMONIC_OPCODE_MAP;
    static {
        Map<Integer, String> tmp = new HashMap<>(1 << 7, 1); // 128
        Map<String, Integer> rev = new HashMap<>();

        for (Opcode op : Opcode.values()) {
            tmp.put(op.code, op.name());
            rev.put(op.name(), op.code);
        }

        OPCODE_MNEMONIC_MAP = Collections.unmodifiableMap(tmp);
        MNEMONIC_OPCODE_MAP = Collections.unmodifiableMap(rev);
    }

    // native function (operand)
    public final static Map<Integer, String> OPERAND_MNEMONIC_MAP;
    public final static Map<String, Integer> MNEMONIC_OPERAND_MAP;
    static {
        Map<Integer, String> tmp = new HashMap<>(1 << 9, 1); // 512
        Map<String, Integer> rev = new HashMap<>();

        for (Operand fn : Operand.values()) {
            tmp.put(fn.code, fn.name());
            rev.put(fn.name(), fn.code);
        }

        OPERAND_MNEMONIC_MAP = Collections.unmodifiableMap(tmp);
        MNEMONIC_OPERAND_MAP = Collections.unmodifiableMap(rev);
    }

    // escape character (escapeType)
    public final static Map<String, String> TYPE_ESCAPE_MAP;
    public final static Map<String, String> ESCAPE_TYPE_MAP;
    static {
        Map<String, String> tmp = new HashMap<>(1 << 5, 1); // 32
        Map<String, String> rev = new HashMap<>();

        for (EscapeType et : EscapeType.values()) {
            tmp.put(et.escapeChar, et.name());
            rev.put(et.name(), et.escapeChar);
        }

        TYPE_ESCAPE_MAP = Collections.unmodifiableMap(tmp);
        ESCAPE_TYPE_MAP = Collections.unmodifiableMap(rev);
    }

    private BinConst() {}

}
