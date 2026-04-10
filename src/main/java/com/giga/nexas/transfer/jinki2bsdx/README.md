# JINKI -> BSDX 迁移

## 定位

这个包负责把 `AKAO / moribito_2` 从 `JINKI`（Baldr Heart）graft 到 `BSDX`（Baldr Sky DX）。

它不是 `BHE -> BSDX` 的语义转译，而是**同引擎资源链迁移**：

- 源：JINKI 的 grp/dat/mek/spm/waz 二进制资源
- 目标：BSDX 基线容器（追加/替换）
- 补充静态资源源：`D:\BDY\NeXAS_Resources\jinki_resources`

## 迁移模型

`step3/4/6/7` 共用一套"资源链驱动"的规则：

1. 从 `Akao.waz` 中深度遍历，抽出当前机体真实使用到的资源索引链
2. 对链上的每个资源做"复用还是尾插"的决策（先按 codeName/fileName 去 BSDX 基线中查重）
3. 形成统一的 `JINKI源索引 -> BSDX目标索引` 映射表
4. `mek / waz` 内部只按这张表做重定向

当前已纳入链分析的事件字段：

| 事件类型 | 索引字段 | 重绑方式 |
|---|---|---|
| `CEventWazaSelect` | `wazFileNo` -> JINKI WazaGroup 顶层索引 | 源->目标 WazaGroup 索引重映射 |
| `CEventSprite` | `spmFileSequence` -> JINKI SpriteGroup 顶层索引 | 源->目标 SpriteGroup 索引重映射 |
| `CEventSe` | `byteDataList[i]` 前 4 字节=seGroupIndex, 后 4 字节=seItemIndex | group/item 分别重映射 |
| `CEventVoice` | `byteDataList[i]` 前 4 字节=BatVoiceGroup 全局索引 | 语音组索引重映射 |

辅助 waz（弹幕/特效类）不递归收集——它们内部没有 `CEventWazaSelect/CEventSprite/CEventSe/CEventVoice` 交叉引用。

## Pipeline 主链 (11 步)

```
AkaoGraftRequest
  └─> AkaoGraftPipeline.execute()
        ├─ Step 1: DeserializeJinkiPackageStep   — 反序列化 JINKI 包
        ├─ Step 2: LoadBsdxBaselineStep          — 加载 BSDX 基线
        ├─ Step 3: BuildImportPlanStep            — 生成资源链导入计划
        ├─ Step 4: AppendGrpEntriesStep           — GRP 条目挂载（去重+尾插）
        ├─ Step 5: SyncProgramMaterialStep        — 同步 ProgramMaterial
        ├─ Step 6: RebindAkaoMekStep              — 重建 Akao.mek
        ├─ Step 7: RebindAkaoWazStep              — 重建 Akao.waz
        ├─ Step 8: ImportStaticAssetsStep         — 落盘静态资源
        ├─ Step 9: PatchMenuDataStep              — 菜单数据补丁
        ├─ Step 10: PatchExeCapacitiesStep        — EXE 容量 patch
        └─ Step 11: PackUpdatePacStep             — 打包 Update3.pac
```

### Step 1: DeserializeJinkiPackageStep

**功能**：反序列化 JINKI 包内所有二进制资源为 DTO。

**输入**：`AkaoGraftRequest`（包含 jinkiGrpDir/DatDir/MekDir/SpmDir/WazDir）
**输出**：`JinkiPackageBundle`

反序列化清单：
- grp 层：`BatVoice.grp`, `MekaGroup.grp`, `SeGroup.grp`, `SpriteGroup.grp`, `WazaGroup.grp`
- dat 层：`Meka.dat`（必需）、`MekaPilot.dat`（可选）
- mek 层：`Akao.mek`
- spm/waz 层：目录下所有 `*.spm` / `*.waz`

字符编码：`windows-31j`

### Step 2: LoadBsdxBaselineStep

**功能**：加载 BSDX 基线容器的全部资源。

**输入**：`AkaoGraftRequest`（bsdxGrpDir/DatDir/MekDir/SpmDir/WazDir）
**输出**：`BsdxBaselineBundle`

比 Step 1 多加载的文件：
- `ProgramMaterial.grp`（JINKI 侧不加载）
- `MapGroup.grp`（JINKI 侧不加载；每个 entry 内部的 array1/2/3 必须与 SpriteGroup/SeGroup/BatVoice count 对齐）
- `MekaPilot.dat`（BSDX 侧为必需）
- `SelectMekaMenu.dat`（菜单链专用）
- 菜单 UI spm：`MekaPilot.spm`, `SelectMekaMenuMeka.spm`

### Step 3: BuildImportPlanStep

**功能**：从 JINKI 源侧提取 AKAO 真实使用到的资源索引链，生成导入计划。

**输入**：`AkaoGraftRequest`, `JinkiPackageBundle`, `BsdxBaselineBundle`
**输出**：`JinkiImportPlan`

核心逻辑：
1. 建立 JINKI / BSDX 两侧的 sprite/waz 文件名到索引的映射表
2. 固定主 mek 文件，全量纳入包内 spm
3. 从 `Akao.waz` 中深度遍历所有 `SkillInfoObject`，通过反射处理嵌套 `*UnitList`
4. 收集 `CEventWazaSelect`（辅助 waz）、`CEventSprite`（sprite）、`CEventSe`（se group/item）的引用链

### Step 4: AppendGrpEntriesStep

**功能**：把 AKAO 资源链挂进 BSDX 基线，产出源到目标索引映射表。

**输入**：`AkaoGraftRequest`, `JinkiPackageBundle`, `BsdxBaselineBundle`, `JinkiImportPlan`
**输出**：`GrpAppendPlan`

当前这一步对 `WazaGroup` 的处理已经收窄为：

- 只把 `JINKI[110] = AKAO` 这个主条目 append 到 BSDX 末尾
- `Akao.waz` 里通过 `wazFileNo = 0..6` 引用到的共享辅助 `waz` 继续复用 BSDX 现有索引
- 不再在 `step4` 扩散追加同名辅助 `WazaGroup` 项
- 主 `AKAO` 和外部实际引用到的共通 `waz`，都会按最终采用的 `.waz.skillList.size()` 重算 `WazaGroup.param`

7 步挂载流程：
1. 挂主机体 MekaGroup → `MekaGroup[103]`（append 模式，count 103→104）
2. 挂主 WazaGroup（只 append `AKAO`）
3. 挂主 SpriteGroup
4. 挂 AKAO 的 BatVoiceGroup
5. 共享辅助 WazaGroup 保持复用 BSDX 现有索引
6. 挂辅助 SpriteGroup 链
7. 挂 SeGroup 组和组内 SeItem 链

**upsert 策略**：先按 codeName/fileName 查找已有条目，存在则复用索引，不存在则尾插。`WazaGroup` 当前特殊：只对主 `AKAO` 条目执行 append，辅助共享 `waz` 不在这一步扩散追加。
MekaGroup 特殊：支持 `fixedMekaGroupIndex` 强制替换指定位置（当前 `null`，使用默认 append）。

**去重匹配规则**：
| GRP 类型 | 匹配字段 |
|---|---|
| WazaGroup | codeName 或 displayName |
| SpriteGroup | codeName 或 fileName |
| BatVoice | characterCodeName |
| SeGroup | seTypeCodeName 或 seType |
| SeItem | seItemCodeName 或 seFileName |

### Step 5: SyncProgramMaterialStep

**功能**：同步 `ProgramMaterial.grp` 外层数组长度并补 values；同时 padding `MapGroup.grp` 每个 entry 的内部数组。

**输入**：`AkaoGraftRequest`, `BsdxBaselineBundle`, `GrpAppendPlan`
**输出**：`ProgramMaterialGrp`（就地修改后返回），`MapGroupGrp`（就地 padding）

三层策略：
1. 先把 `array1/array2/array3` 外层长度追平到当前 GRP 大小
2. 再用外部真源 `ProgramMaterial.grp` 补能安全映射的 `values`
3. 对 `MapGroup.grp` 每个 entry 的内部 array1/2/3 做 padding（见下文）

| 数组 | 对应 GRP | 同步方式 |
|---|---|---|
| array1 | SpriteGroup (138) | 只做"明确安全"的同步（values 为空或源索引=目标索引时同步） |
| array2 | SeGroup (38) | 按 SeGroup group/item 映射回写 |
| array3 | BatVoice (30) | 按 BatVoice 组映射回写 |

**MapGroup.grp 对齐**：

`MapGroup.grp` 每个 entry 内部也有三段数组，长度必须与 SpriteGroup/SeGroup/BatVoice 的顶层数量对齐：

| MapGroup 内部数组 | 对齐目标 | 原始长度 | 追加后长度 |
|---|---|---|---|
| array1 | SpriteGroup.spriteList.size() | 138 | 139 |
| array2 | SeGroup.seList.size() | 38 | 38（不变） |
| array3 | BatVoice.voiceList.size() | 30 | 31 |

追加新 SpriteGroup/BatVoice 条目后，所有 372 个 MapGroup entry 的对应数组都需要同步 padding，否则引擎加载时会因长度不匹配崩溃。

> **注意**：`array1.values[*]` 绑定的是 `MapGroup` 索引，当前没有建立 MapGroup 迁移映射。
> AKAO 的 `moribito_2.spm` 对应的 `ProgramMaterial.array1` 的 values 为空，不影响当前 graft。

### Step 6: RebindAkaoMekStep

**功能**：重建 Akao.mek，回写外部索引。

**输入**：`AkaoGraftRequest`, `JinkiPackageBundle`, `GrpAppendPlan`
**输出**：重建后的 `Mek` 对象

重建策略：不是在源对象上打补丁，而是创建新 Mek 外壳，按分片顺序逐片重建。

真正有语义改动的字段（`MekBasicInfo`）：
- `wazFileSequence` -> 目标 WazaGroup 索引
- `spmFileSequence` -> 目标 SpriteGroup 索引

其他分片（PairBlock/WeaponInfo/AI/Voice/Material）做结构级深拷贝，不改变内部语义。
`MekWeaponInfo.wazSequence` 仍解释为 Akao.waz 内部 skill 索引，不在 Step 6 修改。

### Step 7: RebindAkaoWazStep

**功能**：重建 Akao.waz，回写所有外部资源引用。

**输入**：`AkaoGraftRequest`, `JinkiPackageBundle`, `JinkiImportPlan`, `GrpAppendPlan`
**输出**：重建后的 `Waz` 对象

按 `Waz -> Skill -> Phase -> Unit -> SkillInfoObject` 层级逐层重建。

重绑的外部引用：
- `CEventWazaSelect.wazFileNo` -> 目标 WazaGroup 索引
- `CEventSprite.spmFileSequence` -> 目标 SpriteGroup 索引
- `CEventSe byteDataList` -> 目标 SeGroup/SeItem 索引（小端序覆写）
- `CEventVoice byteDataList` 前 4 字节 -> 目标 BatVoiceGroup 索引

嵌套 unit 列表（`CEventEffect` 等带 `*UnitList` 的对象）通过反射递归重建内部 `data`。

### Step 8: ImportStaticAssetsStep

**功能**：把 Step 6/7 产物和当前机体链上的静态资源落盘。

**输出目录**：`src/main/resources/out/jinki2bsdx_assets_<timestamp>`

落盘清单：
- 7 份修改后的 GRP（MekaGroup/WazaGroup/SpriteGroup/BatVoice/SeGroup/MapGroup/ProgramMaterial）
- 重绑后的 `Akao.mek`（通过 `BsdxBinService.generate` 序列化）
- 重绑后的 `Akao.waz`
- 辅助 waz 文件（原样 Files.copy，不递归重绑）
- 包内所有 spm 文件
- 当前链实际引用到的图像资源（从 spm.imageData 收集文件名）
- 当前链真实关联到的语音和音效文件（优先 .ogg，回退 .wav）

**已知**：21 张图像资源缺口已确认为原始游戏资源本身就缺，按"已知原版缺口"处理。

### Step 9: PatchMenuDataStep

**功能**：补选人菜单链（dat + spm + 图片）。

**策略**：原地替换 `SelectMekaMenu.dat` 第 24 行（index=24，原ピドーコマンダー），将 mekaIndex 改为 103（AKAO append 到 MekaGroup[103]）。dat 行数不变，exe 不需要 patch SelectMekaMenu 循环上界。

修改清单：
- `SelectMekaMenu.dat[24]` — mekaIndex→103，animIndex 和 state 保持原值
- `MekaPilot.dat` — 追加一行 `[103]`（AKAO 的 pilot 记录）
- `Meka.dat` — 追加 AKAO 的机体数据行
- `MekaPilot.spm` — 复用目标动画槽位，替换成 AKAO 的 pilot 图
- `SelectMekaMenuMeka.spm` — 复用目标动画槽位，只修改 `animName`，不再覆盖目标槽位原本的 page/chip/imageName
- 菜单 UI 图片 — 保留 BSDX 原本的菜单图片命名体系，当前产物应继续带出：
  - `selectmekamenumeka_0011_0001.png`
  - `selectmekamenumeka_0012_0001.png`

替换槽位常量：`REPLACE_VISIBLE_SELECT_MENU_SLOT_INDEX = 24`

### Step 10: PatchExeCapacitiesStep

**功能**：patch exe 中的 MekaGroup 容量硬编码上限。

**触发条件**：`request.isPlanExeCapacityPatch() == true`

当 AKAO 被 append 为 `MekaGroup[103]`（count 从 `103 -> 104`）时，meka capacity patch 集合包含以下位点。所有偏移均为 BaldrSky.exe（4,662,784 字节）的**文件绝对偏移**：

| # | 偏移 | 指令 | 函数 | 用途 |
|---|---|---|---|---|
| 1 | `0x1E3F40` | `push 103→104` (6A 67→68) | sub_5E4B20 (init) | 初始化预分配 meka 槽数组 |
| 2 | `0x1E3DA2` | `cmp ebp,103→104` (83 FD 67→68) | sub_5E46A0 (scene cleanup) | 场景切换清理引用计数 |
| 3 | `0x276427` | `cmp esi,103→104` (83 FE 67→68) | sub_676E00 (save read) loop#1 | 存档读取类型标记 |
| 4 | `0x2762EB` | `cmp esi,103→104` (83 FE 67→68) | sub_676E00 (save read) loop#2 | 存档读取机体状态 |
| 5 | `0x275336` | `push 103→104` (6A 67→68) | sub_675520 (save write) | 存档写入缓冲区预分配 |
| 6 | `0x063A85` | `cmp ebx,103→104` (83 FB 67→68) | sub_464630 (resource calc) | 资源大小累加循环 |
| 7 | `0x2749ED` | `cmp esi,103→104` (83 FE 67→68) | save read loop#3 | 第三条并行 save-read 路径 |
| 8 | `0x056CE4` | `push 103→104` (6A 67→68) | sub_4576F0 / sub_45C5B0 | **runtime meka table 预分配** |
| 8b | `0x056F45` | `0x0006D090→0x0006E180` | sub_4576F0 | **runtime meka table 初始化边界：103×4336→104×4336** |
| 9 | `0x056F9B` | `push 103→104` (6A 67→68) | init alt path | 另一条预分配路径 |
| 10 | `0x275158` | `push 103→104` (6A 67→68) | save write alt path | 另一条写出路径 |
| 11 | `0x30390A` | `push 103→104` (6A 67→68) | standalone prealloc | 独立预分配路径 |

**定位方法**：PE section header 被加壳器混淆，无法 VA→file offset 映射。改用函数签名特征（全局变量地址如 dword_876080）在 exe 二进制中搜索，再在函数范围内找 0x67 立即数。

**Patch 类型**：
- `push imm8`（6A xx）修改 +1 偏移
- `cmp reg,imm8`（83 Fx xx）修改 +2 偏移
- `imm32`（如 `cmp edi, 0x6D090`）则直接改 4 字节立即数

**SelectMekaMenu patch**：`0x14F21F` 处的 IMM32 patch 保留；原地替换不增加行数、`targetMaxOffset == 0x33C` 时不触发。

**安全限制**：SelectMekaMenu 可见槽超过 76 项时 pipeline 会报错（objectId 773+ 属于别的菜单分支，需要额外审计）。

**容量汇总**：Step 10 同时汇总 meka/waza/sprite/batVoice/se 五种 GRP 的容量需求，写入 `ExePatchPlan`。

**成对约束**：
- `0x056CE4`
  - 控制 `dword_875F54` 对应 runtime meka table 的预分配条数
- `0x056F45`
  - 控制同一张表的初始化边界：`103 * 4336 -> 104 * 4336`

**对应 first fail 现场**：
- `BaldrSky+0x1e149d / sub_5E13E0`
- `currentIndex = 103`
- `allowedCount = 103`
- 0-based 合法范围只有 `0..102`

**测试锁定点**：
- patched exe 中 `0x056CE4 == 0x68`
- patched exe 中 `0x056F45 == 0x0006E180`

**现象落点**：
- `dword_875F54` 这条 `103 < 103` 的 first fail 已消失
- `confirm` 可通过
- 崩点落到“进入练习模式 / 真正生成机体”这一层

### Step 11: PackUpdatePacStep

**功能**：把 Step 8/9 的输出目录打包成 `Update3.pac`。

**流程**：
1. 对输出根目录执行 `PacUtil.pack()`
2. 等待 `.pacNew` 文件生成
3. 重命名为 `Update3.pac`

**最终产出**：`src/main/resources/out/Update3.pac` + `BaldrSky_*.exe`（patched）

## 关键事实

### 1. AKAO 的主 sprite 入口是 `moribito_2.spm`

迁移时不能挂 `AKAO -> akao.spm`，而是：
- `SpriteGroup: 0001 -> moribito_2.spm`

### 2. GRP 容量硬编码扫描结论（2026-04-06 确认）

**只有 MekaGroup 103 有硬编码循环上界需要 patch。**

| GRP | 原始 count | 迁移后 count | exe 是否需要 patch | 原因 |
|---|---|---|---|---|
| MekaGroup | 103 | 104 | ✅ 6 处 | 硬编码循环上界 |
| WazaGroup | 110 | 111 | ❌ | IMM8 全部是函数参数/vtable 分发 |
| SpriteGroup | 138 | 139 | ❌ | 全部是初始化值 |
| BatVoice | 30 | 31 | ❌ | 30 太常见，184 处命中全是噪音 |
| SeGroup | 38 | 38 | ❌ | SE 引用全部在已有 BSDX 条目中匹配 |
| ProgramMaterial | — | — | ❌ | Step 5 自动同步 array1/array3 |

完整分析报告：`docs/project-deep-dive/exe-grp-capacity-scan.md`

### 3. HellMode 场景切换 0xFFFFFFFF 崩溃

HellMode 场景切换后约 0.5s 出现 `alloc(0xFFFFFFFF)` 导致 OOM。**未经原版复现验证，不确定是否为原版引擎 bug。** 需要用原版 exe + 原版 pac（不带 Update3.pac）复现才能确认。

### 4. exe packer 无 .text hash 校验

menu_only/menu_plus_pac 实验（同样改了 .text 的 0x14F21F）均正常运行，说明 Thinktech packer 未对 .text 做 hash 校验。

## 产物结构

Pipeline 执行后的 `out/` 目录包含：

```
out/
├── jinki2bsdx_assets_<timestamp>/     # Step 8/9 的平铺资源目录
│   ├── MekaGroup.grp
│   ├── WazaGroup.grp
│   ├── SpriteGroup.grp
│   ├── BatVoice.grp
│   ├── MapGroup.grp
│   ├── SeGroup.grp
│   ├── ProgramMaterial.grp
│   ├── Akao.mek
│   ├── Akao.waz
│   ├── Tama*.waz                       # 辅助 waz
│   ├── *.spm                           # sprite 文件
│   ├── *.png / *.ogg / *.wav           # 图像和音频
│   ├── Meka.dat
│   ├── MekaPilot.dat
│   ├── SelectMekaMenu.dat
│   ├── MekaPilot.spm
│   └── SelectMekaMenuMeka.spm
├── Update3.pac                         # Step 11 打包产物
└── BaldrSky_<timestamp>.exe            # Step 10 patched exe
```

## 入口

### 正式入口

```java
// Jinki2BsdxSingleRunner.run(request)
//   -> Jinki2BsdxTransfer.process(request)
//     -> AkaoGraftPipeline.execute(request)
```

所有路径和开关通过 `AkaoGraftRequest` 配置（支持 `-D` 系统属性覆盖）：

| 属性 | 说明 | 默认值 |
|---|---|---|
| `jinki2bsdx.mekaCodeName` | 目标机体 codeName | `AKAO` |
| `jinki2bsdx.spriteFileName` | 主 sprite 文件名 | `moribito_2.spm` |
| `jinki2bsdx.fixedMekaGroupIndex` | 强制指定 MekaGroup 索引（null=append） | `null` |
| `jinki2bsdx.patchMenuData` | 是否 patch 菜单 | `true` |
| `jinki2bsdx.planExeCapacityPatch` | 是否 patch exe meka 容量 | `true` |
| `jinki2bsdx.pacCompressMode` | pac 压缩模式 | `4` |

### 测试入口

| 测试类 | 说明 |
|---|---|
| `TestJinki2BsdxRunner` | 跑完整 pipeline 并断言所有中间产物 |
| `TestDiagnosticRunner` | 13 项诊断：GRP 容量、ProgramMaterial 一致性、索引映射、existFlag 完整性、GRP 二进制验证 |
| `TestDiagnosticRunner2` | SeGroup 映射详情诊断 |
| `TestDiagnosticRunner3` | Baseline 原始容量 + exe 硬编码检查 |

## 当前状态

### 已完成

- Step 1~11 全部实现
- MekaGroup 103→104 的 6 处 exe patch
- SelectMekaMenu 原地替换策略（第 24 行）
- 菜单 spm 图片替换（MekaPilot/SelectMekaMenuMeka）
- 非 meka GRP 的全面硬编码扫描确认

### 已知限制

- `ProgramMaterial.array1.values[*]` 绑定 MapGroup 索引，当前 AKAO graft 中 values 为空不影响
- SelectMekaMenu 超过 76 项后需要继续做 switch/object-id 审计
- 辅助 waz 不递归重绑（当前策略足够，弹幕/特效内部无交叉引用）
- 存在 2 处可能需要追加的 meka patch 位点（`0x2749EB`, `0x056CE3`），待实机验证确认

## 固定修复集合

### exe patch

- `0x056CE4`
  - `push 103 -> push 104`
  - runtime meka table 预分配条数扩到 104
- `0x056F45`
  - `4336 * 103 -> 4336 * 104`
  - runtime meka table 初始化边界扩到 104
- `0x05498B`
  - `56 * 103 -> 56 * 104`
  - `sub_454E60` 的装备菜单文本填表上界扩到 104

### data / graft

- `WeaponEquip.dat` 追加第 104 行
- 输出到：
  - `Config/WeaponEquip.dat`
  - 根目录 `WeaponEquip.dat`

## 测试锁定点

- `TestJinki2BsdxRunner`
  - 检查 `WeaponEquip.dat` 输出行为为 104 行
  - 检查 patched exe：
    - `0x056CE4 == 0x68`
    - `0x056F45 == 0x0006E180`
    - `0x05498B == 0x000016C0`

## 运行结果落点

- 复测目录：
  - `src/main/resources/tmp/baldrsky_20260410_150356/`
- 结果：
  - `frida_stage_probe_log.txt` 仅有 `ready / arm`
  - `crash_v6_log.txt = Exit code: 0x0, Dumps: 0`
