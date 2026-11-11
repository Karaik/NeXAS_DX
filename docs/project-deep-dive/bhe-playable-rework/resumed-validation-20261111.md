# 2026-11-11 继续验收：Naoto准备段中断与再用

完整目标尚未完成，封存包仍为EXE `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`／PAC `8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641`，runtimeValidated=false。本文只固定当日实际执行结果，不把输入准备或源码预计登记为运行通过。

## 会话恢复与副本边界

case17原游戏48692／launcher52608已在独立进程查询中消失，exec session10026也已不存在；其state.json却残留running，最后日志没有终态。因此退出原因和退出码未知，不称正常exit0或游戏自身崩溃。旧状态与日志保留。

对case17/game的28个可重建副本已完成路径／无链接、Git跟踪为0、清单身份、目标及原源SHA256检查；总量5,805,717,300字节，清单外文件0，独立核查一致。但执行删除的命令在CreateProcess阶段被自动审批拒绝，返回仅为`blocked by policy`，命令未执行、删除0字节。预检和受阻记录见`src/test/resources/bhe-runtime-evidence-20261111/case17-orphan-preflight.json`及`case17-orphan-cleanup-status.json`，不能把它们写成已清理。

为继续实机，从case17私有Save准备唯一新case18；没有复用旧lock或进程。游戏PID71360／creation FILETIME134388569299088782，launcher68672／exec session49818；manifest SHA256 `b15f5a9b71f189b54889efecf646edc465ccd7e152b9909acd2cc630b2f10c43`。新桌面控制入口已实际读取并操作本游戏窗口，root正常进入标题→地狱→Practice→Replay。先前旧控制管道错误不再作为新会话已运行的替代证据。当前旧副本暂留，额外临时占用约5.41GiB；未上传二进制。

## 067／068：两件准备段真实受击

两件均保留正常UI模板062的完整813字节前缀（SHA256 `18e98e9cfcb86251a2ed794c21add1d6c42336abbfdcecb566e46e4bf4e20a57`）、玩家109／Kou0／map68／CPU3，以及M布局末级20／21／22、其余普通槽空。067仅B[23,27)，068仅C[17,21)，均360帧。case18录像与case17源逐字节相同；原input／receipt仍准确引用case17路径，不伪称复制到新case根目录。

| 录像 | 本轮真实启动和受击 | 中断与首次恢复 | 采样限制 |
| --- | --- | --- | --- |
| 067／21 | native frame23 START/USE；context33在W32 clock10受67伤、control16/pending1 | context34提前释放并请求state7；RPM53恢复控制，59热归零；RESET为context82 | 361行无缺，16拒读；6行tick相位差保留 |
| 068／22 | native frame17 START/USE；context33在W34 P0 clock16受69伤、control16/pending1 | context34提前释放并请求state7；RPM53恢复控制，60热归零；RESET66，RPM67短heatState3、68回0 | 358行，缺19/84/353，26拒读；不补W34 clock1 |

067起手前冲实际仅36，玩家X2302→2338，敌X2402，未穿位；首剑实际命中。不能用远靶057的前进量预填这一轮，也不能把B在27松开当作34受击释放的原因。21没有走到自身Hit34或自然70结束。068没有进入私有Rise/W33或W18收纳；这是准备段中断，不要求被中断后继续执行后段，也未覆盖玩家空中受击。

两轮都是先START、来袭时仍active、原生damage/reaction、提前离开原图；没有D或另一攻击输入。只有恢复控制，不代表整轮稳定：067尾360仍state7/control6，068尾360仍state8/control91/Z7/TAMA1。STOP后的CPU继续攻击只带旧lastReplayFrame，已按本轮实际tick窗口排除。

## 069／070：普通远离后的单次再用对照

069保留B23，新增left[60,180)、松20、B[200,204)，共720帧。60来自067本轮59/60连续可控、普通门1、热量/heatState/hitstop0的实际观察；方向依据双方当时XY选择远离。它是待试输入，不保证能够脱离更快的敌人。

069首中断仍成立，context33在W32 clock10受67伤、34释放。left60确实使玩家移动，但Kou追击更快，context62又受35伤；自己的RESET推迟到context83，且当时仍state7/control25。B200确有原生输入，但玩家state8、普通门0、heatState3，全窗仅首次21 START/USE，不能记作复用。720接受行，缺204，37拒读；后续持续受击的counter543～564实读G25600，不能写全程G50或归因21泄漏。尾720仍state8/control60/Z103、TAMA3/SCRIPT5。

070保留C17，新增left[61,181)、松20、C[201,205)，共900帧。61来自068实际60/61连续正常；本轮首中断仍在W34 clock16，恢复53、热60归零，但自己的RESET实际为context67，68短heatState3、69回0，未继承旧轮固定时刻。移动将距离拉至counter181约624.4；native201真实第二START/USE（seq5，usedBefore/chainBefore0），证明此条件下22能在中断后再用。

070第二次完整W34 clock0～67后进入W33，两个Hit窗18～22／23～33均有完整样本，敌HP仍6000。玩家各接受样本仍地面，不把Rise名称当空中。W33 clock23恢复fall100已观测；随后在clock59遭小伤及最终33伤/control30/pending1，native334硬击后因停止持续到340才释放并转state7，六帧差不能抹平。没有进入W18，因此第二次完整自然收纳仍未通过。移动期间counter175～181有G25600、182回51200，第二W34/W33阶段均G51200，分段保留。

070共896接受行，缺756/810/811/812/892，58拒读；尾900仍state8/control50/Z127、TAMA17/SCRIPT8。它证明实际复用，不证明摆脱后续CPU或整场对象清零。四份小型JSON证据位于`src/test/resources/bhe-runtime-evidence-20261111/`，分别为`naoto21-kou-startup.json`、`naoto22-kou-startup.json`、`naoto21-kou-escape-reuse.json`、`naoto22-kou-escape-reuse.json`。root逐项核原样native对象、关键RPM表、完整缺样/拒读及来源hash，独立审计结论一致。

## 071／072：侧移后钩拳真实再用及有界稳定窗口

071只保留首B[23,27)，再加up[53,86)，360帧。依据是实际Kou W86/Short Elbow Lv3在clock0～6准备追角，随后保留旧方向，10～17高速突进、13～18攻击；没有修改敌AI或招式。首中断仍在native33/W32clock10。up53实际进入角色输入，RPM54才读到state2及Y1801；本轮RPM53仍为重复前一tick的state7，不能套用旧轮“53已state0”。

071敌clock6后方向96523保持，整个Hit13～18对应RPM63～68接受行均在，玩家HP1933、control/pending0，没有新增damage/reaction；只认可避开这一次肘击。自己的RESET在native68，事件热800，同tickRPM68仍900，保留相位差。热量与heatState首次同时归0是RPM77，80仍为state2/normalGate1/control-pending-hitstop0，支持下一次有依据的按键；后续CPU在145仍造成56伤害，侧移不是无敌。共360接受行，缺122、21拒读，整轮尾仍受击。

072保持上述全部输入，只增加B[80,84)，并把后续空输入延长到720帧。native80/tick400235真实第二START/USE；接受counter81～151覆盖W32 clock0～70，Hit34～36仍落空，敌HP6000。clock40～47实际每步raw位移(-3876,-11652)、速度-12288，合计量化平面回撤约95.9356，不写成沿X退96；clock48停速开门。native151/tick400306在clock70自然释放，152已inactive，随后RESET181仍热2200，188才热量与heatState归0。

072在counter188～210／tick400343～400365有连续23个接受样本：玩家state0/active-1、control/pending0、normalGate1、hitstop0、Z/Vz0、G51200、fall/jump100、热与heatState0、TAMA/SCRIPT0、HP1933。可认可这段中断后实际复用及稳定收尾窗口。211起新SCRIPT，213再受35伤，不能将之后新的CPU攻击追溯成此前收尾失败，也不能把整个720帧末端写成净空。072共718接受行，缺436/631/678，41拒读。

两轮合并小证据`naoto21-kou-side-step-reuse.json`为97680字节，SHA256 `ea0f6ce2091b877ec9c1a6709d57a79728664c15c488311abc85bc18f058f84b`；root与独立审计逐项核原样事件、关键采样、缺失/拒读及实际稳定窗口。069同轴后退失败独立保留，不将其改写成成功。

## 用户暂停与会话关闭

用户在072完成后设目标paused。本轮只固定已完成证据并通过case18的STOP文件请求受管终止。case18现status=stopped、stop_reason=STOP_file、failure=null，子进程退出码3758096385/0xE0000001为主动终止；外层exec session49818返回1，原样保留，不能称游戏自然exit0。游戏/launcher进程和游戏窗口均已确认不存在，owned_process_exit_confirmed/guard_cleanup_complete=true、cleanup_pending=false。

受管程序自动回收case18的28份未变可重建输入，共5,805,717,300字节，无保留变更项或清理错误；Save、录像、日志与清单保留，原Save/base/source检查未变。7条未分类system异常和nvAppTimestamps外部漂移归因未知分别保留。旧case17人工清理仍被审批拦截，其28份副本未被这次case18收尾删除。小终态记录为`naoto-case18-pause-closure.json`，恢复以[本日暂停交接](checkpoint-20261111-paused.md)为准。

## 下一步与完整范围

Naoto20～22准备段中断及各自有限条件再用已证，21另有上述稳定收尾窗口；其它阶段与玩家空中受击未完成，23～25仍须自己的中断条件。方向+D虽有正常入口，但Naoto实际Boost/W6有9帧无主动移动起手，未盲试54逃离。侧移候选已按W86机制执行并取得有限结果，恢复后继续后三件及其它角色，不再重做已固定的071/072。

其余八角色、Sou153、原164候选菜单/实际使用、重载/关卡推进、最终说明与封包验收仍独立保留。源码编码闭合与局部实机成果不能替代这些要求，全部目标保持active。
