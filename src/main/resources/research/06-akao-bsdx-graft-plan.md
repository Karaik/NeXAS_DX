# 06. AKAO 并入 BSDX 的迁移方案

## 当前目标

把 `AKAO / moribito_2` 作为一条完整资源链，从 `JINKI` graft 到 `BSDX`，最终输出可打包的 `Update3.pac`。

## 当前规则

当前迁移模型已经对齐为：

1. 先从 `Akao.waz` 中抽当前机体真实使用到的资源链和索引链
2. 再对链上的每个资源做“复用还是尾插”的决策
3. 形成统一的 `JINKI源索引 -> BSDX目标索引`
4. `mek / waz` 内部只消费这张结果表做重定向

当前这一步对 `WazaGroup` 的处理已经收窄为：

- 只把 `JINKI[110] = AKAO` 这个主条目 append 到 BSDX 末尾
- `Akao.waz` 里通过 `wazFileNo = 0..6` 引用到的共享辅助 `waz` 继续复用 BSDX 现有索引
- 不再在 `step4` 扩散追加同名辅助 `WazaGroup` 项
- 主 `AKAO` 和外部实际引用到的共通 `waz`，都会按最终采用的 `.waz.skillList.size()` 重算 `WazaGroup.param`

当前测试侧还额外有一个重要前提：

- `AKAO` 当前仍然 append 到新的 `MekaGroup[103]`
- 当前测试链也继续验证 `mekaIndex = 103` 的真实运行路径
- 当前收敛重点不再是“是否复用旧槽位”，而是 `103` 进入选人确认后的后续加载链

当前已纳入链分析的对象：

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
- 写入修改后的：
  - `MekaGroup.grp`
  - `WazaGroup.grp`
  - `SpriteGroup.grp`
  - `BatVoice.grp`
  - `SeGroup.grp`
  - `ProgramMaterial.grp`
- 复制链上辅助 `waz`
- 复制链上 `spm`
- 按 `spm.imageData` 补齐当前链引用的图像资源
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

当前已实现两层能力：

1. 汇总本次迁移后的目标容量：
   - `meka`
   - `waza`
   - `sprite`
   - `batVoice`
   - `se`
   - `selectMekaMenuRows`
2. 对 `54F570` 里 `SelectMekaMenu` 的固定 `70` 项上界执行 patch

当前测试侧由于不再增加第 `71` 个可见槽，所以：

- `0x14F21F` 本轮保持原值
- 当前产物 `exe patch = false`

当前已确认并默认禁用的位点：

- `0x56CE3`
- `0x56F9A`

禁用原因已经通过实机矩阵确认：

- `baseline`：能正常启动
- `pac_only`：能正常启动
- `meka_only(只打两处 103 -> 104)`：10 秒内退出，留下 `ConfigNG.dat`，`exitCode = 0xC0000417`
- `menu_only`：能正常启动
- `menu_plus_pac`：能正常启动

还没解决的部分：

- `meka / waza / sprite / batVoice / se / selectMekaMenuRows`
  - 都只汇总需求
  - 未写入 exe
- `SpriteGroup.grp / BatVoice.grp / SeGroup.grp / ProgramMaterial.grp` 的顶层数量读取，本次复查后确认是动态按文件 count 读入
- 所以当前没有证据表明“这四份 grp 的顶层条目数”还需要单独加新的 exe patch
- `SelectMekaMenu` 当前项目号是 `697..766`
- `7A5320` 里：
  - `767..772` 仍然是 generic virtual dispatch
  - `773..784` 已经属于别的菜单对象分支
- 所以当前把“已审安全范围”保守卡在 `76` 项
- 超过 `76` 的可见选机项，pipeline 现在会直接报错，要求继续补 switch/object-id 审计

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
