# 动态验证工作流

> **日期**: 2026-04-06
> **目标**: 在不干扰 Thinktech packer 的前提下，对 BaldrSky.exe patch 后的运行时行为进行验证
> **前置文档**: [exe-grp-capacity-scan.md](exe-grp-capacity-scan.md)
> **脚本**: [`runtime_verify.py`](runtime_verify.py)（完整验证脚本）、[`debug_test.py`](debug_test.py)（最小化 Debug API 测试）

---

## 1. 问题背景

AKAO Graft Pipeline（`jinki2bsdx`）对 BaldrSky.exe 做了两类 patch：

1. **Meka capacity 103→104**（6 处 IMM8）——见 `PatchExeCapacitiesStep`
2. **SelectMekaMenu 行数上界**（1 处 IMM32，可选）——见 `0x14F21F`

静态分析（IDA + 二进制扫描）只能确认 patch 位点存在且字节值正确。但以下问题只有运行时才能回答：

- patch 后游戏是否正常启动？
- 进入 HellMode 是否 crash？（疑似 `VirtualAlloc` size = 0xFFFFFFFF 导致 OOM）
- 存档读/写是否覆盖了 AKAO 条目（meka index 103）？
- 场景切换时 AKAO 引用计数是否正确清理？

### 1.1 为什么不用常见 GUI 调试工具

| 工具 | 问题 |
|---|---|
| **Frida** | `frida.spawn()` 在 packer 解包前注入，直接导致 crash；`frida.attach()` 也可能干扰 Thinktech 保护 |
| **x64dbg / x32dbg** | GUI 工具，操作流程不透明，不利于自动化复现 |
| **Procmon** | 只监控文件/注册表/网络 I/O，看不到内存分配层面的问题 |
| **Cheat Engine** | GUI，且 debug 功能会用 Windows Debug API 的硬件断点，可能与 packer 冲突 |

### 1.2 约束条件

- **不注入 DLL、不做 hook** —— Thinktech packer 有完整性校验，检测到外部代码加载或代码段被修改会拒绝运行或直接 crash（Frida 的 `frida.spawn()` 就是因此挂掉的）
- **不使用 spawn 模式启动游戏** —— 必须让 Thinktech packer 正常解包后，再从外部 attach
- **所有验证通过命令行脚本完成** —— 可重复执行，不依赖 GUI

---

## 2. 完整验证流程

### 2.1 流程步骤

```
1. 跑 pipeline
   mvn "-Dtest=com.giga.nexas.jinki.TestJinki2BsdxRunner#testRunAkaoGraftPipeline" test

2. 复制 bsdx_game 到 tmp
   复制 src/main/resources/bsdx_game/ 到 tmp/baldrsky_{timestamp}/

3. 覆盖 pipeline 产出
   - out/BaldrSky_*.exe → tmp/baldrsky_{timestamp}/BaldrSky.exe
   - out/Update3*.pac → tmp/baldrsky_{timestamp}/Update3.pac

4. Debug API 启动游戏
   用 CreateProcessW(DEBUG_ONLY_THIS_PROCESS) 启动复制目录中的 exe
   监控所有异常事件，等待 crash 或正常退出

5. 查看 log
   检查是否有 FATAL 异常（ACCESS_VIOLATION 等 second-chance）
   如果没有，进入下一轮

6. 循环往复
   直到找出问题或确认稳定
```

### 2.2 一键执行

```powershell
cd d:\Code\NeXAS_DX
python docs\project-deep-dive\runtime_verify.py --loop
```

参数说明：

| 参数 | 默认值 | 说明 |
|---|---|---|
| `--loop` | false | 循环模式，每轮结束后自动重新开始 |
| `--interval` | 5 | 循环间隔秒数 |
| `--pipeline` | true | 是否先跑 pipeline |
| `--no-pipeline` | — | 跳过 pipeline（用已有 out 产出） |
| `--timeout` | 600 | 游戏监控超时秒数 |

---

## 3. 技术细节：Windows Debug API

### 3.1 方案选择

使用 `CreateProcessW` + `DEBUG_ONLY_THIS_PROCESS` flag 启动游戏（不是 `DebugActiveProcess` attach），因为：

- 需要覆盖 exe 和 pac 后才启动，不能"手动双击启动再 attach"
- `DEBUG_ONLY_THIS_PROCESS` 不注入任何代码，只是让系统转发 debug 事件
- 与 packer 无冲突（已验证游戏正常启动到标题画面）

### 3.2 WOW64 结构体偏移

**实测确认**（64-bit Python 调试 32-bit 进程）：

`DEBUG_EVENT` 在 64-bit Windows 上调试 32-bit 进程时，union 前有 **4 字节 padding**（始终为 0），所有指针和 HANDLE 为 **8 字节**。

```
DEBUG_EVENT 总体布局：
  +0:  dwDebugEventCode (DWORD, 4 bytes)
  +4:  dwProcessId (DWORD, 4 bytes)
  +8:  dwThreadId (DWORD, 4 bytes)
  +12: [padding 4 bytes, always 0]    ← 关键！
  +16: union data start

EXCEPTION_DEBUG_INFO (从 union 起始 +16)：
  +16: ExceptionCode (DWORD)
  +20: ExceptionFlags (DWORD)
  +24: ExceptionRecord (ULONG_PTR, 8 bytes)
  +32: ExceptionAddress (ULONG_PTR, 8 bytes)     ← 异常发生地址
  +40: NumberParameters (DWORD)
  +44: [padding 4 bytes]
  +48: ExceptionInformation[0] (ULONG_PTR, 8 bytes each)
  +168: dwFirstChance (DWORD)                    ← 1=first chance, 0=unhandled (FATAL)

LOAD_DLL_DEBUG_INFO (从 union 起始 +16)：
  +16: hFile (HANDLE, 8 bytes)
  +24: lpBaseOfDll (LPVOID, 8 bytes)             ← DLL 基址
  +32: dwDebugInfoFileOffset (DWORD)
  +36: nDebugInfoSize (DWORD)
  +40: lpImageName (LPVOID, 8 bytes)
  +48: fUnicode (WORD)

EXIT_PROCESS_DEBUG_INFO：
  +16: dwExitCode (DWORD)
```

> **注意**：不能直接用 ctypes 的 `EXCEPTION_DEBUG_INFO` 结构体，因为 ctypes 会按 64-bit 对齐（指针 8 字节），但 WOW64 调试器收到的 union 前有额外的 4 字节 padding。实测中直接用 `create_string_buffer(512)` 手动解析偏移是最可靠的方式。

### 3.3 实测结果

用 `debug_test.py` 对 `BaldrSky.exe.bak`（未加壳版）进行了测试：

| 项目 | 结果 |
|---|---|
| `CreateProcessW` + `DEBUG_ONLY_THIS_PROCESS` | ✅ 成功启动 |
| DLL 加载事件 | ✅ 正确捕获（基址如 `0x00007FFC960A0000`） |
| BREAKPOINT (0x80000003) | ✅ ntdll 初始断点，first chance，正常 |
| STATUS_PRIVILEGED_INSTRUCTION (0xC0000096) | 170+ 次，全部 first chance，游戏自动处理（WOW64 正常行为） |
| STATUS_SINGLE_STEP (0x4000001F) | 1 次，first chance，调试器附加产生的，正常 |
| FATAL 异常 (second chance) | ❌ 未检测到 |
| 游戏是否正常运行 | ✅ 正常进入标题画面 |

### 3.4 常见 first-chance 异常

在 64-bit Windows 上调试 32-bit BaldrSky.exe 时，以下 first-chance 异常是**正常现象**，不代表游戏有 bug：

| 异常码 | 名称 | 说明 |
|---|---|---|
| `0x80000003` | BREAKPOINT | ntdll 初始断点，每个被 debug 的进程都会有 |
| `0xC0000096` | PRIVILEGED_INSTRUCTION | WOW64 thunk 触发的特权指令，由 WOW64 层自动处理 |
| `0x4000001F` | SINGLE_STEP | 调试器附加产生的单步异常，由系统自动处理 |
| `0xE06D7363` | CXX_EXCEPTION | MSVC C++ 异常（如 `throw`），由游戏自身 catch |

**只有 second-chance（unhandled）异常才表示真正的 crash**。脚本会特别标记 `FATAL` 并记录到 log。

---

## 4. 验证 Checklist

patch 后的 exe 需要验证以下场景：

| # | 场景 | 预期结果 | 监控方式 |
|---|---|---|---|
| 1 | 游戏正常启动 | 无 FATAL 异常 | Debug API 旁观 |
| 2 | 进入 HellMode | 不 crash；或如果 crash，记录异常地址 | Debug API 旁观 |
| 3 | AKAO 机体在机体选择列表中可见 | SelectMekaMenu 正确显示第 104 行 | 截图对比 |
| 4 | 进入战斗，AKAO 机体可用 | 不 crash，模型/技能/动画正常 | Debug API 旁观 |
| 5 | 存档 → 退出 → 读档 | AKAO 状态完整保留 | 对比存档二进制 |
| 6 | 场景切换（地图→战斗→地图） | AKAO 引用计数正确清理，无泄漏 | Debug API 旁观（观察内存趋势） |

---

## 5. 与静态分析的关系

| 阶段 | 方法 | 产出 |
|---|---|---|
| **静态** | IDA + 二进制扫描 | [exe-grp-capacity-scan.md](exe-grp-capacity-scan.md) — 确认 6 处 meka cap 位点 |
| **构建** | `PatchExeCapacitiesStep` | patched exe 文件 |
| **动态** | 本文档的工作流 | crash 报告、行为验证 |

静态分析回答"patch 在哪里"，动态验证回答"patch 后能不能跑"。
