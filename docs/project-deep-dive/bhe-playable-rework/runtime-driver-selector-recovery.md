# rework-04 驱动文件隔离修正

此报告记录 rework-04/06 的单文件方案与诊断回退。当前 Drs 整树隔离已替代单文件方案，见 `runtime-drs-state-recovery.md`；旧 case 与本历史证据保留。

本轮仅修改运行工具、离线测试和说明。没有启动游戏、运行 Maven、修改 mod 逻辑、注册表、原 Save 或 NVIDIA 原文件；没有提交。运行结果尚需新 case 验证。

## 已证实的停止原因

`rework-04/state.json` 的最终状态是 `failed/guard_violation`。Save alias 尝试 9 次、成功 6 次，原 Save 的新增/删除/变更均为 0，原线程与进程句柄均已关闭。主代理已观察并选择自动更新提示中的「いいえ」，本报告不把该 UI 观察扩展为游戏主菜单或战斗验收。

事件顺序明确：2026-10-26 23:37:08.442124 +09:00，NtCreateFile 请求 `\??\C:\ProgramData\NVIDIA Corporation\Drs\nvdrssel.bin`，access=`0xC0100080`、options=`0x60`、disposition=`3/FILE_OPEN_IF`，被工具判定为 case/已知 OS 缓存范围外的写入；23:37:08.442278 工具开始结束自有进程。退出码 `0xE0000001` 是工具的终止码，不是本次已证实的自主游戏崩溃码。

同时记录了 28 次 `exceptionType=system`，地址均为 `0x76B8BAE4`。现有事件缺少异常码、对应模块与完整调用栈，不能证明这些是驱动正常捕获，也不能证明是 mod 失败。异常处理器继续返回 false；本轮未过滤、吞掉或改写这些异常。

曾为后续 case 增加 DebugSymbol 与 ACCURATE 回溯，但 `rework-06` 证明这类诊断会引入自身符号文件 I/O，现已撤回这两项调用。当前只用 `Process.findModuleByAddress` 记录已加载模块名称/路径/基址/偏移，保留原始 address/context/memory；`backtrace` 明确为 `unavailable/no_symbol_resolution`。模块查找或记录器失败仍返回 false，事件仍标为 `unclassified_observed_exception`，不自动判断已捕获或 mod 故障。

只读核对的 NVIDIA 原文件为 1 字节，SHA256=`4BF5122F344554C53BDE2EBB8CD2B7E3D1600AD631C385A5D7CCE23C7785459A`，修改时间为本次运行前的 20:41:53。它是磁盘文件，不能采用之前 HID 的非磁盘设备分类。OPEN_IF 请求允许创建/打开，并申请读写权限；只有请求参数尚不能证明驱动实际写了哪些内容。

## 策略复核与最小变更

此前全进程文件守卫同时覆盖游戏、Shell32、DirectInput 和显示驱动。合法系统依赖的可写打开意图也会触发它，因而连续 guard 终止并不等于连续发现 mod 错误。此工具不能作为完整 Windows 沙箱来承诺所有依赖已枚举。本轮不继续扩大目录白名单，也不把所有未知依赖统一判为可写缓存。

改动选择单文件副本：prepare 从实际 NVIDIA 文件复制 seed 至 `case/driver-cache/nvdrssel.bin`，保持初始读字节；复制前后核对原文件和副本，manifest 记录来源/相对路径/哈希。launch 再核 seed，发生变化时拒绝用过期副本验证。

NtCreateFile/NtOpenFile、两种属性查询、NtDeleteFile 和 rename/link 目标复用现有 OBJECT_ATTRIBUTES/UNICODE_STRING/信息结构克隆，只对该文件的 DOS/NT 完整路径生效。原参数结构不改。原 selector 加入拒写保护；OPEN_IF 可以在私有副本执行，不是放行原 ProgramData 文件。Drs 目录、相邻 `nvdrsdb*.bin`、`.tmp`、命名流与子路径均不匹配此 alias。

事件用 `driver_file_redirected` 单独记录 NTSTATUS；`driver_file_redirect_count` 与 `driver_file_redirect_success_count` 不增加 Documents/Save 计数，不能冒充存档隔离证据。收尾比较原 selector 与准备时哈希，并保留副本最终哈希。原文件若因外部进程变化也会导致验证失败，不能仅据该比较归因于本游戏。

只复制这个已观察文件保证的是它的初始字节与本进程访问隔离，不证明整个 NVIDIA DRS 数据库或全部 GPU 初始化行为等价。后续如需访问其他资源，应以新的实际路径/权限/行为证据决定，不预先放开 ProgramData 或 Drs 目录。

## 验证

新增三个离线测试，先得到四个预期失败子例：未复制 seed、原 seed 变化未拒绝、副本变化未拒绝、缺少单文件 alias。实现后包含既有 22 项在内共 25 项通过。测试全用临时驱动 seed，不触碰系统 NVIDIA 文件：验证复制内容、非链接副本、源保全、两侧哈希漂移拒绝及单文件正负边界；既有真实 JavaScript 结构测试继续覆盖原参数缓冲区不变。完整注入脚本另做 Node 语法检查，不执行注入。

异常取证保留两个离线回归，但契约已按 `rework-06` 修正：直接执行 JavaScript observer，验证符号服务和回溯调用次数均为 0，module/offset 与原始 context/memory 保留，输入不变；模块/日志失败仍返回 false、不生成“正常捕获”判断。更新后先在旧实现得到 2 项失败（发生了符号调用、仍执行符号/回溯错误分支），移除调用后仍为共 27 项通过。

## rework-06：撤回取证自身的符号 I/O

2026-10-27 00:03:31.778087 +09:00 的唯一 guard 是 NtCreateFile 访问 `\??\D:\a\frida\frida\build\subprojects\frida-core\compat\arch-support.bundle.p\x86\lib\agent\libfrida-agent-raw.pdb`，access=`0x100180`、disposition=`1/FILE_OPEN`、options=`0x60`。随即在 00:03:31.778253 由工具结束自有进程，退出码仍为 `0xE0000001`；`exception_count=0`、Save alias 成功 6 次、原 Save 未变化。这说明新诊断尚未完成 announce 就触发了 Frida 自身 PDB 查找，不能据没有已记录 exception 认定没有发生异常。

修正仅删除刚加的 DebugSymbol.fromAddress 和 Thread.backtrace ACCURATE 诊断调用；不增加 D:\a/PDB 的允许规则，不替换游戏异常链，不改变 Save/driver alias、文件守卫或进程清理。没有获得其他回溯方案不会触发符号 I/O 的实际证据，因此不加替代回溯，也不新增运行依赖。该失败现场与 launch.lock 保持不动，未再次启动游戏。

本次未复用 `rework-04`，没有改其 manifest、日志或 launch.lock。异常和进程清理逻辑保持原样。
