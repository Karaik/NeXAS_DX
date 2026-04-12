# 00. JINKI 流水线 V2 总执行清单

## 1. 整体目标与最终验收标准

当前要重构的是已经跑通的 `JINKI -> BSDX` 流水线。

当前清单状态：

- [x] JINKI V2 全链路已落地。
- [x] V2 主线已经不直接调用旧 `jinki2bsdx.steps.*`。
- [x] 最终输出目录与旧流水线逐文件 byte-identical。
- [x] 最终 `Update3.pac` 解包后内部文件与旧流水线逐文件 byte-identical。
- [ ] BHE converter 接入属于后续阶段，本轮未实现。

重构原因：

- 当前 JINKI 流水线已经能跑完整链路，但主线流程、AKAO 特例、菜单覆写、sidecar、exe 补丁 混在一起。
- 后续还要接 BHE；BHE 会多一层“源游戏资产 -> BSDX 对象格式”的 converter。
- 当前要先把 JINKI 这条同引擎 graft 线整理成可读、可 debug、可继承成果物。
- 这次重构不以“代码优雅”为第一目标，而以“新旧完整产物一致”为第一目标。

最终交付标准：

```text
新流水线打包出来的对象，和旧流水线打包出来的对象，内部所有文件必须完全一致。
```

最终验收必须满足：

- [x] 文件总数一致。
- [x] 文件路径集合一致。
- [x] 文件名集合一致。
- [x] 不能少文件。
- [x] 不能多文件。
- [x] 每个同名文件内容一致。
- [x] 最稳妥标准是逐文件 byte-identical。
- [x] 打包对象解包后比较，结果也必须完全一致。

任意一种情况都算失败：

- [x] 少文件。
- [x] 多文件。
- [x] 同名文件内容不同。
- [x] 路径层级不同。
- [x] 打包后解出的资源集合不一致。

### 1.1 当前 V2 落地结构快照

当前 V2 已经不是第一阶段菜单子实现，也不是平铺步骤列表。

当前根入口：

- [x] `Jinki2BsdxTransferV2`
  - 对外调用入口。
  - 调用方不需要知道内部步骤拆分。

- [x] `JinkiGraftPipelineV2`
  - V2 全链路编排入口。
  - 直接串联 V2 子包内的步骤。
  - 不直接 import / 调用旧 `com.giga.nexas.transfer.jinki2bsdx.steps.*`。

当前 V2 子包结构：

```text
v2
├── graft
├── menu
├── exe
├── output
└── pack
```

职责划分：

- [x] `v2.graft`
  - 源资产加载。
  - 基线加载。
  - 资源闭包。
  - GRP append / reuse。
  - source index -> target index 映射。
  - MEK / WAZ rebind。
  - ProgramMaterial / MapGroup / MekMaterial 对齐。
  - 最终输出目录沉淀。

- [x] `v2.menu`
  - 菜单后置覆盖。
  - 菜单 slot mapping。
  - 菜单 DAT patch。
  - 菜单 SPM image chain rebuild。
  - 菜单 PNG 复制。
  - 菜单 audit。

- [x] `v2.exe`
  - exe 补丁计划。
  - 补丁位点配置。
  - expected / target 字节校验。
  - 补丁执行。
  - 补丁后 exe 一致性校验辅助工具。

- [x] `v2.output`
  - 旁路资源输出建模。
  - output manifest。
  - overwrite audit。
  - 输出目录 / 解包目录相对路径集合比较。
  - 逐文件字节比较。

- [x] `v2.pack`
  - `Update3.pac` 打包。
  - 不承担业务语义。

当前已接管的旧步骤边界：

- [x] `DeserializeJinkiPackageStep` -> `LoadJinkiSourceAssetsStep`
- [x] `LoadBsdxBaselineStep` -> `LoadGraftBaselineStep`
- [x] `BuildImportPlanStep` -> `BuildResourceClosureStep`
- [x] `AppendGrpEntriesStep` -> `AppendGrpEntriesStepV2`
- [x] `SyncProgramMaterialStep` -> `SyncProgramMaterialStepV2`
- [x] `PadBaselineMekMaterialStep` -> `PadBaselineMekMaterialStepV2`
- [x] `RebindAkaoMekStep` -> `RebindMekStepV2`
- [x] `RebindAkaoWazStep` -> `RebindWazStepV2`
- [x] `ImportStaticAssetsStep` -> `ImportStaticAssetsStepV2`
- [x] `PatchMenuDataStep` -> `MenuOverridePipelineV2`
- [x] `PatchExeCapacitiesStep` -> `PatchExeStepV2`
- [x] `PackUpdatePacStep` -> `PackUpdatePacStepV2`

当前结构说明文档：

- [x] `structure-overview.md`
  - 开头写包结构树。
  - 后面写入口和各子包职责。
  - 与本 checklist 的当前状态保持同步。

当前硬验收命令：

```bash
mvn -q "-Dtest=TestJinki2BsdxRunner,TestJinki2BsdxMenuOverrideV2Parity,TestJinki2BsdxSidecarOutputV2Parity,TestJinki2BsdxPipelineV2FinalParity" test
```

其中最终总验收由 `TestJinki2BsdxPipelineV2FinalParity` 承担：

- [x] 旧流水线跑到最终打包。
- [x] V2 流水线跑到最终打包。
- [x] 比较两个输出目录的相对路径集合。
- [x] 比较两个输出目录内每个同名文件字节。
- [x] 复制两个 `Update3.pac`。
- [x] 分别解包两个 `Update3.pac`。
- [x] 比较解包后内部文件相对路径集合。
- [x] 比较解包后内部每个同名文件字节。

## 2. 全流程四大步骤

### 2.1 源游戏转换层

负责：

- [x] 把源游戏资产转换成统一 BSDX 对象格式。
- [x] JINKI 当前基本是 identity。
- [ ] BHE converter 实现（未来阶段，本轮未做）。

不负责：

- [x] 不决定目标槽位。
- [x] 不决定 donor。
- [x] 不执行 graft。
- [x] 不执行菜单覆写。
- [x] 不执行 exe 补丁。
- [x] 不写最终产物。

当前旧代码大致落点：

- `DeserializeJinkiPackageStep`
- `LoadBsdxBaselineStep`
- `JinkiPackageBundle`
- `BsdxBaselineBundle`

当前混入：

- [x] `DeserializeJinkiPackageStep` 中 `WeaponEquip.dat` fallback 属于 旁路资源客制化。
- [x] `AkaoGraftRequest` 同时承担路径、开关和 AKAO 默认 profile。

### 2.2 前置客制化输入

负责：

- [x] 在 graft 主线开始前提供本次移植指定值。
- [x] 固定值允许存在，但必须集中，不散写进主线步骤。

示例：

- [x] 主资源入口：`mek / waz / spm`。
- [x] 目标 codeName。
- [x] 是否固定某个 `MekaGroup` 槽位。
- [x] 菜单可见槽位。
- [x] donor 行。
- [x] 指定 PNG。
- [x] 旁路资源纳入清单。

不负责：

- [x] 不直接改资源。
- [x] 不直接写文件。
- [x] 不直接修改 exe。

当前旧代码大致落点：

- `AkaoGraftRequest`
- `PatchMenuDataStep` 顶部常量
- `PatchMenuDataStep` 中四张 MOD PNG
- `ImportStaticAssetsStep` 中 `WeaponEquip.dat`
- `SyncProgramMaterialStep` 中外部 `ProgramMaterial.grp`

当前混入：

- [x] 菜单 slot / donor / PNG 名散在 `PatchMenuDataStep`。
- [x] `AKAO / 0001 / moribito_2 / Akao.waz` 写在 request 默认值里。
- [x] 旁路资源散在多个步骤。

### 2.3 通用 graft 主线

负责：

- [x] 资源闭包收集。
- [x] GRP append / reuse。
- [x] source index -> target index 映射。
- [x] MEK / WAZ / SPM / DAT 引用重绑。
- [x] SPM 图链重建。
- [x] PNG 尺寸读取与 rect 计算。
- [x] 依赖资源收集。
- [x] 输出产物。

不负责：

- [x] 不知道角色一定是 `AKAO`。
- [x] 不知道菜单一定替第 25 个可见位。
- [x] 不知道图片一定叫 `MOD_001_*`。
- [x] 不知道 exe 补丁 具体偏移。

当前旧代码大致落点：

- `BuildImportPlanStep`
- `AppendGrpEntriesStep`
- `SyncProgramMaterialStep`
- `PadBaselineMekMaterialStep`
- `RebindAkaoMekStep`
- `RebindAkaoWazStep`
- `ImportStaticAssetsStep`
- `PackUpdatePacStep`

当前混入：

- [x] `RebindAkaoMekStep / RebindAkaoWazStep` 名称和部分说明仍 AKAO 化。
- [x] `ImportStaticAssetsStep` 同时做产物写出、辅助 WAZ 输出、图片收集、音频收集、旁路资源 dat 覆写。
- [x] 菜单 SPM 图链重建目前仍放在 `PatchMenuDataStep`。

### 2.4 后置客制化 / 兼容 patch / 输出沉淀

负责：

- [x] 在主线产出映射和 bundle 后，按本次指定事实做覆写。
- [x] 在上一层成果物基础上继续累加兼容 patch。
- [x] 形成下一层可继承成果物。

示例：

- [x] 菜单 dat 覆写。
- [x] 菜单 SPM 图链覆写。
- [x] 旁路资源 dat 覆写。
- [x] battle voice 门槛 bypass。
- [x] meka capacity patch。
- [x] 输出目录沉淀。

不负责：

- [x] 不重新收集 WAZ 闭包。
- [x] 不重新决定通用 remap 规则。
- [x] 不读取源游戏原始格式。

当前旧代码大致落点：

- `PatchMenuDataStep`
- `PatchExeCapacitiesStep`
- `ImportStaticAssetsStep` 中 `WeaponEquip.dat`
- `PackUpdatePacStep`

当前混入：

- [x] `PatchMenuDataStep` 同时承担前置指定值、后置 dat/spm 覆写、PNG 复制、文件输出。
- [x] `PatchExeCapacitiesStep` 名义是 capacity 补丁，实际混入兼容 patch。
- [x] 输出目录尚未明确建模成下一层 基线。

## 3. 成果物链式继承模型

目标链：

```text
BSDX
  -> BSDX+JINKI
  -> BSDX+JINKI+BHE
  -> ...
```

上一层输出成为下一层 基线：

- [x] 每次 graft 输出目录都应能作为下一次 graft 的输入 基线。
- [x] 下一次 graft 不总是从原始 BSDX 开始。
- [x] 测试应能连续表达：
  - `基线0 = original BSDX`
  - `result1 = graft JINKI on 基线0`
  - `基线1 = result1 output`
  - `result2 = graft BHE on 基线1`

同名资源覆盖原则：

- [x] 新成果物中同名资源覆盖旧成果物。
- [x] 覆盖必须进入 清单 / audit。
- [x] dat/grp/mek/waz/spm 覆盖后必须能重新 parse。
- [x] png/ogg/wav 按文件名覆盖。

exe 补丁 累加原则：

- [x] 补丁输入是上一层 补丁后 exe。
- [x] 补丁位点 允许当前值为 expected 或 target。
- [x] 当前值已经是 target 时，记录为 already patched。
- [x] 当前值既不是 expected 也不是 target 时，失败。
- [x] 容量 patch 根据当前成果物最终 GRP count 计算。

## 4. 当前代码里最混乱、最该先下刀的点

第一刀：`PatchMenuDataStep`。

原因：

- [x] 它混入最多菜单客制化：
  - 第 25 个可见位。
  - donor state 行。
  - 四张 MOD PNG。
  - `M_` 菜单资源推导。
  - `Meka.dat` source-only 规则。
- [x] 它同时做多类工作：
  - dat patch。
  - spm patch。
  - PNG 尺寸读取。
  - SPM page/chip/imageData 重建。
  - PNG 复制。
  - 文件输出。
- [x] 它和 MEK/WAZ 主线耦合较低，适合先旁路并行实现。

它和后续 BHE 接入的关系：

- [x] BHE 以后也会有菜单 donor、菜单图、pilot 图、select menu 图等后置客制化。
- [x] 先把菜单 override 的主线规律和指定值分开，BHE 接入时可以复用菜单 override 主线，只替换客制化输入。

本文处理方式：

- [x] 不再拆出第一阶段子 清单。
- [x] 菜单 override 只作为总 清单 的第一阶段。
- [x] 第一阶段细节直接写在本文第 5、6、7 节里。
- [x] 本文必须一直覆盖到最终打包对象完整一致性验收。

## 5. 整个新流水线 的开发阶段顺序

### 5.1 第一阶段：菜单 override 并行实现

目标：

- [x] 旧 `PatchMenuDataStep` 不动。
- [x] 旁路实现菜单 override。
- [x] 新旧菜单产物一致。

范围：

- [x] `Meka.dat`
- [x] `MekaPilot.dat`
- [x] `SelectMekaMenu.dat`
- [x] `MekaPilot.spm`
- [x] `SelectMekaMenuMeka.spm`
- [x] 菜单 PNG 集合

锁住规律：

- [x] 从 `SelectMekaMenu.dat` 推导菜单 anim。
- [x] 从 `MekaPilot.dat` 反查 pilot anim。
- [x] 从 anim.patData 推导 page。
- [x] 重建 SPM page/chip/imageData 链。
- [x] 根据真实 PNG 尺寸和 layout policy 计算 rect。
- [x] 根据 补丁后 SPM 复制菜单 PNG。

与最终完整打包一致性的关系：

- [x] 第一阶段只保证菜单产物新旧一致。
- [x] 第一阶段不比较完整 `Update3.pac`。

### 5.2 第二阶段：sidecar / 输出产物边界整理

目标：

- [x] 把 `WeaponEquip.dat`、外部 `ProgramMaterial.grp`、菜单资源复制等 sidecar 从主线步骤 中剥离。
- [x] 形成可审计 output manifest。

范围：

- [x] `ImportStaticAssetsStep` 中 旁路资源 dat 输出。
- [x] `SyncProgramMaterialStep` 中外部 ProgramMaterial 读取。
- [x] 输出目录同名覆盖记录。

锁住规律：

- [x] 旁路资源由客制化输入声明。
- [x] 输出行为进入 清单。
- [x] 同名资源覆盖可追踪。

与最终完整打包一致性的关系：

- [x] 第二阶段开始比较更大范围的输出目录文件集合。
- [x] 仍不要求完整 流水线 替换旧实现。

### 5.3 第三阶段：兼容 patch 累加模型

目标：

- [x] 把 exe 补丁 从 `PatchExeCapacitiesStep` 中整理为 补丁位点 列表和累加执行模型。
- [x] 支持基于上一层成果物继续 补丁。

范围：

- [x] meka capacity patch。
- [x] WeaponEquip fill bound patch。
- [x] battle voice 门槛 bypass。
- [x] SelectMekaMenu row bound patch。

锁住规律：

- [x] 补丁位点 支持 expected 或 target。
- [x] 已补丁的位点 不重复失败。
- [x] 异常字节直接失败。

与最终完整打包一致性的关系：

- [x] 第三阶段开始比较 补丁后 exe。
- [x] exe 补丁 差异必须可定位到 补丁位点。

### 5.4 第四阶段：主线 graft 并行实现

目标：

- [x] 在旁边重写完整 graft 主线。
- [x] 旧流水线 继续保留。
- [x] 新旧完整输出目录逐步一致。

范围：

- [x] resource closure。
- [x] GRP append/reuse。
- [x] index remap。
- [x] MEK/WAZ rebind。
- [x] image/audio dependency collection。
- [x] output manifest。

锁住规律：

- [x] 主线不散写 AKAO 特例。
- [x] 客制化输入集中。
- [x] 每个阶段输出可审计。

与最终完整打包一致性的关系：

- [x] 第四阶段比较完整输出目录。
- [x] 每个同名文件尽量 byte-identical。

### 5.5 第五阶段：最终打包对象一致性验收

目标：

- [x] 新旧流水线 都跑到最终打包完成。
- [x] 解包两个最终打包对象。
- [x] 比较解包后的全部文件集合和文件内容。

锁住规律：

- [x] 新流水线 最终行为等价于旧流水线。
- [x] 新流水线 可作为后续 BHE 的稳定基线。

## 6. 每个阶段的代码实现 清单

### 6.1 第一阶段：菜单 override

先建：

- [x] `MenuOverrideSpec`
- [x] `MenuLayoutPolicy`
- [x] `MenuSlotMapping`
- [x] `MenuOverrideAudit`
- [x] `MenuOverrideContext`

顶层 步骤：

- [x] `ResolveMenuSlotStep`
- [x] `PatchMenuDatStep`
- [x] `PatchMenuSpmStep`
- [x] `CopyMenuImagesStep`

helper / context / audit：

- [x] `MenuSpmImageChainRebuilder`
- [x] `MenuOverrideContext`
- [x] `MenuOverrideAudit`

先落代码：

- [x] slot 推导。
- [x] dat patch。
- [x] SPM 图链重建。
- [x] PNG 复制。
- [x] 菜单产物写出。

先测：

- [x] slot 推导测试。
- [x] dat 一致性校验 测试。
- [x] spm 一致性校验 测试。
- [x] PNG 输出测试。
- [x] 菜单 流水线 一致性校验 测试。

停点：

- [x] 任一菜单产物新旧 字节 不一致，停止。

### 6.2 第二阶段：sidecar / 输出产物边界

先建：

- [x] `SidecarResourceSpec`
- [x] `OutputManifest`
- [x] `OutputResourceEntry`

顶层 步骤：

- [x] `BuildSidecarOutputsStep`
- [x] `WriteOutputManifestStep`

helper / context / audit：

- [x] `OutputManifestComparator`
- [x] `ResourceOverwriteAudit`

先落代码：

- [x] 把 `WeaponEquip.dat` 补齐逻辑搬出静态资源 步骤。
- [x] 把外部 `ProgramMaterial.grp` 读取声明化。
- [x] 记录每个输出文件来源。
- [x] 记录同名覆盖。

先测：

- [x] 旁路资源 dat 新旧 byte-identical。
- [x] output manifest 文件集合包含旧输出集合。
- [x] 同名覆盖记录可读。

停点：

- [x] 旁路资源输出与旧实现不一致，停止。

### 6.3 第三阶段：兼容 patch 累加

先建：

- [x] `ExePatchSite`
- [x] `ExePatchProfile`
- [x] `ExePatchAudit`

顶层 步骤：

- [x] `BuildExePatchPlanStep`
- [x] `ApplyExePatchStep`

helper / context / audit：

- [x] `PatchBytesVerifier`
- [x] `PatchedExeComparator`

先落代码：

- [x] 把 meka capacity patch位点 迁入 配置。
- [x] 把 `WeaponEquip` fill bound 补丁位点 迁入 配置。
- [x] 把 battle voice bypass 补丁位点 迁入 配置。
- [x] 补丁执行支持 expected / target 双合法值。

先测：

- [x] 补丁后 exe 新旧 byte-identical。
- [x] already 补丁后 exe 再 patch 不失败。
- [x] 异常字节直接失败。

停点：

- [x] exe 字节 不一致且不能解释为合法差异，停止。

### 6.4 第四阶段：通用 graft 主线

先建：

- [x] `LoadJinkiSourceAssetsStep`
- [x] `LoadGraftBaselineStep`
- [x] `BuildResourceClosureStep`
- [x] `AppendGrpEntriesStepV2`
- [x] `SyncProgramMaterialStepV2`
- [x] `PadBaselineMekMaterialStepV2`
- [x] `RebindMekStepV2`
- [x] `RebindWazStepV2`
- [x] `ImportStaticAssetsStepV2`

顶层 步骤：

- [x] `LoadJinkiSourceAssetsStep`
- [x] `LoadGraftBaselineStep`
- [x] `BuildResourceClosureStep`
- [x] `AppendGrpEntriesStepV2`
- [x] `SyncProgramMaterialStepV2`
- [x] `PadBaselineMekMaterialStepV2`
- [x] `RebindMekStepV2`
- [x] `RebindWazStepV2`
- [x] `ImportStaticAssetsStepV2`

helper / context / audit：

- [x] WAZ 闭包遍历逻辑已落在 `BuildResourceClosureStep`。
- [x] SPM 依赖收集逻辑已落在 `ImportStaticAssetsStepV2`。
- [x] 图片依赖收集逻辑已落在 `ImportStaticAssetsStepV2`。
- [x] 音频依赖收集逻辑已落在 `ImportStaticAssetsStepV2`。
- [x] GRP 映射审计通过 `GrpAppendPlan` 和 final 一致性校验 固化。
- [x] 输出审计通过 `ImportedAssetSet` / `OutputManifest` / `OutputManifestComparator` 固化。

先落代码：

- [x] 已迁 source loading。
- [x] 已迁 基线 loading。
- [x] 已迁 resource closure。
- [x] 已迁 GRP mapping。
- [x] 已迁 ProgramMaterial / MapGroup / MekMaterial 对齐。
- [x] 已迁 MEK/WAZ rebind。
- [x] 已迁 dependency collection。
- [x] 已迁 output writing。

先测：

- [x] 每个中间 mapping 与旧实现 semantic-equal。
- [x] 每类核心文件与旧输出 byte-identical。
- [x] 完整输出目录文件集合一致。

停点：

- [x] 任一核心资源不一致且无法解释，停止。

### 6.5 第五阶段：最终打包一致性

先建：

- [x] `PackUpdatePacStepV2`
- [x] `OutputManifestComparator`
- [x] `PatchedExeComparator`
- [x] `TestJinki2BsdxPipelineV2FinalParity`

顶层 步骤：

- [x] 旧流水线 快照 由 `TestJinki2BsdxPipelineV2FinalParity` 直接运行旧 `Jinki2BsdxSingleRunner` 生成。
- [x] V2 流水线 快照 由 `TestJinki2BsdxPipelineV2FinalParity` 直接运行 `Jinki2BsdxTransferV2` 生成。
- [x] 最终目录和解包对象比较由 `OutputManifestComparator` 执行。

helper / context / audit：

- [x] PAC 解包使用 `PacUtil.unpack`。
- [x] 文件树 快照 使用 `OutputManifestComparator.collectRelativeFiles`。
- [x] 字节 diff 失败信息使用 `OutputManifestComparator.ComparisonResult#allDifferences`。
- [x] 补丁后 exe 字节 diff 使用 `PatchedExeComparator`。

先落代码：

- [x] 旧流水线 完整跑到 `Update3.pac`。
- [x] 新流水线 完整跑到 `Update3.pac`。
- [x] 解包旧 pac。
- [x] 解包新 pac。
- [x] 比较文件集合。
- [x] 比较每个同名文件字节。

先测：

- [x] 文件总数一致。
- [x] 文件路径集合一致。
- [x] 每个同名文件 byte-identical。

停点：

- [x] 少文件，停止。
- [x] 多文件，停止。
- [x] 同名文件 字节 不一致，停止。

## 7. 测试与停点 清单

阶段内小测试：

- [x] 每个 步骤 的输入输出对象正确。
- [x] 每个 步骤 的关键推导结果正确。
- [x] 每个 helper 的核心计算正确。

阶段 一致性校验 测试：

- [x] 菜单阶段：菜单 dat/spm/png 新旧一致。
- [x] 旁路资源阶段：旁路资源 dat/grp 新旧一致。
- [x] patch 阶段：补丁后 exe 新旧一致。
- [x] 主线阶段：核心 dat/grp/mek/waz/spm 新旧一致。

阶段集成测试：

- [x] 新阶段接入后，旧完整测试仍通过。
- [x] 新阶段局部 流水线 可独立跑。

最终总验收测试：

- [x] 新旧流水线 都跑到最终打包完成。
- [x] 解包两个最终打包对象。
- [x] 比较解包后的所有文件集合。
- [x] 比较每个同名文件内容。
- [x] 任意差异都算失败。

停点：

- [x] 小测试失败，不进入该阶段后续 步骤。
- [x] 一致性校验 测试失败，不进入下一阶段。
- [x] 阶段集成失败，不进入下一阶段。
- [x] 最终打包对象不一致，不视为完成。

## 8. 最终打包对象完整一致性的验收 清单

比较对象：

- [x] 旧流水线 产出的最终 `Update3.pac`。
- [x] 新流水线 产出的最终 `Update3.pac`。

比较时机：

- [x] 新流水线 完整实现后。
- [x] 新旧流水线 都完成打包后。

比较粒度：

- [x] 先比较 pac 文件本身是否 byte-identical。
- [x] 如果 pac 文件 字节 不一致，解包后继续比较内部文件。
- [x] 内部文件必须逐文件 byte-identical。

文件集合比较：

- [x] 解包旧 pac，生成旧文件树 快照。
- [x] 解包新 pac，生成新文件树 快照。
- [x] 比较文件总数。
- [x] 比较相对路径集合。
- [x] 比较文件名集合。
- [x] 少文件失败。
- [x] 多文件失败。
- [x] 路径层级不同失败。

文件内容比较：

- [x] 对每个同名相对路径读取 字节。
- [x] byte-identical 通过。
- [x] 字节 不一致失败。
- [x] 失败时记录 SHA256、大小、相对路径。

失败定位：

- [x] 根据相对路径回溯到 output manifest。
- [x] 根据 清单 判断来源 步骤。
- [x] 对 dat/grp/mek/waz/spm 可追加 parse 后 semantic diff。
- [x] 对 png/audio 记录 hash 和大小。
- [x] 对 exe 补丁 记录 补丁位点。

必须保留：

- [x] output manifest。
- [x] overwrite audit。
- [x] menu override audit。
- [x] exe 补丁 audit。
- [x] final packed output diff report 当前由测试失败信息输出；需要文件化时接 `DiffReportWriter`。

## 9. Lombok 使用原则

- [x] 数据承载对象适合用 Lombok。
- [x] 步骤/helper 类不乱用 Lombok。
- [x] 不为了省几行代码影响可读性。
- [x] context / mapping / audit / spec 可用 Lombok 减少样板。
- [x] 业务计算方法手写。
- [x] 关键流程不要被 builder 链隐藏。
- [x] 只读推导结果使用 `@Getter` + 构造器。
- [x] 可变上下文使用 `@Data`。
- [x] audit 使用 `@Data` + 手写 add 方法。
- [x] spec 不使用 `@Data`，避免 步骤 内随意修改。

## 10. 暂不实现范围

当前不做：

- [x] 不直接删除旧流水线。
- [x] 不直接替换旧 runner。
- [x] 不直接接 BHE 转换器。
- [x] 不做大而全 DSL。
- [x] 不一次性重写所有 步骤。
- [x] 不为了通用性把所有固定文件名配置化。
- [x] 不为了架构分层牺牲可读性。
- [x] 不在未通过 一致性校验 前接管旧流程。

后续阶段位置：

- [x] 旁路资源输出边界整理挂在第二阶段。
- [x] exe 补丁 累加模型挂在第三阶段。
- [x] 完整 graft 主线并行实现挂在第四阶段。
- [x] 完整输出目录 一致性校验 挂在第四阶段和第五阶段之间。
- [x] 最终 pac 解包 一致性校验 是第五阶段必须完成的总验收，不是可选项。
- [x] BHE converter 接入挂在 JINKI V2 完整验收之后。

## 11. 开发推进原则

- [x] 每一阶段都保留旧实现基线。
- [x] 每一阶段都先做旁路实现。
- [x] 每一阶段都必须有新旧一致性测试。
- [x] 每一阶段都必须能单独验收。
- [x] 只有当前阶段 一致性校验 通过，才能进入下一阶段。
- [x] 不允许用后续阶段修补当前阶段未解释的差异。
