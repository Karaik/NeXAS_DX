# Tsukuyomi -> Nanoha 槽位 grp 映射分析

目标：以 BHE 月詠（Tsukuyomi）为源，**完全占用 BSDX 的 Nanoha 槽位**，避免追加导致的索引漂移，
并在保持 key 不变的前提下完成迁移与验证。

---

## 1) 数据来源与基准

- BHE grp JSON：`src/main/resources/grpBheJson/*.grp.json`
- BSDX grp JSON：`src/main/resources/grpBsdxJson/*.grp.json`
- BHE mek JSON：`src/main/resources/mekBheJson/tsukuyomi.mek.json`
- BSDX mek JSON：`src/main/resources/mekBsdxJson/Nanoha.mek.json`

说明：
- **索引均为 0-based**（列表下标即 grp 索引）。
- mek 内的 `wazFileSequence/spmFileSequence` 与 grp 索引必须一致。

---

## 2) 需要处理的 grp 列表（当前迁移流程会用到）

1. `batvoice.grp`（语音组注册表）
2. `mekagroup.grp`（机体注册表）
3. `wazagroup.grp`（技能注册表）
4. `spritegroup.grp`（战斗 SPM 注册表）
5. `segroup.grp`（音效注册表，当前流程未接入但 MaterialBlock 会引用）

---

## 3) Tsukuyomi -> Nanoha 索引映射（详细）

### 3.1 meka group（机体）

- BHE（源）
  - index: **2**
  - entry: `mekaName=Tsukuyomi`, `mekaCodeName=TSUKUYOMI`
  - mek 内索引：`wazFileSequence=11`, `spmFileSequence=13`

- BSDX（目标 Nanoha 槽位）
  - index: **4**
  - entry: `mekaName=Nanoha`, `mekaCodeName=NANOHA`
  - UI 映射：`SelectMekaMenu.dat` 以 mekaIndex 为 key

**策略**
- 在 index=4 覆盖内容，但**保留 Nanoha 的 key**（mekaName/mekaCodeName）。
- `MekBasicInfo.wazFileSequence/spmFileSequence` 回写为 Nanoha 槽位索引（见下文）。

---

### 3.2 waza group（技能）

- BHE（源）
  - index: **11**
  - entry: `wazaName=◆月詠`, `wazaCodeName=TSUKUYOMI`, `wazaDisplayName=Tsukuyomi`, `param=31`

- BSDX（目标 Nanoha 槽位）
  - index: **15**
  - entry: `wazaName=菜ノ葉`, `wazaCodeName=NANOHA`, `wazaDisplayName=Nanoha`, `param=21`
  - Nanoha.mek 中 `wazFileSequence=15`

**策略**
- 在 index=15 覆盖内容，但**保留 Nanoha 的 key**（wazaName/wazaCodeName/wazaDisplayName）。
- `param` 目前直接拷贝 BHE，后续需确认 BSDX 语义。

---

### 3.3 sprite group（战斗 spm）

- BHE（源）
  - index: **13**
  - entry: `spriteFileName=tsukuyomi.spm`, `spriteCodeName=TSUKUYOMI`, `param=0`

- BSDX（目标 Nanoha 槽位）
  - **注意：BSDX spritegroup 中不存在 spriteCodeName=NANOHA**
  - Nanoha.mek 中 `spmFileSequence=57`
  - index: **57**
  - entry: `spriteFileName=zako_021a.spm`, `spriteCodeName=ZAKO021A`, `param=0`

**策略**
- 以 Nanoha.mek 的 `spmFileSequence` 为准，使用 index=57。
- 覆盖条目时**保留 `spriteFileName/spriteCodeName`**（即保持 `zako_021a.spm` / `ZAKO021A`）。
- 输出主战斗 spm 时应命名为 `zako_021a.spm`，避免索引错位。

---

### 3.4 batvoice（语音组）

- BHE（源）
  - index: **1**
  - entry: `characterName=月詠`, `characterCodeName=TSUKUYOMI`
  - 含 `unk0` 字段（BSDX 不存在，需丢弃）

- BSDX（目标 Nanoha 槽位）
  - index: **2**
  - entry: `characterName=菜ノ葉`, `characterCodeName=NANOHA`

**策略**
- index=2 覆盖语音内容，但**保留 Nanoha 的 key**（characterName/characterCodeName）。
- 语音文件名多数无后缀，保持原样。

**未完成项**
- `MekVoiceInfo.Entry.groupId` 仍是 BHE 范围（6~62），未与 BSDX batvoice 组数对齐。

---

### 3.5 segroup（音效组）

当前迁移流程未接入 segroup，但 `MekMaterialBlock.seGroups` 会引用该索引。

**策略建议**
- 参考 spritegroup 的索引映射方式：
  1) 从 `MekMaterialBlock.seGroups` 收集 BHE 所需索引。
  2) 在 BSDX segroup 中 upsert/替换。
  3) 建立索引映射表。

---

## 4) “保持 Nanoha key” 的统一规则

为避免 key 与索引错位，当前约定：

- grp 内的 **codeName/displayName** 保持 Nanoha，不改为 Tsukuyomi。
- UI SPM 的 `animName` 也保留 Nanoha（仅替换贴图内容）。
- 输出文件名以 Nanoha 槽位为准：
  - `nanoha.mek`
  - `nanoha.waz`
  - `zako_021a.spm`（来自 Nanoha 槽位的 spritegroup 文件名）

后续稳定后，再考虑“脱离 Nanoha key”的方案。

---

## 5) 当前流程中仍需补齐的点

1. `MekVoiceInfo.groupId` 仍未映射到 BSDX batvoice 组。
2. `wazagroup.param` 语义未确认（BHE/BSDX 同名参数不一致）。
3. `MekMaterialBlock.sprite/se/voice groups` 目前置空，未恢复真实索引关系。
4. segroup 仍未纳入迁移流程与静态资源复制。

