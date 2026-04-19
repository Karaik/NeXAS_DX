/**
 * BHE 单机体 initializer 武装补齐包。
 *
 * <p>本包只处理“单机体缺少 initializer 武装时如何补齐”的问题，
 * 不负责公共弹幕资源接入，也不负责 MEK/WAZ 的目标索引重绑。</p>
 *
 * <p>当前模板来源固定为真实 BSDX 二进制资源：
 * `src/main/resources/game/bsdx/mek/Zako116a.mek` 与
 * `src/main/resources/game/bsdx/waz/Zako116a.waz`。
 * 2026-04-19 动态验证发现，BHE initializer skill 转换后会在目标引擎中触发
 * `std::length_error("vector<T> too long")`；BSDX 原生 Zako116a initializer skill
 * 原样追加到 Tsukuyomi 的武装 hover 演示中也会崩溃。因此本包只复用 BSDX 原生 initializer
 * MEK 武装行，并把 WAZ 模板 skill 清洗为通用版：保留 screen/change/blur 等共性事件，
 * 替换机体专属 sprite，清空模板 SE/Voice。material trailing entry 复制当前机体已有条目，
 * 由 `weaponCategory=2` 和清洗后的 `INITIALIZER` skill 共同保留初始器语义。</p>
 */
package com.giga.nexas.transfer.bhe2bsdx.meka.initializer;
