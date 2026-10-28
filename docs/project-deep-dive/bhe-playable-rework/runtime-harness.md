# 九角色隔离运行工具

入口为 `tools/bhe_runtime_session.py`，负责正常游戏验证中的输入/存档保全、进程生命周期和观测；UI 使用 computer-use 的 `@oai/sky`，本工具不发送输入或截图。`rework-08` 已记录 Drs alias 成功41次、Save alias成功6次，但普通 ShadowPlay日志追加被旧全盘白名单误拦。当前采用保全模式，普通仓库外 OS 缓存/日志写入正常执行；实际游戏效果仍需新 case 验证。

## 准备与启动

在仓库根目录运行，明确指定本轮 EXE、模式和原存档。`--pac` 与 `--vanilla` 二选一且必选；`--pac` 可以重复，文件名必须是 `Update3.pac` 至 `Update7.pac`。`--vanilla` 不添加额外 PAC，manifest 记录 vanilla=true/pac_sources=[]。两种模式都拒绝基底中的未选择 Update3–7，原版对照应明确把 --exe 指向原版 BaldrSky.exe；Save、Drs、CRT、源哈希及全部保全流程相同。

```powershell
C:\Python313\python.exe -B tools\bhe_runtime_session.py prepare --case tsukuyomi-01 --base src\main\resources\bsdx_game --exe <本轮EXE绝对路径> --pac <本轮Update3.pac绝对路径> --save-dir C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save
C:\Python313\python.exe -B tools\bhe_runtime_session.py launch --case tsukuyomi-01
# 同环境纯原版对照，不启动游戏：
C:\Python313\python.exe -B tools\bhe_runtime_session.py prepare --case vanilla-01 --base src\main\resources\bsdx_game --exe src\main\resources\ida-reverse\BaldrSky.exe --vanilla --save-dir C:\Users\30250\Documents\GIGA\BALDRSKY_DIVEX\Save
```

可选 `--fresh-save` 仅创建空的私有 Save；与 `--save-seed-dir PATH` 互斥。后者复制明确指定的独立完整 seed（例如已修复标题但保留进度的副本），不修改真实原 Save。`--save-dir` 在所有模式下都必须指向真实原 Save，负责原树哈希、拒写保护及 exact alias/CSIDL 来源。默认仍复制原 Save。receipt 用 `seed_mode=original|fresh|provided`、`seed_source`、`fresh_save`、`original_save_files` 和 `save_seed_files` 区分来源；launch 前核独立 seed 源树及私有副本，原 Save 仍独立核验。seed 拒绝链接及与当前 case 重叠，独立兄弟目录可作只读源。

可选 `--app-manifest PATH` 仅复制用户明确提供的文件至 `game/BaldrSky.exe.manifest`，记录 `app_manifest_source`、源 SHA256及目标 game_files；launch 前检查源和副本，运行后检查目标哈希。源文件也加入明确写保护。工具不会生成、下载或解释为生产 locale 设置。该选项可与 vanilla/PAC 及三种 seed 模式组合，用于显式因果对照；没有删除任何原存档或旧 case。

输出严格位于 `target/bhe-playable-runtime/<case>`。case 仅允许 ASCII 字母、数字、下划线、连字符；拒绝现有目录、链接/目录联接及输入与输出重叠。任何准备失败均保留现场，不删除旧文件、不改源文件。prepare 将全部基底文件复制至 `game`，只在新副本中覆盖选定 EXE/PAC；存档种子复制至 `docs/GIGA/BALDRSKY_DIVEX/Save`。复制前后核对源目录与输出文件 SHA256。

针对 `rework-07` 已观察到的 NVIDIA selector/timestamp 状态关联，prepare 把 `%ProgramData%/NVIDIA Corporation/Drs` 整树复制至本轮 `driver-cache/Drs`，记录 `driver_state` 的逐文件 SHA256/size、文件数和总字节。复制前、复制后原树及副本三份 manifest 与差异保存在 state 的 `driver_state_copy_check`；复制过程中源漂移或副本不一致即拒绝并保留现场。launch 再核两侧完整文件 manifest。旧 case 仅有单文件 seed，须 prepare 新 case，不补写旧现场或删锁复用。

显式诊断开关 `prepare --native-driver-state` 用于只改变驱动状态访问位置的对照：不复制 Drs、不设置 Drs alias，也不把全局 Drs 加入输入拒写规则，正常驱动 OS I/O 按普通仓库外路径执行。默认仍为私有 Drs；新 manifest 以 `driver_state.mode=private|native` 记录，旧 manifest 缺少 mode 时按 private 处理。native 的 `relative_path=null`，其 `files` 仍保留准备时的原树完整清单；准备前后审计保存在 `driver_state_prepare_audit`，普通源漂移不阻断 native 模式的后续运行。

native launch 在创建游戏进程前重新记录完整 `original_driver_state_before`，退出后记录完整 `original_driver_state_after` 及 `original_driver_state_check`。差异或退出审计读取错误仅报告，归因为 `not_determined_external_writers_possible`，不把共享状态变化判为游戏故障；没有 `driver_state_copy_after`。此开关与 vanilla/PAC、fresh/provided/original Save、显式应用 manifest 独立组合，Save/base/源 EXE/PAC/seed/应用 manifest 和仓库的保护保持原规则。它不改全局 NVIDIA profile，不删除缓存，不修改系统图形设置。

launch 永久保留 `launch.lock`，同一 case 只能启动一次。需重试时创建新 case，不能删锁复用。默认等待游戏退出；`--timeout 300` 为 300 秒上限。Ctrl+C 或在本轮目录创建 `STOP` 文件会停止本工具拥有的游戏进程。停止仅通过原始进程句柄，并再次核对 PID、创建 FILETIME 和映像路径；不按进程名称查杀，不结束其它窗口进程。

## 隔离与失败规则

`CreateProcessW(CREATE_SUSPENDED | CREATE_NEW_PROCESS_GROUP)` 创建进程后，工具保留主线程/进程句柄并记录身份。Frida attach 只针对该 PID；全部 hook 安装并确认 `hook_ready` 后才恢复主线程。默认 hook 准备期限 30 秒，可用 `--ready-timeout` 调整至不超过 60 秒。超时或 hook 错误时结束仍挂起的自有进程，不以先恢复后注入兜底。

目录重定向只针对已验证的 `sub_764FA0` 调用：`SHGetSpecialFolderLocation(CSIDL_PERSONAL)` 返回 RVA `0x364FBF`，外层 `SHGetPathFromIDListA` 返回 RVA `0x364FD3`。注入前核对对应两条调用指令字节，未知布局拒绝启动。成功返回路径必须等于原 Documents，再改为本轮 ASCII `docs` 路径；仅缓冲区成功改写后清除 PIDL，内部 A/W 调用或失败返回不会提前消费。日志保留这两类 API 的返回码、PIDL、原路径和 caller，不能从旧日志的 redirect_count=0 单独推断失败原因。不修改系统注册表、系统 Documents 配置、游戏 EXE 或 PAC。原存档全文件清单在 prepare、launch 前和 launch 后比对。

可靠的后备隔离位于文件 I/O：仅将原 `GIGA/BALDRSKY_DIVEX/Save` 的完整路径或目录边界内子路径映射到本轮 `docs/GIGA/BALDRSKY_DIVEX/Save`，支持 DOS/NT 路径和 RootDirectory 相对路径解析。NtCreateFile/NtOpenFile/NtDeleteFile/NtQueryAttributesFile/NtQueryFullAttributesFile 使用独立复制的 OBJECT_ATTRIBUTES、UNICODE_STRING 与名称缓冲区，调用方原结构不改；存在性查询与实际读写保持同一个 Save 副本。rename/link 目标复用相同 alias，并复制信息结构、保留 flags；若无法映射则仍执行原目标写守卫。原 Save 和基底本身始终拒写。

对 `FILE_CREATE`、目录标志、目标已是目录这三个条件同时成立的请求，返回 `STATUS_OBJECT_NAME_COLLISION`，填 IO_STATUS_BLOCK 为 FILE_EXISTS 并清空输出句柄，不调用原生 create，不记 guard_violation。真实新建、覆盖、数据写及 FILE_OPEN_IF 不因该规则放宽。每次 alias 记录 `save_path_redirected` 及实际 NTSTATUS；目录碰撞不是成功 I/O。

默认私有模式下，NVIDIA Drs 使用与 Save 相同的目录边界 alias：仅原 Drs 根及其子路径的 DOS/NT 完整路径生效，RootDirectory 相对名称先解析；存在性查询、读写、删除及 rename/link 目标均使用同一私有树，原 Drs 优先拒写。ProgramData、NVIDIA 其它目录及 `DrsOther` 不匹配。`driver_state_redirected`、`driver_state_redirect_count`、`driver_state_redirect_success_count` 独立于 Save 计数。收尾的 `original_driver_state_check` 只报告原树新增/删除/变化或查询失败，标注外部写入归因未知，不单凭共享驱动状态变化判游戏失败；`driver_state_copy_after` 保存私有树最终 manifest。native 模式不执行这些 Drs alias，按前述正常驱动访问和独立审计规则运行。

文件守卫检查 NtCreateFile、NtOpenFile、NtDeleteFile、NtWriteFile、NtSetInformationFile，按以下次序保全：原 Save、base、默认私有模式的Drs及明确的源EXE/PAC优先拒写（源文件在仓库外也保护）；alias后的当前case路径允许；仓库其余路径拒写；仓库外已解析的普通绝对磁盘路径允许。DOS路径与NT卷路径均检查目录边界和规范化后的路径，RootDirectory相对路径先解析。未知/相对/无法解析的磁盘写目标仍拒绝。没有缓存目录白名单，也不再逐个添加正常OS日志目录。`external_write`记录允许的外部请求及参数，不声称该请求实际修改了磁盘，记录失败不阻断正常OS I/O。

已有目录的纯metadata打开和无写入碰撞处理保留；实际数据/元数据修改仍按上述目标范围检查。管道、字符设备及已验证的HID FILE_OPEN保持设备I/O分类，文件游标移动允许；不增加通用Device例外。未注入子进程继续拒绝。任何保全规则拒绝都记录并使本轮失败，不能把拒绝后继续运行记为通过。

音频调度对象 `\Device\MMCSS\MmThread` 仅按不区分大小写的完整名称识别；访问权限必须是已观察到的只读掩码 `0x00120089` 的子集，disposition 仅允许观察值 `0` 或 `FILE_OPEN=1`，拒绝目录标志和 delete-on-close。NtCreateFile/NtOpenFile 记录 `mmcss_device_open` 并保留原生参数及返回值；相邻名称、子路径、磁盘路径和其它 Device 名称不获得此例外，写入/删除/ACL/最大权限及创建/覆盖请求也不获得例外。

此工具的目标是保全明确输入、存档和仓库，不提供全盘安全沙箱。启动正常游戏通常伴随Windows/驱动的缓存或日志写入，这些已解析的仓库外请求不应被本任务附加的全盘白名单阻断。Frida会改变进程内存及执行时序；此工具不对恶意进程或系统委托服务提供安全隔离承诺。Save/Drs副本、原输入保护及退出验收仍独立执行。

EXE 文件偏移 `0x2789F8` 必须为原始 `8B FF`；`C3 90` 的全局 CRT RET/NOP 以及其它未知字节均拒绝。工具不写该位点，也不替用户恢复它。语音 gate 兼容补丁与该全局异常旁路必须单独审计。

异常处理器只记录并始终返回 false，异常交还原处理链。不会跳过指令、吞异常、写 WER 注册表或自动修改 EXE。工具退出前始终先结束仍活着的自有游戏，再由主线程统一卸载观测并关闭句柄。安装线程不会自行 detach；若终止失败或超时，工具保持运行并保留守卫/原句柄，状态为 failed、cleanup_pending=true，直到确认进程已退出。创建后身份读取失败时，也将原始挂起进程句柄交回主线程负责终止与确认，不在构造函数中丢失所有权。终止不依赖日志写入成功。请通过 Ctrl+C/STOP 正常停止工具，外部强制结束 Python 不能保证 finally 得到执行。

## 证据与验收

`manifest.json` 记录源路径、完整输入与输出 hash、原存档 hash。`state.json` 以临时文件原子替换，运行中每秒更新状态，包含进程身份、实际模块基址、hook_ready、重定向次数、external_write_count、异常/守卫统计、退出码、最终原存档比对、关闭句柄清单。`events.jsonl` 逐事件追加并 flush，保留异常上下文和 Frida detach/crash 文本。不会自动生成 WER dump。

退出状态为 `exited`、`stopped`、`failed`；正常退出还要求退出码 0，以及实际观察到 Documents 重定向或至少一次成功的 Save I/O alias。`redirect_count` 只计 Documents，`save_path_redirect_count` 与 `save_path_redirect_success_count` 分别计 Save alias 尝试及成功，不互相冒充。超时/用户停止返回代码 2，失败返回代码 1。即使 `exited` 也保留 `runtime_acceptance=not_evaluated`：是否完成角色菜单、简介、全部武装预览、训练出招、命中/效果和重载应由主代理结合 UI 与武装清单单独记录。进程活着、没有 dump 或没有异常消息都不是武装效果验收。

```powershell
C:\Python313\python.exe -B -m unittest discover -s tools -p test_bhe_runtime_session.py -v
```

离线测试使用临时目录，验证原输入逐字节保全、复制 hash、覆盖拒绝、Windows 路径边界、链接拒绝、CRT 早退拒绝、未选择覆盖包拒绝、原存档/准备产物新增删除修改检测，并用 Node 实际执行同一 JavaScript 路径策略检查目录逃逸。这些测试不创建游戏进程、不 attach、不操作原存档。

2026-10-27本轮完整离线回归为46项，全部通过；包括native驱动对照及原Save退出审计读取失败后仍继续驱动审计的回归。独立代码审查未发现Important阻塞，另指出的审计异常边界已修复。实际运行已越过存档标题导致的CRT终止，并在原版空存档及独立兼容seed对照中停留主菜单，但窗口捕获仍黑。F3与Snipaste全局贴图键的冲突已单独识别；移开贴图不等于游戏画面恢复。渲染循环、绘制调用和HRESULT正常不等于菜单可见或技能效果通过，当前所有case仍未获得角色玩法验收。
