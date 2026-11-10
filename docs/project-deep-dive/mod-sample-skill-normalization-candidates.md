# Mod 样本技能差分归一化候选与共通 Pipeline 改造建议

## 1. 文档目标与证据边界

本文承接 `mod-sample-skill-behavior-diff.md`，不再逐技能复述 704 行差分，而是回答三个工程问题：

1. 哪些差分属于 BHE -> BSDX 的稳定格式或宿主语义差异，应进入共通 pipeline；
2. 哪些差分可以由共通机制完成主体转换，仅在角色 `Customizer` 中提供少量参数或选择器；
3. 哪些差分仍然依赖具体技能设计，不能包装成全局规则。

本次结论来自以下两类证据：

- 差分证据：178 个 MEK 选中技能、704 条精确差分，其中 `sou` 500 条、`tsukuyomi` 6 条、`misaki` 198 条；110 个技能至少包含一条差分。
- 生产代码证据：当前 `FollowupGraftPipeline`、WAZ/MEK 转换器、Term 重编译器、资源重绑器及 `SouCustomizer`、`MisakiCustomizer` 的实际实现。

Mod 数据是可运行参考，不是所有字段的绝对真值。本文中的“可覆盖”表示差分具有稳定的结构签名，可以由指定层统一生成；除已有实机结论外，不把静态差分直接断言为闪退的唯一根因。

## 2. 结论摘要

最值得优先归一化的是下面三组：

| 优先级 | 归一化项 | 精确差分行 | 技能 | 建议归属 | 结论 |
| :--- | :--- | ---: | ---: | :--- | :--- |
| P0 | `CEventHit Unit 6` 的宿主上下文 Term 转换 | 227 | 75 | 共通 pipeline | 新增一条宿主规则即可直接对齐 Mod 的 `ATTACK2/VAL`；同一规则应审计全部 270 个源节点 |
| P0 | 已有 Term 语义重编译与 aux 索引重排 | 120 | 32 | 共通 pipeline | 当前代码已经具备主要规则，应固化上下文和回归验证，不应在 `Customizer` 重写 |
| P1 | `OBJECTPOS2/CENTER -> NONE` 后的 `CEventEffect Unit 11` 高度补偿 | 201 | 6 | 共通机制 + `Customizer` 参数 | 25 个事件位点共享同一生成机理，角色侧只需提供默认高度和少量覆盖值 |

前三组互不重叠，合计 548 / 704 行，占 77.8%。其中：

- 120 行属于当前共通 Term 规则按代码推导即可吸收的差分；
- 227 行属于新增宿主上下文规则后可直接对齐 Mod 的差分；
- 201 行属于共通生成器加角色参数后可精确复现的差分。

这 77.8% 是设计覆盖率，不是已经运行生产 pipeline 后得到的消差结果。实施时仍需导出真实 pipeline 产物，再与 Mod 做同口径比较。

## 3. 704 行差分的完整分区

下表对全部差分做了无重叠分区，行数合计严格等于 704：

| 差分簇 | 行数 | 技能 | 事件位点 | 角色分布 | 建议处理层 |
| :--- | ---: | ---: | ---: | :--- | :--- |
| `CEventHit Unit 6: PARAM/VAL -> ATTACK2/VAL` | 227 | 75 | 227 | `sou` 214，`misaki` 13 | 共通 pipeline |
| `CEventEffect Unit 11` 高度补偿 | 201 | 6 | 25 | `sou` 45，`misaki` 156 | 共通机制 + `Customizer` 参数 |
| `CEventEffect Unit 7/25` aux 语义重排，与当前规则一致 | 105 | 27 | 77 | `sou` 84，`misaki` 21 | 已有共通 pipeline |
| `CEventHit Unit 6: PARAM/VAL -> NOKEZORITYPE/INPUT` | 43 | 16 | 43 | `sou` 41，`tsukuyomi` 2 | 不跟随 Mod 全局化，先按原生语料审计 |
| `CEventChange` 结构性修改 | 42 | 6 | 14 | 全部 `sou` | 逐技能 `Customizer` |
| 语义 WAZ 目标替换 | 21 | 14 | 21 | 全部 `sou` | 资源校验共通化，替代目标参数化 |
| `CEventEffect Unit 32` 新增 | 18 | 2 | 2 | 全部 `sou` | 共通写入原语 + 逐技能规则 |
| `CEventSprite` 的 `sou.spm -> chinatsu.spm` | 18 | 5 | 18 | 全部 `sou` | 共通资源校验 + `sou` 回退策略 |
| `PARAMCOUNT1/4 -> PARAMCOUNT` | 11 | 5 | 11 | `sou` 6，`misaki` 5 | 已有共通 pipeline |
| `PARAMCOMP/LENGTH -> LENGTH_Z` | 7 | 5 | 7 | `sou` 5，`tsukuyomi` 2 | 共通改写原语 + 技能选择器 |
| `CEventEffect Unit 7/25` 非通用锚点调整 | 6 | 3 | 3 | `sou` 4，`tsukuyomi` 2 | 逐技能 `Customizer` |
| `CEventChange` aux 索引语义重排，与当前规则一致 | 3 | 2 | 3 | `sou` 1，`misaki` 2 | 已有共通 pipeline |
| `CEventEffect Unit 28` aux 重排 / `Unit 10` 数值调整 | 2 | 2 | 2 | `sou` 1，`misaki` 1 | 前者已有共通规则，后者逐技能 |

## 4. 当前架构中真正的归一化边界

### 4.1 已经存在的共通主线

`sou` 和 `misaki` 已经通过各自很薄的 `*GraftPipeline` 委托给：

`src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/followup/FollowupGraftPipeline.java`

主线顺序是：

`baseline -> convert -> closure -> GRP append -> rebind -> initializer -> Customizer -> write`

其中 WAZ 和 MEK 在进入 `Customizer` 前已经完成资源重绑和 Term 重编译。当前唯一角色扩展点是：

`FollowupMekaCustomizer.customizeBeforeWrite(FollowupMekaContext context)`

这意味着：

- 落盘前的技能特例可以留在 `Customizer`；
- 需要读取“BHE 原语义为什么发生 fallback”的规则，不应只放在现有落盘前 hook 中，因为到该阶段部分源语义已经被折叠；
- 共通转换策略需要在 convert/rebind 前取得，不能全部依赖最后一次对象图遍历。

### 4.2 当前 `Customizer` 实际只处理了两类事情

- `SouCustomizer` 只调用 `SouSkillGlitchFixer`，处理 MEK 武器过滤、Lv3 `wazSeq`、热量和 FC 默认技能；没有处理本报告中的 WAZ Term、特效高度、资源替代或相位结构差分。
- `MisakiCustomizer` 只调用 `MisakiSkillGlitchFixer`，定点修正 `Tama05` 两个辅助技能中 `CEventEffect Unit 4` 的 `int5=999 -> 1`；没有处理本报告中的 201 行高度补偿。

因此，本报告列出的 WAZ 归一化项目前并未被角色 `Customizer` 普遍实现。

### 4.3 `tsukuyomi` 仍有一套平行实现

`tsukuyomi` 仍使用 `TsukuyomiGraftPipeline`，其 WAZ slot map、skill converter、object converter 与 follow-up 对应类的逻辑实质相同，差异主要是包名和注释；rebind 层则各自演进。

如果只修改 follow-up 包，`tsukuyomi` 不会自动获得修复。应先把下列纯转换能力下沉到 `meka/bhecommon`：

- BHE 83 槽到 BSDX 72 槽的映射；
- WAZ skill/phase/unit 结构转换；
- 宿主上下文 Term 重编译；
- `CEventEffect` fallback 补偿和审计。

rebind 编排可以暂时保留两个适配入口，但不能继续复制底层语义转换器。

### 4.4 当前代码落点

| 职责 | 当前文件与关键位置 | 与本报告的关系 |
| :--- | :--- | :--- |
| 主 WAZ/MEK 重绑与 `Customizer` 时序 | `meka/followup/FollowupGraftPipeline.java:144-180` | Term 重编译先于落盘前角色 hook |
| 主/辅助 WAZ Term 重编译 | `meka/followup/graft/RebindWazStep.java:55-58,80-82` | 两类 WAZ 都进入同一 rewriter |
| 对象图扫描 | `meka/bhecommon/term/BheInfoCollectionObjectGraphRewriter.java:22-65` | 当前只传递字符串路径，没有结构化 usage |
| Term 主链、fallback 与 aux 转换 | `meka/bhecommon/term/BheInfoCollectionTermConverter.java:70-265` | 已覆盖 120 行，也暴露宿主上下文缺口 |
| `CEventHit` 内层槽转换 | `dto/bsdx/waz/wazfactory/wazinfoclass/obj/CEventHit.java:330-412` | Unit 6 collection 在此被结构复制 |
| `CEventEffect` 内层槽转换 | `dto/bsdx/waz/wazfactory/wazinfoclass/obj/CEventEffect.java:180-263` | BHE Unit 28 被跳过，后续 Unit 顺移 |
| 资源引用重绑 | `meka/followup/graft/RebindWazStep.java:337-369` | 重写 WAZ/SPM/SE 等目标，但校验粒度不足 |
| 当前角色特例 | `meka/sou/SouSkillGlitchFixer.java`、`meka/misaki/MisakiSkillGlitchFixer.java` | 尚无本报告主要 WAZ 归一化规则 |

## 5. P0：让 Term 转换感知宿主事件和内层槽位

### 5.1 证据

差分中共有 270 行来自 `CEventHit Unit 6 / InfoCollectionList #1`，源路径全部是 BHE `PARAM/VAL`：

- 227 行、75 个技能在 Mod 中变为 BSDX `ATTACK2/VAL`；
- 43 行、16 个技能在 Mod 中变为 `NOKEZORITYPE/INPUT`。

对仓库原生语料的同宿主槽位进行全量扫描后得到更强的格式证据：

| 语料 | 含该结构的文件 | `CEventHit Unit 6` collection | 根 group |
| :--- | ---: | ---: | :--- |
| `src/main/resources/wazBheJson/*.json` | 102 | 3,603 | 3,603 条全部为 group 22，即 BHE `PARAM` |
| `src/main/resources/wazBsdxJson/*.json` | 108 | 2,764 | 2,764 条全部为 group 21，即 BSDX `ATTACK2` |

这说明同一个文本标签 `VAL` 在 `CEventHit` 攻击力内层槽位中具有宿主特定含义：

- 按数字 22 原样复制，会在 BSDX 被解释为 `NOKEZORITYPE/INPUT`；
- 按全局同名 `PARAM/VAL` 重编译，会落到 BSDX group 23；
- 按 BSDX 原生同宿主结构，应落到 group 21 `ATTACK2/VAL`。

### 5.2 当前代码缺口

`BheInfoCollectionTermConverter` 当前只按 `groupCodeName + itemDescription` 做全局语义查找。`BheInfoCollectionObjectGraphRewriter` 虽然携带反射路径字符串，但转换器不知道当前 collection 位于哪个事件、哪个内层槽位。

`CEventHit.transBheCEventHitToBsdx` 会先把 Unit 6 的 collection 结构复制到 BSDX DTO，之后 `RebindWazStep` 才对整个 WAZ 运行通用 Term rewriter。这个时序无法表达“仅在 `CEventHit Unit 6` 使用 `ATTACK2/VAL`”。

### 5.3 建议规则

给 Term 重编译增加结构化 usage，而不是根据反射路径字符串做模糊匹配。例如：

```text
DEFAULT
CEVENT_HIT_ATTACK_POWER
CEVENT_EFFECT_LAUNCH_POSITION
CEVENT_EFFECT_POSITION
CEVENT_EFFECT_VECTOR_DIRECTION
CEVENT_CHANGE_CONDITION
CEVENT_CHANGE_TRANSITION
```

当且仅当父节点是 `CEventHit.CEventHitUnit` 且 `unitSlotNum == 6` 时：

1. 要求源主路径是已审计的 `PARAM/VAL` 形状；
2. 输出 BSDX `ATTACK2/VAL`；
3. 保留 `paramList`、合法 aux 数据及 `int2`；
4. 未见过的源形状保护性失败并写入审计，不做隐藏 fallback。

不能把所有 BHE `PARAM/VAL` 全局替换为 `ATTACK2/VAL`，因为其他宿主仍需要 BSDX `PARAM` 语义。

### 5.4 `Customizer` 剩余工作

默认不需要角色参数。43 行 Mod `NOKEZORITYPE/INPUT` 与两侧原生语料的同宿主不变量冲突，不应为了逐字对齐 Mod 而加入 16 个例外。应先对代表技能做实际 pipeline 输出和运行时 A/B；只有出现可重复的业务证据时，才允许显式 opt-in 覆盖。

直接可对齐收益是 227 行；规则的实际审计范围是全部 270 个节点。

## 6. P0：固化当前已经具备的 Term 归一化能力

### 6.1 可由当前规则解释的 120 行

当前 `BheInfoCollectionTermConverter` 已经包含：

- 按语义名称重编译 Term group/item；
- `OBJECTPOS2/CENTER`、`OBJECTPOS2/TOP -> NONE`；
- `PARAMCOUNT1/2/3/4 -> PARAMCOUNT`；
- BHE-only 条件和 selector 的显式 fallback；
- aux list 的索引重排。

在 704 行差分中，以下 120 行与当前规则的目标值一致：

- 105 行 `CEventEffect Unit 7/25` aux 重排，覆盖 77 个事件位点、27 个技能；
- 11 行 `PARAMCOUNT1/4 -> PARAMCOUNT`，覆盖 5 个技能；
- 3 行 `CEventChange` 路径不变、仅 aux 语义索引变化；
- 1 行 `CEventEffect Unit 28` 的 `intList4` 语义索引变化。

这些行不应在 `SouCustomizer` 或 `MisakiCustomizer` 中再次实现。

### 6.2 需要补强而不是重写

当前 `convertAuxList` 对所有宿主使用相同解释，但 `TERM_MIGRATION_PLAN.md` 已明确指出 `POS/OBJECT` 与 `ANGLE/OBJECT` 的 aux 语义不能全局硬套。建议把现有结果保留，同时完成两项结构修正：

1. aux 转换按 usage 和主语法路径分派；
2. Term 转换返回结构化 audit，包括 `exact`、`semantic-collapse`、`fallback`、`dropped-detail`，供后续补偿与验证使用。

`PARAMCOUNT` 折叠只能保证 BSDX 格式可表达，不能恢复 BHE 的 1/2/3/4 通道差异。即使 11 行与 Mod 一致，也不能据此断言相关父子弹幕通信行为完全等价。

## 7. P1：把 CENTER 高度丢失变成共通补偿机制

### 7.1 差分不是 201 个独立调参

201 行全部来自 25 个 `CEventEffect` 事件位点，实际修改只有“确保 Unit 11 存在并设置高度值”这一种机理。一个新 Unit 11 会展开成存在性、帧区间和 6 个整数等 9 行差分，因此行数看起来远大于真实规则数。

精确分布如下：

| 角色 / 技能 | 事件位点 | Mod 操作 | 展开差分行 |
| :--- | ---: | :--- | ---: |
| `sou` / `SP_ISSEN` | 5 | 新增 Unit 11，`int5=30` | 45 |
| `misaki` / 5 个技能 | 15 | 新增 Unit 11，`int5=60` | 135 |
| `misaki` / `M_UPPER04`、`G_BIT09` | 3 | 已有 Unit 11，`int5: 0 -> 60` | 3 |
| `misaki` / `M_UPPER04` | 2 | 新增 Unit 11，`int5=30` | 18 |

`misaki` 的 5 个技能是 `M_UPPER04`、`BOOSTKICK04`、`G_BIT06`、`G_BIT09`、`S_HAMMER06`。

所有 25 个位点都同时满足：

1. BHE 发射位置 Unit 7 使用 `OBJECTPOS2` 索引 19，即 `CENTER`；
2. Mod 中该位置被重编译成 BSDX 索引 0，即 `NONE`；
3. Mod 通过 Unit 11 的高度值补回垂直位置。

因此，Unit 11 不是孤立美术微调，而是 `CENTER -> NONE` 语义损失后的兼容补偿。

### 7.2 建议共通算法

Term 转换结果应暴露 `OBJECTPOS2_CENTER_TO_NONE` 这一 fallback 原因。`CEventEffect` 兼容层接到该标记后：

1. 查询角色策略是否需要补偿；默认不补偿；
2. 查找 Unit 11；不存在则创建 BSDX `CEventValRandom`；
3. 新建时将未使用整数初始化为 0；
4. 帧窗口继承同一 `CEventEffect` 的有效载荷窗口，不按技能硬编码；
5. 将 `int5` 设置为策略给出的目标高度；
6. 重复执行不得再次新增 Unit 11，保证幂等；
7. audit 记录源 fallback、最终高度、选择器和是否覆盖已有值。

### 7.3 `Customizer` 只保留参数

建议由角色 policy 提供：

- `misaki`：对本角色主 WAZ 中符合条件的位点默认 `60`，`M_UPPER04` 的两个已确认位点覆盖为 `30`；
- `sou`：默认关闭，仅 `SP_ISSEN` 的已确认位点返回 `30`；
- `tsukuyomi`、`akao`：本次没有对应正证据，保持关闭。

选择器应使用技能双名称、阶段和源事件语义签名，例如被引用的 effect 身份及帧窗口；不要使用移植后的裸 `wazSeq` 或仅靠事件 ordinal，因为 initializer、资源 append 或事件增删会改变这些数字。

这样共通层负责对象创建、字段初始化、帧继承、幂等和审计，`Customizer` 只负责回答“这个位点补多少”。

## 8. P1：把资源替代改造成“校验 + 策略”，不要硬编码 Mod 索引

### 8.1 SPM 差分具有一个稳定簇

18 行闪退类信号全部位于 `sou`，覆盖 5 个技能：

- `DANTETSU03`
- `TYOUDAN03`
- `S_SHOOT03`
- `B_SYURIKEN03`
- `INITIALIZER`

它们都是解析后的资源身份从 `SELF / SOU / sou.spm` 变为 `CHINATSU2 / chinatsu.spm`。这可以由一条 `sou` 资源回退策略表达，但静态差分不能证明 `chinatsu.spm` 是所有 Sou sprite 引用的安全通用替身。

当前 `BheResourceIndexResolver` 会重绑 SpriteGroup 和公共 action base，`RebindWazStep.validateRebindResult` 只验证 WAZ 非空和映射表非空，没有逐个验证：

- 目标 SpriteGroup 是否存在；
- `actionGroupNumber` 是否在目标 SPM 中存在；
- `actionNumber` 是否在该 action group 中存在；
- 对应图片、pattern 和 program material 闭包是否完整。

### 8.2 建议共通能力

在资源重绑后、`Customizer` 前增加语义引用校验器：

```text
VALID                         -> 保留共通重绑结果
INVALID + FAIL               -> 保护性失败并输出完整定位
INVALID + USE_IMPORTED_SELF  -> 回到已导入的本角色 SPM
INVALID + USE_PROXY          -> 使用 Customizer 指定的代理资源和 action 映射
```

`sou -> chinatsu.spm` 只能作为 `SouCustomizer` 提供的候选 proxy，且应以技能/事件选择器和 action 映射表达，不能写成全局数字 `12 -> 12`。数字相同并不代表资源身份相同。

### 8.3 21 行 WAZ 目标替换不构成一条全局映射

另外 21 行语义 WAZ 引用变化覆盖 14 个 `sou` 技能，目标包括不同 hit effect、烟雾、闪光、弹药和未命名条目。它们不是一个稳定的 source -> target 对，因此：

- 共通层负责资源闭包、语义身份重绑和目标存在性校验；
- 只有校验失败或已有实机证据时，`Customizer` 才提供明确 alias；
- 不把朋友 Mod 中“原始数字相同但语义资源已经变化”的结果当作应复制的共通规则。

若 SPM 的 18 行经运行时验证成立，则可在上述共通校验器上用一条 `sou` policy 覆盖；在验证前不计入 77.8% 的确定归一化收益。

## 9. P2：共通化“修改原语”，但保留逐技能语义

### 9.1 建议建立稳定的 WAZ mutation API

当前 `MisakiSkillGlitchFixer` 自己遍历 WAZ/phase/unit/object，并直接用辅助 WAZ 的裸索引 336、339 和基线索引 488、491 定位。后续每个特例都复制这类遍历，会让 `Customizer` 越来越脆弱。

可以下沉一组不带业务判断的共通原语：

- `findSkill(SkillIdentity)`：按日文名 + 英文名定位并要求唯一；
- `findEvents(EventSelector)`：按阶段、顶层槽位、事件类型和语义锚点定位；
- `ensureEffectUnit(slot, type)`：幂等取得或创建内层 Unit；
- `setRandomValue(field, value)`：设置 `CEventValRandom` 字段并记录 before/after；
- `rewriteTerm(fromPath, toPath)`：只改已声明的 Term 形状；
- `removeEvent` / `upsertChangeEvent`：显式修改相位结构；
- `rewriteSemanticResource(fromIdentity, toIdentity)`：按资源身份而不是裸数字替换。

这些 API 负责唯一性、类型、帧窗口、幂等和审计；`Customizer` 继续保存技能选择器和业务值。

### 9.2 适合使用原语、但不适合全局自动化的簇

- `CEventEffect Unit 32`：18 行只是 2 个事件位点新增 `int5=32`，技能为 `BODYSLUM03`、`EX_KNIFE`；可复用 `ensureEffectUnit`，不能据此给所有技能添加 Unit 32。
- `CEventEffect Unit 10`：只有 `misaki/S_HAMMER06` 1 行 `int5: 0 -> 60`，保留为技能规则。
- 非通用 Unit 7/25 锚点：6 行、3 个事件位点，涉及 `BODYSLUM03`、`HAKKEI03`、`G_FIELD`；目标对象选择各不相同，不能由 `CENTER -> NONE` 自动推导。
- `PARAMCOMP/LENGTH -> LENGTH_Z`：7 行、5 个技能，但相同样本中另有 9 个 BHE `LENGTH` 条件在 Mod 中仍保持 `LENGTH`。因此只能提供共通 Term 改写原语，不能全局改名。
- `CEventChange` 结构修改：42 行集中在 6 个 `sou` 技能，其中 `SP_ISSEN` 27 行、`N_HOUDAN03` 9 行，其余为 `H_LASER03`、`BODYSLUM03`、`S_HAMMER03`、`MUSOU03`。这些涉及新增/删除事件、条件、转移和帧窗口，必须保留逐技能语义。

## 10. 明确不应“一刀切”归一化的部分

### 10.1 Mod 中的 43 行 `NOKEZORITYPE/INPUT`

这 43 行是 BHE group 22 数字直接落入 BSDX group 22 后的解释结果，与 BSDX 原生 `CEventHit Unit 6` 的 2,764 / 2,764 条 `ATTACK2` 不变量冲突。除非运行时证据推翻原生语料，应把它们视为待审计差异，而不是例外表的输入。

### 10.2 映射为 `-1` 的 25 个 BHE 节点组

25 个丢弃节点组全部来自 `sou` 的 6 个技能，分布为：

- 槽位 23 `ﾍﾞｸﾄﾙ：速度XYZ`：8 组；
- 槽位 35 `標的`：1 组；
- 槽位 52 `ＣＰＵ特殊行動`：1 组；
- 槽位 66..69 `ﾏﾙﾁﾛｯｸ:ロック開始・終了`、`ﾏﾙﾁﾛｯｸ:ロック`、`ﾏﾙﾁﾛｯｸ:ターゲットカウンタ`、`ﾏﾙﾁﾛｯｸ:描画フラグ`：15 组。

BSDX 没有同构槽位，不能靠 slot map 或 `Customizer` 数值微调恢复。共通层可以完善 drop audit 和策略接口，但任何替代实现都需要 producer -> state -> consumer 证据和逐技能运行时验证。

### 10.3 Mod 的结构性相位修改

新增或删除 `CEventChange` 会改变状态机拓扑。共通 helper 可以让写法统一，但不能把 `SP_ISSEN`、`N_HOUDAN03` 或 `H_LASER03` 的结构变更推广到其他技能。

### 10.4 `PARAMCOUNT` 折叠的行为精度

`PARAMCOUNT1/2/3/4 -> PARAMCOUNT` 是格式兼容规则，不是无损语义等价。共通 pipeline 应保留该规则和损失审计；具体技能若依赖独立通道，仍需专属恢复策略，不能因为 Mod 也使用单一 `PARAMCOUNT` 就关闭风险检查。

## 11. 建议的共通 Pipeline 形态

当前 `Customizer` 只有落盘前命令式 hook。建议保留兼容性的同时增加只读 policy：

```text
FollowupMekaCustomizer
  policy() -> FollowupMekaPolicy       # 默认返回严格、无角色特例的 DEFAULT
  customizeBeforeWrite(context)        # 保留最终逐技能修正

FollowupMekaPolicy
  termOverrides
  effectPositionFallbackPolicy
  resourceFallbackPolicy
  skillMutationRecipes
```

推荐执行顺序：

1. 结构转换：统一的 BHE WAZ/MEK/GRP/SPM converter；
2. 上下文 Term 重编译：根据宿主事件和内层槽位生成目标 Term，并输出 fallback audit；
3. 格式损失补偿：根据 audit 和角色 policy 处理 Unit 11 等可推导补偿；
4. 资源重绑：按语义身份构建目标引用；
5. 资源闭包校验：验证 WAZ/SPM/action/SE/voice 的实际目标；
6. 角色 mutation recipes：执行少量明确的逐技能结构或数值修正；
7. `customizeBeforeWrite`：仅保留无法声明化的最后修正；
8. 输出统一审计：记录自动规则、角色规则、drop、fallback 和未解析引用。

policy 必须在 convert/rebind 前取得。否则 `CENTER` 已经变成 `NONE` 后，最后的 `Customizer` 无法可靠区分“源本来就是 NONE”和“由 CENTER 降级而来”。

## 12. 实施顺序

### 第一批：低风险、高收益

1. 把 tsukuyomi/follow-up 的纯 WAZ converter 下沉为一份共通实现；
2. 给 Term rewriter 增加 usage，先实现 `CEventHit Unit 6 -> ATTACK2/VAL`；
3. 固化现有 120 行 Term 规则的实际 pipeline 输出回归；
4. 输出逐 collection 的规则命中和 fallback audit。

第一批不需要新增角色技能表，且能避免同一规则在两套 pipeline 漂移。

### 第二批：共通机制 + 少量参数

1. 让 Term 转换返回 `CENTER_TO_NONE` 等结构化原因；
2. 实现幂等 `ensureEffectUnit`；
3. 接入 `CEventEffect Unit 11` 高度补偿 policy；
4. 在 `MisakiCustomizer`、`SouCustomizer` 只声明高度默认值和少数选择器覆盖。

### 第三批：资源与逐技能规则

1. 增加 Sprite/WAZ/action 目标完整性校验；
2. 用运行时证据决定是否启用 `sou -> chinatsu.spm` proxy；
3. 将 Unit 32、LENGTH_Z 和 `CEventChange` 修改写成可审计 recipe；
4. 保留映射为 `-1` 的节点为显式未解决项，不以 no-op 伪装完成。

## 13. 验证矩阵

实施后至少需要以下验证，不能只看对象级单元测试：

| 层次 | 验证内容 | 验收条件 |
| :--- | :--- | :--- |
| 原生语料不变量 | 全量扫描 BHE/BSDX `CEventHit Unit 6` | BHE 已知形状全部命中新规则；BSDX 输出全部为合法 `ATTACK2` 形状 |
| Term | 转换后运行 BSDX analyzer | warning 为 0；每个 fallback 有规则名和源路径 |
| 幂等 | 对同一 WAZ 连续执行两次 normalizer | 不重复新增 Unit 11/32，不重复改写资源 |
| 差分 | 导出真实 pipeline JSON，再运行同口径差分 | 分别报告自动消失、policy 消失、仍保留三类，不用 raw BHE 推测代替产物 |
| 资源 | 遍历全部 WAZ/SPM/action/SE/voice 引用 | 目标存在、范围合法、闭包完整；proxy 使用点可定位 |
| 回归 | 覆盖 150 个 `sou`、8 个 `tsukuyomi`、20 个 `misaki` 选中技能 | 无新增未解析引用，无新增静默 drop |
| 运行时 | 代表技能逐项 A/B | 伤害、命中、位置、相位、取消、资源显示和稳定性分别记录 |

代表技能建议至少包含：

- `STRAIGHT03`、`M_UPPER04`：验证 `ATTACK2/VAL`；
- `N_MISSILE03`、`CLOW05`：专门验证 Mod `NOKEZORITYPE/INPUT` 与原生不变量冲突的 43 行；
- `SP_ISSEN`、`S_HAMMER06`、`G_BIT09`：验证 Unit 11 的 30/60 高度 policy；
- `DANTETSU03`、`TYOUDAN03`、`S_SHOOT03`、`B_SYURIKEN03`、`INITIALIZER`：验证 SPM 资源策略；
- `N_HOUDAN03`、`H_LASER03`、`BODYSLUM03`：验证仍保留为逐技能 recipe 的相位或不可承载结构。

## 14. 最终判断

本批差分中最适合“一劳永逸”的不是按技能复制 Mod 字段，而是补齐两个共通概念：

1. Term 转换必须知道 collection 的宿主事件和内层槽位；
2. 格式降级必须产生结构化原因，后续由共通补偿器和角色 policy 协作处理。

落实这两点后，`Customizer` 不再负责遍历 DTO 和创建底层对象，而只保存角色参数、资源回退选择和少量状态机 recipe。这样既能直接覆盖大多数重复差分，也不会把 `SP_ISSEN`、`N_HOUDAN03`、MultiLock 等真正的技能语义误包装成危险的全局规则。
