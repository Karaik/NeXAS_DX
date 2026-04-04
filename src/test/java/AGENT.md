# Tests / Workflow Playbook

本文件面向需要跑测试或调试数据流水线的 AGENT，配合仓库根目录的 `AGENT.md` 一起阅读。

---

## 数据总览
- **真实资产**位于 `src/main/resources/game/<engine>`（BSDX / BHE / CLARIAS / Jinki 等）。测试直接基于这些二进制文件。
- 解析/生成测试会在 `src/main/resources` 下建立形如 `datBsdxJson`、`grpBsdxGenerated`、`datClariasCsvGenerated` 的目录，这些已被 `.gitignore`。测试成功时有的会自动清理（`TestDat`），有的需要手动删除。
- CSV／JSON 补丁流程同样写在测试里，请不要另起脚本，以免与现有约定冲突。

---

## BSDX 套件
| 类 | 关键测试 | 输出目录 | 备注 |
| --- | --- | --- | --- |
| `com.giga.nexas.bsdx.TestDat` | `testGenerateDatJsonFiles` → `testGenerateDatFilesByJson` → `testDatParseGenerateBinaryConsistency` | `datBsdxJson`, `datBsdxGenerated`, `datBsdxCsvGenerated` | 排序由 `@Order` 控制。Consistency 成功后会🧹清空 JSON/Generated 目录。若要跑 `testToCsv`/`testCsvPatchToJson`，请在 Consistency 之前手动调用。 |
| `TestBin` / `TestGrp` / `TestMek` / `TestSpm` / `TestWaz` | 解析 → 回写 → 二进制比对 | `binBsdxJson`, `grpBsdxGenerated`, … | 无自动清理。`.bin` 测试会自动跳过 `__GLOBAL.bin`。运行命令示例：`mvn "-Dtest=com.giga.nexas.bsdx.TestBin#testGenerateBinJsonFiles" test`。 |
| `bsdx.tmp.*` | 手动实验（编辑器草稿） | - | 这些类不会被常规流程引用，如需参考请确认其副作用。 |

---

## BHE & Jinki 套件
| 类 | 输出目录 | 用途 |
| --- | --- | --- |
| `com.giga.nexas.bhe.TestDat / TestGrp / TestMek / TestSpm / TestWaz` | `datBheJson`, `grpBheGenerated`, … | 主要用作对照或中间数据。没有 `@Order`，也不会自动清理。 |
| `com.giga.nexas.jinki.TestGrp` | `grpJinkiJson`, `grpJinkiGenerated` | 结构与 BSDX 流程一致。 |

---

## CLARIAS 套件
| 类 | 说明 |
| --- | --- |
| `com.giga.nexas.clarias.TestDat` | 逻辑与 BSDX `TestDat` 完全一致，目录为 `datClariasJson` / `datClariasGenerated` / `datClariasCsvGenerated`。 | 

**运行提示**
1. `ending.dat` 达 400+ MB，解析时会把首个 `int` 误读为 `469,762,048` 列 → 非常容易 OOM。推荐：
   - 临时移动/重命名 `src/main/resources/game/clarias/dat/ending.dat`，或
   - 提前增大堆：`set MAVEN_OPTS=-Xmx4g` 再运行 `mvn "-Dtest=com.giga.nexas.clarias.TestDat#testGenerateDatJsonFiles" test`
2. 流程结束（且无错误）会自动清空 JSON/Generated 目录，与 BSDX 行为一致。

---

## JINKI→BSDX AKAO Graft Pipeline
- **入口**：`com.giga.nexas.jinki.TestJinki2BsdxRunner#testRunAkaoGraftPipeline`
- **作用**：将 JINKI（Baldr Heart）的 AKAO 机体移植到 BSDX（Baldr Sky DX），通过 `AkaoGraftPipeline`（11 步流水线）完成资源 diff、索引重绑、菜单 patch、exe patch 和 pac 封包。
- **输出目录**：`src/main/resources/out`（含 `Update3.pac`、patched exe、所有 GRP/DAT/MEK/WAZ/SPM）
- **注意事项**
  - 流水线会读取 JINKI 和 BSDX 双侧的 grp/mek/waz/spm/dat 全套资源，内存需求较高。
  - 辅助 waz（弹幕/特效类 Tama01-05、Effect.waz 等）仅做 `Files.copy` 原样复制，其内部不递归重绑索引（已确认辅助 waz 内部无交叉引用）。
  - `AkaoGraftRequest.planExeCapacityPatch` 默认为 true，但当前验证策略下机体容量数学条件不满足，不会触发实际 exe patch 写入。
  - 测试断言包含：mekaIndex=32、SpriteGroup=139、BatVoice=30、SelectMekaMenu 行数不变、exe 未实际 patch 等硬编码值，若基线数据变更需同步更新。

---
- **入口**：`com.giga.nexas.bhe2bsdx.TransferTest#testPipeline`
- **作用**：注册 BHE/BSDX 全部 grp/mek/waz/spm，挑选 Tsukuyomi 机体执行 `TransMeka.process`，写出变更文件并执行 `PacUtil.pack`。
- **注意事项**
  - 依赖 `.grp/.mek/.waz/.spm` 的完整集合完成索引对齐，但输出目录默认仅包含本次变更的 `grp/mek/waz/spm`。
  - 仍有 TODO（详见 `README_STEP1.md` 和 `steps/TransMeka.java`），运行前请确认是否真的需要。

---

## 运行技巧
1. **单例命令**：`mvn "-Dtest=<FQN>#<method>" test` 可以精确执行单个测试，避免重复扫目录。
2. **并发**：所有重度 IO 测试都使用 `@Execution(SAME_THREAD)`，不要尝试并行运行多个套件，否则输出目录会互相覆盖。
3. **堆设置**：处理 `ending.dat` 或海量 `.bin` 时务必提高 Maven 堆（`set MAVEN_OPTS=-Xmx4g`），否则 Surefire Fork 会直接 OOM。
4. **输出目录**：成功运行后若无需保留结果，请手工删除 `*Json` / `*Generated` / `*CsvGenerated`，避免膨胀仓库占用。
5. **Windows 专用路径**：部分测试（尤其是 Transfer pipeline）写死了 `D:\A\NeXAS_DX`，如需在其它路径运行，请修改常量或创建同名符号链接。

---

## 故障排查速记
| 症状 | 排查步骤                                                                     |
| --- |--------------------------------------------------------------------------|
| `extensionName missing` 报错 | 确认 JSON 中是否带 `extensionName`，以及 `BinaryEngineAdapter#mapPayload` 是否覆盖该扩展 |
| `Unsupported file type` | 说明该扩展未注册到 `*BinService` 或 `EngineType` 未包含对应后缀                           |
| OOM / Java heap space | 确认位 `ending.dat` `opening.dat`实际上是视频，而不是大多数的dat文件，将其跳过即可                 |
| Consistency 测试没有比较任何文件 | 检查 JSON/Generated 目录是否为空（可能被上一次成功运行清空）                                   |

---

维护规则：新增测试或更改输出目录时，请同步更新本文件，让每位 AGENT 能准确了解应该怎么跑、会产出什么、有哪些坑。
