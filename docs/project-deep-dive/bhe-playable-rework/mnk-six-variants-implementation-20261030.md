# Misaki／Naoto／Katou：各六个追加武装的实现检查点

三人原各一个追加武装保留，再各追加五个独立可装备普通MEK，合计18个追加入口。初始化器、Naoto的私有Rise辅助WAZ不另计名额。具体身份、供体、用途、日文简介、取消与物理合同见 `docs/superpowers/specs/2026-10-30-six-variants-misaki-naoto-katou.md`。

实现仅在现有三Customizer接入角色私有AdditionalVariants，使用最终已重绑的本角色MEK/WAZ；每批先在副本验证全部供体签名，再发布新增尾行，原对象、原载荷、原Material前缀及公共资源保留。关键入口、完整suffix、Material关联与变异零写均有实际回归。

物理修正限定新私有武装：Naoto Hook首Hit34f恢复G50／jump fall100；Misaki M2与Naoto三点射的准备／恢复倍率改为绝对100；Katou K1恢复G50，不写坐标或强置受击速度。Astra6 Ultra指出的Misaki M4准备出口漏验已补：完整核P0两分支与P1唯一25f NEXT、条件／目标及无额外循环，原失败测试不换节点。

root实际先跑六角色有效RED40项40失败；后续混合v2只剩Misaki漏guard一项，补后v3及v4均为本组三人28项零失败。本次v5统一137项中，本组三个SixVariantsTest共28项、旧三个VariantTest共12项均零失败／错误／跳过，包含真实源读取、CP932二进制回环、完整原载荷保全、Material独立、幂等和漂移事务拒绝。日志为 `target/bhe-audit-20261026/nine-six-variants-green-attempt-20261030-v5.log`，XML固定于 `.superpowers/sdd/2026-10-30-six-variants-and-sou-validation/green-six-roles-v5/`。

v5整套尚有C组三角色相关失败，不能将本组绿色写成九角色全通过。这次只固定源码／规格／测试成果；尚未生成含54追加武装的完整新PAC，本组三角色新技能的真实命中、跨武装连段、中断／落空及新包关卡验收均未完成。当前实机case07仍旧131244，整体runtimeValidated=false。
