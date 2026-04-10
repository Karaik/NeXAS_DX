# 08. AKAO 战斗语音运行时对照调查

## 目的

确认一件事：

- 为什么原 BSDX 正常角色在战斗中可以播放 `AT / FC` 语音
- 而移植后的第 25 号位 `AKAO` 在同类场景下没有战斗语音

这份文档只写已经通过 24 号位正常角色与 25 号位 AKAO 的并排运行时对照确认下来的事实，不写猜测链。

## 对照对象

- 正常角色：BSDX 第 24 号位，对应 `SelectMekaMenu.dat row 23`
- 问题角色：移植后的第 25 号位 AKAO，对应 `SelectMekaMenu.dat row 24`

## 对照场景

- `AT` 语音
- `FC` 语音

## 对照范围

只看下面这条已经命中的运行时链：

- `sub_64FC60`
- `sub_60CDF0`
- `sub_60CC20`
- `sub_6111E0`
- `sub_610960`
- `sub_667F30`

`sub_60CEC0 / sub_65C020` 也在 probe 范围内，但在这次 `AT / FC` 样本里不是第一处分叉点。

## 取证产物

- `src/main/resources/tmp/voice_compare_runs_latest.json`
- `src/main/resources/tmp/baldrsky_voicecmp_20260411_003455/frida_voice_compare_slot24.log`
- `src/main/resources/tmp/baldrsky_voicecmp_20260411_003543/frida_voice_compare_slot25.log`
- `src/main/resources/tmp/baldrsky_voicecmp_slot24_at2_20260411_004004/frida_voice_compare_slot24_at2.log`

## 真实播放链

战斗语音不是菜单逻辑直接播放，而是走 request 队列：

1. `sub_64FC60`
   - 在战斗动作阶段挑一条候选语音
2. `sub_60CDF0`
   - 尝试把候选 request 写进 `this[1366]`
3. `sub_60CC20`
   - 作为 `sub_60CDF0` 的准入 gate
4. `sub_6111E0`
   - 每帧调 `sub_610960`
5. `sub_610960`
   - 消费 `this[1366]`
6. `sub_667F30`
   - 对这次样本里 `req[12] = -1` 的 request 直接做 BatVoice 播放

也就是说，这里真正决定 24 与 25 是否会说话的，不是菜单第三列，也不是 `MekVoiceInfo.table`，而是：

- `sub_64FC60` 是否能生成 request
- `sub_60CC20` 是否允许 `sub_60CDF0` 把 request 写进 `this[1366]`

## 24 号位样本

### 1. `AT` 语音

24 号位 `AT` 场景下，`sub_64FC60 -> sub_60CDF0 -> sub_60CC20` 会正常通过。

样本一：

- `sub_60CDF0(a2=0x9, a4=0x13, a5=0x1)`
- `sub_60CC20` 进入时：
  - `this[1577] = 26`
  - `retval != 0`
- request 写入后：
  - `+0 = 9`
  - `+4 = 1`
  - `+8 = 19`
  - `+12 = -1`
  - `+16 = 1`
  - `+20 = 0`
- 后续：
  - `sub_6111E0 -> sub_610960`
  - 进入 `sub_667F30`
- 最终 BatVoice key：
  - `group = 19`
  - `item = 1`

样本二：

- `sub_60CDF0(a2=0x9, a4=0x13, a5=0x5)`
- `this[1577] = 26`
- request 写入后：
  - `+8 = 19`
  - `+16 = 5`
- 最终 BatVoice key：
  - `group = 19`
  - `item = 5`

样本三：

- `sub_60CDF0(a2=0x8, a4=0x13, a5=0x8)`
- `this[1577] = 26`
- request 写入后：
  - `+8 = 19`
  - `+16 = 8`
- 最终 BatVoice key：
  - `group = 19`
  - `item = 8`

### 2. `FC` 语音

24 号位 `FC` 场景也走同一条链。

样本：

- `sub_60CDF0(a2=0x3, a4=0x13, a5=0x27)`
- `sub_60CC20` 进入时：
  - `this[1577] = 26`
  - `retval != 0`
- request 写入后：
  - `+0 = 3`
  - `+4 = 1`
  - `+8 = 19`
  - `+12 = -1`
  - `+16 = 39`
  - `+20 = 0`
- 后续：
  - `sub_6111E0 -> sub_610960 -> sub_667F30`
- 最终 BatVoice key：
  - `group = 19`
  - `item = 39`

## 25 号位 AKAO 样本

### 1. `AT` 语音

25 号位 AKAO 在 `AT` 场景下，`sub_64FC60` 也会走到 `sub_60CDF0`，而且候选 BatVoice key 已经是 AKAO 自己那组。

样本一：

- `sub_60CDF0(a2=0x3, a4=0x1e, a5=0x8)`
- 这里的候选 key 已经是：
  - `group = 30`
  - `item = 8`
- 但 `sub_60CC20` 进入时：
  - `this[1577] = 103`
  - `retval = 0`
- 所以 request 没有写入，仍然保持：
  - `+0 = -1`
  - `+4 = -1`
  - `+8 = -1`
  - `+12 = -1`
  - `+16 = -1`
  - `+20 = 0`

样本二：

- `sub_60CDF0(a2=0x8, a4=0x1e, a5=0x9)`
- 候选 key：
  - `group = 30`
  - `item = 9`
- `sub_60CC20` 进入时：
  - `this[1577] = 103`
  - `retval = 0`
- request 仍然全是 `-1`

### 2. `FC` 语音

25 号位 AKAO 的 `FC` 场景也走到同一条链，但仍然被同一个 gate 卡死。

样本：

- `sub_60CDF0(a2=0x3, a4=0x1e, a5=0x1a)`
- 候选 key：
  - `group = 30`
  - `item = 26`
- `sub_60CC20` 进入时：
  - `this[1577] = 103`
  - `retval = 0`
- request 仍然保持：
  - `+0 = -1`
  - `+4 = -1`
  - `+8 = -1`
  - `+12 = -1`
  - `+16 = -1`
  - `+20 = 0`

## 第一处分叉点

24 与 25 的第一处分叉点已经能钉死在：

- 函数：`sub_60CC20`
- 字段：`this[1577]`

并排实值：

| 场景 | 24 号位 | 25 号位 AKAO |
|---|---:|---:|
| `AT` gate 输入 | `this[1577] = 26` | `this[1577] = 103` |
| `AT` gate 返回 | 非 0 | `0` |
| `FC` gate 输入 | `this[1577] = 26` | `this[1577] = 103` |
| `FC` gate 返回 | 非 0 | `0` |

一旦 `sub_60CC20` 返回 `0`：

- `sub_60CDF0` 就不会写 request
- `this[1366]` 继续保持全 `-1`
- `sub_6111E0 -> sub_610960` 就没有可消费的 `AT / FC` 语音 request

所以 25 号位不是“最终 BatVoice 没有条目”，而是：

- **候选 BatVoice key 已经生成出来了**
- 但在 request 写入前就被 gate 掉了

## 这个字段从哪来

`this[1577]` 不是在语音函数里临时算出来的，而是机体初始化时直接写进去的。

链路是：

1. `sub_65B1E0`
   - 读取 `a2[11]`
2. `sub_61FA70`
   - `this[1577] = a3[11]`

对应伪代码关键行：

- `sub_65B1E0`：
  - `v6 = a2[11];`
- `sub_61FA70`：
  - `this[1577] = v13;`
  - 其中 `v13 = a3[11]`

而对于 AKAO，`103` 正是它的 `mekaId`。

## 为什么 24 正常而 25 不正常

关键不是资源文件有无，而是：

- 24 号位在这条战斗语音 gate 上，`this[1577] = 26`
- 25 号位 AKAO 在这条 gate 上，`this[1577] = 103`

`sub_60CC20` 的实现不是开放映射，而是：

- 先 `cmp eax, 0x1A`
- `eax > 0x1A` 直接走 default
- 之后再进 `0..0x1A` 范围内的 jump table

也就是说，这条战斗语音 gate 只认识 BSDX 原 roster 那组值。

`26 = 0x1A` 还能进表；`103 = 0x67` 在 jump table 之前就被排掉了。

## 已确认根因

根因不是：

- `BatVoice` 没有 AKAO 文件
- `MekVoiceInfo.table` 没引用
- `Akao.waz` 没有 `CEventVoice`

这些只能说明它们不是这条 `AT / FC` 路径。

已确认的根因是：

- **`AT / FC` 战斗语音 request 生成链，把 `mekaId` 直接复用成了 gate key**
- **BSDX 原生角色的 `mekaId` 落在 `sub_60CC20` 可识别范围内**
- **追加机体 AKAO 的 `mekaId = 103` 不在这个范围内**
- **所以 25 号位在 request 生成阶段就被挡住了**

## 对修复含义的约束

如果要修，真正需要碰的是：

- `sub_60CC20` 的 gate
  或
- `this[1577]` 的语义来源

而不是：

- 菜单第三列
- `MekVoiceInfo.table`
- `CEventVoice`

原因是：

- 这次对照已经确认 25 号位的候选 `group/item` 本身是正确的
- 差异发生在 request 写入之前

## 一句话总结

24 号位有战斗语音、25 号位 AKAO 没有战斗语音的第一处分叉点已经确认是：

- `sub_60CC20`
- `this[1577]`
- 24 的值是 `26`
- 25 的值是 `103`

25 号位不是不会选 BatVoice，而是候选 key 在 `sub_60CDF0` 写 request 前就被 `sub_60CC20` 按 `103` 拒绝了。

## 修复落点

这次没有去改 `mekaId = 103`，也没有去扩 `sub_60CC20` 的 switch / jump table。

采用的修复是：

- 改 `sub_60CDF0` 里的 gate 失败返回
- 改 `sub_60CEC0` 里的同类 gate 失败返回

位点一：

- `0x20C1FD`
- 原字节：`84 C0 75 06`
- 目标字节：`90 90 EB 06`

位点二：

- `0x20C2CD`
- 原字节：`84 C0 75 06`
- 目标字节：`90 90 EB 06`

对应含义：

- 原逻辑：
  - `sub_60CC20` 返回 `0`
  - `sub_60CDF0 / sub_60CEC0` 直接失败返回
  - request 不写入 `this[1366]`
- 修复后：
  - `sub_60CDF0 / sub_60CEC0` 忽略这一步的零返回
  - 继续执行后面的 request 写入逻辑
  - 直接型 request 和表驱动型 request 都不再被这道 gate 提前截断

## 为什么最终补两处 caller

`sub_60CC20` 有两个 caller：

- `sub_60CDF0`
- `sub_60CEC0`

这次 24 vs 25 的 `AT / FC` 对照里，第一处分叉点虽然发生在 `sub_60CC20`，但首先钉死的是：

- `sub_64FC60 -> sub_60CDF0 -> sub_60CC20`

这说明 `sub_60CDF0` 那一支必须放开。

补完 `0x20C1FD` 以后，`AT / FC` 已经恢复正常，也没有出现新的崩溃。

`sub_60CC20` 还有另一个 caller：

- `sub_60CEC0`

它写的是另一种 combat voice request 结构，同样会因为 `sub_60CC20` 返回 `0` 而提前失败。

为了不把行为只修到半条链，这里把两个 caller 的同型失败早退一起补掉：

- `0x20C1FD`
- `0x20C2CD`

## 代码与测试落点

代码落点：

- `src/main/java/com/giga/nexas/transfer/jinki2bsdx/steps/PatchExeCapacitiesStep.java`

测试落点：

- `src/test/java/com/giga/nexas/jinki/TestJinki2BsdxRunner.java`

测试锁定值：

- `0x20C1FD == 90 90 EB 06`
- `0x20C2CD == 90 90 EB 06`
