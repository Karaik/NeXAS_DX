# 06. AKAO 并入 BSDX 的迁移方案

## 当前目标

把 `AKAO / moribito_2` 作为一条完整资源链，从 `JINKI` graft 到 `BSDX`，最终输出可打包的 `Update3.pac`。

## 当前规则

当前迁移模型已经对齐为：

1. 先从 `Akao.waz` 出发递归抽当前机体真实使用到的 WAZ 闭包、资源链和索引链
2. 再对链上的每个资源做“复用还是尾插”的决策
3. 对辅助 WAZ 形成 key-based skill merge 结果
4. 形成统一的 `JINKI源索引 -> BSDX目标索引`
5. `mek / waz` 内部只消费这些结果表做重定向

当前这一步对 `WazaGroup` 的处理已经收窄为：

- 只把 `JINKI[110] = AKAO` 这个主条目 append 到 BSDX 末尾
- 辅助 WAZ 按文件名复用 BSDX 现有 WazaGroup；目标缺失时 append
- 辅助 WAZ 内部建立 `source skill index -> target skill index`
- 主 `AKAO` 和辅助 WAZ 都会按最终输出 `.waz.skillList.size()` 回写 `WazaGroup.param`

当前测试侧还额外有一个重要前提：

- `AKAO` 当前仍然 append 到新的 `MekaGroup[103]`
- 当前测试链也继续验证 `mekaIndex = 103` 的真实运行路径
- 当前收敛重点不再是“是否复用旧槽位”，而是 `103` 进入选人确认后的后续加载链

当前已纳入链分析的对象：

- `CEventWazaSelect.wazFileNo`
- `CEventWazaSelect.wazSequenceNo`
- `CEventSprite.spmFileSequence`
- `CEventSe.group/item`
- `CEventVoice`

## 当前主链

1. `DeserializeJinkiPackageStep`
2. `LoadBsdxBaselineStep`
3. `BuildImportPlanStep`
4. `AppendGrpEntriesStep`
5. `SyncProgramMaterialStep`
6. `RebindAkaoMekStep`
7. `RebindAkaoWazStep`
8. `ImportStaticAssetsStep`
9. `PatchMenuDataStep`
10. `PatchExeCapacitiesStep`
11. `PackUpdatePacStep`

## 当前实现状态

### Step 5. `SyncProgramMaterialStep`

当前已经不是只补长度。

现在的行为是：

- 先把 `array1 / array2 / array3` 外层长度追平到当前 `grp` 大小
- 再优先用外部真源 `ProgramMaterial.grp` 补能安全映射的 `values`

当前已做：

- `array2` 按 `SeGroup.group/item` 映射回写
- `array3` 按 `BatVoice` 组映射回写
- `array1` 只在安全场景下同步  
  原因：`array1.values[*]` 绑定的是 `MapGroup`，当前尚未建立 `MapGroup` 迁移映射

对当前 AKAO 真源再核过一遍后，可以补充一个当前结论：

- `JINKI` 真源里 `moribito_2.spm` 对应的 `ProgramMaterial.array1[33].values = []`
- 所以当前迁移后 `ProgramMaterial.array1[138] = []` 是正确结果
- 也就是说，`array1.values[*]` 的完整 `MapGroup` 映射在“当前 AKAO 这一次 graft”里不是阻塞项

### Step 8. `ImportStaticAssetsStep`

当前已经实现：

- 输出目录：`src/main/resources/out/jinki2bsdx_assets_<timestamp>`
- 所有资源平铺到输出根目录
- 写入重绑后的 `Akao.mek`
- 写入重绑后的 `Akao.waz`
- 写入合并后的辅助 WAZ
- 写入修改后的：
  - `MekaGroup.grp`
  - `WazaGroup.grp`
  - `SpriteGroup.grp`
  - `BatVoice.grp`
  - `SeGroup.grp`
  - `ProgramMaterial.grp`
- 复制链上 `spm`
- 按 `requiredSpmFiles` 和新增 / 新设 skill 可达 SPM 的 `imageData` 补齐图像资源
- 只复制当前链真实关联的语音和音效

当前实测现状：

- 仍有 `21` 个图像缺失
- 这 `21` 张图已经确认是原始游戏资源本身就缺，当前按“原版缺口”处理

### Step 9. `PatchMenuDataStep`

当前已实现完整菜单链补丁：

- `Meka.dat`
- `MekaPilot.dat`
- `SelectMekaMenu.dat`
- `MekaPilot.spm`
- `SelectMekaMenuMeka.spm`
- 菜单图像：
  - `M_moribito_2.png`
  - `selectmekamenumeka_0011_0001.png`
  - `selectmekamenumeka_0012_0001.png`

当前已验证的实际结果：

- `SelectMekaMenu.dat`
  - 当前测试侧不再增加第 `71` 个可见槽
  - 而是复用第 `25` 个可见槽
  - 当前这行数据已经改成 `SelectMekaMenu.dat[24] = [103, 18, 108]`
  - 槽位位置不变，但这个槽现在绑定到 `MekaGroup[103] = AKAO`
- `MekaPilot.dat`
  - 当前测试侧也不再新增 pilot 行
  - 复用原来的 `MekaPilot.dat[29] = [32]`
- `SelectMekaMenuMeka.spm`
  - 当前测试侧复用原来的 `anim[18]`
  - `anim[18]` 现在只修改 `animName`
  - 目标槽位原本的 page/chip/imageName 保持不动
  - 菜单机体图继续挂在 `selectmekamenumeka_0011_0001.png / selectmekamenumeka_0012_0001.png`
- `MekaPilot.spm`
  - 当前测试侧复用原来的 `anim[29]`
  - `anim[29]` 的 pilot 图已经替成 AKAO

### Step 10. `PatchExeCapacitiesStep`

Step 10 的职责是把 meka 相关运行时容量硬编码从 `103` 放到 `104`，并把容量需求写入 `ExePatchPlan`。

已纳入 patch 集合的关键位点如下：

| # | 偏移 | 说明 |
|---|---|---|
| 1 | `0x1E3F40` | init allocator：`push 103 -> 104` |
| 2 | `0x1E3DA2` | scene cleanup loop bound：`103 -> 104` |
| 3 | `0x276427` | save read loop#1：`103 -> 104` |
| 4 | `0x2762EB` | save read loop#2：`103 -> 104` |
| 5 | `0x275336` | save write allocator：`103 -> 104` |
| 6 | `0x063A85` | resource calc loop bound：`103 -> 104` |
| 7 | `0x2749ED` | save read loop#3：`103 -> 104` |
| 8 | `0x056CE4` | **CMekaGroup runtime table prealloc**：`push 103 -> 104` |
| 8b | `0x056F45` | **CMekaGroup runtime table init loop bound**：`103 * 4336 -> 104 * 4336` |
| 9 | `0x056F9B` | init prealloc alt path：`103 -> 104` |
| 10 | `0x275158` | save write alt path：`103 -> 104` |
| 11 | `0x30390A` | standalone prealloc path：`103 -> 104` |

其中 `0x056CE4 / 0x056F45` 这一组具有成对约束：

- `0x056CE4`
  - 控制 `dword_875F54` 对应 runtime meka table 的预分配条数
- `0x056F45`
  - 控制同一张表的初始化边界：`103 * 4336 -> 104 * 4336`

这组位点对应的 first fail 现场是：

- `BaldrSky+0x1e149d / sub_5E13E0`
- `currentIndex = 103`
- `allowedCount = 103`
- 0-based 合法范围只有 `0..102`

因此这不是资源没进来，而是 runtime meka table 在引擎侧被做成了 103 条。

`SelectMekaMenu` 行数这边由于不再增加第 `71` 个可见槽，所以：

- `0x14F21F` 仍保持原值
- `SelectMekaMenu` 行数上界 patch 不触发

测试锁定点：

- patched exe 中 `0x056CE4 == 0x68`
- patched exe 中 `0x056F45 == 0x0006E180`

现象落点：

- `dword_875F54` 这条 `103 < 103` 的 first fail 已消失
- `confirm` 可通过
- 崩点落到“进入练习模式 / 真正生成机体”这一层

### Step 11. `PackUpdatePacStep`

当前已实现：

- 对 step8/9 的输出根目录执行 `PacUtil.pack()`
- 输出：`src/main/resources/out/Update3.pac`

## 当前 review 结论

### 已确认跟上的部分

- 修改后的 `grp` 现在已经真正写进产物
- `ProgramMaterial.grp` 现在已经不是只补外层长度
- `step8/9/10/11` 已经在同一条链上连通
- `step9` 的菜单 dat/spm/UI 图片已经跟上
- `step10` 的 `54F570` 菜单 70 项上界位点已经补上
- `step10` 的两处 meka patch 已经通过矩阵验证为有害，并从默认产物中移除
- `Update3.pac` 已能生成

### 当前仍存在的严重问题

- 非机体侧 `grp` 相关 exe 位点尚未确认完
- `ProgramMaterial.array1.values[*]` 还没有做成通用 `MapGroup` 映射器
- `SelectMekaMenu` 当前只审到了 `76` 项，可继续扩，但必须先补 `773+` 之后的 switch/object-id 链
- `meka 103 -> 104` 的安全扩容位点还没找到，现有两处位点已经确认不能直接用

## 本轮新增结论

这轮进一步收敛出来的核心问题是：

- 即使把 AKAO 放到菜单槽里，只要底层仍然使用新的 `mekaIndex = 103`
- 真实选人路径仍然高度可疑会踩到 exe 内部与 meka 顶层索引相关的固定边界

所以当前测试侧进一步改成：

- 菜单槽仍然复用第 `25` 个可见槽
- `MekaGroup` 当前仍然使用新的 `103`
- 当前动态验证已经从“hover 是否崩溃”推进到“confirm 之后的加载链是否还存在 103 相关错位”

这版的动态验证结果是：

- 强制进入选机菜单时，AKAO 已经能正常出现在第 25 个可见槽位
- 当前稳定现象是：hover 不崩，confirm 后崩

也就是说，当前测试方向已经从“修菜单显示槽”推进到“确认 AKAO 之后的真实加载链”，用来继续验证 `mekaIndex = 103` 进入后是否还有下游数据或容量没对齐。

## 固定修复集合

### exe

- `0x056CE4`
  - `push 103 -> push 104`
  - `dword_875F54` 对应 runtime meka table 预分配条数扩到 104
- `0x056F45`
  - `4336 * 103 -> 4336 * 104`
  - 同一张 runtime meka table 的初始化边界扩到 104
- `0x05498B`
  - `56 * 103 -> 56 * 104`
  - `sub_454E60` 的装备菜单文本填表上界扩到 104
- `0x20C1FD`
  - `84 C0 75 06 -> 90 90 EB 06`
  - `sub_60CDF0` 的直接型 AT/FC 战斗语音 request 不再被 `sub_60CC20` 的零返回提前截断
- `0x20C2CD`
  - `84 C0 75 06 -> 90 90 EB 06`
  - `sub_60CEC0` 的表驱动型战斗语音 request 不再被 `sub_60CC20` 的零返回提前截断

### data / graft

- `WeaponEquip.dat` graft 到 104 行
- `WeaponEquip.dat` 输出到根目录
- 辅助 WAZ 递归闭包 + key-based merge
- 辅助 WAZ 内部 `CEventWazaSelect` group/skill 双层 remap
- 新增 / 新设 skill 可达 SPM 图片进入输出包

## 测试锁定点

- `TestJinki2BsdxRunner`
  - 断言 `WeaponEquip.dat` 输出为 104 行
  - 断言 patched exe 中：
    - `0x056CE4 == 0x68`
    - `0x056F45 == 0x0006E180`
    - `0x05498B == 0x000016C0`
    - `0x20C1FD == 90 90 EB 06`
    - `0x20C2CD == 90 90 EB 06`
  - 断言 `bomb.waz` 输出为 136 个 skill
  - 断言 `Tama02/Tama04/Tama05` 正确引用 `Bomb[134/133/135]`
  - 断言 `bomb_004_0002.png` 进入输出

## 现象落点

- 旧的 confirm 崩溃链已跨过
- `WeaponEquip.dat + 装备菜单文本填表上界` 对应的崩溃链已跨过
- 复测目录：
  - `src/main/resources/tmp/baldrsky_20260410_150356/`
- 结果：
  - `frida_stage_probe_log.txt` 仅有 `ready / arm`
  - `crash_v6_log.txt = Exit code: 0x0, Dumps: 0`
