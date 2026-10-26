# BHE九角色重做实施账本

Plan: docs/superpowers/plans/2026-10-26-bhe-playable-rework.md

目标处于active；最终必须满足游戏不崩、实际效果、说明书、乱码恢复、保留架构、中文提交/no push和谨慎删除全部要求。

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
