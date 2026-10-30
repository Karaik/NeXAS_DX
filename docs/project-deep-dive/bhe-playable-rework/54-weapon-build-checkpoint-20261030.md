# 九角色54个追加武装：130704构建检查点

2026-10-30。本包仍为 **development candidate，runtimeValidated=false**。目前通过的是九角色各六个独立普通MEK入口、默认14槽、完整资源回环、目录差异及PAC真实解包一致性；Sou的153项菜单技能、新54项真实跨武装连段和取消／受击中断验收仍未完成。

本文只记录下述固定产物，不以持续变化的latest或后续case状态替换其身份。数据独立读取自最终审计JSON、实际build receipt、冻结JUnit XML及case状态；本次文档工作没有执行Maven、Java、UI或CDB。

## 构建与产物身份

实际构建源码为 `f93add4e995d4c94f16d4a4f3dfa71f56e1e4896`（配置九角色六武装默认出装并扩充中文使用说明）。其后 `784fe0b27d2d13ad7a19257ea7de0e2e44e94920` 只增加Freja中高热预算／双FC的旧包运行文档，没有修改本包构建源码或二进制。receipt绑定产物哈希，不自带Git commit字段。

- [实际build receipt](/C:/NeXAS_DX_runs/builds/run_20261030_130356_492/build.json)：`run_20261030_130356_492`。
- 资源目录：`C:/NeXAS_DX_runs/builds/run_20261030_130356_492/sou/sou_assets_20261030_130629_331`。
- EXE：`C:/NeXAS_DX_runs/builds/run_20261030_130356_492/sou/BaldrSky_20261030_130704_077.exe`。
- PAC：`C:/NeXAS_DX_runs/builds/run_20261030_130356_492/sou/Update3.pac`。
- 对照基线：`C:/NeXAS_DX_runs/builds/run_20261030_031642_167/sou/sou_assets_20261030_031923_654`（032001）。

| 文件身份 | 字节数 | SHA256 |
| --- | ---: | --- |
| 只读源 `D:/Code/NeXAS_DX/src/main/resources/ida-reverse/BaldrSky.exe` | 4,793,856 | `713cf605360a55a863f87bfbdea5453aaeeb2a33983ad4a005c9297e3a86e757` |
| 本包EXE | 4,793,856 | `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae` |
| 本包Update3.pac | 1,460,488,767 | `86296c31b693a6260f2d73bf38dfc1d3e44217a8e0261ee5a4a3e0ac7da8f672` |

源EXE、本包EXE和PAC的实读哈希均与receipt匹配；本包EXE与032001相同，PAC身份已改变。receipt继续保留Sou，且 `runtimeValidated=false`。

## 角色、计数和实际六入口

下表ID按最终MekaGroup双名定位；103是Akao，不属于本次九角色，Sou为113。主WazaGroup与Meka ID是两个编号空间。原7个追加加本次47个独立MEK入口，总计54个；phase、路由控制器和私有helper不增加装备计数。

| 角色 | Meka ID | 主 WazaGroup | MEK 总行数（旧→新） | 主 WAZ 技能数（旧→新） | 独立追加 |
| --- | ---: | ---: | ---: | ---: | ---: |
| Tsukuyomi | 104 | 111 | 20→25 | 33→38 | 6 |
| Yuri | 105 | 112 | 15→21 | 27→33 | 6 |
| Freja | 106 | 113 | 17→23 | 32→38 | 6 |
| Nagi | 107 | 114 | 19→24 | 30→35 | 6 |
| Misaki | 108 | 115 | 22→27 | 34→39 | 6 |
| Naoto | 109 | 116 | 21→26 | 32→38 | 6 |
| Katou | 110 | 117 | 21→26 | 32→37 | 6 |
| Wilhelm | 111 | 118 | 18→23 | 32→37 | 6 |
| Motoki | 112 | 119 | 18→23 | 33→38 | 6 |

| 角色 | MEK 武装号 | 主 WAZ 技能号 | 日文装备身份 | 英文技能身份 |
| --- | ---: | ---: | --- | --- |
| Tsukuyomi | 19 | 32 | 袈裟・一閃 | `DX_KESA_ISSEN` |
| Tsukuyomi | 20 | 33 | 打突・留め | `DX_DATOTSU_HOLD` |
| Tsukuyomi | 21 | 34 | 一威・止刃 | `DX_ICHII_BRAKE` |
| Tsukuyomi | 22 | 35 | 逆風・返し | `DX_GYAKUFU_RETURN` |
| Tsukuyomi | 23 | 36 | 銃撃・離脱 | `DX_GUNSHOT_RETREAT` |
| Tsukuyomi | 24 | 37 | 切返・二連 | `DX_KIRIKAESHI_DOUBLE` |
| Yuri | 15 | 27 | ダガー・刻み | `DX_DAGGER_LINK` |
| Yuri | 16 | 28 | アサシンダガー・速刺 | `DX_ASSASSIN_QUICK` |
| Yuri | 17 | 29 | サイドステップ・地走 | `DX_SIDESTEP_GROUND` |
| Yuri | 18 | 30 | パルス・三点射 | `DX_PULSE_THREE` |
| Yuri | 19 | 31 | 狙撃・一射離脱 | `DX_SNIPER_SINGLE` |
| Yuri | 20 | 32 | マイン・置き逃げ | `DX_MINE_RETREAT` |
| Freja | 17 | 32 | スピア・留め突き | `DX_SPEAR_ANCHOR` |
| Freja | 18 | 33 | 斬り上げ・低空 | `DX_UPPERCUT_LOW` |
| Freja | 19 | 34 | バックキック・退避 | `DX_BACKKICK_RETREAT` |
| Freja | 20 | 35 | スイング・双薙ぎ | `DX_SWING_DOUBLE` |
| Freja | 21 | 36 | ジャベリン・二段投擲 | `DX_JAVELIN_TWO` |
| Freja | 22 | 37 | 火炎弾・扇射 | `DX_FLAME_FAN` |
| Nagi | 18 | 29 | 瞬間ディフレクト | `DX_QUICK_DEFLECT` |
| Nagi | 19 | 30 | 斬上げ・接続 | `DX_NAGI_CLOSE_LAUNCH` |
| Nagi | 20 | 31 | 乱れ突き・三連 | `DX_NAGI_THREE_THRUST` |
| Nagi | 21 | 32 | 転移突き・追撃 | `DX_NAGI_TRANSFER_LINK` |
| Nagi | 22 | 33 | 盾打ち・反攻 | `DX_NAGI_BASH_CONFIRM` |
| Nagi | 23 | 34 | 単発リフレクト・解放 | `DX_NAGI_SINGLE_REFLECT` |
| Misaki | 21 | 33 | 転移手・挟撃 | `DX_PINCER_HANDS` |
| Misaki | 22 | 34 | 鉱石・二拍射 | `DX_ORE_DOUBLE_TAP` |
| Misaki | 23 | 35 | 転移手・二度招き | `DX_HAND_TWO_REACH` |
| Misaki | 24 | 36 | 血晶槍・短束 | `DX_CRYSTAL_SHORT_SPEAR` |
| Misaki | 25 | 37 | 血晶剣・帰着 | `DX_CRYSTAL_SWORD_RETURN` |
| Misaki | 26 | 38 | 床召還・重ね置き | `DX_FLOOR_DOUBLE_MARK` |
| Naoto | 20 | 31 | ワンツー・コンファーム | `DX_HIT_CONFIRM_ONE_TWO` |
| Naoto | 21 | 32 | フック・リターン | `DX_HOOK_RETURN` |
| Naoto | 22 | 34 | クロー・起動突き | `DX_CLAW_DEPLOY_RISE` |
| Naoto | 23 | 35 | ダウンネイル・収納 | `DX_DOWN_NAIL_RETRACT` |
| Naoto | 24 | 36 | マシンガン・三点射 | `DX_MACHINEGUN_THREE_BURST` |
| Naoto | 25 | 37 | 炸裂弾・交差連射 | `DX_EXPLOSIVE_STAGGER` |
| Katou | 20 | 31 | トンファー・二連打 | `DX_TONFA_TWO_HIT` |
| Katou | 21 | 32 | トンファー・踏込肘 | `DX_TONFA_ENTRY_ELBOW` |
| Katou | 22 | 33 | トンファー・引き肘 | `DX_TONFA_RECOIL_ELBOW` |
| Katou | 23 | 34 | 蹴崩し・追い肘 | `DX_KICK_CONFIRM_ELBOW` |
| Katou | 24 | 35 | トンファー・叩き納め | `DX_TONFA_SLAM_STOW` |
| Katou | 25 | 36 | ミサイル・二段牽制 | `DX_MISSILE_TWO_BURST` |
| Wilhelm | 17 | 31 | 交互砲撃・離脱 | `DX_ALTERNATE_CANNON_EXIT` |
| Wilhelm | 18 | 32 | キック・導弾追撃 | `DX_WILHELM_KICK_MISSILE` |
| Wilhelm | 19 | 33 | サインミサイル・二拍 | `DX_WILHELM_TWO_BEAT_MISSILE` |
| Wilhelm | 20 | 34 | 照準砲撃・制動 | `DX_WILHELM_AIMED_CANNON_EXIT` |
| Wilhelm | 21 | 35 | 肩ミサイル・圧制 | `DX_WILHELM_SHOULDER_PRESSURE` |
| Wilhelm | 22 | 36 | 対空砲火・収束 | `DX_WILHELM_AA_FINISH` |
| Motoki | 17 | 31 | 集中爆撃・帰投 | `DX_FOCUSED_BOMB_RETURN` |
| Motoki | 18 | 33 | ワンツー・離脱 | `DX_MOTOKI_ONE_TWO_EXIT` |
| Motoki | 19 | 34 | ドリル・確定突き | `DX_MOTOKI_DRILL_CONFIRM` |
| Motoki | 20 | 35 | 排熱弾・即発 | `DX_MOTOKI_QUICK_EXHAUST` |
| Motoki | 21 | 36 | ガンポッド・三連点射 | `DX_MOTOKI_THREE_SHOT_GUNPOD` |
| Motoki | 22 | 37 | ビーム・短照射 | `DX_MOTOKI_SHORT_BEAM` |

Naoto的六个主入口之外还有 `DX_CLAW_DEPLOY_RISE_FOLLOWUP` 私有Rise（主WAZ33，一phase）；它没有MEK装备行。Motoki的 `DX_FOCUSED_BOMB_RETURN` 是MEK17／主WAZ31的可装备router，主WAZ32的 `DX_FOCUSED_BOMB_EXECUTION` 是四phase私有执行，二者只计一个武装。新M6是MEK22／主WAZ37的 `DX_MOTOKI_SHORT_BEAM`，没有公共464复制或额外可装备helper。

## 最终默认14槽

以下均为实际WeaponEquip.dat读回的MEK武装号。槽0..11按 `3*state+button` 排列，A/B/C分别为0/1/2；槽12是FC，槽13是initializer。每角色六个追加均在12个普通槽出现一次，14个武装号互异且在0..152，分类为普通／FC／initializer的0／1／2；日文身份和完整14槽名单与批准表一致。各角色FC与initializer旧默认不变，Sou整行旧默认不变。

| 角色 | 近距 0..2 A/B/C | 搜索冲刺 3..5 A/B/C | 远距 6..8 A/B/C | 普通冲刺 9..11 A/B/C | FC 槽12 | initializer 槽13 |
| --- | --- | --- | --- | --- | ---: | ---: |
| Tsukuyomi | 19 / 20 / 2 | 0 / 21 / 22 | 23 / 11 / 12 | 24 / 5 / 1 | 13 | 18 |
| Yuri | 15 / 16 / 17 | 1 / 4 / 7 | 18 / 19 / 8 | 20 / 10 / 3 | 11 | 14 |
| Freja | 17 / 18 / 19 | 3 / 4 / 2 | 21 / 22 / 8 | 20 / 0 / 10 | 12 | 16 |
| Nagi | 18 / 19 / 20 | 21 / 5 / 7 | 3 / 14 / 4 | 22 / 11 / 23 | 16 | 17 |
| Misaki | 22 / 21 / 0 | 3 / 23 / 25 | 1 / 24 / 26 | 8 / 2 / 10 | 18 | 20 |
| Naoto | 0 / 20 / 1 | 5 / 22 / 23 | 24 / 25 / 16 | 21 / 3 / 7 | 18 | 19 |
| Katou | 10 / 20 / 22 | 2 / 21 / 24 | 13 / 25 / 16 | 23 / 6 / 4 | 18 | 19 |
| Wilhelm | 18 / 1 / 0 | 6 / 9 / 8 | 19 / 21 / 20 | 17 / 22 / 5 | 14 | 16 |
| Motoki | 18 / 19 / 20 | 9 / 11 / 17 | 21 / 5 / 6 | 10 / 22 / 7 | 15 | 16 |

## 已通过的静态与封包证据

| 验证层 | 实际结果 | 固定证据 |
| --- | --- | --- |
| 定向回归v8 | 18类、142项；0失败／0错误／0跳过 | [日志](/D:/Code/NeXAS_DX/target/bhe-audit-20261026/nine-six-variants-regression-20261030-v8.log)、[冻结XML目录](/D:/Code/NeXAS_DX/.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/green-nine-v8) |
| 九角色整链 | root串行Maven实际terminal exit0；runner为1项、0失败／错误／跳过，已产生上述receipt | [整链日志](/D:/Code/NeXAS_DX/target/bhe-audit-20261026/nine-six-variants-full-build-20261030.log)、[runner XML](/D:/Code/NeXAS_DX/target/surefire-reports/TEST-com.giga.nexas.bhe2bsdx.byJinki.TestBheByJinkiRunner.xml) |
| 最终包语义／回环审计v2 | `passed=true`，91,343 checks／0 failures；54入口；115 MEK、20 WAZ均实际generate→parse→generate，完整norm图及字节回环一致；11,542个显式非负WAZ引用在表内 | [audit.json](/C:/NeXAS_DX_runs/audits/nine-six-variants-130704-v2/audit.json) |
| 新旧目录完整差异 | 两侧均3,884文件；新增0、删除0、修改20、其余3,864不变；全文件SHA256，无扩展名排除 | [Astra目录审计](/D:/Code/NeXAS_DX/target/nine-six-variants-build-diff-20261030/audit-final.json) |
| PAC真实解包 | NexasPack exit0；解包3,884文件与最终flat目录逐文件大小／SHA256完全一致，`passed=true` | [独立PAC审计](/C:/NeXAS_DX_runs/audits/nine-six-variants-pac-130704/audit.json) |

目录的20个修改恰为九个角色各自的MEK/WAZ，加 `WazaGroup.grp` 与 `WeaponEquip.dat`。115 MEK、20 WAZ的回环包含继承项；八个共享公共WAZ、Akao／赤緒两文件及Sou WAZ均字节不变。106个非本次九角色MEK保持旧字节；Sou.mek／sou.waz与旧默认行保持。各主WazaGroup的最终param等于上表主WAZ技能数，继承索引表其余行前缀及所有旧AI、Material前缀保全；单角色Material尾slice长度等于最终MEK武装数，三个视图一致，源重复项没有被擅自规范化。日文名称／说明通过严格CP932往返。

语义CLI本身不解包PAC，故其JSON保留 `pacValidated=false`；PAC一致性由表中独立解包审计提供。两项静态审计与receipt都不把 `runtimeValidated` 改成true。

## 旧前缀允许的窄变化

审计对旧MEK字段和旧主WAZ前缀逐项比较，仅承认以下固定身份／位置的批准变化；其它字段、事件和资源引用必须相等。

| 固定身份 | 批准差异 |
| --- | --- |
| Nagi MEK18／`DX_QUICK_DEFLECT` | 仅weaponDescription按下列完整旧文字→新文字修改 |
| 同技能成功P1 | 原无slot36，增加clock0..0、type0、四整数 `(0,0,0,0)` 的armor0；其余成功段载荷不变 |
| Wilhelm `DX_ALTERNATE_CANNON_EXIT` P0 | 原无slot21，增加clock0..0、type2、六整数 `(0,50,0,0,50,0)` 的G50 |
| 同P0 slot27／28 | 各增加clock0..0、type0、四整数 `(0,100,0,100)`；不是0→100坡道 |
| 同P0 slot36 | 原无→clock0..0、type0、四整数 `(0,0,0,0)` 的armor0 |

Nagi旧说明：`短い受付で攻撃を受け止め、成功時はディフレクトへ移る。長く構えず、読み合いから素早く攻めへ戻りたい。`

Nagi新说明：`短い受付で攻撃を受け止め、成功時に斬り返す。反撃後は盾を解き、斬上げなど次の攻めへつなぐ。`

审计未通过全局删除weaponDescription来隐藏差异。WazaGroup仅对九个真实角色行更新技能数；默认装备仅改九角色行，FC／initializer和Sou原行保持。

## 历史别名与v1失败边界

[first audit](/C:/NeXAS_DX_runs/audits/nine-six-variants-130704/audit.json) 的实际结论是91,315 checks／1 failure，`passed=false`，唯一失败为 `emittedWazHasGroup.赤緒.waz`。该失败证据没有覆盖为成功；[v1源码原字节快照](/D:/Code/NeXAS_DX/target/nine-six-variants-artifact-20261030/NineVariantsArtifactAudit20261030.v1-before-legacy-alias.recorded-bytes.bin) 的SHA256为 `4b26797b0311eae07c3fa4039b026d86f50bae6ba30bf9631136c96bb780dc0a`。

只读补证确认032001已同时含 `赤緒.waz` 与 `Akao.waz`，WazaGroup唯一active身份是group110：`赤緒 / AKAO / Akao`、existFlag1、param33。新旧四文件各211,461字节，全部六组pairwise字节相等，统一SHA256为 `5cbe9c3a6f3c107ac01607a0e62095b02106607d4d93993a3131d64958faa85b`；新旧canonical实际解析均33技能。

v2仅增加这一对历史别名的严格合同：四文件存在、固定大小／SHA、六组字节相等、唯一完整group110身份／param33及canonical技能数必须同时成立，才接受 `赤緒.waz → Akao.waz`。其它未登记WAZ继续失败；该别名仍接受源字节保全、引用和完整回环检查。没有新增索引组、删除别名或修改游戏产物。[v2审计CLI](/D:/Code/NeXAS_DX/target/nine-six-variants-artifact-20261030/NineVariantsArtifactAudit20261030.java) 的SHA256为 `65a397d820493bebdfc398e586ff3817e5a9ee3a8199702cdcd2251dfe56f299`。

## 实机状态与待验范围

旧case07仍属于131244旧包：`C:/NeXAS_DX_runs/runtime/combo-capture-recover-20261030-07`。root已通过游戏菜单正常退出；本次实读 [state.json](/C:/NeXAS_DX_runs/runtime/combo-capture-recover-20261030-07/state.json) 为 `status=exited`、`exit_code=0`、`failure=null`、`cleanup_pending=false`，完成清理时间为 `2026-10-30T04:47:06.636629Z`。其PAC为 `a94c6c3550e498fb03cda5c949e06cb93e8b48ce576fbcacd0c5fca3501297b4`，不能转记为本130704新包的54项运行证据。

新case08为 `C:/NeXAS_DX_runs/runtime/six-variants-validation-20261030-08`；[manifest](/C:/NeXAS_DX_runs/runtime/six-variants-validation-20261030-08/manifest.json) 绑定本包EXE／PAC，`2026-10-30T04:50:59.110403Z`启动，PID245000。本次读取 [state](/C:/NeXAS_DX_runs/runtime/six-variants-validation-20261030-08/state.json) 时为running、hook_ready=true、failure=null，`runtime_acceptance=not_evaluated`。root正常UI已看到Hell SETUP与Sou选机；[运行日志](/C:/NeXAS_DX_runs/runtime/six-variants-validation-20261030-08/runtime.cdb.log) 仅留有该菜单边界的 `CASE08_SOUMENU_SELECTED meka=113 preview=20d452d8 player=00000000 TAMA=0 SCRIPT=0`。本次没有由菜单状态推定战斗、全部技能、连段或关卡通过。

仍待本包独立验收：Sou普通菜单0..152逐项技能不闪退；新54项各至少一条不同MEK START、同一目标尚未恢复的真实实效连段／终结连入；命中与落空、普通／SD取消、各关键阶段受击中断后的装甲／重力／倍率恢复、child owner／target及对象回收。旧包的个别成功路线、142项绿色回归和PAC字节一致均不替代这些运行项目。
