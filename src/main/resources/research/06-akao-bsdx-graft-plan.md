# 06. AKAO 并入 BSDX 的迁移方案

## 目标

本文讨论的不是 `BHE -> BSDX` 转译，而是：

- 把 `AKAO / moribito_2` 这条资源链作为新的 BSDX 角色，正式追加进主 BSDX
- 源资源与目标资源属于同款引擎、同构格式
- 问题本质是 BSDX 内部资源 graft，不是跨引擎语义重编译

也就是说，`AKAO` 迁移的正确口径是：

- 不是 `BHE` 资源转译
- 不是覆盖旧角色槽位
- 而是 **JINKI 真源闭包导入 + BSDX 目标索引重绑 + 必要的 exe 容量 patch**

## 当前输入源

当前项目内，`AKAO` 的包内测试输入已经统一为：

- `src/main/resources/game/jinki/grp`
- `src/main/resources/game/jinki/dat`
- `src/main/resources/game/jinki/mek`
- `src/main/resources/game/jinki/spm`
- `src/main/resources/game/jinki/waz`

反序列化产物已经通过镜像测试生成到：

- `src/main/resources/grpJinkiJson`
- `src/main/resources/datJinkiJson`
- `src/main/resources/mekJinkiJson`
- `src/main/resources/spmJinkiJson`
- `src/main/resources/wazJinkiJson`

项目外的：

- `D:\BDY\NeXAS_Resources\jinki_resources`

后续只作为静态资源补充源使用，不再作为 pipeline 输入。

## 迁移对象范围

当前 `jinki` 包内最小真实闭包为：

### grp

- `BatVoice.grp`
- `MekaGroup.grp`
- `SeGroup.grp`
- `SpriteGroup.grp`
- `WazaGroup.grp`

### dat

- `Meka.dat`
- `MekaPilot.dat`

### mek

- `Akao.mek`

### spm

- `moribito_2.spm`
- `C_moribito_2.spm`
- `G_moribito_2.spm`
- `M_moribito_2.spm`
- `Fire.spm`

### waz

- `Akao.waz`
- `Bomb.waz`
- `Effect.waz`
- `Tama01.waz`
- `Tama02.waz`
- `Tama03.waz`
- `Tama04.waz`
- `Tama05.waz`

## 为什么 `AKAO` 不能直接复用现有 BSDX 的同名 loose file

当前主 BSDX 资源树里已经存在部分同名资源，例如：

- `game/bsdx/mek/Akao.mek`
- `game/bsdx/waz/Akao.waz`

但它们不能作为这次 graft 的权威源。原因是：

1. `game/jinki` 才是当前验证过的包内真源
2. 同名 loose file 不能保证内容完全一致
3. 同名 grp 行即使名字一样，参数也可能不同

因此，当前迁移方案不再把“同名文件能否复用”当成核心主线。
现在的正式规则是：

- **默认按 JINKI 真源整体导入**
- 如果未来某些文件要做复用，必须在专门审计步骤里单独证明
- 但这不再影响主 pipeline 的设计

## `AKAO` 链的最小核心

当前已经确认，`AKAO` 迁移的最小核心是一条清楚的链：

### 主体链

- `Akao.mek`
- `Akao.waz`
- `moribito_2.spm`

### grp 注册链

- `MekaGroup` 追加 `AKAO`
- `WazaGroup` 追加 `AKAO`
- `SpriteGroup` 追加 `0001 -> moribito_2.spm`
- `BatVoice` 追加 `AKAO`

### 依赖闭包

- `Bomb.waz`
- `Effect.waz`
- `Tama01.waz`
- `Tama02.waz`
- `Tama03.waz`
- `Tama04.waz`
- `Tama05.waz`
- `Fire.spm`
- `C_/G_/M_ moribito_2.spm`

## 关键事实：`AKAO` 的主 SPM 不是 `AKAO`

这是迁移时最容易搞错的一点。

从：

- `src/main/resources/mekJinkiJson/Akao.mek.json`
- `src/main/resources/grpJinkiJson/SpriteGroup.grp.json`

可以确认：

- `Akao.mek.mekBasicInfo.spmFileSequence = 133`
- JINKI 的 `SpriteGroup` 中，`moribito_2.spm` 才是当前主链入口

因此迁移时：

- 不能追加 `AKAO -> akao.spm`
- 必须迁移 `0001 -> moribito_2.spm` 这条 sprite 注册链

## 迁移总策略

建议分成两个阶段：

### 阶段 A：战斗可用版

目标：

- 让 `AKAO` 作为新机体进入主 BSDX 的战斗加载链
- 暂时不要求菜单层完整可见

### 阶段 B：菜单可见版

目标：

- 让 `AKAO` 出现在主 BSDX 菜单和相关 UI 中
- 补 dat、头像、语音、菜单 spm 等展示层资源

下面先写阶段 A。

## 阶段 A：战斗可用版

### Step 1. 固定源资源闭包

以包内 `game/jinki` 为唯一二进制真源，固定这次 graft 的输入闭包：

- `Akao.mek`
- `Akao.waz`
- `moribito_2.spm`
- `C_moribito_2.spm`
- `G_moribito_2.spm`
- `M_moribito_2.spm`
- `Fire.spm`
- `Bomb.waz`
- `Effect.waz`
- `Tama01.waz`
- `Tama02.waz`
- `Tama03.waz`
- `Tama04.waz`
- `Tama05.waz`
- `BatVoice.grp`
- `MekaGroup.grp`
- `SeGroup.grp`
- `SpriteGroup.grp`
- `WazaGroup.grp`

这一步的目的，是避免迁移过程中混入主 BSDX 中名字相同但版本不明的 loose file。

### Step 2. 生成 ImportPlan

这一步不再生成 `reuse/import diff manifest`。

现在 step2/step3 的正式口径已经改为：

- **按 JINKI 真源整体生成 ImportPlan**

它的职责是：

- 固定主 `mek/waz/spm` 导入闭包
- 固定后续要追加的 grp 顶层条目
- 固定后续要重绑的 `mek/waz` 外部索引目标
- 记录 JINKI 侧源索引，供后面真正重绑使用

当前 `ImportPlan` 的主要输出包括：

- `requiredMekFiles`
- `requiredWazFiles`
- `requiredSpmFiles`
- `grpAppendTargets`
- `programMaterialSyncTargets`
- `mekRebindTargets`
- `wazRebindTargets`

也就是说，这一步已经不再问：

- 哪些文件可以和 BSDX 做文件级复用

而是直接回答：

- 这次 graft 的闭包里必须导入哪些文件
- 后面要如何挂接这些文件

### Step 3. 追加 grp 顶层条目

只追加 AKAO 分支真正需要的条目：

#### `MekaGroup.grp`

- 追加 `AKAO`

#### `WazaGroup.grp`

- 追加 `AKAO`

#### `SpriteGroup.grp`

- 追加 `0001 -> moribito_2.spm`

这里不能保留 JINKI 原有索引。
必须在主 BSDX 的 `SpriteGroup` 中重新分配目标索引，再把这个目标索引回写给 `Akao.mek` 与 `Akao.waz`。

#### `BatVoice.grp`

- 追加 `AKAO`

#### `SeGroup.grp`

当前阶段先不追加顶层组。

原因：

- 目前没有证据表明 `AKAO` 需要新的独立顶层 SE group
- 现阶段先沿用主 BSDX 的共享 SE 体系

### Step 4. 同步 `ProgramMaterial.grp`

只要追加了顶层 grp 条目，就必须同步 `ProgramMaterial.grp`。

当前需要同步的是：

- `SpriteGroup` 追加后，补 `ProgramMaterial.array1`
- `BatVoice` 追加后，补 `ProgramMaterial.array3`

如果当前没有新增顶层 `SeGroup`：

- `ProgramMaterial.array2` 暂时不动

这一步不要求第一轮就补非空值，但必须保证外层长度一致。

### Step 5. 重绑 `Akao.mek`

来源：

- `src/main/resources/game/jinki/mek/Akao.mek`
- `src/main/resources/mekJinkiJson/Akao.mek.json`

至少要回写：

- `mekBasicInfo.wazFileSequence`
- `mekBasicInfo.spmFileSequence`

规则：

- `wazFileSequence` 改成主 BSDX 中新追加的 `AKAO` 在 `WazaGroup` 里的目标索引
- `spmFileSequence` 改成主 BSDX 中新追加的 `0001 -> moribito_2.spm` 在 `SpriteGroup` 里的目标索引

注意：

- `MekWeaponInfo.wazSequence` 不按 `WazaGroup` 顶层索引解释
- 它指向的是 `Akao.waz` 内部 `skillList` 索引

### Step 6. 重绑 `Akao.waz`

来源：

- `src/main/resources/game/jinki/waz/Akao.waz`
- `src/main/resources/wazJinkiJson/Akao.waz.json`

至少要检查并回写：

- 外部 `spmFileSequence`
- 外部 `wazFileNo`

当前已确认：

- `Akao.waz` 里外部 `spmFileSequence` 只出现 `-1` 和 `133`

所以迁移时必须把所有指向 JINKI 主 SPM 链的外部序号重绑到 BSDX 的新 `SpriteGroup` 索引。

`wazFileNo` 的处理规则：

- 不能照搬 JINKI 原 registry 序号
- 要按 BSDX 最终实际导入后的 `WazaGroup` 目标索引重映射

### Step 7. 导入静态资源

把 `ImportPlan` 固定出来的静态资源闭包真正落地到输出集。

至少涉及：

- `moribito_2.spm`
- `C_moribito_2.spm`
- `G_moribito_2.spm`
- `M_moribito_2.spm`
- `Fire.spm`
- `Akao.waz`
- `Bomb.waz`
- `Effect.waz`
- `Tama01..05.waz`

这一步的意义已经不是“边 diff 边猜”，而是按 `ImportPlan` 直接执行。

## 阶段 B：菜单可见版

要让 `AKAO` 真正在主 BSDX 菜单里出现，除了阶段 A 的全部内容，还要补：

- `Meka.dat`
- `MekaPilot.dat`
- 菜单层相关 `spm`
- 项目外头像、语音、UI 图等静态资源

阶段 B 必须和阶段 A 拆开推进，不要混成一轮。

## exe patch 预留

`AKAO` 真追加，不是只改数据。

虽然 `MekaGroup/WazaGroup` 等文件读取本身很多是动态的，但游戏 exe 里存在与内容规模绑定的运行时表。

当前已经确认的方向：

- `153` 是武装页那一侧的硬上限，但不是 AKAO 当前第一优先阻塞
- `AKAO` 这种真追加新机体，更应该优先关注机体侧 `103` 这组容量链

因此对于 `AKAO`：

- 战斗版 migration plan 必须包含 exe patch 预留

## 当前已经落地到代码的 pipeline 口径

当前正式项目代码里，`step1/2/3` 已经改成：

1. `DeserializeJinkiPackageStep`
2. `LoadBsdxBaselineStep`
3. `BuildImportPlanStep`

也就是说，当前主链已经不再使用：

- `BuildDiffManifest`
- `reuse/import manifest`

而是直接进入：

- `ImportPlan`

## 当前 runner 与测试入口

当前可以通过：

- `com.giga.nexas.transfer.jinki2bsdx.Jinki2BsdxSingleRunner`

直接启动主流程。

测试侧入口在：

- `src/test/java/com/giga/nexas/jinki/TestJinki2BsdxRunner.java`

## 迁移 pipeline 写法建议

对 `AKAO`，现在的正式 pipeline 写法应当是：

### AKAO Graft Pipeline

1. `DeserializePackage`
2. `LoadBsdxBaseline`
3. `BuildImportPlan`
   - 固定导入闭包
   - 固定 grp 追加目标
   - 固定后续重绑目标
4. `AppendGrpEntries`
5. `SyncProgramMaterial`
6. `RebindMekIndices`
7. `RebindWazIndices`
8. `ImportStaticAssets`
9. `PatchMenuData`
10. `PatchExeCapacities`

## 当前状态总结

当前已经确认：

1. `AKAO` 不是 `BHE -> BSDX` 转译对象
2. 当前 step3 不再做 diff，而是做 `ImportPlan`
3. `AKAO` 迁移的第一阶段应当按 JINKI 闭包整体导入推进
4. 后续真正的业务重点是：
   - grp 追加
   - ProgramMaterial 同步
   - `Akao.mek` 重绑
   - `Akao.waz` 重绑
   - 机体侧 `103` 容量 patch 预留
