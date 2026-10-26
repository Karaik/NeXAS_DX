# Task1公共Change跳转：实现与验证

基线c27bc14fb8a26dff36b87cd6cf399dc818686b95。生产修改仅CrossRedirectBheCommonProjectileResourcesStep；新增CommonChangeRedirectTest。暂未提交，工作树另有独立乱码任务和原有WIP，不属于本任务diff。

在现有公共重定向步骤加入CEventChange.list2处理，限定当前BHE的JUMP/WAZA（源root16/type[1]）语法，使用appendPlan目标group和skillBase计算引用。缺少显式映射拒绝；phase/算法参数和负哨兵保持。仍由既有Term重编译步骤转换字典，未调整架构。

TDD RED：mvn -q "-Dtest=CommonChangeRedirectTest" test，4tests/3failures/0errors。期望[0,246]实际[0,100]；继承base期望801实际100；缺映射未拒绝。结果保存target/bhe-audit-20261026/CommonChangeRedirectTest-red.txt。

TDD GREEN：相同命令，4tests/0failures/0errors/0skips，Exit0。验证包含CEventChange实际字节写出重解析、不同目标group、动态base、phase/算法/负哨兵与缺映射。

真实输入验证：重新编译target/audit-20261026/CommonChangeProbe.java，使用本轮Maven生成的target/classes，读取实际8份BHE公共WAZ和原版DX宿主，调用生产helper与WAZ生成/解析。39处Change全部命中实际append计划，group/skill不一致0。结果target/bhe-audit-20261026/common-change-fixed/common-change-results.tsv；原探针SPM/SE使用明确identity stub，不能称完整graft或游戏通过。原始数据未写入。

首次调用探针缺class，随后在独立probe-classes重新编译，成功运行并核39条，不把首次PowerShell末尾对象显示误当成功证据。

本任务没有进行游戏验收、没有修其他技能载荷、没有更改Sou实现，不声称最终游戏稳定性已完成。
