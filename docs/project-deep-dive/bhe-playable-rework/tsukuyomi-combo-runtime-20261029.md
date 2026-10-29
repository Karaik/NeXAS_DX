# 月詠新「袈裟・一閃」普通跨武装连段实测

当前固定甲训练靶已完成三个独立武装8「一威」→19「袈裟・一閃」→2原「袈裟」的连续命中，90+31+30=151伤害；正常清链后的第二轮再次造成151，同一录像累计302。两轮后两次伤害前目标控制都分别为56和49，真正恢复在各轮最后伤害之后。此次只调整普通输入时序，没有修改角色逻辑、游戏数据或强制受击。较晚第三招的081失败记录保留，不用成功候选覆盖。

结论限下面的包、实际出装、固定甲靶和已观察输入。086已补齐清链后再起、整条播放和自然收刀最终状态；中断、不同距离／体型／移动靶等仍须单独记录，不由这一条成功路线宣称月詠全部武装或九角色整体完成。

## 身份与正常UI模板

使用131244包：buildRoot `C:/NeXAS_DX_runs/builds/run_20261029_130952_143`，EXE SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`，PAC `a94c6c3550e498fb03cda5c949e06cb93e8b48ce576fbcacd0c5fca3501297b4`。case沿用`C:/NeXAS_DX_runs/runtime/motoki-target-drop-20261029-03`，本节实际玩家为月詠104、敌甲0、地图68，不因case名称误归Motoki，也不跨录像混用同地址的actor生命周期。

最终只读资源在`sou/sou_assets_20261029_131212_949`：

| 文件 | 字节数 | SHA256 |
| --- | --- | --- |
| Tsukuyomi.mek | 59875 | 767447b303a59788a106fef495179ad45e38e26ff463dc7e252648a484b0084d |
| tsukuyomi.waz | 116085 | c6159fde44053e058a1cc8446802c69f2c59890589737030d6cbc4531c367c94 |
| tsukuyomi.spm | 198576 | 0ac711dae94ff9b1638dfa21ba3df1fa102025e7d2e28679b3dcca3299962006 |

根代理通过正常UI选择角色、装备并保存空模板Replay080，SHA256 `c0423607de022e875c673f55e1c68ca4a822c5abe4877af541656f9820c7d56d`，3027帧全零输入。独立解析实际14槽为：

```text
[8, -1, -1, -1, -1, -1, -1, -1, -1, -1, 19, 2, -1, -1]
```

即**近距A8、普通Dash B19、普通Dash C2，其余空**。这是运行模板的出装，不把早期静态候选中的近距B/C当作实际设置。080和生成录像的header及前两压缩块完全保留，工具只生成第三块普通edge／held；没有写武装START、actor状态、敌控制或坐标。

target证据目录为`target/bhe-audit-20261026/tsuk-combo-review-20261029/`，包含最终资源只读解析、录像／receipt／输入副本、原log已完成输入段和独立JSON。根代理执行UI/CDB；报告作者只读证据整理，未运行UI、CDB、Maven或Frida，未改生产、Save、正在使用的输入、其它progress／README，也未提交。报告初次冻结时原全log仍可增长；2026-10-30恢复核验已确认case03于前一日20:02:31以code0退出，闭合全日志另存于`target/bhe-audit-20261026/old-case-closure-20261029/`，本报告各输入段仍按原字节区间对应。

## 输入JSON换行与原receipt字节

fixture生成录像后，根代理将临时输入源JSON统一为CRLF。因此，原receipt的`input_json.sha256`对应**生成时的LF原字节**，不能直接用它判断当前CRLF源JSON的字节hash；原receipt的`output.sha256`仍对应原Replay，录像未因这次文本换行整理而改动。以下当前源文件均在`target/bhe-audit-20261026/`，085为`tsuk-early-followup-085-input-20261029.json`，086为`tsuk-repeat-086-input-20261029.json`：

| 录像 | 原receipt记录的LF字节SHA256 | 当前源JSON的CRLF字节SHA256 |
| --- | --- | --- |
| 085 | 7b7a461b98e77f25a40695c48ea13d42fe554bc0eeff29a4dbb3627db2c2b20f | 70049042a2b27b0b2ce23abf57e823d380d62ed3391fe526e4493ceba69b75fd |
| 086 | 823871713d3f6cd4cd5fe7f9270fd9a84b875d0bb54a95a9e0d02848a2b555cf | 8b00ddcdb0aadec07a47112c507c3acbe7ea4c18573c1bffaddaefc5e74cd1b5 |

`target/bhe-audit-20261026/input-newline-evidence-20261029/audit.json`记录七件换行证据；每件按原receipt SHA严格重建的LF原字节排他保存为相应`*.recorded-json-bytes.bin`。本报告作者独立逐件核对了这七件：binary副本SHA等于原receipt记录；当前源字节恰为对应LF字节逐行转换成CRLF；JSON解析值相同；两种字节解析后经`compile_inputs`生成的普通输入均与真实Replay第三压缩块解压结果逐字节相同，前两块仍与各自模板一致。

月詠085／086还单独核对了真实录像与本目录录像副本，SHA分别为`e74538af7593ca42c17d508308e5f8c90760e9033870fa15cb69f92cc8a7c2ee`和`ed2d2684e0c54770c268ccbe074641d0a40202b80cd9fb454d5f8c7727a5b6e7`，均等于原receipt的输出hash。本目录较早冻结的两个输入JSON副本仍保留原LF字节，属于历史快照；它们与上表的当前源JSON是不同路径，不能混同。新的独立核对见本目录`input-newline-independent-audit.json`与`verify_input_newline_sidecar.py`；本轮没有修改旧receipt、录像、输入源或历史副本，也没有重写旧审计中当时记录的hash。输入换行修正不改变下面的实际运行结论。

## 081失败：第三招已START但miss

`TSUKUYOMI_REPLAY_TRIALS_BEGIN`之后的081输入段限定原log字节`[300374,302543)`，SHA256 `f46bca8ac7728f820f68d12afd5707763ae1972f3bcde1fe3f15118d1d78b50d`，到RESET253结束。

| 帧 | 事件 |
| --- | --- |
| 30／50 | START8，W20 block1 clock7造成90；玩家2354,1808,0，甲2402,1808,0 |
| 54／64 | START19，W32 block0 clock10造成31；**命中前control56、state7**；玩家2481,1808,0、甲2410,1808,0 |
| 86 | 独立START2，但后续0伤害 |
| 126 | 同一甲actor235ad430真正恢复，HP5879 |
| 223／253 | 武装2经W12抜刀中后段释放，随后used3/chain3 RESET |

这只证明8→19连续命中，总121；第三招miss不能被三START／used3掩盖。窗口计划里多次B/C上升沿没有生成更多独立武装，也不额外计入连段。081原录像、receipt和失败片段完整保留。

## 085成功：提前第三招，三武装连续命中

085只保留三段普通held：A30..32、B54..56、C66..70，420帧／tail120；所有区间[start,end)。它把第三招从86提前20帧，不改已成功的前两招或资源。根代理正常UI载入／播放，截图STOP7.00为3HIT/151；独立日志闭合为：

| 事件 | 实际帧 | 伤害／HP／目标状态 |
| --- | --- | --- |
| START/USE8 | 30 | chain此前0，内部结果槽0 |
| HIT8 | 50 | 90；6000→5910；首次命中前control0 |
| START/USE19 | 54 | chain此前1，内部结果槽7 |
| HIT19 | 64 | 31；5910→5879；**precontrol56、state7** |
| START/USE2 | 66 | chain此前2，内部结果槽11 |
| HIT2 | 76 | 30；5879→5849；**precontrol49、state7** |
| ACTOR_RECOVERY | 131 | 同一目标226ce028，state7/control0，发生在三次伤害后 |
| RESET | 233 | used3/chain3，恢复以后才清链 |

三次Hit分别附带真实玩家MEK weapon8/19/2，WAZ W20/W32/W14及实际clock。最后一击玩家2439,1808,0、目标2361,1808,0，W14 clock10。第三Hit不是新19自己的内部派生，也不是回收动画或单招预览。

085完整输入段原字节`[302544,304759)`，SHA256 `4ad860551af141a2010f3ac9a11a154d3dfd21cbb739b895d712c85874cd45f1`；本目录`replay085-success-and086-pending-audit.json`记录source/copy hash、事件和模板保全。其标题中的086 pending是当时的历史状态，后续若补验使用新文件，不改085成功片段或081失败片段。

## 为什么不能只按新19的15帧控制算断链

W32继承原袈裟的Noke控制15/15，而前技一威为60/60。原生`61CD20`只在当前actor控制计时小于本次新控制时增加，不无条件把较长控制覆盖成15。因此081／085的64帧Hit前实时control56、state7与后续76帧仍49是可信的实际结果；不能以静态15简单推算“已经断连”。真正恢复入口也在第三次命中之后才触发。

081第三招理论Hit96..97仍早于恢复126，但根本未命中；它不能被解释成“有后招伤害却目标先恢复”。提前第三招后的真实命中支持时序／空间调整有效，而不需要先增加硬直来凑连击。

## 运动、朝向与打点校准

最终MEK2→W14与新19→W32均单block／单suffix `(0,0)`，共享SPM group63「袈裟」，有效对机Hit仅10..11；不是一整段刀光都带攻击。原2的8..11速度窗口从24插值到0，新19从32到0；19在38f改跳納刀W13，原2在38f回抜刀中W12。081的2已于W32 clock31真实起手，不能归成38f自动收刀抢掉了输入。

一威首Hit50时玩家在甲前48，到了新19Hit64时已在甲后71。其前技W20 block1还在Hit7之后的8..10f带64→8强前冲，释放54时clock10已经过该段；越靶不能全归给新19的32峰值。随后再等22帧才出原袈裟、两帧攻击窗及惯性都可能改变打点；081第三2打点的实际方向／选页并未捕获，不能宣称已隔离唯一miss原因。

本次有一处明确校准：初稿曾把slot15的`root18/type[3,0]/aux3[0,0]`说成“朝目标方向”，这个解释错误。真实Term.grp是ANGLE→ANGLE_SEL.SPRITE，`669CD0 case3→669540 case0`读取**自身sprite方向actor[1453]**。它用当前朝向作为运动向量，并没有从目标XYZ重算追踪角；原先说法已通知根代理并在target报告中更正，旧稿另存，运行事实不改。

slot10在0f给sprite转向增分300，native60A810除10为约30度／逻辑步最大转角；7f归0后停止这一转向增分。不能把300读成一帧强转300度，也不能把自身SPRITE表达式当成自动追踪。SPM同组实际是有限有向矩形、带方向／镜像处理，不能靠刀光或资源页存在推定必命中。085只提前C的成功说明当前正常机制已有可用衔接窗口，尚未证明其它角度／速度设置应怎样修改。

## 086实测：正常清链后的第二轮与最终收尾

086沿用同一080模板，900帧／tail240，普通输入首轮A30..32／B54..56／C66..70，第二轮A450..452／B474..476／C486..490；区间仍为[start,end)。Replay SHA256 `ed2d2684e0c54770c268ccbe074641d0a40202b80cd9fb454d5f8c7727a5b6e7`，模板前两压缩块、输入edge和receipt的Replay输出hash均已离线核对；输入JSON本身的换行hash按前节区分。根代理通过正常UI载入播放并观察STOP15.00；报告作者独立核原log中这次086的两轮事件和本次最终读回，未自行操作UI。

| 事件 | 首轮实际帧／结果 | 第二轮实际帧／结果 |
| --- | --- | --- |
| START/USE8 | 30；此前used0/chain0 | 450；此前used0/chain0 |
| HIT8 | 50；90，HP5910，precontrol0 | 470；90，HP5759，precontrol0 |
| START/USE19 | 54；此前used1/chain1 | 474；此前used1/chain1 |
| HIT19 | 64；31，HP5879，precontrol56/state7 | 484；31，HP5728，precontrol56/state7 |
| START/USE2 | 66；此前used2/chain2 | 486；此前used2/chain2 |
| HIT2 | 76；30，HP5849，precontrol49/state7 | 496；30，HP5698，precontrol49/state7 |
| ACTOR_RECOVERY | 131；最后命中之后 | 551；最后命中之后 |
| RESET | 233；used3/chain3 | 653；used3/chain3 |

六次命中均为本录像同一甲actor `226ce028`；HP6000→5849→5698，单轮151、累计302。第二轮450帧前已在233正常清链，使用计数重新从0/0开始，不把上一轮残留计数当作重复成功。第二轮的相对方向／几何已反向，三招仍分别实际命中；这支持当前固定靶场景的重复，不外推其它起始距离和移动目标。

本次播放结束后的独立标记为：

```text
TSUK_086_FINAL meka=104 active=-1 group=111 skill=0 entry=0 block=0 Z=0 Vz=0 Graw=51200 jump=100 fall=100 heat=0 TAMA=0 SCRIPT=0
```

它对应月詠104地面STAND、无活动武装，垂直速度0、标量G50（raw51200）、跳跃／下落倍率100、热量0、TAMA/SCRIPT均0；不是借用085或其它角色的终态。两轮均经过W12抜刀中后段释放，最终已回待机。

新增排他冻结片段`replay086-completed-playback-and-final-segment.log`限定原log字节`[304760,310127)`，5367字节，SHA256 `b034ba45882578973ea3c4c28e6815a294b9a8989d6497ada9158f5add6be631`；`replay086-completed-final-audit.json`保存独立事件、HP与最终读回核对。原全log仍可增长，不称全log闭合。此前`replay085-success-and086-pending-audit.json`及081失败证据均保留；本报告原pending版本另存为`runtime-doc-before086-final.md`，没有覆盖历史审计。

Replay082／083／084均为420帧、前两块匹配080，**只生成、未执行**；其hash和状态列于本次新JSON，不计入运行覆盖。

目前已证固定甲靶085单轮及086清链后同路线重复，含整条播放和最终收尾。落空／中断、体型与距离变化、完整角色／九角色及加载关卡验收仍未由此证明，整包`runtimeValidated=false`。
