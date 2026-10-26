# BHE九角色重做实施账本

Plan: docs/superpowers/plans/2026-10-26-bhe-playable-rework.md

目标尚未完成；最终必须满足游戏不崩、实际效果、说明书、乱码恢复、保留架构、中文提交/no push和谨慎删除全部要求。当前阻塞与恢复点以文末审计为准。

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
