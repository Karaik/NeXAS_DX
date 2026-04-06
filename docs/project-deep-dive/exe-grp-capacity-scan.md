# BaldrSky.exe GRP 容量硬编码全面扫描报告

> **日期**: 2026-04-06
> **目标文件**: BaldrSky.exe（未加壳版，4,662,784 字节 / 0x472600）
> **搜索方法**: 在 exe 二进制中搜索各 GRP 原始 count 值作为 IMM8/IMM32 立即数的出现位置，排除 call 偏移、跳转偏移、数据段等误报

---

## 1. BSDX 原版 GRP Count 一览

| GRP 文件 | Key | 原始 Count | 对应 16 进制 |
|---|---|---|---|
| MekaGroup.grp | mekaList | **103** | 0x67 |
| WazaGroup.grp | wazaList | **110** | 0x6E |
| SpriteGroup.grp | spriteList | **138** | 0x8A |
| BatVoice.grp | voiceList | **30** | 0x1E |
| SeGroup.grp | seList | **38** | 0x26 |
| ProgramMaterial.grp | array1/array3 | 138 / 30 | (镜像 SpriteGroup / BatVoice) |

> ProgramMaterial.grp 的 `array1` 长度 = SpriteGroup count, `array3` 长度 = BatVoice count
> AKAO mod 不改变 SeGroup 和 ProgramMaterial 的 count

---

## 2. 搜索结果总表

### 2.1 按确定性分类

| GRP | 需 Patch | IMM8 命中 | IMM32 命中 | 确认循环上界 | 确认预分配 | switch-case | 误报/待确认 |
|---|---|---|---|---|---|---|---|
| **MekaGroup 103** | ✅ 已实现 | 9 (排除误报后) | 2 (排除数据段后) | **6 处** (已 patch) + 2 处已追加 + 3 处新发现待审 | 1 处已追加 | 3 处 | 1 处数据段 |
| **WazaGroup 110** | ❌ 动态读取 | 7 | 1 | 0 | 1 处可疑 | 3 处 | 2 处 |
| **SpriteGroup 138** | ❌ 动态读取 | 0 | 5 | 0 | 0 | 0 | 5 处(初始化) |
| **BatVoice 30** | ❌ 动态读取 | 27 | 23 | 0 | 0 | 多处 | **噪音极大** |
| **SeGroup 38** | ❌ 动态读取 | 2 | 6 | 2 处(可疑) | 0 | 0 | 6 处 |

---

## 3. 详细分析

### 3.1 MekaGroup 103 (0x67) — 已确认需要 Patch

#### ✅ 已 Patch 的 8 处循环上界/预分配

| # | 偏移 | 指令 | 函数 | 用途 |
|---|---|---|---|---|
| 1 | `0x1E3F40` | `push 103` (6A 67) | sub_5E4B20 (init) | ensureStructArrayCapacity 预分配 meka 槽数组 |
| 2 | `0x1E3DA2` | `cmp ebp,103` (83 FD 67) | sub_5E46A0 (scene cleanup) | do-while 清理引用计数数组 |
| 3 | `0x276427` | `cmp esi,103` (83 FE 67) | sub_676E00 (save read loop#1) | 存档读取第一个循环 |
| 4 | `0x2762EB` | `cmp esi,103` (83 FE 67) | sub_676E00 (save read loop#2) | 存档读取第二个循环 |
| 5 | `0x275336` | `push 103` (6A 67) | sub_675520 (save write) | ensureStructArrayCapacity 预分配写入缓冲区 |
| 6 | `0x063A85` | `cmp ebx,103` (83 FB 67) | sub_464630 (resource calc) | 资源大小累加循环 |
| 7 | `0x2749EB` | `cmp esi,103` (83 FE 67) | 存档读区 loop#3（独立函数） | do-while(++i&lt;103)，与 #3/#4 指令模式完全相同，存档读取第三个循环 |
| 8 | `0x056CE3` | `push 103` (6A 67) | 初始化预分配 | push 103 后紧跟 call，与 #1 模式类似，另一处预分配调用 |

#### 🆕 新发现的 3 处可能需要 Patch（待审）

| # | 偏移 | 指令 | 分类 | 分析 |
|---|---|---|---|---|
| 9 | `0x056F9A` | `push 103` (6A 67) | **PUSH** | 与 #8 同函数域，前面紧跟 `rep movsd`（复制 138 个 dword = SpriteGroup count），push 103 后跟 lea + call，模式与 #1 类似 |
| 10 | `0x275157` | `push 103` (6A 67) | **PUSH** | 与 #5 同函数域（save write），指令模式几乎相同（33 C0 50 6A 67 8B CF），同一函数的另一条写入路径（参数 4） |
| 11 | `0x303909` | `push 103` (6A 67) | **PUSH** | 完全不同的代码区域，push 0x40(64) + call 后紧接 push 103 + call，操作 `[edi+0x1D0]` 和 `[esi+0x14C]` 相关结构 |

#### ❌ 排除的无关位点

| 偏移 | 分类 | 原因 |
|---|---|---|
| `0x06BDC2` | 误报 | 实际是 `call` 指令的相对偏移 E8 6A 67 FC FF 中的 0x67 |
| `0x438D24` | 数据段 | 前后都是数据模式 (67 7B 00 xx xx xx xx) |
| `0x27F9A9` | switch-case | 连续比较 103/105/110/111，是事件类型分发 |
| `0x28B024` | switch-case | 同上模式 |
| `0x2AFF72` | switch-case | 比较 102/103，短跳转 |
| `0x2C338F` | switch-case | 比较 103/105/110 |
| `0x4502A1~0x450301` | 数据段 | 4 处 mov reg,imm32，全在数据区 |

---

### 3.2 WazaGroup 110 (0x6E) — 确认不需要 Patch

**结论**: WazaGroup 的 GRP count 在引擎中是**动态读取**的，没有硬编码上界循环。

#### IMM8 分析 (7 处)

| 偏移 | 指令 | 分类 | 分析 |
|---|---|---|---|
| `0x079209` | push 110 | PUSH | 函数参数序列的一部分，不涉及 waza 容量 |
| `0x0792C6` | push 110 | PUSH | 同上 |
| `0x1317C3` | push 110 | PUSH | 函数参数序列 |
| `0x131845` | push 110 | PUSH | 函数参数序列 |
| `0x131BDD` | push 110 | PUSH | 函数参数序列 |
| `0x14560E` | push 110 | PUSH | 函数参数序列 |
| `0x146227` | push 110 | PUSH | 函数参数序列 |
| `0x1F9970` | push 110 | PUSH | dword_86CE04 全局变量操作 |
| `0x1F9C10` | push 110 | PUSH | 同上 |
| `0x07C937` | cmp ecx,110 | switch-case | 89 86 BC 00 00 00 后跟 min() 模式 |
| `0x14663C` | cmp eax,110 | LOOP | vtable 调用后比较，可能是 WazaGroup vtable 分发 |
| `0x14665C` | cmp eax,110 | LOOP | 同上 |
| `0x27F9B7` | cmp eax,110 | LOOP | 紧接在 meka switch-case 后面的比较 |
| `0x28B032` | cmp eax,110 | LOOP | 同上 |
| `0x2C32FA` | cmp eax,110 | switch-case | 连续比较 110/99/123 |
| `0x2C3399` | cmp eax,110 | LOOP | 紧接 meka switch 后 |

#### IMM32 (1 处)

| 偏移 | 指令 | 分析 |
|---|---|---|
| `0x14BA84` | mov ecx,110 (B9 6E000000) | 初始化某个值，后面跟 sub ecx, eax; imul; 然后 0F 85 跳转。可能是 waza 参数相关的初始值，不是容量上限 |

**判定**: 所有 110 的出现都是函数参数传递或 vtable 分发，没有循环上界。引擎动态读取 WazaGroup.grp 的 count。

---

### 3.3 SpriteGroup 138 (0x8A) — 确认不需要 Patch

**结论**: SpriteGroup 同样是**动态读取** count，没有硬编码上界。

#### IMM8: 0 处有效命中（唯一的 1 处是 call 相对偏移误报）

#### IMM32 (5 处)

| 偏移 | 指令 | 分析 |
|---|---|---|
| `0x150AE6` | push imm32 138 (68 8A000000) | push 138 后跟 push 0x74(116); push 0; push 0; call。可能是某个初始化参数 |
| `0x295C35` | mov ecx,138 | 循环初始化值，与 BatVoice 30 的类似模式（见 0x2958D1）。可能是某个数据结构的默认初始化值 |
| `0x295CBB` | mov ecx,138 | 同上模式 |
| `0x295D25` | mov ecx,138 | 同上模式 |
| `0x2961D4` | mov ecx,138 | 同上模式 |

**判定**: 所有 138 的出现都是初始化值或函数参数，没有循环上界。引擎动态读取 SpriteGroup.grp 的 count。

---

### 3.4 BatVoice 30 (0x1E) — 确认不需要 Patch

**结论**: BatVoice 也是**动态读取** count。30 是一个非常常见的小数字，搜索结果噪音极大。

#### IMM8: 27 处 cmp + 157 处 push = 184 处
绝大多数是:
- 函数参数 (push 30)
- 窗口/缓冲区大小等通用常量
- 事件类型 switch-case 分发

没有找到类似 meka 的 `do-while(i < 30)` 循环上界模式。

#### IMM32: 23 处
主要是:
- 初始化值 (mov reg, 30)
- 循环计数器初始值
- 缓冲区大小

**判定**: 不需要 patch。引擎动态读取 BatVoice.grp 的 count。

---

### 3.5 SeGroup 38 (0x26) — 确认不需要 Patch

**结论**: SeGroup 同样**动态读取** count。

#### IMM8 (2 处 cmp)

| 偏移 | 指令 | 分析 |
|---|---|---|
| `0x068304` | cmp eax,38 | 函数返回值检查 (75 09 = jnz)，不是循环上界 |
| `0x0BDC18` | cmp eax,38 | 可能是 SE 播放相关的返回值检查 |

#### IMM32 (6 处)
主要是初始化值和 SE 播放参数。

**判定**: 不需要 patch。引擎动态读取 SeGroup.grp 的 count。AKAO mod 不增加 SE 条目。

---

## 4. 结论与建议

### 4.1 当前已实现的 Patch

| Patch | GRP | 原值→目标 | 位点数 | 状态 |
|---|---|---|---|---|
| Meka capacity | MekaGroup | 103→104 | 8 处 | ✅ 已实现 (PatchExeCapacitiesStep) |

### 4.2 需要关注的额外位点（待审）

| 偏移 | 指令 | 建议 |
|---|---|---|
| `0x056F9A` | push 103 → 104 | ⚠️ **建议追加 patch** — 与 #8 同函数域，`rep movsd` 后的预分配调用 |
| `0x275157` | push 103 → 104 | ⚠️ **建议追加 patch** — 与 #5 同函数域，save write 另一条写入路径 |
| `0x303909` | push 103 → 104 | ⚠️ **建议追加 patch** — 独立代码区域，push 103 后紧跟 call |

### 4.3 不需要 Patch 的 GRP

| GRP | 原因 |
|---|---|
| WazaGroup (110) | 动态读取 count，无硬编码循环上界 |
| SpriteGroup (138) | 动态读取 count，无硬编码循环上界 |
| BatVoice (30) | 动态读取 count，无硬编码循环上界 |
| SeGroup (38) | 动态读取 count，无硬编码循环上界；且 AKAO mod 不增加 SE 条目 |

### 4.4 ProgramMaterial 说明

ProgramMaterial.grp 是引擎内部用于跟踪 sprite/batvoice 资源加载状态的结构：
- `array1` 长度 = SpriteGroup count (138)
- `array3` 长度 = BatVoice count (30)
- 当 SpriteGroup/BatVoice count 变化时，Pipeline Step 5 (SyncProgramMaterialStep) 已自动同步更新

---

## 5. 搜索方法说明

### 5.1 IMM8 搜索
- `push imm8`: `6A xx` — 搜索 `6A` 后跟目标值的字节
- `cmp reg,imm8`: `83 F8~FF xx` — 搜索 `83` + `(0xF8|reg)` + 目标值

### 5.2 IMM32 搜索
- `cmp eax,imm32`: `3D xx xx xx xx`
- `push imm32`: `68 xx xx xx xx`
- `mov reg,imm32`: `B8~BF xx xx xx xx`

### 5.3 误报过滤
- 排除 `call` 指令相对偏移 (`E8 xx` 中 xx 碰巧包含目标值)
- 排除跳转偏移 (`0F 8x` / `7x` 后的偏移)
- 排除数据段 (偏移 > 0x448000)
- 排除连续多个 `cmp` 的 switch-case 模式

### 5.4 分类判断
- **LOOP**: `cmp` 后紧跟 `jl/jle/jb/jbe` (0x7C~0x7F) 或 `0F 8C~8F` 近跳转
- **SWITCH-CASE**: 连续 3+ 个 `cmp reg,imm8` 在 100 字节内
- **PUSH**: 后面紧跟 `call` (`E8`)
