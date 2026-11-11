# 2026-11-11 暂停交接

用户已将目标设为paused，必须等用户恢复后继续。完整十项目标保持，runtimeValidated=false，不把Naoto局部结果当九角色54追加、Sou153或原164候选全通过。

## 已固定成果

- 工作分支develop，不建分支/worktree，不push，中文本地提交。暂停前HEAD为2fdf496f2，已固定067～070真实中断、成功再用与失败后退，以及旧case17清理受阻记录。
- 随后071/072已实际运行。21首B23在W32clock10被剑击打断，up53..86避开下一次W86肘击；072的B80真实再用，W32 clock0..70自然结束。counter188～210／tick400343～400365连续23个接受样本地面、物理/热状态正常、TAMA/SCRIPT0；之后新的CPU攻击另记，不认整720尾净空。
- 小证据为src/test/resources/bhe-runtime-evidence-20261111/naoto21-kou-side-step-reuse.json，97,680字节，SHA256 ea0f6ce2091b877ec9c1a6709d57a79728664c15c488311abc85bc18f058f84b。071缺122/21拒读，072缺436/631/678/41拒读均保留。root及独立审计已核原样对象、关键表、缺失与hash。
- 当前Naoto20～22准备段真实中断及有限条件再用已证；21另有上述稳定窗口，22第二次Rise后段再次受击未收纳。其它阶段/空中及23～25中断未完成。不得重做已固定071/072来刷通过。

## case18已关闭

case为target/bhe-playable-runtime/nine-naoto-20261111-18；原游戏71360／creation134388569299088782，launcher68672／exec49818。已通过该case的STOP文件请求受管关闭，state记录stopped/STOP_file/failure=null，子退出码3758096385/0xE0000001，外层exec返回1；这是主动终止记录，不称自然exit0。

owned_process_exit_confirmed和guard_cleanup_complete均true，cleanup_pending=false；最终进程查询和桌面窗口列表均无该游戏/launcher。28份未变可重建输入共5,805,717,300字节已由受管程序清理，Save、Replay、events、manifest、state保留。原Save/base/source检查未变；7条未分类system异常、nvAppTimestamps外部漂移归因未知如实保留。终态小证据为naoto-case18-pause-closure.json。

旧case17的进程与exec早已消失，但state残留running，退出原因/退出码未知。其28份5.41GiB输入经完整预检可重建，实际人工删除命令却被自动审批CreateProcess阶段以blocked by policy拒绝，0字节删除。case17-orphan-preflight.json和case17-orphan-cleanup-status.json固定此事；旧副本仍保留，不篡改它的状态或借新case清理绕过。不要再堆积多个新会话。

## 恢复入口

1. 先核Git、用户WIP、目标已active及无残留任务进程。原Clarias六文件53行功能WIP、Sou旧笔记和其它未跟踪资源仍保留，禁止git add .。
2. 封存构建仍target/bhe-playable-builds/run_20261105_193350_342_bd794139，EXE1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae／PAC8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641，未新建游戏包。Naoto三条日文说明源码比该包新，下次正式构建须纳入。
3. 如需继续实机，从已结束case18的私有Save准备一个全新case，不能复用18的lock/PID或直接运行其已清理game目录。原Save只读保护；所有工作/临时/依赖缓存限本仓库，不上传二进制。
4. 当前可用桌面入口为cua_repl，Windows先listWindows再以getApp({windowId})选择真实返回窗口；旧@oai/sky管道曾不可用。旧窗口2884756已不存在，不能复用或猜句柄。root独占UI，代理只做具体只读审查。
5. 正常训练持久布局仍为M（末级20/21/22、其余普通槽空），未配置R、未生成新R主动敌模板。下一步正常UI设置R（23/24/25）并保存新模板，再先做本模板的CPU来袭基线，不伪造seed/actor。已存在062和067～072需先核文件身份；下个空位候选073必须实际检查空闲。
6. 继续23～25真实中断/恢复，再补玩家空中关键段；其它八角六招、Sou153预览/装备/使用、原164候选实际菜单范围、重载/关卡、Nagi防反/反射核心、Sou34机制及最终手册/封包全部仍在范围。Nagi22共享比较缓存/生命周期结论见nagi22-hit-confirm-lifecycle-20261111.md，不据静态风险盲改。

正式源码/工具仍在src/tools，文档docs，固定小文本src/test/resources；target只放生成数据。UTF-8无BOM/CRLF。用户恢复之前只保留本交接，不继续工作。
