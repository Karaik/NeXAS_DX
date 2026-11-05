# Sou ブースト・プロクター冲刺持续修复（2026-11-05）

Sou122对应的三等级私有本体修复已完成，有效RED为8项失败、0错误；实现后8项GREEN通过（88.127秒，进程exit0）。当前尚待六类既有Sou共52项回归、包含本修复的新封包及修后实机验证。本记录不把原包的正常结束或Java通过写成功能验收通过。

旧包实机证据为[sou-boost-procter-premature-end.json](../../../src/test/resources/bhe-runtime-evidence-20261105/sou-boost-procter-premature-end.json)，19498字节，SHA256 `56cdb20f8de089ab76a22ed48205513845f3ac5fe1016dbe41c60546475adc65`。现场为`target/bhe-playable-runtime/sou-direction-fix-20261105-09`，PID811768、创建FILETIME `134383236752109730`；EXE SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`，PAC SHA256 `959f3627189039b23686e2306f2b38b50c6c87fb9599b0d5a1548100e2ae42bf`。该包没有本轮源码修复。

通过正常UI装备近A122、Dash B0／C2后保存Replay005作为模板，仅替换普通输入块得到600帧Replay006。输入区间为A `[30,34)`、D `[150,190)`，保留头部与前两个编码块；未强制调用武装起手。Replay005为855字节，SHA256 `6c1d11f68ef1c11d8742218435b8d826ca274d0a2b43f1021719fe27bb1aad62`；Replay006为862字节，SHA256 `05001af5d6e03250b79e3a01c5759d221c58a4b61f8bd26d1756a6848d83004a`。这里只引用已归档的结构／哈希，不复制Replay或私人Save内容。

**原生输入帧与RPM采样counter分开使用。** `events.jsonl`自byte3848136起的相关原生日志已核对；普通事件只在`replayReadTick`与事件tick相同、consumer匹配时使用对应输入帧。训练START283中的`requestSeq273`为保留的菜单标签，不是一条新菜单REQ／READY链。

| 原生日志 | native frame | tick | 已观察事实 |
| --- | --- | --- | --- |
| A按下／START283 | 30 | 291612 | edge=held=256，weapon122，previewMode=0，actor 0x24066160 |
| A释放 | 34 | 291616 | held=0 |
| D按下／motion transition | 150 | 291732 | edge=held=2048，root state 0→3 |
| D释放 | 190 | 291772 | held=0 |
| motion transition | 240 | 291822 | root state 3→0 |

只读采样文件`sou122-v1-tama-sampling.json`为3166字节，SHA256 `7e29aedcb57a69e08a087aa862d8df081a1a0b96cc6a2f5a2ad825244aa9d84e`。工具报告成功覆盖counter0..600共601个值，存储8条变化行；另有5次校验拒绝后重采样，原错误字符串均为空，不能统一归因为torn read。JSON的`frame`字段是消费者counter，可能已指向输入消费后的下一帧；读取时未暂停进程，单行也不是原子进程转储。

| 采样counter（JSON frame） | tick | root state | TAMA／本体状态 |
| --- | --- | --- | --- |
| 43 | 291625 | 0 | group3／skill110，本体P0 clock0 |
| 49 | 291630 | 0 | 本体P0 clock6；另见group4／skill256 |
| 151 | 291733 | 3 | 本体P1 clock0，TAMA1 |
| 152 | 291734 | 3 | 本体已P2 clock0，TAMA1 |
| 193 | 291775 | 3 | TAMA0，根机体仍在冲刺 |
| 241 | 291823 | 0 | TAMA0 |
| 600 | 292181 | 0 | 回放停止，TAMA0／script0 |

采样汇总与保留状态未观察到group4／skill257；不宣称能排除非原子读取之间任意短暂对象。P1在相邻counter已转P2，与源码中冲刺期间立即满足退出条件的失配相互印证，但采样本身不是指令级“一帧寿命”的独立证明。P1原定在局部clock10发射横向子体；不能将counter152重命名为该技能的native frame152。窗口内无伤害事件、异常或guard violation，不代表整个session零异常，也不把最终TAMA0当作机制正常。

真实资源关系如下；下面的公共目标编号属于该JINKI继承实机包，正式修复不把这些编号写死。

| 等级／资源 | BHE源 | 实机公共目标／时点 |
| --- | --- | --- |
| Lv1 caller／本体 | sou414 `B_PROCTER01`／Tama03源61 `BIT61` | 24f发group3／108 |
| Lv2 caller／本体 | sou415 `B_PROCTER02`／Tama03源62 `BIT62` | 18f发group3／109 |
| Lv3 caller／本体 | sou416 `B_PROCTER03`／Tama03源63 `BIT48` | 12f发group3／110 |
| 縦子体 | Tama04源145 `0119` | base111+145=256，body P0局部5f |
| 横子体 | Tama04源146 `0183` | base111+146=257，body P1局部10f |

**P1已有正确退出条件，失配的是并列的END分支。** 三个原本体P1的CHANGE均为：

| 顺序 | 原语义与时间窗 | 转换后的情况／本轮处理 |
| --- | --- | --- |
| ① | 0..149f，root NOT SDASH AND NOT BDASH → NEXT | 语义正确，完整保留 |
| ② | 0..149f，root SDASH_END OR BDASH_END → NEXT | fallback成正在SDASH OR BDASH；只从各私有副本删除此条 |
| ③ | 149f，无条件NEXT | 原有自然上限，完整保留 |

`BheInfoCollectionTermConverter`把`ACTION_SDASH_END/BDASH_END`降级为`ACTION_SDASH/BDASH`，导致①在不冲刺时退出、②在冲刺时也退出。原生`66B6E0`的OBJECT3 token4／5分别检查所选机体native state3／4；root选择器`[3,0]`经`668A30`读取self `+0x14C4`。`6685D0`处理每条collection的NOT；`66A730`按CHANGE尾`int1=0/1`执行AND／OR。root NULL返回不可判定3，NOT不将它变成真，因此①不承诺NULL立即退场；保留的149f上限之后仍进入原P2，再由原40f END收尾。

P0原有四项OR守卫仍只属于P0：`INDEX_OVER`检查记忆链序号上限；`TAMAENDCHECK`检查耐久或攻击槽剩余值；`MAPSTOP/MAPSTOP2`使用参数10作为碰撞mask `0b01010`进行接触／重叠检查。10不是超时帧数或HP阈值。本轮未把这些条件搬到P1，也未增加计时器或修改公共转换器。

[SouBoostProcterDashContinuationFixer.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouBoostProcterDashContinuationFixer.java)分别深复制各等级公共本体，追加`DX_SOU_BOOST_PROCTER_LV1/2/3`。只改私有双名并删除P1第②条，保留各等级本来不同的发射尺寸／偏移／寿命等内层payload、P0全部守卫、P1正确NOT-AND与149f上限、P2完整40f收尾，以及原公共子体引用。三个caller各指向对应私有本体，其24／18／12f与其余字段不变。只增加三个内部Skill，不增加MEK玩家武装。

宿主名、组号和追加区间全部通过实际common append plan解析。纯DX测试基线没有继承Tama03／Tama04宿主，合法fallback分别是Tama05／group5和Laser／group6；JINKI继承基线可使用同名Tama03／group3和Tama04／group4。源slice长度必须等于真实源技能表长度，目标必须位于`base+sourceIndex`且在对应源slice内双名唯一；不能在合并宿主里只凭`0119`等英文名选取。八条完整源记录hash、三个公共本体与两个公共子体完整重建payload均受守卫。私有三体必须同时存在、顺序正确且位于该宿主全部公共源slice之后；候选深复制及校验完成后才发布main、host与GRP计数，重复调用严格验证并保留已安装对象身份。

[SouBoostProcterDashContinuationTest.java](../../../src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouBoostProcterDashContinuationTest.java)包含8项：各级caller／payload保全、native3／4下保持P1并能到10f、停冲刺／NULL／149f／40f边界、公共前缀与子体保全及可变节点隔离、动态尾部／幂等、漂移零发布、半安装拒绝、两次真实main／host／WazaGroup二进制回环。原资源只读，输出仅用仓库内受控`@TempDir`；局部条件模型不是游戏模拟器。Counter、Blade、Snow、Flare、Cool Gain五个既有fixture各只新增一次预装调用，保持原断言。

所有本轮记录位于`target/bhe-combined-fixes-20261105`。**前两轮均是夹具失败，不算有效RED。**

| 轮次 | Tests／Failures／Errors／Skipped | 实际停点与归档 |
| --- | --- | --- |
| 第一次夹具失败 | 8／8／0／0 | 错把实机group3写成纯DX宿主预期，actual5；`boost-procter-fixture-failure.xml`、`boost-procter-red.log` |
| 第二次夹具失败 | 8／8／0／0 | 在完整合并宿主按英文名查0119，合法匹配3项；`boost-procter-slice-fixture-failure.xml`、`boost-procter-valid-red.log`（文件名不代表有效RED） |
| 第三次有效RED | 8／8／0／0 | 已越过fixture；缺私有三图、宿主634未增至637、caller未换及漂移未拒绝；`boost-procter-source-slice-red.xml/.log` |
| 实现后GREEN | 8／0／0／0 | 88.127秒，exit0；`boost-procter-green.xml/.log` |

两次夹具修正分别增加实际fallback／源前缀边界验证，以及源slice内双名与源序号的精确定位；公共前缀整图／二进制、完整子体宿主与既有Cool Gain保全断言没有放宽。有效RED后才获准实现生产。独立review未发现功能阻断，指出MEK诊断文字误写クールゲイン；现只将文字改为ブースト・プロクター，随后52项回归重新编译并覆盖该文本修正，全部通过。

| XML | SHA256 |
| --- | --- |
| boost-procter-fixture-failure.xml | `627528aad849e5e178d8c20d662ba362916d78292f81041c542a7c18bfcab088` |
| boost-procter-slice-fixture-failure.xml | `48dcedbac25fa293e6f0f0742eec5516e01b3da5cfdb0df2d2de5eb29e5a40b5` |
| boost-procter-source-slice-red.xml | `16d2e64353bd7adc0a7e5d47263e8a91c4633618c96d010ad8de34eba577dfbe` |
| boost-procter-green.xml | `0b760ab0d96b18572f8b879dfaafe767ea2649623dcd703e7582e8ae8ccdc1af` |

实际相关回归为6类52项：HumanCannonCounter 9、CursedBlade 7、SnowMoon 9、FlareSeeker 8、CoolGain 8、SouCustomizer 11；失败0、错误0、跳过0，进程退出0。XML归档在`boost-procter-related-52`，汇总为`boost-procter-related-result.json`，日志为`boost-procter-related.log`。旧case09已由主代理确认正常exit0、guard clean并自动清理28个副本；该进程结束与副本清理不构成本修复实机通过。新包身份、实际横向子体产生、冲刺保持／停止／中断及再次使用仍需后续独立实机证据。
