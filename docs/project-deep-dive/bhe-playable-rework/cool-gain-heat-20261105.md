# Sou クールゲイン主动扣热恢复（2026-11-05）

本轮在已完成冰片回收的Cool Gain修复器内恢复主动扣热。真实RED为10项／6失败／0错误／4通过；实现后10项及相关六类52项全部GREEN，独立review未发现阻断。新的三体热修包尚未构建或实机验证；case10当前运行的仍是未含本热修的两体旧包。

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

待验证：构建并静态核对新三体PAC身份；沿相同普通输入基线，实际观察P1期间的主动扣热及每实体金额，分别记录正常结束、parent退出／中断、重复使用和自然冷却。当前运行中的case10只能提供两体旧包对照，不能补作新热修验收。
