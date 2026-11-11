# Nagi六新增：运行范围与静态风险（2026-11-06）

本文件固化只读准备，供Naoto之后恢复Nagi验收。源码、资源身份及原生路径已核；这里不登记当前8efe包的Nagi运行通过。两个静态风险不是已复现故障，不据此擅自修复。木桩空防仅证明该条件下有限结束，不能完成防反／反射核心验收。此次仅新增本文件，没有操作UI／进程、修改源码、构建或提交；runtimeValidated仍false。

## 当前包与实际索引

绑定`target/bhe-playable-builds/run_20261105_193350_342_bd794139`，最终资产位于该run下`sou/sou_assets_20261105_193550_582`。PAC SHA256为`8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641`，EXE为`1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`；包身份来自receipt，本轮不冒称重hash大型PAC。

本轮直接重算资源：

| 文件 | 字节数 | SHA256 |
| --- | --- | --- |
| Nagi.mek | 67379 | 825aad1650837df4b80fec7846efe68a68c6f8bb7b35349317cf2f993064c00a |
| nagi.waz | 163105 | bbdc7d2f1f63791cf1c30142234ceaa37ccbc3082c6f2c39dc5bd72fe8129532 |
| tama04.waz | 2025000 | da3a489799782d197717b1bf922049d7f4653693886ace83229c699fc2aec644 |

Nagi两hash与保存的`src/test/resources/bhe-runtime-evidence-20261030/records/audits/nine-six-variants-130704-v2/audit.json.json`内text审计一致，已核角色及roundTrips行。对应Meka107、主WazaGroup114、MEK24项、WAZ35项。字节同一性只支持静态身份，不跨包继承旧实测。

| MEK／主WAZ | 名称／英文身份 | 成功条件与时刻 | 收尾与候选位置 |
| --- | --- | --- | --- |
| 18／114:29 | 瞬間ディフレクト／DX_QUICK_DEFLECT | P0 5～19查ARMOR，5f甲值4；成功P1 Hit19～35、基础150、地面控制字段60/100 | 成功P1 0f清甲、79结束；落空20清甲、39结束。先证来袭，再试成功Hit后接19 |
| 19／114:30 | 斬上げ・接続／DX_NAGI_CLOSE_LAUNCH | P0 0～23目标平面距离≤135则NEXT；P1 Hit7～21、基础60、控制20/40 | P0 23停速、24远距END；P1 22普通门、32结束。优先独立起手及接20 |
| 20／114:31 | 乱れ突き・三連／DX_NAGI_THREE_THRUST | P0/P1均5f去P2；P2 0/3/6发0:413，各基础20、控制4 | P2 8→P3；P3开门、18去盾结束W18，W18 9f结束；无持续重入 |
| 21／114:32 | 転移突き・追撃／DX_NAGI_TRANSFER_LINK | P0 14f发5:474、5:311；474先零伤探测，真Hit在其P2 6～9、基础100、控制30/50 | 父32普通门、39进P1，P1 16结束。探测／发射不代替伤害 |
| 22／114:33 | 盾打ち・反攻／DX_NAGI_BASH_CONFIRM | 地面→P1，14去P3；空中P2 8～90接地进P3、91否则解盾；P3 Hit4～7、基础200、控制30/50 | P3 8～9拟确认后跳新19／W30；否则10门、24去W18。确认时序有风险 |
| 23／114:34 | 単発リフレクト・解放／DX_NAGI_SINGLE_REFLECT | P0 5～17查ARMOR，5f甲值6；成功P1一次发4:317后进P2 | 落空18清甲、19门、32去W18；成功P2 14清甲、15门、30去W18；子体等待有降级 |

主依据：[NagiCustomizer.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/nagi/NagiCustomizer.java)第64～93行、[NagiAdditionalVariants.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/nagi/NagiAdditionalVariants.java)第46～145行，以及[NagiSixVariantsTest.java](../../../src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/nagi/NagiSixVariantsTest.java)的assertBehavior合同。热耗MEK字段依次12／14／12／18／28／16，不能直接当实测raw净耗。

## 真正来袭与回收合同

18和23父图成功条件同为`root0/types[2,6,6]/aux[0,0]`，即ATTACK/ARMOR。[66B0F0.c](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/66B0F0.c)第63～65行要求所选对象为机体、最近受击对象非NULL、三项装甲总量仍大于0；没有近战／射击类别筛选。因此不能把技能名称当攻击类别白名单。

装甲4／6是slot36数值，actual raw还受原生模式处理，应采样确认，不叫无敌。`622B20.c:305～369`显示装甲会消耗，仍有余量时走装甲受击分支并缩减伤害；成功防御不要求HP完全无损。先用弱单次攻击，避免一击耗尽装甲导致条件不成立，再做强攻击／多段／窗口外对照。

18优先弱近战，核来袭→装甲仍余→P1真实反击→清甲。23另外核真实来袭弹体、成功段、4:317行为与回击；成功动画不等于反射兼容，当前不能列已确认的可反射弹种。弱射击候选尚未核完，不预称机枪、导弹、激光全部有效；也不凭原说明猜捕获／反射协议完整。

木桩对照只记录18到39结束、23在18清甲并经32/W18收尾。空防零伤不是核心成功，也不等同安全失败。缺图片继续nonfatal审计。

19～22每个新阶段入口恢复G50、slot27/28倍率100和armor0，删除Charge及旧FF；没有新增slot38无敌。22是主动盾打，不因名字含盾获得额外无敌。其空中P2一次写Vz0后仍需原生接地或91f超时，不能称强制落地。

20四阶段均写scalar0，删旧预算重入／HC消费，P3速度0后去W18；三个0:413是子攻击，不增加装备使用数。21保留5:474探测／条件退出，须核探测→真Hit→回收，父结束不等于子体已归零。

18只缩guard并在成功P1补armor0，没有统一替换原物理事件；成功、空放、取消后的G／倍率分别验。23三阶段恢复G50／倍率100，失败P0 18清甲、成功P2 14清甲，无旧反复受理。

gate辅助先写普通门1，随后写值3；`610300.c:126～148`及`61B530.c`说明值3设置SD门，不在该分支直接清普通门，不能把普通门说成必然只存一帧。18落空20～39仅值3，没有前置普通门1，不据此假定普通ABC可取消。

稳定地面收尾读取active=-1、Z/Vz0、Graw51200、两倍率100、armorWaza／贡献缓存、protectionFlags、TAMA/SCRIPT；20另核scalar0。其它招没有依据要求所有通用变量清0。装甲量、无敌掩码、HP变化和对象回收分别判定。

## 静态风险一：23子体等待条件降级

BHE Tama04源206／HADOUHOU10／◆凪：単発リフレクション对应当前4:317。源P0 0～100的OR出口是root CPU特殊动作3，或root NOT攻击。[BheInfoCollectionTermConverter.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/BheInfoCollectionTermConverter.java)第177～182行把前者降为普通ACTION_ATTACK。

当前`tama04.waz` offset1930278实读Change头`[0,100,2]`，两个集合为：

| 集合 | root／types | params | aux1 | NOT |
| --- | --- | --- | --- | --- |
| 第一项 | 0／[2,1,12] | [3] | [3,0] | 0 |
| 第二项 | 0／[2,1,12] | [] | [3,0] | 1 |

目的NEXT、尾mode1。DX `66B6E0.c`的ACTION_ATTACK不消费参数3；[66A730.c](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/66A730.c)第44～64行处理OR，[6685D0.c](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/6685D0.c)第17～24行处理NOT。root有效时成为“攻击中或不在攻击中”，原P0等待协议已丢失；root NULL的不可判定结果不擅称恒真。

主W34发4:317的真实引用在`nagi.waz` offset158737。子体另有P1 12f准备、P2 0～21攻击40及PARENT_ALL退出；12f须相对实际进入P1计，不混成父成功后固定12f。以上证明静态语义降级，不是已复现崩溃／伤害失败，也不证明全部弹种不可用。下一步先记录真实来袭后317各阶段、root/parent、回击和消失，再决定局部适配；本记录没有授权或实施代码修改。

## 静态风险二：22在Hit窗之后读取当前Hit

新P3 Hit为4～7f、初始次数1；确认在8～9f。当前`nagi.waz` offset156689的头为`[8,9,1]`；156701集合原字为`[0,4,2,6,0,0,1,0,2,0,0,0,0]`，即ATTACK/MEKAHIT==0；156753目的为`[16,1,1,2,114,30,0,0,0]`，跳新19的W30。

它读取“当前攻击事件剩余对机Hit次数”，不是独立WAZAHITCOUNT>0。[66B0F0.c:24～31](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/66B0F0.c)取slot41当前事件；[60E6D0.c](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/60E6D0.c)第12～15行依赖当前索引；[63A7D0.c](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/63A7D0.c)第32行先置索引-1，仅有效帧窗选回。8～9已在4～7之外，存在读不到Hit或比较值未由此分支更新的风险。11/11完整生命周期复核显示，Change处理、时钟推进与Hit刷新分属不同遍，不能仅凭slot41/71的文本排列判断整帧先后；当前Hit为空时仍可能沿共享比较缓存继续求值。详见[生命周期复核](nagi22-hit-confirm-lifecycle-20261111.md)，不将此风险静态判作恒假或不可达。

不预判“命中必不派生”或“落空必派生”。最小对照：真实命中后不取消，看8～9是否跳114/30；同距离偏移／远距落空不得跳；改变前技／此前条件状态再重复，排除共享比较残值。内部跳转应仍是weapon22，没有额外START19或单独扣19热费。静态测试结构断言不代替此判定。

## 近／远／落空与前后招

19距离条件在`nagi.waz` offset136270，头offset136258为`[0,23,1]`，types[2,3,6,2]、param135。DX PARAMCOMP index6是LENGTH；`66B9C0.c:99～111`只算平面距离，index7才加高度差。旧规格“三维距离≤135”不能作为运行判断。近XY但Z差大的目标可能进P1后落空，不等于距离守卫失败。

先测19近距、明显超135且来不及接近、近XY高低差三种；20短按／长按／落空最多一轮三子体；21探测命中、探测落空、目标移开和父提前取消；22地面命中／落空、空中实际接地／未接地超时。没有真Hit时不把后继START或动画算成功链。

优先连段候选是19→20，以及22真盾打→内部19→外部20；后者先排除确认风险。18成功反击控制60/100为接19／20提供依据，仍待同靶控制证据。20子体控制仅4，20→21先作压制候选；21先独立证明真Hit，再按剩余控制选择19／20。内部WAZ跳转不另算装备武装。

## 正常训练的敌攻击入口

已有原生链支持用正常UI启用敌AI，无需写敌actor或伪造第二套输入：

- `datBsdxJson/Button.dat.json`第156项help为`敵機の行動方法を変更します`，四选项按钮175～178；[4C08D0.c:72](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/4C08D0.c)中0xAF～0xB2调用5FD490。
- 原生72EC0D／72EC70／72ECD7／72ED3E依次绑定`0x874EEC`和值0／1／2／3。**第四项value3启用攻击AI；11/10已在Naoto正常训练UI确认显示名为CPU。Nagi仍须另存本角色正常模板。**
- [5FD490.c:263](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/5FD490.c)的3路径同时开启86CD34／86CD2B并加载原机体AI；[6604C0.c:190](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/6604C0.c)在86CD2B为1时执行攻击AI选择660120(kind2)。值0木桩不能验防反核心。
- [446140.c:34](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/446140.c)保存至Replay初始seed[6]；[4492D0.c:84](../../../src/main/resources/ida-reverse/bsdx/export-for-ai/decompile/4492D0.c)恢复并应用5FD490，179行恢复随机种子。已有015／017是seed[6]=0，不能借作主动来袭模板。

流程：root正常UI设Nagi／敌机，选敌行动第四项并保存新模板；只读核seed[1]=107、seed[6]=3，保留头和前两块，仅生成玩家普通输入。先短轮识别敌方真实招式与来袭时点，再使18的5～19或23的5～17窗口覆盖来袭。玩家输入改变也会影响AI，同seed不保证敌招时点不变。

没有找到已证的正常UI录制第二套敌输入或限定COM单一武装能力。Replay第三块每帧只有玩家edge/held两DWORD（`4C2600.c:27`、`5FA990.c:223`）。现有gameplay_start观察器只记玩家（`tools/bhe_runtime_observer.py:168`），敌招靠同一敌actor的WAZ／entry／clock和来袭弹体识别，不要求日志必有敌方START。

弱近战候选：原版**アイランナー／Zako021a，Meka27、WazaGroup51**。默认AI仅踢／头突，基础攻击均20；踢AI距离0～150、W8 Hit22～26，头突距离100～400、W9 Hit28～77。源为`mekBsdxJson/Zako021a.mek.json`和`wazBsdxJson/Zako021a.waz.json`。**当前UI是否可选27仍待确认**；若可选，近距先观察真实踢，再验18，不把候选写成已设置／已命中。弱射击方案尚未核完，Kou默认AI有多种远程招，不是固定弱单发。

## 恢复验收顺序与边界

建议19单招／近远对照→19接20→21子体真Hit→22命中／落空确认→18弱来袭→23射弹及317行为。逐项保留包／正常模板身份、普通输入、真实START／USE、主子图阶段、同靶伤害／控制、取消和回收；落空／中断／再次使用分别登记。

18／23最少补最近受击对象、装甲三项、普通／SD门、HP变化及成功段；23再看317的root/parent和阶段。两个风险仅是优先可证伪项目，不写成已复现或已批准修复。所有运行结论待独立实际证据；本文件不改变原素材、公共转换器、角色逻辑或sealed产物。
