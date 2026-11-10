package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

/**
 * 预览图物料化状态。
 *
 * <p>缺失预览图只影响外部显示资源，不阻塞地图 plan 的形成，因此单独标成 MISSING_NON_BLOCKING。</p>
 */
public enum BheMapPreviewStatus {
    PENDING,
    COPIED,
    MISSING_NON_BLOCKING
}
