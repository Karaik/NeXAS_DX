# tky.pac 实证摘要（2026-02-12）

详细版：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/TKY_PAC_ANALYSIS_AND_GENERAL_FLOW.md`

## 样本与对照路径

- 人工修正包：`src/main/resources/tky.pac`
- 人工修正包解包目录：`target/tky_pac_analysis/tky`
- Java 迁移输出：`src/main/resources/testBhe/tsukuyomi`
- 差异报告：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/tky.pac.report.json`

## 本次复核命令

```powershell
mvn -q "-Dtest=com.giga.nexas.bhe2bsdx.TransferTest#testBatchRunner,com.giga.nexas.transfer.bhe2bsdx.converter.TransferDependencyCollectorTest,com.giga.nexas.transfer.bhe2bsdx.converter.StaticAssetCopierTest,com.giga.nexas.transfer.bhe2bsdx.converter.SeGroupRealDataValidationTest" "-Dtransfer.sources=tsukuyomi" test
```

## 本次实测结论

- `manualCount=120`
- `generatedCount=107`
- `onlyInManualCount=16`
- `onlyInGeneratedCount=3`
- `commonCount=104`
- `sameHashCount=90`
- `diffHashCount=14`

关键差异：

- `onlyInManual` 含 `Bomb.waz`、多项 `ogg/png`
- `onlyInGenerated` 固定为 `c_tsukuyomi.spm`、`fire.spm`、`smoke.spm`
- `diffHashFiles` 14 项，核心包括 `nanoha.mek`、`nanoha.waz`、`SeGroup.grp`、`wazagroup.grp`

## 已确认的迁移流程反映点

1. 主链入口以 `TransferTest.testBatchRunner()` 为准：`src/test/java/com/giga/nexas/bhe2bsdx/TransferTest.java:61`
2. 依赖闭包输出在 `Bhe2BsdxSingleRunner` 接入：
   - `collectWazOutputMap`：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/Bhe2BsdxSingleRunner.java:209`
   - `collectSpmOutputMap`：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/Bhe2BsdxSingleRunner.java:216`
3. `segroup` 映射写回 `CEventSe`：
   - 映射构建：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/SeGroupIndexMapper.java:22`
   - 事件重写：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/WazConverter.java:290`
4. 静态资源收敛复制：
   - 入口：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/StaticAssetCopier.java:29`
   - 引用链：`src/main/java/com/giga/nexas/transfer/bhe2bsdx/converter/StaticAssetCopier.java:161`

## 门禁流程图

```mermaid
flowchart LR
  A[执行 tsukuyomi 迁移] --> B[输出 testBhe/tsukuyomi]
  C[解包人工修正包 tky.pac] --> D[文件名对齐]
  B --> D
  D --> E[SHA256 对照]
  E --> F[onlyInManual/onlyInGenerated/diffHash]
  F --> G[更新报告与迁移流程文档]
```

