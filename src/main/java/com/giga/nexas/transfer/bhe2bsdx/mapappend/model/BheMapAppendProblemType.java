package com.giga.nexas.transfer.bhe2bsdx.mapappend.model;

/**
 * BHE map import plan construction 能识别的问题类型。
 *
 * <p>枚举化后，测试和资源树写入流程可以按类型过滤问题，不需要解析中文审计文本。</p>
 */
public enum BheMapAppendProblemType {
    MISSING_MAP_FILE,
    MAP_PARSE_FAILURE,
    MISSING_INTERNAL_RESOURCE,
    TARGET_NAME_COLLISION,
    MISSING_PREVIEW
}
