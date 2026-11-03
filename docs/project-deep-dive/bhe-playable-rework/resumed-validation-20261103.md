# 2026-11-03恢复验证记录

用户已明确允许仓库内构建、测试和实机验收，解除此前“暂不构建”限制。人工文件写入和删除仍仅限`D:\Code\NeXAS_DX`；不push、不建立分支或worktree、不上传二进制。源码放在src或tools，target仅放可再生成产物。

## 恢复基线

- 分支`develop`，恢复时HEAD为`ffaf1ed17`；原Clarias六文件及旧Sou菜单说明WIP保持原状。
- C盘旧`C:\NeXAS_DX_runs`已不存在，不作为构建、运行或证据输入；D盘恢复前约有338GiB可用空间。
- 原EXE`src/main/resources/ida-reverse/BaldrSky.exe`实测SHA256为`713cf605360a55a863f87bfbdea5453aaeeb2a33983ad4a005c9297e3a86e757`。不使用bsdx_game中的旧宽屏EXE。
- 先验证存储和进程策略，再验证Sou四协议及角色回归，之后才执行整链构建与新包实机验收。旧222项通过、85项历史预览不转记为新包通过。

## 已执行

1. `C:\Python313\python.exe -B -m unittest discover -s tools -p test_bhe_runtime_session.py -v`：54项，失败0、错误0、跳过0，退出码0，20.236秒。TEMP/TMP/TMPDIR及所有fixture限定在仓库target；没有启动游戏或Frida。
2. Java存储策略首轮：5类42项，失败0、错误1、跳过8。Windows强制锁使旧产物不可读；测试在释放旧文件锁前关闭构建对象，导致预期拒绝后，自动清理再次读取同一受锁旧文件并抛错。仅调整该测试资源生命周期，保留产品锁及哈希检查，并补充解锁后可恢复构建、退役旧产物的断言。8项地图测试因未提供静态资源根而跳过；重跑显式提供只读BHE资源根，不将首轮跳过算通过。
3. Astra6 Ultra独立只读审查134/135修复及原始资源：未发现确定阻断项；14个完整Skill原始签名匹配源码常量，私有SE非identity重绑及危险发射边均有对应实现。该结论不替代测试或实机；134公共虫体的条件回收仍需覆盖落空、中断。
4. 修正测试夹具后，经受控入口重跑存储策略5类42项：失败0、错误0、跳过0，退出码0。其中受管构建22项、V2中间封包开关3项、原生子进程模拟8项、地图输出冲突8项、地图事务冲突1项。地图测试显式读取`D:/BDY/NeXAS_Resources/bhe_resources`，仅在仓库target写入测试产物；未执行游戏构建。首轮锁冲突RED报告与本轮摘要保存在可清除的`target/bhe-checks-20261103`，不提交生成二进制。
5. 当前角色相关回归首轮实际执行29类230项：失败0、错误1、跳过0；唯一错误是134回环夹具把GRP命名为`flare-WazaGroup1.grp`，按真实basename分派的解析器不接受该名。仅将两次回环放到独立目录，各使用协议文件名`WazaGroup.grp`，未放宽解析器或原强断言。随后完整重跑该类8项，失败0、错误0、跳过0、退出码0。因此本轮相关范围的229个首轮通过项与该类修后8项均有实际运行证据；不把这写成一次230项全绿运行。
6. 恢复的被动观察器新20项与原运行工具54项联合执行：74项，失败0、错误0、跳过0，退出码0，11.309秒。新观察器先取得19项真实RED，另取得读错后旧请求不得误闭合的1项RED，再修正关联失效处理。仅Node内存模型，不启动Frida或游戏；真实挂点仍待本轮新包确认。

## 当前边界

本轮新整链包已构建并完成静态身份核验，尚无武装实机通过结果。Sou153项、九角色54追加武装的效果、跨武装连段、取消、中断、再次起手、重载和关卡推进必须继续逐项验收。

旧target观察器源码已丢，但保全的case09文本日志回显了四个原观察断点及请求关联条件。恢复观察工具时保留这些条件，安装前核对EXE身份和指令字节；仅观察，不强制出招、不修改动作或异常行为。正式工具存入tools，原始二进制与私人存档不提交。

## 本轮整链与成品核验

受控入口`TestBheByJinkiRunner#testRunNineCharacterRework`真实完成，退出码0；JUnit记录1项、失败0、错误0、跳过0，耗时242.399秒。

- 唯一构建目录：`target/bhe-playable-builds/run_20261103_154924_549_070a8f21`，受管状态COMPLETE。
- EXE：`sou/BaldrSky_20261103_155143_736.exe`，4793856字节，SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`。
- PAC：`sou/Update3.pac`，1460490768字节，SHA256 `10a6d5f50cd528e9206744c53a0dfaac5836f204d384ab3219d07085f5ab1996`。最终封包步骤在发布前调用`PacDeliveryVerifier`真实解包并核路径/内容，成功后清除它创建的验证副本；没有另做第二套解包副本。
- 独立读取最终3884个资产，与保全的180550解包审计比对；先核保全容器和重建原文本的长度/SHA。无新增、无删除，仅`WazaGroup.grp`、`sou.waz`、`tama05.waz`变化；其余3881个长度及SHA相同。审计文本为`target/bhe-checks-20261103/built-assets-vs-180550.json`，不复制旧二进制。
- receipt仍为构建时的`runtimeValidated=false`；上述核验不替代实机。

## 首次隔离运行

case `sou-finite-20261103-01`使用本轮EXE/PAC及空私有Save。4个观察挂点的运行时签名核对和安装实际成功，随后出现正常自动更新询问框；选择不启用更新。

后续出现`NtCreateFile`写入`C:/ProgramData/NVIDIA Corporation/ShadowPlay/CaptureCore.log`请求，守卫拒绝外部写入并主动终止自有进程，终止码`E0000001`。此轮状态`failed/guard_violation`，不能记作游戏自然崩溃，也不算任何武装通过。随后仅对该完整日志路径实现case内私有重定向，不放宽ProgramData目录写权限。

退出已确认、guard释放完成、`cleanup_pending=false`；程序实际清除28份仍与输入一致的game副本，共5805648253字节，清理错误0、保留改变/唯一文件0。私有Config、运行文本与所需临时资料保留；未删除源游戏或构建包，未写删C盘原件。

## 观察工具与精确日志重定向

正式工具`tools/bhe_runtime_observer.py`由保全的四挂点恢复，默认不启用；Astra6 Ultra与root分别核对了被动性、固定EXE/指令签名、请求关联及安装时序。root另补越界请求不能保留旧accepted关联的真实RED，修后20项全通过。

首次守卫停止后，为准确CaptureCore.log路径补4个测试方法，先真实取得6个断言失败、错误0的RED，再实现私有空日志及精确别名；未读取或复制原日志。当前58项harness与20项observer联合回归共78项，失败0、错误0、跳过0，退出码0，11.241秒。`capture-log-all-green.log`及小体积RED日志位于`target/bhe-observer-checks`。

该补丁只影响试跑隔离工具，游戏成品EXE/PAC没有再次构建或修改。新case `sou-finite-20261103-02`使用上一轮已退出case的私有Config作为独立种子，不覆盖真实Save；后续结果另记，不能由78项离线通过推导实机已通过。
