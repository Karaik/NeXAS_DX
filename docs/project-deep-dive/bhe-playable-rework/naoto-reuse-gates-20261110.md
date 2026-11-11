# Naoto20中断后再起被拒：064/065原生门只读核查（2026-11-10）

064/065已证明首个20在W31 clock6被W44真实打断；第二次A虽然存在却没有START。**现有字段不足以唯一认定拒绝原因。首要未采字段是heatState（actor+0x18E8），不是heatRaw。** 原生RESET处理可重新进入heatState3，而普通ABC启动在该值为3时直接拒绝；下一次负热更新才会清状态，即使热量已经是0。不能凭一个RESET、state0或普通门1推断所有启动门都已放开，更不能据此改技能/EXE。

本轮只读case16两份采样、限定tick窗口的events.jsonl、062模板的既有装备身份和当前原生反编译；未操作UI/进程，未新增观察钩子或字段，未修改生产/已有reaction诊断、构建或提交。

## 1. 已证事实，按两轮各自闭界

| 录像 | tick范围 | 采样文件 / SHA256 | 接受与拒读 |
| --- | --- | --- | --- |
| 064 | 279129～279489 | naoto20-kou-interrupt-reuse-sampling.json / `c6c4c77208a73e324ff2b096ec2f45456686dd3e8179b226ff2c37e8ec8f906d` | 813689B，360行，缺counter76；26拒读 |
| 065 | 309523～309882 | naoto20-kou-interrupt-late-reuse-sampling.json / `1e5ac34e7b97b0db977f7969295194f1b4762b2e8a18e0a11a777c952e2a549e` | 801001B，355行，缺86/156/189/210/211/321；29拒读 |

文件均在`target/bhe-playable-runtime/nine-naoto-20261110-16/`，status=complete；缺counter不补造。

两轮首A[27,31)实际START20/USE rawSlot3；RPM28已W31 clock0，nativecontext33受伤后control16/pending1，34在W31 clock6早释并请求受击state7。原37f自然结束及11～16f首Hit尚未到，确实是startup中断。稍后context52有7→0请求，RPM53为active-1/W0/state0/control0/normalGate1，fall100。

两轮热量相同：counter53=2000，54=1600，55=1200，56=800，57=400，58起=0。native RESET都在context66，usedBefore1/chainBefore1、heatRaw0。064第二A56..60有新edge但未START；065第二A67..71也有新edge但未START。065对应原生事件3074 RESET、3075 A67、3077 A68 held、3081 A71松开；每轮仅首27产生START/USE/新热费。

065接受样本66～69均state0、control/pending/reaction0、normalGate1、heatRaw0、active-1。玩家与敌距已约270，**不能归咎只有近距槽**：062/063模板末级A/B/C=20/21/22、其它普通槽空，且第一次原生USE rawSlot3已证明A20经末级回退选中；61BF20从远距类别2向后仍能找到类别3。

两轮counter66已有敌4/8弹体；065首次后续光弹1伤发生nativecontext69，RPM70才见HP1932/fall20，此前67/68尚无新damage/reaction。故69之后的持续弹体压力、hitstop或再次受击不能倒填成67 edge未启动的已证原因。它们会干扰更晚窗口，不能无界延迟试到碰巧出招。

## 2. 选招、门位、RESET与热状态的区别

以下文件在`src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/`。

1. `61E310.c:47～60`用actor实际edge `this[1387]`的0x100/200/400选择A/B/C；它不是直接读取Replay文件中的held。60行起若`this[1594]==3`，直接拒绝普通启动。80行另有heatState1与配置/flag条件，104行还会按identifier bit1筛候选；不能把heatState只理解成热量是否大于0。
2. `61BF20.c`先拒绝虚函数+300或state11，再按当前类别向后查槽。它通过`608CE0.c`读取所选运行槽对象`+88`的可用byte，不仅检查MEK存在与热费。`61E310`最终还要求normalGate1、有本次A/B/C edge且候选slot非负，才写入待启动武装。
3. `607C00.c`在使用时把运行槽`+88`置0；`6097D0.c`清链后调用`608C40(manager,-1,1)`，后者逐槽把这个byte恢复1。因此RESET通常恢复再用资格，但**不代表同一时刻热状态、输入接收和调度都允许START**。
4. `61C1C0.c:16～17`先调用6097D0，然后写`this[1594]=3`。`620130.c`在manager的相应计数/等待满足时走61C1C0。另一方面6097D0也可能由完整状态重置61DA50调用；目前RESET观察点只在6097D0入口，没有调用栈，不能仅凭事件名断言064/065每个调用者及退出后的值。
5. `61CD20.c:274`的真实受击分支也会置heatState3；`621820.c:75～117`在heatState3/4时，即便heatRaw0也可执行负热更新；`621780.c:23～28`在负热后夹到0并清heatState为0。是否已发生这次更新、相对输入检查的实际先后、训练对象的冷却许可谓词，均未在当前采样中直接记录。
6. held本身不是每帧重试edge：065在67只有一个新A边沿，68～70仅保持。若67边沿恰被heatState3/其它门拒绝，之后门已开也不能直接假定这段held会重试成功。064的A56也在热未清且RESET之前，不能用后来的58热0倒推56本应成功。

因此有证据支持的**首要机制假设**是：再起edge遇到RESET/受击后的heatState3，或其与实际角色输入/更新时序的边界；没有依据宣布永久武装泄漏。当前不能排除角色实际输入未到、短停止调度、槽可用byte或其它普通启动门。热量/普通门/受击状态这三个已采值没有充分排除这些门。

## 3. 最小只读字段建议，先补现有actor快照

若根决定后续扩展采样，只需先加下面四个固定actor字段，不必读整套游戏结构、不增加EXE钩子或写状态。继续沿现有身份、tick/counter、环一致性和有界失败策略。

| 字段 | 相对actor offset / 类型 | 原生读取/写入依据 | 要回答的问题 |
| --- | --- | --- | --- |
| heatState | `0x18E8` / int32（this[1594]） | 61E310、61C1C0、61CD20、621780 | RESET后是否3；edge到来时是否仍被热状态门拒绝 |
| hitstopRaw | `0x160C` / int32（this[1411]） | 61CD20:148～149与停止相关更新 | 是否存在当前字段看不出的停止；不把它当control |
| actorInputEdge | `0x15AC` / uint32（this[1387]） | 6221E0:97～106拷普通输入；61E310:47 | Replay A edge是否进入这个角色 |
| actorInputHeld | `0x15B0` / uint32（this[1388]） | 同上；61E310类别选择 | 当前是否只是held，没有新edge |

建议连续保留counter50～75及必要前后文。RPM是有守卫的非原子读，而且当前工具每counter保留最新接受行；**某行heatState0不能反证同counter更早的启动检查没见过3**，同样不能因未采到短暂edge就断言没有输入。native frame和RPM counter继续分列。

只有四字段仍不能分辨时，再考虑第二层读取：manager=`u32(actor+0x154)`，reset等待=`i32(manager+0xA8)`、used/chain=`+0xCC/+0xD8`；运行槽集合位于manager+0x14，其begin/end在manager+0x20/+0x24，按`616950.c`界限定位**rawSlot3**对象，再读对象+0x50的实际MEK号、+0x54槽号、+0x58可用byte。必须确认对象的weaponId=20/slot=3再解释；不能拿weaponId20直接当运行槽下标。此处仅给源码定位，不要求立刻增加指针遍历。

若heatState实际为1，再按该分支补actor flags `0x1598`（this[1382]）和全局`0x875170`；已有protectionFlags0x17C4不是同一个字段，不能替代。没有证据前不扩大字段范围。

## 4. 一次有依据的普通输入对照

保持相同062正常UI模板、相同首A[27,31)，把第二次边沿从67仅后移一次更新为**A[68,72)**。理由是：有明确66 RESET边界、67可能尚在重置后的门状态；68比它多一个更新机会，而旧065后续光弹首次命中在69，仍可尽量将启动问题与后来弹体分开。最好同时带上四字段，核真实START发生在新来袭之前或之后，不以计划帧号预填成功。

这是一轮有界对照，不是“68必定可出招”结论。若有START20，结合实际heatState/edge/slot证据再解释67失败；若仍无START，就按已读字段裁定具体门，不继续逐帧扫描。即使这轮再次被CPU打断，只要先有独立START/USE也说明再起入口可用；完整再起收尾另按实际记录。若门因持续CPU压力根本没有稳定机会，可另做正常UI新模板/行动条件的独立恢复对照，但不得把它冒充同一065中已复用成功。

本报告只定位已证/未采边界。没有依据把RESET之后无START直接定为新招代码错误，也不建议强开normalGate、清heatState、改EXE或重写Replay seed来“修复”验收。

## 5. 四字段实测与下一帧对照（11/11固定）

前四节保留调查时的已证/未采边界，以下为随后实际执行结果。现有采样器在原0x1940读取块内补heatState／hitstop／actualEdge／actualHeld四项，不新增RPM范围或权限、不改接受逻辑；针对性测试先以缺字段失败，补字段后24项全部通过，独立复核偏移、符号和无重叠。旧065原采样没有四字段，未回填也未覆盖。

先原样正常UI重播065，新采样`naoto20-kou-reuse-gates-sampling.json`共361行无缺、19拒读；native tick385522～385883。首START seq9/context27仍被W44在W31clock6打断，RESET66后counter67实际heatState3而heatRaw0，counter68已为0并读到actor edge256；A67仍没有第二START。接着066仅把第二A[67,71)改成[68,72)，其余357组输入和原813B前缀相同：361行无缺、29拒读，tick420057～420416；首STARTseq10仍相同地被打断，实际RESET66、短heatState3之后，native frame68真实第二STARTseq11/USE0/0，RPM69也见active20/actualEdge256。

066第二次W31遭受12次control0的小伤但动作未中断，未进入W12，最终context120在clock37自然释放。之后context136才受硬直攻击，20当时已结束；native288／RPM289再次恢复地面待机。尾360玩家active-1/state0/control0/Z/Vz0、G50、倍率100、heatRaw/heatState0，但TAMA仍1(group4/8)且CPU继续活动；只能认可此条件中断后真实再用与该武装自然结束，不认整场对象归零或所有阶段通过。

配对证据为`src/test/resources/bhe-runtime-evidence-20261110/naoto20-kou-reuse-gates.json`，60071B／SHA256 `93f6ce6ce3f918ef6ebda3271b5d254918a372599e38b8e465cdf2d86d2885ba`。阶段采样与原生路径支持“热量0、RESET已发生仍不足以推断边沿可接受”，并有A67失败/A68成功的直接对照；没有在启动检查点抓取调用栈，不升级成所有失败都只由heatState引起。没有修改技能/EXE绕过此门。
