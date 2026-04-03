package com.giga.nexas.transfer.jinki2bsdx.model;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 真追加场景下用于记录 exe 容量补丁计划的对象。
 */
@Data
public class ExePatchPlan {

    private List<String> notes = new ArrayList<>();
    private List<String> targetOffsets = new ArrayList<>();
}
