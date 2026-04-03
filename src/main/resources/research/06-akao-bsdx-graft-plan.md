# 06. AKAO 并入 BSDX 的迁移方案

## 目标

本方案讨论的不是 `BHE -> BSDX` 转译，而是把 `JINKI` 里的 `AKAO / moribito_2` 作为同引擎资源，正式 graft 到 `BSDX`。

这件事的本质是：

- 以 `game/jinki` 作为真源
- 把 `AKAO` 作为新角色追加进 `BSDX`
- 对 `grp`、`mek`、`waz`、`ProgramMaterial`、exe 容量链做联动处理

## 输入源

当前 pipeline 的正式输入只认包内资源：

- `src/main/resources/game/jinki/grp`
- `src/main/resources/game/jinki/dat`
- `src/main/resources/game/jinki/mek`
- `src/main/resources/game/jinki/spm`
- `src/main/resources/game/jinki/waz`

项目外资源：

- `D:\BDY\NeXAS_Resources\jinki_resources`

当前只作为后续静态资源补充源，不作为主迁移 pipeline 输入。

## 最小迁移闭包

`mek`

- `Akao.mek`

`waz`

- `Akao.waz`
- `Bomb.waz`
- `Effect.waz`
- `Tama01.waz`
- `Tama02.waz`
- `Tama03.waz`
- `Tama04.waz`
- `Tama05.waz`

`spm`

- `moribito_2.spm`
- `C_moribito_2.spm`
- `G_moribito_2.spm`
- `M_moribito_2.spm`
- `Fire.spm`

`grp`

- `MekaGroup.grp`
- `WazaGroup.grp`
- `SpriteGroup.grp`
- `BatVoice.grp`
- `SeGroup.grp`

`dat`

- `Meka.dat`
- `MekaPilot.dat`

## 关键结论

### 1. `AKAO` 的主 sprite 入口是 `moribito_2.spm`

迁移时不能追加 `AKAO -> akao.spm`。

正确做法是：

- 在 `SpriteGroup` 里挂 `0001 -> moribito_2.spm`
- 然后把 `Akao.mek` 和 `Akao.waz` 里引用主 sprite 的位置重绑到新的目标 sprite 索引

### 2. `Mek` 和 `Waz` 的重绑层次不同

`Akao.mek` 当前已经确认要改：

- `mekBasicInfo.wazFileSequence`
- `mekBasicInfo.spmFileSequence`

`Akao.waz` 当前已经确认要改：

- `CEventWazaSelect.wazFileNo`
- `CEventSprite.spmFileSequence`

说明：

- `MekWeaponInfo.wazSequence` 仍然解释为 `Akao.waz` 内部 skill 索引
- `CEventWazaSelect.wazSequenceNo` 仍然解释为目标 `waz` 文件内部 skill 索引

### 3. `ProgramMaterial` 只先同步外层长度

当前这条 `AKAO` 线需要同步：

- `ProgramMaterial.array1`
- `ProgramMaterial.array3`

第一轮目标不是填业务值，而是先保证长度与 `SpriteGroup` / `BatVoice` 对齐。

### 4. exe patch 要留到主链最后

原因：

- 前面的 `grp / mek / waz / ProgramMaterial` 仍然可能失败
- 如果 exe 提前落盘，会留下“看起来像成功产物”的半成品

当前已经改成：

- 前置数据链成功后
- 最后一步才输出 patched exe

## 正式主链

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

## 各步骤职责

### Step 1. `DeserializeJinkiPackageStep`

把 `game/jinki` 里的 `grp/dat/mek/spm/waz` 反序列化成 `JinkiPackageBundle`。

### Step 2. `LoadBsdxBaselineStep`

把 `game/bsdx` 的基线资源载入为 `BsdxBaselineBundle`，供后面追加和重绑使用。

### Step 3. `BuildImportPlanStep`

这一层已经不是 diff。

它的职责是生成 `JinkiImportPlan`，明确：

- 本次必须导入的 `mek/waz/spm` 文件闭包
- 后面要追加哪些 `grp` 顶层条目
- 后面要同步哪些 `ProgramMaterial` 外层数组
- 后面要重绑哪些 `mek/waz` 字段
- JINKI 源侧 `sprite/waz` 索引映射
- BSDX 基线侧 `sprite/waz` 索引映射

这里特别重要的是：

- `sourceWazIndexByFileName`
- `targetWazIndexByFileName`
- `sourceSpriteIndexByFileName`
- `targetSpriteIndexByFileName`

因为 `step7` 的 `CEventWazaSelect / CEventSprite` 重绑，就直接吃这四组数据。

### Step 4. `AppendGrpEntriesStep`

当前策略：

- 同名就复用
- 否则尾插
- 不占 `existFlag=0` 空槽

当前 `AKAO` 跑出来的目标索引是：

- `MekaGroup = 103`
- `WazaGroup = 110`
- `SpriteGroup = 138`
- `BatVoice = 30`

### Step 5. `SyncProgramMaterialStep`

当前只同步外层长度：

- `array1 -> SpriteGroup.size()`
- `array2 -> SeGroup.size()`
- `array3 -> BatVoice.size()`

### Step 6. `RebindAkaoMekStep`

当前做法是“总装配器 + 分片重建器”。

已经真正回写：

- `mekBasicInfo.wazFileSequence -> 110`
- `mekBasicInfo.spmFileSequence -> 138`

其他分片当前保持：

- 显式深拷贝
- 保留原语义
- 不在没有证据时乱改内部编号

### Step 7. `RebindAkaoWazStep`

这一层现在已经不是简单占位，而是：

- 先构建 `WazRebindContext`
- 再按 `Waz -> Skill -> Phase -> Unit -> Object` 逐层重建
- 对叶子对象和带嵌套 unit 的对象分开处理

当前已经明确处理的重绑点：

- `CEventWazaSelect.wazFileNo`
- `CEventSprite.spmFileSequence`

当前已经明确处理的深层结构点：

- `SkillInfoUnknown`
- `CEventSe.byteDataList`
- `CEventVoice.byteDataList`
- 所有带 `*UnitList` 的对象，都会递归重建内部 `data`

这一步的关键原则是：

- 不能只做深拷贝
- 必须递归进入 `CEventEffect` 一类对象内部的 `data`
- 只对已经确认语义的外部编号做重绑

### Step 8. `ImportStaticAssetsStep`

当前仍是占位。

### Step 9. `PatchMenuDataStep`

当前仍是占位。

### Step 10. `PatchExeCapacitiesStep`

当前做法是：

- 把目标 exe 读成 `byte[]`
- 按固定绝对偏移覆写
- 输出到 `src/main/resources/out`
- 文件名格式是 `原名_时间戳.exe`

## 当前实现状态

已完成：

- `step1`
- `step2`
- `step3`
- `step4`
- `step5`
- `step6`
- `step7` 的主装配骨架和关键索引重绑

未完成：

- `step8`
- `step9`
- `step10` 更通用的统一容量策略

## 当前建议

下一步优先级：

1. 继续把 `step7` 里更多已证实的外部编号点补齐
2. 再接 `step8` 静态资源落盘
3. 然后做 `step9` 菜单层补丁

不建议当前回头把 `step7` 改成大杂烩式硬改。
应该继续保持：

- 主装配器明确
- 每一层都能看出改了什么
- 对没有证据的字段暂时不动

## 当前修正口径

当前 `step3/4/7` 已按下面这个迁移模型修正：

1. 从 `JINKI` 的 `Akao.waz` 中抽出当前机体实际使用的资源链  
   当前已覆盖：
   - `wazFileNo`
   - `spmFileSequence`
   - `CEventSe` 的 `segroup/item`

2. 在 `step4` 上对链上的每个资源做“复用或尾插”的决策

3. 生成统一的 `JINKI源索引 -> BSDX目标索引` 结果表

4. `step6/7` 只消费这张结果表做重定向  
   不再直接把 BSDX 基线旧索引表当成最终目标表
