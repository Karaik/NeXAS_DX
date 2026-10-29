# AGENT 指南

> **唯一硬性规范**：所有文件保持 UTF-8（无 BOM）编码 + CRLF 换行。

> **分支约束**：所有修改直接在 `develop` 上完成，不创建或保留 `codex/` 分支，不为本仓库另建开发工作树。

---

## 项目鸟瞰
- **NeXAS_DX**：JavaFX GUI，用于在 NeXAS（GIGA/戯画）系列游戏的二进制资源与 JSON 之间进行互转，并支撑 BHE → BSDX 的移植实验。
- **引擎分支**
  - `BSDX`（Baldr Sky DiveX）：目前最完整，支持 `.waz/.mek/.spm/.grp/.bin/.dat` 的解析与生成。
  - `BHE`（Baldr Heart EXE）：用于对照/迁移，解析 `.waz/.mek/.spm/.grp`，生成 `.spm/.grp`。
  - `CLARIAS`（Clarias）：现阶段聚焦 `.dat`，其它格式待补齐。
- **核心流程**：目录扫描 → 树/卡片联动 → Run Selected / Run All → `BinaryEngineAdapter` 调度 `*BinService` → DTO ↔ 二进制。

---

## 关键目录对照
| 路径 | 用途 |
| --- | --- |
| `MainApplication` / `resources/fxml/MainView.fxml` | JavaFX 入口与主界面 |
| `controller/*` | 各种 GUI 控制器（文件选择、树/卡片、按钮、拖拽、设置等） |
| `controller/model/*` | `WorkspaceState`、树节点描述、类别/动作枚举 |
| `controller/support/DirectoryScanner` | 基于 `EngineType` 扩展集合扫描目录，输出 `WorkspaceCategory` |
| `dto/bsdx|bhe|clarias` | DTO 定义；新增格式从这里起手 |
| `service/*BinService` | Parser/Generator 注册中心（引擎内所有格式必须手动注册） |
| `service/engine/*Adapter` | GUI 批处理用的桥接器（BSDX/BHE/CLARIAS） |
| `io/BinaryReader|BinaryWriter` & `util/ParserUtil` | 统一的二进制 IO 与 `.dat` 列类型常量 |
| `transfer/*` & `src/test/java/com/giga/nexas/bhe2bsdx/*` | 移植实验脚手架（Tsukuyomi pipeline 等） |
| `transfer/jinki2bsdx/*` | **AKAO Graft Pipeline**：JINKI（Baldr Heart）→ BSDX（Baldr Sky DX）机体移植，11 步流水线 |
| `docs/protocol-notes/` | **引擎协议笔记**：从逆向/动态分析得到的 grp / mek / waz / dat 等文件格式的索引语义与结构约定 |
| `src/main/resources/game/<engine>` | 真实游戏资产，所有测试基于这些文件 |

---

## GUI 运行链
1. **目录与偏好**：`FilePickerController` 负责目录选择、`Preferences` 读写以及触发 `DirectoryScanner`。
2. **树 + 卡片联动**：`ModeTreeController` 与 `BranchGridController` 共享 `WorkspaceState`，双击文件等同于点击 “Run selected”。
3. **批处理调度**：`ActionButtonController` 根据 `state.engineType` 调用 `BinaryEngineFactory#create(engine)`，既支持单文件也支持 Category / Run All 批量执行，进度条与状态栏同步更新。
4. **执行适配器**：`*BinaryEngineAdapter` 里统一处理 JSON 读写、`extensionName` → DTO 分派，再委托 `*BinService` 完成实际 `parse/generate`。
5. **输出目录策略**：若用户未锁定输出目录，会跟随输入目录变化；写入前始终会 `Files.createDirectories(...)` 保证存在。

### 扩增格式/引擎的必走步骤
1. 在目标引擎的 `dto/<engine>/<format>` 下定义 DTO。
2. 实现 Parser / Generator，并在 `<Engine>BinService` 的构造器中注册。
3. `MainConst` 里为新扩展定义常量，`EngineType` 的 `parseExtensions`/`generateExtensions` 里补充该扩展，`BinaryEngineAdapter#mapPayload` 内增加 JSON → DTO 的分支。
4. 若 GUI 需要操作该格式，确保 DirectoryScanner 能识别扩展，并在测试中覆盖该流程。
5. 更新本指南与 `src/test/java/AGENT.md`，避免下一位 AGENT 误解。

---

## 测试与数据流水线（TL;DR）
> **详尽说明包含在 [`src/test/java/AGENT.md`](src/test/java/AGENT.md)。**  
> 这里列出最常用的命令与注意事项。

| 场景 | 命令 | 输出位置 / 清理 | 备注                                                                       |
| --- | --- | --- |--------------------------------------------------------------------------|
| BSDX `.dat` 三连（JSON → Bin → 比对） | 依次执行<br>① `mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testGenerateDatJsonFiles" test`<br>② `mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testGenerateDatFilesByJson" test`<br>③ `mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testDatParseGenerateBinaryConsistency" test` | `datBsdxJson` / `datBsdxGenerated`（第 ③ 步成功后自动删除） | CSV 导出/回写测试需在第 ③ 步前手动执行，否则 JSON 会被清空                                     |
| 其它 BSDX 资源（`.bin/.grp/.mek/.spm/.waz`） | `mvn "-Dtest=com.giga.nexas.bsdx.TestBin#..." test` 等 | `*BsdxJson` / `*BsdxGenerated`（不会自动清理） | `.bin` 流程会跳过 `__GLOBAL.bin`，务必按测试类注释的顺序执行                                |
| BHE 对照数据 | `mvn "-Dtest=com.giga.nexas.bhe.TestGrp" test` 等 | 输出到 `datBheJson`、`grpBheGenerated` 等 | 主要用作移植素材，没有自动清理                                                          |
| CLARIAS `.dat` | 同 BSDX 流程（命令将 `bsdx` 换成 `clarias`） | `datClariasJson` / `datClariasGenerated`（成功后自动清理） | `ending.dat` ~430 MB，会读取到异常列数。运行前请提高堆内存（`set MAVEN_OPTS=-Xmx4g`）或暂时移出该文件 |
| Jinki / 其它实验资源 | `mvn "-Dtest=com.giga.nexas.jinki.TestGrp" test` 等 | `grpJinkiJson` / `grpJinkiGenerated` | 结构与 BSDX 流程一致                                                            |
| BHE→BSDX pipeline | `mvn "-Dtest=com.giga.nexas.bhe2bsdx.TransferTest#testPipeline" test` | 输出至 `src/main/resources/testBhe` | 仍在实验阶段，会读取大量资源；默认仅写出变更的 grp/mek/waz/spm 并封包，执行前确认路径与磁盘空间                    |
| AKAO Graft pipeline | `mvn "-Dtest=com.giga.nexas.jinki.TestJinki2BsdxRunner#testRunAkaoGraftPipeline" test` | 输出至 `src/main/resources/out` | JINKI→BSDX 机体移植；11 步流水线，产出 `Update3.pac` + patched exe |

以上测试会直接在 `src/main/resources` 下生成大量 JSON / binary / CSV，全部已列入 `.gitignore`，但**提交前务必确认没有误加大文件**。

---

## 常见坑位
1. **编码**：UTF-8（无 BOM）+ CRLF 是强约束，尤其是 `resources` 下的 JSON（供测试 diff 使用）。
2. **`extensionName`**：所有生成流程都会读取 DTO 的 `extensionName`，新增字段或序列化定制时不要遗漏该属性。
3. **默认 charset**：Reader/Writer 与 GUI 状态都默认 `windows-31j`，手动覆盖时必须同步更新 `WorkspaceState.charset`。
4. **测试顺序**：`TestDat` 依赖 `@Order`；若想使用 CSV patch，需要在 Consistency 测试前运行 `testToCsv` 和 `testCsvPatchToJson`。
5. **Clarias `ending.dat`**：列计数被误读为 4.6e8 行，若不扩大堆内存会迅速 OOM。可通过临时移走该文件或加大 `MAVEN_OPTS` 规避。
6. **转写挂钩**：新增格式务必同步 `MainConst` → `EngineType` → `BinaryEngineFactory` → 对应 `BinaryEngineAdapter` 的 `mapPayload`。
7. **真实资产**：`src/main/resources/game/*` 是完整游戏资源，切勿在不必要的场景下复制/提交。

---

## 常用命令
```powershell
# GUI
mvn -q -DskipTests compile
mvn javafx:run

# 单测（示例）
mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testGenerateDatJsonFiles" test
mvn "-Dtest=com.giga.nexas.bsdx.TestDat#testDatParseGenerateBinaryConsistency" test

# 针对大文件的 JVM 堆设置
set MAVEN_OPTS=-Xmx4g
```

---

如需了解具体测试目录、生成文件及额外注意事项，请务必阅读 [`src/test/java/AGENT.md`](src/test/java/AGENT.md)。新增或修改流程后，同步更新两个 AGENT 文件，保证下一位同学能「一眼上手」。

### BHE九角色可玩改造（2026-10-27）

- 九角色构建入口为`TestBheByJinkiRunner#testRunNineCharacterRework`，只排除Sou的角色重做，保留原有Sou构建阶段及ID113，复用JINKI V2→Tsukuyomi→Followup。输入EXE使用只读`ida-reverse/BaldrSky.exe`（713cf…）；`bsdx_game`仅作运行资源目录，其2f8c…EXE带有会使预览视野异常的旧宽屏补丁。入口在首段process前校验完整基线hash，并在完成时核源未变；每轮独立输出，不覆盖旧包，receipt记录实际源EXE路径及起始hash；可用`-Dbhe.playableBuildRoot=C:/NeXAS_DX_runs/builds`指定跨盘输出根目录。
- 角色逻辑仍在现有Customizer；复制变体使用已重绑的本角色MEK/WAZ及独立Material，实际玩家武装号不得超过152。定制后再次补齐Material容量并同步WazaGroup技能数。
- 两个BHE静态输出器对九个重做角色从最终MEK按身份构造14槽初始装备，Sou保留旧默认行，继承行独立复制；普通武装、FC与initializer不能混槽。
- Motoki新投弹使用单block路由＋私有四block执行技能；TURN/INPUT参数先经SkillSuffix映射，新执行图独立建立四入口，不能只追加phase而继承单入口。多帧CEventVal的绝对恒100必须起终点均100，不能误把末值当作全程值；原技能和公共弹体不改。
- `build.json`记录EXE/PAC身份且初始`runtimeValidated=false`；封包字节一致与Java测试不代表实机通过。隔离试跑工具为`tools/bhe_runtime_session.py`，游戏效果及说明书验收独立记录。
- 连段输入可用游戏原生训练Replay验收；`tools/bhe_replay_input_fixture.py`仅从当前角色正常UI保存的模板生成新录像，保留前两块，只替换普通按钮输入块，不覆盖旧文件。先经正常载入／播放，再核不同武装START、同一目标控制、热量与收尾；格式通过不能代替玩法通过，操作证据见`replay-validation-20261029.md`。
- 两个BHE静态输出器通过`MekaPreloadBudget`补齐`BQMekaMemory.dat`至最终MekaGroup行数；优先保留继承表的schema及全部行，只在本轮目录追加新ID。100M是显式调度权重，不是精确内存；原生force/release/queue与关卡推进仍须实机验收。

### BHE多角色流水线修复（2026-10-08）

- 实施计划与逐项笔记位于`docs/project-deep-dive/pipeline-fixes/`及`docs/superpowers/plans/2026-10-07-bhe-pipeline-fixes.md`。
- Sou武装过滤和Lv3选择必须同步其Material切片与AI索引；三等级源尾表不能原样用于非KOU目标。容量判断依据实际武装消费者，不能只以MEK文件条目数断言153边界。
- 验证只运行明确指定的测试；真实二进制回环使用临时输出，原版资源及共享JSON输入保持只读。
- 静态交付按原版、继承成果和当前覆盖解析资源；`bsdxStaticAssetRoot`仅提供平铺原版媒体，不提供索引表。双Import/Pack验收最终字节，PAC真实解包一致后才报告成功。SPM声明/静态动画引用的图片未找到时仅记`unresolvedImageReferences`，不能当作必需依赖阻断流水线；不制造占位图，保留原图名引用。实际有供体的新图片正常复制，原版继承图片无需重复打包。
- 地图资源prepare阶段的同目标异内容冲突必须在整个地图／MapGroup／资源事务提交前阻断；不能只检查已去重pending与磁盘的冲突。manifest分别累加输出与资源冲突，同一冲突不能重复计数。
