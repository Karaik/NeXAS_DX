# BHE → BSDX 移植流程文档

## 概述

本模块实现将 Baldr Heart EXE (BHE) 的机体资源移植到 Baldr Sky DiveX (BSDX) 的功能。

## 核心转换器

### 1. WazConverter
**文件**: `steps/WazConverter.java`

将 BHE 的 waz 文件（技能数据）转换为 BSDX 格式。

#### 槽位映射 (83 → 72)

BHE 有 83 个槽位，BSDX 只有 72 个。以下 BHE 独有槽位会被丢弃：

| BHE Slot | 名称 | 说明 |
|----------|------|------|
| 23 | ﾍﾞｸﾄﾙ：速度XYZ | BSDX 无对应 |
| 35 | 標的 | BSDX 无对应 |
| 38 | 技ツールパラメータ | BSDX 无对应 |
| 40 | ハイパーアーマー | BSDX 无对应 |
| 43 | 死亡(自爆) | BSDX 无对应 |
| 52 | ＣＰＵ特殊行動 | BSDX 无对应 |
| 62 | 属性 | BSDX 无对应 |
| 66-69 | マルチロック系列 | BSDX 无对应 |

#### 特殊处理

- **Slot 37 (汎用変数)**: BHE 使用 `CEventFreeParam`，BSDX 使用 `CEventVal`
  - 从 `CEventFreeParam.unitList` 中提取 `buffer == 0` 的数据
  - 映射到 BSDX slot 35

### 2. MekConverter
**文件**: `steps/MekConverter.java`

将 BHE 的 mek 文件（机体数据）转换为 BSDX 格式。

#### 子转换器

| 转换器 | 功能 |
|--------|------|
| MekAiConverter | AI 行为转换 |
| MekVoiceConverter | 语音表转换 |
| MekMaterialConverter | 演出资源引用转换 |

### 3. MekMaterialConverter
**文件**: `steps/MekMaterialConverter.java`

处理机体的演出资源引用（sprite/SE/voice）。

#### 配置项

```java
// 设为 false 以保留演出资源引用
private static final boolean CLEAR_MATERIAL_GROUPS = false;
```

#### BHE vs BSDX 差异

- BHE 的 `spriteGroups` 是成对数据 `<u32, u32>`
- BSDX 只保留第一项
- 通过 `spriteIndexMap` 做索引映射

### 4. MekVoiceConverter
**文件**: `steps/MekVoiceConverter.java`

处理机体语音表的转换。

#### 配置项

```java
// 设为 false 以保留语音映射
private static final boolean CLEAR_VOICE_TABLES = false;
```

### 5. BatVoiceConverter
**文件**: `steps/BatVoiceConverter.java`

深拷贝战斗语音组数据。

### 6. SpmConverter
**文件**: `steps/SpmConverter.java`

转换精灵/动画数据。

## InfoCollectionMapper

**文件**: `src/main/java/com/giga/nexas/util/InfoCollectionMapper.java`

处理 BHE/BSDX 之间 InfoCollection 字段名不一致的问题。

### 覆盖的事件类型

| 事件类型 | BHE 字段 | BSDX 字段 |
|----------|----------|-----------|
| CEventTerm | bheInfoCollectionList | bsdxInfoCollectionList |
| CEventMove | bheInfoCollectionList1/2 | bsdxInfoCollectionList1/2 |
| CEventChange | bheInfoCollectionList1/2 | bsdxInfoCollectionList1/2 |
| CEventBlur | bheInfoCollectionList | bsdxInfoCollectionList |
| CEventScreenYure | bheInfoCollectionList | bsdxInfoCollectionList |
| CEventSpriteYure | bheInfoCollectionList | bsdxInfoCollectionList |

## 移植流程 (TransMekaPipeline)

```
1. BatVoice 深拷贝
2. grp 注册表槽位替换
3. spritegroup 索引映射
4. mek/waz/spm 核心资源转换
5. 回写索引保持一致性
6. UI SPM 替换测试槽位
```

## 测试入口

**文件**: `TransferTest.java`

```java
@Test
public void testPipeline() throws Exception {
    // 执行完整移植流程
    // 输出到 src/main/resources/testBhe
    // 打包为 Update3.pac
}
```

## 修复记录

### 2026-01-12

1. **MekMaterialConverter**: `CLEAR_MATERIAL_GROUPS` 改为 `false`
   - 修复：机体没有演出资源的问题

2. **MekVoiceConverter**: `CLEAR_VOICE_TABLES` 改为 `false`
   - 修复：机体不播放语音的问题

3. **WazConverter**: slot 37 的 buffer 条件修复
   - 原代码：`if (unit.getBuffer() != 0) continue;`
   - 修复后：`if (unit.getBuffer() == 0) { ... break; }`
   - 修复：汎用変数数据提取错误的问题

## 已知限制

1. BHE 独有的 11 个槽位功能无法移植
2. MekWeaponInfo 中 BHE 多出的 8 个字段会被丢弃
3. MekBlocks 大小信息在生成时会自动重计算
