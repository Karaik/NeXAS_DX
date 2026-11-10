# DTO Package Map (AGENT)

该目录包含所有引擎相关的数据模型，请在新增格式或扩展现有模型时遵循以下准则。

## 总体结构
```
dto/
 ├─ bsdx/      # Baldr Sky DiveX 专用模型
 │   ├─ bin/
 │   ├─ dat/
 │   ├─ grp/
 │   ├─ mek/
 │   ├─ spm/
 │   └─ waz/
 ├─ bhe/       # Baldr Heart EXE 对照模型（解析用）
 │   ├─ grp/
 │   ├─ mek/
 │   ├─ spm/
 │   └─ waz/
 ├─ clarias/   # Clarias 目前仅聚焦 dat
 └─ ...        # 预留其它移植目标（如 `jinki`）
```

`CLARIAS` 的全量逆向与实现顺序单独记录在 `src/main/java/com/giga/nexas/dto/clarias/README.md`，后续请按该路线推进，不要只凭 BHE/BSDX 类比扩写。

所有 DTO 均继承对应引擎的 `*` 基类（如 `Bsdx`, `Bhe`, `Clarias`），其最重要的字段是 `extensionName`：**生成二进制时会用它来查找对应的 `BsdxGenerator/BheGenerator/ClariasGenerator`，请务必在 JSON 中填充正确的扩展名**。

## 新增/修改模型流程
1. **确定引擎**：BSDX、BHE、CLARIAS 等拥有各自的包层级，勿交叉引用。
2. **建模**：保持字段与二进制结构一一对应，必要时拆成内部静态类或 `record`，以便在 `BinaryReader/Writer` 中按顺序读写。
3. **序列化兼容**：所有 DTO 都通过 Jackson 映射。新增字段默认可序列化/反序列化，如需忽略未知字段请在测试里配置 `mapper.configure(FAIL_ON_UNKNOWN_PROPERTIES, false)`。
4. **注册 Parser/Generator**：完成 DTO 后，要在对应的 `BsdxBinService/BheBinService/ClariasBinService` 注册，它们由 GUI 和测试共同调用。
5. **更新文档**：若 DTO 会被外部 JSON 使用（例如 CSV patch 流程），请在根 `AGENT.md` 的「扩增格式/引擎」小节写明需求，并在测试指南中补充验证步骤。

## 命名/编码约定
- 使用标准 Java 命名（驼峰），保持字段与原始含义一致。对不清楚的字段可以以 `unk` 作为前缀，并在注释中记录猜测。
- Javadoc / 注释可使用简洁中文；任何 GUI 显示的字符串保持英文。
- 与项目其它部分一致：源文件 UTF-8（无 BOM）+ CRLF。

## 逆向类字段约定
- **直接从二进制读出的字段**默认使用 `private`。
- **记录信息 / 调试信息 / 推导信息 / 非二进制原生字段**使用 `public`。
  典型例子：`offset`、`length`、`builtinEmotionCount`、`regularCount`、`regularEntries`、`trailingEntries`。
- 若某个字段的语义尚未被当前引擎自身证据确认，不要直接沿用其它引擎的语义名。
  对未知槽位优先使用中性命名，例如 `stringFieldN`、`intFieldN`、`shortFieldN`、`byteFieldN`。
- `fileName` / `extensionName` 这类框架分发或文件上下文字段，沿用仓库现有风格即可，不强制按上面的 public/private 规则切换。

如需了解解析/生成的上下游逻辑，请参考 `service` 与 `service/engine` 中的桥接器；若 DTO 需要额外文档，请在对应包内再建 `README` 或 `AGENT` 文件。***
