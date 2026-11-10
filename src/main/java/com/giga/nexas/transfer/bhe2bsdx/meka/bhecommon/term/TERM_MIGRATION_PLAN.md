# BHE term 语义迁移计划

本文档对应 `com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term` 包，固定 BHE -> BSDX 的 `InfoCollection / term` 迁移策略。

## 对应代码

- MEK/AI 审计：`src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/MekInfoCollectionTermAuditTest.java`
- WAZ/MEK 迁移矩阵审计：`src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/TermMigrationMatrixAuditTest.java`
- 公共弹幕接入点：`CrossRedirectBheCommonProjectileResourcesStep.redirect(...)`
- 审计输出目录：`src/main/resources/out/bhe2bsdx/term-audit/`

## 职责边界

本包只处理 `InfoCollection / term` 的语义转换。

- 负责：把 BHE term 语义重编译成 BSDX term 语义。
- 负责：覆盖公共弹幕 WAZ、单机体 WAZ、MEK AI 中出现的 `InfoCollection`。
- 负责：对 BSDX 缺失的 BHE 语义给出显式重写或显式 fallback。
- 不负责：WAZ / SPM / SE / BatVoice 的资源索引重定向。
- 不负责：公共资源 append、静态资源输出、mek/waz/spm 文件格式转换。

## 已审计事实

MEK/AI 审计结果：

- BHE MEK 覆盖 `95` 个文件，`2536` 条 `InfoCollection`。
- BSDX MEK 覆盖 `104` 个文件，`2724` 条 `InfoCollection`。
- 两侧 MEK/AI 的唯一语义路径均为 `POS/OBJECT`。
- 两侧 MEK/AI 审计 warning 数均为 `0`。
- MEK/AI 没有引入 WAZ 审计之外的新 term 语义路径。

WAZ 审计矩阵结果：

- BHE WAZ 覆盖 `76882` 条 `InfoCollection`，`173` 种语义路径。
- BSDX WAZ 覆盖 `59449` 条 `InfoCollection`，`126` 种语义路径。
- 两侧共有语义路径 `107` 种。
- BHE-only 语义路径 `66` 种，合计 `1648` 次。
- BSDX-only 语义路径 `19` 种，合计 `3123` 次。

BHE-only 语义分类：

- `PARAM_OPERATOR_CHAIN`：`8` 种路径，合计 `944` 次。
- `PARAMCOUNT_SPLIT`：`12` 种路径，合计 `346` 次。
- `BHE_ONLY_REVIEW`：`26` 种路径，合计 `181` 次。
- `ACTION_SELECTOR`：`11` 种路径，合计 `107` 次。
- `MULTILOCK_SELECTOR`：`6` 种路径，合计 `46` 次。
- `ATTR_FLAG_SELECTOR`：`3` 种路径，合计 `24` 次。

结论：MEK/AI 要接入同一套 converter，但复杂语义规则由 WAZ 全量审计驱动。

## 迁移矩阵输出

`TermMigrationMatrixAuditTest` 会输出以下文件：

- `summary.md`：总量统计、group 差异、MEK 审计桥接。
- `syntax-path-migration-matrix.tsv`：每条语义路径在 BHE/BSDX 两侧的出现次数和迁移状态。
- `bhe-only-path-rule-candidates.tsv`：BHE-only 路径的规则分类、候选动作、样例文件与对象路径。
- `converter-rule-candidate-matrix.tsv`：按规则分类汇总 BHE-only 路径。
- `term-group-migration-matrix.tsv`：两侧 term group 的存在性和 item 数量。
- `term-item-migration-diff.tsv`：两侧 term item 的同名、重排、参数差异。
- `bhe-aux-list-probe.tsv`：BHE 侧 `intList3/intList4` 候选解释探针。
- `bsdx-aux-list-probe.tsv`：BSDX 侧 `intList3/intList4` 候选解释探针。

`BheInfoCollectionTermConverterSemanticTest` 会输出以下文件：

- `summary.md`：转换总量、BSDX analyzer warning 数、目标语义路径数量、发生语义折叠的路径数量。
- `bhe-waz-term-conversion-details.tsv`：每条 BHE WAZ collection 的源语义路径、目标语义路径、目标字段值和 analyzer warning。

全量转换审计结果：

- 输入 `76882` 条 BHE WAZ `InfoCollection`。
- 输出 `76882` 条 BSDX `InfoCollection`。
- BSDX analyzer warning 行数为 `0`。
- 目标语义路径数量为 `142`。
- 发生语义路径变化的映射数量为 `41`。

关键变化映射：

- `PARAM/POS -> PARAM_POS/* -> OPERATOR_TWO/* -> OPERATOR/ADD` 折叠到 `PARAM/POS -> PARAM_POS/*`，合计 `931` 次。
- `PARAM/VECTOR -> ... -> OPERATOR_TWO/ADD -> OPERATOR/ADD` 折叠到 `PARAM/VECTOR -> PARAM_VECTOR/MOVE -> VECTOR1/ALL -> VECTOR2/SPEED`，合计 `12` 次。
- `PARAM/HP -> OPERATOR_TWO/MUL -> OPERATOR/ADD` 折叠到 `PARAM/VAL`，合计 `1` 次。
- `OBJECT4/PARAMCOUNT1/2/3/4` 折叠到 `OBJECT4/PARAMCOUNT`，合计 `346` 次。
- `MULTILOCK / ATTR / FLAG / BATTLESCRIPT / EQUIPMODE / HITRECT_FIRE` 等 BSDX 无承载条件降级到 `PARENT/UNCONDITIONAL`，合计 `76` 次。
- `ACTION_ATTACKCATEGORY / ACTION_ATTACKBUTTONTYPE / ACTION_CPU_SPECIALNO` 等攻击细分降级到 `ACTION_ATTACK`，合计 `26` 次。
- `ACTION_SDASH_END / ACTION_BDASH_END` 分别降级到 `ACTION_SDASH / ACTION_BDASH`，合计 `6` 次。
- `ACTION_ELEC / ACTION_OIL` 降级到 `ACTION_NOKEALL`，合计 `3` 次。
- `ANGLE_SEL/DAMAGE_LASEREDA` 降级到 `ANGLE_SEL/LASER02`，合计 `12` 次。
- `PARAMCOMP/HEIGHTANGLE` 降级到 `PARAMCOMP/HEIGHT`，合计 `12` 次。

## 语义精度损失清单

本节记录“能生成 BSDX 合法 term，但不保证 BHE 原语义无损”的位置。这里的精度损失都是显式策略，不是隐藏 fallback。

| 类别 | 数量 | 目标策略 | 损失内容 | 风险 |
|------|------|----------|----------|------|
| `PARAM_OPERATOR_CHAIN` | `944` | 折叠到 BSDX 可表达的直接参数路径 | 丢失 BHE 运算子修正量，例如 `DIV/ADD/MUL` 的运行时计算 | 中 |
| `PARAMCOUNT_SPLIT` | `346` | `PARAMCOUNT1/2/3/4 -> PARAMCOUNT` | 丢失 BHE 对参数计数槽位的细分 | 中 |
| `MULTILOCK / ATTR / FLAG / BATTLESCRIPT / EQUIPMODE / HITRECT_FIRE` | `76` | 降级到 `PARENT/UNCONDITIONAL` | 条件判断被移除，事件更容易执行 | 高 |
| `OBJECT/OBJECTPOS HIT_WAZA` | 原始 `180`，格式转换后存活 `161` | `HIT_WAZA -> HIT` | 丢失“按技能记忆”的限定，保留最近命中对象 | 中 |
| `OBJECT/OBJECTPOS MULTILOCK_*` | 原始 `57`，格式转换后存活 `38` | `MULTILOCK_* -> ROCK` | 丢失锁定列表，只保留转换时已保存的目标快照 | 中 |
| `OBJECTPOS2 CENTER/TOP` | `938 / 15` | `CENTER/TOP -> NONE` | BSDX aux 无通用半身高/全身高表达，高度追加量丢失 | 中 |
| WAZ BHE-only 目标槽 | 顶层槽 35=`138` units / `136` term collections；`CEventEffect[28]`=`45` | 按被丢弃 unit 写入结构化 drop audit | 目标选择事件本身不可恢复 | 高 |
| 攻击动作细分 | `26` | 降级到 `ACTION_ATTACK` | 丢失攻击类别、按钮类型、CPU 特殊编号等细分 | 中 |
| dash 结束状态 | `6` | `ACTION_SDASH_END -> ACTION_SDASH`，`ACTION_BDASH_END -> ACTION_BDASH` | 丢失“结束帧/结束态”细分 | 低 |
| 异常/受击动作细分 | `3` | `ACTION_ELEC/OIL -> ACTION_NOKEALL` | 丢失异常类型差异 | 中 |
| `DAMAGE_LASEREDA` 角度来源 | `12` | 降级到 `ANGLE_SEL/LASER02` | 丢失伤害激光枝的专有角度来源 | 中 |
| `HEIGHTANGLE` 比较 | `12` | 降级到 `PARAMCOMP/HEIGHT` | 丢失高度角度差异，仅保留高度比较 | 中 |

风险解释：

- 高：条件结构发生变化，可能改变技能分支是否执行。
- 中：保留大类语义，但丢失 BHE 的细分控制。
- 低：保留同族动作或同族状态，通常只丢失阶段细节。

## 核心规则

### 1. 禁止按数字索引直拷

BHE 和 BSDX 的 `term.grp` 结构不同：

- BHE term group 数为 `34`。
- BSDX term group 数为 `30`。
- BHE 独有 group：`ATTR`、`FLAG`、`MULTILOCK`、`OPERATOR`、`OPERATOR_TWO`。
- BSDX 独有 group：`ATTACK2`。

因此转换必须按 `groupCodeName + itemDescription` 语义重编译到 BSDX term index，不能复制 BHE 的 `int1/typeList` 数字。

### 2. BSDX 缺失语义必须显式处理

BHE-only 语义只能进入三种结果：

- 等价重写：BSDX 有等价承载，只是名字或结构不同。
- 审计 fallback：BSDX 没有完整语义，但有业务上可接受的明确降级。
- 保护性失败：没有进入规则表的未知语义不生成错误资源。

`OBJECTPOS / OBJECTPOS2` 使用 `20260331` 既定策略：

- `OBJECTPOS` 按 BHE 的 `termItemDescription` 查 BSDX，同名项不存在时落到 `ROCK`。
- `OBJECTPOS2` 按 BHE 的 `termItemDescription` 查 BSDX，同名项不存在时落到 `NONE`。

禁止在规则表外写隐藏 fallback。

### 3. aux list 必须按语义路径解释

`intList3/intList4` 不能写成全局固定含义。

实际审计结论：

- `POS/OBJECT` 中的 `intList3` 基本可按对象位置引用理解。
- `ANGLE/OBJECT` 中的 `intList3/intList4` 是角度计算的两个对象端点，不能全局硬套 `OBJECTPOS / OBJECTPOS2`。
- `intList4=[23,0]` 在 BHE `ANGLE/OBJECT` 中会命中 BHE `OBJECTPOS/MULTILOCK_LOCK`，硬套 `OBJECTPOS2` 会被误判为越界。

因此 aux list 重写规则必须绑定 `syntaxPathByDescription` 和宿主事件。

## 规则分组

### 共有语义路径

共有语义路径按语义重编译：

1. 解析 BHE collection 的主语法链。
2. 使用每一步的 `groupCodeName + itemDescription` 在 BSDX `term.grp` 中定位目标 group/item。
3. 生成 BSDX 的 `int1/typeList`。
4. 保留参数列表，除非该路径有单独规则。
5. 按路径规则处理 `intList3/intList4`。

### `PARAMCOUNT1/2/3/4`

BHE 存在：

- `OBJECT4/PARAMCOUNT1`
- `OBJECT4/PARAMCOUNT2`
- `OBJECT4/PARAMCOUNT3`
- `OBJECT4/PARAMCOUNT4`

BSDX 只有：

- `OBJECT4/PARAMCOUNT`

候选策略：等价折叠到 `PARAMCOUNT`，保留 compare 链和参数。该规则必须有测试覆盖。

### `PARAM_POS -> OPERATOR_TWO -> OPERATOR`

BHE 存在大量参数运算链，例如：

- `PARAM/POS -> PARAM_POS/HEIGHT2 -> OPERATOR_TWO/DIV -> OPERATOR/ADD`
- `PARAM/POS -> PARAM_POS/LENGTH -> OPERATOR_TWO/DIV -> OPERATOR/ADD`

BSDX 的 `PARAM_POS` 多数直接终止，缺少 `OPERATOR_TWO / OPERATOR` group。

迁移策略：折叠为 BSDX 直接 `PARAM_POS` 或 `PARAM_VECTOR` 路径，并丢弃 BHE 运算子参数。

原因：BSDX term 表没有 `OPERATOR_TWO / OPERATOR`，继续保留运算子会产生不可解析 term。折叠会丢失 BHE 的细分修正量，但能保留基础位置/向量语义。

### BHE-only 对象选择器

重点包括：

- `HIT_WAZA`
- `MULTILOCK_LOCK`
- `MULTILOCK_LOCK_B`
- `MULTILOCK_LOCK_N`
- `MULTILOCK_LENGTH`

迁移策略：

- 对象选择器能映射同名项时按同名项重编译。
- `HIT_WAZA`（OBJECT / OBJECTPOS）落到 `HIT`：两侧都读取最近命中对象，BSDX 仅缺少 BHE 的按技能记忆限定。
- `MULTILOCK_*`（OBJECT / OBJECTPOS 主链与 aux 第一位）落到 **`ROCK`**。
  - BHE MultiLock 返回锁定列表中的具体对象；BSDX `ROCK` 读取已保存目标，适合作为逐发目标快照。
  - `ENEMY` 会在求值时动态搜索最近敌人，会把多个锁定点坍缩成同一个近敌；`PARENTROCK` 会持续读取父对象当前目标，也不是逐发快照。
  - BSDX 无 MultiLock 事件槽：顶层 66–69 会丢弃；BHE `CEventEffect[28]` 标的槽也会因 BSDX 内层结构少一项而丢弃。两者都必须写入 drop audit，位置替代不能恢复锁定圈玩法。
- `OBJECTPOS2/CENTER` 与 `OBJECTPOS2/TOP` 落到 `NONE`，并显式接受半身高/全身高追加量损失。
- 除上述规则外，未知描述、`null` 或越界 aux 索引直接失败，不允许使用通用替代。

### BHE-only action / attr / flag

重点包括：

- `ACTION_ATTACKCATEGORY`
- `ACTION_ATTACKBUTTONTYPE`
- `ACTION_ATTACKTYPE`
- `ACTION_CPU_SPECIALNO`
- `ATTR/HIT`
- `FLAG`

迁移策略：无法承载的条件分支落到 `PARENT/UNCONDITIONAL`，无法承载的 action 细分落到 BSDX 可承载的近似动作。

- `ATTR / FLAG / MULTILOCK` 条件分支落到 `PARENT/UNCONDITIONAL`。
- `BATTLESCRIPT / EQUIPMODE` 条件分支落到 `PARENT/UNCONDITIONAL`。
- `ACTION_ATTACKCATEGORY / ACTION_ATTACKBUTTONTYPE / ACTION_ATTACKTYPE / ACTION_CPU_SPECIALNO` 落到 `ACTION_ATTACK`。
- `ACTION_SDASH_END` 落到 `ACTION_SDASH`。
- `ACTION_BDASH_END` 落到 `ACTION_BDASH`。
- `ACTION_ELEC / ACTION_FIRE / ACTION_OIL` 落到 `ACTION_NOKEALL`。

## 实现 checklist

- [x] 建立 MEK/AI InfoCollection 审计。
- [x] 建立 WAZ/MEK term 迁移矩阵审计。
- [x] 建立 term 字典层，按 `groupCodeName + itemDescription` 查询目标 term。
- [x] 实现共有语义路径重编译。
- [x] 实现 `PARAMCOUNT1/2/3/4 -> PARAMCOUNT` 显式规则。
- [x] 实现 `PARAM_POS -> OPERATOR_TWO -> OPERATOR` 折叠规则。
- [x] 实现 BHE-only 对象选择器 fallback 规则。
- [x] 实现 BHE-only action / attr / flag fallback 规则。
- [x] 把 converter 接入公共弹幕 WAZ。
- [x] 把 converter 接入单机体 WAZ。
- [x] 把 converter 接入 MEK AI。
- [x] 输出转换前后语义对比报告。

## 验收标准

- 所有进入 BSDX 的 `InfoCollection` 必须能被 BSDX analyzer 正常解析。
- BHE-only 语义必须在规则表中有明确去向，包含等价重写或审计 fallback。
- 没有进入规则表的 BHE-only 语义必须保护性失败。
- 测试输出必须能定位到文件、对象路径和原始 collection。
- 代码注释必须说明规则存在的业务原因。
