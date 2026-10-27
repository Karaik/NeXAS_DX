# BHE九角色改造暂停检查点

2026-10-27 10:20 JST，用户明确要求暂时停止并固定成果。目标状态已设为`paused`，不是完成；所有子代理已中断，不再推进排查、测试或玩法验收。恢复须等用户重新开启目标。

## 已固定成果

- develop已有`a8e229133`：恢复旧容量注释4处方向符号，无行为修改。
- develop已有`6f827a43f`：CMekaLoader两组内联数组、尾字段及对象分配同步扩容，保留V2基线及原CRT；37个新增完整指令位点与既有扫描上限闭合。原生指令／哨兵／别名5项、实际Apply幂等与拒绝3项、CRT保留1项通过。此补丁尚未重新生成实机包，不能以094119现场冒称通过。
- 本次冻结另保存：九角色入口恢复原有Sou阶段（不重做Sou）；直接地图SPM通过既有BHE→BSDX转换器生成DX字节，转换后再去重和预检；独立materialize遇同批异内容冲突时停止全部提交。普通SPM缺图仍仅审计，没有占位图或CRT早退。
- 中文说明书已补角色网格位置、已操作的菜单路径、原有Sou范围及当前验证边界；不是最终发行验收稿。

## 最后成功的实际构建

记录：`target/bhe-playable-builds/run_20261027_094119_577/build.json`，latest指针仍指向此轮。

- assetRoot：`sou/sou_assets_20261027_094301_570`。
- EXE：`sou/BaldrSky_20261027_094324_577.exe`，SHA256=`578789308f7ca7cd7a91c5d7a71519ac5c70f3496d199a0d4a9e496f263e6bdd`。
- PAC：`sou/Update3.pac`，SHA256=`e004bcb4dfed7989824292049fbddc32dc8b52e80846d06decacac5b084299d5`。
- 九重做角色＋保留Sou，机体容量114，初始化494304字节；最终资源重解析、封包和PAC解包字节比对成功。该包早于原生loader布局补丁及后补的独立冲突gate。
- `runtimeValidated=false`。地图转换RED1项预期失败（5561对5537字节），随后5类20项GREEN，无跳过。独立同批冲突回归另实际RED1项→GREEN1项；最后与上述原生9项合计10项全过。

## 已确认的运行结果

隔离case：`target/bhe-playable-runtime/rework-hell-sou-01`，使用保全的114行兼容seed，原Save不改。

root实际完成Start→地狱设置菜单→机体选择→月詠（第3行第6列）→装备→点击「袈裟・一閃」。入口不再闪退；455BE5断点打印保存选择113、运行表114并正常继续，未命中CRT。

装备列表及新日文说明显示正常，预览热条会变化，但玩家与敌人不可见。用户已明确回答肉眼也看不到，故这是实际游戏显示故障，不能继续归为黑色捕获限制，也不能把进程存活算技能通过。

只读取证确认玩家CUnit机体ID104、drawEnable=1、SPM非空、page有效、scale=256/256、alpha=256；进入60EA60。SPM图像21为`tsukuyomi_000_0001.png`，主Surface非空；成品PNG对应裁块有非透明像素。源／目标均为CD3DSurface，clip与texture指针存在；一次实际源Surface调用的dst矩形为(-48,181)-(17,248)，需继续排查位置／裁剪及最终提交，不能从对象非空推断texture像素正确。各快照来自不同动作帧，旧指针不能在恢复后的新进程复用。

这次预览中的loader引用／状态数组和尾字段全0，费用vector长度103；没有证据把预览不可见直接归因于loader数组覆盖。原版机体同界面对照尚未进行。最后尝试恢复最小化窗口时，computer-use报告用户输入冲突／窗口仍最小化，未再执行选机操作。

## 恢复后首先处理的未完成项

1. 保留上述可见性故障及调用栈依据，先用原版机体／基线包对照，检查最终位置、裁剪与D3D提交，禁止凭猜测改渲染或吞异常。最小原生入口和字段在后文及CDB日志。
2. `BQMekaMemory.dat`仍只有103行，新增ID的异步加载存在独立长度检查风险。固定对象布局修复并不解决此表。费用语义／小样本校准报告位于`target/bhe-audit-20261026/cmeka-loader-layout-20261027/`；没有可信成本公式，不填0、不借任意donor数值、不关闭检查。Material.spriteGroups payload已由5F0C40→766CB0证明是imageData索引，不是动画号。
3. 独立materialize的同批异内容冲突gate已补并单测通过，但`BheMapImportPipeline`调用prepare后仍没有检查resourceResult原有冲突，可能提交首份pending及地图。有限Pipeline事务回归仅完成设计，尚未写入文件／执行RED，生产Pipeline未修改。现有manifest会把output与resource冲突相加，不要重复复制同一冲突造成双计数。
4. 重新构建包含loader布局补丁的完整包，绑定新EXE/PAC身份后再实测。094119不含该补丁。九角色全量武装预览、训练命中／落空、形态／热量／中断恢复均未完成，正式玩法通过数仍为0。

## 原生诊断入口

下列地址与字段偏移均为十六进制，`poi`表示读取指针。

- 预览重建：5FFB40；玩家`poi(86CF88)`，敌人`poi(poi(86CE14)+144)`，actor+158为CUnit，CUnit+2C为机体ID。
- CMeka绘制60EA60；actor+15B4显示开关，+44 SPM，+1638 page，+A0/A4/A8位置，+16A0/16A4缩放，+CC/D0 flags/alpha。60EADC刚完成alpha合成，ESI=actor、EBP/EDI=屏幕X/Y、[esp+10]/[esp+30]=最终flags/alpha。
- SPM页vector+18/+1C，stride58；page+10为chipBegin，chip stride38；image vector+48，image stride28，+1C为主Surface。图名SSO容量在+18。
- Surface+20..2C为clip；+34/+38像素尺寸，+3C flags，D3D+60 texture、+64 render surface。6E7590为源Surface向目标的矩形绘制，栈含target/dstRECT/srcRECT/flags/alpha。
- 6E41D7是普通DrawPrimitiveUP返回点，EAX为完整HRESULT；其他精确返回点6E3404 SetTexture、6E3C85 SetRenderTarget、6E325A SetViewport。不可把成功HRESULT单独当实际效果验收。

## 停机与保全

CDB39273已明确`.detach`后退出；launcher37914经本case的STOP文件正常停止。state=`stopped`、failure=null、cleanup_pending=false、guard_violations=[]，原Save清单unchanged=true；旧PID579848已不存在。没有存活的本任务游戏或CDB，不重用case锁。

原有六个Clarias相关dirty文件仍是53行新增，不纳入本目标提交；其他既有未跟踪文件、所有旧包、日志、隔离存档及校准报告保留，没有删除。只在develop中文提交，不新建分支／工作树，不push、不改PR。恢复时先读本检查点、progress及Git状态，不启动默认Maven资源套件。
