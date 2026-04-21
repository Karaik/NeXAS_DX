package com.giga.nexas.dto.bsdx.bin.lossless;

import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.AssignStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.BinaryExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.BinaryOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.CallStmt;
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
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterTarget;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.Statement;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.StringRefExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.StringRefKind;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.ToStringExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UnaryExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UpdateOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UpdateStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.VarExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.VariableTarget;

import java.util.Locale;
import java.util.StringJoiner;

/**
 * 把 Structured IR 渲染成 lossless pseudo DSL。
 *
 * <p>所属层级：
 * {@code Structured IR -> Pseudo DSL}。
 *
 * <p>这里优先追求 round-trip 稳定性，而不是“好看”。
 * label、goto、寄存器赋值和 VM 风格调用都会显式保留，这样 parser 才能无猜测地重建同一棵结构树。
 */
public class BsdxBinLosslessDslRenderer {

    /**
     * 把整个 Structured Program 渲染成文本。
     *
     * <p>在当前支持子集里，这一步目标是无损：
     * 每一种输出形式都必须在 {@link BsdxBinLosslessDslParser} 里有直接对应的解析路径。
     */
    public String render(BsdxBinLosslessProgram program) {
        if (program == null || program.statements().isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (Statement statement : program.statements()) {
            builder.append(renderStatement(statement)).append("\n");
        }
        return builder.toString();
    }

    /**
     * 把一条结构化语句渲染成一行 DSL。
     */
    private String renderStatement(Statement statement) {
        if (statement instanceof EntryStmt entry) {
            return "entry " + entry.bank();
        }
        if (statement instanceof EndStmt end) {
            return "end " + end.operand();
        }
        if (statement instanceof MarkerStmt marker) {
            return "marker " + marker.marker();
        }
        if (statement instanceof LinenoStmt lineno) {
            return "lineno " + lineno.lineNumber();
        }
        if (statement instanceof LabelStmt label) {
            return "label " + label.name();
        }
        if (statement instanceof GotoStmt jump) {
            return "goto " + jump.targetLabel();
        }
        if (statement instanceof IfGotoStmt branch) {
            return "if (" + renderExpression(branch.condition(), 0) + ") goto " + branch.targetLabel();
        }
        if (statement instanceof AssignStmt assign) {
            return renderTarget(assign.target()) + " = " + renderExpression(assign.value(), 0);
        }
        if (statement instanceof UpdateStmt update) {
            String target = renderTarget(update.target());
            if (update.operator() == UpdateOperator.INC) {
                return target + "++";
            }
            if (update.operator() == UpdateOperator.DEC) {
                return target + "--";
            }
            return target + " " + updateOperator(update.operator()) + " " + renderExpression(update.value(), 0);
        }
        if (statement instanceof PushStmt push) {
            return "push " + renderRegister(push.register().name());
        }
        if (statement instanceof PopStmt pop) {
            return "pop " + renderRegister(pop.register().name());
        }
        if (statement instanceof ParamStmt param) {
            return "param " + renderExpression(param.value(), 0);
        }
        if (statement instanceof CallStmt callStmt) {
            return renderCall(callStmt.call());
        }
        if (statement instanceof InvokeStmt invoke) {
            return "invoke " + renderCallTarget(invoke.target()) + "/" + invoke.argc();
        }
        throw new IllegalArgumentException("Unsupported statement: " + statement);
    }

    /**
     * 渲染一条表达式，并显式保留运算符优先级。
     *
     * <p>{@code parentPrecedence} 的存在完全是为了 DSL 稳定性，
     * 防止 parser 依赖“碰巧正确”的默认优先级重建出不同的树形结构。
     */
    private String renderExpression(Expression expression, int parentPrecedence) {
        if (expression instanceof ConstExpr constant) {
            return Integer.toString(constant.value());
        }
        if (expression instanceof RegisterExpr register) {
            return renderRegister(register.register().name());
        }
        if (expression instanceof VarExpr varExpr) {
            return renderVariable(varExpr.index());
        }
        if (expression instanceof StringRefExpr stringRef) {
            String prefix = switch (stringRef.kind()) {
                case STR -> "str";
                case STR2 -> "str2";
                case RAW -> "strref";
            };
            if (stringRef.kind() == StringRefKind.RAW) {
                return prefix + "(" + renderExpression(stringRef.index(), 0) + ")";
            }
            return prefix + "[" + renderExpression(stringRef.index(), 0) + "]";
        }
        if (expression instanceof CallExpr call) {
            return renderCall(call);
        }
        if (expression instanceof ToStringExpr toStringExpr) {
            return "to_string(" + renderExpression(toStringExpr.value(), 0) + ")";
        }
        if (expression instanceof UnaryExpr unary) {
            String rendered = unaryOperator(unary) + renderExpression(unary.operand(), precedence(unary));
            return parenthesizeIfNeeded(rendered, precedence(unary), parentPrecedence);
        }
        if (expression instanceof BinaryExpr binary) {
            if (binary.operator() == BinaryOperator.CONCAT) {
                return "concat(" + renderExpression(binary.left(), 0) + ", " + renderExpression(binary.right(), 0) + ")";
            }
            int precedence = precedence(binary);
            String rendered = renderExpression(binary.left(), precedence)
                    + " " + binaryOperator(binary.operator()) + " "
                    + renderExpression(binary.right(), precedence + 1);
            return parenthesizeIfNeeded(rendered, precedence, parentPrecedence);
        }
        throw new IllegalArgumentException("Unsupported expression: " + expression);
    }

    /**
     * 渲染调用目标和其有序参数列表。
     *
     * <p>未知 native id 保持十六进制形式，
     * 这样 DSL 仍然可编辑，而不需要伪造一个并不存在的名字。
     */
    private String renderCall(CallExpr call) {
        String target = renderCallTarget(call.target());
        StringJoiner joiner = new StringJoiner(", ");
        for (Expression argument : call.arguments()) {
            joiner.add(renderExpression(argument, 0));
        }
        return "call " + target + "(" + joiner + ")";
    }

    /**
     * 渲染调用目标本体，不包含参数列表。
     */
    private String renderCallTarget(BsdxBinLosslessProgram.CallTarget target) {
        String rendered = target.mnemonic();
        if (rendered == null || rendered.isBlank()) {
            rendered = String.format(Locale.ROOT, "0x%04X", target.rawId() & 0xFFFF);
        }
        return rendered;
    }

    /**
     * 渲染赋值/更新语句共享的目标节点。
     */
    private String renderTarget(BsdxBinLosslessProgram.Target target) {
        if (target instanceof RegisterTarget registerTarget) {
            return renderRegister(registerTarget.register().name());
        }
        return renderVariable(((VariableTarget) target).index());
    }

    /**
     * 用显式 VM 风格 DSL 形式渲染变量槽位访问。
     */
    private String renderVariable(Expression indexExpression) {
        return "var[" + renderExpression(indexExpression, 0) + "]";
    }

    /**
     * 用 DSL 规定的标准拼写渲染寄存器标识符。
     */
    private String renderRegister(String registerName) {
        return registerName.toLowerCase(Locale.ROOT);
    }

    /**
     * 把单目运算符枚举转换成 DSL token。
     */
    private String unaryOperator(UnaryExpr unary) {
        return switch (unary.operator()) {
            case NEGATE -> "-";
            case LOGICAL_NOT -> "!";
            case BITWISE_NOT -> "~";
        };
    }

    /**
     * 把双目运算符枚举转换成 DSL token。
     */
    private String binaryOperator(BinaryOperator operator) {
        return switch (operator) {
            case ADD -> "+";
            case SUBTRACT -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case MODULO -> "%";
            case LOGICAL_OR -> "||";
            case LOGICAL_AND -> "&&";
            case BIT_OR -> "|";
            case BIT_AND -> "&";
            case BIT_XOR -> "^";
            case LESS_EQUAL -> "<=";
            case GREATER_EQUAL -> ">=";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            case EQUAL -> "==";
            case NOT_EQUAL -> "!=";
            case SHIFT_RIGHT -> ">>";
            case SHIFT_LEFT -> "<<";
            case CONCAT -> throw new IllegalArgumentException("CONCAT must be rendered via concat(left, right).");
        };
    }

    /**
     * 把更新语句运算符转换成 DSL token。
     */
    private String updateOperator(UpdateOperator operator) {
        return switch (operator) {
            case ADD_ASSIGN -> "+=";
            case SUB_ASSIGN -> "-=";
            case MUL_ASSIGN -> "*=";
            case DIV_ASSIGN -> "/=";
            case MOD_ASSIGN -> "%=";
            case OR_ASSIGN -> "|=";
            case AND_ASSIGN -> "&=";
            case XOR_ASSIGN -> "^=";
            case SHR_ASSIGN -> ">>=";
            case SHL_ASSIGN -> "<<=";
            default -> throw new IllegalArgumentException("Unsupported update operator: " + operator);
        };
    }

    /**
     * 返回 DSL renderer/parser 共享的优先级定义。
     */
    private int precedence(Expression expression) {
        if (expression instanceof UnaryExpr) {
            return 7;
        }
        if (expression instanceof BinaryExpr binary) {
            return switch (binary.operator()) {
                case LOGICAL_OR -> 1;
                case LOGICAL_AND -> 2;
                case BIT_OR -> 3;
                case BIT_XOR -> 4;
                case BIT_AND -> 5;
                case EQUAL, NOT_EQUAL, LESS_EQUAL, GREATER_EQUAL, LESS_THAN, GREATER_THAN -> 6;
                case SHIFT_RIGHT, SHIFT_LEFT -> 7;
                case ADD, SUBTRACT -> 8;
                case MULTIPLY, DIVIDE, MODULO -> 9;
                case CONCAT -> 10;
            };
        }
        return 10;
    }

    /**
     * 只有在子表达式否则会被解析成不同树形结构时，才补括号。
     */
    private String parenthesizeIfNeeded(String rendered, int precedence, int parentPrecedence) {
        if (precedence < parentPrecedence) {
            return "(" + rendered + ")";
        }
        return rendered;
    }
}
