# Nagi / Katou 可装备变体实施记录

日期：2026-10-26。本批保留原武装与原技能，通过既有 `customizeBeforeWrite` 和共享 `CharacterWeaponVariants.append` 追加两个独立普通武装；不修改共享工具、Importer、Runner、pipeline、Sou或Clarias。未执行Maven、提交或推送，回归由主代理串行执行。

## 原版输入与边界

行为测试每次直接读取 `game/bhe/mek/nagi/nagi.mek`、`game/bhe/waz/nagi/nagi.waz`、`game/bhe/mek/katou/katou.mek`、`game/bhe/waz/katou/katou.waz`，使用实际BHE解析器和当前转换器、Term重编译器建立目标DTO，不使用旧JSON当作本轮通过证据。原版文件只读，生成回环只写JUnit `@TempDir`。

源布局：Nagi为17武装、8个regular Material、17个trailing、25个总entries；Katou为19/8/19/27。均为非KOU单等级尾表。共享工具承担武装/技能深复制、Material三视图追加、供体合并、容量和幂等核验；本批不重复实现这层。

Katou测试将本角色显式Change目标group从源19映射为233，并把目标MEK `wazFileSequence` 设233，验证新出口使用最终group。该fixture只模拟角色跳转的最终调用边界，不宣称完整graft已经通过；完整pipeline结构同步与实机由主代理统筹。

## Nagi：瞬間ディフレクト

- 源MEK：`シールド：ディフレクト / 0018`，对应主WAZ `シールド：ディフレクト / 0016`，2 phase；源skill位置23仅作证据，不参与生产定位。
- 新MEK名：`瞬間ディフレクト`；新WAZ英文身份：`DX_QUICK_DEFLECT`。保留源weaponSequence、分类和标志；热耗12/12、upgradeExp=0。原长窗口武装保留。
- 说明：`短い受付で攻撃を受け止め、成功時はディフレクトへ移る。長く構えず、読み合いから素早く攻めへ戻りたい。`

以下phase按1起算；槽位为转换后的BSDX槽位。

| 原签名 | 复制变体的预期变动 |
| --- | --- |
| P1 slot0 `CEventSprite` 4..93 | 4..19；载荷不改。 |
| P1 slot0 `CEventSprite` 94..113 | 20..39；载荷不改。 |
| P1 slot36 `CEventVal` 5f `(0,0,0,4)` | 装甲开启保持。 |
| P1 slot36 `CEventVal` 94f `(0,0,0,0)` | 关闭移到20f。 |
| P1 slot47 `CEventVal` 79..113 `(0,3,0,3)` | 恢复/取消窗口移20..39。 |
| P1 slot71 `CEventChange` 5..93、flag1/int1=0；条件root0/type`[2,6,6]`、param空、objects`[0,0]`；目标root16/type`[0,0]`、param空 | 防御触发窗口5..19；条件及原生反击派生不改。 |
| P1 slot71无条件113f，目标root16/type`[0,2]` | 失败出口移39f。 |
| P2 Hit19..35f、唯一启用Unit6直接攻击值150，79f原生出口 | 完整保留，只校验不改写。 |

装甲关闭、动画、取消和防御条件共同校验后才改变独立副本，不能仅改Change窗口而遗留装甲。原短时属性5..19与20f复位等其余事件自然保留；不新增trion计数，也不改变反击威力。

## Katou：トンファー・二連打

- 源MEK：`トンファー連打 / PUNCH2`，对应WAZ `トンファー：トンファー連打 / T_AXE04`，6 phase；源skill位置18仅作证据。
- 収納供体：MEK `トンファー収納 / 0008` → WAZ `トンファー：収納 / 0009`，1 phase。旧返回目标为 `トンファー：構え中 / 0010`，2 phase。均以双名与MEK引用唯一定位。
- 新MEK名：`トンファー・二連打`；新WAZ英文身份：`DX_TONFA_TWO_HIT`。保留源weaponSequence、分类和标志；热耗18/18、upgradeExp=0。原按键持续连打保留。
- 说明：`左右の二撃を出し切り、トンファーを収納する。長い連打に付き合わず、短い攻めから距離を取り直したい。`

| 原签名 | 复制变体的预期变动 |
| --- | --- |
| P3/P5 slot71的3f按键Change，flag1/int1=0，条件root0/type`[2,5,3,3]`、param`[10]`、objects`[0,0]`；P3目标`[0,0]`、P5目标`[0,1]`且param`[1]` | 只移除这两条精确匹配的按键循环。 |
| P3无条件40f，目标root16/type`[0,1]`、param`[5]` | 移12f，目标phase index3，即P4。 |
| P5无条件40f，目标root16/type`[0,0]`、param空 | 移12f，仍进入P6。 |
| P6无条件15f，目标root16/type`[1]`、param为最终角色group及構え中当前skill index | 改跳最终同角色収納skill index。未写源group19或源skill15。 |
| P3/P5 Hit1..5f、唯一启用Unit6直接攻击值均30 | 两击完整保留；不改击退、补正、命中次数及其他攻击载荷。 |
| 収納21f原生结束 | 校验并保持；其武装Material作为追加变体的显式供体传给共享工具。 |

P1/P2/P4准备、转换与其余事件保留。固定流程为P1→P2→P3第一击→P4→P5第二击→P6→収納；不再回到原按键循环或待机架势。

## 行为回归记录

新测试为同目录 `NagiVariantTest` 与 `KatouVariantTest`，各4项。主代理命令：

```powershell
mvn -q "-Dtest=NagiVariantTest,KatouVariantTest" test
```

RED已由主代理确认：8 tests / 8 failures / 0 errors，日志 `target/bhe-audit-20261026/nagi-katou-red.log`。缺少变体和未拒绝漂移均为行为失败，没有把编译或fixture错误计入RED。

GREEN：主代理串行执行后的两份Surefire报告已直接读取核对，NagiVariantTest与KatouVariantTest各4项，合计8项，0失败、0错误、0跳过。报告位于 `target/surefire-reports/com.giga.nexas.transfer.bhe2bsdx.meka.nagi.NagiVariantTest.txt` 与同目录 `com.giga.nexas.transfer.bhe2bsdx.meka.katou.KatouVariantTest.txt`。本代理没有自行运行Maven。

测试覆盖原武装/原技能与BHE源对象/原文件保全、新增武装独立存在、精确窗口/出口、后段签名破坏时拒绝且MEK/WAZ整体不变、已有变体被改坏后拒绝、重复执行不重复追加、Material深复制及収納供体、真实MEK/WAZ生成重解析、windows-31j说明无损编码。

尚未启动游戏，尚未证明短窗防御真实触发、两击实际命中数、装备菜单/预览、热量平衡或中断后状态清理。Java测试及二进制回环不能替代这些实机结论。
