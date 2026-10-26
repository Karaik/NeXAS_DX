# rework-07 NVIDIA Drs 私有状态树

后续 rework-08 的 ShadowPlay 日志误拦促使工具改为输入/存档保全模式，见 `runtime-harness.md`。本报告的 Drs私有树与原树保护仍有效，但“未匹配Drs alias”不再等于拒绝普通仓库外OS写入。

2026-10-27。仅修改运行工具、离线测试及说明，没有启动游戏、Maven、提交、子代理或改产品/mod 架构。Save/base 保护、原生异常 return false、无符号解析及进程清理保持原行为。

## 实际证据与调整理由

`rework-07/state.json` 记录旧 selector alias 成功 12 次、Save alias 成功 6 次，原 Save 未变。唯一 guard 是 NtCreateFile 请求 `C:\ProgramData\NVIDIA Corporation\Drs\nvAppTimestamps`，access=`0xC0100080`、options=`0x60`、disposition=`3/FILE_OPEN_IF`。因此停止仍是隔离工具拒绝驱动状态访问，不能当作 mod 根因；28 次 system 异常也不据数量归因。

同一 Drs 状态中的 selector、timestamp 和数据库不应一部分使用副本、一部分使用全局原数据。此轮把已经观察到的 Drs 目录作为一个状态单元复制，而不再逐文件加例外，也不放开整个 ProgramData 或 NVIDIA 目录。

本轮只读盘点为 6 个普通文件、共 5,476,097 字节：

| 相对路径 | 字节 |
| --- | ---: |
| nvAppTimestamps | 224928 |
| nvdrsdb0.bin | 2625400 |
| nvdrsdb1.bin | 2625400 |
| nvdrssel.bin | 1 |
| nvdrswr.lk | 0 |
| update.bin | 368 |

这是盘点时快照；实际新 case 的 manifest 以 prepare 实时读取结果为准，不硬编码文件数量或文件清单。

## 复制、边界与证据

prepare 从 `%ProgramData%/NVIDIA Corporation/Drs` 复制完整目录到 `case/driver-cache/Drs`，拒绝重解析点/链接。它在复制前取得源文件 manifest，复制后再次读取源和副本，以完整相对路径集合、文件大小及 SHA256 对比。`state.json` 保存 `driver_state_copy_check` 的 before/after/copy 及双方差异；复制过程中发生新增、删除、内容漂移或副本不一致都拒绝，保留新 case 和证据，不删除/恢复原驱动文件。

成功 manifest 使用 `driver_state`，包含 source、relative_path、files、file_count、total_bytes。launch 在创建进程前再核原树和副本。旧单文件 `driver_seed` 不再接受；必须新建 case，旧目录及锁完全不改。

alias 复用现有 Save 的精确目录边界规则：原 Drs 根或 `Drs\...` 子路径转换到私有树，保留后缀大小写；DOS、NT 设备路径及 RootDirectory 相对名称处理一致。NtCreateFile/NtOpenFile/NtDeleteFile、两种属性查询、rename/link 目标均复用原结构克隆机制。原 Drs 整树加入优先拒写规则；`DrsOther`、`Drs\..\Other`、ProgramData 下其它路径均不匹配。该 alias 不改变调用的 disposition 或申请权限，读写实际作用于副本；既有目录碰撞规则保持原样。

日志改用 `driver_state_redirected` 及对应尝试/成功计数，不增加 Documents/Save 的隔离计数。收尾 `original_driver_state_check` 只报告原树差异或读取错误，明确 `attribution=not_determined_external_writers_possible`。共享原树可能被系统其它进程更新，单凭该比较不将游戏判失败；原 Save 的失败判定不变。私有树最终文件 manifest 单独写 `driver_state_copy_after`。

## 离线验证

先更新原三个单文件测试并增加复制时漂移回归，在旧实现得到 5 个预期失败子例：没有 Drs 整树、副本/原树 timestamp 漂移未拒绝、缺目录 alias、复制时新增文件未拒绝。实现后 28 项通过，再补运行期间原树变化的报告边界测试，最终 29 项全部通过。

测试覆盖嵌套文件完整复制、3 文件/12 字节合成 manifest、修改私有副本不改变源、原树/副本内容漂移、复制时外部新增文件后拒绝且证据/副本保留；JavaScript 实际执行验证 Drs 根、selector、timestamp、db、新文件及嵌套文件命中，邻接 DrsOther、父目录逃逸和其它 ProgramData 路径不命中。运行期漂移测试使用模拟进程与临时 Drs，证明只写差异和未知归因、不把原树变化变成游戏失败。既有异常、清理、Save 隔离和原结构保全回归继续通过。

未再次实机验证；本报告不声称 NVIDIA 初始化或九角色战斗已经通过。没有新增符号服务或回溯依赖。
