package com.giga.nexas.transfer.bhe2bsdx.model.followup;

import com.giga.nexas.transfer.bhe2bsdx.model.tsukuyomi.TsukuyomiGraftResult;

/**
 * 后续 9 个单机体共用的结果基类。
 *
 * <p>当前不新增字段，只把 follow-up 单机体统一收敛到一个结果父类上，
 * 方便公共 pipeline 用泛型返回各机体自己的 Result 子类。</p>
 */
public class FollowupGraftResult extends TsukuyomiGraftResult {
}
