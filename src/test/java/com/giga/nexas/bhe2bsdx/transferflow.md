# BHE -> BSDX 迁移流程（以月读 Tsukuyomi 为基准，使用 Nanoha 槽位）

本文档按步骤完整描述当前的 BHE -> BSDX 迁移流程，对应代码位于
`src/test/java/com/giga/nexas/bhe2bsdx/TransferTest.java` 与 `steps/` 包。
重点围绕月读（Tsukuyomi）基准测试，列出实际文件、相互关系以及每一步的数据处理方式。

---

## 0. 范围与目标

该流程的目标是把 **BHE 的单个角色（月读）** 迁移到 BSDX 的数据结构中，并尽量保持
玩法语义一致。迁移的范围包括：

- 机体核心数据（`mek`）
- 技能数据（`waz`）
- 精灵/动画数据（`spm`，含 hitbox 页）
- 注册表（`grp`），用于索引与文件名映射
- UI 资源（驾驶员/选机体界面，替换已有槽位便于测试）
- 静态资源（图片/语音文件），复制到输出根目录

这不是全量游戏迁移，仅迁移指定角色及其相关注册表变更。

---

## 1. 输入与输出文件（实际路径）

### 1.1 BHE 源文件（二进制）
由 `BheBinService` 读取：

- `src/main/resources/game/bhe/grp/*.grp`
- `src/main/resources/game/bhe/mek/*.mek`
- `src/main/resources/game/bhe/waz/*.waz`
- `src/main/resources/game/bhe/spm/*.spm`

月读基准所用关键文件：

- `src/main/resources/game/bhe/mek/tsukuyomi.mek`
- `src/main/resources/game/bhe/waz/tsukuyomi.waz`
- `src/main/resources/game/bhe/spm/tsukuyomi.spm`
- `src/main/resources/game/bhe/spm/c_tsukuyomi.spm`
- `src/main/resources/game/bhe/spm/s_tsukuyomi.spm`
- `src/main/resources/game/bhe/spm/g_tsukuyomi.spm`
- `src/main/resources/game/bhe/spm/m_tsukuyomi.spm`
- `src/main/resources/game/bhe/grp/batvoice.grp`
- `src/main/resources/game/bhe/grp/mekagroup.grp`
- `src/main/resources/game/bhe/grp/wazagroup.grp`
- `src/main/resources/game/bhe/grp/spritegroup.grp`

### 1.2 BSDX 目标文件（二进制）
由 `BsdxBinService` 读取：

- `src/main/resources/game/bsdx/grp/*.grp`
- `src/main/resources/game/bsdx/mek/*.mek`
- `src/main/resources/game/bsdx/waz/*.waz`
- `src/main/resources/game/bsdx/spm/*.spm`
- `src/main/resources/game/bsdx/dat/*.dat`

UI 替换与映射用文件：

- `src/main/resources/game/bsdx/spm/mekapilot.spm`
- `src/main/resources/game/bsdx/spm/selectmekamenumeka.spm`
- `src/main/resources/game/bsdx/dat/SelectMekaMenu.dat`

### 1.3 输出文件（二进制）
由 `TransMekaOutputWriter` 写入输出根目录（不创建子目录）：

- `src/main/resources/testBhe/*.grp`
- `src/main/resources/testBhe/*.mek`
- `src/main/resources/testBhe/*.waz`
- `src/main/resources/testBhe/*.spm`

仅输出本次迁移变更的文件。

### 1.4 静态资源（外部）
由 `StaticAssetCopier` 从 `TransferTest.STATIC_ASSET_ROOT` 复制：

- `D:/BaiduNetdiskDownload/bsdx_bhe/bheAll`

复制目标为 **输出根目录**：

- `src/main/resources/testBhe/`

不创建子目录。

### 1.5 可选 JSON（手工检查用）
以下目录由其他测试生成，仅用于人工比对：

- `src/main/resources/spmBheJson/`
- `src/main/resources/spmBsdxJson/`
- `src/main/resources/mekBheJson/`
- `src/main/resources/wazBheJson/`
- `src/main/resources/datBsdxJson/`

---

## 2. 数据关系（文件间索引约束）

迁移必须保持以下索引一致性：

- `grp/mekagroup.grp` 提供机体 **索引**（`mekaCodeName`），供 UI 与其他表引用。
- `grp/wazagroup.grp` 提供技能 **索引**（`wazaCodeName`/`wazaDisplayName`）。
- `grp/spritegroup.grp` 提供战斗 `spm` **索引**（`spriteFileName`）。
- `MekBasicInfo.wazFileSequence` 必须等于 `wazagroup.grp` 中最终索引。
- `MekBasicInfo.spmFileSequence` 必须等于 `spritegroup.grp` 中最终索引。
- `MekMaterialBlock.spriteGroups` 存储被技能/动画引用的 spritegroup 索引。
- `batvoice.grp` 存储语音文件引用；`MekVoiceInfo` 通过 `groupId` 关联语音组
  （完整验证仍在进行中）。
- `Spm.imageData[].imageName` 是图片文件名，必须存在于磁盘。

---

## 3. 文字流程图（高层）

```text
[BHE 二进制资源]
  - grp: batvoice/mekagroup/wazagroup/spritegroup
  - mek/waz/spm: tsukuyomi + c_/g_/m_/s_ UI spm
            |
            v
[TransferTest#testPipeline]
  - 解析 BHE + BSDX 资源 (windows-31j)
  - 选取 Tsukuyomi 基准
            |
            v
[TransMeka.process]  (Step0..Step6 in TransMekaPipeline)
  Step1  batvoice 深拷贝 + 替换 Nanoha 槽位 (保留 key)
  Step2  grp 替换 Nanoha 槽位 (meka/waza/sprite) 并返回索引
  Step3  spritegroup 映射 (BHE -> BSDX)
  Step4  转换 mek/waz/spm (含 hitbox + WAZ 序号校验)
  Step5  回写 MekBasicInfo 的 waz/spm 索引
  Step6  UI spm 替换 (Nanoha 槽位，默认保留 key)
            |
            v
[TransMekaResult]
  - 转换后的 mek/waz/spm (含 c/s/g/m)
  - UI spm 更新结果
  - grp 索引与 sprite 映射
            |
            v
[输出 + 资源复制]
  - 写出变更的 grp/mek/waz/spm 到 testBhe
  - 复制静态资源到 testBhe 根目录
  - 打包 (PacUtil.pack)
```

---

## 4. 详细步骤（对应代码）

### Step 0: 构建请求
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMeka.java`

- `TransMeka.process(...)` 将旧参数列表封装为 `TransMekaRequest`。
- 统一收拢 BHE 源数据与 BSDX 目标注册表，方便 Pipeline 处理。
- 避免外部调用方频繁改动。

涉及文件：

- BHE：`tsukuyomi.mek`, `tsukuyomi.waz`, `tsukuyomi.spm`, `c_/g_/m_/s_` spm
- BHE grp 中 Tsukuyomi 条目：`mekagroup.grp`, `wazagroup.grp`, `spritegroup.grp`
- BHE batvoice：`batvoice.grp` 中 index=1 的组
- BSDX 注册表：`mekagroup.grp`, `wazagroup.grp`, `spritegroup.grp`, `batvoice.grp`
- BSDX WAZ 注册表：effect/tama/laser/bomb（用于校验 `CEventWazaSelect`）
- BSDX UI spm：`mekapilot.spm`, `selectmekamenumeka.spm`
- BSDX dat：`SelectMekaMenu.dat`

---

### Step 1: BatVoice 深拷贝 + 槽位替换
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/BatVoiceConverter.java`

- BHE `BatVoiceGroup` -> BSDX `BatVoiceGroup`。
- 语音条目深拷贝，避免 BHE/BSDX 类型混用。
- `existFlag` 为空时归一化为 `1`。
- 使用 Nanoha 槽位时：替换目标索引，保留 Nanoha 的 key（角色名/代号）。

原因：

- `MekVoiceInfo` 通过 `groupId` 关联语音组，必须保证索引正确。

---

### Step 2: grp 对齐（meka/waza/sprite）
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/GrpRegistryUpdater.java`

每个注册表的处理逻辑：

- **Nanoha 槽位模式（默认）**：定位目标索引并替换该槽位，保留 Nanoha 的 key。
- **追加模式**：按 `codeName/fileName` 查找；找不到则占空槽或追加。

输出：

- `mekaGroupIndex`, `wazaGroupIndex`, `spriteGroupIndex`
- 这些索引用于后续回写 `MekBasicInfo`。

索引关系：

- `MekBasicInfo.wazFileSequence` == `wazagroup` 索引
- `MekBasicInfo.spmFileSequence` == `spritegroup` 索引

注：

Nanoha 槽位模式下，`wazaGroupIndex`/`spriteGroupIndex` 来自目标 `Nanoha.mek`，
因为 BSDX spritegroup 中没有 `spriteCodeName=NANOHA` 的条目。

---

### Step 3: spritegroup 索引映射（BHE -> BSDX）
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/SpriteGroupIndexMapper.java`

目的：

- BHE `MekMaterialBlock.spriteGroups` 使用 BHE 的索引。
- BSDX 的索引可能不同，需要映射。

Nanoha 槽位模式的特殊点：

- 仍会生成 **按名称匹配** 的映射（不修改 BSDX grp）：
  - 先匹配 `spriteFileName` / `spriteCodeName`
  - 再把 BHE 的 Tsukuyomi 索引强制映射到 Nanoha 目标索引

流程：

1. 从 BHE `MekMaterialBlock` 中收集实际引用的 spritegroup 索引。
2. 构建 `{bheIndex -> bsdxIndex}` 映射：
   - 追加模式：upsert 到 BSDX `spritegroup.grp`。
   - Nanoha 槽位模式：仅按名称匹配（不修改）。
3. 强制加入 Tsukuyomi -> Nanoha 槽位映射（用于 `CEventSprite`）。

输出：

- `spriteIndexMap`，供 `MekMaterialConverter` 与 `WazConverter` 使用。

---

### Step 4: 转换核心资源（mek/waz/spm）
该步骤产出 BSDX 格式的 Tsukuyomi 资源。

#### 4.1 Mek 转换
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekConverter.java`

子步骤：

- 复制 `MekHead`（偏移表）
- 复制 `MekBlocks`（块大小）
- 复制 `MekBasicInfo`（字段一致，直接拷贝）
- 复制 `MekPairBlock`
- 复制 `MekWeaponInfoMap`（丢弃 BHE 独有字段）
- AI 事件：`MekAiConverter`
- 语音表：`MekVoiceConverter`
- 材质块：`MekMaterialConverter`

说明：

- `MekAiConverter` 深拷贝 CPU 事件，补齐 BSDX 字段。
- `MekVoiceConverter` 保持版本号，深拷贝表结构。
- `MekMaterialConverter` 当前默认清空组（见下）。

#### 4.2 MaterialBlock 转换
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekMaterialConverter.java`

当前行为：

- `CLEAR_MATERIAL_GROUPS = true`
- sprite/se/voice 组全部清空，避免演示资源读取引发错误。
- voiceGroups 的数量按 BSDX `batvoice.grp` 的组数对齐。

若后续启用映射：

- BHE 的 `spriteGroups` 为成对索引，BSDX 只保留第一项。
- `spriteIndexMap` 用于索引重映射。

#### 4.3 Waz 转换
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/WazConverter.java`

关键逻辑：

- BHE 有 83 槽位，BSDX 只有 72 槽位。
- 槽位映射表将 BHE -> BSDX，未映射的槽位会丢弃。
- slot 37 特殊处理：
  - BHE `CEventFreeParam` -> BSDX `CEventVal`
  - 仅取 buffer=0 的参数作为结果
- 默认帧信息进行归一化，避免空值。
- `CEventSprite.spmFileSequence` 根据 `spriteIndexMap` 映射，
  使技能/动作指向正确的 SPM 槽位。
- `WazSequenceSanitizer` 校验 `CEventWazaSelect`：
  - `wazSequenceNo` 超界时回退到安全序号。

预期影响：

- 部分 BHE 独有槽位会被丢弃（如 slot 23、slot 52）。

#### 4.4 Spm 转换
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/SpmConverter.java`

范围：

- 主战斗 `tsukuyomi.spm` + UI spm（`c_`/`g_`/`m_`/`s_`）。

处理逻辑：

- 复制顶层字段。
- 重建 `imageData` / `animData` / `pageData` 列表。
- 统一 `numPageData` / `numImageData` / `numAnimData` 计数。
- `chipData` 的 rect 迁移为 BSDX 格式。
- hitbox：调用 `BHE SPMHitArea.transHitbox()` 并补齐默认值：
  - `unk0` 使用 shape 映射
  - `unk1`/`unk2` 作为 z 范围
- `hitFlag` 按 hitRect 数量构建位图。

---

### Step 5: 回写 MekBasicInfo 索引
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMekaPipeline.java`

回写：

- `MekBasicInfo.wazFileSequence = wazaGroupIndex`
- `MekBasicInfo.spmFileSequence = spriteGroupIndex`

确保 mek 与 grp 索引一致。

---

### Step 6: UI SPM 替换（测试策略）
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/UiSpmReplacer.java`

目标：

为了快速测试，使用 **Nanoha 槽位覆盖**，避免新增 UI 槽位。

#### 6.1 MekaPilot.spm
- 通过 `TARGET_PILOT_NAME` 找到动画索引。
- 收集该动画使用的 page。
- 用 `m_tsukuyomi.spm` 的 page 替换。
- 用 `m_tsukuyomi` 的第一张图片替换目标 imageName。
- `keepTargetKey=false` 时才修改 animName；默认保留 Nanoha key。

#### 6.2 SelectMekaMenuMeka.spm
- 用 `TARGET_MEKA_CODE` 在 `mekagroup.grp` 中定位 Nanoha。
- 在 `SelectMekaMenu.dat` 中映射到 UI 动画索引。
- 用 `s_tsukuyomi.spm` page 替换。
- 用 `s_tsukuyomi` 的第一张图片替换 imageName。
- `keepTargetKey=false` 时才修改动画名；默认保留 Nanoha key。

---

## 5. 输出写盘（二进制）
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMekaOutputWriter.java`

流程：

- 仅写出 **变更** 的资源。
- 直接写入输出根目录（不创建子目录）。
- 使用 `BsdxBinService.generate(...)`，编码 `windows-31j`。

Nanoha 槽位模式的输出示例：

- `batvoice.grp`（替换 Nanoha 槽位）
- `mekagroup.grp`
- `wazagroup.grp`
- `spritegroup.grp`
- `nanoha.mek`（内容为 Tsukuyomi，key 保持 Nanoha）
- `nanoha.waz`（内容为 Tsukuyomi，key 保持 Nanoha）
- `zako_021a.spm`（Nanoha 的 sprite 槽位文件名）
- `mekapilot.spm`（若 UI 替换成功）
- `selectmekamenumeka.spm`（若 UI 替换成功）

在 Nanoha 槽位模式下，`c_/g_/m_/s_` 作为 **输入源** 使用，但不会被写出。

---

## 6. 静态资源复制
代码：`src/test/java/com/giga/nexas/bhe2bsdx/steps/StaticAssetCopier.java`

收集来源：

- 输出 spm 的 `imageData[].imageName`
- `batvoice.grp` 的 `voiceFileName`
- （可选）`segroup.grp` 的 `seFileName`，前提是显式传入

复制逻辑：

- 扫描 `STATIC_ASSET_ROOT` 生成索引（大小写不敏感）。
- 找到即复制到输出根目录。
- 记录缺失与重名资源。

注意：

- 资源直接复制到 `src/main/resources/testBhe/` 根目录。
- 当前仅传入 spm 与 batvoice 资源。

---

## 7. 打包步骤（可选）
代码：`com.giga.nexas.util.PacUtil`

`TransferTest#testPipeline` 末尾调用：

- `PacUtil.pack(outputPath, "4")`

若出现 `Size:0` 或包体异常，需要检查 pac 结构或输入目录布局。

---

## 8. 代码位置索引

- 入口：`src/test/java/com/giga/nexas/bhe2bsdx/TransferTest.java`
- 总控：`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMeka.java`
- 流程编排：`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMekaPipeline.java`
- Step1 BatVoice：`src/test/java/com/giga/nexas/bhe2bsdx/steps/BatVoiceConverter.java`
- Step2 grp：`src/test/java/com/giga/nexas/bhe2bsdx/steps/GrpRegistryUpdater.java`
- Step3 sprite 映射：`src/test/java/com/giga/nexas/bhe2bsdx/steps/SpriteGroupIndexMapper.java`
- Step4 mek：`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekConverter.java`
- Step4 AI：`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekAiConverter.java`
- Step4 voice：`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekVoiceConverter.java`
- Step4 material：`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekMaterialConverter.java`
- Step4 waz：`src/test/java/com/giga/nexas/bhe2bsdx/steps/WazConverter.java`
- Step4 waz 校验：`src/test/java/com/giga/nexas/bhe2bsdx/steps/WazSequenceSanitizer.java`
- Step4 spm：`src/test/java/com/giga/nexas/bhe2bsdx/steps/SpmConverter.java`
- Step6 UI：`src/test/java/com/giga/nexas/bhe2bsdx/steps/UiSpmReplacer.java`
- 输出写盘：`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMekaOutputWriter.java`
- 资源复制：`src/test/java/com/giga/nexas/bhe2bsdx/steps/StaticAssetCopier.java`

---

## 9. 已知限制（当前状态）

- `MekMaterialBlock` 的 sprite/se/voice groups 默认清空。
- BHE 的部分 WAZ 槽位因为 83 -> 72 映射被丢弃。
- `spritegroup` 若映射不到仍可能保留 BHE 原索引。
- `PacUtil.pack` 如遇 `Size:0` 仍需进一步确认原因。

---

## 10. 快速运行提示（月读测试）

在项目根目录执行：

```bash
mvn "-Dtest=com.giga.nexas.bhe2bsdx.TransferTest#testPipeline" test
```

检查输出：

- `src/main/resources/testBhe/`（grp/mek/waz/spm 与资源均在此目录）
