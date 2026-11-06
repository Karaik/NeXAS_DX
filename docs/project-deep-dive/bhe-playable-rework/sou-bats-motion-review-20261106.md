# Sou34蝙蝠运动缺失只读审查（2026-11-06）

本记录只保存源码、原始资源及现成反编译的调查结论；未修改生产源码、构建、测试或操作游戏，未声称修复完成。缺图片继续nonfatal审计；无hit单独记效果未证，不据此认定安全失败。

## 首击落空与后续缺失分开

Sou武装34使用KOUMORI03／ナイトメアパーティーLv3，源sou skill143。P0在29f发Tama05源372／目标524；32～35f只有本招命中计数大于0才进入P1，否则94f结束。[既有实测记录](sou-menu-validation-20261105.md#未命中招式的训练调查入口)第34行登记的窗口停留P0且初击未命中，后续蝙蝠段未进入。因此目标525的后续运动缺失不能解释该次首击落空。需先贴近静止靶确认524命中和P1进入，再评价追击。

## 确切丢失路径与原字节

[followup BheToBsdxWazSlotMap.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/followup/convert/waz/BheToBsdxWazSlotMap.java)第43行将BHE slot23映为-1；[BheToBsdxWazSkillConverter.java](../../../src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/followup/convert/waz/BheToBsdxWazSkillConverter.java)第78～85行遇到负目标槽直接返回null，普通事件及该槽FF均不产生目标单元。tsukuyomi公共转换也在其SlotMap第48行声明23→-1。这是明确的数据丢失路径，不是尚未定位的渲染或图片错误。

BHE源`tama05.waz` skill373／`RESEARCH04`／`★蒼：ナイトメアパーティー（蝙蝠）`为四phase，当前公共目标525。P2原slot23为type5 CEventValRandom，两条记录、无本槽FF。读取`src/main/resources/game/bhe/waz/tama05.waz`真实二进制的32字节事件与现有JSON逐值一致：

| 文件offset | 起止帧 | 六整数 | 32字节SHA256 |
| --- | --- | --- | --- |
| 1869969 | 0～55 | [0,32,0,0,16,0] | e8c24f9d54ad49def3584a8d6f6d49c2a862728ebb6fe779a49f545b52852d61 |
| 1870001 | 56～120 | [0,16,0,0,10,0] | 55bdc965849a8cc7a3ff2ebab5120b6e3d83cefce77f08a58a91035c8b23e91a |

P2同时保留方向slot15（term type[2]，对象选择[3,19]）、方向修正16／17、惯性24、攻击46／47及原出口82。P0/P1原来另有平面速度18，不能因P2缺slot23便说整段从来不移动。

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

DX现成反编译`bsdx/export-for-ai/decompile/60FCE0.c:123～175`显示slot20按type在默认目标this[1326]与parent this[1323]之间选择，再依据高度差／平面距离／平面速度更新垂直分量。原蝙蝠P2的[3,19]对象选择必须先与该对象核对，不能盲加slot20造成水平与高度追不同对象。即使对象一致，slot18加slot20的总速度也不自动等于BHE三维速度模。

## 最小局部候选与验证

建议先按actual common plan定位原source slice及双名，以完整源记录及重编译后公共图做守卫；深复制一份私有蝙蝠，只在私有P2补原两条时间和完整六整数的DX slot18，并将三等级caller中实际蝙蝠发射边指向私有图。保留原发射数量、方向、攻击、循环、退出与公共525，按动态尾部更新WazaGroup计数，不增加玩家武装。这个候选仅恢复DX平面追赶，须由实测评价，不是三维等价移植。不要改全局23→18，不要用固定超时或强制命中掩盖原条件。

主caller P1丢失的两个动作另立局部适配断言；先读三等级源动作及方向上下文，不能机械套蝙蝠的转换。若地面／等高追赶仍不能达到所需实际效果，继续核目标绑定及真实高度需求，再决定是否增加有原生供体证据的高度适配。

建议回归：三等级源签名、两段帧边界和全部六整数、实际引用及动态尾部、公共／私有可变节点隔离、原公共图及无关技能保全、幂等／漂移拒绝／半安装拒绝、两次二进制回环；缺图保持nonfatal，不扩大到其它角色或全局转换器。

建议实机：先证524命中和P1进入，再看525的P0→P1→P2、XYZ位移／目标距离、方向变化、命中和原P3回收；分别覆盖等高与高低差、近远距离、方向、正常取消、受击中断、再次使用。首击无命中、后续未生成、P2不追赶、追赶但无命中和回收失败必须分别记录；本审查没有宣称其中任何修复通过。
