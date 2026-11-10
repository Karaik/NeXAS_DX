# 3. aux按消费者和表达式分支转换

日期：2026-10-08。旧代码统一按OBJECTPOS/OBJECTPOS2转换aux，无法表示条件和MEK AI使用的OBJECT字典。

实施中重新闭合了原生分支，纠正审查时“所有ANGLE9→8”的粗糙表述：BHE73F160与BSDX669CD0的ANGLE首项1/2调用位置解析器，首项3才调用OBJECT角度选择器。真实Sou BODYSLUM01的首项1、aux9表示HIT位置，应保持9；Misaki M_UPPER04位置分支FIREPOS14应变为13。ANGLE首项3的OBJECT9/HIT_WAZA才映射目标8/HIT。

typed AuxConsumerPurpose随对象图最近宿主传递：CEventTerm原生type5位置、6条件、9/10角度、11参数；CCpuEvent强制OBJECT，即使其语法根为POS。Change条件List1按OBJECT、动作选择List2未读aux；Move两端位置使用位置分支。条件原生66EA50在解释主链前调用668D70取两组aux首项，第二项未读；AI亦走668D70。参数literal、BEFORE等已知未读分支保留原载荷并审计，不把它们当未知错误。

转换提供结构化AuxAudit样例及完整分类计数，记录来源路径、字典、before/after、第二项未读与CENTER/TOP高度损失。样例最多128条避免全资源遍历无限占内存。已消费的null/负/越界索引带上下文失败；未知typed宿主非空aux明确失败。原主Term链规则保留，本项没有实施高度补偿、条件状态机或Hit位域改写。

验证：先运行AuxConsumerRebindTest旧实现，5项断言失败、1项真实错误（未读999被位置表越界）。实施后定向10项全部通过；覆盖两种ANGLE分支、条件、AI、原生Term type、未知用途、上下文错误、高度损失、真实Sou/Misaki完整对象图。两份MEK重绑真实/合成3项亦通过。测试命令：`mvn -q "-Dtest=AuxConsumerRebindTest,RebindMekStepBheResourceRebindTest" test`。

额外修正：初版typed分配误将Java同类DTO的type9当参数，真实Sou SETUGEKKA01暴露位置aux23被OBJECT误读。对照5E0690/5DF600原生工厂后修为type5位置、6条件、9/10角度、11参数，真实完整对象图随后通过。未用已有错输出作为新期望。

范围：消费者原生源码、对象图转换与资源测试；没有运行游戏。未知消费上下文的新增资源将明确暴露，而非静默猜字典。

独立审阅：生产逻辑符合最新消费者证据；补充了真实Misaki M_UPPER04 P1 Effect#3 Unit28的aux4=[13,0]断言，并将既有null/越界测试从原生数组异常改为带上下文的IllegalStateException。复核命令：`mvn -q "-Dtest=AuxConsumerRebindTest,BheInfoCollectionTermConverterSemanticTest#invalidObjectPositionAuxIndicesFailClosed,RebindMekStepBheResourceRebindTest,RebindWazStepCommonReferenceTest" test`；16项全部通过，无跳过。全量审计写出方法没有执行。
