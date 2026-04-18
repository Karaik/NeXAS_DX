# 12. BHE 公共弹幕资源簇接入口径确认清单

> 工作文档。用于记录 `effect / tama01..05 / laser / bomb` 这组 BHE 公共弹幕资源簇接入 `BSDX + JINKI` baseline 的实现口径。
> 每确认一项后，由 Codex 主动更新勾选状态和结论。
> 如果用户回答前后不一致、漏答关键点、或出现新的职责冲突，必须暂停实现并重新对齐，不得推进实现。
> 对应代码入口：
> `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/tsukuyomi/convert/TsukuyomiConvertOverviewStep.java`
> `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/projectile/BheCommonProjectilePrepareStep.java`

## 固定事实

- [x] BHE 公共弹幕资源由 Tsukuyomi 首次接入。
- [x] 单机体移植不重复转换或 append 这组公共资源。
- [x] Tsukuyomi 外层调用保持不变；第 0 步内部只通过 `BheCommonProjectilePrepareStep.prepare(...)` 接入公共弹幕资源层。
- [x] 公共弹幕 WAZ 使用 `bhe_*` 独立目标 entry，不写入 baseline 同名 WAZ。
- [x] `AppendGrpEntriesStep` 的 key-based merge 是 JINKI 风格，不适用于 BHE common projectile cluster。
- [x] 公共弹幕接入层不做静态 PNG/OGG 复制；静态 sidecar 属于 output/import 阶段。

## 命名空间与容量策略

- [x] BHE 侧迁入的公共资源文件进入 BSDX/JINKI baseline 时使用 `bhe_*` 目标命名空间。
  - 目标命名空间用于避免覆盖 baseline 中已有的同名资源文件。
  - 运行时引用以 GRP 顶层 index、WAZ 内部 skill index、SPM 内部 anim/page/image index 为准，不使用文件名或 codename 作为运行时 key。
  - `bhe_*` 是目标侧资源 entry/file 的隔离规则，不改变 BHE 源数据中 index 的语义。
- [x] `bhe_*` 命名规范：前缀固定小写 `bhe_`，资源主体保留 BHE 源文件原大小写。
  - 示例：`Effect.waz -> bhe_Effect.waz`。
  - 示例：`Tama.spm -> bhe_Tama.spm`。
  - 示例：`RE_tama10.ogg -> bhe_RE_tama10.ogg`。
  - 示例：`Tama_001_0001.png -> bhe_Tama_001_0001.png`。
- [x] 公共 WAZ/SPM 的引用重写必须通过 append plan 中记录的 `sourceIndex -> targetIndex`、`targetName` 完成。
  - 示例：BHE 公共 WAZ 内的 `spmFileSequence = 10` 不能靠 `MekaEffect.spm` 的名字查找目标位置，而要映射到目标 `SpriteGroup` 中 `bhe_MekaEffect.spm` 对应的 index。
  - 示例：BHE 公共 WAZ 内的 `wazFileNo = 0` 不能继续指向 baseline 原始 `Effect.waz`，要映射到目标 `WazaGroup` 中 `bhe_Effect.waz` 对应的 index。
- [x] 公共资源 append 不允许只追加二进制文件；必须同步维护承载数量上界的 GRP/ProgramMaterial/MEK 尾部结构。
  - WAZ 顶层扩容必须同步 `WazaGroup.grp`。
  - SPM 顶层扩容必须同步 `SpriteGroup.grp`，并同步依赖 Sprite 数量的 ProgramMaterial / MEK material 尾部结构。
  - SE / BatVoice 顶层扩容必须先完成对应 GRP、ProgramMaterial、MEK material 尾部容量审计，不能直接按 BHE 全量组数盲目追加。
- [x] 公共资源层产出的 `preparedBaseline` 是单机体移植的输入基线。
  - 单机体阶段不能重复 append 或重定向这批公共资源。
  - 单机体阶段只允许读取公共 append plan，把私有 WAZ 中指向公共资源的引用落到 `preparedBaseline` 的目标 index。
- [x] `mergedPackageBundle` 不作为公共资源二次处理入口。
  - 公共资源接入完成后，公共资源属于 `preparedBaseline` 的既成事实。
  - `mergedPackageBundle` 只承载本次单机体 selected 资源的转换结果视图。
  - 单机体 closure/graft 不能把公共 WAZ/SPM/SE 当作普通 selected 依赖重新扫入。
- [x] `SOU / MISAKI` 的 BatVoice 依赖属于公共弹幕层的外部角色语音特例。
  - 公共资源层记录依赖，但不在公共阶段把两个角色的单机体语音策略定死。
  - 移植 `SOU / MISAKI` 时必须回看本节，避免把公共弹幕层已经占用或引用的 voice 槽位重复导入、覆盖或重定向。
- [x] 公共资源阶段不做全局 Voice / BatVoice 重建。
  - 不新增 BatVoice group。
  - 不复制 voice sidecar。
  - 不为公共资源构建 voice 映射。
  - 只记录 `tama02 / tama04 / tama05` 对 `SOU / MISAKI` 的外部依赖。
- [x] `term / InfoCollection` 属于公共 WAZ 自洽范围，不能放到单机体阶段处理。
  - 公共资源阶段暂定接入全量 BHE term 语义转换。
  - term 转换方法和实现策略待定，单独抽出 `TODO 20260417` 子问题处理。
  - 公共 WAZ redirect 实现不能把 term 当成普通备注字段跳过。
- [x] 公共 SE 使用 1 个 BHE 公共弹幕专属 `SeGroup` 聚合组。
  - 输入来自 8 个公共 WAZ 实际引用到的 BHE 源 `(SeGroup, SeItem)`。
  - 输出是在 `BSDX + JINKI` baseline 上追加 1 个 `bhe_common_projectile_se` 顶层 `SeGroup`。
  - 中间聚合视图只用于建立 source pair 到目标 item 的映射，不作为独立产物。

## 架构入口

- [x] 公共资源准备层的唯一外露入口是 `meka/bhecommon/projectile/BheCommonProjectilePrepareStep`。
- [x] `prepare()` 内部分两步：
  - `convert()`：已实现，负责公共 WAZ/SPM 的 BHE DTO -> BSDX DTO 格式转换。
  - `redirect()`：负责公共资源接入、自重定向、交叉重定向和审计。
- [x] `redirect()` 内部分两步：
  - `selfRedirect()`：已实现公共 WAZ/SPM 目标 entry、公共 SE 聚合组、容量同步和 source -> target 映射。
  - `crossRedirect()`：`TODO 20260417` 空实现，处理公共 WAZ 指向 WAZ/SPM/SE 的跨资源引用重写；Voice 只记录外部依赖，term 通过独立 TODO 子问题接入。
- [x] `TsukuyomiConvertOverviewStep` 只调用 `prepare()`，不直接调用公共资源内部的 convert / redirect / self / cross 细节。
- [x] selfRedirect 目标 entry 接入已实现；crossRedirect 和 term 全量语义转换保留 `TODO 20260417`。

## 单机体 graft 护栏

- [x] 公共 WAZ/SPM 完成 `bhecommon/projectile` 接入后，属于 `preparedBaseline` 的既成事实。
- [x] 单机体 graft 可以读取 `BheCommonProjectileAppendPlan` 中的公共资源映射，用于重写私有 WAZ 对公共资源的引用。
- [x] 单机体 graft 不能使用 `BheCommonProjectileAppendPlan` 重复 append、merge 或输出公共 WAZ/SPM。
- [x] `BuildResourceClosureStep` 扫到公共 WAZ 引用时，不能把公共 WAZ 加入单机体 `requiredWazFiles`。
- [x] `AppendGrpEntriesStep` 不能对公共 WAZ 执行 key-based merge。
- [x] `RebindWazStep` 遇到公共 WAZ 引用时，只能读取公共计划中的只读映射，把引用落到 `preparedBaseline` 的公共追加段。
- [x] 该护栏用于修正 JINKI 架子的默认假设：扫到的辅助 WAZ 不一定都是单机体迁移资源，BHE 公共 WAZ 是公共资源层的基线组成部分。

## WAZ 运行时引用语义

- [x] WAZ 文件内部虽然有 `skillNameJapanese / skillNameEnglish`，但运行时外部引用不靠 codename。
- [x] `CEventWazaSelect.wazFileNo` 引用的是 `WazaGroup.grp` 顶层 index。
  - 示例：`wazFileNo = 7` 表示读取 `WazaGroup[7]` 对应的 WAZ 文件，例如 `Bomb.waz`。
- [x] `CEventWazaSelect.wazSequenceNo` 引用的是目标 WAZ 文件内部的 `skillList` index。
  - 示例：`wazFileNo = 7, wazSequenceNo = 134` 表示 `Bomb.waz.skillList[134]`。
- [x] `skillNameEnglish` 可用于审计和人工对照，但 BHE common projectile cluster 不使用 key-based merge 推导运行时目标 index。
- [x] BHE common projectile cluster 不把 skillList 追加到 baseline 同名 WAZ。
- [x] 公共 WAZ 以 `bhe_*` 新文件独立接入目标侧，内部 skill index 保持源侧序号。

## 公共资源目标索引映射

- [x] 公共 WAZ 新增 8 个 `bhe_*` 顶层 `WazaGroup` entry。
  - BHE 源公共 WAZ index 连续为 `0..7`。
  - 目标映射：`targetWazIndex = baseWazaGroupSize + sourceWazIndex`。
  - 公共 WAZ 内部 `CEventWazaSelect.wazFileNo` 按该公式重写。
  - 新 `bhe_*` WAZ 文件独立存在，`wazSequenceNo` 保持源侧 skill index，不加旧同名 WAZ 的 appendStart。
- [x] 公共 SPM 新增 12 个 `bhe_*` 顶层 `SpriteGroup` entry。
  - BHE 源公共 SPM index 为 `[0,1,2,3,4,5,6,7,8,9,10,173]`，不是连续 `0..11`。
  - 目标映射使用紧凑顺序：`sourceSpriteIndex -> baseSpriteGroupSize + compactOrdinal`。
  - 示例：`0 -> baseSpriteGroupSize + 0`，`10 -> baseSpriteGroupSize + 10`，`173 -> baseSpriteGroupSize + 11`。
  - 公共 WAZ 内部 `CEventSprite.spmFileSequence` 按该映射重写。
  - 新 `bhe_*` SPM 文件独立存在，`actionGroupNumber` 保持源侧 anim index，不叠加旧同名 SPM 的 anim 偏移。
- [x] 公共 SE 新增 1 个 `bhe_common_projectile_se` 顶层 `SeGroup` entry。
  - 目标 group：`targetSeGroupIndex = baseSeGroupSize`。
  - 目标 item：`(sourceSeGroupIndex, sourceSeItemIndex) -> targetSeItemIndex`。
  - 公共 WAZ 内部 `CEventSe` 重写为 `(baseSeGroupSize, targetSeItemIndex)`。
- [x] 单机体 graft 遇到公共资源引用时读取公共 append plan。
  - 公共 WAZ：`source public waz index -> preparedBaseline bhe_* WazaGroup index`。
  - 公共 SPM：`source public sprite index -> preparedBaseline bhe_* SpriteGroup index`。
  - 公共 SE：`source se pair -> preparedBaseline common SeGroup/item`。
  - 单机体 graft 不重新 append 公共资源。

### 20260418 selfRedirect 实现审计

- [x] 已实现 8 个公共 WAZ 目标 entry 接入。
  - 审计输出：`src/main/resources/out/bhe2bsdx/common-projectile-self-redirect/audit.md`
  - 目标区间：`baseWazaGroupSize=108`，目标 index `108..115`。
- [x] 已实现 12 个公共 SPM 目标 entry 接入。
  - 目标区间：`baseSpriteGroupSize=138`，目标 index `138..149`。
  - `sourceSpriteIndex=173` 映射到 `targetSpriteIndex=149`。
- [x] 已实现 1 个 `bhe_common_projectile_se` 聚合 SeGroup。
  - 目标 group：`baseSeGroupSize=38`。
  - 聚合 item 数量：`668`。
  - SE 输入来自 BHE 源公共 WAZ 的实际 `CEventSe` 引用，不从转换后 WAZ 反推。
- [x] 已实现公共资源接入后的容量同步。
  - ProgramMaterial array1/array2 追平 SpriteGroup/SeGroup 顶层长度。
  - MapGroup array1/array2 追平 SpriteGroup/SeGroup 顶层长度。
  - baseline MEK material 的 sprite/se/voice 数组追平目标顶层长度。

## 公共 SPM 候选清单

- [x] 公共 SPM 候选清单完成审计。
- [x] 来源：递归扫描 8 个 BHE 公共 WAZ 内所有 `spmFileSequence >= 0`，并反查 BHE `SpriteGroup.grp` 的 `spriteList[index]`。
- [x] 这批清单写成常量时必须保留本段来源说明，避免魔法值失去依据。
- [x] 2026-04-18 复核：使用三种方法重复确认，结果完全一致。
  - JSON 全字段递归扫描：共 6169 处 `spmFileSequence` 引用，唯一 SpriteGroup index 为 `[0,1,2,3,4,5,6,7,8,9,10,173]`。
  - 文本正则扫描所有 `"spmFileSequence": <int>`：共 6169 处引用，唯一 index 同上。
  - 按 WAZ `skillList -> phase -> unit -> skillInfoObjectList -> *UnitList.data` 结构递归扫描：共 6169 处引用，唯一 index 同上。

| SpriteGroup index | codeName | fileName | 引用来源 |
|---:|---|---|---|
| 0 | `TAMA` | `Tama.spm` | `effect / tama01 / tama02 / tama03 / tama04 / tama05 / laser` |
| 1 | `BOMB` | `bomb.spm` | `effect / tama01 / tama04 / tama05 / bomb` |
| 2 | `ELEC` | `Elec.spm` | `effect / tama01 / tama02 / tama04 / tama05 / laser / bomb` |
| 3 | `SMOKE` | `Smoke.spm` | `effect / tama01 / tama05` |
| 4 | `MARK` | `Mark.spm` | `effect / tama01 / tama02 / tama03 / tama04 / tama05 / laser / bomb` |
| 5 | `PIC` | `Pic.spm` | `effect / tama01 / tama02 / tama03 / tama04 / tama05 / laser / bomb` |
| 6 | `ICE` | `Ice.spm` | `effect / tama05` |
| 7 | `LINK` | `Link.spm` | `effect / tama04 / tama05 / laser` |
| 8 | `FIRE` | `Fire.spm` | `effect / tama04 / tama05 / laser / bomb` |
| 9 | `WIND` | `Wind.spm` | `effect / tama04 / tama05` |
| 10 | `MEKAEFFECT` | `MekaEffect.spm` | `tama05` |
| 173 | `MAPOBJ_KEIZIBAN` | `設置物：掲示板.spm` | `effect` |

## 公共资源跨类型自洽顺序

- [x] 公共资源自洽不是单个 WAZ/SPM/GRP 文件自洽，而是公共弹幕资源簇跨类型自洽。
- [x] 单独转换 WAZ 或单独转换 SPM 都不能直接视为公共资源层完成；必须确认 WAZ/SPM/SE/GRP/term 之间的引用全部落入同一目标索引空间。

建议顺序：

1. **固定公共输入清单**
   - 8 个公共 WAZ：`effect / tama01..05 / laser / bomb`。
   - 12 个公共 SPM：`[0,1,2,3,4,5,6,7,8,9,10,173]`。
   - 668 个公共 SE source pair：来自公共 WAZ 实际 `CEventSe` 引用。
   - `term / InfoCollection`：公共资源阶段接入全量语义转换，方法和策略待定。

2. **追加公共目标 entry**
   - WAZ：新增 8 个 `bhe_*` 顶层 `WazaGroup` entry。
   - SPM：新增 12 个 `bhe_*` 顶层 `SpriteGroup` entry。
   - SE：新增 1 个 `bhe_common_projectile_se` 顶层 `SeGroup` entry。
   - Voice：`SOU / MISAKI` 作为外部角色语音特例记录，不在公共阶段重建。

3. **写入公共资源文件**
   - WAZ：输出独立 `bhe_*` WAZ 文件，skill index 保持源侧序号。
   - SPM：输出独立 `bhe_*` SPM 文件，anim/page/image 内部索引保持源侧语义。
   - SE：公共 SE item 的落盘音频名使用 `bhe_*` 前缀。
   - 静态 PNG/OGG 的复制仍属于 output/import 阶段，但本层必须产出对应 `bhe_*` 引用名。

4. **重写公共 WAZ 内部跨资源引用**
   - `CEventWazaSelect.wazFileNo`：`baseWazaGroupSize + sourceWazIndex`。
   - `CEventWazaSelect.wazSequenceNo`：保持源侧 skill index。
   - `CEventSprite.spmFileSequence`：`sourceSpriteIndex -> baseSpriteGroupSize + compactOrdinal`。
   - `CEventSprite.actionGroupNumber`：保持源侧 anim index。
   - `CEventSe`：`(sourceSeGroupIndex, sourceSeItemIndex) -> (baseSeGroupSize, targetSeItemIndex)`。
   - `InfoCollection / term`：单独 TODO，不与普通 index 重写混写。

5. **同步容量与审计**
   - 同步 WazaGroup / SpriteGroup / SeGroup 顶层 entry 数量。
   - 同步 ProgramMaterial / MapGroup / baseline MEK material 尾部结构。
   - 审计所有 source -> target 映射、目标文件名、GRP entry、WAZ 事件引用。

## 结论记录

- [x] Q1a：公共资源接入由同级 `bhecommon/projectile` 小工程统一承接，而不是继续塞在 Tsukuyomi 私有转换包里。
  - 结论：公共 WAZ/SPM 属于同一个公共弹幕资源簇；可以在 `bhecommon` 内部分成 WAZ 子阶段和 SPM 子阶段，但不能把公共资源混入单机体 selected 转换。

- [x] Q1b：`bhecommon/projectile` 进入 preparedBaseline 前必须完整闭环，不能只接入 WAZ 并另行补齐。
  - 结论：公共 WAZ、公共 SPM、全局 SE、全局 BatVoice、全局 term 的引用策略必须在公共资源层内明确；WAZ 子阶段的 `CEventWazaSelect` 偏移重写必须和 SPM/SE/Voice/term 策略保持一致。

## 已确认 QA 记录

- [x] Q2：`BheCommonProjectilePrepareStep.redirect()` 是否允许原地修改 `inheritedBaseline/preparedBaseline`，还是必须深拷贝后返回新 baseline？
  - 结论：沿用 JINKI 架构，允许在传入的 baseline 对象上原地 append 并同步修正元数据；该 baseline 随后就是 preparedBaseline。
  - 原因 1：graft 步骤在同一个 baseline 对象上累积修改；原地更新后，下游 step 可消费同一个 baseline 引用，不需要新增 inherited/prepared 双 baseline 分支。
  - 原因 2：baseline 内含 WAZ/SPM/MEK/DAT/GRP 等复杂嵌套对象，且存在多态对象与 byte[]；为了 before/after 语义强行深拷贝会引入额外 transfer 成本和复制错误风险。

- [x] Q3：公共 WAZ 是追加到 baseline 同名 WAZ，还是新增 `bhe_*` 顶层 WAZ entry？
  - 结论：新增 8 个 `bhe_*` 顶层 `WazaGroup` entry，并输出 8 个独立 `bhe_*` WAZ 文件。
  - 原因：同名 append 会把 BSDX/JINKI 原公共 WAZ 与 BHE 公共 WAZ 混在同一文件里，破坏公共层隔离，也容易被单机体 closure 重复处理。
  - 元数据：新 `WazaGroup.param` 等于对应 `bhe_*` WAZ 文件自身 `skillList.size()`，不是旧同名 WAZ 的 `appendStart + appendCount`。

- [x] Q4：公共 WAZ 内部 `CEventWazaSelect.wazSequenceNo` 重写是否只处理公共弹幕资源簇内部引用，不处理单机体主 WAZ 的普通引用？
  - 结论：bhecommon WAZ 子阶段只修公共 WAZ 之间的内部引用，不修任何单机体私有引用。
  - 范围：`effect / tama01..05 / laser / bomb` 内部相互引用时，`wazFileNo` 重写到 `bhe_*` WazaGroup entry，`wazSequenceNo` 保持源侧 skill index；`tsukuyomi.waz` 等单机体私有 WAZ 的引用由单机体 rebind 处理。

- [x] Q5：`BheCommonProjectileAppendPlan` 是否需要记录公共资源 source -> target 映射？
  - 结论：必须记录 WAZ、SPM、SE 的 source -> target 映射。
  - WAZ：`sourceWazIndex -> baseWazaGroupSize + sourceWazIndex`。
  - SPM：`sourceSpriteIndex -> baseSpriteGroupSize + compactOrdinal`。
  - SE：`(sourceSeGroupIndex, sourceSeItemIndex) -> (baseSeGroupSize, targetSeItemIndex)`。
  - 原因：单机体 graft 只能读取这些映射把私有 WAZ 公共引用落到 preparedBaseline，不能重新 append 公共资源。

- [x] Q6：公共 WAZ 是否依赖 baseline 存在同名 WAZ 才能接入？
  - 结论：不依赖同名 WAZ 内容 append。
  - 原因：公共 WAZ 作为 `bhe_*` 新顶层 entry 接入，目标 index 来自 baseline `WazaGroup` 原始 size；baseline 同名 WAZ 可用于审计，但不是 append 起点。

- [x] Q7：`commonProjectileResourceBundle` 缺少固定公共 WAZ/SPM 时是否直接 fail？
  - 结论：直接 fail。
  - 原因：公共弹幕资源簇的输入来自实际数据固定清单，必须包含 8 个公共 WAZ 和 12 个公共 SPM；缺少任意文件代表 raw 读取、路径规范化、清单维护或转换步骤存在错误，继续执行会让 append/rewrite 建立在不完整资源视图上。

- [x] Q8：公共 WAZ 接入测试是否只检查结构和 offset，不做字节级一致？
  - 结论：不做字节级一致。
  - 原因：公共 WAZ 接入产物是 BHE 转换后的 `bhe_*` 新文件，不应与 BHE 原始 WAZ 或 BSDX 原始 WAZ 做 byte parity。
  - 验收边界：检查 `bhe_*` WazaGroup entry、`WazaGroup.param`、关键跨资源引用落点、以及生成后二进制可重新 parse。

- [x] Q9：`BuildResourceClosureStep / AppendGrpEntriesStep / RebindWazStep` 从哪个阶段消费 common append plan，避免公共 WAZ 进入 key-based merge 路径？
  - 结论：common append plan 只由 `bhecommon/projectile` 内部消费。
  - 单机体 graft 步骤只消费 `preparedBaseline` 的结果，不处理公共 WAZ 接入，也不对公共 WAZ 执行 key-based merge。
  - 公共资源层必须保证公共 WAZ/SPM 的内部自洽以及公共 WAZ 之间的相互引用自洽。
  - `CEventVoice` 中指向 `SOU / MISAKI` 的外部 BatVoice 依赖作为特例记录，不在公共资源阶段重建；移植 `SOU` 或 `MISAKI` 单机体时再统一处理。

- [x] Q10：公共 WAZ 里的 `term / InfoCollection` 是否属于公共资源阶段必须处理的内容？
  - 结论：属于公共资源阶段，必须处理。
  - 原因：公共 WAZ 已经被隔离到 `bhe_*` 公共资源层，它内部携带的 term 语义也属于这层自洽的一部分；把 term 留给单机体阶段会破坏公共层独立性。
  - 暂定口径：公共资源阶段接入全量 BHE term 语义转换。
  - 执行边界：term 转换方法和实现策略待定，单独开 `TODO 20260417` 子问题，基于实际 InfoCollection/term 数据审计后再落地。

- [x] Q11：公共资源阶段是否重建 Voice / BatVoice？
  - 结论：不重建。
  - 原因：公共 WAZ 中的 `CEventVoice` 只涉及 `SOU / MISAKI` 角色语音外部依赖，不是公共弹幕资源本体。
  - 执行边界：公共阶段只记录 `tama02 / tama04 / tama05` 三处外部依赖；移植 `SOU / MISAKI` 时再处理对应 BatVoice 和 sidecar。

- [x] Q12：`mergedPackageBundle` 和 `preparedBaseline` 的业务边界是什么？
  - 结论：公共资源接入结果属于 `preparedBaseline`，不属于单机体 selected 资源闭包。
  - `mergedPackageBundle` 只作为 Tsukuyomi 私有资源转换结果视图。
  - 单机体 graft 以 `preparedBaseline` 为目标基线，并通过只读公共 append plan 处理私有 WAZ 中的公共引用。
  - 实现护栏：后续接线时必须避免公共 WAZ/SPM/SE 从 `mergedPackageBundle` 进入 `BuildResourceClosureStep / AppendGrpEntriesStep / RebindWazStep` 的普通 selected 路径。

## bhecommon/waz 子阶段实现边界

- 新增 8 个 `bhe_*` 顶层 `WazaGroup` entry。
- 输出 8 个独立 `bhe_*` WAZ 文件，不写入 baseline 同名 WAZ。
- `WazaGroup.param` 同步为新 `bhe_*` WAZ 文件自身 `skillList.size()`。
- 重写公共 WAZ 内部 `CEventWazaSelect`：
  - `wazFileNo = baseWazaGroupSize + sourceWazIndex`
  - `wazSequenceNo = sourceSequenceNo`
- 记录 audit / notes，必须包含具体数值：
  - `baseWazaGroupSize`
  - `sourceWazIndex -> targetWazIndex`
  - `sourceSkillCount`
  - `targetFileName`
- 不处理 PNG/OGG 静态资源复制。
- 不在 WAZ 子阶段单独处理 SPM/SE/term；对应映射由公共资源层其他子问题提供，WAZ rewrite 只消费映射结果。

## bhecommon/spm 子阶段实现边界

- 不塞进 WAZ 子阶段实现；作为同一个 `bhecommon/projectile` 小工程的 SPM 子阶段处理。
- 需要基于公共 WAZ 引用审计结果加载并转换 12 个公共 SPM。
- 公共 SPM 新增 12 个 `bhe_*` 顶层 `SpriteGroup` entry，不做 key-based merge、diff merge 或同名复用。
- 输出 12 个独立 `bhe_*` SPM 文件，不写入 baseline 同名 SPM。
- `SpriteGroup` 目标映射使用紧凑顺序：
  - `sourceSpriteIndex -> baseSpriteGroupSize + compactOrdinal`
  - `173 -> baseSpriteGroupSize + 11`
- 新 `bhe_*` SPM 文件独立存在，SPM 内部 image/page/anim index 保持源侧语义。
- 公共 WAZ 内部 `CEventSprite.actionGroupNumber` 保持源侧 anim index。

### 20260418 SPM 内部索引审计

- [x] 审计对象包含 `src/main/java/com/giga/nexas/dto/bsdx/spm`、`src/main/java/com/giga/nexas/dto/bhe/spm` 的 DTO / parser / generator，以及全量 `spmBheJson`、`spmBsdxJson`。
- [x] BSDX/BHE SPM 的内部数组顺序均为 `pageData -> imageData -> animData`。
- [x] SPM 内部确定存在的数组索引链只有两类：
  - `SPMPageData.chipData[].imageNo` 指向 `imageData`。
  - `SPMAnimData.patData[].pageNo[]` 指向 `pageData`。
- [x] `numImageData / numPageData / numAnimData` 是数组长度元数据，目标 `bhe_*` SPM 必须同步为自身数组长度。
- [x] `numChipData / numPat / patPageNum / hitFlag` 是局部长度或局部结构字段，不按 appendStart 偏移。
- [x] `SPMHitArea`、`SPMRect`、`SPMImageData.imageName`、`waitFrame`、`animRotateDirection`、`animReverseDirection` 不承载跨数组索引。
- [x] 全量 BHE SPM 统计：
  - `chip.imageNo` 引用 131462 处，越界 0，负值 0。
  - `pat.pageNo` 引用 36797 处，存在 310 处越界，其中 `-2` 哨兵 278 处；这些越界来自非公共 SPM 样本。
- [x] 全量 BSDX SPM 统计：
  - `chip.imageNo` 引用 132425 处。
  - `pat.pageNo` 引用 58129 处，存在越界样本；公共 SPM 实现不能把“全量数据中所有整数”当作可偏移索引。
- [x] 12 个公共 BHE SPM 子集统计：
  - `Tama.spm`: image=281, page=1353, anim=292
  - `bomb.spm`: image=117, page=689, anim=103
  - `Elec.spm`: image=58, page=240, anim=65
  - `Smoke.spm`: image=55, page=337, anim=55
  - `Mark.spm`: image=256, page=968, anim=255
  - `Pic.spm`: image=192, page=1387, anim=257
  - `Ice.spm`: image=11, page=44, anim=10
  - `Link.spm`: image=42, page=255, anim=66
  - `Fire.spm`: image=28, page=127, anim=28
  - `Wind.spm`: image=20, page=85, anim=19
  - `MekaEffect.spm`: image=18, page=571, anim=28
  - `設置物：掲示板.spm`: image=20, page=56, anim=56
- [x] 12 个公共 BHE SPM 的 `chip.imageNo` 与 `pat.pageNo` 均无越界、无负值。
- [x] 公共 SPM 独立 `bhe_*` 文件的实现结论：
  - 不把公共 SPM 的 `imageData / pageData / animData` 写入 baseline 同名 SPM。
  - `chip.imageNo` 保持源侧 image index。
  - `pat.pageNo[]` 保持源侧 page index。
  - `CEventSprite.actionGroupNumber` 保持源侧 anim index。
  - `SPMImageData.imageName` 对应的落盘 PNG 名称需要进入 `bhe_*` 命名空间。

## 20260418 公共资源独立性审计

- [x] 审计范围只包含 8 个 BHE 公共 WAZ：`effect.waz / tama01.waz / tama02.waz / tama03.waz / tama04.waz / tama05.waz / laser.waz / bomb.waz`。
- [x] `CEventWazaSelect` 共 4583 处，唯一引用 509 个 `(wazFileNo, wazSequenceNo)` 组合。
- [x] `CEventWazaSelect.wazFileNo` 只引用 `WazaGroup[0..7]`，全部落在 8 个公共 WAZ 内，不引用 Tsukuyomi 或其他单机体 WAZ。
  - 引用次数：`0=3543, 1=32, 2=97, 3=82, 4=28, 5=148, 6=339, 7=314`。
- [x] `CEventSprite` 共 6174 处，引用 `spmFileSequence = -2, 0..10, 173`。
- [x] `spmFileSequence = 0..10, 173` 全部落在公共 SPM 清单内；`-2` 是特殊值，不表示单机体 SPM。
  - `0..10` 对应 `Tama.spm / bomb.spm / Elec.spm / Smoke.spm / Mark.spm / Pic.spm / Ice.spm / Link.spm / Fire.spm / Wind.spm / MekaEffect.spm`。
  - `173` 对应 `設置物：掲示板.spm`，共 15 处引用。
- [x] `CEventSe` 共 3001 处，唯一 `(SeGroup, SeItem)` 组合 668 个。
- [x] `CEventSe` 所有引用都能在 BHE 全局 `SeGroup.grp` 中解析到存在的 group/item；缺失 group 为 0，缺失 item 为 0，空 item 为 0。
- [x] `CEventSe` 引用的是全局音效分类，不依赖 Tsukuyomi 单机体。
  - 主要引用组包括 `W_BULLET / W_BEAM / W_MISSILE / W_SLASH / W_BLOW / METAL / METAL_IMPACT / T_BOMB / T_SPARK / L_LASER / SYSTEM` 等。
- [x] `CEventVoice` 共 13 处，唯一 `(BatVoiceGroup, VoiceIndex)` 组合 6 个。
- [x] `CEventVoice` 所有引用都能在 BHE 全局 `BatVoice.grp` 中解析到存在的 group/voice；缺失 group 为 0，缺失 voice 为 0，空 voice 为 0。
- [x] `CEventVoice` 不依赖 Tsukuyomi，但依赖 BHE 全局 BatVoice 中的具体角色语音。
  - `tama02.waz` 引用 `SOU / 蒼`：`Sou_a01055 / Sou_a01063`。
  - `tama04.waz` 引用 `SOU / 蒼`：`Sou_a01064 / Sou_a01117`。
  - `tama05.waz` 引用 `MISAKI / みさき`：`Misaki_a0404 / Misaki_a0405`。
- [x] `InfoCollection / term` 大量存在，但来源是 BHE 全局 term 语义，不是 Tsukuyomi 单机体私有资源。
  - `bheInfoCollectionList1 = 5976`
  - `bheInfoCollectionList2 = 4921`
  - `bheInfoCollectionList = 22748`
- [x] 审计结论：BHE 公共弹幕资源簇可以脱离单机体移植流程独立自洽。
- [x] 审计边界：它不是只依赖 WAZ/SPM；公共资源层还必须能处理或审计全局 `SeGroup`、全局 `BatVoice` 中的 `SOU/MISAKI` 语音，以及全局 `term` 语义。

### 外部依赖确认

- [x] 对 8 个公共 WAZ 的反序列化 JSON，递归扫描已知资源引用字段后，可以确认显式全局外部依赖只有三类。
  - `SeGroup / SeItem`
  - `BatVoice / Voice`
  - `term / InfoCollection`
- [x] `BatVoice / Voice` 只涉及两个角色组，不存在第三个角色引用。
  - `SOU / 蒼`
  - `MISAKI / みさき`
- [x] 移植 `SOU` 或 `MISAKI` 单机体时，必须重点回看这里的公共弹幕 Voice 引用。
  - 公共资源层会接触 `SOU/MISAKI` 的全局 BatVoice 条目。
  - 单机体移植阶段不能无脑重复导入、覆盖或改写这些公共弹幕依赖的 voice 槽位。
  - 处理 `SOU/MISAKI` 时需要确认公共弹幕层与单机体层对同一 BatVoice 组的处理策略一致。
- [x] 未发现以下单机体依赖。
  - 未发现 `Tsukuyomi.waz` 引用。
  - 未发现其他单机体 WAZ 引用。
  - 未发现 Tsukuyomi 私有 SPM 引用。
  - 未发现公共 SPM 清单以外的有效 SPM 引用。
  - 未发现 `MekaGroup / Mek` 文件引用。
  - 未发现除 `SOU / MISAKI` 以外的 `BatVoice` 角色引用。
- [x] 本结论的 100% 边界：对公共资源接入实现所消费的显式 DTO 数据，确认只有上述三类全局外部依赖。
- [x] 本结论不宣称“运行时绝对不存在隐式依赖”；如需把审计范围扩展到运行时隐式逻辑，需要查 WAZ 事件类反编译和字段语义。
