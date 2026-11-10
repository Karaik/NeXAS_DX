package com.giga.nexas.transfer.bhe2bsdx.meka.bhecommon.term;

/** 决定aux字典的是运行时消费者；表达式根并非在所有宿主中都表示同一用途。 */
public enum AuxConsumerPurpose {
    EXPRESSION, POSITION, ANGLE, OBJECT_REFERENCE, PARAMETER, UNUSED, UNKNOWN
}
