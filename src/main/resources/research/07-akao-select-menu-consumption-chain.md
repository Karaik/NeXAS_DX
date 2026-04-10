# 07. AKAO 选人菜单确认链静态消费分析

## 目的

把这次 `AKAO` 在 Hell Mode 选人菜单里的“可 hover、confirm 后崩溃”问题，先从静态数据和伪代码层彻底拆开。

这份文档只回答 3 件事：

1. 当前 pipeline 到底把哪几份菜单数据改成了什么样
2. `SelectMekaMenu.dat / MekaPilot.dat / SelectMekaMenuMeka.spm / MekaPilot.spm` 在伪代码里是怎么被消费的
3. 现在哪些地方已经能确认，哪些地方仍然只是嫌疑点

不在这份文档里直接下最终 crash 根因结论。

## 当前运行现象

当前最新一轮 runtime 行为是：

- 可以正常启动游戏
- 可以手动进入 Hell Mode
- 鼠标移动到第 25 号位（AKAO）时，不再崩溃
- **按下确认后崩溃**

对应最新测试目录：

- `src/main/resources/tmp/baldrsky_20260408_225955/`

对应崩溃日志：

- `src/main/resources/tmp/baldrsky_20260408_225955/crash_v5_log.txt`

当前关键信号仍然是：

- 异常码：`0xC0000417`
- 寄存器：`EBX = 0x67`
- 即：确认后路径里仍然稳定带着 `mekaIndex = 103`

## 当前 pipeline 实际写出的菜单链

这部分只看当前代码和当前产物，不看理想方案。

### 1. `Meka.dat`

当前代码：

- `PatchMenuDataStep.patchMekaDat(...)`

当前策略：

- 不再按 `JINKI MekaGroup` 的局部槽位 `27` 去取 `Meka.dat`
- 直接对比 `JINKI Meka.dat` 和 `BSDX Meka.dat`
- 取出 `JINKI` 相对 `BSDX` 新增的那一行

当前真实数据：

- `BSDX Meka.dat` 没有 `[103,200]`
- `JINKI Meka.dat` 末尾新增行是 `[103,200]`

项目内 JINKI JSON：

- `src/main/resources/datJinkiJson/Meka.dat.json`
- `rowIndexOf103 = 98`
- `row103 = [103, 200]`

当前产物：

- `src/main/resources/out/jinki2bsdx_assets_20260408_225912_071/Meka.dat`
- `rowIndexOf103 = 99`
- `row103 = [103, 200]`

结论：

- 当前 `Meka.dat` 这条链已经不是 donor 复用
- 而是明确吃了 JINKI 新增出来的真实机体行

### 2. `SelectMekaMenu.dat`

当前代码：

- `PatchMenuDataStep.resolveReplacementSlot(...)`
- `PatchMenuDataStep.patchSelectMekaMenuDat(...)`

当前固定策略：

- 复用 BSDX 第 25 个可见槽位，也就是 `row 24`
- donor state 不是取 `row 24` 自己，而是取 `row 23`

当前 BSDX 基线真实数据：

- `src/main/resources/datBsdxJson/SelectMekaMenu.dat.json`

关键行：

- `row 23 = [26, 93, 106]`
- `row 24 = [32, 18, 108]`
- `row 25 = [45, 52, 109]`
- `row 26 = [35, 44, 110]`

当前代码实际取到的是：

- `sourceMekaIndex = 32`，来自 `row 24` 第 1 列
- `selectMenuAnimIndex = 18`，来自 `row 24` 第 2 列
- `selectMenuState = 106`，来自 donor `row 23` 第 3 列

所以当前 patched 结果不是过去那种 `[103,18,108]`，而是：

- `src/main/resources/out/jinki2bsdx_assets_20260408_225912_071/SelectMekaMenu.dat`
- `row 24 = [103, 18, 106]`

当前产物关键行：

- `row 23 = [26, 93, 106]`
- `row 24 = [103, 18, 106]`
- `row 25 = [45, 52, 109]`
- `row 26 = [35, 44, 110]`

结论：

- 当前 `SelectMekaMenu.dat` 已经不是“只改第一列”
- 它现在的实际逻辑是：
  - 第 1 列：改成 `103`
  - 第 2 列：保留 `18`
  - 第 3 列：改成 donor 行 `23` 的 `106`

### 3. `MekaPilot.dat`

当前代码：

- `PatchMenuDataStep.resolveReplacementSlot(...)`
- `PatchMenuDataStep.findPilotRowIndex(...)`
- `PatchMenuDataStep.patchMekaPilotDat(...)`

当前逻辑：

1. 先从 `SelectMekaMenu.dat[24]` 取出 donor `sourceMekaIndex = 32`
2. 再去 BSDX `MekaPilot.dat` 里找“第一列等于 `32`”的行
3. 找到以后，直接把这行改写成 `[103]`

BSDX 基线真实数据：

- `src/main/resources/datBsdxJson/MekaPilot.dat.json`

关键行：

- `row 28 = [64]`
- `row 29 = [32]`
- `row 30 = [97]`
- `row 31 = [61]`

所以当前 donor pilot 行是：

- `pilotRowIndex = 29`

当前产物：

- `src/main/resources/out/jinki2bsdx_assets_20260408_225912_071/MekaPilot.dat`
- `rows = 70`
- `row 29 = [103]`
- `rowIndexOf103 = 29`

而外部 JINKI 真源则是：

- `D:/BDY/NeXAS_Resources/jinki_resources/MekaPilot.dat`
- `rows = 71`
- `rowIndexOf103 = 70`
- 最后一行是 `[103]`

结论：

- 当前 pipeline 的 `MekaPilot.dat` **不是**按 JINKI 真源末尾 append `[103]` 在做
- 它仍然是在：
  - 复用 BSDX donor 行 `29`
  - 然后把 `[32]` 改成 `[103]`

这条是当前链上最明显的 donor 型处理之一。

### 4. `MekaPilot.spm`

当前代码：

- `PatchMenuDataStep.patchMekaPilotSpm(...)`
- `PatchMenuDataStep.replaceTargetUiSpmAnim(...)`

当前源：

- `src/main/resources/spmJinkiJson/M_moribito_2.spm.json`

源真实数据：

- `numAnimData = 1`
- `numImageData = 1`
- `imageData = ["M_moribito_2.png"]`
- `anim[0].patData[0].pageNo = [0]`

当前目标 donor：

- BSDX `MekaPilot.dat` donor 行是 `row 29`
- 当前代码把 `pilotAnimIndex = pilotRowIndex`
- 所以现在目标 anim 槽位就是 **`anim[29]`**

BSDX 基线目标槽位真实数据：

- `src/main/resources/spmBsdxJson/MekaPilot.spm.json`
- `anim[29].animName = ザコ：ピドーコマンダー`
- `anim[29]` 两个 pat：
  - `pat0 -> page [59]`
  - `pat1 -> page [60]`
- 其中：
  - `page 59 -> imageNos []`
  - `page 60 -> imageNos [47] -> hell_meka_zako009b_0001.png`

当前代码做法：

- 复制整个 BSDX `MekaPilot.spm`
- 把 `anim[29]` 的 page/chip/image 链替成源 `M_moribito_2.spm`
- 同时把 `animName` 改成 AKAO 的 pilot 文本

结论：

- 当前 `MekaPilot.spm` 不是 append 新 anim
- 而是明确复用 **BSDX 第 29 个 pilot anim 槽**

### 5. `SelectMekaMenuMeka.spm`

当前代码：

- `PatchMenuDataStep.patchSelectMekaMenuMekaSpm(...)`
- `PatchMenuDataStep.updateTargetUiSpmAnimName(...)`

当前逻辑：

- 不再拿 `G_moribito_2.spm` 去覆盖 page/chip/image
- 只改目标 anim 槽位的 `animName`

当前目标 donor：

- `SelectMekaMenu.dat[24]` 第 2 列是 `18`
- 所以目标菜单机体图槽位是 **`anim[18]`**

BSDX 基线目标槽位真实数据：

- `src/main/resources/spmBsdxJson/SelectMekaMenuMeka.spm.json`
- `anim[18].animName = 009bピドーコマンダー：PIDO COMMANDER\\r`
- `anim[18]` 两个 pat：
  - `pat0 -> page [36] -> image selectmekamenumeka_0037_0001.png`
  - `pat1 -> page [37] -> image selectmekamenumeka_0038_0001.png`

当前源 `G_moribito_2.spm` 真实数据：

- `src/main/resources/spmJinkiJson/G_moribito_2.spm.json`
- `images = ["MG_moribito_2.png", "SG_moribito_2.png"]`
- `anim 0 -> page [0]`
- `anim 1 -> page [1]`

当前代码已经不再用它去覆盖 `SelectMekaMenuMeka.spm` 的图片链。

结论：

- 当前 `SelectMekaMenuMeka.spm` 仍然复用的是 **BSDX 第 18 个菜单机体图 anim 槽**
- 理论上只改 `animName`
- 不改目标槽位原本的 `page/chip/imageName`

## 当前 output 里一个必须单独记的异常点

当前最新输出目录：

- `src/main/resources/out/jinki2bsdx_assets_20260408_225912_071/`

里面实际存在：

- `selectmekamenumeka_0011_0001.png`
- `selectmekamenumeka_0012_0001.png`

但从 BSDX 基线 `SelectMekaMenuMeka.spm` 的第 18 槽静态数据来看，它原本吃的是：

- `selectmekamenumeka_0037_0001.png`
- `selectmekamenumeka_0038_0001.png`

这说明当前 `step9` 还有一层 **菜单图片链不一致**：

- 当前代码的目标逻辑是“保留第 18 槽原图片名”
- 但当前输出根目录实际带出来的却不是这组图片名

这至少说明两件事中的一件仍然没完全讲通：

1. 实际 patched `SelectMekaMenuMeka.spm` 并不是我们按代码意图理解的那个状态
2. `copyMenuSpmImages(...)` 现在复制图片的依据，不等于“当前槽位实际引用到的图片”

注意：

- 当前 `copyMenuSpmImages(...)` 是按整个 `spm.imageData` 全量扫名字
- 不是只按当前被替换的 anim 槽位实际用到的 page/image 去拷

所以：

- 输出目录里出现 `0011/0012`
- **不等于** 第 18 槽真的就在用 `0011/0012`

这是当前菜单链上最明确的一处“代码意图与产物观测仍有不一致”的点。

## 伪代码里的实际消费链

下面只写已经能从反编译里直接对上的部分。

### 1. `sub_791450(case 8)` 是 Hell Mode 选人菜单的真实创建入口

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/791450.c`

当前已经能确认：

- `case 8`
  - 会分配并构造 `CSelectMekaMenu`
  - 实际调用 `sub_54F570(...)`
- 在进入分发前，先执行：
  - `qword_876F58 = a2`

这说明两点：

1. `54F570` 不是孤立旁支，它就是 Hell Mode 选人菜单对象的真实构造入口
2. 当前菜单模式 `a2` 会先被写进全局 `qword_876F58`，后面 `7A5320` 的命令分支解释会依赖它

### 2. `sub_54F570` 会一次性装配 5 份菜单资源

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/54F570.c`

里面明确加载：

- `SelectMekaMenu.dat`
- `MekaPilot.dat`
- `SelectMekaMenu.spm`
- `SelectMekaMenuMeka.spm`
- `MekaPilot.spm`

更重要的是，这个构造函数内部已经能直接确认出三条句柄装配关系：

- `this + 68`
  - 被加载成 `SelectMekaMenu.spm`
- `this + 388`
  - 被加载成 `SelectMekaMenuMeka.spm`
- `this + 280`
  - 被加载成 `MekaPilot.spm`

这说明当前 Hell Mode 选人菜单至少同时持有两条 UI 资源链：

- 菜单项主图链：
  - `SelectMekaMenu.spm`
  - `SelectMekaMenuMeka.spm`
- 详情 / pilot 图链：
  - `MekaPilot.spm`

### 3. `sub_54F570` 创建 70 个菜单项时，直接把第 1、3 列送进 item ctor

还是看：

- `54F570.c`

创建菜单项的核心调用是：

- `sub_552D00(this + 68, this + 388, objectId, row[0], row[2], selectedFlag)`

从这个调用点可以直接读出：

- 第 1 个句柄参数：
  - `this + 68 = SelectMekaMenu.spm`
- 第 2 个句柄参数：
  - `this + 388 = SelectMekaMenuMeka.spm`
- `objectId`
  - 从 `697 + rowIndex` 生成
- `row[0]`
  - 作为第 5 个参数传入
- `row[2]`
  - 作为第 6 个参数传入

而且这段循环一共会创建 `70` 个菜单项，这和当前 `requiredSelectMekaMenuRows = 70` 是对上的。

结论：

- `SelectMekaMenu.dat` 第 1 列和第 3 列，在 item 构造阶段就已经被直接消费
- 这不是“某个晚期逻辑才顺手看一眼”的字段

### 4. `sub_552D00` 会把第 1、3 列和两个 SPM 句柄一起写进 `CSelectMekaMenuItem`

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/552D00.c`

当前已经能直接确认这些写入：

- `this[39] = a4`
  - 当前 item 的 objectId
- `sub_42F090(this + 17, a2)`
  - 把第 1 个 SPM 句柄挂进 item
  - 也就是 item 持有 `SelectMekaMenu.spm`
- `sub_42F090(this + 106, a3)`
  - 把第 2 个 SPM 句柄挂进 item
  - 也就是 item 持有 `SelectMekaMenuMeka.spm`
- `*(this + 432) = a7`
  - 保存 selectedFlag
- `this[104] = a5`
  - 保存 `row[0]`
- `this[105] = a6`
  - 保存 `row[2]`

更关键的是：

- `sub_552D00(...)` 写完这些字段后
- **会立刻调用 `sub_552800(this)`**

也就是说，这 3 列不是“先缓存，晚点再说”，而是在 item 构造阶段就会继续进入后续消费链。

### 5. `sub_552800` 会按当前 item 的 rowIndex 回读第 2 列，并把它喂给 `SelectMekaMenuMeka.spm`

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/552800.c`

关键代码：

- `v3 = *(this + 156) - 697`
- `v31 = *(*v2 + 12 * v3 + 4)`

这两句已经足够静态确认：

- 当前 item 的 rowIndex 不是额外保存的独立字段，而是：
  - `objectId - 697`
- `v31`
  - 就是这条 row 的第 2 个 `int`
  - 也就是 `SelectMekaMenu.dat` 的 **第 2 列**

后续最关键的调用是：

- `sub_70CE10(..., this + 424, v31, v29, 3, 3, 128)`
- `sub_70CE10(..., this + 424, v31, v29, 0, 0, 128)`

这里当前已经能静态确认：

- `this + 424`
  - 就是前面在 `sub_552D00` 里挂进去的 `SelectMekaMenuMeka.spm`
- `v31`
  - 就是 `SelectMekaMenu.dat row[1]`
- `v29`
  - 则来自 selectedFlag 的布尔转换结果

所以当前第二列的消费链不是模糊猜测，而是：

- `SelectMekaMenu.dat row[1]`
- `-> sub_552800(...)`
- `-> sub_70CE10(..., SelectMekaMenuMeka.spm, animIndex = row[1], ...)`

### 6. `sub_70CE10` 这边可以把“第二列 = 菜单图 animIndex”的说法再钉硬一层

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/70CE10.c`

从这个构造函数本身可以读出：

- `sub_42F090(this + 68, a4)`
  - 第 4 个参数 `a4` 是 SPM 句柄 / 基址
- `*(this + 196) = a5`
  - 第 5 个参数 `a5` 会被存进对象，后面继续解释 anim
- `*(this + 332) = a6`
- `*(this + 376) = a7 << 8`
- `*(this + 404) = a8 << 8`

所以把 `552800` 的调用点带进去以后，当前最稳的参数解释是：

- `a4 = this + 424 = SelectMekaMenuMeka.spm`
- `a5 = v31 = SelectMekaMenu.dat 第 2 列`
- `a6 / a7 / a8`
  - 是模式、屏幕偏移或显示状态相关参数

这也就是为什么现在可以更硬地把第二列定成：

- **`SelectMekaMenuMeka.spm` 的目标 animIndex**

### 7. confirm 路径不是“第三列直接进 `7A5320`”，而是“item 给出命令值后再进 `7A5320`”

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/789910.c`
- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/7AAB80.c`

当前能确认的 confirm 主链是：

1. `789910`
   - 会调 `sub_7AAB80(a1)`
2. `7AAB80`
   - 先走 `sub_79AE70(a1)` 做菜单项扫描/切换
   - confirm 条件满足后，取当前对象 `*(a1 + 36)` 的虚函数 `56` 返回值
   - 然后执行 `sub_7A5320(a1, v72)`

这说明更准确的消费链应该写成：

- `SelectMekaMenu.dat` 第 3 列
- 先进入 `CSelectMekaMenuItem.this[105]`
- 后续由当前菜单项对象的虚函数 `56` 给出 command
- confirm 时把这个 command 送进 `sub_7A5320`

所以当前更准确的说法是：

- 不是“第三列原样直接传给 `sub_7A5320`”
- 而是“第三列先进入 item 状态，再由当前 item 在 confirm 路径上产生命令值”

### 8. `103..107` 和 `108..110` 在 `sub_7A5320` 里确实不是同一类动作

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/7A5320.c`

当前可以直接确认：

- `case 103..107`
  - 是一整组并列 case
  - 会结合 `qword_876F58` 的模式位，去更新：
    - `dword_8773D4`
    - `dword_877500`
    - `dword_8776E4`
    - `dword_8776E8`
  - 然后统一走：
    - `sub_467370(dword_BE9124, value)`
    - `(*(*dword_BF2394 + 28))(dword_BF2394)`
- `case 108`
  - 弹“移动（入れ替え）するデータを選択してください”
  - 调 `sub_71F1E0(...)`
  - 修改 `dword_876ECC`
- `case 109`
  - 弹“削除するデータを選択してください”
  - 调 `sub_71F1E0(...)`
  - 修改 `dword_876ECC`
- `case 110`
  - 是和前两项配套的确认 / 返回分支
  - 调 `sub_71F010()`

所以：

- `106` 明显不属于 `108/109/110` 那组特殊提示菜单动作
- 它属于 `103..107` 这组普通命令族

这也解释了为什么之前把第三列 `108 -> 106` 以后，runtime 依然崩：

- 第三列不是完全无关
- 但它显然也不是当前 confirm 崩溃的唯一决定因素

### 9. `qword_876F58` 会影响 `7A5320` 对命令的解释

还是看：

- `791450.c`

入口处有一条非常关键的赋值：

- `qword_876F58 = a2`

这说明：

- `sub_791450(a1, a2, a3)` 的第 2 个参数
- 会直接决定后面 `7A5320` 里很多 case 的解释方式

结合 `7A5320` 的 `103..107` 组，可以看到它会读这些模式位：

- `& 6`
- `& 0x1800`
- `& 0x6000`
- `& 0x18000`

所以当前更准确的 confirm 命令消费链应写成：

- `SelectMekaMenu.dat` 第 3 列
- `-> item 内部命令值`
- `-> 虚函数 56 返回 command`
- `-> sub_7A5320(a1, command)`
- `-> 再结合全局模式 qword_876F58 决定具体分支落点`

### 10. `MekaPilot.spm` 在详情对象链里确实被单独持有

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/550280.c`

这是 `54F570` 末尾创建详情对象时用到的构造函数。

当前已经能直接确认：

- `sub_42F090(this + 68, a2)`
  - 这里的 `a2` 来自 `54F570` 的 `this + 68`
  - 也就是 `SelectMekaMenu.spm`
- `sub_42F090(this + 556, a3)`
  - 这里的 `a3` 来自 `54F570` 的 `this + 280`
  - 也就是 `MekaPilot.spm`

这说明 `MekaPilot.spm` 并不是“可能以后才会被别的路径用到”的松散资源，
而是在 `CSelectMekaMenuDetail` 构造时就已经作为独立句柄被挂进详情对象。

虽然 `550280.c` 当前还不能直接把 `MekaPilot.dat row -> MekaPilot.spm anim` 的最终消费点钉死，
但它至少已经足够证明：

- confirm / 详情链里确实存在一条单独的 `MekaPilot.spm` 持有与展示路径
- 所以把 `MekaPilot.dat[29] -> MekaPilot.spm.anim[29]` 列成当前**重要嫌疑链之一**，是有静态依据的

### 11. `sub_798540` 是“当前菜单项切换 / hover 激活”链

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/798540.c`

当前已经能直接确认：

- 参数 `a2`
  - 不是 command
  - 而是当前要切换到的菜单项索引
  - 因为它会直接去索引：
    - `dword_BF228C`
    - `dword_BF2280`
- `*(this + 36)`
  - 就是当前菜单项对象
- `(*(*v4 + 56))(v4) != a2`
  - 这里只能稳妥地解释成：
    - 当前菜单项对象会通过虚函数 `56` 提供一个“当前项标识值”
    - `798540` 用它来避免重复切到同一个菜单项
  - 但不能在这一步把它武断写死成“必然等于第 3 列”

切换成功后，这段逻辑会做 4 件事：

1. 把 `BF2280[a2]` 那个菜单项对象挂到 `*(this + 36)`
2. 调当前菜单项对象的虚函数 `20`
   - 传参是 `1`
   - 也就是“进入激活 / 选中态”
3. 再读当前对象的虚函数 `208`
4. 再读当前对象的虚函数 `212`
   - 按返回值进入 `case 0..36` 分类分支
   - 并更新一批全局菜单状态，例如：
     - `case 10` 会写：
       - `*(this + 400) = a2`
       - `dword_87740C = a2`

这条链非常重要，因为它把：

- “鼠标移上去 / 当前项切换”

和：

- “按下确认后的 `7AAB80 -> 7A5320`”

明确分成了两段不同的消费链。

### 12. 菜单项进入激活态时，会主动驱动 detail 更新

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/5524B0.c`

这函数没有静态 caller，是典型的虚函数实现；但从成员布局和行为看，它高概率就是：

- `CSelectMekaMenuItem` 的“激活 / 选中状态切换”处理函数

把它和 `798540` 放在一起看，理由会更硬一些：

- `798540` 在切到新当前项后，会立刻调用：
  - `(*(**v5 + 20))(*v5, 1)`
- `5524B0(this, a2)` 的参数形态正好也是：
  - 当前 item 对象
  - 一个布尔式的激活标记 `a2`
- 并且它的函数体里确实在做：
  - 激活 / 非激活显示切换
  - detail 区域刷新

所以这里虽然还没有“vtable 号位 = 20”的硬标注，
但把 `5524B0` 视为当前菜单项的激活处理函数，证据已经足够强。

当前已经能直接确认的行为是：

- `v6 = *(this + 420)`
  - 这里会先按当前对象已有状态去做显示模式判断
- 当 `a2 != 0` 时：
  - 会更新选中态显示
  - 然后取：
    - `v5 = *(dword_BE8634 + 324)`
  - 如果 detail 对象存在，就执行：
    - `sub_550660(v5, *(this + 156) - 697)`

这句非常关键，因为它说明：

- 当前菜单项在被激活时
- 会把自己的 `rowIndex = objectId - 697`
- 直接喂给 `CSelectMekaMenuDetail`

并且同一个函数最后还会执行：

- `sub_54EDC0(dword_BE8634, *(this + 416))`

也就是说：

- 当前菜单项一旦被激活
- 菜单 detail 区域会立刻收到一次和当前 rowIndex 强绑定的更新

### 13. `sub_550660(detail, rowIndex)` 里，`MekaPilot` 消费链已经能直接静态钉死

看：

- `src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/550660.c`

这是目前最有价值的一段，因为它把：

- 当前 rowIndex
- 当前行第 1 列
- 当前行第 3 列
- `MekaPilot.spm`

真的串到了同一条函数里。

当前能直接确认的链是：

1. 先判断：
   - `if (*(this + 156) != a2)`
   - 也就是 detail 当前 rowIndex 变了才重新刷新
2. 然后清空旧 detail：
   - `sub_54FFB0(this)`
3. 再把新 rowIndex 存进去：
   - `*(this + 156) = a2`
4. 再通过：
   - `v3 = *au_re___invalid_parameter_noinfo_65(&v219, a2)`
   - 从内部 3-int 行表里取出当前行

这里 `au_re___invalid_parameter_noinfo_65(...)` 本身也已经能确认：

- 它按 `12 * a3`
- 从某个连续表里取 3 个 `int`
- 即：
  - `row[0]`
  - `row[1]`
  - `row[2]`

`550660` 对这行的消费是：

- `v213 = row[0]`
- 后面又有：
  - `v6 = au_re___invalid_parameter_noinfo_65(&v209, v204)`
  - `v212 = sub_453B30(*(v6 + 8))`

也就是说：

- 当前 row 的第 1 列会被读出来
- 当前 row 的第 3 列也会被继续解释

随后最关键的一步是：

- 在 `dword_BE8634 + 376 ... +380` 这张表里
- 用 `v213` 去查找匹配项
- 找到以后得到 `v214`

这一步的结构非常像：

- 用 `mekaIndex`
- 反查对应的 `pilotRowIndex`

接着就进入 `MekaPilot.spm`：

- `sub_70CE10(v15, *(this + 292), *(this + 152) - 2, (this + 556), v214, 1, -270, 100, 128)`
- `sub_70CE10(v23, *(this + 292), *(this + 152) - 2, (this + 556), v22, 0, -130, 100, 128)`

这里当前已经能直接确认：

- `this + 556`
  - 就是 `MekaPilot.spm`
- 第 5 个参数：
  - `v214` / `v22`
  - 就是喂给 `MekaPilot.spm` 的 anim 索引

还有一个很小的特例：

- 如果 `v213 == 18 || v213 == 19`
- 则 `v22 = 60`

也就是说，除了这个特例以外：

- 当前 row 的第 1 列
- 会先被解释成 `mekaIndex`
- 再被反查成 `pilotRowIndex`
- 最后拿去驱动 `MekaPilot.spm`

这条链现在已经不是推测，而是静态能直接读出来的真实消费链。

再把它和当前两条真实样本对一下，会更直观：

- 正常的第 24 号位：
  - `SelectMekaMenu.dat row 23 = [26, 93, 106]`
  - 激活时传给 `sub_550660(detail, 23)`
  - `550660` 里先取当前 row 的第 1 列，得到 `v213 = 26`
  - 再反查 pilot 行，得到 `pilotRowIndex = 22`
  - 然后去驱动 `MekaPilot.spm anim[22]`
- donor 的第 25 号位：
  - `SelectMekaMenu.dat row 24 = [32, 18, 108]`
  - 激活时传给 `sub_550660(detail, 24)`
  - `550660` 里先取当前 row 的第 1 列，得到 `v213 = 32`
  - 再反查 pilot 行，得到 `pilotRowIndex = 29`
  - 然后去驱动 `MekaPilot.spm anim[29]`

这说明：

- `550660` 这条 detail 更新链
- 和我们前面用 JSON 反查出来的：
  - `26 -> row 22`
  - `32 -> row 29`
- 是完全对得上的

### 14. 这会直接改写当前嫌疑排序

因为现在运行现象已经是：

- hover 不崩
- confirm 后崩

而上面这条链说明：

- `MekaPilot.dat / MekaPilot.spm` 的 detail 更新
- 实际上已经发生在 **hover / 当前项激活** 阶段
- 不是 confirm 之后才第一次发生

所以当前可以更谨慎地说：

- `MekaPilot` donor 内容链仍然不是完全排除
- 但它已经**不再适合作为 confirm-only 崩溃的第一嫌疑**

因为如果这条链有粗暴的索引错位或资源错位，
更自然的现象应该是：

- hover 时就崩

而不是：

- hover 能过
- confirm 才崩

## 用一个真实正常样本回看：第 24 号位

为了避免把当前 `AKAO` 这条链里的 donor 复用误判成“天然错误”，这里单独拿 BSDX 里一个真实可工作的相邻槽位做对照。

这里的“第 24 号位”按人类数数，对应：

- `SelectMekaMenu.dat` 的 `row 23`

它在 BSDX 基线里的真实数据是：

- `row 23 = [26, 93, 106]`

拆开看：

- 第 1 列：`mekaIndex = 26`
- 第 2 列：`selectMenuAnimIndex = 93`
- 第 3 列：`menuState = 106`

### 1. 第 24 号位不是“row 23 -> pilot row 23”

把 `mekaIndex = 26` 去 BSDX `MekaPilot.dat` 里反查，真实结果是：

- `MekaPilot.dat` 中 `26` 所在行是 **`row 22`**

也就是说：

- 第 24 号可见槽位 `row 23`
- 对应的 pilot 行不是 `23`
- 而是 **`22`**

这和当前代码里的：

- `findPilotRowIndex(MekaPilot.dat, sourceMekaIndex)`

是一致的。

换句话说：

- `MekaPilot.dat` 的行号，本来就不是“按可见槽位顺序平铺”
- 而是要先从 `SelectMekaMenu.dat` 第 1 列拿到 `mekaIndex`
- 再去 `MekaPilot.dat` 里按第 1 列反查

这点非常重要，因为它说明：

- 当前 `AKAO` 链里“先从 donor 行取 `sourceMekaIndex = 32`，再反查到 `pilotRowIndex = 29`”这套机制
- **从 BSDX 原始数据结构上看是成立的**

### 2. 第 24 号位的菜单机体图 anim 也和可见槽位行号无关

第 24 号位在 `SelectMekaMenu.dat` 第 2 列给出的是真实：

- `selectMenuAnimIndex = 93`

去 BSDX `SelectMekaMenuMeka.spm` 看：

- `anim[93].animName = 村正`

这说明：

- 第 2 列确实是在直接决定 `SelectMekaMenuMeka.spm` 的目标 anim 槽
- 而不是“默认等于可见槽位行号”

### 3. 第 24 号位的 pilot anim 同样不是可见槽位行号

对上面反查出来的 `pilotRowIndex = 22`，去 BSDX `MekaPilot.spm` 看：

- `anim[22].animName = 景明村正`

这再次说明：

- pilot 图这条链也不是“第 24 号位 -> anim[23]”
- 而是：
  - 先由 `SelectMekaMenu.dat` 第 1 列拿到 `mekaIndex`
  - 再从 `MekaPilot.dat` 反查出 pilot 行
  - 再用这个 pilot 行号去取 `MekaPilot.spm` 的 anim

### 4. 用第 24 号位对照后，当前应该收回的怀疑

基于这个正常样本，现在可以明确收回一条过早的怀疑：

- “当前 `AKAO` 之所以崩，是因为它复用了 BSDX `MekaPilot.dat row 29` 而不是 JINKI 末尾 append 行”

这句话目前**不能成立**。

更准确的说法应该是：

- 当前 `AKAO` 这条链里，`pilotRowIndex = 29` 的取得方式
- **和 BSDX 正常槽位（如第 24 号位）的原始消费方式是一致的**

所以当前更应该怀疑的，不再是：

- “为什么不是 append 到最后一行”

而是：

- donor `row 29 / anim 29` 被改写后的内容，是否满足 `AKAO` 在后续路径里的真实读取要求

## 再用 donor 本身做一遍基线对照：第 25 号位原始槽

为了避免把“AKAO 复用的 donor 链”误判成完全脱离 BSDX 正常结构，再把 BSDX 原始第 25 号位自己也完整对一遍。

BSDX 基线第 25 号位真实数据：

- `SelectMekaMenu.dat row 24 = [32, 18, 108]`

顺着这条链反查：

- 第 1 列 `32` 在 `MekaPilot.dat` 里对应 **`row 29`**
- 第 2 列 `18` 在 `SelectMekaMenuMeka.spm` 里对应 **`anim[18]`**
- `MekaPilot.dat row 29` 再对应 `MekaPilot.spm` 的 **`anim[29]`**

真实 animName：

- `SelectMekaMenuMeka.spm anim[18].animName = 009bピドーコマンダー：PIDO COMMANDER\\r`
- `MekaPilot.spm anim[29].animName = ザコ：ピドーコマンダー`

这条对照非常重要，因为它说明：

- 当前 pipeline 对 `AKAO` 采用的
  - `selectMenuAnimIndex = 18`
  - `pilotRowIndex = 29`
  - `pilotAnimIndex = 29`
- **并不是凭空拼出来的异常关系**
- 而是精确复用了 BSDX 第 25 号位 donor 原本那条菜单消费链

所以当前真正应该怀疑的，不是：

- “为什么会出现 row 29 / anim 29 这种看起来不连续的映射”

而是：

- “把 donor 这条原本服务于 32 号机体的内容改写成 103 之后，内容本身有没有改够”

## 当前链条里已经能确认的事实

1. 当前 output 的 `SelectMekaMenu.dat[24] = [103, 18, 106]`
2. 当前 output 的 `MekaPilot.dat` 不是 append `[103]`，而是把 donor `row 29` 改成了 `[103]`
3. 当前 `MekaPilot.spm` 明确复用 `anim[29]`
4. 当前 `SelectMekaMenuMeka.spm` 明确复用 `anim[18]`
5. `SelectMekaMenu.dat`：
   - 第 1 列和第 3 列在 item ctor 阶段就进对象
   - 第 2 列在 `sub_552800` 里被按 row index 回读
   - 然后被当成 `SelectMekaMenuMeka.spm` 的 animIndex 继续消费
6. confirm 路径是：
   - 当前 item
   - 虚函数 56 给出的是当前 item 的 objectId / 当前项标识值
   - `sub_7A5320(a1, objectId)`
   - 进入 `case 697..766`
   - 再通过当前 item 的第 3 列状态走 `sub_54EE00 -> sub_453B30`
   - 落到“正常可选槽”或“商店/锁定槽”分支
7. `MekaPilot.spm` 在详情对象链里被单独持有，不是松散资源
8. 当前第 25 号位 `row[2] = 106` 时：
   - `sub_453B30(106) -> 0`
   - 所以它会进入正常机体 confirm 分支
   - 而不是 `108/109/110` 那类特殊菜单分支
9. confirm-only 路径里，第一个真正直接按 `mekaIndex = 103` 读表的地方是：
   - `au_re___invalid_parameter_noinfo_28(dword_875DB0, 103)`
   - 紧接着会进入：
   - `sub_4376B0(dword_86CE04, 103)`
   - 然后立刻继续消费返回的 meka 运行时对象

## confirm-only 首次读取到的 `.mek` 子块：`MekVoiceInfo`

这轮继续往 `dword_86CE04[103]` 里细追以后，当前已经能把 confirm-only 第一批真正消费到的 `.mek` 子块钉到：

- `runtimeMek + 216`

顺着伪代码链：

- `7A5320`
  - `sub_673080(103, -1)`
- `673080`
  - `sub_65A8D0(dword_86CE04, 103, -1)`
- `65A8D0`
  - `sub_657F40(runtimeMek, -1)`
- `657F40`
  - `sub_654B90(runtimeMek)`
- `654B90`
  - 首次使用时会执行：
    - `read_mek_voiceInfo(*(runtimeMek + 216), fileName...)`

这说明：

- confirm-only 路径在真正进入机体运行时对象以后
- **第一批懒加载并立刻被消费的子块，不是菜单图，也不是 pilot 图**
- 而是 `.mek` 里的 **`MekVoiceInfo` 运行时对象**

后续 `7A5320` 立刻会做：

- `sub_65BA00(*(runtimeMek + 216), 6, 0)`
- 如果返回值 `> 0`
  - 再调用：
    - `sub_65C020(*(runtimeMek + 216), ..., 6, 0, chosenIndex, ...)`

也就是：

- confirm-only 路径会立刻查询 `MekVoiceInfo.table[6][0]`
- 并从这个单元里随机选一条语音 entry 去继续播放

### 三组真实数据对照

这里不再泛讲整个菜单系统，只对比 confirm 真正会立刻点到的这个单元：`table[6][0]`。

#### 1. 正常 donor 机体：`Muramasa`

文件：

- `src/main/resources/mekBsdxJson/Muramasa.mek.json`

`MekVoiceInfo` 关键形状：

- `version = 19`
- `builtinEmotionCount = 13`
- `emotions = 0`
- `voiceSlots = 1`
- `rows = 13`

confirm 首次查询的单元：

- `table[6][0] =`
  - `{ voiceType = -1, groupId = 68, weight = 25 }`
  - `{ voiceType = -1, groupId = 69, weight = 25 }`
  - `{ voiceType = -1, groupId = 70, weight = 25 }`
  - `{ voiceType = -1, groupId = 71, weight = 25 }`

再对照 `BSDX BatVoice.grp`：

- `MURAMASA` 顶层组在 `voiceList[19]`
- 该组里：
  - `68 -> ENTER_A01 -> Muramasa_09_01`
  - `69 -> ENTER_A02 -> Muramasa_09_02`
  - `70 -> ENTER_A03 -> Muramasa_09_03`
  - `71 -> ENTER_A04 -> Muramasa_09_04`

结论：

- 对正常 donor 来说，`table[6][0]` 这一格就是 **登场 / confirm 语音**

#### 2. BSDX donor 25 号位原机体：`ZAKO009B`

文件：

- `src/main/resources/mekBsdxJson/Zako009b.mek.json`

`MekVoiceInfo` 关键形状：

- `version = -1`
- `rows = 0`
- `voiceSlots = 0`

结论：

- donor 25 号位原机体自己**没有有效 `MekVoiceInfo`**
- 所以 donor 原始 confirm 路径不会走到一条有效的 enter voice 播放链

这也解释了为什么：

- 不能直接拿 donor 25 号位是否能正常 confirm
- 来证明当前 `AKAO` 的 voice 链一定没问题

#### 3. 当前 patched `AKAO`

文件：

- `src/main/resources/tmp/analysis/akao_output_latest.mek.json`

`MekVoiceInfo` 关键形状：

- `version = 20`
- `builtinEmotionCount = 13`
- `emotions = 1`
- `voiceSlots = 1`
- `rows = 14`

confirm 首次查询的单元：

- `table[6][0] =`
  - `{ voiceType = -1, groupId = 46, weight = 25 }`
  - `{ voiceType = -1, groupId = 47, weight = 25 }`
  - `{ voiceType = -1, groupId = 48, weight = 25 }`
  - `{ voiceType = -1, groupId = 49, weight = 25 }`

再对照 `JINKI BatVoice.grp` 的 `AKAO` 组：

- `AKAO` 顶层组在 `voiceList[20]`
- 该组里：
  - `46 -> ENTER_A01 -> Akao_0901`
  - `47 -> ENTER_A02 -> Akao_0902`
  - `48 -> ENTER_A03 -> Akao_0903`
  - `49 -> ENTER_A04 -> Akao_0904`

结论：

- 当前 patched `AKAO` 的 `MekVoiceInfo.table[6][0]` 本身是**自洽的**
- 它确实在表达“confirm 时随机播一条 `AKAO` 的登场语音”

### 当前第一处可坐实的错位点：`Akao.mek -> MekMaterialBlock.entries[*].voiceGroups`

上一轮一度把：

- `Akao_0901..0904.ogg` 缺失

当成最硬的主结论。

但最新 runtime 已经确认：

- 即使用户手动进入 Hell Mode
- 再手动确认第 25 号位
- 游戏依然会在 confirm 后崩溃

所以当前更准确的排序是：

- `step8` 的 enter voice 落盘缺失，**确实是问题，也已经修了**
- 但它**不是当前 confirm 崩溃的第一主根因**

继续把 confirm 预热路径往前收紧以后，现在第一处已经能直接坐实的真错位点是：

- `Akao.mek -> mekMaterialBlock.entries[*].voiceGroups`

更具体地说：

- 当前 `AKAO` patched `.mek` 里，非空材质语音引用仍然保留在 **source group 20**
- 但 `step4` 之后，`AKAO` 的目标 `BatVoice` 组已经 append 到 **target group 30**
- 而 BSDX 里的 **group 20** 现在实际是 `RAIN_NAVI`

也就是说：

- `AKAO` 当前 `.mek` 里保留的 `voiceGroups[20]`
- 在目标侧已经不再是 “AKAO 语音组”
- 而是错误地指向了 `RAIN_NAVI`

#### 1. 正常 BSDX 可选机体对照：`Muramasa`

文件：

- `src/main/resources/mekBsdxJson/Muramasa.mek.json`

当前静态对照结果：

- `Muramasa` 的 `mekMaterialBlock.entries[*].voiceGroups`
- 所有非空引用都落在 **group 19**

再对照 BSDX 的 `BatVoice.grp`：

- `src/main/resources/grpBsdxJson/BatVoice.grp.json`
- `voiceList[19] = MURAMASA`

这说明正常 BSDX 可选机体的材质语音引用模式是：

- `.mek` 里的 `voiceGroups[groupId]`
- 直接落到目标侧该机体自己的 `BatVoice[groupId]`

也就是：

- `Muramasa.mek` 用 `group 19`
- 因为目标侧 `BatVoice[19]` 就是 `MURAMASA`

#### 2. 当前 AKAO 的对照结果

文件：

- `src/main/resources/tmp/analysis/akao_output_latest.mek.json`

当前静态结果：

- `AKAO` 的 `mekMaterialBlock.entries[*].voiceGroups`
- 所有非空引用仍然都落在 **group 20**

而 `step4` 当前已经确认：

- `AKAO` 的目标 `BatVoice` 组 = **30**

所以这里的结论已经可以直接写死成：

- `AKAO` 当前保留的 **group 20** 本来就应该 remap
- 正确目标就是 **group 30**

#### 3. 第一处可坐实的具体错位

当前第一处可以直接落到具体 entry/group/item 的错位是：

- `mekMaterialBlock.entries[7].voiceGroups[20][3] = 19`

为什么这格是第一处：

- 当前 `entries[7].voiceGroups[20]` 开头是：
  - `[16, 17, 18, 19, 20, 21, ...]`
- 目标侧 BSDX 的 `BatVoice[20]` 现在是 `RAIN_NAVI`
- `RAIN_NAVI` 只有 `19` 条 voice
- 所以合法 item index 只有：
  - `0..18`

也就是说：

- 前 3 个值：
  - `16`
  - `17`
  - `18`
  还勉强在范围内
- 一旦读到第 4 个值：
  - `19`
  就已经越界

#### 4. 这格是怎么在 confirm 预热里直接炸掉的

confirm 主链里会执行：

- `sub_673080(dword_CA754C, -1)`
- `sub_65A8D0(dword_86CE04, 103, -1)`
- `sub_657F40(runtimeMek, -1)`

关键点在：

- `sub_657F40`
  - **先**调用 `sub_6553A0(this, 4u)`
  - **后**才去继续读别的 `.mek` 子块

而 `sub_6553A0(this, 4u)` 会把材质里的 voice event 交给：

- `sub_5F0F20(...)`

`sub_5F0F20(...)` 内部会直接做这条校验：

- `itemIndex < 当前 voice group 的 voice 数量`

所以对当前这格来说：

- group 还是错的 `20`
- 当前 group 20 在目标侧实际是 `RAIN_NAVI`
- `itemIndex = 19`
- 但 `RAIN_NAVI` 的合法上界只有 `18`

于是：

- `sub_6553A0 -> sub_5F0F20`
- 会在 confirm 预热阶段直接命中 `_invalid_parameter_noinfo()`

这已经足够单独解释：

- 为什么 hover 不一定崩
- 但 confirm 会稳定崩

## 仍未最终证实

1. 当前 confirm 后崩溃，到底是：
   - `dword_875DB0[103]` 对应的 meka 主表行不满足读取预期
   - `dword_86CE04[103]` 对应的 `Akao.mek` 运行时对象不满足读取预期
   - 还是它们后续拉起的某个子资源（如声音/材质/动作块）仍然错转
   中的哪一个单点造成
2. 当前 output 根目录为什么稳定带出 `0011/0012`，而不是第 18 槽基线的 `0037/0038`
3. `dword_875DB0[103]` 和 `dword_86CE04[103]` 这两条 confirm-only 首批读取链里，究竟是哪一条先偏离了正常机体 donor 的读取预期

## 当前最值得继续追的方向

从这份静态消费链看，当前已经能先收住一个明确结论：

1. **第一处可坐实错位点已经从 `step8` / enter voice 资源，转移到 `Akao.mek -> MekMaterialBlock.voiceGroups`**
   - `AKAO` 当前 `.mek` 里仍保留 source `group 20`
   - 但目标侧正确组已经是 `group 30`
   - 第一处具体越界是：
     - `entries[7].voiceGroups[20][3] = 19`
   - 它会在：
     - `sub_657F40`
     - `-> sub_6553A0(this, 4u)`
     - `-> sub_5F0F20(...)`
     这条 confirm 预热链上直接炸掉

2. **`dword_86CE04[103]` 这条 meka 运行时对象链仍然是当前主战场**
   - 但现在这条链里第一个已经坐实的坏块，不再是泛泛的“运行时对象整体”
   - 而是更具体的：
     - `MekMaterialBlock.PluginEntry.voiceGroups`

3. **`dword_875DB0[103]`、菜单图、`MekaPilot` 继续降级**
   - 它们当前都还没有比 `voiceGroups` 更早、更硬的越界证据
   - 所以优先级继续低于 `MekMaterialBlock.voiceGroups`

## 一句话总结

当前静态证据已经足够说明：

- `SelectMekaMenu.dat` 三列都是真正会被消费的字段
- 当前 pipeline 对第 25 号位的处理不是简单“改一个 mekaIndex”
- 而是形成了一条很具体的 patched 链：
  - `SelectMekaMenu.dat[24] = [103, 18, 106]`
  - `MekaPilot.dat[29] = [103]`
  - `MekaPilot.spm.anim[29]` 被替成 AKAO pilot 图
  - `SelectMekaMenuMeka.spm.anim[18]` 只改 `animName`

当前最该保留的结论是：

- hover/detail 链已经不是当前主战场
- confirm-only 链已经能钉到：
  - 先读 `dword_875DB0[103]`
  - 再进 `dword_86CE04[103]`
- **当前第一处可坐实的真错转点是：**
  - `Akao.mek -> MekMaterialBlock.entries[*].voiceGroups`
- 其中第一处具体错位已经能落到：
  - `entries[7].voiceGroups[20][3] = 19`
- 这格会在：
  - `sub_657F40`
  - `-> sub_6553A0(this, 4u)`
  - `-> sub_5F0F20(...)`
  上直接命中 `_invalid_parameter_noinfo()`

## 证据补充：`dword_875F54` 运行时 meka table

沿着：

- `BaldrSky+0x1e149d`
- `sub_5E13E0`
- `dword_875F54`

继续回溯后，可以把 first fail 的更上游证据链补齐为：

### 1. `dword_875F54` 的角色

`sub_5E13E0` 并不创建这张表，它只是消费它。

在这条函数里，直接可见的 first fail 是：

- `currentIndex = 103`
- `allowedCount = 103`
- 0-based 合法范围只有 `0..102`

因此 `103 < 103` 失败并不是 step6 inner payload 的 first fail，而是更外层 runtime meka table 的边界失败。

### 2. 构造链

这张表的写入/消费链已经能对上：

- `sub_5E13E0`
  - 读取 `dword_875F54 + 4 * (i + 1084 * mekaIndex) + 4`
- `sub_5E1330`
  - 把单条记录写回这张表
- `sub_673010 / sub_673450`
  - 是这张表的批量清空 / 单条写入入口

而真正决定容量和初始化边界的构造函数是：

- `sub_4576F0`

### 3. 103 的直接来源

`sub_4576F0` 里有两处与这张表直接相关的硬编码：

- `0x056CE4`
  - `push 103`
  - 调 `sub_45C5B0(...)`
  - 作用：给 `dword_875F54` 对应的 runtime meka table 预分配 103 条记录

- `0x056F45`
  - `0x0006D090`
  - 即 `4336 * 103`
  - 作用：初始化循环只跑 103 条记录

这说明：

- `dword_875F54` 的 103 不是资源文件自己少了一项
- 而是引擎侧 runtime table 在构造阶段就被做成了 103 条

### 4. patch 与现象变化的对应关系

对这条链补上：

- `0x056CE4: 103 -> 104`
- `0x056F45: 4336 * 103 -> 4336 * 104`

以后，`dword_875F54` 这条 `103 < 103` 的 first fail 被消掉。

对应的现象变化是：

- `confirm` 可通过
- 崩点不再停在选人确认前
- 崩点前移到：
  - 进入练习模式
  - 真正生成机体

这条变化链说明：

- `5F0C40 / 5F0DF0 / 5F0F20` 只作为 breadcrumb
- `dword_875F54` 这一层 first fail 已经被处理
- 新崩点已经进入更后面的机体生成路径

## 追加更新（2026-04-10）

### 这轮具体干了什么

这轮没有再回头怀疑前三处已冻结的 remap fix，而是继续沿着：

- `BaldrSky+0x1e149d`
- `sub_5E13E0`
- `dword_875F54`

去追这张 runtime meka table 的真实来源。

完成的动作有 3 个：

1. 把 `dword_875F54` 的 first fail 现场钉成：
   - `currentIndex = 103`
   - `allowedCount = 103`
   - 0-based 合法范围只有 `0..102`
2. 往回追到 runtime table 的构造函数，确认 103 的直接来源
3. 在 `step10 / PatchExeCapacitiesStep` 里补上缺失的 exe patch，并完成 pipeline + runtime 复测

### 这轮新增的核心发现

`dword_875F54` 这张表的 103，不是来自菜单链，也不是来自 step6 payload，而是引擎侧 `sub_4576F0` 里直接写死的。

当前已经能直接对上的两处常量是：

- `0x056CE4`
  - `push 103`
  - 调的是 `sub_45C5B0(...)`
  - 作用：给 `dword_875F54` 这张 runtime meka table 预分配 103 个 `4336-byte` 记录

- `0x056F45`
  - `0x0006D090`
  - 也就是 `4336 * 103`
  - 作用：初始化循环只跑 103 条记录

也就是说，之前那个：

- `currentIndex = 103`
- `allowedCount = 103`

并不是资源没进来，而是这张运行时表在构造阶段就被做成了 103 条。

### 这轮代码改动

这轮在 `step10 / PatchExeCapacitiesStep` 里新增了两处正式 patch：

1. `0x056CE4`
   - `103 -> 104`
   - 把 runtime meka table 的容量从 103 放到 104

2. `0x056F45`
   - `0x0006D090 -> 0x0006E180`
   - 也就是把初始化边界从 `103 * 4336` 放到 `104 * 4336`

这两处一起补上以后，`dword_875F54` 才同时满足：

- 多分配一条
- 也多初始化一条

这也解释了为什么更早单独碰 `0x056CE4` 会不稳：

- 只扩容
- 不扩初始化边界
- 最后一条记录依然是不完整的

### 这轮测试如何锁住

runner 测试已经补了两个新断言，直接检查 patched exe：

- `0x056CE4 == 0x68`
- `0x056F45 == 0x0006E180`

所以这轮不是只有“分析结论”，而是：

- 代码已改
- 测试已锁

### 这轮 runtime 复测结果

这轮正式 workflow 的新测试目录是：

- `src/main/resources/tmp/baldrsky_20260410_132803/`

复测结果很关键：

- **confirm 已经过去了**
- 新崩点前移到了：
  - 进入练习模式
  - 真正调用机体生成

新 crash 产物：

- `src/main/resources/tmp/baldrsky_20260410_132803/crash_v5_log.txt`
- `src/main/resources/tmp/baldrsky_20260410_132803/crash_dumps/BaldrSky.exe.1240932.dmp`
- `src/main/resources/tmp/baldrsky_20260410_132803/crash_dumps/BaldrSky.exe.1240932.dmp.analysis.txt`

这轮新的关键栈帧已经浮出来：

- `BaldrSky+0x36169b`
- `BaldrSky+0x2d58f1`
- `BaldrSky+0x2d5a2c`
- `BaldrSky+0x2d5da4`
- `BaldrSky+0x2d87aa`
- `BaldrSky+0x2d835c`
- `BaldrSky+0x2c7909`
- `BaldrSky+0x299c37`
- `BaldrSky+0x29b546`
- `BaldrSky+0x29b85c`

### 这轮结果该怎么理解

这轮最重要的结论不是“还崩”，而是：

- `dword_875F54` 这条 `103 < 103` 的 first fail 已经被消掉
- 之前卡在 confirm 入口的那层 runtime meka table 容量问题已经过去了
- 崩点明确前移到“进入练习模式 / 真正生成机体”的下一层

也就是说，这轮不是空转，而是把问题继续往前推了一整段。
