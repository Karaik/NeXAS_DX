# Mod 样本技能战斗行为基准差分

## 证据边界

本文只比较五类战斗表现信号：闪退引用/帧边界、CEventHit 伤害与连段、CEventEffect 特效与向量、CEventChange/CEventCancel 相位窗口、屏幕与慢动作。覆盖替换造成的机体槽位、基础文本及非战斗环境差异不进入结果。

- BHE 事件先按生产代码 `BheToBsdxWazSlotMap`、`CEventHit#transBheCEventHitToBsdx` 与 `CEventEffect#transBheCEventEffectToBsdx` 归一化为 BSDX 语义，再与 Mod 比较。
- WAZ/SPM 数字索引先通过各自 `WazaGroup.grp` / `SpriteGroup.grp` 解析成资源与技能身份；相同身份的纯索引重绑被过滤。
- `BheInfoCollection` 与 `BsdxInfoCollection` 分别通过各自 `Term.grp` 解码为原始 Term 路径与操作数；不比较引擎内部的分组编号。
- 主键是 `(skillNameJapanese, skillNameEnglish)`；BHE Index、Mod wazSeq 与 Mod MEK 键仅作为辅助定位。阶段与事件编号在本文中从 1 开始；`branchPhaseNo` 保留原始参数值。
- Lv3 `+2` Customizer 映射不在本次分析范围内，未作任何变更。

## 覆盖范围汇总

| 角色 | MEK 选中的可执行技能 | 含高价值差分的技能 | 精确差分行 | 默认转换丢弃的 BHE 节点组 | 未解析源引用 | 未解析 Mod 引用 |
| :--- | ---: | ---: | ---: | ---: | ---: | ---: |
| `sou` | 150 | 97 | 500 | 25 | 0 | 0 |
| `tsukuyomi` | 8 | 4 | 6 | 0 | 0 | 0 |
| `misaki` | 20 | 9 | 198 | 0 | 0 | 0 |

## akao

### 基准数据可用性

- `Update3` 解包载荷包含 0 个 `.waz` 文件和 0 个 `.mek` 文件。
- `Update4` 包含 12 个 `.waz` 文件和 3 个 `.mek` 文件；其中可直接对应角色的文件对为 `aki.waz` / `aki.mek`、`misaki.waz` / `mohawk.mek`、`zako681a.waz` / `zako102a.mek`。
- 本次请求范围内与 `akao` 有关的证据只有 `HellConfig.dat` 和 `SelectMekaMenuMeka.spm`，两者均不包含 Mod `akao` 的 CEventHit/CEventEffect/CEventChange/CEventCancel 技能图。
- 因此，在不虚构证据的前提下，无法生成 `akao` 的逐技能 BHE 源数据与 Mod 基准数据差分行。本章记录这一否定性结果，并从战斗差分中排除编组和槽位替换产物。

## sou

- **MEK 选中的可执行技能数**: 150
- **含高价值差分的技能数**: 97
- **精确字段/节点差分行数**: 500
- **分类行数**: ① 闪退避让点=18; ② 伤害与连段补正=264; ③ 粒子特效与坐标向量=164; ④ 动作相位衔接与取消窗口=54; ⑤ 屏幕演出与慢动作=0

### アームストレートLv3 / STRAIGHT03（原索引：BHE 041 / Mod wazSeq 011）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 0
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### アームアッパーLv3 / UPPER03（原索引：BHE 044 / Mod wazSeq 012）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 1
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### アームショットLv3 / ARMSHOT03（原索引：BHE 047 / Mod wazSeq 013）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 2
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ローキックLv3 / LOWKICK03（原索引：BHE 050 / Mod wazSeq 014）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 3
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[90],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[90],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[90],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[90],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 回し蹴りLv3 / KICK03（原索引：BHE 053 / Mod wazSeq 015）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 4
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=6; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #5 / Mod #5) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 6 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 6 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ジャンピングニーLv3 / NEE03（原索引：BHE 056 / Mod wazSeq 016）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 5
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ショートエルボーLv3 / ELBOW03（原索引：BHE 059 / Mod wazSeq 017）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 6
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### タックルLv3 / TACKLE03（原索引：BHE 062 / Mod wazSeq 018）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 7
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=3; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:165 (EFFECT / ◆ブーストダッシュ煙 / SMOKE24) | 0:157 (EFFECT / 楕円岩 / 0020) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:165 (EFFECT / ◆ブーストダッシュ煙 / SMOKE24) | 0:157 (EFFECT / 楕円岩 / 0020) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:165 (EFFECT / ◆ブーストダッシュ煙 / SMOKE24) | 0:157 (EFFECT / 楕円岩 / 0020) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 崩牙落としLv3 / BODYSLUM03（原索引：BHE 065 / Mod wazSeq 019）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 8
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=14; ③ 粒子特效与坐标向量=11; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **仅 BHE 存在的转换风险边界**: 阶段 2 - 原始 BHE 槽位 35: 1 个事件，帧范围 0..0。这些节点在生产槽位映射中映射为 `-1`，因此默认转换基线与 Mod 均不包含它们；不能将其归因于朋友对 Mod 的修改。
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #5 / Mod #5) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #5 / Mod #5) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[10,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[7,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[10,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[7,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int5` | <不存在> | 32 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[90],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/VIEW -> COMPARE/EQUALOVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[90],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/VIEW2 -> COMPARE/EQUALOVER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 26 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 26 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### フライングクロスチョップLv3 / CROSSCHOP03（原索引：BHE 068 / Mod wazSeq 020）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 9
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ブーストアッパーLv3 / BOOSTUPPER03（原索引：BHE 071 / Mod wazSeq 021）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 10
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### マグナムアッパーLv3 / M_UPPER03（原索引：BHE 074 / Mod wazSeq 022）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 11
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### スライディングキックLv3 / SLIDINGKICK03（原索引：BHE 077 / Mod wazSeq 023）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 12
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[140],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[140],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ブーストキックLv3 / BOOSTKICK03（原索引：BHE 080 / Mod wazSeq 024）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 13
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 70 CEventSlowRate; 阶段 7 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 彗星脚Lv3 / G_STRIKE03（原索引：BHE 083 / Mod wazSeq 025）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 14
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### サマーソルトキックLv3 / SUMMERSOLT03（原索引：BHE 086 / Mod wazSeq 026）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 15
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ドロップキックLv3 / DROPKICK03（原索引：BHE 089 / Mod wazSeq 027）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 16
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[120],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[120],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[120],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[120],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### カイザーキックLv3 / KAISERKICK03（原索引：BHE 092 / Mod wazSeq 028）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 17
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 次元拳法フリッカーLv3 / WARP_P1_03（原索引：BHE 095 / Mod wazSeq 029）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 18
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 次元拳法ナックルボーンLv3 / WARP_P2_03（原索引：BHE 098 / Mod wazSeq 030）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 19
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 次元拳法アッパーカットLv3 / WARP_P3_03（原索引：BHE 101 / Mod wazSeq 031）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 20
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 電光パンチLv3 / S_HAMMER03（原索引：BHE 104 / Mod wazSeq 032）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 21
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[10,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[10,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### プラズマフィンガーLv3 / S_KNUCKLE03（原索引：BHE 107 / Mod wazSeq 033）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 22
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ショックハンマーLv3 / S_HAMMER03（原索引：BHE 110 / Mod wazSeq 034）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 23
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[170],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[170],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/VIEW -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### Ｅ・フィンガーLv3 / E_FINGER03（原索引：BHE 113 / Mod wazSeq 035）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 24
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 48 CEventEffect; 阶段 7 - 槽位 60 CEventScreenYure; 阶段 7 - 槽位 65 CEventBlur; 阶段 7 - 槽位 69 CEventSlow; 阶段 7 - 槽位 70 CEventSlowRate; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=5; ③ 粒子特效与坐标向量=3; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[180],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[180],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:237 (EFFECT / ◆フレイア：槍炎 / HEATBOMB06) | 0:207 (EFFECT / ◆フレイア：槍炎：回転 / HEATBOMB08) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 8 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 8 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ヒートナックルLv3 / HNUCKLE03（原索引：BHE 116 / Mod wazSeq 036）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 25
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 発勁Lv3 / HAKKEI03（原索引：BHE 119 / Mod wazSeq 037）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 26
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[10,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[1,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[10,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[1,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### オーバーヘッドキックLv3 / OVERHEAD_KICK03（原索引：BHE 122 / Mod wazSeq 038）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 27
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[180],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[180],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 無双正拳突きLv3 / MUSOU03（原索引：BHE 125 / Mod wazSeq 039）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 28
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=3; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/WAZAHITCOUNT -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/REVLASERLOST","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ロケットヘッドLv3 / R_HEAD03（原索引：BHE 128 / Mod wazSeq 040）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 29
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 47 CEventCancel; 阶段 7 - 槽位 48 CEventEffect; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 41 CEventHit; 阶段 8 - 槽位 47 CEventCancel; 阶段 8 - 槽位 48 CEventEffect; 阶段 8 - 槽位 71 CEventChange; 阶段 9 - 槽位 71 CEventChange; 阶段 10 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=5; ③ 粒子特效与坐标向量=8; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **仅 BHE 存在的转换风险边界**: 阶段 4 - 原始 BHE 槽位 23: 1 个事件，帧范围 0..5; 阶段 5 - 原始 BHE 槽位 23: 1 个事件，帧范围 0..5; 阶段 6 - 原始 BHE 槽位 23: 1 个事件，帧范围 0..5; 阶段 7 - 原始 BHE 槽位 23: 1 个事件，帧范围 0..5; 阶段 8 - 原始 BHE 槽位 23: 1 个事件，帧范围 0..5; 阶段 9 - 原始 BHE 槽位 23: 1 个事件，帧范围 13..39; 阶段 10 - 原始 BHE 槽位 23: 1 个事件，帧范围 0..5。这些节点在生产槽位映射中映射为 `-1`，因此默认转换基线与 Mod 均不包含它们；不能将其归因于朋友对 Mod 的修改。
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 8 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 6 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 7 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 8 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 现有实机记录仅确认症状：实机释放直接闪退。 该症状不作为字段因果证明；因果边界仍以本节表内的 Mod 数据差异为准。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 13 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 人間砲弾Lv3 / N_HOUDAN03（原索引：BHE 131 / Mod wazSeq 041）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 30
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 48 CEventEffect; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 41 CEventHit; 阶段 8 - 槽位 71 CEventChange; 阶段 9 - 槽位 41 CEventHit; 阶段 9 - 槽位 71 CEventChange; 阶段 10 - 槽位 41 CEventHit; 阶段 10 - 槽位 71 CEventChange; 阶段 11 - 槽位 41 CEventHit; 阶段 11 - 槽位 71 CEventChange; 阶段 12 - 槽位 41 CEventHit; 阶段 12 - 槽位 47 CEventCancel; 阶段 12 - 槽位 71 CEventChange; 阶段 13 - 槽位 47 CEventCancel; 阶段 13 - 槽位 48 CEventEffect; 阶段 13 - 槽位 71 CEventChange; 阶段 14 - 槽位 47 CEventCancel; 阶段 14 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=10; ③ 粒子特效与坐标向量=11; ④ 动作相位衔接与取消窗口=13; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[400],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[400],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[250],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 8 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[400],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[400],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 9 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 10 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 11 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 12 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:234 (EFFECT / ◆煙 / BOM26) | 0:3 (EFFECT / <范围内未命名条目>) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:234 (EFFECT / ◆煙 / BOM26) | 0:3 (EFFECT / <范围内未命名条目>) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 6 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 7 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / 事件存在性` | <不存在> | 存在（帧 22..114） | ④ 动作相位衔接与取消窗口: Mod 新增完整事件及其明确帧窗口。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / 条件 #1` | <不存在> | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[300],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/WAZACOUNT -> COMPARE/EQUALOVER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / flag` | <不存在> | 1 | ④ 动作相位衔接与取消窗口: 改变 CEventChange 的 flag，从而改变编码的条件列表/分支结构。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / int1` | <不存在> | 0 | ④ 动作相位衔接与取消窗口: 精确行为控制值不同；数据可证明修改，但不能单独证明唯一运行时根因。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / nextPhaseNo（推导值，从零开始）` | <不存在> | 1 | ④ 动作相位衔接与取消窗口: 改变推导出的线性/结束阶段目标。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / 转移 #1` | <不存在> | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"JUMP/TRUN -> TURN/NEXT","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 CEventChange 转移路径或操作数。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / endFrame` | <不存在> | 114 | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 1 - 槽位 71 CEventChange (无 / Mod #1) / startFrame` | <不存在> | 22 | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 1 - 槽位 71 CEventChange (BHE #1 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT4 -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[3,0],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_OFF2","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/EQUAL","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/EQUAL","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[1],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/EQUAL","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[],"paramList":[1],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/EQUAL","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #3 / Mod #3) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[2],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/EQUAL","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[],"paramList":[2],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/EQUAL","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #4 / Mod #4) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[3],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/EQUAL","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[],"paramList":[3],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/EQUAL","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 现有实机记录仅确认症状：实机释放后本体隐形且未恢复。 该症状不作为字段因果证明；因果边界仍以本节表内的 Mod 数据差异为准。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 34 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 必滅のデスハンドLv3 / C_PUNCH03（原索引：BHE 134 / Mod wazSeq 042）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 31
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 47 CEventCancel; 阶段 7 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=3; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[400],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[400],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[500],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[500],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### カワラブレイクLv3 / KAWARA03（原索引：BHE 140 / Mod wazSeq 044）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 32
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 65 CEventBlur; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=3; ③ 粒子特效与坐标向量=1; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:82 (EFFECT / リング楕円黄 / RING05) | 0:72 (EFFECT / フラッシュ / FLASH01) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 现有实机记录仅确认症状：实机砸地阶段直接闪退。 该症状不作为字段因果证明；因果边界仍以本节表内的 Mod 数据差异为准。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ナイトメアパーティーLv3 / KOUMORI03（原索引：BHE 143 / Mod wazSeq 045）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 33
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=9; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **仅 BHE 存在的转换风险边界**: 阶段 2 - 原始 BHE 槽位 23: 2 个事件，帧范围 15..15, 21..28。这些节点在生产槽位映射中映射为 `-1`，因此默认转换基线与 Mod 均不包含它们；不能将其归因于朋友对 Mod 的修改。
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 9 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 9 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ビームソードLv3 / SWORD03（原索引：BHE 147 / Mod wazSeq 046）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 34
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 42 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[120],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[120],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 42 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[500],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[500],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### サーベルLv3 / SABER03（原索引：BHE 150 / Mod wazSeq 047）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 35
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=1; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[140],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[140],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:161 (EFFECT / 楕円岩 / 0020) | 0:155 (EFFECT / ミサイル炎2TURN変更終了 / 0018) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ツヴァイハンダーLv3 / T_HANDER03（原索引：BHE 153 / Mod wazSeq 048）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 36
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### スウェーバックナイフLv3 / KNIFE03（原索引：BHE 156 / Mod wazSeq 049）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 37
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ナイフボルテクスLv3 / KNIFE_V03（原索引：BHE 159 / Mod wazSeq 050）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 38
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 42 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=9; ③ 粒子特效与坐标向量=1; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:184 (EFFECT / ◆刀の軌跡：ヒットマーク用 / 0144) | 0:3 (EFFECT / <范围内未命名条目>) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[3],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[3],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:184 (EFFECT / ◆刀の軌跡：ヒットマーク用 / 0144) | 0:3 (EFFECT / <范围内未命名条目>) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[3],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[3],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:184 (EFFECT / ◆刀の軌跡：ヒットマーク用 / 0144) | 0:3 (EFFECT / <范围内未命名条目>) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 2 - 槽位 42 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:37 (EFFECT / ヒットマーク（斬撃4 360度） / HIT_ZANGEKI05) | 0:37 (EFFECT / ヒットマーク相殺 / HITMARK09) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 10 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 10 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### アサシンリッパーLv3 / RIPPER03（原索引：BHE 162 / Mod wazSeq 051）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 39
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=4; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:36 (EFFECT / ヒットマーク（斬撃4） / HIT_ZANGEKI04) | 0:36 (EFFECT / ヒットマークBL2 / HITMARK06) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ランスLv3 / LANCE03（原索引：BHE 165 / Mod wazSeq 052）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 40
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### フィッシャーストライクLv3 / F_STRIKE03（原索引：BHE 168 / Mod wazSeq 053）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 41
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[170],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[170],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ランサーチャージLv3 / LANCER_CHARGE03（原索引：BHE 171 / Mod wazSeq 054）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 42
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 烈風斬Lv3 / REPPUU03（原索引：BHE 174 / Mod wazSeq 055）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 43
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **仅 BHE 存在的转换风险边界**: 阶段 1 - 原始 BHE 槽位 52: 1 个事件，帧范围 0..32。这些节点在生产槽位映射中映射为 `-1`，因此默认转换基线与 Mod 均不包含它们；不能将其归因于朋友对 Mod 的修改。
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### Ｉ・Ａ・ＩLv3 / IAI03（原索引：BHE 177 / Mod wazSeq 056）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 44
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 月光斬Lv3 / ENGETSU03（原索引：BHE 180 / Mod wazSeq 057）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 45
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 69 CEventSlow; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=4; ③ 粒子特效与坐标向量=1; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:37 (EFFECT / ヒットマーク（斬撃4 360度） / HIT_ZANGEKI05) | 0:37 (EFFECT / ヒットマーク相殺 / HITMARK09) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 5 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 5 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### スクレイパーLv3 / SCRAVER03（原索引：BHE 183 / Mod wazSeq 058）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 46
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=8; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 8 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 8 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### サイスLv3 / SCYTHE03（原索引：BHE 186 / Mod wazSeq 059）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 47
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### クレッセントLv3 / SCRACHER03（原索引：BHE 189 / Mod wazSeq 060）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 48
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[15],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[15],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[140],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[140],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### アトラクターエッジLv3 / EGGE03（原索引：BHE 192 / Mod wazSeq 061）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 49
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### チェインエッジLv3 / KUSARIGAMA03（原索引：BHE 195 / Mod wazSeq 062）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 50
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 48 CEventEffect; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 41 CEventHit; 阶段 8 - 槽位 48 CEventEffect; 阶段 8 - 槽位 71 CEventChange; 阶段 9 - 槽位 41 CEventHit; 阶段 9 - 槽位 48 CEventEffect; 阶段 9 - 槽位 71 CEventChange; 阶段 10 - 槽位 41 CEventHit; 阶段 10 - 槽位 48 CEventEffect; 阶段 10 - 槽位 71 CEventChange; 阶段 11 - 槽位 41 CEventHit; 阶段 11 - 槽位 48 CEventEffect; 阶段 11 - 槽位 71 CEventChange; 阶段 12 - 槽位 41 CEventHit; 阶段 12 - 槽位 70 CEventSlowRate; 阶段 12 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=15; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 8 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 9 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 10 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 11 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 12 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 15 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 15 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### スニークエッジLv3 / EGGE02_03（原索引：BHE 198 / Mod wazSeq 063）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 51
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 48 CEventEffect; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 41 CEventHit; 阶段 8 - 槽位 48 CEventEffect; 阶段 8 - 槽位 71 CEventChange; 阶段 9 - 槽位 41 CEventHit; 阶段 9 - 槽位 48 CEventEffect; 阶段 9 - 槽位 71 CEventChange; 阶段 10 - 槽位 70 CEventSlowRate; 阶段 10 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=15; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 8 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 8 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 9 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 9 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 15 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 15 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### クラッシュハンマーLv3 / HAMMER03（原索引：BHE 201 / Mod wazSeq 064）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 52
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### バトルハンマーLv3 / B_HAMMER03（原索引：BHE 204 / Mod wazSeq 065）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 53
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=3; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[220],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### だるま落としLv3 / DARUMA03（原索引：BHE 207 / Mod wazSeq 066）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 54
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 47 CEventCancel; 阶段 8 - 槽位 48 CEventEffect; 阶段 8 - 槽位 71 CEventChange; 阶段 9 - 槽位 70 CEventSlowRate; 阶段 9 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=3; ③ 粒子特效与坐标向量=4; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[160],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 6 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 8 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 7 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 7 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### パイルバンカーLv3 / PILEBUNKER03（原索引：BHE 210 / Mod wazSeq 067）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 55
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### インペリアルストライクLv3 / I_STRIKE03（原索引：BHE 213 / Mod wazSeq 068）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 56
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### プレッシャーLv3 / HNUCKLE03（原索引：BHE 216 / Mod wazSeq 069）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 57
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 65 CEventBlur; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=7; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 7 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 7 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### メタルハーベスターLv3 / MARUNOKO03（原索引：BHE 219 / Mod wazSeq 070）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 58
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=3; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[8],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[8],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[8],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[8],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[8],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[8],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### エクステンドアームLv3 / E_ARM03（原索引：BHE 222 / Mod wazSeq 071）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 59
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### チェーンソーLv3 / CHAINSAW2_03（原索引：BHE 225 / Mod wazSeq 072）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 60
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=4; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[6],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[6],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[6],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[6],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[6],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[6],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[7],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[7],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ウィングゲイザーLv3 / WING_G03（原索引：BHE 228 / Mod wazSeq 073）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 61
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 弾鉄圏Lv3 / DANTETSU03（原索引：BHE 237 / Mod wazSeq 076）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 62
- **涉及阶段与槽位**: 阶段 1 - 槽位 0 CEventSprite; 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 0 CEventSprite; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 0 CEventSprite; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 65 CEventBlur; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=3; ② 伤害与连段补正=16; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 0 CEventSprite (BHE #6 / Mod #6) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 0 CEventSprite (BHE #2 / Mod #2) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 3 - 槽位 0 CEventSprite (BHE #6 / Mod #6) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #5 / Mod #5) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #5 / Mod #5) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 19 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 19 项值，涉及 ① 闪退避让点、② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 仇討の刃Lv3 / ADAUTI03（原索引：BHE 243 / Mod wazSeq 078）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 63
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=3; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:184 (EFFECT / ◆刀の軌跡：ヒットマーク用 / 0144) | 0:184 (EFFECT / ◆回転 / SMOKE34) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 1 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[300],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[300],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[300],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[300],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 5 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 5 项值，涉及 ② 伤害与连段补正、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### スラッガーヒットLv3 / S_HIT03（原索引：BHE 246 / Mod wazSeq 079）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 64
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=7; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[70],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 7 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 7 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ★武器ダミーLv3 / W_DUMMY03（原索引：BHE 249 / Mod wazSeq 080）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 65
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ダブルサブマシンガンLv3 / MACHINEGUN03（原索引：BHE 253 / Mod wazSeq 081）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 66
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### エアリアルマシンガンLv3 / A_MACHINEGUN03（原索引：BHE 256 / Mod wazSeq 082）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 67
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ガトリングLv3 / GATRING03（原索引：BHE 259 / Mod wazSeq 083）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 68
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 1:81 (TAMA01 / ◆ガトリング：甲 / 0068) | 1:27 (TAMA01 / ガトリング：甲 / 0012) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 28 / InfoCollectionList #1` | {"int2":0,"intList3":[0,0],"intList4":[14,0],"paramList":[],"path":"ANGLE/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[13,0],"paramList":[],"path":"ANGLE/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量方向数据。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ライフルLv3 / RIFLE03（原索引：BHE 262 / Mod wazSeq 084）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 69
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #2` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/OVER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ジャベリンシューターLv3 / J_SHOOTER03（原索引：BHE 265 / Mod wazSeq 085）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 70
- **涉及阶段与槽位**: 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 徹甲弾Lv3 / TEKKOU03（原索引：BHE 268 / Mod wazSeq 086）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 71
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 拡散ショットLv3 / BEAM03（原索引：BHE 271 / Mod wazSeq 087）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 72
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ショットガンLv3 / SHOTGUN03（原索引：BHE 274 / Mod wazSeq 088）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 73
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 炸裂弾Lv3 / SAKURETU03（原索引：BHE 277 / Mod wazSeq 089）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 74
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### エネルギーボールLv3 / SPREADBOMB03（原索引：BHE 280 / Mod wazSeq 090）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 75
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### フォトンブラスターLv3 / FOTONBLASTER03（原索引：BHE 283 / Mod wazSeq 091）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 76
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### コントロールボムLv3 / CT_BOMB03（原索引：BHE 286 / Mod wazSeq 092）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 77
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/OBJECT -> OBJECT3/NULL","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/OBJECT -> OBJECT3/NULL","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ホーミングミサイルLv3 / H_MISSILE03（原索引：BHE 289 / Mod wazSeq 093）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 78
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### サインミサイルLv3 / S_MISSILE03（原索引：BHE 292 / Mod wazSeq 094）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 79
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### バズーカ砲Lv3 / BAZOOKA03（原索引：BHE 298 / Mod wazSeq 096）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 80
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### スラッシュバズーカLv3 / S_BAZOOKA03（原索引：BHE 301 / Mod wazSeq 097）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 81
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ＡＳスティンガーLv3 / STINGER03（原索引：BHE 304 / Mod wazSeq 098）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 82
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ＤＤミサイルLv3 / DD_MISSILE03（原索引：BHE 307 / Mod wazSeq 099）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 83
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ナパームミサイルLv3 / N_MISSILE03（原索引：BHE 310 / Mod wazSeq 100）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 84
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ダブルキャノンLv3 / D_CANNON03（原索引：BHE 316 / Mod wazSeq 102）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 85
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### スマッシャーキャノンLv3 / S_CANNON03（原索引：BHE 319 / Mod wazSeq 103）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 86
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 65 CEventBlur; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 65 CEventBlur; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 対空スプレッドキャノンLv3 / T_SPREADBOMB03（原索引：BHE 322 / Mod wazSeq 104）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 87
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ラインレーザーLv3 / LASER03（原索引：BHE 325 / Mod wazSeq 105）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 88
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### チェインライトニングLv3 / C_LIGHTNING03（原索引：BHE 328 / Mod wazSeq 106）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 89
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ホーミングレーザーLv3 / H_LASER03（原索引：BHE 331 / Mod wazSeq 107）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 90
- **涉及阶段与槽位**: 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=3; ⑤ 屏幕演出与慢动作=0
- **仅 BHE 存在的转换风险边界**: 阶段 1 - 原始 BHE 槽位 66: 2 个事件，帧范围 5..5, 71..71; 阶段 1 - 原始 BHE 槽位 67: 1 个事件，帧范围 8..8; 阶段 1 - 原始 BHE 槽位 68: 1 个事件，帧范围 5..5; 阶段 1 - 原始 BHE 槽位 69: 1 个事件，帧范围 5..5; 阶段 3 - 原始 BHE 槽位 69: 1 个事件，帧范围 0..0; 阶段 4 - 原始 BHE 槽位 66: 1 个事件，帧范围 0..0。这些节点在生产槽位映射中映射为 `-1`，因此默认转换基线与 Mod 均不包含它们；不能将其归因于朋友对 Mod 的修改。
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/MULTILOCK -> MULTILOCK/TARGETCOUNTER_MAX -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1","terminated":false,"warnings":["第 1 层的条目超出范围：9"]} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/MULTILOCK -> MULTILOCK/TARGETCOUNTER_MAX -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1","terminated":false,"warnings":["第 1 层的条目超出范围：9"]} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/MULTILOCK -> MULTILOCK/TARGETCOUNTER_MAX -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1","terminated":false,"warnings":["第 1 层的条目超出范围：9"]} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### パンドラミサイルLv3 / DD_MISSILE03（原索引：BHE 334 / Mod wazSeq 108）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 91
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### スタンフレアLv3 / STUN_F03（原索引：BHE 337 / Mod wazSeq 109）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 92
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 凶兆弾Lv3 / TYOUDAN03（原索引：BHE 340 / Mod wazSeq 110）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 93
- **涉及阶段与槽位**: 阶段 1 - 槽位 0 CEventSprite; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 0 CEventSprite; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=6; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 0 CEventSprite (BHE #10 / Mod #10) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 0 CEventSprite (BHE #11 / Mod #11) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 0 CEventSprite (BHE #12 / Mod #12) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 0 CEventSprite (BHE #6 / Mod #6) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 0 CEventSprite (BHE #7 / Mod #7) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 0 CEventSprite (BHE #8 / Mod #8) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 9 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 9 项值，涉及 ① 闪退避让点、② 伤害与连段补正、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### スライダーシュートLv3 / S_SHOOT03（原索引：BHE 343 / Mod wazSeq 111）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 94
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 0 CEventSprite; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=4; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 0 CEventSprite (BHE #1 / Mod #1) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 0 CEventSprite (BHE #2 / Mod #2) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 0 CEventSprite (BHE #3 / Mod #3) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 2 - 槽位 0 CEventSprite (BHE #4 / Mod #4) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[150],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 7 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 7 项值，涉及 ① 闪退避让点、② 伤害与连段补正、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### カタパルトミサイルLv3 / C_MISSILE03（原索引：BHE 346 / Mod wazSeq 112）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 95
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ハチロク砲Lv3 / TRAIN03（原索引：BHE 349 / Mod wazSeq 113）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 96
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=4; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[40],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[18],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[18],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:168 (EFFECT / ◆フラッシュ：電撃＋線火花 / HITMARK33) | 0:168 (EFFECT / ◆ガラドンナ：頭突き煙 / SMOKE19) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 波動砲Lv3 / HADOUHOU03（原索引：BHE 352 / Mod wazSeq 114）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 97
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=1; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ★射撃ダミーLv3 / S_DUMMY03（原索引：BHE 355 / Mod wazSeq 115）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 98
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 手榴弾Lv3 / GRENADE03（原索引：BHE 359 / Mod wazSeq 116）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 99
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 地雷Lv3 / MINE03（原索引：BHE 362 / Mod wazSeq 117）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 100
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ドラム缶Lv3 / DORAMUKAN03（原索引：BHE 365 / Mod wazSeq 118）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 101
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 煙玉Lv3 / KEMURIDAMA_S03（原索引：BHE 368 / Mod wazSeq 119）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 102
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### コンタクトボムLv3 / C_BOMB03（原索引：BHE 371 / Mod wazSeq 120）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 103
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 爆裂シュリケンLv3 / B_SYURIKEN03（原索引：BHE 374 / Mod wazSeq 121）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 104
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 0 CEventSprite; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=1; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 0 CEventSprite (BHE #2 / Mod #2) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/OVER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ① 闪退避让点、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### トマホークLv3 / TOMAHAWK03（原索引：BHE 377 / Mod wazSeq 122）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 105
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ラディカルスピナーLv3 / SPINAR03（原索引：BHE 380 / Mod wazSeq 123）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 106
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### チャクラムLv3 / THACRAM03（原索引：BHE 383 / Mod wazSeq 124）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 107
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 旅立つもふりんLv3 / I_MINE03（原索引：BHE 386 / Mod wazSeq 125）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 108
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 食すもふりんLv3 / EATER03（原索引：BHE 389 / Mod wazSeq 126）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 109
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 彷徨うもふりんLv3 / BOUND03（原索引：BHE 392 / Mod wazSeq 127）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 110
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 熱線照射装置Lv3 / NESSEN03（原索引：BHE 395 / Mod wazSeq 128）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 111
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 火炎放射器Lv3 / KAEN03（原索引：BHE 398 / Mod wazSeq 129）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 112
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 焼夷弾Lv3 / SYOUIDAN03（原索引：BHE 401 / Mod wazSeq 130）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 113
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ゲルマ＝グラビティLv3 / GELMA_G03（原索引：BHE 404 / Mod wazSeq 131）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 114
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ゲルマ＝ポイズンLv3 / GELMA_P03（原索引：BHE 407 / Mod wazSeq 132）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 115
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ゲルマ＝ショックLv3 / GELMA_S03（原索引：BHE 410 / Mod wazSeq 133）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 116
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### アタック・プロクターLv3 / A_PROCTER03（原索引：BHE 413 / Mod wazSeq 134）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 117
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=12; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #6 / Mod #6) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #6 / Mod #6) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 12 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 12 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ブースト・プロクターLv3 / B_PROCTER03（原索引：BHE 416 / Mod wazSeq 135）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 118
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ヒート・プロクターLv3 / H_PROCTER03（原索引：BHE 419 / Mod wazSeq 136）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 119
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ビームビットLv3 / G_BIT03（原索引：BHE 422 / Mod wazSeq 137）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 120
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ドレイドカノンLv3 / DL_CANNON03（原索引：BHE 425 / Mod wazSeq 138）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 121
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ドリルビットLv3 / D_BIT03（原索引：BHE 428 / Mod wazSeq 139）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 122
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ステルス爆撃機Lv3 / KUUBAKU03（原索引：BHE 431 / Mod wazSeq 140）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 123
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 対地強襲ヘリLv3 / H_RAIN03（原索引：BHE 434 / Mod wazSeq 141）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 124
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### サテライトレーザーLv3 / S_LASER03（原索引：BHE 437 / Mod wazSeq 142）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 125
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### パンツァー・レイドLv3 / TANK_BIT03（原索引：BHE 440 / Mod wazSeq 143）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 126
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=1; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 青龍逆鱗陣Lv3 / SEIRYUU03（原索引：BHE 443 / Mod wazSeq 144）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 127
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=4; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[10],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 5 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 5 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### クールゲインLv3 / C_GAIN03（原索引：BHE 449 / Mod wazSeq 146）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 128
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### フレアシーカーLv3 / E_MUSI03（原索引：BHE 452 / Mod wazSeq 147）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 129
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 雪月花Lv3 / SETUGEKKA03（原索引：BHE 455 / Mod wazSeq 148）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 130
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 65 CEventBlur; 阶段 1 - 槽位 69 CEventSlow; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=1; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **仅 BHE 存在的转换风险边界**: 阶段 1 - 原始 BHE 槽位 66: 1 个事件，帧范围 22..22; 阶段 1 - 原始 BHE 槽位 67: 1 个事件，帧范围 23..23; 阶段 1 - 原始 BHE 槽位 68: 1 个事件，帧范围 22..22; 阶段 1 - 原始 BHE 槽位 69: 1 个事件，帧范围 22..22; 阶段 2 - 原始 BHE 槽位 66: 1 个事件，帧范围 0..0; 阶段 2 - 原始 BHE 槽位 67: 1 个事件，帧范围 3..3; 阶段 2 - 原始 BHE 槽位 68: 1 个事件，帧范围 0..0; 阶段 2 - 原始 BHE 槽位 69: 1 个事件，帧范围 0..0; 阶段 3 - 原始 BHE 槽位 66: 1 个事件，帧范围 0..0。这些节点在生产槽位映射中映射为 `-1`，因此默认转换基线与 Mod 均不包含它们；不能将其归因于朋友对 Mod 的修改。
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ネコバットLv3 / NEKOBAT03（原索引：BHE 458 / Mod wazSeq 149）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 131
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### T.K.O / EX_KNUCKLE（原索引：BHE 606 / Mod wazSeq 151）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 132
- **涉及阶段与槽位**: 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 69 CEventSlow; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 59 CEventScreenScale; 阶段 8 - 槽位 60 CEventScreenYure; 阶段 8 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=7; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 7 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 7 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### デッドエンドストライカー / EX_KICK（原索引：BHE 607 / Mod wazSeq 152）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 133
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 65 CEventBlur; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=3; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[350],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[600],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[600],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 5:206 (TAMA05 / ◆だるま落とし：体当たり / 0171) | 5:0 (TAMA05 / 火炎放射 / 0007) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 5 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 5 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 貫壁 / EX_TACKLE（原索引：BHE 608 / Mod wazSeq 153）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 134
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 47 CEventCancel; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### メガブーストアッパー / EX_BOOSTUPPER（原索引：BHE 609 / Mod wazSeq 154）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 135
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=4; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[500],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[500],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### メガブーストキック / EX_BOOSTKICK（原索引：BHE 610 / Mod wazSeq 155）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 136
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[700],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[700],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 破軍煌帝脚 / EX_KAISERKICK（原索引：BHE 611 / Mod wazSeq 156）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 137
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 65 CEventBlur; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 60 CEventScreenYure; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 60 CEventScreenYure; 阶段 7 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[300],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 雷神電光烈閃掌 / EX_E_FIELD（原索引：BHE 613 / Mod wazSeq 158）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 138
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### クライマックスフィンガー / EX_E_FINGER（原索引：BHE 614 / Mod wazSeq 159）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 139
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 59 CEventScreenScale; 阶段 6 - 槽位 65 CEventBlur; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 41 CEventHit; 阶段 7 - 槽位 48 CEventEffect; 阶段 7 - 槽位 59 CEventScreenScale; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 41 CEventHit; 阶段 8 - 槽位 48 CEventEffect; 阶段 8 - 槽位 59 CEventScreenScale; 阶段 8 - 槽位 71 CEventChange; 阶段 9 - 槽位 41 CEventHit; 阶段 9 - 槽位 48 CEventEffect; 阶段 9 - 槽位 59 CEventScreenScale; 阶段 9 - 槽位 71 CEventChange; 阶段 10 - 槽位 41 CEventHit; 阶段 10 - 槽位 48 CEventEffect; 阶段 10 - 槽位 59 CEventScreenScale; 阶段 10 - 槽位 71 CEventChange; 阶段 11 - 槽位 41 CEventHit; 阶段 11 - 槽位 48 CEventEffect; 阶段 11 - 槽位 59 CEventScreenScale; 阶段 11 - 槽位 71 CEventChange; 阶段 12 - 槽位 41 CEventHit; 阶段 12 - 槽位 48 CEventEffect; 阶段 12 - 槽位 59 CEventScreenScale; 阶段 12 - 槽位 69 CEventSlow; 阶段 12 - 槽位 71 CEventChange; 阶段 13 - 槽位 47 CEventCancel; 阶段 13 - 槽位 60 CEventScreenYure; 阶段 13 - 槽位 69 CEventSlow; 阶段 13 - 槽位 70 CEventSlowRate; 阶段 13 - 槽位 71 CEventChange; 阶段 14 - 槽位 41 CEventHit; 阶段 14 - 槽位 48 CEventEffect; 阶段 14 - 槽位 60 CEventScreenYure; 阶段 14 - 槽位 65 CEventBlur; 阶段 14 - 槽位 69 CEventSlow; 阶段 14 - 槽位 70 CEventSlowRate; 阶段 14 - 槽位 71 CEventChange; 阶段 15 - 槽位 48 CEventEffect; 阶段 15 - 槽位 71 CEventChange; 阶段 16 - 槽位 48 CEventEffect; 阶段 16 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=12; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 7 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 8 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[75],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[75],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 9 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 10 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[115],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[115],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 11 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[130],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 12 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 14 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 14 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 12 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 12 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 武幻翔嵐舞 / EX_SWORD（原索引：BHE 615 / Mod wazSeq 160）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 140
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 69 CEventSlow; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 47 CEventCancel; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 60 CEventScreenYure; 阶段 7 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=4; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 16 / wazFileNo:wazSequenceNo（已解析）` | 0:184 (EFFECT / ◆刀の軌跡：ヒットマーク用 / 0144) | 0:3 (EFFECT / <范围内未命名条目>) | ② 伤害与连段补正: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### キリングレイジ / EX_KNIFE（原索引：BHE 616 / Mod wazSeq 161）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 141
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 65 CEventBlur; 阶段 1 - 槽位 69 CEventSlow; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=11; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[9,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[9,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int5` | <不存在> | 32 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 32 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 向量高度数据。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 12 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 12 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### グリガントランス / EX_LANCE（原索引：BHE 617 / Mod wazSeq 162）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 142
- **涉及阶段与槽位**: 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 48 CEventEffect; 阶段 6 - 槽位 65 CEventBlur; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=6; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #6 / Mod #6) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:165 (EFFECT / ◆ブーストダッシュ煙 / SMOKE24) | 0:165 (EFFECT / ◆電撃：縦（残像） / HIBANA25) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #7 / Mod #7) / Unit 3 / wazFileNo:wazSequenceNo（已解析）` | 0:165 (EFFECT / ◆ブーストダッシュ煙 / SMOKE24) | 0:165 (EFFECT / ◆電撃：縦（残像） / HIBANA25) | ③ 粒子特效与坐标向量: 解析索引后语义 WAZ 目标发生变化；纯供体/分组索引变化已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 8 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 8 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 死閃 / EX_IAI（原索引：BHE 618 / Mod wazSeq 163）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 143
- **涉及阶段与槽位**: 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 59 CEventScreenScale; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[50],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### デスペレイター / EX_SCYTHE（原索引：BHE 619 / Mod wazSeq 164）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 144
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 41 CEventHit; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 47 CEventCancel; 阶段 7 - 槽位 59 CEventScreenScale; 阶段 7 - 槽位 69 CEventSlow; 阶段 7 - 槽位 70 CEventSlowRate; 阶段 7 - 槽位 71 CEventChange; 阶段 8 - 槽位 60 CEventScreenYure; 阶段 8 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=6; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 4 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[30],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 6 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 6 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 6 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 朱血染のエッジレギオン / EX_EGGE（原索引：BHE 620 / Mod wazSeq 165）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 145
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 65 CEventBlur; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 59 CEventScreenScale; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[150],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ゴライアスハンマー / EX_HANMER（原索引：BHE 621 / Mod wazSeq 166）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 146
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 65 CEventBlur; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[800],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[800],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[800],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[800],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### インペリアルファイナル / EX_I_STRIKE（原索引：BHE 622 / Mod wazSeq 167）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 147
- **涉及阶段与槽位**: 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[1000],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[1000],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### イニシャライザ / INITIALIZER（原索引：BHE 664 / Mod wazSeq 187）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 148
- **涉及阶段与槽位**: 阶段 1 - 槽位 0 CEventSprite; 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 65 CEventBlur; 阶段 1 - 槽位 69 CEventSlow; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=4; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=3; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 0 CEventSprite (BHE #1 / Mod #1) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 0 CEventSprite (BHE #2 / Mod #2) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 0 CEventSprite (BHE #3 / Mod #3) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 0 CEventSprite (BHE #4 / Mod #4) / spmFileSequence（已解析）` | 12 (SELF / SOU / sou.spm) | 12 (CHINATSU2 / chinatsu.spm) | ① 闪退避让点: 解析后的 SPM 资源身份发生变化；语义相同的自身槽位重绑已过滤。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 7 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 7 项值，涉及 ① 闪退避让点、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 一閃 / SP_ISSEN（原索引：BHE 680 / Mod wazSeq 200）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 149, 150, 151, 152, 153
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 69 CEventSlow; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 69 CEventSlow; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 65 CEventBlur; 阶段 4 - 槽位 69 CEventSlow; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 65 CEventBlur; 阶段 5 - 槽位 69 CEventSlow; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 69 CEventSlow; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=57; ④ 动作相位衔接与取消窗口=27; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 30 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 30 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 30 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 30 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | <不存在> | 30 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 5 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / 事件存在性` | 存在（帧 7..14） | <不存在> | ④ 动作相位衔接与取消窗口: Mod 删除了这个可表示事件，完整窗口/引用路径不再执行。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / branchPhaseNo（原始 paramList[0]）` | 5 | <不存在> | ④ 动作相位衔接与取消窗口: 改变条件分支的原始阶段参数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / 条件 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON","terminated":true} | <不存在> | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / flag` | 1 | <不存在> | ④ 动作相位衔接与取消窗口: 改变 CEventChange 的 flag，从而改变编码的条件列表/分支结构。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / int1` | 0 | <不存在> | ④ 动作相位衔接与取消窗口: 精确行为控制值不同；数据可证明修改，但不能单独证明唯一运行时根因。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / 转移 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"JUMP/TRUN -> TURN/INPUT","terminated":true} | <不存在> | ④ 动作相位衔接与取消窗口: 改变解码后的 CEventChange 转移路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / endFrame` | 14 | <不存在> | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / 无) / startFrame` | 7 | <不存在> | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #2 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON2","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / 事件存在性` | 存在（帧 7..14） | <不存在> | ④ 动作相位衔接与取消窗口: Mod 删除了这个可表示事件，完整窗口/引用路径不再执行。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / branchPhaseNo（原始 paramList[0]）` | 5 | <不存在> | ④ 动作相位衔接与取消窗口: 改变条件分支的原始阶段参数。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / 条件 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON","terminated":true} | <不存在> | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / flag` | 1 | <不存在> | ④ 动作相位衔接与取消窗口: 改变 CEventChange 的 flag，从而改变编码的条件列表/分支结构。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / int1` | 0 | <不存在> | ④ 动作相位衔接与取消窗口: 精确行为控制值不同；数据可证明修改，但不能单独证明唯一运行时根因。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / 转移 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"JUMP/TRUN -> TURN/INPUT","terminated":true} | <不存在> | ④ 动作相位衔接与取消窗口: 改变解码后的 CEventChange 转移路径或操作数。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / endFrame` | 14 | <不存在> | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #1 / 无) / startFrame` | 7 | <不存在> | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #2 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON2","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / 事件存在性` | 存在（帧 7..12） | <不存在> | ④ 动作相位衔接与取消窗口: Mod 删除了这个可表示事件，完整窗口/引用路径不再执行。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / branchPhaseNo（原始 paramList[0]）` | 5 | <不存在> | ④ 动作相位衔接与取消窗口: 改变条件分支的原始阶段参数。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / 条件 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON","terminated":true} | <不存在> | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / flag` | 1 | <不存在> | ④ 动作相位衔接与取消窗口: 改变 CEventChange 的 flag，从而改变编码的条件列表/分支结构。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / int1` | 0 | <不存在> | ④ 动作相位衔接与取消窗口: 精确行为控制值不同；数据可证明修改，但不能单独证明唯一运行时根因。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / 转移 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[5],"path":"JUMP/TRUN -> TURN/INPUT","terminated":true} | <不存在> | ④ 动作相位衔接与取消窗口: 改变解码后的 CEventChange 转移路径或操作数。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / endFrame` | 12 | <不存在> | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / 无) / startFrame` | 7 | <不存在> | ④ 动作相位衔接与取消窗口: 改变该事件的精确激活/终止帧边界。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #2 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[],"path":"PARENT/OBJECT1 -> OBJECT1/INPUT -> INPUT/ATTACKKEY_ON2","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 84 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 84 项值，涉及 ③ 粒子特效与坐标向量、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

## tsukuyomi

- **MEK 选中的可执行技能数**: 8
- **含高价值差分的技能数**: 4
- **精确字段/节点差分行数**: 6
- **分类行数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=2; ⑤ 屏幕演出与慢动作=0

### ◆振り下ろし / 0014（原索引：BHE 011 / Mod wazSeq 011）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 0
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 60 CEventScreenYure; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[125],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[125],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ◆斬り上げ / CLOW05（原索引：BHE 012 / Mod wazSeq 012）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 1
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"NOKEZORITYPE/INPUT","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[135],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[135],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ② 伤害与连段补正、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ◆重力タックル / TACKLE05（原索引：BHE 013 / Mod wazSeq 013）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 2
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=1; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[50],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[1,0],"paramList":[50],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH_Z -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ◆重力球(引き寄せ) / 0015（原索引：BHE 014 / Mod wazSeq 014）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 3
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ◆重力球(落下) / 0016（原索引：BHE 015 / Mod wazSeq 015）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 4
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ◆グラビティフィールド / G_FIELD（原索引：BHE 016 / Mod wazSeq 016）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 5
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 60 CEventScreenYure; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=2; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,1],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,1],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 2 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 2 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### ◆グラビティガン（引力) / RIFLE03（原索引：BHE 017 / Mod wazSeq 017）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 6
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### ◆グラビティガン（斥力) / RIFLE04（原索引：BHE 018 / Mod wazSeq 018）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 7
- **涉及阶段与槽位**: 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

## misaki

- **MEK 选中的可执行技能数**: 20
- **含高价值差分的技能数**: 9
- **精确字段/节点差分行数**: 198
- **分类行数**: ① 闪退避让点=0; ② 伤害与连段补正=13; ③ 粒子特效与坐标向量=178; ④ 动作相位衔接与取消窗口=7; ⑤ 屏幕演出与慢动作=0

### 掴む / EGGE04（原索引：BHE 012 / Mod wazSeq 012）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 0
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[80],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 1 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 1 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 鉱石発射 / WARP_P1_08（原索引：BHE 013 / Mod wazSeq 013）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 1
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 抱きしめる / 0012（原索引：BHE 014 / Mod wazSeq 014）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 2
- **涉及阶段与槽位**: 阶段 1 - 槽位 41 CEventHit; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=4; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 2 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[60],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 4 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 4 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 上昇体当たり / M_UPPER04（原索引：BHE 015 / Mod wazSeq 015）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 3
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 60 CEventScreenYure; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=1; ③ 粒子特效与坐标向量=44; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / endFrame` | <不存在> | 53 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / endFrame` | <不存在> | 33 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | <不存在> | 30 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int5` | 0 | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / endFrame` | <不存在> | 1 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / int5` | <不存在> | 30 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #5 / Mod #5) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int5` | 0 | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 14 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 45 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 45 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 転移手：掴み / WARP_P1_06（原索引：BHE 016 / Mod wazSeq 016）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 4
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=3; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/OVER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/OVER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 3 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/OVER","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[0],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/OVER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 3 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 3 项值，涉及 ④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 転移手：前方ビターン / WARP_P1_11（原索引：BHE 017 / Mod wazSeq 017）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 5
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 転移手：後方ビターン / WARP_P1_09（原索引：BHE 018 / Mod wazSeq 018）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 6
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 転移手：全方位ビターン / WARP_P1_07（原索引：BHE 019 / Mod wazSeq 019）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 7
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 転移手：サーチビターン / WARP_P1_10（原索引：BHE 020 / Mod wazSeq 020）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 8
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 転移手：床召還 / G_BIT07（原索引：BHE 021 / Mod wazSeq 021）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 9
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 现有实机记录仅确认症状：实机释放时严重卡顿并瞬时掉帧。 该症状不作为字段因果证明；因果边界仍以本节表内的 Mod 数据差异为准。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### あたためる / G_BIT04（原索引：BHE 022 / Mod wazSeq 022）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 10
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 现有实机记录仅确认症状：实机命中时出现 999+ Combo Hit 并直接击破目标。 该症状不作为字段因果证明；因果边界仍以本节表内的 Mod 数据差异为准。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 本気：血水晶のツルギ / S_HAMMER06（原索引：BHE 023 / Mod wazSeq 023）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 11
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 47 CEventCancel; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 48 CEventEffect; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=51; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / endFrame` | <不存在> | 3 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 21 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / endFrame` | <不存在> | 8 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 10 / int5` | 0 | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置距离数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 4 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 51 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 51 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 本気：血水晶のカンオケ / 0013（原索引：BHE 024 / Mod wazSeq 024）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 12
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 41 CEventHit; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 41 CEventHit; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=5; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 2 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[20],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #2 / Mod #2) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #3 / Mod #3) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 41 CEventHit (BHE #4 / Mod #4) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[0],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 5 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 5 项值，涉及 ② 伤害与连段补正。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 本気：血水晶のオノ / BOOSTKICK04（原索引：BHE 025 / Mod wazSeq 025）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 13
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 70 CEventSlowRate; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 41 CEventHit; 阶段 4 - 槽位 47 CEventCancel; 阶段 4 - 槽位 70 CEventSlowRate; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 41 CEventHit; 阶段 5 - 槽位 48 CEventEffect; 阶段 5 - 槽位 60 CEventScreenYure; 阶段 5 - 槽位 65 CEventBlur; 阶段 5 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=2; ③ 粒子特效与坐标向量=40; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 4 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[100],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 5 - 槽位 41 CEventHit (BHE #1 / Mod #1) / Unit 6 / InfoCollectionList #1` | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"PARAM/VAL","terminated":true} | {"int2":0,"intList3":[],"intList4":[],"paramList":[200],"path":"ATTACK2/VAL","terminated":true} | ② 伤害与连段补正: 改变 CEventHit 攻击力节点的 Term 取值路径或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / endFrame` | <不存在> | 11 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 9 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / endFrame` | <不存在> | 11 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 3 - 槽位 48 CEventEffect (BHE #4 / Mod #4) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 42 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 42 项值，涉及 ② 伤害与连段补正、③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 本気：血水晶のヤリ / BEAM04（原索引：BHE 026 / Mod wazSeq 026）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 14
- **涉及阶段与槽位**: 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 本気：呪い：距離 / G_BIT05（原索引：BHE 027 / Mod wazSeq 027）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 15
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 本気：呪い：高度 / G_BIT10（原索引：BHE 028 / Mod wazSeq 028）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 16
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 本気：呪い：走行 / G_BIT06（原索引：BHE 029 / Mod wazSeq 029）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 17
- **涉及阶段与槽位**: 阶段 1 - 槽位 47 CEventCancel; 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=11; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 25 / InfoCollectionList #1` | {"int2":0,"intList3":[1,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[1,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变 CEventEffect 位置表达式数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[1,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[1,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / endFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 11 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 11 项值，涉及 ③ 粒子特效与坐标向量。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

### 運命の赤い糸 / G_BIT08（原索引：BHE 030 / Mod wazSeq 030）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 18
- **涉及阶段与槽位**: 阶段 1 - 槽位 59 CEventScreenScale; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 47 CEventCancel; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 59 CEventScreenScale; 阶段 2 - 槽位 65 CEventBlur; 阶段 2 - 槽位 70 CEventSlowRate; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 48 CEventEffect; 阶段 3 - 槽位 59 CEventScreenScale; 阶段 3 - 槽位 60 CEventScreenYure; 阶段 3 - 槽位 65 CEventBlur; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 59 CEventScreenScale; 阶段 4 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=0; ④ 动作相位衔接与取消窗口=0; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `限定高价值信号集合` | 与 Mod 基准数据语义等价 | 与 BHE 默认转换基线语义等价 | 五类限定信号中没有字段级修改；原始索引重绑已过滤。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 在五类限定信号以及语义化 WAZ/SPM 引用上与 Mod 基准数据等价；本数据集不能把独立行为差异归因于这些节点。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据保留了默认转换基线在限定信号上的语义；依据样本既有完整实测结论，该技能在完整 Mod 数据组合下正常完成且不闪退。

### 永遠の愛の誓い / G_BIT09（原索引：BHE 031 / Mod wazSeq 031）

#### 1. 语义节点数据差分（BHE 源数据与 Mod 基准数据）

- **Mod MEK 键**: 19
- **涉及阶段与槽位**: 阶段 1 - 槽位 48 CEventEffect; 阶段 1 - 槽位 70 CEventSlowRate; 阶段 1 - 槽位 71 CEventChange; 阶段 2 - 槽位 48 CEventEffect; 阶段 2 - 槽位 71 CEventChange; 阶段 3 - 槽位 71 CEventChange; 阶段 4 - 槽位 71 CEventChange; 阶段 5 - 槽位 59 CEventScreenScale; 阶段 5 - 槽位 70 CEventSlowRate; 阶段 5 - 槽位 71 CEventChange; 阶段 6 - 槽位 59 CEventScreenScale; 阶段 6 - 槽位 70 CEventSlowRate; 阶段 6 - 槽位 71 CEventChange; 阶段 7 - 槽位 71 CEventChange
- **五类差分计数**: ① 闪退避让点=0; ② 伤害与连段补正=0; ③ 粒子特效与坐标向量=32; ④ 动作相位衔接与取消窗口=4; ⑤ 屏幕演出与慢动作=0
- **关键字段数值差分表**:

| 字段名 | BHE 源数据 / 默认转换 | 朋友 Mod 基准数据 | 修改机理与语义含义 |
| :--- | :--- | :--- | :--- |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / endFrame` | <不存在> | 29 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 1 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / endFrame` | <不存在> | 150 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #1 / Mod #1) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / endFrame` | <不存在> | 150 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int1` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int2` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int3` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int4` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int5` | <不存在> | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / int6` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / startFrame` | <不存在> | 0 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #2 / Mod #2) / Unit 11 / 存在性` | <不存在> | 存在 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 7 / InfoCollectionList #1` | {"int2":0,"intList3":[0,19],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | {"int2":0,"intList3":[0,0],"intList4":[],"paramList":[],"path":"POS/OBJECT","terminated":true} | ③ 粒子特效与坐标向量: 改变解码后的发射锚点表达式或操作数。 |
| `阶段 2 - 槽位 48 CEventEffect (BHE #3 / Mod #3) / Unit 11 / int5` | 0 | 60 | ③ 粒子特效与坐标向量: 改变 CEventEffect 发射位置高度偏移数据。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #2` | {"int2":0,"intList3":[1,0],"intList4":[15,0],"paramList":[300],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[1,0],"intList4":[14,0],"paramList":[300],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/LENGTH -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 2 - 槽位 71 CEventChange (BHE #2 / Mod #2) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[1],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/EQUAL","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[],"paramList":[1],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/EQUAL","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 4 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[0,0],"paramList":[100],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/HEIGHT2 -> COMPARE/EQUALUNDER","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[0,0],"paramList":[100],"path":"PARENT/OBJECT1 -> OBJECT1/PARAM -> PARAMCOMP/HEIGHT2 -> COMPARE/EQUALUNDER","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |
| `阶段 5 - 槽位 71 CEventChange (BHE #1 / Mod #1) / 条件 #1` | {"int2":0,"intList3":[15,0],"intList4":[],"paramList":[1],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT1 -> COMPARE/EQUAL","terminated":true} | {"int2":0,"intList3":[14,0],"intList4":[],"paramList":[1],"path":"PARENT/OBJECT1 -> OBJECT1/0001 -> OBJECT4/PARAMCOUNT -> COMPARE/EQUAL","terminated":true} | ④ 动作相位衔接与取消窗口: 改变解码后的 Term 条件路径或操作数。 |

#### 2. 实际游戏战斗表现对比

- **原有默认转换在游戏里的表现**: 默认转换在 36 个限定字段/节点上不等于 Mod 基准数据。这些差异会改变对应的伤害、引用、窗口、分支或演出参数；未进行逐技能对照实测的字段不作唯一根因断言。
- **朋友 Mod 修改后的实机表现**: Mod 基准数据明确采用表中 36 项值，涉及 ③ 粒子特效与坐标向量、④ 动作相位衔接与取消窗口。依据样本既有完整实测结论，该技能在该数据组合下正常完成且不闪退；表内只陈述可复核的数据机制。

## 解释边界

- Mod 数据是本次比较的基准，但静态差分只能证明数据与语义引用发生变化，不能证明唯一的闪退调用栈。既有实机记录只作为症状保留。
- 解析后的 WAZ 资源身份相同时会过滤原始序号变化。分析器不会把每个外部弹药的内部载荷递归归因到所有调用技能；调用方差分行只证明它选择了哪个语义弹药或特效。
- 映射为 `-1` 的 BHE 顶层槽位只作为转换风险边界报告，不归类为朋友对 Mod 的修改，因为生产默认转换基线本就会丢弃这些节点。
- 报告覆盖每个 Mod MEK 选中的全部唯一 WAZ 序号。未被选中的辅助技能不属于本次交付物的可执行战斗范围。
