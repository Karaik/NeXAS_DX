package com.giga.nexas.dto.bsdx.bin.lossless;

import com.giga.nexas.dto.bsdx.bin.consts.Opcode;
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
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIr;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIrInstruction;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 把 Instruction IR 提升为无损 Structured IR。
 *
 * <p>所属层级：
 * {@code Instruction IR -> Structured IR}。
 *
 * <p>这个类会从平铺指令流里重建寄存器值、算术栈、参数栈和跳转 label。
 * 它刻意保持保守，不恢复高级块结构，而是保留 VM 可见的控制流和寄存器副作用，
 * 这样 lowering 才能回到同一条 opcode 序列。
 */
public class BsdxBinLosslessLifter {

    /**
     * 无损执行 {@code strict IR -> structured IR} 这一步。
     *
     * <p>在当前已支持 opcode 子集内，这一步要求无损。
     * 一旦遇到未支持 opcode 或虚拟机状态不一致，就直接失败，不做猜测。
     */
    public BsdxBinLosslessProgram lift(BsdxBinStrictIr strictIr) {
        if (strictIr == null) {
            return null;
        }

        LiftState state = new LiftState(collectLabels(strictIr.instructions()));
        for (int index = 0; index < strictIr.instructions().size(); index++) {
            String label = state.labels.get(index);
            if (label != null) {
                materializePendingParams(state);
                materializePendingStack(state);
                materializeR1IfNeeded(state, strictIr.instructions(), index);
                flushPending(state);
                state.statements.add(new LabelStmt(label));
            }
            BsdxBinStrictIrInstruction instruction = strictIr.instructions().get(index);
            try {
                liftInstruction(instruction, state, strictIr.instructions(), index);
            } catch (RuntimeException ex) {
                throw new IllegalArgumentException(
                        "lift failed at instruction index " + index
                                + " opcode=" + instruction.opcodeNum()
                                + " operand=" + instruction.operandNum()
                                + " -> " + ex.getMessage(),
                        ex
                );
            }
        }
        flushPending(state);

        if (!state.arithmeticStack.isEmpty() || !state.parameterStack.isEmpty()) {
            throw new IllegalArgumentException("Unbalanced virtual stacks after lifting.");
        }

        return new BsdxBinLosslessProgram(
                strictIr.extensionName(),
                strictIr.charset(),
                strictIr.preCount(),
                strictIr.preInstructions(),
                strictIr.dataSection(),
                strictIr.tailRaw(),
                state.statements,
                strictIr
        );
    }

    /**
     * 把一条底层指令提升到当前 Structured IR 状态机里。
     *
     * <p>外部 state 持有 VM 的瞬时栈和寄存器值，这样本方法才能在不丢失求值顺序的前提下，
     * 把平铺 opcode 流转成带类型的语句与表达式。
     */
    private void liftInstruction(
            BsdxBinStrictIrInstruction instruction,
            LiftState state,
            List<BsdxBinStrictIrInstruction> instructions,
            int instructionIndex
    ) {
        int opcode = instruction.opcodeNum();
        int operand = instruction.operandNum();
        if (opcode == Opcode.CMP_ZERO.code) {
            state.r0 = new UnaryExpr(UnaryOperator.LOGICAL_NOT, requireExpr(state.r0, "LOGICAL_NOT"));
            return;
        }
        if (opcode == Opcode.INC.code) {
            materializePendingParams(state);
            materializePendingStack(state);
            materializeR1IfNeeded(state, instructions, instructionIndex);
            flushPending(state);
            state.statements.add(new UpdateStmt(constantTarget(operand), UpdateOperator.INC, null));
            return;
        }
        if (opcode == Opcode.DEC.code) {
            materializePendingParams(state);
            materializePendingStack(state);
            materializeR1IfNeeded(state, instructions, instructionIndex);
            flushPending(state);
            state.statements.add(new UpdateStmt(constantTarget(operand), UpdateOperator.DEC, null));
            return;
        }
        UpdateOperator updateOperator = updateOperatorForOpcode(opcode);
        if (updateOperator != null) {
            materializePendingParams(state);
            materializePendingStack(state);
            state.statements.add(liftUpdate(state, operand, updateOperator));
            return;
        }
        switch (opcode) {
            case 0 -> {
                flushBeforeR0Write(state);
                state.r0 = new ConstExpr(operand);
            }
            case 4 -> {
                if (operand == 0) {
                    state.arithmeticStack.push(new StackValue(RegisterId.R0, requireExpr(state.r0, "PUSH R0")));
                    state.r0 = null;
                } else if (operand == 1) {
                    state.arithmeticStack.push(new StackValue(RegisterId.R1, requireExpr(state.r1, "PUSH R1")));
                    state.r1 = null;
                } else {
                    throw unsupported(instruction, "Unsupported PUSH register");
                }
            }
            case 5 -> {
                /*
                 * PARAM 会消费当前 R0，并且记录 VM 把它当成数值参数还是字符串表引用。
                 * 这个差异必须一直保留到 DSL 层，否则回编时会失真。
                 */
                state.parameterStack.push(new ParameterValue(toParameterExpression(requireExpr(state.r0, "PARAM"), operand)));
                state.r0 = null;
            }
            case 6 -> {
                StackValue popped = state.arithmeticStack.pollFirst();
                if (popped == null) {
                    if (operand == 0) {
                        flushPending(state);
                        state.statements.add(new PopStmt(RegisterId.R0));
                        state.r0 = new RegisterExpr(RegisterId.R0);
                    } else if (operand == 1) {
                        boolean hadR0 = state.r0 != null;
                        flushPending(state);
                        if (hadR0) {
                            state.r0 = new RegisterExpr(RegisterId.R0);
                        }
                        state.statements.add(new PopStmt(RegisterId.R1));
                        state.r1 = new RegisterExpr(RegisterId.R1);
                    } else {
                        throw unsupported(instruction, "Unsupported POP register");
                    }
                    return;
                }
                if (operand == 0) {
                    flushBeforeR0Write(state);
                    state.r0 = popped.expression();
                } else if (operand == 1) {
                    state.r1 = popped.expression();
                } else {
                    throw unsupported(instruction, "Unsupported POP register");
                }
            }
            case 7 -> {
                flushBeforeR0Write(state);
                int argc = instruction.callArity() != null ? instruction.callArity() : (instruction.operandNum() >>> 16);
                if (state.parameterStack.size() >= argc) {
                    state.r0 = buildCallExpr(instruction, state);
                } else {
                    materializePendingParams(state);
                    state.statements.add(new InvokeStmt(callTarget(instruction), argc));
                    state.r0 = new RegisterExpr(RegisterId.R0);
                }
            }
            case 8 -> {
                if (operand == 0) {
                    state.r0 = new VarExpr(requireExpr(state.r0, "LOAD R0"));
                } else if (operand == 1) {
                    state.r1 = new VarExpr(requireExpr(state.r1, "LOAD R1"));
                } else {
                    throw unsupported(instruction, "Unsupported LOAD register");
                }
            }
            case 9 -> state.r0 = binary(state, operand == 1 ? BinaryOperator.CONCAT : BinaryOperator.ADD);
            case 10 -> state.r0 = binary(state, BinaryOperator.SUBTRACT);
            case 11 -> state.r0 = binary(state, BinaryOperator.MULTIPLY);
            case 12 -> state.r0 = binary(state, BinaryOperator.DIVIDE);
            case 13 -> state.r0 = binary(state, BinaryOperator.MODULO);
            case 14 -> {
                /*
                 * STORE 是当前子集中少数既可能写固定槽位、也可能写 R1 动态槽位的 opcode。
                 * 所以 AST 必须把两种形式都显式建出来，lowering 才能还原精确 opcode。
                 */
                Expression value = requireExpr(state.r0, "STORE");
                Expression index = operand == -1 ? requireExpr(state.r1, "STORE dynamic target") : new ConstExpr(operand);
                state.statements.add(new AssignStmt(new VariableTarget(index), value));
                state.r0 = null;
                state.r1 = null;
            }
            case 15 -> state.r0 = binary(state, BinaryOperator.LOGICAL_OR);
            case 16 -> state.r0 = binary(state, BinaryOperator.LOGICAL_AND);
            case 17 -> state.r0 = binary(state, BinaryOperator.BIT_OR);
            case 18 -> state.r0 = binary(state, BinaryOperator.BIT_AND);
            case 19 -> state.r0 = binary(state, BinaryOperator.BIT_XOR);
            case 20 -> state.r0 = new UnaryExpr(UnaryOperator.BITWISE_NOT, requireExpr(state.r0, "BITWISE_NOT"));
            case 21 -> state.r0 = binary(state, BinaryOperator.LESS_EQUAL);
            case 22 -> state.r0 = binary(state, BinaryOperator.GREATER_EQUAL);
            case 23 -> state.r0 = binary(state, BinaryOperator.LESS_THAN);
            case 24 -> state.r0 = binary(state, BinaryOperator.GREATER_THAN);
            case 25 -> state.r0 = binary(state, BinaryOperator.EQUAL);
            case 26 -> state.r0 = binary(state, BinaryOperator.NOT_EQUAL);
            case 27 -> {
                materializePendingParams(state);
                materializePendingStack(state);
                materializeR1IfNeeded(state, instructions, instructionIndex);
                flushPending(state);
                state.statements.add(new EntryStmt(operand));
            }
            case 28 -> {
                materializePendingParams(state);
                materializePendingStack(state);
                materializeR1IfNeeded(state, instructions, instructionIndex);
                flushPending(state);
                state.statements.add(new EndStmt(operand));
            }
            case 29 -> {
                materializePendingParams(state);
                materializePendingStack(state);
                flushPending(state);
                state.statements.add(new MarkerStmt(operand));
            }
            case 44 -> {
                materializePendingParams(state);
                materializePendingStack(state);
                flushPending(state);
                state.statements.add(new LinenoStmt(operand));
            }
            case 45 -> state.r0 = binary(state, BinaryOperator.SHIFT_RIGHT);
            case 46 -> state.r0 = binary(state, BinaryOperator.SHIFT_LEFT);
            case 61 -> {
                if (operand == 0) {
                    state.r0 = new ToStringExpr(requireExpr(state.r0, "TO_STRING R0"));
                } else if (operand == 1) {
                    state.r1 = new ToStringExpr(requireExpr(state.r1, "TO_STRING R1"));
                } else {
                    throw unsupported(instruction, "Unsupported TO_STRING register");
                }
            }
            case 63 -> {
                materializePendingParams(state);
                materializePendingStack(state);
                materializeR1IfNeeded(state, instructions, instructionIndex);
                flushPending(state);
                state.statements.add(new GotoStmt(resolveLabel(state.labels, operand + 1)));
            }
            case 64 -> {
                materializePendingParams(state);
                materializePendingStack(state);
                materializeR1IfNeeded(state, instructions, instructionIndex);
                Expression condition = requireExpr(state.r0, "JMP_IF_FALSE");
                state.statements.add(new IfGotoStmt(new UnaryExpr(UnaryOperator.LOGICAL_NOT, condition), resolveLabel(state.labels, operand + 1)));
                state.r0 = null;
            }
            case 65 -> {
                materializePendingParams(state);
                materializePendingStack(state);
                materializeR1IfNeeded(state, instructions, instructionIndex);
                state.statements.add(new IfGotoStmt(requireExpr(state.r0, "JMP_IF_TRUE"), resolveLabel(state.labels, operand + 1)));
                state.r0 = null;
            }
            default -> throw unsupported(instruction, "Opcode is outside the current lossless subset");
        }
    }

    /**
     * 从 VM 参数栈重建一个 CALL 表达式。
     *
     * <p>在当前支持子集里它是无损的：参数顺序、arity 和原始函数 id 都直接来自原指令流。
     */
    private CallExpr buildCallExpr(BsdxBinStrictIrInstruction instruction, LiftState state) {
        int argc = instruction.callArity() != null ? instruction.callArity() : (instruction.operandNum() >>> 16);
        List<Expression> arguments = new ArrayList<>(argc);
        for (int i = 0; i < argc; i++) {
            ParameterValue value = state.parameterStack.pollFirst();
            if (value == null) {
                throw unsupported(instruction, "CALL arity exceeds prepared parameter stack");
            }
            arguments.add(0, value.expression());
        }
        return new CallExpr(callTarget(instruction), arguments);
    }

    /**
     * 从 CALL 指令提取目标元数据。
     */
    private CallTarget callTarget(BsdxBinStrictIrInstruction instruction) {
        int rawId = instruction.callNativeId() != null ? instruction.callNativeId() : (instruction.operandNum() & 0xFFFF);
        return new CallTarget(rawId, instruction.callNativeMnemonic(), CallKind.NATIVE);
    }

    /**
     * 把 PARAM 语义转换成显式的结构化表达式。
     *
     * <p>{@code PARAM 0} 直接保留数值表达式。
     * {@code PARAM 1} 则必须保留它到底来自主字符串表、次字符串表，还是运行时表达式。
     */
    private Expression toParameterExpression(Expression expression, int paramKind) {
        if (paramKind == 0) {
            return expression;
        }
        if (paramKind == 1) {
            if (expression instanceof ConstExpr constant) {
                long unsigned = Integer.toUnsignedLong(constant.value());
                if (unsigned >= 0x80000000L) {
                    return new StringRefExpr(StringRefKind.STR2, new ConstExpr(constant.value() & 0x0FFFFFFF));
                }
                return new StringRefExpr(StringRefKind.STR, expression);
            }
            return new StringRefExpr(StringRefKind.RAW, expression);
        }
        throw new IllegalArgumentException("Unsupported PARAM kind: " + paramKind);
    }

    /**
     * 按照 VM 约定 {@code R1 op R0 -> R0} 构造双目表达式。
     *
     * <p>操作数顺序直接影响可逆性，所以这里绝不交换左右侧，也不做表达式化简。
     */
    private Expression binary(LiftState state, BinaryOperator operator) {
        Expression right = requireExpr(state.r0, operator.name());
        Expression left = requireExpr(state.r1, operator.name());
        return new BinaryExpr(operator, left, right);
    }

    /**
     * 把原地更新类 opcode 提升成显式 UpdateStmt。
     *
     * <p>固定槽位形式读取更新值自 R0；动态槽位形式遵循 VM 约定：R0 持有目标索引，R1 持有更新值。
     */
    private UpdateStmt liftUpdate(LiftState state, int operand, UpdateOperator operator) {
        Expression targetIndex = operand == -1 ? requireExpr(state.r0, operator.name() + " dynamic target") : new ConstExpr(operand);
        Expression value = operand == -1 ? requireExpr(state.r1, operator.name() + " dynamic value") : requireExpr(state.r0, operator.name());
        UpdateStmt stmt = new UpdateStmt(new VariableTarget(targetIndex), operator, value);
        state.r0 = null;
        if (operand == -1) {
            state.r1 = null;
        }
        return stmt;
    }

    /**
     * 为固定变量槽位创建 target 节点。
     */
    private VariableTarget constantTarget(int operand) {
        return new VariableTarget(new ConstExpr(operand));
    }

    /**
     * 当算术栈跨过语句边界时，把它显式化成 push 语句。
     *
     * <p>这一步是为了解决“语义上可折叠、但字节上必须保留”的栈操作。
     */
    private void materializePendingStack(LiftState state) {
        if (state.arithmeticStack.isEmpty()) {
            return;
        }
        List<StackValue> pending = new ArrayList<>();
        while (!state.arithmeticStack.isEmpty()) {
            pending.add(0, state.arithmeticStack.pollFirst());
        }
        for (StackValue value : pending) {
            state.statements.add(new AssignStmt(new RegisterTarget(value.register()), value.expression()));
            state.statements.add(new PushStmt(value.register()));
        }
    }

    /**
     * 当参数准备过程被语句边界打断时，把当前参数栈显式化成 param 语句。
     */
    private void materializePendingParams(LiftState state) {
        if (state.parameterStack.isEmpty()) {
            return;
        }
        List<ParameterValue> pending = new ArrayList<>();
        while (!state.parameterStack.isEmpty()) {
            pending.add(0, state.parameterStack.pollFirst());
        }
        for (ParameterValue value : pending) {
            state.statements.add(new ParamStmt(value.expression()));
        }
    }

    /**
     * 把底层更新类 opcode 映射成结构化更新运算符。
     */
    private UpdateOperator updateOperatorForOpcode(int opcode) {
        if (opcode == Opcode.LD_ADD.code) {
            return UpdateOperator.ADD_ASSIGN;
        }
        if (opcode == Opcode.LD_SUB.code) {
            return UpdateOperator.SUB_ASSIGN;
        }
        if (opcode == Opcode.LD_MUL.code) {
            return UpdateOperator.MUL_ASSIGN;
        }
        if (opcode == Opcode.LD_DIV.code) {
            return UpdateOperator.DIV_ASSIGN;
        }
        if (opcode == Opcode.LD_MOD.code) {
            return UpdateOperator.MOD_ASSIGN;
        }
        if (opcode == Opcode.LD_OR.code) {
            return UpdateOperator.OR_ASSIGN;
        }
        if (opcode == Opcode.LD_AND.code) {
            return UpdateOperator.AND_ASSIGN;
        }
        if (opcode == Opcode.LD_XOR.code) {
            return UpdateOperator.XOR_ASSIGN;
        }
        if (opcode == Opcode.LD_SAR.code) {
            return UpdateOperator.SHR_ASSIGN;
        }
        if (opcode == Opcode.LD_SHL.code) {
            return UpdateOperator.SHL_ASSIGN;
        }
        return null;
    }

    /**
     * 在新指令覆盖 R0 之前，把挂起的 R0 表达式落成显式语句。
     *
     * <p>这里是“瞬时 VM 状态”变成“显式结构语句”的关键时刻。
     */
    private void flushBeforeR0Write(LiftState state) {
        if (state.r0 != null) {
            flushPending(state);
        }
    }

    /**
     * 把当前 R0 表达式显式化成 {@code call ...} 或 {@code r0 = ...}。
     *
     * <p>这里不丢语义，只负责决定什么时候必须把瞬时寄存器值落成结构化语句。
     */
    private void flushPending(LiftState state) {
        if (state.r0 == null) {
            return;
        }
        if (state.r0 instanceof CallExpr callExpr) {
            state.statements.add(new CallStmt(callExpr));
        } else {
            state.statements.add(new AssignStmt(new RegisterTarget(RegisterId.R0), state.r0));
        }
        state.r0 = null;
    }

    /**
     * 当后续指令还会继续复用 R1 时，把它强制显式化成 {@code r1 = ...}。
     *
     * <p>lifter 会尽量避免无谓 materialize R1，但如果不显式化就无法精确 lowering，
     * 那就必须提前落成语句。
     */
    private void materializeR1IfNeeded(
            LiftState state,
            List<BsdxBinStrictIrInstruction> instructions,
            int currentInstructionIndex
    ) {
        if (state.r1 == null || state.r1 instanceof RegisterExpr) {
            return;
        }
        if (!willReuseR1BeforeOverwrite(instructions, currentInstructionIndex + 1)) {
            return;
        }

        Expression previous = state.r1;
        state.statements.add(new AssignStmt(new RegisterTarget(RegisterId.R1), previous));
        if (state.r0 != null) {
            state.r0 = replaceExpression(state.r0, previous, new RegisterExpr(RegisterId.R1));
        }
        state.r1 = new RegisterExpr(RegisterId.R1);
    }

    /**
     * 向前查看指令流，判断当前瞬时 R1 是否必须跨指令存活。
     *
     * <p>这不是高层优化，而是可逆性检查。
     * 因为有些 VM 表达式会在 R1 被覆盖前跨多个 opcode 继续复用它。
     */
    private boolean willReuseR1BeforeOverwrite(List<BsdxBinStrictIrInstruction> instructions, int startIndex) {
        for (int index = startIndex; index < instructions.size(); index++) {
            BsdxBinStrictIrInstruction instruction = instructions.get(index);
            int opcode = instruction.opcodeNum();
            int operand = instruction.operandNum();
            if (opcode == Opcode.START.code || opcode == Opcode.END.code) {
                return false;
            }
            if (opcode == Opcode.POP.code && operand == 1) {
                return false;
            }
            if (opcode == Opcode.LOAD.code && operand == 1) {
                return false;
            }
            if (opcode == Opcode.PUSH.code && operand == 1) {
                return true;
            }
            if (opcode == Opcode.STORE.code && operand == -1) {
                return true;
            }
            if (usesR1(opcode)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断某个底层 opcode 在求值时是否会消费 R1。
     */
    private boolean usesR1(int opcode) {
        return switch (opcode) {
            case 9, 10, 11, 12, 13, 15, 16, 17, 18, 19, 21, 22, 23, 24, 25, 26, 45, 46 -> true;
            default -> false;
        };
    }

    /**
     * 在更大的表达式树里替换一个子表达式。
     *
     * <p>当瞬时 R1 被显式化成寄存器赋值后，已经构建好的 R0 表达式也要改成引用 {@code r1}，
     * 不能继续内联旧子树。
     */
    private Expression replaceExpression(Expression expression, Expression target, Expression replacement) {
        if (expression == target) {
            return replacement;
        }
        if (expression instanceof UnaryExpr unary) {
            return new UnaryExpr(unary.operator(), replaceExpression(unary.operand(), target, replacement));
        }
        if (expression instanceof BinaryExpr binary) {
            return new BinaryExpr(
                    binary.operator(),
                    replaceExpression(binary.left(), target, replacement),
                    replaceExpression(binary.right(), target, replacement)
            );
        }
        if (expression instanceof CallExpr call) {
            List<Expression> args = new ArrayList<>(call.arguments().size());
            for (Expression argument : call.arguments()) {
                args.add(replaceExpression(argument, target, replacement));
            }
            return new CallExpr(call.target(), args);
        }
        if (expression instanceof VarExpr varExpr) {
            return new VarExpr(replaceExpression(varExpr.index(), target, replacement));
        }
        if (expression instanceof StringRefExpr stringRef) {
            return new StringRefExpr(stringRef.kind(), replaceExpression(stringRef.index(), target, replacement));
        }
        return expression;
    }

    /**
     * 在 opcode 消费当前 VM 寄存器表达式之前，检查它确实存在。
     */
    private Expression requireExpr(Expression expression, String action) {
        if (expression == null) {
            throw new IllegalArgumentException("Missing R0/R1 expression while lifting " + action);
        }
        return expression;
    }

    /**
     * 扫描跳转指令，并为目标索引分配稳定的 synthetic label。
     *
     * <p>这些 label 是确定性的，只依赖指令位置，不依赖样本文件名或用户命名。
     */
    private Map<Integer, String> collectLabels(List<BsdxBinStrictIrInstruction> instructions) {
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (BsdxBinStrictIrInstruction instruction : instructions) {
            int opcode = instruction.opcodeNum();
            if (opcode != 63 && opcode != 64 && opcode != 65) {
                continue;
            }
            int targetIndex = instruction.operandNum() + 1;
            labels.computeIfAbsent(targetIndex, ignored -> "L" + labels.size());
        }
        return labels;
    }

    /**
     * 把跳转目标索引解析成预扫描阶段创建的 synthetic label。
     */
    private String resolveLabel(Map<Integer, String> labels, int targetIndex) {
        String label = labels.get(targetIndex);
        if (label == null) {
            throw new IllegalArgumentException("Missing label for jump target index " + targetIndex);
        }
        return label;
    }

    /**
     * 为第一版 structured 子集中未支持的 opcode 生成带上下文的异常。
     */
    private IllegalArgumentException unsupported(BsdxBinStrictIrInstruction instruction, String message) {
        return new IllegalArgumentException(message + ": opcode=" + instruction.opcodeNum() + ", operand=" + instruction.operandNum());
    }

    /**
     * lifting 平铺指令流时使用的可变 VM 重建状态。
     */
    private static final class LiftState {
        /**
         * 以指令索引为 key 的 synthetic label 表。
         */
        private final Map<Integer, String> labels;
        /**
         * 当前已经输出的结构化语句列表。
         */
        private final List<Statement> statements = new ArrayList<>();
        /**
         * 由 PUSH/POP 重建出来的算术栈。
         */
        private final Deque<StackValue> arithmeticStack = new ArrayDeque<>();
        /**
         * 由 PARAM/CALL 重建出来的参数栈。
         */
        private final Deque<ParameterValue> parameterStack = new ArrayDeque<>();
        /**
         * VM 寄存器 R0 当前持有的表达式值。
         */
        private Expression r0;
        /**
         * VM 寄存器 R1 当前持有的表达式值。
         */
        private Expression r1;

        private LiftState(Map<Integer, String> labels) {
            this.labels = labels;
        }
    }

    /**
     * 一个待消费的 CALL 参数，以及 PARAM 为它选定的解释语义。
     */
    private record ParameterValue(Expression expression) {
    }

    /**
     * 算术栈里保存的一项值，同时记录它原本来自哪个寄存器。
     */
    private record StackValue(RegisterId register, Expression expression) {
    }
}
