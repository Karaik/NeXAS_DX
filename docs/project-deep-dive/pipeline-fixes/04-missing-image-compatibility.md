# 第4项：缺图兼容纠正

日期：2026-10-08。用户反馈：原版有很多SPM引用的图片并不存在，不能新增“搜索不到对应图片就报错中断”的全局机制。

根因是把静态引用当成运行必需证明。公共输出扫描整个追加段、两Importer收集SPM图名，并在LayeredAssetResolver找不到图片时抛错；AssetDeliveryValidator在动画范围检查后又硬性要求物理图片存在。旧runner因smoke_007图片中断因此是本轮检查策略的回归，不能继续描述为成功发现了已证实必需依赖。

原生核对：76AFA0第299～339行只读取imageData名称、清空纹理句柄，未打开图片，最终正常返回；4042D0经766CB0/7952E0在使用chip时才加载；746740第40～57行打开失败返回0，异步762BC0第59～63行不将读失败升级fatal。本轮未证明所有绘制后端空句柄都安全，结论限定为静态缺图不能直接判SPM/资源包非法。

修改：图片供体未找到时保留原引用、记录独立unresolvedImageReferences并返回；动画检查记录缺图，不升级到致命missingAssets，也不登记不存在的复制文件。存在的图片继续按原来源复制/继承，不生成空图，不借其他游戏同名素材。二进制/索引结构错误、明确必需数据文件、原有MenuOverride自有素材约束仍由相应规则验证。音频实体缺失检查没有一律关闭；后续重跑对明确禁用的无声占位项另按原生规则处理，见下文。

另外修正媒体查找命名空间：媒体根按既有平铺目录契约读取PNG/BMP/OGG/WAV，不递归把其mod/备份子目录、Hell.bin或其他索引文件混成原版媒体加载层。真实runner越过smoke_007后发现的Hell.bin重复，是媒体扫描的另一处错误阻断，不能当作图像资源冲突。

回归：双Importer缺图、公共SPM输出缺图保留图名、未使用imageData缺图、有效动画缺图仍可真正封包/解包并核对内容、不借同名旧原版代替缺BHE供体，以及无关嵌套目录不影响平铺媒体查找。旧缺图硬失败断言改为非致命审计；结构损坏/禁用/越界的负例保持。

真实runner继续推进时，还暴露另一条过强的不变量：Tsukuyomi Material[0]三类外层长度208/55/43，目标GRP数量143/52/32，超出范围部分全部为空。原生5F2EB0按文件声明的组数及每组条目数读取，没有要求等于全局GRP数量。因此去掉长度完全相等断言，保留每个实际条目的索引检查。回归同时证明空预留组可继续、在该组放入越界实际引用仍失败。

继续推进真实runner时，Yuri MEK原有语音表引用BatVoice[32][2]的禁用空项，被初版检查误当缺音频。原生666150取到空项后，667F30直接返回0，不要求声音文件。增加对应回归后允许明确禁用的无声占位，仍检查实际启用音频的索引和载荷。该修正与空预留组一样，只撤回已证明错误的“声明就必需”推断。

定向回归当前共71项全部通过，无失败/错误/跳过，覆盖原五项及本轮纠正；没有用忽略错误来通过测试。未做实机验证，不能据静态缺图审计宣称所有演出图像完整。

定向命令：`mvn -q "-Dtest=LayeredAssetDeliveryTest,ImportStaticAssetsStepBheOutputTest,RebindMekStepBheResourceRebindTest,AuxConsumerRebindTest,MisakiCustomizerTest,SouCustomizerTest,RebindWazStepCommonReferenceTest,BheInfoCollectionTermConverterSemanticTest#invalidObjectPositionAuxIndicesFailClosed" test`。日志已保存到主目录`target/bhe-pipeline-validation-20261008/missing-image-final-tests.log`，Surefire报告合计71项、0失败、0错误、0跳过。

中间完整runner：`mvn -q "-Dtest=TestBheByJinkiRunner#testRunBheByJinki" "-DargLine=-Xmx6g" test`，日志`target/bhe-full-runner-image-correction-final.log`。JINKI/AKAO、Tsukuyomi、Yuri、Freja四阶段已返回；在Nagi验收时因`Nagi.mek -> VoiceInfo.table[13][0][0] -> BatVoice[34][101]`越界/禁用停止，退出码1。原smoke_007缺图已不再阻断。

继续只读核验Nagi原始bytes：BHE源默认组4、输出正确映射为34；FCEND/超技後的DEFAULT/通常槽均保留`[-1,101,50]`和`[-1,102,50]`。源、目标Nagi语音组都只有60条，101/102在源就越界；原生666150第12～15行对此返回0，667F30第28～30行不播放并返回0。因此MEK语音表内的越界语音号记`unresolvedMekVoiceReferences`并保留，不再作为整包非法条件。此处只调整MEK静态语音表，仍验组重绑、实际有效启用语音载荷及WAZ/Material索引。新增真实生成MEK/GRP回归先复现硬失败再通过，未猜改源语音号或借另一角色的语音。

最终runner重跑日志`target/bhe-full-runner-image-correction-verified.log`：JINKI/AKAO及Tsukuyomi～Motoki九个BHE角色阶段全部返回，Sou已到最终Pack验收，因地图资源`bhe_mapB6_ES02_ob01.spm`重解析失败停止，退出码1。BSDX SpmParser在offset5561试图读取下一int，但余下0 bytes。输出与外部BHE供体`mapb6_es02_ob01.spm`的SHA256相同（`8EDC72AF2B580A6A56FF3BD2552153423B4AE46C140BD111539CA1C22BC7BD6C`），地图物化流程原样复制源bytes。因此这里只确认当前BSDX解析器无法读该地图SPM，不能据此断言本次转换把文件写坏或原资源无效。地图SPM格式兼容需另行诊断；本次未改地图转换/解析器，也未跳过该检查或报告整链/实机通过。

限定只读复审未发现本次纠正的阻碍提交问题。按用户要求将本次纠正归入第4项，五项全部重新提交，随后统一迁回develop；不保留codex分支。原六提交末端为fc5d39412，历史已保存为主目录`target/bhe-pipeline-validation-20261008/history-before-cleanup.bundle`。最终代码与已通过71项定向回归的版本保持一致；全部历史日志及Surefire报告已迁至该验证目录。当前提交记录见progress.md及Git日志。
