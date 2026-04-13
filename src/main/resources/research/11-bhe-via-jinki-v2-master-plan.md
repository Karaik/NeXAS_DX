# 11. BHE via JINKI V2 总设计与实施计划

> 编号说明：原需求指定 `01-bhe-via-jinki-v2-master-plan.md`，但 `src/main/resources/research/` 下已经存在 `01-*` 到 `10-*` 文档，因此本文件按顺序顺延为 `11-bhe-via-jinki-v2-master-plan.md`。

## 一、路线先写死

新的 BHE 路线不是：

- 继续扩写旧 `bhe2bsdx`。
- 直接从原始 BSDX 开始 graft BHE。
- 写一个大一统框架，把所有源游戏和所有机体都塞进一套厚配置分支里。

新的路线必须是：

```text
BSDX
-> BSDX + JINKI
-> BSDX + JINKI + BHE
```

也就是说：

```text
BHE 的 baseline 不是原始 BSDX。
BHE 的 baseline 是 JINKI 已经移植完成后的成果物。
```

BHE 新流程的总形态是：

```text
BHE 二进制资源
  -> 很重的 BHE convert 语义收束层
  -> BSDX 资产对象格式
  -> 前置客制化输入
  -> 通用 graft 主线
  -> 后置客制化 / 兼容 patch / 输出沉淀
  -> 最终打包验收
```

这里必须修正一个容易说轻的表述：

```text
BHE 不是“只比 JINKI 多一个 convert”。
BHE 是多了一个很重的 convert 语义收束层。
```

这个 convert 层会决定后面的 graft / post / output 是否真的能复用 JINKI V2 的结构。如果 convert 后的对象仍然带着 BHE 原始 index 空间、term 语义断裂、SE 引用未收束、hitbox 不可生成、弹幕链不完整，那么后面的 graft 主线会被迫塞满 BHE 特判，整个设计就失败了。

## 二、当前阶段优先级：先做 tsukuyomi，后谈抽共通

这一节也是硬规则。

当前阶段目标不是先抽一个完整的 BHE common framework。

当前阶段目标是：

```text
先把 tsukuyomi 这第一台 BHE 机体完整做通。
```

因此：

- 不要一开始就为了后续机体设计很多 common 包、common pipeline、common orchestrator。
- 不要把“未来可能共通”的东西提前抽成厚框架。
- `tsukuyomi` 会顺带承担 BHE 共通资源首次接入职责。
- 但“共通层独立化 / 抽象化”是后续事情，不是当前第一目标。
- 在做通 `tsukuyomi` 的过程中，哪些逻辑事实证明稳定、重复、可复用，再在后续阶段考虑下沉。

当前允许的做法：

```text
tsukuyomi 实现层可以写得具体、直接、甚至有一定重复。
底层 helper 可以保持清晰。
但不要为了还没开始的后续机体，把第一台机体拖进大抽象。
```

## 三、JINKI 作为 BHE baseline 的继承原则

这一节是硬规则。

### 1. BHE 不能跳过 JINKI 层

BHE 必须站在 `BSDX + JINKI` 的成果物上继续做，原因是：

- JINKI 层已经修改了 BSDX 的资源集合。
- JINKI 层已经追加了 MEK/WAZ/SPM/SE/GRP 等资源。
- JINKI 层已经做过菜单、sidecar、EXE patch 和 PAC 输出。
- BHE 后续接入时必须继承这些修改，否则会回退掉 JINKI 成果，或者重新制造同名资源冲突。

因此：

```text
baseline0 = original BSDX
jinkiResult = runJinkiV2(baseline0, jinkiRequest)
bheConvertResult = runBheConvert(bheSource, bheConvertSpec)
bheResult = runBheV2(jinkiResult.outputAsBaseline(), bheConvertResult, bheGraftSpec)
```

上面是原则表达，不要求最终代码长这样。重点是：

```text
test 负责显式串接层与层之间的成果物。
pipeline 只接受调用方显式传入的 baseline / source / spec。
pipeline 内部不能偷偷决定上一层 baseline 是谁。
```

### 2. 必须继承上一层的文件类型

以下文件类型都必须继承上一层成果物：

- `dat`
- `grp`
- `mek`
- `waz`
- `spm`
- `png`
- `sidecar`
- patched exe

如果某个同名文件在 JINKI 层已经存在，而 BHE：

- 直接引用它。
- 间接引用它。
- 或者要重建它。

那么 BHE 必须以 JINKI 层那个文件为基底继续改。

不能：

- 回头拿原始 BSDX 的同名文件重做。
- 用 BHE 资源直接覆盖 JINKI 成果物。
- 在 BHE pipeline 内部隐式读 `game/bsdx` 原始目录作为真实 baseline。

### 3. 同名文件继续改的规则

如果 BHE 需要改一个 JINKI 层已经存在的文件：

```text
输入 = BSDX+JINKI 层同名文件
操作 = BHE 当前层 append / patch / rewrite
输出 = BSDX+JINKI+BHE 层同名文件
```

举例：

- `bomb.waz`
  - 如果 JINKI 层已有 `bomb.waz`，BHE 引用或扩展 `bomb.waz` 时，必须基于 JINKI 层的 `bomb.waz` 继续 append。

- `ProgramMaterial.grp`
  - 如果 JINKI 层已经同步过数组长度，BHE 只能在这个基础上继续扩容。

- `Meka.dat`
  - 如果 JINKI 层已经追加 AKAO 行，BHE patch 时必须保留 AKAO 行。

- patched exe
  - 如果 JINKI 层已经写过 target bytes，BHE EXE patch 必须把这些 site 识别为 already patched，而不是失败或回滚。

### 4. `tsukuyomi` 的特殊位置

第一个 BHE 目标机体明确为：

```text
tsukuyomi
```

`tsukuyomi` 不是普通机体样本。

它会比后续 BHE 机体明显更复杂，但原因不是角色本身特殊，而是：

```text
tsukuyomi 要第一次承担 BHE 共通资源接入到 BSDX+JINKI baseline 的工作。
```

后续其他 BHE 机体应该站在：

```text
BSDX + JINKI + tsukuyomi 共通层
```

继续做自己的机体客制化。

后续机体不应重复做：

- BHE 共通子弹资源首次接入。
- BHE 共通 WAZ/SPM append 空间建立。
- BHE 共通 SE/term/hitbox/projection 映射底座。

### 5. test 如何表达继承链

继承链必须由 test 显式表达。

推荐测试组织方式：

```text
baseline0 = loadOriginalBsdx()
jinkiResult = runJinkiV2(baseline0, jinkiRequest)

tsukuyomiConvert = runBheConvert(tsukuyomiSource, tsukuyomiConvertSpec)
tsukuyomiResult = runBheV2(
    jinkiResult.outputAsBaseline(),
    tsukuyomiConvert,
    tsukuyomiGraftSpec
)

nextBheConvert = runBheConvert(nextBheSource, nextBheConvertSpec)
nextBheResult = runBheV2(
    tsukuyomiResult.outputAsBaseline(),
    nextBheConvert,
    nextBheGraftSpec
)
```

禁止：

- pipeline 自己保存上一层状态。
- pipeline 自己查找“最近一次输出目录”。
- pipeline 内部默认从原始 BSDX 重读 baseline。
- 用全局调度器隐藏层与层的传递。

## 四、每机体独立包原则

这也是硬要求。

```text
每个机体都要有自己的独立包。
```

原因：

- 每个机体后期都会有大量客制化。
- 客制化会越来越多。
- 为了分别维护方便，允许代码重复。
- 不要为了抽象统一，把每个机体都塞进一个大框架里到处加 `if`。
- 共通规律可以沉到底层。
- 但机体实现层要允许独立。

设计原则：

```text
可以有共通能力。
不要有“所有机体共用一个厚重实现，再靠配置分支控制”的大框架。
机体实现层宁可重复，也要可维护、可分别改。
```

建议未来包结构可以保留“独立机体包”的方向，但当前第一阶段不要主动落一个庞大的 common framework。

当前更合适的落点是先围绕 `tsukuyomi` 建清楚边界：

```text
src/main/java/com/giga/nexas/transfer/bhe2bsdx/v2/meka/tsukuyomi
```

如果在 `tsukuyomi` 做通后，某些能力确实需要给第二台、第三台 BHE 机体复用，再考虑把稳定部分下沉。

后续可能演化的包结构：

```text
src/main/java/com/giga/nexas/transfer/bhe2bsdx/v2
├── common
│   ├── convert
│   ├── graft
│   ├── output
│   └── pack
└── meka
    ├── tsukuyomi
    │   ├── TsukuyomiBhePipeline.java
    │   ├── TsukuyomiConvertSpec.java
    │   ├── TsukuyomiGraftSpec.java
    │   ├── TsukuyomiCommonResourceSpec.java
    │   └── ...
    └── next_meka
        ├── NextMekaBhePipeline.java
        ├── NextMekaConvertSpec.java
        └── ...
```

其中：

- `common` 只放已经被验证为稳定共通的底层能力。
- `meka/tsukuyomi` 放第一个 BHE 目标和 BHE 共通资源首次接入。
- 后续每台机体都有自己的独立包。

不要把所有机体都压成：

```text
BheUniversalPipeline + giant config + if/else
```

## 五、tsukuyomi 的职责：BHE 共通资源首次接入

`tsukuyomi` 第一阶段不只是机体移植。

它承担两类职责：

### 1. 机体自身移植

- BHE `tsukuyomi` MEK 转 BSDX MEK。
- BHE `tsukuyomi` WAZ 转 BSDX WAZ。
- BHE `tsukuyomi` SPM 转 BSDX SPM。
- 机体专属 PNG / SE / sidecar / menu / survival。

### 2. BHE 共通资源首次接入

第一次把 BHE 共通资源接入到：

```text
BSDX + JINKI
```

这批共通资源包括但不限于：

- 共通子弹 WAZ。
- 共通弹幕 SPM。
- 共通命中特效。
- 共通 SE。
- 共通 term 语义映射。
- 共通 hitbox 转换策略。
- 共通 projectile 引用规则。

其中重点是：

```text
子弹资源。
```

后续其他 BHE 机体应继承：

```text
BSDX + JINKI + tsukuyomi 共通资源层
```

不要重复做这批共通资源首次接入。

这里的“共通资源层”当前不要求单独抽成一个独立 pipeline 或独立包。

现阶段它先作为 `tsukuyomi` 实施过程中形成的结果存在：

```text
BSDX + JINKI + tsukuyomi(含首次共通资源接入)
```

后续其他 BHE 机体再站在这层基础上继续做。

是否把其中一部分真正抽成 common，是后续阶段在第二台、第三台机体实践后再决定的事。

## 六、共通子弹资源策略：整段尾插 + 全引用统一偏移

这节是当前优先策略，不是模糊建议。

对子弹相关的共通资源，当前优先采用：

```text
整段尾插 + 全引用统一偏移重写
```

而不是 key-based 精细 merge。

这条策略当前主要限定在：

```text
共通子弹 / 弹幕资源簇
```

不要自动扩大成所有资源的一般规则。

不默认套用这条策略的对象包括：

- 机体专属资源。
- 菜单/UI 资源。
- 普通 sidecar。
- 明确有稳定 key、稳定替换规则的资源。

当前优先目标是先把最复杂、最容易炸的共通子弹链做稳。其他资源是否 append、merge、replace，要按各自协议和输出验收单独决定。

### 1. WAZ 的整文件级 append

例子：

当前 baseline 是 `BSDX + JINKI`，其中：

```text
bomb.waz 有 150 个对象
```

BHE 这边：

```text
bomb.waz 有 160 个对象
```

当前优先策略：

1. 直接把 BHE 的 160 个对象整段尾插到 `BSDX + JINKI` 的 `bomb.waz` 后面。
2. 新追加对象起始 offset：

```text
appendStart = bsdxJinkiBombWaz.size()
```

3. BHE 中所有引用 `bomb.waz` 内部 object index 的地方统一重写：

```text
newIndex = oldIndex + appendStart
```

也就是说：

```text
这是整文件级 append，不是 key-based merge。
```

选择这个策略的原因：

- 子弹链里 object index 往往是顺序空间，不一定有稳定 key。
- 精细 merge 很容易误判“看起来同名/同义”的对象，导致引用错位。
- 整段 append 虽然资源更多，但引用关系更可控。
- 第一台 `tsukuyomi` 需要先建立 BHE 共通资源层，稳定性优先于去重。

### 2. WAZ append 后必须重写的引用

凡是 BHE 资源中引用 appended WAZ 内部对象 index 的地方，都必须统一偏移。

包括：

- BHE 自身 WAZ 内部 skill object 引用。
- `CEventEffect` 中 effect number / projectile object 引用。
- `CEventHit` 中 hit effect 引用。
- 其他 WAZ 对共通 `bomb.waz`、`tama*.waz`、`effect.waz` 的引用。

转换层必须记录：

- 原文件名。
- 原 object index。
- 目标文件名。
- appendStart。
- new object index。
- 哪个 skill / event 使用了这个引用。

## 七、SPM 也必须做内部顺序引用平移

SPM 的策略不能只写成“复制文件”。

如果 SPM 相关资源也按 append 方式并入目标层，那么它本质上是：

```text
整段 append + 内部顺序引用统一平移重写
```

### 1. SPM 内部顺序空间

SPM 里至少有这些顺序空间：

- `imageData`
- `pageData`
- `chipData`
- `animData`
- `patData.pageNo`
- `chipData.imageNo`

其中必须特别注意：

```text
imageNo 指向 imageData index。
patData.pageNo 指向 pageData index。
page/chip/imageData 的顺序引用必须保持一致。
```

### 2. SPM append 规则

如果目标层已有某个 SPM：

```text
targetSpm.imageData.size() = oldTargetImageCount
targetSpm.pageData.size() = oldTargetPageCount
targetSpm.animData.size() = oldTargetAnimCount
```

BHE SPM 要整段 append 进去，则：

```text
newImageNo = oldBheImageNo + oldTargetImageCount
newPageNo  = oldBhePageNo  + oldTargetPageCount
newAnimNo  = oldBheAnimNo  + oldTargetAnimCount
```

对每个追加的 chip：

```text
chip.imageNo = chip.imageNo + oldTargetImageCount
```

对每个追加的 pat：

```text
pat.pageNo = pat.pageNo + oldTargetPageCount
```

这不是简单复制文件。

### 3. SPM append 的 audit

必须记录：

- 目标 SPM 文件名。
- append 前 `imageData/pageData/animData` 数量。
- BHE append 数量。
- append 后数量。
- 每个 `imageNo` 偏移规则。
- 每个 `pageNo` 偏移规则。
- 外部 WAZ / MEK 是否引用了 append 后的新 anim/page/object。

如果不做这层审计，后续弹幕错位会很难定位。

## 八、层与层之间用 test 显式串接

这也是硬规则。

不要设计复杂全局调度器。

不要隐藏状态。

不要让 pipeline 内部偷偷决定上一层 baseline。

### 1. 推荐原则

```text
test 负责串接层与层的传递。
pipeline 只接受调用方显式传入的输入。
```

好处：

- 每一层的输入输出都能在测试里看见。
- 失败时能明确知道卡在 convert、graft、post 还是 pack。
- 不需要调试隐藏的全局状态。
- 可以单独重跑某一层。
- 可以保存每层中间成果物做 diff。

### 2. 显式串接伪代码

```java
var baseline0 = loadOriginalBsdx();

var jinkiRequest = buildJinkiRequest(baseline0);
var jinkiResult = runJinkiV2(baseline0, jinkiRequest);

var tsukuyomiSource = loadBheSource("tsukuyomi");
var tsukuyomiConvertSpec = TsukuyomiConvertSpec.defaultSpec();
var tsukuyomiConvertResult = runBheConvert(tsukuyomiSource, tsukuyomiConvertSpec);

var tsukuyomiGraftSpec = TsukuyomiGraftSpec.defaultSpec();
var tsukuyomiResult = runTsukuyomiBheV2(
        jinkiResult.outputAsBaseline(),
        tsukuyomiConvertResult,
        tsukuyomiGraftSpec
);
```

重点不是 API 形式，而是边界：

- `runJinkiV2` 不保存全局状态。
- `runBheConvert` 只输出 convert result。
- `runTsukuyomiBheV2` 明确接收 `jinkiResult.outputAsBaseline()`。
- pipeline 内部不自行寻找“上一层最新输出目录”。

### 3. 禁止事项

禁止：

- pipeline 内部扫描 `resources/out` 找最新目录。
- pipeline 内部默认回到 `game/bsdx` 原始目录。
- 用静态单例保存上一层结果。
- 用全局调度器把层与层的传递藏起来。
- 在 pipeline 中写“如果当前有 JINKI 输出就用 JINKI，否则用原始 BSDX”的隐式逻辑。

## 九、旧 `bhe2bsdx` 的定位

旧 `transfer/bhe2bsdx` 和 `src/test/java/com/giga/nexas/bhe2bsdx` 保留为：

- 需求样本。
- 协议验证样本。
- 转换笔记来源。
- 旧算法参考。

它不再作为新 BHE 长期架构继续增长。

### 可参考的旧类

| 旧位置 | 参考价值 | 新流程落点 |
| --- | --- | --- |
| `TransferTest.testTransSingle()` | 单 WAZ 转换样本入口 | `tsukuyomi/convert/ConvertTsukuyomiWazStep` 和 common WAZ converter |
| `WazConverter` | BHE skill object 转 BSDX object 的实验 | common `BheWazObjectConverter` |
| `MekConverter` | BHE MEK 到 BSDX MEK 的字段映射样本 | `ConvertTsukuyomiMekStep` / common `BheMekConverter` |
| `MekMaterialConverter` | material block 处理样本 | common `BheMekMaterialBlockConverter` |
| `MekAiConverter` | AI 字段错位和 InfoCollection 拷贝样本 | common `BheMekAiConverter` |
| `SpmConverter` | SPM page/chip/image/hitbox 转换样本 | common `BheSpmConverter` |
| `SeGroupIndexMapper` | SE index 映射样本 | common `BheSeReferenceResolver` |
| `SpriteGroupIndexMapper` | Sprite index 映射样本 | common `BheSpriteReferenceResolver` |
| `TransMekaPipeline` | 旧流程串联样本 | 不直接搬，改用 JINKI V2 同构结构 |
| `TransMekaOutputWriter` | 输出落盘样本 | output manifest / audit 化 |
| `UiSpmReplacer` | UI SPM 替换样本 | 机体独立 post 包 |

### 不能直接搬的内容

- 测试侧 `steps` 当生产代码。
- 临时 JSON 路径。
- 单个 `tkytama.waz.json` 写死入口。
- converter 里顺手复制资源。
- output writer 里隐式决定最终文件集合。
- 把所有机体合进一个 `TransMekaPipeline`。

## 十、基于 `20260331` 注释的 convert 任务清单

本节来自 commit：

```text
a80a3ba412bbde694f24d9fdfebfdfae90d11e48
```

这些注释是 BHE convert 阶段必须消化的任务。

### 1. `TransferTest.testTransSingle()`

原注释：

```text
20260331 1 bhe 的所有 *.waz 在这个方法逐个转
```

新定位：

- 它是旧线最小 WAZ 单文件转换样本。
- 新流程不能直接拿它当正式入口。
- 它应沉淀为 common WAZ converter 的测试样本。

tsukuyomi 第一阶段要做：

- 找出 `tsukuyomi` 直接和间接引用的所有 BHE WAZ。
- 对每个 WAZ 做 BHE -> BSDX DTO convert。
- 对共通子弹 WAZ 做整段 append 计划。
- 对引用共通 WAZ 内部 index 的地方做统一偏移。

建议类：

- `ConvertTsukuyomiWazStep`
- `BheWazObjectConverter`
- `BheWazAppendPlan`
- `BheWazInternalIndexRewriter`
- `BheWazConvertAudit`

停点：

- 任一 WAZ 转换后不能 generate / parse。
- 任一 object type 未识别。
- 任一内部 WAZ object index 未重写。

### 2. `MekParser.parseMekMaterialBlock(...)`

原注释：

```text
20260331 2 bhe -> bsdx，常规块（regular）部分直接采用所要移植到目标对象的常规块，
后面挂接的部分置空（flag=0）
```

新定位：

- MEK material 不能机械照抄。
- `tsukuyomi` 的 MEK convert 必须按 BSDX 目标结构重建。
- regular 部分优先采用目标 donor / 目标槽结构。
- BHE trailing / plugin / extra 部分没有可靠映射时置空，flag=0。

建议类：

- `ConvertTsukuyomiMekStep`
- `BheMekMaterialBlockConverter`
- `BheMekMaterialAudit`

停点：

- material block 长度不符合 BSDX 生成器要求。
- flag=0 置空策略无法说明。
- 生成后 MEK 不能 parse 回来。

### 3. `CEventSe`

原注释：

```text
20260331 3
int1：segroup的index
int2：index[int2]
int3：概率（百分数）
int4：？？？（重复几次？）
step1：移植时，bhe的下标，以 filename 为主，去对应找到 bsdx 内的正确的 index[int2]（做寻址收束）
step2：将 bhe 内有，但 bsdx 内没有的 se 放入唯一一个空位内，重排+重定向
```

新定位：

`CEventSe` 是 convert 的核心收束点。

转换规则：

1. 从 BHE group/index 找 filename。
2. 在 `BSDX + JINKI` baseline 的 SE 表中按 filename 查找。
3. 找到则复用目标 group/index。
4. 找不到则在目标层空位中放入 BHE SE。
5. 重排目标 SE 表。
6. 重写所有 `CEventSe` 引用。

注意：

```text
目标层是 BSDX+JINKI，不是原始 BSDX。
```

建议类：

- `BheSeReferenceResolver`
- `BheSeNameIndex`
- `BheSeVacancyAllocator`
- `BheCEventSeRewriter`
- `BheSeAudit`

停点：

- filename 解析失败。
- 空位不足。
- 同一个 BHE SE 映射到多个目标位置。
- 重写后仍残留 BHE index 空间。

### 4. `BheInfoCollection`

#### 4.1 `OBJECTPOS / OBJECTPOS2`

原注释：

```text
20260331 4
term.grp 中 オブジェクト位置 的关联data
以下两个list的元素个数都为 2 or null，
元素1为（オブジェクト位置：OBJECTPOS），直接使用bhe中的termItemDescription，
去bsdx内寻找key，找不到则重定向为 1（標的：ROCK）
元素2为（オブジェクト位置2：OBJECTPOS2），直接使用bhe中的termItemDescription，
去bsdx内寻找key，找不到则重定向为 0（追加処理無し）
```

新定位：

- `BheInfoCollection` 与 `term.grp` 强相关。
- `OBJECTPOS` 和 `OBJECTPOS2` 不能直接照搬 BHE key。
- 映射时必须以 description 查找 `BSDX + JINKI` baseline 中的 term key。
- 找不到时使用明确 fallback：
  - OBJECTPOS -> `1`，`標的：ROCK`。
  - OBJECTPOS2 -> `0`，`追加処理無し`。

建议类：

- `BheTermRegistry`
- `BsdxTermRegistry`
- `ObjectPosTermMapper`
- `BheInfoCollectionConverter`
- `BheTermAudit`

#### 4.2 term 语义拼装与运算子修复

原注释：

```text
20260331 5
int1=termList[0]，int1为term的序列号（index）
第一个语义数为 typelist[0]，
若 typelist[0].param2 > 0（n），则跳转到 typelist[n]，
然后找到 typelist[n].[param2]
以此类推，直到迭代整个 typelist，凑出完整语义
凑出语义后，查看最后一个元素的 param2，
若为负数，则语义拼凑结束，将 paramlist 内的数，按顺序填入到占位符
若为正数，（不可能出现）
此处运算子要修复运算逻辑，保证 bhe 和 bsdx 语义一致，至少要保证语义有含义（无错误）
```

新定位：

- term 语义不是字段拷贝。
- 它要先拼出完整语义，再映射到 BSDX term。
- 运算子逻辑必须修复，否则 AI/skill 条件会错。

建议类：

- `BheTermSemanticResolver`
- `BheTermOperatorMapper`
- `BheInfoCollectionConverter`

停点：

- term 链断裂。
- term 链循环。
- paramList 和占位符不匹配。
- 运算子无对应语义。

### 5. `Spm.SPMHitArea`

原注释：

```text
20260331 6 hitbox转换，见 merge_hitrects_and_unk5.py
```

新定位：

- BHE SPM hitbox 需要转换成 BSDX hit area。
- 不能只复制 rect。
- 需要迁移 `merge_hitrects_and_unk5.py` 的核心策略。

建议类：

- `BheSpmConverter`
- `BheHitboxConverter`
- `BheHitboxMergePolicy`
- `BheSpmAppendPlan`
- `BheSpmInternalIndexRewriter`
- `BheSpmAudit`

特别注意：

如果 SPM 也按整段 append 并入目标同名 SPM，则必须统一平移：

- `imageNo`
- `imageData`
- `pageNo`
- `pageData`
- `animData`

停点：

- shapeType 无规则。
- hitbox merge 策略未迁移。
- SPM 内部 index 未偏移。
- 转换后不能 generate / parse。

### 6. `SkillInfoFactory / CEventEffect / CEventHit`

原注释：

```text
20260331 7 todo 弹幕修复
```

涉及：

- `攻撃：メカ`
- `攻撃：弾`
- `エフェクト`
- `エフェクト番号`
- `ヒットエフェクト`

新定位：

弹幕链横跨：

```text
CEventHit
  -> hit effect
  -> CEventEffect
  -> effect number
  -> WAZ object index
  -> SPM / SE / projectile resources
```

这既是 convert 阶段重点，也是 graft 阶段闭包重点。

建议类：

- `BheProjectileReferenceCollector`
- `BheProjectileConverter`
- `BheProjectileAppendPlan`
- `BheProjectileAudit`

停点：

- hit effect 无目标。
- effect number 无映射。
- projectile 引用未进入闭包。
- 引用共通 WAZ/SPM 后没有做 append 偏移。

## 十一、建议阶段实施计划

### 第一阶段：确认文档和边界

只做文档确认。

不写代码。

停点：

- 本文档未确认，不开始 BHE V2 实现。

### 第二阶段：建立 `tsukuyomi` 独立包和 convert 骨架

先建：

- `bhe2bsdx/v2/meka/tsukuyomi`
- `TsukuyomiConvertSpec`
- `TsukuyomiGraftSpec`
- `TsukuyomiBhePipeline`
- `TsukuyomiConvertContext`
- `TsukuyomiConvertAudit`

当前不主动创建完整 common framework。

如果需要少量 helper，也先服务 `tsukuyomi` 做通；等第二台 BHE 机体开始复用时，再判断是否下沉到 common。

测试：

- 只加载 `tsukuyomi` BHE 源资源。
- 只加载 `BSDX + JINKI` baseline。
- 不转换，先验证显式传值和资源定位。

停点：

- baseline 不是 `BSDX + JINKI`，停止。
- pipeline 内部偷读原始 BSDX，停止。

### 第三阶段：tsukuyomi WAZ + 共通子弹 WAZ append

先做：

- `ConvertTsukuyomiWazStep`
- `BheWazAppendPlan`
- `BheWazInternalIndexRewriter`
- `BheProjectileReferenceCollector`

测试：

- 单 WAZ convert。
- 共通 `bomb.waz` / `tama*.waz` 等整段 append。
- WAZ 内部 object index 统一偏移。
- 转换后 generate / parse。

停点：

- 任一内部 object index 未重写，停止。
- 精细 merge 被误用，停止。

### 第四阶段：tsukuyomi SPM append + hitbox convert

先做：

- `BheSpmConverter`
- `BheHitboxConverter`
- `BheSpmAppendPlan`
- `BheSpmInternalIndexRewriter`

测试：

- imageNo / pageNo 偏移。
- hitbox 转换。
- SPM generate / parse。

停点：

- SPM 只是复制文件，没有偏移内部引用，停止。

### 第五阶段：MEK convert

先做：

- `ConvertTsukuyomiMekStep`
- `BheMekMaterialBlockConverter`
- `BheMekAiConverter`
- `BheInfoCollectionConverter`

测试：

- material regular/trailing 规则。
- AI term 语义。
- MEK generate / parse。

停点：

- material block 不能说明，停止。
- term 语义无含义，停止。

### 第六阶段：SE / term / projectile 全链审计

先做：

- `BheSeReferenceResolver`
- `BheSeVacancyAllocator`
- `ObjectPosTermMapper`
- `BheTermOperatorMapper`
- `BheProjectileAudit`

测试：

- SE filename 定位。
- 空位分配。
- term fallback。
- projectile chain 完整。

停点：

- 任一 fallback 未审计，停止。

### 第七阶段：进入 JINKI V2 同构 graft

先做：

- `BuildTsukuyomiGraftInputStep`
- `TsukuyomiGraftPipeline`
- output manifest / audit。

测试：

- convert 后资产进入 graft。
- GRP append。
- MEK/WAZ/SPM 引用重绑。
- 输出目录可审计。

停点：

- graft 主线里出现 BHE 原始 index 空间，停止。

### 第八阶段：后置和最终打包

先做：

- `TsukuyomiPostPipeline`
- `TsukuyomiSidecarSpec`
- `TsukuyomiExePatchProfile`
- `TsukuyomiPackageTest`

测试：

- 输出目录集合。
- PAC 打包。
- PAC 解包。
- patched exe。

停点：

- 多文件/少文件不可解释，停止。
- 同名文件 byte diff 不可解释，停止。

## 十二、测试与验收设计

### 1. 为什么 test 负责串接

用 test 显式串接层与层，比复杂框架更合适：

- 能明确看到每层输入。
- 能明确保存每层输出。
- 能按层定位失败。
- 能避免 pipeline 隐式读 baseline。
- 能避免隐藏全局状态。
- 能把 `BSDX -> BSDX+JINKI -> BSDX+JINKI+BHE` 写成可读的验收链。

### 2. convert 阶段验收

必须验：

- BHE MEK/WAZ/SPM 转 BSDX DTO。
- DTO 能 generate。
- 生成二进制能 parse。
- SE 引用全部重写到目标层。
- term 语义可拼装。
- hitbox 可转换。
- projectile 引用完整。
- WAZ/SPM append 偏移全部有 audit。

### 3. graft 阶段验收

必须验：

- baseline 是 `BSDX + JINKI`。
- convert 后资源闭包完整。
- GRP append / reuse 正确。
- source index -> target index 映射完整。
- MEK/WAZ/SPM 内部引用不再指向 BHE 原始空间。
- 输出 manifest 能解释所有文件。

### 4. 最终完整打包验收

最终仍回到：

```text
在目标阶段，新 pipeline 的最终打包对象，
与旧/基线流程对应阶段的目标对象，
内部文件集合和文件内容严格一致。
```

无论阶段内怎么分层验收，最终交付时仍然必须回到：

```text
目标阶段最终打包对象内部所有文件完全一致，
一个都不能少，一个都不能多，同名文件内容不能不同。
```

阶段验收可以分层，但最终交付口径不能软化。

对 BHE V2 来说：

- convert 阶段以旧 `bhe2bsdx` 样本和协议断言为基线。
- graft 阶段以 `BSDX + JINKI` 输入和 manifest/audit 为基线。
- final 阶段以输出目录、PAC 解包目录、patched exe bytes 为硬标准。

最终比较：

- 输出目录文件总数。
- 输出目录相对路径集合。
- 输出目录逐文件 bytes。
- `Update3.pac` 解包后文件总数。
- `Update3.pac` 解包后相对路径集合。
- `Update3.pac` 解包后逐文件 bytes。
- patched exe bytes。
- patch site audit。

任意失败，先定位差异来源。

## 十三、暂不实现范围

本轮只重写文档。

暂不做：

- 不删除旧 `bhe2bsdx`。
- 不继续往旧 `bhe2bsdx` 长新 pipeline。
- 不创建 `bhe2bsdx/v2` 代码包。
- 不迁移 `WazConverter`。
- 不迁移 `MekConverter`。
- 不迁移 `SpmConverter`。
- 不实现 tsukuyomi。
- 不实现共通子弹 append。
- 不实现 WAZ 内部 index 偏移。
- 不实现 SPM 内部 index 偏移。
- 不实现 term resolver。
- 不实现 SE 空位分配。
- 不实现 hitbox merge。
- 不实现弹幕修复。
- 不实现 BHE final package test。
- 不改 JINKI V2。
- 不改旧 JINKI pipeline。

旧代码当前只作为：

- 需求样本。
- 转换算法参考。
- 测试数据来源。
- 协议笔记索引。

在这份文档确认之前，不继续开发新 BHE 代码。
