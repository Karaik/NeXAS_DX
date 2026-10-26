# Freja原生热充能链核对与最小入口调整

日期：2026-10-27。结论：保留并优先验收现有FC `武幻剛乱舞`，不新增变体。普通 `スピア` / `ラッシュ` 没有热量条件；此前速度、攻击与热耗调整不构成热循环。FC本身具有 `CHARGE_FREE` 条件、消耗和计数链，且这些控制数据已经完整写入本轮最终字节。

本次没有运行Maven、游戏或子代理。使用现有 `target/classes` 与runtime classpath，通过JShell只读解析原版资源、作内存转换/Term重编译及调用既有FrejaCustomizer，再读取最新平铺产物比较；没有生成临时资源或修改游戏输入。只写本报告及经主代理明确授权的最小说明/预设改动。

## 原版、当前转换与最终字节

主包：`target/bhe-playable-builds/run_20261027_002200_048`，PAC SHA256为 `2c93f7caf599fc160e41b28e4c4fa313a2a7c212dd68c669ba91adc365d349a7`。精确平铺目录：

`target/bhe-playable-builds/run_20261027_002200_048/motoki/motoki_assets_20261027_002350_382`

| 输入/产物 | SHA256 |
| --- | --- |
| `game/bhe/mek/freja/freja.mek` | `62AC2977E48532C7FB9AEE2FD8B357DA46699D9380557C34B7C288AA2169F4F3` |
| `game/bhe/waz/freja/freja.waz` | `21CBE183EE4103D870A4459C377DB14B92C5D3F066972BE26A4CE6D3F18C4D59` |
| `game/bhe/grp/term.grp` | `FE62811AE9794DF353F538C756C72FCA76641BD21CC622533EBFA5418E722C49` |
| `game/bsdx/grp/Term.grp` | `1CA452EF1956927FCCC129B9C030203F99D4CA3074DC5431D6D65C07685E13E3` |
| `game/bsdx/waz/Kou.waz` | `8179817D6755CCC69E77C60FDEF2483FB05A9CFC723BF2250CC1B7529E10B03E` |
| 本轮平铺 `freja.mek` | `E5159057C05C19D61A7FD818CD104C2C73EA5DFA5AD7A2A9706706A2F4B46442` |
| 本轮平铺 `freja.waz` | `685F6131EB7C2103E4630F8E7816C72540C9E96E947E072F20A4BB5AF8682F1C` |
| 本轮平铺 `wazagroup.grp` | `7EA1BBFD7C283C475066EBEB6E538D7BCCC68BB1800C3F9CBDB3CAF658AA190F` |

前五项资源路径相对 `src/main/resources`；后三项相对上述平铺目录。

最终MEK中的FC为weaponId12、`武幻剛乱舞 / 0007`、category1、identifier1、heatMin/Max80/80，指向当前WAZ26 `武幻剛乱舞 / 0001`，8 phase。主WAZ group为113，`WazaGroup[113].param=32`，实际最终WAZ也为32 skill。

将原版Freja WAZ经当前转换和Term重编译后的FC，与平铺 `freja.waz` 对比：全部8 phase的slot35计数、slot50 Charge及slot71 Change共23事件、16槽组，去掉offset/length/slotNum/说明等非协议记录字段后全部一致，mismatch=0。此比较核对了完整collection及嵌套Charge载荷，不是只看事件数量或解析成功。

审计包的FC武器说明为空，默认FC仍是 `叛天轟瀑斧`（id11）。本报告之后授权的说明与默认入口改动尚待重新构建，不能把旧包hash称为包含新说明/新预设的包。

## スピア与ラッシュ的实际边界

- MEK `スピア / 0003` → WAZ `スピア / 0002`，源18、5 phase。当前P3/P5速度与攻击按既有Customizer增强：攻击150→180，热耗24/24。实际Change是接地分支与阶段/结束跳转，没有 `HEAT`、`HEAT_FREE`、`CHARGE_FREE` 或通用计数条件，也没有Charge消费节点。
- MEK `ラッシュ / 0002` → WAZ `ラッシュ / ROD02`，源17、6 phase。其Change同样为接地分支及阶段/结束跳转，没有上述热量或计数依赖。原P1/P2/P4/P5攻击参数分别50/100/100/50，未由本次热核对改变。
- 因此，active channel0恢复不会自动给这两招增加热量派生；也不能把普通武装热耗提高解释成“高热增加连段”。

## 原生Term与Charge含义

BHE、BSDX原版 `term.grp` 均直接解析确认，GUAGE组13的item1为 `HEAT`、item2为 `HEAT_FREE`、item3为 `CHARGE_FREE`。它们是三个不同节点。该FC使用item3，即 `空きヒートチャージ`；没有把它当作普通 `HEAT`。

目标条件 `root0/type[2,5,3,3]` 的原生路径为 `PARENT/OBJECT1 → OBJECT1/GUAGE → GUAGE/CHARGE_FREE → COMPARE/EQUALOVER`；参数20/40/4、对象`[0,0]`及int2=0均保留。计数条件 `root0/type[2,0,0,4]` 对应 `PARAMCOUNT → COMPARE/UNDER`。原始flag、int1和同帧事件顺序也保持，不另行发明逻辑模式。

原版Kou `ドリルLv1 / DRILL01`（源30，3 phase）的P2也实际包含 `CHARGE_FREE < 2` 条件：`root0/type[2,5,3,4]`、param`[2]`、objects`[0,0]`，Change窗口10..155；同阶段slot50在11f保留Charge单位0的`(0,0,0,200)`。这是热查ージ持续/退出的原生对照，不能把其条件误标为普通HEAT。这里只使用对照语义，不借Kou技能或外部资源。

BHE读取 `Term.grp` 大写文件tag首次触发工具分派的 `unsupported .grp file for parsing: Term`；改用同一Windows文件的小写路径 `term.grp` 后成功。该工具异常没有作为游戏崩溃或游戏结论。

## 武幻剛乱舞的已保留控制链

phase以1起算，phase index按0起算。以下是字节中的条件和跳转，不把静态路径表当作实机打数保证。

| 阶段 | 计数/消费 | 资源条件及目标 |
| --- | --- | --- |
| P2 | 0f active0四整数`(0,0,1,1)`；Charge单位0`(0,0,0,2000)`、单位1`(0,0,0,0)` | 24f的两条件节点：计数`<4`、`CHARGE_FREE>=20`，跳phase index2即P3；同帧另有`CHARGE_FREE>=40`跳index3即P4；81f保留原生结束。 |
| P3 | 与P2相同的更新/Charge载荷 | 24f同组条件跳index1即P2，另有`>=40`转P4；75f原生结束保持。 |
| P4 | 0f active0`(0,0,0,0)`重置载荷 | 14f进入下一阶段。 |
| P5 | 0f active0`(0,0,1,1)`；Charge单位0`(0,0,0,400)`、单位1零 | 8f计数`<10`且`CHARGE_FREE>=4`的原始两条件节点跳index4即P5；20f另有`>=40`进入下一阶段；72f结束保持。 |
| P6 | Charge单位0`(0,0,0,4000)`、单位1零 | 51f原生下一阶段。 |
| P7/P8 | P7原Hit59..142、直接攻击值200保持 | P7接地相关派生及P8的111f出口保持。 |

P2/P3原Hit分别7..13、6..12，直接攻击值50；P5为9..20、直接攻击值10。每次命中、补正和实际持续时间仍由游戏执行，不从这些常数直接计算最终总伤害。

active channel0修复确实让三次非零更新和P4重置从错误的全零降级恢复为原载荷，使原来的计数阈值/分段限制有可用的数据基础。它恢复的是控制正确性，也可能纠正原先错误的过长循环；不能一概宣称“恢复计数就增加打数”。它没有新增热量条件、改变Charge、恢复全部四通道，29项既有回归也未代替本FC的运行效果验证。

## 最小裁决与本次授权改动

主代理裁决：使用已有FC链，不新增变体或Term编码。

1. `FrejaCustomizer` 在已有批次写入之前，通过WAZ双名/8 phase和MEK双名/真实引用定位该FC，仅追加日文说明setter到现有原子批次。热耗、Charge、计数和全部Change不改。
2. `PlayableWeaponLoadouts` 仅把Freja默认FC由 `叛天轟瀑斧` 改为 `武幻剛乱舞`。原两种FC均保留可选，不改变武装号/分类或其它装备槽。
3. 使用说明区分普通スピア增强与FC热查ージ链，明确普通热条不等于阶段入口的实际可用预算，低/高入口free实测待验。

日文说明：`ヒートチャージの余力を使う連撃。使用可能な残量に応じて、連撃の継続や締めへの移行が変わる。`

## 必须补的实机对照

- 以进入P2/P3或P5、原生条件实际读取到的 `CHARGE_FREE` 为基准记录低/高预算。不要只把出招前普通热条低/高当作预算值，FC入口与消费可能改变它。
- 对照低于20与达到20的P2/P3阶段，记录是否发生互跳、计数变化和进入后段/原生结束；再对照P5低于4与达到4、退出循环后是否仍达到40的情况。
- 记录实际打击次数、最终伤害、可用预算与Charge消耗、是否到达P7强打，以及结束/中断后能否正常恢复。相同位置、目标和连段状态下比较，保留最终EXE/PAC hash。
- 目前只能确认字节及语法链完整。四整数更新的实际数值运算、预算单位与屏显换算、同帧Change调度顺序和真实收益仍需游戏观测，未据原版简介或静态图宣布完成。

本轮低影响说明/预设改动不增加镜像常量测试。主代理负责现有Freja6项与Loadouts5项集成回归、重新构建和实机验收。本代理已从当前Freja源码读取实际字符串，用Java `windows-31j` encoder/decoder严格往返核对，包含新说明的7个非ASCII游戏字符串全部无损；没有仅断言源码包含某句文字。
