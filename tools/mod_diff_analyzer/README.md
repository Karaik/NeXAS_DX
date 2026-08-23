# Mod 技能战斗行为差分分析器

本工具用于重现
`docs/project-deep-dive/mod-sample-skill-behavior-diff.md`
中的纯战斗行为比较。

工具严格保持以下三项边界：

- 解码后的 Mod 中间文件只写入 `target/mod_diff_analyzer/`；
- 比较前，先按照仓库现有的 BHE 到 BSDX 槽位及字段映射归一化 BHE 源事件；
- 报告排除供体身份、菜单行、驾驶员行、基础文本及语义不变的自身槽位重绑。

在仓库根目录运行：

```powershell
tools\mod_diff_analyzer\run.ps1
```

如果本机 PowerShell 策略阻止运行未签名脚本，可使用仅对当前进程生效的覆盖参数；
该参数不会修改系统策略：

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File tools\mod_diff_analyzer\run.ps1
```

运行器使用现有生产解析器，不创建也不调用新的测试类。它只会清理
`target/mod_diff_analyzer/dump/`，并输出 Markdown 报告和精简的 JSON
证据清单。报告生成器使用原始复合主键
`skillNameJapanese + skillNameEnglish`；WAZ 数字索引只作为辅助证据。

分析器只使用 `akao`、`sou`、`tsukuyomi`、`misaki` 这四个角色标识，
不会改动现有的 Lv3 `+2` Customizer 映射。
