# ProgramMaterialGrp 关系笔记（BSDX）

## 1. 这块是什么

`ProgramMaterial.grp` 不是一个独立自足的随机配置表。

在 BSDX 里，它更像是三组“跨文件索引绑定表”：

- `array1`
  - 绑定精灵图相关资源
- `array2`
  - 绑定音效相关资源
- `array3`
  - 绑定语音相关资源

这里的关键不是“它保存了一串整数”，而是：

- 顶层下标 `i`
  - 先绑定到另一份 `.grp` 的第 `i` 个大项
- `values[*]`
  - 再绑定到该大项内部的具体条目，或绑定到另一个资源表的条目

## 2. 证据来源

### 2.1 项目代码

- `src/main/java/com/giga/nexas/dto/bsdx/grp/groupmap/ProgramMaterialGrp.java`
- `src/main/java/com/giga/nexas/dto/bsdx/grp/parser/impl/ProgramMaterialGrpParser.java`
- `src/main/java/com/giga/nexas/dto/bsdx/grp/groupmap/SpriteGroupGrp.java`
- `src/main/java/com/giga/nexas/dto/bsdx/grp/groupmap/SeGroupGrp.java`
- `src/main/java/com/giga/nexas/dto/bsdx/grp/groupmap/BatVoiceGrp.java`
- `src/main/java/com/giga/nexas/dto/bsdx/grp/groupmap/MapGroupGrp.java`
- `src/test/java/com/giga/nexas/bsdx/TestProgramMaterialRelations.java`

### 2.2 真实样本

- `src/main/resources/game/bsdx/grp/ProgramMaterial.grp`
- `src/main/resources/game/bsdx/grp/SpriteGroup.grp`
- `src/main/resources/game/bsdx/grp/SeGroup.grp`
- `src/main/resources/game/bsdx/grp/BatVoice.grp`
- `src/main/resources/game/bsdx/grp/MapGroup.grp`

### 2.3 辅助参考

- `src/main/java/com/giga/nexas/dto/bhe/grp/memo/memo.txt`
- `src/main/resources/ida-reverse/bhe/export-for-ai/decompile/705AE0.c`
- `src/main/resources/ida-reverse/bhe/export-for-ai/decompile/726230.c`

## 3. 顶层数量关系

当前真实 BSDX 样本中，可以直接验证出：

- `ProgramMaterial.array1.size() == SpriteGroup.spriteList.size() == 138`
- `ProgramMaterial.array2.size() == SeGroup.seList.size() == 38`
- `ProgramMaterial.array3.size() == BatVoice.voiceList.size() == 30`

这说明三段数组的顶层槽位不是自由长度，而是按其他 `.grp` 的顶层条目数量对齐。

## 4. 三段数组分别绑定什么

### 4.1 `array1`

`array1[i]` 的顶层槽位绑定：

- `SpriteGroup.spriteList[i]`

`array1[i].values[*]` 的内部值绑定：

- `MapGroup.groupList[value]`

所以它的实际含义更接近：

- “某个特殊精灵素材，会在哪些 map group / 场景资源组里被程序侧引用”

这不是：

- “另一个 sprite 的索引”
- “同一个 sprite 组里的子项索引”

### 4.2 `array2`

`array2[i]` 的顶层槽位绑定：

- `SeGroup.seList[i]`

`array2[i].values[*]` 的内部值绑定：

- `SeGroup.seList[i].seItems[value]`

所以它的实际含义更接近：

- “这个 SE 大组里，哪些具体音效条目会被 ProgramMaterial 当成程序保留素材使用”

### 4.3 `array3`

`array3[i]` 的顶层槽位绑定：

- `BatVoice.voiceList[i]`

`array3[i].values[*]` 的内部值绑定：

- `BatVoice.voiceList[i].voices[value]`

但要注意：

- 当前 BSDX 真实样本里 `array3[*]` 全空
- 所以顶层绑定已由数量关系确认
- 内层 `values -> voices[value]` 的语义属于“结构上成立，但当前样本没有非空正例可直接举证”

## 5. 具体例子

### 5.1 `array1` 的例子

#### 例子 A

- `array1[0]`
  - 顶层先对应 `SpriteGroup[0] = TAMA / Tama.spm`
- `array1[0].values = [39, 45, 72]`
  - 内层再对应：
  - `MapGroup[39] = ASE_L01 / mapASSEMBLER_L01_01`
  - `MapGroup[45] = NPC_L03 / mapNPC_L03_01`
  - `MapGroup[72] = ARK_S02 / mapARK_S01_02`

#### 例子 B

- `array1[3]`
  - 顶层对应 `SpriteGroup[3] = SMOKE / Smoke.spm`
- `array1[3].values = [0,1,2,9,11,26,33,34,35,36,37,38]`
  - 内层几项例如：
  - `MapGroup[0] = PRACTICE03 / map_PRACTICE_L02`
  - `MapGroup[1] = PRACTICE / mapPractice_L01_01`
  - `MapGroup[33] = ARK_L06 / mapARK_L05_02`
  - `MapGroup[36] = MILITARY_L01 / mapMILITARY_L01_01`

#### 例子 C

- `array1[5]`
  - 顶层对应 `SpriteGroup[5] = PIC / Pic.spm`
- `array1[5].values = [82,83,84,85,100,101,102,103,104,105]`
  - 内层对应一串 map group：
  - `MapGroup[82] = ARK_S12 / mapARK_S02_05`
  - `MapGroup[85] = ARK_S15 / mapARK_S02_08`
  - `MapGroup[100] = MILITARY_S08 / mapMILITARY_S01_08`
  - `MapGroup[105] = MILITARY_S13 / mapMILITARY_S01_13`

### 5.2 `array2` 的例子

#### 例子 A

- `array2[36]`
  - 顶层对应 `SeGroup[36] = PROGRAM`
- `array2[36].values = [0,1,2,3,4,5,6,7,8,12,13,14,15,16,17,18,19,21,22,23,24,25]`
  - 内层几项例如：
  - `0 -> DASH01 / P_Dash01b`
  - `7 -> EXCANCEL / P_ExCancel`
  - `21 -> FCFINISH / P_FCFinish_a`
  - `25 -> COMBOFAILURE / P_ComboFailure`

#### 例子 B

- `array2[37]`
  - 顶层对应 `SeGroup[37] = UDAGAWA`
- `array2[37].values = [51,52,53,54,55,56,57,58,59,60,61,62,63,64,65]`
  - 内层几项例如：
  - `51 -> BOMB01 / U_bomb01_b`
  - `52 -> HAHEN01 / U_hahen01`
  - `65 -> HAHEN14 / U_hahen14`

#### 例子 C

- `array2[0]`
  - 顶层对应 `SeGroup[0] = REACTION`
- `array2[0].values = [299,301,302,303,304,305,306,307,309]`
  - 内层几项例如：
  - `299 -> GROUND02 / RE_Ground02`
  - `301 -> WATER01 / RE_Water01`
  - `309 -> NUMA04 / RE_NumaL01`

### 5.3 `array3` 的例子

当前 BSDX 样本没有非空 `array3[i].values`，但顶层对应可以直接看到：

- `array3[0]` 对应 `BatVoice.voiceList[0] = KOU`
- `array3[1]` 对应 `BatVoice.voiceList[1] = RAIN`
- `array3[2]` 对应 `BatVoice.voiceList[2] = NANOHA`
- `array3[10]` 对应 `BatVoice.voiceList[10] = EIZI`
- `array3[29]` 对应 `BatVoice.voiceList[29] = CHRIS_NAVI`

例如：

- `voiceList[0].voices[0] = AT_A01 / Kou_a0101`
- `voiceList[0].voices[1] = AT_A02 / Kou_a0102`
- `voiceList[29].voices[0] = START01 / Chris_Navi0101`

所以这栏当前应理解成：

- “预留给语音组的程序素材索引表”

只是当前 BSDX 的实盘样本里还没有非空值。

## 6. 当前工作口径

后续修改 `ProgramMaterial.grp` 时，请统一按下面的口径理解：

1. 这三段数组都不是自由数组，而是跨文件位置绑定表。
2. 顶层数量必须和目标 `.grp` 的顶层数量同步。
3. 其中 `values[*]` 不是任意数字，而是另一个资源表里的合法索引。
4. 如果改了顶层数量或中间顺序，不同步改 `ProgramMaterial.grp`，游戏非常容易在加载时崩掉。

## 7. 当前已确认与未完全确认

### 已确认

- `array1` 顶层绑定 `SpriteGroup.spriteList`
- `array1.values[*]` 绑定 `MapGroup.groupList`
- `array2` 顶层绑定 `SeGroup.seList`
- `array2.values[*]` 绑定同组内的 `seItems`
- `array3` 顶层绑定 `BatVoice.voiceList`

### 未完全确认

- `array3.values[*]` 在 BSDX 侧虽然结构上应绑定 `voices[*]`，但当前样本里没有非空正例
- `ProgramMaterial` 被运行时消费时，对这些索引的具体业务动机仍需更多反汇编证据补全
