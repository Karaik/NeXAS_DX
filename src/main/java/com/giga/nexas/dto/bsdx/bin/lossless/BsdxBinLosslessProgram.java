package com.giga.nexas.dto.bsdx.bin.lossless;

import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIrDataSection;
import com.giga.nexas.dto.bsdx.bin.strictir.BsdxBinStrictIr;

import java.util.List;

/**
 * 新可逆 BIN 主链中的 Lossless Structured IR 层。
 *
 * <p>它位于 {@code Bin -> Instruction IR -> Lossless Structured IR -> Lossless Pseudo DSL} 这条链的中间，
 * 负责把平铺的指令流提升成带类型的语句/表达式树，同时继续保留所有非代码元数据。
 *
 * <p>这一层刻意保守：label、goto、寄存器副作用、native call、更新类语句都保持显式，
 * 不在这里做 CFG 结构化或“高级语法美化”，这样 lowering 才能精确重建原指令序列。
 *
 * @param extensionName 原始文件扩展名，回写二进制时仍然需要
 * @param charset 原始字符集，降低回 {@code Bin} 时必须保留
 * @param preCount BIN 头中的预指令数量
 * @param preInstructions 预指令原始字节的无符号整数镜像
 * @param dataSection 字符串表、属性表、constants、68 字节表等数据区镜像
 * @param tailRaw 解析器在已知结构之外保留下来的尾部字节
 * @param statements 表示可执行指令流的结构化语句列表
 * @param originalStrictIr 当本程序直接来自 lifting 且尚未被用户修改时，可用于精确回放的原始 strict IR
 */
public record BsdxBinLosslessProgram(
        String extensionName,
        String charset,
        int preCount,
        List<Integer> preInstructions,
        BsdxBinStrictIrDataSection dataSection,
        List<Integer> tailRaw,
        List<Statement> statements,
        BsdxBinStrictIr originalStrictIr
) {

    /**
     * 规范化所有集合组件，确保后续阶段看到的都是不可变列表。
     *
     * <p>这里不做任何语义改写，只把 {@code null} 容器替换成空不可变集合，因此保持无损。
     */
    public BsdxBinLosslessProgram {
        preInstructions = preInstructions == null ? List.of() : List.copyOf(preInstructions);
        tailRaw = tailRaw == null ? List.of() : List.copyOf(tailRaw);
        statements = statements == null ? List.of() : List.copyOf(statements);
    }

    /**
     * Structured IR 中所有语句节点的基接口。
     *
     * <p>语句集合被故意控制得很小，并且尽量直接映射到底层脚本效果，
     * 避免 lowering 阶段去猜隐藏控制流。
     */
    public sealed interface Statement permits EntryStmt, EndStmt, MarkerStmt, LinenoStmt, LabelStmt,
            GotoStmt, IfGotoStmt, AssignStmt, UpdateStmt, PushStmt, PopStmt, ParamStmt, CallStmt, InvokeStmt {
    }

    /**
     * 表示由 opcode {@code START} 发出的 bank 入口点。
     *
     * @param bank 底层 operand 中存放的 bank 编号
     */
    public record EntryStmt(int bank) implements Statement {
    }

    /**
     * 表示由 opcode {@code END} 发出的 bank 结束标记。
     *
     * @param operand 原始 operand；虽然当前样本通常是 {@code 0}，但仍需无损保留
     */
    public record EndStmt(int operand) implements Statement {
    }

    /**
     * 表示 opcode {@code MARKER}。
     *
     * <p>它表达的是脚本可见的进度/标记信息，而不是控制流结构。
     *
     * @param marker VM 写入的原始 marker 索引
     */
    public record MarkerStmt(int marker) implements Statement {
    }

    /**
     * 表示 opcode {@code LINENO}。
     *
     * @param lineNumber 从原始脚本保留下来的调试/源码行号
     */
    public record LinenoStmt(int lineNumber) implements Statement {
    }

    /**
     * 在 Structured IR 中声明一个跳转目标 label。
     *
     * @param name 根据指令索引派生出来的无损 synthetic label 名称
     */
    public record LabelStmt(String name) implements Statement {
    }

    /**
     * 无条件跳转语句。
     *
     * <p>这里显式保留 goto 风格控制流，而不提前恢复成更高级块结构。
     *
     * @param targetLabel 已解析的目标 label
     */
    public record GotoStmt(String targetLabel) implements Statement {
    }

    /**
     * 条件跳转语句。
     *
     * <p>这一层只保留 {@code if (expr) goto Lx} 形式，不在这里做 if/else 块恢复。
     *
     * @param condition 分支发生时 R0 中看到的条件表达式
     * @param targetLabel 条件满足时跳转到的目标 label
     */
    public record IfGotoStmt(Expression condition, String targetLabel) implements Statement {
    }

    /**
     * 赋值语句。
     *
     * <p>它既用于寄存器显式化，也用于变量写回 opcode 的结构化表达。
     *
     * @param target 赋值目标，可以是 VM 寄存器，也可以是变量槽位表达式
     * @param value 写入目标的无损表达式
     */
    public record AssignStmt(Target target, Expression value) implements Statement {
    }

    /**
     * 复合更新语句。
     *
     * <p>它对应底层的自增、自减和各类原地运算 opcode，
     * 不能简单坍缩成普通赋值，否则 lowering 时会失去原始 opcode 形状。
     *
     * @param target 更新目标，可以是固定变量槽位，也可以是动态槽位表达式
     * @param operator 更新类型
     * @param value 更新值；对 {@code INC/DEC} 这类无右值语句可为 {@code null}
     */
    public record UpdateStmt(Target target, UpdateOperator operator, Expression value) implements Statement {
    }

    /**
     * 显式栈压入语句。
     *
     * <p>它对应底层 {@code PUSH} opcode，用于保留那些没有被高层表达式吸收掉的栈操作。
     *
     * @param register 被压入算术栈的寄存器
     */
    public record PushStmt(RegisterId register) implements Statement {
    }

    /**
     * 显式栈弹出语句。
     *
     * <p>它对应底层 {@code POP} opcode，用于保留那些没有被高层表达式吸收掉的栈操作。
     *
     * @param register 从算术栈接收值的寄存器
     */
    public record PopStmt(RegisterId register) implements Statement {
    }

    /**
     * 显式参数入栈语句。
     *
     * <p>它对应底层 {@code PARAM} opcode，用于保留那些不能和最终 CALL 折叠成单条高层调用语句的场景。
     *
     * @param value 被压入参数栈的表达式
     */
    public record ParamStmt(Expression value) implements Statement {
    }

    /**
     * 调用语句。
     *
     * <p>用于表示“调用发生了，但其返回值没有继续并入当前语句”的情况。
     *
     * @param call 显式调用表达式，内部保留目标元数据和有序参数列表
     */
    public record CallStmt(CallExpr call) implements Statement {
    }

    /**
     * 显式调用落点语句。
     *
     * <p>当参数准备过程被 `LINENO/MARKER` 等元语义指令打断时，CALL 不能再和参数折叠成
     * 一个高层 `call(...)` 表达式，这时就用它来保留底层 CALL 本体。
     *
     * @param target 调用目标元数据
     * @param argc CALL operand 中记录的参数个数
     */
    public record InvokeStmt(CallTarget target, int argc) implements Statement {
    }

    /**
     * 所有赋值目标节点的基接口。
     */
    public sealed interface Target permits RegisterTarget, VariableTarget {
    }

    /**
     * 复合更新语句的运算类型。
     */
    public enum UpdateOperator {
        INC,
        DEC,
        ADD_ASSIGN,
        SUB_ASSIGN,
        MUL_ASSIGN,
        DIV_ASSIGN,
        MOD_ASSIGN,
        OR_ASSIGN,
        AND_ASSIGN,
        XOR_ASSIGN,
        SHR_ASSIGN,
        SHL_ASSIGN
    }

    /**
     * 当前可逆子集中实际需要显式建模的 VM 寄存器。
     */
    public enum RegisterId {
        R0,
        R1
    }

    /**
     * 具体 VM 寄存器的赋值目标。
     *
     * @param register 必须显式 materialize 的底层寄存器
     */
    public record RegisterTarget(RegisterId register) implements Target {
    }

    /**
     * 脚本变量槽位的赋值目标。
     *
     * @param index 运行时解析出变量槽位编号的表达式
     */
    public record VariableTarget(Expression index) implements Target {
    }

    /**
     * Structured IR 中所有表达式节点的基接口。
     *
     * <p>这里的表达式仍然保持 VM 风格：寄存器、变量读取、调用结果都直接建模，
     * 不用高层语言 facade 去遮蔽它们。
     */
    public sealed interface Expression permits ConstExpr, RegisterExpr, VarExpr, StringRefExpr, CallExpr,
            ToStringExpr, UnaryExpr, BinaryExpr {
    }

    /**
     * 直接来自 {@code VAL} operand 的整型立即数。
     *
     * @param value 指令流里保存的有符号 32 位立即数
     */
    public record ConstExpr(int value) implements Expression {
    }

    /**
     * VM 寄存器读取表达式。
     *
     * @param register 当前表达式引用的是哪个寄存器的值
     */
    public record RegisterExpr(RegisterId register) implements Expression {
    }

    /**
     * 由 opcode {@code LOAD} 产生的变量读取表达式。
     *
     * @param index VM 用来解析变量槽位的索引表达式
     */
    public record VarExpr(Expression index) implements Expression {
    }

    /**
     * 区分 {@code PARAM 1} 保留下来的几种字符串访问模式。
     */
    public enum StringRefKind {
        STR,
        STR2,
        RAW
    }

    /**
     * 从字符串参数保留下来的字符串表引用表达式。
     *
     * @param kind VM 对这个字符串引用采用的是哪种底层来源
     * @param index 字符串索引表达式，或原始引用表达式
     */
    public record StringRefExpr(StringRefKind kind, Expression index) implements Expression {
    }

    /**
     * 区分调用目标的语义来源。
     */
    public enum CallKind {
        NATIVE,
        SCRIPT,
        RAW
    }

    /**
     * 调用目标元数据。
     *
     * <p>它会跨 render / parse / lower 阶段持续携带，避免调用目标信息在中途丢失。
     *
     * @param rawId CALL operand 中存放的底层函数 id
     * @param mnemonic 来自 {@link com.giga.nexas.dto.bsdx.bin.consts.Operand} 的可选符号名
     * @param kind 当前管线对该调用目标的理解类型
     */
    public record CallTarget(int rawId, String mnemonic, CallKind kind) {
    }

    /**
     * 函数调用表达式。
     *
     * <p>参数顺序必须和 VM 看到的一致，否则 lowering 时就无法恢复原始 CALL 语义。
     *
     * @param target CALL 的目标元数据
     * @param arguments 参数栈重建后的有序参数列表
     */
    public record CallExpr(CallTarget target, List<Expression> arguments) implements Expression {
        /**
         * 规范化参数列表，同时保持原参数顺序不变。
         */
        public CallExpr {
            arguments = arguments == null ? List.of() : List.copyOf(arguments);
        }
    }

    /**
     * 数值转临时字符串引用表达式。
     *
     * <p>它对应底层 {@code TO_STRING} opcode，用于把当前数值表达式转成临时字符串变量引用。
     *
     * @param value 被转换成字符串的数值表达式
     */
    public record ToStringExpr(Expression value) implements Expression {
    }

    /**
     * 当前可在不丢失 VM 语义前提下表达的单目运算符集合。
     */
    public enum UnaryOperator {
        NEGATE,
        LOGICAL_NOT,
        BITWISE_NOT
    }

    /**
     * 单目表达式节点。
     *
     * @param operator VM 级别的单目运算
     * @param operand 被该运算消费的操作数表达式
     */
    public record UnaryExpr(UnaryOperator operator, Expression operand) implements Expression {
    }

    /**
     * 当前 lossless 子集支持的双目运算符集合。
     */
    public enum BinaryOperator {
        ADD,
        SUBTRACT,
        MULTIPLY,
        DIVIDE,
        MODULO,
        LOGICAL_OR,
        LOGICAL_AND,
        BIT_OR,
        BIT_AND,
        BIT_XOR,
        LESS_EQUAL,
        GREATER_EQUAL,
        LESS_THAN,
        GREATER_THAN,
        EQUAL,
        NOT_EQUAL,
        SHIFT_RIGHT,
        SHIFT_LEFT,
        CONCAT
    }

    /**
     * 双目表达式节点。
     *
     * <p>这里必须严格保留 VM 需要的操作数顺序，不能随意交换左右侧。
     *
     * @param operator VM 级别的双目运算
     * @param left 逻辑左操作数，通常来自执行时的 R1
     * @param right 逻辑右操作数，通常来自执行时的 R0
     */
    public record BinaryExpr(BinaryOperator operator, Expression left, Expression right) implements Expression {
    }
}
