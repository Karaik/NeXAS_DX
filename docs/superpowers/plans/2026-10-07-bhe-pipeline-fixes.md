# BHE 多角色流水线五项修复实施计划

> For agentic workers: use superpowers:subagent-driven-development or superpowers:executing-plans to execute each task. 用户已授权分别实现第1至5项、分别提交、保留笔记；不再等待实施确认。

**Goal:** 修正已确认的Sou Material、公共动画偏移、aux消费者字典、叠加资源交付验证与Misaki动态定位问题，形成五个独立提交。

**Architecture:** 保留TestBheByJinkiRunner的JINKI V2→Tsukuyomi→Followup链。共通行为放入转换/重绑/资源验证层，角色例外放入现有Customizer。资源验收针对有效加载层，PAC仅承载变化文件。

**Tech Stack:** Java 17、Maven、JUnit 5、Jackson/Hutool、现有BSDX/BHE二进制服务。

**Spec:** 用户确认的1～5项提案；调查证据见主工作区docs/project-deep-dive/pipeline-mod-behavior-review-2026-10-07.md。

## Global Constraints

- 所有新建或修改文件使用UTF-8无BOM、CRLF。
- 独立worktree为C:/Users/30250/.codex/worktrees/bhe-pipeline-fixes/NeXAS_DX，起点c6c4e039916b3b4cba4b86474f936d586db670bd。
- 用户原工作区的CLARIAS修改、原版资产与既有生成目录不改动。
- 新资源输出和round-trip均使用JUnit临时目录或本worktree独立输出；禁止运行会清理共享JSON输入的默认测试套件。
- 各项先写行为回归并观察预期失败，再修生产逻辑，验证后提交；每项笔记进入对应提交。
- 不实施第6～10项的角色演出/运动/Hit标志/内容裁剪/公共稀疏追加方案。
- 不把MEK总行数机械限制为153；检查实际进入武装参数bank的编号0..152。超出限制的未消费资源池条目本身不判错误。
- 五项提交顺序固定为1、2、3、4、5。额外修正并入所属项提交，避免无关提交。
- 用户2026-10-08反馈更正：普通SPM图名/静态动画引用可能指向不存在的图片，不作为硬失败依赖。图片缺失只审计、保留引用、继续封包；第4项旧“图片缺失必阻断”假设撤回，见04-missing-image-compatibility.md。按用户要求将纠正归入第4项，五项全部重新提交；旧历史仅保留在备份分支。

## Task 1: Sou武装、等级与Material同步

Files: SouSkillGlitchFixer.java；必要的Sou Material选择helper；SouCustomizerTest.java及真实资源回归；docs/project-deep-dive/pipeline-fixes/01-sou-material.md。

- [ ] 现有Sou/Misaki Customizer测试通过，作为基线。
- [ ] 先写测试：源STRAIGHT的Lv3为tail[2]，UPPER的Lv3为tail[5]；过滤dummy/SCRIPT后仍按武装身份选择，非category0为单条，单条布局不压缩，重复执行不再次切片。
- [ ] 运行 `mvn -q "-Dtest=SouCustomizerTest" test`，确认Material对应断言失败。
- [ ] 从过滤前武装顺序建立源Material切片，按最终等级/武装清单同步生成尾表；选中DTO与重绑DTO使用各自索引空间的记录，不共享可变数组。
- [ ] 保留已有Lv3与热量行为，维护AI旧→新编号；缺失引用明确诊断，不让被删除武装索引指向另一武装。
- [ ] 真实Sou转换后逐武装检查尾表，BSDX生成/重解析；核对其他九角色单条布局不受影响。
- [ ] 笔记记录根因、读取规则、测试命令/结果、153实际消费边界和未跑实机项。
- [ ] 精确暂存本项文件并提交 `fix(bhe): align Sou weapon materials with selected levels`。

## Task 2: MEK内部公共动画映射

Files: tsukuyomi/graft/RebindMekStep.java；followup/graft/RebindMekStep.java；RebindMekStepBheResourceRebindTest.java及双路径回归；02-material-animation.md。

- [ ] 先写两条生产路径的非零动画基址测试，手写期望：[0,10,41]+40→[40,50,81]；私有映射不加公共偏移，负哨兵保留，多个源group合并正确。
- [ ] 验证测试在旧代码失败。
- [ ] remapSpriteGroups逐个内部动画号调用resolveSpriteActionGroupIndex，继续映射外层SpriteGroup。
- [ ] 真实资源测试改为按最终动画身份断言，不保留错误的“内层源数组不变”断言。
- [ ] 运行覆盖双路径、空项、哨兵及真实资源的定向测试，记录笔记并提交 `fix(bhe): rebind animation indices inside MEK materials`。

## Task 3: aux按真实消费者转换

Files: BheInfoCollectionTermConverter.java；BheInfoCollectionObjectGraphRewriter及其调用方；共用ConsumerPurpose类型；MEK/WAZ aux测试；03-aux-consumers.md。

- [ ] 先写真实字典测试：ANGLE首项3的对象9(HIT_WAZA)→目标OBJECT8(HIT)；ANGLE首项1/2及POS位置分支对象9→按OBJECTPOS(HIT)保持位置语义。MEK AI/条件即使根POS也按OBJECT，未读取的第二项保留。
- [ ] 覆盖顶层/嵌套WAZ、条件和MEK AI实际调用链；未知上下文与越界索引给明确诊断。
- [ ] 观察原转换使用同一位置字典导致预期失败。
- [ ] 传入明确消费者用途，不靠随意字符串猜字典；按事件槽/collection使用位置选择OBJECT或OBJECTPOS及第二级表。
- [ ] 保留主Term链现有语义转换，附加CENTER/TOP损失记录；本项不实施高度配方或全局条件降级重写。
- [ ] 运行定向语义回归和真实WAZ/MEK转换检查，记录笔记并提交 `fix(bhe): map auxiliary references by runtime consumer`。

## Task 4: 叠加资源解析与最终交付门禁

Files: Tsukuyomi/Followup ImportStaticAssetsStep、PackUpdatePacStep和主线调用点；共用资源来源/产物验证helper；@TempDir资源与封包测试；04-layered-assets.md。

- [ ] 先写原版继承图片不在BHE目录/增量PAC时仍有效的测试；普通图片在各层均缺失时只审计并继续，明确必需的数据文件/结构错误另行失败。
- [ ] 先复现现有误报/继续成功路径；测试使用本地临时目录与真实资源DTO，禁止用真实安装目录作输出。
- [ ] 按原版→继承前角色→本轮覆盖查资源；保留来源层。图片/音频可继承原版，有供体的本轮新资源落盘，普通缺图保留引用并审计。
- [ ] 对生成MEK/WAZ/SPM/GRP重解析，检查有效group、skill/action索引、相关Material与必需媒体；正确处理负哨兵、禁用项和原版继承。
- [ ] 普通缺图不写入致命missingAssets；其他已明确必需依赖缺失及解析/结构错误带引用链失败。
- [ ] PAC在独立目录打包并解包检查内部相对路径集合和文件内容；无关原版文件无需加入PAC。
- [ ] 真实多角色链独立输出验证；对原有未修问题明确记录具体阻碍，不标实机通过。
- [ ] 笔记记录验收范围、媒体来源和实机限制，提交 `fix(bhe): validate layered assets before delivery`。

## Task 5: Misaki 999按身份和计划定位

Files: MisakiSkillGlitchFixer.java；MisakiCustomizerTest.java；05-misaki-emission.md。

- [ ] 先写两个不同appendBase的相同源技能测试；488/491位置放无关技能保证不会误改；真实源技能双名称/结构匹配。
- [ ] 确认旧代码只对固定编号修复而失败。
- [ ] 在commonProjectile资源内按床召喚/あたためる身份与Unit4异常结构找到源技能，经公共追加计划解析最终目标；验证唯一命中和999原值。
- [ ] 修正最终baseline公共WAZ并验证落盘/重解析的Unit4为1；不在selected私有map猜公共技能。
- [ ] 运行不同base、默认base、重复执行与无关同编号技能测试，更新笔记及指南，提交 `fix(bhe): locate Misaki emission fixes through append plans`。

## Final Verification

- [ ] 五个提交均包含对应行为代码、回归测试和中文笔记，提交顺序正确。
- [ ] 汇总运行本轮定向回归；检查diff/编码、无无关暂存、原工作区不变。
- [ ] 最终独立生成物按有效加载层校验；PAC与输出集合一致，未执行实机验证明确注明。
- [ ] 交付五个hash、笔记入口和实际测试范围；不推送或修改原版安装。
