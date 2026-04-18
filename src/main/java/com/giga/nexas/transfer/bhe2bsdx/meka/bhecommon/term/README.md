# BHE term / InfoCollection 转换包

本包承接 BHE -> BSDX 的全局 term 语义转换，不属于 Tsukuyomi 单机体私有转换，也不属于公共弹幕 `projectile` 内部细节。

## 职责边界

- `InfoCollection` 保存的是 term DSL 实例，迁移时要保留语义，不是保留源侧索引。
- 公共弹幕资源会调用本包的转换能力，但本包必须覆盖 BHE 全局 term 语义。
- WAZ、MEK AI 里的 `InfoCollection` 都属于本包输入范围。
- 本包不负责 WAZ/SPM/SE/BatVoice 的资源索引重定向。

## 审计入口

MEK/AI 的 term 语义审计由测试生成：

`src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/MekInfoCollectionTermAuditTest.java`

输出目录：

`src/main/resources/out/bhe2bsdx/term-audit/mek`

WAZ/MEK term 迁移矩阵由测试生成：

`src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/TermMigrationMatrixAuditTest.java`

输出目录：

`src/main/resources/out/bhe2bsdx/term-audit/migration-matrix`

转换器全量语义对比由测试生成：

`src/test/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/BheInfoCollectionTermConverterSemanticTest.java`

输出目录：

`src/main/resources/out/bhe2bsdx/term-audit/converter`

正式转换入口：

`src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/BheInfoCollectionTermConverter.java`

对象图重写入口：

`src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/BheInfoCollectionObjectGraphRewriter.java`

详细迁移方案见：

`src/main/java/com/giga/nexas/transfer/bhe2bsdx/meka/bhecommon/term/TERM_MIGRATION_PLAN.md`

## 20260418 MEK 审计结论

- BHE MEK 覆盖 `95` 个文件，`2536` 条 InfoCollection。
- BSDX MEK 覆盖 `104` 个文件，`2724` 条 InfoCollection。
- 两侧 MEK/AI 的唯一语义路径均为 `POS/OBJECT`。
- 两侧 MEK/AI 审计 warning 数均为 `0`。
- MEK/AI 没有引入 WAZ 审计之外的新 term 语义路径。

结论：term converter 需要覆盖 MEK/AI 输入，但复杂语义设计主要由 WAZ 全量审计驱动。MEK/AI 不能单独走字段拷贝捷径，避免 MEK 与 WAZ 的 term 方言分裂。

## 接入范围

- 公共弹幕 WAZ：`CrossRedirectBheCommonProjectileResourcesStep` 调用对象图重写入口。
- 单机体 WAZ：`RebindWazStep` 在 WAZ 重建完成后调用对象图重写入口。
- MEK AI：`RebindMekStep` 在 MEK 重建完成后调用对象图重写入口。
