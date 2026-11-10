# Transfer Pipeline Skill

## 1. 使用场景

当任务涉及 `transfer` 目录下的移植流水线时，优先按本 skill 的规则理解问题：

- `JINKI -> BSDX` 当前 V2 重构。
- 未来 `BHE -> BSDX+JINKI` 接入。
- 资源 graft、菜单 override、EXE patch、输出沉淀、PAC 打包。
- 新旧 pipeline 生成物一致性排查。
- `BSDX -> BSDX+JINKI -> BSDX+JINKI+BHE -> ...` 链式成果物继承。

本 skill 的目的不是引入新的大框架，而是把已经跑通的移植经验沉淀成可复用的工作方法：先保证 JINKI V2 和旧 pipeline 完全一致，再把这条稳定主线留给后续 BHE 复用。

## 2. 背景

当前 `JINKI -> BSDX` 旧 pipeline 已经能跑通，也能生成可用结果。它的问题不是功能缺失，而是职责混杂：

- 通用 graft 主线和角色客制化混在一起。
- 菜单 DAT/SPM/PNG override 混在主线步骤里。
- EXE patch 规则和输出沉淀规则不够独立。
- 后续 BHE 如果直接照抄，会再次重复 debug 同一批索引、输出和打包问题。

V2 的真实目标是整理出一个稳定基线：

```text
BSDX -> BSDX+JINKI -> BSDX+JINKI+BHE -> ...
```

上一层成果物会成为下一层 baseline。后续 BHE 不应每次从原始 BSDX 另开一条线，而应继承 `BSDX+JINKI` 的输出、EXE patch 和资源状态。

## 3. 最高完成标准

所有 transfer pipeline 重构都以最终生成物一致为准。

对 `JINKI -> BSDX V2`，当前完成标准固定为：

- 新旧 pipeline 输出目录文件总数一致。
- 新旧 pipeline 输出目录相对路径集合一致。
- 新旧 pipeline 输出目录每个同名文件 byte-identical。
- 新旧 pipeline 生成的 `Update3.pac` 解包后内部文件总数一致。
- 新旧 pipeline 生成的 `Update3.pac` 解包后内部相对路径集合一致。
- 新旧 pipeline 生成的 `Update3.pac` 解包后内部每个同名文件 byte-identical。
- 新旧 pipeline patched exe byte-identical。

任何少文件、多文件、路径层级不同、同名文件内容不同、PAC 解包集合不同，都算失败。失败时先定位差异来源，不要通过扩大忽略列表、跳过断言或修改旧 pipeline 来掩盖。

## 4. 四层工作模型

### 4.1 源游戏转换层

负责把源游戏资产整理成 BSDX graft 主线能消费的资产对象。

当前 JINKI 基本天然成立，因为已有 parser / DTO 可以直接加载资源。未来 BHE 会真正需要 converter，把 BHE 的 MEK/WAZ/SPM/GRP 结构转换成 BSDX 兼容对象。

不负责：

- 选择 graft 槽位。
- 决定 donor。
- 写菜单 override。
- 写 EXE patch。
- 输出最终文件集合。

### 4.2 前置客制化输入

负责声明“这次 graft 要怎么接入”。

典型内容：

- 绑定到哪个目标槽位。
- donor 是谁。
- 主资源入口是什么。
- 哪些 sidecar 必须纳入。
- 哪些菜单 PNG / DAT / SPM override 是本次角色特有的。

这层不是坏东西，但必须集中表达，不能散在主线 step 里到处找。

### 4.3 通用 graft 主线

负责以后尽量复用、少重复 debug 的主链。

典型内容：

- 源资产加载。
- baseline 加载。
- 资源闭包。
- GRP append / reuse。
- source index 到 target index 映射。
- MEK / WAZ / SPM / DAT rebind。
- 根据图片尺寸计算 rect。
- 重建 `anim -> pageData -> chipData -> imageData` 链。
- 收集依赖资源。
- 输出产物沉淀。

如果一个逻辑未来 BHE 也能直接用，它优先属于这里。如果只对 AKAO 菜单或某个角色成立，它不属于这里。

### 4.4 后置客制化 / 兼容 patch / 输出沉淀

负责 graft 主线完成后的定制覆盖和兼容处理。

典型内容：

- 菜单 DAT/SPM/PNG 覆写。
- EXE patch。
- sidecar 输出。
- 输出 manifest / audit。
- PAC 打包。
- 打包后解包比较。

这里必须支持链式成果物。后续从 `BSDX+JINKI` 继续叠 BHE 时，EXE patch 遇到 target bytes 应视为已经 patch，而不是失败。

## 5. 当前代码落点

### `transfer/jinki2bsdx`

旧 JINKI pipeline。

定位：

- 当前可运行基线。
- V2 parity 的对照对象。
- 默认不要修改。

### `transfer/jinki2bsdx/v2`

当前 JINKI pipeline V2。

定位：

- 当前重构主战场。
- 已按 `graft` / `menu` / `exe` / `output` / `pack` 拆分。
- 以旧 pipeline 最终生成物为验收基准。

### `transfer/bhe2bsdx`

历史 BHE 移植实验。

定位：

- 后续接入 BHE 时的重要参考。
- 不应直接把旧实验逻辑硬塞进 JINKI V2 主线。
- 未来应先经过源游戏转换层，再进入通用 graft 主线。

## 6. 修改细则

### 6.1 不改旧基线

旧 pipeline 是验收基准。除非用户明确要求，否则不要改：

- `AkaoGraftPipeline`
- 旧 `steps` 包
- 旧 runner
- 旧输出规则

V2 不一致时，先在 V2 内找原因。

### 6.2 不把客制化散回主线

客制化默认归属：

- 菜单 donor / PNG：`v2.menu.MenuOverrideSpec`
- 菜单 slot mapping：`v2.menu.ResolveMenuSlotStep`
- EXE patch offset：`v2.exe.ExePatchProfile`
- sidecar 声明：`v2.output.SidecarResourceSpec`
- 主资源入口：`AkaoGraftRequest`

不要为了图省事把这些常量写进 `JinkiGraftPipelineV2` 或 `v2.graft`。

### 6.3 不扩大输出集合

输出集合必须和旧 pipeline 完全一致。

不要：

- 多复制 SPM 里所有历史 `imageData` PNG。
- 把 BSDX 原生资源整包带进输出。
- 把临时 manifest / diff / unpack 目录写入最终打包目录。
- 在 pack step 临时补文件。

### 6.4 不绕过 byte parity

对象相等、字段相等、日志看起来相等，都不能替代最终文件 byte-identical。

最终判断必须落到：

- 输出目录相对路径集合。
- 输出目录逐文件 bytes。
- PAC 解包后的相对路径集合。
- PAC 解包后的逐文件 bytes。
- patched exe bytes。

## 7. 开发步骤建议

处理 transfer pipeline 任务时，按这个顺序推进：

1. 先确认任务属于四层模型中的哪一层。
2. 读对应包的 `AGENT.md`、`structure-overview.md` 或 checklist。
3. 确认旧 pipeline 是否只是基线，避免误改。
4. 小步修改，优先保持顶层 step 可读。
5. 对数据承载对象补清楚字段含义，尤其是 index / source / target / baseline / output。
6. 对 helper 补清楚它维护的不变量。
7. 先跑阶段 parity。
8. 最后跑完整最终 parity。
9. parity 通过后再 `git add` 相关文件。

## 8. 测试入口

JINKI V2 当前最重要的测试门槛：

```bash
mvn -q -Dtest=TestJinki2BsdxPipelineV2FinalParity test
```

完整门槛：

```bash
mvn -q "-Dtest=TestJinki2BsdxRunner,TestJinki2BsdxMenuOverrideV2Parity,TestJinki2BsdxSidecarOutputV2Parity,TestJinki2BsdxPipelineV2FinalParity" test
```

测试职责：

- `TestJinki2BsdxRunner`
  - 旧 pipeline 基线回归。

- `TestJinki2BsdxMenuOverrideV2Parity`
  - 菜单 DAT/SPM/PNG 局部 parity。

- `TestJinki2BsdxSidecarOutputV2Parity`
  - sidecar 和 output manifest parity。

- `TestJinki2BsdxPipelineV2FinalParity`
  - 新旧 pipeline 跑到最终打包。
  - 比较输出目录。
  - 解包并比较 `Update3.pac` 内部文件集合和 bytes。
  - 比较 patched exe bytes。

## 9. 文档和注释要求

文档使用中文。类名、文件名、方法名、命令保持真实名称，不要翻译。

注释要解释业务，不要只解释语法：

- 顶层 step 注释说明这一步解决什么业务问题。
- 内部 helper 注释说明为什么存在，以及维护哪个不变量。
- 字段注释说明来源、含义、单位、索引空间。
- 对容易混淆的 source / target / baseline / output 要明确写清。
- 对会影响最终输出集合的地方，要说明为什么不会多文件或少文件。

## 10. Git 暂存边界

只有最终 parity 通过后才暂存相关文件。

可以暂存：

- `src/main/java/com/giga/nexas/transfer/jinki2bsdx/v2/**`
- `src/main/java/com/giga/nexas/transfer/skill/**`
- 对应 V2 parity 测试

不要暂存：

- `src/main/resources/out/**`
- PAC 解包目录
- 临时 diff 文件
- 大量资源分析输出
- 用户已有的无关改动
