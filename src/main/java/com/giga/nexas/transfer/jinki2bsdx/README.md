# JINKI -> BSDX 迁移

## 定位

这个包负责把 `AKAO / moribito_2` 从 `JINKI` graft 到 `BSDX`。

它不是 `BHE -> BSDX` 的语义转译，而是同引擎资源链迁移：

- 源：`src/main/resources/game/jinki`
- 目标：`src/main/resources/game/bsdx`
- 补充静态资源源：`D:\BDY\NeXAS_Resources\jinki_resources`

## 当前迁移模型

当前 `step3/4/6/7` 使用同一套规则：

1. 先从 `Akao.waz` 中抽出当前机体真实使用到的资源链和索引链
2. 对链上的每个资源做“复用还是尾插”的决策
3. 形成统一的 `JINKI源索引 -> BSDX目标索引` 结果表
4. `mek / waz` 内部只按这张结果表做重定向

当前测试侧还额外有一个重要前提：

- `AKAO` 不再占用新的 `MekaGroup[103]`
- 当前测试侧临时复用旧槽位 `MekaGroup[32]`
- 目的不是最终设计，而是先验证“自然进入选人界面的闪退是否来自 meka 顶层索引 103 踩到 exe 固定边界”

当前已纳入链分析的字段：

- `CEventWazaSelect.wazFileNo`
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

## 关键事实

### 1. AKAO 的主 sprite 入口是 `moribito_2.spm`

迁移时不能挂 `AKAO -> akao.spm`，而是要挂：

- `SpriteGroup: 0001 -> moribito_2.spm`

### 2. `ProgramMaterial.grp` 当前已经不只是补外层长度

当前实现分两层：

- 先把 `array1 / array2 / array3` 外层长度追平到当前 `grp` 大小
- 再优先使用外部真源 `ProgramMaterial.grp` 去补能安全映射的 `values`

当前已做：

- `array2` 按 `SeGroup.group/item` 映射回写
- `array3` 按 `BatVoice` 组映射回写
- `array1` 只做“明确安全”的同步  
  说明：`array1.values[*]` 绑定的是 `MapGroup`，当前还没有建立 `MapGroup` 迁移映射，所以只在安全场景下同步

对当前 AKAO 真源再核过一遍后，可以补充一个当前结论：

- `JINKI` 真源里 `moribito_2.spm` 对应的 `ProgramMaterial.array1[33].values = []`
- 所以当前迁移后 `ProgramMaterial.array1[138] = []` 是正确结果
- 也就是说，`array1.values[*]` 的完整 `MapGroup` 映射在“当前 AKAO 这一次 graft”里不是阻塞项

### 3. 修改后的 `grp` 当前已经真的写入产物

现在输出根目录里会包含：

- `MekaGroup.grp`
- `WazaGroup.grp`
- `SpriteGroup.grp`
- `BatVoice.grp`
- `SeGroup.grp`
- `ProgramMaterial.grp`

## Step 8

`ImportStaticAssetsStep` 当前已经实现为真正落盘：

- 输出目录：`src/main/resources/out/jinki2bsdx_assets_<timestamp>`
- 所有资源平铺到输出根目录
- 写入重绑后的 `Akao.mek`
- 写入重绑后的 `Akao.waz`
- 写入修改后的 6 份 `grp`
- 复制当前链上的辅助 `waz`
- 复制当前链上的 `spm`
- 按 `spm.imageData` 补齐当前链实际引用到的图像资源
- 只复制当前链真实关联到的语音和音效

当前已知现状：

- 最新实测缺失静态资源数量：`21`
- 这些缺口当前都是图像资源，不是音频
- 这 `21` 张图已确认是原始游戏资源本身就缺，当前按“已知原版缺口”处理，不再当作本次迁移 bug

## Step 9

`PatchMenuDataStep` 现在已经不只是补两张 dat：

- 补 `Meka.dat`
- 补 `MekaPilot.dat`
- 补 `SelectMekaMenu.dat`
- 补 `MekaPilot.spm`
- 补 `SelectMekaMenuMeka.spm`
- 把菜单 UI 用到的：
  - `M_moribito_2.png`
  - `MG_moribito_2.png`
  - `SG_moribito_2.png`
  一起平铺写入输出根目录

当前验证到的实际结果：

- `SelectMekaMenu.dat`
  - 当前测试侧不再增加第 `71` 个可见槽
  - 而是复用第 `25` 个可见槽
  - 也就是 `SelectMekaMenu.dat[24] = [32, 18, 108]`
  - 槽位位置不变，但这个槽现在绑定到 `MekaGroup[32] = AKAO`
- `MekaPilot.dat`
  - 当前测试侧也不再新增 pilot 行
  - 复用原来的 `MekaPilot.dat[29] = [32]`
- `SelectMekaMenuMeka.spm`
  - 当前测试侧复用原来的 `anim[18]`
  - `anim[18]` 的菜单图已经替成 AKAO
- `MekaPilot.spm`
  - 当前测试侧复用原来的 `anim[29]`
  - `anim[29]` 的 pilot 图已经替成 AKAO

## Step 10

`PatchExeCapacitiesStep` 当前已经完成：

- 汇总本次迁移后的目标容量：
  - `meka`
  - `waza`
  - `sprite`
  - `batVoice`
  - `se`
- `selectMekaMenuRows`

当前测试侧由于不再增加第 `71` 个可见槽，所以：

- `0x14F21F` 本轮保持原值
- 当前产物 `exe patch = false`

当前已明确禁用的位点是：

- `0x56CE3`
- `0x56F9A`

禁用原因已经通过实机矩阵确认：

- `baseline`：能正常启动
- `pac_only`：能正常启动
- `meka_only(只打两处 103 -> 104)`：10 秒内退出，留下 `ConfigNG.dat`，`exitCode = 0xC0000417`
- `menu_only`：能正常启动
- `menu_plus_pac`：能正常启动

也就是说当前测试侧：

- `meka / waza / sprite / batVoice / se / selectMekaMenuRows`
  - 都只汇总需求
  - 未写入 exe

这里还有一个必须保留的风险说明：

- `SpriteGroup.grp / BatVoice.grp / SeGroup.grp / ProgramMaterial.grp` 的顶层数量读取，本次复查后确认是动态按文件 count 读入
- 所以当前没有证据表明“这四份 grp 的顶层条目数”还需要单独加新的 exe patch
- `SelectMekaMenu` 当前项目号区间是 `697..766`
- 这次只是把“70 项上界”抬到了 `71`
- `7A5320` 里：
  - `767..772` 仍然是 generic virtual dispatch
  - `773..784` 已经属于别的菜单对象分支
- 所以当前把“已审安全范围”保守卡在 `76` 项
- 超过 `76` 的可见选机项，pipeline 现在会直接报错，要求继续补 switch/object-id 审计

## Step 11

`PackUpdatePacStep` 当前已经完成：

- 对 step8/9 的输出根目录执行 `PacUtil.pack()`
- 最终产出：`src/main/resources/out/Update3.pac`

## 当前状态

已完成：

- `step1`
- `step2`
- `step3`
- `step4`
- `step5`
- `step6`
- `step7`
- `step8`
- `step9` 的完整菜单链补丁
- `step10` 的容量汇总、选机菜单 70 项上界 patch、机体侧危险 patch 禁用
- `step11` 的 `Update3.pac` 打包
- `runner`
- `test` 启动入口

未完成：

- 非机体侧 `waza / sprite / batVoice / se` 的 exe patch 位点确认
- `ProgramMaterial.array1.values[*]` 的完整通用 `MapGroup` 映射
- `SelectMekaMenu` 超过 `76` 项后的 object-id/switch 扩展

## 当前 review 结论

### 已确认正确

- `step1/2` 的源与基线载入
- `step3/4` 的当前机体资源链提取与源到目标映射
- `step6` 主 `mek` 路由重绑
- `step7` 的 `waz` 主链重绑与 `CEventSe/CEventVoice` 处理
- `step8` 的 `grp + ProgramMaterial + mek/waz/spm` 落盘
- `step9` 的菜单 dat/spm 追加与菜单图片落盘
- `step10` 里 `54F570` 的选机菜单 70 项上界 patch
- `step11` 的 `Update3.pac` 打包

### 当前仍然存在的严重问题

- 非机体侧 `grp` 相关 exe 硬编码位点还没确认完
- `ProgramMaterial.array1.values[*]` 还没有做成通用 `MapGroup` 映射器
- `SelectMekaMenu` 当前只审到了 `76` 项，可继续扩，但必须先补 `773+` 之后的 switch/object-id 链
- `meka 103 -> 104` 的安全扩容位点还没找到，现有两处位点已经确认不能直接用

## 本轮新增结论

这轮进一步收敛出来的核心问题是：

- 即使把 AKAO 放到菜单槽里，只要底层仍然使用新的 `mekaIndex = 103`
- 真实选人路径仍然高度可疑会踩到 exe 内部与 meka 顶层索引相关的固定边界

所以当前测试侧进一步改成：

- 菜单槽仍然复用第 `25` 个可见槽
- `MekaGroup` 也同步复用旧索引 `32`
- 不再让 AKAO 出现在新的 `mekaIndex = 103`

这版的动态验证结果是：

- 强制进入选机菜单时，`objectId 721 -> mekaIndex 32`
- 同时 `MekaGroup[32] = AKAO`

也就是说，当前测试方向已经从“修菜单显示槽”推进到“连 meka 顶层索引一起复用”，用来验证自然选人闪退是否来自 `103` 这一层。
