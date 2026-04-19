# Initializer 共性审计与实现策略

本文档记录 BHE 单机体缺少 `イニシャライザ / INITIALIZER` 武装时，如何在 BSDX 目标侧补齐。
对应实现入口：`EnsureInitializerWeaponStep`，调用位置：`TsukuyomiGraftPipeline` 的 MEK/WAZ 重绑之后、静态资源写出之前。

## 语义

玩家侧语义以用户说明为准，并用外部资料交叉确认：

| 语义点 | 结论 |
|---|---|
| 武装类型 | 本质是 FC / initializer 类武装，不是普通攻击武装。 |
| 画面表现 | 启动后全屏暗化/“关灯”，并伴随慢动作、画面线/blur 等演出。 |
| 连段效果 | 启动后，initializer 前同一连段中已经使用过的武装可以再使用一次。 |
| 资源实现 | `MEK.weaponCategory = 2` 是关键语义字段；WAZ skill 主要承担启动演出和 change 事件。 |

外部资料只用于确认玩家语义，不作为二进制结构依据：
[`BALDR-A-HEAD / BALDR SKY`](https://seesaawiki.jp/baldrahead/d/BALDR%2520SKY)
对 BALDR SKY 的说明提到，initializer 后热量上限与连段继续规则发生变化，且使用前用过的位置攻击可以再使用。

## 数据范围

本次审计使用 2026-04-19 本仓库真实反序列化/二进制数据：

| 数据 | 来源 |
|---|---|
| BSDX MEK | `src/main/resources/game/bsdx/mek/*.mek` |
| BSDX WAZ | `src/main/resources/game/bsdx/waz/*.waz` |
| BHE / JINKI | 用于确认“部分机体缺 initializer”这个迁移场景，但模板实现以 BSDX 目标侧为准。 |

## MEK 武装行共性

BSDX 中共找到 69 个 initializer 武装行。字段共性如下：

| 字段 | 共性 | 备注 |
|---|---|---|
| `weaponName` | `イニシャライザ` | 全部一致。 |
| `weaponSequence` | `INITIALIZER` | 全部一致。 |
| `weaponCategory` | `2` | 核心语义字段，表示 initializer 类武装。 |
| `weaponType` | `0` | 全部一致。 |
| `weaponIdentifier` | `0` | 全部一致。 |
| `weaponUnknownProperty19` | `1` | 全部一致。 |
| `forceCrashAmount` | 多数为 `3` | 58 / 69 为 `3`，少数可操作机体为 `0/2`。 |
| `heatMaxConsumption` | 多数为 `80` | 58 / 69 为 `80`，少数可操作机体为 `0/1`。 |
| `heatMinConsumption` | `0` | 全部一致。 |
| `startPointWhenDemonstrate` | 多数为 `100` | 58 / 69 为 `100`，少数为 `50`。 |

当前补齐策略使用 `Zako116a.mek` 的 initializer 武装行作为模板：

| 字段 | 值 |
|---|---|
| `forceCrashAmount` | `3` |
| `heatMaxConsumption / heatMinConsumption` | `80 / 0` |
| `startPointWhenDemonstrate` | `100` |
| `weaponCategory / weaponType` | `2 / 0` |
| `weaponIdentifier / weaponUnknownProperty19` | `0 / 1` |

选择理由：这是 BSDX 原生敌机侧最常见的 initializer 形态，符合 Tsukuyomi/Yuri 这类 BHE 迁移机体作为敌机/额外机体接入时的默认语义。

## WAZ skill 共性

所有 BSDX initializer skill 都有相同的“骨架”，差异主要在 `unit 0` 的角色 sprite 数量，以及部分机体是否带 voice。

| unit | 共性事件 | 迁移策略 |
|---|---|---|
| `0` | `CEventSprite...`，数量随机体不同 | 机体专属，不能跨机体原样复制；替换为当前机体 STAND/0 号 skill 的 sprite。 |
| `6` | `CEventSpriteYure` | 保留，属于通用抖动演出。 |
| `10` | `CEventValRandom` | 保留。 |
| `19` | `CEventValRandom` | 保留。 |
| `21` | `CEventValRandom` | 保留。 |
| `24/26/27/28/30` | `CEventVal + CEventVal` | 保留。 |
| `36/38/47/68/70` | `CEventVal` | 保留。 |
| `52` | `CEventSe + CEventSe` | 清空；模板 SE 是机体/资源专属，跨机体会污染。 |
| `53` | `CEventVoice`，仅部分机体存在 | 清空；不能套模板角色语音。 |
| `59` | `CEventValRandom + CEventValRandom` | 保留，和画面缩放/演出相关。 |
| `62` | `CEventScreenEffect` | 保留，全屏暗化/画面演出的核心候选。 |
| `63` | `CEventScreenLine` | 保留。 |
| `65` | `CEventBlur` | 保留。 |
| `69` | `CEventVal + CEventVal`，部分签名存在 | 模板里有则保留；不存在也不强造。 |
| `71` | `CEventChange` | 保留，initializer 行为切换/重置的核心候选。 |

## 为什么不能原样复制外部 initializer WAZ

动态验证记录：

| 尝试 | 结果 | 结论 |
|---|---|---|
| BHE `zako116a.waz` 转换后追加 | 运行时 `std::length_error("vector<T> too long")` | BHE 事件结构不能直接塞给 BSDX。 |
| BSDX `Zako116a.waz` initializer 原样追加 | hover initializer 时崩溃 | BSDX 原生 skill 也带模板机体专属 sprite/SE/voice/material，不能跨机体原样复制。 |
| 指向当前机体 STAND/0 号 skill | 不崩，但语义不完整 | 只能作为排障 fallback，不能作为最终实现。 |
| 清洗版 initializer skill | 保留共性 screen/change/blur，替换/清空专属资源 | 当前实现策略。 |

## Material 策略

MEK 尾部 `MekMaterialBlock.trailingEntries` 与武装表按同位顺序消费。
新增 initializer 武装时必须追加一个 material trailing entry，否则武装菜单/演示会读取错位。

| 方案 | 结论 |
|---|---|
| 不追加 material | 结构不自洽，hover/演示风险高。 |
| 复制 `Zako116a` 同位 material | 会把大量 Zako 专属 sprite/se/voice 资源链挂入 Tsukuyomi，动态仍崩。 |
| 复制当前机体已有 material | 当前策略。它只保证菜单/演示素材表同位自洽，不引入跨机体资源。 |

## 当前实现约束

| 约束 | 原因 |
|---|---|
| 只在 MEK/WAZ 重绑后执行 | 避免补齐内容污染 BHE 源侧资源闭包。 |
| 新增 WAZ skill 后必须同步 `WazaGroup.param` | 引擎按该值理解 WAZ skill 数量。 |
| 不复用模板 SE/Voice | 角色语音和 SE 链属于当前机体/资源簇，不允许隐式套用。 |
| 允许本包内保留模板路径魔法值 | 用户已明确允许，但必须记录来源和原因。 |
