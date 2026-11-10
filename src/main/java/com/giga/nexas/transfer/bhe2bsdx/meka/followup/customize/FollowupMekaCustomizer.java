package com.giga.nexas.transfer.bhe2bsdx.meka.followup.customize;

/**
 * follow-up 单机体在“落盘前”保留的唯一客制化扩展点。
 *
 * <p>约束：
 * 1. 只做单机体特例修正；
 * 2. 不回头修改公共资源层；
 * 3. 不改变主线 graft 规则；
 * 4. 当前默认实现都是 no-op。</p>
 */
public interface FollowupMekaCustomizer {

    void customizeBeforeWrite(FollowupMekaContext context);
}
