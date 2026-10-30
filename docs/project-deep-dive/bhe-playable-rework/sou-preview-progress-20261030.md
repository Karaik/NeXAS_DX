# Sou预览进度与人工停止记录：2026-10-30

**当前未完成实机全部目标。** 新180550包case09的独立审计只确认34..62、64..119共85个武装的原生预览闭环；63未通过，120..152未验。135「雪月花」严重对象增长、无READY，暂停取证后已由root通过现有case/STOP人工终止，并非自然崩溃；源发射机制仍在调查。Java回归GREEN和这85项预览都不能代替153项菜单或九角色54追加武装的正常战斗／连段／中断验收。

本文件新增于2026-10-30，不修改已有dirty菜单文档或历史证据。此次工作只读已存JSON、原日志与文件身份，未执行Java、Maven、UI、CDB、Frida、提交或操作现场。

## case与包身份

- 运行目录：[sou-counter-camera-validation-20261030-09](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09)。
- 构建：[run_20261030_180259_554](C:/NeXAS_DX_runs/builds/run_20261030_180259_554)，EXE为`sou/BaldrSky_20261030_180550_498.exe`。
- 实际运行EXE：4,793,856字节，SHA256 `1ac677991627e60ed4095d1874733dcc490d03182c99aaa5067bf8c1b856ebae`。
- 实际运行Update3.pac：1,460,489,372字节，SHA256 `27721046fdaaee3537a1b3c66cdba3a561a1fc1c373dd3f1890a26a802d835ca`。
- 两文件已独立全哈希并与本case manifest／state一致；旧case08／130704 PAC不计入本轮覆盖。

[相机转换适配记录](D:/Code/NeXAS_DX/docs/project-deep-dive/bhe-playable-rework/bhe-camera-object-compatibility-20261030.md)记录的是源码兼容修正、RED→GREEN和当时运行边界；本文件补充新包后续预览进度，不回写旧AV现场。

## 已审预览范围

| 范围／请求 | 当前证据结论 |
| --- | --- |
| 34..62、64..119，共85个武装 | 原生预览闭环已独立核实。84个使用helper原区间内的完整链，107使用原期限后的同请求native链。 |
| 63，REQ700 | 有被接受的START，无同seq READY，后被下一请求取代；未通过。 |
| 30首次REQ28、104首次REQ946、107 REQ955 | 原helper期限negative保留；后续同请求原生READY单列，不能回填为期限内成功。 |
| 104第二次REQ949 | helper长窗口正例通过。41499ms包含REQ前等待，不能直接作为技能单次执行时长。 |
| 120..122本轮helper | 仅scroll阶段超时，无预览完成记录，不能计通过。 |
| 120..152 | 未验；135取证后已人工停止，未取得READY。 |
| 0..33 | 不在本份独立审计覆盖合计内，不据累计记录或其它回合自动补算。 |

每条闭环都精确验证同request seq、同accepted start_seq、同玩家／武装／variant的REQ → SOU_START → MATCHED_START → READY_CANDIDATE → MENU_READY，且最后双方active=-1/-1、对象计数0/0。原始记录按weapon＋requestSeq去重；不以START、早期release或JSON的closed字段单独证明完成。

107的REQ955在tick266638，helper25.383秒期限内无READY；原生READY最终在tick268596出现（+1958游戏tick），完整native链在原截止之后。104首次REQ946也在期限后于+1885tick回到READY。这些技能的早期release与残留对象清理完成不是同一时点。迟到自然闭合有独立证据，期限negative仍原样保留。

## 135当时的暂停快照

root翻页到top120时hover135触发实际REQ1027。原日志独立核到REQ／accepted START均在tick288929，武装135、variant0、玩家370042a8；随后MATCHED_START也为1027。**本次快照没有同seq1027的原生READY，135不计覆盖。**

root报告+74tick后对象数达到182279，P2 clock1，UI显示999HIT，随后CDB暂停。这是v3冻结时的状态；之后的人工停止另记下节，不回改原审计快照。源发射机制仍在调查，成因未在本报告定论；这里不依据START、对象增长或现存转储宣布通过，也不把暂停转储自动称作AV。

[现场minidump](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-object-explosion-first.dmp)：109,925字节，SHA256 `006de1f15f4dd82ad99c49f8b0865aa03cee6b0f517a8611a2408cdaf971c795`。本次只核文件身份和日志，不读写现场内存或恢复执行。

已有只读CDB observer用于收集REQ／START／READY；本次未创建新的自动观察hook、自动恢复或定时监控任务。v3读取时sidecar标running不代表CDB当时未暂停；该快照的系统异常计数为14、runtime_acceptance为not_evaluated，既不称零异常，也不自动等同致命故障。

## 取证后通过STOP人工终止

root保全证据后使用现有case/STOP机制，并核所属进程身份人工终止。CDB会话16751报告“No runnable debuggees”后执行q，launcher会话87199结束；这不是游戏自然崩溃，也没有取得135的原生READY。

[最终state.json](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/state.json)仍保留首次termination wait超时的sticky failure：status=failed，failure为`RuntimeError('Owned process did not terminate within 10 seconds')`，stop_reason=STOP_file，exit_code=3758096385（0xE0000001，owned launcher人工终止码）。首次等待因debugger暂停延迟超过10秒，后续结束不回写成成功状态。

最终cleanup_pending=false，closed_handles为thread_handle／process_handle两项，frida_detached=true，guard_violations=[]。original_save_check为unchanged=true，added／removed／changed均为空；root另以只读方式确认原Save未变。全部case EXE／PAC／隔离Save／evidence保留，没有把停止当作清理或删档。

- [manual-stop-intent.json](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-manual-stop-intent.json)：记录REQ1027、288929→289003、对象182279、notNaturalCrash=true、actorMemoryWrites=false，以及现有STOP机制。
- 四份0x1940（6464）字节内存证据：[player-370042a8](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-player-370042a8.bin)、[parent-58325620](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-parent-58325620.bin)、[pending-ec9dc188](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-pending-ec9dc188.bin)、[existing-d131d370](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-existing-d131d370.bin)。本次仅核文件身份，未解释其中内存。
- [sou135-hang-log-prefix.bin](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-hang-log-prefix.bin)：1,237,729字节，SHA256 `418445ac3516bc64b977a8b204d59d122c7a9e992e6baa61627df3a9c0736fad`；[保全证据manifest](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/evidence/sou135-preserved-evidence-manifest.json)。这份较后前缀不替换v3的1,234,256字节冻结快照。

## 固定证据

- [v3审计报告](D:/Code/NeXAS_DX/target/sou153-case09-preview-independent-20261030/report-v3.md)与[完整audit-v3.json](D:/Code/NeXAS_DX/target/sou153-case09-preview-independent-20261030/audit-v3.json)：85项逐请求链；22份累计JSON共1042行，选域998行去重为88个独立请求，并保留四份期限negative；120..122 helper记录数为0。
- [v2报告](D:/Code/NeXAS_DX/target/sou153-case09-preview-independent-20261030/report-v2.md)及[v1报告](D:/Code/NeXAS_DX/target/sou153-case09-preview-independent-20261030/report.md)保留原样；对应audit文件与日志前缀哈希均未改变。
- v3日志原路径为[case09 runtime.cdb.log](C:/NeXAS_DX_runs/runtime/sou-counter-camera-validation-20261030-09/runtime.cdb.log)，固定前缀1234256字节，UTC `2026-10-30T11:39:16.274727+00:00`，SHA256 `06661b4b9ee5e004c02a98ddbbd78c73632adc819370ad70139733e70bdb03ff`。只对该前缀负责，后续运行不能无证据扩大覆盖。

后续仍需完成135源发射机制调查、未验菜单及正常战斗玩法。当前记录只支持上述固定预览闭环，不支持“Sou全菜单通过”“54新技全部实机通过”或运行问题已经根治。
