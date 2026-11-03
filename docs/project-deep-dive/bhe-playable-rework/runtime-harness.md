# 九角色隔离运行工具

入口为`tools/bhe_runtime_session.py`，负责输入和原存档保全、进程生命周期、文件隔离及文本观测。不发送UI输入或截图，不修改游戏EXE/PAC，不处理或吞掉原生异常；UI操作使用`@oai/sky`。

2026-10-31源码更新了仓库内输出和副本清理规则。2026-11-03用户已明确允许仓库内构建、测试和实机验收；实际执行范围及结果见`resumed-validation-20261103.md`，以下入口说明本身不代表运行通过。

## 准备与输出范围

输出固定为本仓库`target/bhe-playable-runtime/<case>`，不接受C盘或其它runtime_root。case是直系子目录，名称只允许ASCII字母、数字、下划线、连字符。拒绝已有目录、symlink/junction、输入输出重叠及Windows设备名；路径验证先于mkdir和复制。

```powershell
C:\Python313\python.exe -B tools\bhe_runtime_session.py prepare --case checked-01 --base src\main\resources\bsdx_game --exe <本轮EXE路径> --pac <本轮Update3.pac路径> --save-dir C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save
C:\Python313\python.exe -B tools\bhe_runtime_session.py launch --case checked-01
```

`--pac`与`--vanilla`二选一，PAC只允许明确选择的Update3–7；未选择的旧overlay阻断准备。原版对照明确使用只读`ida-reverse/BaldrSky.exe`，基底目录的旧宽屏EXE不作为参考。

准备把基底和本轮EXE/PAC复制到新game。真实`--save-dir`始终只读并受写保护；默认复制到私有`docs/GIGA/BALDRSKY_DIVEX/Save`。`--fresh-save`与`--save-seed-dir`互斥，分别建立空私有Save或复制明确独立seed。显式`--app-manifest`只复制用户提供文件，不生成locale设置。输入、种子、原Save及副本独立记录长度和SHA256，不回写原件。

## 临时目录与使用锁

launch验证case后，在导入Frida前把TEMP/TMP/TMPDIR及Python临时根设置到case/tmp，子进程继承。Python bytecode写入关闭；退出后恢复父进程原环境，不修改系统环境设置。

受管build有`.bhe-playable-owned.json`和既有一字节`.artifact-use.lock`。prepare首次读取受管输入前取共享锁，至复制核验完成；launch重新取共享锁，至退出、审计和副本清理完成。只打开仓库内既有锁，不在外部只读输入旁创建文件。

prepare与launch是两个CLI进程，共享锁不会跨进程保留。两者间由Java构建保留器扫描固定runtime根下prepared/未结束case的manifest及state，保护其EXE/PAC、base和seed等来源。游戏已经复制不等于来源已无消费者。

## 进程与文件隔离

以CreateProcessW的挂起模式创建自有进程，保留句柄并核PID、创建FILETIME和映像路径。只针对该PID安装现有隔离，hook_ready后才恢复。超时/错误时通过原句柄终止并确认退出，不按名称查杀其它进程；原生CRT和异常处理链保持原样。

Documents重定向只作用于已验证sub_764FA0调用，返回RVA为0x364FBF/0x364FD3，注入前核指令。文件后备alias只覆盖原Save的准确目录边界，保留DOS/NT与RootDirectory相对路径处理，不改系统Documents或注册表。

NVIDIA Drs默认复制并alias到case/driver-cache/Drs，复制前后核原树与副本。`--native-driver-state`只保留为显式报错参数，不能再准备或启动不隔离全局驱动状态的case；旧native记录属于历史版本。

目标进程磁盘写入只允许本case；原Save、原输入、仓库其它路径及普通外部路径均拒绝。NtCreate/NtOpen的内联检查与NtWrite/NtDelete/metadata修改一致。外部请求记录为blocked，不能记作已允许。既有目录只读metadata、准确HID/MMCSS设备及IPC例外不是普通磁盘写白名单。

守卫约束目标进程的文件请求，不把驱动服务或操作系统自身行为声称为已由这些hook全面拦截。新策略的运行兼容性仍需实测。

## 退出与输入副本清理

case只能启动一次，launch.lock不删。Ctrl+C、timeout或本轮STOP只终止自有进程。guard保留至进程确认退出、installer结束及Frida释放。原Save审计与基底、源输入、驱动审计分别执行并保留错误，不能因一项失败跳过其它审计。

只有当前launcher确认owned_process_exit_confirmed、guard_cleanup_complete均true且cleanup_pending为false后，才处理game输入副本。原始相对名称先拒绝空组件、dot/dotdot、冒号/ADS、drive-relative及绝对路径；再拒重解析并检查case/game边界。当前副本和仍存在的原输入都必须匹配manifest长度及SHA，删除前再次核验。

只移除这些仍可重建的副本。改变/新增文件、原件缺失的唯一副本、私有Save及文本证据保留；清理错误明确写state并失败。结束目录可能不再含完整可运行game，不能删锁复用。不递归清原输入、未知case，也不清理C盘旧目录。

## 验证边界

离线测试源码为`tools/test_bhe_runtime_session.py`，fixture临时根同样在仓库target内，覆盖输入保护、原Save、进程归属、晚installer、日志失败、路径、使用锁、临时目录、副本清理及外部写入拒绝。它们不等于实机验收。

本轮只作AST、源码及格式静态检查，未运行测试、构建或游戏。旧46项离线通过及后续菜单记录仅对应历史代码，不能转记为新政策已通过。Sou153项和追加54武装的命中/落空、跨武装连段、取消、中断及关卡推进仍需最终新包逐项验收。
