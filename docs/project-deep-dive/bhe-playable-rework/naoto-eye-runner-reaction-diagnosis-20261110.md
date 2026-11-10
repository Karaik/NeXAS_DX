# Naoto受アイランナー攻击无硬直：Replay060只读诊断（2026-11-10）

本次问题的直接解释是**アイランナーW8的基础控制时间为0**：它仍造成伤害、短停止与水平击退/下落倍率变化，但不必让闲置目标进入受击状态。W9头突也有同样的基础控制0，不能作为可靠替代。建议下一步正常UI改用已知可选的Kou0，先观察一次真正非零control的近距来袭，再对齐Naoto出招；无需改资源或制造受击。061仅已生成、未执行的状态保持，不能补写运行结果。

本轮只读case16的`naoto-eye-runner-idle-sampling.json`、`events.jsonl`指定tick160539～160899、实际成品Naoto/Kou MEK、原版Zako021a/Kou WAZ二进制与JSON、现成原生反编译；未操作UI/进程、未构建、未修改代码或提交，未继续Sou34工作。

## 060实际发生了什么

采样文件位于`target/bhe-playable-runtime/nine-naoto-20261110-16/naoto-eye-runner-idle-sampling.json`，798025字节，SHA256 `81f458b1c17fe93ad06c4d4462416172f8b601d581d71b9789049e6948c072a0`。status=complete，361接受行、counter0～360，20次拒读保留。所有接受行中玩家state/control/pendingReaction/reaction均0、activeWeapon=-1；armorWaza/贡献缓存/protectionFlags均0，normalGate1、sdGate0。

| native tick / context | 实际事件 | 说明 |
| --- | --- | --- |
| 160665 / 126 | enemy combat_use，weapon0；随后RPM为G51/W8 | 观察器敌侧是USE，不能补造不存在的通用gameplay_start |
| 160687 / 148 | player damage20，HP1980；同tick reaction_applied | sampler149/tick160688才读到HP变化，保留读序差异 |
| 160751 / 212 | damage16，HP1964；reaction_applied | 不是第二种攻击证据 |
| 160820 / 281 | damage12，HP1952；reaction_applied | counter281/282有同tick先后状态，不能合并 |
| 160885 / 346 | damage8，HP1944；reaction_applied | 无玩家motion到受击状态 |

指定窗口共4次enemy USE、4次damage、4次reaction_applied、2次enemy motion；没有玩家受击motion。原样事件为当时events.jsonl第763～776行，均用tick窗口界定，未混入此前Replay frame1199残留的CPU事件。

第一击后fallModifier在counter149变0，154起逐步回升；玩家X/Y从2302/1808移到2308/1810，再继续后退。各击后都出现类似变化。因此“完全没有反应/完全免疫”不准确；准确描述为“已进入反应应用，存在物理反应，但控制与受击状态仍为0”。本轮玩家始终没有活动武装，本身不能证明任何武装已被中断。

## 原生字段为何得到0控制

以下原生文件前缀为`src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/`。

`640680.c:17～30`把CEventNokezori字段读入运行对象：int2→+84（停止时间），int3→+88（hitback），int4→+92（已在受击时hitback），int5/int6→+96/+100（自机补正两项），int8→+108（水平反应速度），int10→+116（垂直规模），int11→+120（反应重力/倍率字段）。不能把任意一个20当作无条件控制时间。

`61CD20.c:127～151`先按目标是否已在受击总类选+88或+92；只有**攻击者**虚函数+344返回真才加+96或+100，随后写目标`this[1595]`（0x18EC/control）。若control仍为0，152及229行后的“清技能运动/设置pending受击”的正控制分支不会进入；但162行后的水平反应速度，以及193行“control>0或没有active武装”下的垂直/倍率处理仍可执行。这正好解释060的击退/fall0与无硬直可以并存。

自机补正判据需要精确措辞：已在封存EXE只读核CMeka vtable `0x7F12AC + 344 = 0x61C5D0`，该函数是`(*(actor[86]+24)&4)!=0`；`+312`才是`0x624210`的控制mode==0判断。故不能将+344简化成“actor指针等于玩家”或机械等同CPU/人类输入。060未直接采该描述块bit；但实测control0、reaction0，与源基础0且没有应用+20一致，不能拿三个无关D配置raw38当硬直免疫开关。

### 两个敌招的真实原字

已读`src/main/resources/game/bsdx/waz/Zako021a.waz`，下列Nokezori字段与同名JSON逐值一致；当前封存overlay未输出Zako021a.waz，该技能走继承资源。此处未做活进程W8载荷dump，不把静态读回称为动态内存抓取。

| 敌招 | Hit窗 | 地面反应offset | int2停止 | int3基础control | int4已受击control | int5/int6自机补正 | 水平速度 / 垂直规模 / fall字段 |
| --- | --- | ---: | ---: | ---: | ---: | --- | --- |
| W8 KICK / キック | 22～26 | 10132 | 2 | **0** | 5 | 20 / 20 | 6 / 0 / 0 |
| W9 ZUTUKI / 頭突き | 28～77 | 12606 | 1 | **0** | 5 | 20 / 20 | 6 / 20 / 30 |

空中、空中→地面、倒地分支使用同类字段，未发现可用来保证“首击把未受击玩家打断”的非零基础control。W9在目标闲置时可能有向上物理作用，但对正在出招且control0的目标，`61CD20`的193行还会跳过没有活动武装才允许的那部分垂直修改；不能将“头突会浮起”的简介当作硬直/中断保证。

Naoto成品MEK basic中permanentArmor=0；060也没有武装装甲/防护标志证据。其comboImpactFactor=300不是上述`61CD20`直接减掉control的字段，不能据名称发明“Naoto被动抗硬直300”。反应函数在这里没有按训练行为value3把所有控制归0的分支；模式3负责开启敌AI。没有必要先改变训练设置或玩家属性。四次伤害20→16→12→8也不能单独作为玩家抗硬直证据。

## 可行的正常UI下一步：只推荐Kou0

Kou0已在本会话链的正常训练UI反复作为敌方选用，避免引入未证可选的小怪。当前封存Kou.mek的weapon块[454,43243)、AI块[43243,56745)与原版逐字节相同；当前overlay没有Kou.waz覆写。读取原版实际Kou.waz，以下两项非零基础控制不依赖自机补正：

| 默认AI可选近距武装 | 主图/Hit窗 | 原攻击力 | 基础control / 已受击control | 自机补正 | 已核AI入口 |
| --- | --- | ---: | --- | --- | --- |
| weapon1 アームストレート | G9/W15，14～19f | 50 | **30 / 30** | 0 / 0 | 距离0～100，prob字段600 |
| weapon3 ローキック | G9/W21，18～24f | 60 | **20 / 40** | 0 / 0 | 距离0～150，prob字段50 |

二进制反应记录分别在Kou.waz offset24733与42133；它们是有具体非零base control的相对温和单段近战例证，不是保证CPU只会出这两招。正常训练UI没有已证的“只锁敌某一武装”入口，Kou默认AI还可能选其它招；必须按本轮enemy USE与RPM实际G9/WAZ/clock辨认，不能按角色名或概率预写结果。

建议只做一轮短无攻击baseline（约240～360帧，沿现有正常UI近距与行动3），先取得玩家实际control>0及pending/state反应，再用该轮来袭时刻安排一次Naoto准备段输入。若实际敌招不是W15/W21，先读该次确切图与反应，仍不可把别招事件借作这两招证据。确认中断须同时有玩家先START、来袭时仍active、原生damage/reaction、提前离开原武装流程及后续恢复；掉血/击退/fall变化仍不足。

未额外推荐第二敌，因为当前“已正常UI可选＋具体非零首击反应”的证据以Kou0最充分。アイランナーM等相似弱怪并不能仅凭名字或伤害较高升级为合格供体，部分同样基础control0。保持原资源，不添加硬直、不关闭防护或强制START来制造验收。

## 063实际首招W44与Naoto20首轮时点（追加只读核查）

063并未出此前候选W15/W21。实际enemy USE为weapon10「ビームソード」，MEK基础引用W42；RPM本轮实际图为G9/W44「ビームソードLv3／SWORD03」。不能机械以MEK基础W42覆盖实际Lv3图。062/063正常模板读回109/0/map68、行为3，14槽为`[-1,-1,-1,-1,-1,-1,-1,-1,-1,20,21,22,-1,-1]`，因此这里A确实指Naoto20。

`naoto-kou-idle-sampling.json`为809417字节，SHA256 `1b055fbc1e75858b0eaabb05c9bcf79d66543ce313961307b747e7f125fc199e`，361接受行、20拒读。native tick窗口245663～246023；首enemy USE在245679/context16，RPM counter17同tick已见weapon10/W44 clock0。首Hit在245696/context33，实际伤60；同tickreaction_applied读control16/pending1，245697/context34请求state0→7，RPM counter35/tick245698已state7。其后context39敌换weapon28，RPM40见W101，后续连段不能并称为W44的单次攻击。

原版实际Kou.waz的W44只有一段Hit：clock17～22、hitCount1、命中普通取消标志1，源攻击值120（不冒称本场实伤120）；地面Nokezori offset206168的base control16、已有受击50、自机补正0/0，水平反应速度8。空中分支base15/已有受击30。该payload与063首击control16相符，证明本场确有正常非零硬直条件。

W44本图没有slot11/14位置绑定/坐标追位、slot15方向事件、slot18主动平面速度、slot20追高，也没有slot36装甲或slot38无敌事件；这只描述本图载荷，不把它扩成所有外部防护状态已测。sprite方向增分slot10在0f设300，17f设0；`60A810.c`按该增分限制视觉朝向的改变，因此准备段可以调整朝向，攻击开始时停止这项旋转。移动倍率slot24在17～32为300→0，其作用是已有移动向量倍率，不等于平移速度300。063实际敌人在W44 clock0～17始终2402/1808，玩家2302/1808，双方同地面。

**建议首轮只加A[27,31)，不加方向/D/后续攻击。** 按063基线，context27敌已处于W44 clock10，距离首个允许Hit的clock17约6个输入帧。Naoto20新W31继承Jab的自身平面速度和Hit都从clock11才开始；若本轮A在27附近真实START20，则来袭时W31大约clock5～6，先于自身Hit/前冲。这样避免为了创造来袭而先打断敌人，适合作为第一次startup中断。

不能保证输入改变后所有绝对tick/几何不变。准备期朝向、玩家动画受击框、已有输入动量和AI调度都可能改变接触；但只要敌仍同一W44起手，其Hit不能仅因玩家靠近就在本图clock17之前有效（`63A7D0.c`按事件起止窗选工作对象）。063已经在第一有效窗命中；这个单变量候选本身在Naoto clock11前没有新增前冲，不应先假定它会让敌攻击更早。每轮必须重新核enemy实际W44及clock、player外部START20、来袭时仍active20、control/pending与提前释放/受击state；不把context33或“提前六帧”当固定成功断言。

本次只给出候选：若20先START，随后W44造成真实control并在W31首Hit前终止本招，可记该准备段中断；若未START、敌换招、20已先命中/进W12，或原流程未被打断，各自按实际另记。063是闲置玩家受击基线，不提前计20中断通过；后续CPU连段及恢复仍保留独立窗口。未执行建议输入、未修改生产或模板。
