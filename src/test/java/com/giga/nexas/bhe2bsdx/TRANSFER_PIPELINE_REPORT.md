# BHE → BSDX 移植流程详细报告

## 目录

1. [概述](#概述)
2. [主流程图](#主流程图)
3. [Step 1-2: BatVoice 与 GRP 注册表](#step-1-2-batvoice-与-grp-注册表)
4. [Step 3: SpriteIndexMap 构建](#step-3-spriteindexmap-构建)
5. [Step 4: 核心资源转换](#step-4-核心资源转换)
6. [Step 5-6: 索引回写与 UI 替换](#step-5-6-索引回写与-ui-替换)
7. [数据结构差异对照表](#数据结构差异对照表)
8. [已知问题与修复记录](#已知问题与修复记录)

---

## 概述

本模块实现将 **Baldr Heart EXE (BHE)** 的机体资源移植到 **Baldr Sky DiveX (BSDX)** 的功能。

### 涉及的文件类型

| 类型 | BHE 文件 | BSDX 文件 | 说明 |
|------|----------|-----------|------|
| 机体数据 | `tsukuyomi.mek` | `nanoha.mek` | 机体属性、武装、AI、语音 |
| 技能数据 | `tsukuyomi.waz` | `nanoha.waz` | 技能帧数据、事件 |
| 精灵数据 | `c_tsukuyomi.spm` | `c_nanoha.spm` | 动画、hitbox |
| 注册表 | `mekagroup.grp` | `MekaGroup.grp` | 机体索引注册 |
| 语音组 | `batvoice.grp` | `BatVoice.grp` | 战斗语音 |

---

## 主流程图

```mermaid
flowchart TB
    subgraph Input["输入资源 (BHE)"]
        BHE_MEK[tsukuyomi.mek]
        BHE_WAZ[tsukuyomi.waz]
        BHE_SPM[c_tsukuyomi.spm]
        BHE_GRP[mekagroup.grp<br/>wazagroup.grp<br/>spritegroup.grp]
        BHE_VOICE[batvoice.grp]
    end

    subgraph Target["目标资源 (BSDX)"]
        BSDX_MEK[nanoha.mek]
        BSDX_WAZ[nanoha.waz]
        BSDX_SPM[c_nanoha.spm]
        BSDX_GRP[MekaGroup.grp<br/>WazaGroup.grp<br/>SpriteGroup.grp]
        BSDX_VOICE[BatVoice.grp]
    end

    subgraph Pipeline["TransMekaPipeline 6步流程"]
        STEP1[Step1: BatVoice 深拷贝]
        STEP2[Step2: GRP 注册表替换]
        STEP3[Step3: SpriteIndexMap 构建]
        STEP4[Step4: Mek/Waz/Spm 转换]
        STEP5[Step5: 索引回写]
        STEP6[Step6: UI SPM 替换]
    end

    subgraph Output["输出资源"]
        OUT_MEK[转换后 mek]
        OUT_WAZ[转换后 waz]
        OUT_SPM[转换后 spm]
        OUT_GRP[更新后 grp]
        OUT_PAC[Update3.pac]
    end

    Input --> STEP1
    STEP1 --> STEP2
    STEP2 --> STEP3
    STEP3 --> STEP4
    STEP4 --> STEP5
    STEP5 --> STEP6
    STEP6 --> Output

    Target -.->|目标槽位| STEP1
    Target -.->|目标槽位| STEP2
```

---

## Step 1-2: BatVoice 与 GRP 注册表

### Step 1: BatVoice 深拷贝

```mermaid
flowchart LR
    subgraph BHE["BHE BatVoice"]
        BHE_GROUP["BatVoiceGroup<br/>characterCodeName: TSUKUYOMI"]
        BHE_ENTRIES["voiceEntries[]<br/>语音条目列表"]
    end

    subgraph Converter["BatVoiceConverter"]
        FIND["findBatVoiceGroupIndex()<br/>查找目标槽位 NANOHA"]
        COPY["深拷贝 voiceEntries"]
        MERGE["mergeBatVoiceGroup()<br/>保留目标 key 或覆盖"]
    end

    subgraph BSDX["BSDX BatVoice"]
        BSDX_GROUP["BatVoiceGroup[4]<br/>NANOHA 槽位"]
        BSDX_ENTRIES["替换后的 voiceEntries"]
    end

    BHE_GROUP --> FIND
    BHE_ENTRIES --> COPY
    FIND --> MERGE
    COPY --> MERGE
    MERGE --> BSDX_GROUP
    MERGE --> BSDX_ENTRIES
```

**代码位置**: `BatVoiceConverter.java`

**关键逻辑**:
```java
// 查找目标槽位
int batVoiceIndex = findBatVoiceGroupIndex(bsdxBatVoice, "NANOHA");
// 深拷贝并替换
BatVoiceGroup converted = batVoiceConverter.convert(bheBatVoiceGroup);
bsdxBatVoice.getVoiceList().set(batVoiceIndex, merged);
```

### Step 2: GRP 注册表替换

```mermaid
flowchart TB
    subgraph GrpTypes["三种 GRP 注册表"]
        MEKA["MekaGroup.grp<br/>机体注册表"]
        WAZA["WazaGroup.grp<br/>技能注册表"]
        SPRITE["SpriteGroup.grp<br/>精灵注册表"]
    end

    subgraph Updater["GrpRegistryUpdater"]
        FIND_MEKA["findMekaGroupIndexByCode('NANOHA')"]
        FIND_WAZA["findWazaGroupIndexByCode('NANOHA')"]
        FIND_SPRITE["findSpriteGroupIndexByCode('NANOHA')"]

        REPLACE_MEKA["replaceMekaGroupAtIndex()"]
        REPLACE_WAZA["replaceWazaGroupAtIndex()"]
        REPLACE_SPRITE["replaceSpriteGroupAtIndex()"]
    end

    subgraph Result["输出索引"]
        IDX_MEKA["mekaGroupIndex"]
        IDX_WAZA["wazaGroupIndex"]
        IDX_SPRITE["spriteGroupIndex"]
    end

    MEKA --> FIND_MEKA --> REPLACE_MEKA --> IDX_MEKA
    WAZA --> FIND_WAZA --> REPLACE_WAZA --> IDX_WAZA
    SPRITE --> FIND_SPRITE --> REPLACE_SPRITE --> IDX_SPRITE
```

**代码位置**: `GrpRegistryUpdater.java`

**关键逻辑**:
```java
// 按 codeName 查找索引
int mekaIndex = findMekaGroupIndexByCode(bsdxMekaGroup, "NANOHA");
// 替换注册表条目
replaceMekaGroupAtIndex(bsdxMekaGroup, mekaIndex, bheMekaGroup, keepTargetKey);
```

---

## Step 3: SpriteIndexMap 构建

```mermaid
flowchart TB
    subgraph Collect["收集所需索引"]
        MEK_MATERIAL["MekMaterialBlock<br/>spriteGroups 引用"]
        COLLECT["collectRequiredIndicesFromMek()"]
        REQUIRED["requiredSpriteIndices<br/>Set&lt;Integer&gt;"]
    end

    subgraph Build["构建映射表"]
        BHE_SPRITE["BHE SpriteGroup.grp"]
        BSDX_SPRITE["BSDX SpriteGroup.grp"]
        BUILD_MAP["buildMapByName()"]
        MAP["spriteIndexMap<br/>Map&lt;BHE_idx, BSDX_idx&gt;"]
    end

    subgraph Usage["使用场景"]
        WAZ_SPRITE["WazConverter<br/>CEventSprite.spmFileSequence"]
        MEK_SPRITE["MekMaterialConverter<br/>spriteGroups 索引"]
    end

    MEK_MATERIAL --> COLLECT --> REQUIRED
    REQUIRED --> BUILD_MAP
    BHE_SPRITE --> BUILD_MAP
    BSDX_SPRITE --> BUILD_MAP
    BUILD_MAP --> MAP
    MAP --> WAZ_SPRITE
    MAP --> MEK_SPRITE
```

**代码位置**: `SpriteGroupIndexMapper.java`

**关键逻辑**:
```java
// 从 mek 的 materialBlock 收集需要的 sprite 索引
Set<Integer> required = collectRequiredIndicesFromMek(bheMek);
// 按名称匹配构建 BHE→BSDX 索引映射
Map<Integer, Integer> spriteIndexMap = buildMapByName(bheSpriteGroup, bsdxSpriteGroup, required);
```

---

## Step 4: 核心资源转换

### 4.1 MekConverter 总览

```mermaid
flowchart TB
    subgraph Input["BHE Mek"]
        BHE_HEAD["MekHead<br/>偏移表"]
        BHE_BLOCKS["MekBlocks<br/>块大小"]
        BHE_BASIC["MekBasicInfo<br/>基础属性"]
        BHE_PAIR["MekPairBlock<br/>14*8 未知块"]
        BHE_WEAPON["MekWeaponInfoMap<br/>武装表"]
        BHE_AI["MekAiInfoList<br/>AI 行为"]
        BHE_VOICE["MekVoiceInfo<br/>语音表"]
        BHE_MATERIAL["MekMaterialBlock<br/>演出资源"]
    end

    subgraph Converters["子转换器"]
        AI_CONV["MekAiConverter<br/>int26 补零"]
        VOICE_CONV["MekVoiceConverter<br/>深拷贝"]
        MATERIAL_CONV["MekMaterialConverter<br/>spriteGroups 映射"]
    end

    subgraph Output["BSDX Mek"]
        BSDX_HEAD["MekHead"]
        BSDX_BLOCKS["MekBlocks"]
        BSDX_BASIC["MekBasicInfo"]
        BSDX_PAIR["MekPairBlock"]
        BSDX_WEAPON["MekWeaponInfoMap<br/>丢弃 bheInt1-7, feiFlag"]
        BSDX_AI["MekAiInfoList"]
        BSDX_VOICE["MekVoiceInfo"]
        BSDX_MATERIAL["MekMaterialBlock"]
    end

    BHE_HEAD -->|直接拷贝| BSDX_HEAD
    BHE_BLOCKS -->|直接拷贝| BSDX_BLOCKS
    BHE_BASIC -->|BeanCopy| BSDX_BASIC
    BHE_PAIR -->|逐项拷贝| BSDX_PAIR
    BHE_WEAPON -->|字段过滤| BSDX_WEAPON
    BHE_AI --> AI_CONV --> BSDX_AI
    BHE_VOICE --> VOICE_CONV --> BSDX_VOICE
    BHE_MATERIAL --> MATERIAL_CONV --> BSDX_MATERIAL
```

**代码位置**: `MekConverter.java`, `MekAiConverter.java`, `MekVoiceConverter.java`, `MekMaterialConverter.java`

### 4.2 WazConverter 详细流程

```mermaid
flowchart TB
    subgraph Input["BHE Waz (83 槽位)"]
        BHE_SKILL["Skill"]
        BHE_PHASE["SkillPhase"]
        BHE_UNIT["SkillUnit<br/>unitQuantity = BHE槽位号"]
        BHE_INFO["SkillInfoObject<br/>事件数据"]
    end

    subgraph SlotMapping["槽位映射 (83→72)"]
        MAP_TABLE["bheToBsdxSlotMap()"]
        DISCARD["丢弃 BHE 独有槽位:<br/>23, 35, 38, 40, 43,<br/>52, 62, 66-69"]
        SPECIAL["特殊处理 slot 37:<br/>CEventFreeParam → CEventVal"]
    end

    subgraph EventConvert["事件转换"]
        CREATE["SkillInfoFactory.createEventObjectBsdx()"]
        BEAN_COPY["BeanUtil.copyProperties()"]
        TRANS["transBhe*ToBsdx()<br/>字段映射"]
        INFO_MAP["InfoCollectionMapper.copyBheToBsdx()"]
    end

    subgraph Output["BSDX Waz (72 槽位)"]
        BSDX_SKILL["Skill"]
        BSDX_PHASE["SkillPhase"]
        BSDX_UNIT["SkillUnit<br/>unitQuantity = BSDX槽位号"]
        BSDX_INFO["SkillInfoObject"]
    end

    BHE_SKILL --> BHE_PHASE --> BHE_UNIT --> BHE_INFO
    BHE_INFO --> MAP_TABLE
    MAP_TABLE --> DISCARD
    MAP_TABLE --> SPECIAL
    MAP_TABLE --> CREATE
    CREATE --> BEAN_COPY --> TRANS --> INFO_MAP
    INFO_MAP --> BSDX_INFO
    BSDX_INFO --> BSDX_UNIT --> BSDX_PHASE --> BSDX_SKILL
```

**代码位置**: `WazConverter.java`

### 4.3 WazConverter 槽位映射详表

```mermaid
flowchart LR
    subgraph BHE_Slots["BHE 槽位 (83个)"]
        B0_22["0-22: 直接映射"]
        B23["23: 速度XYZ ❌"]
        B24_34["24-34: 偏移-1"]
        B35["35: 標的 ❌"]
        B36["36: → 34"]
        B37["37: 汎用変数 ⚠️"]
        B38["38: 技ツール ❌"]
        B39_41["39-41: 偏移"]
        B40["40: ハイパーアーマー ❌"]
        B42["42: 無敵"]
        B43["43: 死亡(自爆) ❌"]
        B44_51["44-51: 偏移"]
        B52["52: CPU特殊 ❌"]
        B53_61["53-61: 偏移"]
        B62["62: 属性 ❌"]
        B63_65["63-65: 偏移"]
        B66_69["66-69: マルチロック ❌"]
        B70_82["70-82: 偏移"]
    end

    subgraph BSDX_Slots["BSDX 槽位 (72个)"]
        D0_22["0-22"]
        D23_33["23-33"]
        D34["34"]
        D35["35: 汎用変数"]
        D36_38["36-38"]
        D39_55["39-55"]
        D56_58["56-58"]
        D59_71["59-71"]
    end

    B0_22 --> D0_22
    B24_34 --> D23_33
    B36 --> D34
    B37 -->|"CEventFreeParam→CEventVal"| D35
    B39_41 --> D36_38
    B44_51 --> D39_55
    B53_61 --> D39_55
    B63_65 --> D56_58
    B70_82 --> D59_71
```

### 4.4 CEventHit 子槽位映射 (41→33)

```mermaid
flowchart TB
    subgraph BHE_Hit["BHE CEventHit (41个子槽位)"]
        BH0_9["0-9: 攻撃グループ~装甲攻撃力"]
        BH10["10: ダウン時攻撃力 ❌"]
        BH11_16["11-16: ヒートゲージ系 ❌"]
        BH17_27["17-27: 補正~のけぞり"]
        BH28["28: のけぞり優先順位 ❌"]
        BH29_40["29-40: ヒットストップ~ステータス"]
    end

    subgraph BSDX_Hit["BSDX CEventHit (33个子槽位)"]
        BD0_9["0-9"]
        BD10_20["10-20"]
        BD21_32["21-32"]
    end

    BH0_9 --> BD0_9
    BH17_27 --> BD10_20
    BH29_40 --> BD21_32

    style BH10 fill:#f66
    style BH11_16 fill:#f66
    style BH28 fill:#f66
```

**代码位置**: `CEventHit.java:367-372`

```java
final int[] bheToBsdxIdx = {
    0, 1, 2, 3, 4, 5, 6, 7, 8, 9,  // 0-9 直接映射
    -1, -1, -1, -1, -1, -1, -1,    // 10-16 丢弃 (BHE独有)
    10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20,  // 17-27 → 10-20
    -1,                            // 28 丢弃 (のけぞり優先順位)
    21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31, 32  // 29-40 → 21-32
};
```

### 4.5 SpmConverter 流程

```mermaid
flowchart TB
    subgraph Input["BHE Spm"]
        BHE_PAGE["SPMPageData"]
        BHE_ANIM["SPMAnimData"]
        BHE_IMAGE["SPMImageData"]
        BHE_HIT["SPMHitArea<br/>(hitRects)"]
    end

    subgraph Convert["SpmConverter"]
        COPY_PAGE["BeanCopy + 字段补齐"]
        COPY_ANIM["BeanCopy + numPat"]
        COPY_IMAGE["BeanCopy"]
        TRANS_HIT["transHitbox()<br/>形状映射"]
        BUILD_FLAG["buildHitFlag()<br/>按数量设置低位bit"]
    end

    subgraph Output["BSDX Spm"]
        BSDX_PAGE["SPMPageData"]
        BSDX_ANIM["SPMAnimData"]
        BSDX_IMAGE["SPMImageData"]
        BSDX_HIT["SPMHitArea"]
        BSDX_FLAG["hitFlag"]
    end

    BHE_PAGE --> COPY_PAGE --> BSDX_PAGE
    BHE_ANIM --> COPY_ANIM --> BSDX_ANIM
    BHE_IMAGE --> COPY_IMAGE --> BSDX_IMAGE
    BHE_HIT --> TRANS_HIT --> BSDX_HIT
    BSDX_HIT --> BUILD_FLAG --> BSDX_FLAG
```

**代码位置**: `SpmConverter.java`

---

## Step 5-6: 索引回写与 UI 替换

### Step 5: 索引回写

```mermaid
flowchart LR
    subgraph Indices["Pipeline 产出的索引"]
        WAZ_IDX["wazaGroupIndex"]
        SPR_IDX["spriteGroupIndex"]
    end

    subgraph MekBasicInfo["Mek.MekBasicInfo"]
        WAZ_SEQ["wazFileSequence"]
        SPM_SEQ["spmFileSequence"]
    end

    WAZ_IDX -->|"alignMekIndex()"| WAZ_SEQ
    SPR_IDX -->|"alignMekIndex()"| SPM_SEQ
```

**代码位置**: `TransMekaPipeline.java:172-182`

```java
private void alignMekIndex(Mek mek, int wazaIndex, int spriteIndex) {
    if (wazaIndex >= 0) {
        mek.getMekBasicInfo().setWazFileSequence(wazaIndex);
    }
    if (spriteIndex >= 0) {
        mek.getMekBasicInfo().setSpmFileSequence(spriteIndex);
    }
}
```

### Step 6: UI SPM 替换

```mermaid
flowchart TB
    subgraph UiSpm["UI 相关 SPM"]
        MEKA_PILOT["meka_pilot.spm<br/>驾驶员立绘"]
        SELECT_MENU["selectmekamenu_meka.spm<br/>选择界面"]
    end

    subgraph Replacer["UiSpmReplacer"]
        FIND_NANOHA["查找 NANOHA 槽位"]
        REPLACE_PILOT["替换驾驶员立绘"]
        REPLACE_MENU["替换选择界面图标"]
    end

    subgraph Result["替换结果"]
        NEW_PILOT["tsukuyomi 立绘"]
        NEW_MENU["tsukuyomi 图标"]
    end

    MEKA_PILOT --> FIND_NANOHA --> REPLACE_PILOT --> NEW_PILOT
    SELECT_MENU --> FIND_NANOHA --> REPLACE_MENU --> NEW_MENU
```

**代码位置**: `UiSpmReplacer.java`

---

## 数据结构差异对照表

### MekWeaponInfo 字段差异

| 字段位置 | BHE 字段 | BSDX 字段 | 处理方式 |
|----------|----------|-----------|----------|
| 6 | bheInt1 (炎熱発動) | upgradeExp | 丢弃 |
| 7 | bheInt2 (排熱発動) | startPointWhenDemonstrate | 丢弃 |
| 8 | upgradeExp | weaponCategory | 按名称映射 |
| 9 | startPointWhenDemonstrate | weaponType | 按名称映射 |
| 10-11 | bheInt3, bheInt4 | flags... | 丢弃 |
| 12 | weaponCategory | - | 按名称映射 |
| 13 | weaponType | - | 按名称映射 |
| 14-15 | bheInt5, bheInt6 | - | 丢弃 |
| 末尾 | bheInt7, feiFlag, feiList | - | 丢弃 |

### MekMaterialBlock.spriteGroups 差异

| 特性 | BHE | BSDX |
|------|-----|------|
| 数组结构 | `spriteGroups[spriteIndex]` | `spriteGroups[spriteIndex]` |
| 元素格式 | `[actionGroupNum, actionNum, ...]` 成对 | `[actionGroupNum, ...]` 单值 |
| 处理方式 | 取每对的第一项 (actionGroupNum) | 直接使用 |
| 索引映射 | 数组索引通过 spriteIndexMap 重映射 | - |

**重要**: `spriteIndexMap` 用于重映射数组索引（即 `spriteGroups[bheIndex]` → `spriteGroups[bsdxIndex]`），而不是重映射元素值（actionGroupNum）。

### InfoCollection 字段名差异

| 事件类型 | BHE 字段名 | BSDX 字段名 |
|----------|------------|-------------|
| CEventTerm | bheInfoCollectionList | bsdxInfoCollectionList |
| CEventMove | bheInfoCollectionList1/2 | bsdxInfoCollectionList1/2 |
| CEventChange | bheInfoCollectionList1/2 | bsdxInfoCollectionList1/2 |
| CEventBlur | bheInfoCollectionList | bsdxInfoCollectionList |
| CEventScreenYure | bheInfoCollectionList | bsdxInfoCollectionList |
| CEventSpriteYure | bheInfoCollectionList | bsdxInfoCollectionList |

---

## 已知问题与修复记录

### 已修复 (2026-01-12)

| # | 文件 | 问题 | 修复内容 |
|---|------|------|----------|
| 1 | `WazConverter.java:120` | slot 37 的 `buffer != 0` 条件写反 | 改为 `buffer == 0` 并添加 `break` |
| 2 | `MekMaterialConverter.java:21` | `CLEAR_MATERIAL_GROUPS=true` 清空演出资源 | 改为 `false` |
| 3 | `MekVoiceConverter.java:15` | `CLEAR_VOICE_TABLES=true` 清空语音表 | 改为 `false` |
| 4 | `MekMaterialConverter.java:82-144` | `convertSpriteGroups` 错误地用 spriteIndexMap 重映射 actionGroupNum | 改为重映射数组索引，actionGroupNum 保持不变 |

#### Bug #4 详细说明

**原问题**: `convertSpriteGroups` 方法将 BHE 的 `(actionGroupNum, actionNum)` 对中的 `actionGroupNum` 当作 spriteGroupIndex 进行重映射，这是错误的。

**修复后逻辑**:
```java
// 1. 用 spriteIndexMap 重映射数组索引 (bheIndex -> bsdxIndex)
int bsdxIndex = spriteIndexMap.getOrDefault(bheIndex, bheIndex);

// 2. 转换元素格式：取每对的第一项 (actionGroupNum)，不重映射
for (int i = 0; i < pairCount; i++) {
    dst[i] = group[i * 2]; // actionGroupNum 直接保留
}

// 3. 存入正确的数组位置
out.set(bsdxIndex, dst);
```

### 潜在风险点

| # | 模块 | 风险 | 建议 |
|---|------|------|------|
| 1 | SpriteIndexMap | 映射不完整时保留原值 | 检查日志警告 |
| 2 | GRP 索引 | BHE/BSDX 索引顺序不同 | 确认 targetCodeName 正确 |
| 3 | MekBlocks | 块大小从 BHE 拷贝 | 序列化时自动重计算 |
| 4 | CEvent 特殊类型 | 部分字段可能遗漏 | 逐个对比 JSON 输出 |

---

## 文件关联图

```mermaid
flowchart TB
    subgraph GRP["GRP 注册表"]
        MEKA_GRP["MekaGroup.grp"]
        WAZA_GRP["WazaGroup.grp"]
        SPRITE_GRP["SpriteGroup.grp"]
        VOICE_GRP["BatVoice.grp"]
    end

    subgraph Resources["资源文件"]
        MEK["*.mek"]
        WAZ["*.waz"]
        SPM["*.spm"]
    end

    subgraph DAT["DAT 数据"]
        SELECT_DAT["selectmekamenu.dat"]
    end

    MEKA_GRP -->|"mekaIndex"| MEK
    MEKA_GRP -->|"wazFileSequence"| WAZA_GRP
    MEKA_GRP -->|"spmFileSequence"| SPRITE_GRP

    WAZA_GRP -->|"wazIndex"| WAZ
    SPRITE_GRP -->|"spriteIndex"| SPM

    MEK -->|"MekMaterialBlock.spriteGroups"| SPRITE_GRP
    MEK -->|"MekVoiceInfo"| VOICE_GRP
    WAZ -->|"CEventSprite.spmFileSequence"| SPRITE_GRP

    SELECT_DAT -->|"UI 引用"| MEKA_GRP
```

---

## 测试入口

```java
// TransferTest.java
@Test
public void testPipeline() throws Exception {
    TransMekaRequest request = buildRequest();
    TransMekaResult result = pipeline.execute(request);
    // 输出到 src/main/resources/testBhe
    // 打包为 Update3.pac
}
```

---

*文档生成时间: 2026-01-12*
