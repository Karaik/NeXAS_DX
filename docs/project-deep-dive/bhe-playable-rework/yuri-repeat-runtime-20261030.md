# Yuri039：普通移动接近后的两轮跨武装连段

固定甲训练靶已验证两轮1「アサシンダガー」→0「ダガー」，每轮210+13+13+13=249伤害，同一录像累计498；目标HP6000→5751→5502。两轮各自最后伤害之后才真正恢复／清链，第二轮首START时used/chain已重新为0/0。这补齐039此前“仅生成、未证播放”的范围，不改变旧033失败或034的独立重力记录，也不代表Yuri全部武装或九角色最终完成。

## 当前包、正常UI与fixture身份

case为`C:/NeXAS_DX_runs/runtime/combo-capture-recover-20261030-07`，仍使用131244：build `run_20261029_130952_143`，EXE SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`，PAC SHA256 `a94c6c3550e498fb03cda5c949e06cb93e8b48ce576fbcacd0c5fca3501297b4`。这些身份来自case准备manifest；本次没有重复hash大媒体，不能把当前case记作032001新PAC的实机证据。

根代理在用户补充当前肉眼画面后重新激活同一case，恢复捕获正常菜单，再通过正常UI加载039；录像自动PLAY至STOP18.00。UI操作／屏幕观察由根代理完成；本报告作者只读取fixture和新增日志段，未操作UI、CDB、Frida或Maven，未改生产、输入、Save或Replay。

035是此前正常UI保存的Yuri空模板，888字节、SHA256 `49e6d34d128f9daf2d48f83d2b43590bf423c95e83342eef51d20a0d1ea26682`，5787帧全零。039为887字节、SHA256 `3f858089b8cc1806be55fe928761c5101cfa456eb58068af7a1cfd4c999bc8ec`，1080帧／tail240；header与前两编码块逐字节等035，第三块8640字节精确等原普通日程编译结果。

初始玩家105、敌甲0、地图68，实际14槽为：

```text
[1,-1,-1,-1,-1,-1,-1,-1,-1,-1,4,0,-1,-1]
```

即近距A1、普通Dash B4、普通Dash C0，其余空。039只用A／C和方向，没有使用B4。普通输入区间为A30..34、C136..140、left360..390、up390..403、A470..474、C576..580，全部[start,end)。原input JSON633字节、SHA256 `1797e96c848243e508be97f6ed9ad0cbc0085f9008a85ea3f772cec6ff33bb10`直接匹配原receipt，不套用其它七件输入换行差异。

独立核对case03原件→case04只读seed→case07继承的035／039分别字节一致，也吻合case07 manifest的seed记录；实播后再次核所有相关路径hash仍相同。完整身份见`target/yuri039-runtime-20261030/fixture-audit.json`，旧fixture、旧receipt和各case Save未覆盖。

## 两轮真实事件

| 事件 | 首轮 | 第二轮 |
| --- | --- | --- |
| START1 | 30；used/chain此前0/0 | 470；used/chain此前0/0 |
| Hit1 | 99；210，HP5790 | 539；210，HP5541 |
| START0 | 136；used/chain此前1/1 | 576；used/chain此前1/1 |
| 0创建匕首弹体 | 149；公共5:329 | 589；公共5:329 |
| 0三次Hit | 149／150／151，各13，HP5777→5764→5751 | 589／590／591，各13，HP5528→5515→5502 |
| 后继命中前状态 | control50／49／49，均Down10 | control50／49／49，均Down10 |
| ACTOR_RECOVERY | 222，HP5751 | 662，HP5502 |
| RESET | 223，used2/chain2 | 663，used2/chain2 |

同一录像内玩家始终`33b5a0b8`、目标始终`254f8050`、manager为`33a35fc0`。每轮首Hit到末Hit之间没有目标ACTOR_RECOVERY或玩家COMBO_RESET；第二轮首击control0是已经恢复后的新起手，不能把两轮合称一条不断的长连。

1的伤害对应主WAZ group112／W12／block2 clock46；0的三次伤害对应group112／W11／block0 clock13／14／15。两轮都实际创建公共5:329，记录的owner是当前玩家、owner_weapon0、owner_skill11、target是同一甲。child地址可被复用，因此按每轮创建事件与owner／target核归属，不凭相同地址声称两个生命周期相同。此处是两个独立装备武装连接，不是把1内部转场或一次弹体多Hit算成更多武装。

## 普通方向接近与第二轮

实际REPLAY_INPUT记录360帧edge/held=0x40（left）、390帧=0x1（up），其持续区间由原039第三块确认。首轮匕首时玩家2446,1822；第二轮1命中时玩家2119,1748、目标2164,1732，随后0的玩家位置2120,1747。方向移动后已能通过正常近距条件再次START1。

BEGIN到END内调试器命令只包含g、读取指针到伪寄存器、printf和echo，没有写actor／坐标命令。fixture没有修改初始块，方向输入是真实普通Replay数据；因此本轮重接近有正常输入依据，不是强写位置得来的命中。这里不声称任意初始距离、墙边或移动目标都能用同一移动帧数进入近距。

## 这次录像自己的最终读回

```text
YURI_039_CASE07_FINAL meka=105 active=-1 group=112 skill=0 Z=0 Vz=0 Graw=51200 jump=100 fall=100 heat=0 TAMA=0 SCRIPT=0 playerXY=2120,1747
```

此为039播放后独立读回：本角色STAND、无active、地面Z/Vz0、物理G50、jump/fall100、heat0、TAMA0／SCRIPT0。180／620帧PLAYER_RELEASE仍在W11局部G10，不能拿释放中途字段代替末状态；039末G50也不改写旧034侧踢路线实际G100的记录。

## 冻结范围与未完成项

独立证据目录`target/yuri039-runtime-20261030/`新增运行审计、精确原字节段及报告。原活日志捕获前缀56400字节、SHA256 `46b179f72ce4be55ad74a6b78242c0f861430dace6810b52beb24e8f791e3394`；本次完整裸marker BEGIN到END范围为`[50991,56390)`，5399字节、SHA256 `ad87642b9a8408b0378a588fef0baf8426eea499feb8c7c2f5d809ec098a8524`。第二次读源已核前缀和该段未变。**日志仍活，不称整个log关闭**，也不把本次断点读回当作游戏异常退出。

这次只覆盖一个035模板／seed、固定甲0／地图68及这一普通接近日程。同录像两轮不是两个独立RNG seed；不同体型／移动／空中目标、更多距离／墙边、主动CPU、落空与受击／取消中断、其它武装、九角色整体和032001新包仍需后续实测。整体`runtimeValidated=false`保持。
