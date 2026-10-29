# Motoki自主起飞投弹运行记录

## 当前身份与边界

构建`C:/NeXAS_DX_runs/builds/run_20261029_095154_853/build.json`，EXE SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`，PAC `196bcd21d0645240a392dd9bf0cde8516fb8861e93e54a902804a4da36934ae3`。新隔离case为`C:/NeXAS_DX_runs/runtime/motoki-flight-suffix-20261029-02`，私有存档和原Save保护沿既有harness；旧case／旧包不覆盖。

此包含四block、四条独立SkillSuffix、恒100倍率端点及落地gravity50。后补的供体suffix拒绝guard与router独立单入口已经39项回归通过，实际重生成此包Motoki MEK/WAZ逐字节相同，证据`target/bhe-audit-20261026/motoki-suffix-guard-byte-identity-20261029/byte-identity.txt`；无需把不同源码身份偷换成不同二进制。

以下是固定甲靶装备预览，不能替代训练、移动靶、中断和跨武装连段。整体`runtimeValidated`仍为false。

## 先前失败包与修正

首个四block包093012的PAC为cfc673…，实机只离陆后回READY，多轮零伤害。`60E9C0`先把TURN/INPUT参数作为SkillSuffix入口，再取入口第一整数作为实际block；原执行图沿用单(0,0)，因此入口1越界，未进入投弹。旧模型直接param→block，导致静态GREEN漏错。

同轮原生读取重力倍率6，`CEventVal(0,0,0,100)`在0..239是0→100插值；恒100应为(0,100,0,100)。真实失败测试分别为suffix8/5/0/0、modifier1/1/0/0，随后修复。原观测将hex160误当decimal160坐标、将actor1502当跳跃倍率的打印均废弃：坐标为+A0/A4/A8，gravity倍率+177C，jump倍率+1780。错误日志保留，不作几何证据。

## 新包实际执行

正常UI进入地狱、选Motoki112、打开装备、选择新17「集中爆撃・帰投」。CDB仅软件断点观察，不改actor、EXE、位置、输入或计数。

| 事件 | 玩家／目标与实际状态 |
| --- | --- |
| START17 / block0 | 玩家(2202,1808,0)、身高144；目标(2502,1808,0)、身高72；active17 |
| block1准备 | 玩家Z168，原Vz8192，gravity0，倍率均100；中心差168+72-36=204 |
| block2入口 | 玩家(2256,1770,168)、目标仍Z0；Vz0、gravity0 |
| Effect实际消费者648A00 | 玩家(2267,1765,168)、身高144；目标(2502,1808,0)、身高72；中心差204 |
| 首轮真实伤害 | 150、136、121，目标HP6000→5593，总407 |
| block3落地 | 玩家约(2928,1988,168)，随后下落；非仅退出动画 |
| 61CC30实际释放 | block3、Change本槽clock36或37、玩家Z0、Vz0、gravityRaw51200（标量50）、两倍率100；尚未到360超时 |
| 5FF58F原生READY | active=-1、玩家Z0、TAMA=0、SCRIPT=0 |

随后多轮真实发弹及命中，有3至6hit差异，来自原六枚弹体散布与命中补正；不承诺固定407伤害或每枚必命中。至少一次六击伤害为75+150+136+122+107+91=681。结束样本均在实际地面、clock36/37释放，弹体和SCRIPT归零，无全局清理或强制END。

观察脚本`target/bhe-audit-20261026/motoki-flight-observe-20261029.cdb`，当前完整日志为case下`runtime.cdb.log`。`60FCE0`入口打印是执行本轮速度／重力事件之前的状态，因此block3入口gravity0与随后落地gravity50并不矛盾；实际释放值由61CC30另行读取。

## 仍需完成

正确出装并在训练用原生Replay取得不同武装START、同一目标受击控制持续和资源变化，证明前技→17→后技；另验收中途普通取消、SD取消、被击中后的重力与形态恢复，不以新17自己的六次命中当跨武装连段。移动／空中／矮小目标、落空与加载调度、九角色整体矩阵仍继续。
