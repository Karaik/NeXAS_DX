package com.giga.nexas.dto.bsdx.bin.pseudo.renderer;

import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.BsdxGlobalSymbolUtil;
import com.giga.nexas.dto.bsdx.bin.consts.BinConst;
import com.giga.nexas.dto.bsdx.bin.consts.Opcode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Renders a parsed BSDX BIN into readable pseudo code.
 *
 * <p>This is intentionally a one-way renderer for now. It aims to recover the
 * control-flow and syscall shape needed to inspect and later edit level BIN
 * files, while keeping unknown instructions lossless via raw fallback lines.</p>
 */
public class BsdxBinRenderer {

    private static final int OPCODE_DIRECT_JUMP = 0x3F;
    private static final int OPCODE_DIRECT_JUMP_IF_FALSE = 0x40;
    private static final int OPCODE_DIRECT_JUMP_IF_TRUE = 0x41;

    public String renderPseudo(Bin bin) {
        if (bin == null || bin.getInstructions() == null || bin.getInstructions().isEmpty()) {
            return renderConstants(bin);
        }

        StringBuilder out = new StringBuilder();
        RenderState state = new RenderState();
        Map<Integer, String> labels = collectLabels(bin.getInstructions());

        List<Bin.Instruction> instructions = bin.getInstructions();
        for (int index = 0; index < instructions.size(); index++) {
            if (labels.containsKey(index)) {
                flushPending(out, state, true);
                out.append("label ").append(labels.get(index)).append("\r\n");
            }
            renderInstruction(out, bin, instructions, index, state, labels);
        }

        flushPending(out, state, true);
        out.append(renderConstants(bin));
        return out.toString();
    }

    public String renderAssembly(Bin bin) {
        if (bin == null || bin.getInstructions() == null) {
            return "";
        }

        StringBuilder out = new StringBuilder();
        List<Bin.Instruction> instructions = bin.getInstructions();
        for (int i = 0; i < instructions.size(); i++) {
            Bin.Instruction inst = instructions.get(i);
            int opcodeNum = inst.getOpcodeNum();
            int operandNum = inst.getOperandNum();
            String opcode = inst.getOpcode() != null ? inst.getOpcode() : String.format(Locale.ROOT, "0x%04X", opcodeNum);
            out.append(String.format(Locale.ROOT, "%04d: %-16s %d", i, opcode, operandNum));
            if (opcodeNum == Opcode.CALL.code) {
                out.append(" ; ");
                out.append(formatCallTarget(inst));
                out.append(" argc=").append(resolveParamCount(inst));
            }
            out.append("\r\n");
        }
        return out.toString();
    }

    public String renderStrictPseudo(Bin bin) {
        return renderAssembly(bin);
    }

    private void renderInstruction(
            StringBuilder out,
            Bin bin,
            List<Bin.Instruction> instructions,
            int index,
            RenderState state,
            Map<Integer, String> labels
    ) {
        Bin.Instruction inst = instructions.get(index);
        int opcode = inst.getOpcodeNum();
        int operand = inst.getOperandNum();
        String raw = encodeRaw(index, inst);

        switch (opcode) {
            case 0:
                state.pendingRaw.add(raw);
                flushPending(out, state, false);
                state.r0 = Integer.toString(operand);
                state.r0Literal = operand;
                break;
            case 1:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "jmp 0x%X", operand), List.of(raw));
                break;
            case 2:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "if(%s) jmp 0x%X", exprOrRegister(state.r0, "eax"), operand),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 3:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "lb 0x%X", operand), List.of(raw));
                break;
            case 4:
                state.pendingRaw.add(raw);
                if (operand == 0) {
                    state.arithmeticStack.push(exprOrRegister(state.r0, "eax"));
                    clearR0(state);
                } else if (operand == 1) {
                    state.arithmeticStack.push(exprOrRegister(state.r1, "ebx"));
                    clearR1(state);
                } else {
                    appendLine(out, formatRawText(inst), List.of(raw));
                }
                break;
            case 5:
                state.pendingRaw.add(raw);
                if (operand == 0) {
                    state.parameterStack.push(exprOrRegister(state.r0, "eax"));
                    clearR0(state);
                } else if (operand == 1) {
                    state.parameterStack.push(resolveStringLiteral(bin, state.r0Literal, state.r0));
                    clearR0(state);
                } else {
                    appendLine(out, formatRawText(inst), List.of(raw));
                }
                break;
            case 6:
                state.pendingRaw.add(raw);
                if (operand == 0) {
                    state.r0 = state.arithmeticStack.isEmpty() ? "eax" : state.arithmeticStack.pop();
                    state.r0Literal = tryParseInt(state.r0);
                } else if (operand == 1) {
                    state.r1 = state.arithmeticStack.isEmpty() ? "ebx" : state.arithmeticStack.pop();
                    state.r1Literal = tryParseInt(state.r1);
                } else {
                    appendLine(out, formatRawText(inst), List.of(raw));
                }
                break;
            case 7:
                state.pendingRaw.add(raw);
                flushPendingSyscall(out, instructions, index, state);
                state.r0 = buildSyscallExpression(inst, state);
                state.r0Literal = null;
                break;
            case 8:
                state.pendingRaw.add(raw);
                if (operand == 0) {
                    state.r0 = resolvePropertyReference(bin, state.r0Literal, state.r0);
                    state.r0Literal = null;
                } else if (operand == 1) {
                    state.r1 = resolvePropertyReference(bin, state.r1Literal, state.r1);
                    state.r1Literal = null;
                } else {
                    appendLine(out, formatRawText(inst), List.of(raw));
                }
                break;
            case 9:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s + %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0xA:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s - %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0xB:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s * %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0xC:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s / %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0xD:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s %% %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0xE:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s = %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0xF:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s || %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x10:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s && %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x11:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s | %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x12:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s & %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x13:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s ^ %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x14:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(~%s)", exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x15:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s <= %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x16:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s >= %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x17:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s < %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x18:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s > %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x19:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s == %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x1A:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s != %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x1B:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "entry %d", operand), List.of(raw));
                break;
            case 0x1D:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "marker %d", operand), List.of(raw));
                break;
            case 0x21:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "if(%s) jmp2 0x%X", exprOrRegister(state.r0, "eax"), operand),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x22:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "%s++", propertyName(bin, operand)), List.of(raw));
                break;
            case 0x23:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "%s--", propertyName(bin, operand)), List.of(raw));
                break;
            case 0x27:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "if(%s) jmp3 0x%X", exprOrRegister(state.r0, "eax"), operand),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x2B:
                state.pendingRaw.add(raw);
                state.r0 = "!" + exprOrRegister(state.r0, "eax");
                state.r0Literal = null;
                break;
            case 0x2C:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "lineno %d", operand), List.of(raw));
                break;
            case 0x2D:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s >> %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x2E:
                state.pendingRaw.add(raw);
                state.r0 = String.format(Locale.ROOT, "(%s << %s)", exprOrRegister(state.r1, "ebx"), exprOrRegister(state.r0, "eax"));
                state.r0Literal = null;
                break;
            case 0x2F:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s += %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x30:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s -= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x31:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s *= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x32:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s /= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x33:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s %%= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x34:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s |= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x35:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s &= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x36:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s ^= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x37:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s >>= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case 0x38:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "%s <<= %s", propertyName(bin, operand), exprOrRegister(state.r0, "eax")),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case OPCODE_DIRECT_JUMP:
                flushPending(out, state, true);
                appendLine(out, String.format(Locale.ROOT, "jmp_direct %s", labelName(labels, operand + 1)), List.of(raw));
                break;
            case OPCODE_DIRECT_JUMP_IF_FALSE:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "if(!%s) jmp_direct %s", exprOrRegister(state.r0, "eax"), labelName(labels, operand + 1)),
                        takePendingRaw(state));
                clearR0(state);
                break;
            case OPCODE_DIRECT_JUMP_IF_TRUE:
                state.pendingRaw.add(raw);
                appendLine(out,
                        String.format(Locale.ROOT, "if(%s) jmp_direct %s", exprOrRegister(state.r0, "eax"), labelName(labels, operand + 1)),
                        takePendingRaw(state));
                clearR0(state);
                break;
            default:
                flushPending(out, state, false);
                appendLine(out, formatRawText(inst), List.of(raw));
                break;
        }
    }

    private String buildSyscallExpression(Bin.Instruction inst, RenderState state) {
        int paramCount = resolveParamCount(inst);
        List<String> params = new ArrayList<>(paramCount);
        for (int i = 0; i < paramCount; i++) {
            params.add(0, state.parameterStack.isEmpty() ? "<?>" : state.parameterStack.pop());
        }
        return "syscall " + formatCallTarget(inst) + "(" + String.join(", ", params) + ")";
    }

    private int resolveParamCount(Bin.Instruction inst) {
        if (inst.getParamCount() != null) {
            return inst.getParamCount();
        }
        return inst.getOperandNum() >>> 16;
    }

    private String formatCallTarget(Bin.Instruction inst) {
        int nativeId = inst.getNativeId() != null ? inst.getNativeId() : (inst.getOperandNum() & 0xFFFF);
        String name = inst.getNativeFunction();
        if (name == null || name.isBlank()) {
            name = BinConst.OPERAND_MNEMONIC_MAP.get(nativeId);
        }
        if (name != null && !name.isBlank() && !isNumeric(name)) {
            return name;
        }
        return String.format(Locale.ROOT, "0x%04X", nativeId & 0xFFFF);
    }

    private Map<Integer, String> collectLabels(List<Bin.Instruction> instructions) {
        Map<Integer, String> labels = new HashMap<>();
        int nextIndex = 0;
        for (Bin.Instruction inst : instructions) {
            if (inst == null) {
                continue;
            }
            int opcode = inst.getOpcodeNum();
            if (opcode != OPCODE_DIRECT_JUMP
                    && opcode != OPCODE_DIRECT_JUMP_IF_FALSE
                    && opcode != OPCODE_DIRECT_JUMP_IF_TRUE) {
                continue;
            }
            int target = inst.getOperandNum() + 1;
            if (!labels.containsKey(target)) {
                labels.put(target, "label_" + nextIndex++);
            }
        }
        return labels;
    }

    private String renderConstants(Bin bin) {
        if (bin == null || bin.getConstants() == null || bin.getConstants().isEmpty()) {
            return "";
        }
        StringBuilder out = new StringBuilder();
        bin.getConstants().entrySet().stream()
                .sorted(Comparator.comparingInt(Map.Entry::getKey))
                .forEach(entry -> {
                    List<String> values = new ArrayList<>();
                    Integer[] refs = entry.getValue();
                    if (refs != null) {
                        for (Integer ref : refs) {
                            values.add(propertyName(bin, ref == null ? -1 : ref));
                        }
                    }
                    appendLine(out,
                            "global[" + renderGlobalConstantIndex(bin, entry.getKey()) + "] = {" + String.join(",", values) + "}",
                            List.of("CONST:" + entry.getKey() + ":" + String.join(",", values)));
                });
        return out.toString();
    }

    private void flushPending(StringBuilder out, RenderState state, boolean includeExpression) {
        if (state.r0 == null || state.r0.isBlank()) {
            return;
        }
        if (state.r0.startsWith("syscall ")) {
            appendLine(out, state.r0, takePendingRaw(state));
            clearR0(state);
            return;
        }
        if (includeExpression) {
            appendLine(out, "eax = " + state.r0, takePendingRaw(state));
            clearR0(state);
        }
    }

    private void flushPendingSyscall(StringBuilder out, List<Bin.Instruction> instructions, int index, RenderState state) {
        if (state.r0 == null || !state.r0.startsWith("syscall ")) {
            return;
        }
        if (index > 0 && instructions.get(index - 1).getOpcodeNum() == Opcode.PARAM.code) {
            return;
        }
        appendLine(out, state.r0, takePendingRaw(state));
        clearR0(state);
    }

    private String propertyName(Bin bin, int index) {
        List<String> properties = bin == null ? null : bin.getProperties();
        if (properties != null && index >= 0 && index < properties.size()) {
            String name = properties.get(index);
            if (isSafeIdentifier(name)) {
                return name;
            }
            return BsdxGlobalSymbolUtil.aliasForProperty(index, name);
        }
        String globalAlias = globalSymbolAlias(bin, index);
        if (globalAlias != null) {
            return globalAlias;
        }
        return "p[" + index + "]";
    }

    private String resolvePropertyReference(Bin bin, Integer literalIndex, String expression) {
        Integer index = literalIndex != null ? literalIndex : tryParseInt(expression);
        if (index != null) {
            return propertyReference(bin, index);
        }
        return "p[" + exprOrRegister(expression, "eax") + "]";
    }

    private String propertyReference(Bin bin, int index) {
        return propertyName(bin, index);
    }

    /**
     * 把 `__GLOBAL.bin` 的原始全局符号转成旧 pseudo 可安全显示的别名。
     */
    private String globalSymbolAlias(Bin bin, int index) {
        if (bin == null || bin.getGlobalSymbols() == null) {
            return null;
        }
        return BsdxGlobalSymbolUtil.aliasFor(bin.getGlobalSymbols(), index);
    }

    /**
     * 为常量表左值生成可回编的 `global[...]` 索引文本。
     *
     * <p>如果同目录 `__GLOBAL.bin` 里存在符号名，就显示别名；否则回退到原始数字索引。
     */
    private String renderGlobalConstantIndex(Bin bin, int index) {
        String alias = globalSymbolAlias(bin, index);
        return alias != null ? alias : Integer.toString(index);
    }

    private String resolveStringLiteral(Bin bin, Integer literalIndex, String expression) {
        Integer index = literalIndex != null ? literalIndex : tryParseInt(expression);
        if (index != null) {
            long unsigned = Integer.toUnsignedLong(index);
            if (unsigned >= 0x80000000L) {
                int properties2Index = index & 0x0FFFFFFF;
                if (bin != null && bin.getProperties2() != null && properties2Index >= 0 && properties2Index < bin.getProperties2().size()) {
                    return quote(bin.getProperties2().get(properties2Index));
                }
            } else if (bin != null && bin.getStringTable() != null && index >= 0 && index < bin.getStringTable().size()) {
                return quote(bin.getStringTable().get(index));
            }
        }
        return "string[" + exprOrRegister(expression, "eax") + "]";
    }

    private String labelName(Map<Integer, String> labels, int target) {
        return labels.getOrDefault(target, "label_" + target);
    }

    private String exprOrRegister(String expression, String registerName) {
        return expression == null || expression.isBlank() ? registerName : expression;
    }

    private Integer tryParseInt(String value) {
        if (value == null || value.isBlank() || !isNumeric(value.trim())) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private boolean isNumeric(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        String trimmed = value.trim();
        int start = trimmed.startsWith("-") ? 1 : 0;
        if (start == trimmed.length()) {
            return false;
        }
        for (int i = start; i < trimmed.length(); i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    private boolean isSafeIdentifier(String value) {
        if (value == null || value.isBlank()) {
            return false;
        }
        char first = value.charAt(0);
        if (!(Character.isLetter(first) || first == '_')) {
            return false;
        }
        for (int i = 1; i < value.length(); i++) {
            char c = value.charAt(i);
            if (!(Character.isLetterOrDigit(c) || c == '_')) {
                return false;
            }
        }
        return true;
    }

    private String quote(String value) {
        String safe = value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + safe + "\"";
    }

    private void appendLine(StringBuilder out, String content, List<String> rawEntries) {
        out.append(content);
        if (rawEntries != null && !rawEntries.isEmpty()) {
            out.append(" // IRHASH=").append(Integer.toHexString(content.hashCode()));
            out.append(" IR=").append(String.join("|", rawEntries));
        }
        out.append("\r\n");
    }

    private String formatRawText(Bin.Instruction inst) {
        return String.format(Locale.ROOT, "0x%04X,%d", inst.getOpcodeNum(), inst.getOperandNum());
    }

    private String encodeRaw(int index, Bin.Instruction inst) {
        return index + "@" + inst.getOpcodeNum() + "," + inst.getOperandNum();
    }

    private List<String> takePendingRaw(RenderState state) {
        List<String> raw = new ArrayList<>(state.pendingRaw);
        state.pendingRaw.clear();
        return raw;
    }

    private void clearR0(RenderState state) {
        state.r0 = "";
        state.r0Literal = null;
    }

    private void clearR1(RenderState state) {
        state.r1 = "";
        state.r1Literal = null;
    }

    private static final class RenderState {
        private String r0 = "";
        private String r1 = "";
        private Integer r0Literal;
        private Integer r1Literal;
        private final Deque<String> arithmeticStack = new ArrayDeque<>();
        private final Deque<String> parameterStack = new ArrayDeque<>();
        private final List<String> pendingRaw = new ArrayList<>();
    }
}
