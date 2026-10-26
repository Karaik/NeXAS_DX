# 第一批可装备变体：复制工具与三个角色

实施日期：2026-10-26；报告收尾：2026-10-27（Asia/Tokyo）。依据 `variants-implementation-brief.md`，保留现有pipeline/DTO/Customizer架构。本批生产实现已冻结，主代理正在进行合跑和集成；不将本批初值视作九角色重做或游戏验收完成。

## 文件与集成接口

- 新增 `followup/customize/CharacterWeaponVariants.java`，只复制本角色已重绑主WAZ中的技能、MEK普通武装及对应Material。
- 新增 `tsukuyomi/TsukuyomiCustomizer.java`：`TsukuyomiCustomizer.INSTANCE.customizeBeforeWrite(Mek reboundMek, Waz reboundWaz)`。
- `WilhelmCustomizer`、`MotokiCustomizer`继续使用现有 `customizeBeforeWrite(FollowupMekaContext)`。
- 新增 `CharacterWeaponVariantsTest`，覆盖复制工具及这三个变体。pipeline、Importer、Runner和最终数量同步由主代理集成，本批没有编辑这些文件，也没有修改Sou目录。

复制接口固定为：

```java
Mek.MekWeaponInfo CharacterWeaponVariants.append(
    Mek mek, Waz waz, Mek.MekWeaponInfo sourceWeapon, Waz.Skill sourceSkill,
    String newWeaponName, String newSkillEnglish, int heatCost,
    String description, Consumer<Waz.Skill> editCopy,
    Mek.MekWeaponInfo... materialDonors)
```

返回新追加的武装，或已通过完整一致性校验的既有变体。新Skill日文名使用新武装名，英文名使用明确的`DX_`前缀；武装保留源`weaponSequence`、category和标志字段，不发明新的武装属性。

## 原始资源签名与实际改动

测试均重新读取 `game/bhe/mek/<角色>/<角色>.mek` 与 `game/bhe/waz/<角色>/<角色>.waz` 二进制，再经过当前格式转换及Term重编译。实现前另从原WAZ字节按已解析事件offset核对速度六整数与Effect头部，均与本轮选定签名一致。下表phase从1计，旧索引仅用于取证，生产定位使用MEK名称/第二标识、WAZ日英双名及其真实引用。

| 变体 | 源身份与已核签名 | 实现 |
| --- | --- | --- |
| Tsukuyomi：袈裟・一閃 / `DX_KESA_ISSEN` | MEK `袈裟/PUNCH` → WAZ `袈裟/PUNCH`，旧14、1phase；8..11帧速度`(0,24,0,0,0,0)`；10..11帧Hit直接值30；8..11帧Effect保留。38帧原跳本角色`抜刀中/0009`，旧12。 | 独立复制为普通武装，热耗12/12、exp0。速度改`(0,32,0,0,0,0)`；38帧改跳按最终身份定位的`納刀/0008`，保留其30帧退出。纳刀MEK第二标识是`0003`，与WAZ英文名`0008`分开定位；合并其Material，不承诺获得直接装备纳刀时的负热耗。 |
| Wilhelm：交互砲撃・離脱 / `DX_ALTERNATE_CANNON_EXIT` | MEK `主砲斉射/0007` → WAZ `キャタピラ：主砲斉射/0017`，旧25、2phase。P1准备43帧；P2有10条0帧Effect，右侧为#5–8，Unit7源POS/OBJECT的aux3为`[0,2]`，左侧4条`[0,1]`，中心2条`[0,0]`。 | 热耗40/40、exp0。依据最终位置表达式识别4条右侧，只将其外层0..0改8..8，内部相对帧、目标、位置、数量、伤害和弹道保持；50帧原回`キャタピラ：持続/TACKLE07`，改跳`キャタピラ：終了/TACKLE05`，保留该退出技能实际25帧结束。合并退出武装Material。 |
| Motoki：集中爆撃・帰投 / `DX_FOCUSED_BOMB_RETURN` | MEK `飛行形態：集中爆撃/0020` → WAZ `飛行形態：集中爆撃/0023`，旧27、1phase。10帧唯一Effect，Unit4六枚发射载荷`(0,1,0,0,6,0)`；速度0..11为`(1,0,0,0,12,0)`，12..59为`(1,0,0,0,8,0)`。89帧原回`飛行形態：飛行中/BOOST_DASH5`，旧23。 | 热耗40/40、exp0。保留整个Effect及两段速度，只将89帧出口改为本角色`飛行形態：終了/BOOST_DASH6`，保留其30帧正常退出；合并退出武装Material。 |

Wilhelm位置表达式没有照搬源字典：分别核对当前两份Term.grp后，最终POS/OBJECT仍为root20/type`[1]`、右侧aux3`[0,2]`。真实转换测试也核对这一签名。所有出口group取最终MEK的`wazFileSequence`，目标skill序号在当前WAZ中按身份解析，不写旧11/12/13/17/21/22/18/23/24等裸编号。

本轮原WAZ SHA256：

- Tsukuyomi：`8b9c93cfe09d816148bfd7852e1b6a9b268a7fcbe87e14d91a6472cab99b31a1`。
- Wilhelm：`0cdd2e7af7ff7cc9868ff6a869ddc1b9cb9bcfcd40023d89c412ef57c84e912f`。
- Motoki：`61a30a8c02fee7c009c1d9d2e55ded6afdc80ad13b2ff6778eb6263cb3cee402`。

## 复制和失败边界

技能使用现有Jackson多态DTO序列化/反序列化深复制，不用Bean浅复制技能；武装独立复制，Material三个group列表与每个int[]独立。供体必须属于当前同一MEK，按其武装行取得Material；合并保留源数组顺序，再追加供体中尚未出现的资源号。

工具要求非KOU、连续0起MEK map、单武装单Material尾表及regular/trailing/entries三视图一致；新普通武装仅尾追加，实际新编号最多152，不删除/重排旧武装或AI引用。新技能、武装、Material全部在验证与独立副本编辑成功后才一起追加；回调晚期失败不会留下半个武装。名称部分碰撞、Material不匹配、容量不足、源引用不一致均拒绝。

重复执行从原武装/原技能重新构造预期副本，并核对既有武装、技能事件载荷和Material；仅排除offset、length等非二进制记录信息，不以发现同名代替一致性验证。三份日文说明采用brief文字，写入实际新武装说明。

## RED、当前验证状态与限制

主代理串行执行，实施代理未运行Maven：

```powershell
mvn -q "-Dtest=CharacterWeaponVariantsTest" test
```

RED已由主代理确认：8项、8失败、0错误，日志 `target/bhe-audit-20261026/variants-red.log`。新复制工具和Tsukuyomi入口缺失时测试显式断言失败；Wilhelm/Motoki旧no-op未增加武装、仍为16而期望17，符合预期。没有把缺类编译失败或fixture错误当作RED。

报告编写时直接读取的最新 `CharacterWeaponVariantsTest` Surefire文本为8项、0失败、0错误、0跳过；最终合跑总量和集成结论仍待主代理回传，不据这一份报告宣称整轮通过。生产代码此时停止编辑。

八项覆盖：collection四列表和源/复制技能隔离、Material三类数组合并/隔离、幂等及既有载荷漂移、重名/尾表不匹配、KOU/稀疏map/153容量拒绝、回调晚期失败不半写，以及三个真实角色变体。变体测试在转换后为WAZ添加空前缀，并将自身group改为210/211/212，检验身份定位而非源裸索引；这是测试中的最终引用视图，不能当作本轮完整graft产物。

MEK/WAZ只生成到JUnit临时目录；重解析检查完整新技能事件与Material、武装入口/热耗及日文windows-31j无损编码，并比对源文件hash。原角色主技能、武装、原Material前缀与AI保持的断言也属于这些测试。

尚未在游戏中实际装备、预览、命中/落空、退出或重载这三个变体，没有实机平衡、效果或不崩溃证明；pipeline最终数量、Material padding、交付PAC及实机验收由主代理继续处理。本批未提交、未推送、未开子代理。

## 退出donor签名补强（2026-10-27）

独立复核发现，三角色此前只验证退出donor的Change事件类型和帧，未验证其实际结束表达式。把该donor改成跳回拔刀、履带持続或飞行姿态时，变体仍可能建立，不能据目标技能名称认定它会正常退出。

本轮重新直接读取原WAZ二进制，不依赖缓存JSON。Tsukuyomi `納刀/0008` 的Change在offset22728、30..30帧；Wilhelm `キャタピラ：終了/TACKLE05` 在offset58643、25..25帧；Motoki `飛行形態：終了/BOOST_DASH6` 在offset114101、30..30帧。三条均长76字节，签名完全一致：Change.flag=1、Change.int1=0；唯一condition为root0/type`[0]`，唯一destination为root16/type`[0,0]`；两条collection的param、aux3、aux4全空，int2=0。

新增三个真实资源回归用例，各先逐字段核对上述正常签名，再仅将donor的destination改成本角色最终group、按身份定位的持続/飛行skill跳转。预期拒绝并保证整个MEK/WAZ JSON不变；此前8项保留，总计11项。主代理已确认本轮RED：只跑这三个新方法，3项、3失败、0错误，均为预期的“应抛IllegalStateException但没有抛出”，日志 `target/bhe-audit-20261026/normal-end-red.log`，不是fixture或编译失败。

取得RED后，在 `ProtectedCharacterEdits` 增加共享 `requireNormalEnd`，逐字段要求上述完整Change及两条collection签名；null或结构不完整同样抛IllegalStateException。三个Customizer在复制追加前调用它，保留原帧校验，不触及pipeline、Importer或其他角色。

本轮直接读取的最新Surefire报告（`CharacterWeaponVariantsTest.txt`，2026-10-27 00:19:48）为11项、0失败、0错误、0跳过，覆盖原8项以及新增3个donor拒绝用例。修改文件已通过 `git diff --check` 并保持UTF-8无BOM、CRLF；实施代理没有运行Maven。该结果限于本测试类，主代理的整轮合跑及实机验收结论另行记录，生产继续冻结。
