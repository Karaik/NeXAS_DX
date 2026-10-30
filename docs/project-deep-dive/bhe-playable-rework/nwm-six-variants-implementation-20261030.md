# Nagi／Wilhelm／Motoki：各六个追加武装的实现检查点

三人各保留既有一个追加，再加五个独立普通MEK，合计18个追加入口。Motoki原投弹的router与私有四阶段execution仍合计一个武装；新短照射复用本角色原beam的第一批，不复制公共464或创建额外可装备helper。设计、供体、用途、热耗和日文简介见 `docs/superpowers/specs/2026-10-30-six-variants-nagi-wilhelm-motoki.md`。

实现使用现有三个Customizer及各自AdditionalVariants。整批在独立MEK/WAZ副本核身份、phase／完整suffix、事件窗与关键载荷，全部成立后仅发布新增尾部；已有六项会在stage重建后比完整载荷，漂移拒绝，不重建外层既有对象。Nagi既有防反成功分支补清甲；Wilhelm既有交互炮入口补明确物理恢复。原招、公共资源、原Material前缀和旧投弹执行合同保留。

root初次C组回归18项全有效RED。v5统一137项中，C相关九项失败分别来自测试将首供体合法Material重复项误去重、将最终包裸引用套用原转换fixture，以及新私有副本编辑后残留空unit导致完整图回读不同。按既有clone/merge规则保留首供体、以真实供体解析引用并保留完整时间窗/载荷比较，仅移除已编辑副本的空unit后，v6实际137项全绿。

Astra6 Ultra独立审查另找到两类保护缺口：Nagi斬上げ条件入口漏验NEXT目标／aux；Nagi与Wilhelm本轮也修改的既有主供体漏验suffix。新增五项回归后v7为17项5失败0错误；生产局部补完整condition/aux/mode/唯一NEXT，以及主供体和实际使用utility的literal suffix，并在outer stage前只读验证。Astra补充复核未发现正常源误拒，原N1成功反击和W1准备／入口保持。

最终v8统一18类142项零失败／错误／跳过，含本组23个SixVariantsTest、旧Nagi/Motoki与共享变体回归、其余六角色及默认装备14项。日志为 `target/bhe-audit-20261026/nine-six-variants-regression-20261030-v8.log`；XML固定主SDD目录green-nine-v8，旧失败证据不覆盖。

这次只固定源码、规格及定向回归成果。九角色代码总计原7+新47=54个独立追加，但最终PAC字节、全部新技能真实跨武装连段／落空／中断、Sou153项和关卡验收仍未完成；runtimeValidated=false。单次构建或绿色Java回归不替代这些运行条件。
