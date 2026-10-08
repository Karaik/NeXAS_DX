# 五项修复进度

2026-10-08更正：用户指出原版SPM常有不存在的图片引用。第4项初版缺图硬失败策略已撤回；smoke_007停止属于新增检查误判，不能当作已证明的输入缺陷。现在普通缺图仅存独立审计、保留引用并继续封包。媒体平铺命名空间、Material空预留组、明确禁用的无声占位和MEK原源越界语音号的错误严格判断一并修正，详见04-missing-image-compatibility.md。本轮定向回归为71项通过；60/63项旧结果仅作为验证过程记录。

用户随后要求五项全部重新提交。纠正归入第4项，当前分支形成五项提交；原六提交历史备份于`codex/bhe-pipeline-fixes-before-rewrite-20261008`（末端fc5d39412）。代码和测试不因历史整理发生变化。

本轮中间runner：JINKI/AKAO、Tsukuyomi、Yuri、Freja阶段返回，Nagi验收因`VoiceInfo.table[13][0][0] -> BatVoice[34][101]`越界/禁用停止，退出码1；日志`target/bhe-full-runner-image-correction-final.log`。Nagi原源已含101/102而组内仅60条，原生越界返回0不播放；该MEK静态引用的硬失败也已改为独立审计，不改其原值。

本轮最终runner：JINKI/AKAO及Tsukuyomi～Motoki阶段全部返回，Sou最终Pack验收因地图`bhe_mapB6_ES02_ob01.spm`重解析失败停止，退出码1；日志`target/bhe-full-runner-image-correction-verified.log`。输出与BHE供体SHA256相同，当前BSDX解析器在offset5561读尽文件后还要求4 bytes；未证明原资源非法或输出损坏。缺图纠正已通过71项定向回归和缺图PAC往返；地图SPM解析兼容尚需独立诊断，不能宣称完整链/实机通过。

计划：docs/superpowers/plans/2026-10-07-bhe-pipeline-fixes.md。

起点：c6c4e039916b3b4cba4b86474f936d586db670bd。分支：codex/bhe-pipeline-fixes。

用户已授权实现1～5、分别提交、留下笔记。原工作区的无关修改保留。

| 项 | 状态 | 提交 | 验证/笔记 |
| --- | --- | --- | --- |
| 1 Sou Material | 完成 | ee96ea44e | 01-sou-material.md；10项行为/真实回环测试通过 |
| 2 内部动画 | 完成 | 33ed8944d | 02-material-animation.md；双入口及真实资源3项通过 |
| 3 aux消费者 | 完成 | e422d9cb9 | 03-aux-consumers.md；10aux+3MEK+2WAZ+1异常契约通过 |
| 4 叠加资源交付 | 完成，含缺图及原生容错纠正 | 本项提交 | 04-layered-assets.md、04-missing-image-compatibility.md；现行71项定向回归 |
| 5 Misaki动态定位 | 待重提 | 下一项提交 | 05-misaki-emission.md；16项通过 |

实施裁定：沿现有链修复，不改变Sou武装取舍；武装内容裁剪属于未授权第9项。先使既有武装与正确Material同步，实际消费编号交由资源验证检查。若错误，代价是后续需要另行调整内容清单，不以隐藏问题或修改EXE绕过验证。

实施裁定：新worktree中JSON输入目录为原工作区目录junction，仅供读取；测试不得写入这些目录。完整输出独立生成，避免污染用户既有结果。

实施裁定：Misaki的Followup上下文公共转换map可能为空，优先当前公共源/转换视图，再沿继承链找到首次公共准备视图，以身份和appendPlan定位，不从私有selected猜公共编号。

服务状态：三个子任务曾429/503中断。Root接手第一项；第二项和第五项已重新分派，代码互不重叠，Maven测试串行。

第一项只读复核：等级切片、单条幂等、三个资源数组深复制及AI预检查均与计划一致；最新Sou真实UPPER动画族断言与二进制重解析通过。第二项red三项均expected40/actual0，green三项全部通过。

实施裁定：以新闭合原生消费者为准，ANGLE首项1/2按位置字典，3按OBJECT；MEK AI不随POS根转位置。此前ANGLE全局9→8结论撤回。代价是旧调查标签/断言需校准，但避免把本来正确的HIT位置改坏。

第四项复核：原版媒体无需重新打包；有供体的新BHE媒体落盘，图片供体不存在时保留引用并审计。必需声明产物删除、SPM禁用/动画越界、AI武装缺失、菜单Weapon.dat候选153、WeaponEquip第0槽及KOU三等级读取均纳入回归。CEventWazaSelect负group取主MEK自身WAZ，负skill不消费，不能以新门禁拒绝合法哨兵。

实施裁定：第四项音频验收发现Naoto真实23条MEK voiceType=8为显式BatVoice覆盖组，原生65C020负值才回退默认头部version。两份BHE重绑原样复制覆盖值会播目标Gilberto。因此使用已有BatVoice映射补齐显式组重绑，作为本项音频正确性必要修补；负值和组内voiceNo保留。代价是原输出bytes变化，真实声音身份校验优先于复刻旧错误。

真实链尝试：JINKI V2完成，Tsukuyomi在公共媒体输出因smoke_007_0001.png缺失停止。三层媒体目录均无该供体；其他游戏同名资源不作为替代。没有把失败产物标为成功，没有实机验证。

最后定向复核（纠正前历史）：2026-10-08执行五项回归及既有MEK/WAZ重绑和异常契约，共63项全部通过，0失败/错误/跳过。当时runner因上述缺图门禁误判停止；该停止不能作为输入不完整的证明，也没有证明十角色整链或实机通过。

独立复核：第5项converted-only多发射节点别名幂等问题经真实结构回归修正；第4项负WAZ哨兵、MEK语音覆盖组和默认组两个重要问题经原生消费者核验、红绿回归和限定复核消除。

原五项完整改动复核（纠正前历史）：当时36个改动文件均为UTF-8无BOM、CRLF，逐项01～05笔记在对应提交中。当前重建后的文件及提交边界另按Git比对验证；主工作区HEAD及CLARIAS未提交修改保持原状。

当前验证命令：`mvn -q "-Dtest=MisakiCustomizerTest,LayeredAssetDeliveryTest,ImportStaticAssetsStepBheOutputTest,RebindMekStepBheResourceRebindTest,AuxConsumerRebindTest,SouCustomizerTest,RebindWazStepCommonReferenceTest,BheInfoCollectionTermConverterSemanticTest#invalidObjectPositionAuxIndicesFailClosed" test`。纠正后退出码0，71项，无失败/错误/跳过；历史整理后的生产代码和测试须与该已验证版本逐文件一致。
