# 代码文字乱码恢复记录（2026-10-26）

本次只处理当前跟踪的 Java / FXML 代码文字，不修改解析、迁移、索引、资源或 EXE patch 行为。开始时清单为 908 个 Java、2 个 FXML，共 910 个文件；全部能够严格按 UTF-8 解码，U+FFFD 为 0。问题是已经保存为合法 UTF-8 的误解码文本，以及早期提交中直接写成问号的注释，不是当前终端显示编码问题。

已通过 `apply_patch` 修改 34 个原本没有 WIP 的 Java 文件，共 59 行对等替换：58 行乱码或问号文字，另有 initializer 类说明中 1 行过时的 SE 清理描述随当前实现校准。没有修改 FXML，没有创建测试，没有运行 Maven、编译或游戏，没有暂存、提交或推送。

## 恢复依据

- R1：把当前文字严格按 CP936 编码，再严格按 UTF-8 解码，能够无损恢复中文。例如 `鏀堕泦 WAZ 鍐呭祵 sprite 鍥惧儚澶辫触` 可逆为 `收集 WAZ 内嵌 sprite 图像失败`；`鍋忕Щ瓒婄晫` 可逆为 `偏移越界`；`鍋忕Щ鍘熷€间笉绗﹀悎棰勬湡` 可逆为 `偏移原值不符合预期`。
- R2：尾部已经丢字或夹有损坏的 ASCII 时，结合当前字段/方法和 BSDX 同名代码的原始可读注释恢复。Clarias GRP 的问题从 `b73e213f9a` 创建时已存在；`a087f100d0` 只去掉了若干末尾问号，没有恢复原文。这里仅恢复文字，不据另一引擎推定 Clarias 协议。
- R3：问号已经不可逆，按当前代码及引入提交的行为变化重新说明，不能冒称逐字找回。`4460c1d62a` 把 initializer 从清空 unit 52/53 改为只清空 unit 53；`6d688c560e` 又把 package 注释写坏。因此当前说明应为替换 sprite、清空 Voice、保留 unit 52 的 initializer SE，不能回退到更早的“清空 SE/Voice”。

## 每文件修改清单

路径均相对仓库根目录 `D:/Code/NeXAS_DX`，行号为本次修改后的行号。

| 文件 | 行 | 恢复内容及依据 |
| --- | --- | --- |
| `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/initializer/EnsureInitializerWeaponStep.java` | 26、201–203 | 问号注释改写为只清 Voice、保留 SE；类说明同步，R3。 |
| `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/initializer/package-info.java` | 14 | 替换专属 sprite、清空 Voice、保留 SE，material trailing 复制当前机体条目，R3。 |
| `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/followup/graft/ImportStaticAssetsStep.java` | 707 | `收集 WAZ 内嵌 sprite 图像失败: `，R1。 |
| `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/tsukuyomi/graft/ImportStaticAssetsStep.java` | 740 | 同上，R1。 |
| `src/main/java/com/giga/nexas/transfer/jinki2bsdx/steps/ImportStaticAssetsStep.java` | 559 | 同上，R1。 |
| `src/main/java/com/giga/nexas/transfer/jinki2bsdx/v2/graft/ImportStaticAssetsStepV2.java` | 596 | 同上，R1。 |
| `src/main/java/com/giga/nexas/transfer/jinki2bsdx/steps/PatchExeCapacitiesStep.java` | 581、598 | `byte patch 偏移越界`、`byte patch 偏移原值不符合预期`，R1；未改 patch 常量或分支。 |
| `src/test/java/com/giga/nexas/jinki/TestJinki2BsdxRunner.java` | 803 | `缺少 SpriteGroup 源 -> 目标映射，target=`；目标部分可逆，源部分结合方法语义恢复，R1/R2；未改断言。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/Grp.java` | 9、32 | Author 恢复为 `这位同学(Karaik)`，R1；`文件名`，R2。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/GrpFileGenerator.java` | 9 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/GrpGenerator.java` | 18、26 | Author，R1；按实际registerGenerator调用恢复为`注册生成器`，R2。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/BatVoiceGrpGenerator.java` | 11 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/MapGroupGrpGenerator.java` | 11 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/ProgramMaterialGrpGenerator.java` | 11 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/SeGroupGrpGenerator.java` | 11 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/SpriteGroupGrpGenerator.java` | 11 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/TermGrpGenerator.java` | 11 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/WazaGroupGrpGenerator.java` | 11 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/BatVoiceGrp.java` | 10、28、36 | Author 与 `仅记录用`，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/MapGroupGrp.java` | 10、20、31 | Author，R1；`existFlag, 3个字符串, 1个未知int(int1), N个Item(每项4个int), 三段 数组的数组`、`每项4个int`，R2。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/ProgramMaterialGrp.java` | 10 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/SeGroupGrp.java` | 10、21、29 | Author 与 `仅记录用`，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/SpriteGroupGrp.java` | 10、21 | Author 与 `仅记录用`，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/TermGrp.java` | 10 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/WazaGroupGrp.java` | 10、21 | Author 与 `仅记录用`，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/GrpFileParser.java` | 7 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/GrpParser.java` | 13、22 | Author，R1；`注册解析器`，R2。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/BatVoiceGrpParser.java` | 9 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/MapGroupGrpParser.java` | 12、31、36、38、40、42、57、63 | Author，R1；读取存在标志、组名、组代号、资源名、未知整型1、三段、加入结果，R1/R2。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/ProgramMaterialGrpParser.java` | 12、25、28、37 | Author，R1；创建结果对象、读取三段数组的数组、读取一段“数组的数组”的结构说明，R1/R2。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/SeGroupGrpParser.java` | 9 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/SpriteGroupGrpParser.java` | 9 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/TermGrpParser.java` | 9 | Author，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/WazaGroupGrpParser.java` | 9 | Author，R1。 |

## 无逻辑变更核验

34 个文件修改前均无工作区改动，使用 HEAD 作为本子任务前基线逐文件比较。比较前只消除 Java 注释、空白，并把本次明确恢复的异常消息还原成原字面，再比较其余内容：34 个文件全部一致，差异文件数为 0。异常消息共 7 处，参数占位符、拼接参数、异常类型和 cause 均保留。

因此本次只改变注释及错误消息显示文字，没有改变条件、字段、索引、方法调用、断言、资源写出或 EXE patch 行为。`git diff --check` 通过。34 个修改文件均严格 UTF-8、无 BOM、CRLF，无裸 LF / CR。以上是静态文字核验，不代表编译、流水线或实机验证；这些由主代理串行统筹。

以下 6 项已有 WIP 在本次修改前后逐文件 SHA-256 完全相同：`AGENT.md`、`src/test/java/AGENT.md`、Clarias `README.md`、`MekaGroupGrpGenerator.java`、`MekaGroupGrp.java`、`MekaGroupGrpParser.java`。没有回滚、覆盖或暂存它们。

## 完整剩余扫描

修复后再次遍历全部 910 个当前跟踪 Java / FXML 文件：严格 UTF-8 解码失败 0、U+FFFD 0、NUL 0。扫描覆盖私用区字符、典型 UTF-8/CP936 误解码字面、CP936 严格逆变换、CP932 严格逆变换、连续问号及常见 Latin-1 误解码特征；正常日文、半角片假名没有仅因字符形态被判错。CP932 与 Latin-1 检查没有新增确证命中。

剩余 4 行确证乱码全部在禁止编辑的 3 个 Clarias WIP Java 文件中，对应 4 个私用区字符。建议主代理按文字行选择性处理并保持已有功能改动：

| 文件和行 | 当前原字面 | 建议恢复 |
| --- | --- | --- |
| `src/main/java/com/giga/nexas/dto/clarias/grp/generator/impl/MekaGroupGrpGenerator.java:11` | `* @Author 杩欎綅鍚屽(Karaik)` | `* @Author 这位同学(Karaik)`，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/MekaGroupGrp.java:10` | `* @Author 杩欎綅鍚屽(Karaik)` | `* @Author 这位同学(Karaik)`，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/groupmap/MekaGroupGrp.java:21` | `public Integer existFlag; // 浠呰褰曠敤` | `public Integer existFlag; // 仅记录用`，R1。 |
| `src/main/java/com/giga/nexas/dto/clarias/grp/parser/impl/MekaGroupGrpParser.java:9` | `* @Author 杩欎綅鍚屽(Karaik)` | `* @Author 这位同学(Karaik)`，R1。 |

另有 2 行保留的未知语义占位注释：BHE `SkillInfoFactory.java:142` 的 `// ???`、BSDX 同名文件第 129 行的 `// ????`，均位于 `CEventTerm` 的 `0xFFFFFFFF` 分支。没有证据证明它们是中文误编码，不擅自补写事件语义。

主代理补完：上述三份WIP文件中的四行已仅恢复文字。独立审核把四行在内存反向还原后，三文件SHA均与本目标初始baseline逐字节一致，证明既有功能修改保全。提交时使用仅含四行的index patch，不暂存MekaGroup结构校准的既有增量。

边界：本轮清单只覆盖跟踪 Java / FXML，不宣称全部 Markdown / JSON / 二进制资源无乱码。只读排查中还发现 `initializer-commonality.md` 与 `initializer-usage.md` 的早期问号文本；它们不在本次分配的编辑范围，没有修改。

## 2026-10-27 旧 pipeline 遗漏符号复核

本次复核当前跟踪的 924 个 Java、2 个 FXML，共 926 个文件，重点检查旧 pipeline／非现行入口中的误解码及方向符号。结合字符扫描、CP936／CP932／Latin-1 到 UTF-8 的严格逆变换候选、历史提交和代码上下文，新增确认 `transfer/jinki2bsdx/steps/PatchExeCapacitiesStep.java` 的 4 条注释符号问题；没有重做此前 34 文件的恢复，也没有把正常日文、WAZ 格式中的 `⇒` 或未知语义问号占位判作乱码。扫描未发现新的 U+FFFD、私用区字符或 NUL。

第 156 行 `103 * 4336 鈫?104 * 4336` 改为 `103 * 4336 → 104 * 4336`。该行在 `2f6e3bcc733` 首次加入时已损坏；`→` 的 UTF-8 为 `E2 86 92`，其中前两字节按 CP936 解码正好是 `鈫`，尾部问号已丢失信息。恢复方向同时依据 EXPECTED=`4336*103`、TARGET=`4336*104` 及实际 patch 标签中的 `103*4336 -> 104*4336`，不声称可以仅凭问号无损逆转。

第 161、166、171 行的 `push 103 ↙ push 104` 均改为 `push 103 → push 104`。这三行在 `5b274e4369` 首次加入时已是 `↙`，没有历史正确箭头可逐字取回；修正依据各自 EXPECTED=`0x67`、TARGET=`0x68` 和调用标签明确的 `push 103 -> push 104`，属于上下文确定的符号修正。

实施仅替换上述 4 条注释，并追加本节说明；源码其余字节与修复前快照一致，未修改任何逻辑、常量或 patch 行为。两文件均核为严格 UTF-8、无 BOM、CRLF，`git diff --check` 通过。没有为注释新增测试，没有运行 Maven、UI、CDB 或游戏，没有提交。

## 2026-11-05 热修后编码增量复核

以`target/bhe-combined-fixes-20261105/encoding-source-scan-20261105.json`为基线，SHA256 `03d4bc6d67d97fe3d318bd32b4c92de3ac614cafe14c7390f5492a99ee1ec660`；该基线HEAD为`979c31389`。当前核对HEAD为`335a128e6`，热修源码提交为`fcdddea36`。跟踪源码范围仍为986文件（974 Java、8 Python、2 PowerShell及原计划两份FXML），没有新增／删除；984文件与基线逐字节相同，仅以下两份Java变化。其余resources、generated、binary及未跟踪草稿未扩入扫描。

| 变化文件 | 当前SHA256 | 增量文字判断 |
| --- | --- | --- |
| `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouCoolGainReturnProtocolFixer.java` | `ce65f122929969f3e0973ee608bc0572ea66d9af41f16e8eae19d7b453527ea5` | 第39行新增私有冷却实体日文名，正常可读。 |
| `src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouCoolGainReturnProtocolTest.java` | `147025878ba09126d3b42e2aa02c25b6bf08c0af0a63842903e0018bbe4798c2` | 第50行同步名称、第197行原版`発光爆風（小）`供体断言，正常可读。 |

这两文件均重新严格UTF-8解码成功，无BOM、裸LF／CR、U+FFFD、私用区、C1或NUL；全部3条新增非ASCII行已按上下文人工阅读，未发现新增确证乱码。Git基线blob按工作区CRLF还原后的hash与旧清单相同；没有把Git存储换行与工作区换行差异判成文字损坏。本轮是窄增量复核，不宣称重新逐句阅读全部历史注释。

旧BHE `SkillInfoFactory.java:142`的`// ???`及BSDX同名文件第129行的`// ????`仍为未知语义占位，没有可恢复原文证据；104份既有LF文件hash均未变化，单列为换行风格，不作乱码恢复。原六份Clarias相关WIP仍为53行新增、0删除，本轮写入前后六文件hash相同；没有修改源码、格式化、运行Maven／JVM／UI或提交。

小型摘要与两文件hash增量写入[source-encoding-current.json](../../../src/test/resources/bhe-runtime-evidence-20261105/source-encoding-current.json)，SHA256 `f5d717cce8a00693c55a70c7029bfc876db7c30265acb1bc04c3f695f754db59`；未复制271KB旧清单。此结论只覆盖源文本编码，当前热修仍未取得实机成功，不据此扩展游戏验收结论。
