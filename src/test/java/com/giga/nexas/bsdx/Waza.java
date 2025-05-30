package com.giga.nexas.bsdx;

import lombok.Data;

@Data
public class Waza {
    // 第几个阶段
    private String phaseQuantity;
    // 作用于哪几个帧
    private String activeFrameRange;
    // 攻击类型，攻击的对象是谁，对敌对己或不分青红皂白
    private String attackTargetType;
    // hit数
    private String hitCount;
    // 每hit间的间隔
    private String hitInterval;
}
