# 04. BSDX 新增机体所需文件与索引链

## 目标

本文回答的问题是：

- 如果我要在 BSDX 里加一个新机体，**必须**改哪些东西？
- 这些东西之间的索引关系是什么？
- 哪些是“只要能进战斗就够”的硬需求？
- 哪些是“要出现在选机菜单里”的 UI 需求？
- 哪些地方可以从 exe 反汇编里证明，而不是只从 Java 工具链猜？

## 先说结论

### 场景 A：只是让新机体能被战斗逻辑加载

最少要保证：

1. 一个新的 `mek`
2. 一个新的主 `waz`
3. 一个新的主 `spm`
4. `MekaGroup.grp` 里有对应机体条目
5. `WazaGroup.grp` 里有对应主 `waz` 条目
6. `SpriteGroup.grp` 里有对应主 `spm` 条目
7. `mek.wazFileSequence` / `mek.spmFileSequence` 指向上面两个 `.grp` 的正确索引
8. 主 `waz` 事件引用到的其他 `waz/spm` 依赖闭包都齐
9. 如果有独立语音或音效，`BatVoice.grp` / `SeGroup.grp` 与外部资源文件也要齐

### 场景 B：还要让这个机体出现在选机菜单里

在场景 A 基础上，额外还要改：

1. `SelectMekaMenu.dat`
2. `MekaPilot.dat`
3. `SelectMekaMenuMeka.spm`
4. `MekaPilot.spm`

### 场景 C：不是替换旧槽位，而是真的新增顶层条目

如果你不是覆盖旧索引，而是**追加**新的顶层 `.grp` 项：

- `SpriteGroup.grp` 追加顶层项
  - 要同步 `ProgramMaterial.grp.array1`
- `BatVoice.grp` 追加顶层项
  - 要同步 `ProgramMaterial.grp.array3`
- `SeGroup.grp` 追加顶层项
  - 要同步 `ProgramMaterial.grp.array2`

## 证据来源

### Java / 资产链路

- `src/main/java/com/giga/nexas/dto/bsdx/mek/Mek.java`
- `src/main/java/com/giga/nexas/dto/bsdx/mek/parser/MekParser.java`
- `src/main/java/com/giga/nexas/dto/bsdx/waz/wazfactory/wazinfoclass/obj/CEventSprite.java`
- `src/main/java/com/giga/nexas/transfer/bhe2bsdx/Bhe2BsdxSingleRunner.java`
- `src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/TransferDependencyCollector.java`
- `src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/StaticAssetCopier.java`
- `src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/UiSpmReplacer.java`
- `src/main/java/com/giga/nexas/transfer/bhe2bsdx/README.md`

### 真实资产

- `src/main/resources/game/bsdx/mek/*.mek`
- `src/main/resources/game/bsdx/waz/*.waz`
- `src/main/resources/game/bsdx/spm/*.spm`
- `src/main/resources/game/bsdx/grp/*.grp`
- `src/main/resources/game/bsdx/dat/SelectMekaMenu.dat`
- `src/main/resources/game/bsdx/dat/MekaPilot.dat`

### 反汇编

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/65B040.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/655730.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/639270.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/637670.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/54F570.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/53D0C0.c`

## 1. 机体本体最少要有哪三个主文件

### 1.1 `mek`

文件：

- `game/bsdx/mek/<base>.mek`

作用：

- 这是机体主数据
- 里面包含基础属性、武装列表、AI、语音块、素材块

关键字段在 [Mek.java](d:/Code/NeXAS_DX/src/main/java/com/giga/nexas/dto/bsdx/mek/Mek.java)：

- `mekBasicInfo.wazFileSequence`
- `mekBasicInfo.spmFileSequence`

### 1.2 主 `waz`

文件：

- `game/bsdx/waz/<base>.waz`

作用：

- 主技能脚本
- 武装条目里的 `wazSequence` 会索引这份 `waz` 的 `skillList`

### 1.3 主 `spm`

文件：

- `game/bsdx/spm/<base>.spm`

作用：

- 机体主精灵

## 2. `mek` 是怎么挂到 `waz/spm/grp` 上的

### 2.1 真实样例：KOU

看真实数据：

- `src/main/resources/mekBsdxJson/Kou.mek.json`

里面有：

- `mekBasicInfo.wazFileSequence = 9`
- `mekBasicInfo.spmFileSequence = 9`

再看：

- `src/main/resources/grpBsdxJson/WazaGroup.grp.json`
- `src/main/resources/grpBsdxJson/SpriteGroup.grp.json`

第 9 项分别是：

- `WazaGroup[9] = KOU -> Kou.waz`
- `SpriteGroup[9] = KOU -> kou.spm`

所以主链是：

```text
Kou.mek
  -> wazFileSequence = 9
  -> WazaGroup[9] = KOU
  -> Kou.waz

Kou.mek
  -> spmFileSequence = 9
  -> SpriteGroup[9] = KOU
  -> kou.spm
```

### 2.2 机体武装条目不是去索引 `WazaGroup.grp`

`MekWeaponInfo.wazSequence` 的含义要单独强调：

- 它不是 `WazaGroup.grp` 的索引
- 它是主 `Kou.waz` 内部的技能序号

例如 `Kou.mek.json` 前几个武装：

- `BODYBLOW -> wazSequence = 12`
- `STRAIGHT -> wazSequence = 15`

它们的真实含义是：

- `Kou.waz.skillList[12]`
- `Kou.waz.skillList[15]`

## 3. 哪些 `.grp` 是最少必须动的

### 3.1 `MekaGroup.grp`

作用：

- 机体注册表
- 保存 `mekaName / mekaCodeName`

反汇编证据：

- [65B040.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/65B040.c)
  - 读取 `MekaGroup.grp`
  - 先读 count，再按 count 动态加载
- [655730.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/655730.c)
  - 单条 `CMekaGroup` 读取

结论：

- 机体条目本身不是 exe 写死 103 项
- 从文件协议上允许增加条目

实际例子：

- `MekaGroup[0] = KOU`
- `MekaGroup[10] = SORA2`
- `MekaGroup[102] = ZAKO215A`

### 3.2 `WazaGroup.grp`

作用：

- 主 `waz` 注册表
- 把 codeName / displayName 映射到实际主 `waz`

反汇编证据：

- [639270.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/639270.c)
  - `CWazaGroupManager` 读取 `WazaGroup.grp`
- [637670.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/637670.c)
  - 单条 `CWazaGroup` 读取

结论：

- `WazaGroup.grp` 的条目数也是动态读取的

实际例子：

- `WazaGroup[9] = KOU -> Kou.waz`
- `WazaGroup[15] = NANOHA -> Nanoha.waz`

### 3.3 `SpriteGroup.grp`

作用：

- 主 `spm` 注册表
- 也给 `CEventSprite.spmFileSequence` 提供索引空间

实际例子：

- `SpriteGroup[9] = KOU -> kou.spm`
- `SpriteGroup[15] = MAKOTO -> makoto.spm`

所以如果你要加机体：

- 主机体 `spm` 必须有对应的 `SpriteGroup` 条目
- 主 `waz` 中事件引用到的其他 `spm` 也必须在这里有条目

### 3.4 `BatVoice.grp`

作用：

- 角色语音组

项目代码证据：

- `Bhe2BsdxSourceDiscovery.findBatVoiceGroupByCode(...)`
- `Bhe2BsdxSingleRunner`

当前逻辑是：

- 用 `characterCodeName == mekaCodeName` 匹配机体语音组

真实例子：

- `BatVoice.voiceList[0] = KOU`
  - `voice[0] = AT_A01 / Kou_a0101`

所以如果新机体要独立语音：

- `BatVoice.grp` 必须有一组新的 `characterCodeName`
- 并且 codeName 要和机体 codeName 一致

## 4. 新机体不只是三个主文件，还要补依赖闭包

### 4.1 `waz` 可能会继续引用其他 `waz`

项目代码：

- `TransferDependencyCollector.collectWazOutputMap(...)`

规则：

- `CEventWazaSelect` 会继续拉入其他 `waz`

### 4.2 `waz` 还会继续引用其他 `spm`

项目代码：

- `TransferDependencyCollector.collectSpmOutputMap(...)`

规则：

- `CEventSprite.spmFileSequence` 会继续拉入其他 `spm`

### 4.3 KOU 的真实依赖闭包

对 `kou.waz` 做实际闭包统计，得到：

#### WAZ 闭包

- `kou`
- `effect`
- `bomb`
- `laser`
- `tama01`
- `tama02`
- `tama03`
- `tama04`
- `tama05`

#### SPM 闭包

- `kou`
- `mark`
- `smoke`
- `tama`
- `elec`
- `pic`
- `bomb`
- `link`
- `fire`
- `wind`
- `c_kou`
- `g_kou`
- `m_kou`

所以“加一个新机体”的真实工作量通常是：

- 不只是 `mek + 主waz + 主spm`
- 而是一整包闭包资源

## 5. 外部资源文件也要跟着齐

Java 侧已有现成证据：

- `StaticAssetCopier.java`

它会从：

- `SPM.imageData.imageName`
- `BatVoice.voiceFileName`
- `SeGroup.seFileName`

抽出外部资源文件名，然后去资产根目录复制。

这说明：

### 如果你新增 `spm`

- 它里面引用的图片文件要在外部资源目录里存在

### 如果你新增 `BatVoice`

- 对应语音文件要存在

### 如果你新增 `SeGroup` 内音效

- 对应音效文件要存在

## 6. 哪些情况必须同步 `ProgramMaterial.grp`

这个和“新增机体本体”是两回事，但如果你**追加**顶层 `.grp` 项，就绕不过去。

已确认关系见：

- `ProgramMaterialGrp.notes.md`

### 必须同步的情况

- 追加 `SpriteGroup.grp` 顶层条目
  - 要同步 `ProgramMaterial.array1`
- 追加 `BatVoice.grp` 顶层条目
  - 要同步 `ProgramMaterial.array3`
- 追加 `SeGroup.grp` 顶层条目
  - 要同步 `ProgramMaterial.array2`

### 不一定要同步的情况

- 只是在原有 `SeGroup` 某个组里追加 `seItems`
  - 不一定需要改 `array2.size`
- 只替换已有 `SpriteGroup` / `BatVoice` 旧索引位置
  - 顶层数量没变，通常不需要扩 `ProgramMaterial`

## 7. 要不要动选机菜单

### 7.1 如果只是战斗可用

可以不动：

- `SelectMekaMenu.dat`
- `MekaPilot.dat`
- `SelectMekaMenuMeka.spm`
- `MekaPilot.spm`

前提是：

- 你不要求这个机体出现在选机界面里
- 只是通过脚本、替换槽位、测试入口、或别的方式加载战斗

### 7.2 如果要在选机菜单里出现

这四个要一起看。

#### `SelectMekaMenu.dat`

真实数据：

- `row = [mekaIndex, animIndex, unknown]`

项目代码证据：

- `UiSpmReplacer.resolveSelectMenuAnimIndex(...)`

它明确把：

- 第 0 列当 `mekaIndex`
- 第 1 列当 `animIndex`

实际样例：

- `row0 = [0, 0, -1]`
- `row1 = [1, 5, 87]`

#### `MekaPilot.dat`

真实数据：

- 只有 1 列
- `rows = 70`

前几行：

- `row0 = [0]`
- `row1 = [1]`
- `row2 = [5]`

说明：

- 它是菜单机体索引子集
- 不是完整 `MekaGroup.grp` 镜像

#### `SelectMekaMenuMeka.spm`

真实数据：

- `anims = 98`
- `images = 196`

样例：

- `anim[0] = 門倉甲/カゲロウ：KOU KADOKURA/SHADOW WOLF`

#### `MekaPilot.spm`

真实数据：

- `anims = 70`
- `images = 99`

样例：

- `anim[0] = 甲`

### 7.3 exe 反汇编证据

选机菜单对象：

- [54F570.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/54F570.c)
  - `CSelectMekaMenu`

其中明确加载：

- `SelectMekaMenu.dat`
- `MekaPilot.dat`
- `SelectMekaMenu.spm`
- `SelectMekaMenuMeka.spm`
- `MekaPilot.spm`

另一条相关路径：

- [53D0C0.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/53D0C0.c)
  - 也会加载 `MekaPilot.dat`
  - `SelectMekaMenu.dat`
  - `MekaPilot.spm`
  - `SelectMekaMenuMeka.spm`

所以：

- 菜单可见这件事不是只靠 `MekaGroup.grp`
- 还依赖 dat + spm 的菜单资源链

## 8. 真正新增与替换旧槽位，哪个更稳妥

### 替换旧槽位

优点：

- 可以复用旧 `MekaGroup/WazaGroup/SpriteGroup/BatVoice` 索引
- 可以少碰 `ProgramMaterial`
- 可以少碰菜单 dat/spm 的总量问题

这是当前最稳妥的方案。

### 真正新增顶层条目

优点：

- 机体是真正新增，不是覆盖旧内容

缺点：

- 顶层索引联动变多
- `ProgramMaterial` 要同步
- 菜单资源要同步
- 若后续机体太多，还会碰到 exe 内武装页 / 菜单页的固定上限问题

## 9. 实际 checklist

### A. 只要战斗能跑

- `mek/<base>.mek`
- `waz/<base>.waz`
- `spm/<base>.spm`
- `MekaGroup.grp` 条目
- `WazaGroup.grp` 条目
- `SpriteGroup.grp` 条目
- `mekBasicInfo.wazFileSequence` 正确
- `mekBasicInfo.spmFileSequence` 正确
- `waz` 依赖闭包中的其他 `waz` 都存在
- `waz` 依赖闭包中的其他 `spm` 都存在
- 如果有独立语音：
  - `BatVoice.grp` 条目
  - 语音文件存在
- 如果有独立音效：
  - `SeGroup.grp` 条目或组内 item
  - 音效文件存在

### B. 还要在选机菜单里可见

在 A 基础上再加：

- `SelectMekaMenu.dat`
- `MekaPilot.dat`
- `SelectMekaMenuMeka.spm`
- `MekaPilot.spm`

### C. 如果追加了顶层 `.grp` 条目

额外检查：

- `ProgramMaterial.array1`
- `ProgramMaterial.array2`
- `ProgramMaterial.array3`

## 最后一句

**新增一个 BSDX 机体，本质上不是“加一个 mek 文件”，而是“把 `mek -> waz/spm -> grp -> 菜单 dat/spm -> 外部图片/音频资源` 这一整条索引链补齐”。**
