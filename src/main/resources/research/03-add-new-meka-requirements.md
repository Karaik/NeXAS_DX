# 03. BSDX 真正追加一个新机体时必须改哪些东西

## 目标

本文只讨论一种前提：

- **不是替换已有机体槽位**
- **而是在 BSDX 里真正追加一个新的机体**

也就是说，本文默认你要做的是：

1. 追加新的 `MekaGroup` 顶层条目
2. 追加新的主 `WazaGroup` 顶层条目
3. 追加新的主 `SpriteGroup` 顶层条目
4. 如有独立语音，再追加新的 `BatVoice` 顶层条目
5. 如有新的程序音效组，再考虑 `SeGroup` 与 `ProgramMaterial`
6. 让这个新机体不仅战斗能跑，而且必要时能出现在选机菜单

本文不再以“复用旧索引 / 覆盖旧槽位”作为默认路径。

## 证据来源

### Java / 资产链

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

### 关联文档

- `src/main/java/com/giga/nexas/dto/bsdx/grp/BsdxGrpCatalog.notes.md`
- `src/main/java/com/giga/nexas/dto/bsdx/grp/groupmap/ProgramMaterialGrp.notes.md`
- `src/main/resources/research/01-meka-weapon-over-153-crash-analysis.md`
- `src/main/resources/research/02-meka-weapon-153-patch-plan.md`

## 结论先行

真正追加一个新机体时，最少要处理下面几类内容：

### 一组主体资源

1. 新的 `mek`
2. 新的主 `waz`
3. 新的主 `spm`

### 三个主注册表

1. `MekaGroup.grp`
2. `WazaGroup.grp`
3. `SpriteGroup.grp`

### 依赖闭包

1. 主 `waz` 继续引用到的其他 `waz`
2. 主 `waz` 继续引用到的其他 `spm`
3. 新机体主 `spm`、依赖 `spm` 所需的外部图片

### 条件性资源

1. 如果有独立语音：
   - `BatVoice.grp`
   - 语音文件
2. 如果有新的程序音效目标：
   - `SeGroup.grp`
   - 音效文件

### 追加顶层条目后的联动文件

1. `ProgramMaterial.grp`
   - 追加 `SpriteGroup` 顶层条目时，要同步 `array1`
   - 追加 `BatVoice` 顶层条目时，要同步 `array3`
   - 追加 `SeGroup` 顶层条目时，要同步 `array2`

### 如果还要在菜单里显示

1. `SelectMekaMenu.dat`
2. `MekaPilot.dat`
3. `SelectMekaMenuMeka.spm`
4. `MekaPilot.spm`

## 1. 主体资源链

### 1.1 `mek`

文件：

- `game/bsdx/mek/<base>.mek`

作用：

- 新机体的主数据
- 包含基础属性、武装表、AI、素材块、语音块

在 [Mek.java](d:/Code/NeXAS_DX/src/main/java/com/giga/nexas/dto/bsdx/mek/Mek.java) 里，最关键的跨文件字段有两个：

- `mekBasicInfo.wazFileSequence`
- `mekBasicInfo.spmFileSequence`

### 1.2 主 `waz`

文件：

- `game/bsdx/waz/<base>.waz`

作用：

- 机体主技能脚本

注意：

- `mekBasicInfo.wazFileSequence`
  - 指向的是 `WazaGroup.grp` 的顶层条目索引
- `MekWeaponInfo.wazSequence`
  - 指向的是主 `waz` 内部的 `skillList` 序号

这两个不能混淆。

### 1.3 主 `spm`

文件：

- `game/bsdx/spm/<base>.spm`

作用：

- 机体主精灵

对应关系：

- `mekBasicInfo.spmFileSequence`
  - 指向 `SpriteGroup.grp` 顶层条目索引

## 2. 三个主注册表必须追加

### 2.1 `MekaGroup.grp`

作用：

- 机体注册表
- 机体 codeName / mekaName 的顶层索引空间

反汇编证据：

- [65B040.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/65B040.c)
  - `CMekaGroupManager` 读取 `MekaGroup.grp`
  - 先读 count，再动态加载
- [655730.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/655730.c)
  - 单个 `CMekaGroup` 的读取

这说明：

- `MekaGroup.grp` 文件协议本身支持顶层 count 增加

新增机体时必须做：

- 追加新的 `mekaName`
- 追加新的 `mekaCodeName`

### 2.2 `WazaGroup.grp`

作用：

- 主 `waz` 注册表

反汇编证据：

- [639270.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/639270.c)
  - `CWazaGroupManager` 读取 `WazaGroup.grp`
- [637670.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/637670.c)
  - 单个 `CWazaGroup` 读取

新增机体时必须做：

- 追加新的主 `wazaName`
- 追加新的主 `wazaCodeName`
- 追加新的 `wazaDisplayName`

然后让：

- `mekBasicInfo.wazFileSequence`
  - 指向这个新条目

### 2.3 `SpriteGroup.grp`

作用：

- 主 `spm` 注册表
- 也是很多 `CEventSprite` 的索引空间

新增机体时必须做：

- 追加新的 `spriteFileName`
- 追加新的 `spriteCodeName`

然后让：

- `mekBasicInfo.spmFileSequence`
  - 指向这个新条目

## 3. 真实样例：KOU 这条链怎么对上

真实数据里：

- `Kou.mek`
  - `wazFileSequence = 9`
  - `spmFileSequence = 9`

在对应 `.grp` 里：

- `WazaGroup[9] = KOU -> Kou.waz`
- `SpriteGroup[9] = KOU -> kou.spm`
- `MekaGroup[0] = KOU`

这说明：

- `mek` 本身并不直接写文件名
- 它是通过 `WazaGroup / SpriteGroup` 的索引去挂接主资源

新增新机体时，你要复制的不是“文件名关系”，而是：

- **索引关系**

## 4. `MekWeaponInfo.wazSequence` 的真实含义

这点要单独强调，因为最容易误解。

`MekWeaponInfo.wazSequence` 不是：

- `WazaGroup.grp` 索引

它是：

- 当前主 `waz` 文件内部的 `skillList` 索引

例如 `Kou.mek.json` 里：

- `BODYBLOW -> wazSequence = 12`
- `STRAIGHT -> wazSequence = 15`

它们的意思是：

- `Kou.waz.skillList[12]`
- `Kou.waz.skillList[15]`

所以新增机体时：

- 先要有一个新的主 `waz`
- 再让 `mek` 里的每个武装指向这份主 `waz` 的正确技能序号

## 5. 依赖闭包不是可选项

### 5.1 `waz` 会继续拉别的 `waz`

项目代码：

- [TransferDependencyCollector.java](d:/Code/NeXAS_DX/src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/TransferDependencyCollector.java)

规则：

- `CEventWazaSelect`
  - 会继续引用其他 `waz`

### 5.2 `waz` 会继续拉别的 `spm`

同一份代码里：

- `CEventSprite.spmFileSequence`
  - 会继续引用其他 `spm`

### 5.3 KOU 的真实闭包

对 `kou.waz` 做真实依赖统计，结果是：

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

这意味着：

- 真的追加新机体时，不可能只加 3 个主文件就结束
- 你必须把主 `waz` 的整个闭包都补齐

## 6. 外部图片 / 音频资产也要齐

项目代码：

- [StaticAssetCopier.java](d:/Code/NeXAS_DX/src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/StaticAssetCopier.java)

它会从下面这些字段抽出外部文件名：

- `SPM.imageData.imageName`
- `BatVoice.voiceFileName`
- `SeGroup.seFileName`

所以：

### 新的 `spm`

- 不光要有 `.spm`
- 里面引用到的图片也要存在

### 新的 `BatVoice`

- 语音文件要存在

### 新的 `SeGroup`

- 音效文件要存在

## 7. 独立语音要怎么追加

如果新机体要有独立语音组：

- 必须追加 `BatVoice.grp` 顶层 `voiceList` 条目

匹配规则在项目里已经写死：

- `characterCodeName == mekaCodeName`

真实样例：

- `BatVoice.voiceList[0] = KOU`
  - `voice[0] = AT_A01 / Kou_a0101`

新增时你至少要保证：

1. 新的 `characterCodeName`
2. 该 codeName 与机体 codeName 一致
3. 组内 `voices[*]`
4. 对应语音文件存在

## 8. 新增音效时的 `SeGroup`

如果新的 `waz` / `CEventSe` 需要引用新的音效目标：

- 你就要改 `SeGroup.grp`

分两种情况：

### 只往已有 `SeGroup` 大组里加 `seItems`

- 这是相对简单的
- 重点是新增 item 和对应外部音效文件

### 新增整个 `SeGroup` 顶层大组

这就不是只改 `SeGroup.grp` 了，还要同步：

- `ProgramMaterial.grp.array2`

原因已经在：

- `ProgramMaterialGrp.notes.md`

里确认过：

- `array2.size == SeGroup.seList.size`

## 9. 追加顶层条目时，`ProgramMaterial.grp` 是硬联动

这是“真正新增”最容易漏掉的点。

### 9.1 新增 `SpriteGroup` 顶层条目

要同步：

- `ProgramMaterial.array1`

原因：

- `array1.size == SpriteGroup.spriteList.size`

### 9.2 新增 `BatVoice` 顶层条目

要同步：

- `ProgramMaterial.array3`

原因：

- `array3.size == BatVoice.voiceList.size`

### 9.3 新增 `SeGroup` 顶层条目

要同步：

- `ProgramMaterial.array2`

原因：

- `array2.size == SeGroup.seList.size`

这三条都是当前已确认的跨文件位置绑定，不是推测。

## 10. 如果要出现在选机菜单里，必须追加哪些菜单资源

这部分不是战斗链，而是 UI 链。

### 10.1 exe 侧直接加载哪些菜单文件

反汇编：

- [54F570.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/54F570.c)
  - `CSelectMekaMenu`
- [53D0C0.c](d:/Code/NeXAS_DX/src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/53D0C0.c)

可以直接看到它们会加载：

- `SelectMekaMenu.dat`
- `MekaPilot.dat`
- `SelectMekaMenu.spm`
- `SelectMekaMenuMeka.spm`
- `MekaPilot.spm`

所以：

- 不是只加 `MekaGroup.grp` 就能在菜单里出现
- 菜单的数据表和菜单 SPM 也要一起补

### 10.2 `SelectMekaMenu.dat`

真实数据前几行：

- `[0, 0, -1]`
- `[1, 5, 87]`
- `[2, 10, 86]`

项目代码：

- `UiSpmReplacer.resolveSelectMenuAnimIndex(...)`

已明确：

- 第 0 列是 `mekaIndex`
- 第 1 列是 `animIndex`
- 第 2 列尚未完全解码，但菜单逻辑会用到

新增新机体要进菜单：

- 至少要新增一行新的 `mekaIndex -> animIndex`

### 10.3 `MekaPilot.dat`

真实数据：

- 只有 1 列
- `rows = 70`

前几行：

- `[0]`
- `[1]`
- `[5]`

这说明：

- 它是“菜单机体索引子集”
- 不是完整 `MekaGroup.grp` 镜像

新增新机体要进菜单：

- 也要同步给 `MekaPilot.dat` 加一条

### 10.4 `SelectMekaMenuMeka.spm`

真实数据：

- `anims = 98`
- `images = 196`

样例：

- `anim[0] = 門倉甲/カゲロウ：KOU KADOKUERA/SHADOW WOLF`

这说明：

- 每个菜单机体项都有对应 anim

新增新机体时：

- 通常要给这里追加新的 anim/page/image

### 10.5 `MekaPilot.spm`

真实数据：

- `anims = 70`
- `images = 99`

样例：

- `anim[0] = 甲`

这说明：

- pilot 菜单 UI 也是独立资源链

新增新机体时：

- 通常也要追加对应 anim/page/image

## 11. 真实追加的 checklist

### 战斗链最少需要

1. 新的 `mek`
2. 新的主 `waz`
3. 新的主 `spm`
4. `MekaGroup.grp` 新条目
5. `WazaGroup.grp` 新条目
6. `SpriteGroup.grp` 新条目
7. `mek.wazFileSequence` 指向新 `WazaGroup` 条目
8. `mek.spmFileSequence` 指向新 `SpriteGroup` 条目
9. 主 `waz` 的 `CEventWazaSelect` 依赖闭包齐
10. 主 `waz` 的 `CEventSprite` 依赖闭包齐
11. 所有相关外部图片文件齐

### 如果有独立语音

12. `BatVoice.grp` 新条目
13. `characterCodeName == mekaCodeName`
14. 所有语音文件齐
15. 同步 `ProgramMaterial.array3`

### 如果有新的顶层音效组

16. `SeGroup.grp` 新顶层条目
17. 所有音效文件齐
18. 同步 `ProgramMaterial.array2`

### 追加 `SpriteGroup` 顶层条目时

19. 同步 `ProgramMaterial.array1`

### 如果要进菜单

20. `SelectMekaMenu.dat`
21. `MekaPilot.dat`
22. `SelectMekaMenuMeka.spm`
23. `MekaPilot.spm`

## 12. 当前最稳妥的工程认识

真正追加一个新机体，不是“扔 3 个主文件进去”这么简单，而是：

- **把 `mek -> waz/spm -> grp -> ProgramMaterial -> dat/spm 菜单资源 -> 外部图片/音频` 这一整条索引链全部补齐。**

其中最容易漏掉、但最关键的有两组：

1. `ProgramMaterial.grp`
   - 因为它和 `SpriteGroup/BatVoice/SeGroup` 顶层数量绑死
2. 选机菜单资源
   - 因为 exe 直接加载 `SelectMekaMenu.dat / MekaPilot.dat / SelectMekaMenuMeka.spm / MekaPilot.spm`
