# Initializer 补齐使用文档

本文档说明 `EnsureInitializerWeaponStep` 的来源、调用方式、注意事项，以及 `Tsukuyomi` 的接入示例。

## 1. 这个步骤是干什么的

`EnsureInitializerWeaponStep` 用于处理一种固定场景：

- BHE 单机体迁移到 BSDX 后
- 当前机体 `MEK` 里没有 `INITIALIZER` 武装
- 当前机体 `WAZ` 里也没有可直接使用的 initializer skill

这时它会在“单机体资源已经完成重绑之后”，补出一套当前机体可消费的 initializer：

1. 追加一条 `MEK` 武装行
2. 必要时追加一条清洗后的 `WAZ` initializer skill
3. 追加一条同位 `material trailing entry`
4. 由调用方同步 `WazaGroup.param`

## 2. 模板从哪来

模板固定来自 BSDX 原生资源：

- `src/main/resources/game/bsdx/mek/Zako116a.mek`
- `src/main/resources/game/bsdx/waz/Zako116a.waz`

原因不是“Zako116a 是唯一正确答案”，而是：

- 它有完整 initializer 武装行
- 它有完整 initializer skill 骨架
- 它属于 BSDX 原生资源，字段语义和目标引擎一致

注意：

- 不能直接用 BHE initializer 转换结果当模板
- 也不能把 `Zako116a.waz` 的 initializer 原样塞进别的机体

前者会在运行时触发 `std::length_error("vector<T> too long")`
后者会把模板机体自己的 sprite/SE/voice/material 链带进目标机体，hover 演示时会崩

## 3. 现在这一步实际怎么做

它不是“复制一个现成初始器”，而是“按共性重建一个当前机体版初始器”。

### 3.1 MEK 武装行

直接复用模板武装行的大部分字段：

- `weaponName = イニシャライザ`
- `weaponSequence = INITIALIZER`
- `weaponCategory = 2`
- `weaponType = 0`
- `weaponIdentifier = 0`
- `weaponUnknownProperty19 = 1`

并把：

- `wazSequence`

改成当前目标 `WAZ` 中 initializer skill 的真实 index。

### 3.2 WAZ skill

如果当前目标 `WAZ` 已经有 `INITIALIZER` skill：

- 不重复造 skill
- 直接让武装行指向它

如果当前目标 `WAZ` 没有 `INITIALIZER` skill：

- 读取模板 `Zako116a.waz` 的 initializer skill
- 做“清洗版重建”

清洗规则：

- 保留共性事件
  - `CEventScreenEffect`
  - `CEventScreenLine`
  - `CEventBlur`
  - `CEventChange`
  - 以及 initializer 自己的若干 `CEventVal / CEventValRandom`
- 替换 `unit 0` 的 sprite
  - 不用模板 sprite
  - 改成当前机体自己的 `STAND` skill 的 `unit 0`
- ??????
  - `unit 53` ? `Voice`

`unit 52` ??? `SE` ?????
??? Aki / Gregory / ?? `Zako*.waz` ? `INITIALIZER`????????????? BSDX ?? initializer ?? SE?

### 3.3 Material trailing

新增武装后，`MEK` 尾部 `material trailing` 必须同步追加一条。

这里不能复制模板 `Zako116a` 的 material，因为它会把模板机体自己的素材链挂进来。

现在的策略是：

- 复制当前机体已有的一条安全 material

它的目的不是完美还原 initializer 专属演示，而是：

- 保证菜单/hover/演示的读取结构同位自洽
- 不引入别的机体资源链

## 4. 调用时机

必须放在：

1. 单机体 `MEK/WAZ` 已经完成重绑之后
2. 静态资源输出之前

不能放在 selected 转换层，更不能放在前置资源闭包扫描之前。

原因：

- 这一步产出的是目标侧补齐内容
- 如果提前做，前面的 BHE 资源闭包会把它误当成 BHE 源资源

## 5. 怎么调用

当前调用签名：

```java
ensureInitializerWeaponStep.ensureAfterRebind(
    targetWazFileName,
    targetMek,
    targetWaz,
    notes
);
```

参数含义：

- `targetWazFileName`
  - 只用于日志/审计说明
- `targetMek`
  - 已经重绑完成的目标 `MEK`
- `targetWaz`
  - 已经重绑完成的目标 `WAZ`
- `notes`
  - 可选审计输出列表

## 6. 调用后调用方还要做什么

如果这一步追加了新的 initializer skill，调用方必须同步：

- `WazaGroup.param = targetWaz.skillList.size()`

否则 `WazaGroup` 记录的 skill 数和真实 `WAZ` 不一致，运行时会出问题。

## 7. Tsukuyomi 的接入示例

接入位置在：

- [TsukuyomiGraftPipeline.java](/D:/Code/NeXAS_DX/src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/tsukuyomi/TsukuyomiGraftPipeline.java)

顺序是：

1. `rebindMekStep.rebindTsukuyomiMek(...)`
2. `rebindWazStep.rebindTsukuyomiWaz(...)`
3. `ensureInitializerWeaponStep.ensureAfterRebind(...)`
4. `syncMainWazaSkillCountAfterInitializer(...)`
5. `importStaticAssetsStep.importAssets(...)`

调用代码形态：

```java
Mek reboundTsukuyomiMek = rebindMekStep.rebindTsukuyomiMek(...);
Waz reboundTsukuyomiWaz = rebindWazStep.rebindTsukuyomiWaz(...);

ensureInitializerWeaponStep.ensureAfterRebind(
        request.getWazFileName(),
        reboundTsukuyomiMek,
        reboundTsukuyomiWaz,
        convertedBundle.getNotes()
);

syncMainWazaSkillCountAfterInitializer(
        bsdxBaseline,
        grpAppendPlan,
        reboundTsukuyomiWaz
);
```

## 8. 后续机体如何复用

后续做 `Yuri` 或任何别的 BHE 单机体时，不要重新写一套 initializer 逻辑。

复用条件只有这几个：

1. 当前机体已经完成正常 `MEK/WAZ` 转换
2. 当前机体已经完成正常重绑
3. 当前机体 `WAZ` 至少有一个安全基础 skill，可作为 sprite 替换来源
   - 当前实现默认优先找 `STAND`
4. 当前机体 `MEK material trailing` 至少有一条可复制的安全条目

满足这些条件，就直接复用 `EnsureInitializerWeaponStep`。

## 9. 注意事项

### 9.1 不要把 initializer 当成普通武装复制

它不是只看 `weaponCategory=2` 就结束的。
如果没有同位 `material`、没有对应 `WAZ`、没有同步 `WazaGroup.param`，都会出问题。

### 9.2 ????????? sprite / Voice

模板资源链是模板机体自己的，不是通用资源。
跨机体直接复制，动态上已经验证会炸。
- Voice???
- SE???? `unit 52` ??? initializer SE
- material?????????????

### 9.3 这一步是“后置目标侧补齐”

它不是 BHE 源侧转换的一部分。
不要把它混进 BHE 转换器里。

### 9.4 这一步允许保留包内魔法值

例如模板路径放在本包常量里是允许的。
但前提是：

- 来源明确
- 原因明确
- 文档明确

这也是为什么本包同时保留：

- `initializer-commonality.md`
- `initializer-usage.md`
