# 2026-11-03恢复验证记录

用户已明确允许仓库内构建、测试和实机验收，解除此前“暂不构建”限制。人工文件写入和删除仍仅限`D:\Code\NeXAS_DX`；不push、不建立分支或worktree、不上传二进制。源码放在src或tools，target仅放可再生成产物。

## 恢复基线

- 分支`develop`，恢复时HEAD为`ffaf1ed17`；原Clarias六文件及旧Sou菜单说明WIP保持原状。
- C盘旧`C:\NeXAS_DX_runs`已不存在，不作为构建、运行或证据输入；D盘恢复前约有338GiB可用空间。
- 原EXE`src/main/resources/ida-reverse/BaldrSky.exe`实测SHA256为`713cf605360a55a863f87bfbdea5453aaeeb2a33983ad4a005c9297e3a86e757`。不使用bsdx_game中的旧宽屏EXE。
- 先验证存储和进程策略，再验证Sou四协议及角色回归，之后才执行整链构建与新包实机验收。旧222项通过、85项历史预览不转记为新包通过。

## 已执行

1. `C:\Python313\python.exe -B -m unittest discover -s tools -p test_bhe_runtime_session.py -v`：54项，失败0、错误0、跳过0，退出码0，20.236秒。TEMP/TMP/TMPDIR及所有fixture限定在仓库target；没有启动游戏或Frida。
2. Java存储策略首轮：5类42项，失败0、错误1、跳过8。Windows强制锁使旧产物不可读；测试在释放旧文件锁前关闭构建对象，导致预期拒绝后，自动清理再次读取同一受锁旧文件并抛错。仅调整该测试资源生命周期，保留产品锁及哈希检查，并补充解锁后可恢复构建、退役旧产物的断言。8项地图测试因未提供静态资源根而跳过；重跑显式提供只读BHE资源根，不将首轮跳过算通过。
3. Astra6 Ultra独立只读审查134/135修复及原始资源：未发现确定阻断项；14个完整Skill原始签名匹配源码常量，私有SE非identity重绑及危险发射边均有对应实现。该结论不替代测试或实机；134公共虫体的条件回收仍需覆盖落空、中断。
4. 修正测试夹具后，经受控入口重跑存储策略5类42项：失败0、错误0、跳过0，退出码0。其中受管构建22项、V2中间封包开关3项、原生子进程模拟8项、地图输出冲突8项、地图事务冲突1项。地图测试显式读取`D:/BDY/NeXAS_Resources/bhe_resources`，仅在仓库target写入测试产物；未执行游戏构建。首轮锁冲突RED报告与本轮摘要保存在可清除的`target/bhe-checks-20261103`，不提交生成二进制。
5. 当前角色相关回归首轮实际执行29类230项：失败0、错误1、跳过0；唯一错误是134回环夹具把GRP命名为`flare-WazaGroup1.grp`，按真实basename分派的解析器不接受该名。仅将两次回环放到独立目录，各使用协议文件名`WazaGroup.grp`，未放宽解析器或原强断言。随后完整重跑该类8项，失败0、错误0、跳过0、退出码0。因此本轮相关范围的229个首轮通过项与该类修后8项均有实际运行证据；不把这写成一次230项全绿运行。
6. 恢复的被动观察器新20项与原运行工具54项联合执行：74项，失败0、错误0、跳过0，退出码0，11.309秒。新观察器先取得19项真实RED，另取得读错后旧请求不得误闭合的1项RED，再修正关联失效处理。仅Node内存模型，不启动Frida或游戏；真实挂点仍待本轮新包确认。

## 当前边界

本轮新整链包已构建并完成静态身份核验，尚无武装实机通过结果。Sou153项、九角色54追加武装的效果、跨武装连段、取消、中断、再次起手、重载和关卡推进必须继续逐项验收。

旧target观察器源码已丢，但保全的case09文本日志回显了四个原观察断点及请求关联条件。恢复观察工具时保留这些条件，安装前核对EXE身份和指令字节；仅观察，不强制出招、不修改动作或异常行为。正式工具存入tools，原始二进制与私人存档不提交。

## 本轮整链与成品核验

受控入口`TestBheByJinkiRunner#testRunNineCharacterRework`真实完成，退出码0；JUnit记录1项、失败0、错误0、跳过0，耗时242.399秒。

- 唯一构建目录：`target/bhe-playable-builds/run_20261103_154924_549_070a8f21`，受管状态COMPLETE。
- EXE：`sou/BaldrSky_20261103_155143_736.exe`，4793856字节，SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`。
- PAC：`sou/Update3.pac`，1460490768字节，SHA256 `10a6d5f50cd528e9206744c53a0dfaac5836f204d384ab3219d07085f5ab1996`。最终封包步骤在发布前调用`PacDeliveryVerifier`真实解包并核路径/内容，成功后清除它创建的验证副本；没有另做第二套解包副本。
- 独立读取最终3884个资产，与保全的180550解包审计比对；先核保全容器和重建原文本的长度/SHA。无新增、无删除，仅`WazaGroup.grp`、`sou.waz`、`tama05.waz`变化；其余3881个长度及SHA相同。审计文本为`target/bhe-checks-20261103/built-assets-vs-180550.json`，不复制旧二进制。
- receipt仍为构建时的`runtimeValidated=false`；上述核验不替代实机。

## 首次隔离运行

case `sou-finite-20261103-01`使用本轮EXE/PAC及空私有Save。4个观察挂点的运行时签名核对和安装实际成功，随后出现正常自动更新询问框；选择不启用更新。

后续出现`NtCreateFile`写入`C:/ProgramData/NVIDIA Corporation/ShadowPlay/CaptureCore.log`请求，守卫拒绝外部写入并主动终止自有进程，终止码`E0000001`。此轮状态`failed/guard_violation`，不能记作游戏自然崩溃，也不算任何武装通过。随后仅对该完整日志路径实现case内私有重定向，不放宽ProgramData目录写权限。

退出已确认、guard释放完成、`cleanup_pending=false`；程序实际清除28份仍与输入一致的game副本，共5805648253字节，清理错误0、保留改变/唯一文件0。私有Config、运行文本与所需临时资料保留；未删除源游戏或构建包，未写删C盘原件。

## 观察工具与精确日志重定向

正式工具`tools/bhe_runtime_observer.py`由保全的四挂点恢复，默认不启用；Astra6 Ultra与root分别核对了被动性、固定EXE/指令签名、请求关联及安装时序。root另补越界请求不能保留旧accepted关联的真实RED，修后20项全通过。

首次守卫停止后，为准确CaptureCore.log路径补4个测试方法，先真实取得6个断言失败、错误0的RED，再实现私有空日志及精确别名；未读取或复制原日志。当前58项harness与20项observer联合回归共78项，失败0、错误0、跳过0，退出码0，11.241秒。`capture-log-all-green.log`及小体积RED日志位于`target/bhe-observer-checks`。

该补丁只影响试跑隔离工具，游戏成品EXE/PAC没有再次构建或修改。新case `sou-finite-20261103-02`使用上一轮已退出case的私有Config作为独立种子，不覆盖真实Save；后续结果另记，不能由78项离线通过推导实机已通过。

## 第二次启动与兼容种子恢复

case02实际显示正常主菜单、进入模式选择及竞技场菜单，退出后进程码0、failure=null、guard违规0、cleanup_pending=false；28份game输入副本再次由工具核验后清理，共5805648253字节。观测到32次原生异常通知，不将“正常退出”写成“无异常”；这些通知没有被工具吞掉或作为武装通过证据。CaptureCore.log别名确实命中私有路径，但该次底层打开返回C0000022；主菜单及竞技场仍正常工作，不能把该事件写成驱动日志成功写入。

空Save中的地狱模式未解锁，不能用改actor或强制调用入口绕过。原target兼容种子生成器已丢，按`vanilla-encoding-forensics.md`恢复为正式`tools/bhe_private_save_seed.py`，只接受此前批准的六种中文标题与相应日文标签，未知拒绝；首块后2006块、原始group-count及每组1003块均按原生结构校验至EOF。

12项合成测试真实RED→GREEN，失败0、错误0、跳过0。实际单次生成`target/bhe-private-save-seeds/cp932-20261103-01`：78文件共4226277字节，26个标题映射；其余52文件和所有游戏进度尾部字节保全，原Save前后完整SHA清单相同。Save002生成哈希`7dfd7b4088c0bbbea40ebee6c4823dd85df1472ac52e4b815ce717026c173d3e`与历史批准种子一致。审计44687字节，不含存档正文；该私人种子不提交、不随mod分享。

2026-11-04恢复时case03仍为prepared、无进程/launch.lock；其原驱动状态已由系统更新，preflight在创建进程前拒绝。一次原位快照更新仅处理6份Drs副本，没有重拷game；但随后nvAppTimestamps再次由外部变化，证明继续快照更新不能解决可变系统状态约束。正在收紧此处审计语义：私有Drs副本仍必须符合准备清单，外部原Drs的变化保留前/后差异，不修改原件或放开外部写权限。游戏/源EXE/PAC/真实Save/独立种子的哈希保护继续硬失败。

## 2026-11-04恢复后的补充核验

可选combat观测恢复了七个已保全的原生挂点。Astra6 Ultra指出录像缓存可能成为陈旧当前帧，root复核原生`4C2600`字节后收紧：普通事件仅保留lastReplay字段，active字节`+159D`不成立时静默清缓存，输入事件才记录frame。新反例先取得真实RED；34项observer与58项当时harness联合92项全通过。

随后修正系统原Drs漂移审计语义，私有副本仍严格匹配；原树启动前/后分段记录、不归因游戏、不改manifest。全部`test_bhe_*.py`实际联合108项，失败0、错误0、跳过0，退出码0，14.649秒。

case03其后实际安装11处签名匹配的观察挂点，但系统组件尝试写入仓库外`CryptnetUrlCache/MetaData/329B6147266C1E26CD774EA22B79EC2E`，守卫主动停止。failure=guard_violation、终止码E0000001，不当作游戏自然崩溃或武装通过。进程退出/guard释放已确认，原Save不变、cleanup_pending=false，28份5805648253字节game副本自动清除。此轮准备→启动前原Drs有nvAppTimestamps漂移，启动前→退出未变，两段均保留。该证书缓存仅对实际观察到的完整文件准备私有别名，其它文件仍拒绝。

Astra6 Ultra另只读补齐静态继承链：130704-v2报告的9角色54个唯一DX身份、126条默认槽记录，其九角色18份MEK/WAZ及MekaGroup.grp、WeaponEquip.dat、Sou.mek共21个输入哈希均与180550相同；再由本轮3881未变文件将身份、配置热耗、直接索引及默认14槽保全延续至新包。Sou.mek相同结合当前回归合同证明菜单0..152、保留211武装/Material与隐藏高编号的静态内容。新Sou WAZ/宿主及GRP不继承旧图通过，仍以本轮专项回归和后续实机为准。

证书缓存最小补丁只对上述完整MetaData文件别名，不开放其它32位名字或Content文件；case仅新建两空目录，原缓存没有读取或复制。新增4方法先真实7个断言失败、错误0，再最小实现；全部`test_bhe_*.py`联合112项通过，失败0、错误0、跳过0，退出码0，14.799秒。新case `sou-finite-20261104-04`继续用同一已封存EXE/PAC及同一独立兼容种子；没有再次构建。

## 中间指令观测故障与修正

case04实际进入地狱、Sou装备页并预览30；REQ1/START1于tick15051，READY15149，随后EXE78A869访问违例，进程C0000005。无guard违规，不能记为人工停止或验收通过。

case05同EXE/PAC、同独立种子、仅隔离保护，重复人間砲弾数分钟仍运行；再仅安装一个READY中间挂点便复现78A869访问违例。CDB捕获指令`mov eax,[edi+6C]`，EAX/EDI=-5读67，栈中有Frida leave trampoline；case04为-7读65。没有跳过异常、修改寄存器或补游戏EXE；两次失败与case05文本调试日志保留。

根因是恢复的观察器把中间指令也使用函数调用回调对象`{onEnter}`。Frida官方API明确任意指令应直接传入函数；官方实现的CallListener即使没有JS onLeave仍有原生leave处理，会改函数返回槽，ProbeListener则无此处理。只将START/READY/DAMAGE/REACTION四处中间挂点改为直接函数探针，其余七个真实函数入口保留原形式，地址、签名、this.context读取及严格请求关联不变。API契约先真实RED，修后全部`test_bhe_*.py`113项通过，失败0、错误0、跳过0，16.574秒。游戏成品没有改变或再次构建。

官方依据：`https://frida.re/docs/javascript-api/#interceptor`，以及frida-gum的gumquickinterceptor.c中ProbeListener/CallListener与guminterceptor.c的will_trap_on_leave实现。不能把“JS没有显式Memory.write”当成函数式inline hook无行为影响。

## 历史暂停点（2026-11-04，已于11-05恢复）

修后case `sou-probe-fixed-20261104-06`安装11处签名匹配的观测点，已实际连续预览并独立核对12个不同武装：30、37、63、134～141、152，均为variant0。每项都有同请求START/MATCH/CANDIDATE/READY、同身份、双方active=-1和TAMA/SCRIPT=0；自动重复不增加不同武装覆盖数。63首轮伤害45、134为75、135为165；152为205条伤害合计1229，最终敌HP4771。30/37没有伤害事件，不能据闭环宣称有命中。

首六项的257个完整原事件（含152全部205条伤害）以166950字节纯JSON固定在`src/test/resources/bhe-runtime-evidence-20261104/sou-finite-first-preview.json`；另外六项核心链、完整前缀长度/SHA和暂停收尾在同目录`pause-preview-coverage.json`，24513字节。没有复制EXE/PAC、存档、dump、截图或录像。

尝试142前Computer Use检测到用户输入而拒绝该动作，142不计覆盖。随后用户明确要求暂停并关闭游戏。root通过当前case的STOP机制停止精确自有进程715424；最终status=stopped、failure=null、stop_reason=STOP_file、终止码E0000001，进程退出与guard释放均已确认、cleanup_pending=false、原Save未变。28份5805648253字节game输入副本经程序核验后自动清理，错误0。没有仍在运行的BALDR SKY或CDB；这是按用户要求停止，不能写成游戏自然退出。

该暂停点原定下次恢复使用同一封存构建、新准备隔离case，不复用旧launch.lock；当时剩余141个菜单武装及其它玩法范围未验。这些数字和暂停状态仅描述2026-11-04现场，不代表当前状态。

## 当前状态（2026-11-05恢复）

用户已恢复目标，当前继续执行。仓库内构建、测试、实机验收已获许可；构建、运行及临时产物仅限仓库`target`，仓库外不写删。直接在`develop`工作，不新建分支或worktree，不push，不上传或重复保留二进制。

2026-11-03封存的`run_20261103_154924_549_070a8f21`已构建；其后case07/08仍使用这个旧EXE/PAC，现均已终止并完成清理。旧包累计130个不同武装取得variant0菜单自然闭环，带有明确no-hit及99系统异常边界；121/122驻留未READY，123在不安装gameplay/combat探针的case08再次读空surface并以`C0000005`退出。详细请求、证据和剩余范围以[2026-11-05 Sou菜单验收记录](sou-menu-validation-20261105.md)为准，不把本记录早期“尚无实机结果”或历史暂停点当作当前结论。

两项修复现已封入下述11/05新包；123已有本包菜单闭环，133仍未通过。旧包130项不跨包继承为修复通过。全部Sou153项安全、九角色54新增武装的实际效果与跨武装连段，以及取消、中断、再次起手、重载、关卡推进等目标仍未完成。构建receipt保持`runtimeValidated=false`。

## 11/05新封包与退役恢复：保留原构建错误

`run_20261105_123342_656_c9e63039`已生成并发布COMPLETE receipt，EXE为`BaldrSky_20261105_123604_544.exe`，4793856字节，SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`；PAC为1460557257字节，SHA256 `959f3627189039b23686e2306f2b38b50c6c87fb9599b0d5a1548100e2ae42bf`。原full-build Maven进程最终退出1，JUnit为1项、失败0、错误1、跳过0：封包和receipt完成后，旧run的11个目录mtime与封存行不同，`ManagedPlayableBuild.retire`拒绝退役。这个错误仍保留，不能因为产物完整就写成原full runner通过。

只读核旧清单的25169个文件：大小、mtime、SHA均相同，无新增/缺失；仅目录mtime不同。先以目录mtime单独漂移取得1项1失败的RED，再仅在比较副本中忽略目录`modified`，文件及其余路径/类型/身份/锁/消费者保护保留；全Managed回归28项零失败/错误/跳过。随后持项目排他锁仅调用现有pending退役路径，未重新封包、未新建run、未重跑full runner。旧run及pending已移除，删除的归属文件逻辑字节为4653803188；新包、case09清单身份未变，不能把逻辑字节当作精确物理释放量。

新包共3889资产，新增5张真实供体PNG合计66315字节，无删除；4个公共WAZ仅10处ACTION_WAZA参数变化，归一这些精确参数后其它语义一致，3880文件和10份角色主WAZ保持原字节。精简静态/退役证据固定为`src/test/resources/bhe-runtime-evidence-20261105/build-and-retirement-20261105.json`，25636字节，SHA256 `900322e90f90b4bbbba9bca3aac40ec0205c463807cd293226a64427cb35ddc7`；不含二进制、大inventory或完整运行日志。

## 新包case09当前验收边界

`sou-direction-fix-20261105-09`仍运行，PID811768，目标保持active，继续训练验收。已独立固定123～132十条与142～151十条variant0菜单自然闭环，分别有237条/80条敌机伤害事件，玩家伤害均0；150/151为no-hit闭环。两份证据为`sou-direction-fix-123-133.json`（845980字节，归因修正版SHA256 `b4c4008634fb733c030c45daa6a3975097d6eaa5f3ae2ec725f9ee5bb1146d91`）与`sou-direction-fix-142-151.json`（188954字节，SHA256 `362de9a14b3b1f931402f6d55329ad9376371d90997af4279a38cdf2565ed4af`），均位于上述测试资源目录。只纳明确指定的请求，不计自动重复、未知输入区间或旧PAC结果。

133的REQ87首个30秒helper失败保持`closed=false`，原请求随后在tick58649被superseded，触发者/原因未知；不能归因为root手动UI，也不能由之后REQ88/89补成功。REQ87的独立RPM快照只有当时TAMA14/双方空闲等事实。另一个REQ89的对象快照在tick68688看到14个Tama05/528、block5、scalar0、parent=NULL碎片，当前正修其父关系和等待退出；这份快照不跨请求归给87，更不等同READY。全部153项和九角色54新增武装的实际训练/连段验收仍未完成。
