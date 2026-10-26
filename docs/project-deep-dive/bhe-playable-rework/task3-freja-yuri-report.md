# Task3第一批：Freja与Yuri的操作收益

日期：2026-10-26。按 `task3-freja-yuri-brief.md` 实施；这是九角色重做的第一批参数与操作窗口调整，不代表完整角色机制、热量循环或实机验收完成。当前状态：本批实现及定向回归完成，代码已停止编辑，交主代理独立审查。

## 实际改动

| 角色与源身份 | 改动 |
| --- | --- |
| Freja：MEK `スピア/0003` → WAZ `スピア/0002`（原索引18） | P3 slot18的1..8帧、P5 slot18的12..15帧六整数由`(0,32,0,0,16,0)`改为`(0,36,0,0,20,0)`；两个减速段保持。P3的3..10帧、P5的16..24帧Hit Unit6直接攻击值150→180。对应武装热耗24/24、upgradeExp=0。 |
| Yuri：MEK `アサシンダガー/0004` → WAZ `アサシンダガー/STRAIGHT04`（原索引12） | P3 Hit46..48→44..50，Unit6直接值100→140；原同窗Change同步为44..50，其条件与跳转保持，92帧后备结束保持。对应武装热耗28/28、upgradeExp=0。 |
| Yuri：MEK `サイドステップキック/NAGE` → WAZ `サイドステップキック/SIDESTEPKICK04`（原索引15） | P1 slot18的14..28帧由`(0,32,0,0,8,0)`改为`(0,40,0,0,8,0)`；攻击力80及恢复段保持。热耗12/12、upgradeExp=0。 |

phase均从1计，slot均为目标BSDX槽位。实际实现通过WAZ日英双名、phase数量、事件类/帧窗/载荷及MEK双名和其WAZ引用交叉定位，没有按原裸索引选择武装。

两份机体日文简介采用brief文字；三份武装说明对应踏み込み/単発威力、刺入窗口或位置调整。不再用Yuri原文的移动持续伤害作为已完成效果承诺。

## 实现边界

两角色继续使用现有 `customizeBeforeWrite`。新增 `ProtectedCharacterEdits` 只提供精确定位、签名验证和受保护写操作，没有新增pipeline或方案框架。

每个角色先验证全部目标，再执行写操作；速度仅接受原/目标六整数，Hit仅接受原帧窗+原攻击值或目标帧窗+目标攻击值，Unit6必须为单个启用的直接值。Yuri另核对原Change条件与转移载荷。重复执行不会累加；版本漂移、缺失或重复目标在任何写入前失败。攻击值通过替换独立参数列表写入，不就地修改可能共享的源列表。

未新增WAZ/MEK条目、特效、资源引用或玩家入口；未修改公共转换、Importer、Runner、Sou、CLARIAS、CRT或容量处理，未改变既有主线架构。

## 验证记录

测试从 `game/bhe/mek/<角色>/<角色>.mek` 与 `game/bhe/waz/<角色>/<角色>.waz` 原版二进制读取，经当前格式转换及Term对象图重写建立目标对象。生成路径只使用JUnit `@TempDir`，不覆盖游戏原资源或共享JSON。

由主代理串行执行，实施代理未自行运行Maven：

```powershell
mvn -q "-Dtest=FrejaCustomizerTest,YuriCustomizerTest" test
```

RED：Yuri五项、Freja三项先正常失败，例为Yuri实际46帧而期望44帧，Freja实际`(0,32,0,0,16,0)`而期望`(0,36,0,0,20,0)`；漂移与重复身份均因旧no-op不拒绝而失败。Freja另一个正向测试最初因负对照假定不存在的P3 slot19而出错，改为显式创建非速度槽后，主代理单跑下列方法确认1项/1失败/0错误，日志为 `target/bhe-audit-20261026/freja-red-final.log`。编译泛型与fixture错误没有计作行为RED。

```powershell
mvn -q "-Dtest=FrejaCustomizerTest#realSpearImprovesBothBranchesAndPreservesOtherCombatData" test
```

GREEN：主代理串行执行后，Freja4项、Yuri5项全部通过；与源隔离2项、既有重绑2项、公共Change4项组合运行共17项，0失败、0错误、0跳过，退出码0。组合日志为 `target/bhe-audit-20261026/rework-first-green.log`，本批另外直接核对了两份Surefire文本报告的4/5项通过结果。真实MEK/WAZ回环和日文windows-31j无损编码检查包含在已通过测试中，没有将普通编译或生成成功替代这些断言。

九项角色测试覆盖：真实两分支攻击/速度与成本；其他技能、同数值非速度事件、命中计数/补正/击退、恢复段与原源对象保持；整批失败不留下半修改；重复执行；真实MEK/WAZ生成后重解析；相关日文简介的windows-31j无损编码。

独立审查后补充：Unit6的root同样必须属于原/目标签名，不能只检查typeList与参数值。真实转换后Freja长枪第二攻击为BSDX `PARAM/VAL`，root=23；新增两个回归只将该第二攻击root改为24或null，要求在任何写入前拒绝整批。主代理执行RED得到2项/2失败/0错误，日志 `target/bhe-audit-20261026/attack-root-red.log`，没有把表达式保持可解析当作签名未漂移。随后在既有校验内补上root==23保护；主代理最终GREEN为Freja6/Yuri5/NativeHandler1/Isolation2/既有Rebind2/CommonChange4共20项，全部通过、退出码0，日志 `target/bhe-audit-20261026/rework-core-green.log`。首轮9项与组合17项的历史通过记录保持原口径。

未启动游戏，也未完成当前最终graft/PAC或低热/高热、玩家连段/CPU使用的实机测量。静态及二进制回环通过只能证明修改可定位、保存且保持相应边界，不能宣称已证明游戏不崩或手感、强度达标。
