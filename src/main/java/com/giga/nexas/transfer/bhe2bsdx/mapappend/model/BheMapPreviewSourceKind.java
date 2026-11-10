package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

/**
 * 预览图实际采用的源类型。
 *
 * <p>BHE 原始预览优先；只有 plan 明确声明 fallback 文件名时，物料化步骤才允许从 BSDX fallback 取图。</p>
 */
public enum BheMapPreviewSourceKind {
    BHE,
    BSDX_FALLBACK
}
