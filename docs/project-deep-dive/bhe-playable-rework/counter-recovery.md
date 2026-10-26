# 九角色基础计数恢复：限定channel0

日期：2026-10-26。按 `task3-counter-brief.md` 在现有转换架构内修复两个顶层转换入口及Effect内层FreeParam分支；不改角色Customizer、Sou目录、资源规划或Importer，不引入四通道虚拟机。实现与定向回归完成，生产代码冻结，交独立审查；尚未实机验证。

## 变化与范围

源 `buffer != 0` 才表示子载荷存在，`unitSlotNum == 0` 才是第一通道。两路 `convertFreeParam` 原先选择buffer0空槽；Effect分支原先跳过buffer1、还可能误取其它通道。现在三条路径均只搬运实际启用的channel0 CEventVal四整数。

合法零载荷同样保留；存在标志可为2等任意非零值。没有active0时沿用单通道零输出，不要求buffer0哨兵，不把其它通道冒充第一通道，也不因为合法多通道源结构就拒绝公共资源。active0缺失data、类型不符、不完整四整数或重复启用时明确失败。父FreeParam事件帧、目标slot/typeId及Effect其它字段保持原转换契约，四个整数按值复制，不修改源对象。

九角色主WAZ的真实FreeParam经主审查原始资源聚合确认仅启用channel0，本修复可恢复这些基础更新/重置载荷；公共Tama03/Tama05等资源含其它通道，本次仍丢失channel1–3及其独立通知语义。不能据此宣布父子弹幕、热量连斩、Nagi储存、所有形态或全部四通道机制已经完成，具体技能的条件和退出循环仍需另行设计和实机验收。

## 真实回归输入

| 输入 | 精确样本与独立期望 |
| --- | --- |
| `game/bhe/waz/freja/freja.waz` | WAZ26 `武幻剛乱舞/0001`，P2/P3/P5顶层FreeParam channel0均为`(0,0,1,1)`，P4为`(0,0,0,0)`重置；分别走Followup及Tsukuyomi完整WAZ转换，写临时WAZ后重解析。 |
| `game/bhe/waz/misaki/misaki.waz` | WAZ12 `掴む/EGGE04`，P2第二个Effect的源内层slot37包含active0 `(0,0,0,1)`；目标内层slot36保持该四整数、父事件帧及外层11个头部字段，生成后重解析。 |

两份源文件均在测试前后核SHA256，源DTO完整JSON树在转换和目标编辑后保持一致。所有写出仅落JUnit `@TempDir`，不使用原游戏资源或共享JSON作为输出。

## RED/GREEN

主代理串行执行命令；实施代理未自行运行Maven：

```powershell
mvn -q "-Dtest=BheChannelZeroCounterRecoveryTest" test
```

RED：9项、9失败、0错误。真实Freja的三次非零载荷及Misaki Effect的非零载荷都仍变零；合法无哨兵、无active0及全部active的正例不能通过；active0缺data/类型不符未拒绝。日志 `target/bhe-audit-20261026/counter-red.log`，完整Surefire备份 `target/bhe-audit-20261026/BheChannelZeroCounterRecoveryTest-red.txt`。未把编译或fixture错误计作RED。

GREEN：主代理串行执行后，计数器9项、0失败、0错误、0跳过；与Freja6/Yuri5/CommonChange4/源隔离2/既有重绑2/NativeHandler1合计29项全部通过，Maven退出码0。`target/bhe-audit-20261026/counter-and-core-green.log`因Maven静默成功而为0字节，不能靠该空文件证明通过；结果以7份最新Surefire文本报告为据，实施代理另直接核对计数器报告的9/0/0/0。九项包含双真实顶层入口、真实Effect、buffer2、零payload无哨兵、没有active0、乱序全部通道active、缺data、类型不符；合成正例同时执行两路顶层及Effect内层，期望均为独立字面值。生成重解析、源payload及文件hash保全属于测试的实际断言，不用解析无异常替代。

本批5个文件均通过严格UTF-8解码、无BOM及CRLF检查，编码违规0；没有提交或推送。

未启动游戏、未修改CRT或容量旁路，未据载荷恢复宣称热量数值或战斗循环已等价。
