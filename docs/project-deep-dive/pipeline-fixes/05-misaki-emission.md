# 05 Misaki 公共弹幕发射数按身份及追加计划定位

完成日期：2026-10-08；本项对应五项中的第5个独立提交。

## 根因与边界

旧修复器猜测最终 `Tama05[488/491]`，仅当追加基址为152时对应源336/339。公共资源接入会按实际宿主技能数计算基址，纯BSDX/fallback或不同继承成果不能使用固定目标编号。Followup的selectedPackage只含私有WAZ，公共Tama05属于rawSourceBundle.commonProjectileWazByFileName和preparedBaseline。

本项只修复床召還与あたためる的两个已核异常节点，不引入第6～10项演出、运动、Hit位域或公共稀疏追加方案。

## 实现

- 以源双名称`◆みさき：床召還 / 0304`、`◆みさき：あたためる / 0297`唯一确定公共源技能；源编号由实际列表取得，不固定336/339。
- 原始公共BHE DTO提供异常结构及phase/Effect序号：Unit4已启用，局部帧0..0，六整数为`0,1,0,0,999,0`。优先使用它保留只读原值，避免common converted与baseline共享节点后失去幂等定位依据。
- common converted用于交叉核对身份与源索引；当前raw及converted都缺失时只读查看请求提供的previousCharacterResult公共来源，不查私有selected map，也不从target随机同名猜来源。
- 通过commonProjectileAppendPlan映射源公共group到host、base、count及target group，再计算`base + 实际源skill index`。只核该目标索引的双名称与对应节点签名，原版前缀同名不构成歧义。
- 两个目标全部验证后统一将999改为1；已为1保持不变。common-only共享视图若没有原999，则只在源/目标全部启用Unit4都是1、结构及其他载荷完全一致时明确不再改值；不从多个正常1节点猜原异常位置。源正常而目标仍999、其他未知值或结构不一致均明确失败。
- 真实Misaki请求、已计划的公共段缺源/计划/目标、身份或结构不一致均抛出诊断。完全未准备对应公共源及追加段的非Misaki可选上下文记录skip note。

## 回归证据

先执行`mvn -q "-Dtest=MisakiCustomizerTest" test`：9项全部按预期失败，无测试错误。失败包括动态目标保留999、缺失计划/歧义不诊断以及真实原始资源输出仍含999。

实现后执行`mvn -q "-Dtest=MisakiCustomizerTest,SouCustomizerTest" test`。覆盖两个base（152、300）、不同host文件名、旧编号诱饵、双名称源索引移动、唯一性、目标身份/原值诊断、原始/转换共享别名与重复执行、previous来源和可选skip边界。真实`game/bhe/waz/tama05.waz`经生产转换后，修复目标由BSDX generator写入JUnit临时目录，并由生产parser重新读取，两个技能的Unit4分别为3个/4个节点且值全部为1。

独立复核补充：真实converted-only共享源中床召還Unit4为`[999,1,1]`、あたためる为`[1,1,999,1]`。初次实现修复后再用普通1签名定位会出现多命中。新增真实共享重复调用与“源正常/目标仍异常不得猜修”两项测试，16项测试中2项按预期失败；改为全正常结构核验后，MisakiCustomizerTest 16项全部通过，Failures/Errors/Skipped均为0。此前与SouCustomizerTest的联合14+10项也通过。真实输出仅写JUnit临时目录；未启动游戏，未运行full graft，未修改原始资源或既有生成目录。
