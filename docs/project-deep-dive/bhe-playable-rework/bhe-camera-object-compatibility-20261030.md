# BHE camera位置快照到DX对象目标的兼容适配

2026-10-30。共享camera转换已加入窄适配，camera定向12项测试与26类／206项回归均通过。**这只确认数据转换和回归边界；旧130704包的case08仍停在Sou33第一次访问冲突现场，包含本修正的新包尚未实机验收。** 本文编写只读取既有证据，没有执行Java、Maven、UI、CDB或恢复现场。

## Sou33现场与原字节

旧包训练菜单Sou33「カワラブレイク」对应`KAWARA03`，REQ787在tick240617 START，tick240637（+20）首次camera事件触发first-chance AV。异常地址／EIP为`0x00647F62`，ECX为0，ESI为`0x20626508`；异常码`0xc0000005`、parameters`(0,0)`表示读取地址0。Sou30计数等待是另一条已单独调查的路径，不用于解释此处camera解引用。

- [旧case08目录](C:/NeXAS_DX_runs/runtime/six-variants-validation-20261030-08)、[CDB记录](C:/NeXAS_DX_runs/runtime/six-variants-validation-20261030-08/runtime.cdb.log)。
- [Sou33 minidump](C:/NeXAS_DX_runs/runtime/six-variants-validation-20261030-08/evidence/sou033-firstchance.dmp)：105,062字节，SHA256 `85863f73917a1746e552850c50e08b407716b0b09ca47784f6f3d1d22c91365f`，线程421160。独立读取exception stream／x86 context与上述寄存器一致。
- [原字节与原生因果报告](D:/Code/NeXAS_DX/target/sou030-debug-20261030/mnk-sou033-camera-cause.md)、[新鲜Sou33 DTO与offset证据](D:/Code/NeXAS_DX/target/sou030-debug-20261030/fresh-dump-sou033)。该报告保留实施前状态，不回写旧证据。

root的只读堆记录显示：位置Work `0x3604BE40`的XYZ为`0x245000/0x1C4000/0x8000`，按1024缩放为2324／1808／32；对象字段`+0x1C`为0，camera的`+0x24` target也为0。位置已经求出，空值出现在“是否携带对象”这一结果字段。

BHE源KAWARA01／02／03的六条camera与旧130704转换结果的完整74字节逐条相同，unit和short标志没有丢失。KAWARA03 P0外窗20..43、内窗0..23，P1两窗均0..20；每条仅启用位置unit0，short0。P0源camera／inner offset为1117562／1117576，旧输出为1063876／1063890；首次启用恰为20帧，与AV时刻吻合。源`sou.waz` SHA256为`4a06693a9bb33127aa60aa04f2460fd44beadc08a7725876793820f5a23bfc43`，旧输出为`6b776b72842e51e14ab7f12a13accd9c95c918095e69b5cf7dcc3c935c4daae7`。

## BHE允许点值空target，DX camera要求对象

这类源位置表达式为`POS/OBJECT_VAL`：root20、typeList`[3]`、paramList空、intList3`[0,0]`（`OBJECTPOS/MINE`与`OBJECTPOS2/NONE`）、intList4空、int2为0。

DX的`64DB90`初始化CWorkPos并调用`669880`。其case3主动清除outObject和anchor，调用`668FD0`计算XYZ，却不保存后者返回的实体。`668FD0`在MINE选择器下使用当前所属实体；`64DBF0`仅持续更新模式0／1，模式3保持初始化快照。因此这里的NULL来自合法的点值结果形状，不能归因于自机选择失败。

DX camera执行函数`647F20`取得激活的位置Work，保存XYZ和`work[7]`对象字段，随后无条件通过target虚表`+0x138`调用。target为0时，实际在`647F62`访问零地址。short／flags的后续消费不能改变已经发生的解引用。原DX基线和旧130704 EXE在这条路径的六个相关函数局部代码逐字节相同。

BHE对应的`73EE70 case3`也清除对象字段并计算点值，但camera函数`75EB80`把NULL传给`490CD0`；后者先检查`if(a2)`，再访问对象虚方法。源引擎允许camera接收没有对象的点值。通用Term字典在两侧都认识OBJECT_VAL，语法重编译成功不代表camera消费者接受相同的结果形状。

原DX Kou提供合法对象模式对照：232条camera位置原字节已核实，229条typeList为`[1]`，3条为`[1,0]`，没有`[3]`。其中W65 `I_STRIKE03` P0的root20／`[1]`／aux`[0,0]`与当前接受的对象目标形状一致。

## 共享DTO中的精确适配范围

生产改动仅在现有[CEventCamera.transBheCEventCameraToBsdx](D:/Code/NeXAS_DX/src/main/java/com/giga/nexas/dto/bsdx/waz/wazfactory/wazinfoclass/obj/CEventCamera.java:147)内增加29行。followup主机体转换器、tsukuyomi主机体转换器及使用tsukuyomi转换器的公共资源入口均调用此方法；旧`converter/WazConverter`也复用它，无需增加平行转换架构。

位置unit0经工厂建立为DX CEventTerm type5。随后`BeanUtil.copyProperties`会临时把源BHE type7复制进目标，因此窄适配必须在现有代码恢复目标type5之后、把inner挂入目标unit之前执行。它只进入现有active unitSlot0路径，同时要求源为CEventTerm type7、目标为CEventTerm type5，且双方collection列表都恰有一项。源和目标的每项字段分别检查：

| 字段 | 必须满足的输入 | 输出 |
| --- | --- | --- |
| int1／root | 20（POS） | 保留20 |
| typeList | 精确`[3]`（OBJECT_VAL） | 新建独立可变列表`[1]`（OBJECT） |
| paramList | 非null空列表 | 保留 |
| intList3 | 精确`[0,0]`（MINE／NONE） | 保留 |
| intList4 | 非null空列表 | 保留 |
| int2 | 精确0 | 保留 |

检查源形状是必要的：现有InfoCollectionMapper会把null字段归一并只复制首项collection，不能让人工多项／null输入在归一后误命中已审范围。mapper及其既有输出行为没有修改，也不为这些近邻输入新增异常。mapper已创建独立collection和四个列表；本修正再次只替换目标typeList，源DTO、共享源列表和原资源文件保持隔离。

外部camera仍为DX slot66／type31，内层位置仍为type5。外／内帧窗、short标志、原MINE／NONE选择器、方向／距离／高度／flag单元均保留；普通非camera POS、其它root／mode、非MINE、anchor、额外参数／aux4、复合Term、多个collection、禁用／缺失／未识别slot0不触发该适配。没有修改EXE、删除camera、全局改写POS或增加MEK／WAZ／Material／group资源。后续`BheInfoCollectionObjectGraphRewriter`仍按type5的POSITION用途重编译，root20／OBJECT1与aux`[0,0]`保持已核含义。

**接受的语义取舍：** 将“初始化时捕获的自机位置快照”改为“携带自机对象并持续求值的位置模式”。它保留camera事件并满足DX消费者的对象要求，但不能称为严格复现BHE定点镜头。选择器没有被猜测或替换；其它POS快照仍保留原模式。

## 103文件／457候选原字节盘点

[完整盘点JSON](D:/Code/NeXAS_DX/target/sou030-debug-20261030/mnk-camera-adapter-inventory.json)记录103个逻辑平铺WAZ的路径／SHA256、457条候选的外／内offset与帧窗、short、完整collection及原字节终点。其SHA256为`8d0459c20a001e06eaebad6a16c827db608b05ec88ac1ceb8632f7c3b01106e1`；[范围与接入点报告](D:/Code/NeXAS_DX/target/sou030-debug-20261030/mnk-camera-adapter-scope.md)保留逐项表格。

缓存JSON仅用于候选定位；457条位置相机全部回读真实BHE原二进制，核对外窗／short／active位置buffer、内窗及collection全部字段。113份物理文件由103份平铺源和10份角色目录副本组成，10份副本均与对应平铺源逐字节一致。此方法是缓存辅助的原字节候选验证，未宣称重新实现完整83槽WAZ解析器或独立重新枚举全部事件。

精确规则命中19条，全部来自Sou：

| 技能身份 | 位置 | 条数 | short |
| --- | --- | ---: | --- |
| KAWARA01／02／03 | 每级P0、P1 | 6 | 0 |
| KOUMORI01／02／03 | 每级P1 | 3 | 1 |
| JUSOUTOU01／02／03 | 每级P1 | 3 | 1 |
| KISS01／02／03 | 每级P0、P1 | 6 | 1 |
| EX_KAISERKICK | P2 | 1 | 0 |

Sou共有319条位置相机，匹配19条（short0共7条、short1共12条）。其它源WAZ没有匹配。八个公共WAZ中，tama01／tama04／tama05分别有6／9／4条位置相机，其余五份为0；19条公共位置相机均不匹配。除本次KAWARA03现场外，其它命中点属于同消费者的兼容范围，没有逐技能AV复现结论。

## RED、GREEN和独立复核

相机测试只调用已存在的public DTO方法、followup／tsukuyomi实际转换器和实际common wrapper，没有引用未来helper。所有生成回环写JUnit临时目录，原输入和共享JSON不写。

| 验证 | 结果与证据 |
| --- | --- |
| camera修正前RED | camera共12项，6失败／0错误／0跳过；六条正例均为期望`[1]`、实际`[3]`或对应模式词字节差。同期counter八项全绿，合计20项／6失败。见[真实RED日志](D:/Code/NeXAS_DX/target/bhe-audit-20261026/sou-counter-green-camera-red-20261030.log)。 |
| camera修正后GREEN | camera 12项／0失败／0错误／0跳过，见[冻结camera XML](D:/Code/NeXAS_DX/.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/green-sou-counter-camera-full/TEST-com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.wazconvert.CEventCameraCompatibilityTest.xml)。覆盖类型恢复顺序、共享源隔离、所有其它camera单元、近邻输入、两条主转换路径、common wrapper、TermRewrite、事件字节差、Sou全部319条camera中19条模式词差、BHE整Sou回环、原DX Kou232条对象camera及八份真实common原样保全。 |
| root完整定向回归 | 26类／206项／0失败／0错误／0跳过；[执行日志](D:/Code/NeXAS_DX/target/bhe-audit-20261026/sou-counter-camera-full-regression-20261030.log)、[冻结summary.json及同目录26份XML](D:/Code/NeXAS_DX/.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/green-sou-counter-camera-full/summary.json)。本文只读逐份合计冻结XML，结果与summary一致；其中camera12项保持全绿。 |
| Astra独立最终复核 | [report.md](D:/Code/NeXAS_DX/target/sou-counter-camera-followup-review-20261030-astra6-ultra-174801/report.md)／[audit.json](D:/Code/NeXAS_DX/target/sou-counter-camera-followup-review-20261030-astra6-ultra-174801/audit.json)。完整读取camera 29行改动、12项测试及helpers，未发现新增可操作问题；直接读回counter9项与camera12项GREEN XML，并独立重新核457条候选原字节及24份含候选源文件哈希，19条匹配与测试独立身份集一致。Astra没有运行Maven，也未独立汇总root的26类回归。 |

审查时camera生产文件SHA256为`8dfec0a7242c0181f49839a87e93e400995d4d6875eaccab6189924a378d18e0`，测试文件为`acb7f2df4967669946e4616b654aa9dfd714d219c06eb0945babf1b00a09a2d7`。Astra同一轮还关闭了独立Sou计数修正的P5／P6域守卫发现；这不改变本相机修正的作用范围。

## 运行验收边界

旧case08使用的是130704 EXE／PAC：EXE SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`，PAC SHA256 `86296c31b693a6260f2d73bf38dfc1d3e44217a8e0261ee5a4a3e0ac7da8f672`。本次文档读取其sidecar仍为`runtime_acceptance=not_evaluated`；root确认CDB停在Sou33 first-chance AV，未把该旧进程恢复为修复后的测试。

root后续分笔提交、重建并新开case后，仍需复测KAWARA03首次+20相机及P1后续相机、正常收招与镜头恢复，并检查接受的live对象镜头效果。当前没有新包实机结果，不能据GREEN或源码复核宣布camera运行根治、Sou153项菜单全部通过或九角色54追加武装玩法验收完成。
