# 九角色产物复审与实机边界（2026-10-27）

本记录对应代码`c27bc14fb..89b592a90`及构建`run_20261027_014635_068`，排除Sou。静态复审没有发现Important级可行动问题；这不等于游戏验收通过。

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
