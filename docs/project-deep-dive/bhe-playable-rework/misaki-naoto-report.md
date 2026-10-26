# Misaki / Naoto 可装备变体实施记录

日期：2026-10-27。按主代理分配，保留原武装和既有架构，使用现有 `customizeBeforeWrite` 与 `CharacterWeaponVariants.append` 添加本角色变体。编辑范围仅两个Customizer、同目录VariantTest和本报告；不修改共享工具、Importer、Runner或pipeline，不执行Maven、提交或推送。

## 精确设计与源身份

### Misaki：転移手・挟撃

源MEK `転移手：前方ビターン / 0037` → WAZ `転移手：前方ビターン / WARP_P1_11`，3 phase。供体MEK `転移手：後方ビターン / 0033` → WAZ `転移手：後方ビターン / WARP_P1_09`，3 phase。源skill17/18只作取证，不作为生产查找方式。

前/后方P2各有4条0f `CEventEffect`，源内部Unit3目标按顺序为Tama05 `[332,332,331,331]`。动态发射位置、方向、偏移、嵌套时间及其他载荷全部随已重绑Effect保持。新变体保留前方0f四条，独立复制后方四条到外层12f，内部相对帧保持0；P2末尾Sprite由3..20延至3..32，无条件出口20f延至32f，P3原27f退出保持。

新名 `転移手・挟撃`，英文身份 `DX_PINCER_HANDS`，热耗36/36、upgradeExp=0；后方武装作为Material供体。日文说明：`前方、続いて後方から転移手を叩き込む二段の挟撃。床召還で動きを制限し、手を避けた相手には掴みを重ねたい。`

保留 `MisakiSkillGlitchFixer` 现有公共发射数修复。旧测试存在不带MEK/WAZ、只检查公共修复的上下文；该探针路径保留，真实角色载荷则必须通过变体校验。

### Naoto：ワンツー・コンファーム

源MEK `ジャブ / 0001` → WAZ `ジャブ / STRAIGHT07`，1 phase；终结供体MEK `ストレートパンチ / 0003` → WAZ `ストレートパンチ / STRAIGHT04`，1 phase。原skill11/12只作证据，实际目标按当前双名定位。

Jab原Hit11..16f、Unit6直接攻击值20保持；在同窗加入命中条件Change，flag1/int1=0，条件root0/type`[2,6,0,0]`、param`[0]`、objects`[0,0]`，目标root16/type`[1]`、param为最终角色group和现有Straight当前index。条件语义以真实原版Yuri的アサシンダガー P3命中派生转换结果作独立对照，不凭字面猜测。

落空仍走Jab原37f无条件退出。Straight原Hit17..30f、直接攻击值30和57f原生退出完整保留，不额外复制隐藏终结技能。

新名 `ワンツー・コンファーム`，英文身份 `DX_HIT_CONFIRM_ONE_TWO`，热耗20/20、upgradeExp=0；Straight武装作为Material供体。日文说明：`ジャブの命中時だけストレートへつなぐ。空振りでは追撃せず、近距離の確認から次の展開を選ぶ。`

## 测试输入与边界

两个VariantTest每次重新解析原版BHE MEK/WAZ，再调用实际转换器与Term重编译器；不以旧JSON代替当前解析。生成重解析只写JUnit `@TempDir`。

- Misaki还解析原版 `tama05.waz`，建立真实公共修复上下文；目标公共group9、skillBase32，测试前/后方Emitter均已映射为 `[9,364] / [9,363]`，用于阻止写回裸源编号。对比8条完整Effect载荷，独立断言只增加后方批次的外层12f，不覆盖动态位置。
- Naoto将既有Straight由源位置12移到fixture位置13，同时更新MEK对应引用；最终MEK主WAZ group设233，断言新命中目标为 `[233,13]`，不能写源group15或源skill12。该fixture测试最终角色调用边界，不宣称完整graft已运行。
- 两角色覆盖原技能/原武装与原BHE对象保全、深复制、额外Material、幂等、已有变体漂移拒绝、后段签名破坏时MEK/WAZ整体不变，以及真实MEK/WAZ回环和windows-31j说明无损编码。

## 进度与验证记录

当前状态：生产实现与本批8项回归完成，交主代理独立审查。两个Customizer都只通过共享append工具写入独立副本；原普通武装与既有终结技能保持。

```powershell
mvn -q "-Dtest=MisakiVariantTest,NaotoVariantTest" test
```

RED已由主代理确认：8 tests / 8 failures / 0 errors，日志 `target/bhe-audit-20261026/misaki-naoto-red.log`。缺少追加武装或不拒绝漂移为行为失败，没有将编译或fixture错误计入RED。修复后已直接读取主代理串行执行产出的两份Surefire报告：MisakiVariantTest与NaotoVariantTest各4项，合计8项，0失败、0错误、0跳过；本代理没有自行运行Maven。报告位于 `target/surefire-reports/com.giga.nexas.transfer.bhe2bsdx.meka.misaki.MisakiVariantTest.txt` 与同目录 `com.giga.nexas.transfer.bhe2bsdx.meka.naoto.NaotoVariantTest.txt`。

第一次GREEN组合为24 tests / 0 failures / 3 errors：Naoto4项与既有Misaki16项通过，Misaki新测试3项在后方技能深复制时报 `Unrecognized field "empty"`。原因是局部默认Jackson mapper回读 `Waz.Skill.isEmpty` 派生属性；现已对齐共享副本工具，局部配置 `FAIL_ON_UNKNOWN_PROPERTIES=false`，不修改DTO、原解析器、测试断言或深复制要求。修复后本批两份VariantTest报告均通过；首次日志为 `target/bhe-audit-20261026/misaki-naoto-green.log`。扩大组合回归由主代理统一记录。

Misaki实现对前/后方四条Emitter及发射数/间隔、原生阶段出口作签名保护，公共group与331/332对应的目标skill从本轮公共append计划取值；副本后方Emitter保留完整位置与方向对象。Naoto实现校验Jab/Straight攻击与原生出口，新增命中条件使用独立collection/list，目标为当前角色group和当前Straight身份对应位置。

未启动游戏；命中确认在真实战斗中的触发时机、后方手实际生成位置、错拍夹击、装备/预览、热量平衡与中断恢复仍待实机。结构测试或二进制回环不能证明这些实机目标已经完成。
