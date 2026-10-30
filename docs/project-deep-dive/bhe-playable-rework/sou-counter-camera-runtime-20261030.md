# Sou30／Sou33新包菜单预览复测：case09原始证据冻结

> 2026-10-31保全更新：原target冻结目录已清空；当前可用来源是仓库内[完整运行文本](../../../src/test/resources/bhe-runtime-evidence-20261030/records/runtime/sou-counter-camera-validation-20261030-09/runtime.cdb.log.json)。本次只读重建文字字节后，前411254字节及[31567,411254)片段的SHA256仍与下表一致，REQ33／武装30与REQ77／武装33的原READY标记均存在。原图、转储及程序没有转存，本文的看图和运行结论属于当时记录，不是本轮重新运行或新源码验收。

结论：新包中Sou30「人間砲弾」默认level0预览、Sou33「カワラブレイク」预览各有同一REQ对应的原生START、收招和READY证据。本结论仅覆盖这两个预览，不代表Sou153项或九角色54项追加技能全部通过。

本代理独立只读核验，未执行Maven、Java、UI、CDB，未改写活日志、Source、Save或输入。冻结主目录为 `D:/Code/NeXAS_DX/target/sou-counter-camera-runtime-20261030`；机器索引见audit.json，逐条原生记录见observations.json。审计时间：2026-10-30T10:22:47.525249+00:00。

## 包身份与原字节冻结

case：`C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09`。

build收据：`C:/NeXAS_DX_runs/builds/run_20261030_180259_554/build.json`。

运行目录实际EXE完整哈希为 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`；实际Update3.pac为 `27721046fdaaee3537a1b3c66cdba3a561a1fc1c373dd3f1890a26a802d835ca`，1,460,489,372字节。均与case manifest、build收据一致。

只匹配独占一行的裸 `CASE09_SOU30_SOU33_RETEST_BEGIN/END`，不把.echo命令回显当标记。冻结源字节范围为 `[31567,411254)`，包含标记及END行换行：

| 冻结文件 | 字节数 | SHA256 |
| --- | ---: | --- |
| native-retest.raw.log | 379687 | `bc180d11726eabd67b4dbb0170d1cf4c4e17e64054daecebb0c70661a11d5987` |
| runtime-prefix-through-end.log | 411254 | `c924cb513a49cddf56ce8937871fb9d61ec139224d01eb7f54dce1d94444e9a1` |

二读时间为UTC `2026-10-30T10:12:23.003005` 与 `10:12:23.925751`。活日志从534228字节增长到534752字节，但截至END的411254字节前缀逐字节一致；写报告前再次核对该前缀未变。原日志按字节冻结，不规范化其换行；新报告为UTF-8无BOM、CRLF、单个EOF换行。

## 正式预览的请求关联

| 武装 | REQ | 请求tick | START tick | 收招tick | READY tick | 请求／START至READY的tick差值 |
| --- | ---: | ---: | ---: | ---: | ---: | ---: |
| 30 | 33 | 62345 | 62346 | 62443 | 62444 | 99 / 98 |
| 33 | 77 | 68754 | 68754 | 68926 | 68999 | 245 / 245 |

两轮均为variant0、actor `2166d360`、group120；READY时skill0、Z0，玩家和敌机active均-1，obj1/obj2均0。REQ、START序号、actor和武装逐项一致，未使用另一轮READY替代。

Sou30正结果helper墙钟2933ms、日志窗口 `[68450,69335)`；Sou33为4766ms、窗口 `[106505,116093)`。后一个窗口起始包含被替换的REQ76／武装30，必须按REQ77／武装33筛选。helper墙钟包含轮询及取图开销，不能等同纯游戏执行时间。tick差值采用减法：START至READY差98表示包含首尾99个tick值，不混用两种口径。

Sou33原图已实际查看，显示 **2 HIT / 1 CHAIN / 306 DAM**；READY敌机HP5694/6000，与306伤害相符。Sou30截图仅证明可见菜单状态，完成判定来自原生READY。

## Sou30释放编码与自然收尾

REQ28..31逐轮观察到实际私有控制弹group5、skill539。child、owner、created_seq/current_req均闭合；对象地址会复用，不能跨REQ拼接。

| REQ | child | owner | P7应用tick | 子删除tick | 父收招tick | READY tick |
| --- | --- | --- | ---: | ---: | ---: | ---: |
| 28 | 3609a020 | 2f1b5be8 | 61624 | 61690 | 61698 | 61699 |
| 29 | 36c9a018 | 2166d360 | 61773 | 61839 | 61847 | 61848 |
| 30 | 36c9a018 | 2f1b5be8 | 61922 | 61988 | 61996 | 61997 |
| 31 | 36c9a018 | 2166d360 | 62071 | 62137 | 62145 | 62146 |

四轮child都经过P0→P5→P6→P7，P7前counter0，P7 slot35 clock0实际 `work_value=16 / counter_after=16`。子弹在P7 slot71 clock65发出原生删除请求，counter仍16；此时父技能已到P13、clock31，随后P13 clock38正常收招并READY。**父处于P13是收尾中间态，不能写成仍卡P13。**

REQ28请求／START61601至READY61699差98；REQ29..31请求至READY差99，START至READY均差98。offset67910记录 `bd 6 7 8 9`，正式REQ33在停用详细控制弹断点后完成。详细因果与较低观察开销的正式预览是同包不同轮次，分别保留。

## 保留初始helper超时

原记录为 `requestedWeapon=30, closed=false, elapsedMs=25395, logStart=51123, logEnd=53663`。这2540字节属于REQ28，只到子弹删除后的父P13 clock31，确实尚无READY。父收招从offset53663开始，同REQ28的READY位于offset54014。故原墙钟deadline确实到期，而后续同一原生周期自然结束；这是详细断点下的观察性能／期限边界，不能认定永久卡死，也不能倒称当时已READY。

原图 `sou-preview-030-1793354020995.jpg` 未改。按root提供的来源，helper先将该对象加入同一persistent JavaScript会话的 `sou09Records`，再抛异常，后续单条JSON写入未执行。root后来原样导出整数组 `sou-ui-records-through-038.json`；导出文件mtime为UTC `2026-10-30T10:15:37.734856`，这是后续导出时间，非原超时发生时间。本代理二读并按字节冻结整数组；不制造原生事件或补造原单条JSON，数组中的34..38记录不纳入本报告验收。

## Sou33相机记录按REQ隔离

正式REQ77共有 **62条** SOU33_CAMERA_OBJECT，全部 `target=player=2166d360 / target_readable=1`：

| 相机Work地址（日志event字段） | tick范围 | 条数 |
| --- | --- | ---: |
| 202e2f40 | 68774..68805 | 32 |
| 36f2de80 | 68806..68835 | 30 |

首组首次出现于START后20tick，第二个相机Work随后执行，最后正常收招与READY。event字段为相机Work地址，不能当作源码事件文件offset。初汇“64条”混入翻页经过武装33的REQ10两条；完整正式REQ77只能记62条，冻结段其它自动预览REQ不并入该周期。

这支持本次对象相机路径避开旧NULL目标故障。实现仍为已接受的“位置快照→实时对象”兼容取舍，不证明原BHE镜头像素／跟随效果完全等价。

## 异常与验收范围

冻结BEGIN..END段未见Access violation、C0000005、CRT_INVALID_PARAMETER或观察器语法错误；两次80000003是主动调试停顿。case状态快照另有6条UTC09:43:10的KERNELBASE system／unclassified exception，不能称“整个case零exception”；快照guard_violations为空。旧case08已第二次异常后以C0000005自然退出，其资料保留，不再称仅first-chance暂停。

结构审计 `C:/NeXAS_DX_runs/audits/sou-counter-camera-180550/audit.json` 记录12948项检查、0失败；本次冻结报告并重读三项变化文件核对哈希。PACK审计 `C:/NeXAS_DX_runs/audits/sou-counter-camera-pac-180550/audit.json` 记录真解包3884文件且expected/actual清单一致，归档哈希匹配本case实读PAC；本代理未重新解包。206项定向测试由root另行冻结，本次未重跑或重新统计。

Sou30仅覆盖默认level0，尚未证明L1..4、训练连段、中断或死亡路径。Sou33只覆盖本次菜单预览。Sou153项、九角色54项追加技能及完整玩法验收均未完成；build receipt仍为 `runtimeValidated=false`。

## 原结果与源图SHA256

这些原文件均按字节复制到冻结目录；截图不替代原生完成判定。

| 文件 | 字节数 | SHA256 |
| --- | ---: | --- |
| sou030-fixed-preview-ready.json | 271 | `bb92914cd4d04f4b1ec852ccca66f839c8781c03a87007ee412e57ef44ac8bcb` |
| sou033-fixed-preview-ready.json | 273 | `0867e1917992797f484d0b894dd59e6cf99eaa474e18fcf46916279913b40cca` |
| sou-preview-030-1793354020995.jpg | 244765 | `0e30656c09249ef19985e799022eb4f6cbb6422e90f4a2623a9b68cc06ec3b01` |
| sou-preview-030-1793354433626.jpg | 242036 | `dbf287dcd136b0f075816106e09be6cae94e3eb387d2c3a98500493e7df501fe` |
| sou-preview-033-1793354543167.jpg | 249597 | `d5fad99c8c917d41cb7dc94485bcefafd1dd0df358a6d392bb1746f63ff1eba5` |
| sou-ui-records-through-038.json | 2272 | `53a7f0fc93bdec264040af073e1cab659bc0b6b415bea8efa22a80335640867b` |

全部副本哈希、读取时间、原记录来源及事件offset见freeze.json、initial-timeout-provenance.json、observations.json与audit.json。活日志、Source、Save、输入和旧故障证据未改写。
