# 九角色产物复审与实机边界（2026-10-27）

初轮记录对应代码`c27bc14fb..89b592a90`及构建`run_20261027_014635_068`，排除Sou。初轮资源技术复审没有发现Important级可行动问题；后续操作审查另发现并修复默认装备分组问题，见文末。两者均不等于游戏验收通过。

## 最终资源独立复审

复审读取当前源码，并使用已有编译类只读解析最终MEK/WAZ/GRP/DAT，没有重新构建、运行默认Maven测试或修改产物。

| 角色 | 最终MEK武装数 | 对应WAZ技能数 |
| --- | ---: | ---: |
| Tsukuyomi | 20 | 33 |
| Yuri | 15 | 27 |
| Freja | 17 | 32 |
| Nagi | 19 | 30 |
| Misaki | 22 | 34 |
| Naoto | 21 | 32 |
| Katou | 21 | 32 |
| Wilhelm | 18 | 32 |
| Motoki | 18 | 32 |

- WAZ组111至119与WazaGroup技能数一致。所有新增玩家武装ID不超过21，未通过放宽153容量限制绕过校验。
- 最终19个WAZ中的141处技能引用无越界；七个追加变体的分支与正常出口静态有效。
- 14槽装备的普通武装、FC和initializer分类及目标身份有效；九角色AI与从BHE重新读取并执行当前转换后的载荷一致。
- Material三个外层容量208/74/43覆盖最终GRP的151/74/40，多余部分为空，没有发现容量不足。
- Freja默认FC槽实际指向id12的「武幻剛乱舞」。Charge、计数与Change的23个事件、16个槽组与原版经当前转换后的结果一致。
- 已核对collection、嵌套事件及变体Material复制隔离。中断恢复、真实伤害与状态变化仍需要游戏验证。

## 构建身份

主代理重新计算本轮文件SHA256，与latest receipt一致：

- EXE：`09ff572b7d42c2aedbe092e5ad40583377d1cd258d65065bfc7b77d92df31f68`。
- PAC：`79be51b21e5beb082a0d9a059b2f2cce4546ba18833c29142dcd341ca7adf2a6`。
- `runtimeValidated=false`保持不变，没有将旧0022包的运行对照归到最新包。

## 原版与工具对照

- 原存档CP936标题在CP932环境下触发CRT显式终止的输入已定位，见vanilla-encoding-forensics.md。原78文件未覆盖；独立兼容seed不随公共mod包分享。
- vanilla-compat-02、vanilla-fresh-01均由原launcher通过STOP结束，failure为空，原Save完整清单不变。前者共享Drs的nvAppTimestamps发生变化，退出审计标记归因未知，不能宣称原Drs全部不变。
- 空存档原版的主循环约60fps；三个Present内有437次DrawPrimitiveUP，菜单绘制与Present HRESULT均为0。DirectInput三个采样周期返回0。它们只证明相应调用成功，不证明像素或玩家输入正常。
- vanilla-fresh-native-01使用空存档及正常全局Drs访问，Drs重定向次数0，保留原Save/源输入/仓库保护。移开贴图后捕获仍黑，尚无正常菜单或玩法效果证据。
- 已确认F3与本机Snipaste全局贴图键冲突；工具点击明确命中了Snipaste的Paster。按官方Shift+F3暂时隐藏全部贴图后，原生启动提示重新可见；没有删除图片或更改全局快捷键。此前F3未产游戏截图和双窗现象不能直接归因于游戏。
- F3、Alt+F3未产生可确认的游戏原生BMP。普通Down/Return的实际菜单响应仍待核实；未通过内存调用截图、注入游戏输入或导出显存像素伪造验收。
- 原版native对照随后正常STOP，原Save清单不变。最新014635包已在rework-latest-native-01真正启动，越过自动更新提示；80秒快照仍运行、guard0，但捕获仍黑。此有限启动记录没有升级为玩法通过。

运行工具默认私有Drs模式41项离线检查通过；加入显式native-driver-state对照后44项完整离线检查通过。独立审查指出原Save退出审计读取失败会跳过驱动审计，已分开处理并新增读取失败及实际存档差异两项回归，完整46项通过。测试不启动真实游戏，不能代替九角色的装备、预览、训练、命中/落空、热充能和形态中断验收。

诊断用Photoshop文档已保存到`target/bhe-playable-runtime/vanilla-compat-02/window-capture-comparison.psd`并关闭，没有写入默认clarias目录或打开用户已有PSD。

当前没有角色完成正式玩法验收。中文说明书仍为开发稿，最终发行和目标完成均以实机证据为门槛。

## DPI单变量对照

2026-10-27继续检查了DPI虚拟化这一具体假设。原EXE无RT_MANIFEST资源、DPI awareness API或相关声明。新的rework-dpi-01保持014635的EXE/PAC、同一兼容存档seed和native Drs模式，仅添加dpiAware=true的外置manifest；未增加activeCodePage、UAC、supportedOS或PerMonitorV2设置。manifest通过Windows SDK mt验证，源文件只在本地target中保留。

只读GetProcessDpiAwareness查询返回HRESULT=0、awareness=1，证明新进程已成为system-DPI-aware。捕获尺寸从1203×941变为536×426，但捕获仍黑，所以该声明没有解决现象，不加入发行包。窗口的Alt+Space系统菜单能够正常显示和响应，游戏内菜单输入仍未确认。源存档没有覆盖；前一最新包baseline已由原launcher正常STOP且原Save清单不变。

## 说明书操作依据

- 原版`game/bsdx/bin/Tutorial020：使用するボタン.bin`的stringTable[3..9]提供默认键位及可在コンフィグ改键的说明：方向键/数字小键盘移动，A=Z、B=X、C=C，D=Shift/V/数字5，Esc暂停；手柄BUTTON1至4与START。
- Tutorial100的stringTable[2..15]确认近距离、远距离、普通冲刺、搜索冲刺四类，并说明普通冲刺优先于近距离；暂不据教材文本顺序猜DAT行顺序。
- `5E13E0`将DAT普通列i转换为runtimeSlot=4*(i%3)+floor(i/3)，`5E03F0`执行反向；`61E310`把0x100/0x200/0x400分别送入按钮0/1/2。因此普通DAT列为3*state+button，而不是四个攻击按钮。
- `5EF960`扫描上述三键，A+B返回0，A未按而B+C返回1；`61BFF0`将其加12选择装备槽，并要求计量值至少10000。说明书据当前EXE分别写第一FC槽A+B、第二FC槽／本包initializer为B+C，没有将教程160的generic A+B说法照搬到两个独立槽。
- root复核`datBsdxJson/Button.dat.json`第151至154行第6列，四条均为带「します」的说明文字；其余列是坐标／数字，不能把省略「します」的短语冒称实际按钮图片文字。`7A5320`的case152进入state32，`4B8AB0`创建CEquipMenu，证明该说明项的装备入口。
- 选机配置覆写表行25至33（zero-based），不能未经实机确认称为屏幕第26至34格或某一页。完整菜单路径与预览触发仍需实机确认。

## 默认装备分组修正

进一步沿原生消费者确认了普通槽四种state的顺序：0近距離、1サーチダッシュ、2遠距離、3ダッシュ。`61FA70`创建的motion0/2/3/4分别对应Stand/Dash/SearchDash/BoostDash；`61E310`在D held且非BoostDash4时选择state1，有方向或移动状态时选择state3，否则由`61C3B0`的近距谓词选0或2。`5EFC90`确认inputBuffer+520是方向状态。`61BF20`在同一按钮内从当前state往3继续检查可用武装，不跨按钮，也不跨入FC槽。

因此DAT列0..2是近A/B/C，3..5搜索冲刺，6..8远距，9..11普通冲刺。旧默认预设把九角色的多数射击放在9..11，而6..8放了战术／斩击／形态退出；已对九角色只交换这两组三槽，保留0..5、FC、initializer、最终身份取号及153限制。没有改变MEK/WAZ或重排武装编号。

新增九个角色独立回归，使用真实MEK及整体武装号+40的夹具，按literal日文名称验证远距／普通冲刺输出；另更新Tsukuyomi完整14槽期望。主代理实际执行RED：14tests/10failures/0errors/0skips；修复后GREEN14/14。随后16类完整定向回归共113项，失败／错误／跳过均0。日志为target/bhe-audit-20261026/loadout-mode-red-20261027.log、loadout-mode-green-20261027.log及nine-character-modes-green-20261027.log，Surefire XML保存Unicode原文；CLI日志显示的乱码不作为代码损坏证据。

说明书已补原版默认按键、两个FC组合键、训练菜单说明文字、四类分组与空槽回退，以及改造武装和形态进入／退出的默认位置。它仍是开发稿，未把这些静态入口写成已经通过实机操作。

新构建`run_20261027_035924_938`已成功，EXE SHA256仍为`09ff572b7d42c2aedbe092e5ad40583377d1cd258d65065bfc7b77d92df31f68`，PAC为`a525ad5a1d285123845932dd3d2f2cbb14c9e6a8311d90b9f4ffe16ccc615e24`。独立审查实际最终WeaponEquip.dat的九角色行104至112，全部符合上述分组；0..5、12、13的ID与名称不变。九角色MEK/WAZ与旧014635构建逐字节相同，数量、Material、AI、WazaGroup及19个WAZ的141处技能引用均通过检查。

主代理检查并重新运行了只读审查脚本`target/bhe-audit-20261026/loadout-final-20261027/verify-final-loadouts.ps1`，fresh报告`final-loadout-audit.json`为passed=true/errors=[]，并额外断言九份MEK/WAZ未变。该脚本只读取产物并写本次target报告。新包仍未获得实机验收，runtimeValidated=false；当前活动窗口属于使用旧包的DPI对照，不能充当新包证据。

## 当前完成度与阻塞审计

2026-10-27第三个连续目标回合重新核实：launcher53154仍存活，PID506552的映像和创建时间匹配rework-dpi-01；捕获仍黑，未获得菜单／玩法变化。当前926个跟踪Java/FXML均严格UTF-8可读、无U+FFFD；113项定向XML报告仍为0失败／错误／跳过，原Save完整清单再次比对不变。

新增的Windows屏幕键盘试验没有补足输入证据：拖住Down后游戏active变0，不能排除失焦干扰；OSK Shift显示蓝色锁定后重新激活游戏，0.5秒内游戏推进30帧且active=1，但左右Shift的raw/edge/hold和D动作位均0。OSK锁定只是UI状态，不能据此推断实体键盘也不响应。临时Shift已解除、OSK已正常关闭，没有修改系统输入设置。

| 用户要求 | 当前证据与结论 |
| --- | --- |
| 排除Sou重做、保留现有实现，进入游戏不闪退／崩溃 | 此历史检查点035924错误跳过整个Sou阶段；已在下述入口修复中纠正。九角色完整操作仍未完成。 |
| 必须有实际效果 | 行为资源、113项回归、最终装备及引用验证通过；没有九角色实际命中／落空／状态效果证据，未完成。 |
| 中文说明书 | 开发稿已有安装／恢复、键位、FC、武装入口、特色和哈希；准确菜单路径、预览和实机矩阵仍缺，未完成最终验收。 |
| 修复早期代码乱码 | 确证乱码已按上下文恢复，既有WIP保全；当前926文件UTF-8／U+FFFD复核通过，不把CLI显示问题误修进源码。 |
| 逻辑可改，架构保留；develop中文提交，不push | 现有JINKI V2→Tsukuyomi→Followup保留；当前分支develop，本目标提交均中文，没有新codex分支或push。 |
| 删除谨慎 | 本目标无跟踪文件删除，原Save未变，原有六项功能WIP仍53行，旧case和资源保留。 |

前一目标回合有装备修复／构建进展；本回合进行了存活、原Save、捕获及OSK输入复核。同一个阻塞已连续三个目标回合存在：缺少可核实的实际显示和有效游戏输入。所有独立源代码／产物任务已收束，现有证据不足以继续九角色实机验收或宣布交付成功。需要用户确认当前隔离窗口肉眼是否也黑、实体方向键／回车是否响应，或提供显示和输入可验证的运行环境。目标应为blocked，不能标complete；原始九角色目标和验收门槛保持。

## 后续纠正：地狱入口修复与新现场

上述为用户补充反馈前的历史阻塞记录。用户已确认实际窗口可见，故障是进入地狱即退出；035924现场在455BEE确认保存选择113越过0..112运行表。恢复原有Sou阶段并转换直接地图SPM后，094119完整包生成、最终资源验证及PAC解包字节比对成功，机体容量114。

root已在新case rework-hell-sou-01中实际从Start进入地狱设置菜单、打开选机、选月詠并进入装备界面。455BE5断点打印EDI113、运行表114并正常继续。窗口捕获在本轮可见；不能继续把旧的等待肉眼反馈状态视为当前阻塞。月詠新增变体在列表中、日文说明可读，但预览机体暂未显示；加载与动作效果继续取证。

Sou专属目录与目标初始基线相同；已有Sou阶段会延续共享正确性修复及全目录Material padding，不等于回退九角色武装。最新hash及调用栈见hell-entry-crash-20261027.md。其余九角色玩法验收未完成，runtimeValidated=false。
