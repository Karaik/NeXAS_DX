# Naoto六个追加武装：静态核查与待实测清单（2026-11-06）

本文件创建时只固化源码、相关测试、已有审计和当前成品的只读核查。下面的输入安排均为候选，未预填case13实测结果，没有将静态通过或旧包连段继承为当前包玩法通过。本次整理没有修改源码／测试、构建、提交或操作游戏；整包runtimeValidated仍为false。

root报告已由正常UI在`nine-naoto-20261106-13`选中Naoto并进入装备菜单，承袭私有Save的当前普通装备只有近距A=0，其余普通槽空，即将逐招装备及正常保存模板。这是本次交接提供的UI初态，不是本文件作者另行操作或读取UI的结论。尤其不能把静态默认14槽当成当前Save实际出装。

## 当前资源身份与索引

绑定`run_20261105_193350_342_bd794139`，最终资产目录为该run下`sou/sou_assets_20261105_193550_582`，PAC SHA256为`8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641`。本次直接重算两份最终二进制：

| 文件 | 字节数 | SHA256 |
| --- | --- | --- |
| Naoto.mek | 70225 | 06fa2864fa3fb6d8cebd4f56d24de43abdebe93f5a30bf300c7ed2694f1c1624 |
| naoto.waz | 178117 | 5f874345756fa6bf172aae7a950ff1ed873715acc70efa64f7b5c157126779d5 |

两hash与保存在`src/test/resources/bhe-runtime-evidence-20261030/records/audits/nine-six-variants-130704-v2/audit.json.json`的文本包装内审计一致；已解析其text字段核对Naoto角色和roundTrips行。该审计给出Meka109、主WazaGroup116、26个MEK、38个WAZ。这里使用的是当前成品字节同一性的静态证据，不是跨包继承实测。

| MEK | 名称／英文身份 | 主WAZ | 内部依赖 | 固定热耗字段 |
| --- | --- | --- | --- | --- |
| 20 | ワンツー・コンファーム／DX_HIT_CONFIRM_ONE_TWO | 116/31 | 原Straight116/12 | 20 |
| 21 | フック・リターン／DX_HOOK_RETURN | 116/32 | 无新增私有辅助 | 22 |
| 22 | クロー・起動突き／DX_CLAW_DEPLOY_RISE | 116/34 | 私有Rise116/33→原収納116/18 | 26 |
| 23 | ダウンネイル・収納／DX_DOWN_NAIL_RETRACT | 116/35 | 原収納116/18 | 24 |
| 24 | マシンガン・三点射／DX_MACHINEGUN_THREE_BURST | 116/36 | 公共弹体及口焰 | 12 |
| 25 | 炸裂弾・交差連射／DX_EXPLOSIVE_STAGGER | 116/37 | 公共炸裂弹及口焰 | 32 |

热耗列是MEK字段，不能直接当RPM raw heat数。W33是内部辅助，不是第七个追加武装，不应产生额外装备START或额外装备槽热耗。

## 20：现有命中确认一二拳

[NaotoCustomizer.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/naoto/NaotoCustomizer.java)第24～76行：W31的11～16f Hit确认窗口插入攻击命中条件`types[2,6,0,0], param[0], aux[0,0]`，成立跳本角色原Straight W12；37f无条件落空出口保留。W12的17～30f Hit和57f正常结束保持原载荷。内部W31→W12仍属同一个weapon20，不能当第三武装。

旧[Naoto连段记录](naoto-combo-runtime-20261029.md)的0→20→1，普通输入30／44／78，是优先对照；旧实际四Hit为41／55／73／95、共137，后继控制均正。但该记录绑定旧PAC，当前case13须重新证明三次独立START及20内部完整两击，另验落空不派生。不能只看到W12就误认已经启动装备1。

20本轮没有额外物理修正；稳定地面收尾应独立读active=-1、Z0、Vz0、Graw51200及jump/fall100，不能借用旧记录的终态。不同距离、移动／空中靶及取消／受击中断仍待新证据。

## 21：命中取消与打空后退分开

[NaotoAdditionalVariants.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/naoto/NaotoAdditionalVariants.java)第64～72行保留34～36f的100基础Hit及原命中普通取消。40～47f新增绝对恒-12速度，48f速度0并新增普通门1，70f正常结束；没有早SD门、无敌或追踪补丁。

34f、即第一次Hit可能允许取消前，恢复G50及slot27/28倍率100；原准备重力倍率20保留到此，63～70f的恢复改成绝对恒100。测试在[NaotoSixVariantsTest.java](../../../src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/naoto/NaotoSixVariantsTest.java)第150～163行直接核恒速、停速、恢复及打空门。

先A30单招出完，实读40～47后退和48停速，再测墙边／反向。若真实Hit发生，可在H+2／H+6／H+10尝试Jab0；这些是控制窗口候选，不是已证真连。打空后改用A30、B80的候选，目标是晚于clock48的普通门；实际START和clock仍需核。若在Hit后、后退开始前取消，该轮不能计作后退效果已验证。

## 22：真实快路径是目标ACTION_NOKEALL

该招主图W34保留两个phase，P0 67f、P1 53f均跳私有Rise W33。代码第87行的局部变量名ground／airborne，以及旧规格中“自身空中／已经展开”的概括，不能作为真实分支语义。

本次直接读当前`naoto.waz`，所列offset为文件绝对偏移、32位小端序：

| offset | 原字或结构 | 含义 |
| --- | --- | --- |
| 155566 | [0,0,1] | P0 Change的start0／end0／flag1 |
| 155578 | [0,3,2,1,10,0,2,1,0,0,0] | 条件root0，types[2,1,10]，param[]，aux1[1,0]，aux2[]，末int2=0 |
| 155622 | [16,2,0,0,0,0,0,0,0] | 目的root16／types[0,0]无参数，加事件末int1=0：NEXT |
| 155698 | [16,1,1,2,116,33,0,0,0] | P0末尾目的为JUMP116/33 |
| 157574 | [16,1,1,2,116,33,0,0,0] | P1末尾目的为JUMP116/33 |

真实源W16的条件是BHE `types[2,1,12], aux[1,0]`；源term字典说明为OBJECT3/ACTION_NOKEALL。DX重编译为index10，aux[1,0]选目标ROCK，不是self。原生[`66B6E0.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/66B6E0.c)第65～69行的case10调用[`61B100.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/61B100.c)；后者第13～19行检查虚函数条件、state7/8/9以及两个受击相关字段。因此0f目标满足受击总类条件时NEXT进入P1，否则沿P0执行。自身Z高低不构成这条条件的证明。

主展开P0 clock44／P1 clock30保留普通门，提前换招会跳过尚未执行的Rise，这是原有选择；不能据此判“自动上挑失效”。W33保留18～22、23～33两段Hit，只将第二段打开普通命中取消；23f恢复G50及两倍率100，45f原G100改G50。71f出口跳原収納W18，其23f正常结束。出处为AdditionalVariants第74～91行、SixVariantsTest第165～175行。

首先A30单招不追加输入，观察W34→W33→W18及自然收尾。分别对静止目标与正在受击的目标测试P0/P1，不以“站立／空中”代替目标条件。然后在W33第二Hit后的H+2尝试23或Jab0，先核真实Hit和普通门，不预设一个通用绝对B帧。主展开44／30的提前取消另测，不能借用后面W33的23f恢复为前面取消点担保。全部链自然出完后应已収納、没有停留旧ClawStand；再检查地面物理和对象回收。

## 23：重击取消与収納

AdditionalVariants第93～105行保留31～35f轻Hit和36～37f重Hit，仅重Hit新增普通取消1；36f恢复G50及slot27/28倍率100。原71～87倍率事件改为36f恒100点，87f旧ClawStand出口改为55f跳収納W18，并压缩后续sprite等待；31f原爆点等Effect保留。SixVariantsTest第177～183行覆盖这些合同。

先独立装备A30完整出完，核两段Hit、W35→W18及収納结束；未先展开时外观与攻击页也需实际验证。若36f重Hit确实发生，A30／B68可作接Jab的初始候选；若重Hit未发生，就不预期该时刻命中门成立。保留原28～35f向下运动，36f恢复重力不等于立即强制Vz0；应在自然落地后的稳定终态读Vz0。

## 24：完整三点射

AdditionalVariants第107～127行：P0在35f NEXT；P1移除原按键／随机／计数组合循环，只保12f NEXT进P2；原完整“弹体＋两口焰”批次复制到P1 clock0／4／8，普通门从0移9；P2在33f结束。P1九条Effect是三批组合，不能记成九发弹。每批保各自完整子载荷和数量1。

P0的0～13f倍率改绝对恒100，P1/P2入口各恢复G50及两倍率100，P2的21～33恢复也改恒100。SixVariantsTest第185～194行核批次、无按住循环和物理恢复。保留的通用scalar不是剩余三发计数，不能按其数值推发数。

先A30短按单招自然结束，再长按／松开确认最多三批；随后A30／B76是晚于预期P1 clock9的切换候选。以实际P1 clock和第三批已发射为准，不能只凭录像计数认定。射击后的Hit取决于飞行时间和距离；24→21可作为压制衔接候选，但同靶连续控制必须另证。父结束后公共弹体／口焰可以按自身寿命继续，最终TAMA/SCRIPT应归零。

## 25：左右两拍

AdditionalVariants第129～138行保留P0 33f准备，P1左组0f、右组弹体和对应口焰一同移到8f；两组各自原发射口位置和速度不变。普通门2→9f，P1出口39→24f，P2原34f结束。原P1 1～4f速度段保留，没有主动重瞄或新物理恢复代码。SixVariantsTest第196～200行与相关完整Effect比较核双批及其保全。

先A30单招证明左右真实两次发射及对应口焰、批次间差8个本图clock；再以A30／B74作P1 clock9后的切换候选，实际仍须核START／clock。不能在右发之前被中断后补算第二发；正常出完及取消后的物理状态都独立读取，不能套24的恢复证明。25→21只是压制候选，弹体实际Hit是否足以覆盖Hook34f待同靶控制日志确认。

## 正常装备和候选输入的共同边界

以当前正常UI真实出装生成新模板，每次先核14个装备值。可将目标新招装A近距、Jab0装B普通Dash槽、Straight1装C普通Dash槽，其余普通槽空；这是沿旧模板同按钮向后回退的候选配置，仍需当前START确认实际选招，不能按静态默认表假定。使用普通held/edge输入，不写actor、请求武装号或强开取消门。

所有首轮建议A[30,34)并留足长尾；后继B候选可同样保持4帧。21的80、23的68、24的76、25的74只是从已知起手与本图时刻推得的试验档位，Hit停顿、分支和实际调度可能改变绝对帧。22必须先观察私有Rise的实际第二Hit，再定后继输入。不要将同一绝对日程机械套到全部新招。

每项需分别记录：当前包／模板身份、实际START／USE及武装归属、主图与内部辅助entry、真实伤害和目标控制、普通取消、自然结束、落空、受击中断、再次使用及稳定终态。跨武装真连必须是不同装备START且同一目标期间没有恢复，内部WAZ跳转、HUD连击数或成功按出下一招均不充分。

稳定地面收尾候选指标为active=-1、Z0、Vz0、Graw51200（G50）、jump/fall100及TAMA/SCRIPT0；使用期间的正常空中／下降状态不要求提前等于地面终态。没有代码依据要求每个普通scalar均清0。缺图片继续nonfatal审计；无Hit与崩溃／对象泄漏分开判定。

## 2026-11-06 首轮菜单预览实测

root通过正常菜单依次hover20～25；本节作者只读case日志及身份清单，没有操作UI、游戏或调试器。全部事件属于当前8efe包、`nine-naoto-20261106-13`、玩家meka109。它们是菜单预览的实际伤害／释放观察，不证明已经装备、训练使用、真连、同场复用或全部条件安全；上方静态段和候选日程保持原文。

证据为[`naoto-six-menu-first.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto-six-menu-first.json)，128185字节，SHA256 `4320bd224b10d9e88150a50181de9968ece7c25c72b369262aaa948842c6ce54`。完整日志前缀冻结至line1860、997582字节，SHA256 `8a3c10f3c1ff82bb1d657d38e2217cf48428f0055b0e7e5cdfe4650374a271c2`；末条为25下一周期START seq107／tick42570，只用于首周期闭界及计数快照，不计另一项验收。运行日志继续增长，后续训练不纳入本证据。

观察器的menuRequest、matched START及menuReady分支只接受Sou113；这些Naoto事件的`requestSeq=null`，没有Sou请求五链或native READY，也没有可读取的variant字段。每项从该武装首次通用START起，到下一通用START之前划窗，保留窗口所有原生事件及相邻前周期上下文。每个首START与下一START都为TAMA0／SCRIPT0、heat0；下一START是菜单自动重复，不能据此判同场正常再用。

| MEK | 首START seq／tick；原日志行窗 | 实际敌方伤害／末HP | 父释放入口：tick、group/skill、block/clock | 释放时TAMA/SCRIPT、heat raw | 敌恢复／玩家RESET | 下一START闭界 |
| --- | --- | --- | --- | --- | --- | --- |
| 20 | 1／23618；448～461 | 30+45=75；5925 | 23696，116/12，0/57 | 0/0，2000 | 恢复23651与23673；RESET23726，active-1、heat2000 | seq2／23748 |
| 21 | 18／25810；686～694 | 150；5850 | 25903，116/32，0/70 | 0/0，2200 | 恢复25898；RESET25933，active-1、heat2200 | seq19／25955 |
| 22 | 41／29053；889～908 | 45×5+90=315；5685 | 29237，116/18，0/23 | 0/0，0 | 恢复29279；RESET29280，active-1、heat0 | seq42／29331 |
| 23 | 57／33487；1209～1225 | 37×3+150=261；5739 | 33605，116/18，0/23 | 0/0，2400 | 恢复33698；RESET33699，active-1、heat2400 | seq58／33750 |
| 24 | 75／38126；1511～1520 | 7+6+6=19；5981 | 38209，116/36，2/33 | 0/0，1400 | 未见敌恢复；RESET38239，active-1、heat1400 | seq76／38261 |
| 25 | 106／42296；1821～1859 | 15×12+22+20+19+18+17+16=292；5708 | 42390，116/37，2/34 | **3/0**，3200 | **未见敌恢复或combat_reset** | seq107／42570，TAMA0/SCRIPT0、heat0 |

每个窗口伤害都来自一个敌actor，按首击推回HP6000，再逐击扣至表中末HP；没有玩家伤害事件。20的敌actor为`0x230ed010`，21为`0x23be0020`，22／23为`0x22a68028`，24／25为`0x21691578`。相同地址跨不同预览周期可能复用，不等于同一训练生命周期。伤害事件没有逐Hit攻击源／弹体owner字段，因此这里只认定当前孤立预览窗口内的实效与HP算术，不补造弹体归属。

20在tick23651先出现line453敌恢复，后出现同tick的line455第二拳伤害。两拳都命中，但该预览不证明敌人始终没有恢复，更不是0→20→1的跨武装真连。释放时skill12、active20只证明内部Straight属于weapon20；没有额外装备1 START。

22的首轮严格为seq41、315伤害。root在后续自动重复时看到的UI445不混入首轮，也没有为445补造本次事件。116/18释放支持该轮走到収納释放入口，但日志没有完整逐phase转换或物理倍率采样，不能凭最终skill18补证W34目标条件的两条分支和所有W33过程。

25在42390父释放时仍有3个TAMA；前12击各15在释放前，之后6击共112继续发生到42492，合292。只能记录父释放与后续弹体效果，不能说父释放即全部对象回收。42570下一自动周期的零对象快照支持那一较晚时点TAMA/SCRIPT已归零；没有捕获恰好归零的tick、该首周期RESET或空闲READY。24、25伤害记录均未给出敌恢复事件，不能将“没有恢复日志”单独当连续控制证明。

## 前一周期隔离与剩余边界

相邻前周期原始事件在JSON中单独保留，既不计入当前伤害，也不填补当前缺项。尤其22之前是21 seq40／tick29000；它在29034造成150伤害，至22开始前没有该周期release或RESET。更早seq39的release28948不能代替seq40收尾。该150位于首22窗口外，敌actor由`0x23be0020`换成`0x22a68028`，首22起点TAMA/SCRIPT为0。24之前的23 seq74有38076释放，但截至切到24没有该周期RESET／敌恢复；新24敌actor也已更换。未据菜单换项宣称这些前周期自然完整闭环。

六个所选首轮窗口没有异常事件；完整冻结前缀line425有1条system／unclassified KERNELBASE异常，原对象完整保存，不自行归为无害，也不称整会话零异常。EXE/PAC哈希来自case启动manifest，本节没有重新hash大型二进制；证据捕获时state仍running，`runtime_acceptance=not_evaluated`，不预写自然退出或原Save终态保全。

当前可登记的是六招首轮菜单预览实际伤害、各自父释放入口、20～24各自RESET及25较晚下一周期的零对象快照。普通取消／SD取消、受击中断、落空、实际装备和训练、真实跨武装连接、同场复用、稳定物理终态、child完整生命周期及更多目标条件仍未完成；24的三次伤害不替代完整生成批次观察，25的18次伤害不等于18发弹或证明左右发射差8clock。静态默认14槽没有改写成当前Save实际装备，整包`runtimeValidated=false`继续保持。

## 2026-11-06 Replay016：20的当前包真连与同场复用

root在同一case13中正常UI保存015模板，按正常LOAD／PLAY播放016并观察STOP15秒。本节只读独立核验录像、输入、receipt、日志及root保存的最终快照，没有操作游戏或更改输入。当前固定甲靶／地图68上，0→20→1两轮均为四击137，且20之后的普通取消与同场再起成立；这项训练结果与上节菜单预览的75伤害及恢复间隙分别记账。

固定证据[`naoto20-combo-repeat.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto20-combo-repeat.json)为57841字节，SHA256 `e5ff61f354418c62d1d6552eb4c643e9bcb72b726d2bf16e36e976c4b56e2039`。原生窗口只取本会话首个`combat_replay_input` frame0／tick111544（line4493）至第二轮RESET frame586／tick112130（line4563）；完整前缀2566965字节、SHA256 `e5e66b85333425e7301af34867d09ad3539a13916eaa851ae47bef5cb086d374`。71条窗口原始对象全部保留，没有混入此前菜单自动重复；最终tick114655快照另列，不回填成原生窗口终点。

015为858字节、SHA256 `3ea35245989a8ac42bb97e48b84c14a21a74952a3f258c6bbd3cd0e93c2e9e03`，1958帧全零。016为876字节、SHA256 `c26b9ae608e9a2e037b7922f23a6c9b22166ff2bfde3a7983d4be52f52b325e0`，900帧。独立解析三块、各zlib EOF及整文件EOF；头部与前两编码块812字节完全相同，SHA256 `d50685f23089e8b3cb96a011da6390048ae1f4c2dedbe23bae06dccef656b07a`。第三块7200字节的900组edge／held逐帧与六段普通输入相符：A[30,34)、B[44,48)、C[78,82)，以及A[450,454)、B[464,468)、C[498,502)；tail240，六个起始edge以外没有额外攻击或方向输入。

实际模板读回玩家109、敌0、map68，14槽为`[0,-1,-1,-1,-1,-1,-1,-1,-1,-1,20,1,-1,-1]`，即近A0、普通Dash B20／C1，其余空。这是当前正常UI模板的配置，不是静态默认14槽。原生USE的slotRaw0／7／11属于另一排列，不能把它误写成DAT槽0／7／11。

| 项目 | 首轮 | 同一播放第二轮 |
| --- | --- | --- |
| 普通输入及外部START 0→20→1 | frame30／44／78，seq256／257／258 | frame450／464／498，seq259／260／261 |
| 首招0命中 | frame41，30；HP5970 | frame461，30；HP5833 |
| 20内部两击 | frame55／73，27+40；HP5943→5903 | frame475／493，27+40；HP5806→5766 |
| 外部1命中 | frame95，40；HP5863 | frame515，40；HP5726 |
| 三次后继命中前control | 3／4／5 | 3／4／5 |
| 目标真正恢复 | frame123／tick111667，全部四击之后 | frame543／tick112087，全部四击之后 |
| 外部1正常释放入口 | frame136／tick111680，W12 clock57，TAMA0/SCRIPT0 | frame556／tick112100，W12 clock57，TAMA0/SCRIPT0 |
| RESET | frame166／tick111710，used3/chain3、heat3200 | frame586／tick112130，used3/chain3、heat3200 |

同一播放的玩家actor为`0x21bc6018`、敌为`0x235da018`、manager为`0x2405dd08`。八次敌伤害HP连续6000→5863→5726，合274；每轮首末Hit之间没有目标恢复或玩家RESET，后继受击前control均为正。第二轮首START前used/chain重新0/0，heat0、TAMA0/SCRIPT0，说明是在正常恢复后再次起手。两轮是两条各137的真连，不合称一条不断的274长连，也不是两个独立seed。

20在每轮外部START20和START1之间完成27、40两次伤害，中间没有额外START／USE；之后释放记录仍active20、skill12。因此内部第二拳仍属于20，真正的装备1直到frame78／498才启动，不能把内部Straight当作第四件武装。START事件的skill字段可能仍是切换前状态，不用其旧值冒充完整逐phase轨迹。

四处普通换招为0→20的frame44／464和20→1的frame78／498，释放入口normalGate1、sdGate0，与同tick普通B／C输入及后继START匹配。**两次20→1释放入口及其后同tick START均记录SCRIPT4**；其它四个release／cancel事件为SCRIPT0，后续外部Straight释放与最终快照为TAMA0/SCRIPT0。这里没有全程对象计数采样，不声称全过程0，也不把其它菜单时点凑成第三次训练取消。RESET时heat仍3200，不能用稍后heat0覆盖原生RESET读数。

root的只读最终快照为`target/bhe-playable-runtime/nine-naoto-20261106-13/naoto20-combo-repeat-final-snapshot.json`，958字节，SHA256 `eaaf90fd90cac689c8700003eceade62b5f245157529c95c6fb1473d3d8bf5e2`。其PID9552、创建FILETIME及image与本case一致，tick=endTick=114655、tickStable=true；replayCounter900／inactive、heat0／capacity8000、active-1、skill0、TAMA0/SCRIPT0、Z0/Vz0、Graw51200（G50）、jump/fall100、armorWaza0、armorContributionCache0、protection0。它支持该较晚观测时点的地面／热量／对象／装甲收尾，不是native READY或恰好录像结束的tick；counter900也不冒称新增原生输入事件frame900。

本窗口没有异常或观察器错误；此前完整前缀中的1条system／unclassified异常仍保留，不称整会话零异常。case在证据捕获时仍running、runtime_acceptance未评估，后续正常退出与源Save终态另行验收。此前首轮菜单JSON及其冻结前缀均再次核hash不变。

本次新增认可仅限20在上述固定目标、模板和日程的实际两击、普通取消、真实0→20→1连接、同场两轮复用及所述收尾。20落空不派生、SD取消、受击中断、其它目标／距离／seed仍未验；另外五个新增武装的训练连接／取消／复用也没有由这次结果代替。上节六招菜单实效仍按其自身边界保留，九角色54招、Sou153项及整体目标继续未完成，`runtimeValidated=false`。

## 2026-11-06 Naoto21：018自然动作、019失败后继、020单轮真连

本节分别固定三次正常训练播放，均来自root由正常UI保存的017模板和同一8efe包／case13。018证明自然动作，019保留失败连接，020证明单轮21→Straight1真连；三者不互相填补缺失事件。021的同场复用另行记录，本节不提前记通过。

证据[`naoto21-natural-and-links.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto21-natural-and-links.json)为110350字节，SHA256 `5588e2fffd4bd4e7c826f0c228dd3f5b4f96c1fbd1742a74ea2918ba33768e3b`。事件总前缀冻结至line7201、4135554字节，SHA256 `4611b9c84bffc17e8803112ee46c253ecda483dae601e0906d22c89af5ca1929`；三试验各自从首次实际发出的frame30输入到本轮RESET划窗，窗内完整原始对象全部保留，另保留对应Replay载入和前缀异常。日志随后仍可增长，不纳入后续021。

017为865字节，SHA256 `2045e08183e8008de87a3b12ffb1c4ae439cc87c96ae5c7f8af74f235aa6db5c`，2911帧全零。三个Replay都独立解析header、三块zlib及EOF，并逐对核360帧edge／held与各自输入JSON一致，tail200；头部和前两编码块812字节均与017相同，SHA256 `bb7a49f451a1c7c307b41f3e07e169f08032d0247843490df676fa206887e202`。实际109／敌0／map68，14槽`[21,-1,-1,-1,-1,-1,-1,-1,-1,-1,0,1,-1,-1]`，即近A21、普通Dash B0／C1，其余空。

| 播放 | 普通输入，左闭右开 | Replay字节／SHA256 | 实际范围 |
| --- | --- | --- | --- |
| 018 | A[30,34) | 854；`29b7a70b75fab04482d5db65fcb79d110dc5ce54599b3021a675865a55053c3b` | 21单招150及完整自然回撤／停止 |
| 019 | A[30,34)、B[66,70)、C[82,86) | 864；`d67770252f879b78e2074a104b608bc7d069465e19959a72ecaa57a69d43b8d8` | 普通取消到Jab成立，Jab无伤害；C未启动1，候选真连失败 |
| 020 | A[30,34)、C[66,70) | 858；`1550639a1268625a1ded4b686d57d5ac6e8ffcaabf65914bedaaaef7f2d9ab8a` | 固定目标单轮21→1，两击150+40=190真连 |

三轮没有另行发出的native frame0输入事件；本节保留实际最早的frame30，不以RPM counter0补造原生frame0。RPM每轮均保留在原件的361个counter0～360，证据只复制关键变化行和全部拒读对象，并给出完整原件hash。counter是采样读数，不作为原生输入帧或同tick执行顺序的替代。

| 播放 | 首START | 伤害 | 父释放／后继 | 目标恢复／RESET | RPM原件及拒读 |
| --- | --- | --- | --- | --- | --- |
| 018 | seq533，frame30／tick263139 | frame64／263173，150，HP5850 | 21在frame103／263212以W32 clock70释放，heat2200、TAMA0/SCRIPT0 | frame98／263207；RESET133／263242 | 295229字节，SHA`925a96505eead1213772c318bb84aa46572d7bb88f2b2083ee28daf621b2ae15`；22次=3未初始化+19一致性变化 |
| 019 | seq534，frame30／tick299052 | 只有frame64／299086的150，HP5850 | B66后frame67／299089取消W32 clock34，START0 seq535；Jab W11 clock37在frame105／299127释放；未START1 | frame98／299120；RESET135／299157、heat2700 | 297199字节，SHA`521f2ccd1b24b9281852c1b2649815f6463e3f96626ea1a49e8e1d8776d1b583`；47次=7未初始化+40一致性变化 |
| 020 | seq536，frame30／tick337902 | 150在frame64／337936；40在frame91／337963，末HP5810 | C66后frame67／337939取消W32 clock34，START1 seq537；W12 clock57在frame125／337997释放 | frame119／337991；RESET155／338027、heat2900 | 369667字节，SHA`9aa80f76dbf2b83e718fb4a84c8414694613a2a5ee4bcbba1522d1b0e242c0ba`；39次=6未初始化+33一致性变化 |

“完整361个counter”不抹去22／47／39次拒读，也不使RPM变成原子快照。以上RESET热量按原生事件保留；各自counter360的稍后尾部才为inactive、active-1、热0、TAMA0/SCRIPT0、Z0/Vz0、Graw51200、jump/fall100、装甲贡献／保护0。三份尾部不能互换，也不等于游戏进程已经退出。

### 018的实际回撤与单位

actor的A0/A4/A8是整数地图XYZ；向量管理器指针在actor+14A4，其+1C/+20位置为raw/1024。actor+1758是技能速度分量raw/1024，+1744是方向度×1024；它们不是所有运动合成后的净速度。本次回撤结论主要取实际X变化，并同时核raw位置与技能字段。

起始X2302；准备段前进至X2338。clock39／counter72／tick263181仍为X2338；clock40～47／counter73～80／tick263182～263189依次X2326、2314、2302、2290、2278、2266、2254、2242，每步-12，回撤段合-96。该段skillSpeedRaw=-12288、skillDirectionRaw=-92160。clock48／counter81／tick263190速度字段归0且X保持2242，后续到counter360保持此X。因此**回撤段96，但整招起终净X为-60**，不能混写为整招后退96。

clock33样本fallModifier53；clock34在counter65～67均为100，之后该招样本保持100。命中停顿使几个不同counter对应同一clock，不能按counter差替代技能clock差。自然动作中SCRIPT2／4曾出现，clock48样本归0；没有全过程对象数为0的结论。

### 019失败与020成功的区别

019的B在frame66输入，实际frame67才正常取消21并启动Jab0，取消时W32 clock34、normalGate1、sdGate0。Jab未造成伤害；C82及后续held变化虽有原生输入事件，却没有装备1 START。RPM在counter82附近为Jab clock14、normalGate0，与“C没有成功接出”相符，但不据此补造未观察到的几何根因。原始失败输入、事件及采样都保留，不能以020成功将019重标为通过。

020的后继Straight在frame91造成40，**同一敌actor`0x235da018`的原生命中前control6**，HP5850→5810；两次Hit之间没有敌恢复或玩家RESET，目标直到frame119才恢复，支持本固定条件的两武装真连190。该轮在21 clock34便取消，尚未到回撤clock40，不能用它独立证明完整后退；后退效果来自018。

020采样的target读自player+14B8默认目标，保留实际actor／meka／XY／HP／control，不与其它actor或预览指针混用。同tick337963的RPM counter91仍见HP5850/control7，而原生伤害事件已为HP5810/precontrol6；下一RPM counter92才见HP5810/control25。这些是采样和本tick更新先后的真实差异，不用RPM覆盖原生伤害／控制值。换入Jab或Straight后出现各自原准备倍率，也不直接归为21恢复失败。

三原生窗口无异常或观察器错误，冻结前缀中1条system／unclassified异常继续保留，不声称整会话零异常或自行归为无害。来源EXE/PAC仍使用启动manifest身份，本节没有重hash大型二进制、操作UI或修改游戏数据。21目前已证自然实效／回撤及020单轮普通取消真连；同场复用等021独立证据，落空、SD取消、受击中断、墙边／其它方向距离和目标仍未验。其它武装与整体目标不由这三项扩大，`runtimeValidated=false`保持。

## 2026-11-06 Replay021：21→Straight的同场两轮复用

本节单独追加021；018自然回撤、019失败后继及020单轮真连的JSON原件仍为110350字节／SHA256 `5588e2fffd4bd4e7c826f0c228dd3f5b4f96c1fbd1742a74ea2918ba33768e3b`，其冻结日志前缀也已复核不变。没有将021第二轮回填到前三个窗口。

新增[`naoto21-straight-repeat.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto21-straight-repeat.json)为71100字节，SHA256 `313db40f7b5eea7d153a9e2378e374b270d74f86181f66ed263dd3c928894dd9`。021为870字节、SHA256 `14f620669843e10e5b6c86bd4ab444aac4eaf519e883339956336b7eb6d33dea`，900帧／tail300。独立核三块与EOF，头部及前两编码块812字节与017完全相同；全部900组edge／held匹配A[30,34)、C[66,70)、A[450,454)、C[486,490)。实际模板仍109／敌0／map68、近A21及普通Dash B0／C1，其余空，本轮未使用B。

原生窗口line7234～7273，共40条完整原对象，从实际frame30输入开始，到第二轮RESET frame575／tick378655结束；没有另发frame0事件，不由RPM补造。完整前缀4169701字节、SHA256 `ee4cb848b1e0055a0144a00663a33daa100852bb9d8aaec5e2f49b8bda87077f`，后续运行不纳入本窗口。

| 项目 | 第一轮 | 同一次播放第二轮 |
| --- | --- | --- |
| START21 | seq538，frame30／tick378110 | seq540，frame450／tick378530 |
| Hook命中 | frame64／378144，150；HP5850 | frame484／378564，150；HP5660 |
| C输入→实际取消／START1 | 输入66，实际67／378147，seq539 | 输入486，实际487／378567，seq541 |
| 21取消状态 | W32 clock34、normalGate1/sdGate0、heat2200 | W32 clock34、normalGate1/sdGate0、heat2200 |
| Straight命中 | frame91／378171，40；precontrol6，HP5810 | frame511／378591，40；precontrol6，HP5620 |
| 目标恢复 | frame119／378199 | frame539／378619 |
| Straight释放 | frame125／378205，W12 clock57 | frame545／378625，W12 clock57 |
| RESET | frame155／378235，used2/chain2、heat2900 | frame575／378655，used2/chain2、heat2900 |

四击均为同一目标`0x235da018`，玩家为`0x21bc6018`；每轮两击间无目标恢复或玩家RESET，后继命中前control6，支持两轮各190的真连。第二轮首START前used/chain重新0/0，heat0、TAMA0/SCRIPT0；同一普通Replay自然恢复后再次起手，补齐这一路线的同场复用。总380是两条190，不是一条不中断的长连，也不是两个独立seed。

RPM原件`naoto21-straight-repeat-sampling.json`为921457字节，SHA256 `2f962c1f0c766c850b1cd2cda18381f23d6aadb57d8065fdf90daa6e7082da20`。独立核901个counter0～900完整且唯一；新证据只保存35个关键原行及全部94次拒读（6未初始化、88 tick/counter/player变化），没有复制全部901行。counter、技能clock和native输入frame继续分开；同tick目标HP/control更新先后的差异也不以RPM覆盖原生事件。target仍是player+14B8默认目标字段。

counter900／tick378979尾部为inactive、active-1、热0／capacity8000、TAMA0/SCRIPT0、Z0/Vz0、Graw51200、jump/fall100、armorWaza0／contributionCache0／protection0，目标HP5620。它是原生RESET窗口之后的RPM观测，不能把RESET时heat2900改写为0，也不冒称精确录像结束或native READY。释放事件中的SCRIPT0不代表全程0，采样仍保留攻击期间非零SCRIPT。94次拒读及非原子读边界不因最终正常而消失。

当前8efe成品Naoto.mek实际仍含推荐`ジャブ`的旧21说明；本轮已直接核旧CP932说明唯一出现，拟校正的`ストレートパンチ`版本尚未出现在该成品。截至本节取证，源码21说明已由root把后继推荐从Jab校准为Straight；这只是文案更正，不改动作。本次实机结果绑定现有8efe，不能据源码文字改动声称已经重建、当前进程已载入新说明或新包也已验收。

021只扩大当前固定目标／模板／日程下21→1的普通取消真连和同场复用。两次都在21 clock34提前取消，完整回撤仍由018证明；落空、SD取消、受击中断、其它方向／距离／体型／seed仍未验。窗口无异常或观察器错误，但前缀1条system／unclassified异常继续保留；case在捕获时仍running，没有终态／原Save退出保全结论。整体`runtimeValidated=false`保持。

## 22展开排热：源图与原生消费者依据

root决定保留原展开的排热机制。22的MEK热耗26是起手加热配置，不能解释为自然动作结束后仍净留26；当前实现会在展开阶段循环减热。本段补充静态／原生依据，024与025的独立玩法窗口由对应实测证据另行登记，不混用其不同输入。

当前8efe成品`Naoto.mek`内22的值区offset3252，前九个32位整数为`[-1,34,2,26,26,0,80,0,5]`，分别对应机体切换、主WAZ、FC增加、热耗最大／最小、经验、演示距离、category、attack type。它是category0普通武装，attack type5沿用展开供体；不能把type5混同initializer category2。原展开5／W16的对应值为`[-1,16,2,0,0,0,80,0,5]`。

BHE原`naoto.waz` W16／STRAIGHT10在P0 slot45有31f的CEventVal `[1,0,1,-1000]`与31～32f无限FF；P1有17f的`[1,0,1,-2000]`与17～18f无限FF。跟随45→40槽映射转换后，新主W34完整保留这些事件，当前最终`naoto.waz`的真实小端字节为：

| 路径 | offset | 原字 |
| --- | --- | --- |
| W34 P0 slot40普通事件 | 154458 | [31,31,1,0,1,-1000]，前两项为起止帧 |
| W34 P0 slot40 FF | 154486 | [31,32,-1,1,-2] |
| W34 P1 slot40普通事件 | 156402 | [17,17,1,0,1,-2000] |
| W34 P1 slot40 FF | 156430 | [17,18,-1,1,-2] |

结构读取也确认：原W16 P0的同类事件位于42590、FF位于42618；私有Rise W33的P0 slot40计数区146553和収納W18的计数区47304均为普通事件0、FF0。排热来自新主图保留的原展开阶段，不来自Rise或収納额外补偿。普通事件帧数与FF区间是数据字段，实际节奏仍按对应运行采样判断，不能仅把字段写成实际每一个native帧扣热。

原生读取链：[`65F6E0.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/65F6E0.c)第19～27行把MEK武装数值读到记录+92起，热字段为+104/+108，category为+120，attack type为+124；[`65F840.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/65F840.c)按category选择热字段，category0可取+108。[`6208D0.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/6208D0.c)第221～223行把本次热值乘100写入待加热量；[`6221E0.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/6221E0.c)第61～66行在待加热计数到0时调用621780。因此26对应起手2600 raw；不能因后续热条为零判断该配置从未被消费。

排热消费者是[`610300.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/610300.c)第66～78行：slot40的当前累计贡献与actor+0x17BC保存的旧贡献作差，再调用621780；对应槽身份另由0x6103DB的actor+0xC08取槽确认。重复负热FF推动贡献变化；[`621780.c`](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/621780.c)第15～29行执行负差额并夹到零。这与原展开排热机制一致，不需要修改MEK类型、热量地址或EXE。

本次仅在22日文简介中加入`展開中は排熱する。`，保留展爪→Rise→収納以及第二段命中后换招的原说明；动作图、slot40事件／FF、MEK26、原展开、Rise和収納均未改。21在提交15b35ed8a中已改为推荐Straight，本次保留该文本，不回退为Jab。新增22说明通过CP932严格编码／解码往返；把本次唯一字符串替换还原后，整个Java源文件逐字节等于修改前。

当前8efe封包仍未包含新21／22简介：成品Naoto.mek中旧21的Jab推荐与旧22说明各唯一存在，新21的Straight推荐和新增排热句版本均不存在。本次没有构建或改sealed产物；两处文案随以后构建更新，不能据源码文字改动声称当前进程已经载入新说明。此项决定保留动作与热行为，实测是否成立仍以各自冻结窗口为准。

## 2026-11-06 023两次自然播放、024慢分支与025控制链

本节固定023两次播放、024及025，截止025自己的RESET，不纳入026。首次023由root直接操作正常UI；第二次023、024、025由root明确授权的单一UI操作员执行正常LOAD／PLAY，root负责只读采样与决策。本节作者只读既有文件并整理证据，没有操作UI、游戏或调试器。

新增[`naoto23-22-baselines.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto23-22-baselines.json)为159568字节，SHA256 `9803da91e0c782a2254783b9f33c70fc6f76ae34908657a163935631ff1ceaa5`。完整日志前缀6599138字节、SHA256 `687f06264073f134edb6237b333e7cc8c5286705ff504aac90396ad8c77a1f54`，截止line11368／tick704250；四个窗口均保留全部原始事件，各自RPM尾部另列。完整采样与全日志未复制，也没有复制二进制。

模板022为898字节、SHA256 `882ff46f45575478687c2e819d9d60c5439dd5ca0a0b57df1491470be1968678`。实际109／敌0／map68，14槽`[23,-1,-1,-1,-1,-1,-1,-1,-1,-1,22,0,-1,-1]`，即近A23、普通Dash B22／C0，其余空。三个录像均独立解析三块与EOF，头部和前两编码块809字节与022相同，SHA256 `af4a4d435f22993d56ef928776f4f0089c7713d7e227e7e1ebd9a4bf2a16db07`；所有480／480／600帧edge／held逐对核验。

| 录像 | 字节／SHA256 | 普通输入，左闭右开 |
| --- | --- | --- |
| 023 | 852；`a0591b2228c100fabcfd22f6b073e7fd55a6f0d6102e9e24b5298a5456ca1b2e` | A[30,34)，480帧／tail300 |
| 024 | 852；`9352ebc4e83cb9dd42a4a474a078a284be2a94bd4eb1daca88ef47aa155983a6` | B[30,34)，480帧／tail300 |
| 025 | 859；`9573135e2c28a7a24a418c02063e422a9d08faae81e0ed196919a23356bcc09c` | A[30,34)、B[68,72)，600帧／tail350 |

### 首采样失败及023两次单招

首采样`naoto23-natural-sampling.json`为778字节、SHA256 `fdb0d9f64b09692ff5ebf3f4aa9c7e074aba869e22bc75c06829f779c0de06a5`，at为03:26:00.934049 UTC，started=false、complete=false、lastCounter=-1、rows和readErrors为空。root报告等待120秒后到期；文件没有独立等待起点／时长字段，本次不声称从文件算出120秒。

对应实际播放前的Replay023访问line11203在03:26:05.251277，晚4.317228秒；真正START866在03:26:05.854331，晚4.920282秒。因此是采样先超时，不是游戏失败或全零物理状态。更早03:20／03:22的文件访问也保留，单纯菜单／列表访问不能充当实播。第二次有效RPM不填补第一次采样缺口。

| 项目 | 023首次，line11214～11233 | 023第二次，line11234～11253 |
| --- | --- | --- |
| START23 | seq866，frame30／tick595225 | seq867，frame30／tick633559 |
| 伤害 | frame61／63／65／66：37／37／37／150，总261 | 同四个frame和伤害，总261 |
| 末HP | 5739 | 重新从6000起，末5739 |
| W18 clock23释放 | frame112／595307，heat2400、TAMA0/SCRIPT0 | frame112／633641，同状态 |
| 敌恢复／RESET | frame205／206，tick595400／595401 | frame205／206，tick633734／633735 |

第二次RPM原件623169字节，SHA256 `43e3db09f1ef7f9013a443c70c803032bb523c3f4a2f74a7480ded1b14a7bfef`，481个counter0～480完整，53次拒读为3未初始化及50一致性变化。实际counter31 W35、88 W35 clock55、89 W18 clock0、112 W18 clock23、113 idle；重Hit后counter67 normalGate1，target在68进入state8。counter480热／对象／地面物理正常、敌HP5739；过程中SCRIPT2等读数仍保留。两次重播同一Replay不是同场复用，也不是两个独立seed。

### 024：慢分支基线，完整热量口径纠正

024原生窗口line11276～11298：START868在frame30／tick667381；frame116～120各45，121为90，六击315、敌HP5685。W18 clock23在frame194／667545释放，敌恢复236、RESET237／667588。释放和RESET的玩家heat0是真实关键读数，**不能概括为整条曲线全0**；此前口头审读的过宽说法在此明确纠正。

完整481行热分布为0×449、2600×28、1600×2、600×2。counter34／W34 P0 clock3先加至2600，counter62／clock31降至1600，64／clock33为600，66／clock35为0。原展开slot40／FF排热保留，不能因收尾0而称MEK26未生效，也不清除原事件改变特色。

RPM原件624792字节，SHA256 `a3e382f5a4b044fdc74a31be15593eb92b4012da09848137e49bd5e3c39bb46b`；78拒读为5未初始化及73一致性变化。counter31 W34 P0、目标state0，99 W33、171 W18、195 idle，480尾部正常。这仅是目标未受击时的P0自然基线。

### 025：受击分支与重击起七击312

025窗口line11331～11368：23在frame30／tick703989启动seq869，轻击frame61／63／65各37，重击frame66为150。B68输入，实际frame69／tick704028在W35 clock36、normalGate1/sdGate0取消到22，START870。22在frame157～161各23、162为47，六击162；W18释放frame235／704194，敌直到frame290／704249才恢复，RESET291／704250。

十击总423与HP6000→5577吻合，但前三轻击111的precontrol均0、目标state0，不把全部十击称为真连。**23重Hit150接22六击162，共七击312，可确认持续控制**：同敌actor`0x235da018`期间无原生恢复或RESET，22各击precontrol为17／19／19／19／19／19。RPM counter68～162的95个目标样本仅state8／10，control最小18；它与原生首Hit17的同tick读序差异保留，不用RPM替换native。

实际counter70为W34 P0 clock0、target state8/control30，71为P1 clock0，139仍P1 clock53，140进入W33。P1段69个counter只覆盖54个不同clock，有实际停顿；这里只证明目标受击分支选择，不把P1简单说成实际更快，也不把counter当native输入frame。024的P0独立对照保持，分支依据是目标受击状态，非自身高度。

RPM原件779058字节，SHA256 `e114e999a3e5fb110eb1a847bc36936af528fb5302b2a3dd552fbc4ee0561828`，601行counter0～600完整；74拒读为7未初始化及67一致性变化。玩家完整热分布为0×529、2400×39、5000×19、3000×8、1000×6；counter34／73／92／100／106依次2400／5000／3000／1000／0，后三点为P1 clock17／19／21。combat_damage中的heat396～456属于敌actor，不混作玩家热。counter212 W18、236 idle，600尾部热／对象／地面物理正常，敌HP5577。

新证据保存必要原样RPM行及全部53／78／74次拒读，并以完整原件hash约束未复制行。counter、技能clock及native frame严格区分，读序差异和非原子读限制保留。首次失败采样小对象也完整保留。所选原生窗口无异常／观察器错误，前缀1条system／unclassified异常仍保留，不自行归为无害。

本节没有Jab后继、23／22同场复用、SD取消、受击中断、落空或全目标条件通过结论。026是独立后续窗口；不得用其结果回填本节。既有排热机制段和全部原文保持，当前8efe未为新简介重建；case捕获时仍running，整体`runtimeValidated=false`。

## 2026-11-06 026取消至Jab失败与027同场复用

本节新增独立证据[`naoto23-22-cancel-and-repeat.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto23-22-cancel-and-repeat.json)，139491字节，SHA256 `d0f33ae53922f497a48a8c56e6ffab3389537ed721f112a08d5dbe0d7162d4f4`。既有baseline仍为159568字节／SHA256 `9803da91e0c782a2254783b9f33c70fc6f76ae34908657a163935631ff1ceaa5`，其冻结前缀也已复核不变；026失败不被027成功覆盖。两次正常LOAD／PLAY均由root授权的单一UI操作员执行，root负责只读采样与决策，本节作者仅审读文件。

026为864字节、SHA256 `871f567618a9a231de1fd419bdc35b8a7201886312bbba890ce1fe19516ff025`，600帧／tail350，输入A[30,34)、B[68,72)、C[164,168)。027为877字节、SHA256 `631fefb9e2835b32eedff98ad6293909609351ec3bc16fbdd6492f0fb1176cce`，1100帧／tail400，输入A[30,34)、B[68,72)、D[320,324)、A[500,504)、B[538,542)。两份全部600／1100组edge／held独立逐对核验，header／三块／EOF正确，前两编码块与022的809字节前缀完全相同；实际装备仍近A23、普通Dash B22／C0，其余空，未改初态。

原生026窗口line11391～11434，027窗口line11467～11547，分别到各自RESET。完整前缀6690228字节，SHA256 `148efbdbcf0407a6268c3fc96d84f69e3c0150e71f640962b297a7c8c21d1b0f`，截止027的tick824375／frame761。窗口全部原始对象保留；后面的RPM尾部不是原生READY、精确播放终点或终态，未发出的frame0也不由counter0补造。

### 026：取消成功，第三招未命中

START23 seq871在frame30／tick771071，START22 seq872在frame69／771110；前段伤害与025相同，总423，其中仅23重击开始的七击312有持续控制证据。C164确实在frame164／tick771205正常取消active22的W33 clock24，normalGate1／sdGate0、SCRIPT2，随后同tick启动Jab0 seq873；不能把私有W33本身另计一个装备。

Jab没有伤害事件，frame202／tick771243在W11 clock37自然释放，目标直到290／771331恢复，RESET291／771332，used3/chain3但heat仍500。它证明22可普通取消到0，**没有证明第三件武装参与伤害真连**；不能以真实START0、HUD沿用计数或随后正常收尾补算命中。取消／下一START时SCRIPT2如实保留，不能说全过程对象为0。

026 RPM原件779531字节，SHA256 `01dd52a65dfd79f238f2f4c48bb04009a8998172bb5d4171e2d990bd18d98fa4`，601个counter0～600完整，80次拒读为5未初始化及75一致性变化。完整热分布0×403、2400×39、5000×19、3000×8、1000×6、500×125、100×1；展开排热后Jab在counter168新增500，RESET时仍500，counter293的heatState3为100、294归0。尾counter600才是inactive、heat0、对象0、正常地面／G50／倍率100及装甲保护0，敌HP5577；该尾部不回填成RESET热量。

### 027：普通D重新接近后的两轮23→22

| 项目 | 第一轮 | 同一播放第二轮 |
| --- | --- | --- |
| START23 | seq874，frame30／tick823644 | seq876，frame500／tick824114 |
| 23轻三击／重击 | frame61／63／65各37，66为150 | frame531／533／535各37，536为150 |
| START22 | B68输入，实际frame69／823683，seq875 | B538输入，实际frame539／824153，seq877 |
| 22六击 | frame157～161各23、162为47 | frame627～631各23、632为47 |
| 22命中前control | 17／19／19／19／19／19 | 17／19／19／19／19／19 |
| W18 clock23释放 | frame235／823849 | frame705／824319 |
| 目标恢复／RESET | frame290／291，tick823904／823905 | frame760／761，tick824374／824375 |
| 各轮伤害／末HP | 423／5577 | 423／5154 |

20次伤害均记在同一敌actor`0x235da018`，总HP6000→5577→5154，**整段回放累计846**；UI显示423是当前显示段，不是整个回放累计。每轮前三轻击111仍无控制证明；各轮真正认可的控制段均为重Hit开始七击312，两轮之间确有恢复与RESET，不称一条846长连或两个独立seed。

第一轮RESET已在frame291完成，D只在frame320～323普通按住。原生line11506于frame320／tick823934记录玩家0→3，active-1；line11509于frame384／823998记录3→0，active仍-1。RPM从counter321观察到state3，385回state0，中间技能3／4／5属于移动图，不是额外武装START。整数地图X从2736到2863，差127，raw位置2801466→2931712另行保留。**这是正常D移动重新接近，不是SD取消**，也不把counter321写成native输入frame320。

第二轮START23前used/chain重置为0/0，heat0、TAMA0/SCRIPT0；随后两件武装再次命中并走到同样收尾，支持本条件的同场复用。原生窗口没有再次载入、切敌或强写位置；普通D输入及实际移动事件都保留。

027 RPM原件1429791字节，SHA256 `662da927a600f991d56bab97cc8d1a001560bb70bd9a2c549ed115b5481395f1`，1101个counter0～1100完整，169次拒读为5未初始化及164一致性变化。热分布0×957、2400×78、5000×38、3000×16、1000×12；两轮均先加热再展开排热，第二轮变化counter504／543／562／570／576依次2400／5000／3000／1000／0，未写成全程零热。

尾counter1100／tick824713为inactive、active-1、heat0、TAMA0/SCRIPT0、Z0/Vz0、Graw51200、jump/fall100、装甲贡献及保护0，敌HP5154。两份RPM只保留必要原样变化行、完整原件hash及全部249次拒读，不复制完整601／1101行；非原子读、counter与native frame差异以及过程中的非零SCRIPT都保留。

两原生窗口无异常或观察器错误，冻结前缀1条system／unclassified异常仍保留，不自行归为无害。当前认可范围为026的普通取消但Jab未命中、027两轮23重击→22的控制链与普通移动后复用；落空分支、SD取消、受击中断、其它方向距离／目标等仍未验。case捕获时仍running，未记录终态／原Save退出核验，整体`runtimeValidated=false`。

## 2026-11-06 029／030自然基线与031重击接三点射

本节绑定原8efe包／case13，正常UI由root授权的单一操作员执行，root负责只读采样与决策，本节作者只审读文件。新增[`naoto24-25-baselines.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto24-25-baselines.json)为164874字节，SHA256 `c4c2f941da5046e3de24810030565b66cf5983c2d44ae7125c8f1f392cf4419d`；冻结至031的line13826／tick1122476，完整前缀8015540字节、SHA256 `d437746a15dfcf9824ac81e541d0cde5a37cc68a838fdc06b88411871da6e38b`。029、030、031各自到本轮RESET划窗，032候选不纳入。

模板028为840字节、1374帧全零，SHA256 `dcf5912e183e05270cf3d1d27fe18391bd67af48b8f56b367059bdf836502063`；实际109／敌0／map68，14槽`[23,-1,-1,-1,-1,-1,-1,-1,-1,-1,24,25,-1,-1]`。独立核三个Replay全部输入及三块／EOF，头部与前两编码块798字节均与028相同，SHA256 `2b3472578d65f1ff3cba085a04e2d3fc446018b355f3557639f0148906226ae9`。

| 录像 | 普通输入与帧数 | 实际结果 |
| --- | --- | --- |
| 029 | B[30,34)，360帧／tail200；840字节、SHA`8b10cda3c1355bd17c994a0ebc973060c51f707b73dabd59047a5807e85a9ede` | 24独立三击7+6+6=19 |
| 030 | C[30,34)，600帧／tail400；843字节、SHA`2e686b41f54ba2ab3eab21cf2cd65da924b3e169c9ca650474982e438ae7e827` | 25独立27击387；不同于早先菜单292 |
| 031 | A[30,34)、B[68,72)，600帧／tail350；848字节、SHA`0bad1d2f5914a9d8991ca471326f663c3f40e57a34c16cd03d66d4d52d48f24e` | 23接24总270，但只认可重击开始四击159持续控制 |

### 029：三点射的实际对象与一次额外加热

原生窗口line13667～13679：START966在frame30／tick990299；敌伤害frame66／70／74为7／6／6，HP6000→5981，均precontrol0，无敌恢复事件。父W36 P2 clock33在frame113／990382释放，TAMA0/SCRIPT0；RESET143／990412仍heat1400。这里证明单招实效和所述收尾，不构成真连。

RPM原件492646字节、SHA256 `6170e439a57b93389106bf67aec4153e78c6a354b6cb8aed271c5f97718d7f05`，361行完整，64次拒读为4未初始化、59 tick/counter/player变化、1次TAMA count变化。观测到三条1/105实例片段，parent和root均`0x22a74008`：

| 对象 | 首末counter | 首次本体W36 P1 clock | lifetime范围 |
| --- | --- | --- | --- |
| 0x23896a28 | 67～73 | 0 | 0～5 |
| 0x238d8e78 | 71～77 | 4 | 0～5 |
| 0x238ebad8 | 75～81 | 8 | 0～5 |

各片段均由entry0观察至entry1，初期lifetime0有重复样本；counter82起TAMA0。首个原生伤害甚至早于首条对象采样一tick，不能按采样先后强配逐Hit弹体。短命生成／销毁可能发生于两次接受样本之间；三条观测不等于已经捕获所有constructor或完整口焰生成。

完整热曲线为counter34加至1200，67／P1 clock0变1400，之后保持至自然冷却；counter145～148为1000／600／200／0。它支持本输入下P1一次额外200，与实际slot40一次+200／无FF相符，不是每发加200或每发加100。最终counter360热／对象／地面物理正常。

### 030：父释放后的持续效果及地址复用

原生窗口line13702～13762：START967在frame30／tick1031760；27次伤害为15×12、22、20、19、18、17、16、15、14、13、12、11、9、8、7、6，共387，末HP5613，全部precontrol0，没有敌恢复事件，不称真连。父W37 P2 clock34在frame124／1031854释放时TAMA仍3；其后15次伤害合207继续到frame197。RESET298／1032028仍heat3200，不能把父释放写成全部弹体回收。

RPM原件987064字节、SHA256 `9bd06d569a00b1dc0cb885e648575b9cb06d409fbe037296f9c34dd00fcb7535`，601行完整；107次拒读包括5未初始化、87一致性变化、5 ring未闭合、4短链、2 ring变化、3 count变化或无效、1 count变化。两个1/215主对象分别为`0x23898330`（counter65～252）及`0x2332d340`（73～248），首次本体P1 clock0／8，parent/root均玩家，各自lifetime0～173。maxTAMA4、最后非零counter252，253起TAMA0；初末counter是观察界限，不是原生生成／销毁时刻。

1/217共观察63个地址／技能对，其中16个地址发生17次缺席后重现并从lifetime2降到0。例如`0x21d67a30`在125→132、134→153两次重现。按这些断点可描述80段观测片段，**63和80都不能当完整spawn次数**。1/217的parent均为上述两个1/215地址、root均玩家。接受样本未出现1/216或7/323，不能由此说未生成；SCRIPT最大2但未读取其实体身份，不能硬指认为7/323。口焰／短命对象仍有漏采边界。

热量counter34为3200，父释放及RESET时仍3200，counter300～307按自然冷却降到0；最终counter600正常。旧菜单292证据原hash保持，未用本次387改写。

### 031：23重击接24的四击159控制段

原生窗口line13795～13826：START23 seq968为frame30／tick1122272，三次轻击37在frame61／63／65，重Hit150在66。B68输入后实际frame69／1122311正常取消W35 clock36到24，START969。三次机枪伤害3在frame105／109／114，命中前control24／27／26，同一敌`0x235da018`至frame233才恢复，RESET234／1122476，末HP5730。

总七击270中的前三轻击111没有控制证明；**从重击150到机枪三击9，四击159持续控制成立**，两者之间没有敌恢复或RESET。父24在frame152／1122394以W36 P2 clock33释放，TAMA0/SCRIPT0，heat3800。这里只有这一固定目标和输入的单轮连接，032及同场复用尚不计入。

RPM原件818144字节、SHA256 `fc3c09251651211b28dcbe8819d37bf5d7f947e2643ed1bf98a28e9164857f88`，601行完整、90次拒读为6未初始化和84一致性变化。三条1/105首次counter106／110／114对应P1 clock0／4／8，观测结束112／116／121，parent/root均玩家；第三条lifetime到6而前两到5，原值保留，不能统一归一成5。counter122起TAMA0。

本体P1 clock0～8的normalGateRaw为2，clock9～12为1。这里只记录原值，不凭2／1推断更早是否可取消；须由真实输入及START验证。热量counter34／73／106依次2400／3600／3800，RESET仍3800，counter236～245自然冷却归0；最终600尾部正常。

三份完整采样不复制进证据，只保存关键原样行、所有64／107／90次拒读及完整原件hash。采样器的环表／计数／tick守卫不能消除非原子读与短命漏采，counter、技能clock、lifetime和native frame继续分开；没有逐Hit owner探针或SCRIPT身份时不补造归属。所选原生窗口无异常或观察器错误，前缀1条system／unclassified仍保留，不声称全会话零异常。

源码简介更新与当前8efe封包分开：本节实机继续使用旧包文本和既定动作，源码改文案不表示当前进程已加载新说明。整套仍缺24／25复用、落空、SD取消、受击中断及其它条件等，032候选另行验收；case捕获时仍running，`runtimeValidated=false`。

## 2026-11-06 032：三招实际命中与控制空窗，暂不认稳定真连

032通过正常UI实际播放，三个武装均命中，23→24及24→25正常取消成立；该固定靶在本次运行中保持吹飞／倒地连接直到后段命中。但是**现有证据不足以排除自动恢复或受身，不认定这条三招路线已达到稳定真连验收**。根代理转交的Astra原生机制复核指出state8 control0存在某些自动恢复路径，state10也有依资源条件的D受身分支；本节作者只审读运行证据，没有重新执行这项逆向分析。

独立证据[`naoto23-24-25-control-gap.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto23-24-25-control-gap.json)为83349字节，SHA256 `abc253114166757c6232cca52cc01cc4fdd46507f9022a9c84bea3209ddc27ff`。029／030／031基线保持164874字节／SHA256 `c4c2f941da5046e3de24810030565b66cf5983c2d44ae7125c8f1f392cf4419d`及原冻结前缀；032不回填它们，033另行验收、不纳入本窗口。

032为853字节、SHA256 `7b2e58d2c36fe916472e265b155f97e013ef55177a5700854b70382a015c7e31`。独立解析三块及EOF，头部和前两编码块798字节与028相同；600组edge／held完整匹配A[30,34)、B[68,72)、C[116,120)，tail350。实际装备仍109／敌0／map68、近A23与普通Dash B24／C25，其余空，没有强改actor或初始块。

原生窗口line13849～13920，首个实际输入frame30至RESET360／tick1203496；72条原始对象全部保留。完整前缀8066095字节，SHA256 `965bfdb252fbea6d5695b87756be190a3576274a3e39e949b68274cde1ed0464`。native未另发frame0，不用RPM counter0补造；后续尾部不是READY或进程终态。

| 项目 | 本次事实 |
| --- | --- |
| 三次START | 23 seq970：frame30／1203166；24 seq971：69／1203205；25 seq972：116／1203252 |
| 23伤害 | 轻三击各37，frame61／63／65；重击150在66 |
| 24伤害 | 三次3在frame105／109／114，precontrol24／27／26 |
| 24→25取消 | W36 P1 clock10，normalGate1／sdGate0，TAMA2 |
| 25首击 | frame154／tick1203290，6伤害，**state8／precontrol0／normalGate0** |
| 25全段 | 17次伤害合100，最后frame260；全流程24击370，敌HP5630 |
| 25父释放 | frame210／1203346，W37 P2 clock34，heat7000、TAMA3 |
| 敌恢复／RESET | frame333／1203469；frame360／1203496，RESET仍heat7000 |

原生伤害和RPM都保留了空窗。RPM counter145目标control1；146～154连续九个接受样本为state8／control0，target HP5730，25尚未造成首击。首击的native HP已降5724，而counter154同tick仍读到伤害前HP5730；counter155才见HP5724、control40、pendingReaction2，读序差异没有被改写。

counter67仍是state0但control30／pendingReaction2，属于本轮受击应用过渡，不称已经恢复；counter68～260才只出现state8／10。重击到最后伤害期间未记录恢复入口，是这次固定靶的运行事实，**不能单凭“无恢复日志”或state8／10判定对手没有可用恢复机会**。

当前缺少稳定性判断所需的实际阵营映射、目标控制模式、倒地动作子状态和受身资源／资格。已有meka0、state、control、downTime、bounceLevel等字段不补足这些缺项；未采字段不能按0或禁用处理，目标normalGate0也不能替代全部自动恢复／D受身分支检查。因此仅登记三招实效、两次正常取消、此固定靶的实际吹飞连接及观测回收，稳定真连与同场复用保留未验，不反向断言路线必定无法成立。

RPM原件992885字节，SHA256 `74b32e92f4f883112ee81a66cce0f2b014c045d429689919f28d3bebfc73b576`；601行counter0～600完整，全部99次拒读保留：4未初始化、80一致性变化、4环未闭合、4短链、4count变化或无效、2ring变化、1count变化。证据仅复制21个关键原行，不复制完整采样，counter／clock／native frame和非原子读限制继续保留。

玩家热量counter34／73／106／120依次2400／3600／3800／7000；RESET仍7000，counter363起进入自然冷却，380才归0。最后非零TAMA在counter336，337起归零；它与敌恢复333、RESET360及热量归零380是不同时间。counter600尾部为inactive、active-1、heat0、对象0、地面Z0/Vz0、G50／倍率100、装甲贡献和保护0，不据此声明全过程零对象或所有中断安全。

运行由root授权的单一UI操作员执行，root采样与决策，本节作者未操作UI。窗口无异常或观察器错误；前缀1条system／unclassified异常仍保留，不自行归为无害。24源码简介的额外发热文字已校正，当前8efe未为新说明重建，动作未变。033、落空、SD取消、受击中断及更多目标条件另验，case捕获时仍running，`runtimeValidated=false`。

## 2026-11-06 033／034：射击顺序对照与暂停边界

已完成的两个试验以[`naoto23-25-delayed24-checkpoint.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto23-25-delayed24-checkpoint.json)固定精简证据；完整采样留在原case，不复制二进制。033／034均为正常028模板只替换第三普通输入块，分别600帧，首两块798字节保持。它们不是032的补帧，伤害与子弹不得跨回放拼接。

- 033输入A30／C68，实际START23／25在30／69。总23击401；重击150起到frame185共17击272持续控制，晚frame212／230／248三击7／6／5，212的precontrol0且RPM211～212为0，晚18不属于前述控制段。父25在163释放仍TAMA3，292起TAMA0，恢复355、RESET356 heat5600，371才自然冷却至0。601行、107次拒读保留。
- 034输入A30／C68／B156，实际START23／25／24在30／69／156。总28击414，其中23为261、25为144、24为9。重击frame66至233共24击302，后继native precontrol均>0，RPM counter68～233已接受样本control最小14；最后frame260的1点仍precontrol0，counter256～260亦control0。因此仅固定有条件的302主段，不称整414稳定真连，也不扩大到其它目标、距离和受身配置。
- 034三个机枪对象首次观测counter193／197／201对应P1 clock0／4／8；三次3点伤害实际frame200／208／213。首次观测不等于命中，未逐弹强配owner。父24在239释放时TAMA4为两个1/215和两个1/217，机枪从221已不在环；最后非零TAMA291。恢复365、RESET366 heat7000，counter368～385自然冷却归0，末HP5586。601行及101拒读保留。
- 035已生成（B[30,200)、480帧），停在LOAD确认时用户要求暂停；未加载、未播放、未启动采样，不登记长按通过。036仅是同场复用设想，未生成或运行。

## 已审查、尚未实测的D取消路径

Astra对原生61DEA0／61B080／6206E0／62FEF0及当前成品的只读审查：普通门normalGate1、预算>0且功能位允许时，无方向D可在活动普通武装中进入state3搜索冲刺；sdGate0不排除它。方向+D通常请求state4，活动武装时该分支要求active<0，不等价于搜索取消。专用SD门成立则经6206E0释放武装回state0，与state3搜索取消分开记录。

最低验证应含闲置无方向D、各招开门后D、24的clock9前拒绝／后允许对照，以及方向+D对照；需读预算+1928、8750E8／8750F4／8750F8功能位，核真实输入edge、原武装释放、state／nextState和预算变化，不凭动画或预算单项判定。当前未实读这些功能位，不当成已启用。

22在主W34 P0 clock44开normalGate1，切Rise W33及収納W18仍继承1；轻Hit flags0不会关闭已开门。完整动作测试仍选Rise第二Hit后，提前D另记放弃后续动作。23轻段门2、重Hit开1；24／25 P1 clock9开1。D候选时点需实际native clock验证，hitstop缓存不能误算提前取消。此前027的恢复后D移动不补算技能取消。

本次用户明确暂停，后续操作见[11/06交接](checkpoint-20261106-paused.md)。目标尚未完成，runtimeValidated=false；此段只固定已有试验和静态结论，没有新增玩法测试。
