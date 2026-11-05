# 2026-11-05 暂停交接点

用户明确要求“稍微停一下，固定成果，我启动的时候再重启目标”。本轮仅完成关闭测试进程、整理证据与提交收尾；目标未完成，等待用户重新启动后继续。所有工作仍限 D:/Code/NeXAS_DX，直接develop、中文提交、不push、不建分支或worktree，不提交／上传游戏二进制。保留原Clarias六文件53行WIP及既有sou-menu-bank修改。

## 已固定的源码与测试

- 方向页图片收集与ACTION_WAZA实际映射已提交，继续保留缺图片仅审计的兼容行为。Sou123原767A15空surface崩溃所需真实PNG已入包。
- 受管构建比较只忽略目录mtime，所有文件hash／mtime及路径／类型／源保全／消费者／使用锁仍严格；28项回归通过。旧产物自动退役已在实际整链恢复验证。
- Sou133回收协议使用私有本体／冰片，原parent多通道握手改为原生父阶段快照及NULL优先，保原预算与结束时长；源码10项及相关52项已通过（现含冷却体扩展）。
- Sou122三等级各复制自己的本体，只删被误译的P1 END-OR，保留正确NOT-AND、149f上限、P2 40f及各级参数。新增8项与相关52项通过，状态3和4已有实际生成／回收证据。
- 最新冷却补偿提交为fcdddea36：第三私有冷却体复用DX原生slot40单次0f负热事件，每个冷却体减100 raw，原每3f发射频率与预算保留。触发为冷却体生效，区别于BHE命中确认。10项专用、6类52项相关回归与完整构建均通过；**该减热补偿尚未完成实机对照**。
- 编码增量审计已固定：986个跟踪源码，984个与基线hash相同，仅热修两Java变化且新增日文正常；旧未知???和未改动LF文件明确保留边界，不据此改写未知语义。

## 当前唯一构建

`target/bhe-playable-build-latest.json`指向：

- run：`target/bhe-playable-builds/run_20261105_193350_342_bd794139`
- assets：`sou/sou_assets_20261105_193550_582`
- EXE：`sou/BaldrSky_20261105_193620_157.exe`
- EXE SHA256：`1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`
- PAC：`sou/Update3.pac`
- PAC SHA256：`8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641`
- 原EXE仍是`src/main/resources/ida-reverse/BaldrSky.exe`，SHA `713cf605360a55a863f87bfbdea5453aaeeb2a33983ad4a005c9297e3a86e757`；不使用bsdx_game中的旧宽屏EXE。

完整构建1项、0失败／错误／跳过，382.495秒、exit0，旧受管构建已自动退役。3889个成品文件，相对f8e中间包只有Tama05和WazaGroup不同，其余3887字节一致。Tama05新增544冷却体，542 P1冷却引用529→544，GRP行5数量544→545；还原这些后完整旧图语义hash一致。当前receipt仍`runtimeValidated=false`。

## 实机已证明的范围

旧包130项variant0菜单自然闭环与后续包结果分开保留，不能跨包直接累计成当前153项通过。

- case09（959f包）：新20项菜单闭环为123～132、142～151，150／151只有no-hit closure。121两轮实际装备／再次部署、123部署＋后继攻击及热状态／对象回收已有记录；122真实冲刺中提前退场已复现，133冰片等待未回收已定位。
- case10（f8e包，含回收及冲刺修复、未含新减热）：133首REQ1／START1，tick30944→31190，21次伤害合39，READY双空闲、TAMA/SCRIPT=0。122同输入下生成横向4/257并造成150伤害，root停止状态3后正常退场；方向＋D实际进入状态4，落空但生成及回收正常。方向双击仅状态2，不冒充状态4通过。
- case10冷却对照：Replay013普通输入B30、A90～135、C150，实际正常取消133接2；伤害120/2/142，敌HP5736，不认定真连。旧包heat300→2300→3000，私有本体工作期间没有减100；counter477才进入自然冷却，随后每步400降至0。实际capacity=8000。此为热功能缺口基线。
- RPM采样与native事件严格区分；counter可能比实际输入帧领先，非原子读、拒读和未初始化瞬态均保留。case10最终正常exit0，但全会话457条system/unclassified异常及Drs漂移仍如实记录，不称零异常或自行归为无害。

证据均为`src/test/resources/bhe-runtime-evidence-20261105`的小型文本，不含PAC／EXE／截图／dump／录像二进制。

## 暂停时的进程与产物

case10已正常退出并清理28份5,805,717,209字节game副本。最新case11为`target/bhe-playable-runtime/sou-cooling-heat-20261105-11`，刚启动即因本次暂停请求写STOP；PID969268已退出，status=stopped、failure=null、stop_reason=STOP_file、exit=E0000001，owned exit和guard清理均完成，cleanup_pending=false、guard违例为空。28份5,805,717,300字节game副本已自动清理，无错误；原Save检查未变。这是用户要求停止，不是游戏自然崩溃或热修验收成功。

所有子代理已完成，无正在执行的构建／测试；当前源码成果均通过中文提交固定。最新构建及私有Save／小型日志保留，不重建C盘运行目录。不要删除launch.lock或复用已结束case。

## 用户恢复后的第一步

1. 先读此交接、当前Git与latest receipt，确认目标已重新active，保持上述写入／分支／二进制限制。
2. 准备新的唯一case（例如`...-12`，先查不存在），从当前latest EXE/PAC启动。`--save-dir`仍指真实原Save作为只读保护基线；`--save-seed-dir`优先使用已退出case10的`docs/GIGA/BALDRSKY_DIVEX/Save`。该种子清单见`target/bhe-combined-fixes-20261105/case10-save-seed-manifest.json`，90文件4,237,007字节。不要把私有种子当--save-dir。
3. Replay010为正常UI保存的133模板（869B，SHA `2d40ccd31d9cad5535cc30285fdbc30345c1f81449fcbb3bbab802923dd938bf`）；Replay013及`case10/sou133-heat-baseline-input.json`是同输入热量对照，先重核其receipt／hash。保留前两块，只用普通Replay按钮输入，不强写actor。Replay006已于早前私有UI操作发生变化，不能拿它替代旧122输入；122固定输入为005模板生成的009。
4. 首先复测新冷却体每次减100 raw及零下限，比较同输入基线，区分自然冷却。继续检查正常取消／再次使用／受击中断及所需方向、距离、变体。
5. 完成剩余Sou153菜单与实际使用稳定性、九角色各六追加武装的实际效果、真实跨武装连段、落空／取消／中断、重载和关卡推进；完成中文说明书和最终整包审查后才能把runtimeValidated改为true并结束目标。

界面操作经验：游戏有时在鼠标移动后立刻点击时沿用旧悬停项。先用`sky.scroll(..., scrollX:0, scrollY:0)`移动并刷新，确认目标高亮后下一次工具调用再点击；尤其保存／载入／退出。不可把这种误触或未归属请求切换当作技能通过。只用computer-use接口操作UI；原生调试仅按已确认进程身份只读，所有输出限仓库。
