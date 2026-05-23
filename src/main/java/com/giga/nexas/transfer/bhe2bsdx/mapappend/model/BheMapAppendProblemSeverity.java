package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

/**
 * Step1 问题严重度。
 *
 * <p>BLOCKING 会阻止后续真实追加；NON_BLOCKING 只记录素材缺失或降级，不阻止 plan 消费。</p>
 */
public enum BheMapAppendProblemSeverity {
    BLOCKING,
    NON_BLOCKING
}
