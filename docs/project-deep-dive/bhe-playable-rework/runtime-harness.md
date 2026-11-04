# 九角色隔离运行工具

入口为`tools/bhe_runtime_session.py`，负责输入和原存档保全、进程生命周期、文件隔离及文本观测。不发送UI输入或截图，不修改游戏EXE/PAC，不处理或吞掉原生异常；UI操作使用`@oai/sky`。

2026-10-31源码更新了仓库内输出和副本清理规则。2026-11-03用户已明确允许仓库内构建、测试和实机验收；实际执行范围及结果见`resumed-validation-20261103.md`，以下入口说明本身不代表运行通过。

## 准备与输出范围

输出固定为本仓库`target/bhe-playable-runtime/<case>`，不接受C盘或其它runtime_root。case是直系子目录，名称只允许ASCII字母、数字、下划线、连字符。拒绝已有目录、symlink/junction、输入输出重叠及Windows设备名；路径验证先于mkdir和复制。

```powershell
C:\Python313\python.exe -B tools\bhe_runtime_session.py prepare --case checked-01 --base src\main\resources\bsdx_game --exe <本轮EXE路径> --pac <本轮Update3.pac路径> --save-dir C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save
C:\Python313\python.exe -B tools\bhe_runtime_session.py launch --case checked-01
```

需要被动出招观测时，在launch后加`--observe-gameplay`。该模式只接受已验证的`1ac677...`完整EXE身份，创建进程前生成并校验观察脚本，四个挂点内存签名全部符合后安装。菜单REQUEST、真实START与原生READY严格按同一请求关联；对象未清、身份变化、读取失败、越界或新请求替代旧请求都不能补记通过。只读寄存器/内存并记录离散事件，不调用出招、不改动作、返回值或异常处理。

同时加`--observe-combat`可记录七处已有原生战斗/录像观测点；它要求`--observe-gameplay`，默认仍只有四挂点。记录真实扣血、反应应用、恢复入口、状态切换、武装消耗与清链，不能把RESET当目标恢复。录像消费者的active字节`+159D`由固定EXE指令确认，inactive不读取输入。普通事件的`lastReplayFrame/lastReplayConsumer/replayReadTick`仅为最后读取快照；只有录像输入事件本身携带`frame`，避免把停止后的旧帧拼入新预览。普通/SD门与SD预算只记原始字段，不据单个标志判定取消可用或连段通过。

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

原Drs属于系统可变状态，prepare后可能被其它进程更新。launch继续严格校验私有副本，但原树漂移仅作只读审计，不重写准备清单或替换私有副本。创建进程前记录准备基线、当时原树及差异；退出分别记录准备→退出、启动前→退出两段差异，归因为未知且可能有外部写入者。真实Save、独立种子、游戏输入、源EXE/PAC的变化仍阻断启动。

2026-11-03实机启动观察到`C:/ProgramData/NVIDIA Corporation/ShadowPlay/CaptureCore.log`写入请求。仅此完整文件名精确alias到case内新建的空日志；不读取或复制原C日志，不放行ShadowPlay目录、同名前后缀/ADS、兄弟文件或其它NVIDIA路径。prepare记录精确映射，launch前核私有日志仍为空且映射未变；所有实际写入继续经过case守卫。旧case没有此准备信息时拒绝重用，须建立新case。

2026-11-04又观察到当前用户LocalLow下`Microsoft/CryptnetUrlCache/MetaData/329B6147266C1E26CD774EA22B79EC2E`缓存写入。仅该完整MetaData文件alias到case内同名私有路径；不读取或复制原缓存。prepare只建私有MetaData/Content空目录，preflight核映射、空树和无重解析点。Content文件、其它32位名称、ADS、路径前后缀/层级伪装和其它Microsoft目录仍拒绝外部写入；原缓存root列为受保护输入，不通过目录白名单绕过。

目标进程磁盘写入只允许本case；原Save、原输入、仓库其它路径及普通外部路径均拒绝。NtCreate/NtOpen的内联检查与NtWrite/NtDelete/metadata修改一致。外部请求记录为blocked，不能记作已允许。既有目录只读metadata、准确HID/MMCSS设备及IPC例外不是普通磁盘写白名单。

守卫约束目标进程的文件请求，不把驱动服务或操作系统自身行为声称为已由这些hook全面拦截。新策略的运行兼容性仍需实测。

## 退出与输入副本清理

case只能启动一次，launch.lock不删。Ctrl+C、timeout或本轮STOP只终止自有进程。guard保留至进程确认退出、installer结束及Frida释放。原Save审计与基底、源输入、驱动审计分别执行并保留错误，不能因一项失败跳过其它审计。

只有当前launcher确认owned_process_exit_confirmed、guard_cleanup_complete均true且cleanup_pending为false后，才处理game输入副本。原始相对名称先拒绝空组件、dot/dotdot、冒号/ADS、drive-relative及绝对路径；再拒重解析并检查case/game边界。当前副本和仍存在的原输入都必须匹配manifest长度及SHA，删除前再次核验。

只移除这些仍可重建的副本。改变/新增文件、原件缺失的唯一副本、私有Save及文本证据保留；清理错误明确写state并失败。结束目录可能不再含完整可运行game，不能删锁复用。不递归清原输入、未知case，也不清理C盘旧目录。

## 验证边界

离线测试源码为`tools/test_bhe_runtime_session.py`，fixture临时根同样在仓库target内，覆盖输入保护、原Save、进程归属、晚installer、日志失败、路径、使用锁、临时目录、副本清理及外部写入拒绝。它们不等于实机验收。

2026-10-31阶段只作AST、源码及格式静态检查。2026-11-04当前`test_bhe_*.py`联合回归为112项通过，包含harness、被动observer及私有种子工具；旧结果不转记为当前通过。新包已整链构建并核对身份，实际运行范围见`resumed-validation-20261103.md`。Sou153项和追加54武装的命中/落空、跨武装连段、取消、中断及关卡推进仍需最终新包逐项验收。
