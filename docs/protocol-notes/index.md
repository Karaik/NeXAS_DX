# 引擎协议笔记

> 从逆向分析 / 动态调试 / 实测验证得到的 grp / mek / waz / dat 等文件格式的索引语义与结构约定。
> 这些约定不被 Parser/Generator 直接执行，但决定了 pipeline 中数据关联的正确性。

---

## ProgramMaterial.grp — 三维数组的下标语义

`ProgramMaterial.grp` 的外层有三个**等长数组**（长度 = 机体数量，即 `MekaGroupGrp.mekaList.size()`），按 `mekaIndex` 索引。

| 外层数组 | 子数组元素值 = grp 下标 | 引用的 grp 对象 |
|---------|------------------------|----------------|
| **array1** | `spriteGroupIndex` | `SpriteGroup.grp.spriteList[value]` |
| **array2** | `seGroupIndex` | `SeGroup.grp.seList[value]` |
| **array3** | `batVoiceIndex` | `BatVoice.grp.voiceList[value]` |

**核心约定：下标含义继承自对应 grp 对象的内部递归。**

即引擎拿到某个下标值后，直接用它作为对应 grp 内部 list 的索引去取条目：
- array1[mekaIndex] 里存的值 N → `SpriteGroup.grp.spriteList[N]` 取出 SpriteGroupEntry
- array2[mekaIndex] 里存的值 N → `SeGroup.grp.seList[N]` 取出 SeGroupGroup
- array3[mekaIndex] 里存的值 N → `BatVoice.grp.voiceList[N]` 取出 BatVoiceGroup

所有下标都是 0-based，直接对应 grp 内部 list 的序号。

### Pipeline 影响

追加条目到 grp 末尾时（如 SpriteGroup 138→139），**已有条目的下标不变**，所以 ProgramMaterial 里引用旧下标的值不需要重映射。
但如果对 grp 做了**插入/删除/重排**，则 ProgramMaterial 里的下标值必须同步更新，否则引擎会取到错误的条目。

---

## CMaterial（.mek 尾部 MekMaterialBlock）— 三段组数必须与 grp 对齐

每个 `.mek` 文件尾部的 `MekMaterialBlock` 包含若干 `PluginEntry`，每条 entry 有三段组列表：

| 段 | 组列表 | 组数必须等于 |
|----|--------|------------|
| spriteGroups | `List<int[]>` | `SpriteGroup.grp.spriteList.size()` |
| seGroups | `List<int[]>` | `SeGroup.grp.seList.size()` |
| voiceGroups | `List<int[]>` | `BatVoice.grp.voiceList.size()` |

引擎加载任意机体时，读其 .mek 的 CMaterial，用三段组数去和对应 grp 的 list size 对齐做内存分配。
**如果组数对不上，分配会得到 -1，直接崩溃。**

### Pipeline 影响

当 grp 追加了新条目（如 SpriteGroup 138→139, BatVoice 30→31），**所有机体的 .mek** 的 CMaterial 组数都必须同步 padding 到新值。
这不是只改当前被移植机体就行——基线里每个 .mek 都受影响，因为引擎打开任何机体都会读它的 CMaterial。

---

## MapGroup.grp — 每个 entry 的内部数组也必须与 SpriteGroup/SeGroup/BatVoice 对齐

`MapGroup.grp` 的每个 entry 内部有三段数组，长度和对齐关系与 `ProgramMaterial.grp` 相同：

| 内部数组 | 长度 = |
|---------|--------|
| `array1` | `SpriteGroup.grp.spriteList.size()` (138) |
| `array2` | `SeGroup.grp.seList.size()` (38) |
| `array3` | `BatVoice.grp.voiceList.size()` (30) |

全部 372 个 MapGroup entry 的 array1 均为 138、array2 均为 38、array3 均为 30。

### Pipeline 影响

追加新条目到 SpriteGroup/BatVoice 后，MapGroup.grp **每个 entry** 的对应数组都必须同步 padding。
这不是只改一个值——是 372 × 3 段数组都需要检查。
遗漏任何 entry 都会导致引擎在加载对应 MapGroup 时因长度不匹配而崩溃。
