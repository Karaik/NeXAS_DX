# 2026-11-10 恢复实机验收

用户已重新启动目标，11/06暂停交接保留为历史。完整十项目标不变，runtimeValidated=false。恢复HEAD ff82058a2，develop，七份旧跟踪WIP保留；不push、不建分支或worktree，所有写删限本仓库，不提交二进制。

## 本次会话

- 旧case13已确认stopped/failure=null/cleanup_pending=false，原游戏与launcher不存在后才新建case14。
- case：target/bhe-playable-runtime/nine-naoto-20261110-14；游戏PID141376，creation FILETIME134387618277972206；launcher session16816／PID143420。后续必须重新核实时进程，不凭本段宣称仍live。
- 仍使用run_20261105_193350_342_bd794139的EXE1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae／PAC8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641，无重建。Naoto21／22／24三处简介仍待下次构建封入。
- 私有seed取已结束case13的docs/GIGA/BALDRSKY_DIVEX/Save，真实Save为只读保护基线。初始菜单→地狱设置→训练→录像载入均实际正常显示，有机体和敌人；不把初始画面当全部武装验收。
- 035继承843字节，SHA1e827650d74bd7998400e1a6564dea213ec146e01cee186a45d6c8e9c5bceec1。UI已定位，采样器审查前停在LOAD确认，尚未播放。
- 036新生成876字节，SHAe13340c182ec39b60b6ca531a49c885abdfcf8dd327ee7aab16cb18431e6c6eb。1250帧，A30/C68/B156、D420、A600/C638/B726，各短按4帧；同028头和前两块798字节／SHA2b3472578d65f1ff3cba085a04e2d3fc446018b355f3557639f0148906226ae9。正常fixture只替换第三输入块，当前未运行；D是两轮之间重新接近，不预记SD取消。

## 验证工具

旧functions.store临时采样器已丢失；新增tools/bhe_runtime_sampler.py，基于已证地址、固定Replay对象、原进程身份及只读RPM。正式代码不写入target。独立测试／review与实机观察结果后续补记；工具complete只表示采样会话到达目标尾部，不代表全武装通过，也不保证每个counter都有样本。

## 后续范围

先长按24与23→25→延后24复用，再补各招落空、搜索取消／专用SD区别及受击中断；其它八角色六招、Sou153当前包覆盖、133剩余条件、资源重载／关卡推进、乱码及手册仍未完成。Sou34只读调查正在纠正LENGTH_Z消费者解释，未经审查不实施全局替换。

## case14 状态文件占用导致的启动器失败

035仍未确认播放。第一次采样输出naoto24-held-sampling.json为process_error／0样本：它确认原进程已按E0000001退出。launcher session16816最终exit1，case14 status=failed，failure=PermissionError13；events明确记录launcher_error→terminating_owned_process→process-terminated，无skill崩溃认定。owned/guard清理完成、cleanup_pending=false，28份5,805,717,300字节运行副本自动清理；原Save/base/source保全通过，外写计数0、guard违例空。不得复用此case/lock/PID。

根在target/sampler-state-lock-probe-20261110独立复现：普通读句柄保持state.json打开时，write_json的temporary.replace(path)报WinError5／errno13；原target仍generation0，关闭句柄后同函数成功写generation2。采样器每5ms读取case状态与launcher每秒原子替换相遇，暴露Windows共享删除权限限制。原launcher日志无栈，独立复现栈指向bhe_runtime_session.py的replace；保留此证据边界，不伪造原栈。将增加只针对Windows5/32/33的有界replace重试，持续失败仍传播；未重启游戏，待回归和复审。

小型失败证据：src/test/resources/bhe-runtime-evidence-20261110/case14-state-sharing-failure.json；包含事件原行、采样失败及独立复现，未保存二进制。

## 工具修复通过离线验证与独立复审

- 新采样器17项测试通过，覆盖错身份/路径/只读权限、固定Replay对象、环表异常/瞬态、输出界限、超时/失败保全。独立review发现的计数先读与快照不同步P2已用两条先RED后GREEN测试修正，spec/quality复审通过。
- write_json只写一次临时文件，对Windows错误5/32/33的replace操作最多重试40次，间隔不超过25ms且总期限约1秒。永久失败仍抛原异常并保留原target/临时文件；其它错误立即失败。真实Windows读句柄夹具复现旧WinError5，补4项回归后完整session测试70项通过，独立spec/quality复审通过。未改变游戏资源或引擎异常处理。
- 启动器与采样器正式代码均位于tools；此批不构建游戏包。下一实机需新case15、独立身份及输出，不复用已失败case14。70/17离线通过不作为实际技能通过。
- Sou34条件消费者解释已纠正，native供体与局部近似候选固定在sou-nightmare-motion-options-20261110.md；生产实现尚未开始，先证明首击触发再推进。
