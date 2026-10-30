# Freja FC：冷态80与普通Spear预热104的实机对照

同一正常训练模板下，041冷态与042暖态均完成4 Hit、312伤害并自然回到待机。暖态先经真实START4和24热费用，再由普通A+B取消进入12「武幻剛乱舞」；Spear没有命中，312全部来自FC。两次都没有进入后半高预算分支，不能用这组结果声称完整八阶段已验收。

## 包、输入与正常操作

本次仍为case `C:/NeXAS_DX_runs/runtime/combo-capture-recover-20261030-07`，来源 `run_20261029_130952_143` 的131244包，绝不是032001或尚未生成的54追加武装包。实读游戏EXE SHA256为 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`；PAC身份沿case manifest为 `a94c6c3550e498fb03cda5c949e06cb93e8b48ce576fbcacd0c5fca3501297b4`，本项未重复hash大PAC。

正常UI选择Freja106／敌甲0／地图68，装备行 `[4,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,12,-1]`。训练菜单正常选择FC槽「回復」，实际策略为1；起始FCraw20000。正常重置后由UI在原为空的040槽保存全零模板，12472帧、932字节，SHA256 `62227a721f8a7e9503d165aec911799ce131f5da424aea1d3982e218c84177a0`。

既有 `bhe_replay_input_fixture.py` 保留模板前两块，只生成新的普通输入块，未覆盖旧Replay。输入区间均为[start,end)：041仅A+B36..40；042为A30..32、A+B36..40，两者均900帧／tail180。041为850字节，SHA256 `8ba71b38791219be8bb798f01f7b8270ed03de6f8a422681057c09188692ad49`；042为856字节，SHA256 `7f2bcbdc588723f9d032de5b2a63a52fd4af3fd22347e8f751475dc7793e4fc6`。正常Replay菜单依次载入并自动播放至STOP15.00。

调试器仅保留原CRT/AV捕获及只读软件观察点，未写actor、坐标、热、门或强制START；新增budget观察脚本为 `target/freja-fc-runtime-20261030/observe-budget.cdb`。伪寄存器只存读回指针或Replay帧。

## 真实热费与FC段数

| 事件 | 041冷态 | 042暖态 |
| --- | --- | --- |
| 普通START4 | 无 | 30帧，Spear；33帧实际+2400热 |
| FC START12 | 36帧 | 36帧，先释放Spear，used/chain此前1/1 |
| FC费用 | 39帧+8000，before0 | 39帧+8000，before2400 |
| 首Charge | 89帧，热8000/used0，消费2000 | 89帧，热10400/used0，消费2000 |
| 115帧真实free | 60 | 84 |
| 141帧真实free | 40 | 64 |
| 167帧真实free | 20 | 44 |
| 193帧真实free | 0 | 24 |
| 总Charge | 4次，每次2000 | 4次，每次2000 |

这里raw100对应物理热1；free是 `66097D` 的真实返回，不是拿UI槽长度推算。两次均在counter4后离开block2，未进入3/4/5/6/7后半图。暖态余24仍不足后续门，不将其重新命名为高预算。

两次玩家均为254f8050、目标33b560a8；manager在重载时重建，分别为33a357a8与33a34d40，不将不同manager混作同一对象生命周期。

## 伤害、控制与自然收尾

两次FC都在96／122／148／174帧造成75／77／79／81伤害，目标HP6000→5925→5848→5769→5688。后续三击前目标control均39、state8；首击后到末击间没有目标恢复。暖态UI明确Spear 0 Hit／0伤害，FC 4 Hit／312。它证明普通Spear可以取消进FC，未证明Spear命中接FC的真连。

两次玩家自然释放均244帧，entry2/block2 clock75、Z/Vz0、Graw51200、jump/fall100；目标301帧才恢复，302帧清链。冷态used1/chain1，暖态used2/chain2，符合实际使用的装备数。

播放结束后分别只读独立FINAL，均为：`meka106 active-1 group113 skill0 Z0 Vz0 Graw51200 jump100 fall100 heat0 used0 FC20000 TAMA0 SCRIPT0 playerXY2697,1808`。FC20000是既选训练回復策略的结束读数，不冒认为只靠命中回满。

## 冻结证据与边界

`target/freja-fc-runtime-20261030/runtime-audit.json` 固定输入、Replay副本和两个原始日志段。041裸marker段为[96474,100404)，3930字节，SHA256 `9b7ceef96271684facd9eb6234e73201b3932ca15645db0428d07a49b767bead`；042段为[100820,105210)，4390字节，SHA256 `b8840fca6aba59fae2d5ed34b591fb5af3c492bd0fc7a31cc3b768a4866ac67a`。冻结活日志前缀105221字节、SHA256 `d81951ac6ceebc945ec9585e364f47d71edd6fb37a359c4230a18a42dcf85e75`，二次读源确认前缀未变。日志仍活，不声称整log关闭；用户原Save全部78文件与case manifest起始清单逐字节hash相同。

暖态STOP截图为 `target/freja-fc-runtime-20261030/warm-042-stop.png`。本项只覆盖这个模板／seed和固定甲，不包括其它目标、落空／受击中断、高预算完整后半或新54项。下一步使用正常装备10bit→5Swing→4Spear→12FC，必须四个真实START及3000/3000/2400/8000费用均成立，才可判定名义164路线；不能只相加武装最大热费。整体runtimeValidated仍false。
