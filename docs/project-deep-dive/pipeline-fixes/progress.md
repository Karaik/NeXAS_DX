# 五项修复进度

计划：docs/superpowers/plans/2026-10-07-bhe-pipeline-fixes.md。

起点：c6c4e039916b3b4cba4b86474f936d586db670bd。分支：codex/bhe-pipeline-fixes。

用户已授权实现1～5、分别提交、留下笔记。原工作区的无关修改保留。

| 项 | 状态 | 提交 | 验证/笔记 |
| --- | --- | --- | --- |
| 1 Sou Material | 完成 | 03e6de7c1 | 01-sou-material.md；10项行为/真实回环测试通过 |
| 2 内部动画 | 完成 | 53c8fe62f | 02-material-animation.md；双入口及真实资源3项通过 |
| 3 aux消费者 | 完成 | 本项独立提交 | 03-aux-consumers.md；10aux+3MEK+2WAZ+1异常契约通过 |
| 4 叠加资源交付 | 待执行 | 尚未提交 | 04-layered-assets.md |
| 5 Misaki动态定位 | 待执行 | 尚未提交 | 05-misaki-emission.md |

实施裁定：沿现有链修复，不改变Sou武装取舍；武装内容裁剪属于未授权第9项。先使既有武装与正确Material同步，实际消费编号交由资源验证检查。若错误，代价是后续需要另行调整内容清单，不以隐藏问题或修改EXE绕过验证。

实施裁定：新worktree中JSON输入目录为原工作区目录junction，仅供读取；测试不得写入这些目录。完整输出独立生成，避免污染用户既有结果。

实施裁定：Misaki的Followup上下文公共转换map可能为空，优先当前公共源/转换视图，再沿继承链找到首次公共准备视图，以身份和appendPlan定位，不从私有selected猜公共编号。

服务状态：三个子任务曾429/503中断。Root接手第一项；第二项和第五项已重新分派，代码互不重叠，Maven测试串行。

第一项只读复核：等级切片、单条幂等、三个资源数组深复制及AI预检查均与计划一致；最新Sou真实UPPER动画族断言与二进制重解析通过。第二项red三项均expected40/actual0，green三项全部通过。

实施裁定：以新闭合原生消费者为准，ANGLE首项1/2按位置字典，3按OBJECT；MEK AI不随POS根转位置。此前ANGLE全局9→8结论撤回。代价是旧调查标签/断言需校准，但避免把本来正确的HIT位置改坏。
