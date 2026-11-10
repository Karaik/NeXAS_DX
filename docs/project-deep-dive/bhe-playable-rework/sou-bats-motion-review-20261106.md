# Sou34蝙蝠运动缺失只读审查（2026-11-06）

本记录只保存源码、原始资源及现成反编译的调查结论；未修改生产源码、构建、测试或操作游戏，未声称修复完成。缺图片继续nonfatal审计；无hit单独记效果未证，不据此认定安全失败。2026-11-10追加消费者核查并修正原LENGTH_Z说明；原字节记录、历史实测与未实施候选均保留。

## 首击落空与后续缺失分开

Sou武装34使用KOUMORI03／ナイトメアパーティーLv3，源sou skill143。P0在29f发Tama05源372／目标524；32～35f只有本招命中计数大于0才进入P1，否则94f结束。[既有实测记录](sou-menu-validation-20261105.md#未命中招式的训练调查入口)第34行登记的窗口停留P0且初击未命中，后续蝙蝠段未进入。因此目标525的后续运动缺失不能解释该次首击落空。需先贴近静止靶确认524命中和P1进入，再评价蝙蝠各段运动；下述P2已查明是向root中央回返。

## 确切丢失路径与原字节

[followup BheToBsdxWazSlotMap.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/followup/convert/waz/BheToBsdxWazSlotMap.java)第43行将BHE slot23映为-1；[BheToBsdxWazSkillConverter.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/followup/convert/waz/BheToBsdxWazSkillConverter.java)第78～85行遇到负目标槽直接返回null，普通事件及该槽FF均不产生目标单元。tsukuyomi公共转换也在其SlotMap第48行声明23→-1。这是明确的数据丢失路径，不是尚未定位的渲染或图片错误。

BHE源`tama05.waz` skill373／`RESEARCH04`／`★蒼：ナイトメアパーティー（蝙蝠）`为四phase，当前公共目标525。P2原slot23为type5 CEventValRandom，两条记录、无本槽FF。读取`src/main/resources/game/bhe/waz/tama05.waz`真实二进制的32字节事件与现有JSON逐值一致：

| 文件offset | 起止帧 | 六整数 | 32字节SHA256 |
| --- | --- | --- | --- |
| 1869969 | 0～55 | [0,32,0,0,16,0] | e8c24f9d54ad49def3584a8d6f6d49c2a862728ebb6fe779a49f545b52852d61 |
| 1870001 | 56～120 | [0,16,0,0,10,0] | 55bdc965849a8cc7a3ff2ebab5120b6e3d83cefce77f08a58a91035c8b23e91a |

P2原源同时有方向slot15（term type[2]，位置选择[3,19]）、方向修正16／17、惯性24、攻击46／47及原出口82。这里[3,19]使用位置与锚点字典，已确认为root机体中央；当前转换保留root选择但将CENTER降为NONE，见下节。P0/P1原来另有平面速度18，不能因P2缺slot23便说整段从来不移动。

主caller KOUMORI03的P1也有两条slot23被同一路径丢弃，须独立登记；修525不等于整招动作链完整。真实`sou.waz`事件：

| 文件offset | 起止帧 | 六整数 | 32字节SHA256 |
| --- | --- | --- | --- |
| 1142456 | 15～15 | [0,0,0,0,64,0] | 13f3538d10a749bcfc74078fe2e0cbb4caf52fc3c1fb9fe702d70c0e4174d371 |
| 1142488 | 21～28 | [1,0,0,0,0,0] | f14187827f297a34d764ecbbf2cf1ef8fab8595bec43793ef1e1e1dbf5bebfb2 |

这些六整数是完整事件载荷，不能仅摘取一个端点当恒速，也不能解释成X/Y/Z三个独立速度。

## 原生语义与适配边界

现成BHE反编译路径均位于`src/main/resources/ida-reverse/bhe/export-for-ai/decompile`：

- `6E9F60.c:63～69`读取方向的XYZ；`102～129`读取平面速度到this[1363]并清this[1364]；`212～239`读取slot23的速度到this[1364]并清平面速度this[1363]。
- `6EB6F0.c:41～90`在三维速度非零时处理三维方向转动，并调用7355A0；否则160行走平面方向/速度路径。
- `7355A0.c:23～38`先通过`6F24F0`归一化XYZ方向，再乘速度scalar，写向量XYZ。`6F24F0.c:18～28`明确使用三维平方和开根后的归一化。

因此BHE slot23是沿三维方向向量运动的速度，不是可直接拆成固定水平／垂直分量的事件。DX的slot18为平面速度，slot19为跳跃力，slot20为追尾跳跃力；[SkillInfoFactory.java](../../../src/main/java/com/giga/nexas/dto/bsdx/waz/wazfactory/SkillInfoFactory.java)第35～37行声明这些类型。

### 已确认P2朝root机体中央回返

源slot15为root18／ANGLE、type[2]／OBJECTZ，aux1[0,0]是self位置，aux2[3,19]按OBJECTPOS与OBJECTPOS2字典解释；不能把第二个19误当OBJECT/多重锁定枚举。真实`grpBheJson/term.grp.json`内OBJECTPOS index3=PARENTMEKA（大元のメカ）、OBJECTPOS2 index19=CENTER（機体中央）。因此P2的方向是从蝙蝠自身到root机体中央，不是默认敌目标。P2的原LENGTH_Z<=25条件虽然也保存aux2[3,19]，但它属于PARENT/OBJECT1条件消费者，不能沿用ANGLE的位置锚点解释。2026-11-10已确认该条件只用第一个3选择root，第二个19不参与计算；先前“距离出口共用CENTER锚点”的解释撤回。

原生证据：BHE `73DE00.c:48～53`分别返回默认目标this[1205]、direct parent this[1203]和root this[1207]；`73D890.c:91～103`的CENTER分支把机体高度的1/2或条件分支下1/4加入Z。此处保留原生分支，不把CENTER简单写死为任何固定高度。

[BheInfoCollectionTermConverter.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/BheInfoCollectionTermConverter.java)第246～248行让ANGLE的OBJECT／OBJECTZ分支使用POSITION消费者；268～285行分别重编译位置选择与锚点，314～320行将DX缺失的CENTER／TOP降级NONE。因此原[3,19]转换为[3,0]，root语义仍在，CENTER高度锚点丢失。

已直接核当前8efe包最终`tama05.waz`：文件offset2680670的11个小端整数为`[18,1,2,0,2,0,0,2,3,0,0]`，即root18、types[2]、params[]、self aux[0,0]、目标位置aux[3,0]、末int2=0。这是当前公共525内ANGLE/OBJECTZ记录，核查范围是这条集合的真实字节，不冒称完整skill边界hash。

DX `668FD0.c:74～82`的POSITION选择器把ROCK读自actor+5304、root读自actor+5316；`668A30.c:26～31`的对象选择器同样区分默认目标this[1326]、direct parent this[1323]与root this[1329]。DX `60FCE0.c:123～175`的slot20只按type选择默认目标或direct parent；默认目标与这里的root并不同义。不能为了补高度而追向敌人，也不能未经关系核对把direct parent当root。

即使另行获得指向root的高度方案，slot18加垂直分量的总速也不自动等于BHE三维速度模。准确复刻还要处理BHE方向所用CENTER及三维转向；若选择DX原生近似，则应明确采用双方中心对齐，保证水平目标、垂直目标和原距离条件都指向同一root对象，不能称为完全等价。

### 2026-11-10：距离消费者与原生供体校正

BHE `71BDE0.c:21～22`先经`73E1E0.c:10～18`选择两个OBJECT；DX `66EA50.c:22～25`对应`668D70.c:10～18`。两者只读取aux首整数。BHE `71C780.c:224～244`的LENGTH_Z（case8）及DX `66B9C0.c:109～125`的LENGTH_Z（case7）均计算：`round(horizontalDistance) + abs(selfZ + selfHeight/2 - rootZ - rootHeight/2)`。因此原距离条件一直是双方中心高度差与水平距离的组合，不是“self基点到root CENTER的三维欧氏距离”；aux第二整数19不产生CENTER加算。ANGLE方向的CENTER缺失仍成立，与此处是两个不同消费者。

DX还有一个独立差异：`669CD0.c:64～87`的ANGLE/OBJECTZ通过`(x,y-z)`投影求单个方向角；`60A9C0.c:85`最终按平面速度／角度更新移动向量。仅把原三维速度塞到slot18并保留OBJECTZ，会把高度差混入平面转向，不能作为已完成适配。

已读真实DX原版`Tama05.waz`的BLADE（59）、AXE_NAGE（60）及回旋镖0010（62）对应资源：它们在回返段使用ANGLE/OBJECT到parent、slot18、slot20 `int1=1`（direct parent）及LENGTH_Z到parent出口。BLADE的slot20文件offset390079，帧0～156、type1，内部最大增分事件为完整`[0,10,0,0,10,0]`；该事件与原二进制逐值一致。它证明DX存在原生中心高度追尾回返机制，不能证明借用后Sou速度、转向或攻击效果已通过。

`60FCE0.c:131～175`的slot20以`targetZ+targetHeight/2-selfHeight/2-selfZ+heightCorrection`为高度误差，结合水平距离及slot18速度生成垂直速度，并施加增分／越界处理。无默认敌目标改绑的parent模式只有在私有蝙蝠direct parent确为root时才适用。该近似将蝙蝠自身中心对准root中心，与BHE方向从self基点到root CENTER不同；两引擎的LENGTH_Z条件仍可沿原公式保留。

## 历史候选与当前建议

本记录前次只读调查尚未确定[3,19]消费者时，曾提出“私有P2补slot18，先在等高场景看平面追赶，再决定高度适配”的未证候选。该候选没有实施或实测；其中“追赶”不能解释成追敌，slot20默认目标方案也不能继续作为当前建议。此次字典及原生证据已经把P2方向明确为root中央回返。

当前建议先按actual common plan定位source slice、双名及真实parent/root链，以完整源记录和重编译后公共图做守卫。若进入实现阶段，应深复制私有蝙蝠、只重绑三等级caller的相关发射边，并保留原数量、攻击、方向修正、循环、出口、公共525及动态WazaGroup计数；不改全局23→18，不加固定超时或强制命中掩盖原条件。

运动适配需处理P2两条三维速度事件丢失及方向CENTER退化；不能把方向问题误归到距离条件的锚点。2026-11-10找到的局部候选是：私有P2以ANGLE/OBJECT计算root平面方向，保存两段完整速度事件作为slot18曲线，另以slot20的parent模式处理双方中心高度差；三等级caller都直接发射蝙蝠，但仍须实机核实direct parent=root。原LENGTH_Z到root的出口按原比较式保留。该路线是DX原生回返近似，需通过高低差、运动root、原攻击与回收验收；slot20默认敌目标仍不适用，三维总速和转向不等价。

主caller P1丢失的两个动作另立局部适配断言；先读三等级源动作和方向上下文，不能机械套蝙蝠的root回返转换。修公共525相关副本不等于主caller动作也完整。

建议回归：三等级源签名、两段帧边界和完整六整数、ANGLE/POS与PARENT条件的消费者区分、root对象选择、双方中心高度差和原距离公式、实际引用及动态尾部、公共／私有可变节点隔离、原公共图及无关技能保全、幂等／漂移／半安装拒绝、两次二进制回环。缺图保持nonfatal，不扩大到其它角色或全局转换器。

建议实机：先证524命中和P1进入，再看525的P0→P1→P2、XYZ位移、相对root的水平距离与双方中心高度差、方向变化、期间攻击与原P3回收；另记真实XYZ欧氏距离作为诊断，不能把它等同于原LENGTH_Z。让root静止、移动、与蝙蝠存在高低差，并覆盖正常取消、受击中断和再次使用；核direct parent与root是否始终是同一对象，不能用敌目标距离代替root回返距离。首击无命中、后续未生成、P2未向root回返、回返但未命中与最终回收失败分别记录。本审查没有宣称这些修复或实测已通过。
