# 2026-11-06 暂停交接点

用户明确要求“稍微停一下，固定成果，我启动的时候再重启目标”。这是当前有效指令；此前active状态与继续执行要求保留为历史。目标未完成，等待用户再次启动。本轮只固定成果、关闭游戏及提交，不新增玩法试验。

## 工作边界

所有写入／删除限D:/Code/NeXAS_DX。直接develop、中文提交、不push、不新建分支或worktree。正式代码在src／tools，文档在docs；构建与临时运行产物在仓库target；不提交／上传EXE、PAC、截图、dump、Save、Replay或其它二进制。保留Clarias／AGENT六文件53行WIP、旧sou-menu-bank修改及其它未跟踪资源。

## 已有成果与验证范围

本轮暂停前HEAD为9b74398a2；11/06此前提交6aede1fb4、bc987968d、15b35ed8a、9b74398a2分别固定冷却热实测、Naoto六预览与20、21、22／23连段。详细结果见progress、acceptance-matrix-20261106、naoto-six-runtime-20261106、使用说明。

- Sou133实际减热、普通取消、同场复用已记录；所有条件及全部153武装并未完成。
- Naoto20～25菜单与训练均有实际伤害。20／21／22／23已有固定目标连入与同场复用。24／25新增独立效果和射击顺序对照：031重击起159、033前段272、034重击起302保持有限控制。032存在控制空窗；033晚18和034末1点不算前段，不能宣称全套真连。
- 当前新增源码仅Naoto24日文简介一处替换，明确发射开始额外发热200 raw；未改动作。21／22／24三处文案源码均比当前封包新，下次正式构建须纳入。暂停不重建。
- 新增小型JSON保全029～034关键事件、hash和限制。完整RPM／日志／私有存档仍在case13；不复制大型二进制。Nagi只读范围见nagi-runtime-scope-20261106.md，两个静态风险尚未复现，不据此直接修复。
- D的搜索取消与专用SD路径已静态厘清，见Naoto专项末节。sdGate0不等于不能无方向D搜索取消；不得把恢复后移动计为技能取消。

## 当前唯一构建

latest指向target/bhe-playable-builds/run_20261105_193350_342_bd794139；assets为sou/sou_assets_20261105_193550_582。

- EXE sou/BaldrSky_20261105_193620_157.exe：1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae。
- PAC sou/Update3.pac：8efe934e14fda0d7f9c429f9b8d703a710d3213d15bbfc5ec6fb9f41e40b4641。
- 原EXE为src/main/resources/ida-reverse/BaldrSky.exe，SHA713cf605360a55a863f87bfbdea5453aaeeb2a33983ad4a005c9297e3a86e757；不使用bsdx_game旧宽屏EXE。
- 表中身份读取latest／manifest；关闭时另由launcher按原清单核源内容未变。runtimeValidated仍false，封存产物不改写。

## 暂停会话

case13为target/bhe-playable-runtime/nine-naoto-20261106-13，原游戏PID9552、creation FILETIME134383992159715102，launcher session70253／PID53136。暂停时处于Replay035 LOAD确认，尚未确认或播放；没有启动035采样。加载已正常取消，窗口X打开的退出确认尚未完成；随后停止UI操作者，并由已有受管STOP流程关闭核实身份的游戏。state=stopped、failure=null、stop_reason=STOP_file、exit=E0000001，自有进程／guard已收尾、cleanup_pending=false；PID9552及launcher53136均已不存在。这是用户要求停止，不是自然exit0或游戏崩溃。

受管清理已移除28份game输入副本，共5,805,717,300字节（约5.41 GiB），无保留异变副本和清理错误；原Save／base／源EXE及PAC检查均未变。全会话345条system／unclassified异常仍如实记录，外写拦截计数0、guard违例空；nvAppTimestamps漂移归因未定，不声称系统状态全部未变。退出文本证据见[naoto-case13-pause-exit.json](../../../src/test/resources/bhe-runtime-evidence-20261106/naoto-case13-pause-exit.json)。私有Save、既有日志和唯一构建保留；未创建新运行副本。

## 恢复首步

1. 用户重新启动后先读本检查点、Git和latest，确认仅一个受管运行会话；不复用已结束case／旧lock／PID。所有限制继续有效。
2. 沿现有tools/bhe_runtime_session.py及原manifest的启动配置，新建唯一case（先查候选目录不存在），最新EXE/PAC使用原固定构建。真实Save仍是只读保护基线；私有seed优先case13/docs/GIGA/BALDRSKY_DIVEX/Save。不要把私有seed传作真实Save。
3. 优先接续Naoto035长按：Replay028正常UI模板840B，SHAdcf5912e183e05270cf3d1d27fe18391bd67af48b8f56b367059bdf836502063；前三装备nearA23、普通Dash B24／C25，其余空。035已生成843B，SHA1e827650d74bd7998400e1a6564dea213ec146e01cee186a45d6c8e9c5bceec1；B[30,200)、480帧、tail200。先复核文件hash，只正常LOAD／PLAY；重新绑定新PID／creation／image后才采样。033／034完整数据留原case，不能拿新进程补旧窗口。
4. 036未生成。待审路线为A30/C68/B156，再正常D420..424接近，第二轮A600/C638/B726，约1250帧；仍须实际验证，不能预填成功。fixture只改第三普通输入块，前两块／种子／actor不可强写。旧标题时长不代表新输入长度，按槽号选择。
5. 补Naoto落空、D取消、受击中断、24／25复用，再按矩阵推进其它八角色六武装、Sou153预览及实际使用、133剩余条件、资源重载／关卡推进、最终乱码审计和中文说明书。Sou34蝙蝠CENTER／三维slot23问题见sou-bats-motion-review-20261106.md，尚未实施；不能全局23→18或改成追敌跳跃力。

## 采样与控制注意

原生输入帧只认combat_replay_input.frame；RPM counter、clock和lifetime不同。replay consumer是固定对象0x86F700，active+159D／counter+15A0，不是该地址保存的指针。进程读仅VM_READ／QUERY，先核完整路径和创建时间。保留拒读和非原子读边界，不能将后读HP覆盖native伤害前控制。Sou专用请求／READY匹配只覆盖Meka113，Naoto不能借用五链结论。

下一运行进程需新身份绑定，不能盲用会话内固定PID的临时采样脚本。若恢复会话中的functions.store仍有nexas_projectile_sampler，只作读回参考并先重绑身份；正式持久采样工具须放tools，不在target写代码。原采样等待PLAY上限600秒、实际播放60秒；先启动采样，再由唯一UI操作者确认LOAD。UI仅通过computer-use，悬停刷新确认高亮后再点击，主代理统一发令，避免多代理抢输入。

## 暂停提交的验证

没有重新构建或补跑玩法。10份待固定文件通过UTF-8无BOM／CRLF／文本检查，三份JSON可解析；冻结日志前缀hash一致，共208个保留原生对象与原日志对应行逐对象相同。033／034的输入、receipt、采样原件及完整事件窗口hash已重核。新增三份运行证据共272895字节，不含二进制。

Java当前差异经Git换行规范化后，可逆还原为HEAD的唯一一处字符串替换，动作逻辑无其它变化；新日文49字符／CP932 98字节严格往返通过。035文件hash／843字节已核，未生成其采样输出。范围内git diff --check通过。

最终另增加4327字节退出文本证据，四份新JSON合计277222字节；11份提交文件复核编码／文本属性，既有七份WIP内容hash未变。
