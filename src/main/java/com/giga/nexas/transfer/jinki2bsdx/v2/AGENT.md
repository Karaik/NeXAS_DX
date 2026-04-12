# JINKI -> BSDX V2 本地维护规则

本文件只约束 `src/main/java/com/giga/nexas/transfer/jinki2bsdx/v2` 及其子目录。进入这个目录工作时，优先遵守这里的规则；根目录 `AGENT.md` 的编码、换行、测试和提交约束仍然有效。

## 1. 完成标准

V2 的最终目标不是“逻辑上差不多”，而是：

```text
V2 pipeline 生成物必须和旧 pipeline 生成物完全一致。
```

判断标准固定为：

- 输出目录文件总数一致。
- 输出目录相对路径集合一致。
- 输出目录每个同名文件 byte-identical。
- `Update3.pac` 解包后的内部文件总数一致。
- `Update3.pac` 解包后的内部相对路径集合一致。
- `Update3.pac` 解包后的内部每个同名文件 byte-identical。
- patched exe byte-identical。

任意少文件、多文件、路径不同、同名内容不同、PAC 解包后集合不同，都算失败。失败时先定位差异来源，不要通过放宽断言、跳过文件或改旧 pipeline 来让测试变绿。

## 2. 当前流水线结构

```text
v2
├── Jinki2BsdxTransferV2.java
├── JinkiGraftPipelineV2.java
├── graft
├── menu
├── exe
├── output
└── pack
```

根包只保留顶层入口和编排：

- `Jinki2BsdxTransferV2`
  - 对外入口。
  - 接收 `AkaoGraftRequest`。
  - 不暴露内部 step 拆分。

- `JinkiGraftPipelineV2`
  - 完整 V2 编排。
  - 只负责把各层 step 串起来。
  - 不直接写具体菜单图链算法、EXE patch 字节、sidecar 文件列表等细节。

新增业务时优先放进对应子包。不要把新 step、helper、规则常量继续堆回 `v2` 根包。

## 3. 子包职责边界

### `graft`

`graft` 是通用 graft 主线，未来 BHE 也应尽量复用这一层。

负责：

- 加载 JINKI 源资产对象。
- 加载 BSDX baseline 对象。
- 根据 MEK/WAZ/SPM/SE 引用构建资源闭包。
- 追加或复用 GRP 条目。
- 建立 source index 到 target index 的映射。
- 重绑 MEK 内部的 GRP、SPM、SE 等索引。
- 重绑 WAZ 内部的 MEK、SPM、SE 等索引。
- GRP 扩容后同步 `ProgramMaterial`、`MapGroup`、baseline MEK material 长度。
- 导入主资源链引用到的静态资源。

不负责：

- 菜单 donor 槽位。
- 菜单 MOD PNG 文件名。
- EXE patch offset。
- BHE 源格式转换规则。
- 测试用 manifest 比较。

如果一个常量只对 AKAO 菜单生效，它不属于 `graft`。如果一个规则未来换 BHE 源游戏也成立，它才应该优先考虑放进 `graft`。

### `menu`

`menu` 是后置客制化层，不是 graft 主线。

负责：

- 从 `SelectMekaMenu.dat` 和 `MekaPilot.dat` 推导当前可见菜单槽位。
- patch `Meka.dat`、`MekaPilot.dat`、`SelectMekaMenu.dat`。
- 按真实 PNG 尺寸重建 `MekaPilot.spm` / `SelectMekaMenuMeka.spm` 的 `anim -> pageData -> chipData -> imageData` 链。
- 复制 patched 菜单 SPM 实际引用的 PNG。
- 通过 `MenuOverrideAudit` 记录 slot mapping、DAT/SPM patch、写出文件、复制图片、缺失图片。

客制化入口是 `MenuOverrideSpec`。换角色、换 donor、换菜单图片时，优先改 spec 或 spec 构造方式，不要直接改图链重建算法。

### `exe`

`exe` 是兼容 patch 层。

负责：

- 建立 `ExePatchProfile`。
- 根据目标容量/菜单行数生成 patch plan。
- 校验 patch site 的 expected bytes 或 target bytes。
- 写入 patch bytes。
- 记录 applied / already patched / skipped / failed。
- 比较 patched exe 是否和旧 pipeline 结果一致。

这里必须支持链式成果物：

```text
BSDX -> BSDX+JINKI -> BSDX+JINKI+BHE -> ...
```

如果输入 exe 已经是 target bytes，应视为 `already patched`，不要误判为失败。

### `output`

`output` 是输出沉淀、审计和最终比较层。

负责：

- 建模 sidecar 输出资源。
- 记录输出资源的来源、目标相对路径、覆盖关系。
- 生成 output manifest。
- 比较两个输出目录的相对路径集合。
- 逐文件比较 bytes。
- 记录同名覆盖 audit。

最终比较必须使用相对路径，不能只比较文件名。manifest 和 audit 只用于定位差异，不能写进最终打包目录导致输出集合变大。

### `pack`

`pack` 只负责生成 `Update3.pac`。

负责：

- 接收最终输出目录。
- 调用打包能力写出 `Update3.pac`。

不负责：

- 判断 MEK/WAZ/menu/exe 的业务正确性。
- 修改输出目录内容。
- 过滤或补文件。

打包正确性靠打包后解包，再比较内部文件集合和 bytes 验证。

## 4. 旧 pipeline 边界

旧 pipeline 是 V2 的验收基线，默认不要改。

不要为了让 V2 测试通过去修改：

- `AkaoGraftPipeline`
- `PatchMenuDataStep`
- `PatchExeCapacitiesStep`
- `ImportStaticAssetsStep`
- 旧 runner

除非用户明确要求迁移、删除或修旧实现，否则所有 V2 差异都先在 V2 内定位。

## 5. 文档和注释规则

文档必须写中文，但类名、文件名、方法名、命令保持真实名称，不要翻译。

本目录核心文档：

- `structure-overview.md`
  - 说明当前包结构、入口、职责和复用边界。

- `rewrite-checklist.md`
  - 说明总执行 checklist、阶段状态和未完成范围。
  - 已完成项必须打勾。
  - 后续 BHE converter 等未实现内容保持未勾。

注释规则：

- 顶层 step 要说明“这一步在流水线里解决什么问题”。
- helper 要说明“为什么需要这个 helper，以及它维护哪个不变量”。
- 数据承载对象要说明字段含义，尤其是 index、mapping、source/target、baseline/output/root 这类容易混淆的字段。
- 不要写空泛注释，例如“设置值”“获取值”。
- 复杂业务块前应补一句业务意图，例如“这里复用旧 imageData 槽位，是为了保持 SPM imageNo 稳定并避免输出集合扩大”。

## 6. 测试门槛

修改 V2 代码后至少运行：

```bash
mvn -q -Dtest=TestJinki2BsdxPipelineV2FinalParity test
```

最终提交前运行完整门槛：

```bash
mvn -q "-Dtest=TestJinki2BsdxRunner,TestJinki2BsdxMenuOverrideV2Parity,TestJinki2BsdxSidecarOutputV2Parity,TestJinki2BsdxPipelineV2FinalParity" test
```

测试职责：

- `TestJinki2BsdxRunner`
  - 旧 pipeline 基线回归。

- `TestJinki2BsdxMenuOverrideV2Parity`
  - 菜单 DAT、SPM、PNG 的 V2 局部一致性。

- `TestJinki2BsdxSidecarOutputV2Parity`
  - sidecar 输出和 output manifest 边界一致性。

- `TestJinki2BsdxPipelineV2FinalParity`
  - 跑旧 pipeline 到最终打包。
  - 跑 V2 pipeline 到最终打包。
  - 比较两个输出目录。
  - 复制并解包两个 `Update3.pac`。
  - 比较解包后的内部文件集合和 bytes。
  - 比较 patched exe bytes。

局部对象相等只能辅助定位，不能替代最终生成物 byte parity。

## 7. 输出集合规则

不要扩大最终输出集合。

尤其不要：

- 为了“保险”复制整套 SPM `imageData` 引用图片。
- 把 BSDX 原生 skill 的整套图集带进 `Update3.pac`。
- 把临时 manifest、diff、解包目录写进最终打包目录。
- 在 pack step 内临时补文件。

如果需要排查差异，把审计文件写到测试临时目录或明确的非打包目录。

## 8. Git 暂存规则

只有最终 parity 通过后，才暂存 V2 相关文件。

可以暂存：

- `src/main/java/com/giga/nexas/transfer/jinki2bsdx/v2/**`
- 对应 V2 parity 测试

不要暂存：

- `src/main/resources/out/**`
- PAC 解包目录
- 临时 diff 文件
- 大量资源分析输出

如果工作区已有用户或测试生成的无关改动，不要回滚它们，也不要顺手暂存它们。
