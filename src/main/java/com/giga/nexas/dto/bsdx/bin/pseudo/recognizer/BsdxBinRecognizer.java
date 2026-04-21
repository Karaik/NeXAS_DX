package com.giga.nexas.dto.bsdx.bin.pseudo.recognizer;

import com.giga.nexas.dto.bsdx.bin.Bin;
import com.giga.nexas.dto.bsdx.bin.BsdxGlobalSymbolUtil;
import com.giga.nexas.dto.bsdx.bin.consts.BinConst;
import com.giga.nexas.dto.bsdx.bin.consts.Opcode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Compiles pseudo code produced by {@code BsdxBinRenderer} back into {@link Bin}.
 */
public class BsdxBinRecognizer {

    private static final int OPCODE_DIRECT_JUMP = 0x3F;
    private static final int OPCODE_DIRECT_JUMP_IF_FALSE = 0x40;
    private static final int OPCODE_DIRECT_JUMP_IF_TRUE = 0x41;

    public Bin compile(Bin template, String pseudoSource) {
        CompilerContext context = new CompilerContext(template);
        List<String> lines = splitLines(pseudoSource);
        int lineNumber = 0;
        for (String rawLine : lines) {
            lineNumber++;
            PseudoLine pseudoLine = parsePseudoLine(rawLine);
            String line = pseudoLine.content().trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                if (tryReplayLosslessPseudo(pseudoLine, context)) {
                    continue;
                }
                compileLine(line, context);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "line " + lineNumber + ": " + line + " -> " + ex.getMessage(),
                        ex
                );
            }
        }
        context.resolveLabels();
        return context.build();
    }

    public Bin compileStrict(Bin template, String strictSource) {
        CompilerContext context = new CompilerContext(template);
        List<String> lines = splitLines(strictSource);
        int lineNumber = 0;
        for (String rawLine : lines) {
            lineNumber++;
            String line = stripStrictComment(rawLine).trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                compileStrictLine(line, context);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "strict line " + lineNumber + ": " + line + " -> " + ex.getMessage(),
                        ex
                );
            }
        }
        context.resolveLabels();
        return context.build();
    }

    private void compileLine(String line, CompilerContext context) {
        if (line.startsWith("global[")) {
            compileGlobal(line, context);
            return;
        }
        if (line.startsWith("label ")) {
            context.labels.put(line.substring("label ".length()).trim(), context.instructions.size());
            return;
        }
        if (line.startsWith("entry ")) {
            context.addInstruction(Opcode.START.code, parseFlexibleInt(line.substring("entry ".length()).trim()));
            return;
        }
        if (line.startsWith("lb ")) {
            context.addInstruction(3, parseFlexibleInt(line.substring("lb ".length()).trim()));
            return;
        }
        if (line.startsWith("jmp_direct ")) {
            int index = context.addInstruction(OPCODE_DIRECT_JUMP, 0);
            context.unresolvedLabels.put(index, line.substring("jmp_direct ".length()).trim());
            return;
        }
        if (line.startsWith("jmp ")) {
            context.addInstruction(1, parseFlexibleInt(line.substring("jmp ".length()).trim()));
            return;
        }
        if (line.startsWith("marker ")) {
            context.addInstruction(0x1D, parseFlexibleInt(line.substring("marker ".length()).trim()));
            return;
        }
        if (line.startsWith("lineno ")) {
            context.addInstruction(0x2C, parseFlexibleInt(line.substring("lineno ".length()).trim()));
            return;
        }
        if (line.startsWith("if(")) {
            compileConditionalJump(line, context);
            return;
        }
        if (line.endsWith("++")) {
            String name = line.substring(0, line.length() - 2).trim();
            context.addInstruction(0x22, context.requirePropertyIndex(name));
            return;
        }
        if (line.endsWith("--")) {
            String name = line.substring(0, line.length() - 2).trim();
            context.addInstruction(0x23, context.requirePropertyIndex(name));
            return;
        }
        if (isRawInstruction(line)) {
            compileRawInstruction(line, context);
            return;
        }

        Assignment assignment = parseAssignment(line);
        if (assignment != null) {
            compileAssignment(assignment, context);
            return;
        }

        emitExpression(parseExpression(line), context, EmitMode.R0);
    }

    private void compileStrictLine(String line, CompilerContext context) {
        int colon = line.indexOf(':');
        String body = colon >= 0 ? line.substring(colon + 1).trim() : line.trim();
        if (body.isEmpty()) {
            return;
        }

        String[] parts = body.split("\\s+", 3);
        if (parts.length < 2) {
            throw new IllegalArgumentException("Expected '<opcode> <operand>'");
        }

        String opcodeToken = parts[0];
        String operandToken = parts[1];

        int opcodeNum;
        Integer mapped = BinConst.MNEMONIC_OPCODE_MAP.get(opcodeToken);
        if (mapped != null) {
            opcodeNum = mapped;
        } else {
            opcodeNum = parseFlexibleInt(opcodeToken);
        }

        int operandNum = parseFlexibleInt(operandToken);
        context.addInstruction(opcodeNum, operandNum);
    }

    private boolean tryReplayLosslessPseudo(PseudoLine pseudoLine, CompilerContext context) {
        if (pseudoLine.irHash() == null || pseudoLine.irEntries().isEmpty()) {
            return false;
        }
        String content = pseudoLine.content().trim();
        String expectedHash = Integer.toHexString(content.hashCode());
        if (!expectedHash.equalsIgnoreCase(pseudoLine.irHash())) {
            return false;
        }

        if (pseudoLine.irEntries().size() == 1 && pseudoLine.irEntries().get(0).startsWith("CONST:")) {
            // Unmodified global lines keep the original 68-byte table/constants block untouched.
            return true;
        }

        for (String entry : pseudoLine.irEntries()) {
            int at = entry.indexOf('@');
            if (at >= 0) {
                int sourceIndex = parseFlexibleInt(entry.substring(0, at).trim());
                String raw = entry.substring(at + 1).trim();
                int comma = raw.indexOf(',');
                if (comma < 0) {
                    return false;
                }
                int opcodeNum = parseFlexibleInt(raw.substring(0, comma).trim());
                int operandNum = parseFlexibleInt(raw.substring(comma + 1).trim());
                context.replayInstruction(sourceIndex, opcodeNum, operandNum);
                continue;
            }
            int comma = entry.indexOf(',');
            if (comma < 0) {
                return false;
            }
            int opcodeNum = parseFlexibleInt(entry.substring(0, comma).trim());
            int operandNum = parseFlexibleInt(entry.substring(comma + 1).trim());
            context.replayInstruction(context.replayedInstructions.size(), opcodeNum, operandNum);
        }
        return true;
    }

    private void compileGlobal(String line, CompilerContext context) {
        int leftBracket = line.indexOf('[');
        int rightBracket = line.indexOf(']', leftBracket + 1);
        int leftBrace = line.indexOf('{', rightBracket + 1);
        int rightBrace = line.lastIndexOf('}');
        if (leftBracket < 0 || rightBracket < 0 || leftBrace < 0 || rightBrace < leftBrace) {
            throw new IllegalArgumentException("Invalid global line: " + line);
        }
        int index = context.resolveGlobalConstantIndex(line.substring(leftBracket + 1, rightBracket).trim());
        String body = line.substring(leftBrace + 1, rightBrace).trim();
        List<Integer> refs = new ArrayList<>();
        if (!body.isEmpty()) {
            for (String token : splitTopLevel(body, ',')) {
                String trimmed = token.trim();
                if (!trimmed.isEmpty()) {
                    refs.add(resolvePropertyReference(trimmed, context));
                }
            }
        }
        context.constants.put(index, refs.toArray(new Integer[0]));
        context.constantsTouched = true;
    }

    private int resolvePropertyReference(String token, CompilerContext context) {
        PropertyAccess access = tryParsePropertyAccess(token);
        if (access != null && access.indexExpr instanceof NumberExpr numberExpr) {
            return numberExpr.value;
        }
        return context.requirePropertyIndex(token);
    }

    private void compileConditionalJump(String line, CompilerContext context) {
        int close = findMatchingRightParen(line, 3);
        if (close < 0) {
            throw new IllegalArgumentException("Invalid conditional jump: " + line);
        }
        String conditionText = line.substring(3, close);
        String tail = line.substring(close + 1).trim();

        boolean negateDirect = false;
        Expr conditionExpr = parseExpression(conditionText);
        if (conditionExpr instanceof UnaryExpr unaryExpr
                && unaryExpr.operator == Operator.NOT
                && tail.startsWith("jmp_direct ")) {
            negateDirect = true;
            conditionExpr = unaryExpr.operand;
        }

        emitExpression(conditionExpr, context, EmitMode.R0);

        if (tail.startsWith("jmp_direct ")) {
            int index = context.addInstruction(
                    negateDirect ? OPCODE_DIRECT_JUMP_IF_FALSE : OPCODE_DIRECT_JUMP_IF_TRUE,
                    0
            );
            context.unresolvedLabels.put(index, tail.substring("jmp_direct ".length()).trim());
            return;
        }
        if (tail.startsWith("jmp2 ")) {
            context.addInstruction(0x21, parseFlexibleInt(tail.substring("jmp2 ".length()).trim()));
            return;
        }
        if (tail.startsWith("jmp3 ")) {
            context.addInstruction(0x27, parseFlexibleInt(tail.substring("jmp3 ".length()).trim()));
            return;
        }
        if (tail.startsWith("jmp ")) {
            context.addInstruction(2, parseFlexibleInt(tail.substring("jmp ".length()).trim()));
            return;
        }
        throw new IllegalArgumentException("Unsupported conditional jump tail: " + line);
    }

    private void compileRawInstruction(String line, CompilerContext context) {
        int comma = line.indexOf(',');
        int opcode = parseFlexibleInt(line.substring(0, comma).trim());
        int operand = parseFlexibleInt(line.substring(comma + 1).trim());
        context.addInstruction(opcode, operand);
    }

    private Assignment parseAssignment(String line) {
        List<String> operators = Arrays.asList(">>=", "<<=", "+=", "-=", "*=", "/=", "%=", "|=", "&=", "^=", "=");
        for (String op : operators) {
            int idx = findTopLevelOperator(line, op);
            if (idx >= 0) {
                String left = line.substring(0, idx).trim();
                String right = line.substring(idx + op.length()).trim();
                return new Assignment(left, op, right);
            }
        }
        return null;
    }

    private void compileAssignment(Assignment assignment, CompilerContext context) {
        Expr right = parseExpression(assignment.right);
        if ("=".equals(assignment.operator) && "eax".equals(assignment.left)) {
            emitExpression(right, context, EmitMode.R0);
            return;
        }

        int propertyIndex = context.requirePropertyIndex(assignment.left);
        emitExpression(right, context, EmitMode.R0);
        switch (assignment.operator) {
            case "=" -> context.addInstruction(0xE, propertyIndex);
            case "+=" -> context.addInstruction(0x2F, propertyIndex);
            case "-=" -> context.addInstruction(0x30, propertyIndex);
            case "*=" -> context.addInstruction(0x31, propertyIndex);
            case "/=" -> context.addInstruction(0x32, propertyIndex);
            case "%=" -> context.addInstruction(0x33, propertyIndex);
            case "|=" -> context.addInstruction(0x34, propertyIndex);
            case "&=" -> context.addInstruction(0x35, propertyIndex);
            case "^=" -> context.addInstruction(0x36, propertyIndex);
            case ">>=" -> context.addInstruction(0x37, propertyIndex);
            case "<<=" -> context.addInstruction(0x38, propertyIndex);
            default -> throw new IllegalArgumentException("Unsupported assignment operator: " + assignment.operator);
        }
    }

    private void emitExpression(Expr expr, CompilerContext context, EmitMode mode) {
        if (expr instanceof NumberExpr numberExpr) {
            context.addInstruction(0, numberExpr.value);
            return;
        }
        if (expr instanceof StringLiteralExpr stringExpr) {
            context.addInstruction(0, context.requireStringIndex(stringExpr.value));
            return;
        }
        if (expr instanceof IdentifierExpr identifierExpr) {
            if ("eax".equals(identifierExpr.name)) {
                return;
            }
            if ("ebx".equals(identifierExpr.name)) {
                if (mode == EmitMode.R1) {
                    return;
                }
                throw new IllegalArgumentException("ebx can only be used in register-preserving contexts.");
            }
            context.addInstruction(0, context.requirePropertyIndex(identifierExpr.name));
            context.addInstruction(8, 0);
            return;
        }
        if (expr instanceof PropertyAccessExpr propertyAccessExpr) {
            emitExpression(propertyAccessExpr.indexExpr, context, EmitMode.R0);
            context.addInstruction(8, 0);
            return;
        }
        if (expr instanceof StringRefExpr stringRefExpr) {
            emitExpression(stringRefExpr.indexExpr, context, EmitMode.R0);
            return;
        }
        if (expr instanceof UnaryExpr unaryExpr) {
            if (unaryExpr.operator == Operator.NEGATE && unaryExpr.operand instanceof NumberExpr numberExpr) {
                context.addInstruction(0, -numberExpr.value);
                return;
            }
            emitExpression(unaryExpr.operand, context, EmitMode.R0);
            if (unaryExpr.operator == Operator.BIT_NOT) {
                context.addInstruction(0x14, 0);
                return;
            }
            if (unaryExpr.operator == Operator.NOT) {
                context.addInstruction(0x2B, 0);
                return;
            }
            throw new IllegalArgumentException("Unsupported unary operator: " + unaryExpr.operator);
        }
        if (expr instanceof BinaryExpr binaryExpr) {
            emitExpression(binaryExpr.left, context, EmitMode.R0);
            context.addInstruction(4, 0);
            emitExpression(binaryExpr.right, context, EmitMode.R0);
            context.addInstruction(6, 1);
            context.addInstruction(binaryOpcode(binaryExpr.operator), 0);
            return;
        }
        if (expr instanceof SyscallExpr syscallExpr) {
            for (Expr argument : syscallExpr.arguments) {
                emitExpression(argument, context, EmitMode.R0);
                context.addInstruction(5, isStringExpression(argument) ? 1 : 0);
            }
            context.addCallInstruction(syscallExpr.target, syscallExpr.arguments.size());
            return;
        }
        throw new IllegalArgumentException("Unsupported expression: " + expr);
    }

    private boolean isStringExpression(Expr expr) {
        return expr instanceof StringLiteralExpr || expr instanceof StringRefExpr;
    }

    private int binaryOpcode(Operator operator) {
        return switch (operator) {
            case ADD -> 9;
            case SUBTRACT -> 0xA;
            case MULTIPLY -> 0xB;
            case DIVIDE -> 0xC;
            case MODULO -> 0xD;
            case LOGICAL_OR -> 0xF;
            case LOGICAL_AND -> 0x10;
            case BIT_OR -> 0x11;
            case BIT_AND -> 0x12;
            case XOR -> 0x13;
            case LESS_EQUAL -> 0x15;
            case GREATER_EQUAL -> 0x16;
            case LESS_THAN -> 0x17;
            case GREATER_THAN -> 0x18;
            case EQUAL -> 0x19;
            case NOT_EQUAL -> 0x1A;
            case SHIFT_RIGHT -> 0x2D;
            case SHIFT_LEFT -> 0x2E;
            default -> throw new IllegalArgumentException("Unsupported binary operator: " + operator);
        };
    }

    private Expr parseExpression(String text) {
        return new ExpressionParser(text).parse();
    }

    private List<String> splitLines(String pseudoSource) {
        if (pseudoSource == null || pseudoSource.isEmpty()) {
            return List.of();
        }
        return Arrays.asList(pseudoSource.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));
    }

    private String stripComment(String line) {
        int idx = line.indexOf("//");
        return idx >= 0 ? line.substring(0, idx) : line;
    }

    private PseudoLine parsePseudoLine(String rawLine) {
        String line = rawLine == null ? "" : rawLine;
        int commentIndex = line.indexOf("//");
        if (commentIndex < 0) {
            return new PseudoLine(line, null, List.of());
        }

        String content = line.substring(0, commentIndex).trim();
        String comment = line.substring(commentIndex + 2).trim();
        String irHash = null;
        List<String> irEntries = List.of();
        int hashIndex = comment.indexOf("IRHASH=");
        int irIndex = comment.indexOf("IR=");
        if (hashIndex >= 0 && irIndex > hashIndex) {
            irHash = comment.substring(hashIndex + "IRHASH=".length(), irIndex).trim();
            String irBody = comment.substring(irIndex + "IR=".length()).trim();
            if (!irBody.isEmpty()) {
                irEntries = Arrays.stream(irBody.split("\\|"))
                        .map(String::trim)
                        .filter(s -> !s.isEmpty())
                        .toList();
            }
        }
        return new PseudoLine(content, irHash, irEntries);
    }

    private String stripStrictComment(String line) {
        int idx = line.indexOf(';');
        return idx >= 0 ? line.substring(0, idx) : line;
    }

    private boolean isRawInstruction(String line) {
        if (!line.startsWith("0x")) {
            return false;
        }
        int comma = line.indexOf(',');
        if (comma < 0) {
            return false;
        }
        try {
            parseFlexibleInt(line.substring(0, comma).trim());
            parseFlexibleInt(line.substring(comma + 1).trim());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    private static int parseFlexibleInt(String value) {
        String trimmed = value.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return (int) Long.parseLong(trimmed.substring(2), 16);
        }
        return Integer.parseInt(trimmed);
    }

    private int findMatchingRightParen(String text, int searchFrom) {
        int depth = 1;
        boolean inString = false;
        for (int i = searchFrom; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
                continue;
            }
            if (inString) {
                continue;
            }
            if (c == '(') {
                depth++;
            } else if (c == ')') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return -1;
    }

    private int findTopLevelOperator(String text, String operator) {
        int depthParen = 0;
        int depthBracket = 0;
        boolean inString = false;
        for (int i = 0; i <= text.length() - operator.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (inString) {
                continue;
            }
            if (c == '(') depthParen++;
            if (c == ')') depthParen--;
            if (c == '[') depthBracket++;
            if (c == ']') depthBracket--;
            if (depthParen == 0 && depthBracket == 0 && text.startsWith(operator, i)) {
                if ("=".equals(operator) && i > 0) {
                    char prev = text.charAt(i - 1);
                    if (prev == '!' || prev == '<' || prev == '>' || prev == '+' || prev == '-' || prev == '*'
                            || prev == '/' || prev == '%' || prev == '|' || prev == '&' || prev == '^') {
                        continue;
                    }
                }
                return i;
            }
        }
        return -1;
    }

    private List<String> splitTopLevel(String text, char delimiter) {
        List<String> result = new ArrayList<>();
        int last = 0;
        int depthParen = 0;
        int depthBracket = 0;
        boolean inString = false;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '"' && (i == 0 || text.charAt(i - 1) != '\\')) {
                inString = !inString;
            }
            if (inString) {
                continue;
            }
            if (c == '(') depthParen++;
            else if (c == ')') depthParen--;
            else if (c == '[') depthBracket++;
            else if (c == ']') depthBracket--;
            else if (c == delimiter && depthParen == 0 && depthBracket == 0) {
                result.add(text.substring(last, i));
                last = i + 1;
            }
        }
        result.add(text.substring(last));
        return result;
    }

    private PropertyAccess tryParsePropertyAccess(String text) {
        String trimmed = text.trim();
        if (!trimmed.startsWith("p[") || !trimmed.endsWith("]")) {
            return null;
        }
        return new PropertyAccess(parseExpression(trimmed.substring(2, trimmed.length() - 1)));
    }

    private static final class CompilerContext {
        private final Bin template;
        private final List<String> properties;
        private final List<String> strings;
        private final List<Bin.Instruction> instructions = new ArrayList<>();
        private final List<IndexedInstruction> replayedInstructions = new ArrayList<>();
        private final Map<String, Integer> propertyIndices = new LinkedHashMap<>();
        private final Map<String, Integer> globalSymbolIndices = new LinkedHashMap<>();
        private final Map<String, Integer> stringIndices = new LinkedHashMap<>();
        private final Map<String, Integer> labels = new HashMap<>();
        private final Map<Integer, String> unresolvedLabels = new LinkedHashMap<>();
        private final Map<Integer, Integer[]> constants = new LinkedHashMap<>();
        private boolean constantsTouched;
        private boolean semanticCompilationUsed;

        private CompilerContext(Bin template) {
            this.template = template;
            this.properties = template != null && template.getProperties() != null
                    ? new ArrayList<>(template.getProperties())
                    : new ArrayList<>();
            this.strings = template != null && template.getStringTable() != null
                    ? new ArrayList<>(template.getStringTable())
                    : new ArrayList<>();
            for (int i = 0; i < properties.size(); i++) {
                propertyIndices.putIfAbsent(properties.get(i), i);
                propertyIndices.putIfAbsent(BsdxGlobalSymbolUtil.aliasForProperty(i, properties.get(i)), i);
            }
            if (template != null && template.getGlobalSymbols() != null) {
                for (int i = 0; i < template.getGlobalSymbols().size(); i++) {
                    String alias = BsdxGlobalSymbolUtil.aliasFor(template.getGlobalSymbols(), i);
                    if (alias != null) {
                        globalSymbolIndices.putIfAbsent(alias, i);
                    }
                }
            }
            for (int i = 0; i < strings.size(); i++) {
                stringIndices.putIfAbsent(strings.get(i), i);
            }
        }

        private int addInstruction(int opcodeNum, int operandNum) {
            semanticCompilationUsed = true;
            Bin.Instruction inst = buildInstruction(opcodeNum, operandNum);
            inst.setIndex(instructions.size());
            instructions.add(inst);
            return instructions.size() - 1;
        }

        private void replayInstruction(int sourceIndex, int opcodeNum, int operandNum) {
            replayedInstructions.add(new IndexedInstruction(sourceIndex, buildInstruction(opcodeNum, operandNum)));
        }

        private void addCallInstruction(String target, int argc) {
            int nativeId;
            Integer mapped = BinConst.MNEMONIC_OPERAND_MAP.get(target);
            if (mapped != null) {
                nativeId = mapped;
            } else if (target.startsWith("0x") || target.startsWith("0X")) {
                nativeId = (int) Long.parseLong(target.substring(2), 16);
            } else {
                nativeId = Integer.parseInt(target);
            }
            addInstruction(Opcode.CALL.code, ((argc & 0xFFFF) << 16) | (nativeId & 0xFFFF));
        }

        private int requirePropertyIndex(String name) {
            Integer existing = propertyIndices.get(name);
            if (existing != null) {
                return existing;
            }
            Integer global = globalSymbolIndices.get(name);
            if (global != null) {
                return global;
            }
            int index = properties.size();
            properties.add(name);
            propertyIndices.put(name, index);
            return index;
        }

        /**
         * 解析 `global[...]` 左值里的索引文本。
         *
         * <p>这里既接受原始数字索引，也接受通过 `__GLOBAL.bin` 符号表渲染出来的别名。
         */
        private int resolveGlobalConstantIndex(String token) {
            Integer global = globalSymbolIndices.get(token);
            if (global != null) {
                return global;
            }
            Integer parsedAlias = BsdxGlobalSymbolUtil.tryParseAliasIndex(token);
            if (parsedAlias != null) {
                return parsedAlias;
            }
            return parseFlexibleInt(token);
        }

        private int requireStringIndex(String value) {
            Integer existing = stringIndices.get(value);
            if (existing != null) {
                return existing;
            }
            int index = strings.size();
            strings.add(value);
            stringIndices.put(value, index);
            return index;
        }

        private void resolveLabels() {
            for (Map.Entry<Integer, String> entry : unresolvedLabels.entrySet()) {
                Integer targetIndex = labels.get(entry.getValue());
                if (targetIndex == null) {
                    throw new IllegalArgumentException("Unknown label: " + entry.getValue());
                }
                instructions.get(entry.getKey()).setOperandNum(targetIndex - 1);
            }
        }

        private Bin build() {
            Bin result = new Bin();
            if (template != null) {
                result.setExtensionName(template.getExtensionName());
                result.setCharset(template.getCharset());
                result.setPreCount(template.getPreCount());
                result.setPreInstructions(template.getPreInstructions());
                result.setProperties2(template.getProperties2() == null ? null : new ArrayList<>(template.getProperties2()));
                result.setGlobalSymbols(template.getGlobalSymbols() == null ? null : new ArrayList<>(template.getGlobalSymbols()));
                result.setTailRaw(template.tailRaw);
                result.setConstants2(template.getConstants2());
                if (!constantsTouched) {
                    result.setTable(template.getTable() == null ? null : new ArrayList<>(template.getTable()));
                    result.setConstants(template.getConstants() == null ? null : new LinkedHashMap<>(template.getConstants()));
                } else {
                    result.setTable(null);
                }
            }
            result.setProperties(properties);
            result.setStringTable(strings);
            List<Bin.Instruction> finalInstructions;
            if (!semanticCompilationUsed && !replayedInstructions.isEmpty()) {
                replayedInstructions.sort((a, b) -> Integer.compare(a.sourceIndex(), b.sourceIndex()));
                finalInstructions = new ArrayList<>(replayedInstructions.size());
                for (IndexedInstruction indexedInstruction : replayedInstructions) {
                    finalInstructions.add(indexedInstruction.instruction());
                }
            } else {
                finalInstructions = instructions;
            }
            result.setInstructions(finalInstructions);
            if (constantsTouched) {
                result.setConstants(constants);
            }

            List<Integer> entryIndices = new ArrayList<>();
            List<Bin.Instruction> entries = new ArrayList<>();
            for (int i = 0; i < finalInstructions.size(); i++) {
                Bin.Instruction inst = finalInstructions.get(i);
                inst.setIndex(i);
                if (inst.getOpcodeNum() == Opcode.START.code) {
                    entryIndices.add(i);
                    entries.add(inst);
                }
            }
            result.entryPointIndices = entryIndices;
            result.setEntryPoints(entries);
            return result;
        }

        private Bin.Instruction buildInstruction(int opcodeNum, int operandNum) {
            Bin.Instruction inst = new Bin.Instruction();
            inst.setOpcodeNum(opcodeNum);
            inst.setOperandNum(operandNum);
            inst.setOpcode(BinConst.OPCODE_MNEMONIC_MAP.getOrDefault(opcodeNum, Integer.toString(opcodeNum)));
            if (opcodeNum == Opcode.CALL.code) {
                int nativeId = operandNum & 0xFFFF;
                inst.setParamCount(operandNum >>> 16);
                inst.setNativeId(nativeId);
                inst.setNativeFunction(BinConst.OPERAND_MNEMONIC_MAP.getOrDefault(nativeId, Integer.toString(nativeId)));
            } else {
                inst.setParamCount(0);
                inst.setNativeFunction(null);
            }
            return inst;
        }
    }

    private enum EmitMode {
        R0,
        R1
    }

    private record Assignment(String left, String operator, String right) {
    }

    private record PropertyAccess(Expr indexExpr) {
    }

    private record PseudoLine(String content, String irHash, List<String> irEntries) {
    }

    private record IndexedInstruction(int sourceIndex, Bin.Instruction instruction) {
    }

    private interface Expr {
    }

    private record NumberExpr(int value) implements Expr {
    }

    private record StringLiteralExpr(String value) implements Expr {
    }

    private record IdentifierExpr(String name) implements Expr {
    }

    private record PropertyAccessExpr(Expr indexExpr) implements Expr {
    }

    private record StringRefExpr(Expr indexExpr) implements Expr {
    }

    private record UnaryExpr(Operator operator, Expr operand) implements Expr {
    }

    private record BinaryExpr(Operator operator, Expr left, Expr right) implements Expr {
    }

    private record SyscallExpr(String target, List<Expr> arguments) implements Expr {
    }

    private enum Operator {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO,
        LOGICAL_OR,
        LOGICAL_AND,
        BIT_OR,
        BIT_AND,
        XOR,
        LESS_EQUAL,
        GREATER_EQUAL,
        LESS_THAN,
        GREATER_THAN,
        EQUAL,
        NOT_EQUAL,
        SHIFT_RIGHT,
        SHIFT_LEFT,
        NOT,
        NEGATE,
        BIT_NOT
    }

    private static final class ExpressionParser {
        private final List<Token> tokens;
        private int index;

        private ExpressionParser(String source) {
            this.tokens = new Tokenizer(source).tokenize();
        }

        private Expr parse() {
            Expr expr = parseLogicalOr();
            expect(TokenType.EOF);
            return expr;
        }

        private Expr parseLogicalOr() {
            Expr expr = parseLogicalAnd();
            while (matchOperator("||")) {
                expr = new BinaryExpr(Operator.LOGICAL_OR, expr, parseLogicalAnd());
            }
            return expr;
        }

        private Expr parseLogicalAnd() {
            Expr expr = parseBitOr();
            while (matchOperator("&&")) {
                expr = new BinaryExpr(Operator.LOGICAL_AND, expr, parseBitOr());
            }
            return expr;
        }

        private Expr parseBitOr() {
            Expr expr = parseBitXor();
            while (matchOperator("|")) {
                expr = new BinaryExpr(Operator.BIT_OR, expr, parseBitXor());
            }
            return expr;
        }

        private Expr parseBitXor() {
            Expr expr = parseBitAnd();
            while (matchOperator("^")) {
                expr = new BinaryExpr(Operator.XOR, expr, parseBitAnd());
            }
            return expr;
        }

        private Expr parseBitAnd() {
            Expr expr = parseEquality();
            while (matchOperator("&")) {
                expr = new BinaryExpr(Operator.BIT_AND, expr, parseEquality());
            }
            return expr;
        }

        private Expr parseEquality() {
            Expr expr = parseRelational();
            while (true) {
                if (matchOperator("==")) {
                    expr = new BinaryExpr(Operator.EQUAL, expr, parseRelational());
                } else if (matchOperator("!=")) {
                    expr = new BinaryExpr(Operator.NOT_EQUAL, expr, parseRelational());
                } else {
                    return expr;
                }
            }
        }

        private Expr parseRelational() {
            Expr expr = parseShift();
            while (true) {
                if (matchOperator("<=")) {
                    expr = new BinaryExpr(Operator.LESS_EQUAL, expr, parseShift());
                } else if (matchOperator(">=")) {
                    expr = new BinaryExpr(Operator.GREATER_EQUAL, expr, parseShift());
                } else if (matchOperator("<")) {
                    expr = new BinaryExpr(Operator.LESS_THAN, expr, parseShift());
                } else if (matchOperator(">")) {
                    expr = new BinaryExpr(Operator.GREATER_THAN, expr, parseShift());
                } else {
                    return expr;
                }
            }
        }

        private Expr parseShift() {
            Expr expr = parseAdditive();
            while (true) {
                if (matchOperator(">>")) {
                    expr = new BinaryExpr(Operator.SHIFT_RIGHT, expr, parseAdditive());
                } else if (matchOperator("<<")) {
                    expr = new BinaryExpr(Operator.SHIFT_LEFT, expr, parseAdditive());
                } else {
                    return expr;
                }
            }
        }

        private Expr parseAdditive() {
            Expr expr = parseMultiplicative();
            while (true) {
                if (matchOperator("+")) {
                    expr = new BinaryExpr(Operator.ADD, expr, parseMultiplicative());
                } else if (matchOperator("-")) {
                    expr = new BinaryExpr(Operator.SUBTRACT, expr, parseMultiplicative());
                } else {
                    return expr;
                }
            }
        }

        private Expr parseMultiplicative() {
            Expr expr = parseUnary();
            while (true) {
                if (matchOperator("*")) {
                    expr = new BinaryExpr(Operator.MULTIPLY, expr, parseUnary());
                } else if (matchOperator("/")) {
                    expr = new BinaryExpr(Operator.DIVIDE, expr, parseUnary());
                } else if (matchOperator("%")) {
                    expr = new BinaryExpr(Operator.MODULO, expr, parseUnary());
                } else {
                    return expr;
                }
            }
        }

        private Expr parseUnary() {
            if (matchOperator("!")) {
                return new UnaryExpr(Operator.NOT, parseUnary());
            }
            if (matchOperator("-")) {
                return new UnaryExpr(Operator.NEGATE, parseUnary());
            }
            if (matchOperator("~")) {
                return new UnaryExpr(Operator.BIT_NOT, parseUnary());
            }
            return parsePrimary();
        }

        private Expr parsePrimary() {
            Token token = peek();
            if (match(TokenType.NUMBER)) {
                return new NumberExpr(parseFlexibleInt(token.text));
            }
            if (match(TokenType.STRING)) {
                return new StringLiteralExpr(token.text);
            }
            if (match(TokenType.LPAREN)) {
                Expr expr = parseLogicalOr();
                expect(TokenType.RPAREN);
                return expr;
            }
            if (match(TokenType.IDENTIFIER)) {
                return parseIdentifier(token.text);
            }
            throw new IllegalArgumentException("Unexpected token in expression: " + token.text);
        }

        private Expr parseIdentifier(String identifier) {
            if ("syscall".equals(identifier)) {
                Token targetToken = consume();
                if (targetToken.type != TokenType.IDENTIFIER && targetToken.type != TokenType.NUMBER) {
                    throw new IllegalArgumentException("Invalid syscall target: " + targetToken.text);
                }
                expect(TokenType.LPAREN);
                List<Expr> args = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        args.add(parseLogicalOr());
                    } while (match(TokenType.COMMA));
                }
                expect(TokenType.RPAREN);
                return new SyscallExpr(targetToken.text, args);
            }
            if ("p".equals(identifier) && match(TokenType.LBRACKET)) {
                Expr indexExpr = parseLogicalOr();
                expect(TokenType.RBRACKET);
                return new PropertyAccessExpr(indexExpr);
            }
            if ("string".equals(identifier) && match(TokenType.LBRACKET)) {
                Expr indexExpr = parseLogicalOr();
                expect(TokenType.RBRACKET);
                return new StringRefExpr(indexExpr);
            }
            return new IdentifierExpr(identifier);
        }

        private boolean matchOperator(String operator) {
            if (check(TokenType.OPERATOR) && operator.equals(peek().text)) {
                index++;
                return true;
            }
            return false;
        }

        private boolean match(TokenType type) {
            if (check(type)) {
                index++;
                return true;
            }
            return false;
        }

        private boolean check(TokenType type) {
            return peek().type == type;
        }

        private Token peek() {
            return tokens.get(index);
        }

        private Token consume() {
            return tokens.get(index++);
        }

        private void expect(TokenType type) {
            if (!match(type)) {
                throw new IllegalArgumentException("Expected " + type + " but found " + peek().text);
            }
        }
    }

    private enum TokenType {
        NUMBER,
        STRING,
        IDENTIFIER,
        OPERATOR,
        LPAREN,
        RPAREN,
        LBRACKET,
        RBRACKET,
        COMMA,
        EOF
    }

    private record Token(TokenType type, String text) {
    }

    private static final class Tokenizer {
        private static final List<String> TWO_CHAR_OPERATORS = Arrays.asList(
                "||", "&&", "==", "!=", "<=", ">=", ">>", "<<"
        );

        private final String source;
        private final List<Token> tokens = new ArrayList<>();
        private int index;

        private Tokenizer(String source) {
            this.source = source == null ? "" : source;
        }

        private List<Token> tokenize() {
            while (index < source.length()) {
                char c = source.charAt(index);
                if (Character.isWhitespace(c)) {
                    index++;
                    continue;
                }
                if (c == '"') {
                    tokenizeString();
                    continue;
                }
                if (c == '(') {
                    tokens.add(new Token(TokenType.LPAREN, "("));
                    index++;
                    continue;
                }
                if (c == ')') {
                    tokens.add(new Token(TokenType.RPAREN, ")"));
                    index++;
                    continue;
                }
                if (c == '[') {
                    tokens.add(new Token(TokenType.LBRACKET, "["));
                    index++;
                    continue;
                }
                if (c == ']') {
                    tokens.add(new Token(TokenType.RBRACKET, "]"));
                    index++;
                    continue;
                }
                if (c == ',') {
                    tokens.add(new Token(TokenType.COMMA, ","));
                    index++;
                    continue;
                }
                if (tryTokenizeOperator()) {
                    continue;
                }
                if (Character.isDigit(c) || (c == '-' && isNegativeNumberStart())) {
                    tokenizeNumber();
                    continue;
                }
                tokenizeIdentifier();
            }
            tokens.add(new Token(TokenType.EOF, "<eof>"));
            return tokens;
        }

        private boolean tryTokenizeOperator() {
            for (String operator : TWO_CHAR_OPERATORS) {
                if (source.startsWith(operator, index)) {
                    tokens.add(new Token(TokenType.OPERATOR, operator));
                    index += operator.length();
                    return true;
                }
            }
            char c = source.charAt(index);
            if ("+-*/%^~!<>&|".indexOf(c) >= 0) {
                tokens.add(new Token(TokenType.OPERATOR, Character.toString(c)));
                index++;
                return true;
            }
            return false;
        }

        private boolean isNegativeNumberStart() {
            if (index + 1 >= source.length() || !Character.isDigit(source.charAt(index + 1))) {
                return false;
            }
            if (tokens.isEmpty()) {
                return true;
            }
            Token previous = tokens.get(tokens.size() - 1);
            return previous.type == TokenType.OPERATOR
                    || previous.type == TokenType.LPAREN
                    || previous.type == TokenType.COMMA
                    || previous.type == TokenType.LBRACKET;
        }

        private void tokenizeNumber() {
            int start = index;
            index++;
            while (index < source.length()) {
                char c = source.charAt(index);
                if (Character.isDigit(c)
                        || c == 'x'
                        || c == 'X'
                        || (c >= 'a' && c <= 'f')
                        || (c >= 'A' && c <= 'F')) {
                    index++;
                } else {
                    break;
                }
            }
            tokens.add(new Token(TokenType.NUMBER, source.substring(start, index)));
        }

        private void tokenizeString() {
            index++;
            StringBuilder builder = new StringBuilder();
            while (index < source.length()) {
                char c = source.charAt(index++);
                if (c == '\\' && index < source.length()) {
                    char next = source.charAt(index++);
                    builder.append(next);
                    continue;
                }
                if (c == '"') {
                    tokens.add(new Token(TokenType.STRING, builder.toString()));
                    return;
                }
                builder.append(c);
            }
            throw new IllegalArgumentException("Unterminated string literal");
        }

        private void tokenizeIdentifier() {
            int start = index;
            while (index < source.length()) {
                char c = source.charAt(index);
                if (Character.isWhitespace(c)
                        || "()[],".indexOf(c) >= 0
                        || "+-*/%^~=!<>&|".indexOf(c) >= 0) {
                    break;
                }
                index++;
            }
            tokens.add(new Token(TokenType.IDENTIFIER, source.substring(start, index)));
        }
    }
}
