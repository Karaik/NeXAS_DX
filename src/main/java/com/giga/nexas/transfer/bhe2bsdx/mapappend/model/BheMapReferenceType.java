package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

/**
 * BHE .map 内部可直接声明的资源引用类型。
 *
 * <p>类型枚举用于让后续消费方按引用来源处理资源，避免只拿字符串列表再猜它来自哪个字段。</p>
 */
public enum BheMapReferenceType {
    FOREGROUND,
    RESOURCE_SLOT,
    SPRITE_MAP,
    SPRITE_MAP_LIST
}
