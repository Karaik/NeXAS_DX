# 01. BSDX 机体武装超过 153 后翻页闪退分析

## 起因

已观察到一个稳定现象：

1. 当单台 `mek` 的武装数量不超过 `153` 时，游戏可正常进入。
2. 当武装数量超过 `153` 时，游戏并不会在读取资源阶段立刻崩溃。
3. 只有在进入游戏后，把武装页面翻到那个“超过 153 的武装位置”时，游戏才会闪退。

这个现象说明：

- 问题不像是 `.mek` 文件头、块偏移、字符串、计数字段本身写坏。
- 更像是“运行时 UI / 菜单 / 武装列表”在访问超出某个固定容量的槽位时越界。

## 经过

本次排查按下面的顺序进行。

### 1. 先排除 Java 工具链的文件格式限制

先看仓库里的 Java 解析与生成逻辑：

- `src/main/java/com/giga/nexas/dto/bsdx/mek/parser/MekParser.java`
- `src/main/java/com/giga/nexas/dto/bsdx/mek/generator/MekGenerator.java`

结论：

- `MekParser` 读取武装块时，先读 `weaponCount`，然后按这个值循环读取。
- `MekGenerator` 写回武装块时，直接写 `weaponInfoMap.size()`。

也就是说：

- Java 侧 `.mek` 协议没有写死 `153`。
- 只从文件格式角度看，武装数量可以大于 `153`。

### 2. 再排除 `WazaGroup.grp` 自身条目数上限

接着追 `WazaGroup.grp` 的 exe 侧读取逻辑：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/639270.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/637670.c`

关键点：

- `sub_639270` 先从 `WazaGroup.grp` 读取文件头里的 count。
- 之后按 count 动态 `malloc` / push `CWazaGroup`。
- `read_WazaGroupgrp` 本身只是读取单个条目：
  - `existFlag`
  - `wazaName`
  - `wazaCodeName`
  - `wazaDisplayName`
  - `param`

结论：

- `WazaGroup.grp` 的读取逻辑不是固定 `153`。
- 因此 `153` 不是 `WazaGroup.grp` 文件协议的硬上限。

### 3. 顺着 exe 里的 `153 / 0x99` 常量继续追

在 BSDX 反汇编中搜索 `153` 和 `0x99` 后，发现最关键的命中点是：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/4576F0.c`
- 其启动入口在 `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/7D6BF0.c`

`7D6BF0` 很简单：

- 游戏启动时直接调用 `sub_4576F0(dword_870CA8)`。

`4576F0` 里出现了几组非常重要的固定容量：

- `sub_45B260(..., 103, ...)`
- `sub_45B400(..., 372, ...)`
- `sub_45B720(..., 153, ...)`
- `ensureStructArrayCapacity(..., 0x99u, 0)`
- `sub_45B330(..., 153, ...)`

这几组数字不是孤立的。

当前真实 BSDX 数据里：

- `MekaGroup.grp = 103`
- `MapGroup.grp = 372`
- `ProgramMaterial / 武装相关表 = 153`

也就是说：

- `4576F0` 明显在初始化一组“和游戏资源总表数量一致”的运行时结构。
- 同一个函数里，`103` 与 `372` 都能和真实 `.grp` 数量精确对上。
- 那么同级出现的 `153`，就极大概率也是一组“武装相关运行时数组”的固定容量，而不是普通业务值。

### 4. 确认 `4576F0` 所属对象是 `CExtraMode`

继续往下追对象类型，定位到：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/779E40.c`

这里有一行非常关键：

- `*a1 = &CExtraMode::\`vftable';`

并且这整个对象里大量出现：

- `a1[153]`
- `a1 + 153`
- 菜单状态
- 选择状态
- 各种资源页签相关字段

结论：

- `4576F0` 初始化出来的是 `CExtraMode` 这类“额外模式 / 菜单模式 / 武装页模式”的运行时数据。
- 因此 `153` 更像是菜单系统里的武装槽位上限，而不是单纯的文件加载上限。

### 5. 新线索把问题从“加载阶段”进一步收缩到“翻页/选中阶段”

用户补充的症状是：

- 不是一进游戏就崩。
- 而是进入游戏后，把页面拉到那个超过的武装位置时才闪退。

这一步非常关键，因为它说明：

- 资源读取阶段已经通过。
- 真正崩溃发生在“武装页面需要访问那个位置”时。

因此后续排查重点转向：

- 页签切换
- 页面翻动
- 槽位转实际武装编号
- 实际武装编号驱动的并行表访问

### 6. 武装页确实是“页基址 + 槽位偏移 = 实际武装编号”

在下面几个函数里能看到武装页的索引换算：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/7A4200.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/7A5320.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/7A4E10.c`

关键逻辑：

- 页面可见槽位是 `76..85`，也就是一页 `10` 个槽。
- 实际武装编号按下面方式计算：
  - `index = dword_8773D4 + (slot - 76)`

还有另外几套模式基址：

- `dword_877500`
- `dword_8776E4`
- `dword_8776E8`

这说明：

- 菜单 UI 并不是“直接拿当前页第 N 项对象”。
- 它是先维护一个“当前页基址”，再加上当前格子偏移，算出真实武装编号。

### 7. 算出编号后，UI 会直接访问并行状态数组

在 `7A5320.c` 与 `495D30.c` 中能看到一组很关键的数组访问：

- `dword_8771B0[base + offset]`
- `dword_877508[base + offset]`
- `dword_8776F4[base + offset]`
- `dword_8778D0[base + offset]`

尤其是：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/495D30.c`

里面明确写了：

- 当前模式为 0 时，检查 `dword_8771B0[dword_8773D4 + currentOffset]`
- 当前模式为 1 时，检查 `dword_877508[dword_877500 + currentOffset]`
- 当前模式为 2 时，检查 `dword_8776F4[dword_8776E4 + currentOffset]`
- 当前模式为 3 时，检查 `dword_8778D0[dword_8776E8 + currentOffset]`

这说明：

- 一旦菜单计算出实际武装编号，它会立刻拿这个编号去打到并行状态表里。
- 如果这些并行表只初始化到 `153`，那么访问 `154` 及之后的位置就非常危险。

### 8. 同一套 UI 里还存在其他硬编码阈值

在：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/7A4E10.c`

可以看到另一个很直接的限制：

- 对某条分支，若 `v4 >= 99`，直接弹提示：
  - `このデータの保護は解除できません`

这个细节虽然不是 `153` 本身，但它证明了一点：

- 这套武装页 UI / ExtraMode 逻辑里，本来就充满了固定阈值与硬编码容量。
- 因此 `153` 作为固定上限，不是异常现象，而是和这套系统风格一致。

## 结果

综合反汇编与现象，可以得到下面几个结果：

1. `.mek` 文件格式没有 `153` 上限。
2. `WazaGroup.grp` 文件读取也没有 `153` 上限。
3. `153` 出现在 `CExtraMode` 初始化中，并和 `103`、`372` 一样，表现为运行时固定容量。
4. 武装页 UI 会把“页基址 + 槽位偏移”换算成真实武装编号。
5. 换算出的真实武装编号会直接驱动多个并行状态数组访问。
6. 因为崩溃发生在“翻到超过 153 的武装位置”时，而不是启动时，所以更符合“渲染/选中时越界访问”的模式。

## 结论

当前最强结论是：

- **机体武装超过 153 后闪退，不是 `.mek` 文件协议崩，而是 BSDX exe 内部 `CExtraMode` 武装页相关运行时数组只按 `153` 个槽初始化。**

更具体地说：

1. 游戏能先读入你的资源，所以资源文件本身通常没有立刻损坏。
2. 但 UI 翻页到第 `154` 个及以后武装时，会把这个位置换算成真实武装编号。
3. 之后菜单系统会用这个编号去访问 `dword_8771B0 / dword_877508 / dword_8776F4 / dword_8778D0` 之类的并行数组。
4. 这些数组与 `4576F0.c` 中的 `153 / 0x99` 固定容量初始化高度相关。
5. 因此最终崩溃原因，极大概率是：
   - 越界访问
   - 未初始化槽位访问
   - 或依赖同一武装编号的并行结构没有同步扩容

## 当前仍未完全钉死的点

虽然链路已经很清楚，但还没有单点钉死到“哪一条具体指令发生越界访问”。

当前还差的最后一步是：

- 在 `CExtraMode` 武装页渲染链路里，把 `153` 容量对应的具体内存区和 `dword_8771B0` 等全局数组的真正定义位置精确对上。

也就是说：

- 根因层面已经足够确认是“UI 运行时硬上限”。
- 但若要做 exe 补丁，还需要继续追到这些数组的定义和分配点，把所有相关并行表一起扩容。

## 当前可执行建议

### 若不改 exe

- 单台机体武装数不要超过 `153`

### 若要继续逆向补丁

下一步建议继续追：

1. `4576F0.c`
   - `sub_45B720(153, ...)`
   - `ensureStructArrayCapacity(..., 0x99u, 0)`
   - `sub_45B330(153, ...)`
2. `7A4200.c`
   - 页槽位 `76..85` 到实际武装编号的换算
3. `7A5320.c`
   - 选中武装后的逻辑分发
4. `495D30.c`
   - 使用 `base + offset` 访问并行状态数组的点

如果这些位置继续串起来，就能定位最小的 exe 扩容补丁范围。
