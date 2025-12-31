# bhe2bsdx 移植流程记录

## 我做了什么
- 将 `TransMeka` 拆分为 Pipeline + Converter 结构，明确每一步职责与数据流。
- 补齐 `MekConverter` 的多对象深拷贝：`MekHead/MekBlocks/MekBasicInfo/MekPairBlock/MekWeaponInfoMap/MekAiInfoList/MekVoiceInfo/MekMaterialBlock`。
- 修正 `BatVoice` 的深拷贝，避免 BHE/BSDX 类型混用，语音版本不再强制写死。
- 完成 SPM hitbox 迁移：`shapeType` → BSDX `unk0` 映射，`hitFlag` 位图构建，缺省字段填零。
- 新增 grp upsert 与索引对齐：按 codeName/fileName 查找或占空槽追加，回写 `MekBasicInfo.wazFileSequence/spmFileSequence`，并建立 `spritegroup` 索引映射用于 `MekMaterialBlock.spriteGroups`。
- AI 事件与 `BsdxInfoCollection` 跨引擎迁移完成，丢弃 BHE 独有字段（无法直接映射的部分）。
- MaterialBlock 的 sprite/se/voice groups 先按协议置空，避免错误映射影响机体可用性。
- MaterialBlock 的 voiceGroups 数量改为以 BSDX `batvoice.grp` 的组数对齐，避免 BHE/BSDX 组数不一致导致错位。
- MekVoiceInfo 暂按协议置空（emotion/slot/table），避免 groupId 与 batvoice 组数不一致造成崩溃。
- 新增 `TransMekaOutputWriter`：仅输出本次迁移变更的 grp/mek/waz/spm，并执行打包。
- 新增 Nanoha 槽位替换模式：batvoice/meka/waza/sprite 直接替换目标槽位，默认保留 Nanoha key（避免追加索引）。
- spritegroup 槽位由 `Nanoha.mek` 的 `spmFileSequence` 决定（当前为 `zako_021a.spm`），主战斗 spm 输出会跟随该文件名。
- 新增 `grp_mapping_analysis.md`：整理 Tsukuyomi -> Nanoha 的详细索引映射与残留风险。
- 新增 `WazSequenceSanitizer`：对 `CEventWazaSelect` 做序号校验，避免 BHE 引用超出 BSDX 范围的效果/子弹资源导致崩溃。

## Tsukuyomi 基准数据（BHE）
- mek 基础信息：mekName=桜火、mekNameEnglish=OUKA、pilotNameKanji=月詠、pilotNameRoma=TSUKUYOMI。
- mek 描述：桜色のカラーリングに日本刀を携えたシュミクラム。甲華学装隊員、絢夜歌が愛用していたもので、月詠はこのシュミクラムの存在を覚えていませんが、それを全く感じさせない見事な動きを見せます。日本刀を抜刀してからの怒涛の連撃は、まさに鬼神。ＨＰが減ると抜刀からの攻撃バリエーションも増すのでさらに危険です。
- mek 索引：wazFileSequence=11、spmFileSequence=13、mekType=13。
- mek 数值：healthRecovery=45、forceOnKill=150、baseHealth=300、energyIncreaseLevel1=0、energyIncreaseLevel2=6、boosterLevel=0、boosterIncreaseLevel=3、permanentArmor=0、comboImpactFactor=150。
- mek 机动：fightingAbility=5、shootingAbility=1、durability=3、mobility=3、physicsWeight=0、walkingSpeed=2、normalDashSpeed=7、searchDashSpeed=14、boostDashSpeed=12、autoHoverHeight=0、weaponCount=18、aiCount=2、voiceVersion=1。
- waz（tsukuyomi.waz）：skillCount=31，首技能名=立ち。
- spm 主战斗（tsukuyomi.spm）：numPageData=1465、numImageData=22、numAnimData=77、含 hitbox 页=911。
- UI spm：`c_tsukuyomi`(2/1/2)、`g_tsukuyomi`(2/2/2)、`m_tsukuyomi`(1/1/1)、`s_tsukuyomi`(8/1/8)。
- grp 注册表（BHE）：`mekagroup`(mekaName=Tsukuyomi, code=TSUKUYOMI)、`wazagroup`(wazaName=◆月詠, code=TSUKUYOMI, display=Tsukuyomi, param=31)、`spritegroup`(file=tsukuyomi.spm, code=TSUKUYOMI, param=0)。

## SPM hitbox 迁移说明
- BHE `SPMHitArea.shapeType` 映射到 BSDX `SPMHitArea.unk0`：0→1, 1→2, 2→4, 7→3, 8→0, 9→5, 10→7, 11→6。
- `hitRect` 直接拷贝，3D zMin/zMax 写入 `unk1/unk2`，缺省补 0。
- `hitFlag` 以 hitRects 数量设置低位 bit（最多 32 位），用于 BSDX 的启用掩码。

## grp 索引校准流程
- Nanoha 槽位模式：按 `Nanoha.mek` 的 `wazFileSequence/spmFileSequence` 定位目标索引并替换，保留 Nanoha key。
- 追加模式：先在 BSDX `mekagroup/wazagroup/spritegroup` 中按 codeName/fileName 查找匹配项。
- 若无匹配，优先占用 `existFlag=0` 的空槽，否则追加到末尾并将 `existFlag` 置 1。
- 记录新索引回写到 `MekBasicInfo.wazFileSequence/spmFileSequence`；MaterialBlock groups 当前按协议置空，索引映射逻辑保留待验证。
- spritegroup 映射重建：
  - 追加模式：按 codeName/fileName upsert 并建立映射。
  - Nanoha 槽位模式：按名称建立映射（不修改 grp），并将 BHE 的 TSUKUYOMI 索引映射到 Nanoha 目标索引，供 `CEventSprite`/MaterialBlock 使用。

## TransMeka 结构拆分（便于理解流程）
- `TransMeka`：迁移入口，参数保持旧格式，内部调用 Pipeline。
- `TransMekaPipeline`：顺序编排 Step1~Step6，执行迁移主流程。
- `TransMekaRequest/TransMekaResult`：输入/输出聚合对象。
- `BatVoiceConverter`：batvoice 深拷贝。
- `GrpRegistryUpdater`：grp upsert 与索引获取。
- `SpriteGroupIndexMapper`：spritegroup 索引映射表。
- `MekConverter`：mek 总转换；内部拆分 `MekAiConverter/MekVoiceConverter/MekMaterialConverter`。
- `WazConverter`：waz 事件槽位映射与事件复制。
- `SpmConverter`：spm 结构迁移与 hitbox 适配。
- `UiSpmReplacer`：UI SPM 替换（Nanoha 槽位覆盖，默认保留 key）。
- `StaticAssetCopier`：抽取 spm 图片/语音文件名并复制到输出根目录。

## 我之后必须要做的
- UI 选择界面追加新槽位逻辑（当前仅覆盖 Nanoha）。
- 静态资源复制补齐 segroup/其它 grp 引用文件（音效等），并输出缺失清单。
- WAZ 转换中同步 `skillInfoUnknownList`，并补齐 slot52（CPU 特殊行动）等被丢弃槽位的降级逻辑。
- 完整搬运 BHE 的 `Tama01`~`Tama05` 依赖，并与 BSDX 同号资源合并/替换。
- 核对 `MekMaterialBlock` 的 se/voice 组是否也需索引映射。
- 针对 dat/csv，确认是否需要增量合并（如 pilot 列表、语音脚本）。
- 选取若干机体在真实二进制上回放测试，记录崩溃/缺失事件。

## 当前流程存在的问题
- `processWazaSkillUnitCollection` 仍忽略 `skillInfoUnknownList`，只有 Unknown 的槽位会被直接丢弃。
- slot52 在映射表中仍为 `-1`，导致 BHE 的 CPU 行动定义消失。
- `spritegroup` 索引映射依赖 codeName/fileName，未匹配到的条目会保留原索引，仍可能挂错资源。
- `MekMaterialBlock` 的 sprite/se/voice groups 目前全部置空，技能演示相关的真实语义尚未恢复。
- `MekVoiceInfo` 的 `groupId` 与 `batvoice.grp` 的对应关系尚未确认。
- UI 选择界面当前采用 Nanoha 槽位覆盖，尚未实现“追加新槽位”的正式流程。
- 静态资源复制当前只覆盖 spm 图片与追加语音，segroup 等音效尚未纳入。
- `PacUtil.pack` 仍输出 Size:0，需要确认 pac 结构/索引或资源目录组织方式。
