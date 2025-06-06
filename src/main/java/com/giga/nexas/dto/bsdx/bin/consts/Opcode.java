package com.giga.nexas.dto.bsdx.bin.consts;

/**
 * @description 定义了指令集中的所有操作码（Opcode），
 * 每个操作码代表一种基本的虚拟机操作行为。
 * 本常量中除了本注释，其余文档注释均生成自gemini，并由本人逐一校对并修改，
 * 其校对来源来自下方的source
 * @source https://github.com/koukdw/Aquarium_tools/blob/main/research/opcodes.md
 */
public enum Opcode {

    /**
     * <b>指令助记符: Val</b>
     * <br>
     * <b>操作码: 0 (0x0)</b>
     * <p>
     * <b>功能:</b> 将一个立即数赋给通用寄存器 R0。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Value} (u32)</li>
     *   <li><b>含义:</b> 代表要赋给寄存器 R0 的立即数。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = [Operand Value]}</pre>
     * <p>
     * <b>说明:</b>
     * 此指令用于将操作数 {@code Value} (一个立即数) 加载到虚拟机的核心寄存器 R0 中。
     * 它是为后续计算或操作设置初始值或常量的基础指令。虽然指令直接操作 R0，
     * 但 R0 中的值后续可配合 {@link #STORE} 等指令存入内存变量。
     */
    VAL(0),

    /**
     * <b>指令助记符: Push</b>
     * <br>
     * <b>操作码: 4 (0x4)</b>
     * <p>
     * <b>功能:</b> 将指定寄存器的值压入算术栈。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Register Number} (u32)</li>
     *   <li><b>含义:</b> 要将其值压栈的寄存器的编号。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code stack.push(registers[Register Number])}</pre>
     * <p>
     * <b>说明:</b>
     * 将位于 {@code registers[Register Number]} 的值压入虚拟机内部的算术栈顶。
     * 常用于在复杂计算前保存中间值，或为某些需要栈操作数的指令准备数据。
     */
    PUSH(4),

    /**
     * <b>指令助记符: Param</b>
     * <br>
     * <b>操作码: 5 (0x5)</b>
     * <p>
     * <b>功能:</b> 将寄存器 R0 的值作为参数添加到函数调用的参数数组中。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Type} (u32)</li>
     *   <li><b>含义:</b> 参数的类型。0 代表数值 (R0 包含该数值)；1 代表字符串 (R0 包含字符串数组或字符串变量数组的索引)。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code argument_array.add(R0, Type)}</pre>
     * <p>
     * <b>说明:</b>
     * 此指令用于为即将发生的函数调用（通过 {@link #CALL} 指令）准备参数。
     * 它从寄存器 R0 中获取值，并根据操作数 {@code Type} 解释其为数值或字符串索引，然后将其添加到内部的参数列表中。
     * {@code Param_min} 和 {@code Param_max} 指令（如果存在）可在此指令后使用，以标记参数进行边界检查。
     */
    PARAM(5),

    /**
     * <b>指令助记符: Pop</b>
     * <br>
     * <b>操作码: 6 (0x6)</b>
     * <p>
     * <b>功能:</b> 从算术栈顶弹出一个值到指定的寄存器。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Register Number} (u32)</li>
     *   <li><b>含义:</b> 用于接收栈顶值的寄存器的编号。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code registers[Register Number] = stack.pop()}</pre>
     * <p>
     * <b>说明:</b>
     * 从虚拟机内部的算术栈顶取出一个值，并将其存入 {@code registers[Register Number]}。
     * 通常与 {@link #PUSH} 配对使用，用于恢复之前保存的值或获取计算结果。
     */
    POP(6),

    /**
     * <b>指令助记符: Call</b>
     * <br>
     * <b>操作码: 7 (0x7)</b>
     * <p>
     * <b>功能:</b> 调用一个原生函数或脚本函数。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code FunctionID_Arity} (u32)</li>
     *   <li><b>含义:</b> 一个32位整数，其高16位代表函数ID ({@code FunctionID})，低16位代表参数个数 ({@code Arity})。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = execute_function(FunctionID, Arity, argument_array)}</pre>
     * <p>
     * <b>说明:</b>
     * 执行函数调用。操作数被拆分为 {@code FunctionID} 和 {@code Arity}。
     * 如果 {@code FunctionID} 的符号位未设置（值为正数或零），则调用引擎实现的原生函数。
     * 如果 {@code FunctionID} 的符号位已设置（值为负数），则调用在 .bin 文件中实现的脚本函数。
     * 函数的参数通过之前的 {@link #PARAM} 指令准备。如果函数有返回值，它将被置于寄存器 R0 中。
     */
    CALL(7),

    /**
     * <b>指令助记符: Load</b>
     * <br>
     * <b>操作码: 8 (0x8)</b>
     * <p>
     * <b>功能:</b> 从内存加载变量的值到寄存器。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Register Number} (u32)</li>
     *   <li><b>含义:</b> 一个寄存器的编号。该寄存器既作为目标寄存器，也包含要加载的变量的索引（可能带有作用域标志）。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code registers[Register Number] = GET_INT_VARIABLE(registers[Register Number])} (或 GET_STRING_VARIABLE)</pre>
     * <p>
     * <b>说明:</b>
     * 将位于 {@code registers[Register Number]} 所指向的变量的值（整数或字符串索引）加载回 {@code registers[Register Number]}。
     * 寄存器 {@code Register Number} 初始时包含一个变量数组的索引，该索引可能组合了一个可选标志以确定作用域（例如全局变量）。
     */
    LOAD(8),

    /**
     * <b>指令助记符: Add</b>
     * <br>
     * <b>操作码: 9 (0x9)</b>
     * <p>
     * <b>功能:</b> 执行加法或字符串连接操作。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Type} (u32)</li>
     *   <li><b>含义:</b> 操作类型。0 代表数值加法；1 代表字符串连接。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * if (Type == 0) {
     *     R0 = R1 + R0;
     * } else if (Type == 1) {
     *     R0 = CREATE_TEMP_STRING_VARIABLE(concat(GET_STRING(R1), GET_STRING(R0)));
     * }
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 如果 {@code Type} 为 0（数值），则执行 {@code R0 = R1 + R0}。
     * 如果 {@code Type} 为 1（字符串），则 R0 和 R1 均包含字符串数组或变量的索引。
     * 它会获取 R1 和 R0 指向的字符串，将它们连接起来，创建一个新的临时字符串变量来存储结果，并将该临时字符串变量的索引存入 R0。
     * (注意：操作数顺序 R1 + R0 基于 opcodes.md 的描述，可能与某些引擎的 eax = ebx + eax 习惯不同)
     */
    ADD(9),

    /**
     * <b>指令助记符: Sub</b>
     * <br>
     * <b>操作码: 10 (0xA)</b>
     * <p>
     * <b>功能:</b> 执行减法运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R1 - R0}</pre>
     * <p>
     * <b>说明:</b>
     * 从寄存器 R1 的值中减去寄存器 R0 的值，并将结果存回 R0。
     */
    SUB(10),

    /**
     * <b>指令助记符: Mul</b>
     * <br>
     * <b>操作码: 11 (0xB)</b>
     * <p>
     * <b>功能:</b> 执行乘法运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R1 * R0}</pre>
     * <p>
     * <b>说明:</b>
     * 将寄存器 R1 的值与寄存器 R0 的值相乘，并将结果存回 R0。
     */
    MUL(11),

    /**
     * <b>指令助记符: Div</b>
     * <br>
     * <b>操作码: 12 (0xC)</b>
     * <p>
     * <b>功能:</b> 执行除法运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R1 / R0}</pre>
     * <p>
     * <b>说明:</b>
     * 将寄存器 R1 的值除以寄存器 R0 的值，并将商存回 R0。需注意除零错误。
     */
    DIV(12),

    /**
     * <b>指令助记符: Mod</b>
     * <br>
     * <b>操作码: 13 (0xD)</b>
     * <p>
     * <b>功能:</b> 执行取模运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R1 % R0}</pre>
     * <p>
     * <b>说明:</b>
     * 计算寄存器 R1 的值除以寄存器 R0 的值的余数，并将结果存回 R0。需注意除零错误。
     */
    MOD(13),

    /**
     * <b>指令助记符: Store</b>
     * <br>
     * <b>操作码: 14 (0xE)</b>
     * <p>
     * <b>功能:</b> 将寄存器的值写入内存中的变量。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 目标变量的索引。如果为 -1，则目标变量索引在 R0 中，要存储的值在 R1 中；否则，此操作数即为目标变量索引，要存储的值在 R0 中。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * if (Operand_Index == -1) {
     *     SET_VARIABLE(R0, R1); // R0 is index, R1 is value
     * } else {
     *     SET_VARIABLE(Operand_Index, R0); // Operand_Index is index, R0 is value
     * }
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 将一个值（整数或字符串索引）存储到变量中。
     * 如果操作数 {@code Index} 为 -1，则 R0 包含目标变量的索引，R1 包含要存储的值（或字符串索引）。
     * 否则，操作数 {@code Index} 本身就是目标变量的索引，R0 包含要存储的值（或字符串索引）。
     * 如果索引为负数（在清除符号位后），表示操作字符串变量。
     */
    STORE(14),

    /**
     * <b>指令助记符: Or</b>
     * <br>
     * <b>操作码: 15 (0xF)</b>
     * <p>
     * <b>功能:</b> 执行逻辑或 (短路或) 运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R0 || R1} (布尔逻辑)</pre>
     * <p>
     * <b>说明:</b>
     * 对寄存器 R0 和 R1 的值执行逻辑或运算，并将结果（通常为 0 或 1）存回 R0。
     * 这是一个短路操作。
     */
    OR(15),

    /**
     * <b>指令助记符: And</b>
     * <br>
     * <b>操作码: 16 (0x10)</b>
     * <p>
     * <b>功能:</b> 执行逻辑与 (短路与) 运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R0 && R1} (布尔逻辑)</pre>
     * <p>
     * <b>说明:</b>
     * 对寄存器 R0 和 R1 的值执行逻辑与运算，并将结果（通常为 0 或 1）存回 R0。
     * 这是一个短路操作。
     */
    AND(16),

    /**
     * <b>指令助记符: Bit_Or</b>
     * <br>
     * <b>操作码: 17 (0x11)</b>
     * <p>
     * <b>功能:</b> 执行按位或运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R0 | R1}</pre>
     * <p>
     * <b>说明:</b>
     * 对寄存器 R0 和 R1 的值执行按位或运算，并将结果存回 R0。
     */
    BIT_OR(17),

    /**
     * <b>指令助记符: Bit_And</b>
     * <br>
     * <b>操作码: 18 (0x12)</b>
     * <p>
     * <b>功能:</b> 执行按位与运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R0 & R1}</pre>
     * <p>
     * <b>说明:</b>
     * 对寄存器 R0 和 R1 的值执行按位与运算，并将结果存回 R0。
     */
    BIT_AND(18),

    /**
     * <b>指令助记符: Xor</b>
     * <br>
     * <b>操作码: 19 (0x13)</b>
     * <p>
     * <b>功能:</b> 执行按位异或运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R0 ^ R1}</pre>
     * <p>
     * <b>说明:</b>
     * 对寄存器 R0 和 R1 的值执行按位异或运算，并将结果存回 R0。
     */
    XOR(19),

    /**
     * <b>指令助记符: Not</b>
     * <br>
     * <b>操作码: 20 (0x14)</b>
     * <p>
     * <b>功能:</b> 执行逻辑非运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = !R0} (布尔逻辑)</pre>
     * <p>
     * <b>说明:</b>
     * 对寄存器 R0 的值执行逻辑非运算（将其视为布尔值），并将结果（通常为 0 或 1）存回 R0。
     * (注意：opcodes.md 指明为逻辑非 `!R0`，而非按位非 `~R0`)
     */
    NOT(20),

    /**
     * <b>指令助记符: Cmp_Le</b>
     * <br>
     * <b>操作码: 21 (0x15)</b>
     * <p>
     * <b>功能:</b> 比较 R0 是否小于等于 R1。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = (R0 <= R1) ? 1 : 0}</pre>
     * <p>
     * <b>说明:</b>
     * 比较寄存器 R0 的值是否小于或等于 R1 的值。如果条件为真，则 R0 置为 1；否则置为 0。
     */
    CMP_LE(21),

    /**
     * <b>指令助记符: Cmp_Ge</b>
     * <br>
     * <b>操作码: 22 (0x16)</b>
     * <p>
     * <b>功能:</b> 比较 R0 是否大于等于 R1。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = (R0 >= R1) ? 1 : 0}</pre>
     * <p>
     * <b>说明:</b>
     * 比较寄存器 R0 的值是否大于或等于 R1 的值。如果条件为真，则 R0 置为 1；否则置为 0。
     */
    CMP_GE(22),

    /**
     * <b>指令助记符: Cmp_Lt</b>
     * <br>
     * <b>操作码: 23 (0x17)</b>
     * <p>
     * <b>功能:</b> 比较 R0 是否小于 R1。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = (R0 < R1) ? 1 : 0}</pre>
     * <p>
     * <b>说明:</b>
     * 比较寄存器 R0 的值是否小于 R1 的值。如果条件为真，则 R0 置为 1；否则置为 0。
     */
    CMP_LT(23),

    /**
     * <b>指令助记符: Cmp_Gt</b>
     * <br>
     * <b>操作码: 24 (0x18)</b>
     * <p>
     * <b>功能:</b> 比较 R0 是否大于 R1。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = (R0 > R1) ? 1 : 0}</pre>
     * <p>
     * <b>说明:</b>
     * 比较寄存器 R0 的值是否大于 R1 的值。如果条件为真，则 R0 置为 1；否则置为 0。
     */
    CMP_GT(24),

    /**
     * <b>指令助记符: Cmp_Eq</b>
     * <br>
     * <b>操作码: 25 (0x19)</b>
     * <p>
     * <b>功能:</b> 比较 R0 和 R1 是否相等。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Type} (u32)</li>
     *   <li><b>含义:</b> 操作类型。0 代表数值比较；1 代表字符串比较。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * if (Type == 0) {
     *     R0 = (R0 == R1) ? 1 : 0;
     * } else if (Type == 1) {
     *     R0 = COMPARE_STRINGS_EQUAL(R1, R0) ? 1 : 0;
     * }
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 比较寄存器 R0 和 R1 的值是否相等。如果条件为真，则 R0 置为 1；否则置为 0。
     * 操作数 {@code Type} 决定比较方式：
     * 如果为 0（数值），则直接比较 R0 和 R1 的数值。
     * 如果为 1（字符串），则 R0 和 R1 被视为字符串索引，比较它们指向的字符串内容是否相等 (opcodes.md 暗示 R1, R0 的顺序)。
     */
    CMP_EQ(25),

    /**
     * <b>指令助记符: Cmp_Ne</b>
     * <br>
     * <b>操作码: 26 (0x1A)</b>
     * <p>
     * <b>功能:</b> 比较 R0 和 R1 是否不相等。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Type} (u32)</li>
     *   <li><b>含义:</b> 操作类型。0 代表数值比较；1 代表字符串比较。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * if (Type == 0) {
     *     R0 = (R0 != R1) ? 1 : 0;
     * } else if (Type == 1) {
     *     R0 = COMPARE_STRINGS_NOTEQUAL(R1, R0) ? 1 : 0;
     * }
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 比较寄存器 R0 和 R1 的值是否不相等。如果条件为真，则 R0 置为 1；否则置为 0。
     * 操作数 {@code Type} 决定比较方式，同 {@link #CMP_EQ}。
     */
    CMP_NE(26),

    /**
     * <b>指令助记符: Start</b>
     * <br>
     * <b>操作码: 27 (0x1B)</b>
     * <p>
     * <b>功能:</b> 标记脚本的入口点或代码银行 (bank) 的开始。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code BankNumber} (u32)</li>
     *   <li><b>含义:</b> 代码银行的编号，可用于查找银行参数数组。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code current_bank_context = setup_bank(BankNumber)} (概念性)</pre>
     * <p>
     * <b>说明:</b>
     * 标记脚本中一个逻辑块（通常称为 "bank" 或入口点）的开始。
     * {@code BankNumber} 操作数可以被一些函数（如 LoadScript, ChangeBank, CallBank）用来确定加载或调用的具体代码段及其相关参数。
     */
    START(27),

    /**
     * <b>指令助记符: End</b>
     * <br>
     * <b>操作码: 28 (0x1C)</b>
     * <p>
     * <b>功能:</b> 标记当前代码银行 (bank) 的结束。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code cleanup_current_bank_context()} (概念性)</pre>
     * <p>
     * <b>说明:</b>
     * 标记由 {@link #START} 指令开始的当前代码银行的结束。
     */
    END(28),

    /**
     * <b>指令助记符: Marker</b>
     * <br>
     * <b>操作码: 29 (0x1D)</b>
     * <p>
     * <b>功能:</b> 设置一个脚本执行进度标记。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Value} (u32)</li>
     *   <li><b>含义:</b> 标记的索引值。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code read_markers_array[Value] = 1}</pre>
     * <p>
     * <b>说明:</b>
     * 通常用于主脚本中，以跟踪玩家已阅读或经历过的剧情节点。
     * 当此指令执行时，它会使用操作数 {@code Value} 作为索引，在一个字节数组中将对应位置的值从 0 设置为 1。
     * 这使得游戏在快速跳过（fast skipping）时可以停在未读的标记处。已读标记的状态通常保存在存档中。
     */
    MARKER(29),

    /**
     * <b>指令助记符: Inc</b>
     * <br>
     * <b>操作码: 34 (0x22)</b>
     * <p>
     * <b>功能:</b> 将指定变量的值加 1。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 变量的索引。如果为 -1，则变量索引在 R0 中；否则，此操作数即为变量索引。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * if (Operand_Index == -1) {
     *     GET_VARIABLE(R0)++;
     * } else {
     *     GET_VARIABLE(Operand_Index)++;
     * }
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 将指定索引处的变量值增加 1。
     * 如果操作数 {@code Index} 为 -1，则 R0 包含要自增的变量的索引。
     * 否则，操作数 {@code Index} 本身就是要自增的变量的索引。
     */
    INC(34),

    /**
     * <b>指令助记符: Dec</b>
     * <br>
     * <b>操作码: 35 (0x23)</b>
     * <p>
     * <b>功能:</b> 将指定变量的值减 1。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 变量的索引。如果为 -1，则变量索引在 R0 中；否则，此操作数即为变量索引。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * if (Operand_Index == -1) {
     *     GET_VARIABLE(R0)--;
     * } else {
     *     GET_VARIABLE(Operand_Index)--;
     * }
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 将指定索引处的变量值减少 1。
     * 如果操作数 {@code Index} 为 -1，则 R0 包含要自减的变量的索引。
     * 否则，操作数 {@code Index} 本身就是要自减的变量的索引。
     */
    DEC(35),

    /**
     * <b>指令助记符: Cmp_Zero</b>
     * <br>
     * <b>操作码: 43 (0x2B)</b>
     * <p>
     * <b>功能:</b> 判断寄存器 R0 的值是否为 0。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = (R0 == 0) ? 1 : 0}</pre>
     * <p>
     * <b>说明:</b>
     * 比较寄存器 R0 的值是否等于 0。如果为真，则 R0 置为 1；否则置为 0。
     */
    CMP_ZERO(43),

    /**
     * <b>指令助记符: Lineno</b>
     * <br>
     * <b>操作码: 44 (0x2C)</b>
     * <p>
     * <b>功能:</b> 设置当前执行语句对应的源码行号（用于调试）。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Value} (u32)</li>
     *   <li><b>含义:</b> 源码的行号。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code current_debug_line_number = Value}</pre>
     * <p>
     * <b>说明:</b>
     * 设置当前虚拟机正在执行的指令所对应的原始脚本文件中的行号。
     * 此信息主要用于调试目的，通常只在主脚本中使用。当脚本函数被内联时，相同的行号可能会重复出现。
     */
    LINENO(44),

    /**
     * <b>指令助记符: Sar</b>
     * <br>
     * <b>操作码: 45 (0x2D)</b>
     * <p>
     * <b>功能:</b> 执行算术右移运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R1 >> R0} (算术右移)</pre>
     * <p>
     * <b>说明:</b>
     * 将寄存器 R1 的值进行算术右移，移动的位数由寄存器 R0 的值指定，结果存回 R0。
     * 算术右移会保留符号位。
     */
    SAR(45),

    /**
     * <b>指令助记符: Shl</b>
     * <br>
     * <b>操作码: 46 (0x2E)</b>
     * <p>
     * <b>功能:</b> 执行逻辑左移运算。
     * <p>
     * <b>操作数 (Operand):</b> 无显式操作数。
     * <p>
     * <b>行为:</b>
     * <pre>{@code R0 = R1 << R0} (逻辑左移)</pre>
     * <p>
     * <b>说明:</b>
     * 将寄存器 R1 的值进行逻辑左移，移动的位数由寄存器 R0 的值指定，结果存回 R0。
     * (opcodes.md 指明为逻辑左移，对于左移，逻辑移位和算术移位通常结果一致，除非特殊处理溢出到符号位的情况)
     */
    SHL(46),

    /**
     * <b>指令助记符: Ld_add</b>
     * <br>
     * <b>操作码: 47 (MD: 0x30)</b>
     * <p>
     * <b>功能:</b> 将值加到/附加到指定变量，并将结果存回变量 (复合赋值 +=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 目标变量的索引。如果为 -1，则目标变量索引在 R0 中，要加的值在 R1 中；否则，此操作数即为目标变量索引，要加的值在 R0 中。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_add;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_add = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_add = R0;
     * }
     *
     * if (IS_NUMERIC_VAR(target_var_idx)) {
     *     GET_VARIABLE(target_var_idx) += val_to_add;
     * } else if (IS_STRING_VAR(target_var_idx)) {
     *     APPEND_STRING(target_var_idx, GET_STRING(val_to_add));
     * }
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 类似于 {@link #STORE} 的寻址方式。将一个值加到（数值变量）或附加到（字符串变量）位于指定索引的变量上。
     * 如果变量是数值类型，执行 {@code var += value}。
     * 如果变量是字符串类型，执行字符串追加操作。
     */
    LD_ADD(47),

    /**
     * <b>指令助记符: Ld_sub</b>
     * <br>
     * <b>操作码: 48 (MD: 0x31)</b>
     * <p>
     * <b>功能:</b> 从指定变量中减去一个值，并将结果存回变量 (复合赋值 -=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_sub;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_sub = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_sub = R0;
     * }
     * // Assuming numeric operation for SUB
     * GET_VARIABLE(target_var_idx) -= val_to_sub;
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。从指定索引的数值变量中减去一个值。
     */
    LD_SUB(48),

    /**
     * <b>指令助记符: Ld_mul</b>
     * <br>
     * <b>操作码: 49 (MD: 0x32)</b>
     * <p>
     * <b>功能:</b> 将指定变量乘以一个值，并将结果存回变量 (复合赋值 *=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_mul;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_mul = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_mul = R0;
     * }
     * GET_VARIABLE(target_var_idx) *= val_to_mul;
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量乘以一个值。
     */
    LD_MUL(49),

    /**
     * <b>指令助记符: Ld_div</b>
     * <br>
     * <b>操作码: 50 (MD: 0x33)</b>
     * <p>
     * <b>功能:</b> 将指定变量除以一个值，并将结果存回变量 (复合赋值 /=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_div;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_div = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_div = R0;
     * }
     * // Add check for division by zero if necessary
     * GET_VARIABLE(target_var_idx) /= val_to_div;
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量除以一个值。需注意除零错误。
     */
    LD_DIV(50),

    /**
     * <b>指令助记符: Ld_mod</b>
     * <br>
     * <b>操作码: 51 (MD: 0x34)</b>
     * <p>
     * <b>功能:</b> 将指定变量对一个值取模，并将结果存回变量 (复合赋值 %=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_mod;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_mod = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_mod = R0;
     * }
     * // Add check for division by zero if necessary
     * GET_VARIABLE(target_var_idx) %= val_to_mod;
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量对一个值取模。需注意除零错误。
     */
    LD_MOD(51),

    /**
     * <b>指令助记符: Ld_or</b>
     * <br>
     * <b>操作码: 52 (MD: 0x35)</b>
     * <p>
     * <b>功能:</b> 将指定变量与一个值进行按位或，并将结果存回变量 (复合赋值 |=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_or;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_or = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_or = R0;
     * }
     * GET_VARIABLE(target_var_idx) |= val_to_or;
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量与一个值进行按位或运算。
     */
    LD_OR(52),

    /**
     * <b>指令助记符: Ld_and</b>
     * <br>
     * <b>操作码: 53 (MD: 0x36)</b>
     * <p>
     * <b>功能:</b> 将指定变量与一个值进行按位与，并将结果存回变量 (复合赋值 &=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_and;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_and = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_and = R0;
     * }
     * GET_VARIABLE(target_var_idx) &= val_to_and;
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量与一个值进行按位与运算。
     */
    LD_AND(53),

    /**
     * <b>指令助记符: Ld_xor</b>
     * <br>
     * <b>操作码: 54 (MD: 0x37)</b>
     * <p>
     * <b>功能:</b> 将指定变量与一个值进行按位异或，并将结果存回变量 (复合赋值 ^=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * val_to_xor;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     val_to_xor = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     val_to_xor = R0;
     * }
     * GET_VARIABLE(target_var_idx) ^= val_to_xor;
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量与一个值进行按位异或运算。
     */
    LD_XOR(54),

    /**
     * <b>指令助记符: Ld_sar</b>
     * <br>
     * <b>操作码: 55 (MD: 0x38)</b>
     * <p>
     * <b>功能:</b> 将指定变量算术右移一个值指定的位数，并将结果存回变量 (复合赋值 >>=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。要移位的位数由 R0 或 R1 提供 (取决于寻址方式)。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * shift_amount;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     shift_amount = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     shift_amount = R0;
     * }
     * GET_VARIABLE(target_var_idx) >>= shift_amount; // 算术右移
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量算术右移指定位数。
     */
    LD_SAR(55),

    /**
     * <b>指令助记符: Ld_shl</b>
     * <br>
     * <b>操作码: 56 (MD: 0x39)</b>
     * <p>
     * <b>功能:</b> 将指定变量逻辑左移一个值指定的位数，并将结果存回变量 (复合赋值 <<=)。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 同 {@link #LD_ADD}。要移位的位数由 R0 或 R1 提供 (取决于寻址方式)。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * shift_amount;
     * target_var_idx;
     * if (Operand_Index == -1) {
     *     target_var_idx = R0;
     *     shift_amount = R1;
     * } else {
     *     target_var_idx = Operand_Index;
     *     shift_amount = R0;
     * }
     * GET_VARIABLE(target_var_idx) <<= shift_amount; // 逻辑左移
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 寻址方式同 {@link #LD_ADD}。将指定索引的数值变量逻辑左移指定位数。
     */
    LD_SHL(56),

    /**
     * <b>指令助记符: To_String</b>
     * <br>
     * <b>操作码: 61 (MD: 0x3F)</b>
     * <p>
     * <b>功能:</b> 将指定寄存器中的整数值转换为字符串，并将新字符串变量的索引存回该寄存器。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Register Number} (u32)</li>
     *   <li><b>含义:</b> 包含要转换的整数的寄存器编号，转换后也用于存储结果字符串变量的索引。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code
     * string_value = CONVERT_INT_TO_STRING(registers[Register Number]);
     * registers[Register Number] = CREATE_TEMP_STRING_VARIABLE(string_value);
     * }</pre>
     * <p>
     * <b>说明:</b>
     * 将 {@code registers[Register Number]} 中的整数转换为其字符串表示形式。
     * 然后，会创建一个临时的字符串变量来存储这个新字符串，并将这个临时字符串变量的索引存回 {@code registers[Register Number]}。
     */
    TO_STRING(61),

    /**
     * <b>指令助记符: Return</b>
     * <br>
     * <b>操作码: 62 (MD: 0x40)</b>
     * <p>
     * <b>功能:</b> 从当前函数返回，可选地带一个返回值。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Type} (u32)</li>
     *   <li><b>含义:</b> 返回值的类型。0 代表数值；1 代表字符串。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code (Pop call stack frame); return_value = R0;} (根据Type处理R0)</pre>
     * <p>
     * <b>说明:</b>
     * 结束当前函数的执行并返回到调用者。返回值位于寄存器 R0 中。
     * 操作数 {@code Type} 指示返回值的类型：
     * 如果为 0（数值），则 R0 中的值直接作为数值返回。
     * 如果为 1（字符串），则 R0 包含一个字符串变量的索引，该字符串变量的内容是实际的返回字符串（可能是一个临时创建的字符串变量）。
     */
    RETURN(62),

    /**
     * <b>指令助记符: Jmp</b>
     * <br>
     * <b>操作码: 63 (MD: 0x41)</b>
     * <p>
     * <b>功能:</b> 无条件跳转到指定的指令地址。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 要跳转到的目标指令在代码区中的索引。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code ProgramCounter = Index}</pre>
     * <p>
     * <b>说明:</b>
     * 无条件地将程序计数器（ProgramCounter）设置为操作数 {@code Index} 指定的值，从而改变程序的执行流程。
     * (注意: opcodes.md 提到所有指令（包括跳转）都会使PC自增1，这意味着实际存储在操作数中的跳转目标可能需要考虑这一点。此处的行为描述的是逻辑上的跳转目标。)
     */
    JMP(63),

    /**
     * <b>指令助记符: Jmp_if_false</b>
     * <br>
     * <b>操作码: 64 (MD: 0x42)</b>
     * <p>
     * <b>功能:</b> 如果寄存器 R0 的值为 false (0)，则跳转到指定的指令地址。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 如果条件满足，要跳转到的目标指令在代码区中的索引。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code if (R0 == 0) ProgramCounter = Index;}</pre>
     * <p>
     * <b>说明:</b>
     *检查寄存器 R0 的值。如果 R0 等于 0（通常代表布尔值 false），则将程序计数器设置为操作数 {@code Index} 指定的值。
     * 否则，程序继续顺序执行下一条指令。(跳转行为说明同 {@link #JMP})
     */
    JMP_IF_FALSE(64),

    /**
     * <b>指令助记符: Jmp_if_true</b>
     * <br>
     * <b>操作码: 65 (MD: 0x43)</b>
     * <p>
     * <b>功能:</b> 如果寄存器 R0 的值为 true (非0)，则跳转到指定的指令地址。
     * <p>
     * <b>操作数 (Operand):</b>
     * <ul>
     *   <li><b>名称:</b> {@code Index} (u32)</li>
     *   <li><b>含义:</b> 如果条件满足，要跳转到的目标指令在代码区中的索引。</li>
     * </ul>
     * <p>
     * <b>行为:</b>
     * <pre>{@code if (R0 != 0) ProgramCounter = Index;}</pre>
     * <p>
     * <b>说明:</b>
     * 检查寄存器 R0 的值。如果 R0 不等于 0（通常代表布尔值 true，具体可能是1），则将程序计数器设置为操作数 {@code Index} 指定的值。
     * 否则，程序继续顺序执行下一条指令。(跳转行为说明同 {@link #JMP})
     */
    JMP_IF_TRUE(65);

    public final int code;

    Opcode(int code) {
        this.code = code;
    }
}