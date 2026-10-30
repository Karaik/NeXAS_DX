# Sou「人間砲弾」配对计数协议修复

> 2026-10-31资料状态：本文测试计数和旧运行身份只对应记录当时的代码。原target已清空，旧报告／运行日志路径不再作为当前可访问文件保证。现保全的资料见[实机文本证据说明](../../../src/test/resources/bhe-runtime-evidence-20261030/README.md)；程序、PAC、转储、截图、录像、存档及内存二进制没有转存。历史GREEN不能转记为当前源码已通过。

2026-10-30。代码与定向回归已完成，**修后包尚未重建和运行，当前case08不能记为已修复**。本记录只覆盖Sou的N_HOUDAN三等级与配套控制器，不代表Sou全部菜单或新增54武装实机通过。

## 缺陷与原生因果

BHE控制器把等级L放channel0、释放通知放channel3。通用转换只保留channel0，同时将PARAMCOUNT1..4折成BSDX单计数：主技能等待LASTTAMA计数>0，控制器P7的释放通知却变成绝对0；持有时等级升到正值还可能提前触发退出。

req784原生审计（历史路径`target/sou030-native-runtime-20261030/report.md`，已清空）与完整裸段（历史路径`target/sou030-native-runtime-20261030/native-repro.raw.log`，已清空）已证明同一轮的实际因果：

- tick231472，控制器5:526实际创建，随后block0→5；231473进入6，231474进入7。
- owner held0、direction=-1；P7的61032C实际消费work_value0，counter_after0。
- 231540，控制器在block7／clock65请求自然删除，父仍active30／block0／clock89；239910父仍悬浮Z95／G0，未READY。

这排除了“控制器未创建”，不能仅归为预览按住。LASTTAMA缺行不单独当作指针为零的证据；原生报告保留了该观察边界。主G0与原FF是等待协议的一部分，本修复没有强制END、改重力或写actor。

## 局部配对方案与保全边界

[SouHumanCannonCounterFixer](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouHumanCannonCounterFixer.java)在旧Sou等级／Material定制之后执行。按继承appendPlan定位最终Tama05宿主，在原源区间内按双名找到HEATBOMB06，再在同宿主尾部追加私有DX_SOU_HUMAN_CANNON_CONTROL。索引从实际宿主长度解析，不硬写526／539，不搬到Sou主WAZ。

私有P0显式清等级为0；P1..4原加1保留；P7**替换**唯一绝对零事件为0f一次相对+16，四整数(0,16,1,16)，不是绝对写16。P7无计数FF或回边，P5／P6也必须无计数事件／FF。配套三main的P0改为C>15，P1四个相等参数改16..19，原目的地及合法20的default保持：

| 原等级L | 未释放C／退出 | 释放后C／退出 | 原／新飞行entry |
| --- | --- | --- | --- |
| 0 | 0／否 | 16／是 | 6／6 |
| 1 | 1／否 | 17／是 | 5／5 |
| 2 | 2／否 | 18／是 | 4／4 |
| 3 | 3／否 | 19／是 | 3／3 |
| 4 | 4／否 | 20／是 | 2／2 |

三等级保留各自原时序，不能把Lv3窗口复用到其它等级：

| 等级 | Effect帧 | 主等待Change／FF |
| --- | --- | --- |
| N_HOUDAN01 | 41 | 42..134 |
| N_HOUDAN02 | 33 | 34..126 |
| N_HOUDAN03 | 21 | 22..114 |

原升力、SD、按键／WAZACOUNT条件、14／8 phase、精确suffix和65f回收均保持。测试最初过度复用Lv3窗口已按fresh三组literal纠正，未统一压缩源窗口。三个主技能只改控制器序号及五个比较参数。

全部关键签名、宿主计数、三caller与私有图先在独立候选上校验，再发布main／host／WazaGroup param；重复调用复核后保留对象身份。半成品、双名／suffix／条件／计数／FF漂移拒绝，不新增MEK，不改可见153边界、211行Material或AI，不扩大appendPlan的源base／count。已准备的公共原项及完整前缀、未配对caller、源DTO／文件均保全；通用计数转换器不改。

同宿主／输出与Material复核（历史路径`target/sou030-debug-20261030/mnk-host-integration-review.md`，已清空）确认原row30已覆盖完整显式闭包：child sprite5:450；Effect0:200／383／387／389对应sprite4:410、6:104、1:130、5:289；SE38:{254,345,631}，无Voice。同宿主深复制只改身份／计数，资源字段不变，无需新Material行或媒体。公共输出器从最终baseline写整份宿主，会覆盖继承文件；测试另核这一输出路径，未把单独WazGenerator等同整包交付。原字节定位见独立证据（历史路径`target/sou030-debug-20261030/independent-source-evidence.json`，已清空）。

## 回归与待验

[SouHumanCannonCounterTest](../../../src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouHumanCannonCounterTest.java)使用真实BHE输入与DX公共接入夹具，覆盖三等级×五档实际事件字段、原前缀／Material／菜单保全、同宿主输出和二进制回环、幂等身份及零额外写拒绝；这是协议／结构验证。

- 首轮[8项实际RED](../../../.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/red-sou-human-counter/TEST-com.giga.nexas.transfer.bhe2bsdx.meka.sou.SouHumanCannonCounterTest.xml)：8个AssertionFailure、0错误，缺私有尾行／未拒绝漂移；实现后[原8项GREEN](../../../.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/counter-green-camera-red/TEST-com.giga.nexas.transfer.bhe2bsdx.meka.sou.SouHumanCannonCounterTest.xml)。
- 独立复核发现P5／P6仍可偷偷写16，新增[第9项实际RED](../../../.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/camera-green-counter-domain-red/TEST-com.giga.nexas.transfer.bhe2bsdx.meka.sou.SouHumanCannonCounterTest.xml)：9项中1失败、0错误。补齐原型及私有图的空计数槽guard后[9项GREEN](../../../.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/green-sou-counter-camera-full/TEST-com.giga.nexas.transfer.bhe2bsdx.meka.sou.SouHumanCannonCounterTest.xml)。
- root最终26类定向实际结果：**206 tests／0 failures／0 errors／0 skips**，其中counter9项、camera12项均通过。日志为完整定向日志（历史路径`target/bhe-audit-20261026/sou-counter-camera-full-regression-20261030.log`，已清空），冻结[26类汇总与XML](../../../.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/green-sou-counter-camera-full/summary.json)；本记录另核26份XML总数一致。camera修复为独立范围，不把Sou30计数原因泛化为Sou33相机AV。

仍须重建新包，按最终私有身份重定位观察索引，验证正常菜单L0、按住／松开取得L1..4、技计数后备，以及受击／换招／死亡中断、自然对象清理与READY。旧case08的req784只证明缺陷因果，不能被源码或测试GREEN倒填为修后实机通过；全Sou0..152及关卡验收继续单列。

## 后续有限预览记录

上述待验段落描述本修复提交时状态。后来180550包默认L0菜单预览已在REQ33／READY62444闭合，Sou33也有独立预览闭合；见[sou-counter-camera-runtime-20261030.md](sou-counter-camera-runtime-20261030.md)。仍未完成其它蓄力档、正常战斗、受击／死亡／换招及全Sou验收；当前源码不能直接继承这两项历史结果。
