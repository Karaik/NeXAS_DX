# 月詠／Yuri／Freja：各六个追加武装的实现检查点

月詠保留原袈裟・一閃并再加五个，Yuri与Freja各新增六个独立普通MEK，合计18个追加入口。本批实际新加17个，不把内部phase或helper算武装。具体身份、供体、时窗及日文简介见 `docs/superpowers/specs/2026-10-30-six-variants-tsuk-yuri-freja.md`；原招与公共资源保留。

实现使用现有Customizer及角色专属AdditionalVariants，全部新增在独立副本上完成签名与载荷验证后才发布新增尾行；保留旧对象、旧MEK/WAZ/Material前缀，按最终身份关联资源，不改通用pipeline。22个不同供体的完整suffix用审定literal表核对，月詠銃撃保留真实三phase／唯一(2,0)入口，不按phase数猜。

Astra6 Ultra独立审查指出的取消门缺陷已修：Yuri地走先核原SD0..53且无FF，只在私有副本裁至42再开43普通门；Freja两招先完整核唯一SD与type255 FF签名，在私有slot47移除已核整槽回绕，再裁SD末帧44／32、开45／33普通门。原招和其它槽的接地loop保留，未采用仍会永远回绕的“SD与FF一起裁短”。

Freja低空上斩私有主入口jump倍率0改为绝对恒100，48f恢复点也保留恒100；早落地恢复入口补G50／jump fall100／普通0门，保留Vz0与原落地分支。Yuri地走删除高度事件后同时去掉已核空slot20，避免生成器省略空unit导致完整图回读不同，未改global generator或放宽测试。

root先实际运行有效RED40项40失败；Astra回归v3为49项10失败，其中新增9项签名／首active／无FF回绕均真实失败、Yuri完整回环失败1。修后v4为51项只剩新F2物理2失败；物理修补后v5统一137项中，TYF三个SixVariantsTest共23项及Freja/Yuri旧CustomizerTest共11项全部零失败／错误／跳过，月詠共享追加／原载荷断言亦通过。本批包括真实源、CP932回环、完整旧前缀、Material独立、幂等及漂移零写。日志 `target/bhe-audit-20261026/nine-six-variants-green-attempt-20261030-v5.log`；RED与本组GREEN XML固定于主SDD目录。

v5整套仍有C组三角色相关失败。本次固定源码、规格和回归成果，尚未生成含54追加武装的新PAC或完成新武装实机真连／落空／中断验收；旧131244 Freja/Yuri运行记录不替代本批。整体runtimeValidated=false。
