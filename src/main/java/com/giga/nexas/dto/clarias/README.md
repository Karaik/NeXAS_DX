# CLARIAS Reverse Roadmap

本文档用于固定 `CLARIAS` 这条线的总逆向计划、实现顺序与验收边界。

目标不是“先把类抄出来”，而是基于现有 `ida-reverse/clarias` 反汇编与字符串证据，按依赖顺序把整条资源链补齐。

## 当前状态

- 已有：
  - `dat` 通用读写
  - `mek` 伪代码版块级实现，当前已对齐到：
    - 头部 6 offsets
    - basic info block
    - fixed 200-byte pair block
    - weapon block
    - 对真实样本可通过 parse -> json -> generate -> binary consistency
    - 为保证 1:1 回写，block3/block4/block5/block6 均保留原始块字节回写兜底
  - `grp` 初版框架，当前按 `CLARIAS` 反汇编中出现的同名 group 资源与现有 BSDX/BHE group 结构接入
- 未完成：
  - `waz`
  - `spm`
  - `bin`
  - `pac`
  - `CLARIAS` 侧 `term / info collection` 语义分析链

## 证据总览

当前仓库里能直接支撑 CLARIAS 全量逆向的核心证据主要在：

- `src/main/resources/ida-reverse/clarias/export-for-ai/strings.txt`
- `src/main/resources/ida-reverse/clarias/export-for-ai/decompile/*.c`

其中已明确出现的关键资源类型：

- `mek`
  - 读：`sub_829FF0`
  - 写：`sub_829A10`
  - 尾块/材质相关：`sub_82C830`
- `waz`
  - 路径与扩展拼接：`sub_8D3030`
- `grp`
  - `Term.grp`
  - `MapGroup.grp`
  - `WazaGroup.grp`
  - `SpriteGroup.grp`
  - `MekaGroup.grp`
  - `SeGroup.grp`
  - `ProgramMaterial.grp`
  - `BatVoice.grp`
- `bin`
  - `ConvertClarias.bin`
  - `__GLOBAL.bin`
  - 多个场景 `.bin`
- `spm`
  - 字符串表中大量 UI / 战斗 / kingdom 资源
- `pac`
  - `Config.pac`

这说明 `CLARIAS` 绝不是“只有 dat 的简化引擎”；现阶段只是样本与实现都没跟上。

## 实现原则

1. 以 `CLARIAS` 自身反编译/伪代码为准。
2. `BSDX/BHE` 只能作为分层参考，不能直接假定二进制字段完全同构。
3. 没有足够证据时：
   - 优先保留原始块/原始字节
   - 再逐步细化字段
   - 不为了“看起来完整”硬猜语义
4. 每补一个格式，都要同步补：
   - DTO
   - parser/generator
   - `ClariasBinService` 注册
   - adapter `mapPayload`
   - `EngineType`
   - 对应 `Test*`

## 逆向类字段可见范围

`CLARIAS` 侧 reverse DTO 统一遵循以下规则：

- 直接从二进制中读出的字段：`private`
- 记录信息 / 调试信息 / 推导信息 / 非二进制原生字段：`public`

当前常见的 `public` 字段类型应限制在这类用途：

- `offset`
- `length`
- 其它仅用于记录解析位置、分段视图、推导状态的字段

同时补一条命名规则：

- 未经 `CLARIAS` 自身反编译、字符串表、虚函数或 xref 证据确认的槽位，不要直接继承 `BHE/BSDX` 的语义名字。
- 对未知槽位使用中性命名，例如：
  - `stringFieldN`
  - `intFieldN`
  - `shortFieldN`
  - `byteFieldN`

## 推荐顺序

### Phase 0: 样本与索引基建

- [ ] 建立 `game/clarias/{grp,waz,spm,mek,bin}` 样本目录
- [ ] 确认每类格式至少有一组可 round-trip 的最小样本
- [ ] 为 CLARIAS 单独维护“函数 -> 格式块”的索引笔记

原因：

- 现在 `ida-reverse` 里证据很多，但资源样本几乎只有 `dat`
- 没有真实样本，只能做“按伪代码落结构”，不能做二进制一致性校验

### Phase 1: `grp`

- [x] 新增 `dto/clarias/grp/*`
- [x] 实现 `GrpParser` / `GrpGenerator`
- [x] 覆盖以下 group 类型骨架：
  - [x] `Term.grp`
  - [x] `WazaGroup.grp`
  - [x] `SpriteGroup.grp`
  - [x] `MekaGroup.grp`
  - [x] `SeGroup.grp`
  - [x] `MapGroup.grp`
  - [x] `ProgramMaterial.grp`
  - [x] `BatVoice.grp`
- [x] 新增 `com.giga.nexas.clarias.TestGrp`
- [ ] 用真实 `clarias/grp` 样本校准并完成 round-trip

原因：

- `Term.grp` 是后续 `waz/mek/bin` 语义分析的地基
- 其他 group 也是资源索引空间本身，后续 `waz/mek/spm` 都会依赖

### Phase 2: `waz`

- [ ] 新增 `dto/clarias/waz/*`
- [ ] 实现 `WazParser` / `WazGenerator`
- [ ] 校准 `CEvent*` / `SkillInfo*` 与 `CLARIAS` 自身差异
- [ ] 新增 `com.giga.nexas.clarias.TestWaz`

证据：

- `sub_8D3030` 明确处理 `.waz`
- 反编译里能看到 `CEventWazaSelect` 等运行时对象

原因：

- `waz` 是技能/行为资源核心
- 没有 `waz`，后续 `mek` 中的武器与行为引用无法完全对齐

### Phase 3: `mek` 校准

- [x] 建立 `dto/clarias/mek/*` 基础骨架
- [x] 头部 6 offsets 对齐
- [x] `basic` block 按伪代码字段顺序落实现
- [x] 固定 200-byte pair block 落实现
- [x] `weapon` block 按现有写出伪代码落实现
- [x] 真实样本 round-trip binary consistency 通过
- [ ] 继续校准现有 `dto/clarias/mek/*`
- [ ] 用真实样本逐块验证：
  - [ ] block1
  - [ ] weapon block 语义字段命名
  - [ ] block4 真正结构
  - [ ] block5 真正结构
  - [ ] block6 material snapshot 完整结构
- [ ] 对齐 `grp/waz` 引用关系
- [x] `com.giga.nexas.clarias.TestMek` 已升级为真实 round-trip

证据：

- `sub_829FF0` / `sub_829A10` / `sub_82C830`

说明：

- 这一步不是重开 `mek`
- 现在已有实现是“按伪代码落的第一版骨架”，后续在这一 phase 内持续修正

### Phase 4: `spm`

- [ ] 新增 `dto/clarias/spm/*`
- [ ] 实现 `SpmParser` / `SpmGenerator`
- [ ] 新增 `com.giga.nexas.clarias.TestSpm`

证据：

- `strings.txt` 中大量 `.spm`
- 覆盖 UI、战斗、kingdom、菜单、演出等大量资源

原因：

- `spm` 覆盖面极广，是 CLARIAS 资源体系的重要主体
- 很多 `dat/mek/grp` 的索引都会落到 `spm`

### Phase 5: `bin`

- [ ] 新增 `dto/clarias/bin/*`
- [ ] 实现 `BinParser` / `BinGenerator`
- [ ] 处理 `__GLOBAL.bin`
- [ ] 处理 `ConvertClarias.bin`
- [ ] 新增 `com.giga.nexas.clarias.TestBin`

证据：

- `strings.txt` 中明确出现 `ConvertClarias.bin`、`__GLOBAL.bin`、多个场景 `.bin`

原因：

- `bin` 是脚本/流程资源主线
- 没有 `bin`，CLARIAS 只能算“素材资源逆向”，还称不上“整条线逆完”

### Phase 6: `pac`

- [ ] 确认 CLARIAS 的 `pac` 使用方式
- [ ] 校准与现有 `PacUtil` 的兼容性
- [ ] 至少补 `Config.pac` 的打解包验证

证据：

- `strings.txt` 和反编译里都出现 `Config.pac`

### Phase 7: `term / info collection` 语义层

- [ ] 新增 `ClariasInfoCollectionAnalyzer`
- [ ] 新增 `ClariasInfoCollectionSemantic`
- [ ] 建立 `clariasInfoCollectionAnalysis` 侧产物目录
- [ ] 参照 `BHE/BSDX` 的模式，为 `waz/mek` 做路径与语义统计

原因：

- `Term.grp` 已经明确存在
- 真正要做移植、重编译、语义比对，这层迟早要补

## 每阶段交付要求

每完成一个 phase，至少满足：

- [ ] `service` 注册完成
- [ ] `adapter` 可 parse/generate
- [ ] `EngineType` 扩展名可见
- [ ] 有独立 `Test*`
- [ ] 有最小可追踪文档
- [ ] 若能获得真实样本，则补 round-trip binary consistency

## 立即执行顺序

从现在开始，按下面顺序推进，不再跳来跳去：

1. `grp`
2. `waz`
3. `mek` 实样校准
4. `spm`
5. `bin`
6. `pac`
7. `term / info collection` 语义层

## 备注

如果后续反编译证据证明某一格式与 `BSDX/BHE` 高度同构，可以复用其分层设计；
但字段顺序、长度计算、尾块结构必须以 `CLARIAS` 自身证据重新确认，不能直接搬。
