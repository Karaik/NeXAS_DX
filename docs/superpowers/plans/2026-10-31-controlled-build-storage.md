# BHE 构建输出约束与有限保留实施计划

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 在保留九角色和Sou改造目标的前提下，限制工作产物只能写在本仓库内，防止整套构建和试跑副本无限累积。

**Architecture:** 修改现有构建入口、V2封包开关及现有试跑工具的文件管理逻辑，不改移植链结构。清理只处理程序明确拥有且已验证的副本；原始输入、存档、未知目录和活动进程使用的数据不进入清理范围。

**Tech Stack:** Java 17/JUnit 5、Python标准库、现有JINKI V2及BHE移植链。

**Spec:** 仓库`AGENT.md`、`src/test/java/AGENT.md`及本对话最新约束。

## Global Constraints

- 所有人工文件写入和删除仅限`D:\Code\NeXAS_DX`；C盘旧目录只读。
- 当前仅允许源码修改，不运行Maven、javac、测试、游戏或构建，不产生新运行包。
- 代码位于`src`或`tools`，文档位于`docs`；`target`只放可再生成的产物。
- 不复制或提交二进制，不push，不新建分支或worktree；使用develop和中文本地commit。
- 新文本使用UTF-8无BOM、CRLF；原Clarias六文件及旧Sou菜单WIP不混入提交。
- 旧222项通过结果只对应旧已验证源码；本轮新修改未运行测试，不宣称通过或已实机修复。

## Task 1: 约束九角色构建并清理旧受管理产物

**Files:** `src/test/java/com/giga/nexas/bhe2bsdx/byJinki/TestBheByJinkiRunner.java`、同包`ManagedPlayableBuild.java`及`ManagedPlayableBuildTest.java`。

- [ ] 在创建任何目录前验证输出根位于本仓库`target`内，拒绝外部路径、根目录、输入目录及重解析路径。
- [ ] 新运行目录使用明确所有权标记和互斥锁，避免并发构建或未经识别的目录进入清理。
- [ ] 新EXE/PAC及receipt验证成功后才发布latest并清理旧受管理运行；新构建失败保留此前完成产物。
- [ ] 未标记、活动或锁定产物保持原位并明确报告，不能先删部分数据再假称成功。
- [ ] 添加外部根、越界、未拥有目录、失败保旧、成功替代和并发锁等测试源码。本轮不运行。

## Task 2: 避免中间JINKI V2封包

**Files:** `AkaoGraftRequest.java`、`v2/pack/PackUpdatePacStepV2.java`、九角色构建入口及V2封包定向测试。

- [ ] 请求增加默认开启的`packUpdatePac`；V2关闭时返回未封包计划，不调用打包器、不移动PAC。
- [ ] 九角色链显式关闭初始JINKI封包，继续使用原有导出资源树和EXE，最终Sou仍正常封包。
- [ ] 添加关闭时不执行打包及默认行为的测试源码；V1不改。本轮不运行。

## Task 3: 约束试跑目录、临时文件及结束清理

**Files:** `tools/bhe_runtime_session.py`、`tools/test_bhe_runtime_session.py`。

- [ ] 运行根限定在本仓库`target`，所有路径检查必须早于mkdir或复制。
- [ ] 在导入Frida前设置case内临时目录，子进程继承该临时目录。
- [ ] 进程确定结束、隔离已释放后，仅移除与manifest长度及SHA完全一致的game输入副本；保留新增或改变的文件、私有存档和文本证据。
- [ ] 清理拒绝活动/未知/重解析目录，保留逐项结果；不递归删除原始资源或未知内容。
- [ ] 文件守卫拒绝case以外的磁盘写入，保留原有IPC/只读设备例外；外部请求不能继续以“已允许”记账。
- [ ] 补充路径、清理资格、内容变化、临时目录及外部写入策略测试源码。本轮只作AST和格式检查。

## Task 4: 完成134源码集成并审查

**Files:** `SouFlareSeekerSingleTargetFixer.java`、`SouCustomizer.java`及Counter/Blade/Snow/Flare四协议测试。

- [ ] 三caller及私有Lv3有限载体保持此前批准的最小变更；公共技能和原资源不改。
- [ ] 主caller私有SE16:4按真实Followup映射重绑；fixture显式使用非identity映射并隔离byte数组。
- [ ] 保留全部原完整图断言，补错SE/缺映射变体，最后接入Customizer。
- [ ] Astra Ultra独立审查文件管理和134的源码；审查记录明确当前未构建、未运行测试。

## 受控验证入口

新增`tools/Invoke-BheChecks.ps1`仅接受显式测试选择器，设置本仓库target临时目录及`.mvn/repository`依赖缓存，不改变系统环境；`.gitignore`排除该缓存。Runner在写构建目录前检查JVM临时目录。PacUtil与地图暂存均使用owned父目录，native打包子进程明确等待退出。入口源码只作PowerShell Parser检查，本轮未执行。

## 验证边界

本轮只执行`git diff --check`、Python `ast.parse`、文本编码/换行检查和源码交叉审查，不运行本计划新增的任何测试。未来允许构建后，先执行存储策略测试和Sou四协议定向回归，再运行完整相关回归；打包、PAC解包、Sou153项和新增54武装实机连段验收仍单独进行。
