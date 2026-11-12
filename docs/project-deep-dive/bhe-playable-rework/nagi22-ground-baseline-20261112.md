# Nagi22命中／落空实机基线（2026-11-12）

结论为FAILED_BASELINE：近距确实产生盾打300＋内部上斩90伤害，但远距无命中仍进入内部上斩图W30，不符合「命中したときだけ斬上げへつなぐ」的预期。本轮不登记命中确认正确或Nagi22整体验收通过，不以近距390掩盖落空误派生。没有修改技能、EXE或构建，原样失败证据独立保留。

## 正常UI准备与实际输入

继续同case19／游戏57672／EXE1ac677／PAC8efe。先按精确manifest／边界与无reparse／Git检查，把旧090的Katou24单招、099的Katou25单招和081的Katou20远距落空三DAT共2552B移到同case归档；96其它Replay与9份元数据前后hash相同。映射 `replay-slot-archive-katou090-099-and081.json` 为24519B，SHA256 `1d38aceeb732e7a494bed034dd21a40f39e8c943053d8ba614679ec35776b5b3`；旧内容不删除、不用新槽替代旧hash，旧case17不触碰。

root正常UI退出Replay后选择Nagi107。当前SelectMekaMenu.dat零基row28=[107,49,106]，对应可见第3行第9列；不能误取另一行状态列中的107。装备菜单先将22放普通冲刺A，再解除唯一旧近距武装（原生禁止卸空全部普通武装），最后14槽仅index9为22，其余−1，普通／FC／initializer不混槽。敌Kou0、map68、行为停止0。

正常UI录制并保存090 generation2，840B／SHA256 `296538a0bbad986daeef74aff867bd13565254277a835899d31f7c7b93732dff`，2520帧输入均零；seed9为[35598969,107,0,68,0,1,0,38,294]。790B头及前两编码块前缀SHA256 `874c55fbd72bd2e19e72b59382aee3287f526c33382ae326912d1070c2e26bcc`。两份新录像只替换普通输入块，没有写入actor、seed、位置、热量、门或敌状态。

| 试验 | 普通输入[start,end) | 槽位代次／帧数 | 起手实际位置及XY距离 |
| --- | --- | --- | --- |
| nagi22-ground-hit | A[30,34) | 099 gen2／600 | 玩家2302,1808，敌2402,1808；100 |
| nagi22-ground-whiff-left200 | left[0,200)，A[250,254) | 081 gen5／800 | 玩家1384,1885，敌2402,1808；raw XY约1020.907929 |

模板在游戏内配置、保存后才解析；所有普通edge／held对均重编译逐字节一致，头及前两块完整保留。右侧列表旧用户091～098不改。记录中的receipt false是生成时事实，不能反写为实际运行结果。

## 近距：真命中与内部追击成立，但不代表确认条件正确

native30唯一START22／USE，usedBefore/chainBefore=0/0。RPM31进入114/W33 P0，32 P1，47 P3。native frame50／tick1816310实际盾打伤300，目标6000→5700；RPM首次见HP下降在counter51／P3clock4。该clock在51～56重复，不能把接受counter当作局部clock或凭单一hitstop字段推断无暂停。

RPM61进入内部114/W30 P0、62 P1；native frame68／tick1816328上斩再伤90，目标5700→5610，命中前control48。RPM首次见第二HP变化在counter69／W30P1clock7，后续也有clock重复。只有一次START22／USE，没有START19或第二次独立装备热耗；W30只属于22的内部图，不能另算一件新武装或两个装备的跨武装连段。

native104在W30 P1clock32自然释放，RPM105 active−1。玩家上斩后落地时counter130进入state6，131开始空图，native159退出／RPM160回state0；玩家HP全程2000，control／pending／reaction均0，原生无玩家damage或reaction，故记录为落地恢复，不是受击中断。敌native229恢复、玩家RESET230、热与heatState238归0，分别保留时刻。

## 远距：无命中仍进入W30，保留为失败

普通left在native212结束，RPM213确认中立，250实际再起手22。RPM251 W33P0、252 P1、267 P3；P3clock4～7及确认附近样本完整，没有目标damage／reaction，HP始终6000、state0/control0，排除“未出招”或仅漏读伤害。

尽管如此，RPM276仍进入114/W30 P0。它未满足近距进入P1的条件，停留至native300／W30P0clock24自然结束，RPM301 inactive；没有上斩伤害，也没有走预期未命中的W18收盾路径。这是实际误派生，不能因W30最终没有打中就判命中限定正确。RESET330、热338归0；700～800有完整101行正常地面／热／物理／甲及对象尾。

近样本601行无缺、27次拒读；远801行无缺、45次拒读，799与800同tick1839693，只有800的tick-firstTick-counter为−1。近500～600另有101行完整正常尾。两次LOAD相互独立，不拼成同场再用；原生伤害帧50／68与RPM首次HP变化51／69都保留，不互相覆盖。

## 根因尚待调用点观察

已有静态依据见[nagi22命中确认生命周期复核](nagi22-hit-confirm-lifecycle-20261111.md)。当前成品小资源Nagi.mek／nagi.waz已按本包identity实读，W33P3仍Hit4～7、确认8～9；本轮未改该载荷。

可验证假说是：确认时当前Hit已无效，MEKAHIT仍继续后继比较而不刷新共享cache，旧0导致落空派生。63A7D0会撤销窗外slot41索引，60E6D0无当前Hit时返回0；66B0F0仅有Hit才写cache+0x5C/+0x60，却仍返回2；66B480才做实际比较，6685D0使用共享0x86CDFC上下文且本入口不清cache。上述静态链不能代替本次求值瞬间的证据。

下一步仅被动记录P3附近66B0F0／66B480的入出值、slot41索引/clock、当前Hit及剩余次数、cache前后和少量前序求值，不强写状态或修改EXE。原版活跃Hit窗内确认及WAZAHITCOUNT跨窗先例可参考，但后者不是天然每次起手清零，不能直接替换后宣告修复。需要确认根因并重新验近／远、最后Hit帧、取消／再用边界后，才修改通过状态。

## 固定证据范围

`nagi22-ground-hit-miss-baseline.json` 为214796B，SHA256 `4da6ff919a3f36b572afe56cf4f4ce12214d41d1e75d4724587f490f88e2f61e`。46条原样native对象，events冻结前10256行／5862536B，SHA256 `6ce4dd76d4c04884f152fbfbaa99ef8803dd8c396a2c413ce6c6f78e25cae539`。18张RPM表797行／27483单元、31份固定来源及202行完整正常尾按两run分别核对；运行state只保留捕获时身份，不当作固定hash来源。

本轮证明了近距效果与有限正常收尾，也复现了远空挥误派生。根因确认、修复、跨武装连段、专用SD、受击、空中、同场再用、Nagi其余五招及完整九角色／Sou153／原164入口／关卡／最终交付仍未完成；overallNagi22Passed、runtimeValidated和terminalAcceptance均false。只提交小型文本，录像、采样、游戏二进制、截图与转储不提交。
