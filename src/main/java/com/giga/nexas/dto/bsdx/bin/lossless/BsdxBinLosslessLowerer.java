package com.giga.nexas.dto.bsdx.bin.lossless;

import com.giga.nexas.dto.bsdx.bin.consts.BinConst;
import com.giga.nexas.dto.bsdx.bin.consts.Opcode;
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
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterId;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.RegisterTarget;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.Statement;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.StringRefExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.ToStringExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UnaryExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UnaryOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UpdateOperator;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.UpdateStmt;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.VarExpr;
import com.giga.nexas.dto.bsdx.bin.lossless.BsdxBinLosslessProgram.VariableTarget;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIr;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIrInstruction;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把无损 Structured IR 降回 Instruction IR。
 *
 * <p>所属层级：
 * {@code Structured IR -> Instruction IR}。
 *
 * <p>它是 {@link BsdxBinLosslessLifter} 的逆过程，负责重建 round-trip 所需的 VM 指令流，
 * 并刻意避免任何会改变指令顺序的“优化”。
 */
public class BsdxBinLosslessLowerer {

    /**
     * 无损执行 {@code structured IR -> strict IR} 这一步。
     *
     * <p>跳转 label 会在所有语句发射完成之后统一 patch，
     * 这样最终 jump operand 才能和原 BIN 使用同一套 VM 编码规则。
     */
    public BsdxBinStrictIr lower(BsdxBinLosslessProgram program) {
        if (program == null) {
            return null;
        }
        if (program.originalStrictIr() != null) {
            return program.originalStrictIr();
        }

        LoweringState state = new LoweringState();
        for (Statement statement : program.statements()) {
            lowerStatement(statement, state);
        }
        state.resolveLabels();

        List<Integer> entryPointIndices = new ArrayList<>();
        for (int i = 0; i < state.instructions.size(); i++) {
            if (state.instructions.get(i).opcodeNum() == Opcode.START.code) {
                entryPointIndices.add(i);
            }
        }

        return new BsdxBinStrictIr(
                program.extensionName(),
                program.charset(),
                program.preCount(),
                program.preInstructions(),
                List.copyOf(state.instructions),
                program.dataSection(),
                List.copyOf(entryPointIndices),
                program.tailRaw()
        );
    }

    /**
     * 把一条结构化语句 lowering 成一条或多条 VM 指令。
     */
    private void lowerStatement(Statement statement, LoweringState state) {
        if (statement instanceof EntryStmt entry) {
            state.addInstruction(Opcode.START.code, entry.bank());
            return;
        }
        if (statement instanceof EndStmt end) {
            state.addInstruction(Opcode.END.code, end.operand());
            return;
        }
        if (statement instanceof MarkerStmt marker) {
            state.addInstruction(Opcode.MARKER.code, marker.marker());
            return;
        }
        if (statement instanceof LinenoStmt lineno) {
            state.addInstruction(Opcode.LINENO.code, lineno.lineNumber());
            return;
        }
        if (statement instanceof LabelStmt label) {
            state.labels.put(label.name(), state.instructions.size());
            return;
        }
        if (statement instanceof GotoStmt jump) {
            state.addPendingJump(Opcode.JMP.code, jump.targetLabel());
            return;
        }
        if (statement instanceof IfGotoStmt branch) {
            if (branch.condition() instanceof UnaryExpr unary && unary.operator() == UnaryOperator.LOGICAL_NOT) {
                emitExpression(unary.operand(), state);
                state.addPendingJump(Opcode.JMP_IF_FALSE.code, branch.targetLabel());
            } else {
                emitExpression(branch.condition(), state);
                state.addPendingJump(Opcode.JMP_IF_TRUE.code, branch.targetLabel());
            }
            return;
        }
        if (statement instanceof AssignStmt assign) {
            emitAssign(assign, state);
            return;
        }
        if (statement instanceof UpdateStmt update) {
            emitUpdate(update, state);
            return;
        }
        if (statement instanceof PushStmt push) {
            state.addInstruction(Opcode.PUSH.code, registerOperand(push.register()));
            return;
        }
        if (statement instanceof PopStmt pop) {
            state.addInstruction(Opcode.POP.code, registerOperand(pop.register()));
            return;
        }
        if (statement instanceof ParamStmt param) {
            emitExpression(param.value(), state);
            state.addInstruction(Opcode.PARAM.code, param.value() instanceof StringRefExpr ? 1 : 0);
            return;
        }
        if (statement instanceof CallStmt callStmt) {
            emitCall(callStmt.call(), state);
            return;
        }
        if (statement instanceof InvokeStmt invoke) {
            state.addInstruction(Opcode.CALL.code, ((invoke.argc() & 0xFFFF) << 16) | (invoke.target().rawId() & 0xFFFF));
            return;
        }
        throw new IllegalArgumentException("Unsupported statement: " + statement);
    }

    /**
     * 发射寄存器赋值和变量写回对应的底层语义。
     *
     * <p>动态变量写回必须保留“目标槽位经由 R1 传递”的 VM 形式。
     */
    private void emitAssign(AssignStmt assign, LoweringState state) {
        if (assign.target() instanceof RegisterTarget registerTarget) {
            if (registerTarget.register() == RegisterId.R0) {
                emitExpression(assign.value(), state);
                return;
            }
            if (registerTarget.register() == RegisterId.R1) {
                /*
                 * 当前支持子集没有直接的“mov r1, value” opcode。
                 * 精确底层行为是：先算到 r0，再通过 push/pop 转移到 r1。
                 */
                emitExpression(assign.value(), state);
                state.addInstruction(Opcode.PUSH.code, 0);
                state.addInstruction(Opcode.POP.code, 1);
                return;
            }
            throw new IllegalArgumentException("Unsupported register assignment target: " + registerTarget.register());
        }

        if (assign.target() instanceof VariableTarget variableTarget) {
            emitExpression(assign.value(), state);
            if (variableTarget.index() instanceof ConstExpr constant) {
                state.addInstruction(Opcode.STORE.code, constant.value());
            } else {
                state.addInstruction(Opcode.PUSH.code, 0);
                emitExpression(variableTarget.index(), state);
                state.addInstruction(Opcode.POP.code, 1);
                state.addInstruction(Opcode.STORE.code, -1);
            }
            return;
        }

        throw new IllegalArgumentException("Unsupported assignment target: " + assign.target());
    }

    /**
     * 发射底层自增、自减和复合更新 opcode。
     */
    private void emitUpdate(UpdateStmt update, LoweringState state) {
        if (!(update.target() instanceof VariableTarget variableTarget)) {
            throw new IllegalArgumentException("Update target must be a variable slot: " + update.target());
        }

        if (update.operator() == UpdateOperator.INC || update.operator() == UpdateOperator.DEC) {
            if (!(variableTarget.index() instanceof ConstExpr constant)) {
                throw new IllegalArgumentException("INC/DEC currently require a fixed variable slot.");
            }
            state.addInstruction(updateOpcode(update.operator()), constant.value());
            return;
        }

        if (variableTarget.index() instanceof ConstExpr constant) {
            emitExpression(update.value(), state);
            state.addInstruction(updateOpcode(update.operator()), constant.value());
            return;
        }

        /*
         * 动态复合更新遵循 VM 规则：
         * 先把更新值算到 R0，压栈，再把目标索引算到 R0，最后弹到 R1 作为更新值。
         */
        emitExpression(update.value(), state);
        state.addInstruction(Opcode.PUSH.code, 0);
        emitExpression(variableTarget.index(), state);
        state.addInstruction(Opcode.POP.code, 1);
        state.addInstruction(updateOpcode(update.operator()), -1);
    }

    /**
     * 发射 PARAM/CALL 指令，并且严格保留参数顺序。
     */
    private void emitCall(CallExpr call, LoweringState state) {
        for (Expression argument : call.arguments()) {
            if (argument instanceof StringRefExpr stringRef) {
                emitExpression(stringRef, state);
                state.addInstruction(Opcode.PARAM.code, 1);
            } else {
                emitExpression(argument, state);
                state.addInstruction(Opcode.PARAM.code, 0);
            }
        }
        int argc = call.arguments().size();
        int rawId = call.target().rawId();
        state.addInstruction(Opcode.CALL.code, ((argc & 0xFFFF) << 16) | (rawId & 0xFFFF));
    }

    /**
     * 把一条结构化表达式 lowering 成一串 VM 指令，并保证结果落在 R0。
     *
     * <p>这是可逆 lowering 的核心方法。它保持 VM 寄存器纪律显式存在，
     * 而不是对表达式树做高层化简。
     */
    private void emitExpression(Expression expression, LoweringState state) {
        if (expression instanceof ConstExpr constant) {
            state.addInstruction(Opcode.VAL.code, constant.value());
            return;
        }
        if (expression instanceof RegisterExpr registerExpr) {
            if (registerExpr.register() == RegisterId.R0) {
                return;
            }
            if (registerExpr.register() == RegisterId.R1) {
                state.addInstruction(Opcode.PUSH.code, 1);
                state.addInstruction(Opcode.POP.code, 0);
                return;
            }
            throw new IllegalArgumentException("Unsupported register read: " + registerExpr.register());
        }
        if (expression instanceof VarExpr varExpr) {
            emitExpression(varExpr.index(), state);
            state.addInstruction(Opcode.LOAD.code, 0);
            return;
        }
        if (expression instanceof StringRefExpr stringRef) {
            emitStringIndex(stringRef, state);
            return;
        }
        if (expression instanceof ToStringExpr toStringExpr) {
            emitToString(toStringExpr.value(), state);
            return;
        }
        if (expression instanceof CallExpr callExpr) {
            emitCall(callExpr, state);
            return;
        }
        if (expression instanceof UnaryExpr unary) {
            emitExpression(unary.operand(), state);
            switch (unary.operator()) {
                case NEGATE -> {
                    /*
                     * 当前子集没有独立的一元取负 opcode，
                     * 所以这里无损地用 “value * -1” 来表示取负。
                     */
                    state.addInstruction(Opcode.PUSH.code, 0);
                    state.addInstruction(Opcode.VAL.code, -1);
                    state.addInstruction(Opcode.POP.code, 1);
                    state.addInstruction(Opcode.MUL.code, 0);
                }
                case LOGICAL_NOT -> state.addInstruction(Opcode.CMP_ZERO.code, 0);
                case BITWISE_NOT -> state.addInstruction(20, 0);
            }
            return;
        }
        if (expression instanceof BinaryExpr binary) {
            if (binary.left() instanceof RegisterExpr registerExpr && registerExpr.register() == RegisterId.R1) {
                /*
                 * 当左操作数已经是存活中的 R1 时，VM 可以直接消费它。
                 * 如果还按通用路径重发左侧求值，会平白多出无关指令。
                 */
                emitExpression(binary.right(), state);
                state.addInstruction(opcode(binary.operator()), operatorOperand(binary.operator()));
                return;
            }
            if (binary.operator() == BinaryOperator.CONCAT && binary.right() instanceof ToStringExpr toStringExpr) {
                emitExpression(binary.left(), state);
                state.addInstruction(Opcode.PUSH.code, 0);
                emitExpression(toStringExpr.value(), state);
                state.addInstruction(Opcode.POP.code, 1);
                emitToString(new RegisterExpr(RegisterId.R0), state);
                state.addInstruction(opcode(binary.operator()), operatorOperand(binary.operator()));
                return;
            }
            emitExpression(binary.left(), state);
            state.addInstruction(Opcode.PUSH.code, 0);
            emitExpression(binary.right(), state);
            state.addInstruction(Opcode.POP.code, 1);
            state.addInstruction(opcode(binary.operator()), operatorOperand(binary.operator()));
            return;
        }
        throw new IllegalArgumentException("Unsupported expression: " + expression);
    }

    /**
     * 发射 TO_STRING 底层序列。
     */
    private void emitToString(Expression value, LoweringState state) {
        if (value instanceof RegisterExpr registerExpr && registerExpr.register() == RegisterId.R1) {
            state.addInstruction(Opcode.TO_STRING.code, 1);
            return;
        }
        if (!(value instanceof RegisterExpr registerExpr && registerExpr.register() == RegisterId.R0)) {
            emitExpression(value, state);
        }
        state.addInstruction(Opcode.TO_STRING.code, 0);
    }

    /**
     * 发射字符串引用在 VM 中使用的立即数形式。
     *
     * <p>{@code STR2} 分支必须保留引擎使用的高位编码，
     * 不能随意改写成更高层符号，否则很难精确 lowering 回去。
     */
    private void emitStringIndex(StringRefExpr stringRef, LoweringState state) {
        if (stringRef.kind() == BsdxBinLosslessProgram.StringRefKind.STR) {
            emitExpression(stringRef.index(), state);
            return;
        }
        if (stringRef.kind() == BsdxBinLosslessProgram.StringRefKind.STR2) {
            if (!(stringRef.index() instanceof ConstExpr constant)) {
                throw new IllegalArgumentException("STR2 currently requires a constant index.");
            }
            long raw = 0x80000000L | (constant.value() & 0x0FFFFFFFL);
            state.addInstruction(Opcode.VAL.code, (int) raw);
            return;
        }
        emitExpression(stringRef.index(), state);
    }

    /**
     * 把结构化双目运算符映射回统一 opcode 枚举。
     */
    private int opcode(BinaryOperator operator) {
        return switch (operator) {
            case ADD -> Opcode.ADD.code;
            case SUBTRACT -> Opcode.SUB.code;
            case MULTIPLY -> Opcode.MUL.code;
            case DIVIDE -> Opcode.DIV.code;
            case MODULO -> Opcode.MOD.code;
            case LOGICAL_OR -> Opcode.OR.code;
            case LOGICAL_AND -> Opcode.AND.code;
            case BIT_OR -> Opcode.BIT_OR.code;
            case BIT_AND -> Opcode.BIT_AND.code;
            case BIT_XOR -> Opcode.XOR.code;
            case LESS_EQUAL -> Opcode.CMP_LE.code;
            case GREATER_EQUAL -> Opcode.CMP_GE.code;
            case LESS_THAN -> Opcode.CMP_LT.code;
            case GREATER_THAN -> Opcode.CMP_GT.code;
            case EQUAL -> Opcode.CMP_EQ.code;
            case NOT_EQUAL -> Opcode.CMP_NE.code;
            case SHIFT_RIGHT -> Opcode.SAR.code;
            case SHIFT_LEFT -> Opcode.SHL.code;
            case CONCAT -> Opcode.ADD.code;
        };
    }

    /**
     * 返回运算符在 opcode operand 中承载的类型位。
     */
    private int operatorOperand(BinaryOperator operator) {
        return switch (operator) {
            case CONCAT -> 1;
            default -> 0;
        };
    }

    /**
     * 把结构化寄存器枚举映射成 PUSH/POP 使用的寄存器编号。
     */
    private int registerOperand(RegisterId register) {
        return switch (register) {
            case R0 -> 0;
            case R1 -> 1;
        };
    }

    /**
     * 把更新语句运算符映射回底层更新类 opcode。
     */
    private int updateOpcode(UpdateOperator operator) {
        return switch (operator) {
            case INC -> Opcode.INC.code;
            case DEC -> Opcode.DEC.code;
            case ADD_ASSIGN -> Opcode.LD_ADD.code;
            case SUB_ASSIGN -> Opcode.LD_SUB.code;
            case MUL_ASSIGN -> Opcode.LD_MUL.code;
            case DIV_ASSIGN -> Opcode.LD_DIV.code;
            case MOD_ASSIGN -> Opcode.LD_MOD.code;
            case OR_ASSIGN -> Opcode.LD_OR.code;
            case AND_ASSIGN -> Opcode.LD_AND.code;
            case XOR_ASSIGN -> Opcode.LD_XOR.code;
            case SHR_ASSIGN -> Opcode.LD_SAR.code;
            case SHL_ASSIGN -> Opcode.LD_SHL.code;
        };
    }

    /**
     * 重建指令流时使用的可变状态。
     */
    private static final class LoweringState {
        /**
         * 按输出顺序重建出来的指令流。
         */
        private final List<BsdxBinStrictIrInstruction> instructions = new ArrayList<>();

        /**
         * Structured IR 发出的 synthetic label 与其指令索引的映射。
         */
        private final Map<String, Integer> labels = new LinkedHashMap<>();

        /**
         * 需要第二遍才能解析目标 operand 的跳转列表。
         */
        private final List<PendingJump> jumps = new ArrayList<>();

        /**
         * 往重建流尾部追加一条原始指令。
         *
         * <p>CALL 的辅助字段也会在这里重算，
         * 这样后续严格比较时看到的是完整 strict IR 节点。
         */
        private void addInstruction(int opcodeNum, int operandNum) {
            int index = instructions.size();
            Integer nativeId = null;
            Integer arity = null;
            String nativeMnemonic = null;
            if (opcodeNum == Opcode.CALL.code) {
                nativeId = operandNum & 0xFFFF;
                arity = operandNum >>> 16;
                nativeMnemonic = BinConst.OPERAND_MNEMONIC_MAP.get(nativeId);
            }
            instructions.add(new BsdxBinStrictIrInstruction(
                    index,
                    opcodeNum,
                    operandNum,
                    BinConst.OPCODE_MNEMONIC_MAP.get(opcodeNum),
                    arity,
                    nativeId,
                    nativeMnemonic
            ));
        }

        /**
         * 发射一条 jump 占位指令，等 label 已知后再回填 operand。
         */
        private void addPendingJump(int opcodeNum, String label) {
            int index = instructions.size();
            addInstruction(opcodeNum, 0);
            jumps.add(new PendingJump(index, label));
        }

        /**
         * 按照 {@code targetIndex - 1} 规则，把 synthetic label 解析回 VM jump operand。
         */
        private void resolveLabels() {
            for (PendingJump jump : jumps) {
                Integer targetIndex = labels.get(jump.label());
                if (targetIndex == null) {
                    throw new IllegalArgumentException("Unknown label: " + jump.label());
                }
                BsdxBinStrictIrInstruction current = instructions.get(jump.instructionIndex());
                instructions.set(jump.instructionIndex(), new BsdxBinStrictIrInstruction(
                        current.index(),
                        current.opcodeNum(),
                        targetIndex - 1,
                        current.opcodeMnemonic(),
                        current.callArity(),
                        current.callNativeId(),
                        current.callNativeMnemonic()
                ));
            }
        }
    }

    /**
     * 一条等待 label 解析的 jump。
     */
    private record PendingJump(int instructionIndex, String label) {
    }
}
