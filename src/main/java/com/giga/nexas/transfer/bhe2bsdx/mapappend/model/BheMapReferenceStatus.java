package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

/**
 * Step1 对 BHE .map 内部资源引用的处理结论。
 *
 * <p>FOUND_DEFERRED 表示源资源存在但本阶段不复制/转换，只写入 plan 留给后续阶段消费；
 * MISSING_NON_BLOCKING 表示源资源不存在，但当前 Step1 只记录计划，不负责物料化内部资源。</p>
 */
public enum BheMapReferenceStatus {
    FOUND_DEFERRED,
    MISSING_NON_BLOCKING
}
