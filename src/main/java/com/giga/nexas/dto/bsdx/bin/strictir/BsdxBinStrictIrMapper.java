package com.giga.nexas.dto.bsdx.bin.strictir;

import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.consts.BinConst;
import com.giga.nexas.dto.bsdx.bin.consts.Opcode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 旧 {@link Bin} 模型与新 Instruction IR 层之间的映射器。
 *
 * <p>它处在旧 parser/generator 代码与新可逆主链之间，是整个重构中最底层的衔接点。
 * 这一层要求严格无损：{@code opcode/operand}、表区、constants、charset、tail bytes 都必须原样保留，
 * 否则上层 lifting/lowering 再正确也无法做到真正 round-trip。
 */
public final class BsdxBinStrictIrMapper {

    /**
     * 把解析后的 {@link Bin} 提升为 Instruction IR。
     *
     * <p>这是整条 round-trip 链中的 {@code bin -> strict IR} 段。
     * 这里只做结构整理，不改写指令语义，因此要求无损。
     */
    public BsdxBinStrictIr toStrictIr(Bin bin) {
        if (bin == null) {
            return null;
        }

        return new BsdxBinStrictIr(
                bin.getExtensionName(),
                bin.getCharset(),
                bin.getPreCount() == null ? 0 : bin.getPreCount(),
                toIntList(bin.getPreInstructions()),
                toInstructionIr(bin.getInstructions()),
                new BsdxBinStrictIrDataSection(
                        copyStringList(bin.getStringTable()),
                        copyStringList(bin.getProperties()),
                        copyStringList(bin.getProperties2()),
                        toTableIr(bin.getTable()),
                        toMapList(bin.getConstants()),
                        toMapList(bin.getConstants2())
                ),
                bin.entryPointIndices == null ? List.of() : List.copyOf(bin.entryPointIndices),
                toIntList(bin.tailRaw)
        );
    }

    /**
     * 把 Instruction IR 降回旧的 {@link Bin} 模型，交给现有 generator 回写。
     *
     * <p>这是整条 round-trip 链中的 {@code strict IR -> bin} 段。
     * 这一段同样要求无损。
     */
    public Bin fromStrictIr(BsdxBinStrictIr ir) {
        if (ir == null) {
            return null;
        }

        Bin bin = new Bin();
        bin.setExtensionName(ir.extensionName());
        bin.setCharset(ir.charset());
        bin.setPreCount(ir.preCount());
        bin.setPreInstructions(toByteArray(ir.preInstructions()));
        bin.setInstructions(fromInstructionIr(ir.instructions()));
        if (ir.dataSection() != null) {
            bin.setStringTable(copyStringList(ir.dataSection().stringTable()));
            bin.setProperties(copyStringList(ir.dataSection().properties()));
            bin.setProperties2(copyStringList(ir.dataSection().properties2()));
            bin.setTable(fromTableIr(ir.dataSection().tables()));
            bin.setConstants(fromMapList(ir.dataSection().constants()));
            bin.setConstants2(fromMapList(ir.dataSection().constants2()));
        }
        bin.entryPointIndices = ir.entryPointIndices() == null ? null : new ArrayList<>(ir.entryPointIndices());
        bin.tailRaw = toByteArray(ir.tailRaw());

        List<Bin.Instruction> entryPoints = new ArrayList<>();
        if (bin.getInstructions() != null) {
            for (Bin.Instruction instruction : bin.getInstructions()) {
                if (instruction.getOpcodeNum() == Opcode.START.code) {
                    entryPoints.add(instruction);
                }
            }
        }
        bin.setEntryPoints(entryPoints);
        return bin;
    }

    /**
     * 把旧 BIN 指令对象转换成 Instruction IR 记录。
     *
     * <p>其中 CALL 指令会额外带上 arity/nativeId 等拆包字段，
     * 这样上层就不必反复解析 operand 位域。
     */
    private List<BsdxBinStrictIrInstruction> toInstructionIr(List<Bin.Instruction> instructions) {
        if (instructions == null) {
            return List.of();
        }
        List<BsdxBinStrictIrInstruction> result = new ArrayList<>(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            Bin.Instruction inst = instructions.get(i);
            if (inst == null) {
                continue;
            }
            Integer arity = null;
            Integer nativeId = null;
            String nativeMnemonic = null;
            if (inst.getOpcodeNum() == Opcode.CALL.code) {
                arity = inst.getOperandNum() >>> 16;
                nativeId = inst.getOperandNum() & 0xFFFF;
                nativeMnemonic = BinConst.OPERAND_MNEMONIC_MAP.get(nativeId);
            }
            result.add(new BsdxBinStrictIrInstruction(
                    i,
                    inst.getOpcodeNum(),
                    inst.getOperandNum(),
                    BinConst.OPCODE_MNEMONIC_MAP.get(inst.getOpcodeNum()),
                    arity,
                    nativeId,
                    nativeMnemonic
            ));
        }
        return result;
    }

    /**
     * 从 Instruction IR 重建旧的 {@link Bin.Instruction} 对象。
     *
     * <p>核心的 opcode/operand 会原样保留，只补回旧模型所需的辅助字段，
     * 比如 {@code paramCount/nativeId/nativeFunction}。
     */
    private List<Bin.Instruction> fromInstructionIr(List<BsdxBinStrictIrInstruction> instructions) {
        if (instructions == null) {
            return List.of();
        }
        List<Bin.Instruction> result = new ArrayList<>(instructions.size());
        for (int i = 0; i < instructions.size(); i++) {
            BsdxBinStrictIrInstruction ir = instructions.get(i);
            Bin.Instruction inst = new Bin.Instruction();
            inst.setIndex(i);
            inst.setOpcodeNum(ir.opcodeNum());
            inst.setOperandNum(ir.operandNum());
            inst.setOpcode(ir.opcodeMnemonic() != null ? ir.opcodeMnemonic() : Integer.toString(ir.opcodeNum()));
            if (ir.opcodeNum() == Opcode.CALL.code) {
                int nativeId = ir.callNativeId() != null ? ir.callNativeId() : (ir.operandNum() & 0xFFFF);
                inst.setParamCount(ir.callArity() != null ? ir.callArity() : (ir.operandNum() >>> 16));
                inst.setNativeId(nativeId);
                inst.setNativeFunction(ir.callNativeMnemonic() != null ? ir.callNativeMnemonic() : Integer.toString(nativeId));
            } else {
                inst.setParamCount(0);
                inst.setNativeFunction(null);
            }
            result.add(inst);
        }
        return result;
    }

    /**
     * 把原始 68 字节表块转换成无符号整数列表，便于安全挂进 record。
     */
    private List<BsdxBinStrictIrTableEntry> toTableIr(List<byte[]> tables) {
        if (tables == null) {
            return List.of();
        }
        List<BsdxBinStrictIrTableEntry> result = new ArrayList<>(tables.size());
        for (int i = 0; i < tables.size(); i++) {
            result.add(new BsdxBinStrictIrTableEntry(i, toIntList(tables.get(i))));
        }
        return result;
    }

    /**
     * 在 lowering 回 {@link Bin} 时，把表块重新组装成原始字节数组。
     */
    private List<byte[]> fromTableIr(List<BsdxBinStrictIrTableEntry> tables) {
        if (tables == null) {
            return List.of();
        }
        List<byte[]> result = new ArrayList<>(tables.size());
        for (BsdxBinStrictIrTableEntry entry : tables) {
            result.add(toByteArray(entry.raw68()));
        }
        return result;
    }

    /**
     * 把 decoded constant map 转成不可变整数列表，并保留 key 顺序。
     */
    private Map<Integer, List<Integer>> toMapList(Map<Integer, Integer[]> source) {
        if (source == null) {
            return Map.of();
        }
        Map<Integer, List<Integer>> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, Integer[]> entry : source.entrySet()) {
            List<Integer> values = new ArrayList<>();
            if (entry.getValue() != null) {
                for (Integer value : entry.getValue()) {
                    values.add(value);
                }
            }
            result.put(entry.getKey(), List.copyOf(values));
        }
        return result;
    }

    /**
     * 把不可变整数列表常量转回旧 BIN 类使用的数组形式。
     */
    private Map<Integer, Integer[]> fromMapList(Map<Integer, List<Integer>> source) {
        if (source == null) {
            return Map.of();
        }
        Map<Integer, Integer[]> result = new LinkedHashMap<>();
        for (Map.Entry<Integer, List<Integer>> entry : source.entrySet()) {
            result.put(entry.getKey(), entry.getValue().toArray(Integer[]::new));
        }
        return result;
    }

    /**
     * 把原始字节镜像成无符号整数列表，便于放进 record 组件。
     */
    private List<Integer> toIntList(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            return List.of();
        }
        List<Integer> result = new ArrayList<>(bytes.length);
        for (byte value : bytes) {
            result.add(value & 0xFF);
        }
        return List.copyOf(result);
    }

    /**
     * 把无符号整数镜像恢复成 generator 需要的原始字节数组。
     */
    private byte[] toByteArray(List<Integer> values) {
        if (values == null || values.isEmpty()) {
            return new byte[0];
        }
        byte[] result = new byte[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = (byte) (values.get(i) & 0xFF);
        }
        return result;
    }

    /**
     * 把可能为 null 的字符串表规范成不可变列表，不改变内容本身。
     */
    private List<String> copyStringList(List<String> source) {
        return source == null ? List.of() : List.copyOf(source);
    }
}
