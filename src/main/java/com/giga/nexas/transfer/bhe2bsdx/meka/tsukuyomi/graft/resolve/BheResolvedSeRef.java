package com.giga.nexas.transfer.bhe2bsdx.meka.tsukuyomi.graft.resolve;

/**
 * CEventSe 的目标侧引用。
 *
 * <p>{@code common} 用于区分该引用是否来自 BHE 公共资源层。调用方可以用它做审计，
 * 但写回 WAZ 时只应使用 target group/item 两个值。</p>
 */
public record BheResolvedSeRef(
        int targetGroupIndex,
        int targetItemIndex,
        boolean common
) {
}
