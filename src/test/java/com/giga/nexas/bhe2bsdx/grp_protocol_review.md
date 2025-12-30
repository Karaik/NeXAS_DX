# BSDX grp 协议对照与移植不足清单

本文基于现有 BSDX/BHE grp JSON（`src/main/resources/grpBsdxJson`、`src/main/resources/grpBheJson`）
与当前移植流程的实现进行对照，整理 **协议需要满足的位置**，并逐项列出当前移植中的不足/风险。

---

## 1) BSDX 现有 grp 数据概览（对照 BHE）

以下统计来自 JSON 快照（只统计 `existFlag != 0` 的有效条目）：

| grp 名称 | BHE 总数/有效/空槽 | BSDX 总数/有效/空槽 | 关键结论 |
| --- | --- | --- | --- |
| mekagroup | 164 / 95 / 69 | 103 / 103 / 0 | BSDX 无空槽，新增只能追加 |
| wazagroup | 171 / 103 / 68 | 110 / 110 / 0 | BSDX 无空槽，新增只能追加 |
| spritegroup | 208 / 114 / 94 | 138 / 134 / 4 | BSDX 仅 4 个空槽，超出会追加 |
| batvoice | 43 / 18 / 25 | 30 / 30 / 0 | BSDX 无空槽，新增只能追加 |
| segroup | 55 / 54 / 1 | 38 / 37 / 1 | BSDX 只有 1 个空槽 |

补充观察（BSDX grp 现状）：

- `batvoice` 的 `voiceTypeList` 数量为 13，与 BHE 一致。
- `spritegroup.spriteFileName` 均带 `.spm` 后缀；BHE 与 BSDX 一致。
- `batvoice.voiceFileName`、`segroup.seFileName` **普遍无后缀名**。
- `wazagroup.param` 在相同 codeName 下 BHE/BSDX 不一致，例如：
  - `EFFECT`：BHE=291，BSDX=146
  - `NANOHA`：BSDX=21（BHE 的 tsukuyomi 为 31）

---

## 2) grp 协议需要满足的位置（约束点）

### 2.1 全部 grp 的通用规则

1) **索引是主键**  
   grp 列表的下标就是外部资源使用的索引（mek/waz/spm/dat 等）。

2) **existFlag 控制二进制结构**  
   `existFlag == 0` 时，该条目的后续字段在二进制中不会出现。  
   因此，**空槽只能通过 existFlag=0 的条目来保留**，不可随意删减列表长度。

3) **追加会改变列表长度与索引上限**  
   BSDX 现有 grp 里大多数已满（无空槽），追加会改变表长，要求同步更新所有引用索引的资源。

### 2.2 各 grp 的关键约束

#### mekagroup
- `mekaCodeName` 是逻辑主键；索引会被 UI/Dat 使用。
- `SelectMekaMenu.dat` 使用 mekaIndex → UI animIndex 的映射。
- 因此：**新增 meka 需要同时更新 SelectMekaMenu.dat**，否则 UI 不会出现或索引错位。

#### wazagroup
- `MekBasicInfo.wazFileSequence` 必须指向该 grp 的索引。
- `param` 可能与技能数量相关，但 BHE/BSDX 对同名条目数值不同，说明语义可能引擎差异。
- 因此：**param 不能盲拷，需确认规则或从 waz 统计**。

#### spritegroup
- `MekBasicInfo.spmFileSequence` 与该 grp 索引必须一致。
- `MekMaterialBlock.spriteGroups` 也引用该索引。
- `spriteFileName` 必须是 `.spm` 文件名，且与实际 spm 文件一致。

#### batvoice
- `batvoice` 的 list 索引是语音组索引（在其他表中被引用）。
- BHE 结构含 `unk0`，BSDX 无该字段，必须丢弃。
- `voiceFileName` 通常 **没有后缀**，游戏侧会自动拼接。
- 因此：**引用该组的索引必须对齐**，否则游戏读不到语音或崩溃。

#### segroup
- `MekMaterialBlock.seGroups` 与 segroup 索引相关。
- `seFileName` 通常无后缀，游戏侧自动拼接。

---

## 3) 现有移植流程对照（逐项核对）

### Step1: batvoice 深拷贝（Nanoha 槽位替换）
相关代码：
`src/test/java/com/giga/nexas/bhe2bsdx/steps/BatVoiceConverter.java`  
`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMekaPipeline.java`

当前行为：
- 默认替换 Nanoha 槽位（index=2），保留 Nanoha 的 key（characterName/characterCodeName）。
- 若未提供目标槽位，则回退为追加。
- `existFlag` 保留；`unk0` 自动丢弃（BSDX 结构不含该字段）。

协议匹配情况：
- ✅ `unk0` 丢弃正确（BSDX 无该字段）。
- ✅ Nanoha 槽位替换避免了追加带来的索引漂移。
- ❗`MekVoiceInfo.groupId` 未重映射（BHE 的 groupId 范围在 6~62）。
- ❗`voiceGroups` 旧逻辑按 BHE 组数生成，已改为按 BSDX 组数对齐，但 **groupId 本身未处理**。

### Step2: grp 对齐（Nanoha 槽位替换 / upsert）
相关代码：
`src/test/java/com/giga/nexas/bhe2bsdx/steps/GrpRegistryUpdater.java`

当前行为：
- 默认替换 Nanoha 槽位（index=4/15/57），保持 Nanoha key。
- 未提供目标槽位时，才走 codeName/fileName 的 upsert 追加逻辑。

协议匹配情况：
- ✅ Nanoha 槽位替换避免了追加导致的索引漂移。
- ✅ `SelectMekaMenu.dat` 无需新增映射（沿用 Nanoha 索引）。
- ❗wazagroup `param` 直接拷贝 BHE，未验证 BSDX 规则（同名条目参数明显不同）。

### Step3: spritegroup 映射
相关代码：
`src/test/java/com/giga/nexas/bhe2bsdx/steps/SpriteGroupIndexMapper.java`

当前行为：
- Nanoha 槽位模式下跳过映射构建（spritegroup 直接使用 Nanoha 现有索引）。
- 追加模式下仍从 BHE mek.materialBlock 收集索引 → 补入 BSDX spritegroup → 建立映射表。

协议匹配情况：
- ✅ 设计方向正确（索引是主键）。
- ❗MaterialBlock 的 spriteGroups 目前被清空，映射表未发挥作用。

### Step4: mek/waz/spm 转换
相关代码：
`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekConverter.java`  
`src/test/java/com/giga/nexas/bhe2bsdx/steps/MekMaterialConverter.java`  
`src/test/java/com/giga/nexas/bhe2bsdx/steps/WazConverter.java`

当前行为：
- mek: 结构拷贝 + AI/Voice 深拷贝。
- material: 默认清空 sprite/se/voice groups（仅保留组数量）。
- waz: BHE 83 槽 → BSDX 72 槽映射，部分槽位丢弃。

协议匹配情况：
- ✅ Spm/Sprite 主体逻辑匹配。
- ❗segroup/voicegroup 未映射，相关索引语义丢失。
- ❗`MekVoiceInfo.groupId` 直接保留 BHE 值（范围 > BSDX batvoice 组数）。

### Step5: mek 索引回写
相关代码：
`src/test/java/com/giga/nexas/bhe2bsdx/steps/TransMekaPipeline.java`

当前行为：
- 回写 `MekBasicInfo.wazFileSequence/spmFileSequence`。

协议匹配情况：
- ✅ 与当前 grp 索引一致。
- ❗未处理 `SelectMekaMenu.dat`、其他 dat/csv 的索引一致性。

---

## 4) 已识别不足与风险（优先级从高到低）

1) **batvoice 组索引不对齐**  
   BHE `groupId` 范围 6~62，但 BSDX batvoice 仅 30 组。  
   当前没有 `groupId` 重映射逻辑，极易导致语音组越界或错误指向。

2) **wazagroup.param 直接拷贝风险极高**  
   同名条目在 BHE/BSDX 的 param 数值不同（如 EFFECT）。  
   param 语义可能与引擎有关，需按 BSDX 规则计算或映射。

3) **meka/waza 索引追加未同步 UI/dat**  
   追加模式下 BSDX 无空槽，新增条目会改变索引范围。  
   未更新 `SelectMekaMenu.dat` / 其他表时，UI 与逻辑索引会错位。  
   （Nanoha 槽位替换可暂时规避这一问题）

4) **MaterialBlock 的 se/voice 组语义丢失**  
   目前 seGroups/voiceGroups 清空，仅保留数量。  
   技能演示、语音触发与效果绑定仍不完整。

5) **spritegroup 索引映射未实际生效**  
   由于 material spriteGroups 被清空，映射表实际未落地。

6) **batvoice 槽位替换后仍需校准 groupId**  
   当前已改为 Nanoha 槽位替换，避免追加带来的索引漂移；  
   但 `MekVoiceInfo.groupId` 仍未映射，语音表仍可能错位。

---

## 5) 建议的下一步对照核查（按协议优先级）

1) 核对 `MekVoiceInfo.groupId` 与 `batvoice.grp` 的真实映射关系。  
2) 明确 `wazagroup.param` 的 BSDX 语义（是否 = skillCount）。  
3) 决定是否采用“覆盖 Nanoha”策略同步处理 batvoice。  
4) 补齐 segroup/voicegroup 的索引映射与静态资源复制。  
5) 对 `SelectMekaMenu.dat` 建立追加逻辑（不再依赖覆盖）。  
