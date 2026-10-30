# Sou「呪装刀」单目标适配：2026-10-30

> 2026-10-31资料状态：本文测试计数和旧运行身份只对应记录当时的代码。原target已清空，旧报告／运行日志路径不再作为当前可访问文件保证。现保全的资料见[实机文本证据说明](../../../src/test/resources/bhe-runtime-evidence-20261030/README.md)；程序、PAC、转储、截图、录像、存档及内存二进制没有转存。历史GREEN不能转记为当前源码已通过。

Sou菜单第63项进入攻击后会反复重入同一阶段，达到999 HIT仍不收招。根因是BHE的“剩余多锁目标数大于0”在DX条件转换中降为无条件，而多锁生产、游标和消费事件已被丢弃，原本有限的逐目标动作因此失去退出条件。

## 实际修复范围

现有SouCustomizer追加局部SouCursedBladeSingleTargetFixer，限定双名呪装刀Lv1／Lv2／Lv3与JUSOUTOU01／02／03。只删除各等级P1中11／9／7帧的唯一INPUT1自回边，保留完整P0、P1的46帧NEXT、P2的49帧NEXT，以及其余攻击、物理、资源和取消事件。当前适配语义是单一目标的一次动作；无目标时保留原来的普通挥空入口。此改动不提供BHE多目标连续选取机制。

处理前三等级都要通过原始BHE与最终DX的完整Change签名、3阶段及suffix核验。先构造三份独立技能候选，再一次发布最终列表；任一等级漂移、重复slot／event、空列表或部分已安装状态均拒绝额外写入。重复处理已验证的完整安装状态时保持原列表、技能、阶段与事件对象身份。原始资源只读，不修改全局MULTILOCK fallback，也不添加热量预算、弹体、武装或强制结束状态。

## 根因证据

- 旧180550包case09，REQ700在tick177535接受武装63；超过观察期限仍无同请求READY，后被正常切换到64取代。快照显示group120／W231／P1、clock1、G0。
- 原生采样分别得到tick188802的重入后clock0，以及下一轮tick188810、clock7时请求INPUT1。它们是相邻两轮事件，不当作同一轮严格配对。
- 原BHE三等级P1回边的条件为MULTILOCK／TARGETCOUNTER_MAX > 0；旧目标BIN对应包成为无条件INPUT1。源及目标的46／49帧恢复出口都还存在，只是旧无条件回边遮住了出口。
- 原P1没有松键或热量退出条件；三等级全部FreeParam／Heat／Charge相关空头已直接核原BIN，不能靠补一个未经证实的HC消费处理此问题。

原字节证据见target/sou063-debug-20261030/independent-investigation.md（历史路径`target/sou063-debug-20261030/independent-investigation.md`，已清空）、Astra设计与字段核验（历史路径`target/sou063-single-target-design-20261030/design.md`，已清空）及独立交叉审查（历史路径`target/sou063-cross-review-20261030-mnk/report.md`，已清空）。

## 验证边界

新增7项测试先经root实际执行得到7失败／0错误。测试核完整主WAZ仅删三事件、保留46／49恢复路径、源资源隔离、三等级原子拒绝、重复处理对象身份、真正二进制两次生成／读回及字节一致。Counter测试夹具先独立应用本适配，只隔离已另有测试的局部机制，原有完整非counter技能保全断言未放宽。

首次修复后27项中仅二进制完整图比较失败。完整差异盘点恰为209个phaseQuantity=0、阶段／suffix均空的技能壳，其两个名称从null读回为空字符串，共418字段；这些技能的empty为false，不是flag0禁用技能。现有BinaryWriter对null名称写NUL，Parser读回空串；二进制比较的期望图只对已核实的空壳名称作对应处理，其余图和生产代码保持严格比较。首次失败、完整图诊断和后续结果分别保存，不能把测试夹具表示差异当作游戏缺陷。

root最终显式运行原26类206项加新增7项，得到27类213项、0失败／0错误／0跳过；日志为target/bhe-audit-20261026/sou063-single-target-full-regression-20261030.log（历史路径`target/bhe-audit-20261026/sou063-single-target-full-regression-20261030.log`，已清空），27份XML和汇总冻结于[green-sou063-full](D:/Code/NeXAS_DX/.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/green-sou063-full/summary.json)。本源码修复尚未产生新的整链交付包，未证明修后菜单、训练、命中／落空或受击中断通过。180550仍是包含旧自回边的失败对照；后续须在新包重新验收63，不使用其它武装的READY代替。
