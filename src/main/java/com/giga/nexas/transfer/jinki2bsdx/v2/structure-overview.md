# JINKI 流水线 V2 结构总览

## 1. 目录结构

```text
com.giga.nexas.transfer.jinki2bsdx.v2
├── Jinki2BsdxTransferV2.java
├── JinkiGraftPipelineV2.java
├── rewrite-checklist.md
├── structure-overview.md
├── graft
│   ├── LoadJinkiSourceAssetsStep.java
│   ├── LoadGraftBaselineStep.java
│   ├── BuildResourceClosureStep.java
│   ├── AppendGrpEntriesStepV2.java
│   ├── SyncProgramMaterialStepV2.java
│   ├── PadBaselineMekMaterialStepV2.java
│   ├── RebindMekStepV2.java
│   ├── RebindWazStepV2.java
│   └── ImportStaticAssetsStepV2.java
├── menu
│   ├── MenuOverridePipelineV2.java
│   ├── MenuOverrideSpec.java
│   ├── MenuOverrideContext.java
│   ├── MenuOverrideAudit.java
│   ├── MenuSlotMapping.java
│   ├── ResolveMenuSlotStep.java
│   ├── PatchMenuDatStep.java
│   ├── PatchMenuSpmStep.java
│   ├── MenuSpmImageChainRebuilder.java
│   ├── CopyMenuImagesStep.java
│   └── MenuLayoutPolicy.java
├── exe
│   ├── PatchExeStepV2.java
│   ├── BuildExePatchPlanStep.java
│   ├── ApplyExePatchStep.java
│   ├── ExePatchProfile.java
│   ├── ExePatchSite.java
│   ├── ExePatchAudit.java
│   ├── PatchBytesVerifier.java
│   └── PatchedExeComparator.java
├── output
│   ├── BuildSidecarOutputsStep.java
│   ├── SidecarResourceSpec.java
│   ├── OutputManifest.java
│   ├── OutputResourceEntry.java
│   ├── OutputManifestSupport.java
│   ├── OutputManifestComparator.java
│   ├── ResourceOverwriteAudit.java
│   └── WriteOutputManifestStep.java
└── pack
    └── PackUpdatePacStepV2.java
```

## 2. 顶层入口

- `Jinki2BsdxTransferV2`
  - V2 对外入口。
  - 调用方只需要传入 `AkaoGraftRequest`。
  - 调用方不需要知道内部 step 拆分。

- `JinkiGraftPipelineV2`
  - V2 完整编排入口。
  - 负责串联源资产加载、基线加载、资源闭包、索引映射、MEK/WAZ 重绑、输出沉淀、菜单覆写、EXE 补丁和 PAC 打包。
  - 不直接调用旧 `jinki2bsdx.steps` 包内的步骤。
  - 所有内部步骤都受最终输出目录 一致性校验 和解包后 `Update3.pac` 一致性校验 保护。

- `rewrite-checklist.md`
  - 总执行清单。
  - 记录开发阶段、完成状态和最终验收标准。

- `structure-overview.md`
  - 当前文件。
  - 说明包结构、入口职责和复用边界。

## 3. `graft` 包职责

`graft` 是可复用主线。

它负责：

- 加载 JINKI 源侧资源对象。
- 加载当前 BSDX 基线对象。
- 从 MEK/WAZ/SPM/SE 引用构建资源闭包。
- 追加或复用 GRP 条目。
- 生成 源索引到目标索引 的映射。
- 通过映射重绑 MEK 和 WAZ 内部引用。
- GRP 增长后同步 ProgramMaterial、MapGroup 和 基线 MEK material 长度。
- 写出最终平铺输出目录，作为打包输入。

它不负责：

- 菜单 donor 槽位常量。
- MOD 菜单 PNG 文件名。
- EXE 补丁 字节偏移。
- BHE 源格式转换器规则。

## 4. `menu` 包职责

`menu` 是后置客制化层。

它负责：

- 从 `SelectMekaMenu.dat` 和 `MekaPilot.dat` 推导菜单槽位。
- patch 菜单 DAT 文件。
- 根据真实 PNG 尺寸重建菜单 SPM 图链。
- 复制 patch 后菜单 SPM 的 `imageData` 实际引用到的 PNG。
- 审计写出的菜单文件、复制的图片和缺失图片。

复用边界是 `MenuOverrideSpec`。

以后换角色、换 donor、换菜单图时，优先改 `MenuOverrideSpec`，不要改菜单图链重建算法。

## 5. `exe` 包职责

`exe` 是兼容 补丁层。

它负责：

- 计算容量和菜单行数需求。
- 在 `ExePatchProfile` 中定义 补丁位点。
- 用 expected 或 target 字节校验 patch site。
- 记录 applied / already patched 位点。
- 在测试中直接比较 补丁后 exe。

这个包支撑链式成果物：

```text
BSDX -> BSDX+JINKI -> BSDX+JINKI+BHE -> ...
```

如果后续某一层从已经 patched 的 exe 继续开始，target 字节是合法状态，不应该导致失败。

## 6. `output` 包职责

`output` 是输出审计和比较层。

它负责：

- 旁路资源输出建模。
- 输出清单构建。
- 相对路径文件集合比较。
- 逐文件字节比较。
- 同名资源覆盖审计。

最终验收比较的是相对路径，不只是文件名。

以下任意一种都算失败：

- 少文件。
- 多文件。
- 相对路径不同。
- 同一路径文件内容不同。

## 7. `pack` 包职责

`pack` 只负责最终 PAC 打包。

它不理解 MEK / WAZ / menu / exe 的业务语义。

`PackUpdatePacStepV2` 接收输出目录，写出 `Update3.pac`。

正确性不靠 打包步骤 自己判断，而是打包后解包，再比较内部文件集合和逐文件字节。

## 8. 当前测试门槛

当前必须通过：

```bash
mvn -q "-Dtest=TestJinki2BsdxRunner,TestJinki2BsdxMenuOverrideV2Parity,TestJinki2BsdxSidecarOutputV2Parity,TestJinki2BsdxPipelineV2FinalParity" test
```

最强验收测试是 `TestJinki2BsdxPipelineV2FinalParity`：

- 旧流水线 跑到最终打包完成。
- V2 流水线 跑到最终打包完成。
- 比较两个输出目录的相对路径集合。
- 比较两个输出目录内每个文件的 bytes。
- 复制两个 `Update3.pac`。
- 解包两个 `Update3.pac`。
- 比较解包后内部文件的相对路径集合。
- 比较解包后内部每个文件的 bytes。

局部对象断言只能辅助定位。

最终完成标准始终是：生成物和旧流水线 完全一致。
