# 装备预览摄像头异常的EXE基线对照

## 已确认结果

九角色构建入口此前错误地把`AkaoGraftRequest`默认的`ida-reverse/BaldrSky.exe`换成`bsdx_game/BaldrSky.exe`。后者带宽屏改动，在未加载任何移植PAC时，也会让原版甲机体及敌人消失在装备预览视野中；参考EXE在同一存档与同一资源目录下正常显示双方及タックル命中动作。

| 项目 | 参考EXE | 旧构建误用EXE |
| --- | --- | --- |
| 路径 | src/main/resources/ida-reverse/BaldrSky.exe | src/main/resources/bsdx_game/BaldrSky.exe |
| SHA256 | 713cf605360a55a863f87bfbdea5453aaeeb2a33983ad4a005c9297e3a86e757 | 2f8cfc695ffc30df2f02deb8d4902a01abad6e71475e609d1cda703c991c3492 |
| 大小 | 4,793,856 | 4,793,856 |
| 对照case | resume-reference-preview-02 | resume-wide-preview-01 |
| 原版甲 idle | 可见 | 不可见 |
| 原版タックル预览 | 双方、动作与命中可见 | 只有地图、说明、HP／热条 |

两次均为vanilla模式，无Update3–7，基础游戏资源相同、native驱动状态模式相同、诊断seed相同。root通过computer-use实际进入Start→地狱→装备→タックル，观察上述差异。两case均已STOP，failure=null；原存档没有变化。

## EXE差异依据

全文件仅1612字节不同：PE头2字节、.text347字节、末尾无名section1263字节，.rdata/.data/.rsrc相同。主要已核的改动为：

- 45D087／45D0C3的装备镜头补偿宽度800→1920，45D105高度600→1080；45D009／45D041仍减400／300。
- 4464F0／4465B0地图边界钳制由中心400／300、范围分子102400／76800改为960／540、245760／138240。
- 两组窗口尺寸表的各档被统一改为1920×1080。
- 地图tile绘制入口45F320改跳CC1000，新增代码把84×63网格改为154×93，并改写滚动拼接；无名section增加执行属性。

预览actor初始化5FFB40、MEK加载659270、actor绘制60EA60、Surface绘制6E7590在两份EXE中没有字节差异。本次对照定位到继承的EXE改动，不需要猜测修改静态图像或角色动作来修镜头。没有对旧宽屏补丁继续打修补跳转。

## 诊断存档边界

FreshConfig会锁回地狱模式，完整114行原配置的选择113又不适用于纯原版103槽。因此仅为这组对照创建独立`resume-vanilla-seed-selected0-20261027/Save`，将保全兼容seed的Config主块0x422C由113改为0，其余解压字节、前置压缩块和77文件完全一致；源seed与真实原Save哈希不变。新Config SHA256为`56c168a501da1e4c028810a9f2c1d767f937a4da97aeb7682dade5ef3eedc379`，审计在该target根下。

此seed保留114行统计，纯原版会拒绝部分统计并重写私有副本；它只用于同输入渲染对照，不是最终BHE兼容seed。BHE验证继续使用原先保全的114行seed，不改用户选择。

## 构建修正

九角色入口已恢复默认参考EXE，receipt增加`sourceExe`与`sourceExeSha256`。155314完整构建成功，包含既有Sou、原生loader114项布局和地图冲突事务gate；最终资源重解析、封包与PAC解包比对成功。

- buildRoot：`target/bhe-playable-builds/run_20261027_155314_304`。
- EXE SHA256：`1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`。
- PAC SHA256：`e004bcb4dfed7989824292049fbddc32dc8b52e80846d06decacac5b084299d5`，与094119逐字节相同，说明此次相机修复不依赖静态资源变化。
- 原生机体容量114，loader分配1052／state起点588，CRT入口8B FF，装备补偿宽度恢复800。

九角色在修正基线后的实际效果仍要在新case逐项验证，不能将原版对照直接写成九角色已通过。

## 修正基线后的BHE复测

`run_20261027_170134_486`在正确参考EXE上完整重建，EXE仍为`1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`；补齐114行预加载表后的PAC为`a8869c81ef05949e9233d7c2349aedb77c9f1875d45c12c06b6b643da337930a`。原103行费用表前缀逐字节保留，最终封包及解包比对通过。

新隔离case`reference-preload-01`已实际进入地狱、选择月詠并打开装备预览。双方机体与镜头正常；CDB软件断点记录到武装19「袈裟・一閃」的REQ、实际START、敌人45伤害（HP6000→5955）与原生NATIVE_READY，多轮重复一致。该证据只覆盖这项预览，不能替代全部角色、训练及加载调度验收。

此前`resume-bhe-reference-01`在安装新增七hook观察器后立即发生访问异常，没有取得有效回合记录；现场保留在该case。此异常原因尚未确认，不计入角色技能失败。新增观察器已停用，本轮沿用原隔离工具并仅以CDB软件断点记录游戏事件，未再注入第二观察器。
