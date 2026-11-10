package com.giga.nexas.dto.bsdx.bin.lossless;

import com.giga.nexas.dto.bsdx.bin.consts.BinConst;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.AssignStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.BinaryExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.BinaryOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallKind;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallTarget;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.ConstExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.EndStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.EntryStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.Expression;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.GotoStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.IfGotoStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.InvokeStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.LabelStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.LinenoStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.MarkerStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.ParamStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.PopStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.PushStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterId;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterTarget;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.Statement;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.StringRefExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.StringRefKind;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.ToStringExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UnaryExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UnaryOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UpdateOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UpdateStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.VarExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.VariableTarget;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * 把 lossless pseudo DSL 解析回 Structured Program。
 *
 * <p>所属层级：
 * {@code Pseudo DSL -> Structured IR}。
 *
 * <p>它与 {@link BsdxBinLosslessDslRenderer} 成对出现，只接受当前固定 DSL，
 * 确保每一种被接受的语法都有确定性的 lowering 路径。
 */
public class BsdxBinLosslessDslParser {

    private final BsdxBinLosslessDslRenderer renderer = new BsdxBinLosslessDslRenderer();

    /**
     * 解析 DSL 文本，同时复用现有 Structured Program 模板上的非代码元数据。
     *
     * <p>当前 DSL 只覆盖可执行代码，所以数据区元数据仍然来自模板。
     * 在独立 DSL directive 还没引入前，这是保持无损的必要条件。
     */
    public BsdxBinLosslessProgram parse(BsdxBinLosslessProgram template, String source) {
        if (template == null) {
            throw new IllegalArgumentException("Template program is required so non-code metadata stays lossless.");
        }

        /*
         * 如果文本和模板程序的 lossless 渲染结果完全一致，说明用户没有改动代码内容。
         * 这时直接复用模板程序，可以保证未修改路径精确回到原始 structured IR / strict IR。
         */
        if (normalizeSource(renderer.render(template)).equals(normalizeSource(source))) {
            return template;
        }

        List<Statement> statements = new ArrayList<>();
        List<String> lines = source == null
                ? List.of()
                : Arrays.asList(source.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));

        int lineNumber = 0;
        for (String rawLine : lines) {
            lineNumber++;
            String line = rawLine.trim();
            if (line.isEmpty()) {
                continue;
            }
            try {
                statements.add(parseStatement(line));
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException("line " + lineNumber + ": " + line + " -> " + ex.getMessage(), ex);
            }
        }

        return new BsdxBinLosslessProgram(
                template.extensionName(),
                template.charset(),
                template.preCount(),
                template.preInstructions(),
                template.dataSection(),
                template.tailRaw(),
                statements,
                null
        );
    }

    /**
     * 统一源码换行和首尾空白，供“未修改文本快速回放”判断使用。
     */
    private String normalizeSource(String source) {
        if (source == null) {
            return "";
        }
        return source.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    /**
     * 把一行 DSL 解析成一条结构化语句。
     */
    private Statement parseStatement(String line) {
        if (line.startsWith("entry ")) {
            return new EntryStmt(parseFlexibleInt(line.substring("entry ".length()).trim()));
        }
        if (line.startsWith("end ")) {
            return new EndStmt(parseFlexibleInt(line.substring("end ".length()).trim()));
        }
        if (line.startsWith("marker ")) {
            return new MarkerStmt(parseFlexibleInt(line.substring("marker ".length()).trim()));
        }
        if (line.startsWith("lineno ")) {
            return new LinenoStmt(parseFlexibleInt(line.substring("lineno ".length()).trim()));
        }
        if (line.startsWith("label ")) {
            return new LabelStmt(line.substring("label ".length()).trim());
        }
        if (line.startsWith("goto ")) {
            return new GotoStmt(line.substring("goto ".length()).trim());
        }
        if (line.startsWith("push ")) {
            return new PushStmt(parseRegister(line.substring("push ".length()).trim()));
        }
        if (line.startsWith("pop ")) {
            return new PopStmt(parseRegister(line.substring("pop ".length()).trim()));
        }
        if (line.startsWith("if ")) {
            return parseIfGoto(line);
        }
        if (line.startsWith("call ")) {
            return new CallStmt(parseCall(line));
        }
        if (line.startsWith("invoke ")) {
            return parseInvoke(line);
        }
        if (line.startsWith("param ")) {
            return new ParamStmt(parseExpression(line.substring("param ".length()).trim()));
        }
        UpdateStmt update = parseUpdate(line);
        if (update != null) {
            return update;
        }
        Assignment assignment = parseAssignment(line);
        if (assignment == null) {
            throw new IllegalArgumentException("Unsupported statement");
        }
        return new AssignStmt(parseTarget(assignment.left()), parseExpression(assignment.right()));
    }

    /**
     * 解析显式无损分支形式 {@code if (expr) goto Lx}。
     */
    private Statement parseIfGoto(String line) {
        int left = line.indexOf('(');
        int right = findMatchingRightParen(line, left);
        if (left < 0 || right < 0) {
            throw new IllegalArgumentException("Invalid if-goto syntax");
        }
        String tail = line.substring(right + 1).trim();
        if (!tail.startsWith("goto ")) {
            throw new IllegalArgumentException("Only if (...) goto <label> is supported");
        }
        return new IfGotoStmt(parseExpression(line.substring(left + 1, right)), tail.substring("goto ".length()).trim());
    }

    /**
     * 解析显式调用落点语句 {@code invoke Target/argc}。
     */
    private Statement parseInvoke(String line) {
        String body = line.substring("invoke ".length()).trim();
        int slash = body.lastIndexOf('/');
        if (slash < 0) {
            throw new IllegalArgumentException("Invoke syntax must be invoke Target/argc");
        }
        String targetToken = body.substring(0, slash).trim();
        int argc = parseFlexibleInt(body.substring(slash + 1).trim());
        return new InvokeStmt(parseCallTargetToken(targetToken), argc);
    }

    /**
     * 解析赋值目标。
     *
     * <p>DSL 把寄存器目标和变量槽位目标分开，
     * 因为 lowering 在 opcode 层面对这两者的处理完全不同。
     */
    private BsdxBinLosslessProgram.Target parseTarget(String text) {
        String trimmed = text.trim();
        if ("r0".equals(trimmed)) {
            return new RegisterTarget(RegisterId.R0);
        }
        if ("r1".equals(trimmed)) {
            return new RegisterTarget(RegisterId.R1);
        }
        if (trimmed.startsWith("var[") && trimmed.endsWith("]")) {
            return new VariableTarget(parseExpression(trimmed.substring(4, trimmed.length() - 1)));
        }
        throw new IllegalArgumentException("Unsupported assignment target: " + trimmed);
    }

    /**
     * 解析 DSL 中使用的寄存器名字。
     */
    private RegisterId parseRegister(String text) {
        String trimmed = text.trim();
        if ("r0".equals(trimmed)) {
            return RegisterId.R0;
        }
        if ("r1".equals(trimmed)) {
            return RegisterId.R1;
        }
        throw new IllegalArgumentException("Unsupported register: " + trimmed);
    }

    /**
     * 在忽略比较运算符的前提下，找到最外层赋值号。
     */
    private Assignment parseAssignment(String line) {
        int index = findTopLevelEquals(line);
        if (index < 0) {
            return null;
        }
        return new Assignment(line.substring(0, index), line.substring(index + 1));
    }

    /**
     * 解析自增、自减和复合赋值语句。
     */
    private UpdateStmt parseUpdate(String line) {
        if (line.endsWith("++")) {
            return new UpdateStmt(parseTarget(line.substring(0, line.length() - 2)), UpdateOperator.INC, null);
        }
        if (line.endsWith("--")) {
            return new UpdateStmt(parseTarget(line.substring(0, line.length() - 2)), UpdateOperator.DEC, null);
        }

        List<OperatorPattern> patterns = List.of(
                new OperatorPattern(">>=", UpdateOperator.SHR_ASSIGN),
                new OperatorPattern("<<=", UpdateOperator.SHL_ASSIGN),
                new OperatorPattern("+=", UpdateOperator.ADD_ASSIGN),
                new OperatorPattern("-=", UpdateOperator.SUB_ASSIGN),
                new OperatorPattern("*=", UpdateOperator.MUL_ASSIGN),
                new OperatorPattern("/=", UpdateOperator.DIV_ASSIGN),
                new OperatorPattern("%=", UpdateOperator.MOD_ASSIGN),
                new OperatorPattern("|=", UpdateOperator.OR_ASSIGN),
                new OperatorPattern("&=", UpdateOperator.AND_ASSIGN),
                new OperatorPattern("^=", UpdateOperator.XOR_ASSIGN)
        );
        for (OperatorPattern pattern : patterns) {
            int index = findTopLevelOperator(line, pattern.token());
            if (index >= 0) {
                return new UpdateStmt(
                        parseTarget(line.substring(0, index)),
                        pattern.operator(),
                        parseExpression(line.substring(index + pattern.token().length()))
                );
            }
        }
        return null;
    }

    /**
     * 找出用于赋值的顶层 {@code =}，并避开 {@code ==, !=, <=, >=}。
     */
    private int findTopLevelEquals(String line) {
        int depthParen = 0;
        int depthBracket = 0;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '(') {
                depthParen++;
                continue;
            }
            if (c == ')') {
                depthParen--;
                continue;
            }
            if (c == '[') {
                depthBracket++;
                continue;
            }
            if (c == ']') {
                depthBracket--;
                continue;
            }
            if (depthParen == 0 && depthBracket == 0 && c == '=') {
                if ((i > 0 && (line.charAt(i - 1) == '=' || line.charAt(i - 1) == '!' || line.charAt(i - 1) == '<' || line.charAt(i - 1) == '>'))
                        || (i + 1 < line.length() && line.charAt(i + 1) == '=')) {
                    continue;
                }
                return i;
            }
        }
        return -1;
    }

    /**
     * 在括号和下标之外查找某个复合运算符。
     */
    private int findTopLevelOperator(String line, String token) {
        int depthParen = 0;
        int depthBracket = 0;
        for (int i = 0; i <= line.length() - token.length(); i++) {
            char c = line.charAt(i);
            if (c == '(') {
                depthParen++;
                continue;
            }
            if (c == ')') {
                depthParen--;
                continue;
            }
            if (c == '[') {
                depthBracket++;
                continue;
            }
            if (c == ']') {
                depthBracket--;
                continue;
            }
            if (depthParen == 0 && depthBracket == 0 && line.startsWith(token, i)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * 从固定 DSL 语法解析调用语句/表达式。
     */
    private CallExpr parseCall(String text) {
        return (CallExpr) parseExpression(text);
    }

    /**
     * 使用下面的优先级递归下降解析器解析 DSL 表达式。
     */
    private Expression parseExpression(String text) {
        return new ExpressionParser(text).parse();
    }

    /**
     * 为分支语法解析匹配右括号。
     */
    private int findMatchingRightParen(String text, int leftParenIndex) {
        if (leftParenIndex < 0) {
            return -1;
        }
        int depth = 0;
        for (int i = leftParenIndex; i < text.length(); i++) {
            char c = text.charAt(i);
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

    /**
     * 解析 lossless DSL 中使用的十进制或十六进制整数。
     */
    private static int parseFlexibleInt(String token) {
        String trimmed = token.trim();
        if (trimmed.startsWith("0x") || trimmed.startsWith("0X")) {
            return (int) Long.parseLong(trimmed.substring(2), 16);
        }
        return Integer.parseInt(trimmed);
    }

    /**
     * 赋值语法的临时承载体，等目标和表达式分别转成 AST 节点前先占位。
     */
    private record Assignment(String left, String right) {
    }

    /**
     * 复合赋值语法的匹配项。
     */
    private record OperatorPattern(String token, UpdateOperator operator) {
    }

    /**
     * 固定 lossless 表达式语法的递归下降解析器。
     *
     * <p>这套语法存在的目的，是精确保留 renderer 输出的表达式树，
     * 不是为了接受任意高级语言风格输入。
     */
    private static final class ExpressionParser {
        private final List<Token> tokens;
        private int index;

        /**
         * 在递归下降开始前，先把一条表达式文本切成 token 流。
         */
        private ExpressionParser(String source) {
            this.tokens = new Tokenizer(source).tokenize();
        }

        /**
         * 解析完整表达式，并确认后面没有残余 token。
         */
        private Expression parse() {
            Expression expression = parseLogicalOr();
            expect(TokenType.EOF);
            return expression;
        }

        private Expression parseLogicalOr() {
            Expression expression = parseLogicalAnd();
            while (matchOperator("||")) {
                expression = new BinaryExpr(BinaryOperator.LOGICAL_OR, expression, parseLogicalAnd());
            }
            return expression;
        }

        private Expression parseLogicalAnd() {
            Expression expression = parseBitOr();
            while (matchOperator("&&")) {
                expression = new BinaryExpr(BinaryOperator.LOGICAL_AND, expression, parseBitOr());
            }
            return expression;
        }

        private Expression parseBitOr() {
            Expression expression = parseBitXor();
            while (matchOperator("|")) {
                expression = new BinaryExpr(BinaryOperator.BIT_OR, expression, parseBitXor());
            }
            return expression;
        }

        private Expression parseBitXor() {
            Expression expression = parseBitAnd();
            while (matchOperator("^")) {
                expression = new BinaryExpr(BinaryOperator.BIT_XOR, expression, parseBitAnd());
            }
            return expression;
        }

        private Expression parseBitAnd() {
            Expression expression = parseEquality();
            while (matchOperator("&")) {
                expression = new BinaryExpr(BinaryOperator.BIT_AND, expression, parseEquality());
            }
            return expression;
        }

        private Expression parseEquality() {
            Expression expression = parseRelational();
            while (true) {
                if (matchOperator("==")) {
                    expression = new BinaryExpr(BinaryOperator.EQUAL, expression, parseRelational());
                } else if (matchOperator("!=")) {
                    expression = new BinaryExpr(BinaryOperator.NOT_EQUAL, expression, parseRelational());
                } else {
                    return expression;
                }
            }
        }

        private Expression parseRelational() {
            Expression expression = parseShift();
            while (true) {
                if (matchOperator("<=")) {
                    expression = new BinaryExpr(BinaryOperator.LESS_EQUAL, expression, parseShift());
                } else if (matchOperator(">=")) {
                    expression = new BinaryExpr(BinaryOperator.GREATER_EQUAL, expression, parseShift());
                } else if (matchOperator("<")) {
                    expression = new BinaryExpr(BinaryOperator.LESS_THAN, expression, parseShift());
                } else if (matchOperator(">")) {
                    expression = new BinaryExpr(BinaryOperator.GREATER_THAN, expression, parseShift());
                } else {
                    return expression;
                }
            }
        }

        private Expression parseShift() {
            Expression expression = parseAdditive();
            while (true) {
                if (matchOperator(">>")) {
                    expression = new BinaryExpr(BinaryOperator.SHIFT_RIGHT, expression, parseAdditive());
                } else if (matchOperator("<<")) {
                    expression = new BinaryExpr(BinaryOperator.SHIFT_LEFT, expression, parseAdditive());
                } else {
                    return expression;
                }
            }
        }

        private Expression parseAdditive() {
            Expression expression = parseMultiplicative();
            while (true) {
                if (matchOperator("+")) {
                    expression = new BinaryExpr(BinaryOperator.ADD, expression, parseMultiplicative());
                } else if (matchOperator("-")) {
                    expression = new BinaryExpr(BinaryOperator.SUBTRACT, expression, parseMultiplicative());
                } else {
                    return expression;
                }
            }
        }

        private Expression parseMultiplicative() {
            Expression expression = parseUnary();
            while (true) {
                if (matchOperator("*")) {
                    expression = new BinaryExpr(BinaryOperator.MULTIPLY, expression, parseUnary());
                } else if (matchOperator("/")) {
                    expression = new BinaryExpr(BinaryOperator.DIVIDE, expression, parseUnary());
                } else if (matchOperator("%")) {
                    expression = new BinaryExpr(BinaryOperator.MODULO, expression, parseUnary());
                } else {
                    return expression;
                }
            }
        }

        private Expression parseUnary() {
            if (matchOperator("!")) {
                return new UnaryExpr(UnaryOperator.LOGICAL_NOT, parseUnary());
            }
            if (matchOperator("-")) {
                return new UnaryExpr(UnaryOperator.NEGATE, parseUnary());
            }
            if (matchOperator("~")) {
                return new UnaryExpr(UnaryOperator.BITWISE_NOT, parseUnary());
            }
            return parsePrimary();
        }

        private Expression parsePrimary() {
            Token token = peek();
            if (match(TokenType.NUMBER)) {
                return new ConstExpr(parseFlexibleInt(token.text()));
            }
            if (match(TokenType.LPAREN)) {
                Expression expression = parseLogicalOr();
                expect(TokenType.RPAREN);
                return expression;
            }
            if (match(TokenType.IDENTIFIER)) {
                return parseIdentifier(token.text());
            }
            throw new IllegalArgumentException("Unexpected token in expression: " + token.text());
        }

        private Expression parseIdentifier(String text) {
            String normalized = text.toLowerCase(Locale.ROOT);
            if ("r0".equals(normalized)) {
                return new RegisterExpr(RegisterId.R0);
            }
            if ("r1".equals(normalized)) {
                return new RegisterExpr(RegisterId.R1);
            }
            if ("var".equals(normalized)) {
                expect(TokenType.LBRACKET);
                Expression indexExpr = parseLogicalOr();
                expect(TokenType.RBRACKET);
                return new VarExpr(indexExpr);
            }
            if ("str".equals(normalized) || "str2".equals(normalized)) {
                expect(TokenType.LBRACKET);
                Expression indexExpr = parseLogicalOr();
                expect(TokenType.RBRACKET);
                return new StringRefExpr("str2".equals(normalized) ? StringRefKind.STR2 : StringRefKind.STR, indexExpr);
            }
            if ("strref".equals(normalized)) {
                expect(TokenType.LPAREN);
                Expression indexExpr = parseLogicalOr();
                expect(TokenType.RPAREN);
                return new StringRefExpr(StringRefKind.RAW, indexExpr);
            }
            if ("to_string".equals(normalized)) {
                expect(TokenType.LPAREN);
                Expression valueExpr = parseLogicalOr();
                expect(TokenType.RPAREN);
                return new ToStringExpr(valueExpr);
            }
            if ("concat".equals(normalized)) {
                expect(TokenType.LPAREN);
                Expression leftExpr = parseLogicalOr();
                expect(TokenType.COMMA);
                Expression rightExpr = parseLogicalOr();
                expect(TokenType.RPAREN);
                return new BinaryExpr(BinaryOperator.CONCAT, leftExpr, rightExpr);
            }
            if ("call".equals(normalized)) {
                Token targetToken = consume();
                if (targetToken.type() != TokenType.IDENTIFIER && targetToken.type() != TokenType.NUMBER) {
                    throw new IllegalArgumentException("Invalid call target: " + targetToken.text());
                }
                expect(TokenType.LPAREN);
                List<Expression> arguments = new ArrayList<>();
                if (!check(TokenType.RPAREN)) {
                    do {
                        arguments.add(parseLogicalOr());
                    } while (match(TokenType.COMMA));
                }
                expect(TokenType.RPAREN);
                    return new CallExpr(parseCallTargetToken(targetToken.text()), arguments);
                }
                throw new IllegalArgumentException("Unknown identifier: " + text);
            }

        private boolean matchOperator(String operator) {
            if (check(TokenType.OPERATOR) && operator.equals(peek().text())) {
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
            return peek().type() == type;
        }

        private Token consume() {
            return tokens.get(index++);
        }

        private Token peek() {
            return tokens.get(index);
        }

        private void expect(TokenType type) {
            if (!match(type)) {
                throw new IllegalArgumentException("Expected " + type + " but found " + peek().text());
            }
        }
    }

    /**
     * 把 DSL 调用目标 token 解析回原始 VM 元数据。
     *
     * <p>当前可逆子集只支持 native CALL，所以未知名字也会被当成 native id 处理。
     */
    private static CallTarget parseCallTargetToken(String token) {
        Integer mapped = BinConst.MNEMONIC_OPERAND_MAP.get(token);
        if (mapped != null) {
            return new CallTarget(mapped, token, CallKind.NATIVE);
        }
        int rawId = parseFlexibleInt(token);
        return new CallTarget(rawId, BinConst.OPERAND_MNEMONIC_MAP.get(rawId), CallKind.NATIVE);
    }

    private enum TokenType {
        NUMBER,
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

    /**
     * 固定 lossless DSL 表达式语法使用的 tokenizer。
     */
    private static final class Tokenizer {
        private static final List<String> TWO_CHAR_OPERATORS = List.of(
                "||", "&&", "==", "!=", "<=", ">=", ">>", "<<"
        );

        private final String source;
        private final List<Token> tokens = new ArrayList<>();
        private int index;

        private Tokenizer(String source) {
            this.source = source == null ? "" : source;
        }

        /**
         * 产生供 {@link ExpressionParser} 消费的 token 流。
         */
        private List<Token> tokenize() {
            while (index < source.length()) {
                char c = source.charAt(index);
                if (Character.isWhitespace(c)) {
                    index++;
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
                if (tryTwoCharOperator()) {
                    continue;
                }
                /*
                 * 负立即数必须先于通用单字符运算符被识别，
                 * 否则 {@code -1} 会被改写成一元表达式而不是原子常量，破坏 round-trip。
                 */
                if (Character.isDigit(c) || (c == '-' && isNegativeNumberStart())) {
                    tokenizeNumber();
                    continue;
                }
                if ("+-*/%^!<>&|".indexOf(c) >= 0) {
                    tokens.add(new Token(TokenType.OPERATOR, Character.toString(c)));
                    index++;
                    continue;
                }
                tokenizeIdentifier();
            }
            tokens.add(new Token(TokenType.EOF, "<eof>"));
            return tokens;
        }

        /**
         * 尝试发射一个双字符运算符 token，例如 {@code ==} 或 {@code >>}。
         */
        private boolean tryTwoCharOperator() {
            for (String operator : TWO_CHAR_OPERATORS) {
                if (source.startsWith(operator, index)) {
                    tokens.add(new Token(TokenType.OPERATOR, operator));
                    index += operator.length();
                    return true;
                }
            }
            return false;
        }

        /**
         * 判断前导 {@code -} 是在开始一个数字字面量，而不是减号运算符。
         */
        private boolean isNegativeNumberStart() {
            if (index + 1 >= source.length() || !Character.isDigit(source.charAt(index + 1))) {
                return false;
            }
            if (tokens.isEmpty()) {
                return true;
            }
            Token previous = tokens.get(tokens.size() - 1);
            return previous.type() == TokenType.OPERATOR
                    || previous.type() == TokenType.LPAREN
                    || previous.type() == TokenType.COMMA
                    || previous.type() == TokenType.LBRACKET;
        }

        /**
         * 读取一个十进制或十六进制数字 token。
         */
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

        /**
         * 读取一个标识符 token，其中也包括具名调用目标。
         */
        private void tokenizeIdentifier() {
            int start = index;
            while (index < source.length()) {
                char c = source.charAt(index);
                if (Character.isWhitespace(c) || "()[],".indexOf(c) >= 0 || "+-*/%^!<>&|".indexOf(c) >= 0) {
                    break;
                }
                index++;
            }
            tokens.add(new Token(TokenType.IDENTIFIER, source.substring(start, index)));
        }
    }
}
