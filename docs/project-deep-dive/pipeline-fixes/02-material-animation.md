# 02：MEK Material 内部公共动画映射

## 根因与修复

BHE 公共 SPM 的动画接入 BSDX 同名宿主时，会追加到宿主已有动画之后。公共接入计划记录外层 SpriteGroup 映射和内部 actionGroup 基址。WAZ 的 CEventSprite 已同时消费这两层映射，但 Tsukuyomi 与 Followup 两份 RebindMekStep 原先只搬迁 Material 的外层 SpriteGroup，内部动画号保持源值。

例如 BSDX Smoke 宿主原有 40 个动画，源 Material 的动画 `[0,10,41]` 应指向宿主的 `[40,50,81]`。旧实现仍写 `[0,10,41]`，MEK 的预载清单与 WAZ 执行的动画身份不一致。

本项仅修改两份 BHE `RebindMekStep.remapSpriteGroups`：先调用 `resolveSpriteGroupIndex` 定位目标 group，再逐个动画号调用现有 `resolveSpriteActionGroupIndex`，将结果存入独立数组，最后沿用已有的目标 group 合并规则。私有动画内部编号不加公共偏移；负哨兵由 resolver 原样保留。JINKI V2 未改动。

## 回归与验证

定向命令：

```powershell
mvn -q "-Dtest=RebindMekStepBheResourceRebindTest" test
```

先只修改回归测试，在旧生产代码上运行：3 个测试全部按预期失败，0 个执行错误。Tsukuyomi 和 Followup 的真实 public 重绑入口均出现 `expected 40 but was 0`；真实 Tsukuyomi MEK 回归也在 Smoke Material 的首个动画引用上出现同样失败。

随后修改两份生产实现，再运行相同命令：退出码 0，3 个测试全部通过，失败、执行错误和跳过均为 0。

新增的两个入口回归直接调用 `rebindTsukuyomiMek` 与 `rebindFollowupMek`，不反射调用私有 helper、不替换 resolver。手写期望覆盖：

- 公共基址 40：`[0,10,41] → [40,50,81]`。
- 两个源 group 汇入同一目标 group：先映射各自内部动画，再合并重复目标编号。
- 私有动画编号不变，公共和私有负哨兵 `-1/-2` 均保留。
- null group、空 group、null Material entry 与空 Material entry。
- Material 的 entries、regularEntries、trailingEntries 三个视图。
- 输出 entry/数组独立，原始源数组保持不变。

既有真实资源回归读取当前 BHE Tsukuyomi MEK JSON、BHE 公共资源 JSON和 BSDX 原版宿主二进制，在内存构建实际公共 append plan。它保留原有武装、语音、SE、AI term 校验，将错误的“内部源动画数组保持不变”断言改为按公共计划记录的动画基址检查最终身份，并明确断言实际 Smoke 基址为 40。

## 范围与限制

本项未改变公共资源选择、追加顺序、SPM 内容、GRP 容量、武装数和等级规则；未实施公共稀疏追加或角色演出配方。测试没有修改原版资源或现有 JSON 输入，没有运行完整多角色打包链，也没有进行实机验证。后续第 4 项交付门禁仍需检查最终 MEK Material 与有效加载层 SPM 动画索引是否一致。
