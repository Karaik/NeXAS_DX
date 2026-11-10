# 2026-11-10 暂停交接

用户已将目标设为paused；当前只完成停止和成果固定，等待用户重新启动。全目标未完成，runtimeValidated=false。所有写删仍限D:/Code/NeXAS_DX；develop中文提交，不push、不建分支/worktree，不提交二进制；七份原有WIP保留。

## 已固定成果

上一实机成果提交为b08ab02d9及cbcfe14d6：Naoto六招均有实际D搜索冲刺取消，23地面远距、24/25正常跳跃靶分别有完整落空/自然收尾证据；保留远距射击仍命中及射击连段尾弹空窗。九角色164原有入口候选清单独立于54新增及Sou153，尚非菜单/运行全部通过。

本轮新增的M布局已正常UI配置并保存Replay055：玩家109／敌0／map68／行为0，只有普通Dash A20/B21/C22，其余11槽为空。855字节，SHA cad21998372b157c8dbe99c0cce086e7960e7e7b01c1c8f1bc5a9102877ebf42；1980输入对全0，头和前两编码块809字节，SHA 5fb93d28fc8a857c10559e6f9233830a7b845e7decc83a9f8558002b39e4d8ca。

056/057/058只替换055的第三输入块：各1200帧，left[30,140)，对应A/B/C[200,400)长按，再于[650,654)短按同键，尾500帧。分别用于20/21/22的远距空挥、长按与自然结束后再用。输入/receipt位于case15根目录，Replay在私有Save；三份全部输入、前两块及hash已核，但**都未播放**。

- 056：873字节，SHA1c6d913437ea2d3dbd0a0b4ab4cecac378872d4b23f9e32f999600d777ccf567。
- 057：874字节，SHAc7bef201a17d500648fe6645216e76e047f3da5a22447a996ef481859f34bae9。
- 058：874字节，SHA47d83bb6965378ea19575e6ab323c33d629f4f57ee7625ab7c22e25dc4bdba01。

## 停止与清理

case15为target/bhe-playable-runtime/nine-naoto-20261110-15。原游戏PID152620／creation FILETIME134387632535477302，launcher85485／PID149256，均已确认退出。暂停落点是056 LOAD确认，未点Yes；暂停边界上启动的采样等待session24579已结束，started=false/0rows，记录为因用户停止造成的process_error，不是技能崩溃或已执行试验。

受管STOP结果：status=stopped、failure=null、exit=E0000001，owned exit/guard cleanup完成、cleanup_pending=false。自动清理28份输入副本，共5,805,717,300字节（约5.41GiB），无清理错误；原Save/base/source检查未变。保留私有Save、日志、输入/receipt及唯一封存构建。全会话11条system/unclassified异常保留，外写计数0、guard违例空；nvAppTimestamps变化归因未知，不宣称整个系统状态未变。

新增[暂停与M布局文本证据](../../../src/test/resources/bhe-runtime-evidence-20261110/naoto-case15-pause-and-m-layout.json)约12.4KiB，包含精确055～058身份、输入校验、0行采样及退出状态，不复制任何二进制。

## 恢复后的入口

1. 用户明确恢复后重核Git、latest和实际进程；不得复用已结束case15或删除旧lock。当前封存包仍run_20261105_193350_342_bd794139，EXE1ac677…／PAC8efe934e…；不使用bsdx_game旧宽屏EXE。
2. 新建唯一受管case（先核候选目录为空/不存在），私有seed取case15/docs/GIGA/BALDRSKY_DIVEX/Save；真实Save仍为只读保护基线。不要把私有seed传成真实Save参数。
3. 优先正常LOAD056。先用tools/bhe_runtime_sampler.py按新case身份启动新输出，end-counter1200，再通过UI确认PLAY。完成后独立核两次START、完整Hit窗、无伤、自然收尾/复用，不能把未启动或中断算落空。随后057/058。保留本次0行等待输出，不覆盖旧文件。
4. 继续正常UI弱敌/CPU行为3模板、来袭基线及真实中断；然后其余角色、原164候选菜单范围、Sou153、重载/关卡、乱码和最终说明/封包完整验收。Naoto21/22/24源码说明还没有进入当前封存包，后续正式构建需纳入。全部目标未完成，不可因本检查点停工而标complete。

UI经验：浮动底部按钮有时点击不生效，完整右键菜单里的正常按钮可靠；先hover刷新再click。F键快捷键发送可能未生效，不盲重按。现在没有仍在执行的UI子任务/构建/采样；用户未重新启动前不继续。
