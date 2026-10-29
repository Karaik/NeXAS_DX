# Motoki自主起飞投弹 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox syntax for tracking.

**Goal:** 让现有Motoki新武装17从地面可靠起飞、投弹并接地，保留原飞行招式。

**Architecture:** 仅在MotokiCustomizer旁新增角色局部构造器，复用已重绑动作、现有单phase变体接口和原生四phase执行图。旧武装及公共弹体不改，现有输出器继续维护最终技能数和Material容量。2026-10-29修订依据为同名SDD ledger的I1及重力残留裁决。

**Tech Stack:** Java、JUnit5、现有BHE/BSDX DTO与二进制服务、原生WAZ Change/ValRandom。

**Spec:** `docs/project-deep-dive/bhe-playable-rework/motoki-flight-design-20261028.md`。

## Global Constraints

- 所有修改直接在develop，不新建分支或工作树；中文提交，不push。
- UTF-8无BOM、CRLF，编辑使用apply_patch；保留原六个Clarias WIP合计53行。
- 不删除源、旧产物或证据，不修改EXE、Sou或公共炸弹；普通缺失SPM图片仍非致命。
- 主代理独占串行Maven、整链集成、UI、CDB与提交；子代理不运行Maven、UI/CDB/Frida，不提交，不派生子代理。
- 新17仍为同名普通武装、热耗40，武装号不超过152；恰一个新武装与两个新WAZ技能，旧9/13及原始技能字节不变。
- 不改CharacterWeaponVariants通用API／单phase不变量。新图必须按Spec高度门后投弹，接地后正常结束；超时仅取消，不计主流程通过。

---

### Task 1: 构造并验证角色私有飞行执行链

**Files:**
- Create: `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/motoki/MotokiFlightBombVariant.java`
- Modify: `src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/motoki/MotokiCustomizer.java`
- Create: `src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/motoki/MotokiFlightBombVariantTest.java`
- Modify only Motoki assertions: `src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/followup/customize/CharacterWeaponVariantsTest.java`

**Interfaces:**
- Consumes: `Mek`与`Waz`均已重绑；旧22/27/24按双名和原有frame/载荷签名定位，group从MEK读取。
- Produces: `MotokiFlightBombVariant.append(Mek mek, Waz waz)`返回新／既有`Mek.MekWeaponInfo`；由现有`MotokiCustomizer.customizeBeforeWrite`调用。
- Tests只调用现有Customizer，RED阶段不得引用尚不存在的新类，失败必须是行为断言而非编译错误。

- [ ] **Step 1: 写真实失败回归，先不改生产。**

从只读`game/bhe/mek/motoki/motoki.mek`和`game/bhe/waz/motoki/motoki.waz`读取，复用当前测试的转换、私有跳转重绑模式，将group移到212验证动态索引。调用当前MotokiCustomizer后要求：

```text
weaponsAfter = weaponsBefore + 1
skillsAfter = skillsBefore + 2
newWeapon.name = 集中爆撃・帰投
newWeapon.category = 0
newWeapon.heatMin = newWeapon.heatMax = 40
router.phaseCount = 1; router仅以0帧JUMP/WAZA指向新增私有执行技能
execution.nameEnglish = DX_FOCUSED_BOMB_EXECUTION
execution.phaseCount = 4
execution.suffix = [(0,0),(1,0),(2,0),(3,0)]
eachBlock.slot27/28.type0 = (0,100,0,100)
```

按Spec逐项检查P0高度门和240取消、P1准备末端10帧再复核／不足回起飞、P2零帧六枚和79转场、P3基准重力50及LANDING／360取消。生成临时MEK/WAZ后重读再检查；保持源文件／对象、原技能／AI、旧9/13与公共Bomb不变。按新Material并集检查三供体。

局部运动回归必须读取产物中的速度、重力、门和跳转，而非调用构造器算期望；TURN/INPUT先解析suffix入口再读int1实际block，不能直接param→phase，越界或无可执行入口拒绝。type0 modifier按5E6BA0端点和5E68F0窗口线性求值，恒100为`(0,100,0,100)`，必须检查每block首／中／末帧并拒绝未实现的相对模式；不能只取int4。独立使用原生Vz积分／中心高度公式，覆盖正常地面能投弹、空中不重复爬升、准备期间一次／重复目标升高、过高目标取消且不发弹、真实接地后结束及离开时重力50。排队转场不是一次玩家物理积分，但目标可先更新。明确这不是完整引擎模拟。另验证幂等、坏供体／任意未知投弹载荷／漂移／半成品零提交、CP932可编码日文说明。

- [ ] **Step 2: 主代理串行跑RED。**

Run: `mvn -q "-DargLine=-Xmx6g" "-Dtest=MotokiFlightBombVariantTest" test`。

Expected: 原始RED已确认；fix round1针对当前三phase图缺准备末端复核、释放切片及重力50，应发生行为断言失败。fixture或编译错误不能计RED。既有initialZ200、目标在首次准入后升高80的有效RED保留。

- [ ] **Step 3: RED确认后实现。**

新类深复制并编辑Spec四阶段，再用CharacterWeaponVariants生成单phase路由和三行Material并集；最后追加私有执行技能，所有guard和可失败编辑先于目标对象提交。

私有执行技能的suffix不能沿原27单入口，必须独立生成四条`(blockIndex,0)`；四block的modifier100均使用type0绝对端点tuple`(0,100,0,100)`。这两个字段必须在二进制回环后验证，旧NATIVE_READY不证明投弹执行过。

```text
P0: speed=0; vz=(0,8,0,0,8,0); gravity=0; jump/fall modifier=100
    clock0 && HEIGHT2>=200 -> P1
    clock18..239 && HEIGHT2>=200 -> P1
    clock240 -> P3
P1: 原27的0..9片段; vz=0; gravity=0; modifier=100; 不发弹
    clock10 && HEIGHT2>=200 -> P2
    clock10 && otherwise -> P0
P2: 原27的10..89片段，窗口减10、跨界start裁0; vz=0; gravity=0; modifier=100
    clock0发原内层载荷6枚; clock79 -> P3
P3: speed=0; clock0清vz一次; gravity=50; modifier=100
    clock30..359 && LANDING -> END
    clock360 -> END(cancel)
```

移除新P0的原热量／Change无限loop；新P3不保留原负重力或无条件30帧END。原27任意未知载荷拒绝；新片段跨10的ValRandom按裁后窗口重新插值，属于允许重做的新动作，不承诺原横移逐帧一致。路由与执行技能有重名、半份或载荷漂移时拒绝；重新构造的目标语义载荷与既有结果一致才幂等。禁止把“正常路径全部超时取消”视为修复。更新日文简介说明离陆、投下前高度再确认、六枚和着地。

现有CharacterWeaponVariantsTest仅修改Motoki的单技能计数／原89帧直接退出断言以符合新图；其余角色的原断言保留。原结束供体30帧异常的零写入回归保留。

- [ ] **Step 4: 主代理串行GREEN与独立审查。**

Run: `mvn -q "-DargLine=-Xmx6g" "-Dtest=MotokiFlightBombVariantTest,CharacterWeaponVariantsTest,PlayableWeaponLoadoutsTest,NativeErrorHandlerPreservationTest" test`。

要求零失败／错误／跳过；检查UTF8／CRLF和diff。独立审查给出Spec和质量两项结论；修正意见交原实现者处理，不以主代理未审小改绕过。

- [ ] **Step 5: 集成、实机与提交。**

主代理用现有`TestBheByJinkiRunner#testRunNineCharacterRework`完整重建，核EXE仍正确基线、PAC重解析及解包一致。新隔离case用原114行兼容seed，原Save保护保持。记录P1复核和P2零帧发弹实际高度；普通地面及移动目标必须正常投弹并有效命中，结束点必须LANDING、active=-1及弹体计数0。原生调度允许目标在复核后先更新，不能承诺任意瞬移下瞬时高度>=200。补新插值实际位移、不同距离／高度、取消／受击后的基准运动和真正跨武装连段，失败继续重做或取证，不把静态GREEN当可玩通过。

更新`使用说明.md`、`runtime-variants-20261028.md`和progress实际证据；明确未完成的全九角色训练、全武装、预加载调度范围。仅暂存任务文件，中文提交，不push。
