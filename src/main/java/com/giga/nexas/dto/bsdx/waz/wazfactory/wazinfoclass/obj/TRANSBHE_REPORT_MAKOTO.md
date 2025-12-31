# BHE -> BSDX WAZ transBhe 对比报告（Makoto 基准）

本文以 **makoto.waz** 为基准，对照 BHE/BSDX 的实际数据与 `transBhe` 转换逻辑，
逐一检查转换是否一致、是否存在潜在错误，并记录未验证/需补齐的部分。

---

## 1) 数据来源与基准

- BHE 数据：`src/main/resources/wazBheJson/makoto.waz.json`
- BSDX 数据：`src/main/resources/wazBsdxJson/Makoto.waz.json`
- 槽位映射：`src/test/java/com/giga/nexas/bhe2bsdx/steps/WazConverter.java` 中的 `bheToBsdxSlotMap()`

说明：Makoto 在 BHE/BSDX 都存在，用于比对**事件数量与结构**。  
报告不假定两边数据完全一致（不同引擎可能有差异），仅标注“不一致/风险”。

---

## 2) Makoto 基准统计（与 transBhe 相关的 CEvent）

Makoto 中出现的 `transBhe` 事件数量（BHE vs BSDX）：

- CEventEffect：BHE=56，BSDX=58
- CEventCharge：BHE=3，BSDX=3
- CEventCpuButton：BHE=5，BSDX=5
- CEventEscape：BHE=18，BSDX=18
- CEventHeight：BHE=3，BSDX=3
- CEventHit：BHE=10，BSDX=10
- CEventScreenLine：BHE=5，BSDX=5
- CEventBlink：Makoto 中未出现
- CEventRadialLine：Makoto 中未出现
- CEventStatus：Makoto 中未出现

槽位级别计数不一致（仅表示“Makoto 在两版本中本身就不一致”）：

- BHE slot 0 -> BSDX slot 0：362 vs 364  
- BHE slot 54 -> BSDX slot 48（CEventEffect）：56 vs 58  
- BHE slot 82 -> BSDX slot 71：73 vs 75  

---

## 3) transBhe 方法逐一对比（含 Makoto 证据）

### 3.1 CEventHeight
- 现状：已修正为按 `unitSlotNum` 查找 BHE 单元。
- Makoto 证据：BHE unitSlot 典型为 `[1,2]` 或 `[1]`（稀疏）。
- 结论：**原先 list.get(i) 会错位**，现已修正；这是之前崩溃的直接原因。
- 代码：`CEventHeight.transBheCEventHeightToBsdx`

### 3.2 CEventEscape
- 现状：已修正为按 `unitSlotNum` 查找 BHE 单元。
- Makoto 证据：BHE unitSlot 固定为 `[2,8]`（稀疏）。
- 结论：**原先 list.get(i) 会错位**，现已修正。
- 代码：`CEventEscape.transBheCEventEscapeToBsdx`

### 3.3 CEventScreenLine
- 现状：已修正为按 `unitSlotNum` 查找 BHE 单元。
- Makoto 证据：BHE unitSlot 固定为 `[3]`（稀疏）。
- 结论：**原先 list.get(i) 会错位**，现已修正。
- 代码：`CEventScreenLine.transBheCEventScreenLineToBsdx`

### 3.4 CEventRadialLine
- 现状：已修正为按 `unitSlotNum` 查找 BHE 单元。
- Makoto：未出现该事件，无法用 Makoto 验证。
- 结论：结构与 ScreenLine 类似，修正是合理的防错。
- 代码：`CEventRadialLine.transBheCEventRadialLineToBsdx`

### 3.5 CEventCharge
- 现状：已修正为按 `unitSlotNum` 查找 BHE 单元。
- Makoto：出现 3 次，但 unitSlot 在 Makoto 中不稀疏（[0,1]）。
- 结论：修正后更安全（避免其他角色稀疏时错位）。
- 代码：`CEventCharge.transBheCEventChargeToBsdx`

### 3.6 CEventStatus
- 现状：已修正为按 `unitSlotNum` 查找 BHE 单元。
- Makoto：未出现该事件，无法验证。
- 结论：BHE 在读入时只保存 buffer!=0 的单元，**稀疏是常态**，修正必要。
- 代码：`CEventStatus.transBheCEventStatusToBsdx`

### 3.7 CEventEffect
- 现状：使用 unitSlot 搜索 + SHIFT_FROM（BHE 多一个“標的”槽）。
- Makoto 证据：BHE unitSlot 典型为 `[3,4,6,7,16]`（稀疏）。
- 结论：实现方式正确；Makoto 计数差异（56 vs 58）属于两游戏数据差异，**非转换错误**。
- 代码：`CEventEffect.transBheCEventEffectToBsdx`

### 3.8 CEventHit
- 现状：使用 `bheToBsdxIdx` 映射 + unitSlot 搜索。
- Makoto 证据：unitSlot 稀疏（如 `[6,23,24,25,26,27,35,36]`）。
- 结论：索引映射正确，但字段映射很多（int1..int31）仍需人工复核。
- 代码：`CEventHit.transBheCEventHitToBsdx`

### 3.9 CEventCpuButton
- 现状：只处理 unitSlot=0，BHE 读取也只生成 slot=0。
- Makoto：出现 5 次，结构正常。
- 结论：当前实现可用；若出现空列表可考虑改为 `unitSlotNum` 搜索以防崩。
- 代码：`CEventCpuButton.transBheCEventCpuButtonToBsdx`

### 3.10 CEventBlink
- Makoto：未出现，无法验证。
- 现状：先 BeanUtil 复制 `int1/int2/int3/short1`，随后 `transBhe` 仅用 `int9` 覆盖 short1。
- 风险：**short1 的语义是否应来自 BHE.int9 未验证**，需换有该事件的角色校验。
- 代码：`CEventBlink.transBheCEventBlinkToBsdx`

---

## 4) 额外一致性检查（Makoto）

1) **被丢弃槽位（映射为 -1）**  
Makoto 的 BHE 在这些槽位 **均无事件**，因此转换未丢失数据。
（对其它角色仍可能丢失：23, 35, 38, 40, 43, 52, 62, 66~69）

2) **槽位映射与事件类型一致性**  
除 BHE slot37 -> BSDX slot35（CEventFreeParam -> CEventVal）外，其余映射的事件类型一致。
该特殊映射已在 `WazConverter` 中专门处理。

---

## 5) 当前确认的“问题/风险点”

1) **稀疏 unit 列表导致错位**  
已修正：Height/Escape/ScreenLine/RadialLine/Charge/Status  
仍需注意：所有类似结构都必须按 `unitSlotNum` 查找，不能按 list 索引。

2) **CEventBlink 的字段映射未验证**  
Makoto 无该事件，无法校验 `short1` 是否应来自 `int9`。

3) **CEventHit 的字段映射仍需实测**  
映射涉及多个 int 字段，建议挑一个 BSDX/BHE 同角色逐帧对比。

4) **skillInfoUnknownList 未迁移**  
若 BHE 某些事件依赖 unknown block，当前转换会丢失。

---

## 6) 建议的下一步

- 选取一个包含 **CEventBlink/CEventRadialLine/CEventStatus** 的角色，补齐校验。
- 针对 CEventHit 做“字段级”对比（可用 BHE/BSDX 同角色逐帧比对）。
- 如果发现某槽位在 BHE 中出现而映射为 -1，需要补充降级逻辑。

