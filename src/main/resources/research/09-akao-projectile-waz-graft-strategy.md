# 09. AKAO 弹幕 WAZ 嫁接策略

## 目的

确认并固定 `AKAO` 移植里弹幕相关 WAZ 的处理规则。

这份文档只记录：

- `Effect / Tama01..05 / Bomb / Laser` 这些辅助 WAZ 的 BSDX/JINKI 差异
- 为什么不能 raw copy JINKI 辅助 WAZ
- 为什么不能整份覆盖 BSDX 同名 WAZ
- 当前采用的 key-based graft 规则
- 测试锁定点

## 结论

弹幕相关 WAZ 不能按整份文件覆盖。

正确策略是：

1. 递归收集 `Akao.waz` 间接引用到的辅助 WAZ。
2. 对辅助 WAZ 做 key-based merge：
   - BSDX 已有 key：复用 BSDX 槽位。
   - JINKI 非空且 BSDX 没有的 key：append 到 BSDX 对应 WAZ 尾部。
   - JINKI 空槽：不覆盖 BSDX。
3. 对所有输出 WAZ 内部的 `CEventWazaSelect` 做重绑：
   - `wazFileNo` 按 source WazaGroup index -> target WazaGroup index 映射。
   - `wazSequenceNo` 按 source skill index -> target skill index 映射。
4. `WazaGroup.param` 按 merge 后真实 skill count 回写。

## 文件级事实

### `bomb.spm`

`bomb.spm` 不是缺口。

| 文件 | 长度 | SHA256 |
|---|---:|---|
| BSDX `game/bsdx/spm/bomb.spm` | `99374` | `B10FEB708E71C75561053C3BE217BC0C1432915378B13C61E117AAF1E536AC5C` |
| JINKI `jinki_resources/bomb.spm` | `99374` | `B10FEB708E71C75561053C3BE217BC0C1432915378B13C61E117AAF1E536AC5C` |

两边二进制一致。

### `Bomb.waz`

`Bomb.waz` 是真实缺口。

| 文件 | skill count | 说明 |
|---|---:|---|
| BSDX `Bomb.waz` | `133` | 合法 skill index `0..132` |
| JINKI `Bomb.waz` | `136` | 合法 skill index `0..135` |

JINKI 尾部新增：

| index | key |
|---:|---|
| `133` | `0054` |
| `134` | `KINOKO38` |
| `135` | `KINOKO39` |

`Tama02 / Tama04 / Tama05` 会实际引用 `Bomb.waz[133..135]`，所以只复用 BSDX 原生 `Bomb.waz` 不够。

## WazaGroup 映射

当前 pipeline 以 `src/main/resources/game/bsdx/grp` 解析出的 BSDX 基线为准。

在这份基线里，弹幕相关 WazaGroup 映射已经能保持同源序号：

| source WazaGroup | target WazaGroup | 说明 |
|---:|---:|---|
| `0` | `0` | `Effect.waz` |
| `1` | `1` | `Tama01.waz` |
| `2` | `2` | `Tama02.waz` |
| `3` | `3` | `Tama03.waz` |
| `4` | `4` | `Tama04.waz` |
| `5` | `5` | `Tama05.waz` |
| `6` | `6` | `Laser.waz` |
| `7` | `7` | `Bomb.waz` |

旧 JSON 里看到的 `BSDX BOMB=5` 不是当前 pipeline 实际使用的基线。

## 同 key 不同槽位

同 key 在 BSDX/JINKI 两边都非空存在、但槽位不同的项目只有 4 个：

| 文件 | key | BSDX index | JINKI index | 是否被 JINKI WAZ 引用 |
|---|---|---:|---:|---|
| `Tama01.waz` | `SCRIPT_GUN02` | `74` | `75` | 否 |
| `Tama01.waz` | `SCRIPT_RAILGUN` | `77` | `78` | 否 |
| `Tama02.waz` | `KATYUSHA` | `95` | `96` | 否 |
| `Tama02.waz` | `KATYUSHA02` | `96` | `97` | 否 |

这些 key 不构成 AKAO 当前弹幕链的优先缺口。

## 递归收集缺口

`Akao.waz` 第一层引用只会拉到：

- `Effect.waz`
- `Tama01.waz`
- `Tama02.waz`
- `Tama03.waz`
- `Tama04.waz`
- `Tama05.waz`
- `Laser.waz`

但 `Tama01..05.waz` 内部继续引用 `Bomb.waz`。

JINKI 侧引用统计：

| 文件 | `wazFileNo=7` 最大 `wazSequenceNo` |
|---|---:|
| `Tama01.waz` | `4` |
| `Tama02.waz` | `134` |
| `Tama03.waz` | `36` |
| `Tama04.waz` | `133` |
| `Tama05.waz` | `135` |

因此 `Bomb.waz` 必须通过递归引用闭包纳入输出。

## 实现落点

### Step 3: `BuildImportPlanStep`

`requiredWazFiles` 不再只收 `Akao.waz` 第一层引用。

现在按引用闭包递归遍历：

- `Akao.waz`
- 由它直接引用的辅助 WAZ
- 辅助 WAZ 内部继续引用到的 WAZ

因此 `Bomb.waz` 会进入：

- `requiredWazFiles`
- `grpAppendTargets`
- 输出包

### Step 4: `AppendGrpEntriesStep`

生成两类映射：

- source WazaGroup index -> target WazaGroup index
- source WAZ 内部 skill index -> target WAZ 内部 skill index

同时计算 merge 后的 target skill count，并写回 `WazaGroup.param`。

### Step 7 / Step 8: 辅助 WAZ 输出

辅助 WAZ 不再 raw copy。

输出时使用：

- BSDX 同名 WAZ 作为主体
- JINKI 非空新增 skill append 到尾部
- `CEventWazaSelect.wazFileNo` 重绑到目标 WazaGroup
- `CEventWazaSelect.wazSequenceNo` 重绑到 merge 后目标 skill index

## 测试锁定点

`TestJinki2BsdxRunner` 锁定：

- `requiredWazFiles` 包含 `bomb.waz`
- 输出目录包含 `bomb.waz`
- 输出 `Bomb.waz` skill count 为 `136`
- `Tama02.waz` 引用 `Bomb.waz[134]`
- `Tama04.waz` 引用 `Bomb.waz[133]`
- `Tama05.waz` 引用 `Bomb.waz[135]`
- `WazaGroup.param` 等于输出 WAZ 的真实 skill count

测试命令：

- `mvn -q -Dtest=TestJinki2BsdxRunner test`

结果：

- 通过

## 产物

最新产物：

- `src/main/resources/out/jinki2bsdx_assets_20260411_194437_236`
- `src/main/resources/out/BaldrSky_20260411_194445_817.exe`
- `src/main/resources/out/Update3.pac`

输出 WAZ：

| 文件 | skill count |
|---|---:|
| `effect.waz` | `153` |
| `tama01.waz` | `79` |
| `tama02.waz` | `99` |
| `tama03.waz` | `47` |
| `tama04.waz` | `111` |
| `tama05.waz` | `152` |
| `bomb.waz` | `136` |

## 一句话总结

弹幕链的缺口不是 `bomb.spm`，而是辅助 WAZ 没有递归闭包、没有 key-based merge、也没有对二级 `CEventWazaSelect` 做 skill index 重绑。

现在的策略是：

- BSDX 同名 WAZ 保底
- JINKI 非空新增 skill append
- 所有 WAZ 引用按 source -> target group / skill 双层映射重写
