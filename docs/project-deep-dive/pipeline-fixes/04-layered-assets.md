# 4. 叠加资源、最终字节与PAC交付验证

## 2026-10-08 缺图策略更正（优先于以下旧实现记录）

用户指出原版SPM含有很多不存在的图片引用。此前把SPM声明/公共追加段静态引用等同于运行必需，并以缺少smoke_007供体中断流水线，属于新增门禁的错误判断，不是已经证明的资源包故障。现在复制阶段和最终动画检查均只将普通缺图记入独立`unresolvedImageReferences`，保留引用继续处理与封包，不写入致命missingAssets，也不声明一个没有落盘的copiedImageFiles。结构、明确必需数据文件及既有菜单自有素材约束另行检查。

原生76AFA0读imageData只保存名称/置零纹理句柄；4042D0在绘制时才尝试加载。746740打开图片失败返回0，不能用静态图片不存在推出SPM结构非法或必然中断。没有据此宣称所有缺图绘制都安全或实机演出完整。详细修正和71项定向回归见04-missing-image-compatibility.md。本次纠正已归入第4项提交；以下60/63项及旧runner停止记录保留为历史，不代表当前缺图策略。

## 初版实现与验证历史

日期：2026-10-08。有效加载层按本轮输出、前角色/JINKI输出、原版分类资源查询。独立bsdxStaticAssetRoot只提供PNG/BMP/OGG/WAV，不参与GRP索引；初版要求所有新BHE媒体复制进本轮输出，现已纠正为有供体则复制、普通缺图仅审计，合法原版/前层媒体可继承并记录来源。

双Import在落盘后、双Pack在入口处（含pack=false）执行AssetDeliveryValidator。验收实际二进制，禁止以可变baseline DTO假装载荷存在：声明文件缺失、重解析失败、禁用group、WAZ技能/SPM动画/pattern/page/image索引错误和必需媒体缺失均带引用链失败。检查迁移主WAZ完整技能及武装显式依赖闭包，继承前角色与JINKI迁移根；不遍历所有未消费原版技能。

Material读取规则按有效MekaGroup身份：非KOU每个存在武装一条，KOU category0三条，空武装不消费；额外未消费EOF尾表仍重解析但不把它当有效资源引用。AI检查武装存在；玩家装备14列（含第0列）和有效Weapon.dat候选检查0..152，不按MEK总行数截断。CEventWazaSelect负group取当前MEK的自身WAZ，负skill不消费；Change List2仅模式1/2验证WAZ目标，模式0/3按phase数据保留。

原版WAZ/SPM池的完整DTO未变化时不重新生成、不带入增量包；变化或已继承的文件仍保留。PAC真实解包到独立临时目录后，按相对路径集合和SHA256核对内容，成功后才packed=true。临时验证目录位于本次输出父目录并可回收，不修改原版安装。

音频复核中另闭合Naoto错误：MEK语音记录第一整数为显式BatVoice组覆盖，第二整数为组内voiceNo。原生65C020仅在覆盖负值时回退头部version。源Naoto23条覆盖组8对应NAOTO，目标原版组8对应Gilberto；两份BHE RebindMek现在与默认组一样映射非负覆盖组。最终语音表依据`voiceType>=0 ? voiceType : version`验组、条目和媒体，不按角色名称猜默认组。

回归：旧实现先复现原版图片误missing、缺依赖仍可报告pack=false成功、坏SPM/动画索引漏验；真实PAC往返原流程作为正基线。增加KOU、未消费余尾、Weapon.dat第153候选、首装备槽、AI缺武装、迁移根累积、Change/WAZ哨兵和独立MEK语音表用例。Naoto显式覆盖另先复现两条路径的8未映射以及真实23条覆盖值错误，修后通过。

最新定向命令：`mvn -q "-Dtest=LayeredAssetDeliveryTest,ImportStaticAssetsStepBheOutputTest,RebindMekStepBheResourceRebindTest,AuxConsumerRebindTest,MisakiCustomizerTest,SouCustomizerTest" test`；共60项，0失败、0错误、0跳过。包含实际PAC压缩、解包、集合/内容比对和真实MEK/WAZ生成重解析。

最终扩大复核加入RebindWazStepCommonReferenceTest两项和既有invalidObjectPositionAuxIndicesFailClosed一项，共63项全部通过，命令与结果见progress.md。这是纠正前的测试结果，未发现缺图门禁的错误假设；本轮已用保留缺图引用的正例及真实封包回归修正该缺口。

实际多角色runner尝试：`mvn -q "-Dtest=TestBheByJinkiRunner#testRunBheByJinki" "-DargLine=-Xmx6g" test`。JINKI阶段完成；Tsukuyomi公共资源输出因`bhe_smoke_007_0001.png`的供体`smoke_007_0001.png`缺失而失败。已查询BHE、BSDX和JINKI媒体加载目录，均无该文件；其他游戏目录同名文件未作为供体。此轮未交付声称完整可运行的PAC，未实机验证。完整runner日志保存在本worktree `target/bhe-full-runner.log`，所有输出仅落在本worktree的out目录，用户原工作区原版资源不变。

范围限制：未对动态Term跳转的全部运行条件求值，静态资源验证不等于实机行为验证；未实施公共技能稀疏追加或角色6～10项配方。初版“供体缺失必需失败”的图片规则已撤回；不借其他游戏同名素材，也不以EXE旁路冒充交付成功。
