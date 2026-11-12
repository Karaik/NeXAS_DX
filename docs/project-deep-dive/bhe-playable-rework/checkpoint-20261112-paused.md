# 2026-11-12 用户暂停交接

用户已将目标设为paused。本轮暂停前仅核对既有提交、文档、观察器和函数ABI；没有新增条件探针源码、测试代码、Frida附加或技能改动，没有构建／实机重播。已中断正在运行的只读代理，不继续目标工作。

## 已固定成果与未修复问题

- develop HEAD为15b18094b「固定Nagi盾打命中与空挥误派生失败基线」，上一提交f92c50977固定Katou六招地面远距落空及导弹逐体回收。均未push。原七个无关WIP继续保留。
- Nagi22近距盾打300＋内部上斩90，但远距无damage、HP6000仍进入W30。FAILED_BASELINE已保存，整体22／runtimeValidated／terminalAcceptance均false，根因未完成调用点实读，尚未修复。
- 基线及输入身份见nagi22-ground-baseline-20261112.md和nagi22-ground-hit-miss-baseline.json。不要覆盖基线或把假说当成已经修好的结果。

## case19已经退出

暂停后的实际查询确认：游戏PID57672和launcher51164均不存在，未发现BaldrSky.exe进程。最终state为exited／exit_code0／failure=null，owned退出已确认、guard_cleanup_complete=true、cleanup_pending=false、frida已detach、安装线程已结束。不把关闭原因归为STOP文件，state.stop_reason为null。

受管程序自动回收28份仍与原输入一致的game副本，共5805717300B；retained_changed_or_unique及errors均空。Save、Replay、归档、原始采样和日志保留。原Save、原base及封存EXE/PAC检查未变。external_write_count0、guard_violations空；全会话79条未分类异常和nvAppTimestamps漂移保留，退出码0不等于零异常。

完整终态保存为小文本src/test/resources/bhe-runtime-evidence-20261112/case19-terminal-paused.json。旧case17的受阻删除不重试，也不借本次自动清理处理它。

## 用户重新启动目标后的入口

case19已经是终态且game输入副本已回收，不能沿旧PID或仅凭旧running记录继续。恢复时先核目标状态、develop／WIP和无残留进程，再按现有harness从同一封存包与case19私有Save建立新case；不复用case名、不复制资源到仓库外。

Nagi正常UI模板为旧case19的Replay090 gen2，seed[35598969,107,0,68,0,1,0,38,294]，14槽仅index9=22；790B前缀874c55fbd72bd2e19e72b59382aee3287f526c33382ae326912d1070c2e26bcc。099gen2近距A30..34/600f，081gen5远距left0..200+A250..254/800f。新case复制后仍先按对应hash／代次核，不把同槽当同内容。

下一步仅计划被动调用点观察66B0F0／66B480及少量66EA50前史。三个函数为x86 thiscall，入口ECX为求值器、原ESP+4为显式参数、EAX返回／ret4；返回时保存入口this，不借已被覆盖ECX。66EA50入口+54/+58可能仍属前条条件，本次term／owner取+4C/+50；返回值尚未经6685D0的NOT处理。必须对新case实际EXE校验签名后才挂钩，不主动调用游戏函数或写寄存器／cache制造通过。探针若实现，源码放tools，数据才放target，并先做离线测试。

完整九角色／Sou153／原164候选／关卡／日文说明／中文手册等原目标不缩减；等待用户恢复后继续。
