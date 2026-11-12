# BHE九角色重做实施账本

Plan: docs/superpowers/plans/2026-10-26-bhe-playable-rework.md

目标尚未完成；最终必须满足游戏不崩、实际效果、说明书、乱码恢复、保留架构、中文提交/no push和谨慎删除全部要求。当前恢复点以文末最新日期及关联证据为准；历史段落中的“当前”只指当时状态。

## 初始状态

- develop/c27bc14fb8a26dff36b87cd6cf399dc818686b95，现有六项Clarias WIP共53行新增，保持不动。
- 九角色为Tsukuyomi、Yuri、Freja、Nagi、Misaki、Naoto、Katou、Wilhelm、Motoki；Sou不参与角色重做。
- 历史审计已完成，不将其误记为实施完成；本目标刚开始，没有新功能已验收。
- 默认Maven资源测试禁止；测试由主代理串行调度。

## 分工

- 主代理：共通正确性、集成、构建、封包、审查及提交。
- rework_runtime_scout（astra/ultra）：只读确认实际游戏和调试入口，不修改注册表/旧资源，不运行旧补丁脚本。
- audit_conversion_20261026：只读核对Freja/Yuri具体实现方案，等待编辑权。
- research_completeness：只读定位真实代码乱码，等待编辑权。

## 进度

- 已读取当前用户要求、仓库规则、Git状态与历史测试边界；开始Task1失败回归。
- 运行时旧脚本含WER注册表写入和语音gate bypass，不能直接作为新交付验收。
- 本轮没有删除文件、改存档、创建分支/工作树、push或改PR。

### Task1：公共Change跳转

- 新增CommonChangeRedirectTest，全部在内存执行，写读真实CEventChange字节，不写共享资源目录。
- RED：mvn -q "-Dtest=CommonChangeRedirectTest" test，4tests/3failures/0errors；真实失败为[0,246]仍[0,100]、动态base801仍100，以及缺映射未拒绝。phase/算法/负哨兵测试通过。
- 正在实现公共helper的BHE JUMP/WAZA门控与实际append映射，不将非资源参数统一加偏移。

- GREEN：CommonChangeRedirectTest4/4通过；真实39条生成/重解析不一致0；独立规格与质量审查通过。
- 已提交6ef115da1“修复公共弹幕的技能跳转重绑”，未push。
- Task2源隔离两项、旧重绑两项、公共四项通过；待独立审查。
- Task3第一批Freja4项/Yuri5项通过，组合共17项通过，游戏与完整热量循环尚未验收。
- Task4已恢复34个非WIP文件及三个WIP文件的四行文字；反向还原文字后原功能WIP hash不变，准备仅暂存文字。
- 新增NativeErrorHandlerPreservationTest真实链式写出先RED（原8B被改C3），撤掉三个现行profile的全局CRT早退后GREEN；原始EXE只读，未动语音兼容和容量逻辑。

## 第一轮集成与运行

- 最新已提交：6ef115da1公共跳转、fc4c5f32b乱码恢复、fbb6f5d81源对象隔离，均中文提交/no push。原六项功能WIP仍53行新增。
- 新入口TestBheByJinkiRunner#testRunNineCharacterRework，保留架构，九角色在Motoki封包，Sou未运行；每轮独立target/bhe-playable-builds/run_*目录。
- 第一轮构建Exit0（原session83606已终态），路径target/bhe-playable-builds/run_20261026_222722_761/build.json。PAC222687635字节，SHA54f7c7d2d6f9b38f7be26893f75a5e7894ceec4c3c0f5bb3cbe60d821432566b；EXE SHA09ff572b7d42c2aedbe092e5ad40583377d1cd258d65065bfc7b77d92df31f68。
- runtime rework-01准备成功（原session35023已终态）。launch94100已Exit1：挂起attach/load、hook_ready、ResumeThread均成功，但严格文件守卫拦截Shell32的Windows Caches访问及随后目录创建，工具主动结束自有游戏。无游戏窗口、redirect_count=0，不能算游戏崩溃或通过；原Save前后hash不变。
- runtime工具独立审查后的生命周期修复16项离线通过；首轮现场暴露缓存与PIDL关联问题，已由astra进行最小修正，禁止关闭原Save保护或复用旧case锁。
- 当前并无活游戏进程。还未完成任一角色的正式游戏效果验收；九角色玩法仍仅首批Freja/Yuri参数与窗口已实现，其他机制必须继续。

## 当前恢复点

- HEAD=3d36ed573；另三个本轮提交为fbb6f5d81、fc4c5f32b、6ef115da1；不push。
- 原有六项Clarias/AGENT功能WIP仍53行新增；作者/注释四行已单独提交，功能部分未暂存。
- 所有已运行Maven/prepare/launch会话均已终态；无须轮询或重启旧handle。第一轮构建83606成功；runtime prepare35023成功；runtime launch94100因守卫失败退出且原Save不变。
- 角色第一批源代码/helper/测试及Runner仍未提交；角色root保护复审待收束。20项最新GREEN包含Freja6/Yuri5/Native1/Isolation2/旧Rebind2/CommonChange4。
- runtime astra正在按首轮日志修正限定Caches目录、已有目录metadata访问以及PIDL只在成功重定向后消费；不应扩大允许根至整个AppData，不允许放开原Save写入。
- conversion agent接手task3-counter-brief.md：仅恢复真实active channel0，九角色主WAZ均仅0；两objectConverter+Effect内层保持一致，先测试RED。其他通道不能宣称等价，暂不动Sou目录。
- 下一个真实运行使用新case（rework-02），不可删除rework-01/launch.lock或把它当游戏已通过。

## 2026-10-26 第二恢复点

- HEAD=a312d6655。新增已提交3d36ed573保留原生异常处理、90faf5903芙蕾雅/尤里第一批、a312d6655恢复active channel0计数；最新定向Java回归29项通过，尚无角色实机验收完成。
- rework-04已终态failed：首次自动更新提示可见，点击“いいえ”后开始图形初始化；唯一守卫拒绝为NVIDIA的nvdrssel.bin OPEN_IF。工具终止码E0000001，不能宣称mod崩溃或通过。28条system异常缺调用栈，归因未定。原Save新增/删除/修改均0。
- runtime代理正在将唯一已观察的NVIDIA selector文件私有化到case；原文件与Save继续保护。没有活游戏需要接管，不重用rework-04。
- conversion代理实施Tsukuyomi/Wilhelm/Motoki复制变体及有限共享append工具；research_completeness代理实施Nagi/Katou，编辑权互不重叠。主代理持有pipeline/Importer/Runner/所有Maven及commit。
- 主代理正在测试最终MEK驱动的14槽初始装备，替代当前[0,-1,...]默认预设。12个普通槽、FC槽12、initializer槽13由原版Kou真实表/分类核对；具体距离/按键映射待UI确认，不凭名称猜标签。
- Ruling: 继续使用既有两个BHE主线和Customizer，追加有限变体与装备工具；未启用的旧入口不扩大重构。用户存档与全部旧case保留，不push。

## 2026-10-27 当前进度

- 第一批五变体、装备预设、两pipeline数量同步与两Importer已实际合跑48项全过：CharacterWeaponVariants8/Nagi4/Katou4/PlayableLoadouts5/LayeredDelivery26/Importer1。源码尚未提交，等待独立审查闭环。
- 修正Tsukuyomi主线复用素材补齐器的import；使用现有Followup helper的同等baseline行为及MEK重载，不新增另一份补齐实现。
- Misaki/Naoto先RED8项/8fail/0errors；初次GREEN组合24项/3errors为Misaki局部Jackson副本未忽略computed empty属性。已按同一DTO副本策略修正，正在复测；Naoto4及旧Misaki16当次通过。
- 独立审查定位三角色退出donor的正常结束载荷未严核：原版当前输入正确，但漂移为回飞行时仍追加。交原实现者先补RED，再用有限helper修复；尚未完成此审查门禁。
- rework-05用户Escape后由STOP结束，原Save/driver seed均未变。rework-06被新增符号诊断引发的Frida raw.pdb访问误拦，非已证实mod崩溃；工具终止码E0000001，原Save/driver均未变。
- runtime observer现删除DebugSymbol/ACCURATE回溯，保留纯模块/偏移/寄存器信息，异常链仍false交还原生；离线27项通过。不为PDB路径放开守卫。新case rework-07准备中；没有角色实机验收已完成。
- 目标仍active：九角色游戏验收、说明书、最终包和中文提交均未全部完成；不push。

- normal-end唯一Important已修复并获同一审查者复审ADDRESSED，无新critical/important；Misaki/Naoto独立规格与质量审核均通过。
- 当前完整定向套件16类104项全部通过，0失败/错误/跳过，命令与原始日志为nine-character-code-green.log及Surefire XML。
- 当前完整九角色构建run_20261027_002200_048已Exit0，含七个可装备变体及新预设；Sou未运行。PAC SHA256=2c93f7caf599fc160e41b28e4c4fa313a2a7c212dd68c669ba91adc365d349a7，EXE SHA256=09ff572b7d42c2aedbe092e5ad40583377d1cd258d65065bfc7b77d92df31f68。runtimeValidated仍false。
- rework-07私有selector成功12次，随后同Drs的nvAppTimestamps被守卫拒绝并主动终止。运行工具已改为完整Drs状态树（6文件/5476097字节）副本与目录边界alias，原Drs继续拒写；29离线测试通过。新rework-08使用本轮新PAC准备中。
- 中文说明书已创建开发稿，真实菜单路径及逐角色实机矩阵仍待补，未将开发稿当最终说明书验收。

## 2026-10-27 启动故障定位恢复点

- 静态阶段已提交c89e6c252（中文/no push），包括七变体、装备、构建及开发稿。六项原有Clarias/AGENT WIP仍精确53行新增；74个本轮改动Java/FXML文件格式违规0。
- Freja后续只补武幻剛乱舞日文说明和默认FC入口，未改其Charge/计数/Change。Freja6+Loadouts5复测Exit0。此四文件与研究报告尚未提交，当前0022包仍旧默认FC；最终须重建。
- runtime已由全盘白名单改为输入/存档保全模式：case允许、仓库其它路径和原输入拒写，已解析仓库外普通OS日志/缓存允许并审计；不吞异常。新增显式--vanilla，与--pac互斥必选。当前34个离线测试通过，工具/测试/运行文档尚未提交。
- rework-09首次无guard的实际退出：C0000417，Save9/Drs41成功。rework-10-debug复现；CDB只设sxe无法捕获，因为它是CRT显式TerminateProcess状态，不是同码异常。旧CDB因默认srv*栈查询停滞，已核PID/命令行后清理自有debugger，launcher29700已Exit1，原Save未变。
- 纯原版vanilla-01（无额外PAC，原版EXE）也在同环境出现同码终止。CDB用离线-y .，断ntdll!NtTerminateProcess，捕获handle=-1/status=C0000417；无guard。当前不能归因角色移植。
- 真实直接消费者为CRT__mbsbtype_l(67A865)/__mbsbtype(67A900)，由7041E0文本排版调用。当前string bytes=A1BABFCBC0EFCBB9A1BB20BDE1CAF800，CP936解为『克里斯』 结束；CP932为乱码。首NUL在15，逻辑length16，文本index14导致_mb sbtype查询16越界。mbcinfo=046A2C88，其+4与全局87686C均为03A4即932。应定位文本真实供体并处理编码一致性，不能恢复全局CRT RET或据码页差异直接宣布根因全部解决。
- 运行专家rework_runtime_scout只读查CRT/消费者；research_completeness只读解包/查base的Update2.pac、Config/Update及原版资源，报告vanilla-encoding-forensics.md。Maven、CDB和UI仍由root独占。
- LIVE恢复点：vanilla-01 launcher session6813、CDB session55843，目前断在NtTerminateProcess前；PID见该case/state.json（CDB十六进程707B8）。.dump /ma invalid-mbs.dmp已发。不要重启/按名字杀进程；采集完发g让原生终止，再确认launcher原Save比对。其它已知case皆终态。尚无任何角色正式游戏效果验收完成，目标active。

## 2026-10-27 运行对照与恢复点更正

- 上述vanilla-01/CDB现场已结束，不再是LIVE。原版同码退出的直接输入已定位到真实Save002.dat标题的CP936/CP932不一致；独立兼容seed仅改26个存档标题，原78文件未覆盖。完整证据见vanilla-encoding-forensics.md，私有seed不进入公共mod包。
- HEAD=6d0c7938f；Freja默认FC入口和说明已提交。最新构建run_20261027_014635_068/build.json，EXE SHA256=09ff572b7d42c2aedbe092e5ad40583377d1cd258d65065bfc7b77d92df31f68，PAC SHA256=79be51b21e5beb082a0d9a059b2f2cce4546ba18833c29142dcd341ca7adf2a6。未用此包完成实机验收，runtimeValidated仍false。
- vanilla-compat-02和rework-compat-01均已由其原始launcher通过STOP正常结束，failure=null；前者原版加兼容seed，后者使用较旧0022包，不能冒充最新包结果。原版和mod的捕获画面均黑，尚未确定真实显示黑屏还是捕获限制。
- 当前LIVE为纯原版空存档vanilla-fresh-01，launcher43143，PID477696，creation FILETIME134375099066375135，映像为其case/game/BaldrSky.exe。首次自动更新选择否，主窗口8719668；Save/Drs守卫无违规，未沿用旧Config。每次输入前仍须刷新返回的窗口对象。
- 空存档原版也捕获为黑；主菜单System.bin运行、无movie，draw/update两秒各+119。有界三次Present内记录437次DrawPrimitiveUP，菜单绘制及Present HRESULT均0。此结果排除完全只有clear，但不证明最终像素、键盘操作或角色效果正常。
- CDB draw-count采样已自动清断点并detach，exit0；日志target/bhe-playable-runtime/vanilla-fresh-01/draw-count.cdb.log。当前没有为此采样保留的debugger。
- 运行工具41项离线测试本轮重新通过；独立审查未发现Important阻塞。异常始终false交还原链；只读精确MMCSS设备例外、完整私有Drs、独立seed/fresh模式和原输入保全见runtime-harness.md。
- Photoshop仅打开了本任务创建的未保存诊断截图，未打开用户PSD。另存对话框已Escape收起，没有保存至默认clarias目录。尚无角色实机验收完成，目标继续active，不push。

## 2026-10-27 静态复审收束与native驱动对照

- 九角色源码及最新014635产物独立复审未发现Important问题，19个最终WAZ的141处技能引用无越界；装备、AI、Material、WazaGroup与Freja FC载荷核对见static-and-runtime-review-20261027.md。
- vanilla-fresh-01已通过原launcher STOP结束，failure=null、原Save清单不变。当前LIVE改为vanilla-fresh-native-01（原版、空存档、正常全局Drs访问），launcher32682，PID473332/tid502084/creation134375112019869606，Drs redirect0，无guard。移开Snipaste贴图后游戏捕获仍黑，Down/Return后菜单未留下可确认变化。
- F3与Snipaste全局贴图键发生冲突，曾使Paster遮挡游戏及对话框；按官方Shift+F3暂时隐藏全部贴图，未删除图片或修改全局快捷键。之前双窗与F3无游戏截图不能归因渲染。
- 原Save与nativeDrs退出审计拆为独立异常边界，读取失败仍判失败并继续采证；正常Save差异仍判original_save_changed。工具完整46项离线回归通过，不代表游戏验收。
- Photoshop诊断图现已保存为vanilla-compat-02/window-capture-comparison.psd并关闭文档，旧未保存状态失效。没有写入clarias目录。
- 当前仍无角色正式游戏效果验收；最新包runtimeValidated=false，目标active，不push。

### 本轮最终恢复点（取代前述LIVE）

- vanilla-fresh-native-01已STOP结束，failure=null、original_save_check.unchanged=true，共享Drs差异仍按归因未知记录。
- 当前唯一任务内LIVE为rework-latest-native-01，launcher47708，PID507068/tid507196/creation134375123357986812，映像为其case/game/BaldrSky.exe；使用014635的最新EXE/PAC、独立CP932标题兼容seed、正常Drs访问。首次自动更新选否；80秒快照仍running、guard0、Save成功alias14，但捕获仍黑，不能算正式验收。
- 无CDB会话遗留。Snipaste贴图暂时隐藏，图片未删。Photoshop诊断文档已保存并关闭，应用仍可能后台存活；不得强杀用户其它Adobe进程。
- 最近完整离线回归由root重新运行：46项、0失败/错误、exit0。用户原有六项功能WIP仍53行，游戏资源/存档/临时产物不暂存。

### DPI对照继续点

- 以上rework-latest-native-01已STOP且failure=null、原Save清单不变；不得复用旧launch.lock。
- 当前LIVE为rework-dpi-01，launcher53154，PID506552/tid500264/creation134375128993369855。同最新EXE/PAC、同兼容seed/native Drs，仅增加target/bhe-audit-20261026/dpi-aware-only-20261027.manifest。
- 原EXE无内嵌manifest/DPI API；mt验证新manifest通过。只读GetProcessDpiAwareness返回HRESULT0/value1，声明实际生效，但仍黑，故不作为修复或发行变更。Win32 Alt+Space系统菜单可用，普通游戏菜单输入仍缺可确认响应。
- 当前仍等待用户肉眼确认画面以区分真正显示问题和捕获问题；该阻塞在前一目标回合及本自动继续回合均存在。前回合有工具/产物复审与提交进展，本回合有已验证DPI对照进展，不能将过程存活或这些反证升级为玩法验收。

### 原生装备分组核实与修复

- 从原版教程与EXE消费者闭环确认DAT普通列=3*state+button，state0近距離、1サーチダッシュ、2遠距離、3ダッシュ；同按钮会向后回退。旧九角色预设把多数射击放在普通Dash槽，已只交换6..8与9..11，其他槽/身份/153限制保留。
- root实际RED14tests/10failures/0errors，GREEN14/14；完整16类113项全过。独立native审查者复核九角色数组、空槽与+40身份夹具，未发现Important，可提交。
- 新整链构建session32213已exit0，run_20261027_035924_938/build.json，最新assetRoot为motoki/motoki_assets_20261027_040105_624。EXE SHA09ff572b7d42c2aedbe092e5ad40583377d1cd258d65065bfc7b77d92df31f68，PAC SHAa525ad5a1d285123845932dd3d2f2cbb14c9e6a8311d90b9f4ffe16ccc615e24，root重新计算匹配。runtimeValidated仍false。
- 中文说明书已补默认键位、FC1 A+B/initializer B+C、训练菜单说明、改造招式默认入口及形态出入，未猜主菜单分页或预览操作；旧存档需自行核对分组。
- 当前live仍是使用旧014635包的DPI诊断case，不能把它的进程状态归为035924新包的游戏通过。尚无角色正式玩法验收完成，目标active，不push。
- 035924最终产物独立检查通过，root读脚本后实际重跑：九角色行104..112远距／普通Dash身份正确，其余槽保持，九角色MEK/WAZ逐字节未变，19WAZ/141技能引用无越界，报告passed=true/errors=[]。审查脚本与报告位于target/bhe-audit-20261026/loadout-final-20261027，不暂存游戏资源或私有存档。

### 第三个连续目标回合的阻塞审计

- 上一目标回合分类为progress：a18faa3cd、113项GREEN、035924新包和操作说明。此回合实际复核launcher53154与PID506552仍live、身份匹配、捕获仍黑、原Save清单不变；不是凭旧state文件推定存活。
- OSK已尝试：Down拖住后game失焦，Shift latched后激活game的持续观察窗仍未出现DirectInput Shift/D；这不能证明实体键盘故障，也不能算有效游戏操作。OSK临时Shift已解除、窗口已关闭，游戏已重新置前供肉眼检查。
- 当前926个跟踪Java/FXML严格UTF-8读取及U+FFFD检查通过；113项报告仍全绿。无代码任务或子代理任务未收束，未重跑无关Maven或修改其它资源。
- 相同显示／输入阻塞已持续三个连续目标回合，缺少用户实际屏幕与实体键盘反馈；原版／空存档／正常Drs／DPI生效对照均未补足可见、可操作的游戏证据。当前判定为blocked，不是complete或主动paused，九角色原目标不缩减。
- 保留供检查的唯一任务游戏：rework-dpi-01，launcher53154，PID506552/tid500264/creation134375128993369855；使用旧014635包，仅用于显示／输入对照。最新可供后续验收的是035924，runtimeValidated=false。不得复用任何旧launch.lock，不得把旧窗口存活记为新包通过。
- 继续需要用户确认当前隔离窗口肉眼是否也黑，以及实体方向键／回车是否响应；反馈后再按显示或捕获／输入路径继续，随后必须完成最新包九角色全量玩法验收、最终说明书和交付。

### 地狱入口闪退取证与恢复（取代上述阻塞和LIVE状态）

- 用户已确认实际窗口可见，从主菜单进入地狱模式立即退出。等待肉眼反馈的阻塞解除，黑色捕获不能当作产品显示故障。所有旧游戏及CDB已结束，不复用旧PID或case锁。
- 035924九角色包在CRT入口6795F8命中，游戏调用者455BEE；已保存机体ID113超出有效0..112运行表。是错误跳过原有Sou构建阶段导致，与计划“排除Sou重做，保留现有实现”不符。当前runner已恢复Sou阶段，不改写原存档选择。
- 原Config和不可变兼容seed均含选择113、五份114行统计表和114条运行记录，精确解析到EOF。不能使用被113容量试跑重写的私有Config作新seed。详细地址、hash及CDB退出限制见hell-entry-crash-20261027.md。
- 恢复完整链的首次构建在Sou最终封包遇到既有直接地图SPM格式问题，B6_ES02_ob01在BSDX解析5561处失败；正在通过既有转换器修复直接物化路径，最终校验保持启用。
- CMekaLoader的103项固定数组／费用表属于独立后续加载风险，仍在核完整原生消费者，不能归为本次入口栈原因。尚无修复后地狱或角色玩法验收，runtimeValidated=false，不push。

### 完整包重建与地狱入口实测

- 地图转换RED为1项预期失败：旧输出5561字节，DX转换5537字节；GREEN为5类20项全过、无跳过，包含完整静态资源根的137地图路径。
- 094119完整包构建及最终资源校验／PAC解包字节比对成功，Sou保留在113；原生运行容量114、初始化494304字节，CRT原入口保留。EXE/PAC hash见hell-entry-crash-20261027.md与build.json。
- 新隔离case rework-hell-sou-01已从114行不可变seed启动，root通过UI实际进入HELL SYSTEM设置菜单、打开机体选择；455BE5断点确认选择113／运行表114，正常继续。之前入口崩溃已实测消失，未改写原存档。
- 此轮截图能正常显示，不再等待旧的黑屏反馈。094119仍未包含独立CMekaLoader布局修复，不能拿入口通过外推加载和武装效果。该固定布局回归已实际RED，布局补丁正在单独验证。

### 用户要求暂停（2026-10-27 10:20 JST）

- 目标已设paused，所有子代理中断。停止新增排查／测试／验收，只固定已经取得的代码、产物、证据和待办，详见checkpoint-20261027-paused.md。
- 地狱入口已实测修复；月詠新武装说明可读，但预览双方不可见，用户已确认肉眼也不可见。尚未完成原版机体对照，不能判为捕获问题或宣称技能效果通过。
- 原生loader布局与实际Apply合计9项通过，已提交6f827a43f，尚未重建该补丁的游戏包；费用表103行问题另行未解。独立地图同批冲突gate已测过，Pipeline级事务gate尚未实施。
- 当前唯一游戏case rework-hell-sou-01已STOP，CDB已detach退出；failure=null、cleanup_pending=false、原Save unchanged=true，旧PID已不存在。日志、兼容seed、原存档、旧包及原有53行WIP保留；不push。等待用户重新开启目标。

### 用户恢复目标与EXE基线纠正（2026-10-27下午）

- 用户已明确“重新开始直至结束，直到再次说停止”；恢复完整目标，旧暂停检查点保留为历史。原有六项WIP仍53行，不push。
- Pipeline级资源冲突实际RED1项（MapGroup错误改写342234→342289）；在prepare后发现resource冲突即清双方pending并停止事务，保留单次冲突计数。GREEN4类20项全过，无跳过。
- 摄像头对照已确认此前runner选错EXE：bsdx_game的2f8c带1920×1080改动，原默认ida-reverse的713cf保留800×600。相同诊断seed、相同原版资源无移植PAC下，713cf能显示甲与敌人、タックル命中；2f8c只地图／热条。详见render-baseline-comparison-20261027.md。
- 入口已改回713cf并在receipt记录源路径/hash；155314完整构建成功，最终EXE1ac677…、PAC仍e004bc…字节未变。原生114槽布局已进入该EXE，CRT保留。新BHE实机验收准备中，runtimeValidated仍false。

### 正确基线重建及首项预览事件（2026-10-27 17时）

- 费用输出与原生容量/异常链/分层交付共5类37项GREEN；补充坏继承表、103→104、103行不扩容的6项边界测试亦GREEN，均无失败/错误/跳过。坏输入拒绝发生在费用表重写前，不宣称整个Importer输出事务原子。
- 170134完整构建成功，Sou与ID113保留；EXE仍1ac677…，PAC变为a8869c…，114行费用表原103行前缀逐字节不变。新ID的100M是调度权重政策，force/release/queue实机验收尚未完成。
- 新case reference-preload-01已实进地狱、选月詠、打开装备。镜头及双方显示正常；武装19「袈裟・一閃」取得真实REQ→START→敌人45伤害（6000→5955）→原生NATIVE_READY，重复多轮。其余武装、训练与角色矩阵仍在继续，runtimeValidated=false。
- 旧resume-bhe-reference-01新增七hook观察器安装后发生AV，未有有效回合；不归因角色技能。观察器已停用，原现场与dump保留且不提交；本轮仅用既有隔离工具和CDB软件断点。

### 恢复实机与Sou菜单越界（2026-10-28）

- 旧reference-preload-01游戏、launcher与CDB句柄均已不存在；state残留running不能当存活证据。独立复核原Save78文件hash无变化。新case reference-preview-20261028-01使用172154包（与170134的EXE/PAC逐字节相同）。
- 用户补充Sou滚底闪退。新case的CRT断点实际捕获meka113、weapon153、统计bankCount153，caller4BFC40；小型dump和栈保留。最终Sou211条无hidden2，原测试仅证明Material对齐，未覆盖该菜单消费者。
- 在现有Sou定制器仅对>=153编号OR隐藏位2，保留211条及Material/AI/WAZ；initializer174也因此不可菜单装备，详见sou-menu-bank-20261028.md。真实数据RED1项预期失败后，Sou11项+默认装备14项GREEN；修复后整包与实机复测尚未完成。
- 本轮10类63项首次因缺媒体路径跳过10项；补齐`-Dbhe.staticResourceRoot=D:/BDY/NeXAS_Resources/bhe_resources`重跑地图4类20项，失败/错误/跳过均0。普通缺图仍非致命。
- 终审1044文本未发现新增源码乱码；两份initializer文档12行问号损坏已按现有清Voice、保留SE实现恢复可读说明。未机械重编码全库，原Clarias53行WIP保留。
- 月詠銀鈴的残留CTama定位到BIT44：slot71无限loop0..147，射击入口却在159/165，源未完成技能也有同一矛盾；15EC是对象累计时钟，不能误当slot71时钟。角色适配与实机复测继续中，runtimeValidated=false。

### 已提交组件与实机边界复测（2026-10-28下午）

- 本地develop提交13a106078修复initializer文档12行乱码；7aae3ef6d修复Sou菜单统计边界；13860e0d2固定正确EXE/hash、费用输出和地图事务冲突门。不push，原六个Clarias WIP仍精确53行未暂存。
- 161306整包EXE1ac677…／PACb8a6a6…在sou-menu-fixed-20261028-01实机逐页滚到末端，152「朱血染のエッジレギオン」取得真实START、6000→4681扣血及多轮NATIVE_READY，未再CRT；关装备后正常进入训练。case已STOP，failure=null、cleanup_pending=false、原Save unchanged=true。只证明该菜单边界回归，不扩大为全部玩法验收。
- 銀鈴测试初次出现两项夹具错误（缺Self重绑）不计RED；正确夹具继而暴露311处共享SE byte[]写回源DTO。CrossRedirect现对每条SE copy-on-write，仅成功后替换目标列表；源8WAZ不再被污染。该修复不等于所有converter均彻底隔离。
- 有效RED为P0/P1窗口159/165均不在本槽loop0..147；post-Cross按appendPlan定位本轮BIT44并深复制，仅改四个frame int。45项GREEN，无失败/错误/跳过；源对象、原DX二进制前缀及BIT65/66/58字节均保全。
- 162851整包成功，EXE仍1ac677…，PACc26612…；独立比较与161306双方各3884文件，只有tama03.waz四个DWORD不同，其余3883文件逐字节相同。新silver-bell-fixed-20261028-01准备并启动，实际射击/回收等待验收。
- 原Save默认攻击映射仍是Z/X/C/V；当前自动化字符键会触发日文预编辑，而Escape能切换训练底栏。正在查正常游戏键位设置路径，只对私有诊断存档处理输入问题，不改用户原Save或产品默认键位。

### 用户新增连段可玩性要求（2026-10-28）

- 用户新增第7条：新技能必须与其它技能组成连段、达到游玩程度，并参考互联网与非BHE现有角色。此要求是最终验收硬门；当前单招预览命中／回收不替代连段通过，不缩减九角色、全武装、训练与加载调度目标。
- 已并行开展DX网页实战连段资料与原生取消／装备槽／受击链调查。尚未把只看slot47或Hit.cancelFlag当作可连段结论，需记录跨武装实际起手、持续连击、heat与敌受击状态。
- Motoki新增自主起飞方案固定在motoki-flight-design-20261028.md与docs/superpowers/plans/2026-10-28-motoki-ground-flight-bomb.md，沿本角色单phase路由＋私有三phase执行，不改generic架构。SDD ledger在同名.superpowers/sdd目录，根代理独占Maven/UI/CDB/提交。
- 新Motoki真实RED5项均按预期断言失败，无errors/skips；首次实现GREEN31项出现5个slot40供体guard错误，不算修复通过，已交实现者核真实热量事件签名。生产尚未提交／重建，该源码不能与当前运行的旧162851包混证。
- 当前实机case仍silver-bell-fixed-20261028-01（EXE1ac677…、PACc26612…）。已对Yuri、Freja、Nagi、Misaki、Naoto、Katou、Wilhelm、Motoki主要变体取得局部预览证据，详见runtime-variants-20261028.md；Motoki原13／新17多数地面预览不命中，正常飛行前置未满足，仍未判可玩。

### 2026-10-29输入恢复和新图实测

- 恢复时旧游戏／launcher／CDB均已不存在，原Save全清单复核不变；不凭旧state的running续用。C盘新input-resume case中，用户实体F9使A绑定提示消失、A显示F9；自动F9此前未被捕获，不能称自动攻击输入已修复。
- 原生训练Replay输入路径已实机验证：正常UI保存甲的全零模板，只换新录像第三块普通按钮输入，正常载入／播放后真实武装0→2→23、3HIT301、使用链0→3，后两次命中前目标控制20／16。详见replay-validation-20261029.md；九角色还需各自模板、真实连段和中断验收。
- D盘约1.8GB，runner支持显式bhe.playableBuildRoot跨盘输出并直接继承绝对路径；完整run_20261029_093012_409在C盘构建成功。该runner／Replay工具及操作证据已中文本地提交723a8df8b，未push；原Clarias六文件53行完整留在工作区。
- Motoki方案改为起飞／准备／释放／落地四block，准备末端复核高度；新落地使用原生gravity50，避免200残留下一招。高度200是准入门，原生转场重排actor间隙仍可发生目标更新，不承诺任意瞬移目标的同步高度下限，实际有效／连段仍是硬门。
- 上述首个四block包静态33项GREEN、3884文件仅3处变更，但实机新17只升空后回READY、零伤害，验收失败。原因是TURN/INPUT先查SkillSuffix，而复制供体只带(0,0)，入口1不存在；原模型误把参数直接当block。另原倍率(0,0,0,100)实际为0→100插值，实测重力倍率6，不能称恒100。
- 已新增真实失败回归：suffix8tests／5failures，modifier1test／1failure，均0errors/skips。生产补四个独立入口和恒100端点(0,100,0,100)，新定向35项全过；新整链正在重建，尚未实机证明。单纯NATIVE_READY不再作为投弹／落地证据，失败包、错误观测和正确地址口径都保留于SDD ledger与target报告。
- 两个已结束C盘case都通过原harness STOP，failure=null、cleanup_pending=false、原Save unchanged=true。整体目标继续active，runtimeValidated仍false；Motoki生产修改尚未提交。

### 2026-10-29新包固定靶通过与本地保存

- suffix／modifier修正后的run_20261029_095154_853完整构建成功，PAC196bcd21d0645240a392dd9bf0cde8516fb8861e93e54a902804a4da36934ae3，EXE仍1ac677…；当前独立case为C:/NeXAS_DX_runs/runtime/motoki-flight-suffix-20261029-02。
- 新17实机按block0→1→2→3执行，648A00真实发弹时中心高度差204；首次3击407，后续3至6击。61CC30释放时block3／Change本槽clock36或37、Z0、G50、倍率100，随后原生结束TAMA0／SCRIPT0／active-1。仅固定靶预览范围通过，详见motoki-flight-runtime-20261029.md。
- 供体suffix漂移RED4/4/0/0后加本角色guard和router独立入口，最终39项0失败／错误／跳过；独立复审无open Important。根重生成真实095154 Motoki MEK/WAZ，逐字节相同且源不变，无需把guard修改误当新运行字节。
- 已本地中文提交8ec6e2aaf保存Motoki逻辑、测试、设计及运行证据；未push。原Clarias53行仍完整未暂存。整体目标未完成，不将该提交或39GREEN视为连段验收。
- 正常装备UI的实际5E1330提交已确认近距A17、B12、C11；准备在训练保存本角色空Replay模板再试独立输入。hover也会预览，单击选择／第二击装备必须区分；旧自动输入问题不靠改EXE或直接写actor绕过。

### 2026-10-29后台消费者审计继续

- 上一目标回合属于progress：两条本地提交、原生训练三武装输入及Motoki发弹／接地／回收证据改变了下一步。本回合重新查明游戏83032与CDB82380仍为正确case并存活；没有重启旧case，没有活Maven。
- 根在真实进程CDB读回0086CD58=2、0086CD5C=4，支持当前Hell训练mode4。原生预算事件只在mode3关闭；正常敌种最后实例被击毁后32次wrapper更新可last-ref释放，未证明一般可击毁敌人永久等待。
- 独立重解析BQUnit实际99行机体字段均0..102；正常CQ玩家原生固定0，菜单只mode1／4应用选机；原103机体无switchToMekNo跳新增ID，最终包未覆盖BQ生成表或任何BIN。当前正常CQ不会加载新增100M费用行，不为假设三新增CQ机体改变政策。未来新增CQ入口或人工脚本／存档注入须重新验收，详见target的preload-mode3-scope-20261029.md及数据。
- 说明书已按实际UI更正“hover预览不等于装备”、两次单击装备、进入训练路径、Motoki自主起飞行为及095154包身份；本地中文提交5e839477e，不push。仍明确九角色连段未验收，不填未经实际运行的路线。
- 当前1083文本重新严格UTF8读取、无BOM／FFFD／NUL／C1；相对旧审计52项新增／变化、旧路径无缺失。四个私用区字仅在历史坏字引用，源码两处问号串仍为未知分支注释，不据此猜语义。证据encoding-refresh-20261029.json；本轮技术刷新不替代全部自然语言语义审读。
- 九角色171武装／285技能正在独立进行SkillSuffix入口专项；所有玩家入口0已证有效，三处源原有INPUT越界分支须继续区分有意结束与缺失恢复，尚未据裸索引改生产。Nagi反击正常CPU设置与有限输入时序也在后台核实。
- 前台仍在其它应用，先不使用旧坐标操作；连续前台测试便利询问待回复。当前Motoki模板尚未保存，不能用已有甲模板改角色ID来假冒本角色连段。

### 2026-10-29目标空投与实际连段进展

- 用户已明确允许连续前台测试，整体目标继续active。当前唯一case为C:/NeXAS_DX_runs/runtime/motoki-target-drop-20261029-03，PID59844，CDB136608／PTY9965，launcher79869；旧suffix case已正常STOP并验证原Save不变。
- 原095154随机散布在另一份合法seed上全空，不能以旧固定靶成功结束。新revision3只改17父Effect七个白名单Unit为目标XYZ+80、零散布、水平初速0、Vz-24、G50，保留原13及公共468。42项定向回归在14:57再次全部通过，0失败／错误／跳过；131244新包完整构建和产物审计通过，3884文件只有motoki.waz变，11317引用无越界。EXE仍1ac677…，PACa94c6c…，整包runtimeValidated=false。
- Motoki008／026两份原生seed均实际生成六弹并命中，总13Hit1204；补观察026时从11伤害到17首伤无61E760恢复，state8击飞输入为空，不能仅control0否定连续受击。17→12虽正常取消START，但12零伤，不算三武装伤害链。
- Motoki030独立地面17为12Hit1125并正常接地；032通过普通D与方向移动重新接近，同一录像两轮11→17各1204，HP6000→4796→3592，P3自然落地293／783，最后active-1、W0、Z0/Vz0/G50、倍率100、heat0、TAMA0/SCRIPT0。029／031二轮仅输入未START的失败证据保留；近距装备不能在远距自动回找，未据此改引擎。详见motoki-target-combo-runtime-20261029.md。
- Katou10→20→6在早时序278、中时序307及同录像二轮614均有真实三武装伤害链；新20两次均命中、后继控制为正、最终清理正常，详见katou-combo-runtime-20261029.md。当前仅固定甲靶和两档时序范围。
- Naoto正常UI模板075保留109／敌0／map68及真实A0／Dash B20／Dash C1。076早时序30／42／74实际0→20内部两拳→外部1，4Hit137、后继precontrol5／4／7；079中时序两轮各137、HP6000→5863→5726，后继precontrol3／4／5，最后地面W0、active-1、G50、倍率100、heat0、TAMA0/SCRIPT0。077／078仅生成不计实测，独立运行报告已固定在naoto-combo-runtime-20261029.md。
- 使用说明同步最新包与已测路线，仍保留全171菜单武装、其它角色连段、移动／小型／主动CPU／墙边／受击中断、Nagi成功防反、Freja高低FC预算和实际关卡推进等未验项。局部连段不等同于整包或九角色完成；仅develop、中文本地提交、不push，原Clarias六文件53行保留。

### 2026-10-30恢复与新增路线

- 月詠实际装备近A8／Dash B19／Dash C2。081窗口候选的第三武装虽START却零伤；085将C提前至新19命中后2帧，真实8→19→2三Hit151、后继控制56／49。086同回合反向两轮各151，HP6000→5849→5698、轮内无恢复，末尾地面W0／active-1／G50／倍率100／heat0／TAMA0／SCRIPT0。082至084仅生成不计实机，详见tsukuyomi-combo-runtime-20261029.md。
- Yuri已验证两条不同连接：034上翻踢3→侧踢4连续229，随后匕首1启动但零伤；036匕首1→普通匕首0实际210+三次13共249，弹体5/329的owner和目标可对应，末尾地面W0／G50／倍率100／heat0／TAMA0／SCRIPT0。1→4门后侧踢仍未命中，BOUNDS实读倒地目标hurt0..14、攻击者Z34以上；不靠无限扫输入或盲改硬直凑路线。034末尾G100已核来自原BHE W15 block1，不能借036状态写成全部G50。039重新接近后的第二轮尚未实测。
- 临时input JSON在生成fixture后统一CRLF，使7件input源hash不同于旧receipt；已按旧SHA严格重建原LF字节为排他.bin sidecar，逐件核JSON值相同、编译结果等真实Replay第三块、Replay整文件仍匹配receipt。旧receipt和录像没有覆盖，独立审计两份均通过；三份运行报告已补准确对应关系。
- 已本地中文提交0da38d63d保存Motoki目标空投修复，f9b0f46c8保存Katou／Naoto／Motoki路线和说明书，不push。原Clarias六文件53行未改未暂存。
- 旧case03于10月29日20:02:31以code0退出，case04于21:41:55以code0退出；两个退出事件均非exception，launcher正常完成、cleanup_pending=false、原Save全量不变。case04仅有菜单preview，没有可验收的普通Replay输入，不算039通过；各历史exception计数和共享驱动变化照实保留。
- case05鼠标采样正常，但旧标题画面与native状态不一致；真正标题走脚本选择器，ID75实为隐藏滚动条，不是Start。最小化恢复后黑，render loop、TestCooperativeLevel和Present成功仍不能证明像素正常。根保留现场后通过harness STOP，游戏停止码E0000001为主动终止、工具会话exit0，不能混作游戏自然exit0或异常崩溃。原Save不变、cleanup_pending=false，039真实输入0。关闭证据见`target/bhe-audit-20261026/closure05-20261030/`。
- case06沿同131244包、case04只读seed重开，仍捕获黑屏；根启动CDB时遗漏交互终端，EOF导致DEBUGGER_INACTIVE退出C0000354，不能当游戏自身崩溃或运行通过。该操作失误与现场保留；以后CDB显式TTY并用-pd确保调试器退出时分离。case07已独立启动，PID240480、CDB PTY80824、launcher51443，Sky窗口10093462；当前标题native为scene1／kind40000／有效脚本选择器，仍捕获黑屏，没有新连段输入。未复用case锁／PID，目标仍active，整包runtimeValidated=false。

### 2026-10-30说明校准与交付整理

- 当前全部934个src Java及1111个相关文本重新筛查，没有新增确定源码乱码；两处未知分支问号注释没有证据恢复为具体语义，保留未知状态。旧txt清单BOM与既有LF另记，没有借机全库改格式或混入Clarias WIP。
- 最终九角色171武装／285技能的日文复审确认11条定制简介无重大逻辑写反，但Yuri后继推荐、Motoki投弹定位和Misaki敌方视角仍可误导玩家。只在三处既有Customizer校准说明，未改事件、动作、伤害、热耗或架构；手册的Motoki12准确名已单独中文提交7c9d6a270，未push。
- 四类36项相关回归0失败／错误／跳过，随后267.305秒完整构建成功，目录run_20261030_031642_167、EXE032001且hash仍1ac677…，PAC53711b4d74162e0e3c3b008d1c8d53c3b222ac89ae23efda5beaea012440ee5b。独立全3884资产对比仅三个MEK指定说明变化，20个WAZ全相同，18原始块其余字节保全；合408字符串CP932严格往返。详见japanese-descriptions-20261030.md，新包runtimeValidated=false。
- 已从原131244整理独立开发候选C:/NeXAS_DX_runs/delivery-candidates/BHE_DX_131244_development_20261030_031021：同轮EXE／PAC统一安装名，附7c9d6a270版手册、七份引用报告、原receipt、14项基础PAC身份及逐文件校验清单。15文件由独立代理全hash复核通过，无Save、Drs、诊断PS1或原版媒体混入。该候选保留为原实测包，不混作032001；后续候选绑定各自说明与hash。
- case07仍为旧131244且持续存活，本轮重新捕获仍黑；未重复开同样case、未使用内存强制菜单／出招。用户肉眼观察问题仍待回复，Yuri039及剩余角色连段、Nagi成功防反、Freja预算、全武装与关卡推进未扩大验收。目标继续active，不将文案或交付完整性当玩法完成。

### 2026-10-30画面恢复、Yuri039与新增目标

- 用户确认肉眼为游戏初始菜单，并补充此前盖上电脑盖。根重新activate同case07后捕获完整初始菜单，正常进入地狱设置、训练场和Replay列表。画面读取已恢复，不是修改游戏数据得到的黑屏修复；没有重开游戏或切换EXE。持续前台授权继续有效，后续测试保持电脑盖打开。
- 039通过正常载入自动PLAY至STOP18.00；两轮1→0各210+13×3=249，总498，普通方向360／390后第二轮真实START1=470／0=576，HP6000→5751→5502。两轮Hit之间各自无恢复或清链，真正恢复222／662、RESET223／663，最终地面W0／active-1／G50／倍率100／heat0／TAMA0／SCRIPT0。两份独立复核通过，原始BEGIN..END5399字节与活log前缀分别冻结，日志未关闭；详见yuri-repeat-runtime-20261030.md。
- 用户新增第8项：Sou每个已列出的技能都须无闪退；第9项：其余九角色各追加不少于六个技能；第10项：有成果即中文本地提交并固定产物与记录。按每人至少六个独立可装备追加武装执行，不把initializer、私有执行阶段或仅改原招参数计数。目前七角色各1、Yuri／Freja各0，总7／目标至少54，至少还缺47；最终字节盘点与逐角色设计正在进行。原有稳定性、实际效果、连段与说明书目标不降低。
- Sou仍保留现有实现与DX玩家武装0..152限制；已列出的菜单技能逐项纳入预览、选装、使用和收尾验收。高号58项隐藏不是扩容，不能把“没列出”替代已列技能验收。新范围在docs/superpowers/plans/2026-10-30-six-variants-and-sou-validation.md固定，主代理负责方案、串行构建／UI／CDB及提交，不由子代理随意扩架构。

### 2026-11-06冷却修复实机对照与同场复用

- 目标已重新active，11/05暂停交接只作历史。恢复HEAD为d8b9f5d59，develop未变；旧Clarias六文件53行WIP及sou-menu-bank既有修改继续保留。仓库外禁止写删、中文提交、不push、不建分支和worktree的限制继续生效。
- 当前唯一受管构建仍为run_20261105_193350_342_bd794139，EXE1ac677…／PAC8efe934e…；本轮重新核对源EXE713cf…及构建hash，没有重建。新case12使用case10私有Save作为种子，真实Save仅作为只读保护基线。
- Replay013与旧基线869字节／e5593f…完全相同，两轮均按原生输入frame30/90/150启动0/133/2，伤害120+2+142、敌HP5736。新包取消133时heat1900，旧包2300。有效RPM601样本中counter136首减100、284降至0；body P0预算7→210，P1按原预算耗尽，379转P2、400对象清空。128拒读、首轮无效采样均保留。interval字段3不等于实际每3原生帧；实测body clock以4递增的扣热变化见专项文档。
- Replay014只在普通输入块加入第二轮，保留模板头和前两块；同一场frame630/690/750再次启动0/133/2。第二次普通B落空，133启动后到C前伤害13、C后150，最终敌HP5573；没有补算落空伤害，也没有认定真连。两次取消时heat1900、C释放1200、RESET0；稍后身份核实RPM见counter1200/inactive、heat0、TAMA/SCRIPT0。
- case12自然UI退出，launcher返回0，status=exited/failure=null/cleanup_pending=false，自有进程与guard均确认收尾。受管清理28份输入副本5,805,717,300字节，原Save/base/source检查未变。全会话55条system/unclassified异常、外写拦截计数0、guard违例空；nvAppTimestamps漂移归因未知，不称所有系统状态不变。
- 新证据只写src/test/resources/bhe-runtime-evidence-20261106的小型文本，冷却说明及中文使用手册同步当前身份与有限结论。Astra只读调查另确认Sou34的公共蝙蝠P2及主caller P1都丢了BHE slot23；这是三维方向速度，不能直接猜作XYZ三分量或全局23→18。首击524未命中与后续525运动缺失分开，调查见sou-bats-motion-review-20261106.md，尚未实施。
- 下一步仍含133受击中断/档位、全部Sou153安全、九角色各六追加武装的实际效果与跨武装真连、重载/关卡推进，以及最终逐项审计。runtimeValidated=false；关闭本次测试会话不表示暂停目标。

### 2026-11-06 Naoto六招菜单与20真连复测

- 上一目标回合为progress：6aede1fb4固定133主动减热及复用证据。本轮转向九角色54项，最新包身份仍8efe…，无重建。新隔离case为nine-naoto-20261106-13，PID9552／创建FILETIME134383992159715102，launcher70253；从case12私有Save拷种子，真实Save只读保护。
- 正常UI选择Naoto109，六个新增20..25首轮菜单分别有75／150／315／261／19／292伤害。25父释放仍TAMA3，释放后六击合112，下一周期对象归零；没有给它补RESET。Sou专用请求/READY跟踪不覆盖109，只固定通用START、伤害、释放及相邻周期边界，绝不称同请求五链完成。首轮22前一个21自动周期被切换，独立保留，没有拿更早周期的结束充数。
- 正常UI装备近A0、普通Dash B20/C1，其余槽空，保存Naoto模板Replay015（858B、3ea35245…）；fixture仅替换第三普通输入块生成016（876B、c26b9ae6…），900帧、A30/B44/C78及第二轮整体+420。两轮START与普通输入逐tick对应，四击30/27/40/40各137，后继命中前control3/4/5，敌同actor末HP5726；目标恢复123/543，正常RESET166/586。20内部第二拳没有额外START，仍是一件武装。
- root独立身份核实RPM最终快照在tick114655稳定，counter900/inactive、meka109、active-1、heat0、TAMA/SCRIPT0、Z/Vz0、Graw51200、jump/fall100、武装装甲贡献及缓存0、防护flags0。它是稍后读回，非native录像末帧。两次20→1时SCRIPT曾为4，未把最终归零写成全过程为零。
- Astra另确认22展开快路径判的是目标ACTION_NOKEALL，不是变量名暗示的自身空中；最终二进制条件及入口已核，保持源码不变并纠正文档。Sou34蝙蝠P2的[3,19]已证是回root机体中央，转换后[3,0]丢CENTER；不能用追敌跳跃力代替其高度。只更新调查结论，尚未实施蝙蝠适配。
- 当前新增训练范围只有Naoto20的一条固定目标真连及正常取消/复用，另五招仍只有菜单局部效果；全部受击中断/落空/SD条件、其它角色及Sou153目标继续active。case13仍运行，下一步在同一隔离会话中继续Naoto其它招，不能套用前轮退出记录。文本证据仍不含二进制。

### 2026-11-06 Naoto21回撤、失败后继与两轮真连

- 继续现存case13，未重开、未构建。正常UI将nearA设21、普通Dash B0/C1，保存017模板（865B、2045e081…）；按相同模板只替换普通输入块生成018～021，未覆盖015/016或原Save。
- 018只有A[30,34)，360帧；实际150伤害，父W32clock70释放，随后RESET及热量/对象归零。361条RPM中clock40～47实际每步后退12，共96；clock48停止。整招初始X2302、末2242，净位移-60，不能把回撤96当整招净位移。clock34时fall倍率恢复100，过程SCRIPT2/4与22次拒读完整保留。
- 019 A30/B66/C82失败候选：B输入后实际frame67正常取消到Jab，但Jab无命中，C82无START1；不能认定真连。该次47拒读与最终正常收尾分别保留，不把“输入被接受”当作后继有效。
- 020只A30/C66，实际67取消21→1，150伤害frame64、后继40在frame91且敌control6，直到119才恢复，形成有限两招真连190。361RPM和39拒读包括双方位置，但同tick更新先后不覆盖native伤害/控制值。
- 021把上述两招普通输入在同场再做一轮（A450/C486），两轮各190、同敌末HP5620；后继control均6，目标恢复119/539、RESET155/575，RESET时heat2900。901RPM和94拒读保留，末counter900/inactive、heat0/objects0/Z0/Vz0/G51200/倍率100/武装装甲与防护0。成功提前取消发生在21clock34，不能代替018完整回撤。
- 钩拳的日文说明源码仅把后继“ジャブ”改为已证实的“ストレートパンチ”，一处字符串替换可逆验证、CP932严格往返、58字/116B、UTF8无BOM/CRLF均通过，动作图与逻辑未变。本轮不为文案重建整包；当前8efe仍含旧日文，后续正式构建须封入该说明并单独记录成品差异。
- 中文手册、验收矩阵和Naoto专项更新，当前Naoto训练真连/普通取消/同场复用覆盖20、21两件，另外四件、落空/SD/中断和全部原目标仍未完成。游戏留在当前受管会话继续验收，runtimeValidated=false，不push。

### 2026-11-06 Naoto23／22独立动作、重击连接与复用

- 延续case13/PID9552及8efe包，正常UI模板022读回nearA23、普通Dash B22/C0，其余空。023/024各480帧仅普通A30/B30，头及前两块均保留；独立下击两轮各261、独立展爪315，均自然转W18収納并恢复物理/heat/objects。
- 023首次采样等待120秒先到期，实际LOAD晚4.317秒、START晚4.920秒；空采样与随后重播的481样本/53拒读分别保留，没有覆盖失败。等待播放期限改600秒，实际播放期限仍60秒。为减少界面轮转，root将已指定正常回放交单一UI操作员串行执行，root负责采样、方案、审查与提交；同一时刻仅一个UI控制者，未扩大到其它程序。
- 024主W34 P0慢分支→W33→W18；完整热曲线先2600、后1600/600/0，不能仅用release/RESET0写全程零。原展开slot40/FF仍工作，root决定保留排热特色。仅新增一处日文简介“展開中は排熱する。”，动作与热事件未变，当前sealed包未重建。
- 025 A30/B68实际frame69取消23→22；RPMcounter70 W34P0且同靶state8/control30，71进P1，140进W33，支持受击目标分支。10Hit总423中前三轻击111不构成已证持续控制；从重击150至22六击162为7Hit312的控制段，后继precontrol17/19，敌290恢复/291RESET。热曲线2400→5000→3000→1000→0。
- 026追加C164，正常从W33取消到Jab0（SCRIPT2），但Jab未命中，不接受第三武装真连；601样本/80拒读及尾部正常收尾独立保留。
- 027同一播放中正常D320..324重新接近，再A500/B538复用；两轮各423总伤害、各有7Hit312控制段，总846、同敌末HP5154。实际state3采样counter321起、385回0，D属于恢复后移动而非SD取消；第二轮正常START23/22，目标恢复760/RESET761。1101样本/169拒读保留，尾1100热/对象/物理正常。无需人为位置/状态写入。
- 固定三份小型文本证据及报告；Naoto20、21、22、23现有训练连接/普通取消/同场复用，24/25仍待训练。所有落空、SD取消、受击中断及其它角色/Sou153原目标继续未完成。root授权UI操作员正准备下一组nearA23/DashB24/C25的新正常模板028；保持同一受管会话，不新建大型运行副本。

### 2026-11-06 用户要求暂停与成果固定

- 用户最新指令为“稍微停一下，固定成果，我启动的时候再重启目标”；覆盖此前继续执行指令。当前仅整理已完成证据、关闭测试会话并提交，不启动035或其它验收。
- Naoto029／030独立24／25、031受控159主段、032控制空窗与033／034射击顺序对照已固定。034主段24击302受控，但全28击414末1点有空窗；24／25同场复用仍未验。035仅准备LOAD确认，未播放；036未生成。
- 本轮源码只有24日文简介一处替换，明确射击开始额外发热；动作不改。此前21后继推荐、22排热文案及本次24说明尚未封入当前8efe包，不为暂停收尾重建。
- Nagi六招静态风险与D搜索取消／专用SD区别一并保存，不冒称实机通过。最新状态、关闭结果、恢复首步和未完成目标集中于checkpoint-20261106-paused.md。只提交源码／文档／小型文本证据，不push，既有Clarias六文件53行及sou-menu-bank改动保留。
- case13已由受管STOP关闭，status=stopped/failure=null，owned exit及guard完成，cleanup_pending=false；自动清理28份5,805,717,300字节副本。原Save/base/source保全通过，345条system/unclassified及驱动时间戳漂移保留边界。游戏与launcher进程已消失，目标待用户恢复。

### 2026-11-10 用户恢复目标

- 上次目标回合为progress：ff82058a2固定Naoto射击结果及暂停。现用户重新active，旧暂停不阻止继续。develop／既有WIP／latest与旧case终态已核；新唯一case14已启动至正常训练录像菜单。详细身份、035／036输入、工具与后续证据见resumed-validation-20261110.md。
- case14在035确认前遇到启动器PermissionError，已有事件证明由launcher主动结束而非技能崩溃；受管副本已清理。真实Windows状态文件读锁冲突已复现并加有界replace重试，70项session及17项sampler离线回归通过，两轮独立复审通过。下一步新case15实测035，不用离线工具结果代替游戏。
- case15仍同封存8efe包，035第二轮完整采样确认长按三发/一次额外热/正常尾部；036揭示一次D接近不足、第二A未START的失败，040只补D510后两轮均23/25/24 START，累计821但仅302/294主段有连续控制，尾弹空窗保留。037/038/039证明闲置搜索冲刺、机枪第三发后D取消与早D拒绝；未计专用sdGate路径。五份精简JSON186178字节，不提交二进制。
- 采样循环的状态文件高频读取已移除；每次仍核同一内核进程句柄的PID/path/FILETIME/exitstatus，初始/收尾case元数据有界读取并严格验证。23测试及独立复审通过，实际035第二轮至040成功采样。下一步041～045其余五招搜索取消，已生成正常输入但未播放；详见resumed-validation-20261110.md末节。

### 2026-11-10 取消补齐、真实空挥与原入口清单

- 前轮progress为519fcc963/a375e9183及035～040实测。此轮重新核case15同PID/创建时间/launcher85485仍live后继续，不新建运行副本。041～045分别补20/21/22/23/25的D搜索冲刺取消，合24已有六招；保留20渐进fall、被跳过后段及25尾弹，空中/中断不冒称通过。
- 正常UI重新选择Naoto并保存R模板046，解析确认完整14槽/109/0/map68/行为0及2946全零输入。047移动校准后，048在远距真实START23、两Hit窗无命中并自然収納，hold不重启；049/050射击仍命中7/96，作为失败的miss候选保留而非凑通过。当前正常UI准备Jump2新模板051，后续先测高度周期；仍只一个case15。
- 补164原入口候选清单及独立无崩覆盖，避免把54新副本代替原招。原样运行证据、菜单可见/可装备与静态候选严格分开；Sou153及重载/关卡/乱码/手册原目标不减。详细当前记录见resumed-validation-20261110.md、naoto-search-cancel-20261110.md和imported-original-weapon-coverage-20261110.md。
- 后续051正常UI Jump2模板已读回，root亲自执行052无攻击基线及053/054两项高度对照。敌周期107/峰Z140；24三弹均自然到23、25两主体各走49/119/5，均无敌伤害/反应且最终回收，当前落空覆盖23/24/25三件。停止后NPC仍能跳跃，native窗口按实际tick截断。下一步M布局20/21/22停止靶模板055，之后真实AI来袭；case15保持运行，目标active。

### 2026-11-10 用户再次暂停

- 目标状态已由用户设paused。本轮仅正常UI保存M模板055并生成056/057/058，三份未播放；056停在LOAD确认未点Yes，等待采样在暂停边界启动后已随受管STOP结束，0行，不当技能崩溃。case15已stopped/failure=null、owned/guard完成、cleanup_pending=false，自动清理28份5,805,717,300字节副本，源和原Save未变。
- 新交接checkpoint-20261110-paused.md与小型文本naoto-case15-pause-and-m-layout.json固定当前成果/未测项/新case恢复入口。用户恢复前不继续，目标未complete。

### 2026-11-10 暂停交接后再次恢复

- 前轮progress为8f3e0443e：固定M布局与未播放056～058并按用户要求关游戏。现用户重新active；已核develop、旧七WIP及旧case15退出/清理，未见残留任务游戏。复核三份待测输入/receipt/hash一致后准备唯一新case16，沿同封存8efe包和case15私有seed，不复用旧lock或PID。

### 2026-11-10 M布局三件落空与复用完成

- 上一目标轮为术语解释，没有改变验收状态；本轮先重新核active目标、develop及case16活进程，沿用同一实例继续。正常UI实际播放056～058，根采样与独立审计交叉核对，新增三份小型JSON证据，未构建或修改逻辑。
- 20两轮完整W31空挥且不进W12；21两轮完整W32空挥，实际回撤96后停速，整招仍净前进248；22第一轮慢展开→私有Rise两Hit窗→收纳完整落空，第二轮因靠近命中180。三件都长按不重发、同场再START及自然收尾，最终热／对象0、地面物理正常；22第二轮缺W33clock25／26，不补造、不计两次miss或空中验收。
- Naoto新增六件各有至少一个完整落空条件，当前矩阵改为6/6有限条件；真实受击中断仍0/6，其他八角／Sou153／164原入口候选／重载与关卡／源码乱码和最终手册交付都未宣告完成。下一步正常UI设主动敌并创建独立模板，先无攻击基线。

### 2026-11-10 主动敌基线与20真实中断

- 94faff472已固定056～058三招落空/复用及矩阵，未push。随后正常UI建立アイランナーCPU模板059与Kou CPU模板062；两份独立全零基线060/063实际运行，均核玩家109/M装备/行为3，头和seed没有脚本改写。
- アイランナーW8基础control0使本次伤害/物理反应不产生硬直，独立原生诊断后改用Kou；其实际首招为W44 Lv3，control16。064/065真实20起手clock6被打断、提前释放并进入state7，均早于自身Hit；不同于先受伤再按键或普通D取消。
- 064的A56与065的A67都没有第二START；065本轮已有RESET66，不能将唯一拒因写成按早于RESET。两轮尾均仍受CPU持续来袭，不能签稳定复用/尾部。四份小型文本证据和源诊断报告保存完整边界，不改原生反应、EXE或技能去凑通过。
- 当前case16仍运行，root独占UI，fullPractice菜单暂停场地模拟；游戏不另开。下一步只读定位普通出招门/冷却/使用记录与输入消费，必要时补最小只读采样字段。整包目标保持active，runtimeValidated=false；其他角色/Sou153/原164候选/关卡/手册等范围不减。

### 2026-11-11 固定四字段与20中断后复用

- 前一目标轮为实际进展：94faff472固定三招落空，7ec136685固定主动敌来袭和20起手中断；之后补四项只读采样字段并完成065原样重播/066下一帧对照。目标曾短暂停又恢复，本轮核active、develop和既有七WIP，继续完整范围。
- 新065的A67无第二START，066的A68真实START/USE并在W31clock37自然结束；热量归零与RESET不等于可立即接受新边沿。两新采样361行各无缺，19/29拒读和持续CPU尾TAMA1保留；旧065字段不补造。采样器24项离线测试及独立代码/证据复核通过，四字段均在原读取块内。手册同步实际落空/再用边界。
- case16已exited/exit0且受管清理完成，无残留游戏；原Save/base/source检查未变，55条未分类异常与nvAppTimestamps漂移原样保留。继续使用同封存8efe包，从case16私有Save准备新case17；下一步21/22关键准备段中断及恢复，原完整目标、Sou153和其余角色范围不变。

### 2026-11-11 新会话准备与桌面连接待恢复

- f4df67b7d已中文提交四字段、065/066对照、说明书及case16终态，未push。只在确认旧会话终态且无残留后准备/启动case17：游戏48692、creation134388534850760548，launcher52608/session10026，manifest SHA d451b2b2ba1ee119397cbae942f5f1c5768bcb938f7845191e221817aa40f654；EXE/PAC仍1ac677…/8efe…。
- 当前Computer Use原生管道不可用（native pipe unavailable/os error2）；轻量重试及重置JS重连均失败，已向用户请求恢复连接。未绕过正常UI，不重复启动游戏。067/068只生成未播放：沿case16正常062前缀813B，360帧分别B[23,27)与C[17,21)，其余空；123项独立文件核验通过，小准备清单明确runtimeExecuted=false。等待恢复后按各自真实来袭/物理结果审计，不把源预计当已中断。
- 等待期间补Nagi22全生命周期只读复核：不能仅凭Hit4..7/确认8..9静态宣判不可达；当前Hit为空时会继续使用共享比较缓存，Change/clock/Hit更新分属不同遍。旧范围文档已修正先后顺序表述，后续近距真命中与远距落空各一对照，未修改Nagi实现。
- 当前988源文件全量字节扫描与11/05基线/增量闭合，981相同、7变更/新增，7新增非ASCII行语义正常。两处未知类型问号注释改写为当前映射并保留未知语义，逻辑字节未改；小证据保存，未重建游戏包。全目标仍active，实机后续等待桌面控制连接恢复。

### 2026-11-11 控制恢复与21/22实际中断

- 新桌面控制入口已实际读取并操作游戏，旧case17游戏/launcher与exec session已消失但state仍running；退出原因和退出码未知，不归作exit0或自身崩溃。28份5,805,717,300字节静态副本经路径/Git/双源hash与独立核查均可重建，但执行删除被自动审批CreateProcess阶段拒绝，理由仅blocked by policy，命令未执行。预检/受阻JSON保存，副本保留，未换方式重试删除。
- 从case17私有Save建立唯一新case18，游戏71360/creation134388569299088782，launcher68672/session49818，manifest b15f5a9b71f189b54889efecf646edc465ccd7e152b9909acd2cc630b2f10c43；仍封存EXE1ac677/PAC8efe，未构建。root正常UI实际LOAD/PLAY067/068/069/070，不把旧准备清单改成已执行，也不覆盖旧运行。
- 067/068分别证明21在W32 clock10及22在W34 P0 clock16真实受击早释并恢复控制；069普通后退被更快Short Elbow追上，B200无START，保留失败。070拉距后C201真实第二START，完整W34并进入W33，两Hit窗实过但落空，clock59再次受击、未收纳。当前20～22准备中断及20/22再用有有限证据，21再用/整体稳定尾尚待。
- 四份小型JSON已由独立代理及root核原样native、关键RPM表、缺样/拒读和hash，共274,578字节；不提交二进制或截图。正文resumed-validation-20261111.md、矩阵和手册同步真实边界。下一步仅按已核W86先追角后锁方向机制做一次侧移候选，同时继续23～25及其它八角/Sou153/原164候选/重载关卡，完整目标不缩减。

### 2026-11-11 侧移复用固定与用户暂停

- 071保留B23并up53..86，实际避开W86锁角后的肘击；RESET本轮68，热与heatState77归0。072只加B80并延长空尾，真实第二START/USE，W32完整clock0..70、回撤后自然结束；counter188..210有连续23个接受样本地面可控、物理/热态正常且对象0。之后新的CPU攻击与整个720尾仍分开，069失败不改写。
- 新合并文本证据naoto21-kou-side-step-reuse.json为97,680字节，原样native/关键RPM/缺样拒读/hash均由root和独立审计复核；未修改技能、EXE或架构。矩阵与手册同步20～22准备中断及有限再用，21另有有界稳定窗口，其它阶段和23～25仍待验。
- 用户已将目标设paused。case18通过STOP受管终止，status=stopped/failure=null、子码E0000001、外层exec1原样记录；游戏/launcher/窗口均确认消失，owned与guard完成、cleanup_pending=false。28份5,805,717,300字节输入副本自动回收，Save/Replay/log保留，原Save/base/source未变；7条未分类异常与nvAppTimestamps漂移归因未知保留。
- 旧case17的28副本仍因自动审批blocked by policy保留，未借case18清理删除它。暂停恢复以checkpoint-20261111-paused.md为准；没有配置新R布局或播放23～25，全部原目标未完成，不标complete，不继续开发或验收直到用户恢复。

### 2026-11-12 Naoto后三招准备中断与再用

- 本轮前一问答只读核term.grp命名，没有推进实机；恢复后核目标active、develop／b4841dfdb、七WIP和无残留进程，再从已关闭case18私有Save建立唯一case19。游戏57672／creation134389212378677847，launcher51164／exec59749，manifest36d7448d0de0f785abbf6edc8fe2768236a54da62daf9e26d2314fcc9651a69e；仍封存EXE1ac677/PAC8efe，未新构建、不push。
- 正常UI设R末级23/24/25、其它普通/FC/initializer空并保存新073。817B前缀5dd5f85b20799423a548e960c09cd156a20f9ca411f12bce4f0c5c2c491fb8b8；074空输入实际首剑18而非旧33，075～077旧候选未播放。078～080只按各ABC[3,7)，均实际clock15受击→19早释/state7→37恢复；未用坐标/热量/AI内存写制造通过。
- 081～083新增相同按钮[53,57)，均native53真实第二START/USE；二次分别clock27/18/7又被CPU击中，90/81/70转state8，没有自然完整后段，24/25未发弹。六招现在各有准备段中断与有限再用；玩家空中关键阶段/发弹后边界未代填，21既有有界稳定窗口仍按原范围保留。
- 两份新文本证据共314757B；root独立核174条完整native对象、24294表格单元、输入/来源hash、缺样拒读及有界窗口，原生与RPM相位差保留。手册/矩阵及resumed-validation-20261112.md同步，不声明全部54追加、Sou153或原164入口通过。
- 用户询问启用CPU的必要性，已解释仅用于实际受击中断；持续追击不作为耐打要求，也不以全场对象非零判玩家泄漏。下一角色按计划切Katou静止靶，避免重复已证明的Naoto起手。case19仍在完整Practice菜单，root独占UI。旧case17删除仍受前次审批拒绝约束，不换机制重试；所有输出只在仓库，二进制不提交/上传。

### 2026-11-12 Katou六件独立效果与正常收尾

- 前一目标轮为实际进展：3f3e9c161已固定Naoto后三招起手中断/复用；本轮重新核同一case19及游戏/launcher仍live，develop、七WIP保全，未重开会话。正常UI将敌行为改停止，选Katou110、清旧近A并设末级20/21/22，保存084；再设末级23/24/25保存088。两模板110/0/68/seed6=0/14槽及保留前缀均解析确认。
- root实际LOAD/PLAY085/086/087及089/090/099；各仅对应普通ABC[30,34)。六件独立有伤害87/37/37/157/180/100及自然父收尾，24只地面P0；20G0在RPM112正常恢复，21甲/缓存400在202清0，22后撤段实X-128与全招净X-27分别记录。各31行连续正常尾不外推为全部条件完成。
- 25在P1clock9/21实际见两次2/265出现；伤害62/69均早于第二批观察75，不能按两次damage推两批各命中。父119release仍TAMA1，7/366持续至331、332..600全部269行对象/SCRIPT0；没有逐体owner/root字段，未签逐体身份。新两份JSON共298606B，root及独立代理核106原样native/38078单元、来源与输入hash、全缺样拒读及尾窗口。
- 原生训练录像仅001..099，当前七函数代码与原版一致。满槽后只把本case新生成且已验078..083六DAT共5176B移入同case归档，原receipt/input/sampling不改；无内容删除或覆盖，旧证据以原路径+旧hash查archive映射，不以新代次同槽冒充。073/084/088模板、原用户091..098及case17均保留，映射小JSON同步固定。
- 矩阵仅更新Katou实际效果6/6，其连段/落空/取消/中断/复用列不代填。下一步084做20→22及其余独立武装连接；case19保持完整Practice菜单，所有完整目标active，runtimeValidated=false，不push、不提交二进制、不在仓库外写删。

### 2026-11-12 Katou六追加固定靶连接

- 前一目标轮为实际进展：abc13e328固定六单招及安全归档。本轮复核同case19/EXE/PAC、develop和原七WIP；游戏57672及launcher51164仍live，不另复制资源。078..082作为generation2保留旧Naoto归档与hash映射，新的描述性input/receipt/sampling分开，083仍空。
- root正常UI实际LOAD/PLAY五条：20→22在30/63 START、三击120，21→22在30/88 START、两击73，24→23在30/86 START、三击290，24→25在30/86 START、三击249；后继命中前同靶control均为正且前无恢复，四条共同覆盖六追加。23→24虽三击340，但97..126完整30行control0/state8/normal0/Z>0，只认可有限浮空追击；原生确有特定模式的脱离分支，静止靶（action0）未验主动脱出，不改技能凑锁定。
- 普通取消实际成立的源为20/21/23/24；20→22 RPM64已恢复G50、21→22 RPM89甲/缓存归0，各后续保持。只记录对应地面普通窗口，不代填D、空中或受击。25双发窗口118/131，额外damage119/126均早于第二窗口，未捏造两批各中；父175结束仍TAMA1，388..600对象/SCRIPT0，各条有31行连续正常尾。
- 三份文本证据共315921B，root独立核133原样native/31721单元、输入与来源hash/归档旧代次、缺样拒读及物理尾。提交前修正恢复counter/control字段命名混淆；疑似乱码经码点确认只是终端显示，三份磁盘字符串无FFFD/PUA/C1/异常控制。既有证据与原样对象未变；手册/矩阵/katou-combo-runtime-20261112.md同步。
- 仅各播一次，Katou复用/完整落空/SD/受击/更多目标仍待；全部原目标active、runtimeValidated=false，不push、不上传二进制。case19留完整Practice菜单；下一再用候选普通D420..457后留收势至B530/C586，未生成/未播，须实际核位置与门/热，不以源码估算充实测。

### 2026-11-12 Katou六追加同场再用与CPU测试范围

- 前一目标轮92b203410已固定六追加连接。本轮复用同case19和live游戏57672／launcher51164，EXE/PAC仍1ac677／8efe；不重建、不另复制大资源。四组基础试验均正常UI LOAD/PLAY、静止Kou0/action0，078/079/080为generation3，083为generation2；每组内部两轮同靶HP连续，不重载、不补血，也不把四次LOAD拼成一场。
- 20→22两轮各120、21→22各73、24→23各290；24→25首轮249，第二249连接加15晚追伤，同靶HP6000→5751→5487。第二首招USE均0/0，四条共同覆盖六追加的有限同场再用。导弹669..690完整22行control0/state8/normal0/Z>0，晚15单列，累计513不称不断连段。20/21路线普通右移实际接近至整数距离100／103；24/25普通D420..457经自然收势后再起，不作为D技能取消通过。
- 两份repeat小文本共392158B；第一份139081B已有61条native／20168单元独核，合并253077B再核151条native、29张RPM及方向投影共995行／24786单元、38份固定来源hash、三组普通输入与完整101行尾窗。21首轮缺92及各拒读保留，不用第二轮补首轮；runtimeValidated／terminalAcceptance保持false。
- generation2五DAT共4251B只移入同case归档，input/receipt/sampling保留，映射另存小文本。历史证据按原路径＋旧hash＋generation解析两级映射；录像不进Git，073/083/084/088与原091..098受保护，旧case17未触碰、不重试受阻删除。
- 用户再次询问开CPU必要性：已回源确认本轮四组及084/088前缀均enemy action0。基础效果／收招／连接／再用／普通D取消默认静止靶；只有真实受击中断、防反／反射等攻击触发条件才用主动敌，持续追打不作为基础通过门。矩阵明确此方法，手册修正单轮记录与后续再用的时态冲突，报告明确DAT从未提交。
- 本轮只有文档／小型文本证据，不改技能、EXE或架构，不重复构建。Katou完整落空、剩余取消／SD、关键受击、玩家空中及更多目标继续独立验证；其余角色、Sou153、原164候选、重载／关卡及最终说明书等完整目标仍active且未完成。当前仍同case19，root独占UI。

### 2026-11-12 Katou六追加静止靶普通D取消

- 前一中文提交1bb2bf6f5已固定四组同场再用及CPU测试范围，未push。本轮继续同case19／游戏57672／launcher51164／EXE1ac677／PAC8efe，无构建或资源复制；六轮普通输入录像均正常UI LOAD/PLAY到STOP600，静止Kou0/action0，不强写位置、热量、门或敌状态。
- 081/082 gen3：20两Hit后D75、21肘击后D88，均原生normal1/sd0、nextState3提前释；RPM76恢复G51200，RPM89实际甲／缓存400→0，W3→4→5后native148/161回0、RPM149/162确认。20缺49／拒30，21缺5/90／拒22，20的117/600 tick偏移−1保留；两完整101行正常尾通过，非6206E0专用SD或受击验收。
- 为继续22～25，仅将旧085/086/087/089四单招DAT共3353B移入同case归档；精确manifest、源／目的边界与无reparse、Git未跟踪、旧receipt／封存证据hash和独立预检通过。95其它当前Replay与12份旧元数据前后不变；小映射replay-slot-archive-katou-single085-087-089.json保存旧path/hash/gen1，录像不进Git，旧case17不碰。四槽新gen2各自receipt正确指回旧来源。
- 新085/086/087/089分别22/23/24/25：D64/78/86/77在W33c33／W17c8／W35P0c53／W36P1c23真实早释、W3→4→5后自然返回；RESET138/221/221/225单独记录，不与RPM回0的138/152/160/151混同。四轮各601行无缺，拒24/19/22/27及22c38、23c31/32、24c201 tick偏移−1保留；每轮500..600完整101行正常尾。
- 25两批2/265实见62/75，伤害75@62及25@69均早于D77，取消后无新增伤害；父释仍TAMA2/SCRIPT1，100的TAMA3由7/366增到2、2/265仍1，非第三批发射。2/265到100、7/366到330，331..600持续对象/SCRIPT0；分组不是逐体owner/root。六招普通D所测地面窗口成立，专用SD、完整落空、关键受击、空中／更多目标及其他全部角色／Sou153／原164入口／关卡／最终说明书仍待。
- 两份取消JSON共378561B（134145＋244416），root及独立代理回源130条完整native、52张RPM表1562行／49703单元及606行完整稳定尾；各自30／37份固定来源、全部输入重编译、缺样／拒读／相位均核验。第二份SHA7ff1f1a8…22ecd，完整identity及冻结events前缀见katou-dcancel-runtime-20261112.md，不以持续变化state冒充固定来源。
- 本轮只固定文档与小型文本证据，runtimeValidated／terminalAcceptance保持false，原七WIP保全、不push、不上传二进制。游戏留同case19完整Practice菜单，后续仍由root独占实机。

### 2026-11-12 Katou六追加地面远距落空

- 上轮1bb2bf6f5／e4667b929已固定同场再用、静止靶基础测试规则与六招普通D取消，属于实际进展。本轮重核develop和七原WIP、游戏57672／launcher51164仍live、case19 running/extwrite0；同封存EXE1ac677/PAC8efe，不重开或构建。
- 六份旧取消DAT5099B仅移到同case/replay-archive/katou-dcancel081-089-20261112。root精确manifest／边界及无reparse／Git检查和独立归档后审计通过，93其它Replay及18份元数据前后hash不变；新映射28787B/SHA9a8a19ce…79289保留旧path/hash/generation。新081/082 gen4，085/086/087/089 gen3，旧case17不碰、不重试受阻删除。
- 正常UI分别LOAD/PLAY六轮至STOP800。20～24普通left0..200后ABC250..254，25left0..230后C280..284，084/088前缀及action0保留。真实起手250／280，20～24实际距1240.421299、25距1444.011427；Y有偏移，不以速度×帧数充实测。移动G25有原生长时间移动分支支持，201／231释放恢复G50，未采调用栈不归因地图或技能。
- 六轮目标始终6000/state0/control0、无damage/reaction，各关键Hit窗／完整父图无缺。20两击→W15，21完整自动肘／W14／W15，22完整斜向后撤raw(-127488,+29952)，23miss不接W17，24地面完整→W15，25两批及完整三阶段。native自然释331/426/311/321/338/368，RPM332/427/312/322/339/369确认；RESET及冷却另记。20缺5在动作前，拒29/44/43/46/38/38，20counter1/800 tick偏移−1保留，其余无；每轮700..800各101完整正常尾。
- 导弹首轮群计数不能命名逐体寿命。另正常UI重播同089一次，只在只读采样进程临时WATCHED加入2/265与7/366，正式源码／游戏不改；配置和新sample另存target，801无缺／44拒读。实读两弹P0life0..15／25后转P1，分别329／351不再枚举；两爆风分别329／351清direct parent、root始终玩家，life200进P1至229，559／581不再枚举，581..800空场。17/27为样本数，不是life；未观察到时钟加速，PS与障碍具体出口仍未定，快照不冒充析构钩子。
- 两份落空JSON共525567B（227126＋298441），root回源174条原样native、62张RPM表1862行／57493单元，加4张逐体表506行／3542单元，共707行完整正常尾。各自49／45份固定来源、4800对普通输入重编译与历史代次映射均核；完整identity／日志前缀见katou-whiff-runtime-20261112.md。生成约2MB的逐体原始采样留target，配置已嵌入小文本，不重复提交采样或二进制。
- 矩阵与手册只登记六件本次地面远距落空及该独立重播的引用／阶段，不倒填原无watch样本。专用SD、关键受击、空中／更多目标、其它七角色、Sou153、原164候选、重载／关卡和最终交付仍未完成，runtimeValidated／terminalAcceptance保持false。不push、不提交二进制，所有新输出只在仓库。
- 下一步按当前小资源独立解码先测Nagi107追加22「盾打ち・反攻」：仅普通DashA放22，其余空、静止Kou0／map68，正常UI另存模板后A30..34／600f近距；再普通left0..200后A250..254／800f远距。114/W33 P3的Hit4..7与确认8..9、内部114/W30和实际第二伤害分别判，不能静态判不可达，也不把内部上斩计作新武装／热耗；这两候选尚未生成或播放，先固定本轮实测。

### 2026-11-12 Nagi22近远对照与失败基线

- f92c50977已中文固定Katou六件完整地面落空及导弹逐体重播，未push。继续同case19／游戏57672／launcher51164、EXE1ac677/PAC8efe。只归档旧090/099单招及081新whiff三DAT2552B，96其它Replay/9元数据保全；map24519B/SHA1d38acee…6b5b3。旧case17不动。
- 正常UI退出REPLAY后才有完整机体选择；107在当前SelectMekaMenu零基row28=第3行第9列。装备先加普通DashA22再清唯一旧近A，原生不允许所有普通武装均空；右列表选武装后左槽点击落实，右键退出待装备状态。正常UI录制保存090gen2，seed[35598969,107,0,68,0,1,0,38,294]，14槽仅index9=22，790B prefix874c55fb…26bcc。只换普通输入生成099gen2 A30..34/600及081gen5 left0..200 A250..254/800，模板头/两块保留。
- root正常UI分别LOAD/PLAY至STOP。近native30 START22/USE0/0，native50盾打伤300、68内部上斩90，RPM首次HP变化51/69，RPM61进W30/62P1，只有一件22；native104释/RPM105inactive。130落地state6到159，160回0；无玩家伤害/反应，不能当受击。敌恢复229、RESET230、热238归0，500..600101正常尾。
- 远实际起手250，距离1020.907929，完整Hit4..7/确认附近、HP始终6000且无damage/reaction，却RPM276进入W30P0；native300/24结束、RPM301inactive，没有按未命中走W18。这是可复现误派生，未签命中限定通过，未把近390掩盖失败；RESET330/热338/700..800101尾单列。601/801行各无缺，拒27/45；远799/800同tick，仅800偏移−1。
- 新baseline214796B/SHA4da6ff91…f61e固定原样46native、10256行/5862536B日志前缀、18表797行27483单元、31固定来源/1400对普通输入及两个101尾。标记FAILED_BASELINE；overallNagi22Passed/runtimeValidated/terminalAcceptance均false，后续修复不倒填基线。报告、矩阵与手册同步真实边界。
- 根因还未实读确认。当前最强假说为窗后MEKAHIT无当前Hit仍用共享比较cache；实际比较点66B480，slot41 index/clock为actor+C7C/C88，cache位于86CDFC上下文+5C/+60。下一步仅加最小被动调用点观察及少量前序writer，不能无证据改EXE/清缓存制造通过。原版活跃窗确认与WAZAHITCOUNT先例只作方案依据，后者普通起手/释放不自动清，不能直接替换当每次latch。
- 本轮未修改生产源码、架构或游戏包；所有新文件只在仓库，原七WIP保全，不push，不提交二进制。Nagi其余五招与原全部九角色／Sou153／原164入口／关卡／最终手册等目标仍active且未完成。
