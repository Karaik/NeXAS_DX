# Sou クールゲイン回收协议修复（2026-11-05）

本轮修复 Sou 菜单133／`C_GAIN03`对应的冰片回收协议。源码与真实资源回归已完成；尚未生成包含本修复的新PAC，也未进行修后实机验收。下述旧包现场只能证明原问题，Java测试与局部条件模型不能替代菜单READY、训练、取消／中断或连续使用验收。

现场使用`target/bhe-playable-runtime/sou-direction-fix-20261105-09`，进程身份为PID811768、创建FILETIME `134383236752109730`，映像为该case内`game/BaldrSky.exe`。输入来自`run_20261105_123342_656_c9e63039`；既有证据中的EXE／PAC身份如下，本记录没有改动这些封存产物。

| 文件 | SHA256 |
| --- | --- |
| BaldrSky.exe | `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae` |
| Update3.pac | `959f3627189039b23686e2306f2b38b50c6c87fb9599b0d5a1548100e2ae42bf` |

**REQ87与REQ89分开记账。** REQ87／START87在tick36229起手，START时TAMA=0；首次30秒helper失败仍为`closed=false`。tick36308的release记录main449、block1、clock37、TAMA14，tick36338发生reset，均不是READY。tick53381的独立RPM快照只证明当时TAMA14及双方空闲等状态。REQ87最终于tick58649被superseded，未观察到native READY，不用后轮结果补成成功。

原日志4363—4371行记录：REQ88／132在tick58649开始，tick58650即被替换；REQ89／133于tick58650开始，REQ与START均为TAMA=0。主代理没有发出这段间隔内的UI操作，替换的触发者／原因未确定。REQ89的对象不能倒归REQ87，也不能当作132遗留物或已完成的预览。

REQ89的`sou133-tama-snapshot.json`为只读RPM快照，`tickBefore=tickAfter=68688`。计数14，循环链表闭合于sentinel `0x48e8f18`；14个对象全部为group5／skill528、entry=block5、scalar0、parent=NULL、root=`0x35233cf8`，clock范围2—26。该快照不是native hook事件或READY。快照文件3887字节，SHA256 `289aeedeefd20f8e33fd1de4f12f017e9f0030029bc79d67a6978c34474959a0`。

REQ87的长期文本证据为`src/test/resources/bhe-runtime-evidence-20261105/sou-direction-fix-123-133.json`，SHA256 `b4c4008634fb733c030c45daa6a3975097d6eaa5f3ae2ec725f9ee5bb1146d91`。它收录的原日志前缀止于REQ87被替换，2375198字节／4363行，前缀SHA256 `25bf91e93ff849268dbed4f06d79fd03fd582db7bedb2f374c1a8d8dd44233b9`；不包含后续REQ89窗口。REQ89对象快照另存于上述case目录。

资源链由真实BHE记录与最终公共图核对：

| 角色 | BHE源序号／英文名 | 当前公共目标／原行为 |
| --- | --- | --- |
| Lv1 caller | sou447／C_GAIN01 | 27f发射group5／527 |
| Lv2 caller | sou448／C_GAIN02 | 21f发射group5／527 |
| Lv3 caller | sou449／C_GAIN03 | 15f发射group5／527 |
| 本体 | tama05源375／0344 | 目标527，3个phase |
| 冰片 | tama05源376／0345 | 目标528，8个phase |
| 热冷却 | tama05源377／0346 | 目标529，1个phase |

**失配根因是两个BHE计数用途挤入BSDX的单一scalar。** 本体P0用channel0持续`+=7`积累充能，四条原发射边都只在P0产生冰片；P1在1f起按原无限FF执行`-1`。BHE另用channel3（第四通道）在P1的0f写1、P2的0f写2，通知冰片；冰片P5／P6分别等`parent.PARAMCOUNT4==1/2`。

`BheInfoCollectionTermConverter`将`PARAMCOUNT1..4`都映射为单一`PARAMCOUNT`；common与followup两份`BheToBsdxWazObjectConverter.convertFreeParam`只承载active channel0，没有active0的第四通道写入降级为scalar0。因此P1错误清空已有充能，1／2状态通知也不再存在。冰片P5与P6分别保留0..37和0..169的无限循环；原NULL后备条件选的是self `[0,0]`，不能捕捉direct parent消失。这与REQ89中parent=NULL却停留P5的14个对象相符。

批准方案由[SouCoolGainReturnProtocolFixer.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouCoolGainReturnProtocolFixer.java)实现，并接入`SouCustomizer`末尾。只在既有Tama05的动态尾部追加两个深复制私有技能：

1. `DX_SOU_COOL_GAIN_BODY`／`★蒼：クールゲイン：本体・回収`：完整复制公共527，四条P0发冰引用切到私有冰。仅删除P1的0f scalar0降级事件，保留P0的+7、P1的-1及原HP／计数／MEMORYOBJ出口。P1热冷却仍引用公共529；P2的scalar0及20f原出口保留。
2. `DX_SOU_COOL_GAIN_ICE`／`★蒼：クールゲイン：氷・回収`：完整复制公共528。P5先判direct parent NULL进入原P7，随后以self的`CHANGE/PARENT_TURN`（types `[2,8,7]`、aux `[0,0]`）进入P6。P6同样先判parent NULL进入P7，随后以parent scalar<=0（types `[2,0,0,2]`、param `[0]`、aux `[2,0]`）进入P7。保留P7原15f END及其余payload、FF和suffix。
3. 三个main caller只改本体sequence引用，保留27／21／15f和其余字段。公共527／528／529、原DX前缀及其它技能不变；没有修改全局转换器，没有增加固定超时或强制END。

**PARENT_TURN使用保存的父entry，不能简化为“父entry不等于0”。** 原生`60E130.c`在对象每次换块时，从self `+5292`取得direct parent指针，将parent `+5568`的当前entry保存到self `+5600`；`66B230.c`的case7比较当前parent entry与这个保存值。在本条件下parent NULL也满足PARENT_TURN，因此NULL分支必须放在前面，直接进入原P7淡出。`66B6E0.c`的NULL判断使用所选对象；aux `[2,0]`选择direct parent，`[0,0]`选择self。scalar条件由`66EB40.c`读取单计数。

| 冰片当前阶段 | parent当前／保存entry | 模型应有行为 |
| --- | --- | --- |
| P5 | 1／0或2／1 | 检出父阶段变化，进入P6 |
| P5，晚到父P1 | 1／1 | 保持P5；之后父到P2时，以2／1进入P6 |
| P5，晚到父P2 | 2／2 | 保持P5；父消失后经NULL优先分支进入P7 |
| P6 | scalar<=0 | 进入P7，不依赖entry差值 |
| P5或P6 | parent NULL，保存entry为0／1／2任一值 | 优先进入P7 |

进入P6也会刷新父状态快照。测试覆盖lateP1后父P2、再由scalar0进入P7，以及lateP2后parent NULL进入P7；只验证这些后继变化／NULL发生时的出口，不宣称晚到P1／P2就立即进入P6，也不据此认定实际游戏何时满足条件。

六条源记录使用完整flag／双名／83-slot／phase／suffix字节签名守卫；三个公共目标逐一重建并比较完整payload，校验源slice位置。私有图与公共图的可变DTO、集合、Map及数组节点不得共享身份；所有候选与校验完成后才一次发布main列表、host列表和GRP计数。动态尾部、后续合法追加、精确幂等均受测；半安装、双名冲突、来源漂移及私有预算被篡改必须拒绝，不能静默修补。

回归入口为[SouCoolGainReturnProtocolTest.java](../../../src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/sou/SouCoolGainReturnProtocolTest.java)，8项覆盖三caller保全、充能预算、两条回收条件、完整公共／私有图、动态尾部与幂等、漂移零发布、半安装拒绝，以及main／host／WazaGroup两次真实二进制回环。输入真实资源保持只读，输出仅为仓库内受控`@TempDir`；局部条件模型明确不是实机模拟器。

| 阶段 | Tests／Failures／Errors／Skipped | 证据位置（均在target/bhe-combined-fixes-20261105） |
| --- | --- | --- |
| 生产实现前RED | 8／8／0／0 | cool-gain-red.xml、.log |
| 实现后GREEN | 8／0／0／0 | cool-gain-green.xml、.log |
| 相关Sou回归 | 44／0／0／0 | cool-gain-related-44/*.xml、cool-gain-related-result.json、cool-gain-related.log |
| 父快照模型修正后GREEN | 8／0／0／0 | cool-gain-parent-snapshot-green.xml、.log |

RED失败为缺少私有双图／caller未切换及漂移未拒绝，没有fixture执行错误。相关44项为HumanCannonCounter 9、CursedBlade 7、SnowMoon 9、FlareSeeker 8、SouCustomizer 11；四个专用fixture仅预装Cool Gain以隔离整图断言，未放宽原断言。独立审查指出父快照模型过强后，只修测试模型与晚到场景；生产代码不变，同一审查者复核该小diff，主代理再次串行跑8项通过，未因纯模型修正重复44项。

核对归档XML及汇总JSON的SHA256：

| 资料 | SHA256 |
| --- | --- |
| cool-gain-red.xml | `9dedbd03fe5aeda4c0da1cab7b41ef7837c155f8ba35470d02df4da6daf6c16e` |
| cool-gain-green.xml | `4f91a040b202c8b3d614e978bc520b81a343f3f6b560f10550ed122ad1206bfe` |
| cool-gain-related-result.json | `0ed63c7e92bcaae910921f0173608883b0f09e0d903fb38fba84e2eb08a8b72d` |
| cool-gain-parent-snapshot-green.xml | `6e2aaedd9b7eb39624e0b7f697ee068b30acc7745225f6d88072835d1a246d64` |

本记录只登记源码与测试状态。包含该修复的新构建身份、Sou133修后native READY、实际冰片归零及取消／中断／再起表现仍待新封包和独立实机证据。
