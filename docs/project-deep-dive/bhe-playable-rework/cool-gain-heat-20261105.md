# Sou クールゲイン主动扣热恢复（2026-11-05）

本轮在已完成冰片回收的Cool Gain修复器内恢复主动扣热。真实RED为10项／6失败／0错误／4通过；实现后10项及相关六类52项全部GREEN，独立review未发现阻断。三体热修已于11/05完成构建及静态核对，11/06 case12的前两次Replay013已观察到自然冷却前的主动扣热；完整RPM证据来自第二次播放。case10为已经结束的两体旧包基线，不能与新包混证。每实体恰一次的原生调用、受击中断及其它玩法仍未完整验收，build receipt继续runtimeValidated=false。

旧包基线case为`target/bhe-playable-runtime/sou-procter-recovery-20261105-10`，PID923276、创建FILETIME `134383376478515689`。manifest指向`run_20261105_162002_445_28580c2f`，EXE SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`，PAC SHA256 `f8e520d0cc258a05b465d00d0fa8e9b17f314a09d2ab1d1a3e93503ca56fe2ef`。这份包已有private body542／ice543及Boost Procter修复，没有第三份私有cooling。

普通输入基线为B `[30,34)`、A `[90,135)`（保持45帧）、C `[150,154)`，总计600帧。`sou133-heat-baseline-input.json`为345字节，SHA256 `d05e7ac8e5dc69fe6049e1c0a4a7f8a441c1fe108b45390f95abb1e12931ea2e`。既有receipt记录从正常UI模板Replay010生成Replay013，头部及前两个编码块保持一致；本记录不读取或复制Replay／私人Save正文。

`sou133-heat-baseline-sampling.json`为106467字节，SHA256 `5b7cb9570d3f5aaf452f86e93605910faf7e31dfe3ba5ff158f5a8acbbfdde12`。保留counter0..600共601条采样，每个被观察counter保留最近一次一致读数；38次拒绝包括1次player uninitialized、32次counter changed、1次changing list、4次count changed。采样没有暂停进程，字段并非原子快照，counter也不是原生输入帧。

所有保留样本的capacity均为8000 raw，不能按1000上限解释。原生日志确认B在native frame30起手weapon0，A在frame90起手weapon133，C在frame150起手weapon2；热量采样依次为300、2300、3000。

| RPM counter | tick | heat／heatState | 两体旧包本体采样 |
| --- | --- | --- | --- |
| 34 | 506840 | 300／0 | 首次普通武装之后 |
| 94 | 506900 | 2300／0 | C_GAIN03起手之后 |
| 106 | 506912 | 2300／0 | 私有body P0 clock0、scalar7 |
| 136 | 506942 | 2300／0 | P1 clock0、scalar210 |
| 154 | 506960 | 3000／0 | P1 clock18、scalar192，后续weapon2已起手 |
| 378 | 507184 | 3000／0 | P1 clock102、scalar0 |
| 379 | 507185 | 3000／0 | 已转P2 clock0 |
| 400 | 507206 | 3000／0 | 本体消失，TAMA0／script0 |
| 476 | 507282 | 3000／0 | 本体已不存在 |
| 477 | 507283 | 3000／3 | 首条进入自然冷却状态的保留样本 |
| 478..484 | 507284..507290 | 2600→2200→1800→1400→1000→600→200／3 | 无本体 |
| 485 | 507291 | 0／0 | 最后200夹到零 |

native `combat_reset`位于日志10968行、tick507282，`lastReplayFrame=476`且`replayReadTick`同tick，事件记录heatRaw3000。它与下一tick的RPM counter477／heatState3分别记录，不能混成“native477开始降温”。在本体P1工作、P2收尾以及退出后的保留样本里，heat未见主动扣减；随后400 raw步幅的自然冷却不能记作Cool Gain成功。上述数据属于旧包基线，不是三体热修的效果。

**BHE原义是命中确认后向victim传入-100 raw。** 源Tama05 skill377／`0346`／`★蒼：クールゲイン：熱冷却`的P0 slot46有self零伤Hit：offset1910191、start/end0、short1=6、short2=7、int10=-100。完整1350字节源记录SHA256 `f5849d84b11d3eaba2d0143d2440b8922d2ce818be67b3c6f3cec6fcf57fb26c`已在现有fixer中守卫。

BHE反编译目录`src/main/resources/ida-reverse/bhe/export-for-ai/decompile`内：`739F70.c`约28行按读取顺序把int10存到Hit的`this[12]`，`73A2B0.c:10`返回该字段，`6FE150.c:769–776`沿命中处理把原数传给`6FB790`作用于victim。通用Hit转换没有承载这个字段，因此公共5／529及旧私有本体、冰片中没有对应的主动扣热写入；本轮不修改全局Hit转换器或EXE。

**DX已有可复用的slot40事件语法及root差量消费者。** 原版`src/main/resources/game/bsdx/waz/Bomb.waz` SHA256为`737c114d7f6f521830d559f7baf60b8e66b21d7fb61a66541a117ef65d4b49e4`。其skill23／`0004`／`発光爆風（小）`，P0 slot40 event0是type0 `CEventVal`，start/end0，四整数`[0,0,0,-30]`，无FF。原字节offset76993、长度24，六个小端整数为`[0,0,0,0,0,-30]`，该事件记录SHA256 `1b7a1899df331d905e2ddb06dc7470bf2a90a6ba0abd1fe738115b3e9689290e`；这里只声明事件记录签名，没有冒称量过完整skill23记录边界。

DX反编译目录`src/main/resources/ida-reverse/bsdx/export-for-ai/decompile`内，`610300.c:66–78`处理此槽；槽号另由汇编`0x6103DB`的actor `+0xC08`确认。其累计值与actor `+0x17BC`保存的上次贡献作差：actorType0作用自身，type1／2经root `+0x14C4`调用`621780`。`621780.c:15–29`负值路径减热并夹到0；量纲是raw。`611820.c:29/38`先调用`610300`，45行才判断slot71，49行再提交退出，因此单次0f热事件能先于本实体原0f退出处理。

[SouCoolGainReturnProtocolFixer.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouCoolGainReturnProtocolFixer.java)继续使用现有私有扩展，完整family现在为动态连续尾部的body、ice、cooling三体：

- `DX_SOU_COOL_GAIN_BODY`保留既有充能预算、P0冰片引用、P1逐次消耗、P2零写及20f出口；仅把P1原唯一冷却发射边由公共529换到私有cooling。原0..107f发射窗口、数量1、间隔3及无限FF完整保留。
- `DX_SOU_COOL_GAIN_ICE`保留既有parent变化／parent NULL优先回收协议，行为不变。
- `DX_SOU_COOL_GAIN_COOLING`／`★蒼：クールゲイン：熱冷却・専用`深复制受完整guard保护的公共529，只加入P0 slot40的单次0f `CEventVal [0,0,0,-100]`，不加FF。原self零伤Hit、效果／SFX引用、0f NEXT自然退出及其余全部载荷保留。公共527／528／529不改。

**这是on-activation适配，不是on-confirmed-hit等价移植。** BHE原来在self Hit确认后扣热；本方案在每个私有cooling实体生效时扣热。因此命中确认／碰撞条件不同的场景可能有差异，不能宣称逐命中等价。保留的是-100 raw金额及原实体发射数量、间隔和预算约束；不额外强制命中、添加超时或改变自然冷却。这个语义边界已在批准方案、代码注释和测试中明确。

三体须全有或全无，双名、连续顺序、各自完整payload和caller引用全部验证后才发布main列表、host列表及GRP计数；完整安装重复调用保留对象身份，后续合法私有追加不受影响。旧两体安装明确拒绝，不静默迁移成第三体；反序列化回环只接受当前完整三体family。原公共图、源raw、MEK及已有Sou修复的保全断言保留；纯DX测试基线中与Cool Gain共宿Tama05的Boost私有图也受前缀整图／字节保全，相关Boost八项另行通过。

[SouCoolGainReturnProtocolTest.java](../../../src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouCoolGainReturnProtocolTest.java)现有10项真实资源测试：除原预算、冰片回收、caller帧数、原图保全、深复制隔离、动态尾部、幂等及两次二进制回环外，增加原Bomb23事件字节／writer对照、BHE原-100字段对照、原发射packet数量／频率保全、零下限和高raw热量、累计贡献不重复扣减，以及旧两体／缺cooling／错误金额或时刻／退回公共引用拒绝。Bomb供体仅用于测试证明事件语法，生产不导入Bomb技能；局部算术模型不代替实机。

| 验证阶段 | Tests／Failures／Errors／Skipped | 归档结果 |
| --- | --- | --- |
| 有效RED | 10／6／0／0 | 4项原保全仍通过；6项准确失败于缺cooling、宿主少1、缺扣热能力及旧两体未拒绝 |
| 实现后GREEN | 10／0／0／0 | 98.752秒，exit0 |
| 相关六类GREEN | 52／0／0／0 | Boost8、HumanCannonCounter9、CursedBlade7、SnowMoon9、FlareSeeker8、SouCustomizer11 |

归档位于`target/bhe-combined-fixes-20261105`：`cool-gain-heat-red.xml/.log`、`cool-gain-heat-green.xml/.log`、`cool-gain-heat-related-52/*.xml`及`cool-gain-heat-related-result.json`（汇总JSON在父目录）。独立review无阻断；没有把此62项定向通过当作新包或实机通过。

| 资料 | SHA256 |
| --- | --- |
| cool-gain-heat-red.xml | `7780be7b231d01ee327d4241a6637c28825dea2ce703ea8580aeff40dab86d78` |
| cool-gain-heat-green.xml | `e8aeabadc49162110412c57f527ece3e49824f286bc7392c32333f87ecd403ed` |
| cool-gain-heat-related-result.json | `b3586e3a6541070d7096a19abb521b1dbf5c6cd4232d1b974793810923ce9148` |

## 11/06 case12同输入主动减热证据

新包为`run_20261105_193350_342_bd794139`：EXE SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`，PAC SHA256 `8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641`。身份来自case12启动manifest，与11/05构建交接一致；本次证据整理未重新运行构建或测试。case为`target/bhe-playable-runtime/sou-cooling-heat-20261106-12`，PID19232，创建FILETIME `134383973635809045`。

小型证据为[`sou-cool-gain-heat-runtime.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/sou-cool-gain-heat-runtime.json)，73637字节，SHA256 `fa078fd019be2d51770aa7b4b8356d9376b23d3354f24b23a9d4167cf58075fe`。原生日志固定到第二次播放的RESET，line660／tick34307，前缀276439字节、SHA256 `7912263b5800a80ab6db21e397a1e53695541acca075e1f4be334aab092c9ada`；独立RPM继续到counter600／tick34430。后续同场操作不纳入本记录。完整原生事件对象、各轮字节范围／hash、所选原始RPM行及128次拒读全部留在JSON，完整601行采样原件以hash固定。

Replay013仍为869字节，SHA256 `e5593f9cc4a02205f6afeb3d657102927d5fa7af2bea269b8364406fbbe933ae`，与旧包基线一致。第一轮START tick24846／24906／24966，第二轮33861／33921／33981，均为0／133／2。实际输入frame30／90／150由同tick的`combat_replay_input.frame`证明；其它事件的`lastReplayFrame`只作上下文，不代替输入帧。两轮各有120／2／142伤害，均作用同一敌actor，HP6000→5880→5878→5736。两轮133的normalGate=1释放与weapon2 START同tick，释放heat1900、TAMA17。敌机在133起手前已有恢复，故不认定真连；伤害日志也没有逐Hit弹体归属。

首轮采样`sou133-heat-fixed-sampling.json`为557字节，SHA256 `2d1f161d2219071d2af8c4efcec59477aa59e6a5a4c321d14c3ab89dbf77752f`，started=false、complete=false、lastCounter=-1、0行。root诊断为误把固定consumer地址0x86F700当作指针再解引用；这是采样失败，不能补写首轮曲线，也不是游戏崩溃。首轮仍有独立原生释放heat1900／1200及RESET0的记录。

第二轮修正后的`sou133-heat-fixed-sampling-v2.json`为247330字节，SHA256 `a7261b64154dbe650f67be548cb4a6d39fcf1ae476efa7786c3cbf97ae224d6b`，started/complete=true，counter0..600共601行全部保留在原件。128次拒读为tick/counter/count changed109、ring not closed9、short ring5、player uninitialized4、ring changed1；没有将拒读默认为无错或全部归为同一原因。RPM不是原子快照，consumer counter不是原生输入帧。

| 第二轮RPM counter | tick | heat／state | 原样观察 |
| --- | --- | --- | --- |
| 94 | 33925 | 2300／0 | 133起手加热后 |
| 106 | 33937 | 2300／0 | body P0 clock0、scalar7 |
| 135 | 33966 | 2300／0 | P0 clock29、scalar210 |
| 136 | 33967 | 2200／0 | 首次减100；P1 clock0、scalar210，采到5/544 |
| 140／144／148／152 | 33971／33975／33979／33983 | 2100／2000／1900／1800，state0 | P1 clock4／8／12／16 |
| 154 | 33985 | 2500／0 | 普通C增加700；P1 clock18、scalar192 |
| 284 | 34115 | 0／0 | 第30个减100变化点；P1循环后clock8、scalar94 |
| 378／379 | 34209／34210 | 0／0 | P1 scalar0，随后P2 clock0 |
| 399／400 | 34230／34231 | 0／0 | P2 clock20后本体消失，TAMA/SCRIPT0 |
| 477／478 | 34308／34309 | 0／3，随后0／0 | 自然冷却状态出现时已无热可减 |
| 600 | 34430 | 0／0 | Replay inactive，TAMA/SCRIPT0 |

所有601样本capacity=8000；30个负变化均为-100 raw，合计3000，counter284后直到600均为0且未见负值。主动减热发生于P1及state0，早于477的自然冷却状态，与旧包本体回收后仍heat3000的曲线有明确区别。原packet interval字段3未改；实测负变化对应body clock0、4、8…104，循环后0、4、8，且counter168到204之间body clock只由32到36。不能写成实际每3 native帧扣热，也不能按counter简单推算发射频率。

本轮接受该输入下的主动减热、正常取消和采样回收。RPM中的100步幅与冷却体共现不构成每实体恰一次的native调用验收；不足100余热的运行夹零也没有独立覆盖。on-activation与BHE self Hit确认的语义差异保留。所选两轮窗口没有异常事件，但冻结前缀line425有1条system／unclassified KERNELBASE异常，不称整会话零异常，不自行归为无害。该窗口捕获时case仍running，本记录不声称正常退出或退出时原Save保全。

## 11/06 Replay014同次播放内复用

另存[`sou-cool-gain-heat-reuse.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/sou-cool-gain-heat-reuse.json)，46795字节，SHA256 `4d4da4a371e20d6d32550ca7a7878ba078478986bbf7cccf4092c182b2e9042e`；前述Replay013证据字节及hash不变。原生窗口为line683～855，独立前缀截至第二段RESET tick48148；完整前缀380139字节、SHA256 `ef0cf162c12182d7d4fc64edbb4eebf2bf3b4443e8631f2d64547bc4dffd4aed`。后续自然退出由终态证据另记，不混入本玩法窗口。

Replay014为883字节，SHA256 `dc2b97746dcf2672393151bd9c3587aac3cc46c81c9be426ff750cd9d17dc9ec`。本次独立只读解压核对与正常UI模板010的头部、前两编码块一致；第三块1200组普通edge/held输入逐帧与`sou133-heat-reuse-input.json`一致。输入为B30～34、A90～135、C150～154以及B630～634、A690～735、C750～754（左闭右开），没有写actor或强制技能状态。

六次START seq7～12对应tick47102／47162／47222／47702／47762／47822，均与原生`combat_replay_input.frame`30／90／150／630／690／750同tick，武装0／133／2重复两次。两次133→2均normalGate=1，取消时heat1900；两次C释放heat1200，两次RESET heat0。第二次普通B起手时TAMA/SCRIPT均0、heat0，说明前段后可再次正常起手；本窗口没有完整1200-counter热曲线，不能把Replay013曲线冒作该轮的逐帧结果。

第一段伤害120／2／142合264，敌HP6000→5736。第二次B窗口未见伤害；133再次启动后、C启动前出现3+3+3+3+1=13，C启动后150，第二段合163，末HP5573；所有伤害为同一敌actor，HP算术一致。这里按启动时间划分伤害窗口，没有逐Hit弹体归属。不能为B补算120，不能把同输入称同伤害或真连。

`sou133-heat-reuse-final-snapshot.json`为657字节，SHA256 `1e844b62c63f23e525868e053762fc71b1a07b85871c8cf65d6c6f98cf490eaa`；已核PID／创建FILETIME／image与本case一致，tick=endTick=51526、tickStable=true。该稍后只读快照中counter1200、replayActive0、heat0、capacity8000、activeWeapon-1、TAMA/SCRIPT0。它支持该观测时点已收尾，不是native READY，也不是恰好录像结束的tick。

本次接受同次播放内的再次使用、两次正常取消及最终采样回收；受击中断仍未测试。待验证：每实体恰一次的原生调用、低于100余热的运行夹零、parent退出／受击中断，以及所需方向、距离和变体。全部原目标完成前runtimeValidated仍false。


## case12自然退出与终态保全

终态独立存为[`sou-cool-gain-heat-session-end.json`](../../../src/test/resources/bhe-runtime-evidence-20261106/sou-cool-gain-heat-session-end.json)，50885字节，SHA256 `37b54b56fed4e4f6f68499bfb863d3867a2398737d0a6831e6a91d1c3bff0ad6`。两个玩法证据的原hash及其冻结日志前缀均复核不变；没有把终态或退出异常回填进早先玩法窗口。

state在2026-11-06T00:25:22.905129Z记录`exited / failure=null / exit_code=0 / stop_reason=null`，owned_process_exit_confirmed与guard_cleanup_complete为true，cleanup_pending=false。28份受管game输入副本共5,805,717,300字节由harness核验后清理，retained_changed_or_unique和errors均空；本次只读核对列出的28路径均已不存在，没有执行删除。原Save、原base及源EXE/PAC的终态harness检查均未变；这里不冒称重新读取外部Save或独立重hash大型二进制。

全会话日志为421337字节／913行，SHA256 `8848ea5f6cb297de0800a9c29c6955a34b734844a4158177528b6f091a0f13ba`，保留55条system／unclassified异常的完整原始对象；1条位于早先前缀，54条出现在Replay014冻结窗口之后。正常exit0不表示零异常，也没有将这些异常自行归为无害。external_write_count=0、guard_violations=[]仅按监测范围报告。原驱动状态`nvAppTimestamps`前后有漂移，attribution=`not_determined_external_writers_possible`；不称C盘任何文件均未变化，不将漂移归因于游戏。

本次关闭测试会话不等于完成整个目标。runtimeValidated继续false；受击中断、每实体调用以及前述剩余玩法验收仍保持未完成。
