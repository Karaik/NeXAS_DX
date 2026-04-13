# 10. SurvivalMeka DAT 运行时逻辑分析

## 一、结论摘要

当前 AKAO 追加后不能在生存模式出现，第一层原因不是 MEK/WAZ/SPM 主链，也不是 EXE 机体容量 patch，而是：

- 生存模式运行时会直接加载 `SurvivalMekaMain.dat`、`SurvivalMekaZako.dat`、`SurvivalMekaRare.dat`。
- 这三张表的第 0 列会进入最终波次生成项，作为实际生成的 meka id。
- 当前 BSDX baseline 三张表都不包含 AKAO 追加后的 target meka id `103`。
- 当前旧 pipeline 和 V2 输出目录都没有覆盖这三张 survival DAT。

所以即使主线已经把 AKAO 追加到 `MekaGroup` index `103`，生存模式候选池仍然只认识原始 BSDX 表里的机体 id，自然抽不到 AKAO。

## 二、关联文件

### DAT JSON

- `src/main/resources/datBsdxJson/SurvivalMekaMain.dat.json`
- `src/main/resources/datBsdxJson/SurvivalMekaRare.dat.json`
- `src/main/resources/datBsdxJson/SurvivalMekaZako.dat.json`

### BSDX 反编译代码

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/602EF0.c`
  - 生存模式初始化，加载 survival DAT。
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/54BE10.c`
  - 表格 DAT 读取包装函数。
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/54B8C0.c`
  - 把 DAT 行读入 24-byte row record。
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/601CE0.c`
  - 建立候选选择规则。
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/601AE0.c`
  - 按第 4 列筛选候选行。
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/601EB0.c`
  - 从候选池挑选机体并写入实际波次生成项。
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/6033A0.c`
  - 使用实际波次生成项创建生存模式敌机。
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/6778C0.c`
  - 另一条进入 survival 数据初始化的调用路径。

### BHE 对照

- `src/main/resources/ida-reverse/bhe/export-for-ai/decompile/7159E0.c`

BHE 也加载同名 survival DAT，结构上和 BSDX 同源。后续 BHE 接入时，survival 表也应挂在“后置客制化 / 输出沉淀”阶段，而不是塞回通用 graft 主线。

## 三、三张表的基本结构

三张表都是 6 列整数：

```json
"columnCount": 6,
"columnTypes": [
  "IntegerNew",
  "IntegerNew",
  "IntegerNew",
  "IntegerNew",
  "IntegerNew",
  "IntegerNew"
]
```

运行时 `sub_54B8C0` 会把每行读成 6 个 4-byte 值，也就是一个 24-byte record。`sub_54B6D0` 追加 row 时也是按 24 bytes 拷贝。

因此三张表的每一行可以先按下面形式理解：

```text
[col0, col1, col2, col3, col4, col5]
```

当前数据概况：

| 文件 | 行数 | 第 0 列 id 范围 | 是否包含 103 |
| --- | ---: | --- | --- |
| `SurvivalMekaMain.dat` | 24 | `0..26`，但不是连续全集 | 否 |
| `SurvivalMekaRare.dat` | 34 | `2..100`，非连续 | 否 |
| `SurvivalMekaZako.dat` | 86 | `0..102`，非连续 | 否 |

其中 `SurvivalMekaRare.dat` 和 `SurvivalMekaZako.dat` 都有 id `32` 的 donor 行：

```text
SurvivalMekaRare: [32, -1, 0, 4, 10, 0]
SurvivalMekaZako: [32, -1, 0, 1, 4, 8]
```

`SurvivalMekaMain.dat` 没有 id `32`。

## 四、EXE 如何加载这三张表

### 1. 生存模式初始化入口

`sub_602EF0` 是当前最关键的入口。它会在需要扩展 survival stage 数据时加载固定文件：

```c
sub_401C10(&v29, v6, "SurvivalMekaMain.dat", 0x14);
sub_54BE10(this, dword_879A04, &v29, 0);

sub_401C10(&v29, v7, "SurvivalMekaZako.dat", 0x14);
sub_54BE10(this + 6, dword_879A04, &v29, 0);

sub_401C10(&v29, v8, "SurvivalMekaRare.dat", 0x14);
sub_54BE10(this + 12, dword_879A04, &v29, 0);
```

对应关系：

| 运行时位置 | 文件 | 表类型 |
| --- | --- | --- |
| `this` | `SurvivalMekaMain.dat` | Main |
| `this + 6` | `SurvivalMekaZako.dat` | Zako |
| `this + 12` | `SurvivalMekaRare.dat` | Rare |

这里的 `+6` / `+12` 是 `_DWORD*` 偏移，也就是每个表容器约 24 bytes。

### 2. DAT 读取包装

`sub_54BE10` 负责打开 DAT 并调用 `sub_54B8C0`：

```c
if ( sub_76C940(v7, a2, FileSizeHigh, 0, 0) )
{
  v6 = sub_54B8C0(this, v7, a4);
}
```

`sub_54B8C0` 会先读列定义，再逐行读数据。对于这些 survival meka 表，最终每行落成 6 个整数：

```c
switch ( *(Block + i) )
{
  case 1:
    File::read_string(v3, v16);
    v16 += 7;
    break;
  case 2:
    *v16++ = au_re_File::read_2(v3);
    break;
  case 3:
    *v16 = au_re_File::read_4(v3);
    v16 = (v16 + 1);
    break;
}
```

这些 JSON 里列类型都是 `IntegerNew`，运行时会进入 4-byte integer 的读取路径。

## 五、字段含义追踪

下面是按反编译链路得到的字段解释。标记说明：

- “确认”：反编译代码中能直接看到用途。
- “推断”：能看到传递位置，但具体游戏语义仍需要更多逆向或实机验证。
- “未知”：当前关键路径没有观察到明确消费。

### `col0`: meka id

状态：确认。

证据链：

1. `601EB0.c` 从候选行取：

```c
v105 = (... table row pointer ...);
v170 = *v105;
```

2. 随后调用：

```c
sub_500170((dword_8760A4 + v159 + 8), &v170);
sub_437910((dword_86CE04 + 4), *v105);
```

3. `6033A0.c` 后续使用生成项时：

```c
sub_5E1600(2, *v27, v27[1], v58 + 50, v60, 500, 90);
...
v45 = *v27;
*v46 += v27[2] + 1;
```

`*v27` 就是最终生成项里的第 0 个字段，来自 survival row 的 `col0`。它被当作实际 meka id 使用。

结论：

```text
col0 = 生存模式候选机体 id / 最终生成 meka id
```

AKAO 追加到 `MekaGroup` 后 target index 是 `103`。如果三张表没有 row[0] = 103，生存模式不会生成 AKAO。

### `col1`: 生成项附加参数 4

状态：确认传递，具体语义推断。

证据链：

`601EB0.c` 把它放到生成项的第 4 个字段：

```c
v174 = v105[1];
```

按栈上连续内存传入 `sub_500170` 后，生成项大致是：

```text
[col0, computedCost, col3 - 1, col2, col1]
```

`6033A0.c` 中第 4 个字段会在非负时写到生成出来的对象：

```c
if ( v27[4] >= 0 )
{
  v31[1628] = v27[4];
}
```

当前观察：

- Main/Zako 中常见 `-1 / 0 / 1`。
- Rare 中基本是 `-1` 或 `0`。

谨慎结论：

```text
col1 = 生成对象的某个附加模式/标志/配置 id，-1 表示不写入该字段
```

具体业务名仍需继续逆向 `v31[1628]` 的含义。

### `col2`: 生成项附加参数 3

状态：确认传递，具体语义推断。

证据链：

`601EB0.c`：

```c
v111 = v105[2];
v173 = v111;
```

`6033A0.c` 中第 3 个生成字段会参与：

```c
if ( v27[3] >= 0 )
{
  sub_660000(v31[1551], v27[3]);
}
...
sub_5EF790(..., v27[3], 0);
```

当前观察：

- Main 中存在 `0 / 1 / 2`。
- Rare 中存在 `0 / 1`。
- Zako 中存在 `0 / 1`。

谨慎结论：

```text
col2 = 生成对象的动作/类型/AI 相关附加参数之一
```

它会被直接写入最终生成项，不是筛选值。

### `col3`: 生成数量/强度相关参数，写入时减 1

状态：确认传递，具体业务名推断。

证据链：

`601EB0.c`：

```c
v110 = v105[3];
v172 = v110 - 1;
```

也就是说原始 DAT 第 3 列进入生成项时会减 1。

`6033A0.c` 中生成项第 2 个字段会控制循环次数和计数：

```c
for ( v38 = 0; ; ++v38 )
{
  if ( v38 >= v27[2] )
    break;
  ...
}
...
*v46 += v27[2] + 1;
this[67] += v27[2] + 1;
```

当前数据特征：

- Main: 全部为 `1`，写入后为 `0`。
- Zako: 全部为 `1`，写入后为 `0`。
- Rare: `3..10`，写入后为 `2..9`。

谨慎结论：

```text
col3 = 某种生成数量/重复生成/构成规模参数，进入生成项时使用 col3 - 1
```

Rare 的值明显更高，说明它不是简单布尔值。

### `col4`: 候选筛选用的阶段/强度阈值

状态：确认。

证据链：

`601AE0.c` 会读取每个表 row 的第 4 列：

```c
if ( *(row + 16) >= a2[7] )
{
  if ( *(row + 16) <= a2[8] )
  {
    v11[0] = a2[6];       // 表类型
    v11[1] = rowIndex;    // 行号
    v11[2] = *(row + 16); // col4
    sub_604F90(v11);
  }
}
```

由于每个 row 是 6 个 4-byte int，`row + 16` 正好是第 4 列。

后续 `601EB0.c` 中这个值会作为候选的预算/阈值参与判断：

```c
if ( v21[2] <= v152 )
```

结论：

```text
col4 = 生存模式候选筛选用的阶段/强度/预算阈值
```

它不会直接拷贝进最终生成项，但决定某一行什么时候能进入候选池，以及能否在当前预算下被选中。

### `col5`: 当前关键路径未确认消费

状态：未知。

当前在以下关键路径中没有看到明确使用：

- `602EF0.c`
- `601CE0.c`
- `601AE0.c`
- `601EB0.c`
- `6033A0.c`

数据特征：

- Main: `6..25`
- Rare: 全部 `0`
- Zako: 前半原生 zako 多为 `8`，后半 main-like id 行为 `6..22`

谨慎结论：

```text
col5 = 未确认字段，修改 survival 表时应优先复制 donor 行原值，不要自行发明
```

## 六、候选池构建逻辑

`sub_602EF0` 加载三张 meka 表后，会调用多次 `sub_601CE0` 建立候选规则：

```c
sub_601CE0(this, 0, 1, 8, 999, 3, 0, 1);
sub_601CE0(this, 0, 1, 0, 13, 2, 1, 0);
sub_601CE0(this, 1, 1, 0, 999, 3, 0, 0);
sub_601CE0(this, 1, 2, 0, 999, 3, 0, 0);
sub_601CE0(this, 2, 0, 18, 30, 5, 0, 0);
sub_601CE0(this, 3, 0, 0, 20, 2, 0, 1);
sub_601CE0(this, 3, 0, 2, 24, 2, 1, 0);
```

从 `601AE0.c` 可确认 `sub_601CE0` 的第 3 个参数对应 meka 表类型：

| 参数值 | 表 |
| ---: | --- |
| `0` | Main |
| `1` | Zako |
| `2` | Rare |

第 4 / 5 个参数对应 `col4` 的筛选范围：

```text
min <= row.col4 <= max
```

第 7 个参数是起始 row index。

第 8 个参数是行过滤间隔：

```c
v7 = a2[11];
if ( !v7 || !(rowIndex % (v7 + 1)) )
```

也就是说：

- `a8 = 0` 时不过滤间隔。
- `a8 = 1` 时只取 rowIndex 能被 2 整除的行。

这解释了为什么直接把 AKAO row append 到末尾时，要注意 row index 奇偶。若进入的是带间隔规则的候选组，追加位置会影响是否被该规则扫描到。

## 七、最终波次生成逻辑

`sub_601EB0` 会从候选池挑选行，并把 survival row 转成最终生成项。

关键转换：

```c
v105 = pointer_to_survival_meka_row;

v170 = v105[0];      // col0: meka id
Block = v106;        // 运行时计算出的 cost / budget 值
v172 = v105[3] - 1;  // col3 - 1
v173 = v105[2];      // col2
v174 = v105[1];      // col1

sub_500170((stageEntry + 8), &v170);
```

因此最终生成项可以理解为：

```text
[mekaId, computedCost, col3MinusOne, col2, col1]
```

注意：

- `col4` 用于候选筛选，不直接进入最终生成项。
- `col5` 当前关键路径未观察到进入最终生成项。

`6033A0.c` 后续会消费这个最终生成项：

```c
sub_5E1600(2, mekaId, computedCost, x, y, 500, 90);
...
if ( field4 >= 0 )
  object[1628] = field4;

if ( field3 >= 0 )
  sub_660000(object[1551], field3);

for (i = 0; i < col3MinusOne; i++)
  sub_5EF790(...);
```

这进一步确认：

- 第 0 列必须是有效 meka id。
- 第 3 列不只是显示字段，会影响后续生成数量/附加生成。
- 第 1 / 第 2 列会被写入对象或行为路径。

## 八、为什么当前 AKAO 不会出现

当前 AKAO graft 后：

- `GrpAppendPlan.mekaGroupIndex = 103`
- `MekaGroup.grp` 中已经追加 AKAO。
- `Meka.dat`、`MekaPilot.dat`、`SelectMekaMenu.dat` 已覆盖菜单相关入口。
- EXE 机体容量已从 103 扩到 104。

但 survival 入口不看这些菜单 DAT。它看的是：

```text
SurvivalMekaMain.dat
SurvivalMekaZako.dat
SurvivalMekaRare.dat
```

当前输出目录只生成：

```text
Meka.dat
MekaPilot.dat
SelectMekaMenu.dat
WeaponEquip.dat
```

没有覆盖任何 `SurvivalMeka*.dat`。

所以 survival 运行时仍然读原始 BSDX 表，原始表里没有 `103`，最终候选池也没有 `103`。

## 九、推荐修复方向

### 方案定位

这不属于通用 graft 主线，也不属于菜单 override。

它应该属于：

```text
后置客制化 / 输出沉淀
```

建议新增 survival sidecar DAT patch，而不是改旧 pipeline 或把规则塞进 `ImportStaticAssetsStepV2` 的主资源收集逻辑。

### 建议新增对象

建议后续新增：

- `SurvivalMekaOverrideSpec`
  - 声明是否启用 survival 表 patch。
  - 声明要写入哪些表。
  - 声明 donor id / donor row。
  - 声明是否进入 Main / Zako / Rare。

- `PatchSurvivalMekaDatStep`
  - 读取 baseline 三张 survival DAT。
  - 复制 donor 行。
  - 将第 0 列改为 target meka index。
  - 写出 patched `SurvivalMeka*.dat` 到 output root。
  - 记录 audit。

### donor 选择建议

当前可用 donor：

```text
SurvivalMekaRare.dat: [32, -1, 0, 4, 10, 0]
SurvivalMekaZako.dat: [32, -1, 0, 1, 4, 8]
```

如果只想让 AKAO “能在 survival 随机池出现”，可先考虑：

- 在 `SurvivalMekaZako.dat` 中复制 id `32` 的 row，改第 0 列为 `103`。
- 在 `SurvivalMekaRare.dat` 中复制 id `32` 的 row，改第 0 列为 `103`。

`SurvivalMekaMain.dat` 没有 id `32` donor。是否加入 Main 需要单独决定：

- 若加入 Main，需要人工选一个类似主力机体的 donor row。
- 若不加入 Main，AKAO 仍可通过 Zako/Rare 池出现。

### row 追加位置注意

由于 `sub_601AE0` 支持按 row index 间隔筛选：

```text
a8 = 1 时，只取 rowIndex % 2 == 0 的行
```

所以如果某个候选规则依赖奇偶过滤，append 到末尾可能影响被该规则扫描的机会。

后续实现时应审计：

- 目标表当前行数。
- 追加后 row index 的奇偶。
- 目标 row 的 `col4` 是否落在对应候选规则的筛选区间。

## 十、对最终 parity 的影响

如果 V2 默认新增 survival DAT 覆盖，那么它会让最终输出集合多出：

```text
SurvivalMekaMain.dat
SurvivalMekaZako.dat
SurvivalMekaRare.dat
```

这会打破当前“V2 和旧 pipeline 最终输出完全一致”的验收标准，因为旧 pipeline 当前没有输出这三张表。

因此后续实现时需要二选一：

1. 先改旧 pipeline 和 V2，同时更新最终 parity，让新旧都输出 survival DAT。
2. 保持旧 pipeline 不动，把 survival patch 作为 V2 新功能，并新增新的验收口径，不再拿旧 pipeline 作为这部分的 byte-identical 对照。

如果目标是“最终包真的能让 AKAO 出现在 survival”，那必须接受输出集合变化，或者同步升级旧 pipeline 基线。

## 十一、当前未确认点

- `col1` 写入对象字段 `v31[1628]` 的准确游戏语义。
- `col2` 进入 `sub_660000` 的准确含义。
- `col3 - 1` 对 Rare 高值行的准确生成效果。
- `col5` 是否在其他未追踪路径中被消费。
- 追加 row 的最佳位置是否需要保持某种排序，而不仅仅是 append。

这些不影响第一层结论：AKAO 不出现是因为 survival meka DAT 候选表没有包含 target meka id `103`。
